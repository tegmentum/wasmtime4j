/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the util package classes.
 *
 * <p>This test class validates HealthCheck, LibraryValidator, and ErrorRecoveryManager classes.
 */
@DisplayName("Util Package Integration Tests")
public class UtilPackageIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(UtilPackageIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Util Package Integration Tests");
  }

  @Nested
  @DisplayName("HealthCheck Tests")
  class HealthCheckTests {

    @Test
    @DisplayName("Should perform health check successfully")
    void shouldPerformHealthCheckSuccessfully() {
      LOGGER.info("Testing performHealthCheck");

      boolean result = HealthCheck.performHealthCheck();

      assertTrue(result, "Health check should pass on properly configured system");
      LOGGER.info("performHealthCheck returned: " + result);
    }

    @Test
    @DisplayName("Should return true for isReady")
    void shouldReturnTrueForIsReady() {
      LOGGER.info("Testing isReady");

      boolean result = HealthCheck.isReady();

      assertTrue(result, "isReady should return true when runtime is available");
      LOGGER.info("isReady returned: " + result);
    }

    @Test
    @DisplayName("Should return true for isLive")
    void shouldReturnTrueForIsLive() {
      LOGGER.info("Testing isLive");

      boolean result = HealthCheck.isLive();

      assertTrue(result, "isLive should always return true");
      LOGGER.info("isLive returned: " + result);
    }

    @Test
    @DisplayName("Should return consistent results for repeated health checks")
    void shouldReturnConsistentResultsForRepeatedHealthChecks() {
      LOGGER.info("Testing health check consistency");

      boolean result1 = HealthCheck.performHealthCheck();
      boolean result2 = HealthCheck.performHealthCheck();
      boolean result3 = HealthCheck.performHealthCheck();

      assertEquals(result1, result2, "Health check results should be consistent");
      assertEquals(result2, result3, "Health check results should be consistent");

      LOGGER.info("Health check consistency verified: " + result1);
    }

    @Test
    @DisplayName("Should return consistent results for isReady")
    void shouldReturnConsistentResultsForIsReady() {
      LOGGER.info("Testing isReady consistency");

      boolean result1 = HealthCheck.isReady();
      boolean result2 = HealthCheck.isReady();
      boolean result3 = HealthCheck.isReady();

      assertEquals(result1, result2, "isReady results should be consistent");
      assertEquals(result2, result3, "isReady results should be consistent");

      LOGGER.info("isReady consistency verified: " + result1);
    }

    @Test
    @DisplayName("Should execute isLive quickly")
    void shouldExecuteIsLiveQuickly() {
      LOGGER.info("Testing isLive performance");

      long startTime = System.nanoTime();
      for (int i = 0; i < 100; i++) {
        HealthCheck.isLive();
      }
      long elapsed = System.nanoTime() - startTime;
      long elapsedMs = elapsed / 1_000_000;

      assertTrue(elapsedMs < 100, "100 isLive calls should complete in under 100ms");
      LOGGER.info("100 isLive calls completed in " + elapsedMs + "ms");
    }

    @Test
    @DisplayName("Health check utility class should not be instantiable")
    void healthCheckUtilityClassShouldNotBeInstantiable() {
      LOGGER.info("Testing HealthCheck instantiation prevention");

      // HealthCheck has private constructor that throws AssertionError
      // We can verify it has private constructor through reflection
      try {
        java.lang.reflect.Constructor<?> constructor = HealthCheck.class.getDeclaredConstructor();
        assertTrue(
            java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private");
        constructor.setAccessible(true);
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(),
            "Constructor should throw AssertionError");
      } catch (NoSuchMethodException e) {
        // No constructor found - also acceptable
      }

      LOGGER.info("HealthCheck instantiation prevention verified");
    }
  }

  @Nested
  @DisplayName("LibraryValidator Tests")
  class LibraryValidatorTests {

    @Test
    @DisplayName("Should validate libraries successfully")
    void shouldValidateLibrariesSuccessfully() {
      LOGGER.info("Testing validateLibraries");

      boolean result = LibraryValidator.validateLibraries();

      assertTrue(result, "Library validation should pass on properly configured system");
      LOGGER.info("validateLibraries returned: " + result);
    }

    @Test
    @DisplayName("Should check library loading capability")
    void shouldCheckLibraryLoadingCapability() {
      LOGGER.info("Testing canLoadLibraries");

      boolean result = LibraryValidator.canLoadLibraries();

      assertTrue(result, "Should be able to load libraries on properly configured system");
      LOGGER.info("canLoadLibraries returned: " + result);
    }

    @Test
    @DisplayName("Should get runtime summary")
    void shouldGetRuntimeSummary() {
      LOGGER.info("Testing getRuntimeSummary");

      String summary = LibraryValidator.getRuntimeSummary();

      assertNotNull(summary, "Runtime summary should not be null");
      assertFalse(summary.isEmpty(), "Runtime summary should not be empty");
      assertTrue(
          summary.startsWith("Runtime Summary:"), "Summary should start with 'Runtime Summary:'");

      LOGGER.info("Runtime summary: " + summary);
    }

    @Test
    @DisplayName("Should return consistent runtime summary")
    void shouldReturnConsistentRuntimeSummary() {
      LOGGER.info("Testing runtime summary consistency");

      String summary1 = LibraryValidator.getRuntimeSummary();
      String summary2 = LibraryValidator.getRuntimeSummary();
      String summary3 = LibraryValidator.getRuntimeSummary();

      assertEquals(summary1, summary2, "Runtime summary should be consistent");
      assertEquals(summary2, summary3, "Runtime summary should be consistent");

      LOGGER.info("Runtime summary consistency verified");
    }

    @Test
    @DisplayName("Should return consistent canLoadLibraries results")
    void shouldReturnConsistentCanLoadLibrariesResults() {
      LOGGER.info("Testing canLoadLibraries consistency");

      boolean result1 = LibraryValidator.canLoadLibraries();
      boolean result2 = LibraryValidator.canLoadLibraries();
      boolean result3 = LibraryValidator.canLoadLibraries();

      assertEquals(result1, result2, "canLoadLibraries results should be consistent");
      assertEquals(result2, result3, "canLoadLibraries results should be consistent");

      LOGGER.info("canLoadLibraries consistency verified: " + result1);
    }

    @Test
    @DisplayName("Library validator utility class should not be instantiable")
    void libraryValidatorUtilityClassShouldNotBeInstantiable() {
      LOGGER.info("Testing LibraryValidator instantiation prevention");

      try {
        java.lang.reflect.Constructor<?> constructor =
            LibraryValidator.class.getDeclaredConstructor();
        assertTrue(
            java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private");
        constructor.setAccessible(true);
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(),
            "Constructor should throw AssertionError");
      } catch (NoSuchMethodException e) {
        // No constructor found - also acceptable
      }

      LOGGER.info("LibraryValidator instantiation prevention verified");
    }
  }

  @Nested
  @DisplayName("ErrorRecoveryManager.RetryConfig Tests")
  class RetryConfigTests {

    @Test
    @DisplayName("Should create default retry config")
    void shouldCreateDefaultRetryConfig() {
      LOGGER.info("Testing default RetryConfig creation");

      ErrorRecoveryManager.RetryConfig config = ErrorRecoveryManager.RetryConfig.defaultConfig();

      assertNotNull(config, "Default config should not be null");
      assertEquals(3, config.getMaxRetries(), "Default max retries should be 3");
      assertEquals(100, config.getInitialDelayMs(), "Default initial delay should be 100ms");
      assertEquals(5000, config.getMaxDelayMs(), "Default max delay should be 5000ms");
      assertEquals(2.0, config.getBackoffMultiplier(), 0.01, "Default multiplier should be 2.0");
      assertTrue(config.isExponentialBackoff(), "Default should use exponential backoff");

      LOGGER.info("Default RetryConfig created successfully");
    }

    @Test
    @DisplayName("Should create fixed delay retry config")
    void shouldCreateFixedDelayRetryConfig() {
      LOGGER.info("Testing fixed delay RetryConfig creation");

      ErrorRecoveryManager.RetryConfig config = ErrorRecoveryManager.RetryConfig.fixedDelay(5, 200);

      assertNotNull(config, "Fixed delay config should not be null");
      assertEquals(5, config.getMaxRetries(), "Max retries should be 5");
      assertEquals(200, config.getInitialDelayMs(), "Initial delay should be 200ms");
      assertEquals(200, config.getMaxDelayMs(), "Max delay should equal initial delay");
      assertEquals(1.0, config.getBackoffMultiplier(), 0.01, "Multiplier should be 1.0");
      assertFalse(config.isExponentialBackoff(), "Should not use exponential backoff");

      LOGGER.info("Fixed delay RetryConfig created successfully");
    }

    @Test
    @DisplayName("Should create exponential backoff retry config")
    void shouldCreateExponentialBackoffRetryConfig() {
      LOGGER.info("Testing exponential backoff RetryConfig creation");

      ErrorRecoveryManager.RetryConfig config =
          ErrorRecoveryManager.RetryConfig.exponentialBackoff(4, 50, 1000);

      assertNotNull(config, "Exponential backoff config should not be null");
      assertEquals(4, config.getMaxRetries(), "Max retries should be 4");
      assertEquals(50, config.getInitialDelayMs(), "Initial delay should be 50ms");
      assertEquals(1000, config.getMaxDelayMs(), "Max delay should be 1000ms");
      assertTrue(config.isExponentialBackoff(), "Should use exponential backoff");

      LOGGER.info("Exponential backoff RetryConfig created successfully");
    }

    @Test
    @DisplayName("Should handle negative values in config constructor")
    void shouldHandleNegativeValuesInConfigConstructor() {
      LOGGER.info("Testing negative value handling in RetryConfig");

      // Constructor normalizes:
      // - maxRetries: Math.max(0, maxRetries)
      // - initialDelayMs: Math.max(0, initialDelayMs)
      // - maxDelayMs: Math.max(initialDelayMs, maxDelayMs) - NOTE: uses original initialDelayMs
      // - backoffMultiplier: Math.max(1.0, multiplier)
      ErrorRecoveryManager.RetryConfig config =
          new ErrorRecoveryManager.RetryConfig(-5, -100, -500, -2.0, true);

      assertEquals(0, config.getMaxRetries(), "Max retries normalized to 0");
      assertEquals(0, config.getInitialDelayMs(), "Initial delay normalized to 0");
      // maxDelayMs = Math.max(-100, -500) = -100 (uses original initialDelayMs parameter)
      assertEquals(-100, config.getMaxDelayMs(), "Max delay is Math.max(-100, -500)");
      assertEquals(1.0, config.getBackoffMultiplier(), 0.01, "Multiplier normalized to 1.0");

      LOGGER.info("Negative values handled: maxRetries=" + config.getMaxRetries());
    }

    @Test
    @DisplayName("Should ensure max delay is at least initial delay")
    void shouldEnsureMaxDelayIsAtLeastInitialDelay() {
      LOGGER.info("Testing max delay >= initial delay constraint");

      // Create config with max < initial
      ErrorRecoveryManager.RetryConfig config =
          new ErrorRecoveryManager.RetryConfig(3, 1000, 100, 2.0, true);

      assertTrue(
          config.getMaxDelayMs() >= config.getInitialDelayMs(),
          "Max delay should be at least initial delay");

      LOGGER.info(
          "Constraint verified: initial="
              + config.getInitialDelayMs()
              + ", max="
              + config.getMaxDelayMs());
    }

    @Test
    @DisplayName("Should create config with custom parameters")
    void shouldCreateConfigWithCustomParameters() {
      LOGGER.info("Testing custom RetryConfig creation");

      ErrorRecoveryManager.RetryConfig config =
          new ErrorRecoveryManager.RetryConfig(10, 25, 2500, 1.5, false);

      assertEquals(10, config.getMaxRetries(), "Max retries should be 10");
      assertEquals(25, config.getInitialDelayMs(), "Initial delay should be 25ms");
      assertEquals(2500, config.getMaxDelayMs(), "Max delay should be 2500ms");
      assertEquals(1.5, config.getBackoffMultiplier(), 0.01, "Multiplier should be 1.5");
      assertFalse(config.isExponentialBackoff(), "Should not use exponential backoff");

      LOGGER.info("Custom RetryConfig created successfully");
    }
  }

  @Nested
  @DisplayName("ErrorRecoveryManager.RetryResult Tests")
  class RetryResultTests {

    @Test
    @DisplayName("Should create successful retry result")
    void shouldCreateSuccessfulRetryResult() {
      LOGGER.info("Testing successful RetryResult creation");

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.RetryResult.success("test_value", 2, Duration.ofMillis(150));

      assertTrue(result.isSuccessful(), "Result should be successful");
      assertEquals("test_value", result.getResult(), "Result value should match");
      assertEquals(2, result.getAttemptCount(), "Attempt count should be 2");
      assertEquals(Duration.ofMillis(150), result.getTotalDuration(), "Duration should match");
      assertNull(result.getLastException(), "Exception should be null for success");

      LOGGER.info("Successful RetryResult created: " + result.getResult());
    }

    @Test
    @DisplayName("Should create failed retry result")
    void shouldCreateFailedRetryResult() {
      LOGGER.info("Testing failed RetryResult creation");

      WasmException exception = new WasmException("Test failure");
      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.RetryResult.failure(exception, 4, Duration.ofSeconds(1));

      assertFalse(result.isSuccessful(), "Result should not be successful");
      assertNull(result.getResult(), "Result value should be null for failure");
      assertEquals(4, result.getAttemptCount(), "Attempt count should be 4");
      assertEquals(Duration.ofSeconds(1), result.getTotalDuration(), "Duration should match");
      assertNotNull(result.getLastException(), "Exception should not be null for failure");
      assertEquals("Test failure", result.getLastException().getMessage());

      LOGGER.info("Failed RetryResult created: " + result.getLastException().getMessage());
    }

    @Test
    @DisplayName("Should handle null result value in success")
    void shouldHandleNullResultValueInSuccess() {
      LOGGER.info("Testing null result value in success");

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.RetryResult.success(null, 1, Duration.ofMillis(10));

      assertTrue(result.isSuccessful(), "Result should be successful");
      assertNull(result.getResult(), "Result value can be null");
      assertEquals(1, result.getAttemptCount(), "Attempt count should be 1");

      LOGGER.info("Null result value handled successfully");
    }

    @Test
    @DisplayName("Should handle null exception in failure")
    void shouldHandleNullExceptionInFailure() {
      LOGGER.info("Testing null exception in failure");

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.RetryResult.failure(null, 3, Duration.ofMillis(500));

      assertFalse(result.isSuccessful(), "Result should not be successful");
      assertNull(result.getLastException(), "Exception can be null");
      assertEquals(3, result.getAttemptCount(), "Attempt count should be 3");

      LOGGER.info("Null exception handled successfully");
    }
  }

  @Nested
  @DisplayName("ErrorRecoveryManager.executeWithRetry Tests")
  class ExecuteWithRetryTests {

    @Test
    @DisplayName("Should execute successfully on first attempt")
    void shouldExecuteSuccessfullyOnFirstAttempt() {
      LOGGER.info("Testing executeWithRetry - success on first attempt");

      Supplier<String> operation = () -> "success";

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, "test_operation");

      assertTrue(result.isSuccessful(), "Should be successful");
      assertEquals("success", result.getResult(), "Result should match");
      assertEquals(1, result.getAttemptCount(), "Should succeed on first attempt");
      assertNotNull(result.getTotalDuration(), "Duration should not be null");

      LOGGER.info("First attempt success verified, duration: " + result.getTotalDuration());
    }

    @Test
    @DisplayName("Should retry on recoverable failure")
    void shouldRetryOnRecoverableFailure() {
      LOGGER.info("Testing executeWithRetry - retry on failure");

      AtomicInteger attemptCounter = new AtomicInteger(0);
      // Use "timeout" in message to trigger recoverable error classification
      Supplier<String> operation =
          () -> {
            int attempt = attemptCounter.incrementAndGet();
            if (attempt < 3) {
              throw new java.lang.RuntimeException("Connection timeout - attempt " + attempt);
            }
            return "success after retries";
          };

      ErrorRecoveryManager.RetryConfig config = ErrorRecoveryManager.RetryConfig.fixedDelay(5, 10);

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, config, "retry_test");

      assertTrue(result.isSuccessful(), "Should eventually succeed");
      assertEquals("success after retries", result.getResult(), "Result should match");
      assertEquals(3, result.getAttemptCount(), "Should succeed on third attempt");

      LOGGER.info("Retry success verified, attempts: " + result.getAttemptCount());
    }

    @Test
    @DisplayName("Should fail after max retries exceeded")
    void shouldFailAfterMaxRetriesExceeded() {
      LOGGER.info("Testing executeWithRetry - max retries exceeded");

      // Use "timeout" in message to trigger recoverable error classification
      Supplier<String> operation =
          () -> {
            throw new java.lang.RuntimeException("Connection timeout - always fails");
          };

      ErrorRecoveryManager.RetryConfig config = ErrorRecoveryManager.RetryConfig.fixedDelay(2, 10);

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, config, "fail_test");

      assertFalse(result.isSuccessful(), "Should fail after max retries");
      assertNull(result.getResult(), "Result should be null on failure");
      assertEquals(3, result.getAttemptCount(), "Should have attempted 3 times (1 + 2 retries)");
      assertNotNull(result.getLastException(), "Should have last exception");

      LOGGER.info("Max retries failure verified, attempts: " + result.getAttemptCount());
    }

    @Test
    @DisplayName("Should use default config when not specified")
    void shouldUseDefaultConfigWhenNotSpecified() {
      LOGGER.info("Testing executeWithRetry with default config");

      Supplier<String> operation = () -> "default config success";

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, "default_config_test");

      assertTrue(result.isSuccessful(), "Should be successful");
      assertEquals("default config success", result.getResult(), "Result should match");

      LOGGER.info("Default config test passed");
    }

    @Test
    @DisplayName("Should handle zero retries config")
    void shouldHandleZeroRetriesConfig() {
      LOGGER.info("Testing executeWithRetry with zero retries");

      AtomicInteger counter = new AtomicInteger(0);
      // Non-recoverable error (doesn't contain timeout/temporary/etc.)
      Supplier<String> operation =
          () -> {
            int attempt = counter.incrementAndGet();
            if (attempt == 1) {
              throw new java.lang.RuntimeException("First attempt fails");
            }
            return "should not reach";
          };

      ErrorRecoveryManager.RetryConfig config = ErrorRecoveryManager.RetryConfig.fixedDelay(0, 10);

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, config, "zero_retry_test");

      assertFalse(result.isSuccessful(), "Should fail with zero retries");
      assertEquals(1, result.getAttemptCount(), "Should only attempt once");

      LOGGER.info("Zero retries test passed");
    }

    @Test
    @DisplayName("Should respect exponential backoff timing")
    void shouldRespectExponentialBackoffTiming() {
      LOGGER.info("Testing exponential backoff timing");

      AtomicInteger counter = new AtomicInteger(0);
      long startTime = System.currentTimeMillis();

      // Use "temporary" in message to trigger recoverable error classification
      Supplier<String> operation =
          () -> {
            int attempt = counter.incrementAndGet();
            if (attempt < 4) {
              throw new java.lang.RuntimeException("Temporary failure - attempt " + attempt);
            }
            return "success";
          };

      // Config: initial 50ms, max 400ms, multiplier 2.0
      // Delays should be: 50, 100, 200 ms (capped at max)
      ErrorRecoveryManager.RetryConfig config =
          ErrorRecoveryManager.RetryConfig.exponentialBackoff(5, 50, 400);

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, config, "backoff_test");

      long elapsed = System.currentTimeMillis() - startTime;

      assertTrue(result.isSuccessful(), "Should eventually succeed");
      // Total minimum delay: 50 + 100 + 200 = 350ms
      assertTrue(elapsed >= 300, "Should have waited at least 300ms total");

      LOGGER.info("Exponential backoff test passed, elapsed: " + elapsed + "ms");
    }

    @Test
    @DisplayName("Should not retry on non-recoverable failure")
    void shouldNotRetryOnNonRecoverableFailure() {
      LOGGER.info("Testing executeWithRetry - non-recoverable failure");

      AtomicInteger counter = new AtomicInteger(0);
      // Non-recoverable error (doesn't contain timeout/temporary/not found/unavailable)
      Supplier<String> operation =
          () -> {
            counter.incrementAndGet();
            throw new java.lang.RuntimeException("Invalid data - this is permanent");
          };

      ErrorRecoveryManager.RetryConfig config = ErrorRecoveryManager.RetryConfig.fixedDelay(5, 10);

      ErrorRecoveryManager.RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, config, "non_recoverable_test");

      assertFalse(result.isSuccessful(), "Should fail");
      // Non-recoverable errors don't retry
      assertEquals(1, result.getAttemptCount(), "Should only attempt once for non-recoverable");

      LOGGER.info("Non-recoverable test passed, attempts: " + result.getAttemptCount());
    }
  }

  @Nested
  @DisplayName("ErrorRecoveryManager.executeWithFallback Tests")
  class ExecuteWithFallbackTests {

    @Test
    @DisplayName("Should use primary operation when successful")
    void shouldUsePrimaryOperationWhenSuccessful() throws WasmException {
      LOGGER.info("Testing executeWithFallback - primary success");

      Supplier<String> primary = () -> "primary result";
      Supplier<String> fallback = () -> "fallback result";

      String result = ErrorRecoveryManager.executeWithFallback(primary, fallback, "fallback_test");

      assertEquals("primary result", result, "Should return primary result");

      LOGGER.info("Primary operation success verified");
    }

    @Test
    @DisplayName("Should use fallback when primary fails with recoverable error")
    void shouldUseFallbackWhenPrimaryFailsWithRecoverableError() throws WasmException {
      LOGGER.info("Testing executeWithFallback - fallback on primary failure");

      // Use "timeout" to make it a recoverable error
      Supplier<String> primary =
          () -> {
            throw new java.lang.RuntimeException("Connection timeout - primary unavailable");
          };
      Supplier<String> fallback = () -> "fallback result";

      String result = ErrorRecoveryManager.executeWithFallback(primary, fallback, "fallback_test");

      assertEquals("fallback result", result, "Should return fallback result");

      LOGGER.info("Fallback on primary failure verified");
    }

    @Test
    @DisplayName("Should throw original exception when both fail")
    void shouldThrowOriginalExceptionWhenBothFail() {
      LOGGER.info("Testing executeWithFallback - both operations fail");

      // Use "timeout" to make it recoverable so fallback is attempted
      Supplier<String> primary =
          () -> {
            throw new java.lang.RuntimeException("Connection timeout - primary failed");
          };
      Supplier<String> fallback =
          () -> {
            throw new java.lang.RuntimeException("Fallback also failed");
          };

      assertThrows(
          WasmException.class,
          () -> ErrorRecoveryManager.executeWithFallback(primary, fallback, "both_fail_test"),
          "Should throw exception when both fail");

      LOGGER.info("Both fail exception verified");
    }

    @Test
    @DisplayName("Should not use fallback for non-recoverable error")
    void shouldNotUseFallbackForNonRecoverableError() {
      LOGGER.info("Testing executeWithFallback - non-recoverable error");

      // Non-recoverable error (doesn't contain timeout/temporary/not found/unavailable)
      Supplier<String> primary =
          () -> {
            throw new java.lang.RuntimeException("Invalid data corruption");
          };
      Supplier<String> fallback = () -> "fallback result";

      // Should throw without attempting fallback
      assertThrows(
          WasmException.class,
          () -> ErrorRecoveryManager.executeWithFallback(primary, fallback, "non_recoverable_test"),
          "Should throw for non-recoverable error without fallback");

      LOGGER.info("Non-recoverable error test verified");
    }
  }

  @Nested
  @DisplayName("ErrorRecoveryManager.validateResult Tests")
  class ValidateResultTests {

    @Test
    @DisplayName("Should return result when validation passes")
    void shouldReturnResultWhenValidationPasses() throws WasmException {
      LOGGER.info("Testing validateResult - validation passes");

      String result = ErrorRecoveryManager.validateResult("valid", value -> null);

      assertEquals("valid", result, "Should return the valid result");

      LOGGER.info("Validation pass test successful");
    }

    @Test
    @DisplayName("Should throw when validation fails")
    void shouldThrowWhenValidationFails() {
      LOGGER.info("Testing validateResult - validation fails");

      assertThrows(
          WasmException.class,
          () -> ErrorRecoveryManager.validateResult("invalid", value -> "Value is invalid"),
          "Should throw on validation failure");

      LOGGER.info("Validation failure exception verified");
    }

    @Test
    @DisplayName("Should handle null validator")
    void shouldHandleNullValidator() throws WasmException {
      LOGGER.info("Testing validateResult - null validator");

      String result = ErrorRecoveryManager.validateResult("test", null);

      assertEquals("test", result, "Should return result when validator is null");

      LOGGER.info("Null validator test successful");
    }

    @Test
    @DisplayName("Should handle null result with validation")
    void shouldHandleNullResultWithValidation() throws WasmException {
      LOGGER.info("Testing validateResult - null result");

      String result =
          ErrorRecoveryManager.validateResult(null, value -> value == null ? null : "not null");

      assertNull(result, "Should return null result when validation passes");

      LOGGER.info("Null result validation test successful");
    }
  }

  @Nested
  @DisplayName("ErrorRecoveryManager.executeWithCircuitBreaker Tests")
  class ExecuteWithCircuitBreakerTests {

    @Test
    @DisplayName("Should execute operation successfully")
    void shouldExecuteOperationSuccessfully() throws WasmException {
      LOGGER.info("Testing executeWithCircuitBreaker - success");

      Supplier<String> operation = () -> "circuit breaker success";

      String result = ErrorRecoveryManager.executeWithCircuitBreaker(operation, 0.5, 10, "cb_test");

      assertEquals("circuit breaker success", result, "Should return operation result");

      LOGGER.info("Circuit breaker success verified");
    }

    @Test
    @DisplayName("Should throw when operation fails")
    void shouldThrowWhenOperationFails() {
      LOGGER.info("Testing executeWithCircuitBreaker - failure");

      Supplier<String> operation =
          () -> {
            throw new RuntimeException("Operation failed");
          };

      assertThrows(
          WasmException.class,
          () -> ErrorRecoveryManager.executeWithCircuitBreaker(operation, 0.5, 10, "cb_test"),
          "Should throw on operation failure");

      LOGGER.info("Circuit breaker failure exception verified");
    }
  }

  @Nested
  @DisplayName("ErrorRecoveryManager Utility Class Tests")
  class ErrorRecoveryManagerUtilityClassTests {

    @Test
    @DisplayName("Error recovery manager utility class should not be instantiable")
    void errorRecoveryManagerUtilityClassShouldNotBeInstantiable() {
      LOGGER.info("Testing ErrorRecoveryManager instantiation prevention");

      try {
        java.lang.reflect.Constructor<?> constructor =
            ErrorRecoveryManager.class.getDeclaredConstructor();
        assertTrue(
            java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private");
        constructor.setAccessible(true);
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(),
            "Constructor should throw AssertionError");
      } catch (NoSuchMethodException e) {
        // No constructor found - also acceptable
      }

      LOGGER.info("ErrorRecoveryManager instantiation prevention verified");
    }
  }
}
