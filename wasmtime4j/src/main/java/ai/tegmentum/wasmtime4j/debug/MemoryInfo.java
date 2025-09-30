package ai.tegmentum.wasmtime4j.debug;

/**
 * Memory information interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface MemoryInfo {

  /**
   * Gets the memory base address.
   *
   * @return base address
   */
  long getBaseAddress();

  /**
   * Gets the memory size in bytes.
   *
   * @return memory size
   */
  long getSize();

  /**
   * Gets the memory page count.
   *
   * @return page count
   */
  int getPageCount();

  /**
   * Gets the memory maximum pages.
   *
   * @return maximum pages or -1 if unlimited
   */
  int getMaxPages();

  /**
   * Checks if the memory is shared.
   *
   * @return true if shared
   */
  boolean isShared();

  /**
   * Reads memory content.
   *
   * @param address address to read from
   * @param length number of bytes to read
   * @return byte array with memory content
   */
  byte[] readMemory(long address, int length);

  /**
   * Gets memory statistics.
   *
   * @return memory statistics
   */
  MemoryStatistics getStatistics();

  /** Memory statistics interface. */
  interface MemoryStatistics {
    /**
     * Gets the total allocations.
     *
     * @return allocation count
     */
    long getTotalAllocations();

    /**
     * Gets the current memory usage.
     *
     * @return usage in bytes
     */
    long getCurrentUsage();

    /**
     * Gets the peak memory usage.
     *
     * @return peak usage in bytes
     */
    long getPeakUsage();
  }
}
