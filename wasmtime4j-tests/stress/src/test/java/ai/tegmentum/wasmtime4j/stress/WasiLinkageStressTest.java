package ai.tegmentum.wasmtime4j.stress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
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
 * Stress tests for WASI module linking under repeated instantiation.
 *
 * <p>Tests that WASI linkage remains stable after many cycles of module compilation, linking, and
 * instantiation.
 */
public class WasiLinkageStressTest {

  private static final Logger LOGGER = Logger.getLogger(WasiLinkageStressTest.class.getName());

  /**
   * A minimal WASI module that imports wasi_snapshot_preview1 proc_exit. This is the simplest
   * possible WASI module that requires the linker to provide WASI imports.
   */
  private static final String WASI_WAT =
      "(module\n"
          + "  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"_start\")\n"
          + "    nop\n"
          + "  )\n"
          + ")";

  /**
   * A simple non-WASI module with no imports.
   */
  private static final String NON_WASI_WAT =
      "(module\n"
          + "  (func (export \"get42\") (result i32) i32.const 42)\n"
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
  void repeatedWasiInstantiation() throws Exception {
    final int cycles = 500;
    LOGGER.info("Starting repeatedWasiInstantiation with " + cycles + " cycles");

    for (int i = 0; i < cycles; i++) {
      try (Engine engine = runtime.createEngine()) {
        try (Linker<Void> linker = runtime.createLinker(engine)) {
          linker.enableWasi();
          try (Store store = engine.createStore()) {
            try (Module module = engine.compileWat(WASI_WAT)) {
              try (Instance instance = linker.instantiate(store, module)) {
                assertNotNull(instance, "WASI instance should not be null at cycle " + i);
              }
            }
          }
        }
      }

      if ((i + 1) % 100 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + cycles + " WASI instantiation cycles");
      }
    }

    LOGGER.info("repeatedWasiInstantiation completed successfully");
  }

  @Test
  void wasiAfterManyNonWasiCycles() throws Exception {
    final int nonWasiCycles = 1000;
    LOGGER.info(
        "Starting wasiAfterManyNonWasiCycles: "
            + nonWasiCycles
            + " non-WASI cycles then WASI verification");

    // Run many non-WASI cycles
    for (int i = 0; i < nonWasiCycles; i++) {
      try (Engine engine = runtime.createEngine();
          Store store = engine.createStore();
          Module module = engine.compileWat(NON_WASI_WAT);
          Instance instance = module.instantiate(store)) {
        assertNotNull(instance, "Non-WASI instance should not be null at cycle " + i);
      }

      if ((i + 1) % 250 == 0) {
        LOGGER.info("Completed " + (i + 1) + "/" + nonWasiCycles + " non-WASI cycles");
      }
    }

    // Now verify WASI linking still works
    LOGGER.info("Verifying WASI linkage after " + nonWasiCycles + " non-WASI cycles");
    assertDoesNotThrow(
        () -> {
          try (Engine engine = runtime.createEngine();
              Linker<Void> linker = runtime.createLinker(engine)) {
            linker.enableWasi();
            try (Store store = engine.createStore();
                Module module = engine.compileWat(WASI_WAT);
                Instance instance = linker.instantiate(store, module)) {
              assertNotNull(instance, "WASI instance should work after non-WASI cycles");
            }
          }
        },
        "WASI linking should work after many non-WASI cycles");

    LOGGER.info("wasiAfterManyNonWasiCycles completed successfully");
  }

  @Test
  void multipleWasiModulesSequential() throws Exception {
    final int moduleCount = 100;
    LOGGER.info(
        "Starting multipleWasiModulesSequential with " + moduleCount + " sequential modules");

    // Each iteration creates a fresh engine+linker+store+module+instance
    // to test that WASI linkage works reliably across independent sessions
    for (int i = 0; i < moduleCount; i++) {
      try (Engine engine = runtime.createEngine();
          Linker<Void> linker = runtime.createLinker(engine)) {
        linker.enableWasi();

        try (Store store = engine.createStore();
            Module module = engine.compileWat(WASI_WAT);
            Instance instance = linker.instantiate(store, module)) {
          assertNotNull(instance, "WASI instance should not be null at module " + i);
        }
      }

      if ((i + 1) % 25 == 0) {
        LOGGER.info(
            "Completed " + (i + 1) + "/" + moduleCount + " sequential WASI module instantiations");
      }
    }

    LOGGER.info("multipleWasiModulesSequential completed successfully");
  }
}
