package ai.tegmentum.wasmtime4j;

import java.util.Objects;
import java.util.Optional;

/**
 * Metadata container for complex parameter marshaling operations.
 *
 * <p>This class provides additional context and configuration data needed for complex marshaling
 * operations, including type information, size hints, and serialization parameters.
 *
 * @since 1.0.0
 */
public final class MarshalingMetadata {

  private final Integer arrayDimensions;
  private final Class<?> componentType;
  private final Integer collectionSize;
  private final Class<?> keyType;
  private final Class<?> valueType;
  private final String objectClassName;
  private final Integer dataSize;
  private final String stringEncoding;
  private final Boolean useMemoryPassing;
  private final Integer memoryAlignment;

  private MarshalingMetadata(final Builder builder) {
    this.arrayDimensions = builder.arrayDimensions;
    this.componentType = builder.componentType;
    this.collectionSize = builder.collectionSize;
    this.keyType = builder.keyType;
    this.valueType = builder.valueType;
    this.objectClassName = builder.objectClassName;
    this.dataSize = builder.dataSize;
    this.stringEncoding = builder.stringEncoding;
    this.useMemoryPassing = builder.useMemoryPassing;
    this.memoryAlignment = builder.memoryAlignment;
  }

  /**
   * Gets the number of array dimensions.
   *
   * @return optional array dimensions
   */
  public Optional<Integer> getArrayDimensions() {
    return Optional.ofNullable(arrayDimensions);
  }

  /**
   * Gets the component type for arrays and collections.
   *
   * @return optional component type
   */
  public Optional<Class<?>> getComponentType() {
    return Optional.ofNullable(componentType);
  }

  /**
   * Gets the size of collections.
   *
   * @return optional collection size
   */
  public Optional<Integer> getCollectionSize() {
    return Optional.ofNullable(collectionSize);
  }

  /**
   * Gets the key type for maps.
   *
   * @return optional key type
   */
  public Optional<Class<?>> getKeyType() {
    return Optional.ofNullable(keyType);
  }

  /**
   * Gets the value type for maps.
   *
   * @return optional value type
   */
  public Optional<Class<?>> getValueType() {
    return Optional.ofNullable(valueType);
  }

  /**
   * Gets the class name for custom objects.
   *
   * @return optional object class name
   */
  public Optional<String> getObjectClassName() {
    return Optional.ofNullable(objectClassName);
  }

  /**
   * Gets the data size for binary data and strings.
   *
   * @return optional data size
   */
  public Optional<Integer> getDataSize() {
    return Optional.ofNullable(dataSize);
  }

  /**
   * Gets the string encoding for string data.
   *
   * @return optional string encoding
   */
  public Optional<String> getStringEncoding() {
    return Optional.ofNullable(stringEncoding);
  }

  /**
   * Checks if memory-based passing should be used.
   *
   * @return optional memory passing flag
   */
  public Optional<Boolean> shouldUseMemoryPassing() {
    return Optional.ofNullable(useMemoryPassing);
  }

  /**
   * Gets the memory alignment requirement.
   *
   * @return optional memory alignment
   */
  public Optional<Integer> getMemoryAlignment() {
    return Optional.ofNullable(memoryAlignment);
  }

  /**
   * Creates a new builder for marshaling metadata.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for creating MarshalingMetadata instances. */
  public static final class Builder {
    private Integer arrayDimensions;
    private Class<?> componentType;
    private Integer collectionSize;
    private Class<?> keyType;
    private Class<?> valueType;
    private String objectClassName;
    private Integer dataSize;
    private String stringEncoding;
    private Boolean useMemoryPassing;
    private Integer memoryAlignment;

    private Builder() {}

    /**
     * Sets the array dimensions.
     *
     * @param arrayDimensions the number of array dimensions
     * @return this builder
     */
    public Builder withArrayDimensions(final int arrayDimensions) {
      this.arrayDimensions = arrayDimensions;
      return this;
    }

    /**
     * Sets the component type for arrays and collections.
     *
     * @param componentType the component type
     * @return this builder
     */
    public Builder withComponentType(final Class<?> componentType) {
      this.componentType = componentType;
      return this;
    }

