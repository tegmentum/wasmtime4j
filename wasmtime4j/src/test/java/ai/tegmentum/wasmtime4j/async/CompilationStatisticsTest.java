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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for CompilationStatistics class.
 *
 * <p>Verifies detailed statistics about WebAssembly module compilation.
 */
@DisplayName("CompilationStatistics Tests")
class CompilationStatisticsTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create statistics with all values")
    void shouldCreateStatisticsWithAllValues() {
      CompilationStatistics stats =
          new CompilationStatistics(
              100000L, // bytesProcessed
              50, // functionsCompiled
              4, // parallelThreadsUsed
              1024L * 1024, // peakMemoryUsage (1 MB)
              1_000_000_000L, // totalCpuTime (1 second)
              0.75, // cpuUtilization
              3, // optimizationPasses
              "cranelift" // compilerBackend
              );

      assertNotNull(stats, "Stats should not be null");
      assertEquals(100000L, stats.getBytesProcessed(), "Bytes processed should match");
      assertEquals(50, stats.getFunctionsCompiled(), "Functions compiled should match");
      assertEquals(4, stats.getParallelThreadsUsed(), "Parallel threads should match");
      assertEquals(1024L * 1024, stats.getPeakMemoryUsage(), "Peak memory should match");
      assertEquals(1_000_000_000L, stats.getTotalCpuTime(), "Total CPU time should match");
      assertEquals(0.75, stats.getCpuUtilization(), 0.001, "CPU utilization should match");
      assertEquals(3, stats.getOptimizationPasses(), "Optimization passes should match");
      assertEquals("cranelift", stats.getCompilerBackend(), "Compiler backend should match");
    }

    @Test
    @DisplayName("should create statistics with zero values")
    void shouldCreateStatisticsWithZeroValues() {
      CompilationStatistics stats = new CompilationStatistics(0L, 0, 0, 0L, 0L, 0.0, 0, "");

      assertEquals(0L, stats.getBytesProcessed(), "Bytes should be 0");
      assertEquals(0, stats.getFunctionsCompiled(), "Functions should be 0");
      assertEquals(0, stats.getParallelThreadsUsed(), "Threads should be 0");
      assertEquals(0L, stats.getPeakMemoryUsage(), "Peak memory should be 0");
      assertEquals(0L, stats.getTotalCpuTime(), "CPU time should be 0");
      assertEquals(0.0, stats.getCpuUtilization(), 0.001, "CPU utilization should be 0");
      assertEquals(0, stats.getOptimizationPasses(), "Optimization passes should be 0");
      assertEquals("", stats.getCompilerBackend(), "Backend should be empty");
    }
  }

  @Nested
  @DisplayName("Basic Factory Tests")
  class BasicFactoryTests {

    @Test
    @DisplayName("should create basic statistics from bytes processed")
    void shouldCreateBasicStatisticsFromBytesProcessed() {
      CompilationStatistics stats = CompilationStatistics.basic(50000L);

      assertNotNull(stats, "Stats should not be null");
      assertEquals(50000L, stats.getBytesProcessed(), "Bytes processed should match");
      assertEquals(0, stats.getFunctionsCompiled(), "Functions should be 0 (basic)");
      assertEquals(1, stats.getParallelThreadsUsed(), "Threads should be 1 (basic)");
      assertEquals(0L, stats.getPeakMemoryUsage(), "Peak memory should be 0 (basic)");
      assertEquals(0L, stats.getTotalCpuTime(), "CPU time should be 0 (basic)");
      assertEquals(0.0, stats.getCpuUtilization(), 0.001, "CPU utilization should be 0 (basic)");
      assertEquals(0, stats.getOptimizationPasses(), "Optimization passes should be 0 (basic)");
      assertEquals("cranelift", stats.getCompilerBackend(), "Backend should be cranelift (basic)");
    }
  }

  @Nested
  @DisplayName("GetParallelizationEfficiency Tests")
  class GetParallelizationEfficiencyTests {

    @Test
    @DisplayName("should calculate parallelization efficiency")
    void shouldCalculateParallelizationEfficiency() {
      CompilationStatistics stats =
          new CompilationStatistics(
              100000L, 50, 4, 1024L * 1024, 1_000_000_000L, 0.8, 3, "cranelift");

      double efficiency = stats.getParallelizationEfficiency();

      // efficiency = cpuUtilization / parallelThreadsUsed = 0.8 / 4 = 0.2
      assertEquals(0.2, efficiency, 0.001, "Efficiency should be 0.2");
    }

    @Test
    @DisplayName("should return zero efficiency when no threads")
    void shouldReturnZeroEfficiencyWhenNoThreads() {
      CompilationStatistics stats =
          new CompilationStatistics(100000L, 50, 0, 0L, 0L, 0.5, 0, "cranelift");

      assertEquals(0.0, stats.getParallelizationEfficiency(), 0.001, "Efficiency should be 0");
    }

    @Test
    @DisplayName("should calculate efficiency with single thread")
    void shouldCalculateEfficiencyWithSingleThread() {
      CompilationStatistics stats =
          new CompilationStatistics(100000L, 50, 1, 0L, 0L, 0.9, 0, "cranelift");

      // efficiency = 0.9 / 1 = 0.9
      assertEquals(0.9, stats.getParallelizationEfficiency(), 0.001, "Efficiency should be 0.9");
    }
  }

  @Nested
  @DisplayName("GetBytesPerFunction Tests")
  class GetBytesPerFunctionTests {

    @Test
    @DisplayName("should calculate bytes per function")
    void shouldCalculateBytesPerFunction() {
      CompilationStatistics stats =
          new CompilationStatistics(100000L, 50, 4, 0L, 0L, 0.0, 0, "cranelift");

      double bytesPerFunc = stats.getBytesPerFunction();

      // bytesPerFunction = 100000 / 50 = 2000
      assertEquals(2000.0, bytesPerFunc, 0.1, "Bytes per function should be 2000");
    }

    @Test
    @DisplayName("should return zero when no functions")
    void shouldReturnZeroWhenNoFunctions() {
      CompilationStatistics stats =
          new CompilationStatistics(100000L, 0, 4, 0L, 0L, 0.0, 0, "cranelift");

      assertEquals(0.0, stats.getBytesPerFunction(), 0.001, "Bytes per function should be 0");
    }

    @Test
    @DisplayName("should handle large values")
    void shouldHandleLargeValues() {
      CompilationStatistics stats =
          new CompilationStatistics(10_000_000L, 1000, 8, 0L, 0L, 0.0, 0, "cranelift");

      // bytesPerFunction = 10_000_000 / 1000 = 10000
      assertEquals(10000.0, stats.getBytesPerFunction(), 0.1, "Bytes per function should be 10000");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("should return correct bytesProcessed")
    void shouldReturnCorrectBytesProcessed() {
      CompilationStatistics stats = new CompilationStatistics(123456L, 0, 0, 0L, 0L, 0.0, 0, "");

      assertEquals(123456L, stats.getBytesProcessed());
    }

    @Test
    @DisplayName("should return correct functionsCompiled")
    void shouldReturnCorrectFunctionsCompiled() {
      CompilationStatistics stats = new CompilationStatistics(0L, 42, 0, 0L, 0L, 0.0, 0, "");

      assertEquals(42, stats.getFunctionsCompiled());
    }

    @Test
    @DisplayName("should return correct parallelThreadsUsed")
    void shouldReturnCorrectParallelThreadsUsed() {
      CompilationStatistics stats = new CompilationStatistics(0L, 0, 8, 0L, 0L, 0.0, 0, "");

      assertEquals(8, stats.getParallelThreadsUsed());
    }

    @Test
    @DisplayName("should return correct peakMemoryUsage")
    void shouldReturnCorrectPeakMemoryUsage() {
      CompilationStatistics stats =
          new CompilationStatistics(0L, 0, 0, 256L * 1024 * 1024, 0L, 0.0, 0, "");

      assertEquals(256L * 1024 * 1024, stats.getPeakMemoryUsage());
    }

    @Test
    @DisplayName("should return correct totalCpuTime")
    void shouldReturnCorrectTotalCpuTime() {
      CompilationStatistics stats =
          new CompilationStatistics(0L, 0, 0, 0L, 5_000_000_000L, 0.0, 0, "");

      assertEquals(5_000_000_000L, stats.getTotalCpuTime());
    }

    @Test
    @DisplayName("should return correct cpuUtilization")
    void shouldReturnCorrectCpuUtilization() {
      CompilationStatistics stats = new CompilationStatistics(0L, 0, 0, 0L, 0L, 0.85, 0, "");

      assertEquals(0.85, stats.getCpuUtilization(), 0.001);
    }

    @Test
    @DisplayName("should return correct optimizationPasses")
    void shouldReturnCorrectOptimizationPasses() {
      CompilationStatistics stats = new CompilationStatistics(0L, 0, 0, 0L, 0L, 0.0, 5, "");

      assertEquals(5, stats.getOptimizationPasses());
    }

    @Test
    @DisplayName("should return correct compilerBackend")
    void shouldReturnCorrectCompilerBackend() {
      CompilationStatistics stats = new CompilationStatistics(0L, 0, 0, 0L, 0L, 0.0, 0, "winch");

      assertEquals("winch", stats.getCompilerBackend());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      CompilationStatistics stats =
          new CompilationStatistics(
              100000L, 50, 4, 1024L * 1024, 1_000_000_000L, 0.75, 3, "cranelift");

      assertNotNull(stats.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      CompilationStatistics stats = CompilationStatistics.basic(1000L);

      assertTrue(
          stats.toString().contains("CompilationStatistics"), "toString should contain class name");
    }

    @Test
    @DisplayName("should include key fields in toString")
    void shouldIncludeKeyFieldsInToString() {
      CompilationStatistics stats =
          new CompilationStatistics(
              100000L, 50, 4, 1024L * 1024, 1_000_000_000L, 0.75, 3, "cranelift");

      String str = stats.toString();
      assertTrue(str.contains("bytes="), "toString should contain bytes");
      assertTrue(str.contains("functions="), "toString should contain functions");
      assertTrue(str.contains("threads="), "toString should contain threads");
      assertTrue(str.contains("backend="), "toString should contain backend");
      assertTrue(str.contains("efficiency="), "toString should contain efficiency");
    }

    @Test
    @DisplayName("should include backend in toString")
    void shouldIncludeBackendInToString() {
      CompilationStatistics stats =
          new CompilationStatistics(0L, 0, 0, 0L, 0L, 0.0, 0, "cranelift");

      assertTrue(stats.toString().contains("cranelift"), "toString should contain cranelift");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent typical compilation statistics")
    void shouldRepresentTypicalCompilationStatistics() {
      // Simulate compiling a medium-sized WASM module
      CompilationStatistics stats =
          new CompilationStatistics(
              500_000L, // 500 KB of WASM
              150, // 150 functions
              4, // 4 parallel threads
              128L * 1024 * 1024, // 128 MB peak memory
              2_500_000_000L, // 2.5 seconds CPU time
              0.62, // 62% CPU utilization
              2, // 2 optimization passes
              "cranelift" // Cranelift backend
              );

      assertEquals(500_000L, stats.getBytesProcessed());
      assertEquals(150, stats.getFunctionsCompiled());
      assertEquals(4, stats.getParallelThreadsUsed());
      assertEquals("cranelift", stats.getCompilerBackend());

      // Verify calculated metrics
      double bytesPerFunc = stats.getBytesPerFunction();
      assertTrue(bytesPerFunc > 0, "Bytes per function should be positive");
      assertEquals(500_000.0 / 150, bytesPerFunc, 0.1, "Bytes per function calculation");

      double efficiency = stats.getParallelizationEfficiency();
      assertEquals(0.62 / 4, efficiency, 0.001, "Efficiency calculation");
    }

    @Test
    @DisplayName("should handle small module statistics")
    void shouldHandleSmallModuleStatistics() {
      // Small module with single-threaded compilation
      CompilationStatistics stats =
          new CompilationStatistics(1024L, 5, 1, 1024L * 1024, 50_000_000L, 0.95, 1, "cranelift");

      assertEquals(1024L / 5.0, stats.getBytesPerFunction(), 0.1);
      assertEquals(0.95, stats.getParallelizationEfficiency(), 0.001);
    }

    @Test
    @DisplayName("should handle large module statistics")
    void shouldHandleLargeModuleStatistics() {
      // Large module with high parallelism
      CompilationStatistics stats =
          new CompilationStatistics(
              50_000_000L, // 50 MB
              5000, // 5000 functions
              16, // 16 threads
              2L * 1024 * 1024 * 1024, // 2 GB peak memory
              60_000_000_000L, // 60 seconds CPU time
              0.85, // 85% CPU utilization
              3, // 3 optimization passes
              "cranelift");

      assertEquals(50_000_000L / 5000.0, stats.getBytesPerFunction(), 0.1);
      assertEquals(0.85 / 16, stats.getParallelizationEfficiency(), 0.001);
    }
  }
}
