package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.List;

/**
 * Interface for WASI component operations.
 *
 * <p>A WasiComponent represents a WebAssembly component that has been loaded and is ready for
 * instantiation and execution with WASI capabilities. Components in the WASI Preview 2 model
 * support structured interfaces, imports, and exports.
 *
 * <p>Components differ from traditional WebAssembly modules by providing higher-level abstractions
 * and structured interfaces for complex data types and resource management.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiComponent component = context.createComponent(wasmBytes)) {
 *     WasiInstance instance = component.instantiate(config);
 *     Map<String, Object> result = instance.call("exported-function", args);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiComponent extends Closeable {

  /**
   * Gets the name of the component if available.
   *
   * @return the component name, or null if not specified
   */
  String getName();

  /**
   * Gets the component's interface exports.
   *
   * <p>Returns a list of interface names that this component exports. These interfaces can be used
   * to interact with the component after instantiation.
   *
   * @return list of exported interface names
   * @throws WasmException if the component is invalid or exports cannot be retrieved
   */
  List<String> getExports() throws WasmException;

  /**
   * Gets the component's interface imports.
   *
   * <p>Returns a list of interface names that this component requires to be satisfied during
   * instantiation.
   *
   * @return list of required import interface names
   * @throws WasmException if the component is invalid or imports cannot be retrieved
   */
  List<String> getImports() throws WasmException;

  /**
   * Gets metadata about an exported interface.
   *
   * <p>Returns detailed information about the specified exported interface, including function
   * signatures, resource types, and parameter specifications.
   *
   * @param interfaceName the name of the interface to inspect
   * @return metadata for the specified interface
   * @throws WasmException if the component is invalid or interface doesn't exist
   * @throws IllegalArgumentException if interfaceName is null or empty
   */
  WasiInterfaceMetadata getExportMetadata(final String interfaceName) throws WasmException;

  /**
   * Gets metadata about a required import interface.
   *
   * <p>Returns detailed information about the specified import interface, including function
   * signatures, resource types, and parameter specifications.
   *
   * @param interfaceName the name of the interface to inspect
   * @return metadata for the specified interface
   * @throws WasmException if the component is invalid or interface doesn't exist
   * @throws IllegalArgumentException if interfaceName is null or empty
   */
  WasiInterfaceMetadata getImportMetadata(final String interfaceName) throws WasmException;

  /**
   * Instantiates the component with default configuration.
   *
   * <p>Creates a new instance of this component using default WASI configuration. The instance
   * represents a running component with its own isolated state and resources.
   *
   * @return a new WasiInstance of this component
   * @throws WasmException if instantiation fails
   */
  WasiInstance instantiate() throws WasmException;

  /**
   * Instantiates the component with custom configuration.
   *
   * <p>Creates a new instance of this component using the provided configuration. This allows
   * customization of imports, resource limits, and other component settings.
   *
   * @param config the configuration for instantiation
   * @return a new WasiInstance of this component with the specified configuration
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if config is null
   */
  WasiInstance instantiate(final WasiConfig config) throws WasmException;

  /**
   * Validates the component without instantiating it.
   *
   * <p>Performs comprehensive validation of the component including interface compatibility,
   * resource constraints, and security requirements. This can be used to check component validity
   * before expensive instantiation.
   *
   * @throws WasmException if validation fails with details about the specific issues
   */
  void validate() throws WasmException;

  /**
   * Validates the component with specific import requirements.
   *
   * <p>Validates that the component can be instantiated with the specified imports and
   * configuration.
   *
   * @param config the configuration to validate against
   * @throws WasmException if validation fails
   * @throws IllegalArgumentException if config is null
   */
  void validate(final WasiConfig config) throws WasmException;

  /**
   * Gets component statistics and metrics.
   *
   * <p>Returns information about component size, complexity, and resource requirements. This can be
   * useful for monitoring and optimization.
   *
   * @return component statistics
   */
  WasiComponentStats getStats();

  /**
   * Checks if the component is still valid and usable.
   *
   * @return true if the component is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the component and releases associated resources.
   *
   * <p>After calling this method, the component becomes invalid and should not be used. Any
   * instances created from this component may also be affected.
   */
  @Override
  void close();
}
