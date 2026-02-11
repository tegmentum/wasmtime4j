package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.type.WasmTypeException;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Test suite for PanamaGlobal operations (get/set). */
public class PanamaGlobalTest {

  private PanamaEngine engine;
  private PanamaStore store;
  private PanamaModule module;
  private PanamaInstance instance;

  /** Sets up test fixtures before each test. */
  @BeforeEach
  public void setUp() throws Exception {
    // Load the test WASM file with memory, table, and global exports
    // Globals: g_i32 (mut i32, value 42), g_i64 (mut i64, value 100), g_f32 (mut f32, value 3.14)
    final Path wasmPath =
        Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
    final byte[] wasmBytes = Files.readAllBytes(wasmPath);

    engine = new PanamaEngine();
    store = new PanamaStore(engine);
    module = new PanamaModule(engine, wasmBytes);
    instance = new PanamaInstance(module, store);
  }

  /** Cleans up test fixtures after each test. */
  @AfterEach
  public void tearDown() {
    if (instance != null) {
      instance.close();
    }
    if (store != null) {
      store.close();
    }
    if (module != null) {
      module.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("Get I32 global value")
  public void testGetI32Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent(), "Global g_i32 should be present");

    final WasmGlobal global = globalOpt.get();
    assertEquals(WasmValueType.I32, global.getType(), "Global should be I32 type");
    assertTrue(global.isMutable(), "Global should be mutable");

    final WasmValue value = global.get();
    assertEquals(WasmValueType.I32, value.getType(), "Value should be I32 type");
    assertEquals(42, value.asI32(), "Initial value should be 42");
  }

  @Test
  @DisplayName("Set I32 global value")
  public void testSetI32Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Set new value
    global.set(WasmValue.i32(123));

