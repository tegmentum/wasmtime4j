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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FuelCallbackHandler} interface.
 *
 * <p>FuelCallbackHandler manages fuel exhaustion events during WebAssembly execution.
 */
@DisplayName("FuelCallbackHandler Tests")
class FuelCallbackHandlerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(FuelCallbackHandler.class.getModifiers()),
          "FuelCallbackHandler should be public");
      assertTrue(
          FuelCallbackHandler.class.isInterface(), "FuelCallbackHandler should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(FuelCallbackHandler.class),
          "FuelCallbackHandler should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Definition Tests")
  class MethodDefinitionTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "getId should return long");
    }

    @Test
    @DisplayName("should have getStoreId method")
    void shouldHaveGetStoreIdMethod() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("getStoreId");
      assertNotNull(method, "getStoreId method should exist");
      assertEquals(long.class, method.getReturnType(), "getStoreId should return long");
    }

    @Test
    @DisplayName("should have handleExhaustion method")
    void shouldHaveHandleExhaustionMethod() throws NoSuchMethodException {
      final Method method =
          FuelCallbackHandler.class.getMethod("handleExhaustion", FuelExhaustionContext.class);
      assertNotNull(method, "handleExhaustion method should exist");
      assertEquals(
          FuelExhaustionResult.class,
          method.getReturnType(),
          "handleExhaustion should return FuelExhaustionResult");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          FuelCallbackStats.class,
          method.getReturnType(),
          "getStats should return FuelCallbackStats");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("resetStats");
      assertNotNull(method, "resetStats method should exist");
      assertEquals(void.class, method.getReturnType(), "resetStats should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("handleExhaustion should declare WasmException")
    void handleExhaustionShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          FuelCallbackHandler.class.getMethod("handleExhaustion", FuelExhaustionContext.class);
      final Class<?>[] exceptions = method.getExceptionTypes();

      assertTrue(exceptions.length > 0, "handleExhaustion should declare exceptions");
      assertEquals(
          ai.tegmentum.wasmtime4j.exception.WasmException.class,
          exceptions[0],
          "handleExhaustion should declare WasmException");
    }

    @Test
    @DisplayName("getStats should declare WasmException")
    void getStatsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("getStats");
      final Class<?>[] exceptions = method.getExceptionTypes();

      assertTrue(exceptions.length > 0, "getStats should declare exceptions");
      assertEquals(
          ai.tegmentum.wasmtime4j.exception.WasmException.class,
          exceptions[0],
          "getStats should declare WasmException");
    }

    @Test
    @DisplayName("resetStats should declare WasmException")
    void resetStatsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("resetStats");
      final Class<?>[] exceptions = method.getExceptionTypes();

      assertTrue(exceptions.length > 0, "resetStats should declare exceptions");
      assertEquals(
          ai.tegmentum.wasmtime4j.exception.WasmException.class,
          exceptions[0],
          "resetStats should declare WasmException");
    }

    @Test
    @DisplayName("close should declare WasmException")
    void closeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("close");
      final Class<?>[] exceptions = method.getExceptionTypes();

      assertTrue(exceptions.length > 0, "close should declare exceptions");
      assertEquals(
          ai.tegmentum.wasmtime4j.exception.WasmException.class,
          exceptions[0],
          "close should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly 6 declared methods")
    void shouldHave6DeclaredMethods() {
      final Method[] methods = FuelCallbackHandler.class.getDeclaredMethods();
      assertEquals(6, methods.length, "FuelCallbackHandler should have 6 declared methods");
    }
  }

  @Nested
  @DisplayName("Method Modifier Tests")
  class MethodModifierTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : FuelCallbackHandler.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }

    @Test
    @DisplayName("all methods should be abstract")
    void allMethodsShouldBeAbstract() {
      for (final Method method : FuelCallbackHandler.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isAbstract(method.getModifiers()),
            "Method " + method.getName() + " should be abstract");
      }
    }
  }

  @Nested
  @DisplayName("Parameter Type Tests")
  class ParameterTypeTests {

    @Test
    @DisplayName("getId should have no parameters")
    void getIdShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("getId");
      assertEquals(0, method.getParameterCount(), "getId should have no parameters");
    }

    @Test
    @DisplayName("getStoreId should have no parameters")
    void getStoreIdShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("getStoreId");
      assertEquals(0, method.getParameterCount(), "getStoreId should have no parameters");
    }

    @Test
    @DisplayName("handleExhaustion should have one FuelExhaustionContext parameter")
    void handleExhaustionShouldHaveOneParameter() throws NoSuchMethodException {
      final Method method =
          FuelCallbackHandler.class.getMethod("handleExhaustion", FuelExhaustionContext.class);
      assertEquals(1, method.getParameterCount(), "handleExhaustion should have 1 parameter");
      assertEquals(
          FuelExhaustionContext.class,
          method.getParameterTypes()[0],
          "Parameter should be FuelExhaustionContext");
    }

    @Test
    @DisplayName("getStats should have no parameters")
    void getStatsShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("getStats");
      assertEquals(0, method.getParameterCount(), "getStats should have no parameters");
    }

    @Test
    @DisplayName("resetStats should have no parameters")
    void resetStatsShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("resetStats");
      assertEquals(0, method.getParameterCount(), "resetStats should have no parameters");
    }

    @Test
    @DisplayName("close should have no parameters")
    void closeShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = FuelCallbackHandler.class.getMethod("close");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }
  }
}
