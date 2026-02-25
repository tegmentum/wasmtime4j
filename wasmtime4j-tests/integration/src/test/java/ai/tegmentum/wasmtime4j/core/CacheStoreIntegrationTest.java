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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.CacheStore;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for the {@link CacheStore} callback API with Wasmtime's incremental
 * compilation.
 *
 * <p>These tests verify that custom cache store implementations are properly invoked by Wasmtime
 * during module compilation across both JNI and Panama runtimes.
 *
 * @since 1.0.0
 */
@DisplayName("CacheStore Integration Tests")
public class CacheStoreIntegrationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(CacheStoreIntegrationTest.class.getName());

  /** Simple WAT module for compilation caching tests. */
  private static final String SIMPLE_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  /** Creates a tracking in-memory CacheStore that counts get/insert calls. */
  private static TrackingCacheStore createTrackingStore() {
    return new TrackingCacheStore();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Insert callback is invoked during compilation")
  void insertCallbackInvokedDuringCompilation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing insert callback invocation during compilation");

    final TrackingCacheStore store = createTrackingStore();
    final EngineConfig config = new EngineConfig().enableIncrementalCompilation(store);

    try (Engine engine = Engine.create(config)) {
      final Module module = engine.compileWat(SIMPLE_WAT);
      module.close();

      final int insertCalls = store.insertCount.get();
      LOGGER.info("[" + runtime + "] insert() called " + insertCalls + " times");
      assertTrue(insertCalls > 0, "insert() should be called at least once during compilation");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Get callback is invoked on second compilation")
  void getCallbackInvokedOnSecondCompilation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing get callback invocation on second compilation");

    final TrackingCacheStore store = createTrackingStore();
    final EngineConfig config = new EngineConfig().enableIncrementalCompilation(store);

    try (Engine engine = Engine.create(config)) {
      // First compilation populates the cache
      final Module module1 = engine.compileWat(SIMPLE_WAT);
      module1.close();

      final int insertAfterFirst = store.insertCount.get();
      LOGGER.info("[" + runtime + "] After first compile: insert=" + insertAfterFirst);
      assertTrue(insertAfterFirst > 0, "Cache should be populated after first compilation");

      // Second compilation should check the cache
      final int getBeforeSecond = store.getCount.get();
      final Module module2 = engine.compileWat(SIMPLE_WAT);
      module2.close();

      final int getCalls = store.getCount.get() - getBeforeSecond;
      LOGGER.info("[" + runtime + "] Second compile triggered " + getCalls + " get() calls");
      assertTrue(getCalls > 0, "get() should be called during second compilation");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Cached compilation produces working module")
  void cachedCompilationProducesWorkingModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing that cached compilation produces working module");

    final TrackingCacheStore store = createTrackingStore();
    final EngineConfig config = new EngineConfig().enableIncrementalCompilation(store);

    try (Engine engine = Engine.create(config)) {
      // First compile
      Module module1 = engine.compileWat(SIMPLE_WAT);

      // Verify first module works
      try (Store store1 = engine.createStore()) {
        final Instance instance1 = module1.instantiate(store1);
        final WasmValue[] result1 =
            instance1.callFunction("add", WasmValue.i32(3), WasmValue.i32(4));
        LOGGER.info("[" + runtime + "] First module: add(3,4) = " + result1[0].asInt());
        assertTrue(result1[0].asInt() == 7, "First module add(3,4) should return 7");
      }
      module1.close();

      // Second compile (should use cache)
      Module module2 = engine.compileWat(SIMPLE_WAT);

      // Verify second module works identically
      try (Store store2 = engine.createStore()) {
        final Instance instance2 = module2.instantiate(store2);
        final WasmValue[] result2 =
            instance2.callFunction("add", WasmValue.i32(10), WasmValue.i32(20));
        LOGGER.info("[" + runtime + "] Second module: add(10,20) = " + result2[0].asInt());
        assertTrue(result2[0].asInt() == 30, "Second module add(10,20) should return 30");
      }
      module2.close();

      LOGGER.info(
          "["
              + runtime
              + "] Total: "
              + store.insertCount.get()
              + " inserts, "
              + store.getCount.get()
              + " gets");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Cache store returning null from get works as cache miss")
  void cacheStoreReturningNullWorksAsCacheMiss(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cache store returning null from get");

    // A store that never caches (always returns null from get, discards inserts)
    final AtomicInteger getCalls = new AtomicInteger(0);
    final AtomicInteger insertCalls = new AtomicInteger(0);
    final CacheStore nullStore =
        new CacheStore() {
          @Override
          public byte[] get(final byte[] key) {
            getCalls.incrementAndGet();
            return null;
          }

          @Override
          public boolean insert(final byte[] key, final byte[] value) {
            insertCalls.incrementAndGet();
            return true;
          }
        };

    final EngineConfig config = new EngineConfig().enableIncrementalCompilation(nullStore);

    try (Engine engine = Engine.create(config)) {
      final Module module = engine.compileWat(SIMPLE_WAT);
      assertNotNull(module, "Module should compile successfully even with cache misses");

      // Verify module works
      try (Store store = engine.createStore()) {
        final Instance instance = module.instantiate(store);
        final WasmValue[] result = instance.callFunction("add", WasmValue.i32(5), WasmValue.i32(6));
        LOGGER.info(
            "[" + runtime + "] Module with null-returning cache: add(5,6) = " + result[0].asInt());
        assertTrue(result[0].asInt() == 11, "Module should work correctly with cache misses");
      }
      module.close();

      LOGGER.info(
          "["
              + runtime
              + "] Null store: "
              + getCalls.get()
              + " gets, "
              + insertCalls.get()
              + " inserts");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Cache store returning false from insert does not crash")
  void cacheStoreReturningFalseFromInsertDoesNotCrash(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cache store returning false from insert");

    final CacheStore failingStore =
        new CacheStore() {
          @Override
          public byte[] get(final byte[] key) {
            return null;
          }

          @Override
          public boolean insert(final byte[] key, final byte[] value) {
            return false; // Always fail to insert
          }
        };

    final EngineConfig config = new EngineConfig().enableIncrementalCompilation(failingStore);

    try (Engine engine = Engine.create(config)) {
      final Module module = engine.compileWat(SIMPLE_WAT);
      assertNotNull(module, "Module should compile even if cache insert fails");

      try (Store store = engine.createStore()) {
        final Instance instance = module.instantiate(store);
        final WasmValue[] result = instance.callFunction("add", WasmValue.i32(7), WasmValue.i32(8));
        LOGGER.info(
            "[" + runtime + "] Module with failing insert: add(7,8) = " + result[0].asInt());
        assertTrue(result[0].asInt() == 15, "Module should work correctly");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple engines with independent cache stores")
  void multipleEnginesWithIndependentCacheStores(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple engines with independent cache stores");

    final TrackingCacheStore store1 = createTrackingStore();
    final TrackingCacheStore store2 = createTrackingStore();

    final EngineConfig config1 = new EngineConfig().enableIncrementalCompilation(store1);
    final EngineConfig config2 = new EngineConfig().enableIncrementalCompilation(store2);

    try (Engine engine1 = Engine.create(config1);
        Engine engine2 = Engine.create(config2)) {
      // Compile in engine1 only
      final Module module1 = engine1.compileWat(SIMPLE_WAT);
      module1.close();

      final int store1Inserts = store1.insertCount.get();
      final int store2Inserts = store2.insertCount.get();

      LOGGER.info(
          "["
              + runtime
              + "] After compile in engine1: store1="
              + store1Inserts
              + " inserts, store2="
              + store2Inserts
              + " inserts");
      assertTrue(store1Inserts > 0, "Store1 should have inserts");
      assertTrue(store2Inserts == 0, "Store2 should have no inserts (different engine)");

      // Now compile in engine2 and verify store2 gets inserts
      final Module module2 = engine2.compileWat(SIMPLE_WAT);
      module2.close();

      final int store2InsertsAfter = store2.insertCount.get();
      LOGGER.info("[" + runtime + "] After compile in engine2: store2=" + store2InsertsAfter);
      assertTrue(store2InsertsAfter > 0, "Store2 should have inserts after engine2 compile");
    }
  }

  /**
   * A CacheStore implementation that tracks invocation counts for testing.
   *
   * <p>Uses a ConcurrentHashMap for thread-safe storage and AtomicInteger counters for tracking
   * callback invocations.
   */
  private static class TrackingCacheStore implements CacheStore {
    final ConcurrentHashMap<ByteBuffer, byte[]> cache = new ConcurrentHashMap<>();
    final AtomicInteger getCount = new AtomicInteger(0);
    final AtomicInteger insertCount = new AtomicInteger(0);

    @Override
    public byte[] get(final byte[] key) {
      getCount.incrementAndGet();
      return cache.get(ByteBuffer.wrap(key));
    }

    @Override
    public boolean insert(final byte[] key, final byte[] value) {
      insertCount.incrementAndGet();
      cache.put(ByteBuffer.wrap(key), value.clone());
      return true;
    }
  }
}
