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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineConfig}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentEngineConfig")
class ComponentEngineConfigTest {

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("component model is enabled by default")
    void componentModelEnabledByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertTrue(config.isComponentModelEnabled());
    }
  }

  @Nested
  @DisplayName("componentModelEnabled")
  class ComponentModelEnabled {

    @Test
    @DisplayName("can disable component model")
    void canDisable() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      config.componentModelEnabled(false);
      assertFalse(config.isComponentModelEnabled());
    }

    @Test
    @DisplayName("can re-enable component model")
    void canReEnable() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      config.componentModelEnabled(false);
      config.componentModelEnabled(true);
      assertTrue(config.isComponentModelEnabled());
    }

    @Test
    @DisplayName("supports method chaining")
    void supportsChaining() {
      final ComponentEngineConfig config = new ComponentEngineConfig().componentModelEnabled(true);
      assertNotNull(config);
      assertTrue(config.isComponentModelEnabled());
    }
  }

  @Nested
  @DisplayName("toEngineConfig")
  class ToEngineConfig {

    @Test
    @DisplayName("converts to EngineConfig")
    void convertsToEngineConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final EngineConfig engineConfig = config.toEngineConfig();
      assertNotNull(engineConfig);
    }
  }
}
