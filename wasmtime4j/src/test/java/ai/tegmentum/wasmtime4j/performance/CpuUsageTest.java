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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CpuUsage} class.
 *
 * <p>CpuUsage provides detailed CPU usage information for monitoring.
 */
@DisplayName("CpuUsage Tests")
class CpuUsageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(CpuUsage.class.getModifiers()), "CpuUsage should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(CpuUsage.class.getModifiers()), "CpuUsage should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CpuUsage.class.getConstructor(
              double.class, double.class, double.class, double.class, long.class, long.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getUserCpuTime method")
    void shouldHaveGetUserCpuTimeMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getUserCpuTime");
      assertNotNull(method, "getUserCpuTime method should exist");
      assertEquals(double.class, method.getReturnType(), "getUserCpuTime should return double");
    }

    @Test
    @DisplayName("should have getSystemCpuTime method")
    void shouldHaveGetSystemCpuTimeMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getSystemCpuTime");
      assertNotNull(method, "getSystemCpuTime method should exist");
      assertEquals(double.class, method.getReturnType(), "getSystemCpuTime should return double");
    }

    @Test
    @DisplayName("should have getTotalCpuTime method")
    void shouldHaveGetTotalCpuTimeMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getTotalCpuTime");
      assertNotNull(method, "getTotalCpuTime method should exist");
      assertEquals(double.class, method.getReturnType(), "getTotalCpuTime should return double");
    }

    @Test
    @DisplayName("should have getCpuUtilization method")
    void shouldHaveGetCpuUtilizationMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getCpuUtilization");
      assertNotNull(method, "getCpuUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "getCpuUtilization should return double");
    }

    @Test
    @DisplayName("should have getContextSwitches method")
    void shouldHaveGetContextSwitchesMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getContextSwitches");
      assertNotNull(method, "getContextSwitches method should exist");
      assertEquals(long.class, method.getReturnType(), "getContextSwitches should return long");
    }

    @Test
    @DisplayName("should have getSystemCalls method")
    void shouldHaveGetSystemCallsMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getSystemCalls");
      assertNotNull(method, "getSystemCalls method should exist");
      assertEquals(long.class, method.getReturnType(), "getSystemCalls should return long");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have isHighCpuUsage method")
    void shouldHaveIsHighCpuUsageMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("isHighCpuUsage");
      assertNotNull(method, "isHighCpuUsage method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHighCpuUsage should return boolean");
    }

    @Test
    @DisplayName("should have getEfficiency method")
    void shouldHaveGetEfficiencyMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getEfficiency");
      assertNotNull(method, "getEfficiency method should exist");
      assertEquals(double.class, method.getReturnType(), "getEfficiency should return double");
    }

    @Test
    @DisplayName("should have isSystemCallHeavy method")
    void shouldHaveIsSystemCallHeavyMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("isSystemCallHeavy");
      assertNotNull(method, "isSystemCallHeavy method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isSystemCallHeavy should return boolean");
    }

    @Test
    @DisplayName("should have getContextSwitchRate method")
    void shouldHaveGetContextSwitchRateMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getContextSwitchRate");
      assertNotNull(method, "getContextSwitchRate method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getContextSwitchRate should return double");
    }

    @Test
    @DisplayName("should have getSystemCallRate method")
    void shouldHaveGetSystemCallRateMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getSystemCallRate");
      assertNotNull(method, "getSystemCallRate method should exist");
      assertEquals(double.class, method.getReturnType(), "getSystemCallRate should return double");
    }

    @Test
    @DisplayName("should have hasExcessiveContextSwitching method")
    void shouldHaveHasExcessiveContextSwitchingMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("hasExcessiveContextSwitching");
      assertNotNull(method, "hasExcessiveContextSwitching method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "hasExcessiveContextSwitching should return boolean");
    }

    @Test
    @DisplayName("should have getPerformanceScore method")
    void shouldHaveGetPerformanceScoreMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getPerformanceScore");
      assertNotNull(method, "getPerformanceScore method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getPerformanceScore should return double");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have formatPercentage static method")
    void shouldHaveFormatPercentageStaticMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("formatPercentage", double.class);
      assertNotNull(method, "formatPercentage method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "formatPercentage should be static");
      assertEquals(String.class, method.getReturnType(), "formatPercentage should return String");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final CpuUsage cpuUsage = new CpuUsage(0.5, 0.2, 0.7, 0.8, 1000L, 500L);

      assertEquals(0.5, cpuUsage.getUserCpuTime(), 0.001, "User CPU time should match");
      assertEquals(0.2, cpuUsage.getSystemCpuTime(), 0.001, "System CPU time should match");
      assertEquals(0.7, cpuUsage.getTotalCpuTime(), 0.001, "Total CPU time should match");
      assertEquals(0.8, cpuUsage.getCpuUtilization(), 0.001, "CPU utilization should match");
      assertEquals(1000L, cpuUsage.getContextSwitches(), "Context switches should match");
      assertEquals(500L, cpuUsage.getSystemCalls(), "System calls should match");
    }

    @Test
    @DisplayName("should clamp values to valid range")
    void shouldClampValuesToValidRange() {
      final CpuUsage cpuUsage = new CpuUsage(1.5, -0.2, 0.7, 0.8, -100L, -50L);

      assertEquals(1.0, cpuUsage.getUserCpuTime(), 0.001, "User CPU time should be clamped to 1.0");
      assertEquals(
          0.0, cpuUsage.getSystemCpuTime(), 0.001, "System CPU time should be clamped to 0.0");
      assertEquals(0L, cpuUsage.getContextSwitches(), "Context switches should be clamped to 0");
      assertEquals(0L, cpuUsage.getSystemCalls(), "System calls should be clamped to 0");
    }

    @Test
    @DisplayName("should detect high CPU usage")
    void shouldDetectHighCpuUsage() {
      final CpuUsage highUsage = new CpuUsage(0.5, 0.2, 0.7, 0.9, 1000L, 500L);
      assertTrue(highUsage.isHighCpuUsage(), "Should detect high CPU usage");

      final CpuUsage lowUsage = new CpuUsage(0.5, 0.2, 0.7, 0.5, 1000L, 500L);
      assertFalse(lowUsage.isHighCpuUsage(), "Should not detect high CPU usage");
    }

    @Test
    @DisplayName("should format percentage correctly")
    void shouldFormatPercentageCorrectly() {
      assertEquals("50.0%", CpuUsage.formatPercentage(0.5), "Should format 0.5 as 50.0%");
      assertEquals("100.0%", CpuUsage.formatPercentage(1.0), "Should format 1.0 as 100.0%");
    }
  }
}
