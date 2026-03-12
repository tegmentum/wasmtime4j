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

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitCompatibilityResult} class.
 *
 * <p>WitCompatibilityResult provides information about the compatibility between two WIT
 * interfaces, including compatibility status and any issues found.
 */
@DisplayName("WitCompatibilityResult Tests")
class WitCompatibilityResultTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final Set<String> satisfied = Set.of("import1", "import2");
      final Set<String> unsatisfied = Set.of("import3");

      final WitCompatibilityResult result =
          new WitCompatibilityResult(true, "Compatible", satisfied, unsatisfied);

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("Compatible", result.getDetails(), "Details should match");
      assertEquals(2, result.getSatisfiedImports().size(), "Should have 2 satisfied imports");
      assertEquals(1, result.getUnsatisfiedImports().size(), "Should have 1 unsatisfied import");
    }

    @Test
    @DisplayName("should create defensive copies of sets")
    void shouldCreateDefensiveCopiesOfSets() {
      final Set<String> satisfied = new java.util.HashSet<>(Set.of("import1"));
      final Set<String> unsatisfied = new java.util.HashSet<>(Set.of("import2"));

      final WitCompatibilityResult result =
          new WitCompatibilityResult(true, "test", satisfied, unsatisfied);

      // Modify original sets
      satisfied.add("new-import");
      unsatisfied.add("new-import");

      // Result should not be affected
      assertEquals(1, result.getSatisfiedImports().size(), "Satisfied imports should be unchanged");
      assertEquals(
          1, result.getUnsatisfiedImports().size(), "Unsatisfied imports should be unchanged");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("compatible factory should create compatible result")
    void compatibleFactoryShouldCreateCompatibleResult() {
      final Set<String> satisfied = Set.of("wasi:cli/stdio@0.2.0", "wasi:http/types@0.2.0");

      final WitCompatibilityResult result =
          WitCompatibilityResult.compatible("All imports satisfied", satisfied);

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("All imports satisfied", result.getDetails(), "Details should match");
      assertEquals(2, result.getSatisfiedImports().size(), "Should have 2 satisfied imports");
      assertTrue(result.getUnsatisfiedImports().isEmpty(), "Should have no unsatisfied imports");
      assertFalse(result.hasUnsatisfiedImports(), "hasUnsatisfiedImports should return false");
    }

    @Test
    @DisplayName("incompatible factory should create incompatible result")
    void incompatibleFactoryShouldCreateIncompatibleResult() {
      final Set<String> unsatisfied = Set.of("wasi:cli/missing@0.2.0");

      final WitCompatibilityResult result =
          WitCompatibilityResult.incompatible("Missing required import", unsatisfied);

      assertFalse(result.isCompatible(), "Should not be compatible");
      assertEquals("Missing required import", result.getDetails(), "Details should match");
      assertTrue(result.getSatisfiedImports().isEmpty(), "Should have no satisfied imports");
      assertEquals(1, result.getUnsatisfiedImports().size(), "Should have 1 unsatisfied import");
      assertTrue(result.hasUnsatisfiedImports(), "hasUnsatisfiedImports should return true");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("isCompatible should return correct value")
    void isCompatibleShouldReturnCorrectValue() {
      final WitCompatibilityResult compatible = WitCompatibilityResult.compatible("ok", Set.of());
      final WitCompatibilityResult incompatible =
          WitCompatibilityResult.incompatible("fail", Set.of("missing"));

      assertTrue(compatible.isCompatible(), "Compatible result should return true");
      assertFalse(incompatible.isCompatible(), "Incompatible result should return false");
    }

    @Test
    @DisplayName("getDetails should return correct value")
    void getDetailsShouldReturnCorrectValue() {
      final WitCompatibilityResult result =
          WitCompatibilityResult.compatible("Detailed message", Set.of());

      assertEquals("Detailed message", result.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("getSatisfiedImports should return unmodifiable set")
    void getSatisfiedImportsShouldReturnUnmodifiableSet() {
      final WitCompatibilityResult result =
          WitCompatibilityResult.compatible("ok", Set.of("wasi:cli/stdio@0.2.0"));

      final Set<String> imports = result.getSatisfiedImports();
      assertNotNull(imports, "Satisfied imports should not be null");

      try {
        imports.add("new-import");
        // If we get here, the set is not unmodifiable
        assertTrue(false, "Set should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        // Expected behavior
        assertTrue(true, "Set is unmodifiable as expected");
      }
    }

    @Test
    @DisplayName("getUnsatisfiedImports should return unmodifiable set")
    void getUnsatisfiedImportsShouldReturnUnmodifiableSet() {
      final WitCompatibilityResult result =
          WitCompatibilityResult.incompatible("fail", Set.of("missing"));

      final Set<String> imports = result.getUnsatisfiedImports();
      assertNotNull(imports, "Unsatisfied imports should not be null");

      try {
        imports.add("new-import");
        assertTrue(false, "Set should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        assertTrue(true, "Set is unmodifiable as expected");
      }
    }
  }

  @Nested
  @DisplayName("hasUnsatisfiedImports Tests")
  class HasUnsatisfiedImportsTests {

    @Test
    @DisplayName("should return true when there are unsatisfied imports")
    void shouldReturnTrueWhenThereAreUnsatisfiedImports() {
      final WitCompatibilityResult result =
          WitCompatibilityResult.incompatible("fail", Set.of("missing1", "missing2"));

      assertTrue(result.hasUnsatisfiedImports(), "Should return true with unsatisfied imports");
    }

    @Test
    @DisplayName("should return false when there are no unsatisfied imports")
    void shouldReturnFalseWhenThereAreNoUnsatisfiedImports() {
      final WitCompatibilityResult result =
          WitCompatibilityResult.compatible("ok", Set.of("satisfied"));

      assertFalse(
          result.hasUnsatisfiedImports(), "Should return false without unsatisfied imports");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final WitCompatibilityResult result =
          WitCompatibilityResult.compatible("All good", Set.of("import1"));

      final String str = result.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitCompatibilityResult"), "Should contain class name");
      assertTrue(str.contains("compatible=true"), "Should contain compatibility status");
      assertTrue(str.contains("All good"), "Should contain details");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty sets")
    void shouldHandleEmptySets() {
      final WitCompatibilityResult result =
          new WitCompatibilityResult(true, "No imports", Set.of(), Set.of());

      assertTrue(result.getSatisfiedImports().isEmpty(), "Satisfied imports should be empty");
      assertTrue(result.getUnsatisfiedImports().isEmpty(), "Unsatisfied imports should be empty");
      assertFalse(result.hasUnsatisfiedImports(), "hasUnsatisfiedImports should return false");
    }

    @Test
    @DisplayName("should handle large number of imports")
    void shouldHandleLargeNumberOfImports() {
      final Set<String> largeSet = new java.util.HashSet<>();
      for (int i = 0; i < 100; i++) {
        largeSet.add("import-" + i);
      }

      final WitCompatibilityResult result =
          WitCompatibilityResult.compatible("Many imports", largeSet);

      assertEquals(100, result.getSatisfiedImports().size(), "Should have 100 satisfied imports");
    }

    @Test
    @DisplayName("should handle null details in constructor")
    void shouldHandleNullDetailsInConstructor() {
      // The class doesn't explicitly prevent null details, test actual behavior
      final WitCompatibilityResult result =
          new WitCompatibilityResult(true, null, Set.of(), Set.of());

      // This tests if the implementation handles null gracefully
      assertNotNull(result, "Result should be created even with null details");
    }
  }
}
