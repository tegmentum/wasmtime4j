package ai.tegmentum.wasmtime4j;

/**
 * Represents an extended WebAssembly function type with additional metadata.
 *
 * <p>Extended function types carry additional information beyond basic parameter
 * and return types, including:
 *
 * <ul>
 *   <li>Parameter and result names
 *   <li>Documentation and annotations
 *   <li>Performance characteristics
 *   <li>Calling convention information
 * </ul>
 *
 * <p>This interface extends the basic FunctionType to provide enhanced
 * type information for better tooling support and runtime optimization.
 *
 * @since 1.1.0
 */
public interface ExtendedFunctionType extends FunctionType {

    /**
     * Gets the metadata associated with this function type.
     *
     * @return the function type metadata
     */
    FunctionTypeMetadata getMetadata();

    /**
     * Checks if this function type has specific metadata.
     *
     * @param metadataType the type of metadata to check for
     * @return true if the metadata is present
     */
    default boolean hasMetadata(final FunctionTypeMetadata.MetadataType<?> metadataType) {
        return getMetadata().hasMetadata(metadataType);
    }

    /**
     * Gets metadata of a specific type.
     *
     * @param metadataType the type of metadata to retrieve
     * @param <T> the metadata value type
     * @return the metadata value, or empty if not present
     */
    default <T> java.util.Optional<T> getMetadata(final FunctionTypeMetadata.MetadataType<T> metadataType) {
        return getMetadata().getMetadata(metadataType);
    }

    /**
     * Gets the calling convention for this function type.
     *
     * @return the calling convention
     */
    default FunctionTypeMetadata.CallingConvention getCallingConvention() {
        return getMetadata().getCallingConvention();
    }

    /**
     * Gets performance hints for this function type.
     *
     * @return the performance hints
     */
    default FunctionTypeMetadata.PerformanceHints getPerformanceHints() {
        return getMetadata().getPerformanceHints();
    }

    /**
     * Gets the names of function parameters.
     *
     * @return array of parameter names, or empty array if not available
     */
    default String[] getParameterNames() {
        return getMetadata().getParameterNames();
    }

    /**
     * Gets the names of function results.
     *
     * @return array of result names, or empty array if not available
     */
    default String[] getResultNames() {
        return getMetadata().getResultNames();
    }

    /**
     * Creates an extended function type from a basic function type and metadata.
     *
     * @param baseFunctionType the base function type
     * @param metadata the additional metadata
     * @return an extended function type
     */
    static ExtendedFunctionType create(
            final FunctionType baseFunctionType,
            final FunctionTypeMetadata metadata) {

        return new ExtendedFunctionType() {
            @Override
            public WasmValueType[] getParameterTypes() {
                return baseFunctionType.getParameterTypes();
            }

            @Override
            public WasmValueType[] getResultTypes() {
                return baseFunctionType.getResultTypes();
            }

            @Override
            public FunctionTypeMetadata getMetadata() {
                return metadata;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder();
                sb.append("(");

                // Add parameter types with names if available
                final WasmValueType[] paramTypes = getParameterTypes();
                final String[] paramNames = getParameterNames();
                for (int i = 0; i < paramTypes.length; i++) {
                    if (i > 0) sb.append(", ");
                    if (paramNames.length > i && paramNames[i] != null) {
                        sb.append(paramNames[i]).append(": ");
                    }
                    sb.append(paramTypes[i]);
                }

                sb.append(") -> (");

                // Add result types with names if available
                final WasmValueType[] resultTypes = getResultTypes();
                final String[] resultNames = getResultNames();
                for (int i = 0; i < resultTypes.length; i++) {
                    if (i > 0) sb.append(", ");
                    if (resultNames.length > i && resultNames[i] != null) {
                        sb.append(resultNames[i]).append(": ");
                    }
                    sb.append(resultTypes[i]);
                }

                sb.append(")");

                // Add calling convention if not default
                if (getCallingConvention() != FunctionTypeMetadata.CallingConvention.WASM_STANDARD) {
                    sb.append(" [").append(getCallingConvention()).append("]");
                }

                return sb.toString();
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof ExtendedFunctionType)) {
                    // Compare with base function type
                    return obj instanceof FunctionType
                        && baseFunctionType.equals(obj);
                }

                final ExtendedFunctionType other = (ExtendedFunctionType) obj;
                return baseFunctionType.equals(other)
                    && getMetadata().equals(other.getMetadata());
            }

            @Override
            public int hashCode() {
                return java.util.Objects.hash(baseFunctionType, metadata);
            }
        };
    }

    /**
     * Creates an extended function type with parameter and result names.
     *
     * @param parameterTypes the parameter types
     * @param resultTypes the result types
     * @param parameterNames the parameter names
     * @param resultNames the result names
     * @return an extended function type
     */
    static ExtendedFunctionType createWithNames(
            final WasmValueType[] parameterTypes,
            final WasmValueType[] resultTypes,
            final String[] parameterNames,
            final String[] resultNames) {

        final FunctionType baseType = FunctionType.create(parameterTypes, resultTypes);
        final FunctionTypeMetadata metadata = FunctionTypeMetadata.builder()
            .parameterNames(parameterNames)
            .resultNames(resultNames)
            .build();

        return create(baseType, metadata);
    }

    /**
     * Creates an extended function type with documentation.
     *
     * @param parameterTypes the parameter types
     * @param resultTypes the result types
     * @param parameterNames the parameter names
     * @param resultNames the result names
     * @param parameterDocs the parameter documentation
     * @param resultDocs the result documentation
     * @return an extended function type with documentation
     */
    static ExtendedFunctionType createWithDocumentation(
            final WasmValueType[] parameterTypes,
            final WasmValueType[] resultTypes,
            final String[] parameterNames,
            final String[] resultNames,
            final String[] parameterDocs,
            final String[] resultDocs) {

        final FunctionType baseType = FunctionType.create(parameterTypes, resultTypes);
        final FunctionTypeMetadata metadata = FunctionTypeMetadata.builder()
            .parameterNames(parameterNames)
            .resultNames(resultNames)
            .parameterDocumentation(parameterDocs)
            .resultDocumentation(resultDocs)
            .build();

        return create(baseType, metadata);
    }

    /**
     * Creates an extended function type with performance hints.
     *
     * @param baseFunctionType the base function type
     * @param performanceHints the performance hints
     * @return an extended function type with performance hints
     */
    static ExtendedFunctionType createWithPerformanceHints(
            final FunctionType baseFunctionType,
            final FunctionTypeMetadata.PerformanceHints performanceHints) {

        final FunctionTypeMetadata metadata = FunctionTypeMetadata.builder()
            .performanceHints(performanceHints)
            .build();

        return create(baseFunctionType, metadata);
    }
}