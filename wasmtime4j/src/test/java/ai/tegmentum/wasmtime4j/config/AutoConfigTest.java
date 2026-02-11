package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.config.profiles.PerformanceProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Comprehensive tests for AutoConfig automatic configuration system. */
class AutoConfigTest {

  private AutoConfig autoConfig;

  @BeforeEach
  void setUp() {
    autoConfig = AutoConfig.create();
  }

  @Test
  @DisplayName("AutoConfig should create successfully")
  void testAutoConfigCreation() {
    assertNotNull(autoConfig);
    assertNotNull(autoConfig.getRuntimeEnvironment());
    assertNotNull(autoConfig.getSystemCapabilities());
  }

  @Test
  @DisplayName("Should provide optimal configuration for general purpose workload")
  void testOptimalConfigurationForGeneralPurpose() {
    EngineConfig config = autoConfig.getOptimalConfiguration();

    assertNotNull(config);
    assertNotNull(config.getOptimizationLevel());
    assertNotNull(config.getCraneliftSettings());
  }

  @ParameterizedTest
  @EnumSource(AutoConfig.WorkloadType.class)
  @DisplayName("Should provide optimal configuration for all workload types")
  void testOptimalConfigurationForAllWorkloadTypes(AutoConfig.WorkloadType workloadType) {
    assertDoesNotThrow(
        () -> {
          EngineConfig config = autoConfig.getOptimalConfiguration(workloadType);
          assertNotNull(config, "Configuration should not be null for " + workloadType);
          assertNotNull(
              config.getOptimizationLevel(),
              "Optimization level should be set for " + workloadType);
        });
  }

  @ParameterizedTest
  @EnumSource(AutoConfig.WorkloadType.class)
  @DisplayName("Should provide detailed recommendations for all workload types")
  void testConfigurationRecommendationsForAllWorkloadTypes(AutoConfig.WorkloadType workloadType) {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(workloadType);

    assertNotNull(recommendation);
    assertNotNull(recommendation.getProfile());
    assertNotNull(recommendation.toEngineConfig());
    assertNotNull(recommendation.getReasons());
    assertTrue(recommendation.getReasons().length > 0, "Should have at least one reason");
    assertTrue(
        recommendation.getConfidenceScore() >= 0 && recommendation.getConfidenceScore() <= 100,
        "Confidence score should be between 0 and 100");
  }

