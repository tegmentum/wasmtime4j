package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Objects;

/**
 * Represents a variable value with type information for debugging.
 */
public final class VariableValue {
    private final String name;
    private final WasmValue value;
    private final String typeName;
    private final VariableScope scope;

    public VariableValue(final String name, final WasmValue value,
                        final String typeName, final VariableScope scope) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.value = Objects.requireNonNull(value, "Value cannot be null");
        this.typeName = typeName;
        this.scope = Objects.requireNonNull(scope, "Scope cannot be null");
    }

    // Getters
    public String getName() { return name; }
    public WasmValue getValue() { return value; }
    public String getTypeName() { return typeName; }
    public VariableScope getScope() { return scope; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableValue)) return false;
        final VariableValue that = (VariableValue) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(value, that.value) &&
               Objects.equals(typeName, that.typeName) &&
               scope == that.scope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, typeName, scope);
    }

    @Override
    public String toString() {
        return String.format("VariableValue{name='%s', value=%s, type='%s', scope=%s}",
                           name, value, typeName, scope);
    }

    /**
     * Variable scope enumeration.
     */
    public enum VariableScope {
        LOCAL,
        PARAMETER,
        GLOBAL,
        STATIC
    }
}