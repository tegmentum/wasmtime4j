package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WebAssembly module instantiation fails.
 *
 * <p>This exception is thrown when a compiled WebAssembly module cannot be instantiated due to
 * missing imports, incompatible types, initialization failures, or resource constraints.
 * Instantiation occurs after successful compilation but before the instance is ready for execution.
 *
 * <p>Module instantiation exceptions provide detailed information about instantiation failures:
 *
 * <ul>
 *   <li>Instantiation error type and phase
 *   <li>Import/export resolution details
 *   <li>Resource allocation information
 *   <li>Recovery and configuration suggestions
 * </ul>
 *
 * @since 1.0.0
 */
public class ModuleInstantiationException extends InstantiationException {

  private static final long serialVersionUID = 1L;

  /** The specific instantiation error type. */
  private final InstantiationErrorType errorType;

  /** Instantiation phase where the error occurred. */
  private final InstantiationPhase phase;

  /** Import name that failed resolution (if applicable). */
  private final String importName;

  /** Module name for import resolution (if applicable). */
  private final String moduleName;

  /** Recovery suggestion for this instantiation error. */
  private final String recoverySuggestion;

  /** Enumeration of WebAssembly instantiation error types. */
  public enum InstantiationErrorType {
    /** Required import was not provided. */
    MISSING_IMPORT("Required import not provided"),
    /** Import type does not match expected type. */
    IMPORT_TYPE_MISMATCH("Import type mismatch"),
    /** Function import signature mismatch. */
    FUNCTION_SIGNATURE_MISMATCH("Function import signature mismatch"),
    /** Memory import size incompatible. */
    MEMORY_IMPORT_INCOMPATIBLE("Memory import incompatible"),
    /** Table import incompatible. */
    TABLE_IMPORT_INCOMPATIBLE("Table import incompatible"),
    /** Global import type mismatch. */
    GLOBAL_IMPORT_MISMATCH("Global import type mismatch"),
    /** Start function execution failed. */
    START_FUNCTION_FAILED("Start function execution failed"),
    /** Data segment initialization failed. */
    DATA_SEGMENT_INIT_FAILED("Data segment initialization failed"),
    /** Element segment initialization failed. */
    ELEMENT_SEGMENT_INIT_FAILED("Element segment initialization failed"),
    /** Memory allocation failed during instantiation. */
    MEMORY_ALLOCATION_FAILED("Memory allocation failed"),
    /** Table allocation failed during instantiation. */
    TABLE_ALLOCATION_FAILED("Table allocation failed"),
    /** Global initialization failed. */
    GLOBAL_INIT_FAILED("Global initialization failed"),
    /** Resource limit exceeded during instantiation. */
    RESOURCE_LIMIT_EXCEEDED("Resource limit exceeded"),
    /** Instantiation timeout exceeded. */
    TIMEOUT("Instantiation timeout exceeded"),
    /** Multiple memory definitions not supported. */
    MULTIPLE_MEMORIES_UNSUPPORTED("Multiple memories not supported"),
    /** Multiple tables not supported. */
    MULTIPLE_TABLES_UNSUPPORTED("Multiple tables not supported"),
    /** Import resolution failed. */
    IMPORT_RESOLUTION_FAILED("Import resolution failed"),
    /** Linker configuration error. */
    LINKER_ERROR("Linker configuration error"),
    /** Store configuration incompatible. */
    STORE_INCOMPATIBLE("Store configuration incompatible"),
    /** Unknown instantiation error. */
    UNKNOWN("Unknown instantiation error");

    private final String description;

    InstantiationErrorType(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this instantiation error type.
     *
     * @return the error type description
     */
    public String getDescription() {
      return description;
    }
  }

  /** Enumeration of instantiation phases. */
  public enum InstantiationPhase {
    /** Import resolution phase. */
    IMPORT_RESOLUTION("Import Resolution"),
    /** Memory allocation phase. */
    MEMORY_ALLOCATION("Memory Allocation"),
    /** Table allocation phase. */
    TABLE_ALLOCATION("Table Allocation"),
    /** Global initialization phase. */
    GLOBAL_INITIALIZATION("Global Initialization"),
    /** Data segment initialization phase. */
    DATA_SEGMENT_INIT("Data Segment Initialization"),
    /** Element segment initialization phase. */
    ELEMENT_SEGMENT_INIT("Element Segment Initialization"),
    /** Start function execution phase. */
    START_FUNCTION_EXEC("Start Function Execution"),
    /** Final instance setup phase. */
    FINALIZATION("Finalization"),
    /** Unknown instantiation phase. */
    UNKNOWN("Unknown Phase");

