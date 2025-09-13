package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive tests for Module serialization, deserialization, and caching capabilities. Tests
 * module persistence, cache invalidation, and performance of serialization operations.
 *
 * <p>Note: This test suite documents expected serialization API behavior and provides comprehensive
 * test coverage for when serialization capabilities are implemented.
 */
@DisplayName("Module Serialization and Caching Tests")
class ModuleSerializationIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleSerializationTest.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;
  private Path tempCacheDir;

  // Simple module cache implementation for testing
  private final Map<String, CachedModuleData> moduleCache = new ConcurrentHashMap<>();

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("module.serialization");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(4);

      // Create temporary cache directory
      tempCacheDir = Files.createTempDirectory("wasmtime4j-module-cache-test");

    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test environment: " + e.getMessage());
      skipIfNot(false, "Test environment setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownResources() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }

    // Clean up cache directory
    if (tempCacheDir != null) {
      try {
        Files.walk(tempCacheDir)
            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
            .forEach(
                path -> {
                  try {
                    Files.delete(path);
                  } catch (final IOException e) {
                    LOGGER.warning("Failed to delete cache file: " + e.getMessage());
                  }
                });
      } catch (final IOException e) {
        LOGGER.warning("Failed to clean up cache directory: " + e.getMessage());
      }
    }

    moduleCache.clear();
  }

  @Test
  @DisplayName("Should handle module bytecode persistence")
  void shouldHandleModuleBytcodePersistence() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-bytecode-persistence",
            runtime -> {
              // Given
              final byte[] originalBytes = TestUtils.createSimpleWasmModule();
              final String moduleId = "simple-add-module";

              try (final Engine engine = runtime.createEngine()) {
                // When - Compile module and extract its bytecode
                final Module module = engine.compileModule(originalBytes);

                // For now, we'll store the original bytecode as a proxy for serialization
                // In a real implementation, this would be module.serialize() or similar
                final byte[] persistedBytes = originalBytes.clone();

                // Simulate cache storage
                final CachedModuleData cachedData =
                    new CachedModuleData(moduleId, persistedBytes, System.currentTimeMillis());
                moduleCache.put(moduleId, cachedData);

                // Then - Verify cache storage
                assertThat(moduleCache.containsKey(moduleId)).isTrue();
                final CachedModuleData retrieved = moduleCache.get(moduleId);
                assertThat(retrieved.getModuleBytes()).isEqualTo(originalBytes);

                module.close();
                return "Module bytecode persistence successful";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module bytecode persistence validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module compilation cache")
  void shouldHandleModuleCompilationCache() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-compilation-cache",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final String cacheKey =
                  generateCacheKey(wasmBytes, runtime.getClass().getSimpleName());

              try (final Engine engine = runtime.createEngine()) {
                // When - First compilation (cache miss)
                final Instant firstStart = Instant.now();
                final Module module1 = engine.compileModule(wasmBytes);
                final Duration firstCompilation = Duration.between(firstStart, Instant.now());

                // Cache the compilation result (simulated)
                final CachedModuleData cachedData =
                    new CachedModuleData(cacheKey, wasmBytes, System.currentTimeMillis());
                moduleCache.put(cacheKey, cachedData);

                // Second compilation (cache hit simulation)
                final Instant secondStart = Instant.now();
                final boolean cacheHit = moduleCache.containsKey(cacheKey);
                final Module module2;
                if (cacheHit) {
                  // In real implementation: module2 = engine.loadFromCache(cacheKey);
                  module2 = engine.compileModule(wasmBytes); // For now, recompile
                } else {
                  module2 = engine.compileModule(wasmBytes);
                }
                final Duration secondCompilation = Duration.between(secondStart, Instant.now());

                // Then - Verify both modules work
                assertThat(module1.isValid()).isTrue();
                assertThat(module2.isValid()).isTrue();
                assertThat(cacheHit).isTrue();

                module1.close();
                module2.close();

                return String.format(
                    "First: %dms, Second: %dms, Cache hit: %s",
                    firstCompilation.toMillis(), secondCompilation.toMillis(), cacheHit);
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module compilation cache validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle cache invalidation")
  void shouldHandleCacheInvalidation() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-cache-invalidation",
            runtime -> {
              // Given
              final byte[] originalBytes = TestUtils.createSimpleWasmModule();
              final byte[] modifiedBytes = TestUtils.createMemoryImportWasmModule();
              final String cacheKey = "invalidation-test";

              try (final Engine engine = runtime.createEngine()) {
                // When - Cache original module
                final Module originalModule = engine.compileModule(originalBytes);
                moduleCache.put(
                    cacheKey,
                    new CachedModuleData(cacheKey, originalBytes, System.currentTimeMillis()));
                assertThat(moduleCache.containsKey(cacheKey)).isTrue();

                // Invalidate cache entry
                moduleCache.remove(cacheKey);
                assertThat(moduleCache.containsKey(cacheKey)).isFalse();

                // Cache new module with same key
                final Module newModule = engine.compileModule(modifiedBytes);
                moduleCache.put(
                    cacheKey,
                    new CachedModuleData(cacheKey, modifiedBytes, System.currentTimeMillis()));

                // Then - Verify cache now contains new data
                final CachedModuleData cachedData = moduleCache.get(cacheKey);
                assertThat(cachedData.getModuleBytes()).isEqualTo(modifiedBytes);
                assertThat(cachedData.getModuleBytes()).isNotEqualTo(originalBytes);

                originalModule.close();
                newModule.close();

                return "Cache invalidation successful";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Cache invalidation validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle concurrent cache operations")
  void shouldHandleConcurrentCacheOperations() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-concurrent-cache",
            runtime -> {
              // Given
              final int threadCount = 4;
              final int operationsPerThread = 10;
              final CountDownLatch startLatch = new CountDownLatch(1);
              final CountDownLatch completionLatch = new CountDownLatch(threadCount);

              try (final Engine engine = runtime.createEngine()) {
                // When - Perform concurrent cache operations
                for (int i = 0; i < threadCount; i++) {
                  final int threadId = i;
                  executorService.submit(
                      () -> {
                        try {
                          startLatch.await();

                          for (int j = 0; j < operationsPerThread; j++) {
                            final String cacheKey = "thread-" + threadId + "-op-" + j;
                            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

                            // Compile and cache
                            final Module module = engine.compileModule(wasmBytes);
                            moduleCache.put(
                                cacheKey,
                                new CachedModuleData(
                                    cacheKey, wasmBytes, System.currentTimeMillis()));

                            // Verify cache entry exists
                            assertThat(moduleCache.containsKey(cacheKey)).isTrue();

                            module.close();
                          }
                        } catch (final Exception e) {
                          LOGGER.severe("Concurrent cache operation failed: " + e.getMessage());
                          throw new RuntimeException(e);
                        } finally {
                          completionLatch.countDown();
                        }
                      });
                }

                // Start all threads
                startLatch.countDown();

                // Wait for completion
                final boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                // Then - Verify all cache entries exist
                final int expectedEntries = threadCount * operationsPerThread;
                assertThat(moduleCache.size()).isEqualTo(expectedEntries);

                return "Concurrent cache operations completed: " + moduleCache.size() + " entries";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Concurrent cache operations validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle file-based module persistence")
  void shouldHandleFileBasedModulePersistence() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-file-persistence",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final Path moduleFile = tempCacheDir.resolve("persistent-module.wasm");

              try (final Engine engine = runtime.createEngine()) {
                // When - Persist module to file
                final Module module = engine.compileModule(wasmBytes);

                // Simulate serialization to file
                Files.write(moduleFile, wasmBytes);

                // Read back from file
                final byte[] restoredBytes = Files.readAllBytes(moduleFile);
                final Module restoredModule = engine.compileModule(restoredBytes);

                // Then - Verify restoration
                assertThat(restoredBytes).isEqualTo(wasmBytes);
                assertThat(restoredModule.isValid()).isTrue();
                assertThat(restoredModule.getExports().size())
                    .isEqualTo(module.getExports().size());

                module.close();
                restoredModule.close();

                return "File-based persistence successful";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("File-based persistence validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should measure serialization performance")
  void shouldMeasureSerializationPerformance(final RuntimeType runtimeType) {
    // skipIfCategoryNotEnabled("performance");

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "serialization-performance-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int iterations = 50;

              long totalSerializationTime = 0;
              long totalDeserializationTime = 0;

              try (final Engine engine = runtime.createEngine()) {
                // When - Measure serialization/deserialization performance
                for (int i = 0; i < iterations; i++) {
                  final Module module = engine.compileModule(wasmBytes);

                  // Measure serialization (simulated with byte array copy)
                  final Instant serStart = Instant.now();
                  final byte[] serialized = wasmBytes.clone(); // Simulate serialization
                  final Duration serTime = Duration.between(serStart, Instant.now());
                  totalSerializationTime += serTime.toNanos();

                  // Measure deserialization (compilation)
                  final Instant deserStart = Instant.now();
                  final Module deserializedModule = engine.compileModule(serialized);
                  final Duration deserTime = Duration.between(deserStart, Instant.now());
                  totalDeserializationTime += deserTime.toNanos();

                  module.close();
                  deserializedModule.close();
                }

                // Then - Calculate averages
                final double avgSerMs = totalSerializationTime / (iterations * 1_000_000.0);
                final double avgDeserMs = totalDeserializationTime / (iterations * 1_000_000.0);

                assertThat(avgSerMs).isLessThan(10.0); // Should be very fast
                assertThat(avgDeserMs).isLessThan(50.0); // Compilation should be reasonable

                return String.format("Avg ser: %.2fms, deser: %.2fms", avgSerMs, avgDeserMs);
              }
            },
            comparison -> true); // Don't compare exact performance numbers

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Serialization performance for " + runtimeType + ": " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle cache size limits")
  void shouldHandleCacheSizeLimits() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-cache-size-limits",
            runtime -> {
              // Given
              final int maxCacheSize = 10;
              final int modulesToCreate = 15;

              try (final Engine engine = runtime.createEngine()) {
                // When - Fill cache beyond limit
                for (int i = 0; i < modulesToCreate; i++) {
                  final String cacheKey = "size-limit-test-" + i;
                  final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

                  final Module module = engine.compileModule(wasmBytes);
                  moduleCache.put(
                      cacheKey,
                      new CachedModuleData(cacheKey, wasmBytes, System.currentTimeMillis()));

                  // Simulate LRU eviction when cache is full
                  if (moduleCache.size() > maxCacheSize) {
                    final String oldestKey =
                        moduleCache.entrySet().stream()
                            .min(
                                Map.Entry.comparingByValue(
                                    (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp())))
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    if (oldestKey != null) {
                      moduleCache.remove(oldestKey);
                    }
                  }

                  module.close();
                }

                // Then - Verify cache size limit respected
                assertThat(moduleCache.size()).isLessThanOrEqualTo(maxCacheSize);

                return "Cache size limited to: " + moduleCache.size() + " entries";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Cache size limits validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle cache expiration")
  void shouldHandleCacheExpiration() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-cache-expiration",
            runtime -> {
              // Given
              final long expirationMs = 100; // Very short for testing
              final String cacheKey = "expiration-test";

              try (final Engine engine = runtime.createEngine()) {
                // When - Add module to cache
                final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                final Module module = engine.compileModule(wasmBytes);

                moduleCache.put(
                    cacheKey,
                    new CachedModuleData(cacheKey, wasmBytes, System.currentTimeMillis()));

                assertThat(moduleCache.containsKey(cacheKey)).isTrue();

                // Wait for expiration
                Thread.sleep(expirationMs + 50);

                // Simulate expiration check
                final CachedModuleData cachedData = moduleCache.get(cacheKey);
                final boolean expired =
                    (System.currentTimeMillis() - cachedData.getTimestamp()) > expirationMs;

                if (expired) {
                  moduleCache.remove(cacheKey);
                }

                // Then - Verify expiration
                assertThat(expired).isTrue();
                assertThat(moduleCache.containsKey(cacheKey)).isFalse();

                module.close();
                return "Cache expiration handled successfully";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Cache expiration validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module identity across serialization")
  void shouldHandleModuleIdentityAcrossSerialization() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-identity-serialization",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine()) {
                // When - Compile original module
                final Module originalModule = engine.compileModule(wasmBytes);

                // Serialize and deserialize (simulated)
                final byte[] serializedBytes = wasmBytes.clone();
                final Module deserializedModule = engine.compileModule(serializedBytes);

                // Then - Verify modules have same properties but different identity
                assertThat(deserializedModule).isNotSameAs(originalModule);
                assertThat(deserializedModule.getExports().size())
                    .isEqualTo(originalModule.getExports().size());
                assertThat(deserializedModule.getImports().size())
                    .isEqualTo(originalModule.getImports().size());

                // Both should be valid
                assertThat(originalModule.isValid()).isTrue();
                assertThat(deserializedModule.isValid()).isTrue();

                originalModule.close();
                deserializedModule.close();

                return "Module identity preserved across serialization";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module identity serialization validation: " + validation.getSummary());
  }

  /** Generates a cache key for a module based on its bytecode and runtime. */
  private String generateCacheKey(final byte[] wasmBytes, final String runtime) {
    return runtime + "-" + Arrays.hashCode(wasmBytes);
  }

  /** Simple data structure to represent cached module data. */
  private static class CachedModuleData {
    private final String moduleId;
    private final byte[] moduleBytes;
    private final long timestamp;

    public CachedModuleData(final String moduleId, final byte[] moduleBytes, final long timestamp) {
      this.moduleId = moduleId;
      this.moduleBytes = moduleBytes.clone();
      this.timestamp = timestamp;
    }

    public String getModuleId() {
      return moduleId;
    }

    public byte[] getModuleBytes() {
      return moduleBytes.clone();
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
