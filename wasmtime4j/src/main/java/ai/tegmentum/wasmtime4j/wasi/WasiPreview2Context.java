package ai.tegmentum.wasmtime4j.wasi;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for WASI Preview 2 context operations.
 *
 * <p>WasiPreview2Context provides access to the full range of WASI Preview 2 functionality,
 * including async I/O operations, component model features, and WIT interface support.
 *
 * <p>Key WASI Preview 2 features:
 *
 * <ul>
 *   <li>Async stream operations for non-blocking I/O
 *   <li>Component-based resource management
 *   <li>Network operations with TCP/UDP/HTTP support
 *   <li>Enhanced filesystem operations
 *   <li>Pollable resources for event-driven programming
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiConfig config = WasiConfig.builder()
 *     .withWasiVersion(WasiVersion.PREVIEW_2)
 *     .withAsyncOperations(true)
 *     .build();
 *
 * WasiPreview2Context context = WasiPreview2Context.create(config);
 * WasiPreview2Stream stream = context.openInputStream(resourceHandle);
 * CompletableFuture<Integer> bytesRead = stream.readAsync(buffer);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiPreview2Context extends AutoCloseable {

  /**
   * Creates a new WASI Preview 2 context with the specified configuration.
   *
   * @param config the WASI configuration
   * @return a new WasiPreview2Context instance
   * @throws IllegalArgumentException if the configuration is invalid or doesn't specify Preview 2
   */
  static WasiPreview2Context create(final WasiConfig config) {
    if (config.getWasiVersion() != WasiVersion.PREVIEW_2) {
      throw new IllegalArgumentException("Configuration must specify WASI Preview 2");
    }

    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> contextClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.PanamaWasiPreview2Context");
      return (WasiPreview2Context)
          contextClass.getMethod("create", WasiConfig.class).invoke(null, config);
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> contextClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.JniWasiPreview2Context");
        return (WasiPreview2Context)
            contextClass.getMethod("create", WasiConfig.class).invoke(null, config);
      } catch (final ClassNotFoundException e2) {
        throw new RuntimeException(
            "No WasiPreview2Context implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create WASI Preview 2 context", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create WASI Preview 2 context", e);
    }
  }

  // Resource Management

  /**
   * Creates a new resource with the specified type and initialization data.
   *
   * @param resourceType the type of resource to create
   * @param data the initialization data for the resource
   * @return the resource handle
   * @throws WasiException if resource creation fails
   */
  long createResource(String resourceType, ByteBuffer data);

  /**
   * Destroys a resource and releases its associated resources.
   *
   * @param resourceHandle the resource handle to destroy
   * @throws WasiException if resource destruction fails
   */
  void destroyResource(long resourceHandle);

  // Stream Operations

  /**
   * Opens an input stream for the specified resource.
   *
   * @param resourceHandle the resource handle to open a stream for
   * @return a new input stream
   * @throws WasiException if stream creation fails
   */
  WasiPreview2Stream openInputStream(long resourceHandle);

  /**
   * Opens an output stream for the specified resource.
   *
   * @param resourceHandle the resource handle to open a stream for
   * @return a new output stream
   * @throws WasiException if stream creation fails
   */
  WasiPreview2Stream openOutputStream(long resourceHandle);

  /**
   * Opens a bidirectional stream for the specified resource.
   *
   * @param resourceHandle the resource handle to open a stream for
   * @return a new bidirectional stream
   * @throws WasiException if stream creation fails
   */
  WasiPreview2Stream openBidirectionalStream(long resourceHandle);

  // Network Operations

  /**
   * Creates a TCP socket for the specified address family.
   *
   * @param addressFamily the address family (4 for IPv4, 6 for IPv6)
   * @return the TCP socket handle
   * @throws WasiException if socket creation fails
   */
  long createTcpSocket(int addressFamily);

  /**
   * Connects a TCP socket to the specified address and port asynchronously.
   *
   * @param socketHandle the TCP socket handle
   * @param address the address to connect to
   * @param port the port to connect to
   * @return CompletableFuture that completes when connection is established
   */
  CompletableFuture<Void> connectTcpAsync(long socketHandle, String address, int port);

  /**
   * Creates a UDP socket for the specified address family.
   *
   * @param addressFamily the address family (4 for IPv4, 6 for IPv6)
   * @return the UDP socket handle
   * @throws WasiException if socket creation fails
   */
  long createUdpSocket(int addressFamily);

  /**
   * Sends data via UDP socket asynchronously.
   *
   * @param socketHandle the UDP socket handle
   * @param data the data to send
   * @param address the destination address
   * @param port the destination port
   * @return CompletableFuture that completes when data is sent
   */
  CompletableFuture<Void> sendUdpAsync(
      long socketHandle, ByteBuffer data, String address, int port);

  // HTTP Operations

  /**
   * Performs an HTTP request asynchronously.
   *
   * @param method the HTTP method (GET, POST, etc.)
   * @param uri the request URI
   * @param headers the request headers
   * @param body the request body (can be null)
   * @return CompletableFuture that resolves to the HTTP response
   */
  CompletableFuture<WasiHttpResponse> httpRequestAsync(
      String method, String uri, Map<String, String> headers, ByteBuffer body);

  // Filesystem Operations (Preview 2)

  /**
   * Opens a file asynchronously with Preview 2 enhanced error handling.
   *
   * @param path the file path
   * @param openFlags the open flags
   * @param rights the file rights
   * @return CompletableFuture that resolves to the file handle
   */
  CompletableFuture<Long> openFileAsync(String path, int openFlags, long rights);

  /**
   * Reads from a file descriptor asynchronously.
   *
   * @param fd the file descriptor
   * @param buffer the buffer to read into
   * @param offset the file offset to read from
   * @return CompletableFuture that resolves to the number of bytes read
   */
  CompletableFuture<Integer> readFileAsync(int fd, ByteBuffer buffer, long offset);

  /**
   * Writes to a file descriptor asynchronously.
   *
   * @param fd the file descriptor
   * @param buffer the buffer containing data to write
   * @param offset the file offset to write to
   * @return CompletableFuture that resolves to the number of bytes written
   */
  CompletableFuture<Integer> writeFileAsync(int fd, ByteBuffer buffer, long offset);

  // Clock and Random Operations (Preview 2)

  /**
   * Gets the current time for the specified clock asynchronously.
   *
   * @param clockId the clock identifier
   * @param precision the requested precision
   * @return CompletableFuture that resolves to the current time in nanoseconds
   */
  CompletableFuture<Long> getTimeAsync(int clockId, long precision);

  /**
   * Generates random bytes asynchronously.
   *
   * @param buffer the buffer to fill with random bytes
   * @return CompletableFuture that completes when the buffer is filled
   */
  CompletableFuture<Void> getRandomBytesAsync(ByteBuffer buffer);

  // Polling and Event Operations

  /**
   * Polls for ready pollable handles.
   *
   * @param pollables the list of pollable handles to check
   * @param timeoutNanos the timeout in nanoseconds (0 for no timeout)
   * @return list of indices of ready pollables
   * @throws WasiException if polling fails
   */
  List<Integer> poll(List<Long> pollables, long timeoutNanos);

  /**
   * Creates a pollable handle for the specified resource.
   *
   * @param resourceHandle the resource handle
   * @return the pollable handle
   * @throws WasiException if pollable creation fails
   */
  long createPollable(long resourceHandle);

  // Configuration and Status

  /**
   * Gets the WASI configuration used by this context.
   *
   * @return the WASI configuration
   */
  WasiConfig getConfig();

  /**
   * Closes the context and releases all associated resources.
   *
   * <p>After closing, no further operations can be performed on this context.
   */
  @Override
  void close();

  /** Represents an HTTP response in WASI Preview 2. */
  interface WasiHttpResponse {
    /**
     * Gets the HTTP status code.
     *
     * @return the status code
     */
    int getStatusCode();

    /**
     * Gets the response headers.
     *
     * @return immutable map of header names to values
     */
    Map<String, String> getHeaders();

    /**
     * Gets the response body.
     *
     * @return the response body as a ByteBuffer
     */
    ByteBuffer getBody();
  }
}
