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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.profiler.Profiler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniProfiler} class.
 *
 * <p>JniProfiler provides JNI implementation of profiling functionality for WebAssembly execution.
 */
@DisplayName("JniProfiler Tests")
class JniProfilerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(Modifier.isPublic(JniProfiler.class.getModifiers()), "JniProfiler should be public");
      assertTrue(Modifier.isFinal(JniProfiler.class.getModifiers()), "JniProfiler should be final");
    }

    @Test
    @DisplayName("should implement Profiler interface")
    void shouldImplementProfilerInterface() {
      assertTrue(Profiler.class.isAssignableFrom(JniProfiler.class), "JniProfiler should implement Profiler");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = JniProfiler.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Profiling Control Tests")
  class ProfilingControlTests {

    @Test
    @DisplayName("should have startProfiling method")
    void shouldHaveStartProfilingMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("startProfiling");
      assertNotNull(method, "startProfiling method should exist");
      assertEquals(void.class, method.getReturnType(), "startProfiling should return void");
    }

    @Test
    @DisplayName("should have stopProfiling method")
    void shouldHaveStopProfilingMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("stopProfiling");
      assertNotNull(method, "stopProfiling method should exist");
      assertEquals(void.class, method.getReturnType(), "stopProfiling should return void");
    }

    @Test
    @DisplayName("should have isProfiling method")
    void shouldHaveIsProfilingMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("isProfiling");
      assertNotNull(method, "isProfiling method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isProfiling should return boolean");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "reset should return void");
    }
  }

  @Nested
  @DisplayName("Recording Methods Tests")
  class RecordingMethodsTests {

    @Test
    @DisplayName("should have recordFunctionExecution method")
    void shouldHaveRecordFunctionExecutionMethod() throws NoSuchMethodException {
      final Method method =
          JniProfiler.class.getMethod("recordFunctionExecution", String.class, Duration.class, long.class);
      assertNotNull(method, "recordFunctionExecution method should exist");
      assertEquals(void.class, method.getReturnType(), "recordFunctionExecution should return void");
    }

    @Test
    @DisplayName("should have recordCompilation method")
    void shouldHaveRecordCompilationMethod() throws NoSuchMethodException {
      final Method method =
          JniProfiler.class.getMethod("recordCompilation", Duration.class, long.class, boolean.class, boolean.class);
      assertNotNull(method, "recordCompilation method should exist");
      assertEquals(void.class, method.getReturnType(), "recordCompilation should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Methods Tests")
  class StatisticsMethodsTests {

    @Test
    @DisplayName("should have getModulesCompiled method")
    void shouldHaveGetModulesCompiledMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getModulesCompiled");
      assertNotNull(method, "getModulesCompiled method should exist");
      assertEquals(long.class, method.getReturnType(), "getModulesCompiled should return long");
    }

    @Test
    @DisplayName("should have getTotalCompilationTime method")
    void shouldHaveGetTotalCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getTotalCompilationTime");
      assertNotNull(method, "getTotalCompilationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getTotalCompilationTime should return Duration");
    }

    @Test
    @DisplayName("should have getAverageCompilationTime method")
    void shouldHaveGetAverageCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getAverageCompilationTime");
      assertNotNull(method, "getAverageCompilationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getAverageCompilationTime should return Duration");
    }

    @Test
    @DisplayName("should have getCacheHits method")
    void shouldHaveGetCacheHitsMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getCacheHits");
      assertNotNull(method, "getCacheHits method should exist");
      assertEquals(long.class, method.getReturnType(), "getCacheHits should return long");
    }

    @Test
    @DisplayName("should have getCacheMisses method")
    void shouldHaveGetCacheMissesMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getCacheMisses");
      assertNotNull(method, "getCacheMisses method should exist");
      assertEquals(long.class, method.getReturnType(), "getCacheMisses should return long");
    }

    @Test
    @DisplayName("should have getCurrentMemoryBytes method")
    void shouldHaveGetCurrentMemoryBytesMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getCurrentMemoryBytes");
      assertNotNull(method, "getCurrentMemoryBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "getCurrentMemoryBytes should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryBytes method")
    void shouldHaveGetPeakMemoryBytesMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getPeakMemoryBytes");
      assertNotNull(method, "getPeakMemoryBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "getPeakMemoryBytes should return long");
    }

    @Test
    @DisplayName("should have getTotalFunctionCalls method")
    void shouldHaveGetTotalFunctionCallsMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("getTotalFunctionCalls");
      assertNotNull(method, "getTotalFunctionCalls method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalFunctionCalls should return long");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniProfiler.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }
}
