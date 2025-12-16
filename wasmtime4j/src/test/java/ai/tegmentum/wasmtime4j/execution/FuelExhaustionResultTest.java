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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for FuelExhaustionResult class.
 *
 * <p>Verifies the static factory methods and getter methods for fuel exhaustion results.
 */
@DisplayName("FuelExhaustionResult Tests")
class FuelExhaustionResultTest {

  @Nested
  @DisplayName("ContinueWith Factory Tests")
  class ContinueWithFactoryTests {

    @Test
    @DisplayName("should create continue result with additional fuel")
    void shouldCreateContinueResultWithAdditionalFuel() {
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(10000L);

      assertNotNull(result, "Result should not be null");
      assertEquals(FuelExhaustionAction.CONTINUE, result.getAction(), "Action should be CONTINUE");
      assertEquals(10000L, result.getAdditionalFuel(), "Additional fuel should be 10000");
    }

    @Test
    @DisplayName("should accept zero additional fuel")
    void shouldAcceptZeroAdditionalFuel() {
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(0L);

      assertEquals(FuelExhaustionAction.CONTINUE, result.getAction(), "Action should be CONTINUE");
      assertEquals(0L, result.getAdditionalFuel(), "Additional fuel should be 0");
    }

    @Test
    @DisplayName("should reject negative additional fuel")
    void shouldRejectNegativeAdditionalFuel() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> FuelExhaustionResult.continueWith(-1L),
              "Should throw IllegalArgumentException for negative fuel");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should accept large additional fuel value")
    void shouldAcceptLargeAdditionalFuelValue() {
      long largeFuel = Long.MAX_VALUE;
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(largeFuel);

      assertEquals(FuelExhaustionAction.CONTINUE, result.getAction(), "Action should be CONTINUE");
      assertEquals(largeFuel, result.getAdditionalFuel(), "Additional fuel should be MAX_VALUE");
    }

