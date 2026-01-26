/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for profiling package classes.
 *
 * <p>This test class validates the AdvancedProfiler, FlameGraphGenerator, and PerformanceInsights.
 */
@DisplayName("Profiling Integration Tests")
public class ProfilingIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ProfilingIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Profiling Integration Tests");
  }

  @Nested
  @DisplayName("ProfilerConfiguration Tests")
  class ProfilerConfigurationTests {

    @Test
    @DisplayName("Should create configuration with default values")
    void shouldCreateConfigurationWithDefaultValues() {
      LOGGER.info("Testing ProfilerConfiguration default values");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      assertNotNull(config, "Configuration should not be null");
      assertEquals(
          Duration.ofMillis(10), config.getSamplingInterval(), "Default sampling interval");
      assertEquals(10000, config.getMaxSamples(), "Default max samples");
      assertFalse(config.isEnableMemoryProfiling(), "Memory profiling disabled by default");
      assertFalse(config.isEnableJfrIntegration(), "JFR integration disabled by default");
      assertFalse(config.isEnableFlameGraphs(), "Flame graphs disabled by default");
      assertFalse(config.isEnableStackTraceCollection(), "Stack trace collection disabled");

      LOGGER.info("Default configuration verified");
    }

    @Test
    @DisplayName("Should create configuration with custom values")
    void shouldCreateConfigurationWithCustomValues() {
      LOGGER.info("Testing ProfilerConfiguration with custom values");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder()
              .samplingInterval(Duration.ofNanos(100_000)) // 100 microseconds
              .maxSamples(5000)
              .enableMemoryProfiling(true)
              .enableJfrIntegration(true)
              .enableFlameGraphs(true)
              .enableStackTraceCollection(true)
              .build();

      assertEquals(Duration.ofNanos(100_000), config.getSamplingInterval());
      assertEquals(5000, config.getMaxSamples());
      assertTrue(config.isEnableMemoryProfiling());
      assertTrue(config.isEnableJfrIntegration());
      assertTrue(config.isEnableFlameGraphs());
      assertTrue(config.isEnableStackTraceCollection());

      LOGGER.info("Custom configuration verified");
    }

    @Test
    @DisplayName("Should reject null sampling interval")
    void shouldRejectNullSamplingInterval() {
      LOGGER.info("Testing rejection of null sampling interval");

      assertThrows(
          NullPointerException.class,
          () -> AdvancedProfiler.ProfilerConfiguration.builder().samplingInterval(null),
          "Should reject null interval");

      LOGGER.info("Null interval rejection verified");
    }

    @Test
    @DisplayName("Should reject non-positive max samples")
    void shouldRejectNonPositiveMaxSamples() {
      LOGGER.info("Testing rejection of non-positive max samples");

      assertThrows(
          IllegalArgumentException.class,
          () -> AdvancedProfiler.ProfilerConfiguration.builder().maxSamples(0),
          "Should reject zero max samples");

      assertThrows(
          IllegalArgumentException.class,
          () -> AdvancedProfiler.ProfilerConfiguration.builder().maxSamples(-1),
          "Should reject negative max samples");

      LOGGER.info("Non-positive max samples rejection verified");
    }
  }

  @Nested
  @DisplayName("AdvancedProfiler Tests")
  class AdvancedProfilerTests {

    @Test
    @DisplayName("Should create profiler with configuration")
    void shouldCreateProfilerWithConfiguration() {
      LOGGER.info("Testing AdvancedProfiler creation");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        assertNotNull(profiler, "Profiler should not be null");
        assertEquals(config, profiler.getConfiguration(), "Configuration should match");
        assertFalse(profiler.isClosed(), "Profiler should not be closed initially");
      }

      LOGGER.info("Profiler creation verified");
    }

    @Test
    @DisplayName("Should reject null configuration")
    void shouldRejectNullConfiguration() {
      LOGGER.info("Testing rejection of null configuration");

      assertThrows(
          NullPointerException.class, () -> new AdvancedProfiler(null), "Should reject null");

      LOGGER.info("Null configuration rejection verified");
    }

    @Test
    @DisplayName("Should start profiling session")
    void shouldStartProfilingSession() {
      LOGGER.info("Testing profiling session start");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        AdvancedProfiler.ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(5));

        assertNotNull(session, "Session should not be null");
        assertNotNull(session.getStartTime(), "Start time should be set");
        assertFalse(session.isClosed(), "Session should not be closed");
        assertFalse(session.isTimedOut(), "Session should not be timed out");

        session.close();
        assertTrue(session.isClosed(), "Session should be closed after close()");
      }

      LOGGER.info("Profiling session verified");
    }

    @Test
    @DisplayName("Should reject null timeout")
    void shouldRejectNullTimeout() {
      LOGGER.info("Testing rejection of null timeout");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        assertThrows(
            IllegalArgumentException.class,
            () -> profiler.startProfiling(null),
            "Should reject null timeout");
      }

      LOGGER.info("Null timeout rejection verified");
    }

    @Test
    @DisplayName("Should reject negative timeout")
    void shouldRejectNegativeTimeout() {
      LOGGER.info("Testing rejection of negative timeout");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        assertThrows(
            IllegalArgumentException.class,
            () -> profiler.startProfiling(Duration.ofSeconds(-1)),
            "Should reject negative timeout");
      }

      LOGGER.info("Negative timeout rejection verified");
    }

    @Test
    @DisplayName("Should reject start on closed profiler")
    void shouldRejectStartOnClosedProfiler() {
      LOGGER.info("Testing rejection on closed profiler");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      AdvancedProfiler profiler = new AdvancedProfiler(config);
      profiler.close();

      assertTrue(profiler.isClosed(), "Profiler should be closed");
      assertThrows(
          IllegalStateException.class,
          () -> profiler.startProfiling(Duration.ofSeconds(1)),
          "Should reject on closed profiler");

      LOGGER.info("Closed profiler rejection verified");
    }

    @Test
    @DisplayName("Should profile operation and track timing")
    void shouldProfileOperationAndTrackTiming() {
      LOGGER.info("Testing operation profiling");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        String result = profiler.profileOperation("test-op", () -> "result", "PANAMA");

        assertEquals("result", result, "Operation result should be returned");
        assertTrue(profiler.getSampleCount() > 0, "Sample count should be positive");
      }

      LOGGER.info("Operation profiling verified");
    }

    @Test
    @DisplayName("Should record function execution")
    void shouldRecordFunctionExecution() {
      LOGGER.info("Testing function execution recording");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        profiler.recordFunctionExecution("myFunction", Duration.ofMillis(10), 1024, "JNI");
        profiler.recordFunctionExecution("otherFunction", Duration.ofMillis(5), 512, "PANAMA");

        assertEquals(2, profiler.getSampleCount(), "Should have 2 samples");
      }

      LOGGER.info("Function execution recording verified");
    }

    @Test
    @DisplayName("Should record memory allocations with memory profiling enabled")
    void shouldRecordMemoryAllocations() {
      LOGGER.info("Testing memory allocation recording");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().enableMemoryProfiling(true).build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        long id1 = profiler.recordMemoryAllocation(1024, "buffer1");
        long id2 = profiler.recordMemoryAllocation(2048, "buffer2");

        assertTrue(id1 > 0, "First allocation ID should be positive");
        assertTrue(id2 > id1, "Second allocation ID should be greater");
        assertEquals(2, profiler.getAllocationCount(), "Should have 2 allocations");

        profiler.recordMemoryDeallocation(id1);
      }

      LOGGER.info("Memory allocation recording verified");
    }

    @Test
    @DisplayName("Should enforce max samples limit")
    void shouldEnforceMaxSamplesLimit() {
      LOGGER.info("Testing max samples enforcement");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().maxSamples(5).build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        for (int i = 0; i < 10; i++) {
          profiler.recordFunctionExecution("func" + i, Duration.ofMillis(1), 0, "JNI");
        }

        List<AdvancedProfiler.FunctionRecord> records = profiler.getFunctionRecordsSnapshot();
        assertEquals(5, records.size(), "Should have exactly max samples");
      }

      LOGGER.info("Max samples enforcement verified");
    }
  }

  @Nested
  @DisplayName("ProfilingSession Tests")
  class ProfilingSessionTests {

    @Test
    @DisplayName("Should get elapsed time")
    void shouldGetElapsedTime() throws InterruptedException {
      LOGGER.info("Testing elapsed time tracking");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config);
          AdvancedProfiler.ProfilingSession session =
              profiler.startProfiling(Duration.ofMinutes(5))) {

        Thread.sleep(50); // Brief delay

        Duration elapsed = session.getElapsedTime();
        assertTrue(elapsed.toMillis() >= 50, "Elapsed time should be at least 50ms");
      }

      LOGGER.info("Elapsed time tracking verified");
    }

    @Test
    @DisplayName("Should detect timeout")
    void shouldDetectTimeout() throws InterruptedException {
      LOGGER.info("Testing timeout detection");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config);
          AdvancedProfiler.ProfilingSession session =
              profiler.startProfiling(Duration.ofMillis(50))) {

        assertFalse(session.isTimedOut(), "Should not be timed out initially");

        Thread.sleep(100); // Wait for timeout

        assertTrue(session.isTimedOut(), "Should be timed out after waiting");
      }

      LOGGER.info("Timeout detection verified");
    }

    @Test
    @DisplayName("Should get statistics")
    void shouldGetStatistics() {
      LOGGER.info("Testing statistics retrieval");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        profiler.recordFunctionExecution("func1", Duration.ofMillis(10), 1024, "JNI");
        profiler.recordFunctionExecution("func1", Duration.ofMillis(20), 2048, "JNI");
        profiler.recordFunctionExecution("func2", Duration.ofMillis(5), 512, "PANAMA");

        try (AdvancedProfiler.ProfilingSession session =
            profiler.startProfiling(Duration.ofMinutes(5))) {

          AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();

          assertNotNull(stats, "Statistics should not be null");
          assertEquals(3, stats.getTotalCalls(), "Should have 3 total calls");
          assertEquals(Duration.ofMillis(35), stats.getTotalTime(), "Total time should be 35ms");
          assertEquals(3584, stats.getTotalMemory(), "Total memory should be 3584");

          Map<String, Long> functionCalls = stats.getFunctionCalls();
          assertEquals(2, functionCalls.get("func1"), "func1 should have 2 calls");
          assertEquals(1, functionCalls.get("func2"), "func2 should have 1 call");
        }
      }

      LOGGER.info("Statistics retrieval verified");
    }

    @Test
    @DisplayName("Should generate flame graph")
    void shouldGenerateFlameGraph() {
      LOGGER.info("Testing flame graph generation");

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();

      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        profiler.recordFunctionExecution("main", Duration.ofMillis(100), 0, "JNI");
        profiler.recordFunctionExecution("processData", Duration.ofMillis(50), 0, "JNI");

        try (AdvancedProfiler.ProfilingSession session =
            profiler.startProfiling(Duration.ofMinutes(5))) {

          FlameGraphGenerator.FlameFrame frame = session.generateFlameGraph();

          assertNotNull(frame, "Flame frame should not be null");
          assertEquals("root", frame.getName(), "Root frame should be named 'root'");
          assertNotNull(frame.getTotalTime(), "Total time should not be null");
          assertFalse(frame.getChildren().isEmpty(), "Should have child frames");
        }
      }

      LOGGER.info("Flame graph generation verified");
    }
  }

  @Nested
  @DisplayName("FlameGraphGenerator Tests")
  class FlameGraphGeneratorTests {

    @Test
    @DisplayName("Should create generator with default max samples")
    void shouldCreateGeneratorWithDefaultMaxSamples() {
      LOGGER.info("Testing FlameGraphGenerator creation");

      FlameGraphGenerator generator = new FlameGraphGenerator();

      assertNotNull(generator, "Generator should not be null");
      assertEquals(0, generator.getSampleCount(), "Initial sample count should be 0");

      LOGGER.info("Generator creation verified");
    }

    @Test
    @DisplayName("Should create generator with custom max samples")
    void shouldCreateGeneratorWithCustomMaxSamples() {
      LOGGER.info("Testing FlameGraphGenerator with custom max samples");

      FlameGraphGenerator generator = new FlameGraphGenerator(1000);

      assertNotNull(generator, "Generator should not be null");

      LOGGER.info("Custom max samples verified");
    }

    @Test
    @DisplayName("Should record samples")
    void shouldRecordSamples() {
      LOGGER.info("Testing sample recording");

      FlameGraphGenerator generator = new FlameGraphGenerator();

      List<String> stack1 = List.of("main", "processData", "computeHash");
      List<String> stack2 = List.of("main", "loadConfig");

      Map<String, String> metadata = new HashMap<>();
      metadata.put("module", "data-processor");

      long id1 = generator.recordSample(Duration.ofMillis(10), stack1, "main-thread", metadata);
      long id2 = generator.recordSample(Duration.ofMillis(5), stack2, "main-thread", null);

      assertTrue(id1 > 0, "First sample ID should be positive");
      assertTrue(id2 > id1, "Second sample ID should be greater");
      assertEquals(2, generator.getSampleCount(), "Should have 2 samples");

      LOGGER.info("Sample recording verified");
    }

    @Test
    @DisplayName("Should reject null duration")
    void shouldRejectNullDuration() {
      LOGGER.info("Testing rejection of null duration");

      FlameGraphGenerator generator = new FlameGraphGenerator();

      assertThrows(
          NullPointerException.class,
          () -> generator.recordSample(null, List.of("main"), "thread", null),
          "Should reject null duration");

      LOGGER.info("Null duration rejection verified");
    }

    @Test
    @DisplayName("Should reject null stack trace")
    void shouldRejectNullStackTrace() {
      LOGGER.info("Testing rejection of null stack trace");

      FlameGraphGenerator generator = new FlameGraphGenerator();

      assertThrows(
          NullPointerException.class,
          () -> generator.recordSample(Duration.ofMillis(10), null, "thread", null),
          "Should reject null stack trace");

      LOGGER.info("Null stack trace rejection verified");
    }

    @Test
    @DisplayName("Should generate flame graph from samples")
    void shouldGenerateFlameGraphFromSamples() {
      LOGGER.info("Testing flame graph generation");

      FlameGraphGenerator generator = new FlameGraphGenerator();

      generator.recordSample(Duration.ofMillis(10), List.of("main", "compute"), "thread", null);
      generator.recordSample(Duration.ofMillis(20), List.of("main", "io"), "thread", null);
      generator.recordSample(Duration.ofMillis(5), List.of("main", "compute"), "thread", null);

      FlameGraphGenerator.FlameFrame root = generator.generateFlameGraph();

      assertNotNull(root, "Root should not be null");
      assertEquals("all", root.getName(), "Root should be named 'all'");
      assertEquals(Duration.ofMillis(35), root.getTotalTime(), "Total time should be 35ms");

      List<FlameGraphGenerator.FlameFrame> children = root.getChildren();
      assertEquals(1, children.size(), "Should have 1 child (main)");
      assertEquals("main", children.get(0).getName(), "Child should be named 'main'");

      LOGGER.info("Flame graph generation verified");
    }

    @Test
    @DisplayName("Should generate SVG from flame graph")
    void shouldGenerateSvgFromFlameGraph() {
      LOGGER.info("Testing SVG generation");

      FlameGraphGenerator generator = new FlameGraphGenerator();

      generator.recordSample(Duration.ofMillis(100), List.of("main", "work"), "thread", null);

      FlameGraphGenerator.FlameFrame root = generator.generateFlameGraph();
      String svg = generator.generateSvg(root);

      assertNotNull(svg, "SVG should not be null");
      assertTrue(svg.contains("<?xml"), "SVG should have XML declaration");
      assertTrue(svg.contains("<svg"), "SVG should have svg element");
      assertTrue(svg.contains("</svg>"), "SVG should have closing svg element");
      assertTrue(svg.contains("rect"), "SVG should have rect elements");

      LOGGER.info("SVG generation verified: " + svg.length() + " characters");
    }

    @Test
    @DisplayName("Should enforce max samples limit")
    void shouldEnforceMaxSamplesLimit() {
      LOGGER.info("Testing max samples enforcement");

      FlameGraphGenerator generator = new FlameGraphGenerator(5);

      for (int i = 0; i < 10; i++) {
        generator.recordSample(Duration.ofMillis(1), List.of("func" + i), "thread", null);
      }

      assertEquals(5, generator.getSampleCount(), "Should have exactly 5 samples");

      LOGGER.info("Max samples enforcement verified");
    }

    @Test
    @DisplayName("Should clear samples")
    void shouldClearSamples() {
      LOGGER.info("Testing sample clearing");

      FlameGraphGenerator generator = new FlameGraphGenerator();

      generator.recordSample(Duration.ofMillis(10), List.of("main"), "thread", null);
      assertEquals(1, generator.getSampleCount(), "Should have 1 sample before clear");

      generator.clear();
      assertEquals(0, generator.getSampleCount(), "Should have 0 samples after clear");

      LOGGER.info("Sample clearing verified");
    }
  }

  @Nested
  @DisplayName("FlameFrame Tests")
  class FlameFrameTests {

    @Test
    @DisplayName("Should create flame frame")
    void shouldCreateFlameFrame() {
      LOGGER.info("Testing FlameFrame creation");

      List<FlameGraphGenerator.FlameFrame> children = new ArrayList<>();
      FlameGraphGenerator.FlameFrame frame =
          new FlameGraphGenerator.FlameFrame("myFunction", Duration.ofMillis(100), children);

      assertEquals("myFunction", frame.getName(), "Name should match");
      assertEquals(Duration.ofMillis(100), frame.getTotalTime(), "Total time should match");
      assertTrue(frame.getChildren().isEmpty(), "Children should be empty");

      LOGGER.info("FlameFrame creation verified");
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() {
      LOGGER.info("Testing rejection of null name");

      assertThrows(
          NullPointerException.class,
          () -> new FlameGraphGenerator.FlameFrame(null, Duration.ofMillis(10), null),
          "Should reject null name");

      LOGGER.info("Null name rejection verified");
    }

    @Test
    @DisplayName("Should reject null total time")
    void shouldRejectNullTotalTime() {
      LOGGER.info("Testing rejection of null total time");

      assertThrows(
          NullPointerException.class,
          () -> new FlameGraphGenerator.FlameFrame("frame", null, null),
          "Should reject null total time");

      LOGGER.info("Null total time rejection verified");
    }

    @Test
    @DisplayName("Should return immutable children list")
    void shouldReturnImmutableChildrenList() {
      LOGGER.info("Testing immutable children list");

      List<FlameGraphGenerator.FlameFrame> children = new ArrayList<>();
      FlameGraphGenerator.FlameFrame frame =
          new FlameGraphGenerator.FlameFrame("parent", Duration.ofMillis(100), children);

      List<FlameGraphGenerator.FlameFrame> returned = frame.getChildren();

      assertThrows(
          UnsupportedOperationException.class,
          () ->
              returned.add(
                  new FlameGraphGenerator.FlameFrame("child", Duration.ofMillis(10), null)),
          "Returned list should be immutable");

      LOGGER.info("Immutable children list verified");
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      LOGGER.info("Testing FlameFrame toString");

      FlameGraphGenerator.FlameFrame frame =
          new FlameGraphGenerator.FlameFrame("testFunc", Duration.ofMillis(50), null);

      String str = frame.toString();
      assertTrue(str.contains("testFunc"), "toString should contain name");
      assertTrue(str.contains("FlameFrame"), "toString should contain class name");

      LOGGER.info("toString verified: " + str);
    }
  }

  @Nested
  @DisplayName("PerformanceInsights Tests")
  class PerformanceInsightsTests {

    @Test
    @DisplayName("Should create PerformanceInsights")
    void shouldCreatePerformanceInsights() {
      LOGGER.info("Testing PerformanceInsights creation");

      PerformanceInsights insights = new PerformanceInsights();
      assertNotNull(insights, "Insights should not be null");

      LOGGER.info("PerformanceInsights creation verified");
    }

    @Test
    @DisplayName("Should analyze performance data")
    void shouldAnalyzePerformanceData() {
      LOGGER.info("Testing performance analysis");

      PerformanceInsights insights = new PerformanceInsights();

      // Create flame graph
      FlameGraphGenerator generator = new FlameGraphGenerator();
      generator.recordSample(Duration.ofMillis(100), List.of("main", "heavyWork"), "thread", null);
      generator.recordSample(Duration.ofMillis(50), List.of("main", "lightWork"), "thread", null);
      FlameGraphGenerator.FlameFrame flameGraph = generator.generateFlameGraph();

      // Create statistics
      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();
      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        profiler.recordFunctionExecution("heavyWork", Duration.ofMillis(100), 0, "JNI");
        profiler.recordFunctionExecution("lightWork", Duration.ofMillis(50), 0, "JNI");

        try (AdvancedProfiler.ProfilingSession session =
            profiler.startProfiling(Duration.ofMinutes(1))) {

          AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();

          PerformanceInsights.PerformanceInsightsResult result =
              insights.analyzePerformance(flameGraph, stats);

          assertNotNull(result, "Result should not be null");
          assertNotNull(result.getHotSpots(), "Hot spots should not be null");
          assertNotNull(result.getRecommendations(), "Recommendations should not be null");
          assertNotNull(result.getSummary(), "Summary should not be null");
        }
      }

      LOGGER.info("Performance analysis verified");
    }

    @Test
    @DisplayName("Should reject null flame graph")
    void shouldRejectNullFlameGraph() {
      LOGGER.info("Testing rejection of null flame graph");

      PerformanceInsights insights = new PerformanceInsights();

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();
      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        try (AdvancedProfiler.ProfilingSession session =
            profiler.startProfiling(Duration.ofMinutes(1))) {

          AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();

          assertThrows(
              NullPointerException.class,
              () -> insights.analyzePerformance(null, stats),
              "Should reject null flame graph");
        }
      }

      LOGGER.info("Null flame graph rejection verified");
    }

    @Test
    @DisplayName("Should reject null statistics")
    void shouldRejectNullStatistics() {
      LOGGER.info("Testing rejection of null statistics");

      PerformanceInsights insights = new PerformanceInsights();

      FlameGraphGenerator.FlameFrame frame =
          new FlameGraphGenerator.FlameFrame("root", Duration.ofMillis(100), null);

      assertThrows(
          NullPointerException.class,
          () -> insights.analyzePerformance(frame, null),
          "Should reject null statistics");

      LOGGER.info("Null statistics rejection verified");
    }
  }

  @Nested
  @DisplayName("HotSpotType Tests")
  class HotSpotTypeTests {

    @Test
    @DisplayName("Should have all expected hot spot types")
    void shouldHaveAllExpectedHotSpotTypes() {
      LOGGER.info("Testing HotSpotType enum values");

      PerformanceInsights.HotSpotType[] types = PerformanceInsights.HotSpotType.values();

      assertTrue(types.length >= 4, "Should have at least 4 hot spot types");
      assertNotNull(PerformanceInsights.HotSpotType.HIGH_CPU_TIME, "HIGH_CPU_TIME should exist");
      assertNotNull(
          PerformanceInsights.HotSpotType.HIGH_CALL_COUNT, "HIGH_CALL_COUNT should exist");
      assertNotNull(PerformanceInsights.HotSpotType.HIGH_MEMORY, "HIGH_MEMORY should exist");
      assertNotNull(PerformanceInsights.HotSpotType.DEEP_STACK, "DEEP_STACK should exist");

      LOGGER.info("HotSpotType enum verified: " + types.length + " types");
    }

    @Test
    @DisplayName("Should support valueOf")
    void shouldSupportValueOf() {
      LOGGER.info("Testing HotSpotType valueOf");

      assertEquals(
          PerformanceInsights.HotSpotType.HIGH_CPU_TIME,
          PerformanceInsights.HotSpotType.valueOf("HIGH_CPU_TIME"));
      assertEquals(
          PerformanceInsights.HotSpotType.HIGH_CALL_COUNT,
          PerformanceInsights.HotSpotType.valueOf("HIGH_CALL_COUNT"));

      LOGGER.info("valueOf verified");
    }
  }

  @Nested
  @DisplayName("PerformanceSummary Tests")
  class PerformanceSummaryTests {

    @Test
    @DisplayName("Should get summary with all fields")
    void shouldGetSummaryWithAllFields() {
      LOGGER.info("Testing PerformanceSummary fields");

      PerformanceInsights insights = new PerformanceInsights();

      FlameGraphGenerator generator = new FlameGraphGenerator();
      generator.recordSample(Duration.ofMillis(100), List.of("main"), "thread", null);
      FlameGraphGenerator.FlameFrame flameGraph = generator.generateFlameGraph();

      AdvancedProfiler.ProfilerConfiguration config =
          AdvancedProfiler.ProfilerConfiguration.builder().build();
      try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
        profiler.recordFunctionExecution("main", Duration.ofMillis(100), 1024, "JNI");

        try (AdvancedProfiler.ProfilingSession session =
            profiler.startProfiling(Duration.ofMinutes(1))) {

          AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();

          PerformanceInsights.PerformanceInsightsResult result =
              insights.analyzePerformance(flameGraph, stats);

          PerformanceInsights.PerformanceSummary summary = result.getSummary();

          assertNotNull(summary, "Summary should not be null");
          assertEquals(1, summary.getTotalCalls(), "Total calls should be 1");
          assertEquals(
              Duration.ofMillis(100), summary.getTotalTime(), "Total time should be 100ms");
          assertEquals(1024, summary.getTotalMemory(), "Total memory should be 1024");
          assertTrue(summary.getAvgCallTimeNanos() > 0, "Avg call time should be positive");
          assertTrue(summary.getHotSpotCount() >= 0, "Hot spot count should be non-negative");
          assertTrue(
              summary.getHotSpotCoveragePercent() >= 0, "Hot spot coverage should be non-negative");

          String str = summary.toString();
          assertTrue(str.contains("PerformanceSummary"), "toString should contain class name");
        }
      }

      LOGGER.info("PerformanceSummary fields verified");
    }
  }
}
