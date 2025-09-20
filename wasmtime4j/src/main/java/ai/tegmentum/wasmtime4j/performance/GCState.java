package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;

/**
 * Provides information about the current garbage collection state.
 *
 * <p>GC state affects performance characteristics and helps interpret
 * performance measurements in the context of memory management activity.
 *
 * @since 1.0.0
 */
public interface GCState {

  /**
   * Checks if garbage collection is currently active.
   *
   * @return true if GC is currently running
   */
  boolean isGCActive();

  /**
   * Gets the type of garbage collection currently active.
   *
   * @return GC type, or null if no GC is active
   */
  GCType getCurrentGCType();

  /**
   * Gets the timestamp of the last completed garbage collection.
   *
   * @return last GC completion timestamp, or null if no GC has occurred
   */
  Instant getLastGCTime();

  /**
   * Gets the duration of the last garbage collection.
   *
   * @return last GC duration, or null if no GC has occurred
   */
  Duration getLastGCDuration();

  /**
   * Gets the total number of garbage collections that have occurred.
   *
   * @return total GC count
   */
  long getTotalGCCount();

  /**
   * Gets the total time spent in garbage collection.
   *
   * @return cumulative GC time
   */
  Duration getTotalGCTime();

  /**
   * Gets the current garbage collection pressure.
   *
   * @return GC pressure level
   */
  GCPressure getGCPressure();

  /**
   * Gets the amount of memory freed by the last garbage collection.
   *
   * @return bytes freed, or -1 if not available
   */
  long getLastGCMemoryFreed();

  /**
   * Gets the heap usage before the last garbage collection.
   *
   * @return heap usage percentage before GC (0.0 to 1.0), or -1.0 if not available
   */
  double getHeapUsageBeforeLastGC();

  /**
   * Gets the heap usage after the last garbage collection.
   *
   * @return heap usage percentage after GC (0.0 to 1.0), or -1.0 if not available
   */
  double getHeapUsageAfterLastGC();

  /**
   * Gets the efficiency of the last garbage collection.
   *
   * @return GC efficiency percentage (0.0 to 100.0), or -1.0 if not available
   */
  double getLastGCEfficiency();
}