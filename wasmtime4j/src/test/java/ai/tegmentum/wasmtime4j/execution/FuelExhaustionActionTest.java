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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for FuelExhaustionAction enum.
 *
 * <p>Verifies enum values, native codes, and fromCode conversion.
 */
@DisplayName("FuelExhaustionAction Tests")
class FuelExhaustionActionTest {

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have exactly 3 enum values")
    void shouldHaveExactlyThreeEnumValues() {
      FuelExhaustionAction[] values = FuelExhaustionAction.values();

      assertEquals(3, values.length, "Should have exactly 3 enum values");
    }

    @Test
    @DisplayName("should have CONTINUE value")
    void shouldHaveContinueValue() {
      FuelExhaustionAction action = FuelExhaustionAction.CONTINUE;

      assertNotNull(action, "CONTINUE should not be null");
      assertEquals("CONTINUE", action.name(), "Name should be CONTINUE");
    }

    @Test
    @DisplayName("should have TRAP value")
    void shouldHaveTrapValue() {
      FuelExhaustionAction action = FuelExhaustionAction.TRAP;

      assertNotNull(action, "TRAP should not be null");
      assertEquals("TRAP", action.name(), "Name should be TRAP");
    }

    @Test
    @DisplayName("should have PAUSE value")
    void shouldHavePauseValue() {
      FuelExhaustionAction action = FuelExhaustionAction.PAUSE;

      assertNotNull(action, "PAUSE should not be null");
      assertEquals("PAUSE", action.name(), "Name should be PAUSE");
    }

    @Test
    @DisplayName("should be accessible via valueOf")
    void shouldBeAccessibleViaValueOf() {
      assertEquals(FuelExhaustionAction.CONTINUE, FuelExhaustionAction.valueOf("CONTINUE"));
      assertEquals(FuelExhaustionAction.TRAP, FuelExhaustionAction.valueOf("TRAP"));
      assertEquals(FuelExhaustionAction.PAUSE, FuelExhaustionAction.valueOf("PAUSE"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid valueOf")
    void shouldThrowIllegalArgumentExceptionForInvalidValueOf() {
      assertThrows(
          IllegalArgumentException.class,
          () -> FuelExhaustionAction.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("GetCode Tests")
  class GetCodeTests {

    @Test
    @DisplayName("should return code 0 for CONTINUE")
    void shouldReturnCodeZeroForContinue() {
      assertEquals(0, FuelExhaustionAction.CONTINUE.getCode(), "CONTINUE should have code 0");
    }

    @Test
    @DisplayName("should return code 1 for TRAP")
    void shouldReturnCodeOneForTrap() {
      assertEquals(1, FuelExhaustionAction.TRAP.getCode(), "TRAP should have code 1");
    }

    @Test
    @DisplayName("should return code 2 for PAUSE")
    void shouldReturnCodeTwoForPause() {
      assertEquals(2, FuelExhaustionAction.PAUSE.getCode(), "PAUSE should have code 2");
    }

    @Test
    @DisplayName("should have unique codes for all values")
    void shouldHaveUniqueCodesForAllValues() {
      Set<Integer> codes = new HashSet<>();
      for (FuelExhaustionAction action : FuelExhaustionAction.values()) {
        codes.add(action.getCode());
      }

      assertEquals(
          FuelExhaustionAction.values().length,
          codes.size(),
          "All actions should have unique codes");
    }
  }

  @Nested
  @DisplayName("FromCode Tests")
  class FromCodeTests {

    @Test
    @DisplayName("should return CONTINUE for code 0")
    void shouldReturnContinueForCodeZero() {
      FuelExhaustionAction action = FuelExhaustionAction.fromCode(0);

      assertEquals(FuelExhaustionAction.CONTINUE, action, "Code 0 should return CONTINUE");
    }

    @Test
    @DisplayName("should return TRAP for code 1")
    void shouldReturnTrapForCodeOne() {
      FuelExhaustionAction action = FuelExhaustionAction.fromCode(1);

      assertEquals(FuelExhaustionAction.TRAP, action, "Code 1 should return TRAP");
    }

    @Test
    @DisplayName("should return PAUSE for code 2")
    void shouldReturnPauseForCodeTwo() {
      FuelExhaustionAction action = FuelExhaustionAction.fromCode(2);

      assertEquals(FuelExhaustionAction.PAUSE, action, "Code 2 should return PAUSE");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative code")
    void shouldThrowIllegalArgumentExceptionForNegativeCode() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> FuelExhaustionAction.fromCode(-1),
              "Should throw for negative code");

      assertTrue(
          exception.getMessage().contains("-1") || exception.getMessage().contains("Unknown"),
          "Exception should mention the invalid code: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for code 3")
    void shouldThrowIllegalArgumentExceptionForCodeThree() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> FuelExhaustionAction.fromCode(3),
              "Should throw for code 3");

      assertTrue(
          exception.getMessage().contains("3") || exception.getMessage().contains("Unknown"),
          "Exception should mention the invalid code: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for large code")
    void shouldThrowIllegalArgumentExceptionForLargeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> FuelExhaustionAction.fromCode(100),
          "Should throw for code 100");
    }

    @Test
    @DisplayName("should round-trip all actions through code")
    void shouldRoundTripAllActionsThroughCode() {
      for (FuelExhaustionAction original : FuelExhaustionAction.values()) {
        int code = original.getCode();
        FuelExhaustionAction converted = FuelExhaustionAction.fromCode(code);
        assertEquals(original, converted, "Round-trip should preserve action: " + original);
      }
    }
  }

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      FuelExhaustionAction[] values = FuelExhaustionAction.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should be " + i + " for " + values[i]);
      }
    }

