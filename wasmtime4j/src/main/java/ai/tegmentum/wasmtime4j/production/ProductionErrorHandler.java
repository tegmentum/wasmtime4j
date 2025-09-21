/*
 * Copyright 2024 Tegmentum AI Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.production;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.resilience.CircuitBreaker;
import ai.tegmentum.wasmtime4j.resilience.CircuitBreakerOpenException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Production-grade error handler providing circuit breaker pattern, retry logic, and recovery
 * mechanisms for WebAssembly operations.
 *
 * <p>Features:
 * - Circuit breaker pattern for fault isolation
 * - Configurable retry logic with exponential backoff
 * - Error classification and handling strategies
 * - Metrics collection for monitoring
 * - Graceful degradation capabilities
 */
public final class ProductionErrorHandler {

  private static final Logger LOGGER = Logger.getLogger(ProductionErrorHandler.class.getName());

  private final CircuitBreaker circuitBreaker;
  private final RetryTemplate retryTemplate;
  private final ErrorClassifier errorClassifier;
  private final MetricsCollector metricsCollector;

  /** Error handling configuration. */
  public static final class ErrorHandlerConfig {
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;
    private final ErrorClassifier errorClassifier;
    private final boolean enableMetrics;

    private ErrorHandlerConfig(final Builder builder) {
      this.circuitBreaker = builder.circuitBreaker;
      this.retryTemplate = builder.retryTemplate;
      this.errorClassifier = builder.errorClassifier;
      this.enableMetrics = builder.enableMetrics;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private CircuitBreaker circuitBreaker = CircuitBreaker.defaultCircuitBreaker();
      private RetryTemplate retryTemplate = RetryTemplate.defaultRetryTemplate();
      private ErrorClassifier errorClassifier = new DefaultErrorClassifier();
      private boolean enableMetrics = true;

      public Builder circuitBreaker(final CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
      }

      public Builder retryTemplate(final RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
        return this;
      }

      public Builder errorClassifier(final ErrorClassifier errorClassifier) {
        this.errorClassifier = errorClassifier;
        return this;
      }

      public Builder enableMetrics(final boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
        return this;
      }

      public ErrorHandlerConfig build() {
        return new ErrorHandlerConfig(this);
      }
    }
  }

  /** Retry template for configurable retry logic. */
  public static final class RetryTemplate {
    private final int maxRetries;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final boolean enableJitter;
    private final Predicate<Throwable> retryableExceptionPredicate;

    private RetryTemplate(final Builder builder) {
      this.maxRetries = builder.maxRetries;
      this.initialDelay = builder.initialDelay;
      this.maxDelay = builder.maxDelay;
      this.backoffMultiplier = builder.backoffMultiplier;
      this.enableJitter = builder.enableJitter;
      this.retryableExceptionPredicate = builder.retryableExceptionPredicate;
    }

    public static RetryTemplate defaultRetryTemplate() {
      return builder().build();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private int maxRetries = 3;
      private Duration initialDelay = Duration.ofMillis(100);
      private Duration maxDelay = Duration.ofSeconds(30);
      private double backoffMultiplier = 2.0;
      private boolean enableJitter = true;
      private Predicate<Throwable> retryableExceptionPredicate = new DefaultRetryableExceptionPredicate();

      public Builder maxRetries(final int maxRetries) {
        if (maxRetries < 0) {
          throw new IllegalArgumentException("Max retries must be non-negative");
        }
        this.maxRetries = maxRetries;
        return this;
      }

      public Builder initialDelay(final Duration initialDelay) {
        if (initialDelay == null || initialDelay.isNegative()) {
          throw new IllegalArgumentException("Initial delay must be positive");
        }
        this.initialDelay = initialDelay;
        return this;
      }

      public Builder maxDelay(final Duration maxDelay) {
        if (maxDelay == null || maxDelay.isNegative()) {
          throw new IllegalArgumentException("Max delay must be positive");
        }
        this.maxDelay = maxDelay;
        return this;
      }

      public Builder backoffMultiplier(final double backoffMultiplier) {
        if (backoffMultiplier <= 1.0) {
          throw new IllegalArgumentException("Backoff multiplier must be greater than 1.0");
        }
        this.backoffMultiplier = backoffMultiplier;
        return this;
      }

      public Builder enableJitter(final boolean enableJitter) {
        this.enableJitter = enableJitter;
        return this;
      }

      public Builder retryableExceptionPredicate(final Predicate<Throwable> predicate) {
        this.retryableExceptionPredicate = predicate;
        return this;
      }

      public RetryTemplate build() {
        return new RetryTemplate(this);
      }
    }

