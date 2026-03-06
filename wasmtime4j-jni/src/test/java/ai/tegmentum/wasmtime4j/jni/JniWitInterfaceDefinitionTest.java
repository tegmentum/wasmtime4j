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

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import java.util.Collections;
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
      assertThat(funcNames)
          .as("Each export should produce a function name with -func suffix")
          .containsExactly("greet-func", "add-func");
    }

    @Test
    @DisplayName("type names should be derived from exports with -type suffix")
    void typeNamesShouldHaveTypeSuffix() {
      final Set<String> exports = orderedSet("greet", "add");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("test-iface", "1.0.0", "test:pkg", exports, null);

      final List<String> typeNames = def.getTypeNames();
      assertThat(typeNames)
          .as("Each export should produce a type name with -type suffix")
          .containsExactly("greet-type", "add-type");
    }

    @Test
    @DisplayName("function and type lists should have same size as exports")
    void syntheticListsShouldMatchExportCount() {
      final Set<String> exports = orderedSet("a", "b", "c");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", exports, null);

      assertThat(def.getFunctionNames()).hasSize(3);
      assertThat(def.getTypeNames()).hasSize(3);
    }

    @Test
    @DisplayName("empty exports should produce empty function and type lists")
    void emptyExportsShouldProduceEmptyLists() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(
              "iface", "1.0.0", "pkg", Collections.emptySet(), Collections.emptySet());

      assertThat(def.getFunctionNames()).isEmpty();
      assertThat(def.getTypeNames()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Null-Safe Construction")
  class NullSafeConstruction {

    @Test
    @DisplayName("null name should default to 'unknown'")
    void nullNameShouldDefaultToUnknown() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertThat(def.getName()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("null version should default to '1.0.0'")
    void nullVersionShouldDefaultToOneZeroZero() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertThat(def.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("null packageName should default to 'unknown'")
    void nullPackageNameShouldDefaultToUnknown() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertThat(def.getPackageName()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("null export and import sets should produce empty lists")
    void nullSetsShouldProduceEmptyLists() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, null);

      assertThat(def.getExportNames()).isEmpty();
      assertThat(def.getImportNames()).isEmpty();
      assertThat(def.getFunctionNames()).isEmpty();
      assertThat(def.getTypeNames()).isEmpty();
      assertThat(def.getDependencies()).isEmpty();
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

      assertThat(def.getDependencies())
          .containsExactlyInAnyOrder("wasi:io/streams", "wasi:http/types");
    }

    @Test
    @DisplayName("returned dependencies should be unmodifiable")
    void dependenciesShouldBeUnmodifiable() {
      final Set<String> imports = orderedSet("dep1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, imports);

      assertThat(def.getDependencies()).isUnmodifiable();
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

      assertThat(def.getFunctionNames()).isUnmodifiable();
    }

    @Test
    @DisplayName("getTypeNames should return unmodifiable list")
    void typeNamesShouldBeUnmodifiable() {
      final Set<String> exports = orderedSet("fn1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", exports, null);

      assertThat(def.getTypeNames()).isUnmodifiable();
    }

    @Test
    @DisplayName("getExportNames should return unmodifiable list")
    void exportNamesShouldBeUnmodifiable() {
      final Set<String> exports = orderedSet("fn1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", exports, null);

      assertThat(def.getExportNames()).isUnmodifiable();
    }

    @Test
    @DisplayName("getImportNames should return unmodifiable list")
    void importNamesShouldBeUnmodifiable() {
      final Set<String> imports = orderedSet("dep1");
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, imports);

      assertThat(def.getImportNames()).isUnmodifiable();
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
      assertThat(result.isCompatible())
          .as("Same package and version should be compatible")
          .isTrue();
    }

    @Test
    @DisplayName("different packages should be incompatible")
    void differentPackagesShouldBeIncompatible() {
      final JniWitInterfaceDefinition def1 =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg-a", null, null);
      final JniWitInterfaceDefinition def2 =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg-b", null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);
      assertThat(result.isCompatible()).as("Different packages should be incompatible").isFalse();
    }

    @Test
    @DisplayName("different versions should be incompatible")
    void differentVersionsShouldBeIncompatible() {
      final JniWitInterfaceDefinition def1 =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, null);
      final JniWitInterfaceDefinition def2 =
          new JniWitInterfaceDefinition("iface", "2.0.0", "pkg", null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);
      assertThat(result.isCompatible()).as("Different versions should be incompatible").isFalse();
    }

    @Test
    @DisplayName("null other interface should be incompatible")
    void nullOtherShouldBeIncompatible() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("iface", "1.0.0", "pkg", null, null);

      final WitCompatibilityResult result = def.isCompatibleWith(null);
      assertThat(result.isCompatible()).as("Null other should be incompatible").isFalse();
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
      assertThat(witText).contains("interface my-iface {");
      assertThat(witText).contains("hello-func() -> ();");
      assertThat(witText).contains("type hello-type = string;");
      assertThat(witText).endsWith("}\n");
    }

    @Test
    @DisplayName("getWitText with no exports should produce empty interface body")
    void witTextWithNoExportsShouldBeEmpty() {
      final JniWitInterfaceDefinition def =
          new JniWitInterfaceDefinition("empty", "1.0.0", "pkg", null, null);

      final String witText = def.getWitText();
      assertThat(witText).isEqualTo("interface empty {\n}\n");
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
      assertThat(str).contains("my-iface");
      assertThat(str).contains("2.0.0");
      assertThat(str).contains("my:pkg");
      assertThat(str).contains("functionCount=2");
      assertThat(str).contains("typeCount=2");
      assertThat(str).contains("dependencyCount=1");
    }
  }

  /** Creates an ordered set preserving insertion order for deterministic test assertions. */
  private static Set<String> orderedSet(final String... values) {
    final Set<String> set = new LinkedHashSet<>();
    Collections.addAll(set, values);
    return set;
  }
}
