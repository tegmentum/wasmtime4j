package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
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
 * using thread-safe collections and efficient lookup mechanisms.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe component registration and discovery
 *   <li>Advanced dependency resolution with cycle detection
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
  private final ConcurrentMap<String, ComponentSimple> componentsById;
  private final ConcurrentMap<String, ComponentSimple> componentsByName;
  private final ConcurrentMap<ComponentVersion, Set<ComponentSimple>> componentsByVersion;
  private final AtomicLong registrationCounter;

  /**
   * Creates a new JNI component registry.
   *
   * @param engine the component engine this registry belongs to
   */
  public JniComponentRegistry(final JniComponentEngine engine) {
    JniValidation.requireNonNull(engine, "engine");
    this.engine = engine;
    this.componentsById = new ConcurrentHashMap<>();
    this.componentsByName = new ConcurrentHashMap<>();
    this.componentsByVersion = new ConcurrentHashMap<>();
    this.registrationCounter = new AtomicLong(0);
    LOGGER.fine("Created JNI component registry for engine: " + engine.getId());
  }

  @Override
  public void register(final ComponentSimple component) throws WasmException {
    JniValidation.requireNonNull(component, "component");

    final String componentId = component.getId();
    if (componentId == null || componentId.trim().isEmpty()) {
      throw new WasmException("Component ID cannot be null or empty");
    }

    // Check if component is already registered
    if (componentsById.containsKey(componentId)) {
      throw new WasmException("Component with ID '" + componentId + "' is already registered");
    }

    // Validate component before registration
    final ComponentValidationResult validation = engine.validateComponent(component);
    if (!validation.isValid()) {
      throw new WasmException(
          "Component validation failed: " + String.join(", ", validation.getIssues()));
    }

    try {
      // Register component
      componentsById.put(componentId, component);

      // Register by version
      final ComponentVersion version = component.getVersion();
      componentsByVersion
          .computeIfAbsent(version, k -> Collections.synchronizedSet(new HashSet<>()))
          .add(component);

      registrationCounter.incrementAndGet();
      LOGGER.fine("Registered component: " + componentId);

    } catch (final Exception e) {
      // Rollback registration on failure
      componentsById.remove(componentId);
      final ComponentVersion version = component.getVersion();
      final Set<ComponentSimple> versionSet = componentsByVersion.get(version);
      if (versionSet != null) {
        versionSet.remove(component);
        if (versionSet.isEmpty()) {
          componentsByVersion.remove(version);
        }
      }
      throw new WasmException("Failed to register component: " + componentId, e);
    }
  }

  @Override
  public void register(final String name, final ComponentSimple component) throws WasmException {
    JniValidation.requireNonEmpty(name, "name");
    JniValidation.requireNonNull(component, "component");

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
      unregister(component.getId());
      throw new WasmException("Failed to register component by name: " + name, e);
    }
  }

  @Override
  public void unregister(final String componentId) throws WasmException {
    JniValidation.requireNonEmpty(componentId, "componentId");

    final ComponentSimple component = componentsById.get(componentId);
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
      final Set<ComponentSimple> versionSet = componentsByVersion.get(version);
      if (versionSet != null) {
        versionSet.remove(component);
        if (versionSet.isEmpty()) {
          componentsByVersion.remove(version);
        }
      }

      LOGGER.fine("Unregistered component: " + componentId);

    } catch (final Exception e) {
      throw new WasmException("Failed to unregister component: " + componentId, e);
    }
  }

  @Override
  public Optional<ComponentSimple> findById(final String componentId) {
    JniValidation.requireNonEmpty(componentId, "componentId");
    return Optional.ofNullable(componentsById.get(componentId));
  }

  @Override
  public Optional<ComponentSimple> findByName(final String name) {
    JniValidation.requireNonEmpty(name, "name");
    return Optional.ofNullable(componentsByName.get(name));
  }

  @Override
  public List<ComponentSimple> findByVersion(final ComponentVersion version) {
    JniValidation.requireNonNull(version, "version");
    final Set<ComponentSimple> components = componentsByVersion.get(version);
    return components != null ? new ArrayList<>(components) : new ArrayList<>();
  }

  @Override
  public Set<ComponentSimple> getAllComponents() {
    return new HashSet<>(componentsById.values());
  }

  @Override
  public Set<String> getAllComponentIds() {
    return new HashSet<>(componentsById.keySet());
  }

  @Override
  public boolean isRegistered(final String componentId) {
    JniValidation.requireNonEmpty(componentId, "componentId");
    return componentsById.containsKey(componentId);
  }

  @Override
  public int getComponentCount() {
    return componentsById.size();
  }

  @Override
  public Set<ComponentSimple> resolveDependencies(final ComponentSimple component)
      throws WasmException {
    JniValidation.requireNonNull(component, "component");

    try {
      final Set<ComponentSimple> dependencies = new HashSet<>();
      final Set<String> requiredInterfaces = component.getImportedInterfaces();

      // Find components that provide the required interfaces
      for (final ComponentSimple candidate : componentsById.values()) {
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
      throw new WasmException(
          "Failed to resolve dependencies for component: " + component.getId(), e);
    }
  }

  @Override
  public ComponentValidationResult validateDependencies(final ComponentSimple component)
      throws WasmException {
    JniValidation.requireNonNull(component, "component");

    try {
      final List<String> issues = new ArrayList<>();
      final List<String> warnings = new ArrayList<>();

      final Set<String> requiredInterfaces = component.getImportedInterfaces();
      final Set<ComponentSimple> dependencies = resolveDependencies(component);

      // Check if all required interfaces are satisfied
      final Set<String> providedInterfaces = new HashSet<>();
      for (final ComponentSimple dependency : dependencies) {
        providedInterfaces.addAll(dependency.getExportedInterfaces());
      }

      final Set<String> unsatisfiedInterfaces = new HashSet<>(requiredInterfaces);
      unsatisfiedInterfaces.removeAll(providedInterfaces);

      if (!unsatisfiedInterfaces.isEmpty()) {
        issues.add(
            "Unsatisfied interface dependencies: " + String.join(", ", unsatisfiedInterfaces));
      }

      // Check for version compatibility
      for (final ComponentSimple dependency : dependencies) {
        final var compatibility = component.checkCompatibility(dependency);
        if (!compatibility.isCompatible()) {
          warnings.add(
              "Version compatibility issue with dependency "
                  + dependency.getId()
                  + ": "
                  + compatibility.getMessage());
        }
      }

      final boolean valid = issues.isEmpty();
      return new ComponentValidationResult(valid, issues, warnings);

    } catch (final Exception e) {
      throw new WasmException(
          "Failed to validate dependencies for component: " + component.getId(), e);
    }
  }

  @Override
  public List<ComponentSimple> search(final ComponentSearchCriteria criteria) throws WasmException {
    JniValidation.requireNonNull(criteria, "criteria");

    try {
      return componentsById.values().stream()
          .filter(component -> matchesCriteria(component, criteria))
          .collect(Collectors.toList());

    } catch (final Exception e) {
      throw new WasmException("Component search failed", e);
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
      throw new WasmException("Failed to clear component registry", e);
    }
  }

  @Override
  public ComponentRegistryStatistics getStatistics() {
    final int totalComponents = componentsById.size();
    final int namedComponents = componentsByName.size();
    final int versionGroups = componentsByVersion.size();
    final long totalRegistrations = registrationCounter.get();

    return new ComponentRegistryStatistics(
        totalComponents, namedComponents, versionGroups, totalRegistrations);
  }

  /**
   * Checks if adding dependencies would create a circular dependency.
   *
   * @param component the component to check
   * @param dependencies the proposed dependencies
   * @return true if a circular dependency would be created
   */
  private boolean hasCircularDependency(
      final ComponentSimple component, final Set<ComponentSimple> dependencies) {
    try {
      // For each dependency, check if it depends on the component (directly or indirectly)
      for (final ComponentSimple dependency : dependencies) {
        if (dependsOn(dependency, component, new HashSet<>())) {
          return true;
        }
      }
      return false;
    } catch (final Exception e) {
      LOGGER.warning("Error checking for circular dependencies: " + e.getMessage());
      return true; // Assume circular dependency on error for safety
    }
  }

  /**
   * Checks if a component depends on another component (directly or indirectly).
   *
   * @param source the source component
   * @param target the target component to check dependency on
   * @param visited set of already visited components to avoid infinite loops
   * @return true if source depends on target
   */
  private boolean dependsOn(
      final ComponentSimple source, final ComponentSimple target, final Set<String> visited) {
    try {
      if (source.equals(target)) {
        return true;
      }

      final String sourceId = source.getId();
      if (visited.contains(sourceId)) {
        return false; // Already checked this path
      }

      visited.add(sourceId);

      // Get source's dependencies and check if any depend on target
      final Set<ComponentSimple> sourceDeps = resolveDependencies(source);
      for (final ComponentSimple dep : sourceDeps) {
        if (dependsOn(dep, target, visited)) {
          return true;
        }
      }

      return false;
    } catch (final Exception e) {
      LOGGER.warning("Error checking dependency relationship: " + e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a component matches the given search criteria.
   *
   * @param component the component to check
   * @param criteria the search criteria
   * @return true if the component matches the criteria
   */
  private boolean matchesCriteria(
      final ComponentSimple component, final ComponentSearchCriteria criteria) {
    try {
      // Check name pattern
      if (criteria.getNamePattern() != null) {
        final String componentName = getComponentName(component);
        if (componentName == null || !componentName.matches(criteria.getNamePattern())) {
          return false;
        }
      }

      // Check version range
      if (criteria.getMinVersion() != null || criteria.getMaxVersion() != null) {
        final ComponentVersion version = component.getVersion();
        if (criteria.getMinVersion() != null && version.compareTo(criteria.getMinVersion()) < 0) {
          return false;
        }
        if (criteria.getMaxVersion() != null && version.compareTo(criteria.getMaxVersion()) > 0) {
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
  private String getComponentName(final ComponentSimple component) {
    return componentsByName.entrySet().stream()
        .filter(entry -> entry.getValue().equals(component))
        .map(java.util.Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }
}
