package ai.tegmentum.wasmtime4j.component;

/**
 * Component state transition configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentStateTransitionConfig {

  /**
   * Gets allowed state transitions.
   *
   * @return allowed transitions map
   */
  java.util.Map<ComponentState, java.util.Set<ComponentState>> getAllowedTransitions();

  /**
   * Gets transition validators.
   *
   * @return transition validators
   */
  java.util.List<TransitionValidator> getValidators();

  /**
   * Gets transition interceptors.
   *
   * @return transition interceptors
   */
  java.util.List<TransitionInterceptor> getInterceptors();

  /**
   * Gets transition timeout configuration.
   *
   * @return timeout configuration
   */
  TransitionTimeoutConfig getTimeoutConfig();

  /**
   * Gets retry policy for failed transitions.
   *
   * @return retry policy
   */
  TransitionRetryPolicy getRetryPolicy();

  /**
   * Gets rollback configuration.
   *
   * @return rollback configuration
   */
  TransitionRollbackConfig getRollbackConfig();

  /**
   * Gets audit configuration for transitions.
   *
   * @return audit configuration
   */
  TransitionAuditConfig getAuditConfig();

  /**
   * Checks if a transition is allowed.
   *
   * @param from source state
   * @param to target state
   * @return true if allowed
   */
  boolean isTransitionAllowed(ComponentState from, ComponentState to);

  /**
   * Gets transition guards.
   *
   * @param from source state
   * @param to target state
   * @return list of guards
   */
  java.util.List<TransitionGuard> getTransitionGuards(ComponentState from, ComponentState to);

  /**
   * Gets transition actions.
   *
   * @param from source state
   * @param to target state
   * @return list of actions
   */
  java.util.List<TransitionAction> getTransitionActions(ComponentState from, ComponentState to);

  /** Transition validator interface. */
  interface TransitionValidator {
    /**
     * Validates a state transition.
     *
     * @param context transition context
     * @return validation result
     */
    ValidationResult validate(TransitionContext context);

    /**
     * Gets validator name.
     *
     * @return validator name
     */
    String getName();

    /**
     * Gets validator priority.
     *
     * @return priority
     */
    int getPriority();
  }

  /** Transition interceptor interface. */
  interface TransitionInterceptor {
    /**
     * Called before transition.
     *
     * @param context transition context
     * @return true to continue, false to abort
     */
    boolean beforeTransition(TransitionContext context);

    /**
     * Called after successful transition.
     *
     * @param context transition context
     */
    void afterTransition(TransitionContext context);

    /**
     * Called on transition failure.
     *
     * @param context transition context
     * @param error transition error
     */
    void onTransitionFailure(TransitionContext context, Throwable error);

    /**
     * Gets interceptor name.
     *
     * @return interceptor name
     */
    String getName();
  }

  /** Transition guard interface. */
  interface TransitionGuard {
    /**
     * Evaluates guard condition.
     *
     * @param context transition context
     * @return true if guard passes
     */
    boolean evaluate(TransitionContext context);

    /**
     * Gets guard name.
     *
     * @return guard name
     */
    String getName();

    /**
     * Gets guard description.
     *
     * @return guard description
     */
    String getDescription();
  }

  /** Transition action interface. */
  interface TransitionAction {
    /**
     * Executes transition action.
     *
     * @param context transition context
     * @return action result
     */
    ActionResult execute(TransitionContext context);

    /**
     * Gets action name.
     *
     * @return action name
     */
    String getName();

    /**
     * Gets action type.
     *
     * @return action type
     */
    ActionType getType();

    /**
     * Checks if action is reversible.
     *
     * @return true if reversible
     */
    boolean isReversible();
  }

  /** Transition context interface. */
  interface TransitionContext {
    /**
     * Gets component ID.
     *
     * @return component ID
     */
    String getComponentId();

    /**
     * Gets source state.
     *
     * @return source state
     */
    ComponentState getFromState();

    /**
     * Gets target state.
     *
     * @return target state
     */
    ComponentState getToState();

    /**
     * Gets transition trigger.
     *
     * @return transition trigger
     */
    TransitionTrigger getTrigger();

    /**
     * Gets transition metadata.
     *
     * @return metadata map
     */
    java.util.Map<String, Object> getMetadata();

    /**
     * Gets user context.
     *
     * @return user context
     */
    String getUserContext();

    /**
     * Gets transition timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();
  }

  /** Transition timeout configuration interface. */
  interface TransitionTimeoutConfig {
    /**
     * Gets default timeout.
     *
     * @return default timeout in milliseconds
     */
    long getDefaultTimeout();

    /**
     * Gets timeout for specific transitions.
     *
     * @return timeout map
     */
    java.util.Map<String, Long> getTransitionTimeouts();

    /**
     * Gets timeout for transition.
     *
     * @param from source state
     * @param to target state
     * @return timeout in milliseconds
     */
    long getTimeout(ComponentState from, ComponentState to);
  }

  /** Transition retry policy interface. */
  interface TransitionRetryPolicy {
    /**
     * Gets maximum retry attempts.
     *
     * @return max attempts
     */
    int getMaxAttempts();

    /**
     * Gets retry delay.
     *
     * @return delay in milliseconds
     */
    long getRetryDelay();

    /**
     * Gets delay multiplier for exponential backoff.
     *
     * @return delay multiplier
     */
    double getDelayMultiplier();

    /**
     * Gets maximum delay.
     *
     * @return max delay in milliseconds
     */
    long getMaxDelay();

    /**
     * Gets retryable exceptions.
     *
     * @return retryable exception types
     */
    java.util.Set<Class<? extends Throwable>> getRetryableExceptions();
  }

  /** Transition rollback configuration interface. */
  interface TransitionRollbackConfig {
    /**
     * Checks if automatic rollback is enabled.
     *
     * @return true if enabled
     */
    boolean isAutoRollbackEnabled();

    /**
     * Gets rollback timeout.
     *
     * @return timeout in milliseconds
     */
    long getRollbackTimeout();

    /**
     * Gets rollback strategies.
     *
     * @return rollback strategies
     */
    java.util.List<RollbackStrategy> getStrategies();

    /**
     * Checks if rollback validation is enabled.
     *
     * @return true if enabled
     */
    boolean isValidationEnabled();
  }

  /** Transition audit configuration interface. */
  interface TransitionAuditConfig {
    /**
     * Checks if audit logging is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets audit level.
     *
     * @return audit level
     */
    AuditLevel getLevel();

    /**
     * Gets audit destinations.
     *
     * @return audit destinations
     */
    java.util.List<String> getDestinations();

    /**
     * Gets audit filters.
     *
     * @return audit filters
     */
    java.util.List<AuditFilter> getFilters();
  }

  /** Validation result interface. */
  interface ValidationResult {
    /**
     * Checks if validation passed.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Gets validation errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets validation warnings.
     *
     * @return list of warnings
     */
    java.util.List<String> getWarnings();
  }

  /** Action result interface. */
  interface ActionResult {
    /**
     * Checks if action succeeded.
     *
     * @return true if successful
     */
    boolean isSuccess();

    /**
     * Gets result data.
     *
     * @return result data
     */
    Object getData();

    /**
     * Gets error if action failed.
     *
     * @return error or null
     */
    Throwable getError();

    /**
     * Gets execution time.
     *
     * @return execution time in milliseconds
     */
    long getExecutionTime();
  }

  /** Rollback strategy interface. */
  interface RollbackStrategy {
    /**
     * Executes rollback.
     *
     * @param context rollback context
     * @return rollback result
     */
    RollbackResult rollback(RollbackContext context);

    /**
     * Gets strategy name.
     *
     * @return strategy name
     */
    String getName();

    /**
     * Checks if strategy supports the transition.
     *
     * @param from source state
     * @param to target state
     * @return true if supported
     */
    boolean supports(ComponentState from, ComponentState to);
  }

  /** Rollback context interface. */
  interface RollbackContext {
    /**
     * Gets original transition context.
     *
     * @return transition context
     */
    TransitionContext getTransitionContext();

    /**
     * Gets rollback reason.
     *
     * @return rollback reason
     */
    String getReason();

    /**
     * Gets failure information.
     *
     * @return failure info
     */
    FailureInfo getFailureInfo();
  }

  /** Rollback result interface. */
  interface RollbackResult {
    /**
     * Checks if rollback succeeded.
     *
     * @return true if successful
     */
    boolean isSuccess();

    /**
     * Gets rollback error if failed.
     *
     * @return error or null
     */
    Throwable getError();

    /**
     * Gets actions performed during rollback.
     *
     * @return list of actions
     */
    java.util.List<String> getActionsPerformed();
  }

  /** Failure information interface. */
  interface FailureInfo {
    /**
     * Gets failure timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets failure cause.
     *
     * @return failure cause
     */
    Throwable getCause();

    /**
     * Gets failure context.
     *
     * @return failure context
     */
    String getContext();
  }

  /** Audit filter interface. */
  interface AuditFilter {
    /**
     * Checks if transition should be audited.
     *
     * @param context transition context
     * @return true if should audit
     */
    boolean shouldAudit(TransitionContext context);

    /**
     * Gets filter name.
     *
     * @return filter name
     */
    String getName();
  }

  /** Component state enumeration. */
  enum ComponentState {
    /** Uninitialized state. */
    UNINITIALIZED,
    /** Initializing state. */
    INITIALIZING,
    /** Ready state. */
    READY,
    /** Starting state. */
    STARTING,
    /** Running state. */
    RUNNING,
    /** Pausing state. */
    PAUSING,
    /** Paused state. */
    PAUSED,
    /** Stopping state. */
    STOPPING,
    /** Stopped state. */
    STOPPED,
    /** Destroying state. */
    DESTROYING,
    /** Destroyed state. */
    DESTROYED,
    /** Error state. */
    ERROR,
    /** Recovering state. */
    RECOVERING
  }

  /** Transition trigger enumeration. */
  enum TransitionTrigger {
    /** User-initiated trigger. */
    USER,
    /** System-initiated trigger. */
    SYSTEM,
    /** Timer-based trigger. */
    TIMER,
    /** Event-based trigger. */
    EVENT,
    /** Error-based trigger. */
    ERROR,
    /** External trigger. */
    EXTERNAL
  }

  /** Action type enumeration. */
  enum ActionType {
    /** Pre-transition action. */
    PRE_TRANSITION,
    /** Post-transition action. */
    POST_TRANSITION,
    /** Cleanup action. */
    CLEANUP,
    /** Notification action. */
    NOTIFICATION,
    /** Validation action. */
    VALIDATION
  }

  /** Audit level enumeration. */
  enum AuditLevel {
    /** No auditing. */
    NONE,
    /** Error transitions only. */
    ERROR,
    /** Important transitions. */
    IMPORTANT,
    /** All transitions. */
    ALL,
    /** Debug level. */
    DEBUG
  }
}
