package ai.tegmentum.wasmtime4j.component;

/**
 * Represents a type constraint in the WebAssembly component model.
 *
 * <p>ComponentTypeConstraint defines validation rules and restrictions that apply to component
 * types beyond their basic structure. These constraints ensure type safety and proper resource
 * management in component interactions.
 *
 * @since 1.0.0
 */
public interface ComponentTypeConstraint {

  /**
   * Gets the constraint type.
   *
   * <p>Returns the category of constraint this represents (size, range, format, etc.).
   *
   * @return the constraint type
   */
  ConstraintType getType();

  /**
   * Gets the constraint description.
   *
   * <p>Returns a human-readable description of what this constraint enforces.
   *
   * @return constraint description
   */
  String getDescription();

  /**
   * Validates a value against this constraint.
   *
   * <p>Checks if the provided component value satisfies this constraint's requirements.
   *
   * @param value the value to validate
   * @return true if the value satisfies the constraint, false otherwise
   * @throws IllegalArgumentException if value is null
   */
  boolean validate(final ComponentValue value);

  /**
   * Gets the severity level of constraint violations.
   *
   * <p>Returns how serious it is when this constraint is violated.
   *
   * @return constraint severity
   */
  ConstraintSeverity getSeverity();

  /** Categories of type constraints. */
  enum ConstraintType {
    /** Size or length constraints */
    SIZE,
    /** Numeric range constraints */
    RANGE,
    /** Format or pattern constraints */
    FORMAT,
    /** Resource availability constraints */
    RESOURCE,
    /** Nullability constraints */
    NULLABILITY,
    /** Mutability constraints */
    MUTABILITY,
    /** Custom application-specific constraints */
    CUSTOM
  }

  /** Severity levels for constraint violations. */
  enum ConstraintSeverity {
    /** Violation is informational only */
    INFO,
    /** Violation generates a warning */
    WARNING,
    /** Violation causes an error */
    ERROR,
    /** Violation causes immediate failure */
    FATAL
  }
}
