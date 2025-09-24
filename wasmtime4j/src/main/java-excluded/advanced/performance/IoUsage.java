package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.Objects;

/**
 * Detailed I/O usage information for monitoring disk and file system operations.
 *
 * <p>This class provides comprehensive I/O statistics including bytes read/written, operation
 * counts, timing information, and latency measurements.
 *
 * @since 1.0.0
 */
public final class IoUsage {
  private final long bytesRead;
  private final long bytesWritten;
  private final long readOperations;
  private final long writeOperations;
  private final Duration totalIoTime;
  private final double averageLatency;

  /**
   * Creates an I/O usage record.
   *
   * @param bytesRead total bytes read from disk
   * @param bytesWritten total bytes written to disk
   * @param readOperations number of read operations
   * @param writeOperations number of write operations
   * @param totalIoTime cumulative time spent in I/O operations
   * @param averageLatency average I/O operation latency in milliseconds
   */
  public IoUsage(
      final long bytesRead,
      final long bytesWritten,
      final long readOperations,
      final long writeOperations,
      final Duration totalIoTime,
      final double averageLatency) {
    this.bytesRead = Math.max(0, bytesRead);
    this.bytesWritten = Math.max(0, bytesWritten);
    this.readOperations = Math.max(0, readOperations);
    this.writeOperations = Math.max(0, writeOperations);
    this.totalIoTime = Objects.requireNonNull(totalIoTime, "totalIoTime cannot be null");
    this.averageLatency = Math.max(0.0, averageLatency);

    if (totalIoTime.isNegative()) {
      throw new IllegalArgumentException("totalIoTime cannot be negative: " + totalIoTime);
    }
  }

  /**
   * Gets the total bytes read from disk.
   *
   * @return bytes read
   */
  public long getBytesRead() {
    return bytesRead;
  }

  /**
   * Gets the total bytes written to disk.
   *
   * @return bytes written
   */
  public long getBytesWritten() {
    return bytesWritten;
  }

  /**
   * Gets the number of read operations.
   *
   * @return read operation count
   */
  public long getReadOperations() {
    return readOperations;
  }

  /**
   * Gets the number of write operations.
   *
   * @return write operation count
   */
  public long getWriteOperations() {
    return writeOperations;
  }

  /**
   * Gets the cumulative time spent in I/O operations.
   *
   * @return total I/O time
   */
  public Duration getTotalIoTime() {
    return totalIoTime;
  }

  /**
   * Gets the average I/O operation latency.
   *
   * @return average latency in milliseconds
   */
  public double getAverageLatency() {
    return averageLatency;
  }

  /**
   * Gets the total number of I/O operations.
   *
   * @return total operation count
   */
  public long getTotalOperations() {
    return readOperations + writeOperations;
  }

  /**
   * Gets the total bytes transferred (read + written).
   *
   * @return total bytes transferred
   */
  public long getTotalBytesTransferred() {
    return bytesRead + bytesWritten;
  }

  /**
   * Gets the read throughput in bytes per second.
   *
   * @param measurementWindow the time window for rate calculation
   * @return read throughput
   */
  public double getReadThroughput(final Duration measurementWindow) {
    if (measurementWindow.isZero()) {
      return 0.0;
    }
    final double seconds = measurementWindow.toNanos() / 1_000_000_000.0;
    return bytesRead / seconds;
  }

  /**
   * Gets the write throughput in bytes per second.
   *
   * @param measurementWindow the time window for rate calculation
   * @return write throughput
   */
  public double getWriteThroughput(final Duration measurementWindow) {
    if (measurementWindow.isZero()) {
      return 0.0;
    }
    final double seconds = measurementWindow.toNanos() / 1_000_000_000.0;
    return bytesWritten / seconds;
  }

  /**
   * Gets the I/O operations per second.
   *
   * @param measurementWindow the time window for rate calculation
   * @return operations per second
   */
  public double getOperationsPerSecond(final Duration measurementWindow) {
    if (measurementWindow.isZero()) {
      return 0.0;
    }
    final double seconds = measurementWindow.toNanos() / 1_000_000_000.0;
    return getTotalOperations() / seconds;
  }

