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

package ai.tegmentum.wasmtime4j.panama.wasi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.cli.WasiExit;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiExit} class.
 *
 * <p>PanamaWasiExit provides Panama FFI access to WASI Preview 2 program termination operations.
 */
@DisplayName("PanamaWasiExit Tests")
class PanamaWasiExitTest {

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.panama.wasi.cli.PanamaWasiExit",
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
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiExit should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiExit should be public");
    }

    @Test
    @DisplayName("should implement WasiExit interface")
    void shouldImplementWasiExitInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          WasiExit.class.isAssignableFrom(clazz), "PanamaWasiExit should implement WasiExit");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with MemorySegment parameter")
    void shouldHaveConstructorWithMemorySegmentParameter() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasConstructor = false;

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0] == MemorySegment.class) {
          hasConstructor = true;
          break;
        }
      }

      assertTrue(
          hasConstructor, "Should have constructor with MemorySegment parameter (context handle)");
    }

    @Test
    @DisplayName("constructor should be public")
    void constructorShouldBePublic() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0] == MemorySegment.class) {
          assertTrue(
              Modifier.isPublic(constructor.getModifiers()),
              "MemorySegment parameter constructor should be public");
          break;
        }
      }
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have exit method")
    void shouldHaveExitMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("exit", int.class);
      assertNotNull(method, "exit method should exist");
      assertEquals(void.class, method.getReturnType(), "exit should return void");
    }

    @Test
    @DisplayName("exit method should take int parameter")
    void exitMethodShouldTakeIntParameter() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("exit", int.class);
      assertEquals(1, method.getParameterCount(), "exit should take one parameter");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
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
          assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
          assertEquals(
              MemorySegment.class, field.getType(), "contextHandle should be MemorySegment");
          break;
        }
      }

      assertTrue(hasContextHandle, "Should have contextHandle field");
    }

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasLogger = false;

      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if ("LOGGER".equals(field.getName())) {
          hasLogger = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
          assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
          assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
          break;
        }
      }

      assertTrue(hasLogger, "Should have LOGGER field");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should implement all WasiExit methods")
    void shouldImplementAllWasiExitMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();

      for (final Method interfaceMethod : WasiExit.class.getDeclaredMethods()) {
        if (!Modifier.isStatic(interfaceMethod.getModifiers())) {
          try {
            clazz.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
          } catch (final NoSuchMethodException e) {
            throw new AssertionError(
                "Should implement method: " + interfaceMethod.getName(), e);
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("Interface Constants Tests")
  class InterfaceConstantsTests {

    @Test
    @DisplayName("WasiExit should have EXIT_SUCCESS constant")
    void wasiExitShouldHaveExitSuccessConstant() {
      assertEquals(0, WasiExit.EXIT_SUCCESS, "EXIT_SUCCESS should be 0");
    }

    @Test
    @DisplayName("WasiExit should have EXIT_FAILURE constant")
    void wasiExitShouldHaveExitFailureConstant() {
      assertEquals(1, WasiExit.EXIT_FAILURE, "EXIT_FAILURE should be 1");
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

      assertTrue(methodHandleCount >= 1, "Should have at least 1 MethodHandle field for FFI");
    }
  }
}
