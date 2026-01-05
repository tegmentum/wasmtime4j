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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Memory64Compatibility utility class.
 *
 * <p>Memory64Compatibility provides utilities for migrating applications to 64-bit memory support.
 */
@DisplayName("Memory64Compatibility Class Tests")
class Memory64CompatibilityTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(Memory64Compatibility.class.isInterface(), "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Memory64Compatibility.class.getModifiers()),
          "Memory64Compatibility should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(Memory64Compatibility.class.getModifiers()),
          "Memory64Compatibility should be final (utility class)");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = Memory64Compatibility.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private (utility class)");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have setCompatibilityModeEnabled method")
    void shouldHaveSetCompatibilityModeEnabledMethod() throws NoSuchMethodException {
      Method method =
          Memory64Compatibility.class.getMethod("setCompatibilityModeEnabled", boolean.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "setCompatibilityModeEnabled should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "setCompatibilityModeEnabled should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isCompatibilityModeEnabled method")
    void shouldHaveIsCompatibilityModeEnabledMethod() throws NoSuchMethodException {
      Method method = Memory64Compatibility.class.getMethod("isCompatibilityModeEnabled");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "isCompatibilityModeEnabled should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "isCompatibilityModeEnabled should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setMigrationWarningsEnabled method")
    void shouldHaveSetMigrationWarningsEnabledMethod() throws NoSuchMethodException {
      Method method =
          Memory64Compatibility.class.getMethod("setMigrationWarningsEnabled", boolean.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "setMigrationWarningsEnabled should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "setMigrationWarningsEnabled should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isMigrationWarningsEnabled method")
    void shouldHaveIsMigrationWarningsEnabledMethod() throws NoSuchMethodException {
      Method method = Memory64Compatibility.class.getMethod("isMigrationWarningsEnabled");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "isMigrationWarningsEnabled should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "isMigrationWarningsEnabled should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have wrapForCompatibility method")
    void shouldHaveWrapForCompatibilityMethod() throws NoSuchMethodException {
      Method method =
          Memory64Compatibility.class.getMethod("wrapForCompatibility", WasmMemory.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "wrapForCompatibility should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "wrapForCompatibility should be public");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }

    @Test
    @DisplayName("should have analyzeMemoryRequirements method")
    void shouldHaveAnalyzeMemoryRequirementsMethod() throws NoSuchMethodException {
      Method method =
          Memory64Compatibility.class.getMethod(
              "analyzeMemoryRequirements",
              long.class,
              Long.class,
              Memory64Compatibility.MemoryUsagePattern.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "analyzeMemoryRequirements should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "analyzeMemoryRequirements should be public");
      assertEquals(
          Memory64Compatibility.MemoryRecommendation.class,
          method.getReturnType(),
          "Should return MemoryRecommendation");
    }

    @Test
    @DisplayName("should have createMigrationPlan method")
    void shouldHaveCreateMigrationPlanMethod() throws NoSuchMethodException {
      Method method =
          Memory64Compatibility.class.getMethod(
              "createMigrationPlan",
              Memory64Config.class,
              Memory64Compatibility.MemoryRequirements.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "createMigrationPlan should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createMigrationPlan should be public");
      assertEquals(
          Memory64Compatibility.MigrationPlan.class,
          method.getReturnType(),
          "Should return MigrationPlan");
    }

    @Test
    @DisplayName("should have validateCompatibility method")
    void shouldHaveValidateCompatibilityMethod() throws NoSuchMethodException {
      Method method =
          Memory64Compatibility.class.getMethod(
              "validateCompatibility",
              Memory64Config.class,
              Memory64Compatibility.RuntimeInfo.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "validateCompatibility should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "validateCompatibility should be public");
      assertEquals(
          Memory64Compatibility.CompatibilityValidationResult.class,
          method.getReturnType(),
          "Should return CompatibilityValidationResult");
    }

    @Test
    @DisplayName("should have getOptimizationSuggestions method")
    void shouldHaveGetOptimizationSuggestionsMethod() throws NoSuchMethodException {
      Method method =
          Memory64Compatibility.class.getMethod(
              "getOptimizationSuggestions",
              WasmMemory.class,
              Memory64Compatibility.MemoryUsagePattern.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "getOptimizationSuggestions should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "getOptimizationSuggestions should be public");
      assertEquals(
          Memory64Compatibility.OptimizationSuggestions.class,
          method.getReturnType(),
          "Should return OptimizationSuggestions");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have MemoryUsagePattern nested class")
    void shouldHaveMemoryUsagePatternNestedClass() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("MemoryUsagePattern"));
      assertTrue(hasClass, "Should have MemoryUsagePattern nested class");
    }

    @Test
    @DisplayName("should have MemoryRecommendation nested class")
    void shouldHaveMemoryRecommendationNestedClass() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("MemoryRecommendation"));
      assertTrue(hasClass, "Should have MemoryRecommendation nested class");
    }

    @Test
    @DisplayName("should have MemoryRequirements nested class")
    void shouldHaveMemoryRequirementsNestedClass() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("MemoryRequirements"));
      assertTrue(hasClass, "Should have MemoryRequirements nested class");
    }

    @Test
    @DisplayName("should have MigrationPlan nested class")
    void shouldHaveMigrationPlanNestedClass() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("MigrationPlan"));
      assertTrue(hasClass, "Should have MigrationPlan nested class");
    }

    @Test
    @DisplayName("should have MigrationEffort nested enum")
    void shouldHaveMigrationEffortNestedEnum() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasEnum =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("MigrationEffort") && c.isEnum());
      assertTrue(hasEnum, "Should have MigrationEffort nested enum");
    }

    @Test
    @DisplayName("should have RuntimeInfo nested class")
    void shouldHaveRuntimeInfoNestedClass() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("RuntimeInfo"));
      assertTrue(hasClass, "Should have RuntimeInfo nested class");
    }

    @Test
    @DisplayName("should have CompatibilityValidationResult nested class")
    void shouldHaveCompatibilityValidationResultNestedClass() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("CompatibilityValidationResult"));
      assertTrue(hasClass, "Should have CompatibilityValidationResult nested class");
    }

    @Test
    @DisplayName("should have OptimizationSuggestions nested class")
    void shouldHaveOptimizationSuggestionsNestedClass() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("OptimizationSuggestions"));
      assertTrue(hasClass, "Should have OptimizationSuggestions nested class");
    }

    @Test
    @DisplayName("should have OptimizationImpact nested enum")
    void shouldHaveOptimizationImpactNestedEnum() {
      Class<?>[] nestedClasses = Memory64Compatibility.class.getDeclaredClasses();
      boolean hasEnum =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("OptimizationImpact") && c.isEnum());
      assertTrue(hasEnum, "Should have OptimizationImpact nested enum");
    }
  }

  // ========================================================================
  // MigrationEffort Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("MigrationEffort Enum Tests")
  class MigrationEffortEnumTests {

    @Test
    @DisplayName("should have NONE value")
    void shouldHaveNoneValue() {
      assertNotNull(
          Memory64Compatibility.MigrationEffort.valueOf("NONE"), "Should have NONE value");
    }

    @Test
    @DisplayName("should have LOW value")
    void shouldHaveLowValue() {
      assertNotNull(Memory64Compatibility.MigrationEffort.valueOf("LOW"), "Should have LOW value");
    }

    @Test
    @DisplayName("should have MODERATE value")
    void shouldHaveModerateValue() {
      assertNotNull(
          Memory64Compatibility.MigrationEffort.valueOf("MODERATE"), "Should have MODERATE value");
    }

    @Test
    @DisplayName("should have HIGH value")
    void shouldHaveHighValue() {
      assertNotNull(
          Memory64Compatibility.MigrationEffort.valueOf("HIGH"), "Should have HIGH value");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactly4Values() {
      assertEquals(
          4,
          Memory64Compatibility.MigrationEffort.values().length,
          "Should have exactly 4 enum values");
    }
  }

  // ========================================================================
  // OptimizationImpact Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OptimizationImpact Enum Tests")
  class OptimizationImpactEnumTests {

    @Test
    @DisplayName("should have LOW value")
    void shouldHaveLowValue() {
      assertNotNull(
          Memory64Compatibility.OptimizationImpact.valueOf("LOW"), "Should have LOW value");
    }

    @Test
    @DisplayName("should have MEDIUM value")
    void shouldHaveMediumValue() {
      assertNotNull(
          Memory64Compatibility.OptimizationImpact.valueOf("MEDIUM"), "Should have MEDIUM value");
    }

    @Test
    @DisplayName("should have HIGH value")
    void shouldHaveHighValue() {
      assertNotNull(
          Memory64Compatibility.OptimizationImpact.valueOf("HIGH"), "Should have HIGH value");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactly3Values() {
      assertEquals(
          3,
          Memory64Compatibility.OptimizationImpact.values().length,
          "Should have exactly 3 enum values");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected public static methods")
    void shouldHaveAllExpectedPublicStaticMethods() {
      Set<String> expectedMethods =
          Set.of(
              "setCompatibilityModeEnabled",
              "isCompatibilityModeEnabled",
              "setMigrationWarningsEnabled",
              "isMigrationWarningsEnabled",
              "wrapForCompatibility",
              "analyzeMemoryRequirements",
              "createMigrationPlan",
              "validateCompatibility",
              "getOptimizationSuggestions");

      Set<String> actualMethods =
          Arrays.stream(Memory64Compatibility.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class,
          Memory64Compatibility.class.getSuperclass(),
          "Should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          Memory64Compatibility.class.getInterfaces().length,
          "Utility class should not implement any interfaces");
    }
  }
}
