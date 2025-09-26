/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for interface migration between WIT interface versions.
 *
 * <p>This configuration controls migration planning, execution, rollback capabilities,
 * and safety measures during interface evolution.
 *
 * @since 1.0.0
 */
public final class MigrationConfig {

  private final MigrationStrategy strategy;
  private final boolean enableRollback;
  private final boolean validateBeforeMigration;
  private final boolean enableBackup;
  private final Duration maxMigrationTime;
  private final Set<String> criticalFunctions;
  private final Map<String, String> migrationMetadata;
  private final boolean dryRunMode;
  private final int maxRetries;
  private final Duration retryDelay;
  private final boolean enableProgressTracking;
  private final boolean enableRiskAssessment;
  private final RiskToleranceLevel riskTolerance;
  private final Set<String> requiredValidations;
  private final boolean enableCheckpoints;
  private final Duration checkpointInterval;

  private MigrationConfig(Builder builder) {
    this.strategy = builder.strategy;
    this.enableRollback = builder.enableRollback;
    this.validateBeforeMigration = builder.validateBeforeMigration;
    this.enableBackup = builder.enableBackup;
    this.maxMigrationTime = builder.maxMigrationTime;
    this.criticalFunctions = Set.copyOf(builder.criticalFunctions);
    this.migrationMetadata = Map.copyOf(builder.migrationMetadata);
    this.dryRunMode = builder.dryRunMode;
    this.maxRetries = builder.maxRetries;
    this.retryDelay = builder.retryDelay;
    this.enableProgressTracking = builder.enableProgressTracking;
    this.enableRiskAssessment = builder.enableRiskAssessment;
    this.riskTolerance = builder.riskTolerance;
    this.requiredValidations = Set.copyOf(builder.requiredValidations);
    this.enableCheckpoints = builder.enableCheckpoints;
    this.checkpointInterval = builder.checkpointInterval;
  }

  /**
   * Gets the migration strategy.
   *
   * @return migration strategy
   */
  public MigrationStrategy getStrategy() {
    return strategy;
  }

  /**
   * Gets whether rollback is enabled.
   *
   * @return true if rollback is enabled
   */
  public boolean isEnableRollback() {
    return enableRollback;
  }

  /**
   * Gets whether pre-migration validation is enabled.
   *
   * @return true if validation is enabled
   */
  public boolean isValidateBeforeMigration() {
    return validateBeforeMigration;
  }

  /**
   * Gets whether backup is enabled.
   *
   * @return true if backup is enabled
   */
  public boolean isEnableBackup() {
    return enableBackup;
  }

  /**
   * Gets the maximum migration time allowed.
   *
   * @return maximum migration time
   */
  public Duration getMaxMigrationTime() {
    return maxMigrationTime;
  }

  /**
   * Gets the set of critical functions that require special handling.
   *
   * @return critical functions set
   */
  public Set<String> getCriticalFunctions() {
    return criticalFunctions;
  }

  /**
   * Gets the migration metadata.
   *
   * @return migration metadata map
   */
  public Map<String, String> getMigrationMetadata() {
    return migrationMetadata;
  }

  /**
   * Gets whether this is a dry run.
   *
   * @return true if dry run mode is enabled
   */
  public boolean isDryRunMode() {
    return dryRunMode;
  }

  /**
   * Gets the maximum number of retries.
   *
   * @return maximum retries
   */
  public int getMaxRetries() {
    return maxRetries;
  }

  /**
   * Gets the retry delay.
   *
   * @return retry delay
   */
  public Duration getRetryDelay() {
    return retryDelay;
  }

  /**
   * Gets whether progress tracking is enabled.
   *
   * @return true if progress tracking is enabled
   */
  public boolean isEnableProgressTracking() {
    return enableProgressTracking;
  }

  /**
   * Gets whether risk assessment is enabled.
   *
   * @return true if risk assessment is enabled
   */
  public boolean isEnableRiskAssessment() {
    return enableRiskAssessment;
  }

