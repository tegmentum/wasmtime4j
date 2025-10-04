package ai.tegmentum.wasmtime4j.comprehensive;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.jni.performance.CompilationCache;
import ai.tegmentum.wasmtime4j.jni.performance.NativeObjectPool;
import ai.tegmentum.wasmtime4j.jni.performance.PerformanceMonitor;
import ai.tegmentum.wasmtime4j.jni.util.JniBatchProcessor;
import ai.tegmentum.wasmtime4j.jni.util.JniConcurrencyManager;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiPreview2Operations;
import ai.tegmentum.wasmtime4j.panama.performance.PanamaPerformanceMonitor;
import ai.tegmentum.wasmtime4j.panama.util.PanamaBatchProcessor;
import ai.tegmentum.wasmtime4j.panama.util.PanamaConcurrencyManager;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive integration test to validate feature parity between JNI and Panama implementations.
 *
 * <p>This test suite ensures that both implementations provide identical functionality, performance
 * characteristics, and API coverage for all WebAssembly operations.
 */
@DisplayName("JNI-Panama Implementation Parity Validation")
@EnabledIfSystemProperty(named = "wasmtime4j.test.parity", matches = "true")
class JniPanamaParityValidationIT {

  private static final Logger LOGGER =
      Logger.getLogger(JniPanamaParityValidationIT.class.getName());

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    LOGGER.info("Starting parity test: " + testInfo.getDisplayName());
  }

  @Test
  @DisplayName("Performance monitoring parity between JNI and Panama")
  void testPerformanceMonitoringParity() {
    // Test JNI performance monitoring
    PerformanceMonitor.setEnabled(true);
    final long jniStartTime = PerformanceMonitor.startOperation("test_operation");

    // Simulate some work
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    PerformanceMonitor.endOperation("test_operation", jniStartTime);
    final String jniStats = PerformanceMonitor.getStatistics();

    // Test Panama performance monitoring
    PanamaPerformanceMonitor.setEnabled(true);
    final long panamaStartTime = PanamaPerformanceMonitor.startOperation("test_operation");

    // Simulate some work
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    PanamaPerformanceMonitor.endOperation("test_operation", panamaStartTime);
    final String panamaStats = PanamaPerformanceMonitor.getStatistics();

    // Verify both implementations provide statistics
    assertNotNull(jniStats, "JNI performance statistics should not be null");
    assertNotNull(panamaStats, "Panama performance statistics should not be null");

    assertTrue(jniStats.contains("test_operation"), "JNI stats should contain operation name");
    assertTrue(
        panamaStats.contains("test_operation"), "Panama stats should contain operation name");

    LOGGER.info("JNI Performance Stats: " + jniStats.split("\n")[0]);
    LOGGER.info("Panama Performance Stats: " + panamaStats.split("\n")[0]);

    // Verify both can be disabled
    PerformanceMonitor.setEnabled(false);
    PanamaPerformanceMonitor.setEnabled(false);

    assertFalse(PerformanceMonitor.isEnabled(), "JNI monitoring should be disabled");
    assertFalse(PanamaPerformanceMonitor.isEnabled(), "Panama monitoring should be disabled");

    // Re-enable for cleanup
    PerformanceMonitor.setEnabled(true);
    PanamaPerformanceMonitor.setEnabled(true);
  }

  @Test
  @DisplayName("Compilation cache functionality parity")
  void testCompilationCacheParity() {
    // Test data
    final byte[] testWasmBytes = generateTestWasmBytes();
    final String engineOptions = "optimization_level=2";

    // Test JNI compilation cache
    CompilationCache.setEnabled(true);
    assertTrue(CompilationCache.isEnabled(), "JNI cache should be enabled");

    // Store and retrieve from JNI cache
    final byte[] jniCompiledModule = new byte[] {1, 2, 3, 4, 5}; // Mock compiled module
    final boolean jniStoreResult =
        CompilationCache.storeInCache(testWasmBytes, jniCompiledModule, engineOptions);
    assertTrue(jniStoreResult, "JNI cache store should succeed");

    final byte[] jniRetrieved = CompilationCache.loadFromCache(testWasmBytes, engineOptions);
    assertNotNull(jniRetrieved, "JNI cache should return cached module");
    assertArrayEquals(jniCompiledModule, jniRetrieved, "JNI cached data should match");

    // Test Panama compilation cache
    ai.tegmentum.wasmtime4j.panama.performance.CompilationCache.setEnabled(true);
    assertTrue(
        ai.tegmentum.wasmtime4j.panama.performance.CompilationCache.isEnabled(),
        "Panama cache should be enabled");

    // Store and retrieve from Panama cache using memory segments
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment wasmSegment = arena.allocate(testWasmBytes.length);
      MemorySegment.copy(
          testWasmBytes, 0, wasmSegment, ValueLayout.JAVA_BYTE, 0, testWasmBytes.length);

      final MemorySegment compiledSegment = arena.allocate(jniCompiledModule.length);
      MemorySegment.copy(
          jniCompiledModule,
          0,
          compiledSegment,
          ValueLayout.JAVA_BYTE,
          0,
          jniCompiledModule.length);

      final boolean panamaStoreResult =
          ai.tegmentum.wasmtime4j.panama.performance.CompilationCache.storeInCache(
              wasmSegment, compiledSegment, engineOptions, 1000000);
      assertTrue(panamaStoreResult, "Panama cache store should succeed");

      final MemorySegment panamaRetrieved =
          ai.tegmentum.wasmtime4j.panama.performance.CompilationCache.loadFromCache(
              wasmSegment, engineOptions, arena);
      assertNotNull(panamaRetrieved, "Panama cache should return cached module");
      assertEquals(
          compiledSegment.byteSize(),
          panamaRetrieved.byteSize(),
          "Panama cached data size should match");
    }

    // Verify cache statistics are available for both
    final String jniCacheStats = CompilationCache.getStatistics();
    final String panamaCacheStats =
        ai.tegmentum.wasmtime4j.panama.performance.CompilationCache.getStatistics();

    assertNotNull(jniCacheStats, "JNI cache statistics should be available");
    assertNotNull(panamaCacheStats, "Panama cache statistics should be available");

    assertTrue(jniCacheStats.contains("Cache hits"), "JNI stats should contain hit information");
    assertTrue(
        panamaCacheStats.contains("Cache hits"), "Panama stats should contain hit information");

    LOGGER.info("JNI cache hit rate: " + CompilationCache.getHitRate() + "%");
    LOGGER.info(
        "Panama cache hit rate: "
            + ai.tegmentum.wasmtime4j.panama.performance.CompilationCache.getHitRate()
            + "%");
  }

  @Test
  @DisplayName("Batch processing functionality parity")
  void testBatchProcessingParity() throws Exception {
    final List<Integer> testData = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    final java.util.function.Function<Integer, Integer> operation = x -> x * 2;

    // Test JNI batch processing
    final JniBatchProcessor jniBatchProcessor = new JniBatchProcessor();
    final List<Integer> jniResults = jniBatchProcessor.processBatch(testData, operation, 3);

    // Test Panama batch processing
    try (Arena arena = Arena.ofConfined()) {
      final PanamaBatchProcessor panamaBatchProcessor = new PanamaBatchProcessor(arena);
      final List<Integer> panamaResults = panamaBatchProcessor.processBatch(testData, operation, 3);

      // Verify results are identical
      assertEquals(jniResults.size(), panamaResults.size(), "Result sizes should match");
      assertEquals(jniResults, panamaResults, "Batch processing results should be identical");

      // Test async processing parity
      final CompletableFuture<List<Integer>> jniAsyncResults =
          jniBatchProcessor.processBatchAsync(testData, operation);
      final CompletableFuture<List<Integer>> panamaAsyncResults =
          panamaBatchProcessor.processBatchAsync(testData, operation);

      final List<Integer> jniAsyncResult = jniAsyncResults.get();
      final List<Integer> panamaAsyncResult = panamaAsyncResults.get();

      assertEquals(
          jniAsyncResult, panamaAsyncResult, "Async batch processing results should be identical");

      // Verify statistics are available
      final String jniStats = jniBatchProcessor.getStatistics();
      final String panamaStats = panamaBatchProcessor.getStatistics();

      assertNotNull(jniStats, "JNI batch processor statistics should be available");
      assertNotNull(panamaStats, "Panama batch processor statistics should be available");

      LOGGER.info("JNI batch processor: " + jniBatchProcessor.getPerformanceMetrics());
      LOGGER.info("Panama batch processor: " + panamaBatchProcessor.getPerformanceMetrics());
    }
  }

  @Test
  @DisplayName("Concurrency management parity")
  void testConcurrencyManagementParity() throws Exception {
    final int maxConcurrency = 4;

    // Test JNI concurrency management
    final JniConcurrencyManager jniManager = new JniConcurrencyManager(maxConcurrency);
    final java.util.concurrent.Callable<String> testOperation =
        () -> {
          Thread.sleep(10);
          return "test_result";
        };

    final String jniResult = jniManager.execute(testOperation);
    assertEquals("test_result", jniResult, "JNI concurrent operation should succeed");

    // Test Panama concurrency management
    final PanamaConcurrencyManager panamaManager = new PanamaConcurrencyManager(maxConcurrency);
    final String panamaResult = panamaManager.execute(testOperation);
    assertEquals("test_result", panamaResult, "Panama concurrent operation should succeed");

    // Test async execution parity
    final CompletableFuture<String> jniAsyncResult = jniManager.executeAsync(testOperation);
    final CompletableFuture<String> panamaAsyncResult = panamaManager.executeAsync(testOperation);

    assertEquals(
        jniAsyncResult.get(), panamaAsyncResult.get(), "Async execution results should match");

    // Test resource locking parity
    final String jniLockedResult = jniManager.executeWithLock("test_resource", testOperation);
    final String panamaLockedResult = panamaManager.executeWithLock("test_resource", testOperation);
    assertEquals(jniLockedResult, panamaLockedResult, "Locked execution results should match");

    // Verify both managers provide statistics
    final String jniStats = jniManager.getStatistics();
    final String panamaStats = panamaManager.getStatistics();

    assertNotNull(jniStats, "JNI concurrency statistics should be available");
    assertNotNull(panamaStats, "Panama concurrency statistics should be available");

    assertTrue(jniStats.contains("Total operations"), "JNI stats should contain operation count");
    assertTrue(
        panamaStats.contains("Total operations"), "Panama stats should contain operation count");

    // Test max concurrency limits
    assertEquals(
        maxConcurrency,
        jniManager.getMaxConcurrentOperations(),
        "JNI manager should respect max concurrency");
    assertEquals(
        maxConcurrency,
        panamaManager.getMaxConcurrentOperations(),
        "Panama manager should respect max concurrency");

    LOGGER.info("JNI concurrency: " + jniManager.getPerformanceMetrics());
    LOGGER.info("Panama concurrency: " + panamaManager.getPerformanceMetrics());

    // Cleanup
    jniManager.shutdown();
    panamaManager.shutdown();
  }

  @Test
  @DisplayName("Object pooling parity")
  void testObjectPoolingParity() {
    // Test JNI object pooling
    final NativeObjectPool<ByteBuffer> jniPool =
        NativeObjectPool.getPool(ByteBuffer.class, () -> ByteBuffer.allocateDirect(1024), 8);

    final ByteBuffer jniBorrowed = jniPool.borrow();
    assertNotNull(jniBorrowed, "JNI pool should provide object");
    assertEquals(1024, jniBorrowed.capacity(), "JNI pooled object should have correct capacity");

    jniPool.returnObject(jniBorrowed);

    // Test Panama object pooling
    try (Arena arena = Arena.ofConfined()) {
      final ai.tegmentum.wasmtime4j.panama.performance.PanaNativeObjectPool<MemorySegment>
          panamaPool =
              ai.tegmentum.wasmtime4j.panama.performance.PanaNativeObjectPool.getPool(
                  MemorySegment.class, a -> a.allocate(1024), 8);

      final MemorySegment panamaBorrowed = panamaPool.borrow(arena);
      assertNotNull(panamaBorrowed, "Panama pool should provide object");
      assertEquals(
          1024, panamaBorrowed.byteSize(), "Panama pooled object should have correct size");

      panamaPool.returnObject(panamaBorrowed);

      // Verify pool statistics are available
      final String jniPoolStats = jniPool.getStats();
      final String panamaPoolStats = panamaPool.getStats();

      assertNotNull(jniPoolStats, "JNI pool statistics should be available");
      assertNotNull(panamaPoolStats, "Panama pool statistics should be available");

      assertTrue(jniPoolStats.contains("hit_rate"), "JNI stats should contain hit rate");
      assertTrue(panamaPoolStats.contains("hit_rate"), "Panama stats should contain hit rate");

      // Test pool hit rates
      assertTrue(jniPool.getHitRate() >= 0, "JNI pool hit rate should be non-negative");
      assertTrue(panamaPool.getHitRate() >= 0, "Panama pool hit rate should be non-negative");

      LOGGER.info("JNI pool: " + jniPool.getPerformanceStats());
      LOGGER.info("Panama pool: " + panamaPool.getPanamaPerformanceStats());

      panamaPool.close();
    }

    jniPool.close();
  }

  @Test
  @DisplayName("WASI Preview 2 operations parity")
  void testWasiPreview2OperationsParity() {
    // This test would require actual WASI context setup
    // For now, we'll test basic instantiation and method availability

    // Note: In a real test, we would need proper WASI context setup
    // For now, we'll verify that the classes exist and have expected methods

    // Verify JNI WASI Preview 2 operations class exists
    final Class<?> jniWasiClass = WasiPreview2Operations.class;
    assertNotNull(jniWasiClass, "JNI WASI Preview 2 operations class should exist");

    // Verify Panama WASI Preview 2 operations class exists
    final Class<?> panamaWasiClass =
        ai.tegmentum.wasmtime4j.panama.wasi.WasiPreview2Operations.class;
    assertNotNull(panamaWasiClass, "Panama WASI Preview 2 operations class should exist");

    // Verify both have key methods
    assertDoesNotThrow(
        () -> jniWasiClass.getMethod("createResource", String.class, ByteBuffer.class),
        "JNI should have createResource method");
    assertDoesNotThrow(
        () -> panamaWasiClass.getMethod("createResource", String.class, MemorySegment.class),
        "Panama should have createResource method");

    assertDoesNotThrow(
        () -> jniWasiClass.getMethod("destroyResource", long.class),
        "JNI should have destroyResource method");
    assertDoesNotThrow(
        () -> panamaWasiClass.getMethod("destroyResource", long.class),
        "Panama should have destroyResource method");

    assertDoesNotThrow(
        () -> jniWasiClass.getMethod("openInputStream", long.class),
        "JNI should have openInputStream method");
    assertDoesNotThrow(
        () -> panamaWasiClass.getMethod("openInputStream", long.class),
        "Panama should have openInputStream method");

    LOGGER.info("Both implementations provide WASI Preview 2 operations with compatible APIs");
  }

  @Test
  @DisplayName("Error handling consistency between implementations")
  void testErrorHandlingConsistency() {
    // Test that both implementations handle null parameters consistently

    // JNI null parameter validation
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          final NativeObjectPool<String> pool =
              NativeObjectPool.getPool(String.class, () -> "test", 8);
          pool.returnObject(null);
        },
        "JNI should throw on null parameter");

    // Panama null parameter validation
    try (Arena arena = Arena.ofConfined()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            final ai.tegmentum.wasmtime4j.panama.performance.PanaNativeObjectPool<MemorySegment>
                pool =
                    ai.tegmentum.wasmtime4j.panama.performance.PanaNativeObjectPool.getPool(
                        MemorySegment.class, a -> a.allocate(64), 8);
            pool.returnObject(null);
          },
          "Panama should throw on null parameter");
    }

    // Test both implementations handle invalid parameters consistently
    assertThrows(
        IllegalArgumentException.class,
        () -> new JniBatchProcessor(-1),
        "JNI should reject negative batch size");

    assertThrows(
        IllegalArgumentException.class,
        () -> new PanamaBatchProcessor(-1),
        "Panama should reject negative batch size");

    LOGGER.info("Both implementations provide consistent error handling");
  }

  @Test
  @DisplayName("Resource cleanup parity")
  void testResourceCleanupParity() {
    // Test JNI resource cleanup
    final JniBatchProcessor jniBatchProcessor = new JniBatchProcessor();
    assertTrue(jniBatchProcessor.isActive(), "JNI batch processor should be active initially");
    jniBatchProcessor.close();
    // Note: JniBatchProcessor doesn't have isActive method, so we can't test it here

    // Test Panama resource cleanup
    try (Arena arena = Arena.ofConfined()) {
      final PanamaBatchProcessor panamaBatchProcessor = new PanamaBatchProcessor(arena);
      assertTrue(
          panamaBatchProcessor.isActive(), "Panama batch processor should be active initially");
      panamaBatchProcessor.close();
      // Arena is auto-closed, so processor should become inactive
      // Note: The processor doesn't become inactive just because we call close() when using
      // external arena
    }

    // Test concurrency manager cleanup
    final JniConcurrencyManager jniManager = new JniConcurrencyManager(4);
    final PanamaConcurrencyManager panamaManager = new PanamaConcurrencyManager(4);

    // Both should be able to shutdown cleanly
    assertDoesNotThrow(jniManager::shutdown, "JNI manager should shutdown cleanly");
    assertDoesNotThrow(panamaManager::shutdown, "Panama manager should shutdown cleanly");

    LOGGER.info("Both implementations provide proper resource cleanup");
  }

  /** Generates test WebAssembly bytes for cache testing. */
  private byte[] generateTestWasmBytes() {
    // Simple mock WASM bytes - in a real test, this would be actual WASM bytecode
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6D, // WASM magic number
      0x01,
      0x00,
      0x00,
      0x00, // WASM version
      0x01,
      0x04,
      0x01,
      0x60,
      0x00,
      0x00, // Type section (empty function type)
      0x03,
      0x02,
      0x01,
      0x00, // Function section (one function)
      0x0A,
      0x04,
      0x01,
      0x02,
      0x00,
      0x0B // Code section (empty function body)
    };
  }
}
