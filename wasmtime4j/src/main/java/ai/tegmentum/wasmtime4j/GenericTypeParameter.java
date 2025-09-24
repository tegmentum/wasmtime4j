package ai.tegmentum.wasmtime4j;

/**
 * Represents a generic type parameter in typed function references.
 *
 * <p>Generic type parameters allow function types to be parameterized over
 * specific value types, enabling type-safe generic functions in WebAssembly.
 * This is part of the typed function references proposal extension.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Define a generic function type: (T, T) -> T
 * GenericTypeParameter T = GenericTypeParameter.create("T",
 *     WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64);
 *
 * GenericFunctionType genericAdd = GenericFunctionType.create(
 *     new GenericTypeParameter[]{T},
 *     new WasmValueType[]{T.asType(), T.asType()},
 *     new WasmValueType[]{T.asType()});
 *
 * // Specialize for i32: (i32, i32) -> i32
 * FunctionType i32Add = genericAdd.specialize(WasmValueType.I32);
 * }</pre>
 *
 * @since 1.1.0
 */
public interface GenericTypeParameter {

    /**
     * Gets the name of this generic type parameter.
     *
     * @return the parameter name (e.g., "T", "U", "V")
     */
    String getName();

    /**
     * Gets the constraint types that this parameter can be bound to.
     *
     * <p>If no constraints are specified, the parameter can be bound to any
     * WebAssembly value type.
     *
     * @return array of allowed types, or empty array if unconstrained
     */
    WasmValueType[] getConstraints();

    /**
     * Checks if the given type satisfies this parameter's constraints.
     *
     * @param type the type to check
     * @return true if the type is valid for this parameter
     */
    boolean isValidType(final WasmValueType type);

    /**
     * Gets the variance of this type parameter.
     *
     * @return the variance (covariant, contravariant, or invariant)
     */
    TypeVariance getVariance();

    /**
     * Returns this parameter as a type placeholder for use in function signatures.
     *
     * @return a placeholder type representing this parameter
     */
    WasmValueType asType();

    /**
     * Type variance for generic parameters.
     */
    enum TypeVariance {
        /** Parameter type must match exactly. */
        INVARIANT,
        /** Parameter accepts more specific types (out T). */
        COVARIANT,
        /** Parameter accepts more general types (in T). */
        CONTRAVARIANT
    }

    /**
     * Creates a generic type parameter with the given name.
     *
     * @param name the parameter name
     * @return an unconstrained generic type parameter
     */
    static GenericTypeParameter create(final String name) {
        return create(name, TypeVariance.INVARIANT);
    }

    /**
     * Creates a generic type parameter with constraints.
     *
     * @param name the parameter name
     * @param constraints the allowed types for this parameter
     * @return a constrained generic type parameter
     */
    static GenericTypeParameter create(final String name, final WasmValueType... constraints) {
        return create(name, TypeVariance.INVARIANT, constraints);
    }

    /**
     * Creates a generic type parameter with variance.
     *
     * @param name the parameter name
     * @param variance the type variance
     * @return a generic type parameter with variance
     */
    static GenericTypeParameter create(final String name, final TypeVariance variance) {
        return create(name, variance, new WasmValueType[0]);
    }

    /**
     * Creates a generic type parameter with constraints and variance.
     *
     * @param name the parameter name
     * @param variance the type variance
     * @param constraints the allowed types for this parameter
     * @return a fully specified generic type parameter
     */
    static GenericTypeParameter create(
            final String name,
            final TypeVariance variance,
            final WasmValueType... constraints) {

        return new GenericTypeParameter() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public WasmValueType[] getConstraints() {
                return constraints.clone();
            }

            @Override
            public boolean isValidType(final WasmValueType type) {
                if (constraints.length == 0) {
                    return true; // Unconstrained
                }

                for (final WasmValueType constraint : constraints) {
                    if (constraint.equals(type)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public TypeVariance getVariance() {
                return variance;
            }

            @Override
            public WasmValueType asType() {
                return new GenericPlaceholderType(this);
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder();
                switch (variance) {
                    case COVARIANT:
                        sb.append("out ");
                        break;
                    case CONTRAVARIANT:
                        sb.append("in ");
                        break;
                    case INVARIANT:
                    default:
                        // No prefix for invariant
                        break;
                }
                sb.append(name);

                if (constraints.length > 0) {
                    sb.append(" : ");
                    for (int i = 0; i < constraints.length; i++) {
                        if (i > 0) sb.append(" | ");
                        sb.append(constraints[i]);
                    }
                }

                return sb.toString();
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof GenericTypeParameter)) return false;

                final GenericTypeParameter other = (GenericTypeParameter) obj;
                return name.equals(other.getName())
                    && variance == other.getVariance()
                    && java.util.Arrays.equals(constraints, other.getConstraints());
            }

            @Override
            public int hashCode() {
                return java.util.Objects.hash(name, variance, java.util.Arrays.hashCode(constraints));
            }
        };
    }

    /**
     * Creates a numeric type parameter (constrained to numeric types).
     *
     * @param name the parameter name
     * @return a numeric type parameter
     */
    static GenericTypeParameter createNumeric(final String name) {
        return create(name, WasmValueType.I32, WasmValueType.I64,
                      WasmValueType.F32, WasmValueType.F64);
    }

    /**
     * Creates an integer type parameter (constrained to integer types).
     *
     * @param name the parameter name
     * @return an integer type parameter
     */
    static GenericTypeParameter createInteger(final String name) {
        return create(name, WasmValueType.I32, WasmValueType.I64);
    }

    /**
     * Creates a floating-point type parameter (constrained to float types).
     *
     * @param name the parameter name
     * @return a floating-point type parameter
     */
    static GenericTypeParameter createFloat(final String name) {
        return create(name, WasmValueType.F32, WasmValueType.F64);
    }

    /**
     * Creates a reference type parameter (constrained to reference types).
     *
     * @param name the parameter name
     * @return a reference type parameter
     */
    static GenericTypeParameter createReference(final String name) {
        return create(name, WasmValueType.FUNCREF, WasmValueType.EXTERNREF);
    }
}

/**
 * Internal placeholder type for generic type parameters.
 */
class GenericPlaceholderType implements WasmValueType {
    private final GenericTypeParameter parameter;

    GenericPlaceholderType(final GenericTypeParameter parameter) {
        this.parameter = parameter;
    }

    public GenericTypeParameter getParameter() {
        return parameter;
    }

    @Override
    public String toString() {
        return parameter.getName();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GenericPlaceholderType)) return false;
        final GenericPlaceholderType other = (GenericPlaceholderType) obj;
        return parameter.equals(other.parameter);
    }

    @Override
    public int hashCode() {
        return parameter.hashCode();
    }
}