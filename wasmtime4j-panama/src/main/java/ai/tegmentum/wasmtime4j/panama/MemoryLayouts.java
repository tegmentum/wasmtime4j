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
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.logging.Logger;

/**
 * Memory layout definitions for all Wasmtime C API structures.
 *
 * <p>This class defines comprehensive memory layouts matching the Wasmtime C API data structures.
 * All layouts are designed for direct memory access through MemorySegment operations and provide
 * type-safe access to native structure fields.
 *
 * <p>Layout definitions follow Wasmtime C API structure alignment and padding requirements to
 * ensure binary compatibility across all supported platforms.
 */
public final class MemoryLayouts {

  private static final Logger LOGGER = Logger.getLogger(MemoryLayouts.class.getName());

  // Private constructor to prevent instantiation
  private MemoryLayouts() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  // Basic C types
  public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
  public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
  public static final ValueLayout.OfLong C_SIZE_T = ValueLayout.JAVA_LONG;
  public static final ValueLayout C_POINTER = ValueLayout.ADDRESS;
  public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;

  // Wasmtime error types
  public static final ValueLayout.OfInt WASM_ERROR_CODE = ValueLayout.JAVA_INT;

  /**
   * Layout for Wasmtime error structure.
   *
   * <pre>
   * typedef struct wasmtime_error {
   *     int code;
   *     const char* message;
   *     size_t message_len;
   * } wasmtime_error_t;
   * </pre>
   */
  public static final StructLayout WASMTIME_ERROR =
      MemoryLayout.structLayout(
              WASM_ERROR_CODE.withName("code"),
              C_POINTER.withName("message"),
              C_SIZE_T.withName("message_len"))
          .withName("wasmtime_error_t");

  // VarHandles for WASMTIME_ERROR
  public static final VarHandle WASMTIME_ERROR_CODE =
      WASMTIME_ERROR.varHandle(MemoryLayout.PathElement.groupElement("code"));
  public static final VarHandle WASMTIME_ERROR_MESSAGE =
      WASMTIME_ERROR.varHandle(MemoryLayout.PathElement.groupElement("message"));
  public static final VarHandle WASMTIME_ERROR_MESSAGE_LEN =
      WASMTIME_ERROR.varHandle(MemoryLayout.PathElement.groupElement("message_len"));

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
  public static final ValueLayout.OfInt WASM_VALKIND = ValueLayout.JAVA_INT;

