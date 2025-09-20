package ai.tegmentum.wasmtime4j.wasi.impl;

import ai.tegmentum.wasmtime4j.exception.WasiResourceException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiResourcePermissions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Implementation of WasiResource for network socket resources.
 *
 * <p>This implementation provides WASI Preview 2 network resource management including TCP socket
 * operations, connection management, and network I/O with proper permission enforcement and
 * resource limits.
 *
 * @since 1.0.0
 */
public final class WasiSocketResourceImpl extends WasiGenericResourceImpl {

  private static final Logger LOGGER = Logger.getLogger(WasiSocketResourceImpl.class.getName());

  private final String host;
  private final int port;
  private final SocketType socketType;
  private final Set<WasiResourcePermissions> permissions;
  private final Duration connectionTimeout;

  private volatile SocketChannel socketChannel;
  private final AtomicBoolean connected = new AtomicBoolean(false);
  private final AtomicLong bytesReceived = new AtomicLong(0);
  private final AtomicLong bytesSent = new AtomicLong(0);
  private final AtomicLong connectionAttempts = new AtomicLong(0);
  private volatile Instant lastConnectionTime;

  /**
   * Creates a new network socket resource.
   *
   * @param id the unique resource identifier
   * @param name the resource name
   * @param config the resource configuration
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws WasiResourceException if socket setup fails
   */
  public WasiSocketResourceImpl(final long id, final String name, final WasiResourceConfig config) {
    super(id, name, "network", config);

    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    // Extract network-specific configuration
    this.host =
        (String)
            config
                .getProperty("host")
                .orElseThrow(() -> new IllegalArgumentException("Socket host must be specified"));

    final Object portObj =
        config
            .getProperty("port")
            .orElseThrow(() -> new IllegalArgumentException("Socket port must be specified"));
    this.port =
        portObj instanceof Integer ? (Integer) portObj : Integer.parseInt(portObj.toString());

    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
    }

    final String typeStr = (String) config.getProperty("socket_type").orElse("TCP");
    this.socketType = SocketType.fromName(typeStr);

    this.permissions =
        config.getPermissions().getClass().isAssignableFrom(Set.class)
            ? (Set<WasiResourcePermissions>) config.getPermissions()
            : WasiResourcePermissions.READ_WRITE;

    final Object timeoutObj = config.getProperty("connection_timeout").orElse(30);
    final long timeoutSeconds =
        timeoutObj instanceof Integer
            ? (Integer) timeoutObj
            : Long.parseLong(timeoutObj.toString());
    this.connectionTimeout = Duration.ofSeconds(timeoutSeconds);

    // Validate host format (basic validation)
    if (host.trim().isEmpty()) {
      throw new IllegalArgumentException("Host cannot be empty");
    }

