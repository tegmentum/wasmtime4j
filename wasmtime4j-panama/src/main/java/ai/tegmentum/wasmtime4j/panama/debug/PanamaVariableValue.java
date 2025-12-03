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

package ai.tegmentum.wasmtime4j.panama.debug;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a variable value in WebAssembly debugging context using Panama FFI.
 *
 * <p>This class provides a type-safe container for WebAssembly values of different types including
 * integers, floats, vectors, and references.
 *
 * @since 1.0.0
 */
public final class PanamaVariableValue {

  private final ValueType type;
  private final Object value;

  private PanamaVariableValue(final ValueType type, final Object value) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.value = value;
  }

  /**
   * Creates an i32 value.
   *
   * @param value the integer value
   * @return a new i32 value
   */
  public static PanamaVariableValue i32(final int value) {
    return new PanamaVariableValue(ValueType.I32, value);
  }

  /**
   * Creates an i64 value.
   *
   * @param value the long value
   * @return a new i64 value
   */
  public static PanamaVariableValue i64(final long value) {
    return new PanamaVariableValue(ValueType.I64, value);
  }

  /**
   * Creates an f32 value.
   *
   * @param value the float value
   * @return a new f32 value
   */
  public static PanamaVariableValue f32(final float value) {
    return new PanamaVariableValue(ValueType.F32, value);
  }

  /**
   * Creates an f64 value.
   *
   * @param value the double value
   * @return a new f64 value
   */
  public static PanamaVariableValue f64(final double value) {
    return new PanamaVariableValue(ValueType.F64, value);
  }

  /**
   * Creates a v128 value.
   *
   * @param bytes the 16-byte vector value
   * @return a new v128 value
   */
  public static PanamaVariableValue v128(final byte[] bytes) {
    if (bytes == null || bytes.length != 16) {
      throw new IllegalArgumentException("v128 requires exactly 16 bytes");
    }
    return new PanamaVariableValue(ValueType.V128, Arrays.copyOf(bytes, 16));
  }

  /**
   * Creates a funcref value.
   *
   * @param funcIndex the function index, or null for null reference
   * @return a new funcref value
   */
  public static PanamaVariableValue funcRef(final Integer funcIndex) {
    return new PanamaVariableValue(ValueType.FUNCREF, funcIndex);
  }

  /**
   * Creates an externref value.
   *
   * @param refValue the external reference value, or null
   * @return a new externref value
   */
  public static PanamaVariableValue externRef(final Long refValue) {
    return new PanamaVariableValue(ValueType.EXTERNREF, refValue);
  }

  /**
   * Creates a memory reference value.
   *
   * @param address the memory address
   * @param size the memory size
   * @return a new memory value
   */
  public static PanamaVariableValue memory(final long address, final long size) {
    return new PanamaVariableValue(ValueType.MEMORY, new long[] {address, size});
  }

  /**
   * Creates a complex value with JSON representation.
   *
   * @param json the JSON representation
   * @return a new complex value
   */
  public static PanamaVariableValue complex(final String json) {
    return new PanamaVariableValue(ValueType.COMPLEX, json);
  }

  /**
   * Gets the value type.
   *
   * @return the type
   */
  public ValueType getType() {
    return type;
  }

  /**
   * Gets the value as an integer.
   *
   * @return the i32 value
   * @throws IllegalStateException if not an i32
   */
  public int asI32() {
    if (type != ValueType.I32) {
      throw new IllegalStateException("Value is not an i32, it is " + type);
    }
    return (Integer) value;
  }

  /**
   * Gets the value as a long.
   *
   * @return the i64 value
   * @throws IllegalStateException if not an i64
   */
  public long asI64() {
    if (type != ValueType.I64) {
      throw new IllegalStateException("Value is not an i64, it is " + type);
    }
    return (Long) value;
  }

  /**
   * Gets the value as a float.
   *
   * @return the f32 value
   * @throws IllegalStateException if not an f32
   */
  public float asF32() {
    if (type != ValueType.F32) {
      throw new IllegalStateException("Value is not an f32, it is " + type);
    }
    return (Float) value;
  }

  /**
   * Gets the value as a double.
   *
   * @return the f64 value
   * @throws IllegalStateException if not an f64
   */
  public double asF64() {
    if (type != ValueType.F64) {
      throw new IllegalStateException("Value is not an f64, it is " + type);
    }
    return (Double) value;
  }

  /**
   * Gets the value as a v128 byte array.
   *
   * @return a copy of the v128 bytes
   * @throws IllegalStateException if not a v128
   */
  public byte[] asV128() {
    if (type != ValueType.V128) {
      throw new IllegalStateException("Value is not a v128, it is " + type);
    }
    return Arrays.copyOf((byte[]) value, 16);
  }

  /**
   * Gets the value as a function reference.
   *
   * @return the function index, or null
   * @throws IllegalStateException if not a funcref
   */
  public Integer asFuncRef() {
    if (type != ValueType.FUNCREF) {
      throw new IllegalStateException("Value is not a funcref, it is " + type);
    }
    return (Integer) value;
  }

  /**
   * Gets the value as an external reference.
   *
   * @return the external reference, or null
   * @throws IllegalStateException if not an externref
   */
  public Long asExternRef() {
    if (type != ValueType.EXTERNREF) {
      throw new IllegalStateException("Value is not an externref, it is " + type);
    }
    return (Long) value;
  }

  /**
   * Gets the memory address.
   *
   * @return the memory address
   * @throws IllegalStateException if not a memory value
   */
  public long getMemoryAddress() {
    if (type != ValueType.MEMORY) {
      throw new IllegalStateException("Value is not a memory reference, it is " + type);
    }
    return ((long[]) value)[0];
  }

  /**
   * Gets the memory size.
   *
   * @return the memory size
   * @throws IllegalStateException if not a memory value
   */
  public long getMemorySize() {
    if (type != ValueType.MEMORY) {
      throw new IllegalStateException("Value is not a memory reference, it is " + type);
    }
    return ((long[]) value)[1];
  }

  /**
   * Gets the value as a complex JSON string.
   *
   * @return the JSON representation
   * @throws IllegalStateException if not a complex value
   */
  public String asComplex() {
    if (type != ValueType.COMPLEX) {
      throw new IllegalStateException("Value is not a complex type, it is " + type);
    }
    return (String) value;
  }

  /**
   * Checks if this value is null (for references).
   *
   * @return true if null reference
   */
  public boolean isNull() {
    if (type == ValueType.FUNCREF || type == ValueType.EXTERNREF) {
      return value == null;
    }
    return false;
  }

  @Override
  public String toString() {
    switch (type) {
      case I32:
        return "i32(" + value + ")";
      case I64:
        return "i64(" + value + ")";
      case F32:
        return "f32(" + value + ")";
      case F64:
        return "f64(" + value + ")";
      case V128:
        return "v128(" + bytesToHex((byte[]) value) + ")";
      case FUNCREF:
        return value == null ? "funcref(null)" : "funcref(" + value + ")";
      case EXTERNREF:
        return value == null ? "externref(null)" : "externref(" + value + ")";
      case MEMORY:
        final long[] mem = (long[]) value;
        return "memory(addr=" + mem[0] + ", size=" + mem[1] + ")";
      case COMPLEX:
        return "complex(" + value + ")";
      default:
        return "unknown(" + value + ")";
    }
  }

  private static String bytesToHex(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (final byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PanamaVariableValue)) {
      return false;
    }
    final PanamaVariableValue other = (PanamaVariableValue) obj;
    if (type != other.type) {
      return false;
    }
    if (type == ValueType.V128) {
      return Arrays.equals((byte[]) value, (byte[]) other.value);
    }
    if (type == ValueType.MEMORY) {
      return Arrays.equals((long[]) value, (long[]) other.value);
    }
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    if (type == ValueType.V128) {
      return Objects.hash(type, Arrays.hashCode((byte[]) value));
    }
    if (type == ValueType.MEMORY) {
      return Objects.hash(type, Arrays.hashCode((long[]) value));
    }
    return Objects.hash(type, value);
  }

  /** Value type enumeration. */
  public enum ValueType {
    /** 32-bit integer. */
    I32,
    /** 64-bit integer. */
    I64,
    /** 32-bit float. */
    F32,
    /** 64-bit float. */
    F64,
    /** 128-bit vector. */
    V128,
    /** Function reference. */
    FUNCREF,
    /** External reference. */
    EXTERNREF,
    /** Memory reference (address + size). */
    MEMORY,
    /** Complex type (JSON representation). */
    COMPLEX
  }
}
