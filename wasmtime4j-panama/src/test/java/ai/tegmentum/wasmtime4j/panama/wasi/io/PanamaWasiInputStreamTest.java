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
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiInputStream} class.
 *
 * <p>PanamaWasiInputStream provides Panama FFI access to WASI Preview 2 input stream operations.
 */
@DisplayName("PanamaWasiInputStream Tests")
class PanamaWasiInputStreamTest {

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiInputStream",
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
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiInputStream should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiInputStream should be public");
    }

    @Test
    @DisplayName("should implement WasiInputStream interface")
    void shouldImplementWasiInputStreamInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          WasiInputStream.class.isAssignableFrom(clazz),
          "PanamaWasiInputStream should implement WasiInputStream");
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
    @DisplayName("should have read method")
    void shouldHaveReadMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("read", long.class);
      assertNotNull(method, "read method should exist");
      assertEquals(byte[].class, method.getReturnType(), "read should return byte[]");
    }

    @Test
    @DisplayName("should have blockingRead method")
    void shouldHaveBlockingReadMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("blockingRead", long.class);
      assertNotNull(method, "blockingRead method should exist");
      assertEquals(byte[].class, method.getReturnType(), "blockingRead should return byte[]");
    }

    @Test
    @DisplayName("should have skip method")
    void shouldHaveSkipMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("skip", long.class);
      assertNotNull(method, "skip method should exist");
      assertEquals(long.class, method.getReturnType(), "skip should return long");
    }

    @Test
    @DisplayName("should have blockingSkip method")
    void shouldHaveBlockingSkipMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("blockingSkip", long.class);
      assertNotNull(method, "blockingSkip method should exist");
      assertEquals(long.class, method.getReturnType(), "blockingSkip should return long");
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
    @DisplayName("should implement all WasiInputStream methods")
    void shouldImplementAllWasiInputStreamMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();

      for (final Method interfaceMethod : WasiInputStream.class.getDeclaredMethods()) {
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

      assertTrue(methodHandleCount >= 4, "Should have at least 4 MethodHandle fields for FFI");
    }
  }
}
