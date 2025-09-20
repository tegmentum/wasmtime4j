package ai.tegmentum.wasmtime4j.async.reactive;

/**
 * Enumeration of WebAssembly engine event types.
 *
 * <p>This enum represents the various types of events that can occur during the lifecycle of a
 * WebAssembly engine, including startup, shutdown, resource management, and error conditions.
 *
 * <p>These event types are used in reactive engine events to categorize and filter different kinds
 * of engine activities.
 *
 * @since 1.0.0
 */
public enum EngineEventType {

  // Lifecycle events
  /** Engine is starting up and initializing. */
  ENGINE_STARTUP("Engine Startup", "Engine initialization and startup", EventCategory.LIFECYCLE),

  /** Engine is shutting down and cleaning up resources. */
  ENGINE_SHUTDOWN("Engine Shutdown", "Engine shutdown and cleanup", EventCategory.LIFECYCLE),

  /** Engine configuration has been updated. */
  ENGINE_CONFIGURATION_CHANGED(
      "Configuration Changed", "Engine configuration updated", EventCategory.LIFECYCLE),

  // Resource management events
  /** Memory usage has changed significantly. */
  MEMORY_USAGE_CHANGED(
      "Memory Usage Changed", "Significant memory usage change", EventCategory.RESOURCE),

  /** Memory cleanup and garbage collection occurred. */
  MEMORY_CLEANUP("Memory Cleanup", "Memory cleanup and garbage collection", EventCategory.RESOURCE),

  /** Memory usage exceeded warning threshold. */
  MEMORY_WARNING(
      "Memory Warning", "Memory usage warning threshold exceeded", EventCategory.RESOURCE),

  /** Store was created. */
  STORE_CREATED("Store Created", "New WebAssembly store created", EventCategory.RESOURCE),

  /** Store was destroyed. */
  STORE_DESTROYED("Store Destroyed", "WebAssembly store destroyed", EventCategory.RESOURCE),

  /** Module was loaded. */
  MODULE_LOADED("Module Loaded", "WebAssembly module loaded", EventCategory.RESOURCE),

  /** Module was unloaded. */
  MODULE_UNLOADED("Module Unloaded", "WebAssembly module unloaded", EventCategory.RESOURCE),

  /** Instance was created. */
  INSTANCE_CREATED("Instance Created", "WebAssembly instance created", EventCategory.RESOURCE),

  /** Instance was destroyed. */
  INSTANCE_DESTROYED(
      "Instance Destroyed", "WebAssembly instance destroyed", EventCategory.RESOURCE),

  // Performance events
  /** Performance metrics update. */
  PERFORMANCE_METRICS(
      "Performance Metrics", "Engine performance metrics update", EventCategory.PERFORMANCE),

  /** Performance warning threshold exceeded. */
  PERFORMANCE_WARNING(
      "Performance Warning", "Performance warning threshold exceeded", EventCategory.PERFORMANCE),

  /** Compilation cache statistics updated. */
  COMPILATION_CACHE_STATS(
      "Compilation Cache Stats", "Compilation cache statistics", EventCategory.PERFORMANCE),

  // Error and warning events
  /** Generic engine error occurred. */
  ENGINE_ERROR("Engine Error", "Generic engine error condition", EventCategory.ERROR),

  /** Engine warning condition detected. */
  ENGINE_WARNING("Engine Warning", "Engine warning condition", EventCategory.WARNING),

  /** Resource leak detected. */
  RESOURCE_LEAK_DETECTED(
      "Resource Leak Detected", "Potential resource leak detected", EventCategory.WARNING),

  /** Unusual activity pattern detected. */
  UNUSUAL_ACTIVITY("Unusual Activity", "Unusual engine activity pattern", EventCategory.WARNING),

  // Security events
  /** Security validation failure. */
  SECURITY_VALIDATION_FAILURE(
      "Security Validation Failure", "Security validation failed", EventCategory.SECURITY),

  /** Resource limit exceeded. */
  RESOURCE_LIMIT_EXCEEDED(
      "Resource Limit Exceeded", "Resource limit exceeded", EventCategory.SECURITY),

