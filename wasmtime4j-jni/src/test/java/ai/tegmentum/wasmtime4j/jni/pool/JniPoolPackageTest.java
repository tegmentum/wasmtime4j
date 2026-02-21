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

package ai.tegmentum.wasmtime4j.jni.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JNI pool package.
 *
 * <p>This test covers behavioral aspects of classes in the ai.tegmentum.wasmtime4j.jni.pool package
 * including JniPoolStatistics and JniPoolingAllocatorConfigBuilder.
 */
@DisplayName("JNI Pool Package Tests")
class JniPoolPackageTest {

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

  // ========================================================================
  // JniPoolStatistics Tests
  // ========================================================================

  @Nested
  @DisplayName("JniPoolStatistics Tests")
  class JniPoolStatisticsTests {

    @Test
    @DisplayName("JniPoolStatistics default constructor should create empty statistics")
    void jniPoolStatisticsDefaultConstructorShouldCreateEmptyStats() {
      final JniPoolStatistics stats = new JniPoolStatistics();

      assertNotNull(stats, "JniPoolStatistics should be created");
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

    @Test
    @DisplayName("JniPoolStatistics array constructor should set all values")
    void jniPoolStatisticsArrayConstructorShouldSetAllValues() {
      final long[] metrics = createMetrics(10, 5, 20, 15, 8, 3, 4, 4096, 2, 2048, 1, 1024);
      final JniPoolStatistics stats = new JniPoolStatistics(metrics);

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
    @DisplayName("JniPoolStatistics getTotalInstances should sum core and component")
    void jniPoolStatisticsGetTotalInstancesShouldSumCoreAndComponent() {
      final long[] metrics = createMetrics(100, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
      final JniPoolStatistics stats = new JniPoolStatistics(metrics);

      assertEquals(150, stats.getTotalInstances(),
          "totalInstances should be core + component = 150");
    }

    @Test
    @DisplayName("JniPoolStatistics unusedStackBytesResident -1 indicates unavailable")
    void jniPoolStatisticsUnusedStackBytesResidentNegativeOne() {
      final long[] metrics = createMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1);
      final JniPoolStatistics stats = new JniPoolStatistics(metrics);

      assertEquals(-1, stats.getUnusedStackBytesResident(),
          "unusedStackBytesResident should be -1 when unavailable");
    }

    @Test
    @DisplayName("JniPoolStatistics toString should contain relevant info")
    void jniPoolStatisticsToStringShouldContainRelevantInfo() {
      final long[] metrics = createMetrics(10, 5, 20, 15, 8, 3, 4, 4096, 2, 2048, 1, 1024);
      final JniPoolStatistics stats = new JniPoolStatistics(metrics);

      final String str = stats.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("JniPoolStatistics"), "Should contain class name");
      assertTrue(str.contains("coreInstances=10"), "Should contain coreInstances");
      assertTrue(str.contains("componentInstances=5"), "Should contain componentInstances");
      assertTrue(str.contains("totalInstances=15"), "Should contain totalInstances");
    }
  }

  // ========================================================================
  // JniPoolingAllocatorConfigBuilder Tests
  // ========================================================================

  @Nested
  @DisplayName("JniPoolingAllocatorConfigBuilder Tests")
  class JniPoolingAllocatorConfigBuilderTests {

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder build should create config")
    void jniPoolingAllocatorConfigBuilderBuildShouldCreateConfig() {
      JniPoolingAllocatorConfigBuilder builder = new JniPoolingAllocatorConfigBuilder();
      PoolingAllocatorConfig config = builder.build();

      assertNotNull(config, "Build should create config");
      assertTrue(
          config instanceof JniPoolingAllocatorConfig,
          "Build should create JniPoolingAllocatorConfig");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder should support method chaining")
    void jniPoolingAllocatorConfigBuilderShouldSupportMethodChaining() {
      JniPoolingAllocatorConfigBuilder builder = new JniPoolingAllocatorConfigBuilder();

      PoolingAllocatorConfig config =
          builder.instancePoolSize(1000).maxMemorySize(1024L * 1024 * 100).build();

      assertNotNull(config, "Chained build should create config");
      assertEquals(1000, config.getInstancePoolSize(), "Instance pool size should be set");
      assertEquals(1024L * 1024 * 100, config.getMaxMemorySize(), "Max memory size should be set");
    }
  }
}
