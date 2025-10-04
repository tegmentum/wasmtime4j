package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of experimental WASI advanced networking protocols.
 *
 * <p>This class provides experimental advanced networking capabilities as defined in WASI Preview
 * 2, including HTTP/2, HTTP/3, WebSocket, and advanced TCP/UDP features. These protocols enable
 * modern web communication patterns in WASM applications.
 *
 * <p>Supported protocols and features:
 *
 * <ul>
 *   <li>HTTP/2 client with server push support and stream multiplexing
 *   <li>HTTP/3 over QUIC for improved performance and reliability
 *   <li>WebSocket for bidirectional real-time communication
 *   <li>Advanced TCP socket options and configuration
 *   <li>UDP multicast and broadcast support
 *   <li>Network interface enumeration and selection
 *   <li>Connection pooling and keep-alive management
 * </ul>
 *
 * <p>This is an experimental feature and may change in future WASI releases.
 *
 * @since 1.0.0
 */
public final class WasiAdvancedNetworking {

  private static final Logger LOGGER = Logger.getLogger(WasiAdvancedNetworking.class.getName());

  /** Maximum concurrent connections per pool. */
  private static final int MAX_POOL_CONNECTIONS = 100;

  /** Maximum WebSocket frame size (1MB). */
  private static final int MAX_WEBSOCKET_FRAME_SIZE = 1024 * 1024;

  /** The WASI context this networking handler belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Connection handle generator. */
  private final AtomicLong connectionHandleGenerator = new AtomicLong(1);

  /** Active connections tracking. */
  private final Map<Long, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();

  /** WebSocket connections tracking. */
  private final Map<Long, WebSocketInfo> webSocketConnections = new ConcurrentHashMap<>();

