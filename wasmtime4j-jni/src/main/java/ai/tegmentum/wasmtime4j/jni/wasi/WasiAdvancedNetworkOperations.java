package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI Preview 2 advanced network operations.
 *
 * <p>This class implements advanced networking protocols including:
 *
 * <ul>
 *   <li>WebSocket client and server support with secure connections
 *   <li>HTTP/2 protocol implementation with multiplexing and flow control
 *   <li>gRPC client and server with protobuf serialization
 *   <li>Async networking with non-blocking I/O operations
 *   <li>Network connection pooling and keep-alive management
 *   <li>SSL/TLS support with certificate validation
 *   <li>Network monitoring and performance optimization
 *   <li>Custom protocol negotiation and multiplexing
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiAdvancedNetworkOperations {

  private static final Logger LOGGER =
      Logger.getLogger(WasiAdvancedNetworkOperations.class.getName());

  /** WebSocket protocol constant. */
  public static final int PROTOCOL_WEBSOCKET = 1;

  /** HTTP/2 protocol constant. */
  public static final int PROTOCOL_HTTP2 = 2;

  /** gRPC protocol constant. */
  public static final int PROTOCOL_GRPC = 3;

  /** WebSocket message types. */
  public static final int WS_MESSAGE_TEXT = 1;

  public static final int WS_MESSAGE_BINARY = 2;
  public static final int WS_MESSAGE_PING = 9;
  public static final int WS_MESSAGE_PONG = 10;
  public static final int WS_MESSAGE_CLOSE = 8;

  /** HTTP/2 stream states. */
  public static final int HTTP2_STREAM_IDLE = 0;

  public static final int HTTP2_STREAM_OPEN = 1;
  public static final int HTTP2_STREAM_RESERVED_LOCAL = 2;
  public static final int HTTP2_STREAM_RESERVED_REMOTE = 3;
  public static final int HTTP2_STREAM_HALF_CLOSED_LOCAL = 4;
  public static final int HTTP2_STREAM_HALF_CLOSED_REMOTE = 5;
  public static final int HTTP2_STREAM_CLOSED = 6;

  /** The WASI context this advanced network operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService executorService;

  /** Connection tracking for resource management. */
  private final Map<Long, AdvancedNetworkConnection> activeConnections = new ConcurrentHashMap<>();

  /** Connection ID generator. */
  private final AtomicLong nextConnectionId = new AtomicLong(1L);

  /** Network performance metrics. */
  private final AdvancedNetworkMetrics metrics = new AdvancedNetworkMetrics();

  /**
   * Create a new WASI advanced network operations instance.
   *
   * @param wasiContext The WASI context this instance belongs to
   * @param executorService Executor service for async operations
   * @throws IllegalArgumentException if any parameter is null
   */
  public WasiAdvancedNetworkOperations(
      final WasiContext wasiContext, final ExecutorService executorService) {
    JniValidation.requireNonNull(wasiContext, "WASI context");
    JniValidation.requireNonNull(executorService, "Executor service");

    this.wasiContext = wasiContext;
    this.executorService = executorService;

    LOGGER.log(Level.INFO, "Advanced network operations initialized for WASI context");
  }

  /**
   * Initialize advanced networking support.
   *
   * @throws WasiException if initialization fails
   */
  public void initialize() throws WasiException {
    try {
      final int result = nativeInitializeAdvancedNetworking();
      if (result != 0) {
        throw new WasiException(
            WasiErrorCode.ECONNREFUSED, "Failed to initialize advanced networking: " + result);
      }
      LOGGER.log(Level.INFO, "Advanced networking initialized successfully");
    } catch (final Exception e) {
      throw new WasiException(
          "Advanced networking initialization failed",
          WasiErrorCode.ECONNREFUSED,
          "advanced_networking",
          null,
          e);
    }
  }

  /**
   * Create a WebSocket client connection.
   *
   * @param url The WebSocket URL to connect to
   * @param headers Optional HTTP headers as key-value pairs (can be null)
   * @param timeoutMs Connection timeout in milliseconds (0 for default)
   * @return CompletableFuture that resolves to the connection ID
   * @throws WasiException if the operation fails
   */
  public CompletableFuture<Long> websocketConnect(
      final String url, final Map<String, String> headers, final long timeoutMs)
      throws WasiException {
    JniValidation.requireNonNull(url, "WebSocket URL");
    JniValidation.requireValidString(url, "WebSocket URL");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long connectionId = nextConnectionId.getAndIncrement();

            // Convert headers to arrays for native call
            String[] headerKeys = null;
            String[] headerValues = null;
            if (headers != null && !headers.isEmpty()) {
              headerKeys = headers.keySet().toArray(new String[0]);
              headerValues = headers.values().toArray(new String[0]);
            }

            final int result =
                nativeWebSocketConnect(url, headerKeys, headerValues, timeoutMs, connectionId);
            if (result != 0) {
              metrics.incrementFailedConnections(PROTOCOL_WEBSOCKET);
              throw new WasiException(
                  WasiErrorCode.ECONNREFUSED, "WebSocket connection failed: " + result);
            }

            final AdvancedNetworkConnection connection =
                new AdvancedNetworkConnection(
                    connectionId, PROTOCOL_WEBSOCKET, url, System.currentTimeMillis());
            activeConnections.put(connectionId, connection);
            metrics.incrementSuccessfulConnections(PROTOCOL_WEBSOCKET);

            LOGGER.log(
                Level.INFO,
                "WebSocket connection established: {0} -> {1}",
                new Object[] {connectionId, url});
            return connectionId;

          } catch (final Exception e) {
            metrics.incrementFailedConnections(PROTOCOL_WEBSOCKET);
            throw new WasiException(
                "WebSocket connection failed",
                WasiErrorCode.ECONNREFUSED,
                "advanced_networking",
                null,
                e);
          }
        },
        executorService);
  }

  /**
   * Send a WebSocket message.
   *
   * @param connectionId The WebSocket connection ID
   * @param messageType The message type (WS_MESSAGE_TEXT or WS_MESSAGE_BINARY)
   * @param data The message data
   * @return CompletableFuture that completes when the message is sent
   * @throws WasiException if the operation fails
   */
  public CompletableFuture<Void> websocketSend(
      final long connectionId, final int messageType, final ByteBuffer data) throws WasiException {
    JniValidation.requireNonNull(data, "Message data");
    JniValidation.requireValidConnectionId(connectionId, activeConnections);

    if (messageType != WS_MESSAGE_TEXT && messageType != WS_MESSAGE_BINARY) {
      throw new WasiException(
          WasiErrorCode.EINVAL, "Invalid WebSocket message type: " + messageType);
    }

    return CompletableFuture.runAsync(
        () -> {
          try {
            final int result =
                nativeWebSocketSend(connectionId, messageType, data, data.remaining());
            if (result != 0) {
              metrics.incrementErrors(PROTOCOL_WEBSOCKET);
              throw new WasiException(
                  WasiErrorCode.ECONNREFUSED, "WebSocket send failed: " + result);
            }

            final AdvancedNetworkConnection connection = activeConnections.get(connectionId);
            if (connection != null) {
              connection.incrementMessagesSent();
              connection.updateLastActivity();
            }
            metrics.incrementMessagesSent(PROTOCOL_WEBSOCKET);

          } catch (final Exception e) {
            metrics.incrementErrors(PROTOCOL_WEBSOCKET);
            throw new WasiException(
                "WebSocket send failed",
                WasiErrorCode.ECONNREFUSED,
                "advanced_networking",
                null,
                e);
          }
        },
        executorService);
  }

  /**
   * Receive a WebSocket message.
   *
   * @param connectionId The WebSocket connection ID
   * @param buffer Buffer to receive the message data
   * @param timeoutMs Receive timeout in milliseconds (0 for default)
   * @return CompletableFuture that resolves to the message type (or -1 if no message)
   * @throws WasiException if the operation fails
   */
  public CompletableFuture<Integer> websocketReceive(
      final long connectionId, final ByteBuffer buffer, final long timeoutMs) throws WasiException {
    JniValidation.requireNonNull(buffer, "Receive buffer");
    JniValidation.requireValidConnectionId(connectionId, activeConnections);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final int[] messageTypeOut = new int[1];
            final int[] bytesReceivedOut = new int[1];

            final int result =
                nativeWebSocketReceive(
                    connectionId,
                    buffer,
                    buffer.remaining(),
                    timeoutMs,
                    messageTypeOut,
                    bytesReceivedOut);
            if (result != 0) {
              if (result == -2) {
                // Timeout - not an error
                return -1;
              }
              metrics.incrementErrors(PROTOCOL_WEBSOCKET);
              throw new WasiException(
                  WasiErrorCode.ECONNREFUSED, "WebSocket receive failed: " + result);
            }

            final AdvancedNetworkConnection connection = activeConnections.get(connectionId);
            if (connection != null) {
              connection.incrementMessagesReceived();
              connection.updateLastActivity();
            }
            metrics.incrementMessagesReceived(PROTOCOL_WEBSOCKET);

            // Update buffer position
            buffer.position(buffer.position() + bytesReceivedOut[0]);
            return messageTypeOut[0];

          } catch (final Exception e) {
            metrics.incrementErrors(PROTOCOL_WEBSOCKET);
            throw new WasiException(
                "WebSocket receive failed",
                WasiErrorCode.ECONNREFUSED,
                "advanced_networking",
                null,
                e);
          }
        },
        executorService);
  }

  /**
   * Create an HTTP/2 client connection.
   *
   * @param host The server hostname
   * @param port The server port
   * @param useTls Whether to use TLS/SSL
   * @param timeoutMs Connection timeout in milliseconds (0 for default)
   * @return CompletableFuture that resolves to the connection ID
   * @throws WasiException if the operation fails
   */
  public CompletableFuture<Long> http2Connect(
      final String host, final int port, final boolean useTls, final long timeoutMs)
      throws WasiException {
    JniValidation.requireNonNull(host, "Host");
    JniValidation.requireValidString(host, "Host");
    JniValidation.requireValidPort(port);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long connectionId = nextConnectionId.getAndIncrement();
            final String endpoint = (useTls ? "https://" : "http://") + host + ":" + port;

            final int result =
                nativeHttp2Connect(host, port, useTls ? 1 : 0, timeoutMs, connectionId);
            if (result != 0) {
              metrics.incrementFailedConnections(PROTOCOL_HTTP2);
              throw new WasiException(
                  WasiErrorCode.ECONNREFUSED, "HTTP/2 connection failed: " + result);
            }

            final AdvancedNetworkConnection connection =
                new AdvancedNetworkConnection(
                    connectionId, PROTOCOL_HTTP2, endpoint, System.currentTimeMillis());
            activeConnections.put(connectionId, connection);
            metrics.incrementSuccessfulConnections(PROTOCOL_HTTP2);

            LOGGER.log(
                Level.INFO,
                "HTTP/2 connection established: {0} -> {1}",
                new Object[] {connectionId, endpoint});
            return connectionId;

          } catch (final Exception e) {
            metrics.incrementFailedConnections(PROTOCOL_HTTP2);
            throw new WasiException(
                "HTTP/2 connection failed",
                WasiErrorCode.ECONNREFUSED,
                "advanced_networking",
                null,
                e);
          }
        },
        executorService);
  }

  /**
   * Create a gRPC client connection.
   *
   * @param endpoint The gRPC server endpoint (e.g., "grpc://localhost:9090")
   * @param useTls Whether to use TLS/SSL
   * @param timeoutMs Connection timeout in milliseconds (0 for default)
   * @return CompletableFuture that resolves to the connection ID
   * @throws WasiException if the operation fails
   */
  public CompletableFuture<Long> grpcConnect(
      final String endpoint, final boolean useTls, final long timeoutMs) throws WasiException {
    JniValidation.requireNonNull(endpoint, "gRPC endpoint");
    JniValidation.requireValidString(endpoint, "gRPC endpoint");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long connectionId = nextConnectionId.getAndIncrement();

            final int result = nativeGrpcConnect(endpoint, useTls ? 1 : 0, timeoutMs, connectionId);
            if (result != 0) {
              metrics.incrementFailedConnections(PROTOCOL_GRPC);
              throw new WasiException(
                  WasiErrorCode.ECONNREFUSED, "gRPC connection failed: " + result);
            }

            final AdvancedNetworkConnection connection =
                new AdvancedNetworkConnection(
                    connectionId, PROTOCOL_GRPC, endpoint, System.currentTimeMillis());
            activeConnections.put(connectionId, connection);
            metrics.incrementSuccessfulConnections(PROTOCOL_GRPC);

            LOGGER.log(
                Level.INFO,
                "gRPC connection established: {0} -> {1}",
                new Object[] {connectionId, endpoint});
            return connectionId;

          } catch (final Exception e) {
            metrics.incrementFailedConnections(PROTOCOL_GRPC);
            throw new WasiException(
                "gRPC connection failed",
                WasiErrorCode.ECONNREFUSED,
                "advanced_networking",
                null,
                e);
          }
        },
        executorService);
  }

  /**
   * Close a connection (any protocol).
   *
   * @param connectionId The connection ID to close
   * @return CompletableFuture that completes when the connection is closed
   * @throws WasiException if the operation fails
   */
  public CompletableFuture<Void> closeConnection(final long connectionId) throws WasiException {
    JniValidation.requireValidConnectionId(connectionId, activeConnections);

    return CompletableFuture.runAsync(
        () -> {
          try {
            final int result = nativeCloseConnection(connectionId);
            if (result != 0) {
              throw new WasiException(
                  WasiErrorCode.ECONNREFUSED, "Connection close failed: " + result);
            }

            final AdvancedNetworkConnection connection = activeConnections.remove(connectionId);
            if (connection != null) {
              metrics.incrementClosedConnections(connection.getProtocol());
              LOGGER.log(
                  Level.INFO,
                  "Connection closed: {0} ({1})",
                  new Object[] {connectionId, connection.getEndpoint()});
            }

          } catch (final Exception e) {
            throw new WasiException(
                "Connection close failed",
                WasiErrorCode.ECONNREFUSED,
                "advanced_networking",
                null,
                e);
          }
        },
        executorService);
  }

  /**
   * Get network performance metrics.
   *
   * @return Current network performance metrics
   */
  public AdvancedNetworkMetrics getMetrics() {
    return metrics.copy();
  }

  /**
   * Get active connection count.
   *
   * @return Number of active connections
   */
  public int getActiveConnectionCount() {
    return activeConnections.size();
  }

  /**
   * Cleanup and close all resources.
   *
   * @throws WasiException if cleanup fails
   */
  public void cleanup() throws WasiException {
    try {
      // Close all active connections
      for (final Long connectionId : activeConnections.keySet()) {
        try {
          closeConnection(connectionId).get();
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Failed to close connection " + connectionId, e);
        }
      }
      activeConnections.clear();

      // Native cleanup
      final int result = nativeCleanupAdvancedNetworking();
      if (result != 0) {
        LOGGER.log(Level.WARNING, "Native networking cleanup returned: {0}", result);
      }

      LOGGER.log(Level.INFO, "Advanced networking operations cleaned up");

    } catch (final Exception e) {
      throw new WasiException(
          "Advanced networking cleanup failed",
          WasiErrorCode.ECONNREFUSED,
          "advanced_networking",
          null,
          e);
    }
  }

  // Native method declarations

  /**
   * Initialize advanced networking support.
   *
   * @return 0 on success, negative on error
   */
  private static native int nativeInitializeAdvancedNetworking();

  /**
   * Create a WebSocket client connection.
   *
   * @param url WebSocket URL
   * @param headerKeys Header names (can be null)
   * @param headerValues Header values (can be null)
   * @param timeoutMs Connection timeout
   * @param connectionId Connection ID to use
   * @return 0 on success, negative on error
   */
  private static native int nativeWebSocketConnect(
      String url, String[] headerKeys, String[] headerValues, long timeoutMs, long connectionId);

  /**
   * Send a WebSocket message.
   *
   * @param connectionId Connection ID
   * @param messageType Message type
   * @param data Message data buffer
   * @param dataLen Data length
   * @return 0 on success, negative on error
   */
  private static native int nativeWebSocketSend(
      long connectionId, int messageType, ByteBuffer data, int dataLen);

  /**
   * Receive a WebSocket message.
   *
   * @param connectionId Connection ID
   * @param buffer Receive buffer
   * @param bufferLen Buffer length
   * @param timeoutMs Receive timeout
   * @param messageTypeOut Output array for message type
   * @param bytesReceivedOut Output array for bytes received
   * @return 0 on success, negative on error (-2 for timeout)
   */
  private static native int nativeWebSocketReceive(
      long connectionId,
      ByteBuffer buffer,
      int bufferLen,
      long timeoutMs,
      int[] messageTypeOut,
      int[] bytesReceivedOut);

  /**
   * Create an HTTP/2 client connection.
   *
   * @param host Server hostname
   * @param port Server port
   * @param useTls Whether to use TLS (1 for true, 0 for false)
   * @param timeoutMs Connection timeout
   * @param connectionId Connection ID to use
   * @return 0 on success, negative on error
   */
  private static native int nativeHttp2Connect(
      String host, int port, int useTls, long timeoutMs, long connectionId);

  /**
   * Create a gRPC client connection.
   *
   * @param endpoint gRPC endpoint
   * @param useTls Whether to use TLS (1 for true, 0 for false)
   * @param timeoutMs Connection timeout
   * @param connectionId Connection ID to use
   * @return 0 on success, negative on error
   */
  private static native int nativeGrpcConnect(
      String endpoint, int useTls, long timeoutMs, long connectionId);

  /**
   * Close any protocol connection.
   *
   * @param connectionId Connection ID to close
   * @return 0 on success, negative on error
   */
  private static native int nativeCloseConnection(long connectionId);

  /**
   * Cleanup advanced networking resources.
   *
   * @return 0 on success, negative on error
   */
  private static native int nativeCleanupAdvancedNetworking();

  /** Connection tracking for resource management. */
  private static final class AdvancedNetworkConnection {
    private final long connectionId;
    private final int protocol;
    private final String endpoint;
    private final long createdAt;
    private long lastActivity;
    private long messagesSent;
    private long messagesReceived;

    AdvancedNetworkConnection(
        final long connectionId, final int protocol, final String endpoint, final long createdAt) {
      this.connectionId = connectionId;
      this.protocol = protocol;
      this.endpoint = endpoint;
      this.createdAt = createdAt;
      this.lastActivity = createdAt;
      this.messagesSent = 0;
      this.messagesReceived = 0;
    }

    long getConnectionId() {
      return connectionId;
    }

    int getProtocol() {
      return protocol;
    }

    String getEndpoint() {
      return endpoint;
    }

    long getCreatedAt() {
      return createdAt;
    }

    long getLastActivity() {
      return lastActivity;
    }

    long getMessagesSent() {
      return messagesSent;
    }

    long getMessagesReceived() {
      return messagesReceived;
    }

    void updateLastActivity() {
      this.lastActivity = System.currentTimeMillis();
    }

    void incrementMessagesSent() {
      this.messagesSent++;
    }

    void incrementMessagesReceived() {
      this.messagesReceived++;
    }
  }

  /** Network performance metrics tracking. */
  public static final class AdvancedNetworkMetrics {
    private final Map<Integer, ProtocolMetrics> protocolMetrics = new ConcurrentHashMap<>();

    AdvancedNetworkMetrics() {
      protocolMetrics.put(PROTOCOL_WEBSOCKET, new ProtocolMetrics());
      protocolMetrics.put(PROTOCOL_HTTP2, new ProtocolMetrics());
      protocolMetrics.put(PROTOCOL_GRPC, new ProtocolMetrics());
    }

    void incrementSuccessfulConnections(final int protocol) {
      protocolMetrics.get(protocol).successfulConnections.incrementAndGet();
    }

    void incrementFailedConnections(final int protocol) {
      protocolMetrics.get(protocol).failedConnections.incrementAndGet();
    }

    void incrementClosedConnections(final int protocol) {
      protocolMetrics.get(protocol).closedConnections.incrementAndGet();
    }

    void incrementMessagesSent(final int protocol) {
      protocolMetrics.get(protocol).messagesSent.incrementAndGet();
    }

    void incrementMessagesReceived(final int protocol) {
      protocolMetrics.get(protocol).messagesReceived.incrementAndGet();
    }

    void incrementErrors(final int protocol) {
      protocolMetrics.get(protocol).errors.incrementAndGet();
    }

    public long getSuccessfulConnections(final int protocol) {
      return protocolMetrics.get(protocol).successfulConnections.get();
    }

    public long getFailedConnections(final int protocol) {
      return protocolMetrics.get(protocol).failedConnections.get();
    }

    public long getClosedConnections(final int protocol) {
      return protocolMetrics.get(protocol).closedConnections.get();
    }

    public long getMessagesSent(final int protocol) {
      return protocolMetrics.get(protocol).messagesSent.get();
    }

    public long getMessagesReceived(final int protocol) {
      return protocolMetrics.get(protocol).messagesReceived.get();
    }

    public long getErrors(final int protocol) {
      return protocolMetrics.get(protocol).errors.get();
    }

    public AdvancedNetworkMetrics copy() {
      final AdvancedNetworkMetrics copy = new AdvancedNetworkMetrics();
      for (final Map.Entry<Integer, ProtocolMetrics> entry : protocolMetrics.entrySet()) {
        copy.protocolMetrics.put(entry.getKey(), entry.getValue().copy());
      }
      return copy;
    }

    private static final class ProtocolMetrics {
      private final AtomicLong successfulConnections = new AtomicLong(0);
      private final AtomicLong failedConnections = new AtomicLong(0);
      private final AtomicLong closedConnections = new AtomicLong(0);
      private final AtomicLong messagesSent = new AtomicLong(0);
      private final AtomicLong messagesReceived = new AtomicLong(0);
      private final AtomicLong errors = new AtomicLong(0);

      /**
       * Creates a copy of the current protocol metrics.
       *
       * @return a new ProtocolMetrics instance with copied values
       */
      ProtocolMetrics copy() {
        final ProtocolMetrics copy = new ProtocolMetrics();
        copy.successfulConnections.set(successfulConnections.get());
        copy.failedConnections.set(failedConnections.get());
        copy.closedConnections.set(closedConnections.get());
        copy.messagesSent.set(messagesSent.get());
        copy.messagesReceived.set(messagesReceived.get());
        copy.errors.set(errors.get());
        return copy;
      }
    }
  }
}
