package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Interface for WebAssembly Component Model components.
 *
 * <p>A Component represents a WebAssembly component that has been compiled and validated.
 * Components in the Component Model provide structured interfaces, typed imports/exports, and
 * support for composition and linking.
 *
 * <p>Components differ from core WebAssembly modules by providing higher-level abstractions,
 * structured data types, and interface-based composition capabilities following the WebAssembly
 * Component Model specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (Component component = engine.compileComponent(componentBytes)) {
 *     ComponentLinker linker = ComponentLinker.create(engine);
 *     ComponentInstance instance = component.instantiate(store, linker);
 *     // Use component instance
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface Component extends AutoCloseable {

  /**
   * Instantiates the component with the given store and linker.
   *
   * <p>Creates a new instance of this component using the provided store for execution state and
   * linker for import resolution. The instance represents a running component with its own isolated
   * state and exports.
   *
   * @param store the store to use for execution state
   * @param linker the linker to use for import resolution
   * @return a new ComponentInstance of this component
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store or linker is null
   */
  ComponentInstance instantiate(final Store store, final ComponentLinker linker)
      throws WasmException;

  /**
   * Gets the type information for this component.
   *
   * <p>Returns comprehensive type information including imports, exports, and interface
   * definitions. This can be used for validation, tooling, and dynamic interaction.
   *
   * @return the component type information
   * @throws WasmException if type information cannot be retrieved
   */
  ComponentType getType() throws WasmException;

  /**
   * Serializes the component to its binary representation.
   *
   * <p>Returns the component in its WebAssembly component binary format. This can be used for
   * caching, distribution, or persistence of compiled components.
   *
   * @return the component as a byte array in component binary format
   * @throws WasmException if serialization fails
   */
  byte[] serialize() throws WasmException;

  /**
   * Validates the component for correctness and completeness.
   *
   * <p>Performs comprehensive validation including type checking, import/export compatibility, and
   * internal consistency. This can be used to verify component validity before instantiation.
   *
   * @throws WasmException if validation fails with details about specific issues
   */
  void validate() throws WasmException;

  /**
   * Gets metadata about this component.
   *
   * <p>Returns information about component size, complexity, performance characteristics, and other
   * metadata useful for tooling and optimization.
   *
   * @return component metadata
   */
  ComponentMetadata getMetadata();

  /**
   * Checks if this component is still valid and usable.
   *
   * <p>Components become invalid when closed or when their underlying resources are freed.
   *
   * @return true if the component is valid and usable, false otherwise
   */
  boolean isValid();

  /**
   * Closes the component and releases associated resources.
   *
   * <p>After calling this method, the component becomes invalid and should not be used. Any
   * instances created from this component may also be affected depending on the runtime
   * implementation.
   */
  @Override
  void close();
}
