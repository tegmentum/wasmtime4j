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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentInstanceConfig} class.
 *
 * <p>ComponentInstanceConfig provides configuration options for WebAssembly component instance
 * creation including resource limits, security settings, and runtime behavior.
 */
@DisplayName("ComponentInstanceConfig Tests")
class ComponentInstanceConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentInstanceConfig.class.getModifiers()),
          "ComponentInstanceConfig should be public");
      assertTrue(
          Modifier.isFinal(ComponentInstanceConfig.class.getModifiers()),
          "ComponentInstanceConfig should be final");
    }

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHavePublicNoArgConstructor() throws NoSuchMethodException {
      final var constructor = ComponentInstanceConfig.class.getConstructor();
      assertNotNull(constructor, "No-arg constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentInstanceConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          ComponentInstanceConfig.ComponentInstanceConfigBuilder.class,
          method.getReturnType(),
          "builder should return ComponentInstanceConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create config with default values")
    void shouldCreateConfigWithDefaultValues() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();

      assertEquals(
          0L, config.getMaxMemorySize(), "Default max memory size should be 0 (unlimited)");
      assertEquals(0L, config.getMaxStackSize(), "Default max stack size should be 0 (unlimited)");
      assertEquals(
          0L, config.getExecutionTimeoutMs(), "Default execution timeout should be 0 (no timeout)");
      assertEquals(
          ComponentInstanceConfig.SecurityLevel.STRICT,
          config.getSecurityLevel(),
          "Default security level should be STRICT");
      assertTrue(config.isEnableSandbox(), "Default sandbox should be enabled");
      assertTrue(config.isStrictValidation(), "Default strict validation should be enabled");
      assertFalse(config.isAllowHostCalls(), "Default allow host calls should be false");
      assertNotNull(config.getProperties(), "Properties should not be null");
      assertTrue(config.getProperties().isEmpty(), "Default properties should be empty");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("maxMemorySize should set and return correct value")
    void maxMemorySizeShouldSetCorrectValue() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      final ComponentInstanceConfig result = config.maxMemorySize(1024 * 1024);

      assertEquals(1024 * 1024, config.getMaxMemorySize(), "Max memory size should be set");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("maxMemorySize should reject negative values")
    void maxMemorySizeShouldRejectNegativeValues() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.maxMemorySize(-1),
          "Negative memory size should be rejected");
    }

    @Test
    @DisplayName("executionTimeout should set and return correct value")
    void executionTimeoutShouldSetCorrectValue() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      final ComponentInstanceConfig result = config.executionTimeout(5000);

      assertEquals(5000, config.getExecutionTimeoutMs(), "Execution timeout should be set");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("executionTimeout should reject negative values")
    void executionTimeoutShouldRejectNegativeValues() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.executionTimeout(-1),
          "Negative timeout should be rejected");
    }

    @Test
    @DisplayName("securityLevel should set and return correct value")
    void securityLevelShouldSetCorrectValue() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      final ComponentInstanceConfig result =
          config.securityLevel(ComponentInstanceConfig.SecurityLevel.PERMISSIVE);

      assertEquals(
          ComponentInstanceConfig.SecurityLevel.PERMISSIVE,
          config.getSecurityLevel(),
          "Security level should be set");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("securityLevel should reject null")
    void securityLevelShouldRejectNull() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.securityLevel(null),
          "Null security level should be rejected");
    }

    @Test
    @DisplayName("enableSandbox should set and return correct value")
    void enableSandboxShouldSetCorrectValue() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      final ComponentInstanceConfig result = config.enableSandbox(false);

      assertFalse(config.isEnableSandbox(), "Sandbox should be disabled");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("setProperty should set and return correct value")
    void setPropertyShouldSetCorrectValue() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      final ComponentInstanceConfig result = config.setProperty("key1", "value1");

      assertEquals("value1", config.getProperties().get("key1"), "Property should be set");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("setProperty should reject null key")
    void setPropertyShouldRejectNullKey() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setProperty(null, "value"),
          "Null key should be rejected");
    }

    @Test
    @DisplayName("getProperties should return defensive copy")
    void getPropertiesShouldReturnDefensiveCopy() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      config.setProperty("key1", "value1");

      final var properties = config.getProperties();
      properties.put("key2", "value2");

      assertFalse(
          config.getProperties().containsKey("key2"),
          "Modifying returned map should not affect config");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create config with defaults")
    void builderShouldCreateConfigWithDefaults() {
      final ComponentInstanceConfig config = ComponentInstanceConfig.builder().build();

      assertNotNull(config, "Builder should create config");
      assertEquals(0L, config.getMaxMemorySize(), "Default max memory size should be 0");
      assertEquals(
          ComponentInstanceConfig.SecurityLevel.STRICT,
          config.getSecurityLevel(),
          "Default security level should be STRICT");
    }

    @Test
    @DisplayName("builder should chain methods correctly")
    void builderShouldChainMethodsCorrectly() {
      final ComponentInstanceConfig config =
          ComponentInstanceConfig.builder()
              .maxMemorySize(2048)
              .executionTimeout(1000)
              .securityLevel(ComponentInstanceConfig.SecurityLevel.STANDARD)
              .enableSandbox(true)
              .setProperty("debug", true)
              .build();

      assertEquals(2048, config.getMaxMemorySize(), "Max memory size should be set");
      assertEquals(1000, config.getExecutionTimeoutMs(), "Execution timeout should be set");
      assertEquals(
          ComponentInstanceConfig.SecurityLevel.STANDARD,
          config.getSecurityLevel(),
          "Security level should be STANDARD");
      assertTrue(config.isEnableSandbox(), "Sandbox should be enabled");
      assertEquals(true, config.getProperties().get("debug"), "Debug property should be set");
    }
  }

  @Nested
  @DisplayName("SecurityLevel Enum Tests")
  class SecurityLevelEnumTests {

    @Test
    @DisplayName("should have PERMISSIVE level")
    void shouldHavePermissiveLevel() {
      final var level = ComponentInstanceConfig.SecurityLevel.PERMISSIVE;
      assertNotNull(level, "PERMISSIVE level should exist");
    }

    @Test
    @DisplayName("should have STANDARD level")
    void shouldHaveStandardLevel() {
      final var level = ComponentInstanceConfig.SecurityLevel.STANDARD;
      assertNotNull(level, "STANDARD level should exist");
    }

    @Test
    @DisplayName("should have STRICT level")
    void shouldHaveStrictLevel() {
      final var level = ComponentInstanceConfig.SecurityLevel.STRICT;
      assertNotNull(level, "STRICT level should exist");
    }

    @Test
    @DisplayName("should have exactly 3 levels")
    void shouldHaveExactlyThreeLevels() {
      assertEquals(
          3,
          ComponentInstanceConfig.SecurityLevel.values().length,
          "Should have exactly 3 security levels");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle zero memory size")
    void shouldHandleZeroMemorySize() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      config.maxMemorySize(0);

      assertEquals(0L, config.getMaxMemorySize(), "Zero memory size should be valid (unlimited)");
    }

    @Test
    @DisplayName("should handle zero timeout")
    void shouldHandleZeroTimeout() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      config.executionTimeout(0);

      assertEquals(0L, config.getExecutionTimeoutMs(), "Zero timeout should be valid (no timeout)");
    }

    @Test
    @DisplayName("should handle max long values")
    void shouldHandleMaxLongValues() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      config.maxMemorySize(Long.MAX_VALUE);
      config.executionTimeout(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, config.getMaxMemorySize(), "Max long value should be accepted");
      assertEquals(
          Long.MAX_VALUE, config.getExecutionTimeoutMs(), "Max long value should be accepted");
    }

    @Test
    @DisplayName("should handle multiple property settings")
    void shouldHandleMultiplePropertySettings() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      config.setProperty("key1", "value1");
      config.setProperty("key2", 42);
      config.setProperty("key3", true);

      assertEquals(3, config.getProperties().size(), "Should have 3 properties");
      assertEquals("value1", config.getProperties().get("key1"), "String property should be set");
      assertEquals(42, config.getProperties().get("key2"), "Integer property should be set");
      assertEquals(true, config.getProperties().get("key3"), "Boolean property should be set");
    }

    @Test
    @DisplayName("should allow overwriting properties")
    void shouldAllowOverwritingProperties() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      config.setProperty("key1", "value1");
      config.setProperty("key1", "value2");

      assertEquals("value2", config.getProperties().get("key1"), "Property should be overwritten");
    }

    @Test
    @DisplayName("should allow null property values")
    void shouldAllowNullPropertyValues() {
      final ComponentInstanceConfig config = new ComponentInstanceConfig();
      config.setProperty("key1", null);

      assertTrue(config.getProperties().containsKey("key1"), "Key should exist");
      assertEquals(null, config.getProperties().get("key1"), "Value should be null");
    }
  }
}
