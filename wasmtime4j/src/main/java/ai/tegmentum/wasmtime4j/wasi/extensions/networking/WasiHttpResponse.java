package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an HTTP response from WASI HTTP operations.
 *
 * <p>This class encapsulates all components of an HTTP response including
 * status code, headers, and body. Instances are immutable and typically
 * created by HTTP client implementations.
 *
 * <p>Example usage:
 * <pre>{@code
 * WasiHttpResponse response = httpClient.get(URI.create("https://api.example.com/users"));
 *
 * if (response.isSuccessful()) {
 *     String jsonData = response.getBodyAsString();
 *     // Process the response...
 * } else {
 *     System.err.println("Request failed: " + response.getStatusCode() + " " + response.getReasonPhrase());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiHttpResponse {

  private final int statusCode;
  private final String reasonPhrase;
  private final Map<String, String> headers;
  private final ByteBuffer body;
  private final long responseTime;
  private final long contentLength;

  /**
   * Creates a new HTTP response.
   *
   * @param statusCode the HTTP status code
   * @param reasonPhrase the reason phrase for the status code
   * @param headers the response headers
   * @param body the response body
   * @param responseTime the time taken to receive the response in milliseconds
   */
  public WasiHttpResponse(final int statusCode, final String reasonPhrase,
                         final Map<String, String> headers, final ByteBuffer body,
                         final long responseTime) {
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase != null ? reasonPhrase : getDefaultReasonPhrase(statusCode);
    this.headers = Collections.unmodifiableMap(new HashMap<>(headers != null ? headers : new HashMap<>()));
    this.body = body != null ? body.asReadOnlyBuffer() : null;
    this.responseTime = responseTime;
    this.contentLength = body != null ? body.remaining() : 0;
  }

  /**
   * Gets the HTTP status code.
   *
   * @return the status code (e.g., 200, 404, 500)
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Gets the reason phrase for the status code.
   *
   * @return the reason phrase (e.g., "OK", "Not Found", "Internal Server Error")
   */
  public String getReasonPhrase() {
    return reasonPhrase;
  }

  /**
   * Gets all response headers.
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
   * Gets the response body as a ByteBuffer.
   *
   * @return the response body, or null if no body
   */
  public ByteBuffer getBody() {
    return body != null ? body.duplicate() : null;
  }

  /**
   * Gets the response body as a string using UTF-8 encoding.
   *
   * @return the response body as a string, or null if no body
   */
  public String getBodyAsString() {
    if (body == null || body.remaining() == 0) {
      return null;
    }
    final ByteBuffer duplicate = body.duplicate();
    final byte[] bytes = new byte[duplicate.remaining()];
    duplicate.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Gets the response body as a byte array.
   *
   * @return the response body as bytes, or null if no body
   */
  public byte[] getBodyAsBytes() {
    if (body == null || body.remaining() == 0) {
      return null;
    }
    final ByteBuffer duplicate = body.duplicate();
    final byte[] bytes = new byte[duplicate.remaining()];
    duplicate.get(bytes);
    return bytes;
  }

  /**
   * Gets the content length of the response body.
   *
   * @return the content length in bytes
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * Gets the time taken to receive this response.
   *
   * @return the response time in milliseconds
   */
  public long getResponseTime() {
    return responseTime;
  }

  /**
   * Checks if this response has a body.
   *
   * @return true if the response has a body, false otherwise
   */
  public boolean hasBody() {
    return body != null && body.remaining() > 0;
  }

  /**
   * Checks if the response indicates success (status code 200-299).
   *
   * @return true if successful, false otherwise
   */
  public boolean isSuccessful() {
    return statusCode >= 200 && statusCode < 300;
  }

  /**
   * Checks if the response indicates a redirect (status code 300-399).
   *
   * @return true if redirect, false otherwise
   */
  public boolean isRedirect() {
    return statusCode >= 300 && statusCode < 400;
  }

  /**
   * Checks if the response indicates a client error (status code 400-499).
   *
   * @return true if client error, false otherwise
   */
  public boolean isClientError() {
    return statusCode >= 400 && statusCode < 500;
  }

  /**
   * Checks if the response indicates a server error (status code 500-599).
   *
   * @return true if server error, false otherwise
   */
  public boolean isServerError() {
    return statusCode >= 500 && statusCode < 600;
  }

  /**
   * Checks if the response indicates an error (status code 400+).
   *
   * @return true if error, false otherwise
   */
  public boolean isError() {
    return statusCode >= 400;
  }

  /**
   * Gets the Content-Type header value.
   *
   * @return the content type, or null if not present
   */
  public String getContentType() {
    return getHeader("Content-Type");
  }

  /**
   * Checks if the response content type matches the specified type.
   *
   * @param expectedType the expected content type (case-insensitive)
   * @return true if the content type matches, false otherwise
   */
  public boolean isContentType(final String expectedType) {
    final String contentType = getContentType();
    if (contentType == null || expectedType == null) {
      return false;
    }
    // Compare the main type, ignoring parameters like charset
    final String mainType = contentType.split(";")[0].trim();
    return mainType.equalsIgnoreCase(expectedType);
  }

  /**
   * Checks if the response is JSON content.
   *
   * @return true if content type is application/json, false otherwise
   */
  public boolean isJson() {
    return isContentType("application/json");
  }

  /**
   * Checks if the response is XML content.
   *
   * @return true if content type is application/xml or text/xml, false otherwise
   */
  public boolean isXml() {
    return isContentType("application/xml") || isContentType("text/xml");
  }

  /**
   * Checks if the response is HTML content.
   *
   * @return true if content type is text/html, false otherwise
   */
  public boolean isHtml() {
    return isContentType("text/html");
  }

  /**
   * Checks if the response is plain text content.
   *
   * @return true if content type is text/plain, false otherwise
   */
  public boolean isText() {
    return isContentType("text/plain");
  }

  private static String getDefaultReasonPhrase(final int statusCode) {
    switch (statusCode) {
      case 200: return "OK";
      case 201: return "Created";
      case 202: return "Accepted";
      case 204: return "No Content";
      case 301: return "Moved Permanently";
      case 302: return "Found";
      case 304: return "Not Modified";
      case 400: return "Bad Request";
      case 401: return "Unauthorized";
      case 403: return "Forbidden";
      case 404: return "Not Found";
      case 405: return "Method Not Allowed";
      case 409: return "Conflict";
      case 410: return "Gone";
      case 412: return "Precondition Failed";
      case 422: return "Unprocessable Entity";
      case 429: return "Too Many Requests";
      case 500: return "Internal Server Error";
      case 501: return "Not Implemented";
      case 502: return "Bad Gateway";
      case 503: return "Service Unavailable";
      case 504: return "Gateway Timeout";
      default: return "Unknown Status";
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiHttpResponse that = (WasiHttpResponse) obj;
    return statusCode == that.statusCode &&
           responseTime == that.responseTime &&
           contentLength == that.contentLength &&
           Objects.equals(reasonPhrase, that.reasonPhrase) &&
           Objects.equals(headers, that.headers) &&
           Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statusCode, reasonPhrase, headers, body, responseTime, contentLength);
  }

  @Override
  public String toString() {
    return "WasiHttpResponse{" +
           "statusCode=" + statusCode +
           ", reasonPhrase='" + reasonPhrase + '\'' +
           ", headers=" + headers.size() +
           ", hasBody=" + hasBody() +
           ", contentLength=" + contentLength +
           ", responseTime=" + responseTime + "ms" +
           '}';
  }
}