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
import ai.tegmentum.wasmtime4j.component.ComponentRegistry;
import ai.tegmentum.wasmtime4j.component.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentRegistry} interface.
 *
 * <p>ComponentRegistry provides centralized management of WebAssembly components including
 * registration, discovery, dependency resolution, and lifecycle management.
 */
@DisplayName("ComponentRegistry Tests")
class ComponentRegistryTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentRegistry.class.getModifiers()),
          "ComponentRegistry should be public");
      assertTrue(ComponentRegistry.class.isInterface(), "ComponentRegistry should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have register method with component parameter")
    void shouldHaveRegisterMethodWithComponent() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("register", Component.class);
      assertNotNull(method, "register(Component) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have register method with name and component parameters")
    void shouldHaveRegisterMethodWithNameAndComponent() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("register", String.class, Component.class);
      assertNotNull(method, "register(String, Component) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have unregister method")
    void shouldHaveUnregisterMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("unregister", String.class);
      assertNotNull(method, "unregister method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have findById method")
    void shouldHaveFindByIdMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("findById", String.class);
      assertNotNull(method, "findById method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have findByName method")
    void shouldHaveFindByNameMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("findByName", String.class);
      assertNotNull(method, "findByName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have findByVersion method")
    void shouldHaveFindByVersionMethod() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("findByVersion", ComponentVersion.class);
      assertNotNull(method, "findByVersion method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getAllComponents method")
    void shouldHaveGetAllComponentsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("getAllComponents");
      assertNotNull(method, "getAllComponents method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getAllComponentIds method")
    void shouldHaveGetAllComponentIdsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("getAllComponentIds");
      assertNotNull(method, "getAllComponentIds method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have isRegistered method")
    void shouldHaveIsRegisteredMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("isRegistered", String.class);
      assertNotNull(method, "isRegistered method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getComponentCount method")
    void shouldHaveGetComponentCountMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("getComponentCount");
      assertNotNull(method, "getComponentCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have resolveDependencies method")
    void shouldHaveResolveDependenciesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("resolveDependencies", Component.class);
      assertNotNull(method, "resolveDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have validateDependencies method")
    void shouldHaveValidateDependenciesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("validateDependencies", Component.class);
      assertNotNull(method, "validateDependencies method should exist");
      assertEquals(
          ComponentValidationResult.class,
          method.getReturnType(),
          "Should return ComponentValidationResult");
    }

    @Test
    @DisplayName("should have search method")
    void shouldHaveSearchMethod() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("search", ComponentSearchCriteria.class);
      assertNotNull(method, "search method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ComponentRegistryStatistics.class,
          method.getReturnType(),
          "Should return ComponentRegistryStatistics");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("register method should declare WasmException")
    void registerMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("register", Component.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          containsException(exceptionTypes, WasmException.class),
          "register should declare WasmException");
    }

    @Test
    @DisplayName("unregister method should declare WasmException")
    void unregisterMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("unregister", String.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          containsException(exceptionTypes, WasmException.class),
          "unregister should declare WasmException");
    }

    @Test
    @DisplayName("resolveDependencies method should declare WasmException")
    void resolveDependenciesMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("resolveDependencies", Component.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          containsException(exceptionTypes, WasmException.class),
          "resolveDependencies should declare WasmException");
    }

    @Test
    @DisplayName("validateDependencies method should declare WasmException")
    void validateDependenciesMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("validateDependencies", Component.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          containsException(exceptionTypes, WasmException.class),
          "validateDependencies should declare WasmException");
    }

    @Test
    @DisplayName("search method should declare WasmException")
    void searchMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ComponentRegistry.class.getMethod("search", ComponentSearchCriteria.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          containsException(exceptionTypes, WasmException.class),
          "search should declare WasmException");
    }

    @Test
    @DisplayName("clear method should declare WasmException")
    void clearMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentRegistry.class.getMethod("clear");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          containsException(exceptionTypes, WasmException.class),
          "clear should declare WasmException");
    }

    private boolean containsException(
        final Class<?>[] exceptionTypes, final Class<?> targetException) {
      for (Class<?> exceptionType : exceptionTypes) {
        if (exceptionType.equals(targetException)) {
          return true;
        }
      }
      return false;
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    @Test
    @DisplayName("should support empty registry implementation")
    void shouldSupportEmptyRegistryImplementation() {
      final ComponentRegistry registry = createEmptyRegistry();

      assertEquals(0, registry.getComponentCount(), "Empty registry should have zero components");
      assertTrue(
          registry.getAllComponents().isEmpty(),
          "Empty registry should return empty component set");
      assertTrue(
          registry.getAllComponentIds().isEmpty(),
          "Empty registry should return empty component ID set");
    }

    @Test
    @DisplayName("should support findById returning empty")
    void shouldSupportFindByIdReturningEmpty() {
      final ComponentRegistry registry = createEmptyRegistry();

      final Optional<Component> result = registry.findById("nonexistent");
      assertTrue(result.isEmpty(), "Should return empty for nonexistent component");
    }

    @Test
    @DisplayName("should support findByName returning empty")
    void shouldSupportFindByNameReturningEmpty() {
      final ComponentRegistry registry = createEmptyRegistry();

      final Optional<Component> result = registry.findByName("nonexistent");
      assertTrue(result.isEmpty(), "Should return empty for nonexistent component");
    }

    @Test
    @DisplayName("should support isRegistered returning false")
    void shouldSupportIsRegisteredReturningFalse() {
      final ComponentRegistry registry = createEmptyRegistry();

      final boolean result = registry.isRegistered("nonexistent");
      assertTrue(!result, "Should return false for unregistered component");
    }

    @Test
    @DisplayName("should support getStatistics returning valid statistics")
    void shouldSupportGetStatisticsReturningValidStatistics() {
      final ComponentRegistry registry = createEmptyRegistry();

      final ComponentRegistryStatistics stats = registry.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
    }
  }

  /**
   * Creates an empty registry stub implementation.
   *
   * @return empty registry implementation
   */
  private ComponentRegistry createEmptyRegistry() {
    return new ComponentRegistry() {
      @Override
      public void register(final Component component) throws WasmException {
        // Empty implementation
      }

      @Override
      public void register(final String name, final Component component) throws WasmException {
        // Empty implementation
      }

      @Override
      public void unregister(final String componentId) throws WasmException {
        // Empty implementation
      }

      @Override
      public Optional<Component> findById(final String componentId) {
        return Optional.empty();
      }

      @Override
      public Optional<Component> findByName(final String name) {
        return Optional.empty();
      }

      @Override
      public List<Component> findByVersion(final ComponentVersion version) {
        return List.of();
      }

      @Override
      public Set<Component> getAllComponents() {
        return Set.of();
      }

      @Override
      public Set<String> getAllComponentIds() {
        return Set.of();
      }

      @Override
      public boolean isRegistered(final String componentId) {
        return false;
      }

      @Override
      public int getComponentCount() {
        return 0;
      }

      @Override
      public Set<Component> resolveDependencies(final Component component) throws WasmException {
        return Set.of();
      }

      @Override
      public ComponentValidationResult validateDependencies(final Component component)
          throws WasmException {
        return ComponentValidationResult.success(
            new ComponentValidationResult.ValidationContext("test-component", null));
      }

      @Override
      public List<Component> search(final ComponentSearchCriteria criteria) throws WasmException {
        return List.of();
      }

      @Override
      public void clear() throws WasmException {
        // Empty implementation
      }

      @Override
      public ComponentRegistryStatistics getStatistics() {
        return ComponentRegistryStatistics.builder().build();
      }
    };
  }
}
