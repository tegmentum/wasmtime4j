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
package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.GlobalType;

/**
 * Represents a WebAssembly global variable.
 *
 * <p>Globals are WebAssembly values that can be read and (if mutable) written. They provide a way
 * to share state across function calls.
 *
 * @since 1.0.0
 */
public interface WasmGlobal {

  /**
   * Gets the value of this global.
   *
   * @return the current value
   */
  WasmValue get();

  /**
   * Gets the value of this global. Alias for get() for compatibility.
   *
   * @return the current value
   */
  default WasmValue getValue() {
    return get();
  }

  /**
   * Sets the value of this global.
   *
   * @param value the new value
   * @throws UnsupportedOperationException if the global is immutable
   * @throws ai.tegmentum.wasmtime4j.exception.WasmTypeException if the value type does not match
   *     the global type
   */
  void set(final WasmValue value);

  /**
   * Gets the type of this global.
   *
   * @return the global type
   */
  WasmValueType getType();

  /**
   * Checks if this global is mutable.
   *
   * @return true if the global is mutable
   */
  boolean isMutable();

  /**
   * Gets the complete type information for this global.
   *
   * <p>The GlobalType provides full type information including both the value type and mutability.
   * This is more comprehensive than {@link #getType()} which only returns the value type.
   *
   * @return the complete global type information
   * @since 1.0.0
   */
  GlobalType getGlobalType();
}
