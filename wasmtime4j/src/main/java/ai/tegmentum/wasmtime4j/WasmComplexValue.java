package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents complex WebAssembly values that extend beyond basic primitive types.
 *
 * <p>This class enables marshaling of complex Java data structures to and from WebAssembly,
 * including multi-dimensional arrays, collections, and custom POJOs. It provides efficient
 * serialization mechanisms for memory-based parameter passing.
 *
 * <p>Supported complex types:
 *
 * <ul>
 *   <li>Multi-dimensional arrays (int[][], float[][][], etc.)
 *   <li>Java collections (List, Map, Set)
 *   <li>Custom Java objects (POJOs)
 *   <li>Nested structures and union types
 *   <li>Reference types with object handles
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasmComplexValue {

  /** Enumeration of complex value types supported by the marshaling system. */
  public enum ComplexType {
    /** Multi-dimensional array. */
    MULTI_ARRAY,
    /** Java List collection. */
    LIST,
    /** Java Map collection. */
    MAP,
    /** Custom Java object (POJO). */
    OBJECT,
    /** Nested structure. */
    STRUCT,
    /** Union type. */
    UNION,
    /** Binary data blob. */
    BINARY_BLOB,
    /** String data. */
    STRING_DATA,
    /** Null reference. */
    NULL_REF
  }

  private final ComplexType complexType;
  private final Object value;
  private final Class<?> javaType;
  private final MarshalingMetadata metadata;

  /**
   * Creates a complex WebAssembly value with the specified type and data.
   *
   * @param complexType the complex value type
   * @param value the Java value object
   * @param javaType the original Java type for reverse marshaling
   * @param metadata additional marshaling metadata
   * @throws IllegalArgumentException if parameters are invalid
   */
  private WasmComplexValue(
      final ComplexType complexType,
      final Object value,
      final Class<?> javaType,
      final MarshalingMetadata metadata) {
    this.complexType = Objects.requireNonNull(complexType, "Complex type cannot be null");
    this.value = value; // Null is allowed for NULL_REF type
    this.javaType = Objects.requireNonNull(javaType, "Java type cannot be null");
    this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
  }

  /**
   * Gets the complex type of this value.
   *
   * @return the complex type
   */
  public ComplexType getComplexType() {
    return complexType;
  }

  /**
   * Gets the raw value object.
   *
   * @return the value object (may be null for NULL_REF)
   */
  public Object getValue() {
    return value;
  }

  /**
   * Gets the original Java type for this value.
   *
   * @return the Java type
   */
  public Class<?> getJavaType() {
    return javaType;
  }

  /**
   * Gets the marshaling metadata for this value.
   *
   * @return the marshaling metadata
   */
  public MarshalingMetadata getMetadata() {
    return metadata;
  }

  /**
   * Checks if this complex value is null.
   *
   * @return true if this represents a null reference
   */
  public boolean isNull() {
    return complexType == ComplexType.NULL_REF || value == null;
  }

  /**
   * Gets this value as a multi-dimensional array.
   *
   * @param <T> the array component type
   * @return the multi-dimensional array
   * @throws ClassCastException if this value is not a multi-dimensional array
   */
  @SuppressWarnings("unchecked")
  public <T> T asMultiArray() {
    if (complexType != ComplexType.MULTI_ARRAY) {
      throw new ClassCastException("Value is not a multi-dimensional array, but " + complexType);
    }
    return (T) value;
  }

  /**
   * Gets this value as a Java List.
   *
   * @param <T> the list element type
   * @return the list
   * @throws ClassCastException if this value is not a list
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> asList() {
    if (complexType != ComplexType.LIST) {
      throw new ClassCastException("Value is not a list, but " + complexType);
    }
    return (List<T>) value;
  }

  /**
   * Gets this value as a Java Map.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @return the map
   * @throws ClassCastException if this value is not a map
   */
  @SuppressWarnings("unchecked")
  public <K, V> Map<K, V> asMap() {
    if (complexType != ComplexType.MAP) {
      throw new ClassCastException("Value is not a map, but " + complexType);
    }
    return (Map<K, V>) value;
  }

  /**
   * Gets this value as a custom Java object.
   *
   * @param <T> the object type
   * @return the object
   * @throws ClassCastException if this value is not an object
   */
  @SuppressWarnings("unchecked")
  public <T> T asObject() {
    if (complexType != ComplexType.OBJECT) {
      throw new ClassCastException("Value is not an object, but " + complexType);
    }
    return (T) value;
  }

  /**
   * Gets this value as binary data.
   *
   * @return the binary data as byte array
   * @throws ClassCastException if this value is not binary data
   */
  public byte[] asBinaryBlob() {
    if (complexType != ComplexType.BINARY_BLOB) {
      throw new ClassCastException("Value is not binary data, but " + complexType);
    }
    return ((byte[]) value).clone();
  }

  /**
   * Gets this value as string data.
   *
   * @return the string value
   * @throws ClassCastException if this value is not string data
   */
  public String asString() {
    if (complexType != ComplexType.STRING_DATA) {
      throw new ClassCastException("Value is not string data, but " + complexType);
    }
    return (String) value;
  }

  // Factory methods for creating complex values

  /**
   * Creates a complex value for a multi-dimensional array.
   *
   * @param array the multi-dimensional array
   * @return a new WasmComplexValue
   * @throws IllegalArgumentException if array is null or not multi-dimensional
   */
  public static WasmComplexValue multiArray(final Object array) {
    Objects.requireNonNull(array, "Array cannot be null");
    final Class<?> arrayType = array.getClass();

    if (!arrayType.isArray()) {
      throw new IllegalArgumentException("Value must be an array");
    }

    // Check for multi-dimensional array
    final Class<?> componentType = arrayType.getComponentType();
    if (!componentType.isArray()) {
      throw new IllegalArgumentException("Array must be multi-dimensional");
    }

    final MarshalingMetadata metadata =
        MarshalingMetadata.builder()
            .withArrayDimensions(getArrayDimensions(arrayType))
            .withComponentType(getArrayBaseComponentType(arrayType))
            .build();

    return new WasmComplexValue(ComplexType.MULTI_ARRAY, array, arrayType, metadata);
  }

  /**
   * Creates a complex value for a Java List.
   *
   * @param list the list
   * @param elementType the type of list elements
   * @return a new WasmComplexValue
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static WasmComplexValue list(final List<?> list, final Class<?> elementType) {
    Objects.requireNonNull(list, "List cannot be null");
    Objects.requireNonNull(elementType, "Element type cannot be null");

    final MarshalingMetadata metadata =
        MarshalingMetadata.builder()
            .withCollectionSize(list.size())
            .withComponentType(elementType)
            .build();

    return new WasmComplexValue(ComplexType.LIST, list, List.class, metadata);
  }

  /**
   * Creates a complex value for a Java Map.
   *
   * @param map the map
   * @param keyType the type of map keys
   * @param valueType the type of map values
   * @return a new WasmComplexValue
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static WasmComplexValue map(
      final Map<?, ?> map, final Class<?> keyType, final Class<?> valueType) {
    Objects.requireNonNull(map, "Map cannot be null");
    Objects.requireNonNull(keyType, "Key type cannot be null");
    Objects.requireNonNull(valueType, "Value type cannot be null");

    final MarshalingMetadata metadata =
        MarshalingMetadata.builder()
            .withCollectionSize(map.size())
            .withKeyType(keyType)
            .withValueType(valueType)
            .build();

    return new WasmComplexValue(ComplexType.MAP, map, Map.class, metadata);
  }

  /**
   * Creates a complex value for a custom Java object.
   *
   * @param object the object
   * @return a new WasmComplexValue
   * @throws IllegalArgumentException if object is null
   */
  public static WasmComplexValue object(final Object object) {
    Objects.requireNonNull(object, "Object cannot be null");

    final MarshalingMetadata metadata =
        MarshalingMetadata.builder().withObjectClassName(object.getClass().getName()).build();

    return new WasmComplexValue(ComplexType.OBJECT, object, object.getClass(), metadata);
  }

  /**
   * Creates a complex value for binary data.
   *
   * @param data the binary data
   * @return a new WasmComplexValue
   * @throws IllegalArgumentException if data is null
   */
  public static WasmComplexValue binaryBlob(final byte[] data) {
    Objects.requireNonNull(data, "Binary data cannot be null");

    final MarshalingMetadata metadata =
        MarshalingMetadata.builder().withDataSize(data.length).build();

    return new WasmComplexValue(ComplexType.BINARY_BLOB, data.clone(), byte[].class, metadata);
  }

  /**
   * Creates a complex value for string data.
   *
   * @param string the string
   * @return a new WasmComplexValue
   * @throws IllegalArgumentException if string is null
   */
  public static WasmComplexValue string(final String string) {
    Objects.requireNonNull(string, "String cannot be null");

    final MarshalingMetadata metadata =
        MarshalingMetadata.builder()
            .withDataSize(string.length())
            .withStringEncoding("UTF-8")
            .build();

    return new WasmComplexValue(ComplexType.STRING_DATA, string, String.class, metadata);
  }

  /**
   * Creates a null reference complex value.
   *
   * @param expectedType the expected type for the null reference
   * @return a new WasmComplexValue representing null
   */
  public static WasmComplexValue nullRef(final Class<?> expectedType) {
    Objects.requireNonNull(expectedType, "Expected type cannot be null");

    final MarshalingMetadata metadata = MarshalingMetadata.builder().build();

    return new WasmComplexValue(ComplexType.NULL_REF, null, expectedType, metadata);
  }

  /**
   * Validates that this complex value is compatible with the expected type.
   *
   * @param expectedType the expected Java type
   * @throws WasmException if types are incompatible
   */
  public void validateCompatibility(final Class<?> expectedType) throws WasmException {
    Objects.requireNonNull(expectedType, "Expected type cannot be null");

    if (isNull()) {
      // Null values are compatible with any reference type
      return;
    }

    if (!expectedType.isAssignableFrom(javaType)) {
      throw new WasmException(
          "Type incompatibility: expected "
              + expectedType.getName()
              + ", got "
              + javaType.getName());
    }
  }

  /**
   * Gets the number of dimensions in an array type.
   *
   * @param arrayType the array type
   * @return the number of dimensions
   */
  private static int getArrayDimensions(final Class<?> arrayType) {
    int dimensions = 0;
    Class<?> currentType = arrayType;
    while (currentType.isArray()) {
      dimensions++;
      currentType = currentType.getComponentType();
    }
    return dimensions;
  }

  /**
   * Gets the base component type of a multi-dimensional array.
   *
   * @param arrayType the array type
   * @return the base component type
   */
  private static Class<?> getArrayBaseComponentType(final Class<?> arrayType) {
    Class<?> componentType = arrayType;
    while (componentType.isArray()) {
      componentType = componentType.getComponentType();
    }
    return componentType;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WasmComplexValue other = (WasmComplexValue) obj;
    return complexType == other.complexType
        && Objects.equals(javaType, other.javaType)
        && deepEquals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(complexType, javaType, deepHashCode(value));
  }

  @Override
  public String toString() {
    if (isNull()) {
      return "WasmComplexValue{type="
          + complexType
          + ", value=null, javaType="
          + javaType.getSimpleName()
          + "}";
    }

    final String valueStr =
        complexType == ComplexType.BINARY_BLOB
            ? "byte[" + ((byte[]) value).length + "]"
            : value.toString();

    return String.format(
        "WasmComplexValue{type=%s, value=%s, javaType=%s}",
        complexType, valueStr, javaType.getSimpleName());
  }

  /**
   * Performs deep equality comparison for values.
   *
   * @param value1 the first value
   * @param value2 the second value
   * @return true if values are deeply equal
   */
  private static boolean deepEquals(final Object value1, final Object value2) {
    if (value1 == value2) {
      return true;
    }
    if (value1 == null || value2 == null) {
      return false;
    }
    if (value1.getClass().isArray() && value2.getClass().isArray()) {
      if (value1 instanceof byte[] && value2 instanceof byte[]) {
        return Arrays.equals((byte[]) value1, (byte[]) value2);
      }
      return Arrays.deepEquals((Object[]) value1, (Object[]) value2);
    }
    return Objects.equals(value1, value2);
  }

  /**
   * Computes deep hash code for values.
   *
   * @param value the value
   * @return the deep hash code
   */
  private static int deepHashCode(final Object value) {
    if (value == null) {
      return 0;
    }
    if (value.getClass().isArray()) {
      if (value instanceof byte[]) {
        return Arrays.hashCode((byte[]) value);
      }
      return Arrays.deepHashCode((Object[]) value);
    }
    return value.hashCode();
  }
}
