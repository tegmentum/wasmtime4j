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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Ipv6SocketAddress} class.
 *
 * <p>Verifies construction, port validation, flow info, scope ID, address accessor,
 * equals/hashCode, and [addr]:port toString format.
 */
@DisplayName("Ipv6SocketAddress Tests")
class Ipv6SocketAddressTest {

  private static final Ipv6Address LOOPBACK = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create with all fields")
    void shouldCreateWithAllFields() {
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(8080, 42, LOOPBACK, 3);
      assertEquals(8080, sockAddr.getPort(), "Port should be 8080");
      assertEquals(42, sockAddr.getFlowInfo(), "FlowInfo should be 42");
      assertEquals(LOOPBACK, sockAddr.getAddress(), "Address should match");
      assertEquals(3, sockAddr.getScopeId(), "ScopeId should be 3");
    }

    @Test
    @DisplayName("should accept port 0")
    void shouldAcceptPortZero() {
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(0, 0, LOOPBACK, 0);
      assertEquals(0, sockAddr.getPort(), "Port 0 should be accepted");
    }

    @Test
    @DisplayName("should accept port 65535")
    void shouldAcceptPort65535() {
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(65535, 0, LOOPBACK, 0);
      assertEquals(65535, sockAddr.getPort(), "Port 65535 should be accepted");
    }

    @Test
    @DisplayName("should accept zero flow info and scope id")
    void shouldAcceptZeroFlowInfoAndScopeId() {
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(80, 0, LOOPBACK, 0);
      assertEquals(0, sockAddr.getFlowInfo(), "FlowInfo 0 should be accepted");
      assertEquals(0, sockAddr.getScopeId(), "ScopeId 0 should be accepted");
    }

    @Test
    @DisplayName("should accept non-zero flow info")
    void shouldAcceptNonZeroFlowInfo() {
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(80, 0x000FFFFF, LOOPBACK, 0);
      assertEquals(0x000FFFFF, sockAddr.getFlowInfo(), "Non-zero flow info should be accepted");
    }

    @Test
    @DisplayName("should accept non-zero scope id for link-local")
    void shouldAcceptNonZeroScopeId() {
      final Ipv6Address linkLocal =
          new Ipv6Address(new short[] {(short) 0xFE80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(80, 0, linkLocal, 5);
      assertEquals(5, sockAddr.getScopeId(), "Scope ID 5 should be accepted for link-local");
    }

    @Test
    @DisplayName("should throw for negative port")
    void shouldThrowForNegativePort() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6SocketAddress(-1, 0, LOOPBACK, 0),
          "Should throw for negative port");
    }

    @Test
    @DisplayName("should throw for port exceeding 65535")
    void shouldThrowForExcessivePort() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6SocketAddress(65536, 0, LOOPBACK, 0),
          "Should throw for port > 65535");
    }

    @Test
    @DisplayName("should throw for null address")
    void shouldThrowForNullAddress() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6SocketAddress(80, 0, null, 0),
          "Should throw for null address");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("same values should be equal")
    void sameValuesShouldBeEqual() {
      final Ipv6SocketAddress sa1 = new Ipv6SocketAddress(443, 10, LOOPBACK, 2);
      final Ipv6SocketAddress sa2 = new Ipv6SocketAddress(443, 10, LOOPBACK, 2);
      assertEquals(sa1, sa2, "Same values should be equal");
      assertEquals(sa1.hashCode(), sa2.hashCode(), "Same values should have same hashCode");
    }

    @Test
    @DisplayName("different ports should not be equal")
    void differentPortsShouldNotBeEqual() {
      final Ipv6SocketAddress sa1 = new Ipv6SocketAddress(80, 0, LOOPBACK, 0);
      final Ipv6SocketAddress sa2 = new Ipv6SocketAddress(443, 0, LOOPBACK, 0);
      assertNotEquals(sa1, sa2, "Different ports should not be equal");
    }

    @Test
    @DisplayName("different flow info should not be equal")
    void differentFlowInfoShouldNotBeEqual() {
      final Ipv6SocketAddress sa1 = new Ipv6SocketAddress(80, 1, LOOPBACK, 0);
      final Ipv6SocketAddress sa2 = new Ipv6SocketAddress(80, 2, LOOPBACK, 0);
      assertNotEquals(sa1, sa2, "Different flow info should not be equal");
    }

    @Test
    @DisplayName("different scope ids should not be equal")
    void differentScopeIdsShouldNotBeEqual() {
      final Ipv6SocketAddress sa1 = new Ipv6SocketAddress(80, 0, LOOPBACK, 1);
      final Ipv6SocketAddress sa2 = new Ipv6SocketAddress(80, 0, LOOPBACK, 2);
      assertNotEquals(sa1, sa2, "Different scope IDs should not be equal");
    }

    @Test
    @DisplayName("different addresses should not be equal")
    void differentAddressesShouldNotBeEqual() {
      final Ipv6Address addr2 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 2});
      final Ipv6SocketAddress sa1 = new Ipv6SocketAddress(80, 0, LOOPBACK, 0);
      final Ipv6SocketAddress sa2 = new Ipv6SocketAddress(80, 0, addr2, 0);
      assertNotEquals(sa1, sa2, "Different addresses should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final Ipv6SocketAddress sa = new Ipv6SocketAddress(80, 0, LOOPBACK, 0);
      assertNotEquals(null, sa, "Should not equal null");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final Ipv6SocketAddress sa = new Ipv6SocketAddress(80, 0, LOOPBACK, 0);
      assertEquals(sa, sa, "Should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format as [addr]:port")
    void toStringShouldFormatAsBracketedAddrPort() {
      final Ipv6SocketAddress sa = new Ipv6SocketAddress(8080, 0, LOOPBACK, 0);
      final String result = sa.toString();
      assertTrue(result.startsWith("["), "toString should start with [ bracket: " + result);
      assertTrue(result.contains("]:"), "toString should contain ]: separator: " + result);
      assertTrue(result.endsWith("8080"), "toString should end with port: " + result);
    }

    @Test
    @DisplayName("toString should match expected format for loopback")
    void toStringShouldMatchExpectedForLoopback() {
      final Ipv6SocketAddress sa = new Ipv6SocketAddress(443, 0, LOOPBACK, 0);
      assertEquals(
          "[0:0:0:0:0:0:0:1]:443",
          sa.toString(),
          "toString should format loopback as [0:0:0:0:0:0:0:1]:443");
    }

    @Test
    @DisplayName("toString should handle port 0")
    void toStringShouldHandlePortZero() {
      final Ipv6SocketAddress sa = new Ipv6SocketAddress(0, 0, LOOPBACK, 0);
      assertTrue(sa.toString().endsWith(":0"), "toString should end with :0 for port 0");
    }
  }
}
