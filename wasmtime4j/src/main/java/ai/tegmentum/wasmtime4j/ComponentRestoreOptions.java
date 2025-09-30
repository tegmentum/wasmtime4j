package ai.tegmentum.wasmtime4j;

/**
 * Component restore options interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentRestoreOptions {

  /**
   * Gets the restore mode.
   *
   * @return restore mode
   */
  RestoreMode getMode();

  /**
   * Sets the restore mode.
   *
   * @param mode restore mode
   */
  void setMode(RestoreMode mode);

  /**
   * Checks if existing component should be overwritten.
   *
   * @return true if overwrite is enabled
   */
  boolean isOverwriteEnabled();

  /**
   * Sets overwrite enabled state.
   *
   * @param enabled overwrite enabled state
   */
  void setOverwriteEnabled(boolean enabled);

  /**
   * Checks if backup verification should be performed before restore.
   *
   * @return true if verification is enabled
   */
  boolean isVerificationEnabled();

  /**
   * Sets verification enabled state.
   *
   * @param enabled verification enabled state
   */
  void setVerificationEnabled(boolean enabled);

  /**
   * Gets the target component ID.
   *
   * @return target component ID, or null to use original ID
   */
  String getTargetComponentId();

  /**
   * Sets the target component ID.
   *
   * @param componentId target component ID
   */
  void setTargetComponentId(String componentId);

  /**
   * Gets conflict resolution strategy.
   *
   * @return conflict resolution strategy
   */
  ConflictResolution getConflictResolution();

  /**
   * Sets conflict resolution strategy.
   *
   * @param strategy conflict resolution strategy
   */
  void setConflictResolution(ConflictResolution strategy);

  /**
   * Gets restore filters.
   *
   * @return set of restore filters
   */
  java.util.Set<RestoreFilter> getFilters();

  /**
   * Adds a restore filter.
   *
   * @param filter restore filter
   */
  void addFilter(RestoreFilter filter);

  /**
   * Removes a restore filter.
   *
   * @param filter restore filter
   */
  void removeFilter(RestoreFilter filter);

  /**
   * Gets custom restore parameters.
   *
   * @return parameter map
   */
  java.util.Map<String, Object> getParameters();

  /**
   * Sets a custom restore parameter.
   *
   * @param key parameter key
   * @param value parameter value
   */
  void setParameter(String key, Object value);

  /**
   * Gets the restore timeout.
   *
   * @return timeout in milliseconds
   */
  long getTimeout();

  /**
   * Sets the restore timeout.
   *
   * @param timeoutMs timeout in milliseconds
   */
  void setTimeout(long timeoutMs);

  /**
   * Gets post-restore actions.
   *
   * @return list of post-restore actions
   */
  java.util.List<PostRestoreAction> getPostRestoreActions();

  /**
   * Adds a post-restore action.
   *
   * @param action post-restore action
   */
  void addPostRestoreAction(PostRestoreAction action);

  /** Restore mode enumeration. */
  enum RestoreMode {
    /** Complete restore. */
    COMPLETE,
    /** Partial restore. */
    PARTIAL,
    /** State only restore. */
    STATE_ONLY,
    /** Configuration only restore. */
    CONFIG_ONLY,
    /** Custom restore. */
    CUSTOM
  }

  /** Conflict resolution enumeration. */
  enum ConflictResolution {
    /** Fail on conflict. */
    FAIL,
    /** Skip conflicting items. */
    SKIP,
    /** Overwrite existing. */
    OVERWRITE,
    /** Merge when possible. */
    MERGE,
    /** Ask for resolution. */
    INTERACTIVE
  }

  /** Restore filter enumeration. */
  enum RestoreFilter {
    /** Include component code. */
    INCLUDE_CODE,
    /** Include component state. */
    INCLUDE_STATE,
    /** Include configuration. */
    INCLUDE_CONFIG,
    /** Include metadata. */
    INCLUDE_METADATA,
    /** Include security settings. */
    INCLUDE_SECURITY,
    /** Include dependencies. */
    INCLUDE_DEPENDENCIES,
    /** Exclude temporary data. */
    EXCLUDE_TEMPORARY,
    /** Exclude debug info. */
    EXCLUDE_DEBUG
  }

  /** Post-restore action interface. */
  interface PostRestoreAction {
    /**
     * Gets the action name.
     *
     * @return action name
     */
    String getName();

    /**
     * Gets the action type.
     *
     * @return action type
     */
    ActionType getType();

    /**
     * Gets action parameters.
     *
     * @return parameter map
     */
    java.util.Map<String, Object> getParameters();

    /**
     * Checks if the action is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the action priority.
     *
     * @return priority (lower numbers execute first)
     */
    int getPriority();

    /**
     * Executes the action.
     *
     * @param context restore context
     * @return action result
     */
    ActionResult execute(RestoreContext context);
  }

  /** Restore context interface. */
  interface RestoreContext {
    /**
     * Gets the restored component.
     *
     * @return restored component
     */
    Component getRestoredComponent();

    /**
     * Gets the original backup.
     *
     * @return original backup
     */
    ComponentBackup getBackup();

    /**
     * Gets restore options.
     *
     * @return restore options
     */
    ComponentRestoreOptions getOptions();

    /**
     * Gets restore start time.
     *
     * @return start timestamp
     */
    long getStartTime();

    /**
     * Gets restore results so far.
     *
     * @return list of action results
     */
    java.util.List<ActionResult> getResults();
  }

  /** Action result interface. */
  interface ActionResult {
    /**
     * Checks if action was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets action errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets action warnings.
     *
     * @return list of warnings
     */
    java.util.List<String> getWarnings();

    /**
     * Gets action execution time.
     *
     * @return execution time in milliseconds
     */
    long getExecutionTime();

    /**
     * Gets action output data.
     *
     * @return output data
     */
    java.util.Map<String, Object> getOutputData();
  }

  /** Action type enumeration. */
  enum ActionType {
    /** Validation action. */
    VALIDATION,
    /** Initialization action. */
    INITIALIZATION,
    /** Configuration action. */
    CONFIGURATION,
    /** Verification action. */
    VERIFICATION,
    /** Cleanup action. */
    CLEANUP,
    /** Notification action. */
    NOTIFICATION,
    /** Custom action. */
    CUSTOM
  }
}
