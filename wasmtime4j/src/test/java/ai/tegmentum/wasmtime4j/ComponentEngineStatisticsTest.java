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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineStatistics} interface.
 *
 * <p>ComponentEngineStatistics provides statistics about a WebAssembly component engine including
 * component counts, instance counts, memory usage, and uptime.
 */
@DisplayName("ComponentEngineStatistics Tests")
class ComponentEngineStatisticsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentEngineStatistics.class.isInterface(),
          "ComponentEngineStatistics should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentEngineStatistics.class.getModifiers()),
          "ComponentEngineStatistics should be public");
    }
  }

  @Nested
  @DisplayName("Method Declaration Tests")
  class MethodDeclarationTests {

    @Test
    @DisplayName("should have getComponentCount method")
    void shouldHaveGetComponentCountMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineStatistics.class.getMethod("getComponentCount");
      assertNotNull(method, "getComponentCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getInstanceCount method")
    void shouldHaveGetInstanceCountMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineStatistics.class.getMethod("getInstanceCount");
      assertNotNull(method, "getInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineStatistics.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineStatistics.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface behavior. */
    private static final class StubComponentEngineStatistics implements ComponentEngineStatistics {
      private final long componentCount;
      private final long instanceCount;
      private final long memoryUsage;
      private final long uptime;

      StubComponentEngineStatistics(
          final long componentCount,
          final long instanceCount,
          final long memoryUsage,
          final long uptime) {
        this.componentCount = componentCount;
        this.instanceCount = instanceCount;
        this.memoryUsage = memoryUsage;
        this.uptime = uptime;
      }

      @Override
      public long getComponentCount() {
        return componentCount;
      }

      @Override
      public long getInstanceCount() {
        return instanceCount;
      }

      @Override
      public long getMemoryUsage() {
        return memoryUsage;
      }

      @Override
      public long getUptime() {
        return uptime;
      }
    }

    @Test
    @DisplayName("stub should implement all methods")
    void stubShouldImplementAllMethods() {
      final ComponentEngineStatistics stats = new StubComponentEngineStatistics(10, 5, 1024, 60000);

      assertEquals(10, stats.getComponentCount(), "Component count should match");
      assertEquals(5, stats.getInstanceCount(), "Instance count should match");
      assertEquals(1024, stats.getMemoryUsage(), "Memory usage should match");
      assertEquals(60000, stats.getUptime(), "Uptime should match");
    }

    @Test
    @DisplayName("stub should handle zero values")
    void stubShouldHandleZeroValues() {
      final ComponentEngineStatistics stats = new StubComponentEngineStatistics(0, 0, 0, 0);

      assertEquals(0, stats.getComponentCount(), "Component count should be 0");
      assertEquals(0, stats.getInstanceCount(), "Instance count should be 0");
      assertEquals(0, stats.getMemoryUsage(), "Memory usage should be 0");
      assertEquals(0, stats.getUptime(), "Uptime should be 0");
    }

    @Test
    @DisplayName("stub should handle large values")
    void stubShouldHandleLargeValues() {
      final ComponentEngineStatistics stats =
          new StubComponentEngineStatistics(
              Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, stats.getComponentCount(), "Component count should be max");
      assertEquals(Long.MAX_VALUE, stats.getInstanceCount(), "Instance count should be max");
      assertEquals(Long.MAX_VALUE, stats.getMemoryUsage(), "Memory usage should be max");
      assertEquals(Long.MAX_VALUE, stats.getUptime(), "Uptime should be max");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support monitoring component counts")
    void shouldSupportMonitoringComponentCounts() {
      final ComponentEngineStatistics stats =
          new StubImplementationTests.StubComponentEngineStatistics(100, 50, 10240, 3600000);

      assertTrue(stats.getComponentCount() > 0, "Should have loaded components");
      assertTrue(stats.getInstanceCount() > 0, "Should have active instances");
    }

    @Test
    @DisplayName("should support memory monitoring")
    void shouldSupportMemoryMonitoring() {
      final ComponentEngineStatistics stats =
          new StubImplementationTests.StubComponentEngineStatistics(10, 5, 1048576, 1000);

      assertTrue(stats.getMemoryUsage() > 0, "Should report memory usage");
    }

    @Test
    @DisplayName("should support uptime tracking")
    void shouldSupportUptimeTracking() {
      final ComponentEngineStatistics stats =
          new StubImplementationTests.StubComponentEngineStatistics(1, 1, 100, 86400000);

      assertTrue(stats.getUptime() > 0, "Should report uptime");
      assertEquals(86400000, stats.getUptime(), "Should match expected uptime");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should not be instantiable directly")
    void shouldNotBeInstantiableDirectly() {
      assertTrue(
          ComponentEngineStatistics.class.isInterface(),
          "Interface should not be directly instantiable");
    }

    @Test
    @DisplayName("should have exactly four methods")
    void shouldHaveExactlyFourMethods() {
      final Method[] methods = ComponentEngineStatistics.class.getDeclaredMethods();
      assertEquals(4, methods.length, "Should have exactly 4 methods");
    }

    @Test
    @DisplayName("all methods should have no parameters")
    void allMethodsShouldHaveNoParameters() {
      for (final Method method : ComponentEngineStatistics.class.getDeclaredMethods()) {
        assertEquals(
            0, method.getParameterCount(), "Method " + method.getName() + " should have no params");
      }
    }

    @Test
    @DisplayName("all methods should return long")
    void allMethodsShouldReturnLong() {
      for (final Method method : ComponentEngineStatistics.class.getDeclaredMethods()) {
        assertEquals(
            long.class,
            method.getReturnType(),
            "Method " + method.getName() + " should return long");
      }
    }
  }
}
