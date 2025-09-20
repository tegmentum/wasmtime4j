package ai.tegmentum.wasmtime4j.async.reactive;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents an event in the WebAssembly engine lifecycle.
 *
 * <p>EngineEvent provides information about engine state changes, resource management, and
 * operational events. These events enable monitoring of engine health, performance, and resource
 * utilization in reactive applications.
 *
 * <p>Events are immutable and contain all relevant information about the engine state at the time
 * the event was created.
 *
 * @since 1.0.0
 */
public interface EngineEvent {

  /**
   * Gets the unique identifier for this event.
   *
   * @return the event identifier
   */
  String getEventId();

  /**
   * Gets the engine identifier.
   *
   * @return the engine identifier
   */
  String getEngineId();

  /**
   * Gets the event type.
   *
   * @return the event type
   */
  EngineEventType getEventType();

  /**
   * Gets the timestamp when this event was created.
   *
   * @return the event timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the event message describing what occurred.
   *
   * @return the event message
   */
  String getMessage();

  /**
   * Gets additional event details, if available.
   *
   * @return event details, or empty if not available
   */
  Optional<String> getDetails();

  /**
   * Gets the error associated with this event, if any.
   *
   * @return the error, or empty if no error occurred
   */
  Optional<Exception> getError();

  /**
   * Gets the current memory usage in bytes.
   *
   * @return memory usage, or -1 if not available
   */
  long getMemoryUsage();

  /**
   * Gets the number of active stores.
   *
   * @return active store count, or -1 if not available
   */
  int getActiveStores();

  /**
   * Gets the number of active modules.
   *
   * @return active module count, or -1 if not available
   */
  int getActiveModules();

  /**
   * Gets the number of active instances.
   *
   * @return active instance count, or -1 if not available
   */
  int getActiveInstances();

  /**
   * Gets the engine uptime.
   *
   * @return engine uptime, or null if not available
   */
  Optional<Duration> getUptime();

  /**
   * Checks if this event indicates an error condition.
   *
   * @return true if this is an error event
   */
  boolean isError();

  /**
   * Checks if this event indicates a warning condition.
   *
   * @return true if this is a warning event
   */
  boolean isWarning();

  /**
   * Checks if this event is informational.
   *
   * @return true if this is an informational event
   */
  boolean isInformational();

  // Factory methods for creating events

  /**
   * Creates an engine startup event.
   *
   * @param engineId the engine identifier
   * @param message the startup message
   * @return a new engine startup event
   */
  static EngineEvent engineStartup(final String engineId, final String message) {
    return new EngineEventImpl(
        generateEventId(),
        engineId,
        EngineEventType.ENGINE_STARTUP,
        Instant.now(),
        message,
        null,
        null,
        -1,
        -1,
        -1,
        -1,
        null,
        false,
        false,
        true);
  }

  /**
   * Creates an engine shutdown event.
   *
   * @param engineId the engine identifier
   * @param uptime the engine uptime
   * @return a new engine shutdown event
   */
  static EngineEvent engineShutdown(final String engineId, final Duration uptime) {
    return new EngineEventImpl(
        generateEventId(),
        engineId,
        EngineEventType.ENGINE_SHUTDOWN,
        Instant.now(),
        "Engine shutting down",
        null,
        null,
        -1,
        -1,
        -1,
        -1,
        uptime,
        false,
        false,
        true);
  }

  /**
   * Creates a resource management event.
   *
   * @param engineId the engine identifier
   * @param eventType the specific resource event type
   * @param message the event message
   * @param memoryUsage current memory usage
   * @param activeStores number of active stores
   * @param activeModules number of active modules
   * @param activeInstances number of active instances
   * @return a new resource management event
   */
  static EngineEvent resourceManagement(
      final String engineId,
      final EngineEventType eventType,
      final String message,
      final long memoryUsage,
      final int activeStores,
      final int activeModules,
      final int activeInstances) {
    return new EngineEventImpl(
        generateEventId(),
        engineId,
        eventType,
        Instant.now(),
        message,
        null,
        null,
        memoryUsage,
        activeStores,
        activeModules,
        activeInstances,
        null,
        false,
        false,
        true);
  }

