package ai.tegmentum.wasmtime4j.config.profiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.WasmFeature;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Comprehensive tests for PerformanceProfile configuration profiles. */
class PerformanceProfileTest {

  @Test
  @DisplayName("All profiles should create valid configurations")
  void testAllProfilesCreateValidConfigurations() {
    for (PerformanceProfile profile : PerformanceProfile.values()) {
      assertDoesNotThrow(
          () -> {
            EngineConfig config = profile.createConfig();
            assertNotNull(config, "Profile " + profile + " should create non-null config");

            // Verify basic configuration properties are set
            assertNotNull(config.getOptimizationLevel(), "Optimization level should be set");
            assertNotNull(
                config.getCraneliftSettings(), "Cranelift settings should be initialized");
          },
          "Profile " + profile + " should not throw when creating config");
    }
  }

  @Test
  @DisplayName("Maximum Performance profile should optimize for speed")
  void testMaximumPerformanceProfile() {
    EngineConfig config = PerformanceProfile.MAXIMUM_PERFORMANCE.createConfig();

    assertEquals(OptimizationLevel.SPEED, config.getOptimizationLevel());
    assertTrue(config.isParallelCompilation());
    assertFalse(config.isCraneliftDebugVerifier());
    assertFalse(config.isGenerateDebugInfo());
    assertFalse(config.isConsumeFuel());

    // Verify Cranelift settings for maximum performance
    var settings = config.getCraneliftSettings();
    assertTrue(settings.containsKey("opt_level"));
    assertEquals("speed", settings.get("opt_level"));
    assertEquals("false", settings.get("enable_verifier"));
  }

  @Test
  @DisplayName("Memory Optimized profile should minimize memory usage")
  void testMemoryOptimizedProfile() {
    EngineConfig config = PerformanceProfile.MEMORY_OPTIMIZED.createConfig();

    assertEquals(OptimizationLevel.SIZE, config.getOptimizationLevel());
    assertFalse(config.isParallelCompilation());
    assertEquals(512 * 1024, config.getMaxWasmStack()); // 512KB stack limit

    var settings = config.getCraneliftSettings();
    assertEquals("size", settings.get("opt_level"));
    assertEquals("true", settings.get("enable_probestack"));
  }

  @Test
  @DisplayName("Debug profile should enable all debugging features with fast compilation")
  void testDebugProfile() {
    EngineConfig config = PerformanceProfile.DEBUG.createConfig();

    assertEquals(OptimizationLevel.NONE, config.getOptimizationLevel());
    assertTrue(config.isParallelCompilation());
    assertTrue(config.isCraneliftDebugVerifier());
    assertTrue(config.isGenerateDebugInfo());
    assertTrue(config.isConsumeFuel());
    assertTrue(config.isEpochInterruption());

    var settings = config.getCraneliftSettings();
    assertEquals("none", settings.get("opt_level"));
    assertEquals("true", settings.get("enable_verifier"));
    assertEquals("true", settings.get("enable_bounds_checks"));
  }

  @ParameterizedTest
  @EnumSource(PerformanceProfile.class)
  @DisplayName("All profiles should have valid performance characteristics")
  void testPerformanceCharacteristics(PerformanceProfile profile) {
    PerformanceProfile.PerformanceCharacteristics characteristics =
        profile.getPerformanceCharacteristics();

    assertNotNull(characteristics);
    assertTrue(
        characteristics.getPerformanceScore() >= 0 && characteristics.getPerformanceScore() <= 100,
        "Performance score should be between 0 and 100");
    assertTrue(
        characteristics.getMemoryEfficiency() >= 0 && characteristics.getMemoryEfficiency() <= 100,
        "Memory efficiency should be between 0 and 100");
    assertTrue(
        characteristics.getCompilationSpeed() >= 0 && characteristics.getCompilationSpeed() <= 100,
        "Compilation speed should be between 0 and 100");
    assertTrue(
        characteristics.getSecurityLevel() >= 0 && characteristics.getSecurityLevel() <= 100,
        "Security level should be between 0 and 100");
    assertTrue(
        characteristics.getPowerEfficiency() >= 0 && characteristics.getPowerEfficiency() <= 100,
        "Power efficiency should be between 0 and 100");
    assertTrue(
        characteristics.getOverallScore() >= 0 && characteristics.getOverallScore() <= 100,
        "Overall score should be between 0 and 100");
  }

