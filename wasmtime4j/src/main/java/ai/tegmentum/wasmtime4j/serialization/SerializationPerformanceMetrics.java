/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.serialization;

/**
 * Performance metrics for WebAssembly module serialization operations.
 *
 * <p>This class captures detailed performance data about serialization and deserialization
 * operations to enable optimization and performance analysis.
 *
 * @since 1.0.0
 */
public final class SerializationPerformanceMetrics {

  // Timing metrics (in nanoseconds for precision)
  private final long serializationTimeNs;
  private final long deserializationTimeNs;
  private final long compressionTimeNs;
  private final long decompressionTimeNs;
  private final long hashCalculationTimeNs;

  // Throughput metrics
  private final double serializationThroughputMbps;
  private final double deserializationThroughputMbps;
  private final double compressionThroughputMbps;
  private final double decompressionThroughputMbps;

  // Memory usage metrics
  private final long peakMemoryUsageBytes;
  private final long avgMemoryUsageBytes;
  private final long tempMemoryAllocatedBytes;

  // CPU usage metrics
  private final double avgCpuUsagePercent;
  private final double peakCpuUsagePercent;

  // I/O metrics
  private final long bytesRead;
  private final long bytesWritten;
  private final long diskIoTimeNs;

  // Quality metrics
  private final double compressionEfficiency;
  private final double deserializationSpeedRatio; // deserialization_time / serialization_time

  /**
   * Creates performance metrics with all measurements.
   *
   * @param builder the metrics builder
   */
  private SerializationPerformanceMetrics(final Builder builder) {
    this.serializationTimeNs = builder.serializationTimeNs;
    this.deserializationTimeNs = builder.deserializationTimeNs;
    this.compressionTimeNs = builder.compressionTimeNs;
    this.decompressionTimeNs = builder.decompressionTimeNs;
    this.hashCalculationTimeNs = builder.hashCalculationTimeNs;
    this.serializationThroughputMbps = builder.serializationThroughputMbps;
    this.deserializationThroughputMbps = builder.deserializationThroughputMbps;
    this.compressionThroughputMbps = builder.compressionThroughputMbps;
    this.decompressionThroughputMbps = builder.decompressionThroughputMbps;
    this.peakMemoryUsageBytes = builder.peakMemoryUsageBytes;
    this.avgMemoryUsageBytes = builder.avgMemoryUsageBytes;
    this.tempMemoryAllocatedBytes = builder.tempMemoryAllocatedBytes;
    this.avgCpuUsagePercent = builder.avgCpuUsagePercent;
    this.peakCpuUsagePercent = builder.peakCpuUsagePercent;
    this.bytesRead = builder.bytesRead;
    this.bytesWritten = builder.bytesWritten;
    this.diskIoTimeNs = builder.diskIoTimeNs;
    this.compressionEfficiency = builder.compressionEfficiency;
    this.deserializationSpeedRatio = calculateDeserializationSpeedRatio();
  }

  // Getter methods

  public long getSerializationTimeNs() {
    return serializationTimeNs;
  }

  public long getSerializationTimeMs() {
    return serializationTimeNs / 1_000_000;
  }

  public long getDeserializationTimeNs() {
    return deserializationTimeNs;
  }

  public long getDeserializationTimeMs() {
    return deserializationTimeNs / 1_000_000;
  }

  public long getCompressionTimeNs() {
    return compressionTimeNs;
  }

  public long getCompressionTimeMs() {
    return compressionTimeNs / 1_000_000;
  }

  public long getDecompressionTimeNs() {
    return decompressionTimeNs;
  }

  public long getDecompressionTimeMs() {
    return decompressionTimeNs / 1_000_000;
  }

  public long getHashCalculationTimeNs() {
    return hashCalculationTimeNs;
  }

  public long getHashCalculationTimeMs() {
    return hashCalculationTimeNs / 1_000_000;
  }

  public double getSerializationThroughputMbps() {
    return serializationThroughputMbps;
  }

  public double getDeserializationThroughputMbps() {
    return deserializationThroughputMbps;
  }

  public double getCompressionThroughputMbps() {
    return compressionThroughputMbps;
  }

  public double getDecompressionThroughputMbps() {
    return decompressionThroughputMbps;
  }

  public long getPeakMemoryUsageBytes() {
    return peakMemoryUsageBytes;
  }

  public long getAvgMemoryUsageBytes() {
    return avgMemoryUsageBytes;
  }

  public long getTempMemoryAllocatedBytes() {
    return tempMemoryAllocatedBytes;
  }

  public double getAvgCpuUsagePercent() {
    return avgCpuUsagePercent;
  }

  public double getPeakCpuUsagePercent() {
    return peakCpuUsagePercent;
  }

  public long getBytesRead() {
    return bytesRead;
  }

  public long getBytesWritten() {
    return bytesWritten;
  }

  public long getDiskIoTimeNs() {
    return diskIoTimeNs;
  }

  public long getDiskIoTimeMs() {
    return diskIoTimeNs / 1_000_000;
  }

