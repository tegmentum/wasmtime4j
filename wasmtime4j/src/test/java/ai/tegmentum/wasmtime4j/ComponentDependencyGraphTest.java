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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentDependencyGraph} class.
 *
 * <p>ComponentDependencyGraph tracks component dependencies and provides algorithms for dependency
 * resolution, circular dependency detection, and topological sorting.
 */
@DisplayName("ComponentDependencyGraph Tests")
class ComponentDependencyGraphTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentDependencyGraph.class.getModifiers()),
          "ComponentDependencyGraph should be public");
      assertTrue(
          Modifier.isFinal(ComponentDependencyGraph.class.getModifiers()),
          "ComponentDependencyGraph should be final");
      assertFalse(
          ComponentDependencyGraph.class.isInterface(),
          "ComponentDependencyGraph should not be an interface");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with Component parameter")
    void shouldHaveConstructorWithComponentParameter() throws NoSuchMethodException {
      assertNotNull(
          ComponentDependencyGraph.class.getConstructor(Component.class),
          "Constructor with Component should exist");
    }

    @Test
    @DisplayName("should create graph with null root component")
    void shouldCreateGraphWithNullRootComponent() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      assertNull(graph.getRootComponent(), "Root component should be null");
      assertTrue(graph.getDependencies().isEmpty(), "Dependencies should be empty");
      assertFalse(graph.hasCircularDependencies(), "Should have no circular dependencies");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getRootComponent method")
    void shouldHaveGetRootComponentMethod() throws NoSuchMethodException {
      final Method method = ComponentDependencyGraph.class.getMethod("getRootComponent");
      assertNotNull(method, "getRootComponent method should exist");
      assertEquals(Component.class, method.getReturnType(), "Should return Component");
    }

    @Test
    @DisplayName("should have getDependencies method")
    void shouldHaveGetDependenciesMethod() throws NoSuchMethodException {
      final Method method = ComponentDependencyGraph.class.getMethod("getDependencies");
      assertNotNull(method, "getDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getCircularDependencies method")
    void shouldHaveGetCircularDependenciesMethod() throws NoSuchMethodException {
      final Method method = ComponentDependencyGraph.class.getMethod("getCircularDependencies");
      assertNotNull(method, "getCircularDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have hasCircularDependencies method")
    void shouldHaveHasCircularDependenciesMethod() throws NoSuchMethodException {
      final Method method = ComponentDependencyGraph.class.getMethod("hasCircularDependencies");
      assertNotNull(method, "hasCircularDependencies method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have addDependency method")
    void shouldHaveAddDependencyMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDependencyGraph.class.getMethod("addDependency", Component.class);
      assertNotNull(method, "addDependency method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getDependencyCount method")
    void shouldHaveGetDependencyCountMethod() throws NoSuchMethodException {
      final Method method = ComponentDependencyGraph.class.getMethod("getDependencyCount");
      assertNotNull(method, "getDependencyCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Initial State Tests")
  class InitialStateTests {

    @Test
    @DisplayName("should have empty dependencies initially")
    void shouldHaveEmptyDependenciesInitially() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      assertTrue(graph.getDependencies().isEmpty(), "Dependencies should be empty initially");
      assertEquals(0, graph.getDependencyCount(), "Dependency count should be 0 initially");
    }

    @Test
    @DisplayName("should have empty circular dependencies initially")
    void shouldHaveEmptyCircularDependenciesInitially() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      assertTrue(
          graph.getCircularDependencies().isEmpty(),
          "Circular dependencies should be empty initially");
      assertFalse(
          graph.hasCircularDependencies(), "Should not have circular dependencies initially");
    }
  }

  @Nested
  @DisplayName("Dependency Operations Tests")
  class DependencyOperationsTests {

    @Test
    @DisplayName("should add dependency to graph")
    void shouldAddDependencyToGraph() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);
      final Component mockDependency = createMockComponent();

      graph.addDependency(mockDependency);

      assertEquals(1, graph.getDependencyCount(), "Should have one dependency after adding");
      assertTrue(
          graph.getDependencies().contains(mockDependency),
          "Dependencies should contain added one");
    }

    @Test
    @DisplayName("should return defensive copy of dependencies")
    void shouldReturnDefensiveCopyOfDependencies() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);
      final Component mockDependency = createMockComponent();
      graph.addDependency(mockDependency);

      final Set<Component> deps1 = graph.getDependencies();
      final Set<Component> deps2 = graph.getDependencies();

      assertNotSame(deps1, deps2, "Should return different Set instances");
      assertEquals(deps1, deps2, "Sets should have same content");
    }

    @Test
    @DisplayName("should return defensive copy of circular dependencies")
    void shouldReturnDefensiveCopyOfCircularDependencies() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      final Set<String> circular1 = graph.getCircularDependencies();
      final Set<String> circular2 = graph.getCircularDependencies();

      assertNotSame(circular1, circular2, "Should return different Set instances");
    }

    @Test
    @DisplayName("should handle adding null dependency")
    void shouldHandleAddingNullDependency() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      graph.addDependency(null);

      // null is added to the set, which is an implementation detail
      assertTrue(graph.getDependencyCount() <= 1, "Should handle null dependency gracefully");
    }
  }

  @Nested
  @DisplayName("Multiple Dependencies Tests")
  class MultipleDependenciesTests {

    @Test
    @DisplayName("should track multiple dependencies")
    void shouldTrackMultipleDependencies() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      graph.addDependency(createMockComponent());
      graph.addDependency(createMockComponent());
      graph.addDependency(createMockComponent());

      assertEquals(3, graph.getDependencyCount(), "Should have three dependencies");
    }

    @Test
    @DisplayName("should not add duplicate dependencies when same instance")
    void shouldNotAddDuplicateDependencies() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);
      final Component sameDep = createMockComponent();

      graph.addDependency(sameDep);
      graph.addDependency(sameDep);

      assertEquals(1, graph.getDependencyCount(), "Should not add same instance twice");
    }
  }

  @Nested
  @DisplayName("Circular Dependency Detection Tests")
  class CircularDependencyDetectionTests {

    @Test
    @DisplayName("should report no circular dependencies for empty graph")
    void shouldReportNoCircularDependenciesForEmptyGraph() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      assertFalse(
          graph.hasCircularDependencies(), "Empty graph should have no circular dependencies");
      assertTrue(
          graph.getCircularDependencies().isEmpty(), "Circular dependencies set should be empty");
    }

    @Test
    @DisplayName("should report no circular dependencies for simple linear graph")
    void shouldReportNoCircularDependenciesForSimpleLinearGraph() {
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(null);

      graph.addDependency(createMockComponent());
      graph.addDependency(createMockComponent());

      assertFalse(
          graph.hasCircularDependencies(), "Linear graph should have no circular dependencies");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle graph with only root component")
    void shouldHandleGraphWithOnlyRootComponent() {
      final Component root = createMockComponent();
      final ComponentDependencyGraph graph = new ComponentDependencyGraph(root);

      assertEquals(root, graph.getRootComponent(), "Root component should be set");
      assertTrue(graph.getDependencies().isEmpty(), "Should have no dependencies");
      assertEquals(0, graph.getDependencyCount(), "Dependency count should be 0");
    }
  }

  /**
   * Creates a mock Component for testing.
   *
   * @return mock Component
   */
  private Component createMockComponent() {
    return new StubComponent("test-component-" + System.nanoTime());
  }

  /** Minimal stub implementation of Component for testing. */
  private static class StubComponent implements Component {
    private final String id;

    StubComponent(final String id) {
      this.id = id;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public ComponentVersion getVersion() {
      return new ComponentVersion(1, 0, 0);
    }

    @Override
    public long getSize() {
      return 0;
    }

    @Override
    public ComponentMetadata getMetadata() {
      return null;
    }

    @Override
    public boolean exportsInterface(final String interfaceName) {
      return false;
    }

    @Override
    public boolean importsInterface(final String interfaceName) {
      return false;
    }

    @Override
    public java.util.Set<String> getExportedInterfaces() {
      return java.util.Collections.emptySet();
    }

    @Override
    public java.util.Set<String> getImportedInterfaces() {
      return java.util.Collections.emptySet();
    }

    @Override
    public ComponentInstance instantiate() {
      return null;
    }

    @Override
    public ComponentInstance instantiate(final ComponentInstanceConfig config) {
      return null;
    }

    @Override
    public ComponentDependencyGraph getDependencyGraph() {
      return null;
    }

    @Override
    public java.util.Set<Component> resolveDependencies(final ComponentRegistry registry) {
      return java.util.Collections.emptySet();
    }

    @Override
    public ComponentCompatibility checkCompatibility(final Component other) {
      return null;
    }

    @Override
    public WitInterfaceDefinition getWitInterface() {
      return null;
    }

    @Override
    public WitCompatibilityResult checkWitCompatibility(final Component other) {
      return null;
    }

    @Override
    public ComponentResourceUsage getResourceUsage() {
      return null;
    }

    @Override
    public ComponentLifecycleState getLifecycleState() {
      return null;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public ComponentValidationResult validate(final ComponentValidationConfig validationConfig) {
      return null;
    }

    @Override
    public void close() {
      // No-op
    }
  }
}
