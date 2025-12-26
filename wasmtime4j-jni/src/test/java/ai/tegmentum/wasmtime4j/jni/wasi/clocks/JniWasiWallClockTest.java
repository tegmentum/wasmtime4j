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

package ai.tegmentum.wasmtime4j.jni.wasi.clocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiWallClock;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiWallClock} class.
 *
 * <p>JniWasiWallClock provides JNI-based access to WASI Preview 2 wall clock operations.
 */
@DisplayName("JniWasiWallClock Tests")
class JniWasiWallClockTest {

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.wasi.clocks.JniWasiWallClock", false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniWasiWallClock should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "JniWasiWallClock should be public");
    }

    @Test
    @DisplayName("should implement WasiWallClock interface")
    void shouldImplementWasiWallClockInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          WasiWallClock.class.isAssignableFrom(clazz),
          "JniWasiWallClock should implement WasiWallClock");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with long parameter")
    void shouldHaveConstructorWithLongParameter() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasLongConstructor = false;

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0] == long.class) {
          hasLongConstructor = true;
          break;
        }
      }

      assertTrue(hasLongConstructor, "Should have constructor with long parameter (context handle)");
    }

    @Test
    @DisplayName("constructor should be public")
    void constructorShouldBePublic() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0] == long.class) {
          assertTrue(
              Modifier.isPublic(constructor.getModifiers()),
              "Long parameter constructor should be public");
          break;
        }
      }
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have now method")
    void shouldHaveNowMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("now");
      assertNotNull(method, "now method should exist");
      assertEquals(DateTime.class, method.getReturnType(), "now should return DateTime");
    }

    @Test
    @DisplayName("should have resolution method")
    void shouldHaveResolutionMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("resolution");
      assertNotNull(method, "resolution method should exist");
      assertEquals(DateTime.class, method.getReturnType(), "resolution should return DateTime");
    }

    @Test
    @DisplayName("now method should take no parameters")
    void nowMethodShouldTakeNoParameters() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("now");
      assertEquals(0, method.getParameterCount(), "now should take no parameters");
    }

    @Test
    @DisplayName("resolution method should take no parameters")
    void resolutionMethodShouldTakeNoParameters() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("resolution");
      assertEquals(0, method.getParameterCount(), "resolution should take no parameters");
    }
  }

  @Nested
  @DisplayName("Native Method Tests")
  class NativeMethodTests {

    @Test
    @DisplayName("should have native methods declared")
    void shouldHaveNativeMethodsDeclared() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      int nativeMethodCount = 0;

      for (final Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
        }
      }

      assertTrue(nativeMethodCount >= 2, "Should have at least 2 native methods");
    }

    @Test
    @DisplayName("native methods should be private and static")
    void nativeMethodsShouldBePrivateAndStatic() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();

      for (final Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          assertTrue(
              Modifier.isPrivate(method.getModifiers()),
              "Native method " + method.getName() + " should be private");
          assertTrue(
              Modifier.isStatic(method.getModifiers()),
              "Native method " + method.getName() + " should be static");
        }
      }
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
          assertEquals(long.class, field.getType(), "contextHandle should be long");
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
    @DisplayName("should implement all WasiWallClock methods")
    void shouldImplementAllWasiWallClockMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();

      for (final Method interfaceMethod : WasiWallClock.class.getDeclaredMethods()) {
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
  @DisplayName("Return Type Compatibility Tests")
  class ReturnTypeCompatibilityTests {

    @Test
    @DisplayName("DateTime should be valid for wall clock operations")
    void dateTimeShouldBeValidForWallClockOperations() {
      // Verify DateTime can be constructed with valid values
      final DateTime dt = new DateTime(1234567890L, 500000000);
      assertEquals(1234567890L, dt.getSeconds(), "Seconds should match");
      assertEquals(500000000, dt.getNanoseconds(), "Nanoseconds should match");
    }

    @Test
    @DisplayName("DateTime should represent resolution correctly")
    void dateTimeShouldRepresentResolutionCorrectly() {
      // Common resolution is 1 nanosecond (0 seconds, 1 nanosecond)
      final DateTime resolution = new DateTime(0L, 1);
      assertEquals(0L, resolution.getSeconds(), "Resolution seconds should be 0");
      assertEquals(1, resolution.getNanoseconds(), "Resolution nanoseconds should be 1");
    }
  }
}
