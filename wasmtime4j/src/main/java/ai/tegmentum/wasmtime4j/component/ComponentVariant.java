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
package ai.tegmentum.wasmtime4j.component;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a Component Model variant value.
 *
 * <p>A variant is a tagged union type where each case can have an optional payload of a specific
 * type. This is similar to Rust's enum with data or algebraic data types in functional languages.
 *
 * @since 1.0.0
 */
public interface ComponentVariant {

  /**
   * Gets the name of the active variant case.
   *
   * @return the case name
   */
  String getCaseName();

  /**
   * Gets the payload value for this variant case, if any.
   *
   * @return the optional payload value
   */
  Optional<ComponentVal> getPayload();

  /**
   * Checks if this variant has a payload.
   *
   * @return true if the variant case has a payload
   */
  default boolean hasPayload() {
    return getPayload().isPresent();
  }

  /**
   * Creates a new variant with the given case name and no payload.
   *
   * @param caseName the variant case name
   * @return a new ComponentVariant
   */
  static ComponentVariant of(final String caseName) {
    return new Impl(caseName, null);
  }

  /**
   * Creates a new variant with the given case name and payload.
   *
   * @param caseName the variant case name
   * @param payload the payload value
   * @return a new ComponentVariant
   */
  static ComponentVariant of(final String caseName, final ComponentVal payload) {
    return new Impl(caseName, payload);
  }

  /** Default implementation of ComponentVariant. */
  final class Impl implements ComponentVariant {
    private final String caseName;
    private final ComponentVal payload;

    Impl(final String caseName, final ComponentVal payload) {
      if (caseName == null) {
        throw new IllegalArgumentException("Case name cannot be null");
      }
      this.caseName = caseName;
      this.payload = payload;
    }

    @Override
    public String getCaseName() {
      return caseName;
    }

    @Override
    public Optional<ComponentVal> getPayload() {
      return Optional.ofNullable(payload);
    }

    @Override
    public String toString() {
      if (payload != null) {
        return caseName + "(" + payload + ")";
      }
      return caseName;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ComponentVariant)) {
        return false;
      }
      final ComponentVariant other = (ComponentVariant) obj;
      return caseName.equals(other.getCaseName())
          && Objects.equals(payload, other.getPayload().orElse(null));
    }

    @Override
    public int hashCode() {
      return Objects.hash(caseName, payload);
    }
  }
}
