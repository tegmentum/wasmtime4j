package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for managing WebAssembly components and their dependencies.
 *
 * <p>The component registry provides centralized management of components, including registration,
 * discovery, dependency resolution, and lifecycle management.
 *
 * @since 1.0.0
 */
public interface ComponentRegistry {

  /**
   * Registers a component with this registry.
   *
   * @param component the component to register
   * @throws WasmException if registration fails
   * @throws IllegalArgumentException if component is null
   */
  void register(ComponentSimple component) throws WasmException;

  /**
   * Registers a component with a specific name.
   *
   * @param name the name to register the component under
   * @param component the component to register
   * @throws WasmException if registration fails
   * @throws IllegalArgumentException if name or component is null
   */
  void register(String name, ComponentSimple component) throws WasmException;

  /**
   * Unregisters a component from this registry.
   *
   * @param componentId the ID of the component to unregister
   * @throws WasmException if unregistration fails
   */
  void unregister(String componentId) throws WasmException;

  /**
   * Finds a component by its ID.
   *
   * @param componentId the component ID to search for
   * @return the component if found, empty otherwise
   */
  Optional<ComponentSimple> findById(String componentId);

  /**
   * Finds a component by name.
   *
   * @param name the component name to search for
   * @return the component if found, empty otherwise
   */
  Optional<ComponentSimple> findByName(String name);

  /**
   * Finds components by version.
   *
   * @param version the component version to search for
   * @return list of components with the specified version
   */
  List<ComponentSimple> findByVersion(ComponentVersion version);

  /**
   * Gets all registered components.
   *
   * @return set of all registered components
   */
  Set<ComponentSimple> getAllComponents();

  /**
   * Gets all component IDs.
   *
   * @return set of all component IDs
   */
  Set<String> getAllComponentIds();

  /**
   * Checks if a component is registered.
   *
   * @param componentId the component ID to check
   * @return true if the component is registered
   */
  boolean isRegistered(String componentId);

  /**
   * Gets the number of registered components.
   *
   * @return the number of registered components
   */
  int getComponentCount();

  /**
   * Resolves dependencies for a component.
   *
   * @param component the component to resolve dependencies for
   * @return set of resolved dependency components
   * @throws WasmException if dependency resolution fails
   */
  Set<ComponentSimple> resolveDependencies(ComponentSimple component) throws WasmException;

  /**
   * Validates that all component dependencies are satisfied.
   *
   * @param component the component to validate
   * @return validation result
   * @throws WasmException if validation fails
   */
  ComponentValidationResult validateDependencies(ComponentSimple component) throws WasmException;

  /**
   * Searches for components matching the given criteria.
   *
   * @param criteria the search criteria
   * @return list of matching components
   * @throws WasmException if search fails
   */
  List<ComponentSimple> search(ComponentSearchCriteria criteria) throws WasmException;

  /**
   * Clears all registered components.
   *
   * @throws WasmException if clearing fails
   */
  void clear() throws WasmException;

  /**
   * Gets registry statistics.
   *
   * @return registry statistics
   */
  ComponentRegistryStatistics getStatistics();
}
