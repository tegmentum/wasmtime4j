package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive integration tests for WebAssembly global variable creation and management.
 *
 * <p>This test suite validates the complete global variable lifecycle including creation, access,
 * mutation, and type safety across both JNI and Panama implementations.
 */
class GlobalCreationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(GlobalCreationTest.class.getName());

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  private Engine createTestEngine() throws WasmException {
    final EngineConfig config =
        Engine.builder().wasmGc(true).gcSupport(true).wasmFunctionReferences(true);
    return Engine.create(config);
  }

  // Test creation of all basic numeric types

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateI32Global(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing I32 global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.i32(42);
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.I32);
      assertThat(global.isMutable()).isTrue();

      final WasmValue value = global.get();
      assertThat(value.getType()).isEqualTo(WasmValueType.I32);
      assertThat(value.asInt()).isEqualTo(42);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateI64Global(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing I64 global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.i64(123456789L);
      final WasmGlobal global = store.createGlobal(WasmValueType.I64, true, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.I64);
      assertThat(global.isMutable()).isTrue();

      final WasmValue value = global.get();
      assertThat(value.getType()).isEqualTo(WasmValueType.I64);
      assertThat(value.asLong()).isEqualTo(123456789L);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateF32Global(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing F32 global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.f32(3.14159f);
      final WasmGlobal global = store.createGlobal(WasmValueType.F32, true, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.F32);
      assertThat(global.isMutable()).isTrue();

      final WasmValue value = global.get();
      assertThat(value.getType()).isEqualTo(WasmValueType.F32);
      assertThat(value.asFloat()).isEqualTo(3.14159f, offset(0.00001f));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateF64Global(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing F64 global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.f64(2.71828);
      final WasmGlobal global = store.createGlobal(WasmValueType.F64, true, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.F64);
      assertThat(global.isMutable()).isTrue();

      final WasmValue value = global.get();
      assertThat(value.getType()).isEqualTo(WasmValueType.F64);
      assertThat(value.asDouble()).isEqualTo(2.71828, offset(0.00001));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateV128Global(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
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
  }

  // Test mutability

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateImmutableGlobal(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing immutable global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.i32(100);
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, false, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.I32);
      assertThat(global.isMutable()).isFalse();

      final WasmValue value = global.get();
      assertThat(value.asInt()).isEqualTo(100);

      // Attempting to set should throw
      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            global.set(WasmValue.i32(200));
          });
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testMutableGlobalSetAndGet(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing mutable global set and get");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.i32(100);
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, initialValue);

      // Initial value
      assertThat(global.get().asInt()).isEqualTo(100);

      // Set new value
      global.set(WasmValue.i32(200));
      assertThat(global.get().asInt()).isEqualTo(200);

      // Set another value
      global.set(WasmValue.i32(-42));
      assertThat(global.get().asInt()).isEqualTo(-42);
    }
  }

  // Test convenience methods

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateImmutableGlobalConvenience(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing immutable global convenience method");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.i64(999L);
      final WasmGlobal global = store.createImmutableGlobal(WasmValueType.I64, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.I64);
      assertThat(global.isMutable()).isFalse();
      assertThat(global.get().asLong()).isEqualTo(999L);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateMutableGlobalConvenience(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing mutable global convenience method");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.f32(1.23f);
      final WasmGlobal global = store.createMutableGlobal(WasmValueType.F32, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.F32);
      assertThat(global.isMutable()).isTrue();
      assertThat(global.get().asFloat()).isEqualTo(1.23f, offset(0.001f));

      // Test mutation
      global.set(WasmValue.f32(4.56f));
      assertThat(global.get().asFloat()).isEqualTo(4.56f, offset(0.001f));
    }
  }

  // Test all value types - iterates over WasmValueType internally

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateGlobalAllTypes(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing global creation for all value types");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      for (final WasmValueType valueType : WasmValueType.values()) {
        LOGGER.fine("[" + runtime + "] Testing value type: " + valueType);
        final WasmValue initialValue = createDefaultValue(valueType);

        assertDoesNotThrow(
            () -> {
              final WasmGlobal global = store.createGlobal(valueType, true, initialValue);
              assertThat(global).isNotNull();
              assertThat(global.getType()).isEqualTo(valueType);
              assertThat(global.isMutable()).isTrue();

              final WasmValue retrievedValue = global.get();
              if (valueType.isGcReference()) {
                // GC reference types: null values may be returned as ANYREF/FUNCREF/EXTERNREF
                // depending on the runtime representation, so verify the value is null
                assertThat(retrievedValue.getValue()).isNull();
              } else {
                assertThat(retrievedValue.getType()).isEqualTo(valueType);
                assertValuesEqual(initialValue, retrievedValue);
              }
            });
      }
    }
  }

  // Test error conditions

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateGlobalWithNullValueType(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing global creation with null value type");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.i32(42);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            store.createGlobal(null, true, initialValue);
          });
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateGlobalWithNullInitialValue(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing global creation with null initial value");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            store.createGlobal(WasmValueType.I32, true, null);
          });
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateGlobalWithTypeMismatch(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing global creation with type mismatch");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.i32(42); // I32 value

      // Try to create F32 global with I32 value
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            store.createGlobal(WasmValueType.F32, true, initialValue);
          });
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testSetGlobalWithTypeMismatch(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing global set with type mismatch");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));

      // Try to set F32 value on I32 global
      assertThrows(
          Exception.class,
          () -> {
            global.set(WasmValue.f32(3.14f));
          });
    }
  }

  // Test reference types

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateFuncrefGlobal(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing funcref global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.funcref(null); // Null function reference
      final WasmGlobal global = store.createGlobal(WasmValueType.FUNCREF, true, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.FUNCREF);
      assertThat(global.isMutable()).isTrue();

      final WasmValue value = global.get();
      assertThat(value.getType()).isEqualTo(WasmValueType.FUNCREF);
      assertThat(value.asFuncref()).isNull();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateExternrefGlobal(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing externref global creation");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmValue initialValue = WasmValue.externref(null); // Null external reference
      final WasmGlobal global = store.createGlobal(WasmValueType.EXTERNREF, true, initialValue);

      assertThat(global).isNotNull();
      assertThat(global.getType()).isEqualTo(WasmValueType.EXTERNREF);
      assertThat(global.isMutable()).isTrue();

      final WasmValue value = global.get();
      assertThat(value.getType()).isEqualTo(WasmValueType.EXTERNREF);
      assertThat(value.asExternref()).isNull();
    }
  }

  // Test thread safety for mutable globals

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testConcurrentAccessToMutableGlobal(final RuntimeType runtime)
      throws WasmException, InterruptedException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing concurrent access to mutable global");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
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
                      final int currentValue = global.get().asInt();
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
      assertThat(global.get().asInt()).isEqualTo(expectedValue);
    }
  }

  // Test performance characteristics

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testGlobalAccessPerformance(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing global access performance");
    try (Engine engine = createTestEngine();
        Store store = Store.create(engine)) {
      final WasmGlobal global = store.createGlobal(WasmValueType.I64, true, WasmValue.i64(0L));

      final int iterations = 100_000;
      final long startTime = System.nanoTime();

      for (int i = 0; i < iterations; i++) {
        global.set(WasmValue.i64(i));
        final long value = global.get().asLong();
        assertThat(value).isEqualTo(i);
      }

      final long endTime = System.nanoTime();
      final long durationMs = (endTime - startTime) / 1_000_000;

      // Performance assertion - should complete in reasonable time
      assertThat(durationMs).isLessThan(5000); // 5 seconds max for 100k operations

      LOGGER.info(
          String.format(
              "[%s] Global access performance: %d operations in %d ms (%.2f ops/ms)",
              runtime, iterations, durationMs, (double) iterations / durationMs));
    }
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
      case ANYREF:
        return WasmValue.nullAnyRef();
      case EQREF:
        return WasmValue.nullEqRef();
      case I31REF:
        return WasmValue.nullI31Ref();
      case STRUCTREF:
        return WasmValue.nullStructRef();
      case ARRAYREF:
        return WasmValue.nullArrayRef();
      case NULLREF:
        return WasmValue.nullRef();
      case NULLFUNCREF:
        return WasmValue.nullNullFuncRef();
      case NULLEXTERNREF:
        return WasmValue.nullNullExternRef();
      default:
        throw new IllegalArgumentException("Unsupported value type: " + valueType);
    }
  }

  private static void assertValuesEqual(final WasmValue expected, final WasmValue actual) {
    assertThat(actual.getType()).isEqualTo(expected.getType());

    switch (expected.getType()) {
      case I32:
        assertThat(actual.asInt()).isEqualTo(expected.asInt());
        break;
      case I64:
        assertThat(actual.asLong()).isEqualTo(expected.asLong());
        break;
      case F32:
        assertThat(actual.asFloat()).isEqualTo(expected.asFloat(), offset(0.001f));
        break;
      case F64:
        assertThat(actual.asDouble()).isEqualTo(expected.asDouble(), offset(0.001));
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
      case ANYREF:
      case EQREF:
      case I31REF:
      case STRUCTREF:
      case ARRAYREF:
      case NULLREF:
      case NULLFUNCREF:
      case NULLEXTERNREF:
        // GC reference types: verify the value is null
        assertThat(actual.getValue()).isNull();
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + expected.getType());
    }
  }

  // Required for AssertJ offset() method
  private static org.assertj.core.data.Offset<Float> offset(final float value) {
    return org.assertj.core.data.Offset.offset(value);
  }

  private static org.assertj.core.data.Offset<Double> offset(final double value) {
    return org.assertj.core.data.Offset.offset(value);
  }
}
