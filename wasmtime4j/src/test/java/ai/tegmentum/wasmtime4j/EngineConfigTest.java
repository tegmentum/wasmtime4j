package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for EngineConfig class.
 *
 * <p>Tests all configuration options, builder patterns, validation, and factory methods to ensure
 * the configuration API works correctly across all supported features.
 */
@DisplayName("EngineConfig Tests")
final class EngineConfigTest {

  @Nested
  @DisplayName("Basic Configuration Tests")
  class BasicConfigurationTests {

    @Test
    @DisplayName("Default configuration has expected values")
    void testDefaultConfiguration() {
      final EngineConfig config = new EngineConfig();

      // Verify default values
      assertFalse(config.isDebugInfo(), "Debug info should be disabled by default");
      assertFalse(config.isConsumeFuel(), "Fuel consumption should be disabled by default");
      assertEquals(0L, config.getFuelAmount(), "Fuel amount should be 0 by default");
      assertEquals(
          OptimizationLevel.SPEED,
          config.getOptimizationLevel(),
          "Default optimization should be SPEED");
      assertTrue(
          config.isParallelCompilation(), "Parallel compilation should be enabled by default");
      assertFalse(
          config.isCraneliftDebugVerifier(),
          "Cranelift debug verifier should be disabled by default");

      // WASI and runtime features
      assertFalse(config.isWasiEnabled(), "WASI should be disabled by default");
      assertFalse(
          config.isEpochInterruptionEnabled(), "Epoch interruption should be disabled by default");
      assertFalse(config.isMemoryLimitEnabled(), "Memory limits should be disabled by default");
      assertEquals(0L, config.getMemoryLimit(), "Memory limit should be 0 by default");

      // WebAssembly features
      assertTrue(
          config.isWasmBacktraceDetails(), "WASM backtrace details should be enabled by default");
      assertTrue(
          config.isWasmReferenceTypes(), "WASM reference types should be enabled by default");
      assertTrue(config.isWasmSimd(), "WASM SIMD should be enabled by default");
      assertFalse(config.isWasmRelaxedSimd(), "WASM relaxed SIMD should be disabled by default");
      assertTrue(config.isWasmMultiValue(), "WASM multi-value should be enabled by default");
      assertTrue(config.isWasmBulkMemory(), "WASM bulk memory should be enabled by default");
      assertFalse(config.isWasmThreads(), "WASM threads should be disabled by default");
      assertFalse(config.isWasmTailCall(), "WASM tail call should be disabled by default");
      assertFalse(config.isWasmMultiMemory(), "WASM multi-memory should be disabled by default");
      assertFalse(config.isWasmMemory64(), "WASM memory64 should be disabled by default");
    }

    @Test
    @DisplayName("Configuration builder pattern works correctly")
    void testBuilderPattern() {
      final EngineConfig config =
          new EngineConfig()
              .debugInfo(true)
              .consumeFuel(true)
              .fuelAmount(1000L)
              .optimizationLevel(OptimizationLevel.NONE)
              .parallelCompilation(false)
              .craneliftDebugVerifier(true)
              .wasiEnabled(true)
              .epochInterruption(true)
              .memoryLimitEnabled(true)
              .memoryLimit(1024 * 1024L); // 1MB

      // Verify all set values
      assertTrue(config.isDebugInfo(), "Debug info should be enabled");
      assertTrue(config.isConsumeFuel(), "Fuel consumption should be enabled");
      assertEquals(1000L, config.getFuelAmount(), "Fuel amount should be 1000");
      assertEquals(
          OptimizationLevel.NONE, config.getOptimizationLevel(), "Optimization should be NONE");
      assertFalse(config.isParallelCompilation(), "Parallel compilation should be disabled");
      assertTrue(config.isCraneliftDebugVerifier(), "Cranelift debug verifier should be enabled");
      assertTrue(config.isWasiEnabled(), "WASI should be enabled");
      assertTrue(config.isEpochInterruptionEnabled(), "Epoch interruption should be enabled");
      assertTrue(config.isMemoryLimitEnabled(), "Memory limits should be enabled");
      assertEquals(1024 * 1024L, config.getMemoryLimit(), "Memory limit should be 1MB");
    }

