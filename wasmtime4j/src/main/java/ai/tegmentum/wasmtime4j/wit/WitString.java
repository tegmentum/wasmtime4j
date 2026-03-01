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

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.util.Objects;

/**
 * Represents a WIT string value (UTF-8 encoded string).
 *
 * <p>WIT strings are valid UTF-8 encoded character sequences. This class wraps a Java String and
 * provides type-safe conversion to/from WIT string type. Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitString extends WitPrimitiveValue {

  private static final WitType STRING_TYPE = WitType.createString();

  private final String value;

  /**
   * Creates a new WIT string value.
   *
   * @param value the string value (must not be null)
   * @throws ValidationException if value is null
   */
  private WitString(final String value) throws ValidationException {
    super(STRING_TYPE);
    if (value == null) {
      throw new ValidationException("WIT string value cannot be null");
    }
    this.value = value;
    validate();
  }

  /**
   * Creates a WIT string value from a Java String.
   *
   * @param value the string value (must not be null)
   * @return a WIT string value
   * @throws ValidationException if value is null
   */
  public static WitString of(final String value) throws ValidationException {
    return new WitString(value);
  }

  /**
   * Gets the string value.
   *
   * @return the string value (never null)
   */
  public String getValue() {
    return value;
  }

  @Override
  public String toJava() {
    return value;
  }

  @Override
  protected void validate() {
    // Java strings are always valid UTF-16, which can be converted to UTF-8
    // No additional validation needed here
  }

  @Override
  public String toString() {
    return String.format("WitString{value=\"%s\"}", value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitString)) {
      return false;
    }
    final WitString other = (WitString) obj;
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
