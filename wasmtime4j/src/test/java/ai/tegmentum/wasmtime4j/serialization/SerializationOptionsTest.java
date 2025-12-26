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

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SerializationOptions} class.
 *
 * <p>SerializationOptions provides fine-grained control over serialization behavior including
 * performance optimizations, security settings, and metadata preservation options.
 */
@DisplayName("SerializationOptions Tests")
class SerializationOptionsTest {

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("should create default options with createDefault")
    void shouldCreateDefaultOptionsWithCreateDefault() {
      final SerializationOptions options = SerializationOptions.createDefault();

      assertNotNull(options);
      assertTrue(options.isIncludeChecksum());
      assertFalse(options.isPreserveDebugInfo());
      assertFalse(options.isPreserveCustomSections());
      assertFalse(options.isPreserveNameSection());
      assertEquals(64 * 1024, options.getBufferSize()); // 64KB
      assertFalse(options.isUseParallelCompression());
      assertEquals(6, options.getCompressionLevel());
      assertFalse(options.isEnableStreaming());
      assertEquals(5 * 1024 * 1024, options.getStreamingThreshold()); // 5MB
      assertFalse(options.isEncryptSerialization());
      assertEquals("AES/GCM/NoPadding", options.getEncryptionAlgorithm());
      assertNull(options.getEncryptionKey());
      assertTrue(options.isVerifyIntegrity());
      assertFalse(options.isIncludePerformanceMetrics());
      assertTrue(options.isCleanupTempFiles());
      assertEquals("wasmtime4j-", options.getTempFilePrefix());
      assertFalse(options.isUseMemoryMappedFiles());
    }

