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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines a Component Model resource type for use with {@link ComponentLinker}.
 *
 * <p>Resources are handle-based types that allow host-managed objects to be passed to and from
 * WebAssembly components. A resource definition includes:
 *
 * <ul>
 *   <li>A constructor function that creates new resource instances
 *   <li>A destructor function that cleans up resource instances
 *   <li>Optional methods that operate on resource instances
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Define a file resource
 * ComponentResourceDefinition<FileHandle> fileResource =
 *     ComponentResourceDefinition.<FileHandle>builder("file")
 *         .constructor(params -> new FileHandle(params.get(0).asString()))
 *         .destructor(handle -> handle.close())
 *         .method("read", (handle, params) -> {
 *             int count = params.get(0).asS32();
 *             byte[] data = handle.read(count);
 *             return List.of(ComponentVal.list(toComponentVals(data)));
 *         })
 *         .method("write", (handle, params) -> {
 *             List<ComponentVal> bytes = params.get(0).asList();
 *             handle.write(fromComponentVals(bytes));
 *             return List.of();
 *         })
 *         .build();
 *
 * linker.defineResource("wasi:filesystem", "types", "file", fileResource);
 * }</pre>
 *
 * @param <T> the type of the host object managed by this resource
 * @since 1.0.0
 */
public interface ComponentResourceDefinition<T> {

  /**
   * Gets the resource type name.
   *
   * @return the resource name
   */
  String getName();

  /**
   * Gets the constructor function for creating new resource instances.
   *
   * @return the optional constructor
   */
  Optional<ResourceConstructor<T>> getConstructor();

  /**
   * Gets the destructor function for cleaning up resource instances.
   *
   * @return the optional destructor
   */
  Optional<Consumer<T>> getDestructor();

  /**
   * Gets the methods defined for this resource.
   *
   * @return map of method names to implementations
   */
  Map<String, ResourceMethod<T>> getMethods();

  /**
   * Gets a specific method by name.
   *
   * @param name the method name
   * @return the optional method implementation
   */
  default Optional<ResourceMethod<T>> getMethod(final String name) {
    return Optional.ofNullable(getMethods().get(name));
  }

  /**
   * Creates a new builder for a resource definition.
   *
   * @param <T> the type of the host object
   * @param name the resource name
   * @return a new builder
   */
  static <T> Builder<T> builder(final String name) {
    return new Builder<>(name);
  }

  /** Functional interface for resource constructors. */
  @FunctionalInterface
  interface ResourceConstructor<T> {
    /**
     * Creates a new resource instance.
     *
     * @param params the constructor parameters
     * @return the new resource instance
     * @throws WasmException if construction fails
     */
    T construct(java.util.List<ComponentVal> params) throws WasmException;
  }

  /** Functional interface for resource methods. */
  @FunctionalInterface
  interface ResourceMethod<T> {
    /**
     * Invokes the method on a resource instance.
     *
     * @param instance the resource instance
     * @param params the method parameters
     * @return the method results
     * @throws WasmException if the method fails
     */
    java.util.List<ComponentVal> invoke(T instance, java.util.List<ComponentVal> params)
        throws WasmException;
  }

  /** Builder for ComponentResourceDefinition. */
  class Builder<T> {
    private final String name;
    private ResourceConstructor<T> constructor;
    private Consumer<T> destructor;
    private final Map<String, ResourceMethod<T>> methods = new HashMap<>();

    Builder(final String name) {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("Resource name cannot be null or empty");
      }
      this.name = name;
    }

    /**
     * Sets the constructor function.
     *
     * @param constructor the constructor
     * @return this builder
     */
    public Builder<T> constructor(final ResourceConstructor<T> constructor) {
      this.constructor = constructor;
      return this;
    }

    /**
     * Sets the constructor for resources that take no parameters.
     *
     * @param supplier the supplier that creates new instances
     * @return this builder
     */
    public Builder<T> constructor(final Supplier<T> supplier) {
      this.constructor = params -> supplier.get();
      return this;
    }

    /**
     * Sets the destructor function.
     *
     * @param destructor the destructor
     * @return this builder
     */
    public Builder<T> destructor(final Consumer<T> destructor) {
      this.destructor = destructor;
      return this;
    }

    /**
     * Adds a method to the resource.
     *
     * @param name the method name
     * @param method the method implementation
     * @return this builder
     */
    public Builder<T> method(final String name, final ResourceMethod<T> method) {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("Method name cannot be null or empty");
      }
      methods.put(name, method);
      return this;
    }

    /**
     * Builds the resource definition.
     *
     * @return the resource definition
     */
    public ComponentResourceDefinition<T> build() {
      return new Impl<>(name, constructor, destructor, methods);
    }
  }

  /** Default implementation of ComponentResourceDefinition. */
  final class Impl<T> implements ComponentResourceDefinition<T> {
    private final String name;
    private final ResourceConstructor<T> constructor;
    private final Consumer<T> destructor;
    private final Map<String, ResourceMethod<T>> methods;

    Impl(
        final String name,
        final ResourceConstructor<T> constructor,
        final Consumer<T> destructor,
        final Map<String, ResourceMethod<T>> methods) {
      this.name = name;
      this.constructor = constructor;
      this.destructor = destructor;
      this.methods = Map.copyOf(methods);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Optional<ResourceConstructor<T>> getConstructor() {
      return Optional.ofNullable(constructor);
    }

    @Override
    public Optional<Consumer<T>> getDestructor() {
      return Optional.ofNullable(destructor);
    }

    @Override
    public Map<String, ResourceMethod<T>> getMethods() {
      return methods;
    }
  }
}