    @Test
    @DisplayName("WebAssembly feature configuration works correctly")
    void testWebAssemblyFeatures() {
      final EngineConfig config =
          new EngineConfig()
              .wasmBacktraceDetails(false)
              .wasmReferenceTypes(false)
              .wasmSimd(false)
              .wasmRelaxedSimd(true)
              .wasmMultiValue(false)
              .wasmBulkMemory(false)
              .wasmThreads(true)
              .wasmTailCall(true)
              .wasmMultiMemory(true)
              .wasmMemory64(true);

      assertFalse(config.isWasmBacktraceDetails(), "WASM backtrace details should be disabled");
      assertFalse(config.isWasmReferenceTypes(), "WASM reference types should be disabled");
      assertFalse(config.isWasmSimd(), "WASM SIMD should be disabled");
      assertTrue(config.isWasmRelaxedSimd(), "WASM relaxed SIMD should be enabled");
      assertFalse(config.isWasmMultiValue(), "WASM multi-value should be disabled");
      assertFalse(config.isWasmBulkMemory(), "WASM bulk memory should be disabled");
      assertTrue(config.isWasmThreads(), "WASM threads should be enabled");
      assertTrue(config.isWasmTailCall(), "WASM tail call should be enabled");
      assertTrue(config.isWasmMultiMemory(), "WASM multi-memory should be enabled");
      assertTrue(config.isWasmMemory64(), "WASM memory64 should be enabled");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Null optimization level throws IllegalArgumentException")
    void testNullOptimizationLevel() {
      final EngineConfig config = new EngineConfig();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> config.optimizationLevel(null),
              "Should throw IllegalArgumentException for null optimization level");

      assertEquals(
          "Optimization level cannot be null",
          exception.getMessage(),
          "Exception message should be descriptive");
    }

    @Test
    @DisplayName("Negative fuel amount throws IllegalArgumentException")
    void testNegativeFuelAmount() {
      final EngineConfig config = new EngineConfig();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> config.fuelAmount(-1L),
              "Should throw IllegalArgumentException for negative fuel amount");

      assertEquals(
          "Fuel amount cannot be negative",
          exception.getMessage(),
          "Exception message should be descriptive");
    }

    @Test
    @DisplayName("Negative memory limit throws IllegalArgumentException")
    void testNegativeMemoryLimit() {
      final EngineConfig config = new EngineConfig();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> config.memoryLimit(-1L),
              "Should throw IllegalArgumentException for negative memory limit");