    // Verify new value
    final WasmValue newValue = global.get();
    assertEquals(123, newValue.asI32(), "Value should be updated to 123");
  }

  @Test
  @DisplayName("Get I64 global value")
  public void testGetI64Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i64");
    assertTrue(globalOpt.isPresent(), "Global g_i64 should be present");

    final WasmGlobal global = globalOpt.get();
    assertEquals(WasmValueType.I64, global.getType(), "Global should be I64 type");
    assertTrue(global.isMutable(), "Global should be mutable");

    final WasmValue value = global.get();
    assertEquals(WasmValueType.I64, value.getType(), "Value should be I64 type");
    assertEquals(100L, value.asI64(), "Initial value should be 100");
  }

  @Test
  @DisplayName("Set I64 global value")
  public void testSetI64Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i64");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Set new value
    global.set(WasmValue.i64(999L));

    // Verify new value
    final WasmValue newValue = global.get();
    assertEquals(999L, newValue.asI64(), "Value should be updated to 999");
  }

  @Test
  @DisplayName("Get F32 global value")
  public void testGetF32Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_f32");
    assertTrue(globalOpt.isPresent(), "Global g_f32 should be present");

    final WasmGlobal global = globalOpt.get();
    assertEquals(WasmValueType.F32, global.getType(), "Global should be F32 type");
    assertTrue(global.isMutable(), "Global should be mutable");

    final WasmValue value = global.get();
    assertEquals(WasmValueType.F32, value.getType(), "Value should be F32 type");
    assertEquals(3.14f, value.asF32(), 0.01f, "Initial value should be approximately 3.14");
  }

  @Test
  @DisplayName("Set F32 global value")
  public void testSetF32Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_f32");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Set new value
    global.set(WasmValue.f32(1.23f));

    // Verify new value
    final WasmValue newValue = global.get();
    assertEquals(1.23f, newValue.asF32(), 0.01f, "Value should be updated to approximately 1.23");
  }

  @Test
  @DisplayName("Set global with wrong type throws exception")
  public void testSetGlobalWrongTypeThrows() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Attempt to set I32 global with I64 value should throw
    assertThrows(
        WasmTypeException.class,
        () -> global.set(WasmValue.i64(999L)),
        "Setting global with wrong type should throw WasmTypeException");
  }

  @Test
  @DisplayName("Set null value throws exception")
  public void testSetNullValueThrows() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Attempt to set null value should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> global.set(null),
        "Setting null value should throw IllegalArgumentException");
  }

  @Test
  @DisplayName("Multiple set operations on same global")
  public void testMultipleSetOperations() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Set value multiple times
    global.set(WasmValue.i32(1));
    assertEquals(1, global.get().asI32());

    global.set(WasmValue.i32(2));
    assertEquals(2, global.get().asI32());

    global.set(WasmValue.i32(3));
    assertEquals(3, global.get().asI32());
  }

  @Test
  @DisplayName("Negative I32 global value")
  public void testNegativeI32Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Set negative value
    global.set(WasmValue.i32(-456));

    // Verify negative value
    final WasmValue value = global.get();
    assertEquals(-456, value.asI32(), "Value should be -456");
  }

  @Test
  @DisplayName("Negative F32 global value")
  public void testNegativeF32Global() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_f32");
    assertTrue(globalOpt.isPresent());

    final WasmGlobal global = globalOpt.get();

    // Set negative value
    global.set(WasmValue.f32(-3.14f));

    // Verify negative value
    final WasmValue value = global.get();
    assertEquals(-3.14f, value.asF32(), 0.01f, "Value should be approximately -3.14");
  }

  @Test
  @DisplayName("Zero values for all types")
  public void testZeroValues() {
    // Test I32 zero
    Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());
    globalOpt.get().set(WasmValue.i32(0));
    assertEquals(0, globalOpt.get().get().asI32());

    // Test I64 zero
    globalOpt = instance.getGlobal("g_i64");
    assertTrue(globalOpt.isPresent());
    globalOpt.get().set(WasmValue.i64(0L));
    assertEquals(0L, globalOpt.get().get().asI64());

    // Test F32 zero
    globalOpt = instance.getGlobal("g_f32");
    assertTrue(globalOpt.isPresent());
    globalOpt.get().set(WasmValue.f32(0.0f));
    assertEquals(0.0f, globalOpt.get().get().asF32(), 0.0001f);
  }

  @Test
  @DisplayName("Max values for integer types")
  public void testMaxIntegerValues() {
    // Test I32 max
    Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());
    globalOpt.get().set(WasmValue.i32(Integer.MAX_VALUE));
    assertEquals(Integer.MAX_VALUE, globalOpt.get().get().asI32());

    // Test I64 max
    globalOpt = instance.getGlobal("g_i64");
    assertTrue(globalOpt.isPresent());
    globalOpt.get().set(WasmValue.i64(Long.MAX_VALUE));
    assertEquals(Long.MAX_VALUE, globalOpt.get().get().asI64());
  }

  @Test
  @DisplayName("Min values for integer types")
  public void testMinIntegerValues() {
    // Test I32 min
    Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent());
    globalOpt.get().set(WasmValue.i32(Integer.MIN_VALUE));
    assertEquals(Integer.MIN_VALUE, globalOpt.get().get().asI32());

    // Test I64 min
    globalOpt = instance.getGlobal("g_i64");
    assertTrue(globalOpt.isPresent());
    globalOpt.get().set(WasmValue.i64(Long.MIN_VALUE));
    assertEquals(Long.MIN_VALUE, globalOpt.get().get().asI64());
  }

  /** Close safety tests for instance globals (PanamaInstanceGlobal). */
  @Nested
  @DisplayName("Closed Instance Global Detection Tests")
  class ClosedInstanceGlobalDetectionTests {

    private static final Logger LOGGER = Logger.getLogger("ClosedInstanceGlobalDetectionTests");

    @Test
    @DisplayName("get on closed instance global should throw IllegalStateException")
    void getOnClosedInstanceGlobalShouldThrow() {
      final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
      assertTrue(globalOpt.isPresent(), "Global g_i32 should be present");
      final PanamaInstanceGlobal global = (PanamaInstanceGlobal) globalOpt.get();

      global.close();
      LOGGER.info("Instance global closed, attempting get()");

      assertThrows(
          IllegalStateException.class,
          global::get,
          "get() on closed instance global should throw IllegalStateException");
      assertThrows(
          IllegalStateException.class,
          () -> global.set(WasmValue.i32(1)),
          "set() on closed instance global should throw IllegalStateException");
      LOGGER.info("IllegalStateException thrown as expected for get/set on closed instance global");
    }

    @Test
    @DisplayName("double close should be safe")
    void doubleCloseShouldBeSafe() {
      final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
      assertTrue(globalOpt.isPresent(), "Global g_i32 should be present");
      final PanamaInstanceGlobal global = (PanamaInstanceGlobal) globalOpt.get();

      global.close();
      LOGGER.info("First close completed");

      assertDoesNotThrow(global::close, "Second close should not throw");
      LOGGER.info("Second close completed without exception");

      assertThrows(
          IllegalStateException.class,
          global::get,
          "get() after double close should still throw IllegalStateException");
      LOGGER.info("IllegalStateException confirmed after double close");
    }
  }
}
