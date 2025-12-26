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
import java.util.Optional;

/**
 * Represents a WIT option value (nullable value).
 *
 * <p>Options represent values that may or may not be present, similar to {@link Optional} in Java
 * or {@code Option} in Rust. An option can be either "some" (containing a value) or "none" (no
 * value).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create option type definition
 * WitType optionStringType = WitType.option(WitType.createString());
 *
 * // Create option values
 * WitOption some = WitOption.some(optionStringType, WitString.of("Hello"));
 * WitOption none = WitOption.none(optionStringType);
 *
 * // Check and access value
 * if (some.isSome()) {
 *     WitValue value = some.get();  // Returns WitString.of("Hello")
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitOption extends WitValue {

  private final Optional<WitValue> value;
  private final WitType innerType;

  /**
   * Creates a new WIT option value.
   *
   * @param optionType the option type definition
   * @param value the inner value (empty for none)
   */
  private WitOption(final WitType optionType, final Optional<WitValue> value) {
    super(optionType);
    this.innerType = extractInnerType(optionType);
    this.value = value == null ? Optional.empty() : value;
    validate();
  }

  /**
   * Creates a "some" option containing a value.
   *
   * @param optionType the option type (must be an option type)
   * @param value the value to wrap (must not be null and must match inner type)
   * @return a WIT option value containing the given value
   * @throws IllegalArgumentException if value is null or type doesn't match
   */
  public static WitOption some(final WitType optionType, final WitValue value) {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null for some(). Use none() instead.");
    }
    return new WitOption(optionType, Optional.of(value));
  }

  /**
   * Creates a "none" option with no value.
   *
   * @param optionType the option type (must be an option type)
   * @return a WIT option value with no value
   */
  public static WitOption none(final WitType optionType) {
    return new WitOption(optionType, Optional.empty());
  }

  /**
   * Creates a WIT option from a Java Optional.
   *
   * @param optionType the option type (must be an option type)
   * @param value the optional value
   * @return a WIT option value
   */
  public static WitOption of(final WitType optionType, final Optional<WitValue> value) {
    return new WitOption(optionType, value);
  }

  /**
   * Checks if this option contains a value.
   *
   * @return true if this is a "some" option
   */
  public boolean isSome() {
    return value.isPresent();
  }

  /**
   * Checks if this option is empty.
   *
   * @return true if this is a "none" option
   */
  public boolean isNone() {
    return !value.isPresent();
  }

  /**
   * Gets the inner value if present.
   *
   * @return the inner value
   * @throws java.util.NoSuchElementException if this is a "none" option
   */
  public WitValue get() {
    return value.get();
  }

  /**
   * Gets the inner value as an Optional.
   *
   * @return the inner value wrapped in Optional
   */
  public Optional<WitValue> getValue() {
    return value;
  }

  /**
   * Gets the inner type of this option.
   *
   * @return the inner type
   */
  public WitType getInnerType() {
    return innerType;
  }

  @Override
  public Optional<Object> toJava() {
    return value.map(WitValue::toJava);
  }

  @Override
  protected void validate() {
    if (value.isPresent()) {
      final WitValue v = value.get();
      if (!v.getType().equals(innerType)) {
        throw new IllegalArgumentException(
            String.format(
                "Option value has type %s but expected %s",
                v.getType().getName(), innerType.getName()));
      }
    }
  }

  /**
   * Extracts the inner type from an option type.
   *
   * @param optionType the option type
   * @return the inner type
   */
  private static WitType extractInnerType(final WitType optionType) {
    // Get inner type from option type kind
    // This is a simplified extraction - in a full implementation,
    // WitType would provide a getInnerType() method
    if (optionType.getKind() == null
        || optionType.getKind().getCategory() != ai.tegmentum.wasmtime4j.WitTypeCategory.OPTION) {
      throw new IllegalArgumentException("Type must be an option type");
    }

    // Get the inner type from the option type kind
    return optionType.getKind().getInnerType().orElse(WitType.createString());
  }

  @Override
  public String toString() {
    if (value.isPresent()) {
      return String.format("WitOption{some(%s)}", value.get());
    }
    return "WitOption{none}";
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitOption)) {
      return false;
    }
    final WitOption other = (WitOption) obj;
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), value);
  }
}
