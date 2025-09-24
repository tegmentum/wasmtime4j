package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Represents a source code location with file, line, column information.
 * Used for mapping WebAssembly byte offsets back to original source code.
 */
public final class SourceLocation {
    private final String fileName;
    private final int line;
    private final int column;
    private final String symbolName;
    private final int byteOffset;

    public SourceLocation(final String fileName, final int line, final int column,
                         final String symbolName, final int byteOffset) {
        this.fileName = fileName;
        this.line = line;
        this.column = column;
        this.symbolName = symbolName;
        this.byteOffset = byteOffset;
    }

    // Getters
    public String getFileName() { return fileName; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getSymbolName() { return symbolName; }
    public int getByteOffset() { return byteOffset; }

    public boolean hasSymbol() {
        return symbolName != null && !symbolName.trim().isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceLocation)) return false;
        final SourceLocation that = (SourceLocation) o;
        return line == that.line &&
               column == that.column &&
               byteOffset == that.byteOffset &&
               Objects.equals(fileName, that.fileName) &&
               Objects.equals(symbolName, that.symbolName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, line, column, symbolName, byteOffset);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (fileName != null) {
            sb.append(fileName);
        } else {
            sb.append("unknown");
        }
        sb.append(":").append(line);
        if (column > 0) {
            sb.append(":").append(column);
        }
        if (hasSymbol()) {
            sb.append(" (").append(symbolName).append(")");
        }
        sb.append(" [@").append(byteOffset).append("]");
        return sb.toString();
    }
}