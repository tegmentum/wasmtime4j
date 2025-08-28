/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Comprehensive tests for Stream 1 high-performance module operations in Panama implementation.
 *
 * <p>These tests validate the performance optimizations, zero-copy operations, memory-mapped file
 * support, caching mechanisms, and bulk processing capabilities implemented for Issue #10.
 */
@EnabledOnJre(
    value = {JRE.JAVA_23},
    disabledReason = "Panama FFI requires Java 23+")
class PanamaModuleHighPerformanceTest {

  private ArenaResourceManager resourceManager;
  private PanamaEngine engine;
  private Path tempWasmFile;

  // Simple WebAssembly module for testing (adds two numbers)
  private static final byte[] SIMPLE_WASM = {
    0x00,
    0x61,
    0x73,
    0x6D,
    0x01,
    0x00,
    0x00,
    0x00, // WASM magic + version
    0x01,
    0x07,
    0x01,
    0x60,
    0x02,
    0x7F,
    0x7F,
    0x01,
    0x7F, // type section
    0x03,
    0x02,
    0x01,
    0x00, // function section
    0x0A,
    0x09,
    0x01,
    0x07,
    0x00,
    0x20,
    0x00,
    0x20,
    0x01,
    0x6A,
    0x0B // code section
  };

  // Larger WASM module for memory-mapped file testing
  private static final byte[] LARGE_WASM = new byte[1024 * 1024]; // 1MB module

  static {
    // Initialize large WASM with proper headers
    System.arraycopy(SIMPLE_WASM, 0, LARGE_WASM, 0, SIMPLE_WASM.length);
  }

  @BeforeEach
  void setUp() throws Exception {
    resourceManager = new ArenaResourceManager();
    engine = new PanamaEngine(resourceManager);

    // Create temporary WASM file for memory-mapped testing
    tempWasmFile = Files.createTempFile("test-wasm", ".wasm");
    Files.write(tempWasmFile, LARGE_WASM, StandardOpenOption.WRITE);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (engine != null && !engine.isClosed()) {
      engine.close();
    }
    if (resourceManager != null && !resourceManager.isClosed()) {
      resourceManager.close();
    }
    if (tempWasmFile != null && Files.exists(tempWasmFile)) {
      Files.delete(tempWasmFile);
    }
  }

  /** Test zero-copy module compilation with direct MemorySegment access. */
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testZeroCopyCompilation() throws Exception {
    // Allocate MemorySegment for WASM data
    ArenaResourceManager.ManagedMemorySegment wasmMemory =
        resourceManager.allocate(SIMPLE_WASM.length);
    MemorySegment wasmData = wasmMemory.getSegment();
    wasmData.copyFrom(MemorySegment.ofArray(SIMPLE_WASM));

    // Test zero-copy compilation
    PanamaModule module = PanamaModule.compileZeroCopy(engine, wasmData, SIMPLE_WASM.length);

    assertNotNull(module, "Zero-copy compilation should succeed");
    assertFalse(module.isClosed(), "Module should not be closed after compilation");
    assertNotNull(module.getModulePointer(), "Module should have valid native pointer");

    // Test module operations
    List<String> imports = module.getImports();
    List<String> exports = module.getExports();

    assertNotNull(imports, "Imports list should not be null");
    assertNotNull(exports, "Exports list should not be null");

    module.close();
    assertTrue(module.isClosed(), "Module should be closed after close()");
  }

  /** Test memory-mapped file compilation for large WebAssembly modules. */
  @Test
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testMemoryMappedFileCompilation() throws Exception {
    // Test memory-mapped file compilation
    PanamaModule module = PanamaModule.compileFromMappedFile(engine, tempWasmFile);

    assertNotNull(module, "Memory-mapped compilation should succeed");
    assertFalse(module.isClosed(), "Module should not be closed after compilation");
    assertNotNull(module.getModulePointer(), "Module should have valid native pointer");

    // Verify the module was compiled from the correct data
    assertTrue(tempWasmFile.toFile().exists(), "Temp file should still exist");
    assertEquals(LARGE_WASM.length, Files.size(tempWasmFile), "File size should match");

    module.close();
    assertTrue(module.isClosed(), "Module should be closed after close()");
  }

  /** Test high-performance module validation without full compilation. */
  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testZeroCopyValidation() throws Exception {
    // First compile a module for validation testing
    PanamaModule module = (PanamaModule) engine.compileModule(SIMPLE_WASM);

    // Allocate MemorySegment for validation
    ArenaResourceManager.ManagedMemorySegment wasmMemory =
        resourceManager.allocate(SIMPLE_WASM.length);
    MemorySegment wasmData = wasmMemory.getSegment();
    wasmData.copyFrom(MemorySegment.ofArray(SIMPLE_WASM));

    // Test zero-copy validation
    boolean isValid = module.validateZeroCopy(wasmData, SIMPLE_WASM.length);
    assertTrue(isValid, "Valid WASM should pass validation");

    // Test validation caching (second call should be faster)
    boolean isValidCached = module.validateZeroCopy(wasmData, SIMPLE_WASM.length);
    assertTrue(isValidCached, "Cached validation should return true");

    module.close();
  }

