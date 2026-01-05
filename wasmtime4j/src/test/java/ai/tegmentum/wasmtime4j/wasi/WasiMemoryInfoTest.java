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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiMemoryInfo} interface.
 *
 * <p>WasiMemoryInfo provides memory usage information for WASI instances.
 */
@DisplayName("WasiMemoryInfo Tests")
class WasiMemoryInfoTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiMemoryInfo.class.getModifiers()),
          "WasiMemoryInfo should be public");
      assertTrue(WasiMemoryInfo.class.isInterface(), "WasiMemoryInfo should be an interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getCurrentUsage method")
    void shouldHaveGetCurrentUsageMethod() throws NoSuchMethodException {
      final Method method = WasiMemoryInfo.class.getMethod("getCurrentUsage");
      assertNotNull(method, "getCurrentUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPeakUsage method")
    void shouldHaveGetPeakUsageMethod() throws NoSuchMethodException {
      final Method method = WasiMemoryInfo.class.getMethod("getPeakUsage");
      assertNotNull(method, "getPeakUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getLimit method")
    void shouldHaveGetLimitMethod() throws NoSuchMethodException {
      final Method method = WasiMemoryInfo.class.getMethod("getLimit");
      assertNotNull(method, "getLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getUsagePercentage method")
    void shouldHaveGetUsagePercentageMethod() throws NoSuchMethodException {
      final Method method = WasiMemoryInfo.class.getMethod("getUsagePercentage");
      assertNotNull(method, "getUsagePercentage method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have isNearLimit method")
    void shouldHaveIsNearLimitMethod() throws NoSuchMethodException {
      final Method method = WasiMemoryInfo.class.getMethod("isNearLimit");
      assertNotNull(method, "isNearLimit method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Implementation Behavior Tests")
  class ImplementationBehaviorTests {

    @Test
    @DisplayName("implementation with limit should report usage percentage")
    void implementationWithLimitShouldReportUsagePercentage() {
      final WasiMemoryInfo info = createTestInfo(50_000L, 100_000L, 100_000L);

      assertTrue(info.getLimit().isPresent(), "Should have limit");
      assertEquals(100_000L, info.getLimit().get(), "Limit should match");
      assertTrue(info.getUsagePercentage().isPresent(), "Should have usage percentage");
      assertEquals(50.0, info.getUsagePercentage().get(), 0.01, "Usage should be 50%");
    }

    @Test
    @DisplayName("implementation without limit should have empty optional")
    void implementationWithoutLimitShouldHaveEmptyOptional() {
      final WasiMemoryInfo info = createTestInfo(50_000L, 100_000L, -1L);

      assertFalse(info.getLimit().isPresent(), "Should not have limit");
      assertFalse(info.getUsagePercentage().isPresent(), "Should not have usage percentage");
    }

    @Test
    @DisplayName("isNearLimit should return true when usage above 80%")
    void isNearLimitShouldReturnTrueWhenUsageAbove80Percent() {
      final WasiMemoryInfo info = createTestInfo(85_000L, 90_000L, 100_000L);

      assertTrue(info.isNearLimit(), "Should be near limit at 85%");
    }

    @Test
    @DisplayName("isNearLimit should return false when usage below 80%")
    void isNearLimitShouldReturnFalseWhenUsageBelow80Percent() {
      final WasiMemoryInfo info = createTestInfo(50_000L, 60_000L, 100_000L);

      assertFalse(info.isNearLimit(), "Should not be near limit at 50%");
    }

    @Test
    @DisplayName("peak usage should be at least current usage")
    void peakUsageShouldBeAtLeastCurrentUsage() {
      final WasiMemoryInfo info = createTestInfo(50_000L, 80_000L, 100_000L);

      assertTrue(
          info.getPeakUsage() >= info.getCurrentUsage(), "Peak usage should be >= current usage");
    }

    private WasiMemoryInfo createTestInfo(
        final long currentUsage, final long peakUsage, final long limit) {
      return new WasiMemoryInfo() {
        @Override
        public long getCurrentUsage() {
          return currentUsage;
        }

        @Override
        public long getPeakUsage() {
          return peakUsage;
        }

        @Override
        public Optional<Long> getLimit() {
          return limit >= 0 ? Optional.of(limit) : Optional.empty();
        }

        @Override
        public Optional<Double> getUsagePercentage() {
          if (limit <= 0) {
            return Optional.empty();
          }
          return Optional.of((currentUsage * 100.0) / limit);
        }

        @Override
        public boolean isNearLimit() {
          return getUsagePercentage().map(p -> p > 80.0).orElse(false);
        }
      };
    }
  }
}
