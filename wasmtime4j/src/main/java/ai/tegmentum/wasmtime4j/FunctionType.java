package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly function type.
 *
 * <p>A function type describes the signature of a WebAssembly function, including parameter types
 * and return types.
 *
 * @since 1.0.0
 */
public final class FunctionType implements WasmType {

  private final WasmValueType[] paramTypes;
  private final WasmValueType[] returnTypes;

  /**
   * Creates a new function type.
   *
   * @param paramTypes the parameter types (must not be null)
   * @param returnTypes the return types (must not be null)
   * @throws IllegalArgumentException if paramTypes or returnTypes is null, or contains null
   *     elements
   */
  public FunctionType(final WasmValueType[] paramTypes, final WasmValueType[] returnTypes) {
    if (paramTypes == null) {
      throw new IllegalArgumentException("Parameter types cannot be null");
    }
    if (returnTypes == null) {
      throw new IllegalArgumentException("Return types cannot be null");
    }

    // Validate parameter types
    for (int i = 0; i < paramTypes.length; i++) {
      if (paramTypes[i] == null) {
        throw new IllegalArgumentException("Parameter type at index " + i + " cannot be null");
      }
    }

    // Validate return types
    for (int i = 0; i < returnTypes.length; i++) {
      if (returnTypes[i] == null) {
        throw new IllegalArgumentException("Return type at index " + i + " cannot be null");
      }
    }

    this.paramTypes = paramTypes.clone();
    this.returnTypes = returnTypes.clone();
  }

  /**
   * Gets the parameter types.
   *
   * @return array of parameter types
   */
  public WasmValueType[] getParamTypes() {
    return paramTypes.clone();
  }

  /**
   * Gets the return types.
   *
   * @return array of return types
   */
  public WasmValueType[] getReturnTypes() {
    return returnTypes.clone();
  }

  /**
   * Gets the number of parameters.
   *
   * @return parameter count
   */
  public int getParamCount() {
    return paramTypes.length;
  }

  /**
   * Gets the number of return values.
   *
   * @return return value count
   */
  public int getReturnCount() {
    return returnTypes.length;
  }

  /**
   * Checks if this function has multiple return values.
   *
   * @return true if this function returns multiple values, false otherwise
   */
  public boolean hasMultipleReturns() {
    return returnTypes.length > 1;
  }

  /**
   * Validates that the provided parameters match this function's signature.
   *
   * @param params the parameters to validate
   * @throws IllegalArgumentException if parameters don't match the signature
   */
  public void validateParameters(final WasmValue[] params) {
    if (params == null) {
      if (paramTypes.length > 0) {
        throw new IllegalArgumentException(
            "Expected " + paramTypes.length + " parameters, got null");
      }
      return;
    }

    if (params.length != paramTypes.length) {
      throw new IllegalArgumentException(
          "Parameter count mismatch: expected " + paramTypes.length + ", got " + params.length);
    }

    for (int i = 0; i < params.length; i++) {
      if (params[i] == null) {
        throw new IllegalArgumentException("Parameter at index " + i + " is null");
      }
      params[i].validateType(paramTypes[i]);
    }
  }

  /**
   * Checks if this function type is compatible with another function type. Two function types are
   * compatible if they have the same parameter and return types.
   *
   * @param other the other function type
   * @return true if compatible, false otherwise
   */
  public boolean isCompatibleWith(final FunctionType other) {
    if (other == null) {
      return false;
    }

    if (paramTypes.length != other.paramTypes.length
        || returnTypes.length != other.returnTypes.length) {
      return false;
    }

    for (int i = 0; i < paramTypes.length; i++) {
      if (paramTypes[i] != other.paramTypes[i]) {
        return false;
      }
    }

    for (int i = 0; i < returnTypes.length; i++) {
      if (returnTypes[i] != other.returnTypes[i]) {
        return false;
      }
    }

    return true;
  }

  @Override
  public WasmTypeKind getKind() {
    return WasmTypeKind.FUNCTION;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("FunctionType{params=[");
    for (int i = 0; i < paramTypes.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(paramTypes[i]);
    }
    sb.append("], returns=[");
    for (int i = 0; i < returnTypes.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(returnTypes[i]);
    }
    sb.append("]}");
    return sb.toString();
  }
}
