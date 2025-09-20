package ai.tegmentum.wasmtime4j.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.cache.CacheConfiguration;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheKey;
import ai.tegmentum.wasmtime4j.cache.impl.FileBasedModuleCache;
import ai.tegmentum.wasmtime4j.serialization.impl.ModuleMetadataImpl;
import ai.tegmentum.wasmtime4j.serialization.impl.SerializedModuleImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for the Module Serialization System.
 *
 * <p>This test suite validates the complete serialization API including interfaces, concrete
 * implementations, caching, and metadata management.
 */
@DisplayName("Module Serialization System Tests")
class SerializationSystemTest {

  @TempDir Path tempDir;

  private FileBasedModuleCache cache;
  private CacheConfiguration cacheConfig;

  @BeforeEach
  void setUp() {
    // Create cache configuration for testing
    cacheConfig =
        CacheConfiguration.builder()
            .maxSize(100)
            .expireAfter(1, TimeUnit.HOURS)
            .persistence(true, tempDir.toString())
            .statistics(true)
            .build();

    cache = new FileBasedModuleCache(cacheConfig);
  }

  @AfterEach
  void tearDown() {
    if (cache != null) {
      cache.close();
    }
  }

  @Test
  @DisplayName("Should create and validate serialization options")
  void shouldCreateAndValidateSerializationOptions() {
    // Given
    final SerializationOptions options =
        SerializationOptions.builder()
            .compression(CompressionType.ZSTD)
            .includeDebugInfo()
            .includeSourceMap()
            .addOptimization(OptimizationLevel.SPEED)
            .targetPlatform(TargetPlatform.LINUX_X86_64)
            .strictValidation()
            .maxSize(1024 * 1024)
            .build();

    // Then
    assertThat(options.getCompression()).isEqualTo(CompressionType.ZSTD);
    assertThat(options.isIncludeDebugInfo()).isTrue();
    assertThat(options.isIncludeSourceMap()).isTrue();
    assertThat(options.getOptimizations()).contains(OptimizationLevel.SPEED);
    assertThat(options.getTargetPlatform()).isEqualTo(TargetPlatform.LINUX_X86_64);
    assertThat(options.isStrictValidation()).isTrue();
    assertThat(options.getMaxSize()).isEqualTo(1024 * 1024);
  }

  @Test
  @DisplayName("Should create default serialization options")
  void shouldCreateDefaultSerializationOptions() {
    // When
    final SerializationOptions options = SerializationOptions.defaults();

    // Then
    assertThat(options.getCompression()).isEqualTo(CompressionType.NONE);
    assertThat(options.isIncludeDebugInfo()).isFalse();
    assertThat(options.isIncludeSourceMap()).isFalse();
    assertThat(options.getOptimizations()).contains(OptimizationLevel.BASIC);
    assertThat(options.isStrictValidation()).isFalse();
    assertThat(options.getMaxSize()).isEqualTo(-1);
    assertThat(options.isIncludeChecksum()).isTrue();
  }

  @Test
  @DisplayName("Should detect current target platform")
  void shouldDetectCurrentTargetPlatform() {
    // When
    final TargetPlatform current = TargetPlatform.current();

    // Then
    assertThat(current).isNotNull();
    assertThat(current.getOs()).isNotBlank();
    assertThat(current.getArch()).isNotBlank();
    assertThat(current.getIdentifier()).isNotBlank();

    // Should be compatible with itself
    assertThat(current.isCompatibleWith(current)).isTrue();
  }

  @Test
  @DisplayName("Should validate target platform compatibility")
  void shouldValidateTargetPlatformCompatibility() {
    // Given
    final TargetPlatform linux64 = TargetPlatform.LINUX_X86_64;
    final TargetPlatform linuxArm = TargetPlatform.LINUX_AARCH64;
    final TargetPlatform windows64 = TargetPlatform.WINDOWS_X86_64;

    // Then
    assertThat(linux64.isCompatibleWith(linux64)).isTrue();
    assertThat(linux64.isCompatibleWith(linuxArm)).isFalse();
    assertThat(linux64.isCompatibleWith(windows64)).isFalse();

    assertThat(linux64.hasSameOs(linuxArm)).isTrue();
    assertThat(linux64.hasSameOs(windows64)).isFalse();

    assertThat(linux64.hasSameArchitecture(windows64)).isTrue();
    assertThat(linux64.hasSameArchitecture(linuxArm)).isFalse();
  }

