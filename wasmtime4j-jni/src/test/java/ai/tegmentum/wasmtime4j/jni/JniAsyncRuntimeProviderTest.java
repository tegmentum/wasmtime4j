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

import ai.tegmentum.wasmtime4j.async.AsyncRuntime;
import ai.tegmentum.wasmtime4j.async.AsyncRuntimeFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JniAsyncRuntimeProvider class.
 *
 * <p>JniAsyncRuntimeProvider is a ServiceLoader provider for JNI AsyncRuntime implementation.
 */
@DisplayName("JniAsyncRuntimeProvider Class Tests")
class JniAsyncRuntimeProviderTest {

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
          JniAsyncRuntimeProvider.class.isInterface(), "JniAsyncRuntimeProvider should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniAsyncRuntimeProvider.class.getModifiers()),
          "JniAsyncRuntimeProvider should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniAsyncRuntimeProvider.class.getModifiers()),
          "JniAsyncRuntimeProvider should be final");
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
          JniAsyncRuntimeProvider.class.getSuperclass(),
          "JniAsyncRuntimeProvider should extend Object");
    }

    @Test
    @DisplayName("should implement AsyncRuntimeFactory.AsyncRuntimeProvider interface")
    void shouldImplementAsyncRuntimeProvider() {
      assertTrue(
          AsyncRuntimeFactory.AsyncRuntimeProvider.class.isAssignableFrom(
              JniAsyncRuntimeProvider.class),
          "JniAsyncRuntimeProvider should implement AsyncRuntimeFactory.AsyncRuntimeProvider");
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
      Field field = JniAsyncRuntimeProvider.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public default constructor")
    void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = JniAsyncRuntimeProvider.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Default constructor should be public");
      assertEquals(
          0, constructor.getParameterCount(), "Default constructor should have no parameters");
    }

    @Test
    @DisplayName("should have exactly one constructor")
    void shouldHaveExactlyOneConstructor() {
      Constructor<?>[] constructors = JniAsyncRuntimeProvider.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have create method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      Method method = JniAsyncRuntimeProvider.class.getMethod("create");
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(AsyncRuntime.class, method.getReturnType(), "Should return AsyncRuntime");
    }

    @Test
    @DisplayName("create should have @Override annotation")
    void createShouldHaveOverrideAnnotation() throws NoSuchMethodException {
      Method method = JniAsyncRuntimeProvider.class.getMethod("create");
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
    @DisplayName("should implement all AsyncRuntimeProvider methods")
    void shouldImplementAllAsyncRuntimeProviderMethods() throws NoSuchMethodException {
      // AsyncRuntimeFactory.AsyncRuntimeProvider has create() method
      Method interfaceMethod = AsyncRuntimeFactory.AsyncRuntimeProvider.class.getMethod("create");
      Method classMethod = JniAsyncRuntimeProvider.class.getMethod("create");

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
          java.util.Arrays.stream(JniAsyncRuntimeProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      // Should have: create() method
      assertTrue(declaredMethodCount >= 1, "Should have at least 1 declared method");
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
          JniAsyncRuntimeProvider.class.getDeclaredClasses().length,
          "JniAsyncRuntimeProvider should have no nested classes");
    }
  }
}