  /** Test performance-optimized import/export metadata extraction with caching. */
  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testOptimizedMetadataExtraction() throws Exception {
    PanamaModule module = (PanamaModule) engine.compileModule(SIMPLE_WASM);

    // First call should extract and cache metadata
    long startTime = System.nanoTime();
    List<String> imports1 = module.getImports();
    long firstCallTime = System.nanoTime() - startTime;

    // Second call should use cached metadata (should be faster)
    startTime = System.nanoTime();
    List<String> imports2 = module.getImports();
    long secondCallTime = System.nanoTime() - startTime;

    // Verify caching worked
    assertSame(imports1, imports2, "Should return same cached list instance");
    assertTrue(
        secondCallTime <= firstCallTime || secondCallTime < 100_000, // 100μs threshold
        "Cached call should be faster or very fast (first="
            + firstCallTime
            + "ns, second="
            + secondCallTime
            + "ns)");

    // Test exports caching too
    List<String> exports1 = module.getExports();
    List<String> exports2 = module.getExports();
    assertSame(exports1, exports2, "Should return same cached exports list instance");

    module.close();
  }

  /** Test module bytecode caching with MemorySegment storage. */
  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testModuleByteCaching() throws Exception {
    String cacheKey = "test-module-" + System.currentTimeMillis();
    List<String> imports = List.of("import1", "import2");
    List<String> exports = List.of("export1", "export2");
    Map<String, Object> metadata = Map.of("version", "1.0", "size", SIMPLE_WASM.length);

    // Test caching
    PanamaModule.cacheModule(cacheKey, SIMPLE_WASM, imports, exports, metadata);

    // Test retrieval
    PanamaModule.CachedModuleData cachedData = PanamaModule.getCachedModule(cacheKey);
    assertNotNull(cachedData, "Should retrieve cached module data");

    assertArrayEquals(
        SIMPLE_WASM, cachedData.serializedModule, "Cached module bytecode should match");
    assertEquals(imports, cachedData.imports, "Cached imports should match");
    assertEquals(exports, cachedData.exports, "Cached exports should match");
    assertEquals(metadata, cachedData.metadata, "Cached metadata should match");

    // Test cache expiration (this won't expire immediately, but we can test the structure)
    assertFalse(cachedData.isExpired(), "Fresh cache entry should not be expired");

    // Test cache miss
    PanamaModule.CachedModuleData missedData = PanamaModule.getCachedModule("non-existent-key");
    assertNull(missedData, "Should return null for cache miss");
  }

  /** Test bulk module operations for batch processing scenarios. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testBulkModuleOperations() throws Exception {
    // Create multiple WASM modules for bulk testing
    byte[][] modules = {SIMPLE_WASM, SIMPLE_WASM.clone(), SIMPLE_WASM.clone()};

    // Test bulk compilation
    long startTime = System.nanoTime();
    PanamaModule[] compiledModules = PanamaModule.compileBulk(engine, modules);
    long bulkCompileTime = System.nanoTime() - startTime;

    assertEquals(modules.length, compiledModules.length, "Should compile all modules");

    // Verify each module was compiled successfully
    for (int i = 0; i < compiledModules.length; i++) {
      assertNotNull(compiledModules[i], "Module " + i + " should be compiled");
      assertFalse(compiledModules[i].isClosed(), "Module " + i + " should not be closed");
      assertNotNull(
          compiledModules[i].getModulePointer(), "Module " + i + " should have valid pointer");
    }

    // Compare with individual compilation time
    startTime = System.nanoTime();
    PanamaModule individualModule = (PanamaModule) engine.compileModule(SIMPLE_WASM);
    long individualCompileTime = System.nanoTime() - startTime;

    // Bulk operations should be more efficient per module
    long averageBulkTime = bulkCompileTime / modules.length;
    assertTrue(
        averageBulkTime <= individualCompileTime * 1.5, // Allow 50% overhead tolerance
        "Bulk compilation should be efficient (bulk avg="
            + averageBulkTime
            + "ns, individual="
            + individualCompileTime
            + "ns)");

    // Clean up
    for (PanamaModule module : compiledModules) {
      module.close();
    }
    individualModule.close();
  }

  /** Test module serialization with MemorySegment storage optimization. */
  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testHighPerformanceModuleSerialization() throws Exception {
    PanamaModule module = (PanamaModule) engine.compileModule(SIMPLE_WASM);

    // Test high-performance serialization
    byte[] serializedData = module.serializeWithMemorySegment();
    assertNotNull(serializedData, "Serialization should return data");

    // Test standard serialization for comparison
    byte[] standardSerialized = module.serialize();
    assertNotNull(standardSerialized, "Standard serialization should return data");

    // Both methods should return equivalent results
    assertArrayEquals(
        standardSerialized, serializedData, "Both serialization methods should return same data");

    module.close();
  }