  /**
   * Gets the risk tolerance level.
   *
   * @return risk tolerance level
   */
  public RiskToleranceLevel getRiskTolerance() {
    return riskTolerance;
  }

  /**
   * Gets the required validations.
   *
   * @return required validations set
   */
  public Set<String> getRequiredValidations() {
    return requiredValidations;
  }

  /**
   * Gets whether checkpoints are enabled.
   *
   * @return true if checkpoints are enabled
   */
  public boolean isEnableCheckpoints() {
    return enableCheckpoints;
  }

  /**
   * Gets the checkpoint interval.
   *
   * @return checkpoint interval
   */
  public Duration getCheckpointInterval() {
    return checkpointInterval;
  }

  /**
   * Creates a new builder for MigrationConfig.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a safe default migration configuration.
   *
   * @return safe default configuration
   */
  public static MigrationConfig safeDefaults() {
    return new Builder()
        .strategy(MigrationStrategy.INCREMENTAL)
        .enableRollback(true)
        .validateBeforeMigration(true)
        .enableBackup(true)
        .enableRiskAssessment(true)
        .riskTolerance(RiskToleranceLevel.LOW)
        .build();
  }

  /**
   * Creates a fast migration configuration with fewer safety checks.
   *
   * @return fast configuration
   */
  public static MigrationConfig fastDefaults() {
    return new Builder()
        .strategy(MigrationStrategy.DIRECT)
        .validateBeforeMigration(false)
        .enableBackup(false)
        .enableRiskAssessment(false)
        .riskTolerance(RiskToleranceLevel.HIGH)
        .build();
  }

  /**
   * Migration strategy enum.
   */
  public enum MigrationStrategy {
    /** Direct migration in single step */
    DIRECT,
    /** Incremental migration with intermediate steps */
    INCREMENTAL,
    /** Blue-green style migration with parallel instances */
    BLUE_GREEN,
    /** Canary migration with gradual rollout */
    CANARY,
    /** Custom migration strategy */
    CUSTOM
  }

  /**
   * Risk tolerance level enum.
   */
  public enum RiskToleranceLevel {
    /** Very conservative with maximum safety checks */
    VERY_LOW,
    /** Conservative with standard safety checks */
    LOW,
    /** Balanced approach */
    MEDIUM,
    /** Aggressive with minimal safety checks */
    HIGH,
    /** Maximum speed with minimum safety */
    VERY_HIGH
  }

  /**
   * Builder for MigrationConfig.
   */
  public static final class Builder {
    private MigrationStrategy strategy = MigrationStrategy.INCREMENTAL;
    private boolean enableRollback = true;
    private boolean validateBeforeMigration = true;
    private boolean enableBackup = true;
    private Duration maxMigrationTime = Duration.ofHours(1);
    private Set<String> criticalFunctions = Set.of();
    private Map<String, String> migrationMetadata = Map.of();
    private boolean dryRunMode = false;
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(30);
    private boolean enableProgressTracking = true;
    private boolean enableRiskAssessment = true;
    private RiskToleranceLevel riskTolerance = RiskToleranceLevel.MEDIUM;
    private Set<String> requiredValidations = Set.of();
    private boolean enableCheckpoints = false;
    private Duration checkpointInterval = Duration.ofMinutes(10);

    /**
     * Sets the migration strategy.
     *
     * @param strategy the migration strategy
     * @return this builder
     */
    public Builder strategy(MigrationStrategy strategy) {
      this.strategy = strategy != null ? strategy : MigrationStrategy.INCREMENTAL;
      return this;
    }

    /**
     * Sets whether to enable rollback.
     *
     * @param enableRollback true to enable rollback
     * @return this builder
     */
    public Builder enableRollback(boolean enableRollback) {
      this.enableRollback = enableRollback;
      return this;
    }

    /**
     * Sets whether to validate before migration.
     *
     * @param validateBeforeMigration true to validate before migration
     * @return this builder
     */
    public Builder validateBeforeMigration(boolean validateBeforeMigration) {
      this.validateBeforeMigration = validateBeforeMigration;
      return this;
    }

