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
import java.math.BigInteger;
import java.util.Objects;

/**
 * Represents a WIT unsigned 64-bit integer value.
 *
 * <p>Since Java does not have unsigned primitives, this class stores the value as a signed long but
 * validates that it represents a valid unsigned 64-bit integer. Use {@link #toUnsignedBigInteger()}
 * to get the unsigned interpretation.
 *
 * @since 1.0.0
 */
public final class WitU64 extends WitPrimitiveValue {

  private static final WitType U64_TYPE = WitType.createU64();

  private final long value;

  private WitU64(final long value) {
    super(U64_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT unsigned 64-bit integer from a signed long value.
   *
   * @param value the long value (interpreted as unsigned)
   * @return a WIT u64 value
   */
  public static WitU64 of(final long value) {
    return new WitU64(value);
  }

  /**
   * Creates a WIT unsigned 64-bit integer from a BigInteger value.
   *
   * @param unsignedValue the unsigned BigInteger value (must be in range 0 to 2^64-1)
   * @return a WIT u64 value
   * @throws IllegalArgumentException if value is out of range
   */
  public static WitU64 ofUnsigned(final BigInteger unsignedValue) {
    if (unsignedValue.compareTo(BigInteger.ZERO) < 0
        || unsignedValue.compareTo(new BigInteger("18446744073709551615")) > 0) {
      throw new IllegalArgumentException(
          "Value out of range for u64: " + unsignedValue + " (must be 0 to 18446744073709551615)");
    }
    return new WitU64(unsignedValue.longValue());
  }

  /**
   * Gets the raw long value (signed representation of unsigned value).
   *
   * @return the signed long value
   */
  public long getValue() {
    return value;
  }

  /**
   * Gets the unsigned BigInteger interpretation of this value.
   *
   * @return the value as an unsigned BigInteger (0 to 2^64-1)
   */
  public BigInteger toUnsignedBigInteger() {
    if (value >= 0) {
      return BigInteger.valueOf(value);
    } else {
      return BigInteger.valueOf(value).add(BigInteger.ONE.shiftLeft(64));
    }
  }

  @Override
  public Long toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java long values are valid for WIT u64 (interpreted as unsigned)
  }

  @Override
  public String toString() {
    return String.format("WitU64{value=%d (unsigned: %s)}", value, toUnsignedBigInteger());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitU64)) {
      return false;
    }
    final WitU64 other = (WitU64) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