    /**
     * Sets the collection size.
     *
     * @param collectionSize the size of the collection
     * @return this builder
     */
    public Builder withCollectionSize(final int collectionSize) {
      this.collectionSize = collectionSize;
      return this;
    }

    /**
     * Sets the key type for maps.
     *
     * @param keyType the map key type
     * @return this builder
     */
    public Builder withKeyType(final Class<?> keyType) {
      this.keyType = keyType;
      return this;
    }

    /**
     * Sets the value type for maps.
     *
     * @param valueType the map value type
     * @return this builder
     */
    public Builder withValueType(final Class<?> valueType) {
      this.valueType = valueType;
      return this;
    }

    /**
     * Sets the object class name.
     *
     * @param objectClassName the fully qualified class name
     * @return this builder
     */
    public Builder withObjectClassName(final String objectClassName) {
      this.objectClassName = objectClassName;
      return this;
    }

    /**
     * Sets the data size.
     *
     * @param dataSize the size of the data in bytes
     * @return this builder
     */
    public Builder withDataSize(final int dataSize) {
      this.dataSize = dataSize;
      return this;
    }

    /**
     * Sets the string encoding.
     *
     * @param stringEncoding the encoding name (e.g., "UTF-8")
     * @return this builder
     */
    public Builder withStringEncoding(final String stringEncoding) {
      this.stringEncoding = stringEncoding;
      return this;
    }

    /**
     * Sets whether to use memory-based passing.
     *
     * @param useMemoryPassing true to use memory-based passing
     * @return this builder
     */
    public Builder withMemoryPassing(final boolean useMemoryPassing) {
      this.useMemoryPassing = useMemoryPassing;
      return this;
    }

    /**
     * Sets the memory alignment requirement.
     *
     * @param memoryAlignment the alignment in bytes (must be power of 2)
     * @return this builder
     * @throws IllegalArgumentException if alignment is not a power of 2
     */
    public Builder withMemoryAlignment(final int memoryAlignment) {
      if (memoryAlignment <= 0 || (memoryAlignment & (memoryAlignment - 1)) != 0) {
        throw new IllegalArgumentException("Memory alignment must be a positive power of 2");
      }
      this.memoryAlignment = memoryAlignment;
      return this;
    }

    /**
     * Builds the MarshalingMetadata instance.
     *
     * @return a new MarshalingMetadata instance
     */
    public MarshalingMetadata build() {
      return new MarshalingMetadata(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MarshalingMetadata other = (MarshalingMetadata) obj;
    return Objects.equals(arrayDimensions, other.arrayDimensions)
        && Objects.equals(componentType, other.componentType)
        && Objects.equals(collectionSize, other.collectionSize)
        && Objects.equals(keyType, other.keyType)
        && Objects.equals(valueType, other.valueType)
        && Objects.equals(objectClassName, other.objectClassName)
        && Objects.equals(dataSize, other.dataSize)
        && Objects.equals(stringEncoding, other.stringEncoding)
        && Objects.equals(useMemoryPassing, other.useMemoryPassing)
        && Objects.equals(memoryAlignment, other.memoryAlignment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        arrayDimensions,
        componentType,
        collectionSize,
        keyType,
        valueType,
        objectClassName,
        dataSize,
        stringEncoding,
        useMemoryPassing,
        memoryAlignment);
  }

  @Override
  public String toString() {
    return String.format(
        "MarshalingMetadata{arrayDimensions=%s, componentType=%s, collectionSize=%s, "
            + "keyType=%s, valueType=%s, objectClassName='%s', dataSize=%s, "
            + "stringEncoding='%s', useMemoryPassing=%s, memoryAlignment=%s}",
        arrayDimensions,
        componentType != null ? componentType.getSimpleName() : null,
        collectionSize,
        keyType != null ? keyType.getSimpleName() : null,
        valueType != null ? valueType.getSimpleName() : null,
        objectClassName,
        dataSize,
        stringEncoding,
        useMemoryPassing,
        memoryAlignment);
  }
}
