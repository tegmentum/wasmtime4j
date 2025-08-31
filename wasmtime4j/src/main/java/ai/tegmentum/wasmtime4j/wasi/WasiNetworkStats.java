package ai.tegmentum.wasmtime4j.wasi;

/**
 * Network operation statistics for WASI instances.
 *
 * @since 1.0.0
 */
public interface WasiNetworkStats {

  /**
   * Gets the number of network connections established.
   *
   * @return connection count
   */
  long getConnectionCount();

  /**
   * Gets the current number of open connections.
   *
   * @return current open connection count
   */
  int getCurrentConnections();

  /**
   * Gets the total bytes sent over the network.
   *
   * @return bytes sent
   */
  long getBytesSent();

  /**
   * Gets the total bytes received from the network.
   *
   * @return bytes received
   */
  long getBytesReceived();

  /**
   * Gets the number of network errors encountered.
   *
   * @return network error count
   */
  long getNetworkErrors();
}