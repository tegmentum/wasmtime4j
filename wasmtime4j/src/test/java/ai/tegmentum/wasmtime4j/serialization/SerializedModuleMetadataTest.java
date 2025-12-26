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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SerializedModuleMetadata} class.
 *
 * <p>SerializedModuleMetadata contains comprehensive metadata about serialized modules including
 * format information, integrity data, performance metrics, and compatibility information.
 */
@DisplayName("SerializedModuleMetadata Tests")
class SerializedModuleMetadataTest {

  @Nested
  @DisplayName("Builder Default Values Tests")
  class BuilderDefaultValuesTests {

    @Test
    @DisplayName("should create metadata with default values")
    void shouldCreateMetadataWithDefaultValues() {
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      assertNotNull(metadata);
      assertEquals("1.0", metadata.getFormatVersion());
      assertEquals(ModuleSerializationFormat.COMPACT_BINARY_LZ4, metadata.getFormat());
      assertNotNull(metadata.getSerializationTimestamp());
      assertEquals(0L, metadata.getSerializedSize());
      assertEquals(0L, metadata.getOriginalSize());
      assertEquals("", metadata.getSha256Hash());
      assertFalse(metadata.isEncrypted());
    }

    @Test
    @DisplayName("should have default platform info from system properties")
    void shouldHaveDefaultPlatformInfoFromSystemProperties() {
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      assertEquals(System.getProperty("java.version"), metadata.getJavaVersion());
      assertEquals(System.getProperty("os.arch"), metadata.getPlatformArch());
      assertEquals(System.getProperty("os.name"), metadata.getPlatformOs());
      assertEquals("36.0.2", metadata.getWasmtimeVersion());
    }

    @Test
    @DisplayName("should have empty custom metadata by default")
    void shouldHaveEmptyCustomMetadataByDefault() {
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      assertNotNull(metadata.getCustomMetadata());
      assertTrue(metadata.getCustomMetadata().isEmpty());
    }

    @Test
    @DisplayName("should have false debug info flags by default")
    void shouldHaveFalseDebugInfoFlagsByDefault() {
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      assertFalse(metadata.hasSourceMaps());
      assertFalse(metadata.hasDebugSymbols());
      assertFalse(metadata.hasNameSection());
    }
  }

  @Nested
  @DisplayName("Builder Setter Tests")
  class BuilderSetterTests {

    @Test
    @DisplayName("should set format version")
    void shouldSetFormatVersion() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setFormatVersion("2.0").build();

      assertEquals("2.0", metadata.getFormatVersion());
    }

