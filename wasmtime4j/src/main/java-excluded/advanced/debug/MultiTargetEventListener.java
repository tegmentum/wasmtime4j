package ai.tegmentum.wasmtime4j.debug;

/**
 * Event listener for multi-target debugging operations.
 *
 * <p>This interface defines callbacks for various multi-target debugging events,
 * allowing clients to monitor and react to changes in the debugging session.
 *
 * @since 1.0.0
 */
public interface MultiTargetEventListener {

    /**
     * Called when a target is added to the multi-target session.
     *
     * @param target the debug target that was added
     */
    default void onTargetAdded(final DebugTarget target) {
        // Default implementation does nothing
    }

    /**
     * Called when a target is removed from the multi-target session.
     *
     * @param target the debug target that was removed
     */
    default void onTargetRemoved(final DebugTarget target) {
        // Default implementation does nothing
    }

    /**
     * Called when a global breakpoint is set.
     *
     * @param breakpoint the global breakpoint that was set
     */
    default void onGlobalBreakpointSet(final GlobalBreakpoint breakpoint) {
        // Default implementation does nothing
    }

    /**
     * Called when a global breakpoint is removed.
     *
     * @param breakpoint the global breakpoint that was removed
     */
    default void onGlobalBreakpointRemoved(final GlobalBreakpoint breakpoint) {
        // Default implementation does nothing
    }

    /**
     * Called when a debug event occurs on any target.
     *
     * @param target the target where the event occurred
     * @param event the debug event
     */
    default void onTargetEvent(final DebugTarget target, final DebugEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called periodically with performance metrics and status updates.
     *
     * @param metrics the current performance metrics
     */
    default void onStatusUpdate(final MultiTargetPerformanceMetrics metrics) {
        // Default implementation does nothing
    }

    /**
     * Called when a multi-target operation completes.
     *
     * @param result the execution result
     */
    default void onMultiTargetOperationComplete(final MultiTargetExecutionResult result) {
        // Default implementation does nothing
    }

    /**
     * Called when synchronization between targets occurs.
     *
     * @param operationType the type of synchronization operation
     * @param targetCount the number of targets involved
     * @param durationMs the synchronization duration in milliseconds
     */
    default void onTargetSynchronization(final String operationType, final int targetCount, final long durationMs) {
        // Default implementation does nothing
    }

    /**
     * Called when an error occurs in the multi-target debugging session.
     *
     * @param error the error message
     * @param cause the underlying cause, may be null
     */
    default void onError(final String error, final Throwable cause) {
        // Default implementation does nothing
    }
}