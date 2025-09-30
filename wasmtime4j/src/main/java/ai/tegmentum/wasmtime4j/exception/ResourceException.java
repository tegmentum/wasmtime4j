package ai.tegmentum.wasmtime4j.exception;

/**
 * Base exception for resource management errors in WebAssembly operations.
 *
 * <p>This exception is thrown when resource management operations fail, including:
 *
 * <ul>
 *   <li>Memory allocation and deallocation failures
 *   <li>File handle and resource handle management errors
 *   <li>Resource limit exceeded conditions
 *   <li>Resource leak detection and cleanup failures
 *   <li>Native resource management errors
 * </ul>
 *
 * <p>ResourceException provides a unified interface for resource-related errors across JNI and
 * Panama implementations, enabling consistent error handling and recovery strategies for resource
 * management failures.
 *
 * @since 1.0.0
 */
public class ResourceException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The type of resource that encountered the error. */
  private final String resourceType;

  /** The resource identifier (handle, path, etc.) if available. */
  private final String resourceId;

  /** Whether automatic cleanup is recommended after this error. */
  private final boolean cleanupRecommended;

  /**
   * Creates a new resource exception with the specified message.
   *
   * @param message the error message describing the resource failure
   */
  public ResourceException(final String message) {
    super(message);
    this.resourceType = null;
    this.resourceId = null;
    this.cleanupRecommended = false;
  }

  /**
   * Creates a new resource exception with the specified message and cause.
   *
   * @param message the error message describing the resource failure
   * @param cause the underlying cause
   */
  public ResourceException(final String message, final Throwable cause) {
    super(message, cause);
    this.resourceType = null;
    this.resourceId = null;
    this.cleanupRecommended = false;
  }

  /**
   * Creates a new resource exception with resource-specific details.
   *
   * @param message the error message describing the resource failure
   * @param resourceType the type of resource that failed (memory, file, handle, etc.)
   * @param resourceId the resource identifier if available
   */
  public ResourceException(
      final String message, final String resourceType, final String resourceId) {
    super(message);
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.cleanupRecommended = true;
  }

  /**
   * Creates a new resource exception with full details and cause.
   *
   * @param message the error message describing the resource failure
   * @param resourceType the type of resource that failed
   * @param resourceId the resource identifier if available
   * @param cleanupRecommended whether cleanup is recommended
   * @param cause the underlying cause
   */
  public ResourceException(
      final String message,
      final String resourceType,
      final String resourceId,
      final boolean cleanupRecommended,
      final Throwable cause) {
    super(message, cause);
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.cleanupRecommended = cleanupRecommended;
  }

  /**
   * Gets the type of resource that encountered the error.
   *
   * @return the resource type, or null if not specified
   */
  public String getResourceType() {
    return resourceType;
  }

  /**
   * Gets the resource identifier (handle, path, etc.) if available.
   *
   * @return the resource identifier, or null if not available
   */
  public String getResourceId() {
    return resourceId;
  }

  /**
   * Checks if automatic cleanup is recommended after this error.
   *
   * <p>When true, applications should attempt to clean up related resources to prevent leaks.
   *
   * @return true if cleanup is recommended, false otherwise
   */
  public boolean isCleanupRecommended() {
    return cleanupRecommended;
  }

  /**
   * Checks if this is a memory-related resource error.
   *
   * @return true if this is a memory resource error, false otherwise
   */
  public boolean isMemoryResourceError() {
    return resourceType != null && resourceType.toLowerCase().contains("memory");
  }

  /**
   * Checks if this is a handle-related resource error.
   *
   * @return true if this is a handle resource error, false otherwise
   */
  public boolean isHandleResourceError() {
    return resourceType != null && resourceType.toLowerCase().contains("handle");
  }

  /**
   * Provides a formatted description of the resource error for logging and debugging.
   *
   * @return formatted resource error description
   */
  public String getResourceErrorDescription() {
    final StringBuilder desc = new StringBuilder("Resource error");

    if (resourceType != null) {
      desc.append(" [type: ").append(resourceType).append("]");
    }

    if (resourceId != null) {
      desc.append(" [id: ").append(resourceId).append("]");
    }

    if (cleanupRecommended) {
      desc.append(" [cleanup recommended]");
    }

    return desc.toString();
  }
}
