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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PerformanceMetrics} class.
 *
 * <p>PerformanceMetrics provides statistical measures from duration measurements.
 */
@DisplayName("PerformanceMetrics Tests")
class PerformanceMetricsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PerformanceMetrics.class.getModifiers()),
          "PerformanceMetrics should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(PerformanceMetrics.class.getModifiers()),
          "PerformanceMetrics should be public");
    }

    @Test
    @DisplayName("should have constructor with all Duration parameters")
    void shouldHaveConstructorWithDurationParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PerformanceMetrics.class.getConstructor(
              Duration.class,
              Duration.class,
              Duration.class,
              Duration.class,
              Duration.class,
              Duration.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getMin method")
    void shouldHaveGetMinMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getMin");
      assertNotNull(method, "getMin method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getMin should return Duration");
    }

    @Test
    @DisplayName("should have getMax method")
    void shouldHaveGetMaxMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getMax");
      assertNotNull(method, "getMax method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getMax should return Duration");
    }

    @Test
    @DisplayName("should have getAverage method")
    void shouldHaveGetAverageMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getAverage");
      assertNotNull(method, "getAverage method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getAverage should return Duration");
    }

    @Test
    @DisplayName("should have getMedian method")
    void shouldHaveGetMedianMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getMedian");
      assertNotNull(method, "getMedian method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getMedian should return Duration");
    }

    @Test
    @DisplayName("should have getP95 method")
    void shouldHaveGetP95Method() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getP95");
      assertNotNull(method, "getP95 method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getP95 should return Duration");
    }

    @Test
    @DisplayName("should have getP99 method")
    void shouldHaveGetP99Method() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getP99");
      assertNotNull(method, "getP99 method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getP99 should return Duration");
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final PerformanceMetrics metrics =
          new PerformanceMetrics(
              Duration.ofMillis(10),
              Duration.ofMillis(100),
              Duration.ofMillis(50),
              Duration.ofMillis(45),
              Duration.ofMillis(90),
              Duration.ofMillis(95));

      assertEquals(Duration.ofMillis(10), metrics.getMin(), "Min should match");
      assertEquals(Duration.ofMillis(100), metrics.getMax(), "Max should match");
      assertEquals(Duration.ofMillis(50), metrics.getAverage(), "Average should match");
      assertEquals(Duration.ofMillis(45), metrics.getMedian(), "Median should match");
      assertEquals(Duration.ofMillis(90), metrics.getP95(), "P95 should match");
      assertEquals(Duration.ofMillis(95), metrics.getP99(), "P99 should match");
    }

    @Test
    @DisplayName("toString should contain all metric values")
    void toStringShouldContainAllMetricValues() {
      final PerformanceMetrics metrics =
          new PerformanceMetrics(
              Duration.ofMillis(10),
              Duration.ofMillis(100),
              Duration.ofMillis(50),
              Duration.ofMillis(45),
              Duration.ofMillis(90),
              Duration.ofMillis(95));

      final String result = metrics.toString();
      assertTrue(result.contains("10"), "toString should contain min value");
      assertTrue(result.contains("100"), "toString should contain max value");
      assertTrue(result.contains("50"), "toString should contain average value");
      assertTrue(result.contains("45"), "toString should contain median value");
      assertTrue(result.contains("90"), "toString should contain p95 value");
      assertTrue(result.contains("95"), "toString should contain p99 value");
    }
  }
}