  public double getCompressionEfficiency() {
    return compressionEfficiency;
  }

  public double getDeserializationSpeedRatio() {
    return deserializationSpeedRatio;
  }

  /**
   * Gets the total operation time including all phases.
   *
   * @return total time in nanoseconds
   */
  public long getTotalOperationTimeNs() {
    return serializationTimeNs + compressionTimeNs + hashCalculationTimeNs + diskIoTimeNs;
  }

  /**
   * Gets the total operation time in milliseconds.
   *
   * @return total time in milliseconds
   */
  public long getTotalOperationTimeMs() {
    return getTotalOperationTimeNs() / 1_000_000;
  }

  /**
   * Gets the memory efficiency ratio (output size / peak memory usage).
   *
   * @return memory efficiency ratio
   */
  public double getMemoryEfficiencyRatio() {
    if (peakMemoryUsageBytes == 0) {
      return 0.0;
    }
    return (double) bytesWritten / peakMemoryUsageBytes;
  }

  /**
   * Calculates the deserialization speed ratio.
   *
   * @return the ratio of deserialization time to serialization time
   */
  private double calculateDeserializationSpeedRatio() {
    if (serializationTimeNs == 0) {
      return 1.0;
    }
    return (double) deserializationTimeNs / serializationTimeNs;
  }

  /**
   * Checks if the performance metrics indicate optimal performance.
   *
   * @return true if performance is considered optimal
   */
  public boolean isOptimalPerformance() {
    // Define optimal performance criteria
    final double maxAcceptableSerializationTimeMs = 1000.0; // 1 second
    final double minAcceptableThroughputMbps = 10.0; // 10 MB/s
    final double maxAcceptableMemoryOverhead = 2.0; // 2x memory usage

    return getSerializationTimeMs() <= maxAcceptableSerializationTimeMs
        && serializationThroughputMbps >= minAcceptableThroughputMbps
        && getMemoryEfficiencyRatio() >= (1.0 / maxAcceptableMemoryOverhead);
  }

  /**
   * Gets a human-readable performance summary.
   *
   * @return performance summary string
   */
  public String getPerformanceSummary() {
    return String.format(
        "Serialization: %dms (%.1f MB/s), Memory: %.1fMB peak, CPU: %.1f%% avg, "
            + "Compression: %.2fx efficiency, Deserialization ratio: %.2fx",
        getSerializationTimeMs(),
        serializationThroughputMbps,
        peakMemoryUsageBytes / 1024.0 / 1024.0,
        avgCpuUsagePercent,
        compressionEfficiency,
        deserializationSpeedRatio);
  }

  @Override
  public String toString() {
    return String.format(
        "SerializationPerformanceMetrics{serialization=%dms, throughput=%.1fMB/s, "
            + "memory=%.1fMB, compression=%.2fx}",
        getSerializationTimeMs(),
        serializationThroughputMbps,
        peakMemoryUsageBytes / 1024.0 / 1024.0,
        compressionEfficiency);
  }

  /** Builder for creating SerializationPerformanceMetrics instances. */
  public static final class Builder {
    private long serializationTimeNs = 0;
    private long deserializationTimeNs = 0;
    private long compressionTimeNs = 0;
    private long decompressionTimeNs = 0;
    private long hashCalculationTimeNs = 0;
    private double serializationThroughputMbps = 0.0;
    private double deserializationThroughputMbps = 0.0;
    private double compressionThroughputMbps = 0.0;
    private double decompressionThroughputMbps = 0.0;
    private long peakMemoryUsageBytes = 0;
    private long avgMemoryUsageBytes = 0;
    private long tempMemoryAllocatedBytes = 0;
    private double avgCpuUsagePercent = 0.0;
    private double peakCpuUsagePercent = 0.0;
    private long bytesRead = 0;
    private long bytesWritten = 0;
    private long diskIoTimeNs = 0;
    private double compressionEfficiency = 1.0;

    /**
     * Sets timing metrics for serialization operations.
     *
     * @param serializationNs serialization time in nanoseconds
     * @param deserializationNs deserialization time in nanoseconds
     * @param compressionNs compression time in nanoseconds
     * @param decompressionNs decompression time in nanoseconds
     * @param hashNs hash calculation time in nanoseconds
     * @return this builder
     */
    public Builder setTimingMetrics(
        final long serializationNs,
        final long deserializationNs,
        final long compressionNs,
        final long decompressionNs,
        final long hashNs) {
      this.serializationTimeNs =
          requireNonNegative(serializationNs, "Serialization time cannot be negative");
      this.deserializationTimeNs =
          requireNonNegative(deserializationNs, "Deserialization time cannot be negative");
      this.compressionTimeNs =
          requireNonNegative(compressionNs, "Compression time cannot be negative");
      this.decompressionTimeNs =
          requireNonNegative(decompressionNs, "Decompression time cannot be negative");
      this.hashCalculationTimeNs =
          requireNonNegative(hashNs, "Hash calculation time cannot be negative");
      return this;
    }

