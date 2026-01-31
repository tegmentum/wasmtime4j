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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

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
 * Tests for EvictionPolicy enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, and switch statement coverage for
 * WASI key-value cache eviction policies.
 */
@DisplayName("EvictionPolicy Tests")
class EvictionPolicyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 7 enum constants")
    void shouldHaveExactlySevenEnumConstants() {
      final EvictionPolicy[] values = EvictionPolicy.values();

      assertEquals(7, values.length, "EvictionPolicy should have exactly 7 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(EvictionPolicy.class.isEnum(), "EvictionPolicy should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final EvictionPolicy policy : EvictionPolicy.values()) {
        assertNotNull(policy, "Every EvictionPolicy constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have LRU constant")
    void shouldHaveLruConstant() {
      final EvictionPolicy policy = EvictionPolicy.LRU;

      assertNotNull(policy, "LRU should not be null");
      assertEquals("LRU", policy.name(), "Name should be LRU");
    }

    @Test
    @DisplayName("should have LFU constant")
    void shouldHaveLfuConstant() {
      final EvictionPolicy policy = EvictionPolicy.LFU;

      assertNotNull(policy, "LFU should not be null");
      assertEquals("LFU", policy.name(), "Name should be LFU");
    }

    @Test
    @DisplayName("should have FIFO constant")
    void shouldHaveFifoConstant() {
      final EvictionPolicy policy = EvictionPolicy.FIFO;

      assertNotNull(policy, "FIFO should not be null");
      assertEquals("FIFO", policy.name(), "Name should be FIFO");
    }

    @Test
    @DisplayName("should have TTL constant")
    void shouldHaveTtlConstant() {
      final EvictionPolicy policy = EvictionPolicy.TTL;

      assertNotNull(policy, "TTL should not be null");
      assertEquals("TTL", policy.name(), "Name should be TTL");
    }

    @Test
    @DisplayName("should have SIZE_BASED constant")
    void shouldHaveSizeBasedConstant() {
      final EvictionPolicy policy = EvictionPolicy.SIZE_BASED;

      assertNotNull(policy, "SIZE_BASED should not be null");
      assertEquals("SIZE_BASED", policy.name(), "Name should be SIZE_BASED");
    }

    @Test
    @DisplayName("should have RANDOM constant")
    void shouldHaveRandomConstant() {
      final EvictionPolicy policy = EvictionPolicy.RANDOM;

      assertNotNull(policy, "RANDOM should not be null");
      assertEquals("RANDOM", policy.name(), "Name should be RANDOM");
    }

    @Test
    @DisplayName("should have NONE constant")
    void shouldHaveNoneConstant() {
      final EvictionPolicy policy = EvictionPolicy.NONE;

      assertNotNull(policy, "NONE should not be null");
      assertEquals("NONE", policy.name(), "Name should be NONE");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("LRU should have ordinal 0")
    void lruShouldHaveOrdinalZero() {
      assertEquals(0, EvictionPolicy.LRU.ordinal(), "LRU ordinal should be 0");
    }

    @Test
    @DisplayName("LFU should have ordinal 1")
    void lfuShouldHaveOrdinalOne() {
      assertEquals(1, EvictionPolicy.LFU.ordinal(), "LFU ordinal should be 1");
    }

    @Test
    @DisplayName("FIFO should have ordinal 2")
    void fifoShouldHaveOrdinalTwo() {
      assertEquals(2, EvictionPolicy.FIFO.ordinal(), "FIFO ordinal should be 2");
    }

    @Test
    @DisplayName("TTL should have ordinal 3")
    void ttlShouldHaveOrdinalThree() {
      assertEquals(3, EvictionPolicy.TTL.ordinal(), "TTL ordinal should be 3");
    }

    @Test
    @DisplayName("SIZE_BASED should have ordinal 4")
    void sizeBasedShouldHaveOrdinalFour() {
      assertEquals(4, EvictionPolicy.SIZE_BASED.ordinal(), "SIZE_BASED ordinal should be 4");
    }

    @Test
    @DisplayName("RANDOM should have ordinal 5")
    void randomShouldHaveOrdinalFive() {
      assertEquals(5, EvictionPolicy.RANDOM.ordinal(), "RANDOM ordinal should be 5");
    }

    @Test
    @DisplayName("NONE should have ordinal 6")
    void noneShouldHaveOrdinalSix() {
      assertEquals(6, EvictionPolicy.NONE.ordinal(), "NONE ordinal should be 6");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final EvictionPolicy[] values = EvictionPolicy.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(
            i, values[i].ordinal(), "Ordinal should be " + i + " for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          EvictionPolicy.LRU,
          EvictionPolicy.valueOf("LRU"),
          "valueOf('LRU') should return LRU");
      assertEquals(
          EvictionPolicy.LFU,
          EvictionPolicy.valueOf("LFU"),
          "valueOf('LFU') should return LFU");
      assertEquals(
          EvictionPolicy.FIFO,
          EvictionPolicy.valueOf("FIFO"),
          "valueOf('FIFO') should return FIFO");
      assertEquals(
          EvictionPolicy.TTL,
          EvictionPolicy.valueOf("TTL"),
          "valueOf('TTL') should return TTL");
      assertEquals(
          EvictionPolicy.SIZE_BASED,
          EvictionPolicy.valueOf("SIZE_BASED"),
          "valueOf('SIZE_BASED') should return SIZE_BASED");
      assertEquals(
          EvictionPolicy.RANDOM,
          EvictionPolicy.valueOf("RANDOM"),
          "valueOf('RANDOM') should return RANDOM");
      assertEquals(
          EvictionPolicy.NONE,
          EvictionPolicy.valueOf("NONE"),
          "valueOf('NONE') should return NONE");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> EvictionPolicy.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> EvictionPolicy.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 7")
    void valuesShouldReturnArrayOfLengthSeven() {
      assertEquals(
          7,
          EvictionPolicy.values().length,
          "values() should return array with 7 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<EvictionPolicy> valueSet =
          new HashSet<>(Arrays.asList(EvictionPolicy.values()));

      assertTrue(valueSet.contains(EvictionPolicy.LRU), "values() should contain LRU");
      assertTrue(valueSet.contains(EvictionPolicy.LFU), "values() should contain LFU");
      assertTrue(valueSet.contains(EvictionPolicy.FIFO), "values() should contain FIFO");
      assertTrue(valueSet.contains(EvictionPolicy.TTL), "values() should contain TTL");
      assertTrue(
          valueSet.contains(EvictionPolicy.SIZE_BASED),
          "values() should contain SIZE_BASED");
      assertTrue(valueSet.contains(EvictionPolicy.RANDOM), "values() should contain RANDOM");
      assertTrue(valueSet.contains(EvictionPolicy.NONE), "values() should contain NONE");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final EvictionPolicy[] first = EvictionPolicy.values();
      final EvictionPolicy[] second = EvictionPolicy.values();

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
    @DisplayName("toString should match name for all constants")
    void toStringShouldMatchNameForAllConstants() {
      for (final EvictionPolicy policy : EvictionPolicy.values()) {
        assertEquals(
            policy.name(),
            policy.toString(),
            "toString() should match name() for " + policy.name());
      }
    }

    @Test
    @DisplayName("toString should return 'LRU' for LRU")
    void toStringShouldReturnLru() {
      assertEquals(
          "LRU", EvictionPolicy.LRU.toString(), "toString() should return 'LRU'");
    }

    @Test
    @DisplayName("toString should return 'NONE' for NONE")
    void toStringShouldReturnNone() {
      assertEquals(
          "NONE", EvictionPolicy.NONE.toString(), "toString() should return 'NONE'");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final EvictionPolicy policy : EvictionPolicy.values()) {
        final String result;
        switch (policy) {
          case LRU:
            result = "lru";
            break;
          case LFU:
            result = "lfu";
            break;
          case FIFO:
            result = "fifo";
            break;
          case TTL:
            result = "ttl";
            break;
          case SIZE_BASED:
            result = "size_based";
            break;
          case RANDOM:
            result = "random";
            break;
          case NONE:
            result = "none";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList("lru", "lfu", "fifo", "ttl", "size_based", "random", "none")
                .contains(result),
            "Switch should handle " + policy + " but got: " + result);
      }
    }
  }
}
