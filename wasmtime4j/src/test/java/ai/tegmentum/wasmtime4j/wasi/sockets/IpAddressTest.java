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
 * Tests for the {@link IpAddress} tagged union class.
 *
 * <p>Verifies IPv4 and IPv6 factory methods, type checking, accessors, error behavior on wrong
 * variant access, and equals/hashCode/toString.
 */
@DisplayName("IpAddress Tests")
class IpAddressTest {

  @Nested
  @DisplayName("IPv4 Variant Tests")
  class Ipv4VariantTests {

    @Test
    @DisplayName("ipv4() should create IPv4 variant")
    void ipv4ShouldCreateIpv4Variant() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress ip = IpAddress.ipv4(addr);
      assertTrue(ip.isIpv4(), "Should be an IPv4 address");
    }

    @Test
    @DisplayName("getIpv4() should return the IPv4 address")
    void getIpv4ShouldReturnAddress() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final IpAddress ip = IpAddress.ipv4(addr);
      assertEquals(addr, ip.getIpv4(), "getIpv4() should return the stored IPv4 address");
    }

    @Test
    @DisplayName("isIpv4() should return false for IPv6 variant")
    void isIpv4ShouldReturnFalseForIpv6() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpAddress ip = IpAddress.ipv6(addr);
      assertFalse(ip.isIpv4(), "IPv6 address should not be IPv4");
    }

    @Test
    @DisplayName("getIpv6() should throw for IPv4 variant")
    void getIpv6ShouldThrowForIpv4Variant() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final IpAddress ip = IpAddress.ipv4(addr);
      assertThrows(
          IllegalStateException.class, ip::getIpv6, "getIpv6() should throw for IPv4 variant");
    }

    @Test
    @DisplayName("ipv4() should throw for null address")
    void ipv4ShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> IpAddress.ipv4(null),
          "ipv4() should throw for null address");
    }
  }

  @Nested
  @DisplayName("IPv6 Variant Tests")
  class Ipv6VariantTests {

    @Test
    @DisplayName("ipv6() should create IPv6 variant")
    void ipv6ShouldCreateIpv6Variant() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpAddress ip = IpAddress.ipv6(addr);
      assertFalse(ip.isIpv4(), "Should not be IPv4");
    }

    @Test
    @DisplayName("getIpv6() should return the IPv6 address")
    void getIpv6ShouldReturnAddress() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0x2001, 0x0DB8, 0, 0, 0, 0, 0, 1});
      final IpAddress ip = IpAddress.ipv6(addr);
      assertEquals(addr, ip.getIpv6(), "getIpv6() should return the stored IPv6 address");
    }

    @Test
    @DisplayName("getIpv4() should throw for IPv6 variant")
    void getIpv4ShouldThrowForIpv6Variant() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpAddress ip = IpAddress.ipv6(addr);
      assertThrows(
          IllegalStateException.class, ip::getIpv4, "getIpv4() should throw for IPv6 variant");
    }

    @Test
    @DisplayName("ipv6() should throw for null address")
    void ipv6ShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> IpAddress.ipv6(null),
          "ipv6() should throw for null address");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal IPv4 addresses should be equal")
    void equalIpv4AddressesShouldBeEqual() {
      final IpAddress ip1 = IpAddress.ipv4(new Ipv4Address(new byte[] {10, 0, 0, 1}));
      final IpAddress ip2 = IpAddress.ipv4(new Ipv4Address(new byte[] {10, 0, 0, 1}));
      assertEquals(ip1, ip2, "Same IPv4 addresses should be equal");
      assertEquals(ip1.hashCode(), ip2.hashCode(), "Same IPv4 addresses should have same hashCode");
    }

    @Test
    @DisplayName("equal IPv6 addresses should be equal")
    void equalIpv6AddressesShouldBeEqual() {
      final IpAddress ip1 = IpAddress.ipv6(new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1}));
      final IpAddress ip2 = IpAddress.ipv6(new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1}));
      assertEquals(ip1, ip2, "Same IPv6 addresses should be equal");
    }

    @Test
    @DisplayName("IPv4 and IPv6 should not be equal")
    void ipv4AndIpv6ShouldNotBeEqual() {
      final IpAddress ipv4 = IpAddress.ipv4(new Ipv4Address(new byte[] {0, 0, 0, 1}));
      final IpAddress ipv6 = IpAddress.ipv6(new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1}));
      assertNotEquals(ipv4, ipv6, "IPv4 and IPv6 should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final IpAddress ip = IpAddress.ipv4(new Ipv4Address(new byte[] {127, 0, 0, 1}));
      assertNotEquals(null, ip, "IpAddress should not equal null");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("IPv4 toString should delegate to Ipv4Address")
    void ipv4ToStringShouldDelegateToIpv4Address() {
      final IpAddress ip =
          IpAddress.ipv4(new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1}));
      assertEquals("192.168.1.1", ip.toString(), "IPv4 toString should show dotted decimal");
    }

    @Test
    @DisplayName("IPv6 toString should delegate to Ipv6Address")
    void ipv6ToStringShouldDelegateToIpv6Address() {
      final IpAddress ip = IpAddress.ipv6(new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1}));
      final String result = ip.toString();
      assertTrue(result.contains("1"), "IPv6 toString should contain the address: " + result);
    }
  }
}
