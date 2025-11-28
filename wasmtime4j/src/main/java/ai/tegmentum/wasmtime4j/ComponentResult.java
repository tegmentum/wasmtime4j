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

package ai.tegmentum.wasmtime4j;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a Component Model result value.
 *
 * <p>A result type represents the outcome of an operation that can either succeed (ok) or fail
 * (err). Both the success and error cases can optionally carry payload values.
 *
 * <p>This is similar to Rust's {@code Result<T, E>} type or functional programming's Either type.
 *
 * @since 1.0.0
 */
public interface ComponentResult {

  /**
   * Checks if this result is an ok (success) value.
   *
   * @return true if this is an ok result
   */
  boolean isOk();

  /**
   * Checks if this result is an err (failure) value.
   *
   * @return true if this is an err result
   */
  boolean isErr();

  /**
   * Gets the ok value if this is a success result.
   *
   * @return the optional ok value
   */
  Optional<ComponentVal> getOk();

  /**
   * Gets the err value if this is a failure result.
   *
   * @return the optional err value
   */
  Optional<ComponentVal> getErr();

  /**
   * Gets the ok value, throwing an exception if this is an err result.
   *
   * @return the ok value (may be null if {@code result<_, E>})
   * @throws IllegalStateException if this is an err result
   */
  default ComponentVal unwrap() {
    if (isErr()) {
      throw new IllegalStateException("Called unwrap() on an err result: " + getErr().orElse(null));
    }
    return getOk().orElse(null);
  }

  /**
   * Gets the err value, throwing an exception if this is an ok result.
   *
   * @return the err value (may be null if {@code result<T, _>})
   * @throws IllegalStateException if this is an ok result
   */
  default ComponentVal unwrapErr() {
    if (isOk()) {
      throw new IllegalStateException(
          "Called unwrapErr() on an ok result: " + getOk().orElse(null));
    }
    return getErr().orElse(null);
  }

  /**
   * Maps the ok value using the provided function.
   *
   * @param mapper the mapping function
   * @return a new result with the mapped ok value, or the original err
   */
  default ComponentResult map(final Function<ComponentVal, ComponentVal> mapper) {
    if (isOk()) {
      final ComponentVal okVal = getOk().orElse(null);
      return ok(okVal != null ? mapper.apply(okVal) : null);
    }
    return this;
  }

  /**
   * Maps the err value using the provided function.
   *
   * @param mapper the mapping function
   * @return a new result with the mapped err value, or the original ok
   */
  default ComponentResult mapErr(final Function<ComponentVal, ComponentVal> mapper) {
    if (isErr()) {
      final ComponentVal errVal = getErr().orElse(null);
      return err(errVal != null ? mapper.apply(errVal) : null);
    }
    return this;
  }

  /**
   * Creates an ok result with the given value.
   *
   * @param value the success value (may be null for {@code result<_, E>})
   * @return a new ok result
   */
  static ComponentResult ok(final ComponentVal value) {
    return new Impl(true, value, null);
  }

  /**
   * Creates an ok result with no value.
   *
   * @return a new ok result
   */
  static ComponentResult ok() {
    return new Impl(true, null, null);
  }

  /**
   * Creates an err result with the given value.
   *
   * @param error the error value (may be null for {@code result<T, _>})
   * @return a new err result
   */
  static ComponentResult err(final ComponentVal error) {
    return new Impl(false, null, error);
  }

  /**
   * Creates an err result with no value.
   *
   * @return a new err result
   */
  static ComponentResult err() {
    return new Impl(false, null, null);
  }

  /** Default implementation of ComponentResult. */
  final class Impl implements ComponentResult {
    private final boolean isOk;
    private final ComponentVal okValue;
    private final ComponentVal errValue;

    Impl(final boolean isOk, final ComponentVal okValue, final ComponentVal errValue) {
      this.isOk = isOk;
      this.okValue = okValue;
      this.errValue = errValue;
    }

    @Override
    public boolean isOk() {
      return isOk;
    }

    @Override
    public boolean isErr() {
      return !isOk;
    }

    @Override
    public Optional<ComponentVal> getOk() {
      return isOk ? Optional.ofNullable(okValue) : Optional.empty();
    }

    @Override
    public Optional<ComponentVal> getErr() {
      return isOk ? Optional.empty() : Optional.ofNullable(errValue);
    }

    @Override
    public String toString() {
      if (isOk) {
        return "ok(" + okValue + ")";
      }
      return "err(" + errValue + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ComponentResult)) {
        return false;
      }
      final ComponentResult other = (ComponentResult) obj;
      if (isOk != other.isOk()) {
        return false;
      }
      if (isOk) {
        return Objects.equals(okValue, other.getOk().orElse(null));
      }
      return Objects.equals(errValue, other.getErr().orElse(null));
    }

    @Override
    public int hashCode() {
      return Objects.hash(isOk, okValue, errValue);
    }
  }
}
