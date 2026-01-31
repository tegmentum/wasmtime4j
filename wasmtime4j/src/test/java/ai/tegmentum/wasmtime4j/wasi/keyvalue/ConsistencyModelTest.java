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
 * Tests for ConsistencyModel enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, and switch statement coverage for
 * WASI key-value consistency models.
 */
@DisplayName("ConsistencyModel Tests")
class ConsistencyModelTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 8 enum constants")
    void shouldHaveExactlyEightEnumConstants() {
      final ConsistencyModel[] values = ConsistencyModel.values();

      assertEquals(8, values.length, "ConsistencyModel should have exactly 8 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(
          ConsistencyModel.class.isEnum(), "ConsistencyModel should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final ConsistencyModel model : ConsistencyModel.values()) {
        assertNotNull(model, "Every ConsistencyModel constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have EVENTUAL constant")
    void shouldHaveEventualConstant() {
      final ConsistencyModel model = ConsistencyModel.EVENTUAL;

      assertNotNull(model, "EVENTUAL should not be null");
      assertEquals("EVENTUAL", model.name(), "Name should be EVENTUAL");
    }

    @Test
    @DisplayName("should have STRONG constant")
    void shouldHaveStrongConstant() {
      final ConsistencyModel model = ConsistencyModel.STRONG;

      assertNotNull(model, "STRONG should not be null");
      assertEquals("STRONG", model.name(), "Name should be STRONG");
    }

    @Test
    @DisplayName("should have CAUSAL constant")
    void shouldHaveCausalConstant() {
      final ConsistencyModel model = ConsistencyModel.CAUSAL;

      assertNotNull(model, "CAUSAL should not be null");
      assertEquals("CAUSAL", model.name(), "Name should be CAUSAL");
    }

    @Test
    @DisplayName("should have SEQUENTIAL constant")
    void shouldHaveSequentialConstant() {
      final ConsistencyModel model = ConsistencyModel.SEQUENTIAL;

      assertNotNull(model, "SEQUENTIAL should not be null");
      assertEquals("SEQUENTIAL", model.name(), "Name should be SEQUENTIAL");
    }

    @Test
    @DisplayName("should have LINEARIZABLE constant")
    void shouldHaveLinearizableConstant() {
      final ConsistencyModel model = ConsistencyModel.LINEARIZABLE;

      assertNotNull(model, "LINEARIZABLE should not be null");
      assertEquals("LINEARIZABLE", model.name(), "Name should be LINEARIZABLE");
    }

    @Test
    @DisplayName("should have SESSION constant")
    void shouldHaveSessionConstant() {
      final ConsistencyModel model = ConsistencyModel.SESSION;

      assertNotNull(model, "SESSION should not be null");
      assertEquals("SESSION", model.name(), "Name should be SESSION");
    }

    @Test
    @DisplayName("should have MONOTONIC_READ constant")
    void shouldHaveMonotonicReadConstant() {
      final ConsistencyModel model = ConsistencyModel.MONOTONIC_READ;

      assertNotNull(model, "MONOTONIC_READ should not be null");
      assertEquals("MONOTONIC_READ", model.name(), "Name should be MONOTONIC_READ");
    }

    @Test
    @DisplayName("should have MONOTONIC_WRITE constant")
    void shouldHaveMonotonicWriteConstant() {
      final ConsistencyModel model = ConsistencyModel.MONOTONIC_WRITE;

      assertNotNull(model, "MONOTONIC_WRITE should not be null");
      assertEquals("MONOTONIC_WRITE", model.name(), "Name should be MONOTONIC_WRITE");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("EVENTUAL should have ordinal 0")
    void eventualShouldHaveOrdinalZero() {
      assertEquals(0, ConsistencyModel.EVENTUAL.ordinal(), "EVENTUAL ordinal should be 0");
    }

    @Test
    @DisplayName("STRONG should have ordinal 1")
    void strongShouldHaveOrdinalOne() {
      assertEquals(1, ConsistencyModel.STRONG.ordinal(), "STRONG ordinal should be 1");
    }

    @Test
    @DisplayName("CAUSAL should have ordinal 2")
    void causalShouldHaveOrdinalTwo() {
      assertEquals(2, ConsistencyModel.CAUSAL.ordinal(), "CAUSAL ordinal should be 2");
    }

    @Test
    @DisplayName("SEQUENTIAL should have ordinal 3")
    void sequentialShouldHaveOrdinalThree() {
      assertEquals(
          3, ConsistencyModel.SEQUENTIAL.ordinal(), "SEQUENTIAL ordinal should be 3");
    }

    @Test
    @DisplayName("LINEARIZABLE should have ordinal 4")
    void linearizableShouldHaveOrdinalFour() {
      assertEquals(
          4, ConsistencyModel.LINEARIZABLE.ordinal(), "LINEARIZABLE ordinal should be 4");
    }

    @Test
    @DisplayName("SESSION should have ordinal 5")
    void sessionShouldHaveOrdinalFive() {
      assertEquals(5, ConsistencyModel.SESSION.ordinal(), "SESSION ordinal should be 5");
    }

    @Test
    @DisplayName("MONOTONIC_READ should have ordinal 6")
    void monotonicReadShouldHaveOrdinalSix() {
      assertEquals(
          6, ConsistencyModel.MONOTONIC_READ.ordinal(), "MONOTONIC_READ ordinal should be 6");
    }

    @Test
    @DisplayName("MONOTONIC_WRITE should have ordinal 7")
    void monotonicWriteShouldHaveOrdinalSeven() {
      assertEquals(
          7,
          ConsistencyModel.MONOTONIC_WRITE.ordinal(),
          "MONOTONIC_WRITE ordinal should be 7");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final ConsistencyModel[] values = ConsistencyModel.values();

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
          ConsistencyModel.EVENTUAL,
          ConsistencyModel.valueOf("EVENTUAL"),
          "valueOf('EVENTUAL') should return EVENTUAL");
      assertEquals(
          ConsistencyModel.STRONG,
          ConsistencyModel.valueOf("STRONG"),
          "valueOf('STRONG') should return STRONG");
      assertEquals(
          ConsistencyModel.CAUSAL,
          ConsistencyModel.valueOf("CAUSAL"),
          "valueOf('CAUSAL') should return CAUSAL");
      assertEquals(
          ConsistencyModel.SEQUENTIAL,
          ConsistencyModel.valueOf("SEQUENTIAL"),
          "valueOf('SEQUENTIAL') should return SEQUENTIAL");
      assertEquals(
          ConsistencyModel.LINEARIZABLE,
          ConsistencyModel.valueOf("LINEARIZABLE"),
          "valueOf('LINEARIZABLE') should return LINEARIZABLE");
      assertEquals(
          ConsistencyModel.SESSION,
          ConsistencyModel.valueOf("SESSION"),
          "valueOf('SESSION') should return SESSION");
      assertEquals(
          ConsistencyModel.MONOTONIC_READ,
          ConsistencyModel.valueOf("MONOTONIC_READ"),
          "valueOf('MONOTONIC_READ') should return MONOTONIC_READ");
      assertEquals(
          ConsistencyModel.MONOTONIC_WRITE,
          ConsistencyModel.valueOf("MONOTONIC_WRITE"),
          "valueOf('MONOTONIC_WRITE') should return MONOTONIC_WRITE");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ConsistencyModel.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> ConsistencyModel.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 8")
    void valuesShouldReturnArrayOfLengthEight() {
      assertEquals(
          8,
          ConsistencyModel.values().length,
          "values() should return array with 8 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<ConsistencyModel> valueSet =
          new HashSet<>(Arrays.asList(ConsistencyModel.values()));

      assertTrue(
          valueSet.contains(ConsistencyModel.EVENTUAL),
          "values() should contain EVENTUAL");
      assertTrue(
          valueSet.contains(ConsistencyModel.STRONG), "values() should contain STRONG");
      assertTrue(
          valueSet.contains(ConsistencyModel.CAUSAL), "values() should contain CAUSAL");
      assertTrue(
          valueSet.contains(ConsistencyModel.SEQUENTIAL),
          "values() should contain SEQUENTIAL");
      assertTrue(
          valueSet.contains(ConsistencyModel.LINEARIZABLE),
          "values() should contain LINEARIZABLE");
      assertTrue(
          valueSet.contains(ConsistencyModel.SESSION), "values() should contain SESSION");
      assertTrue(
          valueSet.contains(ConsistencyModel.MONOTONIC_READ),
          "values() should contain MONOTONIC_READ");
      assertTrue(
          valueSet.contains(ConsistencyModel.MONOTONIC_WRITE),
          "values() should contain MONOTONIC_WRITE");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final ConsistencyModel[] first = ConsistencyModel.values();
      final ConsistencyModel[] second = ConsistencyModel.values();

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
      for (final ConsistencyModel model : ConsistencyModel.values()) {
        assertEquals(
            model.name(),
            model.toString(),
            "toString() should match name() for " + model.name());
      }
    }

    @Test
    @DisplayName("toString should return 'EVENTUAL' for EVENTUAL")
    void toStringShouldReturnEventual() {
      assertEquals(
          "EVENTUAL",
          ConsistencyModel.EVENTUAL.toString(),
          "toString() should return 'EVENTUAL'");
    }

    @Test
    @DisplayName("toString should return 'LINEARIZABLE' for LINEARIZABLE")
    void toStringShouldReturnLinearizable() {
      assertEquals(
          "LINEARIZABLE",
          ConsistencyModel.LINEARIZABLE.toString(),
          "toString() should return 'LINEARIZABLE'");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final ConsistencyModel model : ConsistencyModel.values()) {
        final String result;
        switch (model) {
          case EVENTUAL:
            result = "eventual";
            break;
          case STRONG:
            result = "strong";
            break;
          case CAUSAL:
            result = "causal";
            break;
          case SEQUENTIAL:
            result = "sequential";
            break;
          case LINEARIZABLE:
            result = "linearizable";
            break;
          case SESSION:
            result = "session";
            break;
          case MONOTONIC_READ:
            result = "monotonic_read";
            break;
          case MONOTONIC_WRITE:
            result = "monotonic_write";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList(
                    "eventual",
                    "strong",
                    "causal",
                    "sequential",
                    "linearizable",
                    "session",
                    "monotonic_read",
                    "monotonic_write")
                .contains(result),
            "Switch should handle " + model + " but got: " + result);
      }
    }
  }
}
