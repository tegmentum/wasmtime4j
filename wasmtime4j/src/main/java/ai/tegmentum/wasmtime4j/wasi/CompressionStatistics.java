package ai.tegmentum.wasmtime4j.wasi;

/**
 * Compression statistics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface CompressionStatistics {

  /**
   * Gets the compression algorithm used.
   *
   * @return algorithm name
   */
  String getAlgorithm();

  /**
   * Gets the compression level.
   *
   * @return compression level (1-9)
   */
  int getCompressionLevel();

  /**
   * Gets the original size in bytes.
   *
   * @return original size
   */
  long getOriginalSize();

  /**
   * Gets the compressed size in bytes.
   *
   * @return compressed size
   */
  long getCompressedSize();

  /**
   * Gets the compression ratio.
   *
   * @return compression ratio (0.0-1.0)
   */
  double getCompressionRatio();

  /**
   * Gets the space saved in bytes.
   *
   * @return space saved
   */
  long getSpaceSaved();

  /**
   * Gets the compression time in milliseconds.
   *
   * @return compression time
   */
  long getCompressionTime();

  /**
   * Gets the decompression time in milliseconds.
   *
   * @return decompression time
   */
  long getDecompressionTime();

  /**
   * Gets the compression throughput in MB/s.
   *
   * @return compression throughput
   */
  double getCompressionThroughput();

  /**
   * Gets the decompression throughput in MB/s.
   *
   * @return decompression throughput
   */
  double getDecompressionThroughput();

  /**
   * Gets the memory usage during compression.
   *
   * @return memory usage in bytes
   */
  long getMemoryUsage();

  /**
   * Gets compression quality metrics.
   *
   * @return quality metrics
   */
  QualityMetrics getQualityMetrics();

  /** Compression quality metrics interface. */
  interface QualityMetrics {
    /**
     * Gets the entropy before compression.
     *
     * @return entropy value
     */
    double getOriginalEntropy();

    /**
     * Gets the entropy after compression.
     *
     * @return entropy value
     */
    double getCompressedEntropy();

    /**
     * Gets the compression efficiency.
     *
     * @return efficiency rating (0.0-1.0)
     */
    double getEfficiency();

    /**
     * Checks if compression was beneficial.
     *
     * @return true if beneficial
     */
    boolean isBeneficial();
  }
}