  /**
   * Gets the average bytes per operation.
   *
   * @return average bytes per operation
   */
  public double getAverageBytesPerOperation() {
    final long totalOps = getTotalOperations();
    return totalOps > 0 ? (double) getTotalBytesTransferred() / totalOps : 0.0;
  }

  /**
   * Gets the read/write ratio.
   *
   * @return read operations / write operations
   */
  public double getReadWriteRatio() {
    return writeOperations > 0
        ? (double) readOperations / writeOperations
        : Double.POSITIVE_INFINITY;
  }

  /**
   * Checks if I/O load is high.
   *
   * <p>Returns true if average latency exceeds 50ms or operation rate is very high.
   *
   * @return true if I/O load is high
   */
  public boolean isHighIoLoad() {
    return averageLatency > 50.0; // More than 50ms average latency
  }

  /**
   * Checks if I/O is sequential or random based on operation patterns.
   *
   * <p>Returns true if operations suggest sequential access (large bytes per operation).
   *
   * @return true if likely sequential I/O
   */
  public boolean isSequentialIo() {
    final double avgBytes = getAverageBytesPerOperation();
    return avgBytes > 64 * 1024; // More than 64KB per operation suggests sequential
  }

  /**
   * Gets the I/O utilization relative to system capacity.
   *
   * <p>This is a rough estimate based on operation latency and throughput.
   *
   * @return I/O utilization estimate (0.0 to 1.0)
   */
  public double getUtilization() {
    // Simple heuristic: high latency or high operation rate indicates utilization
    double utilization = 0.0;

    // Latency-based utilization (50ms+ latency = high utilization)
    if (averageLatency > 50.0) {
      utilization = Math.min(1.0, averageLatency / 200.0); // Scale to 200ms max
    }

    return utilization;
  }

  /**
   * Gets the I/O efficiency score (0.0 to 1.0).
   *
   * <p>Higher scores indicate better I/O efficiency (high throughput, low latency).
   *
   * @param measurementWindow the time window for throughput calculation
   * @return I/O efficiency score
   */
  public double getEfficiencyScore(final Duration measurementWindow) {
    double score = 1.0;

    // Penalize high latency
    if (averageLatency > 100.0) {
      score -= 0.4;
    } else if (averageLatency > 50.0) {
      score -= 0.2;
    }

    // Penalize very small operations (inefficient)
    final double avgBytes = getAverageBytesPerOperation();
    if (avgBytes < 1024) { // Less than 1KB per operation
      score -= 0.3;
    } else if (avgBytes < 4096) { // Less than 4KB per operation
      score -= 0.1;
    }

    return Math.max(0.0, score);
  }

  /**
   * Formats byte count in human-readable format.
   *
   * @param bytes the number of bytes
   * @return formatted size string
   */
  public static String formatBytes(final long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    final int unit = 1024;
    final int exp = (int) (Math.log(bytes) / Math.log(unit));
    final String pre = "KMGTPE".charAt(exp - 1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final IoUsage ioUsage = (IoUsage) obj;
    return bytesRead == ioUsage.bytesRead
        && bytesWritten == ioUsage.bytesWritten
        && readOperations == ioUsage.readOperations
        && writeOperations == ioUsage.writeOperations
        && Double.compare(ioUsage.averageLatency, averageLatency) == 0
        && Objects.equals(totalIoTime, ioUsage.totalIoTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        bytesRead, bytesWritten, readOperations, writeOperations, totalIoTime, averageLatency);
  }

  @Override
  public String toString() {
    return String.format(
        "IoUsage{read=%s (%d ops), written=%s (%d ops), "
            + "totalTime=%s, avgLatency=%.1fms, avgBytesPerOp=%.0f}",
        formatBytes(bytesRead),
        readOperations,
        formatBytes(bytesWritten),
        writeOperations,
        totalIoTime,
        averageLatency,
        getAverageBytesPerOperation());
  }
}
