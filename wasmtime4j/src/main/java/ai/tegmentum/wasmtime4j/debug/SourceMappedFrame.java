package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;
import java.util.Optional;

/**
 * A stack frame with source mapping information.
 *
 * <p>This class represents a single frame in a WebAssembly stack trace that has been
 * mapped to source code information using source maps and/or DWARF debugging data.
 * It combines the original WebAssembly address with resolved source position and
 * function symbol information.
 *
 * @since 1.0.0
 */
public final class SourceMappedFrame {

    private final WasmAddress wasmAddress;
    private final SourcePosition sourcePosition;
    private final FunctionSymbol functionSymbol;

    /**
     * Creates a new source mapped frame.
     *
     * @param wasmAddress the WebAssembly address
     * @param sourcePosition the mapped source position (optional)
     * @param functionSymbol the function symbol information (optional)
     * @throws IllegalArgumentException if wasmAddress is null
     */
    public SourceMappedFrame(
            final WasmAddress wasmAddress,
            final SourcePosition sourcePosition,
            final FunctionSymbol functionSymbol) {
        if (wasmAddress == null) {
            throw new IllegalArgumentException("WASM address cannot be null");
        }

        this.wasmAddress = wasmAddress;
        this.sourcePosition = sourcePosition;
        this.functionSymbol = functionSymbol;
    }

    /**
     * Creates a source mapped frame with only WebAssembly address information.
     *
     * @param wasmAddress the WebAssembly address
     */
    public SourceMappedFrame(final WasmAddress wasmAddress) {
        this(wasmAddress, null, null);
    }

    /**
     * Gets the WebAssembly address for this frame.
     *
     * @return the WASM address
     */
    public WasmAddress getWasmAddress() {
        return wasmAddress;
    }

    /**
     * Gets the mapped source position if available.
     *
     * @return the source position
     */
    public Optional<SourcePosition> getSourcePosition() {
        return Optional.ofNullable(sourcePosition);
    }

    /**
     * Gets the function symbol information if available.
     *
     * @return the function symbol
     */
    public Optional<FunctionSymbol> getFunctionSymbol() {
        return Optional.ofNullable(functionSymbol);
    }

    /**
     * Checks if this frame has source position information.
     *
     * @return true if source position is available
     */
    public boolean hasSourcePosition() {
        return sourcePosition != null;
    }

    /**
     * Checks if this frame has function symbol information.
     *
     * @return true if function symbol is available
     */
    public boolean hasFunctionSymbol() {
        return functionSymbol != null;
    }

    /**
     * Gets the function name if available.
     *
     * @return the function name, or a generated name if not available
     */
    public String getFunctionName() {
        if (functionSymbol != null) {
            return functionSymbol.getName();
        }
        return "func_" + wasmAddress.getFunctionIndex();
    }

    /**
     * Gets the source file name if available.
     *
     * @return the source file name, or "unknown" if not available
     */
    public String getSourceFile() {
        if (sourcePosition != null) {
            return sourcePosition.getSource();
        }
        if (functionSymbol != null) {
            return functionSymbol.getSourceFile();
        }
        return "unknown";
    }

    /**
     * Gets the line number if available.
     *
     * @return the line number, or -1 if not available
     */
    public int getLineNumber() {
        if (sourcePosition != null) {
            return sourcePosition.getLine();
        }
        if (functionSymbol != null) {
            return functionSymbol.getStartLine();
        }
        return -1;
    }

    /**
     * Gets the column number if available.
     *
     * @return the column number, or -1 if not available
     */
    public int getColumnNumber() {
        if (sourcePosition != null) {
            return sourcePosition.getColumn();
        }
        return -1;
    }

    /**
     * Creates a copy with updated source position.
     *
     * @param newSourcePosition the new source position
     * @return a new frame with updated source position
     */
    public SourceMappedFrame withSourcePosition(final SourcePosition newSourcePosition) {
        return new SourceMappedFrame(wasmAddress, newSourcePosition, functionSymbol);
    }

    /**
     * Creates a copy with updated function symbol.
     *
     * @param newFunctionSymbol the new function symbol
     * @return a new frame with updated function symbol
     */
    public SourceMappedFrame withFunctionSymbol(final FunctionSymbol newFunctionSymbol) {
        return new SourceMappedFrame(wasmAddress, sourcePosition, newFunctionSymbol);
    }

