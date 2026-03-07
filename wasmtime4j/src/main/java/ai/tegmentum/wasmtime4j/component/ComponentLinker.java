/*
 * Copyright 2025 Tegmentum AI
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
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
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
   * Defines an async host function that implements a WIT interface function.
   *
   * <p>This is the async variant of {@link #defineFunction(String, String, String,
   * ComponentHostFunction)}. It registers the function using {@code func_new_async} instead of
   * {@code func_new}, allowing it to be called from async contexts.
   *
   * <p>Requires the engine to have been created with async support enabled.
   *
   * @param interfaceNamespace the WIT interface namespace (e.g., "wasi:cli")
   * @param interfaceName the WIT interface name (e.g., "stdout")
   * @param functionName the function name within the interface (e.g., "print")
   * @param implementation the Java implementation of the function
   * @throws WasmException if the function cannot be defined or async support is not enabled
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineFunctionAsync(
      String interfaceNamespace,
      String interfaceName,
      String functionName,
      ComponentHostFunction implementation)
      throws WasmException;

  /**
   * Defines an async host function using the full WIT-style function path.
   *
   * <p>This is the async variant of {@link #defineFunction(String, ComponentHostFunction)}.
   *
   * <p>Requires the engine to have been created with async support enabled.
   *
   * @param witPath the full WIT path to the function
   * @param implementation the Java implementation of the function
   * @throws WasmException if the function cannot be defined or async support is not enabled
   * @throws IllegalArgumentException if witPath is null or malformed
   */
  void defineFunctionAsync(String witPath, ComponentHostFunction implementation)
      throws WasmException;

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
   * Defines a core WebAssembly module under the given instance path and name.
   *
   * <p>This enables providing a core wasm {@link ai.tegmentum.wasmtime4j.Module} as an import to a
   * component. The module is saved within the linker for the specified instance path and name.
   *
   * @param instancePath the linker instance scope path (e.g., "wasi:cli/command")
   * @param name the name to associate the module under
   * @param module the core WebAssembly module to define
   * @throws WasmException if the module definition fails
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.1.0
   */
  void defineModule(String instancePath, String name, ai.tegmentum.wasmtime4j.Module module)
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
   * Pre-instantiates a component for fast repeated instantiation.
   *
   * <p>This performs the expensive type-checking and import resolution work once, allowing
   * subsequent instantiations via {@link ComponentInstancePre#instantiate()} to be significantly
   * faster.
   *
   * @param component the compiled component to pre-instantiate
   * @return a ComponentInstancePre that can be used for fast repeated instantiation
   * @throws WasmException if pre-instantiation fails or imports cannot be satisfied
   * @throws IllegalArgumentException if component is null
   */
  ComponentInstancePre instantiatePre(Component component) throws WasmException;

  /**
   * Asynchronously instantiates a component with all defined imports resolved.
   *
   * <p>This is the async variant of {@link #instantiate(Store, Component)}. It requires the engine
   * to have been created with async support enabled.
   *
   * @param store the store for the new instance
   * @param component the compiled component to instantiate
   * @return a CompletableFuture that completes with a new ComponentInstance
   * @throws WasmException if async support is not enabled
   * @throws IllegalArgumentException if store or component is null
   * @since 1.1.0
   */
  default java.util.concurrent.CompletableFuture<ComponentInstance> instantiateAsync(
      final Store store, final Component component) throws WasmException {
    // Default: delegate to sync instantiation on ForkJoinPool.
    // Implementations should override to use native linker.instantiate_async().
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return instantiate(store, component);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Returns the component type with all imports substituted by the linker's definitions.
   *
   * <p>This method computes a {@link ComponentTypeInfo} reflecting the component's type after the
   * linker has filled in all available imports. The result shows which imports are still
   * unsatisfied and which exports will be available.
   *
   * <p>This is useful for checking link-time compatibility before instantiation, or for generating
   * documentation about a component's effective interface after imports are resolved.
   *
   * @param component the component to compute the substituted type for
   * @return the component type with linker-provided imports substituted
   * @throws WasmException if the computation fails
   * @throws IllegalArgumentException if component is null
   * @since 1.1.0
   */
  default ComponentTypeInfo substitutedComponentType(final Component component)
      throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    // Default: return the component's own type info (no substitution).
    // Implementations should override to use Wasmtime's Linker::substituted_component_type().
    return component.componentType();
  }

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
   * Enables WASI HTTP support in this component linker.
   *
   * <p>WASI Preview 2 must be enabled first via {@link #enableWasiPreview2()} before enabling HTTP
   * support. This adds the wasi:http/types and wasi:http/outgoing-handler interfaces.
   *
   * @throws WasmException if WASI HTTP cannot be enabled (e.g., WASI Preview 2 not enabled first)
   */
  void enableWasiHttp() throws WasmException;

  /**
   * Enables WASI HTTP support with custom configuration.
   *
   * <p>WASI Preview 2 must be enabled first via {@link #enableWasiPreview2()} before enabling HTTP
   * support.
   *
   * @param config the WASI HTTP configuration
   * @throws WasmException if WASI HTTP cannot be enabled
   * @throws IllegalArgumentException if config is null
   */
  void enableWasiHttp(WasiHttpConfig config) throws WasmException;

  /**
   * Enables WASI Config support in this component linker.
   *
   * <p>Adds the {@code wasi:config/store} interfaces, allowing components to read configuration
   * variables at runtime via {@code wasi:config/store.get} and {@code wasi:config/store.get-all}.
   *
   * @throws WasmException if WASI Config cannot be enabled
   */
  void enableWasiConfig() throws WasmException;

  /**
   * Sets configuration variables for WASI Config.
   *
   * <p>These key-value pairs will be available to components via the {@code wasi:config/store}
   * interface. This method also enables WASI Config if not already enabled.
   *
   * @param variables the configuration variables as key-value pairs
   * @throws WasmException if the configuration variables cannot be set
   * @throws IllegalArgumentException if variables is null
   */
  void setConfigVariables(java.util.Map<String, String> variables) throws WasmException;

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
   * <p>This queries the native linker state directly, so it includes interfaces registered by
   * {@link #enableWasiPreview2(WasiPreview2Config)} and {@link #enableWasiHttp(WasiHttpConfig)}.
   *
   * @return set of interface identifiers in "namespace/interface" format
   */
  Set<String> getDefinedInterfaces();

  /**
   * Gets all functions defined for a specific interface.
   *
   * <p>This queries the native linker state directly, so it includes functions registered by WASI
   * enablement calls.
   *
   * @param interfaceNamespace the WIT interface namespace
   * @param interfaceName the WIT interface name
   * @return set of function names, or empty set if interface not defined
   * @throws IllegalArgumentException if any parameter is null
   */
  Set<String> getDefinedFunctions(String interfaceNamespace, String interfaceName);

  /**
   * Checks if WASI Preview 2 support is enabled in this linker.
   *
   * @return true if WASI Preview 2 is enabled
   */
  boolean isWasiP2Enabled();

  /**
   * Checks if WASI HTTP support is enabled in this linker.
   *
   * @return true if WASI HTTP is enabled
   */
  boolean isWasiHttpEnabled();

  /**
   * Gets the number of host functions defined in this linker.
   *
   * @return the count of defined host functions
   */
  int getHostFunctionCount();

  /**
   * Gets the number of interfaces defined in this linker.
   *
   * @return the count of defined interfaces
   */
  int getInterfaceCount();

  /**
   * Enables or disables async support for this linker.
   *
   * <p>When enabled, the linker can define async host functions and the instantiation will support
   * concurrent calls.
   *
   * @param enabled true to enable async support, false to disable
   * @throws WasmException if the operation fails
   */
  void setAsyncSupport(boolean enabled) throws WasmException;

  /**
   * Sets the maximum random buffer size for WASI random operations.
   *
   * @param maxSize the maximum size in bytes
   * @throws WasmException if the operation fails
   * @throws IllegalArgumentException if maxSize is negative
   */
  void setWasiMaxRandomSize(long maxSize) throws WasmException;

  /**
   * Gets the root linker instance for builder-style import definitions.
   *
   * <p>This method provides access to Wasmtime's {@code LinkerInstance} builder pattern, enabling
   * scoped function and resource definitions through method chaining.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * linker.root()
   *     .instance("wasi:cli/stdout@0.2.0")
   *     .funcNew("print", params -> {
   *         System.out.println(params.get(0).asString());
   *         return Collections.emptyList();
   *     });
   * }</pre>
   *
   * @return the root {@link ComponentLinkerInstance}
   * @since 1.1.0
   */
  default ComponentLinkerInstance root() {
    return new ComponentLinkerInstance.Scoped(this, "");
  }

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
   * Allows or disallows shadowing of previously defined names.
   *
   * <p>When enabled, new definitions can override existing ones without error.
   *
   * @param allow true to allow shadowing, false to disallow
   */
  void allowShadowing(boolean allow);

  /**
   * Defines all unknown imports of the given component as traps.
   *
   * <p>This is useful for partially-defined components where some imports should trap at runtime
   * rather than failing at instantiation time.
   *
   * @param component the component whose unknown imports should be trapped
   * @throws WasmException if the operation fails
   * @throws IllegalArgumentException if component is null
   */
  void defineUnknownImportsAsTraps(Component component) throws WasmException;

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
