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
 * Represents a WIT 64-bit floating-point value.
 *
 * <p>This class wraps a Java double value and provides type-safe conversion to/from WIT float64
 * type. Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitFloat64 extends WitPrimitiveValue {

  private static final WitType FLOAT64_TYPE = WitType.createFloat64();

  private final double value;

  /**
   * Creates a new WIT float64 value.
   *
   * @param value the 64-bit floating-point value
   */
  private WitFloat64(final double value) {
    super(FLOAT64_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT float64 value from a Java double.
   *
   * @param value the double value
   * @return a WIT float64 value
   */
  public static WitFloat64 of(final double value) {
    return new WitFloat64(value);
  }

  /**
   * Gets the double value.
   *
   * @return the double value
   */
  public double getValue() {
    return value;
  }

  @Override
  public Double toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java double values (including NaN and infinities) are valid for WIT float64
  }

  @Override
  public String toString() {
    return String.format("WitFloat64{value=%f}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitFloat64)) {
      return false;
    }
    final WitFloat64 other = (WitFloat64) obj;
    return Double.compare(value, other.value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
