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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
import java.io.Closeable;
import java.util.Map;
import java.util.Set;

/**
 * WebAssembly Component Model linker interface for defining host functions and resolving imports.
 *
 * <p>A ComponentLinker provides the mechanism to define host functions and bind imports before
 * instantiating WebAssembly components. Unlike the core module {@link Linker}, ComponentLinker
 * works with WIT (WebAssembly Interface Types) interface definitions and supports the full
 * Component Model type system.
 *
 * <p>ComponentLinkers enable advanced Component Model integration patterns including:
 *
 * <ul>
 *   <li>Host function binding - Define Java functions implementing WIT interfaces
 *   <li>Component composition - Link multiple components together via their interfaces
 *   <li>WASI Preview 2 integration - Automatically provide WASI Preview 2 system interfaces
 *   <li>Resource management - Define and manage Component Model resources
 *   <li>Import resolution - Satisfy all component import requirements before instantiation
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a component linker
 * ComponentLinker linker = ComponentLinker.create(engine);
 *
 * // Define a host function for a WIT interface
 * linker.defineFunction("wasi:cli", "stdout", "print",
 *     ComponentHostFunction.create((params) -> {
 *         String message = params.get(0).asString();
 *         System.out.println(message);
 *         return ComponentVal.tuple();
 *     }));
 *
 * // Enable WASI Preview 2
 * linker.enableWasiPreview2();
 *
 * // Instantiate a component
 * ComponentInstance instance = linker.instantiate(store, component);
 *
 * // Call an exported function
 * ComponentFunc func = instance.getFunc("run");
 * ComponentVal[] results = func.call(ComponentVal.string("hello"));
 * }</pre>
 *
 * <p>ComponentLinkers are thread-safe and can be reused across multiple component instantiations.
 * They are associated with a specific Engine and inherit its configuration.
 *
 * @param <T> the type of user data associated with stores used with this linker
 * @since 1.0.0
 */
public interface ComponentLinker<T> extends Closeable {

  /**
   * Defines a host function that implements a WIT interface function.
   *
   * <p>The function will be available to any component instantiated through this linker that
   * imports the specified interface function. The function signature must match the WIT definition.
   *
   * @param interfaceNamespace the WIT interface namespace (e.g., "wasi:cli")
   * @param interfaceName the WIT interface name (e.g., "stdout")
   * @param functionName the function name within the interface (e.g., "print")
   * @param implementation the Java implementation of the function
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineFunction(
      String interfaceNamespace,
      String interfaceName,
      String functionName,
      ComponentHostFunction implementation)
      throws WasmException;

  /**
   * Defines a host function using the full WIT-style function path.
   *
   * <p>The path should be in the format "namespace:package/interface#function" or
   * "namespace:package/interface@version#function".
   *
   * @param witPath the full WIT path to the function
   * @param implementation the Java implementation of the function
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if witPath is null or malformed
   */
  void defineFunction(String witPath, ComponentHostFunction implementation) throws WasmException;

  /**
   * Defines an entire WIT interface implementation.
   *
   * <p>The implementation map should contain all functions defined in the interface. Missing
   * functions will cause instantiation to fail for components that require them.
   *
   * @param interfaceNamespace the WIT interface namespace (e.g., "wasi:cli")
   * @param interfaceName the WIT interface name (e.g., "stdout")
   * @param functions map of function names to implementations
   * @throws WasmException if the interface cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineInterface(
      String interfaceNamespace, String interfaceName, Map<String, ComponentHostFunction> functions)
      throws WasmException;

  /**
   * Defines a Component Model resource type.
   *
   * <p>Resources are handle-based types that allow host-managed objects to be passed to and from
   * components. The resource definition includes constructor, destructor, and method callbacks.
   *
   * @param interfaceNamespace the WIT interface namespace
   * @param interfaceName the WIT interface name
   * @param resourceName the resource name within the interface
   * @param resourceDefinition the resource type definition
   * @throws WasmException if the resource cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineResource(
      String interfaceNamespace,
      String interfaceName,
      String resourceName,
      ComponentResourceDefinition<?> resourceDefinition)
      throws WasmException;

  /**
   * Links another component's exports as imports for future instantiations.
   *
   * <p>All exports from the provided component instance will be available as imports for components
   * instantiated through this linker.
   *
   * @param instance the component instance whose exports should be linked
   * @throws WasmException if linking fails
   * @throws IllegalArgumentException if instance is null
   */
  void linkInstance(ComponentInstance instance) throws WasmException;

