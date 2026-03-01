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
 * IP socket address variant holding either IPv4 or IPv6 socket address.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public final class IpSocketAddress {
  private final Ipv4SocketAddress ipv4;
  private final Ipv6SocketAddress ipv6;

  private IpSocketAddress(final Ipv4SocketAddress ipv4, final Ipv6SocketAddress ipv6) {
    this.ipv4 = ipv4;
    this.ipv6 = ipv6;
  }

  /**
   * Creates an IPv4 socket address variant.
   *
   * @param ipv4 the IPv4 socket address
   * @return IP socket address containing IPv4
   * @throws IllegalArgumentException if ipv4 is null
   */
  public static IpSocketAddress ipv4(final Ipv4SocketAddress ipv4) {
    if (ipv4 == null) {
      throw new IllegalArgumentException("ipv4 cannot be null");
    }
    return new IpSocketAddress(ipv4, null);
  }

  /**
   * Creates an IPv6 socket address variant.
   *
   * @param ipv6 the IPv6 socket address
   * @return IP socket address containing IPv6
   * @throws IllegalArgumentException if ipv6 is null
   */
  public static IpSocketAddress ipv6(final Ipv6SocketAddress ipv6) {
    if (ipv6 == null) {
      throw new IllegalArgumentException("ipv6 cannot be null");
    }
    return new IpSocketAddress(null, ipv6);
  }

  /**
   * Checks if this is an IPv4 socket address.
   *
   * @return true if IPv4, false if IPv6
   */
  public boolean isIpv4() {
    return ipv4 != null;
  }

  /**
   * Gets the IPv4 socket address.
   *
   * @return the IPv4 socket address
   * @throws IllegalStateException if this is not an IPv4 socket address
   */
  public Ipv4SocketAddress getIpv4() {
    if (ipv4 == null) {
      throw new IllegalStateException("Not an IPv4 socket address");
    }
    return ipv4;
  }

  /**
   * Gets the IPv6 socket address.
   *
   * @return the IPv6 socket address
   * @throws IllegalStateException if this is not an IPv6 socket address
   */
  public Ipv6SocketAddress getIpv6() {
    if (ipv6 == null) {
      throw new IllegalStateException("Not an IPv6 socket address");
    }
    return ipv6;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IpSocketAddress that = (IpSocketAddress) o;
    return Objects.equals(ipv4, that.ipv4) && Objects.equals(ipv6, that.ipv6);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ipv4, ipv6);
  }

  @Override
  public String toString() {
    return ipv4 != null ? ipv4.toString() : ipv6.toString();
  }
}