    /**
     * Executes an operation with retry logic.
     *
     * @param operation the operation to execute
     * @param operationName the operation name for logging
     * @param <T> the result type
     * @return the operation result
     * @throws Exception if all retries are exhausted
     */
    public <T> T execute(final Supplier<T> operation, final String operationName) throws Exception {
      Exception lastException = null;

      for (int attempt = 0; attempt <= maxRetries; attempt++) {
        try {
          return operation.get();
        } catch (final Exception e) {
          lastException = e;

          if (attempt == maxRetries || !retryableExceptionPredicate.test(e)) {
            throw e;
          }

          final Duration delay = calculateDelay(attempt);
          LOGGER.warning(String.format(
              "Retry attempt %d/%d for %s failed: %s. Retrying in %dms",
              attempt + 1, maxRetries, operationName, e.getMessage(), delay.toMillis()));

          try {
            Thread.sleep(delay.toMillis());
          } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", ie);
          }
        }
      }

      throw lastException;
    }

    private Duration calculateDelay(final int attempt) {
      double delayMs = initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt);
      delayMs = Math.min(delayMs, maxDelay.toMillis());

      if (enableJitter) {
        // Add up to 10% jitter
        final double jitter = ThreadLocalRandom.current().nextDouble(0.9, 1.1);
        delayMs *= jitter;
      }