    /**
     * Sets whether to enable backup.
     *
     * @param enableBackup true to enable backup
     * @return this builder
     */
    public Builder enableBackup(boolean enableBackup) {
      this.enableBackup = enableBackup;
      return this;
    }

    /**
     * Sets the maximum migration time.
     *
     * @param maxMigrationTime the maximum migration time
     * @return this builder
     */
    public Builder maxMigrationTime(Duration maxMigrationTime) {
      this.maxMigrationTime = maxMigrationTime != null ? maxMigrationTime : Duration.ofHours(1);
      return this;
    }

    /**
     * Sets the critical functions.
     *
     * @param criticalFunctions the critical functions
     * @return this builder
     */
    public Builder criticalFunctions(Set<String> criticalFunctions) {
      this.criticalFunctions = criticalFunctions != null ? Set.copyOf(criticalFunctions) : Set.of();
      return this;
    }

    /**
     * Sets the migration metadata.
     *
     * @param migrationMetadata the migration metadata
     * @return this builder
     */
    public Builder migrationMetadata(Map<String, String> migrationMetadata) {
      this.migrationMetadata = migrationMetadata != null ? Map.copyOf(migrationMetadata) : Map.of();
      return this;
    }

    /**
     * Sets whether to run in dry run mode.
     *
     * @param dryRunMode true for dry run mode
     * @return this builder
     */
    public Builder dryRunMode(boolean dryRunMode) {
      this.dryRunMode = dryRunMode;
      return this;
    }

    /**
     * Sets the maximum number of retries.
     *
     * @param maxRetries the maximum retries
     * @return this builder
     */
    public Builder maxRetries(int maxRetries) {
      this.maxRetries = Math.max(0, maxRetries);
      return this;
    }

    /**
     * Sets the retry delay.
     *
     * @param retryDelay the retry delay
     * @return this builder
     */
    public Builder retryDelay(Duration retryDelay) {
      this.retryDelay = retryDelay != null ? retryDelay : Duration.ofSeconds(30);
      return this;
    }

    /**
     * Sets whether to enable progress tracking.
     *
     * @param enableProgressTracking true to enable progress tracking
     * @return this builder
     */
    public Builder enableProgressTracking(boolean enableProgressTracking) {
      this.enableProgressTracking = enableProgressTracking;
      return this;
    }

    /**
     * Sets whether to enable risk assessment.
     *
     * @param enableRiskAssessment true to enable risk assessment
     * @return this builder
     */
    public Builder enableRiskAssessment(boolean enableRiskAssessment) {
      this.enableRiskAssessment = enableRiskAssessment;
      return this;
    }

    /**
     * Sets the risk tolerance level.
     *
     * @param riskTolerance the risk tolerance level
     * @return this builder
     */
    public Builder riskTolerance(RiskToleranceLevel riskTolerance) {
      this.riskTolerance = riskTolerance != null ? riskTolerance : RiskToleranceLevel.MEDIUM;
      return this;
    }

    /**
     * Sets the required validations.
     *
     * @param requiredValidations the required validations
     * @return this builder
     */
    public Builder requiredValidations(Set<String> requiredValidations) {
      this.requiredValidations = requiredValidations != null ? Set.copyOf(requiredValidations) : Set.of();
      return this;
    }

    /**
     * Sets whether to enable checkpoints.
     *
     * @param enableCheckpoints true to enable checkpoints
     * @return this builder
     */
    public Builder enableCheckpoints(boolean enableCheckpoints) {
      this.enableCheckpoints = enableCheckpoints;
      return this;
    }

    /**
     * Sets the checkpoint interval.
     *
     * @param checkpointInterval the checkpoint interval
     * @return this builder
     */
    public Builder checkpointInterval(Duration checkpointInterval) {
      this.checkpointInterval = checkpointInterval != null ? checkpointInterval : Duration.ofMinutes(10);
      return this;
    }

    /**
     * Builds the MigrationConfig.
     *
     * @return the configured MigrationConfig
     */
    public MigrationConfig build() {
      return new MigrationConfig(this);
    }
  }
}