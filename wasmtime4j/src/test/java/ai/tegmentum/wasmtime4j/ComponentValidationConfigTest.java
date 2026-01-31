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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentValidationConfig} class.
 *
 * <p>ComponentValidationConfig provides configuration for validating WebAssembly components
 * including security, performance, interfaces, dependencies, strict mode, and time limits.
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
    @DisplayName("should have static builder() method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final var method = ComponentValidationConfig.class.getMethod("builder");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder() should be static");
    }

    @Test
    @DisplayName("should have static defaultConfig() method")
    void shouldHaveStaticDefaultConfigMethod() throws NoSuchMethodException {
      final var method = ComponentValidationConfig.class.getMethod("defaultConfig");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "defaultConfig() should be static");
    }
  }

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("defaultConfig should enable all validations")
    void defaultConfigShouldEnableAllValidations() {
      final ComponentValidationConfig config = ComponentValidationConfig.defaultConfig();

      assertNotNull(config, "defaultConfig should not be null");
      assertTrue(config.isValidateSecurity(), "Default validateSecurity should be true");
      assertTrue(config.isValidatePerformance(), "Default validatePerformance should be true");
      assertTrue(config.isValidateInterfaces(), "Default validateInterfaces should be true");
      assertTrue(
          config.isValidateDependencies(),
          "Default validateDependencies should be true");
      assertFalse(config.isStrictMode(), "Default strictMode should be false");
      assertEquals(
          30000L, config.getMaxValidationTimeMs(),
          "Default maxValidationTimeMs should be 30000");
    }
  }

  @Nested
  @DisplayName("Builder Fluent API Tests")
  class BuilderFluentApiTests {

    @Test
    @DisplayName("should disable security validation")
    void shouldDisableSecurityValidation() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .validateSecurity(false)
          .build();

      assertFalse(config.isValidateSecurity(), "validateSecurity should be false");
    }

    @Test
    @DisplayName("should disable performance validation")
    void shouldDisablePerformanceValidation() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .validatePerformance(false)
          .build();

      assertFalse(config.isValidatePerformance(), "validatePerformance should be false");
    }

    @Test
    @DisplayName("should disable interface validation")
    void shouldDisableInterfaceValidation() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .validateInterfaces(false)
          .build();

      assertFalse(config.isValidateInterfaces(), "validateInterfaces should be false");
    }

    @Test
    @DisplayName("should disable dependency validation")
    void shouldDisableDependencyValidation() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .validateDependencies(false)
          .build();

      assertFalse(config.isValidateDependencies(), "validateDependencies should be false");
    }

    @Test
    @DisplayName("should enable strict mode")
    void shouldEnableStrictMode() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .strictMode(true)
          .build();

      assertTrue(config.isStrictMode(), "strictMode should be true");
    }

    @Test
    @DisplayName("should set maxValidationTimeMs")
    void shouldSetMaxValidationTime() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .maxValidationTimeMs(60000L)
          .build();

      assertEquals(
          60000L, config.getMaxValidationTimeMs(),
          "maxValidationTimeMs should be 60000");
    }

    @Test
    @DisplayName("should support full method chaining")
    void shouldSupportFullChaining() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .validateSecurity(true)
          .validatePerformance(false)
          .validateInterfaces(true)
          .validateDependencies(false)
          .strictMode(true)
          .maxValidationTimeMs(10000L)
          .build();

      assertTrue(config.isValidateSecurity(), "security validation should be enabled");
      assertFalse(config.isValidatePerformance(), "performance validation should be disabled");
      assertTrue(config.isValidateInterfaces(), "interface validation should be enabled");
      assertFalse(config.isValidateDependencies(), "dependency validation should be disabled");
      assertTrue(config.isStrictMode(), "strict mode should be enabled");
      assertEquals(10000L, config.getMaxValidationTimeMs(), "maxValidationTimeMs should be 10000");
    }
  }

  @Nested
  @DisplayName("Boundary Value Tests")
  class BoundaryValueTests {

    @Test
    @DisplayName("should accept zero maxValidationTimeMs")
    void shouldAcceptZeroMaxTime() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .maxValidationTimeMs(0L)
          .build();

      assertEquals(0L, config.getMaxValidationTimeMs(), "Zero maxValidationTimeMs should be 0");
    }

    @Test
    @DisplayName("should accept very large maxValidationTimeMs")
    void shouldAcceptLargeMaxTime() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .maxValidationTimeMs(Long.MAX_VALUE)
          .build();

      assertEquals(
          Long.MAX_VALUE, config.getMaxValidationTimeMs(),
          "Should accept Long.MAX_VALUE");
    }

    @Test
    @DisplayName("should allow disabling all validations")
    void shouldAllowDisablingAllValidations() {
      final ComponentValidationConfig config = ComponentValidationConfig.builder()
          .validateSecurity(false)
          .validatePerformance(false)
          .validateInterfaces(false)
          .validateDependencies(false)
          .build();

      assertFalse(config.isValidateSecurity(), "Security should be disabled");
      assertFalse(config.isValidatePerformance(), "Performance should be disabled");
      assertFalse(config.isValidateInterfaces(), "Interfaces should be disabled");
      assertFalse(config.isValidateDependencies(), "Dependencies should be disabled");
    }
  }
}
