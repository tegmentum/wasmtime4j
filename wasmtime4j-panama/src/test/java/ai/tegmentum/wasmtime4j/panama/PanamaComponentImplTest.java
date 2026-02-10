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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentCompatibility;
import ai.tegmentum.wasmtime4j.ComponentDependencyGraph;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentLifecycleState;
import ai.tegmentum.wasmtime4j.ComponentMetadata;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentValidationConfig;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaComponentImpl} class.
 *
 * <p>PanamaComponent is the Panama implementation of Component.
 */
@DisplayName("PanamaComponentImpl Tests")
class PanamaComponentImplTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      assertTrue(
          !Modifier.isPublic(PanamaComponentImpl.class.getModifiers()),
          "PanamaComponentImpl should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaComponentImpl.class.getModifiers()),
          "PanamaComponentImpl should be final");
    }

    @Test
    @DisplayName("should implement Component interface")
    void shouldImplementComponentInterface() {
      assertTrue(
          Component.class.isAssignableFrom(PanamaComponentImpl.class),
          "PanamaComponentImpl should implement Component");
    }
  }

  @Nested
  @DisplayName("Component Method Tests")
  class ComponentMethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(
          ComponentVersion.class, method.getReturnType(), "Should return ComponentVersion");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(
          ComponentMetadata.class, method.getReturnType(), "Should return ComponentMetadata");
    }

    @Test
    @DisplayName("should have exportsInterface method")
    void shouldHaveExportsInterfaceMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("exportsInterface", String.class);
      assertNotNull(method, "exportsInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have importsInterface method")
    void shouldHaveImportsInterfaceMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("importsInterface", String.class);
      assertNotNull(method, "importsInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getImportedInterfaces method")
    void shouldHaveGetImportedInterfacesMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getImportedInterfaces");
      assertNotNull(method, "getImportedInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("instantiate");
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
    }

    @Test
    @DisplayName("should have instantiate method with config")
    void shouldHaveInstantiateMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          PanamaComponentImpl.class.getMethod("instantiate", ComponentInstanceConfig.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
    }

    @Test
    @DisplayName("should have getDependencyGraph method")
    void shouldHaveGetDependencyGraphMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getDependencyGraph");
      assertNotNull(method, "getDependencyGraph method should exist");
      assertEquals(
          ComponentDependencyGraph.class,
          method.getReturnType(),
          "Should return ComponentDependencyGraph");
    }

    @Test
    @DisplayName("should have resolveDependencies method")
    void shouldHaveResolveDependenciesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentImpl.class.getMethod("resolveDependencies", ComponentRegistry.class);
      assertNotNull(method, "resolveDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have checkCompatibility method")
    void shouldHaveCheckCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentImpl.class.getMethod("checkCompatibility", Component.class);
      assertNotNull(method, "checkCompatibility method should exist");
      assertEquals(
          ComponentCompatibility.class,
          method.getReturnType(),
          "Should return ComponentCompatibility");
    }

    @Test
    @DisplayName("should have getWitInterface method")
    void shouldHaveGetWitInterfaceMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getWitInterface");
      assertNotNull(method, "getWitInterface method should exist");
      assertEquals(
          WitInterfaceDefinition.class,
          method.getReturnType(),
          "Should return WitInterfaceDefinition");
    }

    @Test
    @DisplayName("should have checkWitCompatibility method")
    void shouldHaveCheckWitCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentImpl.class.getMethod("checkWitCompatibility", Component.class);
      assertNotNull(method, "checkWitCompatibility method should exist");
      assertEquals(
          WitCompatibilityResult.class,
          method.getReturnType(),
          "Should return WitCompatibilityResult");
    }

    @Test
    @DisplayName("should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getResourceUsage");
      assertNotNull(method, "getResourceUsage method should exist");
      assertEquals(
          ComponentResourceUsage.class,
          method.getReturnType(),
          "Should return ComponentResourceUsage");
    }

    @Test
    @DisplayName("should have getLifecycleState method")
    void shouldHaveGetLifecycleStateMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("getLifecycleState");
      assertNotNull(method, "getLifecycleState method should exist");
      assertEquals(
          ComponentLifecycleState.class,
          method.getReturnType(),
          "Should return ComponentLifecycleState");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentImpl.class.getMethod("validate", ComponentValidationConfig.class);
      assertNotNull(method, "validate method should exist");
      assertEquals(
          ComponentValidationResult.class,
          method.getReturnType(),
          "Should return ComponentValidationResult");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getDeclaredMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentImpl.class.getDeclaredMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(
          PanamaComponentEngine.class,
          method.getReturnType(),
          "Should return PanamaComponentEngine");
    }

  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with 3 parameters")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaComponentImpl.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 3
            && constructor.getParameterTypes()[0] == MemorySegment.class
            && constructor.getParameterTypes()[1] == String.class
            && constructor.getParameterTypes()[2] == PanamaComponentEngine.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(
          hasExpectedConstructor,
          "Should have constructor with MemorySegment, String, and PanamaComponentEngine");
    }
  }
}
