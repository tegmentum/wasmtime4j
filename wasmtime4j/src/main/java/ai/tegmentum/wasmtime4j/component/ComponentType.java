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

package ai.tegmentum.wasmtime4j.component;

/**
 * Represents a Component Model type.
 *
 * <p>The Component Model defines a rich type system that extends beyond basic WebAssembly types.
 * This enum represents all possible type kinds in the Component Model.
 *
 * @since 1.0.0
 */
public enum ComponentType {
  /** Boolean type. */
  BOOL,
  /** Signed 8-bit integer. */
  S8,
  /** Signed 16-bit integer. */
  S16,
  /** Signed 32-bit integer. */
  S32,
  /** Signed 64-bit integer. */
  S64,
  /** Unsigned 8-bit integer. */
  U8,
  /** Unsigned 16-bit integer. */
  U16,
  /** Unsigned 32-bit integer. */
  U32,
  /** Unsigned 64-bit integer. */
  U64,
  /** 32-bit floating point. */
  F32,
  /** 64-bit floating point. */
  F64,
  /** Unicode character (Unicode scalar value). */
  CHAR,
  /** UTF-8 encoded string. */
  STRING,
  /** Homogeneous list of elements. */
  LIST,
  /** Named fields with heterogeneous types. */
  RECORD,
  /** Anonymous sequence of heterogeneous types. */
  TUPLE,
  /** Tagged union with optional payloads. */
  VARIANT,
  /** Tagged union without payloads (like Rust enum). */
  ENUM,
  /** Optional value (some or none). */
  OPTION,
  /** Result type (ok or err). */
  RESULT,
  /** Bit flags. */
  FLAGS,
  /** Owned resource handle. */
  OWN,
  /** Borrowed resource handle. */
  BORROW,
  /** Future handle (component-model-async). */
  FUTURE,
  /** Stream handle (component-model-async). */
  STREAM,
  /** Error context handle (component-model-async). */
  ERROR_CONTEXT;

  /**
   * Checks if this type is a primitive type.
   *
   * @return true if this is a primitive type
   */
  public boolean isPrimitive() {
    return this == BOOL
        || this == S8
        || this == S16
        || this == S32
        || this == S64
        || this == U8
        || this == U16
        || this == U32
        || this == U64
        || this == F32
        || this == F64
        || this == CHAR
        || this == STRING;
  }

  /**
   * Checks if this type is an integer type.
   *
   * @return true if this is an integer type
   */
  public boolean isInteger() {
    return this == S8
        || this == S16
        || this == S32
        || this == S64
        || this == U8
        || this == U16
        || this == U32
        || this == U64;
  }

  /**
   * Checks if this type is a signed integer type.
   *
   * @return true if this is a signed integer type
   */
  public boolean isSigned() {
    return this == S8 || this == S16 || this == S32 || this == S64;
  }

  /**
   * Checks if this type is an unsigned integer type.
   *
   * @return true if this is an unsigned integer type
   */
  public boolean isUnsigned() {
    return this == U8 || this == U16 || this == U32 || this == U64;
  }

  /**
   * Checks if this type is a floating point type.
   *
   * @return true if this is a floating point type
   */
  public boolean isFloat() {
    return this == F32 || this == F64;
  }

  /**
   * Checks if this type is a compound type.
   *
   * @return true if this is a compound type
   */
  public boolean isCompound() {
    return this == LIST
        || this == RECORD
        || this == TUPLE
        || this == VARIANT
        || this == ENUM
        || this == OPTION
        || this == RESULT
        || this == FLAGS;
  }

  /**
   * Checks if this type is a resource handle type.
   *
   * @return true if this is a resource handle type
   */
  public boolean isResource() {
    return this == OWN || this == BORROW;
  }

  /**
   * Checks if this type is an async handle type (component-model-async).
   *
   * @return true if this is a future, stream, or error context type
   */
  public boolean isAsync() {
    return this == FUTURE || this == STREAM || this == ERROR_CONTEXT;
  }
}
