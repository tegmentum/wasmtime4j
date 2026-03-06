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
     * Parses NnImplementationInfo from a JSON string returned by native code.
     *
     * <p>Expected format: {@code {"version":"...","backends":["..."],"default":"..."}}
     *
     * @param json the JSON string from native
     * @return the parsed implementation info
     */
    public static NnImplementationInfo parseFromJson(final String json) {
      if (json == null || json.isEmpty()) {
        return new NnImplementationInfo("unknown", new java.util.ArrayList<>(), null);
      }

      String version = extractJsonString(json, "version");
      String defaultBackend = extractJsonString(json, "default");
      List<String> backendsList = extractJsonArray(json, "backends");

      if (version == null) {
        version = "unknown";
      }
      if (defaultBackend != null && defaultBackend.isEmpty()) {
        defaultBackend = null;
      }

      return new NnImplementationInfo(version, backendsList, defaultBackend);
    }

    private static String extractJsonString(final String json, final String key) {
      final String pattern = "\"" + key + "\":\"";
      final int start = json.indexOf(pattern);
      if (start < 0) {
        return null;
      }
      final int valueStart = start + pattern.length();
      final int valueEnd = json.indexOf('"', valueStart);
      if (valueEnd < 0) {
        return null;
      }
      return json.substring(valueStart, valueEnd);
    }

    private static List<String> extractJsonArray(final String json, final String key) {
      final List<String> result = new java.util.ArrayList<>();
      final String pattern = "\"" + key + "\":[";
      final int start = json.indexOf(pattern);
      if (start < 0) {
        return result;
      }
      final int arrayStart = start + pattern.length();
      final int arrayEnd = json.indexOf(']', arrayStart);
      if (arrayEnd < 0) {
        return result;
      }
      final String arrayContent = json.substring(arrayStart, arrayEnd);
      if (arrayContent.isEmpty()) {
        return result;
      }
      final String[] items = arrayContent.split(",");
      for (final String item : items) {
        final String trimmed = item.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
          result.add(trimmed.substring(1, trimmed.length() - 1));
        }
      }
      return result;
    }

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
      this.backends = backends != null ? new java.util.ArrayList<>(backends) : null;
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
      return backends != null ? new java.util.ArrayList<>(backends) : null;
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
