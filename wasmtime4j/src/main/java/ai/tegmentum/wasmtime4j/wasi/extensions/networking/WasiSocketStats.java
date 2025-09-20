package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Statistics and performance metrics for a WASI socket.
 *
 * <p>This class provides detailed information about socket usage, performance characteristics, and
 * current state. All values are cumulative since the socket was created.
 *
 * <p>Instances of this class are immutable and represent a snapshot of socket statistics at the
 * time of creation.
 *
 * @since 1.0.0
 */
public final class WasiSocketStats {

  private final long bytesRead;
  private final long bytesWritten;
  private final long packetsRead;
  private final long packetsWritten;
  private final long readOperations;
  private final long writeOperations;
  private final long connectTime;
  private final long lastActivityTime;
  private final int readBufferSize;
  private final int writeBufferSize;
  private final int availableBytes;
  private final boolean isBlocking;

  /**
   * Creates a new socket statistics snapshot.
   *
   * @param bytesRead total bytes read from socket
   * @param bytesWritten total bytes written to socket
   * @param packetsRead total packets read (for datagram sockets)
   * @param packetsWritten total packets written (for datagram sockets)
   * @param readOperations total number of read operations
   * @param writeOperations total number of write operations
   * @param connectTime timestamp when connection was established (0 if not connected)
   * @param lastActivityTime timestamp of last I/O activity
   * @param readBufferSize current receive buffer size
   * @param writeBufferSize current send buffer size
   * @param availableBytes bytes available for reading without blocking
   * @param isBlocking whether socket is in blocking mode
   */
  public WasiSocketStats(
      final long bytesRead,
      final long bytesWritten,
      final long packetsRead,
      final long packetsWritten,
      final long readOperations,
      final long writeOperations,
      final long connectTime,
      final long lastActivityTime,
      final int readBufferSize,
      final int writeBufferSize,
      final int availableBytes,
      final boolean isBlocking) {
    this.bytesRead = bytesRead;
    this.bytesWritten = bytesWritten;
    this.packetsRead = packetsRead;
    this.packetsWritten = packetsWritten;
    this.readOperations = readOperations;
    this.writeOperations = writeOperations;
    this.connectTime = connectTime;
    this.lastActivityTime = lastActivityTime;
    this.readBufferSize = readBufferSize;
    this.writeBufferSize = writeBufferSize;
    this.availableBytes = availableBytes;
    this.isBlocking = isBlocking;
  }

  /**
   * Gets the total number of bytes read from this socket.
   *
   * @return total bytes read
   */
  public long getBytesRead() {
    return bytesRead;
  }

  /**
   * Gets the total number of bytes written to this socket.
   *
   * @return total bytes written
   */
  public long getBytesWritten() {
    return bytesWritten;
  }

  /**
   * Gets the total number of packets read (relevant for datagram sockets).
   *
   * @return total packets read
   */
  public long getPacketsRead() {
    return packetsRead;
  }

  /**
   * Gets the total number of packets written (relevant for datagram sockets).
   *
   * @return total packets written
   */
  public long getPacketsWritten() {
    return packetsWritten;
  }

  /**
   * Gets the total number of read operations performed.
   *
   * @return total read operations
   */
  public long getReadOperations() {
    return readOperations;
  }

  /**
   * Gets the total number of write operations performed.
   *
   * @return total write operations
   */
  public long getWriteOperations() {
    return writeOperations;
  }

  /**
   * Gets the timestamp when the connection was established.
   *
   * @return connection timestamp in milliseconds since epoch, or 0 if not connected
   */
  public long getConnectTime() {
    return connectTime;
  }

  /**
   * Gets the timestamp of the last I/O activity.
   *
   * @return last activity timestamp in milliseconds since epoch
   */
  public long getLastActivityTime() {
    return lastActivityTime;
  }

  /**
   * Gets the current receive buffer size.
   *
   * @return receive buffer size in bytes
   */
  public int getReadBufferSize() {
    return readBufferSize;
  }

  /**
   * Gets the current send buffer size.
   *
   * @return send buffer size in bytes
   */
  public int getWriteBufferSize() {
    return writeBufferSize;
  }

  /**
   * Gets the number of bytes available for reading without blocking.
   *
   * @return available bytes for reading
   */
  public int getAvailableBytes() {
    return availableBytes;
  }

  /**
   * Checks if the socket is in blocking mode.
   *
   * @return true if blocking, false if non-blocking
   */
  public boolean isBlocking() {
    return isBlocking;
  }

  /**
   * Calculates the average bytes per read operation.
   *
   * @return average bytes per read, or 0 if no reads performed
   */
  public double getAverageBytesPerRead() {
    return readOperations > 0 ? (double) bytesRead / readOperations : 0.0;
  }

  /**
   * Calculates the average bytes per write operation.
   *
   * @return average bytes per write, or 0 if no writes performed
   */
  public double getAverageBytesPerWrite() {
    return writeOperations > 0 ? (double) bytesWritten / writeOperations : 0.0;
  }

  /**
   * Calculates the total I/O throughput (bytes read + written).
   *
   * @return total throughput in bytes
   */
  public long getTotalThroughput() {
    return bytesRead + bytesWritten;
  }

  /**
   * Checks if the socket has been connected.
   *
   * @return true if connection time is set, false otherwise
   */
  public boolean hasBeenConnected() {
    return connectTime > 0;
  }

  @Override
  public String toString() {
    return "WasiSocketStats{"
        + "bytesRead="
        + bytesRead
        + ", bytesWritten="
        + bytesWritten
        + ", packetsRead="
        + packetsRead
        + ", packetsWritten="
        + packetsWritten
        + ", readOperations="
        + readOperations
        + ", writeOperations="
        + writeOperations
        + ", connectTime="
        + connectTime
        + ", lastActivityTime="
        + lastActivityTime
        + ", readBufferSize="
        + readBufferSize
        + ", writeBufferSize="
        + writeBufferSize
        + ", availableBytes="
        + availableBytes
        + ", isBlocking="
        + isBlocking
        + '}';
  }
}
