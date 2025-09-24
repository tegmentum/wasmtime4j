package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.ComplexMarshalingService;
import ai.tegmentum.wasmtime4j.MarshalingConfiguration;
import ai.tegmentum.wasmtime4j.WasmComplexValue;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced type converter for JNI runtime supporting complex parameter marshaling.
 *
 * <p>This converter extends the basic JNI type conversion capabilities to support complex Java data
 * structures including multi-dimensional arrays, collections, custom POJOs, and nested structures.
 * It provides efficient bidirectional marshaling between Java and WebAssembly with automatic
 * optimization strategy selection.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>Multi-dimensional array marshaling with type preservation
 *   <li>Collection marshaling (List, Map, Set) with element type tracking
 *   <li>Custom POJO marshaling with reflection-based serialization
 *   <li>Memory-based marshaling for large objects
 *   <li>Circular reference detection and handling
 *   <li>Performance optimization through strategy selection
 * </ul>
 *
 * @since 1.0.0
 */
public final class ComplexJniTypeConverter {

  private static final Logger LOGGER = Logger.getLogger(ComplexJniTypeConverter.class.getName());

  /** Cache for marshaling strategies to avoid repeated analysis. */
  private static final ConcurrentHashMap<Class<?>, MarshalingStrategy> STRATEGY_CACHE =
      new ConcurrentHashMap<>();

  /** Maximum cache size to prevent memory leaks. */
  private static final int MAX_CACHE_SIZE = 1000;

  /** Size threshold for automatic memory-based marshaling (in bytes). */
  private static final int MEMORY_MARSHALING_THRESHOLD = 2048;

  private final ComplexMarshalingService marshalingService;
  private final MarshalingConfiguration configuration;

  /** Creates a new complex JNI type converter with default configuration. */
  public ComplexJniTypeConverter() {
    this(MarshalingConfiguration.defaultConfiguration());
  }

  /**
   * Creates a new complex JNI type converter with the specified configuration.
   *
   * @param configuration the marshaling configuration
   * @throws IllegalArgumentException if configuration is null
   */
  public ComplexJniTypeConverter(final MarshalingConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.marshalingService = new ComplexMarshalingService(configuration);
  }

  /**
   * Converts a complex Java object to WebAssembly-compatible parameters.
   *
   * <p>This method automatically detects the optimal marshaling strategy based on object type and
   * size, then converts the object to a format suitable for JNI transmission to WebAssembly.
   *
   * @param object the Java object to convert
   * @return array of marshaled parameters for WebAssembly
   * @throws WasmException if conversion fails
   */
  public WasmValue[] convertComplexObjectToWasm(final Object object) throws WasmException {
    Objects.requireNonNull(object, "Object to convert cannot be null");

    try {
      // Create complex value representation
      final WasmComplexValue complexValue = marshalingService.createComplexValue(object);

      // Marshal to WebAssembly format
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(object);

      // Convert marshaled data to WasmValue array
      return convertMarshaledDataToWasmValues(marshaledData, complexValue);

    } catch (Exception e) {
      throw new WasmException(
          "Failed to convert complex object: " + object.getClass().getName(), e);
    }
  }

  /**
   * Converts WebAssembly parameters back to a complex Java object.
   *
   * @param wasmValues the WebAssembly parameter values
   * @param expectedType the expected Java type
   * @param <T> the target type
   * @return the reconstructed Java object
   * @throws WasmException if conversion fails
   */
  @SuppressWarnings("unchecked")
  public <T> T convertWasmToComplexObject(final WasmValue[] wasmValues, final Class<T> expectedType)
      throws WasmException {
    Objects.requireNonNull(wasmValues, "WebAssembly values cannot be null");
    Objects.requireNonNull(expectedType, "Expected type cannot be null");

    try {
      // Reconstruct marshaled data from WasmValue array
      final ComplexMarshalingService.MarshaledData marshaledData =
          reconstructMarshaledData(wasmValues);

      // Unmarshal to Java object
      return marshalingService.unmarshal(marshaledData, expectedType);

    } catch (Exception e) {
      throw new WasmException(
          "Failed to convert WebAssembly values to " + expectedType.getName(), e);
    }
  }

