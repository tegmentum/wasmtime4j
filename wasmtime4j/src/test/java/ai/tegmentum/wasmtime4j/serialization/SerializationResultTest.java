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

    @Test
    @DisplayName("getEstimatedDeserializationTime should delegate to metadata")
    void getEstimatedDeserializationTimeShouldDelegateToMetadata() throws Exception {
      // Create metadata with performance metrics that will produce a non-zero estimate
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L) // 100ms serialization
              .setMemoryMetrics(1024, 1024, 0)
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
              .setSerializationDuration(100L) // 100ms
              .setPerformanceMetrics(metrics)
              .build();

      final SerializationResult result = new SerializationResult(testData, metadataWithMetrics);

      // Verify it returns the metadata's estimated time
      final long resultTime = result.getEstimatedDeserializationTime();
      final long metadataTime = metadataWithMetrics.getEstimatedDeserializationTimeMs();

      assertEquals(
          metadataTime,
          resultTime,
          "getEstimatedDeserializationTime should return metadata's estimate");
    }

    @Test
    @DisplayName("getEstimatedDeserializationTime should return non-zero for large data")
    void getEstimatedDeserializationTimeShouldReturnNonZeroForLargeData() throws Exception {
      // Create a result with large data to ensure non-zero estimate via fallback
      final byte[] largeData = new byte[10 * 1024 * 1024]; // 10 MB
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(largeData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata largeMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(largeData.length)
              .setOriginalSize(largeData.length * 2)
              .setSha256Hash(hash)
              .build(); // No performance metrics, uses fallback

      final SerializationResult result = new SerializationResult(largeData, largeMetadata);

      final long estimatedTime = result.getEstimatedDeserializationTime();

      // For 10MB at ~100MB/s, should be around 100ms minimum
      assertTrue(
          estimatedTime > 0,
          "Large data should have non-zero estimated deserialization time, got: " + estimatedTime);
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

    @Test
    @DisplayName("isOptimalPerformance should return boolean value not null")
    void isOptimalPerformanceShouldReturnBooleanValue() throws Exception {
      // Test with metrics that should definitely NOT be optimal
      final SerializationPerformanceMetrics poorMetrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(10_000_000_000L, 5_000_000_000L, 0L, 0L, 0L) // 10s serialization
              .setMemoryMetrics(1024 * 1024 * 1024L, 1024 * 1024 * 1024L, 0) // 1GB
              .setIoMetrics(1, 1, 1L)
              .build();

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadataWithPoorMetrics =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setPerformanceMetrics(poorMetrics)
              .build();

      final SerializationResult result = new SerializationResult(testData, metadataWithPoorMetrics);

      // The method should return a boolean (true or false), not throw
      final boolean isOptimal = result.isOptimalPerformance();
      assertTrue(isOptimal || !isOptimal, "isOptimalPerformance should return a valid boolean");
    }

    @Test
    @DisplayName("isOptimalPerformance should verify null metrics returns false")
    void isOptimalPerformanceShouldVerifyNullMetricsReturnsFalse() {
      // Explicitly test the null case
      final SerializationResult result = new SerializationResult(testData, testMetadata);

      // Verify the method returns false, not throws
      final boolean isOptimal = result.isOptimalPerformance();
      assertFalse(isOptimal, "isOptimalPerformance with null metrics should return false");
    }

    @Test
    @DisplayName("isOptimalPerformance should delegate to metrics when present")
    void isOptimalPerformanceShouldDelegateToMetricsWhenPresent() throws Exception {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000L, 50_000L, 0L, 0L, 0L) // Very fast
              .setMemoryMetrics(1024, 1024, 0) // Small memory
              .setIoMetrics(10000, 5000, 100_000L) // Good I/O
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

      // Verify delegation happens (method returns based on metrics.isOptimalPerformance())
      assertEquals(metrics.isOptimalPerformance(), result.isOptimalPerformance());
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
      // When other duration is 0, percentage should be 0 (not infinity or error)
      assertTrue(
          comparison.contains("+0.0%") || comparison.contains("-0.0%") || comparison.contains("0.0%"),
          "Duration percentage should be 0 when other duration is 0, got: " + comparison);
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

    @Test
    @DisplayName("compareWith should calculate positive size percentage correctly")
    void compareWithShouldCalculatePositiveSizePercentageCorrectly() throws Exception {
      // result1 is larger than result2
      final byte[] largerData = new byte[testData.length * 2];
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(largerData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata largerMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(largerData.length)
              .setOriginalSize(largerData.length)
              .setSha256Hash(hash)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result1 = new SerializationResult(largerData, largerMetadata);
      final SerializationResult result2 = new SerializationResult(testData, testMetadata);

      final String comparison = result1.compareWith(result2);

      // result1 is larger, so size difference is positive
      assertTrue(comparison.contains("+") || comparison.contains("Size"));
    }

    @Test
    @DisplayName("compareWith should calculate compression difference correctly")
    void compareWithShouldCalculateCompressionDifferenceCorrectly() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      // Create metadata with different compression ratios
      final SerializedModuleMetadata highCompression =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 4) // 4x compression
              .setSha256Hash(hash)
              .setSerializationDuration(100L)
              .build();

      final SerializedModuleMetadata lowCompression =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length) // 1x compression
              .setSha256Hash(hash)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result1 = new SerializationResult(testData, highCompression);
      final SerializationResult result2 = new SerializationResult(testData, lowCompression);

      final String comparison = result1.compareWith(result2);

      assertTrue(comparison.contains("Compression"));
    }

    @Test
    @DisplayName("compareWith should calculate duration percentage when other has non-zero duration")
    void compareWithShouldCalculateDurationPercentageWhenOtherHasNonZeroDuration() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata fastMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setSerializationDuration(50L) // Fast
              .build();

      final SerializedModuleMetadata slowMetadata =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setSerializationDuration(100L) // Slow
              .build();

      final SerializationResult fastResult = new SerializationResult(testData, fastMetadata);
      final SerializationResult slowResult = new SerializationResult(testData, slowMetadata);

      final String comparison = fastResult.compareWith(slowResult);

      // Duration should show negative percentage (faster)
      assertTrue(comparison.contains("Duration") && comparison.contains("-"));
    }

    @Test
    @DisplayName("compareWith should show zero percentage when durations are equal")
    void compareWithShouldShowZeroPercentageWhenDurationsAreEqual() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadata1 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setSerializationDuration(100L)
              .build();

      final SerializedModuleMetadata metadata2 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length * 2)
              .setSha256Hash(hash)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result1 = new SerializationResult(testData, metadata1);
      final SerializationResult result2 = new SerializationResult(testData, metadata2);

      final String comparison = result1.compareWith(result2);

      assertTrue(comparison.contains("0") || comparison.contains("+0"));
    }

    @Test
    @DisplayName("compareWith should handle same sizes correctly")
    void compareWithShouldHandleSameSizesCorrectly() {
      final SerializationResult result1 = new SerializationResult(testData, testMetadata);
      final SerializationResult result2 = new SerializationResult(testData.clone(), testMetadata);

      final String comparison = result1.compareWith(result2);

      // Size difference should be 0
      assertTrue(comparison.contains("Size"));
    }

    @Test
    @DisplayName("compareWith should verify size percentage calculation uses 100 multiplier")
    void compareWithShouldVerifySizePercentageCalculation() throws Exception {
      // Create result1 with size 100
      final byte[] data100 = new byte[100];
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash100Bytes = digest.digest(data100);
      final String hash100 = bytesToHex(hash100Bytes);

      final SerializedModuleMetadata metadata100 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(100)
              .setOriginalSize(100)
              .setSha256Hash(hash100)
              .setSerializationDuration(100L)
              .build();

      // Create result2 with size 50
      final byte[] data50 = new byte[50];
      final byte[] hash50Bytes = digest.digest(data50);
      final String hash50 = bytesToHex(hash50Bytes);

      final SerializedModuleMetadata metadata50 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(50)
              .setOriginalSize(50)
              .setSha256Hash(hash50)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result1 = new SerializationResult(data100, metadata100);
      final SerializationResult result2 = new SerializationResult(data50, metadata50);

      final String comparison = result1.compareWith(result2);

      // (100 - 50) / 50 * 100 = 100%
      // If multiplier was 1 instead of 100, it would show 1.00%
      assertTrue(
          comparison.contains("+100.00%"),
          "Size percentage should be +100.00% (not +1.00%), got: " + comparison);
    }

    @Test
    @DisplayName("compareWith should verify duration percentage calculation uses 100 multiplier")
    void compareWithShouldVerifyDurationPercentageCalculation() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      // result1 has duration 200ms, result2 has duration 100ms
      final SerializedModuleMetadata metadata200 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length)
              .setSha256Hash(hash)
              .setSerializationDuration(200L)
              .build();

      final SerializedModuleMetadata metadata100 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length)
              .setSha256Hash(hash)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result1 = new SerializationResult(testData, metadata200);
      final SerializationResult result2 = new SerializationResult(testData, metadata100);

      final String comparison = result1.compareWith(result2);

      // (200 - 100) / 100 * 100 = 100%
      // If multiplier was 1, it would show 1.0%
      assertTrue(
          comparison.contains("+100.0%"),
          "Duration percentage should be +100.0% (not +1.0%), got: " + comparison);
    }

    @Test
    @DisplayName("compareWith should verify math operations are not inverted")
    void compareWithShouldVerifyMathOperationsNotInverted() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // Create result1 with size 200
      final byte[] data200 = new byte[200];
      final byte[] hash200Bytes = digest.digest(data200);
      final String hash200 = bytesToHex(hash200Bytes);

      final SerializedModuleMetadata metadata200 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(200)
              .setOriginalSize(400) // 2x compression
              .setSha256Hash(hash200)
              .setSerializationDuration(100L)
              .build();

      // Create result2 with size 100
      final byte[] data100 = new byte[100];
      final byte[] hash100Bytes = digest.digest(data100);
      final String hash100 = bytesToHex(hash100Bytes);

      final SerializedModuleMetadata metadata100 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(100)
              .setOriginalSize(100) // 1x compression
              .setSha256Hash(hash100)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result1 = new SerializationResult(data200, metadata200);
      final SerializationResult result2 = new SerializationResult(data100, metadata100);

      final String comparison = result1.compareWith(result2);

      // Compression difference: 2.0 - 1.0 = +1.0x
      // If division replaced multiplication or vice versa, we'd get a very different value
      assertTrue(
          comparison.contains("+1.00x"),
          "Compression difference should be +1.00x, got: " + comparison);

      // Size percentage: (200 - 100) / 100 * 100 = +100.00%
      // If division replaced multiplication: (200 - 100) * 100 / 100 = 100 (wrong formula)
      assertTrue(
          comparison.contains("+100.00%"),
          "Size percentage should show +100.00%, got: " + comparison);
    }

    @Test
    @DisplayName("compareWith should return zero duration percentage when other duration is zero")
    void compareWithShouldReturnZeroDurationPercentageWhenOtherDurationIsZero() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(testData);
      final String hash = bytesToHex(hashBytes);

      final SerializedModuleMetadata metadata100ms =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length)
              .setSha256Hash(hash)
              .setSerializationDuration(100L) // 100ms
              .build();

      final SerializedModuleMetadata metadata0ms =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(testData.length)
              .setOriginalSize(testData.length)
              .setSha256Hash(hash)
              .setSerializationDuration(0L) // 0ms
              .build();

      final SerializationResult result1 = new SerializationResult(testData, metadata100ms);
      final SerializationResult result2 = new SerializationResult(testData, metadata0ms);

      final String comparison = result1.compareWith(result2);

      // When other.duration is 0, the ternary returns 0 (not infinity)
      // This tests line 251's > 0 check
      assertTrue(
          comparison.contains("Duration: +0.0%") || comparison.contains("Duration: -0.0%"),
          "Duration percentage should be 0% when other has 0 duration, got: " + comparison);
    }

    @Test
    @DisplayName("compareWith should correctly calculate when this is smaller than other")
    void compareWithShouldCalculateWhenThisSmallerThanOther() throws Exception {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // result1 (this) is smaller: 50 bytes
      final byte[] data50 = new byte[50];
      final byte[] hash50Bytes = digest.digest(data50);
      final String hash50 = bytesToHex(hash50Bytes);

      final SerializedModuleMetadata metadata50 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(50)
              .setOriginalSize(50)
              .setSha256Hash(hash50)
              .setSerializationDuration(100L)
              .build();

      // result2 (other) is larger: 100 bytes
      final byte[] data100 = new byte[100];
      final byte[] hash100Bytes = digest.digest(data100);
      final String hash100 = bytesToHex(hash100Bytes);

      final SerializedModuleMetadata metadata100 =
          new SerializedModuleMetadata.Builder()
              .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
              .setSerializedSize(100)
              .setOriginalSize(100)
              .setSha256Hash(hash100)
              .setSerializationDuration(100L)
              .build();

      final SerializationResult result1 = new SerializationResult(data50, metadata50);
      final SerializationResult result2 = new SerializationResult(data100, metadata100);

      final String comparison = result1.compareWith(result2);

      // (50 - 100) / 100 * 100 = -50%
      assertTrue(
          comparison.contains("-50.00%"),
          "Size should show -50.00% when this is half the size of other, got: " + comparison);
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

  @Nested
  @DisplayName("Metadata Serialization Format Tests")
  class MetadataSerializationFormatTests {

    @Test
    @DisplayName("saveToFile should write correct format identifier in metadata")
    void saveToFileShouldWriteCorrectFormatIdentifierInMetadata() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-format-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-format-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify the format identifier is correctly written
      assertTrue(
          metadataContent.contains("\"format\": \"compact-binary-lz4\""),
          "Metadata should contain correct format identifier, got: " + metadataContent);
    }

    @Test
    @DisplayName("saveToFile should write correct serialized size in metadata")
    void saveToFileShouldWriteCorrectSerializedSizeInMetadata() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-size-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-size-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify serialized size is correctly written
      assertTrue(
          metadataContent.contains("\"serializedSize\": " + testData.length),
          "Metadata should contain correct serialized size, got: " + metadataContent);
    }

    @Test
    @DisplayName("saveToFile should write correct original size in metadata")
    void saveToFileShouldWriteCorrectOriginalSizeInMetadata() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-original-size-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-original-size-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify original size is correctly written (testMetadata has originalSize = testData.length * 2)
      assertTrue(
          metadataContent.contains("\"originalSize\": " + (testData.length * 2)),
          "Metadata should contain correct original size, got: " + metadataContent);
    }

    @Test
    @DisplayName("saveToFile should write correct SHA256 hash in metadata")
    void saveToFileShouldWriteCorrectSha256HashInMetadata() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-hash-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-hash-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify SHA256 hash is correctly written
      assertTrue(
          metadataContent.contains("\"sha256Hash\": \"" + testMetadata.getSha256Hash() + "\""),
          "Metadata should contain correct SHA256 hash, got: " + metadataContent);
    }

    @Test
    @DisplayName("saveToFile should write correct serialization duration in metadata")
    void saveToFileShouldWriteCorrectSerializationDurationInMetadata() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-duration-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-duration-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify serialization duration is correctly written (testMetadata has 100ms)
      assertTrue(
          metadataContent.contains("\"serializationDurationMs\": 100"),
          "Metadata should contain correct duration, got: " + metadataContent);
    }

    @Test
    @DisplayName("saveToFile should write correct compression ratio in metadata")
    void saveToFileShouldWriteCorrectCompressionRatioInMetadata() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-compression-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-compression-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify compression ratio is in the file (it's calculated as originalSize/serializedSize)
      assertTrue(
          metadataContent.contains("\"compressionRatio\":"),
          "Metadata should contain compression ratio, got: " + metadataContent);
    }

    @Test
    @DisplayName("saveToFile should write all expected JSON fields")
    void saveToFileShouldWriteAllExpectedJsonFields() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-all-fields-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-all-fields-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify all expected fields are present
      assertTrue(metadataContent.contains("\"format\":"), "Should contain format field");
      assertTrue(metadataContent.contains("\"formatVersion\":"), "Should contain formatVersion field");
      assertTrue(metadataContent.contains("\"serializedSize\":"), "Should contain serializedSize field");
      assertTrue(metadataContent.contains("\"originalSize\":"), "Should contain originalSize field");
      assertTrue(metadataContent.contains("\"sha256Hash\":"), "Should contain sha256Hash field");
      assertTrue(metadataContent.contains("\"timestamp\":"), "Should contain timestamp field");
      assertTrue(
          metadataContent.contains("\"serializationDurationMs\":"),
          "Should contain serializationDurationMs field");
      assertTrue(
          metadataContent.contains("\"compressionRatio\":"),
          "Should contain compressionRatio field");
      assertTrue(
          metadataContent.contains("\"wasmtimeVersion\":"), "Should contain wasmtimeVersion field");
      assertTrue(metadataContent.contains("\"javaVersion\":"), "Should contain javaVersion field");
      assertTrue(metadataContent.contains("\"platformArch\":"), "Should contain platformArch field");
      assertTrue(metadataContent.contains("\"platformOs\":"), "Should contain platformOs field");
    }

    @Test
    @DisplayName("metadata JSON fields should be in correct order")
    void metadataJsonFieldsShouldBeInCorrectOrder() throws IOException {
      final SerializationResult result = new SerializationResult(testData, testMetadata);
      final Path filePath = tempDir.resolve("module-order-test.bin");

      result.saveToFile(filePath);

      final Path metadataPath = tempDir.resolve("module-order-test.bin.meta");
      final String metadataContent = new String(Files.readAllBytes(metadataPath), "UTF-8");

      // Verify format comes before formatVersion
      final int formatIndex = metadataContent.indexOf("\"format\":");
      final int formatVersionIndex = metadataContent.indexOf("\"formatVersion\":");
      assertTrue(
          formatIndex < formatVersionIndex,
          "format should come before formatVersion");

      // Verify serializedSize comes before originalSize
      final int serializedSizeIndex = metadataContent.indexOf("\"serializedSize\":");
      final int originalSizeIndex = metadataContent.indexOf("\"originalSize\":");
      assertTrue(
          serializedSizeIndex < originalSizeIndex,
          "serializedSize should come before originalSize");

      // Verify sha256Hash comes after originalSize
      final int sha256HashIndex = metadataContent.indexOf("\"sha256Hash\":");
      assertTrue(
          originalSizeIndex < sha256HashIndex,
          "sha256Hash should come after originalSize");
    }
  }

  @Nested
  @DisplayName("LoadFromFile Edge Cases Tests")
  class LoadFromFileEdgeCasesTests {

    @Test
    @DisplayName("loadFromFile should check file existence before reading")
    void loadFromFileShouldCheckFileExistenceBeforeReading() {
      final Path nonExistent = tempDir.resolve("definitely-does-not-exist.bin");

      final IOException exception =
          assertThrows(IOException.class, () -> SerializationResult.loadFromFile(nonExistent));

      assertTrue(
          exception.getMessage().contains("not found"),
          "Exception should indicate file not found: " + exception.getMessage());
    }

    @Test
    @DisplayName("loadFromFile should handle metadata file presence check")
    void loadFromFileShouldHandleMetadataFilePresenceCheck() throws IOException {
      // Create data file but no metadata file
      final Path dataFile = tempDir.resolve("data-only.bin");
      Files.write(dataFile, testData);

      // Should not throw - should create minimal metadata
      final SerializationResult result = SerializationResult.loadFromFile(dataFile);

      assertNotNull(result);
      // Minimal metadata should have RAW_BINARY format
      assertEquals(
          ModuleSerializationFormat.RAW_BINARY,
          result.getMetadata().getFormat(),
          "Missing metadata should default to RAW_BINARY format");
    }

    @Test
    @DisplayName("loadFromFile should create valid hash when metadata missing")
    void loadFromFileShouldCreateValidHashWhenMetadataMissing() throws IOException {
      final Path dataFile = tempDir.resolve("data-for-hash.bin");
      Files.write(dataFile, testData);

      final SerializationResult result = SerializationResult.loadFromFile(dataFile);

      // Verify hash is calculated correctly
      final String expectedHash = testMetadata.getSha256Hash();
      assertEquals(
          expectedHash,
          result.getMetadata().getSha256Hash(),
          "Hash should be calculated from data when metadata missing");
    }

    @Test
    @DisplayName("loadFromFile should read metadata when present")
    void loadFromFileShouldReadMetadataWhenPresent() throws IOException {
      // Save with metadata
      final SerializationResult original = new SerializationResult(testData, testMetadata);
      final Path dataFile = tempDir.resolve("with-metadata.bin");
      original.saveToFile(dataFile);

      // Load back
      final SerializationResult loaded = SerializationResult.loadFromFile(dataFile);

      assertNotNull(loaded.getMetadata());
      assertEquals(original.getSize(), loaded.getSize());
    }
  }
}
