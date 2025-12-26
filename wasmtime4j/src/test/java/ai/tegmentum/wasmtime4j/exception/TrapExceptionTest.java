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

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TrapException} class.
 *
 * <p>TrapException represents runtime trap conditions in WebAssembly execution.
 */
@DisplayName("TrapException Tests")
class TrapExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(TrapException.class.getModifiers()), "TrapException should be public");
    }

    @Test
    @DisplayName("should extend RuntimeException")
    void shouldExtendRuntimeException() {
      assertTrue(
          RuntimeException.class.isAssignableFrom(TrapException.class),
          "TrapException should extend RuntimeException");
    }

    @Test
    @DisplayName("should have TrapType nested enum")
    void shouldHaveTrapTypeNestedEnum() {
      final Class<?>[] declaredClasses = TrapException.class.getDeclaredClasses();
      boolean hasTrapType = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("TrapType") && clazz.isEnum()) {
          hasTrapType = true;
          break;
        }
      }
      assertTrue(hasTrapType, "TrapException should have TrapType nested enum");
    }
  }

  @Nested
  @DisplayName("TrapType Enum Tests")
  class TrapTypeEnumTests {

    @Test
    @DisplayName("should have all expected trap types")
    void shouldHaveAllExpectedTrapTypes() {
      final TrapException.TrapType[] values = TrapException.TrapType.values();
      assertTrue(values.length >= 14, "Should have at least 14 trap types");

      // Check key trap types exist
      assertNotNull(TrapException.TrapType.STACK_OVERFLOW, "Should have STACK_OVERFLOW");
      assertNotNull(
          TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Should have MEMORY_OUT_OF_BOUNDS");
      assertNotNull(
          TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "Should have INTEGER_DIVISION_BY_ZERO");
      assertNotNull(
          TrapException.TrapType.UNREACHABLE_CODE_REACHED, "Should have UNREACHABLE_CODE_REACHED");
      assertNotNull(TrapException.TrapType.OUT_OF_FUEL, "Should have OUT_OF_FUEL");
      assertNotNull(TrapException.TrapType.UNKNOWN, "Should have UNKNOWN");
    }

    @Test
    @DisplayName("each trap type should have description")
    void eachTrapTypeShouldHaveDescription() {
      for (final TrapException.TrapType trapType : TrapException.TrapType.values()) {
        assertNotNull(trapType.getDescription(), trapType.name() + " should have description");
        assertFalse(
            trapType.getDescription().isEmpty(),
            trapType.name() + " description should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with trap type and message")
    void shouldCreateExceptionWithTrapTypeAndMessage() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Access at offset 100");

      assertEquals(
          TrapException.TrapType.MEMORY_OUT_OF_BOUNDS,
          exception.getTrapType(),
          "Trap type should be set");
      assertTrue(
          exception.getMessage().contains("MEMORY_OUT_OF_BOUNDS"),
          "Message should contain trap type");
      assertTrue(
          exception.getMessage().contains("Access at offset 100"), "Message should contain detail");
    }

    @Test
    @DisplayName("should create exception with trap type, message, and cause")
    void shouldCreateExceptionWithTrapTypeMessageAndCause() {
      final RuntimeException cause = new RuntimeException("Underlying error");
      final TrapException exception =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "Recursion too deep", cause);

      assertEquals(
          TrapException.TrapType.STACK_OVERFLOW,
          exception.getTrapType(),
          "Trap type should be set");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("should create exception with full details")
    void shouldCreateExceptionWithFullDetails() {
      final TrapException exception =
          new TrapException(
              TrapException.TrapType.INTEGER_DIVISION_BY_ZERO,
              "Division failed",
              "wasm://module.wasm:func1+0x100",
              "divide",
              256,
              null);

      assertEquals(
          TrapException.TrapType.INTEGER_DIVISION_BY_ZERO,
          exception.getTrapType(),
          "Trap type should match");
      assertEquals(
          "wasm://module.wasm:func1+0x100",
          exception.getWasmBacktrace(),
          "Backtrace should be set");
      assertEquals("divide", exception.getFunctionName(), "Function name should be set");
      assertEquals(
          Integer.valueOf(256),
          exception.getInstructionOffset(),
          "Instruction offset should be set");
      assertTrue(exception.getMessage().contains("divide"), "Message should contain function name");
      assertTrue(
          exception.getMessage().contains("256"), "Message should contain instruction offset");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null or empty message")
    void shouldThrowForNullOrEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TrapException(TrapException.TrapType.UNKNOWN, null),
          "Should throw for null message");

      assertThrows(
          IllegalArgumentException.class,
          () -> new TrapException(TrapException.TrapType.UNKNOWN, ""),
          "Should throw for empty message");
    }

    @Test
    @DisplayName("should default to UNKNOWN when trap type is null")
    void shouldDefaultToUnknownWhenTrapTypeIsNull() {
      final TrapException exception =
          new TrapException(null, "Unknown error", null, null, null, null);

      assertEquals(
          TrapException.TrapType.UNKNOWN,
          exception.getTrapType(),
          "Should default to UNKNOWN trap type");
    }
  }

  @Nested
  @DisplayName("Category Check Methods Tests")
  class CategoryCheckMethodsTests {

    @Test
    @DisplayName("isMemoryError should return true for memory-related traps")
    void isMemoryErrorShouldReturnTrueForMemoryTraps() {
      assertTrue(
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "msg").isMemoryError(),
          "MEMORY_OUT_OF_BOUNDS should be memory error");
      assertTrue(
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, "msg").isMemoryError(),
          "HEAP_MISALIGNED should be memory error");
      assertTrue(
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "msg").isMemoryError(),
          "STACK_OVERFLOW should be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "msg").isMemoryError(),
          "INTEGER_DIVISION_BY_ZERO should not be memory error");
    }

    @Test
    @DisplayName("isArithmeticError should return true for arithmetic-related traps")
    void isArithmeticErrorShouldReturnTrueForArithmeticTraps() {
      assertTrue(
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "msg").isArithmeticError(),
          "INTEGER_OVERFLOW should be arithmetic error");
      assertTrue(
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "msg")
              .isArithmeticError(),
          "INTEGER_DIVISION_BY_ZERO should be arithmetic error");
      assertTrue(
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, "msg")
              .isArithmeticError(),
          "BAD_CONVERSION_TO_INTEGER should be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "msg").isArithmeticError(),
          "STACK_OVERFLOW should not be arithmetic error");
    }

    @Test
    @DisplayName("isControlFlowError should return true for control flow traps")
    void isControlFlowErrorShouldReturnTrueForControlFlowTraps() {
      assertTrue(
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, "msg")
              .isControlFlowError(),
          "INDIRECT_CALL_TO_NULL should be control flow error");
      assertTrue(
          new TrapException(TrapException.TrapType.BAD_SIGNATURE, "msg").isControlFlowError(),
          "BAD_SIGNATURE should be control flow error");
      assertTrue(
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "msg")
              .isControlFlowError(),
          "UNREACHABLE_CODE_REACHED should be control flow error");
      assertTrue(
          new TrapException(TrapException.TrapType.NULL_REFERENCE, "msg").isControlFlowError(),
          "NULL_REFERENCE should be control flow error");
    }

    @Test
    @DisplayName("isResourceExhaustionError should return true for resource exhaustion traps")
    void isResourceExhaustionErrorShouldReturnTrueForResourceExhaustionTraps() {
      assertTrue(
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "msg")
              .isResourceExhaustionError(),
          "STACK_OVERFLOW should be resource exhaustion error");
      assertTrue(
          new TrapException(TrapException.TrapType.OUT_OF_FUEL, "msg").isResourceExhaustionError(),
          "OUT_OF_FUEL should be resource exhaustion error");
      assertTrue(
          new TrapException(TrapException.TrapType.INTERRUPT, "msg").isResourceExhaustionError(),
          "INTERRUPT should be resource exhaustion error");
    }

    @Test
    @DisplayName("isBoundsError should return true for bounds checking traps")
    void isBoundsErrorShouldReturnTrueForBoundsTraps() {
      assertTrue(
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "msg").isBoundsError(),
          "MEMORY_OUT_OF_BOUNDS should be bounds error");
      assertTrue(
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "msg").isBoundsError(),
          "TABLE_OUT_OF_BOUNDS should be bounds error");
      assertTrue(
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "msg").isBoundsError(),
          "ARRAY_OUT_OF_BOUNDS should be bounds error");
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @Test
    @DisplayName("should provide recovery suggestion for each trap type")
    void shouldProvideRecoverySuggestionForEachTrapType() {
      for (final TrapException.TrapType trapType : TrapException.TrapType.values()) {
        final TrapException exception = new TrapException(trapType, "test message");
        assertNotNull(
            exception.getRecoverySuggestion(),
            trapType.name() + " should have recovery suggestion");
        assertFalse(
            exception.getRecoverySuggestion().isEmpty(),
            trapType.name() + " recovery suggestion should not be empty");
      }
    }

    @Test
    @DisplayName("should provide specific suggestion for stack overflow")
    void shouldProvideSpecificSuggestionForStackOverflow() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "test");
      assertTrue(
          exception.getRecoverySuggestion().toLowerCase().contains("recursion")
              || exception.getRecoverySuggestion().toLowerCase().contains("stack"),
          "Stack overflow suggestion should mention recursion or stack");
    }
  }

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("getWasmBacktrace should return null when not set")
    void getWasmBacktraceShouldReturnNullWhenNotSet() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.UNKNOWN, "test message");
      assertNull(exception.getWasmBacktrace(), "Backtrace should be null when not set");
    }

    @Test
    @DisplayName("getFunctionName should return null when not set")
    void getFunctionNameShouldReturnNullWhenNotSet() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.UNKNOWN, "test message");
      assertNull(exception.getFunctionName(), "Function name should be null when not set");
    }

    @Test
    @DisplayName("getInstructionOffset should return null when not set")
    void getInstructionOffsetShouldReturnNullWhenNotSet() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.UNKNOWN, "test message");
      assertNull(
          exception.getInstructionOffset(), "Instruction offset should be null when not set");
    }
  }
}
