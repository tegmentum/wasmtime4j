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
 * Tests for IsolationLevel enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, and switch statement coverage for
 * WASI key-value transaction isolation levels.
 */
@DisplayName("IsolationLevel Tests")
class IsolationLevelTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 5 enum constants")
    void shouldHaveExactlyFiveEnumConstants() {
      final IsolationLevel[] values = IsolationLevel.values();

      assertEquals(5, values.length, "IsolationLevel should have exactly 5 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(IsolationLevel.class.isEnum(), "IsolationLevel should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final IsolationLevel level : IsolationLevel.values()) {
        assertNotNull(level, "Every IsolationLevel constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have READ_UNCOMMITTED constant")
    void shouldHaveReadUncommittedConstant() {
      final IsolationLevel level = IsolationLevel.READ_UNCOMMITTED;

      assertNotNull(level, "READ_UNCOMMITTED should not be null");
      assertEquals("READ_UNCOMMITTED", level.name(), "Name should be READ_UNCOMMITTED");
    }

    @Test
    @DisplayName("should have READ_COMMITTED constant")
    void shouldHaveReadCommittedConstant() {
      final IsolationLevel level = IsolationLevel.READ_COMMITTED;

      assertNotNull(level, "READ_COMMITTED should not be null");
      assertEquals("READ_COMMITTED", level.name(), "Name should be READ_COMMITTED");
    }

    @Test
    @DisplayName("should have REPEATABLE_READ constant")
    void shouldHaveRepeatableReadConstant() {
      final IsolationLevel level = IsolationLevel.REPEATABLE_READ;

      assertNotNull(level, "REPEATABLE_READ should not be null");
      assertEquals("REPEATABLE_READ", level.name(), "Name should be REPEATABLE_READ");
    }

    @Test
    @DisplayName("should have SERIALIZABLE constant")
    void shouldHaveSerializableConstant() {
      final IsolationLevel level = IsolationLevel.SERIALIZABLE;

      assertNotNull(level, "SERIALIZABLE should not be null");
      assertEquals("SERIALIZABLE", level.name(), "Name should be SERIALIZABLE");
    }

    @Test
    @DisplayName("should have SNAPSHOT constant")
    void shouldHaveSnapshotConstant() {
      final IsolationLevel level = IsolationLevel.SNAPSHOT;

      assertNotNull(level, "SNAPSHOT should not be null");
      assertEquals("SNAPSHOT", level.name(), "Name should be SNAPSHOT");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("READ_UNCOMMITTED should have ordinal 0")
    void readUncommittedShouldHaveOrdinalZero() {
      assertEquals(
          0, IsolationLevel.READ_UNCOMMITTED.ordinal(), "READ_UNCOMMITTED ordinal should be 0");
    }

    @Test
    @DisplayName("READ_COMMITTED should have ordinal 1")
    void readCommittedShouldHaveOrdinalOne() {
      assertEquals(
          1, IsolationLevel.READ_COMMITTED.ordinal(), "READ_COMMITTED ordinal should be 1");
    }

    @Test
    @DisplayName("REPEATABLE_READ should have ordinal 2")
    void repeatableReadShouldHaveOrdinalTwo() {
      assertEquals(
          2, IsolationLevel.REPEATABLE_READ.ordinal(), "REPEATABLE_READ ordinal should be 2");
    }

    @Test
    @DisplayName("SERIALIZABLE should have ordinal 3")
    void serializableShouldHaveOrdinalThree() {
      assertEquals(
          3, IsolationLevel.SERIALIZABLE.ordinal(), "SERIALIZABLE ordinal should be 3");
    }

    @Test
    @DisplayName("SNAPSHOT should have ordinal 4")
    void snapshotShouldHaveOrdinalFour() {
      assertEquals(4, IsolationLevel.SNAPSHOT.ordinal(), "SNAPSHOT ordinal should be 4");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final IsolationLevel[] values = IsolationLevel.values();

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
          IsolationLevel.READ_UNCOMMITTED,
          IsolationLevel.valueOf("READ_UNCOMMITTED"),
          "valueOf('READ_UNCOMMITTED') should return READ_UNCOMMITTED");
      assertEquals(
          IsolationLevel.READ_COMMITTED,
          IsolationLevel.valueOf("READ_COMMITTED"),
          "valueOf('READ_COMMITTED') should return READ_COMMITTED");
      assertEquals(
          IsolationLevel.REPEATABLE_READ,
          IsolationLevel.valueOf("REPEATABLE_READ"),
          "valueOf('REPEATABLE_READ') should return REPEATABLE_READ");
      assertEquals(
          IsolationLevel.SERIALIZABLE,
          IsolationLevel.valueOf("SERIALIZABLE"),
          "valueOf('SERIALIZABLE') should return SERIALIZABLE");
      assertEquals(
          IsolationLevel.SNAPSHOT,
          IsolationLevel.valueOf("SNAPSHOT"),
          "valueOf('SNAPSHOT') should return SNAPSHOT");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> IsolationLevel.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> IsolationLevel.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 5")
    void valuesShouldReturnArrayOfLengthFive() {
      assertEquals(
          5,
          IsolationLevel.values().length,
          "values() should return array with 5 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<IsolationLevel> valueSet =
          new HashSet<>(Arrays.asList(IsolationLevel.values()));

      assertTrue(
          valueSet.contains(IsolationLevel.READ_UNCOMMITTED),
          "values() should contain READ_UNCOMMITTED");
      assertTrue(
          valueSet.contains(IsolationLevel.READ_COMMITTED),
          "values() should contain READ_COMMITTED");
      assertTrue(
          valueSet.contains(IsolationLevel.REPEATABLE_READ),
          "values() should contain REPEATABLE_READ");
      assertTrue(
          valueSet.contains(IsolationLevel.SERIALIZABLE),
          "values() should contain SERIALIZABLE");
      assertTrue(
          valueSet.contains(IsolationLevel.SNAPSHOT), "values() should contain SNAPSHOT");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final IsolationLevel[] first = IsolationLevel.values();
      final IsolationLevel[] second = IsolationLevel.values();

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
      for (final IsolationLevel level : IsolationLevel.values()) {
        assertEquals(
            level.name(),
            level.toString(),
            "toString() should match name() for " + level.name());
      }
    }

    @Test
    @DisplayName("toString should return 'READ_UNCOMMITTED' for READ_UNCOMMITTED")
    void toStringShouldReturnReadUncommitted() {
      assertEquals(
          "READ_UNCOMMITTED",
          IsolationLevel.READ_UNCOMMITTED.toString(),
          "toString() should return 'READ_UNCOMMITTED'");
    }

    @Test
    @DisplayName("toString should return 'SNAPSHOT' for SNAPSHOT")
    void toStringShouldReturnSnapshot() {
      assertEquals(
          "SNAPSHOT",
          IsolationLevel.SNAPSHOT.toString(),
          "toString() should return 'SNAPSHOT'");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final IsolationLevel level : IsolationLevel.values()) {
        final String result;
        switch (level) {
          case READ_UNCOMMITTED:
            result = "read_uncommitted";
            break;
          case READ_COMMITTED:
            result = "read_committed";
            break;
          case REPEATABLE_READ:
            result = "repeatable_read";
            break;
          case SERIALIZABLE:
            result = "serializable";
            break;
          case SNAPSHOT:
            result = "snapshot";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList(
                    "read_uncommitted",
                    "read_committed",
                    "repeatable_read",
                    "serializable",
                    "snapshot")
                .contains(result),
            "Switch should handle " + level + " but got: " + result);
      }
    }
  }
}
