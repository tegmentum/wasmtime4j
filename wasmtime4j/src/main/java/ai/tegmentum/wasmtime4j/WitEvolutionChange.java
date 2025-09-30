package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Objects;

/**
 * Represents a change made during WIT interface evolution.
 *
 * <p>This class encapsulates information about individual changes made when evolving from one WIT
 * interface version to another, including the change type, impact, and migration guidance.
 *
 * @since 1.0.0
 */
public final class WitEvolutionChange {

  private final ChangeType type;
  private final String description;
  private final String location;
  private final ChangeImpact impact;
  private final boolean breaking;
  private final List<String> migrationSteps;
  private final String oldValue;
  private final String newValue;

  /**
   * Creates a new WIT evolution change.
   *
   * @param type the change type
   * @param description description of the change
   * @param location location of the change
   * @param impact impact of the change
   * @param breaking whether this is a breaking change
   * @param migrationSteps steps required for migration
   * @param oldValue old value (if applicable)
   * @param newValue new value (if applicable)
   */
  public WitEvolutionChange(
      final ChangeType type,
      final String description,
      final String location,
      final ChangeImpact impact,
      final boolean breaking,
      final List<String> migrationSteps,
      final String oldValue,
      final String newValue) {
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.description = Objects.requireNonNull(description, "description must not be null");
    this.location = Objects.requireNonNull(location, "location must not be null");
    this.impact = Objects.requireNonNull(impact, "impact must not be null");
    this.breaking = breaking;
    this.migrationSteps =
        List.copyOf(Objects.requireNonNull(migrationSteps, "migrationSteps must not be null"));
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Creates a function addition change.
   *
   * @param functionName the function name
   * @param signature the function signature
   * @return function addition change
   */
  public static WitEvolutionChange functionAdded(
      final String functionName, final String signature) {
    return new WitEvolutionChange(
        ChangeType.FUNCTION_ADDED,
        "Function added: " + functionName,
        "interface.functions." + functionName,
        ChangeImpact.LOW,
        false,
        List.of("Update client code to use new function if desired"),
        null,
        signature);
  }

  /**
   * Creates a function removal change.
   *
   * @param functionName the function name
   * @param signature the function signature
   * @return function removal change
   */
  public static WitEvolutionChange functionRemoved(
      final String functionName, final String signature) {
    return new WitEvolutionChange(
        ChangeType.FUNCTION_REMOVED,
        "Function removed: " + functionName,
        "interface.functions." + functionName,
        ChangeImpact.HIGH,
        true,
        List.of("Remove calls to " + functionName, "Use alternative function if available"),
        signature,
        null);
  }

  /**
   * Creates a function signature change.
   *
   * @param functionName the function name
   * @param oldSignature the old signature
   * @param newSignature the new signature
   * @return function signature change
   */
  public static WitEvolutionChange functionSignatureChanged(
      final String functionName, final String oldSignature, final String newSignature) {
    return new WitEvolutionChange(
        ChangeType.FUNCTION_SIGNATURE_CHANGED,
        "Function signature changed: " + functionName,
        "interface.functions." + functionName,
        ChangeImpact.HIGH,
        true,
        List.of("Update function calls to match new signature", "Use type adapters if available"),
        oldSignature,
        newSignature);
  }

  /**
   * Creates a type addition change.
   *
   * @param typeName the type name
   * @param typeDefinition the type definition
   * @return type addition change
   */
  public static WitEvolutionChange typeAdded(final String typeName, final String typeDefinition) {
    return new WitEvolutionChange(
        ChangeType.TYPE_ADDED,
        "Type added: " + typeName,
        "interface.types." + typeName,
        ChangeImpact.LOW,
        false,
        List.of("Update code to use new type if desired"),
        null,
        typeDefinition);
  }

  /**
   * Creates a type removal change.
   *
   * @param typeName the type name
   * @param typeDefinition the type definition
   * @return type removal change
   */
  public static WitEvolutionChange typeRemoved(final String typeName, final String typeDefinition) {
    return new WitEvolutionChange(
        ChangeType.TYPE_REMOVED,
        "Type removed: " + typeName,
        "interface.types." + typeName,
        ChangeImpact.MEDIUM,
        true,
        List.of(
            "Replace usage of " + typeName + " with alternative type",
            "Use type adapters if available"),
        typeDefinition,
        null);
  }

  /**
   * Creates a type modification change.
   *
   * @param typeName the type name
   * @param oldDefinition the old definition
   * @param newDefinition the new definition
   * @param breaking whether this is breaking
   * @return type modification change
   */
  public static WitEvolutionChange typeModified(
      final String typeName,
      final String oldDefinition,
      final String newDefinition,
      final boolean breaking) {
    return new WitEvolutionChange(
        ChangeType.TYPE_MODIFIED,
        "Type modified: " + typeName,
        "interface.types." + typeName,
        breaking ? ChangeImpact.HIGH : ChangeImpact.MEDIUM,
        breaking,
        breaking
            ? List.of("Update code using " + typeName, "Use type adapters for compatibility")
            : List.of("Review code using " + typeName + " for potential optimizations"),
        oldDefinition,
        newDefinition);
  }

  /**
   * Gets the change type.
   *
   * @return change type
   */
  public ChangeType getType() {
    return type;
  }

  /**
   * Gets the change description.
   *
   * @return description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the change location.
   *
   * @return location
   */
  public String getLocation() {
    return location;
  }

  /**
   * Gets the change impact.
   *
   * @return impact
   */
  public ChangeImpact getImpact() {
    return impact;
  }

  /**
   * Checks if this is a breaking change.
   *
   * @return true if breaking
   */
  public boolean isBreaking() {
    return breaking;
  }

  /**
   * Gets migration steps for this change.
   *
   * @return list of migration steps
   */
  public List<String> getMigrationSteps() {
    return migrationSteps;
  }

  /**
   * Gets the old value (if applicable).
   *
   * @return old value
   */
  public String getOldValue() {
    return oldValue;
  }

  /**
   * Gets the new value (if applicable).
   *
   * @return new value
   */
  public String getNewValue() {
    return newValue;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WitEvolutionChange that = (WitEvolutionChange) obj;
    return breaking == that.breaking
        && type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(location, that.location)
        && impact == that.impact
        && Objects.equals(migrationSteps, that.migrationSteps)
        && Objects.equals(oldValue, that.oldValue)
        && Objects.equals(newValue, that.newValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type, description, location, impact, breaking, migrationSteps, oldValue, newValue);
  }

  @Override
  public String toString() {
    return "WitEvolutionChange{"
        + "type="
        + type
        + ", description='"
        + description
        + '\''
        + ", location='"
        + location
        + '\''
        + ", impact="
        + impact
        + ", breaking="
        + breaking
        + '}';
  }

  /** Types of changes that can occur during interface evolution. */
  public enum ChangeType {
    /** Function was added. */
    FUNCTION_ADDED,
    /** Function was removed. */
    FUNCTION_REMOVED,
    /** Function signature was changed. */
    FUNCTION_SIGNATURE_CHANGED,
    /** Function was renamed. */
    FUNCTION_RENAMED,
    /** Type was added. */
    TYPE_ADDED,
    /** Type was removed. */
    TYPE_REMOVED,
    /** Type was modified. */
    TYPE_MODIFIED,
    /** Type was renamed. */
    TYPE_RENAMED,
    /** Import was added. */
    IMPORT_ADDED,
    /** Import was removed. */
    IMPORT_REMOVED,
    /** Export was added. */
    EXPORT_ADDED,
    /** Export was removed. */
    EXPORT_REMOVED,
    /** Interface version changed. */
    VERSION_CHANGED,
    /** Package name changed. */
    PACKAGE_CHANGED
  }

  /** Impact levels for changes. */
  public enum ChangeImpact {
    /** Low impact - cosmetic or minor changes. */
    LOW,
    /** Medium impact - functional changes that may require attention. */
    MEDIUM,
    /** High impact - significant changes requiring code updates. */
    HIGH,
    /** Critical impact - major breaking changes. */
    CRITICAL
  }
}
