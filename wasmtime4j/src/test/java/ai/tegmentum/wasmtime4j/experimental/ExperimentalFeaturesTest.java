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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExperimentalFeatures} - experimental WebAssembly feature flags.
 *
 * <p>Validates feature enable/disable, query methods, Feature enum, and reset behavior.
 */
@DisplayName("ExperimentalFeatures Tests")
class ExperimentalFeaturesTest {

  @AfterEach
  void resetFeatures() {
    ExperimentalFeatures.reset();
  }

  @Nested
  @DisplayName("Feature Enum Tests")
  class FeatureEnumTests {

    @Test
    @DisplayName("Feature enum should have expected values")
    void featureEnumShouldHaveExpectedValues() {
      final ExperimentalFeatures.Feature[] features = ExperimentalFeatures.Feature.values();
      assertEquals(5, features.length, "Feature enum should have exactly 5 values");
    }

    @Test
    @DisplayName("EXCEPTION_HANDLING should have correct system property")
    void exceptionHandlingShouldHaveCorrectSystemProperty() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING;
      assertEquals(
          "wasmtime4j.experimental.exceptions",
          feature.getSystemProperty(),
          "EXCEPTION_HANDLING system property should be correct");
    }

    @Test
    @DisplayName("ADVANCED_SIMD should have correct system property")
    void advancedSimdShouldHaveCorrectSystemProperty() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.ADVANCED_SIMD;
      assertEquals(
          "wasmtime4j.experimental.simd",
          feature.getSystemProperty(),
          "ADVANCED_SIMD system property should be correct");
    }

    @Test
    @DisplayName("MULTI_VALUE should have correct system property")
    void multiValueShouldHaveCorrectSystemProperty() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.MULTI_VALUE;
      assertEquals(
          "wasmtime4j.experimental.multivalue",
          feature.getSystemProperty(),
          "MULTI_VALUE system property should be correct");
    }

    @Test
    @DisplayName("REFERENCE_TYPES_EXTENDED should have correct system property")
    void referenceTypesExtendedShouldHaveCorrectSystemProperty() {
      final ExperimentalFeatures.Feature feature =
          ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED;
      assertEquals(
          "wasmtime4j.experimental.reftypes",
          feature.getSystemProperty(),
          "REFERENCE_TYPES_EXTENDED system property should be correct");
    }

    @Test
    @DisplayName("RELAXED_SIMD should have correct system property")
    void relaxedSimdShouldHaveCorrectSystemProperty() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.RELAXED_SIMD;
      assertEquals(
          "wasmtime4j.experimental.relaxed_simd",
          feature.getSystemProperty(),
          "RELAXED_SIMD system property should be correct");
    }

    @Test
    @DisplayName("all features should default to disabled")
    void allFeaturesShouldDefaultToDisabled() {
      for (final ExperimentalFeatures.Feature feature : ExperimentalFeatures.Feature.values()) {
        assertFalse(
            feature.isDefaultEnabled(),
            "Feature " + feature.name() + " should be disabled by default");
      }
    }

    @Test
    @DisplayName("valueOf should work for all feature names")
    void valueOfShouldWorkForAllFeatureNames() {
      assertDoesNotThrow(
          () -> ExperimentalFeatures.Feature.valueOf("EXCEPTION_HANDLING"),
          "valueOf should work for EXCEPTION_HANDLING");
      assertDoesNotThrow(
          () -> ExperimentalFeatures.Feature.valueOf("ADVANCED_SIMD"),
          "valueOf should work for ADVANCED_SIMD");
      assertDoesNotThrow(
          () -> ExperimentalFeatures.Feature.valueOf("MULTI_VALUE"),
          "valueOf should work for MULTI_VALUE");
      assertDoesNotThrow(
          () -> ExperimentalFeatures.Feature.valueOf("REFERENCE_TYPES_EXTENDED"),
          "valueOf should work for REFERENCE_TYPES_EXTENDED");
      assertDoesNotThrow(
          () -> ExperimentalFeatures.Feature.valueOf("RELAXED_SIMD"),
          "valueOf should work for RELAXED_SIMD");
    }
  }

  @Nested
  @DisplayName("Enable and Disable Feature Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("enableFeature should enable a disabled feature")
    void enableFeatureShouldEnableDisabledFeature() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING;
      assertFalse(
          ExperimentalFeatures.isFeatureEnabled(feature), "Feature should be disabled initially");
      ExperimentalFeatures.enableFeature(feature);
      assertTrue(
          ExperimentalFeatures.isFeatureEnabled(feature),
          "Feature should be enabled after enableFeature()");
    }

    @Test
    @DisplayName("disableFeature should disable an enabled feature")
    void disableFeatureShouldDisableEnabledFeature() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.ADVANCED_SIMD;
      ExperimentalFeatures.enableFeature(feature);
      assertTrue(
          ExperimentalFeatures.isFeatureEnabled(feature),
          "Feature should be enabled after enableFeature()");
      ExperimentalFeatures.disableFeature(feature);
      assertFalse(
          ExperimentalFeatures.isFeatureEnabled(feature),
          "Feature should be disabled after disableFeature()");
    }

    @Test
    @DisplayName("enableFeature should reject null")
    void enableFeatureShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.enableFeature(null),
          "enableFeature should reject null feature");
    }

    @Test
    @DisplayName("disableFeature should reject null")
    void disableFeatureShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.disableFeature(null),
          "disableFeature should reject null feature");
    }
  }

  @Nested
  @DisplayName("IsFeatureEnabled Tests")
  class IsFeatureEnabledTests {

    @Test
    @DisplayName("isFeatureEnabled should return false for disabled features")
    void isFeatureEnabledShouldReturnFalseForDisabledFeatures() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.MULTI_VALUE;
      assertFalse(
          ExperimentalFeatures.isFeatureEnabled(feature), "Disabled feature should return false");
    }

    @Test
    @DisplayName("isFeatureEnabled should return true for enabled features")
    void isFeatureEnabledShouldReturnTrueForEnabledFeatures() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.MULTI_VALUE;
      ExperimentalFeatures.enableFeature(feature);
      assertTrue(
          ExperimentalFeatures.isFeatureEnabled(feature), "Enabled feature should return true");
    }

    @Test
    @DisplayName("isFeatureEnabled should reject null")
    void isFeatureEnabledShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.isFeatureEnabled(null),
          "isFeatureEnabled should reject null feature");
    }
  }

  @Nested
  @DisplayName("GetEnabledFeatures Tests")
  class GetEnabledFeaturesTests {

    @Test
    @DisplayName("getEnabledFeatures should return empty set when none enabled")
    void getEnabledFeaturesShouldReturnEmptySetWhenNoneEnabled() {
      final Set<ExperimentalFeatures.Feature> enabled = ExperimentalFeatures.getEnabledFeatures();
      assertNotNull(enabled, "getEnabledFeatures should not return null");
      assertTrue(enabled.isEmpty(), "No features should be enabled by default");
    }

    @Test
    @DisplayName("getEnabledFeatures should return enabled features")
    void getEnabledFeaturesShouldReturnEnabledFeatures() {
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.RELAXED_SIMD);

      final Set<ExperimentalFeatures.Feature> enabled = ExperimentalFeatures.getEnabledFeatures();
      assertEquals(2, enabled.size(), "Should have exactly 2 enabled features");
      assertTrue(
          enabled.contains(ExperimentalFeatures.Feature.EXCEPTION_HANDLING),
          "Should contain EXCEPTION_HANDLING");
      assertTrue(
          enabled.contains(ExperimentalFeatures.Feature.RELAXED_SIMD),
          "Should contain RELAXED_SIMD");
    }
  }

  @Nested
  @DisplayName("HasEnabledFeatures Tests")
  class HasEnabledFeaturesTests {

    @Test
    @DisplayName("hasEnabledFeatures should return false when none enabled")
    void hasEnabledFeaturesShouldReturnFalseWhenNoneEnabled() {
      assertFalse(
          ExperimentalFeatures.hasEnabledFeatures(),
          "Should return false when no features are enabled");
    }

    @Test
    @DisplayName("hasEnabledFeatures should return true when features enabled")
    void hasEnabledFeaturesShouldReturnTrueWhenFeaturesEnabled() {
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);
      assertTrue(
          ExperimentalFeatures.hasEnabledFeatures(),
          "Should return true when at least one feature is enabled");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear all enabled features")
    void resetShouldClearAllEnabledFeatures() {
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.MULTI_VALUE);

      assertTrue(
          ExperimentalFeatures.hasEnabledFeatures(), "Features should be enabled before reset");

      ExperimentalFeatures.reset();

      assertFalse(
          ExperimentalFeatures.hasEnabledFeatures(), "No features should be enabled after reset");
    }

    @Test
    @DisplayName("reset should allow re-enabling features")
    void resetShouldAllowReEnablingFeatures() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING;
      ExperimentalFeatures.enableFeature(feature);
      ExperimentalFeatures.reset();
      assertFalse(
          ExperimentalFeatures.isFeatureEnabled(feature), "Feature should be disabled after reset");

      ExperimentalFeatures.enableFeature(feature);
      assertTrue(
          ExperimentalFeatures.isFeatureEnabled(feature),
          "Feature should be re-enableable after reset");
    }
  }

  @Nested
  @DisplayName("ValidateFeatureSupport Tests")
  class ValidateFeatureSupportTests {

    @Test
    @DisplayName("validateFeatureSupport should throw for disabled feature")
    void validateFeatureSupportShouldThrowForDisabledFeature() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING;
      final UnsupportedOperationException ex =
          assertThrows(
              UnsupportedOperationException.class,
              () -> ExperimentalFeatures.validateFeatureSupport(feature),
              "Should throw for disabled feature");
      assertNotNull(ex.getMessage(), "Exception should have a message");
      assertTrue(
          ex.getMessage().contains(feature.getSystemProperty()),
          "Exception should reference the system property");
    }

    @Test
    @DisplayName("validateFeatureSupport should not throw for enabled feature")
    void validateFeatureSupportShouldNotThrowForEnabledFeature() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING;
      ExperimentalFeatures.enableFeature(feature);
      assertDoesNotThrow(
          () -> ExperimentalFeatures.validateFeatureSupport(feature),
          "Should not throw for enabled feature");
    }

    @Test
    @DisplayName("validateFeatureSupport should reject null")
    void validateFeatureSupportShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.validateFeatureSupport(null),
          "validateFeatureSupport should reject null feature");
    }
  }

  @Nested
  @DisplayName("Utility Class Tests")
  class UtilityClassTests {

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ExperimentalFeatures.class.getModifiers()),
          "ExperimentalFeatures should be a final class");
    }
  }
}
