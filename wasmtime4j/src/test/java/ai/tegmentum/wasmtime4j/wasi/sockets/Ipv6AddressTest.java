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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Ipv6Address} class.
 *
 * <p>Verifies construction validation (8 segments), defensive copies, equals/hashCode, and
 * colon-hex toString format.
 */
@DisplayName("Ipv6Address Tests")
class Ipv6AddressTest {

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create with valid 8-segment array")
    void shouldCreateWithValidSegments() {
      final short[] segments = {0x2001, 0x0DB8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address addr = new Ipv6Address(segments);
      assertArrayEquals(segments, addr.getSegments(), "Segments should match input");
    }

    @Test
    @DisplayName("should create with all zeros (unspecified)")
    void shouldCreateWithAllZeros() {
      final short[] segments = {0, 0, 0, 0, 0, 0, 0, 0};
      final Ipv6Address addr = new Ipv6Address(segments);
      assertArrayEquals(segments, addr.getSegments(), "All-zero segments should be accepted");
    }

    @Test
    @DisplayName("should create loopback address (::1)")
    void shouldCreateLoopbackAddress() {
      final short[] segments = {0, 0, 0, 0, 0, 0, 0, 1};
      final Ipv6Address addr = new Ipv6Address(segments);
      assertEquals(1, addr.getSegments()[7], "Last segment should be 1 for loopback");
    }

    @Test
    @DisplayName("should create with maximum segment values")
    void shouldCreateWithMaxSegmentValues() {
      final short[] segments = {
        (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF,
        (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF
      };
      final Ipv6Address addr = new Ipv6Address(segments);
      assertArrayEquals(segments, addr.getSegments(), "Max segment values should be accepted");
    }

    @Test
    @DisplayName("should throw for null segments")
    void shouldThrowForNullSegments() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6Address(null),
          "Should throw for null segments");
    }

    @Test
    @DisplayName("should throw for too few segments")
    void shouldThrowForTooFewSegments() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0}),
          "Should throw for 7 segments");
    }

    @Test
    @DisplayName("should throw for too many segments")
    void shouldThrowForTooManySegments() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0, 0}),
          "Should throw for 9 segments");
    }

    @Test
    @DisplayName("should throw for empty array")
    void shouldThrowForEmptyArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6Address(new short[] {}),
          "Should throw for empty array");
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("should defensively copy segments on construction")
    void shouldDefensivelyCopyOnConstruction() {
      final short[] segments = {0x2001, 0x0DB8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address addr = new Ipv6Address(segments);

      segments[0] = 9999;
      assertEquals(
          0x2001, addr.getSegments()[0], "Modifying original segments should not affect address");
    }

    @Test
    @DisplayName("should defensively copy segments on retrieval")
    void shouldDefensivelyCopyOnRetrieval() {
      final short[] segments = {0x2001, 0x0DB8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address addr = new Ipv6Address(segments);

      final short[] retrieved = addr.getSegments();
      retrieved[0] = 9999;
      assertEquals(
          0x2001, addr.getSegments()[0], "Modifying retrieved segments should not affect address");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("same segments should be equal")
    void sameSegmentsShouldBeEqual() {
      final Ipv6Address addr1 = new Ipv6Address(new short[] {0x2001, 0x0DB8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address addr2 = new Ipv6Address(new short[] {0x2001, 0x0DB8, 0, 0, 0, 0, 0, 1});
      assertEquals(addr1, addr2, "Addresses with same segments should be equal");
      assertEquals(
          addr1.hashCode(),
          addr2.hashCode(),
          "Addresses with same segments should have same hashCode");
    }

    @Test
    @DisplayName("different segments should not be equal")
    void differentSegmentsShouldNotBeEqual() {
      final Ipv6Address addr1 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6Address addr2 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 2});
      assertNotEquals(addr1, addr2, "Addresses with different segments should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      assertNotEquals(null, addr, "Address should not equal null");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      assertEquals(addr, addr, "Address should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format as colon-separated hex")
    void toStringShouldFormatAsColonHex() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0x2001, 0x0DB8, 0, 0, 0, 0, 0, 1});
      final String result = addr.toString();
      assertTrue(result.contains("2001"), "toString should contain first segment hex: " + result);
      assertTrue(result.contains("db8"), "toString should contain second segment hex: " + result);
      assertTrue(result.contains(":"), "toString should contain colon separators: " + result);
    }

    @Test
    @DisplayName("toString should format loopback correctly")
    void toStringShouldFormatLoopback() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      assertEquals(
          "0:0:0:0:0:0:0:1", addr.toString(), "Loopback toString should be 0:0:0:0:0:0:0:1");
    }

    @Test
    @DisplayName("toString should handle max segment values")
    void toStringShouldHandleMaxSegments() {
      final short[] maxSegments = {
        (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF,
        (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF, (short) 0xFFFF
      };
      final Ipv6Address addr = new Ipv6Address(maxSegments);
      assertEquals(
          "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
          addr.toString(),
          "Max segments toString should show all ffff");
    }
  }
}
