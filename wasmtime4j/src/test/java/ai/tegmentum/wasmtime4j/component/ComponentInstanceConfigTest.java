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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentInstanceConfig}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentInstanceConfig")
class ComponentInstanceConfigTest {

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("default values")
    void defaultValues() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      assertEquals(0, config.getMaxMemorySize());
      assertEquals(0, config.getMaxStackSize());
      assertEquals(0, config.getExecutionTimeoutMs());
      assertEquals(ComponentInstanceConfig.SecurityLevel.STRICT, config.getSecurityLevel());
      assertTrue(config.isEnableSandbox());
      assertTrue(config.isStrictValidation());
      assertFalse(config.isAllowHostCalls());
      assertTrue(config.getProperties().isEmpty());
    }
  }

  @Nested
  @DisplayName("fluent setters")
  class FluentSetters {

    @Test
    @DisplayName("sets max memory size")
    void setsMaxMemorySize() {
      final ComponentInstanceConfig config =
          new ComponentInstanceConfig().maxMemorySize(1024 * 1024);
      assertEquals(1024 * 1024, config.getMaxMemorySize());
    }

    @Test
    @DisplayName("sets execution timeout")
    void setsExecutionTimeout() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig().executionTimeout(5000);
      assertEquals(5000, config.getExecutionTimeoutMs());
    }

    @Test
    @DisplayName("sets security level")
    void setsSecurityLevel() {
      final ComponentInstanceConfig config =
          new ComponentInstanceConfig()
              .securityLevel(ComponentInstanceConfig.SecurityLevel.PERMISSIVE);
      assertEquals(ComponentInstanceConfig.SecurityLevel.PERMISSIVE, config.getSecurityLevel());
    }

    @Test
    @DisplayName("sets sandbox")
    void setsSandbox() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig().enableSandbox(false);
      assertFalse(config.isEnableSandbox());
    }

    @Test
    @DisplayName("sets property")
    void setsProperty() {
      final ComponentInstanceConfig config =
          new ComponentInstanceConfig().setProperty("key", "value");
      assertEquals("value", config.getProperties().get("key"));
    }
  }

  @Nested
  @DisplayName("validation")
  class Validation {

    @Test
    @DisplayName("rejects negative memory size")
    void rejectsNegativeMemorySize() {
      assertThrows(
          IllegalArgumentException.class, () -> new ComponentInstanceConfig().maxMemorySize(-1));
    }

    @Test
    @DisplayName("rejects negative timeout")
    void rejectsNegativeTimeout() {
      assertThrows(
          IllegalArgumentException.class, () -> new ComponentInstanceConfig().executionTimeout(-1));
    }

    @Test
    @DisplayName("rejects null security level")
    void rejectsNullSecurityLevel() {
      assertThrows(
          IllegalArgumentException.class, () -> new ComponentInstanceConfig().securityLevel(null));
    }

    @Test
    @DisplayName("rejects null property key")
    void rejectsNullPropertyKey() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentInstanceConfig().setProperty(null, "value"));
    }
  }

  @Nested
  @DisplayName("builder")
  class BuilderTests {

    @Test
    @DisplayName("builder creates config with settings")
    void builderCreatesConfig() {
      final ComponentInstanceConfig config =
          ComponentInstanceConfig.builder()
              .maxMemorySize(2048)
              .executionTimeout(3000)
              .securityLevel(ComponentInstanceConfig.SecurityLevel.STANDARD)
              .enableSandbox(false)
              .setProperty("debug", true)
              .build();
      assertEquals(2048, config.getMaxMemorySize());
      assertEquals(3000, config.getExecutionTimeoutMs());
      assertEquals(ComponentInstanceConfig.SecurityLevel.STANDARD, config.getSecurityLevel());
      assertFalse(config.isEnableSandbox());
      assertEquals(true, config.getProperties().get("debug"));
    }
  }

  @Nested
  @DisplayName("SecurityLevel enum")
  class SecurityLevelTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertEquals(3, ComponentInstanceConfig.SecurityLevel.values().length);
      ComponentInstanceConfig.SecurityLevel.valueOf("PERMISSIVE");
      ComponentInstanceConfig.SecurityLevel.valueOf("STANDARD");
      ComponentInstanceConfig.SecurityLevel.valueOf("STRICT");
    }
  }

  @Nested
  @DisplayName("properties isolation")
  class PropertiesIsolation {

    @Test
    @DisplayName("getProperties returns defensive copy")
    void returnsDefensiveCopy() {
      final ComponentInstanceConfig config =
          new ComponentInstanceConfig().setProperty("key", "value");
      config.getProperties().put("hack", "should not persist");
      assertFalse(config.getProperties().containsKey("hack"));
    }
  }
}