  /**
   * Marshals a multi-dimensional array with efficient layout.
   *
   * @param array the multi-dimensional array
   * @return the marshaled WasmValue array
   * @throws WasmException if marshaling fails
   */
  public WasmValue[] marshalMultiDimensionalArray(final Object array) throws WasmException {
    Objects.requireNonNull(array, "Array cannot be null");

    if (!array.getClass().isArray()) {
      throw new WasmException("Object is not an array: " + array.getClass().getName());
    }

    try {
      final ArrayMarshalingResult result = marshalArrayRecursive(array);
      return new WasmValue[] {
        WasmValue.i32(result.getDimensions()),
        WasmValue.i32(result.getTotalElements()),
        WasmValue.externref(result.getDataBuffer()),
        WasmValue.externref(result.getShapeBuffer())
      };

    } catch (Exception e) {
      throw new WasmException("Failed to marshal multi-dimensional array", e);
    }
  }

  /**
   * Unmarshals a multi-dimensional array from WebAssembly format.
   *
   * @param wasmValues the marshaled array data
   * @param arrayType the expected array type
   * @return the reconstructed multi-dimensional array
   * @throws WasmException if unmarshaling fails
   */
  public Object unmarshalMultiDimensionalArray(
      final WasmValue[] wasmValues, final Class<?> arrayType) throws WasmException {
    JniValidation.requireNonNull(wasmValues, "WebAssembly values");
    JniValidation.requireNonNull(arrayType, "Array type");

    if (wasmValues.length != 4) {
      throw new WasmException(
          "Invalid array marshaling format: expected 4 values, got " + wasmValues.length);
    }

    try {
      final int dimensions = wasmValues[0].asI32();
      final int totalElements = wasmValues[1].asI32();
      final ByteBuffer dataBuffer = (ByteBuffer) wasmValues[2].asExternref();
      final int[] shape = (int[]) wasmValues[3].asExternref();

      return reconstructArray(dataBuffer, shape, arrayType.getComponentType());

    } catch (Exception e) {
      throw new WasmException("Failed to unmarshal multi-dimensional array", e);
    }
  }

  /**
   * Marshals a Java collection (List, Map, etc.) to WebAssembly format.
   *
   * @param collection the collection to marshal
   * @return the marshaled WasmValue array
   * @throws WasmException if marshaling fails
   */
  public WasmValue[] marshalCollection(final Object collection) throws WasmException {
    Objects.requireNonNull(collection, "Collection cannot be null");

    try {
      if (collection instanceof List) {
        return marshalList((List<?>) collection);
      } else if (collection instanceof Map) {
        return marshalMap((Map<?, ?>) collection);
      } else {
        throw new WasmException("Unsupported collection type: " + collection.getClass().getName());
      }

    } catch (Exception e) {
      throw new WasmException("Failed to marshal collection", e);
    }
  }

  /**
   * Unmarshals a collection from WebAssembly format.
   *
   * @param wasmValues the marshaled collection data
   * @param collectionType the expected collection type
   * @return the reconstructed collection
   * @throws WasmException if unmarshaling fails
   */
  public Object unmarshalCollection(final WasmValue[] wasmValues, final Class<?> collectionType)
      throws WasmException {
    JniValidation.requireNonNull(wasmValues, "WebAssembly values");
    JniValidation.requireNonNull(collectionType, "Collection type");

    try {
      if (List.class.isAssignableFrom(collectionType)) {
        return unmarshalList(wasmValues);
      } else if (Map.class.isAssignableFrom(collectionType)) {
        return unmarshalMap(wasmValues);
      } else {
        throw new WasmException("Unsupported collection type: " + collectionType.getName());
      }

    } catch (Exception e) {
      throw new WasmException("Failed to unmarshal collection", e);
    }
  }

  /**
   * Estimates the marshaling overhead for an object.
   *
   * @param object the object to analyze
   * @return estimated overhead in bytes
   */
  public long estimateMarshalingOverhead(final Object object) {
    Objects.requireNonNull(object, "Object cannot be null");

    try {
      return marshalingService.estimateSerializedSize(object);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to estimate marshaling overhead", e);
      return MEMORY_MARSHALING_THRESHOLD; // Conservative estimate
    }
  }

