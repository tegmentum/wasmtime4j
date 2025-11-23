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
import java.util.Objects;

/**
 * Represents a WIT unsigned 16-bit integer value.
 *
 * <p>Since Java does not have unsigned primitives, this class stores the value as a signed short
 * but validates that it represents a valid unsigned 16-bit integer (0 to 65,535). Use {@link
 * #toUnsignedInt()} to get the unsigned interpretation.
 *
 * @since 1.0.0
 */
public final class WitU16 extends WitPrimitiveValue {

  private static final WitType U16_TYPE = WitType.createU16();

  private final short value;

  private WitU16(final short value) {
    super(U16_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT unsigned 16-bit integer from a signed short value.
   *
   * @param value the short value (interpreted as unsigned)
   * @return a WIT u16 value
   */
  public static WitU16 of(final short value) {
    return new WitU16(value);
  }

  /**
   * Creates a WIT unsigned 16-bit integer from an unsigned int value.
   *
   * @param unsignedValue the unsigned int value (must be in range 0 to 65,535)
   * @return a WIT u16 value
   * @throws IllegalArgumentException if value is out of range
   */
  public static WitU16 ofUnsigned(final int unsignedValue) {
    if (unsignedValue < 0 || unsignedValue > 65535) {
      throw new IllegalArgumentException(
          "Value out of range for u16: " + unsignedValue + " (must be 0 to 65535)");
    }
    return new WitU16((short) unsignedValue);
  }

  /**
   * Gets the raw short value (signed representation of unsigned value).
   *
   * @return the signed short value
   */
  public short getValue() {
    return value;
  }

  /**
   * Gets the unsigned int interpretation of this value.
   *
   * @return the value as an unsigned int (0 to 65,535)
   */
  public int toUnsignedInt() {
    return Short.toUnsignedInt(value);
  }

  @Override
  public Short toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java short values are valid for WIT u16 (interpreted as unsigned)
  }

  @Override
  public String toString() {
    return String.format("WitU16{value=%d (unsigned: %d)}", value, toUnsignedInt());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitU16)) {
      return false;
    }
    final WitU16 other = (WitU16) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
