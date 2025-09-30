package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the execution context at a specific point during WebAssembly debugging.
 * Contains information about the current function, instruction position, local variables,
 * and operand stack state.
 */
public final class ExecutionContext {
    private final int functionIndex;
    private final String functionName;
    private final int byteOffset;
    private final int instructionOffset;
    private final Map<String, WasmValue> localVariables;
    private final List<WasmValue> operandStack;
    private final SourceLocation sourceLocation;

    public ExecutionContext(final int functionIndex, final String functionName,
                          final int byteOffset, final int instructionOffset,
                          final Map<String, WasmValue> localVariables,
                          final List<WasmValue> operandStack,
                          final SourceLocation sourceLocation) {
        this.functionIndex = functionIndex;
        this.functionName = functionName;
        this.byteOffset = byteOffset;
        this.instructionOffset = instructionOffset;
        this.localVariables = Collections.unmodifiableMap(new HashMap<>(
            Objects.requireNonNull(localVariables, "Local variables cannot be null")));
        this.operandStack = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(operandStack, "Operand stack cannot be null")));
        this.sourceLocation = sourceLocation;
    }

    // Getters
    public int getFunctionIndex() { return functionIndex; }
    public String getFunctionName() { return functionName; }
    public int getByteOffset() { return byteOffset; }
    public int getInstructionOffset() { return instructionOffset; }
    public Map<String, WasmValue> getLocalVariables() { return localVariables; }
    public List<WasmValue> getOperandStack() { return operandStack; }
    public Optional<SourceLocation> getSourceLocation() { return Optional.ofNullable(sourceLocation); }

    /**
     * Gets a local variable by name.
     */
    public Optional<WasmValue> getLocalVariable(final String name) {
        return Optional.ofNullable(localVariables.get(name));
    }

    /**
     * Gets the top value from the operand stack.
     */
    public Optional<WasmValue> getStackTop() {
        return operandStack.isEmpty() ? Optional.empty() :
               Optional.of(operandStack.get(operandStack.size() - 1));
    }

    /**
     * Gets the stack depth.
     */
    public int getStackDepth() {
        return operandStack.size();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionContext)) return false;
        final ExecutionContext that = (ExecutionContext) o;
        return functionIndex == that.functionIndex &&
               byteOffset == that.byteOffset &&
               instructionOffset == that.instructionOffset &&
               Objects.equals(functionName, that.functionName) &&
               Objects.equals(localVariables, that.localVariables) &&
               Objects.equals(operandStack, that.operandStack) &&
               Objects.equals(sourceLocation, that.sourceLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionIndex, functionName, byteOffset, instructionOffset,
                          localVariables, operandStack, sourceLocation);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExecutionContext{");
        sb.append("function=").append(functionName).append("(").append(functionIndex).append(")");
        sb.append(", byteOffset=").append(byteOffset);
        sb.append(", instructionOffset=").append(instructionOffset);
        if (sourceLocation != null) {
            sb.append(", source=").append(sourceLocation);
        }
        sb.append(", locals=").append(localVariables.size());
        sb.append(", stackDepth=").append(operandStack.size());
        sb.append('}');
        return sb.toString();
    }
}