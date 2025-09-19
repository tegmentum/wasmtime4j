package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.Objects;

/**
 * Detailed network usage information for monitoring network I/O operations.
 *
 * <p>This class provides comprehensive network statistics including bytes transferred,
 * packet counts, connection information, and latency measurements.
 *
 * @since 1.0.0
 */
public final class NetworkUsage {
  private final long bytesReceived;
  private final long bytesSent;
  private final long packetsReceived;
  private final long packetsSent;
  private final int activeConnections;
  private final Duration averageConnectionTime;

  /**
   * Creates a network usage record.
   *
   * @param bytesReceived total bytes received from network
   * @param bytesSent total bytes sent to network
   * @param packetsReceived total packets received
   * @param packetsSent total packets sent
   * @param activeConnections number of currently active connections
   * @param averageConnectionTime average connection duration
   */
  public NetworkUsage(
      final long bytesReceived,
      final long bytesSent,
      final long packetsReceived,
      final long packetsSent,
      final int activeConnections,
      final Duration averageConnectionTime) {
    this.bytesReceived = Math.max(0, bytesReceived);
    this.bytesSent = Math.max(0, bytesSent);
    this.packetsReceived = Math.max(0, packetsReceived);
    this.packetsSent = Math.max(0, packetsSent);
    this.activeConnections = Math.max(0, activeConnections);
    this.averageConnectionTime = Objects.requireNonNull(averageConnectionTime, "averageConnectionTime cannot be null");

    if (averageConnectionTime.isNegative()) {
      throw new IllegalArgumentException("averageConnectionTime cannot be negative: " + averageConnectionTime);
    }
  }

  /**
   * Gets the total bytes received from network.
   *
   * @return bytes received
   */
  public long getBytesReceived() {
    return bytesReceived;
  }

  /**
   * Gets the total bytes sent to network.
   *
   * @return bytes sent
   */
  public long getBytesSent() {
    return bytesSent;
  }

  /**
   * Gets the total packets received.
   *
   * @return packets received
   */
  public long getPacketsReceived() {
    return packetsReceived;
  }

  /**
   * Gets the total packets sent.
   *
   * @return packets sent
   */
  public long getPacketsSent() {
    return packetsSent;
  }

  /**
   * Gets the number of currently active connections.
   *
   * @return active connection count
   */
  public int getActiveConnections() {
    return activeConnections;
  }

  /**
   * Gets the average connection duration.
   *
   * @return average connection time
   */
  public Duration getAverageConnectionTime() {
    return averageConnectionTime;
  }

  /**
   * Gets the total bytes transferred (sent + received).
   *
   * @return total bytes transferred
   */
  public long getTotalBytesTransferred() {
    return bytesReceived + bytesSent;
  }

  /**
   * Gets the total packets transferred (sent + received).
   *
   * @return total packets transferred
   */
  public long getTotalPacketsTransferred() {
    return packetsReceived + packetsSent;
  }

  /**
   * Gets the receive throughput in bytes per second.
   *
   * @param measurementWindow the time window for rate calculation
   * @return receive throughput
   */
  public double getReceiveThroughput(final Duration measurementWindow) {
    if (measurementWindow.isZero()) {
      return 0.0;
    }
    final double seconds = measurementWindow.toNanos() / 1_000_000_000.0;
    return bytesReceived / seconds;
  }

  /**
   * Gets the send throughput in bytes per second.
   *
   * @param measurementWindow the time window for rate calculation
   * @return send throughput
   */
  public double getSendThroughput(final Duration measurementWindow) {
    if (measurementWindow.isZero()) {
      return 0.0;
    }
    final double seconds = measurementWindow.toNanos() / 1_000_000_000.0;
    return bytesSent / seconds;
  }

  /**
   * Gets the packet rate (packets per second).
   *
   * @param measurementWindow the time window for rate calculation
   * @return packets per second
   */
  public double getPacketRate(final Duration measurementWindow) {
    if (measurementWindow.isZero()) {
      return 0.0;
    }
    final double seconds = measurementWindow.toNanos() / 1_000_000_000.0;
    return getTotalPacketsTransferred() / seconds;
  }

  /**
   * Gets the average bytes per packet.
   *
   * @return average bytes per packet
   */
  public double getAverageBytesPerPacket() {
    final long totalPackets = getTotalPacketsTransferred();
    return totalPackets > 0 ? (double) getTotalBytesTransferred() / totalPackets : 0.0;
  }

  /**
   * Gets the send/receive ratio for bytes.
   *
   * @return bytes sent / bytes received
   */
  public double getSendReceiveRatio() {
    return bytesReceived > 0 ? (double) bytesSent / bytesReceived : Double.POSITIVE_INFINITY;
  }

  /**
   * Checks if network usage pattern suggests bulk data transfer.
   *
   * <p>Returns true if average bytes per packet is high (> 1KB).
   *
   * @return true if bulk data transfer pattern
   */
  public boolean isBulkDataTransfer() {
    return getAverageBytesPerPacket() > 1024;
  }

  /**
   * Checks if network usage suggests interactive traffic.
   *
   * <p>Returns true if average bytes per packet is low (< 256 bytes) and connection time is short.
   *
   * @return true if interactive traffic pattern
   */
  public boolean isInteractiveTraffic() {
    return getAverageBytesPerPacket() < 256 &&
           averageConnectionTime.compareTo(Duration.ofMinutes(5)) < 0;
  }

  /**
   * Checks if there are too many concurrent connections.
   *
   * <p>Returns true if active connections exceed 100.
   *
   * @return true if high connection count
   */
  public boolean hasHighConnectionCount() {
    return activeConnections > 100;
  }

  /**
   * Gets the network efficiency score (0.0 to 1.0).
   *
   * <p>Higher scores indicate better network efficiency (good packet sizes, reasonable connection counts).
   *
   * @return network efficiency score
   */
  public double getEfficiencyScore() {
    double score = 1.0;

    // Penalize very small packets (inefficient)
    final double avgBytesPerPacket = getAverageBytesPerPacket();
    if (avgBytesPerPacket < 64) {
      score -= 0.3;
    } else if (avgBytesPerPacket < 256) {
      score -= 0.1;
    }

    // Penalize excessive connection count
    if (hasHighConnectionCount()) {
      score -= 0.2;
    }

    // Penalize very short connections (connection overhead)
    if (averageConnectionTime.compareTo(Duration.ofSeconds(1)) < 0) {
      score -= 0.2;
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
    final NetworkUsage that = (NetworkUsage) obj;
    return bytesReceived == that.bytesReceived &&
        bytesSent == that.bytesSent &&
        packetsReceived == that.packetsReceived &&
        packetsSent == that.packetsSent &&
        activeConnections == that.activeConnections &&
        Objects.equals(averageConnectionTime, that.averageConnectionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bytesReceived, bytesSent, packetsReceived, packetsSent,
                       activeConnections, averageConnectionTime);
  }

  @Override
  public String toString() {
    return String.format(
        "NetworkUsage{received=%s (%d packets), sent=%s (%d packets), " +
        "activeConnections=%d, avgConnectionTime=%s, avgBytesPerPacket=%.0f}",
        formatBytes(bytesReceived), packetsReceived, formatBytes(bytesSent), packetsSent,
        activeConnections, averageConnectionTime, getAverageBytesPerPacket());
  }
}