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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ComponentLinker} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the ComponentLinker
 * API, which provides component linking and instantiation capabilities.
 */
@DisplayName("ComponentLinker Interface Tests")
class ComponentLinkerInterfaceTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentLinker Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ComponentLinker should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentLinker.class.isInterface(), "ComponentLinker should be an interface");
    }

    @Test
    @DisplayName("ComponentLinker should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentLinker.class.getModifiers()),
          "ComponentLinker should be public");
    }

    @Test
    @DisplayName("ComponentLinker should extend Closeable")
    void shouldExtendCloseable() {
      Class<?>[] interfaces = ComponentLinker.class.getInterfaces();
      assertEquals(1, interfaces.length, "ComponentLinker should extend exactly one interface");
      assertEquals(Closeable.class, interfaces[0], "ComponentLinker should extend Closeable");
    }

    @Test
    @DisplayName("ComponentLinker should be a generic interface with type parameter T")
    void shouldBeGenericWithTypeParameterT() {
      TypeVariable<?>[] typeParameters = ComponentLinker.class.getTypeParameters();
      assertEquals(1, typeParameters.length, "ComponentLinker should have 1 type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named T");
    }
  }

  // ========================================================================
  // Function Definition Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Function Definition Method Tests")
  class FunctionDefinitionTests {

    @Test
    @DisplayName(
        "should have defineFunction with namespace, interface, function, and implementation")
    void shouldHaveDefineFunctionWithFourParameters() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineFunction",
              String.class,
              String.class,
              String.class,
              ComponentHostFunction.class);
      assertNotNull(method, "defineFunction with 4 parameters should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(4, method.getParameterCount(), "defineFunction should have 4 parameters");
    }

    @Test
    @DisplayName("defineFunction with 4 parameters should throw WasmException")
    void defineFunctionFourParamsShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineFunction",
              String.class,
              String.class,
              String.class,
              ComponentHostFunction.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have defineFunction with WIT path and implementation")
    void shouldHaveDefineFunctionWithWitPath() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineFunction", String.class, ComponentHostFunction.class);
      assertNotNull(method, "defineFunction with WIT path should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "defineFunction should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          ComponentHostFunction.class,
          method.getParameterTypes()[1],
          "Second parameter should be ComponentHostFunction");
    }

    @Test
    @DisplayName("defineFunction with WIT path should throw WasmException")
    void defineFunctionWitPathShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineFunction", String.class, ComponentHostFunction.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Interface Definition Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Definition Method Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("should have defineInterface method")
    void shouldHaveDefineInterfaceMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("defineInterface", String.class, String.class, Map.class);
      assertNotNull(method, "defineInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(3, method.getParameterCount(), "defineInterface should have 3 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          String.class, method.getParameterTypes()[1], "Second parameter should be String");
      assertEquals(Map.class, method.getParameterTypes()[2], "Third parameter should be Map");
    }

    @Test
    @DisplayName("defineInterface should throw WasmException")
    void defineInterfaceShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("defineInterface", String.class, String.class, Map.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Resource Definition Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Definition Method Tests")
  class ResourceDefinitionTests {

    @Test
    @DisplayName("should have defineResource method")
    void shouldHaveDefineResourceMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineResource",
              String.class,
              String.class,
              String.class,
              ComponentResourceDefinition.class);
      assertNotNull(method, "defineResource method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(4, method.getParameterCount(), "defineResource should have 4 parameters");
    }

    @Test
    @DisplayName("defineResource should throw WasmException")
    void defineResourceShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineResource",
              String.class,
              String.class,
              String.class,
              ComponentResourceDefinition.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Linking Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Linking Method Tests")
  class LinkingMethodTests {

    @Test
    @DisplayName("should have linkInstance method")
    void shouldHaveLinkInstanceMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("linkInstance", ComponentInstance.class);
      assertNotNull(method, "linkInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "linkInstance should have 1 parameter");
      assertEquals(
          ComponentInstance.class,
          method.getParameterTypes()[0],
          "Parameter should be ComponentInstance");
    }

    @Test
    @DisplayName("linkInstance should throw WasmException")
    void linkInstanceShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("linkInstance", ComponentInstance.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have linkComponent method")
    void shouldHaveLinkComponentMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("linkComponent", Store.class, Component.class);
      assertNotNull(method, "linkComponent method should exist");
      assertEquals(
          ComponentInstance.class,
          method.getReturnType(),
          "Return type should be ComponentInstance");
      assertEquals(2, method.getParameterCount(), "linkComponent should have 2 parameters");
      assertEquals(Store.class, method.getParameterTypes()[0], "First parameter should be Store");
      assertEquals(
          Component.class,
          method.getParameterTypes()[1],
          "Second parameter should be Component");
    }

    @Test
    @DisplayName("linkComponent should throw WasmException")
    void linkComponentShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("linkComponent", Store.class, Component.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Instantiation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("instantiate", Store.class, Component.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          ComponentInstance.class,
          method.getReturnType(),
          "Return type should be ComponentInstance");
      assertEquals(2, method.getParameterCount(), "instantiate should have 2 parameters");
      assertEquals(Store.class, method.getParameterTypes()[0], "First parameter should be Store");
      assertEquals(
          Component.class,
          method.getParameterTypes()[1],
          "Second parameter should be Component");
    }

    @Test
    @DisplayName("instantiate should throw WasmException")
    void instantiateShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("instantiate", Store.class, Component.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // WASI Preview 2 Method Tests
  // ========================================================================

  @Nested
  @DisplayName("WASI Preview 2 Method Tests")
  class WasiPreview2Tests {

    @Test
    @DisplayName("should have enableWasiPreview2 method without parameters")
    void shouldHaveEnableWasiPreview2Method() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("enableWasiPreview2");
      assertNotNull(method, "enableWasiPreview2 method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "enableWasiPreview2 should have no parameters");
    }

    @Test
    @DisplayName("enableWasiPreview2 should throw WasmException")
    void enableWasiPreview2ShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("enableWasiPreview2");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have enableWasiPreview2 method with config parameter")
    void shouldHaveEnableWasiPreview2WithConfigMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("enableWasiPreview2", WasiPreview2Config.class);
      assertNotNull(method, "enableWasiPreview2 with config should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "enableWasiPreview2 should have 1 parameter");
      assertEquals(
          WasiPreview2Config.class,
          method.getParameterTypes()[0],
          "Parameter should be WasiPreview2Config");
    }

    @Test
    @DisplayName("enableWasiPreview2 with config should throw WasmException")
    void enableWasiPreview2WithConfigShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("enableWasiPreview2", WasiPreview2Config.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Engine and Validation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Engine and Validation Method Tests")
  class EngineValidationTests {

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Return type should be Engine");
      assertEquals(0, method.getParameterCount(), "getEngine should have no parameters");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }
  }

  // ========================================================================
  // Query Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("should have hasInterface method")
    void shouldHaveHasInterfaceMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("hasInterface", String.class, String.class);
      assertNotNull(method, "hasInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(2, method.getParameterCount(), "hasInterface should have 2 parameters");
    }

    @Test
    @DisplayName("should have hasFunction method")
    void shouldHaveHasFunctionMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("hasFunction", String.class, String.class, String.class);
      assertNotNull(method, "hasFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(3, method.getParameterCount(), "hasFunction should have 3 parameters");
    }

    @Test
    @DisplayName("should have getDefinedInterfaces method")
    void shouldHaveGetDefinedInterfacesMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("getDefinedInterfaces");
      assertNotNull(method, "getDefinedInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
      assertEquals(0, method.getParameterCount(), "getDefinedInterfaces should have no parameters");
    }

    @Test
    @DisplayName("getDefinedInterfaces should return Set<String>")
    void getDefinedInterfacesShouldReturnStringSet() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("getDefinedInterfaces");
      Type genericReturnType = method.getGenericReturnType();
      if (genericReturnType instanceof ParameterizedType) {
        ParameterizedType paramType = (ParameterizedType) genericReturnType;
        assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
        Type[] typeArgs = paramType.getActualTypeArguments();
        assertEquals(1, typeArgs.length, "Set should have one type argument");
        assertEquals(String.class, typeArgs[0], "Set type argument should be String");
      }
    }

    @Test
    @DisplayName("should have getDefinedFunctions method")
    void shouldHaveGetDefinedFunctionsMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("getDefinedFunctions", String.class, String.class);
      assertNotNull(method, "getDefinedFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
      assertEquals(2, method.getParameterCount(), "getDefinedFunctions should have 2 parameters");
    }
  }

  // ========================================================================
  // Validation and Alias Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Validation and Alias Method Tests")
  class ValidationAliasTests {

    @Test
    @DisplayName("should have validateImports method")
    void shouldHaveValidateImportsMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("validateImports", Component.class);
      assertNotNull(method, "validateImports method should exist");
      assertEquals(
          ComponentImportValidation.class,
          method.getReturnType(),
          "Return type should be ComponentImportValidation");
      assertEquals(1, method.getParameterCount(), "validateImports should have 1 parameter");
      assertEquals(
          Component.class,
          method.getParameterTypes()[0],
          "Parameter should be Component");
    }

    @Test
    @DisplayName("validateImports should not throw checked exceptions")
    void validateImportsShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("validateImports", Component.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(
          0, exceptionTypes.length, "validateImports should not declare checked exceptions");
    }

    @Test
    @DisplayName("should have aliasInterface method")
    void shouldHaveAliasInterfaceMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "aliasInterface", String.class, String.class, String.class, String.class);
      assertNotNull(method, "aliasInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(4, method.getParameterCount(), "aliasInterface should have 4 parameters");
    }

    @Test
    @DisplayName("aliasInterface should throw WasmException")
    void aliasInterfaceShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "aliasInterface", String.class, String.class, String.class, String.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryTests {

    @Test
    @DisplayName("should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("create", Engine.class);
      assertNotNull(method, "static create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create method should be static");
      assertEquals(
          ComponentLinker.class, method.getReturnType(), "Return type should be ComponentLinker");
      assertEquals(1, method.getParameterCount(), "create should have 1 parameter");
      assertEquals(Engine.class, method.getParameterTypes()[0], "Parameter should be Engine");
    }

    @Test
    @DisplayName("static create should throw WasmException")
    void staticCreateShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("create", Engine.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Method Count and Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count and Completeness Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ComponentLinker should have expected methods")
    void shouldHaveExpectedMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "defineFunction",
                  "defineInterface",
                  "defineResource",
                  "linkInstance",
                  "linkComponent",
                  "instantiate",
                  "enableWasiPreview2",
                  "getEngine",
                  "isValid",
                  "hasInterface",
                  "hasFunction",
                  "getDefinedInterfaces",
                  "getDefinedFunctions",
                  "validateImports",
                  "aliasInterface",
                  "close",
                  "create"));

      Method[] methods = ComponentLinker.class.getDeclaredMethods();
      Set<String> actualMethodNames = new HashSet<>();
      for (Method method : methods) {
        actualMethodNames.add(method.getName());
      }

      for (String expectedMethod : expectedMethods) {
        assertTrue(
            actualMethodNames.contains(expectedMethod),
            "ComponentLinker should declare " + expectedMethod + " method");
      }
    }
  }

  // ========================================================================
  // Interface Integration Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("ComponentLinker should be in ai.tegmentum.wasmtime4j package")
    void shouldBeInCorrectPackage() {
      String packageName = ComponentLinker.class.getPackage().getName();
      assertEquals(
          "ai.tegmentum.wasmtime4j",
          packageName,
          "ComponentLinker should be in ai.tegmentum.wasmtime4j package");
    }

    @Test
    @DisplayName("ComponentLinker should support try-with-resources pattern")
    void shouldSupportTryWithResources() {
      assertTrue(
          Closeable.class.isAssignableFrom(ComponentLinker.class),
          "ComponentLinker should be assignable to Closeable for try-with-resources");
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentLinker.class),
          "ComponentLinker should be assignable to AutoCloseable");
    }

    @Test
    @DisplayName("instantiate should return ComponentInstance")
    void instantiateShouldReturnComponentInstance() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("instantiate", Store.class, Component.class);
      assertEquals(
          ComponentInstance.class,
          method.getReturnType(),
          "instantiate should return ComponentInstance");
    }

    @Test
    @DisplayName("linkComponent should return ComponentInstance")
    void linkComponentShouldReturnComponentInstance() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("linkComponent", Store.class, Component.class);
      assertEquals(
          ComponentInstance.class,
          method.getReturnType(),
          "linkComponent should return ComponentInstance");
    }
  }
}
