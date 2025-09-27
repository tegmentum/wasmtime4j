package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI Preview 2 operations with WIT interface support.
 *
 * <p>This class provides comprehensive WASI Preview 2 operations including component-based WASI
 * with WIT (WebAssembly Interface Types) interfaces and async I/O capabilities. WASI Preview 2
 * introduces the component model which enables more sophisticated composition and async operations.
 *
 * <p>Key WASI Preview 2 features:
 *
 * <ul>
 *   <li>Component-based WASI operations with WIT interfaces
 *   <li>Async I/O operations with proper completion handling
 *   <li>Resource management with proper lifecycle handling
 *   <li>Stream operations for continuous data processing
 *   <li>Enhanced networking with async TCP/UDP/HTTP support
 *   <li>Improved error handling and capability management
 * </ul>
 *
 * <p>WIT Interface Support:
 *
 * <ul>
 *   <li>wasi:filesystem/types - File system types and operations
 *   <li>wasi:io/streams - Stream-based I/O operations
 *   <li>wasi:sockets/network - Network socket operations
 *   <li>wasi:http/types - HTTP client and server operations
 *   <li>wasi:clocks/wall-clock - Clock and time operations
 *   <li>wasi:random/random - Random number generation
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiPreview2Operations {

  private static final Logger LOGGER = Logger.getLogger(WasiPreview2Operations.class.getName());

  /** Maximum number of concurrent async operations. */
  private static final int MAX_ASYNC_OPERATIONS = 1000;

  /** The WASI context this operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Resource handle generator. */
  private final AtomicLong resourceHandleGenerator = new AtomicLong(1);

  /** Active async operations tracking. */
  private final Map<Long, CompletableFuture<Void>> activeOperations = new ConcurrentHashMap<>();

  /** Stream operations handler. */
  private final WasiStreamOperations streamOperations;

  /** Network operations handler. */
  private final WasiNetworkOperations networkOperations;

  /**
   * Creates a new WASI Preview 2 operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @throws JniException if the wasiContext is null
   */
  public WasiPreview2Operations(final WasiContext wasiContext) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    this.wasiContext = wasiContext;
    this.asyncExecutor =
        Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors() * 2, 16),
            r -> {
              final Thread t = new Thread(r, "wasi-preview2-async");
              t.setDaemon(true);
              return t;
            });
    this.streamOperations = new WasiStreamOperations(wasiContext, asyncExecutor);
    this.networkOperations = new WasiNetworkOperations(wasiContext, asyncExecutor);

    LOGGER.info("Created WASI Preview 2 operations handler with async support");
  }

  /**
   * Creates a new resource handle.
   *
   * <p>WASI Preview 2 uses resource handles to manage component resources with proper lifecycle
   * management.
   *
   * @param resourceType the type of resource to create
   * @param data the resource initialization data
   * @return the new resource handle
   * @throws WasiException if resource creation fails
   */
  public long createResource(final String resourceType, final ByteBuffer data) {
    JniValidation.requireNonEmpty(resourceType, "resourceType");
    JniValidation.requireNonNull(data, "data");

    LOGGER.fine(
        () ->
            String.format(
                "Creating resource: type=%s, dataSize=%d", resourceType, data.remaining()));

    try {
      final long handle = resourceHandleGenerator.getAndIncrement();

      // Create resource through native interface
      final int result =
          nativeCreateResource(
              wasiContext.getNativeHandle(), handle, resourceType, data, data.remaining());

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to create resource: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine(
          () -> String.format("Created resource: handle=%d, type=%s", handle, resourceType));
      return handle;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to create resource: " + resourceType, e);
      throw new WasiException("Resource creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Destroys a resource handle.
   *
   * @param handle the resource handle to destroy
   * @throws WasiException if resource destruction fails
   */
  public void destroyResource(final long handle) {
    LOGGER.fine(() -> String.format("Destroying resource: handle=%d", handle));

    try {
      final int result = nativeDestroyResource(wasiContext.getNativeHandle(), handle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to destroy resource: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine(() -> String.format("Destroyed resource: handle=%d", handle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to destroy resource: " + handle, e);
      throw new WasiException("Resource destruction failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Opens an input stream for reading data asynchronously.
   *
   * <p>WIT interface: wasi:io/streams.input-stream
   *
   * @param resourceHandle the resource handle to open stream for
   * @return the input stream handle
   * @throws WasiException if stream creation fails
   */
  public long openInputStream(final long resourceHandle) {
    LOGGER.fine(() -> String.format("Opening input stream: resource=%d", resourceHandle));

    try {
      return streamOperations.openInputStream(resourceHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to open input stream for resource: " + resourceHandle, e);
      throw new WasiException("Input stream creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Opens an output stream for writing data asynchronously.
   *
   * <p>WIT interface: wasi:io/streams.output-stream
   *
   * @param resourceHandle the resource handle to open stream for
   * @return the output stream handle
   * @throws WasiException if stream creation fails
   */
  public long openOutputStream(final long resourceHandle) {
    LOGGER.fine(() -> String.format("Opening output stream: resource=%d", resourceHandle));

    try {
      return streamOperations.openOutputStream(resourceHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to open output stream for resource: " + resourceHandle, e);
      throw new WasiException(
          "Output stream creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Reads data asynchronously from an input stream.
   *
   * <p>WIT interface: wasi:io/streams.input-stream.read
   *
   * @param streamHandle the input stream handle
   * @param buffer the buffer to read data into
   * @return CompletableFuture that resolves to the number of bytes read
   */
  public CompletableFuture<Integer> readAsync(final long streamHandle, final ByteBuffer buffer) {
    JniValidation.requireNonNull(buffer, "buffer");

    LOGGER.fine(
        () ->
            String.format(
                "Async read: stream=%d, bufferSize=%d", streamHandle, buffer.remaining()));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return streamOperations.read(streamHandle, buffer);

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async read failed for stream: " + streamHandle, e);
            throw new RuntimeException("Async read failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Writes data asynchronously to an output stream.
   *
   * <p>WIT interface: wasi:io/streams.output-stream.write
   *
   * @param streamHandle the output stream handle
   * @param buffer the buffer containing data to write
   * @return CompletableFuture that resolves to the number of bytes written
   */
  public CompletableFuture<Integer> writeAsync(final long streamHandle, final ByteBuffer buffer) {
    JniValidation.requireNonNull(buffer, "buffer");

    LOGGER.fine(
        () ->
            String.format(
                "Async write: stream=%d, bufferSize=%d", streamHandle, buffer.remaining()));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return streamOperations.write(streamHandle, buffer);

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async write failed for stream: " + streamHandle, e);
            throw new RuntimeException("Async write failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates a TCP socket for network communication.
   *
   * <p>WIT interface: wasi:sockets/tcp.create-tcp-socket
   *
   * @param addressFamily the address family (IPv4 or IPv6)
   * @return the TCP socket handle
   * @throws WasiException if socket creation fails
   */
  public long createTcpSocket(final int addressFamily) {
    LOGGER.fine(() -> String.format("Creating TCP socket: addressFamily=%d", addressFamily));

    try {
      return networkOperations.createTcpSocket(addressFamily);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to create TCP socket", e);
      throw new WasiException("TCP socket creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Connects a TCP socket asynchronously.
   *
   * <p>WIT interface: wasi:sockets/tcp.tcp-socket.connect
   *
   * @param socketHandle the TCP socket handle
   * @param address the address to connect to
   * @param port the port to connect to
   * @return CompletableFuture that completes when connection is established
   */
  public CompletableFuture<Void> connectTcpAsync(
      final long socketHandle, final String address, final int port) {
    JniValidation.requireNonEmpty(address, "address");
    JniValidation.requireValidPort(port);

    LOGGER.fine(
        () ->
            String.format(
                "Async TCP connect: socket=%d, address=%s, port=%d", socketHandle, address, port));

    return CompletableFuture.runAsync(
        () -> {
          try {
            networkOperations.connectTcp(socketHandle, address, port);
            LOGGER.fine(() -> String.format("TCP connection established: socket=%d", socketHandle));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async TCP connect failed", e);
            throw new RuntimeException("TCP connect failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates a UDP socket for network communication.
   *
   * <p>WIT interface: wasi:sockets/udp.create-udp-socket
   *
   * @param addressFamily the address family (IPv4 or IPv6)
   * @return the UDP socket handle
   * @throws WasiException if socket creation fails
   */
  public long createUdpSocket(final int addressFamily) {
    LOGGER.fine(() -> String.format("Creating UDP socket: addressFamily=%d", addressFamily));

    try {
      return networkOperations.createUdpSocket(addressFamily);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to create UDP socket", e);
      throw new WasiException("UDP socket creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Sends data via UDP socket asynchronously.
   *
   * <p>WIT interface: wasi:sockets/udp.udp-socket.send
   *
   * @param socketHandle the UDP socket handle
   * @param data the data to send
   * @param address the destination address
   * @param port the destination port
   * @return CompletableFuture that completes when data is sent
   */
  public CompletableFuture<Void> sendUdpAsync(
      final long socketHandle, final ByteBuffer data, final String address, final int port) {
    JniValidation.requireNonNull(data, "data");
    JniValidation.requireNonEmpty(address, "address");
    JniValidation.requireValidPort(port);

    LOGGER.fine(
        () ->
            String.format(
                "Async UDP send: socket=%d, address=%s, port=%d, dataSize=%d",
                socketHandle, address, port, data.remaining()));

    return CompletableFuture.runAsync(
        () -> {
          try {
            networkOperations.sendUdp(socketHandle, data, address, port);
            LOGGER.fine(() -> String.format("UDP data sent: socket=%d", socketHandle));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async UDP send failed", e);
            throw new RuntimeException("UDP send failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates an HTTP request.
   *
   * <p>WIT interface: wasi:http/outgoing-handler.handle
   *
   * @param method the HTTP method
   * @param uri the request URI
   * @param headers the request headers
   * @param body the request body (can be null)
   * @return CompletableFuture that resolves to the HTTP response
   */
  public CompletableFuture<WasiHttpResponse> httpRequestAsync(
      final String method,
      final String uri,
      final Map<String, String> headers,
      final ByteBuffer body) {
    JniValidation.requireNonEmpty(method, "method");
    JniValidation.requireNonEmpty(uri, "uri");
    JniValidation.requireNonNull(headers, "headers");

    LOGGER.fine(() -> String.format("Async HTTP request: method=%s, uri=%s", method, uri));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return networkOperations.httpRequest(method, uri, headers, body);

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async HTTP request failed", e);
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Polls for async operation completion.
   *
   * <p>WIT interface: wasi:io/poll.poll
   *
   * @param pollables list of pollable handles to check
   * @param timeoutNanos timeout in nanoseconds (0 for no timeout)
   * @return list of ready pollable indices
   * @throws WasiException if polling fails
   */
  public List<Integer> poll(final List<Long> pollables, final long timeoutNanos) {
    JniValidation.requireNonNull(pollables, "pollables");
    JniValidation.requireNonNegative(timeoutNanos, "timeoutNanos");

    LOGGER.fine(
        () -> String.format("Polling %d handles, timeout=%d ns", pollables.size(), timeoutNanos));

    try {
      final long[] pollableArray = pollables.stream().mapToLong(Long::longValue).toArray();
      final int[] readyIndices =
          nativePoll(wasiContext.getNativeHandle(), pollableArray, timeoutNanos);

      final List<Integer> result = new java.util.ArrayList<>();
      for (final int index : readyIndices) {
        result.add(index);
      }

      LOGGER.fine(() -> String.format("Poll completed: %d ready", result.size()));
      return result;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Poll operation failed", e);
      throw new WasiException("Poll failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /** Closes the WASI Preview 2 operations handler and cleans up resources. */
  public void close() {
    LOGGER.info("Closing WASI Preview 2 operations handler");

    try {
      // Cancel all active operations
      for (final CompletableFuture<Void> operation : activeOperations.values()) {
        operation.cancel(true);
      }
      activeOperations.clear();

      // Close sub-handlers
      streamOperations.close();
      networkOperations.close();

      // Shutdown executor
      asyncExecutor.shutdown();

      LOGGER.info("WASI Preview 2 operations handler closed successfully");

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing WASI Preview 2 operations", e);
    }
  }

  /**
   * Native method to create a resource.
   *
   * @param contextHandle the WASI context handle
   * @param resourceHandle the resource handle
   * @param resourceType the resource type
   * @param data the initialization data
   * @param dataSize the size of the data
   * @return 0 for success, error code for failure
   */
  private static native int nativeCreateResource(
      long contextHandle, long resourceHandle, String resourceType, ByteBuffer data, int dataSize);

  /**
   * Native method to destroy a resource.
   *
   * @param contextHandle the WASI context handle
   * @param resourceHandle the resource handle
   * @return 0 for success, error code for failure
   */
  private static native int nativeDestroyResource(long contextHandle, long resourceHandle);

  /**
   * Native method to poll for ready handles.
   *
   * @param contextHandle the WASI context handle
   * @param pollables array of pollable handles
   * @param timeoutNanos timeout in nanoseconds
   * @return array of ready handle indices
   */
  private static native int[] nativePoll(long contextHandle, long[] pollables, long timeoutNanos);

  /**
   * Native method to compile a WebAssembly component.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param componentBytes the component bytecode
   * @param componentSize the size of the component data
   * @return component ID on success, -1 on failure
   */
  private static native long nativeCompileComponent(
      long contextHandle, ByteBuffer componentBytes, int componentSize);

  /**
   * Native method to instantiate a compiled component.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param componentId the compiled component ID
   * @return instance ID on success, -1 on failure
   */
  private static native long nativeInstantiateComponent(long contextHandle, long componentId);

  /**
   * Native method to create an input stream.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param instanceId the component instance ID
   * @return stream ID on success, -1 on failure
   */
  private static native long nativeCreateInputStream(long contextHandle, long instanceId);

  /**
   * Native method to create an output stream.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param instanceId the component instance ID
   * @return stream ID on success, -1 on failure
   */
  private static native long nativeCreateOutputStream(long contextHandle, long instanceId);

  /**
   * Native method to read from a stream.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param instanceId the component instance ID
   * @param streamId the stream ID
   * @param buffer the buffer to read into
   * @param bufferSize the size of the buffer
   * @return number of bytes read on success, -1 on failure
   */
  private static native int nativeStreamRead(
      long contextHandle, long instanceId, long streamId, ByteBuffer buffer, int bufferSize);

  /**
   * Native method to write to a stream.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param instanceId the component instance ID
   * @param streamId the stream ID
   * @param buffer the buffer to write from
   * @param bufferSize the size of the buffer
   * @return number of bytes written on success, -1 on failure
   */
  private static native int nativeStreamWrite(
      long contextHandle, long instanceId, long streamId, ByteBuffer buffer, int bufferSize);

  /**
   * Native method to close a stream.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param instanceId the component instance ID
   * @param streamId the stream ID
   * @return 0 on success, error code on failure
   */
  private static native int nativeCloseStream(long contextHandle, long instanceId, long streamId);

  /**
   * Native method to get the status of an async operation.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param operationId the operation ID
   * @return operation status (0=running, 1=completed, 2=failed, 3=cancelled, -1=not found)
   */
  private static native int nativeGetOperationStatus(long contextHandle, long operationId);

  /**
   * Native method to cancel an async operation.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @param operationId the operation ID
   * @return 0 on success, error code on failure
   */
  private static native int nativeCancelOperation(long contextHandle, long operationId);

  /**
   * Native method to cleanup completed operations.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @return 0 on success, error code on failure
   */
  private static native int nativeCleanupOperations(long contextHandle);

  /**
   * Native method to get the number of active async operations.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @return number of active operations
   */
  private static native int nativeGetOperationCount(long contextHandle);

  /**
   * Native method to check if networking is enabled.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @return 1 if enabled, 0 if disabled, -1 on error
   */
  private static native int nativeNetworkingEnabled(long contextHandle);

  /**
   * Native method to check if filesystem is enabled.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @return 1 if enabled, 0 if disabled, -1 on error
   */
  private static native int nativeFilesystemEnabled(long contextHandle);

  /**
   * Native method to check if process spawning is enabled.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @return 1 if enabled, 0 if disabled, -1 on error
   */
  private static native int nativeProcessEnabled(long contextHandle);

  /**
   * Native method to get the number of compiled components.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @return number of compiled components
   */
  private static native int nativeGetComponentCount(long contextHandle);

  /**
   * Native method to get the number of active component instances.
   *
   * @param contextHandle the WASI Preview 2 context handle
   * @return number of active instances
   */
  private static native int nativeGetInstanceCount(long contextHandle);

  /** Represents an HTTP response in WASI Preview 2. */
  public static final class WasiHttpResponse {
    private final int statusCode;
    private final Map<String, String> headers;
    private final ByteBuffer body;

    public WasiHttpResponse(
        final int statusCode, final Map<String, String> headers, final ByteBuffer body) {
      this.statusCode = statusCode;
      this.headers = headers != null ? Map.copyOf(headers) : Map.of();
      this.body = body;
    }

    public int getStatusCode() {
      return statusCode;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    public ByteBuffer getBody() {
      return body;
    }
  }
}
