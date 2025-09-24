/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a dependency graph for WebAssembly components with advanced analysis capabilities
 * including circular dependency detection, topological sorting, and dependency resolution.
 *
 * <p>This class provides sophisticated dependency management features including:
 *
 * <ul>
 *   <li>Circular dependency detection and analysis
 *   <li>Topological sorting for execution order determination
 *   <li>Dependency path analysis and optimization
 *   <li>Graph validation and integrity checking
 *   <li>Component isolation and impact analysis
 * </ul>
 *
 * @since 1.0.0
 */
public final class ComponentDependencyGraph {

  private final Map<Component, Set<Component>> dependencies;
  private final Map<Component, Set<Component>> dependents;
  private final Set<ComponentDependencyCycle> circularDependencies;
  private final boolean isValid;
  private final DependencyGraphMetrics metrics;

  /**
   * Creates a new component dependency graph.
   *
   * @param dependencies the component dependencies map
   * @throws WasmException if graph construction fails
   */
  public ComponentDependencyGraph(final Map<Component, Set<Component>> dependencies)
      throws WasmException {
    Objects.requireNonNull(dependencies, "Dependencies cannot be null");

    this.dependencies = new HashMap<>();
    this.dependents = new HashMap<>();

    // Build both forward and reverse dependency maps
    for (final Map.Entry<Component, Set<Component>> entry : dependencies.entrySet()) {
      final Component component = entry.getKey();
      final Set<Component> componentDeps = new HashSet<>(entry.getValue());

      this.dependencies.put(component, Collections.unmodifiableSet(componentDeps));

      // Build reverse dependency map
      for (final Component dependency : componentDeps) {
        this.dependents.computeIfAbsent(dependency, k -> new HashSet<>()).add(component);
      }
    }

    // Make dependents immutable
    for (final Map.Entry<Component, Set<Component>> entry : this.dependents.entrySet()) {
      entry.setValue(Collections.unmodifiableSet(entry.getValue()));
    }

    // Detect circular dependencies
    this.circularDependencies = detectCircularDependencies();
    this.isValid = circularDependencies.isEmpty();
    this.metrics = calculateMetrics();
  }

  /**
   * Gets all components in the dependency graph.
   *
   * @return the set of all components
   */
  public Set<Component> getAllComponents() {
    final Set<Component> allComponents = new HashSet<>();
    allComponents.addAll(dependencies.keySet());
    allComponents.addAll(dependents.keySet());
    return Collections.unmodifiableSet(allComponents);
  }

  /**
   * Gets the direct dependencies of a component.
   *
   * @param component the component to get dependencies for
   * @return the set of direct dependencies
   */
  public Set<Component> getDependencies(final Component component) {
    Objects.requireNonNull(component, "Component cannot be null");
    return dependencies.getOrDefault(component, Collections.emptySet());
  }

  /**
   * Gets the components that depend on a given component.
   *
   * @param component the component to get dependents for
   * @return the set of components that depend on the given component
   */
  public Set<Component> getDependents(final Component component) {
    Objects.requireNonNull(component, "Component cannot be null");
    return dependents.getOrDefault(component, Collections.emptySet());
  }

  /**
   * Gets all transitive dependencies of a component.
   *
   * @param component the component to get transitive dependencies for
   * @return the set of all transitive dependencies
   */
  public Set<Component> getTransitiveDependencies(final Component component) {
    Objects.requireNonNull(component, "Component cannot be null");

    final Set<Component> visited = new HashSet<>();
    final Set<Component> transitiveDeps = new HashSet<>();

    collectTransitiveDependencies(component, visited, transitiveDeps);

    return Collections.unmodifiableSet(transitiveDeps);
  }

  /**
   * Gets all transitive dependents of a component.
   *
   * @param component the component to get transitive dependents for
   * @return the set of all transitive dependents
   */
  public Set<Component> getTransitiveDependents(final Component component) {
    Objects.requireNonNull(component, "Component cannot be null");

    final Set<Component> visited = new HashSet<>();
    final Set<Component> transitiveDeps = new HashSet<>();

    collectTransitiveDependents(component, visited, transitiveDeps);

    return Collections.unmodifiableSet(transitiveDeps);
  }

  /**
   * Checks if the dependency graph contains circular dependencies.
   *
   * @return true if the graph has circular dependencies
   */
  public boolean hasCircularDependencies() {
    return !circularDependencies.isEmpty();
  }

