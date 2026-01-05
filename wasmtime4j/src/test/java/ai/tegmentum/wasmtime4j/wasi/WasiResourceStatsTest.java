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
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiResourceStats} interface.
 *
 * <p>WasiResourceStats provides statistics about WASI resource usage.
 */
@DisplayName("WasiResourceStats Tests")
class WasiResourceStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiResourceStats.class.getModifiers()),
          "WasiResourceStats should be public");
      assertTrue(WasiResourceStats.class.isInterface(), "WasiResourceStats should be an interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getAccessCount method")
    void shouldHaveGetAccessCountMethod() throws NoSuchMethodException {
      final Method method = WasiResourceStats.class.getMethod("getAccessCount");
      assertNotNull(method, "getAccessCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalUsageTime method")
    void shouldHaveGetTotalUsageTimeMethod() throws NoSuchMethodException {
      final Method method = WasiResourceStats.class.getMethod("getTotalUsageTime");
      assertNotNull(method, "getTotalUsageTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getOperationCount method")
    void shouldHaveGetOperationCountMethod() throws NoSuchMethodException {
      final Method method = WasiResourceStats.class.getMethod("getOperationCount");
      assertNotNull(method, "getOperationCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getErrorCount method")
    void shouldHaveGetErrorCountMethod() throws NoSuchMethodException {
      final Method method = WasiResourceStats.class.getMethod("getErrorCount");
      assertNotNull(method, "getErrorCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Implementation Behavior Tests")
  class ImplementationBehaviorTests {

    @Test
    @DisplayName("implementation should track access count")
    void implementationShouldTrackAccessCount() {
      final WasiResourceStats stats = createTestStats(100L, Duration.ofSeconds(10), 50L, 5L);

      assertEquals(100L, stats.getAccessCount(), "Access count should match");
    }

    @Test
    @DisplayName("implementation should track total usage time")
    void implementationShouldTrackTotalUsageTime() {
      final Duration usageTime = Duration.ofMinutes(5);
      final WasiResourceStats stats = createTestStats(100L, usageTime, 50L, 5L);

      assertEquals(usageTime, stats.getTotalUsageTime(), "Usage time should match");
    }

    @Test
    @DisplayName("implementation should track operation count")
    void implementationShouldTrackOperationCount() {
      final WasiResourceStats stats = createTestStats(100L, Duration.ofSeconds(10), 250L, 5L);

      assertEquals(250L, stats.getOperationCount(), "Operation count should match");
    }

    @Test
    @DisplayName("implementation should track error count")
    void implementationShouldTrackErrorCount() {
      final WasiResourceStats stats = createTestStats(100L, Duration.ofSeconds(10), 50L, 15L);

      assertEquals(15L, stats.getErrorCount(), "Error count should match");
    }

    private WasiResourceStats createTestStats(
        final long accessCount,
        final Duration totalUsageTime,
        final long operationCount,
        final long errorCount) {
      return new WasiResourceStats() {
        @Override
        public long getAccessCount() {
          return accessCount;
        }

        @Override
        public Duration getTotalUsageTime() {
          return totalUsageTime;
        }

        @Override
        public long getOperationCount() {
          return operationCount;
        }

        @Override
        public long getErrorCount() {
          return errorCount;
        }
      };
    }
  }
}
