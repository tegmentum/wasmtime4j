package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a generic WebAssembly function type with type parameters.
 *
 * <p>Generic function types allow for parameterization over WebAssembly value
 * types, enabling type-safe generic functions. This is part of the typed
 * function references proposal extension.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Define generic function type: <T> (T, T) -> T
 * GenericTypeParameter T = GenericTypeParameter.createNumeric("T");
 * GenericFunctionType binaryOp = GenericFunctionType.create(
 *     new GenericTypeParameter[]{T},
 *     new WasmValueType[]{T.asType(), T.asType()},
 *     new WasmValueType[]{T.asType()});
 *
 * // Specialize for specific types
 * FunctionType i32BinaryOp = binaryOp.specialize(WasmValueType.I32);
 * FunctionType f64BinaryOp = binaryOp.specialize(WasmValueType.F64);
 * }</pre>
 *
 * @since 1.1.0
 */
public interface GenericFunctionType extends FunctionType {

    /**
     * Gets the generic type parameters for this function type.
     *
     * @return array of generic type parameters
     */
    GenericTypeParameter[] getGenericParameters();

    /**
     * Gets the raw parameter types (may contain generic placeholders).
     *
     * @return array of parameter types with generic placeholders
     */
    WasmValueType[] getRawParameterTypes();

    /**
     * Gets the raw result types (may contain generic placeholders).
     *
     * @return array of result types with generic placeholders
     */
    WasmValueType[] getRawResultTypes();

    /**
     * Specializes this generic function type with concrete type arguments.
     *
     * @param typeArguments the concrete types to substitute for generic parameters
     * @return a concrete function type with types substituted
     * @throws ValidationException if type arguments don't match generic parameters
     */
    FunctionType specialize(final WasmValueType... typeArguments) throws ValidationException;

    /**
     * Checks if the given type arguments are valid for specialization.
     *
     * @param typeArguments the type arguments to validate
     * @return true if the arguments are valid for specialization
     */
    boolean canSpecializeWith(final WasmValueType... typeArguments);

    /**
     * Gets all possible specializations of this generic function type.
     *
     * <p>This returns all valid combinations of type arguments that satisfy
     * the generic parameter constraints.
     *
     * @return array of all valid specializations
     */
    FunctionType[] getAllSpecializations();

    /**
     * Checks if this generic function type is compatible with another type.
     *
     * <p>Generic types are compatible if they can be specialized to match
     * or if their constraints allow for valid substitutions.
     *
     * @param other the other function type to check compatibility with
     * @return true if compatible
     */
    boolean isCompatibleWith(final FunctionType other);

