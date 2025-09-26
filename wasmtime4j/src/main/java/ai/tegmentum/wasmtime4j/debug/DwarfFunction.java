package ai.tegmentum.wasmtime4j.debug;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Function information extracted from DWARF debugging data.
 *
 * <p>This class represents detailed function information obtained from DWARF
 * debugging sections in WebAssembly modules. It provides low-level debugging
 * information including addresses, frame base expressions, and variable locations.
 *
 * @since 1.0.0
 */
public final class DwarfFunction {

    private final String name;
    private final long lowPc;
    private final long highPc;
    private final byte[] frameBase;
    private final List<DwarfVariable> parameters;
    private final List<DwarfVariable> localVariables;
    private final String mangledName;
    private final Integer functionIndex;

    /**
     * Creates a new DWARF function.
     *
     * @param name the function name
     * @param lowPc the low PC (start address)
     * @param highPc the high PC (end address or size)
     * @param frameBase the frame base expression (optional)
     * @param parameters the function parameters
     * @param localVariables the local variables
     * @param mangledName the mangled name (optional)
     * @param functionIndex the WebAssembly function index (optional)
     */
    public DwarfFunction(
            final String name,
            final long lowPc,
            final long highPc,
            final byte[] frameBase,
            final List<DwarfVariable> parameters,
            final List<DwarfVariable> localVariables,
            final String mangledName,
            final Integer functionIndex) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Function name cannot be null or empty");
        }
        if (lowPc < 0) {
            throw new IllegalArgumentException("Low PC cannot be negative: " + lowPc);
        }
        if (highPc < lowPc) {
            throw new IllegalArgumentException("High PC cannot be less than low PC: " + highPc);
        }
        if (functionIndex != null && functionIndex < 0) {
            throw new IllegalArgumentException("Function index cannot be negative: " + functionIndex);
        }

        this.name = name;
        this.lowPc = lowPc;
        this.highPc = highPc;
        this.frameBase = frameBase != null ? frameBase.clone() : null;
        this.parameters = parameters != null ? List.copyOf(parameters) : Collections.emptyList();
        this.localVariables = localVariables != null ? List.copyOf(localVariables) : Collections.emptyList();
        this.mangledName = mangledName;
        this.functionIndex = functionIndex;
    }

    /**
     * Creates a simple DWARF function with minimal information.
     *
     * @param name the function name
     * @param lowPc the start address
     * @param highPc the end address
     */
    public DwarfFunction(final String name, final long lowPc, final long highPc) {
        this(name, lowPc, highPc, null, null, null, null, null);
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
     * Gets the low PC (start address).
     *
     * @return the start address
     */
    public long getLowPc() {
        return lowPc;
    }

    /**
     * Gets the high PC (end address or size).
     *
     * @return the end address or size
     */
    public long getHighPc() {
        return highPc;
    }

    /**
     * Gets the frame base expression if available.
     *
     * @return the frame base expression bytes
     */
    public Optional<byte[]> getFrameBase() {
        return Optional.ofNullable(frameBase).map(byte[]::clone);
    }

    /**
     * Gets the function parameters.
     *
     * @return an immutable list of parameters
     */
    public List<DwarfVariable> getParameters() {
        return parameters;
    }

    /**
     * Gets the local variables.
     *
     * @return an immutable list of local variables
     */
    public List<DwarfVariable> getLocalVariables() {
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
     * Gets the WebAssembly function index if available.
     *
     * @return the function index
     */
    public Optional<Integer> getFunctionIndex() {
        return Optional.ofNullable(functionIndex);
    }

    /**
     * Gets the function size in bytes.
     *
     * @return the function size
     */
    public long getSize() {
        return highPc - lowPc;
    }

    /**
     * Checks if an address is within this function's range.
     *
     * @param address the address to check
     * @return true if the address is within this function
     */
    public boolean containsAddress(final long address) {
        return address >= lowPc && address < highPc;
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
     * Checks if this function has frame base information.
     *
     * @return true if frame base is available
     */
    public boolean hasFrameBase() {
        return frameBase != null;
    }

    /**
     * Finds a parameter by name.
     *
     * @param parameterName the parameter name
     * @return the parameter if found
     */
    public Optional<DwarfVariable> getParameter(final String parameterName) {
        return parameters.stream()
                .filter(p -> Objects.equals(p.getName(), parameterName))
                .findFirst();
    }

    /**
     * Finds a local variable by name.
     *
     * @param variableName the variable name
     * @return the local variable if found
     */
    public Optional<DwarfVariable> getLocalVariable(final String variableName) {
        return localVariables.stream()
                .filter(v -> Objects.equals(v.getName(), variableName))
                .findFirst();
    }

    /**
     * Gets all variables (parameters and locals) in this function.
     *
     * @return a combined list of all variables
     */
    public List<DwarfVariable> getAllVariables() {
        if (parameters.isEmpty()) {
            return localVariables;
        }
        if (localVariables.isEmpty()) {
            return parameters;
        }

        final List<DwarfVariable> all = new java.util.ArrayList<>(parameters);
        all.addAll(localVariables);
        return Collections.unmodifiableList(all);
    }

    /**
     * Creates a formatted display string.
     *
     * @param includeAddress whether to include address information
     * @return formatted display string
     */
    public String toDisplayString(final boolean includeAddress) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);

        if (includeAddress) {
            sb.append(" [0x").append(Long.toHexString(lowPc));
            sb.append("-0x").append(Long.toHexString(highPc)).append("]");
        }

        if (functionIndex != null) {
            sb.append(" (func_").append(functionIndex).append(")");
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
        sb.append(name);

        if (mangledName != null) {
            sb.append(" (").append(mangledName).append(")");
        }

        sb.append("\n  Address: 0x").append(Long.toHexString(lowPc));
        sb.append("-0x").append(Long.toHexString(highPc));
        sb.append(" (").append(getSize()).append(" bytes)");

        if (functionIndex != null) {
            sb.append("\n  WASM index: ").append(functionIndex);
        }

        if (hasFrameBase()) {
            sb.append("\n  Frame base: ").append(frameBase.length).append(" bytes");
        }

        if (hasParameters()) {
            sb.append("\n  Parameters: ").append(parameters.size());
            for (final DwarfVariable param : parameters) {
                sb.append("\n    ").append(param.getName()).append(": ").append(param.getType());
            }
        }

        if (hasLocalVariables()) {
            sb.append("\n  Local variables: ").append(localVariables.size());
        }

        return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final DwarfFunction that = (DwarfFunction) obj;
        return lowPc == that.lowPc
                && highPc == that.highPc
                && Objects.equals(name, that.name)
                && java.util.Arrays.equals(frameBase, that.frameBase)
                && Objects.equals(parameters, that.parameters)
                && Objects.equals(localVariables, that.localVariables)
                && Objects.equals(mangledName, that.mangledName)
                && Objects.equals(functionIndex, that.functionIndex);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, lowPc, highPc, parameters, localVariables, mangledName, functionIndex);
        result = 31 * result + java.util.Arrays.hashCode(frameBase);
        return result;
    }

    @Override
    public String toString() {
        return toDisplayString(true);
    }
}