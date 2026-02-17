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

package ai.tegmentum.wasmtime4j.memory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.test.TestUtils;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Regression test for DESTROYED_POINTERS address reuse bug.
 *
 * <p>When running many engine/store/memory create-destroy cycles in the same JVM (simulating
 * benchmark {@code -f 0} mode), the allocator may reuse freed addresses. If the DESTROYED_POINTERS
 * registry never removes entries, newly allocated resources at reused addresses get incorrectly
 * detected as "already destroyed" and silently leak instead of being freed. After hundreds of
 * iterations, accumulated leaked stores (each holding virtual address space via Wasmtime guard
 * pages) can exhaust resources, causing subsequent shared memory and WASI operations to fail.
 *
 * <p>This test validates that:
 *
 * <ul>
 *   <li>Creating and destroying 600+ engine/store cycles does not corrupt state
 *   <li>Shared memory {@code isShared()} correctly returns true after many cycles
 *   <li>Atomic operations on shared memory succeed after many cycles
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Address Reuse Regression Tests")
public final class AddressReuseRegressionTest {

  private static final Logger LOGGER = Logger.getLogger(AddressReuseRegressionTest.class.getName());

  private static final int WARMUP_CYCLES = 600;

  private static boolean sharedMemorySupported = false;

  private static final String SHARED_MEMORY_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1 1 shared)\n"
          + "  (func (export \"atomic_load\") (param $offset i32) (result i32)\n"
          + "    local.get $offset\n"
          + "    i32.atomic.load\n"
          + "  )\n"
          + "  (func (export \"atomic_store\") (param $offset i32) (param $value i32)\n"
          + "    local.get $offset\n"
          + "    local.get $value\n"
          + "    i32.atomic.store\n"
          + "  )\n"
          + "  (func (export \"atomic_add\") (param $offset i32) (param $value i32) (result i32)\n"
          + "    local.get $offset\n"
          + "    local.get $value\n"
          + "    i32.atomic.rmw.add\n"
          + "  )\n"
          + ")";

  private static final String SIMPLE_WAT =
      "(module\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add\n"
          + "  )\n"
          + ")";

  @BeforeAll
  static void checkSharedMemorySupport() {
    LOGGER.info("Probing shared memory support...");
    try {
      final WasmRuntime runtime = WasmRuntimeFactory.create();
      try {
        final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
        final Engine engine = runtime.createEngine(config);
        try {
          final Module module = engine.compileWat(SHARED_MEMORY_WAT);
          if (module != null) {
            final Store store = runtime.createStore(engine);
            try {
              final Instance instance = module.instantiate(store);
              if (instance != null) {
                final java.util.Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
                if (memoryOpt.isPresent()) {
                  memoryOpt.get().isShared();
                  sharedMemorySupported = true;
                  LOGGER.info("Shared memory support confirmed");
                }
                instance.close();
              }
            } finally {
              store.close();
            }
            module.close();
          }
        } finally {
          engine.close();
        }
      } finally {
        runtime.close();
      }
    } catch (final Exception e) {
      LOGGER.warning("Shared memory not supported: " + e.getMessage());
    }
  }

  @AfterAll
  static void cleanup() {
    TestUtils.clearHandleRegistries();
  }

  @Test
  @DisplayName(
      "Shared memory isShared() returns true after 600+ engine/store create-destroy cycles")
  void sharedMemoryAfterManyCreateDestroyCycles(final TestInfo testInfo) throws Exception {
    assumeTrue(sharedMemorySupported, "Shared memory not supported on this platform/runtime");

    LOGGER.info("Starting " + testInfo.getDisplayName());
    LOGGER.info("Running " + WARMUP_CYCLES + " engine/store create-destroy cycles...");

    // Phase 1: Create and destroy many engines/stores to trigger address reuse
    for (int i = 0; i < WARMUP_CYCLES; i++) {
      final WasmRuntime runtime = WasmRuntimeFactory.create();
      final Engine engine = runtime.createEngine();
      final Store store = runtime.createStore(engine);
      final Module module = engine.compileWat(SIMPLE_WAT);

      final Instance instance = module.instantiate(store);
      final WasmFunction addFn =
          instance
              .getFunction("add")
              .orElseThrow(() -> new RuntimeException("add function not found"));
      final WasmValue[] result = addFn.call(WasmValue.i32(1), WasmValue.i32(2));
      assertEquals(3, result[0].asInt(), "Simple add should work correctly at cycle " + i);

      instance.close();
      module.close();
      store.close();
      engine.close();
      runtime.close();

      if ((i + 1) % 100 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + WARMUP_CYCLES + " cycles");
      }
    }

    LOGGER.info("Warmup complete. Now testing shared memory...");

    // Phase 2: Create shared memory and verify it works correctly
    final WasmRuntime runtime = WasmRuntimeFactory.create();
    try {
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = runtime.createEngine(config);
      try {
        final Store store = runtime.createStore(engine);
        try {
          final Module module = engine.compileWat(SHARED_MEMORY_WAT);
          try {
            final Instance instance = module.instantiate(store);
            try {
              final WasmMemory memory =
                  instance
                      .getMemory("memory")
                      .orElseThrow(() -> new RuntimeException("memory export not found"));

              // Verify isShared() returns true, not false due to error masking
              final boolean shared =
                  assertDoesNotThrow(
                      () -> memory.isShared(),
                      "isShared() should not throw after many create-destroy cycles");
              assertTrue(
                  shared,
                  "Memory declared as shared should report isShared()=true. "
                      + "If false, the DESTROYED_POINTERS bug may be causing "
                      + "resource leaks that exhaust native resources.");

              LOGGER.info("isShared() correctly returned true after " + WARMUP_CYCLES + " cycles");

              // Verify atomic operations work
              final WasmFunction atomicStore =
                  instance
                      .getFunction("atomic_store")
                      .orElseThrow(() -> new RuntimeException("atomic_store not found"));
              final WasmFunction atomicLoad =
                  instance
                      .getFunction("atomic_load")
                      .orElseThrow(() -> new RuntimeException("atomic_load not found"));
              final WasmFunction atomicAdd =
                  instance
                      .getFunction("atomic_add")
                      .orElseThrow(() -> new RuntimeException("atomic_add not found"));

              // Store a value atomically
              atomicStore.call(WasmValue.i32(0), WasmValue.i32(42));

              // Load it back
              WasmValue[] loadResult = atomicLoad.call(WasmValue.i32(0));
              assertEquals(42, loadResult[0].asInt(), "Atomic load should return stored value");

              // Atomic add
              WasmValue[] addResult = atomicAdd.call(WasmValue.i32(0), WasmValue.i32(8));
              assertEquals(42, addResult[0].asInt(), "Atomic add should return previous value");

              // Verify the add took effect
              loadResult = atomicLoad.call(WasmValue.i32(0));
              assertEquals(50, loadResult[0].asInt(), "Atomic load after add should return 50");

              // Verify Java-side atomic operations on shared memory
              assertDoesNotThrow(
                  () -> memory.atomicLoadInt(0),
                  "Java atomicLoadInt should work on shared memory after many cycles");

              final int javaLoadResult = memory.atomicLoadInt(0);
              assertEquals(
                  50,
                  javaLoadResult,
                  "Java atomicLoadInt should return same value as WASM atomic load");

              LOGGER.info("All atomic operations succeeded after " + WARMUP_CYCLES + " cycles");

            } finally {
              instance.close();
            }
          } finally {
            module.close();
          }
        } finally {
          store.close();
        }
      } finally {
        engine.close();
      }
    } finally {
      runtime.close();
    }
  }

  @Test
  @DisplayName("Multiple sequential shared memory instances work after create-destroy cycles")
  void multipleSequentialSharedMemoryInstances(final TestInfo testInfo) throws Exception {
    assumeTrue(sharedMemorySupported, "Shared memory not supported on this platform/runtime");

    LOGGER.info("Starting " + testInfo.getDisplayName());

    // Run 200 cycles of creating shared memory engines then destroying them
    // This specifically tests that DESTROYED_POINTERS entries are cleaned up
    for (int i = 0; i < 200; i++) {
      final int cycle = i;
      final WasmRuntime runtime = WasmRuntimeFactory.create();
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = runtime.createEngine(config);
      final Store store = runtime.createStore(engine);
      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      final Instance instance = module.instantiate(store);

      final WasmMemory memory =
          instance
              .getMemory("memory")
              .orElseThrow(() -> new RuntimeException("memory export not found at cycle " + cycle));

      assertTrue(memory.isShared(), "isShared() should return true at cycle " + cycle);

      instance.close();
      module.close();
      store.close();
      engine.close();
      runtime.close();

      if ((i + 1) % 50 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/200 shared memory cycles");
      }
    }

    LOGGER.info("All 200 shared memory cycles completed successfully");
  }

}
