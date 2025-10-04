package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.exception.RuntimeException;

/**
 * Exception thrown by WebAssembly GC operations.
 *
 * <p>Indicates errors in garbage collection operations such as invalid type casts, field access
 * violations, or heap management failures. Provides specific error categories and context
 * information for debugging.
 *
 * @since 1.0.0
 */
public class GcException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Error code for categorizing GC errors. */
  public enum ErrorCode {
    /** Type validation failed. */
    TYPE_VALIDATION_ERROR,
    /** Object allocation failed. */
    ALLOCATION_ERROR,
    /** Field/element access failed. */
    ACCESS_ERROR,
    /** Type casting failed. */
    CAST_ERROR,
    /** Reference operation failed. */
    REFERENCE_ERROR,
    /** Memory management error. */
    MEMORY_ERROR,
    /** GC collection error. */
    COLLECTION_ERROR,
    /** Runtime internal error. */
    INTERNAL_ERROR
  }

  private final ErrorCode errorCode;
  private final String operation;
  private final transient Object context;

  /**
   * Creates a new GC exception with the specified message.
   *
   * @param message the error message
   */
  public GcException(final String message) {
    super(message);
    this.errorCode = ErrorCode.INTERNAL_ERROR;
    this.operation = null;
    this.context = null;
  }

  /**
   * Creates a new GC exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public GcException(final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = ErrorCode.INTERNAL_ERROR;
    this.operation = null;
    this.context = null;
  }

  /**
   * Creates a new GC exception with the specified cause.
   *
   * @param cause the underlying cause
   */
  public GcException(final Throwable cause) {
    super(cause != null ? cause.getMessage() : "GC operation failed", cause);
    this.errorCode = ErrorCode.INTERNAL_ERROR;
    this.operation = null;
    this.context = null;
  }

  /**
   * Creates a new GC exception with detailed error information.
   *
   * @param message the exception message
   * @param errorCode the error code
   * @param operation the operation that failed
   * @param context additional context information
   */
  public GcException(
      final String message,
      final ErrorCode errorCode,
      final String operation,
      final Object context) {
    super(message);
    this.errorCode = errorCode;
    this.operation = operation;
    this.context = context;
  }

  /**
   * Creates a new GC exception with detailed error information and cause.
   *
   * @param message the exception message
   * @param cause the underlying cause
   * @param errorCode the error code
   * @param operation the operation that failed
   * @param context additional context information
   */
  public GcException(
      final String message,
      final Throwable cause,
      final ErrorCode errorCode,
      final String operation,
      final Object context) {
    super(message, cause);
    this.errorCode = errorCode;
    this.operation = operation;
    this.context = context;
  }

  /**
   * Gets the error code.
   *
   * @return the error code
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Gets the operation that failed.
   *
   * @return the operation name, or null if not specified
   */
  public String getOperation() {
    return operation;
  }

  /**
   * Gets additional context information.
   *
   * @return the context object, or null if not specified
   */
  public Object getContext() {
    return context;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append(": ").append(getMessage());

    if (errorCode != null) {
      sb.append(" [").append(errorCode).append("]");
    }

    if (operation != null) {
      sb.append(" (operation: ").append(operation).append(")");
    }

    if (context != null) {
      sb.append(" (context: ").append(context).append(")");
    }

    return sb.toString();
  }

  /** Exception thrown when type validation fails. */
  public static class TypeValidationException extends GcException {
    private static final long serialVersionUID = 1L;

    public TypeValidationException(
        final String message, final String operation, final Object context) {
      super(message, ErrorCode.TYPE_VALIDATION_ERROR, operation, context);
    }

    public TypeValidationException(
        final String message, final Throwable cause, final String operation, final Object context) {
      super(message, cause, ErrorCode.TYPE_VALIDATION_ERROR, operation, context);
    }
  }

  /** Exception thrown when object allocation fails. */
  public static class AllocationException extends GcException {
    private static final long serialVersionUID = 1L;

    public AllocationException(final String message, final String operation, final Object context) {
      super(message, ErrorCode.ALLOCATION_ERROR, operation, context);
    }

    public AllocationException(
        final String message, final Throwable cause, final String operation, final Object context) {
      super(message, cause, ErrorCode.ALLOCATION_ERROR, operation, context);
    }
  }

  /** Exception thrown when field or element access fails. */
  public static class AccessException extends GcException {
    private static final long serialVersionUID = 1L;

    public AccessException(final String message, final String operation, final Object context) {
      super(message, ErrorCode.ACCESS_ERROR, operation, context);
    }

    public AccessException(
        final String message, final Throwable cause, final String operation, final Object context) {
      super(message, cause, ErrorCode.ACCESS_ERROR, operation, context);
    }
  }

  /** Exception thrown when reference casting fails. */
  public static class CastException extends GcException {
    private static final long serialVersionUID = 1L;

    public CastException(final String message, final String operation, final Object context) {
      super(message, ErrorCode.CAST_ERROR, operation, context);
    }

    public CastException(
        final String message, final Throwable cause, final String operation, final Object context) {
      super(message, cause, ErrorCode.CAST_ERROR, operation, context);
    }
  }

  /** Exception thrown when reference operations fail. */
  public static class ReferenceException extends GcException {
    private static final long serialVersionUID = 1L;

    public ReferenceException(final String message, final String operation, final Object context) {
      super(message, ErrorCode.REFERENCE_ERROR, operation, context);
    }

    public ReferenceException(
        final String message, final Throwable cause, final String operation, final Object context) {
      super(message, cause, ErrorCode.REFERENCE_ERROR, operation, context);
    }
  }

  /** Exception thrown when memory management fails. */
  public static class MemoryException extends GcException {
    private static final long serialVersionUID = 1L;

    public MemoryException(final String message, final String operation, final Object context) {
      super(message, ErrorCode.MEMORY_ERROR, operation, context);
    }

    public MemoryException(
        final String message, final Throwable cause, final String operation, final Object context) {
      super(message, cause, ErrorCode.MEMORY_ERROR, operation, context);
    }
  }

  /** Exception thrown when garbage collection fails. */
  public static class CollectionException extends GcException {
    private static final long serialVersionUID = 1L;

    public CollectionException(final String message, final String operation, final Object context) {
      super(message, ErrorCode.COLLECTION_ERROR, operation, context);
    }

    public CollectionException(
        final String message, final Throwable cause, final String operation, final Object context) {
      super(message, cause, ErrorCode.COLLECTION_ERROR, operation, context);
    }
  }
}
