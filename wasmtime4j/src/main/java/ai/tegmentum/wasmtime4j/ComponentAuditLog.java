package ai.tegmentum.wasmtime4j;

/**
 * Component audit log interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentAuditLog {

  /**
   * Gets the component ID.
   *
   * @return component ID
   */
  String getComponentId();

  /**
   * Gets audit log entries.
   *
   * @return list of audit entries
   */
  java.util.List<AuditEntry> getEntries();

  /**
   * Adds an audit entry.
   *
   * @param entry audit entry
   */
  void addEntry(AuditEntry entry);

  /**
   * Gets audit entries by type.
   *
   * @param type entry type
   * @return list of entries
   */
  java.util.List<AuditEntry> getEntriesByType(AuditEntryType type);

  /**
   * Gets audit entries within time range.
   *
   * @param startTime start timestamp
   * @param endTime end timestamp
   * @return list of entries
   */
  java.util.List<AuditEntry> getEntriesInRange(long startTime, long endTime);

  /** Clears audit log. */
  void clear();

  /**
   * Gets the log size.
   *
   * @return number of entries
   */
  int size();

  /**
   * Checks if the log is empty.
   *
   * @return true if empty
   */
  boolean isEmpty();

  /**
   * Exports audit log.
   *
   * @param format export format
   * @return exported data
   */
  byte[] export(ExportFormat format);

  /** Audit entry interface. */
  interface AuditEntry {
    /**
     * Gets the entry ID.
     *
     * @return entry ID
     */
    String getId();

    /**
     * Gets the entry timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets the entry type.
     *
     * @return entry type
     */
    AuditEntryType getType();

    /**
     * Gets the entry message.
     *
     * @return message
     */
    String getMessage();

    /**
     * Gets the user/actor.
     *
     * @return user identifier
     */
    String getUser();

    /**
     * Gets the action performed.
     *
     * @return action
     */
    String getAction();

    /**
     * Gets the resource affected.
     *
     * @return resource identifier
     */
    String getResource();

    /**
     * Gets additional metadata.
     *
     * @return metadata map
     */
    java.util.Map<String, Object> getMetadata();

    /**
     * Gets the entry severity.
     *
     * @return severity level
     */
    SeverityLevel getSeverity();
  }

  /** Audit entry type enumeration. */
  enum AuditEntryType {
    /** Component creation. */
    COMPONENT_CREATED,
    /** Component instantiation. */
    COMPONENT_INSTANTIATED,
    /** Component execution. */
    COMPONENT_EXECUTED,
    /** Component modification. */
    COMPONENT_MODIFIED,
    /** Component deletion. */
    COMPONENT_DELETED,
    /** Security event. */
    SECURITY_EVENT,
    /** Resource access. */
    RESOURCE_ACCESS,
    /** Configuration change. */
    CONFIGURATION_CHANGE,
    /** Error event. */
    ERROR_EVENT,
    /** System event. */
    SYSTEM_EVENT
  }

  /** Severity level enumeration. */
  enum SeverityLevel {
    /** Information. */
    INFO,
    /** Warning. */
    WARNING,
    /** Error. */
    ERROR,
    /** Critical. */
    CRITICAL
  }

  /** Export format enumeration. */
  enum ExportFormat {
    /** JSON format. */
    JSON,
    /** CSV format. */
    CSV,
    /** XML format. */
    XML,
    /** Plain text. */
    TEXT
  }
}
