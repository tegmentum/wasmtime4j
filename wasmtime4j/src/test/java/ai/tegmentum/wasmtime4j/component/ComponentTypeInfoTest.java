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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentTypeInfo}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentTypeInfo")
class ComponentTypeInfoTest {

  @Nested
  @DisplayName("name-only constructor")
  class NameOnlyConstructor {

    @Test
    @DisplayName("creates with imports and exports")
    void createsWithImportsAndExports() {
      final ComponentTypeInfo info =
          new ComponentTypeInfo(Set.of("wasi:cli/stdout"), Set.of("run"));
      assertEquals(Set.of("wasi:cli/stdout"), info.imports());
      assertEquals(Set.of("run"), info.exports());
      assertTrue(info.importItems().isEmpty());
      assertTrue(info.exportItems().isEmpty());
    }

    @Test
    @DisplayName("rejects null imports")
    void rejectsNullImports() {
      assertThrows(NullPointerException.class, () -> new ComponentTypeInfo(null, Set.of()));
    }

    @Test
    @DisplayName("rejects null exports")
    void rejectsNullExports() {
      assertThrows(NullPointerException.class, () -> new ComponentTypeInfo(Set.of(), null));
    }

    @Test
    @DisplayName("imports and exports are unmodifiable")
    void unmodifiable() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of("a"), Set.of("b"));
      assertThrows(UnsupportedOperationException.class, () -> info.imports().add("c"));
      assertThrows(UnsupportedOperationException.class, () -> info.exports().add("c"));
    }
  }

  @Nested
  @DisplayName("typed constructor")
  class TypedConstructor {

    @Test
    @DisplayName("creates with typed items")
    void createsWithTypedItems() {
      final ComponentItemInfo.ModuleInfo modInfo = new ComponentItemInfo.ModuleInfo("inner");
      final ComponentTypeInfo info =
          new ComponentTypeInfo(Map.of("import1", modInfo), Map.of("export1", modInfo));
      assertEquals(Set.of("import1"), info.imports());
      assertEquals(Set.of("export1"), info.exports());
      assertTrue(info.getImportItem("import1").isPresent());
      assertTrue(info.getExportItem("export1").isPresent());
    }

    @Test
    @DisplayName("rejects null importItems")
    void rejectsNullImportItems() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ComponentTypeInfo((Map<String, ComponentItemInfo>) null, Collections.emptyMap()));
    }

    @Test
    @DisplayName("rejects null exportItems")
    void rejectsNullExportItems() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ComponentTypeInfo(Collections.emptyMap(), (Map<String, ComponentItemInfo>) null));
    }
  }

  @Nested
  @DisplayName("lookup methods")
  class LookupMethods {

    @Test
    @DisplayName("hasImport returns true for existing import")
    void hasImportTrue() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of("a"), Set.of());
      assertTrue(info.hasImport("a"));
    }

    @Test
    @DisplayName("hasImport returns false for missing import")
    void hasImportFalse() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of(), Set.of());
      assertFalse(info.hasImport("missing"));
    }

    @Test
    @DisplayName("hasExport returns true for existing export")
    void hasExportTrue() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of(), Set.of("run"));
      assertTrue(info.hasExport("run"));
    }

    @Test
    @DisplayName("hasExport returns false for missing export")
    void hasExportFalse() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of(), Set.of());
      assertFalse(info.hasExport("missing"));
    }

    @Test
    @DisplayName("getImport returns Optional")
    void getImportOptional() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of("a"), Set.of());
      assertTrue(info.getImport("a").isPresent());
      assertFalse(info.getImport("b").isPresent());
    }

    @Test
    @DisplayName("getExport returns Optional")
    void getExportOptional() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of(), Set.of("run"));
      assertTrue(info.getExport("run").isPresent());
      assertFalse(info.getExport("missing").isPresent());
    }

    @Test
    @DisplayName("null name throws")
    void nullNameThrows() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of(), Set.of());
      assertThrows(NullPointerException.class, () -> info.hasImport(null));
      assertThrows(NullPointerException.class, () -> info.hasExport(null));
      assertThrows(NullPointerException.class, () -> info.getImport(null));
      assertThrows(NullPointerException.class, () -> info.getExport(null));
      assertThrows(NullPointerException.class, () -> info.getImportItem(null));
      assertThrows(NullPointerException.class, () -> info.getExportItem(null));
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equal instances")
    void equalInstances() {
      final ComponentTypeInfo i1 = new ComponentTypeInfo(Set.of("a"), Set.of("b"));
      final ComponentTypeInfo i2 = new ComponentTypeInfo(Set.of("a"), Set.of("b"));
      assertEquals(i1, i2);
      assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    @DisplayName("different imports not equal")
    void differentImportsNotEqual() {
      final ComponentTypeInfo i1 = new ComponentTypeInfo(Set.of("a"), Set.of("b"));
      final ComponentTypeInfo i2 = new ComponentTypeInfo(Set.of("c"), Set.of("b"));
      assertNotEquals(i1, i2);
    }

    @Test
    @DisplayName("equal to self")
    void equalToSelf() {
      final ComponentTypeInfo i1 = new ComponentTypeInfo(Set.of(), Set.of());
      assertEquals(i1, i1);
    }

    @Test
    @DisplayName("not equal to null")
    void notEqualToNull() {
      assertNotEquals(null, new ComponentTypeInfo(Set.of(), Set.of()));
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("includes counts")
    void includesCounts() {
      final ComponentTypeInfo info = new ComponentTypeInfo(Set.of("a", "b"), Set.of("c"));
      final String str = info.toString();
      assertTrue(str.contains("imports=2"));
      assertTrue(str.contains("exports=1"));
    }
  }
}
