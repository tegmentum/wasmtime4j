/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.compilation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for compilation package.
 *
 * <p>This test class validates the JIT performance monitoring and tiered compilation components.
 */
@DisplayName("Compilation Integration Tests")
public class CompilationIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(CompilationIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Compilation Integration Tests");
  }

  @Nested
  @DisplayName("JitPerformanceMonitor Interface Tests")
  class JitPerformanceMonitorInterfaceTests {

    @Test
    @DisplayName("Should verify JitPerformanceMonitor interface exists")
    void shouldVerifyJitPerformanceMonitorInterfaceExists() {
      LOGGER.info("Testing JitPerformanceMonitor interface existence");

      assertTrue(
          JitPerformanceMonitor.class.isInterface(),
          "JitPerformanceMonitor should be an interface");

      LOGGER.info("JitPerformanceMonitor interface verified");
    }

    @Test
    @DisplayName("Should have required interface methods")
    void shouldHaveRequiredInterfaceMethods() throws Exception {
      LOGGER.info("Testing JitPerformanceMonitor interface methods");

      Method startMonitoring = JitPerformanceMonitor.class.getMethod("startMonitoring");
      assertNotNull(startMonitoring, "startMonitoring method should exist");

      Method stopMonitoring = JitPerformanceMonitor.class.getMethod("stopMonitoring");
      assertNotNull(stopMonitoring, "stopMonitoring method should exist");

      Method getCompilationStatistics =
          JitPerformanceMonitor.class.getMethod("getCompilationStatistics");
      assertNotNull(getCompilationStatistics, "getCompilationStatistics method should exist");

      Method getExecutionStatistics =
          JitPerformanceMonitor.class.getMethod("getExecutionStatistics");
      assertNotNull(getExecutionStatistics, "getExecutionStatistics method should exist");

      Method getPerformanceMetrics = JitPerformanceMonitor.class.getMethod("getPerformanceMetrics");
      assertNotNull(getPerformanceMetrics, "getPerformanceMetrics method should exist");

      Method resetStatistics = JitPerformanceMonitor.class.getMethod("resetStatistics");
      assertNotNull(resetStatistics, "resetStatistics method should exist");

      Method isMonitoring = JitPerformanceMonitor.class.getMethod("isMonitoring");
      assertNotNull(isMonitoring, "isMonitoring method should exist");

      LOGGER.info("JitPerformanceMonitor interface methods verified");
    }
  }

  @Nested
  @DisplayName("CompilationStatistics Interface Tests")
  class CompilationStatisticsInterfaceTests {

    @Test
    @DisplayName("Should verify CompilationStatistics nested interface exists")
    void shouldVerifyCompilationStatisticsNestedInterfaceExists() {
      LOGGER.info("Testing CompilationStatistics nested interface existence");

      assertTrue(
          JitPerformanceMonitor.CompilationStatistics.class.isInterface(),
          "CompilationStatistics should be an interface");

      LOGGER.info("CompilationStatistics nested interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing CompilationStatistics interface methods");

      Method getTotalCompilationTime =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getTotalCompilationTime");
      assertNotNull(getTotalCompilationTime, "getTotalCompilationTime method should exist");

      Method getFunctionsCompiled =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getFunctionsCompiled");
      assertNotNull(getFunctionsCompiled, "getFunctionsCompiled method should exist");

      Method getAverageCompilationTime =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getAverageCompilationTime");
      assertNotNull(getAverageCompilationTime, "getAverageCompilationTime method should exist");

      Method getTierStatistics =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getTierStatistics");
      assertNotNull(getTierStatistics, "getTierStatistics method should exist");

      LOGGER.info("CompilationStatistics interface methods verified");
    }
  }

  @Nested
  @DisplayName("ExecutionStatistics Interface Tests")
  class ExecutionStatisticsInterfaceTests {

    @Test
    @DisplayName("Should verify ExecutionStatistics nested interface exists")
    void shouldVerifyExecutionStatisticsNestedInterfaceExists() {
      LOGGER.info("Testing ExecutionStatistics nested interface existence");

      assertTrue(
          JitPerformanceMonitor.ExecutionStatistics.class.isInterface(),
          "ExecutionStatistics should be an interface");

      LOGGER.info("ExecutionStatistics nested interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing ExecutionStatistics interface methods");

      Method getTotalExecutionTime =
          JitPerformanceMonitor.ExecutionStatistics.class.getMethod("getTotalExecutionTime");
      assertNotNull(getTotalExecutionTime, "getTotalExecutionTime method should exist");

      Method getInstructionCount =
          JitPerformanceMonitor.ExecutionStatistics.class.getMethod("getInstructionCount");
      assertNotNull(getInstructionCount, "getInstructionCount method should exist");

      Method getInstructionsPerSecond =
          JitPerformanceMonitor.ExecutionStatistics.class.getMethod("getInstructionsPerSecond");
      assertNotNull(getInstructionsPerSecond, "getInstructionsPerSecond method should exist");

      LOGGER.info("ExecutionStatistics interface methods verified");
    }
  }

  @Nested
  @DisplayName("PerformanceMetrics Interface Tests")
  class PerformanceMetricsInterfaceTests {

    @Test
    @DisplayName("Should verify PerformanceMetrics nested interface exists")
    void shouldVerifyPerformanceMetricsNestedInterfaceExists() {
      LOGGER.info("Testing PerformanceMetrics nested interface existence");

      assertTrue(
          JitPerformanceMonitor.PerformanceMetrics.class.isInterface(),
          "PerformanceMetrics should be an interface");

      LOGGER.info("PerformanceMetrics nested interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing PerformanceMetrics interface methods");

      Method getThroughput =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getThroughput");
      assertNotNull(getThroughput, "getThroughput method should exist");

      Method getAverageLatency =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getAverageLatency");
      assertNotNull(getAverageLatency, "getAverageLatency method should exist");

      Method getP95Latency =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getP95Latency");
      assertNotNull(getP95Latency, "getP95Latency method should exist");

      Method getCacheHitRatio =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getCacheHitRatio");
      assertNotNull(getCacheHitRatio, "getCacheHitRatio method should exist");

      LOGGER.info("PerformanceMetrics interface methods verified");
    }
  }

  @Nested
  @DisplayName("TieredCompilationConfig Interface Tests")
  class TieredCompilationConfigInterfaceTests {

    @Test
    @DisplayName("Should verify TieredCompilationConfig interface exists")
    void shouldVerifyTieredCompilationConfigInterfaceExists() {
      LOGGER.info("Testing TieredCompilationConfig interface existence");

      assertTrue(
          TieredCompilationConfig.class.isInterface(),
          "TieredCompilationConfig should be an interface");

      LOGGER.info("TieredCompilationConfig interface verified");
    }

    @Test
    @DisplayName("Should have required interface methods")
    void shouldHaveRequiredInterfaceMethods() throws Exception {
      LOGGER.info("Testing TieredCompilationConfig interface methods");

      Method isEnabled = TieredCompilationConfig.class.getMethod("isEnabled");
      assertNotNull(isEnabled, "isEnabled method should exist");

      Method setEnabled = TieredCompilationConfig.class.getMethod("setEnabled", boolean.class);
      assertNotNull(setEnabled, "setEnabled method should exist");

      Method getBaselineTier = TieredCompilationConfig.class.getMethod("getBaselineTier");
      assertNotNull(getBaselineTier, "getBaselineTier method should exist");

      Method getOptimizedTier = TieredCompilationConfig.class.getMethod("getOptimizedTier");
      assertNotNull(getOptimizedTier, "getOptimizedTier method should exist");

      Method getTransitionThreshold =
          TieredCompilationConfig.class.getMethod("getTransitionThreshold");
      assertNotNull(getTransitionThreshold, "getTransitionThreshold method should exist");

      Method setTransitionThreshold =
          TieredCompilationConfig.class.getMethod("setTransitionThreshold", int.class);
      assertNotNull(setTransitionThreshold, "setTransitionThreshold method should exist");

      Method getCompilationTimeout =
          TieredCompilationConfig.class.getMethod("getCompilationTimeout");
      assertNotNull(getCompilationTimeout, "getCompilationTimeout method should exist");

      Method setCompilationTimeout =
          TieredCompilationConfig.class.getMethod("setCompilationTimeout", long.class);
      assertNotNull(setCompilationTimeout, "setCompilationTimeout method should exist");

      LOGGER.info("TieredCompilationConfig interface methods verified");
    }
  }

  @Nested
  @DisplayName("TierConfig Interface Tests")
  class TierConfigInterfaceTests {

    @Test
    @DisplayName("Should verify TierConfig nested interface exists")
    void shouldVerifyTierConfigNestedInterfaceExists() {
      LOGGER.info("Testing TierConfig nested interface existence");

      assertTrue(
          TieredCompilationConfig.TierConfig.class.isInterface(),
          "TierConfig should be an interface");

      LOGGER.info("TierConfig nested interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing TierConfig interface methods");

      Method getTierName = TieredCompilationConfig.TierConfig.class.getMethod("getTierName");
      assertNotNull(getTierName, "getTierName method should exist");

      Method getOptimizationLevel =
          TieredCompilationConfig.TierConfig.class.getMethod("getOptimizationLevel");
      assertNotNull(getOptimizationLevel, "getOptimizationLevel method should exist");

      Method isInliningEnabled =
          TieredCompilationConfig.TierConfig.class.getMethod("isInliningEnabled");
      assertNotNull(isInliningEnabled, "isInliningEnabled method should exist");

      Method isVectorizationEnabled =
          TieredCompilationConfig.TierConfig.class.getMethod("isVectorizationEnabled");
      assertNotNull(isVectorizationEnabled, "isVectorizationEnabled method should exist");

      Method getStrategy = TieredCompilationConfig.TierConfig.class.getMethod("getStrategy");
      assertNotNull(getStrategy, "getStrategy method should exist");

      LOGGER.info("TierConfig interface methods verified");
    }
  }

  @Nested
  @DisplayName("CompilationStrategy Enum Tests")
  class CompilationStrategyEnumTests {

    @Test
    @DisplayName("Should have all expected compilation strategies")
    void shouldHaveAllExpectedCompilationStrategies() {
      LOGGER.info("Testing CompilationStrategy enum values");

      TieredCompilationConfig.CompilationStrategy[] strategies =
          TieredCompilationConfig.CompilationStrategy.values();
      assertEquals(3, strategies.length, "Should have 3 compilation strategies");

      assertNotNull(TieredCompilationConfig.CompilationStrategy.FAST, "FAST should exist");
      assertNotNull(TieredCompilationConfig.CompilationStrategy.BALANCED, "BALANCED should exist");
      assertNotNull(
          TieredCompilationConfig.CompilationStrategy.OPTIMIZED, "OPTIMIZED should exist");

      LOGGER.info("CompilationStrategy enum values verified: " + strategies.length);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      LOGGER.info("Testing CompilationStrategy ordinal values");

      assertEquals(
          0,
          TieredCompilationConfig.CompilationStrategy.FAST.ordinal(),
          "FAST should have ordinal 0");
      assertEquals(
          1,
          TieredCompilationConfig.CompilationStrategy.BALANCED.ordinal(),
          "BALANCED should have ordinal 1");
      assertEquals(
          2,
          TieredCompilationConfig.CompilationStrategy.OPTIMIZED.ordinal(),
          "OPTIMIZED should have ordinal 2");

      LOGGER.info("CompilationStrategy ordinal values verified");
    }
  }
}
