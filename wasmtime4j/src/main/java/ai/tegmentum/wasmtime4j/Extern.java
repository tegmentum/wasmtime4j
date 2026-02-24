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

import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.type.ExternTypeInfo;
import ai.tegmentum.wasmtime4j.type.WasmType;

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
 *   <li>Tags (exception handling)
 *   <li>Shared Memories (thread-safe memories)
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

  /**
   * Checks if this extern is a tag.
   *
   * @return true if this is a tag extern
   * @since 1.1.0
   */
  default boolean isTag() {
    return getType() == ExternType.TAG;
  }

  /**
   * Checks if this extern is a shared memory.
   *
   * @return true if this is a shared memory extern
   * @since 1.1.0
   */
  default boolean isSharedMemory() {
    return getType() == ExternType.SHARED_MEMORY;
  }

  /**
   * Attempts to get this extern as a tag.
   *
   * @return the tag, or null if not a tag
   * @since 1.1.0
   */
  default Tag asTag() {
    return null;
  }

  /**
   * Attempts to get this extern as a shared memory.
   *
   * @return the shared memory, or null if not a shared memory
   * @since 1.1.0
   */
  default WasmMemory asSharedMemory() {
    return null;
  }

  /**
   * Gets the detailed type descriptor for this extern.
   *
   * <p>This corresponds to Wasmtime's {@code Extern::ty(store)} which returns the full type
   * information (e.g., {@link ai.tegmentum.wasmtime4j.type.FuncType} with parameter/result types,
   * {@link ai.tegmentum.wasmtime4j.type.MemoryType} with limits, etc.) rather than just the kind
   * discriminant.
   *
   * <p>The default implementation inspects the concrete value through the {@code asXxx()} methods
   * to extract the type. Runtime implementations may override for more efficient native access.
   *
   * @return the detailed type, or null if the type cannot be determined
   * @since 1.1.0
   */
  default WasmType getDetailedType() {
    return null;
  }

  /**
   * Gets the full type information for this extern as an {@link ExternTypeInfo}.
   *
   * <p>This pairs the kind discriminant ({@link ExternType}) with the detailed type data
   * ({@link WasmType}), providing type-safe access via methods like
   * {@link ExternTypeInfo#asFuncType()}, {@link ExternTypeInfo#asTableType()}, etc.
   *
   * @return the extern type info, or null if the detailed type is not available
   * @since 1.1.0
   */
  default ExternTypeInfo getExternTypeInfo() {
    final WasmType detailed = getDetailedType();
    if (detailed == null) {
      return null;
    }
    switch (getType()) {
      case FUNC:
        if (detailed instanceof ai.tegmentum.wasmtime4j.type.FuncType) {
          return ExternTypeInfo.forFunc((ai.tegmentum.wasmtime4j.type.FuncType) detailed);
        }
        break;
      case TABLE:
        if (detailed instanceof ai.tegmentum.wasmtime4j.type.TableType) {
          return ExternTypeInfo.forTable((ai.tegmentum.wasmtime4j.type.TableType) detailed);
        }
        break;
      case MEMORY:
      case SHARED_MEMORY:
        if (detailed instanceof ai.tegmentum.wasmtime4j.type.MemoryType) {
          return ExternTypeInfo.forMemory((ai.tegmentum.wasmtime4j.type.MemoryType) detailed);
        }
        break;
      case GLOBAL:
        if (detailed instanceof ai.tegmentum.wasmtime4j.type.GlobalType) {
          return ExternTypeInfo.forGlobal((ai.tegmentum.wasmtime4j.type.GlobalType) detailed);
        }
        break;
      default:
        break;
    }
    return null;
  }
}
