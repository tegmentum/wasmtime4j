package ai.tegmentum.wasmtime4j.memory;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive memory leak detection tests using the MemoryLeakDetector infrastructure to validate
 * resource lifecycle management, memory cleanup, and leak prevention across all memory operations
 * and runtime types.
 */
@DisplayName("Memory Leak Detection Tests")
final class MemoryLeakDetectionTest extends BaseIntegrationTest {
  private static final Logger LOGGER = Logger.getLogger(MemoryLeakDetectionTest.class.getName());

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up memory leak detection test: " + testInfo.getDisplayName());
    // Pre-test garbage collection to establish baseline
    performGarbageCollection();
  }

  @Override
  protected void doTearDown(final TestInfo testInfo) {
    // Post-test cleanup and validation
    performGarbageCollection();
    LOGGER.info("Completed memory leak detection test: " + testInfo.getDisplayName());
  }

  @Test
  @DisplayName("Should detect no leaks in basic memory operations")
  void shouldDetectNoLeaksInBasicMemoryOperations() {
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofSeconds(30))
            .samplingInterval(100)
            .sampleCount(300)
            .leakThreshold(1.1) // 10% threshold
            .build();

    final MemoryLeakDetector.TestedOperation basicMemoryOps =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            // Create and use memory multiple times
            for (int i = 0; i < 10; i++) {
              final WasmMemory memory = store.createMemory(1, 3);
              
              // Perform various operations
              final byte[] testData = ("Test data iteration " + i).getBytes();
              memory.write(i * 1000, testData);
              final byte[] readData = memory.read(i * 1000, testData.length);
              
              // Grow memory
              memory.grow(1);
              
              // Write to grown area
              memory.write(65536 + i * 100, testData);
              
              // Cleanup happens automatically when memory goes out of scope
              memory.close();
            }
          }
        };

    final MemoryLeakDetector.LeakAnalysisResult result =
        MemoryLeakDetector.detectLeaks("basic_memory_operations", basicMemoryOps, config);

    assertThat(result.isLeakDetected())
        .as("No memory leaks should be detected in basic operations: " + result.getAnalysis())
        .isFalse();

    LOGGER.info("Basic memory operations leak analysis: " + result.getAnalysis());
  }

  @Test
  @DisplayName("Should detect no leaks in cross-runtime memory operations")
  void shouldDetectNoLeaksInCrossRuntimeMemoryOperations() {
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofSeconds(45))
            .samplingInterval(150)
            .leakThreshold(1.15) // Slightly higher threshold for cross-runtime
            .build();

    final var results = MemoryLeakDetector.compareRuntimes("cross_runtime_memory_ops", 
        runtime -> {
          try (final Engine engine = runtime.createEngine();
               final Store store = engine.createStore()) {
            
            // Test with WebAssembly module integration
            final byte[] moduleBytes = TestUtils.createMemoryImportWasmModule();
            final Module module = engine.compileModule(moduleBytes);
            
            for (int iteration = 0; iteration < 5; iteration++) {
              final WasmMemory memory = store.createMemory(2, 5);
              final ImportMap importMap = ImportMap.builder()
                  .addMemory("env", "memory", memory)
                  .build();
              
              final Instance instance = runtime.instantiate(module, importMap);
              final WasmFunction loadFunc = instance.getFunction("load")
                  .orElseThrow(() -> new AssertionError("load function required"));
              
              // Perform memory operations through WebAssembly
              for (int i = 0; i < 20; i++) {
                final int value = iteration * 1000 + i;
                
                // Write data through memory API
                final byte[] data = new byte[4];
                data[0] = (byte) (value & 0xFF);
                data[1] = (byte) ((value >> 8) & 0xFF);
                data[2] = (byte) ((value >> 16) & 0xFF);
                data[3] = (byte) ((value >> 24) & 0xFF);
                memory.write(i * 4, data);
                
                // Read data through WebAssembly
                final WasmValue[] loadResult = loadFunc.call(WasmValue.i32(i * 4));
                assertThat(loadResult[0].asI32()).isEqualTo(value);
              }
              
              memory.close();
            }
          }
        }, config);

    // Validate both runtimes show no leaks
    for (final var entry : results.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final MemoryLeakDetector.LeakAnalysisResult result = entry.getValue();
      
      assertThat(result.isLeakDetected())
          .as(runtimeType + " should not leak memory: " + result.getAnalysis())
          .isFalse();
      
      LOGGER.info(runtimeType + " leak analysis: " + result.getAnalysis());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should detect no leaks in intensive memory lifecycle operations")
  void shouldDetectNoLeaksInIntensiveMemoryLifecycleOperations(final RuntimeType runtimeType) {
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofMinutes(1))
            .samplingInterval(200)
            .sampleCount(300)
            .leakThreshold(1.2) // Higher threshold for intensive operations
            .build();

    final MemoryLeakDetector.TestedOperation intensiveLifecycleOps =
        runtime -> {
          try (final Engine engine = runtime.createEngine()) {
            
            // Intensive store and memory lifecycle testing
            for (int storeIteration = 0; storeIteration < 3; storeIteration++) {
              try (final Store store = engine.createStore()) {
                
                // Create multiple memories with different configurations
                final List<WasmMemory> memories = new ArrayList<>();
                for (int memId = 0; memId < 5; memId++) {
                  final WasmMemory memory = store.createMemory(1 + memId % 3, 5 + memId % 5);
                  memories.add(memory);
                  
                  // Perform operations on each memory
                  final byte[] testData = new byte[1024 * (memId + 1)];
                  Arrays.fill(testData, (byte) (memId + storeIteration));
                  memory.write(0, testData);
                  
                  // Test growth operations
                  if (memory.getPages() < memory.getMaxPages()) {
                    memory.grow(1);
                    memory.write(65536, testData);
                  }
                }
                
                // Verify all memories are functional
                for (int i = 0; i < memories.size(); i++) {
                  final WasmMemory memory = memories.get(i);
                  final byte[] expectedData = new byte[1024 * (i + 1)];
                  Arrays.fill(expectedData, (byte) (i + storeIteration));
                  
                  final byte[] actualData = memory.read(0, expectedData.length);
                  assertThat(actualData).isEqualTo(expectedData);
                }
                
                // Close memories explicitly
                for (final WasmMemory memory : memories) {
                  memory.close();
                }
                
              } // Store auto-closes here
            }
          } // Engine auto-closes here
        };

    final String testName = "intensive_lifecycle_" + runtimeType.name().toLowerCase();
    final MemoryLeakDetector.LeakAnalysisResult result =
        MemoryLeakDetector.detectLeaks(testName, runtimeType, intensiveLifecycleOps, config);

    assertThat(result.isLeakDetected())
        .as("No leaks should be detected in intensive lifecycle operations for " + runtimeType 
            + ": " + result.getAnalysis())
        .isFalse();

    // Validate leak rate is reasonable
    assertThat(result.getLeakRate())
        .as("Leak rate should be minimal for " + runtimeType)
        .isLessThan(100.0); // Less than 100 bytes/second growth

    LOGGER.info("Intensive lifecycle operations for " + runtimeType + ": " + result.getAnalysis());
  }

  @Test
  @DisplayName("Should detect no leaks in concurrent memory operations")
  void shouldDetectNoLeaksInConcurrentMemoryOperations() throws InterruptedException {
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofSeconds(45))
            .samplingInterval(100)
            .sampleCount(450)
            .leakThreshold(1.25) // Higher threshold for concurrent operations
            .build();

    final MemoryLeakDetector.TestedOperation concurrentMemoryOps =
        runtime -> {
          final int threadCount = 4;
          final int operationsPerThread = 25;
          final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
          final CountDownLatch startLatch = new CountDownLatch(1);
          final CountDownLatch completionLatch = new CountDownLatch(threadCount);
          final AtomicInteger totalOperations = new AtomicInteger(0);

          try (final Engine engine = runtime.createEngine();
               final Store store = engine.createStore()) {

            final List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
              final int threadId = t;
              final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                  startLatch.await();
                  
                  for (int i = 0; i < operationsPerThread; i++) {
                    // Each thread works with separate memory regions
                    final WasmMemory memory = store.createMemory(1, 4);
                    final byte[] threadData = ("Thread-" + threadId + "-Op-" + i).getBytes();
                    
                    memory.write(0, threadData);
                    final byte[] readData = memory.read(0, threadData.length);
                    assertThat(readData).isEqualTo(threadData);
                    
                    // Test growth
                    memory.grow(1);
                    memory.write(65536, threadData);
                    
                    totalOperations.incrementAndGet();
                    memory.close();
                  }
                } catch (final Exception e) {
                  throw new RuntimeException("Thread " + threadId + " failed", e);
                } finally {
                  completionLatch.countDown();
                }
              }, executor);
              
              futures.add(future);
            }

            // Start all threads
            startLatch.countDown();
            
            // Wait for completion
            final boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            
            // Wait for all futures
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            assertThat(totalOperations.get()).isEqualTo(threadCount * operationsPerThread);
            
          } finally {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
              executor.shutdownNow();
            }
          }
        };

    // Test with both runtimes for concurrent leak detection
    final var results = MemoryLeakDetector.compareRuntimes(
        "concurrent_memory_operations", concurrentMemoryOps, config);

    for (final var entry : results.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final MemoryLeakDetector.LeakAnalysisResult result = entry.getValue();
      
      assertThat(result.isLeakDetected())
          .as("No leaks should be detected in concurrent operations for " + runtimeType 
              + ": " + result.getAnalysis())
          .isFalse();
      
      LOGGER.info("Concurrent operations for " + runtimeType + ": " + result.getAnalysis());
    }
  }

  @Test
  @DisplayName("Should validate extended duration leak detection with high load")
  void shouldValidateExtendedDurationLeakDetectionWithHighLoad() {
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofMinutes(2)) // Extended duration
            .samplingInterval(250)
            .sampleCount(480) // 2 minutes of samples
            .leakThreshold(1.15) // Stricter threshold for extended testing
            .build();

    final MemoryLeakDetector.TestedOperation highLoadOps =
        runtime -> {
          final byte[] moduleBytes = TestUtils.createMemoryImportWasmModule();
          
          try (final Engine engine = runtime.createEngine()) {
            final Module module = engine.compileModule(moduleBytes);
            
            // High load operation cycling
            for (int cycle = 0; cycle < 50; cycle++) {
              try (final Store store = engine.createStore()) {
                
                // Create multiple memory instances
                for (int memIndex = 0; memIndex < 3; memIndex++) {
                  final WasmMemory memory = store.createMemory(2, 6);
                  final ImportMap importMap = ImportMap.builder()
                      .addMemory("env", "memory", memory)
                      .build();
                  
                  final Instance instance = runtime.instantiate(module, importMap);
                  final WasmFunction loadFunc = instance.getFunction("load")
                      .orElseThrow(() -> new AssertionError("load function required"));
                  
                  // High volume memory operations
                  for (int op = 0; op < 100; op++) {
                    final int offset = op * 4;
                    final int value = cycle * 1000 + memIndex * 100 + op;
                    
                    // Write through memory API
                    final byte[] data = new byte[]{
                        (byte) (value & 0xFF),
                        (byte) ((value >> 8) & 0xFF),
                        (byte) ((value >> 16) & 0xFF),
                        (byte) ((value >> 24) & 0xFF)
                    };
                    memory.write(offset, data);
                    
                    // Verify through WebAssembly
                    final WasmValue[] result = loadFunc.call(WasmValue.i32(offset));
                    assertThat(result[0].asI32()).isEqualTo(value);
                  }
                  
                  // Test memory growth under load
                  if (memory.getPages() < memory.getMaxPages()) {
                    memory.grow(1);
                  }
                  
                  memory.close();
                }
              }
              
              // Occasional small delay to prevent overwhelming
              if (cycle % 10 == 0) {
                Thread.sleep(1);
              }
            }
          }
        };

    final var results = MemoryLeakDetector.compareRuntimes(
        "extended_high_load", highLoadOps, config);

    for (final var entry : results.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final MemoryLeakDetector.LeakAnalysisResult result = entry.getValue();
      
      assertThat(result.isLeakDetected())
          .as("No leaks should be detected in extended high load testing for " + runtimeType 
              + ": " + result.getAnalysis())
          .isFalse();
      
      // Validate leak rate is acceptable for extended testing
      assertThat(result.getLeakRate())
          .as("Leak rate should be minimal for extended testing with " + runtimeType)
          .isLessThan(50.0);
      
      LOGGER.info("Extended high load testing for " + runtimeType + ": " + result.getAnalysis());
    }
  }

  @Test
  @DisplayName("Should validate memory cleanup in error scenarios")  
  void shouldValidateMemoryCleanupInErrorScenarios() {
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofSeconds(30))
            .samplingInterval(100)
            .sampleCount(300)
            .leakThreshold(1.2)
            .build();

    final MemoryLeakDetector.TestedOperation errorScenarioOps =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
               final Store store = engine.createStore()) {
            
            for (int i = 0; i < 20; i++) {
              final WasmMemory memory = store.createMemory(1, 3);
              
              try {
                // Valid operations
                final byte[] data = ("Test " + i).getBytes();
                memory.write(0, data);
                
                // Attempt invalid operations that should fail gracefully
                try {
                  memory.write(-1, data); // Should fail
                } catch (final Exception expected) {
                  // Expected failure - memory should still be valid
                }
                
                try {
                  memory.write(Integer.MAX_VALUE, data); // Should fail
                } catch (final Exception expected) {
                  // Expected failure - memory should still be valid
                }
                
                try {
                  memory.grow(100); // Should fail (exceeds max)
                } catch (final Exception expected) {
                  // Expected failure - memory should still be valid
                }
                
                // Verify memory is still functional after errors
                final byte[] readData = memory.read(0, data.length);
                assertThat(readData).isEqualTo(data);
                
              } finally {
                // Ensure cleanup even after errors
                memory.close();
              }
            }
          }
        };

    final var results = MemoryLeakDetector.compareRuntimes(
        "error_scenarios", errorScenarioOps, config);

    for (final var entry : results.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final MemoryLeakDetector.LeakAnalysisResult result = entry.getValue();
      
      assertThat(result.isLeakDetected())
          .as("No leaks should occur even with error scenarios for " + runtimeType 
              + ": " + result.getAnalysis())
          .isFalse();
      
      LOGGER.info("Error scenarios testing for " + runtimeType + ": " + result.getAnalysis());
    }
  }

  /**
   * Performs thorough garbage collection to establish clean memory baselines.
   */
  private void performGarbageCollection() {
    System.gc();
    System.runFinalization();
    System.gc();
    
    // Give GC time to complete
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}