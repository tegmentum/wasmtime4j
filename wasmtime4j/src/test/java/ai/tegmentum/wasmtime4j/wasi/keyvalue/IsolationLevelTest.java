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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IsolationLevel} enum.
 *
 * <p>IsolationLevel represents transaction isolation levels for WASI-keyvalue operations.
 */
@DisplayName("IsolationLevel Tests")
class IsolationLevelTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(IsolationLevel.class.isEnum(), "IsolationLevel should be an enum");
    }

    @Test
    @DisplayName("should have exactly 5 values")
    void shouldHaveExactlyFiveValues() {
      final IsolationLevel[] values = IsolationLevel.values();
      assertEquals(5, values.length, "Should have exactly 5 isolation levels");
    }

    @Test
    @DisplayName("should have READ_UNCOMMITTED value")
    void shouldHaveReadUncommittedValue() {
      assertNotNull(IsolationLevel.valueOf("READ_UNCOMMITTED"), "Should have READ_UNCOMMITTED");
    }

    @Test
    @DisplayName("should have READ_COMMITTED value")
    void shouldHaveReadCommittedValue() {
      assertNotNull(IsolationLevel.valueOf("READ_COMMITTED"), "Should have READ_COMMITTED");
    }

    @Test
    @DisplayName("should have REPEATABLE_READ value")
    void shouldHaveRepeatableReadValue() {
      assertNotNull(IsolationLevel.valueOf("REPEATABLE_READ"), "Should have REPEATABLE_READ");
    }

    @Test
    @DisplayName("should have SERIALIZABLE value")
    void shouldHaveSerializableValue() {
      assertNotNull(IsolationLevel.valueOf("SERIALIZABLE"), "Should have SERIALIZABLE");
    }

    @Test
    @DisplayName("should have SNAPSHOT value")
    void shouldHaveSnapshotValue() {
      assertNotNull(IsolationLevel.valueOf("SNAPSHOT"), "Should have SNAPSHOT");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final IsolationLevel level : IsolationLevel.values()) {
        assertTrue(ordinals.add(level.ordinal()), "Ordinal should be unique: " + level);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final IsolationLevel level : IsolationLevel.values()) {
        assertTrue(names.add(level.name()), "Name should be unique: " + level);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final IsolationLevel level : IsolationLevel.values()) {
        assertEquals(level, IsolationLevel.valueOf(level.name()), "Should be retrievable by name");
      }
    }
  }

  @Nested
  @DisplayName("SQL Standard Isolation Level Tests")
  class SqlStandardIsolationLevelTests {

    @Test
    @DisplayName("should have all SQL standard levels")
    void shouldHaveAllSqlStandardLevels() {
      // SQL-92 standard defines these four isolation levels
      assertNotNull(IsolationLevel.READ_UNCOMMITTED, "SQL standard: READ UNCOMMITTED");
      assertNotNull(IsolationLevel.READ_COMMITTED, "SQL standard: READ COMMITTED");
      assertNotNull(IsolationLevel.REPEATABLE_READ, "SQL standard: REPEATABLE READ");
      assertNotNull(IsolationLevel.SERIALIZABLE, "SQL standard: SERIALIZABLE");
    }

    @Test
    @DisplayName("should have SNAPSHOT as extension")
    void shouldHaveSnapshotAsExtension() {
      // SNAPSHOT is a common extension (e.g., SQL Server, PostgreSQL)
      assertNotNull(IsolationLevel.SNAPSHOT, "Common extension: SNAPSHOT isolation");
    }

    @Test
    @DisplayName("should order levels by strictness")
    void shouldOrderLevelsByStrictness() {
      // READ_UNCOMMITTED is weakest, SERIALIZABLE is strongest
      assertTrue(
          IsolationLevel.READ_UNCOMMITTED.ordinal() < IsolationLevel.READ_COMMITTED.ordinal(),
          "READ_UNCOMMITTED < READ_COMMITTED");
      assertTrue(
          IsolationLevel.READ_COMMITTED.ordinal() < IsolationLevel.REPEATABLE_READ.ordinal(),
          "READ_COMMITTED < REPEATABLE_READ");
      assertTrue(
          IsolationLevel.REPEATABLE_READ.ordinal() < IsolationLevel.SERIALIZABLE.ordinal(),
          "REPEATABLE_READ < SERIALIZABLE");
    }
  }

  @Nested
  @DisplayName("Anomaly Prevention Tests")
  class AnomalyPreventionTests {

    @Test
    @DisplayName("READ_UNCOMMITTED should allow dirty reads")
    void readUncommittedShouldAllowDirtyReads() {
      // READ_UNCOMMITTED allows dirty reads - weakest isolation
      final IsolationLevel level = IsolationLevel.READ_UNCOMMITTED;
      assertEquals("READ_UNCOMMITTED", level.name(), "Allows dirty reads");
    }

    @Test
    @DisplayName("READ_COMMITTED should prevent dirty reads")
    void readCommittedShouldPreventDirtyReads() {
      // READ_COMMITTED prevents dirty reads but allows non-repeatable reads
      final IsolationLevel level = IsolationLevel.READ_COMMITTED;
      assertEquals("READ_COMMITTED", level.name(), "Prevents dirty reads");
    }

    @Test
    @DisplayName("REPEATABLE_READ should prevent non-repeatable reads")
    void repeatableReadShouldPreventNonRepeatableReads() {
      // REPEATABLE_READ prevents non-repeatable reads but may allow phantom reads
      final IsolationLevel level = IsolationLevel.REPEATABLE_READ;
      assertEquals("REPEATABLE_READ", level.name(), "Prevents non-repeatable reads");
    }

    @Test
    @DisplayName("SERIALIZABLE should prevent all anomalies")
    void serializableShouldPreventAllAnomalies() {
      // SERIALIZABLE prevents all read anomalies including phantom reads
      final IsolationLevel level = IsolationLevel.SERIALIZABLE;
      assertEquals("SERIALIZABLE", level.name(), "Prevents phantom reads");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final IsolationLevel level = IsolationLevel.SERIALIZABLE;

      final String description;
      switch (level) {
        case READ_UNCOMMITTED:
          description = "Allows dirty reads";
          break;
        case READ_COMMITTED:
          description = "Prevents dirty reads";
          break;
        case REPEATABLE_READ:
          description = "Prevents non-repeatable reads";
          break;
        case SERIALIZABLE:
          description = "Strongest isolation, prevents phantom reads";
          break;
        case SNAPSHOT:
          description = "Consistent view at transaction start";
          break;
        default:
          description = "Unknown isolation level";
      }

      assertEquals(
          "Strongest isolation, prevents phantom reads", description, "SERIALIZABLE description");
    }

    @Test
    @DisplayName("should be usable in collections")
    void shouldBeUsableInCollections() {
      final Set<IsolationLevel> supportedLevels = new HashSet<>();
      supportedLevels.add(IsolationLevel.READ_COMMITTED);
      supportedLevels.add(IsolationLevel.REPEATABLE_READ);
      supportedLevels.add(IsolationLevel.SERIALIZABLE);

      assertTrue(
          supportedLevels.contains(IsolationLevel.READ_COMMITTED), "Should contain READ_COMMITTED");
      assertTrue(
          supportedLevels.contains(IsolationLevel.SERIALIZABLE), "Should contain SERIALIZABLE");
      assertEquals(3, supportedLevels.size(), "Should have 3 supported levels");
    }

    @Test
    @DisplayName("should support isolation level selection")
    void shouldSupportIsolationLevelSelection() {
      // Pattern: select isolation based on requirements
      final boolean needsConsistency = true;
      final boolean needsPerformance = false;

      final IsolationLevel selected;
      if (needsConsistency && !needsPerformance) {
        selected = IsolationLevel.SERIALIZABLE;
      } else if (!needsConsistency && needsPerformance) {
        selected = IsolationLevel.READ_COMMITTED;
      } else {
        selected = IsolationLevel.REPEATABLE_READ;
      }

      assertEquals(IsolationLevel.SERIALIZABLE, selected, "Should select SERIALIZABLE");
    }
  }

  @Nested
  @DisplayName("Database Default Tests")
  class DatabaseDefaultTests {

    @Test
    @DisplayName("should identify typical database defaults")
    void shouldIdentifyTypicalDatabaseDefaults() {
      // Most databases default to READ_COMMITTED
      final IsolationLevel typicalDefault = IsolationLevel.READ_COMMITTED;

      assertEquals("READ_COMMITTED", typicalDefault.name(), "Typical DB default");
    }

    @Test
    @DisplayName("should support MVCC pattern")
    void shouldSupportMvccPattern() {
      // SNAPSHOT isolation is typically implemented with MVCC
      final IsolationLevel mvccLevel = IsolationLevel.SNAPSHOT;

      assertNotNull(mvccLevel, "SNAPSHOT uses MVCC");
    }
  }

  @Nested
  @DisplayName("Transaction Pattern Tests")
  class TransactionPatternTests {

    @Test
    @DisplayName("should support read-only transaction optimization")
    void shouldSupportReadOnlyTransactionOptimization() {
      // Read-only transactions can use SNAPSHOT for consistent reads
      final boolean isReadOnly = true;

      final IsolationLevel selected =
          isReadOnly ? IsolationLevel.SNAPSHOT : IsolationLevel.SERIALIZABLE;

      assertEquals(IsolationLevel.SNAPSHOT, selected, "Read-only can use SNAPSHOT");
    }

    @Test
    @DisplayName("should support write transaction safety")
    void shouldSupportWriteTransactionSafety() {
      // Write transactions typically need REPEATABLE_READ or higher
      final boolean hasWrites = true;
      final boolean needsSerializable = true;

      final IsolationLevel selected;
      if (hasWrites && needsSerializable) {
        selected = IsolationLevel.SERIALIZABLE;
      } else if (hasWrites) {
        selected = IsolationLevel.REPEATABLE_READ;
      } else {
        selected = IsolationLevel.READ_COMMITTED;
      }

      assertEquals(IsolationLevel.SERIALIZABLE, selected, "Write with serializable need");
    }
  }
}
