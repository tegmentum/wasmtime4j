package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly function type.
 * 
 * <p>A function type describes the signature of a WebAssembly function,
 * including parameter types and return types.
 * 
 * @since 1.0.0
 */
public final class FunctionType implements WasmType {
    
    private final WasmValueType[] paramTypes;
    private final WasmValueType[] returnTypes;
    
    /**
     * Creates a new function type.
     * 
     * @param paramTypes the parameter types
     * @param returnTypes the return types
     */
    public FunctionType(final WasmValueType[] paramTypes, final WasmValueType[] returnTypes) {
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