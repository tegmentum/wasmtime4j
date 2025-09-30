package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a position in source code with file, line, column, and optional symbol name.
 *
 * <p>This class provides precise location information for mapping WebAssembly instructions
 * back to their original source code locations. It includes both position data and optional
 * symbol information for enhanced debugging.
 *
 * <p>Immutable value object that implements proper equals/hashCode for use in collections.
 *
 * @since 1.0.0
 */
public final class SourcePosition {

    private final String source;
    private final int line;
    private final int column;
    private final String name;

    /**
     * Creates a new source position.
     *
     * @param source the source file path or identifier
     * @param line the line number (1-based)
     * @param column the column number (0-based)
     * @param name optional symbol name (function, variable, etc.)
     * @throws IllegalArgumentException if source is null or empty, or if line/column are negative
     */
    public SourcePosition(final String source, final int line, final int column, final String name) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source cannot be null or empty");
        }
        if (line < 1) {
            throw new IllegalArgumentException("Line number must be positive (1-based): " + line);
        }
        if (column < 0) {
            throw new IllegalArgumentException("Column number cannot be negative: " + column);
        }

        this.source = source;
        this.line = line;
        this.column = column;
        this.name = name;
    }

    /**
     * Creates a new source position without a symbol name.
     *
     * @param source the source file path or identifier
     * @param line the line number (1-based)
     * @param column the column number (0-based)
     * @throws IllegalArgumentException if source is null or empty, or if line/column are negative
     */
    public SourcePosition(final String source, final int line, final int column) {
        this(source, line, column, null);
    }

    /**
     * Gets the source file path or identifier.
     *
     * @return the source file path or identifier
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the line number (1-based).
     *
     * @return the line number
     */
    public int getLine() {
        return line;
    }

    /**
     * Gets the column number (0-based).
     *
     * @return the column number
     */
    public int getColumn() {
        return column;
    }

    /**
     * Gets the optional symbol name.
     *
     * @return the symbol name if available
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Creates a new source position with the specified name.
     *
     * @param name the symbol name
     * @return a new source position with the specified name
     */
    public SourcePosition withName(final String name) {
        return new SourcePosition(this.source, this.line, this.column, name);
    }

    /**
     * Creates a new source position without a name.
     *
     * @return a new source position without a name
     */
    public SourcePosition withoutName() {
        return new SourcePosition(this.source, this.line, this.column, null);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SourcePosition that = (SourcePosition) obj;
        return line == that.line
                && column == that.column
                && Objects.equals(source, that.source)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, line, column, name);
    }

    @Override
    public String toString() {
        if (name != null) {
            return String.format("%s:%d:%d (%s)", source, line, column, name);
        } else {
            return String.format("%s:%d:%d", source, line, column);
        }
    }

    /**
     * Creates a formatted string representation for debugging displays.
     *
     * @param includeSource whether to include the source file path
     * @return formatted string representation
     */
    public String toDisplayString(final boolean includeSource) {
        final StringBuilder sb = new StringBuilder();

        if (includeSource) {
            sb.append(source).append(':');
        }

        sb.append(line).append(':').append(column);

        if (name != null) {
            sb.append(" (").append(name).append(')');
        }

        return sb.toString();
    }

    /**
     * Checks if this position is valid.
     *
     * @return true if the position has valid coordinates
     */
    public boolean isValid() {
        return source != null && !source.trim().isEmpty() && line >= 1 && column >= 0;
    }

    /**
     * Compares this position with another for ordering by line, then column.
     *
     * @param other the other source position
     * @return comparison result
     * @throws IllegalArgumentException if other is null
     */
    public int compareTo(final SourcePosition other) {
        if (other == null) {
            throw new IllegalArgumentException("Other position cannot be null");
        }

        final int sourceCompare = this.source.compareTo(other.source);
        if (sourceCompare != 0) {
            return sourceCompare;
        }

        final int lineCompare = Integer.compare(this.line, other.line);
        if (lineCompare != 0) {
            return lineCompare;
        }

        return Integer.compare(this.column, other.column);
    }

    /**
     * Checks if this position is in the same source file as another.
     *
     * @param other the other source position
     * @return true if both positions are in the same source file
     */
    public boolean isSameSource(final SourcePosition other) {
        return other != null && Objects.equals(this.source, other.source);
    }

    /**
     * Checks if this position is on the same line as another.
     *
     * @param other the other source position
     * @return true if both positions are on the same line in the same source
     */
    public boolean isSameLine(final SourcePosition other) {
        return isSameSource(other) && this.line == other.line;
    }

    /**
     * Calculates the distance in lines between this position and another.
     *
     * @param other the other source position
     * @return the line distance, or -1 if not in the same source
     */
    public int lineDistance(final SourcePosition other) {
        if (!isSameSource(other)) {
            return -1;
        }
        return Math.abs(this.line - other.line);
    }
}