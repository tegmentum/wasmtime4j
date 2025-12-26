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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultExceptionHandlingConfig} class.
 *
 * <p>DefaultExceptionHandlingConfig provides the default implementation of
 * ExceptionHandler.ExceptionHandlingConfig.
 */
@DisplayName("DefaultExceptionHandlingConfig Tests")
class DefaultExceptionHandlingConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(DefaultExceptionHandlingConfig.class.getModifiers()),
          "DefaultExceptionHandlingConfig should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(DefaultExceptionHandlingConfig.class.getModifiers()),
          "DefaultExceptionHandlingConfig should be public");
    }

    @Test
    @DisplayName("should implement ExceptionHandlingConfig interface")
    void shouldImplementExceptionHandlingConfigInterface() {
      assertTrue(
          ExceptionHandler.ExceptionHandlingConfig.class.isAssignableFrom(
              DefaultExceptionHandlingConfig.class),
          "DefaultExceptionHandlingConfig should implement ExceptionHandlingConfig");
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("should have DEFAULT_MAX_UNWIND_DEPTH constant")
    void shouldHaveDefaultMaxUnwindDepthConstant() {
      assertEquals(
          1000,
          DefaultExceptionHandlingConfig.DEFAULT_MAX_UNWIND_DEPTH,
          "DEFAULT_MAX_UNWIND_DEPTH should be 1000");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have builder factory method")
    void shouldHaveBuilderFactoryMethod() {
      final DefaultExceptionHandlingConfig.ConfigBuilder builder =
          DefaultExceptionHandlingConfig.builder();
      assertNotNull(builder, "builder() should return a builder");
    }

    @Test
    @DisplayName("should have getDefault factory method")
    void shouldHaveGetDefaultFactoryMethod() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertNotNull(config, "getDefault() should return a config");
    }
  }

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("default config should have nested try/catch enabled")
    void defaultConfigShouldHaveNestedTryCatchEnabled() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(config.isNestedTryCatchEnabled(), "Nested try/catch should be enabled by default");
    }

    @Test
    @DisplayName("default config should have exception unwinding enabled")
    void defaultConfigShouldHaveExceptionUnwindingEnabled() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isExceptionUnwindingEnabled(), "Exception unwinding should be enabled by default");
    }

    @Test
    @DisplayName("default config should have default max unwind depth")
    void defaultConfigShouldHaveDefaultMaxUnwindDepth() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(
          DefaultExceptionHandlingConfig.DEFAULT_MAX_UNWIND_DEPTH,
          config.getMaxUnwindDepth(),
          "Max unwind depth should be default value");
    }

    @Test
    @DisplayName("default config should have type validation enabled")
    void defaultConfigShouldHaveTypeValidationEnabled() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isExceptionTypeValidationEnabled(),
          "Type validation should be enabled by default");
    }

    @Test
    @DisplayName("default config should have stack traces enabled")
    void defaultConfigShouldHaveStackTracesEnabled() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(config.isStackTracesEnabled(), "Stack traces should be enabled by default");
    }

    @Test
    @DisplayName("default config should have exception propagation enabled")
    void defaultConfigShouldHaveExceptionPropagationEnabled() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isExceptionPropagationEnabled(),
          "Exception propagation should be enabled by default");
    }

    @Test
    @DisplayName("default config should have GC integration disabled")
    void defaultConfigShouldHaveGcIntegrationDisabled() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertFalse(config.isGcIntegrationEnabled(), "GC integration should be disabled by default");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create config with custom nested try/catch")
    void builderShouldCreateConfigWithCustomNestedTryCatch() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().nestedTryCatch(false).build();
      assertFalse(config.isNestedTryCatchEnabled(), "Nested try/catch should be disabled");
    }

    @Test
    @DisplayName("builder should create config with custom exception unwinding")
    void builderShouldCreateConfigWithCustomExceptionUnwinding() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().exceptionUnwinding(false).build();
      assertFalse(config.isExceptionUnwindingEnabled(), "Exception unwinding should be disabled");
    }

    @Test
    @DisplayName("builder should create config with custom max unwind depth")
    void builderShouldCreateConfigWithCustomMaxUnwindDepth() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().maxUnwindDepth(500).build();
      assertEquals(500, config.getMaxUnwindDepth(), "Max unwind depth should be custom value");
    }

    @Test
    @DisplayName("builder should throw on negative max unwind depth")
    void builderShouldThrowOnNegativeMaxUnwindDepth() {
      final DefaultExceptionHandlingConfig.ConfigBuilder builder =
          DefaultExceptionHandlingConfig.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.maxUnwindDepth(-1),
          "Should throw on negative max unwind depth");
    }

    @Test
    @DisplayName("builder should create config with custom type validation")
    void builderShouldCreateConfigWithCustomTypeValidation() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().typeValidation(false).build();
      assertFalse(config.isExceptionTypeValidationEnabled(), "Type validation should be disabled");
    }

    @Test
    @DisplayName("builder should create config with custom stack traces")
    void builderShouldCreateConfigWithCustomStackTraces() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().stackTraces(false).build();
      assertFalse(config.isStackTracesEnabled(), "Stack traces should be disabled");
    }

    @Test
    @DisplayName("builder should create config with custom exception propagation")
    void builderShouldCreateConfigWithCustomExceptionPropagation() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().exceptionPropagation(false).build();
      assertFalse(
          config.isExceptionPropagationEnabled(), "Exception propagation should be disabled");
    }

    @Test
    @DisplayName("builder should create config with custom GC integration")
    void builderShouldCreateConfigWithCustomGcIntegration() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().gcIntegration(true).build();
      assertTrue(config.isGcIntegrationEnabled(), "GC integration should be enabled");
    }

    @Test
    @DisplayName("builder should support method chaining")
    void builderShouldSupportMethodChaining() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder()
              .nestedTryCatch(false)
              .exceptionUnwinding(false)
              .maxUnwindDepth(100)
              .typeValidation(false)
              .stackTraces(false)
              .exceptionPropagation(false)
              .gcIntegration(true)
              .build();

      assertFalse(config.isNestedTryCatchEnabled());
      assertFalse(config.isExceptionUnwindingEnabled());
      assertEquals(100, config.getMaxUnwindDepth());
      assertFalse(config.isExceptionTypeValidationEnabled());
      assertFalse(config.isStackTracesEnabled());
      assertFalse(config.isExceptionPropagationEnabled());
      assertTrue(config.isGcIntegrationEnabled());
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(config, config, "Config should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to config with same values")
    void shouldBeEqualToConfigWithSameValues() {
      final DefaultExceptionHandlingConfig config1 = DefaultExceptionHandlingConfig.getDefault();
      final DefaultExceptionHandlingConfig config2 = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(config1, config2, "Configs with same values should be equal");
    }

    @Test
    @DisplayName("should not be equal to config with different values")
    void shouldNotBeEqualToConfigWithDifferentValues() {
      final DefaultExceptionHandlingConfig config1 = DefaultExceptionHandlingConfig.getDefault();
      final DefaultExceptionHandlingConfig config2 =
          DefaultExceptionHandlingConfig.builder().nestedTryCatch(false).build();
      assertNotEquals(config1, config2, "Configs with different values should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertNotEquals(null, config, "Config should not be equal to null");
    }

    @Test
    @DisplayName("should have consistent hash code for equal configs")
    void shouldHaveConsistentHashCodeForEqualConfigs() {
      final DefaultExceptionHandlingConfig config1 = DefaultExceptionHandlingConfig.getDefault();
      final DefaultExceptionHandlingConfig config2 = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(
          config1.hashCode(), config2.hashCode(), "Equal configs should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should not be null")
    void toStringShouldNotBeNull() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertNotNull(config.toString(), "toString should not be null");
    }

    @Test
    @DisplayName("toString should contain class name")
    void toStringShouldContainClassName() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.toString().contains("DefaultExceptionHandlingConfig"),
          "toString should contain class name");
    }

    @Test
    @DisplayName("toString should contain configuration values")
    void toStringShouldContainConfigurationValues() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      final String str = config.toString();
      assertTrue(str.contains("nestedTryCatch"), "toString should contain nestedTryCatch");
      assertTrue(str.contains("exceptionUnwinding"), "toString should contain exceptionUnwinding");
      assertTrue(str.contains("maxUnwindDepth"), "toString should contain maxUnwindDepth");
    }
  }

  @Nested
  @DisplayName("ConfigBuilder Implementation Tests")
  class ConfigBuilderImplementationTests {

    @Test
    @DisplayName("ConfigBuilder should implement Builder interface")
    void configBuilderShouldImplementBuilderInterface() {
      final DefaultExceptionHandlingConfig.ConfigBuilder builder =
          DefaultExceptionHandlingConfig.builder();
      assertTrue(
          builder instanceof ExceptionHandler.ExceptionHandlingConfig.Builder,
          "ConfigBuilder should implement Builder interface");
    }
  }
}
