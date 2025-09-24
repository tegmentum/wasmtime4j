package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.MultiValueException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for multi-value WebAssembly function support.
 *
 * <p>Tests cover the WebAssembly multi-value proposal functionality including: - Multi-value
 * function signatures and validation - Host function multi-value returns - Parameter and return
 * value marshaling - Error handling for multi-value operations - Performance characteristics
 */
@DisplayName("Multi-Value Function Operations")
class MultiValueFunctionTest {

  @Nested
  @DisplayName("WasmValue Multi-Value Operations")
  class WasmValueMultiValueTest {

    @Test
    @DisplayName("Should create multi-value arrays correctly")
    void testMultiValueCreation() {
      // Test empty multi-value
      WasmValue[] empty = WasmValue.multiValue();
      assertNotNull(empty);
      assertEquals(0, empty.length);

      // Test single value
      WasmValue[] single = WasmValue.multiValue(WasmValue.i32(42));
      assertNotNull(single);
      assertEquals(1, single.length);
      assertEquals(42, single[0].asI32());

      // Test multiple values
      WasmValue[] multiple =
          WasmValue.multiValue(WasmValue.i32(10), WasmValue.f64(3.14), WasmValue.i64(1000L));
      assertNotNull(multiple);
      assertEquals(3, multiple.length);
      assertEquals(10, multiple[0].asI32());
      assertEquals(3.14, multiple[1].asF64(), 0.001);
      assertEquals(1000L, multiple[2].asI64());
    }

    @Test
    @DisplayName("Should detect multi-value arrays correctly")
    void testMultiValueDetection() {
      assertFalse(WasmValue.isMultiValue(null));
      assertFalse(WasmValue.isMultiValue(new WasmValue[0]));
      assertFalse(WasmValue.isMultiValue(new WasmValue[] {WasmValue.i32(1)}));

      assertTrue(WasmValue.isMultiValue(new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)}));

