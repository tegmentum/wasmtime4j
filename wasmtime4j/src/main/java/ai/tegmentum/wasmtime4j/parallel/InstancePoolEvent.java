package ai.tegmentum.wasmtime4j.parallel;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Event representing a significant occurrence in an instance pool.
 *
 * <p>Events include instance creation, destruction, health changes,
 * scaling operations, and other pool lifecycle events.
 *
 * @since 1.0.0
 */
public final class InstancePoolEvent {

  private final EventType eventType;
  private final String poolId;
  private final Optional<String> instanceId;
  private final Instant timestamp;
  private final String description;
  private final Map<String, Object> eventData;
  private final Optional<Throwable> relatedError;

  private InstancePoolEvent(final EventType eventType,
                            final String poolId,
                            final String instanceId,
                            final Instant timestamp,
                            final String description,
                            final Map<String, Object> eventData,
                            final Throwable relatedError) {
    this.eventType = Objects.requireNonNull(eventType);
    this.poolId = Objects.requireNonNull(poolId);
    this.instanceId = Optional.ofNullable(instanceId);
    this.timestamp = Objects.requireNonNull(timestamp);
    this.description = Objects.requireNonNull(description);
    this.eventData = Map.copyOf(eventData);
    this.relatedError = Optional.ofNullable(relatedError);
  }

  /**
   * Creates a new instance pool event.
   *
   * @param eventType the type of event
   * @param poolId the pool identifier
   * @param instanceId the instance identifier (optional)
   * @param description event description
   * @return new event
   */
  public static InstancePoolEvent create(final EventType eventType,
                                         final String poolId,
                                         final String instanceId,
                                         final String description) {
    return new InstancePoolEvent(eventType, poolId, instanceId, Instant.now(),
                                 description, Map.of(), null);
  }

  /**
   * Creates a new instance pool event with data.
   *
   * @param eventType the type of event
   * @param poolId the pool identifier
   * @param instanceId the instance identifier (optional)
   * @param description event description
   * @param eventData additional event data
   * @return new event
   */
  public static InstancePoolEvent create(final EventType eventType,
                                         final String poolId,
                                         final String instanceId,
                                         final String description,
                                         final Map<String, Object> eventData) {
    return new InstancePoolEvent(eventType, poolId, instanceId, Instant.now(),
                                 description, eventData, null);
  }

  /**
   * Creates a new instance pool event with error.
   *
   * @param eventType the type of event
   * @param poolId the pool identifier
   * @param instanceId the instance identifier (optional)
   * @param description event description
   * @param error the related error
   * @return new event
   */
  public static InstancePoolEvent createError(final EventType eventType,
                                              final String poolId,
                                              final String instanceId,
                                              final String description,
                                              final Throwable error) {
    return new InstancePoolEvent(eventType, poolId, instanceId, Instant.now(),
                                 description, Map.of(), error);
  }

  /**
   * Gets the event type.
   *
   * @return event type
   */
  public EventType getEventType() {
    return eventType;
  }

  /**
   * Gets the pool identifier.
   *
   * @return pool ID
   */
  public String getPoolId() {
    return poolId;
  }

  /**
   * Gets the instance identifier.
   *
   * @return instance ID, or empty if not instance-specific
   */
  public Optional<String> getInstanceId() {
    return instanceId;
  }

  /**
   * Gets the event timestamp.
   *
   * @return timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the event description.
   *
   * @return description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets additional event data.
   *
   * @return event data map
   */
  public Map<String, Object> getEventData() {
    return eventData;
  }

  /**
   * Gets any related error.
   *
   * @return related error, or empty if no error
   */
  public Optional<Throwable> getRelatedError() {
    return relatedError;
  }

  /**
   * Checks if this is an error event.
   *
   * @return true if this event represents an error
   */
  public boolean isErrorEvent() {
    return relatedError.isPresent() || eventType.isErrorType();
  }

  /**
   * Types of instance pool events.
   */
  public enum EventType {
    /** Pool was created */
    POOL_CREATED(false),

    /** Pool was destroyed */
    POOL_DESTROYED(false),

    /** Pool was started */
    POOL_STARTED(false),

    /** Pool was stopped */
    POOL_STOPPED(false),

    /** Pool scaled up */
    POOL_SCALED_UP(false),

    /** Pool scaled down */
    POOL_SCALED_DOWN(false),

    /** Instance was created */
    INSTANCE_CREATED(false),

    /** Instance was destroyed */
    INSTANCE_DESTROYED(false),

    /** Instance became healthy */
    INSTANCE_HEALTHY(false),

    /** Instance became unhealthy */
    INSTANCE_UNHEALTHY(true),

    /** Instance was acquired from pool */
    INSTANCE_ACQUIRED(false),

    /** Instance was returned to pool */
    INSTANCE_RETURNED(false),

    /** Health check was performed */
    HEALTH_CHECK_PERFORMED(false),

    /** Cleanup was performed */
    CLEANUP_PERFORMED(false),

    /** Configuration was changed */
    CONFIGURATION_CHANGED(false),

    /** Error occurred in pool operation */
    POOL_ERROR(true),

    /** Error occurred in instance operation */
    INSTANCE_ERROR(true);

    private final boolean isErrorType;

    EventType(final boolean isErrorType) {
      this.isErrorType = isErrorType;
    }

    /**
     * Checks if this event type represents an error.
     *
     * @return true if error type
     */
    public boolean isErrorType() {
      return isErrorType;
    }
  }

  @Override
  public String toString() {
    final String instanceInfo = instanceId.map(id -> ", instance=" + id).orElse("");
    return String.format("InstancePoolEvent{type=%s, pool=%s%s, description='%s'}",
        eventType, poolId, instanceInfo, description);
  }
}