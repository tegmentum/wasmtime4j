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
 * Tests for {@link Ipv6SocketAddress} class.
 *
 * <p>Ipv6SocketAddress represents an IPv6 socket address with port, flow info, address, and scope
 * ID per WASI Preview 2 specification.
 */
@DisplayName("Ipv6SocketAddress Tests")
class Ipv6SocketAddressTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(Ipv6SocketAddress.class.getModifiers()),
          "Ipv6SocketAddress should be public");
      assertTrue(
          Modifier.isFinal(Ipv6SocketAddress.class.getModifiers()),
          "Ipv6SocketAddress should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create socket address with valid parameters")
    void shouldCreateSocketAddressWithValidParameters() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      assertNotNull(socketAddress, "Socket address should not be null");
    }

    @Test
    @DisplayName("should accept port 0")
    void shouldAcceptPort0() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(0, 0, address, 0);

      assertEquals(0, socketAddress.getPort(), "Should accept port 0");
    }

    @Test
    @DisplayName("should accept port 65535")
    void shouldAcceptPort65535() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(65535, 0, address, 0);

      assertEquals(65535, socketAddress.getPort(), "Should accept port 65535");
    }

    @Test
    @DisplayName("should reject negative port")
    void shouldRejectNegativePort() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> new Ipv6SocketAddress(-1, 0, address, 0));

      assertTrue(ex.getMessage().contains("Port"), "Exception should mention Port");
    }

    @Test
    @DisplayName("should reject port above 65535")
    void shouldRejectPortAbove65535() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> new Ipv6SocketAddress(65536, 0, address, 0));

      assertTrue(ex.getMessage().contains("Port"), "Exception should mention Port");
    }

    @Test
    @DisplayName("should reject null address")
    void shouldRejectNullAddress() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv6SocketAddress(80, 0, null, 0));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }

    @Test
    @DisplayName("should accept any flow info value")
    void shouldAcceptAnyFlowInfoValue() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0x12345678, address, 0);

      assertEquals(0x12345678, socketAddress.getFlowInfo(), "Should accept any flow info");
    }

    @Test
    @DisplayName("should accept any scope ID value")
    void shouldAcceptAnyScopeIdValue() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0, address, 2);

      assertEquals(2, socketAddress.getScopeId(), "Should accept scope ID 2");
    }
  }

  @Nested
  @DisplayName("getPort Method Tests")
  class GetPortTests {

    @Test
    @DisplayName("should return correct port")
    void shouldReturnCorrectPort() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      assertEquals(8080, socketAddress.getPort(), "Should return correct port");
    }
  }

  @Nested
  @DisplayName("getFlowInfo Method Tests")
  class GetFlowInfoTests {

    @Test
    @DisplayName("should return correct flow info")
    void shouldReturnCorrectFlowInfo() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0xABCD, address, 0);

      assertEquals(0xABCD, socketAddress.getFlowInfo(), "Should return correct flow info");
    }

    @Test
    @DisplayName("should return zero flow info when not set")
    void shouldReturnZeroFlowInfoWhenNotSet() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0, address, 0);

      assertEquals(0, socketAddress.getFlowInfo(), "Should return zero flow info");
    }
  }

  @Nested
  @DisplayName("getAddress Method Tests")
  class GetAddressTests {

    @Test
    @DisplayName("should return correct address")
    void shouldReturnCorrectAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      assertEquals(address, socketAddress.getAddress(), "Should return correct address");
    }
  }

  @Nested
  @DisplayName("getScopeId Method Tests")
  class GetScopeIdTests {

    @Test
    @DisplayName("should return correct scope ID")
    void shouldReturnCorrectScopeId() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0, address, 5);

      assertEquals(5, socketAddress.getScopeId(), "Should return correct scope ID");
    }

    @Test
    @DisplayName("should return zero scope ID when not set")
    void shouldReturnZeroScopeIdWhenNotSet() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0, address, 0);

      assertEquals(0, socketAddress.getScopeId(), "Should return zero scope ID");
    }
  }

  @Nested
  @DisplayName("equals Method Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      assertEquals(socketAddress, socketAddress, "Socket address should equal itself");
    }

    @Test
    @DisplayName("should be equal to socket address with same parameters")
    void shouldBeEqualToSocketAddressWithSameParameters() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress1 = new Ipv6SocketAddress(8080, 100, address1, 2);
      final Ipv6SocketAddress socketAddress2 = new Ipv6SocketAddress(8080, 100, address2, 2);

      assertEquals(socketAddress1, socketAddress2, "Socket addresses should be equal");
    }

    @Test
    @DisplayName("should not be equal to socket address with different port")
    void shouldNotBeEqualToSocketAddressWithDifferentPort() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress1 = new Ipv6SocketAddress(8080, 0, address, 0);
      final Ipv6SocketAddress socketAddress2 = new Ipv6SocketAddress(8081, 0, address, 0);

      assertNotEquals(socketAddress1, socketAddress2, "Different ports should not be equal");
    }

    @Test
    @DisplayName("should not be equal to socket address with different flow info")
    void shouldNotBeEqualToSocketAddressWithDifferentFlowInfo() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress1 = new Ipv6SocketAddress(8080, 100, address, 0);
      final Ipv6SocketAddress socketAddress2 = new Ipv6SocketAddress(8080, 200, address, 0);

      assertNotEquals(socketAddress1, socketAddress2, "Different flow info should not be equal");
    }

    @Test
    @DisplayName("should not be equal to socket address with different address")
    void shouldNotBeEqualToSocketAddressWithDifferentAddress() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress1 = new Ipv6SocketAddress(8080, 0, address1, 0);
      final Ipv6SocketAddress socketAddress2 = new Ipv6SocketAddress(8080, 0, address2, 0);

      assertNotEquals(socketAddress1, socketAddress2, "Different addresses should not be equal");
    }

    @Test
    @DisplayName("should not be equal to socket address with different scope ID")
    void shouldNotBeEqualToSocketAddressWithDifferentScopeId() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress1 = new Ipv6SocketAddress(80, 0, address, 1);
      final Ipv6SocketAddress socketAddress2 = new Ipv6SocketAddress(80, 0, address, 2);

      assertNotEquals(socketAddress1, socketAddress2, "Different scope IDs should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      assertNotEquals(null, socketAddress, "Should not equal null");
    }

    @Test
    @DisplayName("should not be equal to object of different type")
    void shouldNotBeEqualToObjectOfDifferentType() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      assertNotEquals(socketAddress, "[2001:db8::1]:8080", "Should not equal String");
    }
  }

  @Nested
  @DisplayName("hashCode Method Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should return consistent hash code")
    void shouldReturnConsistentHashCode() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      final int hash1 = socketAddress.hashCode();
      final int hash2 = socketAddress.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    @DisplayName("should return same hash code for equal socket addresses")
    void shouldReturnSameHashCodeForEqualSocketAddresses() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress1 = new Ipv6SocketAddress(8080, 100, address1, 2);
      final Ipv6SocketAddress socketAddress2 = new Ipv6SocketAddress(8080, 100, address2, 2);

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
    @DisplayName("should return bracketed address with port")
    void shouldReturnBracketedAddressWithPort() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(8080, 0, address, 0);

      final String result = socketAddress.toString();
      assertTrue(result.startsWith("["), "Should start with bracket");
      assertTrue(result.endsWith(":8080"), "Should end with :port");
    }

    @Test
    @DisplayName("should handle localhost address")
    void shouldHandleLocalhostAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0, address, 0);

      final String result = socketAddress.toString();
      assertTrue(result.contains("0:0:0:0:0:0:0:1"), "Should format ::1 correctly");
      assertTrue(result.endsWith(":80"), "Should include port");
    }

    @Test
    @DisplayName("should handle zero port")
    void shouldHandleZeroPort() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(0, 0, address, 0);

      final String result = socketAddress.toString();
      assertTrue(result.endsWith(":0"), "Should handle zero port");
    }
  }

  @Nested
  @DisplayName("Link-Local Address Tests")
  class LinkLocalAddressTests {

    @Test
    @DisplayName("should support link-local address with scope ID")
    void shouldSupportLinkLocalAddressWithScopeId() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0, address, 3);

      assertEquals(3, socketAddress.getScopeId(), "Should have scope ID for link-local");
      assertEquals(
          (short) 0xfe80, socketAddress.getAddress().getSegments()[0], "Link-local prefix");
    }

    @Test
    @DisplayName("should differentiate same link-local address with different scope IDs")
    void shouldDifferentiateSameLinkLocalWithDifferentScopeIds() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress1 = new Ipv6SocketAddress(80, 0, address, 1);
      final Ipv6SocketAddress socketAddress2 = new Ipv6SocketAddress(80, 0, address, 2);

      assertNotEquals(socketAddress1, socketAddress2, "Different scope IDs should differ");
    }
  }

  @Nested
  @DisplayName("Flow Info Tests")
  class FlowInfoTests {

    @Test
    @DisplayName("should preserve traffic class in flow info")
    void shouldPreserveTrafficClassInFlowInfo() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      // Flow info contains traffic class (8 bits) and flow label (20 bits)
      final int flowInfo = 0x0F123456; // Traffic class + flow label
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, flowInfo, address, 0);

      assertEquals(flowInfo, socketAddress.getFlowInfo(), "Should preserve flow info");
    }
  }

  @Nested
  @DisplayName("Common Socket Address Tests")
  class CommonSocketAddressTests {

    @Test
    @DisplayName("should support HTTP server on unspecified address")
    void shouldSupportHttpServerOnUnspecifiedAddress() {
      final Ipv6Address unspecified = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(80, 0, unspecified, 0);

      assertEquals(80, socketAddress.getPort(), "HTTP port");
    }

    @Test
    @DisplayName("should support localhost development server")
    void shouldSupportLocalhostDevelopmentServer() {
      final Ipv6Address localhost = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddress = new Ipv6SocketAddress(3000, 0, localhost, 0);

      assertEquals(3000, socketAddress.getPort(), "Dev server port");
    }
  }
}
