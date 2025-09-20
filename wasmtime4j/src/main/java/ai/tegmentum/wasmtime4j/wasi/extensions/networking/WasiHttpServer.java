package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * HTTP server interface for WASI networking operations.
 *
 * <p>Provides high-level HTTP server functionality including request handling,
 * routing, middleware support, and connection management. The server handles
 * HTTP protocol details automatically and provides a convenient API for
 * building web services.
 *
 * <p>The HTTP server supports both synchronous and asynchronous request
 * handling, allowing for scalable web applications.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (WasiHttpServer server = networking.createHttpServer()) {
 *     // Add route handlers
 *     server.addRoute("/api/users", HttpMethod.GET, request -> {
 *         String jsonResponse = getUsersAsJson();
 *         return WasiHttpResponse.builder()
 *             .statusCode(200)
 *             .header("Content-Type", "application/json")
 *             .body(jsonResponse)
 *             .build();
 *     });
 *
 *     // Start server
 *     server.bind(WasiSocketAddress.create("0.0.0.0", 8080));
 *     server.start();
 *
 *     // Server is now accepting requests
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiHttpServer extends AutoCloseable {

  /**
   * Binds the server to a specific address and port.
   *
   * <p>This method configures the server to listen on the specified address
   * and port. The server must be bound before it can be started.
   *
   * @param address the address and port to bind to
   * @throws WasmException if binding fails or permission is denied
   * @throws IllegalArgumentException if address is null
   * @throws IllegalStateException if server is already bound or started
   */
  void bind(final WasiSocketAddress address) throws WasmException;

  /**
   * Starts the HTTP server to accept incoming connections.
   *
   * <p>After calling this method, the server will begin accepting HTTP
   * requests on the bound address. The server must be bound before starting.
   *
   * @throws WasmException if server startup fails
   * @throws IllegalStateException if server is not bound or already started
   */
  void start() throws WasmException;

  /**
   * Stops the HTTP server and closes all active connections.
   *
   * <p>This method gracefully shuts down the server, finishing processing
   * of current requests before closing connections. New requests will be
   * rejected immediately.
   *
   * @throws WasmException if server shutdown fails
   * @throws IllegalStateException if server is not started
   */
  void stop() throws WasmException;

  /**
   * Adds a route handler for the specified path and HTTP method.
   *
   * <p>Route handlers process incoming HTTP requests that match the specified
   * path and method. The handler function receives the request and must
   * return an appropriate response.
   *
   * @param path the URL path to match (supports wildcards and parameters)
   * @param method the HTTP method to match
   * @param handler the function to handle matching requests
   * @throws IllegalArgumentException if path, method, or handler is null
   * @throws IllegalStateException if server is already started
   */
  void addRoute(final String path, final HttpMethod method,
                final Function<WasiHttpRequest, WasiHttpResponse> handler);

  /**
   * Adds a route handler that matches any HTTP method.
   *
   * @param path the URL path to match
   * @param handler the function to handle matching requests
   * @throws IllegalArgumentException if path or handler is null
   * @throws IllegalStateException if server is already started
   */
  void addRoute(final String path, final Function<WasiHttpRequest, WasiHttpResponse> handler);

  /**
   * Adds an asynchronous route handler for the specified path and method.
   *
   * <p>Asynchronous handlers return a CompletableFuture that will be completed
   * with the response. This allows for non-blocking request processing.
   *
   * @param path the URL path to match
   * @param method the HTTP method to match
   * @param handler the async function to handle matching requests
   * @throws IllegalArgumentException if path, method, or handler is null
   * @throws IllegalStateException if server is already started
   */
  void addAsyncRoute(final String path, final HttpMethod method,
                     final Function<WasiHttpRequest, CompletableFuture<WasiHttpResponse>> handler);

  /**
   * Removes a route handler for the specified path and method.
   *
   * @param path the URL path
   * @param method the HTTP method
   * @throws IllegalArgumentException if path or method is null
   * @throws IllegalStateException if server is already started
   */
  void removeRoute(final String path, final HttpMethod method);

  /**
   * Adds middleware that will be executed for all requests.
   *
   * <p>Middleware functions can inspect and modify requests before they
   * reach route handlers, or modify responses before they are sent.
   * Middleware is executed in the order it was added.
   *
   * @param middleware the middleware function
   * @throws IllegalArgumentException if middleware is null
   * @throws IllegalStateException if server is already started
   */
  void addMiddleware(final HttpMiddleware middleware);

  /**
   * Sets the default handler for requests that don't match any routes.
   *
   * <p>If no default handler is set, unmatched requests will receive
   * a 404 Not Found response.
   *
   * @param handler the default request handler
   * @throws IllegalArgumentException if handler is null
   * @throws IllegalStateException if server is already started
   */
  void setDefaultHandler(final Function<WasiHttpRequest, WasiHttpResponse> handler);

  /**
   * Sets the error handler for processing exceptions during request handling.
   *
   * <p>The error handler receives both the original request and the exception
   * that occurred, allowing for custom error responses.
   *
   * @param errorHandler the error handling function
   * @throws IllegalArgumentException if errorHandler is null
   */
  void setErrorHandler(final HttpErrorHandler errorHandler);

  /**
   * Sets the maximum number of concurrent connections.
   *
   * @param maxConnections the maximum concurrent connections (must be positive)
   * @throws IllegalArgumentException if maxConnections is not positive
   * @throws IllegalStateException if server is already started
   */
  void setMaxConnections(final int maxConnections);

  /**
   * Gets the maximum number of concurrent connections.
   *
   * @return the maximum concurrent connections
   */
  int getMaxConnections();

  /**
   * Sets the request timeout in milliseconds.
   *
   * @param timeoutMillis the timeout in milliseconds (0 = no timeout)
   * @throws IllegalArgumentException if timeout is negative
   */
  void setRequestTimeout(final int timeoutMillis);

  /**
   * Gets the request timeout in milliseconds.
   *
   * @return the timeout in milliseconds
   */
  int getRequestTimeout();

  /**
   * Gets the address the server is bound to.
   *
   * @return the server address, or null if not bound
   */
  WasiSocketAddress getServerAddress();

  /**
   * Checks if the server is currently running.
   *
   * @return true if running, false otherwise
   */
  boolean isRunning();

  /**
   * Checks if the server is bound to an address.
   *
   * @return true if bound, false otherwise
   */
  boolean isBound();

  /**
   * Gets the current number of active connections.
   *
   * @return the number of active connections
   */
  int getActiveConnections();

  /**
   * Gets HTTP server statistics and performance metrics.
   *
   * @return server statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiHttpServerStats getStats() throws WasmException;

  /**
   * Closes the HTTP server and releases all associated resources.
   *
   * <p>This method stops the server if it's running and releases all
   * resources including network connections and thread pools.
   */
  @Override
  void close();
}