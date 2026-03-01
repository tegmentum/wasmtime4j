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

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Objects;

/**
 * WebAssembly GC value representation.
 *
 * <p>Represents any value that can be stored in GC objects, including primitive types, packed
 * types, and reference types. Provides type-safe access and conversion methods.
 *
 * @since 1.0.0
 */
public abstract class GcValue {

  /** The type of this GC value including advanced SIMD from Task #307. */
  public enum Type {
    I32,
    I64,
    F32,
    F64,
    V128,
    REFERENCE,
    NULL
  }

  /**
   * Gets the type of this value.
   *
   * @return the value type
   */
  public abstract Type getType();

  /**
   * Checks if this is a null value.
   *
   * @return true if this is null
   */
  public boolean isNull() {
    return getType() == Type.NULL;
  }

  /**
   * Checks if this is a reference value.
   *
   * @return true if this is a reference
   */
  public boolean isReference() {
    return getType() == Type.REFERENCE;
  }

  /**
   * Gets this value as an I32.
   *
   * @return the I32 value
   * @throws IllegalStateException if not an I32
   */
  public int asI32() {
    throw new IllegalStateException("Value is not an I32");
  }

  /**
   * Gets this value as an I64.
   *
   * @return the I64 value
   * @throws IllegalStateException if not an I64
   */
  public long asI64() {
    throw new IllegalStateException("Value is not an I64");
  }

  /**
   * Gets this value as an F32.
   *
   * @return the F32 value
   * @throws IllegalStateException if not an F32
   */
  public float asF32() {
    throw new IllegalStateException("Value is not an F32");
  }

  /**
   * Gets this value as an F64.
   *
   * @return the F64 value
   * @throws IllegalStateException if not an F64
   */
  public double asF64() {
    throw new IllegalStateException("Value is not an F64");
  }

  /**
   * Gets this value as a V128 (16-byte array).
   *
   * @return the V128 value
   * @throws IllegalStateException if not a V128
   */
  public byte[] asV128() {
    throw new IllegalStateException("Value is not a V128");
  }

  /**
   * Gets this value as a reference.
   *
   * @return the reference value
   * @throws IllegalStateException if not a reference
   */
  public GcObject asReference() {
    throw new IllegalStateException("Value is not a reference");
  }

  /**
   * Converts this value to a WasmValue.
   *
   * @return the corresponding WasmValue
   */
  public abstract WasmValue toWasmValue();

  // Static factory methods

  /**
   * Create an I32 value.
   *
   * @param value the I32 value
   * @return the GC value
   */
  public static GcValue i32(final int value) {
    return new I32Value(value);
  }

  /**
   * Create an I64 value.
   *
   * @param value the I64 value
   * @return the GC value
   */
  public static GcValue i64(final long value) {
    return new I64Value(value);
  }

  /**
   * Create an F32 value.
   *
   * @param value the F32 value
   * @return the GC value
   */
  public static GcValue f32(final float value) {
    return new F32Value(value);
  }

  /**
   * Create an F64 value.
   *
   * @param value the F64 value
   * @return the GC value
   */
  public static GcValue f64(final double value) {
    return new F64Value(value);
  }

  /**
   * Create a V128 value.
   *
   * @param value the V128 value (16 bytes)
   * @return the GC value
   */
  public static GcValue v128(final byte[] value) {
    return new V128Value(value);
  }

  /**
   * Create a reference value.
   *
   * @param object the GC object
   * @return the GC value
   */
  public static GcValue reference(final GcObject object) {
    return new ReferenceValue(object);
  }

  /**
   * Create a null value.
   *
   * @return the null GC value
   */
  public static GcValue nullValue() {
    return NullValue.INSTANCE;
  }

  /**
   * Create a GC value from a Java object with automatic type conversion.
   *
   * @param obj the Java object
   * @return the corresponding GC value
   */
  public static GcValue fromObject(final Object obj) {
    if (obj == null) {
      return nullValue();
    }

    if (obj instanceof Integer) {
      return i32((Integer) obj);
    }
    if (obj instanceof Long) {
      return i64((Long) obj);
    }
    if (obj instanceof Float) {
      return f32((Float) obj);
    }
    if (obj instanceof Double) {
      return f64((Double) obj);
    }
    if (obj instanceof byte[]) {
      final byte[] bytes = (byte[]) obj;
      if (bytes.length == 16) {
        return v128(bytes);
      }
    }
    if (obj instanceof GcObject) {
      return reference((GcObject) obj);
    }
    if (obj instanceof GcValue) {
      return (GcValue) obj;
    }

    throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to GcValue");
  }

  // Implementation classes