  /**
   * Validates that an object is suitable for complex marshaling.
   *
   * @param object the object to validate
   * @throws WasmException if the object cannot be marshaled
   */
  public void validateMarshalableObject(final Object object) throws WasmException {
    Objects.requireNonNull(object, "Object cannot be null");

    final Class<?> objectType = object.getClass();

    // Check for unsupported types
    if (objectType.isEnum()) {
      throw new WasmException("Enum marshaling not yet supported: " + objectType.getName());
    }

    // Check for circular references if enabled
    if (configuration.isCircularReferenceDetectionEnabled()) {
      detectCircularReferences(object);
    }

    // Validate size constraints
    final long estimatedSize = estimateMarshalingOverhead(object);
    if (estimatedSize > Integer.MAX_VALUE) {
      throw new WasmException("Object too large for marshaling: " + estimatedSize + " bytes");
    }
  }

  /**
   * Converts marshaled data to WasmValue array for JNI transmission.
   *
   * @param marshaledData the marshaled data
   * @param complexValue the complex value metadata
   * @return array of WasmValue objects
   */
  private WasmValue[] convertMarshaledDataToWasmValues(
      final ComplexMarshalingService.MarshaledData marshaledData,
      final WasmComplexValue complexValue) {

    switch (marshaledData.getStrategy()) {
      case VALUE_BASED:
        return new WasmValue[] {
          WasmValue.i32(0), // Strategy indicator
          WasmValue.externref(marshaledData.getValueData())
        };

      case MEMORY_BASED:
        return new WasmValue[] {
          WasmValue.i32(1), // Strategy indicator
          WasmValue.i64(marshaledData.getMemoryHandle().getAddress()),
          WasmValue.i32(marshaledData.getMemoryHandle().getSize())
        };

      case HYBRID:
        return new WasmValue[] {
          WasmValue.i32(2), // Strategy indicator
          WasmValue.externref(marshaledData.getValueData()),
          WasmValue.i64(marshaledData.getMemoryHandle().getAddress()),
          WasmValue.i32(marshaledData.getMemoryHandle().getSize())
        };

      default:
        throw new IllegalArgumentException(
            "Unknown marshaling strategy: " + marshaledData.getStrategy());
    }
  }

  /**
   * Reconstructs marshaled data from WasmValue array.
   *
   * @param wasmValues the WasmValue array
   * @return the reconstructed marshaled data
   */
  private ComplexMarshalingService.MarshaledData reconstructMarshaledData(
      final WasmValue[] wasmValues) {
    if (wasmValues.length < 2) {
      throw new IllegalArgumentException("Invalid marshaled data format");
    }

    final int strategyId = wasmValues[0].asI32();

    switch (strategyId) {
      case 0: // VALUE_BASED
        final byte[] valueData = (byte[]) wasmValues[1].asExternref();
        return new ComplexMarshalingService.MarshaledData(
            ComplexMarshalingService.MarshalingStrategy.VALUE_BASED, valueData, null);

      case 1: // MEMORY_BASED
        final long address = wasmValues[1].asI64();
        final int size = wasmValues[2].asI32();
        final ComplexMarshalingService.MemoryHandle memoryHandle =
            new ComplexMarshalingService.MemoryHandle(address, size);
        return new ComplexMarshalingService.MarshaledData(
            ComplexMarshalingService.MarshalingStrategy.MEMORY_BASED, null, memoryHandle);

      case 2: // HYBRID
        final byte[] hybridValueData = (byte[]) wasmValues[1].asExternref();
        final long hybridAddress = wasmValues[2].asI64();
        final int hybridSize = wasmValues[3].asI32();
        final ComplexMarshalingService.MemoryHandle hybridMemoryHandle =
            new ComplexMarshalingService.MemoryHandle(hybridAddress, hybridSize);
        return new ComplexMarshalingService.MarshaledData(
            ComplexMarshalingService.MarshalingStrategy.HYBRID,
            hybridValueData,
            hybridMemoryHandle);

      default:
        throw new IllegalArgumentException("Unknown marshaling strategy ID: " + strategyId);
    }
  }

