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

import ai.tegmentum.wasmtime4j.func.Function;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentCapability;
import ai.tegmentum.wasmtime4j.component.ComponentCompatibility;
import ai.tegmentum.wasmtime4j.component.ComponentCompatibilityResult;
import ai.tegmentum.wasmtime4j.component.ComponentDebugInfo;
import ai.tegmentum.wasmtime4j.component.ComponentDependencyGraph;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.component.ComponentEngineDebugInfo;
import ai.tegmentum.wasmtime4j.component.ComponentFeature;
import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentId;
import ai.tegmentum.wasmtime4j.component.ComponentImportValidation;
import ai.tegmentum.wasmtime4j.component.ComponentImportValidation.Builder;
import ai.tegmentum.wasmtime4j.component.ComponentImportValidation.MissingImport;
import ai.tegmentum.wasmtime4j.component.ComponentImportValidation.TypeMismatch;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleState;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentLinkInfo;
import ai.tegmentum.wasmtime4j.component.ComponentLoadConfig;
import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentRegistry;
import ai.tegmentum.wasmtime4j.component.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.component.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.component.ComponentResult;
import ai.tegmentum.wasmtime4j.component.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.component.ComponentSpecification;
import ai.tegmentum.wasmtime4j.component.ComponentStateTransitionConfig;
import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.component.ComponentValFactory;
import ai.tegmentum.wasmtime4j.component.ComponentValidationConfig;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentVariant;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentImportValidation} class.
 *
 * <p>ComponentImportValidation provides result of validating component imports.
 */
