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
package ai.tegmentum.wasmtime4j.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test suite for the default methods in PoolStatistics interface.
 *
 * <p>Tests the getTotalInstances default method implementation.
 */
@DisplayName("PoolStatistics Default Methods Tests")
class PoolStatisticsDefaultMethodsTest {

  /** Test implementation of PoolStatistics for testing default methods. */
  private static class TestPoolStatistics implements PoolStatistics {
    private final long coreInstances;
    private final long componentInstances;

    TestPoolStatistics(final long coreInstances, final long componentInstances) {
      this.coreInstances = coreInstances;
      this.componentInstances = componentInstances;
    }

    @Override
    public long getCoreInstances() {
      return coreInstances;
    }

    @Override
    public long getComponentInstances() {
      return componentInstances;
    }

    @Override
    public long getMemories() {
      return 0;
    }

    @Override
    public long getTables() {
      return 0;
    }

    @Override
    public long getStacks() {
      return 0;
    }

    @Override
    public long getGcHeaps() {
      return 0;
    }

    @Override
    public long getUnusedWarmMemories() {
      return 0;
    }

    @Override
    public long getUnusedMemoryBytesResident() {
      return 0;
    }

    @Override
    public long getUnusedWarmTables() {
      return 0;
    }

    @Override
    public long getUnusedTableBytesResident() {
      return 0;
    }

    @Override
    public long getUnusedWarmStacks() {
      return 0;
    }

    @Override
    public long getUnusedStackBytesResident() {
      return 0;
    }
  }

  @Test
  @DisplayName("should return 0 when no instances allocated")
  void shouldReturnZeroWhenNoInstances() {
    final PoolStatistics stats = new TestPoolStatistics(0, 0);
    assertEquals(0, stats.getTotalInstances(), "Total instances should be 0");
  }

  @Test
  @DisplayName("should return sum of core and component instances")
  void shouldReturnSumOfInstances() {
    final PoolStatistics stats = new TestPoolStatistics(5, 3);
    assertEquals(
        8, stats.getTotalInstances(), "Total instances should be 8 (5 core + 3 component)");
  }

  @Test
  @DisplayName("should handle only core instances")
  void shouldHandleOnlyCoreInstances() {
    final PoolStatistics stats = new TestPoolStatistics(10, 0);
    assertEquals(10, stats.getTotalInstances(), "Total instances should be 10 (core only)");
  }

  @Test
  @DisplayName("should handle only component instances")
  void shouldHandleOnlyComponentInstances() {
    final PoolStatistics stats = new TestPoolStatistics(0, 7);
    assertEquals(7, stats.getTotalInstances(), "Total instances should be 7 (component only)");
  }

  @Test
  @DisplayName("should handle large numbers")
  void shouldHandleLargeNumbers() {
    final PoolStatistics stats = new TestPoolStatistics(1000000L, 9000000L);
    assertEquals(10000000L, stats.getTotalInstances(), "Total instances should be 10000000");
  }
}
