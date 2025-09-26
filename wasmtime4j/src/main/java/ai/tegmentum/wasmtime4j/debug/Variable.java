package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Represents a variable in a WebAssembly debugging context.
 *
 * <p>Variables can be local variables, function parameters, or global variables
 * that are accessible at the current execution point.
 *
 * @since 1.0.0
 */
public final class Variable {

    /** Variable scopes */
    public enum Scope {
        /** Local variable */
        LOCAL,
        /** Function parameter */
        PARAMETER,
        /** Global variable */
        GLOBAL,
        /** Imported variable */
        IMPORTED,
        /** Exported variable */
        EXPORTED,
        /** Temporary variable */
        TEMPORARY
    }

    private final String name;
    private final String type;
    private final VariableValue value;
    private final Scope scope;
    private final int index;
    private final boolean mutable;
    private final boolean visible;
    private final String description;

    /**
     * Creates a new variable.
     *
     * @param name the variable name
     * @param type the variable type
     * @param value the variable value
     * @param scope the variable scope
     * @param index the variable index
     * @param mutable whether the variable is mutable
     * @param visible whether the variable is visible at current position
     * @param description optional description
     */
    public Variable(final String name, final String type, final VariableValue value,
                   final Scope scope, final int index, final boolean mutable,
                   final boolean visible, final String description) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.scope = Objects.requireNonNull(scope, "scope cannot be null");
        this.index = index;
        this.mutable = mutable;
        this.visible = visible;
        this.description = description;
    }

    /**
     * Creates a local variable.
     *
     * @param name variable name
     * @param type variable type
     * @param value variable value
     * @param index local index
     * @return new local variable
     */
    public static Variable local(final String name, final String type,
                                final VariableValue value, final int index) {
        return new Variable(name, type, value, Scope.LOCAL, index, true, true, null);
    }

    /**
     * Creates a parameter variable.
     *
     * @param name parameter name
     * @param type parameter type
     * @param value parameter value
     * @param index parameter index
     * @return new parameter variable
     */
    public static Variable parameter(final String name, final String type,
                                    final VariableValue value, final int index) {
        return new Variable(name, type, value, Scope.PARAMETER, index, false, true, null);
    }

    /**
     * Creates a global variable.
     *
     * @param name global name
     * @param type global type
     * @param value global value
     * @param index global index
     * @param mutable whether the global is mutable
     * @return new global variable
     */
    public static Variable global(final String name, final String type,
                                 final VariableValue value, final int index, final boolean mutable) {
        return new Variable(name, type, value, Scope.GLOBAL, index, mutable, true, null);
    }

    /**
     * Gets the variable name.
     *
     * @return variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the variable type.
     *
     * @return variable type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the variable value.
     *
     * @return variable value
     */
    public VariableValue getValue() {
        return value;
    }

    /**
     * Gets the variable scope.
     *
     * @return variable scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets the variable index.
     *
     * @return variable index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Checks if the variable is mutable.
     *
     * @return true if mutable
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Checks if the variable is visible at the current execution point.
     *
     * @return true if visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Gets the variable description.
     *
     * @return description or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Creates a copy of this variable with a new value.
     *
     * @param newValue the new value
     * @return new variable with updated value
     */
    public Variable withValue(final VariableValue newValue) {
        return new Variable(name, type, newValue, scope, index, mutable, visible, description);
    }

    /**
     * Creates a copy of this variable with updated visibility.
     *
     * @param visible the new visibility
     * @return new variable with updated visibility
     */
    public Variable withVisible(final boolean visible) {
        return new Variable(name, type, value, scope, index, mutable, visible, description);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Variable other = (Variable) obj;
        return index == other.index &&
                mutable == other.mutable &&
                visible == other.visible &&
                Objects.equals(name, other.name) &&
                Objects.equals(type, other.type) &&
                Objects.equals(value, other.value) &&
                scope == other.scope &&
                Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value, scope, index, mutable, visible, description);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Variable{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", value=").append(value);
        sb.append(", scope=").append(scope);
        sb.append(", index=").append(index);
        sb.append(", mutable=").append(mutable);
        sb.append(", visible=").append(visible);
        if (description != null) {
            sb.append(", description='").append(description).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}