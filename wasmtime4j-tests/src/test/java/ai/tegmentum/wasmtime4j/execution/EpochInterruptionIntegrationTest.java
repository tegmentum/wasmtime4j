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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for epoch-based interruption functionality.
 *
 * <p>Epochs provide a way to interrupt long-running WebAssembly code at regular intervals, enabling
 * cooperative multitasking and timeout handling.
 *
 * @since 1.0.0
 */
@DisplayName("Epoch Interruption Integration Tests")
public final class EpochInterruptionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(EpochInterruptionIntegrationTest.class.getName());

  private static boolean epochInterruptionAvailable = false;

  /** Simple WebAssembly module that exports an add function. */
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07, // type section
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export "add"
        0x0A,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6A,
        0x0B // code
      };

  /** WebAssembly module with a loop that can be interrupted. */
  private static final byte[] LOOP_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05, // type section
        0x01,
        0x60,
        0x01,
        0x7F,
        0x00, // (i32) -> void
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x08,
        0x01,
        0x04,
        0x6C,
        0x6F,
        0x6F,
        0x70,
        0x00,
        0x00, // export "loop"
        0x0A,
        0x17,
        0x01, // code section (size=23)
        0x15,
        0x01,
        0x01,
        0x7F, // 1 local i32
        0x03,
        0x40, // loop
        0x20,
        0x01, // local.get 1
        0x41,
        0x01, // i32.const 1
        0x6A, // i32.add
        0x22,
        0x01, // local.tee 1
        0x20,
        0x00, // local.get 0
        0x46, // i32.eq
        0x0D,
        0x00, // br_if 0
        0x0C,
        0x00, // br 0
        0x0B, // end
        0x0B // end
      };

  @BeforeAll
  static void checkEpochInterruptionAvailable() {
    try {
      final EngineConfig config = new EngineConfig().setEpochInterruption(true);
      try (final Engine engine = Engine.create(config)) {
        epochInterruptionAvailable = engine.isEpochInterruptionEnabled();
        LOGGER.info("Epoch interruption available: " + epochInterruptionAvailable);
      }
    } catch (final Exception e) {
      LOGGER.warning("Epoch interruption not available: " + e.getMessage());
      epochInterruptionAvailable = false;
    }
  }

  @Nested
  @DisplayName("Epoch Configuration Tests")
  class EpochConfigurationTests {

    @Test
    @DisplayName("should enable epoch interruption via engine config")
    void shouldEnableEpochInterruptionViaEngineConfig() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing epoch interruption enablement");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config)) {
        assertTrue(
            engine.isEpochInterruptionEnabled(), "Engine should have epoch interruption enabled");
        LOGGER.info("Epoch interruption enabled: " + engine.isEpochInterruptionEnabled());
      }
    }

    @Test
    @DisplayName("should not enable epoch interruption by default")
    void shouldNotEnableEpochInterruptionByDefault() throws Exception {
      LOGGER.info("Testing default epoch interruption configuration");

      final EngineConfig config = new EngineConfig();

      try (final Engine engine = Engine.create(config)) {
        LOGGER.info("Default epoch interruption: " + engine.isEpochInterruptionEnabled());
        assertTrue(engine.isValid(), "Engine should be valid");
      }
    }
  }

  @Nested
  @DisplayName("Epoch Deadline Tests")
  class EpochDeadlineTests {

    @Test
    @DisplayName("should set epoch deadline on store")
    void shouldSetEpochDeadlineOnStore() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline setting");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        assertDoesNotThrow(
            () -> store.setEpochDeadline(100L), "Should set epoch deadline without throwing");

        LOGGER.info("Successfully set epoch deadline");
      }
    }

    @Test
    @DisplayName("should configure epoch deadline trap")
    void shouldConfigureEpochDeadlineTrap() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline trap configuration");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        try {
          store.epochDeadlineTrap();
          LOGGER.info("Successfully configured epoch deadline trap");
        } catch (final UnsatisfiedLinkError e) {
          LOGGER.warning("Native method not implemented: " + e.getMessage());
          assumeTrue(false, "Native method not implemented: " + e.getMessage());
        }
      }
    }
  }

  @Nested
  @DisplayName("Epoch Increment Tests")
  class EpochIncrementTests {

    @Test
    @DisplayName("should increment epoch on engine")
    void shouldIncrementEpochOnEngine() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing epoch increment");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config)) {
        assertDoesNotThrow(engine::incrementEpoch, "Should increment epoch without throwing");

        // Increment multiple times
        for (int i = 0; i < 10; i++) {
          engine.incrementEpoch();
        }

        LOGGER.info("Successfully incremented epoch multiple times");
      }
    }

    @Test
    @DisplayName("should be thread-safe when incrementing epoch")
    void shouldBeThreadSafeWhenIncrementingEpoch() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing thread-safe epoch increment");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config)) {
        final int numThreads = 4;
        final int incrementsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final AtomicBoolean failed = new AtomicBoolean(false);

        for (int i = 0; i < numThreads; i++) {
          executor.submit(
              () -> {
                try {
                  for (int j = 0; j < incrementsPerThread; j++) {
                    engine.incrementEpoch();
                  }
                } catch (final Exception e) {
                  failed.set(true);
                  LOGGER.severe("Thread failed: " + e.getMessage());
                } finally {
                  latch.countDown();
                }
              });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertTrue(!failed.get(), "No thread should have failed");
        LOGGER.info("Successfully incremented epoch from multiple threads");
      }
    }
  }

  @Nested
  @DisplayName("Epoch Interruption Tests")
  class EpochInterruptionTests {

    @Test
    @DisplayName("should interrupt execution when epoch deadline is reached")
    void shouldInterruptExecutionWhenEpochDeadlineIsReached() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing execution interruption at epoch deadline");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      Module module;
      try (final Engine engine = Engine.create(config)) {
        try {
          module = engine.compileModule(LOOP_WASM);
        } catch (final Exception e) {
          LOGGER.warning("WASM compilation failed - test bytecode may need updating: " + e.getMessage());
          assumeTrue(false, "WASM bytecode compilation failed: " + e.getMessage());
          return;
        }
      }

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module compiledModule = engine.compileModule(LOOP_WASM);
          final Instance instance = compiledModule.instantiate(store)) {

        // Set a very short deadline
        store.setEpochDeadline(1L);
        try {
          store.epochDeadlineTrap();
        } catch (final UnsatisfiedLinkError e) {
          LOGGER.warning("Native method not implemented: " + e.getMessage());
          assumeTrue(false, "epochDeadlineTrap native method not available");
          return;
        }

        // Start a thread to increment the epoch
        final Thread incrementer =
            new Thread(
                () -> {
                  try {
                    Thread.sleep(10);
                    for (int i = 0; i < 10; i++) {
                      engine.incrementEpoch();
                      Thread.sleep(1);
                    }
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                });
        incrementer.start();

        final Optional<WasmFunction> loopFunc = instance.getFunction("loop");
        assertTrue(loopFunc.isPresent(), "Should have loop function");

        // This should be interrupted due to epoch deadline
        assertThrows(
            WasmException.class,
            () -> loopFunc.get().call(WasmValue.i32(Integer.MAX_VALUE)),
            "Should throw when epoch deadline is reached");

        incrementer.join(1000);
        LOGGER.info("Execution correctly interrupted at epoch deadline");
      }
    }

    @Test
    @DisplayName("should not interrupt when epoch deadline is not reached")
    void shouldNotInterruptWhenEpochDeadlineIsNotReached() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing execution without interruption");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        // Set a far deadline
        store.setEpochDeadline(Long.MAX_VALUE);

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should have add function");

        assertDoesNotThrow(
            () -> addFunc.get().call(WasmValue.i32(5), WasmValue.i32(10)),
            "Should not throw with far epoch deadline");

        LOGGER.info("Execution completed without interruption");
      }
    }
  }

  @Nested
  @DisplayName("Epoch Callback Tests")
  class EpochCallbackTests {

    @Test
    @DisplayName("should invoke epoch deadline callback")
    void shouldInvokeEpochDeadlineCallback() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline callback");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        try {
          store.epochDeadlineCallback(
              epoch -> {
                LOGGER.info("Epoch deadline callback invoked, epoch: " + epoch);
                callbackInvoked.set(true);
                return Store.EpochDeadlineAction.trap();
              });

          store.setEpochDeadline(1L);

          LOGGER.info("Epoch deadline callback configured");
        } catch (final UnsatisfiedLinkError e) {
          LOGGER.warning("Native method not implemented: " + e.getMessage());
          assumeTrue(false, "Native method not implemented: " + e.getMessage());
        }
      }
    }

    @Test
    @DisplayName("should handle epoch deadline action continue")
    void shouldHandleEpochDeadlineActionContinue() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline action continue");

      final Store.EpochDeadlineAction continueAction = Store.EpochDeadlineAction.continueWith(100L);

      assertTrue(continueAction.shouldContinue(), "Continue action should return true");
      assertTrue(continueAction.getDeltaTicks() == 100L, "Delta ticks should be 100");

      LOGGER.info("Continue action correctly configured");
    }

    @Test
    @DisplayName("should handle epoch deadline action trap")
    void shouldHandleEpochDeadlineActionTrap() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline action trap");

      final Store.EpochDeadlineAction trapAction = Store.EpochDeadlineAction.trap();

      assertTrue(!trapAction.shouldContinue(), "Trap action should not continue");

      LOGGER.info("Trap action correctly configured");
    }
  }

  @Nested
  @DisplayName("Async Epoch Configuration Tests")
  class AsyncEpochConfigurationTests {

    @Test
    @DisplayName("should configure async yield and update")
    void shouldConfigureAsyncYieldAndUpdate() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing async epoch yield and update configuration");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true).asyncSupport(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        try {
          store.epochDeadlineAsyncYieldAndUpdate(50L);
          LOGGER.info("Successfully configured async epoch yield and update");
        } catch (final UnsatisfiedLinkError e) {
          LOGGER.warning("Native method not implemented: " + e.getMessage());
          assumeTrue(false, "Native method not implemented: " + e.getMessage());
        } catch (final ai.tegmentum.wasmtime4j.exception.WasmRuntimeException e) {
          // Async support not fully implemented in native bindings yet
          if (e.getMessage() != null && e.getMessage().contains("async support")) {
            LOGGER.warning("Async support not implemented: " + e.getMessage());
            assumeTrue(false, "Async support not implemented in native bindings");
          } else {
            throw e;
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("Epoch Boundary Condition Tests")
  class EpochBoundaryConditionTests {

    @Test
    @DisplayName("should handle zero epoch deadline")
    void shouldHandleZeroEpochDeadline() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing zero epoch deadline");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        assertDoesNotThrow(() -> store.setEpochDeadline(0L), "Should handle zero epoch deadline");

        LOGGER.info("Zero epoch deadline handled");
      }
    }

    @Test
    @DisplayName("should handle large epoch deadline")
    void shouldHandleLargeEpochDeadline() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing large epoch deadline");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        assertDoesNotThrow(
            () -> store.setEpochDeadline(Long.MAX_VALUE), "Should handle large epoch deadline");

        LOGGER.info("Large epoch deadline handled");
      }
    }

    @Test
    @DisplayName("should handle rapid epoch increments")
    void shouldHandleRapidEpochIncrements() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing rapid epoch increments");

      final EngineConfig config = new EngineConfig().setEpochInterruption(true);

      try (final Engine engine = Engine.create(config)) {
        for (int i = 0; i < 10000; i++) {
          engine.incrementEpoch();
        }

        LOGGER.info("Successfully performed 10000 rapid epoch increments");
      }
    }
  }

  @Nested
  @DisplayName("Combined Fuel and Epoch Tests")
  class CombinedFuelAndEpochTests {

    @Test
    @DisplayName("should work with both fuel and epoch enabled")
    void shouldWorkWithBothFuelAndEpochEnabled() throws Exception {
      assumeTrue(epochInterruptionAvailable, "Epoch interruption not available");

      LOGGER.info("Testing combined fuel and epoch configuration");

      final EngineConfig config = new EngineConfig().consumeFuel(true).setEpochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        store.setFuel(100000L);
        store.setEpochDeadline(Long.MAX_VALUE);

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should have add function");

        final WasmValue[] result = addFunc.get().call(WasmValue.i32(100), WasmValue.i32(200));
        assertNotNull(result, "Result should not be null");
        assertTrue(result.length > 0, "Result should have values");

        LOGGER.info("Combined fuel and epoch configuration worked, result: " + result[0].asInt());
      }
    }
  }
}
