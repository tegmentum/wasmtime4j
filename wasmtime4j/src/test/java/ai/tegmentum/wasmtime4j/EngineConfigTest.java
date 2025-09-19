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
}
