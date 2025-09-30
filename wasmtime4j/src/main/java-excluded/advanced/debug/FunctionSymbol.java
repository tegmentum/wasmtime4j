package ai.tegmentum.wasmtime4j.debug;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Function symbol information extracted from debugging data.
 *
 * <p>This class contains detailed information about a WebAssembly function
 * including its name, signature, source location, and variable information.
 * It combines data from both source maps and DWARF debugging information.
 *
 * @since 1.0.0
 */
public final class FunctionSymbol {

    private final String name;
    private final String signature;
    private final String sourceFile;
    private final int startLine;
    private final int endLine;
    private final List<VariableSymbol> parameters;
    private final List<VariableSymbol> localVariables;
    private final String mangledName;
    private final int functionIndex;

    /**
     * Creates a new function symbol.
     *
     * @param name the function name
     * @param signature the function signature
     * @param sourceFile the source file containing the function
     * @param startLine the starting line number
     * @param endLine the ending line number
     * @param parameters the function parameters
     * @param localVariables the local variables
     * @param mangledName the mangled name (optional)
     * @param functionIndex the WebAssembly function index
     */
    public FunctionSymbol(
            final String name,
            final String signature,
            final String sourceFile,
            final int startLine,
            final int endLine,
            final List<VariableSymbol> parameters,
            final List<VariableSymbol> localVariables,
            final String mangledName,
            final int functionIndex) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Function name cannot be null or empty");
        }
        if (signature == null) {
            throw new IllegalArgumentException("Function signature cannot be null");
        }
        if (sourceFile == null || sourceFile.trim().isEmpty()) {
            throw new IllegalArgumentException("Source file cannot be null or empty");
        }
        if (startLine < 0) {
            throw new IllegalArgumentException("Start line cannot be negative: " + startLine);
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("End line cannot be before start line: " + endLine);
        }
        if (functionIndex < 0) {
            throw new IllegalArgumentException("Function index cannot be negative: " + functionIndex);
        }

        this.name = name;
        this.signature = signature;
        this.sourceFile = sourceFile;
        this.startLine = startLine;
        this.endLine = endLine;
        this.parameters = parameters != null ? List.copyOf(parameters) : Collections.emptyList();
        this.localVariables = localVariables != null ? List.copyOf(localVariables) : Collections.emptyList();
        this.mangledName = mangledName;
        this.functionIndex = functionIndex;
    }

    /**
     * Creates a simple function symbol with minimal information.
     *
     * @param name the function name
     * @param functionIndex the WebAssembly function index
     */
    public FunctionSymbol(final String name, final int functionIndex) {
        this(name, "() -> ()", "unknown", 1, 1, null, null, null, functionIndex);
    }

    /**
     * Gets the function name.
     *
     * @return the function name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the function signature.
     *
     * @return the function signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Gets the source file containing this function.
     *
     * @return the source file path
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Gets the starting line number.
     *
     * @return the start line (1-based)
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Gets the ending line number.
     *
     * @return the end line (1-based)
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Gets the function parameters.
     *
     * @return an immutable list of parameters
     */
    public List<VariableSymbol> getParameters() {
        return parameters;
    }

    /**
     * Gets the local variables.
     *
     * @return an immutable list of local variables
     */
    public List<VariableSymbol> getLocalVariables() {
        return localVariables;
    }

    /**
     * Gets the mangled name if available.
     *
     * @return the mangled name
     */
    public Optional<String> getMangledName() {
        return Optional.ofNullable(mangledName);
    }

    /**
     * Gets the WebAssembly function index.
     *
     * @return the function index
     */
    public int getFunctionIndex() {
        return functionIndex;
    }

    /**
     * Gets the total number of lines in this function.
     *
     * @return the line count
     */
    public int getLineCount() {
        return endLine - startLine + 1;
    }

    /**
     * Gets the total number of variables (parameters + locals).
     *
     * @return the total variable count
     */
    public int getTotalVariables() {
        return parameters.size() + localVariables.size();
    }

    /**
     * Checks if this function has parameter information.
     *
     * @return true if parameters are available
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * Checks if this function has local variable information.
     *
     * @return true if local variables are available
     */
    public boolean hasLocalVariables() {
        return !localVariables.isEmpty();
    }

    /**
     * Finds a parameter by name.
     *
     * @param parameterName the parameter name to find
     * @return the parameter if found
     */
    public Optional<VariableSymbol> getParameter(final String parameterName) {
        return parameters.stream()
                .filter(p -> Objects.equals(p.getName(), parameterName))
                .findFirst();
    }

    /**
     * Finds a local variable by name.
     *
     * @param variableName the variable name to find
     * @return the local variable if found
     */
    public Optional<VariableSymbol> getLocalVariable(final String variableName) {
        return localVariables.stream()
                .filter(v -> Objects.equals(v.getName(), variableName))
                .findFirst();
    }

    /**
     * Finds any variable (parameter or local) by name.
     *
     * @param variableName the variable name to find
     * @return the variable if found
     */
    public Optional<VariableSymbol> getVariable(final String variableName) {
        Optional<VariableSymbol> param = getParameter(variableName);
        if (param.isPresent()) {
            return param;
        }
        return getLocalVariable(variableName);
    }

    /**
     * Checks if a line number is within this function's range.
     *
     * @param line the line number to check
     * @return true if the line is within this function
     */
    public boolean containsLine(final int line) {
        return line >= startLine && line <= endLine;
    }

    /**
     * Creates a copy of this function symbol with updated information.
     *
     * @param newName the new function name
     * @return a new function symbol with the updated name
     */
    public FunctionSymbol withName(final String newName) {
        return new FunctionSymbol(
                newName, signature, sourceFile, startLine, endLine,
                parameters, localVariables, mangledName, functionIndex
        );
    }

    /**
     * Creates a copy with updated signature.
     *
     * @param newSignature the new signature
     * @return a new function symbol with the updated signature
     */
    public FunctionSymbol withSignature(final String newSignature) {
        return new FunctionSymbol(
                name, newSignature, sourceFile, startLine, endLine,
                parameters, localVariables, mangledName, functionIndex
        );
    }

    /**
     * Creates a formatted display string for this function.
     *
     * @param includeLocation whether to include source location
     * @return formatted display string
     */
    public String toDisplayString(final boolean includeLocation) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name).append(signature);

        if (includeLocation && !sourceFile.equals("unknown")) {
            sb.append(" at ").append(sourceFile).append(":").append(startLine);
            if (endLine != startLine) {
                sb.append("-").append(endLine);
            }
        }

        return sb.toString();
    }

    /**
     * Creates a detailed string representation including all information.
     *
     * @return detailed string representation
     */
    public String toDetailedString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(name).append(signature);
        sb.append(" [func_").append(functionIndex).append("]");

        if (!sourceFile.equals("unknown")) {
            sb.append(" at ").append(sourceFile).append(":").append(startLine);
            if (endLine != startLine) {
                sb.append("-").append(endLine);
            }
        }

        if (!parameters.isEmpty()) {
            sb.append("\n  Parameters: ").append(parameters.size());
        }

        if (!localVariables.isEmpty()) {
            sb.append("\n  Local variables: ").append(localVariables.size());
        }

        if (mangledName != null) {
            sb.append("\n  Mangled name: ").append(mangledName);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final FunctionSymbol that = (FunctionSymbol) obj;
        return startLine == that.startLine
                && endLine == that.endLine
                && functionIndex == that.functionIndex
                && Objects.equals(name, that.name)
                && Objects.equals(signature, that.signature)
                && Objects.equals(sourceFile, that.sourceFile)
                && Objects.equals(parameters, that.parameters)
                && Objects.equals(localVariables, that.localVariables)
                && Objects.equals(mangledName, that.mangledName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, signature, sourceFile, startLine, endLine,
                parameters, localVariables, mangledName, functionIndex);
    }

    @Override
    public String toString() {
        return toDisplayString(true);
    }
}