package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.TrapException.TrapType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for the {@link TrapException} class.
 *
 * <p>This test class verifies trap exception construction, trap types, and classification methods.
 */
@DisplayName("TrapException Tests")
class TrapExceptionTest {

  @Nested
  @DisplayName("TrapType Enum Tests")
  class TrapTypeTests {

    @ParameterizedTest
    @EnumSource(TrapType.class)
    @DisplayName("All trap types should have non-null descriptions")
    void allTrapTypesShouldHaveDescriptions(final TrapType trapType) {
      assertNotNull(trapType.getDescription());
      assertFalse(trapType.getDescription().isEmpty());
    }

    @Test
    @DisplayName("STACK_OVERFLOW should have correct description")
    void stackOverflowDescription() {
      assertEquals(
          "Stack overflow - recursion depth exceeded limit",
          TrapType.STACK_OVERFLOW.getDescription());
    }

    @Test
    @DisplayName("MEMORY_OUT_OF_BOUNDS should have correct description")
    void memoryOutOfBoundsDescription() {
      assertEquals("Memory access out of bounds", TrapType.MEMORY_OUT_OF_BOUNDS.getDescription());
    }

    @Test
    @DisplayName("INTEGER_DIVISION_BY_ZERO should have correct description")
    void integerDivisionByZeroDescription() {
      assertEquals("Integer division by zero", TrapType.INTEGER_DIVISION_BY_ZERO.getDescription());
    }

    @Test
    @DisplayName("OUT_OF_FUEL should have correct description")
    void outOfFuelDescription() {
      assertEquals("Execution ran out of fuel", TrapType.OUT_OF_FUEL.getDescription());
    }

    @Test
    @DisplayName("All trap types should be accessible")
    void allTrapTypesAccessible() {
      TrapType[] allTypes = TrapType.values();
      assertEquals(15, allTypes.length);

      // Verify expected types exist
      assertNotNull(TrapType.valueOf("STACK_OVERFLOW"));
      assertNotNull(TrapType.valueOf("MEMORY_OUT_OF_BOUNDS"));
      assertNotNull(TrapType.valueOf("HEAP_MISALIGNED"));
      assertNotNull(TrapType.valueOf("TABLE_OUT_OF_BOUNDS"));
      assertNotNull(TrapType.valueOf("INDIRECT_CALL_TO_NULL"));
      assertNotNull(TrapType.valueOf("BAD_SIGNATURE"));
      assertNotNull(TrapType.valueOf("INTEGER_OVERFLOW"));
      assertNotNull(TrapType.valueOf("INTEGER_DIVISION_BY_ZERO"));
      assertNotNull(TrapType.valueOf("BAD_CONVERSION_TO_INTEGER"));
      assertNotNull(TrapType.valueOf("UNREACHABLE_CODE_REACHED"));
      assertNotNull(TrapType.valueOf("INTERRUPT"));
      assertNotNull(TrapType.valueOf("OUT_OF_FUEL"));
      assertNotNull(TrapType.valueOf("NULL_REFERENCE"));
      assertNotNull(TrapType.valueOf("ARRAY_OUT_OF_BOUNDS"));
      assertNotNull(TrapType.valueOf("UNKNOWN"));
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with trap type and message should set both")
    void constructorWithTrapTypeAndMessage() {
      TrapException exception =
          new TrapException(TrapType.MEMORY_OUT_OF_BOUNDS, "Access at offset 100");

      assertEquals(TrapType.MEMORY_OUT_OF_BOUNDS, exception.getTrapType());
      assertTrue(exception.getMessage().contains("Access at offset 100"));
      assertTrue(exception.getMessage().contains("MEMORY_OUT_OF_BOUNDS"));
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null trap type should default to UNKNOWN")
    void constructorWithNullTrapType() {
      TrapException exception = new TrapException(null, "Test message");

      assertEquals(TrapType.UNKNOWN, exception.getTrapType());
    }

    @Test
    @DisplayName("Constructor with empty message should throw IllegalArgumentException")
    void constructorWithEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            new TrapException(TrapType.STACK_OVERFLOW, "");
          });
    }

