package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.exception.WasiErrorCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI Preview 2 network operations.
 *
 * <p>This class implements the WASI Preview 2 network operations as defined in the WIT interfaces
 * `wasi:sockets/tcp`, `wasi:sockets/udp`, and `wasi:http/outgoing-handler`. It provides async
 * networking capabilities with proper resource management and error handling.
 *
 * <p>Supported network operations:
 *
 * <ul>
 *   <li>TCP socket creation, connection, and data transfer
 *   <li>UDP socket creation and datagram operations
 *   <li>HTTP client operations with async support
 *   <li>Network address resolution and validation
 *   <li>Socket resource lifecycle management
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiNetworkOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiNetworkOperations.class.getName());

  /** Address family for IPv4. */
  public static final int AF_INET = 2;

  /** Address family for IPv6. */
  public static final int AF_INET6 = 10;

  /** Maximum UDP datagram size. */
  private static final int MAX_UDP_DATAGRAM_SIZE = 65507;

  /** The WASI context this network operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Socket handle generator. */
  private final AtomicLong socketHandleGenerator = new AtomicLong(1);

  /** Active sockets tracking. */
  private final Map<Long, SocketInfo> activeSockets = new ConcurrentHashMap<>();

  /**
   * Creates a new WASI network operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiNetworkOperations(final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info("Created WASI network operations handler");
  }

  /**
   * Creates a TCP socket.
   *
   * <p>WIT interface: wasi:sockets/tcp.create-tcp-socket
   *
   * @param addressFamily the address family (AF_INET or AF_INET6)
   * @return the TCP socket handle
   * @throws WasiException if socket creation fails
   */
  public long createTcpSocket(final int addressFamily) throws WasiException {
    validateAddressFamily(addressFamily);

    LOGGER.fine(() -> String.format("Creating TCP socket: addressFamily=%d", addressFamily));

    try {
      final long socketHandle = socketHandleGenerator.getAndIncrement();

      final int result =
          nativeCreateTcpSocket(wasiContext.getNativeHandle(), socketHandle, addressFamily);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to create TCP socket: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      final SocketInfo socketInfo = new SocketInfo(socketHandle, SocketType.TCP, addressFamily);
      activeSockets.put(socketHandle, socketInfo);

      LOGGER.fine(
          () ->
              String.format(
                  "Created TCP socket: handle=%d, addressFamily=%d", socketHandle, addressFamily));
      return socketHandle;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to create TCP socket", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to create TCP socket", e);
      throw new WasiException("TCP socket creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Binds a TCP socket to a local address.
   *
   * <p>WIT interface: wasi:sockets/tcp.tcp-socket.bind
   *
   * @param socketHandle the TCP socket handle
   * @param address the address to bind to
   * @param port the port to bind to
   * @throws WasiException if binding fails
   */
  public void bindTcp(final long socketHandle, final String address, final int port)
      throws WasiException {
    JniValidation.requireNonEmpty(address, "address");
    JniValidation.requireValidPort(port);
    validateTcpSocket(socketHandle);

    LOGGER.fine(
        () ->
            String.format(
                "Binding TCP socket: handle=%d, address=%s, port=%d", socketHandle, address, port));

    try {
      final int result = nativeBindTcp(wasiContext.getNativeHandle(), socketHandle, address, port);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to bind TCP socket: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      // Update socket state
      final SocketInfo socketInfo = activeSockets.get(socketHandle);
      if (socketInfo != null) {
        socketInfo.state = SocketState.BOUND;
        socketInfo.remoteAddress = address;
        socketInfo.remotePort = port;
      }

      LOGGER.fine(() -> String.format("Bound TCP socket: handle=%d", socketHandle));

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to bind TCP socket: " + socketHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to bind TCP socket: " + socketHandle, e);
      throw new WasiException("TCP bind failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Starts listening on a TCP socket.
   *
   * <p>WIT interface: wasi:sockets/tcp.tcp-socket.listen
   *
   * @param socketHandle the TCP socket handle
   * @param backlog the maximum number of pending connections
   * @throws WasiException if listen operation fails
   */
  public void listenTcp(final long socketHandle, final int backlog) throws WasiException {
    if (backlog < 0 || backlog > 1024) {
      throw new WasiException("Invalid backlog: " + backlog, WasiErrorCode.EINVAL);
    }
    validateTcpSocket(socketHandle);

    final SocketInfo socketInfo = activeSockets.get(socketHandle);
    if (socketInfo.state != SocketState.BOUND) {
      throw new WasiException(
          "TCP socket must be bound before listening: " + socketHandle, WasiErrorCode.EINVAL);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Setting TCP socket to listen: handle=%d, backlog=%d", socketHandle, backlog));

    try {
      final int result = nativeListenTcp(wasiContext.getNativeHandle(), socketHandle, backlog);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to listen on TCP socket: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      // Update socket state
      socketInfo.state = SocketState.LISTENING;
      LOGGER.fine(() -> String.format("TCP socket listening: handle=%d", socketHandle));

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to listen on TCP socket: " + socketHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to listen on TCP socket: " + socketHandle, e);
      throw new WasiException("TCP listen failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Accepts a connection on a listening TCP socket.
   *
   * <p>WIT interface: wasi:sockets/tcp.tcp-socket.accept
   *
   * @param socketHandle the listening TCP socket handle
   * @return the handle of the accepted connection
   * @throws WasiException if accept operation fails
   */
  public long acceptTcp(final long socketHandle) throws WasiException {
    validateTcpSocket(socketHandle);

    final SocketInfo socketInfo = activeSockets.get(socketHandle);
    if (socketInfo.state != SocketState.LISTENING) {
      throw new WasiException(
          "TCP socket must be listening to accept connections: " + socketHandle,
          WasiErrorCode.EINVAL);
    }

    LOGGER.fine(() -> String.format("Accepting TCP connection: handle=%d", socketHandle));

    try {
      final TcpAcceptResult result = nativeAcceptTcp(wasiContext.getNativeHandle(), socketHandle);

      if (result.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
        throw new WasiException(
            "Failed to accept TCP connection: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      final long acceptedSocketHandle = socketHandleGenerator.getAndIncrement();
      final SocketInfo acceptedSocketInfo =
          new SocketInfo(acceptedSocketHandle, SocketType.TCP, socketInfo.addressFamily);
      acceptedSocketInfo.state = SocketState.CONNECTED;
      acceptedSocketInfo.remoteAddress = result.remoteAddress;
      acceptedSocketInfo.remotePort = result.remotePort;

      activeSockets.put(acceptedSocketHandle, acceptedSocketInfo);

      LOGGER.fine(
          () ->
              String.format(
                  "Accepted TCP connection: handle=%d, from=%s:%d",
                  acceptedSocketHandle, result.remoteAddress, result.remotePort));

      return acceptedSocketHandle;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to accept TCP connection: " + socketHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to accept TCP connection: " + socketHandle, e);
      throw new WasiException("TCP accept failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Connects a TCP socket to a remote address.
   *
   * <p>WIT interface: wasi:sockets/tcp.tcp-socket.connect
   *
   * @param socketHandle the TCP socket handle
   * @param address the address to connect to
   * @param port the port to connect to
   * @throws WasiException if connection fails
   */
  public void connectTcp(final long socketHandle, final String address, final int port)
      throws WasiException {
    JniValidation.requireNonEmpty(address, "address");
    JniValidation.requireValidPort(port);
    validateTcpSocket(socketHandle);

    LOGGER.fine(
        () ->
            String.format(
                "Connecting TCP socket: handle=%d, address=%s, port=%d",
                socketHandle, address, port));

    try {
      final int result =
          nativeConnectTcp(wasiContext.getNativeHandle(), socketHandle, address, port);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to connect TCP socket: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      // Update socket state
      final SocketInfo socketInfo = activeSockets.get(socketHandle);
      if (socketInfo != null) {
        socketInfo.state = SocketState.CONNECTED;
        socketInfo.remoteAddress = address;
        socketInfo.remotePort = port;
      }

      LOGGER.fine(() -> String.format("Connected TCP socket: handle=%d", socketHandle));

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to connect TCP socket: " + socketHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to connect TCP socket: " + socketHandle, e);
      throw new WasiException("TCP connect failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Sends data via TCP socket.
   *
   * <p>WIT interface: wasi:sockets/tcp.tcp-socket.send
   *
   * @param socketHandle the TCP socket handle
   * @param data the data to send
   * @return the number of bytes sent
   * @throws WasiException if send fails
   */
  public int sendTcp(final long socketHandle, final ByteBuffer data) throws WasiException {
    JniValidation.requireNonNull(data, "data");
    validateTcpSocket(socketHandle);

    final SocketInfo socketInfo = activeSockets.get(socketHandle);
    if (socketInfo.state != SocketState.CONNECTED) {
      throw new WasiException(
          "TCP socket must be connected to send data: " + socketHandle, WasiErrorCode.ENOTCONN);
    }

    final int dataSize = data.remaining();
    if (dataSize == 0) {
      return 0;
    }

    LOGGER.fine(
        () -> String.format("Sending TCP data: handle=%d, size=%d", socketHandle, dataSize));

    try {
      final byte[] buffer = new byte[dataSize];
      data.get(buffer);

      final TcpSendResult result =
          nativeSendTcp(wasiContext.getNativeHandle(), socketHandle, buffer, dataSize);

      if (result.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
        throw new WasiException(
            "Failed to send TCP data: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine(
          () ->
              String.format("Sent TCP data: handle=%d, bytes=%d", socketHandle, result.bytesSent));

      return result.bytesSent;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to send TCP data", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to send TCP data", e);
      throw new WasiException("TCP send failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Receives data from TCP socket.
   *
   * <p>WIT interface: wasi:sockets/tcp.tcp-socket.receive
   *
   * @param socketHandle the TCP socket handle
   * @param buffer the buffer to receive data into
   * @return the number of bytes received
   * @throws WasiException if receive fails
   */
  public int receiveTcp(final long socketHandle, final ByteBuffer buffer) throws WasiException {
    JniValidation.requireNonNull(buffer, "buffer");
    validateTcpSocket(socketHandle);

    final SocketInfo socketInfo = activeSockets.get(socketHandle);
    if (socketInfo.state != SocketState.CONNECTED) {
      throw new WasiException(
          "TCP socket must be connected to receive data: " + socketHandle, WasiErrorCode.ENOTCONN);
    }

    final int maxSize = buffer.remaining();
    if (maxSize == 0) {
      return 0;
    }

    LOGGER.fine(
        () -> String.format("Receiving TCP data: handle=%d, maxSize=%d", socketHandle, maxSize));

    try {
      final byte[] tempBuffer = new byte[maxSize];
      final TcpReceiveResult result =
          nativeReceiveTcp(wasiContext.getNativeHandle(), socketHandle, tempBuffer, maxSize);

      if (result.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
        throw new WasiException(
            "Failed to receive TCP data: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      if (result.bytesReceived > 0) {
        buffer.put(tempBuffer, 0, result.bytesReceived);
      }

      LOGGER.fine(
          () ->
              String.format(
                  "Received TCP data: handle=%d, bytes=%d", socketHandle, result.bytesReceived));

      return result.bytesReceived;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to receive TCP data", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to receive TCP data", e);
      throw new WasiException("TCP receive failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Creates a UDP socket.
   *
   * <p>WIT interface: wasi:sockets/udp.create-udp-socket
   *
   * @param addressFamily the address family (AF_INET or AF_INET6)
   * @return the UDP socket handle
   * @throws WasiException if socket creation fails
   */
  public long createUdpSocket(final int addressFamily) throws WasiException {
    validateAddressFamily(addressFamily);

    LOGGER.fine(() -> String.format("Creating UDP socket: addressFamily=%d", addressFamily));

    try {
      final long socketHandle = socketHandleGenerator.getAndIncrement();

      final int result =
          nativeCreateUdpSocket(wasiContext.getNativeHandle(), socketHandle, addressFamily);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to create UDP socket: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      final SocketInfo socketInfo = new SocketInfo(socketHandle, SocketType.UDP, addressFamily);
      activeSockets.put(socketHandle, socketInfo);

      LOGGER.fine(
          () ->
              String.format(
                  "Created UDP socket: handle=%d, addressFamily=%d", socketHandle, addressFamily));
      return socketHandle;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to create UDP socket", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to create UDP socket", e);
      throw new WasiException("UDP socket creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Sends data via UDP socket.
   *
   * <p>WIT interface: wasi:sockets/udp.udp-socket.send
   *
   * @param socketHandle the UDP socket handle
   * @param data the data to send
   * @param address the destination address
   * @param port the destination port
   * @throws WasiException if send fails
   */
  public void sendUdp(
      final long socketHandle, final ByteBuffer data, final String address, final int port)
      throws WasiException {
    JniValidation.requireNonNull(data, "data");
    JniValidation.requireNonEmpty(address, "address");
    JniValidation.requireValidPort(port);
    validateUdpSocket(socketHandle);

    final int dataSize = data.remaining();
    if (dataSize > MAX_UDP_DATAGRAM_SIZE) {
      throw new WasiException(
          "UDP datagram too large: " + dataSize + " > " + MAX_UDP_DATAGRAM_SIZE,
          WasiErrorCode.EMSGSIZE);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Sending UDP data: handle=%d, address=%s, port=%d, size=%d",
                socketHandle, address, port, dataSize));

    try {
      final byte[] buffer = new byte[dataSize];
      data.get(buffer);

      final int result =
          nativeSendUdp(
              wasiContext.getNativeHandle(), socketHandle, buffer, dataSize, address, port);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to send UDP data: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine(
          () -> String.format("Sent UDP data: handle=%d, bytes=%d", socketHandle, dataSize));

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to send UDP data", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to send UDP data", e);
      throw new WasiException("UDP send failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Receives data from UDP socket.
   *
   * <p>WIT interface: wasi:sockets/udp.udp-socket.receive
   *
   * @param socketHandle the UDP socket handle
   * @param buffer the buffer to receive data into
   * @return the UDP datagram information
   * @throws WasiException if receive fails
   */
  public UdpDatagram receiveUdp(final long socketHandle, final ByteBuffer buffer)
      throws WasiException {
    JniValidation.requireNonNull(buffer, "buffer");
    validateUdpSocket(socketHandle);

    final int maxSize = Math.min(buffer.remaining(), MAX_UDP_DATAGRAM_SIZE);
    if (maxSize == 0) {
      throw new WasiException("Buffer has no remaining space", WasiErrorCode.EINVAL);
    }

    LOGGER.fine(
        () -> String.format("Receiving UDP data: handle=%d, maxSize=%d", socketHandle, maxSize));

    try {
      final byte[] tempBuffer = new byte[maxSize];
      final UdpReceiveResult result =
          nativeReceiveUdp(wasiContext.getNativeHandle(), socketHandle, tempBuffer, maxSize);

      if (result.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
        throw new WasiException(
            "Failed to receive UDP data: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      if (result.bytesReceived > 0) {
        buffer.put(tempBuffer, 0, result.bytesReceived);
      }

      final UdpDatagram datagram =
          new UdpDatagram(result.bytesReceived, result.sourceAddress, result.sourcePort);
      LOGGER.fine(
          () ->
              String.format(
                  "Received UDP data: handle=%d, bytes=%d, from=%s:%d",
                  socketHandle, result.bytesReceived, result.sourceAddress, result.sourcePort));

      return datagram;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to receive UDP data", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to receive UDP data", e);
      throw new WasiException("UDP receive failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Makes an HTTP request.
   *
   * <p>WIT interface: wasi:http/outgoing-handler.handle
   *
   * @param method the HTTP method
   * @param uri the request URI
   * @param headers the request headers
   * @param body the request body (can be null)
   * @return the HTTP response
   * @throws WasiException if the request fails
   */
  public WasiPreview2Operations.WasiHttpResponse httpRequest(
      final String method,
      final String uri,
      final Map<String, String> headers,
      final ByteBuffer body)
      throws WasiException {
    JniValidation.requireNonEmpty(method, "method");
    JniValidation.requireNonEmpty(uri, "uri");
    JniValidation.requireNonNull(headers, "headers");

    LOGGER.fine(() -> String.format("HTTP request: method=%s, uri=%s", method, uri));

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

      // Make native HTTP request
      final HttpResponse response =
          nativeHttpRequest(
              wasiContext.getNativeHandle(), method, uri, headerArray, bodyBytes, bodySize);

      if (response.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(response.errorCode);
        throw new WasiException(
            "HTTP request failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      // Parse response headers
      final Map<String, String> responseHeaders = new ConcurrentHashMap<>();
      if (response.headers != null) {
        for (int i = 0; i < response.headers.length; i += 2) {
          responseHeaders.put(response.headers[i], response.headers[i + 1]);
        }
      }

      // Create response body buffer
      ByteBuffer responseBody = null;
      if (response.body != null && response.body.length > 0) {
        responseBody = ByteBuffer.wrap(response.body);
      }

      final WasiPreview2Operations.WasiHttpResponse httpResponse =
          new WasiPreview2Operations.WasiHttpResponse(
              response.statusCode, responseHeaders, responseBody);

      LOGGER.fine(() -> String.format("HTTP request completed: status=%d", response.statusCode));
      return httpResponse;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "HTTP request failed", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "HTTP request failed", e);
      throw new WasiException("HTTP request failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Closes a socket and releases its resources.
   *
   * @param socketHandle the socket handle to close
   * @throws WasiException if the close operation fails
   */
  public void closeSocket(final long socketHandle) {
    final SocketInfo socketInfo = activeSockets.get(socketHandle);
    if (socketInfo == null) {
      LOGGER.fine(() -> String.format("Socket already closed or invalid: handle=%d", socketHandle));
      return;
    }

    LOGGER.fine(() -> String.format("Closing socket: handle=%d", socketHandle));

    try {
      final int result = nativeCloseSocket(wasiContext.getNativeHandle(), socketHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        LOGGER.warning(
            "Failed to close socket "
                + socketHandle
                + ": "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"));
      }

      activeSockets.remove(socketHandle);
      LOGGER.fine(() -> String.format("Closed socket: handle=%d", socketHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing socket: " + socketHandle, e);
      activeSockets.remove(socketHandle); // Remove anyway to prevent leaks
    }
  }

  /** Closes all active sockets and releases resources. */
  public void close() {
    LOGGER.info("Closing all network operations");

    for (final Long socketHandle : activeSockets.keySet()) {
      try {
        closeSocket(socketHandle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error closing socket during shutdown: " + socketHandle, e);
      }
    }

    activeSockets.clear();
    LOGGER.info("Network operations closed successfully");
  }

  /** Validates address family. */
  private void validateAddressFamily(final int addressFamily) throws WasiException {
    if (addressFamily != AF_INET && addressFamily != AF_INET6) {
      throw new WasiException("Invalid address family: " + addressFamily, WasiErrorCode.EINVAL);
    }
  }

  /** Validates that a socket is a TCP socket. */
  private void validateTcpSocket(final long socketHandle) throws WasiException {
    final SocketInfo socketInfo = activeSockets.get(socketHandle);
    if (socketInfo == null) {
      throw new WasiException("Invalid socket handle: " + socketHandle, WasiErrorCode.EBADF);
    }
    if (socketInfo.type != SocketType.TCP) {
      throw new WasiException("Socket is not a TCP socket: " + socketHandle, WasiErrorCode.EBADF);
    }
  }

  /** Validates that a socket is a UDP socket. */
  private void validateUdpSocket(final long socketHandle) throws WasiException {
    final SocketInfo socketInfo = activeSockets.get(socketHandle);
    if (socketInfo == null) {
      throw new WasiException("Invalid socket handle: " + socketHandle, WasiErrorCode.EBADF);
    }
    if (socketInfo.type != SocketType.UDP) {
      throw new WasiException("Socket is not a UDP socket: " + socketHandle, WasiErrorCode.EBADF);
    }
  }

  // Native method declarations
  private static native int nativeCreateTcpSocket(
      long contextHandle, long socketHandle, int addressFamily);

  private static native int nativeBindTcp(
      long contextHandle, long socketHandle, String address, int port);

  private static native int nativeListenTcp(long contextHandle, long socketHandle, int backlog);

  private static native TcpAcceptResult nativeAcceptTcp(long contextHandle, long socketHandle);

  private static native int nativeConnectTcp(
      long contextHandle, long socketHandle, String address, int port);

  private static native TcpSendResult nativeSendTcp(
      long contextHandle, long socketHandle, byte[] data, int dataSize);

  private static native TcpReceiveResult nativeReceiveTcp(
      long contextHandle, long socketHandle, byte[] buffer, int bufferSize);

  private static native int nativeCreateUdpSocket(
      long contextHandle, long socketHandle, int addressFamily);

  private static native int nativeSendUdp(
      long contextHandle, long socketHandle, byte[] data, int dataSize, String address, int port);

  private static native UdpReceiveResult nativeReceiveUdp(
      long contextHandle, long socketHandle, byte[] buffer, int bufferSize);

  private static native HttpResponse nativeHttpRequest(
      long contextHandle, String method, String uri, String[] headers, byte[] body, int bodySize);

  private static native int nativeCloseSocket(long contextHandle, long socketHandle);

  /** Socket type enumeration. */
  public enum SocketType {
    TCP,
    UDP
  }

  /** Socket state enumeration. */
  public enum SocketState {
    CREATED,
    BOUND,
    LISTENING,
    CONNECTED,
    CLOSED
  }

  /** Socket information class. */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification = "Data class for socket information, fields used for network management")
  public static final class SocketInfo {
    public final long handle;
    public final SocketType type;
    public final int addressFamily;
    public final long createdAt;
    public volatile SocketState state = SocketState.CREATED;
    public volatile String remoteAddress;
    public volatile int remotePort;

    /**
     * Creates socket info.
     *
     * @param handle the socket handle
     * @param type the socket type
     * @param addressFamily the address family
     */
    public SocketInfo(final long handle, final SocketType type, final int addressFamily) {
      this.handle = handle;
      this.type = type;
      this.addressFamily = addressFamily;
      this.createdAt = System.currentTimeMillis();
    }
  }

  /** UDP datagram information. */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification = "Data class for UDP datagram information, fields used for network I/O")
  public static final class UdpDatagram {
    public final int bytesReceived;
    public final String sourceAddress;
    public final int sourcePort;

    /**
     * Creates UDP datagram information.
     *
     * @param bytesReceived number of bytes received
     * @param sourceAddress source IP address
     * @param sourcePort source port number
     */
    public UdpDatagram(final int bytesReceived, final String sourceAddress, final int sourcePort) {
      this.bytesReceived = bytesReceived;
      this.sourceAddress = sourceAddress;
      this.sourcePort = sourcePort;
    }
  }

  /** UDP receive result from native code. */
  private static final class UdpReceiveResult {
    public final int errorCode;
    public final int bytesReceived;
    public final String sourceAddress;
    public final int sourcePort;

    public UdpReceiveResult(
        final int errorCode,
        final int bytesReceived,
        final String sourceAddress,
        final int sourcePort) {
      this.errorCode = errorCode;
      this.bytesReceived = bytesReceived;
      this.sourceAddress = sourceAddress;
      this.sourcePort = sourcePort;
    }
  }

  /** TCP accept result from native code. */
  private static final class TcpAcceptResult {
    public final int errorCode;
    public final String remoteAddress;
    public final int remotePort;

    /**
     * Creates a TCP accept result.
     *
     * @param errorCode the error code
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     */
    public TcpAcceptResult(final int errorCode, final String remoteAddress, final int remotePort) {
      this.errorCode = errorCode;
      this.remoteAddress = remoteAddress;
      this.remotePort = remotePort;
    }
  }

  /** TCP send result from native code. */
  private static final class TcpSendResult {
    public final int errorCode;
    public final int bytesSent;

    /**
     * Creates a TCP send result.
     *
     * @param errorCode the error code
     * @param bytesSent the number of bytes sent
     */
    public TcpSendResult(final int errorCode, final int bytesSent) {
      this.errorCode = errorCode;
      this.bytesSent = bytesSent;
    }
  }

  /** TCP receive result from native code. */
  private static final class TcpReceiveResult {
    public final int errorCode;
    public final int bytesReceived;

    /**
     * Creates a TCP receive result.
     *
     * @param errorCode the error code
     * @param bytesReceived the number of bytes received
     */
    public TcpReceiveResult(final int errorCode, final int bytesReceived) {
      this.errorCode = errorCode;
      this.bytesReceived = bytesReceived;
    }
  }

  /** HTTP response from native code. */
  private static final class HttpResponse {
    public final int errorCode;
    public final int statusCode;
    public final String[] headers;
    public final byte[] body;

    public HttpResponse(
        final int errorCode, final int statusCode, final String[] headers, final byte[] body) {
      this.errorCode = errorCode;
      this.statusCode = statusCode;
      this.headers = headers;
      this.body = body;
    }
  }
}
