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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link RuntimeType} enum.
 *
 * <p>This test class verifies the RuntimeType enum which identifies the underlying technology used
 * to provide WebAssembly functionality.
 */
@DisplayName("RuntimeType Tests")
class RuntimeTypeTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("Should have JNI value")
    void shouldHaveJniValue() {
      assertNotNull(RuntimeType.valueOf("JNI"), "Should have JNI value");
    }

    @Test
    @DisplayName("Should have PANAMA value")
    void shouldHavePanamaValue() {
      assertNotNull(RuntimeType.valueOf("PANAMA"), "Should have PANAMA value");
    }

    @Test
    @DisplayName("Should have exactly 2 values")
    void shouldHaveExactly2Values() {
      assertEquals(2, RuntimeType.values().length, "Should have exactly 2 runtime types");
    }

    @Test
    @DisplayName("JNI should be at ordinal 0")
    void jniShouldBeAtOrdinal0() {
      assertEquals(0, RuntimeType.JNI.ordinal(), "JNI should be at ordinal 0");
    }

    @Test
    @DisplayName("PANAMA should be at ordinal 1")
    void panamaShouldBeAtOrdinal1() {
      assertEquals(1, RuntimeType.PANAMA.ordinal(), "PANAMA should be at ordinal 1");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return JNI for 'JNI'")
    void valueOfShouldReturnJniForJni() {
      assertEquals(RuntimeType.JNI, RuntimeType.valueOf("JNI"), "valueOf('JNI') should return JNI");
    }

    @Test
    @DisplayName("valueOf should return PANAMA for 'PANAMA'")
    void valueOfShouldReturnPanamaForPanama() {
      assertEquals(
          RuntimeType.PANAMA,
          RuntimeType.valueOf("PANAMA"),
          "valueOf('PANAMA') should return PANAMA");
    }

    @Test
    @DisplayName("valueOf should throw for invalid value")
    void valueOfShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RuntimeType.valueOf("INVALID"),
          "valueOf should throw for invalid value");
    }

    @Test
    @DisplayName("valueOf should throw for lowercase 'jni'")
    void valueOfShouldThrowForLowercaseJni() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RuntimeType.valueOf("jni"),
          "valueOf should throw for lowercase 'jni'");
    }

    @Test
    @DisplayName("valueOf should throw for lowercase 'panama'")
    void valueOfShouldThrowForLowercasePanama() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RuntimeType.valueOf("panama"),
          "valueOf should throw for lowercase 'panama'");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null")
    void valueOfShouldThrowNpeForNull() {
      assertThrows(
          NullPointerException.class,
          () -> RuntimeType.valueOf(null),
          "valueOf should throw NPE for null");
    }
  }

  @Nested
  @DisplayName("name Tests")
  class NameTests {

    @Test
    @DisplayName("JNI name should be 'JNI'")
    void jniNameShouldBeJni() {
      assertEquals("JNI", RuntimeType.JNI.name(), "JNI name should be 'JNI'");
    }

    @Test
    @DisplayName("PANAMA name should be 'PANAMA'")
    void panamaNameShouldBePanama() {
      assertEquals("PANAMA", RuntimeType.PANAMA.name(), "PANAMA name should be 'PANAMA'");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("JNI toString should return 'JNI'")
    void jniToStringShouldReturnJni() {
      assertEquals("JNI", RuntimeType.JNI.toString(), "JNI toString should return 'JNI'");
    }

    @Test
    @DisplayName("PANAMA toString should return 'PANAMA'")
    void panamaToStringShouldReturnPanama() {
      assertEquals(
          "PANAMA", RuntimeType.PANAMA.toString(), "PANAMA toString should return 'PANAMA'");
    }
  }

  @Nested
  @DisplayName("values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array with JNI and PANAMA")
    void valuesShouldReturnArrayWithJniAndPanama() {
      final RuntimeType[] values = RuntimeType.values();

      assertEquals(2, values.length, "Should have 2 values");
      assertEquals(RuntimeType.JNI, values[0], "First value should be JNI");
      assertEquals(RuntimeType.PANAMA, values[1], "Second value should be PANAMA");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final RuntimeType[] values1 = RuntimeType.values();
      final RuntimeType[] values2 = RuntimeType.values();

      assertTrue(values1 != values2, "values() should return new array each call");
      assertEquals(values1.length, values2.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final RuntimeType type = RuntimeType.JNI;
      String result;

      switch (type) {
        case JNI:
          result = "Using JNI bindings";
          break;
        case PANAMA:
          result = "Using Panama FFI";
          break;
        default:
          result = "Unknown";
      }

      assertEquals("Using JNI bindings", result, "Switch should work with JNI");
    }

    @Test
    @DisplayName("Should be comparable using equals")
    void shouldBeComparableUsingEquals() {
      final RuntimeType type1 = RuntimeType.JNI;
      final RuntimeType type2 = RuntimeType.JNI;
      final RuntimeType type3 = RuntimeType.PANAMA;

      assertEquals(type1, type2, "Same enum values should be equal");
      assertTrue(!type1.equals(type3), "Different enum values should not be equal");
    }

    @Test
    @DisplayName("Should be usable with compareTo")
    void shouldBeUsableWithCompareTo() {
      assertTrue(
          RuntimeType.JNI.compareTo(RuntimeType.PANAMA) < 0,
          "JNI should be less than PANAMA by ordinal");
      assertTrue(
          RuntimeType.PANAMA.compareTo(RuntimeType.JNI) > 0,
          "PANAMA should be greater than JNI by ordinal");
      assertEquals(
          0, RuntimeType.JNI.compareTo(RuntimeType.JNI), "Same value should compare equal");
    }
  }
}
