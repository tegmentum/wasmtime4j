package ai.tegmentum.wasmtime4j;

/**
 * WIT interface migration plan interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface WitInterfaceMigrationPlan {

  /**
   * Gets the plan ID.
   *
   * @return plan ID
   */
  String getPlanId();

  /**
   * Gets the source interface version.
   *
   * @return source version
   */
  String getSourceVersion();

  /**
   * Gets the target interface version.
   *
   * @return target version
   */
  String getTargetVersion();

  /**
   * Gets migration steps.
   *
   * @return list of migration steps
   */
  java.util.List<MigrationStep> getSteps();

  /**
   * Gets migration strategy.
   *
   * @return migration strategy
   */
  MigrationStrategy getStrategy();

  /**
   * Gets estimated migration time.
   *
   * @return estimated time in milliseconds
   */
  long getEstimatedTime();

  /**
   * Gets migration risks.
   *
   * @return list of migration risks
   */
  java.util.List<MigrationRisk> getRisks();

  /**
   * Gets rollback plan.
   *
   * @return rollback plan
   */
  RollbackPlan getRollbackPlan();

  /**
   * Validates the migration plan.
   *
   * @return validation result
   */
  ValidationResult validate();

  /**
   * Executes the migration plan.
   *
   * @param context migration context
   * @return migration result
   */
  MigrationResult execute(MigrationContext context);

  /**
   * Gets migration prerequisites.
   *
   * @return list of prerequisites
   */
  java.util.List<Prerequisite> getPrerequisites();

  /** Migration step interface. */
  interface MigrationStep {
    /**
     * Gets the step ID.
     *
     * @return step ID
     */
    String getStepId();

    /**
     * Gets the step name.
     *
     * @return step name
     */
    String getName();

    /**
     * Gets the step description.
     *
     * @return description
     */
    String getDescription();

    /**
     * Gets the step type.
     *
     * @return step type
     */
    StepType getType();

    /**
     * Gets step parameters.
     *
     * @return parameter map
     */
    java.util.Map<String, Object> getParameters();

    /**
     * Gets step dependencies.
     *
     * @return list of dependency step IDs
     */
    java.util.List<String> getDependencies();

    /**
     * Gets estimated execution time.
     *
     * @return estimated time in milliseconds
     */
    long getEstimatedTime();

    /**
     * Checks if step is reversible.
     *
     * @return true if reversible
     */
    boolean isReversible();

    /**
     * Executes the step.
     *
     * @param context step context
     * @return step result
     */
    StepResult execute(StepContext context);
  }

  /** Migration risk interface. */
  interface MigrationRisk {
    /**
     * Gets the risk ID.
     *
     * @return risk ID
     */
    String getRiskId();

    /**
     * Gets the risk description.
     *
     * @return description
     */
    String getDescription();

    /**
     * Gets the risk probability.
     *
     * @return probability (0.0-1.0)
     */
    double getProbability();

    /**
     * Gets the risk impact.
     *
     * @return impact level
     */
    RiskImpact getImpact();

    /**
     * Gets risk mitigation strategies.
     *
     * @return list of mitigation strategies
     */
    java.util.List<String> getMitigationStrategies();
  }

  /** Rollback plan interface. */
  interface RollbackPlan {
    /**
     * Gets rollback steps.
     *
     * @return list of rollback steps
     */
    java.util.List<RollbackStep> getSteps();

    /**
     * Gets estimated rollback time.
     *
     * @return estimated time in milliseconds
     */
    long getEstimatedTime();

    /**
     * Checks if rollback is guaranteed.
     *
     * @return true if guaranteed
     */
    boolean isGuaranteed();

    /**
     * Executes the rollback plan.
     *
     * @param context rollback context
     * @return rollback result
     */
    RollbackResult execute(RollbackContext context);
  }

  /** Rollback step interface. */
  interface RollbackStep {
    /**
     * Gets the step ID.
     *
     * @return step ID
     */
    String getStepId();

    /**
     * Gets the step description.
     *
     * @return description
     */
    String getDescription();

    /**
     * Executes the rollback step.
     *
     * @param context rollback context
     * @return step result
     */
    StepResult execute(RollbackContext context);
  }

  /** Prerequisite interface. */
  interface Prerequisite {
    /**
     * Gets the prerequisite ID.
     *
     * @return prerequisite ID
     */
    String getId();

    /**
     * Gets the prerequisite description.
     *
     * @return description
     */
    String getDescription();

    /**
     * Gets the prerequisite type.
     *
     * @return prerequisite type
     */
    PrerequisiteType getType();

    /**
     * Checks if prerequisite is satisfied.
     *
     * @param context migration context
     * @return true if satisfied
     */
    boolean isSatisfied(MigrationContext context);
  }

  /** Migration context interface. */
  interface MigrationContext {
    /**
     * Gets the source component.
     *
     * @return source component
     */
    Component getSourceComponent();

    /**
     * Gets the target interface definition.
     *
     * @return target interface
     */
    WitInterfaceIntrospection getTargetInterface();

    /**
     * Gets migration configuration.
     *
     * @return configuration map
     */
    java.util.Map<String, Object> getConfiguration();

    /**
     * Gets migration listeners.
     *
     * @return list of listeners
     */
    java.util.List<MigrationListener> getListeners();
  }

  /** Step context interface. */
  interface StepContext {
    /**
     * Gets the migration context.
     *
     * @return migration context
     */
    MigrationContext getMigrationContext();

    /**
     * Gets step-specific data.
     *
     * @return step data
     */
    java.util.Map<String, Object> getStepData();

    /**
     * Gets previous step results.
     *
     * @return list of previous results
     */
    java.util.List<StepResult> getPreviousResults();
  }

  /** Rollback context interface. */
  interface RollbackContext {
    /**
     * Gets the original migration context.
     *
     * @return migration context
     */
    MigrationContext getMigrationContext();

    /**
     * Gets migration results to rollback.
     *
     * @return migration result
     */
    MigrationResult getMigrationResult();

    /**
     * Gets rollback reason.
     *
     * @return rollback reason
     */
    String getReason();
  }

  /** Migration result interface. */
  interface MigrationResult {
    /**
     * Checks if migration was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets migration errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets migration warnings.
     *
     * @return list of warnings
     */
    java.util.List<String> getWarnings();

    /**
     * Gets step results.
     *
     * @return list of step results
     */
    java.util.List<StepResult> getStepResults();

    /**
     * Gets migration duration.
     *
     * @return duration in milliseconds
     */
    long getDuration();

    /**
     * Gets the migrated component.
     *
     * @return migrated component, or null if failed
     */
    Component getMigratedComponent();
  }

  /** Step result interface. */
  interface StepResult {
    /**
     * Checks if step was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets step errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets step output data.
     *
     * @return output data
     */
    java.util.Map<String, Object> getOutputData();

    /**
     * Gets step execution time.
     *
     * @return execution time in milliseconds
     */
    long getExecutionTime();
  }

  /** Rollback result interface. */
  interface RollbackResult {
    /**
     * Checks if rollback was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets rollback errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets rollback duration.
     *
     * @return duration in milliseconds
     */
    long getDuration();
  }

  /** Validation result interface. */
  interface ValidationResult {
    /**
     * Checks if plan is valid.
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

  /** Migration listener interface. */
  interface MigrationListener {
    /**
     * Called when migration starts.
     *
     * @param context migration context
     */
    void onMigrationStart(MigrationContext context);

    /**
     * Called when a step completes.
     *
     * @param step completed step
     * @param result step result
     */
    void onStepComplete(MigrationStep step, StepResult result);

    /**
     * Called when migration completes.
     *
     * @param result migration result
     */
    void onMigrationComplete(MigrationResult result);

    /**
     * Called when migration fails.
     *
     * @param error failure reason
     */
    void onMigrationError(Throwable error);
  }

  /** Migration strategy enumeration. */
  enum MigrationStrategy {
    /** Big bang migration. */
    BIG_BANG,
    /** Phased migration. */
    PHASED,
    /** Blue-green migration. */
    BLUE_GREEN,
    /** Rolling migration. */
    ROLLING,
    /** Canary migration. */
    CANARY
  }

  /** Step type enumeration. */
  enum StepType {
    /** Validation step. */
    VALIDATION,
    /** Backup step. */
    BACKUP,
    /** Code transformation. */
    TRANSFORMATION,
    /** Data migration. */
    DATA_MIGRATION,
    /** Configuration update. */
    CONFIGURATION,
    /** Testing step. */
    TESTING,
    /** Deployment step. */
    DEPLOYMENT,
    /** Verification step. */
    VERIFICATION,
    /** Cleanup step. */
    CLEANUP
  }

  /** Risk impact enumeration. */
  enum RiskImpact {
    /** Low impact. */
    LOW,
    /** Medium impact. */
    MEDIUM,
    /** High impact. */
    HIGH,
    /** Critical impact. */
    CRITICAL
  }

  /** Prerequisite type enumeration. */
  enum PrerequisiteType {
    /** System requirement. */
    SYSTEM,
    /** Dependency requirement. */
    DEPENDENCY,
    /** Configuration requirement. */
    CONFIGURATION,
    /** Permission requirement. */
    PERMISSION,
    /** Resource requirement. */
    RESOURCE
  }
}
