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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * A pre-instantiated WebAssembly component optimized for fast repeated instantiation.
 *
 * <p>ComponentInstancePre represents a component that has been prepared for instantiation with most
 * of the expensive setup work (type-checking and import resolution) already completed. This allows
 * for very fast creation of instances when needed.
 *
 * <p>ComponentInstancePre is particularly useful in scenarios where the same component needs to be
 * instantiated multiple times, such as in serverless functions or request handling.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentLinker linker = ComponentLinker.create(engine);
 * linker.enableWasiPreview2();
 *
 * // Pre-instantiate the component (expensive work done here)
 * ComponentInstancePre pre = linker.instantiatePre(component);
 *
 * // Fast repeated instantiation
 * ComponentInstance instance1 = pre.instantiate();
 * ComponentInstance instance2 = pre.instantiate();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentInstancePre extends Closeable {

  /**
   * Creates a new component instance from this pre-instantiated component.
   *
   * <p>This method is optimized for speed and should be significantly faster than normal
   * instantiation since most preparation work has already been completed. A fresh store with WASI
   * context (if configured) is created for each instantiation.
   *
   * @return a new ComponentInstance
   * @throws WasmException if instantiation fails
   */
  ComponentInstance instantiate() throws WasmException;

  /**
   * Gets the engine associated with this pre-instantiated component.
   *
   * @return the Engine used for pre-instantiation
   */
  Engine getEngine();

  /**
   * Checks if this pre-instantiated component is still valid and usable.
   *
   * @return true if the component is valid, false otherwise
   */
  boolean isValid();

  /**
   * Gets the number of instances that have been created from this pre-instantiated component.
   *
   * @return instance count
   */
  long getInstanceCount();

  /**
   * Gets the preparation time in nanoseconds.
   *
   * <p>This is the time taken to perform the initial pre-instantiation setup, including
   * type-checking and import resolution.
   *
   * @return preparation time in nanoseconds
   */
  long getPreparationTimeNs();

  /**
   * Gets the average instantiation time in nanoseconds.
   *
   * <p>This is the average time taken for each call to {@link #instantiate()}, calculated across
   * all instances created from this pre-instantiated component.
   *
   * @return average instantiation time in nanoseconds, or 0 if no instances have been created
   */
  long getAverageInstantiationTimeNs();

  /**
   * Closes the pre-instantiated component and releases associated resources.
   *
   * <p>After closing, the component becomes invalid and should not be used. Any instances created
   * from this ComponentInstancePre remain valid.
   */
  @Override
  void close();
}
