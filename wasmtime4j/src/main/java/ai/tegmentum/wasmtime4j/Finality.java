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

/**
 * Represents the finality modifier for WebAssembly GC types.
 *
 * <p>In the WebAssembly GC proposal, types can be declared with a finality modifier that controls
 * whether they can be used as supertypes for other types (subtyping/inheritance).
 *
 * <p>This affects type checking and runtime behavior:
 *
 * <ul>
 *   <li>{@link #FINAL} - The type cannot have subtypes. This allows for more efficient runtime
 *       checks since the type cannot be extended.
 *   <li>{@link #NON_FINAL} - The type can be used as a supertype. Other types can declare this type
 *       as their supertype.
 * </ul>
 *
 * <p>Example in WebAssembly text format:
 *
 * <pre>
 * (type $point (struct (field $x i32) (field $y i32)))           ;; non-final by default
 * (type $point3d (sub $point (struct                              ;; extends $point
 *     (field $x i32) (field $y i32) (field $z i32))))
 *
 * (type $sealed (sub final (struct (field i32))))                 ;; final - cannot be extended
 * </pre>
 *
 * @since 1.0.0
 */
public enum Finality {

  /**
   * The type is final and cannot have subtypes.
   *
   * <p>Final types allow for more efficient runtime type checks since the runtime knows no other
   * types can be substituted.
   */
  FINAL("final"),

  /**
   * The type is non-final and can be used as a supertype.
   *
   * <p>Non-final types can be extended by other types using the {@code sub} declaration.
   */
  NON_FINAL("sub");

  private final String wasmKeyword;

  Finality(final String wasmKeyword) {
    this.wasmKeyword = wasmKeyword;
  }

  /**
   * Gets the WebAssembly text format keyword for this finality.
   *
   * @return the WASM keyword
   */
  public String getWasmKeyword() {
    return wasmKeyword;
  }

  /**
   * Checks if this finality allows subtyping.
   *
   * @return true if this type can have subtypes
   */
  public boolean allowsSubtypes() {
    return this == NON_FINAL;
  }

  /**
   * Checks if this finality is final.
   *
   * @return true if this is final
   */
  public boolean isFinal() {
    return this == FINAL;
  }

  /**
   * Parses finality from a WebAssembly keyword.
   *
   * @param keyword the keyword to parse
   * @return the corresponding Finality
   * @throws IllegalArgumentException if keyword is not recognized
   */
  public static Finality fromWasmKeyword(final String keyword) {
    if (keyword == null) {
      throw new IllegalArgumentException("keyword cannot be null");
    }
    for (Finality f : values()) {
      if (f.wasmKeyword.equals(keyword)) {
        return f;
      }
    }
    throw new IllegalArgumentException("Unknown finality keyword: " + keyword);
  }

  @Override
  public String toString() {
    return wasmKeyword;
  }
}
