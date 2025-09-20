package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a security event in the WebAssembly runtime.
 *
 * <p>Security events capture important security-related activities, violations, and state changes
 * for audit, monitoring, and analysis purposes.
 *
 * @since 1.0.0
 */
public final class SecurityEvent {

  private final String id;
  private final SecurityEventType type;
  private final SecuritySeverity severity;
  private final Instant timestamp;
  private final String message;
  private final Map<String, Object> context;
  private final Optional<String> sourceModule;
  private final Optional<String> sourceInstance;
  private final Optional<String> userId;
  private final Optional<String> sessionId;
  private final Optional<Throwable> exception;
  private final boolean acknowledged;
  private final Optional<String> remediationAction;

  private SecurityEvent(final Builder builder) {
    this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
    this.type = Objects.requireNonNull(builder.type, "Event type cannot be null");
    this.severity = builder.severity != null ? builder.severity : builder.type.getSeverity();
    this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    this.message = Objects.requireNonNull(builder.message, "Message cannot be null");
    this.context = Map.copyOf(builder.context);
    this.sourceModule = Optional.ofNullable(builder.sourceModule);
    this.sourceInstance = Optional.ofNullable(builder.sourceInstance);
    this.userId = Optional.ofNullable(builder.userId);
    this.sessionId = Optional.ofNullable(builder.sessionId);
    this.exception = Optional.ofNullable(builder.exception);
    this.acknowledged = builder.acknowledged;
    this.remediationAction = Optional.ofNullable(builder.remediationAction);
  }

  /**
   * Creates a new security event builder.
   *
   * @param type the event type
   * @param message the event message
   * @return new builder instance
   */
  public static Builder builder(final SecurityEventType type, final String message) {
    return new Builder(type, message);
  }

  /**
   * Creates a simple security event with default values.
   *
   * @param type the event type
   * @param message the event message
   * @return new security event
   */
  public static SecurityEvent of(final SecurityEventType type, final String message) {
    return builder(type, message).build();
  }

  /**
   * Creates a security event with context.
   *
   * @param type the event type
   * @param message the event message
   * @param context additional context information
   * @return new security event
   */
  public static SecurityEvent of(
      final SecurityEventType type, final String message, final Map<String, Object> context) {
    return builder(type, message).withContext(context).build();
  }

  /**
   * Creates a security violation event.
   *
   * @param message the violation message
   * @param context additional context information
   * @return new security violation event
   */
  public static SecurityEvent violation(final String message, final Map<String, Object> context) {
    return builder(SecurityEventType.SECURITY_POLICY_VIOLATION, message)
        .withSeverity(SecuritySeverity.HIGH)
        .withContext(context)
        .build();
  }

  /**
   * Creates a critical security event.
   *
   * @param type the event type
   * @param message the event message
   * @param context additional context information
   * @return new critical security event
   */
  public static SecurityEvent critical(
      final SecurityEventType type, final String message, final Map<String, Object> context) {
    return builder(type, message)
        .withSeverity(SecuritySeverity.CRITICAL)
        .withContext(context)
        .build();
  }

  // Getters
  public String getId() {
    return id;
  }

  public SecurityEventType getType() {
    return type;
  }

