package ai.tegmentum.wasmtime4j.wasi;

/**
 * Deduplication statistics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DeduplicationStatistics {

  /**
   * Gets the total number of blocks processed.
   *
   * @return block count
   */
  long getTotalBlocks();

  /**
   * Gets the number of duplicate blocks found.
   *
   * @return duplicate block count
   */
  long getDuplicateBlocks();

  /**
   * Gets the number of unique blocks.
   *
   * @return unique block count
   */
  long getUniqueBlocks();

  /**
   * Gets the deduplication ratio.
   *
   * @return deduplication ratio (0.0-1.0)
   */
  double getDeduplicationRatio();

  /**
   * Gets the space saved in bytes.
   *
   * @return space saved
   */
  long getSpaceSaved();

  /**
   * Gets the original size in bytes.
   *
   * @return original size
   */
  long getOriginalSize();

  /**
   * Gets the deduplicated size in bytes.
   *
   * @return deduplicated size
   */
  long getDeduplicatedSize();

  /**
   * Gets the processing time in milliseconds.
   *
   * @return processing time
   */
  long getProcessingTime();

  /**
   * Gets the hash collision count.
   *
   * @return collision count
   */
  int getHashCollisions();

  /**
   * Gets the hash algorithm used.
   *
   * @return hash algorithm name
   */
  String getHashAlgorithm();

  /**
   * Gets block statistics.
   *
   * @return block statistics
   */
  BlockStatistics getBlockStatistics();

  /** Block statistics interface. */
  interface BlockStatistics {
    /**
     * Gets the average block size.
     *
     * @return average size in bytes
     */
    double getAverageBlockSize();

    /**
     * Gets the minimum block size.
     *
     * @return minimum size in bytes
     */
    int getMinBlockSize();

    /**
     * Gets the maximum block size.
     *
     * @return maximum size in bytes
     */
    int getMaxBlockSize();

    /**
     * Gets the most frequently duplicated block size.
     *
     * @return most common duplicate size
     */
    int getMostCommonDuplicateSize();
  }
}
