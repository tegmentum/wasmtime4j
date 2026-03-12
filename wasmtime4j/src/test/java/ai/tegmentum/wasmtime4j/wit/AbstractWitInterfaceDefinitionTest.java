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
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AbstractWitInterfaceDefinition}. */
@DisplayName("AbstractWitInterfaceDefinition Tests")
class AbstractWitInterfaceDefinitionTest {

  /**
   * Concrete test subclass of AbstractWitInterfaceDefinition that provides a minimal implementation
   * of the abstract class for testing purposes.
   */
  private static final class TestWitInterfaceDefinition extends AbstractWitInterfaceDefinition {

    TestWitInterfaceDefinition(
        final String name,
        final String version,
        final String packageName,
        final List<String> functionNames,
        final List<String> typeNames,
        final Set<String> dependencies,
        final List<String> importNames,
        final List<String> exportNames) {
      super(
          name,
          version,
          packageName,
          functionNames,
          typeNames,
          dependencies,
          importNames,
          exportNames);
    }

    @Override
    public String getWitText() {
      return "interface " + getName() + " {}";
    }
  }

  private TestWitInterfaceDefinition createDefinition(
      final String name,
      final String version,
      final String packageName,
      final List<String> functionNames,
      final List<String> typeNames,
      final Set<String> dependencies,
      final List<String> importNames,
      final List<String> exportNames) {
    return new TestWitInterfaceDefinition(
        name,
        version,
        packageName,
        functionNames,
        typeNames,
        dependencies,
        importNames,
        exportNames);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with all nulls should use defaults")
    void constructorWithAllNullsShouldUseDefaults() {
      final TestWitInterfaceDefinition def =
          createDefinition(null, null, null, null, null, null, null, null);

      assertEquals("", def.getName());
      assertEquals("0.0.0", def.getVersion());
      assertEquals("", def.getPackageName());
      assertTrue(def.getFunctionNames().isEmpty());
      assertTrue(def.getTypeNames().isEmpty());
      assertTrue(def.getDependencies().isEmpty());
      assertTrue(def.getImportNames().isEmpty());
      assertTrue(def.getExportNames().isEmpty());
    }

    @Test
    @DisplayName("Constructor with actual values should return correct getters")
    void constructorWithActualValues() {
      final List<String> functions = List.of("func1", "func2");
      final List<String> types = List.of("type1", "type2");
      final Set<String> deps = Set.of("dep1", "dep2");
      final List<String> imports = List.of("import1");
      final List<String> exports = List.of("export1");

      final TestWitInterfaceDefinition def =
          createDefinition("my-iface", "1.2.3", "my:pkg", functions, types, deps, imports, exports);

      assertEquals("my-iface", def.getName());
      assertEquals("1.2.3", def.getVersion());
      assertEquals("my:pkg", def.getPackageName());
      assertEquals(2, def.getFunctionNames().size());
      assertTrue(def.getFunctionNames().contains("func1"));
      assertTrue(def.getFunctionNames().contains("func2"));
      assertEquals(2, def.getTypeNames().size());
      assertTrue(def.getTypeNames().contains("type1"));
      assertTrue(def.getTypeNames().contains("type2"));
      assertEquals(2, def.getDependencies().size());
      assertTrue(def.getDependencies().contains("dep1"));
      assertTrue(def.getDependencies().contains("dep2"));
      assertEquals(1, def.getImportNames().size());
      assertTrue(def.getImportNames().contains("import1"));
      assertEquals(1, def.getExportNames().size());
      assertTrue(def.getExportNames().contains("export1"));
    }

    @Test
    @DisplayName("Constructor defensively copies input lists")
    void constructorDefensivelyCopiesInputLists() {
      final ArrayList<String> functions = new ArrayList<>(List.of("func1"));
      final ArrayList<String> types = new ArrayList<>(List.of("type1"));
      final HashSet<String> deps = new HashSet<>(Set.of("dep1"));
      final ArrayList<String> imports = new ArrayList<>(List.of("import1"));
      final ArrayList<String> exports = new ArrayList<>(List.of("export1"));

      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", functions, types, deps, imports, exports);

      // Mutate the original collections
      functions.add("func2");
      types.add("type2");
      deps.add("dep2");
      imports.add("import2");
      exports.add("export2");

      // Verify that the definition is not affected
      assertEquals(1, def.getFunctionNames().size());
      assertEquals(1, def.getTypeNames().size());
      assertEquals(1, def.getDependencies().size());
      assertEquals(1, def.getImportNames().size());
      assertEquals(1, def.getExportNames().size());
    }
  }

