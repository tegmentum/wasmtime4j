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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JniModuleCacheProvider class.
 *
 * <p>JniModuleCacheProvider is a JNI provider for ModuleCache that is registered via
 * META-INF/services for ServiceLoader discovery.
 */
@DisplayName("JniModuleCacheProvider Class Tests")
class JniModuleCacheProviderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class, not an interface")
    void shouldBeAClass() {
      assertFalse(
          JniModuleCacheProvider.class.isInterface(), "JniModuleCacheProvider should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniModuleCacheProvider.class.getModifiers()),
          "JniModuleCacheProvider should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniModuleCacheProvider.class.getModifiers()),
          "JniModuleCacheProvider should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class,
          JniModuleCacheProvider.class.getSuperclass(),
          "JniModuleCacheProvider should extend Object");
    }

    @Test
    @DisplayName("should implement ModuleCacheFactory.ModuleCacheProvider interface")
    void shouldImplementModuleCacheProvider() {
      assertTrue(
          ModuleCacheFactory.ModuleCacheProvider.class.isAssignableFrom(
              JniModuleCacheProvider.class),
          "JniModuleCacheProvider should implement ModuleCacheFactory.ModuleCacheProvider");
    }

    @Test
    @DisplayName("should have exactly 1 interface")
    void shouldHaveExactlyOneInterface() {
      assertEquals(
          1,
          JniModuleCacheProvider.class.getInterfaces().length,
          "JniModuleCacheProvider should implement exactly 1 interface");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have implicit default constructor")
    void shouldHaveDefaultConstructor() {
      Constructor<?>[] constructors = JniModuleCacheProvider.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Default constructor should be public");
      assertEquals(
          0, constructor.getParameterCount(), "Default constructor should have no parameters");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have create method with correct signature")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      Method method =
          JniModuleCacheProvider.class.getMethod("create", Engine.class, ModuleCacheConfig.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(ModuleCache.class, method.getReturnType(), "Should return ModuleCache");
    }

    @Test
    @DisplayName("create should have 2 parameters")
    void createShouldHaveTwoParameters() throws NoSuchMethodException {
      Method method =
          JniModuleCacheProvider.class.getMethod("create", Engine.class, ModuleCacheConfig.class);
      assertEquals(2, method.getParameterCount(), "create should have 2 parameters");

      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Engine.class, paramTypes[0], "First parameter should be Engine");
      assertEquals(
          ModuleCacheConfig.class, paramTypes[1], "Second parameter should be ModuleCacheConfig");
    }

    @Test
    @DisplayName("create should have @Override annotation")
    void createShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method =
          JniModuleCacheProvider.class.getMethod("create", Engine.class, ModuleCacheConfig.class);
      assertTrue(
          method.isAnnotationPresent(Override.class), "create should have @Override annotation");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("should implement all ModuleCacheProvider methods")
    void shouldImplementAllModuleCacheProviderMethods() throws NoSuchMethodException {
      // ModuleCacheFactory.ModuleCacheProvider has create(Engine, ModuleCacheConfig) method
      Method interfaceMethod =
          ModuleCacheFactory.ModuleCacheProvider.class.getMethod(
              "create", Engine.class, ModuleCacheConfig.class);
      Method classMethod =
          JniModuleCacheProvider.class.getMethod("create", Engine.class, ModuleCacheConfig.class);

      assertEquals(
          interfaceMethod.getReturnType(),
          classMethod.getReturnType(),
          "Return type should match interface");
      assertEquals(
          interfaceMethod.getParameterCount(),
          classMethod.getParameterCount(),
          "Parameter count should match interface");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected declared methods")
    void shouldHaveExpectedDeclaredMethods() {
      long declaredMethodCount =
          java.util.Arrays.stream(JniModuleCacheProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      // Should have: create() method
      assertEquals(1, declaredMethodCount, "Should have exactly 1 declared method");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          JniModuleCacheProvider.class.getDeclaredFields().length,
          "JniModuleCacheProvider should have no declared fields");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          JniModuleCacheProvider.class.getDeclaredClasses().length,
          "JniModuleCacheProvider should have no nested classes");
    }
  }
}
