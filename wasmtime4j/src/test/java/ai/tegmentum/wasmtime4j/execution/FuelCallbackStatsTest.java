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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for FuelCallbackStats class.
 *
 * <p>Verifies the constructor and getter methods for fuel callback handler statistics.
 */
@DisplayName("FuelCallbackStats Tests")
class FuelCallbackStatsTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create stats with all zero values")
    void shouldCreateStatsWithAllZeroValues() {
      FuelCallbackStats stats = new FuelCallbackStats(0L, 0L, 0L, 0L, 0L);

      assertNotNull(stats, "Stats should not be null");
      assertEquals(0L, stats.getExhaustionEvents(), "exhaustionEvents should be 0");
      assertEquals(0L, stats.getTotalFuelAdded(), "totalFuelAdded should be 0");
      assertEquals(0L, stats.getContinuedCount(), "continuedCount should be 0");
      assertEquals(0L, stats.getTrappedCount(), "trappedCount should be 0");
      assertEquals(0L, stats.getPausedCount(), "pausedCount should be 0");
    }

    @Test
    @DisplayName("should create stats with typical values")
    void shouldCreateStatsWithTypicalValues() {
      FuelCallbackStats stats =
          new FuelCallbackStats(
              10L, // 10 exhaustion events
              5_000_000L, // 5 million fuel added
              8L, // 8 continued
              1L, // 1 trapped
              1L // 1 paused
              );

      assertEquals(10L, stats.getExhaustionEvents(), "exhaustionEvents should be 10");
      assertEquals(5_000_000L, stats.getTotalFuelAdded(), "totalFuelAdded should be 5M");
      assertEquals(8L, stats.getContinuedCount(), "continuedCount should be 8");
      assertEquals(1L, stats.getTrappedCount(), "trappedCount should be 1");
      assertEquals(1L, stats.getPausedCount(), "pausedCount should be 1");
    }

    @Test
    @DisplayName("should create stats with large values")
    void shouldCreateStatsWithLargeValues() {
      FuelCallbackStats stats =
          new FuelCallbackStats(
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE / 3,
              Long.MAX_VALUE / 3,
              Long.MAX_VALUE / 3);

      assertEquals(Long.MAX_VALUE, stats.getExhaustionEvents(), "exhaustionEvents should be MAX");
      assertEquals(Long.MAX_VALUE, stats.getTotalFuelAdded(), "totalFuelAdded should be MAX");
    }
  }

  @Nested
  @DisplayName("GetExhaustionEvents Tests")
  class GetExhaustionEventsTests {

    @Test
    @DisplayName("should return correct exhaustionEvents")
    void shouldReturnCorrectExhaustionEvents() {
      FuelCallbackStats stats = new FuelCallbackStats(100L, 0L, 0L, 0L, 0L);

      assertEquals(100L, stats.getExhaustionEvents(), "getExhaustionEvents should return 100");
    }

    @Test
    @DisplayName("should handle single exhaustion event")
    void shouldHandleSingleExhaustionEvent() {
      FuelCallbackStats stats = new FuelCallbackStats(1L, 1000L, 1L, 0L, 0L);

      assertEquals(1L, stats.getExhaustionEvents(), "getExhaustionEvents should return 1");
    }
  }

  @Nested
  @DisplayName("GetTotalFuelAdded Tests")
  class GetTotalFuelAddedTests {

    @Test
    @DisplayName("should return correct totalFuelAdded")
    void shouldReturnCorrectTotalFuelAdded() {
      long expectedFuel = 10_000_000L;
      FuelCallbackStats stats = new FuelCallbackStats(5L, expectedFuel, 5L, 0L, 0L);

      assertEquals(expectedFuel, stats.getTotalFuelAdded(), "getTotalFuelAdded should return 10M");
    }

    @Test
    @DisplayName("should handle zero fuel added when all trapped")
    void shouldHandleZeroFuelAddedWhenAllTrapped() {
      FuelCallbackStats stats = new FuelCallbackStats(5L, 0L, 0L, 5L, 0L);

      assertEquals(0L, stats.getTotalFuelAdded(), "getTotalFuelAdded should be 0 when all trapped");
    }
  }

  @Nested
  @DisplayName("GetContinuedCount Tests")
  class GetContinuedCountTests {

    @Test
    @DisplayName("should return correct continuedCount")
    void shouldReturnCorrectContinuedCount() {
      FuelCallbackStats stats = new FuelCallbackStats(10L, 1000L, 7L, 2L, 1L);

      assertEquals(7L, stats.getContinuedCount(), "getContinuedCount should return 7");
    }

    @Test
    @DisplayName("should handle all events continued")
    void shouldHandleAllEventsContinued() {
      FuelCallbackStats stats = new FuelCallbackStats(100L, 100_000L, 100L, 0L, 0L);

      assertEquals(100L, stats.getContinuedCount(), "All 100 events should have continued");
      assertEquals(0L, stats.getTrappedCount(), "No events should be trapped");
      assertEquals(0L, stats.getPausedCount(), "No events should be paused");
    }
  }

  @Nested
  @DisplayName("GetTrappedCount Tests")
  class GetTrappedCountTests {

    @Test
    @DisplayName("should return correct trappedCount")
    void shouldReturnCorrectTrappedCount() {
      FuelCallbackStats stats = new FuelCallbackStats(10L, 5000L, 5L, 5L, 0L);

      assertEquals(5L, stats.getTrappedCount(), "getTrappedCount should return 5");
    }

    @Test
    @DisplayName("should handle all events trapped")
    void shouldHandleAllEventsTrapped() {
      FuelCallbackStats stats = new FuelCallbackStats(50L, 0L, 0L, 50L, 0L);

      assertEquals(0L, stats.getContinuedCount(), "No events should have continued");
      assertEquals(50L, stats.getTrappedCount(), "All 50 events should be trapped");
      assertEquals(0L, stats.getTotalFuelAdded(), "No fuel should be added when all trapped");
    }
  }

  @Nested
  @DisplayName("GetPausedCount Tests")
  class GetPausedCountTests {

    @Test
    @DisplayName("should return correct pausedCount")
    void shouldReturnCorrectPausedCount() {
      FuelCallbackStats stats = new FuelCallbackStats(20L, 10000L, 10L, 5L, 5L);

      assertEquals(5L, stats.getPausedCount(), "getPausedCount should return 5");
    }

    @Test
    @DisplayName("should handle all events paused")
    void shouldHandleAllEventsPaused() {
      FuelCallbackStats stats = new FuelCallbackStats(25L, 0L, 0L, 0L, 25L);

      assertEquals(0L, stats.getContinuedCount(), "No events should have continued");
      assertEquals(0L, stats.getTrappedCount(), "No events should be trapped");
      assertEquals(25L, stats.getPausedCount(), "All 25 events should be paused");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      FuelCallbackStats stats = new FuelCallbackStats(0L, 0L, 0L, 0L, 0L);

      assertNotNull(stats.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      FuelCallbackStats stats = new FuelCallbackStats(0L, 0L, 0L, 0L, 0L);

      assertTrue(
          stats.toString().contains("FuelCallbackStats"), "toString should contain class name");
    }

    @Test
    @DisplayName("should include field names in toString")
    void shouldIncludeFieldNamesInToString() {
      FuelCallbackStats stats = new FuelCallbackStats(1L, 2L, 3L, 4L, 5L);

      String result = stats.toString();
      assertTrue(result.contains("exhaustionEvents"), "toString should contain exhaustionEvents");
      assertTrue(result.contains("totalFuelAdded"), "toString should contain totalFuelAdded");
      assertTrue(result.contains("continuedCount"), "toString should contain continuedCount");
      assertTrue(result.contains("trappedCount"), "toString should contain trappedCount");
      assertTrue(result.contains("pausedCount"), "toString should contain pausedCount");
    }

    @Test
    @DisplayName("should include values in toString")
    void shouldIncludeValuesInToString() {
      FuelCallbackStats stats = new FuelCallbackStats(123L, 456L, 789L, 10L, 11L);

      String result = stats.toString();
      assertTrue(result.contains("123"), "toString should contain exhaustionEvents value");
      assertTrue(result.contains("456"), "toString should contain totalFuelAdded value");
    }
  }

  @Nested
  @DisplayName("Consistency Tests")
  class ConsistencyTests {

    @Test
    @DisplayName("should have event counts sum approximately to exhaustion events")
    void shouldHaveEventCountsSumApproximatelyToExhaustionEvents() {
      // In well-formed stats, continued + trapped + paused should equal exhaustionEvents
      FuelCallbackStats stats = new FuelCallbackStats(10L, 50000L, 7L, 2L, 1L);

      long total = stats.getContinuedCount() + stats.getTrappedCount() + stats.getPausedCount();
      assertEquals(
          stats.getExhaustionEvents(), total, "Sum of outcomes should equal exhaustion events");
    }

    @Test
    @DisplayName("should allow inconsistent counts for flexibility")
    void shouldAllowInconsistentCountsForFlexibility() {
      // The class doesn't enforce consistency - it's just data
      FuelCallbackStats stats = new FuelCallbackStats(5L, 1000L, 10L, 20L, 30L);

      // This is inconsistent (5 events but 60 outcomes) but should still work
      assertEquals(5L, stats.getExhaustionEvents());
      assertEquals(10L, stats.getContinuedCount());
      assertEquals(20L, stats.getTrappedCount());
      assertEquals(30L, stats.getPausedCount());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent long-running module scenario")
    void shouldRepresentLongRunningModuleScenario() {
      // Module that ran for a long time with mostly refueling, some traps
      FuelCallbackStats stats =
          new FuelCallbackStats(
              1000L, // 1000 exhaustion events
              500_000_000L, // 500 million fuel added over lifetime
              950L, // 950 continued (95%)
              40L, // 40 trapped (4%)
              10L // 10 paused (1%)
              );

      assertEquals(1000L, stats.getExhaustionEvents(), "Should have 1000 events");
      assertEquals(500_000_000L, stats.getTotalFuelAdded(), "Should have 500M total fuel");

      // Verify percentages
      double continuedRate = (double) stats.getContinuedCount() / stats.getExhaustionEvents();
      double trappedRate = (double) stats.getTrappedCount() / stats.getExhaustionEvents();
      double pausedRate = (double) stats.getPausedCount() / stats.getExhaustionEvents();

      assertEquals(0.95, continuedRate, 0.01, "Continue rate should be ~95%");
      assertEquals(0.04, trappedRate, 0.01, "Trap rate should be ~4%");
      assertEquals(0.01, pausedRate, 0.01, "Pause rate should be ~1%");
    }

    @Test
    @DisplayName("should represent strict resource limit scenario")
    void shouldRepresentStrictResourceLimitScenario() {
      // Module with strict limits - all exhaustions result in trap
      FuelCallbackStats stats = new FuelCallbackStats(100L, 0L, 0L, 100L, 0L);

      assertEquals(100L, stats.getExhaustionEvents(), "Should have 100 events");
      assertEquals(0L, stats.getTotalFuelAdded(), "No fuel should be added");
      assertEquals(0L, stats.getContinuedCount(), "None should continue");
      assertEquals(100L, stats.getTrappedCount(), "All should trap");
    }

    @Test
    @DisplayName("should represent async cooperative scenario")
    void shouldRepresentAsyncCooperativeScenario() {
      // Async scenario where exhaustions trigger pause for external scheduling
      FuelCallbackStats stats =
          new FuelCallbackStats(
              50L, // 50 exhaustion events
              25_000_000L, // 25 million fuel eventually added
              25L, // 25 continued after async wait
              5L, // 5 trapped due to timeout
              20L // 20 paused awaiting external event
              );

      assertEquals(50L, stats.getExhaustionEvents(), "Should have 50 events");
      assertEquals(20L, stats.getPausedCount(), "20 should be paused");

      // Continued + paused that resumed = most events eventually continue
      long resolvedContinuations = stats.getContinuedCount();
      assertTrue(resolvedContinuations > 0, "Some should have continued after pause");
    }

    @Test
    @DisplayName("should handle first-time stats scenario")
    void shouldHandleFirstTimeStatsScenario() {
      // First exhaustion event
      FuelCallbackStats stats =
          new FuelCallbackStats(
              1L, // First exhaustion
              1_000_000L, // 1 million fuel added
              1L, // Continued
              0L, // Not trapped
              0L // Not paused
              );

      assertEquals(1L, stats.getExhaustionEvents(), "Should have 1 event");
      assertEquals(1_000_000L, stats.getTotalFuelAdded(), "1M fuel added");
      assertEquals(1L, stats.getContinuedCount(), "1 continued");
    }
  }
}
