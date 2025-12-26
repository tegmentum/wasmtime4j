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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExperimentalFeatures} class.
 *
 * <p>ExperimentalFeatures provides experimental WebAssembly feature flags and configuration.
 */
@DisplayName("ExperimentalFeatures Tests")
class ExperimentalFeaturesTest {

  @AfterEach
  void tearDown() {
    ExperimentalFeatures.reset();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ExperimentalFeatures.class.getModifiers()),
          "ExperimentalFeatures should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExperimentalFeatures.class.getModifiers()),
          "ExperimentalFeatures should be public");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = ExperimentalFeatures.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private for utility class");
    }
  }

  @Nested
  @DisplayName("Feature Enum Tests")
  class FeatureEnumTests {

    @Test
    @DisplayName("should have EXCEPTION_HANDLING feature")
    void shouldHaveExceptionHandlingFeature() {
      assertNotNull(
          ExperimentalFeatures.Feature.valueOf("EXCEPTION_HANDLING"),
          "Should have EXCEPTION_HANDLING enum value");
    }

    @Test
    @DisplayName("should have ADVANCED_SIMD feature")
    void shouldHaveAdvancedSimdFeature() {
      assertNotNull(
          ExperimentalFeatures.Feature.valueOf("ADVANCED_SIMD"),
          "Should have ADVANCED_SIMD enum value");
    }

    @Test
    @DisplayName("should have MULTI_VALUE feature")
    void shouldHaveMultiValueFeature() {
      assertNotNull(
          ExperimentalFeatures.Feature.valueOf("MULTI_VALUE"),
          "Should have MULTI_VALUE enum value");
    }

    @Test
    @DisplayName("should have REFERENCE_TYPES_EXTENDED feature")
    void shouldHaveReferenceTypesExtendedFeature() {
      assertNotNull(
          ExperimentalFeatures.Feature.valueOf("REFERENCE_TYPES_EXTENDED"),
          "Should have REFERENCE_TYPES_EXTENDED enum value");
    }

    @Test
    @DisplayName("should have RELAXED_SIMD feature")
    void shouldHaveRelaxedSimdFeature() {
      assertNotNull(
          ExperimentalFeatures.Feature.valueOf("RELAXED_SIMD"),
          "Should have RELAXED_SIMD enum value");
    }

    @Test
    @DisplayName("should have exactly 5 features")
    void shouldHaveExactlyFiveFeatures() {
      assertEquals(
          5,
          ExperimentalFeatures.Feature.values().length,
          "Feature enum should have exactly 5 values");
    }

    @Test
    @DisplayName("features should have system property names")
    void featuresShouldHaveSystemPropertyNames() {
      for (final ExperimentalFeatures.Feature feature : ExperimentalFeatures.Feature.values()) {
        assertNotNull(feature.getSystemProperty(), "Each feature should have a system property");
        assertTrue(
            feature.getSystemProperty().startsWith("wasmtime4j.experimental."),
            "System property should start with wasmtime4j.experimental.");
      }
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have enableFeature method")
    void shouldHaveEnableFeatureMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatures.class.getMethod("enableFeature", ExperimentalFeatures.Feature.class);
      assertNotNull(method, "enableFeature method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "enableFeature should be static");
    }

    @Test
    @DisplayName("should have disableFeature method")
    void shouldHaveDisableFeatureMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatures.class.getMethod(
              "disableFeature", ExperimentalFeatures.Feature.class);
      assertNotNull(method, "disableFeature method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "disableFeature should be static");
    }

    @Test
    @DisplayName("should have isFeatureEnabled method")
    void shouldHaveIsFeatureEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatures.class.getMethod(
              "isFeatureEnabled", ExperimentalFeatures.Feature.class);
      assertNotNull(method, "isFeatureEnabled method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isFeatureEnabled should be static");
      assertEquals(boolean.class, method.getReturnType(), "isFeatureEnabled should return boolean");
    }

    @Test
    @DisplayName("should have getEnabledFeatures method")
    void shouldHaveGetEnabledFeaturesMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatures.class.getMethod("getEnabledFeatures");
      assertNotNull(method, "getEnabledFeatures method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getEnabledFeatures should be static");
      assertEquals(Set.class, method.getReturnType(), "getEnabledFeatures should return Set");
    }

    @Test
    @DisplayName("should have validateFeatureSupport method")
    void shouldHaveValidateFeatureSupportMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatures.class.getMethod(
              "validateFeatureSupport", ExperimentalFeatures.Feature.class);
      assertNotNull(method, "validateFeatureSupport method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "validateFeatureSupport should be static");
    }

    @Test
    @DisplayName("should have hasEnabledFeatures method")
    void shouldHaveHasEnabledFeaturesMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatures.class.getMethod("hasEnabledFeatures");
      assertNotNull(method, "hasEnabledFeatures method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "hasEnabledFeatures should be static");
      assertEquals(
          boolean.class, method.getReturnType(), "hasEnabledFeatures should return boolean");
    }
  }

  @Nested
  @DisplayName("Feature Enable/Disable Tests")
  class FeatureEnableDisableTests {

    @Test
    @DisplayName("should enable feature programmatically")
    void shouldEnableFeatureProgrammatically() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING;
      assertFalse(
          ExperimentalFeatures.isFeatureEnabled(feature), "Feature should be disabled initially");

      ExperimentalFeatures.enableFeature(feature);
      assertTrue(
          ExperimentalFeatures.isFeatureEnabled(feature),
          "Feature should be enabled after enableFeature");
    }

    @Test
    @DisplayName("should disable feature programmatically")
    void shouldDisableFeatureProgrammatically() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.ADVANCED_SIMD;
      ExperimentalFeatures.enableFeature(feature);
      assertTrue(ExperimentalFeatures.isFeatureEnabled(feature), "Feature should be enabled");

      ExperimentalFeatures.disableFeature(feature);
      assertFalse(
          ExperimentalFeatures.isFeatureEnabled(feature),
          "Feature should be disabled after disableFeature");
    }

    @Test
    @DisplayName("should throw on null feature for enableFeature")
    void shouldThrowOnNullFeatureForEnableFeature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.enableFeature(null),
          "Should throw on null feature");
    }

    @Test
    @DisplayName("should throw on null feature for disableFeature")
    void shouldThrowOnNullFeatureForDisableFeature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.disableFeature(null),
          "Should throw on null feature");
    }

    @Test
    @DisplayName("should throw on null feature for isFeatureEnabled")
    void shouldThrowOnNullFeatureForIsFeatureEnabled() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.isFeatureEnabled(null),
          "Should throw on null feature");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should throw on null feature for validateFeatureSupport")
    void shouldThrowOnNullFeatureForValidateFeatureSupport() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExperimentalFeatures.validateFeatureSupport(null),
          "Should throw on null feature");
    }

    @Test
    @DisplayName("should throw UnsupportedOperationException for disabled feature")
    void shouldThrowUnsupportedOperationExceptionForDisabledFeature() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.MULTI_VALUE;
      assertFalse(ExperimentalFeatures.isFeatureEnabled(feature), "Feature should be disabled");

      assertThrows(
          UnsupportedOperationException.class,
          () -> ExperimentalFeatures.validateFeatureSupport(feature),
          "Should throw UnsupportedOperationException for disabled feature");
    }

    @Test
    @DisplayName("should not throw for enabled feature validation")
    void shouldNotThrowForEnabledFeatureValidation() {
      final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.RELAXED_SIMD;
      ExperimentalFeatures.enableFeature(feature);

      ExperimentalFeatures.validateFeatureSupport(feature);
    }
  }

  @Nested
  @DisplayName("Enabled Features Collection Tests")
  class EnabledFeaturesCollectionTests {

    @Test
    @DisplayName("should return empty set when no features enabled")
    void shouldReturnEmptySetWhenNoFeaturesEnabled() {
      final Set<ExperimentalFeatures.Feature> enabled = ExperimentalFeatures.getEnabledFeatures();
      assertNotNull(enabled, "Enabled features set should not be null");
      assertTrue(enabled.isEmpty(), "No features should be enabled by default");
    }

    @Test
    @DisplayName("should return enabled features in set")
    void shouldReturnEnabledFeaturesInSet() {
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);

      final Set<ExperimentalFeatures.Feature> enabled = ExperimentalFeatures.getEnabledFeatures();
      assertEquals(2, enabled.size(), "Should have 2 enabled features");
      assertTrue(
          enabled.contains(ExperimentalFeatures.Feature.EXCEPTION_HANDLING),
          "Should contain EXCEPTION_HANDLING");
      assertTrue(
          enabled.contains(ExperimentalFeatures.Feature.ADVANCED_SIMD),
          "Should contain ADVANCED_SIMD");
    }

    @Test
    @DisplayName("hasEnabledFeatures should return false when no features enabled")
    void hasEnabledFeaturesShouldReturnFalseWhenNoFeaturesEnabled() {
      assertFalse(
          ExperimentalFeatures.hasEnabledFeatures(),
          "hasEnabledFeatures should return false when no features enabled");
    }

    @Test
    @DisplayName("hasEnabledFeatures should return true when features enabled")
    void hasEnabledFeaturesShouldReturnTrueWhenFeaturesEnabled() {
      ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.MULTI_VALUE);
      assertTrue(
          ExperimentalFeatures.hasEnabledFeatures(),
          "hasEnabledFeatures should return true when features enabled");
    }
  }

  @Nested
  @DisplayName("Feature Instance Method Tests")
  class FeatureInstanceMethodTests {

    @Test
    @DisplayName("Feature should have getSystemProperty method")
    void featureShouldHaveGetSystemPropertyMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatures.Feature.class.getMethod("getSystemProperty");
      assertNotNull(method, "getSystemProperty method should exist");
      assertEquals(String.class, method.getReturnType(), "getSystemProperty should return String");
    }

    @Test
    @DisplayName("Feature should have isDefaultEnabled method")
    void featureShouldHaveIsDefaultEnabledMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatures.Feature.class.getMethod("isDefaultEnabled");
      assertNotNull(method, "isDefaultEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isDefaultEnabled should return boolean");
    }

    @Test
    @DisplayName("Feature should have isEnabled method")
    void featureShouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatures.Feature.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEnabled should return boolean");
    }

    @Test
    @DisplayName("all features should have default enabled as false")
    void allFeaturesShouldHaveDefaultEnabledAsFalse() {
      for (final ExperimentalFeatures.Feature feature : ExperimentalFeatures.Feature.values()) {
        assertFalse(
            feature.isDefaultEnabled(), "Feature " + feature + " should not be enabled by default");
      }
    }
  }
}
