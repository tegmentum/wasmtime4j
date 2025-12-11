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

/**
 * Represents an external value that can be imported or exported by a WebAssembly module.
 *
 * <p>Extern is the unified representation for all WebAssembly external values including:
 *
 * <ul>
 *   <li>Functions (both host and wasm functions)
 *   <li>Tables
 *   <li>Memories
 *   <li>Globals
 * </ul>
 *
 * <p>This interface provides a common API for working with external values regardless of their
 * specific type. You can check the type and cast to the appropriate subtype as needed.
 *
 * @since 1.0.0
 */
public interface Extern {

  /**
   * Gets the type of this external value.
   *
   * @return the extern type
   */
  ExternType getType();

  /**
   * Checks if this extern is a function.
   *
   * @return true if this is a function extern
   */
  default boolean isFunction() {
    return getType() == ExternType.FUNC;
  }

  /**
   * Checks if this extern is a table.
   *
   * @return true if this is a table extern
   */
  default boolean isTable() {
    return getType() == ExternType.TABLE;
  }

  /**
   * Checks if this extern is a memory.
   *
   * @return true if this is a memory extern
   */
  default boolean isMemory() {
    return getType() == ExternType.MEMORY;
  }

  /**
   * Checks if this extern is a global.
   *
   * @return true if this is a global extern
   */
  default boolean isGlobal() {
    return getType() == ExternType.GLOBAL;
  }

  /**
   * Attempts to get this extern as a function.
   *
   * @return the function, or null if not a function
   */
  default WasmFunction asFunction() {
    return null;
  }

  /**
   * Attempts to get this extern as a table.
   *
   * @return the table, or null if not a table
   */
  default WasmTable asTable() {
    return null;
  }

  /**
   * Attempts to get this extern as a memory.
   *
   * @return the memory, or null if not a memory
   */
  default WasmMemory asMemory() {
    return null;
  }

  /**
   * Attempts to get this extern as a global.
   *
   * @return the global, or null if not a global
   */
  default WasmGlobal asGlobal() {
    return null;
  }
}
