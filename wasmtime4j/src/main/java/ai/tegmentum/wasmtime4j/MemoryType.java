package ai.tegmentum.wasmtime4j;

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

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.MEMORY;
  }
}