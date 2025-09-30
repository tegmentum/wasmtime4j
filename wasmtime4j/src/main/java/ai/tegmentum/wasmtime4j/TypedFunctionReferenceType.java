package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly typed function reference type.
 *
 * <p>This type carries precise function signature information as part of the typed function
 * references proposal. Unlike basic funcref, this type preserves the exact parameter and return
 * types, enabling compile-time type checking.
 *
 * @since 1.1.0
 */
public class TypedFunctionReferenceType {
  private final FunctionType functionType;

  /**
   * Creates a typed function reference type.
   *
   * @param functionType the function type this reference carries
   */
  public TypedFunctionReferenceType(final FunctionType functionType) {
    this.functionType = functionType;
  }

  /**
   * Gets the function type associated with this reference type.
   *
   * @return the function type
   */
  public FunctionType getFunctionType() {
    return functionType;
  }

  @Override
  public String toString() {
    return "(ref " + functionType + ")";
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TypedFunctionReferenceType)) {
      return false;
    }
    final TypedFunctionReferenceType other = (TypedFunctionReferenceType) obj;
    return functionType.equals(other.functionType);
  }

  @Override
  public int hashCode() {
    return functionType.hashCode();
  }
}
