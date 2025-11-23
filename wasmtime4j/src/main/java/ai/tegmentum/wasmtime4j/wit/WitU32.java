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
 * Represents a WIT unsigned 32-bit integer value.
 *
 * <p>Since Java does not have unsigned primitives, this class stores the value as a signed int but
 * validates that it represents a valid unsigned 32-bit integer (0 to 4,294,967,295). Use {@link
 * #toUnsignedLong()} to get the unsigned interpretation.
 *
 * @since 1.0.0
 */
public final class WitU32 extends WitPrimitiveValue {

  private static final WitType U32_TYPE = WitType.createU32();

  private final int value;

  private WitU32(final int value) {
    super(U32_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT unsigned 32-bit integer from a signed int value.
   *
   * @param value the int value (interpreted as unsigned)
   * @return a WIT u32 value
   */
  public static WitU32 of(final int value) {
    return new WitU32(value);
  }

  /**
   * Creates a WIT unsigned 32-bit integer from an unsigned long value.
   *
   * @param unsignedValue the unsigned long value (must be in range 0 to 4,294,967,295)
   * @return a WIT u32 value
   * @throws IllegalArgumentException if value is out of range
   */
  public static WitU32 ofUnsigned(final long unsignedValue) {
    if (unsignedValue < 0 || unsignedValue > 0xFFFFFFFFL) {
      throw new IllegalArgumentException(
          "Value out of range for u32: " + unsignedValue + " (must be 0 to 4294967295)");
    }
    return new WitU32((int) unsignedValue);
  }

  /**
   * Gets the raw int value (signed representation of unsigned value).
   *
   * @return the signed int value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets the unsigned long interpretation of this value.
   *
   * @return the value as an unsigned long (0 to 4,294,967,295)
   */
  public long toUnsignedLong() {
    return Integer.toUnsignedLong(value);
  }

  @Override
  public Integer toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java int values are valid for WIT u32 (interpreted as unsigned)
  }

  @Override
  public String toString() {
    return String.format("WitU32{value=%d (unsigned: %d)}", value, toUnsignedLong());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitU32)) {
      return false;
    }
    final WitU32 other = (WitU32) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
