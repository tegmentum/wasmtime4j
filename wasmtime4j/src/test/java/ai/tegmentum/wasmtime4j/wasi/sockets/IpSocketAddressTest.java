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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IpSocketAddress} class.
 *
 * <p>IpSocketAddress is a variant type that holds either an IPv4 or IPv6 socket address per WASI
 * Preview 2 specification.
 */
@DisplayName("IpSocketAddress Tests")
class IpSocketAddressTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(IpSocketAddress.class.getModifiers()),
          "IpSocketAddress should be public");
      assertTrue(
          Modifier.isFinal(IpSocketAddress.class.getModifiers()),
          "IpSocketAddress should be final");
    }
  }

  @Nested
  @DisplayName("IPv4 Factory Method Tests")
  class Ipv4FactoryMethodTests {

    @Test
    @DisplayName("should create IPv4 socket address")
    void shouldCreateIpv4SocketAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertNotNull(socketAddress, "Socket address should not be null");
      assertTrue(socketAddress.isIpv4(), "Should be IPv4");
    }

    @Test
    @DisplayName("should reject null IPv4 socket address")
    void shouldRejectNullIpv4SocketAddress() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> IpSocketAddress.ipv4(null));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }

    @Test
    @DisplayName("should return correct IPv4 socket address")
    void shouldReturnCorrectIpv4SocketAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(80, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertEquals(ipv4Socket, socketAddress.getIpv4(), "Should return correct IPv4 socket");
    }
  }

  @Nested
  @DisplayName("IPv6 Factory Method Tests")
  class Ipv6FactoryMethodTests {

    @Test
    @DisplayName("should create IPv6 socket address")
    void shouldCreateIpv6SocketAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(8080, 0, address, 0);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv6(ipv6Socket);

      assertNotNull(socketAddress, "Socket address should not be null");
      assertFalse(socketAddress.isIpv4(), "Should not be IPv4");
    }

    @Test
    @DisplayName("should reject null IPv6 socket address")
    void shouldRejectNullIpv6SocketAddress() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> IpSocketAddress.ipv6(null));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }

    @Test
    @DisplayName("should return correct IPv6 socket address")
    void shouldReturnCorrectIpv6SocketAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(443, 0, address, 0);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv6(ipv6Socket);

      assertEquals(ipv6Socket, socketAddress.getIpv6(), "Should return correct IPv6 socket");
    }
  }

  @Nested
  @DisplayName("isIpv4 Method Tests")
  class IsIpv4Tests {

    @Test
    @DisplayName("should return true for IPv4 socket address")
    void shouldReturnTrueForIpv4() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertTrue(socketAddress.isIpv4(), "Should be IPv4");
    }

    @Test
    @DisplayName("should return false for IPv6 socket address")
    void shouldReturnFalseForIpv6() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(8080, 0, address, 0);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv6(ipv6Socket);

      assertFalse(socketAddress.isIpv4(), "Should not be IPv4");
    }
  }

  @Nested
  @DisplayName("getIpv4 Method Tests")
  class GetIpv4Tests {

    @Test
    @DisplayName("should return IPv4 socket address when IPv4")
    void shouldReturnIpv4SocketAddressWhenIpv4() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertNotNull(socketAddress.getIpv4(), "Should return IPv4 socket");
      assertEquals(ipv4Socket, socketAddress.getIpv4(), "Should return correct IPv4 socket");
    }

    @Test
    @DisplayName("should throw when not IPv4")
    void shouldThrowWhenNotIpv4() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(8080, 0, address, 0);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv6(ipv6Socket);

      assertThrows(IllegalStateException.class, () -> socketAddress.getIpv4());
    }
  }

  @Nested
  @DisplayName("getIpv6 Method Tests")
  class GetIpv6Tests {

    @Test
    @DisplayName("should return IPv6 socket address when IPv6")
    void shouldReturnIpv6SocketAddressWhenIpv6() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(8080, 0, address, 0);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv6(ipv6Socket);

      assertNotNull(socketAddress.getIpv6(), "Should return IPv6 socket");
      assertEquals(ipv6Socket, socketAddress.getIpv6(), "Should return correct IPv6 socket");
    }

    @Test
    @DisplayName("should throw when not IPv6")
    void shouldThrowWhenNotIpv6() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertThrows(IllegalStateException.class, () -> socketAddress.getIpv6());
    }
  }

  @Nested
  @DisplayName("equals Method Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertEquals(socketAddress, socketAddress, "Should equal itself");
    }

    @Test
    @DisplayName("should be equal to another with same IPv4 socket")
    void shouldBeEqualToAnotherWithSameIpv4Socket() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket1 = new Ipv4SocketAddress(8080, address1);
      final Ipv4SocketAddress ipv4Socket2 = new Ipv4SocketAddress(8080, address2);
      final IpSocketAddress socketAddress1 = IpSocketAddress.ipv4(ipv4Socket1);
      final IpSocketAddress socketAddress2 = IpSocketAddress.ipv4(ipv4Socket2);

      assertEquals(socketAddress1, socketAddress2, "Should be equal");
    }

    @Test
    @DisplayName("should be equal to another with same IPv6 socket")
    void shouldBeEqualToAnotherWithSameIpv6Socket() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6Socket1 = new Ipv6SocketAddress(443, 0, address1, 0);
      final Ipv6SocketAddress ipv6Socket2 = new Ipv6SocketAddress(443, 0, address2, 0);
      final IpSocketAddress socketAddress1 = IpSocketAddress.ipv6(ipv6Socket1);
      final IpSocketAddress socketAddress2 = IpSocketAddress.ipv6(ipv6Socket2);

      assertEquals(socketAddress1, socketAddress2, "Should be equal");
    }

    @Test
    @DisplayName("should not be equal when different address families")
    void shouldNotBeEqualWhenDifferentAddressFamilies() {
      final Ipv4Address ipv4Address = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv6Address ipv6Address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(80, ipv4Address);
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(80, 0, ipv6Address, 0);
      final IpSocketAddress socketAddress1 = IpSocketAddress.ipv4(ipv4Socket);
      final IpSocketAddress socketAddress2 = IpSocketAddress.ipv6(ipv6Socket);

      assertNotEquals(socketAddress1, socketAddress2, "Different families should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertNotEquals(null, socketAddress, "Should not equal null");
    }
  }

  @Nested
  @DisplayName("hashCode Method Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should return consistent hash code")
    void shouldReturnConsistentHashCode() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      final int hash1 = socketAddress.hashCode();
      final int hash2 = socketAddress.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    @DisplayName("should return same hash code for equal socket addresses")
    void shouldReturnSameHashCodeForEqualSocketAddresses() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket1 = new Ipv4SocketAddress(8080, address1);
      final Ipv4SocketAddress ipv4Socket2 = new Ipv4SocketAddress(8080, address2);
      final IpSocketAddress socketAddress1 = IpSocketAddress.ipv4(ipv4Socket1);
      final IpSocketAddress socketAddress2 = IpSocketAddress.ipv4(ipv4Socket2);

      assertEquals(
          socketAddress1.hashCode(), socketAddress2.hashCode(), "Equal should have same hash");
    }
  }

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return IPv4 string for IPv4 socket")
    void shouldReturnIpv4StringForIpv4Socket() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(8080, address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      assertEquals("192.168.1.1:8080", socketAddress.toString(), "Should return IPv4 format");
    }

    @Test
    @DisplayName("should return IPv6 string for IPv6 socket")
    void shouldReturnIpv6StringForIpv6Socket() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(8080, 0, address, 0);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv6(ipv6Socket);

      final String result = socketAddress.toString();
      assertTrue(result.startsWith("["), "Should have IPv6 bracket format");
      assertTrue(result.endsWith(":8080"), "Should include port");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should handle dual-stack server binding")
    void shouldHandleDualStackServerBinding() {
      // IPv4 wildcard 0.0.0.0
      final Ipv4Address ipv4Any = new Ipv4Address(new byte[] {0, 0, 0, 0});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(80, ipv4Any);
      final IpSocketAddress ipv4Bind = IpSocketAddress.ipv4(ipv4Socket);

      // IPv6 wildcard ::
      final Ipv6Address ipv6Any = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});
      final Ipv6SocketAddress ipv6Socket = new Ipv6SocketAddress(80, 0, ipv6Any, 0);
      final IpSocketAddress ipv6Bind = IpSocketAddress.ipv6(ipv6Socket);

      assertTrue(ipv4Bind.isIpv4(), "IPv4 bind address");
      assertFalse(ipv6Bind.isIpv4(), "IPv6 bind address");
    }

    @Test
    @DisplayName("should properly identify address family before access")
    void shouldProperlyIdentifyAddressFamilyBeforeAccess() {
      final Ipv4Address ipv4Address = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress ipv4Socket = new Ipv4SocketAddress(3000, ipv4Address);
      final IpSocketAddress socketAddress = IpSocketAddress.ipv4(ipv4Socket);

      // Safe pattern: check before access
      if (socketAddress.isIpv4()) {
        final Ipv4SocketAddress retrieved = socketAddress.getIpv4();
        assertEquals(3000, retrieved.getPort(), "Port from IPv4 socket");
      } else {
        final Ipv6SocketAddress retrieved = socketAddress.getIpv6();
        assertEquals(3000, retrieved.getPort(), "Port from IPv6 socket");
      }
    }
  }
}
