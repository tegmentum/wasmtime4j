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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionStatus enum.
 *
 * <p>This test class verifies the enum structure, values, and functionality for ExecutionStatus
 * using reflection-based testing.
 */
@DisplayName("ExecutionStatus Tests")
class ExecutionStatusTest {

  // ========================================================================
  // Enum Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("ExecutionStatus should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ExecutionStatus.class.isEnum(), "ExecutionStatus should be an enum");
    }

    @Test
    @DisplayName("ExecutionStatus should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionStatus.class.getModifiers()),
          "ExecutionStatus should be public");
    }

    @Test
    @DisplayName("ExecutionStatus should be a final enum (implicitly)")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(ExecutionStatus.class.getModifiers()),
          "ExecutionStatus should be final");
    }

    @Test
    @DisplayName("ExecutionStatus should have exactly 13 values")
    void shouldHaveExactlyThirteenValues() {
      ExecutionStatus[] values = ExecutionStatus.values();
      assertEquals(13, values.length, "ExecutionStatus should have exactly 13 values");
    }
  }

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("PENDING should exist")
    void shouldHavePending() {
      assertNotNull(ExecutionStatus.valueOf("PENDING"), "PENDING should exist");
    }

    @Test
    @DisplayName("INITIALIZING should exist")
    void shouldHaveInitializing() {
      assertNotNull(ExecutionStatus.valueOf("INITIALIZING"), "INITIALIZING should exist");
    }

    @Test
    @DisplayName("RUNNING should exist")
    void shouldHaveRunning() {
      assertNotNull(ExecutionStatus.valueOf("RUNNING"), "RUNNING should exist");
    }

    @Test
    @DisplayName("SUSPENDED should exist")
    void shouldHaveSuspended() {
      assertNotNull(ExecutionStatus.valueOf("SUSPENDED"), "SUSPENDED should exist");
    }

    @Test
    @DisplayName("PAUSED should exist")
    void shouldHavePaused() {
      assertNotNull(ExecutionStatus.valueOf("PAUSED"), "PAUSED should exist");
    }

    @Test
    @DisplayName("COMPLETED should exist")
    void shouldHaveCompleted() {
      assertNotNull(ExecutionStatus.valueOf("COMPLETED"), "COMPLETED should exist");
    }

    @Test
    @DisplayName("FAILED should exist")
    void shouldHaveFailed() {
      assertNotNull(ExecutionStatus.valueOf("FAILED"), "FAILED should exist");
    }

    @Test
    @DisplayName("TERMINATED should exist")
    void shouldHaveTerminated() {
      assertNotNull(ExecutionStatus.valueOf("TERMINATED"), "TERMINATED should exist");
    }

    @Test
    @DisplayName("CANCELLED should exist")
    void shouldHaveCancelled() {
      assertNotNull(ExecutionStatus.valueOf("CANCELLED"), "CANCELLED should exist");
    }

    @Test
    @DisplayName("TIMED_OUT should exist")
    void shouldHaveTimedOut() {
      assertNotNull(ExecutionStatus.valueOf("TIMED_OUT"), "TIMED_OUT should exist");
    }

    @Test
    @DisplayName("CLEANING_UP should exist")
    void shouldHaveCleaningUp() {
      assertNotNull(ExecutionStatus.valueOf("CLEANING_UP"), "CLEANING_UP should exist");
    }

    @Test
    @DisplayName("ERROR_RECOVERY should exist")
    void shouldHaveErrorRecovery() {
      assertNotNull(ExecutionStatus.valueOf("ERROR_RECOVERY"), "ERROR_RECOVERY should exist");
    }

    @Test
    @DisplayName("UNKNOWN should exist")
    void shouldHaveUnknown() {
      assertNotNull(ExecutionStatus.valueOf("UNKNOWN"), "UNKNOWN should exist");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allValuesShouldHaveUniqueNames() {
      ExecutionStatus[] values = ExecutionStatus.values();
      Set<String> names = new HashSet<>();
      for (ExecutionStatus status : values) {
        names.add(status.name());
      }
      assertEquals(values.length, names.size(), "All enum values should have unique names");
    }

    @Test
    @DisplayName("All enum values should have unique ordinals")
    void allValuesShouldHaveUniqueOrdinals() {
      ExecutionStatus[] values = ExecutionStatus.values();
      Set<Integer> ordinals = new HashSet<>();
      for (ExecutionStatus status : values) {
        ordinals.add(status.ordinal());
      }
      assertEquals(values.length, ordinals.size(), "All enum values should have unique ordinals");
    }
  }

  // ========================================================================
  // Enum Functionality Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Functionality Tests")
  class EnumFunctionalityTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllConstants() {
      ExecutionStatus[] values = ExecutionStatus.values();
      assertNotNull(values, "values() should not return null");
      assertEquals(13, values.length, "values() should return 13 constants");
    }

    @Test
    @DisplayName("valueOf() should return correct enum for valid name")
    void valueOfShouldReturnCorrectEnum() {
      assertEquals(
          ExecutionStatus.PENDING,
          ExecutionStatus.valueOf("PENDING"),
          "valueOf should return correct enum");
      assertEquals(
          ExecutionStatus.RUNNING,
          ExecutionStatus.valueOf("RUNNING"),
          "valueOf should return correct enum");
    }

    @Test
    @DisplayName("valueOf() should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExecutionStatus.valueOf("INVALID_STATUS"),
          "valueOf should throw for invalid name");
    }

    @Test
    @DisplayName("name() should return the enum constant name")
    void nameShouldReturnConstantName() {
      assertEquals("PENDING", ExecutionStatus.PENDING.name(), "name() should return PENDING");
      assertEquals("RUNNING", ExecutionStatus.RUNNING.name(), "name() should return RUNNING");
    }

    @Test
    @DisplayName("ordinal() should return sequential values starting from 0")
    void ordinalShouldReturnSequentialValues() {
      ExecutionStatus[] values = ExecutionStatus.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should match index");
      }
    }

    @Test
    @DisplayName("toString() should return the enum name")
    void toStringShouldReturnEnumName() {
      assertEquals("PENDING", ExecutionStatus.PENDING.toString(), "toString should return name");
      assertEquals(
          "COMPLETED", ExecutionStatus.COMPLETED.toString(), "toString should return name");
    }

    @Test
    @DisplayName("Enum values should be comparable")
    void enumValuesShouldBeComparable() {
      assertTrue(
          ExecutionStatus.PENDING.compareTo(ExecutionStatus.RUNNING) < 0,
          "PENDING should come before RUNNING");
      assertTrue(
          ExecutionStatus.UNKNOWN.compareTo(ExecutionStatus.PENDING) > 0,
          "UNKNOWN should come after PENDING");
      assertEquals(
          0,
          ExecutionStatus.COMPLETED.compareTo(ExecutionStatus.COMPLETED),
          "Same should be equal");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("PENDING should have ordinal 0")
    void pendingShouldHaveOrdinalZero() {
      assertEquals(0, ExecutionStatus.PENDING.ordinal(), "PENDING should have ordinal 0");
    }

    @Test
    @DisplayName("UNKNOWN should have ordinal 12")
    void unknownShouldHaveOrdinalTwelve() {
      assertEquals(12, ExecutionStatus.UNKNOWN.ordinal(), "UNKNOWN should have ordinal 12");
    }

    @Test
    @DisplayName("Ordinal values should be continuous from 0 to 12")
    void ordinalsShouldBeContinuous() {
      ExecutionStatus[] values = ExecutionStatus.values();
      assertEquals(0, values[0].ordinal(), "First ordinal should be 0");
      assertEquals(12, values[values.length - 1].ordinal(), "Last ordinal should be 12");

      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal at index " + i + " should be " + i);
      }
    }
  }

  // ========================================================================
  // Category Tests
  // ========================================================================

  @Nested
  @DisplayName("Category Tests")
  class CategoryTests {

    @Test
    @DisplayName("Active statuses should be present")
    void activeStatusesShouldBePresent() {
      Set<String> activeStatuses =
          Set.of("PENDING", "INITIALIZING", "RUNNING", "SUSPENDED", "PAUSED");
      Set<String> actualStatuses = new HashSet<>();
      for (ExecutionStatus status : ExecutionStatus.values()) {
        actualStatuses.add(status.name());
      }

      assertTrue(
          actualStatuses.containsAll(activeStatuses), "All active statuses should be present");
    }

    @Test
    @DisplayName("Terminal statuses should be present")
    void terminalStatusesShouldBePresent() {
      Set<String> terminalStatuses =
          Set.of("COMPLETED", "FAILED", "TERMINATED", "CANCELLED", "TIMED_OUT");
      Set<String> actualStatuses = new HashSet<>();
      for (ExecutionStatus status : ExecutionStatus.values()) {
        actualStatuses.add(status.name());
      }

      assertTrue(
          actualStatuses.containsAll(terminalStatuses), "All terminal statuses should be present");
    }

    @Test
    @DisplayName("Recovery statuses should be present")
    void recoveryStatusesShouldBePresent() {
      Set<String> recoveryStatuses = Set.of("CLEANING_UP", "ERROR_RECOVERY", "UNKNOWN");
      Set<String> actualStatuses = new HashSet<>();
      for (ExecutionStatus status : ExecutionStatus.values()) {
        actualStatuses.add(status.name());
      }

      assertTrue(
          actualStatuses.containsAll(recoveryStatuses), "All recovery statuses should be present");
    }
  }

  // ========================================================================
  // Collection Compatibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Collection Compatibility Tests")
  class CollectionCompatibilityTests {

    @Test
    @DisplayName("Enum values should work with Arrays.asList")
    void enumValuesShouldWorkWithArraysList() {
      var list = Arrays.asList(ExecutionStatus.values());
      assertEquals(13, list.size(), "List should contain 13 elements");
      assertTrue(list.contains(ExecutionStatus.RUNNING), "List should contain RUNNING");
    }

    @Test
    @DisplayName("Enum values should work with HashSet")
    void enumValuesShouldWorkWithHashSet() {
      Set<ExecutionStatus> set = new HashSet<>(Arrays.asList(ExecutionStatus.values()));
      assertEquals(13, set.size(), "Set should contain 13 elements");
      assertTrue(set.contains(ExecutionStatus.COMPLETED), "Set should contain COMPLETED");
    }
  }

  // ========================================================================
  // Enum Instance Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Instance Tests")
  class EnumInstanceTests {

    @Test
    @DisplayName("Enum constants should be singleton")
    void enumConstantsShouldBeSingleton() {
      ExecutionStatus first = ExecutionStatus.valueOf("RUNNING");
      ExecutionStatus second = ExecutionStatus.valueOf("RUNNING");

      assertTrue(first == second, "Enum constants should be the same instance");
    }

    @Test
    @DisplayName("Enum should not allow null values")
    void enumShouldNotAllowNullValues() {
      ExecutionStatus[] values = ExecutionStatus.values();
      for (ExecutionStatus status : values) {
        assertNotNull(status, "Enum value should not be null");
      }
    }

    @Test
    @DisplayName("Each enum should have correct declaring class")
    void eachEnumShouldHaveCorrectDeclaringClass() {
      for (ExecutionStatus status : ExecutionStatus.values()) {
        assertEquals(
            ExecutionStatus.class,
            status.getDeclaringClass(),
            status.name() + " should have ExecutionStatus as declaring class");
      }
    }
  }
}
