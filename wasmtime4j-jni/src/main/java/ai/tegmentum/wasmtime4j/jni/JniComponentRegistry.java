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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentRegistry;
import ai.tegmentum.wasmtime4j.component.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * JNI implementation of the ComponentRegistry interface.
 *
 * <p>This class provides component registration, discovery, and dependency management functionality
 * using thread-safe collections for concurrent access.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe component registration and discovery
 *   <li>Dependency resolution with cycle detection
 *   <li>Component search and filtering capabilities
 *   <li>Version compatibility checking
 *   <li>Registry statistics and monitoring
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniComponentRegistry implements ComponentRegistry {

  private static final Logger LOGGER = Logger.getLogger(JniComponentRegistry.class.getName());

  private final JniComponentEngine engine;
  private final ConcurrentMap<String, Component> componentsById;
  private final ConcurrentMap<String, Component> componentsByName;
  private final ConcurrentMap<ComponentVersion, Set<Component>> componentsByVersion;
  private final AtomicLong registrationCounter;

  /**
   * Creates a new JNI component registry.
   *
   * @param engine the component engine this registry belongs to
   */
  public JniComponentRegistry(final JniComponentEngine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    this.engine = engine;
    this.componentsById = new ConcurrentHashMap<>();
    this.componentsByName = new ConcurrentHashMap<>();
    this.componentsByVersion = new ConcurrentHashMap<>();
    this.registrationCounter = new AtomicLong(0);
    LOGGER.fine("Created JNI component registry");
  }

  @Override
  public void register(final Component component) throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }

    final String componentId = component.getId();
    if (componentId == null || componentId.trim().isEmpty()) {
      throw new WasmException("Component ID cannot be null or empty");
    }

    // Check if component is already registered
    if (componentsById.containsKey(componentId)) {
      throw new WasmException("Component with ID '" + componentId + "' is already registered");
    }

    try {
      // Validate component before registration
      final ComponentValidationResult validation = engine.validateComponent(component);
      if (!validation.isValid()) {
        final String errorMessages =
            validation.getErrors().stream()
                .map(e -> e.getMessage())
                .collect(Collectors.joining(", "));
        throw new WasmException("Component validation failed: " + errorMessages);
      }

      // Register component
      componentsById.put(componentId, component);

      // Register by version
      final ComponentVersion version = component.getVersion();
      componentsByVersion
          .computeIfAbsent(version, k -> Collections.synchronizedSet(new HashSet<>()))
          .add(component);

      registrationCounter.incrementAndGet();
      LOGGER.fine("Registered component: " + componentId);

    } catch (final WasmException e) {
      // Rollback registration on failure
      rollbackRegistration(component);
      throw e;
    } catch (final Exception e) {
      // Rollback registration on failure
      rollbackRegistration(component);
      throw new WasmException("Failed to register component: " + e.getMessage(), e);
    }
  }

  @Override
  public void register(final String name, final Component component) throws WasmException {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }

    // Check if name is already taken
    if (componentsByName.containsKey(name)) {
      throw new WasmException("Component with name '" + name + "' is already registered");
    }

    try {
      // Register component by ID first
      register(component);

      // Then register by name
      componentsByName.put(name, component);
      LOGGER.fine("Registered component by name: " + name + " -> " + component.getId());

    } catch (final WasmException e) {
      // If ID registration failed, don't register by name
      throw e;
    } catch (final Exception e) {
      // If name registration failed, unregister by ID
      try {
        unregister(component.getId());
      } catch (final WasmException ignored) {
        // Ignore errors during rollback
      }
      throw new WasmException("Failed to register component by name: " + e.getMessage(), e);
    }
  }

  @Override
  public void unregister(final String componentId) throws WasmException {
    if (componentId == null || componentId.trim().isEmpty()) {
      throw new IllegalArgumentException("Component ID cannot be null or empty");
    }

    final Component component = componentsById.get(componentId);
    if (component == null) {
      throw new WasmException("Component with ID '" + componentId + "' is not registered");
    }

    try {
      // Remove from all indexes
      componentsById.remove(componentId);

      // Remove from name index
      componentsByName.entrySet().removeIf(entry -> entry.getValue().equals(component));

      // Remove from version index
      final ComponentVersion version = component.getVersion();
      final Set<Component> versionSet = componentsByVersion.get(version);
      if (versionSet != null) {
        versionSet.remove(component);
        if (versionSet.isEmpty()) {
          componentsByVersion.remove(version);
        }
      }

      LOGGER.fine("Unregistered component: " + componentId);

    } catch (final Exception e) {
      throw new WasmException("Failed to unregister component: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<Component> findById(final String componentId) {
    if (componentId == null || componentId.trim().isEmpty()) {
      throw new IllegalArgumentException("Component ID cannot be null or empty");
    }
    return Optional.ofNullable(componentsById.get(componentId));
  }

  @Override
  public Optional<Component> findByName(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    return Optional.ofNullable(componentsByName.get(name));
  }

  @Override
  public List<Component> findByVersion(final ComponentVersion version) {
    if (version == null) {
      throw new IllegalArgumentException("Version cannot be null");
    }
    final Set<Component> components = componentsByVersion.get(version);
    return components != null ? new ArrayList<>(components) : new ArrayList<>();
  }

  @Override
  public Set<Component> getAllComponents() {
    return new HashSet<>(componentsById.values());
  }

  @Override
  public Set<String> getAllComponentIds() {
    return new HashSet<>(componentsById.keySet());
  }

  @Override
  public boolean isRegistered(final String componentId) {
    if (componentId == null || componentId.trim().isEmpty()) {
      throw new IllegalArgumentException("Component ID cannot be null or empty");
    }
    return componentsById.containsKey(componentId);
  }

  @Override
  public int getComponentCount() {
    return componentsById.size();
  }

  @Override
  public Set<Component> resolveDependencies(final Component component)
      throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }

    try {
      final Set<Component> dependencies = new HashSet<>();
      final Set<String> requiredInterfaces = component.getImportedInterfaces();

      // Resolve dependencies by matching imported/exported interfaces
      for (final Component candidate : componentsById.values()) {
        if (candidate.equals(component)) {
          continue; // Skip self
        }

        final Set<String> providedInterfaces = candidate.getExportedInterfaces();
        for (final String requiredInterface : requiredInterfaces) {
          if (providedInterfaces.contains(requiredInterface)) {
            dependencies.add(candidate);
            break; // One match is enough to include this component
          }
        }
      }

      // Check for circular dependencies
      if (hasCircularDependency(component, dependencies)) {
        throw new WasmException("Circular dependency detected for component: " + component.getId());
      }

      LOGGER.fine(
          "Resolved " + dependencies.size() + " dependencies for component: " + component.getId());
      return dependencies;

    } catch (final Exception e) {
      throw new WasmException("Failed to resolve dependencies: " + e.getMessage(), e);
    }
  }

  @Override
  public ComponentValidationResult validateDependencies(final Component component)
      throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }

    try {
      final List<ComponentValidationResult.ValidationError> errors = new ArrayList<>();
      final List<ComponentValidationResult.ValidationWarning> warnings = new ArrayList<>();

      final Set<String> requiredInterfaces = component.getImportedInterfaces();
      final Set<Component> dependencies = resolveDependencies(component);

      // Check if all required interfaces are satisfied
      final Set<String> providedInterfaces = new HashSet<>();
      for (final Component dependency : dependencies) {
        providedInterfaces.addAll(dependency.getExportedInterfaces());
      }

      final Set<String> unsatisfiedInterfaces = new HashSet<>(requiredInterfaces);
      unsatisfiedInterfaces.removeAll(providedInterfaces);

      if (!unsatisfiedInterfaces.isEmpty()) {
        errors.add(
            new ComponentValidationResult.ValidationError(
                "UNSATISFIED_DEPENDENCIES",
                "Unsatisfied interface dependencies: " + String.join(", ", unsatisfiedInterfaces),
                "dependencies",
                ComponentValidationResult.ErrorSeverity.HIGH));
      }

      // Check for version compatibility
      for (final Component dependency : dependencies) {
        final ai.tegmentum.wasmtime4j.component.ComponentCompatibility compatibility =
            component.checkCompatibility(dependency);
        if (!compatibility.isCompatible()) {
          warnings.add(
              new ComponentValidationResult.ValidationWarning(
                  "VERSION_COMPATIBILITY",
                  "Version compatibility issue with dependency "
                      + dependency.getId()
                      + ": "
                      + compatibility.getMessage(),
                  "dependency:" + dependency.getId()));
        }
      }

      final boolean valid = errors.isEmpty();
      final ComponentValidationResult.ValidationContext context =
          new ComponentValidationResult.ValidationContext(
              component.getId(), component.getVersion());
      return new ComponentValidationResult(
          valid, errors, warnings, Collections.emptyList(), context);

    } catch (final Exception e) {
      throw new WasmException("Failed to validate dependencies: " + e.getMessage(), e);
    }
  }

  @Override
  public List<Component> search(final ComponentSearchCriteria criteria) throws WasmException {
    if (criteria == null) {
      throw new IllegalArgumentException("Criteria cannot be null");
    }

    try {
      return componentsById.values().stream()
          .filter(component -> matchesCriteria(component, criteria))
          .collect(Collectors.toList());

    } catch (final Exception e) {
      throw new WasmException("Failed to search components: " + e.getMessage(), e);
    }
  }

  @Override
  public void clear() throws WasmException {
    try {
      componentsById.clear();
      componentsByName.clear();
      componentsByVersion.clear();
      registrationCounter.set(0);
      LOGGER.fine("Cleared component registry");

    } catch (final Exception e) {
      throw new WasmException("Failed to clear registry: " + e.getMessage(), e);
    }
  }

  @Override
  public ComponentRegistryStatistics getStatistics() {
    final int totalComponents = componentsById.size();
    final long totalRegistrations = registrationCounter.get();

    return ComponentRegistryStatistics.builder()
        .totalComponents(totalComponents)
        .totalRegistrations(totalRegistrations)
        .build();
  }

  /**
   * Rolls back component registration on failure.
   *
   * @param component the component to rollback
   */
  private void rollbackRegistration(final Component component) {
    try {
      final String componentId = component.getId();
      componentsById.remove(componentId);

      final ComponentVersion version = component.getVersion();
      final Set<Component> versionSet = componentsByVersion.get(version);
      if (versionSet != null) {
        versionSet.remove(component);
        if (versionSet.isEmpty()) {
          componentsByVersion.remove(version);
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Error during registration rollback: " + e.getMessage());
    }
  }

  /**
   * Checks if a component has circular dependencies.
   *
   * @param component the component to check
   * @param dependencies the resolved dependencies
   * @return true if circular dependency detected
   */
  private boolean hasCircularDependency(
      final Component component, final Set<Component> dependencies) {
    final Set<Component> visited = new HashSet<>();
    final Set<Component> recursionStack = new HashSet<>();

    return hasCircularDependencyDFS(component, dependencies, visited, recursionStack);
  }

  /**
   * DFS-based circular dependency detection.
   *
   * @param current the current component
   * @param allDependencies all available dependencies
   * @param visited visited components
   * @param recursionStack recursion stack for cycle detection
   * @return true if cycle detected
   */
  private boolean hasCircularDependencyDFS(
      final Component current,
      final Set<Component> allDependencies,
      final Set<Component> visited,
      final Set<Component> recursionStack) {

    visited.add(current);
    recursionStack.add(current);

    // Check dependencies of current component
    for (final Component dep : allDependencies) {
      if (recursionStack.contains(dep)) {
        return true; // Cycle detected
      }

      if (!visited.contains(dep)) {
        try {
          final Set<Component> depDependencies = resolveDependencies(dep);
          if (hasCircularDependencyDFS(dep, depDependencies, visited, recursionStack)) {
            return true;
          }
        } catch (final WasmException e) {
          // If we can't resolve dependencies, assume no cycle
          LOGGER.warning("Error resolving dependencies for cycle detection: " + e.getMessage());
        }
      }
    }

    recursionStack.remove(current);
    return false;
  }

  /**
   * Checks if a component matches the given search criteria.
   *
   * @param component the component to check
   * @param criteria the search criteria
   * @return true if the component matches the criteria
   */
  private boolean matchesCriteria(
      final Component component, final ComponentSearchCriteria criteria) {
    try {
      // Check name pattern
      if (criteria.getNamePattern().isPresent()) {
        final String componentName = getComponentName(component);
        if (componentName == null || !componentName.matches(criteria.getNamePattern().get())) {
          return false;
        }
      }

      // Check version range
      if (criteria.getMinVersion().isPresent() || criteria.getMaxVersion().isPresent()) {
        final ComponentVersion version = component.getVersion();
        if (criteria.getMinVersion().isPresent()
            && version.compareTo(criteria.getMinVersion().get()) < 0) {
          return false;
        }
        if (criteria.getMaxVersion().isPresent()
            && version.compareTo(criteria.getMaxVersion().get()) > 0) {
          return false;
        }
      }

      // Check required interfaces
      if (criteria.getRequiredInterfaces() != null && !criteria.getRequiredInterfaces().isEmpty()) {
        final Set<String> exportedInterfaces = component.getExportedInterfaces();
        if (!exportedInterfaces.containsAll(criteria.getRequiredInterfaces())) {
          return false;
        }
      }

      return true;
    } catch (final Exception e) {
      LOGGER.warning("Error matching component criteria: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets the name of a component if it was registered by name.
   *
   * @param component the component
   * @return the component name, or null if not registered by name
   */
  private String getComponentName(final Component component) {
    return componentsByName.entrySet().stream()
        .filter(entry -> entry.getValue().equals(component))
        .map(java.util.Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }
}
