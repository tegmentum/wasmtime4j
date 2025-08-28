package ai.tegmentum.wasmtime4j.jni.wasi.exception;

/**
 * WASI exception for permission and security violations.
 *
 * <p>This exception is thrown when WASI operations are denied due to security policies or
 * permission restrictions, including:
 *
 * <ul>
 *   <li>Sandbox boundary violations
 *   <li>Unauthorized file system access
 *   <li>Environment variable access denied
 *   <li>Dangerous operation attempts
 *   <li>Resource limit exceeded
 * </ul>
 *
 * <p>The exception provides specific context about the security violation and the attempted
 * operation for proper error handling and logging.
 *
 * @since 1.0.0
 */
public final class WasiPermissionException extends WasiException {

  private static final long serialVersionUID = 1L;

  /** The type of permission violation. */
  private final PermissionViolationType violationType;

  /** The attempted resource or operation. */
  private final String attemptedResource;

  /** The security policy that was violated. */
  private final String violatedPolicy;

  /**
   * Creates a new WASI permission exception.
   *
   * @param message the error message
   * @param violationType the type of permission violation
   * @param operation the operation that was denied
   * @param resource the resource that was accessed
   */
  public WasiPermissionException(
      final String message,
      final PermissionViolationType violationType,
      final String operation,
      final String resource) {
    super(message, WasiErrorCode.EPERM, operation, resource);
    this.violationType = violationType;
    this.attemptedResource = resource;
    this.violatedPolicy = null;
  }

  /**
   * Creates a new WASI permission exception with policy information.
   *
   * @param message the error message
   * @param violationType the type of permission violation
   * @param operation the operation that was denied
   * @param resource the resource that was accessed
   * @param violatedPolicy the security policy that was violated
   */
  public WasiPermissionException(
      final String message,
      final PermissionViolationType violationType,
      final String operation,
      final String resource,
      final String violatedPolicy) {
    super(message, WasiErrorCode.EPERM, operation, resource);
    this.violationType = violationType;
    this.attemptedResource = resource;
    this.violatedPolicy = violatedPolicy;
  }

  /**
   * Creates a new WASI permission exception with access denied error code.
   *
   * @param message the error message
   * @param violationType the type of permission violation
   * @param operation the operation that was denied
   * @param resource the resource that was accessed
   */
  public WasiPermissionException(
      final String message,
      final PermissionViolationType violationType,
      final String operation,
      final String resource,
      final boolean useAccessDeniedCode) {
    super(
        message,
        useAccessDeniedCode ? WasiErrorCode.EACCES : WasiErrorCode.EPERM,
        operation,
        resource);
    this.violationType = violationType;
    this.attemptedResource = resource;
    this.violatedPolicy = null;
  }

  /**
   * Gets the type of permission violation.
   *
   * @return the permission violation type
   */
  public PermissionViolationType getViolationType() {
    return violationType;
  }

  /**
   * Gets the attempted resource or operation.
   *
   * @return the attempted resource
   */
  public String getAttemptedResource() {
    return attemptedResource;
  }

  /**
   * Gets the security policy that was violated.
   *
   * @return the violated policy, or null if not specified
   */
  public String getViolatedPolicy() {
    return violatedPolicy;
  }

  /**
   * Checks if this violation involves file system access.
   *
   * @return true if this is a file system access violation, false otherwise
   */
  public boolean isFileSystemViolation() {
    return violationType == PermissionViolationType.FILE_SYSTEM_ACCESS
        || violationType == PermissionViolationType.SANDBOX_ESCAPE
        || violationType == PermissionViolationType.PATH_TRAVERSAL;
  }

  /**
   * Checks if this violation involves dangerous operations.
   *
   * @return true if this is a dangerous operation violation, false otherwise
   */
  public boolean isDangerousOperationViolation() {
    return violationType == PermissionViolationType.DANGEROUS_OPERATION;
  }

  /**
   * Checks if this violation involves resource limits.
   *
   * @return true if this is a resource limit violation, false otherwise
   */
  public boolean isResourceLimitViolation() {
    return violationType == PermissionViolationType.RESOURCE_LIMIT_EXCEEDED;
  }

  /**
   * Factory method for file system access denied errors.
   *
   * @param operation the operation that was denied
   * @param filePath the file path that was denied access
   * @return a new file system access denied exception
   */
  public static WasiPermissionException fileSystemAccessDenied(
      final String operation, final String filePath) {
    return new WasiPermissionException(
        String.format("File system access denied: %s", filePath),
        PermissionViolationType.FILE_SYSTEM_ACCESS,
        operation,
        filePath,
        true);
  }

