package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Engine interface.
 *
 * <p>Tests verify engine creation, configuration, module compilation, store creation, and resource
 * management.
 */
@DisplayName("Engine Interface Tests")
class EngineTest {

  /** Simple WebAssembly module that exports an add function. */
  private static final String SIMPLE_ADD_WAT =
      "(module\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add))\n";

  /** Minimal valid WebAssembly module (empty module). */
  private static final byte[] MINIMAL_WASM = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

  private Engine engine;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
  }

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  @Nested
  @DisplayName("Engine Creation Tests")
  class EngineCreationTests {

    @Test
    @DisplayName("should create engine with default configuration")
    void shouldCreateEngineWithDefaultConfiguration() throws WasmException {
      try (Engine defaultEngine = Engine.create()) {
        assertNotNull(defaultEngine, "Engine should not be null");
        assertTrue(defaultEngine.isValid(), "Engine should be valid after creation");
      }
    }

    @Test
    @DisplayName("should create engine with custom configuration")
    void shouldCreateEngineWithCustomConfiguration() throws WasmException {
      final EngineConfig config = new EngineConfig().consumeFuel(true).setEpochInterruption(true);

      try (Engine customEngine = Engine.create(config)) {
        assertNotNull(customEngine, "Engine should not be null");
        assertTrue(customEngine.isValid(), "Engine should be valid after creation");
        assertTrue(customEngine.isFuelEnabled(), "Fuel should be enabled per configuration");
        assertTrue(
            customEngine.isEpochInterruptionEnabled(),
            "Epoch interruption should be enabled per configuration");
      }
    }

    @Test
    @DisplayName("should create engine using builder pattern")
    void shouldCreateEngineUsingBuilderPattern() throws WasmException {
      final EngineConfig config = Engine.builder().debugInfo(true).consumeFuel(false);

      try (Engine builtEngine = Engine.create(config)) {
        assertNotNull(builtEngine, "Engine should not be null");
        assertTrue(builtEngine.isValid(), "Engine should be valid after creation");
      }
    }

    @Test
    @DisplayName("should throw exception when creating engine with null config")
    void shouldThrowExceptionWhenCreatingEngineWithNullConfig() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Engine.create(null),
          "Should throw IllegalArgumentException for null config");
    }
  }

  @Nested
  @DisplayName("Engine Configuration Tests")
  class EngineConfigurationTests {

    @Test
    @DisplayName("should return engine configuration")
    void shouldReturnEngineConfiguration() {
      final EngineConfig config = engine.getConfig();
      assertNotNull(config, "Engine config should not be null");
    }

    @Test
    @DisplayName("should report fuel disabled by default")
    void shouldReportFuelDisabledByDefault() {
      assertFalse(engine.isFuelEnabled(), "Fuel should be disabled by default");
    }

    @Test
    @DisplayName("should report epoch interruption disabled by default")
    void shouldReportEpochInterruptionDisabledByDefault() {
      assertFalse(
          engine.isEpochInterruptionEnabled(), "Epoch interruption should be disabled by default");
    }

    @Test
    @DisplayName("should report coredump on trap status")
    void shouldReportCoredumpOnTrapStatus() {
      // Just verify the method exists and returns a boolean
      assertNotNull(
          Boolean.valueOf(engine.isCoredumpOnTrapEnabled()),
          "isCoredumpOnTrapEnabled should return a boolean");
    }

    @Test
    @DisplayName("should report async status")
    void shouldReportAsyncStatus() {
      // Just verify the method exists and returns a boolean
      assertNotNull(Boolean.valueOf(engine.isAsync()), "isAsync should return a boolean");
    }

    @Test
    @DisplayName("should return memory limit in pages")
    void shouldReturnMemoryLimitInPages() {
      final int memoryLimit = engine.getMemoryLimitPages();
      assertTrue(memoryLimit >= 0, "Memory limit should be non-negative");
    }

    @Test
    @DisplayName("should return stack size limit")
    void shouldReturnStackSizeLimit() {
      final long stackSize = engine.getStackSizeLimit();
      assertTrue(stackSize >= 0, "Stack size should be non-negative");
    }

    @Test
    @DisplayName("should return max instances")
    void shouldReturnMaxInstances() {
      final int maxInstances = engine.getMaxInstances();
      assertTrue(maxInstances >= 0, "Max instances should be non-negative");
    }

    @Test
    @DisplayName("should return reference count")
    void shouldReturnReferenceCount() {
      final long refCount = engine.getReferenceCount();
      assertTrue(refCount >= 0, "Reference count should be non-negative");
    }
  }

  @Nested
  @DisplayName("Store Creation Tests")
  class StoreCreationTests {

    @Test
    @DisplayName("should create store from engine")
    void shouldCreateStoreFromEngine() throws WasmException {
      try (Store store = engine.createStore()) {
        assertNotNull(store, "Store should not be null");
      }
    }

    @Test
    @DisplayName("should create store with custom data")
    void shouldCreateStoreWithCustomData() throws WasmException {
      final String customData = "test-data";
      try (Store store = engine.createStore(customData)) {
        assertNotNull(store, "Store should not be null");
      }
    }

    @Test
    @DisplayName("should create store with null data")
    void shouldCreateStoreWithNullData() throws WasmException {
      try (Store store = engine.createStore(null)) {
        assertNotNull(store, "Store should not be null even with null data");
      }
    }

    @Test
    @DisplayName("should create multiple stores from same engine")
    void shouldCreateMultipleStoresFromSameEngine() throws WasmException {
      try (Store store1 = engine.createStore();
          Store store2 = engine.createStore();
          Store store3 = engine.createStore()) {
        assertNotNull(store1, "First store should not be null");
        assertNotNull(store2, "Second store should not be null");
        assertNotNull(store3, "Third store should not be null");
        assertNotSame(store1, store2, "Stores should be different instances");
        assertNotSame(store2, store3, "Stores should be different instances");
      }
    }
  }

  @Nested
  @DisplayName("Module Compilation Tests")
  class ModuleCompilationTests {

    @Test
    @DisplayName("should compile module from valid wasm bytes")
    void shouldCompileModuleFromValidWasmBytes() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      assertNotNull(module, "Module should not be null");
      module.close();
    }

    @Test
    @DisplayName("should compile module from WAT string")
    void shouldCompileModuleFromWatString() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      assertNotNull(module, "Module should not be null");
      module.close();
    }

    @Test
    @DisplayName("should compile module from input stream")
    void shouldCompileModuleFromInputStream() throws WasmException, IOException {
      try (ByteArrayInputStream stream = new ByteArrayInputStream(MINIMAL_WASM)) {
        final Module module = engine.compileFromStream(stream);
        assertNotNull(module, "Module should not be null");
        module.close();
      }
    }

    @Test
    @DisplayName("should throw exception for null wasm bytes")
    void shouldThrowExceptionForNullWasmBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileModule(null),
          "Should throw IllegalArgumentException for null bytes");
    }

    @Test
    @DisplayName("should throw exception for null WAT string")
    void shouldThrowExceptionForNullWatString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileWat(null),
          "Should throw IllegalArgumentException for null WAT");
    }

    @Test
    @DisplayName("should throw exception for empty WAT string")
    void shouldThrowExceptionForEmptyWatString() {
      assertThrows(
          Exception.class,
          () -> engine.compileWat(""),
          "Should throw exception for empty WAT string");
    }

    @Test
    @DisplayName("should throw exception for null input stream")
    void shouldThrowExceptionForNullInputStream() {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileFromStream(null),
          "Should throw IllegalArgumentException for null stream");
    }

    @Test
    @DisplayName("should throw exception for invalid wasm bytes")
    void shouldThrowExceptionForInvalidWasmBytes() {
      final byte[] invalidWasm = {0x00, 0x00, 0x00, 0x00};
      assertThrows(
          WasmException.class,
          () -> engine.compileModule(invalidWasm),
          "Should throw WasmException for invalid WASM bytes");
    }

    @Test
    @DisplayName("should throw exception for invalid WAT syntax")
    void shouldThrowExceptionForInvalidWatSyntax() {
      final String invalidWat = "(module (invalid syntax here))";
      assertThrows(
          WasmException.class,
          () -> engine.compileWat(invalidWat),
          "Should throw WasmException for invalid WAT syntax");
    }
  }

  @Nested
  @DisplayName("Precompilation Tests")
  class PrecompilationTests {

    @Test
    @DisplayName("should precompile module from wasm bytes")
    void shouldPrecompileModuleFromWasmBytes() throws WasmException {
      final byte[] precompiled = engine.precompileModule(MINIMAL_WASM);
      assertNotNull(precompiled, "Precompiled bytes should not be null");
      assertTrue(precompiled.length > 0, "Precompiled bytes should not be empty");
    }

    @Test
    @DisplayName("should throw exception for null bytes in precompile")
    void shouldThrowExceptionForNullBytesInPrecompile() {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.precompileModule(null),
          "Should throw IllegalArgumentException for null bytes");
    }

    @Test
    @DisplayName("should detect precompiled module")
    void shouldDetectPrecompiledModule() throws WasmException {
      final byte[] precompiled = engine.precompileModule(MINIMAL_WASM);
      final Precompiled detected = engine.detectPrecompiled(precompiled);
      assertEquals(Precompiled.MODULE, detected, "Should detect as precompiled module");
    }

    @Test
    @DisplayName("should return null for non-precompiled bytes")
    void shouldReturnNullForNonPrecompiledBytes() {
      final Precompiled detected = engine.detectPrecompiled(MINIMAL_WASM);
      assertNull(detected, "Should return null for non-precompiled bytes");
    }

    @Test
    @DisplayName("should throw exception for null bytes in detect")
    void shouldThrowExceptionForNullBytesInDetect() {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.detectPrecompiled(null),
          "Should throw IllegalArgumentException for null bytes");
    }

    @Test
    @DisplayName("should return precompile compatibility hash")
    void shouldReturnPrecompileCompatibilityHash() {
      final byte[] hash = engine.precompileCompatibilityHash();
      assertNotNull(hash, "Compatibility hash should not be null");
    }
  }

  @Nested
  @DisplayName("Feature Support Tests")
  class FeatureSupportTests {

    @Test
    @DisplayName("should check feature support for reference types")
    void shouldCheckFeatureSupportForReferenceTypes() {
      // Just verify the method works without throwing
      final boolean supported = engine.supportsFeature(WasmFeature.REFERENCE_TYPES);
      assertNotNull(Boolean.valueOf(supported), "Should return a boolean");
    }

    @Test
    @DisplayName("should check feature support for SIMD")
    void shouldCheckFeatureSupportForSimd() {
      final boolean supported = engine.supportsFeature(WasmFeature.SIMD);
      assertNotNull(Boolean.valueOf(supported), "Should return a boolean");
    }

    @Test
    @DisplayName("should check feature support for bulk memory")
    void shouldCheckFeatureSupportForBulkMemory() {
      final boolean supported = engine.supportsFeature(WasmFeature.BULK_MEMORY);
      assertNotNull(Boolean.valueOf(supported), "Should return a boolean");
    }

    @Test
    @DisplayName("should check feature support for multi-value")
    void shouldCheckFeatureSupportForMultiValue() {
      final boolean supported = engine.supportsFeature(WasmFeature.MULTI_VALUE);
      assertNotNull(Boolean.valueOf(supported), "Should return a boolean");
    }

    @Test
    @DisplayName("should throw exception for null feature")
    void shouldThrowExceptionForNullFeature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.supportsFeature(null),
          "Should throw IllegalArgumentException for null feature");
    }
  }

  @Nested
  @DisplayName("Epoch Interruption Tests")
  class EpochInterruptionTests {

    @Test
    @DisplayName("should increment epoch without error")
    void shouldIncrementEpochWithoutError() throws WasmException {
      try (Engine epochEngine = Engine.create(new EngineConfig().setEpochInterruption(true))) {
        // Should not throw
        epochEngine.incrementEpoch();
        epochEngine.incrementEpoch();
        epochEngine.incrementEpoch();
      }
    }

    @Test
    @DisplayName("should increment epoch on non-epoch engine without error")
    void shouldIncrementEpochOnNonEpochEngineWithoutError() {
      // Even without epoch interruption enabled, this should not throw
      engine.incrementEpoch();
    }
  }

  @Nested
  @DisplayName("Engine Comparison Tests")
  class EngineComparisonTests {

    @Test
    @DisplayName("should report same engine as same")
    void shouldReportSameEngineAsSame() {
      assertTrue(engine.same(engine), "Engine should be same as itself");
    }

    @Test
    @DisplayName("should throw exception for null engine comparison")
    void shouldThrowExceptionForNullEngineComparison() {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.same(null),
          "Should throw IllegalArgumentException for null engine");
    }

    @Test
    @DisplayName("should compare engines with same config")
    void shouldCompareEnginesWithSameConfig() throws WasmException {
      final EngineConfig config = new EngineConfig().consumeFuel(true);
      try (Engine engine1 = Engine.create(config);
          Engine engine2 = Engine.create(config)) {
        // Two separately created engines with same config may or may not be "same"
        // depending on implementation - just verify it doesn't throw
        assertNotNull(Boolean.valueOf(engine1.same(engine2)));
      }
    }
  }

  @Nested
  @DisplayName("Engine Lifecycle Tests")
  class EngineLifecycleTests {

    @Test
    @DisplayName("should be valid before close")
    void shouldBeValidBeforeClose() {
      assertTrue(engine.isValid(), "Engine should be valid before close");
    }

    @Test
    @DisplayName("should be invalid after close")
    void shouldBeInvalidAfterClose() throws WasmException {
      final Engine tempEngine = Engine.create();
      assertTrue(tempEngine.isValid(), "Engine should be valid before close");
      tempEngine.close();
      assertFalse(tempEngine.isValid(), "Engine should be invalid after close");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() throws WasmException {
      final Engine tempEngine = Engine.create();
      tempEngine.close();
      // Second close should not throw
      tempEngine.close();
      assertFalse(tempEngine.isValid(), "Engine should remain invalid");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() throws WasmException {
      Engine tempEngine;
      try (Engine autoClosedEngine = Engine.create()) {
        tempEngine = autoClosedEngine;
        assertTrue(autoClosedEngine.isValid(), "Engine should be valid inside try block");
      }
      assertFalse(tempEngine.isValid(), "Engine should be invalid after try-with-resources");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should capture engine statistics")
    void shouldCaptureEngineStatistics() {
      final ai.tegmentum.wasmtime4j.performance.EngineStatistics stats = engine.captureStatistics();
      assertNotNull(stats, "Statistics should not be null");
    }

    @Test
    @DisplayName("should return pooling allocator metrics or null")
    void shouldReturnPoolingAllocatorMetricsOrNull() {
      // May return null if pooling is not enabled
      final ai.tegmentum.wasmtime4j.pool.PoolStatistics metrics =
          engine.getPoolingAllocatorMetrics();
      // Just verify it doesn't throw - may be null
    }
  }

  @Nested
  @DisplayName("Pulley Interpreter Tests")
  class PulleyInterpreterTests {

    @Test
    @DisplayName("should report pulley status")
    void shouldReportPulleyStatus() {
      // Just verify the method exists and returns a boolean
      final boolean isPulley = engine.isPulley();
      assertNotNull(Boolean.valueOf(isPulley), "isPulley should return a boolean");
    }
  }
}
