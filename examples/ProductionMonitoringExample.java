package ai.tegmentum.wasmtime4j.examples;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.util.HealthCheck;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Production monitoring example demonstrating comprehensive observability patterns for Wasmtime4j.
 *
 * <p>This example shows:
 * - Performance metrics collection
 * - Health checking and alerting
 * - Resource monitoring and management
 * - Structured logging for observability
 * - Circuit breaker pattern for resilience
 * - Rate limiting and throttling
 */
public final class ProductionMonitoringExample {

    private static final Logger LOGGER = Logger.getLogger(ProductionMonitoringExample.class.getName());

    /** Metrics collection for monitoring. */
    private static final class Metrics {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong moduleCompilations = new AtomicLong(0);
        private final AtomicLong compilationTime = new AtomicLong(0);
        private final Map<String, AtomicLong> functionCalls = new ConcurrentHashMap<>();

        public void recordRequest(final long executionTimeNanos) {
            requestCount.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeNanos);
        }

        public void recordError() {
            errorCount.incrementAndGet();
        }

        public void recordCompilation(final long compilationTimeNanos) {
            moduleCompilations.incrementAndGet();
            compilationTime.addAndGet(compilationTimeNanos);
        }

        public void recordFunctionCall(final String functionName) {
            functionCalls.computeIfAbsent(functionName, k -> new AtomicLong(0)).incrementAndGet();
        }

