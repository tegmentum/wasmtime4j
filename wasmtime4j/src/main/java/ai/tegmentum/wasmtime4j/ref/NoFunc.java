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
 * Represents the nofunc heap type - the bottom type in the func type hierarchy.
 *
 * <p>NoFunc is an uninhabited type, meaning no values can have this type. It serves as the bottom
 * type in the function reference type hierarchy, used for type system completeness in the
 * WebAssembly GC proposal.
 *
 * <p>This type is primarily used in type checking and validation rather than for storing actual
 * values.
 *
 * <p>Type hierarchy:
 *
 * <pre>
 *       func
 *         |
 *      nofunc (bottom)
 * </pre>
 *
 * @since 1.1.0
 */
public final class NoFunc implements HeapType {

  /** Singleton instance of NoFunc. */
  public static final NoFunc INSTANCE = new NoFunc();

  private NoFunc() {
    // Private constructor for singleton
  }

  /**
   * Gets the singleton instance of NoFunc.
   *
   * @return the NoFunc instance
   */
  public static NoFunc getInstance() {
    return INSTANCE;
  }

  @Override
  public WasmValueType getValueType() {
    return WasmValueType.FUNCREF;
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
    // Bottom type is a subtype of all func types
    return other instanceof NoFunc || other.getValueType() == WasmValueType.FUNCREF;
  }

  @Override
  public String getTypeName() {
    return "nofunc";
  }

  @Override
  public String toString() {
    return "nofunc";
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof NoFunc;
  }

  @Override
  public int hashCode() {
    return NoFunc.class.hashCode();
  }
}
