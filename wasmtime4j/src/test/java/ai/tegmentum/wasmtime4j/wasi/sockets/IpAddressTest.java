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
 * Tests for {@link IpAddress} variant type.
 *
 * <p>IpAddress holds either IPv4 or IPv6 address per WASI Preview 2 specification.
 */
@DisplayName("IpAddress Tests")
class IpAddressTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(IpAddress.class.getModifiers()), "IpAddress should be public");
      assertTrue(Modifier.isFinal(IpAddress.class.getModifiers()), "IpAddress should be final");
    }

    @Test
    @DisplayName("should not have public constructor")
    void shouldNotHavePublicConstructor() {
      // IpAddress uses static factory methods
      assertEquals(
          0,
          java.util.Arrays.stream(IpAddress.class.getConstructors())
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .count(),
          "Should not have public constructors");
    }
  }

  @Nested
  @DisplayName("IPv4 Factory Method Tests")
  class Ipv4FactoryTests {

    @Test
    @DisplayName("should create IPv4 address variant")
    void shouldCreateIpv4AddressVariant() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertNotNull(address, "Address should not be null");
      assertTrue(address.isIpv4(), "Should be IPv4 variant");
    }

    @Test
    @DisplayName("should reject null IPv4 address")
    void shouldRejectNullIpv4Address() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> IpAddress.ipv4(null));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }

    @Test
    @DisplayName("should return correct IPv4 address")
    void shouldReturnCorrectIpv4Address() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertEquals(ipv4, address.getIpv4(), "Should return the IPv4 address");
    }
  }

  @Nested
  @DisplayName("IPv6 Factory Method Tests")
  class Ipv6FactoryTests {

    @Test
    @DisplayName("should create IPv6 address variant")
    void shouldCreateIpv6AddressVariant() {
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final IpAddress address = IpAddress.ipv6(ipv6);

      assertNotNull(address, "Address should not be null");
      assertFalse(address.isIpv4(), "Should not be IPv4 variant");
    }

    @Test
    @DisplayName("should reject null IPv6 address")
    void shouldRejectNullIpv6Address() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> IpAddress.ipv6(null));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }

    @Test
    @DisplayName("should return correct IPv6 address")
    void shouldReturnCorrectIpv6Address() {
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final IpAddress address = IpAddress.ipv6(ipv6);

      assertEquals(ipv6, address.getIpv6(), "Should return the IPv6 address");
    }
  }

  @Nested
  @DisplayName("isIpv4 Method Tests")
  class IsIpv4Tests {

    @Test
    @DisplayName("should return true for IPv4 variant")
    void shouldReturnTrueForIpv4Variant() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertTrue(address.isIpv4(), "Should return true for IPv4");
    }

    @Test
    @DisplayName("should return false for IPv6 variant")
    void shouldReturnFalseForIpv6Variant() {
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpAddress address = IpAddress.ipv6(ipv6);

      assertFalse(address.isIpv4(), "Should return false for IPv6");
    }
  }

  @Nested
  @DisplayName("getIpv4 Method Tests")
  class GetIpv4Tests {

    @Test
    @DisplayName("should return IPv4 address when variant is IPv4")
    void shouldReturnIpv4AddressWhenVariantIsIpv4() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertNotNull(address.getIpv4(), "Should return IPv4 address");
      assertEquals(ipv4, address.getIpv4(), "Should return correct address");
    }

    @Test
    @DisplayName("should throw IllegalStateException when variant is IPv6")
    void shouldThrowIllegalStateExceptionWhenVariantIsIpv6() {
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpAddress address = IpAddress.ipv6(ipv6);

      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> address.getIpv4());

      assertTrue(ex.getMessage().contains("IPv4"), "Exception should mention IPv4");
    }
  }

  @Nested
  @DisplayName("getIpv6 Method Tests")
  class GetIpv6Tests {

    @Test
    @DisplayName("should return IPv6 address when variant is IPv6")
    void shouldReturnIpv6AddressWhenVariantIsIpv6() {
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final IpAddress address = IpAddress.ipv6(ipv6);

      assertNotNull(address.getIpv6(), "Should return IPv6 address");
      assertEquals(ipv6, address.getIpv6(), "Should return correct address");
    }

    @Test
    @DisplayName("should throw IllegalStateException when variant is IPv4")
    void shouldThrowIllegalStateExceptionWhenVariantIsIpv4() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> address.getIpv6());

      assertTrue(ex.getMessage().contains("IPv6"), "Exception should mention IPv6");
    }
  }

  @Nested
  @DisplayName("equals Method Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertEquals(address, address, "Address should equal itself");
    }

    @Test
    @DisplayName("should be equal to address with same IPv4")
    void shouldBeEqualToAddressWithSameIpv4() {
      final Ipv4Address ipv4a = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address ipv4b = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final IpAddress address1 = IpAddress.ipv4(ipv4a);
      final IpAddress address2 = IpAddress.ipv4(ipv4b);

      assertEquals(address1, address2, "Addresses should be equal");
    }

    @Test
    @DisplayName("should be equal to address with same IPv6")
    void shouldBeEqualToAddressWithSameIpv6() {
      final Ipv6Address ipv6a = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address ipv6b = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final IpAddress address1 = IpAddress.ipv6(ipv6a);
      final IpAddress address2 = IpAddress.ipv6(ipv6b);

      assertEquals(address1, address2, "Addresses should be equal");
    }

    @Test
    @DisplayName("should not be equal to address with different IPv4")
    void shouldNotBeEqualToAddressWithDifferentIpv4() {
      final Ipv4Address ipv4a = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address ipv4b = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final IpAddress address1 = IpAddress.ipv4(ipv4a);
      final IpAddress address2 = IpAddress.ipv4(ipv4b);

      assertNotEquals(address1, address2, "Different addresses should not be equal");
    }

    @Test
    @DisplayName("should not be equal when one is IPv4 and other is IPv6")
    void shouldNotBeEqualWhenDifferentVariants() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpAddress address1 = IpAddress.ipv4(ipv4);
      final IpAddress address2 = IpAddress.ipv6(ipv6);

      assertNotEquals(address1, address2, "IPv4 and IPv6 should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertNotEquals(null, address, "Should not equal null");
    }

    @Test
    @DisplayName("should not be equal to object of different type")
    void shouldNotBeEqualToObjectOfDifferentType() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertNotEquals(address, "127.0.0.1", "Should not equal String");
    }
  }

  @Nested
  @DisplayName("hashCode Method Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should return consistent hash code")
    void shouldReturnConsistentHashCode() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      final int hash1 = address.hashCode();
      final int hash2 = address.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    @DisplayName("should return same hash code for equal IPv4 addresses")
    void shouldReturnSameHashCodeForEqualIpv4Addresses() {
      final Ipv4Address ipv4a = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address ipv4b = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final IpAddress address1 = IpAddress.ipv4(ipv4a);
      final IpAddress address2 = IpAddress.ipv4(ipv4b);

      assertEquals(
          address1.hashCode(), address2.hashCode(), "Equal addresses should have same hash");
    }

    @Test
    @DisplayName("should return same hash code for equal IPv6 addresses")
    void shouldReturnSameHashCodeForEqualIpv6Addresses() {
      final Ipv6Address ipv6a = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address ipv6b = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final IpAddress address1 = IpAddress.ipv6(ipv6a);
      final IpAddress address2 = IpAddress.ipv6(ipv6b);

      assertEquals(
          address1.hashCode(), address2.hashCode(), "Equal addresses should have same hash");
    }
  }

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return IPv4 string for IPv4 variant")
    void shouldReturnIpv4StringForIpv4Variant() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertEquals("192.168.1.1", address.toString(), "Should return IPv4 string");
    }

    @Test
    @DisplayName("should return IPv6 string for IPv6 variant")
    void shouldReturnIpv6StringForIpv6Variant() {
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final IpAddress address = IpAddress.ipv6(ipv6);

      final String result = address.toString();
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains(":"), "IPv6 string should contain colons");
    }

    @Test
    @DisplayName("should return localhost IPv4 string")
    void shouldReturnLocalhostIpv4String() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      assertEquals("127.0.0.1", address.toString(), "Should return localhost string");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should match WASI sockets network ip-address variant")
    void shouldMatchWasiSocketsNetworkIpAddressVariant() {
      // Per WASI Preview 2: wasi:sockets/network@0.2.0
      // ip-address is a variant with ipv4 and ipv6 cases
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv6Address ipv6 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});

      final IpAddress ipv4Address = IpAddress.ipv4(ipv4);
      final IpAddress ipv6Address = IpAddress.ipv6(ipv6);

      assertTrue(ipv4Address.isIpv4(), "IPv4 variant should report isIpv4 true");
      assertFalse(ipv6Address.isIpv4(), "IPv6 variant should report isIpv4 false");
    }

    @Test
    @DisplayName("should be used for DNS resolution results")
    void shouldBeUsedForDnsResolutionResults() {
      // IpAddress is used to represent resolved addresses from DNS lookups
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {93, (byte) 184, (byte) 216, 34});
      final IpAddress resolved = IpAddress.ipv4(ipv4);

      assertTrue(resolved.isIpv4(), "Resolved IPv4 should be IPv4 variant");
      assertNotNull(resolved.getIpv4(), "Should be able to get IPv4 address");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support pattern matching style access")
    void shouldSupportPatternMatchingStyleAccess() {
      final Ipv4Address ipv4 = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final IpAddress address = IpAddress.ipv4(ipv4);

      final String result;
      if (address.isIpv4()) {
        result = "IPv4: " + address.getIpv4().toString();
      } else {
        result = "IPv6: " + address.getIpv6().toString();
      }

      assertEquals("IPv4: 10.0.0.1", result, "Pattern matching access should work");
    }

    @Test
    @DisplayName("should support both address families")
    void shouldSupportBothAddressFamilies() {
      final IpAddress[] addresses = {
        IpAddress.ipv4(new Ipv4Address(new byte[] {127, 0, 0, 1})),
        IpAddress.ipv6(new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1}))
      };

      int ipv4Count = 0;
      int ipv6Count = 0;
      for (final IpAddress addr : addresses) {
        if (addr.isIpv4()) {
          ipv4Count++;
        } else {
          ipv6Count++;
        }
      }

      assertEquals(1, ipv4Count, "Should have 1 IPv4 address");
      assertEquals(1, ipv6Count, "Should have 1 IPv6 address");
    }
  }
}
