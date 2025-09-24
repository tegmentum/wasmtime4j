package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when complex parameter marshaling operations fail.
 *
 * <p>This exception provides detailed information about marshaling failures, including the specific
 * operation that failed, the object type involved, and recovery suggestions where applicable.
 *
 * @since 1.0.0
 */
public class MarshalingException extends WasmException {

  /** Enumeration of marshaling operation types for better error classification. */
  public enum OperationType {
    /** Object serialization operation */
    SERIALIZATION,
    /** Object deserialization operation */
    DESERIALIZATION,
    /** Type conversion operation */
    TYPE_CONVERSION,
    /** Memory allocation operation */
    MEMORY_ALLOCATION,
    /** Memory layout operation */
    MEMORY_LAYOUT,
    /** Array marshaling operation */
    ARRAY_MARSHALING,
    /** Collection marshaling operation */
    COLLECTION_MARSHALING,
    /** Object graph traversal operation */
    OBJECT_GRAPH_TRAVERSAL,
    /** Circular reference detection */
    CIRCULAR_REFERENCE_DETECTION,
    /** Strategy selection operation */
    STRATEGY_SELECTION
  }

  private final OperationType operationType;
  private final String objectTypeName;
  private final long estimatedSize;
  private final String recoveryHint;

  /**
   * Creates a new marshaling exception with the specified details.
   *
   * @param message the error message
   * @param operationType the type of operation that failed
   * @param objectTypeName the name of the object type being marshaled
   * @param estimatedSize the estimated size of the object (if known)
   * @param recoveryHint suggestion for recovering from this error
   */
  public MarshalingException(
      final String message,
      final OperationType operationType,
      final String objectTypeName,
      final long estimatedSize,
      final String recoveryHint) {
    super(formatMessage(message, operationType, objectTypeName, estimatedSize, recoveryHint));
    this.operationType = operationType;
    this.objectTypeName = objectTypeName;
    this.estimatedSize = estimatedSize;
    this.recoveryHint = recoveryHint;
  }

  /**
   * Creates a new marshaling exception with the specified details and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param operationType the type of operation that failed
   * @param objectTypeName the name of the object type being marshaled
   * @param estimatedSize the estimated size of the object (if known)
   * @param recoveryHint suggestion for recovering from this error
   */
  public MarshalingException(
      final String message,
      final Throwable cause,
      final OperationType operationType,
      final String objectTypeName,
      final long estimatedSize,
      final String recoveryHint) {
    super(
        formatMessage(message, operationType, objectTypeName, estimatedSize, recoveryHint), cause);
    this.operationType = operationType;
    this.objectTypeName = objectTypeName;
    this.estimatedSize = estimatedSize;
    this.recoveryHint = recoveryHint;
  }

  /**
   * Creates a marshaling exception for serialization failures.
   *
   * @param objectTypeName the type being serialized
   * @param cause the underlying cause
   * @return a new MarshalingException
   */
  public static MarshalingException serializationFailure(
      final String objectTypeName, final Throwable cause) {
    return new MarshalingException(
        "Failed to serialize object",
        cause,
        OperationType.SERIALIZATION,
        objectTypeName,
        -1,
        "Ensure the object implements Serializable or use custom marshaling");
  }

  /**
   * Creates a marshaling exception for deserialization failures.
   *
   * @param objectTypeName the type being deserialized
   * @param cause the underlying cause
   * @return a new MarshalingException
   */
  public static MarshalingException deserializationFailure(
      final String objectTypeName, final Throwable cause) {
    return new MarshalingException(
        "Failed to deserialize object",
        cause,
        OperationType.DESERIALIZATION,
        objectTypeName,
        -1,
        "Verify the marshaled data format and class compatibility");
  }

  /**
   * Creates a marshaling exception for type conversion failures.
   *
   * @param sourceType the source type
   * @param targetType the target type
   * @return a new MarshalingException
   */
  public static MarshalingException typeConversionFailure(
      final String sourceType, final String targetType) {
    return new MarshalingException(
        "Failed to convert type",
        OperationType.TYPE_CONVERSION,
        sourceType + " -> " + targetType,
        -1,
        "Use compatible types or implement custom type converters");
  }

  /**
   * Creates a marshaling exception for memory allocation failures.
   *
   * @param requestedSize the requested memory size
   * @param cause the underlying cause
   * @return a new MarshalingException
   */
  public static MarshalingException memoryAllocationFailure(
      final long requestedSize, final Throwable cause) {
    return new MarshalingException(
        "Failed to allocate memory",
        cause,
        OperationType.MEMORY_ALLOCATION,
        "Memory",
        requestedSize,
        "Reduce object size or increase available memory");
  }

  /**
   * Creates a marshaling exception for array marshaling failures.
   *
   * @param arrayType the array type
   * @param arraySize the array size
   * @param cause the underlying cause
   * @return a new MarshalingException
   */
  public static MarshalingException arrayMarshalingFailure(
      final String arrayType, final long arraySize, final Throwable cause) {
    return new MarshalingException(
        "Failed to marshal array",
        cause,
        OperationType.ARRAY_MARSHALING,
        arrayType,
        arraySize,
        "Use smaller arrays or enable memory-based marshaling");
  }

