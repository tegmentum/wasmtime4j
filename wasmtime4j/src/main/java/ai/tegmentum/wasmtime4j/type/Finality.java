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
 * Represents the finality of a type in the WebAssembly GC proposal.
 *
 * <p>In the GC proposal, types can be either final (cannot be subtyped) or non-final (can be
 * subtyped). By default, types are final.
 *
 * @since 1.0.0
 */
public enum Finality {

  /** The type is final and cannot be subtyped. This is the default. */
  FINAL,

  /** The type is non-final and can be subtyped. */
  NON_FINAL;

  /**
   * Checks if this finality is final.
   *
   * @return true if the type is final
   */
  public boolean isFinal() {
    return this == FINAL;
  }

  /**
   * Checks if this finality is non-final (subtypeable).
   *
   * @return true if the type is non-final
   */
  public boolean isNonFinal() {
    return this == NON_FINAL;
  }

  /**
   * Creates a finality from a boolean flag.
   *
   * @param isFinal true for final, false for non-final
   * @return the corresponding finality
   */
  public static Finality fromBoolean(final boolean isFinal) {
    return isFinal ? FINAL : NON_FINAL;
  }
}