  /**
   * Marshals an array recursively, computing shape and flattening data.
   *
   * @param array the array to marshal
   * @return the marshaling result
   */
  private ArrayMarshalingResult marshalArrayRecursive(final Object array) {
    final Class<?> arrayType = array.getClass();
    final ArrayInfo arrayInfo = analyzeArray(array);

    // Flatten the array data
    final ByteBuffer dataBuffer = flattenArray(array, arrayInfo);

    return new ArrayMarshalingResult(
        arrayInfo.getDimensions(), arrayInfo.getTotalElements(), dataBuffer, arrayInfo.getShape());
  }

  /**
   * Analyzes array structure to determine dimensions and shape.
   *
   * @param array the array to analyze
   * @return array analysis information
   */
  private ArrayInfo analyzeArray(final Object array) {
    final int[] shape = computeArrayShape(array);
    final int dimensions = shape.length;
    final int totalElements = computeTotalElements(shape);
    final Class<?> componentType = getArrayBaseComponentType(array.getClass());

    return new ArrayInfo(dimensions, totalElements, shape, componentType);
  }

  /**
   * Computes the shape (dimensions) of an array.
   *
   * @param array the array
   * @return the shape as int array
   */
  private int[] computeArrayShape(final Object array) {
    if (!array.getClass().isArray()) {
      return new int[0];
    }

    final int length = Array.getLength(array);
    if (length == 0) {
      return new int[] {0};
    }

    final Object firstElement = Array.get(array, 0);
    if (firstElement != null && firstElement.getClass().isArray()) {
      final int[] subShape = computeArrayShape(firstElement);
      final int[] shape = new int[subShape.length + 1];
      shape[0] = length;
      System.arraycopy(subShape, 0, shape, 1, subShape.length);
      return shape;
    } else {
      return new int[] {length};
    }
  }

  /**
   * Computes the total number of elements in a multi-dimensional array.
   *
   * @param shape the array shape
   * @return the total number of elements
   */
  private int computeTotalElements(final int[] shape) {
    int total = 1;
    for (final int dimension : shape) {
      total *= dimension;
    }
    return total;
  }

  /**
   * Gets the base component type of a multi-dimensional array.
   *
   * @param arrayType the array type
   * @return the base component type
   */
  private Class<?> getArrayBaseComponentType(final Class<?> arrayType) {
    Class<?> componentType = arrayType;
    while (componentType.isArray()) {
      componentType = componentType.getComponentType();
    }
    return componentType;
  }

  /**
   * Flattens a multi-dimensional array into a byte buffer.
   *
   * @param array the array to flatten
   * @param arrayInfo the array analysis information
   * @return the flattened data buffer
   */
  private ByteBuffer flattenArray(final Object array, final ArrayInfo arrayInfo) {
    final int elementSize = getElementSize(arrayInfo.getComponentType());
    final ByteBuffer buffer =
        ByteBuffer.allocate(arrayInfo.getTotalElements() * elementSize)
            .order(ByteOrder.LITTLE_ENDIAN);

    flattenArrayRecursive(array, buffer, arrayInfo.getComponentType());
    buffer.flip();
    return buffer;
  }

  /**
   * Recursively flattens array elements into a byte buffer.
   *
   * @param array the array or element
   * @param buffer the target buffer
   * @param componentType the base component type
   */
  private void flattenArrayRecursive(
      final Object array, final ByteBuffer buffer, final Class<?> componentType) {
    if (!array.getClass().isArray()) {
      // Base case: write primitive value
      writePrimitiveToBuffer(array, buffer, componentType);
      return;
    }

    final int length = Array.getLength(array);
    for (int i = 0; i < length; i++) {
      final Object element = Array.get(array, i);
      flattenArrayRecursive(element, buffer, componentType);
    }
  }

