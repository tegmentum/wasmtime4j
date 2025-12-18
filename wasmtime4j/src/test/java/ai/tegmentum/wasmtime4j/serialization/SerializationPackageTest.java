/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the serialization package classes.
 *
 * <p>This package provides WebAssembly module serialization, caching, and performance metrics.
 */
@DisplayName("Serialization Package Tests")
class SerializationPackageTest {

  @Nested
  @DisplayName("CacheStatistics Tests")
  class CacheStatisticsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CacheStatistics.class.getModifiers()),
          "CacheStatistics should be final");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CacheStatistics.class.getConstructor(
              long.class,
              long.class,
              long.class,
              long.class,
              long.class,
              double.class,
              long.class,
              long.class,
              long.class);
      assertNotNull(constructor, "Constructor should exist");
    }

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 10L, 5L, 0.95, 1000L, 1024L * 1024L, 512L * 1024L);

      assertEquals(100L, stats.getMemoryHits(), "Memory hits should match");
      assertEquals(50L, stats.getDiskHits(), "Disk hits should match");
      assertEquals(25L, stats.getDistributedHits(), "Distributed hits should match");
      assertEquals(10L, stats.getMisses(), "Misses should match");
      assertEquals(5L, stats.getEvictions(), "Evictions should match");
      assertEquals(0.95, stats.getHitRatio(), 0.001, "Hit ratio should match");
    }

    @Test
    @DisplayName("should calculate total hits correctly")
    void shouldCalculateTotalHitsCorrectly() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 10L, 5L, 0.95, 1000L, 1024L, 512L);

      assertEquals(175L, stats.getTotalHits(), "Total hits should be sum of all hit types");
    }

    @Test
    @DisplayName("should calculate total requests correctly")
    void shouldCalculateTotalRequestsCorrectly() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 10L, 5L, 0.95, 1000L, 1024L, 512L);

      assertEquals(185L, stats.getTotalRequests(), "Total requests should be hits + misses");
    }

    @Test
    @DisplayName("should calculate memory hit ratio correctly")
    void shouldCalculateMemoryHitRatioCorrectly() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 10L, 5L, 0.95, 1000L, 1024L, 512L);

      final double expected = 100.0 / 185.0;
      assertEquals(
          expected, stats.getMemoryHitRatio(), 0.001, "Memory hit ratio should be correct");
    }

    @Test
    @DisplayName("should calculate disk cache size in MB")
    void shouldCalculateDiskCacheSizeInMB() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 10L, 5L, 0.95, 1000L, 1024L * 1024L, 512L);

      assertEquals(1.0, stats.getDiskCacheSizeMB(), 0.001, "Disk cache size MB should match");
    }

    @Test
    @DisplayName("should calculate total cache size")
    void shouldCalculateTotalCacheSize() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 10L, 5L, 0.95, 1000L, 1024L, 512L);

      assertEquals(
          1536L,
          stats.getTotalCacheSizeBytes(),
          "Total cache size should be sum of disk and memory");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 10L, 5L, 0.95, 1000L, 1024L, 512L);

      assertNotNull(stats.toString(), "toString should not return null");
      assertFalse(stats.toString().isEmpty(), "toString should not be empty");
    }

    @Test
    @DisplayName("should handle zero requests for hit ratio calculation")
    void shouldHandleZeroRequestsForHitRatioCalculation() {
      final CacheStatistics stats = new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L, 0L);

      assertEquals(
          0.0, stats.getMemoryHitRatio(), 0.001, "Memory hit ratio should be 0 with no requests");
      assertEquals(
          0.0, stats.getDiskHitRatio(), 0.001, "Disk hit ratio should be 0 with no requests");
    }
  }

  @Nested
  @DisplayName("ModuleSerializationFormat Tests")
  class ModuleSerializationFormatTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          ModuleSerializationFormat.class.isEnum(), "ModuleSerializationFormat should be an enum");
    }

    @Test
    @DisplayName("should have all expected formats")
    void shouldHaveAllExpectedFormats() {
      final ModuleSerializationFormat[] formats = ModuleSerializationFormat.values();

      assertEquals(5, formats.length, "Should have 5 serialization formats");
    }

    @Test
    @DisplayName("should have COMPACT_BINARY_LZ4 format")
    void shouldHaveCompactBinaryLz4Format() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.COMPACT_BINARY_LZ4;

      assertEquals("compact-binary-lz4", format.getIdentifier(), "Identifier should match");
      assertEquals("cbz4", format.getFileExtension(), "File extension should match");
      assertTrue(format.supportsStreaming(), "Should support streaming");
      assertTrue(format.supportsCompression(), "Should support compression");
      assertFalse(format.supportsHighCompression(), "Should not support high compression");
    }

    @Test
    @DisplayName("should have COMPACT_BINARY_GZIP format")
    void shouldHaveCompactBinaryGzipFormat() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.COMPACT_BINARY_GZIP;

      assertEquals("compact-binary-gzip", format.getIdentifier(), "Identifier should match");
      assertEquals("cbgz", format.getFileExtension(), "File extension should match");
      assertTrue(format.supportsHighCompression(), "Should support high compression");
    }

    @Test
    @DisplayName("should have RAW_BINARY format")
    void shouldHaveRawBinaryFormat() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.RAW_BINARY;

      assertEquals("raw-binary", format.getIdentifier(), "Identifier should match");
      assertEquals("bin", format.getFileExtension(), "File extension should match");
      assertFalse(format.supportsCompression(), "Should not support compression");
    }

    @Test
    @DisplayName("should parse format from identifier")
    void shouldParseFormatFromIdentifier() {
      final ModuleSerializationFormat format =
          ModuleSerializationFormat.fromIdentifier("compact-binary-lz4");

      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4, format, "Should parse correct format");
    }

    @Test
    @DisplayName("should throw on invalid identifier")
    void shouldThrowOnInvalidIdentifier() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ModuleSerializationFormat.fromIdentifier("invalid-format"),
          "Should throw on invalid identifier");
    }

    @Test
    @DisplayName("should throw on null identifier")
    void shouldThrowOnNullIdentifier() {
      assertThrows(
          NullPointerException.class,
          () -> ModuleSerializationFormat.fromIdentifier(null),
          "Should throw on null identifier");
    }

    @Test
    @DisplayName("should get optimal format for memory cache use case")
    void shouldGetOptimalFormatForMemoryCache() {
      final ModuleSerializationFormat format =
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.MEMORY_CACHE);

      assertEquals(
          ModuleSerializationFormat.RAW_BINARY,
          format,
          "Should return RAW_BINARY for memory cache");
    }

    @Test
    @DisplayName("should get optimal format for disk cache use case")
    void shouldGetOptimalFormatForDiskCache() {
      final ModuleSerializationFormat format =
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.DISK_CACHE);

      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4,
          format,
          "Should return COMPACT_BINARY_LZ4 for disk cache");
    }

    @Test
    @DisplayName("should get optimal format for network transmission")
    void shouldGetOptimalFormatForNetworkTransmission() {
      final ModuleSerializationFormat format =
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.NETWORK_TRANSMISSION);

      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP,
          format,
          "Should return COMPACT_BINARY_GZIP for network");
    }

    @Test
    @DisplayName("should throw on null use case")
    void shouldThrowOnNullUseCase() {
      assertThrows(
          NullPointerException.class,
          () -> ModuleSerializationFormat.getOptimalFormat(null),
          "Should throw on null use case");
    }
  }

  @Nested
  @DisplayName("SerializationOptions Tests")
  class SerializationOptionsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(SerializationOptions.class.getModifiers()),
          "SerializationOptions should be final");
    }

    @Test
    @DisplayName("should create default options")
    void shouldCreateDefaultOptions() {
      final SerializationOptions options = SerializationOptions.createDefault();

      assertNotNull(options, "Default options should not be null");
      assertTrue(options.isIncludeChecksum(), "Default should include checksum");
      assertFalse(options.isPreserveDebugInfo(), "Default should not preserve debug info");
    }

    @Test
    @DisplayName("should create production options")
    void shouldCreateProductionOptions() {
      final SerializationOptions options = SerializationOptions.createProduction();

      assertNotNull(options, "Production options should not be null");
      assertTrue(options.isIncludeChecksum(), "Production should include checksum");
      assertTrue(options.isVerifyIntegrity(), "Production should verify integrity");
      assertTrue(options.isEnableStreaming(), "Production should enable streaming");
    }

    @Test
    @DisplayName("should create development options")
    void shouldCreateDevelopmentOptions() {
      final SerializationOptions options = SerializationOptions.createDevelopment();

      assertNotNull(options, "Development options should not be null");
      assertTrue(options.isPreserveDebugInfo(), "Development should preserve debug info");
      assertTrue(options.isPreserveNameSection(), "Development should preserve name section");
      assertFalse(options.isCleanupTempFiles(), "Development should not cleanup temp files");
    }

    @Test
    @DisplayName("should create high performance options")
    void shouldCreateHighPerformanceOptions() {
      final SerializationOptions options = SerializationOptions.createHighPerformance();

      assertNotNull(options, "High performance options should not be null");
      assertFalse(options.isIncludeChecksum(), "High performance should skip checksum");
      assertEquals(
          1, options.getCompressionLevel(), "High performance should use fast compression");
      assertTrue(
          options.isUseMemoryMappedFiles(), "High performance should use memory mapped files");
    }

    @Test
    @DisplayName("should create secure options with encryption key")
    void shouldCreateSecureOptionsWithEncryptionKey() {
      final byte[] encryptionKey = new byte[32];
      final SerializationOptions options = SerializationOptions.createSecure(encryptionKey);

      assertNotNull(options, "Secure options should not be null");
      assertTrue(options.isEncryptSerialization(), "Secure should enable encryption");
      assertTrue(options.isVerifyIntegrity(), "Secure should verify integrity");
      assertNotNull(options.getEncryptionKey(), "Encryption key should not be null");
    }

    @Test
    @DisplayName("should throw on null encryption key for secure options")
    void shouldThrowOnNullEncryptionKeyForSecureOptions() {
      assertThrows(
          NullPointerException.class,
          () -> SerializationOptions.createSecure(null),
          "Should throw on null encryption key");
    }

    @Test
    @DisplayName("should build options with builder")
    void shouldBuildOptionsWithBuilder() {
      final SerializationOptions options =
          new SerializationOptions.Builder()
              .includeChecksum(true)
              .preserveDebugInfo(true)
              .setBufferSize(128 * 1024)
              .setCompressionLevel(5)
              .build();

      assertTrue(options.isIncludeChecksum(), "Should have checksum enabled");
      assertTrue(options.isPreserveDebugInfo(), "Should preserve debug info");
      assertEquals(128 * 1024, options.getBufferSize(), "Buffer size should match");
      assertEquals(5, options.getCompressionLevel(), "Compression level should match");
    }

    @Test
    @DisplayName("should reject invalid compression level")
    void shouldRejectInvalidCompressionLevel() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().setCompressionLevel(10).build(),
          "Should reject compression level > 9");

      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().setCompressionLevel(-1).build(),
          "Should reject negative compression level");
    }

    @Test
    @DisplayName("should reject zero buffer size")
    void shouldRejectZeroBufferSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().setBufferSize(0).build(),
          "Should reject zero buffer size");
    }

    @Test
    @DisplayName("should require encryption key when encryption is enabled")
    void shouldRequireEncryptionKeyWhenEncryptionEnabled() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().encryptSerialization(true).build(),
          "Should require encryption key when encryption enabled");
    }

    @Test
    @DisplayName("should support custom metadata")
    void shouldSupportCustomMetadata() {
      final SerializationOptions options =
          new SerializationOptions.Builder()
              .addCustomMetadata("key1", "value1")
              .addCustomMetadata("key2", "value2")
              .build();

      final Map<String, String> metadata = options.getCustomMetadata();
      assertEquals("value1", metadata.get("key1"), "Should have custom metadata key1");
      assertEquals("value2", metadata.get("key2"), "Should have custom metadata key2");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final SerializationOptions options = SerializationOptions.createDefault();

      assertNotNull(options.toString(), "toString should not return null");
      assertFalse(options.toString().isEmpty(), "toString should not be empty");
    }
  }

  @Nested
  @DisplayName("CacheConfiguration Tests")
  class CacheConfigurationTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CacheConfiguration.class.getModifiers()),
          "CacheConfiguration should be final");
    }

    @Test
    @DisplayName("should create default configuration")
    void shouldCreateDefaultConfiguration() {
      final CacheConfiguration config = CacheConfiguration.createDefault();

      assertNotNull(config, "Default configuration should not be null");
      assertEquals(1000, config.getMaxMemoryEntries(), "Default max memory entries");
      assertEquals(Duration.ofHours(1), config.getMemoryCacheTtl(), "Default memory TTL");
      assertFalse(config.isDiskCacheEnabled(), "Default disk cache should be disabled");
    }

    @Test
    @DisplayName("should create production configuration")
    void shouldCreateProductionConfiguration() {
      final CacheConfiguration config = CacheConfiguration.createProduction();

      assertNotNull(config, "Production configuration should not be null");
      assertEquals(10_000, config.getMaxMemoryEntries(), "Production max memory entries");
      assertTrue(config.isDiskCacheEnabled(), "Production should enable disk cache");
      assertEquals(Duration.ofDays(7), config.getDiskCacheTtl(), "Production disk TTL");
    }

    @Test
    @DisplayName("should create memory only configuration")
    void shouldCreateMemoryOnlyConfiguration() {
      final CacheConfiguration config = CacheConfiguration.createMemoryOnly();

      assertNotNull(config, "Memory only configuration should not be null");
      assertEquals(1000, config.getMaxMemoryEntries(), "Memory only max entries");
      assertFalse(config.isDiskCacheEnabled(), "Memory only should disable disk cache");
      assertFalse(
          config.isDistributedCacheEnabled(), "Memory only should disable distributed cache");
    }

    @Test
    @DisplayName("should create high performance configuration")
    void shouldCreateHighPerformanceConfiguration() {
      final CacheConfiguration config = CacheConfiguration.createHighPerformance();

      assertNotNull(config, "High performance configuration should not be null");
      assertEquals(50_000, config.getMaxMemoryEntries(), "High performance max entries");
      assertTrue(config.isPreloadCacheOnStartup(), "High performance should preload cache");
      assertEquals(2, config.getMaintenanceThreads(), "High performance maintenance threads");
    }

    @Test
    @DisplayName("should build configuration with builder")
    void shouldBuildConfigurationWithBuilder() {
      final Path diskPath = Paths.get(System.getProperty("java.io.tmpdir"), "test-cache");
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(5000)
              .setMaxMemoryUsage(256 * 1024 * 1024)
              .setMemoryCacheTtl(Duration.ofHours(2))
              .enableDiskCache(diskPath)
              .setDiskCacheTtl(Duration.ofDays(3))
              .setMaintenanceInterval(Duration.ofMinutes(5))
              .build();

      assertEquals(5000, config.getMaxMemoryEntries(), "Max entries should match");
      assertEquals(256 * 1024 * 1024, config.getMaxMemoryUsageBytes(), "Max memory should match");
      assertTrue(config.isDiskCacheEnabled(), "Disk cache should be enabled");
      assertEquals(diskPath, config.getDiskCacheDirectory(), "Disk directory should match");
    }

    @Test
    @DisplayName("should disable disk cache")
    void shouldDisableDiskCache() {
      final CacheConfiguration config = new CacheConfiguration.Builder().disableDiskCache().build();

      assertFalse(config.isDiskCacheEnabled(), "Disk cache should be disabled");
      assertNull(config.getDiskCacheDirectory(), "Disk directory should be null");
    }

    @Test
    @DisplayName("should enable encryption")
    void shouldEnableEncryption() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableEncryption("AES/GCM/NoPadding").build();

      assertTrue(config.isEncryptCacheEntries(), "Encryption should be enabled");
      assertEquals("AES/GCM/NoPadding", config.getEncryptionAlgorithm(), "Algorithm should match");
    }

    @Test
    @DisplayName("should reject non-positive max memory entries")
    void shouldRejectNonPositiveMaxMemoryEntries() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CacheConfiguration.Builder().setMaxMemoryEntries(0).build(),
          "Should reject zero max entries");

      assertThrows(
          IllegalArgumentException.class,
          () -> new CacheConfiguration.Builder().setMaxMemoryEntries(-1).build(),
          "Should reject negative max entries");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final CacheConfiguration config = CacheConfiguration.createDefault();

      assertNotNull(config.toString(), "toString should not return null");
      assertFalse(config.toString().isEmpty(), "toString should not be empty");
    }
  }

  @Nested
  @DisplayName("SerializationPerformanceMetrics Tests")
  class SerializationPerformanceMetricsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(SerializationPerformanceMetrics.class.getModifiers()),
          "SerializationPerformanceMetrics should be final");
    }

    @Test
    @DisplayName("should build metrics with timing")
    void shouldBuildMetricsWithTiming() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(1_000_000L, 500_000L, 200_000L, 100_000L, 50_000L)
              .build();

      assertEquals(1_000_000L, metrics.getSerializationTimeNs(), "Serialization time should match");
      assertEquals(
          500_000L, metrics.getDeserializationTimeNs(), "Deserialization time should match");
      assertEquals(1L, metrics.getSerializationTimeMs(), "Serialization time ms should be correct");
    }

    @Test
    @DisplayName("should build metrics with throughput")
    void shouldBuildMetricsWithThroughput() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setThroughputMetrics(100.0, 150.0, 80.0, 120.0)
              .build();

      assertEquals(
          100.0,
          metrics.getSerializationThroughputMbps(),
          0.001,
          "Serialization throughput should match");
      assertEquals(
          150.0,
          metrics.getDeserializationThroughputMbps(),
          0.001,
          "Deserialization throughput should match");
    }

    @Test
    @DisplayName("should build metrics with memory usage")
    void shouldBuildMetricsWithMemoryUsage() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setMemoryMetrics(1024L * 1024L, 512L * 1024L, 256L * 1024L)
              .build();

      assertEquals(1024L * 1024L, metrics.getPeakMemoryUsageBytes(), "Peak memory should match");
      assertEquals(512L * 1024L, metrics.getAvgMemoryUsageBytes(), "Avg memory should match");
    }

    @Test
    @DisplayName("should build metrics with CPU usage")
    void shouldBuildMetricsWithCpuUsage() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder().setCpuMetrics(50.0, 80.0).build();

      assertEquals(50.0, metrics.getAvgCpuUsagePercent(), 0.001, "Avg CPU should match");
      assertEquals(80.0, metrics.getPeakCpuUsagePercent(), 0.001, "Peak CPU should match");
    }

    @Test
    @DisplayName("should reject invalid CPU percentages")
    void shouldRejectInvalidCpuPercentages() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().setCpuMetrics(-1.0, 50.0).build(),
          "Should reject negative avg CPU");

      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().setCpuMetrics(50.0, 101.0).build(),
          "Should reject peak CPU > 100");
    }

    @Test
    @DisplayName("should build metrics with IO metrics")
    void shouldBuildMetricsWithIoMetrics() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setIoMetrics(1024L, 2048L, 100_000L)
              .build();

      assertEquals(1024L, metrics.getBytesRead(), "Bytes read should match");
      assertEquals(2048L, metrics.getBytesWritten(), "Bytes written should match");
      assertEquals(100_000L, metrics.getDiskIoTimeNs(), "Disk IO time should match");
    }

    @Test
    @DisplayName("should calculate total operation time")
    void shouldCalculateTotalOperationTime() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(1_000_000L, 500_000L, 200_000L, 100_000L, 50_000L)
              .setIoMetrics(0L, 0L, 100_000L)
              .build();

      final long expected = 1_000_000L + 200_000L + 50_000L + 100_000L;
      assertEquals(
          expected, metrics.getTotalOperationTimeNs(), "Total operation time should be correct");
    }

    @Test
    @DisplayName("should calculate memory efficiency ratio")
    void shouldCalculateMemoryEfficiencyRatio() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setMemoryMetrics(1024L, 512L, 256L)
              .setIoMetrics(0L, 512L, 0L)
              .build();

      assertEquals(
          0.5, metrics.getMemoryEfficiencyRatio(), 0.001, "Memory efficiency should be 0.5");
    }

    @Test
    @DisplayName("should determine optimal performance")
    void shouldDetermineOptimalPerformance() {
      final SerializationPerformanceMetrics optimalMetrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L) // 100ms
              .setThroughputMetrics(50.0, 100.0, 0.0, 0.0)
              .setMemoryMetrics(1024L, 512L, 256L)
              .setIoMetrics(0L, 512L, 0L)
              .build();

      assertTrue(optimalMetrics.isOptimalPerformance(), "Should be optimal performance");
    }

    @Test
    @DisplayName("should get performance summary")
    void shouldGetPerformanceSummary() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(1_000_000_000L, 500_000_000L, 0L, 0L, 0L) // 1 second
              .setThroughputMetrics(100.0, 200.0, 0.0, 0.0)
              .setMemoryMetrics(1024L * 1024L, 512L * 1024L, 256L * 1024L)
              .setCpuMetrics(50.0, 80.0)
              .setCompressionEfficiency(2.0)
              .build();

      final String summary = metrics.getPerformanceSummary();
      assertNotNull(summary, "Performance summary should not be null");
      assertFalse(summary.isEmpty(), "Performance summary should not be empty");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder().build();

      assertNotNull(metrics.toString(), "toString should not return null");
    }
  }

  @Nested
  @DisplayName("SerializedModuleMetadata Tests")
  class SerializedModuleMetadataTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(SerializedModuleMetadata.class.getModifiers()),
          "SerializedModuleMetadata should be final");
    }

    @Test
    @DisplayName("should build metadata with basic fields")
    void shouldBuildMetadataWithBasicFields() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setFormatVersion("1.0")
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(1024L)
              .setOriginalSize(2048L)
              .setSha256Hash("abc123")
              .build();

      assertEquals("1.0", metadata.getFormatVersion(), "Format version should match");
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4,
          metadata.getFormat(),
          "Format should match");
      assertEquals(1024L, metadata.getSerializedSize(), "Serialized size should match");
      assertEquals(2048L, metadata.getOriginalSize(), "Original size should match");
      assertEquals("abc123", metadata.getSha256Hash(), "Hash should match");
    }

    @Test
    @DisplayName("should build metadata with module info")
    void shouldBuildMetadataWithModuleInfo() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setModuleName("test-module")
              .setModuleVersion("1.0.0")
              .setCounts(5, 10, 20, 2, 1, 3)
              .build();

      assertEquals("test-module", metadata.getModuleName(), "Module name should match");
      assertEquals("1.0.0", metadata.getModuleVersion(), "Module version should match");
      assertEquals(5, metadata.getImportCount(), "Import count should match");
      assertEquals(10, metadata.getExportCount(), "Export count should match");
      assertEquals(20, metadata.getFunctionCount(), "Function count should match");
    }

    @Test
    @DisplayName("should build metadata with encryption info")
    void shouldBuildMetadataWithEncryptionInfo() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setEncrypted(true, "AES/GCM/NoPadding").build();

      assertTrue(metadata.isEncrypted(), "Should be encrypted");
      assertEquals(
          "AES/GCM/NoPadding", metadata.getEncryptionAlgorithm(), "Algorithm should match");
    }

    @Test
    @DisplayName("should build metadata with debug info flags")
    void shouldBuildMetadataWithDebugInfoFlags() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setDebugInfo(true, true, true).build();

      assertTrue(metadata.hasSourceMaps(), "Should have source maps");
      assertTrue(metadata.hasDebugSymbols(), "Should have debug symbols");
      assertTrue(metadata.hasNameSection(), "Should have name section");
    }

    @Test
    @DisplayName("should build metadata with platform info")
    void shouldBuildMetadataWithPlatformInfo() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setWasmtimeVersion("36.0.2")
              .setJavaVersion("21.0.1")
              .setPlatformInfo("aarch64", "Mac OS X")
              .build();

      assertEquals("36.0.2", metadata.getWasmtimeVersion(), "Wasmtime version should match");
      assertEquals("21.0.1", metadata.getJavaVersion(), "Java version should match");
      assertEquals("aarch64", metadata.getPlatformArch(), "Platform arch should match");
      assertEquals("Mac OS X", metadata.getPlatformOs(), "Platform OS should match");
    }

    @Test
    @DisplayName("should build metadata with custom metadata")
    void shouldBuildMetadataWithCustomMetadata() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .addCustomMetadata("key1", "value1")
              .addCustomMetadata("key2", "value2")
              .build();

      final Map<String, String> customMetadata = metadata.getCustomMetadata();
      assertEquals("value1", customMetadata.get("key1"), "Custom metadata key1 should match");
      assertEquals("value2", customMetadata.get("key2"), "Custom metadata key2 should match");
    }

    @Test
    @DisplayName("should calculate compression ratio from sizes")
    void shouldCalculateCompressionRatioFromSizes() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setOriginalSize(2000L)
              .setSerializedSize(1000L)
              .build();

      assertEquals(2.0, metadata.getCompressionRatio(), 0.001, "Compression ratio should be 2.0");
    }

    @Test
    @DisplayName("should reject negative sizes")
    void shouldRejectNegativeSizes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setSerializedSize(-1).build(),
          "Should reject negative serialized size");

      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setOriginalSize(-1).build(),
          "Should reject negative original size");
    }

    @Test
    @DisplayName("should reject negative counts")
    void shouldRejectNegativeCounts() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCounts(-1, 0, 0, 0, 0, 0).build(),
          "Should reject negative import count");
    }

    @Test
    @DisplayName("should get estimated deserialization time")
    void shouldGetEstimatedDeserializationTime() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1024L)
              .setSerializationDuration(100L)
              .build();

      assertTrue(
          metadata.getEstimatedDeserializationTimeMs() > 0, "Estimated time should be positive");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      assertNotNull(metadata.toString(), "toString should not return null");
      assertFalse(metadata.toString().isEmpty(), "toString should not be empty");
    }
  }

  @Nested
  @DisplayName("SerializationResult Tests")
  class SerializationResultTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(SerializationResult.class.getModifiers()),
          "SerializationResult should be final");
    }

    @Test
    @DisplayName("should create result with data and metadata")
    void shouldCreateResultWithDataAndMetadata() {
      final byte[] data = new byte[] {1, 2, 3, 4, 5};
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setSerializedSize(5L).setOriginalSize(10L).build();

      final SerializationResult result = new SerializationResult(data, metadata);

      assertArrayEquals(data, result.getSerializedData(), "Data should match");
      assertEquals(metadata, result.getMetadata(), "Metadata should match");
      assertEquals(5L, result.getSize(), "Size should match");
    }

    @Test
    @DisplayName("should calculate size in MB")
    void shouldCalculateSizeInMB() {
      final byte[] data = new byte[1024 * 1024]; // 1 MB
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setSerializedSize(data.length).build();

      final SerializationResult result = new SerializationResult(data, metadata);

      assertEquals(1.0, result.getSizeMB(), 0.001, "Size should be 1.0 MB");
    }

    @Test
    @DisplayName("should get compression ratio from metadata")
    void shouldGetCompressionRatioFromMetadata() {
      final byte[] data = new byte[100];
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(100L)
              .setOriginalSize(200L)
              .build();

      final SerializationResult result = new SerializationResult(data, metadata);

      assertEquals(2.0, result.getCompressionRatio(), 0.001, "Compression ratio should be 2.0");
    }

    @Test
    @DisplayName("should throw on null data")
    void shouldThrowOnNullData() {
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      assertThrows(
          NullPointerException.class,
          () -> new SerializationResult(null, metadata),
          "Should throw on null data");
    }

    @Test
    @DisplayName("should throw on null metadata")
    void shouldThrowOnNullMetadata() {
      final byte[] data = new byte[] {1, 2, 3};

      assertThrows(
          NullPointerException.class,
          () -> new SerializationResult(data, null),
          "Should throw on null metadata");
    }

    @Test
    @DisplayName("should return defensive copy of data")
    void shouldReturnDefensiveCopyOfData() {
      final byte[] originalData = new byte[] {1, 2, 3, 4, 5};
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      final SerializationResult result = new SerializationResult(originalData, metadata);
      final byte[] returnedData = result.getSerializedData();

      // Modify returned data
      returnedData[0] = 99;

      // Original should not be affected
      assertEquals(1, result.getSerializedData()[0], "Data should be defensively copied");
    }

    @Test
    @DisplayName("should create copy with new metadata")
    void shouldCreateCopyWithNewMetadata() {
      final byte[] data = new byte[] {1, 2, 3};
      final SerializedModuleMetadata metadata1 =
          new SerializedModuleMetadata.Builder().setModuleName("module1").build();
      final SerializedModuleMetadata metadata2 =
          new SerializedModuleMetadata.Builder().setModuleName("module2").build();

      final SerializationResult result1 = new SerializationResult(data, metadata1);
      final SerializationResult result2 = result1.withMetadata(metadata2);

      assertEquals(
          "module1",
          result1.getMetadata().getModuleName(),
          "Original metadata should be unchanged");
      assertEquals(
          "module2", result2.getMetadata().getModuleName(), "New result should have new metadata");
      assertArrayEquals(
          result1.getSerializedData(), result2.getSerializedData(), "Data should be same");
    }

    @Test
    @DisplayName("should compare with another result")
    void shouldCompareWithAnotherResult() {
      final byte[] data1 = new byte[1000];
      final byte[] data2 = new byte[2000];
      final SerializedModuleMetadata metadata1 =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1000L)
              .setSerializationDuration(100L)
              .build();
      final SerializedModuleMetadata metadata2 =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(2000L)
              .setSerializationDuration(200L)
              .build();

      final SerializationResult result1 = new SerializationResult(data1, metadata1);
      final SerializationResult result2 = new SerializationResult(data2, metadata2);

      final String comparison = result1.compareWith(result2);

      assertNotNull(comparison, "Comparison should not be null");
      assertFalse(comparison.isEmpty(), "Comparison should not be empty");
      assertTrue(comparison.contains("Size"), "Comparison should mention size");
    }

    @Test
    @DisplayName("should get summary")
    void shouldGetSummary() {
      final byte[] data = new byte[1024];
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(1024L)
              .setOriginalSize(2048L)
              .setSerializationDuration(50L)
              .build();

      final SerializationResult result = new SerializationResult(data, metadata);
      final String summary = result.getSummary();

      assertNotNull(summary, "Summary should not be null");
      assertFalse(summary.isEmpty(), "Summary should not be empty");
      assertTrue(summary.contains("Format"), "Summary should mention format");
    }

    @Test
    @DisplayName("should get estimated deserialization time")
    void shouldGetEstimatedDeserializationTime() {
      final byte[] data = new byte[1024];
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1024L)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result = new SerializationResult(data, metadata);

      assertTrue(
          result.getEstimatedDeserializationTime() >= 0, "Estimated time should be non-negative");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final byte[] data = new byte[100];
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();
      final SerializationResult result = new SerializationResult(data, metadata);

      assertNotNull(result.toString(), "toString should not return null");
      assertFalse(result.toString().isEmpty(), "toString should not be empty");
    }

    @Test
    @DisplayName("equals should work correctly")
    void equalsShouldWorkCorrectly() {
      final byte[] data = new byte[] {1, 2, 3};
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      final SerializationResult result1 = new SerializationResult(data, metadata);
      final SerializationResult result2 = new SerializationResult(data, metadata);

      assertEquals(result1, result2, "Equal results should be equal");
      assertEquals(result1.hashCode(), result2.hashCode(), "Hash codes should be equal");
    }
  }

  @Nested
  @DisplayName("ModuleSerializationEngine Class Tests")
  class ModuleSerializationEngineTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(ModuleSerializationEngine.class.getModifiers()),
          "ModuleSerializationEngine should be a final class");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = ModuleSerializationEngine.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
    }

    @Test
    @DisplayName("should have executor constructor")
    void shouldHaveExecutorConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          ModuleSerializationEngine.class.getConstructor(Executor.class);
      assertNotNull(constructor, "Executor constructor should exist");
    }

    @Test
    @DisplayName("should have serialize method")
    void shouldHaveSerializeMethod() throws NoSuchMethodException {
      final Method method =
          ModuleSerializationEngine.class.getMethod(
              "serialize",
              ai.tegmentum.wasmtime4j.Module.class,
              ModuleSerializationFormat.class,
              SerializationOptions.class);
      assertNotNull(method, "serialize method should exist");
      assertEquals(
          SerializationResult.class, method.getReturnType(), "Should return SerializationResult");
    }

    @Test
    @DisplayName("should have deserialize method")
    void shouldHaveDeserializeMethod() throws NoSuchMethodException {
      final Method method =
          ModuleSerializationEngine.class.getMethod(
              "deserialize", byte[].class, SerializedModuleMetadata.class);
      assertNotNull(method, "deserialize method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have serializeToStream method")
    void shouldHaveSerializeToStreamMethod() throws NoSuchMethodException {
      final Method method =
          ModuleSerializationEngine.class.getMethod(
              "serializeToStream",
              ai.tegmentum.wasmtime4j.Module.class,
              java.io.OutputStream.class,
              ModuleSerializationFormat.class,
              SerializationOptions.class);
      assertNotNull(method, "serializeToStream method should exist");
      assertEquals(
          SerializedModuleMetadata.class,
          method.getReturnType(),
          "Should return SerializedModuleMetadata");
    }

    @Test
    @DisplayName("should have deserializeFromStream method")
    void shouldHaveDeserializeFromStreamMethod() throws NoSuchMethodException {
      final Method method =
          ModuleSerializationEngine.class.getMethod(
              "deserializeFromStream", java.io.InputStream.class, SerializedModuleMetadata.class);
      assertNotNull(method, "deserializeFromStream method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should create instance with default constructor")
    void shouldCreateInstanceWithDefaultConstructor() {
      final ModuleSerializationEngine engine = new ModuleSerializationEngine();
      assertNotNull(engine, "Engine should be created");
    }
  }

  @Nested
  @DisplayName("ModuleSerializationCache Class Tests")
  class ModuleSerializationCacheTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(ModuleSerializationCache.class.getModifiers()),
          "ModuleSerializationCache should be a final class");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ModuleSerializationCache.class),
          "ModuleSerializationCache should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have CacheConfiguration constructor")
    void shouldHaveCacheConfigurationConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          ModuleSerializationCache.class.getConstructor(CacheConfiguration.class);
      assertNotNull(constructor, "CacheConfiguration constructor should exist");
    }

    @Test
    @DisplayName("should have store method")
    void shouldHaveStoreMethod() throws NoSuchMethodException {
      final Method method =
          ModuleSerializationCache.class.getMethod(
              "store", byte[].class, SerializedModuleMetadata.class);
      assertNotNull(method, "store method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String (content hash)");
    }

    @Test
    @DisplayName("should have retrieve method")
    void shouldHaveRetrieveMethod() throws NoSuchMethodException {
      final Method method = ModuleSerializationCache.class.getMethod("retrieve", String.class);
      assertNotNull(method, "retrieve method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<CacheEntry>");
    }

    @Test
    @DisplayName("should have remove method")
    void shouldHaveRemoveMethod() throws NoSuchMethodException {
      final Method method = ModuleSerializationCache.class.getMethod("remove", String.class);
      assertNotNull(method, "remove method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = ModuleSerializationCache.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ModuleSerializationCache.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(CacheStatistics.class, method.getReturnType(), "Should return CacheStatistics");
    }

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method = ModuleSerializationCache.class.getMethod("contains", String.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have warmCache method")
    void shouldHaveWarmCacheMethod() throws NoSuchMethodException {
      final Method method =
          ModuleSerializationCache.class.getMethod("warmCache", Map.class, Map.class);
      assertNotNull(method, "warmCache method should exist");
    }

    @Test
    @DisplayName("should have DistributedCacheConnector nested interface")
    void shouldHaveDistributedCacheConnectorNestedInterface() {
      final Class<?>[] declaredClasses = ModuleSerializationCache.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("DistributedCacheConnector")) {
          found = true;
          assertTrue(clazz.isInterface(), "DistributedCacheConnector should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have DistributedCacheConnector nested interface");
    }

    @Test
    @DisplayName("should have CacheEntry nested class")
    void shouldHaveCacheEntryNestedClass() {
      final Class<?>[] declaredClasses = ModuleSerializationCache.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("CacheEntry")) {
          found = true;
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "CacheEntry should be a final class");
          break;
        }
      }
      assertTrue(found, "Should have CacheEntry nested class");
    }
  }
}
