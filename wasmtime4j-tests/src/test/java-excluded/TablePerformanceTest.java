package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Performance tests for WebAssembly table operations.
 *
 * <p>This test suite evaluates the performance characteristics of table operations under various
 * scenarios including: - Single-threaded sequential access patterns - Bulk operations (fill, copy)
 * performance - Table growth performance - Concurrent access patterns - Memory usage patterns
 *
 * <p>These tests are designed to identify performance regressions and ensure that table operations
 * meet reasonable performance expectations across different runtime implementations.
 */
@EnabledIfSystemProperty(named = "test.performance", matches = "true")
public class TablePerformanceTest {

  private static final Logger logger = Logger.getLogger(TablePerformanceTest.class.getName());

  // Performance test parameters
  private static final int LARGE_TABLE_SIZE = 10000;
  private static final int MEDIUM_TABLE_SIZE = 1000;
  private static final int SMALL_TABLE_SIZE = 100;
  private static final int CONCURRENT_THREADS = 4;
  private static final int ITERATIONS = 1000;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    logger.info("Setting up performance test: " + testInfo.getDisplayName());

    try {
      runtime = WasmRuntimeFactory.createRuntime();
      engine = runtime.createEngine();
      store = runtime.createStore(engine);

      logger.info("Using runtime implementation: " + runtime.getClass().getSimpleName());
    } catch (Exception e) {
      logger.severe("Failed to set up test environment: " + e.getMessage());
      throw new RuntimeException("Test setup failed", e);
    }
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      try {
        store.close();
      } catch (Exception e) {
        logger.warning("Failed to close store: " + e.getMessage());
      }
    }

    if (engine != null) {
      try {
        engine.close();
      } catch (Exception e) {
        logger.warning("Failed to close engine: " + e.getMessage());
      }
    }

    if (runtime != null) {
      try {
        runtime.close();
      } catch (Exception e) {
        logger.warning("Failed to close runtime: " + e.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Performance test: Sequential table access patterns")
  void testSequentialAccessPerformance() throws WasmException {
    final String wat =
        String.format(
            """
            (module
              (table $test_table %d funcref)
              (export "test_table" (table $test_table))
            )
            """,
            LARGE_TABLE_SIZE);

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Test sequential write performance
    long startTime = System.nanoTime();
    for (int i = 0; i < table.getSize(); i++) {
      table.set(i, null);
    }
    long writeTime = System.nanoTime() - startTime;

    // Test sequential read performance
    startTime = System.nanoTime();
    for (int i = 0; i < table.getSize(); i++) {
      table.get(i);
    }
    long readTime = System.nanoTime() - startTime;

    double writeOpsPerSecond = (double) table.getSize() / (writeTime / 1_000_000_000.0);
    double readOpsPerSecond = (double) table.getSize() / (readTime / 1_000_000_000.0);

    logger.info(String.format("Sequential access performance for %d elements:", table.getSize()));
    logger.info(
        String.format(
            "  Write: %.2f ops/sec (%.3f ms total)", writeOpsPerSecond, writeTime / 1_000_000.0));
    logger.info(
        String.format(
            "  Read:  %.2f ops/sec (%.3f ms total)", readOpsPerSecond, readTime / 1_000_000.0));

    // Performance expectations (adjust based on reasonable benchmarks)
    assertTrue(writeOpsPerSecond > 10000, "Write operations should exceed 10,000 ops/sec");
    assertTrue(readOpsPerSecond > 50000, "Read operations should exceed 50,000 ops/sec");
  }

  @Test
  @DisplayName("Performance test: Random table access patterns")
  void testRandomAccessPerformance() throws WasmException {
    final String wat =
        String.format(
            """
            (module
              (table $test_table %d funcref)
              (export "test_table" (table $test_table))
            )
            """,
            MEDIUM_TABLE_SIZE);

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Generate random indices for access
    int[] randomIndices = new int[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      randomIndices[i] = (int) (Math.random() * table.getSize());
    }

    // Test random write performance
    long startTime = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) {
      table.set(randomIndices[i], null);
    }
    long writeTime = System.nanoTime() - startTime;

    // Test random read performance
    startTime = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) {
      table.get(randomIndices[i]);
    }
    long readTime = System.nanoTime() - startTime;

    double writeOpsPerSecond = (double) ITERATIONS / (writeTime / 1_000_000_000.0);
    double readOpsPerSecond = (double) ITERATIONS / (readTime / 1_000_000_000.0);

    logger.info(String.format("Random access performance for %d operations:", ITERATIONS));
    logger.info(
        String.format(
            "  Write: %.2f ops/sec (%.3f ms total)", writeOpsPerSecond, writeTime / 1_000_000.0));
    logger.info(
        String.format(
            "  Read:  %.2f ops/sec (%.3f ms total)", readOpsPerSecond, readTime / 1_000_000.0));

    // Performance expectations for random access
    assertTrue(writeOpsPerSecond > 5000, "Random write operations should exceed 5,000 ops/sec");
    assertTrue(readOpsPerSecond > 20000, "Random read operations should exceed 20,000 ops/sec");
  }

  @Test
  @DisplayName("Performance test: Table bulk operations")
  void testBulkOperationsPerformance() throws WasmException {
    final String wat =
        String.format(
            """
            (module
              (table $test_table %d funcref)
              (export "test_table" (table $test_table))
            )
            """,
            LARGE_TABLE_SIZE);

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Test fill performance
    long startTime = System.nanoTime();
    table.fill(0, table.getSize(), null);
    long fillTime = System.nanoTime() - startTime;

    // Test copy performance (copy half of the table to the other half)
    int halfSize = table.getSize() / 2;
    startTime = System.nanoTime();
    table.copy(halfSize, 0, halfSize);
    long copyTime = System.nanoTime() - startTime;

    double fillOpsPerSecond = (double) table.getSize() / (fillTime / 1_000_000_000.0);
    double copyOpsPerSecond = (double) halfSize / (copyTime / 1_000_000_000.0);

    logger.info(String.format("Bulk operations performance:"));
    logger.info(
        String.format(
            "  Fill %d elements: %.2f ops/sec (%.3f ms total)",
            table.getSize(), fillOpsPerSecond, fillTime / 1_000_000.0));
    logger.info(
        String.format(
            "  Copy %d elements: %.2f ops/sec (%.3f ms total)",
            halfSize, copyOpsPerSecond, copyTime / 1_000_000.0));

    // Performance expectations for bulk operations
    assertTrue(fillOpsPerSecond > 100000, "Fill operations should exceed 100,000 ops/sec");
    assertTrue(copyOpsPerSecond > 50000, "Copy operations should exceed 50,000 ops/sec");
  }

  @Test
  @DisplayName("Performance test: Table growth operations")
  void testTableGrowthPerformance() throws WasmException {
    final String wat =
        String.format(
            """
            (module
              (table $test_table 1 %d funcref)
              (export "test_table" (table $test_table))
            )
            """,
            LARGE_TABLE_SIZE);

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Test incremental growth
    int growthSteps = 10;
    int elementsPerStep = (LARGE_TABLE_SIZE - 1) / growthSteps;

    long totalGrowTime = 0;
    int totalGrowthElements = 0;

    for (int step = 0; step < growthSteps; step++) {
      long startTime = System.nanoTime();
      int previousSize = table.grow(elementsPerStep, null);
      long growTime = System.nanoTime() - startTime;

      totalGrowTime += growTime;
      totalGrowthElements += elementsPerStep;

      assertTrue(previousSize >= 0, "Growth should succeed within limits");
    }

    double growthOpsPerSecond = (double) totalGrowthElements / (totalGrowTime / 1_000_000_000.0);

    logger.info(String.format("Table growth performance:"));
    logger.info(
        String.format(
            "  Grew by %d elements in %d steps: %.2f ops/sec (%.3f ms total)",
            totalGrowthElements, growthSteps, growthOpsPerSecond, totalGrowTime / 1_000_000.0));

    // Performance expectations for growth operations
    assertTrue(growthOpsPerSecond > 10000, "Growth operations should exceed 10,000 ops/sec");
  }

  @Test
  @DisplayName("Performance test: Concurrent table access")
  void testConcurrentAccessPerformance() throws WasmException, InterruptedException {
    final String wat =
        String.format(
            """
            (module
              (table $test_table %d funcref)
              (export "test_table" (table $test_table))
            )
            """,
            MEDIUM_TABLE_SIZE);

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
    CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
    AtomicLong totalOperations = new AtomicLong(0);

    long startTime = System.nanoTime();

    // Create concurrent workers
    for (int thread = 0; thread < CONCURRENT_THREADS; thread++) {
      final int threadId = thread;
      executor.submit(
          () -> {
            try {
              int operationsPerThread = ITERATIONS / CONCURRENT_THREADS;
              int startIndex = threadId * (table.getSize() / CONCURRENT_THREADS);
              int endIndex =
                  Math.min(startIndex + (table.getSize() / CONCURRENT_THREADS), table.getSize());

              // Each thread works on its own section to avoid conflicts
              for (int i = 0; i < operationsPerThread; i++) {
                int index = startIndex + (i % (endIndex - startIndex));

                // Alternate between read and write operations
                if (i % 2 == 0) {
                  table.set(index, null);
                } else {
                  table.get(index);
                }

                totalOperations.incrementAndGet();
              }
            } catch (Exception e) {
              logger.severe(
                  "Concurrent access error in thread " + threadId + ": " + e.getMessage());
            } finally {
              latch.countDown();
            }
          });
    }

    // Wait for all threads to complete
    boolean completed = latch.await(30, TimeUnit.SECONDS);
    assertTrue(completed, "All concurrent operations should complete within timeout");

    long totalTime = System.nanoTime() - startTime;
    executor.shutdown();

    double concurrentOpsPerSecond = (double) totalOperations.get() / (totalTime / 1_000_000_000.0);

    logger.info(String.format("Concurrent access performance:"));
    logger.info(
        String.format(
            "  %d operations across %d threads: %.2f ops/sec (%.3f ms total)",
            totalOperations.get(),
            CONCURRENT_THREADS,
            concurrentOpsPerSecond,
            totalTime / 1_000_000.0));

    // Performance expectations for concurrent access
    assertTrue(concurrentOpsPerSecond > 5000, "Concurrent operations should exceed 5,000 ops/sec");
  }

  @Test
  @DisplayName("Performance test: Memory usage patterns")
  void testMemoryUsagePatterns() throws WasmException {
    // Measure memory usage before creating large tables
    Runtime jvmRuntime = Runtime.getRuntime();
    jvmRuntime.gc(); // Suggest garbage collection
    long initialMemory = jvmRuntime.totalMemory() - jvmRuntime.freeMemory();

    // Create multiple tables of varying sizes
    final String smallWat =
        String.format(
            """
            (module
              (table $small_table %d funcref)
              (export "small_table" (table $small_table))
            )
            """,
            SMALL_TABLE_SIZE);

    final String mediumWat =
        String.format(
            """
            (module
              (table $medium_table %d funcref)
              (export "medium_table" (table $medium_table))
            )
            """,
            MEDIUM_TABLE_SIZE);

    final String largeWat =
        String.format(
            """
            (module
              (table $large_table %d funcref)
              (export "large_table" (table $large_table))
            )
            """,
            LARGE_TABLE_SIZE);

    Module smallModule = runtime.compileModule(smallWat);
    Instance smallInstance = runtime.instantiate(smallModule, store);
    final WasmTable smallTable = smallInstance.getTable("small_table");

    jvmRuntime.gc();
    long afterSmallTable = jvmRuntime.totalMemory() - jvmRuntime.freeMemory();

    Module mediumModule = runtime.compileModule(mediumWat);
    Instance mediumInstance = runtime.instantiate(mediumModule, store);
    final WasmTable mediumTable = mediumInstance.getTable("medium_table");

    jvmRuntime.gc();
    long afterMediumTable = jvmRuntime.totalMemory() - jvmRuntime.freeMemory();

    Module largeModule = runtime.compileModule(largeWat);
    Instance largeInstance = runtime.instantiate(largeModule, store);
    final WasmTable largeTable = largeInstance.getTable("large_table");

    jvmRuntime.gc();
    long afterLargeTable = jvmRuntime.totalMemory() - jvmRuntime.freeMemory();

    long smallTableMemory = afterSmallTable - initialMemory;
    long mediumTableMemory = afterMediumTable - afterSmallTable;
    long largeTableMemory = afterLargeTable - afterMediumTable;

    logger.info("Memory usage patterns:");
    logger.info(
        String.format(
            "  Small table (%d elements): %d KB", SMALL_TABLE_SIZE, smallTableMemory / 1024));
    logger.info(
        String.format(
            "  Medium table (%d elements): %d KB", MEDIUM_TABLE_SIZE, mediumTableMemory / 1024));
    logger.info(
        String.format(
            "  Large table (%d elements): %d KB", LARGE_TABLE_SIZE, largeTableMemory / 1024));

    // Memory usage should scale reasonably (not more than 100 bytes per element)
    assertTrue(
        smallTableMemory < SMALL_TABLE_SIZE * 100, "Small table memory usage should be reasonable");
    assertTrue(
        mediumTableMemory < MEDIUM_TABLE_SIZE * 100,
        "Medium table memory usage should be reasonable");
    assertTrue(
        largeTableMemory < LARGE_TABLE_SIZE * 100, "Large table memory usage should be reasonable");

    // Test that tables are actually functional
    smallTable.set(0, null);
    mediumTable.fill(0, 10, null);
    largeTable.copy(1000, 0, 100);

    logger.info("Memory usage patterns test completed successfully");
  }

  @Test
  @DisplayName("Performance test: Cross-table operations")
  void testCrossTableOperationsPerformance() throws WasmException {
    final String wat =
        String.format(
            """
            (module
              (table $source_table %d funcref)
              (table $dest_table %d funcref)
              (export "source_table" (table $source_table))
              (export "dest_table" (table $dest_table))
            )
            """,
            MEDIUM_TABLE_SIZE, MEDIUM_TABLE_SIZE);

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable sourceTable = instance.getTable("source_table");
    WasmTable destTable = instance.getTable("dest_table");

    // Test cross-table copy performance
    int copySize = MEDIUM_TABLE_SIZE / 4;
    long startTime = System.nanoTime();

    // Perform multiple cross-table copy operations
    int copies = 4;
    for (int i = 0; i < copies; i++) {
      int srcOffset = i * copySize;
      int destOffset = i * copySize;
      destTable.copy(destOffset, sourceTable, srcOffset, copySize);
    }

    long copyTime = System.nanoTime() - startTime;

    double crossTableOpsPerSecond = (double) (copies * copySize) / (copyTime / 1_000_000_000.0);

    logger.info(String.format("Cross-table operations performance:"));
    logger.info(
        String.format(
            "  %d copies of %d elements each: %.2f ops/sec (%.3f ms total)",
            copies, copySize, crossTableOpsPerSecond, copyTime / 1_000_000.0));

    // Performance expectations for cross-table operations
    assertTrue(
        crossTableOpsPerSecond > 10000, "Cross-table operations should exceed 10,000 ops/sec");
  }
}
