package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution tracing configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionTracingConfig {

  /**
   * Checks if tracing is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets tracing enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the trace output destination.
   *
   * @return output destination
   */
  String getOutputDestination();

  /**
   * Sets the trace output destination.
   *
   * @param destination output destination
   */
  void setOutputDestination(String destination);

  /**
   * Gets the trace buffer size.
   *
   * @return buffer size
   */
  int getBufferSize();

  /**
   * Sets the trace buffer size.
   *
   * @param size buffer size
   */
  void setBufferSize(int size);

  /**
   * Gets trace filters.
   *
   * @return set of trace filters
   */
  java.util.Set<TraceFilter> getFilters();

  /**
   * Adds a trace filter.
   *
   * @param filter trace filter
   */
  void addFilter(TraceFilter filter);

  /**
   * Removes a trace filter.
   *
   * @param filter trace filter
   */
  void removeFilter(TraceFilter filter);

  /**
   * Gets the trace format.
   *
   * @return trace format
   */
  TraceFormat getFormat();

  /**
   * Sets the trace format.
   *
   * @param format trace format
   */
  void setFormat(TraceFormat format);

  /**
   * Gets the flush interval in milliseconds.
   *
   * @return flush interval
   */
  long getFlushInterval();

  /**
   * Sets the flush interval.
   *
   * @param intervalMs flush interval in milliseconds
   */
  void setFlushInterval(long intervalMs);

  /** Trace format enumeration. */
  enum TraceFormat {
    /** JSON format. */
    JSON,
    /** Binary format. */
    BINARY,
    /** Plain text. */
    TEXT,
    /** Chrome tracing format. */
    CHROME_TRACE
  }
}
