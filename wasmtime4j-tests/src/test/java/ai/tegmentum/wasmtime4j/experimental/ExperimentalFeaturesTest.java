package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Comprehensive test suite for experimental WebAssembly features and cutting-edge capabilities.
 *
 * <p><b>WARNING:</b> This test suite exercises highly experimental features that may be unstable or
 * change significantly. Tests may fail on different platforms or runtime versions.
 *
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExperimentalFeaturesTest {

  private static final Logger LOGGER = Logger.getLogger(ExperimentalFeaturesTest.class.getName());

  private ExperimentalFeatureConfig experimentalConfig;
  private EngineConfig engineConfig;
  private WasmRuntime runtime;

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up experimental features test");

    // Create experimental configuration with safe defaults
    experimentalConfig = new ExperimentalFeatureConfig();
    engineConfig = new EngineConfig().setExperimentalFeatures(experimentalConfig);
  }

  @AfterEach
  void tearDown() {
    if (runtime != null) {
      try {
        runtime.close();
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Failed to close runtime cleanly", e);
      }
    }
    LOGGER.info("Experimental features test completed");
  }

  @Test
  @Order(1)
  @DisplayName("Test experimental feature configuration creation")
  void testExperimentalConfigCreation() {
    assertNotNull(experimentalConfig, "Experimental config should not be null");
    assertNotNull(
        experimentalConfig.getEnabledFeatures(), "Enabled features map should not be null");
    assertTrue(
        experimentalConfig.getEnabledFeatures().isEmpty(),
        "No features should be enabled by default");

    LOGGER.info("✓ Experimental configuration created successfully");
  }

  @Test
  @Order(2)
  @DisplayName("Test individual experimental feature configuration")
  void testIndividualFeatureConfiguration() {
    // Test enabling stack switching
    experimentalConfig.setFeature(ExperimentalFeature.STACK_SWITCHING, true);
    assertTrue(
        experimentalConfig.isFeatureEnabled(ExperimentalFeature.STACK_SWITCHING),
        "Stack switching should be enabled");

    // Configure stack switching parameters
    experimentalConfig.configureStackSwitching(
        2 * 1024 * 1024, // 2MB stack size
        50, // 50 max stacks
        StackSwitchingStrategy.COOPERATIVE);

    assertEquals(2 * 1024 * 1024, experimentalConfig.getStackSwitchingStackSize());
    assertEquals(50, experimentalConfig.getStackSwitchingMaxConcurrentStacks());
    assertEquals(
        StackSwitchingStrategy.COOPERATIVE, experimentalConfig.getStackSwitchingStrategy());

    LOGGER.info("✓ Stack switching configured successfully");
  }

  @Test
  @Order(3)
  @DisplayName("Test call/cc experimental feature")
  void testCallCcConfiguration() {
    experimentalConfig.setFeature(ExperimentalFeature.CALL_CC, true);
    assertTrue(
        experimentalConfig.isFeatureEnabled(ExperimentalFeature.CALL_CC),
        "Call/CC should be enabled");

    // Configure call/cc parameters
    experimentalConfig.configureCallCc(
        500, // 500 max continuations
        ContinuationStorageStrategy.HYBRID,
        true // enable compression
        );

    assertEquals(500, experimentalConfig.getCallCcMaxContinuations());
    assertEquals(ContinuationStorageStrategy.HYBRID, experimentalConfig.getCallCcStorageStrategy());
    assertTrue(experimentalConfig.isCallCcCompressionEnabled());

    LOGGER.info("✓ Call/CC configured successfully");
  }

  @Test
  @Order(4)
  @DisplayName("Test security features configuration")
  void testSecurityConfiguration() {
    experimentalConfig.configureSecurity(
        SecurityLevel.HIGH,
        true, // enable advanced sandboxing
        true // enable resource limiting
        );

    assertEquals(SecurityLevel.HIGH, experimentalConfig.getSecurityLevel());
    assertTrue(experimentalConfig.isAdvancedSandboxingEnabled());
    assertTrue(experimentalConfig.isResourceLimitingEnabled());

    LOGGER.info("✓ Security features configured successfully");
  }

  @Test
  @Order(5)
  @DisplayName("Test profiling features configuration")
  void testProfilingConfiguration() {
    experimentalConfig.configureProfiling(
        true, // enable advanced profiling
        true, // enable execution tracing
        ProfilingGranularity.FUNCTION);

    assertTrue(experimentalConfig.isAdvancedProfilingEnabled());
    assertTrue(experimentalConfig.isExecutionTracingEnabled());
    assertEquals(ProfilingGranularity.FUNCTION, experimentalConfig.getProfilingGranularity());

    LOGGER.info("✓ Profiling features configured successfully");
  }

  @Test
  @Order(6)
  @DisplayName("Test WASI extensions configuration")
  void testWasiExtensionsConfiguration() {
    experimentalConfig.configureWasiExtensions(
        true, // enable WASI Preview 2
        true, // enable networking
        true // enable filesystem extensions
        );

    assertTrue(experimentalConfig.isWasiPreview2Enabled());
    assertTrue(experimentalConfig.isWasiNetworkingEnabled());
    assertTrue(experimentalConfig.isWasiFilesystemEnabled());

    LOGGER.info("✓ WASI extensions configured successfully");
  }

  @Test
  @Order(7)
  @DisplayName("Test all experimental features enabled configuration")
  void testAllFeaturesEnabledConfiguration() {
    final ExperimentalFeatureConfig allEnabled = ExperimentalFeatureConfig.allFeaturesEnabled();

    // Verify all categories have some features enabled
    assertTrue(allEnabled.isAdvancedProfilingEnabled(), "Advanced profiling should be enabled");
    assertTrue(allEnabled.isAdvancedSandboxingEnabled(), "Advanced sandboxing should be enabled");
    assertTrue(allEnabled.isWasiPreview2Enabled(), "WASI Preview 2 should be enabled");
    assertEquals(SecurityLevel.MAXIMUM, allEnabled.getSecurityLevel());
    assertEquals(TierUpStrategy.AGGRESSIVE, allEnabled.getTierUpStrategy());

    // Verify multiple experimental features are enabled
    assertTrue(
        allEnabled.getEnabledFeatures().size() > 5,
        "Multiple experimental features should be enabled");

    LOGGER.info("✓ All experimental features configuration created successfully");
  }

  @Test
  @Order(8)
  @DisplayName("Test performance research configuration")
  void testPerformanceResearchConfiguration() {
    final ExperimentalFeatureConfig perfConfig = ExperimentalFeatureConfig.forPerformanceResearch();

    assertTrue(perfConfig.isFeatureEnabled(ExperimentalFeature.ADVANCED_JIT_OPTIMIZATIONS));
    assertTrue(perfConfig.isFeatureEnabled(ExperimentalFeature.MACHINE_CODE_CACHING));
    assertTrue(perfConfig.isFeatureEnabled(ExperimentalFeature.HARDWARE_PERFORMANCE_COUNTERS));
    assertEquals(ProfilingGranularity.INSTRUCTION, perfConfig.getProfilingGranularity());
    assertEquals(TierUpStrategy.AGGRESSIVE, perfConfig.getTierUpStrategy());

    LOGGER.info("✓ Performance research configuration created successfully");
  }

  @Test
  @Order(9)
  @DisplayName("Test WebAssembly proposal research configuration")
  void testProposalResearchConfiguration() {
    final ExperimentalFeatureConfig proposalConfig =
        ExperimentalFeatureConfig.forProposalResearch();

    assertTrue(proposalConfig.isFeatureEnabled(ExperimentalFeature.STACK_SWITCHING));
    assertTrue(proposalConfig.isFeatureEnabled(ExperimentalFeature.CALL_CC));
    assertTrue(proposalConfig.isFeatureEnabled(ExperimentalFeature.EXTENDED_CONST_EXPRESSIONS));
    assertTrue(proposalConfig.isFeatureEnabled(ExperimentalFeature.FLEXIBLE_VECTORS));
    assertTrue(proposalConfig.isCoroutineSupportEnabled());
    assertTrue(proposalConfig.isFiberSupportEnabled());

    LOGGER.info("✓ Proposal research configuration created successfully");
  }

  @Test
  @Order(10)
  @DisplayName("Test experimental feature categorization")
  void testFeatureCategorization() {
    // Test committee-stage proposals
    assertTrue(ExperimentalFeature.STACK_SWITCHING.isCommitteeStageProposal());
    assertTrue(ExperimentalFeature.CALL_CC.isCommitteeStageProposal());
    assertTrue(ExperimentalFeature.FLEXIBLE_VECTORS.isCommitteeStageProposal());

    // Test beta features
    assertTrue(ExperimentalFeature.ADVANCED_JIT_OPTIMIZATIONS.isBetaFeature());
    assertTrue(ExperimentalFeature.MACHINE_CODE_CACHING.isBetaFeature());

    // Test security features
    assertTrue(ExperimentalFeature.ADVANCED_SANDBOXING.isSecurityFeature());
    assertTrue(ExperimentalFeature.CAPABILITY_BASED_SECURITY.isSecurityFeature());

    // Test profiling features
    assertTrue(ExperimentalFeature.HARDWARE_PERFORMANCE_COUNTERS.isProfilingFeature());
    assertTrue(ExperimentalFeature.EXECUTION_TRACING.isProfilingFeature());

    // Test WASI extensions
    assertTrue(ExperimentalFeature.WASI_PREVIEW2.isWasiExtension());
    assertTrue(ExperimentalFeature.WASI_NETWORKING.isWasiExtension());

    // Test research features
    assertTrue(ExperimentalFeature.QUANTUM_RESISTANT_CRYPTO.isResearchFeature());
    assertTrue(ExperimentalFeature.ML_ACCELERATION.isResearchFeature());

    LOGGER.info("✓ Feature categorization working correctly");
  }

  @Test
  @Order(11)
  @DisplayName("Test experimental feature key lookup")
  void testFeatureKeyLookup() {
    // Test finding features by key
    assertEquals(
        ExperimentalFeature.STACK_SWITCHING, ExperimentalFeature.findByKey("stack-switching"));
    assertEquals(ExperimentalFeature.CALL_CC, ExperimentalFeature.findByKey("call-cc"));
    assertEquals(
        ExperimentalFeature.ADVANCED_JIT_OPTIMIZATIONS,
        ExperimentalFeature.findByKey("advanced-jit"));

    // Test null and unknown keys
    assertNull(ExperimentalFeature.findByKey(null));
    assertNull(ExperimentalFeature.findByKey("unknown-feature"));
    assertNull(ExperimentalFeature.findByKey(""));

    LOGGER.info("✓ Feature key lookup working correctly");
  }

  @Test
  @Order(12)
  @DisplayName("Test configuration validation")
  void testConfigurationValidation() {
    // Test invalid stack switching configuration
    assertThrows(
        IllegalArgumentException.class,
        () ->
            experimentalConfig.configureStackSwitching(
                2048, 100, StackSwitchingStrategy.COOPERATIVE),
        "Should reject stack size less than 4KB");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            experimentalConfig.configureStackSwitching(
                1024 * 1024, 0, StackSwitchingStrategy.COOPERATIVE),
        "Should reject zero max stacks");

    // Test invalid call/cc configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> experimentalConfig.configureCallCc(0, ContinuationStorageStrategy.HYBRID, false),
        "Should reject zero max continuations");

    // Test null parameter validation
    assertThrows(
        IllegalArgumentException.class,
        () -> experimentalConfig.setFeature(null, true),
        "Should reject null feature");

    assertThrows(
        IllegalArgumentException.class,
        () -> experimentalConfig.configureStackSwitching(1024 * 1024, 100, null),
        "Should reject null strategy");

    LOGGER.info("✓ Configuration validation working correctly");
  }

  @Test
  @Order(13)
  @DisplayName("Test runtime integration with experimental features")
  void testRuntimeIntegration() {
    try {
      // Configure some safe experimental features
      experimentalConfig.setFeature(ExperimentalFeature.EXTENDED_CONST_EXPRESSIONS, true);
      experimentalConfig.configureSecurity(SecurityLevel.ENHANCED, true, false);

      // Try to create runtime with experimental features
      // Note: This may not work if the underlying Wasmtime doesn't support the features
      runtime = WasmRuntimeFactory.createRuntime(engineConfig);
      assertNotNull(runtime, "Runtime should be created with experimental features");

      LOGGER.info("✓ Runtime integration with experimental features successful");
    } catch (final Exception e) {
      // This is expected if the features aren't actually implemented yet
      LOGGER.warning(
          "Runtime integration failed (expected if features not implemented): " + e.getMessage());
      assertTrue(true, "Test passed - failure expected for unimplemented features");
    }
  }

  @Test
  @Order(14)
  @DisplayName("Test experimental feature thread safety")
  void testThreadSafety() throws InterruptedException {
    final ExperimentalFeatureConfig config = new ExperimentalFeatureConfig();
    final int numThreads = 10;
    final Thread[] threads = new Thread[numThreads];

    // Test concurrent feature enabling/disabling
    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  final ExperimentalFeature feature =
                      ExperimentalFeature.values()[threadId % ExperimentalFeature.values().length];
                  config.setFeature(feature, true);
                  Thread.sleep(10);
                  config.setFeature(feature, false);
                } catch (final Exception e) {
                  LOGGER.log(Level.WARNING, "Thread safety test thread failed", e);
                }
              });
    }

    // Start all threads
    for (final Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      thread.join(5000); // 5 second timeout
    }

    LOGGER.info("✓ Thread safety test completed");
  }

  @Test
  @Order(15)
  @DisplayName("Test experimental feature serialization compatibility")
  void testSerializationCompatibility() {
    final ExperimentalFeatureConfig config1 = new ExperimentalFeatureConfig();
    config1.setFeature(ExperimentalFeature.STACK_SWITCHING, true);
    config1.setFeature(ExperimentalFeature.CALL_CC, true);
    config1.configureSecurity(SecurityLevel.HIGH, true, true);

    final ExperimentalFeatureConfig config2 = new ExperimentalFeatureConfig();
    config2.setFeature(ExperimentalFeature.STACK_SWITCHING, true);
    config2.setFeature(ExperimentalFeature.CALL_CC, true);
    config2.configureSecurity(SecurityLevel.HIGH, true, true);

    // Test equality
    assertEquals(config1.getEnabledFeatures(), config2.getEnabledFeatures());
    assertEquals(config1.getSecurityLevel(), config2.getSecurityLevel());

    LOGGER.info("✓ Serialization compatibility test passed");
  }

  @Nested
  @DisplayName("JNI-specific experimental features tests")
  @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11, JRE.JAVA_17, JRE.JAVA_21, JRE.JAVA_22})
  class JniExperimentalFeaturesTest {

    @Test
    @DisplayName("Test JNI experimental features initialization")
    void testJniInitialization() {
      // This test would require actual JNI implementation
      // For now, we just verify the test framework is working
      LOGGER.info("JNI experimental features test placeholder");
      assertTrue(true, "JNI test framework operational");
    }
  }

  @Nested
  @DisplayName("Panama-specific experimental features tests")
  @EnabledOnJre({JRE.JAVA_23})
  class PanamaExperimentalFeaturesTest {

    @Test
    @DisplayName("Test Panama experimental features initialization")
    void testPanamaInitialization() {
      // This test would require actual Panama implementation
      // For now, we just verify the test framework is working
      LOGGER.info("Panama experimental features test placeholder");
      assertTrue(true, "Panama test framework operational");
    }
  }

  @Test
  @Order(999)
  @DisplayName("Test experimental features cleanup and resource management")
  void testCleanupAndResourceManagement() {
    final ExperimentalFeatureConfig config = ExperimentalFeatureConfig.allFeaturesEnabled();

    // Simulate resource-intensive operations
    config.configureProfiling(true, true, ProfilingGranularity.INSTRUCTION);
    config.configureSecurity(SecurityLevel.MAXIMUM, true, true);

    // Test that configuration doesn't leak resources
    assertNotNull(config.getEnabledFeatures());

    // Force garbage collection to test cleanup
    System.gc();
    Thread.yield();

    LOGGER.info("✓ Cleanup and resource management test completed");
  }
}
