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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultExceptionHandlingConfig} - builder pattern config for exception handling.
 *
 * <p>Validates default values, builder customization, equals/hashCode, and toString.
 */
@DisplayName("DefaultExceptionHandlingConfig Tests")
class DefaultExceptionHandlingConfigTest {

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("getDefault should return non-null config")
    void getDefaultShouldReturnNonNullConfig() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertNotNull(config, "getDefault() should return a non-null config");
    }

    @Test
    @DisplayName("default nestedTryCatch should be true")
    void defaultNestedTryCatchShouldBeTrue() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isNestedTryCatchEnabled(),
          "Default nestedTryCatch should be true");
    }

    @Test
    @DisplayName("default exceptionUnwinding should be true")
    void defaultExceptionUnwindingShouldBeTrue() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isExceptionUnwindingEnabled(),
          "Default exceptionUnwinding should be true");
    }

    @Test
    @DisplayName("default maxUnwindDepth should be 1000")
    void defaultMaxUnwindDepthShouldBe1000() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(
          1000,
          config.getMaxUnwindDepth(),
          "Default maxUnwindDepth should be 1000");
    }

    @Test
    @DisplayName("DEFAULT_MAX_UNWIND_DEPTH constant should be 1000")
    void defaultMaxUnwindDepthConstantShouldBe1000() {
      assertEquals(
          1000,
          DefaultExceptionHandlingConfig.DEFAULT_MAX_UNWIND_DEPTH,
          "DEFAULT_MAX_UNWIND_DEPTH should be 1000");
    }

    @Test
    @DisplayName("default typeValidation should be true")
    void defaultTypeValidationShouldBeTrue() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isExceptionTypeValidationEnabled(),
          "Default typeValidation should be true");
    }

    @Test
    @DisplayName("default stackTraces should be true")
    void defaultStackTracesShouldBeTrue() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isStackTracesEnabled(),
          "Default stackTraces should be true");
    }

    @Test
    @DisplayName("default exceptionPropagation should be true")
    void defaultExceptionPropagationShouldBeTrue() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config.isExceptionPropagationEnabled(),
          "Default exceptionPropagation should be true");
    }

    @Test
    @DisplayName("default gcIntegration should be false")
    void defaultGcIntegrationShouldBeFalse() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertFalse(
          config.isGcIntegrationEnabled(),
          "Default gcIntegration should be false");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should return non-null builder")
    void builderShouldReturnNonNullBuilder() {
      final DefaultExceptionHandlingConfig.ConfigBuilder builder =
          DefaultExceptionHandlingConfig.builder();
      assertNotNull(builder, "builder() should return non-null builder");
    }

    @Test
    @DisplayName("builder should allow disabling nestedTryCatch")
    void builderShouldAllowDisablingNestedTryCatch() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().nestedTryCatch(false).build();
      assertFalse(
          config.isNestedTryCatchEnabled(),
          "nestedTryCatch should be disabled when set to false");
    }

    @Test
    @DisplayName("builder should allow disabling exceptionUnwinding")
    void builderShouldAllowDisablingExceptionUnwinding() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().exceptionUnwinding(false).build();
      assertFalse(
          config.isExceptionUnwindingEnabled(),
          "exceptionUnwinding should be disabled when set to false");
    }

    @Test
    @DisplayName("builder should allow setting custom maxUnwindDepth")
    void builderShouldAllowSettingCustomMaxUnwindDepth() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().maxUnwindDepth(500).build();
      assertEquals(
          500,
          config.getMaxUnwindDepth(),
          "maxUnwindDepth should be 500 when set to 500");
    }

    @Test
    @DisplayName("builder should reject negative maxUnwindDepth")
    void builderShouldRejectNegativeMaxUnwindDepth() {
      final DefaultExceptionHandlingConfig.ConfigBuilder builder =
          DefaultExceptionHandlingConfig.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.maxUnwindDepth(-1),
          "builder should reject negative maxUnwindDepth");
    }

    @Test
    @DisplayName("builder should allow zero maxUnwindDepth")
    void builderShouldAllowZeroMaxUnwindDepth() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().maxUnwindDepth(0).build();
      assertEquals(
          0,
          config.getMaxUnwindDepth(),
          "maxUnwindDepth should be 0 when set to 0");
    }

    @Test
    @DisplayName("builder should allow disabling typeValidation")
    void builderShouldAllowDisablingTypeValidation() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().typeValidation(false).build();
      assertFalse(
          config.isExceptionTypeValidationEnabled(),
          "typeValidation should be disabled when set to false");
    }

    @Test
    @DisplayName("builder should allow disabling stackTraces")
    void builderShouldAllowDisablingStackTraces() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().stackTraces(false).build();
      assertFalse(
          config.isStackTracesEnabled(),
          "stackTraces should be disabled when set to false");
    }

    @Test
    @DisplayName("builder should allow disabling exceptionPropagation")
    void builderShouldAllowDisablingExceptionPropagation() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().exceptionPropagation(false).build();
      assertFalse(
          config.isExceptionPropagationEnabled(),
          "exceptionPropagation should be disabled when set to false");
    }

    @Test
    @DisplayName("builder should allow enabling gcIntegration")
    void builderShouldAllowEnablingGcIntegration() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder().gcIntegration(true).build();
      assertTrue(
          config.isGcIntegrationEnabled(),
          "gcIntegration should be enabled when set to true");
    }

    @Test
    @DisplayName("builder should support fluent chaining")
    void builderShouldSupportFluentChaining() {
      final DefaultExceptionHandlingConfig config =
          DefaultExceptionHandlingConfig.builder()
              .nestedTryCatch(false)
              .exceptionUnwinding(false)
              .maxUnwindDepth(50)
              .typeValidation(false)
              .stackTraces(false)
              .exceptionPropagation(false)
              .gcIntegration(true)
              .build();

      assertFalse(config.isNestedTryCatchEnabled(), "nestedTryCatch should be false");
      assertFalse(config.isExceptionUnwindingEnabled(), "exceptionUnwinding should be false");
      assertEquals(50, config.getMaxUnwindDepth(), "maxUnwindDepth should be 50");
      assertFalse(config.isExceptionTypeValidationEnabled(), "typeValidation should be false");
      assertFalse(config.isStackTracesEnabled(), "stackTraces should be false");
      assertFalse(config.isExceptionPropagationEnabled(), "exceptionPropagation should be false");
      assertTrue(config.isGcIntegrationEnabled(), "gcIntegration should be true");
    }
  }

  @Nested
  @DisplayName("ExceptionHandlingConfig Interface Conformance Tests")
  class InterfaceConformanceTests {

    @Test
    @DisplayName("should implement ExceptionHandlingConfig interface")
    void shouldImplementExceptionHandlingConfigInterface() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertTrue(
          config instanceof ExceptionHandler.ExceptionHandlingConfig,
          "Should implement ExceptionHandler.ExceptionHandlingConfig");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for configs with same values")
    void equalsShouldReturnTrueForSameValues() {
      final DefaultExceptionHandlingConfig a = DefaultExceptionHandlingConfig.getDefault();
      final DefaultExceptionHandlingConfig b = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(a, b, "Configs with same default values should be equal");
    }

    @Test
    @DisplayName("equals should return false for configs with different values")
    void equalsShouldReturnFalseForDifferentValues() {
      final DefaultExceptionHandlingConfig a = DefaultExceptionHandlingConfig.getDefault();
      final DefaultExceptionHandlingConfig b =
          DefaultExceptionHandlingConfig.builder().gcIntegration(true).build();
      assertNotEquals(a, b, "Configs with different values should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertNotEquals(null, config, "Config should not equal null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal configs")
    void hashCodeShouldBeConsistent() {
      final DefaultExceptionHandlingConfig a = DefaultExceptionHandlingConfig.getDefault();
      final DefaultExceptionHandlingConfig b = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(a.hashCode(), b.hashCode(), "Equal configs should have same hashCode");
    }

    @Test
    @DisplayName("equals should be reflexive")
    void equalsShouldBeReflexive() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertEquals(config, config, "Config should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should not be null")
    void toStringShouldNotBeNull() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      assertNotNull(config.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("toString should contain class name")
    void toStringShouldContainClassName() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      final String str = config.toString();
      assertTrue(
          str.contains("DefaultExceptionHandlingConfig"),
          "toString should contain class name, got: " + str);
    }

    @Test
    @DisplayName("toString should contain field values")
    void toStringShouldContainFieldValues() {
      final DefaultExceptionHandlingConfig config = DefaultExceptionHandlingConfig.getDefault();
      final String str = config.toString();
      assertTrue(str.contains("nestedTryCatch=true"), "toString should contain nestedTryCatch");
      assertTrue(str.contains("maxUnwindDepth=1000"), "toString should contain maxUnwindDepth");
      assertTrue(str.contains("gcIntegration=false"), "toString should contain gcIntegration");
    }
  }
}