    private final String description;

    InstantiationPhase(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this instantiation phase.
     *
     * @return the phase description
     */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Creates a new module instantiation exception with the specified error type and message.
   *
   * @param errorType the specific instantiation error type
   * @param message the error message
   */
  public ModuleInstantiationException(
      final InstantiationErrorType errorType, final String message) {
    this(errorType, message, InstantiationPhase.UNKNOWN, null, null, null);
  }

  /**
   * Creates a new module instantiation exception with the specified error type, message, and cause.
   *
   * @param errorType the specific instantiation error type
   * @param message the error message
   * @param cause the underlying cause
   */
  public ModuleInstantiationException(
      final InstantiationErrorType errorType, final String message, final Throwable cause) {
    this(errorType, message, InstantiationPhase.UNKNOWN, null, null, cause);
  }

  /**
   * Creates a new module instantiation exception with detailed error information.
   *
   * @param errorType the specific instantiation error type
   * @param message the error message
   * @param phase instantiation phase where error occurred
   * @param importName import name that failed (may be null)
   * @param moduleName module name for import (may be null)
   * @param cause the underlying cause (may be null)
   */
  public ModuleInstantiationException(
      final InstantiationErrorType errorType,
      final String message,
      final InstantiationPhase phase,
      final String importName,
      final String moduleName,
      final Throwable cause) {
    super(formatMessage(errorType, message, phase, importName, moduleName), cause);
    this.errorType = errorType != null ? errorType : InstantiationErrorType.UNKNOWN;
    this.phase = phase != null ? phase : InstantiationPhase.UNKNOWN;
    this.importName = importName;
    this.moduleName = moduleName;
    this.recoverySuggestion = generateRecoverySuggestion(this.errorType);
  }

  /**
   * Gets the specific instantiation error type.
   *
   * @return the instantiation error type
   */
  public InstantiationErrorType getErrorType() {
    return errorType;
  }

  /**
   * Gets the instantiation phase where the error occurred.
   *
   * @return the instantiation phase
   */
  public InstantiationPhase getPhase() {
    return phase;
  }

  /**
   * Gets the import name that failed resolution.
   *
   * @return the import name, or null if not applicable
   */
  public String getImportName() {
    return importName;
  }

  /**
   * Gets the module name for import resolution.
   *
   * @return the module name, or null if not applicable
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Gets a recovery suggestion for this instantiation error.
   *
   * @return the recovery suggestion
   */
  public String getRecoverySuggestion() {
    return recoverySuggestion;
  }

  /**
   * Checks if this instantiation error is related to imports.
   *
   * @return true if this is an import error, false otherwise
   */
  public boolean isImportError() {
    return errorType == InstantiationErrorType.MISSING_IMPORT
        || errorType == InstantiationErrorType.IMPORT_TYPE_MISMATCH
        || errorType == InstantiationErrorType.FUNCTION_SIGNATURE_MISMATCH
        || errorType == InstantiationErrorType.MEMORY_IMPORT_INCOMPATIBLE
        || errorType == InstantiationErrorType.TABLE_IMPORT_INCOMPATIBLE
        || errorType == InstantiationErrorType.GLOBAL_IMPORT_MISMATCH
        || errorType == InstantiationErrorType.IMPORT_RESOLUTION_FAILED;
  }

  /**
   * Checks if this instantiation error is related to resource allocation.
   *
   * @return true if this is a resource error, false otherwise
   */
  public boolean isResourceError() {
    return errorType == InstantiationErrorType.MEMORY_ALLOCATION_FAILED
        || errorType == InstantiationErrorType.TABLE_ALLOCATION_FAILED
        || errorType == InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED
        || errorType == InstantiationErrorType.TIMEOUT;
  }

  /**
   * Checks if this instantiation error is related to initialization.
   *
   * @return true if this is an initialization error, false otherwise
   */
  public boolean isInitializationError() {
    return errorType == InstantiationErrorType.START_FUNCTION_FAILED
        || errorType == InstantiationErrorType.DATA_SEGMENT_INIT_FAILED
        || errorType == InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED
        || errorType == InstantiationErrorType.GLOBAL_INIT_FAILED;
  }

  /**
   * Checks if this instantiation error is related to configuration.
   *
   * @return true if this is a configuration error, false otherwise
   */
  public boolean isConfigurationError() {
    return errorType == InstantiationErrorType.LINKER_ERROR
        || errorType == InstantiationErrorType.STORE_INCOMPATIBLE
        || errorType == InstantiationErrorType.MULTIPLE_MEMORIES_UNSUPPORTED
        || errorType == InstantiationErrorType.MULTIPLE_TABLES_UNSUPPORTED;
  }

  /**
   * Formats the exception message with instantiation error details.
   *
   * @param errorType the instantiation error type
   * @param message the base message
   * @param phase the instantiation phase
   * @param importName the import name
   * @param moduleName the module name
   * @return the formatted message
   */
  private static String formatMessage(
      final InstantiationErrorType errorType,
      final String message,
      final InstantiationPhase phase,
      final String importName,
      final String moduleName) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder();

    if (errorType != null) {
      sb.append("[").append(errorType.name()).append("] ");
    }

    sb.append(message);

    if (phase != null && phase != InstantiationPhase.UNKNOWN) {
      sb.append(" (phase: ").append(phase.getDescription()).append(")");
    }

    if (moduleName != null
        && !moduleName.isEmpty()
        && importName != null
        && !importName.isEmpty()) {
      sb.append(" (import: ").append(moduleName).append(".").append(importName).append(")");
    } else if (importName != null && !importName.isEmpty()) {
      sb.append(" (import: ").append(importName).append(")");
    } else if (moduleName != null && !moduleName.isEmpty()) {
      sb.append(" (module: ").append(moduleName).append(")");
    }

    return sb.toString();
  }

