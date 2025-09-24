package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WebAssembly module validation fails.
 *
 * <p>This exception is thrown when a WebAssembly module fails structural validation, type checking,
 * or other validation requirements during compilation. Module validation ensures that the
 * WebAssembly bytecode is well-formed and follows the specification.
 *
 * <p>Module validation exceptions provide detailed information about validation failures:
 *
 * <ul>
 *   <li>Validation error type and category
 *   <li>Module section and offset information
 *   <li>Type system violation details
 *   <li>Recovery and fix suggestions
 * </ul>
 *
 * @since 1.0.0
 */
public class ModuleValidationException extends ValidationException {

  private static final long serialVersionUID = 1L;

  /** The specific validation error type. */
  private final ValidationErrorType errorType;

  /** Module section where validation failed (if available). */
  private final String moduleSection;

  /** Byte offset in module where validation failed (if available). */
  private final Integer byteOffset;

  /** Recovery suggestion for this validation error. */
  private final String recoverySuggestion;

  /** Enumeration of WebAssembly validation error types. */
  public enum ValidationErrorType {
    /** Invalid WebAssembly magic number or version. */
    INVALID_MAGIC_NUMBER("Invalid WebAssembly magic number or version"),
    /** Module structure is malformed or incomplete. */
    MALFORMED_MODULE("Module structure is malformed"),
    /** Type section contains invalid type definitions. */
    INVALID_TYPE_DEFINITION("Invalid type definition"),
    /** Function signature validation failed. */
    INVALID_FUNCTION_SIGNATURE("Invalid function signature"),
    /** Import section contains invalid imports. */
    INVALID_IMPORT("Invalid import declaration"),
    /** Export section contains invalid exports. */
    INVALID_EXPORT("Invalid export declaration"),
    /** Memory definition exceeds limits or is invalid. */
    INVALID_MEMORY_DEFINITION("Invalid memory definition"),
    /** Table definition is invalid or exceeds limits. */
    INVALID_TABLE_DEFINITION("Invalid table definition"),
    /** Global definition is invalid. */
    INVALID_GLOBAL_DEFINITION("Invalid global definition"),
    /** Function body contains invalid bytecode. */
    INVALID_FUNCTION_BODY("Invalid function body"),
    /** Stack type validation failed. */
    TYPE_MISMATCH("Type mismatch in stack operations"),
    /** Control flow validation failed. */
    INVALID_CONTROL_FLOW("Invalid control flow"),
    /** Memory operations validation failed. */
    INVALID_MEMORY_OPERATION("Invalid memory operation"),
    /** Table operations validation failed. */
    INVALID_TABLE_OPERATION("Invalid table operation"),
    /** Call instruction validation failed. */
    INVALID_CALL("Invalid function call"),
    /** Local variable validation failed. */
    INVALID_LOCAL_ACCESS("Invalid local variable access"),
    /** Global variable validation failed. */
    INVALID_GLOBAL_ACCESS("Invalid global variable access"),
    /** Constant expression validation failed. */
    INVALID_CONSTANT_EXPRESSION("Invalid constant expression"),
    /** Data segment validation failed. */
    INVALID_DATA_SEGMENT("Invalid data segment"),
    /** Element segment validation failed. */
    INVALID_ELEMENT_SEGMENT("Invalid element segment"),
    /** Feature not supported in current configuration. */
    UNSUPPORTED_FEATURE("Unsupported WebAssembly feature"),
    /** Module exceeds implementation limits. */
    LIMIT_EXCEEDED("Module exceeds implementation limits"),
    /** Unknown validation error. */
    UNKNOWN("Unknown validation error");

    private final String description;

