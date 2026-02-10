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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the performance package classes.
 *
 * <p>This package provides performance metrics, profiling, and compilation statistics.
 */
@DisplayName("Performance Package Tests")
class PerformancePackageTest {

  @Nested
  @DisplayName("PerformanceMetrics Tests")
  class PerformanceMetricsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PerformanceMetrics.class.getModifiers()),
          "PerformanceMetrics should be final");
    }

    @Test
    @DisplayName("should have constructor with all duration parameters")
    void shouldHaveConstructorWithAllDurationParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PerformanceMetrics.class.getConstructor(
              Duration.class,
              Duration.class,
              Duration.class,
              Duration.class,
              Duration.class,
              Duration.class);
      assertNotNull(constructor, "Constructor should exist");
    }

    @Test
    @DisplayName("should have getMin method")
    void shouldHaveGetMinMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getMin");
      assertNotNull(method, "getMin method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMax method")
    void shouldHaveGetMaxMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getMax");
      assertNotNull(method, "getMax method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getAverage method")
    void shouldHaveGetAverageMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getAverage");
      assertNotNull(method, "getAverage method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMedian method")
    void shouldHaveGetMedianMethod() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getMedian");
      assertNotNull(method, "getMedian method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getP95 method")
    void shouldHaveGetP95Method() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getP95");
      assertNotNull(method, "getP95 method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getP99 method")
    void shouldHaveGetP99Method() throws NoSuchMethodException {
      final Method method = PerformanceMetrics.class.getMethod("getP99");
      assertNotNull(method, "getP99 method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should create instance with durations")
    void shouldCreateInstanceWithDurations() {
      final Duration min = Duration.ofMillis(1);
      final Duration max = Duration.ofMillis(100);
      final Duration average = Duration.ofMillis(50);
      final Duration median = Duration.ofMillis(45);
      final Duration p95 = Duration.ofMillis(90);
      final Duration p99 = Duration.ofMillis(98);

      final PerformanceMetrics metrics =
          new PerformanceMetrics(min, max, average, median, p95, p99);

      assertEquals(min, metrics.getMin(), "Min should match");
      assertEquals(max, metrics.getMax(), "Max should match");
      assertEquals(average, metrics.getAverage(), "Average should match");
      assertEquals(median, metrics.getMedian(), "Median should match");
      assertEquals(p95, metrics.getP95(), "P95 should match");
      assertEquals(p99, metrics.getP99(), "P99 should match");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final PerformanceMetrics metrics =
          new PerformanceMetrics(
              Duration.ofMillis(1),
              Duration.ofMillis(100),
              Duration.ofMillis(50),
              Duration.ofMillis(45),
              Duration.ofMillis(90),
              Duration.ofMillis(98));

      assertNotNull(metrics.toString(), "toString should not return null");
      assertFalse(metrics.toString().isEmpty(), "toString should not be empty");
    }
  }

  @Nested
  @DisplayName("CompilationPhase Tests")
  class CompilationPhaseTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CompilationPhase.class.getModifiers()),
          "CompilationPhase should be final");
    }

    @Test
    @DisplayName("should have constructor with name, duration, and metrics")
    void shouldHaveConstructorWithNameDurationAndMetrics() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CompilationPhase.class.getConstructor(String.class, Duration.class, Map.class);
      assertNotNull(constructor, "Constructor should exist");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = CompilationPhase.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final CompilationPhase phase =
          new CompilationPhase("parsing", Duration.ofMillis(100), Map.of("lines", 1000));

      assertEquals("parsing", phase.getName(), "Name should match");
      assertEquals(Duration.ofMillis(100), phase.getDuration(), "Duration should match");
      assertNotNull(phase.getMetrics(), "Metrics should not be null");
    }

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      assertThrows(
          NullPointerException.class,
          () -> new CompilationPhase(null, Duration.ofMillis(100), Map.of()),
          "Should reject null name");
    }

    @Test
    @DisplayName("should reject null duration")
    void shouldRejectNullDuration() {
      assertThrows(
          NullPointerException.class,
          () -> new CompilationPhase("test", null, Map.of()),
          "Should reject null duration");
    }

    @Test
    @DisplayName("should reject null metrics")
    void shouldRejectNullMetrics() {
      assertThrows(
          NullPointerException.class,
          () -> new CompilationPhase("test", Duration.ofMillis(100), null),
          "Should reject null metrics");
    }

    @Test
    @DisplayName("should reject empty name")
    void shouldRejectEmptyName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CompilationPhase("  ", Duration.ofMillis(100), Map.of()),
          "Should reject empty name");
    }

    @Test
    @DisplayName("should reject negative duration")
    void shouldRejectNegativeDuration() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CompilationPhase("test", Duration.ofMillis(-1), Map.of()),
          "Should reject negative duration");
    }
  }

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatEnumTests {

    @Test
    @DisplayName("should have all expected export formats")
    void shouldHaveAllExpectedExportFormats() {
      final ExportFormat[] formats = ExportFormat.values();

      assertEquals(6, formats.length, "Should have 6 export formats");

      final Set<String> formatNames =
          Set.of("JSON", "CSV", "BINARY", "JFR", "FLAME_GRAPH", "JMH_JSON");

      for (final ExportFormat format : formats) {
        assertTrue(
            formatNames.contains(format.name()), "Format " + format.name() + " should be expected");
      }
    }

    @Test
    @DisplayName("valueOf should return correct format")
    void valueOfShouldReturnCorrectFormat() {
      assertEquals(ExportFormat.JSON, ExportFormat.valueOf("JSON"));
      assertEquals(ExportFormat.CSV, ExportFormat.valueOf("CSV"));
    }

    @Test
    @DisplayName("export formats should have file extension")
    void exportFormatsShouldHaveFileExtension() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("getFileExtension");
      assertNotNull(method, "getFileExtension method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("export formats should have mime type")
    void exportFormatsShouldHaveMimeType() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("getMimeType");
      assertNotNull(method, "getMimeType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("each format should have non-empty file extension")
    void eachFormatShouldHaveNonEmptyFileExtension() {
      for (final ExportFormat format : ExportFormat.values()) {
        assertNotNull(
            format.getFileExtension(), format.name() + " file extension should not be null");
        assertFalse(
            format.getFileExtension().isEmpty(),
            format.name() + " file extension should not be empty");
      }
    }

    @Test
    @DisplayName("each format should have non-empty mime type")
    void eachFormatShouldHaveNonEmptyMimeType() {
      for (final ExportFormat format : ExportFormat.values()) {
        assertNotNull(format.getMimeType(), format.name() + " mime type should not be null");
        assertFalse(
            format.getMimeType().isEmpty(), format.name() + " mime type should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("MemoryUsage Tests")
  class MemoryUsageTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(MemoryUsage.class.getModifiers()), "MemoryUsage should be final");
    }

    @Test
    @DisplayName("should have getHeapUsed method")
    void shouldHaveGetHeapUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getHeapUsed");
      assertNotNull(method, "getHeapUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getHeapMax method")
    void shouldHaveGetHeapMaxMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getHeapMax");
      assertNotNull(method, "getHeapMax method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getNonHeapUsed method")
    void shouldHaveGetNonHeapUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryUsage.class.getMethod("getNonHeapUsed");
      assertNotNull(method, "getNonHeapUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("CpuUsage Tests")
  class CpuUsageTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(CpuUsage.class.getModifiers()), "CpuUsage should be final");
    }

    @Test
    @DisplayName("should have getUserCpuTime method")
    void shouldHaveGetUserCpuTimeMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getUserCpuTime");
      assertNotNull(method, "getUserCpuTime method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getSystemCpuTime method")
    void shouldHaveGetSystemCpuTimeMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getSystemCpuTime");
      assertNotNull(method, "getSystemCpuTime method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getCpuUtilization method")
    void shouldHaveGetCpuUtilizationMethod() throws NoSuchMethodException {
      final Method method = CpuUsage.class.getMethod("getCpuUtilization");
      assertNotNull(method, "getCpuUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("ThreadUsage Tests")
  class ThreadUsageTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(ThreadUsage.class.getModifiers()), "ThreadUsage should be final");
    }

    @Test
    @DisplayName("should have getActiveThreads method")
    void shouldHaveGetActiveThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getActiveThreads");
      assertNotNull(method, "getActiveThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPeakThreads method")
    void shouldHaveGetPeakThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getPeakThreads");
      assertNotNull(method, "getPeakThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getTotalStartedThreads method")
    void shouldHaveGetTotalStartedThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getTotalStartedThreads");
      assertNotNull(method, "getTotalStartedThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("IoUsage Tests")
  class IoUsageTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(IoUsage.class.getModifiers()), "IoUsage should be final");
    }

    @Test
    @DisplayName("should have getBytesRead method")
    void shouldHaveGetBytesReadMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getBytesRead");
      assertNotNull(method, "getBytesRead method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBytesWritten method")
    void shouldHaveGetBytesWrittenMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getBytesWritten");
      assertNotNull(method, "getBytesWritten method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("NetworkUsage Tests")
  class NetworkUsageTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(NetworkUsage.class.getModifiers()), "NetworkUsage should be final");
    }

    @Test
    @DisplayName("should have getBytesReceived method")
    void shouldHaveGetBytesReceivedMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getBytesReceived");
      assertNotNull(method, "getBytesReceived method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBytesSent method")
    void shouldHaveGetBytesSentMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getBytesSent");
      assertNotNull(method, "getBytesSent method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("ProfileMetric Tests")
  class ProfileMetricTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ProfileMetric.class.isEnum(), "ProfileMetric should be an enum");
    }

    @Test
    @DisplayName("should have getDisplayName method")
    void shouldHaveGetDisplayNameMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("getDisplayName");
      assertNotNull(method, "getDisplayName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getOverheadFactor method")
    void shouldHaveGetOverheadFactorMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("getOverheadFactor");
      assertNotNull(method, "getOverheadFactor method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Target Tests")
  class TargetTests {

    @Test
    @DisplayName("Target should be an enum or class")
    void targetShouldBeEnumOrClass() {
      assertTrue(
          Target.class.isEnum() || Modifier.isFinal(Target.class.getModifiers()),
          "Target should be an enum or final class");
    }
  }

  @Nested
  @DisplayName("FunctionStatistics Tests")
  class FunctionStatisticsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(FunctionStatistics.class.getModifiers()),
          "FunctionStatistics should be final");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getIndex");
      assertNotNull(method, "getIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getBytecodeSize method")
    void shouldHaveGetBytecodeSizeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getBytecodeSize");
      assertNotNull(method, "getBytecodeSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCompilationTime method")
    void shouldHaveGetCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getCompilationTime");
      assertNotNull(method, "getCompilationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("EngineStatistics Tests")
  class EngineStatisticsTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(EngineStatistics.class.isInterface(), "EngineStatistics should be an interface");
    }

    @Test
    @DisplayName("should have getModulesCompiled method")
    void shouldHaveGetModulesCompiledMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getModulesCompiled");
      assertNotNull(method, "getModulesCompiled method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFunctionsExecuted method")
    void shouldHaveGetFunctionsExecutedMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getFunctionsExecuted");
      assertNotNull(method, "getFunctionsExecuted method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalCompilationTime method")
    void shouldHaveGetTotalCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getTotalCompilationTime");
      assertNotNull(method, "getTotalCompilationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("CompilerConfig Tests")
  class CompilerConfigTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CompilerConfig.class.getModifiers()), "CompilerConfig should be final");
    }

    @Test
    @DisplayName("should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have hasDebugInfo method")
    void shouldHaveHasDebugInfoMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("hasDebugInfo");
      assertNotNull(method, "hasDebugInfo method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getCompilerName method")
    void shouldHaveGetCompilerNameMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("getCompilerName");
      assertNotNull(method, "getCompilerName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