  /**
   * Factory method for sandbox escape attempts.
   *
   * @param operation the operation that attempted to escape
   * @param filePath the file path that would escape the sandbox
   * @return a new sandbox escape exception
   */
  public static WasiPermissionException sandboxEscape(
      final String operation, final String filePath) {
    return new WasiPermissionException(
        String.format("Sandbox escape attempt detected: %s", filePath),
        PermissionViolationType.SANDBOX_ESCAPE,
        operation,
        filePath);
  }

  /**
   * Factory method for path traversal attacks.
   *
   * @param operation the operation that attempted path traversal
   * @param filePath the file path with traversal attempt
   * @return a new path traversal exception
   */
  public static WasiPermissionException pathTraversal(
      final String operation, final String filePath) {
    return new WasiPermissionException(
        String.format("Path traversal attack detected: %s", filePath),
        PermissionViolationType.PATH_TRAVERSAL,
        operation,
        filePath);
  }

  /**
   * Factory method for environment variable access denied errors.
   *
   * @param operation the operation that was denied
   * @param variableName the environment variable that was denied access
   * @return a new environment access denied exception
   */
  public static WasiPermissionException environmentAccessDenied(
      final String operation, final String variableName) {
    return new WasiPermissionException(
        String.format("Environment variable access denied: %s", variableName),
        PermissionViolationType.ENVIRONMENT_ACCESS,
        operation,
        variableName,
        true);
  }

  /**
   * Factory method for dangerous operation attempts.
   *
   * @param operation the dangerous operation that was attempted
   * @param resource the resource involved in the dangerous operation
   * @return a new dangerous operation exception
   */
  public static WasiPermissionException dangerousOperation(
      final String operation, final String resource) {
    return new WasiPermissionException(
        String.format("Dangerous operation not permitted: %s", operation),
        PermissionViolationType.DANGEROUS_OPERATION,
        operation,
        resource);
  }

  /**
   * Factory method for resource limit exceeded errors.
   *
   * @param operation the operation that exceeded limits
   * @param resourceType the type of resource that was exceeded
   * @param limit the limit that was exceeded
   * @return a new resource limit exceeded exception
   */
  public static WasiPermissionException resourceLimitExceeded(
      final String operation, final String resourceType, final String limit) {
    return new WasiPermissionException(
        String.format("Resource limit exceeded for %s: %s", resourceType, limit),
        PermissionViolationType.RESOURCE_LIMIT_EXCEEDED,
        operation,
        resourceType,
        limit);
  }

  /**
   * Factory method for capability not granted errors.
   *
   * @param operation the operation that requires capability
   * @param capability the capability that was not granted
   * @return a new capability not granted exception
   */
  public static WasiPermissionException capabilityNotGranted(
      final String operation, final String capability) {
    return new WasiPermissionException(
        String.format("Required capability not granted: %s", capability),
        PermissionViolationType.CAPABILITY_NOT_GRANTED,
        operation,
        capability);
  }

  /** Enumeration of permission violation types. */
  public enum PermissionViolationType {

    /** File system access violation. */
    FILE_SYSTEM_ACCESS("File system access denied"),

    /** Sandbox boundary escape attempt. */
    SANDBOX_ESCAPE("Sandbox escape attempt"),

    /** Path traversal attack attempt. */
    PATH_TRAVERSAL("Path traversal attack"),

    /** Environment variable access denied. */
    ENVIRONMENT_ACCESS("Environment variable access denied"),

    /** Dangerous operation attempt. */
    DANGEROUS_OPERATION("Dangerous operation attempt"),

    /** Resource limit exceeded. */
    RESOURCE_LIMIT_EXCEEDED("Resource limit exceeded"),

    /** Required capability not granted. */
    CAPABILITY_NOT_GRANTED("Capability not granted"),

    /** Security policy violation. */
    SECURITY_POLICY_VIOLATION("Security policy violation"),

    /** Unknown permission violation. */
    UNKNOWN("Unknown permission violation");

    /** Description of the violation type. */
    private final String description;

    /**
     * Creates a new permission violation type.
     *
     * @param description the description of the violation
     */
    PermissionViolationType(final String description) {
      this.description = description;
    }

    /**
     * Gets the description of this violation type.
     *
     * @return the violation description
     */
    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }
  }
}
