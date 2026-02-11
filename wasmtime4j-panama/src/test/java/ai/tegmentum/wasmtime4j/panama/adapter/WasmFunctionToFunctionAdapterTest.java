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

package ai.tegmentum.wasmtime4j.panama.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.func.Function;
import ai.tegmentum.wasmtime4j.WasmFunction;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmFunctionToFunctionAdapter} class.
 *
 * <p>WasmFunctionToFunctionAdapter bridges the gap between the WasmFunction interface and the
 * Function interface used by the Caller interface, handling conversion between WasmValue and Object
 * types.
 */
@DisplayName("WasmFunctionToFunctionAdapter Tests")
class WasmFunctionToFunctionAdapterTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasmFunctionToFunctionAdapter.class.getModifiers()),
          "WasmFunctionToFunctionAdapter should be public");
      assertTrue(
          Modifier.isFinal(WasmFunctionToFunctionAdapter.class.getModifiers()),
          "WasmFunctionToFunctionAdapter should be final");
    }

    @Test
    @DisplayName("should implement Function interface")
    void shouldImplementFunctionInterface() {
      assertTrue(
          Function.class.isAssignableFrom(WasmFunctionToFunctionAdapter.class),
          "WasmFunctionToFunctionAdapter should implement Function");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with WasmFunction parameter")
    void shouldHaveConstructorWithWasmFunction() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasmFunctionToFunctionAdapter.class.getConstructor(WasmFunction.class);
      assertNotNull(constructor, "Constructor with WasmFunction should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Call Method Tests")
  class CallMethodTests {

    @Test
    @DisplayName("should have call method with varargs")
    void shouldHaveCallMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("call", Object[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(Object[].class, method.getReturnType(), "Should return Object[]");
    }

    @Test
    @DisplayName("should have callSingle method")
    void shouldHaveCallSingleMethod() throws NoSuchMethodException {
      final Method method =
          WasmFunctionToFunctionAdapter.class.getMethod("callSingle", Object[].class);
      assertNotNull(method, "callSingle method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }
  }

  @Nested
  @DisplayName("Async Call Method Tests")
  class AsyncCallMethodTests {

    @Test
    @DisplayName("should have callAsync method with varargs")
    void shouldHaveCallAsyncMethod() throws NoSuchMethodException {
      final Method method =
          WasmFunctionToFunctionAdapter.class.getMethod("callAsync", Object[].class);
      assertNotNull(method, "callAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have callAsync method with timeout")
    void shouldHaveCallAsyncMethodWithTimeout() throws NoSuchMethodException {
      final Method method =
          WasmFunctionToFunctionAdapter.class.getMethod(
              "callAsync", long.class, TimeUnit.class, Object[].class);
      assertNotNull(method, "callAsync method with timeout should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have callSingleAsync method")
    void shouldHaveCallSingleAsyncMethod() throws NoSuchMethodException {
      final Method method =
          WasmFunctionToFunctionAdapter.class.getMethod("callSingleAsync", Object[].class);
      assertNotNull(method, "callSingleAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have callSingleAsync method with timeout")
    void shouldHaveCallSingleAsyncMethodWithTimeout() throws NoSuchMethodException {
      final Method method =
          WasmFunctionToFunctionAdapter.class.getMethod(
              "callSingleAsync", long.class, TimeUnit.class, Object[].class);
      assertNotNull(method, "callSingleAsync method with timeout should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Signature Method Tests")
  class SignatureMethodTests {

    @Test
    @DisplayName("should have getSignature method")
    void shouldHaveGetSignatureMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("getSignature");
      assertNotNull(method, "getSignature method should exist");
      assertEquals(
          Function.FunctionSignature.class,
          method.getReturnType(),
          "Should return FunctionSignature");
    }

    @Test
    @DisplayName("should have getParameterTypes method")
    void shouldHaveGetParameterTypesMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("getParameterTypes");
      assertNotNull(method, "getParameterTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getReturnTypes method")
    void shouldHaveGetReturnTypesMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("getReturnTypes");
      assertNotNull(method, "getReturnTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getParameterCount method")
    void shouldHaveGetParameterCountMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("getParameterCount");
      assertNotNull(method, "getParameterCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getReturnCount method")
    void shouldHaveGetReturnCountMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("getReturnCount");
      assertNotNull(method, "getReturnCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDelegate method")
    void shouldHaveGetDelegateMethod() throws NoSuchMethodException {
      final Method method = WasmFunctionToFunctionAdapter.class.getMethod("getDelegate");
      assertNotNull(method, "getDelegate method should exist");
      assertEquals(WasmFunction.class, method.getReturnType(), "Should return WasmFunction");
    }
  }
}
