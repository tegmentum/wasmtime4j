package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model component linking.
 *
 * <p>A ComponentLinker provides import resolution and linking capabilities for component
 * instantiation. It manages the mapping between component imports and their implementations,
 * enabling component composition and dependency injection.
 *
 * <p>The linker supports linking components to other components, host-provided implementations,
 * and interface-based composition patterns defined by the Component Model specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentLinker linker = ComponentLinker.create(engine);
 *
 * // Define a component import
 * linker.defineComponent("dependency", dependencyComponent);
 *
 * // Define a host interface implementation
 * linker.defineInterface("host-interface", hostInterface);
 *
 * // Instantiate with resolved imports
 * ComponentInstance instance = linker.instantiate(store, component);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentLinker extends Closeable {

  /**
   * Creates a new ComponentLinker for the given engine.
   *
   * <p>Creates a linker instance that can be used to resolve imports for components compiled with
   * the specified engine.
   *
   * @param engine the engine to create the linker for
   * @return a new ComponentLinker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   */
  static ComponentLinker create(final Engine engine) throws WasmException {
    throw new UnsupportedOperationException("Component linker creation not yet implemented");
  }

  /**
   * Defines a component import resolution.
   *
   * <p>Maps the specified import name to a component implementation. When a component being
   * instantiated imports the named component, it will be resolved to the provided component.
   *
   * @param name the import name to resolve
   * @param component the component to use for resolution
   * @throws WasmException if the definition fails
   * @throws IllegalArgumentException if name is null/empty or component is null
   */
  void defineComponent(final String name, final Component component) throws WasmException;

  /**
   * Defines an interface import resolution.
   *
   * <p>Maps the specified interface name to an interface implementation. This can be used to
   * provide host-implemented interfaces or bridge to other component interfaces.
   *
   * @param name the interface name to resolve
   * @param interfaceType the interface implementation to use for resolution
   * @throws WasmException if the definition fails
   * @throws IllegalArgumentException if name is null/empty or interfaceType is null
   */
  void defineInterface(final String name, final InterfaceType interfaceType) throws WasmException;

  /**
   * Defines a function import resolution.
   *
   * <p>Maps the specified function name to a function implementation. This allows providing
   * host-implemented functions that can be called by components.
   *
   * @param name the function name to resolve
   * @param function the function implementation
   * @throws WasmException if the definition fails
   * @throws IllegalArgumentException if name is null/empty or function is null
   */
  void defineFunction(final String name, final ComponentFunction function) throws WasmException;

  /**
   * Defines a resource import resolution.
   *
   * <p>Maps the specified resource name to a resource implementation. This enables sharing
   * resources between components and providing host-managed resources.
   *
   * @param name the resource name to resolve
   * @param resource the resource implementation
   * @throws WasmException if the definition fails
   * @throws IllegalArgumentException if name is null/empty or resource is null
   */
  void defineResource(final String name, final ComponentResource resource) throws WasmException;

  /**
   * Instantiates a component with this linker's import resolutions.
   *
   * <p>Creates a new instance of the specified component, resolving all imports using the
   * definitions provided to this linker. All required imports must be satisfied for
   * instantiation to succeed.
   *
   * @param store the store to use for execution state
   * @param component the component to instantiate
   * @return a new ComponentInstance with resolved imports
   * @throws WasmException if instantiation fails or imports cannot be resolved
   * @throws IllegalArgumentException if store or component is null
   */
  ComponentInstance instantiate(final Store store, final Component component) throws WasmException;

  /**
   * Gets the names of all defined imports.
   *
   * <p>Returns a list of all import names that have been defined in this linker. This can be
   * used for discovery and validation of available imports.
   *
   * @return list of defined import names
   */
  List<String> getDefinedImports();

  /**
   * Checks if a specific import is defined.
   *
   * <p>Returns true if the linker has a definition for the specified import name.
   *
   * @param name the import name to check
   * @return true if the import is defined, false otherwise
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean hasImport(final String name);

  /**
   * Gets the type of a defined import.
   *
   * <p>Returns type information for the specified import, or empty if the import is not defined.
   *
   * @param name the import name to get type information for
   * @return type information for the import, or empty if not defined
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentImportType> getImportType(final String name);

  /**
   * Validates that all imports for a component can be resolved.
   *
   * <p>Checks that all imports required by the specified component have been defined in this
   * linker and that their types are compatible.
   *
   * @param component the component to validate imports for
   * @throws WasmException if validation fails with details about missing or incompatible imports
   * @throws IllegalArgumentException if component is null
   */
  void validateImports(final Component component) throws WasmException;

  /**
   * Gets metadata about this linker's configuration.
   *
   * <p>Returns information about defined imports, linking performance, and other metadata useful
   * for debugging and optimization.
   *
   * @return linker metadata
   */
  ComponentLinkerMetadata getMetadata();

  /**
   * Creates a copy of this linker with the same import definitions.
   *
   * <p>Returns a new linker instance with all the same import definitions as this linker. This
   * can be useful for creating variations or avoiding shared state.
   *
   * @return a new ComponentLinker with the same definitions
   * @throws WasmException if cloning fails
   */
  ComponentLinker clone() throws WasmException;

  /**
   * Checks if this linker is still valid and usable.
   *
   * <p>Linkers become invalid when closed or when their associated engine is destroyed.
   *
   * @return true if the linker is valid and usable, false otherwise
   */
  boolean isValid();

  /**
   * Closes the linker and releases associated resources.
   *
   * <p>After calling this method, the linker becomes invalid and should not be used. Any
   * component instances created using this linker will not be affected.
   */
  @Override
  void close();
}