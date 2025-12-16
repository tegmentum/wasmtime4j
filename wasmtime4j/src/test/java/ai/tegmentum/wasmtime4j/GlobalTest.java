package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the WasmGlobal interface.
 *
 * <p>Tests verify global creation, value operations, mutability, and type handling for all
 * WebAssembly value types (i32, i64, f32, f64).
 */
@DisplayName("WasmGlobal Interface Tests")
class GlobalTest {

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
  @DisplayName("Global Creation Tests")
  class GlobalCreationTests {

    @Test
    @DisplayName("should create mutable i32 global")
    void shouldCreateMutableI32Global() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.I32, global.getType(), "Global type should be I32");
      assertTrue(global.isMutable(), "Global should be mutable");
    }

    @Test
    @DisplayName("should create immutable i32 global")
    void shouldCreateImmutableI32Global() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, false, WasmValue.i32(100));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.I32, global.getType(), "Global type should be I32");
      assertFalse(global.isMutable(), "Global should be immutable");
    }

    @Test
    @DisplayName("should create i64 global")
    void shouldCreateI64Global() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.I64, true, WasmValue.i64(9876543210L));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.I64, global.getType(), "Global type should be I64");
    }

    @Test
    @DisplayName("should create f32 global")
    void shouldCreateF32Global() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.F32, true, WasmValue.f32(3.14f));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.F32, global.getType(), "Global type should be F32");
    }

    @Test
    @DisplayName("should create f64 global")
    void shouldCreateF64Global() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.F64, true, WasmValue.f64(2.718281828));
      assertNotNull(global, "Global should not be null");
      assertEquals(WasmValueType.F64, global.getType(), "Global type should be F64");
    }
  }

  @Nested
  @DisplayName("I32 Global Value Tests")
  class I32GlobalValueTests {

    @Test
    @DisplayName("should get initial i32 value")
    void shouldGetInitialI32Value() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));
      final WasmValue value = global.get();
      assertNotNull(value, "Value should not be null");
      assertEquals(42, value.asI32(), "Initial value should be 42");
    }

    @Test
    @DisplayName("should set and get i32 value")
    void shouldSetAndGetI32Value() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      global.set(WasmValue.i32(100));
      assertEquals(100, global.get().asI32(), "Value should be updated to 100");
    }

    @Test
    @DisplayName("should handle maximum i32 value")
    void shouldHandleMaximumI32Value() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.I32, true, WasmValue.i32(Integer.MAX_VALUE));
      assertEquals(Integer.MAX_VALUE, global.get().asI32(), "Should handle MAX_VALUE");
    }

    @Test
    @DisplayName("should handle minimum i32 value")
    void shouldHandleMinimumI32Value() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.I32, true, WasmValue.i32(Integer.MIN_VALUE));
      assertEquals(Integer.MIN_VALUE, global.get().asI32(), "Should handle MIN_VALUE");
    }

    @Test
    @DisplayName("should handle negative i32 values")
    void shouldHandleNegativeI32Values() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(-12345));
      assertEquals(-12345, global.get().asI32(), "Should handle negative value");

      global.set(WasmValue.i32(-99999));
      assertEquals(-99999, global.get().asI32(), "Should update to negative value");
    }
  }

  @Nested
  @DisplayName("I64 Global Value Tests")
  class I64GlobalValueTests {

    @Test
    @DisplayName("should get initial i64 value")
    void shouldGetInitialI64Value() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.I64, true, WasmValue.i64(123456789012345L));
      assertEquals(123456789012345L, global.get().asI64(), "Initial value should match");
    }

    @Test
    @DisplayName("should set and get i64 value")
    void shouldSetAndGetI64Value() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I64, true, WasmValue.i64(0L));
      global.set(WasmValue.i64(999999999999L));
      assertEquals(999999999999L, global.get().asI64(), "Value should be updated");
    }

    @Test
    @DisplayName("should handle maximum i64 value")
    void shouldHandleMaximumI64Value() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.I64, true, WasmValue.i64(Long.MAX_VALUE));
      assertEquals(Long.MAX_VALUE, global.get().asI64(), "Should handle MAX_VALUE");
    }

    @Test
    @DisplayName("should handle minimum i64 value")
    void shouldHandleMinimumI64Value() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.I64, true, WasmValue.i64(Long.MIN_VALUE));
      assertEquals(Long.MIN_VALUE, global.get().asI64(), "Should handle MIN_VALUE");
    }
  }

  @Nested
  @DisplayName("F32 Global Value Tests")
  class F32GlobalValueTests {

    @Test
    @DisplayName("should get initial f32 value")
    void shouldGetInitialF32Value() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.F32, true, WasmValue.f32(3.14159f));
      assertEquals(3.14159f, global.get().asF32(), 0.00001f, "Initial value should match");
    }

    @Test
    @DisplayName("should set and get f32 value")
    void shouldSetAndGetF32Value() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.F32, true, WasmValue.f32(0.0f));
      global.set(WasmValue.f32(2.71828f));
      assertEquals(2.71828f, global.get().asF32(), 0.00001f, "Value should be updated");
    }

    @Test
    @DisplayName("should handle special f32 values")
    void shouldHandleSpecialF32Values() throws WasmException {
      // NaN
      final WasmGlobal nanGlobal =
          store.createGlobal(WasmValueType.F32, true, WasmValue.f32(Float.NaN));
      assertTrue(Float.isNaN(nanGlobal.get().asF32()), "Should preserve NaN");

      // Positive Infinity
      final WasmGlobal posInfGlobal =
          store.createGlobal(WasmValueType.F32, true, WasmValue.f32(Float.POSITIVE_INFINITY));
      assertEquals(
          Float.POSITIVE_INFINITY, posInfGlobal.get().asF32(), "Should preserve positive infinity");

      // Negative Infinity
      final WasmGlobal negInfGlobal =
          store.createGlobal(WasmValueType.F32, true, WasmValue.f32(Float.NEGATIVE_INFINITY));
      assertEquals(
          Float.NEGATIVE_INFINITY, negInfGlobal.get().asF32(), "Should preserve negative infinity");
    }

    @Test
    @DisplayName("should handle negative f32 values")
    void shouldHandleNegativeF32Values() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.F32, true, WasmValue.f32(-123.456f));
      assertEquals(-123.456f, global.get().asF32(), 0.001f, "Should handle negative value");
    }
  }

  @Nested
  @DisplayName("F64 Global Value Tests")
  class F64GlobalValueTests {

    @Test
    @DisplayName("should get initial f64 value")
    void shouldGetInitialF64Value() throws WasmException {
      final WasmGlobal global =
          store.createGlobal(WasmValueType.F64, true, WasmValue.f64(3.141592653589793));
      assertEquals(
          3.141592653589793, global.get().asF64(), 0.000000000001, "Initial value should match");
    }

    @Test
    @DisplayName("should set and get f64 value")
    void shouldSetAndGetF64Value() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.F64, true, WasmValue.f64(0.0));
      global.set(WasmValue.f64(2.718281828459045));
      assertEquals(
          2.718281828459045, global.get().asF64(), 0.000000000001, "Value should be updated");
    }

    @Test
    @DisplayName("should handle special f64 values")
    void shouldHandleSpecialF64Values() throws WasmException {
      // NaN
      final WasmGlobal nanGlobal =
          store.createGlobal(WasmValueType.F64, true, WasmValue.f64(Double.NaN));
      assertTrue(Double.isNaN(nanGlobal.get().asF64()), "Should preserve NaN");

      // Positive Infinity
      final WasmGlobal posInfGlobal =
          store.createGlobal(WasmValueType.F64, true, WasmValue.f64(Double.POSITIVE_INFINITY));
      assertEquals(
          Double.POSITIVE_INFINITY,
          posInfGlobal.get().asF64(),
          "Should preserve positive infinity");

      // Negative Infinity
      final WasmGlobal negInfGlobal =
          store.createGlobal(WasmValueType.F64, true, WasmValue.f64(Double.NEGATIVE_INFINITY));
      assertEquals(
          Double.NEGATIVE_INFINITY,
          negInfGlobal.get().asF64(),
          "Should preserve negative infinity");
    }
  }

  @Nested
  @DisplayName("Mutability Tests")
  class MutabilityTests {

    @Test
    @DisplayName("should allow setting mutable global")
    void shouldAllowSettingMutableGlobal() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      assertTrue(global.isMutable(), "Global should be mutable");
      // Should not throw
      global.set(WasmValue.i32(42));
      assertEquals(42, global.get().asI32(), "Value should be updated");
    }

    @Test
    @DisplayName("should report immutable status correctly")
    void shouldReportImmutableStatusCorrectly() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, false, WasmValue.i32(100));
      assertFalse(global.isMutable(), "Global should be immutable");
    }
  }

  @Nested
  @DisplayName("GlobalType Tests")
  class GlobalTypeTests {

    @Test
    @DisplayName("should return GlobalType for i32")
    void shouldReturnGlobalTypeForI32() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      final GlobalType globalType = global.getGlobalType();
      assertNotNull(globalType, "GlobalType should not be null");
      assertEquals(WasmValueType.I32, globalType.getValueType(), "Content type should be I32");
      assertTrue(globalType.isMutable(), "Should be mutable");
    }

    @Test
    @DisplayName("should return GlobalType for immutable f64")
    void shouldReturnGlobalTypeForImmutableF64() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.F64, false, WasmValue.f64(0.0));
      final GlobalType globalType = global.getGlobalType();
      assertNotNull(globalType, "GlobalType should not be null");
      assertEquals(WasmValueType.F64, globalType.getValueType(), "Content type should be F64");
      assertFalse(globalType.isMutable(), "Should be immutable");
    }
  }

  @Nested
  @DisplayName("getValue Alias Tests")
  class GetValueAliasTests {

    @Test
    @DisplayName("getValue should return same as get")
    void getValueShouldReturnSameAsGet() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));
      assertEquals(
          global.get().asI32(),
          global.getValue().asI32(),
          "getValue() should return same value as get()");
    }
  }

  @Nested
  @DisplayName("Multiple Global Operations Tests")
  class MultipleGlobalOperationsTests {

    @Test
    @DisplayName("should handle multiple globals independently")
    void shouldHandleMultipleGlobalsIndependently() throws WasmException {
      final WasmGlobal global1 = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(10));
      final WasmGlobal global2 = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(20));
      final WasmGlobal global3 = store.createGlobal(WasmValueType.I64, true, WasmValue.i64(30L));

      assertEquals(10, global1.get().asI32(), "Global1 should have value 10");
      assertEquals(20, global2.get().asI32(), "Global2 should have value 20");
      assertEquals(30L, global3.get().asI64(), "Global3 should have value 30");

      global1.set(WasmValue.i32(100));
      assertEquals(100, global1.get().asI32(), "Global1 should be updated to 100");
      assertEquals(20, global2.get().asI32(), "Global2 should remain 20");
      assertEquals(30L, global3.get().asI64(), "Global3 should remain 30");
    }

    @Test
    @DisplayName("should handle rapid set/get operations")
    void shouldHandleRapidSetGetOperations() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));

      for (int i = 0; i < 100; i++) {
        global.set(WasmValue.i32(i));
        assertEquals(i, global.get().asI32(), "Value should match after set at iteration " + i);
      }
    }
  }
}
