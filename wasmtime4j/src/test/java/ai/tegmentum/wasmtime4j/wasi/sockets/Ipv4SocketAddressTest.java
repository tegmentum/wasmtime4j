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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Ipv4SocketAddress} class.
 *
 * <p>Verifies construction, port validation [0,65535], address accessor, equals/hashCode, and
 * addr:port toString format.
 */
@DisplayName("Ipv4SocketAddress Tests")
class Ipv4SocketAddressTest {

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create with valid port and address")
    void shouldCreateWithValidPortAndAddress() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      assertEquals(8080, sockAddr.getPort(), "Port should be 8080");
      assertEquals(addr, sockAddr.getAddress(), "Address should match");
    }

    @Test
    @DisplayName("should accept port 0")
    void shouldAcceptPortZero() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(0, addr);
      assertEquals(0, sockAddr.getPort(), "Port 0 should be accepted");
    }

    @Test
    @DisplayName("should accept port 65535")
    void shouldAcceptPort65535() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(65535, addr);
      assertEquals(65535, sockAddr.getPort(), "Port 65535 should be accepted");
    }

    @Test
    @DisplayName("should throw for negative port")
    void shouldThrowForNegativePort() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4SocketAddress(-1, addr),
          "Should throw for negative port");
    }

    @Test
    @DisplayName("should throw for port exceeding 65535")
    void shouldThrowForExcessivePort() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4SocketAddress(65536, addr),
          "Should throw for port > 65535");
    }

    @Test
    @DisplayName("should throw for null address")
    void shouldThrowForNullAddress() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4SocketAddress(80, null),
          "Should throw for null address");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("same port and address should be equal")
    void samePortAndAddressShouldBeEqual() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final Ipv4SocketAddress sa1 = new Ipv4SocketAddress(443, addr);
      final Ipv4SocketAddress sa2 = new Ipv4SocketAddress(443, addr);
      assertEquals(sa1, sa2, "Same port and address should be equal");
      assertEquals(
          sa1.hashCode(), sa2.hashCode(), "Same port and address should have same hashCode");
    }

    @Test
    @DisplayName("different ports should not be equal")
    void differentPortsShouldNotBeEqual() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final Ipv4SocketAddress sa1 = new Ipv4SocketAddress(80, addr);
      final Ipv4SocketAddress sa2 = new Ipv4SocketAddress(443, addr);
      assertNotEquals(sa1, sa2, "Different ports should not be equal");
    }

    @Test
    @DisplayName("different addresses should not be equal")
    void differentAddressesShouldNotBeEqual() {
      final Ipv4SocketAddress sa1 =
          new Ipv4SocketAddress(80, new Ipv4Address(new byte[] {10, 0, 0, 1}));
      final Ipv4SocketAddress sa2 =
          new Ipv4SocketAddress(80, new Ipv4Address(new byte[] {10, 0, 0, 2}));
      assertNotEquals(sa1, sa2, "Different addresses should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final Ipv4SocketAddress sa =
          new Ipv4SocketAddress(80, new Ipv4Address(new byte[] {127, 0, 0, 1}));
      assertNotEquals(null, sa, "Should not equal null");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final Ipv4SocketAddress sa =
          new Ipv4SocketAddress(80, new Ipv4Address(new byte[] {127, 0, 0, 1}));
      assertEquals(sa, sa, "Should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format as addr:port")
    void toStringShouldFormatAsAddrPort() {
      final Ipv4SocketAddress sa =
          new Ipv4SocketAddress(8080, new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1}));
      assertEquals("192.168.1.1:8080", sa.toString(), "toString should format as addr:port");
    }

    @Test
    @DisplayName("toString should handle port 0")
    void toStringShouldHandlePortZero() {
      final Ipv4SocketAddress sa =
          new Ipv4SocketAddress(0, new Ipv4Address(new byte[] {0, 0, 0, 0}));
      assertEquals("0.0.0.0:0", sa.toString(), "toString should handle port 0");
    }

    @Test
    @DisplayName("toString should handle max port")
    void toStringShouldHandleMaxPort() {
      final Ipv4SocketAddress sa =
          new Ipv4SocketAddress(65535, new Ipv4Address(new byte[] {127, 0, 0, 1}));
      assertEquals("127.0.0.1:65535", sa.toString(), "toString should handle max port 65535");
    }
  }
}
