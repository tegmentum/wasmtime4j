package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client interface for WASI networking operations.
 *
 * <p>Provides high-level HTTP functionality including GET, POST, PUT, DELETE and other HTTP
 * methods, with support for headers, request/response bodies, and asynchronous operations.
 *
 * <p>The HTTP client handles connection management, redirects, and other HTTP protocol details
 * automatically. It's built on top of the socket layer but provides a more convenient API for HTTP
 * communications.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiHttpClient client = networking.createHttpClient()) {
 *     WasiHttpRequest request = WasiHttpRequest.builder()
 *         .method(HttpMethod.GET)
 *         .uri("https://api.example.com/data")
 *         .header("Accept", "application/json")
 *         .build();
 *
 *     WasiHttpResponse response = client.send(request);
 *     System.out.println("Status: " + response.getStatusCode());
 *     System.out.println("Body: " + response.getBodyAsString());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiHttpClient extends AutoCloseable {

  /**
   * Sends an HTTP request synchronously and returns the response.
   *
   * <p>This method blocks until the complete response is received. Use {@link
   * #sendAsync(WasiHttpRequest)} for non-blocking operations.
   *
   * @param request the HTTP request to send
   * @return the HTTP response
   * @throws WasmException if the request fails or permission is denied
   * @throws IllegalArgumentException if request is null
   * @throws IllegalStateException if client is closed
   */
  WasiHttpResponse send(final WasiHttpRequest request) throws WasmException;

  /**
   * Sends an HTTP request asynchronously and returns a future for the response.
   *
   * <p>This method returns immediately with a CompletableFuture that will be completed when the
   * response is available. The request is processed in the background.
   *
   * @param request the HTTP request to send
   * @return a CompletableFuture that will contain the HTTP response
   * @throws IllegalArgumentException if request is null
   * @throws IllegalStateException if client is closed
   */
  CompletableFuture<WasiHttpResponse> sendAsync(final WasiHttpRequest request);

  /**
   * Convenience method for sending a GET request to the specified URI.
   *
   * @param uri the URI to send the GET request to
   * @return the HTTP response
   * @throws WasmException if the request fails
   * @throws IllegalArgumentException if uri is null
   */
  WasiHttpResponse get(final URI uri) throws WasmException;

  /**
   * Convenience method for sending a GET request with custom headers.
   *
   * @param uri the URI to send the GET request to
   * @param headers additional headers to include
   * @return the HTTP response
   * @throws WasmException if the request fails
   * @throws IllegalArgumentException if uri is null
   */
  WasiHttpResponse get(final URI uri, final Map<String, String> headers) throws WasmException;

  /**
   * Convenience method for sending a POST request with a body.
   *
   * @param uri the URI to send the POST request to
   * @param body the request body
   * @param contentType the content type of the body
   * @return the HTTP response
   * @throws WasmException if the request fails
   * @throws IllegalArgumentException if uri or contentType is null
   */
  WasiHttpResponse post(final URI uri, final ByteBuffer body, final String contentType)
      throws WasmException;

  /**
   * Convenience method for sending a POST request with string body.
   *
   * @param uri the URI to send the POST request to
   * @param body the request body as a string
   * @param contentType the content type of the body
   * @return the HTTP response
   * @throws WasmException if the request fails
   * @throws IllegalArgumentException if uri or contentType is null
   */
  WasiHttpResponse post(final URI uri, final String body, final String contentType)
      throws WasmException;

  /**
   * Convenience method for sending a PUT request with a body.
   *
   * @param uri the URI to send the PUT request to
   * @param body the request body
   * @param contentType the content type of the body
   * @return the HTTP response
   * @throws WasmException if the request fails
   * @throws IllegalArgumentException if uri or contentType is null
   */
  WasiHttpResponse put(final URI uri, final ByteBuffer body, final String contentType)
      throws WasmException;

  /**
   * Convenience method for sending a DELETE request.
   *
   * @param uri the URI to send the DELETE request to
   * @return the HTTP response
   * @throws WasmException if the request fails
   * @throws IllegalArgumentException if uri is null
   */
  WasiHttpResponse delete(final URI uri) throws WasmException;

  /**
   * Sets the default timeout for HTTP requests.
   *
   * @param timeoutMillis the timeout in milliseconds (0 = no timeout)
   * @throws IllegalArgumentException if timeout is negative
   * @throws IllegalStateException if client is closed
   */
  void setTimeout(final int timeoutMillis);

  /**
   * Gets the current default timeout for HTTP requests.
   *
   * @return the timeout in milliseconds (0 = no timeout)
   */
  int getTimeout();

  /**
   * Sets whether to follow HTTP redirects automatically.
   *
   * @param followRedirects true to follow redirects, false otherwise
   * @throws IllegalStateException if client is closed
   */
  void setFollowRedirects(final boolean followRedirects);

  /**
   * Gets whether HTTP redirects are followed automatically.
   *
   * @return true if redirects are followed, false otherwise
   */
  boolean isFollowRedirects();

  /**
   * Sets the maximum number of redirects to follow.
   *
   * @param maxRedirects the maximum redirects (0 = no redirects)
   * @throws IllegalArgumentException if maxRedirects is negative
   * @throws IllegalStateException if client is closed
   */
  void setMaxRedirects(final int maxRedirects);

  /**
   * Gets the maximum number of redirects to follow.
   *
   * @return the maximum redirects
   */
  int getMaxRedirects();

  /**
   * Gets HTTP client statistics and performance metrics.
   *
   * @return client statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiHttpClientStats getStats() throws WasmException;

  /**
   * Checks if the HTTP client is closed.
   *
   * @return true if closed, false otherwise
   */
  boolean isClosed();

  /**
   * Closes the HTTP client and releases all associated resources.
   *
   * <p>This includes closing any persistent connections and canceling any pending asynchronous
   * requests. After calling this method, the client becomes unusable.
   */
  @Override
  void close();
}
