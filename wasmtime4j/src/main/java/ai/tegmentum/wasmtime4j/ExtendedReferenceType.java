package ai.tegmentum.wasmtime4j;

/**
 * Represents extended reference types from the reference types proposal extensions.
 *
 * <p>This interface defines extended reference types beyond the basic funcref and
 * externref, including:
 *
 * <ul>
 *   <li>Nullable and non-nullable reference distinctions
 *   <li>Reference type inheritance and subtyping
 *   <li>Structured reference types
 *   <li>Array and struct reference types
 * </ul>
 *
 * @since 1.1.0
 */
public interface ExtendedReferenceType extends WasmValueType {

    /**
     * Checks if this reference type is nullable.
     *
     * @return true if the reference can be null, false if non-nullable
     */
    boolean isNullable();

    /**
     * Gets the base type of this reference (without nullability).
     *
     * @return the base reference type
     */
    WasmValueType getBaseType();

    /**
     * Gets the nullability-adjusted type.
     *
     * @param nullable whether the result should be nullable
     * @return a reference type with the specified nullability
     */
    ExtendedReferenceType withNullability(final boolean nullable);

    /**
     * Checks if this reference type is a subtype of another reference type.
     *
     * @param other the potential supertype
     * @return true if this is a subtype of other
     */
    boolean isSubtypeOf(final ExtendedReferenceType other);

    /**
     * Gets the heap type category for this reference.
     *
     * @return the heap type category
     */
    HeapType getHeapType();

    /**
     * Heap type categories for reference types.
     */
    enum HeapType {
        /** Function heap type (for function references). */
        FUNC,
        /** External heap type (for host object references). */
        EXTERN,
        /** Any heap type (top of reference type hierarchy). */
        ANY,
        /** No heap type (bottom of reference type hierarchy). */
        NONE,
        /** Struct heap type (for structured data). */
        STRUCT,
        /** Array heap type (for array data). */
        ARRAY,
        /** Custom heap type (user-defined). */
        CUSTOM
    }

    /**
     * Creates a nullable reference type.
     *
     * @param baseType the base type
     * @return a nullable reference type
     */
    static ExtendedReferenceType nullable(final WasmValueType baseType) {
        return create(baseType, true, inferHeapType(baseType));
    }

    /**
     * Creates a non-nullable reference type.
     *
     * @param baseType the base type
     * @return a non-nullable reference type
     */
    static ExtendedReferenceType nonNullable(final WasmValueType baseType) {
        return create(baseType, false, inferHeapType(baseType));
    }

    /**
     * Creates an extended reference type.
     *
     * @param baseType the base type
     * @param nullable whether the type is nullable
     * @param heapType the heap type category
     * @return an extended reference type
     */
    static ExtendedReferenceType create(
            final WasmValueType baseType,
            final boolean nullable,
            final HeapType heapType) {

        return new ExtendedReferenceType() {
            @Override
            public boolean isNullable() {
                return nullable;
            }

            @Override
            public WasmValueType getBaseType() {
                return baseType;
            }

            @Override
            public ExtendedReferenceType withNullability(final boolean newNullable) {
                if (newNullable == nullable) {
                    return this;
                }
                return create(baseType, newNullable, heapType);
            }

            @Override
            public boolean isSubtypeOf(final ExtendedReferenceType other) {
                // Nullability subtyping: non-nullable is subtype of nullable
                if (nullable && !other.isNullable()) {
                    return false;
                }

                // Base type subtyping
                return FunctionTypeValidator.isSubtypeOf(baseType, other.getBaseType());
            }

            @Override
            public HeapType getHeapType() {
                return heapType;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder();
                if (!nullable) {
                    sb.append("(ref ");
                } else {
                    sb.append("(ref null ");
                }

                switch (heapType) {
                    case FUNC:
                        sb.append("func");
                        break;
                    case EXTERN:
                        sb.append("extern");
                        break;
                    case ANY:
                        sb.append("any");
                        break;
                    case NONE:
                        sb.append("none");
                        break;
                    case STRUCT:
                        sb.append("struct");
                        break;
                    case ARRAY:
                        sb.append("array");
                        break;
                    case CUSTOM:
                        sb.append(baseType.toString());
                        break;
                }

                sb.append(")");
                return sb.toString();
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof ExtendedReferenceType)) return false;

                final ExtendedReferenceType other = (ExtendedReferenceType) obj;
                return nullable == other.isNullable()
                    && baseType.equals(other.getBaseType())
                    && heapType == other.getHeapType();
            }

            @Override
            public int hashCode() {
                return java.util.Objects.hash(baseType, nullable, heapType);
            }
        };
    }

    /**
     * Infers the heap type from a base type.
     */
    private static HeapType inferHeapType(final WasmValueType baseType) {
        if (baseType == WasmValueType.FUNCREF || baseType instanceof TypedFunctionReferenceType) {
            return HeapType.FUNC;
        } else if (baseType == WasmValueType.EXTERNREF) {
            return HeapType.EXTERN;
        } else {
            return HeapType.CUSTOM;
        }
    }
}