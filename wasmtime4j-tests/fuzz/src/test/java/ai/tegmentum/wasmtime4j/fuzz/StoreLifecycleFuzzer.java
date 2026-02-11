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

package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Fuzz tests for Store lifecycle management.
 *
 * <p>This fuzzer tests the robustness of Store operations by:
 *
 * <ul>
 *   <li>Rapid create/close cycles in random order
 *   <li>Operations on closed stores
 *   <li>Fuel manipulation with extreme values
 *   <li>Store data round-trip with various object types
 *   <li>Multiple stores from the same engine
 * </ul>
 *
 * @since 1.0.0
 */
public class StoreLifecycleFuzzer {

  private static final String SIMPLE_MODULE_WAT =
      """
      (module
        (func $identity (param i32) (result i32)
          local.get 0)
        (export "identity" (func $identity))
      )
      """;

  /**
   * Rapid create/close cycles with stores closed in random order.
   *
   * <p>Creates N stores, then closes them in a fuzzed permutation order. Verifies no resource leaks
   * or crashes.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzStoreCreateClose(final FuzzedDataProvider data) {
    final int storeCount = data.consumeInt(1, 50);

    try (Engine engine = Engine.create()) {
      final List<Store> stores = new ArrayList<>();
      for (int i = 0; i < storeCount; i++) {
        try {
          stores.add(engine.createStore());
        } catch (WasmException e) {
          // Expected: resource limits may be hit
          break;
        }
      }

      // Build a fuzzed permutation for close order
      final List<Integer> closeOrder = new ArrayList<>();
      for (int i = 0; i < stores.size(); i++) {
        closeOrder.add(i);
      }
      // Shuffle using fuzzed seed
      if (!closeOrder.isEmpty()) {
        final long seed = data.consumeLong();
        Collections.shuffle(closeOrder, new Random(seed));
      }

      // Close in fuzzed order
      for (final int index : closeOrder) {
        try {
          stores.get(index).close();
        } catch (Exception e) {
          // Expected: double-close or already-closed
        }
      }
    } catch (WasmException e) {
      // Expected: engine creation may fail
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a store, closes it, then attempts various operations on the closed store.
   *
   * <p>All operations should either throw cleanly or return safely. No JVM crash should occur.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzStoreOperationsAfterClose(final FuzzedDataProvider data) {
    final int operationChoice = data.consumeInt(0, 4);

    try (Engine engine = Engine.create()) {
      final Store store = engine.createStore();
      store.close();

      switch (operationChoice) {
        case 0 -> {
          try {
            store.getData();
          } catch (IllegalStateException e) {
            // Expected: store is closed
          }
        }
        case 1 -> {
          try {
            store.setData("test-data");
          } catch (IllegalStateException e) {
            // Expected: store is closed
          }
        }
        case 2 -> {
          try {
            store.addFuel(100);
          } catch (IllegalStateException e) {
            // Expected: store is closed
          } catch (WasmException e) {
            // Expected: fuel operation may fail
          } catch (UnsupportedOperationException e) {
            // Expected: fuel may not be enabled
          }
        }
        case 3 -> {
          try {
            store.isValid();
          } catch (IllegalStateException e) {
            // Expected: store is closed
          }
        }
        case 4 -> {
          try {
            store.close();
          } catch (Exception e) {
            // Expected: double close
          }
        }
        default -> { }
      }
    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests fuel manipulation with fuzzed values including edge cases.
   *
   * <p>Creates an engine with fuel consumption enabled, then adds and consumes fuel with fuzzed
   * amounts including negatives, zero, and Long.MAX_VALUE.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzFuelManipulation(final FuzzedDataProvider data) {
    final long addAmount = data.consumeLong();
    final long consumeAmount = data.consumeLong();
    final int operationCount = data.consumeInt(1, 10);

    try {
      final EngineConfig config = new EngineConfig().consumeFuel(true);
      try (Engine engine = Engine.create(config);
          Store store = engine.createStore()) {
        for (int i = 0; i < operationCount; i++) {
          try {
            store.addFuel(addAmount);
          } catch (WasmException e) {
            // Expected: invalid fuel amount
          } catch (IllegalArgumentException e) {
            // Expected: negative fuel
          } catch (UnsupportedOperationException e) {
            // Expected: fuel not enabled
          }

          try {
            store.consumeFuel(consumeAmount);
          } catch (WasmException e) {
            // Expected: insufficient fuel or invalid amount
          } catch (IllegalArgumentException e) {
            // Expected: negative amount
          } catch (UnsupportedOperationException e) {
            // Expected: fuel not enabled
          }

          try {
            store.getRemainingFuel();
          } catch (WasmException e) {
            // Expected
          } catch (UnsupportedOperationException e) {
            // Expected: fuel not enabled
          }

          try {
            store.getFuel();
          } catch (WasmException e) {
            // Expected
          } catch (UnsupportedOperationException e) {
            // Expected
          }
        }
      }
    } catch (WasmException e) {
      // Expected: config or engine creation may fail
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests Store data round-trip with various fuzzed object types.
   *
   * <p>Sets data with String, byte[], Integer, or null, then gets it back and verifies identity.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzStoreDataRoundTrip(final FuzzedDataProvider data) {
    final int typeChoice = data.consumeInt(0, 3);

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Object testData;
      switch (typeChoice) {
        case 0 -> testData = data.consumeString(data.consumeInt(0, 1000));
        case 1 -> testData = data.consumeBytes(data.consumeInt(0, 1000));
        case 2 -> testData = data.consumeInt();
        case 3 -> testData = null;
        default -> testData = null;
      }

      try {
        store.setData(testData);
        final Object retrieved = store.getData();
        // Verify identity: should be the exact same object reference
        if (testData != null && retrieved != testData) {
          throw new AssertionError(
              "Store data round-trip failed: expected same reference but got different object");
        }
        if (testData == null && retrieved != null) {
          throw new AssertionError(
              "Store data round-trip failed: expected null but got " + retrieved);
        }
      } catch (IllegalStateException e) {
        // Expected: store may be in invalid state
      } catch (UnsupportedOperationException e) {
        // Expected: setData may not be supported
      }
    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates N stores from one engine, performs operations on each, then closes the engine first and
   * attempts store operations.
   *
   * <p>Validates that no JVM crash occurs when stores outlive their engine.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMultipleStoresSameEngine(final FuzzedDataProvider data) {
    final int storeCount = data.consumeInt(2, 20);
    final int argValue = data.consumeInt();

    try {
      final Engine engine = Engine.create();
      final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
      final List<Store> stores = new ArrayList<>();
      final List<Instance> instances = new ArrayList<>();

      // Create stores and instances
      for (int i = 0; i < storeCount; i++) {
        try {
          final Store store = engine.createStore();
          stores.add(store);
          try {
            final Instance instance = module.instantiate(store);
            instances.add(instance);
          } catch (WasmException e) {
            // Expected: instantiation may fail
          }
        } catch (WasmException e) {
          // Expected: resource limits
          break;
        }
      }

      // Operate on each instance
      for (final Instance instance : instances) {
        try {
          instance.callFunction("identity", WasmValue.i32(argValue));
        } catch (WasmException e) {
          // Expected
        }
      }

      // Close engine first
      module.close();
      engine.close();

      // Now try store operations (engine is closed)
      for (final Store store : stores) {
        try {
          store.isValid();
        } catch (IllegalStateException e) {
          // Expected: engine may be closed
        }
      }

      // Clean up instances and stores
      for (final Instance instance : instances) {
        try {
          instance.close();
        } catch (Exception e) {
          // Expected
        }
      }
      for (final Store store : stores) {
        try {
          store.close();
        } catch (Exception e) {
          // Expected
        }
      }
    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
