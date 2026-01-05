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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiImportResolver interface.
 *
 * <p>WasiImportResolver provides implementations for component interface imports, supplying host
 * functions, other components, or resource providers.
 */
@DisplayName("WasiImportResolver Interface Tests")
class WasiImportResolverTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiImportResolver.class.isInterface(), "WasiImportResolver should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiImportResolver.class.getModifiers()),
          "WasiImportResolver should be public");
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
          WasiImportResolver.class.getInterfaces().length,
          "WasiImportResolver should not extend any interfaces");
    }
  }

  // ========================================================================
  // Interface Metadata Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Metadata Method Tests")
  class InterfaceMetadataMethodTests {

    @Test
    @DisplayName("should have getInterfaceName method")
    void shouldHaveGetInterfaceNameMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("getInterfaceName");
      assertNotNull(method, "getInterfaceName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getInterfaceVersion method")
    void shouldHaveGetInterfaceVersionMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("getInterfaceVersion");
      assertNotNull(method, "getInterfaceVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getProvidedFunctions method")
    void shouldHaveGetProvidedFunctionsMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("getProvidedFunctions");
      assertNotNull(method, "getProvidedFunctions method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getProvidedResourceTypes method")
    void shouldHaveGetProvidedResourceTypesMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("getProvidedResourceTypes");
      assertNotNull(method, "getProvidedResourceTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  // ========================================================================
  // Function Resolution Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Function Resolution Method Tests")
  class FunctionResolutionMethodTests {

    @Test
    @DisplayName("should have resolveFunction method")
    void shouldHaveResolveFunctionMethod() throws NoSuchMethodException {
      Method method =
          WasiImportResolver.class.getMethod("resolveFunction", String.class, List.class);
      assertNotNull(method, "resolveFunction method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }

    @Test
    @DisplayName("should have canResolveFunction method")
    void shouldHaveCanResolveFunctionMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("canResolveFunction", String.class);
      assertNotNull(method, "canResolveFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getFunctionMetadata method")
    void shouldHaveGetFunctionMetadataMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("getFunctionMetadata", String.class);
      assertNotNull(method, "getFunctionMetadata method should exist");
      assertEquals(
          WasiFunctionMetadata.class, method.getReturnType(), "Should return WasiFunctionMetadata");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }
  }

  // ========================================================================
  // Resource Management Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Management Method Tests")
  class ResourceManagementMethodTests {

    @Test
    @DisplayName("should have createResource method")
    void shouldHaveCreateResourceMethod() throws NoSuchMethodException {
      Method method =
          WasiImportResolver.class.getMethod("createResource", String.class, Object[].class);
      assertNotNull(method, "createResource method should exist");
      assertEquals(WasiResource.class, method.getReturnType(), "Should return WasiResource");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }

    @Test
    @DisplayName("should have canCreateResourceType method")
    void shouldHaveCanCreateResourceTypeMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("canCreateResourceType", String.class);
      assertNotNull(method, "canCreateResourceType method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getResourceTypeMetadata method")
    void shouldHaveGetResourceTypeMetadataMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("getResourceTypeMetadata", String.class);
      assertNotNull(method, "getResourceTypeMetadata method should exist");
      assertEquals(
          WasiResourceTypeMetadata.class,
          method.getReturnType(),
          "Should return WasiResourceTypeMetadata");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }
  }

  // ========================================================================
  // Property Management Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Property Management Method Tests")
  class PropertyManagementMethodTests {

    @Test
    @DisplayName("should have getProperties method")
    void shouldHaveGetPropertiesMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("getProperties");
      assertNotNull(method, "getProperties method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have setProperty method")
    void shouldHaveSetPropertyMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("setProperty", String.class, Object.class);
      assertNotNull(method, "setProperty method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }

    @Test
    @DisplayName("should have initialize method")
    void shouldHaveInitializeMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("initialize", WasiComponent.class);
      assertNotNull(method, "initialize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }

    @Test
    @DisplayName("should have cleanup method")
    void shouldHaveCleanupMethod() throws NoSuchMethodException {
      Method method = WasiImportResolver.class.getMethod("cleanup");
      assertNotNull(method, "cleanup method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
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
          WasiImportResolver.class.getDeclaredClasses().length,
          "WasiImportResolver should have no nested classes");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 13 abstract methods")
    void shouldHaveExpectedMethodCount() {
      long abstractMethodCount =
          Arrays.stream(WasiImportResolver.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractMethodCount >= 13, "Should have at least 13 abstract methods");
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
          WasiImportResolver.class.getDeclaredFields().length,
          "WasiImportResolver should have no declared fields");
    }
  }
}
