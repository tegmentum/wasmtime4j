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

import java.util.Objects;

/**
 * Represents a WIT unsigned 8-bit integer value.
 *
 * <p>Since Java does not have unsigned primitives, this class stores the value as a signed byte but
 * validates that it represents a valid unsigned 8-bit integer (0 to 255). Use {@link
 * #toUnsignedInt()} to get the unsigned interpretation.
 *
 * @since 1.0.0
 */
public final class WitU8 extends WitPrimitiveValue {

  private static final WitType U8_TYPE = WitType.createU8();

  private final byte value;

  private WitU8(final byte value) {
    super(U8_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT unsigned 8-bit integer from a signed byte value.
   *
   * @param value the byte value (interpreted as unsigned)
   * @return a WIT u8 value
   */
  public static WitU8 of(final byte value) {
    return new WitU8(value);
  }

  /**
   * Creates a WIT unsigned 8-bit integer from an unsigned int value.
   *
   * @param unsignedValue the unsigned int value (must be in range 0 to 255)
   * @return a WIT u8 value
   * @throws IllegalArgumentException if value is out of range
   */
  public static WitU8 ofUnsigned(final int unsignedValue) {
    if (unsignedValue < 0 || unsignedValue > 255) {
      throw new IllegalArgumentException(
          "Value out of range for u8: " + unsignedValue + " (must be 0 to 255)");
    }
    return new WitU8((byte) unsignedValue);
  }

  /**
   * Gets the raw byte value (signed representation of unsigned value).
   *
   * @return the signed byte value
   */
  public byte getValue() {
    return value;
  }

  /**
   * Gets the unsigned int interpretation of this value.
   *
   * @return the value as an unsigned int (0 to 255)
   */
  public int toUnsignedInt() {
    return Byte.toUnsignedInt(value);
  }

  @Override
  public Byte toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java byte values are valid for WIT u8 (interpreted as unsigned)
  }

  @Override
  public String toString() {
    return String.format("WitU8{value=%d (unsigned: %d)}", value, toUnsignedInt());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitU8)) {
      return false;
    }
    final WitU8 other = (WitU8) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
