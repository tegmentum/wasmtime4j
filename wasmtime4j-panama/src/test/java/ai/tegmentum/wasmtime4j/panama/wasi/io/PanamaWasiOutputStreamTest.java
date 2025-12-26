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

package ai.tegmentum.wasmtime4j.panama.wasi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiOutputStream} class.
 *
 * <p>PanamaWasiOutputStream provides Panama FFI access to WASI Preview 2 output stream operations.
 */
@DisplayName("PanamaWasiOutputStream Tests")
class PanamaWasiOutputStreamTest {

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiOutputStream",
        false,
        getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiOutputStream should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          Modifier.isPublic(clazz.getModifiers()), "PanamaWasiOutputStream should be public");
    }

    @Test
    @DisplayName("should implement WasiOutputStream interface")
    void shouldImplementWasiOutputStreamInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          WasiOutputStream.class.isAssignableFrom(clazz),
          "PanamaWasiOutputStream should implement WasiOutputStream");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with two MemorySegment parameters")
    void shouldHaveConstructorWithTwoMemorySegmentParameters() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasConstructor = false;

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 2
            && params[0] == MemorySegment.class
            && params[1] == MemorySegment.class) {
          hasConstructor = true;
          break;
        }
      }

      assertTrue(hasConstructor, "Should have constructor with two MemorySegment parameters");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have checkWrite method")
    void shouldHaveCheckWriteMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("checkWrite");
      assertNotNull(method, "checkWrite method should exist");
      assertEquals(long.class, method.getReturnType(), "checkWrite should return long");
    }

    @Test
    @DisplayName("should have write method")
    void shouldHaveWriteMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("write", byte[].class);
      assertNotNull(method, "write method should exist");
      assertEquals(void.class, method.getReturnType(), "write should return void");
    }

    @Test
    @DisplayName("should have blockingWriteAndFlush method")
    void shouldHaveBlockingWriteAndFlushMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("blockingWriteAndFlush", byte[].class);
      assertNotNull(method, "blockingWriteAndFlush method should exist");
      assertEquals(void.class, method.getReturnType(), "blockingWriteAndFlush should return void");
    }

    @Test
    @DisplayName("should have flush method")
    void shouldHaveFlushMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("flush");
      assertNotNull(method, "flush method should exist");
      assertEquals(void.class, method.getReturnType(), "flush should return void");
    }

    @Test
    @DisplayName("should have blockingFlush method")
    void shouldHaveBlockingFlushMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("blockingFlush");
      assertNotNull(method, "blockingFlush method should exist");
      assertEquals(void.class, method.getReturnType(), "blockingFlush should return void");
    }

    @Test
    @DisplayName("should have writeZeroes method")
    void shouldHaveWriteZeroesMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("writeZeroes", long.class);
      assertNotNull(method, "writeZeroes method should exist");
      assertEquals(void.class, method.getReturnType(), "writeZeroes should return void");
    }

    @Test
    @DisplayName("should have blockingWriteZeroesAndFlush method")
    void shouldHaveBlockingWriteZeroesAndFlushMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("blockingWriteZeroesAndFlush", long.class);
      assertNotNull(method, "blockingWriteZeroesAndFlush method should exist");
      assertEquals(
          void.class, method.getReturnType(), "blockingWriteZeroesAndFlush should return void");
    }

    @Test
    @DisplayName("should have splice method")
    void shouldHaveSpliceMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("splice", WasiInputStream.class, long.class);
      assertNotNull(method, "splice method should exist");
      assertEquals(long.class, method.getReturnType(), "splice should return long");
    }

    @Test
    @DisplayName("should have blockingSplice method")
    void shouldHaveBlockingSpliceMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("blockingSplice", WasiInputStream.class, long.class);
      assertNotNull(method, "blockingSplice method should exist");
      assertEquals(long.class, method.getReturnType(), "blockingSplice should return long");
    }

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "subscribe should return WasiPollable");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have contextHandle field")
    void shouldHaveContextHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasContextHandle = false;

      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if ("contextHandle".equals(field.getName())) {
          hasContextHandle = true;
          assertEquals(
              MemorySegment.class, field.getType(), "contextHandle should be MemorySegment");
          break;
        }
      }

      assertTrue(hasContextHandle, "Should have contextHandle field");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should implement all WasiOutputStream methods")
    void shouldImplementAllWasiOutputStreamMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();

      for (final Method interfaceMethod : WasiOutputStream.class.getDeclaredMethods()) {
        if (!Modifier.isStatic(interfaceMethod.getModifiers())) {
          try {
            clazz.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
          } catch (final NoSuchMethodException e) {
            throw new AssertionError("Should implement method: " + interfaceMethod.getName(), e);
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("Panama FFI Handle Tests")
  class PanamaFfiHandleTests {

    @Test
    @DisplayName("should have static MethodHandle fields for FFI")
    void shouldHaveStaticMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      int methodHandleCount = 0;

      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if (field.getType().getName().equals("java.lang.invoke.MethodHandle")
            && Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())) {
          methodHandleCount++;
        }
      }

      assertTrue(methodHandleCount >= 8, "Should have at least 8 MethodHandle fields for FFI");
    }
  }
}
