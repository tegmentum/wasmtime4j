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

package ai.tegmentum.wasmtime4j.jni.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmFunctionToFunctionAdapter} class.
 *
 * <p>This test class verifies the adapter that bridges WasmFunction to Function interface.
 */
@DisplayName("WasmFunctionToFunctionAdapter Tests")
class WasmFunctionToFunctionAdapterTest {

  /**
   * Functional interface that can throw WasmException.
   */
  @FunctionalInterface
  private interface WasmCallHandler {
    WasmValue[] apply(WasmValue[] args) throws WasmException;
  }

  /**
   * Creates a mock WasmFunction for testing.
   */
  private WasmFunction createMockFunction(
      final String name,
      final WasmValueType[] paramTypes,
      final WasmValueType[] returnTypes,
      final WasmCallHandler callHandler) {
    return new TestWasmFunction(name, paramTypes, returnTypes, callHandler);
  }

  /**
   * Test implementation of WasmFunction.
   */
  private static class TestWasmFunction implements WasmFunction {
    private final String name;
    private final WasmValueType[] paramTypes;
    private final WasmValueType[] returnTypes;
    private final WasmCallHandler callHandler;

    TestWasmFunction(
        final String name,
        final WasmValueType[] paramTypes,
        final WasmValueType[] returnTypes,
        final WasmCallHandler callHandler) {
      this.name = name;
      this.paramTypes = paramTypes;
      this.returnTypes = returnTypes;
      this.callHandler = callHandler;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public FunctionType getFunctionType() {
      WasmValueType[] params = paramTypes != null ? paramTypes : new WasmValueType[0];
      WasmValueType[] returns = returnTypes != null ? returnTypes : new WasmValueType[0];
      return new FunctionType(params, returns);
    }

    @Override
    public WasmValue[] call(final WasmValue... args) throws WasmException {
      if (callHandler != null) {
        return callHandler.apply(args);
      }
      return new WasmValue[0];
    }

    @Override
    public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
      return CompletableFuture.supplyAsync(() -> {
        try {
          return call(params);
        } catch (final WasmException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  /**
   * Test implementation of WasmFunction that throws on getFunctionType().
   */
  private static class FailingWasmFunction implements WasmFunction {
    @Override
    public String getName() {
      return "invalid";
    }

    @Override
    public FunctionType getFunctionType() {
      throw new RuntimeException("Invalid function");
    }

    @Override
    public WasmValue[] call(final WasmValue... args) {
      return new WasmValue[0];
    }

    @Override
    public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
      return CompletableFuture.completedFuture(new WasmValue[0]);
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmFunctionToFunctionAdapter should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasmFunctionToFunctionAdapter.class.getModifiers()),
          "WasmFunctionToFunctionAdapter should be final");
    }

    @Test
    @DisplayName("WasmFunctionToFunctionAdapter should implement Function")
    void shouldImplementFunction() {
      assertTrue(
          Function.class.isAssignableFrom(WasmFunctionToFunctionAdapter.class),
          "WasmFunctionToFunctionAdapter should implement Function");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid delegate")
    void constructorShouldAcceptValidDelegate() {
      final WasmFunction delegate = createMockFunction("test", null, null, null);

      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertNotNull(adapter, "Adapter should not be null");
      assertSame(delegate, adapter.getDelegate(), "Delegate should match");
    }

    @Test
    @DisplayName("Constructor should throw on null delegate")
    void constructorShouldThrowOnNullDelegate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasmFunctionToFunctionAdapter<>(null),
          "Should throw on null delegate");
    }
  }

  @Nested
  @DisplayName("getName Tests")
  class GetNameTests {

