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
 * Represents a WIT 32-bit IEEE 754 floating-point value.
 *
 * <p>This class wraps a Java float value and provides type-safe conversion to/from WIT float32
 * type. Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitFloat32 extends WitPrimitiveValue {

  private static final WitType FLOAT32_TYPE = WitType.createFloat32();

  private final float value;

  /**
   * Creates a new WIT float32 value.
   *
   * @param value the 32-bit floating-point value
   */
  private WitFloat32(final float value) {
    super(FLOAT32_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT float32 value from a Java float.
   *
   * @param value the float value
   * @return a WIT float32 value
   */
  public static WitFloat32 of(final float value) {
    return new WitFloat32(value);
  }

  /**
   * Gets the float value.
   *
   * @return the float value
   */
  public float getValue() {
    return value;
  }

  @Override
  public Float toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java float values (including NaN, Infinity) are valid for WIT float32
  }

  @Override
  public String toString() {
    return String.format("WitFloat32{value=%f}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitFloat32)) {
      return false;
    }
    final WitFloat32 other = (WitFloat32) obj;
    return Float.floatToIntBits(value) == Float.floatToIntBits(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