  /** Untrusted operation attempted. */
  UNTRUSTED_OPERATION(
      "Untrusted Operation", "Untrusted operation attempted", EventCategory.SECURITY),

  // Operational events
  /** Background maintenance task started. */
  MAINTENANCE_STARTED(
      "Maintenance Started", "Background maintenance task started", EventCategory.OPERATIONAL),

  /** Background maintenance task completed. */
  MAINTENANCE_COMPLETED(
      "Maintenance Completed", "Background maintenance task completed", EventCategory.OPERATIONAL),

  /** Engine statistics reset. */
  STATISTICS_RESET(
      "Statistics Reset", "Engine statistics counters reset", EventCategory.OPERATIONAL),

  /** Configuration reload completed. */
  CONFIGURATION_RELOAD(
      "Configuration Reload", "Engine configuration reloaded", EventCategory.OPERATIONAL);

  private final String displayName;
  private final String description;
  private final EventCategory category;

  EngineEventType(
      final String displayName, final String description, final EventCategory category) {
    this.displayName = displayName;
    this.description = description;
    this.category = category;
  }

  /**
   * Gets the human-readable display name for this event type.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the detailed description of this event type.
   *
   * @return the event type description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the category of this event type.
   *
   * @return the event category
   */
  public EventCategory getCategory() {
    return category;
  }

  /**
   * Checks if this event type represents an error condition.
   *
   * @return true if this is an error event type
   */
  public boolean isError() {
    return category == EventCategory.ERROR || category == EventCategory.SECURITY;
  }

  /**
   * Checks if this event type represents a warning condition.
   *
   * @return true if this is a warning event type
   */
  public boolean isWarning() {
    return category == EventCategory.WARNING;
  }

  /**
   * Checks if this event type is informational.
   *
   * @return true if this is an informational event type
   */
  public boolean isInformational() {
    return !isError() && !isWarning();
  }

  /**
   * Checks if this event type is related to resource management.
   *
   * @return true if this is a resource management event type
   */
  public boolean isResourceRelated() {
    return category == EventCategory.RESOURCE;
  }

  /**
   * Checks if this event type is related to performance.
   *
   * @return true if this is a performance-related event type
   */
  public boolean isPerformanceRelated() {
    return category == EventCategory.PERFORMANCE;
  }

  /**
   * Checks if this event type is related to security.
   *
   * @return true if this is a security-related event type
   */
  public boolean isSecurityRelated() {
    return category == EventCategory.SECURITY;
  }

  /**
   * Gets all event types in a specific category.
   *
   * @param category the event category
   * @return array of event types in the category
   */
  public static EngineEventType[] getEventsByCategory(final EventCategory category) {
    return java.util.Arrays.stream(values())
        .filter(type -> type.category == category)
        .toArray(EngineEventType[]::new);
  }

  /**
   * Gets all error event types.
   *
   * @return array of error event types
   */
  public static EngineEventType[] getErrorEvents() {
    return java.util.Arrays.stream(values())
        .filter(EngineEventType::isError)
        .toArray(EngineEventType[]::new);
  }

  /**
   * Gets all warning event types.
   *
   * @return array of warning event types
   */
  public static EngineEventType[] getWarningEvents() {
    return java.util.Arrays.stream(values())
        .filter(EngineEventType::isWarning)
        .toArray(EngineEventType[]::new);
  }

  /** Categories for grouping engine event types. */
  public enum EventCategory {
    /** Engine lifecycle events (startup, shutdown, configuration). */
    LIFECYCLE("Lifecycle"),

    /** Resource management events (memory, stores, modules, instances). */
    RESOURCE("Resource"),

    /** Performance and optimization events. */
    PERFORMANCE("Performance"),

    /** Error conditions and failures. */
    ERROR("Error"),

    /** Warning conditions that don't constitute errors. */
    WARNING("Warning"),

    /** Security-related events and validations. */
    SECURITY("Security"),

    /** Operational and maintenance events. */
    OPERATIONAL("Operational");

    private final String displayName;

    EventCategory(final String displayName) {
      this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this category.
     *
     * @return the display name
     */
    public String getDisplayName() {
      return displayName;
    }
  }
}
