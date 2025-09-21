package ai.tegmentum.wasmtime4j.diagnostics;

/**
 * Formatting options for WebAssembly stack traces.
 *
 * <p>This interface provides configuration options for controlling how stack traces
 * are formatted and displayed, including verbosity levels and filtering options.
 *
 * @since 1.0.0
 */
public interface StackTraceFormatOptions {

    /**
     * Verbosity levels for stack trace formatting.
     */
    enum Verbosity {
        /** Minimal information - function names only */
        MINIMAL,
        /** Standard information - function names and offsets */
        STANDARD,
        /** Detailed information - includes source locations when available */
        DETAILED,
        /** Comprehensive information - includes all available debug data */
        COMPREHENSIVE
    }

    /**
     * Frame filtering options.
     */
    enum FrameFilter {
        /** Show all frames */
        ALL,
        /** Show only WebAssembly frames */
        WASM_ONLY,
        /** Show only host function frames */
        HOST_ONLY,
        /** Show only frames with source information */
        SOURCE_ONLY,
        /** Custom filtering */
        CUSTOM
    }

    /**
     * Gets the verbosity level for formatting.
     *
     * @return the verbosity level
     */
    Verbosity getVerbosity();

    /**
     * Gets the frame filter to apply.
     *
     * @return the frame filter
     */
    FrameFilter getFrameFilter();

    /**
     * Gets the maximum number of frames to display.
     *
     * @return the maximum frame count, or -1 for no limit
     */
    int getMaxFrames();

    /**
     * Checks if source code snippets should be included.
     *
     * @return true if source snippets should be included
     */
    boolean includeSourceSnippets();

    /**
     * Checks if local variable information should be included.
     *
     * @return true if local variables should be included
     */
    boolean includeLocalVariables();

    /**
     * Checks if instruction offsets should be included.
     *
     * @return true if instruction offsets should be included
     */
    boolean includeInstructionOffsets();

    /**
     * Checks if module names should be included.
     *
     * @return true if module names should be included
     */
    boolean includeModuleNames();

    /**
     * Checks if frame numbers should be included.
     *
     * @return true if frame numbers should be included
     */
    boolean includeFrameNumbers();

    /**
     * Gets the indentation string for nested frames.
     *
     * @return the indentation string
     */
    String getIndentation();

    /**
     * Gets the line separator for stack trace lines.
     *
     * @return the line separator
     */
    String getLineSeparator();

    /**
     * Checks if colors should be used in the output.
     *
     * @return true if colors should be used
     */
    boolean useColors();

    /**
     * Creates a builder for constructing StackTraceFormatOptions instances.
     *
     * @return a new format options builder
     */
    static StackTraceFormatOptionsBuilder builder() {
        return new StackTraceFormatOptionsBuilder();
    }

    /**
     * Creates minimal formatting options.
     *
     * @return minimal format options
     */
    static StackTraceFormatOptions minimal() {
        return builder()
            .verbosity(Verbosity.MINIMAL)
            .frameFilter(FrameFilter.ALL)
            .maxFrames(10)
            .build();
    }

    /**
     * Creates standard formatting options.
     *
     * @return standard format options
     */
    static StackTraceFormatOptions standard() {
        return builder()
            .verbosity(Verbosity.STANDARD)
            .frameFilter(FrameFilter.ALL)
            .maxFrames(50)
            .includeInstructionOffsets(true)
            .includeModuleNames(true)
            .build();
    }

    /**
     * Creates detailed formatting options.
     *
     * @return detailed format options
     */
    static StackTraceFormatOptions detailed() {
        return builder()
            .verbosity(Verbosity.DETAILED)
            .frameFilter(FrameFilter.ALL)
            .maxFrames(100)
            .includeSourceSnippets(true)
            .includeLocalVariables(true)
            .includeInstructionOffsets(true)
            .includeModuleNames(true)
            .includeFrameNumbers(true)
            .build();
    }
}