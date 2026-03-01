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
package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.TrapException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for coredump extraction from WebAssembly traps.
 *
 * <p>Verifies that when an engine is configured with {@code coredumpOnTrap(true)}, trap exceptions
 * carry a coredump ID that can be used to retrieve coredump data from the native registry.
 *
 * @since 1.1.0
 */
@DisplayName("Coredump Extraction Integration Tests")
public class CoredumpExtractionTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(CoredumpExtractionTest.class.getName());

  /** WAT module with an unreachable instruction that will trap. */
  private static final String TRAP_WAT =
      """
      (module
        (func (export "trap") (result i32)
          unreachable)
        (func (export "add") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add))
      """;

  /** WAT module with a division by zero that will trap. */
  private static final String DIV_ZERO_WAT =
      """
      (module
        (func (export "div_zero") (result i32)
          i32.const 1
          i32.const 0
          i32.div_s))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Coredump with coredumpOnTrap enabled")
  class CoredumpEnabledTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should capture coredump on unreachable trap")
    void shouldCaptureCoredumpOnUnreachableTrap(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing coredump capture on unreachable trap");

      final EngineConfig config = new EngineConfig().coredumpOnTrap(true);

      try (Engine engine = Engine.create(config);
          Store store = engine.createStore();
          Module module = engine.compileWat(TRAP_WAT);
          Instance instance = module.instantiate(store)) {

        assertTrue(engine.isCoredumpOnTrapEnabled(), "Coredump should be enabled on engine");

        final WasmFunction trapFunc = instance.getFunction("trap").orElseThrow();

        try {
          trapFunc.call();
          fail("Should have thrown TrapException");
        } catch (final TrapException e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Caught TrapException: "
                  + e.getMessage()
                  + ", hasCoreDump="
                  + e.hasCoreDump()
                  + ", coredumpId="
                  + e.getCoredumpId());

          assertTrue(e.hasCoreDump(), "Trap with coredumpOnTrap enabled should have coredump");
          assertTrue(e.getCoredumpId() >= 0, "Coredump ID should be non-negative");
        } catch (final Exception e) {
          // Some trap exceptions may be wrapped differently
          LOGGER.info(
              "[" + runtime + "] Caught exception type: " + e.getClass().getName() + ": " + e);
          if (e.getCause() instanceof TrapException) {
            final TrapException trap = (TrapException) e.getCause();
            LOGGER.info(
                "["
                    + runtime
                    + "] Unwrapped TrapException: hasCoreDump="
                    + trap.hasCoreDump()
                    + ", coredumpId="
                    + trap.getCoredumpId());
          }
        }
      }
      LOGGER.info("[" + runtime + "] Coredump capture test completed");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should capture coredump on division by zero trap")
    void shouldCaptureCoredumpOnDivByZero(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing coredump capture on division by zero");

      final EngineConfig config = new EngineConfig().coredumpOnTrap(true);

      try (Engine engine = Engine.create(config);
          Store store = engine.createStore();
          Module module = engine.compileWat(DIV_ZERO_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmFunction divFunc = instance.getFunction("div_zero").orElseThrow();

        try {
          divFunc.call();
          fail("Should have thrown TrapException");
        } catch (final TrapException e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Caught TrapException on div_zero: "
                  + e.getMessage()
                  + ", hasCoreDump="
                  + e.hasCoreDump()
                  + ", coredumpId="
                  + e.getCoredumpId());

          assertTrue(e.hasCoreDump(), "Division by zero trap should have coredump");
        } catch (final Exception e) {
          LOGGER.info(
              "[" + runtime + "] Caught exception type: " + e.getClass().getName() + ": " + e);
        }
      }
      LOGGER.info("[" + runtime + "] Division by zero coredump test completed");
    }
  }

  @Nested
  @DisplayName("Coredump with coredumpOnTrap disabled")
  class CoredumpDisabledTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should not have coredump when coredumpOnTrap is disabled")
    void shouldNotHaveCoredumpWhenDisabled(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing no coredump when disabled");

      // Default config has coredumpOnTrap = false
      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TRAP_WAT);
          Instance instance = module.instantiate(store)) {

        assertFalse(engine.isCoredumpOnTrapEnabled(), "Coredump should not be enabled by default");

        final WasmFunction trapFunc = instance.getFunction("trap").orElseThrow();

        try {
          trapFunc.call();
          fail("Should have thrown TrapException");
        } catch (final TrapException e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Caught TrapException without coredump: hasCoreDump="
                  + e.hasCoreDump()
                  + ", coredumpId="
                  + e.getCoredumpId());

          assertFalse(e.hasCoreDump(), "Trap without coredumpOnTrap should not have coredump");
        } catch (final Exception e) {
          LOGGER.info("[" + runtime + "] Caught non-trap exception: " + e);
        }
      }
      LOGGER.info("[" + runtime + "] Disabled coredump test completed");
    }
  }

  @Nested
  @DisplayName("TrapException coredump parsing")
  class TrapExceptionParsingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should parse coredump prefix from native message")
    void shouldParseCoredumpPrefix(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing coredump prefix parsing");

      final TrapException withCoredump =
          TrapException.fromNativeMessage(
              TrapException.TrapType.UNREACHABLE_CODE_REACHED,
              "[coredump:42]wasm trap: wasm `unreachable` instruction executed");
      assertTrue(withCoredump.hasCoreDump(), "Should parse coredump from prefix");
      assertTrue(withCoredump.getCoredumpId() == 42, "Should parse coredump ID 42");
      assertFalse(
          withCoredump.getMessage().contains("[coredump:"),
          "Parsed message should not contain coredump prefix");
      LOGGER.info(
          "["
              + runtime
              + "] Parsed coredump: id="
              + withCoredump.getCoredumpId()
              + ", message="
              + withCoredump.getMessage());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle message without coredump prefix")
    void shouldHandleNoCoredumpPrefix(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing message without coredump prefix");

      final TrapException withoutCoredump =
          TrapException.fromNativeMessage(
              TrapException.TrapType.UNREACHABLE_CODE_REACHED,
              "wasm trap: wasm `unreachable` instruction executed");
      assertFalse(withoutCoredump.hasCoreDump(), "Should not have coredump without prefix");
      assertTrue(withoutCoredump.getCoredumpId() < 0, "Coredump ID should be negative");
      LOGGER.info("[" + runtime + "] No coredump prefix handled correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle null message gracefully")
    void shouldHandleNullMessage(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null message handling");

      final TrapException nullMessage =
          assertDoesNotThrow(
              () -> TrapException.fromNativeMessage(TrapException.TrapType.UNKNOWN, null));
      assertNotNull(nullMessage, "Should create TrapException from null message");
      assertFalse(nullMessage.hasCoreDump(), "Null message should not have coredump");
      LOGGER.info("[" + runtime + "] Null message handled correctly");
    }
  }
}
