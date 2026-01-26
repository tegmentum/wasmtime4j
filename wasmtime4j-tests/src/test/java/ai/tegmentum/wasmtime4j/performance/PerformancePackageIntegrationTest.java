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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive functional tests for the performance package classes.
 *
 * <p>Tests cover: CpuUsage, MemoryUsage, ProfileMetric, ExportFormat and their related
 * functionality.
 *
 * @since 1.0.0
 */
@DisplayName("Performance Package Integration Tests")
public final class PerformancePackageIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PerformancePackageIntegrationTest.class.getName());

  // ========================================================================
  // CpuUsage Tests
  // ========================================================================

  @Nested
  @DisplayName("CpuUsage Tests")
  class CpuUsageTests {

    @Test
    @DisplayName("should create CpuUsage with all metrics")
    void shouldCreateCpuUsageWithAllMetrics(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CpuUsage cpu = new CpuUsage(0.6, 0.2, 0.8, 0.75, 1000, 5000);

      assertEquals(0.6, cpu.getUserCpuTime(), 0.001, "User CPU time should match");
      assertEquals(0.2, cpu.getSystemCpuTime(), 0.001, "System CPU time should match");
      assertEquals(0.8, cpu.getTotalCpuTime(), 0.001, "Total CPU time should match");
      assertEquals(0.75, cpu.getCpuUtilization(), 0.001, "CPU utilization should match");
      assertEquals(1000, cpu.getContextSwitches(), "Context switches should match");
      assertEquals(5000, cpu.getSystemCalls(), "System calls should match");

      LOGGER.info("CpuUsage: " + cpu);
    }

    @Test
    @DisplayName("should clamp values to valid range")
    void shouldClampValuesToValidRange(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Values exceeding 1.0 should be clamped
      final CpuUsage cpu = new CpuUsage(1.5, 1.2, 2.0, 1.1, -100, -200);

      assertEquals(1.0, cpu.getUserCpuTime(), 0.001, "User time should be clamped to 1.0");
      assertEquals(1.0, cpu.getSystemCpuTime(), 0.001, "System time should be clamped to 1.0");
      assertEquals(1.0, cpu.getTotalCpuTime(), 0.001, "Total time should be clamped to 1.0");
      assertEquals(1.0, cpu.getCpuUtilization(), 0.001, "Utilization should be clamped to 1.0");
      assertEquals(0, cpu.getContextSwitches(), "Negative context switches should be 0");
      assertEquals(0, cpu.getSystemCalls(), "Negative system calls should be 0");

      LOGGER.info("Values clamped correctly");
    }

    @Test
    @DisplayName("should detect high CPU usage")
    void shouldDetectHighCpuUsage(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CpuUsage highUsage = new CpuUsage(0.7, 0.2, 0.9, 0.85, 100, 100);
      final CpuUsage normalUsage = new CpuUsage(0.5, 0.1, 0.6, 0.5, 100, 100);

      assertTrue(highUsage.isHighCpuUsage(), "85% should be high CPU usage");
      assertFalse(normalUsage.isHighCpuUsage(), "50% should not be high CPU usage");

      LOGGER.info("High CPU usage detection works correctly");
    }

    @Test
    @DisplayName("should calculate efficiency")
    void shouldCalculateEfficiency(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CpuUsage cpu = new CpuUsage(0.6, 0.2, 0.8, 0.5, 100, 100);

      assertEquals(0.75, cpu.getEfficiency(), 0.001, "Efficiency should be user/total = 0.75");

      LOGGER.info("Efficiency: " + cpu.getEfficiency());
    }

    @Test
    @DisplayName("should detect system call heavy workload")
    void shouldDetectSystemCallHeavyWorkload(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CpuUsage heavy = new CpuUsage(0.4, 0.4, 0.8, 0.5, 100, 100);
      final CpuUsage normal = new CpuUsage(0.7, 0.1, 0.8, 0.5, 100, 100);

      assertTrue(heavy.isSystemCallHeavy(), "50% system time is system call heavy");
      assertFalse(normal.isSystemCallHeavy(), "12.5% system time is not heavy");

      LOGGER.info("System call detection works correctly");
    }

    @Test
    @DisplayName("should calculate performance score")
    void shouldCalculatePerformanceScore(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CpuUsage goodCpu = new CpuUsage(0.6, 0.1, 0.7, 0.5, 100, 100);
      final CpuUsage poorCpu = new CpuUsage(0.3, 0.6, 0.9, 0.95, 50000, 100);

      assertTrue(
          goodCpu.getPerformanceScore() > poorCpu.getPerformanceScore(),
          "Good CPU should have higher score than poor CPU");
      assertTrue(goodCpu.getPerformanceScore() > 0.5, "Good CPU should have good score");

      LOGGER.info(
          "Performance scores: good="
              + goodCpu.getPerformanceScore()
              + ", poor="
              + poorCpu.getPerformanceScore());
    }

    @Test
    @DisplayName("should format percentage correctly")
    void shouldFormatPercentageCorrectly(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals("75.0%", CpuUsage.formatPercentage(0.75));
      assertEquals("100.0%", CpuUsage.formatPercentage(1.0));
      assertEquals("0.0%", CpuUsage.formatPercentage(0.0));

      LOGGER.info("Percentage formatting works correctly");
    }

    @Test
    @DisplayName("should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CpuUsage cpu1 = new CpuUsage(0.6, 0.2, 0.8, 0.75, 1000, 5000);
      final CpuUsage cpu2 = new CpuUsage(0.6, 0.2, 0.8, 0.75, 1000, 5000);
      final CpuUsage cpu3 = new CpuUsage(0.5, 0.2, 0.7, 0.75, 1000, 5000);

      assertEquals(cpu1, cpu2, "Same values should be equal");
      assertEquals(cpu1.hashCode(), cpu2.hashCode(), "Hash codes should match");
      assertFalse(cpu1.equals(cpu3), "Different values should not be equal");

      LOGGER.info("Equals and hashCode work correctly");
    }
  }

  // ========================================================================
  // MemoryUsage Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryUsage Tests")
  class MemoryUsageTests {

    @Test
    @DisplayName("should create MemoryUsage with all metrics")
    void shouldCreateMemoryUsageWithAllMetrics(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final MemoryUsage mem =
          new MemoryUsage(
              500 * 1024 * 1024L, // heapUsed
              700 * 1024 * 1024L, // heapCommitted
              1024 * 1024 * 1024L, // heapMax
              50 * 1024 * 1024L, // nonHeapUsed
              100 * 1024 * 1024L, // nonHeapCommitted
              20 * 1024 * 1024L, // directMemoryUsed
              10 * 1024 * 1024L, // nativeMemoryUsed
              100, // gcCount
              Duration.ofSeconds(5) // gcTime
              );

      assertEquals(500 * 1024 * 1024L, mem.getHeapUsed(), "Heap used should match");
      assertEquals(1024 * 1024 * 1024L, mem.getHeapMax(), "Heap max should match");
      assertEquals(100, mem.getGcCount(), "GC count should match");
      assertEquals(Duration.ofSeconds(5), mem.getGcTime(), "GC time should match");

      LOGGER.info("MemoryUsage: " + mem);
    }

    @Test
    @DisplayName("should calculate heap utilization")
    void shouldCalculateHeapUtilization(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final MemoryUsage mem =
          new MemoryUsage(
              500 * 1024 * 1024L,
              700 * 1024 * 1024L,
              1000 * 1024 * 1024L,
              0,
              0,
              0,
              0,
              0,
              Duration.ZERO);

      assertEquals(0.5, mem.getHeapUtilization(), 0.001, "Utilization should be 50%");

      LOGGER.info("Heap utilization: " + mem.getHeapUtilization());
    }

    @Test
    @DisplayName("should calculate total memory used")
    void shouldCalculateTotalMemoryUsed(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final MemoryUsage mem = new MemoryUsage(100, 200, 1000, 50, 100, 30, 20, 0, Duration.ZERO);

      assertEquals(200, mem.getTotalMemoryUsed(), "Total should be heap+nonHeap+direct+native");

      LOGGER.info("Total memory used: " + mem.getTotalMemoryUsed());
    }

    @Test
    @DisplayName("should detect memory pressure")
    void shouldDetectMemoryPressure(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final MemoryUsage highPressure =
          new MemoryUsage(850, 900, 1000, 0, 0, 0, 0, 0, Duration.ZERO);
      final MemoryUsage lowPressure = new MemoryUsage(500, 700, 1000, 0, 0, 0, 0, 0, Duration.ZERO);

      assertTrue(highPressure.isMemoryPressure(), "85% should be memory pressure");
      assertFalse(lowPressure.isMemoryPressure(), "50% should not be memory pressure");

      LOGGER.info("Memory pressure detection works correctly");
    }

    @Test
    @DisplayName("should detect frequent garbage collection")
    void shouldDetectFrequentGarbageCollection(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final MemoryUsage frequentGc =
          new MemoryUsage(100, 200, 1000, 0, 0, 0, 0, 100, Duration.ofSeconds(60));
      final MemoryUsage rareGc =
          new MemoryUsage(100, 200, 1000, 0, 0, 0, 0, 10, Duration.ofSeconds(5));

      assertTrue(
          frequentGc.isFrequentGarbageCollection(Duration.ofSeconds(1000)),
          "6% GC time should be frequent");
      assertFalse(
          rareGc.isFrequentGarbageCollection(Duration.ofSeconds(1000)),
          "0.5% GC time should not be frequent");

      LOGGER.info("GC frequency detection works correctly");
    }

    @Test
    @DisplayName("should format bytes correctly")
    void shouldFormatBytesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals("500 B", MemoryUsage.formatBytes(500));
      assertEquals("1.0 KB", MemoryUsage.formatBytes(1024));
      assertEquals("1.0 MB", MemoryUsage.formatBytes(1024 * 1024));
      assertEquals("1.0 GB", MemoryUsage.formatBytes(1024 * 1024 * 1024));

      LOGGER.info("Byte formatting works correctly");
    }

    @Test
    @DisplayName("should clamp negative values to zero")
    void shouldClampNegativeValuesToZero(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final MemoryUsage mem =
          new MemoryUsage(-100, -200, -300, -50, -100, -30, -20, -10, Duration.ZERO);

      assertEquals(0, mem.getHeapUsed(), "Negative heap used should be 0");
      assertEquals(0, mem.getNativeMemoryUsed(), "Negative native memory should be 0");
      assertEquals(0, mem.getGcCount(), "Negative GC count should be 0");

      LOGGER.info("Negative values clamped to zero");
    }

    @Test
    @DisplayName("should reject negative GC time")
    void shouldRejectNegativeGcTime(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> new MemoryUsage(100, 200, 1000, 0, 0, 0, 0, 0, Duration.ofSeconds(-5)),
          "Should throw for negative GC time");

      LOGGER.info("Negative GC time rejected");
    }
  }

  // ========================================================================
  // ProfileMetric Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ProfileMetric Enum Tests")
  class ProfileMetricTests {

    @Test
    @DisplayName("should have all expected metrics")
    void shouldHaveAllExpectedMetrics(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Set<String> expected =
          Set.of(
              "CPU_USAGE",
              "MEMORY_USAGE",
              "FUNCTION_CALLS",
              "COMPILATION_TIME",
              "GC_ACTIVITY",
              "I_O_OPERATIONS",
              "THREAD_ACTIVITY",
              "CACHE_PERFORMANCE",
              "JIT_ACTIVITY");

      for (final ProfileMetric metric : ProfileMetric.values()) {
        assertTrue(expected.contains(metric.name()), "Unexpected metric: " + metric.name());
      }

      assertEquals(expected.size(), ProfileMetric.values().length, "Should have all metrics");
      LOGGER.info("Found " + ProfileMetric.values().length + " profile metrics");
    }

    @Test
    @DisplayName("should have display names and descriptions")
    void shouldHaveDisplayNamesAndDescriptions(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      for (final ProfileMetric metric : ProfileMetric.values()) {
        assertNotNull(metric.getDisplayName(), metric.name() + " should have display name");
        assertNotNull(metric.getDescription(), metric.name() + " should have description");
        assertTrue(
            metric.getOverheadFactor() > 0, metric.name() + " should have positive overhead");
      }

      LOGGER.info("All metrics have display names and descriptions");
    }

    @Test
    @DisplayName("should identify low and high overhead metrics")
    void shouldIdentifyLowAndHighOverheadMetrics(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(
          ProfileMetric.CACHE_PERFORMANCE.isLowOverhead(),
          "Cache performance should be low overhead");
      assertTrue(
          ProfileMetric.COMPILATION_TIME.isLowOverhead(),
          "Compilation time should be low overhead");

      assertFalse(
          ProfileMetric.FUNCTION_CALLS.isLowOverhead(),
          "Function calls should not be low overhead");

      LOGGER.info("Overhead classification works correctly");
    }

    @Test
    @DisplayName("should identify resource metrics")
    void shouldIdentifyResourceMetrics(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(ProfileMetric.CPU_USAGE.isResourceMetric(), "CPU_USAGE is resource metric");
      assertTrue(ProfileMetric.MEMORY_USAGE.isResourceMetric(), "MEMORY_USAGE is resource metric");
      assertTrue(
          ProfileMetric.THREAD_ACTIVITY.isResourceMetric(), "THREAD_ACTIVITY is resource metric");

      assertFalse(
          ProfileMetric.COMPILATION_TIME.isResourceMetric(),
          "COMPILATION_TIME is not resource metric");

      LOGGER.info("Resource metric classification works correctly");
    }

    @Test
    @DisplayName("should identify compilation metrics")
    void shouldIdentifyCompilationMetrics(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(
          ProfileMetric.COMPILATION_TIME.isCompilationMetric(),
          "COMPILATION_TIME is compilation metric");
      assertTrue(
          ProfileMetric.JIT_ACTIVITY.isCompilationMetric(), "JIT_ACTIVITY is compilation metric");
      assertTrue(
          ProfileMetric.CACHE_PERFORMANCE.isCompilationMetric(),
          "CACHE_PERFORMANCE is compilation metric");

      assertFalse(
          ProfileMetric.CPU_USAGE.isCompilationMetric(), "CPU_USAGE is not compilation metric");

      LOGGER.info("Compilation metric classification works correctly");
    }

    @Test
    @DisplayName("should identify metrics requiring frequent sampling")
    void shouldIdentifyMetricsRequiringFrequentSampling(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(
          ProfileMetric.FUNCTION_CALLS.requiresFrequentSampling(),
          "FUNCTION_CALLS requires frequent sampling");
      assertTrue(
          ProfileMetric.THREAD_ACTIVITY.requiresFrequentSampling(),
          "THREAD_ACTIVITY requires frequent sampling");

      assertFalse(
          ProfileMetric.COMPILATION_TIME.requiresFrequentSampling(),
          "COMPILATION_TIME doesn't require frequent sampling");

      LOGGER.info("Sampling frequency requirements identified correctly");
    }
  }

  // ========================================================================
  // ExportFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatTests {

    @Test
    @DisplayName("should have all expected formats")
    void shouldHaveAllExpectedFormats(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Set<String> expected =
          Set.of("JSON", "CSV", "BINARY", "JFR", "FLAME_GRAPH", "JMH_JSON");

      for (final ExportFormat format : ExportFormat.values()) {
        assertTrue(expected.contains(format.name()), "Unexpected format: " + format.name());
      }

      assertEquals(expected.size(), ExportFormat.values().length, "Should have all formats");
      LOGGER.info("Found " + ExportFormat.values().length + " export formats");
    }

    @Test
    @DisplayName("should have correct file extensions")
    void shouldHaveCorrectFileExtensions(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals("json", ExportFormat.JSON.getFileExtension());
      assertEquals("csv", ExportFormat.CSV.getFileExtension());
      assertEquals("bin", ExportFormat.BINARY.getFileExtension());
      assertEquals("jfr", ExportFormat.JFR.getFileExtension());
      assertEquals("svg", ExportFormat.FLAME_GRAPH.getFileExtension());

      LOGGER.info("File extensions correct");
    }

    @Test
    @DisplayName("should have correct MIME types")
    void shouldHaveCorrectMimeTypes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals("application/json", ExportFormat.JSON.getMimeType());
      assertEquals("text/csv", ExportFormat.CSV.getMimeType());
      assertEquals("application/octet-stream", ExportFormat.BINARY.getMimeType());
      assertEquals("image/svg+xml", ExportFormat.FLAME_GRAPH.getMimeType());

      LOGGER.info("MIME types correct");
    }

    @Test
    @DisplayName("should identify human readable formats")
    void shouldIdentifyHumanReadableFormats(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(ExportFormat.JSON.isHumanReadable(), "JSON should be human readable");
      assertTrue(ExportFormat.CSV.isHumanReadable(), "CSV should be human readable");
      assertFalse(ExportFormat.BINARY.isHumanReadable(), "BINARY should not be human readable");
      assertFalse(ExportFormat.JFR.isHumanReadable(), "JFR should not be human readable");

      LOGGER.info("Human readability detection works correctly");
    }

    @Test
    @DisplayName("should identify compressed formats")
    void shouldIdentifyCompressedFormats(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(ExportFormat.BINARY.isCompressed(), "BINARY should be compressed");
      assertTrue(ExportFormat.JFR.isCompressed(), "JFR should be compressed");
      assertFalse(ExportFormat.JSON.isCompressed(), "JSON should not be compressed");
      assertFalse(ExportFormat.CSV.isCompressed(), "CSV should not be compressed");

      LOGGER.info("Compression detection works correctly");
    }

    @Test
    @DisplayName("should identify web compatible formats")
    void shouldIdentifyWebCompatibleFormats(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(ExportFormat.JSON.isWebCompatible(), "JSON should be web compatible");
      assertTrue(ExportFormat.CSV.isWebCompatible(), "CSV should be web compatible");
      assertFalse(ExportFormat.BINARY.isWebCompatible(), "BINARY should not be web compatible");

      LOGGER.info("Web compatibility detection works correctly");
    }

    @Test
    @DisplayName("should identify visualization formats")
    void shouldIdentifyVisualizationFormats(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(
          ExportFormat.FLAME_GRAPH.isVisualizationFormat(),
          "FLAME_GRAPH should be visualization format");
      assertTrue(ExportFormat.JFR.isVisualizationFormat(), "JFR should be visualization format");
      assertFalse(
          ExportFormat.JSON.isVisualizationFormat(), "JSON should not be visualization format");

      LOGGER.info("Visualization format detection works correctly");
    }

    @Test
    @DisplayName("should generate correct filename")
    void shouldGenerateCorrectFilename(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals("profile.json", ExportFormat.JSON.getFilename("profile"));
      assertEquals("data.csv", ExportFormat.CSV.getFilename("data"));
      assertEquals("report.svg", ExportFormat.FLAME_GRAPH.getFilename("report"));

      LOGGER.info("Filename generation works correctly");
    }

    @Test
    @DisplayName("should parse format from string")
    void shouldParseFormatFromString(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(ExportFormat.JSON, ExportFormat.fromString("json"));
      assertEquals(ExportFormat.JSON, ExportFormat.fromString("JSON"));
      assertEquals(ExportFormat.CSV, ExportFormat.fromString("csv"));
      assertEquals(ExportFormat.FLAME_GRAPH, ExportFormat.fromString("flame_graph"));
      assertEquals(ExportFormat.FLAME_GRAPH, ExportFormat.fromString("flamegraph"));
      assertEquals(ExportFormat.JMH_JSON, ExportFormat.fromString("jmh"));

      LOGGER.info("String parsing works correctly");
    }

    @Test
    @DisplayName("should throw for invalid format string")
    void shouldThrowForInvalidFormatString(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> ExportFormat.fromString("invalid"),
          "Should throw for invalid format");

      assertThrows(
          IllegalArgumentException.class,
          () -> ExportFormat.fromString(""),
          "Should throw for empty string");

      assertThrows(
          IllegalArgumentException.class,
          () -> ExportFormat.fromString(null),
          "Should throw for null");

      LOGGER.info("Invalid format strings rejected");
    }

    @Test
    @DisplayName("should get best match for requirements")
    void shouldGetBestMatchForRequirements(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(
          ExportFormat.JSON,
          ExportFormat.getBestMatch(true, false, true),
          "Human readable, web compatible should be JSON");

      assertEquals(
          ExportFormat.BINARY,
          ExportFormat.getBestMatch(false, true, false),
          "Compressed, not web should be BINARY");

      assertEquals(
          ExportFormat.CSV,
          ExportFormat.getBestMatch(false, false, true),
          "Web compatible, not human readable should be CSV");

      LOGGER.info("Best match selection works correctly");
    }

    @Test
    @DisplayName("should have reasonable size multipliers")
    void shouldHaveReasonableSizeMultipliers(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(
          ExportFormat.BINARY.getEstimatedSizeMultiplier() < 1.0, "Binary should have size < 1.0");
      assertTrue(
          ExportFormat.JSON.getEstimatedSizeMultiplier() > 1.0, "JSON should have size > 1.0");
      assertTrue(
          ExportFormat.CSV.getEstimatedSizeMultiplier() >= 1.0, "CSV should have size >= 1.0");

      LOGGER.info("Size multipliers are reasonable");
    }
  }
}
