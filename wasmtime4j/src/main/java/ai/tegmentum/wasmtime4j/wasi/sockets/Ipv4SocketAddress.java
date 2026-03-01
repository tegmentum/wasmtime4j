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
package ai.tegmentum.wasmtime4j.wasi.sockets;

import java.util.Objects;

/**
 * IPv4 socket address containing port and IPv4 address.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public final class Ipv4SocketAddress {
  private final int port;
  private final Ipv4Address address;

  /**
   * Creates an IPv4 socket address.
   *
   * @param port port number (0-65535)
   * @param address IPv4 address
   * @throws IllegalArgumentException if port is out of range or address is null
   */
  public Ipv4SocketAddress(final int port, final Ipv4Address address) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be in range [0, 65535], got: " + port);
    }
    if (address == null) {
      throw new IllegalArgumentException("address cannot be null");
    }
    this.port = port;
    this.address = address;
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
   * Gets the IPv4 address.
   *
   * @return the IPv4 address
   */
  public Ipv4Address getAddress() {
    return address;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ipv4SocketAddress that = (Ipv4SocketAddress) o;
    return port == that.port && Objects.equals(address, that.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(port, address);
  }

  @Override
  public String toString() {
    return address.toString() + ":" + port;
  }
}
