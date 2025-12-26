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

package ai.tegmentum.wasmtime4j.wasi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiPollable} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI pollable
 * API using reflection-based testing.
 */
@DisplayName("WasiPollable Interface Tests")
class WasiPollableTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiPollable should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiPollable.class.isInterface(), "WasiPollable should be an interface");
    }

    @Test
    @DisplayName("WasiPollable should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiPollable.class.getModifiers()), "WasiPollable should be public");
    }

    @Test
    @DisplayName("WasiPollable should extend WasiResource")
    void shouldExtendWasiResource() {
      Class<?>[] interfaces = WasiPollable.class.getInterfaces();
      assertEquals(1, interfaces.length, "WasiPollable should extend 1 interface");
      assertEquals(WasiResource.class, interfaces[0], "WasiPollable should extend WasiResource");
    }
  }

  // ========================================================================
  // Block Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Block Method Tests")
  class BlockMethodTests {

    @Test
    @DisplayName("should have block method")
    void shouldHaveBlockMethod() throws NoSuchMethodException {
      Method method = WasiPollable.class.getMethod("block");
      assertNotNull(method, "block method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "block should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "block should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Ready Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Ready Method Tests")
  class ReadyMethodTests {

    @Test
    @DisplayName("should have ready method returning boolean")
    void shouldHaveReadyMethod() throws NoSuchMethodException {
      Method method = WasiPollable.class.getMethod("ready");
      assertNotNull(method, "ready method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "ready should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "ready should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiPollable should have exactly 2 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiPollable.class.getDeclaredMethods();
      assertEquals(2, methods.length, "WasiPollable should have exactly 2 methods");
    }
  }

  // ========================================================================
  // Method Signature Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Verification Tests")
  class MethodSignatureVerificationTests {

    @Test
    @DisplayName("All WasiPollable methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = WasiPollable.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            "Method " + method.getName() + " should be public and abstract");
      }
    }

    @Test
    @DisplayName("All methods should throw WasmException")
    void allMethodsShouldThrowWasmException() {
      Method[] methods = WasiPollable.class.getDeclaredMethods();

      for (Method method : methods) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        assertEquals(
            1, exceptionTypes.length, "Method " + method.getName() + " should throw 1 exception");
        assertEquals(
            WasmException.class,
            exceptionTypes[0],
            "Method " + method.getName() + " should throw WasmException");
      }
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("WasiPollable should inherit getId from WasiResource")
    void shouldInheritGetIdFromWasiResource() throws NoSuchMethodException {
      Method method = WasiPollable.class.getMethod("getId");
      assertNotNull(method, "getId method should be inherited");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("WasiPollable should inherit getType from WasiResource")
    void shouldInheritGetTypeFromWasiResource() throws NoSuchMethodException {
      Method method = WasiPollable.class.getMethod("getType");
      assertNotNull(method, "getType method should be inherited");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("WasiPollable should inherit isValid from WasiResource")
    void shouldInheritIsValidFromWasiResource() throws NoSuchMethodException {
      Method method = WasiPollable.class.getMethod("isValid");
      assertNotNull(method, "isValid method should be inherited");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("WasiPollable should inherit close from WasiResource")
    void shouldInheritCloseFromWasiResource() throws NoSuchMethodException {
      Method method = WasiPollable.class.getMethod("close");
      assertNotNull(method, "close method should be inherited");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("WasiPollable should inherit getOwner from WasiResource")
    void shouldInheritGetOwnerFromWasiResource() throws NoSuchMethodException {
      Method method = WasiPollable.class.getMethod("getOwner");
      assertNotNull(method, "getOwner method should be inherited");
      assertEquals(
          ai.tegmentum.wasmtime4j.wasi.WasiInstance.class,
          method.getReturnType(),
          "Return type should be WasiInstance");
    }
  }
}
