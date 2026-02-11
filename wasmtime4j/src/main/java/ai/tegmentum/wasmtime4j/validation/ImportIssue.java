package ai.tegmentum.wasmtime4j.validation;

import java.util.Objects;

/**
 * Represents an issue found during import validation.
 *
 * <p>Import issues include missing imports, type mismatches, circular dependencies, and other
 * problems that prevent successful module instantiation.
 *
 * @since 1.0.0
 */
public final class ImportIssue {

  private final Severity severity;
  private final Type type;
  private final String moduleName;
  private final String importName;
  private final String message;
  private final String expectedType;
  private final String actualType;

  /**
   * Creates a new import issue.
   *
   * @param severity the severity level of the issue
   * @param type the type of issue
   * @param moduleName the module name where the issue occurred
   * @param importName the import name that has the issue
   * @param message a detailed description of the issue
   * @param expectedType the expected type (null if not applicable)
   * @param actualType the actual type found (null if not applicable)
   */
  public ImportIssue(
      final Severity severity,
      final Type type,
      final String moduleName,
      final String importName,
      final String message,
      final String expectedType,
      final String actualType) {
    this.severity = Objects.requireNonNull(severity, "severity");
    this.type = Objects.requireNonNull(type, "type");
    this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
    this.importName = Objects.requireNonNull(importName, "importName");
    this.message = Objects.requireNonNull(message, "message");
    this.expectedType = expectedType;
    this.actualType = actualType;
  }

  /**
   * Creates a new import issue without type information.
   *
   * @param severity the severity level of the issue
   * @param type the type of issue
   * @param moduleName the module name where the issue occurred
   * @param importName the import name that has the issue
   * @param message a detailed description of the issue
   */
  public ImportIssue(
      final Severity severity,
      final Type type,
      final String moduleName,
      final String importName,
      final String message) {
    this(severity, type, moduleName, importName, message, null, null);
  }

  /**
   * Gets the severity level of this issue.
   *
   * @return the severity level
   */
  public Severity getSeverity() {
    return severity;
  }

  /**
   * Gets the type of this issue.
   *
   * @return the issue type
   */
  public Type getType() {
    return type;
  }

  /**
   * Gets the module name where this issue occurred.
   *
   * @return the module name
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Gets the import name that has this issue.
   *
   * @return the import name
   */
  public String getImportName() {
    return importName;
  }

  /**
   * Gets a detailed description of this issue.
   *
   * @return the issue message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the expected type (if applicable).
   *
   * @return the expected type, or null if not applicable
   */
  public String getExpectedType() {
    return expectedType;
  }

  /**
   * Gets the actual type found (if applicable).
   *
   * @return the actual type, or null if not applicable
   */
  public String getActualType() {
    return actualType;
  }

  /**
   * Gets the full import identifier.
   *
   * @return a string in the format "moduleName::importName"
   */
  public String getImportIdentifier() {
    return moduleName + "::" + importName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(severity).append(" ").append(type).append(": ");
    sb.append(getImportIdentifier()).append(" - ").append(message);

    if (expectedType != null && actualType != null) {
      sb.append(" (expected: ").append(expectedType);
      sb.append(", actual: ").append(actualType).append(")");
    }

    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ImportIssue that = (ImportIssue) obj;
    return severity == that.severity
        && type == that.type
        && Objects.equals(moduleName, that.moduleName)
        && Objects.equals(importName, that.importName)
        && Objects.equals(message, that.message)
        && Objects.equals(expectedType, that.expectedType)
        && Objects.equals(actualType, that.actualType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(severity, type, moduleName, importName, message, expectedType, actualType);
  }

  /** Severity levels for import issues. */
  public enum Severity {
    /** Informational issue that doesn't prevent instantiation. */
    INFO,
    /** Warning that might cause issues but doesn't prevent instantiation. */
    WARNING,
    /** Error that will prevent successful instantiation. */
    ERROR,
    /** Critical error that indicates a serious problem. */
    CRITICAL
  }

  /** Types of import issues. */
  public enum Type {
    /** Import is missing from the linker. */
    MISSING_IMPORT,
    /** Import type doesn't match expected type. */
    TYPE_MISMATCH,
    /** Circular dependency detected. */
    CIRCULAR_DEPENDENCY,
    /** Import signature is incompatible. */
    SIGNATURE_MISMATCH,
    /** Import is not available in the specified module. */
    MODULE_NOT_FOUND,
    /** Import name is not exported by the target module. */
    EXPORT_NOT_FOUND,
    /** Import cannot be resolved due to multiple candidates. */
    AMBIGUOUS_IMPORT,
    /** Import validation failed for unknown reasons. */
    VALIDATION_FAILED
  }
}
