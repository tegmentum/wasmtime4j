package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Optional;

/**
 * Source code location information for debugging.
 *
 * <p>This interface provides mapping from WebAssembly bytecode to original source code
 * locations when debug information or source maps are available.
 *
 * @since 1.0.0
 */
public interface SourceLocation {

    /**
     * Gets the source file name.
     *
     * @return the source file name, or empty if not available
     */
    Optional<String> getFileName();

    /**
     * Gets the source file path.
     *
     * @return the source file path, or empty if not available
     */
    Optional<String> getFilePath();

    /**
     * Gets the line number in the source file.
     *
     * @return the line number (1-based), or empty if not available
     */
    Optional<Integer> getLineNumber();

    /**
     * Gets the column number in the source line.
     *
     * @return the column number (1-based), or empty if not available
     */
    Optional<Integer> getColumnNumber();

    /**
     * Gets the end line number for multi-line constructs.
     *
     * @return the end line number, or empty if not available
     */
    Optional<Integer> getEndLineNumber();

    /**
     * Gets the end column number for multi-character constructs.
     *
     * @return the end column number, or empty if not available
     */
    Optional<Integer> getEndColumnNumber();

    /**
     * Gets the source text for this location if available.
     *
     * @return the source text, or empty if not available
     */
    Optional<String> getSourceText();

    /**
     * Gets the function or method name at this location.
     *
     * @return the function name, or empty if not available
     */
    Optional<String> getFunctionName();

    /**
     * Checks if this location represents the beginning of a statement.
     *
     * @return true if this is a statement boundary
     */
    boolean isStatementBoundary();

    /**
     * Checks if this location represents the beginning of an expression.
     *
     * @return true if this is an expression boundary
     */
    boolean isExpressionBoundary();

    /**
     * Formats the source location as a human-readable string.
     *
     * @return formatted source location
     */
    String format();

    /**
     * Creates a builder for constructing SourceLocation instances.
     *
     * @return a new source location builder
     */
    static SourceLocationBuilder builder() {
        return new SourceLocationBuilder();
    }

    /**
     * Creates a SourceLocation with basic file and line information.
     *
     * @param fileName the file name
     * @param lineNumber the line number
     * @return the source location
     */
    static SourceLocation of(final String fileName, final int lineNumber) {
        return builder()
            .fileName(fileName)
            .lineNumber(lineNumber)
            .build();
    }

    /**
     * Creates a SourceLocation with file, line, and column information.
     *
     * @param fileName the file name
     * @param lineNumber the line number
     * @param columnNumber the column number
     * @return the source location
     */
    static SourceLocation of(final String fileName, final int lineNumber, final int columnNumber) {
        return builder()
            .fileName(fileName)
            .lineNumber(lineNumber)
            .columnNumber(columnNumber)
            .build();
    }
}