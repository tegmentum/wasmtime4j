package ai.tegmentum.wasmtime4j.wasi.sockets;

/**
 * IP protocol version selection for WASI sockets.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public enum IpAddressFamily {
  /** Internet Protocol version 4 (AF_INET equivalent). */
  IPV4,

  /** Internet Protocol version 6 (AF_INET6 equivalent). */
  IPV6
}
