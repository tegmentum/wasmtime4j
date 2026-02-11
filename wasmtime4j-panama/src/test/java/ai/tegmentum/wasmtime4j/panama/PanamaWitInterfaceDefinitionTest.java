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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaWitInterfaceDefinition} class.
 *
 * <p>This test class verifies the Panama implementation of WitInterfaceDefinition interface using
 * reflection-based testing to avoid triggering native library loading.
 */
@DisplayName("PanamaWitInterfaceDefinition Tests")
class PanamaWitInterfaceDefinitionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaWitInterfaceDefinition should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWitInterfaceDefinition.class.getModifiers()),
          "PanamaWitInterfaceDefinition should be final");
    }

    @Test
    @DisplayName("PanamaWitInterfaceDefinition should implement WitInterfaceDefinition interface")
    void shouldImplementWitInterfaceDefinitionInterface() {
      assertTrue(
          WitInterfaceDefinition.class.isAssignableFrom(PanamaWitInterfaceDefinition.class),
          "PanamaWitInterfaceDefinition should implement WitInterfaceDefinition");
    }

    @Test
    @DisplayName("PanamaWitInterfaceDefinition should be package-private class")
    void shouldBePackagePrivateClass() {
      assertFalse(
          Modifier.isPublic(PanamaWitInterfaceDefinition.class.getModifiers()),
          "PanamaWitInterfaceDefinition should be package-private");
    }
  }

  @Nested
  @DisplayName("Interface Method Implementation Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("Should implement getName method")
    void shouldImplementGetNameMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("Should implement getVersion method")
    void shouldImplementGetVersionMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "getVersion should return String");
    }

    @Test
    @DisplayName("Should implement getPackageName method")
    void shouldImplementGetPackageNameMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getPackageName");
      assertNotNull(method, "getPackageName method should exist");
      assertEquals(String.class, method.getReturnType(), "getPackageName should return String");
    }

    @Test
    @DisplayName("Should implement getFunctionNames method")
    void shouldImplementGetFunctionNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getFunctionNames");
      assertNotNull(method, "getFunctionNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getFunctionNames should return List");
    }

    @Test
    @DisplayName("Should implement getTypeNames method")
    void shouldImplementGetTypeNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getTypeNames");
      assertNotNull(method, "getTypeNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getTypeNames should return List");
    }

    @Test
    @DisplayName("Should implement getDependencies method")
    void shouldImplementGetDependenciesMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getDependencies");
      assertNotNull(method, "getDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "getDependencies should return Set");
    }

    @Test
    @DisplayName("Should implement isCompatibleWith method")
    void shouldImplementIsCompatibleWithMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitInterfaceDefinition.class.getMethod(
              "isCompatibleWith", WitInterfaceDefinition.class);
      assertNotNull(method, "isCompatibleWith method should exist");
      assertEquals(
          WitCompatibilityResult.class,
          method.getReturnType(),
          "isCompatibleWith should return WitCompatibilityResult");
    }

    @Test
    @DisplayName("Should implement getWitText method")
    void shouldImplementGetWitTextMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getWitText");
      assertNotNull(method, "getWitText method should exist");
      assertEquals(String.class, method.getReturnType(), "getWitText should return String");
    }

    @Test
    @DisplayName("Should implement getImportNames method")
    void shouldImplementGetImportNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getImportNames");
      assertNotNull(method, "getImportNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getImportNames should return List");
    }

    @Test
    @DisplayName("Should implement getExportNames method")
    void shouldImplementGetExportNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaWitInterfaceDefinition.class.getMethod("getExportNames");
      assertNotNull(method, "getExportNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getExportNames should return List");
    }
  }

  @Nested
  @DisplayName("Package-Private Method Tests")
  class PackagePrivateMethodTests {

    @Test
    @DisplayName("Should have addFunction method")
    void shouldHaveAddFunctionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitInterfaceDefinition.class.getDeclaredMethod("addFunction", String.class);
      assertNotNull(method, "addFunction method should exist");
      assertEquals(void.class, method.getReturnType(), "addFunction should return void");
      assertFalse(Modifier.isPublic(method.getModifiers()), "addFunction should not be public");
      assertFalse(Modifier.isPrivate(method.getModifiers()), "addFunction should not be private");
    }

    @Test
    @DisplayName("Should have addType method")
    void shouldHaveAddTypeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitInterfaceDefinition.class.getDeclaredMethod("addType", String.class);
      assertNotNull(method, "addType method should exist");
      assertEquals(void.class, method.getReturnType(), "addType should return void");
      assertFalse(Modifier.isPublic(method.getModifiers()), "addType should not be public");
      assertFalse(Modifier.isPrivate(method.getModifiers()), "addType should not be private");
    }

    @Test
    @DisplayName("Should have addDependency method")
    void shouldHaveAddDependencyMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitInterfaceDefinition.class.getDeclaredMethod("addDependency", String.class);
      assertNotNull(method, "addDependency method should exist");
      assertEquals(void.class, method.getReturnType(), "addDependency should return void");
      assertFalse(Modifier.isPublic(method.getModifiers()), "addDependency should not be public");
      assertFalse(Modifier.isPrivate(method.getModifiers()), "addDependency should not be private");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have constructor with four String parameters")
    void shouldHaveConstructorWithFourParams() {
      boolean foundConstructor = false;
      for (final java.lang.reflect.Constructor<?> constructor :
          PanamaWitInterfaceDefinition.class.getDeclaredConstructors()) {
        final Class<?>[] paramTypes = constructor.getParameterTypes();
        if (paramTypes.length == 4
            && String.class.equals(paramTypes[0])
            && String.class.equals(paramTypes[1])
            && String.class.equals(paramTypes[2])
            && String.class.equals(paramTypes[3])) {
          foundConstructor = true;
          break;
        }
      }
      assertTrue(foundConstructor, "Should have constructor(String, String, String, String)");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have name field")
    void shouldHaveNameField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("name");
      assertNotNull(field, "name field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "name should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "name should be private");
      assertEquals(String.class, field.getType(), "name should be String type");
    }

    @Test
    @DisplayName("Should have version field")
    void shouldHaveVersionField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("version");
      assertNotNull(field, "version field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "version should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "version should be private");
      assertEquals(String.class, field.getType(), "version should be String type");
    }

    @Test
    @DisplayName("Should have packageName field")
    void shouldHavePackageNameField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("packageName");
      assertNotNull(field, "packageName field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "packageName should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "packageName should be private");
      assertEquals(String.class, field.getType(), "packageName should be String type");
    }

    @Test
    @DisplayName("Should have functionNames field")
    void shouldHaveFunctionNamesField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("functionNames");
      assertNotNull(field, "functionNames field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "functionNames should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "functionNames should be private");
      assertEquals(List.class, field.getType(), "functionNames should be List type");
    }

    @Test
    @DisplayName("Should have typeNames field")
    void shouldHaveTypeNamesField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("typeNames");
      assertNotNull(field, "typeNames field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "typeNames should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "typeNames should be private");
      assertEquals(List.class, field.getType(), "typeNames should be List type");
    }

    @Test
    @DisplayName("Should have dependencies field")
    void shouldHaveDependenciesField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("dependencies");
      assertNotNull(field, "dependencies field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "dependencies should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "dependencies should be private");
      assertEquals(Set.class, field.getType(), "dependencies should be Set type");
    }

    @Test
    @DisplayName("Should have witText field")
    void shouldHaveWitTextField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("witText");
      assertNotNull(field, "witText field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "witText should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "witText should be private");
      assertEquals(String.class, field.getType(), "witText should be String type");
    }

    @Test
    @DisplayName("Should have importNames field")
    void shouldHaveImportNamesField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("importNames");
      assertNotNull(field, "importNames field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "importNames should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "importNames should be private");
      assertEquals(List.class, field.getType(), "importNames should be List type");
    }

    @Test
    @DisplayName("Should have exportNames field")
    void shouldHaveExportNamesField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaWitInterfaceDefinition.class.getDeclaredField("exportNames");
      assertNotNull(field, "exportNames field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "exportNames should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "exportNames should be private");
      assertEquals(List.class, field.getType(), "exportNames should be List type");
    }
  }

  @Nested
  @DisplayName("Interface Implementation Completeness Tests")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("Should implement all WitInterfaceDefinition interface methods")
    void shouldImplementAllWitInterfaceDefinitionMethods() {
      final Set<String> interfaceMethods =
          Arrays.stream(WitInterfaceDefinition.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      final Set<String> classMethods =
          Arrays.stream(PanamaWitInterfaceDefinition.class.getMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String methodName : interfaceMethods) {
        assertTrue(
            classMethods.contains(methodName),
            "PanamaWitInterfaceDefinition should implement " + methodName);
      }
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have parseWitText private method")
    void shouldHaveParseWitTextMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitInterfaceDefinition.class.getDeclaredMethod("parseWitText", String.class);
      assertNotNull(method, "parseWitText method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "parseWitText should be private");
      assertEquals(void.class, method.getReturnType(), "parseWitText should return void");
    }
  }

  @Nested
  @DisplayName("Functional Tests")
  class FunctionalTests {

    @Test
    @DisplayName("Should create instance with basic parameters")
    void shouldCreateInstanceWithBasicParameters() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final Object instance =
          constructor.newInstance("test-interface", "1.0.0", "test:package", "");

      assertNotNull(instance, "Instance should be created");
      assertTrue(
          instance instanceof WitInterfaceDefinition, "Instance should be WitInterfaceDefinition");
    }

    @Test
    @DisplayName("Should handle null name parameter")
    void shouldHandleNullNameParameter() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance(null, "1.0.0", "test:package", "");

      assertEquals("", instance.getName(), "Null name should be converted to empty string");
    }

    @Test
    @DisplayName("Should handle null version parameter")
    void shouldHandleNullVersionParameter() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", null, "test:package", "");

      assertEquals("0.0.0", instance.getVersion(), "Null version should be converted to 0.0.0");
    }

    @Test
    @DisplayName("Should handle null packageName parameter")
    void shouldHandleNullPackageNameParameter() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", null, "");

      assertEquals("", instance.getPackageName(), "Null packageName should be converted to empty");
    }

    @Test
    @DisplayName("Should return empty lists initially")
    void shouldReturnEmptyListsInitially() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", "");

      assertNotNull(instance.getFunctionNames(), "getFunctionNames should not return null");
      assertNotNull(instance.getTypeNames(), "getTypeNames should not return null");
      assertNotNull(instance.getImportNames(), "getImportNames should not return null");
      assertNotNull(instance.getExportNames(), "getExportNames should not return null");
      assertNotNull(instance.getDependencies(), "getDependencies should not return null");
      assertTrue(instance.getFunctionNames().isEmpty(), "functionNames should be empty initially");
      assertTrue(instance.getTypeNames().isEmpty(), "typeNames should be empty initially");
    }

    @Test
    @DisplayName("Should return unmodifiable lists")
    void shouldReturnUnmodifiableLists() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", "");

      final List<String> functions = instance.getFunctionNames();
      try {
        functions.add("test");
        // If we get here, the list is modifiable which is wrong
        assertTrue(false, "getFunctionNames should return unmodifiable list");
      } catch (UnsupportedOperationException e) {
        // Expected behavior
        assertTrue(true);
      }
    }

    @Test
    @DisplayName("Should return unmodifiable set for dependencies")
    void shouldReturnUnmodifiableSet() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", "");

      final Set<String> deps = instance.getDependencies();
      try {
        deps.add("test");
        // If we get here, the set is modifiable which is wrong
        assertTrue(false, "getDependencies should return unmodifiable set");
      } catch (UnsupportedOperationException e) {
        // Expected behavior
        assertTrue(true);
      }
    }
  }

  @Nested
  @DisplayName("WIT Text Parsing Tests")
  class WitParsingTests {

    @Test
    @DisplayName("Should parse function declarations from WIT text")
    void shouldParseFunctionDeclarations() throws Exception {
      final String witText = "add: func(a: s32, b: s32) -> s32\nmultiply: func(x: s32) -> s32";

      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", witText);

      assertFalse(instance.getFunctionNames().isEmpty(), "Should parse functions from WIT text");
      assertTrue(instance.getFunctionNames().contains("add"), "Should parse 'add' function name");
      assertTrue(
          instance.getFunctionNames().contains("multiply"),
          "Should parse 'multiply' function name");
    }

    @Test
    @DisplayName("Should parse use dependencies from WIT text")
    void shouldParseUseDependencies() throws Exception {
      final String witText = "use wasi:io/streams\nuse wasi:clocks/monotonic-clock";

      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", witText);

      assertFalse(instance.getDependencies().isEmpty(), "Should parse dependencies from WIT text");
    }

    @Test
    @DisplayName("Should parse type declarations from WIT text")
    void shouldParseTypeDeclarations() throws Exception {
      final String witText =
          "type my-type = s32\nrecord point {\n  x: s32,\n  y: s32\n}\nenum status { ok, error }";

      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", witText);

      assertFalse(instance.getTypeNames().isEmpty(), "Should parse types from WIT text");
    }

    @Test
    @DisplayName("Should parse import declarations from WIT text")
    void shouldParseImportDeclarations() throws Exception {
      final String witText = "import wasi:io/streams\nimport wasi:clocks/wall-clock";

      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", witText);

      assertFalse(instance.getImportNames().isEmpty(), "Should parse imports from WIT text");
    }

    @Test
    @DisplayName("Should parse export declarations from WIT text")
    void shouldParseExportDeclarations() throws Exception {
      final String witText = "export wasi:io/streams\nexport my-interface";

      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", witText);

      assertFalse(instance.getExportNames().isEmpty(), "Should parse exports from WIT text");
    }
  }

  @Nested
  @DisplayName("Compatibility Tests")
  class CompatibilityTests {

    @Test
    @DisplayName("Should return incompatible for null parameter")
    void shouldReturnIncompatibleForNull() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", "");

      final WitCompatibilityResult result = instance.isCompatibleWith(null);
      assertNotNull(result, "Compatibility result should not be null");
      assertFalse(result.isCompatible(), "Should be incompatible with null");
    }

    @Test
    @DisplayName("Should be compatible with self")
    void shouldBeCompatibleWithSelf() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", "");

      final WitCompatibilityResult result = instance.isCompatibleWith(instance);
      assertNotNull(result, "Compatibility result should not be null");
      assertTrue(result.isCompatible(), "Should be compatible with self");
    }

    @Test
    @DisplayName("Should be incompatible with different interface name")
    void shouldBeIncompatibleWithDifferentName() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance1 =
          (WitInterfaceDefinition) constructor.newInstance("test1", "1.0.0", "test:pkg", "");
      final WitInterfaceDefinition instance2 =
          (WitInterfaceDefinition) constructor.newInstance("test2", "1.0.0", "test:pkg", "");

      final WitCompatibilityResult result = instance1.isCompatibleWith(instance2);
      assertNotNull(result, "Compatibility result should not be null");
      assertFalse(result.isCompatible(), "Should be incompatible with different name");
    }

    @Test
    @DisplayName("Should be incompatible with different major version")
    void shouldBeIncompatibleWithDifferentMajorVersion() throws Exception {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaWitInterfaceDefinition.class.getDeclaredConstructor(
              String.class, String.class, String.class, String.class);
      constructor.setAccessible(true);

      final WitInterfaceDefinition instance1 =
          (WitInterfaceDefinition) constructor.newInstance("test", "1.0.0", "test:pkg", "");
      final WitInterfaceDefinition instance2 =
          (WitInterfaceDefinition) constructor.newInstance("test", "2.0.0", "test:pkg", "");

      final WitCompatibilityResult result = instance1.isCompatibleWith(instance2);
      assertNotNull(result, "Compatibility result should not be null");
      assertFalse(result.isCompatible(), "Should be incompatible with different major version");
    }
  }
}
