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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * WIT interface evolution and versioning system.
 *
 * <p>This system manages the evolution of WIT interfaces over time, providing
 * backward and forward compatibility checking, interface adaptation, and migration support.
 * It enables safe interface changes while maintaining component interoperability.
 *
 * @since 1.0.0
 */
public interface WitInterfaceEvolution {

  /**
   * Analyzes the evolution path between two interface versions.
   *
   * @param fromVersion the source interface version
   * @param toVersion the target interface version
   * @return evolution analysis result
   * @throws WasmException if analysis fails
   */
  InterfaceEvolutionAnalysis analyzeEvolution(WitInterfaceVersion fromVersion, WitInterfaceVersion toVersion) throws WasmException;

  /**
   * Checks backward compatibility between interface versions.
   *
   * @param olderVersion the older interface version
   * @param newerVersion the newer interface version
   * @return backward compatibility result
   */
  BackwardCompatibilityResult checkBackwardCompatibility(WitInterfaceVersion olderVersion, WitInterfaceVersion newerVersion);

  /**
   * Checks forward compatibility between interface versions.
   *
   * @param newerVersion the newer interface version
   * @param olderVersion the older interface version
   * @return forward compatibility result
   */
  ForwardCompatibilityResult checkForwardCompatibility(WitInterfaceVersion newerVersion, WitInterfaceVersion olderVersion);

  /**
   * Creates an interface adapter for bridging between versions.
   *
   * @param sourceVersion the source interface version
   * @param targetVersion the target interface version
   * @param adaptationConfig adapter configuration
   * @return interface adapter
   * @throws WasmException if adapter creation fails
   */
  InterfaceAdapter createAdapter(WitInterfaceVersion sourceVersion, WitInterfaceVersion targetVersion,
                                 AdaptationConfig adaptationConfig) throws WasmException;

  /**
   * Validates an interface evolution strategy.
   *
   * @param strategy the evolution strategy to validate
   * @return validation result
   */
  EvolutionValidationResult validateEvolutionStrategy(InterfaceEvolutionStrategy strategy);

  /**
   * Creates an interface migration plan.
   *
   * @param currentInterface the current interface
   * @param targetInterface the target interface
   * @param migrationConfig migration configuration
   * @return interface migration plan
   * @throws WasmException if migration planning fails
   */
  InterfaceMigrationPlan createMigrationPlan(WitInterfaceDefinition currentInterface,
                                             WitInterfaceDefinition targetInterface,
                                             MigrationConfig migrationConfig) throws WasmException;

  /**
   * Executes an interface migration.
   *
   * @param migrationPlan the migration plan to execute
   * @return migration execution result
   * @throws WasmException if migration execution fails
   */
  MigrationExecutionResult executeMigration(InterfaceMigrationPlan migrationPlan) throws WasmException;

  /**
   * Gets the evolution history of an interface.
   *
   * @param interfaceName the interface name
   * @return evolution history
   */
  InterfaceEvolutionHistory getEvolutionHistory(String interfaceName);

  /**
   * Registers a new interface version.
   *
   * @param interfaceVersion the interface version to register
   * @throws WasmException if registration fails
   */
  void registerInterfaceVersion(WitInterfaceVersion interfaceVersion) throws WasmException;

  /**
   * Deprecates an interface version.
   *
   * @param interfaceVersion the interface version to deprecate
   * @param deprecationInfo deprecation information
   * @throws WasmException if deprecation fails
   */
  void deprecateInterfaceVersion(WitInterfaceVersion interfaceVersion, DeprecationInfo deprecationInfo) throws WasmException;

  /**
   * Gets all versions of an interface.
   *
   * @param interfaceName the interface name
   * @return list of interface versions
   */
  List<WitInterfaceVersion> getInterfaceVersions(String interfaceName);

  /**
   * Finds the best compatible interface version.
   *
   * @param interfaceName the interface name
   * @param requirements compatibility requirements
   * @return best compatible version, if found
   */
  Optional<WitInterfaceVersion> findCompatibleVersion(String interfaceName, CompatibilityRequirements requirements);

