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

package ai.tegmentum.wasmtime4j.panama.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.pool.PoolStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link PanamaPoolStatistics}. */
@DisplayName("PanamaPoolStatistics Tests")
class PanamaPoolStatisticsTest {

  /** Creates a metrics array matching the Wasmtime PoolingAllocatorMetrics field order. */
  private static long[] createMetrics(
      final long coreInstances,
      final long componentInstances,
      final long memories,
      final long tables,
      final long stacks,
      final long gcHeaps,
      final long unusedWarmMemories,
      final long unusedMemoryBytesResident,
      final long unusedWarmTables,
      final long unusedTableBytesResident,
      final long unusedWarmStacks,
      final long unusedStackBytesResident) {
    return new long[] {
      coreInstances,
      componentInstances,
      memories,
      tables,
      stacks,
      gcHeaps,
      unusedWarmMemories,
      unusedMemoryBytesResident,
      unusedWarmTables,
      unusedTableBytesResident,
      unusedWarmStacks,
      unusedStackBytesResident
    };
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaPoolStatistics should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaPoolStatistics.class.getModifiers()),
          "PanamaPoolStatistics should be final");
    }

    @Test
    @DisplayName("PanamaPoolStatistics should implement PoolStatistics")
    void shouldImplementPoolStatistics() {
      assertTrue(
          PoolStatistics.class.isAssignableFrom(PanamaPoolStatistics.class),
          "PanamaPoolStatistics should implement PoolStatistics");
    }
  }

  @Nested
  @DisplayName("Default Constructor Tests")
  class DefaultConstructorTests {

    @Test
    @DisplayName("Default constructor should create statistics with zero values")
    void defaultConstructorShouldCreateStatisticsWithZeroValues() {
      final PanamaPoolStatistics stats = new PanamaPoolStatistics();

      assertEquals(0, stats.getCoreInstances(), "coreInstances should be 0");
      assertEquals(0, stats.getComponentInstances(), "componentInstances should be 0");
      assertEquals(0, stats.getMemories(), "memories should be 0");
      assertEquals(0, stats.getTables(), "tables should be 0");
      assertEquals(0, stats.getStacks(), "stacks should be 0");
      assertEquals(0, stats.getGcHeaps(), "gcHeaps should be 0");
      assertEquals(0, stats.getUnusedWarmMemories(), "unusedWarmMemories should be 0");
      assertEquals(0, stats.getUnusedMemoryBytesResident(), "unusedMemoryBytesResident should be 0");
      assertEquals(0, stats.getUnusedWarmTables(), "unusedWarmTables should be 0");
      assertEquals(0, stats.getUnusedTableBytesResident(), "unusedTableBytesResident should be 0");
      assertEquals(0, stats.getUnusedWarmStacks(), "unusedWarmStacks should be 0");
      assertEquals(0, stats.getUnusedStackBytesResident(), "unusedStackBytesResident should be 0");
      assertEquals(0, stats.getTotalInstances(), "totalInstances should be 0");
    }
  }

  @Nested
  @DisplayName("Array Constructor Tests")
  class ArrayConstructorTests {

    @Test
    @DisplayName("Array constructor should set all values correctly")
    void arrayConstructorShouldSetAllValuesCorrectly() {
      final long[] metrics = createMetrics(10, 5, 20, 15, 8, 3, 4, 4096, 2, 2048, 1, 1024);
      final PanamaPoolStatistics stats = new PanamaPoolStatistics(metrics);

      assertEquals(10, stats.getCoreInstances(), "coreInstances should match");
      assertEquals(5, stats.getComponentInstances(), "componentInstances should match");
      assertEquals(20, stats.getMemories(), "memories should match");
      assertEquals(15, stats.getTables(), "tables should match");
      assertEquals(8, stats.getStacks(), "stacks should match");
      assertEquals(3, stats.getGcHeaps(), "gcHeaps should match");
      assertEquals(4, stats.getUnusedWarmMemories(), "unusedWarmMemories should match");
      assertEquals(4096, stats.getUnusedMemoryBytesResident(),
          "unusedMemoryBytesResident should match");
      assertEquals(2, stats.getUnusedWarmTables(), "unusedWarmTables should match");
      assertEquals(2048, stats.getUnusedTableBytesResident(),
          "unusedTableBytesResident should match");
      assertEquals(1, stats.getUnusedWarmStacks(), "unusedWarmStacks should match");
      assertEquals(1024, stats.getUnusedStackBytesResident(),
          "unusedStackBytesResident should match");
    }

    @Test
    @DisplayName("Array constructor should reject null array")
    void arrayConstructorShouldRejectNullArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolStatistics(null),
          "Should reject null metrics array");
    }

    @Test
    @DisplayName("Array constructor should reject wrong-size array")
    void arrayConstructorShouldRejectWrongSizeArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolStatistics(new long[10]),
          "Should reject array with wrong length");
    }
  }

  @Nested
  @DisplayName("Total Instances Tests")
  class TotalInstancesTests {

    @Test
    @DisplayName("getTotalInstances should sum core and component instances")
    void getTotalInstancesShouldSumCoreAndComponent() {
      final long[] metrics = createMetrics(10, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
      final PanamaPoolStatistics stats = new PanamaPoolStatistics(metrics);

      assertEquals(15, stats.getTotalInstances(), "totalInstances = core + component = 15");
    }
  }

  @Nested
  @DisplayName("Unused Stack Bytes Resident Tests")
  class UnusedStackBytesResidentTests {

    @Test
    @DisplayName("unusedStackBytesResident -1 indicates unavailable")
    void unusedStackBytesResidentNegativeOneIndicatesUnavailable() {
      final long[] metrics = createMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1);
      final PanamaPoolStatistics stats = new PanamaPoolStatistics(metrics);

      assertEquals(-1, stats.getUnusedStackBytesResident(),
          "unusedStackBytesResident should be -1 when unavailable");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all field names")
    void toStringShouldIncludeAllFieldNames() {
      final long[] metrics = createMetrics(10, 5, 20, 15, 8, 3, 4, 4096, 2, 2048, 1, 1024);
      final PanamaPoolStatistics stats = new PanamaPoolStatistics(metrics);

      final String str = stats.toString();

      assertTrue(str.contains("coreInstances=10"), "Should contain coreInstances");
      assertTrue(str.contains("componentInstances=5"), "Should contain componentInstances");
      assertTrue(str.contains("memories=20"), "Should contain memories");
      assertTrue(str.contains("tables=15"), "Should contain tables");
      assertTrue(str.contains("stacks=8"), "Should contain stacks");
      assertTrue(str.contains("gcHeaps=3"), "Should contain gcHeaps");
      assertTrue(str.contains("totalInstances=15"), "Should contain totalInstances");
      assertTrue(str.startsWith("PanamaPoolStatistics{"), "Should start with class name");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle maximum long values")
    void shouldHandleMaximumLongValues() {
      final long[] metrics = new long[12];
      java.util.Arrays.fill(metrics, Long.MAX_VALUE);
      final PanamaPoolStatistics stats = new PanamaPoolStatistics(metrics);

      assertEquals(Long.MAX_VALUE, stats.getCoreInstances(),
          "Should handle max long for coreInstances");
    }

    @Test
    @DisplayName("Should handle zero values in all fields")
    void shouldHandleZeroValuesInAllFields() {
      final PanamaPoolStatistics stats = new PanamaPoolStatistics(new long[12]);

      assertEquals(0, stats.getCoreInstances(), "Should handle zero");
      assertEquals(0, stats.getTotalInstances(), "totalInstances should be 0");
    }
  }
}