  // Value type constants
  public static final int WASM_I32 = 0;
  public static final int WASM_I64 = 1;
  public static final int WASM_F32 = 2;
  public static final int WASM_F64 = 3;
  public static final int WASM_ANYREF = 4;
  public static final int WASM_FUNCREF = 5;

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
   * } wasm_val_value_t;
   * </pre>
   */
  public static final UnionLayout WASM_VAL_VALUE =
      MemoryLayout.unionLayout(
              ValueLayout.JAVA_INT.withName("i32"),
              ValueLayout.JAVA_LONG.withName("i64"),
              ValueLayout.JAVA_FLOAT.withName("f32"),
              ValueLayout.JAVA_DOUBLE.withName("f64"),
              C_POINTER.withName("ref"))
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
              WASM_VALKIND.withName("kind"),
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

  /**
   * Layout for WebAssembly byte vector (used for WASM bytecode).
   *
   * <pre>
   * typedef struct wasm_byte_vec {
   *     size_t size;
   *     uint8_t* data;
   * } wasm_byte_vec_t;
   * </pre>
   */
  public static final StructLayout WASM_BYTE_VEC =
      MemoryLayout.structLayout(C_SIZE_T.withName("size"), C_POINTER.withName("data"))
          .withName("wasm_byte_vec_t");

  // VarHandles for WASM_BYTE_VEC
  public static final VarHandle WASM_BYTE_VEC_SIZE =
      WASM_BYTE_VEC.varHandle(MemoryLayout.PathElement.groupElement("size"));
  public static final VarHandle WASM_BYTE_VEC_DATA =
      WASM_BYTE_VEC.varHandle(MemoryLayout.PathElement.groupElement("data"));

  /**
   * Layout for WebAssembly value vector.
   *
   * <pre>
   * typedef struct wasm_val_vec {
   *     size_t size;
   *     wasm_val_t* data;
   * } wasm_val_vec_t;
   * </pre>
   */
  public static final StructLayout WASM_VAL_VEC =
      MemoryLayout.structLayout(C_SIZE_T.withName("size"), C_POINTER.withName("data"))
          .withName("wasm_val_vec_t");

  // VarHandles for WASM_VAL_VEC
  public static final VarHandle WASM_VAL_VEC_SIZE =
      WASM_VAL_VEC.varHandle(MemoryLayout.PathElement.groupElement("size"));
  public static final VarHandle WASM_VAL_VEC_DATA =
      WASM_VAL_VEC.varHandle(MemoryLayout.PathElement.groupElement("data"));

  /**
   * Layout for WebAssembly function type.
   *
   * <pre>
   * typedef struct wasm_functype {
   *     wasm_val_vec_t params;
   *     wasm_val_vec_t results;
   * } wasm_functype_t;
   * </pre>
   */
  public static final StructLayout WASM_FUNCTYPE =
      MemoryLayout.structLayout(WASM_VAL_VEC.withName("params"), WASM_VAL_VEC.withName("results"))
          .withName("wasm_functype_t");

  // VarHandles for WASM_FUNCTYPE
  public static final VarHandle WASM_FUNCTYPE_PARAMS =
      WASM_FUNCTYPE.varHandle(MemoryLayout.PathElement.groupElement("params"));
  public static final VarHandle WASM_FUNCTYPE_RESULTS =
      WASM_FUNCTYPE.varHandle(MemoryLayout.PathElement.groupElement("results"));

  /**
   * Layout for WebAssembly global type.
   *
   * <pre>
   * typedef struct wasm_globaltype {
   *     wasm_valkind_t content;
   *     wasm_mutability_t mutability;
   * } wasm_globaltype_t;
   * </pre>
   */
  public static final StructLayout WASM_GLOBALTYPE =
      MemoryLayout.structLayout(
              WASM_VALKIND.withName("content"), ValueLayout.JAVA_INT.withName("mutability"))
          .withName("wasm_globaltype_t");

  // VarHandles for WASM_GLOBALTYPE
  public static final VarHandle WASM_GLOBALTYPE_CONTENT =
      WASM_GLOBALTYPE.varHandle(MemoryLayout.PathElement.groupElement("content"));
  public static final VarHandle WASM_GLOBALTYPE_MUTABILITY =
      WASM_GLOBALTYPE.varHandle(MemoryLayout.PathElement.groupElement("mutability"));

  /**
   * Layout for WebAssembly memory limits.
   *
   * <pre>
   * typedef struct wasm_limits {
   *     uint32_t min;
   *     uint32_t max;
   *     bool has_max;
   * } wasm_limits_t;
   * </pre>
   */
  public static final StructLayout WASM_LIMITS =
      MemoryLayout.structLayout(
              ValueLayout.JAVA_INT.withName("min"),
              ValueLayout.JAVA_INT.withName("max"),
              C_BOOL.withName("has_max"),
              MemoryLayout.paddingLayout(3) // Alignment padding
              )
          .withName("wasm_limits_t");

  // VarHandles for WASM_LIMITS
  public static final VarHandle WASM_LIMITS_MIN =
      WASM_LIMITS.varHandle(MemoryLayout.PathElement.groupElement("min"));
  public static final VarHandle WASM_LIMITS_MAX =
      WASM_LIMITS.varHandle(MemoryLayout.PathElement.groupElement("max"));
  public static final VarHandle WASM_LIMITS_HAS_MAX =
      WASM_LIMITS.varHandle(MemoryLayout.PathElement.groupElement("has_max"));

  /**
   * Layout for WebAssembly memory type.
   *
   * <pre>
   * typedef struct wasm_memorytype {
   *     wasm_limits_t limits;
   * } wasm_memorytype_t;
   * </pre>
   */
  public static final StructLayout WASM_MEMORYTYPE =
      MemoryLayout.structLayout(WASM_LIMITS.withName("limits")).withName("wasm_memorytype_t");

  // VarHandles for WASM_MEMORYTYPE
  public static final VarHandle WASM_MEMORYTYPE_LIMITS =
      WASM_MEMORYTYPE.varHandle(MemoryLayout.PathElement.groupElement("limits"));

  /**
   * Layout for WebAssembly table type.
   *
   * <pre>
   * typedef struct wasm_tabletype {
   *     wasm_valkind_t element;
   *     wasm_limits_t limits;
   * } wasm_tabletype_t;
   * </pre>
   */
  public static final StructLayout WASM_TABLETYPE =
      MemoryLayout.structLayout(
              WASM_VALKIND.withName("element"),
              MemoryLayout.paddingLayout(4), // Alignment padding
              WASM_LIMITS.withName("limits"))
          .withName("wasm_tabletype_t");

  // VarHandles for WASM_TABLETYPE
  public static final VarHandle WASM_TABLETYPE_ELEMENT =
      WASM_TABLETYPE.varHandle(MemoryLayout.PathElement.groupElement("element"));
  public static final VarHandle WASM_TABLETYPE_LIMITS =
      WASM_TABLETYPE.varHandle(MemoryLayout.PathElement.groupElement("limits"));

  /**
   * Layout for WebAssembly import type.
   *
   * <pre>
   * typedef struct wasm_importtype {
   *     wasm_byte_vec_t module;
   *     wasm_byte_vec_t name;
   *     wasm_externtype_t* type;
   * } wasm_importtype_t;
   * </pre>
   */
  public static final StructLayout WASM_IMPORTTYPE =
      MemoryLayout.structLayout(
              WASM_BYTE_VEC.withName("module"),
              WASM_BYTE_VEC.withName("name"),
              C_POINTER.withName("type"))
          .withName("wasm_importtype_t");

  // VarHandles for WASM_IMPORTTYPE
  public static final VarHandle WASM_IMPORTTYPE_MODULE =
      WASM_IMPORTTYPE.varHandle(MemoryLayout.PathElement.groupElement("module"));
  public static final VarHandle WASM_IMPORTTYPE_NAME =
      WASM_IMPORTTYPE.varHandle(MemoryLayout.PathElement.groupElement("name"));
  public static final VarHandle WASM_IMPORTTYPE_TYPE =
      WASM_IMPORTTYPE.varHandle(MemoryLayout.PathElement.groupElement("type"));

  /**
   * Layout for WebAssembly export type.
   *
   * <pre>
   * typedef struct wasm_exporttype {
   *     wasm_byte_vec_t name;
   *     wasm_externtype_t* type;
   * } wasm_exporttype_t;
   * </pre>
   */
  public static final StructLayout WASM_EXPORTTYPE =
      MemoryLayout.structLayout(WASM_BYTE_VEC.withName("name"), C_POINTER.withName("type"))
          .withName("wasm_exporttype_t");

  // VarHandles for WASM_EXPORTTYPE
  public static final VarHandle WASM_EXPORTTYPE_NAME =
      WASM_EXPORTTYPE.varHandle(MemoryLayout.PathElement.groupElement("name"));
  public static final VarHandle WASM_EXPORTTYPE_TYPE =
      WASM_EXPORTTYPE.varHandle(MemoryLayout.PathElement.groupElement("type"));

  /**
   * Layout for Wasmtime configuration structure.
   *
   * <pre>
   * typedef struct wasmtime_config {
   *     bool debug_info;
   *     bool consume_fuel;
   *     bool epoch_interruption;
   *     bool max_wasm_stack;
   *     bool parallel_compilation;
   * } wasmtime_config_t;
   * </pre>
   */
  public static final StructLayout WASMTIME_CONFIG =
      MemoryLayout.structLayout(
              C_BOOL.withName("debug_info"),
              C_BOOL.withName("consume_fuel"),
              C_BOOL.withName("epoch_interruption"),
              C_BOOL.withName("max_wasm_stack"),
              C_BOOL.withName("parallel_compilation"),
              MemoryLayout.paddingLayout(3) // Alignment padding
              )
          .withName("wasmtime_config_t");

  // VarHandles for WASMTIME_CONFIG
  public static final VarHandle WASMTIME_CONFIG_DEBUG_INFO =
      WASMTIME_CONFIG.varHandle(MemoryLayout.PathElement.groupElement("debug_info"));
  public static final VarHandle WASMTIME_CONFIG_CONSUME_FUEL =
      WASMTIME_CONFIG.varHandle(MemoryLayout.PathElement.groupElement("consume_fuel"));
  public static final VarHandle WASMTIME_CONFIG_EPOCH_INTERRUPTION =
      WASMTIME_CONFIG.varHandle(MemoryLayout.PathElement.groupElement("epoch_interruption"));
  public static final VarHandle WASMTIME_CONFIG_MAX_WASM_STACK =
      WASMTIME_CONFIG.varHandle(MemoryLayout.PathElement.groupElement("max_wasm_stack"));
  public static final VarHandle WASMTIME_CONFIG_PARALLEL_COMPILATION =
      WASMTIME_CONFIG.varHandle(MemoryLayout.PathElement.groupElement("parallel_compilation"));

  // Mutability constants
  public static final int WASM_CONST = 0;
  public static final int WASM_VAR = 1;

  // External type kinds
  public static final int WASM_EXTERN_FUNC = 0;
  public static final int WASM_EXTERN_GLOBAL = 1;
  public static final int WASM_EXTERN_TABLE = 2;
  public static final int WASM_EXTERN_MEMORY = 3;

  /**
   * Gets the size of a WebAssembly value in bytes based on its kind.
   *
   * @param kind the value kind
   * @return size in bytes
   */
  public static int getValueSize(final int kind) {
    return switch (kind) {
      case WASM_I32, WASM_F32 -> 4;
      case WASM_I64, WASM_F64 -> 8;
      case WASM_ANYREF, WASM_FUNCREF -> (int) C_POINTER.byteSize();
      default -> throw new IllegalArgumentException("Unknown value kind: " + kind);
    };
  }

  /**
   * Checks if a value kind represents a reference type.
   *
   * @param kind the value kind
   * @return true if reference type, false otherwise
   */
  public static boolean isReferenceType(final int kind) {
    return kind == WASM_ANYREF || kind == WASM_FUNCREF;
  }

  /**
   * Checks if a value kind represents a numeric type.
   *
   * @param kind the value kind
   * @return true if numeric type, false otherwise
   */
  public static boolean isNumericType(final int kind) {
    return kind == WASM_I32 || kind == WASM_I64 || kind == WASM_F32 || kind == WASM_F64;
  }

  /**
   * Gets the string representation of a value kind.
   *
   * @param kind the value kind
   * @return string representation
   */
  public static String valueKindToString(final int kind) {
    return switch (kind) {
      case WASM_I32 -> "i32";
      case WASM_I64 -> "i64";
      case WASM_F32 -> "f32";
      case WASM_F64 -> "f64";
      case WASM_ANYREF -> "anyref";
      case WASM_FUNCREF -> "funcref";
      default -> "unknown(" + kind + ")";
    };
  }

  // External type constants for Wasmtime
  public static final int WASMTIME_EXTERN_FUNC = 0;
  public static final int WASMTIME_EXTERN_GLOBAL = 1;
  public static final int WASMTIME_EXTERN_MEMORY = 2;
  public static final int WASMTIME_EXTERN_TABLE = 3;

  /**
   * Layout for Wasmtime export structure.
   *
   * <pre>
   * typedef struct wasmtime_extern {
   *     uint8_t kind;
   *     // padding for alignment
   *     union {
   *         void* func;
   *         void* global;
   *         void* table;
   *         void* memory;
   *     } of;
   * } wasmtime_extern_t;
   * </pre>
   */
  public static final StructLayout WASMTIME_EXPORT_LAYOUT =
      MemoryLayout.structLayout(
              ValueLayout.JAVA_BYTE.withName("kind"),
              MemoryLayout.paddingLayout(7), // Padding for 8-byte alignment
              ValueLayout.ADDRESS.withName("of") // Union represented as single pointer
              )
          .withName("wasmtime_extern_t");

  // VarHandles for WASMTIME_EXPORT_LAYOUT
  public static final VarHandle WASMTIME_EXPORT_KIND =
      WASMTIME_EXPORT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("kind"));
  public static final VarHandle WASMTIME_EXPORT_OF =
      WASMTIME_EXPORT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("of"));

  /**
   * Gets the string representation of an external type kind.
   *
   * @param kind the external type kind
   * @return string representation
   */
  public static String externKindToString(final int kind) {
    return switch (kind) {
      case WASM_EXTERN_FUNC -> "function";
      case WASM_EXTERN_GLOBAL -> "global";
      case WASM_EXTERN_TABLE -> "table";
      case WASM_EXTERN_MEMORY -> "memory";
      default -> "unknown(" + kind + ")";
    };
  }

  static {
    LOGGER.fine("Initialized Wasmtime memory layouts");
  }
}
