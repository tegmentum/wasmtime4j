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

package ai.tegmentum.wasmtime4j.type;

/**
 * Enumeration of external value types in WebAssembly.
 *
 * <p>WebAssembly defines several kinds of external values that can be imported or exported:
 *
 * <ul>
 *   <li>{@link #FUNC} - Functions (callable code)
 *   <li>{@link #TABLE} - Tables (arrays of references)
 *   <li>{@link #MEMORY} - Linear memories (byte arrays)
 *   <li>{@link #GLOBAL} - Globals (typed mutable/immutable values)
 *   <li>{@link #TAG} - Tags (exception handling)
 *   <li>{@link #SHARED_MEMORY} - Shared linear memories (thread-safe byte arrays)
 * </ul>
 *
 * @since 1.0.0
 */
public enum ExternType {
  /**
   * Function extern type.
   *
   * <p>Represents a WebAssembly function that can be called. Functions have a type signature
   * defining their parameter and result types.
   */
  FUNC(0),

  /**
   * Table extern type.
   *
   * <p>Represents a WebAssembly table, which is an array of reference values. Tables are used for
   * indirect function calls and storing opaque references.
   */
  TABLE(1),

  /**
   * Memory extern type.
   *
   * <p>Represents a WebAssembly linear memory, which is a resizable byte array. Memories are used
   * for storing and accessing data.
   */
  MEMORY(2),

  /**
   * Global extern type.
   *
   * <p>Represents a WebAssembly global variable. Globals can be mutable or immutable and have a
   * specific value type.
   */
  GLOBAL(3),

  /**
   * Tag extern type.
   *
   * <p>Represents a WebAssembly tag used in the exception handling proposal. Tags identify
   * exception types for throw and catch operations.
   *
   * @since 1.1.0
   */
  TAG(4),

  /**
   * Shared memory extern type.
   *
   * <p>Represents a WebAssembly shared linear memory that can be accessed concurrently from
   * multiple threads. Requires the engine to be configured with {@code wasmThreads(true)}.
   *
   * @since 1.1.0
   */
  SHARED_MEMORY(5);

  private final int code;

  ExternType(final int code) {
    this.code = code;
  }

  /**
   * Gets the numeric code for this extern type.
   *
   * @return the numeric code
   */
  public int getCode() {
    return code;
  }

  /**
   * Gets the ExternType for a numeric code.
   *
   * @param code the numeric code
   * @return the corresponding ExternType
   * @throws IllegalArgumentException if the code is invalid
   */
  public static ExternType fromCode(final int code) {
    switch (code) {
      case 0:
        return FUNC;
      case 1:
        return TABLE;
      case 2:
        return MEMORY;
      case 3:
        return GLOBAL;
      case 4:
        return TAG;
      case 5:
        return SHARED_MEMORY;
      default:
        throw new IllegalArgumentException("Unknown extern type code: " + code);
    }
  }
}
