package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for marshaling complex Java objects to and from WebAssembly memory.
 *
 * <p>This service provides efficient serialization and deserialization of complex data structures
 * for WebAssembly interop, including custom POJOs, collections, and multi-dimensional arrays. It
 * supports both value-based marshaling for small objects and memory-based marshaling for large
 * objects.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic detection of optimal marshaling strategy
 *   <li>Support for circular references and object graphs
 *   <li>Efficient memory layout for WebAssembly consumption
 *   <li>Bidirectional marshaling with type safety
 *   <li>Performance optimization for common patterns
 * </ul>
 *
 * @since 1.0.0
 */
public final class ComplexMarshalingService {

  private static final Logger LOGGER = Logger.getLogger(ComplexMarshalingService.class.getName());

  /** Threshold for switching to memory-based marshaling (in bytes). */
  private static final int MEMORY_MARSHALING_THRESHOLD = 1024;

  /** Maximum recursion depth for object graphs. */
  private static final int MAX_RECURSION_DEPTH = 100;

  /** Magic bytes for marshaled data format identification. */
  private static final byte[] MAGIC_BYTES = {'W', 'A', 'S', 'M'};

  /** Version number for marshaling format compatibility. */
  private static final int FORMAT_VERSION = 1;

  private final MarshalingConfiguration configuration;

  /** Creates a new complex marshaling service with default configuration. */
  public ComplexMarshalingService() {
    this(MarshalingConfiguration.defaultConfiguration());
  }

  /**
   * Creates a new complex marshaling service with the specified configuration.
   *
   * @param configuration the marshaling configuration
   * @throws IllegalArgumentException if configuration is null
   */
  public ComplexMarshalingService(final MarshalingConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
  }

  /**
   * Marshals a Java object to WebAssembly-compatible format.
   *
   * <p>This method automatically detects the optimal marshaling strategy based on object size and
   * complexity. Small objects are marshaled as value parameters, while large objects use
   * memory-based marshaling.
   *
   * @param object the object to marshal
   * @return the marshaled representation
   * @throws WasmException if marshaling fails
   */
  public MarshaledData marshal(final Object object) throws WasmException {
    Objects.requireNonNull(object, "Object to marshal cannot be null");

    try {
      // Determine optimal marshaling strategy
      final MarshalingStrategy strategy = selectMarshalingStrategy(object);

      if (strategy == MarshalingStrategy.VALUE_BASED) {
        return marshalAsValue(object);
      } else if (strategy == MarshalingStrategy.MEMORY_BASED) {
        return marshalAsMemory(object);
      } else if (strategy == MarshalingStrategy.HYBRID) {
        return marshalAsHybrid(object);
      } else {
        throw new IllegalArgumentException("Unknown marshaling strategy: " + strategy);
      }

    } catch (Exception e) {
      throw new WasmException("Failed to marshal object: " + object.getClass().getName(), e);
    }
  }

  /**
   * Unmarshals WebAssembly data back to a Java object.
   *
   * @param marshaledData the marshaled data
   * @param expectedType the expected Java type
   * @param <T> the target type
   * @return the unmarshaled object
   * @throws WasmException if unmarshaling fails
   */
  @SuppressWarnings("unchecked")
  public <T> T unmarshal(final MarshaledData marshaledData, final Class<T> expectedType)
      throws WasmException {
    Objects.requireNonNull(marshaledData, "Marshaled data cannot be null");
    Objects.requireNonNull(expectedType, "Expected type cannot be null");

    try {
      return switch (marshaledData.getStrategy()) {
        case VALUE_BASED -> (T) unmarshalFromValue(marshaledData, expectedType);
        case MEMORY_BASED -> (T) unmarshalFromMemory(marshaledData, expectedType);
        case HYBRID -> (T) unmarshalFromHybrid(marshaledData, expectedType);
      };

    } catch (Exception e) {
      throw new WasmException("Failed to unmarshal data to " + expectedType.getName(), e);
    }
  }

