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

package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.WitType;

/**
 * Base class for WebAssembly Interface Type (WIT) primitive values.
 *
 * <p>Primitive values represent the fundamental scalar types in the WIT type system:
 *
 * <ul>
 *   <li>Boolean: bool
 *   <li>Signed integers: s8, s16, s32, s64
 *   <li>Unsigned integers: u8, u16, u32, u64
 *   <li>Floating-point: float32, float64
 *   <li>Character: char (Unicode scalar value)
 *   <li>String: string (UTF-8 encoded)
 * </ul>
 *
 * <p>This class can only be extended by specific primitive value implementations in this package.
 * All primitive values are immutable and thread-safe.
 *
 * <p>Permitted subclasses:
 *
 * <ul>
 *   <li>WitBool - for boolean values
 *   <li>WitS8, WitS16, WitS32, WitS64 - for signed integers
 *   <li>WitU8, WitU16, WitU32, WitU64 - for unsigned integers
 *   <li>WitFloat32, WitFloat64 - for floating-point numbers
 *   <li>WitChar - for Unicode characters
 *   <li>WitString - for UTF-8 strings
 * </ul>
 *
 * @since 1.0.0
 */
public abstract class WitPrimitiveValue extends WitValue {

  /**
   * Creates a new primitive WIT value with the specified type.
   *
   * @param type the WIT type of this primitive value (must not be null)
   * @throws IllegalArgumentException if type is null or not a primitive type
   */
  protected WitPrimitiveValue(final WitType type) {
    super(type);
    validatePrimitiveType(type);
  }

  /**
   * Validates that the provided type is a primitive WIT type.
   *
   * @param type the type to validate
   * @throws IllegalArgumentException if type is not a primitive type
   */
  private void validatePrimitiveType(final WitType type) {
    if (!type.getKind().getPrimitiveType().isPresent()) {
      throw new IllegalArgumentException(
          String.format("Type %s is not a primitive WIT type", type));
    }
  }

  /**
   * Checks if this primitive value is a numeric type (integer or floating-point).
   *
   * @return true if this is a numeric type
   */
  public boolean isNumeric() {
    return getType()
        .getKind()
        .getPrimitiveType()
        .map(p -> p.isInteger() || p.isFloatingPoint())
        .orElse(false);
  }

  /**
   * Checks if this primitive value is an integer type (signed or unsigned).
   *
   * @return true if this is an integer type
   */
  public boolean isInteger() {
    return getType().getKind().getPrimitiveType().map(p -> p.isInteger()).orElse(false);
  }

  /**
   * Checks if this primitive value is a floating-point type.
   *
   * @return true if this is a floating-point type
   */
  public boolean isFloatingPoint() {
    return getType().getKind().getPrimitiveType().map(p -> p.isFloatingPoint()).orElse(false);
  }

  /**
   * Checks if this primitive value is an unsigned integer type.
   *
   * @return true if this is an unsigned integer type
   */
  public boolean isUnsigned() {
    return getType().getKind().getPrimitiveType().map(p -> p.isUnsignedInteger()).orElse(false);
  }

  /**
   * Checks if this primitive value is a signed integer type.
   *
   * @return true if this is a signed integer type
   */
  public boolean isSigned() {
    return getType().getKind().getPrimitiveType().map(p -> p.isSignedInteger()).orElse(false);
  }
}
