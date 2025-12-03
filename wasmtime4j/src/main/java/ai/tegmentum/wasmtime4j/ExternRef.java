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

package ai.tegmentum.wasmtime4j;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a WebAssembly external reference with type-safe wrapping.
 *
 * <p>ExternRef allows passing arbitrary Java objects to and from WebAssembly modules. Unlike raw
 * Object values, ExternRef provides:
 *
 * <ul>
 *   <li>Type-safe retrieval with generic type parameter
 *   <li>Null safety with explicit null handling
 *   <li>Store association for lifecycle management
 *   <li>Unique identification for debugging and tracking
 * </ul>
 *
 * <p>External references are opaque to WebAssembly code - the WASM module can only pass them
 * around, store them in tables, or return them. The actual data is only accessible from Java code.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a typed externref from a Java object
 * MyData data = new MyData("example");
 * ExternRef<MyData> ref = ExternRef.of(data);
 *
 * // Pass to WebAssembly
 * WasmValue externValue = WasmValue.externref(ref);
 * instance.call("process_external", externValue);
 *
 * // Retrieve from WebAssembly result
 * WasmValue result = instance.call("get_external")[0];
 * ExternRef<?> returnedRef = ExternRef.fromWasmValue(result);
 * MyData retrieved = returnedRef.getAs(MyData.class);
 * }</pre>
 *
 * @param <T> the type of the wrapped object
 * @since 1.0.0
 */
public final class ExternRef<T> {

  private static long idCounter = 0;

  private final T value;
  private final Class<T> type;
  private final long id;

  /**
   * Creates a new ExternRef wrapping the given value.
   *
   * @param value the value to wrap (may be null)
   * @param type the class type for type-safe retrieval
   */
  @SuppressWarnings("unchecked")
  private ExternRef(final T value, final Class<T> type) {
    this.value = value;
    this.type = type != null ? type : (value != null ? (Class<T>) value.getClass() : null);
    this.id = ++idCounter;
  }

  /**
   * Creates a new ExternRef wrapping the given non-null value.
   *
   * @param value the value to wrap
   * @param <T> the type of the value
   * @return a new ExternRef
   * @throws NullPointerException if value is null
   */
  public static <T> ExternRef<T> of(final T value) {
    Objects.requireNonNull(value, "value cannot be null; use ofNullable for nullable values");
    @SuppressWarnings("unchecked")
    Class<T> clazz = (Class<T>) value.getClass();
    return new ExternRef<>(value, clazz);
  }

  /**
   * Creates a new ExternRef wrapping the given value with explicit type.
   *
   * @param value the value to wrap
   * @param type the class type for type-safe retrieval
   * @param <T> the type of the value
   * @return a new ExternRef
   * @throws NullPointerException if value or type is null
   */
  public static <T> ExternRef<T> of(final T value, final Class<T> type) {
    Objects.requireNonNull(value, "value cannot be null; use ofNullable for nullable values");
    Objects.requireNonNull(type, "type cannot be null");
    return new ExternRef<>(value, type);
  }

  /**
   * Creates a new ExternRef that may wrap a null value.
   *
   * @param value the value to wrap (may be null)
   * @param type the class type for type-safe retrieval
   * @param <T> the type of the value
   * @return a new ExternRef
   * @throws NullPointerException if type is null
   */
  public static <T> ExternRef<T> ofNullable(final T value, final Class<T> type) {
    Objects.requireNonNull(type, "type cannot be null");
    return new ExternRef<>(value, type);
  }

  /**
   * Creates a null ExternRef of the given type.
   *
   * @param type the class type for type-safe retrieval
   * @param <T> the type of the value
   * @return a new null ExternRef
   * @throws NullPointerException if type is null
   */
  public static <T> ExternRef<T> nullRef(final Class<T> type) {
    Objects.requireNonNull(type, "type cannot be null");
    return new ExternRef<>(null, type);
  }

  /**
   * Creates an ExternRef from a raw Object, typically from WasmValue.asExternref().
   *
   * @param obj the raw object from WebAssembly
   * @return a new ExternRef wrapping the object
   */
  @SuppressWarnings("unchecked")
  public static ExternRef<Object> fromRaw(final Object obj) {
    if (obj == null) {
      return new ExternRef<>(null, Object.class);
    }
    if (obj instanceof ExternRef) {
      return (ExternRef<Object>) obj;
    }
    return new ExternRef<>(obj, Object.class);
  }