    /**
     * Sets throughput metrics for serialization operations.
     *
     * @param serializationMbps serialization throughput in MB/s
     * @param deserializationMbps deserialization throughput in MB/s
     * @param compressionMbps compression throughput in MB/s
     * @param decompressionMbps decompression throughput in MB/s
     * @return this builder
     */
    public Builder setThroughputMetrics(
        final double serializationMbps,
        final double deserializationMbps,
        final double compressionMbps,
        final double decompressionMbps) {
      this.serializationThroughputMbps =
          requireNonNegative(serializationMbps, "Serialization throughput cannot be negative");
      this.deserializationThroughputMbps =
          requireNonNegative(deserializationMbps, "Deserialization throughput cannot be negative");
      this.compressionThroughputMbps =
          requireNonNegative(compressionMbps, "Compression throughput cannot be negative");
      this.decompressionThroughputMbps =
          requireNonNegative(decompressionMbps, "Decompression throughput cannot be negative");
      return this;
    }

    /**
     * Sets memory usage metrics for serialization operations.
     *
     * @param peakBytes peak memory usage in bytes
     * @param avgBytes average memory usage in bytes
     * @param tempBytes temporary memory allocated in bytes
     * @return this builder
     */
    public Builder setMemoryMetrics(
        final long peakBytes, final long avgBytes, final long tempBytes) {
      this.peakMemoryUsageBytes =
          requireNonNegative(peakBytes, "Peak memory usage cannot be negative");
      this.avgMemoryUsageBytes =
          requireNonNegative(avgBytes, "Average memory usage cannot be negative");
      this.tempMemoryAllocatedBytes =
          requireNonNegative(tempBytes, "Temporary memory cannot be negative");
      return this;
    }

    /**
     * Sets CPU usage metrics for serialization operations.
     *
     * @param avgPercent average CPU usage percentage (0-100)
     * @param peakPercent peak CPU usage percentage (0-100)
     * @return this builder
     */
    public Builder setCpuMetrics(final double avgPercent, final double peakPercent) {
      this.avgCpuUsagePercent =
          requireInRange(avgPercent, 0.0, 100.0, "Average CPU usage must be between 0 and 100");
      this.peakCpuUsagePercent =
          requireInRange(peakPercent, 0.0, 100.0, "Peak CPU usage must be between 0 and 100");
      return this;
    }

    /**
     * Sets disk I/O metrics for serialization operations.
     *
     * @param read bytes read from disk
     * @param written bytes written to disk
     * @param ioTimeNs disk I/O time in nanoseconds
     * @return this builder
     */
    public Builder setIoMetrics(final long read, final long written, final long ioTimeNs) {
      this.bytesRead = requireNonNegative(read, "Bytes read cannot be negative");
      this.bytesWritten = requireNonNegative(written, "Bytes written cannot be negative");
      this.diskIoTimeNs = requireNonNegative(ioTimeNs, "Disk I/O time cannot be negative");
      return this;
    }

    /**
     * Sets compression efficiency metric.
     *
     * @param efficiency compression efficiency ratio (output/input size)
     * @return this builder
     */
    public Builder setCompressionEfficiency(final double efficiency) {
      this.compressionEfficiency =
          requirePositive(efficiency, "Compression efficiency must be positive");
      return this;
    }

    /**
     * Calculates and sets throughput based on data size and timing.
     *
     * @param dataSizeBytes the size of data processed
     * @return this builder
     */
    public Builder calculateThroughput(final long dataSizeBytes) {
      requireNonNegative(dataSizeBytes, "Data size cannot be negative");

      if (serializationTimeNs > 0) {
        final double dataSizeMb = dataSizeBytes / (1024.0 * 1024.0);
        final double serializationTimeSec = serializationTimeNs / 1_000_000_000.0;
        this.serializationThroughputMbps = dataSizeMb / serializationTimeSec;
      }

      if (deserializationTimeNs > 0) {
        final double dataSizeMb = dataSizeBytes / (1024.0 * 1024.0);
        final double deserializationTimeSec = deserializationTimeNs / 1_000_000_000.0;
        this.deserializationThroughputMbps = dataSizeMb / deserializationTimeSec;
      }

      return this;
    }

    public SerializationPerformanceMetrics build() {
      return new SerializationPerformanceMetrics(this);
    }

    private static long requireNonNegative(final long value, final String message) {
      if (value < 0) {
        throw new IllegalArgumentException(message);
      }
      return value;
    }

    private static double requireNonNegative(final double value, final String message) {
      if (value < 0.0) {
        throw new IllegalArgumentException(message);
      }
      return value;
    }

    private static double requirePositive(final double value, final String message) {
      if (value <= 0.0) {
        throw new IllegalArgumentException(message);
      }
      return value;
    }

    private static double requireInRange(
        final double value, final double min, final double max, final String message) {
      if (value < min || value > max) {
        throw new IllegalArgumentException(
            message + " (was: " + value + ", expected: " + min + "-" + max + ")");
      }
      return value;
    }
  }
}