    @Test
    @DisplayName("should set format")
    void shouldSetFormat() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_GZIP)
              .build();

      assertEquals(ModuleSerializationFormat.COMPACT_BINARY_GZIP, metadata.getFormat());
    }

    @Test
    @DisplayName("should set serialization timestamp")
    void shouldSetSerializationTimestamp() {
      final Instant timestamp = Instant.parse("2024-01-15T10:30:00Z");
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setSerializationTimestamp(timestamp).build();

      assertEquals(timestamp, metadata.getSerializationTimestamp());
    }

    @Test
    @DisplayName("should set sizes")
    void shouldSetSizes() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(5000L)
              .setOriginalSize(10000L)
              .build();

      assertEquals(5000L, metadata.getSerializedSize());
      assertEquals(10000L, metadata.getOriginalSize());
    }

    @Test
    @DisplayName("should set hash values")
    void shouldSetHashValues() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSha256Hash("abc123def456")
              .setSha1Hash("sha1hash")
              .build();

      assertEquals("abc123def456", metadata.getSha256Hash());
      assertEquals("sha1hash", metadata.getSha1Hash());
    }

    @Test
    @DisplayName("should set encryption info")
    void shouldSetEncryptionInfo() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setEncrypted(true, "AES-256-GCM").build();

      assertTrue(metadata.isEncrypted());
      assertEquals("AES-256-GCM", metadata.getEncryptionAlgorithm());
    }

    @Test
    @DisplayName("should set module info")
    void shouldSetModuleInfo() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setModuleName("test-module")
              .setModuleVersion("1.2.3")
              .build();

      assertEquals("test-module", metadata.getModuleName());
      assertEquals("1.2.3", metadata.getModuleVersion());
    }

    @Test
    @DisplayName("should set entity counts")
    void shouldSetEntityCounts() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setCounts(10, 5, 20, 3, 1, 2).build();

      assertEquals(10, metadata.getImportCount());
      assertEquals(5, metadata.getExportCount());
      assertEquals(20, metadata.getFunctionCount());
      assertEquals(3, metadata.getGlobalCount());
      assertEquals(1, metadata.getMemoryCount());
      assertEquals(2, metadata.getTableCount());
    }

    @Test
    @DisplayName("should set serialization duration and compression ratio")
    void shouldSetSerializationDurationAndCompressionRatio() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializationDuration(150L)
              .setCompressionRatio(2.5)
              .build();

      assertEquals(150L, metadata.getSerializationDurationMs());
      assertEquals(2.5, metadata.getCompressionRatio(), 0.001);
    }

    @Test
    @DisplayName("should set performance metrics")
    void shouldSetPerformanceMetrics() {
      final SerializationPerformanceMetrics performanceMetrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L)
              .build();

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setPerformanceMetrics(performanceMetrics).build();

      assertNotNull(metadata.getPerformanceMetrics());
      assertEquals(100_000_000L, metadata.getPerformanceMetrics().getSerializationTimeNs());
    }

    @Test
    @DisplayName("should set platform info")
    void shouldSetPlatformInfo() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setWasmtimeVersion("35.0.0")
              .setJavaVersion("21.0.1")
              .setPlatformInfo("aarch64", "Linux")
              .build();

      assertEquals("35.0.0", metadata.getWasmtimeVersion());
      assertEquals("21.0.1", metadata.getJavaVersion());
      assertEquals("aarch64", metadata.getPlatformArch());
      assertEquals("Linux", metadata.getPlatformOs());
    }

    @Test
    @DisplayName("should add custom metadata")
    void shouldAddCustomMetadata() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .addCustomMetadata("key1", "value1")
              .addCustomMetadata("key2", "value2")
              .build();

      assertEquals(2, metadata.getCustomMetadata().size());
      assertEquals("value1", metadata.getCustomMetadata().get("key1"));
      assertEquals("value2", metadata.getCustomMetadata().get("key2"));
    }

    @Test
    @DisplayName("should set custom metadata map")
    void shouldSetCustomMetadataMap() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setCustomMetadata(Map.of("a", "1", "b", "2", "c", "3"))
              .build();

      assertEquals(3, metadata.getCustomMetadata().size());
    }

    @Test
    @DisplayName("should set debug info flags")
    void shouldSetDebugInfoFlags() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setDebugInfo(true, true, true).build();

      assertTrue(metadata.hasSourceMaps());
      assertTrue(metadata.hasDebugSymbols());
      assertTrue(metadata.hasNameSection());
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("setFormatVersion should throw on null")
    void setFormatVersionShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new SerializedModuleMetadata.Builder().setFormatVersion(null));
    }

    @Test
    @DisplayName("setFormat should throw on null")
    void setFormatShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class, () -> new SerializedModuleMetadata.Builder().setFormat(null));
    }

    @Test
    @DisplayName("setSerializationTimestamp should throw on null")
    void setSerializationTimestampShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new SerializedModuleMetadata.Builder().setSerializationTimestamp(null));
    }

    @Test
    @DisplayName("setSerializedSize should throw on negative value")
    void setSerializedSizeShouldThrowOnNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setSerializedSize(-1L));
    }

    @Test
    @DisplayName("setOriginalSize should throw on negative value")
    void setOriginalSizeShouldThrowOnNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setOriginalSize(-1L));
    }

    @Test
    @DisplayName("setSha256Hash should throw on null")
    void setSha256HashShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new SerializedModuleMetadata.Builder().setSha256Hash(null));
    }

    @Test
    @DisplayName("setCounts should throw on negative values")
    void setCountsShouldThrowOnNegativeValues() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCounts(-1, 0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("setSerializationDuration should throw on negative value")
    void setSerializationDurationShouldThrowOnNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setSerializationDuration(-1L));
    }

    @Test
    @DisplayName("setCompressionRatio should throw on non-positive value")
    void setCompressionRatioShouldThrowOnNonPositiveValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCompressionRatio(0.0));
    }

    @Test
    @DisplayName("addCustomMetadata should throw on null key")
    void addCustomMetadataShouldThrowOnNullKey() {
      assertThrows(
          NullPointerException.class,
          () -> new SerializedModuleMetadata.Builder().addCustomMetadata(null, "value"));
    }
  }

  @Nested
  @DisplayName("Integrity Validation Tests")
  class IntegrityValidationTests {

    @Test
    @DisplayName("validateIntegrity should throw on null data")
    void validateIntegrityShouldThrowOnNullData() {
      final SerializedModuleMetadata metadata = new SerializedModuleMetadata.Builder().build();

      assertThrows(NullPointerException.class, () -> metadata.validateIntegrity(null));
    }

    @Test
    @DisplayName("validateIntegrity should return false when size mismatch")
    void validateIntegrityShouldReturnFalseWhenSizeMismatch() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().setSerializedSize(100L).build();

      final byte[] data = new byte[50]; // Wrong size
      assertFalse(metadata.validateIntegrity(data));
    }

    @Test
    @DisplayName("validateIntegrity should return true when hash matches")
    void validateIntegrityShouldReturnTrueWhenHashMatches() throws Exception {
      final byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(data);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(data.length)
              .setSha256Hash(hash)
              .build();

      assertTrue(metadata.validateIntegrity(data));
    }

    @Test
    @DisplayName("validateIntegrity should return false when hash mismatch")
    void validateIntegrityShouldReturnFalseWhenHashMismatch() {
      final byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(data.length)
              .setSha256Hash("wronghash")
              .build();

      assertFalse(metadata.validateIntegrity(data));
    }

    private String bytesToHex(final byte[] bytes) {
      final StringBuilder sb = new StringBuilder();
      for (final byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    }
  }

  @Nested
  @DisplayName("Estimated Deserialization Time Tests")
  class EstimatedDeserializationTimeTests {

    @Test
    @DisplayName("should estimate using performance metrics when available")
    void shouldEstimateUsingPerformanceMetricsWhenAvailable() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L)
              .build();

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializationDuration(100L)
              .setPerformanceMetrics(metrics)
              .build();

      // Speed ratio is 0.5, so 100ms * 0.5 = 50ms
      assertEquals(50L, metadata.getEstimatedDeserializationTimeMs());
    }

    @Test
    @DisplayName("should estimate using size and format when no metrics")
    void shouldEstimateUsingSizeAndFormatWhenNoMetrics() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1024L * 10L) // 10 KB
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .build();

      // Fallback: 1ms per KB baseline * compression factor 1.5 = 10 * 1.5 = 15
      assertTrue(metadata.getEstimatedDeserializationTimeMs() > 0);
    }
  }

  @Nested
  @DisplayName("Compression Ratio Calculation Tests")
  class CompressionRatioCalculationTests {

    @Test
    @DisplayName("build should calculate compression ratio from sizes")
    void buildShouldCalculateCompressionRatioFromSizes() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setOriginalSize(10000L)
              .setSerializedSize(5000L)
              .build();

      // 10000 / 5000 = 2.0
      assertEquals(2.0, metadata.getCompressionRatio(), 0.001);
    }
  }

  @Nested
  @DisplayName("Custom Metadata Immutability Tests")
  class CustomMetadataImmutabilityTests {

    @Test
    @DisplayName("getCustomMetadata should return unmodifiable map")
    void getCustomMetadataShouldReturnUnmodifiableMap() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder().addCustomMetadata("key", "value").build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> metadata.getCustomMetadata().put("new", "value"));
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(5000L)
              .setOriginalSize(10000L)
              .setCounts(5, 3, 10, 2, 1, 1)
              .build();

      final String result = metadata.toString();

      assertNotNull(result);
      assertTrue(result.contains("SerializedModuleMetadata"));
      assertTrue(result.contains("compact-binary-lz4"));
      assertTrue(result.contains("5000 bytes"));
      assertTrue(result.contains("imports=5"));
      assertTrue(result.contains("exports=3"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("SerializedModuleMetadata should be final")
    void serializedModuleMetadataShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(SerializedModuleMetadata.class.getModifiers()));
    }

    @Test
    @DisplayName("Builder should be final")
    void builderShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              SerializedModuleMetadata.Builder.class.getModifiers()));
    }
  }
}
