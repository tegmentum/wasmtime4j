/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Memory layout definitions for Wasmtime C API value structures.
 *
 * <p>This class defines memory layouts matching the Wasmtime C API data structures used for value
 * conversion. All layouts are designed for direct memory access through MemorySegment operations
 * and provide type-safe access to native structure fields.
 *
 * <p>Layout definitions follow Wasmtime C API structure alignment and padding requirements to
 * ensure binary compatibility across all supported platforms.
 */
public final class MemoryLayouts {

  // Private constructor to prevent instantiation
  private MemoryLayouts() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Layout for WebAssembly value types.
   *
   * <pre>
   * typedef enum wasm_valkind_t {
   *     WASM_I32,
   *     WASM_I64,
   *     WASM_F32,
   *     WASM_F64,
   *     WASM_ANYREF,
   *     WASM_FUNCREF
   * } wasm_valkind_t;
   * </pre>
   */
  // Value type constants
  public static final int WASM_I32 = 0;

  public static final int WASM_I64 = 1;
  public static final int WASM_F32 = 2;
  public static final int WASM_F64 = 3;
  public static final int WASM_V128 = 4;
  public static final int WASM_ANYREF = 5;
  public static final int WASM_FUNCREF = 6;

  /** V128 type size in bytes. */
  public static final int V128_BYTE_SIZE = 16;

  /**
   * Layout for WebAssembly value.
   *
   * <pre>
   * typedef union wasm_val_value {
   *     int32_t i32;
   *     int64_t i64;
   *     float f32;
   *     double f64;
   *     void* ref;
   *     uint8_t v128[16];
   * } wasm_val_value_t;
   * </pre>
   */
  public static final UnionLayout WASM_VAL_VALUE =
      MemoryLayout.unionLayout(
              ValueLayout.JAVA_INT.withName("i32"),
              ValueLayout.JAVA_LONG.withName("i64"),
              ValueLayout.JAVA_FLOAT.withName("f32"),
              ValueLayout.JAVA_DOUBLE.withName("f64"),
              ValueLayout.ADDRESS.withName("ref"),
              MemoryLayout.sequenceLayout(V128_BYTE_SIZE, ValueLayout.JAVA_BYTE).withName("v128"))
          .withName("wasm_val_value_t");

  /**
   * Layout for WebAssembly value structure.
   *
   * <pre>
   * typedef struct wasm_val {
   *     wasm_valkind_t kind;
   *     wasm_val_value_t of;
   * } wasm_val_t;
   * </pre>
   */
  public static final StructLayout WASM_VAL =
      MemoryLayout.structLayout(
              ValueLayout.JAVA_INT.withName("kind"),
              MemoryLayout.paddingLayout(4), // Alignment padding
              WASM_VAL_VALUE.withName("of"))
          .withName("wasm_val_t");

  // VarHandles for WASM_VAL
  public static final VarHandle WASM_VAL_KIND =
      WASM_VAL.varHandle(MemoryLayout.PathElement.groupElement("kind"));
  public static final VarHandle WASM_VAL_I32 =
      WASM_VAL.varHandle(
          MemoryLayout.PathElement.groupElement("of"),
          MemoryLayout.PathElement.groupElement("i32"));
  public static final VarHandle WASM_VAL_I64 =
      WASM_VAL.varHandle(
          MemoryLayout.PathElement.groupElement("of"),
          MemoryLayout.PathElement.groupElement("i64"));
  public static final VarHandle WASM_VAL_F32 =
      WASM_VAL.varHandle(
          MemoryLayout.PathElement.groupElement("of"),
          MemoryLayout.PathElement.groupElement("f32"));
  public static final VarHandle WASM_VAL_F64 =
      WASM_VAL.varHandle(
          MemoryLayout.PathElement.groupElement("of"),
          MemoryLayout.PathElement.groupElement("f64"));
  public static final VarHandle WASM_VAL_REF =
      WASM_VAL.varHandle(
          MemoryLayout.PathElement.groupElement("of"),
          MemoryLayout.PathElement.groupElement("ref"));

  /** Offset of the union 'of' field within WASM_VAL struct (kind + padding). */
  public static final long WASM_VAL_UNION_OFFSET = 8;

  /**
   * Extracts a V128 value from a WASM_VAL memory segment.
   *
   * <p>The V128 value is stored in the union field at offset 8 (after kind + padding).
   *
   * @param segment the memory segment containing the WASM_VAL struct
   * @return the V128 value as a memory segment (16 bytes)
   * @throws IllegalArgumentException if segment is null or too small
   */
  public static MemorySegment getV128Value(final MemorySegment segment) {
    if (segment == null) {
      throw new IllegalArgumentException("Memory segment cannot be null");
    }
    // WASM_VAL struct: kind (4 bytes) + padding (4 bytes) + union (at least 16 bytes for v128)
    if (segment.byteSize() < WASM_VAL_UNION_OFFSET + 16) {
      throw new IllegalArgumentException(
          "Memory segment too small for V128 value: " + segment.byteSize());
    }

    return segment.asSlice(WASM_VAL_UNION_OFFSET, 16);
  }
}
