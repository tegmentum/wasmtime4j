package ai.tegmentum.wasmtime4j.stress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.IOException;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Stress tests for combinations of features under stress that individually work but may conflict.
 *
 * <p>Tests interactions between shared memory, WASI, fuel tracking, SIMD, and other engine
 * features.
 */
public class CrossFeatureInteractionStressTest {

  private static final Logger LOGGER =
      Logger.getLogger(CrossFeatureInteractionStressTest.class.getName());

  private static final String SHARED_MEMORY_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1 10 shared)\n"
          + "  (func (export \"load\") (param i32) (result i32)\n"
          + "    local.get 0\n"
          + "    i32.atomic.load\n"
          + "  )\n"
          + ")";

  private static final String WASI_WAT =
      "(module\n"
          + "  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"_start\") nop)\n"
          + ")";

  private static final String SIMPLE_WAT =
      "(module\n"
          + "  (func (export \"get42\") (result i32) i32.const 42)\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + ")";

  private static final String FUEL_WAT =
      "(module\n"
          + "  (func (export \"count\") (param i32) (result i32)\n"
          + "    (local $i i32)\n"
          + "    (local.set $i (i32.const 0))\n"
          + "    (block $break\n"
          + "      (loop $loop\n"
          + "        (br_if $break (i32.ge_u (local.get $i) (local.get 0)))\n"
          + "        (local.set $i (i32.add (local.get $i) (i32.const 1)))\n"
          + "        (br $loop)\n"
          + "      )\n"
          + "    )\n"
          + "    (local.get $i)\n"
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
  void sharedMemoryThenWasi() throws Exception {
    final int cycles = 200;
    LOGGER.info(
        "Starting sharedMemoryThenWasi: " + cycles + " cycles alternating shared memory and WASI");

    for (int i = 0; i < cycles; i++) {
      // Shared memory cycle (requires threads feature)
      final EngineConfig threadsConfig = new EngineConfig();
      threadsConfig.addWasmFeature(WasmFeature.THREADS);

      try (Engine engine = runtime.createEngine(threadsConfig);
          Store store = engine.createStore();
          Module module = engine.compileWat(SHARED_MEMORY_WAT);
          Instance instance = module.instantiate(store)) {
        assertNotNull(instance, "Shared memory instance null at cycle " + i);
      }

      // WASI cycle
      try (Engine engine = runtime.createEngine();
          Linker<Void> linker = runtime.createLinker(engine)) {
        linker.enableWasi();
        try (Store store = engine.createStore();
            Module module = engine.compileWat(WASI_WAT);
            Instance instance = linker.instantiate(store, module)) {
          assertNotNull(instance, "WASI instance null at cycle " + i);
        }
      }

      if ((i + 1) % 50 == 0) {
        LOGGER.info(
            "Completed " + (i + 1) + "/" + cycles + " shared-memory/WASI alternation cycles");
      }
    }

    LOGGER.info("sharedMemoryThenWasi completed successfully");
  }

  @Test
  void multiFeatureEngineConfig() throws Exception {
    final int cycles = 300;
    LOGGER.info(
        "Starting multiFeatureEngineConfig: "
            + cycles
            + " cycles with various feature combinations");

    // Different feature combinations to test
    final WasmFeature[][] featureSets = {
      {WasmFeature.THREADS, WasmFeature.SIMD},
      {WasmFeature.MULTI_VALUE, WasmFeature.BULK_MEMORY},
      {WasmFeature.SIMD},
      {WasmFeature.THREADS},
    };

    for (int i = 0; i < cycles; i++) {
      final WasmFeature[] features = featureSets[i % featureSets.length];
      final EngineConfig config = new EngineConfig();
      for (final WasmFeature feature : features) {
        config.addWasmFeature(feature);
      }

      try (Engine engine = runtime.createEngine(config);
          Store store = engine.createStore();
          Module module = engine.compileWat(SIMPLE_WAT);
          Instance instance = module.instantiate(store)) {
        assertNotNull(
            instance,
            "Instance null at cycle " + i + " with feature set " + (i % featureSets.length));
        assertTrue(instance.getFunction("get42").isPresent(), "Missing get42 at cycle " + i);
      }

      if ((i + 1) % 100 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + cycles + " multi-feature engine config cycles");
      }
    }

    LOGGER.info("multiFeatureEngineConfig completed successfully");
  }

  @Test
  void fuelTrackingUnderStress() throws Exception {
    final int cycles = 500;
    LOGGER.info(
        "Starting fuelTrackingUnderStress: " + cycles + " cycles with fuel-tracked function calls");

    for (int i = 0; i < cycles; i++) {
      final int cycle = i;
      final EngineConfig config = new EngineConfig();
      config.consumeFuel(true);

      try (Engine engine = runtime.createEngine(config);
          Store store = engine.createStore();
          Module module = engine.compileWat(FUEL_WAT);
          Instance instance = module.instantiate(store)) {

        // Add enough fuel for a small loop
        store.addFuel(100_000L);

        final WasmFunction countFunc =
            instance
                .getFunction("count")
                .orElseThrow(() -> new AssertionError("Missing count function at cycle " + cycle));

        // Call with a small loop count that should succeed with available fuel
        final WasmValue[] result = countFunc.call(WasmValue.i32(10));
        assertNotNull(result, "Result null at cycle " + i);
        assertTrue(result.length > 0, "Result should have at least one value at cycle " + i);

        // Verify fuel was consumed
        final long remainingFuel = store.getFuel();
        assertTrue(
            remainingFuel < 100_000L,
            "Fuel should be consumed at cycle " + i + ", remaining: " + remainingFuel);
      }

      if ((i + 1) % 100 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + cycles + " fuel tracking cycles");
      }
    }

    // Final verification that fuel tracking still works
    LOGGER.info("Verifying fuel tracking after " + cycles + " cycles");
    assertDoesNotThrow(
        () -> {
          final EngineConfig config = new EngineConfig();
          config.consumeFuel(true);
          try (Engine engine = runtime.createEngine(config);
              Store store = engine.createStore();
              Module module = engine.compileWat(FUEL_WAT);
              Instance instance = module.instantiate(store)) {
            store.addFuel(100_000L);
            final WasmFunction countFunc =
                instance
                    .getFunction("count")
                    .orElseThrow(() -> new AssertionError("Missing count function"));
            countFunc.call(WasmValue.i32(5));
          }
        },
        "Fuel tracking should work after stress cycles");

    LOGGER.info("fuelTrackingUnderStress completed successfully");
  }

  /** Inner helper class for runtime exceptions in lambdas. */
  private static class AssertionError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    AssertionError(final String message) {
      super(message);
    }
  }
}
