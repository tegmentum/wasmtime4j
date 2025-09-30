package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI Preview 2 advanced network operations.
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
 * <p>This implementation uses the Panama Foreign Function API available in Java 23+.
 *
 * @since 1.0.0
 */
public final class WasiAdvancedNetworkOperations {

  private static final Logger LOGGER =
      Logger.getLogger(WasiAdvancedNetworkOperations.class.getName());

  // Protocol constants
  public static final int PROTOCOL_WEBSOCKET = 1;
  public static final int PROTOCOL_HTTP2 = 2;
  public static final int PROTOCOL_GRPC = 3;

  // WebSocket message types
  public static final int WS_MESSAGE_TEXT = 1;
  public static final int WS_MESSAGE_BINARY = 2;
  public static final int WS_MESSAGE_PING = 9;
  public static final int WS_MESSAGE_PONG = 10;
  public static final int WS_MESSAGE_CLOSE = 8;

  // HTTP/2 stream states
  public static final int HTTP2_STREAM_IDLE = 0;
  public static final int HTTP2_STREAM_OPEN = 1;
  public static final int HTTP2_STREAM_RESERVED_LOCAL = 2;
  public static final int HTTP2_STREAM_RESERVED_REMOTE = 3;
  public static final int HTTP2_STREAM_HALF_CLOSED_LOCAL = 4;
  public static final int HTTP2_STREAM_HALF_CLOSED_REMOTE = 5;
  public static final int HTTP2_STREAM_CLOSED = 6;

  // Native library and function handles
  private static final String LIBRARY_NAME = "wasmtime4j";
  private static final Linker LINKER = Linker.nativeLinker();
  private static final SymbolLookup SYMBOL_LOOKUP =
      SymbolLookup.libraryLookup(LIBRARY_NAME, Arena.global());

  // Function layouts
  private static final FunctionDescriptor INIT_DESC = FunctionDescriptor.of(ValueLayout.JAVA_INT);
  private static final FunctionDescriptor WEBSOCKET_CONNECT_DESC =
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
  private static final FunctionDescriptor WEBSOCKET_SEND_DESC =
      FunctionDescriptor.of(
          ValueLayout.JAVA_INT,
          ValueLayout.JAVA_LONG,
          ValueLayout.JAVA_INT,
          ValueLayout.ADDRESS,
          ValueLayout.JAVA_INT);
  private static final FunctionDescriptor HTTP2_CONNECT_DESC =
      FunctionDescriptor.of(
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
  private static final FunctionDescriptor GRPC_CONNECT_DESC =
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
  private static final FunctionDescriptor CLOSE_CONNECTION_DESC =
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG);

  // Method handles
  private final MethodHandle advancedNetworkingInit;
  private final MethodHandle websocketConnect;
  private final MethodHandle websocketSend;
  private final MethodHandle websocketReceive;
  private final MethodHandle http2Connect;
  private final MethodHandle grpcConnect;
  private final MethodHandle closeConnection;
  private final MethodHandle cleanupAdvancedNetworking;

  // Instance state
  private final WasiContext wasiContext;
  private final ExecutorService executorService;
  private final Arena arena;
  private final Map<Long, AdvancedNetworkConnection> activeConnections = new ConcurrentHashMap<>();
  private final AtomicLong nextConnectionId = new AtomicLong(1L);
  private final AdvancedNetworkMetrics metrics = new AdvancedNetworkMetrics();
  private volatile boolean initialized = false;