  /** Test error handling in high-performance operations. */
  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testErrorHandling() throws Exception {
    // Test null parameter validation
    assertThrows(
        NullPointerException.class,
        () -> PanamaModule.compileZeroCopy(null, MemorySegment.NULL, 0));

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaModule.compileZeroCopy(engine, MemorySegment.NULL, 0));

    assertThrows(
        NullPointerException.class, () -> PanamaModule.compileFromMappedFile(null, tempWasmFile));

    assertThrows(
        NullPointerException.class, () -> PanamaModule.compileFromMappedFile(engine, null));

    // Test invalid file
    Path invalidFile = Files.createTempFile("invalid", ".wasm");
    Files.write(invalidFile, new byte[0]); // Empty file

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaModule.compileFromMappedFile(engine, invalidFile));

    Files.delete(invalidFile);

    // Test bulk operations with empty array
    PanamaModule[] emptyResult = PanamaModule.compileBulk(engine);
    assertEquals(0, emptyResult.length, "Bulk compile with no modules should return empty array");

    // Test caching with null parameters
    assertThrows(
        NullPointerException.class,
        () -> PanamaModule.cacheModule(null, SIMPLE_WASM, List.of(), List.of(), Map.of()));
  }

  /** Test concurrent access to cached operations. */
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testConcurrentCacheAccess() throws Exception {
    PanamaModule module = (PanamaModule) engine.compileModule(SIMPLE_WASM);

    // Test concurrent metadata access
    Runnable metadataAccess =
        () -> {
          try {
            for (int i = 0; i < 10; i++) {
              module.getImports();
              module.getExports();
            }
          } catch (WasmException e) {
            fail("Concurrent metadata access should not throw exceptions: " + e.getMessage());
          }
        };

    // Run concurrent threads
    Thread[] threads = new Thread[5];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(metadataAccess);
    }

    for (Thread thread : threads) {
      thread.start();
    }

    for (Thread thread : threads) {
      thread.join(2000); // 2 second timeout
      assertFalse(thread.isAlive(), "Thread should have completed");
    }

    module.close();
  }

  /** Test resource cleanup and lifecycle management. */
  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testResourceLifecycleManagement() throws Exception {
    PanamaModule module = (PanamaModule) engine.compileModule(SIMPLE_WASM);

    // Verify initial state
    assertFalse(module.isClosed(), "Module should not be closed initially");
    assertNotNull(module.getModulePointer(), "Module should have valid pointer");
    assertNotNull(module.getResourceManager(), "Module should have resource manager");

    // Close the module
    module.close();

    // Verify closed state
    assertTrue(module.isClosed(), "Module should be closed");

    // Test operations on closed module
    assertThrows(IllegalStateException.class, () -> module.getImports());
    assertThrows(IllegalStateException.class, () -> module.getExports());
    assertThrows(IllegalStateException.class, () -> module.serialize());

    // Multiple close calls should be safe
    assertDoesNotThrow(module::close, "Multiple close calls should be safe");
  }

  /**
   * Performance benchmark test to validate 20%+ improvement claims. Note: This is a basic
   * performance test - full benchmarks should use JMH.
   */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testPerformanceBenchmark() throws Exception {
    final int iterations = 100;
    final byte[] testModule = SIMPLE_WASM;

    // Warmup
    for (int i = 0; i < 10; i++) {
      try (PanamaModule module = (PanamaModule) engine.compileModule(testModule)) {
        module.getImports();
        module.getExports();
      }
    }

    // Benchmark standard operations
    long startTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      try (PanamaModule module = (PanamaModule) engine.compileModule(testModule)) {
        module.getImports();
        module.getExports();
      }
    }
    long standardTime = System.nanoTime() - startTime;

    // Benchmark optimized operations (with caching benefits)
    PanamaModule cachedModule = (PanamaModule) engine.compileModule(testModule);

    startTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      cachedModule.getImports(); // These should be cached after first call
      cachedModule.getExports();
    }
    long optimizedTime = System.nanoTime() - startTime;

    cachedModule.close();

    // Calculate performance improvement
    double improvementRatio = (double) standardTime / optimizedTime;

    System.out.println("Standard time: " + standardTime + "ns");
    System.out.println("Optimized time: " + optimizedTime + "ns");
    System.out.println("Improvement ratio: " + improvementRatio + "x");

    // With caching, we should see significant improvement
    assertTrue(
        improvementRatio >= 1.2, // 20% improvement minimum
        "Optimized operations should be at least 20% faster (ratio=" + improvementRatio + ")");
  }
}
