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
 * Represents the mutability of a WebAssembly global variable.
 *
 * <p>WebAssembly globals can be either immutable (constant) or mutable (variable). This enum
 * provides a type-safe representation of this attribute.
 *
 * @since 1.1.0
 */
public enum Mutability {

  /** An immutable (constant) global. */
  CONST,

  /** A mutable (variable) global. */
  VAR;

  /**
   * Checks if this represents an immutable global.
   *
   * @return true if this is CONST
   */
  public boolean isConst() {
    return this == CONST;
  }

  /**
   * Checks if this represents a mutable global.
   *
   * @return true if this is VAR
   */
  public boolean isVar() {
    return this == VAR;
  }

  /**
   * Converts a boolean mutability flag to a Mutability enum value.
   *
   * @param mutable true for VAR, false for CONST
   * @return the corresponding Mutability value
   */
  public static Mutability fromBoolean(final boolean mutable) {
    return mutable ? VAR : CONST;
  }
}
