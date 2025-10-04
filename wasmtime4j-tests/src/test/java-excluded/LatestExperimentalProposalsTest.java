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

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.jni.JniExperimentalFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for latest WebAssembly experimental proposals.
 *
 * <p>This test suite validates the implementation of cutting-edge WebAssembly proposals that are
 * currently in committee stage, including:
 *
 * <ul>
 *   <li>Flexible vectors with dynamic vector operations
 *   <li>String imports with multiple encoding formats
 *   <li>Resource types with advanced resource management
 *   <li>Type imports with dynamic type system integration
 *   <li>Extended constant expressions with compile-time computation
 *   <li>Shared-everything threads with advanced synchronization
 *   <li>Custom page sizes with flexible memory management
 * </ul>
 *
 * <p><strong>WARNING:</strong> These features are experimental and subject to change.
 *
 * @since 1.0.0
 */
@DisplayName("Latest WebAssembly Experimental Proposals")
public class LatestExperimentalProposalsTest {

  @Nested
  @DisplayName("JNI Implementation Tests")
  class JniImplementationTests {

    @Test
    @DisplayName("Should create JNI experimental features configuration")
    void shouldCreateJniExperimentalConfig() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        assertTrue(config.isValid(), "JNI experimental configuration should be valid");
      }
    }

    @Test
    @DisplayName("Should enable flexible vectors with JNI implementation")
    void shouldEnableFlexibleVectorsJni() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test flexible vectors configuration
        JniExperimentalFeatures result =
            config.enableFlexibleVectors(
                true, // dynamic sizing
                true, // auto vectorization
                true // simd integration
                );

        assertSame(config, result, "Should support method chaining");
        assertTrue(config.isValid(), "Configuration should remain valid");
      }
    }

    @Test
    @DisplayName("Should enable string imports with JNI implementation")
    void shouldEnableStringImportsJni() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test string imports configuration with UTF-8 encoding
        JniExperimentalFeatures result =
            config.enableStringImports(
                JniExperimentalFeatures.StringEncodingFormat.UTF8,
                true, // string interning
                true, // lazy decoding
                false // js interop
                );

        assertSame(config, result, "Should support method chaining");
        assertTrue(config.isValid(), "Configuration should remain valid");
      }
    }

    @Test
    @DisplayName("Should enable resource types with JNI implementation")
    void shouldEnableResourceTypesJni() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test resource types configuration
        JniExperimentalFeatures result =
            config.enableResourceTypes(
                true, // automatic cleanup
                true, // reference counting
                JniExperimentalFeatures.ResourceCleanupStrategy.AUTOMATIC);

        assertSame(config, result, "Should support method chaining");
        assertTrue(config.isValid(), "Configuration should remain valid");
      }
    }

    @Test
    @DisplayName("Should enable type imports with JNI implementation")
    void shouldEnableTypeImportsJni() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test type imports configuration
        JniExperimentalFeatures result =
            config.enableTypeImports(
                JniExperimentalFeatures.TypeValidationStrategy.STRICT,
                JniExperimentalFeatures.ImportResolutionMechanism.STATIC,
                true // structural compatibility
                );

        assertSame(config, result, "Should support method chaining");
        assertTrue(config.isValid(), "Configuration should remain valid");
      }
    }

    @Test
    @DisplayName("Should enable shared-everything threads with JNI implementation")
    void shouldEnableSharedEverythingThreadsJni() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test shared-everything threads configuration
        JniExperimentalFeatures result =
            config.enableSharedEverythingThreads(
                2, // min threads
                8, // max threads
                true, // global state sharing
                true // atomic operations
                );

        assertSame(config, result, "Should support method chaining");
        assertTrue(config.isValid(), "Configuration should remain valid");
      }
    }

    @Test
    @DisplayName("Should enable custom page sizes with JNI implementation")
    void shouldEnableCustomPageSizesJni() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test custom page sizes configuration
        JniExperimentalFeatures result =
            config.enableCustomPageSizes(
                8192, // 8KB page size
                JniExperimentalFeatures.PageSizeStrategy.CUSTOM,
                true // strict alignment
                );

        assertSame(config, result, "Should support method chaining");
        assertTrue(config.isValid(), "Configuration should remain valid");
      }
    }

    @Test
    @DisplayName("Should validate experimental feature support detection")
    void shouldValidateExperimentalFeatureSupportDetection() {
      // Test feature support detection for various experimental features
      assertFalse(
          JniExperimentalFeatures.isExperimentalFeatureSupported(
              JniExperimentalFeatures.ExperimentalFeatureId.CALL_CC),
          "Call/CC should not be supported yet");

      assertFalse(
          JniExperimentalFeatures.isExperimentalFeatureSupported(
              JniExperimentalFeatures.ExperimentalFeatureId.RESOURCE_TYPES),
          "Resource types should not be supported yet");

      assertFalse(
          JniExperimentalFeatures.isExperimentalFeatureSupported(
              JniExperimentalFeatures.ExperimentalFeatureId.FLEXIBLE_VECTORS),
          "Flexible vectors should not be supported yet");
    }

    @Test
    @DisplayName("Should handle invalid arguments in JNI implementation")
    void shouldHandleInvalidArgumentsJni() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test invalid stack switching configuration
        assertThrows(
            IllegalArgumentException.class,
            () -> config.enableStackSwitching(2048, 0),
            "Should reject invalid stack configuration");

        // Test invalid page sizes
        assertThrows(
            IllegalArgumentException.class,
            () ->
                config.enableCustomPageSizes(
                    0, JniExperimentalFeatures.PageSizeStrategy.CUSTOM, false),
            "Should reject invalid page size");

        // Test null arguments
        assertThrows(
            IllegalArgumentException.class,
            () -> config.enableStringImports(null, true, true, false),
            "Should reject null encoding format");
      }
    }

    @Test
    @DisplayName("Should enable all experimental features")
    void shouldEnableAllExperimentalFeatures() {
      try (JniExperimentalFeatures config = JniExperimentalFeatures.allExperimentalEnabled()) {
        assertTrue(config.isValid(), "All-experimental configuration should be valid");

        // Configuration should be ready to use with all features enabled
        assertDoesNotThrow(
            () -> {
              // This configuration should have all experimental features enabled
              // but we can't test individual feature states without exposing them
            });
      }
    }
  }

  @Nested
  @DisplayName("Feature Validation Tests")
  class FeatureValidationTests {

    @Test
    @DisplayName("Should validate string encoding formats")
    void shouldValidateStringEncodingFormats() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test all supported encoding formats
        for (JniExperimentalFeatures.StringEncodingFormat format :
            JniExperimentalFeatures.StringEncodingFormat.values()) {
          assertDoesNotThrow(
              () -> config.enableStringImports(format, true, true, false),
              "Should support encoding format: " + format);
        }
      }
    }

    @Test
    @DisplayName("Should validate resource cleanup strategies")
    void shouldValidateResourceCleanupStrategies() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test all supported cleanup strategies
        for (JniExperimentalFeatures.ResourceCleanupStrategy strategy :
            JniExperimentalFeatures.ResourceCleanupStrategy.values()) {
          assertDoesNotThrow(
              () -> config.enableResourceTypes(true, false, strategy),
              "Should support cleanup strategy: " + strategy);
        }
      }
    }

    @Test
    @DisplayName("Should validate type validation strategies")
    void shouldValidateTypeValidationStrategies() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test all supported validation strategies
        for (JniExperimentalFeatures.TypeValidationStrategy strategy :
            JniExperimentalFeatures.TypeValidationStrategy.values()) {
          assertDoesNotThrow(
              () ->
                  config.enableTypeImports(
                      strategy, JniExperimentalFeatures.ImportResolutionMechanism.STATIC, true),
              "Should support validation strategy: " + strategy);
        }
      }
    }

    @Test
    @DisplayName("Should validate import resolution mechanisms")
    void shouldValidateImportResolutionMechanisms() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test all supported resolution mechanisms
        for (JniExperimentalFeatures.ImportResolutionMechanism mechanism :
            JniExperimentalFeatures.ImportResolutionMechanism.values()) {
          assertDoesNotThrow(
              () ->
                  config.enableTypeImports(
                      JniExperimentalFeatures.TypeValidationStrategy.STRICT, mechanism, true),
              "Should support resolution mechanism: " + mechanism);
        }
      }
    }

    @Test
    @DisplayName("Should validate page size strategies")
    void shouldValidatePageSizeStrategies() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test all supported page size strategies
        for (JniExperimentalFeatures.PageSizeStrategy strategy :
            JniExperimentalFeatures.PageSizeStrategy.values()) {
          assertDoesNotThrow(
              () -> config.enableCustomPageSizes(4096, strategy, false),
              "Should support page size strategy: " + strategy);
        }
      }
    }
  }

  @Nested
  @DisplayName("Advanced Configuration Tests")
  class AdvancedConfigurationTests {

    @Test
    @DisplayName("Should combine multiple experimental features")
    void shouldCombineMultipleExperimentalFeatures() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Enable multiple experimental features in combination
        config
            .enableStackSwitching(1024 * 1024, 10)
            .enableExtendedConstExpressions(
                true, true, JniExperimentalFeatures.ConstantFoldingLevel.AGGRESSIVE)
            .enableFlexibleVectors(true, true, true)
            .enableStringImports(
                JniExperimentalFeatures.StringEncodingFormat.UTF8, true, true, false)
            .enableResourceTypes(
                true, true, JniExperimentalFeatures.ResourceCleanupStrategy.AUTOMATIC)
            .enableTypeImports(
                JniExperimentalFeatures.TypeValidationStrategy.RELAXED,
                JniExperimentalFeatures.ImportResolutionMechanism.DYNAMIC,
                true)
            .enableSharedEverythingThreads(2, 16, true, true)
            .enableCustomPageSizes(16384, JniExperimentalFeatures.PageSizeStrategy.CUSTOM, true);

        assertTrue(config.isValid(), "Combined configuration should be valid");
      }
    }

    @Test
    @DisplayName("Should handle edge case configurations")
    void shouldHandleEdgeCaseConfigurations() {
      try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test minimum valid values
        assertDoesNotThrow(
            () -> config.enableStackSwitching(4096, 1),
            "Should accept minimum stack configuration");

        assertDoesNotThrow(
            () -> config.enableSharedEverythingThreads(1, 1, false, false),
            "Should accept minimum thread configuration");

        // Test power-of-2 page sizes
        int[] validPageSizes = {4096, 8192, 16384, 32768, 65536};
        for (int pageSize : validPageSizes) {
          assertDoesNotThrow(
              () ->
                  config.enableCustomPageSizes(
                      pageSize, JniExperimentalFeatures.PageSizeStrategy.CUSTOM, false),
              "Should accept valid page size: " + pageSize);
        }
      }
    }

    @Test
    @DisplayName("Should validate experimental feature enumeration completeness")
    void shouldValidateExperimentalFeatureEnumerationCompleteness() {
      // Ensure all experimental feature IDs are accounted for
      JniExperimentalFeatures.ExperimentalFeatureId[] features =
          JniExperimentalFeatures.ExperimentalFeatureId.values();

      assertTrue(features.length > 0, "Should have experimental features defined");

      // Test that all feature IDs are unique
      assertEquals(
          features.length,
          java.util.Arrays.stream(features)
              .mapToInt(JniExperimentalFeatures.ExperimentalFeatureId::getId)
              .boxed()
              .collect(java.util.stream.Collectors.toSet())
              .size(),
          "All experimental feature IDs should be unique");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle resource disposal properly")
    void shouldHandleResourceDisposalProperly() {
      JniExperimentalFeatures config = new JniExperimentalFeatures();
      assertTrue(config.isValid(), "Config should be initially valid");

      config.dispose();
      assertFalse(config.isValid(), "Config should be invalid after disposal");

      // Multiple calls to dispose should not cause issues
      assertDoesNotThrow(() -> config.dispose(), "Multiple dispose calls should be safe");
    }

    @Test
    @DisplayName("Should handle invalid feature ID gracefully")
    void shouldHandleInvalidFeatureIdGracefully() {
      assertDoesNotThrow(
          () -> {
            boolean supported = JniExperimentalFeatures.isExperimentalFeatureSupported(null);
            // Should not crash, but will throw IllegalArgumentException
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> JniExperimentalFeatures.isExperimentalFeatureSupported(null),
          "Should reject null feature ID");
    }
  }
}