  /**
   * Interface evolution analysis result.
   */
  interface InterfaceEvolutionAnalysis {
    /**
     * Gets the source interface version.
     *
     * @return source version
     */
    WitInterfaceVersion getSourceVersion();

    /**
     * Gets the target interface version.
     *
     * @return target version
     */
    WitInterfaceVersion getTargetVersion();

    /**
     * Gets the evolution type.
     *
     * @return evolution type
     */
    EvolutionType getEvolutionType();

    /**
     * Gets breaking changes between versions.
     *
     * @return list of breaking changes
     */
    List<BreakingChange> getBreakingChanges();

    /**
     * Gets non-breaking changes between versions.
     *
     * @return list of non-breaking changes
     */
    List<NonBreakingChange> getNonBreakingChanges();

    /**
     * Gets required adaptations for compatibility.
     *
     * @return list of required adaptations
     */
    List<RequiredAdaptation> getRequiredAdaptations();

    /**
     * Gets the migration complexity level.
     *
     * @return migration complexity
     */
    MigrationComplexity getMigrationComplexity();

    /**
     * Gets estimated migration effort.
     *
     * @return migration effort estimate
     */
    MigrationEffort getEstimatedEffort();
  }

  /**
   * Backward compatibility analysis result.
   */
  interface BackwardCompatibilityResult {
    /**
     * Checks if backward compatibility is maintained.
     *
     * @return true if backward compatible
     */
    boolean isBackwardCompatible();

    /**
     * Gets backward compatibility issues.
     *
     * @return list of compatibility issues
     */
    List<CompatibilityIssue> getIssues();

    /**
     * Gets the compatibility level.
     *
     * @return compatibility level
     */
    CompatibilityLevel getCompatibilityLevel();

    /**
     * Gets suggestions for maintaining compatibility.
     *
     * @return list of suggestions
     */
    List<String> getSuggestions();
  }

  /**
   * Forward compatibility analysis result.
   */
  interface ForwardCompatibilityResult {
    /**
     * Checks if forward compatibility is maintained.
     *
     * @return true if forward compatible
     */
    boolean isForwardCompatible();

    /**
     * Gets forward compatibility issues.
     *
     * @return list of compatibility issues
     */
    List<CompatibilityIssue> getIssues();

    /**
     * Gets the compatibility level.
     *
     * @return compatibility level
     */
    CompatibilityLevel getCompatibilityLevel();

    /**
     * Gets the risk assessment for forward compatibility.
     *
     * @return risk assessment
     */
    RiskAssessment getRiskAssessment();
  }

  /**
   * Interface adapter for version bridging.
   */
  interface InterfaceAdapter {
    /**
     * Gets the source interface version.
     *
     * @return source version
     */
    WitInterfaceVersion getSourceVersion();

    /**
     * Gets the target interface version.
     *
     * @return target version
     */
    WitInterfaceVersion getTargetVersion();

    /**
     * Adapts a call from source to target interface.
     *
     * @param functionName the function name
     * @param sourceArgs the source arguments
     * @return adapted arguments for target interface
     * @throws WasmException if adaptation fails
     */
    WasmValue[] adaptCall(String functionName, WasmValue[] sourceArgs) throws WasmException;

    /**
     * Adapts a return value from target to source interface.
     *
     * @param functionName the function name
     * @param targetResult the target return value
     * @return adapted return value for source interface
     * @throws WasmException if adaptation fails
     */
    WasmValue adaptReturn(String functionName, WasmValue targetResult) throws WasmException;

    /**
     * Gets adaptation statistics.
     *
     * @return adaptation statistics
     */
    AdaptationStatistics getStatistics();
  }

  /**
   * Interface migration plan.
   */
  interface InterfaceMigrationPlan {
    /**
     * Gets the migration identifier.
     *
     * @return migration ID
     */
    String getId();

    /**
     * Gets the source interface.
     *
     * @return source interface
     */
    WitInterfaceDefinition getSourceInterface();

