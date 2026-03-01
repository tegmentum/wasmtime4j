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
 * Error codes for WASI-NN operations.
 *
 * <p>These codes correspond to the error enum in the WASI-NN specification.
 *
 * @since 1.0.0
 */
public enum NnErrorCode {

  /** Invalid argument provided to a function. */
  INVALID_ARGUMENT("invalid-argument", "Invalid argument provided"),

  /** Invalid model encoding format. */
  INVALID_ENCODING("invalid-encoding", "Unsupported or invalid model encoding format"),

  /** Operation timed out. */
  TIMEOUT("timeout", "Operation exceeded timeout limit"),

  /** Runtime error during inference. */
  RUNTIME_ERROR("runtime-error", "Runtime error during execution"),

  /** Operation not supported by the backend. */
  UNSUPPORTED_OPERATION("unsupported-operation", "Operation not supported by backend"),

  /** Model or data too large for available resources. */
  TOO_LARGE("too-large", "Resource too large for available memory"),

  /** Model or resource not found. */
  NOT_FOUND("not-found", "Model or resource not found"),

  /** Security constraint violation. */
  SECURITY("security", "Security constraint violated"),

  /** Unknown or unclassified error. */
  UNKNOWN("unknown", "Unknown error occurred");

  private final String wasiName;
  private final String description;

  NnErrorCode(final String wasiName, final String description) {
    this.wasiName = wasiName;
    this.description = description;
  }

  /**
   * Gets the WASI-NN specification name for this error code.
   *
   * @return the WASI-NN error name
   */
  public String getWasiName() {
    return wasiName;
  }

  /**
   * Gets a human-readable description of this error code.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Parses an error code from its WASI-NN name.
   *
   * @param wasiName the WASI-NN error name
   * @return the corresponding error code
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static NnErrorCode fromWasiName(final String wasiName) {
    for (final NnErrorCode code : values()) {
      if (code.wasiName.equals(wasiName)) {
        return code;
      }
    }
    throw new IllegalArgumentException("Unknown error code: " + wasiName);
  }

  /**
   * Gets the native code value for this error code.
   *
   * @return the native code (ordinal)
   */
  public int getNativeCode() {
    return ordinal();
  }

  /**
   * Creates an error code from a native code.
   *
   * @param code the native code
   * @return the corresponding error code
   * @throws IllegalArgumentException if the code is invalid
   */
  public static NnErrorCode fromNativeCode(final int code) {
    final NnErrorCode[] values = values();
    if (code < 0 || code >= values.length) {
      return UNKNOWN;
    }
    return values[code];
  }

  @Override
  public String toString() {
    return wasiName + ": " + description;
  }
}
