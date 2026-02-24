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
 * Represents the heap types available in WebAssembly with GC support.
 *
 * <p>Heap types classify the kinds of references that can be stored in WebAssembly tables, passed
 * as function parameters, or returned from functions. The WebAssembly GC proposal introduces a
 * hierarchy of heap types.
 *
 * <p>The type hierarchy is:
 *
 * <pre>
 *                    any
 *                   /   \
 *                 eq     extern
 *              / | | \      |
 *           i31 struct array noextern
 *             |
 *           none
 *
 *   func    exn    cont
 *     |      |       |
 *   nofunc  noexn  nocont
 * </pre>
 *
 * @since 1.0.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "DB_DUPLICATE_SWITCH_CLAUSES",
    justification =
        "Multiple heap types intentionally share the same supertype check logic"
            + " as per WebAssembly GC type hierarchy specification")
public enum HeapType {

  /**
   * The any heap type - top type for all internal GC references.
   *
   * <p>All other GC heap types (except extern) are subtypes of any.
   */
  ANY("any"),

  /**
   * The eq heap type - references that support equality testing.
   *
   * <p>Subtypes include i31, struct, and array.
   */
  EQ("eq"),

  /**
   * The i31 heap type - immediate 31-bit integers.
   *
   * <p>These are efficient scalar values stored inline in references.
   */
  I31("i31"),

  /**
   * The struct heap type - references to struct instances.
   *
   * <p>Structs are composite types with named/indexed fields.
   */
  STRUCT("struct"),

  /**
   * The array heap type - references to array instances.
   *
   * <p>Arrays are homogeneous sequences of elements.
   */
  ARRAY("array"),

  /**
   * The func heap type - references to functions.
   *
   * <p>Function references can be called indirectly.
   */
  FUNC("func"),

  /**
   * The nofunc heap type - bottom type for function references.
   *
   * <p>Only null is a valid nofunc value.
   */
  NOFUNC("nofunc"),

  /**
   * The extern heap type - external references from the host.
   *
   * <p>ExternRef values are opaque to WebAssembly code.
   */
  EXTERN("extern"),

  /**
   * The noextern heap type - bottom type for external references.
   *
   * <p>Only null is a valid noextern value.
   */
  NOEXTERN("noextern"),

  /**
   * The exn heap type - references to exceptions.
   *
   * <p>Exception references are used by the exception handling proposal.
   */
  EXN("exn"),

  /**
   * The noexn heap type - bottom type for exception references.
   *
   * <p>Only null is a valid noexn value.
   */
  NOEXN("noexn"),

  /**
   * The cont heap type - references to continuations.
   *
   * <p>Continuation references are used by the stack switching proposal.
   */
  CONT("cont"),

  /**
   * The nocont heap type - bottom type for continuation references.
   *
   * <p>Only null is a valid nocont value.
   */
  NOCONT("nocont"),

  /**
   * The none heap type - bottom type for the eq hierarchy.
   *
   * <p>Only null is a valid none value.
   */
  NONE("none"),

  /**
   * A concrete type index referencing a defined type.
   *
   * <p>This represents a reference to a specific struct, array, or function type defined in the
   * module's type section.
   */
  CONCRETE("concrete");

  private final String wasmName;

  HeapType(final String wasmName) {
    this.wasmName = wasmName;
  }

  /**
   * Gets the WebAssembly text format name for this heap type.
   *
   * @return the WASM name
   */
  public String getWasmName() {
    return wasmName;
  }

  /**
   * Checks if this heap type is a subtype of another.
   *
   * @param supertype the potential supertype
   * @return true if this is a subtype of supertype
   */
  public boolean isSubtypeOf(final HeapType supertype) {
    if (this == supertype) {
      return true;
    }

    switch (this) {
      case ANY:
        return supertype == ANY;
      case EQ:
        return supertype == ANY || supertype == EQ;
      case I31:
      case STRUCT:
      case ARRAY:
        return supertype == ANY || supertype == EQ;
      case NONE:
        return supertype == ANY
            || supertype == EQ
            || supertype == I31
            || supertype == STRUCT
            || supertype == ARRAY
            || supertype == NONE;
      case FUNC:
        return supertype == FUNC;
      case NOFUNC:
        return supertype == FUNC || supertype == NOFUNC;
      case EXTERN:
        return supertype == EXTERN;
      case NOEXTERN:
        return supertype == EXTERN || supertype == NOEXTERN;
      case EXN:
        return supertype == EXN;
      case NOEXN:
        return supertype == EXN || supertype == NOEXN;
      case CONT:
        return supertype == CONT;
      case NOCONT:
        return supertype == CONT || supertype == NOCONT;
      case CONCRETE:
        // Concrete types need external type information to determine subtyping
        return false;
      default:
        return false;
    }
  }

  /**
   * Checks if this heap type is an abstract type (not concrete).
   *
   * @return true if this is an abstract heap type
   */
  public boolean isAbstract() {
    return this != CONCRETE;
  }

