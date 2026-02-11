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

package ai.tegmentum.wasmtime4j.ref;

import ai.tegmentum.wasmtime4j.WasmValueType;

/**
 * Represents the none heap type - the bottom type in the any/eq type hierarchy.
 *
 * <p>NoneRef is an uninhabited type, meaning no values can have this type. It serves as the bottom
 * type in the GC reference type hierarchy, used for type system completeness in the WebAssembly GC
 * proposal.
 *
 * <p>This type is primarily used in type checking and validation rather than for storing actual
 * values. It is a subtype of all GC heap types (struct, array, i31, eq, any).
 *
 * <p>Type hierarchy:
 *
 * <pre>
 *        any
 *         |
 *         eq
 *       / | \
 *    i31 struct array
 *       \  |  /
 *        none (bottom)
 * </pre>
 *
 * @since 1.1.0
 */
public final class NoneRef implements HeapType {

  /** Singleton instance of NoneRef. */
  public static final NoneRef INSTANCE = new NoneRef();

  private NoneRef() {
    // Private constructor for singleton
  }

  /**
   * Gets the singleton instance of NoneRef.
   *
   * @return the NoneRef instance
   */
  public static NoneRef getInstance() {
    return INSTANCE;
  }

  @Override
  public WasmValueType getValueType() {
    return WasmValueType.ANYREF;
  }

  @Override
  public boolean isNullable() {
    return true; // Bottom types are always nullable
  }

  @Override
  public boolean isBottom() {
    return true;
  }

  @Override
  public boolean isSubtypeOf(final HeapType other) {
    // Bottom type is a subtype of all GC heap types (any, eq, i31, struct, array)
    if (other instanceof NoneRef) {
      return true;
    }
    WasmValueType otherType = other.getValueType();
    return otherType == WasmValueType.ANYREF
        || otherType == WasmValueType.EQREF
        || otherType == WasmValueType.I31REF
        || otherType == WasmValueType.STRUCTREF
        || otherType == WasmValueType.ARRAYREF;
  }

  @Override
  public String getTypeName() {
    return "none";
  }

  @Override
  public String toString() {
    return "none";
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof NoneRef;
  }

  @Override
  public int hashCode() {
    return NoneRef.class.hashCode();
  }
}
