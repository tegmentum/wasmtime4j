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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.func.CallHook;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CallHook}.
 *
 * <p>Verifies enum structure, constants, getValue/fromValue, ordinals, and round-trip conversion.
 */
@DisplayName("CallHook Tests")
class CallHookTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(CallHook.class.isEnum(), "CallHook should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactValueCount() {
      assertEquals(4, CallHook.values().length, "CallHook should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain CALLING_WASM")
    void shouldContainCallingWasm() {
      assertNotNull(CallHook.CALLING_WASM, "CALLING_WASM constant should exist");
      assertEquals("CALLING_WASM", CallHook.CALLING_WASM.name(), "CALLING_WASM name should match");
    }

    @Test
    @DisplayName("should contain RETURNING_FROM_WASM")
    void shouldContainReturningFromWasm() {
      assertNotNull(CallHook.RETURNING_FROM_WASM, "RETURNING_FROM_WASM constant should exist");
    }

    @Test
    @DisplayName("should contain CALLING_HOST")
    void shouldContainCallingHost() {
      assertNotNull(CallHook.CALLING_HOST, "CALLING_HOST constant should exist");
    }

    @Test
    @DisplayName("should contain RETURNING_FROM_HOST")
    void shouldContainReturningFromHost() {
      assertNotNull(CallHook.RETURNING_FROM_HOST, "RETURNING_FROM_HOST constant should exist");
    }
  }

  @Nested
  @DisplayName("GetValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("CALLING_WASM should have value 0")
    void callingWasmShouldHaveValue0() {
      assertEquals(0, CallHook.CALLING_WASM.getValue(), "CALLING_WASM should have value 0");
    }

    @Test
    @DisplayName("RETURNING_FROM_WASM should have value 1")
    void returningFromWasmShouldHaveValue1() {
      assertEquals(
          1, CallHook.RETURNING_FROM_WASM.getValue(), "RETURNING_FROM_WASM should have value 1");
    }

    @Test
    @DisplayName("CALLING_HOST should have value 2")
    void callingHostShouldHaveValue2() {
      assertEquals(2, CallHook.CALLING_HOST.getValue(), "CALLING_HOST should have value 2");
    }

    @Test
    @DisplayName("RETURNING_FROM_HOST should have value 3")
    void returningFromHostShouldHaveValue3() {
      assertEquals(
          3, CallHook.RETURNING_FROM_HOST.getValue(), "RETURNING_FROM_HOST should have value 3");
    }

    @Test
    @DisplayName("should have unique values across all constants")
    void shouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (final CallHook hook : CallHook.values()) {
        values.add(hook.getValue());
      }
      assertEquals(CallHook.values().length, values.size(), "All values should be unique");
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("should return CALLING_WASM for value 0")
    void shouldReturnCallingWasmForValue0() {
      assertEquals(
          CallHook.CALLING_WASM, CallHook.fromValue(0), "fromValue(0) should return CALLING_WASM");
    }

    @Test
    @DisplayName("should return RETURNING_FROM_WASM for value 1")
    void shouldReturnReturningFromWasmForValue1() {
      assertEquals(
          CallHook.RETURNING_FROM_WASM,
          CallHook.fromValue(1),
          "fromValue(1) should return RETURNING_FROM_WASM");
    }

    @Test
    @DisplayName("should return CALLING_HOST for value 2")
    void shouldReturnCallingHostForValue2() {
      assertEquals(
          CallHook.CALLING_HOST, CallHook.fromValue(2), "fromValue(2) should return CALLING_HOST");
    }

    @Test
    @DisplayName("should return RETURNING_FROM_HOST for value 3")
    void shouldReturnReturningFromHostForValue3() {
      assertEquals(
          CallHook.RETURNING_FROM_HOST,
          CallHook.fromValue(3),
          "fromValue(3) should return RETURNING_FROM_HOST");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative value")
    void shouldThrowForNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CallHook.fromValue(-1),
          "fromValue(-1) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for out-of-range value")
    void shouldThrowForOutOfRangeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CallHook.fromValue(99),
          "fromValue(99) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should round-trip getValue and fromValue for all constants")
    void shouldRoundTripGetValueAndFromValue() {
      for (final CallHook hook : CallHook.values()) {
        assertEquals(
            hook,
            CallHook.fromValue(hook.getValue()),
            "Round-trip should return original for " + hook.name());
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final CallHook value : CallHook.values()) {
        assertEquals(
            value, CallHook.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CallHook.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support native FFI code round-trip via int array")
    void shouldSupportNativeFfiCodeRoundTrip() {
      final int[] nativeCodes = new int[CallHook.values().length];
      for (int i = 0; i < CallHook.values().length; i++) {
        nativeCodes[i] = CallHook.values()[i].getValue();
      }
      for (int i = 0; i < nativeCodes.length; i++) {
        final CallHook resolved = CallHook.fromValue(nativeCodes[i]);
        assertEquals(
            CallHook.values()[i], resolved, "FFI round-trip should preserve enum at index " + i);
      }
    }
  }
}
