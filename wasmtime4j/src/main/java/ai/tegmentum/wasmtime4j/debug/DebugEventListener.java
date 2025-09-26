package ai.tegmentum.wasmtime4j.debug;

/**
 * Listener interface for WebAssembly debugging events.
 *
 * <p>Implementations of this interface can be registered with debug sessions
 * to receive notifications about debugging events such as breakpoint hits,
 * step completions, and execution state changes.
 *
 * @since 1.0.0
 */
public interface DebugEventListener {

    /**
     * Called when a debug event occurs.
     *
     * @param event the debug event
     */
    void onDebugEvent(final DebugEvent event);

    /**
     * Called when a breakpoint is hit.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the breakpoint event
     */
    default void onBreakpointHit(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when a step operation completes.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the step event
     */
    default void onStepComplete(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when execution is paused.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the pause event
     */
    default void onExecutionPaused(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when execution is resumed.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the resume event
     */
    default void onExecutionResumed(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when execution completes.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the completion event
     */
    default void onExecutionComplete(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when an error occurs during debugging.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the error event
     */
    default void onError(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when a variable value changes.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the variable change event
     */
    default void onVariableChanged(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when memory is modified.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the memory change event
     */
    default void onMemoryChanged(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when a function is called.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the function call event
     */
    default void onFunctionCall(final DebugEvent event) {
        onDebugEvent(event);
    }

    /**
     * Called when a function returns.
     *
     * <p>Default implementation calls {@link #onDebugEvent(DebugEvent)}.
     *
     * @param event the function return event
     */
    default void onFunctionReturn(final DebugEvent event) {
        onDebugEvent(event);
    }
}