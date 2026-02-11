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

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentCapability;
import ai.tegmentum.wasmtime4j.component.ComponentCompatibility;
import ai.tegmentum.wasmtime4j.component.ComponentCompatibilityResult;
import ai.tegmentum.wasmtime4j.component.ComponentDebugInfo;
import ai.tegmentum.wasmtime4j.component.ComponentDependencyGraph;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.component.ComponentEngineDebugInfo;
import ai.tegmentum.wasmtime4j.component.ComponentFeature;
import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentId;
import ai.tegmentum.wasmtime4j.component.ComponentImportValidation;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleState;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentLinkInfo;
import ai.tegmentum.wasmtime4j.component.ComponentLoadConfig;
import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentRegistry;
import ai.tegmentum.wasmtime4j.component.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.component.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.component.ComponentResult;
import ai.tegmentum.wasmtime4j.component.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.component.ComponentSpecification;
import ai.tegmentum.wasmtime4j.component.ComponentStateTransitionConfig;
import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.component.ComponentValFactory;
import ai.tegmentum.wasmtime4j.component.ComponentValidationConfig;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentVariant;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ComponentInstance} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the
 * ComponentInstance API, which represents an instantiated WebAssembly component.
 */
@DisplayName("ComponentInstance Interface Tests")
class ComponentInstanceInterfaceTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentInstance Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ComponentInstance should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentInstance.class.isInterface(), "ComponentInstance should be an interface");
    }

    @Test
    @DisplayName("ComponentInstance should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentInstance.class.getModifiers()),
          "ComponentInstance should be public");
    }

    @Test
    @DisplayName("ComponentInstance should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      Class<?>[] interfaces = ComponentInstance.class.getInterfaces();
      assertEquals(1, interfaces.length, "ComponentInstance should extend exactly one interface");
      assertEquals(
          AutoCloseable.class, interfaces[0], "ComponentInstance should extend AutoCloseable");
    }
  }

  // ========================================================================
  // Identification Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Identification Method Tests")
  class IdentificationMethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getId should have no parameters");
    }

    @Test
    @DisplayName("should have getComponent method")
    void shouldHaveGetComponentMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getComponent");
      assertNotNull(method, "getComponent method should exist");
      assertEquals(
          Component.class, method.getReturnType(), "Return type should be Component");
      assertEquals(0, method.getParameterCount(), "getComponent should have no parameters");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          ComponentInstanceState.class,
          method.getReturnType(),
          "Return type should be ComponentInstanceState");
      assertEquals(0, method.getParameterCount(), "getState should have no parameters");
    }
  }

  // ========================================================================
  // Function Invocation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Function Invocation Method Tests")
  class FunctionInvocationTests {

    @Test
    @DisplayName("should have invoke method")
    void shouldHaveInvokeMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
      assertEquals(2, method.getParameterCount(), "invoke should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          Object[].class, method.getParameterTypes()[1], "Second parameter should be Object[]");
      assertTrue(method.isVarArgs(), "invoke should be a varargs method");
    }

    @Test
    @DisplayName("invoke should throw WasmException")
    void invokeShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("invoke", String.class, Object[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have hasFunction method")
    void shouldHaveHasFunctionMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("hasFunction", String.class);
      assertNotNull(method, "hasFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(1, method.getParameterCount(), "hasFunction should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("should have getFunc method")
    void shouldHaveGetFuncMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getFunc", String.class);
      assertNotNull(method, "getFunc method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
      assertEquals(1, method.getParameterCount(), "getFunc should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("getFunc should return Optional<ComponentFunction>")
    void getFuncShouldReturnOptionalComponentFunction() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getFunc", String.class);
      Type genericReturnType = method.getGenericReturnType();
      if (genericReturnType instanceof ParameterizedType) {
        ParameterizedType paramType = (ParameterizedType) genericReturnType;
        assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
        Type[] typeArgs = paramType.getActualTypeArguments();
        assertEquals(1, typeArgs.length, "Optional should have one type argument");
        assertEquals(
            ComponentFunction.class,
            typeArgs[0],
            "Optional type argument should be ComponentFunction");
      }
    }

    @Test
    @DisplayName("getFunc should throw WasmException")
    void getFuncShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getFunc", String.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Export Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Export Method Tests")
  class ExportMethodTests {

    @Test
    @DisplayName("should have getExportedFunctions method")
    void shouldHaveGetExportedFunctionsMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getExportedFunctions");
      assertNotNull(method, "getExportedFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
      assertEquals(0, method.getParameterCount(), "getExportedFunctions should have no parameters");
    }

    @Test
    @DisplayName("getExportedFunctions should return Set<String>")
    void getExportedFunctionsShouldReturnStringSet() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getExportedFunctions");
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
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
      assertEquals(
          0, method.getParameterCount(), "getExportedInterfaces should have no parameters");
    }

    @Test
    @DisplayName("getExportedInterfaces should throw WasmException")
    void getExportedInterfacesShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getExportedInterfaces");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Interface Binding Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Binding Method Tests")
  class InterfaceBindingTests {

    @Test
    @DisplayName("should have bindInterface method")
    void shouldHaveBindInterfaceMethod() throws NoSuchMethodException {
      Method method =
          ComponentInstance.class.getMethod("bindInterface", String.class, Object.class);
      assertNotNull(method, "bindInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "bindInterface should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          Object.class, method.getParameterTypes()[1], "Second parameter should be Object");
    }

    @Test
    @DisplayName("bindInterface should throw WasmException")
    void bindInterfaceShouldThrowWasmException() throws NoSuchMethodException {
      Method method =
          ComponentInstance.class.getMethod("bindInterface", String.class, Object.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Configuration and Resource Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Configuration and Resource Method Tests")
  class ConfigurationResourceTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ComponentInstanceConfig.class,
          method.getReturnType(),
          "Return type should be ComponentInstanceConfig");
      assertEquals(0, method.getParameterCount(), "getConfig should have no parameters");
    }

    @Test
    @DisplayName("should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getResourceUsage");
      assertNotNull(method, "getResourceUsage method should exist");
      assertEquals(
          ComponentResourceUsage.class,
          method.getReturnType(),
          "Return type should be ComponentResourceUsage");
      assertEquals(0, method.getParameterCount(), "getResourceUsage should have no parameters");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }

    @Test
    @DisplayName("should have pause method")
    void shouldHavePauseMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("pause");
      assertNotNull(method, "pause method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "pause should have no parameters");
    }

    @Test
    @DisplayName("pause should throw WasmException")
    void pauseShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("pause");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have resume method")
    void shouldHaveResumeMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("resume");
      assertNotNull(method, "resume method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "resume should have no parameters");
    }

    @Test
    @DisplayName("resume should throw WasmException")
    void resumeShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("resume");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "stop should have no parameters");
    }

    @Test
    @DisplayName("stop should throw WasmException")
    void stopShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("stop");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should declare one exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }

    @Test
    @DisplayName("close should not throw checked exceptions")
    void closeShouldNotThrowCheckedExceptions() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("close");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(0, exceptionTypes.length, "close should not declare checked exceptions");
    }
  }

  // ========================================================================
  // Method Count and Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count and Completeness Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ComponentInstance should have expected methods")
    void shouldHaveExpectedMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "getId",
                  "getComponent",
                  "getState",
                  "invoke",
                  "hasFunction",
                  "getFunc",
                  "getExportedFunctions",
                  "getExportedInterfaces",
                  "bindInterface",
                  "getConfig",
                  "getResourceUsage",
                  "isValid",
                  "pause",
                  "resume",
                  "stop",
                  "close"));

      Method[] methods = ComponentInstance.class.getDeclaredMethods();
      Set<String> actualMethodNames = new HashSet<>();
      for (Method method : methods) {
        actualMethodNames.add(method.getName());
      }

      for (String expectedMethod : expectedMethods) {
        assertTrue(
            actualMethodNames.contains(expectedMethod),
            "ComponentInstance should declare " + expectedMethod + " method");
      }
    }

    @Test
    @DisplayName("All ComponentInstance methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ComponentInstance.class.getDeclaredMethods();

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
    @DisplayName("ComponentInstance should be in ai.tegmentum.wasmtime4j package")
    void shouldBeInCorrectPackage() {
      String packageName = ComponentInstance.class.getPackage().getName();
      assertEquals(
          "ai.tegmentum.wasmtime4j",
          packageName,
          "ComponentInstance should be in ai.tegmentum.wasmtime4j package");
    }

    @Test
    @DisplayName("ComponentInstance should support try-with-resources pattern")
    void shouldSupportTryWithResources() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentInstance.class),
          "ComponentInstance should be assignable to AutoCloseable for try-with-resources");
    }

    @Test
    @DisplayName("getComponent should return Component type")
    void getComponentShouldReturnComponent() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getComponent");
      assertEquals(
          Component.class,
          method.getReturnType(),
          "getComponent should return Component");
    }

    @Test
    @DisplayName("getState should return ComponentInstanceState type")
    void getStateShouldReturnComponentInstanceState() throws NoSuchMethodException {
      Method method = ComponentInstance.class.getMethod("getState");
      assertEquals(
          ComponentInstanceState.class,
          method.getReturnType(),
          "getState should return ComponentInstanceState");
    }
  }
}
