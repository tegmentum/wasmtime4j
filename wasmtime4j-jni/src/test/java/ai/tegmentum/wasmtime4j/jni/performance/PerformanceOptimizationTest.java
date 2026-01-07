package ai.tegmentum.wasmtime4j.jni.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Tests for performance optimization components. */
class PerformanceOptimizationTest {

  @BeforeEach
  void setUp() {
    // Reset performance state before each test
    PerformanceMonitor.reset();
    CompilationCache.clear();
    OptimizedMarshalling.reset();
    // Note: Not clearing pools in setup as tests need them available
  }

  @AfterEach
  void tearDown() {
    // Clean up after tests - clear pool contents but don't close the pools
    // as subsequent tests may need them
  }

  @Test
  void testPerformanceMonitoringBasics() {
    // Arrange
    PerformanceMonitor.setEnabled(true);
    PerformanceMonitor.setLowOverheadMode(false); // Disable sampling for test

    // Act
    final long startTime = PerformanceMonitor.startOperation("test_operation", "unit_test");

    // Simulate some work
    try {
      Thread.sleep(1);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    PerformanceMonitor.endOperation("test_operation", startTime);

    // Assert
    final String stats = PerformanceMonitor.getStatistics();
    assertThat(stats).contains("test_operation");
    assertThat(stats).contains("JNI calls");

    final String operationStats = PerformanceMonitor.getOperationStats("test_operation");
    assertThat(operationStats).contains("1 calls");
  }

  @Test
  void testPerformanceMonitoringTargets() {
    // Arrange
    PerformanceMonitor.setEnabled(true);

    // Act - simulate fast operation
    final long startTime = PerformanceMonitor.startOperation("fast_operation");
    PerformanceMonitor.endOperation("fast_operation", startTime);

    // Assert
    final double avgOverhead = PerformanceMonitor.getAverageJniOverhead();
    assertThat(avgOverhead).isGreaterThanOrEqualTo(0);

    // Performance target is 100ns, but actual overhead will depend on system
    // Just verify the monitoring is working
    assertThat(PerformanceMonitor.getStatistics()).contains("Performance target");
  }

  @Test
  void testOptimizedMarshalling() {
    // Arrange
    final WasmValue[] simpleParams = new WasmValue[] {WasmValue.i32(42)};
    final WasmValue[] multipleParams =
        new WasmValue[] {
          WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f), WasmValue.f64(4.0)
        };

    // Act
    final Object[] simpleMarshalled = OptimizedMarshalling.marshalParameters(simpleParams);
    final Object[] multipleMarshalled = OptimizedMarshalling.marshalParameters(multipleParams);

    // Assert - marshalParameters returns one object per parameter
    assertThat(simpleMarshalled).hasSize(1);
    assertThat(multipleMarshalled).hasSize(4); // One marshalled object per WasmValue parameter

    // Test unmarshalling
    final WasmValueType[] expectedTypes = new WasmValueType[] {WasmValueType.I32};
    final Object[] nativeResults = new Object[] {42};
    final WasmValue[] unmarshalled =
        OptimizedMarshalling.unmarshalResults(nativeResults, expectedTypes);

    assertThat(unmarshalled).hasSize(1);
    assertThat(unmarshalled[0].asI32()).isEqualTo(42);
  }

  @Test
  void testNativeObjectPool() {
    // Arrange
    final NativeObjectPool<byte[]> pool =
        NativeObjectPool.getPool(byte[].class, () -> new byte[1024], 8, 2);

    // Act
    final byte[] buffer1 = pool.borrow();
    final byte[] buffer2 = pool.borrow();
    final byte[] buffer3 = pool.borrow(); // Should create new since pool is small

    // Assert
    assertThat(buffer1).isNotNull();
    assertThat(buffer2).isNotNull();
    assertThat(buffer3).isNotNull();
    assertThat(pool.getBorrowedCount()).isEqualTo(3);

    // Return objects
    pool.returnObject(buffer1);
    pool.returnObject(buffer2);
    pool.returnObject(buffer3);

    assertThat(pool.getBorrowedCount()).isEqualTo(0);
    assertThat(pool.getAvailableCount()).isGreaterThanOrEqualTo(2);

    // Test statistics
    assertThat(pool.getTotalBorrows()).isEqualTo(3);
    assertThat(pool.getTotalReturns()).isEqualTo(3);
    assertThat(pool.getHitRate()).isGreaterThanOrEqualTo(0.0);
  }

