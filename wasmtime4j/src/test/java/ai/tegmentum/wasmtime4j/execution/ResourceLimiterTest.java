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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourceLimiter} interface.
 *
 * <p>ResourceLimiter controls WebAssembly resource consumption including memory and table growth.
 */
@DisplayName("ResourceLimiter Tests")
class ResourceLimiterTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ResourceLimiter.class.getModifiers()),
          "ResourceLimiter should be public");
      assertTrue(ResourceLimiter.class.isInterface(), "ResourceLimiter should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ResourceLimiter.class),
          "ResourceLimiter should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Definition Tests")
  class MethodDefinitionTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "getId should return long");
      assertEquals(0, method.getParameterCount(), "getId should have no parameters");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ResourceLimiterConfig.class,
          method.getReturnType(),
          "getConfig should return ResourceLimiterConfig");
    }

    @Test
    @DisplayName("should have allowMemoryGrow method")
    void shouldHaveAllowMemoryGrowMethod() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertNotNull(method, "allowMemoryGrow method should exist");
      assertEquals(boolean.class, method.getReturnType(), "allowMemoryGrow should return boolean");
      assertEquals(2, method.getParameterCount(), "allowMemoryGrow should have 2 parameters");
    }

    @Test
    @DisplayName("should have allowTableGrow method")
    void shouldHaveAllowTableGrowMethod() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      assertNotNull(method, "allowTableGrow method should exist");
      assertEquals(boolean.class, method.getReturnType(), "allowTableGrow should return boolean");
      assertEquals(2, method.getParameterCount(), "allowTableGrow should have 2 parameters");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          ResourceLimiterStats.class,
          method.getReturnType(),
          "getStats should return ResourceLimiterStats");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("resetStats");
      assertNotNull(method, "resetStats method should exist");
      assertEquals(void.class, method.getReturnType(), "resetStats should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("getConfig should declare WasmException")
    void getConfigShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("getConfig");
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "getConfig should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "getConfig should declare WasmException");
    }

    @Test
    @DisplayName("allowMemoryGrow should declare WasmException")
    void allowMemoryGrowShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "allowMemoryGrow should declare exceptions");
      assertEquals(
          WasmException.class, exceptions[0], "allowMemoryGrow should declare WasmException");
    }

    @Test
    @DisplayName("allowTableGrow should declare WasmException")
    void allowTableGrowShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "allowTableGrow should declare exceptions");
      assertEquals(
          WasmException.class, exceptions[0], "allowTableGrow should declare WasmException");
    }

    @Test
    @DisplayName("getStats should declare WasmException")
    void getStatsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("getStats");
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "getStats should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "getStats should declare WasmException");
    }

    @Test
    @DisplayName("resetStats should declare WasmException")
    void resetStatsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("resetStats");
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "resetStats should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "resetStats should declare WasmException");
    }

    @Test
    @DisplayName("close should declare WasmException")
    void closeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("close");
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "close should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "close should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Parameter Type Tests")
  class ParameterTypeTests {

    @Test
    @DisplayName("allowMemoryGrow parameters should be long")
    void allowMemoryGrowParametersShouldBeLong() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      final Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "First parameter should be long (currentPages)");
      assertEquals(long.class, paramTypes[1], "Second parameter should be long (requestedPages)");
    }

    @Test
    @DisplayName("allowTableGrow parameters should be long")
    void allowTableGrowParametersShouldBeLong() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      final Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "First parameter should be long (currentElements)");
      assertEquals(
          long.class, paramTypes[1], "Second parameter should be long (requestedElements)");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly 7 declared methods")
    void shouldHave7DeclaredMethods() {
      final Method[] methods = ResourceLimiter.class.getDeclaredMethods();
      assertEquals(7, methods.length, "ResourceLimiter should have 7 declared methods");
    }
  }

  @Nested
  @DisplayName("Method Modifier Tests")
  class MethodModifierTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : ResourceLimiter.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }

    @Test
    @DisplayName("all methods should be abstract")
    void allMethodsShouldBeAbstract() {
      for (final Method method : ResourceLimiter.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isAbstract(method.getModifiers()),
            "Method " + method.getName() + " should be abstract");
      }
    }
  }

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getId should return primitive long")
    void getIdShouldReturnPrimitiveLong() throws NoSuchMethodException {
      final Method method = ResourceLimiter.class.getMethod("getId");
      assertEquals(long.class, method.getReturnType(), "getId should return primitive long");
    }

    @Test
    @DisplayName("allowMemoryGrow should return primitive boolean")
    void allowMemoryGrowShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertEquals(
          boolean.class, method.getReturnType(), "allowMemoryGrow should return primitive boolean");
    }

    @Test
    @DisplayName("allowTableGrow should return primitive boolean")
    void allowTableGrowShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      final Method method =
          ResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      assertEquals(
          boolean.class, method.getReturnType(), "allowTableGrow should return primitive boolean");
    }
  }
}
