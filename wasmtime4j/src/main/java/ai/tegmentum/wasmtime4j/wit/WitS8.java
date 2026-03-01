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
package ai.tegmentum.wasmtime4j.wit;

import java.util.Objects;

/**
 * Represents a WIT signed 8-bit integer value.
 *
 * <p>This class wraps a Java byte value and provides type-safe conversion to/from WIT s8 type.
 * Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitS8 extends WitPrimitiveValue {

  private static final WitType S8_TYPE = WitType.createS8();

  private final byte value;

  /**
   * Creates a new WIT s8 value.
   *
   * @param value the 8-bit signed integer value
   */
  private WitS8(final byte value) {
    super(S8_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT s8 value from a Java byte.
   *
   * @param value the byte value
   * @return a WIT s8 value
   */
  public static WitS8 of(final byte value) {
    return new WitS8(value);
  }

  /**
   * Gets the byte value.
   *
   * @return the byte value
   */
  public byte getValue() {
    return value;
  }

  @Override
  public Byte toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java byte values are valid for WIT s8
  }

  @Override
  public String toString() {
    return String.format("WitS8{value=%d}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitS8)) {
      return false;
    }
    final WitS8 other = (WitS8) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
