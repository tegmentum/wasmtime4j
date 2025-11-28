/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * WASI Preview 2 Socket Address.
 *
 * <p>Represents an IP socket address (IP address and port number).
 *
 * <p>This class maps to the wasi:sockets/network@0.2.0 ip-socket-address type.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create an IPv4 address
 * WasiSocketAddress addr = WasiSocketAddress.ipv4(new byte[]{127, 0, 0, 1}, 8080);
 *
 * // Create from Java InetSocketAddress
 * WasiSocketAddress addr2 = WasiSocketAddress.fromInetSocketAddress(
 *     new InetSocketAddress("localhost", 8080));
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiSocketAddress {

  private final WasiAddressFamily family;
  private final byte[] address;
  private final int port;

  private WasiSocketAddress(final WasiAddressFamily family, final byte[] address, final int port) {
    this.family = family;
    this.address = address.clone();
    this.port = port;
  }

  /**
   * Creates an IPv4 socket address.
   *
   * @param address the 4-byte IPv4 address
   * @param port the port number (0-65535)
   * @return a new socket address
   * @throws IllegalArgumentException if address is not 4 bytes or port is out of range
   */
  public static WasiSocketAddress ipv4(final byte[] address, final int port) {
    if (address == null || address.length != 4) {
      throw new IllegalArgumentException("IPv4 address must be exactly 4 bytes");
    }
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 0 and 65535");
    }
    return new WasiSocketAddress(WasiAddressFamily.IPV4, address, port);
  }

  /**
   * Creates an IPv6 socket address.
   *
   * @param address the 16-byte IPv6 address
   * @param port the port number (0-65535)
   * @return a new socket address
   * @throws IllegalArgumentException if address is not 16 bytes or port is out of range
   */
  public static WasiSocketAddress ipv6(final byte[] address, final int port) {
    if (address == null || address.length != 16) {
      throw new IllegalArgumentException("IPv6 address must be exactly 16 bytes");
    }
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 0 and 65535");
    }
    return new WasiSocketAddress(WasiAddressFamily.IPV6, address, port);
  }

  /**
   * Creates a socket address from a Java InetSocketAddress.
   *
   * @param socketAddress the Java socket address
   * @return a new WASI socket address
   * @throws IllegalArgumentException if the address type is not supported
   */
  public static WasiSocketAddress fromInetSocketAddress(final InetSocketAddress socketAddress) {
    if (socketAddress == null) {
      throw new IllegalArgumentException("Socket address cannot be null");
    }
    InetAddress inetAddress = socketAddress.getAddress();
    int port = socketAddress.getPort();

    if (inetAddress instanceof Inet4Address) {
      return ipv4(inetAddress.getAddress(), port);
    } else if (inetAddress instanceof Inet6Address) {
      return ipv6(inetAddress.getAddress(), port);
    } else {
      throw new IllegalArgumentException("Unsupported address type: " + inetAddress.getClass());
    }
  }

  /**
   * Gets the address family.
   *
   * @return the address family
   */
  public WasiAddressFamily getFamily() {
    return family;
  }

  /**
   * Gets the raw address bytes.
   *
   * @return a copy of the address bytes
   */
  public byte[] getAddress() {
    return address.clone();
  }

  /**
   * Gets the port number.
   *
   * @return the port number
   */
  public int getPort() {
    return port;
  }

  /**
   * Converts this address to a Java InetSocketAddress.
   *
   * @return a Java socket address
   * @throws RuntimeException if the address cannot be converted
   */
  public InetSocketAddress toInetSocketAddress() {
    try {
      InetAddress inetAddress = InetAddress.getByAddress(address);
      return new InetSocketAddress(inetAddress, port);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Failed to convert address", e);
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
    WasiSocketAddress that = (WasiSocketAddress) obj;
    return port == that.port && family == that.family && Arrays.equals(address, that.address);
  }

  @Override
  public int hashCode() {
    int result = family.hashCode();
    result = 31 * result + Arrays.hashCode(address);
    result = 31 * result + port;
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (family == WasiAddressFamily.IPV4) {
      sb.append(address[0] & 0xFF)
          .append(".")
          .append(address[1] & 0xFF)
          .append(".")
          .append(address[2] & 0xFF)
          .append(".")
          .append(address[3] & 0xFF);
    } else {
      sb.append("[");
      for (int i = 0; i < 16; i += 2) {
        if (i > 0) {
          sb.append(":");
        }
        sb.append(String.format("%02x%02x", address[i] & 0xFF, address[i + 1] & 0xFF));
      }
      sb.append("]");
    }
    sb.append(":").append(port);
    return sb.toString();
  }
}
