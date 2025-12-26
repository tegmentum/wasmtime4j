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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompilationPhase} class.
 *
 * <p>CompilationPhase represents a phase in the WebAssembly compilation process with timing and
 * metrics.
 */
@DisplayName("CompilationPhase Tests")
class CompilationPhaseTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CompilationPhase.class.getModifiers()),
          "CompilationPhase should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CompilationPhase.class.getModifiers()),
          "CompilationPhase should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CompilationPhase.class.getConstructor(String.class, Duration.class, Map.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getDuration should return Duration");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(Map.class, method.getReturnType(), "getMetrics should return Map");
    }
  }

  @Nested
  @DisplayName("Derived Metric Method Tests")
  class DerivedMetricMethodTests {

    @Test
    @DisplayName("should have getPercentageOfTotal method")
    void shouldHaveGetPercentageOfTotalMethod() throws NoSuchMethodException {
      final Method method =
          CompilationPhase.class.getMethod("getPercentageOfTotal", Duration.class);
      assertNotNull(method, "getPercentageOfTotal method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getPercentageOfTotal should return double");
    }

    @Test
    @DisplayName("should have isSignificantPhase method")
    void shouldHaveIsSignificantPhaseMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("isSignificantPhase", Duration.class);
      assertNotNull(method, "isSignificantPhase method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isSignificantPhase should return boolean");
    }

    @Test
    @DisplayName("should have getMetricAsString method")
    void shouldHaveGetMetricAsStringMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getMetricAsString", String.class);
      assertNotNull(method, "getMetricAsString method should exist");
      assertEquals(String.class, method.getReturnType(), "getMetricAsString should return String");
    }

    @Test
    @DisplayName("should have getMetricAsNumber method")
    void shouldHaveGetMetricAsNumberMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getMetricAsNumber", String.class);
      assertNotNull(method, "getMetricAsNumber method should exist");
      assertEquals(double.class, method.getReturnType(), "getMetricAsNumber should return double");
    }

    @Test
    @DisplayName("should have hasMetric method")
    void shouldHaveHasMetricMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("hasMetric", String.class);
      assertNotNull(method, "hasMetric method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasMetric should return boolean");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final Map<String, Object> metrics = Map.of("iterations", 100, "optimizations", 5);
      final CompilationPhase phase =
          new CompilationPhase("optimization", Duration.ofMillis(250), metrics);

      assertEquals("optimization", phase.getName(), "Name should match");
      assertEquals(Duration.ofMillis(250), phase.getDuration(), "Duration should match");
      assertEquals(metrics, phase.getMetrics(), "Metrics should match");
    }

    @Test
    @DisplayName("should throw exception for null name")
    void shouldThrowExceptionForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> new CompilationPhase(null, Duration.ofMillis(100), Map.of()),
          "Should throw exception for null name");
    }

    @Test
    @DisplayName("should throw exception for empty name")
    void shouldThrowExceptionForEmptyName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CompilationPhase("  ", Duration.ofMillis(100), Map.of()),
          "Should throw exception for empty name");
    }

    @Test
    @DisplayName("should throw exception for negative duration")
    void shouldThrowExceptionForNegativeDuration() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CompilationPhase("parsing", Duration.ofMillis(-100), Map.of()),
          "Should throw exception for negative duration");
    }

    @Test
    @DisplayName("should calculate percentage of total correctly")
    void shouldCalculatePercentageOfTotalCorrectly() {
      final CompilationPhase phase =
          new CompilationPhase("optimization", Duration.ofMillis(250), Map.of());
      final Duration totalTime = Duration.ofMillis(1000);

      assertEquals(25.0, phase.getPercentageOfTotal(totalTime), 0.001, "Percentage should be 25%");
    }

    @Test
    @DisplayName("should handle zero total time")
    void shouldHandleZeroTotalTime() {
      final CompilationPhase phase =
          new CompilationPhase("optimization", Duration.ofMillis(250), Map.of());

      assertEquals(
          0.0,
          phase.getPercentageOfTotal(Duration.ZERO),
          0.001,
          "Should return 0 for zero total time");
    }

    @Test
    @DisplayName("should detect significant phase")
    void shouldDetectSignificantPhase() {
      final CompilationPhase phase =
          new CompilationPhase("optimization", Duration.ofMillis(300), Map.of());
      final Duration totalTime = Duration.ofMillis(1000);

      assertTrue(phase.isSignificantPhase(totalTime), "30% should be significant");
    }

    @Test
    @DisplayName("should detect non-significant phase")
    void shouldDetectNonSignificantPhase() {
      final CompilationPhase phase =
          new CompilationPhase("validation", Duration.ofMillis(100), Map.of());
      final Duration totalTime = Duration.ofMillis(1000);

      assertFalse(phase.isSignificantPhase(totalTime), "10% should not be significant");
    }

    @Test
    @DisplayName("should get metric as string")
    void shouldGetMetricAsString() {
      final Map<String, Object> metrics = Map.of("mode", "aggressive", "level", 3);
      final CompilationPhase phase =
          new CompilationPhase("optimization", Duration.ofMillis(100), metrics);

      assertEquals("aggressive", phase.getMetricAsString("mode"), "Should return metric as string");
      assertEquals("3", phase.getMetricAsString("level"), "Should convert number to string");
      assertEquals(
          "", phase.getMetricAsString("unknown"), "Should return empty string for unknown metric");
    }

    @Test
    @DisplayName("should get metric as number")
    void shouldGetMetricAsNumber() {
      final Map<String, Object> metrics = Map.of("iterations", 100, "ratio", 0.75);
      final CompilationPhase phase =
          new CompilationPhase("optimization", Duration.ofMillis(100), metrics);

      assertEquals(
          100.0, phase.getMetricAsNumber("iterations"), 0.001, "Should return integer as double");
      assertEquals(0.75, phase.getMetricAsNumber("ratio"), 0.001, "Should return double");
      assertEquals(
          0.0, phase.getMetricAsNumber("unknown"), 0.001, "Should return 0 for unknown metric");
    }

    @Test
    @DisplayName("should check if metric exists")
    void shouldCheckIfMetricExists() {
      final Map<String, Object> metrics = Map.of("iterations", 100);
      final CompilationPhase phase =
          new CompilationPhase("optimization", Duration.ofMillis(100), metrics);

      assertTrue(phase.hasMetric("iterations"), "Should detect existing metric");
      assertFalse(phase.hasMetric("unknown"), "Should not detect unknown metric");
    }
  }
}
