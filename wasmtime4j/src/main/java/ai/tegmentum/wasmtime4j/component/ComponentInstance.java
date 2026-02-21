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

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents an instantiated WebAssembly component.
 *
 * <p>This interface provides core component instance functionality for:
 *
 * <ul>
 *   <li>Component function invocation
 *   <li>Interface binding and method dispatch
 *   <li>Component state management and lifecycle
 *   <li>Resource usage monitoring
 * </ul>
 *
 * <p>Component instances are created from compiled components through the {@link ComponentEngine}
 * and provide the runtime execution environment for component operations.
 *
 * @since 1.0.0
 */
public interface ComponentInstance extends AutoCloseable {

  /**
   * Gets the unique identifier for this component instance.
   *
   * @return the instance identifier
   */
  String getId();

  /**
   * Gets the component that this instance was created from.
   *
   * @return the parent component
   */
  Component getComponent();

  /**
   * Invokes a function exported by this component.
   *
   * @param functionName the name of the function to invoke
   * @param args the arguments to pass to the function
   * @return the result of the function invocation
   * @throws WasmException if function invocation fails
   */
  Object invoke(String functionName, Object... args) throws WasmException;

  /**
   * Checks if this component instance exports the specified function.
   *
   * @param functionName the function name to check
   * @return true if the function is exported, false otherwise
   */
  boolean hasFunction(String functionName);

  /**
   * Gets a component function by name.
   *
   * <p>This method returns a first-class function object that can be invoked multiple times without
   * the overhead of name lookup on each call. The returned {@link ComponentFunction} remains valid
   * as long as this component instance is valid.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Optional<ComponentFunction> addFunc = instance.getFunc("add");
   * if (addFunc.isPresent()) {
   *     Object result = addFunc.get().call(1, 2);
   * }
   * }</pre>
   *
   * @param functionName the name of the function to retrieve
   * @return an Optional containing the function if found, or empty if not found
   * @throws WasmException if function retrieval fails due to an error
   */
  Optional<ComponentFunction> getFunc(String functionName) throws WasmException;

  /**
   * Gets a component function by pre-computed export index.
   *
   * <p>This method provides O(1) function lookup using a {@link ComponentExportIndex} obtained from
   * {@link Component#exportIndex(ComponentExportIndex, String)}. This is more efficient than
   * string-based lookup when the same function is accessed repeatedly.
   *
   * @param exportIndex the pre-computed export index
   * @return an Optional containing the function if found, or empty if not found
   * @throws WasmException if function retrieval fails due to an error
   * @throws IllegalArgumentException if exportIndex is null
   * @since 1.0.0
   */
  Optional<ComponentFunction> getFunc(ComponentExportIndex exportIndex) throws WasmException;

  /**
   * Gets all exported function names from this component instance.
   *
   * @return set of exported function names
   */
  Set<String> getExportedFunctions();

  /**
   * Gets the exported interfaces from this component instance.
   *
   * @return map of interface name to interface definition
   * @throws WasmException if interface retrieval fails
   */
  Map<String, WitInterfaceDefinition> getExportedInterfaces() throws WasmException;

  /**
   * Binds an imported interface to this component instance.
   *
   * <p><strong>Note:</strong> This method has no backing Wasmtime API. The Panama implementation
   * throws {@link UnsupportedOperationException}.
   *
   * @param interfaceName the name of the interface to bind
   * @param implementation the implementation to bind
   * @throws WasmException if binding fails
   * @throws UnsupportedOperationException if the runtime does not support this operation
   * @deprecated Inconsistent support across runtimes. Panama throws UnsupportedOperationException.
   */
  @Deprecated
  void bindInterface(String interfaceName, Object implementation) throws WasmException;

  /**
   * Checks if this component instance exports a specific resource type.
   *
   * @param resourceName the resource name to check
   * @return true if the resource is exported, false otherwise
   * @throws WasmException if the lookup fails
   */
  boolean hasResource(String resourceName) throws WasmException;

  /**
   * Looks up a core module exported by this component instance.
   *
   * <p>Some components export core WebAssembly modules that can be instantiated separately.
   * This method retrieves such a module by name.
   *
   * @param moduleName the name of the exported module
   * @return an Optional containing the module if found, or empty if not exported
   * @throws WasmException if the lookup fails
   * @throws IllegalArgumentException if moduleName is null or empty
   * @since 1.0.0
   */
  Optional<Module> getModule(String moduleName) throws WasmException;

  /**
   * Gets the configuration used to create this instance.
   *
   * @return the instance configuration
   */
  ComponentInstanceConfig getConfig();

  /**
   * Checks if this component instance is still valid and usable.
   *
   * @return true if the instance is valid, false otherwise
   */
  boolean isValid();

  /**
   * Pauses execution of this component instance.
   *
   * @throws WasmException if pause operation fails
   */
  void pause() throws WasmException;

  /**
   * Resumes execution of this component instance.
   *
   * @throws WasmException if resume operation fails
   */
  void resume() throws WasmException;

  /**
   * Stops this component instance.
   *
   * @throws WasmException if stop operation fails
   */
  void stop() throws WasmException;

  @Override
  void close();
}
