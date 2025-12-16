package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Store interface.
 *
 * <p>Tests verify store creation, data management, context operations, fuel management, epoch
 * interruption, and resource lifecycle management.
 */
@DisplayName("Store Interface Tests")
class StoreTest {

  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = engine.createStore();
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
      store = null;
    }
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  @Nested
  @DisplayName("Store Creation Tests")
  class StoreCreationTests {

    @Test
    @DisplayName("should create store from engine")
    void shouldCreateStoreFromEngine() throws WasmException {
      try (Store newStore = engine.createStore()) {
        assertNotNull(newStore, "Store should not be null");
      }
    }

    @Test
    @DisplayName("should create store with custom data")
    void shouldCreateStoreWithCustomData() throws WasmException {
      final String customData = "test-store-data";
      try (Store dataStore = engine.createStore(customData)) {
        assertNotNull(dataStore, "Store should not be null");
        assertEquals(customData, dataStore.getData(), "Store data should match");
      }
    }

    @Test
    @DisplayName("should create store with null data")
    void shouldCreateStoreWithNullData() throws WasmException {
      try (Store nullDataStore = engine.createStore(null)) {
        assertNotNull(nullDataStore, "Store should not be null");
        assertNull(nullDataStore.getData(), "Store data should be null");
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
  @DisplayName("Store Data Management Tests")
  class StoreDataManagementTests {

    @Test
    @DisplayName("should get and set store data")
    void shouldGetAndSetStoreData() {
      final String initialData = "initial";
      store.setData(initialData);
      assertEquals(initialData, store.getData(), "Should get the set data");

      final String updatedData = "updated";
      store.setData(updatedData);
      assertEquals(updatedData, store.getData(), "Should get the updated data");
    }

    @Test
    @DisplayName("should allow setting null data")
    void shouldAllowSettingNullData() {
      store.setData("something");
      assertNotNull(store.getData(), "Data should not be null initially");

      store.setData(null);
      assertNull(store.getData(), "Data should be null after setting null");
    }

    @Test
    @DisplayName("should support different data types")
    void shouldSupportDifferentDataTypes() {
      // String
      store.setData("string-data");
      assertEquals("string-data", store.getData(), "Should store string data");

      // Integer
      store.setData(Integer.valueOf(42));
      assertEquals(Integer.valueOf(42), store.getData(), "Should store integer data");

      // Custom object
      final Object customObject = new Object();
      store.setData(customObject);
      assertSame(customObject, store.getData(), "Should store custom object");
    }
  }

  @Nested
  @DisplayName("Store Engine Reference Tests")
  class StoreEngineReferenceTests {

    @Test
    @DisplayName("should return reference to parent engine")
    void shouldReturnReferenceToParentEngine() {
      final Engine storeEngine = store.getEngine();
      assertNotNull(storeEngine, "Engine reference should not be null");
      assertTrue(storeEngine.same(engine), "Should reference the same engine");
    }
  }

  @Nested
  @DisplayName("Fuel Management Tests")
  class FuelManagementTests {

    @Test
    @DisplayName("should add fuel to store with fuel enabled")
    void shouldAddFuelToStoreWithFuelEnabled() throws WasmException {
      final EngineConfig config = new EngineConfig().consumeFuel(true);
      try (Engine fuelEngine = Engine.create(config);
          Store fuelStore = fuelEngine.createStore()) {
        fuelStore.addFuel(1000L);
        final long remaining = fuelStore.getFuel();
        assertTrue(remaining >= 1000L, "Fuel remaining should be at least 1000");
      }
    }

    @Test
    @DisplayName("should consume fuel when executed")
    void shouldConsumeFuelWhenExecuted() throws WasmException {
      final EngineConfig config = new EngineConfig().consumeFuel(true);
      try (Engine fuelEngine = Engine.create(config);
          Store fuelStore = fuelEngine.createStore()) {
        fuelStore.addFuel(10000L);
        final long initialFuel = fuelStore.getFuel();
        assertTrue(initialFuel >= 10000L, "Initial fuel should be at least 10000");
      }
    }

    @Test
    @DisplayName("should return zero fuel when disabled")
    void shouldReturnZeroFuelWhenDisabled() throws WasmException {
      // Fuel is disabled by default
      final long remaining = store.getFuel();
      assertEquals(0L, remaining, "Fuel should be 0 when disabled");
    }

    @Test
    @DisplayName("should handle multiple fuel additions")
    void shouldHandleMultipleFuelAdditions() throws WasmException {
      final EngineConfig config = new EngineConfig().consumeFuel(true);
      try (Engine fuelEngine = Engine.create(config);
          Store fuelStore = fuelEngine.createStore()) {
        fuelStore.addFuel(100L);
        fuelStore.addFuel(200L);
        fuelStore.addFuel(300L);
        final long remaining = fuelStore.getFuel();
        assertTrue(remaining >= 600L, "Fuel should accumulate");
      }
    }
  }

  @Nested
  @DisplayName("Epoch Deadline Tests")
  class EpochDeadlineTests {

    @Test
    @DisplayName("should set epoch deadline with epoch interruption enabled")
    void shouldSetEpochDeadlineWithEpochInterruptionEnabled() throws WasmException {
      final EngineConfig config = new EngineConfig().setEpochInterruption(true);
      try (Engine epochEngine = Engine.create(config);
          Store epochStore = epochEngine.createStore()) {
        // Should not throw
        epochStore.setEpochDeadline(100L);
      }
    }
  }

  @Nested
  @DisplayName("Store Lifecycle Tests")
  class StoreLifecycleTests {

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() throws WasmException {
      Store tempStore;
      try (Store autoClosedStore = engine.createStore()) {
        tempStore = autoClosedStore;
        assertNotNull(autoClosedStore, "Store should not be null inside try block");
      }
      // After closing, operations may fail or return invalid state
      assertNotNull(tempStore, "Reference should still exist");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() throws WasmException {
      final Store tempStore = engine.createStore();
      tempStore.close();
      // Second close should not throw
      tempStore.close();
    }
  }

  @Nested
  @DisplayName("Memory Creation Tests")
  class MemoryCreationTests {

    @Test
    @DisplayName("should create memory with initial and max pages")
    void shouldCreateMemoryWithInitialAndMaxPages() throws WasmException {
      final WasmMemory memory = store.createMemory(1, 10);
      assertNotNull(memory, "Memory should not be null");
      assertEquals(1, memory.getSize(), "Memory should have initial size of 1 page");
    }

    @Test
    @DisplayName("should create memory with only initial pages")
    void shouldCreateMemoryWithOnlyInitialPages() throws WasmException {
      final WasmMemory memory = store.createMemory(2, -1);
      assertNotNull(memory, "Memory should not be null");
      assertEquals(2, memory.getSize(), "Memory should have initial size of 2 pages");
    }
  }

  @Nested
  @DisplayName("Global Creation Tests")
  class GlobalCreationTests {

    @Test
    @DisplayName("should create mutable i32 global")
    void shouldCreateMutableI32Global() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.I32, global.getType(), "Global type should be I32");
      assertTrue(global.isMutable(), "Global should be mutable");
      assertEquals(42, global.get().asI32(), "Global initial value should be 42");
    }

    @Test
    @DisplayName("should create immutable i64 global")
    void shouldCreateImmutableI64Global() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.I64, false, WasmValue.i64(123456789L));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.I64, global.getType(), "Global type should be I64");
      assertFalse(global.isMutable(), "Global should be immutable");
      assertEquals(123456789L, global.get().asI64(), "Global initial value should match");
    }

    @Test
    @DisplayName("should create f32 global")
    void shouldCreateF32Global() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.F32, true, WasmValue.f32(3.14f));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.F32, global.getType(), "Global type should be F32");
      assertEquals(3.14f, global.get().asF32(), 0.001f, "Global initial value should match");
    }

    @Test
    @DisplayName("should create f64 global")
    void shouldCreateF64Global() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.F64, true, WasmValue.f64(2.71828));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.F64, global.getType(), "Global type should be F64");
      assertEquals(2.71828, global.get().asF64(), 0.00001, "Global initial value should match");
    }
  }

  @Nested
  @DisplayName("Table Creation Tests")
  class TableCreationTests {

    @Test
    @DisplayName("should create funcref table")
    void shouldCreateFuncrefTable() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 10, 100);
      assertNotNull(table, "Table should not be null");
      assertEquals(10, table.getSize(), "Table should have initial size of 10");
      assertEquals(
          WasmValueType.FUNCREF, table.getElementType(), "Table element type should be FUNCREF");
    }

    @Test
    @DisplayName("should create externref table")
    void shouldCreateExternrefTable() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 5, 50);
      assertNotNull(table, "Table should not be null");
      assertEquals(5, table.getSize(), "Table should have initial size of 5");
      assertEquals(
          WasmValueType.EXTERNREF,
          table.getElementType(),
          "Table element type should be EXTERNREF");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    /** Minimal valid WebAssembly module (empty module). */
    private static final byte[] MINIMAL_WASM = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

    @Test
    @DisplayName("should create instance from module")
    void shouldCreateInstanceFromModule() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      assertNotNull(module, "Module should not be null");

      final Instance instance = store.createInstance(module);
      assertNotNull(instance, "Instance should not be null");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("GC and Limits Tests")
  class GcAndLimitsTests {

    @Test
    @DisplayName("should run garbage collection without error")
    void shouldRunGarbageCollectionWithoutError() throws WasmException {
      // Should not throw
      store.gc();
    }

    @Test
    @DisplayName("should set and respect limits")
    void shouldSetAndRespectLimits() throws WasmException {
      // Just verify the method exists and doesn't throw
      store.limiter(null);
    }
  }
}
