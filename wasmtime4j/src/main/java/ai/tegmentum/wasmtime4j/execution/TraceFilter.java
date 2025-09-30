package ai.tegmentum.wasmtime4j.execution;

/**
 * Trace filter interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface TraceFilter {

  /**
   * Gets the filter name.
   *
   * @return filter name
   */
  String getName();

  /**
   * Gets the filter type.
   *
   * @return filter type
   */
  FilterType getType();

  /**
   * Gets the filter pattern.
   *
   * @return filter pattern
   */
  String getPattern();

  /**
   * Sets the filter pattern.
   *
   * @param pattern filter pattern
   */
  void setPattern(String pattern);

  /**
   * Checks if the filter is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets the filter enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the filter action.
   *
   * @return filter action
   */
  FilterAction getAction();

  /**
   * Sets the filter action.
   *
   * @param action filter action
   */
  void setAction(FilterAction action);

  /**
   * Tests if a trace event matches this filter.
   *
   * @param event trace event
   * @return true if matches
   */
  boolean matches(TraceEvent event);

  /**
   * Gets the filter priority.
   *
   * @return filter priority
   */
  int getPriority();

  /**
   * Sets the filter priority.
   *
   * @param priority filter priority
   */
  void setPriority(int priority);

  /**
   * Gets filter conditions.
   *
   * @return list of conditions
   */
  java.util.List<FilterCondition> getConditions();

  /**
   * Adds a filter condition.
   *
   * @param condition filter condition
   */
  void addCondition(FilterCondition condition);

  /**
   * Removes a filter condition.
   *
   * @param condition filter condition
   */
  void removeCondition(FilterCondition condition);

  /** Filter type enumeration. */
  enum FilterType {
    /** Function name filter. */
    FUNCTION_NAME,
    /** Module name filter. */
    MODULE_NAME,
    /** Event type filter. */
    EVENT_TYPE,
    /** Duration filter. */
    DURATION,
    /** Memory usage filter. */
    MEMORY_USAGE,
    /** Custom filter. */
    CUSTOM
  }

  /** Filter action enumeration. */
  enum FilterAction {
    /** Include matching events. */
    INCLUDE,
    /** Exclude matching events. */
    EXCLUDE,
    /** Mark matching events. */
    MARK,
    /** Transform matching events. */
    TRANSFORM
  }

  /** Filter condition interface. */
  interface FilterCondition {
    /**
     * Gets the condition field.
     *
     * @return field name
     */
    String getField();

    /**
     * Gets the condition operator.
     *
     * @return operator
     */
    ConditionOperator getOperator();

    /**
     * Gets the condition value.
     *
     * @return condition value
     */
    Object getValue();

    /**
     * Tests the condition against a trace event.
     *
     * @param event trace event
     * @return true if condition is met
     */
    boolean test(TraceEvent event);
  }

  /** Trace event interface. */
  interface TraceEvent {
    /**
     * Gets the event type.
     *
     * @return event type
     */
    String getType();

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
     * Gets event properties.
     *
     * @return properties map
     */
    java.util.Map<String, Object> getProperties();

    /**
     * Gets the event duration.
     *
     * @return duration in nanoseconds
     */
    long getDuration();
  }

  /** Condition operator enumeration. */
  enum ConditionOperator {
    /** Equals. */
    EQUALS,
    /** Not equals. */
    NOT_EQUALS,
    /** Greater than. */
    GREATER_THAN,
    /** Less than. */
    LESS_THAN,
    /** Greater than or equal. */
    GREATER_THAN_OR_EQUAL,
    /** Less than or equal. */
    LESS_THAN_OR_EQUAL,
    /** Contains. */
    CONTAINS,
    /** Starts with. */
    STARTS_WITH,
    /** Ends with. */
    ENDS_WITH,
    /** Matches regex. */
    MATCHES
  }
}
