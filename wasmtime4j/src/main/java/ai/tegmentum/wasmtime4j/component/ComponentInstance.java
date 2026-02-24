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
   * Gets a typed component function by name and signature string.
   *
   * <p>This is a convenience method that combines {@link #getFunc(String)} with {@link
   * ComponentTypedFunc#create(ComponentFunc, String)} to produce a type-safe function handle. The
   * returned {@link ComponentTypedFunc} provides direct primitive type access for maximum
   * performance.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Optional<ComponentTypedFunc> addFunc = instance.getTypedFunc("add", "s32,s32->s32");
   * if (addFunc.isPresent()) {
   *     int result = addFunc.get().callS32S32ToS32(10, 20);
   * }
   * }</pre>
   *
   * @param functionName the name of the function to retrieve
   * @param signature the type signature string (e.g., "s32,s32->s32")
   * @return an Optional containing the typed function if found, or empty if not found
   * @throws WasmException if function retrieval fails
   * @throws IllegalArgumentException if functionName or signature is null or empty
   * @since 1.1.0
   */
  default Optional<ComponentTypedFunc> getTypedFunc(
      final String functionName, final String signature) throws WasmException {
    if (functionName == null || functionName.isEmpty()) {
      throw new IllegalArgumentException("functionName cannot be null or empty");
    }
    if (signature == null || signature.isEmpty()) {
      throw new IllegalArgumentException("signature cannot be null or empty");
    }

    final Optional<ComponentFunction> func = getFunc(functionName);
    if (func.isEmpty()) {
      return Optional.empty();
    }

    final ComponentFunction componentFunction = func.get();
    if (!(componentFunction instanceof ComponentFunc)) {
      throw new WasmException(
          "ComponentFunction does not implement ComponentFunc, "
              + "cannot create typed function for: " + functionName);
    }

    return Optional.of(
        ComponentTypedFunc.create((ComponentFunc) componentFunction, signature));
  }

  /**
   * Gets all exported function names from this component instance.
   *
   * @return set of exported function names
   */
  Set<String> getExportedFunctions();

  /**
   * Checks if this component instance exports a specific resource type.
   *
   * @param resourceName the resource name to check
   * @return true if the resource is exported, false otherwise
   * @throws WasmException if the lookup fails
   */
  boolean hasResource(String resourceName) throws WasmException;

  /**
   * Gets the resource type definition exported by this component instance.
   *
   * <p>If the component exports a resource type with the given name, this method returns a handle
   * representing that resource type. The handle can be used to create instances of the resource
   * or to check resource type compatibility.
   *
   * @param resourceName the name of the resource to retrieve
   * @return an Optional containing the resource handle if found, or empty if not exported
   * @throws WasmException if the lookup fails
   * @throws IllegalArgumentException if resourceName is null or empty
   * @since 1.0.0
   */
  default Optional<ComponentResourceHandle> getExportedResource(final String resourceName)
      throws WasmException {
    if (resourceName == null || resourceName.isEmpty()) {
      throw new IllegalArgumentException("resourceName cannot be null or empty");
    }
    // Default: delegates to hasResource and returns a handle with no native backing
    if (hasResource(resourceName)) {
      return Optional.of(ComponentResourceHandle.own(resourceName, 0));
    }
    return Optional.empty();
  }

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

  @Override
  void close();
}
