/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

      assertNotNull(global);
      assertEquals(WasmValueType.I32, global.getType());
      assertTrue(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(WasmValueType.I32, value.getType());
      assertEquals(42, value.asInt());
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

      assertNotNull(global);
      assertEquals(WasmValueType.I64, global.getType());
      assertTrue(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(WasmValueType.I64, value.getType());
      assertEquals(123456789L, value.asLong());
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

      assertNotNull(global);
      assertEquals(WasmValueType.F32, global.getType());
      assertTrue(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(WasmValueType.F32, value.getType());
      assertEquals(3.14159f, value.asFloat(), 0.00001f);
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

      assertNotNull(global);
      assertEquals(WasmValueType.F64, global.getType());
      assertTrue(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(WasmValueType.F64, value.getType());
      assertEquals(2.71828, value.asDouble(), 0.00001);
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

      assertNotNull(global);
      assertEquals(WasmValueType.V128, global.getType());
      assertTrue(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(WasmValueType.V128, value.getType());
      assertArrayEquals(vectorData, value.asV128());
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

      assertNotNull(global);
      assertEquals(WasmValueType.I32, global.getType());
      assertFalse(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(100, value.asInt());

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
      assertEquals(100, global.get().asInt());

      // Set new value
      global.set(WasmValue.i32(200));
      assertEquals(200, global.get().asInt());

      // Set another value
      global.set(WasmValue.i32(-42));
      assertEquals(-42, global.get().asInt());
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

      assertNotNull(global);
      assertEquals(WasmValueType.I64, global.getType());
      assertFalse(global.isMutable());
      assertEquals(999L, global.get().asLong());
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

      assertNotNull(global);
      assertEquals(WasmValueType.F32, global.getType());
      assertTrue(global.isMutable());
      assertEquals(1.23f, global.get().asFloat(), 0.001f);

      // Test mutation
      global.set(WasmValue.f32(4.56f));
      assertEquals(4.56f, global.get().asFloat(), 0.001f);
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
      // ExnRef and ContRef types are not yet supported as global types in the native layer
      final java.util.Set<WasmValueType> unsupportedGlobalTypes =
          java.util.EnumSet.of(
              WasmValueType.EXNREF,
              WasmValueType.NULLEXNREF,
              WasmValueType.CONTREF,
              WasmValueType.NULLCONTREF);
      for (final WasmValueType valueType : WasmValueType.values()) {
        if (unsupportedGlobalTypes.contains(valueType)) {
          LOGGER.fine("[" + runtime + "] Skipping unsupported global type: " + valueType);
          continue;
        }
        LOGGER.fine("[" + runtime + "] Testing value type: " + valueType);
        final WasmValue initialValue = createDefaultValue(valueType);

        assertDoesNotThrow(
            () -> {
              final WasmGlobal global = store.createGlobal(valueType, true, initialValue);
              assertNotNull(global);
              assertEquals(valueType, global.getType());
              assertTrue(global.isMutable());

              final WasmValue retrievedValue = global.get();
              if (valueType.isGcReference()) {
                // GC reference types: null values may be returned as ANYREF/FUNCREF/EXTERNREF
                // depending on the runtime representation, so verify the value is null
                assertNull(retrievedValue.getValue());
              } else {
                assertEquals(valueType, retrievedValue.getType());
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

      assertNotNull(global);
      assertEquals(WasmValueType.FUNCREF, global.getType());
      assertTrue(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(WasmValueType.FUNCREF, value.getType());
      assertNull(value.asFuncref());
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

      assertNotNull(global);
      assertEquals(WasmValueType.EXTERNREF, global.getType());
      assertTrue(global.isMutable());

      final WasmValue value = global.get();
      assertEquals(WasmValueType.EXTERNREF, value.getType());
      assertNull(value.asExternref());
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
      assertEquals(expectedValue, global.get().asInt());
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
        assertEquals(i, value);
      }

      final long endTime = System.nanoTime();
      final long durationMs = (endTime - startTime) / 1_000_000;

      // Performance assertion - should complete in reasonable time
      assertTrue(durationMs < 5000, "Expected < 5000ms, got " + durationMs); // 5 seconds max

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
      case EXNREF:
        return WasmValue.nullExnRef();
      case NULLEXNREF:
        return WasmValue.nullNullExnRef();
      case CONTREF:
        return WasmValue.nullContRef();
      case NULLCONTREF:
        return WasmValue.nullNullContRef();
      default:
        throw new IllegalArgumentException("Unsupported value type: " + valueType);
    }
  }

  private static void assertValuesEqual(final WasmValue expected, final WasmValue actual) {
    assertEquals(expected.getType(), actual.getType());

    switch (expected.getType()) {
      case I32:
        assertEquals(expected.asInt(), actual.asInt());
        break;
      case I64:
        assertEquals(expected.asLong(), actual.asLong());
        break;
      case F32:
        assertEquals(expected.asFloat(), actual.asFloat(), 0.001f);
        break;
      case F64:
        assertEquals(expected.asDouble(), actual.asDouble(), 0.001);
        break;
      case V128:
        assertArrayEquals(expected.asV128(), actual.asV128());
        break;
      case FUNCREF:
        assertEquals(expected.asFuncref(), actual.asFuncref());
        break;
      case EXTERNREF:
        assertEquals(expected.asExternref(), actual.asExternref());
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
        assertNull(actual.getValue());
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + expected.getType());
    }
  }
}