  @Nested
  @DisplayName("Unmodifiable Collection Tests")
  class UnmodifiableCollectionTests {

    @Test
    @DisplayName("getFunctionNames returns unmodifiable list")
    void getFunctionNamesReturnsUnmodifiableList() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", List.of("func1"), null, null, null, null);

      assertThrows(UnsupportedOperationException.class, () -> def.getFunctionNames().add("x"));
    }

    @Test
    @DisplayName("getTypeNames returns unmodifiable list")
    void getTypeNamesReturnsUnmodifiableList() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, List.of("type1"), null, null, null);

      assertThrows(UnsupportedOperationException.class, () -> def.getTypeNames().add("x"));
    }

    @Test
    @DisplayName("getDependencies returns unmodifiable set")
    void getDependenciesReturnsUnmodifiableSet() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, Set.of("dep1"), null, null);

      assertThrows(UnsupportedOperationException.class, () -> def.getDependencies().add("x"));
    }

    @Test
    @DisplayName("getImportNames returns unmodifiable list")
    void getImportNamesReturnsUnmodifiableList() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, List.of("import1"), null);

      assertThrows(UnsupportedOperationException.class, () -> def.getImportNames().add("x"));
    }

    @Test
    @DisplayName("getExportNames returns unmodifiable list")
    void getExportNamesReturnsUnmodifiableList() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, List.of("export1"));

      assertThrows(UnsupportedOperationException.class, () -> def.getExportNames().add("x"));
    }
  }

  @Nested
  @DisplayName("isCompatibleWith Tests")
  class CompatibilityTests {

    @Test
    @DisplayName("isCompatibleWith null returns incompatible")
    void isCompatibleWithNullReturnsIncompatible() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def.isCompatibleWith(null);

      assertFalse(result.isCompatible());
      assertTrue(result.getDetails().contains("null"));
    }

    @Test
    @DisplayName("isCompatibleWith name mismatch returns incompatible")
    void isCompatibleWithNameMismatchReturnsIncompatible() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface-a", "1.0.0", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface-b", "1.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
      assertTrue(result.getDetails().contains("iface-a"));
      assertTrue(result.getDetails().contains("iface-b"));
    }

    @Test
    @DisplayName("isCompatibleWith major version mismatch returns incompatible")
    void isCompatibleWithMajorVersionMismatchReturnsIncompatible() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "2.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
      assertTrue(result.getDetails().contains("1.0.0"));
      assertTrue(result.getDetails().contains("2.0.0"));
    }

    @Test
    @DisplayName("isCompatibleWith non-numeric version skips version check")
    void isCompatibleWithNonNumericVersionSkipsVersionCheck() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface", "alpha.0.0", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "beta.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertTrue(result.isCompatible());
    }

    @Test
    @DisplayName("isCompatibleWith missing functions returns incompatible")
    void isCompatibleWithMissingFunctionsReturnsIncompatible() {
      final TestWitInterfaceDefinition def1 =
          createDefinition(
              "iface", "1.0.0", "pkg", List.of("func1", "func2"), null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "1.0.0", "pkg", List.of("func1"), null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
      assertTrue(result.getUnsatisfiedImports().contains("function:func2"));
      assertFalse(result.getUnsatisfiedImports().contains("function:func1"));
    }

    @Test
    @DisplayName("isCompatibleWith missing types returns incompatible")
    void isCompatibleWithMissingTypesReturnsIncompatible() {
      final TestWitInterfaceDefinition def1 =
          createDefinition(
              "iface", "1.0.0", "pkg", null, List.of("type1", "type2"), null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "1.0.0", "pkg", null, List.of("type1"), null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
      assertTrue(result.getUnsatisfiedImports().contains("type:type2"));
      assertFalse(result.getUnsatisfiedImports().contains("type:type1"));
    }

    @Test
    @DisplayName("isCompatibleWith both missing functions and types returns incompatible")
    void isCompatibleWithBothMissingFunctionsAndTypesReturnsIncompatible() {
      final TestWitInterfaceDefinition def1 =
          createDefinition(
              "iface", "1.0.0", "pkg", List.of("func1"), List.of("type1"), null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
      assertTrue(result.getUnsatisfiedImports().contains("function:func1"));
      assertTrue(result.getUnsatisfiedImports().contains("type:type1"));
      assertEquals(2, result.getUnsatisfiedImports().size());
      assertTrue(result.getDetails().contains("Missing"));
    }

    @Test
    @DisplayName("isCompatibleWith compatible case returns compatible")
    void isCompatibleWithCompatibleCase() {
      final TestWitInterfaceDefinition def1 =
          createDefinition(
              "iface", "1.0.0", "pkg", List.of("func1"), List.of("type1"), null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition(
              "iface",
              "1.0.0",
              "pkg",
              List.of("func1", "func2"),
              List.of("type1", "type2"),
              null,
              null,
              null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertTrue(result.isCompatible());
      assertFalse(result.hasUnsatisfiedImports());
    }

    @Test
    @DisplayName("isCompatibleWith same major but different minor version is compatible")
    void isCompatibleWithSameMajorDifferentMinorIsCompatible() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "1.5.3", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertTrue(result.isCompatible());
    }

    @Test
    @DisplayName("isCompatibleWith satisfied imports contain function and type names")
    void isCompatibleWithSatisfiedImportsContainFunctionAndTypeNames() {
      final TestWitInterfaceDefinition def1 =
          createDefinition(
              "iface",
              "1.0.0",
              "pkg",
              List.of("func1", "func2"),
              List.of("type1"),
              null,
              null,
              null);
      final TestWitInterfaceDefinition def2 =
          createDefinition(
              "iface",
              "1.0.0",
              "pkg",
              List.of("func1", "func2"),
              List.of("type1"),
              null,
              null,
              null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertTrue(result.isCompatible());
      final Set<String> satisfied = result.getSatisfiedImports();
      assertEquals(3, satisfied.size());
      assertTrue(satisfied.contains("func1"));
      assertTrue(satisfied.contains("func2"));
      assertTrue(satisfied.contains("type1"));
    }

    @Test
    @DisplayName("isCompatibleWith unsatisfied imports contain prefixed names")
    void isCompatibleWithUnsatisfiedImportsContainPrefixedNames() {
      final TestWitInterfaceDefinition def1 =
          createDefinition(
              "iface",
              "1.0.0",
              "pkg",
              List.of("doSomething"),
              List.of("MyRecord"),
              null,
              null,
              null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
      assertTrue(result.getUnsatisfiedImports().contains("function:doSomething"));
      assertTrue(result.getUnsatisfiedImports().contains("type:MyRecord"));
    }

    @Test
    @DisplayName("isCompatibleWith single component version like '1'")
    void isCompatibleWithSingleComponentVersion() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface", "1", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "1", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertTrue(result.isCompatible());
    }

    @Test
    @DisplayName("isCompatibleWith single component version mismatch is incompatible")
    void isCompatibleWithSingleComponentVersionMismatch() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface", "1", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "2", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
    }

    @Test
    @DisplayName("isCompatibleWith empty functions and types is compatible")
    void isCompatibleWithEmptyFunctionsAndTypesIsCompatible() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface", "1.0.0", "pkg", List.of(), List.of(), null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition(
              "iface", "1.0.0", "pkg", List.of("func1"), List.of("type1"), null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertTrue(result.isCompatible());
      assertTrue(result.getSatisfiedImports().isEmpty());
    }

    @Test
    @DisplayName("isCompatibleWith only one non-numeric version part skips version check")
    void isCompatibleWithOnlyOneNonNumericVersionSkipsCheck() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface", "alpha.0.0", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      // Non-numeric first version means NumberFormatException is caught, version check skipped
      assertTrue(result.isCompatible());
    }

    @Test
    @DisplayName("isCompatibleWith incompatible result has empty satisfied imports")
    void incompatibleResultHasEmptySatisfiedImports() {
      final TestWitInterfaceDefinition def1 =
          createDefinition("iface-a", "1.0.0", "pkg", null, null, null, null, null);
      final TestWitInterfaceDefinition def2 =
          createDefinition("iface-b", "1.0.0", "pkg", null, null, null, null, null);

      final WitCompatibilityResult result = def1.isCompatibleWith(def2);

      assertFalse(result.isCompatible());
      assertTrue(result.getSatisfiedImports().isEmpty());
    }
  }

  @Nested
  @DisplayName("Mutable Accessor Tests")
  class MutableAccessorTests {

    @Test
    @DisplayName("mutableFunctionNames allows modification")
    void mutableFunctionNamesAllowsModification() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", List.of("func1"), null, null, null, null);

      def.mutableFunctionNames().add("func2");

      assertEquals(2, def.getFunctionNames().size());
      assertTrue(def.getFunctionNames().contains("func2"));
    }

    @Test
    @DisplayName("mutableTypeNames allows modification")
    void mutableTypeNamesAllowsModification() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, List.of("type1"), null, null, null);

      def.mutableTypeNames().add("type2");

      assertEquals(2, def.getTypeNames().size());
      assertTrue(def.getTypeNames().contains("type2"));
    }

    @Test
    @DisplayName("mutableDependencies allows modification")
    void mutableDependenciesAllowsModification() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, Set.of("dep1"), null, null);

      def.mutableDependencies().add("dep2");

      assertEquals(2, def.getDependencies().size());
      assertTrue(def.getDependencies().contains("dep2"));
    }

    @Test
    @DisplayName("mutableImportNames allows modification")
    void mutableImportNamesAllowsModification() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, List.of("import1"), null);

      def.mutableImportNames().add("import2");

      assertEquals(2, def.getImportNames().size());
      assertTrue(def.getImportNames().contains("import2"));
    }

    @Test
    @DisplayName("mutableExportNames allows modification")
    void mutableExportNamesAllowsModification() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, List.of("export1"));

      def.mutableExportNames().add("export2");

      assertEquals(2, def.getExportNames().size());
      assertTrue(def.getExportNames().contains("export2"));
    }

    @Test
    @DisplayName("mutableFunctionNames returns same backing list as getFunctionNames")
    void mutableFunctionNamesReturnsSameBackingList() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", "pkg", null, null, null, null, null);

      def.mutableFunctionNames().add("added");

      assertTrue(def.getFunctionNames().contains("added"));
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString includes class name and all expected fields")
    void toStringIncludesExpectedFields() {
      final TestWitInterfaceDefinition def =
          createDefinition(
              "my-iface",
              "2.1.0",
              "my:pkg",
              List.of("f1", "f2", "f3"),
              List.of("t1", "t2"),
              Set.of("d1"),
              null,
              null);

      final String str = def.toString();

      assertTrue(str.contains("TestWitInterfaceDefinition"));
      assertTrue(str.contains("my-iface"));
      assertTrue(str.contains("2.1.0"));
      assertTrue(str.contains("my:pkg"));
      assertTrue(str.contains("functionCount=3"));
      assertTrue(str.contains("typeCount=2"));
      assertTrue(str.contains("dependencyCount=1"));
    }

    @Test
    @DisplayName("toString with defaults includes correct counts")
    void toStringWithDefaultsIncludesCorrectCounts() {
      final TestWitInterfaceDefinition def =
          createDefinition(null, null, null, null, null, null, null, null);

      final String str = def.toString();

      assertTrue(str.contains("name=''"));
      assertTrue(str.contains("version='0.0.0'"));
      assertTrue(str.contains("packageName=''"));
      assertTrue(str.contains("functionCount=0"));
      assertTrue(str.contains("typeCount=0"));
      assertTrue(str.contains("dependencyCount=0"));
    }
  }

  @Nested
  @DisplayName("getWitText Tests")
  class WitTextTests {

    @Test
    @DisplayName("getWitText is implemented by concrete subclass")
    void getWitTextIsImplementedBySubclass() {
      final TestWitInterfaceDefinition def =
          createDefinition("my-iface", "1.0.0", "pkg", null, null, null, null, null);

      assertNotNull(def.getWitText());
      assertEquals("interface my-iface {}", def.getWitText());
    }
  }

  @Nested
  @DisplayName("Null Default Boundary Tests")
  class NullDefaultBoundaryTests {

    @Test
    @DisplayName("Only name is null - defaults to empty string, others keep values")
    void onlyNameNull() {
      final TestWitInterfaceDefinition def =
          createDefinition(
              null,
              "3.0.0",
              "some:pkg",
              List.of("f"),
              List.of("t"),
              Set.of("d"),
              List.of("i"),
              List.of("e"));

      assertEquals("", def.getName());
      assertEquals("3.0.0", def.getVersion());
      assertEquals("some:pkg", def.getPackageName());
      assertEquals(1, def.getFunctionNames().size());
    }

    @Test
    @DisplayName("Only version is null - defaults to 0.0.0")
    void onlyVersionNull() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", null, "pkg", null, null, null, null, null);

      assertEquals("iface", def.getName());
      assertEquals("0.0.0", def.getVersion());
    }

    @Test
    @DisplayName("Only packageName is null - defaults to empty string")
    void onlyPackageNameNull() {
      final TestWitInterfaceDefinition def =
          createDefinition("iface", "1.0.0", null, null, null, null, null, null);

      assertEquals("iface", def.getName());
      assertEquals("", def.getPackageName());
    }
  }
}
