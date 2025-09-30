package ai.tegmentum.wasmtime4j;

/**
 * Result of a WebAssembly component garbage collection.
 *
 * @since 1.0.0
 */
public interface ComponentGarbageCollectionResult {

  /**
   * Gets the GC result status.
   *
   * @return the status (SUCCESS, FAILED, PARTIAL)
   */
  String getStatus();

  /**
   * Gets the amount of memory reclaimed in bytes.
   *
   * @return the memory reclaimed in bytes
   */
  long getMemoryReclaimed();

  /**
   * Gets the GC duration in milliseconds.
   *
   * @return the duration in milliseconds
   */
  long getDuration();

  /**
   * Gets the number of objects collected.
   *
   * @return the object count
   */
  long getObjectsCollected();
}
