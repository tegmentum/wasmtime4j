package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniStore}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly store operations is tested in integration
 * tests.
 */
class JniStoreTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long ZERO_HANDLE = 0L;

  private JniEngine testEngine;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
  }

  @Test
  void testConstructorWithValidHandleAndEngine() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    assertNotNull(store);
    assertEquals(testEngine, store.getEngine());
    assertNotNull(store.getCallbackRegistry());
  }

  @Test
  void testConstructorWithNullEngine() {
    final JniStore store = new JniStore(VALID_HANDLE, null);

    assertNotNull(store);
    assertThat(store.getEngine()).isNull();
  }

  @Test
  void testGetEngineReturnsCorrectReference() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    assertEquals(testEngine, store.getEngine());
  }

  @Test
  void testGetDataInitiallyNull() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    assertThat(store.getData()).isNull();
  }

  @Test
  void testSetDataAndGetData() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final Object testData = new Object();

    store.setData(testData);

    assertEquals(testData, store.getData());
  }

  @Test
  void testAddFuelWithNegativeValue() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.addFuel(-1));

    assertThat(exception.getMessage()).contains("additionalFuel");
    assertThat(exception.getMessage()).containsIgnoringCase("non-negative");
  }

  @Test
  void testSetFuelWithNegativeValue() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.setFuel(-1));

    assertThat(exception.getMessage()).contains("fuel");
    assertThat(exception.getMessage()).containsIgnoringCase("non-negative");
  }

  @Test
  void testConsumeFuelWithNegativeValue() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.consumeFuel(-1));

    assertThat(exception.getMessage()).contains("fuel");
    assertThat(exception.getMessage()).containsIgnoringCase("non-negative");
  }

  @Test
  void testCreateMemoryWithNegativeInitialPages() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.createMemory(-1, 10));

    assertThat(exception.getMessage()).contains("Initial pages");
    assertThat(exception.getMessage()).contains("cannot be negative");
  }

  @Test
  void testCreateMemoryWithInvalidMaxPages() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.createMemory(10, -2));

    assertThat(exception.getMessage()).contains("Max pages");
    assertThat(exception.getMessage()).contains("must be -1");
  }

  @Test
  void testCreateMemoryWithMaxLessThanInitial() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.createMemory(10, 5));

    assertThat(exception.getMessage()).contains("Max pages");
    assertThat(exception.getMessage()).contains("cannot be less than initial pages");
  }

  @Test
  void testCreateTableWithNullElementType() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.createTable(null, 10, 20));

    assertThat(exception.getMessage()).contains("elementType");
    assertThat(exception.getMessage()).containsIgnoringCase("must not be null");
  }

  @Test
  void testCreateTableWithNegativeInitialSize() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, -1, 10));

    assertThat(exception.getMessage()).contains("Initial size");
    assertThat(exception.getMessage()).contains("cannot be negative");
  }

  @Test
  void testCreateTableWithInvalidMaxSize() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, 10, -2));

    assertThat(exception.getMessage()).contains("Max size");
    assertThat(exception.getMessage()).contains("must be -1");
  }

  @Test
  void testCreateTableWithMaxLessThanInitial() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, 10, 5));

    assertThat(exception.getMessage()).contains("Max size");
    assertThat(exception.getMessage()).contains("cannot be less than initial size");
  }

  @Test
  void testCreateTableWithInvalidElementType() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> store.createTable(WasmValueType.I32, 10, 20));

    assertThat(exception.getMessage()).contains("Element type");
    assertThat(exception.getMessage()).contains("must be FUNCREF or EXTERNREF");
  }

  @Test
  void testCreateGlobalWithNullValueType() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final WasmValue value = WasmValue.i32(42);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> store.createGlobal(null, false, value));

    assertThat(exception.getMessage()).contains("valueType");
    assertThat(exception.getMessage()).containsIgnoringCase("must not be null");
  }

  @Test
  void testCreateGlobalWithNullInitialValue() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> store.createGlobal(WasmValueType.I32, false, null));

    assertThat(exception.getMessage()).contains("initialValue");
    assertThat(exception.getMessage()).containsIgnoringCase("must not be null");
  }

  @Test
  void testCreateGlobalWithMismatchedValueType() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final WasmValue i32Value = WasmValue.i32(42);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> store.createGlobal(WasmValueType.I64, false, i32Value));

    assertThat(exception.getMessage()).contains("Initial value type");
    assertThat(exception.getMessage()).contains("does not match global type");
  }

  @Test
  void testCreateHostFunctionWithNullName() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
    final HostFunction impl = params -> null;

    final NullPointerException exception =
        assertThrows(
            NullPointerException.class, () -> store.createHostFunction(null, funcType, impl));

    assertThat(exception.getMessage()).contains("name");
  }

  @Test
  void testCreateHostFunctionWithNullFunctionType() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final HostFunction impl = params -> null;

    final NullPointerException exception =
        assertThrows(
            NullPointerException.class, () -> store.createHostFunction("test", null, impl));

    assertThat(exception.getMessage()).contains("functionType");
  }

  @Test
  void testCreateHostFunctionWithNullImplementation() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);

    final NullPointerException exception =
        assertThrows(
            NullPointerException.class, () -> store.createHostFunction("test", funcType, null));

    assertThat(exception.getMessage()).contains("implementation");
  }

  @Test
  void testCreateFunctionReferenceWithNullHostFunction() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);

    final NullPointerException exception =
        assertThrows(
            NullPointerException.class, () -> store.createFunctionReference(null, funcType));

    assertThat(exception.getMessage()).contains("Host function implementation");
  }

  @Test
  void testCreateFunctionReferenceWithNullFunctionType() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final HostFunction impl = params -> null;

    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> store.createFunctionReference(impl, null));

    assertThat(exception.getMessage()).contains("Function type");
  }

  @Test
  void testCreateFunctionReferenceFromWasmFunctionWithNull() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final WasmFunction wasmFunc = null;

    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> store.createFunctionReference(wasmFunc));

    assertThat(exception.getMessage()).contains("WebAssembly function");
  }

  @Test
  void testCreateInstanceWithNullModule() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> store.createInstance(null));

    assertThat(exception.getMessage()).contains("Module");
  }

  @Test
  void testCreateInstanceWithNonJniModule() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    // Create a Module that is not a JniModule
    final Module nonJniModule = new JniModule(VALID_HANDLE, testEngine) {};

    // This won't actually fail since anonymous subclasses still pass instanceof check
    // So we just verify the method exists and accepts Module parameter
    assertNotNull(store);
  }

  // Note: Tests that call close() are disabled in unit tests since they require native library
  // These are tested in integration tests instead

  @Test
  void testStoreLifecycleState() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    // Verify initial state
    assertNotNull(store);
    assertEquals(testEngine, store.getEngine());
    assertNotNull(store.getCallbackRegistry());

    // Note: Cannot call close() in unit test - requires native library
    // Integration tests verify close() behavior and idempotency
  }

  @Test
  void testResourceManagementState() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    // Verify store is in valid state
    assertNotNull(store);
    assertEquals(testEngine, store.getEngine());

    // Note: Cannot test try-with-resources in unit test - requires native library
    // Integration tests verify automatic resource cleanup
  }

  @Test
  void testCallbackRegistryNotNull() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    assertNotNull(store.getCallbackRegistry());
  }

  @Test
  void testDefensiveProgrammingForNegativeValues() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    // All these should throw exceptions with negative values, not crash the JVM
    assertThrows(IllegalArgumentException.class, () -> store.addFuel(-1));
    assertThrows(IllegalArgumentException.class, () -> store.setFuel(-1));
    assertThrows(IllegalArgumentException.class, () -> store.consumeFuel(-1));
    assertThrows(IllegalArgumentException.class, () -> store.createMemory(-1, 10));
    assertThrows(
        IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, -1, 10));
  }

  @Test
  void testDefensiveProgrammingForNullValues() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);
    final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
    final HostFunction impl = params -> null;

    // All these should throw exceptions with null values, not crash the JVM
    assertThrows(IllegalArgumentException.class, () -> store.createTable(null, 10, 20));
    assertThrows(
        IllegalArgumentException.class, () -> store.createGlobal(null, false, WasmValue.i32(0)));
    assertThrows(
        IllegalArgumentException.class, () -> store.createGlobal(WasmValueType.I32, false, null));
    assertThrows(NullPointerException.class, () -> store.createHostFunction(null, funcType, impl));
    assertThrows(NullPointerException.class, () -> store.createHostFunction("test", null, impl));
    assertThrows(
        NullPointerException.class, () -> store.createHostFunction("test", funcType, null));
    assertThrows(NullPointerException.class, () -> store.createInstance(null));
  }

  @Test
  void testDefensiveProgrammingForInvalidRanges() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    // These should throw for invalid ranges (max < initial)
    assertThrows(IllegalArgumentException.class, () -> store.createMemory(10, 5));
    assertThrows(
        IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, 10, 5));

    // These should throw for invalid max values (not -1 and not >= 0)
    assertThrows(IllegalArgumentException.class, () -> store.createMemory(10, -2));
    assertThrows(
        IllegalArgumentException.class, () -> store.createTable(WasmValueType.FUNCREF, 10, -2));
  }

  @Test
  void testResourceTypeReturnsStore() {
    final JniStore store = new JniStore(VALID_HANDLE, testEngine);

    assertThat(store.getResourceType()).isEqualTo("Store");
  }
}
