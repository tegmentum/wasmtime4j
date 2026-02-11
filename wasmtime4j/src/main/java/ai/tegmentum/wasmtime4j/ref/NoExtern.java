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
 * Represents the noextern heap type - the bottom type in the extern type hierarchy.
 *
 * <p>NoExtern is an uninhabited type, meaning no values can have this type. It serves as the bottom
 * type in the extern reference type hierarchy, used for type system completeness in the WebAssembly
 * GC proposal.
 *
 * <p>This type is primarily used in type checking and validation rather than for storing actual
 * values.
 *
 * <p>Type hierarchy:
 *
 * <pre>
 *      extern
 *         |
 *      noextern (bottom)
 * </pre>
 *
 * @since 1.1.0
 */
public final class NoExtern implements HeapType {

  /** Singleton instance of NoExtern. */
  public static final NoExtern INSTANCE = new NoExtern();

  private NoExtern() {
    // Private constructor for singleton
  }

  /**
   * Gets the singleton instance of NoExtern.
   *
   * @return the NoExtern instance
   */
  public static NoExtern getInstance() {
    return INSTANCE;
  }

  @Override
  public WasmValueType getValueType() {
    return WasmValueType.EXTERNREF;
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
    // Bottom type is a subtype of all extern types
    return other instanceof NoExtern || other.getValueType() == WasmValueType.EXTERNREF;
  }

  @Override
  public String getTypeName() {
    return "noextern";
  }

  @Override
  public String toString() {
    return "noextern";
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof NoExtern;
  }

  @Override
  public int hashCode() {
    return NoExtern.class.hashCode();
  }
}
