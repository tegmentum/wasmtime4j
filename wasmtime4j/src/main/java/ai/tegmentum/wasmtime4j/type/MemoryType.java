package ai.tegmentum.wasmtime4j.type;

/**
 * Represents the type information of a WebAssembly memory.
 *
 * <p>This interface provides access to memory type metadata including minimum and maximum page
 * counts. Memory pages are 64KB each in WebAssembly.
 *
 * @since 1.0.0
 */
public interface MemoryType extends WasmType {

  /**
   * Gets the minimum number of memory pages required.
   *
   * @return the minimum page count
   */
  long getMinimum();

  /**
   * Gets the maximum number of memory pages allowed.
   *
   * @return the maximum page count, or empty if unlimited
   */
  java.util.Optional<Long> getMaximum();

  /**
   * Checks if this memory is 64-bit addressable.
   *
   * @return true if 64-bit, false if 32-bit
   */
  boolean is64Bit();

  /**
   * Checks if this memory is shared between threads.
   *
   * @return true if shared, false if private
   */
  boolean isShared();

  /**
   * Gets the page size for this memory type in bytes.
   *
   * <p>The standard WebAssembly page size is 65536 bytes (64KB). Custom page sizes are a
   * WebAssembly proposal that allows smaller page sizes.
   *
   * @return the page size in bytes
   * @since 1.1.0
   */
  default long getPageSize() {
    return 65536L;
  }

  /**
   * Gets the log2 of the page size for this memory type.
   *
   * <p>For the standard 64KB page size, this returns 16 (2^16 = 65536).
   *
   * @return the log2 of the page size
   * @since 1.1.0
   */
  default int getPageSizeLog2() {
    return 16;
  }

  /**
   * Creates a MemoryType with the specified minimum and maximum page counts.
   *
   * @param min the minimum page count
   * @param max the maximum page count, or empty if unlimited
   * @return a new MemoryType
   * @throws IllegalArgumentException if min is negative or max is null
   */
  static MemoryType of(final long min, final java.util.OptionalLong max) {
    if (min < 0) {
      throw new IllegalArgumentException("min cannot be negative");
    }
    if (max == null) {
      throw new IllegalArgumentException("max cannot be null; use OptionalLong.empty()");
    }
    final Long maxValue = max.isPresent() ? max.getAsLong() : null;
    return new DefaultMemoryType(min, maxValue, false, false, 65536L);
  }

  /**
   * Creates a 64-bit addressable memory type with the specified minimum and maximum page counts.
   *
   * <p>This is a convenience factory for creating memory types with {@code is64Bit} set to true,
   * corresponding to the memory64 proposal.
   *
   * @param min the minimum page count
   * @param max the maximum page count, or empty if unlimited
   * @return a new 64-bit MemoryType
   * @throws IllegalArgumentException if min is negative or max is null
   */
  static MemoryType memory64(final long min, final java.util.OptionalLong max) {
    if (min < 0) {
      throw new IllegalArgumentException("min cannot be negative");
    }
    if (max == null) {
      throw new IllegalArgumentException("max cannot be null; use OptionalLong.empty()");
    }
    final Long maxValue = max.isPresent() ? max.getAsLong() : null;
    return new DefaultMemoryType(min, maxValue, true, false, 65536L);
  }

  /**
   * Creates a shared memory type with the specified minimum and maximum page counts.
   *
   * <p>Shared memories are required to have a maximum size. This is a convenience factory for
   * creating memory types with {@code shared} set to true.
   *
   * @param min the minimum page count
   * @param max the maximum page count (required for shared memories)
   * @return a new shared MemoryType
   * @throws IllegalArgumentException if min is negative or max is less than min
   */
  static MemoryType shared(final long min, final long max) {
    if (min < 0) {
      throw new IllegalArgumentException("min cannot be negative");
    }
    if (max < min) {
      throw new IllegalArgumentException("max (" + max + ") cannot be less than min (" + min + ")");
    }
    return new DefaultMemoryType(min, max, false, true, 65536L);
  }

  /**
   * Creates a new MemoryTypeBuilder for constructing MemoryType instances.
   *
   * @return a new builder
   */
  static MemoryTypeBuilder builder() {
    return new MemoryTypeBuilder();
  }

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.MEMORY;
  }
}
