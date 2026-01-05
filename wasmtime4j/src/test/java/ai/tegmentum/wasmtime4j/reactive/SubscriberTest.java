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
 * Comprehensive test suite for the Subscriber interface.
 *
 * <p>Subscriber is a Java 8 compatible reactive streams interface that receives elements from a
 * Publisher.
 */
@DisplayName("Subscriber Interface Tests")
class SubscriberTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Subscriber.class.isInterface(), "Subscriber should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Subscriber.class.getModifiers()), "Subscriber should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(Subscriber.class.getModifiers()),
          "Subscriber should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should be a generic interface with 1 type parameter")
    void shouldBeAGenericInterfaceWith1TypeParameter() {
      TypeVariable<?>[] typeParams = Subscriber.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Subscriber should have exactly 1 type parameter");
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
          0,
          Subscriber.class.getInterfaces().length,
          "Subscriber should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have onSubscribe method")
    void shouldHaveOnSubscribeMethod() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onSubscribe", Subscription.class);
      assertNotNull(method, "onSubscribe method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "onSubscribe should be abstract");
    }

    @Test
    @DisplayName("should have onNext method")
    void shouldHaveOnNextMethod() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onNext", Object.class);
      assertNotNull(method, "onNext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "onNext should be abstract");
    }

    @Test
    @DisplayName("should have onError method")
    void shouldHaveOnErrorMethod() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onError", Throwable.class);
      assertNotNull(method, "onError method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "onError should be abstract");
    }

    @Test
    @DisplayName("should have onComplete method")
    void shouldHaveOnCompleteMethod() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onComplete");
      assertNotNull(method, "onComplete method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "onComplete should be abstract");
    }

    @Test
    @DisplayName("should have exactly 4 abstract methods")
    void shouldHaveExactly4AbstractMethods() {
      long abstractMethods =
          Arrays.stream(Subscriber.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(4, abstractMethods, "Subscriber should have exactly 4 abstract methods");
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
          Arrays.stream(Subscriber.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "Subscriber should have no default methods");
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
      Set<String> expectedMethods = Set.of("onSubscribe", "onNext", "onError", "onComplete");

      Set<String> actualMethods =
          Arrays.stream(Subscriber.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Subscriber should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactly4DeclaredMethods() {
      long methodCount =
          Arrays.stream(Subscriber.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(4, methodCount, "Subscriber should have exactly 4 declared methods");
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
          Arrays.stream(Subscriber.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "Subscriber should have no static methods");
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
          Subscriber.class.getDeclaredFields().length,
          "Subscriber should have no declared fields");
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
          Subscriber.class.getDeclaredClasses().length,
          "Subscriber should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("onSubscribe should have 1 parameter")
    void onSubscribeShouldHave1Parameter() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onSubscribe", Subscription.class);
      assertEquals(1, method.getParameterCount(), "onSubscribe should have 1 parameter");
    }

    @Test
    @DisplayName("onSubscribe parameter should be Subscription")
    void onSubscribeParameterShouldBeSubscription() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onSubscribe", Subscription.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Subscription.class, paramTypes[0], "onSubscribe param should be Subscription");
    }

    @Test
    @DisplayName("onNext should have 1 parameter")
    void onNextShouldHave1Parameter() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onNext", Object.class);
      assertEquals(1, method.getParameterCount(), "onNext should have 1 parameter");
    }

    @Test
    @DisplayName("onNext parameter should be T (Object erased)")
    void onNextParameterShouldBeT() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onNext", Object.class);
      String genericParam = method.getGenericParameterTypes()[0].getTypeName();
      assertEquals("T", genericParam, "onNext param should be T");
    }

    @Test
    @DisplayName("onError should have 1 parameter")
    void onErrorShouldHave1Parameter() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onError", Throwable.class);
      assertEquals(1, method.getParameterCount(), "onError should have 1 parameter");
    }

    @Test
    @DisplayName("onError parameter should be Throwable")
    void onErrorParameterShouldBeThrowable() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onError", Throwable.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Throwable.class, paramTypes[0], "onError param should be Throwable");
    }

    @Test
    @DisplayName("onComplete should have no parameters")
    void onCompleteShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onComplete");
      assertEquals(0, method.getParameterCount(), "onComplete should have no parameters");
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
      Arrays.stream(Subscriber.class.getDeclaredMethods())
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
    @DisplayName("onSubscribe should return void")
    void onSubscribeShouldReturnVoid() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onSubscribe", Subscription.class);
      assertEquals(void.class, method.getReturnType(), "onSubscribe should return void");
    }

    @Test
    @DisplayName("onNext should return void")
    void onNextShouldReturnVoid() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onNext", Object.class);
      assertEquals(void.class, method.getReturnType(), "onNext should return void");
    }

    @Test
    @DisplayName("onError should return void")
    void onErrorShouldReturnVoid() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onError", Throwable.class);
      assertEquals(void.class, method.getReturnType(), "onError should return void");
    }

    @Test
    @DisplayName("onComplete should return void")
    void onCompleteShouldReturnVoid() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onComplete");
      assertEquals(void.class, method.getReturnType(), "onComplete should return void");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("onSubscribe should not declare any exceptions")
    void onSubscribeShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onSubscribe", Subscription.class);
      assertEquals(
          0, method.getExceptionTypes().length, "onSubscribe should not declare exceptions");
    }

    @Test
    @DisplayName("onNext should not declare any exceptions")
    void onNextShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onNext", Object.class);
      assertEquals(0, method.getExceptionTypes().length, "onNext should not declare exceptions");
    }

    @Test
    @DisplayName("onError should not declare any exceptions")
    void onErrorShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onError", Throwable.class);
      assertEquals(0, method.getExceptionTypes().length, "onError should not declare exceptions");
    }

    @Test
    @DisplayName("onComplete should not declare any exceptions")
    void onCompleteShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Subscriber.class.getMethod("onComplete");
      assertEquals(
          0, method.getExceptionTypes().length, "onComplete should not declare exceptions");
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
      TypeVariable<?>[] typeParams = Subscriber.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have 1 type parameter");
      assertEquals(1, typeParams[0].getBounds().length, "T should have 1 bound (Object)");
      assertEquals(Object.class, typeParams[0].getBounds()[0], "T's bound should be Object");
    }
  }
}