    @Test
    @DisplayName("getName should return function name")
    void getNameShouldReturnFunctionName() {
      final WasmFunction delegate = createMockFunction("myFunction", null, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertEquals("myFunction", adapter.getName(), "Name should match");
    }

    @Test
    @DisplayName("getName should return null for null name")
    void getNameShouldReturnNullForNullName() {
      final WasmFunction delegate = createMockFunction(null, null, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertNull(adapter.getName(), "Name should be null");
    }
  }

  @Nested
  @DisplayName("getParameterTypes Tests")
  class GetParameterTypesTests {

    @Test
    @DisplayName("getParameterTypes should return correct types")
    void getParameterTypesShouldReturnCorrectTypes() {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.I64};
      final WasmFunction delegate = createMockFunction("test", params, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final List<Function.ValueType> types = adapter.getParameterTypes();

      assertEquals(2, types.size(), "Should have 2 parameter types");
      assertEquals(Function.ValueType.I32, types.get(0), "First type should be I32");
      assertEquals(Function.ValueType.I64, types.get(1), "Second type should be I64");
    }

    @Test
    @DisplayName("getParameterTypes should return empty list for no params")
    void getParameterTypesShouldReturnEmptyListForNoParams() {
      final WasmFunction delegate = createMockFunction("test", null, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final List<Function.ValueType> types = adapter.getParameterTypes();

      assertTrue(types.isEmpty(), "Should return empty list");
    }
  }

  @Nested
  @DisplayName("getReturnTypes Tests")
  class GetReturnTypesTests {

    @Test
    @DisplayName("getReturnTypes should return correct types")
    void getReturnTypesShouldReturnCorrectTypes() {
      final WasmValueType[] returns = {WasmValueType.F64};
      final WasmFunction delegate = createMockFunction("test", null, returns, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final List<Function.ValueType> types = adapter.getReturnTypes();

      assertEquals(1, types.size(), "Should have 1 return type");
      assertEquals(Function.ValueType.F64, types.get(0), "Type should be F64");
    }
  }

  @Nested
  @DisplayName("getParameterCount Tests")
  class GetParameterCountTests {

    @Test
    @DisplayName("getParameterCount should return correct count")
    void getParameterCountShouldReturnCorrectCount() {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.I64, WasmValueType.F32};
      final WasmFunction delegate = createMockFunction("test", params, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertEquals(3, adapter.getParameterCount(), "Should have 3 parameters");
    }

    @Test
    @DisplayName("getParameterCount should return 0 for no params")
    void getParameterCountShouldReturnZeroForNoParams() {
      final WasmFunction delegate = createMockFunction("test", null, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertEquals(0, adapter.getParameterCount(), "Should have 0 parameters");
    }
  }

  @Nested
  @DisplayName("getReturnCount Tests")
  class GetReturnCountTests {

    @Test
    @DisplayName("getReturnCount should return correct count")
    void getReturnCountShouldReturnCorrectCount() {
      final WasmValueType[] returns = {WasmValueType.I32, WasmValueType.I64};
      final WasmFunction delegate = createMockFunction("test", null, returns, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertEquals(2, adapter.getReturnCount(), "Should have 2 returns");
    }
  }

  @Nested
  @DisplayName("call Tests")
  class CallTests {

    @Test
    @DisplayName("call should invoke delegate and return results")
    void callShouldInvokeDelegateAndReturnResults() throws WasmException {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.I32};
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("add", params, returns, args -> {
        int a = args[0].asInt();
        int b = args[1].asInt();
        return new WasmValue[]{WasmValue.i32(a + b)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call(5, 3);

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(8, results[0], "Result should be 8");
    }

    @Test
    @DisplayName("call should handle no arguments")
    void callShouldHandleNoArguments() throws WasmException {
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("getConstant", null, returns, args -> {
        return new WasmValue[]{WasmValue.i32(42)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call();

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(42, results[0], "Result should be 42");
    }

    @Test
    @DisplayName("call should handle void function")
    void callShouldHandleVoidFunction() throws WasmException {
      final WasmValueType[] params = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("logValue", params, null, args -> {
        return new WasmValue[0];
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call(100);

      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "Should have no results");
    }

    @Test
    @DisplayName("call should handle WasmValue arguments")
    void callShouldHandleWasmValueArguments() throws WasmException {
      final WasmValueType[] params = {WasmValueType.I32};
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("double", params, returns, args -> {
        int a = args[0].asInt();
        return new WasmValue[]{WasmValue.i32(a * 2)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call(WasmValue.i32(10));

      assertNotNull(results, "Results should not be null");
      assertEquals(20, results[0], "Result should be 20");
    }

    @Test
    @DisplayName("call should propagate WasmException")
    void callShouldPropagateWasmException() {
      final WasmFunction delegate = createMockFunction("failing", null, null, args -> {
        throw new WasmException("Function execution failed");
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertThrows(WasmException.class, () -> adapter.call(), "Should propagate WasmException");
    }
  }

  @Nested
  @DisplayName("callSingle Tests")
  class CallSingleTests {

    @Test
    @DisplayName("callSingle should return single result")
    void callSingleShouldReturnSingleResult() throws WasmException {
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("getValue", null, returns, args -> {
        return new WasmValue[]{WasmValue.i32(99)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object result = adapter.callSingle();

      assertEquals(99, result, "Result should be 99");
    }

    @Test
    @DisplayName("callSingle should return null for void function")
    void callSingleShouldReturnNullForVoidFunction() throws WasmException {
      final WasmFunction delegate = createMockFunction("voidFunc", null, null, args -> {
        return new WasmValue[0];
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object result = adapter.callSingle();

      assertNull(result, "Result should be null for void function");
    }

    @Test
    @DisplayName("callSingle should throw for multiple return values")
    void callSingleShouldThrowForMultipleReturnValues() {
      final WasmValueType[] returns = {WasmValueType.I32, WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("divMod", null, returns, args -> {
        return new WasmValue[]{WasmValue.i32(10), WasmValue.i32(5)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertThrows(
          WasmException.class,
          () -> adapter.callSingle(),
          "Should throw for multiple return values");
    }
  }

  @Nested
  @DisplayName("getSignature Tests")
  class GetSignatureTests {

    @Test
    @DisplayName("getSignature should return valid signature")
    void getSignatureShouldReturnValidSignature() {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.F64};
      final WasmValueType[] returns = {WasmValueType.I64};
      final WasmFunction delegate = createMockFunction("test", params, returns, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Function.FunctionSignature sig = adapter.getSignature();

      assertNotNull(sig, "Signature should not be null");
      assertEquals(2, sig.getParameterTypes().size(), "Should have 2 params");
      assertEquals(1, sig.getReturnTypes().size(), "Should have 1 return");
    }

    @Test
    @DisplayName("signature matches should work correctly")
    void signatureMatchesShouldWorkCorrectly() {
      final WasmValueType[] params = {WasmValueType.I32};
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate1 = createMockFunction("func1", params, returns, null);
      final WasmFunction delegate2 = createMockFunction("func2", params, returns, null);
      final WasmFunctionToFunctionAdapter<?> adapter1 =
          new WasmFunctionToFunctionAdapter<>(delegate1);
      final WasmFunctionToFunctionAdapter<?> adapter2 =
          new WasmFunctionToFunctionAdapter<>(delegate2);

      assertTrue(
          adapter1.getSignature().matches(adapter2.getSignature()),
          "Matching signatures should be true");
    }
  }

  @Nested
  @DisplayName("isValid Tests")
  class IsValidTests {

    @Test
    @DisplayName("isValid should return true for valid function")
    void isValidShouldReturnTrueForValidFunction() {
      final WasmFunction delegate = createMockFunction("test", null, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertTrue(adapter.isValid(), "Should be valid");
    }

    @Test
    @DisplayName("isValid should return false when delegate throws")
    void isValidShouldReturnFalseWhenDelegateThrows() {
      final WasmFunction delegate = new FailingWasmFunction();
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertFalse(adapter.isValid(), "Should be invalid when delegate throws");
    }
  }

  @Nested
  @DisplayName("callAsync Tests")
  class CallAsyncTests {

    @Test
    @DisplayName("callAsync should complete successfully")
    void callAsyncShouldCompleteSuccessfully() throws Exception {
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("asyncFunc", null, returns, args -> {
        return new WasmValue[]{WasmValue.i32(123)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final CompletableFuture<Object[]> future = adapter.callAsync();
      final Object[] results = future.get(5, TimeUnit.SECONDS);

      assertNotNull(results, "Results should not be null");
      assertEquals(123, results[0], "Result should be 123");
    }

    @Test
    @DisplayName("callAsync with timeout should work")
    void callAsyncWithTimeoutShouldWork() throws Exception {
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("asyncFunc", null, returns, args -> {
        return new WasmValue[]{WasmValue.i32(456)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final CompletableFuture<Object[]> future = adapter.callAsync(5L, TimeUnit.SECONDS, new Object[0]);
      final Object[] results = future.get(10, TimeUnit.SECONDS);

      assertNotNull(results, "Results should not be null");
      assertEquals(456, results[0], "Result should be 456");
    }

    @Test
    @DisplayName("callAsync should propagate exception")
    void callAsyncShouldPropagateException() {
      final WasmFunction delegate = createMockFunction("failingAsync", null, null, args -> {
        throw new WasmException("Async failure");
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final CompletableFuture<Object[]> future = adapter.callAsync();

      assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS),
          "Should propagate exception");
    }
  }

  @Nested
  @DisplayName("callSingleAsync Tests")
  class CallSingleAsyncTests {

    @Test
    @DisplayName("callSingleAsync should return single result")
    void callSingleAsyncShouldReturnSingleResult() throws Exception {
      final WasmValueType[] returns = {WasmValueType.F64};
      final WasmFunction delegate = createMockFunction("asyncSingle", null, returns, args -> {
        return new WasmValue[]{WasmValue.f64(3.14)};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final CompletableFuture<Object> future = adapter.callSingleAsync();
      final Object result = future.get(5, TimeUnit.SECONDS);

      assertEquals(3.14, result, "Result should be 3.14");
    }
  }

  @Nested
  @DisplayName("Type Conversion Tests")
  class TypeConversionTests {

    @Test
    @DisplayName("Should convert Integer to I32")
    void shouldConvertIntegerToI32() throws WasmException {
      final WasmValueType[] params = {WasmValueType.I32};
      final WasmValueType[] returns = {WasmValueType.I32};
      final WasmFunction delegate = createMockFunction("identity", params, returns, args -> {
        return new WasmValue[]{WasmValue.i32(args[0].asInt())};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call(42);

      assertEquals(42, results[0], "Should convert Integer correctly");
    }

    @Test
    @DisplayName("Should convert Long to I64")
    void shouldConvertLongToI64() throws WasmException {
      final WasmValueType[] params = {WasmValueType.I64};
      final WasmValueType[] returns = {WasmValueType.I64};
      final WasmFunction delegate = createMockFunction("identity", params, returns, args -> {
        return new WasmValue[]{WasmValue.i64(args[0].asLong())};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, results[0], "Should convert Long correctly");
    }

    @Test
    @DisplayName("Should convert Float to F32")
    void shouldConvertFloatToF32() throws WasmException {
      final WasmValueType[] params = {WasmValueType.F32};
      final WasmValueType[] returns = {WasmValueType.F32};
      final WasmFunction delegate = createMockFunction("identity", params, returns, args -> {
        return new WasmValue[]{WasmValue.f32(args[0].asFloat())};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call(1.5f);

      assertEquals(1.5f, results[0], "Should convert Float correctly");
    }

    @Test
    @DisplayName("Should convert Double to F64")
    void shouldConvertDoubleToF64() throws WasmException {
      final WasmValueType[] params = {WasmValueType.F64};
      final WasmValueType[] returns = {WasmValueType.F64};
      final WasmFunction delegate = createMockFunction("identity", params, returns, args -> {
        return new WasmValue[]{WasmValue.f64(args[0].asDouble())};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call(2.718281828);

      assertEquals(2.718281828, results[0], "Should convert Double correctly");
    }

    @Test
    @DisplayName("Should handle null as externref")
    void shouldHandleNullAsExternref() throws WasmException {
      final WasmValueType[] params = {WasmValueType.EXTERNREF};
      final WasmValueType[] returns = {WasmValueType.EXTERNREF};
      final WasmFunction delegate = createMockFunction("passThrough", params, returns, args -> {
        return new WasmValue[]{args[0]};
      });
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      final Object[] results = adapter.call((Object) null);

      assertNull(results[0], "Should handle null correctly");
    }
  }

  @Nested
  @DisplayName("getDelegate Tests")
  class GetDelegateTests {

    @Test
    @DisplayName("getDelegate should return original delegate")
    void getDelegateShouldReturnOriginalDelegate() {
      final WasmFunction delegate = createMockFunction("test", null, null, null);
      final WasmFunctionToFunctionAdapter<?> adapter =
          new WasmFunctionToFunctionAdapter<>(delegate);

      assertSame(delegate, adapter.getDelegate(), "Should return same delegate");
    }
  }
}
