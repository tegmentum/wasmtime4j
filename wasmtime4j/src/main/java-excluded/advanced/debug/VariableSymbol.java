package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;
import java.util.Optional;

/**
 * Variable symbol information from debugging data.
 *
 * <p>This class represents information about a variable (parameter or local)
 * extracted from DWARF debugging information or source maps. It includes
 * the variable name, type, scope, and WebAssembly local index if available.
 *
 * @since 1.0.0
 */
public final class VariableSymbol {

    private final String name;
    private final String type;
    private final int scopeStart;
    private final int scopeEnd;
    private final Integer wasmIndex;
    private final VariableKind kind;

    /**
     * Enumeration of variable kinds.
     */
    public enum VariableKind {
        /** Function parameter */
        PARAMETER,
        /** Local variable */
        LOCAL,
        /** Global variable */
        GLOBAL,
        /** Unknown kind */
        UNKNOWN
    }

    /**
     * Creates a new variable symbol.
     *
     * @param name the variable name
     * @param type the variable type
     * @param scopeStart the scope start line
     * @param scopeEnd the scope end line
     * @param wasmIndex the WebAssembly local index (optional)
     * @param kind the variable kind
     */
    public VariableSymbol(
            final String name,
            final String type,
            final int scopeStart,
            final int scopeEnd,
            final Integer wasmIndex,
            final VariableKind kind) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Variable type cannot be null");
        }
        if (scopeStart < 0) {
            throw new IllegalArgumentException("Scope start cannot be negative: " + scopeStart);
        }
        if (scopeEnd < scopeStart) {
            throw new IllegalArgumentException("Scope end cannot be before scope start: " + scopeEnd);
        }
        if (wasmIndex != null && wasmIndex < 0) {
            throw new IllegalArgumentException("WASM index cannot be negative: " + wasmIndex);
        }

        this.name = name;
        this.type = type;
        this.scopeStart = scopeStart;
        this.scopeEnd = scopeEnd;
        this.wasmIndex = wasmIndex;
        this.kind = kind != null ? kind : VariableKind.UNKNOWN;
    }

    /**
     * Creates a simple variable symbol.
     *
     * @param name the variable name
     * @param type the variable type
     * @param kind the variable kind
     */
    public VariableSymbol(final String name, final String type, final VariableKind kind) {
        this(name, type, 1, Integer.MAX_VALUE, null, kind);
    }

    /**
     * Gets the variable name.
     *
     * @return the variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the variable type.
     *
     * @return the variable type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the scope start line.
     *
     * @return the scope start line (1-based)
     */
    public int getScopeStart() {
        return scopeStart;
    }

    /**
     * Gets the scope end line.
     *
     * @return the scope end line (1-based)
     */
    public int getScopeEnd() {
        return scopeEnd;
    }

    /**
     * Gets the WebAssembly local index if available.
     *
     * @return the WASM index
     */
    public Optional<Integer> getWasmIndex() {
        return Optional.ofNullable(wasmIndex);
    }

    /**
     * Gets the variable kind.
     *
     * @return the variable kind
     */
    public VariableKind getKind() {
        return kind;
    }

    /**
     * Gets the scope size in lines.
     *
     * @return the number of lines in scope
     */
    public int getScopeSize() {
        return scopeEnd - scopeStart + 1;
    }

    /**
     * Checks if a line number is within this variable's scope.
     *
     * @param line the line number to check
     * @return true if the line is within scope
     */
    public boolean isInScope(final int line) {
        return line >= scopeStart && line <= scopeEnd;
    }

    /**
     * Checks if this variable is a parameter.
     *
     * @return true if this is a parameter
     */
    public boolean isParameter() {
        return kind == VariableKind.PARAMETER;
    }

    /**
     * Checks if this variable is a local variable.
     *
     * @return true if this is a local variable
     */
    public boolean isLocal() {
        return kind == VariableKind.LOCAL;
    }

    /**
     * Checks if this variable is a global variable.
     *
     * @return true if this is a global variable
     */
    public boolean isGlobal() {
        return kind == VariableKind.GLOBAL;
    }

    /**
     * Checks if this variable has a known WebAssembly index.
     *
     * @return true if WASM index is available
     */
    public boolean hasWasmIndex() {
        return wasmIndex != null;
    }

    /**
     * Creates a copy with updated type information.
     *
     * @param newType the new type
     * @return a new variable symbol with updated type
     */
    public VariableSymbol withType(final String newType) {
        return new VariableSymbol(name, newType, scopeStart, scopeEnd, wasmIndex, kind);
    }

    /**
     * Creates a copy with updated scope information.
     *
     * @param newScopeStart the new scope start
     * @param newScopeEnd the new scope end
     * @return a new variable symbol with updated scope
     */
    public VariableSymbol withScope(final int newScopeStart, final int newScopeEnd) {
        return new VariableSymbol(name, type, newScopeStart, newScopeEnd, wasmIndex, kind);
    }

    /**
     * Creates a copy with WebAssembly index information.
     *
     * @param newWasmIndex the WebAssembly index
     * @return a new variable symbol with WASM index
     */
    public VariableSymbol withWasmIndex(final int newWasmIndex) {
        return new VariableSymbol(name, type, scopeStart, scopeEnd, newWasmIndex, kind);
    }

    /**
     * Creates a formatted display string for this variable.
     *
     * @param includeScope whether to include scope information
     * @return formatted display string
     */
    public String toDisplayString(final boolean includeScope) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(type);

        if (includeScope && scopeStart != 1 || scopeEnd != Integer.MAX_VALUE) {
            sb.append(" (scope: ").append(scopeStart);
            if (scopeEnd != Integer.MAX_VALUE) {
                sb.append("-").append(scopeEnd);
            }
            sb.append(")");
        }

        if (wasmIndex != null) {
            sb.append(" [local_").append(wasmIndex).append("]");
        }

        return sb.toString();
    }

    /**
     * Creates a detailed string representation.
     *
     * @return detailed string representation
     */
    public String toDetailedString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(kind.toString().toLowerCase()).append(" ");
        sb.append(name).append(": ").append(type);

        if (scopeStart != 1 || scopeEnd != Integer.MAX_VALUE) {
            sb.append("\n  Scope: line ").append(scopeStart);
            if (scopeEnd != Integer.MAX_VALUE) {
                sb.append(" to ").append(scopeEnd);
            } else {
                sb.append(" to end");
            }
        }

        if (wasmIndex != null) {
            sb.append("\n  WASM index: ").append(wasmIndex);
        }

        return sb.toString();
    }

    /**
     * Creates a parameter variable symbol.
     *
     * @param name the parameter name
     * @param type the parameter type
     * @param wasmIndex the WebAssembly index
     * @return a parameter variable symbol
     */
    public static VariableSymbol parameter(final String name, final String type, final int wasmIndex) {
        return new VariableSymbol(name, type, 1, Integer.MAX_VALUE, wasmIndex, VariableKind.PARAMETER);
    }

    /**
     * Creates a local variable symbol.
     *
     * @param name the variable name
     * @param type the variable type
     * @param scopeStart the scope start line
     * @param scopeEnd the scope end line
     * @param wasmIndex the WebAssembly index
     * @return a local variable symbol
     */
    public static VariableSymbol local(
            final String name, final String type, final int scopeStart, final int scopeEnd, final int wasmIndex) {
        return new VariableSymbol(name, type, scopeStart, scopeEnd, wasmIndex, VariableKind.LOCAL);
    }

    /**
     * Creates a global variable symbol.
     *
     * @param name the variable name
     * @param type the variable type
     * @return a global variable symbol
     */
    public static VariableSymbol global(final String name, final String type) {
        return new VariableSymbol(name, type, 1, Integer.MAX_VALUE, null, VariableKind.GLOBAL);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final VariableSymbol that = (VariableSymbol) obj;
        return scopeStart == that.scopeStart
                && scopeEnd == that.scopeEnd
                && Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(wasmIndex, that.wasmIndex)
                && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, scopeStart, scopeEnd, wasmIndex, kind);
    }

    @Override
    public String toString() {
        return toDisplayString(false);
    }
}