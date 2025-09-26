package ai.tegmentum.wasmtime4j.debug;

/**
 * Types of debug protocol messages.
 *
 * <p>This enum defines all message types used in the remote debugging protocol.
 * Messages are categorized as requests (require responses), responses (reply to requests),
 * or notifications (no response expected).
 *
 * @since 1.0.0
 */
public enum DebugMessageType {

    // Handshake and connection management
    HANDSHAKE(MessageCategory.REQUEST),
    HANDSHAKE_RESPONSE(MessageCategory.RESPONSE),
    DISCONNECT(MessageCategory.NOTIFICATION),
    HEARTBEAT(MessageCategory.REQUEST),
    HEARTBEAT_RESPONSE(MessageCategory.RESPONSE),

    // Session management
    SESSION_ATTACHED(MessageCategory.NOTIFICATION),
    SESSION_DETACHED(MessageCategory.NOTIFICATION),
    SESSION_LIST(MessageCategory.REQUEST),
    SESSION_LIST_RESPONSE(MessageCategory.RESPONSE),

    // Breakpoint management
    SET_BREAKPOINT(MessageCategory.REQUEST),
    BREAKPOINT_SET(MessageCategory.RESPONSE),
    REMOVE_BREAKPOINT(MessageCategory.REQUEST),
    BREAKPOINT_REMOVED(MessageCategory.RESPONSE),
    LIST_BREAKPOINTS(MessageCategory.REQUEST),
    BREAKPOINTS_LIST(MessageCategory.RESPONSE),
    BREAKPOINT_HIT(MessageCategory.NOTIFICATION),

    // Execution control
    CONTINUE_EXECUTION(MessageCategory.REQUEST),
    EXECUTION_CONTINUED(MessageCategory.RESPONSE),
    STEP_INTO(MessageCategory.REQUEST),
    STEP_OVER(MessageCategory.REQUEST),
    STEP_OUT(MessageCategory.REQUEST),
    STEP_COMPLETED(MessageCategory.RESPONSE),
    PAUSE_EXECUTION(MessageCategory.REQUEST),
    EXECUTION_PAUSED(MessageCategory.RESPONSE),
    EXECUTION_STOPPED(MessageCategory.NOTIFICATION),

    // Stack and variables
    GET_STACK_TRACE(MessageCategory.REQUEST),
    STACK_TRACE(MessageCategory.RESPONSE),
    GET_VARIABLES(MessageCategory.REQUEST),
    VARIABLES(MessageCategory.RESPONSE),
    SET_VARIABLE(MessageCategory.REQUEST),
    VARIABLE_SET(MessageCategory.RESPONSE),

    // Expression evaluation
    EVALUATE_EXPRESSION(MessageCategory.REQUEST),
    EXPRESSION_RESULT(MessageCategory.RESPONSE),

    // Memory operations
    READ_MEMORY(MessageCategory.REQUEST),
    MEMORY_DATA(MessageCategory.RESPONSE),
    WRITE_MEMORY(MessageCategory.REQUEST),
    MEMORY_WRITTEN(MessageCategory.RESPONSE),
    SEARCH_MEMORY(MessageCategory.REQUEST),
    MEMORY_SEARCH_RESULT(MessageCategory.RESPONSE),

    // Profiling and performance
    START_PROFILING(MessageCategory.REQUEST),
    PROFILING_STARTED(MessageCategory.RESPONSE),
    STOP_PROFILING(MessageCategory.REQUEST),
    PROFILING_STOPPED(MessageCategory.RESPONSE),
    PROFILING_DATA(MessageCategory.NOTIFICATION),

    // Multi-target debugging
    ATTACH_TARGET(MessageCategory.REQUEST),
    TARGET_ATTACHED(MessageCategory.RESPONSE),
    DETACH_TARGET(MessageCategory.REQUEST),
    TARGET_DETACHED(MessageCategory.RESPONSE),
    LIST_TARGETS(MessageCategory.REQUEST),
    TARGETS_LIST(MessageCategory.RESPONSE),

    // Distributed debugging
    SYNC_BREAKPOINTS(MessageCategory.REQUEST),
    BREAKPOINTS_SYNCED(MessageCategory.RESPONSE),
    COORDINATE_STEP(MessageCategory.REQUEST),
    STEP_COORDINATED(MessageCategory.RESPONSE),
    BROADCAST_EVENT(MessageCategory.NOTIFICATION),

    // Time-travel debugging
    START_RECORDING(MessageCategory.REQUEST),
    RECORDING_STARTED(MessageCategory.RESPONSE),
    STOP_RECORDING(MessageCategory.REQUEST),
    RECORDING_STOPPED(MessageCategory.RESPONSE),
    REPLAY_TO_POINT(MessageCategory.REQUEST),
    REPLAY_COMPLETED(MessageCategory.RESPONSE),
    GET_EXECUTION_HISTORY(MessageCategory.REQUEST),
    EXECUTION_HISTORY(MessageCategory.RESPONSE),

    // Memory debugging
    START_HEAP_ANALYSIS(MessageCategory.REQUEST),
    HEAP_ANALYSIS_STARTED(MessageCategory.RESPONSE),
    GET_HEAP_SNAPSHOT(MessageCategory.REQUEST),
    HEAP_SNAPSHOT(MessageCategory.RESPONSE),
    DETECT_MEMORY_LEAKS(MessageCategory.REQUEST),
    MEMORY_LEAKS_DETECTED(MessageCategory.RESPONSE),

    // IDE integration
    SET_SOURCE_BREAKPOINT(MessageCategory.REQUEST),
    SOURCE_BREAKPOINT_SET(MessageCategory.RESPONSE),
    GET_SOURCE_MAP(MessageCategory.REQUEST),
    SOURCE_MAP(MessageCategory.RESPONSE),
    HOVER_INFO(MessageCategory.REQUEST),
    HOVER_INFO_RESPONSE(MessageCategory.RESPONSE),

    // Analytics and monitoring
    GET_DEBUG_STATS(MessageCategory.REQUEST),
    DEBUG_STATS(MessageCategory.RESPONSE),
    PERFORMANCE_METRICS(MessageCategory.NOTIFICATION),
    RESOURCE_USAGE(MessageCategory.NOTIFICATION),

    // Error handling
    ERROR(MessageCategory.RESPONSE),
    WARNING(MessageCategory.NOTIFICATION),
    INFO(MessageCategory.NOTIFICATION);

    private final MessageCategory category;

    DebugMessageType(final MessageCategory category) {
        this.category = category;
    }

    /**
     * Gets the message category.
     *
     * @return the message category
     */
    public MessageCategory getCategory() {
        return category;
    }

    /**
     * Checks if this is a request message type.
     *
     * @return true if this is a request message type
     */
    public boolean isRequest() {
        return category == MessageCategory.REQUEST;
    }

    /**
     * Checks if this is a response message type.
     *
     * @return true if this is a response message type
     */
    public boolean isResponse() {
        return category == MessageCategory.RESPONSE;
    }

    /**
     * Checks if this is a notification message type.
     *
     * @return true if this is a notification message type
     */
    public boolean isNotification() {
        return category == MessageCategory.NOTIFICATION;
    }

    /**
     * Message categories for protocol classification.
     */
    public enum MessageCategory {
        /** Request messages that expect a response */
        REQUEST,

        /** Response messages that reply to requests */
        RESPONSE,

        /** Notification messages that don't expect responses */
        NOTIFICATION
    }
}