  /**
   * Checks if this heap type is a top type in its hierarchy.
   *
   * <p>Top types are: {@code any}, {@code func}, {@code extern}, {@code exn}, {@code cont}.
   *
   * @return true if this is a top type
   */
  public boolean isTop() {
    return this == ANY || this == FUNC || this == EXTERN || this == EXN || this == CONT;
  }

  /**
   * Gets the top type for this heap type's hierarchy.
   *
   * @return the corresponding top type
   */
  public HeapType getTopType() {
    if (this == EXTERN || this == NOEXTERN) {
      return EXTERN;
    } else if (isSubtypeOf(FUNC)) {
      return FUNC;
    } else if (isSubtypeOf(EXN)) {
      return EXN;
    } else if (isSubtypeOf(CONT)) {
      return CONT;
    }
    // any hierarchy (any, eq, i31, struct, array, none) and concrete
    return ANY;
  }

  /**
   * Checks if this heap type is a bottom type.
   *
   * @return true if this is a bottom type (none, nofunc, noextern, noexn, nocont)
   */
  public boolean isBottom() {
    return this == NONE || this == NOFUNC || this == NOEXTERN || this == NOEXN || this == NOCONT;
  }

  /**
   * Checks if this is the {@code func} heap type.
   *
   * @return true if this is func
   */
  public boolean isFunc() {
    return this == FUNC;
  }

  /**
   * Checks if this is the {@code extern} heap type.
   *
   * @return true if this is extern
   */
  public boolean isExtern() {
    return this == EXTERN;
  }

  /**
   * Checks if this is the {@code any} heap type.
   *
   * @return true if this is any
   */
  public boolean isAny() {
    return this == ANY;
  }

  /**
   * Checks if this is the {@code eq} heap type.
   *
   * @return true if this is eq
   */
  public boolean isEq() {
    return this == EQ;
  }

  /**
   * Checks if this is the {@code i31} heap type.
   *
   * @return true if this is i31
   */
  public boolean isI31() {
    return this == I31;
  }

  /**
   * Checks if this is the {@code struct} heap type.
   *
   * @return true if this is struct
   */
  public boolean isStruct() {
    return this == STRUCT;
  }

  /**
   * Checks if this is the {@code array} heap type.
   *
   * @return true if this is array
   */
  public boolean isArray() {
    return this == ARRAY;
  }

  /**
   * Checks if this is the {@code none} heap type.
   *
   * @return true if this is none
   */
  public boolean isNone() {
    return this == NONE;
  }

  /**
   * Checks if this is the {@code nofunc} heap type.
   *
   * @return true if this is nofunc
   */
  public boolean isNoFunc() {
    return this == NOFUNC;
  }

  /**
   * Checks if this is the {@code noextern} heap type.
   *
   * @return true if this is noextern
   */
  public boolean isNoExtern() {
    return this == NOEXTERN;
  }

  /**
   * Checks if this is the {@code exn} heap type.
   *
   * @return true if this is exn
   */
  public boolean isExn() {
    return this == EXN;
  }

  /**
   * Checks if this is the {@code noexn} heap type.
   *
   * @return true if this is noexn
   */
  public boolean isNoExn() {
    return this == NOEXN;
  }

  /**
   * Checks if this is the {@code cont} heap type.
   *
   * @return true if this is cont
   */
  public boolean isCont() {
    return this == CONT;
  }

  /**
   * Checks if this is the {@code nocont} heap type.
   *
   * @return true if this is nocont
   */
  public boolean isNoCont() {
    return this == NOCONT;
  }

  /**
   * Checks if this is a concrete (user-defined) heap type.
   *
   * @return true if this is a concrete type index
   */
  public boolean isConcrete() {
    return this == CONCRETE;
  }

  /**
   * Checks if this heap type supports equality testing (ref.eq).
   *
   * @return true if this type supports equality
   */
  public boolean supportsEquality() {
    return isSubtypeOf(EQ);
  }

  /**
   * Gets the bottom type for this heap type's hierarchy.
   *
   * @return the corresponding bottom type
   */
  public HeapType getBottomType() {
    if (isSubtypeOf(EQ) || this == ANY) {
      return NONE;
    } else if (isSubtypeOf(FUNC)) {
      return NOFUNC;
    } else if (isSubtypeOf(EXTERN)) {
      return NOEXTERN;
    } else if (isSubtypeOf(EXN)) {
      return NOEXN;
    } else if (isSubtypeOf(CONT)) {
      return NOCONT;
    }
    return NONE;
  }

  /**
   * Parses a heap type from its WebAssembly text format name.
   *
   * @param wasmName the WASM name to parse
   * @return the corresponding HeapType
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static HeapType fromWasmName(final String wasmName) {
    if (wasmName == null) {
      throw new IllegalArgumentException("wasmName cannot be null");
    }
    for (HeapType type : values()) {
      if (type.wasmName.equals(wasmName)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown heap type: " + wasmName);
  }

  @Override
  public String toString() {
    return wasmName;
  }
}