  private static final class I32Value extends GcValue {
    private final int value;

    I32Value(final int value) {
      this.value = value;
    }

    @Override
    public Type getType() {
      return Type.I32;
    }

    @Override
    public int asI32() {
      return value;
    }

    @Override
    public WasmValue toWasmValue() {
      return WasmValue.i32(value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof I32Value)) {
        return false;
      }
      return value == ((I32Value) obj).value;
    }

    @Override
    public int hashCode() {
      return Integer.hashCode(value);
    }

    @Override
    public String toString() {
      return "i32(" + value + ")";
    }
  }

  private static final class I64Value extends GcValue {
    private final long value;

    I64Value(final long value) {
      this.value = value;
    }

    @Override
    public Type getType() {
      return Type.I64;
    }

    @Override
    public long asI64() {
      return value;
    }

    @Override
    public WasmValue toWasmValue() {
      return WasmValue.i64(value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof I64Value)) {
        return false;
      }
      return value == ((I64Value) obj).value;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(value);
    }

    @Override
    public String toString() {
      return "i64(" + value + ")";
    }
  }

  private static final class F32Value extends GcValue {
    private final float value;

    F32Value(final float value) {
      this.value = value;
    }

    @Override
    public Type getType() {
      return Type.F32;
    }

    @Override
    public float asF32() {
      return value;
    }

    @Override
    public WasmValue toWasmValue() {
      return WasmValue.f32(value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof F32Value)) {
        return false;
      }
      return Float.compare(value, ((F32Value) obj).value) == 0;
    }

    @Override
    public int hashCode() {
      return Float.hashCode(value);
    }

    @Override
    public String toString() {
      return "f32(" + value + ")";
    }
  }

  private static final class F64Value extends GcValue {
    private final double value;

    F64Value(final double value) {
      this.value = value;
    }

    @Override
    public Type getType() {
      return Type.F64;
    }

    @Override
    public double asF64() {
      return value;
    }

    @Override
    public WasmValue toWasmValue() {
      return WasmValue.f64(value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof F64Value)) {
        return false;
      }
      return Double.compare(value, ((F64Value) obj).value) == 0;
    }

    @Override
    public int hashCode() {
      return Double.hashCode(value);
    }

    @Override
    public String toString() {
      return "f64(" + value + ")";
    }
  }

  private static final class V128Value extends GcValue {
    private final byte[] value;

    V128Value(final byte[] value) {
      if (value.length != 16) {
        throw new IllegalArgumentException("V128 value must be exactly 16 bytes");
      }
      this.value = value.clone();
    }

    @Override
    public Type getType() {
      return Type.V128;
    }

    @Override
    public byte[] asV128() {
      return value.clone();
    }

    @Override
    public WasmValue toWasmValue() {
      // Convert bytes to long for V128 representation
      long low = 0;
      long high = 0;
      for (int i = 0; i < 8; i++) {
        low |= ((long) (value[i] & 0xFF)) << (i * 8);
        high |= ((long) (value[i + 8] & 0xFF)) << (i * 8);
      }
      return WasmValue.v128(high, low);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof V128Value)) {
        return false;
      }
      return java.util.Arrays.equals(value, ((V128Value) obj).value);
    }

    @Override
    public int hashCode() {
      return java.util.Arrays.hashCode(value);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("v128(");
      for (int i = 0; i < value.length; i++) {
        if (i > 0) {
          sb.append(" ");
        }
        sb.append(String.format("%02x", value[i] & 0xFF));
      }
      sb.append(")");
      return sb.toString();
    }
  }

  private static final class ReferenceValue extends GcValue {
    private final GcObject object;

    ReferenceValue(final GcObject object) {
      this.object = object;
    }

    @Override
    public Type getType() {
      return Type.REFERENCE;
    }

    @Override
    public boolean isNull() {
      return object == null || object.isNull();
    }

    @Override
    public GcObject asReference() {
      return object;
    }

    @Override
    public WasmValue toWasmValue() {
      return object != null ? object.toWasmValue() : WasmValue.externref(null);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ReferenceValue)) {
        return false;
      }
      return Objects.equals(object, ((ReferenceValue) obj).object);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(object);
    }

    @Override
    public String toString() {
      return "ref(" + object + ")";
    }
  }

  private static final class NullValue extends GcValue {
    static final NullValue INSTANCE = new NullValue();

    private NullValue() {}

    @Override
    public Type getType() {
      return Type.NULL;
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public WasmValue toWasmValue() {
      return WasmValue.externref(null);
    }

    @Override
    public boolean equals(final Object obj) {
      return obj instanceof NullValue;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public String toString() {
      return "null";
    }
  }
}
