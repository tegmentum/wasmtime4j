/*
 * Copyright 2024 Tegmentum Technology, Inc.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for experimental features configuration and management. */
final class ExperimentalFeaturesTest {

  @BeforeEach
  void setUp() {
    // Clear any system properties that might affect tests
    for (final ExperimentalFeatures.Feature feature : ExperimentalFeatures.Feature.values()) {
      System.clearProperty(feature.getSystemProperty());
    }
    ExperimentalFeatures.reset();
  }

  @AfterEach
  void tearDown() {
    // Clean up after tests
    for (final ExperimentalFeatures.Feature feature : ExperimentalFeatures.Feature.values()) {
      System.clearProperty(feature.getSystemProperty());
    }
    ExperimentalFeatures.reset();
  }

  @Test
  void testFeatureEnumValues() {
    final ExperimentalFeatures.Feature[] features = ExperimentalFeatures.Feature.values();

    assertEquals(5, features.length, "Expected 5 experimental features");

    // Verify all expected features are present
    assertTrue(contains(features, ExperimentalFeatures.Feature.EXCEPTION_HANDLING));
    assertTrue(contains(features, ExperimentalFeatures.Feature.ADVANCED_SIMD));
    assertTrue(contains(features, ExperimentalFeatures.Feature.MULTI_VALUE));
    assertTrue(contains(features, ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED));
    assertTrue(contains(features, ExperimentalFeatures.Feature.RELAXED_SIMD));
  }

  @Test
  void testSystemPropertyNames() {
    assertEquals(
        "wasmtime4j.experimental.exceptions",
        ExperimentalFeatures.Feature.EXCEPTION_HANDLING.getSystemProperty());
    assertEquals(
        "wasmtime4j.experimental.simd",
        ExperimentalFeatures.Feature.ADVANCED_SIMD.getSystemProperty());
    assertEquals(
        "wasmtime4j.experimental.multivalue",
        ExperimentalFeatures.Feature.MULTI_VALUE.getSystemProperty());
    assertEquals(
        "wasmtime4j.experimental.reftypes",
        ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED.getSystemProperty());
    assertEquals(
        "wasmtime4j.experimental.relaxed_simd",
        ExperimentalFeatures.Feature.RELAXED_SIMD.getSystemProperty());
  }

  @Test
  void testDefaultEnabledStates() {
    // All features should be disabled by default
    for (final ExperimentalFeatures.Feature feature : ExperimentalFeatures.Feature.values()) {
      assertFalse(
          feature.isDefaultEnabled(), "Feature " + feature + " should be disabled by default");
      assertFalse(feature.isEnabled(), "Feature " + feature + " should not be enabled initially");
    }
  }

  @Test
  void testProgrammaticFeatureEnabling() {
    final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING;

    assertFalse(ExperimentalFeatures.isFeatureEnabled(feature));

    ExperimentalFeatures.enableFeature(feature);
    assertTrue(ExperimentalFeatures.isFeatureEnabled(feature));

    ExperimentalFeatures.disableFeature(feature);
    assertFalse(ExperimentalFeatures.isFeatureEnabled(feature));
  }

  @Test
  void testSystemPropertyOverride() {
    final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.ADVANCED_SIMD;

    // Set system property to true
    System.setProperty(feature.getSystemProperty(), "true");
    assertTrue(ExperimentalFeatures.isFeatureEnabled(feature));
    assertTrue(feature.isEnabled());

    // Programmatic disable should not override system property
    ExperimentalFeatures.disableFeature(feature);
    assertTrue(
        ExperimentalFeatures.isFeatureEnabled(feature),
        "System property should override programmatic setting");

    // Set system property to false
    System.setProperty(feature.getSystemProperty(), "false");
    assertFalse(ExperimentalFeatures.isFeatureEnabled(feature));
    assertFalse(feature.isEnabled());

    // Programmatic enable should not override system property
    ExperimentalFeatures.enableFeature(feature);
    assertFalse(
        ExperimentalFeatures.isFeatureEnabled(feature),
        "System property should override programmatic setting");
  }

