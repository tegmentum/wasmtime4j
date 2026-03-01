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
package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

/**
 * WebAssembly GC I31 instance.
 *
 * <p>Represents an immediate 31-bit signed integer stored as a reference. I31 values provide
 * efficient storage for small integers without heap allocation while maintaining reference
 * semantics for equality comparison.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * I31Instance value = runtime.createI31(42);
 * int intValue = value.getValue();
 * int unsignedValue = value.getUnsignedValue();
 *
 * boolean equal = value.refEquals(runtime.createI31(42)); // false (different objects)
 * boolean valueEqual = value.getValue() == 42; // true (same value)
 * }</pre>
 *
 * @since 1.0.0
 */
public interface I31Instance extends GcObject {

  /**
   * Creates an I31 from a signed integer value (checked).
   *
   * <p>This is a convenience factory that uses the default runtime.
   *
   * @param value the signed integer value (must fit in 31 bits)
   * @return the I31 instance
   * @throws GcException if the value is out of range or creation fails
   * @since 1.1.0
   */
  static I31Instance ofSigned(final int value) throws GcException {
    try {
      return WasmRuntimeFactory.create().getGcRuntime().createI31(value);
    } catch (final WasmException e) {
      throw new GcException("Failed to create I31 instance", e);
    }
  }

  /**
   * Creates an I31 from an unsigned integer value (checked).
   *
   * <p>This is a convenience factory that uses the default runtime.
   *
   * @param value the unsigned integer value (must be 0 to 2^31-1)
   * @return the I31 instance
   * @throws GcException if the value is out of range or creation fails
   * @since 1.1.0
   */
  static I31Instance ofUnsigned(final int value) throws GcException {
    try {
      return WasmRuntimeFactory.create().getGcRuntime().createI31Unsigned(value);
    } catch (final WasmException e) {
      throw new GcException("Failed to create I31 instance", e);
    }
  }

  /**
   * Creates an I31 from a signed integer value (wrapping, truncates to 31 bits).
   *
   * <p>This is a convenience factory that uses the default runtime.
   *
   * @param value the signed integer value (will be truncated to 31 bits)
   * @return the I31 instance
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  static I31Instance wrappingSigned(final int value) throws GcException {
    try {
      return WasmRuntimeFactory.create().getGcRuntime().createI31Wrapping(value);
    } catch (final WasmException e) {
      throw new GcException("Failed to create I31 instance", e);
    }
  }

  /**
   * Creates an I31 from an unsigned integer value (wrapping, truncates to 31 bits).
   *
   * <p>This is a convenience factory that uses the default runtime.
   *
   * @param value the unsigned integer value (will be truncated to 31 bits)
   * @return the I31 instance
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  static I31Instance wrappingUnsigned(final int value) throws GcException {
    try {
      return WasmRuntimeFactory.create().getGcRuntime().createI31WrappingUnsigned(value);
    } catch (final WasmException e) {
      throw new GcException("Failed to create I31 instance", e);
    }
  }

  /**
   * Gets the signed 31-bit integer value.
   *
   * @return the signed value
   */
  int getValue();

  /**
   * Gets the signed 31-bit integer value.
   *
   * @return the signed value
   */
  int getSignedValue();

  /**
   * Gets the unsigned 31-bit integer value.
   *
   * @return the unsigned value
   */
  int getUnsignedValue();
}