    ValidationErrorType(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this validation error type.
     *
     * @return the error type description
     */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Creates a new module validation exception with the specified error type and message.
   *
   * @param errorType the specific validation error type
   * @param message the error message
   */
  public ModuleValidationException(final ValidationErrorType errorType, final String message) {
    this(errorType, message, null, null, null);
  }

  /**
   * Creates a new module validation exception with the specified error type, message, and cause.
   *
   * @param errorType the specific validation error type
   * @param message the error message
   * @param cause the underlying cause
   */
  public ModuleValidationException(
      final ValidationErrorType errorType, final String message, final Throwable cause) {
    this(errorType, message, null, null, cause);
  }

  /**
   * Creates a new module validation exception with detailed error information.
   *
   * @param errorType the specific validation error type
   * @param message the error message
   * @param moduleSection module section where error occurred (may be null)
   * @param byteOffset byte offset where error occurred (may be null)
   * @param cause the underlying cause (may be null)
   */
  public ModuleValidationException(
      final ValidationErrorType errorType,
      final String message,
      final String moduleSection,
      final Integer byteOffset,
      final Throwable cause) {
    super(formatMessage(errorType, message, moduleSection, byteOffset), cause);
    this.errorType = errorType != null ? errorType : ValidationErrorType.UNKNOWN;
    this.moduleSection = moduleSection;
    this.byteOffset = byteOffset;
    this.recoverySuggestion = generateRecoverySuggestion(this.errorType);
  }

  /**
   * Gets the specific validation error type.
   *
   * @return the validation error type
   */
  public ValidationErrorType getErrorType() {
    return errorType;
  }

  /**
   * Gets the module section where validation failed.
   *
   * @return the module section name, or null if not available
   */
  public String getModuleSection() {
    return moduleSection;
  }

  /**
   * Gets the byte offset where validation failed.
   *
   * @return the byte offset, or null if not available
   */
  public Integer getByteOffset() {
    return byteOffset;
  }

  /**
   * Gets a recovery suggestion for this validation error.
   *
   * @return the recovery suggestion
   */
  public String getRecoverySuggestion() {
    return recoverySuggestion;
  }

  /**
   * Checks if this validation error is related to module structure.
   *
   * @return true if this is a structural error, false otherwise
   */
  public boolean isStructuralError() {
    return errorType == ValidationErrorType.INVALID_MAGIC_NUMBER
        || errorType == ValidationErrorType.MALFORMED_MODULE
        || errorType == ValidationErrorType.INVALID_DATA_SEGMENT
        || errorType == ValidationErrorType.INVALID_ELEMENT_SEGMENT;
  }

  /**
   * Checks if this validation error is related to the type system.
   *
   * @return true if this is a type error, false otherwise
   */
  public boolean isTypeError() {
    return errorType == ValidationErrorType.INVALID_TYPE_DEFINITION
        || errorType == ValidationErrorType.INVALID_FUNCTION_SIGNATURE
        || errorType == ValidationErrorType.TYPE_MISMATCH;
  }

  /**
   * Checks if this validation error is related to imports/exports.
   *
   * @return true if this is an import/export error, false otherwise
   */
  public boolean isImportExportError() {
    return errorType == ValidationErrorType.INVALID_IMPORT
        || errorType == ValidationErrorType.INVALID_EXPORT;
  }

  /**
   * Checks if this validation error is related to memory operations.
   *
   * @return true if this is a memory error, false otherwise
   */
  public boolean isMemoryError() {
    return errorType == ValidationErrorType.INVALID_MEMORY_DEFINITION
        || errorType == ValidationErrorType.INVALID_MEMORY_OPERATION;
  }

  /**
   * Checks if this validation error is related to unsupported features.
   *
   * @return true if this is a feature support error, false otherwise
   */
  public boolean isFeatureError() {
    return errorType == ValidationErrorType.UNSUPPORTED_FEATURE
        || errorType == ValidationErrorType.LIMIT_EXCEEDED;
  }

  /**
   * Formats the exception message with validation error details.
   *
   * @param errorType the validation error type
   * @param message the base message
   * @param moduleSection the module section
   * @param byteOffset the byte offset
   * @return the formatted message
   */
  private static String formatMessage(
      final ValidationErrorType errorType,
      final String message,
      final String moduleSection,
      final Integer byteOffset) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder();

    if (errorType != null) {
      sb.append("[").append(errorType.name()).append("] ");
    }

    sb.append(message);

    if (moduleSection != null && !moduleSection.isEmpty()) {
      sb.append(" (section: ").append(moduleSection).append(")");
    }

    if (byteOffset != null) {
      sb.append(" (offset: ").append(byteOffset).append(")");
    }

    return sb.toString();
  }

  /**
   * Generates a recovery suggestion based on the validation error type.
   *
   * @param errorType the validation error type
   * @return a recovery suggestion
   */
  private static String generateRecoverySuggestion(final ValidationErrorType errorType) {
    switch (errorType) {
      case INVALID_MAGIC_NUMBER:
        return "Ensure the input is valid WebAssembly bytecode, not WAT text format";
      case MALFORMED_MODULE:
        return "Check for binary corruption or incomplete module data";
      case INVALID_TYPE_DEFINITION:
        return "Review function type definitions for correct parameter and result types";
      case INVALID_FUNCTION_SIGNATURE:
        return "Verify function signatures match their type declarations";
      case INVALID_IMPORT:
        return "Check import declarations for correct module and name references";
      case INVALID_EXPORT:
        return "Verify export declarations reference valid internal definitions";
      case INVALID_MEMORY_DEFINITION:
        return "Ensure memory limits are within allowed ranges";
      case INVALID_TABLE_DEFINITION:
        return "Check table size limits and element type compatibility";
      case INVALID_GLOBAL_DEFINITION:
        return "Verify global variable types and mutability declarations";
      case INVALID_FUNCTION_BODY:
        return "Review function bytecode for invalid instructions or structure";
      case TYPE_MISMATCH:
        return "Check stack operand types match instruction requirements";
      case INVALID_CONTROL_FLOW:
        return "Verify control flow instructions have proper block structure";
      case INVALID_MEMORY_OPERATION:
        return "Check memory operation alignment and bounds";
      case INVALID_TABLE_OPERATION:
        return "Verify table operations use correct indices and types";
      case INVALID_CALL:
        return "Ensure function calls match declared signatures";
      case INVALID_LOCAL_ACCESS:
        return "Check local variable indices are within function scope";
      case INVALID_GLOBAL_ACCESS:
        return "Verify global variable access matches mutability";
      case INVALID_CONSTANT_EXPRESSION:
        return "Use only constant instructions in initializer expressions";
      case INVALID_DATA_SEGMENT:
        return "Check data segment offsets and memory references";
      case INVALID_ELEMENT_SEGMENT:
        return "Verify element segment table references and offsets";
      case UNSUPPORTED_FEATURE:
        return "Enable required WebAssembly features in engine configuration";
      case LIMIT_EXCEEDED:
        return "Reduce module complexity or split into smaller modules";
      case UNKNOWN:
      default:
        return "Review WebAssembly module for specification compliance";
    }
  }
}
