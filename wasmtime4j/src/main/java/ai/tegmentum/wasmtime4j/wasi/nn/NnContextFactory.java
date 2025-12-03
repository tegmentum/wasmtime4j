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

package ai.tegmentum.wasmtime4j.wasi.nn;

/**
 * Factory interface for creating WASI-NN contexts.
 *
 * <p>This interface is implemented by runtime providers to create {@link NnContext} instances.
 * Users typically do not interact with this interface directly, instead using the factory method
 * on {@link ai.tegmentum.wasmtime4j.WasmRuntime}.
 *
 * @since 1.0.0
 */
public interface NnContextFactory {

  /**
   * Creates a new NnContext.
   *
   * @return a new WASI-NN context
   * @throws NnException if WASI-NN is not available or context creation fails
   */
  NnContext createNnContext() throws NnException;

  /**
   * Checks if WASI-NN is available in this runtime.
   *
   * @return true if WASI-NN is supported
   */
  boolean isNnAvailable();

  /**
   * Gets the default execution target for this runtime.
   *
   * <p>Returns CPU if no accelerators are available.
   *
   * @return the default execution target
   */
  default NnExecutionTarget getDefaultExecutionTarget() {
    return NnExecutionTarget.CPU;
  }
}