    /**
     * Gets the target interface.
     *
     * @return target interface
     */
    WitInterfaceDefinition getTargetInterface();

    /**
     * Gets the migration steps.
     *
     * @return list of migration steps
     */
    List<MigrationStep> getSteps();

    /**
     * Gets the estimated migration time.
     *
     * @return estimated duration
     */
    java.time.Duration getEstimatedDuration();

    /**
     * Gets migration risks.
     *
     * @return list of identified risks
     */
    List<MigrationRisk> getRisks();

    /**
     * Gets rollback steps.
     *
     * @return list of rollback steps
     */
    List<MigrationStep> getRollbackSteps();
  }

  /**
   * Migration execution result.
   */
  interface MigrationExecutionResult {
    /**
     * Gets the migration plan that was executed.
     *
     * @return migration plan
     */
    InterfaceMigrationPlan getPlan();

    /**
     * Checks if migration was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets the execution start time.
     *
     * @return start time
     */
    Instant getStartTime();

    /**
     * Gets the execution end time.
     *
     * @return end time
     */
    Instant getEndTime();

    /**
     * Gets the actual migration duration.
     *
     * @return actual duration
     */
    java.time.Duration getActualDuration();

    /**
     * Gets completed migration steps.
     *
     * @return list of completed steps
     */
    List<MigrationStep> getCompletedSteps();

    /**
     * Gets failed migration steps.
     *
     * @return list of failed steps
     */
    List<MigrationStep> getFailedSteps();

    /**
     * Gets any error that occurred during migration.
     *
     * @return migration error, if any
     */
    Optional<Exception> getError();

    /**
     * Gets migration metrics.
     *
     * @return migration metrics
     */
    Map<String, Object> getMetrics();
  }

  /**
   * Interface evolution history.
   */
  interface InterfaceEvolutionHistory {
    /**
     * Gets the interface name.
     *
     * @return interface name
     */
    String getInterfaceName();

    /**
     * Gets all versions in chronological order.
     *
     * @return list of versions
     */
    List<WitInterfaceVersion> getVersionHistory();

    /**
     * Gets major version changes.
     *
     * @return list of major version changes
     */
    List<VersionChange> getMajorChanges();

    /**
     * Gets minor version changes.
     *
     * @return list of minor version changes
     */
    List<VersionChange> getMinorChanges();

    /**
     * Gets patch version changes.
     *
     * @return list of patch version changes
     */
    List<VersionChange> getPatchChanges();

    /**
     * Gets deprecation history.
     *
     * @return list of deprecation events
     */
    List<DeprecationEvent> getDeprecationHistory();

    /**
     * Gets migration history.
     *
     * @return list of completed migrations
     */
    List<CompletedMigration> getMigrationHistory();
  }

  /**
   * Types of interface evolution.
   */
  enum EvolutionType {
    /** Major version change with breaking changes */
    MAJOR,
    /** Minor version change with new features */
    MINOR,
    /** Patch version change with fixes only */
    PATCH,
    /** Custom evolution path */
    CUSTOM
  }

  /**
   * Migration complexity levels.
   */
  enum MigrationComplexity {
    /** Simple migration with no breaking changes */
    SIMPLE,
    /** Moderate migration with some adaptations needed */
    MODERATE,
    /** Complex migration with significant changes */
    COMPLEX,
    /** Critical migration requiring careful planning */
    CRITICAL
  }

  /**
   * Migration effort estimation.
   */
  enum MigrationEffort {
    /** Low effort migration */
    LOW,
    /** Medium effort migration */
    MEDIUM,
    /** High effort migration */
    HIGH,
    /** Very high effort migration */
    VERY_HIGH
  }

  /**
   * Compatibility levels.
   */
  enum CompatibilityLevel {
    /** Fully compatible */
    FULL,
    /** Mostly compatible with minor issues */
    PARTIAL,
    /** Limited compatibility requiring adaptations */
    LIMITED,
    /** No compatibility - breaking changes */
    NONE
  }

