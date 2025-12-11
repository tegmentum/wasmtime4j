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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Main entry point for WASI-NN machine learning operations.
 *
 * <p>NnContext provides methods for loading ML models and checking available backends. It is
 * obtained from a WasmRuntime instance and must be closed when no longer needed.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasmRuntime runtime = WasmRuntimeFactory.create();
 *      NnContext nn = runtime.createNnContext()) {
 *
 *     // Check if ONNX backend is available
 *     if (nn.isEncodingSupported(NnGraphEncoding.ONNX)) {
 *         // Load model from bytes
 *         byte[] modelData = Files.readAllBytes(Path.of("model.onnx"));
 *         try (NnGraph graph = nn.loadGraph(modelData, NnGraphEncoding.ONNX, NnExecutionTarget.CPU)) {
 *             // Create execution context and run inference
 *             try (NnGraphExecutionContext exec = graph.createExecutionContext()) {
 *                 NnTensor input = NnTensor.fromFloatArray(new int[]{1, 28, 28}, inputData);
 *                 List<NnTensor> outputs = exec.computeByIndex(input);
 *                 float[] predictions = outputs.get(0).toFloatArray();
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface NnContext extends AutoCloseable {

  /**
   * Loads a graph (ML model) from raw byte data.
   *
   * <p>The byte data should contain a complete model in the specified encoding format.
   *
   * @param modelData the model data as bytes
   * @param encoding the model encoding format
   * @param target the execution target (CPU, GPU, TPU)
   * @return a loaded graph
   * @throws NnException if loading fails
   * @throws NullPointerException if any argument is null
   */
  NnGraph loadGraph(byte[] modelData, NnGraphEncoding encoding, NnExecutionTarget target)
      throws NnException;

  /**
   * Loads a graph from multiple byte arrays.
   *
   * <p>Some model formats (like OpenVINO) require multiple files - this method accepts a list of
   * byte arrays for such cases.
   *
   * @param modelParts the model data parts as byte arrays
   * @param encoding the model encoding format
   * @param target the execution target (CPU, GPU, TPU)
   * @return a loaded graph
   * @throws NnException if loading fails
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if modelParts is empty
   */
  NnGraph loadGraph(List<byte[]> modelParts, NnGraphEncoding encoding, NnExecutionTarget target)
      throws NnException;

  /**
   * Loads a graph from a file path.
   *
   * <p>This is a convenience method that reads the file and calls {@link #loadGraph(byte[],
   * NnGraphEncoding, NnExecutionTarget)}.
   *
   * @param modelPath the path to the model file
   * @param encoding the model encoding format
   * @param target the execution target (CPU, GPU, TPU)
   * @return a loaded graph
   * @throws NnException if loading fails
   * @throws NullPointerException if any argument is null
   */
  NnGraph loadGraphFromFile(Path modelPath, NnGraphEncoding encoding, NnExecutionTarget target)
      throws NnException;

  /**
   * Loads a graph by name from a preconfigured model registry.
   *
   * <p>This method allows loading models that have been preloaded or registered with the runtime.
   * Support for this feature depends on the backend configuration.
   *
   * @param name the registered model name
   * @return a loaded graph
   * @throws NnException if loading fails or the model is not found
   * @throws NullPointerException if name is null
   */
  NnGraph loadGraphByName(String name) throws NnException;

  /**
   * Gets the set of supported model encodings.
   *
   * @return the set of supported encodings
   */
  Set<NnGraphEncoding> getSupportedEncodings();

  /**
   * Gets the set of supported execution targets.
   *
   * @return the set of supported targets
   */
  Set<NnExecutionTarget> getSupportedTargets();

  /**
   * Checks if a specific model encoding is supported.
   *
   * @param encoding the encoding to check
   * @return true if the encoding is supported
   */
  boolean isEncodingSupported(NnGraphEncoding encoding);

  /**
   * Checks if a specific execution target is supported.
   *
   * @param target the target to check
   * @return true if the target is supported
   */
  boolean isTargetSupported(NnExecutionTarget target);

  /**
   * Checks if WASI-NN is available in this runtime.
   *
   * <p>WASI-NN is an experimental feature that may not be available in all Wasmtime builds.
   *
   * @return true if WASI-NN is available
   */
  boolean isAvailable();

  /**
   * Gets information about the WASI-NN implementation.
   *
   * @return implementation information
   */
  NnImplementationInfo getImplementationInfo();

  /**
   * Checks if this context is still valid (not closed).
   *
   * @return true if the context is valid
   */
  boolean isValid();

  /**
   * Closes this context and releases native resources.
   *
   * <p>Any graphs created from this context should be closed before closing the context.
   */
  @Override
  void close();

  /** Information about the WASI-NN implementation. */
  final class NnImplementationInfo {

    private final String version;
    private final List<String> backends;
    private final String defaultBackend;

    /**
     * Creates a new NnImplementationInfo.
     *
     * @param version the implementation version
     * @param backends the list of available backend names
     * @param defaultBackend the default backend name (may be null)
     */
    public NnImplementationInfo(
        final String version, final List<String> backends, final String defaultBackend) {
      this.version = version;
      this.backends = backends;
      this.defaultBackend = defaultBackend;
    }

    /**
     * Gets the implementation version.
     *
     * @return the version
     */
    public String getVersion() {
      return version;
    }

    /**
     * Gets the list of available backend names.
     *
     * @return the backends
     */
    public List<String> getBackends() {
      return backends;
    }

    /**
     * Gets the default backend name.
     *
     * @return the default backend name (may be null)
     */
    public String getDefaultBackend() {
      return defaultBackend;
    }

    /**
     * Checks if a specific backend is available.
     *
     * @param backendName the backend name to check
     * @return true if the backend is available
     */
    public boolean hasBackend(final String backendName) {
      return backends != null && backends.contains(backendName);
    }
  }
}
