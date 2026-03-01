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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HostFunction} interface.
 *
 * <p>HostFunction represents a host function that can be called from WebAssembly.
 */
@DisplayName("HostFunction Tests")
class HostFunctionTest {

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have voidFunction static method")
    void shouldHaveVoidFunctionMethod() throws NoSuchMethodException {
      final Method method =
          HostFunction.class.getMethod("voidFunction", HostFunction.VoidHostFunction.class);
      assertNotNull(method, "voidFunction method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "voidFunction should be static");
      assertEquals(
          HostFunction.class, method.getReturnType(), "voidFunction should return HostFunction");
    }

    @Test
    @DisplayName("should have singleValue static method")
    void shouldHaveSingleValueMethod() throws NoSuchMethodException {
      final Method method =
          HostFunction.class.getMethod("singleValue", HostFunction.SingleValueHostFunction.class);
      assertNotNull(method, "singleValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "singleValue should be static");
      assertEquals(
          HostFunction.class, method.getReturnType(), "singleValue should return HostFunction");
    }

    @Test
    @DisplayName("should have multiValue static method")
    void shouldHaveMultiValueMethod() throws NoSuchMethodException {
      final Method method =
          HostFunction.class.getMethod("multiValue", HostFunction.MultiValueHostFunction.class);
      assertNotNull(method, "multiValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "multiValue should be static");
      assertEquals(
          HostFunction.class, method.getReturnType(), "multiValue should return HostFunction");
    }

    @Test
    @DisplayName("should have withValidation static method")
    void shouldHaveWithValidationMethod() throws NoSuchMethodException {
      final Method method =
          HostFunction.class.getMethod("withValidation", HostFunction.class, WasmValueType[].class);
      assertNotNull(method, "withValidation method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "withValidation should be static");
      assertEquals(
          HostFunction.class, method.getReturnType(), "withValidation should return HostFunction");
    }

    @Test
    @DisplayName("should have withFullValidation static method")
    void shouldHaveWithFullValidationMethod() throws NoSuchMethodException {
      final Method method =
          HostFunction.class.getMethod(
              "withFullValidation",
              HostFunction.class,
              WasmValueType[].class,
              WasmValueType[].class);
      assertNotNull(method, "withFullValidation method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "withFullValidation should be static");
      assertEquals(
          HostFunction.class,
          method.getReturnType(),
          "withFullValidation should return HostFunction");
    }
  }

  @Nested
  @DisplayName("voidFunction Factory Tests")
  class VoidFunctionFactoryTests {

    @Test
    @DisplayName("voidFunction should return empty array")
    void voidFunctionShouldReturnEmptyArray() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.voidFunction(
              params -> {
                // Do nothing
              });

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty array");
    }

    @Test
    @DisplayName("voidFunction should receive parameters")
    void voidFunctionShouldReceiveParameters() throws WasmException {
      final int[] receivedValue = {0};

      final HostFunction hostFunc =
          HostFunction.voidFunction(
              params -> {
                receivedValue[0] = params[0].asInt();
              });

      final WasmValue[] params = {WasmValue.i32(42)};
      hostFunc.execute(params);

      assertEquals(42, receivedValue[0], "Should receive parameter value");
    }
  }

  @Nested
  @DisplayName("singleValue Factory Tests")
  class SingleValueFactoryTests {

    @Test
    @DisplayName("singleValue should return single value")
    void singleValueShouldReturnSingleValue() throws WasmException {
      final HostFunction hostFunc = HostFunction.singleValue(params -> WasmValue.i32(100));

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Result should have one element");
      assertEquals(100, result[0].asInt(), "Result should be 100");
    }

    @Test
    @DisplayName("singleValue should handle null return")
    void singleValueShouldHandleNullReturn() throws WasmException {
      final HostFunction hostFunc = HostFunction.singleValue(params -> null);

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty when null is returned");
    }

    @Test
    @DisplayName("singleValue should process parameters")
    void singleValueShouldProcessParameters() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.singleValue(
              params -> {
                final int a = params[0].asInt();
                final int b = params[1].asInt();
                return WasmValue.i32(a + b);
              });

      final WasmValue[] params = {WasmValue.i32(10), WasmValue.i32(20)};
      final WasmValue[] result = hostFunc.execute(params);

      assertEquals(30, result[0].asInt(), "Should compute sum of parameters");
    }
  }

  @Nested
  @DisplayName("multiValue Factory Tests")
  class MultiValueFactoryTests {

    @Test
    @DisplayName("multiValue should return multiple values")
    void multiValueShouldReturnMultipleValues() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.multiValue(
              params -> new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)});

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertNotNull(result, "Result should not be null");
      assertEquals(3, result.length, "Result should have three elements");
      assertEquals(1, result[0].asInt(), "First value should be 1");
      assertEquals(2, result[1].asInt(), "Second value should be 2");
      assertEquals(3, result[2].asInt(), "Third value should be 3");
    }