    @Test
    @DisplayName("should accept typical refuel amount")
    void shouldAcceptTypicalRefuelAmount() {
      long typicalRefuel = 1_000_000L; // 1 million fuel units
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(typicalRefuel);

      assertEquals(FuelExhaustionAction.CONTINUE, result.getAction(), "Action should be CONTINUE");
      assertEquals(typicalRefuel, result.getAdditionalFuel(), "Additional fuel should match");
    }
  }

  @Nested
  @DisplayName("Trap Factory Tests")
  class TrapFactoryTests {

    @Test
    @DisplayName("should create trap result")
    void shouldCreateTrapResult() {
      FuelExhaustionResult result = FuelExhaustionResult.trap();

      assertNotNull(result, "Result should not be null");
      assertEquals(FuelExhaustionAction.TRAP, result.getAction(), "Action should be TRAP");
    }

    @Test
    @DisplayName("should have zero additional fuel for trap")
    void shouldHaveZeroAdditionalFuelForTrap() {
      FuelExhaustionResult result = FuelExhaustionResult.trap();

      assertEquals(0L, result.getAdditionalFuel(), "Trap should have 0 additional fuel");
    }

    @Test
    @DisplayName("should create consistent trap results")
    void shouldCreateConsistentTrapResults() {
      FuelExhaustionResult result1 = FuelExhaustionResult.trap();
      FuelExhaustionResult result2 = FuelExhaustionResult.trap();

      assertEquals(result1.getAction(), result2.getAction(), "Both traps should have same action");
      assertEquals(
          result1.getAdditionalFuel(),
          result2.getAdditionalFuel(),
          "Both traps should have same additional fuel");
    }
  }

  @Nested
  @DisplayName("Pause Factory Tests")
  class PauseFactoryTests {

    @Test
    @DisplayName("should create pause result")
    void shouldCreatePauseResult() {
      FuelExhaustionResult result = FuelExhaustionResult.pause();

      assertNotNull(result, "Result should not be null");
      assertEquals(FuelExhaustionAction.PAUSE, result.getAction(), "Action should be PAUSE");
    }

    @Test
    @DisplayName("should have zero additional fuel for pause")
    void shouldHaveZeroAdditionalFuelForPause() {
      FuelExhaustionResult result = FuelExhaustionResult.pause();

      assertEquals(0L, result.getAdditionalFuel(), "Pause should have 0 additional fuel");
    }

    @Test
    @DisplayName("should create consistent pause results")
    void shouldCreateConsistentPauseResults() {
      FuelExhaustionResult result1 = FuelExhaustionResult.pause();
      FuelExhaustionResult result2 = FuelExhaustionResult.pause();

      assertEquals(result1.getAction(), result2.getAction(), "Both pauses should have same action");
      assertEquals(
          result1.getAdditionalFuel(),
          result2.getAdditionalFuel(),
          "Both pauses should have same additional fuel");
    }
  }

  @Nested
  @DisplayName("GetAction Tests")
  class GetActionTests {

    @Test
    @DisplayName("should return CONTINUE for continueWith")
    void shouldReturnContinueForContinueWith() {
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(100L);

      assertEquals(
          FuelExhaustionAction.CONTINUE, result.getAction(), "getAction should return CONTINUE");
    }

    @Test
    @DisplayName("should return TRAP for trap")
    void shouldReturnTrapForTrap() {
      FuelExhaustionResult result = FuelExhaustionResult.trap();

      assertEquals(FuelExhaustionAction.TRAP, result.getAction(), "getAction should return TRAP");
    }

    @Test
    @DisplayName("should return PAUSE for pause")
    void shouldReturnPauseForPause() {
      FuelExhaustionResult result = FuelExhaustionResult.pause();

      assertEquals(FuelExhaustionAction.PAUSE, result.getAction(), "getAction should return PAUSE");
    }
  }

  @Nested
  @DisplayName("GetAdditionalFuel Tests")
  class GetAdditionalFuelTests {

    @Test
    @DisplayName("should return correct additional fuel for continue")
    void shouldReturnCorrectAdditionalFuelForContinue() {
      long expectedFuel = 50000L;
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(expectedFuel);

      assertEquals(
          expectedFuel, result.getAdditionalFuel(), "getAdditionalFuel should return 50000");
    }

    @Test
    @DisplayName("should return zero for trap")
    void shouldReturnZeroForTrap() {
      FuelExhaustionResult result = FuelExhaustionResult.trap();

      assertEquals(0L, result.getAdditionalFuel(), "getAdditionalFuel should return 0 for trap");
    }

    @Test
    @DisplayName("should return zero for pause")
    void shouldReturnZeroForPause() {
      FuelExhaustionResult result = FuelExhaustionResult.pause();

      assertEquals(0L, result.getAdditionalFuel(), "getAdditionalFuel should return 0 for pause");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString for continue")
    void shouldProduceNonNullToStringForContinue() {
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(100L);

      assertNotNull(result.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should produce non-null toString for trap")
    void shouldProduceNonNullToStringForTrap() {
      FuelExhaustionResult result = FuelExhaustionResult.trap();

      assertNotNull(result.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should produce non-null toString for pause")
    void shouldProduceNonNullToStringForPause() {
      FuelExhaustionResult result = FuelExhaustionResult.pause();

      assertNotNull(result.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(100L);

      assertTrue(
          result.toString().contains("FuelExhaustionResult"), "toString should contain class name");
    }

    @Test
    @DisplayName("should include action in toString")
    void shouldIncludeActionInToString() {
      FuelExhaustionResult continueResult = FuelExhaustionResult.continueWith(100L);
      FuelExhaustionResult trapResult = FuelExhaustionResult.trap();
      FuelExhaustionResult pauseResult = FuelExhaustionResult.pause();

      assertTrue(
          continueResult.toString().contains("CONTINUE"),
          "toString should contain CONTINUE action");
      assertTrue(trapResult.toString().contains("TRAP"), "toString should contain TRAP action");
      assertTrue(pauseResult.toString().contains("PAUSE"), "toString should contain PAUSE action");
    }

    @Test
    @DisplayName("should include additionalFuel in toString")
    void shouldIncludeAdditionalFuelInToString() {
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(12345L);

      String str = result.toString();
      assertTrue(str.contains("additionalFuel"), "toString should contain additionalFuel field");
      assertTrue(str.contains("12345"), "toString should contain fuel value");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle typical refueling scenario")
    void shouldHandleTypicalRefuelingScenario() {
      // Simulate a callback that adds more fuel
      long refuelAmount = 100_000L;
      FuelExhaustionResult result = FuelExhaustionResult.continueWith(refuelAmount);

      assertEquals(FuelExhaustionAction.CONTINUE, result.getAction(), "Should continue execution");
      assertEquals(refuelAmount, result.getAdditionalFuel(), "Should add the specified fuel");
    }

    @Test
    @DisplayName("should handle termination scenario")
    void shouldHandleTerminationScenario() {
      // Simulate a callback that decides to trap
      FuelExhaustionResult result = FuelExhaustionResult.trap();

      assertEquals(FuelExhaustionAction.TRAP, result.getAction(), "Should trap execution");
      assertEquals(0L, result.getAdditionalFuel(), "No fuel needed for trap");
    }

    @Test
    @DisplayName("should handle cooperative scheduling scenario")
    void shouldHandleCooperativeSchedulingScenario() {
      // Simulate a callback that pauses for async handling
      FuelExhaustionResult result = FuelExhaustionResult.pause();

      assertEquals(FuelExhaustionAction.PAUSE, result.getAction(), "Should pause execution");
      assertEquals(0L, result.getAdditionalFuel(), "No fuel needed for pause");
    }

    @Test
    @DisplayName("should support decision logic based on result")
    void shouldSupportDecisionLogicBasedOnResult() {
      FuelExhaustionResult[] results = {
        FuelExhaustionResult.continueWith(1000L),
        FuelExhaustionResult.trap(),
        FuelExhaustionResult.pause()
      };

      for (FuelExhaustionResult result : results) {
        switch (result.getAction()) {
          case CONTINUE:
            assertTrue(result.getAdditionalFuel() >= 0, "CONTINUE should have non-negative fuel");
            break;
          case TRAP:
            assertEquals(0L, result.getAdditionalFuel(), "TRAP should have 0 fuel");
            break;
          case PAUSE:
            assertEquals(0L, result.getAdditionalFuel(), "PAUSE should have 0 fuel");
            break;
          default:
            throw new AssertionError("Unknown action: " + result.getAction());
        }
      }
    }
  }
}
