package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution trace data interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionTraceData {

  /**
   * Gets the trace ID.
   *
   * @return trace ID
   */
  String getTraceId();

  /**
   * Gets the execution ID.
   *
   * @return execution ID
   */
  String getExecutionId();

  /**
   * Gets the trace start time.
   *
   * @return start timestamp
   */
  long getStartTime();

  /**
   * Gets the trace end time.
   *
   * @return end timestamp
   */
  long getEndTime();

  /**
   * Gets the total trace duration.
   *
   * @return duration in nanoseconds
   */
  long getDuration();

  /**
   * Gets trace events.
   *
   * @return list of trace events
   */
  java.util.List<TraceEvent> getEvents();

  /**
   * Gets trace metadata.
   *
   * @return metadata map
   */
  java.util.Map<String, Object> getMetadata();

  /**
   * Gets trace statistics.
   *
   * @return trace statistics
   */
  TraceStatistics getStatistics();

  /**
   * Filters trace events.
   *
   * @param filter trace filter
   * @return filtered events
   */
  java.util.List<TraceEvent> filter(TraceFilter filter);

  /**
   * Exports trace data.
   *
   * @param format export format
   * @return exported data
   */
  byte[] export(ExportFormat format);

  /**
   * Gets trace sampling configuration.
   *
   * @return sampling config
   */
  SamplingConfig getSamplingConfig();

  /**
   * Gets trace compression information.
   *
   * @return compression info
   */
  CompressionInfo getCompressionInfo();

  /** Trace event interface. */
  interface TraceEvent {
    /**
     * Gets the event ID.
     *
     * @return event ID
     */
    String getId();

    /**
     * Gets the event type.
     *
     * @return event type
     */
    EventType getType();

    /**
     * Gets the event timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the module name.
     *
     * @return module name
     */
    String getModuleName();

    /**
     * Gets the thread ID.
     *
     * @return thread ID
     */
    long getThreadId();

    /**
     * Gets event data.
     *
     * @return event data
     */
    java.util.Map<String, Object> getData();

    /**
     * Gets the event duration.
     *
     * @return duration in nanoseconds
     */
    long getDuration();

    /**
     * Gets the stack trace.
     *
     * @return stack trace
     */
    java.util.List<StackFrame> getStackTrace();
  }

  /** Trace statistics interface. */
  interface TraceStatistics {
    /**
     * Gets total event count.
     *
     * @return event count
     */
    int getTotalEvents();

    /**
     * Gets event count by type.
     *
     * @return event type counts
     */
    java.util.Map<EventType, Integer> getEventCounts();

    /**
     * Gets average event duration.
     *
     * @return average duration in nanoseconds
     */
    double getAverageEventDuration();

    /**
     * Gets total memory allocated during trace.
     *
     * @return memory in bytes
     */
    long getTotalMemoryAllocated();

    /**
     * Gets function call statistics.
     *
     * @return function call stats
     */
    FunctionCallStatistics getFunctionCallStatistics();
  }

  /** Sampling configuration interface. */
  interface SamplingConfig {
    /**
     * Gets the sampling rate.
     *
     * @return sampling rate (0.0-1.0)
     */
    double getSamplingRate();

    /**
     * Gets the sampling strategy.
     *
     * @return sampling strategy
     */
    SamplingStrategy getStrategy();

    /**
     * Gets the maximum trace size.
     *
     * @return max size in bytes
     */
    long getMaxTraceSize();
  }

  /** Compression information interface. */
  interface CompressionInfo {
    /**
     * Gets the compression algorithm.
     *
     * @return compression algorithm
     */
    String getAlgorithm();

    /**
     * Gets the original size.
     *
     * @return original size in bytes
     */
    long getOriginalSize();

    /**
     * Gets the compressed size.
     *
     * @return compressed size in bytes
     */
    long getCompressedSize();

    /**
     * Gets the compression ratio.
     *
     * @return compression ratio
     */
    double getCompressionRatio();
  }

  /** Function call statistics interface. */
  interface FunctionCallStatistics {
    /**
     * Gets total function calls.
     *
     * @return total calls
     */
    int getTotalCalls();

    /**
     * Gets unique functions called.
     *
     * @return unique function count
     */
    int getUniqueFunctions();

    /**
     * Gets call frequency by function.
     *
     * @return function call frequencies
     */
    java.util.Map<String, Integer> getCallFrequencies();

    /**
     * Gets average call duration by function.
     *
     * @return function call durations
     */
    java.util.Map<String, Double> getAverageCallDurations();
  }

  /** Stack frame interface. */
  interface StackFrame {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the module name.
     *
     * @return module name
     */
    String getModuleName();

    /**
     * Gets the instruction offset.
     *
     * @return instruction offset
     */
    int getInstructionOffset();

    /**
     * Gets the frame depth.
     *
     * @return frame depth
     */
    int getDepth();
  }

  /** Event type enumeration. */
  enum EventType {
    /** Function entry. */
    FUNCTION_ENTRY,
    /** Function exit. */
    FUNCTION_EXIT,
    /** Memory allocation. */
    MEMORY_ALLOCATION,
    /** Memory deallocation. */
    MEMORY_DEALLOCATION,
    /** Instruction execution. */
    INSTRUCTION_EXECUTION,
    /** Exception thrown. */
    EXCEPTION_THROWN,
    /** Exception handled. */
    EXCEPTION_HANDLED,
    /** Custom event. */
    CUSTOM
  }

  /** Export format enumeration. */
  enum ExportFormat {
    /** JSON format. */
    JSON,
    /** Binary format. */
    BINARY,
    /** Chrome trace format. */
    CHROME_TRACE,
    /** Flame graph format. */
    FLAME_GRAPH
  }

  /** Sampling strategy enumeration. */
  enum SamplingStrategy {
    /** Random sampling. */
    RANDOM,
    /** Systematic sampling. */
    SYSTEMATIC,
    /** Stratified sampling. */
    STRATIFIED,
    /** Adaptive sampling. */
    ADAPTIVE
  }
}
