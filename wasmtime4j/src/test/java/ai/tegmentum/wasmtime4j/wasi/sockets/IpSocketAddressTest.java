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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link IpSocketAddress} tagged union class.
 *
 * <p>Verifies IPv4 and IPv6 socket address factories, type checking, accessors, error behavior
 * on wrong variant access, and equals/hashCode/toString.
 */
@DisplayName("IpSocketAddress Tests")
class IpSocketAddressTest {

  @Nested
  @DisplayName("IPv4 Socket Variant Tests")
  class Ipv4SocketVariantTests {

    @Test
    @DisplayName("ipv4() should create IPv4 socket address variant")
    void ipv4ShouldCreateIpv4Variant() {
      final Ipv4Address addr = new Ipv4Address(new byte[]{127, 0, 0, 1});
      final Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress ip = IpSocketAddress.ipv4(sockAddr);
      assertTrue(ip.isIpv4(), "Should be an IPv4 socket address");
    }

    @Test
    @DisplayName("getIpv4() should return the IPv4 socket address")
    void getIpv4ShouldReturnSocketAddress() {
      final Ipv4Address addr = new Ipv4Address(new byte[]{10, 0, 0, 1});
      final Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(443, addr);
      final IpSocketAddress ip = IpSocketAddress.ipv4(sockAddr);
      assertEquals(sockAddr, ip.getIpv4(), "getIpv4() should return the stored socket address");
    }

    @Test
    @DisplayName("getIpv6() should throw for IPv4 variant")
    void getIpv6ShouldThrowForIpv4Variant() {
      final Ipv4Address addr =
          new Ipv4Address(new byte[]{(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(80, addr);
      final IpSocketAddress ip = IpSocketAddress.ipv4(sockAddr);
      assertThrows(
          IllegalStateException.class,
          ip::getIpv6,
          "getIpv6() should throw for IPv4 variant");
    }

    @Test
    @DisplayName("ipv4() should throw for null")
    void ipv4ShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> IpSocketAddress.ipv4(null),
          "ipv4() should throw for null");
    }
  }

  @Nested
  @DisplayName("IPv6 Socket Variant Tests")
  class Ipv6SocketVariantTests {

    @Test
    @DisplayName("ipv6() should create IPv6 socket address variant")
    void ipv6ShouldCreateIpv6Variant() {
      final Ipv6Address addr = new Ipv6Address(new short[]{0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(8080, 0, addr, 0);
      final IpSocketAddress ip = IpSocketAddress.ipv6(sockAddr);
      assertFalse(ip.isIpv4(), "Should not be IPv4");
    }

    @Test
    @DisplayName("getIpv6() should return the IPv6 socket address")
    void getIpv6ShouldReturnSocketAddress() {
      final Ipv6Address addr = new Ipv6Address(new short[]{0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(443, 0, addr, 0);
      final IpSocketAddress ip = IpSocketAddress.ipv6(sockAddr);
      assertEquals(sockAddr, ip.getIpv6(), "getIpv6() should return the stored socket address");
    }

    @Test
    @DisplayName("getIpv4() should throw for IPv6 variant")
    void getIpv4ShouldThrowForIpv6Variant() {
      final Ipv6Address addr = new Ipv6Address(new short[]{0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(80, 0, addr, 0);
      final IpSocketAddress ip = IpSocketAddress.ipv6(sockAddr);
      assertThrows(
          IllegalStateException.class,
          ip::getIpv4,
          "getIpv4() should throw for IPv6 variant");
    }

    @Test
    @DisplayName("ipv6() should throw for null")
    void ipv6ShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> IpSocketAddress.ipv6(null),
          "ipv6() should throw for null");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal IPv4 socket addresses should be equal")
    void equalIpv4SocketAddressesShouldBeEqual() {
      final Ipv4Address addr = new Ipv4Address(new byte[]{10, 0, 0, 1});
      final IpSocketAddress ip1 =
          IpSocketAddress.ipv4(new Ipv4SocketAddress(80, addr));
      final IpSocketAddress ip2 =
          IpSocketAddress.ipv4(new Ipv4SocketAddress(80, addr));
      assertEquals(ip1, ip2, "Same IPv4 socket addresses should be equal");
      assertEquals(
          ip1.hashCode(), ip2.hashCode(),
          "Same IPv4 socket addresses should have same hashCode");
    }

    @Test
    @DisplayName("different ports should not be equal")
    void differentPortsShouldNotBeEqual() {
      final Ipv4Address addr = new Ipv4Address(new byte[]{10, 0, 0, 1});
      final IpSocketAddress ip1 =
          IpSocketAddress.ipv4(new Ipv4SocketAddress(80, addr));
      final IpSocketAddress ip2 =
          IpSocketAddress.ipv4(new Ipv4SocketAddress(443, addr));
      assertNotEquals(ip1, ip2, "Different ports should not be equal");
    }

    @Test
    @DisplayName("IPv4 and IPv6 variants should not be equal")
    void ipv4AndIpv6VariantsShouldNotBeEqual() {
      final IpSocketAddress ipv4 =
          IpSocketAddress.ipv4(
              new Ipv4SocketAddress(80, new Ipv4Address(new byte[]{0, 0, 0, 1})));
      final IpSocketAddress ipv6 =
          IpSocketAddress.ipv6(
              new Ipv6SocketAddress(
                  80, 0, new Ipv6Address(new short[]{0, 0, 0, 0, 0, 0, 0, 1}), 0));
      assertNotEquals(ipv4, ipv6, "IPv4 and IPv6 variants should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final IpSocketAddress ip =
          IpSocketAddress.ipv4(
              new Ipv4SocketAddress(80, new Ipv4Address(new byte[]{127, 0, 0, 1})));
      assertNotEquals(null, ip, "IpSocketAddress should not equal null");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("IPv4 variant toString should delegate to Ipv4SocketAddress")
    void ipv4ToStringShouldDelegate() {
      final IpSocketAddress ip =
          IpSocketAddress.ipv4(
              new Ipv4SocketAddress(
                  8080, new Ipv4Address(new byte[]{(byte) 192, (byte) 168, 1, 1})));
      assertEquals(
          "192.168.1.1:8080", ip.toString(),
          "IPv4 toString should show addr:port");
    }

    @Test
    @DisplayName("IPv6 variant toString should delegate to Ipv6SocketAddress")
    void ipv6ToStringShouldDelegate() {
      final IpSocketAddress ip =
          IpSocketAddress.ipv6(
              new Ipv6SocketAddress(
                  443, 0, new Ipv6Address(new short[]{0, 0, 0, 0, 0, 0, 0, 1}), 0));
      final String result = ip.toString();
      assertTrue(
          result.contains("443"),
          "IPv6 toString should contain port: " + result);
      assertTrue(
          result.startsWith("["),
          "IPv6 toString should start with bracket: " + result);
    }
  }
}
