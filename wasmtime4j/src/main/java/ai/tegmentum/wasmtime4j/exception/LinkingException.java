package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WebAssembly module linking fails.
 *
 * <p>This exception is thrown when WebAssembly modules cannot be linked together due to
 * import/export mismatches, missing dependencies, or incompatible interfaces. Linking occurs during
 * module instantiation when resolving imports against exports from other modules or host functions.
 *
 * <p>Linking exceptions provide detailed information about linking failures:
 *
 * <ul>
 *   <li>Linking error type and category
 *   <li>Import/export name and module details
 *   <li>Type signature information
 *   <li>Resolution and configuration suggestions
 * </ul>
 *
 * @since 1.0.0
 */
public class LinkingException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The specific linking error type. */
  private final LinkingErrorType errorType;

  /** Module name where linking failed. */
  private final String moduleName;

  /** Import or export name that failed linking. */
  private final String itemName;

  /** Expected type signature. */
  private final String expectedType;

  /** Actual type signature (if available). */
  private final String actualType;

  /** Recovery suggestion for this linking error. */
  private final String recoverySuggestion;

  /** Enumeration of WebAssembly linking error types. */
  public enum LinkingErrorType {
    /** Import not found in any linked module. */
    IMPORT_NOT_FOUND("Import not found"),
    /** Export not found in specified module. */
    EXPORT_NOT_FOUND("Export not found"),
    /** Function signature mismatch between import and export. */
    FUNCTION_SIGNATURE_MISMATCH("Function signature mismatch"),
    /** Memory size mismatch between import and export. */
    MEMORY_SIZE_MISMATCH("Memory size mismatch"),
    /** Memory limits incompatible between import and export. */
    MEMORY_LIMITS_INCOMPATIBLE("Memory limits incompatible"),
    /** Table size mismatch between import and export. */
    TABLE_SIZE_MISMATCH("Table size mismatch"),
    /** Table element type mismatch between import and export. */
    TABLE_TYPE_MISMATCH("Table element type mismatch"),
    /** Global type mismatch between import and export. */
    GLOBAL_TYPE_MISMATCH("Global type mismatch"),
    /** Global mutability mismatch between import and export. */
    GLOBAL_MUTABILITY_MISMATCH("Global mutability mismatch"),
    /** Circular dependency detected between modules. */
    CIRCULAR_DEPENDENCY("Circular dependency detected"),
    /** Module namespace conflict. */
    NAMESPACE_CONFLICT("Module namespace conflict"),
    /** Host function binding failed. */
    HOST_FUNCTION_BINDING_FAILED("Host function binding failed"),
    /** WASI import resolution failed. */
    WASI_IMPORT_FAILED("WASI import resolution failed"),
    /** Component model linking failed. */
    COMPONENT_LINKING_FAILED("Component model linking failed"),
    /** Interface type mismatch in component linking. */
    INTERFACE_TYPE_MISMATCH("Interface type mismatch"),
    /** Resource type linking failed. */
    RESOURCE_TYPE_LINKING_FAILED("Resource type linking failed"),
    /** Capability requirement not satisfied. */
    CAPABILITY_NOT_SATISFIED("Capability requirement not satisfied"),
    /** Linker configuration error. */
    LINKER_CONFIGURATION_ERROR("Linker configuration error"),
    /** Unknown linking error. */
    UNKNOWN("Unknown linking error");

    private final String description;

    LinkingErrorType(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this linking error type.
     *
     * @return the error type description
     */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Creates a new linking exception with the specified error type and message.
   *
   * @param errorType the specific linking error type
   * @param message the error message
   */
  public LinkingException(final LinkingErrorType errorType, final String message) {
    this(errorType, message, null, null, null, null, null);
  }

  /**
   * Creates a new linking exception with the specified error type, message, and cause.
   *
   * @param errorType the specific linking error type
   * @param message the error message
   * @param cause the underlying cause
   */
  public LinkingException(
      final LinkingErrorType errorType, final String message, final Throwable cause) {
    this(errorType, message, null, null, null, null, cause);
  }

  /**
   * Creates a new linking exception with detailed error information.
   *
   * @param errorType the specific linking error type
   * @param message the error message
   * @param moduleName module name where linking failed (may be null)
   * @param itemName import/export name that failed (may be null)
   * @param expectedType expected type signature (may be null)
   * @param actualType actual type signature (may be null)
   * @param cause the underlying cause (may be null)
   */
  public LinkingException(
      final LinkingErrorType errorType,
      final String message,
      final String moduleName,
      final String itemName,
      final String expectedType,
      final String actualType,
      final Throwable cause) {
    super(formatMessage(errorType, message, moduleName, itemName, expectedType, actualType), cause);
    this.errorType = errorType != null ? errorType : LinkingErrorType.UNKNOWN;
    this.moduleName = moduleName;
    this.itemName = itemName;
    this.expectedType = expectedType;
    this.actualType = actualType;
    this.recoverySuggestion = generateRecoverySuggestion(this.errorType);
  }

  /**
   * Gets the specific linking error type.
   *
   * @return the linking error type
   */
  public LinkingErrorType getErrorType() {
    return errorType;
  }

  /**
   * Gets the module name where linking failed.
   *
   * @return the module name, or null if not available
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Gets the import or export name that failed linking.
   *
   * @return the item name, or null if not available
   */
  public String getItemName() {
    return itemName;
  }

  /**
   * Gets the expected type signature.
   *
   * @return the expected type, or null if not available
   */
  public String getExpectedType() {
    return expectedType;
  }

  /**
   * Gets the actual type signature.
   *
   * @return the actual type, or null if not available
   */
  public String getActualType() {
    return actualType;
  }

  /**
   * Gets a recovery suggestion for this linking error.
   *
   * @return the recovery suggestion
   */
  public String getRecoverySuggestion() {
    return recoverySuggestion;
  }

  /**
   * Checks if this linking error is related to missing imports/exports.
   *
   * @return true if this is a missing item error, false otherwise
   */
  public boolean isMissingItemError() {
    return errorType == LinkingErrorType.IMPORT_NOT_FOUND
        || errorType == LinkingErrorType.EXPORT_NOT_FOUND;
  }

  /**
   * Checks if this linking error is related to type mismatches.
   *
   * @return true if this is a type mismatch error, false otherwise
   */
  public boolean isTypeMismatchError() {
    return errorType == LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH
        || errorType == LinkingErrorType.MEMORY_SIZE_MISMATCH
        || errorType == LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE
        || errorType == LinkingErrorType.TABLE_SIZE_MISMATCH
        || errorType == LinkingErrorType.TABLE_TYPE_MISMATCH
        || errorType == LinkingErrorType.GLOBAL_TYPE_MISMATCH
        || errorType == LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH
        || errorType == LinkingErrorType.INTERFACE_TYPE_MISMATCH;
  }

  /**
   * Checks if this linking error is related to host function binding.
   *
   * @return true if this is a host function error, false otherwise
   */
  public boolean isHostFunctionError() {
    return errorType == LinkingErrorType.HOST_FUNCTION_BINDING_FAILED
        || errorType == LinkingErrorType.WASI_IMPORT_FAILED;
  }

  /**
   * Checks if this linking error is related to component model features.
   *
   * @return true if this is a component error, false otherwise
   */
  public boolean isComponentError() {
    return errorType == LinkingErrorType.COMPONENT_LINKING_FAILED
        || errorType == LinkingErrorType.INTERFACE_TYPE_MISMATCH
        || errorType == LinkingErrorType.RESOURCE_TYPE_LINKING_FAILED
        || errorType == LinkingErrorType.CAPABILITY_NOT_SATISFIED;
  }

  /**
   * Checks if this linking error is related to configuration issues.
   *
   * @return true if this is a configuration error, false otherwise
   */
  public boolean isConfigurationError() {
    return errorType == LinkingErrorType.CIRCULAR_DEPENDENCY
        || errorType == LinkingErrorType.NAMESPACE_CONFLICT
        || errorType == LinkingErrorType.LINKER_CONFIGURATION_ERROR;
  }

  /**
   * Formats the exception message with linking error details.
   *
   * @param errorType the linking error type
   * @param message the base message
   * @param moduleName the module name
   * @param itemName the item name
   * @param expectedType the expected type
   * @param actualType the actual type
   * @return the formatted message
   */
  private static String formatMessage(
      final LinkingErrorType errorType,
      final String message,
      final String moduleName,
      final String itemName,
      final String expectedType,
      final String actualType) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder();

    if (errorType != null) {
      sb.append("[").append(errorType.name()).append("] ");
    }

    sb.append(message);

    if (moduleName != null && !moduleName.isEmpty() && itemName != null && !itemName.isEmpty()) {
      sb.append(" (").append(moduleName).append(".").append(itemName).append(")");
    } else if (itemName != null && !itemName.isEmpty()) {
      sb.append(" (").append(itemName).append(")");
    } else if (moduleName != null && !moduleName.isEmpty()) {
      sb.append(" (module: ").append(moduleName).append(")");
    }

    if (expectedType != null
        && !expectedType.isEmpty()
        && actualType != null
        && !actualType.isEmpty()) {
      sb.append(" (expected: ")
          .append(expectedType)
          .append(", actual: ")
          .append(actualType)
          .append(")");
    } else if (expectedType != null && !expectedType.isEmpty()) {
      sb.append(" (expected: ").append(expectedType).append(")");
    }

    return sb.toString();
  }

  /**
   * Generates a recovery suggestion based on the linking error type.
   *
   * @param errorType the linking error type
   * @return a recovery suggestion
   */
  private static String generateRecoverySuggestion(final LinkingErrorType errorType) {
    switch (errorType) {
      case IMPORT_NOT_FOUND:
        return "Ensure all required imports are provided through the linker";
      case EXPORT_NOT_FOUND:
        return "Verify export name exists in the target module";
      case FUNCTION_SIGNATURE_MISMATCH:
        return "Check function parameter and return types match exactly";
      case MEMORY_SIZE_MISMATCH:
        return "Ensure memory minimum size requirements are satisfied";
      case MEMORY_LIMITS_INCOMPATIBLE:
        return "Check memory maximum limits are compatible";
      case TABLE_SIZE_MISMATCH:
        return "Verify table minimum size requirements are satisfied";
      case TABLE_TYPE_MISMATCH:
        return "Ensure table element types match exactly";
      case GLOBAL_TYPE_MISMATCH:
        return "Check global variable types match between import and export";
      case GLOBAL_MUTABILITY_MISMATCH:
        return "Verify global variable mutability specifications match";
      case CIRCULAR_DEPENDENCY:
        return "Restructure modules to avoid circular import dependencies";
      case NAMESPACE_CONFLICT:
        return "Use unique module names to avoid namespace conflicts";
      case HOST_FUNCTION_BINDING_FAILED:
        return "Check host function implementation and signature compatibility";
      case WASI_IMPORT_FAILED:
        return "Ensure WASI support is enabled and properly configured";
      case COMPONENT_LINKING_FAILED:
        return "Review component interface definitions and implementations";
      case INTERFACE_TYPE_MISMATCH:
        return "Check WIT interface types match between components";
      case RESOURCE_TYPE_LINKING_FAILED:
        return "Verify resource type definitions are compatible";
      case CAPABILITY_NOT_SATISFIED:
        return "Ensure all required capabilities are provided";
      case LINKER_CONFIGURATION_ERROR:
        return "Review linker setup and module registration";
      case UNKNOWN:
      default:
        return "Check module compatibility and linker configuration";
    }
  }
}
