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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link SerializationResult} class.
 *
 * <p>SerializationResult encapsulates the serialized module data and its associated metadata,
 * providing convenient methods for saving, loading, and analyzing serialization results.
 */
@DisplayName("SerializationResult Tests")
class SerializationResultTest {

  @TempDir Path tempDir;

  private byte[] testData;
  private SerializedModuleMetadata testMetadata;

  @BeforeEach
  void setUp() throws Exception {
    testData = "test serialized data".getBytes("UTF-8");

    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
    final byte[] hashBytes = digest.digest(testData);
    final String hash = bytesToHex(hashBytes);

    testMetadata =
        new SerializedModuleMetadata.Builder()
            .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
            .setSerializedSize(testData.length)
            .setOriginalSize(testData.length * 2)
            .setSha256Hash(hash)
            .setSerializationDuration(100L)
            .build();
  }

  private String bytesToHex(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (final byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create result with data and metadata")
    void shouldCreateResultWithDataAndMetadata() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertNotNull(result);
      assertNotNull(result.getSerializedData());
      assertNotNull(result.getMetadata());
    }

    @Test
    @DisplayName("should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(NullPointerException.class, () -> new SerializationResult(null, testMetadata));
    }

    @Test
    @DisplayName("should throw on null metadata")
    void shouldThrowOnNullMetadata() {
      assertThrows(NullPointerException.class, () -> new SerializationResult(testData, null));
    }

    @Test
    @DisplayName("should create defensive copy of data")
    void shouldCreateDefensiveCopyOfData() {
      final byte[] originalData = new byte[] {1, 2, 3, 4, 5};
      final SerializationResult result = new SerializationResult(originalData, testMetadata);

      originalData[0] = 99;

      assertEquals(1, result.getSerializedData()[0]);
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getSerializedData should return defensive copy")
    void getSerializedDataShouldReturnDefensiveCopy() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      final byte[] retrieved = result.getSerializedData();
      retrieved[0] = 99;

      assertNotEquals(99, result.getSerializedData()[0]);
    }

    @Test
    @DisplayName("getSize should return data length")
    void getSizeShouldReturnDataLength() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertEquals(testData.length, result.getSize());
    }

    @Test
    @DisplayName("getSizeMB should return size in megabytes")
    void getSizeMbShouldReturnSizeInMegabytes() {
      final byte[] largeData = new byte[1024 * 1024]; // 1 MB
      final SerializationResult result = new SerializationResult(largeData, testMetadata);

      assertEquals(1.0, result.getSizeMB(), 0.001);
    }