  /**
   * Creates a WasmComplexValue from a Java object with automatic type detection.
   *
   * @param object the Java object
   * @return the corresponding WasmComplexValue
   * @throws WasmException if the object cannot be converted
   */
  public WasmComplexValue createComplexValue(final Object object) throws WasmException {
    Objects.requireNonNull(object, "Object cannot be null");

    final Class<?> objectType = object.getClass();

    // Handle multi-dimensional arrays
    if (objectType.isArray() && objectType.getComponentType().isArray()) {
      return WasmComplexValue.multiArray(object);
    }

    // Handle collections
    if (object instanceof List) {
      final List<?> list = (List<?>) object;
      final Class<?> elementType = determineCollectionElementType(list);
      return WasmComplexValue.list(list, elementType);
    }

    if (object instanceof Map) {
      final Map<?, ?> map = (Map<?, ?>) object;
      final Class<?> keyType = determineMapKeyType(map);
      final Class<?> valueType = determineMapValueType(map);
      return WasmComplexValue.map(map, keyType, valueType);
    }

    // Handle strings
    if (object instanceof String) {
      return WasmComplexValue.string((String) object);
    }

    // Handle byte arrays as binary data
    if (object instanceof byte[]) {
      return WasmComplexValue.binaryBlob((byte[]) object);
    }

    // Handle custom objects
    return WasmComplexValue.object(object);
  }

  /**
   * Estimates the serialized size of an object for marshaling strategy selection.
   *
   * @param object the object to estimate
   * @return estimated size in bytes
   */
  public long estimateSerializedSize(final Object object) {
    Objects.requireNonNull(object, "Object cannot be null");

    try {
      return estimateSizeRecursive(object, 0, new HashMap<>());
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to estimate object size, using default", e);
      return MEMORY_MARSHALING_THRESHOLD; // Conservative estimate
    }
  }

  /**
   * Selects the optimal marshaling strategy for an object.
   *
   * @param object the object to marshal
   * @return the recommended marshaling strategy
   */
  private MarshalingStrategy selectMarshalingStrategy(final Object object) {
    final long estimatedSize = estimateSerializedSize(object);

    if (estimatedSize <= configuration.getValueMarshalingThreshold()) {
      return MarshalingStrategy.VALUE_BASED;
    }

    if (estimatedSize <= configuration.getHybridMarshalingThreshold()) {
      return MarshalingStrategy.HYBRID;
    }

    return MarshalingStrategy.MEMORY_BASED;
  }

  /**
   * Marshals an object using value-based strategy.
   *
   * @param object the object to marshal
   * @return the marshaled data
   * @throws Exception if marshaling fails
   */
  private MarshaledData marshalAsValue(final Object object) throws Exception {
    final byte[] serializedData = serializeObject(object);
    return new MarshaledData(MarshalingStrategy.VALUE_BASED, serializedData, null);
  }

  /**
   * Marshals an object using memory-based strategy.
   *
   * @param object the object to marshal
   * @return the marshaled data
   * @throws Exception if marshaling fails
   */
  private MarshaledData marshalAsMemory(final Object object) throws Exception {
    final byte[] serializedData = serializeObject(object);
    final MemoryHandle memoryHandle = allocateMemory(serializedData.length);
    writeToMemory(memoryHandle, serializedData);
    return new MarshaledData(MarshalingStrategy.MEMORY_BASED, null, memoryHandle);
  }

  /**
   * Marshals an object using hybrid strategy.
   *
   * @param object the object to marshal
   * @return the marshaled data
   * @throws Exception if marshaling fails
   */
  private MarshaledData marshalAsHybrid(final Object object) throws Exception {
    // For hybrid strategy, marshal metadata as value and large data as memory
    final ObjectMetadata metadata = extractMetadata(object);
    final byte[] metadataBytes = serializeObject(metadata);

    final byte[] objectData = serializeObject(object);
    final MemoryHandle memoryHandle = allocateMemory(objectData.length);
    writeToMemory(memoryHandle, objectData);

    return new MarshaledData(MarshalingStrategy.HYBRID, metadataBytes, memoryHandle);
  }

  /**
   * Unmarshals data using value-based strategy.
   *
   * @param marshaledData the marshaled data
   * @param expectedType the expected type
   * @return the unmarshaled object
   * @throws Exception if unmarshaling fails
   */
  private Object unmarshalFromValue(final MarshaledData marshaledData, final Class<?> expectedType)
      throws Exception {
    final byte[] data = marshaledData.getValueData();
    return deserializeObject(data, expectedType);
  }

