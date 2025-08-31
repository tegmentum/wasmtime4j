package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WASI resource management operations fail.
 *
 * <p>This exception is thrown when WASI resource operations fail, including:
 *
 * <ul>
 *   <li>Resource handle allocation failures
 *   <li>Resource cleanup and disposal errors
 *   <li>Resource lifetime management issues
 *   <li>Resource limit exceeded errors
 *   <li>Resource access permission errors
 * </ul>
 *
 * <p>Resource exceptions provide detailed context about resource management failures,
 * including resource types, handle identifiers, and cleanup guidance to prevent resource leaks.
 *
 * @since 1.0.0
 */
public class WasiResourceException extends WasiException {

  private static final long serialVersionUID = 1L;

  /** The resource type associated with this error. */
  private final ResourceType resourceType;

  /** The resource handle identifier if applicable. */
  private final String resourceHandle;

  /** Whether cleanup is required after this error. */
  private final boolean cleanupRequired;

  /** The resource operation that failed. */
  private final ResourceOperation resourceOperation;

  /**
   * WASI resource types for better error categorization and handling.
   */
  public enum ResourceType {
    /** File handle resource. */
    FILE,
    /** Directory handle resource. */
    DIRECTORY,
    /** Socket handle resource. */
    SOCKET,
    /** Memory resource. */
    MEMORY,
    /** Thread/execution context resource. */
    EXECUTION_CONTEXT,
    /** Timer resource. */
    TIMER,
    /** Event resource. */
    EVENT,
    /** Generic system resource. */
    SYSTEM
  }

  /**
   * Resource operations for better error context.
   */
  public enum ResourceOperation {
    /** Resource allocation operation. */
    ALLOCATION,
    /** Resource access operation. */
    ACCESS,
    /** Resource modification operation. */
    MODIFICATION,
    /** Resource cleanup/disposal operation. */
    CLEANUP,
    /** Resource lifetime management operation. */
    LIFETIME_MANAGEMENT
  }

  /**
   * Creates a new resource exception with the specified message.
   *
   * @param message the error message
   */
  public WasiResourceException(final String message) {
    super(message, "resource-operation", null, true, ErrorCategory.RESOURCE_LIMIT);
    this.resourceType = ResourceType.SYSTEM;
    this.resourceHandle = null;
    this.cleanupRequired = false;
    this.resourceOperation = ResourceOperation.ACCESS;
  }

  /**
   * Creates a new resource exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasiResourceException(final String message, final Throwable cause) {
    super(message, "resource-operation", null, true, ErrorCategory.RESOURCE_LIMIT, cause);
    this.resourceType = ResourceType.SYSTEM;
    this.resourceHandle = null;
    this.cleanupRequired = false;
    this.resourceOperation = ResourceOperation.ACCESS;
  }

  /**
   * Creates a new resource exception with resource-specific details.
   *
   * @param message the error message
   * @param resourceType the type of resource involved
   * @param resourceOperation the resource operation that failed
   */
  public WasiResourceException(
      final String message,
      final ResourceType resourceType,
      final ResourceOperation resourceOperation) {
    super(
        message,
        formatOperation(resourceOperation, resourceType),
        formatResourceIdentifier(resourceType, null),
        isRetryableOperation(resourceOperation),
        getErrorCategory(resourceType, resourceOperation));
    this.resourceType = resourceType;
    this.resourceHandle = null;
    this.cleanupRequired = requiresCleanup(resourceOperation);
    this.resourceOperation = resourceOperation;
  }

  /**
   * Creates a new resource exception with full resource details.
   *
   * @param message the error message
   * @param resourceType the type of resource involved
   * @param resourceHandle the resource handle identifier
   * @param resourceOperation the resource operation that failed
   */
  public WasiResourceException(
      final String message,
      final ResourceType resourceType,
      final String resourceHandle,
      final ResourceOperation resourceOperation) {
    super(
        message,
        formatOperation(resourceOperation, resourceType),
        formatResourceIdentifier(resourceType, resourceHandle),
        isRetryableOperation(resourceOperation),
        getErrorCategory(resourceType, resourceOperation));
    this.resourceType = resourceType;
    this.resourceHandle = resourceHandle;
    this.cleanupRequired = requiresCleanup(resourceOperation);
    this.resourceOperation = resourceOperation;
  }

  /**
   * Creates a new resource exception with full details and cause.
   *
   * @param message the error message
   * @param resourceType the type of resource involved
   * @param resourceHandle the resource handle identifier (may be null)
   * @param resourceOperation the resource operation that failed
   * @param cause the underlying cause
   */
  public WasiResourceException(
      final String message,
      final ResourceType resourceType,
      final String resourceHandle,
      final ResourceOperation resourceOperation,
      final Throwable cause) {
    super(
        message,
        formatOperation(resourceOperation, resourceType),
        formatResourceIdentifier(resourceType, resourceHandle),
        isRetryableOperation(resourceOperation),
        getErrorCategory(resourceType, resourceOperation),
        cause);
    this.resourceType = resourceType;
    this.resourceHandle = resourceHandle;
    this.cleanupRequired = requiresCleanup(resourceOperation);
    this.resourceOperation = resourceOperation;
  }

