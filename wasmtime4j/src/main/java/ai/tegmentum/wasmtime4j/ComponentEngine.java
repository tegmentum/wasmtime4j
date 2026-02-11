package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitSupportInfo;
import java.util.List;
import java.util.Optional;

/**
 * WebAssembly component compilation engine interface.
 *
 * <p>A ComponentEngine extends the base Engine interface to provide component-specific compilation
 * and management capabilities. It supports the WebAssembly Component Model, allowing composition
 * and linking of components.
 *
 * <p>ComponentEngines are thread-safe and can be shared across multiple threads for concurrent
 * component compilation and instantiation.
 *
 * @since 1.0.0
 */
public interface ComponentEngine extends Engine {

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
   * Links multiple components together.
   *
   * <p>This method validates that the components can be linked together by checking their
   * import/export compatibility and creates a linked component instance.
   *
   * @param components the list of components to link
   * @return a linked component representing the composition
   * @throws WasmException if linking fails due to incompatible interfaces
   * @throws IllegalArgumentException if components is null or empty
   */
  Component linkComponents(List<Component> components) throws WasmException;

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
   * Gets the component registry associated with this engine.
   *
   * <p>The registry provides component discovery, registration, and management capabilities.
   *
   * @return the component registry
   */
  ComponentRegistry getRegistry();

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
  ComponentInstance createInstance(
      Component component, Store store, List<Component> imports) throws WasmException;

  /**
   * Validates a component without creating an instance.
   *
   * <p>This method performs comprehensive validation of a component including interface
   * compatibility, resource requirements, and security constraints.
   *
   * @param component the component to validate
   * @return validation result with any issues found
   * @throws IllegalArgumentException if component is null
   */
  ComponentValidationResult validateComponent(Component component);

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
   * Sets the component registry for this engine.
   *
   * @param registry the component registry to use
   * @throws IllegalArgumentException if registry is null
   */
  void setRegistry(ComponentRegistry registry);
}
