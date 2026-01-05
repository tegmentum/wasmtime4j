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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentOptimizationResult} interface.
 *
 * <p>ComponentOptimizationResult represents the result of component optimization operations.
 */
@DisplayName("ComponentOptimizationResult Tests")
class ComponentOptimizationResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentOptimizationResult.class.getModifiers()),
          "ComponentOptimizationResult should be public");
      assertTrue(
          ComponentOptimizationResult.class.isInterface(),
          "ComponentOptimizationResult should be an interface");
    }
  }

  @Nested
  @DisplayName("Core Method Tests")
  class CoreMethodTests {

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          ComponentOptimizationResult.OptimizationStatus.class,
          method.getReturnType(),
          "Should return OptimizationStatus");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getEndTime method")
    void shouldHaveGetEndTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getEndTime");
      assertNotNull(method, "getEndTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Optimization Result Method Tests")
  class OptimizationResultMethodTests {

    @Test
    @DisplayName("should have getPerformanceImprovement method")
    void shouldHaveGetPerformanceImprovementMethod() throws NoSuchMethodException {
      final Method method =
          ComponentOptimizationResult.class.getMethod("getPerformanceImprovement");
      assertNotNull(method, "getPerformanceImprovement method should exist");
      assertEquals(
          ComponentOptimizationResult.PerformanceImprovement.class,
          method.getReturnType(),
          "Should return PerformanceImprovement");
    }

    @Test
    @DisplayName("should have getMemoryOptimization method")
    void shouldHaveGetMemoryOptimizationMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getMemoryOptimization");
      assertNotNull(method, "getMemoryOptimization method should exist");
      assertEquals(
          ComponentOptimizationResult.MemoryOptimizationResult.class,
          method.getReturnType(),
          "Should return MemoryOptimizationResult");
    }

    @Test
    @DisplayName("should have getCompilationOptimization method")
    void shouldHaveGetCompilationOptimizationMethod() throws NoSuchMethodException {
      final Method method =
          ComponentOptimizationResult.class.getMethod("getCompilationOptimization");
      assertNotNull(method, "getCompilationOptimization method should exist");
      assertEquals(
          ComponentOptimizationResult.CompilationOptimizationResult.class,
          method.getReturnType(),
          "Should return CompilationOptimizationResult");
    }

    @Test
    @DisplayName("should have getRuntimeOptimization method")
    void shouldHaveGetRuntimeOptimizationMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getRuntimeOptimization");
      assertNotNull(method, "getRuntimeOptimization method should exist");
      assertEquals(
          ComponentOptimizationResult.RuntimeOptimizationResult.class,
          method.getReturnType(),
          "Should return RuntimeOptimizationResult");
    }
  }

  @Nested
  @DisplayName("Error and Warning Method Tests")
  class ErrorWarningMethodTests {

    @Test
    @DisplayName("should have getErrors method")
    void shouldHaveGetErrorsMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getErrors");
      assertNotNull(method, "getErrors method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(
          ComponentOptimizationResult.OptimizationMetrics.class,
          method.getReturnType(),
          "Should return OptimizationMetrics");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationResult.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(
          ComponentOptimizationResult.OptimizationSummary.class,
          method.getReturnType(),
          "Should return OptimizationSummary");
    }
  }

  @Nested
  @DisplayName("OptimizationStatus Enum Tests")
  class OptimizationStatusEnumTests {

    @Test
    @DisplayName("OptimizationStatus should be an enum")
    void optimizationStatusShouldBeAnEnum() {
      assertTrue(
          ComponentOptimizationResult.OptimizationStatus.class.isEnum(),
          "OptimizationStatus should be an enum");
    }

    @Test
    @DisplayName("OptimizationStatus should have correct values")
    void optimizationStatusShouldHaveCorrectValues() {
      final var values = ComponentOptimizationResult.OptimizationStatus.values();
      assertEquals(6, values.length, "Should have 6 status values");

      assertEquals(
          ComponentOptimizationResult.OptimizationStatus.PENDING,
          ComponentOptimizationResult.OptimizationStatus.valueOf("PENDING"));
      assertEquals(
          ComponentOptimizationResult.OptimizationStatus.RUNNING,
          ComponentOptimizationResult.OptimizationStatus.valueOf("RUNNING"));
      assertEquals(
          ComponentOptimizationResult.OptimizationStatus.COMPLETED,
          ComponentOptimizationResult.OptimizationStatus.valueOf("COMPLETED"));
      assertEquals(
          ComponentOptimizationResult.OptimizationStatus.FAILED,
          ComponentOptimizationResult.OptimizationStatus.valueOf("FAILED"));
      assertEquals(
          ComponentOptimizationResult.OptimizationStatus.CANCELLED,
          ComponentOptimizationResult.OptimizationStatus.valueOf("CANCELLED"));
      assertEquals(
          ComponentOptimizationResult.OptimizationStatus.PARTIAL,
          ComponentOptimizationResult.OptimizationStatus.valueOf("PARTIAL"));
    }
  }

  @Nested
  @DisplayName("ErrorSeverity Enum Tests")
  class ErrorSeverityEnumTests {

    @Test
    @DisplayName("ErrorSeverity should be an enum")
    void errorSeverityShouldBeAnEnum() {
      assertTrue(
          ComponentOptimizationResult.ErrorSeverity.class.isEnum(),
          "ErrorSeverity should be an enum");
    }

    @Test
    @DisplayName("ErrorSeverity should have correct values")
    void errorSeverityShouldHaveCorrectValues() {
      final var values = ComponentOptimizationResult.ErrorSeverity.values();
      assertEquals(4, values.length, "Should have 4 severity values");

      assertEquals(
          ComponentOptimizationResult.ErrorSeverity.LOW,
          ComponentOptimizationResult.ErrorSeverity.valueOf("LOW"));
      assertEquals(
          ComponentOptimizationResult.ErrorSeverity.MEDIUM,
          ComponentOptimizationResult.ErrorSeverity.valueOf("MEDIUM"));
      assertEquals(
          ComponentOptimizationResult.ErrorSeverity.HIGH,
          ComponentOptimizationResult.ErrorSeverity.valueOf("HIGH"));
      assertEquals(
          ComponentOptimizationResult.ErrorSeverity.CRITICAL,
          ComponentOptimizationResult.ErrorSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("WarningCategory Enum Tests")
  class WarningCategoryEnumTests {

    @Test
    @DisplayName("WarningCategory should be an enum")
    void warningCategoryShouldBeAnEnum() {
      assertTrue(
          ComponentOptimizationResult.WarningCategory.class.isEnum(),
          "WarningCategory should be an enum");
    }

    @Test
    @DisplayName("WarningCategory should have correct values")
    void warningCategoryShouldHaveCorrectValues() {
      final var values = ComponentOptimizationResult.WarningCategory.values();
      assertEquals(4, values.length, "Should have 4 warning category values");

      assertEquals(
          ComponentOptimizationResult.WarningCategory.PERFORMANCE,
          ComponentOptimizationResult.WarningCategory.valueOf("PERFORMANCE"));
      assertEquals(
          ComponentOptimizationResult.WarningCategory.MEMORY,
          ComponentOptimizationResult.WarningCategory.valueOf("MEMORY"));
      assertEquals(
          ComponentOptimizationResult.WarningCategory.COMPATIBILITY,
          ComponentOptimizationResult.WarningCategory.valueOf("COMPATIBILITY"));
      assertEquals(
          ComponentOptimizationResult.WarningCategory.CONFIGURATION,
          ComponentOptimizationResult.WarningCategory.valueOf("CONFIGURATION"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have PerformanceImprovement nested interface")
    void shouldHavePerformanceImprovementNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PerformanceImprovement")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "PerformanceImprovement should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have PerformanceImprovement nested interface");
    }

    @Test
    @DisplayName("should have MemoryOptimizationResult nested interface")
    void shouldHaveMemoryOptimizationResultNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("MemoryOptimizationResult")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "MemoryOptimizationResult should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have MemoryOptimizationResult nested interface");
    }

    @Test
    @DisplayName("should have CompilationOptimizationResult nested interface")
    void shouldHaveCompilationOptimizationResultNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("CompilationOptimizationResult")) {
          found = true;
          assertTrue(
              nestedClass.isInterface(), "CompilationOptimizationResult should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have CompilationOptimizationResult nested interface");
    }

    @Test
    @DisplayName("should have RuntimeOptimizationResult nested interface")
    void shouldHaveRuntimeOptimizationResultNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("RuntimeOptimizationResult")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "RuntimeOptimizationResult should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have RuntimeOptimizationResult nested interface");
    }

    @Test
    @DisplayName("should have OptimizationError nested interface")
    void shouldHaveOptimizationErrorNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("OptimizationError")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "OptimizationError should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have OptimizationError nested interface");
    }

    @Test
    @DisplayName("should have OptimizationWarning nested interface")
    void shouldHaveOptimizationWarningNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("OptimizationWarning")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "OptimizationWarning should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have OptimizationWarning nested interface");
    }

    @Test
    @DisplayName("should have OptimizationMetrics nested interface")
    void shouldHaveOptimizationMetricsNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("OptimizationMetrics")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "OptimizationMetrics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have OptimizationMetrics nested interface");
    }

    @Test
    @DisplayName("should have OptimizationSummary nested interface")
    void shouldHaveOptimizationSummaryNestedInterface() {
      final var nestedClasses = ComponentOptimizationResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("OptimizationSummary")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "OptimizationSummary should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have OptimizationSummary nested interface");
    }
  }
}
