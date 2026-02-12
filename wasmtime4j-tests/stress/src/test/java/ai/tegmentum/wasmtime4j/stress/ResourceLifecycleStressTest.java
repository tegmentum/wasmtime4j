package ai.tegmentum.wasmtime4j.stress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Stress tests for resource lifecycle management.
 *
 * <p>Tests that creating and destroying thousands of engine/store/module/instance cycles does not
 * corrupt state or leak resources.
 */
public class ResourceLifecycleStressTest {

  private static final Logger LOGGER =
      Logger.getLogger(ResourceLifecycleStressTest.class.getName());

  private static final String SIMPLE_WAT =
      "(module\n"
          + "  (func (export \"get42\") (result i32) i32.const 42)\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
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
  void rapidCreateDestroyCycles() throws Exception {
    final int cycles = 2000;
    LOGGER.info("Starting rapidCreateDestroyCycles with " + cycles + " cycles");

    for (int i = 0; i < cycles; i++) {
      try (Engine engine = runtime.createEngine()) {
        try (Store store = engine.createStore()) {
          try (Module module = engine.compileWat(SIMPLE_WAT)) {
            try (Instance instance = module.instantiate(store)) {
              assertNotNull(instance, "Instance should not be null at cycle " + i);
            }
          }
        }
      }

      if ((i + 1) % 500 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + cycles + " create-destroy cycles");
      }
    }

    // Verify final operations succeed after all cycles
    LOGGER.info("Verifying final operation after " + cycles + " cycles");
    try (Engine engine = runtime.createEngine()) {
      try (Store store = engine.createStore()) {
        try (Module module = engine.compileWat(SIMPLE_WAT)) {
          try (Instance instance = module.instantiate(store)) {
            assertNotNull(instance, "Final instance should not be null");
            assertTrue(
                instance.getFunction("get42").isPresent(),
                "Final instance should have get42 export");
          }
        }
      }
    }

    LOGGER.info("rapidCreateDestroyCycles completed successfully");
  }

  @Test
  void nestedResourceLifecycles() throws Exception {
    final int engineCount = 10;
    final int storesPerEngine = 20;
    final int modulesPerStore = 5;
    final int instancesPerModule = 3;

    LOGGER.info(
        "Starting nestedResourceLifecycles: "
            + engineCount
            + " engines x "
            + storesPerEngine
            + " stores x "
            + modulesPerStore
            + " modules x "
            + instancesPerModule
            + " instances");

    int totalInstances = 0;

    for (int e = 0; e < engineCount; e++) {
      try (Engine engine = runtime.createEngine()) {
        for (int s = 0; s < storesPerEngine; s++) {
          try (Store store = engine.createStore()) {
            for (int m = 0; m < modulesPerStore; m++) {
              try (Module module = engine.compileWat(SIMPLE_WAT)) {
                for (int inst = 0; inst < instancesPerModule; inst++) {
                  try (Instance instance = module.instantiate(store)) {
                    assertNotNull(
                        instance,
                        "Instance should not be null at e="
                            + e
                            + " s="
                            + s
                            + " m="
                            + m
                            + " inst="
                            + inst);
                    totalInstances++;
                  }
                }
              }
            }
          }
        }
      }

      if ((e + 1) % 5 == 0) {
        LOGGER.info(
            "Completed engine "
                + (e + 1)
                + "/"
                + engineCount
                + ", total instances so far: "
                + totalInstances);
      }
    }

    LOGGER.info("nestedResourceLifecycles completed: " + totalInstances + " total instances");
  }

  @Test
  void outOfOrderDestruction() throws Exception {
    final int batchSize = 100;
    LOGGER.info("Starting outOfOrderDestruction with batch size " + batchSize);

    for (int batch = 0; batch < 10; batch++) {
      final List<Engine> engines = new ArrayList<>();
      final List<Store> stores = new ArrayList<>();
      final List<Module> modules = new ArrayList<>();
      final List<Instance> instances = new ArrayList<>();

      try {
        // Create resources
        for (int i = 0; i < batchSize; i++) {
          final Engine engine = runtime.createEngine();
          engines.add(engine);
          final Store store = engine.createStore();
          stores.add(store);
          final Module module = engine.compileWat(SIMPLE_WAT);
          modules.add(module);
          final Instance instance = module.instantiate(store);
          instances.add(instance);
        }

        LOGGER.info(
            "Batch "
                + (batch + 1)
                + ": Created "
                + batchSize
                + " resource sets, closing in"
                + " shuffled order");

        // Close instances in shuffled order
        final List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
          indices.add(i);
        }
        Collections.shuffle(indices);

        for (final int idx : indices) {
          instances.get(idx).close();
        }

        // Close modules in reverse order
        for (int i = modules.size() - 1; i >= 0; i--) {
          modules.get(i).close();
        }

        // Close stores in forward order
        for (final Store store : stores) {
          store.close();
        }

        // Close engines in shuffled order
        Collections.shuffle(indices);
        for (final int idx : indices) {
          engines.get(idx).close();
        }
      } catch (final Exception e) {
        // Clean up any remaining resources on failure
        for (final Instance instance : instances) {
          try {
            instance.close();
          } catch (final Exception ignored) {
            // Already closed or failed
          }
        }
        for (final Module module : modules) {
          try {
            module.close();
          } catch (final Exception ignored) {
            // Already closed or failed
          }
        }
        for (final Store store : stores) {
          try {
            store.close();
          } catch (final Exception ignored) {
            // Already closed or failed
          }
        }
        for (final Engine engine : engines) {
          try {
            engine.close();
          } catch (final Exception ignored) {
            // Already closed or failed
          }
        }
        throw e;
      }
    }

    // Verify operations still work after out-of-order destruction
    assertDoesNotThrow(
        () -> {
          try (Engine engine = runtime.createEngine();
              Store store = engine.createStore();
              Module module = engine.compileWat(SIMPLE_WAT);
              Instance instance = module.instantiate(store)) {
            assertNotNull(instance);
          }
        },
        "Operations should succeed after out-of-order destruction");

    LOGGER.info("outOfOrderDestruction completed successfully");
  }
}
