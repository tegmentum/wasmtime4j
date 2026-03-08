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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for JniWitInterfaceDefinition.
 *
 * <p>Verifies synthetic naming behavior, null-safe construction, compatibility checking, and WIT
 * text generation.
 */
@DisplayName("JniWitInterfaceDefinition Tests")
class JniWitInterfaceDefinitionTest {

  @Nested
  @DisplayName("Synthetic Naming")
  class SyntheticNaming {

    @Test
    @DisplayName("function names should be derived from exports with -func suffix")
    void functionNamesShouldHaveFuncSuffix() {
      final Set<String> exports = orderedSet("greet", "add");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("test-iface", "1.0.0", "test:pkg", exports, null);

      final List<String> funcNames = def.getFunctionNames();
      assertEquals(
          Arrays.asList("greet-func", "add-func"),
          funcNames,
          "Each export should produce a function name with -func suffix");
    }

    @Test
    @DisplayName("type names should be derived from exports with -type suffix")
    void typeNamesShouldHaveTypeSuffix() {
      final Set<String> exports = orderedSet("greet", "add");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("test-iface", "1.0.0", "test:pkg", exports, null);

      final List<String> typeNames = def.getTypeNames();
      assertEquals(
          Arrays.asList("greet-type", "add-type"),
          typeNames,
          "Each export should produce a type name with -type suffix");
    }

    @Test
    @DisplayName("function and type lists should have same size as exports")
    void syntheticListsShouldMatchExportCount() {
      final Set<String> exports = orderedSet("a", "b", "c");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", exports, null);

      assertEquals(3, def.getFunctionNames().size());
      assertEquals(3, def.getTypeNames().size());
    }

    @Test
    @DisplayName("empty exports should produce empty function and type lists")
    void emptyExportsShouldProduceEmptyLists() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(
              "iface", "1.0.0", "pkg", Collections.emptySet(), Collections.emptySet());

