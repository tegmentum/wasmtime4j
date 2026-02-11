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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitInterfaceIntrospection;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WitInterfaceIntrospection} interface.
 *
 * <p>This test class verifies the interface structure, method signatures, nested interfaces, and
 * enums for the WIT interface introspection API.
 */
@DisplayName("WitInterfaceIntrospection Tests")
class WitInterfaceIntrospectionTest {

  // ========================================================================
  // Main Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WitInterfaceIntrospection should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WitInterfaceIntrospection.class.isInterface(),
          "WitInterfaceIntrospection should be an interface");
    }

    @Test
    @DisplayName("WitInterfaceIntrospection should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WitInterfaceIntrospection.class.getModifiers()),
          "WitInterfaceIntrospection should be public");
    }

    @Test
    @DisplayName("WitInterfaceIntrospection should not extend any interface")
    void shouldNotExtendAnyInterface() {
      Class<?>[] interfaces = WitInterfaceIntrospection.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "WitInterfaceIntrospection should not extend any interface");
    }
  }

  @Nested
  @DisplayName("Main Interface Method Tests")
  class MainInterfaceMethodTests {

    @Test
    @DisplayName("should have getInterfaceName method")
    void shouldHaveGetInterfaceNameMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getInterfaceName");
      assertNotNull(method, "getInterfaceName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getInterfaceName should have no parameters");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getVersion should have no parameters");
    }

    @Test
    @DisplayName("should have getFunctions method")
    void shouldHaveGetFunctionsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getFunctions");
      assertNotNull(method, "getFunctions method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(0, method.getParameterCount(), "getFunctions should have no parameters");
    }

    @Test
    @DisplayName("should have getTypes method")
    void shouldHaveGetTypesMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getTypes");
      assertNotNull(method, "getTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(0, method.getParameterCount(), "getTypes should have no parameters");
    }

    @Test
    @DisplayName("should have getResources method")
    void shouldHaveGetResourcesMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getResources");
      assertNotNull(method, "getResources method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(0, method.getParameterCount(), "getResources should have no parameters");
    }

    @Test
    @DisplayName("should have getDocumentation method")
    void shouldHaveGetDocumentationMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getDocumentation");
      assertNotNull(method, "getDocumentation method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getDocumentation should have no parameters");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
      assertEquals(0, method.getParameterCount(), "getMetadata should have no parameters");
    }

    @Test
    @DisplayName("should have isCompatibleWith method")
    void shouldHaveIsCompatibleWithMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceIntrospection.class.getMethod(
              "isCompatibleWith", WitInterfaceIntrospection.class);
      assertNotNull(method, "isCompatibleWith method should exist");
      assertEquals(
          WitInterfaceIntrospection.CompatibilityResult.class,
          method.getReturnType(),
          "Return type should be CompatibilityResult");
      assertEquals(1, method.getParameterCount(), "isCompatibleWith should have 1 parameter");
    }

    @Test
    @DisplayName("should have getDependencies method")
    void shouldHaveGetDependenciesMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getDependencies");
      assertNotNull(method, "getDependencies method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(0, method.getParameterCount(), "getDependencies should have no parameters");
    }

    @Test
    @DisplayName("should have getExports method")
    void shouldHaveGetExportsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getExports");
      assertNotNull(method, "getExports method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(0, method.getParameterCount(), "getExports should have no parameters");
    }

    @Test
    @DisplayName("should have getImports method")
    void shouldHaveGetImportsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.class.getMethod("getImports");
      assertNotNull(method, "getImports method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(0, method.getParameterCount(), "getImports should have no parameters");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionInfo Interface Tests")
  class FunctionInfoInterfaceTests {

    @Test
    @DisplayName("FunctionInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.FunctionInfo.class;
      assertTrue(nestedClass.isInterface(), "FunctionInfo should be an interface");
      assertTrue(nestedClass.isMemberClass(), "FunctionInfo should be a member class");
    }

    @Test
    @DisplayName("FunctionInfo should have all required methods")
    void shouldHaveAllRequiredMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "getName", "getParameters", "getReturnTypes", "getDocumentation", "isAsync"));

      Set<String> actualMethods = new HashSet<>();
      for (Method m : WitInterfaceIntrospection.FunctionInfo.class.getDeclaredMethods()) {
        actualMethods.add(m.getName());
      }

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "FunctionInfo should have method: " + expected);
      }
    }

    @Test
    @DisplayName("getName should return String")
    void getNameShouldReturnString() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.FunctionInfo.class.getMethod("getName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("getParameters should return List")
    void getParametersShouldReturnList() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.FunctionInfo.class.getMethod("getParameters");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("isAsync should return boolean")
    void isAsyncShouldReturnBoolean() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.FunctionInfo.class.getMethod("isAsync");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("TypeInfo Interface Tests")
  class TypeInfoInterfaceTests {

    @Test
    @DisplayName("TypeInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.TypeInfo.class;
      assertTrue(nestedClass.isInterface(), "TypeInfo should be an interface");
    }

    @Test
    @DisplayName("TypeInfo should have all required methods")
    void shouldHaveAllRequiredMethods() {
      Set<String> expectedMethods =
          new HashSet<>(Arrays.asList("getName", "getKind", "getDefinition", "getDocumentation"));

      Set<String> actualMethods = new HashSet<>();
      for (Method m : WitInterfaceIntrospection.TypeInfo.class.getDeclaredMethods()) {
        actualMethods.add(m.getName());
      }

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "TypeInfo should have method: " + expected);
      }
    }

    @Test
    @DisplayName("getKind should return TypeKind")
    void getKindShouldReturnTypeKind() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.TypeInfo.class.getMethod("getKind");
      assertEquals(
          WitInterfaceIntrospection.TypeKind.class,
          method.getReturnType(),
          "Should return TypeKind");
    }
  }

  @Nested
  @DisplayName("ParameterInfo Interface Tests")
  class ParameterInfoInterfaceTests {

    @Test
    @DisplayName("ParameterInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.ParameterInfo.class;
      assertTrue(nestedClass.isInterface(), "ParameterInfo should be an interface");
    }

    @Test
    @DisplayName("ParameterInfo should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.ParameterInfo.class.getMethod("getName"),
          "Should have getName");
      assertNotNull(
          WitInterfaceIntrospection.ParameterInfo.class.getMethod("getType"),
          "Should have getType");
      assertNotNull(
          WitInterfaceIntrospection.ParameterInfo.class.getMethod("isOptional"),
          "Should have isOptional");
      assertNotNull(
          WitInterfaceIntrospection.ParameterInfo.class.getMethod("getDocumentation"),
          "Should have getDocumentation");
    }

    @Test
    @DisplayName("getType should return TypeInfo")
    void getTypeShouldReturnTypeInfo() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.ParameterInfo.class.getMethod("getType");
      assertEquals(
          WitInterfaceIntrospection.TypeInfo.class,
          method.getReturnType(),
          "Should return TypeInfo");
    }

    @Test
    @DisplayName("isOptional should return boolean")
    void isOptionalShouldReturnBoolean() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.ParameterInfo.class.getMethod("isOptional");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("ResourceInfo Interface Tests")
  class ResourceInfoInterfaceTests {

    @Test
    @DisplayName("ResourceInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.ResourceInfo.class;
      assertTrue(nestedClass.isInterface(), "ResourceInfo should be an interface");
    }

    @Test
    @DisplayName("ResourceInfo should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.ResourceInfo.class.getMethod("getName"), "Should have getName");
      assertNotNull(
          WitInterfaceIntrospection.ResourceInfo.class.getMethod("getMethods"),
          "Should have getMethods");
      assertNotNull(
          WitInterfaceIntrospection.ResourceInfo.class.getMethod("getConstructor"),
          "Should have getConstructor");
      assertNotNull(
          WitInterfaceIntrospection.ResourceInfo.class.getMethod("getDocumentation"),
          "Should have getDocumentation");
    }

    @Test
    @DisplayName("getMethods should return List")
    void getMethodsShouldReturnList() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.ResourceInfo.class.getMethod("getMethods");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("getConstructor should return ConstructorInfo")
    void getConstructorShouldReturnConstructorInfo() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.ResourceInfo.class.getMethod("getConstructor");
      assertEquals(
          WitInterfaceIntrospection.ConstructorInfo.class,
          method.getReturnType(),
          "Should return ConstructorInfo");
    }
  }

  @Nested
  @DisplayName("MethodInfo Interface Tests")
  class MethodInfoInterfaceTests {

    @Test
    @DisplayName("MethodInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.MethodInfo.class;
      assertTrue(nestedClass.isInterface(), "MethodInfo should be an interface");
    }

    @Test
    @DisplayName("MethodInfo should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.MethodInfo.class.getMethod("getName"), "Should have getName");
      assertNotNull(
          WitInterfaceIntrospection.MethodInfo.class.getMethod("getParameters"),
          "Should have getParameters");
      assertNotNull(
          WitInterfaceIntrospection.MethodInfo.class.getMethod("getReturnTypes"),
          "Should have getReturnTypes");
      assertNotNull(
          WitInterfaceIntrospection.MethodInfo.class.getMethod("getKind"), "Should have getKind");
      assertNotNull(
          WitInterfaceIntrospection.MethodInfo.class.getMethod("getDocumentation"),
          "Should have getDocumentation");
    }

    @Test
    @DisplayName("getKind should return MethodKind")
    void getKindShouldReturnMethodKind() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.MethodInfo.class.getMethod("getKind");
      assertEquals(
          WitInterfaceIntrospection.MethodKind.class,
          method.getReturnType(),
          "Should return MethodKind");
    }
  }

  @Nested
  @DisplayName("ConstructorInfo Interface Tests")
  class ConstructorInfoInterfaceTests {

    @Test
    @DisplayName("ConstructorInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.ConstructorInfo.class;
      assertTrue(nestedClass.isInterface(), "ConstructorInfo should be an interface");
    }

    @Test
    @DisplayName("ConstructorInfo should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.ConstructorInfo.class.getMethod("getParameters"),
          "Should have getParameters");
      assertNotNull(
          WitInterfaceIntrospection.ConstructorInfo.class.getMethod("getDocumentation"),
          "Should have getDocumentation");
    }

    @Test
    @DisplayName("getParameters should return List")
    void getParametersShouldReturnList() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.ConstructorInfo.class.getMethod("getParameters");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("DependencyInfo Interface Tests")
  class DependencyInfoInterfaceTests {

    @Test
    @DisplayName("DependencyInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.DependencyInfo.class;
      assertTrue(nestedClass.isInterface(), "DependencyInfo should be an interface");
    }

    @Test
    @DisplayName("DependencyInfo should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.DependencyInfo.class.getMethod("getName"),
          "Should have getName");
      assertNotNull(
          WitInterfaceIntrospection.DependencyInfo.class.getMethod("getVersionConstraint"),
          "Should have getVersionConstraint");
      assertNotNull(
          WitInterfaceIntrospection.DependencyInfo.class.getMethod("isOptional"),
          "Should have isOptional");
    }

    @Test
    @DisplayName("isOptional should return boolean")
    void isOptionalShouldReturnBoolean() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.DependencyInfo.class.getMethod("isOptional");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("ExportInfo Interface Tests")
  class ExportInfoInterfaceTests {

    @Test
    @DisplayName("ExportInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.ExportInfo.class;
      assertTrue(nestedClass.isInterface(), "ExportInfo should be an interface");
    }

    @Test
    @DisplayName("ExportInfo should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.ExportInfo.class.getMethod("getName"), "Should have getName");
      assertNotNull(
          WitInterfaceIntrospection.ExportInfo.class.getMethod("getType"), "Should have getType");
      assertNotNull(
          WitInterfaceIntrospection.ExportInfo.class.getMethod("getTarget"),
          "Should have getTarget");
    }

    @Test
    @DisplayName("getType should return ExportType")
    void getTypeShouldReturnExportType() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.ExportInfo.class.getMethod("getType");
      assertEquals(
          WitInterfaceIntrospection.ExportType.class,
          method.getReturnType(),
          "Should return ExportType");
    }
  }

  @Nested
  @DisplayName("ImportInfo Interface Tests")
  class ImportInfoInterfaceTests {

    @Test
    @DisplayName("ImportInfo should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.ImportInfo.class;
      assertTrue(nestedClass.isInterface(), "ImportInfo should be an interface");
    }

    @Test
    @DisplayName("ImportInfo should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.ImportInfo.class.getMethod("getName"), "Should have getName");
      assertNotNull(
          WitInterfaceIntrospection.ImportInfo.class.getMethod("getType"), "Should have getType");
      assertNotNull(
          WitInterfaceIntrospection.ImportInfo.class.getMethod("getSource"),
          "Should have getSource");
    }

    @Test
    @DisplayName("getType should return ImportType")
    void getTypeShouldReturnImportType() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.ImportInfo.class.getMethod("getType");
      assertEquals(
          WitInterfaceIntrospection.ImportType.class,
          method.getReturnType(),
          "Should return ImportType");
    }
  }

  @Nested
  @DisplayName("CompatibilityResult Interface Tests")
  class CompatibilityResultInterfaceTests {

    @Test
    @DisplayName("CompatibilityResult should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.CompatibilityResult.class;
      assertTrue(nestedClass.isInterface(), "CompatibilityResult should be an interface");
    }

    @Test
    @DisplayName("CompatibilityResult should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.CompatibilityResult.class.getMethod("isCompatible"),
          "Should have isCompatible");
      assertNotNull(
          WitInterfaceIntrospection.CompatibilityResult.class.getMethod("getIssues"),
          "Should have getIssues");
      assertNotNull(
          WitInterfaceIntrospection.CompatibilityResult.class.getMethod("getScore"),
          "Should have getScore");
    }

    @Test
    @DisplayName("isCompatible should return boolean")
    void isCompatibleShouldReturnBoolean() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.CompatibilityResult.class.getMethod("isCompatible");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("getScore should return double")
    void getScoreShouldReturnDouble() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.CompatibilityResult.class.getMethod("getScore");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("CompatibilityIssue Interface Tests")
  class CompatibilityIssueInterfaceTests {

    @Test
    @DisplayName("CompatibilityIssue should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceIntrospection.CompatibilityIssue.class;
      assertTrue(nestedClass.isInterface(), "CompatibilityIssue should be an interface");
    }

    @Test
    @DisplayName("CompatibilityIssue should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceIntrospection.CompatibilityIssue.class.getMethod("getType"),
          "Should have getType");
      assertNotNull(
          WitInterfaceIntrospection.CompatibilityIssue.class.getMethod("getMessage"),
          "Should have getMessage");
      assertNotNull(
          WitInterfaceIntrospection.CompatibilityIssue.class.getMethod("getSeverity"),
          "Should have getSeverity");
      assertNotNull(
          WitInterfaceIntrospection.CompatibilityIssue.class.getMethod("getAffectedElement"),
          "Should have getAffectedElement");
    }

    @Test
    @DisplayName("getType should return IssueType")
    void getTypeShouldReturnIssueType() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.CompatibilityIssue.class.getMethod("getType");
      assertEquals(
          WitInterfaceIntrospection.IssueType.class,
          method.getReturnType(),
          "Should return IssueType");
    }

    @Test
    @DisplayName("getSeverity should return IssueSeverity")
    void getSeverityShouldReturnIssueSeverity() throws NoSuchMethodException {
      Method method = WitInterfaceIntrospection.CompatibilityIssue.class.getMethod("getSeverity");
      assertEquals(
          WitInterfaceIntrospection.IssueSeverity.class,
          method.getReturnType(),
          "Should return IssueSeverity");
    }
  }

  // ========================================================================
  // Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("TypeKind Enum Tests")
  class TypeKindEnumTests {

    @Test
    @DisplayName("TypeKind should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitInterfaceIntrospection.TypeKind.class.isEnum(), "TypeKind should be an enum");
    }

    @Test
    @DisplayName("TypeKind should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceIntrospection.TypeKind[] values = WitInterfaceIntrospection.TypeKind.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceIntrospection.TypeKind v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("PRIMITIVE"), "Should have PRIMITIVE");
      assertTrue(valueNames.contains("RECORD"), "Should have RECORD");
      assertTrue(valueNames.contains("VARIANT"), "Should have VARIANT");
      assertTrue(valueNames.contains("ENUM"), "Should have ENUM");
      assertTrue(valueNames.contains("LIST"), "Should have LIST");
      assertTrue(valueNames.contains("OPTION"), "Should have OPTION");
      assertTrue(valueNames.contains("RESULT"), "Should have RESULT");
      assertTrue(valueNames.contains("TUPLE"), "Should have TUPLE");
      assertTrue(valueNames.contains("FLAGS"), "Should have FLAGS");
      assertTrue(valueNames.contains("RESOURCE"), "Should have RESOURCE");
      assertEquals(10, values.length, "Should have exactly 10 values");
    }

    @Test
    @DisplayName("TypeKind.valueOf should work correctly")
    void valueOfShouldWork() {
      assertEquals(
          WitInterfaceIntrospection.TypeKind.PRIMITIVE,
          WitInterfaceIntrospection.TypeKind.valueOf("PRIMITIVE"));
      assertEquals(
          WitInterfaceIntrospection.TypeKind.RECORD,
          WitInterfaceIntrospection.TypeKind.valueOf("RECORD"));
    }
  }

  @Nested
  @DisplayName("MethodKind Enum Tests")
  class MethodKindEnumTests {

    @Test
    @DisplayName("MethodKind should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceIntrospection.MethodKind.class.isEnum(), "MethodKind should be an enum");
    }

    @Test
    @DisplayName("MethodKind should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceIntrospection.MethodKind[] values = WitInterfaceIntrospection.MethodKind.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceIntrospection.MethodKind v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("INSTANCE"), "Should have INSTANCE");
      assertTrue(valueNames.contains("STATIC"), "Should have STATIC");
      assertTrue(valueNames.contains("CONSTRUCTOR"), "Should have CONSTRUCTOR");
      assertTrue(valueNames.contains("DESTRUCTOR"), "Should have DESTRUCTOR");
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("ExportType Enum Tests")
  class ExportTypeEnumTests {

    @Test
    @DisplayName("ExportType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceIntrospection.ExportType.class.isEnum(), "ExportType should be an enum");
    }

    @Test
    @DisplayName("ExportType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceIntrospection.ExportType[] values = WitInterfaceIntrospection.ExportType.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceIntrospection.ExportType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("FUNCTION"), "Should have FUNCTION");
      assertTrue(valueNames.contains("TYPE"), "Should have TYPE");
      assertTrue(valueNames.contains("RESOURCE"), "Should have RESOURCE");
      assertTrue(valueNames.contains("INTERFACE"), "Should have INTERFACE");
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("ImportType Enum Tests")
  class ImportTypeEnumTests {

    @Test
    @DisplayName("ImportType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceIntrospection.ImportType.class.isEnum(), "ImportType should be an enum");
    }

    @Test
    @DisplayName("ImportType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceIntrospection.ImportType[] values = WitInterfaceIntrospection.ImportType.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceIntrospection.ImportType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("FUNCTION"), "Should have FUNCTION");
      assertTrue(valueNames.contains("TYPE"), "Should have TYPE");
      assertTrue(valueNames.contains("RESOURCE"), "Should have RESOURCE");
      assertTrue(valueNames.contains("INTERFACE"), "Should have INTERFACE");
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("IssueType Enum Tests")
  class IssueTypeEnumTests {

    @Test
    @DisplayName("IssueType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitInterfaceIntrospection.IssueType.class.isEnum(), "IssueType should be an enum");
    }

    @Test
    @DisplayName("IssueType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceIntrospection.IssueType[] values = WitInterfaceIntrospection.IssueType.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceIntrospection.IssueType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("MISSING_FUNCTION"), "Should have MISSING_FUNCTION");
      assertTrue(
          valueNames.contains("FUNCTION_SIGNATURE_MISMATCH"),
          "Should have FUNCTION_SIGNATURE_MISMATCH");
      assertTrue(valueNames.contains("MISSING_TYPE"), "Should have MISSING_TYPE");
      assertTrue(
          valueNames.contains("TYPE_DEFINITION_MISMATCH"), "Should have TYPE_DEFINITION_MISMATCH");
      assertTrue(valueNames.contains("VERSION_MISMATCH"), "Should have VERSION_MISMATCH");
      assertTrue(valueNames.contains("BREAKING_CHANGE"), "Should have BREAKING_CHANGE");
      assertEquals(6, values.length, "Should have exactly 6 values");
    }
  }

  @Nested
  @DisplayName("IssueSeverity Enum Tests")
  class IssueSeverityEnumTests {

    @Test
    @DisplayName("IssueSeverity should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceIntrospection.IssueSeverity.class.isEnum(),
          "IssueSeverity should be an enum");
    }

    @Test
    @DisplayName("IssueSeverity should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceIntrospection.IssueSeverity[] values =
          WitInterfaceIntrospection.IssueSeverity.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceIntrospection.IssueSeverity v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("INFO"), "Should have INFO");
      assertTrue(valueNames.contains("WARNING"), "Should have WARNING");
      assertTrue(valueNames.contains("ERROR"), "Should have ERROR");
      assertTrue(valueNames.contains("CRITICAL"), "Should have CRITICAL");
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("WitInterfaceIntrospection should have expected number of nested types")
    void shouldHaveExpectedNestedTypes() {
      Class<?>[] declaredClasses = WitInterfaceIntrospection.class.getDeclaredClasses();

      int interfaceCount = 0;
      int enumCount = 0;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.isInterface()) {
          interfaceCount++;
        } else if (clazz.isEnum()) {
          enumCount++;
        }
      }

      // Expected nested interfaces: FunctionInfo, TypeInfo, ParameterInfo, ResourceInfo,
      // MethodInfo, ConstructorInfo, DependencyInfo, ExportInfo, ImportInfo,
      // CompatibilityResult, CompatibilityIssue
      assertEquals(11, interfaceCount, "Should have 11 nested interfaces");

      // Expected enums: TypeKind, MethodKind, ExportType, ImportType, IssueType, IssueSeverity
      assertEquals(6, enumCount, "Should have 6 enums");
    }

    @Test
    @DisplayName("All nested interfaces should be public")
    void allNestedInterfacesShouldBePublic() {
      Class<?>[] declaredClasses = WitInterfaceIntrospection.class.getDeclaredClasses();

      for (Class<?> clazz : declaredClasses) {
        if (clazz.isInterface()) {
          assertTrue(
              Modifier.isPublic(clazz.getModifiers()),
              "Nested interface " + clazz.getSimpleName() + " should be public");
        }
      }
    }

    @Test
    @DisplayName("All nested enums should be public")
    void allNestedEnumsShouldBePublic() {
      Class<?>[] declaredClasses = WitInterfaceIntrospection.class.getDeclaredClasses();

      for (Class<?> clazz : declaredClasses) {
        if (clazz.isEnum()) {
          assertTrue(
              Modifier.isPublic(clazz.getModifiers()),
              "Nested enum " + clazz.getSimpleName() + " should be public");
        }
      }
    }
  }
}
