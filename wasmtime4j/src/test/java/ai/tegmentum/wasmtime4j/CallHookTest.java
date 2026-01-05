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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CallHook enum.
 *
 * <p>CallHook represents the different states of transitions between WebAssembly and host code
 * during execution. This test verifies the enum structure, values, and conversion methods.
 */
@DisplayName("CallHook Enum Tests")
class CallHookTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(CallHook.class.isEnum(), "CallHook should be an enum");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      assertEquals(4, CallHook.values().length, "CallHook should have exactly 4 values");
    }

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      Set<String> expectedValues =
          Set.of("CALLING_WASM", "RETURNING_FROM_WASM", "CALLING_HOST", "RETURNING_FROM_HOST");
      Set<String> actualValues =
          Arrays.stream(CallHook.values()).map(Enum::name).collect(Collectors.toSet());
      assertEquals(expectedValues, actualValues, "CallHook should have all expected values");
    }
  }

  // ========================================================================
  // Enum Value Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("CALLING_WASM should exist")
    void callingWasmShouldExist() {
      CallHook value = CallHook.CALLING_WASM;
      assertNotNull(value, "CALLING_WASM should exist");
      assertEquals("CALLING_WASM", value.name(), "CALLING_WASM should have correct name");
    }

    @Test
    @DisplayName("RETURNING_FROM_WASM should exist")
    void returningFromWasmShouldExist() {
      CallHook value = CallHook.RETURNING_FROM_WASM;
      assertNotNull(value, "RETURNING_FROM_WASM should exist");
      assertEquals(
          "RETURNING_FROM_WASM", value.name(), "RETURNING_FROM_WASM should have correct name");
    }

    @Test
    @DisplayName("CALLING_HOST should exist")
    void callingHostShouldExist() {
      CallHook value = CallHook.CALLING_HOST;
      assertNotNull(value, "CALLING_HOST should exist");
      assertEquals("CALLING_HOST", value.name(), "CALLING_HOST should have correct name");
    }

    @Test
    @DisplayName("RETURNING_FROM_HOST should exist")
    void returningFromHostShouldExist() {
      CallHook value = CallHook.RETURNING_FROM_HOST;
      assertNotNull(value, "RETURNING_FROM_HOST should exist");
      assertEquals(
          "RETURNING_FROM_HOST", value.name(), "RETURNING_FROM_HOST should have correct name");
    }
  }

  // ========================================================================
  // getValue Method Tests
  // ========================================================================

  @Nested
  @DisplayName("getValue Method Tests")
  class GetValueMethodTests {

    @Test
    @DisplayName("CALLING_WASM should have value 0")
    void callingWasmShouldHaveValueZero() {
      assertEquals(0, CallHook.CALLING_WASM.getValue(), "CALLING_WASM should have value 0");
    }

    @Test
    @DisplayName("RETURNING_FROM_WASM should have value 1")
    void returningFromWasmShouldHaveValueOne() {
      assertEquals(
          1, CallHook.RETURNING_FROM_WASM.getValue(), "RETURNING_FROM_WASM should have value 1");
    }

    @Test
    @DisplayName("CALLING_HOST should have value 2")
    void callingHostShouldHaveValueTwo() {
      assertEquals(2, CallHook.CALLING_HOST.getValue(), "CALLING_HOST should have value 2");
    }

    @Test
    @DisplayName("RETURNING_FROM_HOST should have value 3")
    void returningFromHostShouldHaveValueThree() {
      assertEquals(
          3, CallHook.RETURNING_FROM_HOST.getValue(), "RETURNING_FROM_HOST should have value 3");
    }

    @Test
    @DisplayName("all values should have unique numeric values")
    void allValuesShouldHaveUniqueNumericValues() {
      int[] values = Arrays.stream(CallHook.values()).mapToInt(CallHook::getValue).toArray();
      Set<Integer> uniqueValues = Arrays.stream(values).boxed().collect(Collectors.toSet());
      assertEquals(
          values.length,
          uniqueValues.size(),
          "All CallHook values should have unique numeric values");
    }

    @Test
    @DisplayName("values should be consecutive starting from 0")
    void valuesShouldBeConsecutiveStartingFromZero() {
      int[] values =
          Arrays.stream(CallHook.values()).mapToInt(CallHook::getValue).sorted().toArray();
      int[] expected = {0, 1, 2, 3};
      assertArrayEquals(expected, values, "Values should be consecutive starting from 0");
    }
  }

  // ========================================================================
  // fromValue Method Tests
  // ========================================================================

  @Nested
  @DisplayName("fromValue Method Tests")
  class FromValueMethodTests {

    @Test
    @DisplayName("fromValue(0) should return CALLING_WASM")
    void fromValueZeroShouldReturnCallingWasm() {
      assertEquals(
          CallHook.CALLING_WASM, CallHook.fromValue(0), "fromValue(0) should return CALLING_WASM");
    }

    @Test
    @DisplayName("fromValue(1) should return RETURNING_FROM_WASM")
    void fromValueOneShouldReturnReturningFromWasm() {
      assertEquals(
          CallHook.RETURNING_FROM_WASM,
          CallHook.fromValue(1),
          "fromValue(1) should return RETURNING_FROM_WASM");
    }

    @Test
    @DisplayName("fromValue(2) should return CALLING_HOST")
    void fromValueTwoShouldReturnCallingHost() {
      assertEquals(
          CallHook.CALLING_HOST, CallHook.fromValue(2), "fromValue(2) should return CALLING_HOST");
    }

    @Test
    @DisplayName("fromValue(3) should return RETURNING_FROM_HOST")
    void fromValueThreeShouldReturnReturningFromHost() {
      assertEquals(
          CallHook.RETURNING_FROM_HOST,
          CallHook.fromValue(3),
          "fromValue(3) should return RETURNING_FROM_HOST");
    }

    @Test
    @DisplayName("fromValue should throw IllegalArgumentException for invalid value")
    void fromValueShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CallHook.fromValue(4),
          "fromValue(4) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromValue should throw IllegalArgumentException for negative value")
    void fromValueShouldThrowForNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CallHook.fromValue(-1),
          "fromValue(-1) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromValue should throw IllegalArgumentException for large value")
    void fromValueShouldThrowForLargeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CallHook.fromValue(100),
          "fromValue(100) should throw IllegalArgumentException");
    }
  }

  // ========================================================================
  // Round-Trip Tests
  // ========================================================================

  @Nested
  @DisplayName("Round-Trip Conversion Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getValue and fromValue should be inverses for all values")
    void getValueAndFromValueShouldBeInverses() {
      for (CallHook hook : CallHook.values()) {
        int value = hook.getValue();
        CallHook roundTrip = CallHook.fromValue(value);
        assertEquals(
            hook, roundTrip, "Round-trip conversion should preserve value for " + hook.name());
      }
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("ordinal values should match expected order")
    void ordinalValuesShouldMatchExpectedOrder() {
      assertEquals(0, CallHook.CALLING_WASM.ordinal(), "CALLING_WASM ordinal should be 0");
      assertEquals(
          1, CallHook.RETURNING_FROM_WASM.ordinal(), "RETURNING_FROM_WASM ordinal should be 1");
      assertEquals(2, CallHook.CALLING_HOST.ordinal(), "CALLING_HOST ordinal should be 2");
      assertEquals(
          3, CallHook.RETURNING_FROM_HOST.ordinal(), "RETURNING_FROM_HOST ordinal should be 3");
    }

    @Test
    @DisplayName("getValue should match ordinal for all values")
    void getValueShouldMatchOrdinal() {
      for (CallHook hook : CallHook.values()) {
        assertEquals(
            hook.ordinal(), hook.getValue(), "getValue should match ordinal for " + hook.name());
      }
    }
  }

  // ========================================================================
  // Enum Standard Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Standard Method Tests")
  class EnumStandardMethodTests {

    @Test
    @DisplayName("valueOf should work for all values")
    void valueOfShouldWorkForAllValues() {
      assertEquals(
          CallHook.CALLING_WASM,
          CallHook.valueOf("CALLING_WASM"),
          "valueOf should work for CALLING_WASM");
      assertEquals(
          CallHook.RETURNING_FROM_WASM,
          CallHook.valueOf("RETURNING_FROM_WASM"),
          "valueOf should work for RETURNING_FROM_WASM");
      assertEquals(
          CallHook.CALLING_HOST,
          CallHook.valueOf("CALLING_HOST"),
          "valueOf should work for CALLING_HOST");
      assertEquals(
          CallHook.RETURNING_FROM_HOST,
          CallHook.valueOf("RETURNING_FROM_HOST"),
          "valueOf should work for RETURNING_FROM_HOST");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for unknown value")
    void valueOfShouldThrowForUnknownValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CallHook.valueOf("UNKNOWN"),
          "valueOf should throw for unknown value");
    }

    @Test
    @DisplayName("toString should return name")
    void toStringShouldReturnName() {
      for (CallHook hook : CallHook.values()) {
        assertEquals(
            hook.name(), hook.toString(), "toString should return name for " + hook.name());
      }
    }
  }
}