      assertTrue(
          WasmValue.isMultiValue(
              new WasmValue[] {WasmValue.i32(1), WasmValue.f64(2.0), WasmValue.i64(3L)}));
    }

    @Test
    @DisplayName("Should get first and last values correctly")
    void testFirstAndLastValues() {
      // Test null array
      assertNull(WasmValue.getFirstValue(null));
      assertNull(WasmValue.getLastValue(null));

      // Test empty array
      assertNull(WasmValue.getFirstValue(new WasmValue[0]));
      assertNull(WasmValue.getLastValue(new WasmValue[0]));

      // Test single value
      WasmValue[] single = {WasmValue.i32(42)};
      assertEquals(42, WasmValue.getFirstValue(single).asI32());
      assertEquals(42, WasmValue.getLastValue(single).asI32());

      // Test multiple values
      WasmValue[] multiple = {WasmValue.i32(10), WasmValue.f64(3.14), WasmValue.i64(1000L)};
      assertEquals(10, WasmValue.getFirstValue(multiple).asI32());
      assertEquals(1000L, WasmValue.getLastValue(multiple).asI64());
    }

    @Test
    @DisplayName("Should extract values by type correctly")
    void testExtractByType() {
      WasmValue[] values = {
        WasmValue.i32(10),
        WasmValue.f64(3.14),
        WasmValue.i32(20),
        WasmValue.i64(1000L),
        WasmValue.i32(30)
      };

      // Extract I32 values
      WasmValue[] i32Values = WasmValue.extractByType(values, WasmValueType.I32);
      assertEquals(3, i32Values.length);
      assertEquals(10, i32Values[0].asI32());
      assertEquals(20, i32Values[1].asI32());
      assertEquals(30, i32Values[2].asI32());

      // Extract F64 values
      WasmValue[] f64Values = WasmValue.extractByType(values, WasmValueType.F64);
      assertEquals(1, f64Values.length);
      assertEquals(3.14, f64Values[0].asF64(), 0.001);

      // Extract non-existent type
      WasmValue[] f32Values = WasmValue.extractByType(values, WasmValueType.F32);
      assertEquals(0, f32Values.length);

      // Test with null inputs
      assertEquals(0, WasmValue.extractByType(null, WasmValueType.I32).length);
      assertEquals(0, WasmValue.extractByType(values, null).length);
    }

    @Test
    @DisplayName("Should validate multi-value arrays correctly")
    void testMultiValueValidation() {
      WasmValue[] values = {WasmValue.i32(10), WasmValue.f64(3.14), WasmValue.i64(1000L)};

      WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64, WasmValueType.I64};

      // Valid case should not throw
      WasmValue.validateMultiValue(values, expectedTypes);

      // Test count mismatch
      WasmValueType[] wrongCount = {WasmValueType.I32, WasmValueType.F64};
      assertThrows(
          IllegalArgumentException.class, () -> WasmValue.validateMultiValue(values, wrongCount));

      // Test type mismatch
      WasmValueType[] wrongTypes = {
        WasmValueType.I32,
        WasmValueType.I32, // Should be F64
        WasmValueType.I64
      };
      assertThrows(
          IllegalArgumentException.class, () -> WasmValue.validateMultiValue(values, wrongTypes));

      // Test null inputs
      assertThrows(
          IllegalArgumentException.class, () -> WasmValue.validateMultiValue(null, expectedTypes));
      assertThrows(
          IllegalArgumentException.class, () -> WasmValue.validateMultiValue(values, null));
    }

    @Test
    @DisplayName("Should validate multi-value limits")
    void testMultiValueLimits() {
      // Valid case - within limits
      WasmValue[] validArray = new WasmValue[10];
      for (int i = 0; i < 10; i++) {
        validArray[i] = WasmValue.i32(i);
      }
      WasmValue.validateMultiValueLimits(validArray);

      // Edge case - exactly at limit
      WasmValue[] atLimit = new WasmValue[16];
      for (int i = 0; i < 16; i++) {
        atLimit[i] = WasmValue.i32(i);
      }
      WasmValue.validateMultiValueLimits(atLimit);

      // Invalid case - exceeds limit
      WasmValue[] exceedsLimit = new WasmValue[17];
      for (int i = 0; i < 17; i++) {
        exceedsLimit[i] = WasmValue.i32(i);
      }
      MultiValueException exception =
          assertThrows(
              MultiValueException.class, () -> WasmValue.validateMultiValueLimits(exceedsLimit));

      assertEquals(17, exception.getActualCount());
      assertEquals(16, exception.getExpectedCount());
      assertTrue(exception.hasCountInfo());
    }

    @Test
    @DisplayName("Should handle multi-value string representation")
    void testMultiValueToString() {
      // Test null
      assertEquals("null", WasmValue.multiValueToString(null));

      // Test empty
      assertEquals("[]", WasmValue.multiValueToString(new WasmValue[0]));

      // Test single value
      WasmValue[] single = {WasmValue.i32(42)};
      String singleStr = WasmValue.multiValueToString(single);
      assertTrue(singleStr.contains("42"));
      assertTrue(singleStr.startsWith("["));
      assertTrue(singleStr.endsWith("]"));

      // Test multiple values
      WasmValue[] multiple = {WasmValue.i32(10), WasmValue.f64(3.14)};
      String multiStr = WasmValue.multiValueToString(multiple);
      assertTrue(multiStr.contains("10"));
      assertTrue(multiStr.contains("3.14"));
      assertTrue(multiStr.contains(","));
    }

    @Test
    @DisplayName("Should copy multi-value arrays correctly")
    void testMultiValueCopy() {
      // Test null
      assertNull(WasmValue.copyMultiValue(null));

      // Test copying
      WasmValue[] original = {WasmValue.i32(10), WasmValue.f64(3.14), WasmValue.i64(1000L)};

      WasmValue[] copy = WasmValue.copyMultiValue(original);
      assertNotNull(copy);
      assertEquals(original.length, copy.length);

      // Verify values are the same
      for (int i = 0; i < original.length; i++) {
        assertEquals(original[i].getType(), copy[i].getType());
        assertEquals(original[i].getValue(), copy[i].getValue());
      }

      // Verify it's a different array instance
      assertTrue(original != copy);
    }
  }

  @Nested
  @DisplayName("FunctionType Multi-Value Support")
  class FunctionTypeMultiValueTest {

    @Test
    @DisplayName("Should create multi-value function types")
    void testMultiValueFunctionTypeCreation() {
      // Test multi-value factory method
      WasmValueType[] params = {WasmValueType.I32, WasmValueType.F64};
      WasmValueType[] returns = {WasmValueType.I32, WasmValueType.I64, WasmValueType.F32};

      FunctionType funcType = FunctionType.multiValue(params, returns);
      assertNotNull(funcType);
      assertEquals(2, funcType.getParamCount());
      assertEquals(3, funcType.getReturnCount());
      assertTrue(funcType.hasMultipleReturns());

      // Test no params, multiple returns
      FunctionType noParamsMultiReturns =
          FunctionType.multiValueNoParams(WasmValueType.I32, WasmValueType.F64);
      assertEquals(0, noParamsMultiReturns.getParamCount());
      assertEquals(2, noParamsMultiReturns.getReturnCount());
      assertTrue(noParamsMultiReturns.hasMultipleReturns());

      // Test multiple params, no returns
      FunctionType multiParamsNoReturns =
          FunctionType.multiValueNoReturns(WasmValueType.I32, WasmValueType.F64);
      assertEquals(2, multiParamsNoReturns.getParamCount());
      assertEquals(0, multiParamsNoReturns.getReturnCount());
      assertFalse(multiParamsNoReturns.hasMultipleReturns());
    }

    @Test
    @DisplayName("Should validate return values correctly")
    void testReturnValueValidation() {
      WasmValueType[] returnTypes = {WasmValueType.I32, WasmValueType.F64};
      FunctionType funcType = new FunctionType(new WasmValueType[0], returnTypes);

      // Valid return values
      WasmValue[] validReturns = {WasmValue.i32(42), WasmValue.f64(3.14)};
      funcType.validateReturnValues(validReturns);

      // Invalid count
      WasmValue[] wrongCount = {WasmValue.i32(42)};
      assertThrows(IllegalArgumentException.class, () -> funcType.validateReturnValues(wrongCount));

      // Invalid types
      WasmValue[] wrongTypes = {
        WasmValue.f64(3.14), // Should be I32
        WasmValue.i32(42) // Should be F64
      };
      assertThrows(IllegalArgumentException.class, () -> funcType.validateReturnValues(wrongTypes));
    }

    @Test
    @DisplayName("Should match multi-value patterns correctly")
    void testMultiValuePatternMatching() {
      WasmValueType[] params = {WasmValueType.I32, WasmValueType.F64};
      WasmValueType[] returns = {WasmValueType.I32, WasmValueType.I64};
      FunctionType funcType = new FunctionType(params, returns);

      // Exact match
      assertTrue(funcType.matchesMultiValuePattern(params, returns));

      // Different params
      WasmValueType[] differentParams = {WasmValueType.I64, WasmValueType.F64};
      assertFalse(funcType.matchesMultiValuePattern(differentParams, returns));

      // Different returns
      WasmValueType[] differentReturns = {WasmValueType.F32, WasmValueType.I64};
      assertFalse(funcType.matchesMultiValuePattern(params, differentReturns));

      // Different counts
      WasmValueType[] shorterParams = {WasmValueType.I32};
      assertFalse(funcType.matchesMultiValuePattern(shorterParams, returns));

      // Null inputs
      assertFalse(funcType.matchesMultiValuePattern(null, returns));
      assertFalse(funcType.matchesMultiValuePattern(params, null));
    }

    @Test
    @DisplayName("Should validate multi-value limits for function types")
    void testFunctionTypeMultiValueLimits() {
      // Valid function type within limits
      WasmValueType[] validParams = new WasmValueType[8];
      WasmValueType[] validReturns = new WasmValueType[8];
      for (int i = 0; i < 8; i++) {
        validParams[i] = WasmValueType.I32;
        validReturns[i] = WasmValueType.I32;
      }

      FunctionType validType = new FunctionType(validParams, validReturns);
      validType.validateMultiValueLimits(); // Should not throw

      // Too many parameters
      WasmValueType[] tooManyParams = new WasmValueType[17];
      for (int i = 0; i < 17; i++) {
        tooManyParams[i] = WasmValueType.I32;
      }

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            FunctionType invalidType = new FunctionType(tooManyParams, new WasmValueType[0]);
            invalidType.validateMultiValueLimits();
          });

      // Too many returns
      WasmValueType[] tooManyReturns = new WasmValueType[17];
      for (int i = 0; i < 17; i++) {
        tooManyReturns[i] = WasmValueType.I32;
      }

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            FunctionType invalidType = new FunctionType(new WasmValueType[0], tooManyReturns);
            invalidType.validateMultiValueLimits();
          });
    }
  }

  @Nested
  @DisplayName("HostFunction Multi-Value Support")
  class HostFunctionMultiValueTest {

    @Test
    @DisplayName("Should create void host functions")
    void testVoidHostFunction() {
      final boolean[] executed = {false};

      HostFunction voidFunc =
          HostFunction.voidFunction(
              (params) -> {
                executed[0] = true;
                assertEquals(1, params.length);
                assertEquals(42, params[0].asI32());
              });

      try {
        WasmValue[] result = voidFunc.execute(new WasmValue[] {WasmValue.i32(42)});
        assertNotNull(result);
        assertEquals(0, result.length);
        assertTrue(executed[0]);
      } catch (WasmException e) {
        throw new RuntimeException(e);
      }
    }

    @Test
    @DisplayName("Should create single-value host functions")
    void testSingleValueHostFunction() {
      HostFunction singleFunc =
          HostFunction.singleValue(
              (params) -> {
                assertEquals(2, params.length);
                int a = params[0].asI32();
                int b = params[1].asI32();
                return WasmValue.i32(a + b);
              });

      try {
        WasmValue[] result =
            singleFunc.execute(new WasmValue[] {WasmValue.i32(10), WasmValue.i32(20)});
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(30, result[0].asI32());
      } catch (WasmException e) {
        throw new RuntimeException(e);
      }
    }

    @Test
    @DisplayName("Should create multi-value host functions")
    void testMultiValueHostFunction() {
      HostFunction multiFunc =
          HostFunction.multiValue(
              (params) -> {
                assertEquals(2, params.length);
                int a = params[0].asI32();
                int b = params[1].asI32();
                return WasmValue.multiValue(
                    WasmValue.i32(a + b), // sum
                    WasmValue.i32(a - b), // difference
                    WasmValue.i32(a * b) // product
                    );
              });

      try {
        WasmValue[] result =
            multiFunc.execute(new WasmValue[] {WasmValue.i32(10), WasmValue.i32(3)});
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(13, result[0].asI32()); // sum
        assertEquals(7, result[1].asI32()); // difference
        assertEquals(30, result[2].asI32()); // product
      } catch (WasmException e) {
        throw new RuntimeException(e);
      }
    }

    @Test
    @DisplayName("Should create host functions with validation")
    void testHostFunctionWithValidation() {
      HostFunction baseFunc =
          (params) -> WasmValue.multiValue(WasmValue.i32(42), WasmValue.f64(3.14));

      HostFunction validatedFunc =
          HostFunction.withValidation(baseFunc, WasmValueType.I32, WasmValueType.F64);

      try {
        WasmValue[] result = validatedFunc.execute(new WasmValue[0]);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(42, result[0].asI32());
        assertEquals(3.14, result[1].asF64(), 0.001);
      } catch (WasmException e) {
        throw new RuntimeException(e);
      }

      // Test validation failure
      HostFunction invalidFunc =
          (params) ->
              WasmValue.multiValue(
                  WasmValue.f64(3.14), // Wrong type - should be I32
                  WasmValue.i32(42) // Wrong type - should be F64
                  );

      HostFunction validatedInvalidFunc =
          HostFunction.withValidation(invalidFunc, WasmValueType.I32, WasmValueType.F64);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            try {
              validatedInvalidFunc.execute(new WasmValue[0]);
            } catch (WasmException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  @Nested
  @DisplayName("MultiValueException Tests")
  class MultiValueExceptionTest {

    @Test
    @DisplayName("Should create count mismatch exceptions")
    void testCountMismatchExceptions() {
      MultiValueException exception = MultiValueException.countMismatch(3, 2);

      assertNotNull(exception);
      assertEquals(3, exception.getExpectedCount());
      assertEquals(2, exception.getActualCount());
      assertTrue(exception.hasCountInfo());
      assertFalse(exception.hasOperationInfo());
      assertTrue(exception.getMessage().contains("expected 3"));
      assertTrue(exception.getMessage().contains("got 2"));

      // With operation
      MultiValueException withOp = MultiValueException.countMismatch(3, 2, "function_call");
      assertEquals("function_call", withOp.getOperation());
      assertTrue(withOp.hasOperationInfo());
      assertTrue(withOp.getMessage().contains("function_call"));
    }

    @Test
    @DisplayName("Should create type validation exceptions")
    void testTypeValidationExceptions() {
      MultiValueException exception = MultiValueException.typeValidationError(1, "I32", "F64");

      assertNotNull(exception);
      assertTrue(exception.getMessage().contains("index 1"));
      assertTrue(exception.getMessage().contains("I32"));
      assertTrue(exception.getMessage().contains("F64"));
    }

    @Test
    @DisplayName("Should create marshaling exceptions")
    void testMarshalingExceptions() {
      RuntimeException cause = new RuntimeException("Native marshaling failed");
      MultiValueException exception =
          MultiValueException.marshalingError("parameter_marshaling", 5, cause);

      assertNotNull(exception);
      assertEquals("parameter_marshaling", exception.getOperation());
      assertEquals(5, exception.getActualCount());
      assertEquals(-1, exception.getExpectedCount());
      assertEquals(cause, exception.getCause());
      assertTrue(exception.getMessage().contains("parameter_marshaling"));
      assertTrue(exception.getMessage().contains("5 values"));
    }

    @Test
    @DisplayName("Should create limit exceeded exceptions")
    void testLimitExceededException() {
      MultiValueException exception = MultiValueException.limitExceeded(20, 16);

      assertNotNull(exception);
      assertEquals(16, exception.getExpectedCount());
      assertEquals(20, exception.getActualCount());
      assertTrue(exception.hasCountInfo());
      assertTrue(exception.getMessage().contains("20 values"));
      assertTrue(exception.getMessage().contains("max allowed: 16"));
    }

    @Test
    @DisplayName("Should create invalid value array exceptions")
    void testInvalidValueArrayException() {
      MultiValueException exception = MultiValueException.invalidValueArray("validation");

      assertNotNull(exception);
      assertEquals("validation", exception.getOperation());
      assertTrue(exception.hasOperationInfo());
      assertFalse(exception.hasCountInfo());
      assertTrue(exception.getMessage().contains("validation"));
    }

    @Test
    @DisplayName("Should format toString correctly")
    void testToStringFormatting() {
      MultiValueException simpleException = new MultiValueException("Simple error");
      assertTrue(simpleException.toString().contains("Simple error"));
      assertTrue(simpleException.toString().startsWith("MultiValueException"));

      MultiValueException fullException =
          new MultiValueException("Full error", 3, 2, "test_operation");
      String fullStr = fullException.toString();
      assertTrue(fullStr.contains("Full error"));
      assertTrue(fullStr.contains("expected: 3"));
      assertTrue(fullStr.contains("actual: 2"));
      assertTrue(fullStr.contains("operation: test_operation"));
    }
  }
}
