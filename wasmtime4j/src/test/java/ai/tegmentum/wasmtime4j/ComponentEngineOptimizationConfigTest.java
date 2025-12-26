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
 * Tests for {@link ComponentEngineOptimizationConfig} interface.
 *
 * <p>ComponentEngineOptimizationConfig provides configuration for WebAssembly component engine
 * optimization.
 */
@DisplayName("ComponentEngineOptimizationConfig Tests")
class ComponentEngineOptimizationConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentEngineOptimizationConfig.class.getModifiers()),
          "ComponentEngineOptimizationConfig should be public");
      assertTrue(
          ComponentEngineOptimizationConfig.class.isInterface(),
          "ComponentEngineOptimizationConfig should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      final Method method =
          ComponentEngineOptimizationConfig.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isOptimizationEnabled method")
    void shouldHaveIsOptimizationEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ComponentEngineOptimizationConfig.class.getMethod("isOptimizationEnabled");
      assertNotNull(method, "isOptimizationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getOptimizationTimeout method")
    void shouldHaveGetOptimizationTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          ComponentEngineOptimizationConfig.class.getMethod("getOptimizationTimeout");
      assertNotNull(method, "getOptimizationTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface contract. */
    private static class StubComponentEngineOptimizationConfig
        implements ComponentEngineOptimizationConfig {
      private final String optimizationLevel;
      private final boolean optimizationEnabled;
      private final long optimizationTimeout;

      StubComponentEngineOptimizationConfig(
          final String optimizationLevel,
          final boolean optimizationEnabled,
          final long optimizationTimeout) {
        this.optimizationLevel = optimizationLevel;
        this.optimizationEnabled = optimizationEnabled;
        this.optimizationTimeout = optimizationTimeout;
      }

      @Override
      public String getOptimizationLevel() {
        return optimizationLevel;
      }

      @Override
      public boolean isOptimizationEnabled() {
        return optimizationEnabled;
      }

      @Override
      public long getOptimizationTimeout() {
        return optimizationTimeout;
      }
    }

    @Test
    @DisplayName("stub should return correct optimization level")
    void stubShouldReturnCorrectOptimizationLevel() {
      final ComponentEngineOptimizationConfig config =
          new StubComponentEngineOptimizationConfig("aggressive", true, 30000L);

      assertEquals("aggressive", config.getOptimizationLevel(), "Level should be aggressive");
    }

    @Test
    @DisplayName("stub should return correct optimization enabled state")
    void stubShouldReturnCorrectOptimizationEnabledState() {
      final ComponentEngineOptimizationConfig configEnabled =
          new StubComponentEngineOptimizationConfig("standard", true, 30000L);
      final ComponentEngineOptimizationConfig configDisabled =
          new StubComponentEngineOptimizationConfig("standard", false, 30000L);

      assertTrue(configEnabled.isOptimizationEnabled(), "Optimization should be enabled");
      assertFalse(configDisabled.isOptimizationEnabled(), "Optimization should be disabled");
    }

    @Test
    @DisplayName("stub should return correct optimization timeout")
    void stubShouldReturnCorrectOptimizationTimeout() {
      final ComponentEngineOptimizationConfig config =
          new StubComponentEngineOptimizationConfig("standard", true, 60000L);

      assertEquals(60000L, config.getOptimizationTimeout(), "Timeout should be 60 seconds");
    }

    @Test
    @DisplayName("stub should handle zero timeout")
    void stubShouldHandleZeroTimeout() {
      final ComponentEngineOptimizationConfig config =
          new StubComponentEngineOptimizationConfig("standard", true, 0L);

      assertEquals(0L, config.getOptimizationTimeout(), "Timeout can be 0");
    }

    @Test
    @DisplayName("stub should handle max long timeout")
    void stubShouldHandleMaxLongTimeout() {
      final ComponentEngineOptimizationConfig config =
          new StubComponentEngineOptimizationConfig("standard", true, Long.MAX_VALUE);

      assertEquals(
          Long.MAX_VALUE, config.getOptimizationTimeout(), "Should handle max long timeout");
    }

    @Test
    @DisplayName("stub should handle different optimization levels")
    void stubShouldHandleDifferentOptimizationLevels() {
      final ComponentEngineOptimizationConfig none =
          new StubComponentEngineOptimizationConfig("none", false, 30000L);
      final ComponentEngineOptimizationConfig standard =
          new StubComponentEngineOptimizationConfig("standard", true, 30000L);
      final ComponentEngineOptimizationConfig aggressive =
          new StubComponentEngineOptimizationConfig("aggressive", true, 30000L);
      final ComponentEngineOptimizationConfig maximum =
          new StubComponentEngineOptimizationConfig("maximum", true, 30000L);

      assertEquals("none", none.getOptimizationLevel(), "Should be none");
      assertEquals("standard", standard.getOptimizationLevel(), "Should be standard");
      assertEquals("aggressive", aggressive.getOptimizationLevel(), "Should be aggressive");
      assertEquals("maximum", maximum.getOptimizationLevel(), "Should be maximum");
    }

    @Test
    @DisplayName("stub should handle null optimization level")
    void stubShouldHandleNullOptimizationLevel() {
      final ComponentEngineOptimizationConfig config =
          new StubComponentEngineOptimizationConfig(null, true, 30000L);

      assertEquals(null, config.getOptimizationLevel(), "Level can be null");
    }

    @Test
    @DisplayName("stub should handle empty optimization level")
    void stubShouldHandleEmptyOptimizationLevel() {
      final ComponentEngineOptimizationConfig config =
          new StubComponentEngineOptimizationConfig("", true, 30000L);

      assertEquals("", config.getOptimizationLevel(), "Level can be empty");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have exactly 3 declared methods")
    void shouldHaveExactlyThreeDeclaredMethods() {
      final var methods = ComponentEngineOptimizationConfig.class.getDeclaredMethods();
      assertEquals(3, methods.length, "Should have exactly 3 declared methods");
    }

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() {
      final var methods = ComponentEngineOptimizationConfig.class.getMethods();
      final var methodNames =
          java.util.Arrays.stream(methods)
              .map(Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getOptimizationLevel"), "Should have getOptimizationLevel");
      assertTrue(
          methodNames.contains("isOptimizationEnabled"), "Should have isOptimizationEnabled");
      assertTrue(
          methodNames.contains("getOptimizationTimeout"), "Should have getOptimizationTimeout");
    }
  }
}