  /**
   * Gets all circular dependencies in the graph.
   *
   * @return the set of circular dependency cycles
   */
  public Set<ComponentDependencyCycle> getCircularDependencies() {
    return Collections.unmodifiableSet(circularDependencies);
  }

  /**
   * Checks if the dependency graph is valid (no circular dependencies).
   *
   * @return true if the graph is valid
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Gets a topologically sorted list of components for execution order.
   *
   * @return the topologically sorted component list
   * @throws WasmException if the graph contains circular dependencies
   */
  public List<Component> getTopologicalOrder() throws WasmException {
    if (hasCircularDependencies()) {
      throw new WasmException(
          "Cannot perform topological sort on graph with circular dependencies");
    }

    return performTopologicalSort();
  }

  /**
   * Gets the dependency path between two components.
   *
   * @param from the source component
   * @param to the target component
   * @return the dependency path, or empty list if no path exists
   */
  public List<Component> getDependencyPath(final Component from, final Component to) {
    Objects.requireNonNull(from, "From component cannot be null");
    Objects.requireNonNull(to, "To component cannot be null");

    return findDependencyPath(from, to, new HashSet<>());
  }

  /**
   * Checks if one component depends on another (directly or transitively).
   *
   * @param component the component to check
   * @param dependency the potential dependency
   * @return true if component depends on dependency
   */
  public boolean dependsOn(final Component component, final Component dependency) {
    Objects.requireNonNull(component, "Component cannot be null");
    Objects.requireNonNull(dependency, "Dependency cannot be null");

    return getTransitiveDependencies(component).contains(dependency);
  }

  /**
   * Gets components that have no dependencies (root components).
   *
   * @return the set of root components
   */
  public Set<Component> getRootComponents() {
    return getAllComponents().stream()
        .filter(component -> getDependencies(component).isEmpty())
        .collect(Collectors.toSet());
  }

  /**
   * Gets components that have no dependents (leaf components).
   *
   * @return the set of leaf components
   */
  public Set<Component> getLeafComponents() {
    return getAllComponents().stream()
        .filter(component -> getDependents(component).isEmpty())
        .collect(Collectors.toSet());
  }

  /**
   * Gets isolated components that have neither dependencies nor dependents.
   *
   * @return the set of isolated components
   */
  public Set<Component> getIsolatedComponents() {
    return getAllComponents().stream()
        .filter(
            component -> getDependencies(component).isEmpty() && getDependents(component).isEmpty())
        .collect(Collectors.toSet());
  }

  /**
   * Analyzes the impact of removing a component from the graph.
   *
   * @param component the component to analyze for removal
   * @return the impact analysis result
   */
  public ComponentRemovalImpactAnalysis analyzeRemovalImpact(final Component component) {
    Objects.requireNonNull(component, "Component cannot be null");

    final Set<Component> affectedComponents = getTransitiveDependents(component);
    final Set<Component> orphanedComponents = new HashSet<>();

    // Find components that would become orphaned
    for (final Component dependent : affectedComponents) {
      final Set<Component> depDependencies = getDependencies(dependent);
      if (depDependencies.size() == 1 && depDependencies.contains(component)) {
        orphanedComponents.add(dependent);
      }
    }

    return new ComponentRemovalImpactAnalysis(
        component, affectedComponents, orphanedComponents, !affectedComponents.isEmpty());
  }

  /**
   * Gets metrics about the dependency graph.
   *
   * @return the dependency graph metrics
   */
  public DependencyGraphMetrics getMetrics() {
    return metrics;
  }

  /**
   * Creates a subgraph containing only the specified components and their dependencies.
   *
   * @param components the components to include in the subgraph
   * @return the dependency subgraph
   * @throws WasmException if subgraph creation fails
   */
  public ComponentDependencyGraph createSubgraph(final Set<Component> components)
      throws WasmException {
    Objects.requireNonNull(components, "Components cannot be null");

    final Map<Component, Set<Component>> subgraphDeps = new HashMap<>();

    for (final Component component : components) {
      final Set<Component> componentDeps =
          getDependencies(component).stream()
              .filter(components::contains)
              .collect(Collectors.toSet());
      subgraphDeps.put(component, componentDeps);
    }

    return new ComponentDependencyGraph(subgraphDeps);
  }

  private Set<ComponentDependencyCycle> detectCircularDependencies() {
    final Set<ComponentDependencyCycle> cycles = new HashSet<>();
    final Set<Component> visited = new HashSet<>();
    final Set<Component> recursionStack = new HashSet<>();

    for (final Component component : getAllComponents()) {
      if (!visited.contains(component)) {
        detectCircularDependenciesHelper(
            component, visited, recursionStack, cycles, new java.util.ArrayList<>());
      }
    }

    return cycles;
  }

