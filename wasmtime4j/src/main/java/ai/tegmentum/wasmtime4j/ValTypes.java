package ai.tegmentum.wasmtime4j;

/**
 * Utility class for creating ValType instances.
 *
 * <p>This class provides static factory methods for creating ValType objects representing
 * WebAssembly value types.
 *
 * @since 1.0.0
 */
public final class ValTypes {

  private ValTypes() {
    // Utility class
  }

  /**
   * Creates a ValType from a WasmValueType.
   *
   * @param valueType the WebAssembly value type
   * @return a ValType wrapping the given value type
   * @throws IllegalArgumentException if valueType is null
   */
  public static ValType from(final WasmValueType valueType) {
    return SimpleValType.Factory.from(valueType);
  }

  /**
   * Creates an I32 value type.
   *
   * @return an I32 ValType
   */
  public static ValType i32() {
    return SimpleValType.Factory.i32();
  }

  /**
   * Creates an I64 value type.
   *
   * @return an I64 ValType
   */
  public static ValType i64() {
    return SimpleValType.Factory.i64();
  }

  /**
   * Creates an F32 value type.
   *
   * @return an F32 ValType
   */
  public static ValType f32() {
    return SimpleValType.Factory.f32();
  }

  /**
   * Creates an F64 value type.
   *
   * @return an F64 ValType
   */
  public static ValType f64() {
    return SimpleValType.Factory.f64();
  }

  /**
   * Creates a V128 value type.
   *
   * @return a V128 ValType
   */
  public static ValType v128() {
    return SimpleValType.Factory.v128();
  }

  /**
   * Creates a FUNCREF value type.
   *
   * @return a FUNCREF ValType
   */
  public static ValType funcref() {
    return SimpleValType.Factory.funcref();
  }

  /**
   * Creates an EXTERNREF value type.
   *
   * @return an EXTERNREF ValType
   */
  public static ValType externref() {
    return SimpleValType.Factory.externref();
  }
}
