package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Represents a WebAssembly instruction address with function index and bytecode offset.
 *
 * <p>This class provides precise addressing for WebAssembly instructions, enabling
 * accurate mapping between WebAssembly bytecode and source code locations. It includes
 * both the function context and the specific instruction offset within that function.
 *
 * <p>Immutable value object that implements proper equals/hashCode for use in collections.
 *
 * @since 1.0.0
 */
public final class WasmAddress implements Comparable<WasmAddress> {

    private final int functionIndex;
    private final int instructionOffset;

    /**
     * Creates a new WebAssembly address.
     *
     * @param functionIndex the function index in the module
     * @param instructionOffset the bytecode offset within the function
     * @throws IllegalArgumentException if functionIndex or instructionOffset are negative
     */
    public WasmAddress(final int functionIndex, final int instructionOffset) {
        if (functionIndex < 0) {
            throw new IllegalArgumentException("Function index cannot be negative: " + functionIndex);
        }
        if (instructionOffset < 0) {
            throw new IllegalArgumentException("Instruction offset cannot be negative: " + instructionOffset);
        }

        this.functionIndex = functionIndex;
        this.instructionOffset = instructionOffset;
    }

    /**
     * Gets the function index in the WebAssembly module.
     *
     * @return the function index
     */
    public int getFunctionIndex() {
        return functionIndex;
    }

    /**
     * Gets the instruction offset within the function.
     *
     * @return the instruction offset
     */
    public int getInstructionOffset() {
        return instructionOffset;
    }

    /**
     * Creates a new address with the specified function index.
     *
     * @param functionIndex the new function index
     * @return a new address with the specified function index
     * @throws IllegalArgumentException if functionIndex is negative
     */
    public WasmAddress withFunctionIndex(final int functionIndex) {
        return new WasmAddress(functionIndex, this.instructionOffset);
    }

    /**
     * Creates a new address with the specified instruction offset.
     *
     * @param instructionOffset the new instruction offset
     * @return a new address with the specified instruction offset
     * @throws IllegalArgumentException if instructionOffset is negative
     */
    public WasmAddress withInstructionOffset(final int instructionOffset) {
        return new WasmAddress(this.functionIndex, instructionOffset);
    }

    /**
     * Creates a new address with an offset added to the current instruction offset.
     *
     * @param offset the offset to add
     * @return a new address with the adjusted instruction offset
     * @throws IllegalArgumentException if the resulting offset would be negative
     */
    public WasmAddress addOffset(final int offset) {
        final int newOffset = this.instructionOffset + offset;
        if (newOffset < 0) {
            throw new IllegalArgumentException("Resulting offset cannot be negative: " + newOffset);
        }
        return new WasmAddress(this.functionIndex, newOffset);
    }

    /**
     * Checks if this address is in the same function as another.
     *
     * @param other the other WebAssembly address
     * @return true if both addresses are in the same function
     */
    public boolean isSameFunction(final WasmAddress other) {
        return other != null && this.functionIndex == other.functionIndex;
    }

    /**
     * Calculates the instruction distance between this address and another in the same function.
     *
     * @param other the other WebAssembly address
     * @return the instruction distance, or -1 if not in the same function
     */
    public int instructionDistance(final WasmAddress other) {
        if (!isSameFunction(other)) {
            return -1;
        }
        return Math.abs(this.instructionOffset - other.instructionOffset);
    }

    /**
     * Checks if this address is valid.
     *
     * @return true if both function index and instruction offset are non-negative
     */
    public boolean isValid() {
        return functionIndex >= 0 && instructionOffset >= 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final WasmAddress that = (WasmAddress) obj;
        return functionIndex == that.functionIndex && instructionOffset == that.instructionOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionIndex, instructionOffset);
    }

    @Override
    public int compareTo(final WasmAddress other) {
        if (other == null) {
            throw new IllegalArgumentException("Other address cannot be null");
        }

        final int functionCompare = Integer.compare(this.functionIndex, other.functionIndex);
        if (functionCompare != 0) {
            return functionCompare;
        }

        return Integer.compare(this.instructionOffset, other.instructionOffset);
    }

    @Override
    public String toString() {
        return String.format("func[%d]+0x%x", functionIndex, instructionOffset);
    }

    /**
     * Creates a formatted string representation for debugging displays.
     *
     * @param showHex whether to display offsets in hexadecimal
     * @return formatted string representation
     */
    public String toDisplayString(final boolean showHex) {
        if (showHex) {
            return String.format("func[%d]+0x%x", functionIndex, instructionOffset);
        } else {
            return String.format("func[%d]+%d", functionIndex, instructionOffset);
        }
    }

    /**
     * Creates a compact string representation.
     *
     * @return compact string representation
     */
    public String toCompactString() {
        return String.format("%d:%x", functionIndex, instructionOffset);
    }

    /**
     * Parses a WebAssembly address from a string representation.
     *
     * <p>Supports formats:
     * <ul>
     * <li>"func[N]+0xM" - function N, offset M (hex)</li>
     * <li>"func[N]+M" - function N, offset M (decimal)</li>
     * <li>"N:M" - function N, offset M (hex)</li>
     * </ul>
     *
     * @param str the string representation
     * @return the parsed WebAssembly address
     * @throws IllegalArgumentException if the string format is invalid
     */
    public static WasmAddress parse(final String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("Address string cannot be null or empty");
        }

        final String trimmed = str.trim();

        // Try compact format "N:M"
        if (trimmed.contains(":") && !trimmed.startsWith("func")) {
            final String[] parts = trimmed.split(":");
            if (parts.length == 2) {
                try {
                    final int functionIndex = Integer.parseInt(parts[0]);
                    final int instructionOffset = Integer.parseInt(parts[1], 16);
                    return new WasmAddress(functionIndex, instructionOffset);
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid address format: " + str, e);
                }
            }
        }

        // Try full format "func[N]+0xM" or "func[N]+M"
        if (trimmed.startsWith("func[")) {
            final int bracketEnd = trimmed.indexOf(']');
            if (bracketEnd > 5) {
                try {
                    final int functionIndex = Integer.parseInt(trimmed.substring(5, bracketEnd));
                    final String offsetPart = trimmed.substring(bracketEnd + 1);

                    if (offsetPart.startsWith("+0x")) {
                        final int instructionOffset = Integer.parseInt(offsetPart.substring(3), 16);
                        return new WasmAddress(functionIndex, instructionOffset);
                    } else if (offsetPart.startsWith("+")) {
                        final int instructionOffset = Integer.parseInt(offsetPart.substring(1));
                        return new WasmAddress(functionIndex, instructionOffset);
                    }
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid address format: " + str, e);
                }
            }
        }

        throw new IllegalArgumentException("Invalid address format: " + str);
    }

    /**
     * Creates a WebAssembly address for the start of a function.
     *
     * @param functionIndex the function index
     * @return an address pointing to the start of the function
     * @throws IllegalArgumentException if functionIndex is negative
     */
    public static WasmAddress functionStart(final int functionIndex) {
        return new WasmAddress(functionIndex, 0);
    }

    /**
     * Validates that the address components are within reasonable bounds.
     *
     * @param maxFunctionIndex maximum valid function index
     * @param maxInstructionOffset maximum valid instruction offset
     * @return true if the address is within bounds
     */
    public boolean isWithinBounds(final int maxFunctionIndex, final int maxInstructionOffset) {
        return functionIndex >= 0 && functionIndex <= maxFunctionIndex
                && instructionOffset >= 0 && instructionOffset <= maxInstructionOffset;
    }
}