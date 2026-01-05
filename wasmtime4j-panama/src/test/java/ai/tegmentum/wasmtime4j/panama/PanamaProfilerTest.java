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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.profiler.Profiler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaProfiler} class.
 *
 * <p>PanamaProfiler provides profiling for WebAssembly execution.
 */
@DisplayName("PanamaProfiler Tests")
class PanamaProfilerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaProfiler.class.getModifiers()),
          "PanamaProfiler should be public");
      assertTrue(
          Modifier.isFinal(PanamaProfiler.class.getModifiers()), "PanamaProfiler should be final");
    }

    @Test
    @DisplayName("should implement Profiler interface")
    void shouldImplementProfilerInterface() {
      assertTrue(
          Profiler.class.isAssignableFrom(PanamaProfiler.class),
          "PanamaProfiler should implement Profiler");
    }
  }

  @Nested
  @DisplayName("Profiling Control Method Tests")
  class ProfilingControlMethodTests {

    @Test
    @DisplayName("should have startProfiling method")
    void shouldHaveStartProfilingMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("startProfiling");
      assertNotNull(method, "startProfiling method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stopProfiling method")
    void shouldHaveStopProfilingMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("stopProfiling");
      assertNotNull(method, "stopProfiling method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isProfiling method")
    void shouldHaveIsProfilingMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("isProfiling");
      assertNotNull(method, "isProfiling method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Recording Method Tests")
  class RecordingMethodTests {

    @Test
    @DisplayName("should have recordFunctionExecution method")
    void shouldHaveRecordFunctionExecutionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaProfiler.class.getMethod(
              "recordFunctionExecution", String.class, Duration.class, long.class);
      assertNotNull(method, "recordFunctionExecution method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have recordCompilation method")
    void shouldHaveRecordCompilationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaProfiler.class.getMethod(
              "recordCompilation", Duration.class, long.class, boolean.class, boolean.class);
      assertNotNull(method, "recordCompilation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Compilation Statistics Method Tests")
  class CompilationStatisticsMethodTests {

    @Test
    @DisplayName("should have getModulesCompiled method")
    void shouldHaveGetModulesCompiledMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getModulesCompiled");
      assertNotNull(method, "getModulesCompiled method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalCompilationTime method")
    void shouldHaveGetTotalCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getTotalCompilationTime");
      assertNotNull(method, "getTotalCompilationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getAverageCompilationTime method")
    void shouldHaveGetAverageCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getAverageCompilationTime");
      assertNotNull(method, "getAverageCompilationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getBytesCompiled method")
    void shouldHaveGetBytesCompiledMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getBytesCompiled");
      assertNotNull(method, "getBytesCompiled method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getOptimizedModules method")
    void shouldHaveGetOptimizedModulesMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getOptimizedModules");
      assertNotNull(method, "getOptimizedModules method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Cache Statistics Method Tests")
  class CacheStatisticsMethodTests {

    @Test
    @DisplayName("should have getCacheHits method")
    void shouldHaveGetCacheHitsMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getCacheHits");
      assertNotNull(method, "getCacheHits method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCacheMisses method")
    void shouldHaveGetCacheMissesMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getCacheMisses");
      assertNotNull(method, "getCacheMisses method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Memory Statistics Method Tests")
  class MemoryStatisticsMethodTests {

    @Test
    @DisplayName("should have getCurrentMemoryBytes method")
    void shouldHaveGetCurrentMemoryBytesMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getCurrentMemoryBytes");
      assertNotNull(method, "getCurrentMemoryBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryBytes method")
    void shouldHaveGetPeakMemoryBytesMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getPeakMemoryBytes");
      assertNotNull(method, "getPeakMemoryBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Execution Statistics Method Tests")
  class ExecutionStatisticsMethodTests {

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getFunctionCallsPerSecond method")
    void shouldHaveGetFunctionCallsPerSecondMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getFunctionCallsPerSecond");
      assertNotNull(method, "getFunctionCallsPerSecond method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getTotalFunctionCalls method")
    void shouldHaveGetTotalFunctionCallsMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getTotalFunctionCalls");
      assertNotNull(method, "getTotalFunctionCalls method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaProfiler.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
      try {
        PanamaProfiler.class.getConstructor();
      } catch (NoSuchMethodException e) {
        throw new AssertionError("Default constructor should exist", e);
      }
    }
  }
}