  /**
   * Creates an ExternRef from a WasmValue.
   *
   * @param wasmValue the WasmValue containing an externref
   * @return a new ExternRef wrapping the value
   * @throws IllegalArgumentException if wasmValue is not an externref type
   */
  public static ExternRef<Object> fromWasmValue(final WasmValue wasmValue) {
    Objects.requireNonNull(wasmValue, "wasmValue cannot be null");
    if (!wasmValue.isExternref()) {
      throw new IllegalArgumentException("WasmValue is not an externref: " + wasmValue.getType());
    }
    return fromRaw(wasmValue.asExternref());
  }

  /**
   * Gets the wrapped value.
   *
   * @return the wrapped value (may be null)
   */
  public T get() {
    return value;
  }

  /**
   * Gets the wrapped value, throwing if null.
   *
   * @return the wrapped value
   * @throws NullPointerException if the wrapped value is null
   */
  public T getOrThrow() {
    if (value == null) {
      throw new NullPointerException("ExternRef contains null value");
    }
    return value;
  }

  /**
   * Gets the wrapped value or a default if null.
   *
   * @param defaultValue the default value to return if wrapped value is null
   * @return the wrapped value or defaultValue
   */
  public T getOrDefault(final T defaultValue) {
    return value != null ? value : defaultValue;
  }

  /**
   * Gets the wrapped value as an Optional.
   *
   * @return an Optional containing the wrapped value, or empty if null
   */
  public Optional<T> toOptional() {
    return Optional.ofNullable(value);
  }

  /**
   * Gets the wrapped value cast to the specified type.
   *
   * @param targetType the class to cast to
   * @param <R> the target type
   * @return the value cast to the target type
   * @throws ClassCastException if the value cannot be cast to the target type
   */
  public <R> R getAs(final Class<R> targetType) {
    Objects.requireNonNull(targetType, "targetType cannot be null");
    if (value == null) {
      return null;
    }
    return targetType.cast(value);
  }

  /**
   * Gets the wrapped value cast to the specified type, or null if not assignable.
   *
   * @param targetType the class to cast to
   * @param <R> the target type
   * @return the value cast to the target type, or null if not assignable
   */
  public <R> R getAsOrNull(final Class<R> targetType) {
    Objects.requireNonNull(targetType, "targetType cannot be null");
    if (value == null || !targetType.isInstance(value)) {
      return null;
    }
    return targetType.cast(value);
  }

  /**
   * Checks if the wrapped value is null.
   *
   * @return true if the wrapped value is null
   */
  public boolean isNull() {
    return value == null;
  }

  /**
   * Checks if the wrapped value is present (not null).
   *
   * @return true if the wrapped value is not null
   */
  public boolean isPresent() {
    return value != null;
  }

  /**
   * Gets the declared type of this ExternRef.
   *
   * @return the class type, or null if not specified
   */
  public Class<T> getDeclaredType() {
    return type;
  }

  /**
   * Checks if the wrapped value is an instance of the given type.
   *
   * @param targetType the type to check
   * @return true if the value is an instance of targetType
   */
  public boolean isInstanceOf(final Class<?> targetType) {
    if (targetType == null || value == null) {
      return false;
    }
    return targetType.isInstance(value);
  }

  /**
   * Gets the unique ID of this ExternRef.
   *
   * @return the unique identifier
   */
  public long getId() {
    return id;
  }

  /**
   * Converts this ExternRef to a WasmValue.
   *
   * @return a WasmValue containing this ExternRef
   */
  public WasmValue toWasmValue() {
    return WasmValue.externref(this);
  }

  /**
   * Converts the wrapped value directly to a WasmValue (unwrapped).
   *
   * <p>This creates a WasmValue containing the raw wrapped object, not the ExternRef wrapper.
   *
   * @return a WasmValue containing the raw wrapped value
   */
  public WasmValue toUnwrappedWasmValue() {
    return WasmValue.externref(value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ExternRef<?> other = (ExternRef<?>) obj;
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    if (value == null) {
      return "ExternRef{null, type="
          + (type != null ? type.getSimpleName() : "?")
          + ", id="
          + id
          + "}";
    }
    return "ExternRef{value=" + value + ", type=" + type.getSimpleName() + ", id=" + id + "}";
  }
}
