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
 * Tests for the {@link WasiOutputStream} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI output
 * stream API using reflection-based testing.
 */
@DisplayName("WasiOutputStream Interface Tests")
class WasiOutputStreamTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiOutputStream should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiOutputStream.class.isInterface(), "WasiOutputStream should be an interface");
    }

    @Test
    @DisplayName("WasiOutputStream should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiOutputStream.class.getModifiers()),
          "WasiOutputStream should be public");
    }

    @Test
    @DisplayName("WasiOutputStream should extend WasiResource")
    void shouldExtendWasiResource() {
      Class<?>[] interfaces = WasiOutputStream.class.getInterfaces();
      assertEquals(1, interfaces.length, "WasiOutputStream should extend 1 interface");
      assertEquals(
          WasiResource.class, interfaces[0], "WasiOutputStream should extend WasiResource");
    }
  }

  // ========================================================================
  // Write Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Write Method Tests")
  class WriteMethodTests {

    @Test
    @DisplayName("should have checkWrite method returning long")
    void shouldHaveCheckWriteMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("checkWrite");
      assertNotNull(method, "checkWrite method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "checkWrite should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "checkWrite should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have write method with byte[] parameter")
    void shouldHaveWriteMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("write", byte[].class);
      assertNotNull(method, "write method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "write should have 1 parameter");
      assertEquals(byte[].class, method.getParameterTypes()[0], "Parameter should be byte[]");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "write should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have blockingWriteAndFlush method with byte[] parameter")
    void shouldHaveBlockingWriteAndFlushMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("blockingWriteAndFlush", byte[].class);
      assertNotNull(method, "blockingWriteAndFlush method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "blockingWriteAndFlush should have 1 parameter");
      assertEquals(byte[].class, method.getParameterTypes()[0], "Parameter should be byte[]");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "blockingWriteAndFlush should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Flush Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Flush Method Tests")
  class FlushMethodTests {

    @Test
    @DisplayName("should have flush method")
    void shouldHaveFlushMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("flush");
      assertNotNull(method, "flush method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "flush should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "flush should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have blockingFlush method")
    void shouldHaveBlockingFlushMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("blockingFlush");
      assertNotNull(method, "blockingFlush method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "blockingFlush should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "blockingFlush should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // WriteZeroes Method Tests
  // ========================================================================

  @Nested
  @DisplayName("WriteZeroes Method Tests")
  class WriteZeroesMethodTests {

    @Test
    @DisplayName("should have writeZeroes method with long parameter")
    void shouldHaveWriteZeroesMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("writeZeroes", long.class);
      assertNotNull(method, "writeZeroes method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "writeZeroes should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "writeZeroes should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have blockingWriteZeroesAndFlush method with long parameter")
    void shouldHaveBlockingWriteZeroesAndFlushMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("blockingWriteZeroesAndFlush", long.class);
      assertNotNull(method, "blockingWriteZeroesAndFlush method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(
          1, method.getParameterCount(), "blockingWriteZeroesAndFlush should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(
          1, exceptionTypes.length, "blockingWriteZeroesAndFlush should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Splice Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Splice Method Tests")
  class SpliceMethodTests {

    @Test
    @DisplayName("should have splice method with WasiInputStream and long parameters")
    void shouldHaveSpliceMethod() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("splice", WasiInputStream.class, long.class);
      assertNotNull(method, "splice method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(2, method.getParameterCount(), "splice should have 2 parameters");
      assertEquals(
          WasiInputStream.class,
          method.getParameterTypes()[0],
          "First parameter should be WasiInputStream");
      assertEquals(long.class, method.getParameterTypes()[1], "Second parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "splice should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have blockingSplice method with WasiInputStream and long parameters")
    void shouldHaveBlockingSpliceMethod() throws NoSuchMethodException {
      Method method =
          WasiOutputStream.class.getMethod("blockingSplice", WasiInputStream.class, long.class);
      assertNotNull(method, "blockingSplice method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(2, method.getParameterCount(), "blockingSplice should have 2 parameters");
      assertEquals(
          WasiInputStream.class,
          method.getParameterTypes()[0],
          "First parameter should be WasiInputStream");
      assertEquals(long.class, method.getParameterTypes()[1], "Second parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "blockingSplice should throw 1 exception");
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
      Method method = WasiOutputStream.class.getMethod("subscribe");
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
    @DisplayName("WasiOutputStream should have exactly 10 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiOutputStream.class.getDeclaredMethods();
      assertEquals(10, methods.length, "WasiOutputStream should have exactly 10 methods");
    }
  }

  // ========================================================================
  // Method Signature Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Verification Tests")
  class MethodSignatureVerificationTests {

    @Test
    @DisplayName("All WasiOutputStream methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = WasiOutputStream.class.getDeclaredMethods();

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
      Method[] methods = WasiOutputStream.class.getDeclaredMethods();

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
    @DisplayName("WasiOutputStream should inherit getId from WasiResource")
    void shouldInheritGetIdFromWasiResource() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("getId");
      assertNotNull(method, "getId method should be inherited");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("WasiOutputStream should inherit getType from WasiResource")
    void shouldInheritGetTypeFromWasiResource() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("getType");
      assertNotNull(method, "getType method should be inherited");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("WasiOutputStream should inherit isValid from WasiResource")
    void shouldInheritIsValidFromWasiResource() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("isValid");
      assertNotNull(method, "isValid method should be inherited");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("WasiOutputStream should inherit close from WasiResource")
    void shouldInheritCloseFromWasiResource() throws NoSuchMethodException {
      Method method = WasiOutputStream.class.getMethod("close");
      assertNotNull(method, "close method should be inherited");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }
}