    /**
     * Creates a copy with both source position and function symbol.
     *
     * @param newSourcePosition the new source position
     * @param newFunctionSymbol the new function symbol
     * @return a new frame with updated information
     */
    public SourceMappedFrame with(final SourcePosition newSourcePosition, final FunctionSymbol newFunctionSymbol) {
        return new SourceMappedFrame(wasmAddress, newSourcePosition, newFunctionSymbol);
    }

    /**
     * Creates a formatted string representation for stack trace display.
     *
     * @param includeWasmInfo whether to include WebAssembly address information
     * @return formatted string representation
     */
    public String toDisplayString(final boolean includeWasmInfo) {
        final StringBuilder sb = new StringBuilder();

        // Function name and signature
        if (functionSymbol != null) {
            sb.append(functionSymbol.getName());
            if (!functionSymbol.getSignature().equals("() -> ()")) {
                sb.append(functionSymbol.getSignature());
            } else {
                sb.append("()");
            }
        } else {
            sb.append("func_").append(wasmAddress.getFunctionIndex()).append("()");
        }

        // Source location
        if (sourcePosition != null) {
            sb.append(" at ").append(sourcePosition.toString());
        } else if (functionSymbol != null && !functionSymbol.getSourceFile().equals("unknown")) {
            sb.append(" at ").append(functionSymbol.getSourceFile())
              .append(":").append(functionSymbol.getStartLine());
        } else if (includeWasmInfo) {
            sb.append(" at ").append(wasmAddress.toString());
        }

        return sb.toString();
    }

    /**
     * Creates a detailed string representation with all available information.
     *
     * @return detailed string representation
     */
    public String toDetailedString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Frame: ").append(getFunctionName());

        if (sourcePosition != null) {
            sb.append("\n  Source: ").append(sourcePosition.toString());
        }

        if (functionSymbol != null) {
            sb.append("\n  Function: ").append(functionSymbol.toDetailedString());
        }

        sb.append("\n  WASM: ").append(wasmAddress.toString());

        return sb.toString();
    }

    /**
     * Creates a compact string representation suitable for logs.
     *
     * @return compact string representation
     */
    public String toCompactString() {
        if (sourcePosition != null) {
            return String.format("%s@%s:%d:%d",
                    getFunctionName(),
                    sourcePosition.getSource(),
                    sourcePosition.getLine(),
                    sourcePosition.getColumn());
        } else {
            return String.format("%s@%s",
                    getFunctionName(),
                    wasmAddress.toCompactString());
        }
    }

    /**
     * Checks if this frame represents the same location as another frame.
     *
     * @param other the other frame
     * @return true if they represent the same location
     */
    public boolean isSameLocation(final SourceMappedFrame other) {
        if (other == null) {
            return false;
        }

        // Compare WebAssembly addresses
        if (!wasmAddress.equals(other.wasmAddress)) {
            return false;
        }

        // Compare source positions if available
        if (sourcePosition != null && other.sourcePosition != null) {
            return sourcePosition.equals(other.sourcePosition);
        }

        return true;
    }

    /**
     * Gets the mapping quality based on available information.
     *
     * @return mapping quality enumeration
     */
    public MappingQuality getMappingQuality() {
        if (sourcePosition != null && functionSymbol != null) {
            return MappingQuality.EXCELLENT;
        } else if (sourcePosition != null || functionSymbol != null) {
            return MappingQuality.GOOD;
        } else {
            return MappingQuality.POOR;
        }
    }

    /**
     * Enumeration of mapping quality levels.
     */
    public enum MappingQuality {
        /** Excellent mapping with both source position and function symbol */
        EXCELLENT,
        /** Good mapping with either source position or function symbol */
        GOOD,
        /** Poor mapping with only WebAssembly address */
        POOR
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final SourceMappedFrame that = (SourceMappedFrame) obj;
        return Objects.equals(wasmAddress, that.wasmAddress)
                && Objects.equals(sourcePosition, that.sourcePosition)
                && Objects.equals(functionSymbol, that.functionSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wasmAddress, sourcePosition, functionSymbol);
    }

    @Override
    public String toString() {
        return toDisplayString(true);
    }
}