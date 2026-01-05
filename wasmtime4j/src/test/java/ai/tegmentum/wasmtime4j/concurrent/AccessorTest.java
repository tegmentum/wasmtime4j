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

package ai.tegmentum.wasmtime4j.concurrent;

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
 * Comprehensive test suite for the Accessor interface.
 *
 * <p>Accessor provides concurrent access to store data from multiple threads. It enables safe
 * concurrent access to Store data when running multiple WebAssembly guests concurrently.
 *
 * @param <T> the type of user data stored in the associated Store
 */
@DisplayName("Accessor Interface Tests")
class AccessorTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Accessor.class.isInterface(), "Accessor should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Accessor.class.getModifiers()), "Accessor should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(Accessor.class.getModifiers()),
          "Accessor should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should be a generic interface")
    void shouldBeAGenericInterface() {
      TypeVariable<?>[] typeParams = Accessor.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Accessor should have exactly 1 type parameter");
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
          0, Accessor.class.getInterfaces().length, "Accessor should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getData");
      assertNotNull(method, "getData method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (erased T)");
      assertFalse(method.isDefault(), "getData should be abstract");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isValid should be abstract");
    }

    @Test
    @DisplayName("should have getStoreId method")
    void shouldHaveGetStoreIdMethod() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getStoreId");
      assertNotNull(method, "getStoreId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getStoreId should be abstract");
    }

    @Test
    @DisplayName("should have complete method")
    void shouldHaveCompleteMethod() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("complete");
      assertNotNull(method, "complete method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "complete should be abstract");
    }

    @Test
    @DisplayName("should have fail method")
    void shouldHaveFailMethod() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("fail", Throwable.class);
      assertNotNull(method, "fail method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertFalse(method.isDefault(), "fail should be abstract");
    }

    @Test
    @DisplayName("should have exactly 5 abstract methods")
    void shouldHaveExactly5AbstractMethods() {
      long abstractMethods =
          Arrays.stream(Accessor.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(5, abstractMethods, "Accessor should have exactly 5 abstract methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getData with Class parameter as default method")
    void shouldHaveGetDataWithClassParameterAsDefaultMethod() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getData", Class.class);
      assertNotNull(method, "getData(Class) method should exist");
      assertTrue(method.isDefault(), "getData(Class) should be a default method");
    }

    @Test
    @DisplayName("should have exactly 1 default method")
    void shouldHaveExactly1DefaultMethod() {
      long defaultMethods =
          Arrays.stream(Accessor.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(1, defaultMethods, "Accessor should have exactly 1 default method");
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
      Set<String> expectedMethods = Set.of("getData", "isValid", "getStoreId", "complete", "fail");

      Set<String> actualMethods =
          Arrays.stream(Accessor.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Accessor should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 6 declared methods")
    void shouldHaveExactly6DeclaredMethods() {
      long methodCount =
          Arrays.stream(Accessor.class.getDeclaredMethods()).filter(m -> !m.isSynthetic()).count();
      assertEquals(6, methodCount, "Accessor should have exactly 6 declared methods");
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
          Arrays.stream(Accessor.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "Accessor should have no static methods");
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
          0, Accessor.class.getDeclaredFields().length, "Accessor should have no declared fields");
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
          0, Accessor.class.getDeclaredClasses().length, "Accessor should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getData should have no parameters")
    void getDataShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getData");
      assertEquals(0, method.getParameterCount(), "getData should have no parameters");
    }

    @Test
    @DisplayName("getData with Class should have 1 parameter")
    void getDataWithClassShouldHave1Parameter() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getData", Class.class);
      assertEquals(1, method.getParameterCount(), "getData(Class) should have 1 parameter");
    }

    @Test
    @DisplayName("isValid should have no parameters")
    void isValidShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("isValid");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }

    @Test
    @DisplayName("getStoreId should have no parameters")
    void getStoreIdShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getStoreId");
      assertEquals(0, method.getParameterCount(), "getStoreId should have no parameters");
    }

    @Test
    @DisplayName("complete should have no parameters")
    void completeShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("complete");
      assertEquals(0, method.getParameterCount(), "complete should have no parameters");
    }

    @Test
    @DisplayName("fail should have 1 parameter")
    void failShouldHave1Parameter() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("fail", Throwable.class);
      assertEquals(1, method.getParameterCount(), "fail should have 1 parameter");
    }

    @Test
    @DisplayName("fail parameter should be Throwable")
    void failParameterShouldBeThrowable() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("fail", Throwable.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Throwable.class, paramTypes[0], "fail param should be Throwable");
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
      Arrays.stream(Accessor.class.getDeclaredMethods())
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
    @DisplayName("isValid should return primitive boolean")
    void isValidShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("isValid");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isValid should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("getStoreId should return primitive long")
    void getStoreIdShouldReturnPrimitiveLong() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getStoreId");
      assertEquals(
          long.class, method.getReturnType(), "getStoreId should return primitive long, not Long");
    }

    @Test
    @DisplayName("complete should return void")
    void completeShouldReturnVoid() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("complete");
      assertEquals(void.class, method.getReturnType(), "complete should return void");
    }

    @Test
    @DisplayName("fail should return void")
    void failShouldReturnVoid() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("fail", Throwable.class);
      assertEquals(void.class, method.getReturnType(), "fail should return void");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("complete should declare WasmException")
    void completeShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("complete");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "complete should declare 1 exception");
      assertEquals("WasmException", exceptions[0].getSimpleName(), "Should declare WasmException");
    }

    @Test
    @DisplayName("fail should declare WasmException")
    void failShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("fail", Throwable.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "fail should declare 1 exception");
      assertEquals("WasmException", exceptions[0].getSimpleName(), "Should declare WasmException");
    }

    @Test
    @DisplayName("getData should not declare any exceptions")
    void getDataShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getData");
      assertEquals(0, method.getExceptionTypes().length, "getData should not declare exceptions");
    }

    @Test
    @DisplayName("isValid should not declare any exceptions")
    void isValidShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("isValid");
      assertEquals(0, method.getExceptionTypes().length, "isValid should not declare exceptions");
    }

    @Test
    @DisplayName("getStoreId should not declare any exceptions")
    void getStoreIdShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getStoreId");
      assertEquals(
          0, method.getExceptionTypes().length, "getStoreId should not declare exceptions");
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
      TypeVariable<?>[] typeParams = Accessor.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have 1 type parameter");
      assertEquals(1, typeParams[0].getBounds().length, "T should have 1 bound (Object)");
      assertEquals(Object.class, typeParams[0].getBounds()[0], "T's bound should be Object");
    }

    @Test
    @DisplayName("getData return type should be T")
    void getDataReturnTypeShouldBeT() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getData");
      String genericReturn = method.getGenericReturnType().getTypeName();
      assertEquals("T", genericReturn, "getData should return T");
    }

    @Test
    @DisplayName("getData with Class should have generic type parameter")
    void getDataWithClassShouldHaveGenericTypeParameter() throws NoSuchMethodException {
      Method method = Accessor.class.getMethod("getData", Class.class);
      TypeVariable<?>[] typeParams = method.getTypeParameters();
      assertEquals(1, typeParams.length, "getData(Class) should have 1 type parameter");
      assertEquals("U", typeParams[0].getName(), "Type parameter should be U");
    }
  }
}
