package ai.tegmentum.wasmtime4j.wasi.sockets;

import java.util.Objects;

/**
 * IPv6 socket address containing port, flow info, address, and scope ID.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public final class Ipv6SocketAddress {
  private final int port;
  private final int flowInfo;
  private final Ipv6Address address;
  private final int scopeId;

  /**
   * Creates an IPv6 socket address.
   *
   * @param port port number (0-65535)
   * @param flowInfo IPv6 flow information (20-bit traffic class and flow label)
   * @param address IPv6 address
   * @param scopeId IPv6 scope identifier for link-local addresses
   * @throws IllegalArgumentException if port is out of range or address is null
   */
  public Ipv6SocketAddress(
      final int port, final int flowInfo, final Ipv6Address address, final int scopeId) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be in range [0, 65535], got: " + port);
    }
    if (address == null) {
      throw new IllegalArgumentException("address cannot be null");
    }
    this.port = port;
    this.flowInfo = flowInfo;
    this.address = address;
    this.scopeId = scopeId;
  }

  /**
   * Gets the port number.
   *
   * @return port number (0-65535)
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the flow information.
   *
   * @return IPv6 flow information
   */
  public int getFlowInfo() {
    return flowInfo;
  }

  /**
   * Gets the IPv6 address.
   *
   * @return the IPv6 address
   */
  public Ipv6Address getAddress() {
    return address;
  }

  /**
   * Gets the scope identifier.
   *
   * @return IPv6 scope identifier
   */
  public int getScopeId() {
    return scopeId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ipv6SocketAddress that = (Ipv6SocketAddress) o;
    return port == that.port
        && flowInfo == that.flowInfo
        && scopeId == that.scopeId
        && Objects.equals(address, that.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(port, flowInfo, address, scopeId);
  }

  @Override
  public String toString() {
    return "[" + address.toString() + "]:" + port;
  }
}
