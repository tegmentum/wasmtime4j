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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for IpAddressFamily enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, and switch statement coverage
 * for WASI socket IP address families.
 */
@DisplayName("IpAddressFamily Tests")
class IpAddressFamilyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 2 enum constants")
    void shouldHaveExactlyTwoEnumConstants() {
      final IpAddressFamily[] values = IpAddressFamily.values();

      assertEquals(2, values.length, "IpAddressFamily should have exactly 2 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(IpAddressFamily.class.isEnum(), "IpAddressFamily should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final IpAddressFamily family : IpAddressFamily.values()) {
        assertNotNull(family, "Every IpAddressFamily constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have IPV4 constant")
    void shouldHaveIpv4Constant() {
      final IpAddressFamily family = IpAddressFamily.IPV4;

      assertNotNull(family, "IPV4 should not be null");
      assertEquals("IPV4", family.name(), "Name should be IPV4");
    }

    @Test
    @DisplayName("should have IPV6 constant")
    void shouldHaveIpv6Constant() {
      final IpAddressFamily family = IpAddressFamily.IPV6;

      assertNotNull(family, "IPV6 should not be null");
      assertEquals("IPV6", family.name(), "Name should be IPV6");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return IPV4 for 'IPV4'")
    void valueOfShouldReturnIpv4() {
      assertEquals(
          IpAddressFamily.IPV4,
          IpAddressFamily.valueOf("IPV4"),
          "valueOf('IPV4') should return IPV4");
    }

    @Test
    @DisplayName("valueOf should return IPV6 for 'IPV6'")
    void valueOfShouldReturnIpv6() {
      assertEquals(
          IpAddressFamily.IPV6,
          IpAddressFamily.valueOf("IPV6"),
          "valueOf('IPV6') should return IPV6");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> IpAddressFamily.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> IpAddressFamily.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }

    @Test
    @DisplayName("valueOf should be case-sensitive")
    void valueOfShouldBeCaseSensitive() {
      assertThrows(
          IllegalArgumentException.class,
          () -> IpAddressFamily.valueOf("ipv4"),
          "valueOf('ipv4') should throw because enum names are case-sensitive");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 2")
    void valuesShouldReturnArrayOfLengthTwo() {
      assertEquals(
          2, IpAddressFamily.values().length, "values() should return array with 2 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<IpAddressFamily> valueSet = new HashSet<>(Arrays.asList(IpAddressFamily.values()));

      assertTrue(valueSet.contains(IpAddressFamily.IPV4), "values() should contain IPV4");
      assertTrue(valueSet.contains(IpAddressFamily.IPV6), "values() should contain IPV6");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final IpAddressFamily[] first = IpAddressFamily.values();
      final IpAddressFamily[] second = IpAddressFamily.values();

      assertTrue(first != second, "values() should return a new array instance each call");
      assertEquals(
          Arrays.asList(first),
          Arrays.asList(second),
          "values() arrays should have identical contents");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return 'IPV4' for IPV4")
    void toStringShouldReturnIpv4() {
      assertEquals("IPV4", IpAddressFamily.IPV4.toString(), "toString() should return 'IPV4'");
    }

    @Test
    @DisplayName("toString should return 'IPV6' for IPV6")
    void toStringShouldReturnIpv6() {
      assertEquals("IPV6", IpAddressFamily.IPV6.toString(), "toString() should return 'IPV6'");
    }

    @Test
    @DisplayName("toString should match name for all constants")
    void toStringShouldMatchNameForAllConstants() {
      for (final IpAddressFamily family : IpAddressFamily.values()) {
        assertEquals(
            family.name(),
            family.toString(),
            "toString() should match name() for " + family.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final IpAddressFamily family : IpAddressFamily.values()) {
        final String result;
        switch (family) {
          case IPV4:
            result = "ipv4";
            break;
          case IPV6:
            result = "ipv6";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList("ipv4", "ipv6").contains(result),
            "Switch should handle " + family + " but got: " + result);
      }
    }
  }
}
