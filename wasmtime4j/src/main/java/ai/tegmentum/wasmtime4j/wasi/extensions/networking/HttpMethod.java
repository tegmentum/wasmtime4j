package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Enumeration of HTTP methods for WASI HTTP operations.
 *
 * <p>Represents the standard HTTP methods as defined in RFC 7231
 * and other relevant HTTP specifications. Each method has specific
 * semantics for how it should be handled by HTTP clients and servers.
 *
 * @since 1.0.0
 */
public enum HttpMethod {

  /**
   * GET method - retrieve information identified by the URI.
   * Safe and idempotent method that should not have side effects.
   */
  GET("GET"),

  /**
   * POST method - submit data to be processed by the resource.
   * Not safe or idempotent, may have side effects.
   */
  POST("POST"),

  /**
   * PUT method - replace the target resource with the request payload.
   * Not safe but idempotent.
   */
  PUT("PUT"),

  /**
   * DELETE method - remove the target resource.
   * Not safe but idempotent.
   */
  DELETE("DELETE"),

  /**
   * HEAD method - identical to GET except returns only headers.
   * Safe and idempotent method.
   */
  HEAD("HEAD"),

  /**
   * OPTIONS method - describe communication options for the target resource.
   * Safe and idempotent method.
   */
  OPTIONS("OPTIONS"),

  /**
   * PATCH method - apply partial modifications to a resource.
   * Not safe or idempotent.
   */
  PATCH("PATCH"),

  /**
   * TRACE method - perform a message loop-back test.
   * Safe and idempotent method.
   */
  TRACE("TRACE"),

  /**
   * CONNECT method - establish a tunnel to the server.
   * Special method for proxy connections.
   */
  CONNECT("CONNECT");

  private final String name;

  HttpMethod(final String name) {
    this.name = name;
  }

  /**
   * Gets the string representation of this HTTP method.
   *
   * @return the method name (e.g., "GET", "POST")
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if this method is considered safe according to HTTP semantics.
   *
   * <p>Safe methods are those that do not modify resources on the server
   * and should not have side effects beyond retrieval.
   *
   * @return true if the method is safe, false otherwise
   */
  public boolean isSafe() {
    return this == GET || this == HEAD || this == OPTIONS || this == TRACE;
  }

  /**
   * Checks if this method is considered idempotent according to HTTP semantics.
   *
   * <p>Idempotent methods are those that can be repeated multiple times
   * with the same effect as a single request.
   *
   * @return true if the method is idempotent, false otherwise
   */
  public boolean isIdempotent() {
    return this != POST && this != PATCH;
  }

  /**
   * Checks if this method typically includes a request body.
   *
   * @return true if the method commonly has a body, false otherwise
   */
  public boolean hasBody() {
    return this == POST || this == PUT || this == PATCH;
  }

  /**
   * Creates an HttpMethod from its string representation.
   *
   * @param name the method name (case-sensitive)
   * @return the corresponding HttpMethod
   * @throws IllegalArgumentException if the method name is not recognized
   */
  public static HttpMethod fromString(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("HTTP method name cannot be null");
    }

    for (final HttpMethod method : values()) {
      if (method.name.equals(name)) {
        return method;
      }
    }
    throw new IllegalArgumentException("Unknown HTTP method: " + name);
  }

  @Override
  public String toString() {
    return name;
  }
}