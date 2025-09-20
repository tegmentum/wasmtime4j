package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.performance.events.PerformanceListener;
import java.util.List;

/**
 * Advanced performance profiler for real-time WebAssembly performance monitoring.
 *
 * <p>The performance profiler provides comprehensive real-time monitoring capabilities including:
 *
 * <ul>
 *   <li>Continuous performance metric collection
 *   <li>Real-time event detection and notification
 *   <li>Profile snapshot capture and analysis
 *   <li>Configurable monitoring thresholds and intervals
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Create profiler with custom configuration
 * ProfilerConfig config = ProfilerConfig.builder()
 *     .samplingInterval(Duration.ofMillis(100))
 *     .enableAllMetrics()
 *     .maxSnapshots(100)
 *     .build();
 *
 * PerformanceProfiler profiler = PerformanceProfiler.create(engine, config);
 *
 * // Add event listener
 * profiler.addPerformanceListener(event -> {
 *   if (event.isCritical()) {
 *     System.err.println("Critical performance issue: " + event.getMessage());
 *   }
 * });
 *
 * // Start profiling
 * profiler.startProfiling();
 *
 * // ... run WebAssembly operations ...
 *
 * // Capture snapshot
 * ProfileSnapshot snapshot = profiler.captureSnapshot();
 * System.out.println("Performance score: " + snapshot.getPerformanceScore());
 *
 * // Stop profiling
 * profiler.stopProfiling();
 * profiler.close();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface PerformanceProfiler extends AutoCloseable {

  /**
   * Creates a performance profiler with default configuration.
   *
   * @param engine the engine to profile
   * @return new performance profiler
   * @throws IllegalArgumentException if engine is null
   */
  static PerformanceProfiler create(final ai.tegmentum.wasmtime4j.Engine engine) {
    throw new UnsupportedOperationException("Implementation must be provided by runtime factory");
  }

  /**
   * Creates a performance profiler with custom configuration.
   *
   * @param engine the engine to profile
   * @param config the profiler configuration
   * @return new performance profiler
   * @throws IllegalArgumentException if engine or config is null
   */
  static PerformanceProfiler create(
      final ai.tegmentum.wasmtime4j.Engine engine, final ProfilerConfig config) {
    throw new UnsupportedOperationException("Implementation must be provided by runtime factory");
  }

  /**
   * Starts performance profiling.
   *
   * <p>This begins continuous monitoring of performance metrics and event detection. Profiling
   * continues until {@link #stopProfiling()} is called.
   *
   * @throws IllegalStateException if profiling is already active
   */
  void startProfiling();

  /**
   * Stops performance profiling.
   *
   * <p>This ends continuous monitoring but preserves collected data and snapshots.
   *
   * @throws IllegalStateException if profiling is not active
   */
  void stopProfiling();

  /**
   * Temporarily pauses performance profiling.
   *
   * <p>Profiling can be resumed with {@link #resumeProfiling()}.
   *
   * @throws IllegalStateException if profiling is not active
   */
  void pauseProfiling();

  /**
   * Resumes paused performance profiling.
   *
   * @throws IllegalStateException if profiling is not paused
   */
  void resumeProfiling();

  /**
   * Checks if performance profiling is currently active.
   *
   * @return true if profiling is active
   */
  boolean isProfiling();

  /**
   * Checks if performance profiling is currently paused.
   *
   * @return true if profiling is paused
   */
  boolean isPaused();

  /**
   * Captures a performance snapshot at the current moment.
   *
   * <p>This provides a comprehensive view of current performance metrics, resource usage, and
   * function profiles.
   *
   * @return performance snapshot
   */
  ProfileSnapshot captureSnapshot();

  /**
   * Gets all captured performance snapshots.
   *
   * <p>Snapshots are automatically captured at regular intervals during profiling and can also be
   * manually captured with {@link #captureSnapshot()}.
   *
   * @return list of performance snapshots in chronological order
   */
  List<ProfileSnapshot> getSnapshots();

  /**
   * Clears all captured performance snapshots.
   *
   * <p>This frees memory used by historical snapshots but does not affect ongoing profiling.
   */
  void clearSnapshots();

  /**
   * Adds a performance event listener.
   *
   * <p>The listener will receive real-time notifications about performance events such as high
   * resource usage, slow operations, or detected bottlenecks.
   *
   * @param listener the listener to add
   * @throws IllegalArgumentException if listener is null
   */
  void addPerformanceListener(PerformanceListener listener);

  /**
   * Removes a performance event listener.
   *
   * @param listener the listener to remove
   * @return true if the listener was removed
   */
  boolean removePerformanceListener(PerformanceListener listener);

  /**
   * Gets the current profiler configuration.
   *
   * @return profiler configuration
   */
  ProfilerConfig getConfig();

  /**
   * Updates the profiler configuration.
   *
   * <p>Configuration changes take effect immediately. If profiling is active, some changes may
   * require stopping and restarting profiling.
   *
   * @param config the new configuration
   * @throws IllegalArgumentException if config is null
   */
  void updateConfig(ProfilerConfig config);

  /**
   * Gets real-time performance metrics.
   *
   * <p>This provides current performance metrics without creating a full snapshot. Useful for
   * lightweight monitoring displays.
   *
   * @return current performance metrics
   */
  PerformanceMetrics getCurrentMetrics();

  /**
   * Forces garbage collection and captures before/after metrics.
   *
   * <p>This is useful for measuring the impact of garbage collection on performance.
   *
   * @return GC impact metrics
   */
  GcImpactMetrics measureGcImpact();

  /**
   * Gets the profiler overhead as a percentage of total execution time.
   *
   * <p>Lower values indicate less impact on the profiled application.
   *
   * @return profiler overhead percentage (0.0 to 100.0)
   */
  double getProfilerOverhead();

  /**
   * Exports profiling data in a specific format.
   *
   * <p>Supported formats include JSON, CSV, and custom binary formats.
   *
   * @param format the export format
   * @return exported data as string
   * @throws IllegalArgumentException if format is not supported
   */
  String exportData(ExportFormat format);

  /**
   * Closes the profiler and releases all resources.
   *
   * <p>This stops profiling if active and cleans up all allocated resources. The profiler cannot be
   * used after closing.
   */
  @Override
  void close();
}
