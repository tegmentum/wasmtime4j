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
 * Represents a WIT signed 16-bit integer value.
 *
 * <p>This class wraps a Java short value and provides type-safe conversion to/from WIT s16 type.
 * Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitS16 extends WitPrimitiveValue {

  private static final WitType S16_TYPE = WitType.createS16();

  private final short value;

  /**
   * Creates a new WIT s16 value.
   *
   * @param value the 16-bit signed integer value
   */
  private WitS16(final short value) {
    super(S16_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT s16 value from a Java short.
   *
   * @param value the short value
   * @return a WIT s16 value
   */
  public static WitS16 of(final short value) {
    return new WitS16(value);
  }

  /**
   * Gets the short value.
   *
   * @return the short value
   */
  public short getValue() {
    return value;
  }

  @Override
  public Short toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java short values are valid for WIT s16
  }

  @Override
  public String toString() {
    return String.format("WitS16{value=%d}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitS16)) {
      return false;
    }
    final WitS16 other = (WitS16) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
