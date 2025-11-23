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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a WIT result value (success or error).
 *
 * <p>Results represent computations that can either succeed (ok) or fail (err), similar to {@code
 * Result} in Rust. Both ok and err can optionally carry payload values.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create result type definition
 * WitType resultType = WitType.result(
 *     Optional.of(WitType.createString()),  // Ok type
 *     Optional.of(WitType.createS32())      // Error type
 * );
 *
 * // Create result values
 * WitResult success = WitResult.ok(resultType, WitString.of("Success!"));
 * WitResult failure = WitResult.err(resultType, WitS32.of(404));
 *
 * // Check and access values
 * if (success.isOk()) {
 *     WitValue value = success.getOk().get();  // Returns WitString.of("Success!")
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitResult extends WitValue {

  private final boolean isOk;
  private final Optional<WitValue> value;
  private final WitType okType;
  private final WitType errorType;

  /**
   * Creates a new WIT result value.
   *
   * @param resultType the result type definition
   * @param isOk true for ok, false for err
   * @param value the payload value (may be empty for results without payload)
   */
  private WitResult(final WitType resultType, final boolean isOk, final Optional<WitValue> value) {
    super(resultType);
    this.isOk = isOk;
    this.value = value == null ? Optional.empty() : value;
    final Optional<WitType>[] types = extractTypes(resultType);
    this.okType = types[0].orElse(null);
    this.errorType = types[1].orElse(null);
    validate();
  }

  /**
   * Creates an "ok" result with a value.
   *
   * @param resultType the result type (must be a result type)
   * @param value the ok value (must match the ok type)
   * @return a WIT result value representing success
   * @throws IllegalArgumentException if value type doesn't match
   */
  public static WitResult ok(final WitType resultType, final WitValue value) {
    return new WitResult(resultType, true, Optional.ofNullable(value));
  }

  /**
   * Creates an "ok" result without a value.
   *
   * @param resultType the result type (must be a result type with no ok type)
   * @return a WIT result value representing success without a value
   */
  public static WitResult ok(final WitType resultType) {
    return new WitResult(resultType, true, Optional.empty());
  }

  /**
   * Creates an "err" result with a value.
   *
   * @param resultType the result type (must be a result type)
   * @param error the error value (must match the error type)
   * @return a WIT result value representing failure
   * @throws IllegalArgumentException if error type doesn't match
   */
  public static WitResult err(final WitType resultType, final WitValue error) {
    return new WitResult(resultType, false, Optional.ofNullable(error));
  }

  /**
   * Creates an "err" result without a value.
   *
   * @param resultType the result type (must be a result type with no error type)
   * @return a WIT result value representing failure without a value
   */
  public static WitResult err(final WitType resultType) {
    return new WitResult(resultType, false, Optional.empty());
  }

  /**
   * Checks if this is an "ok" result.
   *
   * @return true if this is a success result
   */
  public boolean isOk() {
    return isOk;
  }

  /**
   * Checks if this is an "err" result.
   *
   * @return true if this is an error result
   */
  public boolean isErr() {
    return !isOk;
  }

  /**
   * Gets the ok value if this is an ok result.
   *
   * @return the ok value, or empty if this is an err result
   */
  public Optional<WitValue> getOk() {
    return isOk ? value : Optional.empty();
  }

  /**
   * Gets the error value if this is an err result.
   *
   * @return the error value, or empty if this is an ok result
   */
  public Optional<WitValue> getErr() {
    return isOk ? Optional.empty() : value;
  }

  /**
   * Gets the payload value regardless of ok/err status.
   *
   * @return the payload value
   */
  public Optional<WitValue> getValue() {
    return value;
  }

  @Override
  public Object toJava() {
    final Map<String, Object> result = new HashMap<>(2);
    result.put("isOk", isOk);
    if (value.isPresent()) {
      result.put(isOk ? "ok" : "err", value.get().toJava());
    }
    return result;
  }

  @Override
  protected void validate() {
    if (value.isPresent()) {
      final WitValue v = value.get();
      final WitType expectedType = isOk ? okType : errorType;

      if (expectedType == null) {
        throw new IllegalArgumentException(
            String.format(
                "Result %s variant does not accept a payload", isOk ? "ok" : "err"));
      }

      if (!v.getType().equals(expectedType)) {
        throw new IllegalArgumentException(
            String.format(
                "Result %s value has type %s but expected %s",
                isOk ? "ok" : "err", v.getType().getName(), expectedType.getName()));
      }
    } else {
      // No value present - verify this is expected
      final WitType expectedType = isOk ? okType : errorType;
      if (expectedType != null) {
        throw new IllegalArgumentException(
            String.format(
                "Result %s variant requires a payload of type %s",
                isOk ? "ok" : "err", expectedType.getName()));
      }
    }
  }

  /**
   * Extracts the ok and error types from a result type.
   *
   * @param resultType the result type
   * @return an array of [okType, errorType] as Optionals
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Optional<WitType>[] extractTypes(final WitType resultType) {
    // Get ok/error types from result type kind
    // This is a simplified extraction - in a full implementation,
    // WitType would provide getOkType() and getErrorType() methods
    if (resultType.getKind() == null || !"RESULT".equals(resultType.getKind().toString())) {
      throw new IllegalArgumentException("Type must be a result type");
    }

    // For now, return placeholders
    // In the full implementation, this would extract from WitType.getKind()
    // This will be enhanced when WitTypeKind is fully implemented
    final Optional<WitType>[] result =
        new Optional[] {Optional.<WitType>empty(), Optional.<WitType>empty()};
    return result;
  }

  @Override
  public String toString() {
    if (value.isPresent()) {
      return String.format("WitResult{%s(%s)}", isOk ? "ok" : "err", value.get());
    }
    return String.format("WitResult{%s}", isOk ? "ok" : "err");
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitResult)) {
      return false;
    }
    final WitResult other = (WitResult) obj;
    return isOk == other.isOk && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), isOk, value);
  }
}
