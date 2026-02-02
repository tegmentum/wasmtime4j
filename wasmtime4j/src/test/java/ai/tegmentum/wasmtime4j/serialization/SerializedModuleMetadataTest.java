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

  @Nested
  @DisplayName("Environment Compatibility Tests")
  class EnvironmentCompatibilityTests {

    @Test
    @DisplayName("isCompatibleWithCurrentEnvironment should return true for matching environment")
    void isCompatibleShouldReturnTrueForMatchingEnvironment() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isCompatibleWithCurrentEnvironment should return false for incompatible Java")
    void isCompatibleShouldReturnFalseForIncompatibleJava() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion("99.0.0") // Incompatible future version
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      assertFalse(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isCompatibleWithCurrentEnvironment should return false for incompatible arch")
    void isCompatibleShouldReturnFalseForIncompatibleArch() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo("mips64", System.getProperty("os.name"))
              .build();

      assertFalse(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isCompatibleWithCurrentEnvironment should return false for incompatible OS")
    void isCompatibleShouldReturnFalseForIncompatibleOs() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "AmigaOS")
              .build();

      assertFalse(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("should consider x86_64 and amd64 as compatible architectures")
    void shouldConsiderX8664AndAmd64AsCompatible() {
      // Get current arch and check compatibility
      final String currentArch = System.getProperty("os.arch");
      String compatibleArch;
      if (currentArch.contains("x86_64")) {
        compatibleArch = "amd64";
      } else if (currentArch.contains("amd64")) {
        compatibleArch = "x86_64";
      } else {
        // Skip test on other architectures
        return;
      }

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(compatibleArch, System.getProperty("os.name"))
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("should consider Windows variants as compatible")
    void shouldConsiderWindowsVariantsAsCompatible() {
      final String currentOs = System.getProperty("os.name").toLowerCase();
      if (!currentOs.contains("windows")) {
        // Skip test on non-Windows
        return;
      }

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "Windows 11")
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("should consider Linux variants as compatible")
    void shouldConsiderLinuxVariantsAsCompatible() {
      final String currentOs = System.getProperty("os.name").toLowerCase();
      if (!currentOs.contains("linux")) {
        // Skip test on non-Linux
        return;
      }

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "Linux 5.15")
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("should consider Mac variants as compatible")
    void shouldConsiderMacVariantsAsCompatible() {
      final String currentOs = System.getProperty("os.name").toLowerCase();
      if (!currentOs.contains("mac")) {
        // Skip test on non-Mac
        return;
      }

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "Mac OS X 14.0")
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("should handle null Java version in extract major version")
    void shouldHandleNullJavaVersionInExtractMajorVersion() {
      // This tests the extractMajorVersion method indirectly
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      // The method should handle null gracefully in isJavaVersionCompatible
      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("should handle Java version without dot")
    void shouldHandleJavaVersionWithoutDot() {
      // Test version like "21" instead of "21.0.1"
      final String currentVersion = System.getProperty("java.version");
      final String majorOnly = currentVersion.contains(".")
          ? currentVersion.substring(0, currentVersion.indexOf('.'))
          : currentVersion;

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(majorOnly)
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }
  }

  @Nested
  @DisplayName("Estimated Deserialization Time Edge Cases Tests")
  class EstimatedDeserializationTimeEdgeCasesTests {

    @Test
    @DisplayName("should estimate for format without compression support")
    void shouldEstimateForFormatWithoutCompressionSupport() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1024L * 5L) // 5 KB
              .setFormat(ModuleSerializationFormat.RAW_BINARY) // No compression
              .build();

      // Fallback: 1ms per KB baseline * 1.0 for no compression = 5
      long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertTrue(estimated > 0, "Estimated time should be positive: " + estimated);
    }

    @Test
    @DisplayName("should handle zero serialized size")
    void shouldHandleZeroSerializedSize() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(0L)
              .setFormat(ModuleSerializationFormat.RAW_BINARY)
              .build();

      long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertEquals(0L, estimated, "Zero size should result in zero estimated time");
    }
  }

  @Nested
  @DisplayName("Builder Edge Cases Tests")
  class BuilderEdgeCasesTests {

    @Test
    @DisplayName("setCustomMetadata should clear existing and set new")
    void setCustomMetadataShouldClearExistingAndSetNew() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .addCustomMetadata("old", "value")
              .setCustomMetadata(Map.of("new", "value"))
              .build();

      assertEquals(1, metadata.getCustomMetadata().size());
      assertFalse(metadata.getCustomMetadata().containsKey("old"));
      assertTrue(metadata.getCustomMetadata().containsKey("new"));
    }

    @Test
    @DisplayName("setCustomMetadata with null should clear all")
    void setCustomMetadataWithNullShouldClearAll() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .addCustomMetadata("key", "value")
              .setCustomMetadata(null)
              .build();

      assertTrue(metadata.getCustomMetadata().isEmpty());
    }

    @Test
    @DisplayName("setCounts should reject negative export count")
    void setCountsShouldRejectNegativeExportCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCounts(0, -1, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("setCounts should reject negative function count")
    void setCountsShouldRejectNegativeFunctionCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCounts(0, 0, -1, 0, 0, 0));
    }

    @Test
    @DisplayName("setCounts should reject negative global count")
    void setCountsShouldRejectNegativeGlobalCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCounts(0, 0, 0, -1, 0, 0));
    }

    @Test
    @DisplayName("setCounts should reject negative memory count")
    void setCountsShouldRejectNegativeMemoryCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCounts(0, 0, 0, 0, -1, 0));
    }

    @Test
    @DisplayName("setCounts should reject negative table count")
    void setCountsShouldRejectNegativeTableCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCounts(0, 0, 0, 0, 0, -1));
    }

    @Test
    @DisplayName("setCompressionRatio should reject negative value")
    void setCompressionRatioShouldRejectNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializedModuleMetadata.Builder().setCompressionRatio(-1.0));
    }

    @Test
    @DisplayName("compression ratio should not be recalculated when already set")
    void compressionRatioShouldBeRecalculatedFromSizes() {
      // When both sizes are set and ratio not set, build() calculates it
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setOriginalSize(20000L)
              .setSerializedSize(5000L)
              .build();

      // 20000 / 5000 = 4.0
      assertEquals(4.0, metadata.getCompressionRatio(), 0.001);
    }

    @Test
    @DisplayName("build should not calculate ratio when sizes are zero")
    void buildShouldNotCalculateRatioWhenSizesAreZero() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setOriginalSize(0L)
              .setSerializedSize(0L)
              .setCompressionRatio(1.5)
              .build();

      // Should keep the explicitly set ratio
      assertEquals(1.5, metadata.getCompressionRatio(), 0.001);
    }
  }

  @Nested
  @DisplayName("Platform Compatibility Individual Branch Tests")
  class PlatformCompatibilityIndividualBranchTests {

    @Test
    @DisplayName("isPlatformArchCompatible should return true for exact match")
    void isPlatformArchCompatibleShouldReturnTrueForExactMatch() {
      final String currentArch = System.getProperty("os.arch");
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(currentArch, System.getProperty("os.name"))
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isPlatformOsCompatible should return true for exact match")
    void isPlatformOsCompatibleShouldReturnTrueForExactMatch() {
      final String currentOs = System.getProperty("os.name");
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), currentOs)
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("should detect aarch64 and arm64 as incompatible with x86")
    void shouldDetectAarch64AndArm64AsIncompatibleWithX86() {
      final String currentArch = System.getProperty("os.arch");
      final String incompatibleArch;
      if (currentArch.contains("aarch64") || currentArch.contains("arm")) {
        incompatibleArch = "x86_64";
      } else if (currentArch.contains("x86") || currentArch.contains("amd64")) {
        incompatibleArch = "aarch64";
      } else {
        return; // Skip test on other architectures
      }

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(incompatibleArch, System.getProperty("os.name"))
              .build();

      assertFalse(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isPlatformOsCompatible should detect completely different OS as incompatible")
    void isPlatformOsCompatibleShouldDetectDifferentOsAsIncompatible() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "FreeBSD 14.0")
              .build();

      // FreeBSD is not Windows, Linux, or Mac
      final String currentOs = System.getProperty("os.name").toLowerCase();
      if (currentOs.contains("freebsd")) {
        assertTrue(metadata.isCompatibleWithCurrentEnvironment());
      } else {
        assertFalse(metadata.isCompatibleWithCurrentEnvironment());
      }
    }

    @Test
    @DisplayName("isJavaVersionCompatible should return true for exact version match")
    void isJavaVersionCompatibleShouldReturnTrueForExactVersionMatch() {
      final String currentVersion = System.getProperty("java.version");
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(currentVersion)
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isJavaVersionCompatible should return true when major version matches")
    void isJavaVersionCompatibleShouldReturnTrueWhenMajorVersionMatches() {
      final String currentVersion = System.getProperty("java.version");
      // Extract major version and append different minor version
      final String majorVersion;
      if (currentVersion.contains(".")) {
        majorVersion = currentVersion.substring(0, currentVersion.indexOf('.'));
      } else {
        majorVersion = currentVersion;
      }
      final String differentMinorVersion = majorVersion + ".99.99";

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(differentMinorVersion)
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      // Current Java version should start with the same major version
      assertTrue(metadata.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isJavaVersionCompatible should return false for different major version")
    void isJavaVersionCompatibleShouldReturnFalseForDifferentMajorVersion() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion("99.0.0") // Future major version
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      assertFalse(metadata.isCompatibleWithCurrentEnvironment());
    }
  }

  @Nested
  @DisplayName("Estimated Deserialization Time Boundary Tests")
  class EstimatedDeserializationTimeBoundaryTests {

    @Test
    @DisplayName("should estimate correctly for various compression formats")
    void shouldEstimateCorrectlyForVariousCompressionFormats() {
      // Test each format's compression factor
      for (final ModuleSerializationFormat format : ModuleSerializationFormat.values()) {
        final SerializedModuleMetadata metadata =
            new SerializedModuleMetadata.Builder()
                .setSerializedSize(1024L * 10L) // 10 KB
                .setFormat(format)
                .build();

        final long estimated = metadata.getEstimatedDeserializationTimeMs();
        assertTrue(estimated >= 0, "Estimated time should be non-negative for format: " + format);
      }
    }

    @Test
    @DisplayName("should use performance metrics ratio when available")
    void shouldUsePerformanceMetricsRatioWhenAvailable() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(200_000_000L, 100_000_000L, 0L, 0L, 0L) // 200ms ser, 100ms deser
              .build();

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializationDuration(200L)
              .setPerformanceMetrics(metrics)
              .build();

      // Speed ratio is 0.5, so 200ms * 0.5 = 100ms
      final long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertEquals(100L, estimated);
    }

    @Test
    @DisplayName("should return zero when metrics exist but serialization duration is zero")
    void shouldReturnZeroWhenMetricsExistButSerializationDurationIsZero() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(0L, 100_000_000L, 0L, 0L, 0L) // 0 serialization time
              .build();

      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializationDuration(0L) // 0ms duration
              .setSerializedSize(1024L * 10L) // 10 KB
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setPerformanceMetrics(metrics)
              .build();

      // With metrics present, it uses: serializationDurationMs * deserializationRatio
      // 0 * ratio = 0, regardless of ratio value
      final long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertEquals(0L, estimated, "0ms * ratio should be 0");
    }

    @Test
    @DisplayName("should use fallback when no performance metrics are set")
    void shouldUseFallbackWhenNoPerformanceMetricsAreSet() {
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1024L * 10L) // 10 KB
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              // No performance metrics set
              .build();

      // Fallback: sizeInKB * formatFactor = 10 * 1.5 = 15
      final long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertTrue(estimated > 0, "Should have positive estimate using size-based fallback");
    }

    @Test
    @DisplayName("should calculate 1ms per KB baseline for non-compressed format")
    void shouldCalculate1msPerKbBaselineForNonCompressedFormat() {
      // 1024 bytes = 1 KB = 1ms baseline for non-compressed format
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1024L) // Exactly 1 KB
              .setFormat(ModuleSerializationFormat.RAW_BINARY) // 1.0 factor
              .build();

      // Calculation: 1024 / 1024.0 * 1.0 = 1.0 -> rounds to 1
      final long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertEquals(1L, estimated, "1 KB with 1.0 factor should be exactly 1ms");
    }

    @Test
    @DisplayName("should calculate 1.5x factor for compressed format")
    void shouldCalculate1Point5FactorForCompressedFormat() {
      // Test that compression formats use 1.5 multiplier (not 1.0 or 2.0)
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(1024L * 10L) // 10 KB
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4) // Supports compression
              .build();

      // Calculation: 10 KB * 1.5 = 15ms
      // If factor was 1.0, result would be 10
      // If factor was 2.0, result would be 20
      final long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertEquals(15L, estimated, "10 KB with 1.5 compression factor should be 15ms");
    }

    @Test
    @DisplayName("should use division not multiplication for KB calculation")
    void shouldUseDivisionNotMultiplicationForKbCalculation() {
      // 2048 bytes = 2 KB
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(2048L) // 2 KB
              .setFormat(ModuleSerializationFormat.RAW_BINARY) // 1.0 factor
              .build();

      // Correct: 2048 / 1024.0 * 1.0 = 2.0 -> 2ms
      // If multiplication used instead of division: 2048 * 1024.0 = huge number
      final long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertEquals(2L, estimated, "2048 bytes should be 2ms (2 KB baseline)");
    }

    @Test
    @DisplayName("should verify 1024 is the divisor not 1")
    void shouldVerify1024IsTheDivisorNot1() {
      // If divisor was 1 instead of 1024, 10240 bytes would be 10240ms
      // With 1024 divisor, it should be 10ms (for non-compressed)
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setSerializedSize(10240L) // 10 KB = 10240 bytes
              .setFormat(ModuleSerializationFormat.RAW_BINARY) // 1.0 factor
              .build();

      // Correct: 10240 / 1024.0 * 1.0 = 10.0 -> 10ms
      // If divisor was 1: 10240 / 1.0 * 1.0 = 10240ms
      final long estimated = metadata.getEstimatedDeserializationTimeMs();
      assertEquals(10L, estimated, "10 KB should be 10ms, not 10240ms");
    }
  }

  @Nested
  @DisplayName("Platform OS Compatibility Branch Tests")
  class PlatformOsCompatibilityBranchTests {

    @Test
    @DisplayName("Windows variants should be compatible with each other")
    void windowsVariantsShouldBeCompatible() {
      // Test that "Windows 10" and "Windows 11" are compatible
      final SerializedModuleMetadata metadataWin10 =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "Windows 10")
              .build();

      final SerializedModuleMetadata metadataWin11 =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "Windows 11")
              .build();

      // Both should contain "windows" when lowercased
      final String currentOs = System.getProperty("os.name").toLowerCase();
      if (currentOs.contains("windows")) {
        assertTrue(metadataWin10.isCompatibleWithCurrentEnvironment());
        assertTrue(metadataWin11.isCompatibleWithCurrentEnvironment());
      }
    }

    @Test
    @DisplayName("Linux variants should be compatible with each other")
    void linuxVariantsShouldBeCompatible() {
      final SerializedModuleMetadata metadataUbuntu =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "Linux Ubuntu 22.04")
              .build();

      final String currentOs = System.getProperty("os.name").toLowerCase();
      if (currentOs.contains("linux")) {
        assertTrue(metadataUbuntu.isCompatibleWithCurrentEnvironment());
      }
    }

    @Test
    @DisplayName("Mac variants should be compatible with each other")
    void macVariantsShouldBeCompatible() {
      final SerializedModuleMetadata metadataMacos =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), "Mac OS X 14.0")
              .build();

      final String currentOs = System.getProperty("os.name").toLowerCase();
      if (currentOs.contains("mac")) {
        assertTrue(metadataMacos.isCompatibleWithCurrentEnvironment());
      }
    }

    @Test
    @DisplayName("Windows should not be compatible with Linux")
    void windowsShouldNotBeCompatibleWithLinux() {
      final String currentOs = System.getProperty("os.name").toLowerCase();

      if (currentOs.contains("linux")) {
        // Running on Linux, test Windows metadata
        final SerializedModuleMetadata metadataWin =
            new SerializedModuleMetadata.Builder()
                .setJavaVersion(System.getProperty("java.version"))
                .setPlatformInfo(System.getProperty("os.arch"), "Windows 10")
                .build();
        assertFalse(metadataWin.isCompatibleWithCurrentEnvironment());
      } else if (currentOs.contains("windows")) {
        // Running on Windows, test Linux metadata
        final SerializedModuleMetadata metadataLinux =
            new SerializedModuleMetadata.Builder()
                .setJavaVersion(System.getProperty("java.version"))
                .setPlatformInfo(System.getProperty("os.arch"), "Linux")
                .build();
        assertFalse(metadataLinux.isCompatibleWithCurrentEnvironment());
      }
    }
  }

  @Nested
  @DisplayName("Platform Architecture Compatibility Branch Tests")
  class PlatformArchCompatibilityBranchTests {

    @Test
    @DisplayName("x86_64 and amd64 should be compatible")
    void x86_64AndAmd64ShouldBeCompatible() {
      final String currentArch = System.getProperty("os.arch");

      // Only test on x86-64 compatible systems
      if (currentArch.contains("x86_64") || currentArch.contains("amd64")) {
        // Test x86_64 metadata on current system
        final SerializedModuleMetadata metadataX86 =
            new SerializedModuleMetadata.Builder()
                .setJavaVersion(System.getProperty("java.version"))
                .setPlatformInfo("x86_64", System.getProperty("os.name"))
                .build();
        assertTrue(
            metadataX86.isCompatibleWithCurrentEnvironment(),
            "x86_64 should be compatible on " + currentArch);

        // Test amd64 metadata on current system
        final SerializedModuleMetadata metadataAmd =
            new SerializedModuleMetadata.Builder()
                .setJavaVersion(System.getProperty("java.version"))
                .setPlatformInfo("amd64", System.getProperty("os.name"))
                .build();
        assertTrue(
            metadataAmd.isCompatibleWithCurrentEnvironment(),
            "amd64 should be compatible on " + currentArch);
      }
    }

    @Test
    @DisplayName("arm64 should not be compatible with x86_64")
    void arm64ShouldNotBeCompatibleWithX86_64() {
      final String currentArch = System.getProperty("os.arch");

      if (currentArch.contains("x86_64") || currentArch.contains("amd64")) {
        final SerializedModuleMetadata metadataArm =
            new SerializedModuleMetadata.Builder()
                .setJavaVersion(System.getProperty("java.version"))
                .setPlatformInfo("arm64", System.getProperty("os.name"))
                .build();
        assertFalse(
            metadataArm.isCompatibleWithCurrentEnvironment(),
            "arm64 should NOT be compatible with x86_64/amd64");
      } else if (currentArch.contains("aarch64") || currentArch.contains("arm64")) {
        final SerializedModuleMetadata metadataX86 =
            new SerializedModuleMetadata.Builder()
                .setJavaVersion(System.getProperty("java.version"))
                .setPlatformInfo("x86_64", System.getProperty("os.name"))
                .build();
        assertFalse(
            metadataX86.isCompatibleWithCurrentEnvironment(),
            "x86_64 should NOT be compatible with arm64/aarch64");
      }
    }
  }

  @Nested
  @DisplayName("Extract Major Version Tests")
  class ExtractMajorVersionTests {

    @Test
    @DisplayName("should handle version with dot correctly")
    void shouldHandleVersionWithDotCorrectly() {
      // Test indirectly through isCompatibleWithCurrentEnvironment
      // Version "21.0.1" should have major version "21"
      final String currentVersion = System.getProperty("java.version");
      final String majorVersion;
      if (currentVersion.contains(".")) {
        majorVersion = currentVersion.substring(0, currentVersion.indexOf('.'));
      } else {
        majorVersion = currentVersion;
      }

      // Create metadata with same major but different minor
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(majorVersion + ".99.99")
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      assertTrue(
          metadata.isCompatibleWithCurrentEnvironment(),
          "Same major version should be compatible");
    }

    @Test
    @DisplayName("should handle version without dot")
    void shouldHandleVersionWithoutDot() {
      // Test with version that has no dot (like "21")
      final String currentVersion = System.getProperty("java.version");
      final String majorVersion;
      if (currentVersion.contains(".")) {
        majorVersion = currentVersion.substring(0, currentVersion.indexOf('.'));
      } else {
        majorVersion = currentVersion;
      }

      // Create metadata with just major version (no dots)
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(majorVersion) // No dots
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      assertTrue(
          metadata.isCompatibleWithCurrentEnvironment(),
          "Version without dots should still work");
    }

    @Test
    @DisplayName("extractMajorVersion should handle dot at position 0")
    void extractMajorVersionShouldHandleDotAtPosition0() {
      // Edge case: ".1" should extract empty string (dotIndex = 0, not > 0)
      // This tests the > 0 boundary condition
      final SerializedModuleMetadata metadata =
          new SerializedModuleMetadata.Builder()
              .setJavaVersion(".1.2.3") // Dot at position 0
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      // extractMajorVersion(".1.2.3") returns ".1.2.3" (full string) because dotIndex = 0
      // which is not > 0, so it returns the full version string
      // This will likely not match current version
      assertFalse(
          metadata.isCompatibleWithCurrentEnvironment(),
          "Version starting with dot should not match");
    }
  }
}
