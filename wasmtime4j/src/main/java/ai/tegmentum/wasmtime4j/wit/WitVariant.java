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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a WIT variant value (sum type / tagged union).
 *
 * <p>Variants are discriminated unions where exactly one case is active at a time. Each case has a
 * name and an optional payload value. Variants are similar to enums but can carry associated data.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create variant type definition
 * Map<String, Optional<WitType>> cases = new LinkedHashMap<>();
 * cases.put("success", Optional.of(WitType.createString()));
 * cases.put("error", Optional.of(WitType.createS32()));
 * cases.put("pending", Optional.empty());  // No payload
 *
 * WitType variantType = WitType.variant("result", cases);
 *
 * // Create variant values
 * WitVariant success = WitVariant.of(variantType, "success", WitString.of("Done!"));
 * WitVariant error = WitVariant.of(variantType, "error", WitS32.of(404));
 * WitVariant pending = WitVariant.of(variantType, "pending");  // No payload
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitVariant extends WitValue {

  private final String caseName;
  private final Optional<WitValue> payload;

  /**
   * Creates a new WIT variant value with a payload.
   *
   * @param variantType the variant type definition
   * @param caseName the active case name
   * @param payload the case payload (may be null for cases without payload)
   */
  private WitVariant(
      final WitType variantType, final String caseName, final Optional<WitValue> payload) {
    super(variantType);
    if (caseName == null || caseName.isEmpty()) {
      throw new IllegalArgumentException("Case name cannot be null or empty");
    }
    this.caseName = caseName;
    this.payload = payload == null ? Optional.empty() : payload;
    validate();
  }

  /**
   * Creates a WIT variant with a payload.
   *
   * @param variantType the variant type (must be a variant type)
   * @param caseName the active case name
   * @param payload the payload value (must match the case's payload type)
   * @return a WIT variant value
   * @throws IllegalArgumentException if case doesn't exist or payload type doesn't match
   */
  public static WitVariant of(
      final WitType variantType, final String caseName, final WitValue payload) {
    return new WitVariant(variantType, caseName, Optional.ofNullable(payload));
  }

  /**
   * Creates a WIT variant without a payload.
   *
   * @param variantType the variant type (must be a variant type)
   * @param caseName the active case name (case must not have a payload type)
   * @return a WIT variant value
   * @throws IllegalArgumentException if case doesn't exist or case requires a payload
   */
  public static WitVariant of(final WitType variantType, final String caseName) {
    return new WitVariant(variantType, caseName, Optional.empty());
  }

  /**
   * Gets the active case name.
   *
   * @return the case name
   */
  public String getCaseName() {
    return caseName;
  }

  /**
   * Gets the payload value if present.
   *
   * @return the payload value, or empty if this case has no payload
   */
  public Optional<WitValue> getPayload() {
    return payload;
  }

  /**
   * Checks if this variant has a payload.
   *
   * @return true if a payload is present
   */
  public boolean hasPayload() {
    return payload.isPresent();
  }

  @Override
  public Object toJava() {
    final Map<String, Object> result = new HashMap<>(2);
    result.put("case", caseName);
    if (payload.isPresent()) {
      result.put("payload", payload.get().toJava());
    }
    return result;
  }

  @Override
  protected void validate() {
    // Extract expected cases from variant type
    final Map<String, Optional<WitType>> cases = extractCases(getType());

    // Verify case exists in variant definition
    if (!cases.containsKey(caseName)) {
      throw new IllegalArgumentException(
          String.format(
              "Case '%s' not found in variant type. Available cases: %s",
              caseName, cases.keySet()));
    }

    // Verify payload matches expected type
    final Optional<WitType> expectedPayloadType = cases.get(caseName);

    if (expectedPayloadType.isPresent()) {
      // Case expects a payload
      if (!payload.isPresent()) {
        throw new IllegalArgumentException(
            String.format(
                "Case '%s' requires a payload of type %s",
                caseName, expectedPayloadType.get().getName()));
      }
      // Verify payload type matches
      if (!payload.get().getType().equals(expectedPayloadType.get())) {
        throw new IllegalArgumentException(
            String.format(
                "Case '%s' payload has type %s but expected %s",
                caseName, payload.get().getType().getName(), expectedPayloadType.get().getName()));
      }
    } else {
      // Case does not expect a payload
      if (payload.isPresent()) {
        throw new IllegalArgumentException(
            String.format("Case '%s' does not accept a payload", caseName));
      }
    }
  }

  /**
   * Extracts the case definitions from a variant type.
   *
   * @param variantType the variant type
   * @return a map of case names to optional payload types
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Optional<WitType>> extractCases(final WitType variantType) {
    // Get cases from variant type kind
    // This is a simplified extraction - in a full implementation,
    // WitType would provide a getCases() method
    if (variantType.getKind() == null || !"VARIANT".equals(variantType.getKind().toString())) {
      throw new IllegalArgumentException("Type must be a variant type");
    }

    // For now, return an empty map as a placeholder
    // In the full implementation, this would extract from WitType.getKind().getCases()
    // This will be enhanced when WitTypeKind is fully implemented
    return new LinkedHashMap<>();
  }

  @Override
  public String toString() {
    if (payload.isPresent()) {
      return String.format("WitVariant{case='%s', payload=%s}", caseName, payload.get());
    }
    return String.format("WitVariant{case='%s'}", caseName);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitVariant)) {
      return false;
    }
    final WitVariant other = (WitVariant) obj;
    return caseName.equals(other.caseName) && payload.equals(other.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), caseName, payload);
  }
}
