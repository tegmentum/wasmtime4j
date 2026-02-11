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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a WIT enum value (discriminated choice without payload).
 *
 * <p>Enums are a set of named discriminants without associated data. They are similar to variants
 * but simpler - each enum value is just a name from a predefined set.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create enum type definition
 * WitType colorType = WitType.enumType("color", Arrays.asList("red", "green", "blue"));
 *
 * // Create enum values
 * WitEnum red = WitEnum.of(colorType, "red");
 * WitEnum blue = WitEnum.of(colorType, "blue");
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitEnum extends WitValue {

  private final String discriminant;

  /**
   * Creates a new WIT enum value.
   *
   * @param enumType the enum type definition
   * @param discriminant the discriminant name
   */
  private WitEnum(final WitType enumType, final String discriminant) {
    super(enumType);
    if (discriminant == null || discriminant.isEmpty()) {
      throw new IllegalArgumentException("Discriminant cannot be null or empty");
    }
    this.discriminant = discriminant;
    validate();
  }

  /**
   * Creates a WIT enum value.
   *
   * @param enumType the enum type (must be an enum type)
   * @param discriminant the discriminant name (must be in the enum's value set)
   * @return a WIT enum value
   * @throws IllegalArgumentException if discriminant is not in the enum's value set
   */
  public static WitEnum of(final WitType enumType, final String discriminant) {
    return new WitEnum(enumType, discriminant);
  }

  /**
   * Gets the discriminant name.
   *
   * @return the discriminant name
   */
  public String getDiscriminant() {
    return discriminant;
  }

  @Override
  public String toJava() {
    return discriminant;
  }

  @Override
  protected void validate() {
    // Extract expected discriminants from enum type
    final List<String> validDiscriminants = extractDiscriminants(getType());

    // Verify discriminant exists in enum definition
    if (!validDiscriminants.contains(discriminant)) {
      throw new IllegalArgumentException(
          String.format(
              "Discriminant '%s' not found in enum type. Valid discriminants: %s",
              discriminant, validDiscriminants));
    }
  }

  /**
   * Extracts the discriminant names from an enum type.
   *
   * @param enumType the enum type
   * @return a list of valid discriminant names
   */
  private static List<String> extractDiscriminants(final WitType enumType) {
    // Get discriminants from enum type kind
    // This is a simplified extraction - in a full implementation,
    // WitType would provide a getDiscriminants() method
    if (enumType.getKind() == null || !"ENUM".equals(enumType.getKind().toString())) {
      throw new IllegalArgumentException("Type must be an enum type");
    }

    // For now, return an empty list as a placeholder
    // In the full implementation, this would extract from WitType.getKind().getValues()
    // This will be enhanced when WitTypeKind is fully implemented
    return Arrays.asList();
  }

  @Override
  public String toString() {
    return String.format("WitEnum{%s}", discriminant);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitEnum)) {
      return false;
    }
    final WitEnum other = (WitEnum) obj;
    return discriminant.equals(other.discriminant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), discriminant);
  }
}