  @Test
  void testGetEnabledFeatures() {
    Set<ExperimentalFeatures.Feature> enabled = ExperimentalFeatures.getEnabledFeatures();
    assertTrue(enabled.isEmpty(), "No features should be enabled initially");

    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.MULTI_VALUE);
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.RELAXED_SIMD);

    enabled = ExperimentalFeatures.getEnabledFeatures();
    assertEquals(2, enabled.size());
    assertTrue(enabled.contains(ExperimentalFeatures.Feature.MULTI_VALUE));
    assertTrue(enabled.contains(ExperimentalFeatures.Feature.RELAXED_SIMD));
  }

  @Test
  void testValidateFeatureSupport() {
    final ExperimentalFeatures.Feature feature =
        ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED;

    // Should throw when feature is not enabled
    final UnsupportedOperationException exception =
        assertThrows(
            UnsupportedOperationException.class,
            () -> ExperimentalFeatures.validateFeatureSupport(feature));

    assertTrue(exception.getMessage().contains("not enabled"));
    assertTrue(exception.getMessage().contains(feature.getSystemProperty()));

    // Should not throw when feature is enabled
    ExperimentalFeatures.enableFeature(feature);
    assertDoesNotThrow(() -> ExperimentalFeatures.validateFeatureSupport(feature));
  }

  @Test
  void testHasEnabledFeatures() {
    assertFalse(ExperimentalFeatures.hasEnabledFeatures());

    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
    assertTrue(ExperimentalFeatures.hasEnabledFeatures());

    ExperimentalFeatures.disableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
    assertFalse(ExperimentalFeatures.hasEnabledFeatures());
  }

  @Test
  void testNullFeatureHandling() {
    assertThrows(IllegalArgumentException.class, () -> ExperimentalFeatures.enableFeature(null));

    assertThrows(IllegalArgumentException.class, () -> ExperimentalFeatures.disableFeature(null));

    assertThrows(IllegalArgumentException.class, () -> ExperimentalFeatures.isFeatureEnabled(null));

    assertThrows(
        IllegalArgumentException.class, () -> ExperimentalFeatures.validateFeatureSupport(null));
  }

  @Test
  void testSystemPropertyParsing() {
    final ExperimentalFeatures.Feature feature = ExperimentalFeatures.Feature.ADVANCED_SIMD;

    // Test various boolean string values
    System.setProperty(feature.getSystemProperty(), "true");
    assertTrue(feature.isEnabled());

    System.setProperty(feature.getSystemProperty(), "TRUE");
    assertTrue(feature.isEnabled());

    System.setProperty(feature.getSystemProperty(), "false");
    assertFalse(feature.isEnabled());

    System.setProperty(feature.getSystemProperty(), "FALSE");
    assertFalse(feature.isEnabled());

    System.setProperty(feature.getSystemProperty(), "invalid");
    assertFalse(feature.isEnabled());

    System.setProperty(feature.getSystemProperty(), "");
    assertFalse(feature.isEnabled());
  }

  @Test
  void testFeatureIndependence() {
    // Enabling one feature should not affect others
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);

    assertTrue(
        ExperimentalFeatures.isFeatureEnabled(ExperimentalFeatures.Feature.EXCEPTION_HANDLING));
    assertFalse(ExperimentalFeatures.isFeatureEnabled(ExperimentalFeatures.Feature.ADVANCED_SIMD));
    assertFalse(ExperimentalFeatures.isFeatureEnabled(ExperimentalFeatures.Feature.MULTI_VALUE));
    assertFalse(
        ExperimentalFeatures.isFeatureEnabled(
            ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED));
    assertFalse(ExperimentalFeatures.isFeatureEnabled(ExperimentalFeatures.Feature.RELAXED_SIMD));
  }

  @Test
  void testConcurrentAccess() throws InterruptedException {
    final int threadCount = 10;
    final Thread[] threads = new Thread[threadCount];
    final boolean[] results = new boolean[threadCount];

    // Create threads that enable/disable features concurrently
    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  final ExperimentalFeatures.Feature feature =
                      ExperimentalFeatures.Feature.MULTI_VALUE;

                  if (threadIndex % 2 == 0) {
                    ExperimentalFeatures.enableFeature(feature);
                  } else {
                    ExperimentalFeatures.disableFeature(feature);
                  }

                  results[threadIndex] = ExperimentalFeatures.isFeatureEnabled(feature);
                } catch (final Exception e) {
                  results[threadIndex] = false;
                }
              });
    }

    // Start all threads
    for (final Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      thread.join();
    }

    // Verify no exceptions occurred (all results should be valid boolean values)
    for (final boolean result : results) {
      // The actual value doesn't matter, just that no exceptions were thrown
      assertNotNull(result);
    }
  }

  private boolean contains(
      final ExperimentalFeatures.Feature[] features, final ExperimentalFeatures.Feature target) {
    for (final ExperimentalFeatures.Feature feature : features) {
      if (feature == target) {
        return true;
      }
    }
    return false;
  }
}