      assertEquals(
          "Memory limit cannot be negative",
          exception.getMessage(),
          "Exception message should be descriptive");
    }

    @Test
    @DisplayName("Zero values are accepted for fuel amount and memory limit")
    void testZeroValues() {
      final EngineConfig config = new EngineConfig().fuelAmount(0L).memoryLimit(0L);

      assertEquals(0L, config.getFuelAmount(), "Zero fuel amount should be accepted");
      assertEquals(0L, config.getMemoryLimit(), "Zero memory limit should be accepted");
    }

    @Test
    @DisplayName("Large values are accepted for fuel amount and memory limit")
    void testLargeValues() {
      final long largeFuel = Long.MAX_VALUE;
      final long largeMemory = Long.MAX_VALUE;

      final EngineConfig config = new EngineConfig().fuelAmount(largeFuel).memoryLimit(largeMemory);

      assertEquals(largeFuel, config.getFuelAmount(), "Large fuel amount should be accepted");
      assertEquals(largeMemory, config.getMemoryLimit(), "Large memory limit should be accepted");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("forSpeed() creates speed-optimized configuration")
    void testForSpeed() {
      final EngineConfig config = EngineConfig.forSpeed();

      assertNotNull(config, "forSpeed() should return non-null configuration");
      assertEquals(
          OptimizationLevel.SPEED,
          config.getOptimizationLevel(),
          "Speed config should use SPEED optimization");
      assertTrue(config.isParallelCompilation(), "Speed config should enable parallel compilation");
      assertFalse(config.isDebugInfo(), "Speed config should not enable debug info");
      assertFalse(
          config.isCraneliftDebugVerifier(), "Speed config should not enable debug verifier");
    }

    @Test
    @DisplayName("forSize() creates size-optimized configuration")
    void testForSize() {
      final EngineConfig config = EngineConfig.forSize();

      assertNotNull(config, "forSize() should return non-null configuration");
      assertEquals(
          OptimizationLevel.SIZE,
          config.getOptimizationLevel(),
          "Size config should use SIZE optimization");
      assertTrue(config.isParallelCompilation(), "Size config should enable parallel compilation");
      assertFalse(config.isDebugInfo(), "Size config should not enable debug info");
      assertFalse(
          config.isCraneliftDebugVerifier(), "Size config should not enable debug verifier");
    }

    @Test
    @DisplayName("forDebug() creates debug-optimized configuration")
    void testForDebug() {
      final EngineConfig config = EngineConfig.forDebug();

      assertNotNull(config, "forDebug() should return non-null configuration");
      assertEquals(
          OptimizationLevel.NONE,
          config.getOptimizationLevel(),
          "Debug config should use NONE optimization");
      assertTrue(config.isDebugInfo(), "Debug config should enable debug info");
      assertTrue(config.isCraneliftDebugVerifier(), "Debug config should enable debug verifier");
    }

    @Test
    @DisplayName("forProduction() creates production-optimized configuration")
    void testForProduction() {
      final EngineConfig config = EngineConfig.forProduction();

      assertNotNull(config, "forProduction() should return non-null configuration");
      assertEquals(
          OptimizationLevel.SPEED_AND_SIZE,
          config.getOptimizationLevel(),
          "Production config should use SPEED_AND_SIZE optimization");
      assertTrue(
          config.isParallelCompilation(), "Production config should enable parallel compilation");
      assertFalse(config.isDebugInfo(), "Production config should not enable debug info");
      assertFalse(
          config.isWasmBacktraceDetails(), "Production config should disable backtrace details");
      assertFalse(
          config.isCraneliftDebugVerifier(), "Production config should not enable debug verifier");
    }

    @Test
    @DisplayName("forSecurity() creates security-hardened configuration")
    void testForSecurity() {
      final EngineConfig config = EngineConfig.forSecurity();

      assertNotNull(config, "forSecurity() should return non-null configuration");
      assertEquals(
          OptimizationLevel.SPEED,
          config.getOptimizationLevel(),
          "Security config should use SPEED optimization");
      assertTrue(config.isMemoryLimitEnabled(), "Security config should enable memory limits");
      assertEquals(
          256 * 1024 * 1024L,
          config.getMemoryLimit(),
          "Security config should set 256MB memory limit");
      assertTrue(config.isConsumeFuel(), "Security config should enable fuel consumption");
      assertEquals(1000000L, config.getFuelAmount(), "Security config should set fuel amount");
      assertTrue(
          config.isEpochInterruptionEnabled(), "Security config should enable epoch interruption");
      assertFalse(config.isWasmThreads(), "Security config should disable threads");
      assertFalse(config.isWasmTailCall(), "Security config should disable tail call");
      assertFalse(config.isWasmMultiMemory(), "Security config should disable multi-memory");
      assertFalse(config.isWasmMemory64(), "Security config should disable memory64");
    }

    @Test
    @DisplayName("forCompatibility() creates maximum compatibility configuration")
    void testForCompatibility() {
      final EngineConfig config = EngineConfig.forCompatibility();

      assertNotNull(config, "forCompatibility() should return non-null configuration");
      assertEquals(
          OptimizationLevel.SPEED,
          config.getOptimizationLevel(),
          "Compatibility config should use SPEED optimization");
      assertTrue(
          config.isWasmReferenceTypes(), "Compatibility config should enable reference types");
      assertTrue(config.isWasmSimd(), "Compatibility config should enable SIMD");
      assertTrue(config.isWasmRelaxedSimd(), "Compatibility config should enable relaxed SIMD");
      assertTrue(config.isWasmMultiValue(), "Compatibility config should enable multi-value");
      assertTrue(config.isWasmBulkMemory(), "Compatibility config should enable bulk memory");
      assertTrue(config.isWasmThreads(), "Compatibility config should enable threads");
      assertTrue(config.isWasmTailCall(), "Compatibility config should enable tail call");
      assertTrue(config.isWasmMultiMemory(), "Compatibility config should enable multi-memory");
      assertTrue(config.isWasmMemory64(), "Compatibility config should enable memory64");
    }

    @Test
    @DisplayName("forEmbedded() creates low-resource configuration")
    void testForEmbedded() {
      final EngineConfig config = EngineConfig.forEmbedded();

      assertNotNull(config, "forEmbedded() should return non-null configuration");
      assertEquals(
          OptimizationLevel.SIZE,
          config.getOptimizationLevel(),
          "Embedded config should use SIZE optimization");
      assertFalse(
          config.isParallelCompilation(), "Embedded config should disable parallel compilation");
      assertFalse(config.isDebugInfo(), "Embedded config should disable debug info");
      assertFalse(
          config.isWasmBacktraceDetails(), "Embedded config should disable backtrace details");
      assertFalse(config.isWasmThreads(), "Embedded config should disable threads");
      assertFalse(config.isWasmSimd(), "Embedded config should disable SIMD");
      assertFalse(config.isWasmRelaxedSimd(), "Embedded config should disable relaxed SIMD");
      assertTrue(config.isMemoryLimitEnabled(), "Embedded config should enable memory limits");
      assertEquals(
          32 * 1024 * 1024L,
          config.getMemoryLimit(),
          "Embedded config should set 32MB memory limit");
    }

    @Test
    @DisplayName("Factory methods return independent instances")
    void testFactoryMethodIndependence() {
      final EngineConfig speedConfig = EngineConfig.forSpeed();
      final EngineConfig sizeConfig = EngineConfig.forSize();
      final EngineConfig debugConfig = EngineConfig.forDebug();

      // Modify one configuration
      speedConfig.debugInfo(true);

      // Verify others are not affected
      assertFalse(sizeConfig.isDebugInfo(), "Size config should remain unchanged");
      assertTrue(debugConfig.isDebugInfo(), "Debug config should retain its settings");
    }
  }

  @Nested
  @DisplayName("Configuration Combinations Tests")
  class ConfigurationCombinationTests {

    @Test
    @DisplayName("Can combine factory method with additional settings")
    void testFactoryMethodCombination() {
      final EngineConfig config =
          EngineConfig.forSpeed()
              .wasiEnabled(true)
              .consumeFuel(true)
              .fuelAmount(5000L)
              .memoryLimitEnabled(true)
              .memoryLimit(2 * 1024 * 1024L); // 2MB

      // Should retain factory settings
      assertEquals(
          OptimizationLevel.SPEED,
          config.getOptimizationLevel(),
          "Should retain SPEED optimization");
      assertTrue(config.isParallelCompilation(), "Should retain parallel compilation");

      // Should have additional settings
      assertTrue(config.isWasiEnabled(), "Should enable WASI");
      assertTrue(config.isConsumeFuel(), "Should enable fuel consumption");
      assertEquals(5000L, config.getFuelAmount(), "Should set fuel amount");
      assertTrue(config.isMemoryLimitEnabled(), "Should enable memory limits");
      assertEquals(2 * 1024 * 1024L, config.getMemoryLimit(), "Should set memory limit");
    }

    @Test
    @DisplayName("Can override factory method settings")
    void testFactoryMethodOverride() {
      final EngineConfig config =
          EngineConfig.forDebug()
              .optimizationLevel(OptimizationLevel.SPEED)
              .debugInfo(false)
              .craneliftDebugVerifier(false);

      // Should have overridden values
      assertEquals(
          OptimizationLevel.SPEED, config.getOptimizationLevel(), "Should override optimization");
      assertFalse(config.isDebugInfo(), "Should override debug info");
      assertFalse(config.isCraneliftDebugVerifier(), "Should override debug verifier");
    }

    @Test
    @DisplayName("Comprehensive configuration with all features enabled")
    void testComprehensiveConfiguration() {
      final EngineConfig config =
          new EngineConfig()
              // Core settings
              .debugInfo(true)
              .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
              .parallelCompilation(true)
              .craneliftDebugVerifier(true)
              // Runtime features
              .consumeFuel(true)
              .fuelAmount(10000L)
              .wasiEnabled(true)
              .epochInterruption(true)
              .memoryLimitEnabled(true)
              .memoryLimit(4 * 1024 * 1024L) // 4MB
              // WebAssembly features
              .wasmBacktraceDetails(true)
              .wasmReferenceTypes(true)
              .wasmSimd(true)
              .wasmRelaxedSimd(true)
              .wasmMultiValue(true)
              .wasmBulkMemory(true)
              .wasmThreads(true)
              .wasmTailCall(true)
              .wasmMultiMemory(true)
              .wasmMemory64(true);

      // Verify all settings are correctly applied
      assertTrue(config.isDebugInfo());
      assertEquals(OptimizationLevel.SPEED_AND_SIZE, config.getOptimizationLevel());
      assertTrue(config.isParallelCompilation());
      assertTrue(config.isCraneliftDebugVerifier());
      assertTrue(config.isConsumeFuel());
      assertEquals(10000L, config.getFuelAmount());
      assertTrue(config.isWasiEnabled());
      assertTrue(config.isEpochInterruptionEnabled());
      assertTrue(config.isMemoryLimitEnabled());
      assertEquals(4 * 1024 * 1024L, config.getMemoryLimit());
      assertTrue(config.isWasmBacktraceDetails());
      assertTrue(config.isWasmReferenceTypes());
      assertTrue(config.isWasmSimd());
      assertTrue(config.isWasmRelaxedSimd());
      assertTrue(config.isWasmMultiValue());
      assertTrue(config.isWasmBulkMemory());
      assertTrue(config.isWasmThreads());
      assertTrue(config.isWasmTailCall());
      assertTrue(config.isWasmMultiMemory());
      assertTrue(config.isWasmMemory64());
    }
  }

  @Nested
  @DisplayName("Advanced Validation Tests")
  class AdvancedValidationTests {

    @Test
    @DisplayName("validate() passes for valid configuration")
    void testValidConfiguration() {
      final EngineConfig config =
          new EngineConfig()
              .debugInfo(true)
              .craneliftDebugVerifier(true)
              .parallelCompilation(false) // Disabled when debug verifier is enabled
              .consumeFuel(true)
              .fuelAmount(1000L)
              .memoryLimitEnabled(true)
              .memoryLimit(64 * 1024 * 1024L) // 64MB
              .wasmSimd(true)
              .wasmRelaxedSimd(true) // Requires SIMD to be enabled
              .wasmThreads(true)
              .wasmReferenceTypes(true); // Required for threads

      // Should not throw any exception
      config.validate();
    }

    @Test
    @DisplayName("validate() throws for cranelift debug verifier without debug info")
    void testCraneliftDebugVerifierWithoutDebugInfo() {
      final EngineConfig config = new EngineConfig().debugInfo(false).craneliftDebugVerifier(true);

      final IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              config::validate,
              "Should throw IllegalStateException when cranelift debug verifier is enabled without"
                  + " debug info");

      assertEquals(
          "Cranelift debug verifier requires debug info to be enabled", exception.getMessage());
    }

    @Test
    @DisplayName("validate() throws for parallel compilation with debug verifier")
    void testParallelCompilationWithDebugVerifier() {
      final EngineConfig config =
          new EngineConfig().debugInfo(true).craneliftDebugVerifier(true).parallelCompilation(true);

      final IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              config::validate,
              "Should throw IllegalStateException when parallel compilation is enabled with debug"
                  + " verifier");

      assertEquals(
          "Parallel compilation should be disabled when using Cranelift debug verifier",
          exception.getMessage());
    }

    @Test
    @DisplayName("validate() throws for memory limit enabled without positive limit")
    void testMemoryLimitEnabledWithoutPositiveLimit() {
      final EngineConfig config = new EngineConfig().memoryLimitEnabled(true).memoryLimit(0L);

      final IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              config::validate,
              "Should throw IllegalStateException when memory limit is enabled but limit is not"
                  + " positive");

      assertEquals(
          "Memory limit must be positive when memory limiting is enabled", exception.getMessage());
    }

    @Test
    @DisplayName("validate() throws for fuel consumption enabled without positive amount")
    void testFuelConsumptionEnabledWithoutPositiveAmount() {
      final EngineConfig config = new EngineConfig().consumeFuel(true).fuelAmount(0L);

      final IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              config::validate,
              "Should throw IllegalStateException when fuel consumption is enabled but amount is"
                  + " not positive");

      assertEquals(
          "Fuel amount must be positive when fuel consumption is enabled", exception.getMessage());
    }

    @Test
    @DisplayName("validate() throws for relaxed SIMD without regular SIMD")
    void testRelaxedSimdWithoutSimd() {
      final EngineConfig config = new EngineConfig().wasmSimd(false).wasmRelaxedSimd(true);

      final IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              config::validate,
              "Should throw IllegalStateException when relaxed SIMD is enabled without regular"
                  + " SIMD");

      assertEquals("Relaxed SIMD requires SIMD to be enabled", exception.getMessage());
    }

    @Test
    @DisplayName("validate() throws for threads without reference types")
    void testThreadsWithoutReferenceTypes() {
      final EngineConfig config = new EngineConfig().wasmThreads(true).wasmReferenceTypes(false);

      final IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              config::validate,
              "Should throw IllegalStateException when threads are enabled without reference"
                  + " types");

      assertEquals(
          "WebAssembly threads require reference types to be enabled", exception.getMessage());
    }

    @Test
    @DisplayName("validate() throws for memory64 and multi-memory together")
    void testMemory64WithMultiMemory() {
      final EngineConfig config = new EngineConfig().wasmMemory64(true).wasmMultiMemory(true);

      final IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              config::validate,
              "Should throw IllegalStateException when both memory64 and multi-memory are enabled");

      assertEquals(
          "Memory64 and multi-memory features may conflict and should not be enabled together",
          exception.getMessage());
    }

    @Test
    @DisplayName("validate() throws for unreasonable memory limits")
    void testUnreasonableMemoryLimits() {
      // Test too small memory limit
      final EngineConfig configTooSmall =
          new EngineConfig().memoryLimitEnabled(true).memoryLimit(512L); // Less than 1MB

      final IllegalStateException exceptionTooSmall =
          assertThrows(
              IllegalStateException.class,
              configTooSmall::validate,
              "Should throw IllegalStateException for memory limit too small");

      assertTrue(exceptionTooSmall.getMessage().contains("Memory limit must be between"));

      // Test too large memory limit
      final EngineConfig configTooLarge =
          new EngineConfig().memoryLimitEnabled(true).memoryLimit(32L * 1024 * 1024 * 1024); // 32GB

      final IllegalStateException exceptionTooLarge =
          assertThrows(
              IllegalStateException.class,
              configTooLarge::validate,
              "Should throw IllegalStateException for memory limit too large");

      assertTrue(exceptionTooLarge.getMessage().contains("Memory limit must be between"));
    }
  }

  @Nested
  @DisplayName("Configuration Compatibility Tests")
  class ConfigurationCompatibilityTests {

    @Test
    @DisplayName("isCompatibleWith() returns true for identical configurations")
    void testIdenticalConfigurations() {
      final EngineConfig config1 = EngineConfig.forProduction();
      final EngineConfig config2 = EngineConfig.forProduction();

      assertTrue(
          config1.isCompatibleWith(config2), "Identical configurations should be compatible");
      assertTrue(config2.isCompatibleWith(config1), "Compatibility should be symmetric");
    }

    @Test
    @DisplayName("isCompatibleWith() returns false for null configuration")
    void testNullConfiguration() {
      final EngineConfig config = new EngineConfig();

      assertFalse(
          config.isCompatibleWith(null), "Configuration should not be compatible with null");
    }

    @Test
    @DisplayName("isCompatibleWith() checks WebAssembly feature compatibility")
    void testWebAssemblyFeatureCompatibility() {
      final EngineConfig configBasic =
          new EngineConfig().wasmReferenceTypes(true).wasmSimd(false).wasmThreads(false);

      final EngineConfig configAdvanced =
          new EngineConfig().wasmReferenceTypes(true).wasmSimd(true).wasmThreads(true);

      // Basic should be compatible with advanced (advanced supports all basic features)
      assertTrue(
          configBasic.isCompatibleWith(configAdvanced),
          "Basic configuration should be compatible with more advanced configuration");

      // Advanced should not be compatible with basic (basic doesn't support all advanced features)
      assertFalse(
          configAdvanced.isCompatibleWith(configBasic),
          "Advanced configuration should not be compatible with more basic configuration");
    }

    @Test
    @DisplayName("isCompatibleWith() checks fuel consumption compatibility")
    void testFuelConsumptionCompatibility() {
      final EngineConfig configWithFuel = new EngineConfig().consumeFuel(true).fuelAmount(1000L);

      final EngineConfig configWithoutFuel = new EngineConfig().consumeFuel(false);

      assertFalse(
          configWithFuel.isCompatibleWith(configWithoutFuel),
          "Fuel-enabled configuration should not be compatible with fuel-disabled configuration");
      assertFalse(
          configWithoutFuel.isCompatibleWith(configWithFuel),
          "Fuel-disabled configuration should not be compatible with fuel-enabled configuration");
    }

    @Test
    @DisplayName("isCompatibleWith() checks memory limit compatibility")
    void testMemoryLimitCompatibility() {
      final EngineConfig configSmallMemory =
          new EngineConfig().memoryLimitEnabled(true).memoryLimit(64 * 1024 * 1024L); // 64MB

      final EngineConfig configLargeMemory =
          new EngineConfig().memoryLimitEnabled(true).memoryLimit(256 * 1024 * 1024L); // 256MB

      final EngineConfig configNoMemoryLimit = new EngineConfig().memoryLimitEnabled(false);

      // Small memory should be compatible with large memory
      assertTrue(
          configSmallMemory.isCompatibleWith(configLargeMemory),
          "Configuration with smaller memory limit should be compatible with larger memory limit");

      // Large memory should not be compatible with small memory
      assertFalse(
          configLargeMemory.isCompatibleWith(configSmallMemory),
          "Configuration with larger memory limit should not be compatible with smaller memory"
              + " limit");

      // No memory limit should be compatible with any memory limit
      assertTrue(
          configNoMemoryLimit.isCompatibleWith(configSmallMemory),
          "Configuration without memory limit should be compatible with limited memory");
      assertTrue(
          configNoMemoryLimit.isCompatibleWith(configLargeMemory),
          "Configuration without memory limit should be compatible with limited memory");
    }
  }

  @Nested
  @DisplayName("Configuration Utility Tests")
  class ConfigurationUtilityTests {

    @Test
    @DisplayName("copy() creates independent copy")
    void testCopy() {
      final EngineConfig original =
          new EngineConfig()
              .debugInfo(true)
              .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
              .consumeFuel(true)
              .fuelAmount(5000L)
              .wasmThreads(true);

      final EngineConfig copy = original.copy();

      // Verify copy has same values
      assertEquals(original.isDebugInfo(), copy.isDebugInfo());
      assertEquals(original.getOptimizationLevel(), copy.getOptimizationLevel());
      assertEquals(original.isConsumeFuel(), copy.isConsumeFuel());
      assertEquals(original.getFuelAmount(), copy.getFuelAmount());
      assertEquals(original.isWasmThreads(), copy.isWasmThreads());

      // Verify copy is independent
      copy.debugInfo(false);
      assertTrue(original.isDebugInfo(), "Original should remain unchanged when copy is modified");
      assertFalse(copy.isDebugInfo(), "Copy should be modified");
    }

    @Test
    @DisplayName("getSummary() returns informative summary")
    void testGetSummary() {
      final EngineConfig config =
          new EngineConfig()
              .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
              .debugInfo(true)
              .parallelCompilation(false)
              .consumeFuel(true)
              .fuelAmount(2000L)
              .memoryLimitEnabled(true)
              .memoryLimit(128 * 1024 * 1024L) // 128MB
              .epochInterruption(true)
              .wasmReferenceTypes(true)
              .wasmSimd(true)
              .wasmThreads(true);

      final String summary = config.getSummary();

      assertNotNull(summary, "Summary should not be null");
      assertTrue(
          summary.contains("optimization=SPEED_AND_SIZE"),
          "Summary should contain optimization level");
      assertTrue(summary.contains("debug=true"), "Summary should contain debug info");
      assertTrue(summary.contains("parallel=false"), "Summary should contain parallel compilation");
      assertTrue(summary.contains("fuel=2000"), "Summary should contain fuel amount");
      assertTrue(summary.contains("memoryLimit=128MB"), "Summary should contain memory limit");
      assertTrue(
          summary.contains("epochInterruption=true"), "Summary should contain epoch interruption");
      assertTrue(summary.contains("ref-types"), "Summary should contain enabled WASM features");
      assertTrue(summary.contains("simd"), "Summary should contain enabled WASM features");
      assertTrue(summary.contains("threads"), "Summary should contain enabled WASM features");
    }

    @Test
    @DisplayName("equals() and hashCode() work correctly")
    void testEqualsAndHashCode() {
      final EngineConfig config1 =
          new EngineConfig()
              .debugInfo(true)
              .optimizationLevel(OptimizationLevel.SPEED)
              .consumeFuel(true)
              .fuelAmount(1000L);

      final EngineConfig config2 =
          new EngineConfig()
              .debugInfo(true)
              .optimizationLevel(OptimizationLevel.SPEED)
              .consumeFuel(true)
              .fuelAmount(1000L);

      final EngineConfig config3 =
          new EngineConfig()
              .debugInfo(false)
              .optimizationLevel(OptimizationLevel.SPEED)
              .consumeFuel(true)
              .fuelAmount(1000L);

      // Test equals
      assertEquals(config1, config2, "Configurations with same values should be equal");
      assertTrue(config1.equals(config1), "Configuration should equal itself");
      assertFalse(config1.equals(null), "Configuration should not equal null");
      assertFalse(config1.equals("not a config"), "Configuration should not equal different type");
      assertFalse(
          config1.equals(config3), "Configurations with different values should not be equal");

      // Test hashCode consistency
      assertEquals(
          config1.hashCode(),
          config2.hashCode(),
          "Equal configurations should have same hash code");
      assertTrue(
          config1.hashCode() != config3.hashCode(),
          "Different configurations should likely have different hash codes");
    }

    @Test
    @DisplayName("toString() returns summary")
    void testToString() {
      final EngineConfig config = EngineConfig.forDebug();
      final String toStringResult = config.toString();
      final String summaryResult = config.getSummary();

      assertEquals(
          summaryResult, toStringResult, "toString() should return same result as getSummary()");
    }
  }
}
