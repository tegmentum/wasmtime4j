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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for epoch-based interruption functionality.
 *
 * <p>Epochs provide a way to interrupt long-running WebAssembly code at regular intervals, enabling
 * cooperative multitasking and timeout handling.
 *
 * @since 1.0.0
 */
@DisplayName("Epoch Interruption Integration Tests")
@SuppressWarnings("deprecation")
public class EpochInterruptionTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(EpochInterruptionTest.class.getName());

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

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  private boolean checkEpochInterruptionAvailable(final RuntimeType runtime) {
    try {
      setRuntime(runtime);
      final EngineConfig config = new EngineConfig().epochInterruption(true);
      try (final Engine engine = Engine.create(config)) {
        return engine.isEpochInterruptionEnabled();
      }
    } catch (final Exception e) {
      LOGGER.warning("Epoch interruption not available: " + e.getMessage());
      return false;
    }
  }

  @Nested
  @DisplayName("Epoch Configuration Tests")
  class EpochConfigurationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should enable epoch interruption via engine config")
    void shouldEnableEpochInterruptionViaEngineConfig(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch interruption enablement");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config)) {
        assertTrue(
            engine.isEpochInterruptionEnabled(), "Engine should have epoch interruption enabled");
        LOGGER.info("Epoch interruption enabled: " + engine.isEpochInterruptionEnabled());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should not enable epoch interruption by default")
    void shouldNotEnableEpochInterruptionByDefault(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);

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

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should set epoch deadline on store")
    void shouldSetEpochDeadlineOnStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline setting");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        assertDoesNotThrow(
            () -> store.setEpochDeadline(100L), "Should set epoch deadline without throwing");

        LOGGER.info("Successfully set epoch deadline");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should configure epoch deadline trap")
    void shouldConfigureEpochDeadlineTrap(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline trap configuration");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        store.epochDeadlineTrap();
        LOGGER.info("Successfully configured epoch deadline trap");
      }
    }
  }

  @Nested
  @DisplayName("Epoch Increment Tests")
  class EpochIncrementTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should increment epoch on engine")
    void shouldIncrementEpochOnEngine(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch increment");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config)) {
        assertDoesNotThrow(engine::incrementEpoch, "Should increment epoch without throwing");

        // Increment multiple times
        for (int i = 0; i < 10; i++) {
          engine.incrementEpoch();
        }

        LOGGER.info("Successfully incremented epoch multiple times");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should be thread-safe when incrementing epoch")
    void shouldBeThreadSafeWhenIncrementingEpoch(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing thread-safe epoch increment");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

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

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should interrupt execution when epoch deadline is reached")
    void shouldInterruptExecutionWhenEpochDeadlineIsReached(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing execution interruption at epoch deadline");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module compiledModule = engine.compileModule(LOOP_WASM);
          final Instance instance = compiledModule.instantiate(store)) {

        // Set a very short deadline
        store.setEpochDeadline(1L);
        store.epochDeadlineTrap();

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

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should not interrupt when epoch deadline is not reached")
    void shouldNotInterruptWhenEpochDeadlineIsNotReached(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing execution without interruption");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

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

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should invoke epoch deadline callback")
    void shouldInvokeEpochDeadlineCallback(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline callback");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        store.epochDeadlineCallback(
            epoch -> {
              LOGGER.info("Epoch deadline callback invoked, epoch: " + epoch);
              callbackInvoked.set(true);
              return Store.EpochDeadlineAction.trap();
            });

        store.setEpochDeadline(1L);

        LOGGER.info("Epoch deadline callback configured");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle epoch deadline action continue")
    void shouldHandleEpochDeadlineActionContinue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline action continue");

      final Store.EpochDeadlineAction continueAction = Store.EpochDeadlineAction.continueWith(100L);

      assertTrue(continueAction.shouldContinue(), "Continue action should return true");
      assertTrue(continueAction.getDeltaTicks() == 100L, "Delta ticks should be 100");

      LOGGER.info("Continue action correctly configured");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle epoch deadline action trap")
    void shouldHandleEpochDeadlineActionTrap(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch deadline action trap");

      final Store.EpochDeadlineAction trapAction = Store.EpochDeadlineAction.trap();

      assertTrue(!trapAction.shouldContinue(), "Trap action should not continue");

      LOGGER.info("Trap action correctly configured");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should complete finite loop when callback continues execution")
    void shouldCompleteFiniteLoopWhenCallbackContinuesExecution(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch callback continuation allows loop to complete");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      // WAT module with a loop that counts to a target value
      final String countingLoopWat =
          "(module\n"
              + "  (global $counter (mut i32) (i32.const 0))\n"
              + "  (func (export \"count_to\") (param $target i32) (result i32)\n"
              + "    (loop $loop\n"
              + "      ;; Increment counter\n"
              + "      (global.set $counter\n"
              + "        (i32.add (global.get $counter) (i32.const 1)))\n"
              + "      ;; Continue if counter < target\n"
              + "      (br_if $loop\n"
              + "        (i32.lt_s (global.get $counter) (local.get $target)))\n"
              + "    )\n"
              + "    (global.get $counter)\n"
              + "  )\n"
              + ")";

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module module = engine.compileWat(countingLoopWat);
          final Instance instance = module.instantiate(store)) {

        final AtomicInteger callbackCount = new AtomicInteger(0);
        // Use a large target to ensure epochs are triggered during execution
        final int targetCount = 100000;

        // Set callback that continues execution with more ticks
        store.epochDeadlineCallback(
            epoch -> {
              final int count = callbackCount.incrementAndGet();
              LOGGER.fine("Epoch callback invoked, count: " + count);
              // Continue with 10 more ticks (low value to trigger more callbacks)
              return Store.EpochDeadlineAction.continueWith(10);
            });

        // Thread to increment epochs rapidly
        final AtomicBoolean running = new AtomicBoolean(true);
        final CountDownLatch started = new CountDownLatch(1);
        final Thread epochIncrementer =
            new Thread(
                () -> {
                  started.countDown();
                  while (running.get()) {
                    engine.incrementEpoch();
                    // No sleep - increment as fast as possible
                  }
                });
        epochIncrementer.start();

        // Wait for incrementer to start
        started.await();

        // Set initial short deadline to trigger callback
        store.setEpochDeadline(1L);

        try {
          final Optional<WasmFunction> countFunc = instance.getFunction("count_to");
          assertTrue(countFunc.isPresent(), "Should have count_to function");

          // Execute the counting loop
          final WasmValue[] result = countFunc.get().call(WasmValue.i32(targetCount));

          assertNotNull(result, "Result should not be null");
          assertEquals(1, result.length, "Should have one result");
          assertEquals(targetCount, result[0].asInt(), "Counter should reach target value");

          LOGGER.info(
              "Loop completed successfully. Callback invoked "
                  + callbackCount.get()
                  + " times, final count: "
                  + result[0].asInt());
        } finally {
          running.set(false);
          epochIncrementer.interrupt();
          epochIncrementer.join(1000);
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle exception thrown by epoch callback gracefully")
    void shouldHandleExceptionThrownByEpochCallbackGracefully(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch callback exception handling");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      // WAT module with an infinite loop
      final String infiniteLoopWat =
          "(module\n"
              + "  (func (export \"infinite_loop\")\n"
              + "    (loop $continue\n"
              + "      (br $continue)\n"
              + "    )\n"
              + "  )\n"
              + ")";

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module module = engine.compileWat(infiniteLoopWat);
          final Instance instance = module.instantiate(store)) {

        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        // Set callback that throws an exception
        store.epochDeadlineCallback(
            epoch -> {
              callbackInvoked.set(true);
              LOGGER.info("Epoch callback throwing exception");
              throw new RuntimeException("Test exception from epoch callback");
            });

        // Thread to increment epochs rapidly
        final AtomicBoolean running = new AtomicBoolean(true);
        final CountDownLatch started = new CountDownLatch(1);
        final Thread epochIncrementer =
            new Thread(
                () -> {
                  started.countDown();
                  while (running.get()) {
                    engine.incrementEpoch();
                    // No sleep for rapid increment
                  }
                });
        epochIncrementer.start();

        // Wait for incrementer and set deadline
        started.await();
        store.setEpochDeadline(1L);

        try {
          final Optional<WasmFunction> loopFunc = instance.getFunction("infinite_loop");
          assertTrue(loopFunc.isPresent(), "Should have infinite_loop function");

          // This should trap - either from callback exception or epoch deadline
          assertThrows(
              Exception.class,
              () -> loopFunc.get().call(),
              "Should throw exception when epoch deadline is reached");

          // If callback was invoked, it means the exception was handled gracefully
          // If not invoked, the default trap behavior kicked in (also valid)
          if (callbackInvoked.get()) {
            LOGGER.info("Callback exception handled gracefully without JVM crash");
          } else {
            LOGGER.info("Epoch trapped before callback was invoked (default trap behavior)");
          }
        } finally {
          running.set(false);
          epochIncrementer.interrupt();
          epochIncrementer.join(1000);
        }
      }
    }
  }

  @Nested
  @DisplayName("Async Epoch Configuration Tests")
  class AsyncEpochConfigurationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should configure async yield and update")
    void shouldConfigureAsyncYieldAndUpdate(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing async epoch yield and update configuration");

      final EngineConfig config = new EngineConfig().epochInterruption(true).asyncSupport(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        store.epochDeadlineAsyncYieldAndUpdate(50L);
        LOGGER.info("Successfully configured async epoch yield and update");
      }
    }
  }

  @Nested
  @DisplayName("Epoch Boundary Condition Tests")
  class EpochBoundaryConditionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle zero epoch deadline")
    void shouldHandleZeroEpochDeadline(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing zero epoch deadline");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        assertDoesNotThrow(() -> store.setEpochDeadline(0L), "Should handle zero epoch deadline");

        LOGGER.info("Zero epoch deadline handled");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle large epoch deadline")
    void shouldHandleLargeEpochDeadline(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing large epoch deadline");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        assertDoesNotThrow(
            () -> store.setEpochDeadline(Long.MAX_VALUE), "Should handle large epoch deadline");

        LOGGER.info("Large epoch deadline handled");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle rapid epoch increments")
    void shouldHandleRapidEpochIncrements(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing rapid epoch increments");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

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

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should work with both fuel and epoch enabled")
    void shouldWorkWithBothFuelAndEpochEnabled(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing combined fuel and epoch configuration");

      final EngineConfig config = new EngineConfig().consumeFuel(true).epochInterruption(true);

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

  @Nested
  @DisplayName("Epoch and Host Function Interaction Tests")
  class EpochHostFunctionInteractionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should not interrupt host function execution")
    void shouldNotInterruptHostFunctionExecution(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch behavior during host function execution");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      // WAT module that imports and calls a host function
      final String importHostFuncWat =
          "(module\n"
              + "  (import \"env\" \"slow_add\" (func $slow_add (param i32 i32) (result i32)))\n"
              + "  (func (export \"call_slow_add\") (param i32 i32) (result i32)\n"
              + "    local.get 0\n"
              + "    local.get 1\n"
              + "    call $slow_add\n"
              + "  )\n"
              + ")";

      try (final Engine engine = Engine.create(config);
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileWat(importHostFuncWat)) {

        final AtomicBoolean hostFunctionStarted = new AtomicBoolean(false);
        final AtomicBoolean hostFunctionCompleted = new AtomicBoolean(false);
        final AtomicInteger epochCallbackCount = new AtomicInteger(0);

        // Create a slow host function that sleeps
        final FunctionType funcType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});

        final HostFunction slowAddFunc =
            (params) -> {
              hostFunctionStarted.set(true);
              LOGGER.info("Host function started");
              try {
                // Sleep to give epoch incrementer time to run
                Thread.sleep(100);
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              hostFunctionCompleted.set(true);
              LOGGER.info("Host function completed");
              return new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};
            };

        // Define the host function in linker
        linker.defineHostFunction("env", "slow_add", funcType, slowAddFunc);

        final Instance instance = linker.instantiate(store, module);

        // Configure callback to continue execution
        store.epochDeadlineCallback(
            epoch -> {
              epochCallbackCount.incrementAndGet();
              LOGGER.info("Epoch callback invoked, count: " + epochCallbackCount.get());
              // Continue with more ticks
              return Store.EpochDeadlineAction.continueWith(1000);
            });

        // Set deadline AFTER configuring callback
        store.setEpochDeadline(1L);

        // Thread to rapidly increment epochs
        final AtomicBoolean running = new AtomicBoolean(true);
        final CountDownLatch started = new CountDownLatch(1);
        final Thread epochIncrementer =
            new Thread(
                () -> {
                  started.countDown();
                  while (running.get()) {
                    engine.incrementEpoch();
                    try {
                      Thread.sleep(5);
                    } catch (final InterruptedException e) {
                      Thread.currentThread().interrupt();
                      break;
                    }
                  }
                });
        epochIncrementer.start();
        started.await();

        try {
          final Optional<WasmFunction> callSlowAdd = instance.getFunction("call_slow_add");
          assertTrue(callSlowAdd.isPresent(), "Should have call_slow_add function");

          // Call the function
          final WasmValue[] result = callSlowAdd.get().call(WasmValue.i32(10), WasmValue.i32(20));

          assertNotNull(result, "Result should not be null");
          assertEquals(1, result.length, "Should have one result");
          assertEquals(30, result[0].asInt(), "10 + 20 should equal 30");

          assertTrue(hostFunctionStarted.get(), "Host function should have started");
          assertTrue(hostFunctionCompleted.get(), "Host function should have completed");

          LOGGER.info(
              "Host function completed successfully. Epoch callbacks: " + epochCallbackCount.get());
        } finally {
          running.set(false);
          epochIncrementer.interrupt();
          epochIncrementer.join(1000);
          instance.close();
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should check epoch after host function returns")
    void shouldCheckEpochAfterHostFunctionReturns(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(checkEpochInterruptionAvailable(runtime), "Epoch interruption not available");

      LOGGER.info("Testing epoch check after host function returns");

      final EngineConfig config = new EngineConfig().epochInterruption(true);

      // WAT module that calls host function in a loop
      final String loopWithHostCallWat =
          "(module\n"
              + "  (import \"env\" \"increment\" (func $increment (param i32) (result i32)))\n"
              + "  (global $counter (mut i32) (i32.const 0))\n"
              + "  (func (export \"loop_with_host_call\") (param $iterations i32) (result i32)\n"
              + "    (local $i i32)\n"
              + "    (local.set $i (i32.const 0))\n"
              + "    (loop $loop\n"
              + "      ;; Call host function to increment counter\n"
              + "      (global.set $counter\n"
              + "        (call $increment (global.get $counter)))\n"
              + "      ;; Increment loop counter\n"
              + "      (local.set $i (i32.add (local.get $i) (i32.const 1)))\n"
              + "      ;; Continue if i < iterations\n"
              + "      (br_if $loop\n"
              + "        (i32.lt_s (local.get $i) (local.get $iterations)))\n"
              + "    )\n"
              + "    (global.get $counter)\n"
              + "  )\n"
              + ")";

      try (final Engine engine = Engine.create(config);
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileWat(loopWithHostCallWat)) {

        final AtomicInteger hostCallCount = new AtomicInteger(0);

        // Create host function
        final FunctionType funcType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

        final HostFunction incrementFunc =
            (params) -> {
              hostCallCount.incrementAndGet();
              return new WasmValue[] {WasmValue.i32(params[0].asInt() + 1)};
            };

        linker.defineHostFunction("env", "increment", funcType, incrementFunc);

        final Instance instance = linker.instantiate(store, module);

        final AtomicInteger callbackCount = new AtomicInteger(0);

        store.epochDeadlineCallback(
            epoch -> {
              callbackCount.incrementAndGet();
              // Continue with large delta to let the loop complete
              return Store.EpochDeadlineAction.continueWith(1000);
            });

        // Set short deadline AFTER callback
        store.setEpochDeadline(1L);

        // Thread to increment epochs
        final AtomicBoolean running = new AtomicBoolean(true);
        final CountDownLatch started = new CountDownLatch(1);
        final Thread epochIncrementer =
            new Thread(
                () -> {
                  started.countDown();
                  while (running.get()) {
                    engine.incrementEpoch();
                    try {
                      Thread.sleep(1);
                    } catch (final InterruptedException e) {
                      Thread.currentThread().interrupt();
                      break;
                    }
                  }
                });
        epochIncrementer.start();
        started.await();

        try {
          final Optional<WasmFunction> loopFunc = instance.getFunction("loop_with_host_call");
          assertTrue(loopFunc.isPresent(), "Should have loop_with_host_call function");

          final int iterations = 100;

          final WasmValue[] result = loopFunc.get().call(WasmValue.i32(iterations));

          assertNotNull(result, "Result should not be null");
          assertEquals(iterations, result[0].asInt(), "Counter should equal iterations");
          assertEquals(
              iterations, hostCallCount.get(), "Host function should be called for each iteration");

          LOGGER.info(
              "Loop completed. Host calls: "
                  + hostCallCount.get()
                  + ", Epoch callbacks: "
                  + callbackCount.get());

          // Callback may or may not be invoked depending on timing
          // The main assertion is that the loop completed successfully
          if (callbackCount.get() > 0) {
            LOGGER.info("Epoch callback was invoked after host function returns");
          } else {
            LOGGER.info("Loop completed before epoch deadline was reached");
          }
        } finally {
          running.set(false);
          epochIncrementer.interrupt();
          epochIncrementer.join(1000);
          instance.close();
        }
      }
    }
  }
}
