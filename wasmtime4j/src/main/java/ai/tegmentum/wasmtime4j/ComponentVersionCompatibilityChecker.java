package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Component version compatibility checking interface.
 *
 * <p>This interface provides comprehensive version compatibility validation for WebAssembly
 * components, supporting semantic versioning, interface versioning, and custom compatibility rules.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Semantic version compatibility checking
 *   <li>Interface version validation
 *   <li>Dependency version resolution
 *   <li>Breaking change detection
 *   <li>Compatibility matrix management
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentVersionCompatibilityChecker {

  /**
   * Checks if two component versions are compatible.
   *
   * @param sourceVersion the source component version
   * @param targetVersion the target component version
   * @return compatibility result with detailed information
   * @throws IllegalArgumentException if any version is null
   */
  ComponentVersionCompatibilityResult checkCompatibility(
      ComponentVersion sourceVersion, ComponentVersion targetVersion);

  /**
   * Checks if a component version is compatible with a set of dependency versions.
   *
   * @param componentVersion the component version
   * @param dependencyVersions the dependency versions to check against
   * @return compatibility result for each dependency
   * @throws IllegalArgumentException if any parameter is null
   */
  Map<String, ComponentVersionCompatibilityResult> checkDependencyCompatibility(
      ComponentVersion componentVersion, Map<String, ComponentVersion> dependencyVersions);

  /**
   * Finds the best compatible version from a set of available versions.
   *
   * @param requiredVersion the required version specification
   * @param availableVersions the available versions to choose from
   * @return the best compatible version, or null if none are compatible
   * @throws IllegalArgumentException if any parameter is null
   */
  ComponentVersion findBestCompatibleVersion(
      ComponentVersionRequirement requiredVersion, Set<ComponentVersion> availableVersions);

  /**
   * Validates interface compatibility between component versions.
   *
   * @param sourceInterfaces the source component interfaces
   * @param targetInterfaces the target component interfaces
   * @return interface compatibility result
   * @throws IllegalArgumentException if any parameter is null
   */
  InterfaceCompatibilityResult checkInterfaceCompatibility(
      Set<WitInterfaceDefinition> sourceInterfaces, Set<WitInterfaceDefinition> targetInterfaces);

  /**
   * Checks for breaking changes between component versions.
   *
   * @param oldVersion the old component version
   * @param newVersion the new component version
   * @return breaking change analysis result
   * @throws IllegalArgumentException if any parameter is null
   */
  BreakingChangeAnalysisResult analyzeBreakingChanges(
      ComponentVersion oldVersion, ComponentVersion newVersion);

  /**
   * Creates a compatibility matrix for a set of components.
   *
   * @param components the components to analyze
   * @return compatibility matrix showing all pairwise compatibility results
   * @throws WasmException if analysis fails
   * @throws IllegalArgumentException if components is null or empty
   */
  ComponentCompatibilityMatrix createCompatibilityMatrix(Set<ComponentSimple> components)
      throws WasmException;

  /**
   * Validates a component upgrade path.
   *
   * @param currentVersion the current version
   * @param targetVersion the target version to upgrade to
   * @return upgrade path validation result
   * @throws IllegalArgumentException if any parameter is null
   */
  UpgradePathValidationResult validateUpgradePath(
      ComponentVersion currentVersion, ComponentVersion targetVersion);

  /**
   * Gets compatibility rules for a specific component type.
   *
   * @param componentType the component type
   * @return set of compatibility rules
   * @throws IllegalArgumentException if componentType is null or empty
   */
  Set<CompatibilityRule> getCompatibilityRules(String componentType);

  /**
   * Adds a custom compatibility rule.
   *
   * @param rule the compatibility rule to add
   * @throws WasmException if the rule cannot be added
   * @throws IllegalArgumentException if rule is null
   */
  void addCompatibilityRule(CompatibilityRule rule) throws WasmException;

  /**
   * Removes a compatibility rule.
   *
   * @param ruleId the ID of the rule to remove
   * @throws WasmException if the rule cannot be removed
   * @throws IllegalArgumentException if ruleId is null or empty
   */
  void removeCompatibilityRule(String ruleId) throws WasmException;

  /** Component version compatibility result. */
  final class ComponentVersionCompatibilityResult {
    private final boolean compatible;
    private final String message;
    private final CompatibilityLevel level;
    private final Set<String> issues;
    private final Set<String> warnings;

    /**
     * Creates a new compatibility result.
     *
     * @param compatible whether components are compatible
     * @param message descriptive message about compatibility
     * @param level compatibility level achieved
     * @param issues set of compatibility issues found
     * @param warnings set of compatibility warnings
     */
    public ComponentVersionCompatibilityResult(
        boolean compatible,
        String message,
        CompatibilityLevel level,
        Set<String> issues,
        Set<String> warnings) {
      this.compatible = compatible;
      this.message = message;
      this.level = level;
      this.issues = issues == null ? null : new java.util.HashSet<>(issues);
      this.warnings = warnings == null ? null : new java.util.HashSet<>(warnings);
    }

    public boolean isCompatible() {
      return compatible;
    }

    public String getMessage() {
      return message;
    }

    public CompatibilityLevel getLevel() {
      return level;
    }

    public Set<String> getIssues() {
      return issues == null ? null : new java.util.HashSet<>(issues);
    }

    public Set<String> getWarnings() {
      return warnings == null ? null : new java.util.HashSet<>(warnings);
    }
  }

  /** Interface compatibility result. */
  final class InterfaceCompatibilityResult {
    private final boolean compatible;
    private final Map<String, InterfaceVersionCompatibility> interfaceResults;
    private final Set<String> missingInterfaces;
    private final Set<String> extraInterfaces;

    /**
     * Creates a new interface compatibility result.
     *
     * @param compatible whether all interfaces are compatible
     * @param interfaceResults detailed results for each interface
     * @param missingInterfaces interfaces missing in target
     * @param extraInterfaces interfaces not in source
     */
    public InterfaceCompatibilityResult(
        boolean compatible,
        Map<String, InterfaceVersionCompatibility> interfaceResults,
        Set<String> missingInterfaces,
        Set<String> extraInterfaces) {
      this.compatible = compatible;
      this.interfaceResults =
          interfaceResults == null ? null : new java.util.HashMap<>(interfaceResults);
      this.missingInterfaces =
          missingInterfaces == null ? null : new java.util.HashSet<>(missingInterfaces);
      this.extraInterfaces =
          extraInterfaces == null ? null : new java.util.HashSet<>(extraInterfaces);
    }

    public boolean isCompatible() {
      return compatible;
    }

    public Map<String, InterfaceVersionCompatibility> getInterfaceResults() {
      return interfaceResults == null ? null : new java.util.HashMap<>(interfaceResults);
    }

    public Set<String> getMissingInterfaces() {
      return missingInterfaces == null ? null : new java.util.HashSet<>(missingInterfaces);
    }

    public Set<String> getExtraInterfaces() {
      return extraInterfaces == null ? null : new java.util.HashSet<>(extraInterfaces);
    }
  }

  /** Interface version compatibility details. */
  final class InterfaceVersionCompatibility {
    private final String interfaceName;
    private final boolean compatible;
    private final WitInterfaceVersion sourceVersion;
    private final WitInterfaceVersion targetVersion;
    private final Set<String> incompatibleFunctions;
    private final Set<String> incompatibleTypes;

    /**
     * Creates a new interface version compatibility result.
     *
     * @param interfaceName name of the interface
     * @param compatible whether versions are compatible
     * @param sourceVersion source interface version
     * @param targetVersion target interface version
     * @param incompatibleFunctions functions that are incompatible
     * @param incompatibleTypes types that are incompatible
     */
    public InterfaceVersionCompatibility(
        String interfaceName,
        boolean compatible,
        WitInterfaceVersion sourceVersion,
        WitInterfaceVersion targetVersion,
        Set<String> incompatibleFunctions,
        Set<String> incompatibleTypes) {
      this.interfaceName = interfaceName;
      this.compatible = compatible;
      this.sourceVersion = sourceVersion;
      this.targetVersion = targetVersion;
      this.incompatibleFunctions =
          incompatibleFunctions == null ? null : new java.util.HashSet<>(incompatibleFunctions);
      this.incompatibleTypes =
          incompatibleTypes == null ? null : new java.util.HashSet<>(incompatibleTypes);
    }

    public String getInterfaceName() {
      return interfaceName;
    }

    public boolean isCompatible() {
      return compatible;
    }

    public WitInterfaceVersion getSourceVersion() {
      return sourceVersion;
    }

    public WitInterfaceVersion getTargetVersion() {
      return targetVersion;
    }

    public Set<String> getIncompatibleFunctions() {
      return incompatibleFunctions == null ? null : new java.util.HashSet<>(incompatibleFunctions);
    }

    public Set<String> getIncompatibleTypes() {
      return incompatibleTypes == null ? null : new java.util.HashSet<>(incompatibleTypes);
    }
  }

  /** Breaking change analysis result. */
  final class BreakingChangeAnalysisResult {
    private final boolean hasBreakingChanges;
    private final List<BreakingChange> breakingChanges;
    private final List<CompatibleChange> compatibleChanges;
    private final VersionChangeType changeType;

    /**
     * Creates a new breaking change analysis result.
     *
     * @param hasBreakingChanges whether breaking changes were found
     * @param breakingChanges list of breaking changes
     * @param compatibleChanges list of compatible changes
     * @param changeType type of version change
     */
    public BreakingChangeAnalysisResult(
        boolean hasBreakingChanges,
        List<BreakingChange> breakingChanges,
        List<CompatibleChange> compatibleChanges,
        VersionChangeType changeType) {
      this.hasBreakingChanges = hasBreakingChanges;
      this.breakingChanges =
          breakingChanges == null ? null : new java.util.ArrayList<>(breakingChanges);
      this.compatibleChanges =
          compatibleChanges == null ? null : new java.util.ArrayList<>(compatibleChanges);
      this.changeType = changeType;
    }

    public boolean hasBreakingChanges() {
      return hasBreakingChanges;
    }

    public List<BreakingChange> getBreakingChanges() {
      return breakingChanges == null ? null : new java.util.ArrayList<>(breakingChanges);
    }

    public List<CompatibleChange> getCompatibleChanges() {
      return compatibleChanges == null ? null : new java.util.ArrayList<>(compatibleChanges);
    }

    public VersionChangeType getChangeType() {
      return changeType;
    }
  }

  /** Component compatibility matrix. */
  final class ComponentCompatibilityMatrix {
    private final Map<String, Map<String, ComponentVersionCompatibilityResult>> matrix;
    private final Set<String> componentIds;

    public ComponentCompatibilityMatrix(
        Map<String, Map<String, ComponentVersionCompatibilityResult>> matrix,
        Set<String> componentIds) {
      this.matrix = matrix == null ? null : new java.util.HashMap<>(matrix);
      this.componentIds = componentIds == null ? null : new java.util.HashSet<>(componentIds);
    }

    public ComponentVersionCompatibilityResult getCompatibility(String source, String target) {
      return matrix.getOrDefault(source, Map.of()).get(target);
    }

    public Map<String, Map<String, ComponentVersionCompatibilityResult>> getMatrix() {
      return matrix == null ? null : new java.util.HashMap<>(matrix);
    }

    public Set<String> getComponentIds() {
      return componentIds == null ? null : new java.util.HashSet<>(componentIds);
    }
  }

  /** Upgrade path validation result. */
  final class UpgradePathValidationResult {
    private final boolean valid;
    private final List<ComponentVersion> intermediatePath;
    private final Set<String> blockers;
    private final Set<String> warnings;

    /**
     * Creates a new upgrade path validation result.
     *
     * @param valid whether the upgrade path is valid
     * @param intermediatePath intermediate versions in the upgrade path
     * @param blockers blocking issues preventing upgrade
     * @param warnings warning messages for the upgrade
     */
    public UpgradePathValidationResult(
        boolean valid,
        List<ComponentVersion> intermediatePath,
        Set<String> blockers,
        Set<String> warnings) {
      this.valid = valid;
      this.intermediatePath =
          intermediatePath == null ? null : new java.util.ArrayList<>(intermediatePath);
      this.blockers = blockers == null ? null : new java.util.HashSet<>(blockers);
      this.warnings = warnings == null ? null : new java.util.HashSet<>(warnings);
    }

    public boolean isValid() {
      return valid;
    }

    public List<ComponentVersion> getIntermediatePath() {
      return intermediatePath == null ? null : new java.util.ArrayList<>(intermediatePath);
    }

    public Set<String> getBlockers() {
      return blockers == null ? null : new java.util.HashSet<>(blockers);
    }

    public Set<String> getWarnings() {
      return warnings == null ? null : new java.util.HashSet<>(warnings);
    }
  }

  /** Breaking change details. */
  final class BreakingChange {
    private final BreakingChangeType type;
    private final String description;
    private final String affectedElement;
    private final Severity severity;

    /**
     * Creates a new breaking change.
     *
     * @param type type of breaking change
     * @param description description of the change
     * @param affectedElement element affected by the change
     * @param severity severity level of the change
     */
    public BreakingChange(
        BreakingChangeType type, String description, String affectedElement, Severity severity) {
      this.type = type;
      this.description = description;
      this.affectedElement = affectedElement;
      this.severity = severity;
    }

    public BreakingChangeType getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public String getAffectedElement() {
      return affectedElement;
    }

    public Severity getSeverity() {
      return severity;
    }
  }

  /** Compatible change details. */
  final class CompatibleChange {
    private final CompatibleChangeType type;
    private final String description;
    private final String affectedElement;

    /**
     * Creates a new compatible change.
     *
     * @param type type of compatible change
     * @param description description of the change
     * @param affectedElement element affected by the change
     */
    public CompatibleChange(CompatibleChangeType type, String description, String affectedElement) {
      this.type = type;
      this.description = description;
      this.affectedElement = affectedElement;
    }

    public CompatibleChangeType getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public String getAffectedElement() {
      return affectedElement;
    }
  }

  /** Compatibility rule. */
  final class CompatibilityRule {
    private final String id;
    private final String componentType;
    private final VersionPattern sourcePattern;
    private final VersionPattern targetPattern;
    private final CompatibilityLevel level;
    private final String description;

    /**
     * Creates a new compatibility rule.
     *
     * @param id unique identifier for the rule
     * @param componentType type of component this rule applies to
     * @param sourcePattern pattern for source versions
     * @param targetPattern pattern for target versions
     * @param level compatibility level this rule provides
     * @param description description of the rule
     */
    public CompatibilityRule(
        String id,
        String componentType,
        VersionPattern sourcePattern,
        VersionPattern targetPattern,
        CompatibilityLevel level,
        String description) {
      this.id = id;
      this.componentType = componentType;
      this.sourcePattern = sourcePattern;
      this.targetPattern = targetPattern;
      this.level = level;
      this.description = description;
    }

    public String getId() {
      return id;
    }

    public String getComponentType() {
      return componentType;
    }

    public VersionPattern getSourcePattern() {
      return sourcePattern;
    }

    public VersionPattern getTargetPattern() {
      return targetPattern;
    }

    public CompatibilityLevel getLevel() {
      return level;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Version pattern for matching versions. */
  final class VersionPattern {
    private final String pattern;
    private final PatternType type;

    public VersionPattern(String pattern, PatternType type) {
      this.pattern = pattern;
      this.type = type;
    }

    public String getPattern() {
      return pattern;
    }

    public PatternType getType() {
      return type;
    }

    /**
     * Checks if this pattern matches the given version.
     *
     * @param version version to match against
     * @return true if the pattern matches the version
     */
    public boolean matches(ComponentVersion version) {
      switch (type) {
        case EXACT:
          return version.toString().equals(pattern);
        case SEMANTIC_RANGE:
          return matchesSemanticRange(version);
        case REGEX:
          return version.toString().matches(pattern);
        default:
          return false;
      }
    }

    private boolean matchesSemanticRange(ComponentVersion version) {
      // Implement semantic version range matching
      return true; // Placeholder
    }
  }

  /** Component version requirement specification. */
  final class ComponentVersionRequirement {
    private final String requirement;
    private final RequirementType type;

    public ComponentVersionRequirement(String requirement, RequirementType type) {
      this.requirement = requirement;
      this.type = type;
    }

    public String getRequirement() {
      return requirement;
    }

    public RequirementType getType() {
      return type;
    }
  }

  /** Compatibility levels. */
  enum CompatibilityLevel {
    FULLY_COMPATIBLE,
    BACKWARD_COMPATIBLE,
    FORWARD_COMPATIBLE,
    INCOMPATIBLE
  }

  /** Version change types. */
  enum VersionChangeType {
    MAJOR,
    MINOR,
    PATCH,
    PRERELEASE,
    BUILD
  }

  /** Breaking change types. */
  enum BreakingChangeType {
    FUNCTION_REMOVED,
    FUNCTION_SIGNATURE_CHANGED,
    TYPE_REMOVED,
    TYPE_DEFINITION_CHANGED,
    INTERFACE_REMOVED,
    INTERFACE_VERSION_INCOMPATIBLE
  }

  /** Compatible change types. */
  enum CompatibleChangeType {
    FUNCTION_ADDED,
    TYPE_ADDED,
    INTERFACE_ADDED,
    PARAMETER_ADDED_WITH_DEFAULT,
    DOCUMENTATION_UPDATED
  }

  /** Breaking change severity levels. */
  enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  /** Pattern types for version matching. */
  enum PatternType {
    EXACT,
    SEMANTIC_RANGE,
    REGEX
  }

  /** Requirement types for version specifications. */
  enum RequirementType {
    EXACT,
    MINIMUM,
    MAXIMUM,
    RANGE,
    COMPATIBLE,
    LATEST
  }
}
