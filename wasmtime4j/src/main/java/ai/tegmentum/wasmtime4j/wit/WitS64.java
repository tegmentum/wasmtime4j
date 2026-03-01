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
 * Represents a WIT signed 64-bit integer value.
 *
 * <p>This class wraps a Java long value and provides type-safe conversion to/from WIT s64 type.
 * Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitS64 extends WitPrimitiveValue {

  private static final WitType S64_TYPE = WitType.createS64();

  private final long value;

  /**
   * Creates a new WIT s64 value.
   *
   * @param value the 64-bit signed integer value
   */
  private WitS64(final long value) {
    super(S64_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT s64 value from a Java long.
   *
   * @param value the long value
   * @return a WIT s64 value
   */
  public static WitS64 of(final long value) {
    return new WitS64(value);
  }

  /**
   * Gets the long value.
   *
   * @return the long value
   */
  public long getValue() {
    return value;
  }

  @Override
  public Long toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // All Java long values are valid for WIT s64
  }

  @Override
  public String toString() {
    return String.format("WitS64{value=%d}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitS64)) {
      return false;
    }
    final WitS64 other = (WitS64) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
