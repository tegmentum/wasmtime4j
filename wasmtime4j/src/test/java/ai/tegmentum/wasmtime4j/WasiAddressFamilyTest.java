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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiAddressFamily enum.
 *
 * <p>WasiAddressFamily represents the address family for socket operations (IPv4 or IPv6).
 */
@DisplayName("WasiAddressFamily Enum Tests")
class WasiAddressFamilyTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiAddressFamily.class.isEnum(), "WasiAddressFamily should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiAddressFamily.class.getModifiers()),
          "WasiAddressFamily should be public");
    }
  }

  // ========================================================================
  // Enum Value Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have IPV4 value")
    void shouldHaveIpv4Value() {
      assertNotNull(WasiAddressFamily.valueOf("IPV4"), "IPV4 should exist");
    }

    @Test
    @DisplayName("should have IPV6 value")
    void shouldHaveIpv6Value() {
      assertNotNull(WasiAddressFamily.valueOf("IPV6"), "IPV6 should exist");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactly2Values() {
      assertEquals(2, WasiAddressFamily.values().length, "Should have exactly 2 address families");
    }

    @Test
    @DisplayName("IPV4 should be at ordinal 0")
    void ipv4ShouldBeOrdinal0() {
      assertEquals(0, WasiAddressFamily.IPV4.ordinal(), "IPV4 should be at ordinal 0");
    }

    @Test
    @DisplayName("IPV6 should be at ordinal 1")
    void ipv6ShouldBeOrdinal1() {
      assertEquals(1, WasiAddressFamily.IPV6.ordinal(), "IPV6 should be at ordinal 1");
    }
  }

  // ========================================================================
  // getValue Method Tests
  // ========================================================================

  @Nested
  @DisplayName("getValue Method Tests")
  class GetValueMethodTests {

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WasiAddressFamily.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("IPV4 getValue should return 0")
    void ipv4GetValueShouldReturn0() {
      assertEquals(0, WasiAddressFamily.IPV4.getValue(), "IPV4 getValue should return 0");
    }

    @Test
    @DisplayName("IPV6 getValue should return 1")
    void ipv6GetValueShouldReturn1() {
      assertEquals(1, WasiAddressFamily.IPV6.getValue(), "IPV6 getValue should return 1");
    }
  }

  // ========================================================================
  // fromValue Method Tests
  // ========================================================================

  @Nested
  @DisplayName("fromValue Method Tests")
  class FromValueMethodTests {

    @Test
    @DisplayName("should have fromValue static method")
    void shouldHaveFromValueMethod() throws NoSuchMethodException {
      Method method = WasiAddressFamily.class.getMethod("fromValue", int.class);
      assertNotNull(method, "fromValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiAddressFamily.class, method.getReturnType(), "Should return WasiAddressFamily");
    }

    @Test
    @DisplayName("fromValue(0) should return IPV4")
    void fromValue0ShouldReturnIpv4() {
      assertEquals(
          WasiAddressFamily.IPV4,
          WasiAddressFamily.fromValue(0),
          "fromValue(0) should return IPV4");
    }

    @Test
    @DisplayName("fromValue(1) should return IPV6")
    void fromValue1ShouldReturnIpv6() {
      assertEquals(
          WasiAddressFamily.IPV6,
          WasiAddressFamily.fromValue(1),
          "fromValue(1) should return IPV6");
    }

    @Test
    @DisplayName("fromValue should throw for unknown value")
    void fromValueShouldThrowForUnknownValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiAddressFamily.fromValue(99),
          "fromValue(99) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromValue should throw for negative value")
    void fromValueShouldThrowForNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiAddressFamily.fromValue(-1),
          "fromValue(-1) should throw IllegalArgumentException");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have value field")
    void shouldHaveValueField() throws NoSuchFieldException {
      java.lang.reflect.Field field = WasiAddressFamily.class.getDeclaredField("value");
      assertNotNull(field, "value field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          WasiAddressFamily.class.getDeclaredClasses().length,
          "WasiAddressFamily should have no nested classes");
    }
  }

  // ========================================================================
  // Roundtrip Tests
  // ========================================================================

  @Nested
  @DisplayName("Roundtrip Tests")
  class RoundtripTests {

    @Test
    @DisplayName("getValue and fromValue should roundtrip for all values")
    void getValueAndFromValueShouldRoundtrip() {
      for (WasiAddressFamily family : WasiAddressFamily.values()) {
        int value = family.getValue();
        WasiAddressFamily recovered = WasiAddressFamily.fromValue(value);
        assertEquals(family, recovered, "Roundtrip should preserve identity for " + family);
      }
    }
  }
}