        public void printSummary() {
            final long requests = requestCount.get();
            final long errors = errorCount.get();
            final long avgExecutionTimeNanos = requests > 0 ? totalExecutionTime.get() / requests : 0;
            final long compilations = moduleCompilations.get();
            final long avgCompilationTimeNanos = compilations > 0 ? compilationTime.get() / compilations : 0;

            System.out.println("\n=== Performance Metrics ===");
            System.out.printf("Total Requests: %d%n", requests);
            System.out.printf("Total Errors: %d%n", errors);
            System.out.printf("Error Rate: %.2f%%%n", requests > 0 ? (errors * 100.0 / requests) : 0.0);
            System.out.printf("Average Execution Time: %.3f ms%n", avgExecutionTimeNanos / 1_000_000.0);
            System.out.printf("Module Compilations: %d%n", compilations);
            System.out.printf("Average Compilation Time: %.3f ms%n", avgCompilationTimeNanos / 1_000_000.0);

            if (!functionCalls.isEmpty()) {
                System.out.println("\nFunction Call Statistics:");
                functionCalls.forEach((name, count) ->
                    System.out.printf("  %s: %d calls%n", name, count.get())
                );
            }

            // Memory statistics
            final Runtime runtime = Runtime.getRuntime();
            final long totalMemory = runtime.totalMemory();
            final long freeMemory = runtime.freeMemory();
            final long usedMemory = totalMemory - freeMemory;
            final long maxMemory = runtime.maxMemory();

            System.out.println("\nMemory Statistics:");
            System.out.printf("  Used Memory: %.2f MB%n", usedMemory / 1024.0 / 1024.0);
            System.out.printf("  Total Memory: %.2f MB%n", totalMemory / 1024.0 / 1024.0);
            System.out.printf("  Max Memory: %.2f MB%n", maxMemory / 1024.0 / 1024.0);
            System.out.printf("  Memory Usage: %.2f%%%n", (usedMemory * 100.0 / maxMemory));
        }
    }

    /** Circuit breaker for handling failures gracefully. */
    private static final class CircuitBreaker {
        private enum State { CLOSED, OPEN, HALF_OPEN }

        private State state = State.CLOSED;
        private int failureCount = 0;
        private Instant lastFailureTime = Instant.now();
        private final int failureThreshold;
        private final Duration timeout;

        public CircuitBreaker(final int failureThreshold, final Duration timeout) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
        }

        public boolean canExecute() {
            if (state == State.CLOSED) {
                return true;
            }

            if (state == State.OPEN) {
                if (Duration.between(lastFailureTime, Instant.now()).compareTo(timeout) > 0) {
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            }

            // HALF_OPEN state
            return true;
        }

        public void recordSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }

        public void recordFailure() {
            failureCount++;
            lastFailureTime = Instant.now();

            if (state == State.HALF_OPEN || failureCount >= failureThreshold) {
                state = State.OPEN;
                LOGGER.warning("Circuit breaker opened due to failures");
            }
        }

        public State getState() {
            return state;
        }
    }

    /** Rate limiter for controlling request flow. */
    private static final class RateLimiter {
        private final long maxRequestsPerSecond;
        private long lastRefillTime;
        private long availableTokens;

        public RateLimiter(final long maxRequestsPerSecond) {
            this.maxRequestsPerSecond = maxRequestsPerSecond;
            this.lastRefillTime = System.nanoTime();
            this.availableTokens = maxRequestsPerSecond;
        }

        public synchronized boolean tryAcquire() {
            refillTokens();

            if (availableTokens > 0) {
                availableTokens--;
                return true;
            }

            return false;
        }

        private void refillTokens() {
            final long now = System.nanoTime();
            final long timePassed = now - lastRefillTime;
            final long tokensToAdd = (timePassed / 1_000_000_000L) * maxRequestsPerSecond;

            if (tokensToAdd > 0) {
                availableTokens = Math.min(maxRequestsPerSecond, availableTokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }

    /** Production service wrapping WebAssembly execution with monitoring. */
    private static final class MonitoredWasmService implements AutoCloseable {
        private final WasmRuntime runtime;
        private final Engine engine;
        private final Module calculatorModule;
        private final Metrics metrics;
        private final CircuitBreaker circuitBreaker;
        private final RateLimiter rateLimiter;
        private final ScheduledExecutorService scheduler;

        /** Simple calculator WebAssembly module for demonstration. */
        private static final byte[] CALCULATOR_WASM = {
            0x00, 0x61, 0x73, 0x6d, // WASM magic number
            0x01, 0x00, 0x00, 0x00, // WASM version
            0x01, 0x07,             // Type section
            0x01,                   // 1 type
            0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // (i32, i32) -> i32
            0x03, 0x02,             // Function section
            0x01, 0x00,             // 1 function, type 0
            0x07, 0x0b,             // Export section
            0x01, 0x07, 0x6d, 0x75, 0x6c, 0x74, 0x69, 0x70, 0x6c, 0x79, 0x00, 0x00, // export "multiply"
            0x0a, 0x09,             // Code section
            0x01, 0x07, 0x00,       // 1 function, 7 bytes, 0 locals
            0x20, 0x00,             // local.get 0
            0x20, 0x01,             // local.get 1
            0x6c,                   // i32.mul
            0x0b                    // end
        };

        public MonitoredWasmService() throws Exception {
            this.metrics = new Metrics();
            this.circuitBreaker = new CircuitBreaker(5, Duration.ofSeconds(30));
            this.rateLimiter = new RateLimiter(100); // 100 requests per second
            this.scheduler = Executors.newScheduledThreadPool(2);

            // Initialize WebAssembly runtime
            this.runtime = WasmRuntimeFactory.create();
            this.engine = runtime.createEngine();

            // Compile module with timing
            final long startTime = System.nanoTime();
            this.calculatorModule = engine.compileModule(CALCULATOR_WASM);
            final long compilationTime = System.nanoTime() - startTime;
            metrics.recordCompilation(compilationTime);

            // Start background monitoring
            startBackgroundMonitoring();

            LOGGER.info("MonitoredWasmService initialized successfully");
        }

        /**
         * Executes a monitored WebAssembly function call.
         *
         * @param a first parameter
         * @param b second parameter
         * @return multiplication result
         * @throws Exception if execution fails
         */
        public int multiply(final int a, final int b) throws Exception {
            // Rate limiting
            if (!rateLimiter.tryAcquire()) {
                throw new Exception("Rate limit exceeded");
            }

            // Circuit breaker check
            if (!circuitBreaker.canExecute()) {
                throw new Exception("Service unavailable (circuit breaker open)");
            }

            final long startTime = System.nanoTime();
            try {
                try (Store store = engine.createStore()) {
                    try (Instance instance = calculatorModule.instantiate(store)) {
                        final WasmFunction multiplyFunc = instance.getFunction("multiply")
                            .orElseThrow(() -> new Exception("Function 'multiply' not found"));

                        metrics.recordFunctionCall("multiply");

                        final WasmValue[] result = multiplyFunc.call(
                            WasmValue.i32(a),
                            WasmValue.i32(b)
                        );

                        final int value = result[0].asInt();

                        // Record successful execution
                        final long executionTime = System.nanoTime() - startTime;
                        metrics.recordRequest(executionTime);
                        circuitBreaker.recordSuccess();

                        LOGGER.fine(String.format("multiply(%d, %d) = %d (%.3f ms)",
                            a, b, value, executionTime / 1_000_000.0));

                        return value;
                    }
                }
            } catch (final Exception e) {
                metrics.recordError();
                circuitBreaker.recordFailure();
                LOGGER.log(Level.WARNING, "Function execution failed", e);
                throw e;
            }
        }

        /**
         * Starts background monitoring tasks.
         */
        private void startBackgroundMonitoring() {
            // Health check every 30 seconds
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    final boolean healthy = HealthCheck.isReady();
                    if (!healthy) {
                        LOGGER.warning("Health check failed - service may be degraded");
                    } else {
                        LOGGER.fine("Health check passed");
                    }
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Health check execution failed", e);
                }
            }, 0, 30, TimeUnit.SECONDS);

            // Metrics reporting every 60 seconds
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    LOGGER.info("Periodic metrics report:");
                    metrics.printSummary();

                    // Circuit breaker status
                    LOGGER.info("Circuit breaker state: " + circuitBreaker.getState());
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Metrics reporting failed", e);
                }
            }, 60, 60, TimeUnit.SECONDS);

            // Memory monitoring every 15 seconds
            scheduler.scheduleAtFixedRate(() -> {
                final Runtime javaRuntime = Runtime.getRuntime();
                final long usedMemory = javaRuntime.totalMemory() - javaRuntime.freeMemory();
                final long maxMemory = javaRuntime.maxMemory();
                final double memoryUsagePercent = (usedMemory * 100.0 / maxMemory);

                if (memoryUsagePercent > 80.0) {
                    LOGGER.warning(String.format("High memory usage: %.2f%%", memoryUsagePercent));

                    // Trigger garbage collection if memory usage is very high
                    if (memoryUsagePercent > 90.0) {
                        LOGGER.info("Triggering garbage collection due to high memory usage");
                        System.gc();
                    }
                }
            }, 0, 15, TimeUnit.SECONDS);
        }

        /**
         * Gets current service metrics.
         *
         * @return metrics snapshot
         */
        public Metrics getMetrics() {
            return metrics;
        }

        /**
         * Gets current circuit breaker state.
         *
         * @return circuit breaker state
         */
        public CircuitBreaker.State getCircuitBreakerState() {
            return circuitBreaker.getState();
        }

        @Override
        public void close() throws Exception {
            LOGGER.info("Shutting down MonitoredWasmService");

            // Shutdown background tasks
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (final InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Close WebAssembly resources
            if (calculatorModule != null) {
                calculatorModule.close();
            }
            if (engine != null) {
                engine.close();
            }
            if (runtime != null) {
                runtime.close();
            }

            LOGGER.info("MonitoredWasmService shutdown complete");
        }
    }

    /** Private constructor to prevent instantiation. */
    private ProductionMonitoringExample() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Main entry point for the production monitoring example.
     *
     * @param args command line arguments (not used)
     */
    public static void main(final String[] args) {
        System.out.println("Production Monitoring Example for Wasmtime4j");
        System.out.println("=============================================");

        try (MonitoredWasmService service = new MonitoredWasmService()) {

            System.out.println("\nStarting monitored WebAssembly service...");
            System.out.println("The service will run for 3 minutes demonstrating monitoring capabilities.");

            // Simulate production workload
            simulateWorkload(service);

            // Final metrics report
            System.out.println("\n=== Final Metrics Report ===");
            service.getMetrics().printSummary();
            System.out.println("\nCircuit Breaker State: " + service.getCircuitBreakerState());

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Production monitoring example failed", e);
            System.exit(1);
        }

        System.out.println("\nProduction monitoring example completed successfully!");
    }

    /**
     * Simulates a production workload with varying patterns.
     *
     * @param service the monitored service
     */
    private static void simulateWorkload(final MonitoredWasmService service) {
        final int durationMinutes = 3;
        final long endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000);

        int requestCount = 0;
        while (System.currentTimeMillis() < endTime) {
            try {
                // Normal operations
                for (int i = 0; i < 10; i++) {
                    final int a = (int) (Math.random() * 100);
                    final int b = (int) (Math.random() * 100);
                    final int result = service.multiply(a, b);

                    if (++requestCount % 100 == 0) {
                        System.out.printf("Processed %d requests (latest: %d * %d = %d)%n",
                            requestCount, a, b, result);
                    }
                }

                // Simulate occasional bursts (might trigger rate limiting)
                if (Math.random() < 0.1) {
                    System.out.println("Simulating burst traffic...");
                    for (int i = 0; i < 50; i++) {
                        try {
                            service.multiply(10, 20);
                        } catch (final Exception e) {
                            // Expected during rate limiting
                            if (e.getMessage().contains("Rate limit")) {
                                System.out.println("Rate limiting activated");
                                break;
                            }
                        }
                    }
                }

                // Brief pause between batches
                Thread.sleep(100);

            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error during workload simulation", e);
            }
        }
    }
}