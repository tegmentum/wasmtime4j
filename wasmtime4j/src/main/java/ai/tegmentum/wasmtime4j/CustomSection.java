package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly custom section.
 *
 * <p>Custom sections contain arbitrary data embedded in WebAssembly modules for metadata,
 * debugging information, or tooling-specific purposes. They do not affect WebAssembly execution
 * semantics and can be safely ignored by implementations.
 *
 * @since 1.0.0
 */
public final class CustomSection {

  private final String name;
  private final byte[] data;
  private final CustomSectionType type;

  /**
   * Creates a new custom section.
   *
   * @param name the name of the custom section
   * @param data the raw binary data of the section
   * @param type the type of the custom section
   * @throws IllegalArgumentException if name is null or empty, data is null, or type is null
   */
  public CustomSection(final String name, final byte[] data, final CustomSectionType type) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Custom section name cannot be null or empty");
    }
    if (data == null) {
      throw new IllegalArgumentException("Custom section data cannot be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Custom section type cannot be null");
    }

    this.name = name;
    this.data = data.clone(); // Defensive copy
    this.type = type;
  }

  /**
   * Gets the name of this custom section.
   *
   * @return the section name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the raw binary data of this custom section.
   *
   * @return a copy of the section data
   */
  public byte[] getData() {
    return data.clone(); // Defensive copy
  }

  /**
   * Gets the size of this custom section in bytes.
   *
   * @return the data size in bytes
   */
  public int getSize() {
    return data.length;
  }

  /**
   * Gets the type of this custom section.
   *
   * @return the section type
   */
  public CustomSectionType getType() {
    return type;
  }

  /**
   * Checks if this custom section is empty.
   *
   * @return true if the section data is empty
   */
  public boolean isEmpty() {
    return data.length == 0;
  }

  /**
   * Creates a custom section with unknown type.
   *
   * @param name the section name
   * @param data the section data
   * @return a new custom section
   * @throws IllegalArgumentException if name or data is invalid
   */
  public static CustomSection createUnknown(final String name, final byte[] data) {
    return new CustomSection(name, data, CustomSectionType.UNKNOWN);
  }

  @Override
  public String toString() {
    return String.format("CustomSection{name='%s', type=%s, size=%d}", name, type, data.length);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CustomSection that = (CustomSection) obj;
    return name.equals(that.name)
        && java.util.Arrays.equals(data, that.data)
        && type == that.type;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + java.util.Arrays.hashCode(data);
    result = 31 * result + type.hashCode();
    return result;
  }
}