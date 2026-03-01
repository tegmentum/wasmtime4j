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
package ai.tegmentum.wasmtime4j.config;

/**
 * Configuration for record/replay execution tracing.
 *
 * <p>When recording or replaying are enabled, the engine captures or replays execution traces for
 * deterministic debugging. This requires NaN canonicalization and deterministic relaxed SIMD to be
 * enabled in the engine configuration.
 *
 * <p>Record/replay is useful for debugging non-deterministic behavior in WebAssembly programs by
 * recording an execution trace that can be replayed exactly later.
 *
 * @since 1.1.0
 */
public enum RRConfig {

  /** No record/replay is enabled (default). */
  NONE("none"),

  /** Execution recording is enabled. Stores will record execution traces. */
  RECORDING("recording"),

  /** Execution replaying is enabled. Stores will replay recorded execution traces. */
  REPLAYING("replaying");

  private final String rustName;

  RRConfig(final String rustName) {
    this.rustName = rustName;
  }

  /**
   * Gets the Rust-side configuration name for this RR config.
   *
   * @return the Rust configuration string
   */
  public String getRustName() {
    return rustName;
  }

  /**
   * Parses an RR config from its Rust configuration string.
   *
   * @param name the Rust configuration name
   * @return the corresponding RRConfig
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static RRConfig fromString(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("RRConfig name cannot be null");
    }
    for (final RRConfig config : values()) {
      if (config.rustName.equals(name)) {
        return config;
      }
    }
    throw new IllegalArgumentException("Unknown RRConfig: " + name);
  }
}
