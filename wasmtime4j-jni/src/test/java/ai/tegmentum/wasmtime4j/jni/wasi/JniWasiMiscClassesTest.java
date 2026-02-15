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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.wasi.clocks.JniWasiTimezone;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiTimezone;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JNI WASI miscellaneous classes.
 *
 * <p>This test class verifies the structure and interface compliance of WASI-related JNI classes
 * without loading native libraries.
 *
 * <p>Classes tested:
 *
 * <ul>
 *   <li>{@link JniWasiPollable}
 *   <li>{@link JniWasiTimezone}
 * </ul>
 */
@DisplayName("JNI WASI Miscellaneous Classes Tests")
class JniWasiMiscClassesTest {

  // ===================================================================================
  // JniWasiPollable Tests
  // ===================================================================================

  @Nested
  @DisplayName("JniWasiPollable Tests")
  class JniWasiPollableTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("JniWasiPollable should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(JniWasiPollable.class.getModifiers()),
            "JniWasiPollable should be final");
      }

      @Test
      @DisplayName("JniWasiPollable should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(JniWasiPollable.class.getModifiers()),
            "JniWasiPollable should be public");
      }

      @Test
      @DisplayName("JniWasiPollable should extend JniResource")
      void shouldExtendJniResource() {
        assertTrue(
            JniResource.class.isAssignableFrom(JniWasiPollable.class),
            "JniWasiPollable should extend JniResource");
      }

      @Test
      @DisplayName("JniWasiPollable should implement WasiPollable")
      void shouldImplementWasiPollable() {
        assertTrue(
            WasiPollable.class.isAssignableFrom(JniWasiPollable.class),
            "JniWasiPollable should implement WasiPollable");
      }
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

      @Test
      @DisplayName("Should have LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        Field field = JniWasiPollable.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
      }

      @Test
      @DisplayName("Should have contextHandle field")
      void shouldHaveContextHandleField() throws NoSuchFieldException {
        Field field = JniWasiPollable.class.getDeclaredField("contextHandle");
        assertNotNull(field, "contextHandle field should exist");
        assertEquals(long.class, field.getType(), "contextHandle should be long type");
        assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("Should have constructor with long, long parameters")
      void shouldHaveConstructorWithHandles() throws NoSuchMethodException {
        Constructor<?> constructor =
            JniWasiPollable.class.getDeclaredConstructor(long.class, long.class);
        assertNotNull(constructor, "Constructor with handles should exist");
        assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("Should have block method")
      void shouldHaveBlockMethod() throws NoSuchMethodException {
        Method method = JniWasiPollable.class.getMethod("block");
        assertNotNull(method, "block method should exist");
        assertEquals(void.class, method.getReturnType(), "block should return void");
      }

      @Test
      @DisplayName("Should have ready method")
      void shouldHaveReadyMethod() throws NoSuchMethodException {
        Method method = JniWasiPollable.class.getMethod("ready");
        assertNotNull(method, "ready method should exist");
        assertEquals(boolean.class, method.getReturnType(), "ready should return boolean");
      }

      @Test
      @DisplayName("Should have getId method")
      void shouldHaveGetIdMethod() throws NoSuchMethodException {
        Method method = JniWasiPollable.class.getMethod("getId");
        assertNotNull(method, "getId method should exist");
        assertEquals(long.class, method.getReturnType(), "getId should return long");
      }

      @Test
      @DisplayName("Should have getType method")
      void shouldHaveGetTypeMethod() throws NoSuchMethodException {
        Method method = JniWasiPollable.class.getMethod("getType");
        assertNotNull(method, "getType method should exist");
        assertEquals(String.class, method.getReturnType(), "getType should return String");
      }

      @Test
      @DisplayName("Should have isValid method")
      void shouldHaveIsValidMethod() throws NoSuchMethodException {
        Method method = JniWasiPollable.class.getMethod("isValid");
        assertNotNull(method, "isValid method should exist");
        assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
      }

      @Test
      @DisplayName("Should have invoke method")
      void shouldHaveInvokeMethod() throws NoSuchMethodException {
        Method method = JniWasiPollable.class.getMethod("invoke", String.class, Object[].class);
        assertNotNull(method, "invoke method should exist");
        assertEquals(Object.class, method.getReturnType(), "invoke should return Object");
      }
    }

    @Nested
    @DisplayName("Native Method Tests")
    class NativeMethodTests {

      @Test
      @DisplayName("Should have native methods for pollable operations")
      void shouldHaveNativePollableOperations() {
        Set<String> nativeMethods = new HashSet<>();
        for (Method method : JniWasiPollable.class.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            nativeMethods.add(method.getName());
          }
        }

        assertTrue(nativeMethods.contains("nativeBlock"), "Should have nativeBlock");
        assertTrue(nativeMethods.contains("nativeReady"), "Should have nativeReady");
        assertTrue(nativeMethods.contains("nativeClose"), "Should have nativeClose");
      }
    }

    @Nested
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.jni.wasi.io",
            JniWasiPollable.class.getPackage().getName(),
            "Should be in ai.tegmentum.wasmtime4j.jni.wasi.io package");
      }
    }
  }

  // ===================================================================================
  // JniWasiTimezone Tests
  // ===================================================================================

  @Nested
  @DisplayName("JniWasiTimezone Tests")
  class JniWasiTimezoneTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("JniWasiTimezone should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(JniWasiTimezone.class.getModifiers()),
            "JniWasiTimezone should be final");
      }

      @Test
      @DisplayName("JniWasiTimezone should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(JniWasiTimezone.class.getModifiers()),
            "JniWasiTimezone should be public");
      }

      @Test
      @DisplayName("JniWasiTimezone should implement WasiTimezone")
      void shouldImplementWasiTimezone() {
        assertTrue(
            WasiTimezone.class.isAssignableFrom(JniWasiTimezone.class),
            "JniWasiTimezone should implement WasiTimezone");
      }
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

      @Test
      @DisplayName("Should have LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        Field field = JniWasiTimezone.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
      }

      @Test
      @DisplayName("Should have contextHandle field")
      void shouldHaveContextHandleField() throws NoSuchFieldException {
        Field field = JniWasiTimezone.class.getDeclaredField("contextHandle");
        assertNotNull(field, "contextHandle field should exist");
        assertEquals(long.class, field.getType(), "contextHandle should be long type");
        assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("Should have constructor with long parameter")
      void shouldHaveConstructorWithHandle() throws NoSuchMethodException {
        Constructor<?> constructor = JniWasiTimezone.class.getDeclaredConstructor(long.class);
        assertNotNull(constructor, "Constructor with handle should exist");
        assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("Should have display method")
      void shouldHaveDisplayMethod() throws NoSuchMethodException {
        Method method =
            JniWasiTimezone.class.getMethod(
                "display", ai.tegmentum.wasmtime4j.wasi.clocks.DateTime.class);
        assertNotNull(method, "display method should exist");
        assertEquals(
            ai.tegmentum.wasmtime4j.wasi.clocks.TimezoneDisplay.class,
            method.getReturnType(),
            "display should return TimezoneDisplay");
      }

      @Test
      @DisplayName("Should have utcOffset method")
      void shouldHaveUtcOffsetMethod() throws NoSuchMethodException {
        Method method =
            JniWasiTimezone.class.getMethod(
                "utcOffset", ai.tegmentum.wasmtime4j.wasi.clocks.DateTime.class);
        assertNotNull(method, "utcOffset method should exist");
        assertEquals(int.class, method.getReturnType(), "utcOffset should return int");
      }
    }

    @Nested
    @DisplayName("Native Method Tests")
    class NativeMethodTests {

      @Test
      @DisplayName("Should have native methods for timezone operations")
      void shouldHaveNativeTimezoneOperations() {
        Set<String> nativeMethods = new HashSet<>();
        for (Method method : JniWasiTimezone.class.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            nativeMethods.add(method.getName());
          }
        }

        assertTrue(nativeMethods.contains("nativeDisplay"), "Should have nativeDisplay");
        assertTrue(nativeMethods.contains("nativeUtcOffset"), "Should have nativeUtcOffset");
      }
    }

    @Nested
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.jni.wasi.clocks",
            JniWasiTimezone.class.getPackage().getName(),
            "Should be in ai.tegmentum.wasmtime4j.jni.wasi.clocks package");
      }
    }
  }

  // ===================================================================================
  // Cross-Class Consistency Tests
  // ===================================================================================

  @Nested
  @DisplayName("Cross-Class Consistency Tests")
  class CrossClassConsistencyTests {

    @Test
    @DisplayName("All WASI resource classes should be final")
    void allWasiResourceClassesShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniWasiPollable.class.getModifiers()),
          "JniWasiPollable should be final");
      assertTrue(
          Modifier.isFinal(JniWasiTimezone.class.getModifiers()),
          "JniWasiTimezone should be final");
    }

    @Test
    @DisplayName("All WASI resource classes should be public")
    void allWasiResourceClassesShouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniWasiPollable.class.getModifiers()),
          "JniWasiPollable should be public");
      assertTrue(
          Modifier.isPublic(JniWasiTimezone.class.getModifiers()),
          "JniWasiTimezone should be public");
    }

    @Test
    @DisplayName("Classes extending JniResource should have contextHandle")
    void classesExtendingJniResourceShouldHaveContextHandle() throws NoSuchFieldException {
      // JniWasiPollable
      Field field = JniWasiPollable.class.getDeclaredField("contextHandle");
      assertNotNull(field, "JniWasiPollable should have contextHandle");
    }

    @Test
    @DisplayName("All classes should have LOGGER field")
    void allClassesShouldHaveLoggerField() throws NoSuchFieldException {
      Field[] loggerFields = {
        JniWasiPollable.class.getDeclaredField("LOGGER"),
        JniWasiTimezone.class.getDeclaredField("LOGGER")
      };

      for (Field field : loggerFields) {
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
        assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      }
    }
  }

  // ===================================================================================
  // Interface Compliance Tests
  // ===================================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("JniWasiPollable should implement all WasiPollable methods")
    void jniWasiPollableShouldImplementAllMethods() {
      verifyInterfaceImplementation(JniWasiPollable.class, WasiPollable.class);
    }

    @Test
    @DisplayName("JniWasiTimezone should implement all WasiTimezone methods")
    void jniWasiTimezoneShouldImplementAllMethods() {
      verifyInterfaceImplementation(JniWasiTimezone.class, WasiTimezone.class);
    }

    private void verifyInterfaceImplementation(
        final Class<?> implClass, final Class<?> interfaceClass) {
      for (Method interfaceMethod : interfaceClass.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : implClass.getMethods()) {
            if (methodsMatch(interfaceMethod, implMethod)) {
              found = true;
              break;
            }
          }
          assertTrue(
              found,
              implClass.getSimpleName()
                  + " should implement: "
                  + interfaceMethod.getName()
                  + " from "
                  + interfaceClass.getSimpleName());
        }
      }
    }

    private boolean methodsMatch(final Method a, final Method b) {
      if (!a.getName().equals(b.getName())) {
        return false;
      }
      return Arrays.equals(a.getParameterTypes(), b.getParameterTypes());
    }
  }
}
