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
 * Tests for {@link FunctionType} validation methods: {@link FunctionType#validateReturnValues} and
 * {@link FunctionType#matchesMultiValuePattern}.
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

  // ---------- isCompatibleWith ----------

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isCompatibleWith exact match returns true")
  void isCompatibleWithExactMatch(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isCompatibleWith with exact match");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertTrue(
        funcType.isCompatibleWith(
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64})),
        "Exact match should return true");

    LOGGER.info("[" + runtime + "] isCompatibleWith returned true for exact match");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isCompatibleWith param mismatch returns false")
  void isCompatibleWithParamMismatch(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isCompatibleWith with param mismatch");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertFalse(
        funcType.isCompatibleWith(
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.F64})),
        "Param type mismatch should return false");

    LOGGER.info("[" + runtime + "] isCompatibleWith returned false for param mismatch");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isCompatibleWith return mismatch returns false")
  void isCompatibleWithReturnMismatch(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isCompatibleWith with return mismatch");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertFalse(
        funcType.isCompatibleWith(
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F32})),
        "Return type mismatch should return false");

    LOGGER.info("[" + runtime + "] isCompatibleWith returned false for return mismatch");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isCompatibleWith null returns false")
  void isCompatibleWithNull(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isCompatibleWith with null argument");

    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});

    assertFalse(funcType.isCompatibleWith(null), "Null should return false");

    LOGGER.info("[" + runtime + "] isCompatibleWith returned false for null argument");
  }
}
