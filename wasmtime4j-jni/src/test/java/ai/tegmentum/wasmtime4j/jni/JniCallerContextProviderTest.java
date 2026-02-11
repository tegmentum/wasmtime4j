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

import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.spi.CallerContextProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JniCallerContextProvider class.
 *
 * <p>JniCallerContextProvider is a JNI implementation of CallerContextProvider that delegates to
 * JniHostFunction's ThreadLocal-based caller context mechanism.
 */
@DisplayName("JniCallerContextProvider Class Tests")
class JniCallerContextProviderTest {

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
          JniCallerContextProvider.class.isInterface(),
          "JniCallerContextProvider should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniCallerContextProvider.class.getModifiers()),
          "JniCallerContextProvider should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniCallerContextProvider.class.getModifiers()),
          "JniCallerContextProvider should be final");
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
          JniCallerContextProvider.class.getSuperclass(),
          "JniCallerContextProvider should extend Object");
    }

    @Test
    @DisplayName("should implement CallerContextProvider interface")
    void shouldImplementCallerContextProvider() {
      assertTrue(
          CallerContextProvider.class.isAssignableFrom(JniCallerContextProvider.class),
          "JniCallerContextProvider should implement CallerContextProvider");
    }

    @Test
    @DisplayName("should have exactly 1 interface")
    void shouldHaveExactlyOneInterface() {
      assertEquals(
          1,
          JniCallerContextProvider.class.getInterfaces().length,
          "JniCallerContextProvider should implement exactly 1 interface");
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
      Constructor<?>[] constructors = JniCallerContextProvider.class.getDeclaredConstructors();
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
    @DisplayName("should have getCurrentCaller method")
    void shouldHaveGetCurrentCallerMethod() throws NoSuchMethodException {
      Method method = JniCallerContextProvider.class.getMethod("getCurrentCaller");
      assertNotNull(method, "getCurrentCaller method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(Caller.class, method.getReturnType(), "Should return Caller");
    }

    @Test
    @DisplayName("getCurrentCaller method should match interface signature")
    void getCurrentCallerMethodShouldMatchInterfaceSignature() throws NoSuchMethodException {
      // Verify the method exists and matches the interface method signature
      // Note: @Override annotation may not be retained at runtime with all compilers
      Method method = JniCallerContextProvider.class.getMethod("getCurrentCaller");
      Method interfaceMethod = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertEquals(
          interfaceMethod.getReturnType(),
          method.getReturnType(),
          "Return type should match interface");
    }

    @Test
    @DisplayName("getCurrentCaller should have type parameter T")
    void getCurrentCallerShouldHaveTypeParameter() throws NoSuchMethodException {
      Method method = JniCallerContextProvider.class.getMethod("getCurrentCaller");
      TypeVariable<?>[] typeParams = method.getTypeParameters();
      assertEquals(1, typeParams.length, "getCurrentCaller should have 1 type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("should implement all CallerContextProvider methods")
    void shouldImplementAllCallerContextProviderMethods() throws NoSuchMethodException {
      // CallerContextProvider has getCurrentCaller() method
      Method interfaceMethod = CallerContextProvider.class.getMethod("getCurrentCaller");
      Method classMethod = JniCallerContextProvider.class.getMethod("getCurrentCaller");

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
          java.util.Arrays.stream(JniCallerContextProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      // Should have: getCurrentCaller() method
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
    @DisplayName("should have no non-synthetic declared fields")
    void shouldHaveNoNonSyntheticDeclaredFields() {
      // Filter out synthetic fields (e.g., $jacocoData from code coverage)
      long nonSyntheticCount =
          java.util.Arrays.stream(JniCallerContextProvider.class.getDeclaredFields())
              .filter(f -> !f.isSynthetic())
              .count();
      assertEquals(
          0,
          nonSyntheticCount,
          "JniCallerContextProvider should have no non-synthetic declared fields");
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
          JniCallerContextProvider.class.getDeclaredClasses().length,
          "JniCallerContextProvider should have no nested classes");
    }
  }
}
