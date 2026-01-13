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

package ai.tegmentum.wasmtime4j.profiling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler.ProfilingStatistics;
import ai.tegmentum.wasmtime4j.profiling.FlameGraphGenerator.FlameFrame;
import ai.tegmentum.wasmtime4j.profiling.PerformanceInsights.HotSpot;
import ai.tegmentum.wasmtime4j.profiling.PerformanceInsights.HotSpotType;
import ai.tegmentum.wasmtime4j.profiling.PerformanceInsights.PerformanceInsightsResult;
import ai.tegmentum.wasmtime4j.profiling.PerformanceInsights.PerformanceSummary;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for PerformanceInsights. */
@DisplayName("PerformanceInsights Tests")
class PerformanceInsightsTest {

  private static final Logger LOGGER = Logger.getLogger(PerformanceInsightsTest.class.getName());

  private PerformanceInsights insights;

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up PerformanceInsights test");
    insights = new PerformanceInsights();
  }

  @Nested
  @DisplayName("Basic Analysis Tests")
  class BasicAnalysisTests {

    @Test
    @DisplayName("Should analyze performance with empty data")
    void shouldAnalyzePerformanceWithEmptyData() {
      LOGGER.info("Testing analysis with empty data");

      FlameFrame emptyRoot = new FlameFrame("all", Duration.ZERO, new ArrayList<>());
      ProfilingStatistics emptyStats = createStatistics(0, Duration.ZERO, 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(emptyRoot, emptyStats);

      assertThat(result).isNotNull();
      assertThat(result.getHotSpots()).isEmpty();
      assertThat(result.getRecommendations()).isNotEmpty();
      assertThat(result.getSummary()).isNotNull();
    }

    @Test
    @DisplayName("Should reject null flame graph")
    void shouldRejectNullFlameGraph() {
      LOGGER.info("Testing null flame graph rejection");

      ProfilingStatistics stats =
          createStatistics(10, Duration.ofMillis(100), 1000, new HashMap<>());

      assertThatThrownBy(() -> insights.analyzePerformance(null, stats))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Flame graph cannot be null");
    }

    @Test
    @DisplayName("Should reject null statistics")
    void shouldRejectNullStatistics() {
      LOGGER.info("Testing null statistics rejection");

      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), new ArrayList<>());

      assertThatThrownBy(() -> insights.analyzePerformance(root, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Statistics cannot be null");
    }
  }

  @Nested
  @DisplayName("Hot Spot Detection Tests")
  class HotSpotDetectionTests {

    @Test
    @DisplayName("Should detect hot spots from flame graph")
    void shouldDetectHotSpotsFromFlameGraph() {
      LOGGER.info("Testing hot spot detection from flame graph");

      // Create flame frame with a hot spot (>5% of total time)
      FlameFrame hotChild = new FlameFrame("hotFunction", Duration.ofMillis(60), new ArrayList<>());
      FlameFrame coldChild =
          new FlameFrame("coldFunction", Duration.ofMillis(40), new ArrayList<>());
      List<FlameFrame> children = new ArrayList<>();
      children.add(hotChild);
      children.add(coldChild);
      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), children);

      Map<String, Long> functionCalls = new HashMap<>();
      functionCalls.put("hotFunction", 10L);
      functionCalls.put("coldFunction", 5L);
      ProfilingStatistics stats = createStatistics(15, Duration.ofMillis(100), 1000, functionCalls);

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      assertThat(result.getHotSpots()).isNotEmpty();

      // Should find the hot function
      boolean foundHotFunction =
          result.getHotSpots().stream().anyMatch(hs -> hs.getFunctionName().equals("hotFunction"));
      assertThat(foundHotFunction).isTrue();
    }

    @Test
    @DisplayName("Should detect hot spots from high call counts")
    void shouldDetectHotSpotsFromHighCallCounts() {
      LOGGER.info("Testing hot spot detection from call counts");

      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), new ArrayList<>());

      Map<String, Long> functionCalls = new HashMap<>();
      functionCalls.put("frequentFunction", 100L); // 50% of calls
      functionCalls.put("rareFunction", 100L); // 50% of calls
      ProfilingStatistics stats =
          createStatistics(200, Duration.ofMillis(100), 1000, functionCalls);

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      // Both functions have high call percentages (>5%)
      assertThat(result.getHotSpots()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should limit hot spots to max 10")
    void shouldLimitHotSpotsToMax10() {
      LOGGER.info("Testing hot spot limit");

      List<FlameFrame> children = new ArrayList<>();
      for (int i = 0; i < 15; i++) {
        children.add(new FlameFrame("func" + i, Duration.ofMillis(10), new ArrayList<>()));
      }
      FlameFrame root = new FlameFrame("all", Duration.ofMillis(150), children);

      Map<String, Long> functionCalls = new HashMap<>();
      for (int i = 0; i < 15; i++) {
        functionCalls.put("func" + i, 10L);
      }
      ProfilingStatistics stats =
          createStatistics(150, Duration.ofMillis(150), 1500, functionCalls);

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      assertThat(result.getHotSpots().size()).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should sort hot spots by time percentage descending")
    void shouldSortHotSpotsByTimePercentageDescending() {
      LOGGER.info("Testing hot spot sorting");

      List<FlameFrame> children = new ArrayList<>();
      children.add(new FlameFrame("lowFunc", Duration.ofMillis(10), new ArrayList<>()));
      children.add(new FlameFrame("highFunc", Duration.ofMillis(80), new ArrayList<>()));
      children.add(new FlameFrame("midFunc", Duration.ofMillis(10), new ArrayList<>()));
      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), children);

      ProfilingStatistics stats = createStatistics(3, Duration.ofMillis(100), 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      List<HotSpot> hotSpots = result.getHotSpots();
      if (hotSpots.size() >= 2) {
        for (int i = 0; i < hotSpots.size() - 1; i++) {
          assertThat(hotSpots.get(i).getTimePercentage())
              .isGreaterThanOrEqualTo(hotSpots.get(i + 1).getTimePercentage());
        }
      }
    }
  }

  @Nested
  @DisplayName("Recommendation Generation Tests")
  class RecommendationGenerationTests {

    @Test
    @DisplayName("Should generate critical recommendation for >50% hot spot")
    void shouldGenerateCriticalRecommendationForMajorHotSpot() {
      LOGGER.info("Testing critical recommendation generation");

      FlameFrame hotChild =
          new FlameFrame("criticalFunc", Duration.ofMillis(60), new ArrayList<>());
      List<FlameFrame> children = new ArrayList<>();
      children.add(hotChild);
      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), children);

      ProfilingStatistics stats = createStatistics(10, Duration.ofMillis(100), 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      boolean hasCriticalRecommendation =
          result.getRecommendations().stream()
              .anyMatch(r -> r.contains("Critical") || r.contains("critical"));
      assertThat(hasCriticalRecommendation).isTrue();
    }

    @Test
    @DisplayName("Should generate high impact recommendation for >20% hot spot")
    void shouldGenerateHighImpactRecommendationForSignificantHotSpot() {
      LOGGER.info("Testing high impact recommendation generation");

      FlameFrame hotChild =
          new FlameFrame("significantFunc", Duration.ofMillis(30), new ArrayList<>());
      List<FlameFrame> children = new ArrayList<>();
      children.add(hotChild);
      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), children);

      ProfilingStatistics stats = createStatistics(10, Duration.ofMillis(100), 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      boolean hasHighImpactRecommendation =
          result.getRecommendations().stream()
              .anyMatch(r -> r.contains("High impact") || r.contains("high impact"));
      assertThat(hasHighImpactRecommendation).isTrue();
    }

    @Test
    @DisplayName("Should generate memory recommendation for high memory usage")
    void shouldGenerateMemoryRecommendationForHighMemoryUsage() {
      LOGGER.info("Testing memory recommendation generation");

      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), new ArrayList<>());
      // More than 100MB of memory
      ProfilingStatistics stats =
          createStatistics(10, Duration.ofMillis(100), 200_000_000, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      boolean hasMemoryRecommendation =
          result.getRecommendations().stream().anyMatch(r -> r.toLowerCase().contains("memory"));
      assertThat(hasMemoryRecommendation).isTrue();
    }

    @Test
    @DisplayName("Should provide default recommendation when no issues found")
    void shouldProvideDefaultRecommendationWhenNoIssuesFound() {
      LOGGER.info("Testing default recommendation");

      FlameFrame root = new FlameFrame("all", Duration.ZERO, new ArrayList<>());
      ProfilingStatistics stats = createStatistics(0, Duration.ZERO, 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      assertThat(result.getRecommendations()).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Performance Summary Tests")
  class PerformanceSummaryTests {

    @Test
    @DisplayName("Should generate complete performance summary")
    void shouldGenerateCompletePerformanceSummary() {
      LOGGER.info("Testing performance summary generation");

      FlameFrame child = new FlameFrame("func", Duration.ofMillis(50), new ArrayList<>());
      List<FlameFrame> children = new ArrayList<>();
      children.add(child);
      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), children);

      // Create stats with 10 function calls (the actual recorded count)
      Map<String, Long> functionCalls = new HashMap<>();
      functionCalls.put("func", 10L);
      ProfilingStatistics stats = createStatistics(10, Duration.ofMillis(100), 5000, functionCalls);

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);
      PerformanceSummary summary = result.getSummary();

      assertThat(summary).isNotNull();
      assertThat(summary.getTotalCalls()).isEqualTo(10);
      assertThat(summary.getTotalTime().toMillis()).isGreaterThan(0);
      assertThat(summary.getTotalMemory()).isGreaterThanOrEqualTo(0);
      assertThat(summary.getAvgCallTimeNanos()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate average call time correctly")
    void shouldCalculateAverageCallTimeCorrectly() {
      LOGGER.info("Testing average call time calculation");

      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), new ArrayList<>());
      ProfilingStatistics stats = createStatistics(50, Duration.ofMillis(500), 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);
      PerformanceSummary summary = result.getSummary();

      // 500ms / 50 calls = 10ms = 10,000,000 ns
      assertThat(summary.getAvgCallTimeNanos()).isEqualTo(10_000_000.0);
    }

    @Test
    @DisplayName("Should handle zero calls in summary")
    void shouldHandleZeroCallsInSummary() {
      LOGGER.info("Testing summary with zero calls");

      FlameFrame root = new FlameFrame("all", Duration.ZERO, new ArrayList<>());
      ProfilingStatistics stats = createStatistics(0, Duration.ZERO, 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);
      PerformanceSummary summary = result.getSummary();

      assertThat(summary.getTotalCalls()).isZero();
      assertThat(summary.getAvgCallTimeNanos()).isZero();
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      LOGGER.info("Testing summary toString");

      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), new ArrayList<>());
      ProfilingStatistics stats =
          createStatistics(10, Duration.ofMillis(100), 1000, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);
      String str = result.getSummary().toString();

      assertThat(str).contains("PerformanceSummary");
      assertThat(str).contains("calls=10");
    }
  }

  @Nested
  @DisplayName("HotSpot Tests")
  class HotSpotTests {

    @Test
    @DisplayName("Should have all hot spot types")
    void shouldHaveAllHotSpotTypes() {
      LOGGER.info("Testing hot spot types");

      assertThat(HotSpotType.values())
          .containsExactlyInAnyOrder(
              HotSpotType.HIGH_CPU_TIME,
              HotSpotType.HIGH_CALL_COUNT,
              HotSpotType.HIGH_MEMORY,
              HotSpotType.DEEP_STACK);
    }

    @Test
    @DisplayName("HotSpot should have meaningful toString")
    void hotSpotShouldHaveMeaningfulToString() {
      LOGGER.info("Testing HotSpot toString");

      // Create a flame frame that will produce a hot spot
      FlameFrame child = new FlameFrame("testFunc", Duration.ofMillis(60), new ArrayList<>());
      List<FlameFrame> children = new ArrayList<>();
      children.add(child);
      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), children);

      ProfilingStatistics stats = createStatistics(10, Duration.ofMillis(100), 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);

      if (!result.getHotSpots().isEmpty()) {
        String str = result.getHotSpots().get(0).toString();
        assertThat(str).contains("HotSpot");
        assertThat(str).contains("function=");
      }
    }
  }

  @Nested
  @DisplayName("Result Immutability Tests")
  class ResultImmutabilityTests {

    @Test
    @DisplayName("Should return immutable hot spots list")
    void shouldReturnImmutableHotSpotsList() {
      LOGGER.info("Testing hot spots list immutability");

      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), new ArrayList<>());
      ProfilingStatistics stats = createStatistics(10, Duration.ofMillis(100), 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);
      List<HotSpot> hotSpots = result.getHotSpots();

      assertThatThrownBy(() -> hotSpots.add(null))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should return immutable recommendations list")
    void shouldReturnImmutableRecommendationsList() {
      LOGGER.info("Testing recommendations list immutability");

      FlameFrame root = new FlameFrame("all", Duration.ofMillis(100), new ArrayList<>());
      ProfilingStatistics stats = createStatistics(10, Duration.ofMillis(100), 0, new HashMap<>());

      PerformanceInsightsResult result = insights.analyzePerformance(root, stats);
      List<String> recommendations = result.getRecommendations();

      assertThatThrownBy(() -> recommendations.add("test"))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  // Helper method to create ProfilingStatistics
  private ProfilingStatistics createStatistics(
      final long totalCalls,
      final Duration totalTime,
      final long totalMemory,
      final Map<String, Long> functionCalls) {

    // Use reflection or package-private constructor access
    // For now, we'll use a simple approach with the AdvancedProfiler
    AdvancedProfiler.ProfilerConfiguration config =
        AdvancedProfiler.ProfilerConfiguration.builder().build();
    AdvancedProfiler profiler = new AdvancedProfiler(config);

    try {
      // Record function executions to build statistics
      for (Map.Entry<String, Long> entry : functionCalls.entrySet()) {
        for (long i = 0; i < entry.getValue(); i++) {
          long timePerCall = totalCalls > 0 ? totalTime.toNanos() / totalCalls : 0;
          long memoryPerCall = totalCalls > 0 ? totalMemory / totalCalls : 0;
          profiler.recordFunctionExecution(
              entry.getKey(), Duration.ofNanos(timePerCall), memoryPerCall, "TEST");
        }
      }

      // If no function calls specified but we need some stats
      if (functionCalls.isEmpty() && totalCalls > 0) {
        long timePerCall = totalTime.toNanos() / totalCalls;
        long memoryPerCall = totalMemory / totalCalls;
        for (long i = 0; i < totalCalls; i++) {
          profiler.recordFunctionExecution(
              "default", Duration.ofNanos(timePerCall), memoryPerCall, "TEST");
        }
      }

      AdvancedProfiler.ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(1));
      ProfilingStatistics stats = session.getStatistics();
      session.close();
      return stats;
    } finally {
      profiler.close();
    }
  }
}
