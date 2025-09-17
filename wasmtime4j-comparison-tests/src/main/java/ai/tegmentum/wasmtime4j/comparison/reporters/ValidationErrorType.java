package ai.tegmentum.wasmtime4j.comparison.reporters;

/** Types of validation errors. */
public enum ValidationErrorType {
  /** Schema was not found. */
  SCHEMA_NOT_FOUND,
  /** Unsupported format. */
  UNSUPPORTED_FORMAT,
  /** Unsupported version. */
  UNSUPPORTED_VERSION,
  /** Invalid format. */
  INVALID_FORMAT,
  /** Missing required field. */
  MISSING_REQUIRED_FIELD,
  /** Invalid field value. */
  INVALID_FIELD_VALUE,
  /** Validation exception. */
  VALIDATION_EXCEPTION
}
