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

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.MEMORY;
  }
}