    @Test
    @DisplayName("should have empty custom metadata by default")
    void shouldHaveEmptyCustomMetadataByDefault() {
      final SerializationOptions options = SerializationOptions.createDefault();

      assertNotNull(options.getCustomMetadata());
      assertTrue(options.getCustomMetadata().isEmpty());
    }
  }

  @Nested
  @DisplayName("Production Configuration Tests")
  class ProductionConfigurationTests {

    @Test
    @DisplayName("should create production options")
    void shouldCreateProductionOptions() {
      final SerializationOptions options = SerializationOptions.createProduction();

      assertNotNull(options);
      assertTrue(options.isIncludeChecksum());
      assertFalse(options.isPreserveCustomSections());
      assertFalse(options.isPreserveDebugInfo());
      assertEquals(128 * 1024, options.getBufferSize()); // 128KB
      assertTrue(options.isUseParallelCompression());
      assertEquals(6, options.getCompressionLevel());
      assertTrue(options.isEnableStreaming());
      assertEquals(10 * 1024 * 1024, options.getStreamingThreshold()); // 10MB
      assertTrue(options.isVerifyIntegrity());
      assertTrue(options.isIncludePerformanceMetrics());
      assertTrue(options.isIncludePlatformInfo());
      assertTrue(options.isCleanupTempFiles());
    }
  }

  @Nested
  @DisplayName("Development Configuration Tests")
  class DevelopmentConfigurationTests {

    @Test
    @DisplayName("should create development options")
    void shouldCreateDevelopmentOptions() {
      final SerializationOptions options = SerializationOptions.createDevelopment();

      assertNotNull(options);
      assertTrue(options.isIncludeChecksum());
      assertTrue(options.isPreserveCustomSections());
      assertTrue(options.isPreserveDebugInfo());
      assertTrue(options.isPreserveNameSection());
      assertEquals(64 * 1024, options.getBufferSize()); // 64KB
      assertFalse(options.isUseParallelCompression());
      assertEquals(1, options.getCompressionLevel());
      assertFalse(options.isEnableStreaming());
      assertTrue(options.isVerifyIntegrity());
      assertTrue(options.isIncludePerformanceMetrics());
      assertTrue(options.isIncludeCompilerInfo());
      assertTrue(options.isIncludePlatformInfo());
      assertFalse(options.isCleanupTempFiles());
    }
  }

  @Nested
  @DisplayName("High Performance Configuration Tests")
  class HighPerformanceConfigurationTests {

    @Test
    @DisplayName("should create high performance options")
    void shouldCreateHighPerformanceOptions() {
      final SerializationOptions options = SerializationOptions.createHighPerformance();

      assertNotNull(options);
      assertFalse(options.isIncludeChecksum());
      assertFalse(options.isPreserveCustomSections());
      assertFalse(options.isPreserveDebugInfo());
      assertEquals(1024 * 1024, options.getBufferSize()); // 1MB
      assertTrue(options.isUseParallelCompression());
      assertEquals(1, options.getCompressionLevel());
      assertTrue(options.isEnableStreaming());
      assertEquals(1024 * 1024, options.getStreamingThreshold()); // 1MB
      assertFalse(options.isVerifyIntegrity());
      assertFalse(options.isIncludePerformanceMetrics());
      assertTrue(options.isUseMemoryMappedFiles());
      assertTrue(options.isCleanupTempFiles());
    }
  }

  @Nested
  @DisplayName("Secure Configuration Tests")
  class SecureConfigurationTests {

    @Test
    @DisplayName("should create secure options")
    void shouldCreateSecureOptions() {
      final byte[] encryptionKey = new byte[32];
      final SerializationOptions options = SerializationOptions.createSecure(encryptionKey);

      assertNotNull(options);
      assertTrue(options.isIncludeChecksum());
      assertEquals(64 * 1024, options.getBufferSize());
      assertEquals(9, options.getCompressionLevel());
      assertTrue(options.isEncryptSerialization());
      assertArrayEquals(encryptionKey, options.getEncryptionKey());
      assertTrue(options.isVerifyIntegrity());
      assertTrue(options.isIncludePerformanceMetrics());
      assertTrue(options.isCleanupTempFiles());
    }

    @Test
    @DisplayName("createSecure should throw on null encryption key")
    void createSecureShouldThrowOnNullEncryptionKey() {
      assertThrows(NullPointerException.class, () -> SerializationOptions.createSecure(null));
    }
  }

  @Nested
  @DisplayName("Builder Core Options Tests")
  class BuilderCoreOptionsTests {

    @Test
    @DisplayName("should set include checksum")
    void shouldSetIncludeChecksum() {
      final SerializationOptions options =
          new SerializationOptions.Builder().includeChecksum(false).build();

      assertFalse(options.isIncludeChecksum());
    }

    @Test
    @DisplayName("should set preserve debug info")
    void shouldSetPreserveDebugInfo() {
      final SerializationOptions options =
          new SerializationOptions.Builder().preserveDebugInfo(true).build();

      assertTrue(options.isPreserveDebugInfo());
    }

    @Test
    @DisplayName("should set preserve custom sections")
    void shouldSetPreserveCustomSections() {
      final SerializationOptions options =
          new SerializationOptions.Builder().preserveCustomSections(true).build();

      assertTrue(options.isPreserveCustomSections());
    }

    @Test
    @DisplayName("should set preserve name section")
    void shouldSetPreserveNameSection() {
      final SerializationOptions options =
          new SerializationOptions.Builder().preserveNameSection(true).build();

      assertTrue(options.isPreserveNameSection());
    }
  }

  @Nested
  @DisplayName("Builder Performance Options Tests")
  class BuilderPerformanceOptionsTests {

    @Test
    @DisplayName("should set buffer size")
    void shouldSetBufferSize() {
      final SerializationOptions options =
          new SerializationOptions.Builder().setBufferSize(256 * 1024).build();

      assertEquals(256 * 1024, options.getBufferSize());
    }

    @Test
    @DisplayName("should set use parallel compression")
    void shouldSetUseParallelCompression() {
      final SerializationOptions options =
          new SerializationOptions.Builder().useParallelCompression(true).build();

      assertTrue(options.isUseParallelCompression());
    }

    @Test
    @DisplayName("should set compression level")
    void shouldSetCompressionLevel() {
      final SerializationOptions options =
          new SerializationOptions.Builder().setCompressionLevel(9).build();

      assertEquals(9, options.getCompressionLevel());
    }

    @Test
    @DisplayName("should set enable streaming")
    void shouldSetEnableStreaming() {
      final SerializationOptions options =
          new SerializationOptions.Builder().enableStreaming(true).build();

      assertTrue(options.isEnableStreaming());
    }

    @Test
    @DisplayName("should set streaming threshold")
    void shouldSetStreamingThreshold() {
      final SerializationOptions options =
          new SerializationOptions.Builder().setStreamingThreshold(20 * 1024 * 1024).build();

      assertEquals(20 * 1024 * 1024, options.getStreamingThreshold());
    }
  }

  @Nested
  @DisplayName("Builder Security Options Tests")
  class BuilderSecurityOptionsTests {

    @Test
    @DisplayName("should set encrypt serialization")
    void shouldSetEncryptSerialization() {
      final byte[] key = new byte[32];
      final SerializationOptions options =
          new SerializationOptions.Builder()
              .encryptSerialization(true)
              .setEncryptionKey(key)
              .build();

      assertTrue(options.isEncryptSerialization());
    }

    @Test
    @DisplayName("should set encryption algorithm")
    void shouldSetEncryptionAlgorithm() {
      final SerializationOptions options =
          new SerializationOptions.Builder().setEncryptionAlgorithm("AES/CBC/PKCS5Padding").build();

      assertEquals("AES/CBC/PKCS5Padding", options.getEncryptionAlgorithm());
    }

    @Test
    @DisplayName("should set encryption key")
    void shouldSetEncryptionKey() {
      final byte[] key = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
      final SerializationOptions options =
          new SerializationOptions.Builder()
              .encryptSerialization(true)
              .setEncryptionKey(key)
              .build();

      assertArrayEquals(key, options.getEncryptionKey());
    }

    @Test
    @DisplayName("should set verify integrity")
    void shouldSetVerifyIntegrity() {
      final SerializationOptions options =
          new SerializationOptions.Builder().verifyIntegrity(false).build();

      assertFalse(options.isVerifyIntegrity());
    }
  }

  @Nested
  @DisplayName("Builder Metadata Options Tests")
  class BuilderMetadataOptionsTests {

    @Test
    @DisplayName("should set include performance metrics")
    void shouldSetIncludePerformanceMetrics() {
      final SerializationOptions options =
          new SerializationOptions.Builder().includePerformanceMetrics(true).build();

      assertTrue(options.isIncludePerformanceMetrics());
    }

    @Test
    @DisplayName("should set include compiler info")
    void shouldSetIncludeCompilerInfo() {
      final SerializationOptions options =
          new SerializationOptions.Builder().includeCompilerInfo(true).build();

      assertTrue(options.isIncludeCompilerInfo());
    }

    @Test
    @DisplayName("should set include platform info")
    void shouldSetIncludePlatformInfo() {
      final SerializationOptions options =
          new SerializationOptions.Builder().includePlatformInfo(true).build();

      assertTrue(options.isIncludePlatformInfo());
    }

    @Test
    @DisplayName("should add custom metadata")
    void shouldAddCustomMetadata() {
      final SerializationOptions options =
          new SerializationOptions.Builder()
              .addCustomMetadata("key1", "value1")
              .addCustomMetadata("key2", "value2")
              .build();

      assertEquals(2, options.getCustomMetadata().size());
      assertEquals("value1", options.getCustomMetadata().get("key1"));
      assertEquals("value2", options.getCustomMetadata().get("key2"));
    }

    @Test
    @DisplayName("should set custom metadata map")
    void shouldSetCustomMetadataMap() {
      final SerializationOptions options =
          new SerializationOptions.Builder()
              .setCustomMetadata(Map.of("a", "1", "b", "2", "c", "3"))
              .build();

      assertEquals(3, options.getCustomMetadata().size());
    }
  }

  @Nested
  @DisplayName("Builder File System Options Tests")
  class BuilderFileSystemOptionsTests {

    @Test
    @DisplayName("should set cleanup temp files")
    void shouldSetCleanupTempFiles() {
      final SerializationOptions options =
          new SerializationOptions.Builder().cleanupTempFiles(false).build();

      assertFalse(options.isCleanupTempFiles());
    }

    @Test
    @DisplayName("should set temp file prefix")
    void shouldSetTempFilePrefix() {
      final SerializationOptions options =
          new SerializationOptions.Builder().setTempFilePrefix("myapp-").build();

      assertEquals("myapp-", options.getTempFilePrefix());
    }

    @Test
    @DisplayName("should set use memory mapped files")
    void shouldSetUseMemoryMappedFiles() {
      final SerializationOptions options =
          new SerializationOptions.Builder().useMemoryMappedFiles(true).build();

      assertTrue(options.isUseMemoryMappedFiles());
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("setBufferSize should throw on non-positive value")
    void setBufferSizeShouldThrowOnNonPositiveValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().setBufferSize(0));
    }

    @Test
    @DisplayName("setCompressionLevel should throw on level below 0")
    void setCompressionLevelShouldThrowOnLevelBelowZero() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().setCompressionLevel(-1));
    }

    @Test
    @DisplayName("setCompressionLevel should throw on level above 9")
    void setCompressionLevelShouldThrowOnLevelAboveNine() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().setCompressionLevel(10));
    }

    @Test
    @DisplayName("setStreamingThreshold should throw on negative value")
    void setStreamingThresholdShouldThrowOnNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().setStreamingThreshold(-1));
    }

    @Test
    @DisplayName("setEncryptionAlgorithm should throw on null")
    void setEncryptionAlgorithmShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new SerializationOptions.Builder().setEncryptionAlgorithm(null));
    }

    @Test
    @DisplayName("addCustomMetadata should throw on null key")
    void addCustomMetadataShouldThrowOnNullKey() {
      assertThrows(
          NullPointerException.class,
          () -> new SerializationOptions.Builder().addCustomMetadata(null, "value"));
    }

    @Test
    @DisplayName("setTempFilePrefix should throw on null")
    void setTempFilePrefixShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new SerializationOptions.Builder().setTempFilePrefix(null));
    }

    @Test
    @DisplayName("build should throw when encryption enabled without key")
    void buildShouldThrowWhenEncryptionEnabledWithoutKey() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationOptions.Builder().encryptSerialization(true).build());
    }
  }

  @Nested
  @DisplayName("Encryption Key Defensive Copy Tests")
  class EncryptionKeyDefensiveCopyTests {

    @Test
    @DisplayName("getEncryptionKey should return defensive copy")
    void getEncryptionKeyShouldReturnDefensiveCopy() {
      final byte[] originalKey = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
      final SerializationOptions options =
          new SerializationOptions.Builder()
              .encryptSerialization(true)
              .setEncryptionKey(originalKey)
              .build();

      final byte[] retrievedKey = options.getEncryptionKey();
      retrievedKey[0] = 99;

      // Original key in options should be unchanged
      assertEquals(1, options.getEncryptionKey()[0]);
    }

    @Test
    @DisplayName("getEncryptionKey should return null when not set")
    void getEncryptionKeyShouldReturnNullWhenNotSet() {
      final SerializationOptions options = SerializationOptions.createDefault();

      assertNull(options.getEncryptionKey());
    }
  }

  @Nested
  @DisplayName("Custom Metadata Immutability Tests")
  class CustomMetadataImmutabilityTests {

    @Test
    @DisplayName("getCustomMetadata should return unmodifiable map")
    void getCustomMetadataShouldReturnUnmodifiableMap() {
      final SerializationOptions options =
          new SerializationOptions.Builder().addCustomMetadata("key", "value").build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> options.getCustomMetadata().put("new", "value"));
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final SerializationOptions options = SerializationOptions.createDefault();
      final String result = options.toString();

      assertNotNull(result);
      assertTrue(result.contains("SerializationOptions"));
      assertTrue(result.contains("checksum=true"));
      assertTrue(result.contains("compression=6"));
      assertTrue(result.contains("streaming=false"));
      assertTrue(result.contains("buffer=64KB"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("SerializationOptions should be final")
    void serializationOptionsShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(SerializationOptions.class.getModifiers()));
    }

    @Test
    @DisplayName("Builder should be final")
    void builderShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(SerializationOptions.Builder.class.getModifiers()));
    }
  }
}
