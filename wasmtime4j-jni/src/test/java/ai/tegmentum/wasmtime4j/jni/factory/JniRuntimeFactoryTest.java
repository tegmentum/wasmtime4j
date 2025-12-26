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

package ai.tegmentum.wasmtime4j.jni.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniRuntimeFactory} class.
 *
 * <p>This test class verifies the factory pattern implementation, static method signatures, and
 * class structure using reflection-based testing since no mocking is allowed.
 */
@DisplayName("JniRuntimeFactory Tests")
class JniRuntimeFactoryTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniRuntimeFactory should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniRuntimeFactory.class.getModifiers()),
          "JniRuntimeFactory should be final");
    }

    @Test
    @DisplayName("JniRuntimeFactory should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(JniRuntimeFactory.class.getModifiers()),
          "JniRuntimeFactory should be public");
    }

    @Test
    @DisplayName("JniRuntimeFactory should have private constructor")
    void shouldHavePrivateConstructor() throws Exception {
      Constructor<?>[] constructors = JniRuntimeFactory.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("JniRuntimeFactory private constructor should throw AssertionError")
    void constructorShouldThrowAssertionError() throws Exception {
      Constructor<?> constructor = JniRuntimeFactory.class.getDeclaredConstructors()[0];
      constructor.setAccessible(true);

      InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              constructor::newInstance,
              "Constructor should throw when invoked");

      assertTrue(
          exception.getCause() instanceof AssertionError,
          "Cause should be AssertionError to prevent instantiation");
    }

    @Test
    @DisplayName("JniRuntimeFactory should not extend any class other than Object")
    void shouldNotExtendAnyClass() {
      assertEquals(
          Object.class,
          JniRuntimeFactory.class.getSuperclass(),
          "JniRuntimeFactory should only extend Object");
    }

    @Test
    @DisplayName("JniRuntimeFactory should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      Class<?>[] interfaces = JniRuntimeFactory.class.getInterfaces();
      assertEquals(0, interfaces.length, "JniRuntimeFactory should not implement any interfaces");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("JniRuntimeFactory should have LOGGER field")
    void shouldHaveLoggerField() throws Exception {
      Field field = JniRuntimeFactory.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("createRuntime should be a public static method")
    void createRuntimeShouldBePublicStatic() throws Exception {
      Method method = JniRuntimeFactory.class.getMethod("createRuntime");
      assertNotNull(method, "createRuntime method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createRuntime should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createRuntime should be static");
      assertEquals(0, method.getParameterCount(), "createRuntime should have no parameters");
    }

    @Test
    @DisplayName("isAvailable should be a public static method returning boolean")
    void isAvailableShouldBePublicStatic() throws Exception {
      Method method = JniRuntimeFactory.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isAvailable should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isAvailable should be static");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isAvailable should have no parameters");
    }

    @Test
    @DisplayName("getImplementationName should return 'JNI'")
    void getImplementationNameShouldReturnJni() {
      String name = JniRuntimeFactory.getImplementationName();
      assertEquals("JNI", name, "Implementation name should be 'JNI'");
    }

    @Test
    @DisplayName("getImplementationName should be a public static method")
    void getImplementationNameShouldBePublicStatic() throws Exception {
      Method method = JniRuntimeFactory.class.getMethod("getImplementationName");
      assertNotNull(method, "getImplementationName method should exist");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "getImplementationName should be public");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "getImplementationName should be static");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(
          0, method.getParameterCount(), "getImplementationName should have no parameters");
    }

    @Test
    @DisplayName("getWasmtimeVersion should be a public static method")
    void getWasmtimeVersionShouldBePublicStatic() throws Exception {
      Method method = JniRuntimeFactory.class.getMethod("getWasmtimeVersion");
      assertNotNull(method, "getWasmtimeVersion method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getWasmtimeVersion should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getWasmtimeVersion should be static");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getWasmtimeVersion should have no parameters");
    }

    @Test
    @DisplayName("getWasmtimeVersion should return a non-null string")
    void getWasmtimeVersionShouldReturnNonNull() {
      String version = JniRuntimeFactory.getWasmtimeVersion();
      assertNotNull(version, "Wasmtime version should not be null");
    }

    @Test
    @DisplayName("getFactoryInfo should be a public static method")
    void getFactoryInfoShouldBePublicStatic() throws Exception {
      Method method = JniRuntimeFactory.class.getMethod("getFactoryInfo");
      assertNotNull(method, "getFactoryInfo method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getFactoryInfo should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getFactoryInfo should be static");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getFactoryInfo should have no parameters");
    }

    @Test
    @DisplayName("getFactoryInfo should return non-empty string")
    void getFactoryInfoShouldReturnNonEmptyString() {
      String info = JniRuntimeFactory.getFactoryInfo();
      assertNotNull(info, "Factory info should not be null");
      assertFalse(info.isEmpty(), "Factory info should not be empty");
      assertTrue(info.contains("JNI"), "Factory info should mention JNI");
    }

    @Test
    @DisplayName("validateEnvironment should be a public static method")
    void validateEnvironmentShouldBePublicStatic() throws Exception {
      Method method = JniRuntimeFactory.class.getMethod("validateEnvironment");
      assertNotNull(method, "validateEnvironment method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "validateEnvironment should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "validateEnvironment should be static");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "validateEnvironment should have no parameters");
    }

    @Test
    @DisplayName("getMinimumJavaVersion should return '8'")
    void getMinimumJavaVersionShouldReturn8() {
      String version = JniRuntimeFactory.getMinimumJavaVersion();
      assertEquals("8", version, "Minimum Java version should be '8'");
    }

    @Test
    @DisplayName("getMaximumJavaVersion should return '22'")
    void getMaximumJavaVersionShouldReturn22() {
      String version = JniRuntimeFactory.getMaximumJavaVersion();
      assertEquals("22", version, "Maximum Java version should be '22'");
    }

    @Test
    @DisplayName("isJavaVersionCompatible should be a public static method")
    void isJavaVersionCompatibleShouldBePublicStatic() throws Exception {
      Method method = JniRuntimeFactory.class.getMethod("isJavaVersionCompatible");
      assertNotNull(method, "isJavaVersionCompatible method should exist");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "isJavaVersionCompatible should be public");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "isJavaVersionCompatible should be static");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(
          0, method.getParameterCount(), "isJavaVersionCompatible should have no parameters");
    }
  }

  // ========================================================================
  // Java Version Parsing Tests
  // ========================================================================

  @Nested
  @DisplayName("Java Version Parsing Tests")
  class JavaVersionParsingTests {

    @Test
    @DisplayName("getMajorJavaVersion should be a private static method")
    void getMajorJavaVersionShouldBePrivateStatic() throws Exception {
      Method method =
          JniRuntimeFactory.class.getDeclaredMethod("getMajorJavaVersion", String.class);
      assertNotNull(method, "getMajorJavaVersion method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "getMajorJavaVersion should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getMajorJavaVersion should be static");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(1, method.getParameterCount(), "getMajorJavaVersion should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("getMajorJavaVersion should parse Java 8 format correctly")
    void shouldParseJava8Format() throws Exception {
      Method method =
          JniRuntimeFactory.class.getDeclaredMethod("getMajorJavaVersion", String.class);
      method.setAccessible(true);

      int version = (int) method.invoke(null, "1.8.0_291");
      assertEquals(8, version, "Should parse '1.8.0_291' as major version 8");

      version = (int) method.invoke(null, "1.7.0_80");
      assertEquals(7, version, "Should parse '1.7.0_80' as major version 7");
    }

    @Test
    @DisplayName("getMajorJavaVersion should parse Java 9+ format correctly")
    void shouldParseJava9PlusFormat() throws Exception {
      Method method =
          JniRuntimeFactory.class.getDeclaredMethod("getMajorJavaVersion", String.class);
      method.setAccessible(true);

      int version = (int) method.invoke(null, "11.0.11");
      assertEquals(11, version, "Should parse '11.0.11' as major version 11");

      version = (int) method.invoke(null, "17.0.1");
      assertEquals(17, version, "Should parse '17.0.1' as major version 17");

      version = (int) method.invoke(null, "21");
      assertEquals(21, version, "Should parse '21' as major version 21");
    }

    @Test
    @DisplayName("getMajorJavaVersion should throw for null input")
    void shouldThrowForNullInput() throws Exception {
      Method method =
          JniRuntimeFactory.class.getDeclaredMethod("getMajorJavaVersion", String.class);
      method.setAccessible(true);

      InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              () -> method.invoke(null, (String) null),
              "Should throw for null input");

      assertTrue(
          exception.getCause() instanceof IllegalArgumentException,
          "Cause should be IllegalArgumentException");
    }

    @Test
    @DisplayName("getMajorJavaVersion should throw for empty input")
    void shouldThrowForEmptyInput() throws Exception {
      Method method =
          JniRuntimeFactory.class.getDeclaredMethod("getMajorJavaVersion", String.class);
      method.setAccessible(true);

      InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              () -> method.invoke(null, ""),
              "Should throw for empty input");

      assertTrue(
          exception.getCause() instanceof IllegalArgumentException,
          "Cause should be IllegalArgumentException");
    }
  }

  // ========================================================================
  // Factory Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Factory Pattern Tests")
  class FactoryPatternTests {

    @Test
    @DisplayName("Factory should provide static-only access")
    void factoryShouldProvideStaticOnlyAccess() {
      // All public methods should be static
      Method[] publicMethods = JniRuntimeFactory.class.getMethods();

      for (Method method : publicMethods) {
        // Skip methods inherited from Object
        if (method.getDeclaringClass() == Object.class) {
          continue;
        }

        assertTrue(
            Modifier.isStatic(method.getModifiers()),
            "Public method " + method.getName() + " should be static");
      }
    }

    @Test
    @DisplayName("Factory should have all required factory methods")
    void factoryShouldHaveAllRequiredFactoryMethods() throws Exception {
      // List of required factory methods
      String[] requiredMethods = {
        "createRuntime",
        "isAvailable",
        "getImplementationName",
        "getWasmtimeVersion",
        "getFactoryInfo",
        "validateEnvironment",
        "getMinimumJavaVersion",
        "getMaximumJavaVersion",
        "isJavaVersionCompatible"
      };

      for (String methodName : requiredMethods) {
        Method method = JniRuntimeFactory.class.getMethod(methodName);
        assertNotNull(method, "Method " + methodName + " should exist");
        assertTrue(
            Modifier.isPublic(method.getModifiers()), "Method " + methodName + " should be public");
        assertTrue(
            Modifier.isStatic(method.getModifiers()), "Method " + methodName + " should be static");
      }
    }
  }

  // ========================================================================
  // Information Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Information Method Tests")
  class InformationMethodTests {

    @Test
    @DisplayName("getFactoryInfo should include implementation name")
    void getFactoryInfoShouldIncludeImplementationName() {
      String info = JniRuntimeFactory.getFactoryInfo();
      assertTrue(info.contains("JNI"), "Factory info should contain implementation name");
    }

    @Test
    @DisplayName("getFactoryInfo should include availability status")
    void getFactoryInfoShouldIncludeAvailabilityStatus() {
      String info = JniRuntimeFactory.getFactoryInfo();
      assertTrue(info.contains("Available"), "Factory info should contain availability status");
    }

    @Test
    @DisplayName("isAvailable should return consistent results")
    void isAvailableShouldReturnConsistentResults() {
      boolean first = JniRuntimeFactory.isAvailable();
      boolean second = JniRuntimeFactory.isAvailable();
      assertEquals(first, second, "isAvailable should return consistent results");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("JniRuntimeFactory should have expected number of public methods")
    void shouldHaveExpectedPublicMethodCount() {
      Method[] allMethods = JniRuntimeFactory.class.getDeclaredMethods();
      int publicStaticCount = 0;

      for (Method method : allMethods) {
        if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
          publicStaticCount++;
        }
      }

      // 9 public static methods expected
      assertEquals(9, publicStaticCount, "JniRuntimeFactory should have 9 public static methods");
    }

    @Test
    @DisplayName("JniRuntimeFactory should have exactly one private method")
    void shouldHaveOnePrivateMethod() {
      Method[] allMethods = JniRuntimeFactory.class.getDeclaredMethods();
      int privateMethodCount = 0;

      for (Method method : allMethods) {
        if (Modifier.isPrivate(method.getModifiers()) && !method.isSynthetic()) {
          privateMethodCount++;
        }
      }

      // 1 private method: getMajorJavaVersion
      assertEquals(
          1,
          privateMethodCount,
          "JniRuntimeFactory should have exactly 1 private method (getMajorJavaVersion)");
    }
  }
}
