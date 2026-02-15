package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaStore} that exercise actual native store creation, lifecycle management,
 * user data, fuel management, resource creation, and epoch handling.
 */
@DisplayName("PanamaStore Tests")
class PanamaStoreTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaStoreTest.class.getName());

  private PanamaEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    resources.add(engine);
    LOGGER.info("Created PanamaEngine for store tests");
  }

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

  private PanamaStore createStore() throws WasmException {
    final PanamaStore store = new PanamaStore(engine);
    resources.add(store);
    return store;
  }

  private PanamaModule compileWat(final String wat) throws WasmException {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    return module;
  }

  private static final String FUNCTION_MODULE_WAT =
      """
      (module
        (func (export "get42") (result i32)
          i32.const 42
        )
      )
      """;

  // ===== Constructor Tests =====

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should reject null engine")
    void shouldRejectNullEngine() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaStore(null));
      assertThat(ex.getMessage()).contains("Engine cannot be null");
      LOGGER.info("Correctly rejected null engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject closed engine")
    void shouldRejectClosedEngine() throws Exception {
      final PanamaEngine closedEngine = new PanamaEngine();
      closedEngine.close();

      assertThrows(IllegalStateException.class, () -> new PanamaStore(closedEngine));
      LOGGER.info("Correctly rejected closed engine");
    }

    @Test
    @DisplayName("Should create valid store from engine")
    void shouldCreateValidStore() throws Exception {
      final PanamaStore store = createStore();

      assertTrue(store.isValid(), "Store should be valid after creation");
      assertNotNull(store.getNativeStore(), "Native store pointer should not be null");
      assertThat(store.getEngine()).isSameAs(engine);
      LOGGER.info("Successfully created valid store");
    }

    @Test
    @DisplayName("Should reject null engine with limits")
    void shouldRejectNullEngineWithLimits() throws Exception {
      final StoreLimits limits = StoreLimits.builder().build();

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaStore(null, limits));
      assertThat(ex.getMessage()).contains("Engine cannot be null");
      LOGGER.info("Correctly rejected null engine with limits");
    }

    @Test
    @DisplayName("Should reject null limits")
    void shouldRejectNullLimits() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaStore(engine, null));
      assertThat(ex.getMessage()).contains("Limits cannot be null");
      LOGGER.info("Correctly rejected null limits");
    }

    @Test
    @DisplayName("Should create store with limits")
    void shouldCreateStoreWithLimits() throws Exception {
      final StoreLimits limits =
          StoreLimits.builder().memorySize(1024 * 1024).instances(10).tableElements(100).build();
      final PanamaStore store = new PanamaStore(engine, limits);
      resources.add(store);

      assertTrue(store.isValid(), "Store with limits should be valid");
      LOGGER.info("Created store with limits successfully");
    }
  }

  // ===== forModule Factory Tests =====

  @Nested
  @DisplayName("forModule Factory Tests")
  class ForModuleTests {

    @Test
    @DisplayName("Should reject null module")
    void shouldRejectNullModule() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> PanamaStore.forModule(null));
      assertThat(ex.getMessage()).contains("module cannot be null");
      LOGGER.info("Correctly rejected null module");
    }

    @Test
    @DisplayName("forModule should create a store from a module")
    void shouldCreateStoreFromModule() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final PanamaStore moduleStore = PanamaStore.forModule(module);
      assertNotNull(moduleStore, "Store created from module should not be null");
      assertNotNull(moduleStore.getEngine(), "Store engine should not be null");
      moduleStore.close();
      LOGGER.info("forModule successfully created store from module");
    }

    @Test
    @DisplayName("Should reject closed module")
    void shouldRejectClosedModule() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);
      module.close();

      assertThrows(IllegalStateException.class, () -> PanamaStore.forModule(module));
      LOGGER.info("Correctly rejected closed module");
    }
  }

  // ===== User Data Tests =====

  @Nested
  @DisplayName("User Data Tests")
  class UserDataTests {

    @Test
    @DisplayName("Should store and retrieve user data")
    void shouldStoreAndRetrieveUserData() throws Exception {
      final PanamaStore store = createStore();

      assertNull(store.getData(), "Initial user data should be null");

      store.setData("test-data");
      assertEquals("test-data", store.getData());

      store.setData(42);
      assertEquals(42, store.getData());

      store.setData(null);
      assertNull(store.getData(), "User data should be null after setting null");
      LOGGER.info("User data storage and retrieval works correctly");
    }
  }

  // ===== Lifecycle Tests =====

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Store should be valid after creation")
    void storeShouldBeValidAfterCreation() throws Exception {
      final PanamaStore store = createStore();

      assertTrue(store.isValid(), "Store should be valid");
      LOGGER.info("Store is valid after creation");
    }

    @Test
    @DisplayName("Store should be invalid after close")
    void storeShouldBeInvalidAfterClose() throws Exception {
      final PanamaStore store = new PanamaStore(engine);

      store.close();
      assertFalse(store.isValid(), "Store should be invalid after close");
      LOGGER.info("Store is invalid after close");
    }

    @Test
    @DisplayName("Double close should not throw")
    void doubleCloseShouldNotThrow() throws Exception {
      final PanamaStore store = new PanamaStore(engine);

      store.close();
      assertDoesNotThrow(store::close, "Double close should not throw");
      LOGGER.info("Double close succeeded without exception");
    }

    @Test
    @DisplayName("Operations on closed store should throw")
    void operationsOnClosedStoreShouldThrow() throws Exception {
      final PanamaStore store = new PanamaStore(engine);
      store.close();

      assertThrows(IllegalStateException.class, () -> store.setFuel(100));
      assertThrows(IllegalStateException.class, store::getFuel);
      assertThrows(IllegalStateException.class, () -> store.addFuel(50));
      assertThrows(IllegalStateException.class, () -> store.consumeFuel(10));
      assertThrows(IllegalStateException.class, () -> store.setEpochDeadline(1));
      assertThrows(IllegalStateException.class, store::getCallbackRegistry);
      assertThrows(IllegalStateException.class, store::gc);
      LOGGER.info("All operations correctly throw on closed store");
    }

    @Test
    @DisplayName("Should return engine reference")
    void shouldReturnEngineReference() throws Exception {
      final PanamaStore store = createStore();

      assertThat(store.getEngine()).isSameAs(engine);
      LOGGER.info("Store returns correct engine reference");
    }

    @Test
    @DisplayName("Should provide resource manager")
    void shouldProvideResourceManager() throws Exception {
      final PanamaStore store = createStore();

      assertNotNull(store.getResourceManager(), "Resource manager should not be null");
      LOGGER.info("Resource manager is available");
    }
  }

  // ===== Fuel Management Tests =====

  @Nested
  @DisplayName("Fuel Management Tests")
  class FuelManagementTests {

    @Test
    @DisplayName("Should reject negative fuel for setFuel")
    void shouldRejectNegativeFuelForSet() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.setFuel(-1));
      LOGGER.info("Correctly rejected negative fuel for setFuel");
    }

    @Test
    @DisplayName("Should reject negative fuel for addFuel")
    void shouldRejectNegativeFuelForAdd() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.addFuel(-1));
      LOGGER.info("Correctly rejected negative fuel for addFuel");
    }

    @Test
    @DisplayName("Should reject negative fuel for consumeFuel")
    void shouldRejectNegativeFuelForConsume() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.consumeFuel(-1));
      LOGGER.info("Correctly rejected negative fuel for consumeFuel");
    }

    @Test
    @DisplayName("getRemainingFuel should delegate to getFuel")
    void getRemainingFuelShouldDelegateToGetFuel() throws Exception {
      final PanamaStore store = createStore();

      // Both methods should return the same value or throw the same exception
      try {
        final long fuel = store.getFuel();
        final long remaining = store.getRemainingFuel();
        assertEquals(fuel, remaining, "getRemainingFuel should match getFuel");
        LOGGER.info("getRemainingFuel correctly delegates to getFuel: " + fuel);
      } catch (final WasmException e) {
        // If getFuel throws, getRemainingFuel should also throw
        assertThrows(WasmException.class, store::getRemainingFuel);
        LOGGER.info("Both getFuel and getRemainingFuel throw consistently");
      }
    }
  }

  // ===== Callback Registry Tests =====

  @Nested
  @DisplayName("Callback Registry Tests")
  class CallbackRegistryTests {

    @Test
    @DisplayName("Should lazily create callback registry")
    void shouldLazilyCreateCallbackRegistry() throws Exception {
      final PanamaStore store = createStore();

      final CallbackRegistry registry = store.getCallbackRegistry();
      assertNotNull(registry, "Callback registry should not be null");
      assertThat(registry).isInstanceOf(PanamaCallbackRegistry.class);
      LOGGER.info("Callback registry created lazily");
    }

    @Test
    @DisplayName("Should return same callback registry on subsequent calls")
    void shouldReturnSameCallbackRegistry() throws Exception {
      final PanamaStore store = createStore();

      final CallbackRegistry registry1 = store.getCallbackRegistry();
      final CallbackRegistry registry2 = store.getCallbackRegistry();
      assertThat(registry1).isSameAs(registry2);
      LOGGER.info("Callback registry is singleton per store");
    }
  }

  // ===== Host Function Creation Tests =====

  @Nested
  @DisplayName("Host Function Creation Tests")
  class HostFunctionCreationTests {

    @Test
    @DisplayName("Should reject null name for createHostFunction")
    void shouldRejectNullName() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () ->
              store.createHostFunction(
                  null,
                  ai.tegmentum.wasmtime4j.type.FunctionType.of(
                      new WasmValueType[] {}, new WasmValueType[] {}),
                  params -> new WasmValue[0]));
      LOGGER.info("Correctly rejected null function name");
    }

    @Test
    @DisplayName("Should reject null function type")
    void shouldRejectNullFunctionType() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> store.createHostFunction("test", null, params -> new WasmValue[0]));
      LOGGER.info("Correctly rejected null function type");
    }

    @Test
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () ->
              store.createHostFunction(
                  "test",
                  ai.tegmentum.wasmtime4j.type.FunctionType.of(
                      new WasmValueType[] {}, new WasmValueType[] {}),
                  null));
      LOGGER.info("Correctly rejected null implementation");
    }

    @Test
    @DisplayName("Should create host function")
    void shouldCreateHostFunction() throws Exception {
      final PanamaStore store = createStore();

      final ai.tegmentum.wasmtime4j.WasmFunction hostFunc =
          store.createHostFunction(
              "test_func",
              ai.tegmentum.wasmtime4j.type.FunctionType.of(
                  new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32}),
              params -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)});

      assertNotNull(hostFunc, "Host function should not be null");
      LOGGER.info("Created host function successfully");
    }
  }

  // ===== Global Creation Tests =====

  @Nested
  @DisplayName("Global Creation Tests")
  class GlobalCreationTests {

    @Test
    @DisplayName("Should reject null value type")
    void shouldRejectNullValueType() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class, () -> store.createGlobal(null, true, WasmValue.i32(0)));
      LOGGER.info("Correctly rejected null value type");
    }

    @Test
    @DisplayName("Should reject null initial value")
    void shouldRejectNullInitialValue() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class, () -> store.createGlobal(WasmValueType.I32, true, null));
      LOGGER.info("Correctly rejected null initial value");
    }

    @Test
    @DisplayName("Should reject mismatched value type")
    void shouldRejectMismatchedValueType() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> store.createGlobal(WasmValueType.I64, true, WasmValue.i32(42)));
      LOGGER.info("Correctly rejected mismatched value type");
    }
  }

  // ===== Table Creation Tests =====

  @Nested
  @DisplayName("Table Creation Tests")
  class TableCreationTests {

    @Test
    @DisplayName("Should reject null element type")
    void shouldRejectNullElementType() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.createTable(null, 1, 10));
      LOGGER.info("Correctly rejected null element type");
    }

    @Test
    @DisplayName("Should reject negative initial size")
    void shouldRejectNegativeInitialSize() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, -1, 10));
      LOGGER.info("Correctly rejected negative initial size");
    }

    @Test
    @DisplayName("Should reject invalid max size")
    void shouldRejectInvalidMaxSize() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, 1, -2));
      LOGGER.info("Correctly rejected invalid max size");
    }
  }

  // ===== Memory Creation Tests =====

  @Nested
  @DisplayName("Memory Creation Tests")
  class MemoryCreationTests {

    @Test
    @DisplayName("Should reject negative initial pages")
    void shouldRejectNegativeInitialPages() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.createMemory(-1, 10));
      LOGGER.info("Correctly rejected negative initial pages");
    }

    @Test
    @DisplayName("Should reject invalid max pages")
    void shouldRejectInvalidMaxPages() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.createMemory(1, -2));
      LOGGER.info("Correctly rejected invalid max pages");
    }

    @Test
    @DisplayName("Should reject invalid shared memory max pages")
    void shouldRejectInvalidSharedMemoryMaxPages() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.createSharedMemory(1, 0));
      assertThrows(IllegalArgumentException.class, () -> store.createSharedMemory(1, -1));
      assertThrows(IllegalArgumentException.class, () -> store.createSharedMemory(-1, 10));
      LOGGER.info("Correctly rejected invalid shared memory parameters");
    }
  }

  // ===== Instance Creation Tests =====

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("Should reject null module for createInstance")
    void shouldRejectNullModule() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.createInstance(null));
      LOGGER.info("Correctly rejected null module for createInstance");
    }
  }

  // ===== Execution Stats Tests =====

  @Nested
  @DisplayName("Execution Stats Tests")
  class ExecutionStatsTests {

    @Test
    @DisplayName("Execution count on closed store should return 0")
    void executionCountOnClosedStoreShouldReturnZero() throws Exception {
      final PanamaStore store = new PanamaStore(engine);
      store.close();

      assertEquals(0, store.getExecutionCount(), "Closed store execution count should be 0");
      LOGGER.info("Closed store correctly returns 0 for execution count");
    }

    @Test
    @DisplayName("Execution time on closed store should return 0")
    void executionTimeOnClosedStoreShouldReturnZero() throws Exception {
      final PanamaStore store = new PanamaStore(engine);
      store.close();

      assertEquals(
          0, store.getTotalExecutionTimeMicros(), "Closed store execution time should be 0");
      LOGGER.info("Closed store correctly returns 0 for execution time");
    }

    @Test
    @DisplayName("Pending exception on closed store should return false")
    void pendingExceptionOnClosedStoreShouldReturnFalse() throws Exception {
      final PanamaStore store = new PanamaStore(engine);
      store.close();

      assertFalse(store.hasPendingException(), "Closed store should not have pending exception");
      LOGGER.info("Closed store correctly returns false for hasPendingException");
    }
  }

  // ===== Epoch Tests =====

  @Nested
  @DisplayName("Epoch Handling Tests")
  class EpochHandlingTests {

    @Test
    @DisplayName("Should reject negative delta ticks for async yield")
    void shouldRejectNegativeDeltaTicks() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class, () -> store.epochDeadlineAsyncYieldAndUpdate(-1));
      LOGGER.info("Correctly rejected negative delta ticks");
    }
  }

  // ===== Fuel Async Yield Interval Tests =====

  @Nested
  @DisplayName("Fuel Async Yield Interval Tests")
  class FuelAsyncYieldTests {

    @Test
    @DisplayName("Should set and get fuel async yield interval")
    void shouldSetAndGetFuelAsyncYieldInterval() throws Exception {
      final PanamaStore store = createStore();

      assertEquals(0, store.getFuelAsyncYieldInterval(), "Default should be 0");

      store.setFuelAsyncYieldInterval(1000);
      assertEquals(1000, store.getFuelAsyncYieldInterval());
      LOGGER.info("Fuel async yield interval set and retrieved correctly");
    }

    @Test
    @DisplayName("Should reject negative interval")
    void shouldRejectNegativeInterval() throws Exception {
      final PanamaStore store = createStore();

      assertThrows(IllegalArgumentException.class, () -> store.setFuelAsyncYieldInterval(-1));
      LOGGER.info("Correctly rejected negative fuel async yield interval");
    }
  }

}
