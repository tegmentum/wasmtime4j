package ai.tegmentum.wasmtime4j.gc;

/**
 * Result of a garbage collection operation.
 *
 * <p>This class contains statistics about a garbage collection cycle, including the number of
 * objects and bytes collected.
 */
public final class GcCollectionResult {
  /** Number of objects collected. */
  public long objectsCollected;

  /** Bytes collected. */
  public long bytesCollected;

  /** Default constructor initializing all fields to zero. */
  public GcCollectionResult() {
    this.objectsCollected = 0;
    this.bytesCollected = 0;
  }

  /**
   * Gets the number of objects collected.
   *
   * @return the number of objects collected
   */
  public long getObjectsCollected() {
    return objectsCollected;
  }

  /**
   * Gets the number of bytes collected.
   *
   * @return the number of bytes collected
   */
  public long getBytesCollected() {
    return bytesCollected;
  }

  @Override
  public String toString() {
    return String.format(
        "GcCollectionResult{objectsCollected=%d, bytesCollected=%d}",
        objectsCollected, bytesCollected);
  }
}
