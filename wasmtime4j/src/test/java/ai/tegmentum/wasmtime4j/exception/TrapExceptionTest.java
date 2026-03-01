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
  @DisplayName("Category Method False Cases Tests")
  class CategoryMethodFalseCasesTests {

    @Test
    @DisplayName("isMemoryError should return false for all non-memory trap types")
    void isMemoryErrorShouldReturnFalseForNonMemoryTraps() {
      // Test all trap types that should NOT be memory errors
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "msg").isMemoryError(),
          "INTEGER_OVERFLOW should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "msg").isMemoryError(),
          "INTEGER_DIVISION_BY_ZERO should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, "msg")
              .isMemoryError(),
          "BAD_CONVERSION_TO_INTEGER should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, "msg").isMemoryError(),
          "INDIRECT_CALL_TO_NULL should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_SIGNATURE, "msg").isMemoryError(),
          "BAD_SIGNATURE should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "msg").isMemoryError(),
          "UNREACHABLE_CODE_REACHED should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.OUT_OF_FUEL, "msg").isMemoryError(),
          "OUT_OF_FUEL should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTERRUPT, "msg").isMemoryError(),
          "INTERRUPT should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "msg").isMemoryError(),
          "TABLE_OUT_OF_BOUNDS should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "msg").isMemoryError(),
          "ARRAY_OUT_OF_BOUNDS should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.NULL_REFERENCE, "msg").isMemoryError(),
          "NULL_REFERENCE should not be memory error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNKNOWN, "msg").isMemoryError(),
          "UNKNOWN should not be memory error");
    }

    @Test
    @DisplayName("isArithmeticError should return false for all non-arithmetic trap types")
    void isArithmeticErrorShouldReturnFalseForNonArithmeticTraps() {
      // Test all trap types that should NOT be arithmetic errors
      assertFalse(
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "msg").isArithmeticError(),
          "MEMORY_OUT_OF_BOUNDS should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, "msg").isArithmeticError(),
          "HEAP_MISALIGNED should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "msg").isArithmeticError(),
          "STACK_OVERFLOW should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, "msg")
              .isArithmeticError(),
          "INDIRECT_CALL_TO_NULL should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_SIGNATURE, "msg").isArithmeticError(),
          "BAD_SIGNATURE should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "msg")
              .isArithmeticError(),
          "UNREACHABLE_CODE_REACHED should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.OUT_OF_FUEL, "msg").isArithmeticError(),
          "OUT_OF_FUEL should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTERRUPT, "msg").isArithmeticError(),
          "INTERRUPT should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "msg").isArithmeticError(),
          "TABLE_OUT_OF_BOUNDS should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "msg").isArithmeticError(),
          "ARRAY_OUT_OF_BOUNDS should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.NULL_REFERENCE, "msg").isArithmeticError(),
          "NULL_REFERENCE should not be arithmetic error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNKNOWN, "msg").isArithmeticError(),
          "UNKNOWN should not be arithmetic error");
    }

    @Test
    @DisplayName("isControlFlowError should return false for all non-control-flow trap types")
    void isControlFlowErrorShouldReturnFalseForNonControlFlowTraps() {
      // Test all trap types that should NOT be control flow errors
      assertFalse(
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "msg")
              .isControlFlowError(),
          "MEMORY_OUT_OF_BOUNDS should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, "msg").isControlFlowError(),
          "HEAP_MISALIGNED should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "msg").isControlFlowError(),
          "STACK_OVERFLOW should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "msg").isControlFlowError(),
          "INTEGER_OVERFLOW should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "msg")
              .isControlFlowError(),
          "INTEGER_DIVISION_BY_ZERO should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, "msg")
              .isControlFlowError(),
          "BAD_CONVERSION_TO_INTEGER should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.OUT_OF_FUEL, "msg").isControlFlowError(),
          "OUT_OF_FUEL should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTERRUPT, "msg").isControlFlowError(),
          "INTERRUPT should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "msg").isControlFlowError(),
          "TABLE_OUT_OF_BOUNDS should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "msg").isControlFlowError(),
          "ARRAY_OUT_OF_BOUNDS should not be control flow error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNKNOWN, "msg").isControlFlowError(),
          "UNKNOWN should not be control flow error");
    }

    @Test
    @DisplayName(
        "isResourceExhaustionError should return false for all non-resource-exhaustion trap types")
    void isResourceExhaustionErrorShouldReturnFalseForNonResourceExhaustionTraps() {
      // Test all trap types that should NOT be resource exhaustion errors
      assertFalse(
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "msg")
              .isResourceExhaustionError(),
          "MEMORY_OUT_OF_BOUNDS should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, "msg")
              .isResourceExhaustionError(),
          "HEAP_MISALIGNED should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "msg")
              .isResourceExhaustionError(),
          "INTEGER_OVERFLOW should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "msg")
              .isResourceExhaustionError(),
          "INTEGER_DIVISION_BY_ZERO should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, "msg")
              .isResourceExhaustionError(),
          "BAD_CONVERSION_TO_INTEGER should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, "msg")
              .isResourceExhaustionError(),
          "INDIRECT_CALL_TO_NULL should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_SIGNATURE, "msg")
              .isResourceExhaustionError(),
          "BAD_SIGNATURE should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "msg")
              .isResourceExhaustionError(),
          "UNREACHABLE_CODE_REACHED should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "msg")
              .isResourceExhaustionError(),
          "TABLE_OUT_OF_BOUNDS should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "msg")
              .isResourceExhaustionError(),
          "ARRAY_OUT_OF_BOUNDS should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.NULL_REFERENCE, "msg")
              .isResourceExhaustionError(),
          "NULL_REFERENCE should not be resource exhaustion error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNKNOWN, "msg").isResourceExhaustionError(),
          "UNKNOWN should not be resource exhaustion error");
    }

    @Test
    @DisplayName("isBoundsError should return false for all non-bounds trap types")
    void isBoundsErrorShouldReturnFalseForNonBoundsTraps() {
      // Test all trap types that should NOT be bounds errors
      assertFalse(
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, "msg").isBoundsError(),
          "HEAP_MISALIGNED should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "msg").isBoundsError(),
          "STACK_OVERFLOW should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "msg").isBoundsError(),
          "INTEGER_OVERFLOW should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "msg").isBoundsError(),
          "INTEGER_DIVISION_BY_ZERO should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, "msg")
              .isBoundsError(),
          "BAD_CONVERSION_TO_INTEGER should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, "msg").isBoundsError(),
          "INDIRECT_CALL_TO_NULL should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.BAD_SIGNATURE, "msg").isBoundsError(),
          "BAD_SIGNATURE should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "msg").isBoundsError(),
          "UNREACHABLE_CODE_REACHED should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.OUT_OF_FUEL, "msg").isBoundsError(),
          "OUT_OF_FUEL should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.INTERRUPT, "msg").isBoundsError(),
          "INTERRUPT should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.NULL_REFERENCE, "msg").isBoundsError(),
          "NULL_REFERENCE should not be bounds error");
      assertFalse(
          new TrapException(TrapException.TrapType.UNKNOWN, "msg").isBoundsError(),
          "UNKNOWN should not be bounds error");
    }
  }

  @Nested
  @DisplayName("Category Method Individual Branch Tests")
  class CategoryMethodIndividualBranchTests {

    @Test
    @DisplayName("isMemoryError each branch should be tested individually")
    void isMemoryErrorEachBranchShouldBeTested() {
      // Test first condition: trapType == TrapType.MEMORY_OUT_OF_BOUNDS
      final TrapException memOutOfBounds =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "test");
      assertTrue(memOutOfBounds.isMemoryError(), "First branch: MEMORY_OUT_OF_BOUNDS");

      // Test second condition: trapType == TrapType.HEAP_MISALIGNED
      final TrapException heapMisaligned =
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, "test");
      assertTrue(heapMisaligned.isMemoryError(), "Second branch: HEAP_MISALIGNED");

      // Test third condition: trapType == TrapType.STACK_OVERFLOW
      final TrapException stackOverflow =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "test");
      assertTrue(stackOverflow.isMemoryError(), "Third branch: STACK_OVERFLOW");
    }

    @Test
    @DisplayName("isArithmeticError each branch should be tested individually")
    void isArithmeticErrorEachBranchShouldBeTested() {
      // Test first condition: trapType == TrapType.INTEGER_OVERFLOW
      final TrapException intOverflow =
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "test");
      assertTrue(intOverflow.isArithmeticError(), "First branch: INTEGER_OVERFLOW");

      // Test second condition: trapType == TrapType.INTEGER_DIVISION_BY_ZERO
      final TrapException divByZero =
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "test");
      assertTrue(divByZero.isArithmeticError(), "Second branch: INTEGER_DIVISION_BY_ZERO");

      // Test third condition: trapType == TrapType.BAD_CONVERSION_TO_INTEGER
      final TrapException badConv =
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, "test");
      assertTrue(badConv.isArithmeticError(), "Third branch: BAD_CONVERSION_TO_INTEGER");
    }

    @Test
    @DisplayName("isControlFlowError each branch should be tested individually")
    void isControlFlowErrorEachBranchShouldBeTested() {
      // Test first condition: trapType == TrapType.INDIRECT_CALL_TO_NULL
      final TrapException indirectNull =
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, "test");
      assertTrue(indirectNull.isControlFlowError(), "First branch: INDIRECT_CALL_TO_NULL");

      // Test second condition: trapType == TrapType.BAD_SIGNATURE
      final TrapException badSig = new TrapException(TrapException.TrapType.BAD_SIGNATURE, "test");
      assertTrue(badSig.isControlFlowError(), "Second branch: BAD_SIGNATURE");

      // Test third condition: trapType == TrapType.UNREACHABLE_CODE_REACHED
      final TrapException unreachable =
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "test");
      assertTrue(unreachable.isControlFlowError(), "Third branch: UNREACHABLE_CODE_REACHED");

      // Test fourth condition: trapType == TrapType.NULL_REFERENCE
      final TrapException nullRef =
          new TrapException(TrapException.TrapType.NULL_REFERENCE, "test");
      assertTrue(nullRef.isControlFlowError(), "Fourth branch: NULL_REFERENCE");
    }

    @Test
    @DisplayName("isResourceExhaustionError each branch should be tested individually")
    void isResourceExhaustionErrorEachBranchShouldBeTested() {
      // Test first condition: trapType == TrapType.STACK_OVERFLOW
      final TrapException stackOverflow =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "test");
      assertTrue(stackOverflow.isResourceExhaustionError(), "First branch: STACK_OVERFLOW");

      // Test second condition: trapType == TrapType.OUT_OF_FUEL
      final TrapException outOfFuel = new TrapException(TrapException.TrapType.OUT_OF_FUEL, "test");
      assertTrue(outOfFuel.isResourceExhaustionError(), "Second branch: OUT_OF_FUEL");

      // Test third condition: trapType == TrapType.INTERRUPT
      final TrapException interrupt = new TrapException(TrapException.TrapType.INTERRUPT, "test");
      assertTrue(interrupt.isResourceExhaustionError(), "Third branch: INTERRUPT");
    }

    @Test
    @DisplayName("isBoundsError each branch should be tested individually")
    void isBoundsErrorEachBranchShouldBeTested() {
      // Test first condition: trapType == TrapType.MEMORY_OUT_OF_BOUNDS
      final TrapException memOutOfBounds =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "test");
      assertTrue(memOutOfBounds.isBoundsError(), "First branch: MEMORY_OUT_OF_BOUNDS");

      // Test second condition: trapType == TrapType.TABLE_OUT_OF_BOUNDS
      final TrapException tableOutOfBounds =
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "test");
      assertTrue(tableOutOfBounds.isBoundsError(), "Second branch: TABLE_OUT_OF_BOUNDS");

      // Test third condition: trapType == TrapType.ARRAY_OUT_OF_BOUNDS
      final TrapException arrayOutOfBounds =
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "test");
      assertTrue(arrayOutOfBounds.isBoundsError(), "Third branch: ARRAY_OUT_OF_BOUNDS");
    }
  }

  @Nested
  @DisplayName("Category Overlap Verification Tests")
  class CategoryOverlapVerificationTests {

    @Test
    @DisplayName("STACK_OVERFLOW should be both memory and resource exhaustion error")
    void stackOverflowShouldBeBothMemoryAndResourceExhaustion() {
      final TrapException stackOverflow =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "test");
      assertTrue(stackOverflow.isMemoryError(), "STACK_OVERFLOW should be memory error");
      assertTrue(
          stackOverflow.isResourceExhaustionError(),
          "STACK_OVERFLOW should be resource exhaustion error");
      assertFalse(stackOverflow.isArithmeticError(), "STACK_OVERFLOW should not be arithmetic");
      assertFalse(stackOverflow.isControlFlowError(), "STACK_OVERFLOW should not be control flow");
      assertFalse(stackOverflow.isBoundsError(), "STACK_OVERFLOW should not be bounds error");
    }

    @Test
    @DisplayName("MEMORY_OUT_OF_BOUNDS should be both memory and bounds error")
    void memoryOutOfBoundsShouldBeBothMemoryAndBounds() {
      final TrapException memOob =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "test");
      assertTrue(memOob.isMemoryError(), "MEMORY_OUT_OF_BOUNDS should be memory error");
      assertTrue(memOob.isBoundsError(), "MEMORY_OUT_OF_BOUNDS should be bounds error");
      assertFalse(memOob.isArithmeticError(), "MEMORY_OUT_OF_BOUNDS should not be arithmetic");
      assertFalse(memOob.isControlFlowError(), "MEMORY_OUT_OF_BOUNDS should not be control flow");
      assertFalse(
          memOob.isResourceExhaustionError(),
          "MEMORY_OUT_OF_BOUNDS should not be resource exhaustion");
    }

    @Test
    @DisplayName("UNKNOWN trap type should not belong to any category")
    void unknownShouldNotBelongToAnyCategory() {
      final TrapException unknown = new TrapException(TrapException.TrapType.UNKNOWN, "test");
      assertFalse(unknown.isMemoryError(), "UNKNOWN should not be memory error");
      assertFalse(unknown.isArithmeticError(), "UNKNOWN should not be arithmetic error");
      assertFalse(unknown.isControlFlowError(), "UNKNOWN should not be control flow error");
      assertFalse(
          unknown.isResourceExhaustionError(), "UNKNOWN should not be resource exhaustion error");
      assertFalse(unknown.isBoundsError(), "UNKNOWN should not be bounds error");
    }

    @Test
    @DisplayName("All trap types should have consistent category membership")
    void allTrapTypesShouldHaveConsistentCategoryMembership() {
      for (final TrapException.TrapType trapType : TrapException.TrapType.values()) {
        final TrapException exception = new TrapException(trapType, "test");

        // Each trap type should have a definitive answer for each category
        final boolean isMemory = exception.isMemoryError();
        final boolean isArithmetic = exception.isArithmeticError();
        final boolean isControlFlow = exception.isControlFlowError();
        final boolean isResourceExhaustion = exception.isResourceExhaustionError();
        final boolean isBounds = exception.isBoundsError();

        // Verify consistent with repeated calls
        assertEquals(
            isMemory, exception.isMemoryError(), trapType + " isMemoryError should be consistent");
        assertEquals(
            isArithmetic,
            exception.isArithmeticError(),
            trapType + " isArithmeticError should be consistent");
        assertEquals(
            isControlFlow,
            exception.isControlFlowError(),
            trapType + " isControlFlowError should be consistent");
        assertEquals(
            isResourceExhaustion,
            exception.isResourceExhaustionError(),
            trapType + " isResourceExhaustionError should be consistent");
        assertEquals(
            isBounds, exception.isBoundsError(), trapType + " isBoundsError should be consistent");
      }
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
