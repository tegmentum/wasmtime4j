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
 * Tests for the {@link WasiInputStream} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI input
 * stream API using reflection-based testing.
 */
@DisplayName("WasiInputStream Interface Tests")
class WasiInputStreamTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiInputStream should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiInputStream.class.isInterface(), "WasiInputStream should be an interface");
    }

    @Test
    @DisplayName("WasiInputStream should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiInputStream.class.getModifiers()),
          "WasiInputStream should be public");
    }

    @Test
    @DisplayName("WasiInputStream should extend WasiResource")
    void shouldExtendWasiResource() {
      Class<?>[] interfaces = WasiInputStream.class.getInterfaces();
      assertEquals(1, interfaces.length, "WasiInputStream should extend 1 interface");
      assertEquals(WasiResource.class, interfaces[0], "WasiInputStream should extend WasiResource");
    }
  }

  // ========================================================================
  // Read Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Read Method Tests")
  class ReadMethodTests {

    @Test
    @DisplayName("should have read method with long parameter returning byte[]")
    void shouldHaveReadMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("read", long.class);
      assertNotNull(method, "read method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
      assertEquals(1, method.getParameterCount(), "read should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "read should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have blockingRead method with long parameter returning byte[]")
    void shouldHaveBlockingReadMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("blockingRead", long.class);
      assertNotNull(method, "blockingRead method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
      assertEquals(1, method.getParameterCount(), "blockingRead should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "blockingRead should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Skip Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Skip Method Tests")
  class SkipMethodTests {

    @Test
    @DisplayName("should have skip method with long parameter returning long")
    void shouldHaveSkipMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("skip", long.class);
      assertNotNull(method, "skip method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(1, method.getParameterCount(), "skip should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "skip should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have blockingSkip method with long parameter returning long")
    void shouldHaveBlockingSkipMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("blockingSkip", long.class);
      assertNotNull(method, "blockingSkip method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(1, method.getParameterCount(), "blockingSkip should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "blockingSkip should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Subscribe Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Subscribe Method Tests")
  class SubscribeMethodTests {

    @Test
    @DisplayName("should have subscribe method returning WasiPollable")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "Return type should be WasiPollable");
      assertEquals(0, method.getParameterCount(), "subscribe should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "subscribe should throw 1 exception");
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
    @DisplayName("WasiInputStream should have exactly 5 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiInputStream.class.getDeclaredMethods();
      assertEquals(5, methods.length, "WasiInputStream should have exactly 5 methods");
    }
  }

  // ========================================================================
  // Method Signature Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Verification Tests")
  class MethodSignatureVerificationTests {

    @Test
    @DisplayName("All WasiInputStream methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = WasiInputStream.class.getDeclaredMethods();

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
      Method[] methods = WasiInputStream.class.getDeclaredMethods();

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
    @DisplayName("WasiInputStream should inherit getId from WasiResource")
    void shouldInheritGetIdFromWasiResource() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("getId");
      assertNotNull(method, "getId method should be inherited");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("WasiInputStream should inherit getType from WasiResource")
    void shouldInheritGetTypeFromWasiResource() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("getType");
      assertNotNull(method, "getType method should be inherited");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("WasiInputStream should inherit isValid from WasiResource")
    void shouldInheritIsValidFromWasiResource() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("isValid");
      assertNotNull(method, "isValid method should be inherited");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("WasiInputStream should inherit close from WasiResource")
    void shouldInheritCloseFromWasiResource() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("close");
      assertNotNull(method, "close method should be inherited");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }
}