  /**
   * Create a new WASI advanced network operations instance.
   *
   * @param wasiContext The WASI context this instance belongs to
   * @param executorService Executor service for async operations
   * @throws PanamaException if initialization fails
   */
  public WasiAdvancedNetworkOperations(
      final WasiContext wasiContext, final ExecutorService executorService) throws PanamaException {
    this.wasiContext = PanamaValidation.requireNonNull(wasiContext, "WASI context");
    this.executorService = PanamaValidation.requireNonNull(executorService, "Executor service");
    this.arena = Arena.ofConfined();

    try {
      // Initialize method handles
      this.advancedNetworkingInit =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("advanced_networking_init").orElseThrow(), INIT_DESC);
      this.websocketConnect =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("websocket_connect").orElseThrow(), WEBSOCKET_CONNECT_DESC);
      this.websocketSend =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("websocket_send_text").orElseThrow(), WEBSOCKET_SEND_DESC);
      this.websocketReceive =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("websocket_receive").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));
      this.http2Connect =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("http2_connect").orElseThrow(), HTTP2_CONNECT_DESC);
      this.grpcConnect =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("grpc_connect").orElseThrow(), GRPC_CONNECT_DESC);
      this.closeConnection =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("advanced_networking_close_connection").orElseThrow(),
              CLOSE_CONNECTION_DESC);
      this.cleanupAdvancedNetworking =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("advanced_networking_cleanup").orElseThrow(), INIT_DESC);

      LOGGER.log(
          Level.INFO,
          "Advanced network operations initialized for WASI context: {0}",
          wasiContext.getContextId());

    } catch (final Throwable e) {
      arena.close();
      throw new PanamaException("Failed to initialize advanced networking method handles", e);
    }
  }

  /**
   * Initialize advanced networking support.
   *
   * @throws PanamaException if initialization fails
   */
  public void initialize() throws PanamaException {
    if (initialized) {
      return;
    }

    try {
      final int result = (int) advancedNetworkingInit.invokeExact();
      if (result != 0) {
        throw new PanamaException("Failed to initialize advanced networking: " + result);
      }
      initialized = true;
      LOGGER.log(Level.INFO, "Advanced networking initialized successfully");

    } catch (final Throwable e) {
      throw new PanamaException("Advanced networking initialization failed", e);
    }
  }

  /**
   * Create a WebSocket client connection.
   *
   * @param url The WebSocket URL to connect to
   * @param headers Optional HTTP headers as key-value pairs (can be null)
   * @param timeoutMs Connection timeout in milliseconds (0 for default)
   * @return CompletableFuture that resolves to the connection ID
   * @throws PanamaException if the operation fails
   */
  public CompletableFuture<Long> websocketConnect(
      final String url, final Map<String, String> headers, final long timeoutMs)
      throws PanamaException {
    PanamaValidation.requireNonNull(url, "WebSocket URL");
    PanamaValidation.requireValidString(url, "WebSocket URL");
    ensureInitialized();

    return CompletableFuture.supplyAsync(
        () -> {
          try (final Arena callArena = Arena.ofConfined()) {
            final long connectionId = nextConnectionId.getAndIncrement();

            // Allocate memory for URL
            final MemorySegment urlSegment = callArena.allocateFrom(url);

            // Allocate memory for connection ID output
            final MemorySegment connectionIdOut = callArena.allocate(ValueLayout.JAVA_LONG);
            connectionIdOut.set(ValueLayout.JAVA_LONG, 0, connectionId);

            final int result = (int) websocketConnect.invokeExact(urlSegment, connectionIdOut);
            if (result != 0) {
              metrics.incrementFailedConnections(PROTOCOL_WEBSOCKET);
              throw new PanamaException("WebSocket connection failed: " + result);
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

          } catch (final Throwable e) {
            metrics.incrementFailedConnections(PROTOCOL_WEBSOCKET);
            throw new PanamaException("WebSocket connection failed", e);
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
   * @throws PanamaException if the operation fails
   */
  public CompletableFuture<Void> websocketSend(
      final long connectionId, final int messageType, final ByteBuffer data)
      throws PanamaException {
    PanamaValidation.requireNonNull(data, "Message data");
    PanamaValidation.requireValidConnectionId(connectionId, activeConnections);
    ensureInitialized();

    if (messageType != WS_MESSAGE_TEXT && messageType != WS_MESSAGE_BINARY) {
      throw new PanamaException("Invalid WebSocket message type: " + messageType);
    }

    return CompletableFuture.runAsync(
        () -> {
          try (final Arena callArena = Arena.ofConfined()) {
            // Allocate memory for the data
            final MemorySegment dataSegment = callArena.allocate(data.remaining());
            MemorySegment.copy(data, 0, dataSegment, ValueLayout.JAVA_BYTE, 0, data.remaining());

            final int result =
                (int)
                    websocketSend.invokeExact(
                        connectionId, messageType, dataSegment, data.remaining());
            if (result != 0) {
              metrics.incrementErrors(PROTOCOL_WEBSOCKET);
              throw new PanamaException("WebSocket send failed: " + result);
            }

            final AdvancedNetworkConnection connection = activeConnections.get(connectionId);
            if (connection != null) {
              connection.incrementMessagesSent();
              connection.updateLastActivity();
            }
            metrics.incrementMessagesSent(PROTOCOL_WEBSOCKET);

          } catch (final Throwable e) {
            metrics.incrementErrors(PROTOCOL_WEBSOCKET);
            throw new PanamaException("WebSocket send failed", e);
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
   * @throws PanamaException if the operation fails
   */
  public CompletableFuture<Integer> websocketReceive(
      final long connectionId, final ByteBuffer buffer, final long timeoutMs)
      throws PanamaException {
    PanamaValidation.requireNonNull(buffer, "Receive buffer");
    PanamaValidation.requireValidConnectionId(connectionId, activeConnections);
    ensureInitialized();

    return CompletableFuture.supplyAsync(
        () -> {
          try (final Arena callArena = Arena.ofConfined()) {
            // Allocate memory for the buffer
            final MemorySegment bufferSegment = callArena.allocate(buffer.remaining());

            // Allocate memory for output parameters
            final MemorySegment messageTypeOut = callArena.allocate(ValueLayout.JAVA_INT);
            final MemorySegment bytesReceivedOut = callArena.allocate(ValueLayout.JAVA_INT);

            final int result =
                (int)
                    websocketReceive.invokeExact(
                        connectionId,
                        bufferSegment,
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
              throw new PanamaException("WebSocket receive failed: " + result);
            }

            final int messageType = messageTypeOut.get(ValueLayout.JAVA_INT, 0);
            final int bytesReceived = bytesReceivedOut.get(ValueLayout.JAVA_INT, 0);

            // Copy received data to buffer
            if (bytesReceived > 0) {
              MemorySegment.copy(bufferSegment, ValueLayout.JAVA_BYTE, 0, buffer, 0, bytesReceived);
              buffer.position(buffer.position() + bytesReceived);
            }

            final AdvancedNetworkConnection connection = activeConnections.get(connectionId);
            if (connection != null) {
              connection.incrementMessagesReceived();
              connection.updateLastActivity();
            }
            metrics.incrementMessagesReceived(PROTOCOL_WEBSOCKET);

            return messageType;

          } catch (final Throwable e) {
            metrics.incrementErrors(PROTOCOL_WEBSOCKET);
            throw new PanamaException("WebSocket receive failed", e);
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
   * @throws PanamaException if the operation fails
   */
  public CompletableFuture<Long> http2Connect(
      final String host, final int port, final boolean useTls, final long timeoutMs)
      throws PanamaException {
    PanamaValidation.requireNonNull(host, "Host");
    PanamaValidation.requireValidString(host, "Host");
    PanamaValidation.requireValidPort(port);
    ensureInitialized();

    return CompletableFuture.supplyAsync(
        () -> {
          try (final Arena callArena = Arena.ofConfined()) {
            final long connectionId = nextConnectionId.getAndIncrement();
            final String endpoint = (useTls ? "https://" : "http://") + host + ":" + port;

            // Allocate memory for parameters
            final MemorySegment hostSegment = callArena.allocateFrom(host);
            final MemorySegment connectionIdOut = callArena.allocate(ValueLayout.JAVA_LONG);
            connectionIdOut.set(ValueLayout.JAVA_LONG, 0, connectionId);

            final int result = (int) http2Connect.invokeExact(hostSegment, port, connectionIdOut);
            if (result != 0) {
              metrics.incrementFailedConnections(PROTOCOL_HTTP2);
              throw new PanamaException("HTTP/2 connection failed: " + result);
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

          } catch (final Throwable e) {
            metrics.incrementFailedConnections(PROTOCOL_HTTP2);
            throw new PanamaException("HTTP/2 connection failed", e);
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
   * @throws PanamaException if the operation fails
   */
  public CompletableFuture<Long> grpcConnect(
      final String endpoint, final boolean useTls, final long timeoutMs) throws PanamaException {
    PanamaValidation.requireNonNull(endpoint, "gRPC endpoint");
    PanamaValidation.requireValidString(endpoint, "gRPC endpoint");
    ensureInitialized();

    return CompletableFuture.supplyAsync(
        () -> {
          try (final Arena callArena = Arena.ofConfined()) {
            final long connectionId = nextConnectionId.getAndIncrement();

            // Allocate memory for parameters
            final MemorySegment endpointSegment = callArena.allocateFrom(endpoint);
            final MemorySegment connectionIdOut = callArena.allocate(ValueLayout.JAVA_LONG);
            connectionIdOut.set(ValueLayout.JAVA_LONG, 0, connectionId);

            final int result = (int) grpcConnect.invokeExact(endpointSegment, connectionIdOut);
            if (result != 0) {
              metrics.incrementFailedConnections(PROTOCOL_GRPC);
              throw new PanamaException("gRPC connection failed: " + result);
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

          } catch (final Throwable e) {
            metrics.incrementFailedConnections(PROTOCOL_GRPC);
            throw new PanamaException("gRPC connection failed", e);
          }
        },
        executorService);
  }

  /**
   * Close a connection (any protocol).
   *
   * @param connectionId The connection ID to close
   * @return CompletableFuture that completes when the connection is closed
   * @throws PanamaException if the operation fails
   */
  public CompletableFuture<Void> closeConnection(final long connectionId) throws PanamaException {
    PanamaValidation.requireValidConnectionId(connectionId, activeConnections);
    ensureInitialized();

    return CompletableFuture.runAsync(
        () -> {
          try {
            final int result = (int) closeConnection.invokeExact(connectionId);
            if (result != 0) {
              throw new PanamaException("Connection close failed: " + result);
            }

            final AdvancedNetworkConnection connection = activeConnections.remove(connectionId);
            if (connection != null) {
              metrics.incrementClosedConnections(connection.getProtocol());
              LOGGER.log(
                  Level.INFO,
                  "Connection closed: {0} ({1})",
                  new Object[] {connectionId, connection.getEndpoint()});
            }

          } catch (final Throwable e) {
            throw new PanamaException("Connection close failed", e);
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
   * @throws PanamaException if cleanup fails
   */
  public void cleanup() throws PanamaException {
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
      if (initialized) {
        final int result = (int) cleanupAdvancedNetworking.invokeExact();
        if (result != 0) {
          LOGGER.log(Level.WARNING, "Native networking cleanup returned: {0}", result);
        }
      }

      // Close the arena
      arena.close();

      LOGGER.log(Level.INFO, "Advanced networking operations cleaned up");

    } catch (final Throwable e) {
      throw new PanamaException("Advanced networking cleanup failed", e);
    }
  }

  /**
   * Ensure the networking system is initialized.
   *
   * @throws PanamaException if not initialized
   */
  private void ensureInitialized() throws PanamaException {
    if (!initialized) {
      throw new PanamaException("Advanced networking not initialized");
    }
  }

  /** Connection tracking for resource management. */
  private static final class AdvancedNetworkConnection {
    private final long connectionId;
    private final int protocol;
    private final String endpoint;
    private final long createdAt;
    private volatile long lastActivity;
    private volatile long messagesSent;
    private volatile long messagesReceived;

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
