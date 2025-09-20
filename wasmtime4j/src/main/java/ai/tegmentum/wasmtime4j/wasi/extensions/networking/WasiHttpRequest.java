package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an HTTP request for WASI HTTP operations.
 *
 * <p>This class encapsulates all components of an HTTP request including method, URI, headers, and
 * body. Instances are immutable and constructed using the builder pattern for flexibility.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiHttpRequest request = WasiHttpRequest.builder()
 *     .method(HttpMethod.POST)
 *     .uri("https://api.example.com/users")
 *     .header("Content-Type", "application/json")
 *     .header("Authorization", "Bearer token123")
 *     .body("{\"name\":\"John\",\"email\":\"john@example.com\"}")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiHttpRequest {

  private final HttpMethod method;
  private final URI uri;
  private final Map<String, String> headers;
  private final ByteBuffer body;
  private final int timeoutMillis;

  private WasiHttpRequest(
      final HttpMethod method,
      final URI uri,
      final Map<String, String> headers,
      final ByteBuffer body,
      final int timeoutMillis) {
    this.method = method;
    this.uri = uri;
    this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
    this.body = body != null ? body.asReadOnlyBuffer() : null;
    this.timeoutMillis = timeoutMillis;
  }

  /**
   * Gets the HTTP method for this request.
   *
   * @return the HTTP method
   */
  public HttpMethod getMethod() {
    return method;
  }

  /**
   * Gets the URI for this request.
   *
   * @return the request URI
   */
  public URI getUri() {
    return uri;
  }

  /**
   * Gets all headers for this request.
   *
   * @return an immutable map of headers
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Gets a specific header value.
   *
   * @param name the header name (case-insensitive)
   * @return the header value, or null if not present
   */
  public String getHeader(final String name) {
    if (name == null) {
      return null;
    }
    // Perform case-insensitive header lookup
    for (final Map.Entry<String, String> entry : headers.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(name)) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Checks if a header is present.
   *
   * @param name the header name (case-insensitive)
   * @return true if the header is present, false otherwise
   */
  public boolean hasHeader(final String name) {
    return getHeader(name) != null;
  }

  /**
   * Gets the request body as a ByteBuffer.
   *
   * @return the request body, or null if no body
   */
  public ByteBuffer getBody() {
    return body != null ? body.duplicate() : null;
  }

  /**
   * Gets the request body as a string using UTF-8 encoding.
   *
   * @return the request body as a string, or null if no body
   */
  public String getBodyAsString() {
    if (body == null) {
      return null;
    }
    final ByteBuffer duplicate = body.duplicate();
    final byte[] bytes = new byte[duplicate.remaining()];
    duplicate.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Gets the request body length in bytes.
   *
   * @return the body length, or 0 if no body
   */
  public int getBodyLength() {
    return body != null ? body.remaining() : 0;
  }

  /**
   * Gets the timeout for this request.
   *
   * @return the timeout in milliseconds, or 0 if no timeout
   */
  public int getTimeoutMillis() {
    return timeoutMillis;
  }

  /**
   * Checks if this request has a body.
   *
   * @return true if the request has a body, false otherwise
   */
  public boolean hasBody() {
    return body != null && body.remaining() > 0;
  }

  /**
   * Creates a new builder for constructing HTTP requests.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder initialized with the values from this request.
   *
   * @return a builder with this request's values
   */
  public Builder toBuilder() {
    final Builder builder = new Builder();
    builder.method = this.method;
    builder.uri = this.uri;
    builder.headers.putAll(this.headers);
    builder.body = this.body != null ? this.body.duplicate() : null;
    builder.timeoutMillis = this.timeoutMillis;
    return builder;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiHttpRequest that = (WasiHttpRequest) obj;
    return timeoutMillis == that.timeoutMillis
        && method == that.method
        && Objects.equals(uri, that.uri)
        && Objects.equals(headers, that.headers)
        && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, uri, headers, body, timeoutMillis);
  }

  @Override
  public String toString() {
    return "WasiHttpRequest{"
        + "method="
        + method
        + ", uri="
        + uri
        + ", headers="
        + headers.size()
        + ", hasBody="
        + hasBody()
        + ", bodyLength="
        + getBodyLength()
        + ", timeout="
        + timeoutMillis
        + '}';
  }

  /** Builder for constructing WasiHttpRequest instances. */
  public static final class Builder {
    private HttpMethod method = HttpMethod.GET;
    private URI uri;
    private final Map<String, String> headers = new HashMap<>();
    private ByteBuffer body;
    private int timeoutMillis = 0;

    private Builder() {
      // Package private constructor
    }

    /**
     * Sets the HTTP method.
     *
     * @param method the HTTP method
     * @return this builder for chaining
     * @throws IllegalArgumentException if method is null
     */
    public Builder method(final HttpMethod method) {
      if (method == null) {
        throw new IllegalArgumentException("HTTP method cannot be null");
      }
      this.method = method;
      return this;
    }

    /**
     * Sets the request URI.
     *
     * @param uri the request URI
     * @return this builder for chaining
     * @throws IllegalArgumentException if uri is null
     */
    public Builder uri(final URI uri) {
      if (uri == null) {
        throw new IllegalArgumentException("URI cannot be null");
      }
      this.uri = uri;
      return this;
    }

    /**
     * Sets the request URI from a string.
     *
     * @param uri the request URI as a string
     * @return this builder for chaining
     * @throws IllegalArgumentException if uri is null or invalid
     */
    public Builder uri(final String uri) {
      if (uri == null) {
        throw new IllegalArgumentException("URI string cannot be null");
      }
      try {
        return uri(URI.create(uri));
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid URI: " + uri, e);
      }
    }

    /**
     * Adds a header to the request.
     *
     * @param name the header name
     * @param value the header value
     * @return this builder for chaining
     * @throws IllegalArgumentException if name or value is null
     */
    public Builder header(final String name, final String value) {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Header name cannot be null or empty");
      }
      if (value == null) {
        throw new IllegalArgumentException("Header value cannot be null");
      }
      headers.put(name, value);
      return this;
    }

    /**
     * Adds multiple headers to the request.
     *
     * @param headers the headers to add
     * @return this builder for chaining
     * @throws IllegalArgumentException if headers map is null or contains null keys/values
     */
    public Builder headers(final Map<String, String> headers) {
      if (headers == null) {
        throw new IllegalArgumentException("Headers map cannot be null");
      }
      for (final Map.Entry<String, String> entry : headers.entrySet()) {
        header(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Sets the request body from a ByteBuffer.
     *
     * @param body the request body
     * @return this builder for chaining
     */
    public Builder body(final ByteBuffer body) {
      this.body = body != null ? body.duplicate() : null;
      return this;
    }

    /**
     * Sets the request body from a string using UTF-8 encoding.
     *
     * @param body the request body as a string
     * @return this builder for chaining
     */
    public Builder body(final String body) {
      if (body == null) {
        this.body = null;
      } else {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        this.body = ByteBuffer.wrap(bytes);
      }
      return this;
    }

    /**
     * Sets the request body from a byte array.
     *
     * @param body the request body as bytes
     * @return this builder for chaining
     */
    public Builder body(final byte[] body) {
      this.body = body != null ? ByteBuffer.wrap(body) : null;
      return this;
    }

    /**
     * Sets the request timeout.
     *
     * @param timeoutMillis the timeout in milliseconds (0 = no timeout)
     * @return this builder for chaining
     * @throws IllegalArgumentException if timeout is negative
     */
    public Builder timeout(final int timeoutMillis) {
      if (timeoutMillis < 0) {
        throw new IllegalArgumentException("Timeout cannot be negative: " + timeoutMillis);
      }
      this.timeoutMillis = timeoutMillis;
      return this;
    }

    /**
     * Builds the HTTP request.
     *
     * @return a new WasiHttpRequest instance
     * @throws IllegalStateException if required fields are not set
     */
    public WasiHttpRequest build() {
      if (uri == null) {
        throw new IllegalStateException("URI must be set");
      }

      // Set Content-Length header if body is present and not already set
      if (body != null && body.remaining() > 0 && !headers.containsKey("Content-Length")) {
        headers.put("Content-Length", String.valueOf(body.remaining()));
      }

      return new WasiHttpRequest(method, uri, headers, body, timeoutMillis);
    }
  }
}
