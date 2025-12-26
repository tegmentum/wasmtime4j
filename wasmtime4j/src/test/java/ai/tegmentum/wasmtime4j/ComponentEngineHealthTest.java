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
 * Tests for {@link ComponentEngineHealth} interface.
 *
 * <p>ComponentEngineHealth provides health information for WebAssembly component engines.
 */
@DisplayName("ComponentEngineHealth Tests")
class ComponentEngineHealthTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentEngineHealth.class.getModifiers()),
          "ComponentEngineHealth should be public");
      assertTrue(
          ComponentEngineHealth.class.isInterface(),
          "ComponentEngineHealth should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getHealthStatus method")
    void shouldHaveGetHealthStatusMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineHealth.class.getMethod("getHealthStatus");
      assertNotNull(method, "getHealthStatus method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isHealthy method")
    void shouldHaveIsHealthyMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineHealth.class.getMethod("isHealthy");
      assertNotNull(method, "isHealthy method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getLastHealthCheckTime method")
    void shouldHaveGetLastHealthCheckTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineHealth.class.getMethod("getLastHealthCheckTime");
      assertNotNull(method, "getLastHealthCheckTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getHealthStatus should take no parameters")
    void getHealthStatusShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = ComponentEngineHealth.class.getMethod("getHealthStatus");
      assertEquals(0, method.getParameterCount(), "getHealthStatus should take no parameters");
    }

    @Test
    @DisplayName("isHealthy should take no parameters")
    void isHealthyShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = ComponentEngineHealth.class.getMethod("isHealthy");
      assertEquals(0, method.getParameterCount(), "isHealthy should take no parameters");
    }

    @Test
    @DisplayName("getLastHealthCheckTime should take no parameters")
    void getLastHealthCheckTimeShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = ComponentEngineHealth.class.getMethod("getLastHealthCheckTime");
      assertEquals(
          0, method.getParameterCount(), "getLastHealthCheckTime should take no parameters");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have exactly 3 declared methods")
    void shouldHaveExactlyThreeDeclaredMethods() {
      final var methods = ComponentEngineHealth.class.getDeclaredMethods();
      assertEquals(3, methods.length, "Should have exactly 3 declared methods");
    }

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() {
      final var methods = ComponentEngineHealth.class.getMethods();
      final var methodNames =
          java.util.Arrays.stream(methods)
              .map(Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getHealthStatus"), "Should have getHealthStatus");
      assertTrue(methodNames.contains("isHealthy"), "Should have isHealthy");
      assertTrue(
          methodNames.contains("getLastHealthCheckTime"), "Should have getLastHealthCheckTime");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface contract. */
    private static class StubComponentEngineHealth implements ComponentEngineHealth {
      private final String status;
      private final boolean healthy;
      private final long lastCheckTime;

      StubComponentEngineHealth(
          final String status, final boolean healthy, final long lastCheckTime) {
        this.status = status;
        this.healthy = healthy;
        this.lastCheckTime = lastCheckTime;
      }

      @Override
      public String getHealthStatus() {
        return status;
      }

      @Override
      public boolean isHealthy() {
        return healthy;
      }

      @Override
      public long getLastHealthCheckTime() {
        return lastCheckTime;
      }
    }

    @Test
    @DisplayName("stub should return healthy status")
    void stubShouldReturnHealthyStatus() {
      final ComponentEngineHealth health = new StubComponentEngineHealth("UP", true, 1000L);

      assertEquals("UP", health.getHealthStatus(), "Status should be UP");
      assertTrue(health.isHealthy(), "Should be healthy");
      assertEquals(1000L, health.getLastHealthCheckTime(), "Last check time should be 1000");
    }

    @Test
    @DisplayName("stub should return unhealthy status")
    void stubShouldReturnUnhealthyStatus() {
      final ComponentEngineHealth health =
          new StubComponentEngineHealth("DOWN - Out of memory", false, 2000L);

      assertEquals("DOWN - Out of memory", health.getHealthStatus(), "Status should indicate down");
      assertTrue(!health.isHealthy(), "Should not be healthy");
      assertEquals(2000L, health.getLastHealthCheckTime(), "Last check time should be 2000");
    }

    @Test
    @DisplayName("stub should handle null status")
    void stubShouldHandleNullStatus() {
      final ComponentEngineHealth health = new StubComponentEngineHealth(null, true, 0L);

      assertEquals(null, health.getHealthStatus(), "Status can be null");
    }

    @Test
    @DisplayName("stub should handle zero timestamp")
    void stubShouldHandleZeroTimestamp() {
      final ComponentEngineHealth health = new StubComponentEngineHealth("UP", true, 0L);

      assertEquals(0L, health.getLastHealthCheckTime(), "Timestamp can be zero");
    }

    @Test
    @DisplayName("stub should handle max long timestamp")
    void stubShouldHandleMaxLongTimestamp() {
      final ComponentEngineHealth health =
          new StubComponentEngineHealth("UP", true, Long.MAX_VALUE);

      assertEquals(
          Long.MAX_VALUE, health.getLastHealthCheckTime(), "Timestamp can be max long value");
    }
  }
}
