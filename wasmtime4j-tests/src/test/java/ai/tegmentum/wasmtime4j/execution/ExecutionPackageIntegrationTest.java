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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive functional tests for the execution package classes.
 *
 * <p>Tests cover: ExecutionStatus, FuelExhaustionAction, FuelAdjustment, FuelExhaustionContext,
 * FuelExhaustionResult, FuelCallbackStats, ResourceLimiterConfig, ResourceLimiterStats.
 *
 * @since 1.0.0
 */
@DisplayName("Execution Package Integration Tests")
public final class ExecutionPackageIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ExecutionPackageIntegrationTest.class.getName());

  // ========================================================================
  // ExecutionStatus Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExecutionStatus Enum Tests")
  class ExecutionStatusTests {

    @Test
    @DisplayName("should have all expected status values")
    void shouldHaveAllExpectedStatusValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Set<String> expected =
          Set.of(
              "PENDING",
              "INITIALIZING",
              "RUNNING",
              "SUSPENDED",
              "PAUSED",
              "COMPLETED",
              "FAILED",
              "TERMINATED",
              "CANCELLED",
              "TIMED_OUT",
              "CLEANING_UP",
              "ERROR_RECOVERY",
              "UNKNOWN");

      for (final ExecutionStatus status : ExecutionStatus.values()) {
        assertTrue(expected.contains(status.name()), "Unexpected status: " + status.name());
      }

      assertEquals(expected.size(), ExecutionStatus.values().length, "Should have all statuses");
      LOGGER.info("Found " + ExecutionStatus.values().length + " execution statuses");
    }

    @Test
    @DisplayName("should be able to get status by name")
    void shouldBeAbleToGetStatusByName(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(ExecutionStatus.RUNNING, ExecutionStatus.valueOf("RUNNING"));
      assertEquals(ExecutionStatus.COMPLETED, ExecutionStatus.valueOf("COMPLETED"));
      assertEquals(ExecutionStatus.FAILED, ExecutionStatus.valueOf("FAILED"));

      LOGGER.info("Status by name lookup works correctly");
    }
  }

  // ========================================================================
  // FuelExhaustionAction Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("FuelExhaustionAction Enum Tests")
  class FuelExhaustionActionTests {

    @Test
    @DisplayName("should have correct codes for each action")
    void shouldHaveCorrectCodesForEachAction(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(0, FuelExhaustionAction.CONTINUE.getCode(), "CONTINUE code should be 0");
      assertEquals(1, FuelExhaustionAction.TRAP.getCode(), "TRAP code should be 1");
      assertEquals(2, FuelExhaustionAction.PAUSE.getCode(), "PAUSE code should be 2");

      LOGGER.info("All action codes verified");
    }

    @Test
    @DisplayName("should convert from code correctly")
    void shouldConvertFromCodeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(FuelExhaustionAction.CONTINUE, FuelExhaustionAction.fromCode(0));
      assertEquals(FuelExhaustionAction.TRAP, FuelExhaustionAction.fromCode(1));
      assertEquals(FuelExhaustionAction.PAUSE, FuelExhaustionAction.fromCode(2));

      LOGGER.info("Code to action conversion works correctly");
    }

    @Test
    @DisplayName("should throw exception for invalid code")
    void shouldThrowExceptionForInvalidCode(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> FuelExhaustionAction.fromCode(99),
              "Should throw for invalid code");

      assertTrue(ex.getMessage().contains("99"), "Exception should mention the invalid code");
      LOGGER.info("Invalid code throws: " + ex.getMessage());
    }
  }

  // ========================================================================
  // FuelAdjustment Tests
  // ========================================================================

  @Nested
  @DisplayName("FuelAdjustment Tests")
  class FuelAdjustmentTests {

    @Test
    @DisplayName("should create FuelAdjustment with amount and reason")
    void shouldCreateFuelAdjustmentWithAmountAndReason(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelAdjustment adjustment = new FuelAdjustment(1000L, "Test refuel");

      assertEquals(1000L, adjustment.getAmount(), "Amount should match");
      assertEquals("Test refuel", adjustment.getReason(), "Reason should match");

      LOGGER.info("Created FuelAdjustment: amount=" + adjustment.getAmount());
    }

    @Test
    @DisplayName("should allow negative adjustment (fuel consumption)")
    void shouldAllowNegativeAdjustment(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelAdjustment adjustment = new FuelAdjustment(-500L, "Consumed by operation");

      assertEquals(-500L, adjustment.getAmount(), "Negative amount should be allowed");

      LOGGER.info("Negative adjustment created successfully");
    }

    @Test
    @DisplayName("should allow zero adjustment")
    void shouldAllowZeroAdjustment(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelAdjustment adjustment = new FuelAdjustment(0L, "No change");

      assertEquals(0L, adjustment.getAmount(), "Zero amount should be allowed");

      LOGGER.info("Zero adjustment created successfully");
    }
  }

  // ========================================================================
  // FuelExhaustionContext Tests
  // ========================================================================

  @Nested
  @DisplayName("FuelExhaustionContext Tests")
  class FuelExhaustionContextTests {

    @Test
    @DisplayName("should create context with all properties")
    void shouldCreateContextWithAllProperties(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelExhaustionContext ctx =
          new FuelExhaustionContext(123L, 10000L, 50000L, 5, "compute_heavy");

      assertEquals(123L, ctx.getStoreId(), "Store ID should match");
      assertEquals(10000L, ctx.getFuelConsumed(), "Fuel consumed should match");
      assertEquals(50000L, ctx.getInitialFuel(), "Initial fuel should match");
      assertEquals(5, ctx.getExhaustionCount(), "Exhaustion count should match");
      assertTrue(ctx.getFunctionName().isPresent(), "Function name should be present");
      assertEquals("compute_heavy", ctx.getFunctionName().get(), "Function name should match");

      LOGGER.info("Context created: " + ctx);
    }

    @Test
    @DisplayName("should handle null function name")
    void shouldHandleNullFunctionName(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelExhaustionContext ctx = new FuelExhaustionContext(1L, 100L, 1000L, 1, null);

      assertFalse(ctx.getFunctionName().isPresent(), "Function name should be empty");

      LOGGER.info("Null function name handled correctly");
    }

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelExhaustionContext ctx =
          new FuelExhaustionContext(123L, 10000L, 50000L, 5, "test_func");
      final String str = ctx.toString();

      assertTrue(str.contains("123"), "Should contain store ID");
      assertTrue(str.contains("10000"), "Should contain fuel consumed");
      assertTrue(str.contains("50000"), "Should contain initial fuel");
      assertTrue(str.contains("5"), "Should contain exhaustion count");
      assertTrue(str.contains("test_func"), "Should contain function name");

      LOGGER.info("toString: " + str);
    }
  }

  // ========================================================================
  // FuelExhaustionResult Tests
  // ========================================================================

  @Nested
  @DisplayName("FuelExhaustionResult Tests")
  class FuelExhaustionResultTests {

    @Test
    @DisplayName("should create continue result with fuel")
    void shouldCreateContinueResultWithFuel(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelExhaustionResult result = FuelExhaustionResult.continueWith(5000L);

      assertEquals(FuelExhaustionAction.CONTINUE, result.getAction(), "Action should be CONTINUE");
      assertEquals(5000L, result.getAdditionalFuel(), "Additional fuel should match");

      LOGGER.info("Continue result: " + result);
    }

    @Test
    @DisplayName("should create trap result")
    void shouldCreateTrapResult(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelExhaustionResult result = FuelExhaustionResult.trap();

      assertEquals(FuelExhaustionAction.TRAP, result.getAction(), "Action should be TRAP");
      assertEquals(0L, result.getAdditionalFuel(), "Additional fuel should be 0 for trap");

      LOGGER.info("Trap result: " + result);
    }

    @Test
    @DisplayName("should create pause result")
    void shouldCreatePauseResult(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelExhaustionResult result = FuelExhaustionResult.pause();

      assertEquals(FuelExhaustionAction.PAUSE, result.getAction(), "Action should be PAUSE");
      assertEquals(0L, result.getAdditionalFuel(), "Additional fuel should be 0 for pause");

      LOGGER.info("Pause result: " + result);
    }

    @Test
    @DisplayName("should reject negative fuel")
    void shouldRejectNegativeFuel(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> FuelExhaustionResult.continueWith(-100L),
              "Should throw for negative fuel");

      assertTrue(ex.getMessage().contains("-100"), "Exception should mention invalid value");
      LOGGER.info("Negative fuel rejected: " + ex.getMessage());
    }

    @Test
    @DisplayName("should allow zero fuel for continue")
    void shouldAllowZeroFuelForContinue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelExhaustionResult result = FuelExhaustionResult.continueWith(0L);

      assertEquals(FuelExhaustionAction.CONTINUE, result.getAction());
      assertEquals(0L, result.getAdditionalFuel());

      LOGGER.info("Zero fuel continue allowed");
    }
  }

  // ========================================================================
  // FuelCallbackStats Tests
  // ========================================================================

  @Nested
  @DisplayName("FuelCallbackStats Tests")
  class FuelCallbackStatsTests {

    @Test
    @DisplayName("should create stats with all values")
    void shouldCreateStatsWithAllValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelCallbackStats stats = new FuelCallbackStats(100L, 50000L, 80L, 15L, 5L);

      assertEquals(100L, stats.getExhaustionEvents(), "Exhaustion events should match");
      assertEquals(50000L, stats.getTotalFuelAdded(), "Total fuel added should match");
      assertEquals(80L, stats.getContinuedCount(), "Continued count should match");
      assertEquals(15L, stats.getTrappedCount(), "Trapped count should match");
      assertEquals(5L, stats.getPausedCount(), "Paused count should match");

      LOGGER.info("Stats: " + stats);
    }

    @Test
    @DisplayName("toString should include all metrics")
    void toStringShouldIncludeAllMetrics(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FuelCallbackStats stats = new FuelCallbackStats(10L, 5000L, 8L, 1L, 1L);
      final String str = stats.toString();

      assertTrue(str.contains("exhaustionEvents=10"), "Should contain exhaustion events");
      assertTrue(str.contains("totalFuelAdded=5000"), "Should contain total fuel added");
      assertTrue(str.contains("continuedCount=8"), "Should contain continued count");
      assertTrue(str.contains("trappedCount=1"), "Should contain trapped count");
      assertTrue(str.contains("pausedCount=1"), "Should contain paused count");

      LOGGER.info("toString: " + str);
    }
  }

  // ========================================================================
  // ResourceLimiterConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("ResourceLimiterConfig Tests")
  class ResourceLimiterConfigTests {

    @Test
    @DisplayName("should create default config with no limits")
    void shouldCreateDefaultConfigWithNoLimits(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ResourceLimiterConfig config = ResourceLimiterConfig.defaults();

      assertFalse(config.getMaxMemoryBytes().isPresent(), "No memory bytes limit by default");
      assertFalse(config.getMaxMemoryPages().isPresent(), "No memory pages limit by default");
      assertFalse(config.getMaxTableElements().isPresent(), "No table elements limit by default");
      assertFalse(config.getMaxInstances().isPresent(), "No instances limit by default");
      assertFalse(config.getMaxTables().isPresent(), "No tables limit by default");
      assertFalse(config.getMaxMemories().isPresent(), "No memories limit by default");

      LOGGER.info("Default config has no limits");
    }

    @Test
    @DisplayName("should create config with builder")
    void shouldCreateConfigWithBuilder(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ResourceLimiterConfig config =
          ResourceLimiterConfig.builder()
              .maxMemoryBytes(10 * 1024 * 1024) // 10 MB
              .maxMemoryPages(160) // 10 MB in 64KB pages
              .maxTableElements(1000)
              .maxInstances(5)
              .maxTables(10)
              .maxMemories(2)
              .build();

      assertTrue(config.getMaxMemoryBytes().isPresent(), "Memory bytes limit should be set");
      assertEquals(10 * 1024 * 1024, config.getMaxMemoryBytes().getAsLong());
      assertTrue(config.getMaxMemoryPages().isPresent(), "Memory pages limit should be set");
      assertEquals(160, config.getMaxMemoryPages().getAsLong());
      assertTrue(config.getMaxTableElements().isPresent(), "Table elements limit should be set");
      assertEquals(1000, config.getMaxTableElements().getAsLong());
      assertTrue(config.getMaxInstances().isPresent(), "Instances limit should be set");
      assertEquals(5, config.getMaxInstances().getAsInt());
      assertTrue(config.getMaxTables().isPresent(), "Tables limit should be set");
      assertEquals(10, config.getMaxTables().getAsInt());
      assertTrue(config.getMaxMemories().isPresent(), "Memories limit should be set");
      assertEquals(2, config.getMaxMemories().getAsInt());

      LOGGER.info("Config: " + config);
    }

    @Test
    @DisplayName("should reject negative memory bytes")
    void shouldRejectNegativeMemoryBytes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxMemoryBytes(-1).build(),
              "Should throw for negative memory bytes");

      assertTrue(ex.getMessage().contains("-1"), "Exception should mention invalid value");
      LOGGER.info("Negative memory bytes rejected: " + ex.getMessage());
    }

    @Test
    @DisplayName("should reject negative instances")
    void shouldRejectNegativeInstances(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxInstances(-1).build(),
              "Should throw for negative instances");

      assertTrue(ex.getMessage().contains("-1"), "Exception should mention invalid value");
      LOGGER.info("Negative instances rejected: " + ex.getMessage());
    }

    @Test
    @DisplayName("should allow zero limits")
    void shouldAllowZeroLimits(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxMemoryBytes(0).maxInstances(0).build();

      assertEquals(0, config.getMaxMemoryBytes().getAsLong(), "Zero memory should be allowed");
      assertEquals(0, config.getMaxInstances().getAsInt(), "Zero instances should be allowed");

      LOGGER.info("Zero limits allowed");
    }
  }

  // ========================================================================
  // ResourceLimiterStats Tests
  // ========================================================================

  @Nested
  @DisplayName("ResourceLimiterStats Tests")
  class ResourceLimiterStatsTests {

    @Test
    @DisplayName("should create stats with all values")
    void shouldCreateStatsWithAllValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ResourceLimiterStats stats = new ResourceLimiterStats(1024 * 1024, 500, 100, 5, 50, 2);

      assertEquals(1024 * 1024, stats.getTotalMemoryBytes(), "Total memory should match");
      assertEquals(500, stats.getTotalTableElements(), "Total table elements should match");
      assertEquals(100, stats.getMemoryGrowRequests(), "Memory grow requests should match");
      assertEquals(5, stats.getMemoryGrowDenials(), "Memory grow denials should match");
      assertEquals(50, stats.getTableGrowRequests(), "Table grow requests should match");
      assertEquals(2, stats.getTableGrowDenials(), "Table grow denials should match");

      LOGGER.info("Stats: " + stats);
    }

    @Test
    @DisplayName("should calculate memory denial rate")
    void shouldCalculateMemoryDenialRate(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 100, 25, 0, 0);

      assertEquals(0.25, stats.getMemoryDenialRate(), 0.001, "Memory denial rate should be 25%");

      LOGGER.info("Memory denial rate: " + stats.getMemoryDenialRate());
    }

    @Test
    @DisplayName("should calculate table denial rate")
    void shouldCalculateTableDenialRate(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 50, 10);

      assertEquals(0.20, stats.getTableDenialRate(), 0.001, "Table denial rate should be 20%");

      LOGGER.info("Table denial rate: " + stats.getTableDenialRate());
    }

    @Test
    @DisplayName("should handle zero requests in denial rate")
    void shouldHandleZeroRequestsInDenialRate(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 0, 0);

      assertEquals(0.0, stats.getMemoryDenialRate(), 0.001, "Should be 0 with no requests");
      assertEquals(0.0, stats.getTableDenialRate(), 0.001, "Should be 0 with no requests");

      LOGGER.info("Zero requests handled correctly");
    }
  }
}
