package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitSupportInfo;
import java.io.Closeable;
import java.util.List;
import java.util.Optional;

/**
 * WebAssembly component compilation engine interface.
 *
 * <p>A ComponentEngine provides component-specific compilation and management capabilities. It
 * supports the WebAssembly Component Model, allowing composition and linking of components.
 *
 * <p>ComponentEngines use composition rather than inheritance with the base Engine interface.
 * Wasmtime has a single engine type that handles both modules and components. The ComponentEngine
 * wraps a regular Engine with additional component model functionality. Access the underlying
 * Engine via {@link #getEngine()}.
 *
 * <p>ComponentEngines are thread-safe and can be shared across multiple threads for concurrent
 * component compilation and instantiation.
 *
 * @since 1.0.0
 */
public interface ComponentEngine extends Closeable {

  /**
   * Gets the underlying Wasmtime engine.
   *
   * <p>The returned Engine can be used for core module operations, store creation, and other
   * non-component engine functionality.
   *
   * @return the underlying engine
   */
  Engine getEngine();

  /**
   * Compiles WebAssembly component bytecode into a component using this engine.
   *
   * <p>This method validates and compiles the provided WebAssembly component bytecode, including
   * component model validation and interface checking.
   *
   * @param componentBytes the WebAssembly component bytecode to compile
   * @return a compiled Component
   * @throws WasmException if compilation fails due to invalid bytecode or engine issues
   * @throws IllegalArgumentException if componentBytes is null
   */
  Component compileComponent(byte[] componentBytes) throws WasmException;

  /**
   * Compiles WebAssembly component bytecode with a specific name.
   *
   * @param componentBytes the WebAssembly component bytecode to compile
   * @param name the name to assign to the component
   * @return a compiled Component
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if componentBytes is null or name is null/empty
   */
  Component compileComponent(byte[] componentBytes, String name) throws WasmException;

  /**
   * Validates component compatibility.
   *
   * <p>This method checks if two components can work together by examining their interfaces,
   * imports, and exports.
   *
   * @param source the source component
   * @param target the target component
   * @return a compatibility result with detailed information
   * @throws IllegalArgumentException if source or target is null
   */
  WitCompatibilityResult checkCompatibility(Component source, Component target);

  /**
   * Creates a component instance from a compiled component.
   *
   * <p>This method creates an executable instance of a component that can be used to invoke
   * component functions and access component state.
   *
   * @param component the compiled component
   * @param store the store to create the instance in
   * @return a component instance
   * @throws WasmException if instance creation fails
   * @throws IllegalArgumentException if component or store is null
   */
  ComponentInstance createInstance(Component component, Store store) throws WasmException;

  /**
   * Creates a component instance with import linking.
   *
   * @param component the compiled component
   * @param store the store to create the instance in
   * @param imports the components to link as imports
   * @return a component instance
   * @throws WasmException if instance creation or linking fails
   * @throws IllegalArgumentException if any parameter is null
   */
  ComponentInstance createInstance(Component component, Store store, List<Component> imports)
      throws WasmException;

  /**
   * Gets information about WIT (WebAssembly Interface Types) support.
   *
   * @return WIT support information
   */
  WitSupportInfo getWitSupportInfo();

  /**
   * Checks if this engine supports component model features.
   *
   * @return true if component model is supported, false otherwise
   */
  boolean supportsComponentModel();

  /**
   * Gets the maximum number of components that can be linked together.
   *
   * @return the maximum link depth, or empty if unlimited
   */
  Optional<Integer> getMaxLinkDepth();

  /**
   * Checks if two engines share the same underlying Wasmtime engine.
   *
   * @param other the other engine to compare with
   * @return true if both share the same underlying engine
   */
  boolean same(Engine other);

  /**
   * Checks if async support is enabled.
   *
   * @return true if async support is enabled
   */
  boolean isAsync();

  /**
   * Checks if this engine is still valid (not closed).
   *
   * @return true if the engine is valid
   */
  boolean isValid();

  /**
   * Deserializes a component from previously serialized bytes.
   *
   * <p>This method can be used to quickly load a previously compiled component without going
   * through the compilation process again. The bytes must have been created by a compatible version
   * of the same engine.
   *
   * @param bytes the serialized component data
   * @return the deserialized Component
   * @throws WasmException if deserialization fails or data is invalid
   * @throws IllegalArgumentException if bytes is null or empty
   * @since 1.0.0
   */
  Component deserializeComponent(byte[] bytes) throws WasmException;

  /**
   * Deserializes a component from a previously serialized file.
   *
   * <p>This is more efficient than reading the file into memory and then calling {@link
   * #deserializeComponent(byte[])} because it uses memory-mapped I/O.
   *
   * @param path the path to the serialized component file
   * @return the deserialized Component
   * @throws WasmException if deserialization fails or the file is invalid
   * @throws IllegalArgumentException if path is null or empty
   * @since 1.0.0
   */
  Component deserializeComponentFile(String path) throws WasmException;

  /**
   * Detects if bytes contain a precompiled module or component.
   *
   * @param bytes the bytes to check
   * @return the precompiled type, or null if not precompiled
   */
  ai.tegmentum.wasmtime4j.Precompiled detectPrecompiled(byte[] bytes);
}
