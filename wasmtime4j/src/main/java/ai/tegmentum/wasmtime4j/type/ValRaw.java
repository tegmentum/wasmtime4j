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
package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Objects;

/**
 * Raw representation of a WebAssembly value for low-level operations.
 *
 * <p>ValRaw provides a type-unsafe but efficient way to work with WebAssembly values. Unlike {@link
 * WasmValue} which provides type-safe access, ValRaw stores all values as raw bits, allowing for
 * zero-copy interop with native code.
 *
 * <p><b>Warning:</b> Using ValRaw incorrectly can lead to undefined behavior or crashes. This API
 * is intended for advanced use cases where type safety can be guaranteed externally and maximum
 * performance is required.
 *
 * <p>The raw value is stored as a 128-bit quantity (two longs) to accommodate all WebAssembly types
 * including v128 (SIMD).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create from typed values
 * ValRaw i32Val = ValRaw.i32(42);
 * ValRaw f64Val = ValRaw.f64(3.14159);
 *
 * // Convert to typed values (caller must ensure correct type)
 * int i = i32Val.asI32();
 * double d = f64Val.asF64();
 *
 * // Low-level access to raw bits
 * long lowBits = i32Val.getLowBits();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ValRaw {

  private final long lowBits;
  private final long highBits;

  private ValRaw(final long lowBits, final long highBits) {
    this.lowBits = lowBits;
    this.highBits = highBits;
  }

  /**
   * Creates a ValRaw containing an i32 value.
   *
   * @param value the i32 value
   * @return a new ValRaw
   */
  public static ValRaw i32(final int value) {
    return new ValRaw(Integer.toUnsignedLong(value), 0);
  }

  /**
   * Creates a ValRaw containing an i64 value.
   *
   * @param value the i64 value
   * @return a new ValRaw
   */
  public static ValRaw i64(final long value) {
    return new ValRaw(value, 0);
  }

  /**
   * Creates a ValRaw containing an f32 value.
   *
   * @param value the f32 value
   * @return a new ValRaw
   */
  public static ValRaw f32(final float value) {
    return new ValRaw(Integer.toUnsignedLong(Float.floatToRawIntBits(value)), 0);
  }

  /**
   * Creates a ValRaw containing an f64 value.
   *
   * @param value the f64 value
   * @return a new ValRaw
   */
  public static ValRaw f64(final double value) {
    return new ValRaw(Double.doubleToRawLongBits(value), 0);
  }

  /**
   * Creates a ValRaw containing a v128 value.
   *
   * @param lowBits the low 64 bits
   * @param highBits the high 64 bits
   * @return a new ValRaw
   */
  public static ValRaw v128(final long lowBits, final long highBits) {
    return new ValRaw(lowBits, highBits);
  }

  /**
   * Creates a ValRaw containing a funcref index.
   *
   * @param funcIndex the function index, or -1 for null
   * @return a new ValRaw
   */
  public static ValRaw funcref(final int funcIndex) {
    return new ValRaw(Integer.toUnsignedLong(funcIndex), 0);
  }

  /**
   * Creates a ValRaw containing an externref pointer.
   *
   * @param ptr the externref pointer value
   * @return a new ValRaw
   */
  public static ValRaw externref(final long ptr) {
    return new ValRaw(ptr, 0);
  }

  /**
   * Creates a ValRaw containing a GC reference pointer.
   *
   * @param ptr the GC reference pointer value
   * @return a new ValRaw
   */
  public static ValRaw anyref(final long ptr) {
    return new ValRaw(ptr, 0);
  }

  /**
   * Creates a null funcref.
   *
   * @return a ValRaw representing null funcref
   */
  public static ValRaw nullFuncref() {
    return new ValRaw(-1, 0);
  }

  /**
   * Creates a null externref.
   *
   * @return a ValRaw representing null externref
   */
  public static ValRaw nullExternref() {
    return new ValRaw(0, 0);
  }

  /**
   * Creates a ValRaw from raw bit representation.
   *
   * @param lowBits the low 64 bits
   * @param highBits the high 64 bits
   * @return a new ValRaw
   */
  public static ValRaw fromRawBits(final long lowBits, final long highBits) {
    return new ValRaw(lowBits, highBits);
  }

  /**
   * Gets the low 64 bits of this value.
   *
   * @return the low bits
   */
  public long getLowBits() {
    return lowBits;
  }

  /**
   * Gets the high 64 bits of this value.
   *
   * @return the high bits
   */
  public long getHighBits() {
    return highBits;
  }

  /**
   * Interprets this value as an i32.
   *
   * <p><b>Warning:</b> No type checking is performed. Caller must ensure this contains an i32.
   *
   * @return the i32 value
   */
  public int asI32() {
    return (int) lowBits;
  }

  /**
   * Interprets this value as a u32 (unsigned i32 as long).
   *
   * @return the u32 value as a long
   */
  public long asU32() {
    return lowBits & 0xFFFFFFFFL;
  }

  /**
   * Interprets this value as an i64.
   *
   * <p><b>Warning:</b> No type checking is performed. Caller must ensure this contains an i64.
   *
   * @return the i64 value
   */
  public long asI64() {
    return lowBits;
  }

  /**
   * Interprets this value as an f32.
   *
   * <p><b>Warning:</b> No type checking is performed. Caller must ensure this contains an f32.
   *
   * @return the f32 value
   */
  public float asF32() {
    return Float.intBitsToFloat((int) lowBits);
  }

  /**
   * Interprets this value as an f64.
   *
   * <p><b>Warning:</b> No type checking is performed. Caller must ensure this contains an f64.
   *
   * @return the f64 value
   */
  public double asF64() {
    return Double.longBitsToDouble(lowBits);
  }

  /**
   * Gets the v128 low bits.
   *
   * @return the low 64 bits of the v128
   */
  public long asV128Low() {
    return lowBits;
  }

  /**
   * Gets the v128 high bits.
   *
   * @return the high 64 bits of the v128
   */
  public long asV128High() {
    return highBits;
  }

  /**
   * Interprets this value as a funcref index.
   *
   * @return the function index, or -1 for null
   */
  public int asFuncrefIndex() {
    return (int) lowBits;
  }

  /**
   * Interprets this value as an externref pointer.
   *
   * @return the externref pointer
   */
  public long asExternrefPtr() {
    return lowBits;
  }

  /**
   * Interprets this value as a GC reference pointer.
   *
   * @return the GC reference pointer
   */
  public long asAnyrefPtr() {
    return lowBits;
  }

  /**
   * Converts this raw value to a typed WasmValue.
   *
   * @param type the expected type
   * @return a typed WasmValue
   * @throws IllegalArgumentException if type is null
   */
  public WasmValue toWasmValue(final WasmValueType type) {
    Objects.requireNonNull(type, "type cannot be null");
    switch (type) {
      case I32:
        return WasmValue.i32(asI32());
      case I64:
        return WasmValue.i64(asI64());
      case F32:
        return WasmValue.f32(asF32());
      case F64:
        return WasmValue.f64(asF64());
      case V128:
        return WasmValue.v128(lowBits, highBits);
      case FUNCREF:
        return asFuncrefIndex() == -1
            ? WasmValue.nullFuncref()
            : WasmValue.funcref(asFuncrefIndex());
      case EXTERNREF:
        return asExternrefPtr() == 0
            ? WasmValue.nullExternref()
            : WasmValue.externref(asExternrefPtr());
      default:
        throw new IllegalArgumentException("Unsupported type for conversion: " + type);
    }
  }

  /**
   * Creates a ValRaw from a typed WasmValue.
   *
   * @param value the typed value
   * @return a new ValRaw
   * @throws IllegalArgumentException if value is null
   */
  public static ValRaw fromWasmValue(final WasmValue value) {
    Objects.requireNonNull(value, "value cannot be null");
    switch (value.getType()) {
      case I32:
        return i32(value.asInt());
      case I64:
        return i64(value.asLong());
      case F32:
        return f32(value.asFloat());
      case F64:
        return f64(value.asDouble());
      case V128:
        byte[] v128Bytes = value.asV128();
        // Convert byte[] to two longs (little-endian)
        long low = bytesToLong(v128Bytes, 0);
        long high = bytesToLong(v128Bytes, 8);
        return v128(low, high);
      case FUNCREF:
        Object func = value.asFuncref();
        return func == null ? nullFuncref() : funcref(System.identityHashCode(func));
      case EXTERNREF:
        Object ext = value.asExternref();
        return ext == null ? nullExternref() : externref(System.identityHashCode(ext));
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ValRaw)) {
      return false;
    }
    ValRaw other = (ValRaw) obj;
    return lowBits == other.lowBits && highBits == other.highBits;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lowBits, highBits);
  }

  @Override
  public String toString() {
    return "ValRaw{low=0x"
        + Long.toHexString(lowBits)
        + ", high=0x"
        + Long.toHexString(highBits)
        + "}";
  }

  /**
   * Converts 8 bytes from a byte array to a long (little-endian).
   *
   * @param bytes the byte array
   * @param offset the starting offset
   * @return the long value
   */
  private static long bytesToLong(final byte[] bytes, final int offset) {
    long result = 0;
    for (int i = 0; i < 8; i++) {
      result |= ((long) (bytes[offset + i] & 0xFF)) << (i * 8);
    }
    return result;
  }
}