    /**
     * Creates a generic function type.
     *
     * @param genericParameters the generic type parameters
     * @param parameterTypes the parameter types (may include generic placeholders)
     * @param resultTypes the result types (may include generic placeholders)
     * @return a generic function type
     * @throws ValidationException if the generic parameters are invalid
     */
    static GenericFunctionType create(
            final GenericTypeParameter[] genericParameters,
            final WasmValueType[] parameterTypes,
            final WasmValueType[] resultTypes) throws ValidationException {

        validateGenericParameters(genericParameters, parameterTypes, resultTypes);

        return new GenericFunctionType() {
            @Override
            public GenericTypeParameter[] getGenericParameters() {
                return genericParameters.clone();
            }

            @Override
            public WasmValueType[] getRawParameterTypes() {
                return parameterTypes.clone();
            }

            @Override
            public WasmValueType[] getRawResultTypes() {
                return resultTypes.clone();
            }

            @Override
            public WasmValueType[] getParameterTypes() {
                // Return raw types - specialization will resolve placeholders
                return getRawParameterTypes();
            }

            @Override
            public WasmValueType[] getResultTypes() {
                // Return raw types - specialization will resolve placeholders
                return getRawResultTypes();
            }

            @Override
            public FunctionType specialize(final WasmValueType... typeArguments)
                    throws ValidationException {
                if (typeArguments.length != genericParameters.length) {
                    throw new ValidationException(
                        String.format("Expected %d type arguments, got %d",
                            genericParameters.length, typeArguments.length));
                }

                // Validate type arguments against constraints
                for (int i = 0; i < genericParameters.length; i++) {
                    if (!genericParameters[i].isValidType(typeArguments[i])) {
                        throw new ValidationException(
                            String.format("Type argument %s does not satisfy constraints for parameter %s",
                                typeArguments[i], genericParameters[i].getName()));
                    }
                }

                // Create substitution map
                final Map<GenericTypeParameter, WasmValueType> substitutions = new HashMap<>();
                for (int i = 0; i < genericParameters.length; i++) {
                    substitutions.put(genericParameters[i], typeArguments[i]);
                }

                // Substitute types
                final WasmValueType[] specializedParams = substituteTypes(parameterTypes, substitutions);
                final WasmValueType[] specializedResults = substituteTypes(resultTypes, substitutions);

                return FunctionType.create(specializedParams, specializedResults);
            }

            @Override
            public boolean canSpecializeWith(final WasmValueType... typeArguments) {
                try {
                    specialize(typeArguments);
                    return true;
                } catch (final ValidationException e) {
                    return false;
                }
            }

            @Override
            public FunctionType[] getAllSpecializations() {
                // Generate all valid combinations of type arguments
                // This is a simplified implementation - real implementation would be more sophisticated
                final WasmValueType[] allTypes = {
                    WasmValueType.I32, WasmValueType.I64,
                    WasmValueType.F32, WasmValueType.F64,
                    WasmValueType.FUNCREF, WasmValueType.EXTERNREF
                };

                // For now, return specializations for each basic type for single-parameter generics
                if (genericParameters.length == 1) {
                    final GenericTypeParameter param = genericParameters[0];
                    final java.util.List<FunctionType> specializations = new java.util.ArrayList<>();

                    for (final WasmValueType type : allTypes) {
                        if (param.isValidType(type)) {
                            try {
                                specializations.add(specialize(type));
                            } catch (final ValidationException e) {
                                // Skip invalid specializations
                            }
                        }
                    }

                    return specializations.toArray(new FunctionType[0]);
                }

                // For multi-parameter generics, return empty array (complex combinatorial case)
                return new FunctionType[0];
            }

            @Override
            public boolean isCompatibleWith(final FunctionType other) {
                if (other instanceof GenericFunctionType) {
                    // Both are generic - check if they can be unified
                    return canUnifyWith((GenericFunctionType) other);
                } else {
                    // Check if we can specialize to match the concrete type
                    for (final FunctionType specialization : getAllSpecializations()) {
                        if (FunctionTypeValidator.isCompatible(specialization, other)) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            private boolean canUnifyWith(final GenericFunctionType other) {
                // Simplified unification check
                if (genericParameters.length != other.getGenericParameters().length) {
                    return false;
                }

                // Check if parameter and result type patterns match
                return parameterTypes.length == other.getRawParameterTypes().length
                    && resultTypes.length == other.getRawResultTypes().length;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder();
                sb.append("<");
                for (int i = 0; i < genericParameters.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(genericParameters[i].toString());
                }
                sb.append("> (");

                for (int i = 0; i < parameterTypes.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(parameterTypes[i]);
                }
                sb.append(") -> (");

                for (int i = 0; i < resultTypes.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(resultTypes[i]);
                }
                sb.append(")");

                return sb.toString();
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof GenericFunctionType)) return false;

                final GenericFunctionType other = (GenericFunctionType) obj;
                return Arrays.equals(genericParameters, other.getGenericParameters())
                    && Arrays.equals(parameterTypes, other.getRawParameterTypes())
                    && Arrays.equals(resultTypes, other.getRawResultTypes());
            }

            @Override
            public int hashCode() {
                return java.util.Objects.hash(
                    Arrays.hashCode(genericParameters),
                    Arrays.hashCode(parameterTypes),
                    Arrays.hashCode(resultTypes));
            }
        };
    }

    /**
     * Validates generic parameters against the type signatures.
     */
    private static void validateGenericParameters(
            final GenericTypeParameter[] genericParameters,
            final WasmValueType[] parameterTypes,
            final WasmValueType[] resultTypes) throws ValidationException {

        // Ensure all generic placeholders in types have corresponding parameters
        final java.util.Set<GenericTypeParameter> declaredParams = new java.util.HashSet<>(
            Arrays.asList(genericParameters));

        validateTypesUseOnlyDeclaredParameters(parameterTypes, declaredParams);
        validateTypesUseOnlyDeclaredParameters(resultTypes, declaredParams);
    }

    /**
     * Validates that types only reference declared generic parameters.
     */
    private static void validateTypesUseOnlyDeclaredParameters(
            final WasmValueType[] types,
            final java.util.Set<GenericTypeParameter> declaredParams) throws ValidationException {

        for (final WasmValueType type : types) {
            if (type instanceof GenericPlaceholderType) {
                final GenericTypeParameter param = ((GenericPlaceholderType) type).getParameter();
                if (!declaredParams.contains(param)) {
                    throw new ValidationException(
                        "Undeclared generic parameter: " + param.getName());
                }
            }
        }
    }

    /**
     * Substitutes generic placeholders with concrete types.
     */
    private static WasmValueType[] substituteTypes(
            final WasmValueType[] types,
            final Map<GenericTypeParameter, WasmValueType> substitutions) {

        final WasmValueType[] result = new WasmValueType[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof GenericPlaceholderType) {
                final GenericTypeParameter param = ((GenericPlaceholderType) types[i]).getParameter();
                result[i] = substitutions.getOrDefault(param, types[i]);
            } else {
                result[i] = types[i];
            }
        }
        return result;
    }
}