package ai.tegmentum.wasmtime4j.debug;

/**
 * Result of executing an operation on a single debug target.
 *
 * <p>This record contains the outcome of a debugging operation on a specific target,
 * including the debug event produced and any error that occurred.
 *
 * @param targetId the target identifier
 * @param event the debug event produced by the operation, may be null if operation failed
 * @param error the error that occurred, may be null if operation succeeded
 *
 * @since 1.0.0
 */
public record TargetExecutionResult(
        String targetId,
        DebugEvent event,
        Throwable error
) {

    /**
     * Checks if the operation was successful.
     *
     * @return true if the operation succeeded (no error)
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Checks if the operation failed.
     *
     * @return true if the operation failed (has error)
     */
    public boolean isFailure() {
        return error != null;
    }

    /**
     * Gets the error message if the operation failed.
     *
     * @return the error message, or null if no error
     */
    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }

    /**
     * Gets the debug event type if available.
     *
     * @return the event type, or null if no event
     */
    public DebugEventType getEventType() {
        return event != null ? event.getType() : null;
    }

    /**
     * Formats the result as a string.
     *
     * @return formatted result
     */
    public String format() {
        if (isSuccess()) {
            final String eventInfo = event != null
                ? String.format(" -> %s", event.getType())
                : " -> completed";
            return String.format("%s: SUCCESS%s", targetId, eventInfo);
        } else {
            return String.format("%s: FAILED -> %s", targetId, getErrorMessage());
        }
    }

    @Override
    public String toString() {
        return format();
    }
}