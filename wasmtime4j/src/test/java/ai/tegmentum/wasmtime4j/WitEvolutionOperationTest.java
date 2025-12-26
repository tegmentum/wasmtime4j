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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEvolutionOperation} enum.
 *
 * <p>WitEvolutionOperation defines the types of evolution operations that can be performed on WIT
 * interfaces while maintaining compatibility and semantic correctness.
 */
@DisplayName("WitEvolutionOperation Tests")
class WitEvolutionOperationTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitEvolutionOperation.class.isEnum(), "WitEvolutionOperation should be an enum");
    }

    @Test
    @DisplayName("should have expected number of operations")
    void shouldHaveExpectedNumberOfOperations() {
      final var values = WitEvolutionOperation.values();
      assertTrue(values.length >= 35, "Should have at least 35 evolution operations");
    }

    @Test
    @DisplayName("OperationComplexity should be nested enum")
    void operationComplexityShouldBeNestedEnum() {
      assertTrue(
          WitEvolutionOperation.OperationComplexity.class.isEnum(),
          "OperationComplexity should be an enum");
    }

    @Test
    @DisplayName("OperationCategory should be nested enum")
    void operationCategoryShouldBeNestedEnum() {
      assertTrue(
          WitEvolutionOperation.OperationCategory.class.isEnum(),
          "OperationCategory should be an enum");
    }

    @Test
    @DisplayName("MigrationEffort should be nested enum")
    void migrationEffortShouldBeNestedEnum() {
      assertTrue(
          WitEvolutionOperation.MigrationEffort.class.isEnum(),
          "MigrationEffort should be an enum");
    }
  }

  @Nested
  @DisplayName("Operation Properties Tests")
  class OperationPropertiesTests {

    @Test
    @DisplayName("ADD_FUNCTION should be non-breaking with LOW complexity")
    void addFunctionShouldBeNonBreakingWithLowComplexity() {
      final var op = WitEvolutionOperation.ADD_FUNCTION;

      assertFalse(op.isBreaking());
      assertTrue(op.isSafe());
      assertEquals(WitEvolutionOperation.OperationComplexity.LOW, op.getComplexity());
      assertNotNull(op.getDescription());
      assertTrue(op.getDescription().contains("function"));
    }

    @Test
    @DisplayName("REMOVE_FUNCTION should be breaking with MEDIUM complexity")
    void removeFunctionShouldBeBreakingWithMediumComplexity() {
      final var op = WitEvolutionOperation.REMOVE_FUNCTION;

      assertTrue(op.isBreaking());
      assertFalse(op.isSafe());
      assertEquals(WitEvolutionOperation.OperationComplexity.MEDIUM, op.getComplexity());
    }

    @Test
    @DisplayName("MODIFY_FUNCTION_SIGNATURE should be breaking with HIGH complexity")
    void modifyFunctionSignatureShouldBeBreakingWithHighComplexity() {
      final var op = WitEvolutionOperation.MODIFY_FUNCTION_SIGNATURE;

      assertTrue(op.isBreaking());
      assertEquals(WitEvolutionOperation.OperationComplexity.HIGH, op.getComplexity());
    }

    @Test
    @DisplayName("ADD_TYPE should be non-breaking")
    void addTypeShouldBeNonBreaking() {
      final var op = WitEvolutionOperation.ADD_TYPE;

      assertFalse(op.isBreaking());
      assertTrue(op.isSafe());
      assertEquals(WitEvolutionOperation.OperationComplexity.LOW, op.getComplexity());
    }

    @Test
    @DisplayName("REMOVE_TYPE should be breaking with HIGH complexity")
    void removeTypeShouldBeBreakingWithHighComplexity() {
      final var op = WitEvolutionOperation.REMOVE_TYPE;

      assertTrue(op.isBreaking());
      assertEquals(WitEvolutionOperation.OperationComplexity.HIGH, op.getComplexity());
    }

    @Test
    @DisplayName("ADD_OPTIONAL_FIELD should be non-breaking")
    void addOptionalFieldShouldBeNonBreaking() {
      final var op = WitEvolutionOperation.ADD_OPTIONAL_FIELD;

      assertFalse(op.isBreaking());
      assertTrue(op.isSafe());
    }

    @Test
    @DisplayName("ADD_REQUIRED_FIELD should be breaking")
    void addRequiredFieldShouldBeBreaking() {
      final var op = WitEvolutionOperation.ADD_REQUIRED_FIELD;

      assertTrue(op.isBreaking());
      assertFalse(op.isSafe());
    }

    @Test
    @DisplayName("MAKE_FIELD_OPTIONAL should be non-breaking")
    void makeFieldOptionalShouldBeNonBreaking() {
      final var op = WitEvolutionOperation.MAKE_FIELD_OPTIONAL;

      assertFalse(op.isBreaking());
    }

    @Test
    @DisplayName("MAKE_FIELD_REQUIRED should be breaking")
    void makeFieldRequiredShouldBeBreaking() {
      final var op = WitEvolutionOperation.MAKE_FIELD_REQUIRED;

      assertTrue(op.isBreaking());
    }
  }

  @Nested
  @DisplayName("Category Tests")
  class CategoryTests {

    @Test
    @DisplayName("function operations should have FUNCTION category")
    void functionOperationsShouldHaveFunctionCategory() {
      assertEquals(
          WitEvolutionOperation.OperationCategory.FUNCTION,
          WitEvolutionOperation.ADD_FUNCTION.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.FUNCTION,
          WitEvolutionOperation.REMOVE_FUNCTION.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.FUNCTION,
          WitEvolutionOperation.MODIFY_FUNCTION_SIGNATURE.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.FUNCTION,
          WitEvolutionOperation.RENAME_FUNCTION.getCategory());
    }

    @Test
    @DisplayName("type operations should have TYPE category")
    void typeOperationsShouldHaveTypeCategory() {
      assertEquals(
          WitEvolutionOperation.OperationCategory.TYPE,
          WitEvolutionOperation.ADD_TYPE.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.TYPE,
          WitEvolutionOperation.REMOVE_TYPE.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.TYPE,
          WitEvolutionOperation.MODIFY_TYPE.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.TYPE,
          WitEvolutionOperation.RENAME_TYPE.getCategory());
    }

    @Test
    @DisplayName("field operations should have TYPE category")
    void fieldOperationsShouldHaveTypeCategory() {
      assertEquals(
          WitEvolutionOperation.OperationCategory.TYPE,
          WitEvolutionOperation.ADD_OPTIONAL_FIELD.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.TYPE,
          WitEvolutionOperation.ADD_REQUIRED_FIELD.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.TYPE,
          WitEvolutionOperation.REMOVE_FIELD.getCategory());
    }

    @Test
    @DisplayName("import/export operations should have INTERFACE category")
    void importExportOperationsShouldHaveInterfaceCategory() {
      assertEquals(
          WitEvolutionOperation.OperationCategory.INTERFACE,
          WitEvolutionOperation.ADD_IMPORT.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.INTERFACE,
          WitEvolutionOperation.REMOVE_IMPORT.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.INTERFACE,
          WitEvolutionOperation.ADD_EXPORT.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.INTERFACE,
          WitEvolutionOperation.REMOVE_EXPORT.getCategory());
    }

    @Test
    @DisplayName("parameter operations should have SIGNATURE category")
    void parameterOperationsShouldHaveSignatureCategory() {
      assertEquals(
          WitEvolutionOperation.OperationCategory.SIGNATURE,
          WitEvolutionOperation.ADD_PARAMETER.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.SIGNATURE,
          WitEvolutionOperation.REMOVE_PARAMETER.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.SIGNATURE,
          WitEvolutionOperation.REORDER_PARAMETERS.getCategory());
      assertEquals(
          WitEvolutionOperation.OperationCategory.SIGNATURE,
          WitEvolutionOperation.CHANGE_RETURN_TYPE.getCategory());
    }

    @Test
    @DisplayName("version operations should have METADATA category")
    void versionOperationsShouldHaveMetadataCategory() {
      assertEquals(
          WitEvolutionOperation.OperationCategory.METADATA,
          WitEvolutionOperation.UPDATE_VERSION.getCategory());
    }
  }

  @Nested
  @DisplayName("Migration Effort Tests")
  class MigrationEffortTests {

    @Test
    @DisplayName("LOW complexity non-breaking should have LOW effort")
    void lowComplexityNonBreakingShouldHaveLowEffort() {
      final var op = WitEvolutionOperation.ADD_FUNCTION;
      assertEquals(WitEvolutionOperation.MigrationEffort.LOW, op.getEstimatedEffort());
    }

    @Test
    @DisplayName("LOW complexity breaking should have MEDIUM effort")
    void lowComplexityBreakingShouldHaveMediumEffort() {
      // Find a LOW complexity breaking operation
      // Looking at enum, there isn't one - LOW breaking would map to MEDIUM
      // Let's verify the pattern with the existing operations
      for (WitEvolutionOperation op : WitEvolutionOperation.values()) {
        if (op.getComplexity() == WitEvolutionOperation.OperationComplexity.LOW
            && op.isBreaking()) {
          assertEquals(WitEvolutionOperation.MigrationEffort.MEDIUM, op.getEstimatedEffort());
        }
      }
    }

    @Test
    @DisplayName("MEDIUM complexity non-breaking should have MEDIUM effort")
    void mediumComplexityNonBreakingShouldHaveMediumEffort() {
      final var op = WitEvolutionOperation.MAKE_FIELD_OPTIONAL;
      assertEquals(WitEvolutionOperation.MigrationEffort.MEDIUM, op.getEstimatedEffort());
    }

    @Test
    @DisplayName("MEDIUM complexity breaking should have HIGH effort")
    void mediumComplexityBreakingShouldHaveHighEffort() {
      final var op = WitEvolutionOperation.REMOVE_FUNCTION;
      assertEquals(WitEvolutionOperation.MigrationEffort.HIGH, op.getEstimatedEffort());
    }

    @Test
    @DisplayName("HIGH complexity non-breaking should have HIGH effort")
    void highComplexityNonBreakingShouldHaveHighEffort() {
      final var op = WitEvolutionOperation.GENERALIZE_TYPE;
      assertEquals(WitEvolutionOperation.MigrationEffort.HIGH, op.getEstimatedEffort());
    }

    @Test
    @DisplayName("HIGH complexity breaking should have VERY_HIGH effort")
    void highComplexityBreakingShouldHaveVeryHighEffort() {
      final var op = WitEvolutionOperation.MODIFY_FUNCTION_SIGNATURE;
      assertEquals(WitEvolutionOperation.MigrationEffort.VERY_HIGH, op.getEstimatedEffort());
    }
  }

  @Nested
  @DisplayName("OperationComplexity Enum Tests")
  class OperationComplexityEnumTests {

    @Test
    @DisplayName("should have all expected complexity levels")
    void shouldHaveAllExpectedComplexityLevels() {
      final var values = WitEvolutionOperation.OperationComplexity.values();
      assertEquals(3, values.length);

      assertNotNull(WitEvolutionOperation.OperationComplexity.LOW);
      assertNotNull(WitEvolutionOperation.OperationComplexity.MEDIUM);
      assertNotNull(WitEvolutionOperation.OperationComplexity.HIGH);
    }
  }

  @Nested
  @DisplayName("OperationCategory Enum Tests")
  class OperationCategoryEnumTests {

    @Test
    @DisplayName("should have all expected categories")
    void shouldHaveAllExpectedCategories() {
      final var values = WitEvolutionOperation.OperationCategory.values();
      assertEquals(5, values.length);

      assertNotNull(WitEvolutionOperation.OperationCategory.FUNCTION);
      assertNotNull(WitEvolutionOperation.OperationCategory.TYPE);
      assertNotNull(WitEvolutionOperation.OperationCategory.INTERFACE);
      assertNotNull(WitEvolutionOperation.OperationCategory.SIGNATURE);
      assertNotNull(WitEvolutionOperation.OperationCategory.METADATA);
    }
  }

  @Nested
  @DisplayName("MigrationEffort Enum Tests")
  class MigrationEffortEnumTests {

    @Test
    @DisplayName("should have all expected effort levels")
    void shouldHaveAllExpectedEffortLevels() {
      final var values = WitEvolutionOperation.MigrationEffort.values();
      assertEquals(4, values.length);

      assertNotNull(WitEvolutionOperation.MigrationEffort.LOW);
      assertNotNull(WitEvolutionOperation.MigrationEffort.MEDIUM);
      assertNotNull(WitEvolutionOperation.MigrationEffort.HIGH);
      assertNotNull(WitEvolutionOperation.MigrationEffort.VERY_HIGH);
    }
  }

  @Nested
  @DisplayName("Description Tests")
  class DescriptionTests {

    @Test
    @DisplayName("all operations should have non-null descriptions")
    void allOperationsShouldHaveNonNullDescriptions() {
      for (WitEvolutionOperation op : WitEvolutionOperation.values()) {
        assertNotNull(op.getDescription(), "Operation " + op.name() + " should have description");
        assertFalse(
            op.getDescription().isEmpty(),
            "Operation " + op.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("descriptions should be meaningful")
    void descriptionsShouldBeMeaningful() {
      assertTrue(
          WitEvolutionOperation.ADD_FUNCTION
              .getDescription()
              .toLowerCase()
              .contains("add"));
      assertTrue(
          WitEvolutionOperation.REMOVE_FUNCTION
              .getDescription()
              .toLowerCase()
              .contains("remove"));
      assertTrue(
          WitEvolutionOperation.MODIFY_TYPE
              .getDescription()
              .toLowerCase()
              .contains("modify"));
      assertTrue(
          WitEvolutionOperation.RENAME_TYPE
              .getDescription()
              .toLowerCase()
              .contains("rename"));
    }
  }

  @Nested
  @DisplayName("Breaking Change Analysis Tests")
  class BreakingChangeAnalysisTests {

    @Test
    @DisplayName("adding elements should generally be non-breaking")
    void addingElementsShouldGenerallyBeNonBreaking() {
      assertFalse(WitEvolutionOperation.ADD_FUNCTION.isBreaking());
      assertFalse(WitEvolutionOperation.ADD_TYPE.isBreaking());
      assertFalse(WitEvolutionOperation.ADD_OPTIONAL_FIELD.isBreaking());
      assertFalse(WitEvolutionOperation.ADD_VARIANT_CASE.isBreaking());
      assertFalse(WitEvolutionOperation.ADD_ENUM_VALUE.isBreaking());
      assertFalse(WitEvolutionOperation.ADD_IMPORT.isBreaking());
      assertFalse(WitEvolutionOperation.ADD_EXPORT.isBreaking());
    }

    @Test
    @DisplayName("removing elements should generally be breaking")
    void removingElementsShouldGenerallyBeBreaking() {
      assertTrue(WitEvolutionOperation.REMOVE_FUNCTION.isBreaking());
      assertTrue(WitEvolutionOperation.REMOVE_TYPE.isBreaking());
      assertTrue(WitEvolutionOperation.REMOVE_FIELD.isBreaking());
      assertTrue(WitEvolutionOperation.REMOVE_VARIANT_CASE.isBreaking());
      assertTrue(WitEvolutionOperation.REMOVE_ENUM_VALUE.isBreaking());
      assertTrue(WitEvolutionOperation.REMOVE_IMPORT.isBreaking());
      assertTrue(WitEvolutionOperation.REMOVE_EXPORT.isBreaking());
    }

    @Test
    @DisplayName("modifying signatures should be breaking")
    void modifyingSignaturesShouldBeBreaking() {
      assertTrue(WitEvolutionOperation.MODIFY_FUNCTION_SIGNATURE.isBreaking());
      assertTrue(WitEvolutionOperation.ADD_PARAMETER.isBreaking());
      assertTrue(WitEvolutionOperation.REMOVE_PARAMETER.isBreaking());
      assertTrue(WitEvolutionOperation.REORDER_PARAMETERS.isBreaking());
      assertTrue(WitEvolutionOperation.CHANGE_RETURN_TYPE.isBreaking());
    }

    @Test
    @DisplayName("renaming should be breaking")
    void renamingShouldBeBreaking() {
      assertTrue(WitEvolutionOperation.RENAME_FUNCTION.isBreaking());
      assertTrue(WitEvolutionOperation.RENAME_TYPE.isBreaking());
    }
  }

  @Nested
  @DisplayName("isSafe Tests")
  class IsSafeTests {

    @Test
    @DisplayName("isSafe should be opposite of isBreaking")
    void isSafeShouldBeOppositeOfIsBreaking() {
      for (WitEvolutionOperation op : WitEvolutionOperation.values()) {
        assertEquals(
            !op.isBreaking(),
            op.isSafe(),
            "isSafe should be opposite of isBreaking for " + op.name());
      }
    }
  }
}