  /**
   * Creates an error event.
   *
   * @param engineId the engine identifier
   * @param message the error message
   * @param error the error that occurred
   * @return a new error event
   */
  static EngineEvent engineError(
      final String engineId, final String message, final Exception error) {
    return new EngineEventImpl(
        generateEventId(),
        engineId,
        EngineEventType.ENGINE_ERROR,
        Instant.now(),
        message,
        null,
        error,
        -1,
        -1,
        -1,
        -1,
        null,
        true,
        false,
        false);
  }

  /**
   * Creates a warning event.
   *
   * @param engineId the engine identifier
   * @param message the warning message
   * @param details additional warning details
   * @return a new warning event
   */
  static EngineEvent engineWarning(
      final String engineId, final String message, final String details) {
    return new EngineEventImpl(
        generateEventId(),
        engineId,
        EngineEventType.ENGINE_WARNING,
        Instant.now(),
        message,
        details,
        null,
        -1,
        -1,
        -1,
        -1,
        null,
        false,
        true,
        false);
  }

  // Helper method to generate event IDs
  private static String generateEventId() {
    return "engine-event-" + System.nanoTime();
  }

  /** Default implementation of EngineEvent. */
  final class EngineEventImpl implements EngineEvent {
    private final String eventId;
    private final String engineId;
    private final EngineEventType eventType;
    private final Instant timestamp;
    private final String message;
    private final String details;
    private final Exception error;
    private final long memoryUsage;
    private final int activeStores;
    private final int activeModules;
    private final int activeInstances;
    private final Duration uptime;
    private final boolean isError;
    private final boolean isWarning;
    private final boolean isInformational;

    EngineEventImpl(
        final String eventId,
        final String engineId,
        final EngineEventType eventType,
        final Instant timestamp,
        final String message,
        final String details,
        final Exception error,
        final long memoryUsage,
        final int activeStores,
        final int activeModules,
        final int activeInstances,
        final Duration uptime,
        final boolean isError,
        final boolean isWarning,
        final boolean isInformational) {
      this.eventId = eventId;
      this.engineId = engineId;
      this.eventType = eventType;
      this.timestamp = timestamp;
      this.message = message;
      this.details = details;
      this.error = error;
      this.memoryUsage = memoryUsage;
      this.activeStores = activeStores;
      this.activeModules = activeModules;
      this.activeInstances = activeInstances;
      this.uptime = uptime;
      this.isError = isError;
      this.isWarning = isWarning;
      this.isInformational = isInformational;
    }

    @Override
    public String getEventId() {
      return eventId;
    }

    @Override
    public String getEngineId() {
      return engineId;
    }

    @Override
    public EngineEventType getEventType() {
      return eventType;
    }

    @Override
    public Instant getTimestamp() {
      return timestamp;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Optional<String> getDetails() {
      return Optional.ofNullable(details);
    }

    @Override
    public Optional<Exception> getError() {
      return Optional.ofNullable(error);
    }

    @Override
    public long getMemoryUsage() {
      return memoryUsage;
    }

    @Override
    public int getActiveStores() {
      return activeStores;
    }

    @Override
    public int getActiveModules() {
      return activeModules;
    }

    @Override
    public int getActiveInstances() {
      return activeInstances;
    }

    @Override
    public Optional<Duration> getUptime() {
      return Optional.ofNullable(uptime);
    }

    @Override
    public boolean isError() {
      return isError;
    }

    @Override
    public boolean isWarning() {
      return isWarning;
    }

    @Override
    public boolean isInformational() {
      return isInformational;
    }

    @Override
    public String toString() {
      return String.format(
          "EngineEvent{eventId='%s', engineId='%s', eventType=%s, message='%s', isError=%s,"
              + " isWarning=%s}",
          eventId, engineId, eventType, message, isError, isWarning);
    }
  }
}