    @Test
    @DisplayName("ordinal should match code for all values")
    void ordinalShouldMatchCodeForAllValues() {
      // In this enum, ordinal == code by design
      for (FuelExhaustionAction action : FuelExhaustionAction.values()) {
        assertEquals(action.ordinal(), action.getCode(), "Ordinal should match code for " + action);
      }
    }
  }

  @Nested
  @DisplayName("Enum Contract Tests")
  class EnumContractTests {

    @Test
    @DisplayName("should be comparable")
    void shouldBeComparable() {
      assertTrue(
          FuelExhaustionAction.CONTINUE.compareTo(FuelExhaustionAction.TRAP) < 0,
          "CONTINUE should be before TRAP");
      assertTrue(
          FuelExhaustionAction.TRAP.compareTo(FuelExhaustionAction.PAUSE) < 0,
          "TRAP should be before PAUSE");
      assertEquals(
          0,
          FuelExhaustionAction.CONTINUE.compareTo(FuelExhaustionAction.CONTINUE),
          "CONTINUE should equal itself");
    }

    @Test
    @DisplayName("should support iteration via values()")
    void shouldSupportIterationViaValues() {
      FuelExhaustionAction[] values = FuelExhaustionAction.values();

      assertEquals(FuelExhaustionAction.CONTINUE, values[0], "First value should be CONTINUE");
      assertEquals(FuelExhaustionAction.TRAP, values[1], "Second value should be TRAP");
      assertEquals(FuelExhaustionAction.PAUSE, values[2], "Third value should be PAUSE");
    }

    @Test
    @DisplayName("should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      for (FuelExhaustionAction action : FuelExhaustionAction.values()) {
        String result;
        switch (action) {
          case CONTINUE:
            result = "continue";
            break;
          case TRAP:
            result = "trap";
            break;
          case PAUSE:
            result = "pause";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList("continue", "trap", "pause").contains(result),
            "Switch should handle all actions");
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support native FFI conversion pattern")
    void shouldSupportNativeFfiConversionPattern() {
      // Simulate receiving a code from native and converting back
      int[] nativeCodes = {0, 1, 2};

      for (int nativeCode : nativeCodes) {
        FuelExhaustionAction action = FuelExhaustionAction.fromCode(nativeCode);
        int convertedCode = action.getCode();
        assertEquals(nativeCode, convertedCode, "Native code should round-trip correctly");
      }
    }

    @Test
    @DisplayName("should support callback decision pattern")
    void shouldSupportCallbackDecisionPattern() {
      // Simulate a callback decision based on action
      for (FuelExhaustionAction action : FuelExhaustionAction.values()) {
        boolean shouldAddFuel = (action == FuelExhaustionAction.CONTINUE);
        boolean shouldTerminate = (action == FuelExhaustionAction.TRAP);
        boolean shouldSuspend = (action == FuelExhaustionAction.PAUSE);

        // Exactly one should be true
        int trueCount =
            (shouldAddFuel ? 1 : 0) + (shouldTerminate ? 1 : 0) + (shouldSuspend ? 1 : 0);
        assertEquals(1, trueCount, "Exactly one decision should be true for " + action);
      }
    }

    @Test
    @DisplayName("should provide meaningful action semantics")
    void shouldProvideMeaningfulActionSemantics() {
      // CONTINUE = 0: default/success case, add more fuel
      assertEquals(
          0, FuelExhaustionAction.CONTINUE.getCode(), "CONTINUE should be code 0 (default)");

      // TRAP = 1: error/termination case
      assertEquals(1, FuelExhaustionAction.TRAP.getCode(), "TRAP should indicate termination");

      // PAUSE = 2: suspension for async
      assertEquals(2, FuelExhaustionAction.PAUSE.getCode(), "PAUSE should indicate suspension");
    }
  }
}
