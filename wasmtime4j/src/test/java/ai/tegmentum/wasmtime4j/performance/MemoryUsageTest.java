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
 * Tests for {@link MemoryUsage} class.
 *
 * <p>MemoryUsage provides detailed memory usage information for the JVM and native components.
 */
@DisplayName("MemoryUsage Tests")
class MemoryUsageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(MemoryUsage.class.getModifiers()), "MemoryUsage should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(MemoryUsage.class.getModifiers()), "MemoryUsage should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          MemoryUsage.class.getConstructor(
              long.class,
              long.class,
              long.class,
              long.class,
              long.class,
              long.class,
              long.class,
              long.class,
              Duration.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getHeapUsed method")
    void shouldHaveGetHeapUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getHeapUsed");
      assertNotNull(method, "getHeapUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getHeapUsed should return long");
    }

    @Test
    @DisplayName("should have getHeapCommitted method")
    void shouldHaveGetHeapCommittedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getHeapCommitted");
      assertNotNull(method, "getHeapCommitted method should exist");
      assertEquals(long.class, method.getReturnType(), "getHeapCommitted should return long");
    }

    @Test
    @DisplayName("should have getHeapMax method")
    void shouldHaveGetHeapMaxMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getHeapMax");
      assertNotNull(method, "getHeapMax method should exist");
      assertEquals(long.class, method.getReturnType(), "getHeapMax should return long");
    }

    @Test
    @DisplayName("should have getNonHeapUsed method")
    void shouldHaveGetNonHeapUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getNonHeapUsed");
      assertNotNull(method, "getNonHeapUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getNonHeapUsed should return long");
    }

    @Test
    @DisplayName("should have getNonHeapCommitted method")
    void shouldHaveGetNonHeapCommittedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getNonHeapCommitted");
      assertNotNull(method, "getNonHeapCommitted method should exist");
      assertEquals(long.class, method.getReturnType(), "getNonHeapCommitted should return long");
    }

    @Test
    @DisplayName("should have getDirectMemoryUsed method")
    void shouldHaveGetDirectMemoryUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getDirectMemoryUsed");
      assertNotNull(method, "getDirectMemoryUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getDirectMemoryUsed should return long");
    }

    @Test
    @DisplayName("should have getNativeMemoryUsed method")
    void shouldHaveGetNativeMemoryUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getNativeMemoryUsed");
      assertNotNull(method, "getNativeMemoryUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getNativeMemoryUsed should return long");
    }

    @Test
    @DisplayName("should have getGcCount method")
    void shouldHaveGetGcCountMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getGcCount");
      assertNotNull(method, "getGcCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getGcCount should return long");
    }

    @Test
    @DisplayName("should have getGcTime method")
    void shouldHaveGetGcTimeMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getGcTime");
      assertNotNull(method, "getGcTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getGcTime should return Duration");
    }
  }

  @Nested
  @DisplayName("Derived Metric Method Tests")
  class DerivedMetricMethodTests {

    @Test
    @DisplayName("should have getHeapUtilization method")
    void shouldHaveGetHeapUtilizationMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getHeapUtilization");
      assertNotNull(method, "getHeapUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "getHeapUtilization should return double");
    }

    @Test
    @DisplayName("should have getTotalMemoryUsed method")
    void shouldHaveGetTotalMemoryUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getTotalMemoryUsed");
      assertNotNull(method, "getTotalMemoryUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalMemoryUsed should return long");
    }

    @Test
    @DisplayName("should have isMemoryPressure method")
    void shouldHaveIsMemoryPressureMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("isMemoryPressure");
      assertNotNull(method, "isMemoryPressure method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isMemoryPressure should return boolean");
    }

    @Test
    @DisplayName("should have isFrequentGarbageCollection method")
    void shouldHaveIsFrequentGarbageCollectionMethod() throws NoSuchMethodException {
      final Method method =
          MemoryUsage.class.getMethod("isFrequentGarbageCollection", Duration.class);
      assertNotNull(method, "isFrequentGarbageCollection method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isFrequentGarbageCollection should return boolean");
    }

    @Test
    @DisplayName("should have getEfficiencyScore method")
    void shouldHaveGetEfficiencyScoreMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getEfficiencyScore", Duration.class);
      assertNotNull(method, "getEfficiencyScore method should exist");
      assertEquals(double.class, method.getReturnType(), "getEfficiencyScore should return double");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have formatBytes static method")
    void shouldHaveFormatBytesStaticMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("formatBytes", long.class);
      assertNotNull(method, "formatBytes method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "formatBytes should be static");
      assertEquals(String.class, method.getReturnType(), "formatBytes should return String");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final MemoryUsage memoryUsage =
          new MemoryUsage(
              1024L * 1024L * 500L, // heapUsed
              1024L * 1024L * 600L, // heapCommitted
              1024L * 1024L * 1024L, // heapMax
              1024L * 1024L * 100L, // nonHeapUsed
              1024L * 1024L * 150L, // nonHeapCommitted
              1024L * 1024L * 50L, // directMemoryUsed
              1024L * 1024L * 20L, // nativeMemoryUsed
              10L, // gcCount
              Duration.ofMillis(500)); // gcTime

      assertEquals(1024L * 1024L * 500L, memoryUsage.getHeapUsed(), "Heap used should match");
      assertEquals(10L, memoryUsage.getGcCount(), "GC count should match");
      assertEquals(Duration.ofMillis(500), memoryUsage.getGcTime(), "GC time should match");
    }

    @Test
    @DisplayName("should calculate heap utilization correctly")
    void shouldCalculateHeapUtilizationCorrectly() {
      final MemoryUsage memoryUsage =
          new MemoryUsage(500L, 600L, 1000L, 100L, 150L, 50L, 20L, 10L, Duration.ofMillis(500));

      assertEquals(0.5, memoryUsage.getHeapUtilization(), 0.001, "Heap utilization should be 50%");
    }

    @Test
    @DisplayName("should format bytes correctly")
    void shouldFormatBytesCorrectly() {
      assertEquals("0 B", MemoryUsage.formatBytes(0), "Should format 0 bytes");
      assertEquals("1023 B", MemoryUsage.formatBytes(1023), "Should format bytes");
      assertEquals("1.0 KB", MemoryUsage.formatBytes(1024), "Should format KB");
      assertEquals("1.0 MB", MemoryUsage.formatBytes(1024L * 1024L), "Should format MB");
      assertEquals("1.0 GB", MemoryUsage.formatBytes(1024L * 1024L * 1024L), "Should format GB");
    }
  }
}
