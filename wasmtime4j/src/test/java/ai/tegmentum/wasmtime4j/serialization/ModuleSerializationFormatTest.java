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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleSerializationFormat} enum.
 *
 * <p>ModuleSerializationFormat defines the different serialization formats available for
 * WebAssembly modules, each optimized for specific use cases and performance characteristics.
 */
@DisplayName("ModuleSerializationFormat Tests")
class ModuleSerializationFormatTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have 5 serialization formats")
    void shouldHave5SerializationFormats() {
      assertEquals(5, ModuleSerializationFormat.values().length);
    }

    @Test
    @DisplayName("should have COMPACT_BINARY_LZ4 format")
    void shouldHaveCompactBinaryLz4Format() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.COMPACT_BINARY_LZ4;
      assertNotNull(format);
      assertEquals("compact-binary-lz4", format.getIdentifier());
      assertEquals("cbz4", format.getFileExtension());
      assertTrue(format.supportsStreaming());
      assertTrue(format.supportsCompression());
      assertFalse(format.supportsHighCompression());
    }

    @Test
    @DisplayName("should have COMPACT_BINARY_GZIP format")
    void shouldHaveCompactBinaryGzipFormat() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.COMPACT_BINARY_GZIP;
      assertNotNull(format);
      assertEquals("compact-binary-gzip", format.getIdentifier());
      assertEquals("cbgz", format.getFileExtension());
      assertTrue(format.supportsStreaming());
      assertTrue(format.supportsCompression());
      assertTrue(format.supportsHighCompression());
    }

    @Test
    @DisplayName("should have RAW_BINARY format")
    void shouldHaveRawBinaryFormat() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.RAW_BINARY;
      assertNotNull(format);
      assertEquals("raw-binary", format.getIdentifier());
      assertEquals("bin", format.getFileExtension());
      assertFalse(format.supportsStreaming());
      assertFalse(format.supportsCompression());
      assertFalse(format.supportsHighCompression());
    }

    @Test
    @DisplayName("should have STREAMING_BINARY format")
    void shouldHaveStreamingBinaryFormat() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.STREAMING_BINARY;
      assertNotNull(format);
      assertEquals("streaming-binary", format.getIdentifier());
      assertEquals("stream", format.getFileExtension());
      assertTrue(format.supportsStreaming());
      assertFalse(format.supportsCompression());
      assertFalse(format.supportsHighCompression());
    }

    @Test
    @DisplayName("should have MEMORY_MAPPED format")
    void shouldHaveMemoryMappedFormat() {
      final ModuleSerializationFormat format = ModuleSerializationFormat.MEMORY_MAPPED;
      assertNotNull(format);
      assertEquals("memory-mapped", format.getIdentifier());
      assertEquals("mmap", format.getFileExtension());
      assertFalse(format.supportsStreaming());
      assertFalse(format.supportsCompression());
      assertFalse(format.supportsHighCompression());
    }
  }

  @Nested
  @DisplayName("fromIdentifier Tests")
  class FromIdentifierTests {

    @Test
    @DisplayName("fromIdentifier should return COMPACT_BINARY_LZ4")
    void fromIdentifierShouldReturnCompactBinaryLz4() {
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4,
          ModuleSerializationFormat.fromIdentifier("compact-binary-lz4"));
    }

    @Test
    @DisplayName("fromIdentifier should return COMPACT_BINARY_GZIP")
    void fromIdentifierShouldReturnCompactBinaryGzip() {
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP,
          ModuleSerializationFormat.fromIdentifier("compact-binary-gzip"));
    }

    @Test
    @DisplayName("fromIdentifier should return RAW_BINARY")
    void fromIdentifierShouldReturnRawBinary() {
      assertEquals(
          ModuleSerializationFormat.RAW_BINARY,
          ModuleSerializationFormat.fromIdentifier("raw-binary"));
    }

    @Test
    @DisplayName("fromIdentifier should return STREAMING_BINARY")
    void fromIdentifierShouldReturnStreamingBinary() {
      assertEquals(
          ModuleSerializationFormat.STREAMING_BINARY,
          ModuleSerializationFormat.fromIdentifier("streaming-binary"));
    }

    @Test
    @DisplayName("fromIdentifier should return MEMORY_MAPPED")
    void fromIdentifierShouldReturnMemoryMapped() {
      assertEquals(
          ModuleSerializationFormat.MEMORY_MAPPED,
          ModuleSerializationFormat.fromIdentifier("memory-mapped"));
    }

    @Test
    @DisplayName("fromIdentifier should throw on null")
    void fromIdentifierShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class, () -> ModuleSerializationFormat.fromIdentifier(null));
    }

    @Test
    @DisplayName("fromIdentifier should throw on unknown identifier")
    void fromIdentifierShouldThrowOnUnknownIdentifier() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ModuleSerializationFormat.fromIdentifier("unknown-format"));

      assertTrue(exception.getMessage().contains("Unknown serialization format"));
      assertTrue(exception.getMessage().contains("unknown-format"));
    }
  }

  @Nested
  @DisplayName("getOptimalFormat Tests")
  class GetOptimalFormatTests {

    @Test
    @DisplayName("getOptimalFormat should return RAW_BINARY for MEMORY_CACHE")
    void getOptimalFormatShouldReturnRawBinaryForMemoryCache() {
      assertEquals(
          ModuleSerializationFormat.RAW_BINARY,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.MEMORY_CACHE));
    }

    @Test
    @DisplayName("getOptimalFormat should return COMPACT_BINARY_LZ4 for DISK_CACHE")
    void getOptimalFormatShouldReturnCompactBinaryLz4ForDiskCache() {
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.DISK_CACHE));
    }

    @Test
    @DisplayName("getOptimalFormat should return COMPACT_BINARY_GZIP for NETWORK_TRANSMISSION")
    void getOptimalFormatShouldReturnCompactBinaryGzipForNetworkTransmission() {
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.NETWORK_TRANSMISSION));
    }

    @Test
    @DisplayName("getOptimalFormat should return COMPACT_BINARY_GZIP for LONG_TERM_STORAGE")
    void getOptimalFormatShouldReturnCompactBinaryGzipForLongTermStorage() {
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.LONG_TERM_STORAGE));
    }

    @Test
    @DisplayName("getOptimalFormat should return STREAMING_BINARY for LARGE_MODULES")
    void getOptimalFormatShouldReturnStreamingBinaryForLargeModules() {
      assertEquals(
          ModuleSerializationFormat.STREAMING_BINARY,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.LARGE_MODULES));
    }

    @Test
    @DisplayName("getOptimalFormat should return MEMORY_MAPPED for MEMORY_CONSTRAINED")
    void getOptimalFormatShouldReturnMemoryMappedForMemoryConstrained() {
      assertEquals(
          ModuleSerializationFormat.MEMORY_MAPPED,
          ModuleSerializationFormat.getOptimalFormat(
              ModuleSerializationFormat.SerializationUseCase.MEMORY_CONSTRAINED));
    }

    @Test
    @DisplayName("getOptimalFormat should throw on null use case")
    void getOptimalFormatShouldThrowOnNullUseCase() {
      assertThrows(
          NullPointerException.class, () -> ModuleSerializationFormat.getOptimalFormat(null));
    }
  }

  @Nested
  @DisplayName("SerializationUseCase Enum Tests")
  class SerializationUseCaseEnumTests {

    @Test
    @DisplayName("SerializationUseCase should be an enum")
    void serializationUseCaseShouldBeEnum() {
      assertTrue(ModuleSerializationFormat.SerializationUseCase.class.isEnum());
    }

    @Test
    @DisplayName("SerializationUseCase should have 6 values")
    void serializationUseCaseShouldHave6Values() {
      assertEquals(6, ModuleSerializationFormat.SerializationUseCase.values().length);
    }

    @Test
    @DisplayName("SerializationUseCase should have all expected values")
    void serializationUseCaseShouldHaveAllExpectedValues() {
      assertNotNull(ModuleSerializationFormat.SerializationUseCase.MEMORY_CACHE);
      assertNotNull(ModuleSerializationFormat.SerializationUseCase.DISK_CACHE);
      assertNotNull(ModuleSerializationFormat.SerializationUseCase.NETWORK_TRANSMISSION);
      assertNotNull(ModuleSerializationFormat.SerializationUseCase.LONG_TERM_STORAGE);
      assertNotNull(ModuleSerializationFormat.SerializationUseCase.LARGE_MODULES);
      assertNotNull(ModuleSerializationFormat.SerializationUseCase.MEMORY_CONSTRAINED);
    }

    @Test
    @DisplayName("SerializationUseCase ordinals should be consistent")
    void serializationUseCaseOrdinalsShouldBeConsistent() {
      assertEquals(0, ModuleSerializationFormat.SerializationUseCase.MEMORY_CACHE.ordinal());
      assertEquals(1, ModuleSerializationFormat.SerializationUseCase.DISK_CACHE.ordinal());
      assertEquals(
          2, ModuleSerializationFormat.SerializationUseCase.NETWORK_TRANSMISSION.ordinal());
      assertEquals(3, ModuleSerializationFormat.SerializationUseCase.LONG_TERM_STORAGE.ordinal());
      assertEquals(4, ModuleSerializationFormat.SerializationUseCase.LARGE_MODULES.ordinal());
      assertEquals(5, ModuleSerializationFormat.SerializationUseCase.MEMORY_CONSTRAINED.ordinal());
    }

    @Test
    @DisplayName("SerializationUseCase valueOf should work")
    void serializationUseCaseValueOfShouldWork() {
      assertEquals(
          ModuleSerializationFormat.SerializationUseCase.MEMORY_CACHE,
          ModuleSerializationFormat.SerializationUseCase.valueOf("MEMORY_CACHE"));
      assertEquals(
          ModuleSerializationFormat.SerializationUseCase.DISK_CACHE,
          ModuleSerializationFormat.SerializationUseCase.valueOf("DISK_CACHE"));
      assertEquals(
          ModuleSerializationFormat.SerializationUseCase.NETWORK_TRANSMISSION,
          ModuleSerializationFormat.SerializationUseCase.valueOf("NETWORK_TRANSMISSION"));
    }
  }

  @Nested
  @DisplayName("Enum valueOf Tests")
  class EnumValueOfTests {

    @Test
    @DisplayName("valueOf should work for all formats")
    void valueOfShouldWorkForAllFormats() {
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_LZ4,
          ModuleSerializationFormat.valueOf("COMPACT_BINARY_LZ4"));
      assertEquals(
          ModuleSerializationFormat.COMPACT_BINARY_GZIP,
          ModuleSerializationFormat.valueOf("COMPACT_BINARY_GZIP"));
      assertEquals(
          ModuleSerializationFormat.RAW_BINARY, ModuleSerializationFormat.valueOf("RAW_BINARY"));
      assertEquals(
          ModuleSerializationFormat.STREAMING_BINARY,
          ModuleSerializationFormat.valueOf("STREAMING_BINARY"));
      assertEquals(
          ModuleSerializationFormat.MEMORY_MAPPED,
          ModuleSerializationFormat.valueOf("MEMORY_MAPPED"));
    }

    @Test
    @DisplayName("ordinals should be consistent")
    void ordinalsShouldBeConsistent() {
      assertEquals(0, ModuleSerializationFormat.COMPACT_BINARY_LZ4.ordinal());
      assertEquals(1, ModuleSerializationFormat.COMPACT_BINARY_GZIP.ordinal());
      assertEquals(2, ModuleSerializationFormat.RAW_BINARY.ordinal());
      assertEquals(3, ModuleSerializationFormat.STREAMING_BINARY.ordinal());
      assertEquals(4, ModuleSerializationFormat.MEMORY_MAPPED.ordinal());
    }
  }

  @Nested
  @DisplayName("Format Capabilities Tests")
  class FormatCapabilitiesTests {

    @Test
    @DisplayName("only COMPACT_BINARY_LZ4 and COMPACT_BINARY_GZIP support compression")
    void onlyCompactFormatsSupportCompression() {
      assertTrue(ModuleSerializationFormat.COMPACT_BINARY_LZ4.supportsCompression());
      assertTrue(ModuleSerializationFormat.COMPACT_BINARY_GZIP.supportsCompression());
      assertFalse(ModuleSerializationFormat.RAW_BINARY.supportsCompression());
      assertFalse(ModuleSerializationFormat.STREAMING_BINARY.supportsCompression());
      assertFalse(ModuleSerializationFormat.MEMORY_MAPPED.supportsCompression());
    }

    @Test
    @DisplayName("only COMPACT_BINARY_GZIP supports high compression")
    void onlyCompactBinaryGzipSupportsHighCompression() {
      assertFalse(ModuleSerializationFormat.COMPACT_BINARY_LZ4.supportsHighCompression());
      assertTrue(ModuleSerializationFormat.COMPACT_BINARY_GZIP.supportsHighCompression());
      assertFalse(ModuleSerializationFormat.RAW_BINARY.supportsHighCompression());
      assertFalse(ModuleSerializationFormat.STREAMING_BINARY.supportsHighCompression());
      assertFalse(ModuleSerializationFormat.MEMORY_MAPPED.supportsHighCompression());
    }

    @Test
    @DisplayName("streaming formats support streaming")
    void streamingFormatsSupportStreaming() {
      assertTrue(ModuleSerializationFormat.COMPACT_BINARY_LZ4.supportsStreaming());
      assertTrue(ModuleSerializationFormat.COMPACT_BINARY_GZIP.supportsStreaming());
      assertFalse(ModuleSerializationFormat.RAW_BINARY.supportsStreaming());
      assertTrue(ModuleSerializationFormat.STREAMING_BINARY.supportsStreaming());
      assertFalse(ModuleSerializationFormat.MEMORY_MAPPED.supportsStreaming());
    }
  }
}
