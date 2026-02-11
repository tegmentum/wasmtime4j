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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;

/**
 * Tests for the ExecutionAdjustments interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ExecutionAdjustments using reflection-based testing.
 */
@DisplayName("ExecutionAdjustments Tests")
class ExecutionAdjustmentsTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionAdjustments should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionAdjustments.class.isInterface(), "ExecutionAdjustments should be an interface");
    }

    @Test
    @DisplayName("ExecutionAdjustments should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionAdjustments.class.getModifiers()),
          "ExecutionAdjustments should be public");
    }

    @Test
    @DisplayName("ExecutionAdjustments should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionAdjustments.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionAdjustments should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getExecutionId method")
    void shouldHaveGetExecutionIdMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("getExecutionId");
      assertNotNull(method, "getExecutionId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getQuotaAdjustments method")
    void shouldHaveGetQuotaAdjustmentsMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("getQuotaAdjustments");
      assertNotNull(method, "getQuotaAdjustments method should exist");
      assertEquals(
          ExecutionAdjustments.QuotaAdjustments.class,
          method.getReturnType(),
          "Return type should be QuotaAdjustments");
    }

    @Test
    @DisplayName("should have setQuotaAdjustments method")
    void shouldHaveSetQuotaAdjustmentsMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAdjustments.class.getMethod(
              "setQuotaAdjustments", ExecutionAdjustments.QuotaAdjustments.class);
      assertNotNull(method, "setQuotaAdjustments method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getPriorityAdjustments method")
    void shouldHaveGetPriorityAdjustmentsMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("getPriorityAdjustments");
      assertNotNull(method, "getPriorityAdjustments method should exist");
      assertEquals(
          ExecutionAdjustments.PriorityAdjustments.class,
          method.getReturnType(),
          "Return type should be PriorityAdjustments");
    }

    @Test
    @DisplayName("should have setPriorityAdjustments method")
    void shouldHaveSetPriorityAdjustmentsMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAdjustments.class.getMethod(
              "setPriorityAdjustments", ExecutionAdjustments.PriorityAdjustments.class);
      assertNotNull(method, "setPriorityAdjustments method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getPerformanceAdjustments method")
    void shouldHaveGetPerformanceAdjustmentsMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("getPerformanceAdjustments");
      assertNotNull(method, "getPerformanceAdjustments method should exist");
      assertEquals(
          ExecutionAdjustments.PerformanceAdjustments.class,
          method.getReturnType(),
          "Return type should be PerformanceAdjustments");
    }

    @Test
    @DisplayName("should have setPerformanceAdjustments method")
    void shouldHaveSetPerformanceAdjustmentsMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAdjustments.class.getMethod(
              "setPerformanceAdjustments", ExecutionAdjustments.PerformanceAdjustments.class);
      assertNotNull(method, "setPerformanceAdjustments method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getReason method")
    void shouldHaveGetReasonMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("getReason");
      assertNotNull(method, "getReason method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have apply method")
    void shouldHaveApplyMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("apply");
      assertNotNull(method, "apply method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have revert method")
    void shouldHaveRevertMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.class.getMethod("revert");
      assertNotNull(method, "revert method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // QuotaAdjustments Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("QuotaAdjustments Interface Tests")
  class QuotaAdjustmentsTests {

    @Test
    @DisplayName("QuotaAdjustments should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionAdjustments.QuotaAdjustments.class.isInterface(),
          "QuotaAdjustments should be an interface");
      assertTrue(
          ExecutionAdjustments.QuotaAdjustments.class.isMemberClass(),
          "QuotaAdjustments should be a member class");
    }

    @Test
    @DisplayName("QuotaAdjustments should have getFuelAdjustment method")
    void shouldHaveGetFuelAdjustmentMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.QuotaAdjustments.class.getMethod("getFuelAdjustment");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaAdjustments should have getMemoryAdjustment method")
    void shouldHaveGetMemoryAdjustmentMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.QuotaAdjustments.class.getMethod("getMemoryAdjustment");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaAdjustments should have getTimeAdjustment method")
    void shouldHaveGetTimeAdjustmentMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.QuotaAdjustments.class.getMethod("getTimeAdjustment");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaAdjustments should have getInstructionAdjustment method")
    void shouldHaveGetInstructionAdjustmentMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAdjustments.QuotaAdjustments.class.getMethod("getInstructionAdjustment");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // PriorityAdjustments Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("PriorityAdjustments Interface Tests")
  class PriorityAdjustmentsTests {

    @Test
    @DisplayName("PriorityAdjustments should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionAdjustments.PriorityAdjustments.class.isInterface(),
          "PriorityAdjustments should be an interface");
      assertTrue(
          ExecutionAdjustments.PriorityAdjustments.class.isMemberClass(),
          "PriorityAdjustments should be a member class");
    }

    @Test
    @DisplayName("PriorityAdjustments should have getPriorityLevel method")
    void shouldHaveGetPriorityLevelMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.PriorityAdjustments.class.getMethod("getPriorityLevel");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("PriorityAdjustments should have getBoostFactor method")
    void shouldHaveGetBoostFactorMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.PriorityAdjustments.class.getMethod("getBoostFactor");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }
  }

  // ========================================================================
  // PerformanceAdjustments Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("PerformanceAdjustments Interface Tests")
  class PerformanceAdjustmentsTests {

    @Test
    @DisplayName("PerformanceAdjustments should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionAdjustments.PerformanceAdjustments.class.isInterface(),
          "PerformanceAdjustments should be an interface");
      assertTrue(
          ExecutionAdjustments.PerformanceAdjustments.class.isMemberClass(),
          "PerformanceAdjustments should be a member class");
    }

    @Test
    @DisplayName("PerformanceAdjustments should have getCpuAffinity method")
    void shouldHaveGetCpuAffinityMethod() throws NoSuchMethodException {
      Method method = ExecutionAdjustments.PerformanceAdjustments.class.getMethod("getCpuAffinity");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("PerformanceAdjustments should have getThreadPoolSize method")
    void shouldHaveGetThreadPoolSizeMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAdjustments.PerformanceAdjustments.class.getMethod("getThreadPoolSize");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("PerformanceAdjustments should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAdjustments.PerformanceAdjustments.class.getMethod("getOptimizationLevel");
      assertEquals(
          ExecutionAdjustments.OptimizationLevel.class,
          method.getReturnType(),
          "Return type should be OptimizationLevel");
    }
  }

  // ========================================================================
  // OptimizationLevel Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OptimizationLevel Enum Tests")
  class OptimizationLevelTests {

    @Test
    @DisplayName("OptimizationLevel should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionAdjustments.OptimizationLevel.class.isEnum(),
          "OptimizationLevel should be an enum");
      assertTrue(
          ExecutionAdjustments.OptimizationLevel.class.isMemberClass(),
          "OptimizationLevel should be a member class");
    }

    @Test
    @DisplayName("OptimizationLevel should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionAdjustments.OptimizationLevel.class.getModifiers()),
          "OptimizationLevel should be public");
    }

    @Test
    @DisplayName("OptimizationLevel should have 4 values")
    void shouldHaveFourValues() {
      ExecutionAdjustments.OptimizationLevel[] values =
          ExecutionAdjustments.OptimizationLevel.values();
      assertEquals(4, values.length, "OptimizationLevel should have 4 values");
    }

    @Test
    @DisplayName("OptimizationLevel should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("NONE", "BASIC", "STANDARD", "AGGRESSIVE");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionAdjustments.OptimizationLevel level :
          ExecutionAdjustments.OptimizationLevel.values()) {
        actualNames.add(level.name());
      }
      assertEquals(expectedNames, actualNames, "OptimizationLevel should have expected values");
    }

    @Test
    @DisplayName("NONE should have ordinal 0")
    void noneShouldHaveOrdinalZero() {
      assertEquals(
          0, ExecutionAdjustments.OptimizationLevel.NONE.ordinal(), "NONE should have ordinal 0");
    }

    @Test
    @DisplayName("AGGRESSIVE should have ordinal 3")
    void aggressiveShouldHaveOrdinalThree() {
      assertEquals(
          3,
          ExecutionAdjustments.OptimizationLevel.AGGRESSIVE.ordinal(),
          "AGGRESSIVE should have ordinal 3");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ExecutionAdjustments should have 1 nested enum")
    void shouldHaveOneNestedEnum() {
      Class<?>[] nestedClasses = ExecutionAdjustments.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(1, enumCount, "ExecutionAdjustments should have 1 nested enum");
    }

    @Test
    @DisplayName("ExecutionAdjustments should have 3 nested interfaces")
    void shouldHaveThreeNestedInterfaces() {
      Class<?>[] nestedClasses = ExecutionAdjustments.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(3, interfaceCount, "ExecutionAdjustments should have 3 nested interfaces");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ExecutionAdjustments should have at least 11 methods")
    void shouldHaveAtLeastElevenMethods() {
      Method[] methods = ExecutionAdjustments.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 11,
          "ExecutionAdjustments should have at least 11 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("All methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ExecutionAdjustments.class.getDeclaredMethods();
      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            method.getName() + " should be public and abstract");
      }
    }
  }
}
