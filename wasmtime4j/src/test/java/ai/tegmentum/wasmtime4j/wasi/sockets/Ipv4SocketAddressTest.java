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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Ipv4SocketAddress} class.
 *
 * <p>Ipv4SocketAddress represents an IPv4 socket address with port and address per WASI Preview 2
 * specification.
 */
@DisplayName("Ipv4SocketAddress Tests")
class Ipv4SocketAddressTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(Ipv4SocketAddress.class.getModifiers()),
          "Ipv4SocketAddress should be public");
      assertTrue(
          Modifier.isFinal(Ipv4SocketAddress.class.getModifiers()),
          "Ipv4SocketAddress should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create socket address with valid port and address")
    void shouldCreateSocketAddressWithValidPortAndAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertNotNull(socketAddress, "Socket address should not be null");
    }

    @Test
    @DisplayName("should accept port 0")
    void shouldAcceptPort0() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(0, address);

      assertEquals(0, socketAddress.getPort(), "Should accept port 0");
    }

    @Test
    @DisplayName("should accept port 65535")
    void shouldAcceptPort65535() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(65535, address);

      assertEquals(65535, socketAddress.getPort(), "Should accept port 65535");
    }

    @Test
    @DisplayName("should reject negative port")
    void shouldRejectNegativePort() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(-1, address));

      assertTrue(ex.getMessage().contains("Port"), "Exception should mention Port");
    }

    @Test
    @DisplayName("should reject port above 65535")
    void shouldRejectPortAbove65535() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(65536, address));

      assertTrue(ex.getMessage().contains("Port"), "Exception should mention Port");
    }

    @Test
    @DisplayName("should reject null address")
    void shouldRejectNullAddress() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(80, null));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }
  }

  @Nested
  @DisplayName("getPort Method Tests")
  class GetPortTests {

    @Test
    @DisplayName("should return correct port")
    void shouldReturnCorrectPort() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertEquals(8080, socketAddress.getPort(), "Should return correct port");
    }

    @Test
    @DisplayName("should return well-known ports correctly")
    void shouldReturnWellKnownPortsCorrectly() {
      final Ipv4Address address = new Ipv4Address(new byte[] {127, 0, 0, 1});

      assertEquals(80, new Ipv4SocketAddress(80, address).getPort(), "HTTP port");
      assertEquals(443, new Ipv4SocketAddress(443, address).getPort(), "HTTPS port");
      assertEquals(22, new Ipv4SocketAddress(22, address).getPort(), "SSH port");
    }
  }

  @Nested
  @DisplayName("getAddress Method Tests")
  class GetAddressTests {

    @Test
    @DisplayName("should return correct address")
    void shouldReturnCorrectAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertEquals(address, socketAddress.getAddress(), "Should return correct address");
    }

    @Test
    @DisplayName("should return same address instance")
    void shouldReturnSameAddressInstance() {
      final Ipv4Address address = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(80, address);

      assertEquals(address, socketAddress.getAddress(), "Should return the same address");
    }
  }

  @Nested
  @DisplayName("equals Method Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertEquals(socketAddress, socketAddress, "Socket address should equal itself");
    }

    @Test
    @DisplayName("should be equal to socket address with same port and address")
    void shouldBeEqualToSocketAddressWithSamePortAndAddress() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress1 = new Ipv4SocketAddress(8080, address1);
      final Ipv4SocketAddress socketAddress2 = new Ipv4SocketAddress(8080, address2);

      assertEquals(socketAddress1, socketAddress2, "Socket addresses should be equal");
    }

    @Test
    @DisplayName("should not be equal to socket address with different port")
    void shouldNotBeEqualToSocketAddressWithDifferentPort() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress1 = new Ipv4SocketAddress(8080, address);
      final Ipv4SocketAddress socketAddress2 = new Ipv4SocketAddress(8081, address);

      assertNotEquals(socketAddress1, socketAddress2, "Different ports should not be equal");
    }

    @Test
    @DisplayName("should not be equal to socket address with different address")
    void shouldNotBeEqualToSocketAddressWithDifferentAddress() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final Ipv4SocketAddress socketAddress1 = new Ipv4SocketAddress(8080, address1);
      final Ipv4SocketAddress socketAddress2 = new Ipv4SocketAddress(8080, address2);

      assertNotEquals(socketAddress1, socketAddress2, "Different addresses should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertNotEquals(null, socketAddress, "Should not equal null");
    }

    @Test
    @DisplayName("should not be equal to object of different type")
    void shouldNotBeEqualToObjectOfDifferentType() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertNotEquals(socketAddress, "192.168.1.1:8080", "Should not equal String");
    }
  }

  @Nested
  @DisplayName("hashCode Method Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should return consistent hash code")
    void shouldReturnConsistentHashCode() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      final int hash1 = socketAddress.hashCode();
      final int hash2 = socketAddress.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    @DisplayName("should return same hash code for equal socket addresses")
    void shouldReturnSameHashCodeForEqualSocketAddresses() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress1 = new Ipv4SocketAddress(8080, address1);
      final Ipv4SocketAddress socketAddress2 = new Ipv4SocketAddress(8080, address2);

      assertEquals(
          socketAddress1.hashCode(),
          socketAddress2.hashCode(),
          "Equal socket addresses should have same hash");
    }
  }

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return address:port format")
    void shouldReturnAddressPortFormat() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertEquals("192.168.1.1:8080", socketAddress.toString(), "Should return address:port");
    }

    @Test
    @DisplayName("should handle localhost address")
    void shouldHandleLocalhostAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(80, address);

      assertEquals("127.0.0.1:80", socketAddress.toString(), "Should format localhost correctly");
    }

    @Test
    @DisplayName("should handle zero port")
    void shouldHandleZeroPort() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(0, address);

      assertEquals("0.0.0.0:0", socketAddress.toString(), "Should handle zero port");
    }

    @Test
    @DisplayName("should handle max port")
    void shouldHandleMaxPort() {
      final Ipv4Address address =
          new Ipv4Address(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(65535, address);

      assertEquals("255.255.255.255:65535", socketAddress.toString(), "Should handle max values");
    }
  }

  @Nested
  @DisplayName("Common Socket Address Tests")
  class CommonSocketAddressTests {

    @Test
    @DisplayName("should support HTTP server address")
    void shouldSupportHttpServerAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(80, address);

      assertEquals(80, socketAddress.getPort(), "HTTP port");
      assertEquals("0.0.0.0:80", socketAddress.toString(), "Bind to all interfaces");
    }

    @Test
    @DisplayName("should support HTTPS server address")
    void shouldSupportHttpsServerAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(443, address);

      assertEquals(443, socketAddress.getPort(), "HTTPS port");
    }

    @Test
    @DisplayName("should support localhost development server")
    void shouldSupportLocalhostDevelopmentServer() {
      final Ipv4Address localhost = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(3000, localhost);

      assertEquals("127.0.0.1:3000", socketAddress.toString(), "Dev server address");
    }
  }

  @Nested
  @DisplayName("Port Range Tests")
  class PortRangeTests {

    @Test
    @DisplayName("should support well-known ports (0-1023)")
    void shouldSupportWellKnownPorts() {
      final Ipv4Address address = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final Ipv4SocketAddress http = new Ipv4SocketAddress(80, address);
      final Ipv4SocketAddress https = new Ipv4SocketAddress(443, address);
      final Ipv4SocketAddress ssh = new Ipv4SocketAddress(22, address);

      assertEquals(80, http.getPort(), "HTTP port");
      assertEquals(443, https.getPort(), "HTTPS port");
      assertEquals(22, ssh.getPort(), "SSH port");
    }

    @Test
    @DisplayName("should support registered ports (1024-49151)")
    void shouldSupportRegisteredPorts() {
      final Ipv4Address address = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(8080, address);

      assertEquals(8080, socketAddress.getPort(), "Alternative HTTP port");
    }

    @Test
    @DisplayName("should support dynamic ports (49152-65535)")
    void shouldSupportDynamicPorts() {
      final Ipv4Address address = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final Ipv4SocketAddress socketAddress = new Ipv4SocketAddress(49152, address);

      assertEquals(49152, socketAddress.getPort(), "Dynamic port");
    }
  }
}
