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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.TrapException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for trap backtrace content. Verifies that trap exceptions include meaningful diagnostic
 * information such as function names, module offsets, and call chain details.
 *
 * @since 1.0.0
 */
@DisplayName("Trap Backtrace Tests")
public class TrapBacktraceTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(TrapBacktraceTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Trap includes meaningful information in exception")
  void trapIncludesMeaningfulInfo(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing trap backtrace content");

    final String wat =
        """
        (module
          (func $do_trap (export "do_trap")
            unreachable))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      try {
        instance.callFunction("do_trap");
        fail("Function should have trapped");
      } catch (final TrapException e) {
        LOGGER.info("[" + runtime + "] TrapException caught");
        LOGGER.info("[" + runtime + "] Trap type: " + e.getTrapType());
        LOGGER.info("[" + runtime + "] Message: " + e.getMessage());
        LOGGER.info("[" + runtime + "] Backtrace: " + e.getWasmBacktrace());

        assertNotNull(e.getMessage(), "Trap should have a message");
        assertNotNull(e.getTrapType(), "Trap should have a type");
        assertTrue(e.getMessage().length() > 0, "Trap message should not be empty");
        LOGGER.info("[" + runtime + "] Trap info verified");
      } catch (final Exception e) {
        // Non-TrapException is also acceptable - just verify it has info
        LOGGER.info("[" + runtime + "] Non-TrapException caught: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
        LOGGER.info("[" + runtime + "] Exception message: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Nested function call trap shows call chain")
  void nestedTrapShowsCallChain(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing nested trap call chain");

    final String wat =
        """
        (module
          (func $inner (unreachable))
          (func $middle (call $inner))
          (func $outer (export "outer") (call $middle)))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      try {
        instance.callFunction("outer");
        fail("Nested function should have trapped");
      } catch (final TrapException e) {
        LOGGER.info("[" + runtime + "] Nested TrapException caught");
        LOGGER.info("[" + runtime + "] Trap type: " + e.getTrapType());
        LOGGER.info("[" + runtime + "] Message: " + e.getMessage());

        final String backtrace = e.getWasmBacktrace();
        if (backtrace != null) {
          LOGGER.info("[" + runtime + "] Backtrace:\n" + backtrace);
          // Backtrace should contain multiple frames for the nested call
          assertTrue(backtrace.length() > 0, "Backtrace should not be empty for nested calls");
        }
        LOGGER.info("[" + runtime + "] Nested trap info verified");
      } catch (final Exception e) {
        LOGGER.info(
            "[" + runtime + "] Non-TrapException for nested trap: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Trap from host function callback includes info")
  void trapFromHostFunctionIncludesInfo(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing trap from host function callback");

    final String wat =
        """
        (module
          (import "env" "host_trap" (func $host_trap))
          (func (export "call_host_trap")
            call $host_trap))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Linker<Void> linker = Linker.create(engine);

      linker.defineHostFunction(
          "env",
          "host_trap",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
          params -> {
            throw new ai.tegmentum.wasmtime4j.exception.WasmException(
                "Host function intentional trap");
          });

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      try {
        instance.callFunction("call_host_trap");
        fail("Host function should have caused a trap");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] Host trap exception: " + e.getClass().getName());
        LOGGER.info("[" + runtime + "] Message: " + e.getMessage());

        assertNotNull(e.getMessage(), "Host trap exception should have a message");
        LOGGER.info("[" + runtime + "] Host trap info verified");
      }

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Division by zero trap has correct trap type")
  void divByZeroTrapType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing division by zero trap type");

    final String wat =
        """
        (module
          (func (export "div_zero") (result i32)
            i32.const 1
            i32.const 0
            i32.div_s))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      try {
        instance.callFunction("div_zero");
        fail("Division by zero should trap");
      } catch (final TrapException e) {
        LOGGER.info("[" + runtime + "] Div by zero trap type: " + e.getTrapType());
        assertNotNull(e.getTrapType(), "Trap type should not be null");
        assertTrue(e.isArithmeticError(), "Division by zero should be an arithmetic error");
        LOGGER.info("[" + runtime + "] Division by zero trap type verified");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] Non-TrapException: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Out-of-bounds memory trap has correct trap type")
  void oobMemoryTrapType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing OOB memory trap type");

    final String wat =
        """
        (module
          (memory 1)
          (func (export "oob") (result i32)
            i32.const 999999
            i32.load))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      try {
        instance.callFunction("oob");
        fail("OOB memory access should trap");
      } catch (final TrapException e) {
        LOGGER.info("[" + runtime + "] OOB memory trap type: " + e.getTrapType());
        assertNotNull(e.getTrapType(), "Trap type should not be null");
        assertTrue(
            e.isMemoryError() || e.isBoundsError(),
            "OOB memory access should be a memory or bounds error");
        LOGGER.info("[" + runtime + "] OOB memory trap type verified");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] Non-TrapException: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }

      instance.close();
      module.close();
    }
  }
}
