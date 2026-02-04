package ai.tegmentum.wasmtime4j.wasi;

/**
 * Constants for WASI Preview 2 advanced network operations.
 *
 * <p>This interface defines protocol identifiers, message types, and stream states used by WASI
 * networking implementations including WebSocket, HTTP/2, and gRPC protocols.
 *
 * @since 1.0.0
 */
public final class WasiNetworkConstants {

  /** Private constructor to prevent instantiation. */
  private WasiNetworkConstants() {
    throw new AssertionError("Constants class should not be instantiated");
  }

  // ===== Protocol Identifiers =====

  /** WebSocket protocol identifier. */
  public static final int PROTOCOL_WEBSOCKET = 1;

  /** HTTP/2 protocol identifier. */
  public static final int PROTOCOL_HTTP2 = 2;

  /** gRPC protocol identifier. */
  public static final int PROTOCOL_GRPC = 3;

  // ===== WebSocket Message Types =====

  /** WebSocket text message type. */
  public static final int WS_MESSAGE_TEXT = 1;

  /** WebSocket binary message type. */
  public static final int WS_MESSAGE_BINARY = 2;

  /** WebSocket close message type. */
  public static final int WS_MESSAGE_CLOSE = 8;

  /** WebSocket ping message type. */
  public static final int WS_MESSAGE_PING = 9;

  /** WebSocket pong message type. */
  public static final int WS_MESSAGE_PONG = 10;

  // ===== HTTP/2 Stream States =====

  /** HTTP/2 stream idle state. */
  public static final int HTTP2_STREAM_IDLE = 0;

  /** HTTP/2 stream open state. */
  public static final int HTTP2_STREAM_OPEN = 1;

  /** HTTP/2 stream reserved (local) state. */
  public static final int HTTP2_STREAM_RESERVED_LOCAL = 2;

  /** HTTP/2 stream reserved (remote) state. */
  public static final int HTTP2_STREAM_RESERVED_REMOTE = 3;

  /** HTTP/2 stream half-closed (local) state. */
  public static final int HTTP2_STREAM_HALF_CLOSED_LOCAL = 4;

  /** HTTP/2 stream half-closed (remote) state. */
  public static final int HTTP2_STREAM_HALF_CLOSED_REMOTE = 5;

  /** HTTP/2 stream closed state. */
  public static final int HTTP2_STREAM_CLOSED = 6;

  // ===== Default Configuration Values =====

  /** Default WebSocket connection timeout in milliseconds. */
  public static final int DEFAULT_WS_TIMEOUT_MS = 30000;

  /** Default HTTP/2 max concurrent streams. */
  public static final int DEFAULT_HTTP2_MAX_CONCURRENT_STREAMS = 100;

  /** Default connection pool size. */
  public static final int DEFAULT_CONNECTION_POOL_SIZE = 10;

  /** Maximum message size (16 MB). */
  public static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
}
