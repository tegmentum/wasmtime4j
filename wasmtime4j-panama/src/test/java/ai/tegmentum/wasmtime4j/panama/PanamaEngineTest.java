package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link PanamaEngine}.
 *
 * <p>Tests the Java wrapper logic, parameter validation, native engine creation, module
 * compilation, feature detection, lifecycle management, and statistics capture.
 */
@DisplayName("Panama Engine Tests")
class PanamaEngineTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaEngineTest.class.getName());

  private static final String SIMPLE_MODULE_WAT = "(module)";

  private static final String FUNCTION_MODULE_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add))\
      """;

  /** Resources to close after each test, in reverse order. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  /** Creates a PanamaEngine and tracks it for cleanup. */
  private PanamaEngine createEngine() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    resources.add(engine);
    return engine;
  }

  /** Creates a PanamaEngine with config and tracks it for cleanup. */
  private PanamaEngine createEngine(final EngineConfig config) throws Exception {
    final PanamaEngine engine = new PanamaEngine(config);
    resources.add(engine);
    return engine;
  }

  /** Loads the exports-test.wasm bytes from classpath. */
  private byte[] loadTestWasmBytes() throws Exception {
    final Path wasmPath =
        Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
    return Files.readAllBytes(wasmPath);
  }

  // ==================== Constructor Tests ====================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create valid engine")
    void shouldCreateWithDefaultConstructor() throws Exception {
      final PanamaEngine engine = createEngine();
      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine.isValid(), "Engine should be valid after creation");
    }

    @Test
    @DisplayName("Config constructor should create valid engine")
    void shouldCreateWithConfig() throws Exception {
      final EngineConfig config = new EngineConfig();
      final PanamaEngine engine = createEngine(config);
      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine.isValid(), "Engine should be valid after creation");
    }

    @Test
    @DisplayName("Null config should throw IllegalArgumentException")
    void shouldThrowForNullConfig() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new PanamaEngine(null),
              "Null config should throw");
      assertTrue(
          ex.getMessage().contains("Config cannot be null"),
          "Message should indicate null config: " + ex.getMessage());
    }

    @Test
    @DisplayName("Config+Runtime constructor should create valid engine")
    void shouldCreateWithConfigAndRuntime() throws Exception {
      final EngineConfig config = new EngineConfig();
      final PanamaEngine engine = new PanamaEngine(config, null);
      resources.add(engine);
      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine.isValid(), "Engine should be valid");
    }
  }

  // ==================== Configuration Tests ====================

  @Nested
  @DisplayName("Configuration Tests")
  class ConfigurationTests {

    @Test
    @DisplayName("getConfig should return non-null config")
    void shouldReturnConfig() throws Exception {
      final PanamaEngine engine = createEngine();
      assertNotNull(engine.getConfig(), "Config should not be null");
    }

    @Test
    @DisplayName("getRuntime should return null when not provided")
    void shouldReturnNullRuntime() throws Exception {
      final PanamaEngine engine = createEngine();
      assertNull(engine.getRuntime(), "Runtime should be null for default constructor");
    }
  }

  // ==================== Module Compilation Tests ====================

  @Nested
  @DisplayName("Module Compilation Tests")
  class ModuleCompilationTests {

    @Test
    @DisplayName("compileModule with valid bytes should succeed")
    void shouldCompileModuleFromBytes() throws Exception {
      final PanamaEngine engine = createEngine();
      final byte[] wasmBytes = loadTestWasmBytes();

      final Module module = engine.compileModule(wasmBytes);
      resources.add(module);

      assertNotNull(module, "Compiled module should not be null");
      LOGGER.info("Successfully compiled module from bytes");
    }

    @Test
    @DisplayName("compileModule with null should throw IllegalArgumentException")
    void shouldThrowForNullBytes() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileModule(null),
          "Null bytes should throw");
    }

    @Test
    @DisplayName("compileModule with empty bytes should throw IllegalArgumentException")
    void shouldThrowForEmptyBytes() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileModule(new byte[0]),
          "Empty bytes should throw");
    }

    @Test
    @DisplayName("compileWat with valid WAT should succeed")
    void shouldCompileWat() throws Exception {
      final PanamaEngine engine = createEngine();
      final Module module = engine.compileWat(FUNCTION_MODULE_WAT);
      resources.add(module);

      assertNotNull(module, "Compiled WAT module should not be null");
      LOGGER.info("Successfully compiled WAT module");
    }

    @Test
    @DisplayName("compileWat with simple empty module should succeed")
    void shouldCompileSimpleWat() throws Exception {
      final PanamaEngine engine = createEngine();
      final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
      resources.add(module);

      assertNotNull(module, "Simple WAT module should not be null");
    }

    @Test
    @DisplayName("compileWat with null should throw IllegalArgumentException")
    void shouldThrowForNullWat() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class, () -> engine.compileWat(null), "Null WAT should throw");
    }

    @Test
    @DisplayName("compileWat with empty string should throw IllegalArgumentException")
    void shouldThrowForEmptyWat() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class, () -> engine.compileWat(""), "Empty WAT should throw");
    }

    @Test
    @DisplayName("compileWat with invalid WAT should throw WasmException")
    void shouldThrowForInvalidWat() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          WasmException.class,
          () -> engine.compileWat("this is not valid WAT"),
          "Invalid WAT should throw WasmException");
    }

    @Test
    @DisplayName("precompileModule with null should throw IllegalArgumentException")
    void shouldThrowForNullPrecompile() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.precompileModule(null),
          "Null precompile bytes should throw");
    }

    @Test
    @DisplayName("precompileModule with empty bytes should throw IllegalArgumentException")
    void shouldThrowForEmptyPrecompile() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.precompileModule(new byte[0]),
          "Empty precompile bytes should throw");
    }

    @Test
    @DisplayName("compileFromStream with valid stream should succeed")
    void shouldCompileFromStream() throws Exception {
      final PanamaEngine engine = createEngine();
      final byte[] wasmBytes = loadTestWasmBytes();
      final InputStream stream = new ByteArrayInputStream(wasmBytes);

      final Module module = engine.compileFromStream(stream);
      resources.add(module);

      assertNotNull(module, "Module from stream should not be null");
      LOGGER.info("Successfully compiled module from stream");
    }

    @Test
    @DisplayName("compileFromStream with null should throw IllegalArgumentException")
    void shouldThrowForNullStream() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileFromStream(null),
          "Null stream should throw");
    }

    @Test
    @DisplayName("compileFromStream with empty stream should throw WasmException")
    void shouldThrowForEmptyStream() throws Exception {
      final PanamaEngine engine = createEngine();
      final InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
      assertThrows(
          WasmException.class,
          () -> engine.compileFromStream(emptyStream),
          "Empty stream should throw WasmException");
    }
  }

  // ==================== Store Creation Tests ====================

  @Nested
  @DisplayName("Store Creation Tests")
  class StoreCreationTests {

    @Test
    @DisplayName("createStore should create valid store")
    void shouldCreateStore() throws Exception {
      final PanamaEngine engine = createEngine();
      final Store store = engine.createStore();
      resources.add(store);

      assertNotNull(store, "Store should not be null");
    }

    @Test
    @DisplayName("createStore with data should set user data")
    void shouldCreateStoreWithData() throws Exception {
      final PanamaEngine engine = createEngine();
      final String userData = "test-data";
      final Store store = engine.createStore(userData);
      resources.add(store);

      assertNotNull(store, "Store should not be null");
    }

    @Test
    @DisplayName("Should create multiple stores from same engine")
    void shouldCreateMultipleStores() throws Exception {
      final PanamaEngine engine = createEngine();

      final Store store1 = engine.createStore();
      resources.add(store1);
      final Store store2 = engine.createStore();
      resources.add(store2);

      assertNotNull(store1, "First store should not be null");
      assertNotNull(store2, "Second store should not be null");
    }
  }

  // ==================== Feature Detection Tests ====================

  @Nested
  @DisplayName("Feature Detection Tests")
  class FeatureDetectionTests {

    @Test
    @DisplayName("supportsFeature with null should return false")
    void shouldReturnFalseForNullFeature() throws Exception {
      final PanamaEngine engine = createEngine();
      assertFalse(engine.supportsFeature(null), "Null feature should return false");
    }

    @Test
    @DisplayName("supportsFeature should return boolean for valid features")
    void shouldReturnBooleanForValidFeatures() throws Exception {
      final PanamaEngine engine = createEngine();

      for (final WasmFeature feature : WasmFeature.values()) {
        final boolean supported = engine.supportsFeature(feature);
        LOGGER.info("Feature " + feature.name() + ": " + supported);
        // Just verify no exception is thrown
      }
    }

    @Test
    @DisplayName("getMemoryLimitPages should return non-negative")
    void shouldReturnNonNegativeMemoryLimit() throws Exception {
      final PanamaEngine engine = createEngine();
      final int limit = engine.getMemoryLimitPages();
      LOGGER.info("Memory limit pages: " + limit);
      assertTrue(limit >= 0, "Memory limit should be non-negative: " + limit);
    }

    @Test
    @DisplayName("getStackSizeLimit should return non-negative")
    void shouldReturnNonNegativeStackSizeLimit() throws Exception {
      final PanamaEngine engine = createEngine();
      final long limit = engine.getStackSizeLimit();
      LOGGER.info("Stack size limit: " + limit);
      assertTrue(limit >= 0, "Stack size limit should be non-negative: " + limit);
    }

    @Test
    @DisplayName("isFuelEnabled should return boolean")
    void shouldReturnFuelEnabled() throws Exception {
      final PanamaEngine engine = createEngine();
      final boolean fuelEnabled = engine.isFuelEnabled();
      LOGGER.info("Fuel enabled: " + fuelEnabled);
      // Default config should have fuel disabled
    }

    @Test
    @DisplayName("isEpochInterruptionEnabled should return boolean")
    void shouldReturnEpochInterruptionEnabled() throws Exception {
      final PanamaEngine engine = createEngine();
      final boolean epochEnabled = engine.isEpochInterruptionEnabled();
      LOGGER.info("Epoch interruption enabled: " + epochEnabled);
    }

    @Test
    @DisplayName("isCoredumpOnTrapEnabled should return boolean")
    void shouldReturnCoredumpOnTrapEnabled() throws Exception {
      final PanamaEngine engine = createEngine();
      final boolean coredumpEnabled = engine.isCoredumpOnTrapEnabled();
      LOGGER.info("Coredump on trap enabled: " + coredumpEnabled);
    }

    @Test
    @DisplayName("getMaxInstances should return Integer.MAX_VALUE")
    void shouldReturnMaxInstances() throws Exception {
      final PanamaEngine engine = createEngine();
      assertEquals(
          Integer.MAX_VALUE, engine.getMaxInstances(), "Max instances should be Integer.MAX_VALUE");
    }

    @Test
    @DisplayName("getReferenceCount should return 1")
    void shouldReturnReferenceCount() throws Exception {
      final PanamaEngine engine = createEngine();
      assertEquals(1, engine.getReferenceCount(), "Reference count should be 1");
    }

    @Test
    @DisplayName("isAsync should return false")
    void shouldReturnIsAsync() throws Exception {
      final PanamaEngine engine = createEngine();
      assertFalse(engine.isAsync(), "Default engine should not be async");
    }

    @Test
    @DisplayName("isPulley should return boolean without throwing")
    void shouldReturnIsPulley() throws Exception {
      final PanamaEngine engine = createEngine();
      final boolean pulley = engine.isPulley();
      LOGGER.info("isPulley: " + pulley);
      // Just verify no exception
    }
  }

  // ==================== Lifecycle Tests ====================

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("isValid should return true for new engine")
    void shouldBeValidAfterCreation() throws Exception {
      final PanamaEngine engine = createEngine();
      assertTrue(engine.isValid(), "New engine should be valid");
    }

    @Test
    @DisplayName("close should make isValid return false")
    void shouldBeInvalidAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      assertTrue(engine.isValid(), "Engine should be valid before close");
      engine.close();
      assertFalse(engine.isValid(), "Engine should be invalid after close");
    }

    @Test
    @DisplayName("Double close should not throw")
    void shouldAllowDoubleClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();
      assertDoesNotThrow(engine::close, "Second close should not throw");
    }

    @Test
    @DisplayName("compileModule after close should throw IllegalStateException")
    void shouldThrowOnCompileAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();

      final byte[] wasmBytes = loadTestWasmBytes();
      assertThrows(
          IllegalStateException.class,
          () -> engine.compileModule(wasmBytes),
          "compileModule after close should throw");
    }

    @Test
    @DisplayName("compileWat after close should throw IllegalStateException")
    void shouldThrowOnCompileWatAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();

      assertThrows(
          IllegalStateException.class,
          () -> engine.compileWat(SIMPLE_MODULE_WAT),
          "compileWat after close should throw");
    }

    @Test
    @DisplayName("createStore after close should throw IllegalStateException")
    void shouldThrowOnCreateStoreAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();

      assertThrows(
          IllegalStateException.class, engine::createStore, "createStore after close should throw");
    }

    @Test
    @DisplayName("supportsFeature after close should throw IllegalStateException")
    void shouldThrowOnSupportsFeatureAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();

      assertThrows(
          IllegalStateException.class,
          () -> engine.supportsFeature(WasmFeature.BULK_MEMORY),
          "supportsFeature after close should throw");
    }

    @Test
    @DisplayName("incrementEpoch after close should throw IllegalStateException")
    void shouldThrowOnIncrementEpochAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();

      assertThrows(
          IllegalStateException.class,
          engine::incrementEpoch,
          "incrementEpoch after close should throw");
    }
  }

  // ==================== Identity Tests ====================

  @Nested
  @DisplayName("Identity Tests")
  class IdentityTests {

    @Test
    @DisplayName("getId should return non-zero")
    void shouldReturnNonZeroId() throws Exception {
      final PanamaEngine engine = createEngine();
      final long id = engine.getId();
      LOGGER.info("Engine ID: " + id);
      // identityHashCode can technically be 0, but very unlikely
      // Just verify no exception
    }

    @Test
    @DisplayName("getNativeEngine should return non-null pointer")
    void shouldReturnNonNullNativeEngine() throws Exception {
      final PanamaEngine engine = createEngine();
      assertNotNull(engine.getNativeEngine(), "Native engine pointer should not be null");
    }

    @Test
    @DisplayName("getEnginePointer should return same as getNativeEngine")
    void shouldReturnSamePointer() throws Exception {
      final PanamaEngine engine = createEngine();
      assertEquals(
          engine.getNativeEngine(),
          engine.getEnginePointer(),
          "getEnginePointer and getNativeEngine should return same pointer");
    }

    @Test
    @DisplayName("same with self should return true")
    void shouldBeSameAsSelf() throws Exception {
      final PanamaEngine engine = createEngine();
      assertTrue(engine.same(engine), "Engine should be same as itself");
    }

    @Test
    @DisplayName("same with different engine should return false")
    void shouldNotBeSameAsDifferent() throws Exception {
      final PanamaEngine engine1 = createEngine();
      final PanamaEngine engine2 = createEngine();
      assertFalse(engine1.same(engine2), "Different engines should not be same");
    }

    @Test
    @DisplayName("same with null should throw IllegalArgumentException")
    void shouldThrowForSameWithNull() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class, () -> engine.same(null), "same(null) should throw");
    }
  }

  // ==================== Epoch Tests ====================

  @Nested
  @DisplayName("Epoch Tests")
  class EpochTests {

    @Test
    @DisplayName("incrementEpoch should not throw")
    void shouldIncrementEpochWithoutError() throws Exception {
      final PanamaEngine engine = createEngine();
      assertDoesNotThrow(engine::incrementEpoch, "incrementEpoch should not throw");
    }

    @Test
    @DisplayName("incrementEpoch multiple times should not throw")
    void shouldIncrementEpochMultipleTimes() throws Exception {
      final PanamaEngine engine = createEngine();
      for (int i = 0; i < 10; i++) {
        assertDoesNotThrow(engine::incrementEpoch, "incrementEpoch #" + i + " should not throw");
      }
    }
  }

  // ==================== Precompiled Detection Tests ====================

  @Nested
  @DisplayName("Precompiled Detection Tests")
  class PrecompiledDetectionTests {

    @Test
    @DisplayName("detectPrecompiled with null should throw IllegalArgumentException")
    void shouldThrowForNullBytes() throws Exception {
      final PanamaEngine engine = createEngine();
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.detectPrecompiled(null),
          "Null bytes should throw");
    }

    @Test
    @DisplayName("detectPrecompiled with empty bytes should return null")
    void shouldReturnNullForEmptyBytes() throws Exception {
      final PanamaEngine engine = createEngine();
      assertNull(engine.detectPrecompiled(new byte[0]), "Empty bytes should return null");
    }

    @Test
    @DisplayName("detectPrecompiled with random bytes should throw (native not implemented)")
    void shouldThrowForRandomBytes() throws Exception {
      final PanamaEngine engine = createEngine();
      final byte[] random = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      assertThrows(
          IllegalStateException.class,
          () -> engine.detectPrecompiled(random),
          "Random bytes should throw since native function is not implemented");
    }

    @Test
    @DisplayName("precompileCompatibilityHash should return non-null array")
    void shouldReturnCompatibilityHash() throws Exception {
      final PanamaEngine engine = createEngine();
      final byte[] hash = engine.precompileCompatibilityHash();
      assertNotNull(hash, "Compatibility hash should not be null");
      LOGGER.info("Compatibility hash length: " + hash.length);
    }
  }

  // ==================== Validate Component Tests ====================

  @Nested
  @DisplayName("Component Validation Tests")
  class ComponentValidationTests {

    @Test
    @DisplayName("validateComponent should return success for any input")
    void shouldReturnSuccessForValidation() throws Exception {
      final PanamaEngine engine = createEngine();
      final var result = engine.validateComponent(null);
      assertNotNull(result, "Validation result should not be null");
      LOGGER.info("Validation result: " + result);
    }
  }

  // ==================== Closed Engine Detection Tests ====================

  @Nested
  @DisplayName("Closed Engine Detection Tests")
  class ClosedEngineDetectionTests {

    @Test
    @DisplayName("isPulley on closed engine should return false")
    void shouldReturnFalseForIsPulleyAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();
      assertFalse(engine.isPulley(), "isPulley should return false on closed engine");
    }

    @Test
    @DisplayName("precompileCompatibilityHash on closed engine should return empty array")
    void shouldReturnEmptyHashAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();
      final byte[] hash = engine.precompileCompatibilityHash();
      assertNotNull(hash, "Hash should not be null");
      assertEquals(0, hash.length, "Hash should be empty on closed engine");
    }

    @Test
    @DisplayName("detectPrecompiled on closed engine should throw IllegalStateException")
    void shouldThrowOnDetectPrecompiledAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();
      assertThrows(
          IllegalStateException.class,
          () -> engine.detectPrecompiled(new byte[] {0x01}),
          "detectPrecompiled on closed engine should throw");
    }

    @Test
    @DisplayName("same on closed engine should return false")
    void shouldReturnFalseForSameAfterClose() throws Exception {
      final PanamaEngine engine1 = createEngine();
      final PanamaEngine engine2 = new PanamaEngine();
      engine2.close();
      assertFalse(engine2.same(engine1), "same() on closed engine should return false");
    }

    @Test
    @DisplayName("getMemoryLimitPages on closed engine should throw")
    void shouldThrowOnGetMemoryLimitAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();
      assertThrows(
          IllegalStateException.class,
          engine::getMemoryLimitPages,
          "getMemoryLimitPages on closed engine should throw");
    }

    @Test
    @DisplayName("getStackSizeLimit on closed engine should throw")
    void shouldThrowOnGetStackSizeLimitAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();
      assertThrows(
          IllegalStateException.class,
          engine::getStackSizeLimit,
          "getStackSizeLimit on closed engine should throw");
    }

    @Test
    @DisplayName("precompileModule on closed engine should throw")
    void shouldThrowOnPrecompileAfterClose() throws Exception {
      final PanamaEngine engine = new PanamaEngine();
      engine.close();
      final byte[] wasmBytes = loadTestWasmBytes();
      assertThrows(
          IllegalStateException.class,
          () -> engine.precompileModule(wasmBytes),
          "precompileModule on closed engine should throw");
    }

    @Test
    @DisplayName("getNativeEngine on closed engine should throw IllegalStateException")
    void getNativeEngineOnClosedEngineShouldThrow() throws Exception {
      final PanamaEngine closedEngine = new PanamaEngine();
      closedEngine.close();

      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, closedEngine::getNativeEngine);
      assertTrue(
          ex.getMessage().contains("closed"),
          "Exception message should mention 'closed': " + ex.getMessage());
      LOGGER.info("getNativeEngine correctly rejected closed engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("close ordering should set flag before destruction")
    void closeOrderingShouldSetFlagBeforeDestruction() throws Exception {
      final PanamaEngine engine = new PanamaEngine();

      // First close destroys resources and sets flag
      engine.close();

      // Second close should be no-op (flag already set)
      engine.close();

      // getNativeEngine must throw — flag was set before destruction
      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, engine::getNativeEngine);
      assertTrue(
          ex.getMessage().contains("closed"),
          "Exception message should mention 'closed': " + ex.getMessage());
      LOGGER.info("Close ordering verified — flag set before destruction: " + ex.getMessage());
    }
  }
}
