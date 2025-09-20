package ai.tegmentum.wasmtime4j.performance.events;

/**
 * Listener interface for receiving performance events.
 *
 * <p>Implementations of this interface can be registered with performance profilers to receive
 * real-time notifications about performance conditions and issues.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * PerformanceListener listener = event -> {
 *   if (event.isCritical()) {
 *     logger.error("Critical performance issue: " + event.getMessage());
 *     // Take corrective action
 *   } else if (event.isWarning()) {
 *     logger.warn("Performance warning: " + event.getMessage());
 *   }
 * };
 *
 * profiler.addPerformanceListener(listener);
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface PerformanceListener {

  /**
   * Called when a performance event occurs.
   *
   * <p>This method should execute quickly to avoid impacting performance monitoring. For expensive
   * operations, consider dispatching to a separate thread.
   *
   * @param event the performance event that occurred
   */
  void onPerformanceEvent(PerformanceEvent event);

  /**
   * Creates a filtering listener that only processes events of specific types.
   *
   * @param delegate the delegate listener to call for matching events
   * @param types the event types to process
   * @return filtered listener
   */
  static PerformanceListener filtering(
      final PerformanceListener delegate, final PerformanceEventType... types) {
    if (types.length == 0) {
      return delegate;
    }

    return event -> {
      for (final PerformanceEventType type : types) {
        if (event.getType() == type) {
          delegate.onPerformanceEvent(event);
          break;
        }
      }
    };
  }

  /**
   * Creates a severity filtering listener that only processes events above a minimum severity.
   *
   * @param delegate the delegate listener to call for matching events
   * @param minSeverity the minimum severity level (0.0 to 1.0)
   * @return severity-filtered listener
   */
  static PerformanceListener severityFiltering(
      final PerformanceListener delegate, final double minSeverity) {
    return event -> {
      if (event.getSeverity() >= minSeverity) {
        delegate.onPerformanceEvent(event);
      }
    };
  }

  /**
   * Creates a critical-only listener that only processes critical events.
   *
   * @param delegate the delegate listener to call for critical events
   * @return critical-only listener
   */
  static PerformanceListener criticalOnly(final PerformanceListener delegate) {
    return event -> {
      if (event.isCritical()) {
        delegate.onPerformanceEvent(event);
      }
    };
  }

  /**
   * Creates a throttling listener that limits the rate of event processing.
   *
   * <p>This is useful to prevent overwhelming the system with too many events.
   *
   * @param delegate the delegate listener
   * @param maxEventsPerSecond the maximum events per second to process
   * @return throttled listener
   */
  static PerformanceListener throttling(
      final PerformanceListener delegate, final double maxEventsPerSecond) {
    return new ThrottlingPerformanceListener(delegate, maxEventsPerSecond);
  }

  /**
   * Creates a logging listener that logs events to the provided logger.
   *
   * @param logger the logger to use
   * @return logging listener
   */
  static PerformanceListener logging(final java.util.logging.Logger logger) {
    return event -> {
      if (event.isCritical()) {
        logger.severe(event.toLogString());
      } else if (event.isWarning()) {
        logger.warning(event.toLogString());
      } else {
        logger.info(event.toLogString());
      }
    };
  }

  /**
   * Creates a composite listener that delegates to multiple listeners.
   *
   * @param listeners the listeners to delegate to
   * @return composite listener
   */
  static PerformanceListener composite(final PerformanceListener... listeners) {
    if (listeners.length == 0) {
      return event -> {
        /* no-op */
      };
    }
    if (listeners.length == 1) {
      return listeners[0];
    }

    return event -> {
      for (final PerformanceListener listener : listeners) {
        try {
          listener.onPerformanceEvent(event);
        } catch (final Exception e) {
          // Log error but continue with other listeners
          System.err.println("Error in performance listener: " + e.getMessage());
        }
      }
    };
  }
}

/** Implementation of throttling performance listener. */
final class ThrottlingPerformanceListener implements PerformanceListener {
  private final PerformanceListener delegate;
  private final long minIntervalNanos;
  private volatile long lastEventTime = 0;

  ThrottlingPerformanceListener(
      final PerformanceListener delegate, final double maxEventsPerSecond) {
    this.delegate = delegate;
    this.minIntervalNanos = (long) (1_000_000_000.0 / maxEventsPerSecond);
  }

  @Override
  public void onPerformanceEvent(final PerformanceEvent event) {
    final long now = System.nanoTime();
    final long last = lastEventTime;

    if (now - last >= minIntervalNanos) {
      if (lastEventTime == last) { // CAS-like check
        lastEventTime = now;
        delegate.onPerformanceEvent(event);
      }
    }
  }
}