  @Test
  @DisplayName("Performance characteristics should reflect profile priorities")
  void testPerformanceCharacteristicsReflectPriorities() {
    var maxPerf = PerformanceProfile.MAXIMUM_PERFORMANCE.getPerformanceCharacteristics();
    var memOpt = PerformanceProfile.MEMORY_OPTIMIZED.getPerformanceCharacteristics();
    var debug = PerformanceProfile.DEBUG.getPerformanceCharacteristics();

    // Maximum performance should have highest performance score
    assertTrue(
        maxPerf.getPerformanceScore() > memOpt.getPerformanceScore(),
        "Maximum performance should outperform memory optimized");
    assertTrue(
        maxPerf.getPerformanceScore() > debug.getPerformanceScore(),
        "Maximum performance should outperform debug");

    // Memory optimized should have highest memory efficiency
    assertTrue(
        memOpt.getMemoryEfficiency() > maxPerf.getMemoryEfficiency(),
        "Memory optimized should be more memory efficient than maximum performance");

    // Debug should have highest compilation speed (no optimizations)
    assertTrue(
        debug.getCompilationSpeed() > maxPerf.getCompilationSpeed(),
        "Debug should compile faster than maximum performance");

    // Debug should have highest security level (verifier enabled)
    assertTrue(
        debug.getSecurityLevel() > maxPerf.getSecurityLevel(),
        "Debug should be more secure than maximum performance");

    // Memory optimized should have highest power efficiency
    assertTrue(
        memOpt.getPowerEfficiency() > maxPerf.getPowerEfficiency(),
        "Memory optimized should be more power efficient than maximum performance");
  }

  @ParameterizedTest
  @EnumSource(PerformanceProfile.class)
  @DisplayName("All profiles should have recommended use cases")
  void testRecommendedUseCases(PerformanceProfile profile) {
    String[] useCases = profile.getRecommendedUseCases();

    assertNotNull(useCases);
    assertTrue(useCases.length > 0, "Profile should have at least one recommended use case");

    for (String useCase : useCases) {
      assertNotNull(useCase);
      assertFalse(useCase.trim().isEmpty(), "Use case should not be empty");
    }
  }

  @Test
  @DisplayName("Production suitability should be correctly identified")
  void testProductionSuitability() {
    // Production suitable profiles
    assertTrue(PerformanceProfile.MAXIMUM_PERFORMANCE.isProductionSuitable());
    assertTrue(PerformanceProfile.BALANCED.isProductionSuitable());
    assertTrue(PerformanceProfile.MEMORY_OPTIMIZED.isProductionSuitable());

    // Development/testing profiles
    assertFalse(PerformanceProfile.DEBUG.isProductionSuitable());
  }

  @Test
  @DisplayName("Suitable profiles should be returned for different environments")
  void testGetSuitableProfiles() {
    PerformanceProfile[] profiles = PerformanceProfile.getSuitableProfiles();

    assertNotNull(profiles);
    assertTrue(profiles.length > 0, "Should return at least one suitable profile");

    // All returned profiles should be valid
    for (PerformanceProfile profile : profiles) {
      assertNotNull(profile);
      assertDoesNotThrow(() -> profile.createConfig());
    }
  }

  @Test
  @DisplayName("Recommended profile should be valid")
  void testGetRecommendedProfile() {
    PerformanceProfile recommended = PerformanceProfile.getRecommendedProfile();

    assertNotNull(recommended);
    assertDoesNotThrow(() -> recommended.createConfig());
  }

  @Test
  @DisplayName("Profile comparison should work correctly")
  void testProfileComparison() {
    PerformanceProfile maxPerf = PerformanceProfile.MAXIMUM_PERFORMANCE;
    PerformanceProfile memOpt = PerformanceProfile.MEMORY_OPTIMIZED;
    PerformanceProfile debug = PerformanceProfile.DEBUG;

    // Maximum performance should compare higher than memory optimized
    assertTrue(maxPerf.comparePerformance(memOpt) > 0);

    // Maximum performance should compare higher than debug
    assertTrue(maxPerf.comparePerformance(debug) > 0);

    // Memory optimized should compare higher than debug
    assertTrue(memOpt.comparePerformance(debug) > 0);

    // Profile should equal itself
    assertEquals(0, maxPerf.comparePerformance(maxPerf));
  }

