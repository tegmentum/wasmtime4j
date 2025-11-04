package ai.tegmentum.wasmtime4j;

import java.util.Objects;

/**
 * Simple implementation of ValType that wraps a WasmValueType.
 *
 * <p>This implementation provides type querying and matching capabilities without requiring native
 * calls, as all operations can be performed purely in Java by delegating to WasmValueType.
 *
 * @since 1.0.0
 */
final class SimpleValType implements ValType {

  private final WasmValueType valueType;

  /**
   * Creates a new SimpleValType wrapping the given value type.
   *
   * @param valueType the WebAssembly value type
   * @throws IllegalArgumentException if valueType is null
   */
  SimpleValType(final WasmValueType valueType) {
    if (valueType == null) {
      throw new IllegalArgumentException("valueType cannot be null");
    }
    this.valueType = valueType;
  }

  @Override
  public WasmValueType getValueType() {
    return valueType;
  }

  @Override
  public boolean isNumeric() {
    return valueType.isNumeric();
  }

  @Override
  public boolean isInteger() {
    return valueType.isInteger();
  }

  @Override
  public boolean isFloat() {
    return valueType.isFloat();
  }

  @Override
  public boolean isReference() {
    return valueType.isReference();
  }

  @Override
  public boolean isVector() {
    return valueType.isVector();
  }

  @Override
  public boolean matches(final ValType other) {
    if (other == null) {
      return false;
    }

    final WasmValueType otherType = other.getValueType();

    // For non-reference types, matching is the same as equality
    if (!this.isReference() || !other.isReference()) {
      return this.valueType == otherType;
    }

    // Reference types: implement subtyping rules
    // For now, only exact matches are supported
    // TODO: Implement proper reference type subtyping when GC types are added
    return this.valueType == otherType;
  }

  @Override
  public boolean eq(final ValType other) {
    if (other == null) {
      return false;
    }
    return this.valueType == other.getValueType();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ValType)) {
      return false;
    }
    final ValType other = (ValType) obj;
    return this.eq(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valueType);
  }

  @Override
  public String toString() {
    return "ValType(" + valueType + ")";
  }

  /**
   * Static factory methods for creating ValType instances.
   */
  static final class Factory {

    private Factory() {
      // Utility class
    }

    /**
     * Creates a ValType from a WasmValueType.
     *
     * @param valueType the WebAssembly value type
     * @return a ValType wrapping the given value type
     * @throws IllegalArgumentException if valueType is null
     */
    static ValType from(final WasmValueType valueType) {
      return new SimpleValType(valueType);
    }

    /**
     * Creates an I32 value type.
     *
     * @return an I32 ValType
     */
    static ValType i32() {
      return new SimpleValType(WasmValueType.I32);
    }

    /**
     * Creates an I64 value type.
     *
     * @return an I64 ValType
     */
    static ValType i64() {
      return new SimpleValType(WasmValueType.I64);
    }

    /**
     * Creates an F32 value type.
     *
     * @return an F32 ValType
     */
    static ValType f32() {
      return new SimpleValType(WasmValueType.F32);
    }

    /**
     * Creates an F64 value type.
     *
     * @return an F64 ValType
     */
    static ValType f64() {
      return new SimpleValType(WasmValueType.F64);
    }

    /**
     * Creates a V128 value type.
     *
     * @return a V128 ValType
     */
    static ValType v128() {
      return new SimpleValType(WasmValueType.V128);
    }

    /**
     * Creates a FUNCREF value type.
     *
     * @return a FUNCREF ValType
     */
    static ValType funcref() {
      return new SimpleValType(WasmValueType.FUNCREF);
    }

    /**
     * Creates an EXTERNREF value type.
     *
     * @return an EXTERNREF ValType
     */
    static ValType externref() {
      return new SimpleValType(WasmValueType.EXTERNREF);
    }
  }
}
