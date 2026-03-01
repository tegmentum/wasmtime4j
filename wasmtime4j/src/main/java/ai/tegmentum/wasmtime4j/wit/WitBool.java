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
 * Represents a WIT boolean value.
 *
 * <p>This class wraps a Java boolean value and provides type-safe conversion to/from WIT bool type.
 * Boolean values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitBool extends WitPrimitiveValue {

  private static final WitType BOOL_TYPE = WitType.createBool();

  /** Cached instance for true value. */
  public static final WitBool TRUE = new WitBool(true);

  /** Cached instance for false value. */
  public static final WitBool FALSE = new WitBool(false);

  private final boolean value;

  /**
   * Creates a new WIT boolean value.
   *
   * @param value the boolean value
   */
  private WitBool(final boolean value) {
    super(BOOL_TYPE);
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT boolean value from a Java boolean.
   *
   * @param value the boolean value
   * @return a WIT boolean value
   */
  public static WitBool of(final boolean value) {
    return value ? TRUE : FALSE;
  }

  /**
   * Gets the boolean value.
   *
   * @return the boolean value
   */
  public boolean getValue() {
    return value;
  }

  @Override
  public Boolean toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // Boolean values are always valid
  }

  @Override
  public String toString() {
    return String.format("WitBool{value=%s}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitBool)) {
      return false;
    }
    final WitBool other = (WitBool) obj;
    return value == other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
