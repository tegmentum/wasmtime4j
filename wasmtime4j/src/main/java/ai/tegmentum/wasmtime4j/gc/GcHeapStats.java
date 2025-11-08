package ai.tegmentum.wasmtime4j.gc;

/**
 * Simple heap statistics returned by native GC operations.
 *
 * <p>This class provides basic heap statistics from the WebAssembly GC runtime, including total
 * allocated bytes and current heap size.
 *
 * @since 1.0.0
 */
public final class GcHeapStats {
  /** Total objects allocated. */
  public long totalAllocated;

  /** Current heap size in bytes. */
  public long currentHeapSize;

  /** Number of major collections performed. */
  public long majorCollections;

  /** Default constructor for JNI instantiation. */
  public GcHeapStats() {
    this.totalAllocated = 0;
    this.currentHeapSize = 0;
    this.majorCollections = 0;
  }

  /**
   * Gets the total number of bytes allocated.
   *
   * @return total bytes allocated
   */
  public long getTotalAllocated() {
    return totalAllocated;
  }

  /**
   * Gets the current heap size in bytes.
   *
   * @return current heap size
   */
  public long getCurrentHeapSize() {
    return currentHeapSize;
  }

  /**
   * Gets the number of major collections performed.
   *
   * @return number of major collections
   */
  public long getMajorCollections() {
    return majorCollections;
  }

  @Override
  public String toString() {
    return String.format(
        "GcHeapStats{totalAllocated=%d, currentHeapSize=%d, majorCollections=%d}",
        totalAllocated, currentHeapSize, majorCollections);
  }
}
