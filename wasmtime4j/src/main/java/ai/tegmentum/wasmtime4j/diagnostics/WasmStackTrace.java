package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.List;
import java.util.Optional;

/**
 * WebAssembly execution stack trace information.
 *
 * <p>This interface provides access to the call stack at the time of an error,
 * including both WebAssembly and host function calls.
 *
 * @since 1.0.0
 */
public interface WasmStackTrace {

    /**
     * Stack frame types to distinguish different kinds of calls.
     */
    enum FrameType {
        /** WebAssembly function frame */
        WASM,
        /** Host function frame */
        HOST,
        /** Native/JNI frame */
        NATIVE,
        /** Unknown frame type */
        UNKNOWN
    }

    /**
     * Represents a single frame in the WebAssembly call stack.
     */
    interface StackFrame {
        /**
         * Gets the frame type.
         *
         * @return the frame type
         */
        FrameType getType();

        /**
         * Gets the module name for this frame.
         *
         * @return the module name, or empty if not available
         */
        Optional<String> getModuleName();

        /**
         * Gets the function name for this frame.
         *
         * @return the function name, or empty if not available
         */
        Optional<String> getFunctionName();

        /**
         * Gets the function index for this frame.
         *
         * @return the function index, or empty if not available
         */
        Optional<Integer> getFunctionIndex();

        /**
         * Gets the instruction offset within the function.
         *
         * @return the instruction offset, or empty if not available
         */
        Optional<Long> getInstructionOffset();

        /**
         * Gets the source location if debug information is available.
         *
         * @return the source location, or empty if not available
         */
        Optional<SourceLocation> getSourceLocation();

        /**
         * Gets whether this frame represents the current execution point.
         *
         * @return true if this is the current frame
         */
        boolean isCurrent();

        /**
         * Gets the frame's local variable information if available.
         *
         * @return list of local variables, or empty list if not available
         */
        List<VariableInfo> getLocalVariables();
    }

    /**
     * Gets all stack frames from most recent (top) to least recent (bottom).
     *
     * @return list of stack frames
     */
    List<StackFrame> getFrames();

    /**
     * Gets the current (top) frame.
     *
     * @return the current frame, or empty if no frames
     */
    Optional<StackFrame> getCurrentFrame();

    /**
     * Gets the frame depth (number of frames).
     *
     * @return the frame depth
     */
    int getDepth();

    /**
     * Gets frames filtered by type.
     *
     * @param frameType the frame type to filter by
     * @return list of frames of the specified type
     */
    List<StackFrame> getFramesByType(FrameType frameType);

    /**
     * Gets the WebAssembly frames only.
     *
     * @return list of WebAssembly frames
     */
    default List<StackFrame> getWasmFrames() {
        return getFramesByType(FrameType.WASM);
    }

    /**
     * Gets the host function frames only.
     *
     * @return list of host function frames
     */
    default List<StackFrame> getHostFrames() {
        return getFramesByType(FrameType.HOST);
    }

    /**
     * Checks if the stack trace is complete.
     *
     * @return true if the stack trace is complete
     */
    boolean isComplete();

    /**
     * Checks if the stack trace was truncated due to limits.
     *
     * @return true if the stack trace was truncated
     */
    boolean isTruncated();

    /**
     * Gets the maximum frame limit that was applied.
     *
     * @return the frame limit, or empty if no limit was applied
     */
    Optional<Integer> getFrameLimit();

    /**
     * Formats the stack trace as a human-readable string.
     *
     * @return formatted stack trace
     */
    String format();

    /**
     * Formats the stack trace with the specified options.
     *
     * @param options the formatting options
     * @return formatted stack trace
     */
    String format(StackTraceFormatOptions options);

    /**
     * Creates a builder for constructing WasmStackTrace instances.
     *
     * @return a new stack trace builder
     */
    static WasmStackTraceBuilder builder() {
        return new WasmStackTraceBuilder();
    }
}