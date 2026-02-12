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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link FunctionType} validation methods: {@link FunctionType#validateReturnValues},
 * {@link FunctionType#matchesMultiValuePattern}, {@link FunctionType#validateMultiValueLimits}, and
 * {@link FunctionType#getMaxValueCount}.
 *
 * <p>All methods under test are pure Java with no native calls, but DualRuntimeTest is used for
 * consistency with the project testing pattern.
 *
 * @since 1.0.0
 */
@DisplayName("FunctionType Validation Tests")
public class FunctionTypeValidationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(FunctionTypeValidationTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ---------- validateReturnValues ----------

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateReturnValues accepts single I32 return")
  void validateReturnValuesAcceptsSingleI32(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateReturnValues with single I32 return");

    final FunctionType funcType =
        new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

    assertDoesNotThrow(
        () -> funcType.validateReturnValues(new WasmValue[] {WasmValue.i32(42)}),
        "Single I32 return value should pass validation");

    LOGGER.info("[" + runtime + "] validateReturnValues accepted single I32 return");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateReturnValues accepts multi-return [I32, F64]")
  void validateReturnValuesAcceptsMultiReturn(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateReturnValues with multi-return [I32, F64]");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[0], new WasmValueType[] {WasmValueType.I32, WasmValueType.F64});

    assertDoesNotThrow(
        () ->
            funcType.validateReturnValues(new WasmValue[] {WasmValue.i32(10), WasmValue.f64(3.14)}),
        "Multi-return [I32, F64] should pass validation");

    LOGGER.info("[" + runtime + "] validateReturnValues accepted multi-return [I32, F64]");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateReturnValues with null array throws for non-void return type")
  void validateReturnValuesNullThrowsForNonVoid(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateReturnValues null array with non-void returns");

    final FunctionType funcType =
        new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> funcType.validateReturnValues(null),
            "Null return values for non-void function should throw IllegalArgumentException");

    LOGGER.info("[" + runtime + "] Threw as expected: " + ex.getMessage());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateReturnValues with null array accepted for void function")
  void validateReturnValuesNullAcceptedForVoid(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateReturnValues null array for void function");

    final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);

    assertDoesNotThrow(
        () -> funcType.validateReturnValues(null),
        "Null return values for void function should not throw");

    LOGGER.info("[" + runtime + "] validateReturnValues accepted null for void function");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateReturnValues count mismatch throws")
  void validateReturnValuesCountMismatchThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateReturnValues with count mismatch");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[0], new WasmValueType[] {WasmValueType.I32, WasmValueType.F64});

    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> funcType.validateReturnValues(new WasmValue[] {WasmValue.i32(1)}),
            "Wrong return count should throw IllegalArgumentException");

    LOGGER.info("[" + runtime + "] Threw as expected for count mismatch: " + ex.getMessage());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateReturnValues type mismatch throws")
  void validateReturnValuesTypeMismatchThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateReturnValues with type mismatch");

    final FunctionType funcType =
        new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

    // Provide F64 where I32 is expected
    assertThrows(
        Exception.class,
        () -> funcType.validateReturnValues(new WasmValue[] {WasmValue.f64(3.14)}),
        "F64 for I32 slot should throw from validateType");

    LOGGER.info("[" + runtime + "] validateReturnValues threw for type mismatch");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateReturnValues null element throws")
  void validateReturnValuesNullElementThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateReturnValues with null element");

    final FunctionType funcType =
        new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> funcType.validateReturnValues(new WasmValue[] {null}),
            "Null element in return values should throw IllegalArgumentException");

    LOGGER.info("[" + runtime + "] Threw as expected for null element: " + ex.getMessage());
  }

  // ---------- matchesMultiValuePattern ----------

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesMultiValuePattern exact match returns true")
  void matchesMultiValuePatternExactMatch(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesMultiValuePattern with exact match");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertTrue(
        funcType.matchesMultiValuePattern(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64}),
        "Exact match should return true");

    LOGGER.info("[" + runtime + "] matchesMultiValuePattern returned true for exact match");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesMultiValuePattern param mismatch returns false")
  void matchesMultiValuePatternParamMismatch(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesMultiValuePattern with param mismatch");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertFalse(
        funcType.matchesMultiValuePattern(
            new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.F64}),
        "Param type mismatch should return false");

    LOGGER.info("[" + runtime + "] matchesMultiValuePattern returned false for param mismatch");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesMultiValuePattern return mismatch returns false")
  void matchesMultiValuePatternReturnMismatch(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesMultiValuePattern with return mismatch");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertFalse(
        funcType.matchesMultiValuePattern(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F32}),
        "Return type mismatch should return false");

    LOGGER.info("[" + runtime + "] matchesMultiValuePattern returned false for return mismatch");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesMultiValuePattern null args returns false")
  void matchesMultiValuePatternNullArgs(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesMultiValuePattern with null arguments");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertFalse(
        funcType.matchesMultiValuePattern(null, new WasmValueType[] {WasmValueType.F64}),
        "Null params should return false");

    assertFalse(
        funcType.matchesMultiValuePattern(new WasmValueType[] {WasmValueType.I32}, null),
        "Null returns should return false");

    LOGGER.info("[" + runtime + "] matchesMultiValuePattern returned false for null arguments");
  }

  // ---------- getMaxValueCount + validateMultiValueLimits ----------

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getMaxValueCount returns 16")
  void getMaxValueCountReturns16(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getMaxValueCount");

    assertEquals(16, FunctionType.getMaxValueCount(), "WebAssembly multi-value limit should be 16");

    LOGGER.info("[" + runtime + "] getMaxValueCount returned 16");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateMultiValueLimits passes within limit")
  void validateMultiValueLimitsWithinLimit(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateMultiValueLimits with 16 params and 16 returns");

    final WasmValueType[] sixteenTypes = new WasmValueType[16];
    for (int i = 0; i < 16; i++) {
      sixteenTypes[i] = WasmValueType.I32;
    }

    final FunctionType funcType = new FunctionType(sixteenTypes, sixteenTypes.clone());

    assertDoesNotThrow(
        funcType::validateMultiValueLimits, "16 params + 16 returns should not exceed limits");

    LOGGER.info("[" + runtime + "] validateMultiValueLimits passed with 16+16 types");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateMultiValueLimits too many params throws")
  void validateMultiValueLimitsTooManyParams(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateMultiValueLimits with 17 params");

    final WasmValueType[] seventeenTypes = new WasmValueType[17];
    for (int i = 0; i < 17; i++) {
      seventeenTypes[i] = WasmValueType.I32;
    }

    final FunctionType funcType =
        new FunctionType(seventeenTypes, new WasmValueType[] {WasmValueType.I32});

    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            funcType::validateMultiValueLimits,
            "17 params should exceed multi-value limit");

    LOGGER.info("[" + runtime + "] Threw as expected for 17 params: " + ex.getMessage());
  }
}