  @Test
  void testNativeObjectPoolConcurrency() throws InterruptedException {
    // Arrange
    final NativeObjectPool<Integer[]> pool =
        NativeObjectPool.getPool(Integer[].class, () -> new Integer[10], 16, 4);

    final int threadCount = 10;
    final Thread[] threads = new Thread[threadCount];
    final boolean[] results = new boolean[threadCount];

    // Act - concurrent access
    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  for (int j = 0; j < 100; j++) {
                    final Integer[] buffer = pool.borrow();
                    if (buffer != null) {
                      buffer[0] = j;
                      pool.returnObject(buffer);
                    }
                  }
                  results[threadIndex] = true;
                } catch (final Exception e) {
                  results[threadIndex] = false;
                }
              });
      threads[i].start();
    }

    // Wait for completion
    for (final Thread thread : threads) {
      thread.join(1000);
    }

    // Assert
    for (final boolean result : results) {
      assertThat(result).isTrue();
    }

    assertThat(pool.getTotalBorrows()).isEqualTo(threadCount * 100);
    assertThat(pool.getTotalReturns()).isEqualTo(threadCount * 100);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testCallBatch() throws Exception {
    // Arrange
    try (final CallBatch batch = new CallBatch(4, 1000)) {

      // Act
      final CompletableFuture<WasmValue[]> result1 =
          batch.addFunctionCall(12345L, new Object[] {1, 2}, "add");
      final CompletableFuture<WasmValue[]> result2 =
          batch.addFunctionCall(12346L, new Object[] {3, 4}, "multiply");

      assertThat(batch.size()).isEqualTo(2);
      assertThat(batch.isEmpty()).isFalse();

      // Execute batch
      batch.execute();

      // Assert
      assertThat(batch.isExecuted()).isTrue();
      assertThat(result1.isDone()).isTrue();
      assertThat(result2.isDone()).isTrue();

      // Get results (will be empty arrays from mock implementation)
      final WasmValue[] values1 = result1.get();
      final WasmValue[] values2 = result2.get();

      assertThat(values1).isNotNull();
      assertThat(values2).isNotNull();
    }
  }

  @Test
  void testCallBatchMemoryRead() throws Exception {
    // Arrange
    try (final CallBatch batch = new CallBatch()) {

      // Act
      final CompletableFuture<WasmValue[]> result = batch.addMemoryRead(67890L, 100, 256);

      assertThat(batch.size()).isEqualTo(1);

      batch.execute();

      // Assert
      assertThat(result.isDone()).isTrue();
      final WasmValue[] values = result.get();
      assertThat(values).isNotNull();
    }
  }

  @Test
  void testCallBatchLimits() {
    // Arrange
    final CallBatch batch = new CallBatch(2, 0); // Max size 2

    // Act & Assert
    batch.addFunctionCall(1L, new Object[0], "func1");
    batch.addFunctionCall(2L, new Object[0], "func2");

    // Should throw when exceeding limit
    assertThatThrownBy(() -> batch.addFunctionCall(3L, new Object[0], "func3"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("full");

    batch.close();
  }

  @Test
  void testCompilationCache() {
    // Arrange
    CompilationCache.setEnabled(true);
    final byte[] wasmBytes = new byte[] {0x00, 0x61, 0x73, 0x6d}; // Fake WASM header
    final String options = "test_options";

    // Act - first load should be cache miss
    final byte[] cached1 = CompilationCache.loadFromCache(wasmBytes, options);
    assertThat(cached1).isNull();

    // Store in cache
    final byte[] compiled = new byte[] {0x01, 0x02, 0x03, 0x04};
    final boolean stored = CompilationCache.storeInCache(wasmBytes, compiled, options);
    assertThat(stored).isTrue();

    // Second load should be cache hit
    final byte[] cached2 = CompilationCache.loadFromCache(wasmBytes, options);
    assertThat(cached2).isEqualTo(compiled);

    // Assert
    assertThat(CompilationCache.getHitRate()).isEqualTo(50.0); // 1 hit out of 2 attempts
    final String stats = CompilationCache.getStatistics();
    assertThat(stats).contains("Cache hits: 1");
    assertThat(stats).contains("Cache misses: 1");
  }

  @Test
  void testCompilationCacheDisabled() {
    // Arrange
    CompilationCache.setEnabled(false);
    final byte[] wasmBytes = new byte[] {0x00, 0x61, 0x73, 0x6d};

    // Act
    final byte[] cached = CompilationCache.loadFromCache(wasmBytes, "options");
    final boolean stored = CompilationCache.storeInCache(wasmBytes, wasmBytes, "options");

    // Assert
    assertThat(cached).isNull();
    assertThat(stored).isFalse();
    assertThat(CompilationCache.getStatistics()).contains("disabled");
  }

  @Test
  void testPerformanceMonitoringDisabled() {
    // Arrange
    PerformanceMonitor.setEnabled(false);

    // Act
    final long startTime = PerformanceMonitor.startOperation("test");
    PerformanceMonitor.endOperation("test", startTime);

    // Assert
    final String stats = PerformanceMonitor.getStatistics();
    assertThat(stats).contains("disabled");
  }

  @Test
  void testPerformanceIssuesDetection() {
    // Arrange
    PerformanceMonitor.setEnabled(true);
    PerformanceMonitor.recordAllocation(1000 * 1024 * 1024); // 1GB allocation

    // Act
    final String issues = PerformanceMonitor.getPerformanceIssues();

    // Assert
    assertThat(issues).isNotNull();
    assertThat(issues).contains("memory leak");
  }

  @Test
  void testOptimizedMarshallingStatistics() {
    // Arrange
    final WasmValue[] params =
        new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)};

    // Act - perform several marshalling operations
    for (int i = 0; i < 10; i++) {
      OptimizedMarshalling.marshalParameters(params);
    }

    // Assert
    final String stats = OptimizedMarshalling.getStatistics();
    assertThat(stats).contains("Optimized Marshalling");
    assertThat(stats).contains("Strategy usage");
  }

  @Test
  void testMemoryAllocationTracking() {
    // Arrange
    PerformanceMonitor.setEnabled(true);
    PerformanceMonitor.reset();

    // Act
    PerformanceMonitor.recordAllocation(1024);
    PerformanceMonitor.recordAllocation(2048);
    PerformanceMonitor.recordDeallocation(512);

    // Assert
    final String stats = PerformanceMonitor.getStatistics();
    assertThat(stats).contains("Native allocations: 2");
    assertThat(stats).contains("Native deallocations: 1");
    assertThat(stats).contains("Net allocated bytes: 2,560"); // 1024 + 2048 - 512
  }
}
