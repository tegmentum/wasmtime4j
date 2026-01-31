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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasiSocketAddress} socket address handling. */
@DisplayName("WasiSocketAddress")
final class WasiSocketAddressTest {

  @Nested
  @DisplayName("IPv4 address creation")
  final class Ipv4CreationTests {

    @Test
    @DisplayName("should create valid IPv4 address")
    void shouldCreateValidIpv4Address() {
      final byte[] addr = new byte[] {127, 0, 0, 1};
      final WasiSocketAddress socketAddr = WasiSocketAddress.ipv4(addr, 8080);
      assertEquals(WasiAddressFamily.IPV4, socketAddr.getFamily(), "Family should be IPV4");
      assertEquals(8080, socketAddr.getPort(), "Port should be 8080");
      assertArrayEquals(addr, socketAddr.getAddress(), "Address bytes should match");
    }

    @Test
    @DisplayName("should reject null IPv4 address")
    void shouldRejectNullIpv4Address() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiSocketAddress.ipv4(null, 80),
              "Expected IllegalArgumentException for null address");
      assertTrue(
          exception.getMessage().contains("4 bytes"),
          "Exception message should mention 4 bytes requirement");
    }

    @Test
    @DisplayName("should reject wrong-size IPv4 address")
    void shouldRejectWrongSizeIpv4Address() {
      final byte[] wrongSize = new byte[] {1, 2, 3};
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSocketAddress.ipv4(wrongSize, 80),
          "Expected IllegalArgumentException for 3-byte address");
    }

    @Test
    @DisplayName("should reject negative port for IPv4")
    void shouldRejectNegativePort() {
      final byte[] addr = new byte[] {127, 0, 0, 1};
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSocketAddress.ipv4(addr, -1),
          "Expected IllegalArgumentException for negative port");
    }

    @Test
    @DisplayName("should reject port exceeding 65535 for IPv4")
    void shouldRejectPortExceeding65535() {
      final byte[] addr = new byte[] {127, 0, 0, 1};
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSocketAddress.ipv4(addr, 65536),
          "Expected IllegalArgumentException for port > 65535");
    }

    @Test
    @DisplayName("should accept boundary port values for IPv4")
    void shouldAcceptBoundaryPortValues() {
      final byte[] addr = new byte[] {0, 0, 0, 0};
      final WasiSocketAddress minPort = WasiSocketAddress.ipv4(addr, 0);
      assertEquals(0, minPort.getPort(), "Should accept port 0");
      final WasiSocketAddress maxPort = WasiSocketAddress.ipv4(addr, 65535);
      assertEquals(65535, maxPort.getPort(), "Should accept port 65535");
    }
  }

  @Nested
  @DisplayName("IPv6 address creation")
  final class Ipv6CreationTests {

    @Test
    @DisplayName("should create valid IPv6 address")
    void shouldCreateValidIpv6Address() {
      final byte[] addr = new byte[16];
      addr[15] = 1; // ::1 (loopback)
      final WasiSocketAddress socketAddr = WasiSocketAddress.ipv6(addr, 443);
      assertEquals(WasiAddressFamily.IPV6, socketAddr.getFamily(), "Family should be IPV6");
      assertEquals(443, socketAddr.getPort(), "Port should be 443");
      assertEquals(16, socketAddr.getAddress().length, "Address should be 16 bytes");
    }

    @Test
    @DisplayName("should reject null IPv6 address")
    void shouldRejectNullIpv6Address() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSocketAddress.ipv6(null, 80),
          "Expected IllegalArgumentException for null IPv6 address");
    }

    @Test
    @DisplayName("should reject wrong-size IPv6 address")
    void shouldRejectWrongSizeIpv6Address() {
      final byte[] wrongSize = new byte[4];
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSocketAddress.ipv6(wrongSize, 80),
          "Expected IllegalArgumentException for 4-byte IPv6 address");
    }
  }

  @Nested
  @DisplayName("fromInetSocketAddress conversion")
  final class FromInetSocketAddressTests {

    @Test
    @DisplayName("should convert IPv4 InetSocketAddress")
    void shouldConvertIpv4InetSocketAddress() {
      final InetSocketAddress inetAddr = new InetSocketAddress("127.0.0.1", 8080);
      final WasiSocketAddress wasiAddr = WasiSocketAddress.fromInetSocketAddress(inetAddr);
      assertEquals(WasiAddressFamily.IPV4, wasiAddr.getFamily(), "Family should be IPV4");
      assertEquals(8080, wasiAddr.getPort(), "Port should be 8080");
    }

    @Test
    @DisplayName("should reject null InetSocketAddress")
    void shouldRejectNullInetSocketAddress() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSocketAddress.fromInetSocketAddress(null),
          "Expected IllegalArgumentException for null InetSocketAddress");
    }
  }

  @Nested
  @DisplayName("toInetSocketAddress conversion")
  final class ToInetSocketAddressTests {

    @Test
    @DisplayName("should convert IPv4 to InetSocketAddress")
    void shouldConvertIpv4ToInetSocketAddress() {
      final WasiSocketAddress wasiAddr =
          WasiSocketAddress.ipv4(new byte[] {127, 0, 0, 1}, 9090);
      final InetSocketAddress inetAddr = wasiAddr.toInetSocketAddress();
      assertNotNull(inetAddr, "Converted InetSocketAddress should not be null");
      assertEquals(9090, inetAddr.getPort(), "Port should match after conversion");
    }
  }

  @Nested
  @DisplayName("defensive copy of address bytes")
  final class DefensiveCopyTests {

    @Test
    @DisplayName("should return defensive copy of address bytes")
    void shouldReturnDefensiveCopy() {
      final byte[] original = new byte[] {10, 0, 0, 1};
      final WasiSocketAddress addr = WasiSocketAddress.ipv4(original, 80);
      final byte[] retrieved = addr.getAddress();
      retrieved[0] = 99;
      assertArrayEquals(
          new byte[] {10, 0, 0, 1},
          addr.getAddress(),
          "Modifying returned bytes should not affect internal state");
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  final class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal for same address and port")
    void shouldBeEqualForSameAddressAndPort() {
      final WasiSocketAddress addr1 =
          WasiSocketAddress.ipv4(new byte[] {10, 0, 0, 1}, 80);
      final WasiSocketAddress addr2 =
          WasiSocketAddress.ipv4(new byte[] {10, 0, 0, 1}, 80);
      assertEquals(addr1, addr2, "Same address and port should be equal");
      assertEquals(addr1.hashCode(), addr2.hashCode(), "Hash codes should match");
    }

    @Test
    @DisplayName("should not be equal for different ports")
    void shouldNotBeEqualForDifferentPorts() {
      final WasiSocketAddress addr1 =
          WasiSocketAddress.ipv4(new byte[] {10, 0, 0, 1}, 80);
      final WasiSocketAddress addr2 =
          WasiSocketAddress.ipv4(new byte[] {10, 0, 0, 1}, 443);
      assertNotEquals(addr1, addr2, "Different ports should not be equal");
    }

    @Test
    @DisplayName("should not be equal for different addresses")
    void shouldNotBeEqualForDifferentAddresses() {
      final WasiSocketAddress addr1 =
          WasiSocketAddress.ipv4(new byte[] {10, 0, 0, 1}, 80);
      final WasiSocketAddress addr2 =
          WasiSocketAddress.ipv4(new byte[] {10, 0, 0, 2}, 80);
      assertNotEquals(addr1, addr2, "Different addresses should not be equal");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should format IPv4 address correctly")
    void shouldFormatIpv4Correctly() {
      final WasiSocketAddress addr =
          WasiSocketAddress.ipv4(new byte[] {(byte) 192, (byte) 168, (byte) 1, (byte) 1}, 8080);
      final String str = addr.toString();
      assertEquals("192.168.1.1:8080", str, "IPv4 toString should use dotted decimal notation");
    }

    @Test
    @DisplayName("should format IPv6 address with brackets")
    void shouldFormatIpv6WithBrackets() {
      final byte[] ipv6Addr = new byte[16];
      ipv6Addr[15] = 1;
      final WasiSocketAddress addr = WasiSocketAddress.ipv6(ipv6Addr, 443);
      final String str = addr.toString();
      assertTrue(str.startsWith("["), "IPv6 toString should start with bracket");
      assertTrue(str.contains("]:443"), "IPv6 toString should end with ]:port");
    }
  }
}
