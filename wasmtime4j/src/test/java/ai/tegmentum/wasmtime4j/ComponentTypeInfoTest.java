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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentTypeInfo;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentTypeInfo} class.
 *
 * <p>ComponentTypeInfo represents a frozen snapshot of a component's type-level information,
 * including all imports and exports. It corresponds to Wasmtime's {@code types::Component} type.
 */
@DisplayName("ComponentTypeInfo Tests")
class ComponentTypeInfoTest {

  private static Set<String> setOf(final String... values) {
    final Set<String> set = new LinkedHashSet<>();
    Collections.addAll(set, values);
    return set;
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with empty imports and exports")
    void shouldCreateInstanceWithEmptySets() {
      final ComponentTypeInfo info =
          new ComponentTypeInfo(Collections.emptySet(), Collections.emptySet());

      assertNotNull(info, "ComponentTypeInfo should be created");
      assertTrue(info.imports().isEmpty(), "Imports should be empty");
      assertTrue(info.exports().isEmpty(), "Exports should be empty");
    }

    @Test
    @DisplayName("should create instance with populated imports and exports")
    void shouldCreateInstanceWithPopulatedSets() {
      final Set<String> imports = setOf("wasi:cli/stdin@0.2.0", "wasi:cli/stdout@0.2.0");
      final Set<String> exports = setOf("run", "wasi:http/handler@0.2.0");

      final ComponentTypeInfo info = new ComponentTypeInfo(imports, exports);

      assertEquals(2, info.imports().size(), "Should have 2 imports");
      assertEquals(2, info.exports().size(), "Should have 2 exports");
    }

    @Test
    @DisplayName("should throw NullPointerException when imports is null")
    void shouldThrowWhenImportsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentTypeInfo(null, Collections.emptySet()),
          "Should throw NullPointerException for null imports");
    }

    @Test
    @DisplayName("should throw NullPointerException when exports is null")
    void shouldThrowWhenExportsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentTypeInfo(Collections.emptySet(), null),
          "Should throw NullPointerException for null exports");
    }

    @Test
    @DisplayName("should defensively copy input sets")
    void shouldDefensivelyCopyInputSets() {
      final Set<String> imports = new LinkedHashSet<>();
      imports.add("wasi:cli/stdin@0.2.0");
      final Set<String> exports = new LinkedHashSet<>();
      exports.add("run");

      final ComponentTypeInfo info = new ComponentTypeInfo(imports, exports);

      // Modify originals
      imports.add("added-later");
      exports.add("also-added");

      assertEquals(1, info.imports().size(), "Imports should not be affected by original mutation");
      assertEquals(1, info.exports().size(), "Exports should not be affected by original mutation");
    }

    @Test
    @DisplayName("should return unmodifiable sets")
    void shouldReturnUnmodifiableSets() {
      final ComponentTypeInfo info = new ComponentTypeInfo(setOf("import1"), setOf("export1"));

      assertThrows(
          UnsupportedOperationException.class,
          () -> info.imports().add("new-import"),
          "Imports set should be unmodifiable");
      assertThrows(
          UnsupportedOperationException.class,
          () -> info.exports().add("new-export"),
          "Exports set should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("Query Tests")
  class QueryTests {

    private final ComponentTypeInfo info =
        new ComponentTypeInfo(
            setOf("wasi:cli/stdin@0.2.0", "wasi:cli/stdout@0.2.0"),
            setOf("run", "wasi:http/handler@0.2.0"));

    @Test
    @DisplayName("hasImport should return true for existing import")
    void hasImportShouldReturnTrueForExisting() {
      assertTrue(info.hasImport("wasi:cli/stdin@0.2.0"), "Should find existing import");
    }

    @Test
    @DisplayName("hasImport should return false for non-existing import")
    void hasImportShouldReturnFalseForNonExisting() {
      assertFalse(
          info.hasImport("wasi:filesystem/types@0.2.0"), "Should not find non-existing import");
    }

    @Test
    @DisplayName("hasImport should throw for null name")
    void hasImportShouldThrowForNull() {
      assertThrows(
          NullPointerException.class,
          () -> info.hasImport(null),
          "Should throw NullPointerException for null name");
    }

    @Test
    @DisplayName("hasExport should return true for existing export")
    void hasExportShouldReturnTrueForExisting() {
      assertTrue(info.hasExport("run"), "Should find existing export");
    }

    @Test
    @DisplayName("hasExport should return false for non-existing export")
    void hasExportShouldReturnFalseForNonExisting() {
      assertFalse(info.hasExport("nonexistent"), "Should not find non-existing export");
    }

    @Test
    @DisplayName("hasExport should throw for null name")
    void hasExportShouldThrowForNull() {
      assertThrows(
          NullPointerException.class,
          () -> info.hasExport(null),
          "Should throw NullPointerException for null name");
    }

    @Test
    @DisplayName("getImport should return present Optional for existing import")
    void getImportShouldReturnPresentForExisting() {
      final Optional<String> result = info.getImport("wasi:cli/stdout@0.2.0");

      assertTrue(result.isPresent(), "Should return present Optional");
      assertEquals("wasi:cli/stdout@0.2.0", result.get(), "Should return the import name");
    }

    @Test
    @DisplayName("getImport should return empty Optional for non-existing import")
    void getImportShouldReturnEmptyForNonExisting() {
      final Optional<String> result = info.getImport("nonexistent");

      assertTrue(result.isEmpty(), "Should return empty Optional for non-existing import");
    }

    @Test
    @DisplayName("getImport should throw for null name")
    void getImportShouldThrowForNull() {
      assertThrows(
          NullPointerException.class,
          () -> info.getImport(null),
          "Should throw NullPointerException for null name");
    }

    @Test
    @DisplayName("getExport should return present Optional for existing export")
    void getExportShouldReturnPresentForExisting() {
      final Optional<String> result = info.getExport("run");

      assertTrue(result.isPresent(), "Should return present Optional");
      assertEquals("run", result.get(), "Should return the export name");
    }

    @Test
    @DisplayName("getExport should return empty Optional for non-existing export")
    void getExportShouldReturnEmptyForNonExisting() {
      final Optional<String> result = info.getExport("nonexistent");

      assertTrue(result.isEmpty(), "Should return empty Optional for non-existing export");
    }

    @Test
    @DisplayName("getExport should throw for null name")
    void getExportShouldThrowForNull() {
      assertThrows(
          NullPointerException.class,
          () -> info.getExport(null),
          "Should throw NullPointerException for null name");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final ComponentTypeInfo info = new ComponentTypeInfo(setOf("import1"), setOf("export1"));

      assertEquals(info, info, "Should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to another instance with same data")
    void shouldBeEqualToSameData() {
      final ComponentTypeInfo info1 = new ComponentTypeInfo(setOf("import1"), setOf("export1"));
      final ComponentTypeInfo info2 = new ComponentTypeInfo(setOf("import1"), setOf("export1"));

      assertEquals(info1, info2, "Should be equal with same imports and exports");
    }

    @Test
    @DisplayName("should not be equal when imports differ")
    void shouldNotBeEqualWhenImportsDiffer() {
      final ComponentTypeInfo info1 = new ComponentTypeInfo(setOf("import1"), setOf("export1"));
      final ComponentTypeInfo info2 = new ComponentTypeInfo(setOf("import2"), setOf("export1"));

      assertNotEquals(info1, info2, "Should not be equal with different imports");
    }

    @Test
    @DisplayName("should not be equal when exports differ")
    void shouldNotBeEqualWhenExportsDiffer() {
      final ComponentTypeInfo info1 = new ComponentTypeInfo(setOf("import1"), setOf("export1"));
      final ComponentTypeInfo info2 = new ComponentTypeInfo(setOf("import1"), setOf("export2"));

      assertNotEquals(info1, info2, "Should not be equal with different exports");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final ComponentTypeInfo info =
          new ComponentTypeInfo(Collections.emptySet(), Collections.emptySet());

      assertNotEquals(null, info, "Should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final ComponentTypeInfo info =
          new ComponentTypeInfo(Collections.emptySet(), Collections.emptySet());

      assertNotEquals("string", info, "Should not be equal to a String");
    }

    @Test
    @DisplayName("equal instances should have same hashCode")
    void equalInstancesShouldHaveSameHashCode() {
      final ComponentTypeInfo info1 = new ComponentTypeInfo(setOf("a", "b"), setOf("x", "y"));
      final ComponentTypeInfo info2 = new ComponentTypeInfo(setOf("a", "b"), setOf("x", "y"));

      assertEquals(
          info1.hashCode(), info2.hashCode(), "Equal instances should have the same hashCode");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain ComponentTypeInfo prefix")
    void toStringShouldContainPrefix() {
      final ComponentTypeInfo info =
          new ComponentTypeInfo(Collections.emptySet(), Collections.emptySet());

      assertTrue(
          info.toString().startsWith("ComponentTypeInfo{"),
          "toString should start with 'ComponentTypeInfo{'");
    }

    @Test
    @DisplayName("toString should contain import and export counts")
    void toStringShouldContainCounts() {
      final ComponentTypeInfo info = new ComponentTypeInfo(setOf("i1", "i2"), setOf("e1"));

      final String str = info.toString();

      assertTrue(str.contains("imports=2"), "toString should contain 'imports=2'");
      assertTrue(str.contains("exports=1"), "toString should contain 'exports=1'");
    }
  }
}