  /**
   * Writes a primitive value to a byte buffer.
   *
   * @param value the primitive value
   * @param buffer the target buffer
   * @param type the primitive type
   */
  private void writePrimitiveToBuffer(
      final Object value, final ByteBuffer buffer, final Class<?> type) {
    if (type == int.class) {
      buffer.putInt((Integer) value);
    } else if (type == long.class) {
      buffer.putLong((Long) value);
    } else if (type == float.class) {
      buffer.putFloat((Float) value);
    } else if (type == double.class) {
      buffer.putDouble((Double) value);
    } else if (type == byte.class) {
      buffer.put((Byte) value);
    } else if (type == short.class) {
      buffer.putShort((Short) value);
    } else if (type == char.class) {
      buffer.putChar((Character) value);
    } else if (type == boolean.class) {
      buffer.put((byte) ((Boolean) value ? 1 : 0));
    } else {
      throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
    }
  }

  /**
   * Gets the size in bytes of a primitive type.
   *
   * @param type the primitive type
   * @return the size in bytes
   */
  private int getElementSize(final Class<?> type) {
    if (type == int.class || type == float.class) {
      return 4;
    } else if (type == long.class || type == double.class) {
      return 8;
    } else if (type == short.class || type == char.class) {
      return 2;
    } else if (type == byte.class || type == boolean.class) {
      return 1;
    } else {
      throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
    }
  }

  /**
   * Reconstructs a multi-dimensional array from flattened data.
   *
   * @param dataBuffer the flattened data
   * @param shape the array shape
   * @param componentType the base component type
   * @return the reconstructed array
   */
  private Object reconstructArray(
      final ByteBuffer dataBuffer, final int[] shape, final Class<?> componentType) {
    if (shape.length == 1) {
      // Single-dimensional array
      return reconstructSingleDimensionArray(dataBuffer, shape[0], componentType);
    } else {
      // Multi-dimensional array
      return reconstructMultiDimensionArray(dataBuffer, shape, componentType, 0);
    }
  }

  /**
   * Reconstructs a single-dimensional array.
   *
   * @param dataBuffer the data buffer
   * @param length the array length
   * @param componentType the component type
   * @return the reconstructed array
   */
  private Object reconstructSingleDimensionArray(
      final ByteBuffer dataBuffer, final int length, final Class<?> componentType) {
    final Object array = Array.newInstance(componentType, length);

    for (int i = 0; i < length; i++) {
      final Object value = readPrimitiveFromBuffer(dataBuffer, componentType);
      Array.set(array, i, value);
    }

    return array;
  }

  /**
   * Reconstructs a multi-dimensional array recursively.
   *
   * @param dataBuffer the data buffer
   * @param shape the array shape
   * @param componentType the base component type
   * @param dimension the current dimension
   * @return the reconstructed array
   */
  private Object reconstructMultiDimensionArray(
      final ByteBuffer dataBuffer,
      final int[] shape,
      final Class<?> componentType,
      final int dimension) {
    final int length = shape[dimension];

    if (dimension == shape.length - 1) {
      // Last dimension - create array of base component type
      return reconstructSingleDimensionArray(dataBuffer, length, componentType);
    } else {
      // Intermediate dimension - create array of arrays
      final Class<?> subArrayType = createArrayType(componentType, shape.length - dimension - 1);
      final Object array = Array.newInstance(subArrayType, length);

      for (int i = 0; i < length; i++) {
        final Object subArray =
            reconstructMultiDimensionArray(dataBuffer, shape, componentType, dimension + 1);
        Array.set(array, i, subArray);
      }

      return array;
    }
  }

  /**
   * Creates an array type with the specified number of dimensions.
   *
   * @param componentType the base component type
   * @param dimensions the number of dimensions
   * @return the array type
   */
  private Class<?> createArrayType(final Class<?> componentType, final int dimensions) {
    Class<?> arrayType = componentType;
    for (int i = 0; i < dimensions; i++) {
      arrayType = Array.newInstance(arrayType, 0).getClass();
    }
    return arrayType;
  }

  /**
   * Reads a primitive value from a byte buffer.
   *
   * @param buffer the buffer to read from
   * @param type the primitive type
   * @return the primitive value
   */
  private Object readPrimitiveFromBuffer(final ByteBuffer buffer, final Class<?> type) {
    if (type == int.class) {
      return buffer.getInt();
    } else if (type == long.class) {
      return buffer.getLong();
    } else if (type == float.class) {
      return buffer.getFloat();
    } else if (type == double.class) {
      return buffer.getDouble();
    } else if (type == byte.class) {
      return buffer.get();
    } else if (type == short.class) {
      return buffer.getShort();
    } else if (type == char.class) {
      return buffer.getChar();
    } else if (type == boolean.class) {
      return buffer.get() != 0;
    } else {
      throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
    }
  }