  /**
   * Generates a recovery suggestion based on the instantiation error type.
   *
   * @param errorType the instantiation error type
   * @return a recovery suggestion
   */
  private static String generateRecoverySuggestion(final InstantiationErrorType errorType) {
    switch (errorType) {
      case MISSING_IMPORT:
        return "Provide all required imports through the linker";
      case IMPORT_TYPE_MISMATCH:
        return "Ensure import types match module expectations";
      case FUNCTION_SIGNATURE_MISMATCH:
        return "Verify function import signatures match declarations";
      case MEMORY_IMPORT_INCOMPATIBLE:
        return "Check memory import size and limits compatibility";
      case TABLE_IMPORT_INCOMPATIBLE:
        return "Verify table import size and element type compatibility";
      case GLOBAL_IMPORT_MISMATCH:
        return "Ensure global import types and mutability match";
      case START_FUNCTION_FAILED:
        return "Debug start function for runtime errors or missing imports";
      case DATA_SEGMENT_INIT_FAILED:
        return "Check data segment memory bounds and initialization";
      case ELEMENT_SEGMENT_INIT_FAILED:
        return "Verify element segment table bounds and references";
      case MEMORY_ALLOCATION_FAILED:
        return "Increase memory limits or reduce memory requirements";
      case TABLE_ALLOCATION_FAILED:
        return "Increase table limits or reduce table size";
      case GLOBAL_INIT_FAILED:
        return "Check global initializer expressions for validity";
      case RESOURCE_LIMIT_EXCEEDED:
        return "Increase resource limits or reduce module requirements";
      case TIMEOUT:
        return "Increase instantiation timeout or optimize initialization";
      case MULTIPLE_MEMORIES_UNSUPPORTED:
        return "Use single memory or enable multi-memory support";
      case MULTIPLE_TABLES_UNSUPPORTED:
        return "Use single table or enable multi-table support";
      case IMPORT_RESOLUTION_FAILED:
        return "Check import namespace and name resolution";
      case LINKER_ERROR:
        return "Review linker configuration and import setup";
      case STORE_INCOMPATIBLE:
        return "Ensure store configuration matches module requirements";
      case UNKNOWN:
      default:
        return "Review module imports and instantiation configuration";
    }
  }
}
