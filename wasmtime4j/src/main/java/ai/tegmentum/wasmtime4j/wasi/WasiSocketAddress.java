package ai.tegmentum.wasmtime4j.wasi;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Represents a socket address in the WASI environment.
 *
 * <p>A socket address combines an IP address with a port number to specify a network endpoint.
 * This class supports both IPv4 and IPv6 addresses.
 *
 * @since 1.0.0
 */
public final class WasiSocketAddress {

  /**
   * Address family types.
   */
  public enum Family {
    /** IPv4 address family. */
    INET(2),
    /** IPv6 address family. */
    INET6(10),
    /** Unix domain socket address family. */
    UNIX(1);

    private final int value;

    Family(final int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  private final Family family;
  private final String host;
  private final int port;
  private final String path; // For Unix domain sockets

  /**
   * Creates an IPv4 or IPv6 socket address.
   *
   * @param family the address family (INET or INET6)
   * @param host the host address
   * @param port the port number
   */
  public WasiSocketAddress(final Family family, final String host, final int port) {
    this.family = family;
    this.host = host;
    this.port = port;
    this.path = null;
  }

  /**
   * Creates a Unix domain socket address.
   *
   * @param path the filesystem path for the socket
   */
  public WasiSocketAddress(final String path) {
    this.family = Family.UNIX;
    this.host = null;
    this.port = 0;
    this.path = path;
  }

  /**
   * Creates an IPv4 socket address.
   *
   * @param host the IPv4 host address
   * @param port the port number
   * @return a new IPv4 socket address
   */
  public static WasiSocketAddress ipv4(final String host, final int port) {
    return new WasiSocketAddress(Family.INET, host, port);
  }

  /**
   * Creates an IPv6 socket address.
   *
   * @param host the IPv6 host address
   * @param port the port number
   * @return a new IPv6 socket address
   */
  public static WasiSocketAddress ipv6(final String host, final int port) {
    return new WasiSocketAddress(Family.INET6, host, port);
  }

  /**
   * Creates a Unix domain socket address.
   *
   * @param path the filesystem path for the socket
   * @return a new Unix domain socket address
   */
  public static WasiSocketAddress unix(final String path) {
    return new WasiSocketAddress(path);
  }

  /**
   * Creates a socket address by automatically detecting the address family.
   *
   * @param host the host address (IPv4, IPv6, or filesystem path)
   * @param port the port number (ignored for Unix domain sockets)
   * @return a new socket address with automatically detected family
   */
  public static WasiSocketAddress create(final String host, final int port) {
    if (host.startsWith("/") || host.startsWith("./")) {
      // Looks like a filesystem path
      return unix(host);
    } else if (host.contains(":")) {
      // Likely IPv6
      return ipv6(host, port);
    } else {
      // Assume IPv4
      return ipv4(host, port);
    }
  }

  /**
   * Gets the address family.
   *
   * @return the address family
   */
  public Family getFamily() {
    return family;
  }

  /**
   * Gets the host address.
   *
   * <p>For IPv4 and IPv6 addresses, this returns the IP address string. For Unix domain sockets,
   * this returns null.
   *
   * @return the host address, or null for Unix domain sockets
   */
  public String getHost() {
    return host;
  }

  /**
   * Gets the port number.
   *
   * <p>For IPv4 and IPv6 addresses, this returns the port number. For Unix domain sockets,
   * this returns 0.
   *
   * @return the port number, or 0 for Unix domain sockets
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the filesystem path.
   *
   * <p>For Unix domain sockets, this returns the filesystem path. For IPv4 and IPv6 addresses,
   * this returns null.
   *
   * @return the filesystem path, or null for network addresses
   */
  public String getPath() {
    return path;
  }

  /**
   * Checks if this is an IPv4 address.
   *
   * @return true if this is an IPv4 address, false otherwise
   */
  public boolean isIPv4() {
    return family == Family.INET;
  }

  /**
   * Checks if this is an IPv6 address.
   *
   * @return true if this is an IPv6 address, false otherwise
   */
  public boolean isIPv6() {
    return family == Family.INET6;
  }

  /**
   * Checks if this is a Unix domain socket address.
   *
   * @return true if this is a Unix domain socket address, false otherwise
   */
  public boolean isUnix() {
    return family == Family.UNIX;
  }

  /**
   * Checks if this address represents a loopback interface.
   *
   * @return true if this is a loopback address, false otherwise
   */
  public boolean isLoopback() {
    if (family == Family.UNIX) {
      return false;
    }

    try {
      final InetAddress addr = InetAddress.getByName(host);
      return addr.isLoopbackAddress();
    } catch (final UnknownHostException e) {
      return false;
    }
  }

  /**
   * Checks if this address represents a wildcard (any) interface.
   *
   * @return true if this is a wildcard address, false otherwise
   */
  public boolean isWildcard() {
    if (family == Family.UNIX) {
      return false;
    }

    try {
      final InetAddress addr = InetAddress.getByName(host);
      return addr.isAnyLocalAddress();
    } catch (final UnknownHostException e) {
      return false;
    }
  }

  /**
   * Converts this socket address to a string representation.
   *
   * <p>For IPv4 addresses, returns "host:port". For IPv6 addresses, returns "[host]:port".
   * For Unix domain sockets, returns the path.
   *
   * @return a string representation of this socket address
   */
  public String toAddressString() {
    switch (family) {
      case INET:
        return host + ":" + port;
      case INET6:
        return "[" + host + "]:" + port;
      case UNIX:
        return path;
      default:
        return toString();
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

    final WasiSocketAddress other = (WasiSocketAddress) obj;
    return family == other.family
        && port == other.port
        && (host != null ? host.equals(other.host) : other.host == null)
        && (path != null ? path.equals(other.path) : other.path == null);
  }

  @Override
  public int hashCode() {
    int result = family.hashCode();
    result = 31 * result + (host != null ? host.hashCode() : 0);
    result = 31 * result + port;
    result = 31 * result + (path != null ? path.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    switch (family) {
      case INET:
        return String.format("WasiSocketAddress{family=INET, host='%s', port=%d}", host, port);
      case INET6:
        return String.format("WasiSocketAddress{family=INET6, host='%s', port=%d}", host, port);
      case UNIX:
        return String.format("WasiSocketAddress{family=UNIX, path='%s'}", path);
      default:
        return String.format("WasiSocketAddress{family=%s}", family);
    }
  }
}