package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Functional interface for handling errors during HTTP request processing.
 *
 * <p>Error handlers are invoked when exceptions occur during request processing,
 * allowing applications to provide custom error responses and logging. The
 * handler receives both the original request and the exception that occurred.
 *
 * <p>Error handlers should be designed to be resilient and not throw exceptions
 * themselves, as this could lead to infinite error handling loops.
 *
 * <p>Example usage:
 * <pre>{@code
 * HttpErrorHandler customErrorHandler = (request, exception) -> {
 *     // Log the error
 *     logger.error("Error processing request: " + request.getUri(), exception);
 *
 *     // Return appropriate error response based on exception type
 *     if (exception instanceof SecurityException) {
 *         return WasiHttpResponse.builder()
 *             .statusCode(403)
 *             .reasonPhrase("Forbidden")
 *             .header("Content-Type", "application/json")
 *             .body("{\"error\":\"Access denied\"}")
 *             .build();
 *     } else if (exception instanceof IllegalArgumentException) {
 *         return WasiHttpResponse.builder()
 *             .statusCode(400)
 *             .reasonPhrase("Bad Request")
 *             .header("Content-Type", "application/json")
 *             .body("{\"error\":\"Invalid request: " + exception.getMessage() + "\"}")
 *             .build();
 *     } else {
 *         // Generic server error
 *         return WasiHttpResponse.builder()
 *             .statusCode(500)
 *             .reasonPhrase("Internal Server Error")
 *             .header("Content-Type", "application/json")
 *             .body("{\"error\":\"Internal server error\"}")
 *             .build();
 *     }
 * };
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface HttpErrorHandler {

  /**
   * Handles an error that occurred during HTTP request processing.
   *
   * <p>This method is called when an exception occurs in a route handler or
   * middleware. The implementation should return an appropriate HTTP response
   * based on the type of error and the application's error handling policy.
   *
   * <p><strong>Important:</strong> This method should not throw exceptions.
   * If an exception is thrown from this method, the server will return a
   * generic 500 Internal Server Error response.
   *
   * @param request the original HTTP request being processed
   * @param exception the exception that occurred during processing
   * @return an HTTP response representing the error
   */
  WasiHttpResponse handleError(final WasiHttpRequest request, final Throwable exception);

  /**
   * Creates a default error handler that provides standard HTTP error responses.
   *
   * <p>The default handler maps common exception types to appropriate HTTP
   * status codes and provides generic error messages.
   *
   * @return a default error handler implementation
   */
  static HttpErrorHandler createDefault() {
    return (request, exception) -> {
      // Determine status code based on exception type
      int statusCode = 500;
      String reasonPhrase = "Internal Server Error";

      if (exception instanceof IllegalArgumentException) {
        statusCode = 400;
        reasonPhrase = "Bad Request";
      } else if (exception instanceof SecurityException) {
        statusCode = 403;
        reasonPhrase = "Forbidden";
      } else if (exception instanceof UnsupportedOperationException) {
        statusCode = 501;
        reasonPhrase = "Not Implemented";
      }

      // Create basic error response
      final String errorBody = String.format("{\"error\":\"%s\",\"message\":\"%s\"}",
          reasonPhrase, sanitizeErrorMessage(exception.getMessage()));

      // Note: This would require WasiHttpResponse to have a builder
      // For now, we'll use a constructor-based approach
      return new WasiHttpResponse(statusCode, reasonPhrase,
          java.util.Map.of("Content-Type", "application/json"),
          java.nio.ByteBuffer.wrap(errorBody.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
          0);
    };
  }

  /**
   * Creates an error handler that logs errors and delegates to another handler.
   *
   * @param delegate the delegate error handler
   * @return a logging error handler
   */
  static HttpErrorHandler withLogging(final HttpErrorHandler delegate) {
    return (request, exception) -> {
      // Log the error (in a real implementation, use proper logging framework)
      System.err.println("HTTP Error processing " + request.getMethod() + " " +
                        request.getUri() + ": " + exception.getMessage());
      exception.printStackTrace(System.err);

      // Delegate to the provided handler
      return delegate.handleError(request, exception);
    };
  }

  /**
   * Creates an error handler that provides detailed error information for development.
   *
   * <p><strong>Warning:</strong> This handler includes stack traces in responses
   * and should only be used in development environments, not in production.
   *
   * @return a development-oriented error handler
   */
  static HttpErrorHandler createVerbose() {
    return (request, exception) -> {
      final StringBuilder errorDetails = new StringBuilder();
      errorDetails.append("{\n");
      errorDetails.append("  \"error\": \"").append(exception.getClass().getSimpleName()).append("\",\n");
      errorDetails.append("  \"message\": \"").append(sanitizeErrorMessage(exception.getMessage())).append("\",\n");
      errorDetails.append("  \"request\": {\n");
      errorDetails.append("    \"method\": \"").append(request.getMethod()).append("\",\n");
      errorDetails.append("    \"uri\": \"").append(request.getUri()).append("\"\n");
      errorDetails.append("  },\n");
      errorDetails.append("  \"stackTrace\": [\n");

      // Add stack trace elements
      final StackTraceElement[] stackTrace = exception.getStackTrace();
      for (int i = 0; i < Math.min(stackTrace.length, 10); i++) { // Limit to first 10 frames
        if (i > 0) {
          errorDetails.append(",\n");
        }
        errorDetails.append("    \"").append(stackTrace[i].toString()).append("\"");
      }

      errorDetails.append("\n  ]\n}");

      return new WasiHttpResponse(500, "Internal Server Error",
          java.util.Map.of("Content-Type", "application/json"),
          java.nio.ByteBuffer.wrap(errorDetails.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
          0);
    };
  }

  /**
   * Sanitizes an error message to prevent JSON injection and ensure safe output.
   *
   * @param message the error message to sanitize
   * @return the sanitized message
   */
  private static String sanitizeErrorMessage(final String message) {
    if (message == null) {
      return "Unknown error";
    }
    // Escape quotes and newlines to prevent JSON injection
    return message.replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
  }
}