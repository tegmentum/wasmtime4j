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

import ai.tegmentum.wasmtime4j.component.ComponentLoadConfig;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLoadConfig} interface.
 *
 * <p>ComponentLoadConfig provides configuration for WebAssembly component loading.
 */
@DisplayName("ComponentLoadConfig Tests")
class ComponentLoadConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentLoadConfig.class.getModifiers()),
          "ComponentLoadConfig should be public");
      assertTrue(
          ComponentLoadConfig.class.isInterface(), "ComponentLoadConfig should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getSourcePath method")
    void shouldHaveGetSourcePathMethod() throws NoSuchMethodException {
      final Method method = ComponentLoadConfig.class.getMethod("getSourcePath");
      assertNotNull(method, "getSourcePath method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getLoadTimeout method")
    void shouldHaveGetLoadTimeoutMethod() throws NoSuchMethodException {
      final Method method = ComponentLoadConfig.class.getMethod("getLoadTimeout");
      assertNotNull(method, "getLoadTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isValidationEnabled method")
    void shouldHaveIsValidationEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentLoadConfig.class.getMethod("isValidationEnabled");
      assertNotNull(method, "isValidationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getLoadStrategy method")
    void shouldHaveGetLoadStrategyMethod() throws NoSuchMethodException {
      final Method method = ComponentLoadConfig.class.getMethod("getLoadStrategy");
      assertNotNull(method, "getLoadStrategy method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface contract. */
    private static class StubComponentLoadConfig implements ComponentLoadConfig {
      private final String sourcePath;
      private final long loadTimeout;
      private final boolean validationEnabled;
      private final String loadStrategy;

      StubComponentLoadConfig(
          final String sourcePath,
          final long loadTimeout,
          final boolean validationEnabled,
          final String loadStrategy) {
        this.sourcePath = sourcePath;
        this.loadTimeout = loadTimeout;
        this.validationEnabled = validationEnabled;
        this.loadStrategy = loadStrategy;
      }

      @Override
      public String getSourcePath() {
        return sourcePath;
      }

      @Override
      public long getLoadTimeout() {
        return loadTimeout;
      }

      @Override
      public boolean isValidationEnabled() {
        return validationEnabled;
      }

      @Override
      public String getLoadStrategy() {
        return loadStrategy;
      }
    }

    @Test
    @DisplayName("stub should return correct source path")
    void stubShouldReturnCorrectSourcePath() {
      final ComponentLoadConfig config =
          new StubComponentLoadConfig("/path/to/component.wasm", 5000L, true, "lazy");

      assertEquals("/path/to/component.wasm", config.getSourcePath(), "Source path should match");
    }

    @Test
    @DisplayName("stub should return correct load timeout")
    void stubShouldReturnCorrectLoadTimeout() {
      final ComponentLoadConfig config =
          new StubComponentLoadConfig("/path/to/component.wasm", 10000L, true, "eager");

      assertEquals(10000L, config.getLoadTimeout(), "Load timeout should be 10 seconds");
    }

    @Test
    @DisplayName("stub should return correct validation enabled state")
    void stubShouldReturnCorrectValidationEnabledState() {
      final ComponentLoadConfig configEnabled =
          new StubComponentLoadConfig("/path/to/component.wasm", 5000L, true, "lazy");
      final ComponentLoadConfig configDisabled =
          new StubComponentLoadConfig("/path/to/component.wasm", 5000L, false, "lazy");

      assertTrue(configEnabled.isValidationEnabled(), "Validation should be enabled");
      assertFalse(configDisabled.isValidationEnabled(), "Validation should be disabled");
    }

    @Test
    @DisplayName("stub should return correct load strategy")
    void stubShouldReturnCorrectLoadStrategy() {
      final ComponentLoadConfig config =
          new StubComponentLoadConfig("/path/to/component.wasm", 5000L, true, "parallel");

      assertEquals("parallel", config.getLoadStrategy(), "Load strategy should be parallel");
    }

    @Test
    @DisplayName("stub should handle zero timeout")
    void stubShouldHandleZeroTimeout() {
      final ComponentLoadConfig config =
          new StubComponentLoadConfig("/path/to/component.wasm", 0L, true, "lazy");

      assertEquals(0L, config.getLoadTimeout(), "Load timeout can be 0");
    }

    @Test
    @DisplayName("stub should handle max timeout")
    void stubShouldHandleMaxTimeout() {
      final ComponentLoadConfig config =
          new StubComponentLoadConfig("/path/to/component.wasm", Long.MAX_VALUE, true, "lazy");

      assertEquals(Long.MAX_VALUE, config.getLoadTimeout(), "Should handle max timeout");
    }

    @Test
    @DisplayName("stub should handle null source path")
    void stubShouldHandleNullSourcePath() {
      final ComponentLoadConfig config = new StubComponentLoadConfig(null, 5000L, true, "lazy");

      assertEquals(null, config.getSourcePath(), "Source path can be null");
    }

    @Test
    @DisplayName("stub should handle different load strategies")
    void stubShouldHandleDifferentLoadStrategies() {
      final ComponentLoadConfig lazy = new StubComponentLoadConfig("/path", 1000L, true, "lazy");
      final ComponentLoadConfig eager = new StubComponentLoadConfig("/path", 1000L, true, "eager");
      final ComponentLoadConfig streaming =
          new StubComponentLoadConfig("/path", 1000L, true, "streaming");

      assertEquals("lazy", lazy.getLoadStrategy(), "Should be lazy");
      assertEquals("eager", eager.getLoadStrategy(), "Should be eager");
      assertEquals("streaming", streaming.getLoadStrategy(), "Should be streaming");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactlyFourDeclaredMethods() {
      final var methods = ComponentLoadConfig.class.getDeclaredMethods();
      assertEquals(4, methods.length, "Should have exactly 4 declared methods");
    }

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() {
      final var methods = ComponentLoadConfig.class.getMethods();
      final var methodNames =
          java.util.Arrays.stream(methods)
              .map(Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getSourcePath"), "Should have getSourcePath");
      assertTrue(methodNames.contains("getLoadTimeout"), "Should have getLoadTimeout");
      assertTrue(methodNames.contains("isValidationEnabled"), "Should have isValidationEnabled");
      assertTrue(methodNames.contains("getLoadStrategy"), "Should have getLoadStrategy");
    }
  }
}