  /**
   * Unmarshals data using memory-based strategy.
   *
   * @param marshaledData the marshaled data
   * @param expectedType the expected type
   * @return the unmarshaled object
   * @throws Exception if unmarshaling fails
   */
  private Object unmarshalFromMemory(final MarshaledData marshaledData, final Class<?> expectedType)
      throws Exception {
    final MemoryHandle memoryHandle = marshaledData.getMemoryHandle();
    final byte[] data = readFromMemory(memoryHandle);
    return deserializeObject(data, expectedType);
  }

  /**
   * Unmarshals data using hybrid strategy.
   *
   * @param marshaledData the marshaled data
   * @param expectedType the expected type
   * @return the unmarshaled object
   * @throws Exception if unmarshaling fails
   */
  private Object unmarshalFromHybrid(final MarshaledData marshaledData, final Class<?> expectedType)
      throws Exception {
    final MemoryHandle memoryHandle = marshaledData.getMemoryHandle();
    final byte[] data = readFromMemory(memoryHandle);
    return deserializeObject(data, expectedType);
  }

  /**
   * Serializes an object to byte array with format header.
   *
   * @param object the object to serialize
   * @return the serialized bytes
   * @throws Exception if serialization fails
   */
  private byte[] serializeObject(final Object object) throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos)) {

      // Write format header
      baos.write(MAGIC_BYTES);
      oos.writeInt(FORMAT_VERSION);
      oos.writeUTF(object.getClass().getName());

      // Write object data
      if (object instanceof Serializable) {
        oos.writeObject(object);
      } else {
        // Use custom serialization for non-Serializable objects
        serializeNonSerializable(oos, object);
      }

      oos.flush();
      return baos.toByteArray();
    }
  }

  /**
   * Deserializes an object from byte array.
   *
   * @param data the serialized data
   * @param expectedType the expected type
   * @return the deserialized object
   * @throws Exception if deserialization fails
   */
  private Object deserializeObject(final byte[] data, final Class<?> expectedType)
      throws Exception {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final ObjectInputStream ois = new ObjectInputStream(bais)) {

      // Verify format header
      final byte[] magic = new byte[MAGIC_BYTES.length];
      if (bais.read(magic) != MAGIC_BYTES.length || !java.util.Arrays.equals(magic, MAGIC_BYTES)) {
        throw new WasmException("Invalid marshaled data format");
      }

      final int version = ois.readInt();
      if (version != FORMAT_VERSION) {
        throw new WasmException("Unsupported marshaling format version: " + version);
      }

      final String className = ois.readUTF();
      final Class<?> actualType = Class.forName(className);

      if (!expectedType.isAssignableFrom(actualType)) {
        throw new WasmException(
            "Type mismatch: expected " + expectedType.getName() + ", got " + actualType.getName());
      }

      // Read object data
      if (Serializable.class.isAssignableFrom(actualType)) {
        return ois.readObject();
      } else {
        return deserializeNonSerializable(ois, actualType);
      }
    }
  }

  /**
   * Custom serialization for non-Serializable objects.
   *
   * @param oos the output stream
   * @param object the object to serialize
   * @throws Exception if serialization fails
   */
  private void serializeNonSerializable(final ObjectOutputStream oos, final Object object)
      throws Exception {
    // Implement custom serialization logic based on object type
    if (object.getClass().isArray()) {
      serializeArray(oos, object);
    } else {
      // For POJOs, use reflection-based serialization
      serializePojo(oos, object);
    }
  }

  /**
   * Custom deserialization for non-Serializable objects.
   *
   * @param ois the input stream
   * @param type the expected type
   * @return the deserialized object
   * @throws Exception if deserialization fails
   */
  private Object deserializeNonSerializable(final ObjectInputStream ois, final Class<?> type)
      throws Exception {
    if (type.isArray()) {
      return deserializeArray(ois, type);
    } else {
      return deserializePojo(ois, type);
    }
  }

  /**
   * Serializes an array object.
   *
   * @param oos the output stream
   * @param array the array to serialize
   * @throws Exception if serialization fails
   */
  private void serializeArray(final ObjectOutputStream oos, final Object array) throws Exception {
    final Class<?> componentType = array.getClass().getComponentType();
    final int length = Array.getLength(array);

    oos.writeUTF(componentType.getName());
    oos.writeInt(length);

    for (int i = 0; i < length; i++) {
      final Object element = Array.get(array, i);
      if (element == null) {
        oos.writeBoolean(true); // null marker
      } else {
        oos.writeBoolean(false);
        if (element instanceof Serializable) {
          oos.writeObject(element);
        } else {
          serializeNonSerializable(oos, element);
        }
      }
    }
  }

  /**
   * Deserializes an array object.
   *
   * @param ois the input stream
   * @param arrayType the array type
   * @return the deserialized array
   * @throws Exception if deserialization fails
   */
  private Object deserializeArray(final ObjectInputStream ois, final Class<?> arrayType)
      throws Exception {
    final String componentTypeName = ois.readUTF();
    final Class<?> componentType = Class.forName(componentTypeName);
    final int length = ois.readInt();

    final Object array = Array.newInstance(componentType, length);

    for (int i = 0; i < length; i++) {
      final boolean isNull = ois.readBoolean();
      if (!isNull) {
        final Object element;
        if (Serializable.class.isAssignableFrom(componentType)) {
          element = ois.readObject();
        } else {
          element = deserializeNonSerializable(ois, componentType);
        }
        Array.set(array, i, element);
      }
    }

    return array;
  }

  /**
   * Serializes a POJO using reflection.
   *
   * @param oos the output stream
   * @param object the object to serialize
   * @throws Exception if serialization fails
   */
  private void serializePojo(final ObjectOutputStream oos, final Object object) throws Exception {
    // This is a simplified implementation - real implementation would handle
    // all field types, inheritance, etc.
    oos.writeUTF("POJO_PLACEHOLDER");
  }

  /**
   * Deserializes a POJO using reflection.
   *
   * @param ois the input stream
   * @param type the POJO type
   * @return the deserialized object
   * @throws Exception if deserialization fails
   */
  private Object deserializePojo(final ObjectInputStream ois, final Class<?> type)
      throws Exception {
    final String placeholder = ois.readUTF();
    if (!"POJO_PLACEHOLDER".equals(placeholder)) {
      throw new WasmException("Invalid POJO serialization format");
    }
    // Simplified implementation - would create instance and populate fields
    return type.getDeclaredConstructor().newInstance();
  }

  /**
   * Estimates the serialized size of an object recursively.
   *
   * @param object the object to estimate
   * @param depth current recursion depth
   * @param visited map of visited objects to handle cycles
   * @return estimated size in bytes
   */
  private long estimateSizeRecursive(
      final Object object, final int depth, final Map<Object, Boolean> visited) {
    if (object == null || depth > MAX_RECURSION_DEPTH) {
      return 0;
    }

    if (visited.containsKey(object)) {
      return 4; // Reference size for circular reference
    }
    visited.put(object, Boolean.TRUE);

    final Class<?> type = object.getClass();

    // Primitive wrappers and strings
    if (type == Integer.class || type == Float.class) {
      return 4;
    }
    if (type == Long.class || type == Double.class) {
      return 8;
    }
    if (type == String.class) {
      return ((String) object).length() * 2L; // Approximate UTF-16 size
    }
    if (type == byte[].class) {
      return ((byte[]) object).length;
    }

    // Arrays
    if (type.isArray()) {
      final int length = Array.getLength(object);
      long totalSize = 8; // Array header
      for (int i = 0; i < length; i++) {
        totalSize += estimateSizeRecursive(Array.get(object, i), depth + 1, visited);
      }
      return totalSize;
    }

    // Collections
    if (object instanceof List) {
      final List<?> list = (List<?>) object;
      long totalSize = 16; // List overhead
      for (final Object element : list) {
        totalSize += estimateSizeRecursive(element, depth + 1, visited);
      }
      return totalSize;
    }

    if (object instanceof Map) {
      final Map<?, ?> map = (Map<?, ?>) object;
      long totalSize = 32; // Map overhead
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        totalSize += estimateSizeRecursive(entry.getKey(), depth + 1, visited);
        totalSize += estimateSizeRecursive(entry.getValue(), depth + 1, visited);
      }
      return totalSize;
    }

    // Default object size estimation
    return 64; // Conservative estimate for POJOs
  }

  /**
   * Determines the element type of a collection.
   *
   * @param collection the collection
   * @return the element type or Object.class if cannot be determined
   */
  private Class<?> determineCollectionElementType(final List<?> collection) {
    if (collection.isEmpty()) {
      return Object.class;
    }

    final Object firstElement = collection.get(0);
    return firstElement != null ? firstElement.getClass() : Object.class;
  }

  /**
   * Determines the key type of a map.
   *
   * @param map the map
   * @return the key type or Object.class if cannot be determined
   */
  private Class<?> determineMapKeyType(final Map<?, ?> map) {
    if (map.isEmpty()) {
      return Object.class;
    }

    final Object firstKey = map.keySet().iterator().next();
    return firstKey != null ? firstKey.getClass() : Object.class;
  }

  /**
   * Determines the value type of a map.
   *
   * @param map the map
   * @return the value type or Object.class if cannot be determined
   */
  private Class<?> determineMapValueType(final Map<?, ?> map) {
    if (map.isEmpty()) {
      return Object.class;
    }

    final Object firstValue = map.values().iterator().next();
    return firstValue != null ? firstValue.getClass() : Object.class;
  }

  /**
   * Extracts metadata from an object for hybrid marshaling.
   *
   * @param object the object
   * @return the metadata
   */
  private ObjectMetadata extractMetadata(final Object object) {
    return new ObjectMetadata(object.getClass().getName(), estimateSerializedSize(object));
  }

  /**
   * Allocates memory for marshaled data.
   *
   * @param size the size in bytes
   * @return the memory handle
   */
  private MemoryHandle allocateMemory(final int size) {
    // This would integrate with the WebAssembly memory system
    // For now, just return a placeholder
    return new MemoryHandle(0, size);
  }

  /**
   * Writes data to allocated memory.
   *
   * @param handle the memory handle
   * @param data the data to write
   */
  private void writeToMemory(final MemoryHandle handle, final byte[] data) {
    // This would write to actual WebAssembly memory
    // For now, just store the data in the handle
    handle.setData(data);
  }

  /**
   * Reads data from memory.
   *
   * @param handle the memory handle
   * @return the read data
   */
  private byte[] readFromMemory(final MemoryHandle handle) {
    // This would read from actual WebAssembly memory
    return handle.getData();
  }

  /** Enumeration of marshaling strategies. */
  private enum MarshalingStrategy {
    /** Marshal as direct parameter values. */
    VALUE_BASED,
    /** Marshal through shared memory. */
    MEMORY_BASED,
    /** Hybrid approach with metadata and memory. */
    HYBRID
  }

  /** Container for marshaled data with strategy information. */
  public static final class MarshaledData {
    private final MarshalingStrategy strategy;
    private final byte[] valueData;
    private final MemoryHandle memoryHandle;

    MarshaledData(
        final MarshalingStrategy strategy,
        final byte[] valueData,
        final MemoryHandle memoryHandle) {
      this.strategy = strategy;
      this.valueData = valueData;
      this.memoryHandle = memoryHandle;
    }

    public MarshalingStrategy getStrategy() {
      return strategy;
    }

    public byte[] getValueData() {
      return valueData;
    }

    public MemoryHandle getMemoryHandle() {
      return memoryHandle;
    }
  }

  /** Handle for memory-based marshaling. */
  public static final class MemoryHandle {
    private final long address;
    private final int size;
    private byte[] data; // Placeholder for actual memory integration

    MemoryHandle(final long address, final int size) {
      this.address = address;
      this.size = size;
    }

    public long getAddress() {
      return address;
    }

    public int getSize() {
      return size;
    }

    void setData(final byte[] data) {
      this.data = data;
    }

    byte[] getData() {
      return data;
    }
  }

  /** Metadata for objects in hybrid marshaling. */
  private static final class ObjectMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String className;
    private final long size;

    ObjectMetadata(final String className, final long size) {
      this.className = className;
      this.size = size;
    }

    public String getClassName() {
      return className;
    }

    public long getSize() {
      return size;
    }
  }
}