      assertTrue(def.getFunctionNames().isEmpty());
      assertTrue(def.getTypeNames().isEmpty());
    }
  }

  @Nested
  @DisplayName("Null-Safe Construction")
  class NullSafeConstruction {

    @Test
    @DisplayName("null name should default to empty string")
    void nullNameShouldDefaultToEmpty() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertEquals("", def.getName());
    }

    @Test
    @DisplayName("null version should default to '0.0.0'")
    void nullVersionShouldDefaultToZeroZeroZero() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertEquals("0.0.0", def.getVersion());
    }

    @Test
    @DisplayName("null packageName should default to empty string")
    void nullPackageNameShouldDefaultToEmpty() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertEquals("", def.getPackageName());
    }

    @Test
    @DisplayName("null export and import sets should produce empty lists")
    void nullSetsShouldProduceEmptyLists() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, null);

      assertTrue(def.getExportNames().isEmpty());
      assertTrue(def.getImportNames().isEmpty());
      assertTrue(def.getFunctionNames().isEmpty());
      assertTrue(def.getTypeNames().isEmpty());
      assertTrue(def.getDependencies().isEmpty());
    }
  }

  @Nested
  @DisplayName("Dependencies")
  class Dependencies {

    @Test
    @DisplayName("dependencies should be derived from imports")
    void dependenciesShouldMatchImports() {
      final Set<String> imports = orderedSet("wasi:io/streams", "wasi:http/types");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, imports);

      final Set<String> expected = new HashSet<>();
      expected.add("wasi:io/streams");
      expected.add("wasi:http/types");
      assertEquals(expected, new HashSet<>(def.getDependencies()));
      assertEquals(2, def.getDependencies().size());
    }

    @Test
    @DisplayName("returned dependencies should be unmodifiable")
    void dependenciesShouldBeUnmodifiable() {
      final Set<String> imports = orderedSet("dep1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, imports);

      assertThrows(UnsupportedOperationException.class, () -> def.getDependencies().add(null));
    }
  }

  @Nested
  @DisplayName("Immutability")
  class Immutability {

    @Test
    @DisplayName("getFunctionNames should return unmodifiable list")
    void functionNamesShouldBeUnmodifiable() {
      final Set<String> exports = orderedSet("fn1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", exports, null);

      assertThrows(UnsupportedOperationException.class, () -> def.getFunctionNames().add(null));
    }

    @Test
    @DisplayName("getTypeNames should return unmodifiable list")
    void typeNamesShouldBeUnmodifiable() {
      final Set<String> exports = orderedSet("fn1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", exports, null);

      assertThrows(UnsupportedOperationException.class, () -> def.getTypeNames().add(null));
    }

    @Test
    @DisplayName("getExportNames should return unmodifiable list")
    void exportNamesShouldBeUnmodifiable() {
      final Set<String> exports = orderedSet("fn1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", exports, null);

      assertThrows(UnsupportedOperationException.class, () -> def.getExportNames().add(null));
    }

    @Test
    @DisplayName("getImportNames should return unmodifiable list")
    void importNamesShouldBeUnmodifiable() {
      final Set<String> imports = orderedSet("dep1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, imports);

      assertThrows(UnsupportedOperationException.class, () -> def.getImportNames().add(null));
    }
  }

  @Nested
  @DisplayName("Compatibility")
  class Compatibility {

    @Test
    @DisplayName("compatible interfaces should return compatible result")
    void samePackageAndVersionShouldBeCompatible() {
      final JniWitInterfaceDefinition def1 =
          new JniWitInterfaceDefinition("iface", "1.0.0", "test:pkg", null, null);
      final JniWitInterfaceDefinition def2 =
          new JniWitInterfaceDefinition("iface", "1.0.0", "test:pkg", null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);
      assertTrue(result.isCompatible(), "Same package and version should be compatible");
    }

    @Test
    @DisplayName("different names should be incompatible")
    void differentNamesShouldBeIncompatible() {
      final JniWitInterfaceDefinition def1 =
          new JniWitInterfaceDefinition("iface-a", "1.0.0", "pkg", null, null);
      final JniWitInterfaceDefinition def2 =
          new JniWitInterfaceDefinition("iface-b", "1.0.0", "pkg", null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);
      assertFalse(result.isCompatible(), "Different names should be incompatible");
    }

    @Test
    @DisplayName("different versions should be incompatible")
    void differentVersionsShouldBeIncompatible() {
      final JniWitInterfaceDefinition def1 =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, null);
      final JniWitInterfaceDefinition def2 =
          new JniWitInterfaceDefinition("iface", "2.0.0", "pkg", null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);
      assertFalse(result.isCompatible(), "Different versions should be incompatible");
    }

    @Test
    @DisplayName("null other interface should be incompatible")
    void nullOtherShouldBeIncompatible() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, null);

      final WitCompatibilityResult result = def.isCompatibleWith(null);
      assertFalse(result.isCompatible(), "Null other should be incompatible");
    }
  }

  @Nested
  @DisplayName("WIT Text Generation")
  class WitTextGeneration {

    @Test
    @DisplayName("getWitText should include synthetic function and type names")
    void witTextShouldContainSyntheticNames() {
      final Set<String> exports = orderedSet("hello");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("my-iface", "1.0.0", "pkg", exports, null);

      final String witText = def.getWitText();
      assertTrue(
          witText.contains("interface my-iface {"),
          "Expected WIT text to contain: interface my-iface {");
      assertTrue(
          witText.contains("hello-func() -> ();"),
          "Expected WIT text to contain: hello-func() -> ();");
      assertTrue(
          witText.contains("type hello-type = string;"),
          "Expected WIT text to contain: type hello-type = string;");
      assertTrue(witText.endsWith("}\n"), "Expected WIT text to end with: }\\n");
    }

    @Test
    @DisplayName("getWitText with no exports should produce empty interface body")
    void witTextWithNoExportsShouldBeEmpty() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("empty", "1.0.0", "pkg", null, null);

      final String witText = def.getWitText();
      assertEquals("interface empty {\n}\n", witText);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("toString should include name, version, package, and counts")
    void toStringShouldContainKeyFields() {
      final Set<String> exports = orderedSet("fn1", "fn2");
      final Set<String> imports = orderedSet("dep1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("my-iface", "2.0.0", "my:pkg", exports, imports);

      final String str = def.toString();
      assertTrue(str.contains("my-iface"), "Expected string to contain: my-iface");
      assertTrue(str.contains("2.0.0"), "Expected string to contain: 2.0.0");
      assertTrue(str.contains("my:pkg"), "Expected string to contain: my:pkg");
      assertTrue(str.contains("functionCount=2"), "Expected string to contain: functionCount=2");
      assertTrue(str.contains("typeCount=2"), "Expected string to contain: typeCount=2");
      assertTrue(
          str.contains("dependencyCount=1"), "Expected string to contain: dependencyCount=1");
    }
  }

  /** Creates an ordered set preserving insertion order for deterministic test assertions. */
  private static Set<String> orderedSet(final String... values) {
    final Set<String> set = new LinkedHashSet<>();
    Collections.addAll(set, values);
    return set;
  }
}
