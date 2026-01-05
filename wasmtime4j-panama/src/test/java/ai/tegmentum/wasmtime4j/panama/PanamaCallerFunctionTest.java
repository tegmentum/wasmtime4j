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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.TypedFunc;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaCallerFunction} class.
 *
 * <p>PanamaCallerFunction wraps a function handle from a Caller context.
 */
@DisplayName("PanamaCallerFunction Tests")
class PanamaCallerFunctionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      // Not public - package-private
      assertTrue(
          !Modifier.isPublic(PanamaCallerFunction.class.getModifiers()),
          "PanamaCallerFunction should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaCallerFunction.class.getModifiers()),
          "PanamaCallerFunction should be final");
    }

    @Test
    @DisplayName("should implement WasmFunction interface")
    void shouldImplementWasmFunctionInterface() {
      assertTrue(
          WasmFunction.class.isAssignableFrom(PanamaCallerFunction.class),
          "PanamaCallerFunction should implement WasmFunction");
    }

    @Test
    @DisplayName("should implement TypedFunctionSupport interface")
    void shouldImplementTypedFunctionSupportInterface() {
      assertTrue(
          TypedFunc.TypedFunctionSupport.class.isAssignableFrom(PanamaCallerFunction.class),
          "PanamaCallerFunction should implement TypedFunctionSupport");
    }
  }

  @Nested
  @DisplayName("WasmFunction Method Tests")
  class WasmFunctionMethodTests {

    @Test
    @DisplayName("should have call method")
    void shouldHaveCallMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerFunction.class.getMethod("call", WasmValue[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "Should return WasmValue[]");
    }

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerFunction.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(FunctionType.class, method.getReturnType(), "Should return FunctionType");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerFunction.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("TypedFunctionSupport Method Tests")
  class TypedFunctionSupportMethodTests {

    @Test
    @DisplayName("should have asTyped method")
    void shouldHaveAsTypedMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerFunction.class.getMethod("asTyped", String.class);
      assertNotNull(method, "asTyped method should exist");
      assertEquals(TypedFunc.class, method.getReturnType(), "Should return TypedFunc");
    }
  }

  @Nested
  @DisplayName("Async Method Tests")
  class AsyncMethodTests {

    @Test
    @DisplayName("should have callAsync method")
    void shouldHaveCallAsyncMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerFunction.class.getMethod("callAsync", WasmValue[].class);
      assertNotNull(method, "callAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerFunction.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with 3 parameters")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaCallerFunction.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 3) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(hasExpectedConstructor, "Should have constructor with 3 parameters");
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerFunction.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
