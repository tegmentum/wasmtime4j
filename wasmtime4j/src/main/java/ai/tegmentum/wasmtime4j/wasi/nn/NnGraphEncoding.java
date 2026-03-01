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
 * Represents the ML model encodings (formats) supported by WASI-NN.
 *
 * <p>These encodings correspond to the graph_encoding enum in the WASI-NN specification. The
 * availability of specific backends depends on the Wasmtime build configuration.
 *
 * @since 1.0.0
 */
public enum NnGraphEncoding {

  /** OpenVINO Intermediate Representation format. */
  OPENVINO("openvino", "OpenVINO IR"),

  /** ONNX (Open Neural Network Exchange) format. */
  ONNX("onnx", "ONNX"),

  /** TensorFlow SavedModel format. */
  TENSORFLOW("tensorflow", "TensorFlow"),

  /** PyTorch JIT format. */
  PYTORCH("pytorch", "PyTorch"),

  /** TensorFlow Lite format. */
  TENSORFLOWLITE("tensorflowlite", "TensorFlow Lite"),

  /** GGML format (for LLMs like LLaMA). */
  GGML("ggml", "GGML"),

  /** Autodetect format from file content. */
  AUTODETECT("autodetect", "Auto-detect");

  private final String wasiName;
  private final String displayName;

  NnGraphEncoding(final String wasiName, final String displayName) {
    this.wasiName = wasiName;
    this.displayName = displayName;
  }

  /**
   * Gets the WASI-NN specification name for this encoding.
   *
   * @return the WASI-NN encoding name
   */
  public String getWasiName() {
    return wasiName;
  }

  /**
   * Gets a human-readable display name for this encoding.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Parses a graph encoding from its WASI-NN name.
   *
   * @param wasiName the WASI-NN encoding name
   * @return the corresponding graph encoding
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static NnGraphEncoding fromWasiName(final String wasiName) {
    for (final NnGraphEncoding encoding : values()) {
      if (encoding.wasiName.equals(wasiName)) {
        return encoding;
      }
    }
    throw new IllegalArgumentException("Unknown graph encoding: " + wasiName);
  }

  /**
   * Gets the native code value for this graph encoding.
   *
   * @return the native code (ordinal)
   */
  public int getNativeCode() {
    return ordinal();
  }

  /**
   * Creates a graph encoding from a native code.
   *
   * @param code the native code
   * @return the corresponding graph encoding
   * @throws IllegalArgumentException if the code is invalid
   */
  public static NnGraphEncoding fromNativeCode(final int code) {
    final NnGraphEncoding[] values = values();
    if (code < 0 || code >= values.length) {
      throw new IllegalArgumentException("Invalid graph encoding code: " + code);
    }
    return values[code];
  }

  @Override
  public String toString() {
    return displayName;
  }
}
