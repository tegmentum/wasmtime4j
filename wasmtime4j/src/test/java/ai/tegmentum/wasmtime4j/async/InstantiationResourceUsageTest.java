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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for InstantiationResourceUsage class.
 *
 * <p>Verifies resource usage information during WebAssembly instance instantiation.
 */
@DisplayName("InstantiationResourceUsage Tests")
class InstantiationResourceUsageTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create resource usage with all values")
    void shouldCreateResourceUsageWithAllValues() {
      Instant now = Instant.now();

      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(
              1024L * 1024, // memoryUsed (1 MB)
              2L * 1024 * 1024, // peakMemoryUsed (2 MB)
              0.75, // cpuUtilization
              512L * 1024, // nativeMemoryUsed (512 KB)
              100, // functionsLinked
              5, // tablesAllocated
              10, // globalsInitialized
              1024L * 1024 * 10, // compilationCacheSize (10 MB)
              now // measurementTime
              );

      assertNotNull(usage, "Usage should not be null");
      assertEquals(1024L * 1024, usage.getMemoryUsed(), "Memory used should match");
      assertEquals(2L * 1024 * 1024, usage.getPeakMemoryUsed(), "Peak memory should match");
      assertEquals(0.75, usage.getCpuUtilization(), 0.001, "CPU utilization should match");
      assertEquals(512L * 1024, usage.getNativeMemoryUsed(), "Native memory should match");
      assertEquals(100, usage.getFunctionsLinked(), "Functions linked should match");
      assertEquals(5, usage.getTablesAllocated(), "Tables allocated should match");
      assertEquals(10, usage.getGlobalsInitialized(), "Globals initialized should match");
      assertEquals(1024L * 1024 * 10, usage.getCompilationCacheSize(), "Cache size should match");
      assertEquals(now, usage.getMeasurementTime(), "Measurement time should match");
    }

    @Test
    @DisplayName("should clamp CPU utilization between 0 and 1")
    void shouldClampCpuUtilizationBetweenZeroAndOne() {
      Instant now = Instant.now();

      // Test negative CPU utilization clamped to 0
      InstantiationResourceUsage usageNeg =
          new InstantiationResourceUsage(0L, 0L, -0.5, 0L, 0, 0, 0, 0L, now);
      assertEquals(0.0, usageNeg.getCpuUtilization(), 0.001, "Negative should clamp to 0");

      // Test above 1 CPU utilization clamped to 1
      InstantiationResourceUsage usageHigh =
          new InstantiationResourceUsage(0L, 0L, 1.5, 0L, 0, 0, 0, 0L, now);
      assertEquals(1.0, usageHigh.getCpuUtilization(), 0.001, "Above 1 should clamp to 1");
    }

    @Test
    @DisplayName("should ensure peak memory is at least current memory")
    void shouldEnsurePeakMemoryIsAtLeastCurrentMemory() {
      Instant now = Instant.now();

      // Peak memory less than current memory should be adjusted
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(1024L, 512L, 0.5, 0L, 0, 0, 0, 0L, now);

      // Peak should be max(peakMemoryUsed, memoryUsed) = max(512, 1024) = 1024
      assertEquals(1024L, usage.getPeakMemoryUsed(), "Peak should be at least current");
    }
  }

  @Nested
  @DisplayName("Empty Factory Tests")
  class EmptyFactoryTests {

    @Test
    @DisplayName("should create empty resource usage")
    void shouldCreateEmptyResourceUsage() {
      InstantiationResourceUsage usage = InstantiationResourceUsage.empty();

      assertNotNull(usage, "Usage should not be null");
      assertEquals(0L, usage.getMemoryUsed(), "Memory should be 0");
      assertEquals(0L, usage.getPeakMemoryUsed(), "Peak memory should be 0");
      assertEquals(0.0, usage.getCpuUtilization(), 0.001, "CPU utilization should be 0");
      assertEquals(0L, usage.getNativeMemoryUsed(), "Native memory should be 0");
      assertEquals(0, usage.getFunctionsLinked(), "Functions should be 0");
      assertEquals(0, usage.getTablesAllocated(), "Tables should be 0");
      assertEquals(0, usage.getGlobalsInitialized(), "Globals should be 0");
      assertEquals(0L, usage.getCompilationCacheSize(), "Cache size should be 0");
      assertNotNull(usage.getMeasurementTime(), "Measurement time should not be null");
    }
  }

  @Nested
  @DisplayName("WithMemory Factory Tests")
  class WithMemoryFactoryTests {

    @Test
    @DisplayName("should create resource usage with memory only")
    void shouldCreateResourceUsageWithMemoryOnly() {
      long memoryUsed = 2L * 1024 * 1024;

      InstantiationResourceUsage usage = InstantiationResourceUsage.withMemory(memoryUsed);

      assertNotNull(usage, "Usage should not be null");
      assertEquals(memoryUsed, usage.getMemoryUsed(), "Memory should match");
      assertEquals(memoryUsed, usage.getPeakMemoryUsed(), "Peak memory should match current");
      assertEquals(0.0, usage.getCpuUtilization(), 0.001, "CPU utilization should be 0");
      assertEquals(0L, usage.getNativeMemoryUsed(), "Native memory should be 0");
      assertEquals(0, usage.getFunctionsLinked(), "Functions should be 0");
      assertEquals(0, usage.getTablesAllocated(), "Tables should be 0");
      assertEquals(0, usage.getGlobalsInitialized(), "Globals should be 0");
      assertEquals(0L, usage.getCompilationCacheSize(), "Cache size should be 0");
    }
  }

  @Nested
  @DisplayName("GetTotalMemoryUsed Tests")
  class GetTotalMemoryUsedTests {

    @Test
    @DisplayName("should calculate total memory used")
    void shouldCalculateTotalMemoryUsed() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(
              1024L * 1024, // 1 MB managed
              0L,
              0.0,
              512L * 1024, // 512 KB native
              0,
              0,
              0,
              0L,
              Instant.now());

      long total = usage.getTotalMemoryUsed();

      // total = memoryUsed + nativeMemoryUsed = 1MB + 512KB
      assertEquals(1024L * 1024 + 512L * 1024, total, "Total memory should be sum");
    }
  }

  @Nested
  @DisplayName("GetTotalPeakMemoryUsed Tests")
  class GetTotalPeakMemoryUsedTests {

    @Test
    @DisplayName("should calculate total peak memory used")
    void shouldCalculateTotalPeakMemoryUsed() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(
              512L * 1024, // 512 KB current
              2L * 1024 * 1024, // 2 MB peak
              0.0,
              256L * 1024, // 256 KB native
              0,
              0,
              0,
              0L,
              Instant.now());

      long totalPeak = usage.getTotalPeakMemoryUsed();

      // totalPeak = peakMemoryUsed + nativeMemoryUsed = 2MB + 256KB
      assertEquals(2L * 1024 * 1024 + 256L * 1024, totalPeak, "Total peak should be sum");
    }
  }

  @Nested
  @DisplayName("GetMemoryEfficiency Tests")
  class GetMemoryEfficiencyTests {

    @Test
    @DisplayName("should calculate memory efficiency")
    void shouldCalculateMemoryEfficiency() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(
              512L * 1024, // 512 KB current
              1024L * 1024, // 1 MB peak
              0.0,
              0L,
              0,
              0,
              0,
              0L,
              Instant.now());

      double efficiency = usage.getMemoryEfficiency();

      // efficiency = current / peak = 512KB / 1MB = 0.5
      assertEquals(0.5, efficiency, 0.001, "Efficiency should be 0.5");
    }

    @Test
    @DisplayName("should return 1.0 when no peak memory")
    void shouldReturnOneWhenNoPeakMemory() {
      InstantiationResourceUsage usage = InstantiationResourceUsage.empty();

      assertEquals(
          1.0, usage.getMemoryEfficiency(), 0.001, "Efficiency should be 1.0 when no peak");
    }
  }

  @Nested
  @DisplayName("GetTotalResourcesAllocated Tests")
  class GetTotalResourcesAllocatedTests {

    @Test
    @DisplayName("should calculate total resources allocated")
    void shouldCalculateTotalResourcesAllocated() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(
              0L,
              0L,
              0.0,
              0L,
              50, // functionsLinked
              3, // tablesAllocated
              7, // globalsInitialized
              0L,
              Instant.now());

      int total = usage.getTotalResourcesAllocated();

      assertEquals(60, total, "Total resources should be sum");
    }
  }

  @Nested
  @DisplayName("HasResourcesAllocated Tests")
  class HasResourcesAllocatedTests {

    @Test
    @DisplayName("should return true when resources allocated")
    void shouldReturnTrueWhenResourcesAllocated() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(0L, 0L, 0.0, 0L, 1, 0, 0, 0L, Instant.now());

      assertTrue(usage.hasResourcesAllocated(), "Should have resources allocated");
    }

    @Test
    @DisplayName("should return false when no resources allocated")
    void shouldReturnFalseWhenNoResourcesAllocated() {
      InstantiationResourceUsage usage = InstantiationResourceUsage.empty();

      assertFalse(usage.hasResourcesAllocated(), "Should NOT have resources allocated");
    }
  }

  @Nested
  @DisplayName("HasCompilationCache Tests")
  class HasCompilationCacheTests {

    @Test
    @DisplayName("should return true when cache present")
    void shouldReturnTrueWhenCachePresent() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(0L, 0L, 0.0, 0L, 0, 0, 0, 1024L, Instant.now());

      assertTrue(usage.hasCompilationCache(), "Should have compilation cache");
    }

    @Test
    @DisplayName("should return false when no cache")
    void shouldReturnFalseWhenNoCache() {
      InstantiationResourceUsage usage = InstantiationResourceUsage.empty();

      assertFalse(usage.hasCompilationCache(), "Should NOT have compilation cache");
    }
  }

  @Nested
  @DisplayName("WithMemoryUsed Tests")
  class WithMemoryUsedTests {

    @Test
    @DisplayName("should create copy with updated memory")
    void shouldCreateCopyWithUpdatedMemory() {
      InstantiationResourceUsage original =
          new InstantiationResourceUsage(1024L, 2048L, 0.5, 512L, 10, 2, 5, 4096L, Instant.now());

      InstantiationResourceUsage updated = original.withMemoryUsed(3000L);

      assertEquals(3000L, updated.getMemoryUsed(), "Memory should be updated");
      assertEquals(3000L, updated.getPeakMemoryUsed(), "Peak should be max of original and new");
      // Other fields preserved
      assertEquals(0.5, updated.getCpuUtilization(), 0.001);
      assertEquals(512L, updated.getNativeMemoryUsed());
      assertEquals(10, updated.getFunctionsLinked());
    }

    @Test
    @DisplayName("should update peak if new memory exceeds")
    void shouldUpdatePeakIfNewMemoryExceeds() {
      InstantiationResourceUsage original =
          new InstantiationResourceUsage(1024L, 2048L, 0.5, 0L, 0, 0, 0, 0L, Instant.now());

      InstantiationResourceUsage updated = original.withMemoryUsed(5000L);

      assertEquals(5000L, updated.getMemoryUsed());
      assertEquals(5000L, updated.getPeakMemoryUsed(), "Peak should be updated to 5000");
    }
  }

  @Nested
  @DisplayName("WithCpuUtilization Tests")
  class WithCpuUtilizationTests {

    @Test
    @DisplayName("should create copy with updated CPU utilization")
    void shouldCreateCopyWithUpdatedCpuUtilization() {
      InstantiationResourceUsage original =
          new InstantiationResourceUsage(1024L, 2048L, 0.5, 512L, 10, 2, 5, 4096L, Instant.now());

      InstantiationResourceUsage updated = original.withCpuUtilization(0.85);

      assertEquals(0.85, updated.getCpuUtilization(), 0.001, "CPU should be updated");
      // Other fields preserved
      assertEquals(1024L, updated.getMemoryUsed());
      assertEquals(512L, updated.getNativeMemoryUsed());
      assertEquals(10, updated.getFunctionsLinked());
    }
  }

  @Nested
  @DisplayName("WithResourceCounts Tests")
  class WithResourceCountsTests {

    @Test
    @DisplayName("should create copy with updated resource counts")
    void shouldCreateCopyWithUpdatedResourceCounts() {
      InstantiationResourceUsage original =
          new InstantiationResourceUsage(1024L, 2048L, 0.5, 512L, 10, 2, 5, 4096L, Instant.now());

      InstantiationResourceUsage updated = original.withResourceCounts(50, 10, 20);

      assertEquals(50, updated.getFunctionsLinked(), "Functions should be updated");
      assertEquals(10, updated.getTablesAllocated(), "Tables should be updated");
      assertEquals(20, updated.getGlobalsInitialized(), "Globals should be updated");
      // Other fields preserved
      assertEquals(1024L, updated.getMemoryUsed());
      assertEquals(0.5, updated.getCpuUtilization(), 0.001);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      InstantiationResourceUsage usage = InstantiationResourceUsage.empty();

      assertNotNull(usage.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      InstantiationResourceUsage usage = InstantiationResourceUsage.empty();

      assertTrue(
          usage.toString().contains("InstantiationResourceUsage"),
          "toString should contain class name");
    }

    @Test
    @DisplayName("should include key fields in toString")
    void shouldIncludeKeyFieldsInToString() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(
              1024L * 1024, 2L * 1024 * 1024, 0.5, 512L * 1024, 100, 5, 10, 1024L, Instant.now());

      String str = usage.toString();
      assertTrue(str.contains("memory="), "toString should contain memory");
      assertTrue(str.contains("cpu="), "toString should contain cpu");
      assertTrue(str.contains("resources="), "toString should contain resources");
      assertTrue(str.contains("cache="), "toString should contain cache");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent typical instantiation resource usage")
    void shouldRepresentTypicalInstantiationResourceUsage() {
      InstantiationResourceUsage usage =
          new InstantiationResourceUsage(
              32L * 1024 * 1024, // 32 MB managed
              64L * 1024 * 1024, // 64 MB peak
              0.65, // 65% CPU
              8L * 1024 * 1024, // 8 MB native
              250, // 250 functions
              10, // 10 tables
              50, // 50 globals
              16L * 1024 * 1024, // 16 MB cache
              Instant.now());

      assertEquals(40L * 1024 * 1024, usage.getTotalMemoryUsed(), "Total memory should be 40 MB");
      assertEquals(72L * 1024 * 1024, usage.getTotalPeakMemoryUsed(), "Total peak should be 72 MB");
      assertEquals(0.5, usage.getMemoryEfficiency(), 0.001, "Efficiency should be 0.5");
      assertEquals(310, usage.getTotalResourcesAllocated(), "Total resources should be 310");
      assertTrue(usage.hasResourcesAllocated());
      assertTrue(usage.hasCompilationCache());
    }

    @Test
    @DisplayName("should handle zero resource instantiation")
    void shouldHandleZeroResourceInstantiation() {
      InstantiationResourceUsage usage = InstantiationResourceUsage.empty();

      assertEquals(0L, usage.getTotalMemoryUsed());
      assertEquals(0L, usage.getTotalPeakMemoryUsed());
      assertEquals(1.0, usage.getMemoryEfficiency(), 0.001);
      assertEquals(0, usage.getTotalResourcesAllocated());
      assertFalse(usage.hasResourcesAllocated());
      assertFalse(usage.hasCompilationCache());
    }

    @Test
    @DisplayName("should track resource usage evolution")
    void shouldTrackResourceUsageEvolution() {
      // Initial state
      InstantiationResourceUsage initial = InstantiationResourceUsage.empty();

      // After memory allocation
      InstantiationResourceUsage afterMemory = initial.withMemoryUsed(1024L * 1024);
      assertEquals(1024L * 1024, afterMemory.getMemoryUsed());

      // After CPU increases
      InstantiationResourceUsage afterCpu = afterMemory.withCpuUtilization(0.8);
      assertEquals(0.8, afterCpu.getCpuUtilization(), 0.001);

      // After resources linked
      InstantiationResourceUsage afterResources = afterCpu.withResourceCounts(100, 5, 20);
      assertEquals(125, afterResources.getTotalResourcesAllocated());
    }
  }
}