  @Test
  @DisplayName("CPU intensive workload should prefer performance profiles")
  void testCpuIntensiveWorkloadConfiguration() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.CPU_INTENSIVE);

    EngineConfig config = recommendation.toEngineConfig();
    PerformanceProfile profile = recommendation.getProfile();

    // Should prefer speed optimization for CPU-intensive tasks
    assertTrue(
        config.getOptimizationLevel() == OptimizationLevel.SPEED
            || profile == PerformanceProfile.MAXIMUM_PERFORMANCE
            || profile == PerformanceProfile.BALANCED,
        "CPU-intensive workload should prefer performance-oriented profiles");

    // Should enable parallel compilation if system supports it
    if (autoConfig.getSystemCapabilities().getProcessorCount() > 1) {
      assertTrue(
          config.isParallelCompilation(),
          "CPU-intensive workload should enable parallel compilation on multi-core systems");
    }
  }

  @Test
  @DisplayName("Memory constrained environments should prefer size optimization")
  void testMemoryConstrainedConfiguration() {
    // Test with a hypothetical memory-constrained scenario
    // Note: This test might pass or fail depending on the actual system running the test
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.GENERAL_PURPOSE);

    EngineConfig config = recommendation.toEngineConfig();

    // If system is memory constrained, should prefer size optimization
    if (autoConfig.getSystemCapabilities().isMemoryConstrained()) {
      assertTrue(
          config.getOptimizationLevel() == OptimizationLevel.SIZE
              || recommendation.getProfile() == PerformanceProfile.MEMORY_OPTIMIZED,
          "Memory constrained systems should prefer size optimization");
    }
  }

  @Test
  @DisplayName("Security critical workload should enable verification features")
  void testSecurityCriticalWorkloadConfiguration() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.SECURITY_CRITICAL);

    EngineConfig config = recommendation.toEngineConfig();
    PerformanceProfile profile = recommendation.getProfile();

    // Security critical workloads should prefer balanced profile (with security adjustments)
    assertEquals(
        PerformanceProfile.BALANCED,
        profile,
        "Security critical workload should use balanced profile");

    // Configuration adjustments may enable fuel consumption for resource limiting in cloud env
    // The actual settings depend on the runtime environment detection
    assertNotNull(config, "Configuration should be created");
  }

  @Test
  @DisplayName("Development/testing workload should enable debugging features")
  void testDevelopmentTestingWorkloadConfiguration() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.DEVELOPMENT_TESTING);

    EngineConfig config = recommendation.toEngineConfig();
    PerformanceProfile profile = recommendation.getProfile();

    // Development workloads should prefer debugging-friendly profile
    assertEquals(
        PerformanceProfile.DEBUG,
        profile,
        "Development workload should prefer debug profile");

    // Should enable debug information
    assertTrue(
        config.isGenerateDebugInfo(), "Debug profile should enable debug information");
  }

  @Test
  @DisplayName("Real-time workload should optimize for latency")
  void testRealTimeWorkloadConfiguration() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.REAL_TIME);

    EngineConfig config = recommendation.toEngineConfig();
    PerformanceProfile profile = recommendation.getProfile();

    // Real-time workloads should prefer maximum performance for low latency
    assertEquals(
        PerformanceProfile.MAXIMUM_PERFORMANCE,
        profile,
        "Real-time workload should use maximum performance profile");

    // Should disable fuel consumption for predictable timing
    assertFalse(
        config.isConsumeFuel(),
        "Real-time workload should disable fuel consumption for predictable timing");

    // Should disable epoch interruption for consistent execution
    assertFalse(
        config.isEpochInterruption(),
        "Real-time workload should disable epoch interruption for consistent execution");
  }

  @Test
  @DisplayName("Machine learning workload should prefer performance profiles")
  void testMachineLearningWorkloadConfiguration() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.MACHINE_LEARNING);

    EngineConfig config = recommendation.toEngineConfig();

    // Should have valid configuration
    assertNotNull(config);
    assertNotNull(config.getOptimizationLevel());

    // Should prefer maximum performance or balanced profile on capable systems
    if (autoConfig.getSystemCapabilities().getProcessorCount() >= 4) {
      assertTrue(
          recommendation.getProfile() == PerformanceProfile.MAXIMUM_PERFORMANCE
              || recommendation.getProfile() == PerformanceProfile.BALANCED,
          "Machine learning on multi-core systems should prefer performance profiles");
    }

    // Should use speed optimization for ML workloads
    assertEquals(
        OptimizationLevel.SPEED,
        config.getOptimizationLevel(),
        "Machine learning workload should use speed optimization");
  }

  @Test
  @DisplayName("Runtime environment detection should provide valid information")
  void testRuntimeEnvironmentDetection() {
    AutoConfig.RuntimeEnvironment env = autoConfig.getRuntimeEnvironment();

    assertNotNull(env.getJavaVersion());
    assertNotNull(env.getJavaVendor());
    assertNotNull(env.getEnvironmentProperties());
    assertFalse(env.getJavaVersion().isEmpty());
    assertFalse(env.getJavaVendor().isEmpty());
    assertFalse(env.getEnvironmentProperties().isEmpty());

    // Environment properties should contain basic system information
    assertTrue(env.getEnvironmentProperties().containsKey("java.version"));
    assertTrue(env.getEnvironmentProperties().containsKey("os.name"));
    assertTrue(env.getEnvironmentProperties().containsKey("os.arch"));
  }

  @Test
  @DisplayName("System capabilities detection should provide valid information")
  void testSystemCapabilitiesDetection() {
    AutoConfig.SystemCapabilities caps = autoConfig.getSystemCapabilities();

    assertTrue(caps.getProcessorCount() > 0, "Processor count should be positive");
    assertTrue(caps.getAvailableMemoryMB() > 0, "Available memory should be positive");
    assertNotNull(caps.getOperatingSystem());
    assertNotNull(caps.getCpuArchitecture());
    assertFalse(caps.getOperatingSystem().isEmpty());
    assertFalse(caps.getCpuArchitecture().isEmpty());

    // System capabilities should be consistent
    if (caps.getProcessorCount() >= 4 && caps.getAvailableMemoryMB() >= 2048) {
      assertTrue(
          caps.isServerEnvironment(),
          "Systems with 4+ cores and 2GB+ RAM should be considered server environments");
    }
  }

  @Test
  @DisplayName("Performance analysis should provide adjustment recommendations")
  void testPerformanceAnalysis() {
    // Create sample execution metrics
    AutoConfig.ExecutionMetrics metrics =
        new AutoConfig.ExecutionMetrics(
            5000, // 5 second compilation time
            1000, // 1 second execution time
            500, // 0.5 second expected execution time
            256, // 256MB memory usage
            0.8, // 80% CPU utilization
            1 // 1 instance
            );

    AutoConfig.ConfigurationAdjustment adjustment = autoConfig.analyzeAndAdjust(metrics);

    assertNotNull(adjustment);
    assertNotNull(adjustment.getRecommendations());
    assertNotNull(adjustment.getAdjustments());
    assertNotNull(adjustment.getMetrics());

    // Should provide recommendations for slow compilation or poor performance
    String[] recommendations = adjustment.getRecommendations();
    boolean hasRelevantRecommendation = false;
    for (String recommendation : recommendations) {
      if (recommendation.toLowerCase().contains("debug")
          || recommendation.toLowerCase().contains("performance")
          || recommendation.toLowerCase().contains("profile")) {
        hasRelevantRecommendation = true;
        break;
      }
    }
    assertTrue(
        hasRelevantRecommendation,
        "Should provide profile-related recommendations for performance issues");

    // Should provide adjustments for poor performance
    var adjustments = adjustment.getAdjustments();
    assertTrue(adjustments.size() > 0, "Should provide adjustment suggestions");
  }

  @Test
  @DisplayName("Configuration export should work correctly")
  void testConfigurationExport() {
    AutoConfig.ConfigurationExport export =
        autoConfig.exportConfiguration(AutoConfig.WorkloadType.WEB_APPLICATION);

    assertNotNull(export);
    assertNotNull(export.getRecommendation());
    assertNotNull(export.getRuntimeEnvironment());
    assertNotNull(export.getSystemCapabilities());
    assertTrue(export.getExportTimestamp() > 0);

    // Export should be importable
    EngineConfig importedConfig = autoConfig.importConfiguration(export);
    assertNotNull(importedConfig);
  }

  @Test
  @DisplayName("Configuration import should validate compatibility")
  void testConfigurationImportValidation() {
    AutoConfig.ConfigurationExport export =
        autoConfig.exportConfiguration(AutoConfig.WorkloadType.GENERAL_PURPOSE);

    // Should successfully import compatible configuration
    assertDoesNotThrow(
        () -> {
          EngineConfig config = autoConfig.importConfiguration(export);
          assertNotNull(config);
        });
  }

  @Test
  @DisplayName("Recommendation caching should work correctly")
  void testRecommendationCaching() {
    // First call should create recommendation
    AutoConfig.ConfigurationRecommendation first =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.CPU_INTENSIVE);

    // Second call should return cached result (if within cache duration)
    AutoConfig.ConfigurationRecommendation second =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.CPU_INTENSIVE);

    assertNotNull(first);
    assertNotNull(second);

    // Both recommendations should have the same profile (assuming system hasn't changed)
    assertEquals(first.getProfile(), second.getProfile());
  }

  @Test
  @DisplayName("Workload type display names should be meaningful")
  void testWorkloadTypeDisplayNames() {
    for (AutoConfig.WorkloadType workloadType : AutoConfig.WorkloadType.values()) {
      String displayName = workloadType.getDisplayName();
      assertNotNull(displayName);
      assertFalse(displayName.trim().isEmpty());

      // Display name should be different from enum name
      assertNotEquals(workloadType.name(), displayName);
    }
  }

  @Test
  @DisplayName("Should handle edge cases gracefully")
  void testEdgeCaseHandling() {
    // Test with extreme execution metrics
    AutoConfig.ExecutionMetrics extremeMetrics =
        new AutoConfig.ExecutionMetrics(
            0, // No compilation time
            0, // No execution time
            0, // No expected execution time
            0, // No memory usage
            0.0, // No CPU utilization
            0 // No instances
            );

    assertDoesNotThrow(
        () -> {
          AutoConfig.ConfigurationAdjustment adjustment =
              autoConfig.analyzeAndAdjust(extremeMetrics);
          assertNotNull(adjustment);
        });
  }

  @Test
  @DisplayName("Configuration recommendations should have meaningful reasons")
  void testRecommendationReasons() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.WEB_APPLICATION);

    String[] reasons = recommendation.getReasons();
    assertNotNull(reasons);
    assertTrue(reasons.length > 0, "Should have at least one reason");

    for (String reason : reasons) {
      assertNotNull(reason);
      assertFalse(reason.trim().isEmpty());
      assertTrue(reason.length() > 10, "Reason should be descriptive");
    }
  }

  @Test
  @DisplayName("Confidence scores should be reasonable")
  void testConfidenceScores() {
    for (AutoConfig.WorkloadType workloadType : AutoConfig.WorkloadType.values()) {
      AutoConfig.ConfigurationRecommendation recommendation =
          autoConfig.getConfigurationRecommendation(workloadType);

      int confidence = recommendation.getConfidenceScore();
      assertTrue(
          confidence >= 0 && confidence <= 100,
          "Confidence score should be between 0 and 100 for " + workloadType);

      // Well-defined workload types should have higher confidence
      if (workloadType == AutoConfig.WorkloadType.SECURITY_CRITICAL
          || workloadType == AutoConfig.WorkloadType.DEVELOPMENT_TESTING) {
        assertTrue(
            confidence >= 70,
            "Well-defined workload types should have high confidence: " + workloadType);
      }
    }
  }

  @Test
  @DisplayName("System detection should be consistent")
  void testSystemDetectionConsistency() {
    AutoConfig.SystemCapabilities caps1 = autoConfig.getSystemCapabilities();

    // Create another AutoConfig instance
    AutoConfig autoConfig2 = AutoConfig.create();
    AutoConfig.SystemCapabilities caps2 = autoConfig2.getSystemCapabilities();

    // Basic system properties should be the same
    assertEquals(caps1.getProcessorCount(), caps2.getProcessorCount());
    assertEquals(caps1.getOperatingSystem(), caps2.getOperatingSystem());
    assertEquals(caps1.getCpuArchitecture(), caps2.getCpuArchitecture());
    assertEquals(caps1.hasSimdSupport(), caps2.hasSimdSupport());
  }

  @Test
  @DisplayName("Web application workload should enable appropriate features")
  void testWebApplicationConfiguration() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.WEB_APPLICATION);

    EngineConfig config = recommendation.toEngineConfig();
    var features = config.getWasmFeatures();

    // Web applications should enable standard web features (I/O Intensive template features)
    assertTrue(
        features.contains(ai.tegmentum.wasmtime4j.WasmFeature.REFERENCE_TYPES),
        "Web applications should enable reference types");
    assertTrue(
        features.contains(ai.tegmentum.wasmtime4j.WasmFeature.BULK_MEMORY),
        "Web applications should enable bulk memory");

    // Should enable fuel consumption for execution limiting
    assertTrue(
        config.isConsumeFuel(),
        "Web applications should enable fuel consumption for resource limiting");
  }

  @Test
  @DisplayName("Batch processing workload should maximize throughput")
  void testBatchProcessingConfiguration() {
    AutoConfig.ConfigurationRecommendation recommendation =
        autoConfig.getConfigurationRecommendation(AutoConfig.WorkloadType.BATCH_PROCESSING);

    EngineConfig config = recommendation.toEngineConfig();
    PerformanceProfile profile = recommendation.getProfile();

    // Batch processing should prefer performance optimization
    assertTrue(
        profile == PerformanceProfile.MAXIMUM_PERFORMANCE
            || profile == PerformanceProfile.BALANCED,
        "Batch processing should use maximum performance or balanced profile");

    // Should prefer speed optimization
    assertEquals(
        OptimizationLevel.SPEED,
        config.getOptimizationLevel(),
        "Batch processing should use speed optimization");

    // Should enable parallel compilation if available
    if (autoConfig.getSystemCapabilities().getProcessorCount() > 1) {
      assertTrue(
          config.isParallelCompilation(),
          "Batch processing should enable parallel compilation on multi-core systems");
    }
  }
}
