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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Ipv4Address} class.
 *
 * <p>Ipv4Address represents an IPv4 address as four octets per WASI Preview 2 specification.
 */
@DisplayName("Ipv4Address Tests")
class Ipv4AddressTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(Ipv4Address.class.getModifiers()), "Ipv4Address should be public");
      assertTrue(Modifier.isFinal(Ipv4Address.class.getModifiers()), "Ipv4Address should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create address with valid 4-byte array")
    void shouldCreateAddressWithValidArray() {
      final byte[] octets = new byte[] {(byte) 192, (byte) 168, 1, 1};
      final Ipv4Address address = new Ipv4Address(octets);

      assertNotNull(address, "Address should not be null");
    }

    @Test
    @DisplayName("should reject null octets array")
    void shouldRejectNullOctets() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(null));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }

    @Test
    @DisplayName("should reject array with less than 4 bytes")
    void shouldRejectArrayWithLessThan4Bytes() {
      final byte[] octets = new byte[] {1, 2, 3};

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(octets));

      assertTrue(ex.getMessage().contains("4"), "Exception should mention 4 bytes");
    }

    @Test
    @DisplayName("should reject array with more than 4 bytes")
    void shouldRejectArrayWithMoreThan4Bytes() {
      final byte[] octets = new byte[] {1, 2, 3, 4, 5};

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(octets));

      assertTrue(ex.getMessage().contains("4"), "Exception should mention 4 bytes");
    }

    @Test
    @DisplayName("should reject empty array")
    void shouldRejectEmptyArray() {
      final byte[] octets = new byte[0];

      assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(octets));
    }

    @Test
    @DisplayName("should create defensive copy of input array")
    void shouldCreateDefensiveCopyOfInput() {
      final byte[] octets = new byte[] {(byte) 192, (byte) 168, 1, 1};
      final Ipv4Address address = new Ipv4Address(octets);

      // Modify original array
      octets[0] = 10;

      // Address should retain original value
      assertArrayEquals(
          new byte[] {(byte) 192, (byte) 168, 1, 1},
          address.getOctets(),
          "Address should not be affected by changes to original array");
    }
  }

  @Nested
  @DisplayName("getOctets Method Tests")
  class GetOctetsTests {

    @Test
    @DisplayName("should return correct octets")
    void shouldReturnCorrectOctets() {
      final byte[] expected = new byte[] {10, 0, 0, 1};
      final Ipv4Address address = new Ipv4Address(expected);

      assertArrayEquals(expected, address.getOctets(), "Should return correct octets");
    }

    @Test
    @DisplayName("should return defensive copy")
    void shouldReturnDefensiveCopy() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      final byte[] octets1 = address.getOctets();
      final byte[] octets2 = address.getOctets();

      assertNotSame(octets1, octets2, "Should return different array instances");
      assertArrayEquals(octets1, octets2, "Arrays should have same content");
    }

    @Test
    @DisplayName("should not allow modification of address through returned array")
    void shouldNotAllowModificationThroughReturnedArray() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final byte[] octets = address.getOctets();

      // Modify returned array
      octets[0] = 10;

      // Address should retain original value
      assertEquals(
          (byte) 192,
          address.getOctets()[0],
          "Address should not be affected by changes to returned array");
    }

    @Test
    @DisplayName("should handle all zeros address")
    void shouldHandleAllZerosAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});

      assertArrayEquals(new byte[] {0, 0, 0, 0}, address.getOctets(), "Should handle 0.0.0.0");
    }

    @Test
    @DisplayName("should handle broadcast address")
    void shouldHandleBroadcastAddress() {
      final Ipv4Address address =
          new Ipv4Address(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255});

      assertArrayEquals(
          new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
          address.getOctets(),
          "Should handle 255.255.255.255");
    }
  }

  @Nested
  @DisplayName("equals Method Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertEquals(address, address, "Address should equal itself");
    }

    @Test
    @DisplayName("should be equal to address with same octets")
    void shouldBeEqualToAddressWithSameOctets() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertEquals(address1, address2, "Addresses with same octets should be equal");
    }

    @Test
    @DisplayName("should not be equal to address with different octets")
    void shouldNotBeEqualToAddressWithDifferentOctets() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {10, 0, 0, 1});

      assertNotEquals(address1, address2, "Addresses with different octets should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertNotEquals(null, address, "Address should not equal null");
    }

    @Test
    @DisplayName("should not be equal to object of different type")
    void shouldNotBeEqualToObjectOfDifferentType() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertNotEquals(address, "192.168.1.1", "Address should not equal String");
    }

    @Test
    @DisplayName("should be symmetric")
    void shouldBeSymmetric() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertEquals(address1, address2, "address1 should equal address2");
      assertEquals(address2, address1, "address2 should equal address1");
    }

    @Test
    @DisplayName("should be transitive")
    void shouldBeTransitive() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address3 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertEquals(address1, address2, "address1 should equal address2");
      assertEquals(address2, address3, "address2 should equal address3");
      assertEquals(address1, address3, "address1 should equal address3");
    }
  }

  @Nested
  @DisplayName("hashCode Method Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should return consistent hash code")
    void shouldReturnConsistentHashCode() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      final int hash1 = address.hashCode();
      final int hash2 = address.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    @DisplayName("should return same hash code for equal addresses")
    void shouldReturnSameHashCodeForEqualAddresses() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertEquals(
          address1.hashCode(), address2.hashCode(), "Equal addresses should have same hash");
    }

    @Test
    @DisplayName("should likely return different hash codes for different addresses")
    void shouldLikelyReturnDifferentHashCodesForDifferentAddresses() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {10, 0, 0, 1});

      // Note: This is not a requirement, but addresses should have different hashes
      assertNotEquals(
          address1.hashCode(),
          address2.hashCode(),
          "Different addresses should likely have different hashes");
    }
  }

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return standard dotted decimal notation")
    void shouldReturnStandardDottedDecimalNotation() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertEquals("192.168.1.1", address.toString(), "Should return standard notation");
    }

    @Test
    @DisplayName("should handle zero address")
    void shouldHandleZeroAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});

      assertEquals("0.0.0.0", address.toString(), "Should format zero address correctly");
    }

    @Test
    @DisplayName("should handle broadcast address")
    void shouldHandleBroadcastAddress() {
      final Ipv4Address address =
          new Ipv4Address(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255});

      assertEquals("255.255.255.255", address.toString(), "Should format broadcast correctly");
    }

    @Test
    @DisplayName("should handle localhost address")
    void shouldHandleLocalhostAddress() {
      final Ipv4Address address = new Ipv4Address(new byte[] {127, 0, 0, 1});

      assertEquals("127.0.0.1", address.toString(), "Should format localhost correctly");
    }

    @Test
    @DisplayName("should handle high byte values as unsigned")
    void shouldHandleHighByteValuesAsUnsigned() {
      // Java bytes are signed, so 200 is stored as -56
      final Ipv4Address address =
          new Ipv4Address(new byte[] {(byte) 200, (byte) 180, (byte) 150, (byte) 100});

      assertEquals("200.180.150.100", address.toString(), "Should treat bytes as unsigned");
    }
  }

  @Nested
  @DisplayName("Common Address Tests")
  class CommonAddressTests {

    @Test
    @DisplayName("should support localhost 127.0.0.1")
    void shouldSupportLocalhost() {
      final Ipv4Address localhost = new Ipv4Address(new byte[] {127, 0, 0, 1});

      assertArrayEquals(
          new byte[] {127, 0, 0, 1}, localhost.getOctets(), "Should support localhost address");
    }

    @Test
    @DisplayName("should support private network 10.x.x.x")
    void shouldSupportPrivateNetwork10() {
      final Ipv4Address address = new Ipv4Address(new byte[] {10, 0, 0, 1});

      assertEquals("10.0.0.1", address.toString(), "Should support 10.x.x.x network");
    }

    @Test
    @DisplayName("should support private network 172.16.x.x")
    void shouldSupportPrivateNetwork172() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 172, 16, 0, 1});

      assertEquals("172.16.0.1", address.toString(), "Should support 172.16.x.x network");
    }

    @Test
    @DisplayName("should support multicast address range")
    void shouldSupportMulticastAddress() {
      // 224.0.0.1 is the all-hosts multicast address
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 224, 0, 0, 1});

      assertEquals("224.0.0.1", address.toString(), "Should support multicast addresses");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle single octet differences")
    void shouldHandleSingleOctetDifferences() {
      final Ipv4Address address1 = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final Ipv4Address address2 = new Ipv4Address(new byte[] {10, 0, 0, 2});

      assertNotEquals(address1, address2, "Addresses differing by one octet should not be equal");
      assertFalse(address1.toString().equals(address2.toString()), "String representations differ");
    }

    @Test
    @DisplayName("should handle maximum values in each octet")
    void shouldHandleMaximumValuesInEachOctet() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 255, 0, (byte) 255, 0});

      assertEquals("255.0.255.0", address.toString(), "Should handle 255 values correctly");
    }
  }
}
