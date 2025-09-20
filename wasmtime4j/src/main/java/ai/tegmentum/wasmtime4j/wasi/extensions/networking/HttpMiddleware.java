package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Functional interface for HTTP middleware components.
 *
 * <p>Middleware functions are executed in the order they are added to the server
 * and can inspect, modify, or reject requests before they reach route handlers.
 * They can also modify responses before they are sent to clients.
 *
 * <p>Middleware can be used for cross-cutting concerns such as:
 * <ul>
 *   <li>Authentication and authorization</li>
 *   <li>Request logging and metrics</li>
 *   <li>CORS handling</li>
 *   <li>Rate limiting</li>
 *   <li>Request/response transformation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Logging middleware
 * HttpMiddleware loggingMiddleware = (request, next) -> {
 *     long startTime = System.currentTimeMillis();
 *     WasiHttpResponse response = next.handle(request);
 *     long duration = System.currentTimeMillis() - startTime;
 *
 *     System.out.println(request.getMethod() + " " + request.getUri() +
 *                       " -> " + response.getStatusCode() + " (" + duration + "ms)");
 *     return response;
 * };
 *
 * // Authentication middleware
 * HttpMiddleware authMiddleware = (request, next) -> {
 *     String authHeader = request.getHeader("Authorization");
 *     if (authHeader == null || !isValidToken(authHeader)) {
 *         return WasiHttpResponse.builder()
 *             .statusCode(401)
 *             .reasonPhrase("Unauthorized")
 *             .build();
 *     }
 *     return next.handle(request);
 * };
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface HttpMiddleware {

  /**
   * Processes an HTTP request with middleware logic.
   *
   * <p>The middleware function receives the incoming request and a "next" handler
   * that represents the next middleware in the chain or the final route handler.
   * The middleware can:
   * <ul>
   *   <li>Inspect the request and call next.handle(request) to continue processing</li>
   *   <li>Modify the request and call next.handle(modifiedRequest)</li>
   *   <li>Return a response immediately without calling next (short-circuit)</li>
   *   <li>Call next.handle(request) and modify the response before returning it</li>
   * </ul>
   *
   * @param request the incoming HTTP request
   * @param next the next handler in the middleware chain
   * @return the HTTP response (either from next handler or created by this middleware)
   */
  WasiHttpResponse handle(final WasiHttpRequest request, final NextHandler next);

  /**
   * Represents the next handler in the middleware chain.
   *
   * <p>This interface allows middleware to delegate request processing to
   * the next component in the chain, which could be another middleware
   * or the final route handler.
   */
  @FunctionalInterface
  interface NextHandler {

    /**
     * Handles the request using the next component in the chain.
     *
     * @param request the request to process
     * @return the response from the next handler
     */
    WasiHttpResponse handle(final WasiHttpRequest request);
  }

  /**
   * Creates a middleware that adds a header to all responses.
   *
   * @param name the header name
   * @param value the header value
   * @return a middleware that adds the specified header
   */
  static HttpMiddleware addResponseHeader(final String name, final String value) {
    return (request, next) -> {
      final WasiHttpResponse response = next.handle(request);
      // Note: This would require WasiHttpResponse to be mutable or have a builder
      // For now, we'll document this as a utility method that implementations can use
      return response; // TODO: Add header modification capability
    };
  }

  /**
   * Creates a middleware that logs all requests and responses.
   *
   * @return a logging middleware
   */
  static HttpMiddleware createLoggingMiddleware() {
    return (request, next) -> {
      final long startTime = System.currentTimeMillis();
      final WasiHttpResponse response = next.handle(request);
      final long duration = System.currentTimeMillis() - startTime;

      System.out.println(String.format("%s %s -> %d (%dms)",
          request.getMethod(), request.getUri(), response.getStatusCode(), duration));

      return response;
    };
  }

  /**
   * Creates a middleware that adds CORS headers for cross-origin requests.
   *
   * @param allowedOrigins comma-separated list of allowed origins ("*" for all)
   * @param allowedMethods comma-separated list of allowed HTTP methods
   * @param allowedHeaders comma-separated list of allowed headers
   * @return a CORS middleware
   */
  static HttpMiddleware createCorsMiddleware(final String allowedOrigins,
                                           final String allowedMethods,
                                           final String allowedHeaders) {
    return (request, next) -> {
      // Handle preflight OPTIONS request
      if (request.getMethod() == HttpMethod.OPTIONS) {
        // Note: This would require a response builder
        // return WasiHttpResponse.builder()
        //     .statusCode(200)
        //     .header("Access-Control-Allow-Origin", allowedOrigins)
        //     .header("Access-Control-Allow-Methods", allowedMethods)
        //     .header("Access-Control-Allow-Headers", allowedHeaders)
        //     .build();
      }

      final WasiHttpResponse response = next.handle(request);
      // TODO: Add CORS headers to response
      return response;
    };
  }

  /**
   * Creates a middleware that enforces rate limiting.
   *
   * @param requestsPerMinute maximum requests per minute per client
   * @return a rate limiting middleware
   */
  static HttpMiddleware createRateLimitingMiddleware(final int requestsPerMinute) {
    // Note: This would require implementation of rate limiting logic
    // with client IP tracking and request counting
    return (request, next) -> {
      // TODO: Implement rate limiting logic
      // if (isRateLimited(clientIp)) {
      //     return WasiHttpResponse.builder()
      //         .statusCode(429)
      //         .reasonPhrase("Too Many Requests")
      //         .build();
      // }
      return next.handle(request);
    };
  }
}