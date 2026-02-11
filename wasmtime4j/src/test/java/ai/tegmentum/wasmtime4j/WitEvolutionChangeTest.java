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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitEvolutionChange;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEvolutionChange} class.
 *
 * <p>WitEvolutionChange represents changes made during WIT interface evolution, including change
 * type, impact, and migration guidance.
 */
@DisplayName("WitEvolutionChange Tests")
class WitEvolutionChangeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WitEvolutionChange.class.getModifiers()),
          "WitEvolutionChange should be public");
      assertTrue(
          Modifier.isFinal(WitEvolutionChange.class.getModifiers()),
          "WitEvolutionChange should be final");
    }

    @Test
    @DisplayName("ChangeType should be public enum")
    void changeTypeShouldBePublicEnum() {
      assertTrue(WitEvolutionChange.ChangeType.class.isEnum(), "ChangeType should be an enum");
    }

    @Test
    @DisplayName("ChangeImpact should be public enum")
    void changeImpactShouldBePublicEnum() {
      assertTrue(WitEvolutionChange.ChangeImpact.class.isEnum(), "ChangeImpact should be an enum");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final List<String> migrationSteps = List.of("Step 1", "Step 2");

      final var change =
          new WitEvolutionChange(
              WitEvolutionChange.ChangeType.FUNCTION_ADDED,
              "Added new function",
              "interface.functions.foo",
              WitEvolutionChange.ChangeImpact.LOW,
              false,
              migrationSteps,
              null,
              "func foo() -> string");

      assertEquals(WitEvolutionChange.ChangeType.FUNCTION_ADDED, change.getType());
      assertEquals("Added new function", change.getDescription());
      assertEquals("interface.functions.foo", change.getLocation());
      assertEquals(WitEvolutionChange.ChangeImpact.LOW, change.getImpact());
      assertFalse(change.isBreaking());
      assertEquals(2, change.getMigrationSteps().size());
      assertNull(change.getOldValue());
      assertEquals("func foo() -> string", change.getNewValue());
    }

    @Test
    @DisplayName("should throw NullPointerException for null type")
    void shouldThrowNpeForNullType() {
      assertThrows(
          NullPointerException.class,
          () ->
              new WitEvolutionChange(
                  null,
                  "desc",
                  "loc",
                  WitEvolutionChange.ChangeImpact.LOW,
                  false,
                  List.of(),
                  null,
                  null));
    }

    @Test
    @DisplayName("should throw NullPointerException for null description")
    void shouldThrowNpeForNullDescription() {
      assertThrows(
          NullPointerException.class,
          () ->
              new WitEvolutionChange(
                  WitEvolutionChange.ChangeType.TYPE_ADDED,
                  null,
                  "loc",
                  WitEvolutionChange.ChangeImpact.LOW,
                  false,
                  List.of(),
                  null,
                  null));
    }

    @Test
    @DisplayName("should throw NullPointerException for null location")
    void shouldThrowNpeForNullLocation() {
      assertThrows(
          NullPointerException.class,
          () ->
              new WitEvolutionChange(
                  WitEvolutionChange.ChangeType.TYPE_ADDED,
                  "desc",
                  null,
                  WitEvolutionChange.ChangeImpact.LOW,
                  false,
                  List.of(),
                  null,
                  null));
    }

    @Test
    @DisplayName("should throw NullPointerException for null impact")
    void shouldThrowNpeForNullImpact() {
      assertThrows(
          NullPointerException.class,
          () ->
              new WitEvolutionChange(
                  WitEvolutionChange.ChangeType.TYPE_ADDED,
                  "desc",
                  "loc",
                  null,
                  false,
                  List.of(),
                  null,
                  null));
    }

    @Test
    @DisplayName("should throw NullPointerException for null migration steps")
    void shouldThrowNpeForNullMigrationSteps() {
      assertThrows(
          NullPointerException.class,
          () ->
              new WitEvolutionChange(
                  WitEvolutionChange.ChangeType.TYPE_ADDED,
                  "desc",
                  "loc",
                  WitEvolutionChange.ChangeImpact.LOW,
                  false,
                  null,
                  null,
                  null));
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("functionAdded should create correct change")
    void functionAddedShouldCreateCorrectChange() {
      final var change = WitEvolutionChange.functionAdded("greet", "func greet(name: string)");

      assertEquals(WitEvolutionChange.ChangeType.FUNCTION_ADDED, change.getType());
      assertTrue(change.getDescription().contains("greet"));
      assertEquals("interface.functions.greet", change.getLocation());
      assertEquals(WitEvolutionChange.ChangeImpact.LOW, change.getImpact());
      assertFalse(change.isBreaking());
      assertNull(change.getOldValue());
      assertEquals("func greet(name: string)", change.getNewValue());
    }

    @Test
    @DisplayName("functionRemoved should create breaking change")
    void functionRemovedShouldCreateBreakingChange() {
      final var change = WitEvolutionChange.functionRemoved("oldFunc", "func oldFunc()");

      assertEquals(WitEvolutionChange.ChangeType.FUNCTION_REMOVED, change.getType());
      assertTrue(change.getDescription().contains("oldFunc"));
      assertEquals(WitEvolutionChange.ChangeImpact.HIGH, change.getImpact());
      assertTrue(change.isBreaking());
      assertEquals("func oldFunc()", change.getOldValue());
      assertNull(change.getNewValue());
      assertFalse(change.getMigrationSteps().isEmpty());
    }

    @Test
    @DisplayName("functionSignatureChanged should create breaking change")
    void functionSignatureChangedShouldCreateBreakingChange() {
      final var change =
          WitEvolutionChange.functionSignatureChanged(
              "process", "func process(x: s32)", "func process(x: s32, y: s32)");

      assertEquals(WitEvolutionChange.ChangeType.FUNCTION_SIGNATURE_CHANGED, change.getType());
      assertEquals(WitEvolutionChange.ChangeImpact.HIGH, change.getImpact());
      assertTrue(change.isBreaking());
      assertEquals("func process(x: s32)", change.getOldValue());
      assertEquals("func process(x: s32, y: s32)", change.getNewValue());
    }

    @Test
    @DisplayName("typeAdded should create non-breaking change")
    void typeAddedShouldCreateNonBreakingChange() {
      final var change = WitEvolutionChange.typeAdded("person", "record person { name: string }");

      assertEquals(WitEvolutionChange.ChangeType.TYPE_ADDED, change.getType());
      assertEquals(WitEvolutionChange.ChangeImpact.LOW, change.getImpact());
      assertFalse(change.isBreaking());
      assertNull(change.getOldValue());
      assertEquals("record person { name: string }", change.getNewValue());
    }

    @Test
    @DisplayName("typeRemoved should create breaking change")
    void typeRemovedShouldCreateBreakingChange() {
      final var change = WitEvolutionChange.typeRemoved("old-type", "record old-type {}");

      assertEquals(WitEvolutionChange.ChangeType.TYPE_REMOVED, change.getType());
      assertEquals(WitEvolutionChange.ChangeImpact.MEDIUM, change.getImpact());
      assertTrue(change.isBreaking());
      assertEquals("record old-type {}", change.getOldValue());
      assertNull(change.getNewValue());
    }

    @Test
    @DisplayName("typeModified with breaking=true should create high impact change")
    void typeModifiedWithBreakingShouldCreateHighImpactChange() {
      final var change =
          WitEvolutionChange.typeModified(
              "config", "record config { a: s32 }", "record config { a: string }", true);

      assertEquals(WitEvolutionChange.ChangeType.TYPE_MODIFIED, change.getType());
      assertEquals(WitEvolutionChange.ChangeImpact.HIGH, change.getImpact());
      assertTrue(change.isBreaking());
    }

    @Test
    @DisplayName("typeModified with breaking=false should create medium impact change")
    void typeModifiedWithoutBreakingShouldCreateMediumImpactChange() {
      final var change =
          WitEvolutionChange.typeModified(
              "config", "record config { a: s32 }", "record config { a: s32, b: s32 }", false);

      assertEquals(WitEvolutionChange.ChangeType.TYPE_MODIFIED, change.getType());
      assertEquals(WitEvolutionChange.ChangeImpact.MEDIUM, change.getImpact());
      assertFalse(change.isBreaking());
    }
  }

  @Nested
  @DisplayName("ChangeType Enum Tests")
  class ChangeTypeEnumTests {

    @Test
    @DisplayName("should have all expected change types")
    void shouldHaveAllExpectedChangeTypes() {
      final var types = WitEvolutionChange.ChangeType.values();

      assertTrue(types.length >= 10, "Should have at least 10 change types");

      // Verify key types exist
      assertNotNull(WitEvolutionChange.ChangeType.FUNCTION_ADDED);
      assertNotNull(WitEvolutionChange.ChangeType.FUNCTION_REMOVED);
      assertNotNull(WitEvolutionChange.ChangeType.FUNCTION_SIGNATURE_CHANGED);
      assertNotNull(WitEvolutionChange.ChangeType.TYPE_ADDED);
      assertNotNull(WitEvolutionChange.ChangeType.TYPE_REMOVED);
      assertNotNull(WitEvolutionChange.ChangeType.TYPE_MODIFIED);
      assertNotNull(WitEvolutionChange.ChangeType.IMPORT_ADDED);
      assertNotNull(WitEvolutionChange.ChangeType.IMPORT_REMOVED);
      assertNotNull(WitEvolutionChange.ChangeType.EXPORT_ADDED);
      assertNotNull(WitEvolutionChange.ChangeType.EXPORT_REMOVED);
    }
  }

  @Nested
  @DisplayName("ChangeImpact Enum Tests")
  class ChangeImpactEnumTests {

    @Test
    @DisplayName("should have all expected impact levels")
    void shouldHaveAllExpectedImpactLevels() {
      final var impacts = WitEvolutionChange.ChangeImpact.values();

      assertEquals(4, impacts.length, "Should have exactly 4 impact levels");

      assertNotNull(WitEvolutionChange.ChangeImpact.LOW);
      assertNotNull(WitEvolutionChange.ChangeImpact.MEDIUM);
      assertNotNull(WitEvolutionChange.ChangeImpact.HIGH);
      assertNotNull(WitEvolutionChange.ChangeImpact.CRITICAL);
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equal changes should be equal")
    void equalChangesShouldBeEqual() {
      final var change1 =
          new WitEvolutionChange(
              WitEvolutionChange.ChangeType.FUNCTION_ADDED,
              "desc",
              "loc",
              WitEvolutionChange.ChangeImpact.LOW,
              false,
              List.of("step1"),
              null,
              "new");

      final var change2 =
          new WitEvolutionChange(
              WitEvolutionChange.ChangeType.FUNCTION_ADDED,
              "desc",
              "loc",
              WitEvolutionChange.ChangeImpact.LOW,
              false,
              List.of("step1"),
              null,
              "new");

      assertEquals(change1, change2);
      assertEquals(change1.hashCode(), change2.hashCode());
    }

    @Test
    @DisplayName("different changes should not be equal")
    void differentChangesShouldNotBeEqual() {
      final var change1 = WitEvolutionChange.functionAdded("func1", "sig1");
      final var change2 = WitEvolutionChange.functionAdded("func2", "sig2");

      assertNotEquals(change1, change2);
    }

    @Test
    @DisplayName("change should equal itself")
    void changeShouldEqualItself() {
      final var change = WitEvolutionChange.functionAdded("func", "sig");
      assertEquals(change, change);
    }

    @Test
    @DisplayName("change should not equal null")
    void changeShouldNotEqualNull() {
      final var change = WitEvolutionChange.functionAdded("func", "sig");
      assertNotEquals(null, change);
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var change = WitEvolutionChange.functionAdded("myFunc", "func myFunc()");

      final String str = change.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitEvolutionChange"), "Should contain class name");
      assertTrue(str.contains("FUNCTION_ADDED"), "Should contain change type");
      assertTrue(str.contains("myFunc"), "Should contain function name");
    }
  }

  @Nested
  @DisplayName("Migration Steps Tests")
  class MigrationStepsTests {

    @Test
    @DisplayName("getMigrationSteps should return unmodifiable list")
    void getMigrationStepsShouldReturnUnmodifiableList() {
      final var change = WitEvolutionChange.functionRemoved("func", "sig");
      final List<String> steps = change.getMigrationSteps();

      assertNotNull(steps, "Migration steps should not be null");

      try {
        steps.add("new step");
        assertTrue(false, "List should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        assertTrue(true, "List is unmodifiable as expected");
      }
    }

    @Test
    @DisplayName("factory methods should provide relevant migration steps")
    void factoryMethodsShouldProvideRelevantMigrationSteps() {
      final var removedChange = WitEvolutionChange.functionRemoved("func", "sig");
      assertFalse(removedChange.getMigrationSteps().isEmpty());

      final var addedChange = WitEvolutionChange.functionAdded("func", "sig");
      assertFalse(addedChange.getMigrationSteps().isEmpty());
    }
  }
}
