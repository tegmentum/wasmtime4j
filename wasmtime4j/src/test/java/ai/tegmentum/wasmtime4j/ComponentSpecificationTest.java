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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentSpecification} class.
 *
 * <p>ComponentSpecification defines the structure and requirements of a WebAssembly component
 * including its imports, exports, and dependencies.
 */
@DisplayName("ComponentSpecification Tests")
class ComponentSpecificationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentSpecification.class.getModifiers()),
          "ComponentSpecification should be public");
      assertTrue(
          Modifier.isFinal(ComponentSpecification.class.getModifiers()),
          "ComponentSpecification should be final");
      assertFalse(
          ComponentSpecification.class.isInterface(),
          "ComponentSpecification should not be an interface");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("constructor should set all fields")
    void constructorShouldSetAllFields() {
      final List<String> imports = List.of("wasi:io/poll", "wasi:io/streams");
      final List<String> exports = List.of("run", "process");
      final Map<String, String> metadata = Map.of("author", "test", "license", "MIT");

      final ComponentSpecification spec =
          new ComponentSpecification("my-component", "1.0.0", imports, exports, metadata);

      assertEquals("my-component", spec.getComponentName(), "Component name should match");
      assertEquals("1.0.0", spec.getVersion(), "Version should match");
      assertEquals(2, spec.getImports().size(), "Should have 2 imports");
      assertEquals(2, spec.getExports().size(), "Should have 2 exports");
      assertEquals(2, spec.getMetadata().size(), "Should have 2 metadata entries");
    }

    @Test
    @DisplayName("constructor should reject null component name")
    void constructorShouldRejectNullComponentName() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentSpecification(null, "1.0.0", List.of(), List.of(), Map.of()),
          "Should throw for null componentName");
    }

    @Test
    @DisplayName("constructor should reject null version")
    void constructorShouldRejectNullVersion() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentSpecification("component", null, List.of(), List.of(), Map.of()),
          "Should throw for null version");
    }

    @Test
    @DisplayName("constructor should reject null imports")
    void constructorShouldRejectNullImports() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentSpecification("component", "1.0.0", null, List.of(), Map.of()),
          "Should throw for null imports");
    }

    @Test
    @DisplayName("constructor should reject null exports")
    void constructorShouldRejectNullExports() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentSpecification("component", "1.0.0", List.of(), null, Map.of()),
          "Should throw for null exports");
    }

    @Test
    @DisplayName("constructor should reject null metadata")
    void constructorShouldRejectNullMetadata() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentSpecification("component", "1.0.0", List.of(), List.of(), null),
          "Should throw for null metadata");
    }

    @Test
    @DisplayName("constructor should accept empty collections")
    void constructorShouldAcceptEmptyCollections() {
      final ComponentSpecification spec =
          new ComponentSpecification("component", "1.0.0", List.of(), List.of(), Map.of());

      assertTrue(spec.getImports().isEmpty(), "Imports should be empty");
      assertTrue(spec.getExports().isEmpty(), "Exports should be empty");
      assertTrue(spec.getMetadata().isEmpty(), "Metadata should be empty");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getComponentName method")
    void shouldHaveGetComponentNameMethod() throws NoSuchMethodException {
      final Method method = ComponentSpecification.class.getMethod("getComponentName");
      assertNotNull(method, "getComponentName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = ComponentSpecification.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getImports method")
    void shouldHaveGetImportsMethod() throws NoSuchMethodException {
      final Method method = ComponentSpecification.class.getMethod("getImports");
      assertNotNull(method, "getImports method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getExports method")
    void shouldHaveGetExportsMethod() throws NoSuchMethodException {
      final Method method = ComponentSpecification.class.getMethod("getExports");
      assertNotNull(method, "getExports method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      final Method method = ComponentSpecification.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("imports should be immutable")
    void importsShouldBeImmutable() {
      final List<String> imports = new java.util.ArrayList<>();
      imports.add("import1");

      final ComponentSpecification spec =
          new ComponentSpecification("component", "1.0.0", imports, List.of(), Map.of());

      // Modify original list
      imports.add("import2");

      // Verify spec was not affected
      assertEquals(
          1, spec.getImports().size(), "Spec should not be affected by original list modification");

      // Verify returned list is immutable
      assertThrows(
          UnsupportedOperationException.class,
          () -> spec.getImports().add("newImport"),
          "Returned list should be immutable");
    }

    @Test
    @DisplayName("exports should be immutable")
    void exportsShouldBeImmutable() {
      final List<String> exports = new java.util.ArrayList<>();
      exports.add("export1");

      final ComponentSpecification spec =
          new ComponentSpecification("component", "1.0.0", List.of(), exports, Map.of());

      // Modify original list
      exports.add("export2");

      // Verify spec was not affected
      assertEquals(
          1, spec.getExports().size(), "Spec should not be affected by original list modification");

      // Verify returned list is immutable
      assertThrows(
          UnsupportedOperationException.class,
          () -> spec.getExports().add("newExport"),
          "Returned list should be immutable");
    }

    @Test
    @DisplayName("metadata should be immutable")
    void metadataShouldBeImmutable() {
      final Map<String, String> metadata = new java.util.HashMap<>();
      metadata.put("key1", "value1");

      final ComponentSpecification spec =
          new ComponentSpecification("component", "1.0.0", List.of(), List.of(), metadata);

      // Modify original map
      metadata.put("key2", "value2");

      // Verify spec was not affected
      assertEquals(
          1, spec.getMetadata().size(), "Spec should not be affected by original map modification");

      // Verify returned map is immutable
      assertThrows(
          UnsupportedOperationException.class,
          () -> spec.getMetadata().put("newKey", "newValue"),
          "Returned map should be immutable");
    }
  }

  @Nested
  @DisplayName("Behavior Tests")
  class BehaviorTests {

    @Test
    @DisplayName("should preserve import order")
    void shouldPreserveImportOrder() {
      final List<String> imports = List.of("import-a", "import-b", "import-c");

      final ComponentSpecification spec =
          new ComponentSpecification("component", "1.0.0", imports, List.of(), Map.of());

      assertEquals("import-a", spec.getImports().get(0), "First import should be preserved");
      assertEquals("import-b", spec.getImports().get(1), "Second import should be preserved");
      assertEquals("import-c", spec.getImports().get(2), "Third import should be preserved");
    }

    @Test
    @DisplayName("should preserve export order")
    void shouldPreserveExportOrder() {
      final List<String> exports = List.of("export-x", "export-y", "export-z");

      final ComponentSpecification spec =
          new ComponentSpecification("component", "1.0.0", List.of(), exports, Map.of());

      assertEquals("export-x", spec.getExports().get(0), "First export should be preserved");
      assertEquals("export-y", spec.getExports().get(1), "Second export should be preserved");
      assertEquals("export-z", spec.getExports().get(2), "Third export should be preserved");
    }

    @Test
    @DisplayName("should allow special characters in names")
    void shouldAllowSpecialCharactersInNames() {
      final ComponentSpecification spec =
          new ComponentSpecification(
              "my:component/name@1.0.0",
              "1.0.0-beta+build.123",
              List.of("wasi:io/poll@0.2.0"),
              List.of("run"),
              Map.of());

      assertEquals(
          "my:component/name@1.0.0", spec.getComponentName(), "Component name with special chars");
      assertEquals("1.0.0-beta+build.123", spec.getVersion(), "Semver with prerelease");
    }
  }
}
