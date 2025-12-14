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
 * Configuration for the level of detail included in WebAssembly backtraces.
 *
 * <p>When a WebAssembly trap occurs, wasmtime can generate backtraces with varying levels of
 * detail. This configuration controls how much information is captured, which affects both the
 * usefulness of error messages and the runtime overhead.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EngineConfig config = new EngineConfig()
 *     .wasmBacktraceDetails(WasmBacktraceDetails.ENABLE);
 * Engine engine = Engine.create(config);
 * }</pre>
 *
 * @since 1.1.0
 */
public enum WasmBacktraceDetails {

  /**
   * Disable backtrace collection entirely.
   *
   * <p>No backtrace information will be captured when a trap occurs. This provides the best
   * performance but the least debugging information.
   *
   * <p>Use this setting when:
   *
   * <ul>
   *   <li>Maximum performance is critical
   *   <li>Debugging information is not needed
   *   <li>Running trusted code that shouldn't trap
   * </ul>
   */
  DISABLE(0),

  /**
   * Enable basic backtrace collection.
   *
   * <p>Backtraces will include function names and module information when available. This provides
   * a good balance between debugging capability and performance.
   *
   * <p>Use this setting for:
   *
   * <ul>
   *   <li>Development and debugging
   *   <li>Production environments where debugging is sometimes needed
   * </ul>
   */
  ENABLE(1),

  /**
   * Use environment-based configuration.
   *
   * <p>The backtrace detail level is determined by the WASMTIME_BACKTRACE_DETAILS environment
   * variable. If not set, defaults to ENABLE.
   *
   * <p>Valid environment values:
   *
   * <ul>
   *   <li>"0" or "disable" - equivalent to DISABLE
   *   <li>"1" or "enable" - equivalent to ENABLE
   * </ul>
   */
  ENVIRONMENT(2);

  private final int value;

  WasmBacktraceDetails(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value for this setting.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets the setting from its numeric value.
   *
   * @param value the numeric value
   * @return the corresponding setting
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static WasmBacktraceDetails fromValue(final int value) {
    for (WasmBacktraceDetails details : values()) {
      if (details.value == value) {
        return details;
      }
    }
    throw new IllegalArgumentException("Unknown backtrace details value: " + value);
  }

  /**
   * Checks if backtraces are enabled for this setting.
   *
   * <p>For ENVIRONMENT setting, this returns true as backtrace may be enabled.
   *
   * @return true if backtraces may be collected
   */
  public boolean isEnabled() {
    return this != DISABLE;
  }
}
