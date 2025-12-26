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
 * Tests for {@link ComponentValidationConfig} class.
 *
 * <p>ComponentValidationConfig provides configuration options for validating WebAssembly
 * components.
 */
@DisplayName("ComponentValidationConfig Tests")
class ComponentValidationConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentValidationConfig.class.getModifiers()),
          "ComponentValidationConfig should be public");
      assertTrue(
          Modifier.isFinal(ComponentValidationConfig.class.getModifiers()),
          "ComponentValidationConfig should be final");
    }

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentValidationConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have defaultConfig method")
    void shouldHaveDefaultConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentValidationConfig.class.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultConfig should be static");
    }
  }

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("default config should have security validation enabled")
    void defaultConfigShouldHaveSecurityValidationEnabled() {
      final var config = ComponentValidationConfig.defaultConfig();

      assertTrue(config.isValidateSecurity(), "Security validation should be enabled by default");
    }

    @Test
    @DisplayName("default config should have performance validation enabled")
    void defaultConfigShouldHavePerformanceValidationEnabled() {
      final var config = ComponentValidationConfig.defaultConfig();

      assertTrue(
          config.isValidatePerformance(), "Performance validation should be enabled by default");
    }

    @Test
    @DisplayName("default config should have interface validation enabled")
    void defaultConfigShouldHaveInterfaceValidationEnabled() {
      final var config = ComponentValidationConfig.defaultConfig();

      assertTrue(
          config.isValidateInterfaces(), "Interface validation should be enabled by default");
    }

    @Test
    @DisplayName("default config should have dependency validation enabled")
    void defaultConfigShouldHaveDependencyValidationEnabled() {
      final var config = ComponentValidationConfig.defaultConfig();

      assertTrue(
          config.isValidateDependencies(), "Dependency validation should be enabled by default");
    }

    @Test
    @DisplayName("default config should have strict mode disabled")
    void defaultConfigShouldHaveStrictModeDisabled() {
      final var config = ComponentValidationConfig.defaultConfig();

      assertFalse(config.isStrictMode(), "Strict mode should be disabled by default");
    }

    @Test
    @DisplayName("default config should have 30 second timeout")
    void defaultConfigShouldHave30SecondTimeout() {
      final var config = ComponentValidationConfig.defaultConfig();

      assertEquals(30000L, config.getMaxValidationTimeMs(), "Default timeout should be 30 seconds");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create config")
    void builderShouldCreateConfig() {
      final var config = ComponentValidationConfig.builder().build();

      assertNotNull(config, "Builder should create config");
    }

    @Test
    @DisplayName("builder should set validateSecurity")
    void builderShouldSetValidateSecurity() {
      final var config = ComponentValidationConfig.builder().validateSecurity(false).build();

      assertFalse(config.isValidateSecurity(), "Should disable security validation");
    }

    @Test
    @DisplayName("builder should set validatePerformance")
    void builderShouldSetValidatePerformance() {
      final var config = ComponentValidationConfig.builder().validatePerformance(false).build();

      assertFalse(config.isValidatePerformance(), "Should disable performance validation");
    }

    @Test
    @DisplayName("builder should set validateInterfaces")
    void builderShouldSetValidateInterfaces() {
      final var config = ComponentValidationConfig.builder().validateInterfaces(false).build();

      assertFalse(config.isValidateInterfaces(), "Should disable interface validation");
    }

    @Test
    @DisplayName("builder should set validateDependencies")
    void builderShouldSetValidateDependencies() {
      final var config = ComponentValidationConfig.builder().validateDependencies(false).build();

      assertFalse(config.isValidateDependencies(), "Should disable dependency validation");
    }

    @Test
    @DisplayName("builder should set strictMode")
    void builderShouldSetStrictMode() {
      final var config = ComponentValidationConfig.builder().strictMode(true).build();

      assertTrue(config.isStrictMode(), "Should enable strict mode");
    }

    @Test
    @DisplayName("builder should set maxValidationTimeMs")
    void builderShouldSetMaxValidationTimeMs() {
      final var config = ComponentValidationConfig.builder().maxValidationTimeMs(60000L).build();

      assertEquals(60000L, config.getMaxValidationTimeMs(), "Should set timeout to 60 seconds");
    }

    @Test
    @DisplayName("builder should allow chaining")
    void builderShouldAllowChaining() {
      final var config =
          ComponentValidationConfig.builder()
              .validateSecurity(false)
              .validatePerformance(false)
              .validateInterfaces(true)
              .validateDependencies(true)
              .strictMode(true)
              .maxValidationTimeMs(10000L)
              .build();

      assertFalse(config.isValidateSecurity(), "Security validation should be disabled");
      assertFalse(config.isValidatePerformance(), "Performance validation should be disabled");
      assertTrue(config.isValidateInterfaces(), "Interface validation should be enabled");
      assertTrue(config.isValidateDependencies(), "Dependency validation should be enabled");
      assertTrue(config.isStrictMode(), "Strict mode should be enabled");
      assertEquals(10000L, config.getMaxValidationTimeMs(), "Timeout should be 10 seconds");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle zero timeout")
    void shouldHandleZeroTimeout() {
      final var config = ComponentValidationConfig.builder().maxValidationTimeMs(0L).build();

      assertEquals(0L, config.getMaxValidationTimeMs(), "Timeout can be 0");
    }

    @Test
    @DisplayName("should handle max long timeout")
    void shouldHandleMaxLongTimeout() {
      final var config =
          ComponentValidationConfig.builder().maxValidationTimeMs(Long.MAX_VALUE).build();

      assertEquals(
          Long.MAX_VALUE, config.getMaxValidationTimeMs(), "Should handle max long timeout");
    }

    @Test
    @DisplayName("should handle all validations disabled")
    void shouldHandleAllValidationsDisabled() {
      final var config =
          ComponentValidationConfig.builder()
              .validateSecurity(false)
              .validatePerformance(false)
              .validateInterfaces(false)
              .validateDependencies(false)
              .build();

      assertFalse(config.isValidateSecurity(), "Security validation should be disabled");
      assertFalse(config.isValidatePerformance(), "Performance validation should be disabled");
      assertFalse(config.isValidateInterfaces(), "Interface validation should be disabled");
      assertFalse(config.isValidateDependencies(), "Dependency validation should be disabled");
    }

    @Test
    @DisplayName("multiple builders should create independent configs")
    void multipleBuildsShouldCreateIndependentConfigs() {
      final var config1 = ComponentValidationConfig.builder().strictMode(true).build();
      final var config2 = ComponentValidationConfig.builder().strictMode(false).build();

      assertTrue(config1.isStrictMode(), "Config1 should have strict mode enabled");
      assertFalse(config2.isStrictMode(), "Config2 should have strict mode disabled");
    }
  }
}
