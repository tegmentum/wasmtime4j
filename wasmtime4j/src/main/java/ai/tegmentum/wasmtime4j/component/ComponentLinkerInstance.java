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

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a scoped instance within a {@link ComponentLinker} for defining host imports.
 *
 * <p>This interface corresponds to Wasmtime's {@code LinkerInstance} type, which provides a scoped
 * builder pattern for defining functions, resources, and nested instances within a specific
 * interface path in the component linker.
 *
 * <p>A {@code ComponentLinkerInstance} is obtained from {@link ComponentLinker#root()} and scoped
 * further via {@link #instance(String)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentLinker<?> linker = ComponentLinker.create(engine);
 * linker.root()
 *     .instance("wasi:cli/stdout@0.2.0")
 *     .funcNew("print", params -> {
 *         System.out.println(params.get(0).asString());
 *         return Collections.emptyList();
 *     });
 * }</pre>
 *
 * @since 1.1.0
 */
public interface ComponentLinkerInstance {

  /**
   * Defines a host function within this instance scope.
   *
   * @param name the function name within this scope
   * @param implementation the host function implementation
   * @throws WasmException if the function definition fails
   * @throws IllegalArgumentException if name or implementation is null or empty
   */
  void funcNew(String name, ComponentHostFunction implementation) throws WasmException;

  /**
   * Defines an async host function within this instance scope.
   *
   * <p>Requires the engine to have been created with async support enabled.
   *
   * @param name the function name within this scope
   * @param implementation the host function implementation
   * @throws WasmException if the function definition fails or async support is not enabled
   * @throws IllegalArgumentException if name or implementation is null or empty
   */
  void funcNewAsync(String name, ComponentHostFunction implementation) throws WasmException;

  /**
   * Defines a Component Model resource type within this instance scope.
   *
   * <p>The scope path must be a valid interface path (e.g., "wasi:cli/stdout@0.2.0") to define a
   * resource. Calling this method at the root scope will fail.
   *
   * @param name the resource name within this scope
   * @param definition the resource type definition
   * @throws WasmException if the resource definition fails or scope is invalid for resources
   * @throws IllegalArgumentException if name or definition is null or empty
   */
  void resource(String name, ComponentResourceDefinition<?> definition) throws WasmException;

  /**
   * Associates a core WebAssembly module with this linker instance scope.
   *
   * <p>This corresponds to Wasmtime's {@code LinkerInstance::module()} which allows embedding
   * a core WebAssembly module within a component linker definition. This is used when a component
   * imports a core module.
   *
   * <p>The default implementation throws {@link UnsupportedOperationException}. Implementations
   * should override to call the native binding.
   *
   * @param name the name to associate the module under
   * @param module the core WebAssembly module to associate
   * @throws WasmException if the module association fails
   * @throws IllegalArgumentException if name or module is null
   * @since 1.1.0
   */
  default void module(final String name, final ai.tegmentum.wasmtime4j.Module module)
      throws WasmException {
    throw new UnsupportedOperationException(
        "Core module association in component linker requires native implementation");
  }

  /**
   * Defines a concurrent host function within this instance scope.
   *
   * <p>Concurrent host functions cooperatively interleave with other component operations on the
   * same store, enabling true concurrent component execution. This is part of the Component Model
   * async proposal.
   *
   * <p>The default implementation delegates to {@link #funcNewAsync(String, ComponentHostFunction)}.
   * Implementations should override to use native concurrent function support.
   *
   * @param name the function name within this scope
   * @param implementation the host function implementation
   * @throws WasmException if the function definition fails
   * @throws IllegalArgumentException if name or implementation is null or empty
   * @since 1.1.0
   */
  default void funcNewConcurrent(final String name, final ComponentHostFunction implementation)
      throws WasmException {
    funcNewAsync(name, implementation);
  }

  /**
   * Defines an async resource type within this instance scope.
   *
   * <p>Async resources support asynchronous drop operations in the Component Model async proposal.
   * The default implementation delegates to {@link #resource(String, ComponentResourceDefinition)}.
   *
   * @param name the resource name within this scope
   * @param definition the resource type definition
   * @throws WasmException if the resource definition fails
   * @throws IllegalArgumentException if name or definition is null or empty
   * @since 1.1.0
   */
  default void resourceAsync(final String name, final ComponentResourceDefinition<?> definition)
      throws WasmException {
    resource(name, definition);
  }

  /**
   * Defines a concurrent resource type within this instance scope.
   *
   * <p>Concurrent resources support concurrent drop and method operations. The default
   * implementation delegates to {@link #resourceAsync(String, ComponentResourceDefinition)}.
   *
   * @param name the resource name within this scope
   * @param definition the resource type definition
   * @throws WasmException if the resource definition fails
   * @throws IllegalArgumentException if name or definition is null or empty
   * @since 1.1.0
   */
  default void resourceConcurrent(final String name, final ComponentResourceDefinition<?> definition)
      throws WasmException {
    resourceAsync(name, definition);
  }

  /**
   * Creates a nested instance scope within this scope.
   *
   * <p>This corresponds to entering a named instance in Wasmtime's linker hierarchy. The returned
   * {@code ComponentLinkerInstance} is scoped to the combined path.
   *
   * @param name the instance name to enter (e.g., "wasi:cli/stdout@0.2.0" or a sub-path)
   * @return a new {@code ComponentLinkerInstance} scoped to the named instance
   * @throws WasmException if the instance scope cannot be entered
   * @throws IllegalArgumentException if name is null or empty
   */
  ComponentLinkerInstance instance(String name) throws WasmException;

  /**
   * Default implementation that wraps a {@link ComponentLinker} with a scoped path.
   *
   * <p>This class records the interface path and delegates operations to the underlying linker,
   * building appropriate WIT paths from the scope. This approach avoids the need to hold a native
   * {@code LinkerInstance} handle across FFI boundaries (which is impractical due to Rust borrow
   * lifetimes).
   */
  final class Scoped implements ComponentLinkerInstance {

    private final ComponentLinker<?> linker;
    private final String scopePath;

    /**
     * Creates a new scoped linker instance.
     *
     * @param linker the underlying component linker
     * @param scopePath the current scope path (empty string for root)
     */
    public Scoped(final ComponentLinker<?> linker, final String scopePath) {
      if (linker == null) {
        throw new IllegalArgumentException("linker cannot be null");
      }
      if (scopePath == null) {
        throw new IllegalArgumentException("scopePath cannot be null");
      }
      this.linker = linker;
      this.scopePath = scopePath;
    }

    @Override
    public void funcNew(final String name, final ComponentHostFunction implementation)
        throws WasmException {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("name cannot be null or empty");
      }
      if (implementation == null) {
        throw new IllegalArgumentException("implementation cannot be null");
      }
      final String witPath = scopePath.isEmpty() ? name : scopePath + "#" + name;
      linker.defineFunction(witPath, implementation);
    }

    @Override
    public void funcNewAsync(final String name, final ComponentHostFunction implementation)
        throws WasmException {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("name cannot be null or empty");
      }
      if (implementation == null) {
        throw new IllegalArgumentException("implementation cannot be null");
      }
      final String witPath = scopePath.isEmpty() ? name : scopePath + "#" + name;
      linker.defineFunctionAsync(witPath, implementation);
    }

    @Override
    public void resource(final String name, final ComponentResourceDefinition<?> definition)
        throws WasmException {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("name cannot be null or empty");
      }
      if (definition == null) {
        throw new IllegalArgumentException("definition cannot be null");
      }
      final String[] parts = parseInterfacePath(scopePath);
      linker.defineResource(parts[0], parts[1], name, definition);
    }

    @Override
    public ComponentLinkerInstance instance(final String name) throws WasmException {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("name cannot be null or empty");
      }
      final String newPath = scopePath.isEmpty() ? name : scopePath + "/" + name;
      return new Scoped(linker, newPath);
    }

    /**
     * Gets the current scope path.
     *
     * @return the scope path (empty string for root scope)
     */
    public String getScopePath() {
      return scopePath;
    }

    /**
     * Parses a WIT interface path into namespace and interface name components.
     *
     * <p>Expected format: "namespace:package/interface[@version]". For example,
     * "wasi:cli/stdout@0.2.0" parses to namespace="wasi:cli" and interfaceName="stdout".
     *
     * @param path the interface path to parse
     * @return a two-element array: [namespace, interfaceName]
     * @throws WasmException if the path format is invalid
     */
    private static String[] parseInterfacePath(final String path) throws WasmException {
      if (path == null || path.isEmpty()) {
        throw new WasmException(
            "Cannot define resource at root scope; "
                + "must be inside an interface instance (e.g., root().instance(\"ns:pkg/iface\"))");
      }
      final int slashIndex = path.indexOf('/');
      if (slashIndex < 0) {
        throw new WasmException(
            "Invalid interface path format: '"
                + path
                + "'; expected 'namespace:package/interface'");
      }
      final String namespace = path.substring(0, slashIndex);
      String interfacePart = path.substring(slashIndex + 1);
      // Strip version suffix if present (e.g., "stdout@0.2.0" → "stdout")
      final int atIndex = interfacePart.indexOf('@');
      if (atIndex >= 0) {
        interfacePart = interfacePart.substring(0, atIndex);
      }
      return new String[] {namespace, interfacePart};
    }
  }
}
