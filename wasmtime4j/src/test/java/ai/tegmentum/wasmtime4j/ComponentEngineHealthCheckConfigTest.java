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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineHealthCheckConfig} interface.
 *
 * <p>ComponentEngineHealthCheckConfig defines configuration for WebAssembly component engine health
 * checks including interval, timeout, and enabled state.
 */
@DisplayName("ComponentEngineHealthCheckConfig Tests")
class ComponentEngineHealthCheckConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentEngineHealthCheckConfig.class.isInterface(),
          "ComponentEngineHealthCheckConfig should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentEngineHealthCheckConfig.class.getModifiers()),
          "ComponentEngineHealthCheckConfig should be public");
    }
  }

  @Nested
  @DisplayName("Method Declaration Tests")
  class MethodDeclarationTests {

    @Test
    @DisplayName("should have getHealthCheckInterval method")
    void shouldHaveGetHealthCheckIntervalMethod() throws NoSuchMethodException {
      final Method method =
          ComponentEngineHealthCheckConfig.class.getMethod("getHealthCheckInterval");
      assertNotNull(method, "getHealthCheckInterval method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getHealthCheckTimeout method")
    void shouldHaveGetHealthCheckTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          ComponentEngineHealthCheckConfig.class.getMethod("getHealthCheckTimeout");
      assertNotNull(method, "getHealthCheckTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isHealthCheckEnabled method")
    void shouldHaveIsHealthCheckEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ComponentEngineHealthCheckConfig.class.getMethod("isHealthCheckEnabled");
      assertNotNull(method, "isHealthCheckEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface behavior. */
    private static final class StubHealthCheckConfig implements ComponentEngineHealthCheckConfig {
      private final long interval;
      private final long timeout;
      private final boolean enabled;

      StubHealthCheckConfig(final long interval, final long timeout, final boolean enabled) {
        this.interval = interval;
        this.timeout = timeout;
        this.enabled = enabled;
      }

      @Override
      public long getHealthCheckInterval() {
        return interval;
      }

      @Override
      public long getHealthCheckTimeout() {
        return timeout;
      }

      @Override
      public boolean isHealthCheckEnabled() {
        return enabled;
      }
    }

    @Test
    @DisplayName("stub should implement all methods")
    void stubShouldImplementAllMethods() {
      final ComponentEngineHealthCheckConfig config = new StubHealthCheckConfig(30000, 5000, true);

      assertEquals(30000, config.getHealthCheckInterval(), "Interval should match");
      assertEquals(5000, config.getHealthCheckTimeout(), "Timeout should match");
      assertTrue(config.isHealthCheckEnabled(), "Should be enabled");
    }

    @Test
    @DisplayName("stub should handle disabled state")
    void stubShouldHandleDisabledState() {
      final ComponentEngineHealthCheckConfig config = new StubHealthCheckConfig(60000, 10000, false);

      assertEquals(60000, config.getHealthCheckInterval(), "Interval should match");
      assertEquals(10000, config.getHealthCheckTimeout(), "Timeout should match");
      assertFalse(config.isHealthCheckEnabled(), "Should be disabled");
    }

    @Test
    @DisplayName("stub should handle zero values")
    void stubShouldHandleZeroValues() {
      final ComponentEngineHealthCheckConfig config = new StubHealthCheckConfig(0, 0, false);

      assertEquals(0, config.getHealthCheckInterval(), "Interval should be 0");
      assertEquals(0, config.getHealthCheckTimeout(), "Timeout should be 0");
      assertFalse(config.isHealthCheckEnabled(), "Should be disabled");
    }

    @Test
    @DisplayName("stub should handle large values")
    void stubShouldHandleLargeValues() {
      final ComponentEngineHealthCheckConfig config =
          new StubHealthCheckConfig(Long.MAX_VALUE, Long.MAX_VALUE, true);

      assertEquals(Long.MAX_VALUE, config.getHealthCheckInterval(), "Interval should be max");
      assertEquals(Long.MAX_VALUE, config.getHealthCheckTimeout(), "Timeout should be max");
      assertTrue(config.isHealthCheckEnabled(), "Should be enabled");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support typical health check configuration")
    void shouldSupportTypicalHealthCheckConfiguration() {
      // Typical config: 30s interval, 5s timeout, enabled
      final ComponentEngineHealthCheckConfig config =
          new StubImplementationTests.StubHealthCheckConfig(30000, 5000, true);

      assertTrue(config.isHealthCheckEnabled(), "Health checks should be enabled");
      assertTrue(config.getHealthCheckInterval() > 0, "Should have positive interval");
      assertTrue(config.getHealthCheckTimeout() > 0, "Should have positive timeout");
      assertTrue(
          config.getHealthCheckInterval() > config.getHealthCheckTimeout(),
          "Interval should be greater than timeout");
    }

    @Test
    @DisplayName("should support disabled health checks")
    void shouldSupportDisabledHealthChecks() {
      final ComponentEngineHealthCheckConfig config =
          new StubImplementationTests.StubHealthCheckConfig(0, 0, false);

      assertFalse(config.isHealthCheckEnabled(), "Health checks should be disabled");
    }

    @Test
    @DisplayName("should support aggressive health checking")
    void shouldSupportAggressiveHealthChecking() {
      // Aggressive config: 1s interval, 500ms timeout
      final ComponentEngineHealthCheckConfig config =
          new StubImplementationTests.StubHealthCheckConfig(1000, 500, true);

      assertTrue(config.isHealthCheckEnabled(), "Health checks should be enabled");
      assertEquals(1000, config.getHealthCheckInterval(), "Should have 1s interval");
      assertEquals(500, config.getHealthCheckTimeout(), "Should have 500ms timeout");
    }

    @Test
    @DisplayName("should support relaxed health checking")
    void shouldSupportRelaxedHealthChecking() {
      // Relaxed config: 5 minute interval, 30s timeout
      final ComponentEngineHealthCheckConfig config =
          new StubImplementationTests.StubHealthCheckConfig(300000, 30000, true);

      assertTrue(config.isHealthCheckEnabled(), "Health checks should be enabled");
      assertEquals(300000, config.getHealthCheckInterval(), "Should have 5 minute interval");
      assertEquals(30000, config.getHealthCheckTimeout(), "Should have 30s timeout");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should not be instantiable directly")
    void shouldNotBeInstantiableDirectly() {
      assertTrue(
          ComponentEngineHealthCheckConfig.class.isInterface(),
          "Interface should not be directly instantiable");
    }

    @Test
    @DisplayName("should have exactly three methods")
    void shouldHaveExactlyThreeMethods() {
      final Method[] methods = ComponentEngineHealthCheckConfig.class.getDeclaredMethods();
      assertEquals(3, methods.length, "Should have exactly 3 methods");
    }

    @Test
    @DisplayName("all methods should have no parameters")
    void allMethodsShouldHaveNoParameters() {
      for (final Method method : ComponentEngineHealthCheckConfig.class.getDeclaredMethods()) {
        assertEquals(
            0, method.getParameterCount(), "Method " + method.getName() + " should have no params");
      }
    }
  }
}
