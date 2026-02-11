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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Ipv4Address} class.
 *
 * <p>Verifies construction validation (4 bytes required), defensive copies on octets,
 * equals/hashCode, and dotted-decimal toString format.
 */
@DisplayName("Ipv4Address Tests")
class Ipv4AddressTest {

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create with valid 4-byte array")
    void shouldCreateWithValidBytes() {
      final byte[] octets = {127, 0, 0, 1};
      final Ipv4Address addr = new Ipv4Address(octets);
      assertArrayEquals(octets, addr.getOctets(), "Octets should match input");
    }

    @Test
    @DisplayName("should create with all zeros")
    void shouldCreateWithAllZeros() {
      final byte[] octets = {0, 0, 0, 0};
      final Ipv4Address addr = new Ipv4Address(octets);
      assertArrayEquals(octets, addr.getOctets(), "All-zero octets should be accepted");
    }

    @Test
    @DisplayName("should create with all 255s (broadcast)")
    void shouldCreateWithAll255s() {
      final byte[] octets = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
      final Ipv4Address addr = new Ipv4Address(octets);
      final byte[] retrieved = addr.getOctets();
      assertEquals((byte) 0xFF, retrieved[0], "First octet should be 0xFF");
      assertEquals((byte) 0xFF, retrieved[3], "Last octet should be 0xFF");
    }

    @Test
    @DisplayName("should throw for null octets")
    void shouldThrowForNullOctets() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4Address(null),
          "Should throw for null octets");
    }

    @Test
    @DisplayName("should throw for too few bytes")
    void shouldThrowForTooFewBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4Address(new byte[] {1, 2, 3}),
          "Should throw for 3 bytes");
    }

    @Test
    @DisplayName("should throw for too many bytes")
    void shouldThrowForTooManyBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4Address(new byte[] {1, 2, 3, 4, 5}),
          "Should throw for 5 bytes");
    }

    @Test
    @DisplayName("should throw for empty array")
    void shouldThrowForEmptyArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4Address(new byte[] {}),
          "Should throw for empty array");
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("should defensively copy octets on construction")
    void shouldDefensivelyCopyOnConstruction() {
      final byte[] octets = {10, 0, 0, 1};
      final Ipv4Address addr = new Ipv4Address(octets);

      octets[0] = 99;
      assertEquals(10, addr.getOctets()[0], "Modifying original octets should not affect address");
    }

    @Test
    @DisplayName("should defensively copy octets on retrieval")
    void shouldDefensivelyCopyOnRetrieval() {
      final byte[] octets = {10, 0, 0, 1};
      final Ipv4Address addr = new Ipv4Address(octets);

      final byte[] retrieved = addr.getOctets();
      retrieved[0] = 99;
      assertEquals(10, addr.getOctets()[0], "Modifying retrieved octets should not affect address");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("same octets should be equal")
    void sameOctetsShouldBeEqual() {
      final Ipv4Address addr1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address addr2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      assertEquals(addr1, addr2, "Addresses with same octets should be equal");
      assertEquals(
          addr1.hashCode(),
          addr2.hashCode(),
          "Addresses with same octets should have same hashCode");
    }

    @Test
    @DisplayName("different octets should not be equal")
    void differentOctetsShouldNotBeEqual() {
      final Ipv4Address addr1 = new Ipv4Address(new byte[] {10, 0, 0, 1});
      final Ipv4Address addr2 = new Ipv4Address(new byte[] {10, 0, 0, 2});
      assertNotEquals(addr1, addr2, "Addresses with different octets should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertNotEquals(null, addr, "Address should not equal null");
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertNotEquals("127.0.0.1", addr, "Address should not equal a String");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {10, 0, 0, 1});
      assertEquals(addr, addr, "Address should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format as dotted decimal")
    void toStringShouldFormatAsDottedDecimal() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 100});
      assertEquals("192.168.1.100", addr.toString(), "toString should format as dotted decimal");
    }

    @Test
    @DisplayName("toString should handle loopback")
    void toStringShouldHandleLoopback() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertEquals("127.0.0.1", addr.toString(), "toString should format loopback as 127.0.0.1");
    }

    @Test
    @DisplayName("toString should handle all zeros")
    void toStringShouldHandleAllZeros() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {0, 0, 0, 0});
      assertEquals("0.0.0.0", addr.toString(), "toString should format all zeros as 0.0.0.0");
    }

    @Test
    @DisplayName("toString should handle high byte values (unsigned)")
    void toStringShouldHandleHighByteValues() {
      final Ipv4Address addr =
          new Ipv4Address(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
      assertEquals(
          "255.255.255.255", addr.toString(), "toString should handle 0xFF bytes as unsigned 255");
    }
  }
}