    @Test
    @DisplayName("multiValue should return empty array")
    void multiValueShouldReturnEmptyArray() throws WasmException {
      final HostFunction hostFunc = HostFunction.multiValue(params -> new WasmValue[0]);

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertEquals(0, result.length, "Result should be empty");
    }
  }

  @Nested
  @DisplayName("Lambda Implementation Tests")
  class LambdaImplementationTests {

    @Test
    @DisplayName("should work with lambda expression")
    void shouldWorkWithLambdaExpression() throws WasmException {
      final HostFunction hostFunc = params -> new WasmValue[] {WasmValue.i64(42L)};

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertEquals(42L, result[0].asLong(), "Should return lambda result");
    }

    @Test
    @DisplayName("should work with method reference")
    void shouldWorkWithMethodReference() throws WasmException {
      final HostFunction hostFunc = this::testHostFunction;

      final WasmValue[] result = hostFunc.execute(new WasmValue[] {WasmValue.i32(5)});

      assertEquals(10, result[0].asInt(), "Should return method reference result");
    }

    private WasmValue[] testHostFunction(final WasmValue[] params) {
      return new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)};
    }
  }

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("execute should declare WasmException")
    void executeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = HostFunction.class.getMethod("execute", WasmValue[].class);
      final Class<?>[] exceptions = method.getExceptionTypes();

      assertTrue(exceptions.length > 0, "execute should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "execute should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Parameter Type Tests")
  class ParameterTypeTests {

    @Test
    @DisplayName("should handle I32 parameters")
    void shouldHandleI32Parameters() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * 2));

      final WasmValue[] result = hostFunc.execute(new WasmValue[] {WasmValue.i32(21)});

      assertEquals(42, result[0].asInt(), "Should handle I32");
    }

    @Test
    @DisplayName("should handle I64 parameters")
    void shouldHandleI64Parameters() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.singleValue(params -> WasmValue.i64(params[0].asLong() * 2L));

      final WasmValue[] result = hostFunc.execute(new WasmValue[] {WasmValue.i64(100L)});

      assertEquals(200L, result[0].asLong(), "Should handle I64");
    }

    @Test
    @DisplayName("should handle F32 parameters")
    void shouldHandleF32Parameters() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.singleValue(params -> WasmValue.f32(params[0].asFloat() * 2.0f));

      final WasmValue[] result = hostFunc.execute(new WasmValue[] {WasmValue.f32(1.5f)});

      assertEquals(3.0f, result[0].asFloat(), 0.001f, "Should handle F32");
    }

    @Test
    @DisplayName("should handle F64 parameters")
    void shouldHandleF64Parameters() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.singleValue(params -> WasmValue.f64(params[0].asDouble() * 2.0));

      final WasmValue[] result = hostFunc.execute(new WasmValue[] {WasmValue.f64(3.14)});

      assertEquals(6.28, result[0].asDouble(), 0.001, "Should handle F64");
    }

    @Test
    @DisplayName("should handle mixed parameter types")
    void shouldHandleMixedParameterTypes() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.multiValue(
              params -> {
                final int i32Val = params[0].asInt();
                final long i64Val = params[1].asLong();
                final float f32Val = params[2].asFloat();
                return new WasmValue[] {
                  WasmValue.i32(i32Val), WasmValue.i64(i64Val), WasmValue.f32(f32Val)
                };
              });

      final WasmValue[] params = {WasmValue.i32(10), WasmValue.i64(20L), WasmValue.f32(3.14f)};
      final WasmValue[] result = hostFunc.execute(params);

      assertEquals(10, result[0].asInt(), "I32 should match");
      assertEquals(20L, result[1].asLong(), "I64 should match");
      assertEquals(3.14f, result[2].asFloat(), 0.001f, "F32 should match");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty parameters")
    void shouldHandleEmptyParameters() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.singleValue(
              params -> {
                assertEquals(0, params.length, "Parameters should be empty");
                return WasmValue.i32(42);
              });

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertEquals(42, result[0].asInt(), "Should return value with empty params");
    }

    @Test
    @DisplayName("should handle large number of parameters")
    void shouldHandleLargeNumberOfParameters() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.singleValue(
              params -> {
                int sum = 0;
                for (final WasmValue param : params) {
                  sum += param.asInt();
                }
                return WasmValue.i32(sum);
              });

      final WasmValue[] params = new WasmValue[100];
      for (int i = 0; i < 100; i++) {
        params[i] = WasmValue.i32(1);
      }
      final WasmValue[] result = hostFunc.execute(params);

      assertEquals(100, result[0].asInt(), "Should sum 100 parameters");
    }

    @Test
    @DisplayName("should handle large number of return values")
    void shouldHandleLargeNumberOfReturnValues() throws WasmException {
      final HostFunction hostFunc =
          HostFunction.multiValue(
              params -> {
                final WasmValue[] results = new WasmValue[50];
                for (int i = 0; i < 50; i++) {
                  results[i] = WasmValue.i32(i);
                }
                return results;
              });

      final WasmValue[] result = hostFunc.execute(new WasmValue[0]);

      assertEquals(50, result.length, "Should return 50 values");
      for (int i = 0; i < 50; i++) {
        assertEquals(i, result[i].asInt(), "Value at index " + i + " should be " + i);
      }
    }
  }
}