    @Test
    @DisplayName("Constructor with null message should throw IllegalArgumentException")
    void constructorWithNullMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            new TrapException(TrapType.STACK_OVERFLOW, null);
          });
    }

    @Test
    @DisplayName("Constructor with trap type, message, and cause should set all")
    void constructorWithTrapTypeMessageAndCause() {
      Throwable cause = new RuntimeException("Root cause");
      TrapException exception =
          new TrapException(TrapType.INTEGER_OVERFLOW, "Overflow occurred", cause);

      assertEquals(TrapType.INTEGER_OVERFLOW, exception.getTrapType());
      assertTrue(exception.getMessage().contains("Overflow occurred"));
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructor() {
      Throwable cause = new RuntimeException("Root cause");
      TrapException exception =
          new TrapException(
              TrapType.MEMORY_OUT_OF_BOUNDS,
              "Memory access error",
              "0: function[0]::add\n1: function[1]::main",
              "add",
              42,
              cause);

      assertEquals(TrapType.MEMORY_OUT_OF_BOUNDS, exception.getTrapType());
      assertEquals("0: function[0]::add\n1: function[1]::main", exception.getWasmBacktrace());
      assertEquals("add", exception.getFunctionName());
      assertEquals(Integer.valueOf(42), exception.getInstructionOffset());
      assertSame(cause, exception.getCause());
      assertTrue(exception.getMessage().contains("add"));
      assertTrue(exception.getMessage().contains("42"));
    }

    @Test
    @DisplayName("Constructor with null optional fields should accept them")
    void constructorWithNullOptionalFields() {
      TrapException exception =
          new TrapException(TrapType.STACK_OVERFLOW, "Stack overflow", null, null, null, null);

      assertEquals(TrapType.STACK_OVERFLOW, exception.getTrapType());
      assertNull(exception.getWasmBacktrace());
      assertNull(exception.getFunctionName());
      assertNull(exception.getInstructionOffset());
      assertNull(exception.getCause());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getTrapType should return correct trap type")
    void getTrapType() {
      TrapException exception = new TrapException(TrapType.TABLE_OUT_OF_BOUNDS, "Table error");
      assertEquals(TrapType.TABLE_OUT_OF_BOUNDS, exception.getTrapType());
    }

    @Test
    @DisplayName("getWasmBacktrace should return backtrace")
    void getWasmBacktrace() {
      String backtrace = "0: 0x1234 - <module>::func1\n1: 0x5678 - <module>::func2";
      TrapException exception =
          new TrapException(
              TrapType.UNREACHABLE_CODE_REACHED, "Unreachable", backtrace, null, null, null);
      assertEquals(backtrace, exception.getWasmBacktrace());
    }

    @Test
    @DisplayName("getFunctionName should return function name")
    void getFunctionName() {
      TrapException exception =
          new TrapException(
              TrapType.BAD_SIGNATURE, "Signature mismatch", null, "my_function", null, null);
      assertEquals("my_function", exception.getFunctionName());
    }

    @Test
    @DisplayName("getInstructionOffset should return offset")
    void getInstructionOffset() {
      TrapException exception =
          new TrapException(TrapType.HEAP_MISALIGNED, "Misaligned access", null, null, 256, null);
      assertEquals(Integer.valueOf(256), exception.getInstructionOffset());
    }

    @Test
    @DisplayName("getRecoverySuggestion should return non-null suggestion")
    void getRecoverySuggestion() {
      TrapException exception =
          new TrapException(TrapType.INTEGER_DIVISION_BY_ZERO, "Division by zero");
      assertNotNull(exception.getRecoverySuggestion());
      assertFalse(exception.getRecoverySuggestion().isEmpty());
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @ParameterizedTest
    @EnumSource(TrapType.class)
    @DisplayName("All trap types should have recovery suggestions")
    void allTrapTypesShouldHaveRecoverySuggestions(final TrapType trapType) {
      TrapException exception = new TrapException(trapType, "Test");
      assertNotNull(exception.getRecoverySuggestion());
      assertFalse(exception.getRecoverySuggestion().isEmpty());
    }

    @Test
    @DisplayName("STACK_OVERFLOW should suggest reducing recursion")
    void stackOverflowRecoverySuggestion() {
      TrapException exception = new TrapException(TrapType.STACK_OVERFLOW, "Stack overflow");
      assertTrue(
          exception.getRecoverySuggestion().toLowerCase().contains("recursion")
              || exception.getRecoverySuggestion().toLowerCase().contains("stack"));
    }

    @Test
    @DisplayName("OUT_OF_FUEL should suggest increasing fuel")
    void outOfFuelRecoverySuggestion() {
      TrapException exception = new TrapException(TrapType.OUT_OF_FUEL, "Out of fuel");
      assertTrue(exception.getRecoverySuggestion().toLowerCase().contains("fuel"));
    }
  }

  @Nested
  @DisplayName("Classification Method Tests")
  class ClassificationMethodTests {

    @Test
    @DisplayName("isMemoryError should return true for memory-related traps")
    void isMemoryError() {
      assertTrue(new TrapException(TrapType.MEMORY_OUT_OF_BOUNDS, "Test").isMemoryError());
      assertTrue(new TrapException(TrapType.HEAP_MISALIGNED, "Test").isMemoryError());
      assertTrue(new TrapException(TrapType.STACK_OVERFLOW, "Test").isMemoryError());

      assertFalse(new TrapException(TrapType.INTEGER_OVERFLOW, "Test").isMemoryError());
      assertFalse(new TrapException(TrapType.BAD_SIGNATURE, "Test").isMemoryError());
    }

    @Test
    @DisplayName("isArithmeticError should return true for arithmetic-related traps")
    void isArithmeticError() {
      assertTrue(new TrapException(TrapType.INTEGER_OVERFLOW, "Test").isArithmeticError());
      assertTrue(new TrapException(TrapType.INTEGER_DIVISION_BY_ZERO, "Test").isArithmeticError());
      assertTrue(new TrapException(TrapType.BAD_CONVERSION_TO_INTEGER, "Test").isArithmeticError());

      assertFalse(new TrapException(TrapType.MEMORY_OUT_OF_BOUNDS, "Test").isArithmeticError());
      assertFalse(new TrapException(TrapType.STACK_OVERFLOW, "Test").isArithmeticError());
    }

    @Test
    @DisplayName("isControlFlowError should return true for control flow-related traps")
    void isControlFlowError() {
      assertTrue(new TrapException(TrapType.INDIRECT_CALL_TO_NULL, "Test").isControlFlowError());
      assertTrue(new TrapException(TrapType.BAD_SIGNATURE, "Test").isControlFlowError());
      assertTrue(new TrapException(TrapType.UNREACHABLE_CODE_REACHED, "Test").isControlFlowError());
      assertTrue(new TrapException(TrapType.NULL_REFERENCE, "Test").isControlFlowError());

      assertFalse(new TrapException(TrapType.INTEGER_OVERFLOW, "Test").isControlFlowError());
      assertFalse(new TrapException(TrapType.MEMORY_OUT_OF_BOUNDS, "Test").isControlFlowError());
    }

    @Test
    @DisplayName("isResourceExhaustionError should return true for resource exhaustion traps")
    void isResourceExhaustionError() {
      assertTrue(new TrapException(TrapType.STACK_OVERFLOW, "Test").isResourceExhaustionError());
      assertTrue(new TrapException(TrapType.OUT_OF_FUEL, "Test").isResourceExhaustionError());
      assertTrue(new TrapException(TrapType.INTERRUPT, "Test").isResourceExhaustionError());

      assertFalse(new TrapException(TrapType.INTEGER_OVERFLOW, "Test").isResourceExhaustionError());
      assertFalse(new TrapException(TrapType.BAD_SIGNATURE, "Test").isResourceExhaustionError());
    }

    @Test
    @DisplayName("isBoundsError should return true for bounds checking traps")
    void isBoundsError() {
      assertTrue(new TrapException(TrapType.MEMORY_OUT_OF_BOUNDS, "Test").isBoundsError());
      assertTrue(new TrapException(TrapType.TABLE_OUT_OF_BOUNDS, "Test").isBoundsError());
      assertTrue(new TrapException(TrapType.ARRAY_OUT_OF_BOUNDS, "Test").isBoundsError());

      assertFalse(new TrapException(TrapType.INTEGER_OVERFLOW, "Test").isBoundsError());
      assertFalse(new TrapException(TrapType.BAD_SIGNATURE, "Test").isBoundsError());
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("TrapException should extend RuntimeException (package-local)")
    void shouldExtendRuntimeException() {
      TrapException exception = new TrapException(TrapType.UNKNOWN, "Test");
      assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("TrapException should be a checked exception (extends WasmException)")
    void shouldBeCheckedException() {
      TrapException exception = new TrapException(TrapType.UNKNOWN, "Test");
      assertTrue(exception instanceof Exception);
      assertTrue(exception instanceof WasmException);
      // The package-local RuntimeException extends WasmException (checked), not
      // java.lang.RuntimeException
      assertFalse(java.lang.RuntimeException.class.isAssignableFrom(exception.getClass()));
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Message should include trap type name")
    void messageShouldIncludeTrapTypeName() {
      TrapException exception =
          new TrapException(TrapType.INTEGER_DIVISION_BY_ZERO, "Division error");
      assertTrue(exception.getMessage().contains("INTEGER_DIVISION_BY_ZERO"));
    }

    @Test
    @DisplayName("Message should include function name when provided")
    void messageShouldIncludeFunctionName() {
      TrapException exception =
          new TrapException(
              TrapType.MEMORY_OUT_OF_BOUNDS, "Memory error", null, "test_function", null, null);
      assertTrue(exception.getMessage().contains("test_function"));
    }

    @Test
    @DisplayName("Message should include instruction offset when provided")
    void messageShouldIncludeInstructionOffset() {
      TrapException exception =
          new TrapException(TrapType.MEMORY_OUT_OF_BOUNDS, "Memory error", null, null, 12345, null);
      assertTrue(exception.getMessage().contains("12345"));
    }

    @Test
    @DisplayName("Message should include both function name and offset when provided")
    void messageShouldIncludeBothFunctionNameAndOffset() {
      TrapException exception =
          new TrapException(
              TrapType.MEMORY_OUT_OF_BOUNDS, "Memory error", null, "my_func", 99, null);
      assertTrue(exception.getMessage().contains("my_func"));
      assertTrue(exception.getMessage().contains("99"));
    }
  }
}
