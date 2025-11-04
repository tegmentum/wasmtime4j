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
 * Represents a WIT signed 32-bit integer value.
 *
 * <p>This class wraps a Java int value and provides type-safe conversion to/from WIT s32 type.
 * Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitS32 extends WitPrimitiveValue {

  private static final WitType S32_TYPE = WitType.createS32();

  private final int value;

  /**
   * Creates a new WIT s32 value.
   *
   * @param value the 32-bit signed integer value
   */
  private WitS32(final int value) {
    super(S32_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT s32 value from a Java int.
   *
   * @param value the integer value
   * @return a WIT s32 value
   */
  public static WitS32 of(final int value) {
    return new WitS32(value);
  }

  /**
   * Gets the integer value.
   *
   * @return the integer value
   */
  public int getValue() {
    return value;
  }

  @Override
  public Integer toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java int values are valid for WIT s32
  }

  @Override
  public String toString() {
    return String.format("WitS32{value=%d}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitS32)) {
      return false;
    }
    final WitS32 other = (WitS32) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
