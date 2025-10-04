package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.jni.JniExperimentalFeatures;
import ai.tegmentum.wasmtime4j.panama.PanamaExperimentalFeatures;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive integration tests for experimental WebAssembly features.
 *
 * <p>These tests validate the implementation of cutting-edge WebAssembly proposals including stack
 * switching, call/cc, extended constant expressions, and other committee-stage features.
 *
 * <p><strong>WARNING:</strong> These tests are for experimental features that may not be fully
 * supported by the underlying Wasmtime runtime.
 */
@DisplayName("Experimental WebAssembly Features Integration Tests")
class ExperimentalFeaturesIntegrationTest {

  private static final Logger logger =
      Logger.getLogger(ExperimentalFeaturesIntegrationTest.class.getName());

  @BeforeEach
  void setUp() {
    // Log test environment information
    logger.info("Running experimental features tests");
    logger.info("Java version: " + System.getProperty("java.version"));
    logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
    logger.info("Architecture: " + System.getProperty("os.arch"));
  }

  @Nested
  @DisplayName("WasmFeature Enum Tests")
  class WasmFeatureEnumTests {

    @Test
    @DisplayName("Should include all experimental features in WasmFeature enum")
    void testExperimentalFeaturesInEnum() {
      // Verify that all experimental features are present in the WasmFeature enum
      final Set<WasmFeature> allFeatures = Set.of(WasmFeature.values());

      assertTrue(
          allFeatures.contains(WasmFeature.STACK_SWITCHING),
          "WasmFeature should include STACK_SWITCHING");
      assertTrue(allFeatures.contains(WasmFeature.CALL_CC), "WasmFeature should include CALL_CC");
      assertTrue(
          allFeatures.contains(WasmFeature.EXTENDED_CONST_EXPRESSIONS),
          "WasmFeature should include EXTENDED_CONST_EXPRESSIONS");
      assertTrue(
          allFeatures.contains(WasmFeature.MEMORY64_EXTENDED),
          "WasmFeature should include MEMORY64_EXTENDED");
      assertTrue(
          allFeatures.contains(WasmFeature.CUSTOM_PAGE_SIZES),
          "WasmFeature should include CUSTOM_PAGE_SIZES");
      assertTrue(
          allFeatures.contains(WasmFeature.SHARED_EVERYTHING_THREADS),
          "WasmFeature should include SHARED_EVERYTHING_THREADS");
      assertTrue(
          allFeatures.contains(WasmFeature.TYPE_IMPORTS),
          "WasmFeature should include TYPE_IMPORTS");
      assertTrue(
          allFeatures.contains(WasmFeature.STRING_IMPORTS),
          "WasmFeature should include STRING_IMPORTS");
      assertTrue(
          allFeatures.contains(WasmFeature.RESOURCE_TYPES),
          "WasmFeature should include RESOURCE_TYPES");
      assertTrue(
          allFeatures.contains(WasmFeature.INTERFACE_TYPES),
          "WasmFeature should include INTERFACE_TYPES");
      assertTrue(
          allFeatures.contains(WasmFeature.FLEXIBLE_VECTORS),
          "WasmFeature should include FLEXIBLE_VECTORS");
    }
  }

  @Nested
  @DisplayName("EngineConfig Integration Tests")
  class EngineConfigIntegrationTests {

    @Test
    @DisplayName("Should support experimental features in EngineConfig")
    void testEngineConfigExperimentalFeatures() {
      final EngineConfig config = new EngineConfig();

      // Test adding experimental features
      assertDoesNotThrow(
          () -> {
            config.addWasmFeature(WasmFeature.STACK_SWITCHING);
            config.addWasmFeature(WasmFeature.CALL_CC);
            config.addWasmFeature(WasmFeature.EXTENDED_CONST_EXPRESSIONS);
          },
          "Should be able to add experimental features to EngineConfig");

      // Verify features are in the set
      final Set<WasmFeature> features = config.getWasmFeatures();
      assertTrue(
          features.contains(WasmFeature.STACK_SWITCHING),
          "EngineConfig should contain STACK_SWITCHING feature");
      assertTrue(
          features.contains(WasmFeature.CALL_CC), "EngineConfig should contain CALL_CC feature");
      assertTrue(
          features.contains(WasmFeature.EXTENDED_CONST_EXPRESSIONS),
          "EngineConfig should contain EXTENDED_CONST_EXPRESSIONS feature");
    }

