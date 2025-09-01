package ai.tegmentum.wasmtime4j.instance;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive test suite for Instance concurrency patterns, thread safety validation, and
 * concurrent execution scenarios. Tests instance behavior under various concurrent access patterns,
 * ensuring thread safety and consistent behavior across different threading models.
 */
@DisplayName("Instance Concurrency Tests")
final class InstanceConcurrencyTest {

  private static final Logger LOGGER = Logger.getLogger(InstanceConcurrencyTest.class.getName());

  private final Map<String, Object> testMetrics = new HashMap<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    TestUtils.skipIfCategoryNotEnabled(TestCategories.INSTANCE);
    testMetrics.clear();
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
  }

  /**
   * Execute test with both JNI and Panama runtimes if available.
   *
   * @param testAction The test action to execute with each runtime
   */
  private void runWithBothRuntimes(final RuntimeTestAction testAction) {
    final List<RuntimeType> availableRuntimes = WasmRuntimeFactory.getAvailableRuntimes();

    for (final RuntimeType runtimeType : availableRuntimes) {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        LOGGER.info("Testing with runtime: " + runtimeType);
        testAction.execute(runtime, runtimeType);
      } catch (final Exception e) {
        throw new RuntimeException("Test failed with runtime " + runtimeType, e);
      }
    }
  }

  /**
   * Add a test metric for tracking and analysis.
   *
   * @param message The metric message
   */
  private void addTestMetric(final String message) {
    testMetrics.put(Instant.now().toString(), message);
    LOGGER.info("Test metric: " + message);
  }

  /** Functional interface for runtime-specific test actions. */
  @FunctionalInterface
  private interface RuntimeTestAction {
    void execute(WasmRuntime runtime, RuntimeType runtimeType) throws Exception;
  }

  @Nested
  @DisplayName("Basic Concurrent Function Call Tests")
  final class BasicConcurrentFunctionCallTests {

    @Test
    @DisplayName("Should handle high-frequency concurrent function calls")
    void shouldHandleHighFrequencyConcurrentFunctionCalls() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numThreads = 20;
              final int callsPerThread = 500;
              final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
              final AtomicInteger successCount = new AtomicInteger(0);
              final AtomicInteger errorCount = new AtomicInteger(0);
              final List<Duration> callTimes = Collections.synchronizedList(new ArrayList<>());

              try {
                final CountDownLatch startLatch = new CountDownLatch(1);
                final CountDownLatch completionLatch = new CountDownLatch(numThreads);

                for (int i = 0; i < numThreads; i++) {
                  final int threadId = i;
                  executor.submit(
                      () -> {
                        try {
                          // Wait for all threads to be ready
                          startLatch.await();

                          for (int j = 0; j < callsPerThread; j++) {
                            final Instant callStart = Instant.now();
                            try {
                              final WasmValue[] result =
                                  instance.callFunction(
                                      "add", WasmValue.i32(threadId), WasmValue.i32(j));
                              final Duration callTime = Duration.between(callStart, Instant.now());
                              callTimes.add(callTime);

                              if (result[0].asI32() == threadId + j) {
                                successCount.incrementAndGet();
                              } else {
                                errorCount.incrementAndGet();
                              }
                            } catch (final Exception e) {
                              errorCount.incrementAndGet();
                              LOGGER.warning(
                                  String.format(
                                      "Thread %d call %d failed: %s", threadId, j, e.getMessage()));
                            }
                          }
                        } catch (final InterruptedException e) {
                          Thread.currentThread().interrupt();
                          errorCount.incrementAndGet();
                        } finally {
                          completionLatch.countDown();
                        }
                      });
                }

                // Start all threads simultaneously
                final Instant testStart = Instant.now();
                startLatch.countDown();

                // Wait for completion
                assertThat(completionLatch.await(60, TimeUnit.SECONDS)).isTrue();
                final Duration totalTime = Duration.between(testStart, Instant.now());

                // Validate results
                final int totalCalls = numThreads * callsPerThread;
                assertThat(successCount.get()).isEqualTo(totalCalls);
                assertThat(errorCount.get()).isEqualTo(0);

                // Calculate performance metrics
                final double callsPerSecond = totalCalls / (totalTime.toMillis() / 1000.0);
                final double avgCallTimeMs =
                    callTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                        / 1_000_000.0;

                assertThat(callsPerSecond).isGreaterThan(1000);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "High-frequency calls: %d successful, %.0f calls/sec with %s",
                      successCount.get(),
                      successCount.get() / (System.currentTimeMillis() / 1000.0),
                      runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should maintain thread safety with simultaneous function calls")
    void shouldMaintainThreadSafetyWithSimultaneousFunctionCalls() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_mutable");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numThreads = 10;
              final int operationsPerThread = 50;
              final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
              final CyclicBarrier barrier = new CyclicBarrier(numThreads);
              final AtomicInteger consistencyErrors = new AtomicInteger(0);
              final AtomicInteger totalOperations = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                  final int threadId = i;
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            try {
                              // Wait for all threads to be ready
                              barrier.await(10, TimeUnit.SECONDS);

                              for (int j = 0; j < operationsPerThread; j++) {
                                final int setValue = threadId * 1000 + j;

                                try {
                                  // Set the global value
                                  instance.callFunction("set", WasmValue.i32(setValue));

                                  // Immediately read it back
                                  final WasmValue[] result = instance.callFunction("get");
                                  final int readValue = result[0].asI32();

                                  // The value might have been changed by another thread,
                                  // but it should be a valid value from some thread
                                  final boolean isValidValue =
                                      isValidThreadValue(
                                          readValue, numThreads, operationsPerThread);
                                  if (!isValidValue) {
                                    consistencyErrors.incrementAndGet();
                                    LOGGER.warning(
                                        String.format(
                                            "Invalid value read: %d (set %d)",
                                            readValue, setValue));
                                  }

                                  totalOperations.incrementAndGet();

                                } catch (final Exception e) {
                                  consistencyErrors.incrementAndGet();
                                  LOGGER.warning(
                                      String.format(
                                          "Thread %d operation %d failed: %s",
                                          threadId, j, e.getMessage()));
                                }
                              }
                            } catch (final Exception e) {
                              consistencyErrors.incrementAndGet();
                              LOGGER.warning(
                                  "Thread "
                                      + threadId
                                      + " barrier/execution failed: "
                                      + e.getMessage());
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);

                // Should have completed all operations with minimal errors
                assertThat(totalOperations.get()).isEqualTo(numThreads * operationsPerThread);

                // Allow for some race conditions but should be mostly consistent
                final double errorRate = (double) consistencyErrors.get() / totalOperations.get();
                assertThat(errorRate).isLessThan(0.1); // Less than 10% error rate

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Thread safety: %d operations, %d consistency errors with %s",
                      totalOperations.get(), consistencyErrors.get(), runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should handle concurrent instance creation and usage")
    void shouldHandleConcurrentInstanceCreationAndUsage() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            final int numThreads = 15;
            final int operationsPerInstance = 20;
            final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            final AtomicInteger successfulInstances = new AtomicInteger(0);
            final AtomicInteger failedOperations = new AtomicInteger(0);

            try (final Engine engine = runtime.createEngine()) {
              final Module module = engine.compileModule(moduleBytes);

              final List<CompletableFuture<Void>> futures = new ArrayList<>();
              final CountDownLatch startLatch = new CountDownLatch(1);

              for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                final CompletableFuture<Void> future =
                    CompletableFuture.runAsync(
                        () -> {
                          try {
                            startLatch.await();

                            try (final Store store = engine.createStore();
                                final Instance instance = module.instantiate(store)) {

                              // Verify instance is functional
                              for (int j = 0; j < operationsPerInstance; j++) {
                                final WasmValue[] result =
                                    instance.callFunction(
                                        "add", WasmValue.i32(threadId), WasmValue.i32(j));

                                if (result[0].asI32() != threadId + j) {
                                  failedOperations.incrementAndGet();
                                }
                              }

                              successfulInstances.incrementAndGet();

                            } catch (final Exception e) {
                              failedOperations.incrementAndGet();
                              LOGGER.warning(
                                  "Thread "
                                      + threadId
                                      + " instance creation/usage failed: "
                                      + e.getMessage());
                            }
                          } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            failedOperations.incrementAndGet();
                          }
                        },
                        executor);
                futures.add(future);
              }

              // Start all threads simultaneously
              startLatch.countDown();

              // Wait for all threads to complete
              CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                  .get(45, TimeUnit.SECONDS);

              // All instances should be successfully created and used
              assertThat(successfulInstances.get()).isEqualTo(numThreads);
              assertThat(failedOperations.get()).isEqualTo(0);

            } finally {
              executor.shutdownNow();
              executor.awaitTermination(10, TimeUnit.SECONDS);
            }

            addTestMetric(
                String.format(
                    "Concurrent instances: %d successful, %d failures with %s",
                    successfulInstances.get(), failedOperations.get(), runtimeType));
          });
    }

    /** Check if a value could have been set by any of the threads. */
    private boolean isValidThreadValue(
        final int value, final int numThreads, final int operationsPerThread) {
      for (int threadId = 0; threadId < numThreads; threadId++) {
        for (int operation = 0; operation < operationsPerThread; operation++) {
          if (value == threadId * 1000 + operation) {
            return true;
          }
        }
      }
      return false;
    }
  }

  @Nested
  @DisplayName("Concurrent Memory Operations Tests")
  final class ConcurrentMemoryOperationTests {

    @Test
    @DisplayName("Should handle concurrent memory access patterns")
    void shouldHandleConcurrentMemoryAccessPatterns() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numThreads = 8;
              final int accessesPerThread = 100;
              final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
              final AtomicInteger successfulAccesses = new AtomicInteger(0);
              final AtomicInteger boundaryErrors = new AtomicInteger(0);
              final AtomicInteger otherErrors = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                  final int threadId = i;
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < accessesPerThread; j++) {
                              try {
                                // Use thread-specific memory regions to avoid interference
                                final int baseOffset = threadId * 1000;
                                final int offset = baseOffset + (j * 4);
                                final int value = threadId * 10000 + j;

                                // Store value
                                instance.callFunction(
                                    "store", WasmValue.i32(offset), WasmValue.i32(value));

                                // Load and verify
                                final WasmValue[] result =
                                    instance.callFunction("load", WasmValue.i32(offset));
                                if (result[0].asI32() == value) {
                                  successfulAccesses.incrementAndGet();
                                } else {
                                  otherErrors.incrementAndGet();
                                }

                              } catch (final WasmException e) {
                                if (e.getMessage().contains("out of bounds")
                                    || e.getMessage().contains("bounds")) {
                                  boundaryErrors.incrementAndGet();
                                } else {
                                  otherErrors.incrementAndGet();
                                }
                              } catch (final Exception e) {
                                otherErrors.incrementAndGet();
                                LOGGER.warning(
                                    String.format(
                                        "Thread %d access %d unexpected error: %s",
                                        threadId, j, e.getMessage()));
                              }
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);

                // Should have some successful accesses
                assertThat(successfulAccesses.get()).isGreaterThan(0);

                // Boundary errors are acceptable, other errors are not
                assertThat(otherErrors.get()).isEqualTo(0);

                final int totalOperations =
                    successfulAccesses.get() + boundaryErrors.get() + otherErrors.get();
                assertThat(totalOperations).isEqualTo(numThreads * accessesPerThread);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Concurrent memory: %d successful, %d boundary errors, %d other errors with"
                          + " %s",
                      successfulAccesses.get(),
                      boundaryErrors.get(),
                      otherErrors.get(),
                      runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should maintain data integrity under concurrent memory operations")
    void shouldMaintainDataIntegrityUnderConcurrentMemoryOperations() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Initialize known data patterns
              final Map<Integer, Integer> expectedData = new HashMap<>();
              for (int i = 0; i < 50; i++) {
                final int offset = i * 8; // Use 8-byte spacing to minimize overlap
                final int value = i * 123; // Distinctive values
                expectedData.put(offset, value);

                try {
                  instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(value));
                } catch (final WasmException e) {
                  expectedData.remove(offset);
                  break; // Memory boundary reached
                }
              }

              if (expectedData.isEmpty()) {
                addTestMetric("No accessible memory for integrity test with " + runtimeType);
                return;
              }

              // Concurrent readers to verify data integrity
              final int numReaders = 6;
              final ExecutorService executor = Executors.newFixedThreadPool(numReaders);
              final AtomicInteger integrityViolations = new AtomicInteger(0);
              final AtomicInteger totalReads = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numReaders; i++) {
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < 200; j++) {
                              for (final Map.Entry<Integer, Integer> entry :
                                  expectedData.entrySet()) {
                                try {
                                  final int offset = entry.getKey();
                                  final int expectedValue = entry.getValue();

                                  final WasmValue[] result =
                                      instance.callFunction("load", WasmValue.i32(offset));
                                  totalReads.incrementAndGet();

                                  if (result[0].asI32() != expectedValue) {
                                    integrityViolations.incrementAndGet();
                                    LOGGER.warning(
                                        String.format(
                                            "Data integrity violation at offset %d: expected %d,"
                                                + " got %d",
                                            offset, expectedValue, result[0].asI32()));
                                  }
                                } catch (final Exception e) {
                                  // Any exception during reading is an integrity violation
                                  integrityViolations.incrementAndGet();
                                }
                              }
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all readers to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(20, TimeUnit.SECONDS);

                // Should have no integrity violations
                assertThat(integrityViolations.get()).isEqualTo(0);
                assertThat(totalReads.get()).isGreaterThan(0);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Data integrity: %d reads, %d violations with %s",
                      totalReads.get(), integrityViolations.get(), runtimeType));
            }
          });
    }
  }

  @Nested
  @DisplayName("Concurrent Export Access Tests")
  final class ConcurrentExportAccessTests {

    @Test
    @DisplayName("Should handle concurrent export enumeration and access")
    void shouldHandleConcurrentExportEnumerationAndAccess() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numThreads = 12;
              final int accessesPerThread = 100;
              final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
              final AtomicInteger successfulEnumerations = new AtomicInteger(0);
              final AtomicInteger successfulAccesses = new AtomicInteger(0);
              final AtomicInteger errors = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < accessesPerThread; j++) {
                              try {
                                // Enumerate exports
                                final String[] exports = instance.getExportNames();
                                if (exports.length > 0) {
                                  successfulEnumerations.incrementAndGet();
                                }

                                // Access each export
                                for (final String exportName : exports) {
                                  if (instance.getFunction(exportName).isPresent()) {
                                    successfulAccesses.incrementAndGet();
                                  }
                                }

                              } catch (final Exception e) {
                                errors.incrementAndGet();
                                LOGGER.warning(
                                    "Concurrent export access failed: " + e.getMessage());
                              }
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(25, TimeUnit.SECONDS);

                // Should have successful operations
                assertThat(successfulEnumerations.get()).isGreaterThan(0);
                assertThat(successfulAccesses.get()).isGreaterThan(0);
                assertThat(errors.get()).isEqualTo(0);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Concurrent exports: %d enumerations, %d accesses, %d errors with %s",
                      successfulEnumerations.get(),
                      successfulAccesses.get(),
                      errors.get(),
                      runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should provide consistent export information under concurrent access")
    void shouldProvideConsistentExportInformationUnderConcurrentAccess() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Get reference export information
              final String[] referenceExports = instance.getExportNames();
              final Map<String, String> referenceSignatures = new HashMap<>();

              for (final String exportName : referenceExports) {
                instance
                    .getFunction(exportName)
                    .ifPresent(
                        func -> {
                          final var funcType = func.getFunctionType();
                          final String signature =
                              java.util.Arrays.toString(funcType.getParamTypes())
                                  + " -> "
                                  + java.util.Arrays.toString(funcType.getReturnTypes());
                          referenceSignatures.put(exportName, signature);
                        });
              }

              final int numThreads = 10;
              final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
              final AtomicInteger consistencyErrors = new AtomicInteger(0);
              final AtomicInteger totalChecks = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < 50; j++) {
                              try {
                                // Check export names consistency
                                final String[] exports = instance.getExportNames();
                                if (!java.util.Arrays.equals(exports, referenceExports)) {
                                  consistencyErrors.incrementAndGet();
                                }
                                totalChecks.incrementAndGet();

                                // Check function signature consistency
                                for (final String exportName : exports) {
                                  instance
                                      .getFunction(exportName)
                                      .ifPresent(
                                          func -> {
                                            final var funcType = func.getFunctionType();
                                            final String signature =
                                                java.util.Arrays.toString(funcType.getParamTypes())
                                                    + " -> "
                                                    + java.util.Arrays.toString(
                                                        funcType.getReturnTypes());

                                            if (!signature.equals(
                                                referenceSignatures.get(exportName))) {
                                              consistencyErrors.incrementAndGet();
                                            }
                                            totalChecks.incrementAndGet();
                                          });
                                }

                              } catch (final Exception e) {
                                consistencyErrors.incrementAndGet();
                                LOGGER.warning(
                                    "Export consistency check failed: " + e.getMessage());
                              }
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(20, TimeUnit.SECONDS);

                // Should have no consistency errors
                assertThat(consistencyErrors.get()).isEqualTo(0);
                assertThat(totalChecks.get()).isGreaterThan(0);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Export consistency: %d checks, %d errors with %s",
                      totalChecks.get(), consistencyErrors.get(), runtimeType));
            }
          });
    }
  }

  @Nested
  @DisplayName("Resource Contention and Cleanup Tests")
  final class ResourceContentionTests {

    @Test
    @DisplayName("Should handle resource contention during instance lifecycle")
    void shouldHandleResourceContentionDuringInstanceLifecycle() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            final int numThreads = 10;
            final int instancesPerThread = 5;
            final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            final AtomicInteger successfulInstances = new AtomicInteger(0);
            final AtomicInteger failedInstances = new AtomicInteger(0);

            try (final Engine engine = runtime.createEngine()) {
              final Module module = engine.compileModule(moduleBytes);

              final List<CompletableFuture<Void>> futures = new ArrayList<>();

              for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                final CompletableFuture<Void> future =
                    CompletableFuture.runAsync(
                        () -> {
                          for (int j = 0; j < instancesPerThread; j++) {
                            try (final Store store = engine.createStore()) {

                              // Create instance
                              final Instance instance = module.instantiate(store);

                              // Use instance briefly
                              final WasmValue[] result =
                                  instance.callFunction(
                                      "add", WasmValue.i32(threadId), WasmValue.i32(j));

                              if (result[0].asI32() == threadId + j) {
                                successfulInstances.incrementAndGet();
                              } else {
                                failedInstances.incrementAndGet();
                              }

                              // Close instance
                              instance.close();

                            } catch (final Exception e) {
                              failedInstances.incrementAndGet();
                              LOGGER.warning(
                                  String.format(
                                      "Thread %d instance %d failed: %s",
                                      threadId, j, e.getMessage()));
                            }
                          }
                        },
                        executor);
                futures.add(future);
              }

              // Wait for all threads to complete
              CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                  .get(30, TimeUnit.SECONDS);

              final int expectedInstances = numThreads * instancesPerThread;
              assertThat(successfulInstances.get()).isEqualTo(expectedInstances);
              assertThat(failedInstances.get()).isEqualTo(0);

            } finally {
              executor.shutdownNow();
              executor.awaitTermination(10, TimeUnit.SECONDS);
            }

            addTestMetric(
                String.format(
                    "Resource contention: %d successful, %d failed instances with %s",
                    successfulInstances.get(), failedInstances.get(), runtimeType));
          });
    }

    @Test
    @DisplayName("Should handle concurrent instance close operations safely")
    void shouldHandleConcurrentInstanceCloseOperationsSafely() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            final int numInstances = 20;
            final List<Instance> instances = new ArrayList<>();

            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes)) {

              // Create multiple instances
              for (int i = 0; i < numInstances; i++) {
                final Instance instance = module.instantiate(store);
                instances.add(instance);

                // Verify instance works
                final WasmValue[] result =
                    instance.callFunction("add", WasmValue.i32(i), WasmValue.i32(1));
                assertThat(result[0].asI32()).isEqualTo(i + 1);
              }

              // Close all instances concurrently
              final ExecutorService executor = Executors.newFixedThreadPool(numInstances);
              final AtomicInteger successfulCloses = new AtomicInteger(0);
              final AtomicInteger failedCloses = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numInstances; i++) {
                  final Instance instance = instances.get(i);
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            try {
                              instance.close();
                              successfulCloses.incrementAndGet();
                            } catch (final Exception e) {
                              failedCloses.incrementAndGet();
                              LOGGER.warning("Instance close failed: " + e.getMessage());
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all closes to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(15, TimeUnit.SECONDS);

                // All closes should succeed
                assertThat(successfulCloses.get()).isEqualTo(numInstances);
                assertThat(failedCloses.get()).isEqualTo(0);

                // All instances should be invalid
                for (final Instance instance : instances) {
                  assertThat(instance.isValid()).isFalse();
                }

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
              }
            }

            addTestMetric(
                String.format(
                    "Concurrent closes: %d successful, %d failed with %s",
                    numInstances, 0, runtimeType));
          });
    }
  }
}
