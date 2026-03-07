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
 * Tri-state enum for configuration options that support auto-detection.
 *
 * <p>This corresponds to Wasmtime's {@code wasmtime::Enabled} enum, which allows configuration
 * options to be explicitly enabled, explicitly disabled, or set to auto-detect based on other
 * configuration settings.
 *
 * <p>Used by configuration options such as GC support, shared memory, memory protection keys, and
 * pagemap scanning.
 *
 * @since 1.1.0
 */
public enum Enabled {
  /**
   * Automatic detection based on other configuration.
   *
   * <p>The runtime will determine the appropriate setting based on related configuration options
   * and platform capabilities.
   */
  AUTO,

  /** Explicitly enabled. */
  YES,

  /** Explicitly disabled. */
  NO;

  /**
   * Converts this value to a JSON-compatible string representation.
   *
   * @return "auto", "yes", or "no"
   */
  public String toJsonValue() {
    switch (this) {
      case AUTO:
        return "auto";
      case YES:
        return "yes";
      case NO:
        return "no";
      default:
        return "auto";
    }
  }

  /**
   * Converts a boolean to an Enabled value.
   *
   * @param value the boolean value
   * @return {@link #YES} if true, {@link #NO} if false
   */
  public static Enabled fromBoolean(final boolean value) {
    return value ? YES : NO;
  }

  /**
   * Parses a string to an Enabled value.
   *
   * @param value the string value ("auto", "yes", "no", "true", "false")
   * @return the corresponding Enabled value
   * @throws IllegalArgumentException if the string is not a valid Enabled value
   */
  public static Enabled fromString(final String value) {
    if (value == null) {
      throw new IllegalArgumentException("Enabled value cannot be null");
    }
    if ("auto".equalsIgnoreCase(value)) {
      return AUTO;
    } else if ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) {
      return YES;
    } else if ("no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      return NO;
    } else {
      throw new IllegalArgumentException("Unknown Enabled value: " + value);
    }
  }
}
