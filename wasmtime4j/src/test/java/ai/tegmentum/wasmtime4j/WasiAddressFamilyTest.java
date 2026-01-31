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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiAddressFamily}.
 *
 * <p>Verifies enum structure, constants, getValue/fromValue, and round-trip conversion.
 */
@DisplayName("WasiAddressFamily Tests")
class WasiAddressFamilyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(WasiAddressFamily.class.isEnum(),
          "WasiAddressFamily should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactValueCount() {
      assertEquals(2, WasiAddressFamily.values().length,
          "WasiAddressFamily should have exactly 2 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain IPV4")
    void shouldContainIpv4() {
      assertNotNull(WasiAddressFamily.IPV4, "IPV4 constant should exist");
      assertEquals("IPV4", WasiAddressFamily.IPV4.name(), "IPV4 name should match");
    }

    @Test
    @DisplayName("should contain IPV6")
    void shouldContainIpv6() {
      assertNotNull(WasiAddressFamily.IPV6, "IPV6 constant should exist");
      assertEquals("IPV6", WasiAddressFamily.IPV6.name(), "IPV6 name should match");
    }
  }

  @Nested
  @DisplayName("GetValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("IPV4 should have value 0")
    void ipv4ShouldHaveValue0() {
      assertEquals(0, WasiAddressFamily.IPV4.getValue(),
          "IPV4 should have value 0");
    }

    @Test
    @DisplayName("IPV6 should have value 1")
    void ipv6ShouldHaveValue1() {
      assertEquals(1, WasiAddressFamily.IPV6.getValue(),
          "IPV6 should have value 1");
    }

    @Test
    @DisplayName("should have unique values across all constants")
    void shouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (final WasiAddressFamily value : WasiAddressFamily.values()) {
        values.add(value.getValue());
      }
      assertEquals(WasiAddressFamily.values().length, values.size(),
          "All values should be unique");
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("should return IPV4 for value 0")
    void shouldReturnIpv4ForValue0() {
      assertEquals(WasiAddressFamily.IPV4, WasiAddressFamily.fromValue(0),
          "fromValue(0) should return IPV4");
    }

    @Test
    @DisplayName("should return IPV6 for value 1")
    void shouldReturnIpv6ForValue1() {
      assertEquals(WasiAddressFamily.IPV6, WasiAddressFamily.fromValue(1),
          "fromValue(1) should return IPV6");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative value")
    void shouldThrowForNegativeValue() {
      assertThrows(IllegalArgumentException.class,
          () -> WasiAddressFamily.fromValue(-1),
          "fromValue(-1) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for out-of-range value")
    void shouldThrowForOutOfRangeValue() {
      assertThrows(IllegalArgumentException.class,
          () -> WasiAddressFamily.fromValue(99),
          "fromValue(99) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should round-trip getValue and fromValue for all constants")
    void shouldRoundTripGetValueAndFromValue() {
      for (final WasiAddressFamily value : WasiAddressFamily.values()) {
        assertEquals(value, WasiAddressFamily.fromValue(value.getValue()),
            "Round-trip should return original for " + value.name());
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final WasiAddressFamily value : WasiAddressFamily.values()) {
        assertEquals(value, WasiAddressFamily.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> WasiAddressFamily.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final WasiAddressFamily[] values = WasiAddressFamily.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support native FFI code round-trip via int array")
    void shouldSupportNativeFfiCodeRoundTrip() {
      final int[] nativeCodes = new int[WasiAddressFamily.values().length];
      for (int i = 0; i < WasiAddressFamily.values().length; i++) {
        nativeCodes[i] = WasiAddressFamily.values()[i].getValue();
      }
      for (int i = 0; i < nativeCodes.length; i++) {
        final WasiAddressFamily resolved = WasiAddressFamily.fromValue(nativeCodes[i]);
        assertEquals(WasiAddressFamily.values()[i], resolved,
            "FFI round-trip should preserve enum at index " + i);
      }
    }
  }
}
