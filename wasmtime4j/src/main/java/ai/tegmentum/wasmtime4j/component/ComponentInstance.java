package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model component instances.
 *
 * <p>A ComponentInstance represents a running instance of a component with its own execution state
 * and exported interfaces. Component instances provide access to exported functions, resources,
 * and interfaces defined by the component.
 *
 * <p>Component instances maintain isolation between different instantiations of the same component
 * and provide structured access to component exports through the Component Model interface system.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentInstance instance = component.instantiate(store, linker);
 * ComponentExport export = instance.getExport("my-interface");
 * ComponentFunction func = export.getFunction("my-function");
 * ComponentValue result = func.call(args);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentInstance extends Closeable {

  /**
   * Gets a specific export by name.
   *
   * <p>Returns the exported interface, function, or resource with the specified name. Exports can
   * be functions, interfaces, resources, or other component-defined entities.
   *
   * @param name the name of the export to retrieve
   * @return the component export, or empty if not found
   * @throws WasmException if the instance is invalid or export retrieval fails
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentExport> getExport(final String name) throws WasmException;

  /**
   * Gets all exports from this component instance.
   *
   * <p>Returns a map of all exported interfaces, functions, and resources provided by this
   * component instance. The keys are export names and values are the corresponding exports.
   *
   * @return map of export names to ComponentExport objects
   * @throws WasmException if the instance is invalid or exports cannot be retrieved
   */
  Map<String, ComponentExport> getExports() throws WasmException;

  /**
   * Gets the names of all exports.
   *
   * <p>Returns a list of all export names available from this component instance. This can be
   * used for discovery and enumeration of available component functionality.
   *
   * @return list of export names
   * @throws WasmException if the instance is invalid
   */
  java.util.List<String> getExportNames() throws WasmException;

  /**
   * Checks if a specific export exists.
   *
   * <p>Returns true if the component instance exports an entity with the specified name.
   *
   * @param name the name to check for
   * @return true if the export exists, false otherwise
   * @throws WasmException if the instance is invalid
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean hasExport(final String name) throws WasmException;

  /**
   * Gets type information for a specific export.
   *
   * <p>Returns detailed type information for the specified export, including function signatures,
   * resource types, or interface definitions.
   *
   * @param name the name of the export
   * @return type information for the export, or empty if export doesn't exist
   * @throws WasmException if the instance is invalid or type retrieval fails
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentExportType> getExportType(final String name) throws WasmException;

  /**
   * Gets the component that created this instance.
   *
   * <p>Returns the original component from which this instance was created. This can be used to
   * access component-level information and metadata.
   *
   * @return the parent component
   */
  Component getComponent();

  /**
   * Gets statistics and metrics for this component instance.
   *
   * <p>Returns information about execution time, memory usage, resource consumption, and other
   * runtime metrics useful for monitoring and optimization.
   *
   * @return component instance statistics
   */
  ComponentInstanceStats getStats();

  /**
   * Checks if this component instance is still valid and usable.
   *
   * <p>Component instances become invalid when closed, when their parent component is closed, or
   * when their execution store is destroyed.
   *
   * @return true if the instance is valid and usable, false otherwise
   */
  boolean isValid();

  /**
   * Closes the component instance and releases associated resources.
   *
   * <p>After calling this method, the instance becomes invalid and should not be used. This will
   * clean up any component-specific state and resources but will not affect the parent component.
   */
  @Override
  void close();
}