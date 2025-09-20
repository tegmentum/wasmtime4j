package ai.tegmentum.wasmtime4j.serialization;

/**
 * Compression types available for module serialization.
 *
 * <p>Different compression types provide different trade-offs between compression ratio, speed, and
 * memory usage. The choice of compression type affects both serialization and deserialization
 * performance.
 *
 * @since 1.0.0
 */
public enum CompressionType {

  /**
   * No compression applied.
   *
   * <p>Fastest serialization and deserialization with largest output size. Recommended for
   * scenarios where speed is more important than storage space or network transfer time.
   */
  NONE("none", 0),

  /**
   * LZ4 compression.
   *
   * <p>Fast compression and decompression with moderate compression ratio. Good balance between
   * speed and size reduction. Suitable for most production scenarios where both performance and
   * storage efficiency are important.
   */
  LZ4("lz4", 1),

  /**
   * Zstandard (zstd) compression.
   *
   * <p>High compression ratio with reasonable speed. Best choice when storage space or network
   * transfer efficiency is critical. Slightly slower than LZ4 but provides better compression.
   */
  ZSTD("zstd", 2),

  /**
   * GZIP compression.
   *
   * <p>Widely supported compression format with good compression ratio. Slower than LZ4 and ZSTD
   * but provides good interoperability. Use when compatibility with external tools is required.
   */
  GZIP("gzip", 3);

  private final String name;
  private final int value;

  CompressionType(final String name, final int value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Gets the string name of this compression type.
   *
   * @return the compression type name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the numeric value of this compression type.
   *
   * @return the compression type value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets a CompressionType by its string name.
   *
   * @param name the compression type name
   * @return the corresponding CompressionType
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static CompressionType fromName(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Compression type name cannot be null");
    }

    for (final CompressionType type : values()) {
      if (type.name.equals(name)) {
        return type;
      }
    }

    throw new IllegalArgumentException("Unknown compression type: " + name);
  }

  /**
   * Gets a CompressionType by its numeric value.
   *
   * @param value the compression type value
   * @return the corresponding CompressionType
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static CompressionType fromValue(final int value) {
    for (final CompressionType type : values()) {
      if (type.value == value) {
        return type;
      }
    }

    throw new IllegalArgumentException("Unknown compression type value: " + value);
  }

  @Override
  public String toString() {
    return name;
  }
}