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
package ai.tegmentum.wasmtime4j.gc;

/**
 * Represents the finality of a WebAssembly GC type definition.
 *
 * <p>In the WebAssembly GC proposal, types can be declared as {@code final} (the default) or {@code
 * non-final}. A final type cannot be extended (subtyped), while a non-final type may have subtypes.
 *
 * <p>This maps to Wasmtime's type finality concept, used when defining struct types, array types,
 * and exception types in the GC type system.
 *
 * @since 1.1.0
 */
public enum Finality {

  /** The type is final and cannot be subtyped. This is the default for WebAssembly GC types. */
  FINAL,

  /** The type is non-final and may be subtyped by other type definitions. */
  NON_FINAL;

  /**
   * Returns whether this finality permits subtyping.
   *
   * @return true if the type can be subtyped (i.e. is non-final)
   */
  public boolean allowsSubtyping() {
    return this == NON_FINAL;
  }
}
