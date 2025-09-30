package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;

/**
 * Represents a typed WebAssembly function reference with strong type checking.
 *
 * <p>The typed function references proposal extends the basic funcref type with
 * strongly-typed function references that carry their precise signature. This
 * enables:
 *
 * <ul>
 *   <li>Compile-time type checking for function references
 *   <li>Function subtyping and compatibility checking
 *   <li>Type-parameterized function references
 *   <li>Safe function reference casting
 * </ul>
 *
 * <p>Unlike basic funcref, typed function references preserve the complete
 * signature information and enable the runtime to validate function calls
 * at both compile-time and runtime.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a typed function reference with explicit type
 * FunctionType mathOpType = FunctionType.create(
 *     new WasmValueType[]{WasmValueType.I32, WasmValueType.I32},
 *     new WasmValueType[]{WasmValueType.I32});
 *
 * TypedFunctionReference addRef = TypedFunctionReference.create(
 *     addFunction, mathOpType);
 *
 * // Type-safe invocation
 * WasmValue result = addRef.callTyped(WasmValue.i32(10), WasmValue.i32(20));
 *
 * // Subtype checking
 * if (addRef.isSubtypeOf(genericMathType)) {
 *     // Safe to use as genericMathType
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public interface TypedFunctionReference extends FunctionReference {

    /**
     * Gets the exact function type of this typed reference.
     *
     * <p>Unlike the base getFunctionType(), this method returns the precise
     * type information including any type parameters or constraints.
     *
     * @return the precise function type
     */
    @Override
    FunctionType getFunctionType();

    /**
     * Calls this typed function reference with type validation.
     *
     * <p>This method performs strict type checking on parameters and return
     * values, throwing a ValidationException if types don't match exactly.
     *
     * @param params the parameters to pass (must match parameter types exactly)
     * @return the results (guaranteed to match return types)
     * @throws ValidationException if parameter types don't match
     * @throws WasmException if function execution fails
     */
    WasmValue[] callTyped(final WasmValue... params) throws ValidationException, WasmException;

    /**
     * Checks if this function reference is a subtype of the given type.
     *
     * <p>Function subtyping follows contravariant parameter types and
     * covariant return types:
     * <ul>
     *   <li>Parameters: subtype accepts more general parameter types
     *   <li>Returns: subtype provides more specific return types
     * </ul>
     *
     * @param superType the potential supertype to check against
     * @return true if this reference is a subtype of superType
     */
    boolean isSubtypeOf(final FunctionType superType);

    /**
     * Checks if this function reference is compatible with the given type.
     *
     * <p>Compatibility is less strict than subtyping and allows for safe
     * function calls even if types aren't in a subtype relationship.
     *
     * @param targetType the type to check compatibility with
     * @return true if this reference can be safely called as targetType
     */
    boolean isCompatibleWith(final FunctionType targetType);

    /**
     * Attempts to cast this function reference to the given type.
     *
     * <p>This performs a safe downcast if possible, returning a new typed
     * reference with the more specific type information.
     *
     * @param targetType the target type to cast to
     * @return a typed reference with the target type
     * @throws ValidationException if the cast is not valid
     */
    TypedFunctionReference castTo(final FunctionType targetType) throws ValidationException;

    /**
     * Gets the type variance information for this function reference.
     *
     * <p>Variance describes how the function type behaves with respect to
     * subtyping:
     * <ul>
     *   <li>INVARIANT: exact type match required
     *   <li>COVARIANT: allows more specific types
     *   <li>CONTRAVARIANT: allows more general types
     * </ul>
     *
     * @return the type variance
     */
    TypeVariance getVariance();

    /**
     * Checks if this function reference supports generic type parameters.
     *
     * @return true if this function supports generics
     */
    boolean isGeneric();

    /**
     * Gets the generic type parameters if this function is generic.
     *
     * @return array of generic type parameters, or empty array if not generic
     */
    GenericTypeParameter[] getGenericParameters();

    /**
     * Creates a specialized instance of this generic function with concrete types.
     *
     * @param typeArguments the concrete types for generic parameters
     * @return a specialized typed function reference
     * @throws ValidationException if type arguments don't match generic parameters
     * @throws UnsupportedOperationException if this function is not generic
     */
    TypedFunctionReference specialize(final WasmValueType... typeArguments)
        throws ValidationException;

    /**
     * Validates parameter types against this function's signature.
     *
     * @param params the parameters to validate
     * @throws ValidationException if parameters don't match the signature
     */
    void validateParameters(final WasmValue... params) throws ValidationException;

    /**
     * Validates return types against this function's signature.
     *
     * @param results the return values to validate
     * @throws ValidationException if return values don't match the signature
     */
    void validateResults(final WasmValue... results) throws ValidationException;

    /**
     * Gets additional type metadata associated with this function.
     *
     * @return the type metadata, or null if none available
     */
    FunctionTypeMetadata getTypeMetadata();

    /**
     * Type variance enumeration for function references.
     */
    enum TypeVariance {
        /** Exact type match required. */
        INVARIANT,
        /** More specific types allowed (return type variance). */
        COVARIANT,
        /** More general types allowed (parameter type variance). */
        CONTRAVARIANT
    }

    /**
     * Creates a typed function reference from a regular function reference.
     *
     * @param functionRef the base function reference
     * @param exactType the exact type information
     * @return a typed function reference
     * @throws ValidationException if the function doesn't match the type
     */
    static TypedFunctionReference create(
            final FunctionReference functionRef,
            final FunctionType exactType) throws ValidationException {

        return new TypedFunctionReference() {
            @Override
            public FunctionType getFunctionType() {
                return exactType;
            }

            @Override
            public WasmValue[] callTyped(final WasmValue... params)
                    throws ValidationException, WasmException {
                validateParameters(params);
                final WasmValue[] results = functionRef.call(params);
                validateResults(results);
                return results;
            }

            @Override
            public WasmValue[] call(final WasmValue... params) throws WasmException {
                return functionRef.call(params);
            }

            @Override
            public String getName() {
                return functionRef.getName();
            }

            @Override
            public boolean isValid() {
                return functionRef.isValid();
            }

            @Override
            public long getId() {
                return functionRef.getId();
            }

            @Override
            public boolean isSubtypeOf(final FunctionType superType) {
                return FunctionTypeValidator.isSubtype(exactType, superType);
            }

            @Override
            public boolean isCompatibleWith(final FunctionType targetType) {
                return FunctionTypeValidator.isCompatible(exactType, targetType);
            }

            @Override
            public TypedFunctionReference castTo(final FunctionType targetType)
                    throws ValidationException {
                if (!isCompatibleWith(targetType)) {
                    throw new ValidationException(
                        String.format("Cannot cast function type %s to %s",
                            exactType, targetType));
                }
                return create(functionRef, targetType);
            }

            @Override
            public TypeVariance getVariance() {
                return TypeVariance.CONTRAVARIANT; // Default for function types
            }

            @Override
            public boolean isGeneric() {
                return exactType instanceof GenericFunctionType;
            }

            @Override
            public GenericTypeParameter[] getGenericParameters() {
                if (isGeneric()) {
                    return ((GenericFunctionType) exactType).getGenericParameters();
                }
                return new GenericTypeParameter[0];
            }

            @Override
            public TypedFunctionReference specialize(final WasmValueType... typeArguments)
                    throws ValidationException {
                if (!isGeneric()) {
                    throw new UnsupportedOperationException(
                        "Cannot specialize non-generic function");
                }
                final GenericFunctionType genericType = (GenericFunctionType) exactType;
                final FunctionType specializedType = genericType.specialize(typeArguments);
                return create(functionRef, specializedType);
            }

            @Override
            public void validateParameters(final WasmValue... params) throws ValidationException {
                final WasmValueType[] paramTypes = exactType.getParameterTypes();
                if (params.length != paramTypes.length) {
                    throw new ValidationException(
                        String.format("Expected %d parameters, got %d",
                            paramTypes.length, params.length));
                }

                for (int i = 0; i < params.length; i++) {
                    if (!params[i].getType().equals(paramTypes[i])) {
                        throw new ValidationException(
                            String.format("Parameter %d type mismatch: expected %s, got %s",
                                i, paramTypes[i], params[i].getType()));
                    }
                }
            }

            @Override
            public void validateResults(final WasmValue... results) throws ValidationException {
                final WasmValueType[] resultTypes = exactType.getResultTypes();
                if (results.length != resultTypes.length) {
                    throw new ValidationException(
                        String.format("Expected %d results, got %d",
                            resultTypes.length, results.length));
                }

                for (int i = 0; i < results.length; i++) {
                    if (!results[i].getType().equals(resultTypes[i])) {
                        throw new ValidationException(
                            String.format("Result %d type mismatch: expected %s, got %s",
                                i, resultTypes[i], results[i].getType()));
                    }
                }
            }

            @Override
            public FunctionTypeMetadata getTypeMetadata() {
                return exactType instanceof ExtendedFunctionType
                    ? ((ExtendedFunctionType) exactType).getMetadata()
                    : null;
            }
        };
    }

    /**
     * Creates a typed function reference with variance information.
     *
     * @param functionRef the base function reference
     * @param exactType the exact type information
     * @param variance the type variance
     * @return a typed function reference with variance
     * @throws ValidationException if the function doesn't match the type
     */
    static TypedFunctionReference createWithVariance(
            final FunctionReference functionRef,
            final FunctionType exactType,
            final TypeVariance variance) throws ValidationException {

        final TypedFunctionReference base = create(functionRef, exactType);

        return new TypedFunctionReference() {
            @Override
            public FunctionType getFunctionType() {
                return base.getFunctionType();
            }

            @Override
            public WasmValue[] callTyped(final WasmValue... params)
                    throws ValidationException, WasmException {
                return base.callTyped(params);
            }

            @Override
            public WasmValue[] call(final WasmValue... params) throws WasmException {
                return base.call(params);
            }

            @Override
            public String getName() {
                return base.getName();
            }

            @Override
            public boolean isValid() {
                return base.isValid();
            }

            @Override
            public long getId() {
                return base.getId();
            }

            @Override
            public boolean isSubtypeOf(final FunctionType superType) {
                return base.isSubtypeOf(superType);
            }

            @Override
            public boolean isCompatibleWith(final FunctionType targetType) {
                return base.isCompatibleWith(targetType);
            }

            @Override
            public TypedFunctionReference castTo(final FunctionType targetType)
                    throws ValidationException {
                return base.castTo(targetType);
            }

            @Override
            public TypeVariance getVariance() {
                return variance;
            }

            @Override
            public boolean isGeneric() {
                return base.isGeneric();
            }

            @Override
            public GenericTypeParameter[] getGenericParameters() {
                return base.getGenericParameters();
            }

            @Override
            public TypedFunctionReference specialize(final WasmValueType... typeArguments)
                    throws ValidationException {
                return base.specialize(typeArguments);
            }

            @Override
            public void validateParameters(final WasmValue... params) throws ValidationException {
                base.validateParameters(params);
            }

            @Override
            public void validateResults(final WasmValue... results) throws ValidationException {
                base.validateResults(results);
            }

            @Override
            public FunctionTypeMetadata getTypeMetadata() {
                return base.getTypeMetadata();
            }
        };
    }
}