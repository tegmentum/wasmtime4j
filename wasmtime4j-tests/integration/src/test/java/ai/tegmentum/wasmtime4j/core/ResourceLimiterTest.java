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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.ResourceLimiter;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for the dynamic {@link ResourceLimiter} callback API.
 *
 * <p>These tests verify that the {@link Store#setResourceLimiter(ResourceLimiter)} method works
 * correctly across both JNI and Panama runtimes, ensuring that the limiter callbacks are invoked
 * with correct parameters and that allow/deny decisions are respected by the Wasmtime engine.
 *
 * @since 1.0.0
 */
@DisplayName("Resource Limiter Integration Tests")
public class ResourceLimiterTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ResourceLimiterTest.class.getName());

  /** WAT module with growable memory (1 page initial, 10 pages max). */
  private static final String MEMORY_GROW_WAT =
      """
      (module
        (memory (export "mem") 1 10)
        (func (export "grow") (param i32) (result i32)
          local.get 0
          memory.grow)
        (func (export "size") (result i32)
          memory.size))
      """;

  /** WAT module with growable table (2 elements initial, 20 elements max). */
  private static final String TABLE_GROW_WAT =
      """
      (module
        (table (export "t") 2 20 funcref)
        (func (export "grow") (param i32) (result i32)
          ref.null func
          local.get 0
          table.grow 0)
        (func (export "size") (result i32)
          table.size 0))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory growth allowed by limiter succeeds")
  void memoryGrowthAllowedByLimiter(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory growth allowed by limiter");

    final AtomicInteger callCount = new AtomicInteger(0);

    final ResourceLimiter limiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            callCount.incrementAndGet();
            LOGGER.info(
                "["
                    + runtime
                    + "] memoryGrowing called: current="
                    + currentBytes
                    + ", desired="
                    + desiredBytes
                    + ", maximum="
                    + maximumBytes);
            return true; // Always allow
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            return true;
          }
        };

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      store.setResourceLimiter(limiter);

      final Module module = engine.compileWat(MEMORY_GROW_WAT);
      final Instance instance = module.instantiate(store);

      assertEquals(1, instance.callFunction("size")[0].asInt(), "Initial memory size should be 1");

      // Grow by 2 pages - should succeed since limiter allows
      final int prevSize = instance.callFunction("grow", WasmValue.i32(2))[0].asInt();
      assertEquals(1, prevSize, "Previous size should be 1");
      assertEquals(3, instance.callFunction("size")[0].asInt(), "Memory should have grown to 3");

      assertTrue(callCount.get() > 0, "Limiter should have been called at least once");
      LOGGER.info("[" + runtime + "] Limiter was called " + callCount.get() + " time(s)");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory growth denied by limiter fails")
  void memoryGrowthDeniedByLimiter(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory growth denied by limiter");

    final AtomicBoolean denyMode = new AtomicBoolean(false);
    final AtomicBoolean denied = new AtomicBoolean(false);

    final ResourceLimiter limiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            if (!denyMode.get()) {
              LOGGER.info(
                  "["
                      + runtime
                      + "] memoryGrowing called during init: current="
                      + currentBytes
                      + ", desired="
                      + desiredBytes
                      + " -> ALLOW");
              return true; // Allow during instantiation
            }
            LOGGER.info(
                "["
                    + runtime
                    + "] memoryGrowing called: current="
                    + currentBytes
                    + ", desired="
                    + desiredBytes
                    + " -> DENY");
            denied.set(true);
            return false; // Deny after instantiation
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            return true;
          }
        };

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      store.setResourceLimiter(limiter);

      final Module module = engine.compileWat(MEMORY_GROW_WAT);
      final Instance instance = module.instantiate(store);

      // Switch to deny mode after instantiation
      denyMode.set(true);

      assertEquals(1, instance.callFunction("size")[0].asInt(), "Initial memory size should be 1");

      // Grow should fail because limiter denies it
      final int result = instance.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(-1, result, "Growth should return -1 when denied by limiter");
      assertEquals(
          1,
          instance.callFunction("size")[0].asInt(),
          "Memory size should remain 1 after denied growth");

      assertTrue(denied.get(), "Limiter memoryGrowing should have been called");
      LOGGER.info("[" + runtime + "] Memory growth correctly denied by limiter");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Table growth allowed by limiter succeeds")
  void tableGrowthAllowedByLimiter(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table growth allowed by limiter");

    final AtomicInteger callCount = new AtomicInteger(0);

    final ResourceLimiter limiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            return true;
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            callCount.incrementAndGet();
            LOGGER.info(
                "["
                    + runtime
                    + "] tableGrowing called: current="
                    + currentElements
                    + ", desired="
                    + desiredElements
                    + ", maximum="
                    + maximumElements);
            return true; // Allow
          }
        };

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      store.setResourceLimiter(limiter);

      final Module module = engine.compileWat(TABLE_GROW_WAT);
      final Instance instance = module.instantiate(store);

      assertEquals(2, instance.callFunction("size")[0].asInt(), "Initial table size should be 2");

      // Grow by 3 elements
      final int prevSize = instance.callFunction("grow", WasmValue.i32(3))[0].asInt();
      assertEquals(2, prevSize, "Previous table size should be 2");
      assertEquals(5, instance.callFunction("size")[0].asInt(), "Table should have grown to 5");

      assertTrue(callCount.get() > 0, "Limiter tableGrowing should have been called");
      LOGGER.info("[" + runtime + "] Table growth allowed, called " + callCount.get() + " time(s)");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Table growth denied by limiter fails")
  void tableGrowthDeniedByLimiter(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table growth denied by limiter");

    final AtomicBoolean denyMode = new AtomicBoolean(false);

    final ResourceLimiter limiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            return true;
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            if (!denyMode.get()) {
              LOGGER.info(
                  "["
                      + runtime
                      + "] tableGrowing called during init: current="
                      + currentElements
                      + ", desired="
                      + desiredElements
                      + " -> ALLOW");
              return true; // Allow during instantiation
            }
            LOGGER.info(
                "["
                    + runtime
                    + "] tableGrowing called: current="
                    + currentElements
                    + ", desired="
                    + desiredElements
                    + " -> DENY");
            return false; // Deny after instantiation
          }
        };

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      store.setResourceLimiter(limiter);

      final Module module = engine.compileWat(TABLE_GROW_WAT);
      final Instance instance = module.instantiate(store);

      // Switch to deny mode after instantiation
      denyMode.set(true);

      assertEquals(2, instance.callFunction("size")[0].asInt(), "Initial table size should be 2");

      // Grow should fail
      final int result = instance.callFunction("grow", WasmValue.i32(2))[0].asInt();
      assertEquals(-1, result, "Table growth should return -1 when denied by limiter");
      assertEquals(
          2,
          instance.callFunction("size")[0].asInt(),
          "Table size should remain 2 after denied growth");

      LOGGER.info("[" + runtime + "] Table growth correctly denied by limiter");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Limiter receives correct current and desired byte values for memory")
  void limiterReceivesCorrectMemoryValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing limiter receives correct memory values");

    final long pageSize = 65536L; // WebAssembly page size
    final List<long[]> receivedValues = Collections.synchronizedList(new ArrayList<>());

    final ResourceLimiter limiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            receivedValues.add(new long[] {currentBytes, desiredBytes, maximumBytes});
            LOGGER.info(
                "["
                    + runtime
                    + "] memoryGrowing: current="
                    + currentBytes
                    + ", desired="
                    + desiredBytes
                    + ", maximum="
                    + maximumBytes);
            return true;
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            return true;
          }
        };

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      store.setResourceLimiter(limiter);

      final Module module = engine.compileWat(MEMORY_GROW_WAT);
      final Instance instance = module.instantiate(store);

      // Grow by 1 page (from 1 page to 2 pages)
      instance.callFunction("grow", WasmValue.i32(1));

      assertFalse(receivedValues.isEmpty(), "Limiter should have been called");

      // Find the callback for this specific grow operation
      final long[] lastValues = receivedValues.get(receivedValues.size() - 1);
      assertEquals(
          1 * pageSize, lastValues[0], "Current bytes should be 1 page (" + pageSize + " bytes)");
      assertEquals(
          2 * pageSize,
          lastValues[1],
          "Desired bytes should be 2 pages (" + (2 * pageSize) + " bytes)");

      LOGGER.info("[" + runtime + "] Limiter received correct byte values");

      // Grow by 2 more pages (from 2 to 4)
      receivedValues.clear();
      instance.callFunction("grow", WasmValue.i32(2));

      assertFalse(receivedValues.isEmpty(), "Limiter should have been called for second grow");
      final long[] secondValues = receivedValues.get(receivedValues.size() - 1);
      assertEquals(
          2 * pageSize, secondValues[0], "Current bytes should be 2 pages after first grow");
      assertEquals(
          4 * pageSize, secondValues[1], "Desired bytes should be 4 pages for second grow");

      LOGGER.info("[" + runtime + "] Second grow also received correct values");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Limiter receives correct table element values")
  void limiterReceivesCorrectTableValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing limiter receives correct table values");

    final List<int[]> receivedValues = Collections.synchronizedList(new ArrayList<>());

    final ResourceLimiter limiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            return true;
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            receivedValues.add(new int[] {currentElements, desiredElements, maximumElements});
            LOGGER.info(
                "["
                    + runtime
                    + "] tableGrowing: current="
                    + currentElements
                    + ", desired="
                    + desiredElements
                    + ", maximum="
                    + maximumElements);
            return true;
          }
        };

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      store.setResourceLimiter(limiter);

      final Module module = engine.compileWat(TABLE_GROW_WAT);
      final Instance instance = module.instantiate(store);

      // Grow by 3 elements (from 2 to 5)
      instance.callFunction("grow", WasmValue.i32(3));

      assertFalse(receivedValues.isEmpty(), "Limiter should have been called");

      final int[] lastValues = receivedValues.get(receivedValues.size() - 1);
      assertEquals(2, lastValues[0], "Current elements should be 2");
      assertEquals(5, lastValues[1], "Desired elements should be 5");
      assertEquals(20, lastValues[2], "Maximum elements should be 20");

      LOGGER.info("[" + runtime + "] Table limiter received correct element values");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Conditional limiter allows then denies based on threshold")
  void conditionalLimiterAllowsThenDenies(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing conditional memory limiter with threshold");

    final long maxAllowedBytes = 3 * 65536L; // 3 pages max

    final ResourceLimiter limiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            final boolean allowed = desiredBytes <= maxAllowedBytes;
            LOGGER.info(
                "["
                    + runtime
                    + "] memoryGrowing: desired="
                    + desiredBytes
                    + ", threshold="
                    + maxAllowedBytes
                    + " -> "
                    + (allowed ? "ALLOW" : "DENY"));
            return allowed;
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            return true;
          }
        };

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      store.setResourceLimiter(limiter);

      final Module module = engine.compileWat(MEMORY_GROW_WAT);
      final Instance instance = module.instantiate(store);

      // Grow by 1 (to 2 pages = 2*65536 bytes) - should succeed (within 3 page threshold)
      final int result1 = instance.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(1, result1, "First grow should succeed and return previous size 1");
      assertEquals(2, instance.callFunction("size")[0].asInt(), "Should be 2 pages now");

      // Grow by 1 more (to 3 pages = 3*65536 bytes) - should succeed (at threshold)
      final int result2 = instance.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(2, result2, "Second grow should succeed and return previous size 2");
      assertEquals(3, instance.callFunction("size")[0].asInt(), "Should be 3 pages now");

      // Grow by 1 more (to 4 pages = 4*65536 bytes) - should fail (exceeds threshold)
      final int result3 = instance.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(-1, result3, "Third grow should fail because it exceeds threshold");
      assertEquals(
          3,
          instance.callFunction("size")[0].asInt(),
          "Should still be 3 pages after denied growth");

      LOGGER.info("[" + runtime + "] Conditional limiter correctly allowed then denied");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple stores with different limiters are independent")
  void multipleStoresWithDifferentLimiters(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple stores with different limiters");

    final AtomicInteger store1Calls = new AtomicInteger(0);
    final AtomicInteger store2Calls = new AtomicInteger(0);
    final AtomicBoolean store2DenyMode = new AtomicBoolean(false);

    final ResourceLimiter allowLimiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            store1Calls.incrementAndGet();
            return true; // Allow all
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            return true;
          }
        };

    final ResourceLimiter denyAfterInitLimiter =
        new ResourceLimiter() {
          @Override
          public boolean memoryGrowing(
              final long currentBytes, final long desiredBytes, final long maximumBytes) {
            store2Calls.incrementAndGet();
            if (!store2DenyMode.get()) {
              return true; // Allow during instantiation
            }
            return false; // Deny after instantiation
          }

          @Override
          public boolean tableGrowing(
              final int currentElements, final int desiredElements, final int maximumElements) {
            return true;
          }
        };

    try (Engine engine = Engine.create();
        Store store1 = engine.createStore();
        Store store2 = engine.createStore()) {
      store1.setResourceLimiter(allowLimiter);
      store2.setResourceLimiter(denyAfterInitLimiter);

      final Module module = engine.compileWat(MEMORY_GROW_WAT);

      final Instance instance1 = module.instantiate(store1);
      final Instance instance2 = module.instantiate(store2);

      // Switch store2 to deny mode after both instances are created
      store2DenyMode.set(true);

      // Store 1 should allow growth
      final int result1 = instance1.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(1, result1, "Store 1 (allow) should succeed with previous size 1");

      // Store 2 should deny growth
      final int result2 = instance2.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(-1, result2, "Store 2 (deny) should fail with -1");

      assertTrue(store1Calls.get() > 0, "Allow limiter should have been called for store 1");
      assertTrue(store2Calls.get() > 0, "Deny limiter should have been called for store 2");

      LOGGER.info(
          "["
              + runtime
              + "] Multiple stores: store1 calls="
              + store1Calls.get()
              + ", store2 calls="
              + store2Calls.get());

      instance1.close();
      instance2.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setResourceLimiter with null throws IllegalArgumentException")
  void setResourceLimiterNullThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing null limiter throws exception");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> store.setResourceLimiter(null),
          "Setting null limiter should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null limiter correctly threw IllegalArgumentException");
    }
  }
}
