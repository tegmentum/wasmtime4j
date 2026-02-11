package ai.tegmentum.wasmtime4j.stress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.IOException;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Stress tests targeting the DESTROYED_POINTERS address reuse bug pattern.
 *
 * <p>When native memory is freed and reallocated, the allocator may return the same address. If the
 * DESTROYED_POINTERS set still contains that address, new valid objects get incorrectly rejected.
 * These tests create high volume create-destroy cycles specifically designed to trigger this
 * condition.
 */
public class AddressReuseStressTest {

  private static final Logger LOGGER = Logger.getLogger(AddressReuseStressTest.class.getName());

  private static final String SIMPLE_WAT =
      "(module\n"
          + "  (func (export \"get42\") (result i32) i32.const 42)\n"
          + ")";

  private static final String SHARED_MEMORY_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1 10 shared)\n"
          + "  (func (export \"load\") (param i32) (result i32)\n"
          + "    local.get 0\n"
          + "    i32.atomic.load\n"
          + "  )\n"
          + "  (func (export \"store\") (param i32 i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.atomic.store\n"
          + "  )\n"
          + ")";

  private WasmRuntime runtime;

  @BeforeEach
  void setUp() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    runtime = WasmRuntimeFactory.create();
    LOGGER.info("Runtime created: " + runtime.getClass().getSimpleName());
  }

  @AfterEach
  void tearDown() throws IOException {
    if (runtime != null) {
      runtime.close();
    }
  }

  @Test
  void highVolumeAddressReuse() throws Exception {
    final int cycles = 5000;
    LOGGER.info("Starting highVolumeAddressReuse with " + cycles + " cycles");

    // Create and immediately destroy resources to maximize address reuse probability.
    // Small allocations are more likely to be reused by the allocator.
    for (int i = 0; i < cycles; i++) {
      try (Engine engine = runtime.createEngine()) {
        try (Store store = engine.createStore()) {
          try (Module module = engine.compileWat(SIMPLE_WAT)) {
            try (Instance instance = module.instantiate(store)) {
              // Immediately discard - forces quick reallocation of same addresses
              assertNotNull(instance);
            }
          }
        }
      }

      if ((i + 1) % 1000 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + cycles + " address reuse cycles");
      }
    }

    // After all the churn, verify new allocations work correctly.
    // If address reuse is incorrectly handled, this will fail with
    // "destroyed pointer" or similar errors.
    LOGGER.info("Verifying new allocations after " + cycles + " cycles");
    try (Engine engine = runtime.createEngine();
        Store store = engine.createStore();
        Module module = engine.compileWat(SIMPLE_WAT);
        Instance instance = module.instantiate(store)) {
      assertNotNull(instance, "New instance should be valid after address reuse cycles");
      assertTrue(
          instance.getFunction("get42").isPresent(),
          "Function export should be accessible after address reuse cycles");
    }

    LOGGER.info("highVolumeAddressReuse completed successfully");
  }

  @Test
  void sharedMemoryAfterAddressReuse() throws Exception {
    final int reuseCycles = 1000;
    LOGGER.info(
        "Starting sharedMemoryAfterAddressReuse: "
            + reuseCycles
            + " reuse cycles then shared memory with atomics");

    // Phase 1: Churn through many allocations to trigger address reuse
    for (int i = 0; i < reuseCycles; i++) {
      try (Engine engine = runtime.createEngine();
          Store store = engine.createStore();
          Module module = engine.compileWat(SIMPLE_WAT);
          Instance instance = module.instantiate(store)) {
        assertNotNull(instance);
      }

      if ((i + 1) % 250 == 0) {
        LOGGER.info("Reuse phase: " + (i + 1) + "/" + reuseCycles + " cycles");
      }
    }

    // Phase 2: Now try shared memory with atomics — this uses different native
    // code paths that may trip over stale destroyed-pointer entries
    LOGGER.info("Starting shared memory phase after " + reuseCycles + " reuse cycles");

    final EngineConfig threadsConfig = new EngineConfig();
    threadsConfig.addWasmFeature(ai.tegmentum.wasmtime4j.WasmFeature.THREADS);

    assertDoesNotThrow(
        () -> {
          try (Engine engine = runtime.createEngine(threadsConfig);
              Store store = engine.createStore();
              Module module = engine.compileWat(SHARED_MEMORY_WAT);
              Instance instance = module.instantiate(store)) {
            assertNotNull(instance, "Shared memory instance should work after address reuse");
            assertTrue(
                instance.getFunction("load").isPresent(),
                "load function should be accessible");
            assertTrue(
                instance.getFunction("store").isPresent(),
                "store function should be accessible");
          }
        },
        "Shared memory with atomics should work after address reuse cycles");

    LOGGER.info("sharedMemoryAfterAddressReuse completed successfully");
  }

  @Test
  void interleaveResourceTypes() throws Exception {
    final int cycles = 2000;
    LOGGER.info("Starting interleaveResourceTypes with " + cycles + " cycles");

    // Alternate between creating different resource types to maximize
    // the probability of address reuse across different resource categories
    for (int i = 0; i < cycles; i++) {
      final int phase = i % 4;

      switch (phase) {
        case 0:
          // Engine-only creation and destruction
          try (Engine engine = runtime.createEngine()) {
            assertNotNull(engine, "Engine should not be null at cycle " + i);
          }
          break;

        case 1:
          // Engine + Store
          try (Engine engine = runtime.createEngine();
              Store store = engine.createStore()) {
            assertNotNull(store, "Store should not be null at cycle " + i);
          }
          break;

        case 2:
          // Engine + Module (no instantiation)
          try (Engine engine = runtime.createEngine();
              Module module = engine.compileWat(SIMPLE_WAT)) {
            assertNotNull(module, "Module should not be null at cycle " + i);
          }
          break;

        case 3:
          // Full stack: Engine + Store + Module + Instance
          try (Engine engine = runtime.createEngine();
              Store store = engine.createStore();
              Module module = engine.compileWat(SIMPLE_WAT);
              Instance instance = module.instantiate(store)) {
            assertNotNull(instance, "Instance should not be null at cycle " + i);
          }
          break;

        default:
          break;
      }

      if ((i + 1) % 500 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + cycles + " interleaved resource cycles");
      }
    }

    // Verify all resource types still work after interleaved destruction
    LOGGER.info("Verifying all resource types after interleaved cycles");
    try (Engine engine = runtime.createEngine();
        Store store = engine.createStore();
        Module module = engine.compileWat(SIMPLE_WAT);
        Instance instance = module.instantiate(store)) {
      assertNotNull(instance, "Final instance should be valid");
      assertTrue(
          instance.getFunction("get42").isPresent(),
          "Final function export should be accessible");
    }

    LOGGER.info("interleaveResourceTypes completed successfully");
  }
}
