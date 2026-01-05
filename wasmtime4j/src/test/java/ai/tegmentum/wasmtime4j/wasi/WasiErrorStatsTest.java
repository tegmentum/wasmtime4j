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
 * Tests for {@link WasiErrorStats} interface.
 *
 * <p>WasiErrorStats provides error statistics for WASI components.
 */
@DisplayName("WasiErrorStats Tests")
class WasiErrorStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiErrorStats.class.getModifiers()),
          "WasiErrorStats should be public");
      assertTrue(WasiErrorStats.class.isInterface(), "WasiErrorStats should be an interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getTotalErrors method")
    void shouldHaveGetTotalErrorsMethod() throws NoSuchMethodException {
      final Method method = WasiErrorStats.class.getMethod("getTotalErrors");
      assertNotNull(method, "getTotalErrors method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getErrorsByType method")
    void shouldHaveGetErrorsByTypeMethod() throws NoSuchMethodException {
      final Method method = WasiErrorStats.class.getMethod("getErrorsByType");
      assertNotNull(method, "getErrorsByType method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getFatalErrors method")
    void shouldHaveGetFatalErrorsMethod() throws NoSuchMethodException {
      final Method method = WasiErrorStats.class.getMethod("getFatalErrors");
      assertNotNull(method, "getFatalErrors method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getRecoverableErrors method")
    void shouldHaveGetRecoverableErrorsMethod() throws NoSuchMethodException {
      final Method method = WasiErrorStats.class.getMethod("getRecoverableErrors");
      assertNotNull(method, "getRecoverableErrors method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Implementation Behavior Tests")
  class ImplementationBehaviorTests {

    @Test
    @DisplayName("implementation should track total errors")
    void implementationShouldTrackTotalErrors() {
      final Map<String, Long> errorsByType = new HashMap<>();
      final WasiErrorStats stats = createTestStats(100L, errorsByType, 10L, 90L);

      assertEquals(100L, stats.getTotalErrors(), "Total errors should match");
    }

    @Test
    @DisplayName("implementation should track errors by type")
    void implementationShouldTrackErrorsByType() {
      final Map<String, Long> errorsByType = new HashMap<>();
      errorsByType.put("IOException", 50L);
      errorsByType.put("SecurityException", 30L);
      errorsByType.put("TimeoutException", 20L);

      final WasiErrorStats stats = createTestStats(100L, errorsByType, 10L, 90L);

      assertEquals(3, stats.getErrorsByType().size(), "Should have 3 error types");
      assertEquals(
          50L, stats.getErrorsByType().get("IOException"), "IOException count should match");
      assertEquals(
          30L,
          stats.getErrorsByType().get("SecurityException"),
          "SecurityException count should match");
    }

    @Test
    @DisplayName("implementation should distinguish fatal and recoverable errors")
    void implementationShouldDistinguishFatalAndRecoverableErrors() {
      final Map<String, Long> errorsByType = new HashMap<>();
      final WasiErrorStats stats = createTestStats(100L, errorsByType, 15L, 85L);

      assertEquals(15L, stats.getFatalErrors(), "Fatal errors should match");
      assertEquals(85L, stats.getRecoverableErrors(), "Recoverable errors should match");
    }

    @Test
    @DisplayName("total errors should equal fatal plus recoverable")
    void totalErrorsShouldEqualFatalPlusRecoverable() {
      final Map<String, Long> errorsByType = new HashMap<>();
      final long fatal = 25L;
      final long recoverable = 75L;
      final WasiErrorStats stats =
          createTestStats(fatal + recoverable, errorsByType, fatal, recoverable);

      assertEquals(
          stats.getTotalErrors(),
          stats.getFatalErrors() + stats.getRecoverableErrors(),
          "Total should equal fatal + recoverable");
    }

    private WasiErrorStats createTestStats(
        final long totalErrors,
        final Map<String, Long> errorsByType,
        final long fatalErrors,
        final long recoverableErrors) {
      return new WasiErrorStats() {
        @Override
        public long getTotalErrors() {
          return totalErrors;
        }

        @Override
        public Map<String, Long> getErrorsByType() {
          return new HashMap<>(errorsByType);
        }

        @Override
        public long getFatalErrors() {
          return fatalErrors;
        }

        @Override
        public long getRecoverableErrors() {
          return recoverableErrors;
        }
      };
    }
  }
}
