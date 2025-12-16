package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmtimeException.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmtimeException} class.
 *
 * <p>This test class verifies the construction and behavior of Wasmtime-specific exceptions,
 * including error codes and native stack traces.
 */
@DisplayName("WasmtimeException Tests")
class WasmtimeExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmtimeException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(WasmtimeException.class),
          "WasmtimeException should extend WasmException");
    }

    @Test
    @DisplayName("WasmtimeException should be a checked exception")
    void shouldBeCheckedException() {
      WasmtimeException exception = new WasmtimeException("Test");

      assertTrue(exception instanceof Exception, "WasmtimeException should be an Exception");
      assertFalse(
          java.lang.RuntimeException.class.isAssignableFrom(exception.getClass()),
          "WasmtimeException should not extend RuntimeException");
    }

    @Test
    @DisplayName("WasmtimeException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WasmtimeException.class),
          "WasmtimeException should be serializable");
    }
  }

  @Nested
  @DisplayName("ErrorCode Enum Tests")
  class ErrorCodeEnumTests {

    @Test
    @DisplayName("ErrorCode should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(ErrorCode.valueOf("UNKNOWN"), "ErrorCode should have UNKNOWN value");
    }

    @Test
    @DisplayName("ErrorCode should have COMPILATION_FAILED value")
    void shouldHaveCompilationFailedValue() {
      assertNotNull(
          ErrorCode.valueOf("COMPILATION_FAILED"),
          "ErrorCode should have COMPILATION_FAILED value");
    }

    @Test
    @DisplayName("ErrorCode should have INSTANTIATION_FAILED value")
    void shouldHaveInstantiationFailedValue() {
      assertNotNull(
          ErrorCode.valueOf("INSTANTIATION_FAILED"),
          "ErrorCode should have INSTANTIATION_FAILED value");
    }

    @Test
    @DisplayName("ErrorCode should have FUNCTION_CALL_FAILED value")
    void shouldHaveFunctionCallFailedValue() {
      assertNotNull(
          ErrorCode.valueOf("FUNCTION_CALL_FAILED"),
          "ErrorCode should have FUNCTION_CALL_FAILED value");
    }

    @Test
    @DisplayName("ErrorCode should have MEMORY_ACCESS_VIOLATION value")
    void shouldHaveMemoryAccessViolationValue() {
      assertNotNull(
          ErrorCode.valueOf("MEMORY_ACCESS_VIOLATION"),
          "ErrorCode should have MEMORY_ACCESS_VIOLATION value");
    }

    @Test
    @DisplayName("ErrorCode should have STACK_OVERFLOW value")
    void shouldHaveStackOverflowValue() {
      assertNotNull(
          ErrorCode.valueOf("STACK_OVERFLOW"), "ErrorCode should have STACK_OVERFLOW value");
    }

    @Test
    @DisplayName("ErrorCode should have TRAP value")
    void shouldHaveTrapValue() {
      assertNotNull(ErrorCode.valueOf("TRAP"), "ErrorCode should have TRAP value");
    }

    @Test
    @DisplayName("ErrorCode should have RESOURCE_EXHAUSTED value")
    void shouldHaveResourceExhaustedValue() {
      assertNotNull(
          ErrorCode.valueOf("RESOURCE_EXHAUSTED"),
          "ErrorCode should have RESOURCE_EXHAUSTED value");
    }

    @Test
    @DisplayName("ErrorCode should have INVALID_CONFIGURATION value")
    void shouldHaveInvalidConfigurationValue() {
      assertNotNull(
          ErrorCode.valueOf("INVALID_CONFIGURATION"),
          "ErrorCode should have INVALID_CONFIGURATION value");
    }

    @Test
    @DisplayName("ErrorCode should have UNSUPPORTED_OPERATION value")
    void shouldHaveUnsupportedOperationValue() {
      assertNotNull(
          ErrorCode.valueOf("UNSUPPORTED_OPERATION"),
          "ErrorCode should have UNSUPPORTED_OPERATION value");
    }

    @Test
    @DisplayName("ErrorCode should have NATIVE_LIBRARY_ERROR value")
    void shouldHaveNativeLibraryErrorValue() {
      assertNotNull(
          ErrorCode.valueOf("NATIVE_LIBRARY_ERROR"),
          "ErrorCode should have NATIVE_LIBRARY_ERROR value");
    }

    @Test
    @DisplayName("ErrorCode should have THREADING_ERROR value")
    void shouldHaveThreadingErrorValue() {
      assertNotNull(
          ErrorCode.valueOf("THREADING_ERROR"), "ErrorCode should have THREADING_ERROR value");
    }

    @Test
    @DisplayName("ErrorCode should have WASI_ERROR value")
    void shouldHaveWasiErrorValue() {
      assertNotNull(ErrorCode.valueOf("WASI_ERROR"), "ErrorCode should have WASI_ERROR value");
    }

    @Test
    @DisplayName("ErrorCode should have VALIDATION_ERROR value")
    void shouldHaveValidationErrorValue() {
      assertNotNull(
          ErrorCode.valueOf("VALIDATION_ERROR"), "ErrorCode should have VALIDATION_ERROR value");
    }

    @Test
    @DisplayName("ErrorCode should have LINKING_ERROR value")
    void shouldHaveLinkingErrorValue() {
      assertNotNull(
          ErrorCode.valueOf("LINKING_ERROR"), "ErrorCode should have LINKING_ERROR value");
    }

    @Test
    @DisplayName("ErrorCode should have FUEL_EXHAUSTED value")
    void shouldHaveFuelExhaustedValue() {
      assertNotNull(
          ErrorCode.valueOf("FUEL_EXHAUSTED"), "ErrorCode should have FUEL_EXHAUSTED value");
    }

    @Test
    @DisplayName("ErrorCode should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(ErrorCode.valueOf("TIMEOUT"), "ErrorCode should have TIMEOUT value");
    }

    @Test
    @DisplayName("ErrorCode should have INTERRUPTED value")
    void shouldHaveInterruptedValue() {
      assertNotNull(ErrorCode.valueOf("INTERRUPTED"), "ErrorCode should have INTERRUPTED value");
    }

    @Test
    @DisplayName("ErrorCode should have 18 values")
    void shouldHave18Values() {
      assertEquals(18, ErrorCode.values().length, "ErrorCode should have exactly 18 values");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message only should set message and default error code")
    void constructorWithMessageOnly() {
      WasmtimeException exception = new WasmtimeException("Test error");

      assertEquals("Test error", exception.getMessage(), "Message should be set");
      assertEquals(
          ErrorCode.UNKNOWN, exception.getErrorCode(), "Error code should default to UNKNOWN");
      assertNull(exception.getCause(), "Cause should be null");
      assertNull(exception.getNativeStackTrace(), "Native stack trace should be null");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCause() {
      Throwable cause = new RuntimeException("Root cause");
      WasmtimeException exception = new WasmtimeException("Test error", cause);

      assertEquals("Test error", exception.getMessage(), "Message should be set");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          ErrorCode.UNKNOWN, exception.getErrorCode(), "Error code should default to UNKNOWN");
    }

    @Test
    @DisplayName("Constructor with message and error code should set both")
    void constructorWithMessageAndErrorCode() {
      WasmtimeException exception =
          new WasmtimeException("Compilation failed", ErrorCode.COMPILATION_FAILED);

      assertEquals("Compilation failed", exception.getMessage(), "Message should be set");
      assertEquals(
          ErrorCode.COMPILATION_FAILED,
          exception.getErrorCode(),
          "Error code should be COMPILATION_FAILED");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with full details should set all fields")
    void constructorWithFullDetails() {
      Throwable cause = new RuntimeException("Root cause");
      String nativeTrace = "0: wasmtime::runtime::func\n1: wasmtime::runtime::call";
      WasmtimeException exception =
          new WasmtimeException("Trap occurred", cause, ErrorCode.TRAP, nativeTrace);

      assertEquals("Trap occurred", exception.getMessage(), "Message should be set");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(ErrorCode.TRAP, exception.getErrorCode(), "Error code should be TRAP");
      assertEquals(
          nativeTrace, exception.getNativeStackTrace(), "Native stack trace should be set");
    }

    @Test
    @DisplayName("Constructor should use UNKNOWN for null error code")
    void constructorShouldUseUnknownForNullErrorCode() {
      WasmtimeException exception = new WasmtimeException("Error", null, null, null);

      assertEquals(
          ErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN when null is provided");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getErrorCode should return the error code")
    void getErrorCodeShouldReturnErrorCode() {
      WasmtimeException exception =
          new WasmtimeException("Memory error", ErrorCode.MEMORY_ACCESS_VIOLATION);

      assertEquals(
          ErrorCode.MEMORY_ACCESS_VIOLATION,
          exception.getErrorCode(),
          "getErrorCode should return MEMORY_ACCESS_VIOLATION");
    }

    @Test
    @DisplayName("getNativeStackTrace should return native trace when set")
    void getNativeStackTraceShouldReturnTrace() {
      String trace = "native stack trace here";
      WasmtimeException exception = new WasmtimeException("Error", null, ErrorCode.UNKNOWN, trace);

      assertEquals(
          trace, exception.getNativeStackTrace(), "getNativeStackTrace should return the trace");
    }

    @Test
    @DisplayName("getNativeStackTrace should return null when not set")
    void getNativeStackTraceShouldReturnNullWhenNotSet() {
      WasmtimeException exception = new WasmtimeException("Error");

      assertNull(
          exception.getNativeStackTrace(), "getNativeStackTrace should return null when not set");
    }

    @Test
    @DisplayName("hasNativeStackTrace should return true when trace is set")
    void hasNativeStackTraceShouldReturnTrueWhenSet() {
      WasmtimeException exception =
          new WasmtimeException("Error", null, ErrorCode.UNKNOWN, "trace");

      assertTrue(
          exception.hasNativeStackTrace(),
          "hasNativeStackTrace should return true when trace is set");
    }

    @Test
    @DisplayName("hasNativeStackTrace should return false when trace is null")
    void hasNativeStackTraceShouldReturnFalseWhenNull() {
      WasmtimeException exception = new WasmtimeException("Error");

      assertFalse(
          exception.hasNativeStackTrace(),
          "hasNativeStackTrace should return false when trace is null");
    }

    @Test
    @DisplayName("hasNativeStackTrace should return false when trace is empty")
    void hasNativeStackTraceShouldReturnFalseWhenEmpty() {
      WasmtimeException exception = new WasmtimeException("Error", null, ErrorCode.UNKNOWN, "");

      assertFalse(
          exception.hasNativeStackTrace(),
          "hasNativeStackTrace should return false when trace is empty");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include class name")
    void toStringShouldIncludeClassName() {
      WasmtimeException exception = new WasmtimeException("Test error");

      assertTrue(
          exception.toString().contains("WasmtimeException"), "toString should include class name");
    }

    @Test
    @DisplayName("toString should include error code")
    void toStringShouldIncludeErrorCode() {
      WasmtimeException exception = new WasmtimeException("Test error", ErrorCode.TRAP);

      assertTrue(exception.toString().contains("TRAP"), "toString should include error code");
    }

    @Test
    @DisplayName("toString should include message")
    void toStringShouldIncludeMessage() {
      WasmtimeException exception = new WasmtimeException("My error message", ErrorCode.UNKNOWN);

      assertTrue(
          exception.toString().contains("My error message"), "toString should include message");
    }

    @Test
    @DisplayName("toString should handle null message")
    void toStringShouldHandleNullMessage() {
      WasmtimeException exception = new WasmtimeException(null, null, ErrorCode.UNKNOWN, null);

      String result = exception.toString();
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("WasmtimeException"), "toString should still include class name");
    }
  }

  @Nested
  @DisplayName("Error Code Usage Tests")
  class ErrorCodeUsageTests {

    @Test
    @DisplayName("COMPILATION_FAILED should be used for compilation errors")
    void compilationFailedShouldBeUsedForCompilationErrors() {
      WasmtimeException exception =
          new WasmtimeException("Invalid WebAssembly bytecode", ErrorCode.COMPILATION_FAILED);

      assertEquals(
          ErrorCode.COMPILATION_FAILED,
          exception.getErrorCode(),
          "COMPILATION_FAILED should be used for compilation errors");
    }

    @Test
    @DisplayName("INSTANTIATION_FAILED should be used for instantiation errors")
    void instantiationFailedShouldBeUsedForInstantiationErrors() {
      WasmtimeException exception =
          new WasmtimeException("Missing import", ErrorCode.INSTANTIATION_FAILED);

      assertEquals(
          ErrorCode.INSTANTIATION_FAILED,
          exception.getErrorCode(),
          "INSTANTIATION_FAILED should be used for instantiation errors");
    }

    @Test
    @DisplayName("TRAP should be used for trap errors")
    void trapShouldBeUsedForTrapErrors() {
      WasmtimeException exception =
          new WasmtimeException("Out of bounds memory access", ErrorCode.TRAP);

      assertEquals(ErrorCode.TRAP, exception.getErrorCode(), "TRAP should be used for trap errors");
    }

    @Test
    @DisplayName("FUEL_EXHAUSTED should be used for fuel exhaustion")
    void fuelExhaustedShouldBeUsedForFuelExhaustion() {
      WasmtimeException exception =
          new WasmtimeException("Execution ran out of fuel", ErrorCode.FUEL_EXHAUSTED);

      assertEquals(
          ErrorCode.FUEL_EXHAUSTED,
          exception.getErrorCode(),
          "FUEL_EXHAUSTED should be used for fuel exhaustion");
    }

    @Test
    @DisplayName("TIMEOUT should be used for timeout errors")
    void timeoutShouldBeUsedForTimeoutErrors() {
      WasmtimeException exception = new WasmtimeException("Execution timed out", ErrorCode.TIMEOUT);

      assertEquals(
          ErrorCode.TIMEOUT, exception.getErrorCode(), "TIMEOUT should be used for timeout errors");
    }
  }

  @Nested
  @DisplayName("Exception Chaining Tests")
  class ExceptionChainingTests {

    @Test
    @DisplayName("Should support exception chaining with error code")
    void shouldSupportExceptionChainingWithErrorCode() {
      Exception rootCause = new Exception("Native error");
      WasmtimeException wasmtimeException =
          new WasmtimeException("Wasmtime error", rootCause, ErrorCode.NATIVE_LIBRARY_ERROR, null);

      assertSame(rootCause, wasmtimeException.getCause(), "Cause should be the root exception");
      assertEquals(
          ErrorCode.NATIVE_LIBRARY_ERROR,
          wasmtimeException.getErrorCode(),
          "Error code should be preserved");
    }

    @Test
    @DisplayName("Should preserve native stack trace through chaining")
    void shouldPreserveNativeStackTrace() {
      String nativeTrace = "0: wasmtime::func1\n1: wasmtime::func2";
      WasmtimeException exception =
          new WasmtimeException(
              "Error", new RuntimeException("Cause"), ErrorCode.TRAP, nativeTrace);

      assertEquals(
          nativeTrace, exception.getNativeStackTrace(), "Native stack trace should be preserved");
    }
  }
}