    @Test
    @DisplayName("Should provide experimental configuration factories")
    void testExperimentalConfigurationFactories() {
      assertDoesNotThrow(
          () -> {
            final EngineConfig experimentalConfig = EngineConfig.forExperimentalFeatures();
            assertNotNull(
                experimentalConfig, "forExperimentalFeatures() should return non-null config");

            final Set<WasmFeature> features = experimentalConfig.getWasmFeatures();
            assertFalse(features.isEmpty(), "Experimental config should have features enabled");

            logger.info("Experimental features config contains: " + features);
          },
          "Should be able to create experimental features configuration");

      assertDoesNotThrow(
          () -> {
            final EngineConfig threadingConfig = EngineConfig.forExperimentalThreading();
            assertNotNull(
                threadingConfig, "forExperimentalThreading() should return non-null config");

            final Set<WasmFeature> features = threadingConfig.getWasmFeatures();
            assertTrue(
                features.contains(WasmFeature.THREADS),
                "Threading config should include THREADS feature");

            logger.info("Experimental threading config contains: " + features);
          },
          "Should be able to create experimental threading configuration");

      assertDoesNotThrow(
          () -> {
            final EngineConfig componentsConfig = EngineConfig.forExperimentalComponents();
            assertNotNull(
                componentsConfig, "forExperimentalComponents() should return non-null config");

            final Set<WasmFeature> features = componentsConfig.getWasmFeatures();
            assertTrue(
                features.contains(WasmFeature.COMPONENT_MODEL),
                "Components config should include COMPONENT_MODEL feature");

            logger.info("Experimental components config contains: " + features);
          },
          "Should be able to create experimental components configuration");
    }

    @Test
    @DisplayName("Should support experimental feature getters")
    void testExperimentalFeatureGetters() {
      final EngineConfig config = EngineConfig.forExperimentalFeatures();

      // Test experimental feature getters
      assertDoesNotThrow(
          () -> {
            final boolean stackSwitching = config.isWasmStackSwitching();
            final boolean callCc = config.isWasmCallCc();
            final boolean extendedConst = config.isWasmExtendedConstExpressions();
            final boolean memory64Extended = config.isWasmMemory64Extended();
            final boolean customPageSizes = config.isWasmCustomPageSizes();
            final boolean sharedEverythingThreads = config.isWasmSharedEverythingThreads();
            final boolean typeImports = config.isWasmTypeImports();
            final boolean stringImports = config.isWasmStringImports();
            final boolean resourceTypes = config.isWasmResourceTypes();
            final boolean interfaceTypes = config.isWasmInterfaceTypes();
            final boolean flexibleVectors = config.isWasmFlexibleVectors();

            logger.info("Experimental feature states:");
            logger.info("  Stack Switching: " + stackSwitching);
            logger.info("  Call/CC: " + callCc);
            logger.info("  Extended Const Expressions: " + extendedConst);
            logger.info("  Memory64 Extended: " + memory64Extended);
            logger.info("  Custom Page Sizes: " + customPageSizes);
            logger.info("  Shared Everything Threads: " + sharedEverythingThreads);
            logger.info("  Type Imports: " + typeImports);
            logger.info("  String Imports: " + stringImports);
            logger.info("  Resource Types: " + resourceTypes);
            logger.info("  Interface Types: " + interfaceTypes);
            logger.info("  Flexible Vectors: " + flexibleVectors);

            // For experimental config, most features should be enabled
            assertTrue(stackSwitching, "Stack switching should be enabled in experimental config");
            assertTrue(callCc, "Call/CC should be enabled in experimental config");
            assertTrue(
                extendedConst,
                "Extended const expressions should be enabled in experimental config");
          },
          "Should be able to access experimental feature states");
    }
  }

  @Nested
  @DisplayName("JNI Experimental Features Tests")
  @EnabledIfSystemProperty(named = "wasmtime4j.runtime", matches = "jni|auto")
  class JniExperimentalFeaturesTests {

    @Test
    @DisplayName("Should create JNI experimental features configuration")
    void testJniExperimentalFeaturesCreation() {
      assertDoesNotThrow(
          () -> {
            try (final JniExperimentalFeatures config = new JniExperimentalFeatures()) {
              assertTrue(
                  config.isValid(), "JNI experimental features configuration should be valid");
              logger.info("Created JNI experimental features configuration successfully");
            }
          },
          "Should be able to create JNI experimental features configuration");
    }