  /**
   * Creates a marshaling exception for collection marshaling failures.
   *
   * @param collectionType the collection type
   * @param collectionSize the collection size
   * @param cause the underlying cause
   * @return a new MarshalingException
   */
  public static MarshalingException collectionMarshalingFailure(
      final String collectionType, final long collectionSize, final Throwable cause) {
    return new MarshalingException(
        "Failed to marshal collection",
        cause,
        OperationType.COLLECTION_MARSHALING,
        collectionType,
        collectionSize,
        "Reduce collection size or use streaming marshaling");
  }

  /**
   * Creates a marshaling exception for circular reference detection.
   *
   * @param objectType the object type with circular reference
   * @return a new MarshalingException
   */
  public static MarshalingException circularReferenceDetected(final String objectType) {
    return new MarshalingException(
        "Circular reference detected in object graph",
        OperationType.CIRCULAR_REFERENCE_DETECTION,
        objectType,
        -1,
        "Break circular references or disable circular reference detection");
  }

  /**
   * Creates a marshaling exception for object size limitations.
   *
   * @param objectType the object type
   * @param actualSize the actual object size
   * @param maxAllowedSize the maximum allowed size
   * @return a new MarshalingException
   */
  public static MarshalingException objectTooLarge(
      final String objectType, final long actualSize, final long maxAllowedSize) {
    return new MarshalingException(
        String.format(
            "Object size (%d bytes) exceeds maximum allowed (%d bytes)",
            actualSize, maxAllowedSize),
        OperationType.STRATEGY_SELECTION,
        objectType,
        actualSize,
        "Use streaming marshaling or break object into smaller parts");
  }

  /**
   * Creates a marshaling exception for unsupported object types.
   *
   * @param objectType the unsupported object type
   * @return a new MarshalingException
   */
  public static MarshalingException unsupportedObjectType(final String objectType) {
    return new MarshalingException(
        "Object type not supported for marshaling",
        OperationType.TYPE_CONVERSION,
        objectType,
        -1,
        "Use supported types or implement custom marshaling logic");
  }

  /**
   * Gets the operation type that failed.
   *
   * @return the operation type
   */
  public OperationType getOperationType() {
    return operationType;
  }

  /**
   * Gets the name of the object type being marshaled.
   *
   * @return the object type name
   */
  public String getObjectTypeName() {
    return objectTypeName;
  }

  /**
   * Gets the estimated size of the object being marshaled.
   *
   * @return the estimated size in bytes, or -1 if unknown
   */
  public long getEstimatedSize() {
    return estimatedSize;
  }

  /**
   * Gets the recovery hint for this error.
   *
   * @return the recovery hint
   */
  public String getRecoveryHint() {
    return recoveryHint;
  }

  /**
   * Checks if this exception is recoverable.
   *
   * @return true if the operation might succeed with different parameters
   */
  public boolean isRecoverable() {
    return switch (operationType) {
      case MEMORY_ALLOCATION, OBJECT_GRAPH_TRAVERSAL, STRATEGY_SELECTION -> true;
      case CIRCULAR_REFERENCE_DETECTION -> true; // Can disable detection
      case TYPE_CONVERSION, SERIALIZATION, DESERIALIZATION -> false; // Generally not recoverable
      case ARRAY_MARSHALING, COLLECTION_MARSHALING -> true; // Can use different strategy
      case MEMORY_LAYOUT -> false; // Layout issues are typically not recoverable
    };
  }

  /**
   * Gets suggested retry strategies for recoverable exceptions.
   *
   * @return array of suggested retry strategies
   */
  public String[] getRetryStrategies() {
    if (!isRecoverable()) {
      return new String[0];
    }

    return switch (operationType) {
      case MEMORY_ALLOCATION -> new String[] {
        "Reduce object size", "Use memory-based marshaling", "Increase available memory"
      };
      case OBJECT_GRAPH_TRAVERSAL -> new String[] {
        "Reduce object graph depth",
        "Break complex objects into simpler parts",
        "Use custom serialization"
      };
      case CIRCULAR_REFERENCE_DETECTION -> new String[] {
        "Disable circular reference detection",
        "Break circular references manually",
        "Use object references instead of embedded objects"
      };
      case STRATEGY_SELECTION -> new String[] {
        "Use memory-based marshaling",
        "Enable streaming marshaling",
        "Break object into smaller chunks"
      };
      case ARRAY_MARSHALING, COLLECTION_MARSHALING -> new String[] {
        "Use smaller data structures",
        "Enable memory-based marshaling",
        "Use lazy loading for large collections"
      };
      default -> new String[0];
    };
  }

  /**
   * Formats the error message with contextual information.
   *
   * @param message the base message
   * @param operationType the operation type
   * @param objectTypeName the object type name
   * @param estimatedSize the estimated size
   * @param recoveryHint the recovery hint
   * @return the formatted message
   */
  private static String formatMessage(
      final String message,
      final OperationType operationType,
      final String objectTypeName,
      final long estimatedSize,
      final String recoveryHint) {
    final StringBuilder sb = new StringBuilder();
    sb.append(message);

    if (operationType != null) {
      sb.append(" [Operation: ").append(operationType).append("]");
    }

    if (objectTypeName != null && !objectTypeName.isEmpty()) {
      sb.append(" [Type: ").append(objectTypeName).append("]");
    }

    if (estimatedSize >= 0) {
      sb.append(" [Size: ").append(estimatedSize).append(" bytes]");
    }

    if (recoveryHint != null && !recoveryHint.isEmpty()) {
      sb.append(" [Hint: ").append(recoveryHint).append("]");
    }

    return sb.toString();
  }
}
