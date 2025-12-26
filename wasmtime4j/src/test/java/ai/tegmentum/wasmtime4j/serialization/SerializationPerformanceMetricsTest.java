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
 * Tests for {@link SerializationPerformanceMetrics} class.
 *
 * <p>SerializationPerformanceMetrics captures detailed performance data about serialization and
 * deserialization operations to enable optimization and performance analysis.
 */
@DisplayName("SerializationPerformanceMetrics Tests")
class SerializationPerformanceMetricsTest {

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create metrics with default values")
    void shouldCreateMetricsWithDefaultValues() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder().build();

      assertNotNull(metrics);
      assertEquals(0L, metrics.getSerializationTimeNs());
      assertEquals(0L, metrics.getDeserializationTimeNs());
      assertEquals(0L, metrics.getCompressionTimeNs());
      assertEquals(0L, metrics.getDecompressionTimeNs());
      assertEquals(0L, metrics.getHashCalculationTimeNs());
    }

    @Test
    @DisplayName("should set timing metrics")
    void shouldSetTimingMetrics() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(
                  1_000_000_000L, // 1 second serialization
                  500_000_000L, // 0.5 second deserialization
                  200_000_000L, // 0.2 second compression
                  100_000_000L, // 0.1 second decompression
                  50_000_000L) // 0.05 second hash
              .build();

      assertEquals(1_000_000_000L, metrics.getSerializationTimeNs());
      assertEquals(1000L, metrics.getSerializationTimeMs());
      assertEquals(500_000_000L, metrics.getDeserializationTimeNs());
      assertEquals(500L, metrics.getDeserializationTimeMs());
      assertEquals(200_000_000L, metrics.getCompressionTimeNs());
      assertEquals(200L, metrics.getCompressionTimeMs());
      assertEquals(100_000_000L, metrics.getDecompressionTimeNs());
      assertEquals(100L, metrics.getDecompressionTimeMs());
      assertEquals(50_000_000L, metrics.getHashCalculationTimeNs());
      assertEquals(50L, metrics.getHashCalculationTimeMs());
    }

    @Test
    @DisplayName("should set throughput metrics")
    void shouldSetThroughputMetrics() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setThroughputMetrics(100.0, 200.0, 150.0, 180.0)
              .build();

      assertEquals(100.0, metrics.getSerializationThroughputMbps(), 0.001);
      assertEquals(200.0, metrics.getDeserializationThroughputMbps(), 0.001);
      assertEquals(150.0, metrics.getCompressionThroughputMbps(), 0.001);
      assertEquals(180.0, metrics.getDecompressionThroughputMbps(), 0.001);
    }

    @Test
    @DisplayName("should set memory metrics")
    void shouldSetMemoryMetrics() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setMemoryMetrics(
                  1024L * 1024L * 100L, // 100 MB peak
                  1024L * 1024L * 50L, // 50 MB average
                  1024L * 1024L * 25L) // 25 MB temp
              .build();

      assertEquals(1024L * 1024L * 100L, metrics.getPeakMemoryUsageBytes());
      assertEquals(1024L * 1024L * 50L, metrics.getAvgMemoryUsageBytes());
      assertEquals(1024L * 1024L * 25L, metrics.getTempMemoryAllocatedBytes());
    }

    @Test
    @DisplayName("should set CPU metrics")
    void shouldSetCpuMetrics() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder().setCpuMetrics(45.5, 92.3).build();

      assertEquals(45.5, metrics.getAvgCpuUsagePercent(), 0.001);
      assertEquals(92.3, metrics.getPeakCpuUsagePercent(), 0.001);
    }

    @Test
    @DisplayName("should set IO metrics")
    void shouldSetIoMetrics() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setIoMetrics(
                  1024L * 1024L * 10L, // 10 MB read
                  1024L * 1024L * 5L, // 5 MB written
                  100_000_000L) // 100 ms IO time
              .build();

      assertEquals(1024L * 1024L * 10L, metrics.getBytesRead());
      assertEquals(1024L * 1024L * 5L, metrics.getBytesWritten());
      assertEquals(100_000_000L, metrics.getDiskIoTimeNs());
      assertEquals(100L, metrics.getDiskIoTimeMs());
    }

    @Test
    @DisplayName("should set compression efficiency")
    void shouldSetCompressionEfficiency() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder().setCompressionEfficiency(0.65).build();

      assertEquals(0.65, metrics.getCompressionEfficiency(), 0.001);
    }

    @Test
    @DisplayName("should calculate throughput from data size")
    void shouldCalculateThroughputFromDataSize() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(
                  1_000_000_000L, // 1 second serialization
                  500_000_000L, // 0.5 second deserialization
                  0L,
                  0L,
                  0L)
              .calculateThroughput(1024L * 1024L * 10L) // 10 MB
              .build();

      // 10 MB in 1 second = 10 MB/s
      assertEquals(10.0, metrics.getSerializationThroughputMbps(), 0.1);
      // 10 MB in 0.5 second = 20 MB/s
      assertEquals(20.0, metrics.getDeserializationThroughputMbps(), 0.1);
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("setTimingMetrics should throw on negative serialization time")
    void setTimingMetricsShouldThrowOnNegativeSerializationTime() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new SerializationPerformanceMetrics.Builder().setTimingMetrics(-1L, 0L, 0L, 0L, 0L));
    }

    @Test
    @DisplayName("setTimingMetrics should throw on negative deserialization time")
    void setTimingMetricsShouldThrowOnNegativeDeserializationTime() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new SerializationPerformanceMetrics.Builder().setTimingMetrics(0L, -1L, 0L, 0L, 0L));
    }

    @Test
    @DisplayName("setThroughputMetrics should throw on negative throughput")
    void setThroughputMetricsShouldThrowOnNegativeThroughput() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new SerializationPerformanceMetrics.Builder()
                  .setThroughputMetrics(-1.0, 0.0, 0.0, 0.0));
    }

    @Test
    @DisplayName("setMemoryMetrics should throw on negative memory")
    void setMemoryMetricsShouldThrowOnNegativeMemory() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().setMemoryMetrics(-1L, 0L, 0L));
    }

    @Test
    @DisplayName("setCpuMetrics should throw on CPU below 0")
    void setCpuMetricsShouldThrowOnCpuBelowZero() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().setCpuMetrics(-0.1, 50.0));
    }

    @Test
    @DisplayName("setCpuMetrics should throw on CPU above 100")
    void setCpuMetricsShouldThrowOnCpuAbove100() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().setCpuMetrics(50.0, 100.1));
    }

    @Test
    @DisplayName("setIoMetrics should throw on negative bytes")
    void setIoMetricsShouldThrowOnNegativeBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().setIoMetrics(-1L, 0L, 0L));
    }

    @Test
    @DisplayName("setCompressionEfficiency should throw on non-positive efficiency")
    void setCompressionEfficiencyShouldThrowOnNonPositiveEfficiency() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().setCompressionEfficiency(0.0));
    }

    @Test
    @DisplayName("calculateThroughput should throw on negative data size")
    void calculateThroughputShouldThrowOnNegativeDataSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new SerializationPerformanceMetrics.Builder().calculateThroughput(-1L));
    }
  }

  @Nested
  @DisplayName("Calculated Metrics Tests")
  class CalculatedMetricsTests {

    @Test
    @DisplayName("getTotalOperationTimeNs should return sum of all phases")
    void getTotalOperationTimeNsShouldReturnSumOfAllPhases() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 30_000_000L, 20_000_000L, 10_000_000L)
              .setIoMetrics(0L, 0L, 5_000_000L)
              .build();

      // serialization + compression + hash + disk IO
      final long expected = 100_000_000L + 30_000_000L + 10_000_000L + 5_000_000L;
      assertEquals(expected, metrics.getTotalOperationTimeNs());
    }

    @Test
    @DisplayName("getTotalOperationTimeMs should convert to milliseconds")
    void getTotalOperationTimeMsShouldConvertToMilliseconds() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 0L, 0L, 0L, 0L)
              .build();

      assertEquals(100L, metrics.getTotalOperationTimeMs());
    }

    @Test
    @DisplayName("getDeserializationSpeedRatio should calculate ratio correctly")
    void getDeserializationSpeedRatioShouldCalculateRatioCorrectly() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L)
              .build();

      // 50ms / 100ms = 0.5
      assertEquals(0.5, metrics.getDeserializationSpeedRatio(), 0.001);
    }

    @Test
    @DisplayName("getDeserializationSpeedRatio should return 1.0 when serialization time is zero")
    void getDeserializationSpeedRatioShouldReturnOneWhenSerializationTimeIsZero() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(0L, 50_000_000L, 0L, 0L, 0L)
              .build();

      assertEquals(1.0, metrics.getDeserializationSpeedRatio(), 0.001);
    }

    @Test
    @DisplayName("getMemoryEfficiencyRatio should calculate ratio correctly")
    void getMemoryEfficiencyRatioShouldCalculateRatioCorrectly() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setMemoryMetrics(1000L, 500L, 200L)
              .setIoMetrics(0L, 500L, 0L)
              .build();

      // 500 bytes written / 1000 bytes peak memory = 0.5
      assertEquals(0.5, metrics.getMemoryEfficiencyRatio(), 0.001);
    }

    @Test
    @DisplayName("getMemoryEfficiencyRatio should return 0.0 when peak memory is zero")
    void getMemoryEfficiencyRatioShouldReturnZeroWhenPeakMemoryIsZero() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder().build();

      assertEquals(0.0, metrics.getMemoryEfficiencyRatio(), 0.001);
    }
  }

  @Nested
  @DisplayName("isOptimalPerformance Tests")
  class IsOptimalPerformanceTests {

    @Test
    @DisplayName("isOptimalPerformance should return true for good metrics")
    void isOptimalPerformanceShouldReturnTrueForGoodMetrics() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L) // 100ms
              .setThroughputMetrics(50.0, 100.0, 0.0, 0.0) // 50 MB/s
              .setMemoryMetrics(1024L * 1024L * 10L, 0L, 0L) // 10 MB peak
              .setIoMetrics(0L, 1024L * 1024L * 5L, 0L) // 5 MB written
              .build();

      assertTrue(metrics.isOptimalPerformance());
    }

    @Test
    @DisplayName("isOptimalPerformance should return false for slow serialization")
    void isOptimalPerformanceShouldReturnFalseForSlowSerialization() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(2_000_000_000L, 0L, 0L, 0L, 0L) // 2 seconds (> 1 second)
              .setThroughputMetrics(50.0, 0.0, 0.0, 0.0)
              .setMemoryMetrics(1024L * 1024L, 0L, 0L)
              .setIoMetrics(0L, 512L * 1024L, 0L)
              .build();

      assertFalse(metrics.isOptimalPerformance());
    }

    @Test
    @DisplayName("isOptimalPerformance should return false for low throughput")
    void isOptimalPerformanceShouldReturnFalseForLowThroughput() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 0L, 0L, 0L, 0L)
              .setThroughputMetrics(5.0, 0.0, 0.0, 0.0) // 5 MB/s (< 10 MB/s)
              .setMemoryMetrics(1024L * 1024L, 0L, 0L)
              .setIoMetrics(0L, 512L * 1024L, 0L)
              .build();

      assertFalse(metrics.isOptimalPerformance());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L)
              .setThroughputMetrics(100.0, 200.0, 0.0, 0.0)
              .setMemoryMetrics(1024L * 1024L * 100L, 0L, 0L)
              .setCompressionEfficiency(0.5)
              .build();

      final String result = metrics.toString();

      assertNotNull(result);
      assertTrue(result.contains("SerializationPerformanceMetrics"));
      assertTrue(result.contains("100ms"));
      assertTrue(result.contains("100.0MB/s"));
    }

    @Test
    @DisplayName("getPerformanceSummary should return formatted summary")
    void getPerformanceSummaryShouldReturnFormattedSummary() {
      final SerializationPerformanceMetrics metrics =
          new SerializationPerformanceMetrics.Builder()
              .setTimingMetrics(100_000_000L, 50_000_000L, 0L, 0L, 0L)
              .setThroughputMetrics(100.0, 200.0, 0.0, 0.0)
              .setMemoryMetrics(1024L * 1024L * 100L, 0L, 0L)
              .setCpuMetrics(50.0, 90.0)
              .setCompressionEfficiency(0.5)
              .build();

      final String summary = metrics.getPerformanceSummary();

      assertNotNull(summary);
      assertTrue(summary.contains("Serialization"));
      assertTrue(summary.contains("Memory"));
      assertTrue(summary.contains("CPU"));
      assertTrue(summary.contains("Compression"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("SerializationPerformanceMetrics should be final")
    void serializationPerformanceMetricsShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(SerializationPerformanceMetrics.class.getModifiers()));
    }

    @Test
    @DisplayName("Builder should be final")
    void builderShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              SerializationPerformanceMetrics.Builder.class.getModifiers()));
    }
  }
}
