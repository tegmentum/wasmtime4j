package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultWasmExecutionContext}.
 *
 * <p>Validates factory creation, optimization level management, PGO flag, branch hint application,
 * execution statistics, branch prediction statistics, and reset behavior.
 */
@DisplayName("DefaultWasmExecutionContext Tests")
class DefaultWasmExecutionContextTest {

  private static final Logger LOGGER =
      Logger.getLogger(DefaultWasmExecutionContextTest.class.getName());

  @Nested
  @DisplayName("Default Creation Tests")
  class DefaultCreationTests {

    @Test
    @DisplayName("createDefault should return BASIC optimization level")
    void createDefaultShouldReturnBasicOptimizationLevel() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();

      assertEquals(
          WasmExecutionContext.OptimizationLevel.BASIC,
          ctx.getOptimizationLevel(),
          "Default context should use BASIC optimization level");
      LOGGER.info("Default optimization level: " + ctx.getOptimizationLevel());
    }

    @Test
    @DisplayName("createDefault should disable profile-guided optimization")
    void createDefaultShouldDisablePGO() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();

      assertFalse(
          ctx.isProfileGuidedOptimizationEnabled(), "Default context should have PGO disabled");
      LOGGER.info("Default PGO enabled: " + ctx.isProfileGuidedOptimizationEnabled());
    }
  }

  @Nested
  @DisplayName("Custom Creation Tests")
  class CustomCreationTests {

    @Test
    @DisplayName("create with custom optimization level should preserve it")
    void createWithCustomOptimizationLevelShouldPreserveIt() {
      for (final WasmExecutionContext.OptimizationLevel level :
          WasmExecutionContext.OptimizationLevel.values()) {
        final WasmExecutionContext ctx = WasmExecutionContext.create(level, false);

        assertEquals(
            level,
            ctx.getOptimizationLevel(),
            "Context should preserve optimization level " + level);
        LOGGER.info("Created context with level: " + level + " → " + ctx.getOptimizationLevel());
      }
    }

    @Test
    @DisplayName("create with PGO enabled should return true")
    void createWithPgoEnabledShouldReturnTrue() {
      final WasmExecutionContext ctx =
          WasmExecutionContext.create(WasmExecutionContext.OptimizationLevel.AGGRESSIVE, true);

      assertTrue(
          ctx.isProfileGuidedOptimizationEnabled(),
          "Context created with PGO=true should report PGO enabled");
      LOGGER.info("PGO enabled context: " + ctx.isProfileGuidedOptimizationEnabled());
    }
  }

  @Nested
  @DisplayName("Optimization Level Mutation Tests")
  class OptimizationLevelMutationTests {

    @Test
    @DisplayName("setOptimizationLevel should change the level")
    void setOptimizationLevelShouldChangeLevel() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      assertEquals(WasmExecutionContext.OptimizationLevel.BASIC, ctx.getOptimizationLevel());

      ctx.setOptimizationLevel(WasmExecutionContext.OptimizationLevel.MAXIMUM);
      assertEquals(
          WasmExecutionContext.OptimizationLevel.MAXIMUM,
          ctx.getOptimizationLevel(),
          "Optimization level should be MAXIMUM after set");

      ctx.setOptimizationLevel(WasmExecutionContext.OptimizationLevel.NONE);
      assertEquals(
          WasmExecutionContext.OptimizationLevel.NONE,
          ctx.getOptimizationLevel(),
          "Optimization level should be NONE after set");
      LOGGER.info("Optimization level mutation verified");
    }

    @Test
    @DisplayName("OptimizationLevel.isAtLeast should compare correctly")
    void optimizationLevelIsAtLeastShouldCompareCorrectly() {
      assertTrue(
          WasmExecutionContext.OptimizationLevel.MAXIMUM.isAtLeast(
              WasmExecutionContext.OptimizationLevel.NONE),
          "MAXIMUM should be at least NONE");
      assertTrue(
          WasmExecutionContext.OptimizationLevel.MAXIMUM.isAtLeast(
              WasmExecutionContext.OptimizationLevel.MAXIMUM),
          "MAXIMUM should be at least MAXIMUM");
      assertFalse(
          WasmExecutionContext.OptimizationLevel.NONE.isAtLeast(
              WasmExecutionContext.OptimizationLevel.BASIC),
          "NONE should not be at least BASIC");
      assertFalse(
          WasmExecutionContext.OptimizationLevel.BASIC.isAtLeast(
              WasmExecutionContext.OptimizationLevel.AGGRESSIVE),
          "BASIC should not be at least AGGRESSIVE");
      LOGGER.info("OptimizationLevel.isAtLeast comparisons verified");
    }

    @Test
    @DisplayName("OptimizationLevel.getLevel should return correct int value")
    void optimizationLevelGetLevelShouldReturnCorrectValue() {
      assertEquals(0, WasmExecutionContext.OptimizationLevel.NONE.getLevel());
      assertEquals(1, WasmExecutionContext.OptimizationLevel.BASIC.getLevel());
      assertEquals(2, WasmExecutionContext.OptimizationLevel.AGGRESSIVE.getLevel());
      assertEquals(3, WasmExecutionContext.OptimizationLevel.MAXIMUM.getLevel());
      LOGGER.info("OptimizationLevel int values verified");
    }
  }

  @Nested
  @DisplayName("Branch Hint Application Tests")
  class BranchHintApplicationTests {

    @Test
    @DisplayName("applyBranchHint should return true")
    void applyBranchHintShouldReturnTrue() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();

      final boolean result = ctx.applyBranchHint(BranchHintingInstructions.LIKELY_TAKEN, 0x100L);

      assertTrue(result, "applyBranchHint should return true");
      LOGGER.info("Branch hint applied: " + result);
    }

    @Test
    @DisplayName("applyBranchHint with additional data should return true")
    void applyBranchHintWithAdditionalDataShouldReturnTrue() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();

      final boolean result =
          ctx.applyBranchHint(BranchHintingInstructions.LIKELY_TAKEN, 0x200L, "extra1", "extra2");

      assertTrue(result, "applyBranchHint with additional data should return true");
      LOGGER.info("Branch hint with extra data applied: " + result);
    }
  }

  @Nested
  @DisplayName("Execution Statistics Tests")
  class ExecutionStatisticsTests {

    @Test
    @DisplayName("getExecutionStatistics should return non-null with zero counters")
    void getExecutionStatisticsShouldReturnNonNullWithZeroCounters() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      final WasmExecutionContext.ExecutionStatistics stats = ctx.getExecutionStatistics();

      assertNotNull(stats, "ExecutionStatistics should not be null");
      assertEquals(0, stats.getInstructionCount(), "Initial instruction count should be 0");
      assertEquals(0, stats.getFunctionCallCount(), "Initial function call count should be 0");
      assertEquals(0, stats.getBranchCount(), "Initial branch count should be 0");
      assertEquals(
          0, stats.getCorrectBranchPredictions(), "Initial correct predictions should be 0");
      assertEquals(0, stats.getExecutionTimeNanos(), "Initial execution time should be 0");
      LOGGER.info(
          "Initial stats: instructions="
              + stats.getInstructionCount()
              + ", calls="
              + stats.getFunctionCallCount()
              + ", branches="
              + stats.getBranchCount()
              + ", time="
              + stats.getExecutionTimeNanos());
    }

    @Test
    @DisplayName("memory statistics should return zero values")
    void memoryStatisticsShouldReturnZeroValues() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      final WasmExecutionContext.ExecutionStatistics.MemoryStatistics memStats =
          ctx.getExecutionStatistics().getMemoryStatistics();

      assertNotNull(memStats, "MemoryStatistics should not be null");
      assertEquals(0, memStats.getTotalAllocatedBytes(), "Total allocated bytes should be 0");
      assertEquals(0, memStats.getCurrentUsageBytes(), "Current usage bytes should be 0");
      assertEquals(0, memStats.getPeakUsageBytes(), "Peak usage bytes should be 0");
      assertEquals(0, memStats.getGarbageCollections(), "Garbage collections should be 0");
      LOGGER.info(
          "Memory stats: allocated="
              + memStats.getTotalAllocatedBytes()
              + ", current="
              + memStats.getCurrentUsageBytes()
              + ", peak="
              + memStats.getPeakUsageBytes()
              + ", gc="
              + memStats.getGarbageCollections());
    }

    @Test
    @DisplayName("hot spot analysis should return empty arrays")
    void hotSpotAnalysisShouldReturnEmptyArrays() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      final WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis hotSpots =
          ctx.getExecutionStatistics().getHotSpotAnalysis();

      assertNotNull(hotSpots, "HotSpotAnalysis should not be null");
      assertNotNull(hotSpots.getHotFunctions(), "Function hot spots should not be null");
      assertNotNull(hotSpots.getHotBranches(), "Branch hot spots should not be null");
      assertNotNull(hotSpots.getMemoryHotSpots(), "Memory hot spots should not be null");
      assertEquals(0, hotSpots.getHotFunctions().length, "Function hot spots should be empty");
      assertEquals(0, hotSpots.getHotBranches().length, "Branch hot spots should be empty");
      assertEquals(0, hotSpots.getMemoryHotSpots().length, "Memory hot spots should be empty");
      LOGGER.info(
          "HotSpot arrays: functions="
              + hotSpots.getHotFunctions().length
              + ", branches="
              + hotSpots.getHotBranches().length
              + ", memory="
              + hotSpots.getMemoryHotSpots().length);
    }
  }

  @Nested
  @DisplayName("Branch Prediction Statistics Tests")
  class BranchPredictionStatisticsTests {

    @Test
    @DisplayName("initial branch prediction stats should be zero")
    void initialBranchPredictionStatsShouldBeZero() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      final WasmExecutionContext.BranchPredictionStatistics branchStats =
          ctx.getBranchPredictionStatistics();

      assertNotNull(branchStats, "BranchPredictionStatistics should not be null");
      assertEquals(0, branchStats.getTotalPredictions(), "Total predictions should be 0");
      assertEquals(0, branchStats.getCorrectPredictions(), "Correct predictions should be 0");
      LOGGER.info(
          "Branch stats: total="
              + branchStats.getTotalPredictions()
              + ", correct="
              + branchStats.getCorrectPredictions());
    }

    @Test
    @DisplayName("accuracy should be zero when no predictions exist")
    void accuracyShouldBeZeroWhenNoPredictions() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      final WasmExecutionContext.BranchPredictionStatistics branchStats =
          ctx.getBranchPredictionStatistics();

      assertEquals(
          0.0, branchStats.getAccuracy(), 0.001, "Accuracy should be 0.0 with no predictions");
      LOGGER.info("Accuracy with no predictions: " + branchStats.getAccuracy());
    }

    @Test
    @DisplayName("getMispredictionsByType should return defensive copy")
    void getMispredictionsByTypeShouldReturnDefensiveCopy() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      final WasmExecutionContext.BranchPredictionStatistics branchStats =
          ctx.getBranchPredictionStatistics();

      final Map<?, ?> map1 = branchStats.getMispredictionsByType();
      final Map<?, ?> map2 = branchStats.getMispredictionsByType();

      assertNotNull(map1, "Mispredictions map should not be null");
      assertNotNull(map2, "Mispredictions map should not be null on second call");
      assertNotSame(map1, map2, "getMispredictionsByType should return a defensive copy");
      LOGGER.info("Defensive copy verified: map1 != map2");
    }

    @Test
    @DisplayName("getTopMispredictions should return empty array")
    void getTopMispredictionsShouldReturnEmptyArray() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();
      final WasmExecutionContext.BranchPredictionStatistics branchStats =
          ctx.getBranchPredictionStatistics();

      final WasmExecutionContext.BranchPredictionStatistics.MispredictedBranch[] top =
          branchStats.getTopMispredictions();
      assertNotNull(top, "Top mispredictions should not be null");
      assertEquals(0, top.length, "Top mispredictions should be empty initially");
      LOGGER.info("Top mispredictions length: " + top.length);
    }
  }

  @Nested
  @DisplayName("Reset Statistics Tests")
  class ResetStatisticsTests {

    @Test
    @DisplayName("resetStatistics should clear all counters")
    void resetStatisticsShouldClearAllCounters() {
      final WasmExecutionContext ctx = WasmExecutionContext.createDefault();

      // Apply a branch hint to ensure some internal state exists
      ctx.applyBranchHint(BranchHintingInstructions.LIKELY_TAKEN, 0x100L);

      // Reset
      assertDoesNotThrow(ctx::resetStatistics, "resetStatistics should not throw");

      // Verify execution statistics are zero
      final WasmExecutionContext.ExecutionStatistics stats = ctx.getExecutionStatistics();
      assertEquals(0, stats.getInstructionCount(), "Instruction count should be 0 after reset");
      assertEquals(0, stats.getFunctionCallCount(), "Function call count should be 0 after reset");
      assertEquals(0, stats.getBranchCount(), "Branch count should be 0 after reset");

      // Verify branch prediction statistics are zero
      final WasmExecutionContext.BranchPredictionStatistics branchStats =
          ctx.getBranchPredictionStatistics();
      assertEquals(
          0, branchStats.getTotalPredictions(), "Total predictions should be 0 after reset");
      assertEquals(
          0, branchStats.getCorrectPredictions(), "Correct predictions should be 0 after reset");
      assertTrue(
          branchStats.getMispredictionsByType().isEmpty(),
          "Mispredictions map should be empty after reset");

      LOGGER.info("All statistics cleared after resetStatistics()");
    }
  }
}
