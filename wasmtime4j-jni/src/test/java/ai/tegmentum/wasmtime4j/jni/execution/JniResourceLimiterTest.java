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

package ai.tegmentum.wasmtime4j.jni.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterConfig;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterStats;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JniResourceLimiter class.
 *
 * <p>JniResourceLimiter provides JNI-based resource limiting for WebAssembly execution.
 */
@DisplayName("JniResourceLimiter Class Tests")
class JniResourceLimiterTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class, not an interface")
    void shouldBeAClass() {
      assertFalse(JniResourceLimiter.class.isInterface(), "JniResourceLimiter should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniResourceLimiter.class.getModifiers()),
          "JniResourceLimiter should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniResourceLimiter.class.getModifiers()),
          "JniResourceLimiter should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() {
      assertEquals(
          JniResource.class,
          JniResourceLimiter.class.getSuperclass(),
          "JniResourceLimiter should extend JniResource");
    }

    @Test
    @DisplayName("should implement ResourceLimiter interface")
    void shouldImplementResourceLimiter() {
      assertTrue(
          ResourceLimiter.class.isAssignableFrom(JniResourceLimiter.class),
          "JniResourceLimiter should implement ResourceLimiter");
    }

    @Test
    @DisplayName("should implement AutoCloseable through ResourceLimiter")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniResourceLimiter.class),
          "JniResourceLimiter should implement AutoCloseable");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniResourceLimiter.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
    }

    @Test
    @DisplayName("should have config field")
    void shouldHaveConfigField() throws NoSuchFieldException {
      Field field = JniResourceLimiter.class.getDeclaredField("config");
      assertNotNull(field, "config field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "config should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "config should be final");
      assertEquals(
          ResourceLimiterConfig.class,
          field.getType(),
          "config should be of type ResourceLimiterConfig");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create(ResourceLimiterConfig) method")
    void shouldHaveStaticCreateWithConfigMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("create", ResourceLimiterConfig.class);
      assertNotNull(method, "create(ResourceLimiterConfig) method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          JniResourceLimiter.class, method.getReturnType(), "Should return JniResourceLimiter");
    }

    @Test
    @DisplayName("should have static createDefault() method")
    void shouldHaveStaticCreateDefaultMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("createDefault");
      assertNotNull(method, "createDefault() method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          JniResourceLimiter.class, method.getReturnType(), "Should return JniResourceLimiter");
    }

    @Test
    @DisplayName("should have static getLimiterCount() method")
    void shouldHaveStaticGetLimiterCountMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("getLimiterCount");
      assertNotNull(method, "getLimiterCount() method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  // ========================================================================
  // Instance Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          ResourceLimiterConfig.class,
          method.getReturnType(),
          "Should return ResourceLimiterConfig");
    }

    @Test
    @DisplayName("should have allowMemoryGrow method")
    void shouldHaveAllowMemoryGrowMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertNotNull(method, "allowMemoryGrow method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(2, method.getParameterCount(), "Should have 2 parameters");
    }

    @Test
    @DisplayName("should have allowTableGrow method")
    void shouldHaveAllowTableGrowMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      assertNotNull(method, "allowTableGrow method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(2, method.getParameterCount(), "Should have 2 parameters");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          ResourceLimiterStats.class, method.getReturnType(), "Should return ResourceLimiterStats");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("resetStats");
      assertNotNull(method, "resetStats method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Protected Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Protected Method Tests")
  class ProtectedMethodTests {

    @Test
    @DisplayName("should have doClose method")
    void shouldHaveDoCloseMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("doClose");
      assertNotNull(method, "doClose method should exist");
      assertTrue(Modifier.isProtected(method.getModifiers()), "Should be protected");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getResourceType method")
    void shouldHaveGetResourceTypeMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("getResourceType");
      assertNotNull(method, "getResourceType method should exist");
      assertTrue(Modifier.isProtected(method.getModifiers()), "Should be protected");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  // ========================================================================
  // Private Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("should have parseStatsJson private method")
    void shouldHaveParseStatsJsonMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("parseStatsJson", String.class);
      assertNotNull(method, "parseStatsJson method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(
          ResourceLimiterStats.class, method.getReturnType(), "Should return ResourceLimiterStats");
    }

    @Test
    @DisplayName("should have extractLongValue private method")
    void shouldHaveExtractLongValueMethod() throws NoSuchMethodException {
      Method method =
          JniResourceLimiter.class.getDeclaredMethod(
              "extractLongValue", String.class, String.class);
      assertNotNull(method, "extractLongValue method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  // ========================================================================
  // Native Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Native Method Tests")
  class NativeMethodTests {

    @Test
    @DisplayName("should have nativeCreate native method")
    void shouldHaveNativeCreateMethod() throws NoSuchMethodException {
      Method method =
          JniResourceLimiter.class.getDeclaredMethod(
              "nativeCreate", long.class, long.class, long.class, int.class, int.class, int.class);
      assertNotNull(method, "nativeCreate method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have nativeCreateDefault native method")
    void shouldHaveNativeCreateDefaultMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("nativeCreateDefault");
      assertNotNull(method, "nativeCreateDefault method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have nativeFree native method")
    void shouldHaveNativeFreeMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("nativeFree", long.class);
      assertNotNull(method, "nativeFree method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have nativeAllowMemoryGrow native method")
    void shouldHaveNativeAllowMemoryGrowMethod() throws NoSuchMethodException {
      Method method =
          JniResourceLimiter.class.getDeclaredMethod(
              "nativeAllowMemoryGrow", long.class, long.class, long.class);
      assertNotNull(method, "nativeAllowMemoryGrow method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have nativeAllowTableGrow native method")
    void shouldHaveNativeAllowTableGrowMethod() throws NoSuchMethodException {
      Method method =
          JniResourceLimiter.class.getDeclaredMethod(
              "nativeAllowTableGrow", long.class, long.class, long.class);
      assertNotNull(method, "nativeAllowTableGrow method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have nativeGetStatsJson native method")
    void shouldHaveNativeGetStatsJsonMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("nativeGetStatsJson", long.class);
      assertNotNull(method, "nativeGetStatsJson method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have nativeResetStats native method")
    void shouldHaveNativeResetStatsMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("nativeResetStats", long.class);
      assertNotNull(method, "nativeResetStats method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have nativeGetCount native method")
    void shouldHaveNativeGetCountMethod() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("nativeGetCount");
      assertNotNull(method, "nativeGetCount method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() {
      Set<String> expectedPublicMethods =
          new HashSet<>(
              Arrays.asList(
                  "create",
                  "createDefault",
                  "getLimiterCount",
                  "getId",
                  "getConfig",
                  "allowMemoryGrow",
                  "allowTableGrow",
                  "getStats",
                  "resetStats"));

      Set<String> actualPublicMethods =
          Arrays.stream(JniResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(
                  m -> Modifier.isStatic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedPublicMethods) {
        assertTrue(
            actualPublicMethods.contains(expected), "Should have public method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 8 native methods")
    void shouldHaveExactly8NativeMethods() {
      long nativeCount =
          Arrays.stream(JniResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isNative(m.getModifiers()))
              .count();
      assertEquals(8, nativeCount, "Should have exactly 8 native methods");
    }

    @Test
    @DisplayName("should have exactly 3 static factory/utility methods")
    void shouldHaveExactly3StaticPublicMethods() {
      long staticPublicCount =
          Arrays.stream(JniResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(3, staticPublicCount, "Should have exactly 3 static public methods");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("should implement all ResourceLimiter methods")
    void shouldImplementAllResourceLimiterMethods() {
      Set<String> interfaceMethods =
          Arrays.stream(ResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isDefault())
              .map(Method::getName)
              .collect(Collectors.toSet());

      Set<String> classMethods =
          Arrays.stream(JniResourceLimiter.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      // Also include methods from parent class
      Set<String> parentMethods =
          Arrays.stream(JniResourceLimiter.class.getSuperclass().getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      Set<String> allMethods = new java.util.HashSet<>(classMethods);
      allMethods.addAll(parentMethods);

      for (String expected : interfaceMethods) {
        assertTrue(
            allMethods.contains(expected), "Should implement ResourceLimiter method: " + expected);
      }
    }

    @Test
    @DisplayName("getId should have @Override annotation")
    void getIdShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("getId");
      assertTrue(
          method.isAnnotationPresent(Override.class), "getId should have @Override annotation");
    }

    @Test
    @DisplayName("getConfig should have @Override annotation")
    void getConfigShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("getConfig");
      assertTrue(
          method.isAnnotationPresent(Override.class), "getConfig should have @Override annotation");
    }

    @Test
    @DisplayName("allowMemoryGrow should have @Override annotation")
    void allowMemoryGrowShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertTrue(
          method.isAnnotationPresent(Override.class),
          "allowMemoryGrow should have @Override annotation");
    }

    @Test
    @DisplayName("allowTableGrow should have @Override annotation")
    void allowTableGrowShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      assertTrue(
          method.isAnnotationPresent(Override.class),
          "allowTableGrow should have @Override annotation");
    }

    @Test
    @DisplayName("getStats should have @Override annotation")
    void getStatsShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("getStats");
      assertTrue(
          method.isAnnotationPresent(Override.class), "getStats should have @Override annotation");
    }

    @Test
    @DisplayName("resetStats should have @Override annotation")
    void resetStatsShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getMethod("resetStats");
      assertTrue(
          method.isAnnotationPresent(Override.class),
          "resetStats should have @Override annotation");
    }
  }

  // ========================================================================
  // JniResource Override Tests
  // ========================================================================

  @Nested
  @DisplayName("JniResource Override Tests")
  class JniResourceOverrideTests {

    @Test
    @DisplayName("doClose should have @Override annotation")
    void doCloseShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("doClose");
      assertTrue(
          method.isAnnotationPresent(Override.class), "doClose should have @Override annotation");
    }

    @Test
    @DisplayName("getResourceType should have @Override annotation")
    void getResourceTypeShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniResourceLimiter.class.getDeclaredMethod("getResourceType");
      assertTrue(
          method.isAnnotationPresent(Override.class),
          "getResourceType should have @Override annotation");
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
      final Constructor<?>[] constructors = JniResourceLimiter.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      final Constructor<?> constructor = constructors[0];
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

      final Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(
          ResourceLimiterConfig.class,
          paramTypes[1],
          "Second parameter should be ResourceLimiterConfig");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no public nested classes")
    void shouldHaveNoPublicNestedClasses() {
      long publicNestedCount =
          Arrays.stream(JniResourceLimiter.class.getDeclaredClasses())
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .count();
      assertEquals(0, publicNestedCount, "Should have no public nested classes");
    }
  }
}
