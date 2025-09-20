package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Represents a socket address for WASI networking operations.
 *
 * <p>A socket address consists of an address (IP address or filesystem path) and optional port
 * number, depending on the address family. This class provides factory methods for creating
 * addresses for different socket families.
 *
 * <p>Instances of this class are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WasiSocketAddress {

  private final SocketFamily family;
  private final String address;
  private final int port;
  private final String path;

  private WasiSocketAddress(
      final SocketFamily family, final String address, final int port, final String path) {
    this.family = family;
    this.address = address;
    this.port = port;
    this.path = path;
  }

  /**
   * Creates an IPv4 socket address with the specified IP address and port.
   *
   * @param address the IPv4 address (e.g., "192.168.1.1" or "localhost")
   * @param port the port number (1-65535)
   * @return a new WasiSocketAddress for IPv4
   * @throws IllegalArgumentException if address is null/empty or port is invalid
   */
  public static WasiSocketAddress createInet(final String address, final int port) {
    if (address == null || address.trim().isEmpty()) {
      throw new IllegalArgumentException("IPv4 address cannot be null or empty");
    }
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
    }

    // Validate that it's a valid IPv4 address
    try {
      final InetAddress inetAddr = InetAddress.getByName(address);
      if (inetAddr.getAddress().length != 4) {
        throw new IllegalArgumentException("Address is not a valid IPv4 address: " + address);
      }
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IPv4 address: " + address, e);
    }

    return new WasiSocketAddress(SocketFamily.INET, address, port, null);
  }

  /**
   * Creates an IPv6 socket address with the specified IP address and port.
   *
   * @param address the IPv6 address (e.g., "2001:db8::1" or "::1")
   * @param port the port number (1-65535)
   * @return a new WasiSocketAddress for IPv6
   * @throws IllegalArgumentException if address is null/empty or port is invalid
   */
  public static WasiSocketAddress createInet6(final String address, final int port) {
    if (address == null || address.trim().isEmpty()) {
      throw new IllegalArgumentException("IPv6 address cannot be null or empty");
    }
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
    }

    // Validate that it's a valid IPv6 address
    try {
      final InetAddress inetAddr = InetAddress.getByName(address);
      if (inetAddr.getAddress().length != 16) {
        throw new IllegalArgumentException("Address is not a valid IPv6 address: " + address);
      }
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IPv6 address: " + address, e);
    }

    return new WasiSocketAddress(SocketFamily.INET6, address, port, null);
  }

  /**
   * Creates a Unix domain socket address with the specified filesystem path.
   *
   * @param path the filesystem path for the Unix socket
   * @return a new WasiSocketAddress for Unix domain sockets
   * @throws IllegalArgumentException if path is null or empty
   */
  public static WasiSocketAddress createUnix(final String path) {
    if (path == null || path.trim().isEmpty()) {
      throw new IllegalArgumentException("Unix socket path cannot be null or empty");
    }
    if (path.length() > 108) { // Standard Unix socket path limit
      throw new IllegalArgumentException(
          "Unix socket path too long (max 108 characters): " + path.length());
    }

    return new WasiSocketAddress(SocketFamily.UNIX, null, 0, path);
  }

  /**
   * Convenience method to create an IPv4 address (automatically detects family).
   *
   * @param address the IP address string
   * @param port the port number
   * @return a new WasiSocketAddress
   * @throws IllegalArgumentException if address is invalid or port is out of range
   */
  public static WasiSocketAddress create(final String address, final int port) {
    if (address == null || address.trim().isEmpty()) {
      throw new IllegalArgumentException("Address cannot be null or empty");
    }

    // Try to determine if it's IPv4 or IPv6
    try {
      final InetAddress inetAddr = InetAddress.getByName(address);
      if (inetAddr.getAddress().length == 4) {
        return createInet(address, port);
      } else if (inetAddr.getAddress().length == 16) {
        return createInet6(address, port);
      } else {
        throw new IllegalArgumentException("Unknown address format: " + address);
      }
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Invalid address: " + address, e);
    }
  }

  /**
   * Gets the socket family of this address.
   *
   * @return the socket family
   */
  public SocketFamily getFamily() {
    return family;
  }

  /**
   * Gets the IP address string (for INET/INET6 families).
   *
   * @return the IP address string, or null for Unix sockets
   */
  public String getAddress() {
    return address;
  }

  /**
   * Gets the port number (for INET/INET6 families).
   *
   * @return the port number, or 0 for Unix sockets
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the filesystem path (for UNIX family).
   *
   * @return the filesystem path, or null for IP sockets
   */
  public String getPath() {
    return path;
  }

  /**
   * Checks if this is an IP-based socket address (IPv4 or IPv6).
   *
   * @return true if this is an IP address, false for Unix sockets
   */
  public boolean isIpAddress() {
    return family == SocketFamily.INET || family == SocketFamily.INET6;
  }

  /**
   * Checks if this is a Unix domain socket address.
   *
   * @return true if this is a Unix socket, false for IP addresses
   */
  public boolean isUnixSocket() {
    return family == SocketFamily.UNIX;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiSocketAddress that = (WasiSocketAddress) obj;
    return port == that.port
        && family == that.family
        && Objects.equals(address, that.address)
        && Objects.equals(path, that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(family, address, port, path);
  }

  @Override
  public String toString() {
    switch (family) {
      case INET:
        return address + ":" + port;
      case INET6:
        return "[" + address + "]:" + port;
      case UNIX:
        return "unix:" + path;
      default:
        return "unknown:" + family;
    }
  }
}
