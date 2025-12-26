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
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FunctionStatistics} class.
 *
 * <p>FunctionStatistics provides detailed compilation statistics for individual functions.
 */
@DisplayName("FunctionStatistics Tests")
class FunctionStatisticsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(FunctionStatistics.class.getModifiers()),
          "FunctionStatistics should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FunctionStatistics.class.getModifiers()),
          "FunctionStatistics should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          FunctionStatistics.class.getConstructor(
              String.class,
              int.class,
              long.class,
              long.class,
              Duration.class,
              int.class,
              Map.class);
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
      final Method method = FunctionStatistics.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getIndex");
      assertNotNull(method, "getIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "getIndex should return int");
    }

    @Test
    @DisplayName("should have getBytecodeSize method")
    void shouldHaveGetBytecodeSizeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getBytecodeSize");
      assertNotNull(method, "getBytecodeSize method should exist");
      assertEquals(long.class, method.getReturnType(), "getBytecodeSize should return long");
    }

    @Test
    @DisplayName("should have getCompiledSize method")
    void shouldHaveGetCompiledSizeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getCompiledSize");
      assertNotNull(method, "getCompiledSize method should exist");
      assertEquals(long.class, method.getReturnType(), "getCompiledSize should return long");
    }

    @Test
    @DisplayName("should have getCompilationTime method")
    void shouldHaveGetCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getCompilationTime");
      assertNotNull(method, "getCompilationTime method should exist");
      assertEquals(
          Duration.class, method.getReturnType(), "getCompilationTime should return Duration");
    }

    @Test
    @DisplayName("should have getBasicBlockCount method")
    void shouldHaveGetBasicBlockCountMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getBasicBlockCount");
      assertNotNull(method, "getBasicBlockCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getBasicBlockCount should return int");
    }

    @Test
    @DisplayName("should have getOptimizations method")
    void shouldHaveGetOptimizationsMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getOptimizations");
      assertNotNull(method, "getOptimizations method should exist");
      assertEquals(Map.class, method.getReturnType(), "getOptimizations should return Map");
    }
  }

  @Nested
  @DisplayName("Derived Metric Method Tests")
  class DerivedMetricMethodTests {

    @Test
    @DisplayName("should have getCodeExpansionRatio method")
    void shouldHaveGetCodeExpansionRatioMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getCodeExpansionRatio");
      assertNotNull(method, "getCodeExpansionRatio method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getCodeExpansionRatio should return double");
    }

    @Test
    @DisplayName("should have getCompilationRate method")
    void shouldHaveGetCompilationRateMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getCompilationRate");
      assertNotNull(method, "getCompilationRate method should exist");
      assertEquals(double.class, method.getReturnType(), "getCompilationRate should return double");
    }

    @Test
    @DisplayName("should have getCompilationTimePerBasicBlock method")
    void shouldHaveGetCompilationTimePerBasicBlockMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getCompilationTimePerBasicBlock");
      assertNotNull(method, "getCompilationTimePerBasicBlock method should exist");
      assertEquals(
          Duration.class,
          method.getReturnType(),
          "getCompilationTimePerBasicBlock should return Duration");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have isEfficientCompilation method")
    void shouldHaveIsEfficientCompilationMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("isEfficientCompilation");
      assertNotNull(method, "isEfficientCompilation method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isEfficientCompilation should return boolean");
    }

    @Test
    @DisplayName("should have isPerformanceCritical method")
    void shouldHaveIsPerformanceCriticalMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("isPerformanceCritical");
      assertNotNull(method, "isPerformanceCritical method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isPerformanceCritical should return boolean");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final FunctionStatistics stats =
          new FunctionStatistics(
              "testFunction", 0, 100L, 200L, Duration.ofMillis(50), 10, Map.of("inlining", true));

      assertEquals("testFunction", stats.getName(), "Name should match");
      assertEquals(0, stats.getIndex(), "Index should match");
      assertEquals(100L, stats.getBytecodeSize(), "Bytecode size should match");
      assertEquals(200L, stats.getCompiledSize(), "Compiled size should match");
      assertEquals(
          Duration.ofMillis(50), stats.getCompilationTime(), "Compilation time should match");
      assertEquals(10, stats.getBasicBlockCount(), "Basic block count should match");
      assertNotNull(stats.getOptimizations(), "Optimizations should not be null");
    }

    @Test
    @DisplayName("should calculate code expansion ratio")
    void shouldCalculateCodeExpansionRatio() {
      final FunctionStatistics stats =
          new FunctionStatistics(
              "testFunction", 0, 100L, 200L, Duration.ofMillis(50), 10, Map.of());

      assertEquals(2.0, stats.getCodeExpansionRatio(), 0.001, "Code expansion ratio should be 2.0");
    }
  }
}