  private void detectCircularDependenciesHelper(
      final Component component,
      final Set<Component> visited,
      final Set<Component> recursionStack,
      final Set<ComponentDependencyCycle> cycles,
      final List<Component> currentPath) {

    visited.add(component);
    recursionStack.add(component);
    currentPath.add(component);

    for (final Component dependency : getDependencies(component)) {
      if (!visited.contains(dependency)) {
        detectCircularDependenciesHelper(dependency, visited, recursionStack, cycles, currentPath);
      } else if (recursionStack.contains(dependency)) {
        // Found a cycle
        final int cycleStart = currentPath.indexOf(dependency);
        final List<Component> cycle =
            new java.util.ArrayList<>(currentPath.subList(cycleStart, currentPath.size()));
        cycles.add(new ComponentDependencyCycle(cycle));
      }
    }

    recursionStack.remove(component);
    currentPath.remove(currentPath.size() - 1);
  }

  private List<Component> performTopologicalSort() {
    final Map<Component, Integer> inDegree = new HashMap<>();
    final java.util.Queue<Component> queue = new java.util.LinkedList<>();
    final List<Component> result = new java.util.ArrayList<>();

    // Calculate in-degrees
    for (final Component component : getAllComponents()) {
      inDegree.put(component, getDependencies(component).size());
    }

    // Add components with no dependencies to queue
    for (final Map.Entry<Component, Integer> entry : inDegree.entrySet()) {
      if (entry.getValue() == 0) {
        queue.offer(entry.getKey());
      }
    }

    // Process queue
    while (!queue.isEmpty()) {
      final Component component = queue.poll();
      result.add(component);

      // Reduce in-degree for dependents
      for (final Component dependent : getDependents(component)) {
        final int newInDegree = inDegree.get(dependent) - 1;
        inDegree.put(dependent, newInDegree);
        if (newInDegree == 0) {
          queue.offer(dependent);
        }
      }
    }

    return result;
  }

  private void collectTransitiveDependencies(
      final Component component,
      final Set<Component> visited,
      final Set<Component> transitiveDeps) {

    if (visited.contains(component)) {
      return;
    }

    visited.add(component);

    for (final Component dependency : getDependencies(component)) {
      transitiveDeps.add(dependency);
      collectTransitiveDependencies(dependency, visited, transitiveDeps);
    }
  }

  private void collectTransitiveDependents(
      final Component component,
      final Set<Component> visited,
      final Set<Component> transitiveDeps) {

    if (visited.contains(component)) {
      return;
    }

    visited.add(component);

    for (final Component dependent : getDependents(component)) {
      transitiveDeps.add(dependent);
      collectTransitiveDependents(dependent, visited, transitiveDeps);
    }
  }

  private List<Component> findDependencyPath(
      final Component from, final Component to, final Set<Component> visited) {

    if (from.equals(to)) {
      return List.of(from);
    }

    if (visited.contains(from)) {
      return Collections.emptyList();
    }

    visited.add(from);

    for (final Component dependency : getDependencies(from)) {
      final List<Component> path = findDependencyPath(dependency, to, new HashSet<>(visited));
      if (!path.isEmpty()) {
        final List<Component> fullPath = new java.util.ArrayList<>();
        fullPath.add(from);
        fullPath.addAll(path);
        return fullPath;
      }
    }

    return Collections.emptyList();
  }

  private DependencyGraphMetrics calculateMetrics() {
    final Set<Component> allComponents = getAllComponents();
    final int totalComponents = allComponents.size();
    final int totalDependencies = dependencies.values().stream().mapToInt(Set::size).sum();

    final double averageDependencies =
        totalComponents > 0 ? (double) totalDependencies / totalComponents : 0.0;

    final int maxDependencies = dependencies.values().stream().mapToInt(Set::size).max().orElse(0);

    final int maxDependents = dependents.values().stream().mapToInt(Set::size).max().orElse(0);

    final int rootComponentCount = getRootComponents().size();
    final int leafComponentCount = getLeafComponents().size();
    final int isolatedComponentCount = getIsolatedComponents().size();
    final int circularDependencyCount = circularDependencies.size();

    return new DependencyGraphMetrics(
        totalComponents,
        totalDependencies,
        averageDependencies,
        maxDependencies,
        maxDependents,
        rootComponentCount,
        leafComponentCount,
        isolatedComponentCount,
        circularDependencyCount,
        isValid);
  }
}
