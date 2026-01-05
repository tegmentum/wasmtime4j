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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiPreview2Config class.
 *
 * <p>WasiPreview2Config provides configuration for WASI Preview 2 support in the Component Model.
 * This test verifies the class structure and method signatures.
 */
@DisplayName("WasiPreview2Config Class Tests")
class WasiPreview2ConfigTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!WasiPreview2Config.class.isInterface(), "WasiPreview2Config should be a class");
      assertTrue(!WasiPreview2Config.class.isEnum(), "WasiPreview2Config should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiPreview2Config.class.getModifiers()),
          "WasiPreview2Config should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiPreview2Config.class.getModifiers()),
          "WasiPreview2Config should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = WasiPreview2Config.class.getDeclaredConstructors();
      assertTrue(constructors.length >= 1, "WasiPreview2Config should have at least 1 constructor");
      for (Constructor<?> constructor : constructors) {
        assertTrue(
            Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private (use builder pattern)");
      }
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have static minimal method")
    void shouldHaveStaticMinimalMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("minimal");
      assertNotNull(method, "minimal method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "minimal should be static");
      assertEquals(
          WasiPreview2Config.class,
          method.getReturnType(),
          "minimal should return WasiPreview2Config");
    }

    @Test
    @DisplayName("should have static inherited method")
    void shouldHaveStaticInheritedMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("inherited");
      assertNotNull(method, "inherited method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "inherited should be static");
      assertEquals(
          WasiPreview2Config.class,
          method.getReturnType(),
          "inherited should return WasiPreview2Config");
    }

    @Test
    @DisplayName("static factory methods should have no parameters")
    void staticFactoryMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("builder").getParameterCount(),
          "builder should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("minimal").getParameterCount(),
          "minimal should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("inherited").getParameterCount(),
          "inherited should have 0 params");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getArgs method")
    void shouldHaveGetArgsMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("getArgs");
      assertNotNull(method, "getArgs method should exist");
      assertEquals(List.class, method.getReturnType(), "getArgs should return List");
    }

    @Test
    @DisplayName("should have getEnv method")
    void shouldHaveGetEnvMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("getEnv");
      assertNotNull(method, "getEnv method should exist");
      assertEquals(Map.class, method.getReturnType(), "getEnv should return Map");
    }

    @Test
    @DisplayName("should have isInheritEnv method")
    void shouldHaveIsInheritEnvMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("isInheritEnv");
      assertNotNull(method, "isInheritEnv method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInheritEnv should return boolean");
    }

    @Test
    @DisplayName("should have isInheritStdio method")
    void shouldHaveIsInheritStdioMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("isInheritStdio");
      assertNotNull(method, "isInheritStdio method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInheritStdio should return boolean");
    }

    @Test
    @DisplayName("should have getPreopenDirs method")
    void shouldHaveGetPreopenDirsMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("getPreopenDirs");
      assertNotNull(method, "getPreopenDirs method should exist");
      assertEquals(List.class, method.getReturnType(), "getPreopenDirs should return List");
    }

    @Test
    @DisplayName("should have isAllowNetwork method")
    void shouldHaveIsAllowNetworkMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("isAllowNetwork");
      assertNotNull(method, "isAllowNetwork method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isAllowNetwork should return boolean");
    }

    @Test
    @DisplayName("should have isAllowClock method")
    void shouldHaveIsAllowClockMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("isAllowClock");
      assertNotNull(method, "isAllowClock method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isAllowClock should return boolean");
    }

    @Test
    @DisplayName("should have isAllowRandom method")
    void shouldHaveIsAllowRandomMethod() throws NoSuchMethodException {
      final Method method = WasiPreview2Config.class.getMethod("isAllowRandom");
      assertNotNull(method, "isAllowRandom method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isAllowRandom should return boolean");
    }

    @Test
    @DisplayName("all accessor methods should have no parameters")
    void allAccessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("getArgs").getParameterCount(),
          "getArgs should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("getEnv").getParameterCount(),
          "getEnv should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("isInheritEnv").getParameterCount(),
          "isInheritEnv should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("isInheritStdio").getParameterCount(),
          "isInheritStdio should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("getPreopenDirs").getParameterCount(),
          "getPreopenDirs should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("isAllowNetwork").getParameterCount(),
          "isAllowNetwork should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("isAllowClock").getParameterCount(),
          "isAllowClock should have 0 params");
      assertEquals(
          0,
          WasiPreview2Config.class.getMethod("isAllowRandom").getParameterCount(),
          "isAllowRandom should have 0 params");
    }
  }

  // ========================================================================
  // Nested PreopenDir Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested PreopenDir Class Tests")
  class NestedPreopenDirClassTests {

    @Test
    @DisplayName("should have public static final nested PreopenDir class")
    void shouldHavePublicStaticFinalNestedPreopenDirClass() {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      boolean hasPreopenDir = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("PreopenDir")) {
          hasPreopenDir = true;
          assertTrue(Modifier.isPublic(clazz.getModifiers()), "PreopenDir should be public");
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "PreopenDir should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "PreopenDir should be final");
        }
      }
      assertTrue(hasPreopenDir, "WasiPreview2Config should have a nested PreopenDir class");
    }

    @Test
    @DisplayName("PreopenDir should have getHostPath method")
    void preopenDirShouldHaveGetHostPathMethod() throws NoSuchMethodException {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("PreopenDir")) {
          Method method = clazz.getMethod("getHostPath");
          assertNotNull(method, "PreopenDir should have getHostPath method");
          assertEquals(Path.class, method.getReturnType(), "getHostPath should return Path");
        }
      }
    }

    @Test
    @DisplayName("PreopenDir should have getGuestPath method")
    void preopenDirShouldHaveGetGuestPathMethod() throws NoSuchMethodException {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("PreopenDir")) {
          Method method = clazz.getMethod("getGuestPath");
          assertNotNull(method, "PreopenDir should have getGuestPath method");
          assertEquals(String.class, method.getReturnType(), "getGuestPath should return String");
        }
      }
    }

    @Test
    @DisplayName("PreopenDir should have isReadOnly method")
    void preopenDirShouldHaveIsReadOnlyMethod() throws NoSuchMethodException {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("PreopenDir")) {
          Method method = clazz.getMethod("isReadOnly");
          assertNotNull(method, "PreopenDir should have isReadOnly method");
          assertEquals(boolean.class, method.getReturnType(), "isReadOnly should return boolean");
        }
      }
    }
  }

  // ========================================================================
  // Nested Builder Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Builder Class Tests")
  class NestedBuilderClassTests {

    @Test
    @DisplayName("should have public static final nested Builder class")
    void shouldHavePublicStaticFinalNestedBuilderClass() {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      boolean hasBuilder = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          hasBuilder = true;
          assertTrue(Modifier.isPublic(clazz.getModifiers()), "Builder should be public");
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "Builder should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "Builder should be final");
        }
      }
      assertTrue(hasBuilder, "WasiPreview2Config should have a nested Builder class");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Method buildMethod = clazz.getMethod("build");
          assertNotNull(buildMethod, "Builder should have build method");
          assertEquals(
              WasiPreview2Config.class,
              buildMethod.getReturnType(),
              "build should return WasiPreview2Config");
        }
      }
    }

    @Test
    @DisplayName("Builder should have fluent setter methods")
    void builderShouldHaveFluentSetterMethods() {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Set<String> expectedMethods =
              Set.of(
                  "args",
                  "addArgs",
                  "env",
                  "inheritEnv",
                  "inheritStdio",
                  "preopenDir",
                  "allowNetwork",
                  "allowClock",
                  "allowRandom");
          Set<String> actualMethods =
              Arrays.stream(clazz.getDeclaredMethods())
                  .map(Method::getName)
                  .collect(Collectors.toSet());
          for (String expected : expectedMethods) {
            assertTrue(actualMethods.contains(expected), "Builder should have method: " + expected);
          }
        }
      }
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getArgs",
              "getEnv",
              "isInheritEnv",
              "isInheritStdio",
              "getPreopenDirs",
              "isAllowNetwork",
              "isAllowClock",
              "isAllowRandom",
              "builder",
              "minimal",
              "inherited");

      Set<String> actualMethods =
          Arrays.stream(WasiPreview2Config.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "WasiPreview2Config should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private final fields")
    void shouldHavePrivateFinalFields() {
      Set<String> expectedFields =
          Set.of(
              "args",
              "env",
              "inheritEnv",
              "inheritStdio",
              "preopenDirs",
              "allowNetwork",
              "allowClock",
              "allowRandom");

      for (String fieldName : expectedFields) {
        try {
          Field field = WasiPreview2Config.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), fieldName + " field should be final");
        } catch (NoSuchFieldException e) {
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }

    @Test
    @DisplayName("args field should be of type List")
    void argsFieldShouldBeOfTypeList() throws NoSuchFieldException {
      Field field = WasiPreview2Config.class.getDeclaredField("args");
      assertEquals(List.class, field.getType(), "args field should be of type List");
    }

    @Test
    @DisplayName("env field should be of type Map")
    void envFieldShouldBeOfTypeMap() throws NoSuchFieldException {
      Field field = WasiPreview2Config.class.getDeclaredField("env");
      assertEquals(Map.class, field.getType(), "env field should be of type Map");
    }

    @Test
    @DisplayName("boolean fields should be of type boolean")
    void booleanFieldsShouldBeOfTypeBoolean() {
      Set<String> booleanFields =
          Set.of("inheritEnv", "inheritStdio", "allowNetwork", "allowClock", "allowRandom");

      for (String fieldName : booleanFields) {
        try {
          Field field = WasiPreview2Config.class.getDeclaredField(fieldName);
          assertEquals(
              boolean.class, field.getType(), fieldName + " field should be of type boolean");
        } catch (NoSuchFieldException e) {
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }
  }

  // ========================================================================
  // Nested Classes Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Count Tests")
  class NestedClassesCountTests {

    @Test
    @DisplayName("should have exactly 2 nested classes")
    void shouldHaveExactly2NestedClasses() {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      assertEquals(
          2, declaredClasses.length, "WasiPreview2Config should have exactly 2 nested classes");
    }

    @Test
    @DisplayName("nested classes should be PreopenDir and Builder")
    void nestedClassesShouldBePreopenDirAndBuilder() {
      Class<?>[] declaredClasses = WasiPreview2Config.class.getDeclaredClasses();
      Set<String> classNames =
          Arrays.stream(declaredClasses).map(Class::getSimpleName).collect(Collectors.toSet());
      assertTrue(classNames.contains("PreopenDir"), "Should have PreopenDir nested class");
      assertTrue(classNames.contains("Builder"), "Should have Builder nested class");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object directly")
    void shouldExtendObjectDirectly() {
      assertEquals(
          Object.class,
          WasiPreview2Config.class.getSuperclass(),
          "WasiPreview2Config should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          WasiPreview2Config.class.getInterfaces().length,
          "WasiPreview2Config should not implement any interfaces");
    }
  }
}