@DisplayName("ComponentImportValidation Tests")
class ComponentImportValidationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentImportValidation.class.getModifiers()),
          "ComponentImportValidation should be public");
      assertTrue(
          Modifier.isFinal(ComponentImportValidation.class.getModifiers()),
          "ComponentImportValidation should be final");
    }

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentImportValidation.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have success factory method")
    void shouldHaveSuccessFactoryMethod() throws NoSuchMethodException {
      final Method method = ComponentImportValidation.class.getMethod("success", List.class);
      assertNotNull(method, "success method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "success should be static");
    }

    @Test
    @DisplayName("should have failure factory method")
    void shouldHaveFailureFactoryMethod() throws NoSuchMethodException {
      final Method method =
          ComponentImportValidation.class.getMethod("failure", List.class, List.class, List.class);
      assertNotNull(method, "failure method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "failure should be static");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("success should create valid result")
    void successShouldCreateValidResult() {
      final var satisfiedImports = List.of("wasi:io/streams@0.2.0", "wasi:filesystem/types@0.2.0");

      final var result = ComponentImportValidation.success(satisfiedImports);

      assertTrue(result.isValid(), "Success result should be valid");
      assertEquals(2, result.getSatisfiedImports().size(), "Should have 2 satisfied imports");
      assertTrue(result.getMissingImports().isEmpty(), "Should have no missing imports");
      assertTrue(result.getTypeMismatches().isEmpty(), "Should have no type mismatches");
    }

    @Test
    @DisplayName("failure should create invalid result")
    void failureShouldCreateInvalidResult() {
      final var satisfiedImports = List.of("wasi:io/streams@0.2.0");
      final var missingImports = List.of(new MissingImport("wasi", "filesystem/types", null));
      final var typeMismatches =
          List.of(new TypeMismatch("wasi:cli/stdout", "resource", "func", "Type mismatch"));

      final var result =
          ComponentImportValidation.failure(satisfiedImports, missingImports, typeMismatches);

      assertFalse(result.isValid(), "Failure result should not be valid");
      assertEquals(1, result.getSatisfiedImports().size(), "Should have 1 satisfied import");
      assertEquals(1, result.getMissingImports().size(), "Should have 1 missing import");
      assertEquals(1, result.getTypeMismatches().size(), "Should have 1 type mismatch");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("isValid should return true when no issues")
    void isValidShouldReturnTrueWhenNoIssues() {
      final var result = ComponentImportValidation.success(List.of("import1"));

      assertTrue(result.isValid(), "Should be valid when no issues");
    }

    @Test
    @DisplayName("isValid should return false when missing imports")
    void isValidShouldReturnFalseWhenMissingImports() {
      final var missingImports = List.of(new MissingImport("ns", "interface", null));
      final var result = ComponentImportValidation.failure(List.of(), missingImports, List.of());

      assertFalse(result.isValid(), "Should not be valid with missing imports");
    }

    @Test
    @DisplayName("isValid should return false when type mismatches")
    void isValidShouldReturnFalseWhenTypeMismatches() {
      final var typeMismatches = List.of(new TypeMismatch("path", "expected", "actual", "details"));
      final var result = ComponentImportValidation.failure(List.of(), List.of(), typeMismatches);

      assertFalse(result.isValid(), "Should not be valid with type mismatches");
    }
  }

  @Nested
  @DisplayName("Summary Tests")
  class SummaryTests {

    @Test
    @DisplayName("getSummary should indicate success")
    void getSummaryShouldIndicateSuccess() {
      final var result = ComponentImportValidation.success(List.of("import1", "import2"));

      final var summary = result.getSummary();

      assertTrue(
          summary.contains("2 imports satisfied"), "Summary should indicate satisfied count");
    }

    @Test
    @DisplayName("getSummary should indicate missing imports")
    void getSummaryShouldIndicateMissingImports() {
      final var missingImports =
          List.of(
              new MissingImport("ns1", "interface1", null),
              new MissingImport("ns2", "interface2", null));
      final var result = ComponentImportValidation.failure(List.of(), missingImports, List.of());

      final var summary = result.getSummary();

      assertTrue(summary.contains("failed"), "Summary should indicate failure");
      assertTrue(summary.contains("2 missing import"), "Summary should indicate missing count");
    }

    @Test
    @DisplayName("getSummary should indicate type mismatches")
    void getSummaryShouldIndicateTypeMismatches() {
      final var typeMismatches =
          List.of(
              new TypeMismatch("path1", "exp1", "act1", "det1"),
              new TypeMismatch("path2", "exp2", "act2", "det2"),
              new TypeMismatch("path3", "exp3", "act3", "det3"));
      final var result = ComponentImportValidation.failure(List.of(), List.of(), typeMismatches);

      final var summary = result.getSummary();

      assertTrue(summary.contains("failed"), "Summary should indicate failure");
      assertTrue(summary.contains("3 type mismatch"), "Summary should indicate mismatch count");
    }
  }

  @Nested
  @DisplayName("MissingImport Tests")
  class MissingImportTests {

    @Test
    @DisplayName("should create missing import with all fields")
    void shouldCreateMissingImportWithAllFields() {
      final var missing = new MissingImport("wasi", "filesystem/types", "read");

      assertEquals("wasi", missing.getInterfaceNamespace(), "Namespace should match");
      assertEquals("filesystem/types", missing.getInterfaceName(), "Interface name should match");
      assertEquals("read", missing.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("should create missing import without function name")
    void shouldCreateMissingImportWithoutFunctionName() {
      final var missing = new MissingImport("wasi", "filesystem/types", null);

      assertEquals("wasi", missing.getInterfaceNamespace(), "Namespace should match");
      assertEquals("filesystem/types", missing.getInterfaceName(), "Interface name should match");
      assertNull(missing.getFunctionName(), "Function name should be null");
    }

    @Test
    @DisplayName("getWitPath should build correct path with namespace and function")
    void getWitPathShouldBuildCorrectPathWithNamespaceAndFunction() {
      final var missing = new MissingImport("wasi", "filesystem/types", "read");

      assertEquals(
          "wasi/filesystem/types#read",
          missing.getWitPath(),
          "WIT path should include namespace, interface, and function");
    }

    @Test
    @DisplayName("getWitPath should build correct path without function")
    void getWitPathShouldBuildCorrectPathWithoutFunction() {
      final var missing = new MissingImport("wasi", "filesystem/types", null);

      assertEquals(
          "wasi/filesystem/types",
          missing.getWitPath(),
          "WIT path should include namespace and interface");
    }

    @Test
    @DisplayName("getWitPath should build correct path without namespace")
    void getWitPathShouldBuildCorrectPathWithoutNamespace() {
      final var missing = new MissingImport(null, "local-interface", "process");

      assertEquals(
          "local-interface#process",
          missing.getWitPath(),
          "WIT path should include interface and function");
    }

    @Test
    @DisplayName("toString should return WIT path")
    void toStringShouldReturnWitPath() {
      final var missing = new MissingImport("wasi", "io/streams", null);

      assertEquals(missing.getWitPath(), missing.toString(), "toString should return WIT path");
    }
  }

  @Nested
  @DisplayName("TypeMismatch Tests")
  class TypeMismatchTests {

    @Test
    @DisplayName("should create type mismatch with all fields")
    void shouldCreateTypeMismatchWithAllFields() {
      final var mismatch = new TypeMismatch("wasi:io/streams#write", "resource", "func", "Details");

      assertEquals("wasi:io/streams#write", mismatch.getWitPath(), "WIT path should match");
      assertEquals("resource", mismatch.getExpectedType(), "Expected type should match");
      assertEquals("func", mismatch.getActualType(), "Actual type should match");
      assertEquals("Details", mismatch.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("toString should format type mismatch")
    void toStringShouldFormatTypeMismatch() {
      final var mismatch = new TypeMismatch("path#func", "i32", "i64", "Size mismatch");

      final var str = mismatch.toString();

      assertTrue(str.contains("path#func"), "Should contain WIT path");
      assertTrue(str.contains("expected i32"), "Should contain expected type");
      assertTrue(str.contains("got i64"), "Should contain actual type");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create valid result when no issues")
    void builderShouldCreateValidResultWhenNoIssues() {
      final var result =
          ComponentImportValidation.builder()
              .addSatisfied("import1")
              .addSatisfied("import2")
              .build();

      assertTrue(result.isValid(), "Should be valid with no issues");
      assertEquals(2, result.getSatisfiedImports().size(), "Should have 2 satisfied imports");
    }

    @Test
    @DisplayName("builder should create invalid result when missing imports")
    void builderShouldCreateInvalidResultWhenMissingImports() {
      final var result =
          ComponentImportValidation.builder()
              .addSatisfied("import1")
              .addMissing(new MissingImport("ns", "interface", null))
              .build();

      assertFalse(result.isValid(), "Should not be valid with missing imports");
      assertEquals(1, result.getSatisfiedImports().size(), "Should have 1 satisfied import");
      assertEquals(1, result.getMissingImports().size(), "Should have 1 missing import");
    }

    @Test
    @DisplayName("builder should create invalid result when type mismatches")
    void builderShouldCreateInvalidResultWhenTypeMismatches() {
      final var result =
          ComponentImportValidation.builder()
              .addSatisfied("import1")
              .addMismatch(new TypeMismatch("path", "exp", "act", "det"))
              .build();

      assertFalse(result.isValid(), "Should not be valid with type mismatches");
      assertEquals(1, result.getTypeMismatches().size(), "Should have 1 type mismatch");
    }

    @Test
    @DisplayName("builder should allow chaining")
    void builderShouldAllowChaining() {
      final var builder = ComponentImportValidation.builder();

      final var result =
          builder
              .addSatisfied("import1")
              .addSatisfied("import2")
              .addMissing(new MissingImport("ns", "iface", null))
              .addMismatch(new TypeMismatch("path", "exp", "act", "det"))
              .build();

      assertFalse(result.isValid(), "Should not be valid");
      assertEquals(2, result.getSatisfiedImports().size(), "Should have 2 satisfied imports");
      assertEquals(1, result.getMissingImports().size(), "Should have 1 missing import");
      assertEquals(1, result.getTypeMismatches().size(), "Should have 1 type mismatch");
    }

    @Test
    @DisplayName("builder methods should return same builder instance")
    void builderMethodsShouldReturnSameBuilderInstance() {
      final var builder = ComponentImportValidation.builder();

      final var builder1 = builder.addSatisfied("import1");
      final var builder2 = builder1.addMissing(new MissingImport("ns", "iface", null));
      final var builder3 = builder2.addMismatch(new TypeMismatch("p", "e", "a", "d"));

      // All should be the same instance for fluent API
      assertEquals(builder, builder1, "addSatisfied should return same builder");
      assertEquals(builder1, builder2, "addMissing should return same builder");
      assertEquals(builder2, builder3, "addMismatch should return same builder");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("getSatisfiedImports should return immutable list")
    void getSatisfiedImportsShouldReturnImmutableList() {
      final var result = ComponentImportValidation.success(List.of("import1"));

      try {
        result.getSatisfiedImports().add("new-import");
        assertFalse(true, "Should have thrown UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        assertTrue(true, "Satisfied imports list should be immutable");
      }
    }

    @Test
    @DisplayName("getMissingImports should return immutable list")
    void getMissingImportsShouldReturnImmutableList() {
      final var missingImports = List.of(new MissingImport("ns", "iface", null));
      final var result = ComponentImportValidation.failure(List.of(), missingImports, List.of());

      try {
        result.getMissingImports().add(new MissingImport("ns2", "iface2", null));
        assertFalse(true, "Should have thrown UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        assertTrue(true, "Missing imports list should be immutable");
      }
    }

    @Test
    @DisplayName("getTypeMismatches should return immutable list")
    void getTypeMismatchesShouldReturnImmutableList() {
      final var typeMismatches = List.of(new TypeMismatch("p", "e", "a", "d"));
      final var result = ComponentImportValidation.failure(List.of(), List.of(), typeMismatches);

      try {
        result.getTypeMismatches().add(new TypeMismatch("p2", "e2", "a2", "d2"));
        assertFalse(true, "Should have thrown UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        assertTrue(true, "Type mismatches list should be immutable");
      }
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty satisfied imports")
    void shouldHandleEmptySatisfiedImports() {
      final var result = ComponentImportValidation.success(List.of());

      assertTrue(result.isValid(), "Should be valid with empty satisfied imports");
      assertTrue(result.getSatisfiedImports().isEmpty(), "Satisfied imports should be empty");
    }

    @Test
    @DisplayName("should handle both missing and mismatches")
    void shouldHandleBothMissingAndMismatches() {
      final var missingImports = List.of(new MissingImport("ns", "iface", null));
      final var typeMismatches = List.of(new TypeMismatch("p", "e", "a", "d"));
      final var result =
          ComponentImportValidation.failure(List.of(), missingImports, typeMismatches);

      final var summary = result.getSummary();

      assertTrue(summary.contains("missing import"), "Summary should mention missing imports");
      assertTrue(summary.contains("type mismatch"), "Summary should mention type mismatches");
    }
  }
}
