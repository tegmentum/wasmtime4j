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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FunctionInfo class.
 *
 * <p>FunctionInfo provides metadata about functions in a WebAssembly module including their index,
 * name, type, and whether they are imported or defined locally. This test verifies the class
 * structure and method signatures.
 */
@DisplayName("FunctionInfo Class Tests")
class FunctionInfoTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!FunctionInfo.class.isInterface(), "FunctionInfo should be a class");
      assertTrue(!FunctionInfo.class.isEnum(), "FunctionInfo should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FunctionInfo.class.getModifiers()), "FunctionInfo should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(FunctionInfo.class.getModifiers()), "FunctionInfo should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 4 parameters")
    void shouldHavePublicConstructorWith4Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          FunctionInfo.class.getConstructor(int.class, String.class, FuncType.class, boolean.class);
      assertNotNull(constructor, "Constructor with 4 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          FunctionInfo.class.getConstructor(int.class, String.class, FuncType.class, boolean.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(4, paramTypes.length, "Constructor should have 4 parameters");
      assertEquals(int.class, paramTypes[0], "First parameter should be int (index)");
      assertEquals(String.class, paramTypes[1], "Second parameter should be String (name)");
      assertEquals(FuncType.class, paramTypes[2], "Third parameter should be FuncType (funcType)");
      assertEquals(boolean.class, paramTypes[3], "Fourth parameter should be boolean (isImport)");
    }

    @Test
    @DisplayName("should have only one public constructor")
    void shouldHaveOnlyOnePublicConstructor() {
      long publicConstructors =
          Arrays.stream(FunctionInfo.class.getConstructors())
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .count();
      assertEquals(1, publicConstructors, "FunctionInfo should have exactly 1 public constructor");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("getIndex");
      assertNotNull(method, "getIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "getIndex should return int");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getFuncType method")
    void shouldHaveGetFuncTypeMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("getFuncType");
      assertNotNull(method, "getFuncType method should exist");
      assertEquals(FuncType.class, method.getReturnType(), "getFuncType should return FuncType");
    }

    @Test
    @DisplayName("should have isImport method")
    void shouldHaveIsImportMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("isImport");
      assertNotNull(method, "isImport method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isImport should return boolean");
    }

    @Test
    @DisplayName("should have isLocal method")
    void shouldHaveIsLocalMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("isLocal");
      assertNotNull(method, "isLocal method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isLocal should return boolean");
    }
  }

  // ========================================================================
  // Object Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Methods Tests")
  class ObjectMethodsTests {

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          FunctionInfo.class,
          method.getDeclaringClass(),
          "toString should be declared in FunctionInfo");
    }

    @Test
    @DisplayName("toString should return String")
    void toStringShouldReturnString() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("toString");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private final index field")
    void shouldHavePrivateFinalIndexField() throws NoSuchFieldException {
      Field field = FunctionInfo.class.getDeclaredField("index");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "index field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "index field should be final");
      assertEquals(int.class, field.getType(), "index field should be int");
    }

    @Test
    @DisplayName("should have private final name field")
    void shouldHavePrivateFinalNameField() throws NoSuchFieldException {
      Field field = FunctionInfo.class.getDeclaredField("name");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "name field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "name field should be final");
      assertEquals(String.class, field.getType(), "name field should be String");
    }

    @Test
    @DisplayName("should have private final funcType field")
    void shouldHavePrivateFinalFuncTypeField() throws NoSuchFieldException {
      Field field = FunctionInfo.class.getDeclaredField("funcType");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "funcType field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "funcType field should be final");
      assertEquals(FuncType.class, field.getType(), "funcType field should be FuncType");
    }

    @Test
    @DisplayName("should have private final isImport field")
    void shouldHavePrivateFinalIsImportField() throws NoSuchFieldException {
      Field field = FunctionInfo.class.getDeclaredField("isImport");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "isImport field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "isImport field should be final");
      assertEquals(boolean.class, field.getType(), "isImport field should be boolean");
    }

    @Test
    @DisplayName("should have exactly 4 fields")
    void shouldHaveExactly4Fields() {
      Field[] fields = FunctionInfo.class.getDeclaredFields();
      assertEquals(4, fields.length, "FunctionInfo should have exactly 4 fields");
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
          Set.of("getIndex", "getName", "getFuncType", "isImport", "isLocal", "toString");

      Set<String> actualMethods =
          Arrays.stream(FunctionInfo.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "FunctionInfo should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 6 public methods")
    void shouldHaveExactly6PublicMethods() {
      long publicMethods =
          Arrays.stream(FunctionInfo.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertEquals(6, publicMethods, "FunctionInfo should have exactly 6 public methods");
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
          Object.class, FunctionInfo.class.getSuperclass(), "FunctionInfo should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          FunctionInfo.class.getInterfaces().length,
          "FunctionInfo should not implement any interfaces");
    }
  }

  // ========================================================================
  // Method Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Semantic Tests")
  class MethodSemanticTests {

    @Test
    @DisplayName("isImport and isLocal should have no parameters")
    void isImportAndIsLocalShouldHaveNoParameters() throws NoSuchMethodException {
      Method isImport = FunctionInfo.class.getMethod("isImport");
      Method isLocal = FunctionInfo.class.getMethod("isLocal");

      assertEquals(0, isImport.getParameterCount(), "isImport should have no parameters");
      assertEquals(0, isLocal.getParameterCount(), "isLocal should have no parameters");
    }

    @Test
    @DisplayName("getIndex should return primitive int")
    void getIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = FunctionInfo.class.getMethod("getIndex");
      assertEquals(int.class, method.getReturnType(), "getIndex should return primitive int");
      assertFalse(
          method.getReturnType().equals(Integer.class),
          "getIndex should not return Integer wrapper");
    }

    @Test
    @DisplayName("isImport should return primitive boolean")
    void isImportShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = FunctionInfo.class.getMethod("isImport");
      assertEquals(
          boolean.class, method.getReturnType(), "isImport should return primitive boolean");
      assertFalse(
          method.getReturnType().equals(Boolean.class),
          "isImport should not return Boolean wrapper");
    }

    @Test
    @DisplayName("isLocal should return primitive boolean")
    void isLocalShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = FunctionInfo.class.getMethod("isLocal");
      assertEquals(
          boolean.class, method.getReturnType(), "isLocal should return primitive boolean");
      assertFalse(
          method.getReturnType().equals(Boolean.class),
          "isLocal should not return Boolean wrapper");
    }
  }

  // ========================================================================
  // No Static Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Methods Tests")
  class StaticMethodsTests {

    @Test
    @DisplayName("should have no public static methods")
    void shouldHaveNoPublicStaticMethods() {
      long staticMethods =
          Arrays.stream(FunctionInfo.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "FunctionInfo should have no public static methods");
    }
  }

  // ========================================================================
  // No Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          FunctionInfo.class.getDeclaredClasses().length,
          "FunctionInfo should have no nested classes");
    }
  }
}
