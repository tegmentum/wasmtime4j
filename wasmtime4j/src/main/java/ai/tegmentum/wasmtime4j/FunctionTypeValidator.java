package ai.tegmentum.wasmtime4j;

/**
 * Utility class for validating and comparing function types with subtyping rules.
 *
 * <p>This class implements the WebAssembly function subtyping rules as specified in the typed
 * function references proposal:
 *
 * <ul>
 *   <li>Function types are contravariant in parameter types
 *   <li>Function types are covariant in result types
 *   <li>A function type A is a subtype of B if:
 *       <ul>
 *         <li>A accepts more general parameter types than B (contravariant)
 *         <li>A returns more specific result types than B (covariant)
 *       </ul>
 * </ul>
 *
 * @since 1.1.0
 */
public final class FunctionTypeValidator {

  private FunctionTypeValidator() {
    // Utility class - no instantiation
  }

  /**
   * Checks if function type A is a subtype of function type B.
   *
   * <p>Function subtyping follows these rules:
   *
   * <ul>
   *   <li>Parameter types: contravariant (A can accept more general parameters)
   *   <li>Result types: covariant (A can return more specific results)
   * </ul>
   *
   * @param subtype the potential subtype
   * @param supertype the potential supertype
   * @return true if subtype is a subtype of supertype
   */
  public static boolean isSubtype(final FunctionType subtype, final FunctionType supertype) {
    if (subtype == null || supertype == null) {
      return false;
    }

    if (subtype.equals(supertype)) {
      return true;
    }

    // Check parameter count
    final WasmValueType[] subParams = subtype.getParamTypes();
    final WasmValueType[] superParams = supertype.getParamTypes();
    if (subParams.length != superParams.length) {
      return false;
    }

    // Check result count
    final WasmValueType[] subResults = subtype.getReturnTypes();
    final WasmValueType[] superResults = supertype.getReturnTypes();
    if (subResults.length != superResults.length) {
      return false;
    }

    // Check parameter types (contravariant)
    for (int i = 0; i < subParams.length; i++) {
      if (!isSubtypeOf(superParams[i], subParams[i])) {
        return false;
      }
    }

    // Check result types (covariant)
    for (int i = 0; i < subResults.length; i++) {
      if (!isSubtypeOf(subResults[i], superResults[i])) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if two function types are compatible for calling.
   *
   * <p>Compatibility is more lenient than subtyping and allows for safe function calls even if
   * types aren't in a perfect subtype relationship.
   *
   * @param callerType the type expected by the caller
   * @param calleeType the actual type of the function being called
   * @return true if the callee can be safely called as the caller type
   */
  public static boolean isCompatible(final FunctionType callerType, final FunctionType calleeType) {
    if (callerType == null || calleeType == null) {
      return false;
    }

    // Exact match is always compatible
    if (callerType.equals(calleeType)) {
      return true;
    }

    // Check if there's a subtype relationship
    if (isSubtype(calleeType, callerType)) {
      return true;
    }

    // Additional compatibility checks for WebAssembly-specific rules
    return isWeaklyCompatible(callerType, calleeType);
  }

  /** Checks weak compatibility for WebAssembly-specific type coercion rules. */
  private static boolean isWeaklyCompatible(
      final FunctionType expected, final FunctionType actual) {
    // Check parameter and result counts
    if (expected.getParamTypes().length != actual.getParamTypes().length
        || expected.getReturnTypes().length != actual.getReturnTypes().length) {
      return false;
    }

    // Check if types can be safely coerced
    final WasmValueType[] expectedParams = expected.getParamTypes();
    final WasmValueType[] actualParams = actual.getParamTypes();
    for (int i = 0; i < expectedParams.length; i++) {
      if (!canCoerce(expectedParams[i], actualParams[i])) {
        return false;
      }
    }

    final WasmValueType[] expectedResults = expected.getReturnTypes();
    final WasmValueType[] actualResults = actual.getReturnTypes();
    for (int i = 0; i < expectedResults.length; i++) {
      if (!canCoerce(actualResults[i], expectedResults[i])) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if one value type is a subtype of another.
   *
   * <p>This implements WebAssembly value type subtyping rules:
   *
   * <ul>
   *   <li>Reference types have subtyping relationships
   *   <li>Numeric types are exactly equal (no subtyping)
   *   <li>Function references can have subtyping
   * </ul>
   *
   * @param subtype the potential subtype
   * @param supertype the potential supertype
   * @return true if subtype is a subtype of supertype
   */
  public static boolean isSubtypeOf(final WasmValueType subtype, final WasmValueType supertype) {
    if (subtype == null || supertype == null) {
      return false;
    }

    // Exact equality
    if (subtype.equals(supertype)) {
      return true;
    }

    // Check numeric type compatibility
    if (isNumericType(subtype) && isNumericType(supertype)) {
      return false; // Numeric types must be exactly equal
    }

    // Check reference type subtyping
    if (isReferenceType(subtype) && isReferenceType(supertype)) {
      return checkReferenceSubtyping(subtype, supertype);
    }

    return false;
  }

  /** Checks if a type can be coerced to another type safely. */
  private static boolean canCoerce(final WasmValueType from, final WasmValueType to) {
    if (from.equals(to)) {
      return true;
    }

    // WebAssembly allows some implicit coercions in specific contexts
    // For function types, we're generally strict about type matching
    return isSubtypeOf(from, to);
  }

  /** Checks reference type subtyping relationships. */
  private static boolean checkReferenceSubtyping(
      final WasmValueType subtype, final WasmValueType supertype) {
    // Basic reference type hierarchy:
    // - All reference types are subtypes of externref
    // - Function references are subtypes of funcref
    // - Specific function types are subtypes of more general function types

    if (supertype == WasmValueType.EXTERNREF) {
      // Everything is a subtype of externref
      return isReferenceType(subtype);
    }

    if (supertype == WasmValueType.FUNCREF) {
      // Function references are subtypes of funcref
      return isFunctionReferenceType(subtype);
    }

    // For typed function references, we would check function type subtyping
    // but TypedFunctionReferenceType is a separate class not a WasmValueType
    // So this case is handled through ExtendedReferenceType

    return false;
  }

  /** Checks if a type is a numeric type. */
  private static boolean isNumericType(final WasmValueType type) {
    return type == WasmValueType.I32
        || type == WasmValueType.I64
        || type == WasmValueType.F32
        || type == WasmValueType.F64;
  }

  /** Checks if a type is a reference type. */
  private static boolean isReferenceType(final WasmValueType type) {
    return type == WasmValueType.FUNCREF || type == WasmValueType.EXTERNREF;
  }

  /** Checks if a type is a function reference type. */
  private static boolean isFunctionReferenceType(final WasmValueType type) {
    return type == WasmValueType.FUNCREF;
  }

  /**
   * Validates function type compatibility for a function call.
   *
   * @param expectedType the function type expected at the call site
   * @param actualType the actual function type being called
   * @param parameters the actual parameters being passed
   * @throws ai.tegmentum.wasmtime4j.exception.ValidationException if types are incompatible
   */
  public static void validateCall(
      final FunctionType expectedType, final FunctionType actualType, final WasmValue[] parameters)
      throws ai.tegmentum.wasmtime4j.exception.ValidationException {

    if (!isCompatible(expectedType, actualType)) {
      throw new ai.tegmentum.wasmtime4j.exception.ValidationException(
          String.format("Function type mismatch: expected %s, got %s", expectedType, actualType));
    }

    // Validate parameter count
    if (parameters.length != expectedType.getParamTypes().length) {
      throw new ai.tegmentum.wasmtime4j.exception.ValidationException(
          String.format(
              "Parameter count mismatch: expected %d, got %d",
              expectedType.getParamTypes().length, parameters.length));
    }

    // Validate parameter types
    final WasmValueType[] expectedParams = expectedType.getParamTypes();
    for (int i = 0; i < parameters.length; i++) {
      if (!parameters[i].getType().equals(expectedParams[i])) {
        throw new ai.tegmentum.wasmtime4j.exception.ValidationException(
            String.format(
                "Parameter %d type mismatch: expected %s, got %s",
                i, expectedParams[i], parameters[i].getType()));
      }
    }
  }

  /**
   * Validates function type compatibility for function reference assignment.
   *
   * @param targetType the target function type for assignment
   * @param sourceType the source function type being assigned
   * @throws ai.tegmentum.wasmtime4j.exception.ValidationException if types are incompatible
   */
  public static void validateAssignment(
      final FunctionType targetType, final FunctionType sourceType)
      throws ai.tegmentum.wasmtime4j.exception.ValidationException {

    if (!isSubtype(sourceType, targetType)) {
      throw new ai.tegmentum.wasmtime4j.exception.ValidationException(
          String.format(
              "Cannot assign function type %s to %s: not a subtype", sourceType, targetType));
    }
  }

  /**
   * Gets the most specific common supertype of two function types.
   *
   * @param type1 the first function type
   * @param type2 the second function type
   * @return the most specific common supertype, or null if none exists
   */
  public static FunctionType getCommonSupertype(
      final FunctionType type1, final FunctionType type2) {
    if (type1 == null || type2 == null) {
      return null;
    }

    if (type1.equals(type2)) {
      return type1;
    }

    if (isSubtype(type1, type2)) {
      return type2;
    }

    if (isSubtype(type2, type1)) {
      return type1;
    }

    // If neither is a subtype of the other, look for a common supertype
    // In WebAssembly, this would typically be a more general function type
    // For simplicity, we return null (no common supertype found)
    return null;
  }
}