  /**
   * Creates a new WASI advanced networking handler.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiAdvancedNetworking(
      final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info("Created WASI advanced networking handler");
  }

  /**
   * Creates an HTTP/2 connection.
   *
   * <p>Establishes an HTTP/2 connection with multiplexing support for efficient communication.
   *
   * @param host the target host
   * @param port the target port
   * @param tlsEnabled whether to use TLS (HTTPS)
   * @param options the HTTP/2 connection options
   * @return CompletableFuture that resolves to the connection handle
   * @throws WasiException if connection creation fails
   */
  public CompletableFuture<Long> createHttp2ConnectionAsync(
      final String host, final int port, final boolean tlsEnabled, final Http2Options options) {
    JniValidation.requireNonEmpty(host, "host");
    JniValidation.requireValidPort(port);
    JniValidation.requireNonNull(options, "options");

    if (activeConnections.size() >= MAX_POOL_CONNECTIONS) {
      throw new WasiException("Maximum connections exceeded", WasiErrorCode.ENOMEM);
    }

    LOGGER.info(
        () ->
            String.format(
                "Creating HTTP/2 connection: host=%s, port=%d, tls=%b", host, port, tlsEnabled));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long connectionHandle = connectionHandleGenerator.getAndIncrement();

            final Http2ConnectionResult result =
                nativeCreateHttp2Connection(
                    wasiContext.getNativeHandle(),
                    connectionHandle,
                    host,
                    port,
                    tlsEnabled,
                    options.maxConcurrentStreams,
                    options.windowSize,
                    options.enableServerPush,
                    options.headerTableSize,
                    options.connectionTimeoutMs);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create HTTP/2 connection: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            final ConnectionInfo connectionInfo =
                new ConnectionInfo(
                    connectionHandle,
                    host,
                    port,
                    ProtocolType.HTTP2,
                    tlsEnabled,
                    System.currentTimeMillis());
            activeConnections.put(connectionHandle, connectionInfo);

            LOGGER.info(
                () ->
                    String.format(
                        "Created HTTP/2 connection: handle=%d, streams=%d",
                        connectionHandle, result.availableStreams));

            return connectionHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create HTTP/2 connection", e);
            throw new RuntimeException("HTTP/2 connection failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates an HTTP/3 connection over QUIC.
   *
   * <p>Establishes an HTTP/3 connection for improved performance and reduced latency.
   *
   * @param host the target host
   * @param port the target port
   * @param options the HTTP/3 connection options
   * @return CompletableFuture that resolves to the connection handle
   * @throws WasiException if connection creation fails
   */
  public CompletableFuture<Long> createHttp3ConnectionAsync(
      final String host, final int port, final Http3Options options) {
    JniValidation.requireNonEmpty(host, "host");
    JniValidation.requireValidPort(port);
    JniValidation.requireNonNull(options, "options");

    if (activeConnections.size() >= MAX_POOL_CONNECTIONS) {
      throw new WasiException("Maximum connections exceeded", WasiErrorCode.ENOMEM);
    }

    LOGGER.info(() -> String.format("Creating HTTP/3 connection: host=%s, port=%d", host, port));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long connectionHandle = connectionHandleGenerator.getAndIncrement();

            final Http3ConnectionResult result =
                nativeCreateHttp3Connection(
                    wasiContext.getNativeHandle(),
                    connectionHandle,
                    host,
                    port,
                    options.maxBidirectionalStreams,
                    options.maxUnidirectionalStreams,
                    options.idleTimeoutMs,
                    options.keepAliveIntervalMs,
                    options.congestionControl);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create HTTP/3 connection: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            final ConnectionInfo connectionInfo =
                new ConnectionInfo(
                    connectionHandle,
                    host,
                    port,
                    ProtocolType.HTTP3,
                    true, // HTTP/3 is always encrypted
                    System.currentTimeMillis());
            activeConnections.put(connectionHandle, connectionInfo);

            LOGGER.info(
                () ->
                    String.format(
                        "Created HTTP/3 connection: handle=%d, rtt=%dms",
                        connectionHandle, result.roundTripTimeMs));

            return connectionHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create HTTP/3 connection", e);
            throw new RuntimeException("HTTP/3 connection failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Makes an HTTP/2 request on an existing connection.
   *
   * @param connectionHandle the HTTP/2 connection handle
   * @param method the HTTP method
   * @param path the request path
   * @param headers the request headers
   * @param body the request body (can be null)
   * @return CompletableFuture that resolves to the HTTP response
   * @throws WasiException if the request fails
   */
  public CompletableFuture<Http2Response> http2RequestAsync(
      final long connectionHandle,
      final String method,
      final String path,
      final Map<String, String> headers,
      final ByteBuffer body) {
    JniValidation.requireNonEmpty(method, "method");
    JniValidation.requireNonEmpty(path, "path");
    JniValidation.requireNonNull(headers, "headers");

    final ConnectionInfo connection = activeConnections.get(connectionHandle);
    if (connection == null || connection.protocolType != ProtocolType.HTTP2) {
      throw new WasiException(
          "Invalid HTTP/2 connection handle: " + connectionHandle, WasiErrorCode.EBADF);
    }

    LOGGER.fine(
        () ->
            String.format(
                "HTTP/2 request: connection=%d, method=%s, path=%s",
                connectionHandle, method, path));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Prepare headers array
            final String[] headerArray = new String[headers.size() * 2];
            int index = 0;
            for (final Map.Entry<String, String> entry : headers.entrySet()) {
              headerArray[index++] = entry.getKey();
              headerArray[index++] = entry.getValue();
            }

            // Prepare body
            byte[] bodyBytes = null;
            int bodySize = 0;
            if (body != null && body.hasRemaining()) {
              bodySize = body.remaining();
              bodyBytes = new byte[bodySize];
              body.get(bodyBytes);
            }

            final Http2ResponseResult result =
                nativeHttp2Request(
                    wasiContext.getNativeHandle(),
                    connectionHandle,
                    method,
                    path,
                    headerArray,
                    bodyBytes,
                    bodySize);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "HTTP/2 request failed: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Parse response headers
            final Map<String, String> responseHeaders = new ConcurrentHashMap<>();
            if (result.headers != null) {
              for (int i = 0; i < result.headers.length; i += 2) {
                responseHeaders.put(result.headers[i], result.headers[i + 1]);
              }
            }

            // Create response body buffer
            ByteBuffer responseBody = null;
            if (result.body != null && result.body.length > 0) {
              responseBody = ByteBuffer.wrap(result.body);
            }

            final Http2Response response =
                new Http2Response(
                    result.statusCode,
                    responseHeaders,
                    responseBody,
                    result.streamId,
                    result.serverPush);

            LOGGER.fine(
                () ->
                    String.format(
                        "HTTP/2 response: connection=%d, status=%d, stream=%d",
                        connectionHandle, result.statusCode, result.streamId));

            return response;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "HTTP/2 request failed", e);
            throw new RuntimeException("HTTP/2 request failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates a WebSocket connection.
   *
   * @param uri the WebSocket URI
   * @param protocols the supported protocols
   * @param headers additional headers
   * @param options WebSocket options
   * @return CompletableFuture that resolves to the WebSocket handle
   * @throws WasiException if WebSocket creation fails
   */
  public CompletableFuture<Long> createWebSocketAsync(
      final String uri,
      final List<String> protocols,
      final Map<String, String> headers,
      final WebSocketOptions options) {
    JniValidation.requireNonEmpty(uri, "uri");
    JniValidation.requireNonNull(protocols, "protocols");
    JniValidation.requireNonNull(headers, "headers");
    JniValidation.requireNonNull(options, "options");

    LOGGER.info(() -> String.format("Creating WebSocket connection: uri=%s", uri));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long webSocketHandle = connectionHandleGenerator.getAndIncrement();

            // Prepare protocols array
            final String[] protocolArray = protocols.toArray(new String[0]);

            // Prepare headers array
            final String[] headerArray = new String[headers.size() * 2];
            int index = 0;
            for (final Map.Entry<String, String> entry : headers.entrySet()) {
              headerArray[index++] = entry.getKey();
              headerArray[index++] = entry.getValue();
            }

            final WebSocketConnectionResult result =
                nativeCreateWebSocket(
                    wasiContext.getNativeHandle(),
                    webSocketHandle,
                    uri,
                    protocolArray,
                    headerArray,
                    options.maxFrameSize,
                    options.pingIntervalMs,
                    options.compression);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create WebSocket connection: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            final WebSocketInfo webSocketInfo =
                new WebSocketInfo(
                    webSocketHandle,
                    uri,
                    result.selectedProtocol,
                    WebSocketState.OPEN,
                    System.currentTimeMillis());
            webSocketConnections.put(webSocketHandle, webSocketInfo);

            LOGGER.info(
                () ->
                    String.format(
                        "Created WebSocket connection: handle=%d, protocol=%s",
                        webSocketHandle, result.selectedProtocol));

            return webSocketHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create WebSocket connection", e);
            throw new RuntimeException("WebSocket connection failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Sends a WebSocket message.
   *
   * @param webSocketHandle the WebSocket handle
   * @param message the message to send
   * @param messageType the message type (text or binary)
   * @return CompletableFuture that completes when message is sent
   * @throws WasiException if send fails
   */
  public CompletableFuture<Void> sendWebSocketMessageAsync(
      final long webSocketHandle,
      final ByteBuffer message,
      final WebSocketMessageType messageType) {
    JniValidation.requireNonNull(message, "message");
    JniValidation.requireNonNull(messageType, "messageType");

    final WebSocketInfo webSocket = webSocketConnections.get(webSocketHandle);
    if (webSocket == null) {
      throw new WasiException("Invalid WebSocket handle: " + webSocketHandle, WasiErrorCode.EBADF);
    }

    if (webSocket.state != WebSocketState.OPEN) {
      throw new WasiException("WebSocket is not open: " + webSocketHandle, WasiErrorCode.ENOTCONN);
    }

    final int messageSize = message.remaining();
    if (messageSize > MAX_WEBSOCKET_FRAME_SIZE) {
      throw new WasiException(
          "WebSocket message too large: " + messageSize, WasiErrorCode.EMSGSIZE);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Sending WebSocket message: handle=%d, type=%s, size=%d",
                webSocketHandle, messageType, messageSize));

    return CompletableFuture.runAsync(
        () -> {
          try {
            final byte[] messageBytes = new byte[messageSize];
            message.get(messageBytes);

            final int result =
                nativeSendWebSocketMessage(
                    wasiContext.getNativeHandle(),
                    webSocketHandle,
                    messageBytes,
                    messageSize,
                    messageType.ordinal());

            if (result != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
              throw new WasiException(
                  "Failed to send WebSocket message: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            LOGGER.fine(() -> String.format("Sent WebSocket message: handle=%d", webSocketHandle));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send WebSocket message", e);
            throw new RuntimeException("WebSocket send failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Receives a WebSocket message.
   *
   * @param webSocketHandle the WebSocket handle
   * @param buffer the buffer to receive into
   * @param timeoutMs timeout in milliseconds
   * @return CompletableFuture that resolves to the received message
   * @throws WasiException if receive fails
   */
  public CompletableFuture<WebSocketMessage> receiveWebSocketMessageAsync(
      final long webSocketHandle, final ByteBuffer buffer, final long timeoutMs) {
    JniValidation.requireNonNull(buffer, "buffer");

    final WebSocketInfo webSocket = webSocketConnections.get(webSocketHandle);
    if (webSocket == null) {
      throw new WasiException("Invalid WebSocket handle: " + webSocketHandle, WasiErrorCode.EBADF);
    }

    if (webSocket.state != WebSocketState.OPEN) {
      throw new WasiException("WebSocket is not open: " + webSocketHandle, WasiErrorCode.ENOTCONN);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Receiving WebSocket message: handle=%d, bufferSize=%d, timeout=%dms",
                webSocketHandle, buffer.remaining(), timeoutMs));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final byte[] tempBuffer = new byte[buffer.remaining()];
            final WebSocketReceiveResult result =
                nativeReceiveWebSocketMessage(
                    wasiContext.getNativeHandle(), webSocketHandle, tempBuffer, timeoutMs);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to receive WebSocket message: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            if (result.bytesReceived > 0) {
              buffer.put(tempBuffer, 0, result.bytesReceived);
            }

            final WebSocketMessage message =
                new WebSocketMessage(
                    result.bytesReceived,
                    WebSocketMessageType.values()[result.messageType],
                    result.isFinal);

            LOGGER.fine(
                () ->
                    String.format(
                        "Received WebSocket message: handle=%d, bytes=%d, type=%s",
                        webSocketHandle, result.bytesReceived, message.messageType));

            return message;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to receive WebSocket message", e);
            throw new RuntimeException("WebSocket receive failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Enables UDP multicast on a socket.
   *
   * @param socketHandle the UDP socket handle
   * @param multicastAddress the multicast address to join
   * @param interfaceAddress the interface address to use
   * @return CompletableFuture that completes when multicast is enabled
   * @throws WasiException if multicast setup fails
   */
  public CompletableFuture<Void> enableUdpMulticastAsync(
      final long socketHandle, final String multicastAddress, final String interfaceAddress) {
    JniValidation.requireNonEmpty(multicastAddress, "multicastAddress");
    JniValidation.requireNonEmpty(interfaceAddress, "interfaceAddress");

    LOGGER.fine(
        () ->
            String.format(
                "Enabling UDP multicast: socket=%d, group=%s, interface=%s",
                socketHandle, multicastAddress, interfaceAddress));

    return CompletableFuture.runAsync(
        () -> {
          try {
            final int result =
                nativeEnableUdpMulticast(
                    wasiContext.getNativeHandle(),
                    socketHandle,
                    multicastAddress,
                    interfaceAddress);

            if (result != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
              throw new WasiException(
                  "Failed to enable UDP multicast: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            LOGGER.fine(() -> String.format("Enabled UDP multicast: socket=%d", socketHandle));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to enable UDP multicast", e);
            throw new RuntimeException("UDP multicast setup failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Lists available network interfaces.
   *
   * @return CompletableFuture that resolves to the list of network interfaces
   * @throws WasiException if enumeration fails
   */
  public CompletableFuture<List<NetworkInterface>> listNetworkInterfacesAsync() {
    LOGGER.fine("Enumerating network interfaces");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final NetworkInterfaceInfo[] interfaces =
                nativeListNetworkInterfaces(wasiContext.getNativeHandle());

            final List<NetworkInterface> result = new java.util.ArrayList<>();
            for (final NetworkInterfaceInfo info : interfaces) {
              result.add(
                  new NetworkInterface(
                      info.name, info.description, info.isUp, info.isLoopback, info.addresses));
            }

            LOGGER.fine(() -> String.format("Found %d network interfaces", result.size()));
            return result;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to enumerate network interfaces", e);
            throw new RuntimeException(
                "Network interface enumeration failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Closes a connection and releases its resources.
   *
   * @param connectionHandle the connection handle to close
   */
  public void closeConnection(final long connectionHandle) {
    final ConnectionInfo connection = activeConnections.get(connectionHandle);
    final WebSocketInfo webSocket = webSocketConnections.get(connectionHandle);

    if (connection == null && webSocket == null) {
      LOGGER.fine(
          () -> String.format("Connection already closed or invalid: handle=%d", connectionHandle));
      return;
    }

    LOGGER.fine(() -> String.format("Closing connection: handle=%d", connectionHandle));

    try {
      final int result = nativeCloseConnection(wasiContext.getNativeHandle(), connectionHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        LOGGER.warning(
            "Failed to close connection "
                + connectionHandle
                + ": "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"));
      }

      activeConnections.remove(connectionHandle);
      if (webSocket != null) {
        webSocket.state = WebSocketState.CLOSED;
        webSocketConnections.remove(connectionHandle);
      }

      LOGGER.fine(() -> String.format("Closed connection: handle=%d", connectionHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing connection: " + connectionHandle, e);
      activeConnections.remove(connectionHandle);
      webSocketConnections.remove(connectionHandle);
    }
  }

  /** Closes all connections and releases resources. */
  public void close() {
    LOGGER.info("Closing all advanced networking connections");

    for (final Long connectionHandle : activeConnections.keySet()) {
      try {
        closeConnection(connectionHandle);
      } catch (final Exception e) {
        LOGGER.log(
            Level.WARNING, "Error closing connection during shutdown: " + connectionHandle, e);
      }
    }

    for (final Long webSocketHandle : webSocketConnections.keySet()) {
      try {
        closeConnection(webSocketHandle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error closing WebSocket during shutdown: " + webSocketHandle, e);
      }
    }

    activeConnections.clear();
    webSocketConnections.clear();

    LOGGER.info("Advanced networking handler closed successfully");
  }

  // Native method declarations
  private static native Http2ConnectionResult nativeCreateHttp2Connection(
      long contextHandle,
      long connectionHandle,
      String host,
      int port,
      boolean tlsEnabled,
      int maxConcurrentStreams,
      int windowSize,
      boolean enableServerPush,
      int headerTableSize,
      long connectionTimeoutMs);

  private static native Http3ConnectionResult nativeCreateHttp3Connection(
      long contextHandle,
      long connectionHandle,
      String host,
      int port,
      int maxBidirectionalStreams,
      int maxUnidirectionalStreams,
      long idleTimeoutMs,
      long keepAliveIntervalMs,
      String congestionControl);

  private static native Http2ResponseResult nativeHttp2Request(
      long contextHandle,
      long connectionHandle,
      String method,
      String path,
      String[] headers,
      byte[] body,
      int bodySize);

  private static native WebSocketConnectionResult nativeCreateWebSocket(
      long contextHandle,
      long webSocketHandle,
      String uri,
      String[] protocols,
      String[] headers,
      int maxFrameSize,
      long pingIntervalMs,
      boolean compression);

  private static native int nativeSendWebSocketMessage(
      long contextHandle, long webSocketHandle, byte[] message, int messageSize, int messageType);

  private static native WebSocketReceiveResult nativeReceiveWebSocketMessage(
      long contextHandle, long webSocketHandle, byte[] buffer, long timeoutMs);

  private static native int nativeEnableUdpMulticast(
      long contextHandle, long socketHandle, String multicastAddress, String interfaceAddress);

  private static native NetworkInterfaceInfo[] nativeListNetworkInterfaces(long contextHandle);

  private static native int nativeCloseConnection(long contextHandle, long connectionHandle);

  /** Protocol type enumeration. */
  public enum ProtocolType {
    HTTP2,
    HTTP3,
    WEBSOCKET
  }

  /** WebSocket state enumeration. */
  public enum WebSocketState {
    CONNECTING,
    OPEN,
    CLOSING,
    CLOSED
  }

  /** WebSocket message type enumeration. */
  public enum WebSocketMessageType {
    TEXT,
    BINARY,
    PING,
    PONG,
    CLOSE
  }

  /** HTTP/2 connection options. */
  public static final class Http2Options {
    public final int maxConcurrentStreams;
    public final int windowSize;
    public final boolean enableServerPush;
    public final int headerTableSize;
    public final long connectionTimeoutMs;

    /**
     * Creates HTTP/2 options.
     *
     * @param maxConcurrentStreams maximum concurrent streams
     * @param windowSize window size in bytes
     * @param enableServerPush whether to enable server push
     * @param headerTableSize header table size in bytes
     * @param connectionTimeoutMs connection timeout in milliseconds
     */
    public Http2Options(
        final int maxConcurrentStreams,
        final int windowSize,
        final boolean enableServerPush,
        final int headerTableSize,
        final long connectionTimeoutMs) {
      this.maxConcurrentStreams = Math.max(1, maxConcurrentStreams);
      this.windowSize = Math.max(1024, windowSize);
      this.enableServerPush = enableServerPush;
      this.headerTableSize = Math.max(4096, headerTableSize);
      this.connectionTimeoutMs = Math.max(1000, connectionTimeoutMs);
    }

    public static Http2Options defaultOptions() {
      return new Http2Options(100, 65536, true, 4096, 30000);
    }
  }

  /** HTTP/3 connection options. */
  public static final class Http3Options {
    public final int maxBidirectionalStreams;
    public final int maxUnidirectionalStreams;
    public final long idleTimeoutMs;
    public final long keepAliveIntervalMs;
    public final String congestionControl;

    /**
     * Creates HTTP/3 options.
     *
     * @param maxBidirectionalStreams maximum bidirectional streams
     * @param maxUnidirectionalStreams maximum unidirectional streams
     * @param idleTimeoutMs idle timeout in milliseconds
     * @param keepAliveIntervalMs keep-alive interval in milliseconds
     * @param congestionControl congestion control algorithm (may be null)
     */
    public Http3Options(
        final int maxBidirectionalStreams,
        final int maxUnidirectionalStreams,
        final long idleTimeoutMs,
        final long keepAliveIntervalMs,
        final String congestionControl) {
      this.maxBidirectionalStreams = Math.max(1, maxBidirectionalStreams);
      this.maxUnidirectionalStreams = Math.max(1, maxUnidirectionalStreams);
      this.idleTimeoutMs = Math.max(1000, idleTimeoutMs);
      this.keepAliveIntervalMs = Math.max(1000, keepAliveIntervalMs);
      this.congestionControl = congestionControl != null ? congestionControl : "cubic";
    }

    public static Http3Options defaultOptions() {
      return new Http3Options(100, 100, 60000, 15000, "cubic");
    }
  }

  /** WebSocket connection options. */
  public static final class WebSocketOptions {
    public final int maxFrameSize;
    public final long pingIntervalMs;
    public final boolean compression;

    /**
     * Creates WebSocket options.
     *
     * @param maxFrameSize maximum frame size in bytes
     * @param pingIntervalMs ping interval in milliseconds
     * @param compression whether compression is enabled
     */
    public WebSocketOptions(
        final int maxFrameSize, final long pingIntervalMs, final boolean compression) {
      this.maxFrameSize = Math.min(MAX_WEBSOCKET_FRAME_SIZE, Math.max(1024, maxFrameSize));
      this.pingIntervalMs = Math.max(1000, pingIntervalMs);
      this.compression = compression;
    }

    public static WebSocketOptions defaultOptions() {
      return new WebSocketOptions(1024 * 1024, 30000, true);
    }
  }

  /** Connection information class. */
  public static final class ConnectionInfo {
    public final long handle;
    public final String host;
    public final int port;
    public final ProtocolType protocolType;
    public final boolean tlsEnabled;
    public final long createdAt;

    /**
     * Creates connection information.
     *
     * @param handle the connection handle
     * @param host the host address
     * @param port the port number
     * @param protocolType the protocol type
     * @param tlsEnabled whether TLS is enabled
     * @param createdAt creation timestamp
     */
    public ConnectionInfo(
        final long handle,
        final String host,
        final int port,
        final ProtocolType protocolType,
        final boolean tlsEnabled,
        final long createdAt) {
      this.handle = handle;
      this.host = host;
      this.port = port;
      this.protocolType = protocolType;
      this.tlsEnabled = tlsEnabled;
      this.createdAt = createdAt;
    }
  }

  /** WebSocket information class. */
  public static final class WebSocketInfo {
    public final long handle;
    public final String uri;
    public final String selectedProtocol;
    public volatile WebSocketState state;
    public final long createdAt;

    /**
     * Creates WebSocket information.
     *
     * @param handle the WebSocket handle
     * @param uri the WebSocket URI
     * @param selectedProtocol the selected protocol
     * @param state the WebSocket state
     * @param createdAt creation timestamp
     */
    public WebSocketInfo(
        final long handle,
        final String uri,
        final String selectedProtocol,
        final WebSocketState state,
        final long createdAt) {
      this.handle = handle;
      this.uri = uri;
      this.selectedProtocol = selectedProtocol;
      this.state = state;
      this.createdAt = createdAt;
    }
  }

  /** HTTP/2 response class. */
  public static final class Http2Response {
    public final int statusCode;
    public final Map<String, String> headers;
    public final ByteBuffer body;
    public final int streamId;
    public final boolean serverPush;

    /**
     * Creates HTTP/2 response.
     *
     * @param statusCode the HTTP status code
     * @param headers the response headers (may be null)
     * @param body the response body
     * @param streamId the stream ID
     * @param serverPush whether this is a server push
     */
    public Http2Response(
        final int statusCode,
        final Map<String, String> headers,
        final ByteBuffer body,
        final int streamId,
        final boolean serverPush) {
      this.statusCode = statusCode;
      this.headers =
          headers != null
              ? Collections.unmodifiableMap(new java.util.HashMap<>(headers))
              : Collections.emptyMap();
      this.body = body;
      this.streamId = streamId;
      this.serverPush = serverPush;
    }
  }

  /** WebSocket message class. */
  public static final class WebSocketMessage {
    public final int bytesReceived;
    public final WebSocketMessageType messageType;
    public final boolean isFinal;

    /**
     * Creates WebSocket message.
     *
     * @param bytesReceived number of bytes received
     * @param messageType the message type
     * @param isFinal whether this is the final fragment
     */
    public WebSocketMessage(
        final int bytesReceived, final WebSocketMessageType messageType, final boolean isFinal) {
      this.bytesReceived = bytesReceived;
      this.messageType = messageType;
      this.isFinal = isFinal;
    }
  }

  /** Network interface information. */
  public static final class NetworkInterface {
    public final String name;
    public final String description;
    public final boolean isUp;
    public final boolean isLoopback;
    public final String[] addresses;

    /**
     * Creates network interface information.
     *
     * @param name the interface name
     * @param description the interface description
     * @param isUp whether the interface is up
     * @param isLoopback whether this is a loopback interface
     * @param addresses the interface addresses (may be null)
     */
    public NetworkInterface(
        final String name,
        final String description,
        final boolean isUp,
        final boolean isLoopback,
        final String[] addresses) {
      this.name = name;
      this.description = description;
      this.isUp = isUp;
      this.isLoopback = isLoopback;
      this.addresses = addresses != null ? addresses.clone() : new String[0];
    }
  }

  // Native result classes
  private static final class Http2ConnectionResult {
    public final int errorCode;
    public final int availableStreams;

    public Http2ConnectionResult(final int errorCode, final int availableStreams) {
      this.errorCode = errorCode;
      this.availableStreams = availableStreams;
    }
  }

  private static final class Http3ConnectionResult {
    public final int errorCode;
    public final long roundTripTimeMs;

    public Http3ConnectionResult(final int errorCode, final long roundTripTimeMs) {
      this.errorCode = errorCode;
      this.roundTripTimeMs = roundTripTimeMs;
    }
  }

  private static final class Http2ResponseResult {
    public final int errorCode;
    public final int statusCode;
    public final String[] headers;
    public final byte[] body;
    public final int streamId;
    public final boolean serverPush;

    public Http2ResponseResult(
        final int errorCode,
        final int statusCode,
        final String[] headers,
        final byte[] body,
        final int streamId,
        final boolean serverPush) {
      this.errorCode = errorCode;
      this.statusCode = statusCode;
      this.headers = headers;
      this.body = body;
      this.streamId = streamId;
      this.serverPush = serverPush;
    }
  }

  private static final class WebSocketConnectionResult {
    public final int errorCode;
    public final String selectedProtocol;

    public WebSocketConnectionResult(final int errorCode, final String selectedProtocol) {
      this.errorCode = errorCode;
      this.selectedProtocol = selectedProtocol;
    }
  }

  private static final class WebSocketReceiveResult {
    public final int errorCode;
    public final int bytesReceived;
    public final int messageType;
    public final boolean isFinal;

    public WebSocketReceiveResult(
        final int errorCode,
        final int bytesReceived,
        final int messageType,
        final boolean isFinal) {
      this.errorCode = errorCode;
      this.bytesReceived = bytesReceived;
      this.messageType = messageType;
      this.isFinal = isFinal;
    }
  }

  private static final class NetworkInterfaceInfo {
    public final String name;
    public final String description;
    public final boolean isUp;
    public final boolean isLoopback;
    public final String[] addresses;

    public NetworkInterfaceInfo(
        final String name,
        final String description,
        final boolean isUp,
        final boolean isLoopback,
        final String[] addresses) {
      this.name = name;
      this.description = description;
      this.isUp = isUp;
      this.isLoopback = isLoopback;
      this.addresses = addresses;
    }
  }
}