  @Test
  @DisplayName("Profile application should preserve base configuration")
  void testProfileApplicationPreservesBaseConfig() {
    EngineConfig baseConfig =
        new EngineConfig()
            .setMaxWasmStack(1024 * 1024) // 1MB
            .setAsyncStackSize(512 * 1024); // 512KB

    EngineConfig modifiedConfig = PerformanceProfile.BALANCED.applyTo(baseConfig);

    assertNotNull(modifiedConfig);
    // The profile should have applied its settings
    assertEquals(OptimizationLevel.SPEED, modifiedConfig.getOptimizationLevel());
    assertTrue(modifiedConfig.isParallelCompilation());

    // But the base config object should be unchanged (immutable)
    assertNotSame(baseConfig, modifiedConfig);
  }

  @Test
  @DisplayName("Profile display names and descriptions should be meaningful")
  void testProfileDisplayNamesAndDescriptions() {
    for (PerformanceProfile profile : PerformanceProfile.values()) {
      assertNotNull(profile.getDisplayName());
      assertFalse(profile.getDisplayName().trim().isEmpty());

      assertNotNull(profile.getDescription());
      assertFalse(profile.getDescription().trim().isEmpty());

      // Display name should be more concise than description
      assertTrue(profile.getDisplayName().length() < profile.getDescription().length());
    }
  }

  @Test
  @DisplayName("Cranelift settings should be valid for all profiles")
  void testCraneliftSettingsValidity() {
    for (PerformanceProfile profile : PerformanceProfile.values()) {
      EngineConfig config = profile.createConfig();
      var settings = config.getCraneliftSettings();

      assertNotNull(settings);

      // All profiles should have basic optimization level setting
      assertTrue(settings.containsKey("opt_level"), "Profile " + profile + " should set opt_level");

      String optLevel = settings.get("opt_level");
      assertTrue(
          Set.of("none", "speed", "size", "speed_and_size").contains(optLevel),
          "Invalid optimization level: " + optLevel);

      // Verifier setting should be present and valid
      if (settings.containsKey("enable_verifier")) {
        String verifier = settings.get("enable_verifier");
        assertTrue(
            Set.of("true", "false").contains(verifier), "Invalid verifier setting: " + verifier);
      }
    }
  }

  @Test
  @DisplayName("Performance characteristics toString should be informative")
  void testPerformanceCharacteristicsToString() {
    PerformanceProfile.PerformanceCharacteristics characteristics =
        PerformanceProfile.BALANCED.getPerformanceCharacteristics();

    String toString = characteristics.toString();
    assertNotNull(toString);
    assertFalse(toString.isEmpty());

    // Should contain key metrics
    assertTrue(toString.contains("performance="));
    assertTrue(toString.contains("memory="));
    assertTrue(toString.contains("compilation="));
    assertTrue(toString.contains("security="));
    assertTrue(toString.contains("power="));
    assertTrue(toString.contains("overall="));
  }

  @Test
  @DisplayName("Configuration should handle null engine config gracefully")
  void testNullEngineConfigHandling() {
    for (PerformanceProfile profile : PerformanceProfile.values()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            profile.applyTo(null);
          },
          "Profile " + profile + " should reject null engine config");
    }
  }

  @Test
  @DisplayName("WebAssembly features should be appropriate for profiles")
  void testWebAssemblyFeatureConfiguration() {
    // Check that profiles create valid feature sets
    EngineConfig debugConfig = PerformanceProfile.DEBUG.createConfig();
    Set<WasmFeature> debugFeatures = debugConfig.getWasmFeatures();
    // Debug profile should have standard features

    EngineConfig maxPerfConfig = PerformanceProfile.MAXIMUM_PERFORMANCE.createConfig();
    Set<WasmFeature> maxPerfFeatures = maxPerfConfig.getWasmFeatures();
    // Maximum performance might enable all available features

    // Features should be non-null for all profiles
    assertNotNull(debugFeatures);
    assertNotNull(maxPerfFeatures);
  }
}