    LOGGER.fine(
        "Created socket resource '"
            + name
            + "' for "
            + host
            + ":"
            + port
            + " ("
            + socketType
            + ")");
  }

  /**
   * Establishes a connection to the remote host.
   *
   * @throws WasmException if connection fails
   */
  public void connect() throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.WRITE);
    recordAccess();

    if (connected.get()) {
      throw new WasiResourceException("Socket is already connected");
    }

    connectionAttempts.incrementAndGet();

    try {
      socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(true);

      final SocketAddress address = new InetSocketAddress(host, port);

      // In a real implementation, this would handle timeouts properly
      final boolean connectionResult = socketChannel.connect(address);

      if (connectionResult) {
        connected.set(true);
        lastConnectionTime = Instant.now();
        LOGGER.fine("Connected to " + host + ":" + port);
      } else {
        throw new WasiResourceException("Failed to connect to " + host + ":" + port);
      }
    } catch (final IOException e) {
      if (socketChannel != null) {
        try {
          socketChannel.close();
        } catch (final IOException closeEx) {
          LOGGER.warning("Failed to close socket after connection error: " + closeEx.getMessage());
        }
        socketChannel = null;
      }
      throw new WasiResourceException("Connection failed: " + e.getMessage(), e);
    }
  }

  /**
   * Sends data through the socket.
   *
   * @param data the data to send
   * @return the number of bytes sent
   * @throws WasmException if sending fails
   */
  public int send(final byte[] data) throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.WRITE);
    recordAccess();

    if (!connected.get() || socketChannel == null) {
      throw new WasiResourceException("Socket is not connected");
    }

    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }

    try {
      final ByteBuffer buffer = ByteBuffer.wrap(data);
      final int bytesSentNow = socketChannel.write(buffer);

      if (bytesSentNow > 0) {
        bytesSent.addAndGet(bytesSentNow);
        LOGGER.fine("Sent " + bytesSentNow + " bytes");
      }

      return bytesSentNow;
    } catch (final IOException e) {
      throw new WasiResourceException("Failed to send data: " + e.getMessage(), e);
    }
  }

  /**
   * Receives data from the socket.
   *
   * @param buffer the buffer to receive data into
   * @return the number of bytes received
   * @throws WasmException if receiving fails
   */
  public int receive(final byte[] buffer) throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.READ);
    recordAccess();

    if (!connected.get() || socketChannel == null) {
      throw new WasiResourceException("Socket is not connected");
    }

    if (buffer == null) {
      throw new IllegalArgumentException("Buffer cannot be null");
    }

    try {
      final ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length);
      final int bytesReceivedNow = socketChannel.read(byteBuffer);

      if (bytesReceivedNow > 0) {
        byteBuffer.flip();
        byteBuffer.get(buffer, 0, bytesReceivedNow);
        bytesReceived.addAndGet(bytesReceivedNow);
        LOGGER.fine("Received " + bytesReceivedNow + " bytes");
      }

      return bytesReceivedNow;
    } catch (final IOException e) {
      throw new WasiResourceException("Failed to receive data: " + e.getMessage(), e);
    }
  }

  /**
   * Disconnects the socket.
   *
   * @throws WasmException if disconnection fails
   */
  public void disconnect() throws WasmException {
    ensureValid();
    recordAccess();

    if (!connected.get()) {
      return; // Already disconnected
    }

    try {
      if (socketChannel != null) {
        socketChannel.close();
        socketChannel = null;
      }
      connected.set(false);
      LOGGER.fine("Disconnected from " + host + ":" + port);
    } catch (final IOException e) {
      throw new WasiResourceException("Failed to disconnect: " + e.getMessage(), e);
    }
  }

  /**
   * Gets socket-specific statistics.
   *
   * @return socket usage statistics
   */
  public SocketStats getSocketStats() {
    return new SocketStats() {
      @Override
      public long getBytesReceived() {
        return bytesReceived.get();
      }

      @Override
      public long getBytesSent() {
        return bytesSent.get();
      }

      @Override
      public long getConnectionAttempts() {
        return connectionAttempts.get();
      }

      @Override
      public boolean isConnected() {
        return connected.get();
      }

      @Override
      public String getRemoteAddress() {
        return host + ":" + port;
      }

      @Override
      public SocketType getSocketType() {
        return socketType;
      }

      @Override
      public Instant getLastConnectionTime() {
        return lastConnectionTime;
      }
    };
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }

    // Handle socket-specific operations first
    switch (operation.toLowerCase()) {
      case "connect":
        connect();
        return null;

      case "disconnect":
        disconnect();
        return null;

      case "send":
        if (parameters.length < 1) {
          throw new IllegalArgumentException("send requires data parameter");
        }
        return send((byte[]) parameters[0]);

      case "receive":
        if (parameters.length < 1) {
          throw new IllegalArgumentException("receive requires buffer parameter");
        }
        return receive((byte[]) parameters[0]);

      case "is_connected":
        return connected.get();

      case "get_stats":
        return getSocketStats();

      case "get_remote_address":
        return host + ":" + port;

      default:
        // Delegate to parent for common operations
        return super.invoke(operation, parameters);
    }
  }

  @Override
  protected void performCleanup() {
    // Close socket connection if still open
    if (connected.get() && socketChannel != null) {
      try {
        socketChannel.close();
        connected.set(false);
        LOGGER.fine("Closed socket connection during cleanup");
      } catch (final IOException e) {
        LOGGER.warning("Error closing socket during cleanup: " + e.getMessage());
      }
    }

    super.performCleanup();
    LOGGER.fine("Socket resource cleanup completed for " + getName());
  }

  /**
   * Ensures the resource has the specified permission.
   *
   * @param required the required permission
   * @throws WasiResourceException if permission is not granted
   */
  private void ensurePermission(final WasiResourcePermissions required)
      throws WasiResourceException {
    if (!permissions.contains(required)) {
      throw new WasiResourceException("Permission denied: " + required.getName());
    }
  }

  /** Enumeration of socket types. */
  public enum SocketType {
    TCP("tcp"),
    UDP("udp");

    private final String name;

    SocketType(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static SocketType fromName(final String name) {
      if (name == null) {
        return TCP; // Default
      }

      for (final SocketType type : values()) {
        if (type.name.equalsIgnoreCase(name.trim())) {
          return type;
        }
      }

      return TCP; // Default fallback
    }
  }

  /** Interface for socket-specific statistics. */
  public interface SocketStats {
    long getBytesReceived();

    long getBytesSent();

    long getConnectionAttempts();

    boolean isConnected();

    String getRemoteAddress();

    SocketType getSocketType();

    Instant getLastConnectionTime();
  }
}
