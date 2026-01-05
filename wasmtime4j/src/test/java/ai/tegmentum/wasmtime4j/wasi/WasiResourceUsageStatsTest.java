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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiResourceUsageStats} interface.
 *
 * <p>WasiResourceUsageStats provides resource usage statistics for WASI components.
 */
@DisplayName("WasiResourceUsageStats Tests")
class WasiResourceUsageStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiResourceUsageStats.class.getModifiers()),
          "WasiResourceUsageStats should be public");
      assertTrue(
          WasiResourceUsageStats.class.isInterface(),
          "WasiResourceUsageStats should be an interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getTotalResourcesCreated method")
    void shouldHaveGetTotalResourcesCreatedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageStats.class.getMethod("getTotalResourcesCreated");
      assertNotNull(method, "getTotalResourcesCreated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCurrentResourceCount method")
    void shouldHaveGetCurrentResourceCountMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageStats.class.getMethod("getCurrentResourceCount");
      assertNotNull(method, "getCurrentResourceCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPeakResourceCount method")
    void shouldHaveGetPeakResourceCountMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageStats.class.getMethod("getPeakResourceCount");
      assertNotNull(method, "getPeakResourceCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getResourceCountsByType method")
    void shouldHaveGetResourceCountsByTypeMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageStats.class.getMethod("getResourceCountsByType");
      assertNotNull(method, "getResourceCountsByType method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getResourceCreationsByType method")
    void shouldHaveGetResourceCreationsByTypeMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageStats.class.getMethod("getResourceCreationsByType");
      assertNotNull(method, "getResourceCreationsByType method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Implementation Behavior Tests")
  class ImplementationBehaviorTests {

    @Test
    @DisplayName("implementation should track resource counts")
    void implementationShouldTrackResourceCounts() {
      final Map<String, Integer> countsByType = new HashMap<>();
      final Map<String, Long> creationsByType = new HashMap<>();
      final WasiResourceUsageStats stats =
          createTestStats(1000L, 50, 75, countsByType, creationsByType);

      assertEquals(1000L, stats.getTotalResourcesCreated(), "Total created should match");
      assertEquals(50, stats.getCurrentResourceCount(), "Current count should match");
      assertEquals(75, stats.getPeakResourceCount(), "Peak count should match");
    }

    @Test
    @DisplayName("implementation should track counts by type")
    void implementationShouldTrackCountsByType() {
      final Map<String, Integer> countsByType = new HashMap<>();
      countsByType.put("FileHandle", 10);
      countsByType.put("Socket", 5);
      countsByType.put("Stream", 15);

      final Map<String, Long> creationsByType = new HashMap<>();
      final WasiResourceUsageStats stats =
          createTestStats(1000L, 30, 50, countsByType, creationsByType);

      assertEquals(3, stats.getResourceCountsByType().size(), "Should have 3 types");
      assertEquals(10, stats.getResourceCountsByType().get("FileHandle"));
      assertEquals(5, stats.getResourceCountsByType().get("Socket"));
      assertEquals(15, stats.getResourceCountsByType().get("Stream"));
    }

    @Test
    @DisplayName("implementation should track creations by type")
    void implementationShouldTrackCreationsByType() {
      final Map<String, Integer> countsByType = new HashMap<>();
      final Map<String, Long> creationsByType = new HashMap<>();
      creationsByType.put("FileHandle", 500L);
      creationsByType.put("Socket", 200L);
      creationsByType.put("Stream", 300L);

      final WasiResourceUsageStats stats =
          createTestStats(1000L, 30, 50, countsByType, creationsByType);

      assertEquals(3, stats.getResourceCreationsByType().size(), "Should have 3 types");
      assertEquals(500L, stats.getResourceCreationsByType().get("FileHandle"));
      assertEquals(200L, stats.getResourceCreationsByType().get("Socket"));
      assertEquals(300L, stats.getResourceCreationsByType().get("Stream"));
    }

    @Test
    @DisplayName("peak count should be at least current count")
    void peakCountShouldBeAtLeastCurrentCount() {
      final Map<String, Integer> countsByType = new HashMap<>();
      final Map<String, Long> creationsByType = new HashMap<>();
      final WasiResourceUsageStats stats =
          createTestStats(1000L, 50, 75, countsByType, creationsByType);

      assertTrue(
          stats.getPeakResourceCount() >= stats.getCurrentResourceCount(),
          "Peak should be >= current");
    }

    private WasiResourceUsageStats createTestStats(
        final long totalResourcesCreated,
        final int currentResourceCount,
        final int peakResourceCount,
        final Map<String, Integer> resourceCountsByType,
        final Map<String, Long> resourceCreationsByType) {
      return new WasiResourceUsageStats() {
        @Override
        public long getTotalResourcesCreated() {
          return totalResourcesCreated;
        }

        @Override
        public int getCurrentResourceCount() {
          return currentResourceCount;
        }

        @Override
        public int getPeakResourceCount() {
          return peakResourceCount;
        }

        @Override
        public Map<String, Integer> getResourceCountsByType() {
          return new HashMap<>(resourceCountsByType);
        }

        @Override
        public Map<String, Long> getResourceCreationsByType() {
          return new HashMap<>(resourceCreationsByType);
        }
      };
    }
  }
}
