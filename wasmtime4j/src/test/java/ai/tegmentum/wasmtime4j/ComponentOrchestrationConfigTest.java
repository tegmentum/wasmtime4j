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
 * Tests for {@link ComponentOrchestrationConfig} interface.
 *
 * <p>ComponentOrchestrationConfig provides configuration for component orchestration.
 */
@DisplayName("ComponentOrchestrationConfig Tests")
class ComponentOrchestrationConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentOrchestrationConfig.class.getModifiers()),
          "ComponentOrchestrationConfig should be public");
      assertTrue(
          ComponentOrchestrationConfig.class.isInterface(),
          "ComponentOrchestrationConfig should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getOrchestrationMode method")
    void shouldHaveGetOrchestrationModeMethod() throws NoSuchMethodException {
      final Method method = ComponentOrchestrationConfig.class.getMethod("getOrchestrationMode");
      assertNotNull(method, "getOrchestrationMode method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getCoordinationTimeout method")
    void shouldHaveGetCoordinationTimeoutMethod() throws NoSuchMethodException {
      final Method method = ComponentOrchestrationConfig.class.getMethod("getCoordinationTimeout");
      assertNotNull(method, "getCoordinationTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isOrchestrationEnabled method")
    void shouldHaveIsOrchestrationEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentOrchestrationConfig.class.getMethod("isOrchestrationEnabled");
      assertNotNull(method, "isOrchestrationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxConcurrentComponents method")
    void shouldHaveGetMaxConcurrentComponentsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentOrchestrationConfig.class.getMethod("getMaxConcurrentComponents");
      assertNotNull(method, "getMaxConcurrentComponents method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface contract. */
    private static class StubComponentOrchestrationConfig implements ComponentOrchestrationConfig {
      private final String orchestrationMode;
      private final long coordinationTimeout;
      private final boolean orchestrationEnabled;
      private final int maxConcurrentComponents;

      StubComponentOrchestrationConfig(
          final String orchestrationMode,
          final long coordinationTimeout,
          final boolean orchestrationEnabled,
          final int maxConcurrentComponents) {
        this.orchestrationMode = orchestrationMode;
        this.coordinationTimeout = coordinationTimeout;
        this.orchestrationEnabled = orchestrationEnabled;
        this.maxConcurrentComponents = maxConcurrentComponents;
      }

      @Override
      public String getOrchestrationMode() {
        return orchestrationMode;
      }

      @Override
      public long getCoordinationTimeout() {
        return coordinationTimeout;
      }

      @Override
      public boolean isOrchestrationEnabled() {
        return orchestrationEnabled;
      }

      @Override
      public int getMaxConcurrentComponents() {
        return maxConcurrentComponents;
      }
    }

    @Test
    @DisplayName("stub should return correct orchestration mode")
    void stubShouldReturnCorrectOrchestrationMode() {
      final ComponentOrchestrationConfig config =
          new StubComponentOrchestrationConfig("sequential", 30000L, true, 10);

      assertEquals("sequential", config.getOrchestrationMode(), "Mode should be sequential");
    }

    @Test
    @DisplayName("stub should return correct coordination timeout")
    void stubShouldReturnCorrectCoordinationTimeout() {
      final ComponentOrchestrationConfig config =
          new StubComponentOrchestrationConfig("parallel", 60000L, true, 10);

      assertEquals(60000L, config.getCoordinationTimeout(), "Timeout should be 60 seconds");
    }

    @Test
    @DisplayName("stub should return correct orchestration enabled state")
    void stubShouldReturnCorrectOrchestrationEnabledState() {
      final ComponentOrchestrationConfig configEnabled =
          new StubComponentOrchestrationConfig("parallel", 30000L, true, 10);
      final ComponentOrchestrationConfig configDisabled =
          new StubComponentOrchestrationConfig("parallel", 30000L, false, 10);

      assertTrue(configEnabled.isOrchestrationEnabled(), "Orchestration should be enabled");
      assertFalse(configDisabled.isOrchestrationEnabled(), "Orchestration should be disabled");
    }

    @Test
    @DisplayName("stub should return correct max concurrent components")
    void stubShouldReturnCorrectMaxConcurrentComponents() {
      final ComponentOrchestrationConfig config =
          new StubComponentOrchestrationConfig("parallel", 30000L, true, 100);

      assertEquals(100, config.getMaxConcurrentComponents(), "Max concurrent should be 100");
    }

    @Test
    @DisplayName("stub should handle zero timeout")
    void stubShouldHandleZeroTimeout() {
      final ComponentOrchestrationConfig config =
          new StubComponentOrchestrationConfig("parallel", 0L, true, 10);

      assertEquals(0L, config.getCoordinationTimeout(), "Timeout can be 0");
    }

    @Test
    @DisplayName("stub should handle max long timeout")
    void stubShouldHandleMaxLongTimeout() {
      final ComponentOrchestrationConfig config =
          new StubComponentOrchestrationConfig("parallel", Long.MAX_VALUE, true, 10);

      assertEquals(
          Long.MAX_VALUE, config.getCoordinationTimeout(), "Should handle max long timeout");
    }

    @Test
    @DisplayName("stub should handle max int concurrent components")
    void stubShouldHandleMaxIntConcurrentComponents() {
      final ComponentOrchestrationConfig config =
          new StubComponentOrchestrationConfig("parallel", 30000L, true, Integer.MAX_VALUE);

      assertEquals(
          Integer.MAX_VALUE,
          config.getMaxConcurrentComponents(),
          "Should handle max int concurrent");
    }

    @Test
    @DisplayName("stub should handle different orchestration modes")
    void stubShouldHandleDifferentOrchestrationModes() {
      final ComponentOrchestrationConfig sequential =
          new StubComponentOrchestrationConfig("sequential", 30000L, true, 10);
      final ComponentOrchestrationConfig parallel =
          new StubComponentOrchestrationConfig("parallel", 30000L, true, 10);
      final ComponentOrchestrationConfig distributed =
          new StubComponentOrchestrationConfig("distributed", 30000L, true, 10);

      assertEquals("sequential", sequential.getOrchestrationMode(), "Should be sequential");
      assertEquals("parallel", parallel.getOrchestrationMode(), "Should be parallel");
      assertEquals("distributed", distributed.getOrchestrationMode(), "Should be distributed");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactlyFourDeclaredMethods() {
      final var methods = ComponentOrchestrationConfig.class.getDeclaredMethods();
      assertEquals(4, methods.length, "Should have exactly 4 declared methods");
    }

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() {
      final var methods = ComponentOrchestrationConfig.class.getMethods();
      final var methodNames =
          java.util.Arrays.stream(methods)
              .map(Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getOrchestrationMode"), "Should have getOrchestrationMode");
      assertTrue(
          methodNames.contains("getCoordinationTimeout"), "Should have getCoordinationTimeout");
      assertTrue(
          methodNames.contains("isOrchestrationEnabled"), "Should have isOrchestrationEnabled");
      assertTrue(
          methodNames.contains("getMaxConcurrentComponents"),
          "Should have getMaxConcurrentComponents");
    }
  }
}
