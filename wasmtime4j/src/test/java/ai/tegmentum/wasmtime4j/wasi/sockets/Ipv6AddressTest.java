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
 * Tests for {@link Ipv6Address} class.
 *
 * <p>Ipv6Address represents an IPv6 address as eight 16-bit segments per WASI Preview 2
 * specification.
 */
@DisplayName("Ipv6Address Tests")
class Ipv6AddressTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(Ipv6Address.class.getModifiers()), "Ipv6Address should be public");
      assertTrue(Modifier.isFinal(Ipv6Address.class.getModifiers()), "Ipv6Address should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create address with valid 8-segment array")
    void shouldCreateAddressWithValidArray() {
      final short[] segments =
          new short[] {0x2001, 0x0db8, (short) 0x85a3, 0, 0, (short) 0x8a2e, 0x0370, 0x7334};
      final Ipv6Address address = new Ipv6Address(segments);

      assertNotNull(address, "Address should not be null");
    }

    @Test
    @DisplayName("should reject null segments array")
    void shouldRejectNullSegments() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(null));

      assertTrue(ex.getMessage().contains("null"), "Exception should mention null");
    }

    @Test
    @DisplayName("should reject array with less than 8 segments")
    void shouldRejectArrayWithLessThan8Segments() {
      final short[] segments = new short[] {1, 2, 3, 4, 5, 6, 7};

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(segments));

      assertTrue(ex.getMessage().contains("8"), "Exception should mention 8 segments");
    }

    @Test
    @DisplayName("should reject array with more than 8 segments")
    void shouldRejectArrayWithMoreThan8Segments() {
      final short[] segments = new short[] {1, 2, 3, 4, 5, 6, 7, 8, 9};

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(segments));

      assertTrue(ex.getMessage().contains("8"), "Exception should mention 8 segments");
    }

    @Test
    @DisplayName("should reject empty array")
    void shouldRejectEmptyArray() {
      final short[] segments = new short[0];

      assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(segments));
    }

    @Test
    @DisplayName("should create defensive copy of input array")
    void shouldCreateDefensiveCopyOfInput() {
      final short[] segments = new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address address = new Ipv6Address(segments);

      // Modify original array
      segments[0] = (short) 0xfe80;

      // Address should retain original value
      assertEquals(
          (short) 0x2001,
          address.getSegments()[0],
          "Address should not be affected by changes to original array");
    }
  }

  @Nested
  @DisplayName("getSegments Method Tests")
  class GetSegmentsTests {

    @Test
    @DisplayName("should return correct segments")
    void shouldReturnCorrectSegments() {
      final short[] expected = new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address address = new Ipv6Address(expected);

      assertArrayEquals(expected, address.getSegments(), "Should return correct segments");
    }

    @Test
    @DisplayName("should return defensive copy")
    void shouldReturnDefensiveCopy() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      final short[] segments1 = address.getSegments();
      final short[] segments2 = address.getSegments();

      assertNotSame(segments1, segments2, "Should return different array instances");
      assertArrayEquals(segments1, segments2, "Arrays should have same content");
    }

    @Test
    @DisplayName("should not allow modification of address through returned array")
    void shouldNotAllowModificationThroughReturnedArray() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final short[] segments = address.getSegments();

      // Modify returned array
      segments[0] = (short) 0xfe80;

      // Address should retain original value
      assertEquals(
          (short) 0x2001,
          address.getSegments()[0],
          "Address should not be affected by changes to returned array");
    }

    @Test
    @DisplayName("should handle all zeros address")
    void shouldHandleAllZerosAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});

      assertArrayEquals(
          new short[] {0, 0, 0, 0, 0, 0, 0, 0}, address.getSegments(), "Should handle :: address");
    }

    @Test
    @DisplayName("should handle all ones address")
    void shouldHandleAllOnesAddress() {
      final short[] allOnes =
          new short[] {
            (short) 0xFFFF,
            (short) 0xFFFF,
            (short) 0xFFFF,
            (short) 0xFFFF,
            (short) 0xFFFF,
            (short) 0xFFFF,
            (short) 0xFFFF,
            (short) 0xFFFF
          };
      final Ipv6Address address = new Ipv6Address(allOnes);

      assertArrayEquals(allOnes, address.getSegments(), "Should handle all-ones address");
    }
  }

  @Nested
  @DisplayName("equals Method Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertEquals(address, address, "Address should equal itself");
    }

    @Test
    @DisplayName("should be equal to address with same segments")
    void shouldBeEqualToAddressWithSameSegments() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertEquals(address1, address2, "Addresses with same segments should be equal");
    }

    @Test
    @DisplayName("should not be equal to address with different segments")
    void shouldNotBeEqualToAddressWithDifferentSegments() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});

      assertNotEquals(address1, address2, "Addresses with different segments should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertNotEquals(null, address, "Address should not equal null");
    }

    @Test
    @DisplayName("should not be equal to object of different type")
    void shouldNotBeEqualToObjectOfDifferentType() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertNotEquals(address, "2001:db8::1", "Address should not equal String");
    }

    @Test
    @DisplayName("should be symmetric")
    void shouldBeSymmetric() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertEquals(address1, address2, "address1 should equal address2");
      assertEquals(address2, address1, "address2 should equal address1");
    }

    @Test
    @DisplayName("should be transitive")
    void shouldBeTransitive() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address3 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

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
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      final int hash1 = address.hashCode();
      final int hash2 = address.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    @DisplayName("should return same hash code for equal addresses")
    void shouldReturnSameHashCodeForEqualAddresses() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertEquals(
          address1.hashCode(), address2.hashCode(), "Equal addresses should have same hash");
    }

    @Test
    @DisplayName("should likely return different hash codes for different addresses")
    void shouldLikelyReturnDifferentHashCodesForDifferentAddresses() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});

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
    @DisplayName("should return colon-separated hex notation")
    void shouldReturnColonSeparatedHexNotation() {
      final Ipv6Address address =
          new Ipv6Address(
              new short[] {0x2001, 0x0db8, (short) 0x85a3, 0, 0, (short) 0x8a2e, 0x0370, 0x7334});

      final String expected = "2001:db8:85a3:0:0:8a2e:370:7334";
      assertEquals(expected, address.toString(), "Should return hex notation");
    }

    @Test
    @DisplayName("should handle zero address")
    void shouldHandleZeroAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});

      assertEquals("0:0:0:0:0:0:0:0", address.toString(), "Should format :: as 0:0:0:0:0:0:0:0");
    }

    @Test
    @DisplayName("should handle localhost address")
    void shouldHandleLocalhostAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});

      assertEquals("0:0:0:0:0:0:0:1", address.toString(), "Should format ::1 correctly");
    }

    @Test
    @DisplayName("should handle high segment values as unsigned")
    void shouldHandleHighSegmentValuesAsUnsigned() {
      final Ipv6Address address =
          new Ipv6Address(
              new short[] {
                (short) 0xFFFF, (short) 0xFE80, (short) 0xABCD, 0, 0, 0, 0, (short) 0xDEAD
              });

      assertTrue(address.toString().contains("ffff"), "Should treat segments as unsigned");
      assertTrue(address.toString().contains("fe80"), "Should include all segments");
    }

    @Test
    @DisplayName("should handle link-local address")
    void shouldHandleLinkLocalAddress() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0x0001, 0x0002, 0x0003, 0x0004});

      final String str = address.toString();
      assertTrue(str.startsWith("fe80:"), "Link-local address should start with fe80:");
    }
  }

  @Nested
  @DisplayName("Common Address Tests")
  class CommonAddressTests {

    @Test
    @DisplayName("should support localhost ::1")
    void shouldSupportLocalhost() {
      final Ipv6Address localhost = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});

      assertArrayEquals(
          new short[] {0, 0, 0, 0, 0, 0, 0, 1},
          localhost.getSegments(),
          "Should support localhost address");
    }

    @Test
    @DisplayName("should support unspecified address ::")
    void shouldSupportUnspecifiedAddress() {
      final Ipv6Address unspecified = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});

      assertArrayEquals(
          new short[] {0, 0, 0, 0, 0, 0, 0, 0},
          unspecified.getSegments(),
          "Should support unspecified address");
    }

    @Test
    @DisplayName("should support link-local prefix")
    void shouldSupportLinkLocalPrefix() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});

      assertEquals((short) 0xfe80, address.getSegments()[0], "Should support fe80:: prefix");
    }

    @Test
    @DisplayName("should support multicast address")
    void shouldSupportMulticastAddress() {
      // ff02::1 is all-nodes multicast
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xff02, 0, 0, 0, 0, 0, 0, 1});

      assertEquals((short) 0xff02, address.getSegments()[0], "Should support multicast prefix");
    }

    @Test
    @DisplayName("should support documentation address 2001:db8::")
    void shouldSupportDocumentationAddress() {
      final Ipv6Address address = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertTrue(address.toString().startsWith("2001:db8:"), "Should support documentation prefix");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle single segment differences")
    void shouldHandleSingleSegmentDifferences() {
      final Ipv6Address address1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address address2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 2});

      assertNotEquals(address1, address2, "Addresses differing by one segment should not be equal");
      assertFalse(address1.toString().equals(address2.toString()), "String representations differ");
    }

    @Test
    @DisplayName("should handle maximum values in each segment")
    void shouldHandleMaximumValuesInEachSegment() {
      final Ipv6Address address =
          new Ipv6Address(
              new short[] {
                (short) 0xFFFF, 0, (short) 0xFFFF, 0, (short) 0xFFFF, 0, (short) 0xFFFF, 0
              });

      assertTrue(address.toString().contains("ffff"), "Should handle 0xFFFF values correctly");
    }

    @Test
    @DisplayName("should handle IPv4-mapped IPv6 address")
    void shouldHandleIpv4MappedAddress() {
      // ::ffff:192.0.2.1 represented as segments
      final Ipv6Address address =
          new Ipv6Address(
              new short[] {0, 0, 0, 0, 0, (short) 0xFFFF, (short) 0xC000, (short) 0x0201});

      assertArrayEquals(
          new short[] {0, 0, 0, 0, 0, (short) 0xFFFF, (short) 0xC000, (short) 0x0201},
          address.getSegments(),
          "Should handle IPv4-mapped address");
    }
  }
}
