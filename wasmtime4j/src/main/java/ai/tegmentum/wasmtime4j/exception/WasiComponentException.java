package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WASI component model operations fail.
 *
 * <p>This exception is thrown when WASI component operations fail, including:
 *
 * <ul>
 *   <li>Component instantiation failures
 *   <li>Interface binding errors
 *   <li>Export/import resolution problems
 *   <li>Component linking issues
 *   <li>Component execution errors
 * </ul>
 *
 * <p>Component exceptions provide additional context specific to the WASI component model,
 * including component identifiers, interface names, and operation context.
 *
 * @since 1.0.0
 */
public class WasiComponentException extends WasiException {

  private static final long serialVersionUID = 1L;

  /** The component identifier associated with this error. */
  private final String componentId;

  /** The interface name if the error is interface-related. */
  private final String interfaceName;

  /** The component operation type that failed. */
  private final ComponentOperation operationType;

  /**
   * Component operation types for better error categorization.
   */
  public enum ComponentOperation {
    /** Component instantiation operation. */
    INSTANTIATION,
    /** Interface binding operation. */
    INTERFACE_BINDING,
    /** Export resolution operation. */
    EXPORT_RESOLUTION,
    /** Import resolution operation. */
    IMPORT_RESOLUTION,
    /** Component linking operation. */
    LINKING,
    /** Component execution operation. */
    EXECUTION,
    /** Component lifecycle management. */
    LIFECYCLE
  }

  /**
   * Creates a new component exception with the specified message.
   *
   * @param message the error message
   */
  public WasiComponentException(final String message) {
    super(message, "component-operation", null, false, ErrorCategory.COMPONENT);
    this.componentId = null;
    this.interfaceName = null;
    this.operationType = ComponentOperation.EXECUTION;
  }

  /**
   * Creates a new component exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasiComponentException(final String message, final Throwable cause) {
    super(message, "component-operation", null, false, ErrorCategory.COMPONENT, cause);
    this.componentId = null;
    this.interfaceName = null;
    this.operationType = ComponentOperation.EXECUTION;
  }

  /**
   * Creates a new component exception with component-specific details.
   *
   * @param message the error message
   * @param componentId the component identifier
   * @param operationType the component operation that failed
   */
  public WasiComponentException(
      final String message, final String componentId, final ComponentOperation operationType) {
    super(
        message,
        formatOperation(operationType),
        componentId,
        isRetryableOperation(operationType),
        ErrorCategory.COMPONENT);
    this.componentId = componentId;
    this.interfaceName = null;
    this.operationType = operationType;
  }

  /**
   * Creates a new component exception with interface-specific details.
   *
   * @param message the error message
   * @param componentId the component identifier
   * @param interfaceName the interface name
   * @param operationType the component operation that failed
   */
  public WasiComponentException(
      final String message,
      final String componentId,
      final String interfaceName,
      final ComponentOperation operationType) {
    super(
        message,
        formatOperation(operationType),
        formatResource(componentId, interfaceName),
        isRetryableOperation(operationType),
        ErrorCategory.COMPONENT);
    this.componentId = componentId;
    this.interfaceName = interfaceName;
    this.operationType = operationType;
  }

  /**
   * Creates a new component exception with full details and cause.
   *
   * @param message the error message
   * @param componentId the component identifier
   * @param interfaceName the interface name (may be null)
   * @param operationType the component operation that failed
   * @param cause the underlying cause
   */
  public WasiComponentException(
      final String message,
      final String componentId,
      final String interfaceName,
      final ComponentOperation operationType,
      final Throwable cause) {
    super(
        message,
        formatOperation(operationType),
        formatResource(componentId, interfaceName),
        isRetryableOperation(operationType),
        ErrorCategory.COMPONENT,
        cause);
    this.componentId = componentId;
    this.interfaceName = interfaceName;
    this.operationType = operationType;
  }

  /**
   * Gets the component identifier associated with this error.
   *
   * @return the component identifier, or null if not specified
   */
  public String getComponentId() {
    return componentId;
  }

  /**
   * Gets the interface name if the error is interface-related.
   *
   * @return the interface name, or null if not applicable
   */
  public String getInterfaceName() {
    return interfaceName;
  }

  /**
   * Gets the component operation type that failed.
   *
   * @return the operation type
   */
  public ComponentOperation getOperationType() {
    return operationType;
  }

  /**
   * Checks if this is an instantiation error.
   *
   * @return true if this is an instantiation error, false otherwise
   */
  public boolean isInstantiationError() {
    return operationType == ComponentOperation.INSTANTIATION;
  }

  /**
   * Checks if this is an interface binding error.
   *
   * @return true if this is an interface binding error, false otherwise
   */
  public boolean isInterfaceBindingError() {
    return operationType == ComponentOperation.INTERFACE_BINDING;
  }

  /**
   * Checks if this is an import/export resolution error.
   *
   * @return true if this is a resolution error, false otherwise
   */
  public boolean isResolutionError() {
    return operationType == ComponentOperation.EXPORT_RESOLUTION
        || operationType == ComponentOperation.IMPORT_RESOLUTION;
  }

  /**
   * Checks if this is a linking error.
   *
   * @return true if this is a linking error, false otherwise
   */
  public boolean isLinkingError() {
    return operationType == ComponentOperation.LINKING;
  }

  /**
   * Checks if this is an execution error.
   *
   * @return true if this is an execution error, false otherwise
   */
  public boolean isExecutionError() {
    return operationType == ComponentOperation.EXECUTION;
  }

  /**
   * Formats the operation type for display.
   *
   * @param operationType the operation type
   * @return formatted operation string
   */
  private static String formatOperation(final ComponentOperation operationType) {
    if (operationType == null) {
      return "component-operation";
    }

    switch (operationType) {
      case INSTANTIATION:
        return "component-instantiation";
      case INTERFACE_BINDING:
        return "interface-binding";
      case EXPORT_RESOLUTION:
        return "export-resolution";
      case IMPORT_RESOLUTION:
        return "import-resolution";
      case LINKING:
        return "component-linking";
      case EXECUTION:
        return "component-execution";
      case LIFECYCLE:
        return "component-lifecycle";
      default:
        return "component-operation";
    }
  }

  /**
   * Formats the resource identifier combining component and interface.
   *
   * @param componentId the component identifier
   * @param interfaceName the interface name
   * @return formatted resource string
   */
  private static String formatResource(final String componentId, final String interfaceName) {
    if (componentId == null && interfaceName == null) {
      return null;
    }

    if (interfaceName == null) {
      return componentId;
    }

    if (componentId == null) {
      return "interface:" + interfaceName;
    }

    return componentId + ":" + interfaceName;
  }

  /**
   * Determines if an operation type is retryable.
   *
   * @param operationType the operation type
   * @return true if retryable, false otherwise
   */
  private static boolean isRetryableOperation(final ComponentOperation operationType) {
    if (operationType == null) {
      return false;
    }

    switch (operationType) {
      case INSTANTIATION:
      case EXECUTION:
        return true; // May be retryable depending on the specific error
      case INTERFACE_BINDING:
      case EXPORT_RESOLUTION:
      case IMPORT_RESOLUTION:
      case LINKING:
      case LIFECYCLE:
        return false; // Configuration/structural errors are typically not retryable
      default:
        return false;
    }
  }
}