  @Test
  @DisplayName("Should manage WebAssembly features")
  void shouldManageWebAssemblyFeatures() {
    // Test feature properties
    assertThat(WasmFeature.MULTI_VALUE.isStable()).isTrue();
    assertThat(WasmFeature.SIMD.isStable()).isFalse();
    assertThat(WasmFeature.THREADS.requiresSpecialSupport()).isTrue();
    assertThat(WasmFeature.BULK_MEMORY.requiresSpecialSupport()).isFalse();

    // Test feature lookup
    assertThat(WasmFeature.fromName("multi-value")).isEqualTo(WasmFeature.MULTI_VALUE);
    assertThatThrownBy(() -> WasmFeature.fromName("invalid-feature"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should create and validate module metadata")
  void shouldCreateAndValidateModuleMetadata() {
    // Given
    final ModuleMetadataImpl metadata =
        ModuleMetadataImpl.builder()
            .formatVersion("1.0")
            .wasmtimeVersion("36.0.2")
            .targetPlatform(TargetPlatform.LINUX_X86_64)
            .compilationTimestamp(Instant.now())
            .optimizationLevels(Set.of(OptimizationLevel.SPEED))
            .compressionType(CompressionType.ZSTD)
            .hasDebugInfo(true)
            .hasSourceMap(true)
            .originalModuleHash("abc123")
            .dataSize(1024)
            .originalSize(2048)
            .customProperty("compiler", "wasmtime4j")
            .checksum("def456")
            .build();

    // Then
    assertThat(metadata.getFormatVersion()).isEqualTo("1.0");
    assertThat(metadata.getWasmtimeVersion()).isEqualTo("36.0.2");
    assertThat(metadata.getTargetPlatform()).isEqualTo(TargetPlatform.LINUX_X86_64);
    assertThat(metadata.getOptimizationLevels()).contains(OptimizationLevel.SPEED);
    assertThat(metadata.getCompressionType()).isEqualTo(CompressionType.ZSTD);
    assertThat(metadata.hasDebugInfo()).isTrue();
    assertThat(metadata.hasSourceMap()).isTrue();
    assertThat(metadata.getOriginalModuleHash()).isEqualTo("abc123");
    assertThat(metadata.getDataSize()).isEqualTo(1024);
    assertThat(metadata.getOriginalSize()).isEqualTo(2048);
    assertThat(metadata.getCompressionRatio()).isEqualTo(2.0);
    assertThat(metadata.getCustomProperties()).containsEntry("compiler", "wasmtime4j");
    assertThat(metadata.getChecksum()).isEqualTo("def456");

    // Test compatibility
    assertThat(metadata.isCompatibleWith(TargetPlatform.LINUX_X86_64)).isTrue();
    assertThat(metadata.isCompatibleWith(TargetPlatform.WINDOWS_X86_64)).isFalse();
    assertThat(metadata.isCompatibleWith("36.0.2")).isTrue();
    assertThat(metadata.isCompatibleWith("35.0.0")).isFalse();
  }

  @Test
  @DisplayName("Should create and validate serialized modules")
  void shouldCreateAndValidateSerializedModules() {
    // Given
    final byte[] testData = "test module data".getBytes();
    final ModuleMetadataImpl metadata =
        ModuleMetadataImpl.builder()
            .formatVersion("1.0")
            .wasmtimeVersion("36.0.2")
            .targetPlatform(TargetPlatform.current())
            .dataSize(testData.length)
            .originalSize(testData.length * 2)
            .build();

    // When
    final SerializedModuleImpl serializedModule = new SerializedModuleImpl(testData, metadata);

    // Then
    assertThat(serializedModule.getData()).isEqualTo(testData);
    assertThat(serializedModule.getMetadata()).isEqualTo(metadata);
    assertThat(serializedModule.getSize()).isEqualTo(testData.length);
    assertThat(serializedModule.getCompressionType()).isEqualTo(CompressionType.NONE);
    assertThat(serializedModule.hasDebugInfo()).isFalse();
    assertThat(serializedModule.hasSourceMap()).isFalse();
    assertThat(serializedModule.getChecksum()).isNotBlank().hasSize(64); // SHA-256 hex

    // Test data integrity
    final String originalChecksum = serializedModule.getChecksum();
    final SerializedModuleImpl anotherModule = new SerializedModuleImpl(testData, metadata);
    assertThat(anotherModule.getChecksum()).isEqualTo(originalChecksum);
  }

  @Test
  @DisplayName("Should create and validate module cache keys")
  void shouldCreateAndValidateModuleCacheKeys() {
    // Given
    final byte[] wasmHash = "test-hash".getBytes();
    final ai.tegmentum.wasmtime4j.cache.CompilationSettings compilationSettings =
        ai.tegmentum.wasmtime4j.cache.CompilationSettings.defaults();

    // When
    final ModuleCacheKey key =
        ModuleCacheKey.create(wasmHash, null, compilationSettings, "linux-x86_64", "wasmtime4j-1.0");

    // Then
    assertThat(key.getWasmHash()).isEqualTo(wasmHash);
    assertThat(key.getCompilationSettings()).isEqualTo(compilationSettings);
    assertThat(key.getTargetPlatform()).isEqualTo("linux-x86_64");
    assertThat(key.getCompilerVersion()).isEqualTo("wasmtime4j-1.0");
    assertThat(key.getTimestamp()).isGreaterThan(0);
    assertThat(key.toStringRepresentation()).isNotBlank();
    assertThat(key.getShortHash()).hasSize(8);

    // Test compatibility
    final ModuleCacheKey sameKey =
        ModuleCacheKey.create(wasmHash, null, compilationSettings, "linux-x86_64", "wasmtime4j-1.0");
    assertThat(key.isCompatibleWith(sameKey)).isTrue();

    final ModuleCacheKey differentKey =
        ModuleCacheKey.create("different".getBytes(), null, compilationSettings, "linux-x86_64", "wasmtime4j-1.0");
    assertThat(key.isCompatibleWith(differentKey)).isFalse();
  }

  @Test
  @DisplayName("Should manage file-based cache operations")
  void shouldManageFileBasedCacheOperations() {
    // Given
    final byte[] testData = "cached module data".getBytes();
    final ModuleMetadataImpl metadata =
        ModuleMetadataImpl.builder()
            .formatVersion("1.0")
            .targetPlatform(TargetPlatform.current())
            .dataSize(testData.length)
            .build();
    final SerializedModule module = new SerializedModuleImpl(testData, metadata);
    final ModuleCacheKey key = ModuleCacheKey.create("test".getBytes(), null, null);

    // When & Then
    assertThat(cache.isEmpty()).isTrue();
    assertThat(cache.size()).isEqualTo(0);
    assertThat(cache.containsKey(key)).isFalse();

    // Put module in cache
    cache.put(key, module);
    assertThat(cache.isEmpty()).isFalse();
    assertThat(cache.size()).isEqualTo(1);
    assertThat(cache.containsKey(key)).isTrue();

    // Retrieve module from cache
    final var cachedModule = cache.get(key);
    assertThat(cachedModule).isPresent();
    assertThat(cachedModule.get().getData()).isEqualTo(testData);

    // Verify statistics
    final var stats = cache.getStatistics();
    assertThat(stats.getHitCount()).isEqualTo(1);
    assertThat(stats.getMissCount()).isEqualTo(0);
    assertThat(stats.getPutCount()).isEqualTo(1);
    assertThat(stats.getHitRate()).isEqualTo(100.0);

    // Test cache miss
    final ModuleCacheKey missingKey = ModuleCacheKey.create("missing".getBytes(), null, null);
    assertThat(cache.get(missingKey)).isEmpty();
    assertThat(cache.getStatistics().getMissCount()).isEqualTo(1);

    // Invalidate entry
    cache.invalidate(key);
    assertThat(cache.containsKey(key)).isFalse();
    assertThat(cache.size()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle cache expiration")
  void shouldHandleCacheExpiration() throws IOException, InterruptedException {
    // Given - Create cache with very short expiration
    final CacheConfiguration shortExpiryConfig =
        CacheConfiguration.builder()
            .maxSize(100)
            .expireAfter(50, TimeUnit.MILLISECONDS) // Very short expiration
            .persistence(true, tempDir.toString())
            .statistics(true)
            .build();

    try (final FileBasedModuleCache expiryCache = new FileBasedModuleCache(shortExpiryConfig)) {
      final byte[] testData = "expiring module".getBytes();
      final ModuleMetadataImpl metadata = ModuleMetadataImpl.builder().build();
      final SerializedModule module = new SerializedModuleImpl(testData, metadata);
      final ModuleCacheKey key = ModuleCacheKey.create("expiry-test".getBytes(), null, null);

      // When
      expiryCache.put(key, module);
      assertThat(expiryCache.containsKey(key)).isTrue();

      // Wait for expiration
      Thread.sleep(100);

      // Then - Entry should be expired and removed
      assertThat(expiryCache.get(key)).isEmpty();
      assertThat(expiryCache.containsKey(key)).isFalse();
    }
  }

  @Test
  @DisplayName("Should handle cache size limits and eviction")
  void shouldHandleCacheSizeLimitsAndEviction() {
    // Given - Create cache with size limit of 2
    final CacheConfiguration limitedConfig =
        CacheConfiguration.builder()
            .maxSize(2)
            .persistence(true, tempDir.toString())
            .statistics(true)
            .build();

    try (final FileBasedModuleCache limitedCache = new FileBasedModuleCache(limitedConfig)) {
      final ModuleMetadataImpl metadata = ModuleMetadataImpl.builder().build();

      // When - Add 3 modules (exceeding limit)
      final ModuleCacheKey key1 = ModuleCacheKey.create("key1".getBytes(), null, null);
      final ModuleCacheKey key2 = ModuleCacheKey.create("key2".getBytes(), null, null);
      final ModuleCacheKey key3 = ModuleCacheKey.create("key3".getBytes(), null, null);

      limitedCache.put(key1, new SerializedModuleImpl("data1".getBytes(), metadata));
      limitedCache.put(key2, new SerializedModuleImpl("data2".getBytes(), metadata));
      limitedCache.put(key3, new SerializedModuleImpl("data3".getBytes(), metadata));

      // Then - Cache should not exceed size limit
      assertThat(limitedCache.size()).isLessThanOrEqualTo(2);

      // Statistics should show evictions
      final var stats = limitedCache.getStatistics();
      assertThat(stats.getEvictionCount()).isGreaterThan(0);
    }
  }

  @Test
  @DisplayName("Should perform cache maintenance operations")
  void shouldPerformCacheMaintenanceOperations() {
    // Given
    final byte[] testData = "maintenance test".getBytes();
    final ModuleMetadataImpl metadata = ModuleMetadataImpl.builder().build();
    final SerializedModule module = new SerializedModuleImpl(testData, metadata);
    final ModuleCacheKey key = ModuleCacheKey.create("maintenance".getBytes(), null, null);

    cache.put(key, module);
    final long initialMaintenanceCount = cache.getStatistics().getMaintenanceCount();

    // When
    cache.performMaintenance();

    // Then
    final var stats = cache.getStatistics();
    assertThat(stats.getMaintenanceCount()).isEqualTo(initialMaintenanceCount + 1);
    assertThat(stats.getAverageOperationTime()).isNotNull();
    assertThat(stats.getCollectionDuration()).isGreaterThan(Duration.ZERO);
  }

  @Test
  @DisplayName("Should validate cache configuration")
  void shouldValidateCacheConfiguration() {
    // Test valid configuration
    final CacheConfiguration validConfig =
        CacheConfiguration.builder()
            .maxSize(1000)
            .maxMemoryUsage(1024 * 1024)
            .expireAfter(1, TimeUnit.HOURS)
            .evictionPolicy(CacheConfiguration.EvictionPolicy.LRU)
            .integrityChecking(true)
            .statistics(true)
            .persistence(true, tempDir.toString())
            .compression(true)
            .build();

    assertThat(validConfig.getMaxSize()).isEqualTo(1000);
    assertThat(validConfig.getMaxMemoryUsage()).isEqualTo(1024 * 1024);
    assertThat(validConfig.getExpirationDuration()).isEqualTo(Duration.ofHours(1));
    assertThat(validConfig.getEvictionPolicy()).isEqualTo(CacheConfiguration.EvictionPolicy.LRU);
    assertThat(validConfig.isIntegrityCheckingEnabled()).isTrue();
    assertThat(validConfig.isStatisticsEnabled()).isTrue();
    assertThat(validConfig.isPersistenceEnabled()).isTrue();
    assertThat(validConfig.getPersistencePath()).isEqualTo(tempDir.toString());
    assertThat(validConfig.isCompressionEnabled()).isTrue();

    // Test invalid configuration for file-based cache
    assertThatThrownBy(() -> new FileBasedModuleCache(
        CacheConfiguration.builder().persistence(false, null).build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File-based cache requires persistence");
  }

  @Test
  @DisplayName("Should handle compression types")
  void shouldHandleCompressionTypes() {
    // Test all compression types
    for (final CompressionType type : CompressionType.values()) {
      assertThat(type.getName()).isNotBlank();
      assertThat(type.getValue()).isGreaterThanOrEqualTo(0);
      assertThat(CompressionType.fromName(type.getName())).isEqualTo(type);
      assertThat(CompressionType.fromValue(type.getValue())).isEqualTo(type);
    }

    // Test invalid lookups
    assertThatThrownBy(() -> CompressionType.fromName("invalid"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CompressionType.fromValue(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should handle optimization levels")
  void shouldHandleOptimizationLevels() {
    // Test level comparison
    assertThat(OptimizationLevel.SPEED.isMoreAggressiveThan(OptimizationLevel.BASIC)).isTrue();
    assertThat(OptimizationLevel.BASIC.isLessAggressiveThan(OptimizationLevel.SPEED)).isTrue();
    assertThat(OptimizationLevel.SIZE.isMoreAggressiveThan(OptimizationLevel.NONE)).isTrue();

    // Test level lookups
    for (final OptimizationLevel level : OptimizationLevel.values()) {
      assertThat(OptimizationLevel.fromName(level.getName())).isEqualTo(level);
      assertThat(OptimizationLevel.fromLevel(level.getLevel())).isEqualTo(level);
    }
  }

  @Test
  @DisplayName("Should provide comprehensive summary strings")
  void shouldProvideComprehensiveSummaryStrings() {
    // Test metadata summary
    final ModuleMetadataImpl metadata = ModuleMetadataImpl.builder().build();
    final String metadataSummary = metadata.toSummaryString();
    assertThat(metadataSummary).contains("ModuleMetadata");
    assertThat(metadataSummary).contains("format=");
    assertThat(metadataSummary).contains("platform=");

    // Test statistics summary
    final var stats = cache.getStatistics();
    final String statsSummary = stats.toSummaryString();
    assertThat(statsSummary).contains("CacheStatistics");
    assertThat(statsSummary).contains("requests=");
    assertThat(statsSummary).contains("hitRate=");

    // Test serialization options toString
    final SerializationOptions options = SerializationOptions.defaults();
    final String optionsString = options.toString();
    assertThat(optionsString).isNotBlank();
  }
}