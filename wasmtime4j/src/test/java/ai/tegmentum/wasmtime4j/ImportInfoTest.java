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
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ImportInfo class.
 *
 * <p>ImportInfo provides information about an import definition in a linker including module name,
 * import name, type, signature, and source details. This test verifies the class structure and
 * method signatures.
 */
@DisplayName("ImportInfo Class Tests")
class ImportInfoTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!ImportInfo.class.isInterface(), "ImportInfo should be a class");
      assertTrue(!ImportInfo.class.isEnum(), "ImportInfo should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(ImportInfo.class.getModifiers()), "ImportInfo should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(ImportInfo.class.getModifiers()), "ImportInfo should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 7 parameters")
    void shouldHavePublicConstructorWith7Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          ImportInfo.class.getConstructor(
              String.class,
              String.class,
              ImportInfo.ImportType.class,
              Optional.class,
              Instant.class,
              boolean.class,
              Optional.class);
      assertNotNull(constructor, "Constructor with 7 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          ImportInfo.class.getConstructor(
              String.class,
              String.class,
              ImportInfo.ImportType.class,
              Optional.class,
              Instant.class,
              boolean.class,
              Optional.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(7, paramTypes.length, "Constructor should have 7 parameters");
      assertEquals(String.class, paramTypes[0], "First parameter should be String (moduleName)");
      assertEquals(String.class, paramTypes[1], "Second parameter should be String (importName)");
      assertEquals(
          ImportInfo.ImportType.class,
          paramTypes[2],
          "Third parameter should be ImportType (importType)");
      assertEquals(
          Optional.class, paramTypes[3], "Fourth parameter should be Optional (typeSignature)");
      assertEquals(Instant.class, paramTypes[4], "Fifth parameter should be Instant (definedAt)");
      assertEquals(
          boolean.class, paramTypes[5], "Sixth parameter should be boolean (isHostFunction)");
      assertEquals(
          Optional.class,
          paramTypes[6],
          "Seventh parameter should be Optional (sourceDescription)");
    }

    @Test
    @DisplayName("should have only one public constructor")
    void shouldHaveOnlyOnePublicConstructor() {
      long publicConstructors =
          Arrays.stream(ImportInfo.class.getConstructors())
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .count();
      assertEquals(1, publicConstructors, "ImportInfo should have exactly 1 public constructor");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertEquals(String.class, method.getReturnType(), "getModuleName should return String");
    }

    @Test
    @DisplayName("should have getImportName method")
    void shouldHaveGetImportNameMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("getImportName");
      assertNotNull(method, "getImportName method should exist");
      assertEquals(String.class, method.getReturnType(), "getImportName should return String");
    }

    @Test
    @DisplayName("should have getImportType method")
    void shouldHaveGetImportTypeMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("getImportType");
      assertNotNull(method, "getImportType method should exist");
      assertEquals(
          ImportInfo.ImportType.class,
          method.getReturnType(),
          "getImportType should return ImportType");
    }

    @Test
    @DisplayName("should have getTypeSignature method")
    void shouldHaveGetTypeSignatureMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("getTypeSignature");
      assertNotNull(method, "getTypeSignature method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getTypeSignature should return Optional");
    }

    @Test
    @DisplayName("should have getDefinedAt method")
    void shouldHaveGetDefinedAtMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("getDefinedAt");
      assertNotNull(method, "getDefinedAt method should exist");
      assertEquals(Instant.class, method.getReturnType(), "getDefinedAt should return Instant");
    }

    @Test
    @DisplayName("should have isHostFunction method")
    void shouldHaveIsHostFunctionMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("isHostFunction");
      assertNotNull(method, "isHostFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHostFunction should return boolean");
    }

    @Test
    @DisplayName("should have getSourceDescription method")
    void shouldHaveGetSourceDescriptionMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("getSourceDescription");
      assertNotNull(method, "getSourceDescription method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getSourceDescription should return Optional");
    }

    @Test
    @DisplayName("should have getImportIdentifier method")
    void shouldHaveGetImportIdentifierMethod() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("getImportIdentifier");
      assertNotNull(method, "getImportIdentifier method should exist");
      assertEquals(
          String.class, method.getReturnType(), "getImportIdentifier should return String");
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
      final Method method = ImportInfo.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          ImportInfo.class,
          method.getDeclaringClass(),
          "toString should be declared in ImportInfo");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          ImportInfo.class, method.getDeclaringClass(), "equals should be declared in ImportInfo");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          ImportInfo.class,
          method.getDeclaringClass(),
          "hashCode should be declared in ImportInfo");
    }

    @Test
    @DisplayName("toString should return String")
    void toStringShouldReturnString() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("toString");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }

    @Test
    @DisplayName("equals should return boolean")
    void equalsShouldReturnBoolean() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("equals", Object.class);
      assertEquals(boolean.class, method.getReturnType(), "equals should return boolean");
    }

    @Test
    @DisplayName("hashCode should return int")
    void hashCodeShouldReturnInt() throws NoSuchMethodException {
      final Method method = ImportInfo.class.getMethod("hashCode");
      assertEquals(int.class, method.getReturnType(), "hashCode should return int");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private final moduleName field")
    void shouldHavePrivateFinalModuleNameField() throws NoSuchFieldException {
      Field field = ImportInfo.class.getDeclaredField("moduleName");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "moduleName field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "moduleName field should be final");
      assertEquals(String.class, field.getType(), "moduleName field should be String");
    }

    @Test
    @DisplayName("should have private final importName field")
    void shouldHavePrivateFinalImportNameField() throws NoSuchFieldException {
      Field field = ImportInfo.class.getDeclaredField("importName");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "importName field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "importName field should be final");
      assertEquals(String.class, field.getType(), "importName field should be String");
    }

    @Test
    @DisplayName("should have private final importType field")
    void shouldHavePrivateFinalImportTypeField() throws NoSuchFieldException {
      Field field = ImportInfo.class.getDeclaredField("importType");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "importType field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "importType field should be final");
      assertEquals(
          ImportInfo.ImportType.class, field.getType(), "importType field should be ImportType");
    }

    @Test
    @DisplayName("should have private final typeSignature field")
    void shouldHavePrivateFinalTypeSignatureField() throws NoSuchFieldException {
      Field field = ImportInfo.class.getDeclaredField("typeSignature");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "typeSignature field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "typeSignature field should be final");
      assertEquals(Optional.class, field.getType(), "typeSignature field should be Optional");
    }

    @Test
    @DisplayName("should have private final definedAt field")
    void shouldHavePrivateFinalDefinedAtField() throws NoSuchFieldException {
      Field field = ImportInfo.class.getDeclaredField("definedAt");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "definedAt field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "definedAt field should be final");
      assertEquals(Instant.class, field.getType(), "definedAt field should be Instant");
    }

    @Test
    @DisplayName("should have private final isHostFunction field")
    void shouldHavePrivateFinalIsHostFunctionField() throws NoSuchFieldException {
      Field field = ImportInfo.class.getDeclaredField("isHostFunction");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "isHostFunction field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "isHostFunction field should be final");
      assertEquals(boolean.class, field.getType(), "isHostFunction field should be boolean");
    }

    @Test
    @DisplayName("should have private final sourceDescription field")
    void shouldHavePrivateFinalSourceDescriptionField() throws NoSuchFieldException {
      Field field = ImportInfo.class.getDeclaredField("sourceDescription");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "sourceDescription field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "sourceDescription field should be final");
      assertEquals(Optional.class, field.getType(), "sourceDescription field should be Optional");
    }

    @Test
    @DisplayName("should have exactly 7 fields")
    void shouldHaveExactly7Fields() {
      Field[] fields = ImportInfo.class.getDeclaredFields();
      assertEquals(7, fields.length, "ImportInfo should have exactly 7 fields");
    }
  }

  // ========================================================================
  // Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ImportType Enum Tests")
  class ImportTypeEnumTests {

    @Test
    @DisplayName("should have ImportType nested enum")
    void shouldHaveImportTypeNestedEnum() {
      Class<?>[] declaredClasses = ImportInfo.class.getDeclaredClasses();
      boolean hasImportType = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ImportType") && clazz.isEnum()) {
          hasImportType = true;
          break;
        }
      }
      assertTrue(hasImportType, "ImportInfo should have ImportType nested enum");
    }

    @Test
    @DisplayName("ImportType should be public")
    void importTypeShouldBePublic() {
      assertTrue(
          Modifier.isPublic(ImportInfo.ImportType.class.getModifiers()),
          "ImportType should be public");
    }

    @Test
    @DisplayName("ImportType should have FUNCTION value")
    void importTypeShouldHaveFunctionValue() {
      ImportInfo.ImportType type = ImportInfo.ImportType.FUNCTION;
      assertNotNull(type, "ImportType.FUNCTION should exist");
    }

    @Test
    @DisplayName("ImportType should have MEMORY value")
    void importTypeShouldHaveMemoryValue() {
      ImportInfo.ImportType type = ImportInfo.ImportType.MEMORY;
      assertNotNull(type, "ImportType.MEMORY should exist");
    }

    @Test
    @DisplayName("ImportType should have TABLE value")
    void importTypeShouldHaveTableValue() {
      ImportInfo.ImportType type = ImportInfo.ImportType.TABLE;
      assertNotNull(type, "ImportType.TABLE should exist");
    }

    @Test
    @DisplayName("ImportType should have GLOBAL value")
    void importTypeShouldHaveGlobalValue() {
      ImportInfo.ImportType type = ImportInfo.ImportType.GLOBAL;
      assertNotNull(type, "ImportType.GLOBAL should exist");
    }

    @Test
    @DisplayName("ImportType should have INSTANCE value")
    void importTypeShouldHaveInstanceValue() {
      ImportInfo.ImportType type = ImportInfo.ImportType.INSTANCE;
      assertNotNull(type, "ImportType.INSTANCE should exist");
    }

    @Test
    @DisplayName("ImportType should have exactly 5 values")
    void importTypeShouldHaveExactly5Values() {
      assertEquals(
          5, ImportInfo.ImportType.values().length, "ImportType should have exactly 5 values");
    }

    @Test
    @DisplayName("ImportType ordinals should be sequential starting from 0")
    void importTypeOrdinalsShouldBeSequential() {
      assertEquals(0, ImportInfo.ImportType.FUNCTION.ordinal(), "FUNCTION ordinal should be 0");
      assertEquals(1, ImportInfo.ImportType.MEMORY.ordinal(), "MEMORY ordinal should be 1");
      assertEquals(2, ImportInfo.ImportType.TABLE.ordinal(), "TABLE ordinal should be 2");
      assertEquals(3, ImportInfo.ImportType.GLOBAL.ordinal(), "GLOBAL ordinal should be 3");
      assertEquals(4, ImportInfo.ImportType.INSTANCE.ordinal(), "INSTANCE ordinal should be 4");
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
              "getModuleName",
              "getImportName",
              "getImportType",
              "getTypeSignature",
              "getDefinedAt",
              "isHostFunction",
              "getSourceDescription",
              "getImportIdentifier",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(ImportInfo.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "ImportInfo should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 11 public methods")
    void shouldHaveExactly11PublicMethods() {
      long publicMethods =
          Arrays.stream(ImportInfo.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertEquals(11, publicMethods, "ImportInfo should have exactly 11 public methods");
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
          Object.class, ImportInfo.class.getSuperclass(), "ImportInfo should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          ImportInfo.class.getInterfaces().length,
          "ImportInfo should not implement any interfaces");
    }
  }

  // ========================================================================
  // Method Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Semantic Tests")
  class MethodSemanticTests {

    @Test
    @DisplayName("isHostFunction should return primitive boolean")
    void isHostFunctionShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = ImportInfo.class.getMethod("isHostFunction");
      assertEquals(
          boolean.class, method.getReturnType(), "isHostFunction should return primitive boolean");
      assertFalse(
          method.getReturnType().equals(Boolean.class),
          "isHostFunction should not return Boolean wrapper");
    }

    @Test
    @DisplayName("all getter methods should have no parameters")
    void allGetterMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      String[] methodNames = {
        "getModuleName",
        "getImportName",
        "getImportType",
        "getTypeSignature",
        "getDefinedAt",
        "isHostFunction",
        "getSourceDescription",
        "getImportIdentifier"
      };
      for (String name : methodNames) {
        Method method = ImportInfo.class.getMethod(name);
        assertEquals(0, method.getParameterCount(), name + " should have no parameters");
      }
    }
  }

  // ========================================================================
  // Static Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Methods Tests")
  class StaticMethodsTests {

    @Test
    @DisplayName("should have no public static methods")
    void shouldHaveNoPublicStaticMethods() {
      long staticMethods =
          Arrays.stream(ImportInfo.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "ImportInfo should have no public static methods");
    }
  }

  // ========================================================================
  // Nested Classes Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Count Tests")
  class NestedClassesCountTests {

    @Test
    @DisplayName("should have exactly 1 nested class (ImportType)")
    void shouldHaveExactly1NestedClass() {
      assertEquals(
          1,
          ImportInfo.class.getDeclaredClasses().length,
          "ImportInfo should have exactly 1 nested class (ImportType)");
    }
  }
}
