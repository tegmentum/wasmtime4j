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
 * Represents a loaded ML model graph in WASI-NN.
 *
 * <p>A graph is an opaque handle to a loaded neural network model. It is created by loading model
 * data through the {@link NnContext} and can be used to create execution contexts for inference.
 *
 * <p>Graphs must be closed when no longer needed to release native resources.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (NnContext context = wasmtime.createNnContext();
 *      NnGraph graph = context.loadGraph(modelBytes, NnGraphEncoding.ONNX, NnExecutionTarget.CPU)) {
 *     try (NnGraphExecutionContext exec = graph.createExecutionContext()) {
 *         // Run inference
 *         NnTensor[] outputs = exec.compute(inputs);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface NnGraph extends AutoCloseable {

  /**
   * Gets the native handle for this graph.
   *
   * <p>This is an implementation detail used for FFI calls.
   *
   * @return the native handle
   */
  long getNativeHandle();

  /**
   * Gets the encoding (format) of the loaded model.
   *
   * @return the graph encoding
   */
  NnGraphEncoding getEncoding();

  /**
   * Gets the execution target for this graph.
   *
   * @return the execution target
   */
  NnExecutionTarget getExecutionTarget();

  /**
   * Creates an execution context for running inference on this graph.
   *
   * <p>The returned context must be closed when no longer needed.
   *
   * @return a new execution context
   * @throws NnException if the execution context could not be created
   */
  NnGraphExecutionContext createExecutionContext() throws NnException;

  /**
   * Checks if this graph is still valid (not closed).
   *
   * @return true if the graph is valid
   */
  boolean isValid();

  /**
   * Gets the model name if available.
   *
   * @return the model name, or null if not available
   */
  String getModelName();

  /**
   * Closes this graph and releases native resources.
   *
   * <p>After closing, the graph cannot be used for creating execution contexts.
   */
  @Override
  void close();
}
