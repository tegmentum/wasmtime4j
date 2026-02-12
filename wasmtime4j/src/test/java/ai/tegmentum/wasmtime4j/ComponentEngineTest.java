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

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentRegistry;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitSupportInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ComponentEngine} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the ComponentEngine
 * API, which provides component-specific compilation and management capabilities.
 */
@DisplayName("ComponentEngine Interface Tests")
class ComponentEngineTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentEngine Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ComponentEngine should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentEngine.class.isInterface(), "ComponentEngine should be an interface");
    }

    @Test
    @DisplayName("ComponentEngine should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentEngine.class.getModifiers()),
          "ComponentEngine should be public");
    }

    @Test
    @DisplayName("ComponentEngine should extend Engine interface")
    void shouldExtendEngineInterface() {
      Class<?>[] interfaces = ComponentEngine.class.getInterfaces();
      assertEquals(1, interfaces.length, "ComponentEngine should extend exactly one interface");
      assertEquals(Engine.class, interfaces[0], "ComponentEngine should extend Engine interface");
    }
  }

  // ========================================================================
  // Component Compilation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Component Compilation Method Tests")
  class CompilationMethodTests {

    @Test
    @DisplayName("should have compileComponent(byte[]) method")
    void shouldHaveCompileComponentWithBytesMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("compileComponent", byte[].class);
      assertNotNull(method, "compileComponent(byte[]) method should exist");
      assertEquals(Component.class, method.getReturnType(), "Return type should be Component");
      assertEquals(1, method.getParameterCount(), "compileComponent should have 1 parameter");
      assertEquals(byte[].class, method.getParameterTypes()[0], "Parameter should be byte[]");
    }

    @Test
    @DisplayName("compileComponent(byte[]) should throw WasmException")
    void compileComponentShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("compileComponent", byte[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have compileComponent(byte[], String) method")
    void shouldHaveCompileComponentWithBytesAndNameMethod() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod("compileComponent", byte[].class, String.class);
      assertNotNull(method, "compileComponent(byte[], String) method should exist");
      assertEquals(Component.class, method.getReturnType(), "Return type should be Component");
      assertEquals(2, method.getParameterCount(), "compileComponent should have 2 parameters");
      assertEquals(byte[].class, method.getParameterTypes()[0], "First parameter should be byte[]");
      assertEquals(
          String.class, method.getParameterTypes()[1], "Second parameter should be String");
    }

    @Test
    @DisplayName("compileComponent(byte[], String) should throw WasmException")
    void compileComponentWithNameShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod("compileComponent", byte[].class, String.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Component Linking Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Component Linking Method Tests")
  class LinkingMethodTests {

    @Test
    @DisplayName("should have linkComponents method")
    void shouldHaveLinkComponentsMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("linkComponents", List.class);
      assertNotNull(method, "linkComponents method should exist");
      assertEquals(Component.class, method.getReturnType(), "Return type should be Component");
      assertEquals(1, method.getParameterCount(), "linkComponents should have 1 parameter");
    }

    @Test
    @DisplayName("linkComponents should accept List<Component> parameter")
    void linkComponentsShouldAcceptComponentList() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("linkComponents", List.class);
      Type[] genericParameterTypes = method.getGenericParameterTypes();
      assertEquals(1, genericParameterTypes.length, "Should have 1 generic parameter type");

      if (genericParameterTypes[0] instanceof ParameterizedType) {
        ParameterizedType paramType = (ParameterizedType) genericParameterTypes[0];
        assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
        Type[] typeArgs = paramType.getActualTypeArguments();
        assertEquals(1, typeArgs.length, "List should have one type argument");
        assertEquals(Component.class, typeArgs[0], "List type argument should be Component");
      }
    }

    @Test
    @DisplayName("linkComponents should throw WasmException")
    void linkComponentsShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("linkComponents", List.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Compatibility Check Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Compatibility Check Method Tests")
  class CompatibilityCheckTests {

    @Test
    @DisplayName("should have checkCompatibility method")
    void shouldHaveCheckCompatibilityMethod() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod("checkCompatibility", Component.class, Component.class);
      assertNotNull(method, "checkCompatibility method should exist");
      assertEquals(
          WitCompatibilityResult.class,
          method.getReturnType(),
          "Return type should be WitCompatibilityResult");
      assertEquals(2, method.getParameterCount(), "checkCompatibility should have 2 parameters");
      assertEquals(
          Component.class, method.getParameterTypes()[0], "First parameter should be Component");
      assertEquals(
          Component.class, method.getParameterTypes()[1], "Second parameter should be Component");
    }

    @Test
    @DisplayName("checkCompatibility should not throw checked exceptions")
    void checkCompatibilityShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod("checkCompatibility", Component.class, Component.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(
          0, exceptionTypes.length, "checkCompatibility should not declare checked exceptions");
    }
  }

  // ========================================================================
  // Registry Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Registry Method Tests")
  class RegistryMethodTests {

    @Test
    @DisplayName("should have getRegistry method")
    void shouldHaveGetRegistryMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("getRegistry");
      assertNotNull(method, "getRegistry method should exist");
      assertEquals(
          ComponentRegistry.class,
          method.getReturnType(),
          "Return type should be ComponentRegistry");
      assertEquals(0, method.getParameterCount(), "getRegistry should have no parameters");
    }

    @Test
    @DisplayName("should have setRegistry method")
    void shouldHaveSetRegistryMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("setRegistry", ComponentRegistry.class);
      assertNotNull(method, "setRegistry method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setRegistry should have 1 parameter");
      assertEquals(
          ComponentRegistry.class,
          method.getParameterTypes()[0],
          "Parameter should be ComponentRegistry");
    }

    @Test
    @DisplayName("getRegistry should not throw checked exceptions")
    void getRegistryShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("getRegistry");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(0, exceptionTypes.length, "getRegistry should not declare checked exceptions");
    }

    @Test
    @DisplayName("setRegistry should not throw checked exceptions")
    void setRegistryShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("setRegistry", ComponentRegistry.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(0, exceptionTypes.length, "setRegistry should not declare checked exceptions");
    }
  }

  // ========================================================================
  // Instance Creation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Creation Method Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should have createInstance(Component, Store) method")
    void shouldHaveCreateInstanceMethod() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod("createInstance", Component.class, Store.class);
      assertNotNull(method, "createInstance(Component, Store) method should exist");
      assertEquals(
          ComponentInstance.class,
          method.getReturnType(),
          "Return type should be ComponentInstance");
      assertEquals(2, method.getParameterCount(), "createInstance should have 2 parameters");
      assertEquals(
          Component.class, method.getParameterTypes()[0], "First parameter should be Component");
      assertEquals(Store.class, method.getParameterTypes()[1], "Second parameter should be Store");
    }

    @Test
    @DisplayName("createInstance(Component, Store) should throw WasmException")
    void createInstanceShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod("createInstance", Component.class, Store.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have createInstance(Component, Store, List) method")
    void shouldHaveCreateInstanceWithImportsMethod() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod(
              "createInstance", Component.class, Store.class, List.class);
      assertNotNull(method, "createInstance(Component, Store, List) method should exist");
      assertEquals(
          ComponentInstance.class,
          method.getReturnType(),
          "Return type should be ComponentInstance");
      assertEquals(3, method.getParameterCount(), "createInstance should have 3 parameters");
      assertEquals(
          Component.class, method.getParameterTypes()[0], "First parameter should be Component");
      assertEquals(Store.class, method.getParameterTypes()[1], "Second parameter should be Store");
      assertEquals(List.class, method.getParameterTypes()[2], "Third parameter should be List");
    }

    @Test
    @DisplayName("createInstance(Component, Store, List) should throw WasmException")
    void createInstanceWithImportsShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod(
              "createInstance", Component.class, Store.class, List.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Validation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validateComponent method")
    void shouldHaveValidateComponentMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("validateComponent", Component.class);
      assertNotNull(method, "validateComponent method should exist");
      assertEquals(
          ComponentValidationResult.class,
          method.getReturnType(),
          "Return type should be ComponentValidationResult");
      assertEquals(1, method.getParameterCount(), "validateComponent should have 1 parameter");
      assertEquals(Component.class, method.getParameterTypes()[0], "Parameter should be Component");
    }

    @Test
    @DisplayName("validateComponent should not throw checked exceptions")
    void validateComponentShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("validateComponent", Component.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(
          0, exceptionTypes.length, "validateComponent should not declare checked exceptions");
    }
  }

  // ========================================================================
  // WIT Support Method Tests
  // ========================================================================

  @Nested
  @DisplayName("WIT Support Method Tests")
  class WitSupportTests {

    @Test
    @DisplayName("should have getWitSupportInfo method")
    void shouldHaveGetWitSupportInfoMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("getWitSupportInfo");
      assertNotNull(method, "getWitSupportInfo method should exist");
      assertEquals(
          WitSupportInfo.class, method.getReturnType(), "Return type should be WitSupportInfo");
      assertEquals(0, method.getParameterCount(), "getWitSupportInfo should have no parameters");
    }

    @Test
    @DisplayName("getWitSupportInfo should not throw checked exceptions")
    void getWitSupportInfoShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("getWitSupportInfo");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(
          0, exceptionTypes.length, "getWitSupportInfo should not declare checked exceptions");
    }
  }

  // ========================================================================
  // Component Model Support Tests
  // ========================================================================

  @Nested
  @DisplayName("Component Model Support Tests")
  class ComponentModelSupportTests {

    @Test
    @DisplayName("should have supportsComponentModel method")
    void shouldHaveSupportsComponentModelMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("supportsComponentModel");
      assertNotNull(method, "supportsComponentModel method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(
          0, method.getParameterCount(), "supportsComponentModel should have no parameters");
    }

    @Test
    @DisplayName("supportsComponentModel should not throw checked exceptions")
    void supportsComponentModelShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("supportsComponentModel");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(
          0, exceptionTypes.length, "supportsComponentModel should not declare checked exceptions");
    }
  }

  // ========================================================================
  // Max Link Depth Tests
  // ========================================================================

  @Nested
  @DisplayName("Max Link Depth Tests")
  class MaxLinkDepthTests {

    @Test
    @DisplayName("should have getMaxLinkDepth method")
    void shouldHaveGetMaxLinkDepthMethod() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("getMaxLinkDepth");
      assertNotNull(method, "getMaxLinkDepth method should exist");
      assertEquals(0, method.getParameterCount(), "getMaxLinkDepth should have no parameters");
    }

    @Test
    @DisplayName("getMaxLinkDepth should return Optional<Integer>")
    void getMaxLinkDepthShouldReturnOptionalInteger() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("getMaxLinkDepth");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");

      Type genericReturnType = method.getGenericReturnType();
      if (genericReturnType instanceof ParameterizedType) {
        ParameterizedType paramType = (ParameterizedType) genericReturnType;
        assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
        Type[] typeArgs = paramType.getActualTypeArguments();
        assertEquals(1, typeArgs.length, "Optional should have one type argument");
        assertEquals(Integer.class, typeArgs[0], "Optional type argument should be Integer");
      }
    }

    @Test
    @DisplayName("getMaxLinkDepth should not throw checked exceptions")
    void getMaxLinkDepthShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentEngine.class.getMethod("getMaxLinkDepth");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(
          0, exceptionTypes.length, "getMaxLinkDepth should not declare checked exceptions");
    }
  }

  // ========================================================================
  // Method Count Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count and Completeness Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ComponentEngine should have expected component-specific methods")
    void shouldHaveExpectedComponentMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "compileComponent",
                  "linkComponents",
                  "checkCompatibility",
                  "getRegistry",
                  "setRegistry",
                  "createInstance",
                  "validateComponent",
                  "getWitSupportInfo",
                  "supportsComponentModel",
                  "getMaxLinkDepth"));

      Method[] methods = ComponentEngine.class.getDeclaredMethods();
      Set<String> actualMethodNames = new HashSet<>();
      for (Method method : methods) {
        actualMethodNames.add(method.getName());
      }

      for (String expectedMethod : expectedMethods) {
        assertTrue(
            actualMethodNames.contains(expectedMethod),
            "ComponentEngine should declare " + expectedMethod + " method");
      }
    }

    @Test
    @DisplayName("All ComponentEngine methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ComponentEngine.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            "Method " + method.getName() + " should be public and abstract");
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
    @DisplayName("ComponentEngine should be in ai.tegmentum.wasmtime4j.component package")
    void shouldBeInCorrectPackage() {
      String packageName = ComponentEngine.class.getPackage().getName();
      assertEquals(
          "ai.tegmentum.wasmtime4j.component",
          packageName,
          "ComponentEngine should be in ai.tegmentum.wasmtime4j.component package");
    }

    @Test
    @DisplayName("ComponentEngine extends Engine which extends Closeable")
    void shouldExtendCloseableViaEngine() {
      // ComponentEngine -> Engine -> Closeable
      assertTrue(
          Engine.class.isAssignableFrom(ComponentEngine.class),
          "ComponentEngine should be assignable to Engine");
      assertTrue(
          java.io.Closeable.class.isAssignableFrom(Engine.class),
          "Engine should be assignable to Closeable");
    }

    @Test
    @DisplayName("ComponentEngine should be usable with try-with-resources pattern")
    void shouldSupportTryWithResources() {
      // Verify the inheritance chain supports AutoCloseable
      assertTrue(
          java.lang.AutoCloseable.class.isAssignableFrom(ComponentEngine.class),
          "ComponentEngine should be assignable to AutoCloseable for try-with-resources");
    }
  }

  // ========================================================================
  // Parameter Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Parameter Type Tests")
  class ParameterTypeTests {

    @Test
    @DisplayName("createInstance third parameter should be List<Component>")
    void createInstanceThirdParameterShouldBeComponentList() throws NoSuchMethodException {
      Method method =
          ComponentEngine.class.getMethod(
              "createInstance", Component.class, Store.class, List.class);
      Type[] genericParameterTypes = method.getGenericParameterTypes();
      assertEquals(3, genericParameterTypes.length, "Should have 3 generic parameter types");

      // Third parameter should be List<Component>
      if (genericParameterTypes[2] instanceof ParameterizedType) {
        ParameterizedType paramType = (ParameterizedType) genericParameterTypes[2];
        assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
        Type[] typeArgs = paramType.getActualTypeArguments();
        assertEquals(1, typeArgs.length, "List should have one type argument");
        assertEquals(Component.class, typeArgs[0], "List type argument should be Component");
      }
    }

    @Test
    @DisplayName("All methods should use appropriate parameter types")
    void allMethodsShouldUseAppropriateParameterTypes() throws NoSuchMethodException {
      // Verify component-related parameters use Component
      Method compileMethod = ComponentEngine.class.getMethod("compileComponent", byte[].class);
      assertEquals(
          Component.class,
          compileMethod.getReturnType(),
          "compileComponent should return Component");

      // Verify store-related parameters use Store
      Method createMethod =
          ComponentEngine.class.getMethod("createInstance", Component.class, Store.class);
      assertEquals(
          Store.class, createMethod.getParameterTypes()[1], "createInstance should accept Store");

      // Verify registry-related parameters use ComponentRegistry
      Method setRegistryMethod =
          ComponentEngine.class.getMethod("setRegistry", ComponentRegistry.class);
      assertEquals(
          ComponentRegistry.class,
          setRegistryMethod.getParameterTypes()[0],
          "setRegistry should accept ComponentRegistry");
    }
  }
}