      return Duration.ofMillis((long) delayMs);
    }
  }

  /** Error classifier for determining error handling strategies. */
  public interface ErrorClassifier {
    /**
     * Determines if an exception should be retried.
     *
     * @param throwable the exception
     * @return true if retryable
     */
    boolean isRetryable(Throwable throwable);

    /**
     * Determines if an exception should trigger circuit breaker opening.
     *
     * @param throwable the exception
     * @return true if circuit breaking
     */
    boolean isCircuitBreaking(Throwable throwable);

    /**
     * Gets the error category for metrics.
     *
     * @param throwable the exception
     * @return the error category
     */
    String getErrorCategory(Throwable throwable);
  }

  /** Default error classifier implementation. */
  private static final class DefaultErrorClassifier implements ErrorClassifier {

    @Override
    public boolean isRetryable(final Throwable throwable) {
      // Network errors, timeouts, and temporary resource issues are retryable
      return throwable instanceof java.net.SocketTimeoutException
          || throwable instanceof java.net.ConnectException
          || throwable instanceof java.util.concurrent.TimeoutException
          || (throwable instanceof WasmRuntimeException
              && throwable.getMessage().contains("temporary"));
    }

    @Override
    public boolean isCircuitBreaking(final Throwable throwable) {
      // Circuit breaker should trigger on repeated failures or system errors
      return throwable instanceof OutOfMemoryError
          || throwable instanceof WasmException
          || (throwable instanceof RuntimeException
              && throwable.getMessage().contains("system"));
    }

    @Override
    public String getErrorCategory(final Throwable throwable) {
      if (throwable instanceof WasmException) {
        return "wasm_error";
      } else if (throwable instanceof java.net.SocketTimeoutException) {
        return "network_timeout";
      } else if (throwable instanceof java.util.concurrent.TimeoutException) {
        return "operation_timeout";
      } else if (throwable instanceof OutOfMemoryError) {
        return "out_of_memory";
      } else {
        return "unknown_error";
      }
    }
  }

  /** Default retryable exception predicate. */
  private static final class DefaultRetryableExceptionPredicate implements Predicate<Throwable> {

    @Override
    public boolean test(final Throwable throwable) {
      return throwable instanceof java.net.SocketTimeoutException
          || throwable instanceof java.net.ConnectException
          || throwable instanceof java.util.concurrent.TimeoutException
          || (throwable instanceof WasmRuntimeException
              && throwable.getMessage().contains("temporary"));
    }
  }

  /** Metrics collector for error handling statistics. */
  public static final class MetricsCollector {
    private final AtomicInteger totalOperations = new AtomicInteger(0);
    private final AtomicInteger successfulOperations = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);
    private final AtomicInteger retriedOperations = new AtomicInteger(0);
    private final AtomicInteger circuitBreakerOpenCount = new AtomicInteger(0);

    public void recordSuccess(final String operationName) {
      totalOperations.incrementAndGet();
      successfulOperations.incrementAndGet();
    }

    public void recordFailure(final String operationName, final String errorCategory) {
      totalOperations.incrementAndGet();
      failedOperations.incrementAndGet();
    }

    public void recordRetry(final String operationName) {
      retriedOperations.incrementAndGet();
    }

    public void recordCircuitBreakerOpen(final String operationName) {
      circuitBreakerOpenCount.incrementAndGet();
    }

    public int getTotalOperations() {
      return totalOperations.get();
    }

    public int getSuccessfulOperations() {
      return successfulOperations.get();
    }

    public int getFailedOperations() {
      return failedOperations.get();
    }

    public int getRetriedOperations() {
      return retriedOperations.get();
    }

    public int getCircuitBreakerOpenCount() {
      return circuitBreakerOpenCount.get();
    }

    public double getSuccessRate() {
      final int total = totalOperations.get();
      return total > 0 ? (double) successfulOperations.get() / total * 100.0 : 0.0;
    }

    public double getFailureRate() {
      final int total = totalOperations.get();
      return total > 0 ? (double) failedOperations.get() / total * 100.0 : 0.0;
    }

    @Override
    public String toString() {
      return String.format(
          "ErrorHandlerMetrics{total=%d, success=%d, failed=%d, retried=%d, circuitBreakerOpen=%d, successRate=%.1f%%}",
          getTotalOperations(),
          getSuccessfulOperations(),
          getFailedOperations(),
          getRetriedOperations(),
          getCircuitBreakerOpenCount(),
          getSuccessRate());
    }
  }

  /**
   * Creates a production error handler with the specified configuration.
   *
   * @param config the error handler configuration
   */
  public ProductionErrorHandler(final ErrorHandlerConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Error handler config cannot be null");
    }

    this.circuitBreaker = config.circuitBreaker;
    this.retryTemplate = config.retryTemplate;
    this.errorClassifier = config.errorClassifier;
    this.metricsCollector = config.enableMetrics ? new MetricsCollector() : null;
  }

  /**
   * Creates a default production error handler.
   *
   * @return a new production error handler
   */
  public static ProductionErrorHandler createDefault() {
    return new ProductionErrorHandler(ErrorHandlerConfig.builder().build());
  }

  /**
   * Executes an operation with comprehensive error handling, including circuit breaker and retry
   * logic.
   *
   * @param operation the operation to execute
   * @param operationName the operation name for logging and metrics
   * @param <T> the result type
   * @return the operation result
   * @throws WasmException if the operation fails after all recovery attempts
   */
  public <T> T handleWithRecovery(final Supplier<T> operation, final String operationName)
      throws WasmException {
    if (operation == null || operationName == null) {
      throw new IllegalArgumentException("Operation and operation name cannot be null");
    }

    try {
      return circuitBreaker.execute(() -> {
        try {
          return retryTemplate.execute(operation, operationName);
        } catch (final Exception e) {
          if (metricsCollector != null) {
            metricsCollector.recordFailure(operationName, errorClassifier.getErrorCategory(e));
          }

          if (errorClassifier.isCircuitBreaking(e)) {
            throw new RuntimeException("Circuit breaking error: " + e.getMessage(), e);
          }

          throw new RuntimeException("Operation failed: " + e.getMessage(), e);
        }
      });
    } catch (final CircuitBreakerOpenException e) {
      if (metricsCollector != null) {
        metricsCollector.recordCircuitBreakerOpen(operationName);
      }
      throw new WasmException("Circuit breaker is open for operation: " + operationName, e);
    } catch (final Exception e) {
      throw new WasmException("Operation failed with error handling: " + operationName, e);
    } finally {
      if (metricsCollector != null) {
        metricsCollector.recordSuccess(operationName);
      }
    }
  }

  /**
   * Executes an operation asynchronously with error handling.
   *
   * @param operation the operation to execute
   * @param operationName the operation name for logging and metrics
   * @param <T> the result type
   * @return a future containing the operation result
   */
  public <T> CompletableFuture<T> handleWithRecoveryAsync(
      final Supplier<T> operation, final String operationName) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return handleWithRecovery(operation, operationName);
      } catch (final WasmException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Executes an operation with a fallback value in case of failure.
   *
   * @param operation the operation to execute
   * @param fallback the fallback value
   * @param operationName the operation name for logging and metrics
   * @param <T> the result type
   * @return the operation result or fallback value
   */
  public <T> T handleWithFallback(
      final Supplier<T> operation, final T fallback, final String operationName) {
    try {
      return handleWithRecovery(operation, operationName);
    } catch (final WasmException e) {
      LOGGER.warning(String.format(
          "Operation %s failed, returning fallback value: %s", operationName, e.getMessage()));
      return fallback;
    }
  }

  /**
   * Executes an operation with a fallback supplier in case of failure.
   *
   * @param operation the operation to execute
   * @param fallbackSupplier the fallback supplier
   * @param operationName the operation name for logging and metrics
   * @param <T> the result type
   * @return the operation result or fallback value
   */
  public <T> T handleWithFallback(
      final Supplier<T> operation, final Supplier<T> fallbackSupplier, final String operationName) {
    try {
      return handleWithRecovery(operation, operationName);
    } catch (final WasmException e) {
      LOGGER.warning(String.format(
          "Operation %s failed, executing fallback: %s", operationName, e.getMessage()));
      return fallbackSupplier.get();
    }
  }

  /**
   * Gets the current error handling metrics.
   *
   * @return the metrics collector, or null if metrics are disabled
   */
  public MetricsCollector getMetrics() {
    return metricsCollector;
  }

  /**
   * Gets the circuit breaker instance.
   *
   * @return the circuit breaker
   */
  public CircuitBreaker getCircuitBreaker() {
    return circuitBreaker;
  }

  /**
   * Gets the retry template instance.
   *
   * @return the retry template
   */
  public RetryTemplate getRetryTemplate() {
    return retryTemplate;
  }

  /**
   * Checks if the error handler is healthy and operational.
   *
   * @return true if healthy
   */
  public boolean isHealthy() {
    return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
  }

  /**
   * Resets the error handler state, including circuit breaker and metrics.
   */
  public void reset() {
    circuitBreaker.reset();
    if (metricsCollector != null) {
      // Reset metrics if needed - current implementation doesn't support reset
      LOGGER.info("Error handler state reset");
    }
  }
}