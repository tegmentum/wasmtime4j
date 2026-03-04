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

import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineConfig} class.
 *
 * <p>ComponentEngineConfig provides a convenience wrapper around {@link EngineConfig} that
 * pre-configures Component Model support.
 */
@DisplayName("ComponentEngineConfig Tests")
class ComponentEngineConfigTest {

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("should have componentModelEnabled true by default")
    void shouldHaveComponentModelEnabledByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertTrue(config.isComponentModelEnabled(), "Component model should be enabled by default");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("componentModelEnabled should set and return config for chaining")
    void componentModelEnabledShouldSetAndReturnConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngineConfig result = config.componentModelEnabled(false);

      assertFalse(config.isComponentModelEnabled(), "Component model should be disabled");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("should handle toggling componentModelEnabled")
    void shouldHandleTogglingComponentModelEnabled() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      config.componentModelEnabled(false);
      assertFalse(
          config.isComponentModelEnabled(), "Component model should be disabled after setting");

      config.componentModelEnabled(true);
      assertTrue(
          config.isComponentModelEnabled(), "Component model should be re-enabled after toggling");
    }
  }

  @Nested
  @DisplayName("toEngineConfig Tests")
  class ToEngineConfigTests {

    @Test
    @DisplayName("should have toEngineConfig method returning EngineConfig")
    void shouldHaveToEngineConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineConfig.class.getMethod("toEngineConfig");
      assertNotNull(method, "toEngineConfig method should exist");
      assertEquals(EngineConfig.class, method.getReturnType(), "Should return EngineConfig");
    }

    @Test
    @DisplayName("toEngineConfig should return valid EngineConfig")
    void toEngineConfigShouldReturnValidEngineConfig() {
      final ComponentEngineConfig componentConfig = new ComponentEngineConfig();
      final EngineConfig engineConfig = componentConfig.toEngineConfig();

      assertNotNull(engineConfig, "Should return non-null EngineConfig");
    }

    @Test
    @DisplayName("toEngineConfig should apply componentModelEnabled setting")
    void toEngineConfigShouldApplyComponentModelEnabled() {
      final ComponentEngineConfig enabledConfig = new ComponentEngineConfig();
      enabledConfig.componentModelEnabled(true);
      final EngineConfig engineEnabled = enabledConfig.toEngineConfig();
      assertNotNull(engineEnabled, "EngineConfig from enabled config should not be null");

      final ComponentEngineConfig disabledConfig = new ComponentEngineConfig();
      disabledConfig.componentModelEnabled(false);
      final EngineConfig engineDisabled = disabledConfig.toEngineConfig();
      assertNotNull(engineDisabled, "EngineConfig from disabled config should not be null");
    }
  }
}
