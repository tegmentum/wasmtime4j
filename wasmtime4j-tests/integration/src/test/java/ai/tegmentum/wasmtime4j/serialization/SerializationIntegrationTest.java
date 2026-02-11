/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for serialization package.
 *
 * <p>This test class validates the serialization enums, classes, and interfaces.
 */
@DisplayName("Serialization Integration Tests")
public class SerializationIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(SerializationIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Serialization Integration Tests");
  }

  @Nested
  @DisplayName("ModuleSerializationFormat Tests")
  class ModuleSerializationFormatTests {

    @Test
    @DisplayName("Should have all expected serialization formats")
    void shouldHaveAllExpectedSerializationFormats() {
      LOGGER.info("Testing ModuleSerializationFormat enum values");

      ModuleSerializationFormat[] formats = ModuleSerializationFormat.values();
      assertEquals(5, formats.length, "Should have 5 serialization formats");

      assertNotNull(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4, "COMPACT_BINARY_LZ4 should exist");
      assertNotNull(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP, "COMPACT_BINARY_GZIP should exist");
      assertNotNull(ModuleSerializationFormat.RAW_BINARY, "RAW_BINARY should exist");
      assertNotNull(ModuleSerializationFormat.STREAMING_BINARY, "STREAMING_BINARY should exist");
      assertNotNull(ModuleSerializationFormat.MEMORY_MAPPED, "MEMORY_MAPPED should exist");

      LOGGER.info("ModuleSerializationFormat enum values verified: " + formats.length);
    }

    @Test
    @DisplayName("Should have correct identifiers")
    void shouldHaveCorrectIdentifiers() {
      LOGGER.info("Testing ModuleSerializationFormat identifiers");

      assertEquals(
          "compact-binary-lz4",
          ModuleSerializationFormat.COMPACT_BINARY_LZ4.getIdentifier(),
          "COMPACT_BINARY_LZ4 identifier should match");
      assertEquals(
          "compact-binary-gzip",
          ModuleSerializationFormat.COMPACT_BINARY_GZIP.getIdentifier(),
          "COMPACT_BINARY_GZIP identifier should match");
      assertEquals(
          "raw-binary",
          ModuleSerializationFormat.RAW_BINARY.getIdentifier(),
          "RAW_BINARY identifier should match");
      assertEquals(
          "streaming-binary",
          ModuleSerializationFormat.STREAMING_BINARY.getIdentifier(),
          "STREAMING_BINARY identifier should match");
      assertEquals(
          "memory-mapped",
          ModuleSerializationFormat.MEMORY_MAPPED.getIdentifier(),
          "MEMORY_MAPPED identifier should match");

      LOGGER.info("ModuleSerializationFormat identifiers verified");
    }

    @Test
    @DisplayName("Should have correct file extensions")
    void shouldHaveCorrectFileExtensions() {
      LOGGER.info("Testing ModuleSerializationFormat file extensions");

      assertEquals(
          "cbz4",
          ModuleSerializationFormat.COMPACT_BINARY_LZ4.getFileExtension(),
          "COMPACT_BINARY_LZ4 extension should be cbz4");
      assertEquals(
          "cbgz",
          ModuleSerializationFormat.COMPACT_BINARY_GZIP.getFileExtension(),
          "COMPACT_BINARY_GZIP extension should be cbgz");
      assertEquals(
          "bin",
          ModuleSerializationFormat.RAW_BINARY.getFileExtension(),
          "RAW_BINARY extension should be bin");
      assertEquals(
          "stream",
          ModuleSerializationFormat.STREAMING_BINARY.getFileExtension(),
          "STREAMING_BINARY extension should be stream");
      assertEquals(
          "mmap",
          ModuleSerializationFormat.MEMORY_MAPPED.getFileExtension(),
          "MEMORY_MAPPED extension should be mmap");

      LOGGER.info("ModuleSerializationFormat file extensions verified");
    }

    @Test
    @DisplayName("Should report compression support correctly")
    void shouldReportCompressionSupportCorrectly() {
      LOGGER.info("Testing ModuleSerializationFormat compression support");

      assertTrue(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4.supportsCompression(),
          "COMPACT_BINARY_LZ4 should support compression");
      assertTrue(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP.supportsCompression(),
          "COMPACT_BINARY_GZIP should support compression");
      assertFalse(
          ModuleSerializationFormat.RAW_BINARY.supportsCompression(),
          "RAW_BINARY should not support compression");

      LOGGER.info("ModuleSerializationFormat compression support verified");
    }

    @Test
    @DisplayName("Should report streaming support correctly")
    void shouldReportStreamingSupportCorrectly() {
      LOGGER.info("Testing ModuleSerializationFormat streaming support");

      assertTrue(
          ModuleSerializationFormat.STREAMING_BINARY.supportsStreaming(),
          "STREAMING_BINARY should support streaming");
      assertTrue(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4.supportsStreaming(),
          "COMPACT_BINARY_LZ4 should support streaming");
      assertFalse(
          ModuleSerializationFormat.RAW_BINARY.supportsStreaming(),
          "RAW_BINARY should not support streaming");

      LOGGER.info("ModuleSerializationFormat streaming support verified");
    }

    @Test
    @DisplayName("Should report high compression support correctly")
    void shouldReportHighCompressionSupportCorrectly() {
      LOGGER.info("Testing ModuleSerializationFormat high compression support");

      assertTrue(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP.supportsHighCompression(),
          "COMPACT_BINARY_GZIP should support high compression");
      assertFalse(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4.supportsHighCompression(),
          "COMPACT_BINARY_LZ4 should not support high compression");

      LOGGER.info("ModuleSerializationFormat high compression support verified");
    }

    @Test
    @DisplayName("Should support fromIdentifier lookup")
    void shouldSupportFromIdentifierLookup() {
      LOGGER.info("Testing ModuleSerializationFormat fromIdentifier");

      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4,
          ModuleSerializationFormat.fromIdentifier("compact-binary-lz4"),
          "Should find COMPACT_BINARY_LZ4 by identifier");
      assertEquals(
          ModuleSerializationFormat.RAW_BINARY,
          ModuleSerializationFormat.fromIdentifier("raw-binary"),
          "Should find RAW_BINARY by identifier");

      LOGGER.info("ModuleSerializationFormat fromIdentifier verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid identifier")
    void shouldThrowExceptionForInvalidIdentifier() {
      LOGGER.info("Testing ModuleSerializationFormat invalid identifier");

      assertThrows(
          IllegalArgumentException.class,
          () -> ModuleSerializationFormat.fromIdentifier("invalid"),
          "Should throw for invalid identifier");
      assertThrows(
          NullPointerException.class,
          () -> ModuleSerializationFormat.fromIdentifier(null),
          "Should throw for null identifier");

      LOGGER.info("ModuleSerializationFormat invalid identifier handling verified");
    }
  }

  @Nested
  @DisplayName("SerializationUseCase Tests")
  class SerializationUseCaseTests {

    @Test
    @DisplayName("Should have all expected use cases")
    void shouldHaveAllExpectedUseCases() {
      LOGGER.info("Testing SerializationUseCase enum values");

      ModuleSerializationFormat.SerializationUseCase[] useCases =
          ModuleSerializationFormat.SerializationUseCase.values();
      assertEquals(6, useCases.length, "Should have 6 use cases");

      assertNotNull(
          ModuleSerializationFormat.SerializationUseCase.MEMORY_CACHE, "MEMORY_CACHE should exist");
      assertNotNull(
          ModuleSerializationFormat.SerializationUseCase.DISK_CACHE, "DISK_CACHE should exist");
      assertNotNull(
          ModuleSerializationFormat.SerializationUseCase.NETWORK_TRANSMISSION,
          "NETWORK_TRANSMISSION should exist");
      assertNotNull(
          ModuleSerializationFormat.SerializationUseCase.LONG_TERM_STORAGE,
          "LONG_TERM_STORAGE should exist");
      assertNotNull(
          ModuleSerializationFormat.SerializationUseCase.LARGE_MODULES,
          "LARGE_MODULES should exist");
      assertNotNull(
          ModuleSerializationFormat.SerializationUseCase.MEMORY_CONSTRAINED,
          "MEMORY_CONSTRAINED should exist");

      LOGGER.info("SerializationUseCase enum values verified: " + useCases.length);
    }

    @Test
    @DisplayName("Should get optimal format for each use case")
    void shouldGetOptimalFormatForEachUseCase() {
      LOGGER.info("Testing getOptimalFormat for each use case");

      assertEquals(
          ModuleSerializationFormat.RAW_BINARY,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.MEMORY_CACHE),
          "MEMORY_CACHE should use RAW_BINARY");
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.DISK_CACHE),
          "DISK_CACHE should use COMPACT_BINARY_LZ4");
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.NETWORK_TRANSMISSION),
          "NETWORK_TRANSMISSION should use COMPACT_BINARY_GZIP");
      assertEquals(
          ModuleSerializationFormat.STREAMING_BINARY,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.LARGE_MODULES),
          "LARGE_MODULES should use STREAMING_BINARY");
      assertEquals(
          ModuleSerializationFormat.MEMORY_MAPPED,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.MEMORY_CONSTRAINED),
          "MEMORY_CONSTRAINED should use MEMORY_MAPPED");

      LOGGER.info("getOptimalFormat verified for each use case");
    }
  }

  @Nested
  @DisplayName("SerializationOptions Tests")
  class SerializationOptionsTests {

    @Test
    @DisplayName("Should verify SerializationOptions class exists")
    void shouldVerifySerializationOptionsClassExists() {
      LOGGER.info("Testing SerializationOptions class existence");

      assertNotNull(SerializationOptions.class, "SerializationOptions class should exist");

      LOGGER.info("SerializationOptions class verified");
    }
  }

  @Nested
  @DisplayName("SerializationResult Tests")
  class SerializationResultTests {

    @Test
    @DisplayName("Should verify SerializationResult class exists")
    void shouldVerifySerializationResultClassExists() {
      LOGGER.info("Testing SerializationResult class existence");

      assertNotNull(SerializationResult.class, "SerializationResult class should exist");

      LOGGER.info("SerializationResult class verified");
    }
  }

  @Nested
  @DisplayName("SerializedModuleMetadata Tests")
  class SerializedModuleMetadataTests {

    @Test
    @DisplayName("Should verify SerializedModuleMetadata class exists")
    void shouldVerifySerializedModuleMetadataClassExists() {
      LOGGER.info("Testing SerializedModuleMetadata class existence");

      assertNotNull(SerializedModuleMetadata.class, "SerializedModuleMetadata class should exist");

      LOGGER.info("SerializedModuleMetadata class verified");
    }
  }

  @Nested
  @DisplayName("CacheStatistics Tests")
  class CacheStatisticsTests {

    @Test
    @DisplayName("Should verify CacheStatistics class exists")
    void shouldVerifyCacheStatisticsClassExists() {
      LOGGER.info("Testing CacheStatistics class existence");

      assertNotNull(CacheStatistics.class, "CacheStatistics class should exist");

      LOGGER.info("CacheStatistics class verified");
    }
  }

  @Nested
  @DisplayName("CacheConfiguration Tests")
  class CacheConfigurationTests {

    @Test
    @DisplayName("Should verify CacheConfiguration class exists")
    void shouldVerifyCacheConfigurationClassExists() {
      LOGGER.info("Testing CacheConfiguration class existence");

      assertNotNull(CacheConfiguration.class, "CacheConfiguration class should exist");

      LOGGER.info("CacheConfiguration class verified");
    }
  }

  @Nested
  @DisplayName("ModuleSerializationEngine Tests")
  class ModuleSerializationEngineTests {

    @Test
    @DisplayName("Should verify ModuleSerializationEngine class exists")
    void shouldVerifyModuleSerializationEngineClassExists() {
      LOGGER.info("Testing ModuleSerializationEngine class existence");

      assertNotNull(
          ModuleSerializationEngine.class, "ModuleSerializationEngine class should exist");
      assertFalse(
          ModuleSerializationEngine.class.isInterface(),
          "ModuleSerializationEngine should be a class");

      LOGGER.info("ModuleSerializationEngine class verified");
    }
  }

  @Nested
  @DisplayName("ModuleSerializationCache Tests")
  class ModuleSerializationCacheTests {

    @Test
    @DisplayName("Should verify ModuleSerializationCache class exists")
    void shouldVerifyModuleSerializationCacheClassExists() {
      LOGGER.info("Testing ModuleSerializationCache class existence");

      assertNotNull(ModuleSerializationCache.class, "ModuleSerializationCache class should exist");
      assertFalse(
          ModuleSerializationCache.class.isInterface(),
          "ModuleSerializationCache should be a class");

      LOGGER.info("ModuleSerializationCache class verified");
    }
  }

  @Nested
  @DisplayName("SerializationPerformanceMetrics Tests")
  class SerializationPerformanceMetricsTests {

    @Test
    @DisplayName("Should verify SerializationPerformanceMetrics class exists")
    void shouldVerifySerializationPerformanceMetricsClassExists() {
      LOGGER.info("Testing SerializationPerformanceMetrics class existence");

      assertNotNull(
          SerializationPerformanceMetrics.class,
          "SerializationPerformanceMetrics class should exist");

      LOGGER.info("SerializationPerformanceMetrics class verified");
    }
  }
}