  /**
   * Breaking change information.
   */
  interface BreakingChange {
    String getDescription();
    ChangeType getType();
    String getLocation();
    ChangeImpact getImpact();
    List<String> getSuggestions();
  }

  /**
   * Non-breaking change information.
   */
  interface NonBreakingChange {
    String getDescription();
    ChangeType getType();
    String getLocation();
    ChangeCategory getCategory();
  }

  /**
   * Required adaptation information.
   */
  interface RequiredAdaptation {
    String getDescription();
    AdaptationType getType();
    String getFunctionName();
    AdaptationComplexity getComplexity();
    boolean isAutomatable();
  }

  /**
   * Compatibility issue information.
   */
  interface CompatibilityIssue {
    String getDescription();
    IssueType getType();
    IssueSeverity getSeverity();
    String getLocation();
    List<String> getResolutions();
  }

  /**
   * Migration step information.
   */
  interface MigrationStep {
    String getId();
    String getDescription();
    MigrationStepType getType();
    java.time.Duration getEstimatedDuration();
    List<String> getPreconditions();
    List<String> getActions();
    boolean isRollbackable();
  }

  /**
   * Migration risk information.
   */
  interface MigrationRisk {
    String getDescription();
    RiskLevel getLevel();
    RiskCategory getCategory();
    List<String> getMitigations();
    double getProbability();
    RiskImpact getImpact();
  }

  /**
   * Version change information.
   */
  interface VersionChange {
    WitInterfaceVersion getFromVersion();
    WitInterfaceVersion getToVersion();
    Instant getChangeTime();
    String getDescription();
    List<String> getChanges();
    ChangeType getChangeType();
  }

  /**
   * Deprecation information.
   */
  interface DeprecationInfo {
    Instant getDeprecationDate();
    Optional<Instant> getRemovalDate();
    String getReason();
    Optional<WitInterfaceVersion> getReplacementVersion();
    List<String> getMigrationGuidance();
  }

  /**
   * Deprecation event information.
   */
  interface DeprecationEvent {
    WitInterfaceVersion getVersion();
    DeprecationInfo getDeprecationInfo();
    Instant getEventTime();
  }

  /**
   * Completed migration information.
   */
  interface CompletedMigration {
    String getMigrationId();
    WitInterfaceVersion getFromVersion();
    WitInterfaceVersion getToVersion();
    Instant getStartTime();
    Instant getEndTime();
    boolean wasSuccessful();
    Optional<String> getErrorMessage();
  }

  /**
   * Risk assessment information.
   */
  interface RiskAssessment {
    RiskLevel getOverallRisk();
    List<String> getRiskFactors();
    List<String> getRecommendations();
    double getConfidenceLevel();
  }

  /**
   * Adaptation statistics.
   */
  interface AdaptationStatistics {
    long getTotalAdaptations();
    long getSuccessfulAdaptations();
    long getFailedAdaptations();
    double getAverageAdaptationTime();
    Map<String, Long> getAdaptationsByType();
  }

  // Enums for various categorizations
  enum ChangeType { ADDITION, REMOVAL, MODIFICATION, RENAME }
  enum ChangeImpact { LOW, MEDIUM, HIGH, CRITICAL }
  enum ChangeCategory { FEATURE, BUGFIX, IMPROVEMENT, REFACTORING }
  enum AdaptationType { TYPE_CONVERSION, PARAMETER_MAPPING, RETURN_TRANSFORMATION, INTERFACE_BRIDGING }
  enum AdaptationComplexity { SIMPLE, MODERATE, COMPLEX }
  enum IssueType { TYPE_MISMATCH, MISSING_FUNCTION, INCOMPATIBLE_SIGNATURE, VERSION_CONFLICT }
  enum IssueSeverity { INFO, WARNING, ERROR, CRITICAL }
  enum MigrationStepType { PREPARATION, TRANSFORMATION, VALIDATION, CLEANUP }
  enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
  enum RiskCategory { COMPATIBILITY, PERFORMANCE, SECURITY, OPERATIONAL }
  enum RiskImpact { MINIMAL, MODERATE, SIGNIFICANT, SEVERE }
}