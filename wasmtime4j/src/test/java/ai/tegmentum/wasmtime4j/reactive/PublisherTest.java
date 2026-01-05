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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Publisher interface.
 *
 * <p>Publisher is a Java 8 compatible reactive streams interface that provides minimal reactive
 * streams implementation, avoiding the use of java.util.concurrent.Flow which was introduced in
 * Java 9.
 */
@DisplayName("Publisher Interface Tests")
class PublisherTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Publisher.class.isInterface(), "Publisher should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Publisher.class.getModifiers()), "Publisher should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(Publisher.class.getModifiers()),
          "Publisher should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should be a generic interface with 1 type parameter")
    void shouldBeAGenericInterfaceWith1TypeParameter() {
      TypeVariable<?>[] typeParams = Publisher.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Publisher should have exactly 1 type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
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
          0, Publisher.class.getInterfaces().length, "Publisher should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      assertNotNull(method, "subscribe method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "subscribe should be abstract");
    }

    @Test
    @DisplayName("should have exactly 1 abstract method")
    void shouldHaveExactly1AbstractMethod() {
      long abstractMethods =
          Arrays.stream(Publisher.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(1, abstractMethods, "Publisher should have exactly 1 abstract method");
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
          Arrays.stream(Publisher.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "Publisher should have no default methods");
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
      Set<String> expectedMethods = Set.of("subscribe");

      Set<String> actualMethods =
          Arrays.stream(Publisher.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Publisher should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 1 declared method")
    void shouldHaveExactly1DeclaredMethod() {
      long methodCount =
          Arrays.stream(Publisher.class.getDeclaredMethods()).filter(m -> !m.isSynthetic()).count();
      assertEquals(1, methodCount, "Publisher should have exactly 1 declared method");
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
          Arrays.stream(Publisher.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "Publisher should have no static methods");
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
          Publisher.class.getDeclaredFields().length,
          "Publisher should have no declared fields");
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
          Publisher.class.getDeclaredClasses().length,
          "Publisher should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("subscribe should have 1 parameter")
    void subscribeShouldHave1Parameter() throws NoSuchMethodException {
      Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      assertEquals(1, method.getParameterCount(), "subscribe should have 1 parameter");
    }

    @Test
    @DisplayName("subscribe parameter should be Subscriber")
    void subscribeParameterShouldBeSubscriber() throws NoSuchMethodException {
      Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Subscriber.class, paramTypes[0], "subscribe param should be Subscriber");
    }

    @Test
    @DisplayName("subscribe parameter should have wildcard type")
    void subscribeParameterShouldHaveWildcardType() throws NoSuchMethodException {
      Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      Type[] genericParamTypes = method.getGenericParameterTypes();
      assertEquals(1, genericParamTypes.length, "Should have 1 generic param type");

      // The parameter type should be Subscriber<? super T>
      assertTrue(
          genericParamTypes[0] instanceof ParameterizedType,
          "Parameter should be parameterized type");
      ParameterizedType paramType = (ParameterizedType) genericParamTypes[0];
      assertEquals(Subscriber.class, paramType.getRawType(), "Raw type should be Subscriber");

      // Check the wildcard type argument
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertTrue(
          typeArgs[0] instanceof WildcardType, "Type argument should be wildcard (? super T)");

      WildcardType wildcard = (WildcardType) typeArgs[0];
      Type[] lowerBounds = wildcard.getLowerBounds();
      assertEquals(1, lowerBounds.length, "Should have 1 lower bound (super T)");
      assertTrue(lowerBounds[0] instanceof TypeVariable, "Lower bound should be type variable T");
      assertEquals("T", ((TypeVariable<?>) lowerBounds[0]).getName(), "Lower bound should be T");
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
      Arrays.stream(Publisher.class.getDeclaredMethods())
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
    @DisplayName("subscribe should return void")
    void subscribeShouldReturnVoid() throws NoSuchMethodException {
      Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      assertEquals(void.class, method.getReturnType(), "subscribe should return void");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("subscribe should not declare any exceptions")
    void subscribeShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      assertEquals(0, method.getExceptionTypes().length, "subscribe should not declare exceptions");
    }
  }

  // ========================================================================
  // Type Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Parameter Tests")
  class TypeParameterTests {

    @Test
    @DisplayName("type parameter T should have no bounds")
    void typeParameterTShouldHaveNoBounds() {
      TypeVariable<?>[] typeParams = Publisher.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have 1 type parameter");
      assertEquals(1, typeParams[0].getBounds().length, "T should have 1 bound (Object)");
      assertEquals(Object.class, typeParams[0].getBounds()[0], "T's bound should be Object");
    }
  }
}
