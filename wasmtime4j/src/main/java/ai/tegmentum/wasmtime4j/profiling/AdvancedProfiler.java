package ai.tegmentum.wasmtime4j.profiling;

/**
 * Advanced profiler interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface AdvancedProfiler {

  /** Starts profiling. */
  void startProfiling();

  /** Stops profiling. */
  void stopProfiling();

  /**
   * Gets profiling statistics.
   *
   * @return profiling statistics as string
   */
  String getStatistics();

  /**
   * Checks if profiling is active.
   *
   * @return true if profiling is active
   */
  boolean isActive();

  /** Profiling sample interface. */
  interface ProfilingSample {
    /**
     * Gets the sample timestamp.
     *
     * @return timestamp in milliseconds
     */
    long getTimestamp();

    /**
     * Gets the sample value.
     *
     * @return sample value
     */
    double getValue();
  }

  /** Profiler configuration interface. */
  interface ProfilerConfiguration {
    /**
     * Gets the sampling interval in milliseconds.
     *
     * @return sampling interval
     */
    long getSamplingInterval();

    /**
     * Checks if profiling is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the profiler name.
     *
     * @return profiler name
     */
    String getProfilerName();
  }

  /** Profiling statistics interface. */
  interface ProfilingStatistics {
    /**
     * Gets the total number of samples.
     *
     * @return sample count
     */
    long getSampleCount();

    /**
     * Gets the profiling duration in milliseconds.
     *
     * @return duration
     */
    long getDuration();

    /**
     * Gets the average sample value.
     *
     * @return average value
     */
    double getAverageValue();
  }
}
