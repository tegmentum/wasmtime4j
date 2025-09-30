package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a stack frame in the WebAssembly call stack during debugging.
 * Contains function information, variables, and source location mapping.
 */
public final class StackFrame {
    private final int functionIndex;
    private final String functionName;
    private final int byteOffset;
    private final Map<String, WasmValue> variables;
    private final SourceLocation sourceLocation;

    public StackFrame(final int functionIndex, final String functionName,
                     final int byteOffset, final Map<String, WasmValue> variables,
                     final SourceLocation sourceLocation) {
        this.functionIndex = functionIndex;
        this.functionName = functionName;
        this.byteOffset = byteOffset;
        this.variables = Collections.unmodifiableMap(new HashMap<>(
            Objects.requireNonNull(variables, "Variables cannot be null")));
        this.sourceLocation = sourceLocation;
    }

    // Getters
    public int getFunctionIndex() { return functionIndex; }
    public String getFunctionName() { return functionName; }
    public int getByteOffset() { return byteOffset; }
    public Map<String, WasmValue> getVariables() { return variables; }
    public Optional<SourceLocation> getSourceLocation() { return Optional.ofNullable(sourceLocation); }

    /**
     * Gets a variable by name.
     */
    public Optional<WasmValue> getVariable(final String name) {
        return Optional.ofNullable(variables.get(name));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof StackFrame)) return false;
        final StackFrame that = (StackFrame) o;
        return functionIndex == that.functionIndex &&
               byteOffset == that.byteOffset &&
               Objects.equals(functionName, that.functionName) &&
               Objects.equals(variables, that.variables) &&
               Objects.equals(sourceLocation, that.sourceLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionIndex, functionName, byteOffset, variables, sourceLocation);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StackFrame{");
        sb.append("function=").append(functionName).append("(").append(functionIndex).append(")");
        sb.append(", byteOffset=").append(byteOffset);
        if (sourceLocation != null) {
            sb.append(", source=").append(sourceLocation);
        }
        sb.append(", variables=").append(variables.size());
        sb.append('}');
        return sb.toString();
    }
}