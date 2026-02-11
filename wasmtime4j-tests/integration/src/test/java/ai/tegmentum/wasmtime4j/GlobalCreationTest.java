package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive integration tests for WebAssembly global variable creation and management.
 *
 * <p>This test suite validates the complete global variable lifecycle including creation, access,
 * mutation, and type safety across both JNI and Panama implementations.
 */
class GlobalCreationTest {

  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = Store.create(engine);
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  // Test creation of all basic numeric types

  @Test
  void testCreateI32Global() throws WasmException {
    final WasmValue initialValue = WasmValue.i32(42);
    final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.I32);
    assertThat(global.isMutable()).isTrue();

    final WasmValue value = global.get();
    assertThat(value.getType()).isEqualTo(WasmValueType.I32);
    assertThat(value.asI32()).isEqualTo(42);
  }

  @Test
  void testCreateI64Global() throws WasmException {
    final WasmValue initialValue = WasmValue.i64(123456789L);
    final WasmGlobal global = store.createGlobal(WasmValueType.I64, true, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.I64);
    assertThat(global.isMutable()).isTrue();

    final WasmValue value = global.get();
    assertThat(value.getType()).isEqualTo(WasmValueType.I64);
    assertThat(value.asI64()).isEqualTo(123456789L);
  }

  @Test
  void testCreateF32Global() throws WasmException {
    final WasmValue initialValue = WasmValue.f32(3.14159f);
    final WasmGlobal global = store.createGlobal(WasmValueType.F32, true, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.F32);
    assertThat(global.isMutable()).isTrue();

    final WasmValue value = global.get();
    assertThat(value.getType()).isEqualTo(WasmValueType.F32);
    assertThat(value.asF32()).isEqualTo(3.14159f, offset(0.00001f));
  }

  @Test
  void testCreateF64Global() throws WasmException {
    final WasmValue initialValue = WasmValue.f64(2.71828);
    final WasmGlobal global = store.createGlobal(WasmValueType.F64, true, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.F64);
    assertThat(global.isMutable()).isTrue();

    final WasmValue value = global.get();
    assertThat(value.getType()).isEqualTo(WasmValueType.F64);
    assertThat(value.asF64()).isEqualTo(2.71828, offset(0.00001));
  }

  @Test
  void testCreateV128Global() throws WasmException {
    final byte[] vectorData = new byte[16];
    for (int i = 0; i < 16; i++) {
      vectorData[i] = (byte) (i * 17); // Test pattern
    }
    final WasmValue initialValue = WasmValue.v128(vectorData);
    final WasmGlobal global = store.createGlobal(WasmValueType.V128, true, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.V128);
    assertThat(global.isMutable()).isTrue();

    final WasmValue value = global.get();
    assertThat(value.getType()).isEqualTo(WasmValueType.V128);
    assertThat(value.asV128()).isEqualTo(vectorData);
  }

  // Test mutability

  @Test
  void testCreateImmutableGlobal() throws WasmException {
    final WasmValue initialValue = WasmValue.i32(100);
    final WasmGlobal global = store.createGlobal(WasmValueType.I32, false, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.I32);
    assertThat(global.isMutable()).isFalse();

    final WasmValue value = global.get();
    assertThat(value.asI32()).isEqualTo(100);

    // Attempting to set should throw
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          global.set(WasmValue.i32(200));
        });
  }

  @Test
  void testMutableGlobalSetAndGet() throws WasmException {
    final WasmValue initialValue = WasmValue.i32(100);
    final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, initialValue);

    // Initial value
    assertThat(global.get().asI32()).isEqualTo(100);

    // Set new value
    global.set(WasmValue.i32(200));
    assertThat(global.get().asI32()).isEqualTo(200);

    // Set another value
    global.set(WasmValue.i32(-42));
    assertThat(global.get().asI32()).isEqualTo(-42);
  }

  // Test convenience methods

  @Test
  void testCreateImmutableGlobalConvenience() throws WasmException {
    final WasmValue initialValue = WasmValue.i64(999L);
    final WasmGlobal global = store.createImmutableGlobal(WasmValueType.I64, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.I64);
    assertThat(global.isMutable()).isFalse();
    assertThat(global.get().asI64()).isEqualTo(999L);
  }

  @Test
  void testCreateMutableGlobalConvenience() throws WasmException {
    final WasmValue initialValue = WasmValue.f32(1.23f);
    final WasmGlobal global = store.createMutableGlobal(WasmValueType.F32, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.F32);
    assertThat(global.isMutable()).isTrue();
    assertThat(global.get().asF32()).isEqualTo(1.23f, offset(0.001f));

    // Test mutation
    global.set(WasmValue.f32(4.56f));
    assertThat(global.get().asF32()).isEqualTo(4.56f, offset(0.001f));
  }

  // Test all value types using parameterized test

  @ParameterizedTest
  @EnumSource(WasmValueType.class)
  void testCreateGlobalAllTypes(final WasmValueType valueType) throws WasmException {
    // Skip WasmGC reference types - they require struct/array type definitions
    // which cannot be created standalone via store.createGlobal()
    Assumptions.assumeFalse(
        valueType.isGcReference(),
        "WasmGC reference types require type definitions and are tested separately");

    final WasmValue initialValue = createDefaultValue(valueType);

    assertDoesNotThrow(
        () -> {
          final WasmGlobal global = store.createGlobal(valueType, true, initialValue);
          assertThat(global).isNotNull();
          assertThat(global.getType()).isEqualTo(valueType);
          assertThat(global.isMutable()).isTrue();

          final WasmValue retrievedValue = global.get();
          assertThat(retrievedValue.getType()).isEqualTo(valueType);
          assertValuesEqual(initialValue, retrievedValue);
        });
  }

  // Test error conditions

  @Test
  void testCreateGlobalWithNullValueType() {
    final WasmValue initialValue = WasmValue.i32(42);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          store.createGlobal(null, true, initialValue);
        });
  }

  @Test
  void testCreateGlobalWithNullInitialValue() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          store.createGlobal(WasmValueType.I32, true, null);
        });
  }

  @Test
  void testCreateGlobalWithTypeMismatch() {
    final WasmValue initialValue = WasmValue.i32(42); // I32 value

    // Try to create F32 global with I32 value
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          store.createGlobal(WasmValueType.F32, true, initialValue);
        });
  }

  @Test
  void testSetGlobalWithTypeMismatch() throws WasmException {
    final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));

    // Try to set F32 value on I32 global
    assertThrows(
        Exception.class,
        () -> {
          global.set(WasmValue.f32(3.14f));
        });
  }

  // Test reference types

  @Test
  void testCreateFuncrefGlobal() throws WasmException {
    final WasmValue initialValue = WasmValue.funcref(null); // Null function reference
    final WasmGlobal global = store.createGlobal(WasmValueType.FUNCREF, true, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.FUNCREF);
    assertThat(global.isMutable()).isTrue();

    final WasmValue value = global.get();
    assertThat(value.getType()).isEqualTo(WasmValueType.FUNCREF);
    assertThat(value.asFuncref()).isNull();
  }

  @Test
  void testCreateExternrefGlobal() throws WasmException {
    final WasmValue initialValue = WasmValue.externref(null); // Null external reference
    final WasmGlobal global = store.createGlobal(WasmValueType.EXTERNREF, true, initialValue);

    assertThat(global).isNotNull();
    assertThat(global.getType()).isEqualTo(WasmValueType.EXTERNREF);
    assertThat(global.isMutable()).isTrue();

    final WasmValue value = global.get();
    assertThat(value.getType()).isEqualTo(WasmValueType.EXTERNREF);
    assertThat(value.asExternref()).isNull();
  }

  // Test thread safety for mutable globals

  @Test
  void testConcurrentAccessToMutableGlobal() throws WasmException, InterruptedException {
    final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));

    final int numThreads = 10;
    final int incrementsPerThread = 100;
    final Thread[] threads = new Thread[numThreads];

    for (int i = 0; i < numThreads; i++) {
      threads[i] =
          new Thread(
              () -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                  synchronized (global) {
                    final int currentValue = global.get().asI32();
                    global.set(WasmValue.i32(currentValue + 1));
                  }
                }
              });
    }

    // Start all threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify final value
    final int expectedValue = numThreads * incrementsPerThread;
    assertThat(global.get().asI32()).isEqualTo(expectedValue);
  }

  // Test performance characteristics

  @Test
  void testGlobalAccessPerformance() throws WasmException {
    final WasmGlobal global = store.createGlobal(WasmValueType.I64, true, WasmValue.i64(0L));

    final int iterations = 100_000;
    final long startTime = System.nanoTime();

    for (int i = 0; i < iterations; i++) {
      global.set(WasmValue.i64(i));
      final long value = global.get().asI64();
      assertThat(value).isEqualTo(i);
    }

    final long endTime = System.nanoTime();
    final long durationMs = (endTime - startTime) / 1_000_000;

    // Performance assertion - should complete in reasonable time
    assertThat(durationMs).isLessThan(5000); // 5 seconds max for 100k operations

    System.out.printf(
        "Global access performance: %d operations in %d ms (%.2f ops/ms)%n",
        iterations, durationMs, (double) iterations / durationMs);
  }

  // Helper methods

  private static WasmValue createDefaultValue(final WasmValueType valueType) {
    switch (valueType) {
      case I32:
        return WasmValue.i32(42);
      case I64:
        return WasmValue.i64(123456789L);
      case F32:
        return WasmValue.f32(3.14f);
      case F64:
        return WasmValue.f64(2.718);
      case V128:
        final byte[] vectorData = new byte[16];
        for (int i = 0; i < 16; i++) {
          vectorData[i] = (byte) i;
        }
        return WasmValue.v128(vectorData);
      case FUNCREF:
        return WasmValue.funcref(null);
      case EXTERNREF:
        return WasmValue.externref(null);
      default:
        throw new IllegalArgumentException("Unsupported value type: " + valueType);
    }
  }

  private static void assertValuesEqual(final WasmValue expected, final WasmValue actual) {
    assertThat(actual.getType()).isEqualTo(expected.getType());

    switch (expected.getType()) {
      case I32:
        assertThat(actual.asI32()).isEqualTo(expected.asI32());
        break;
      case I64:
        assertThat(actual.asI64()).isEqualTo(expected.asI64());
        break;
      case F32:
        assertThat(actual.asF32()).isEqualTo(expected.asF32(), offset(0.001f));
        break;
      case F64:
        assertThat(actual.asF64()).isEqualTo(expected.asF64(), offset(0.001));
        break;
      case V128:
        assertThat(actual.asV128()).isEqualTo(expected.asV128());
        break;
      case FUNCREF:
        assertThat(actual.asFuncref()).isEqualTo(expected.asFuncref());
        break;
      case EXTERNREF:
        assertThat(actual.asExternref()).isEqualTo(expected.asExternref());
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + expected.getType());
    }
  }

  // Required for AssertJ offset() method
  private static org.assertj.core.data.Offset<Float> offset(float value) {
    return org.assertj.core.data.Offset.offset(value);
  }

  private static org.assertj.core.data.Offset<Double> offset(double value) {
    return org.assertj.core.data.Offset.offset(value);
  }
}