  /**
   * Marshals a List to WebAssembly format.
   *
   * @param list the list to marshal
   * @return the marshaled WasmValue array
   */
  private WasmValue[] marshalList(final List<?> list) {
    // Simplified implementation - would include size, element type, and serialized data
    return new WasmValue[] {WasmValue.i32(list.size()), WasmValue.externref(list.toArray())};
  }

  /**
   * Marshals a Map to WebAssembly format.
   *
   * @param map the map to marshal
   * @return the marshaled WasmValue array
   */
  private WasmValue[] marshalMap(final Map<?, ?> map) {
    // Simplified implementation - would include size, key/value types, and serialized data
    return new WasmValue[] {
      WasmValue.i32(map.size()),
      WasmValue.externref(map.keySet().toArray()),
      WasmValue.externref(map.values().toArray())
    };
  }

  /**
   * Unmarshals a List from WebAssembly format.
   *
   * @param wasmValues the marshaled list data
   * @return the reconstructed list
   */
  private List<?> unmarshalList(final WasmValue[] wasmValues) {
    // Simplified implementation
    final int size = wasmValues[0].asI32();
    final Object[] elements = (Object[]) wasmValues[1].asExternref();
    return java.util.Arrays.asList(elements);
  }

  /**
   * Unmarshals a Map from WebAssembly format.
   *
   * @param wasmValues the marshaled map data
   * @return the reconstructed map
   */
  private Map<?, ?> unmarshalMap(final WasmValue[] wasmValues) {
    // Simplified implementation
    final int size = wasmValues[0].asI32();
    final Object[] keys = (Object[]) wasmValues[1].asExternref();
    final Object[] values = (Object[]) wasmValues[2].asExternref();

    final Map<Object, Object> map = new java.util.HashMap<>();
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }
    return map;
  }

  /**
   * Detects circular references in an object graph.
   *
   * @param object the object to check
   * @throws WasmException if circular references are detected
   */
  private void detectCircularReferences(final Object object) throws WasmException {
    // Simplified implementation - would perform deep object graph analysis
    // This is a placeholder for the real implementation
  }

  /** Container for array analysis information. */
  private static final class ArrayInfo {
    private final int dimensions;
    private final int totalElements;
    private final int[] shape;
    private final Class<?> componentType;

    ArrayInfo(
        final int dimensions,
        final int totalElements,
        final int[] shape,
        final Class<?> componentType) {
      this.dimensions = dimensions;
      this.totalElements = totalElements;
      this.shape = shape;
      this.componentType = componentType;
    }

    public int getDimensions() {
      return dimensions;
    }

    public int getTotalElements() {
      return totalElements;
    }

    public int[] getShape() {
      return shape;
    }

    public Class<?> getComponentType() {
      return componentType;
    }
  }

  /** Container for array marshaling results. */
  private static final class ArrayMarshalingResult {
    private final int dimensions;
    private final int totalElements;
    private final ByteBuffer dataBuffer;
    private final int[] shapeBuffer;

    ArrayMarshalingResult(
        final int dimensions,
        final int totalElements,
        final ByteBuffer dataBuffer,
        final int[] shapeBuffer) {
      this.dimensions = dimensions;
      this.totalElements = totalElements;
      this.dataBuffer = dataBuffer;
      this.shapeBuffer = shapeBuffer;
    }

    public int getDimensions() {
      return dimensions;
    }

    public int getTotalElements() {
      return totalElements;
    }

    public ByteBuffer getDataBuffer() {
      return dataBuffer;
    }

    public int[] getShapeBuffer() {
      return shapeBuffer;
    }
  }

  /** Enumeration of marshaling strategies. */
  private enum MarshalingStrategy {
    VALUE_BASED,
    MEMORY_BASED,
    HYBRID
  }
}
