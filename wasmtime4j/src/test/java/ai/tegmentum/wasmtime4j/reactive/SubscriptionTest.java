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

package ai.tegmentum.wasmtime4j.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Subscription interface.
 *
 * <p>Subscription is a Java 8 compatible reactive streams interface for controlling flow between
 * Publisher and Subscriber.
 */
@DisplayName("Subscription Interface Tests")
class SubscriptionTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Subscription.class.isInterface(), "Subscription should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Subscription.class.getModifiers()), "Subscription should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(Subscription.class.getModifiers()),
          "Subscription should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should not have type parameters")
    void shouldNotHaveTypeParameters() {
      TypeVariable<?>[] typeParams = Subscription.class.getTypeParameters();
      assertEquals(0, typeParams.length, "Subscription should have no type parameters");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          Subscription.class.getInterfaces().length,
          "Subscription should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have request method")
    void shouldHaveRequestMethod() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("request", long.class);
      assertNotNull(method, "request method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "request should be abstract");
    }

    @Test
    @DisplayName("should have cancel method")
    void shouldHaveCancelMethod() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("cancel");
      assertNotNull(method, "cancel method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "cancel should be abstract");
    }

    @Test
    @DisplayName("should have exactly 2 abstract methods")
    void shouldHaveExactly2AbstractMethods() {
      long abstractMethods =
          Arrays.stream(Subscription.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(2, abstractMethods, "Subscription should have exactly 2 abstract methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(Subscription.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "Subscription should have no default methods");
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
      Set<String> expectedMethods = Set.of("request", "cancel");

      Set<String> actualMethods =
          Arrays.stream(Subscription.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "Subscription should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 2 declared methods")
    void shouldHaveExactly2DeclaredMethods() {
      long methodCount =
          Arrays.stream(Subscription.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(2, methodCount, "Subscription should have exactly 2 declared methods");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethods =
          Arrays.stream(Subscription.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "Subscription should have no static methods");
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
          Subscription.class.getDeclaredFields().length,
          "Subscription should have no declared fields");
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
          Subscription.class.getDeclaredClasses().length,
          "Subscription should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("request should have 1 parameter")
    void requestShouldHave1Parameter() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("request", long.class);
      assertEquals(1, method.getParameterCount(), "request should have 1 parameter");
    }

    @Test
    @DisplayName("request parameter should be long")
    void requestParameterShouldBeLong() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("request", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "request param should be primitive long");
    }

    @Test
    @DisplayName("cancel should have no parameters")
    void cancelShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("cancel");
      assertEquals(0, method.getParameterCount(), "cancel should have no parameters");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(Subscription.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("request should return void")
    void requestShouldReturnVoid() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("request", long.class);
      assertEquals(void.class, method.getReturnType(), "request should return void");
    }

    @Test
    @DisplayName("cancel should return void")
    void cancelShouldReturnVoid() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("cancel");
      assertEquals(void.class, method.getReturnType(), "cancel should return void");
    }

    @Test
    @DisplayName("request parameter should be primitive long")
    void requestParameterShouldBePrimitiveLong() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("request", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(
          long.class, paramTypes[0], "request should take primitive long, not Long wrapper");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("request should not declare any exceptions")
    void requestShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("request", long.class);
      assertEquals(0, method.getExceptionTypes().length, "request should not declare exceptions");
    }

    @Test
    @DisplayName("cancel should not declare any exceptions")
    void cancelShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Subscription.class.getMethod("cancel");
      assertEquals(0, method.getExceptionTypes().length, "cancel should not declare exceptions");
    }
  }
}
