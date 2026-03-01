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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Panama-specific test suite for PanamaGlobal close safety operations.
 *
 * <p>Generic global get/set and type tests have been migrated to {@code GlobalApiDualRuntimeTest}
 * in the integration test module.
 */
public class PanamaGlobalTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaGlobalTest.class.getName());

  private PanamaEngine engine;
  private PanamaStore store;
  private PanamaModule module;
  private PanamaInstance instance;

  /** Sets up test fixtures before each test. */
  @BeforeEach
  public void setUp() throws Exception {
    // Load the test WASM file with memory, table, and global exports
    // Globals: g_i32 (mut i32, value 42), g_i64 (mut i64, value 100), g_f32 (mut f32, value 3.14)
    final Path wasmPath =
        Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
    final byte[] wasmBytes = Files.readAllBytes(wasmPath);

    engine = new PanamaEngine();
    store = new PanamaStore(engine);
    module = new PanamaModule(engine, wasmBytes);
    instance = new PanamaInstance(module, store);
  }

  /** Cleans up test fixtures after each test. */
  @AfterEach
  public void tearDown() {
    if (instance != null) {
      instance.close();
    }
    if (store != null) {
      store.close();
    }
    if (module != null) {
      module.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("get on closed instance global should throw IllegalStateException")
  void getOnClosedInstanceGlobalShouldThrow() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent(), "Global g_i32 should be present");
    final PanamaInstanceGlobal global = (PanamaInstanceGlobal) globalOpt.get();

    global.close();
    LOGGER.info("Instance global closed, attempting get()");

    assertThrows(
        IllegalStateException.class,
        global::get,
        "get() on closed instance global should throw IllegalStateException");
    assertThrows(
        IllegalStateException.class,
        () -> global.set(WasmValue.i32(1)),
        "set() on closed instance global should throw IllegalStateException");
    LOGGER.info("IllegalStateException thrown as expected for get/set on closed instance global");
  }

  @Test
  @DisplayName("double close should be safe")
  void doubleCloseShouldBeSafe() {
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
    assertTrue(globalOpt.isPresent(), "Global g_i32 should be present");
    final PanamaInstanceGlobal global = (PanamaInstanceGlobal) globalOpt.get();

    global.close();
    LOGGER.info("First close completed");

    assertDoesNotThrow(global::close, "Second close should not throw");
    LOGGER.info("Second close completed without exception");

    assertThrows(
        IllegalStateException.class,
        global::get,
        "get() after double close should still throw IllegalStateException");
    LOGGER.info("IllegalStateException confirmed after double close");
  }
}