  /**
   * Gets the resource type associated with this error.
   *
   * @return the resource type
   */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /**
   * Gets the resource handle identifier if applicable.
   *
   * @return the resource handle, or null if not specified
   */
  public String getResourceHandle() {
    return resourceHandle;
  }

  /**
   * Checks if cleanup is required after this error.
   *
   * @return true if cleanup is required, false otherwise
   */
  public boolean isCleanupRequired() {
    return cleanupRequired;
  }

  /**
   * Gets the resource operation that failed.
   *
   * @return the resource operation
   */
  public ResourceOperation getResourceOperation() {
    return resourceOperation;
  }

  /**
   * Checks if this is a resource allocation error.
   *
   * @return true if this is an allocation error, false otherwise
   */
  public boolean isAllocationError() {
    return resourceOperation == ResourceOperation.ALLOCATION;
  }

  /**
   * Checks if this is a resource access error.
   *
   * @return true if this is an access error, false otherwise
   */
  public boolean isAccessError() {
    return resourceOperation == ResourceOperation.ACCESS;
  }

  /**
   * Checks if this is a resource cleanup error.
   *
   * @return true if this is a cleanup error, false otherwise
   */
  public boolean isCleanupError() {
    return resourceOperation == ResourceOperation.CLEANUP;
  }

  /**
   * Checks if this error involves a file resource.
   *
   * @return true if this is a file resource error, false otherwise
   */
  public boolean isFileResourceError() {
    return resourceType == ResourceType.FILE || resourceType == ResourceType.DIRECTORY;
  }

  /**
   * Checks if this error involves a network resource.
   *
   * @return true if this is a network resource error, false otherwise
   */
  public boolean isNetworkResourceError() {
    return resourceType == ResourceType.SOCKET;
  }

  /**
   * Checks if this error involves a memory resource.
   *
   * @return true if this is a memory resource error, false otherwise
   */
  public boolean isMemoryResourceError() {
    return resourceType == ResourceType.MEMORY;
  }

  /**
   * Formats the operation for display.
   *
   * @param operation the resource operation
   * @param resourceType the resource type
   * @return formatted operation string
   */
  private static String formatOperation(
      final ResourceOperation operation, final ResourceType resourceType) {
    if (operation == null && resourceType == null) {
      return "resource-operation";
    }

    final String operationStr = operation != null ? operation.name().toLowerCase().replace('_', '-') : "operation";
    final String resourceStr = resourceType != null ? resourceType.name().toLowerCase() : "resource";

    return resourceStr + "-" + operationStr;
  }

  /**
   * Formats the resource identifier combining type and handle.
   *
   * @param resourceType the resource type
   * @param resourceHandle the resource handle
   * @return formatted resource string
   */
  private static String formatResourceIdentifier(
      final ResourceType resourceType, final String resourceHandle) {
    if (resourceType == null && resourceHandle == null) {
      return null;
    }

    if (resourceHandle == null) {
      return resourceType != null ? resourceType.name().toLowerCase() : null;
    }

    if (resourceType == null) {
      return resourceHandle;
    }

    return resourceType.name().toLowerCase() + ":" + resourceHandle;
  }

  /**
   * Determines if a resource operation is retryable.
   *
   * @param operation the resource operation
   * @return true if retryable, false otherwise
   */
  private static boolean isRetryableOperation(final ResourceOperation operation) {
    if (operation == null) {
      return false;
    }

    switch (operation) {
      case ALLOCATION:
      case ACCESS:
        return true; // May succeed with retry or different parameters
      case MODIFICATION:
      case CLEANUP:
      case LIFETIME_MANAGEMENT:
        return false; // Structural issues that won't resolve with retry
      default:
        return false;
    }
  }

  /**
   * Determines the appropriate error category for the resource and operation.
   *
   * @param resourceType the resource type
   * @param operation the resource operation
   * @return the appropriate error category
   */
  private static ErrorCategory getErrorCategory(
      final ResourceType resourceType, final ResourceOperation operation) {
    if (resourceType == null) {
      return ErrorCategory.RESOURCE_LIMIT;
    }

    switch (resourceType) {
      case FILE:
      case DIRECTORY:
        return ErrorCategory.FILE_SYSTEM;
      case SOCKET:
        return ErrorCategory.NETWORK;
      case MEMORY:
      case EXECUTION_CONTEXT:
      case TIMER:
      case EVENT:
      case SYSTEM:
        return ErrorCategory.RESOURCE_LIMIT;
      default:
        return ErrorCategory.SYSTEM;
    }
  }

  /**
   * Determines if cleanup is required for the operation.
   *
   * @param operation the resource operation
   * @return true if cleanup is required, false otherwise
   */
  private static boolean requiresCleanup(final ResourceOperation operation) {
    if (operation == null) {
      return false;
    }

    switch (operation) {
      case ALLOCATION:
        return true; // Partial allocation may need cleanup
      case ACCESS:
      case MODIFICATION:
        return false; // Read-only or temporary operations
      case CLEANUP:
        return false; // Already a cleanup operation
      case LIFETIME_MANAGEMENT:
        return true; // Lifetime errors may leave resources in inconsistent state
      default:
        return false;
    }
  }
}