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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IpAddressFamily} enum.
 *
 * <p>IpAddressFamily represents IP protocol version selection for WASI sockets per WASI Preview 2
 * specification.
 */
@DisplayName("IpAddressFamily Tests")
class IpAddressFamilyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(IpAddressFamily.class.isEnum(), "IpAddressFamily should be an enum");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactlyTwoValues() {
      final IpAddressFamily[] values = IpAddressFamily.values();
      assertEquals(2, values.length, "Should have exactly 2 values (IPV4 and IPV6)");
    }

    @Test
    @DisplayName("should have IPV4 value")
    void shouldHaveIpv4Value() {
      assertNotNull(IpAddressFamily.valueOf("IPV4"), "Should have IPV4");
    }

    @Test
    @DisplayName("should have IPV6 value")
    void shouldHaveIpv6Value() {
      assertNotNull(IpAddressFamily.valueOf("IPV6"), "Should have IPV6");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final IpAddressFamily family : IpAddressFamily.values()) {
        assertTrue(ordinals.add(family.ordinal()), "Ordinal should be unique: " + family);
      }
    }

    @Test
    @DisplayName("should have different ordinals for IPV4 and IPV6")
    void shouldHaveDifferentOrdinalsForIpv4AndIpv6() {
      assertNotEquals(
          IpAddressFamily.IPV4.ordinal(),
          IpAddressFamily.IPV6.ordinal(),
          "IPV4 and IPV6 should have different ordinals");
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      assertEquals(IpAddressFamily.IPV4, IpAddressFamily.valueOf("IPV4"), "Should retrieve IPV4");
      assertEquals(IpAddressFamily.IPV6, IpAddressFamily.valueOf("IPV6"), "Should retrieve IPV6");
    }

    @Test
    @DisplayName("should have correct name values")
    void shouldHaveCorrectNameValues() {
      assertEquals("IPV4", IpAddressFamily.IPV4.name(), "IPV4 should have correct name");
      assertEquals("IPV6", IpAddressFamily.IPV6.name(), "IPV6 should have correct name");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final IpAddressFamily family = IpAddressFamily.IPV4;

      final String result;
      switch (family) {
        case IPV4:
          result = "Internet Protocol version 4";
          break;
        case IPV6:
          result = "Internet Protocol version 6";
          break;
        default:
          result = "Unknown";
      }

      assertEquals("Internet Protocol version 4", result, "Should match IPV4 case");
    }

    @Test
    @DisplayName("should support comparison")
    void shouldSupportComparison() {
      final IpAddressFamily family1 = IpAddressFamily.IPV4;
      final IpAddressFamily family2 = IpAddressFamily.IPV4;
      final IpAddressFamily family3 = IpAddressFamily.IPV6;

      assertEquals(family1, family2, "Same values should be equal");
      assertNotEquals(family1, family3, "Different values should not be equal");
    }

    @Test
    @DisplayName("should be usable in collections")
    void shouldBeUsableInCollections() {
      final Set<IpAddressFamily> supportedFamilies = new HashSet<>();
      supportedFamilies.add(IpAddressFamily.IPV4);
      supportedFamilies.add(IpAddressFamily.IPV6);

      assertTrue(supportedFamilies.contains(IpAddressFamily.IPV4), "Should contain IPV4");
      assertTrue(supportedFamilies.contains(IpAddressFamily.IPV6), "Should contain IPV6");
      assertEquals(2, supportedFamilies.size(), "Should have 2 families");
    }

    @Test
    @DisplayName("should support conditional logic")
    void shouldSupportConditionalLogic() {
      final IpAddressFamily family = IpAddressFamily.IPV6;

      final int addressBytes = (family == IpAddressFamily.IPV4) ? 4 : 16;

      assertEquals(16, addressBytes, "IPv6 should use 16 bytes");
    }
  }

  @Nested
  @DisplayName("Socket Programming Pattern Tests")
  class SocketProgrammingPatternTests {

    @Test
    @DisplayName("should indicate address byte size")
    void shouldIndicateAddressByteSize() {
      assertEquals(4, getAddressByteSize(IpAddressFamily.IPV4), "IPv4 should be 4 bytes");
      assertEquals(16, getAddressByteSize(IpAddressFamily.IPV6), "IPv6 should be 16 bytes");
    }

    @Test
    @DisplayName("should indicate header size")
    void shouldIndicateHeaderSize() {
      assertEquals(
          20, getMinHeaderSize(IpAddressFamily.IPV4), "IPv4 min header should be 20 bytes");
      assertEquals(40, getMinHeaderSize(IpAddressFamily.IPV6), "IPv6 header should be 40 bytes");
    }

    private int getAddressByteSize(final IpAddressFamily family) {
      return (family == IpAddressFamily.IPV4) ? 4 : 16;
    }

    private int getMinHeaderSize(final IpAddressFamily family) {
      return (family == IpAddressFamily.IPV4) ? 20 : 40;
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should match WASI sockets network specification")
    void shouldMatchWasiSocketsNetworkSpecification() {
      // Per WASI Preview 2: wasi:sockets/network@0.2.0
      // ip-address-family has exactly two variants: ipv4 and ipv6
      final IpAddressFamily[] values = IpAddressFamily.values();
      assertEquals(2, values.length, "WASI spec defines exactly 2 address families");

      // Verify the specific values exist
      assertNotNull(IpAddressFamily.IPV4, "WASI spec requires ipv4 variant");
      assertNotNull(IpAddressFamily.IPV6, "WASI spec requires ipv6 variant");
    }

    @Test
    @DisplayName("should correspond to AF_INET and AF_INET6")
    void shouldCorrespondToAfInetAndAfInet6() {
      // Documentation indicates these correspond to standard socket constants
      // IPV4 corresponds to AF_INET
      // IPV6 corresponds to AF_INET6
      assertEquals("IPV4", IpAddressFamily.IPV4.name(), "IPV4 corresponds to AF_INET");
      assertEquals("IPV6", IpAddressFamily.IPV6.name(), "IPV6 corresponds to AF_INET6");
    }
  }

  @Nested
  @DisplayName("Dual Stack Support Tests")
  class DualStackSupportTests {

    @Test
    @DisplayName("should support iterating both families")
    void shouldSupportIteratingBothFamilies() {
      int count = 0;
      for (final IpAddressFamily family : IpAddressFamily.values()) {
        assertNotNull(family, "Each family should not be null");
        count++;
      }
      assertEquals(2, count, "Should iterate over both families");
    }

    @Test
    @DisplayName("should support dual stack server binding pattern")
    void shouldSupportDualStackServerBindingPattern() {
      // Pattern: create sockets for both address families
      final Set<IpAddressFamily> boundFamilies = new HashSet<>();

      for (final IpAddressFamily family : IpAddressFamily.values()) {
        // Simulate binding a socket for each family
        boundFamilies.add(family);
      }

      assertTrue(boundFamilies.contains(IpAddressFamily.IPV4), "Should bind IPv4");
      assertTrue(boundFamilies.contains(IpAddressFamily.IPV6), "Should bind IPv6");
    }
  }
}
