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
 * Tests for {@link Collector}.
 *
 * <p>Verifies enum structure, constants, toNativeCode/fromNativeCode, and round-trip
 * conversion.
 */
@DisplayName("Collector Tests")
class CollectorTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(Collector.class.isEnum(), "Collector should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactValueCount() {
      assertEquals(3, Collector.values().length,
          "Collector should have exactly 3 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain AUTO")
    void shouldContainAuto() {
      assertNotNull(Collector.AUTO, "AUTO constant should exist");
      assertEquals("AUTO", Collector.AUTO.name(), "AUTO name should match");
    }

    @Test
    @DisplayName("should contain DEFERRED_REFERENCE_COUNTING")
    void shouldContainDeferredReferenceCounting() {
      assertNotNull(Collector.DEFERRED_REFERENCE_COUNTING,
          "DEFERRED_REFERENCE_COUNTING constant should exist");
    }

    @Test
    @DisplayName("should contain NULL")
    void shouldContainNull() {
      assertNotNull(Collector.NULL, "NULL constant should exist");
    }
  }

  @Nested
  @DisplayName("ToNativeCode Tests")
  class ToNativeCodeTests {

    @Test
    @DisplayName("AUTO should have native code 0")
    void autoShouldHaveNativeCode0() {
      assertEquals(0, Collector.AUTO.toNativeCode(),
          "AUTO should have native code 0");
    }

    @Test
    @DisplayName("DEFERRED_REFERENCE_COUNTING should have native code 1")
    void deferredRcShouldHaveNativeCode1() {
      assertEquals(1, Collector.DEFERRED_REFERENCE_COUNTING.toNativeCode(),
          "DEFERRED_REFERENCE_COUNTING should have native code 1");
    }

    @Test
    @DisplayName("NULL should have native code 2")
    void nullShouldHaveNativeCode2() {
      assertEquals(2, Collector.NULL.toNativeCode(),
          "NULL should have native code 2");
    }

    @Test
    @DisplayName("should have unique native codes across all constants")
    void shouldHaveUniqueNativeCodes() {
      final Set<Integer> codes = new HashSet<>();
      for (final Collector value : Collector.values()) {
        codes.add(value.toNativeCode());
      }
      assertEquals(Collector.values().length, codes.size(),
          "All native codes should be unique");
    }
  }

  @Nested
  @DisplayName("FromNativeCode Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should return AUTO for code 0")
    void shouldReturnAutoForCode0() {
      assertEquals(Collector.AUTO, Collector.fromNativeCode(0),
          "fromNativeCode(0) should return AUTO");
    }

    @Test
    @DisplayName("should return DEFERRED_REFERENCE_COUNTING for code 1")
    void shouldReturnDeferredRcForCode1() {
      assertEquals(Collector.DEFERRED_REFERENCE_COUNTING, Collector.fromNativeCode(1),
          "fromNativeCode(1) should return DEFERRED_REFERENCE_COUNTING");
    }

    @Test
    @DisplayName("should return NULL for code 2")
    void shouldReturnNullForCode2() {
      assertEquals(Collector.NULL, Collector.fromNativeCode(2),
          "fromNativeCode(2) should return NULL");
    }

    @Test
    @DisplayName("should return AUTO for invalid negative code")
    void shouldReturnAutoForNegativeCode() {
      assertEquals(Collector.AUTO, Collector.fromNativeCode(-1),
          "fromNativeCode(-1) should return AUTO as fallback");
    }

    @Test
    @DisplayName("should return AUTO for invalid out-of-range code")
    void shouldReturnAutoForOutOfRangeCode() {
      assertEquals(Collector.AUTO, Collector.fromNativeCode(99),
          "fromNativeCode(99) should return AUTO as fallback");
    }

    @Test
    @DisplayName("should round-trip toNativeCode and fromNativeCode for all constants")
    void shouldRoundTripNativeCode() {
      for (final Collector value : Collector.values()) {
        assertEquals(value, Collector.fromNativeCode(value.toNativeCode()),
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
      for (final Collector value : Collector.values()) {
        assertEquals(value, Collector.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> Collector.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final Collector[] values = Collector.values();
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
    @DisplayName("should support switch statement over all values")
    void shouldSupportSwitchStatement() {
      for (final Collector collector : Collector.values()) {
        final String result;
        switch (collector) {
          case AUTO:
          case DEFERRED_REFERENCE_COUNTING:
          case NULL:
            result = collector.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(collector.name(), result,
            "Switch should handle " + collector.name());
      }
    }
  }
}