  /**
   * Links a compiled component's exports as imports.
   *
   * <p>The component will be instantiated and its exports will be available as imports for
   * components instantiated through this linker.
   *
   * @param store the store to instantiate the component in
   * @param component the component whose exports should be linked
   * @return the instantiated component for further use
   * @throws WasmException if linking fails
   * @throws IllegalArgumentException if any parameter is null
   */
  ComponentInstance linkComponent(Store store, Component component) throws WasmException;

  /**
   * Instantiates a WebAssembly component using this linker to resolve imports.
   *
   * <p>The linker will provide all defined functions, interfaces, and resources to satisfy the
   * component's import requirements. If any imports cannot be satisfied, instantiation will fail.
   *
   * @param store the store to instantiate the component in
   * @param component the compiled component to instantiate
   * @return a new ComponentInstance with all imports resolved
   * @throws WasmException if instantiation fails or imports cannot be satisfied
   * @throws IllegalArgumentException if store or component is null
   */
  ComponentInstance instantiate(Store store, Component component) throws WasmException;

  /**
   * Enables WASI Preview 2 support for components instantiated through this linker.
   *
   * <p>This automatically defines all WASI Preview 2 interfaces that components can import,
   * including filesystem, networking, clocks, and random number generation.
   *
   * @throws WasmException if WASI Preview 2 cannot be enabled
   */
  void enableWasiPreview2() throws WasmException;

  /**
   * Enables WASI Preview 2 with custom configuration.
   *
   * @param config the WASI Preview 2 configuration
   * @throws WasmException if WASI Preview 2 cannot be enabled
   * @throws IllegalArgumentException if config is null
   */
  void enableWasiPreview2(WasiPreview2Config config) throws WasmException;

  /**
   * Gets the engine associated with this linker.
   *
   * @return the Engine that created this linker
   */
  Engine getEngine();

  /**
   * Checks if the linker is still valid and usable.
   *
   * @return true if the linker is valid, false otherwise
   */
  boolean isValid();

  /**
   * Checks if a specific interface has been defined in this linker.
   *
   * @param interfaceNamespace the WIT interface namespace
   * @param interfaceName the WIT interface name
   * @return true if the interface is defined, false otherwise
   * @throws IllegalArgumentException if any parameter is null
   */
  boolean hasInterface(String interfaceNamespace, String interfaceName);

  /**
   * Checks if a specific function within an interface has been defined.
   *
   * @param interfaceNamespace the WIT interface namespace
   * @param interfaceName the WIT interface name
   * @param functionName the function name
   * @return true if the function is defined, false otherwise
   * @throws IllegalArgumentException if any parameter is null
   */
  boolean hasFunction(String interfaceNamespace, String interfaceName, String functionName);

  /**
   * Gets all interfaces currently defined in this linker.
   *
   * @return set of interface identifiers in "namespace:package/interface" format
   */
  Set<String> getDefinedInterfaces();

  /**
   * Gets all functions defined for a specific interface.
   *
   * @param interfaceNamespace the WIT interface namespace
   * @param interfaceName the WIT interface name
   * @return set of function names, or empty set if interface not defined
   * @throws IllegalArgumentException if any parameter is null
   */
  Set<String> getDefinedFunctions(String interfaceNamespace, String interfaceName);

  /**
   * Validates that all imports for a component can be satisfied by this linker.
   *
   * @param component the component to validate
   * @return validation result with detailed information about any missing imports
   * @throws IllegalArgumentException if component is null
   */
  ComponentImportValidation validateImports(Component component);

  /**
   * Creates an alias from one interface to another.
   *
   * <p>This allows an interface defined under one name to also satisfy imports under another name.
   *
   * @param fromNamespace the source interface namespace
   * @param fromInterface the source interface name
   * @param toNamespace the target interface namespace
   * @param toInterface the target interface name
   * @throws WasmException if the alias cannot be created
   * @throws IllegalArgumentException if any parameter is null
   */
  void aliasInterface(
      String fromNamespace, String fromInterface, String toNamespace, String toInterface)
      throws WasmException;

  /**
   * Closes the linker and releases associated resources.
   *
   * <p>After closing, the linker becomes invalid and should not be used.
   */
  @Override
  void close();

  /**
   * Creates a new ComponentLinker for the given engine.
   *
   * @param <T> the type of user data associated with stores used with this linker
   * @param engine the engine to create the linker for
   * @return a new ComponentLinker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   */
  static <T> ComponentLinker<T> create(Engine engine) throws WasmException {
    return ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory.create()
        .createComponentLinker(engine);
  }
}
