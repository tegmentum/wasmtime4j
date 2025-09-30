package ai.tegmentum.wasmtime4j.debug;

/**
 * Execution tracer interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionTracer {

  /** Starts tracing execution. */
  void startTracing();

  /** Stops tracing execution. */
  void stopTracing();

  /**
   * Gets trace events.
   *
   * @return list of trace events
   */
  java.util.List<TraceEvent> getTraceEvents();

  /** Clears trace history. */
  void clearTrace();

  /**
   * Checks if tracing is active.
   *
   * @return true if tracing
   */
  boolean isTracing();

  /**
   * Sets the trace filter.
   *
   * @param filter trace filter
   */
  void setTraceFilter(TraceFilter filter);

  /** Trace event interface. */
  interface TraceEvent {
    /**
     * Gets the event type.
     *
     * @return event type
     */
    TraceEventType getEventType();

    /**
     * Gets the timestamp.
     *
     * @return timestamp in nanoseconds
     */
    long getTimestamp();

    /**
     * Gets the instruction address.
     *
     * @return instruction address
     */
    long getInstructionAddress();

    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets additional data.
     *
     * @return event data
     */
    String getEventData();
  }

  /** Trace event type enumeration. */
  enum TraceEventType {
    /** Function call. */
    FUNCTION_CALL,
    /** Function return. */
    FUNCTION_RETURN,
    /** Instruction execution. */
    INSTRUCTION,
    /** Memory access. */
    MEMORY_ACCESS,
    /** Exception thrown. */
    EXCEPTION
  }

  /** Trace filter interface. */
  interface TraceFilter {
    /**
     * Checks if event should be traced.
     *
     * @param eventType event type
     * @param functionName function name
     * @return true if should trace
     */
    boolean shouldTrace(TraceEventType eventType, String functionName);

    /**
     * Gets the maximum trace buffer size.
     *
     * @return buffer size
     */
    int getMaxBufferSize();
  }
}