  public SecuritySeverity getSeverity() {
    return severity;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, Object> getContext() {
    return context;
  }

  public Optional<String> getSourceModule() {
    return sourceModule;
  }

  public Optional<String> getSourceInstance() {
    return sourceInstance;
  }

  public Optional<String> getUserId() {
    return userId;
  }

  public Optional<String> getSessionId() {
    return sessionId;
  }

  public Optional<Throwable> getException() {
    return exception;
  }

  public boolean isAcknowledged() {
    return acknowledged;
  }

  public Optional<String> getRemediationAction() {
    return remediationAction;
  }

  /**
   * Gets a context value by key.
   *
   * @param key the context key
   * @return optional context value
   */
  public Optional<Object> getContextValue(final String key) {
    return Optional.ofNullable(context.get(key));
  }

  /**
   * Gets a typed context value by key.
   *
   * @param key the context key
   * @param type the expected value type
   * @param <T> the value type
   * @return optional typed context value
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getContextValue(final String key, final Class<T> type) {
    final Object value = context.get(key);
    if (value != null && type.isAssignableFrom(value.getClass())) {
      return Optional.of((T) value);
    }
    return Optional.empty();
  }

  /**
   * Checks if this event is a security violation.
   *
   * @return true if this is a security violation event
   */
  public boolean isSecurityViolation() {
    return type.isSecurityViolation();
  }

  /**
   * Checks if this event requires immediate attention.
   *
   * @return true if this event requires immediate attention
   */
  public boolean requiresImmediateAttention() {
    return severity.requiresImmediateAttention();
  }

  /**
   * Checks if this event should trigger alerts.
   *
   * @return true if this event should trigger alerts
   */
  public boolean shouldTriggerAlert() {
    return severity.shouldTriggerAlert();
  }

  /**
   * Creates a new event with acknowledgment status changed.
   *
   * @param acknowledged the new acknowledgment status
   * @return new event with updated acknowledgment status
   */
  public SecurityEvent withAcknowledged(final boolean acknowledged) {
    return new Builder(this).withAcknowledged(acknowledged).build();
  }

  /**
   * Creates a new event with remediation action set.
   *
   * @param remediationAction the remediation action taken
   * @return new event with remediation action
   */
  public SecurityEvent withRemediationAction(final String remediationAction) {
    return new Builder(this).withRemediationAction(remediationAction).build();
  }

  /**
   * Formats this event as a human-readable string.
   *
   * @return formatted event string
   */
  public String format() {
    final StringBuilder sb = new StringBuilder();
    sb.append(severity.format(type.getDescription()));
    sb.append(" - ").append(message);
    if (sourceModule.isPresent()) {
      sb.append(" [module: ").append(sourceModule.get()).append("]");
    }
    if (sourceInstance.isPresent()) {
      sb.append(" [instance: ").append(sourceInstance.get()).append("]");
    }
    return sb.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SecurityEvent that = (SecurityEvent) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "SecurityEvent{"
        + "id='"
        + id
        + '\''
        + ", type="
        + type
        + ", severity="
        + severity
        + ", timestamp="
        + timestamp
        + ", message='"
        + message
        + '\''
        + ", acknowledged="
        + acknowledged
        + '}';
  }

  /** Builder for SecurityEvent. */
  public static final class Builder {
    private String id;
    private SecurityEventType type;
    private SecuritySeverity severity;
    private Instant timestamp;
    private String message;
    private Map<String, Object> context = Map.of();
    private String sourceModule;
    private String sourceInstance;
    private String userId;
    private String sessionId;
    private Throwable exception;
    private boolean acknowledged = false;
    private String remediationAction;

    private Builder(final SecurityEventType type, final String message) {
      this.type = type;
      this.message = message;
    }

    private Builder(final SecurityEvent event) {
      this.id = event.id;
      this.type = event.type;
      this.severity = event.severity;
      this.timestamp = event.timestamp;
      this.message = event.message;
      this.context = event.context;
      this.sourceModule = event.sourceModule.orElse(null);
      this.sourceInstance = event.sourceInstance.orElse(null);
      this.userId = event.userId.orElse(null);
      this.sessionId = event.sessionId.orElse(null);
      this.exception = event.exception.orElse(null);
      this.acknowledged = event.acknowledged;
      this.remediationAction = event.remediationAction.orElse(null);
    }

    public Builder withId(final String id) {
      this.id = id;
      return this;
    }

    public Builder withSeverity(final SecuritySeverity severity) {
      this.severity = severity;
      return this;
    }

    public Builder withTimestamp(final Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder withContext(final Map<String, Object> context) {
      this.context = context != null ? context : Map.of();
      return this;
    }

    public Builder withContextValue(final String key, final Object value) {
      final Map<String, Object> newContext = new java.util.HashMap<>(this.context);
      newContext.put(key, value);
      this.context = newContext;
      return this;
    }

    public Builder withSourceModule(final String sourceModule) {
      this.sourceModule = sourceModule;
      return this;
    }

    public Builder withSourceInstance(final String sourceInstance) {
      this.sourceInstance = sourceInstance;
      return this;
    }

    public Builder withUserId(final String userId) {
      this.userId = userId;
      return this;
    }

    public Builder withSessionId(final String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder withException(final Throwable exception) {
      this.exception = exception;
      return this;
    }

    public Builder withAcknowledged(final boolean acknowledged) {
      this.acknowledged = acknowledged;
      return this;
    }

    public Builder withRemediationAction(final String remediationAction) {
      this.remediationAction = remediationAction;
      return this;
    }

    public SecurityEvent build() {
      return new SecurityEvent(this);
    }
  }
}