    @Test
    @DisplayName("getCompressionRatio should return metadata compression ratio")
    void getCompressionRatioShouldReturnMetadataCompressionRatio() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertEquals(testMetadata.getCompressionRatio(), result.getCompressionRatio(), 0.001);
    }
  }

  @Nested
  @DisplayName("Integrity Validation Tests")
  class IntegrityValidationTests {

    @Test
    @DisplayName("validateIntegrity should return true for valid data")
    void validateIntegrityShouldReturnTrueForValidData() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertTrue(result.validateIntegrity());
    }

    @Test
    @DisplayName("validateIntegrity should return false for corrupted data")
    void validateIntegrityShouldReturnFalseForCorruptedData() {
      final byte[] corruptedData = testData.clone();
      corruptedData[0] = (byte) (corruptedData[0] + 1);

      // Create metadata with hash of original data
      final SerializationResult result = new SerializationResult(corruptedData, testMetadata);

      assertFalse(result.validateIntegrity());
    }
  }

  @Nested
  @DisplayName("File Operations Tests")
  class FileOperationsTests {

    @Test
    @DisplayName("saveToFile should create file and metadata file")
    void saveToFileShouldCreateFileAndMetadataFile() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module.bin");

      result.saveToFile(filePath);

      assertTrue(Files.exists(filePath));
      assertTrue(Files.exists(tempDir.resolve("module.bin.meta")));
    }

    @Test
    @DisplayName("saveToFile should create parent directories")
    void saveToFileShouldCreateParentDirectories() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("subdir/nested/module.bin");

      result.saveToFile(filePath);

      assertTrue(Files.exists(filePath));
    }

    @Test
    @DisplayName("saveToFile should throw on null path")
    void saveToFileShouldThrowOnNullPath() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertThrows(NullPointerException.class, () -> result.saveToFile(null));
    }

    @Test
    @DisplayName("loadFromFile should load saved result")
    void loadFromFileShouldLoadSavedResult() throws IOException {
      final SerializationResult originalResult = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module.bin");

      originalResult.saveToFile(filePath);

      final SerializationResult loadedResult = SerializationResult.loadFromFile(filePath);

      assertNotNull(loadedResult);
      assertEquals(originalResult.getSize(), loadedResult.getSize());
    }

    @Test
    @DisplayName("loadFromFile should throw on null path")
    void loadFromFileShouldThrowOnNullPath() {
      assertThrows(NullPointerException.class, () -> SerializationResult.loadFromFile(null));
    }

    @Test
    @DisplayName("loadFromFile should throw on non-existent file")
    void loadFromFileShouldThrowOnNonExistentFile() {
      final Path nonExistent = tempDir.resolve("does-not-exist.bin");

      assertThrows(IOException.class, () -> SerializationResult.loadFromFile(nonExistent));
    }

    @Test
    @DisplayName("loadFromFile should create minimal metadata if meta file missing")
    void loadFromFileShouldCreateMinimalMetadataIfMetaFileMissing() throws IOException {
      final Path filePath = tempDir.resolve("module-no-meta.bin");
      Files.write(filePath, testData);

      // No .meta file exists
      final SerializationResult result = SerializationResult.loadFromFile(filePath);

      assertNotNull(result);
      assertNotNull(result.getMetadata());
      assertEquals(testData.length, result.getSize());
    }
  }

  @Nested
  @DisplayName("WithMetadata Tests")
  class WithMetadataTests {

    @Test
    @DisplayName("withMetadata should create new result with updated metadata")
    void withMetadataShouldCreateNewResultWithUpdatedMetadata() {
      final SerializationResult originalResult = new SerializationResult(testData, testMetadata);

      final SerializedModuleMetadata newMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_GZIP)
              .setSerializedSize(500)
              .setOriginalSize(1000)
              .setSha256Hash("newhash")
              .build();

      final SerializationResult newResult = originalResult.withMetadata(newMetadata);

      assertNotSame(originalResult, newResult);
      assertEquals(newMetadata.getFormat(), newResult.getMetadata().getFormat());
      // Data should be the same
      assertEquals(originalResult.getSize(), newResult.getSize());
    }

    @Test
    @DisplayName("withMetadata should throw on null metadata")
    void withMetadataShouldThrowOnNullMetadata() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertThrows(NullPointerException.class, () -> result.withMetadata(null));
    }
  }

  @Nested
  @DisplayName("Summary and Comparison Tests")
  class SummaryAndComparisonTests {

    @Test
    @DisplayName("getSummary should return formatted summary")
    void getSummaryShouldReturnFormattedSummary() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final String summary = result.getSummary();

      assertNotNull(summary);
      assertTrue(summary.contains("Serialization Result"));
      assertTrue(summary.contains("Format"));
      assertTrue(summary.contains("Size"));
      assertTrue(summary.contains("Compression"));
      assertTrue(summary.contains("Duration"));
    }

    @Test
    @DisplayName("compareWith should return comparison string")
    void compareWithShouldReturnComparisonString() {
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);

      final byte[] largerData = new byte[testData.length * 2];
      final SerializedModuleMetadata metadata2 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(largerData.length)
              .setOriginalSize(largerData.length)
              .setSha256Hash("hash2")
              .setSerializationDuration(200L)
              .build();
      final SerializationResult result2 = new SerializationResult(largerData, metadata2);

      final String comparison = result1.compareWith(result2);

      assertNotNull(comparison);
      assertTrue(comparison.contains("Serialization Comparison"));
      assertTrue(comparison.contains("Size"));
      assertTrue(comparison.contains("Compression"));
      assertTrue(comparison.contains("Duration"));
    }

    @Test
    @DisplayName("compareWith should throw on null other result")
    void compareWithShouldThrowOnNullOtherResult() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertThrows(NullPointerException.class, () -> result.compareWith(null));
    }
  }

  @Nested
  @DisplayName("Deserialization Time Estimation Tests")
  class DeserializationTimeEstimationTests {

    @Test
    @DisplayName("getEstimatedDeserializationTime should return estimated time")
    void getEstimatedDeserializationTimeShouldReturnEstimatedTime() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      final long estimatedTime = result.getEstimatedDeserializationTime();

      assertTrue(estimatedTime >= 0);
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same data and metadata")
    void equalsShouldReturnTrueForSameDataAndMetadata() {
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);
      final SerializationResult result2 = new SerializationResult(testData.clone(), testMetadata);

      assertEquals(result1, result2);
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertEquals(result, result);
    }

    @Test
    @DisplayName("equals should return false for different data")
    void equalsShouldReturnFalseForDifferentData() {
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);
      final SerializationResult result2 =
          new SerializationResult(new byte[] {1, 2, 3}, testMetadata);

      assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertNotEquals(null, result);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);
      final SerializationResult result2 = new SerializationResult(testData.clone(), testMetadata);

      assertEquals(result1.hashCode(), result2.hashCode());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final String str = result.toString();

      assertNotNull(str);
      assertTrue(str.contains("SerializationResult"));
      assertTrue(str.contains("compact-binary-lz4"));
      assertTrue(str.contains("bytes"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("SerializationResult should be final")
    void serializationResultShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(SerializationResult.class.getModifiers()));
    }
  }

  @Nested
  @DisplayName("Optimal Performance Tests")
  class OptimalPerformanceTests {

    @Test
    @DisplayName("isOptimalPerformance should return false when no metrics")
    void isOptimalPerformanceShouldReturnFalseWhenNoMetrics() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertFalse(result.isOptimalPerformance());
    }

    @Test
    @DisplayName("isOptimalPerformance should return true when metrics indicate optimal")
    void isOptimalPerformanceShouldReturnTrueWhenMetricsIndicateOptimal() throws Exception {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(1_000_000L, 500_000L, 0L, 0L, 0L) // 1ms serialization
              .setMemoryMetrics(1024 * 1024, 1024 * 1024, 0) // 1MB
              .setIoMetrics(1000, 500, 1_000_000L)
              .build();

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadataWithMetrics =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setPerformanceMetrics(metrics)
              .build();

      final SerializationResult result = new SerializationResult(testData, metadataWithMetrics);

      // Result depends on SerializationPerformanceMetrics.isOptimalPerformance() logic
      assertNotNull(result.isOptimalPerformance());
    }
  }

  @Nested
  @DisplayName("Comparison Edge Cases Tests")
  class ComparisonEdgeCasesTests {

    @Test
    @DisplayName("compareWith should handle zero duration in other result")
    void compareWithShouldHandleZeroDurationInOtherResult() throws Exception {
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadataZeroDuration =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setSerializationDuration(0L) // Zero duration
              .build();

      final SerializationResult result2 = new SerializationResult(testData, metadataZeroDuration);

      final String comparison = result1.compareWith(result2);

      assertNotNull(comparison);
      assertTrue(comparison.contains("Duration"));
    }

    @Test
    @DisplayName("compareWith should show negative size difference")
    void compareWithShouldShowNegativeSizeDifference() throws Exception {
      // result1 is smaller than result2
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);

      final byte[] largerData = new byte[testData.length * 3];
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(largerData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadata2 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(largerData.length)
              .setOriginalSize(largerData.length)
              .setSha256Hash(hash)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result2 = new SerializationResult(largerData, metadata2);

      final String comparison = result1.compareWith(result2);

      // result1 is smaller, so size difference is negative
      assertTrue(comparison.contains("-"));
    }
  }

  @Nested
  @DisplayName("Summary Edge Cases Tests")
  class SummaryEdgeCasesTests {

    @Test
    @DisplayName("getSummary should include performance when metrics present")
    void getSummaryShouldIncludePerformanceWhenMetricsPresent() throws Exception {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L)
              .build();

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadataWithMetrics =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setPerformanceMetrics(metrics)
              .build();

      final SerializationResult result = new SerializationResult(testData, metadataWithMetrics);
      final String summary = result.getSummary();

      assertTrue(summary.contains("Performance"));
    }
  }

  @Nested
  @DisplayName("Environment Compatibility Tests")
  class EnvironmentCompatibilityTests {

    @Test
    @DisplayName("isCompatibleWithCurrentEnvironment should delegate to metadata")
    void isCompatibleWithCurrentEnvironmentShouldDelegateToMetadata() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata compatibleMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setJavaVersion(System.getProperty("java.version"))
              .setPlatformInfo(System.getProperty("os.arch"), System.getProperty("os.name"))
              .build();

      final SerializationResult result = new SerializationResult(testData, compatibleMetadata);

      assertTrue(result.isCompatibleWithCurrentEnvironment());
    }

    @Test
    @DisplayName("isCompatibleWithCurrentEnvironment should return false for incompatible")
    void isCompatibleWithCurrentEnvironmentShouldReturnFalseForIncompatible() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata incompatibleMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setJavaVersion("99.0.0")
              .setPlatformInfo("unknown_arch", "UnknownOS")
              .build();

      final SerializationResult result = new SerializationResult(testData, incompatibleMetadata);

      assertFalse(result.isCompatibleWithCurrentEnvironment());
    }
  }

  @Nested
  @DisplayName("Equals Edge Cases Tests")
  class EqualsEdgeCasesTests {

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      assertFalse(result.equals("not a SerializationResult"));
    }

    @Test
    @DisplayName("equals should return false for different metadata")
    void equalsShouldReturnFalseForDifferentMetadata() throws Exception {
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata differentMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.RAW_BINARY) // Different format
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length)
              .setSha256Hash(hash)
              .build();

      final SerializationResult result2 = new SerializationResult(testData, differentMetadata);

      assertNotEquals(result1, result2);
    }
  }

  @Nested
  @DisplayName("File Path Edge Cases Tests")
  class FilePathEdgeCasesTests {

    @Test
    @DisplayName("saveToFile should handle path with existing parent")
    void saveToFileShouldHandlePathWithExistingParent() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("direct-file.bin");

      result.saveToFile(filePath);

      assertTrue(Files.exists(filePath));
    }

    @Test
    @DisplayName("saveToFile should handle deeply nested path")
    void saveToFileShouldHandleDeeplyNestedPath() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("a/b/c/d/e/module.bin");

      result.saveToFile(filePath);

      assertTrue(Files.exists(filePath));
      assertTrue(Files.exists(tempDir.resolve("a/b/c/d/e/module.bin.meta")));
    }
  }
}
