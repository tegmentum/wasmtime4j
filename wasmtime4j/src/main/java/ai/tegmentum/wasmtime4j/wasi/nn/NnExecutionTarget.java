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
 * Represents the execution targets (hardware accelerators) supported by WASI-NN.
 *
 * <p>These targets correspond to the execution_target enum in the WASI-NN specification. The
 * availability of specific targets depends on the host hardware and runtime configuration.
 *
 * @since 1.0.0
 */
public enum NnExecutionTarget {

  /** CPU execution (always available). */
  CPU("cpu", "CPU"),

  /** GPU execution (requires GPU hardware and drivers). */
  GPU("gpu", "GPU"),

  /** TPU execution (requires TPU hardware). */
  TPU("tpu", "TPU");

  private final String wasiName;
  private final String displayName;

  NnExecutionTarget(final String wasiName, final String displayName) {
    this.wasiName = wasiName;
    this.displayName = displayName;
  }

  /**
   * Gets the WASI-NN specification name for this execution target.
   *
   * @return the WASI-NN target name
   */
  public String getWasiName() {
    return wasiName;
  }

  /**
   * Gets a human-readable display name for this execution target.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Parses an execution target from its WASI-NN name.
   *
   * @param wasiName the WASI-NN target name
   * @return the corresponding execution target
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static NnExecutionTarget fromWasiName(final String wasiName) {
    for (final NnExecutionTarget target : values()) {
      if (target.wasiName.equals(wasiName)) {
        return target;
      }
    }
    throw new IllegalArgumentException("Unknown execution target: " + wasiName);
  }

  /**
   * Gets the native code value for this execution target.
   *
   * @return the native code (ordinal)
   */
  public int getNativeCode() {
    return ordinal();
  }

  /**
   * Creates an execution target from a native code.
   *
   * @param code the native code
   * @return the corresponding execution target
   * @throws IllegalArgumentException if the code is invalid
   */
  public static NnExecutionTarget fromNativeCode(final int code) {
    final NnExecutionTarget[] values = values();
    if (code < 0 || code >= values.length) {
      throw new IllegalArgumentException("Invalid execution target code: " + code);
    }
    return values[code];
  }

  @Override
  public String toString() {
    return displayName;
  }
}
