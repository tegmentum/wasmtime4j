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

package ai.tegmentum.wasmtime4j.ref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
 * Comprehensive test suite for the NoExtern class.
 *
 * <p>NoExtern represents the noextern heap type - the bottom type in the extern type hierarchy. It
 * is an uninhabited type that serves for type system completeness in the WebAssembly GC proposal.
 */
@DisplayName("NoExtern Class Tests")
class NoExternTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(NoExtern.class.isInterface(), "NoExtern should be a class, not an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(NoExtern.class.getModifiers()), "NoExtern should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(NoExtern.class.getModifiers()), "NoExtern should be final");
    }

    @Test
    @DisplayName("should not have type parameters")
    void shouldNotHaveTypeParameters() {
      TypeVariable<?>[] typeParams = NoExtern.class.getTypeParameters();
      assertEquals(0, typeParams.length, "NoExtern should have no type parameters");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should implement HeapType")
    void shouldImplementHeapType() {
      Class<?>[] interfaces = NoExtern.class.getInterfaces();
      assertEquals(1, interfaces.length, "NoExtern should implement exactly 1 interface");
      assertEquals(HeapType.class, interfaces[0], "NoExtern should implement HeapType");
    }

    @Test
    @DisplayName("should be assignable to HeapType")
    void shouldBeAssignableToHeapType() {
      assertTrue(
          HeapType.class.isAssignableFrom(NoExtern.class),
          "HeapType should be assignable from NoExtern");
    }
  }

  // ========================================================================
  // Static Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Field Tests")
  class StaticFieldTests {

    @Test
    @DisplayName("should have INSTANCE static field")
    void shouldHaveInstanceField() throws NoSuchFieldException {
      Field field = NoExtern.class.getField("INSTANCE");
      assertNotNull(field, "INSTANCE field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "INSTANCE should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "INSTANCE should be final");
      assertTrue(Modifier.isPublic(field.getModifiers()), "INSTANCE should be public");
      assertEquals(NoExtern.class, field.getType(), "INSTANCE should be of type NoExtern");
    }

    @Test
    @DisplayName("should have exactly 1 public static field")
    void shouldHaveExactly1PublicStaticField() {
      long staticFields =
          Arrays.stream(NoExtern.class.getDeclaredFields())
              .filter(f -> Modifier.isStatic(f.getModifiers()))
              .filter(f -> Modifier.isPublic(f.getModifiers()))
              .count();
      assertEquals(1, staticFields, "NoExtern should have exactly 1 public static field");
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
      Constructor<?>[] constructors = NoExtern.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "NoExtern should have exactly 1 constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("constructor should have no parameters")
    void constructorShouldHaveNoParameters() {
      Constructor<?>[] constructors = NoExtern.class.getDeclaredConstructors();
      assertEquals(0, constructors[0].getParameterCount(), "Constructor should have no parameters");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have getInstance static method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getInstance should be static");
      assertEquals(NoExtern.class, method.getReturnType(), "getInstance should return NoExtern");
      assertEquals(0, method.getParameterCount(), "getInstance should have no parameters");
    }

    @Test
    @DisplayName("should have exactly 1 static method")
    void shouldHaveExactly1StaticMethod() {
      long staticMethods =
          Arrays.stream(NoExtern.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(1, staticMethods, "NoExtern should have exactly 1 static method");
    }
  }

  // ========================================================================
  // Instance Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertFalse(Modifier.isStatic(method.getModifiers()), "getValueType should be instance");
    }

    @Test
    @DisplayName("should have isNullable method")
    void shouldHaveIsNullableMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isNullable");
      assertNotNull(method, "isNullable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isBottom method")
    void shouldHaveIsBottomMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isBottom");
      assertNotNull(method, "isBottom method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSubtypeOf method")
    void shouldHaveIsSubtypeOfMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isSubtypeOf", HeapType.class);
      assertNotNull(method, "isSubtypeOf method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have getTypeName method")
    void shouldHaveGetTypeNameMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getTypeName");
      assertNotNull(method, "getTypeName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
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
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getInstance",
              "getValueType",
              "isNullable",
              "isBottom",
              "isSubtypeOf",
              "getTypeName",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(NoExtern.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "NoExtern should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 9 declared methods")
    void shouldHaveExactly9DeclaredMethods() {
      // 1 static (getInstance) + 8 instance methods
      long methodCount =
          Arrays.stream(NoExtern.class.getDeclaredMethods()).filter(m -> !m.isSynthetic()).count();
      assertEquals(9, methodCount, "NoExtern should have exactly 9 declared methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have exactly 1 declared field")
    void shouldHaveExactly1DeclaredField() {
      assertEquals(
          1, NoExtern.class.getDeclaredFields().length, "NoExtern should have exactly 1 field");
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
          0, NoExtern.class.getDeclaredClasses().length, "NoExtern should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getInstance should have no parameters")
    void getInstanceShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getInstance");
      assertEquals(0, method.getParameterCount(), "getInstance should have no parameters");
    }

    @Test
    @DisplayName("getValueType should have no parameters")
    void getValueTypeShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getValueType");
      assertEquals(0, method.getParameterCount(), "getValueType should have no parameters");
    }

    @Test
    @DisplayName("isNullable should have no parameters")
    void isNullableShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isNullable");
      assertEquals(0, method.getParameterCount(), "isNullable should have no parameters");
    }

    @Test
    @DisplayName("isBottom should have no parameters")
    void isBottomShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isBottom");
      assertEquals(0, method.getParameterCount(), "isBottom should have no parameters");
    }

    @Test
    @DisplayName("isSubtypeOf should have 1 parameter")
    void isSubtypeOfShouldHave1Parameter() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isSubtypeOf", HeapType.class);
      assertEquals(1, method.getParameterCount(), "isSubtypeOf should have 1 parameter");
    }

    @Test
    @DisplayName("getTypeName should have no parameters")
    void getTypeNameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getTypeName");
      assertEquals(0, method.getParameterCount(), "getTypeName should have no parameters");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all declared methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(NoExtern.class.getDeclaredMethods())
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
    @DisplayName("isNullable should return primitive boolean")
    void isNullableShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isNullable");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isNullable should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("isBottom should return primitive boolean")
    void isBottomShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isBottom");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isBottom should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("isSubtypeOf should return primitive boolean")
    void isSubtypeOfShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isSubtypeOf", HeapType.class);
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isSubtypeOf should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("hashCode should return primitive int")
    void hashCodeShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("hashCode");
      assertEquals(
          int.class, method.getReturnType(), "hashCode should return primitive int, not Integer");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("getInstance should not declare any exceptions")
    void getInstanceShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getInstance");
      assertEquals(
          0, method.getExceptionTypes().length, "getInstance should not declare exceptions");
    }

    @Test
    @DisplayName("getValueType should not declare any exceptions")
    void getValueTypeShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("getValueType");
      assertEquals(
          0, method.getExceptionTypes().length, "getValueType should not declare exceptions");
    }

    @Test
    @DisplayName("isSubtypeOf should not declare any exceptions")
    void isSubtypeOfShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = NoExtern.class.getMethod("isSubtypeOf", HeapType.class);
      assertEquals(
          0, method.getExceptionTypes().length, "isSubtypeOf should not declare exceptions");
    }
  }
}
