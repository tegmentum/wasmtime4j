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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLoadConditions} class.
 *
 * <p>ComponentLoadConditions defines requirements and constraints for loading WebAssembly
 * components.
 */
@DisplayName("ComponentLoadConditions Tests")
class ComponentLoadConditionsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentLoadConditions.class.getModifiers()),
          "ComponentLoadConditions should be public");
      assertTrue(
          Modifier.isFinal(ComponentLoadConditions.class.getModifiers()),
          "ComponentLoadConditions should be final");
      assertFalse(
          ComponentLoadConditions.class.isInterface(),
          "ComponentLoadConditions should not be an interface");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = ComponentLoadConditions.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Default constructor should be public");
    }

    @Test
    @DisplayName("should have parameterized constructor")
    void shouldHaveParameterizedConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          ComponentLoadConditions.class.getConstructor(
              List.class, List.class, long.class, boolean.class);
      assertNotNull(constructor, "Parameterized constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "Parameterized constructor should be public");
    }

    @Test
    @DisplayName("default constructor should set default values")
    void defaultConstructorShouldSetDefaultValues() {
      final ComponentLoadConditions conditions = new ComponentLoadConditions();

      assertNotNull(conditions.getRequiredImports(), "requiredImports should not be null");
      assertTrue(conditions.getRequiredImports().isEmpty(), "requiredImports should be empty");

      assertNotNull(conditions.getRequiredFeatures(), "requiredFeatures should not be null");
      assertTrue(conditions.getRequiredFeatures().isEmpty(), "requiredFeatures should be empty");

      assertEquals(
          1024L * 1024 * 1024,
          conditions.getMaxMemoryBytes(),
          "maxMemoryBytes should be 1GB by default");

      assertFalse(
          conditions.isAllowUnsafeFeatures(), "allowUnsafeFeatures should be false by default");
    }

    @Test
    @DisplayName("parameterized constructor should set values correctly")
    void parameterizedConstructorShouldSetValuesCorrectly() {
      final List<String> requiredImports = List.of("wasi:cli/terminal", "wasi:filesystem/types");
      final List<String> requiredFeatures = List.of("threads", "simd");
      final long maxMemoryBytes = 512L * 1024 * 1024;
      final boolean allowUnsafeFeatures = true;

      final ComponentLoadConditions conditions =
          new ComponentLoadConditions(
              requiredImports, requiredFeatures, maxMemoryBytes, allowUnsafeFeatures);

      assertEquals(
          requiredImports, conditions.getRequiredImports(), "requiredImports should match");
      assertEquals(
          requiredFeatures, conditions.getRequiredFeatures(), "requiredFeatures should match");
      assertEquals(maxMemoryBytes, conditions.getMaxMemoryBytes(), "maxMemoryBytes should match");
      assertTrue(conditions.isAllowUnsafeFeatures(), "allowUnsafeFeatures should be true");
    }

    @Test
    @DisplayName("constructor should reject null requiredImports")
    void constructorShouldRejectNullRequiredImports() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentLoadConditions(null, List.of(), 1024L, false),
          "Should throw NullPointerException for null requiredImports");
    }

    @Test
    @DisplayName("constructor should reject null requiredFeatures")
    void constructorShouldRejectNullRequiredFeatures() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentLoadConditions(List.of(), null, 1024L, false),
          "Should throw NullPointerException for null requiredFeatures");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getRequiredImports should return immutable list")
    void getRequiredImportsShouldReturnImmutableList() {
      final List<String> requiredImports = List.of("import1", "import2");
      final ComponentLoadConditions conditions =
          new ComponentLoadConditions(requiredImports, List.of(), 1024L, false);

      final List<String> result = conditions.getRequiredImports();
      assertThrows(
          UnsupportedOperationException.class,
          () -> result.add("new-import"),
          "Returned list should be immutable");
    }

    @Test
    @DisplayName("getRequiredFeatures should return immutable list")
    void getRequiredFeaturesShouldReturnImmutableList() {
      final List<String> requiredFeatures = List.of("feature1", "feature2");
      final ComponentLoadConditions conditions =
          new ComponentLoadConditions(List.of(), requiredFeatures, 1024L, false);

      final List<String> result = conditions.getRequiredFeatures();
      assertThrows(
          UnsupportedOperationException.class,
          () -> result.add("new-feature"),
          "Returned list should be immutable");
    }

    @Test
    @DisplayName("getMaxMemoryBytes should return correct value")
    void getMaxMemoryBytesShouldReturnCorrectValue() {
      final long maxMemory = 256L * 1024 * 1024;
      final ComponentLoadConditions conditions =
          new ComponentLoadConditions(List.of(), List.of(), maxMemory, false);

      assertEquals(maxMemory, conditions.getMaxMemoryBytes(), "maxMemoryBytes should match");
    }

    @Test
    @DisplayName("isAllowUnsafeFeatures should return correct value")
    void isAllowUnsafeFeaturesShouldReturnCorrectValue() {
      final ComponentLoadConditions conditionsTrue =
          new ComponentLoadConditions(List.of(), List.of(), 1024L, true);
      assertTrue(conditionsTrue.isAllowUnsafeFeatures(), "Should return true when set to true");

      final ComponentLoadConditions conditionsFalse =
          new ComponentLoadConditions(List.of(), List.of(), 1024L, false);
      assertFalse(conditionsFalse.isAllowUnsafeFeatures(), "Should return false when set to false");
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("constructor should create defensive copy of requiredImports")
    void constructorShouldCreateDefensiveCopyOfRequiredImports() {
      final List<String> imports = new java.util.ArrayList<>();
      imports.add("import1");
      imports.add("import2");

      final ComponentLoadConditions conditions =
          new ComponentLoadConditions(imports, List.of(), 1024L, false);

      // Modify original list
      imports.add("import3");

      // Conditions should not reflect the change
      assertEquals(2, conditions.getRequiredImports().size(), "Should have original 2 imports");
      assertFalse(
          conditions.getRequiredImports().contains("import3"),
          "Should not contain later-added import");
    }

    @Test
    @DisplayName("constructor should create defensive copy of requiredFeatures")
    void constructorShouldCreateDefensiveCopyOfRequiredFeatures() {
      final List<String> features = new java.util.ArrayList<>();
      features.add("feature1");

      final ComponentLoadConditions conditions =
          new ComponentLoadConditions(List.of(), features, 1024L, false);

      // Modify original list
      features.add("feature2");

      // Conditions should not reflect the change
      assertEquals(1, conditions.getRequiredFeatures().size(), "Should have original 1 feature");
      assertFalse(
          conditions.getRequiredFeatures().contains("feature2"),
          "Should not contain later-added feature");
    }
  }
}