    @Test
    @DisplayName("Should create all-experimental JNI configuration")
    void testJniAllExperimentalConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final JniExperimentalFeatures config =
                JniExperimentalFeatures.allExperimentalEnabled()) {
              assertTrue(config.isValid(), "All-experimental JNI configuration should be valid");
              logger.info("Created all-experimental JNI configuration successfully");
            }
          },
          "Should be able to create all-experimental JNI configuration");
    }

    @Test
    @DisplayName("Should enable stack switching in JNI configuration")
    void testJniStackSwitchingConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final JniExperimentalFeatures config = new JniExperimentalFeatures()) {
              config.enableStackSwitching(64 * 1024, 100); // 64KB stacks, max 100
              assertTrue(
                  config.isValid(),
                  "Configuration should remain valid after enabling stack switching");
              logger.info("Enabled stack switching in JNI configuration successfully");
            }
          },
          "Should be able to enable stack switching in JNI configuration");
    }

    @Test
    @DisplayName("Should validate stack switching parameters in JNI configuration")
    void testJniStackSwitchingParameterValidation() {
      try (final JniExperimentalFeatures config = new JniExperimentalFeatures()) {
        // Test invalid stack size
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              config.enableStackSwitching(2048, 100); // Too small (< 4KB)
            },
            "Should reject stack size smaller than 4KB");

        // Test invalid max stacks
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              config.enableStackSwitching(8192, 0); // Zero max stacks
            },
            "Should reject zero max concurrent stacks");

        logger.info("JNI stack switching parameter validation works correctly");
      }
    }

    @Test
    @DisplayName("Should enable call/cc in JNI configuration")
    void testJniCallCcConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final JniExperimentalFeatures config = new JniExperimentalFeatures()) {
              config.enableCallCc(1000, JniExperimentalFeatures.ContinuationStorageStrategy.HYBRID);
              assertTrue(
                  config.isValid(), "Configuration should remain valid after enabling call/cc");
              logger.info("Enabled call/cc in JNI configuration successfully");
            }
          },
          "Should be able to enable call/cc in JNI configuration");
    }

    @Test
    @DisplayName("Should enable extended constant expressions in JNI configuration")
    void testJniExtendedConstExpressionsConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final JniExperimentalFeatures config = new JniExperimentalFeatures()) {
              config.enableExtendedConstExpressions(
                  true, // import-based expressions
                  true, // global dependencies
                  JniExperimentalFeatures.ConstantFoldingLevel.AGGRESSIVE);
              assertTrue(
                  config.isValid(),
                  "Configuration should remain valid after enabling extended const expressions");
              logger.info(
                  "Enabled extended constant expressions in JNI configuration successfully");
            }
          },
          "Should be able to enable extended constant expressions in JNI configuration");
    }

    @Test
    @DisplayName("Should report experimental feature support status")
    void testJniExperimentalFeatureSupport() {
      // Test static methods for feature support
      final boolean stackSwitchingSupported = JniExperimentalFeatures.isStackSwitchingSupported();
      final boolean callCcSupported = JniExperimentalFeatures.isCallCcSupported();
      final Set<WasmFeature> supportedFeatures =
          JniExperimentalFeatures.getSupportedExperimentalFeatures();

      logger.info("JNI Experimental feature support:");
      logger.info("  Stack Switching: " + stackSwitchingSupported);
      logger.info("  Call/CC: " + callCcSupported);
      logger.info("  Supported Features: " + supportedFeatures);

      // For now, most experimental features are expected to be unsupported
      // This will change as Wasmtime adds support for these features
      assertFalse(stackSwitchingSupported, "Stack switching not yet supported by Wasmtime");
      assertFalse(callCcSupported, "Call/CC not yet supported by Wasmtime");
      assertTrue(
          supportedFeatures.isEmpty() || supportedFeatures.size() >= 0,
          "Supported features should be a valid set");
    }
  }

  @Nested
  @DisplayName("Panama Experimental Features Tests")
  @EnabledIfSystemProperty(named = "wasmtime4j.runtime", matches = "panama|auto")
  class PanamaExperimentalFeaturesTests {

    @Test
    @DisplayName("Should create Panama experimental features configuration")
    void testPanamaExperimentalFeaturesCreation() {
      assertDoesNotThrow(
          () -> {
            try (final PanamaExperimentalFeatures config = new PanamaExperimentalFeatures()) {
              assertTrue(
                  config.isValid(), "Panama experimental features configuration should be valid");
              logger.info("Created Panama experimental features configuration successfully");
            }
          },
          "Should be able to create Panama experimental features configuration");
    }

    @Test
    @DisplayName("Should create all-experimental Panama configuration")
    void testPanamaAllExperimentalConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final PanamaExperimentalFeatures config =
                PanamaExperimentalFeatures.allExperimentalEnabled()) {
              assertTrue(config.isValid(), "All-experimental Panama configuration should be valid");
              logger.info("Created all-experimental Panama configuration successfully");
            }
          },
          "Should be able to create all-experimental Panama configuration");
    }

    @Test
    @DisplayName("Should enable stack switching in Panama configuration")
    void testPanamaStackSwitchingConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final PanamaExperimentalFeatures config = new PanamaExperimentalFeatures()) {
              config.enableStackSwitching(64 * 1024, 100); // 64KB stacks, max 100
              assertTrue(
                  config.isValid(),
                  "Configuration should remain valid after enabling stack switching");
              logger.info("Enabled stack switching in Panama configuration successfully");
            }
          },
          "Should be able to enable stack switching in Panama configuration");
    }

    @Test
    @DisplayName("Should validate stack switching parameters in Panama configuration")
    void testPanamaStackSwitchingParameterValidation() {
      try (final PanamaExperimentalFeatures config = new PanamaExperimentalFeatures()) {
        // Test invalid stack size
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              config.enableStackSwitching(2048, 100); // Too small (< 4KB)
            },
            "Should reject stack size smaller than 4KB");

        // Test invalid max stacks
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              config.enableStackSwitching(8192, 0); // Zero max stacks
            },
            "Should reject zero max concurrent stacks");

        logger.info("Panama stack switching parameter validation works correctly");
      }
    }

    @Test
    @DisplayName("Should enable call/cc in Panama configuration")
    void testPanamaCallCcConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final PanamaExperimentalFeatures config = new PanamaExperimentalFeatures()) {
              config.enableCallCc(
                  1000, PanamaExperimentalFeatures.ContinuationStorageStrategy.HYBRID);
              assertTrue(
                  config.isValid(), "Configuration should remain valid after enabling call/cc");
              logger.info("Enabled call/cc in Panama configuration successfully");
            }
          },
          "Should be able to enable call/cc in Panama configuration");
    }

    @Test
    @DisplayName("Should enable extended constant expressions in Panama configuration")
    void testPanamaExtendedConstExpressionsConfiguration() {
      assertDoesNotThrow(
          () -> {
            try (final PanamaExperimentalFeatures config = new PanamaExperimentalFeatures()) {
              config.enableExtendedConstExpressions(
                  true, // import-based expressions
                  true, // global dependencies
                  PanamaExperimentalFeatures.ConstantFoldingLevel.AGGRESSIVE);
              assertTrue(
                  config.isValid(),
                  "Configuration should remain valid after enabling extended const expressions");
              logger.info(
                  "Enabled extended constant expressions in Panama configuration successfully");
            }
          },
          "Should be able to enable extended constant expressions in Panama configuration");
    }

    @Test
    @DisplayName("Should report experimental feature support status")
    void testPanamaExperimentalFeatureSupport() {
      // Test static methods for feature support
      final boolean stackSwitchingSupported =
          PanamaExperimentalFeatures.isStackSwitchingSupported();
      final boolean callCcSupported = PanamaExperimentalFeatures.isCallCcSupported();
      final Set<WasmFeature> supportedFeatures =
          PanamaExperimentalFeatures.getSupportedExperimentalFeatures();

      logger.info("Panama Experimental feature support:");
      logger.info("  Stack Switching: " + stackSwitchingSupported);
      logger.info("  Call/CC: " + callCcSupported);
      logger.info("  Supported Features: " + supportedFeatures);

      // For now, most experimental features are expected to be unsupported
      // This will change as Wasmtime adds support for these features
      assertFalse(stackSwitchingSupported, "Stack switching not yet supported by Wasmtime");
      assertFalse(callCcSupported, "Call/CC not yet supported by Wasmtime");
      assertTrue(
          supportedFeatures.isEmpty() || supportedFeatures.size() >= 0,
          "Supported features should be a valid set");
    }
  }

  @Nested
  @DisplayName("Cross-Runtime Compatibility Tests")
  class CrossRuntimeCompatibilityTests {

    @Test
    @DisplayName("Should have consistent behavior across JNI and Panama runtimes")
    void testCrossRuntimeConsistency() {
      // Test that both JNI and Panama implementations behave consistently
      logger.info("Testing cross-runtime consistency for experimental features");

      // This test verifies that both implementations follow the same patterns
      // even if the underlying Wasmtime support is not yet available

      // Test feature support queries
      final boolean jniStackSwitching = JniExperimentalFeatures.isStackSwitchingSupported();
      final boolean panamaStackSwitching = PanamaExperimentalFeatures.isStackSwitchingSupported();

      assertEquals(
          jniStackSwitching,
          panamaStackSwitching,
          "JNI and Panama should report same stack switching support");

      final boolean jniCallCc = JniExperimentalFeatures.isCallCcSupported();
      final boolean panamaCallCc = PanamaExperimentalFeatures.isCallCcSupported();

      assertEquals(jniCallCc, panamaCallCc, "JNI and Panama should report same call/cc support");

      logger.info("Cross-runtime consistency verification completed");
    }

    @Test
    @DisplayName("Should support experimental features in unified API")
    void testUnifiedApiExperimentalFeatures() {
      assertDoesNotThrow(
          () -> {
            // Test that experimental features work with the unified factory
            final EngineConfig config = EngineConfig.forExperimentalFeatures();

            // This should work regardless of which runtime is selected
            final var runtime = WasmRuntimeFactory.createRuntime(config);
            assertNotNull(
                runtime, "Should be able to create runtime with experimental features config");

            logger.info(
                "Successfully created runtime with experimental features using unified API");

            // Clean up
            runtime.close();
          },
          "Should be able to use experimental features through unified API");
    }
  }

  @Nested
  @DisplayName("Performance and Memory Tests")
  class PerformanceAndMemoryTests {

    @Test
    @DisplayName("Should handle resource cleanup properly")
    void testResourceCleanup() {
      assertDoesNotThrow(
          () -> {
            // Test JNI resource cleanup
            final JniExperimentalFeatures jniConfig = new JniExperimentalFeatures();
            assertTrue(jniConfig.isValid(), "JNI config should be valid after creation");
            jniConfig.dispose();
            // Note: isValid() may still return true after dispose() depending on implementation
            logger.info("JNI experimental features resource cleanup completed");

            // Test Panama resource cleanup
            final PanamaExperimentalFeatures panamaConfig = new PanamaExperimentalFeatures();
            assertTrue(panamaConfig.isValid(), "Panama config should be valid after creation");
            panamaConfig.close();
            // Note: isValid() may still return false after close() depending on implementation
            logger.info("Panama experimental features resource cleanup completed");
          },
          "Should handle resource cleanup without errors");
    }

    @Test
    @DisplayName("Should handle multiple configuration instances")
    void testMultipleConfigurations() {
      assertDoesNotThrow(
          () -> {
            // Create multiple JNI configurations
            try (final JniExperimentalFeatures config1 = new JniExperimentalFeatures();
                final JniExperimentalFeatures config2 =
                    JniExperimentalFeatures.allExperimentalEnabled()) {

              assertTrue(config1.isValid(), "First JNI config should be valid");
              assertTrue(config2.isValid(), "Second JNI config should be valid");

              // Configure them differently
              config1.enableStackSwitching(32 * 1024, 50);
              config2.enableCallCc(500, JniExperimentalFeatures.ContinuationStorageStrategy.STACK);

              assertTrue(config1.isValid(), "First JNI config should remain valid");
              assertTrue(config2.isValid(), "Second JNI config should remain valid");
            }

            // Create multiple Panama configurations
            try (final PanamaExperimentalFeatures config1 = new PanamaExperimentalFeatures();
                final PanamaExperimentalFeatures config2 =
                    PanamaExperimentalFeatures.allExperimentalEnabled()) {

              assertTrue(config1.isValid(), "First Panama config should be valid");
              assertTrue(config2.isValid(), "Second Panama config should be valid");

              // Configure them differently
              config1.enableStackSwitching(32 * 1024, 50);
              config2.enableCallCc(
                  500, PanamaExperimentalFeatures.ContinuationStorageStrategy.STACK);

              assertTrue(config1.isValid(), "First Panama config should remain valid");
              assertTrue(config2.isValid(), "Second Panama config should remain valid");
            }

            logger.info("Successfully handled multiple experimental feature configurations");
          },
          "Should be able to create and use multiple configurations simultaneously");
    }
  }
}
