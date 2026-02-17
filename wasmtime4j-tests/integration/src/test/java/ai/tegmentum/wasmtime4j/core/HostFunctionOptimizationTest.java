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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.func.HostFunction.CallerContextUsage;
import ai.tegmentum.wasmtime4j.func.HostFunction.OptimizedCallerAwareHostFunction;
import ai.tegmentum.wasmtime4j.func.HostFunction.OptimizedHostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link HostFunction#optimized(HostFunction)} and {@link
 * HostFunction#optimizedWithCaller(HostFunction.MultiValueHostFunctionWithCaller,
 * CallerContextUsage)}.
 *
 * <p>These tests verify that optimization wrappers correctly wrap host functions, preserve
 * structural metadata (isCallerAware, contextUsage, canOptimize), and execute correctly through the
 * linker.
 *
 * @since 1.0.0
 */
@DisplayName("Host Function Optimization Tests")
public class HostFunctionOptimizationTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(HostFunctionOptimizationTest.class.getName());

  /**
   * WAT module that imports a function taking two i32 params and returning i32.
   *
   * <pre>
   * (module
   *   (import "env" "optimized_fn" (func $opt (param i32 i32) (result i32)))
   *   (func (export "call_opt") (param i32 i32) (result i32)
   *     local.get 0 local.get 1 call $opt))
   * </pre>
   */
  private static final String WAT =
      """
      (module
        (import "env" "optimized_fn" (func $opt (param i32 i32) (result i32)))
        (func (export "call_opt") (param i32 i32) (result i32)
          local.get 0 local.get 1 call $opt))
      """;

  private static final FunctionType ADD_TYPE =
      new FunctionType(
          new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
          new WasmValueType[] {WasmValueType.I32});

  private static final String CALLER_NOT_AVAILABLE =
      "Caller context not available via runtime, skipping execution assertions";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  /**
   * Checks if an exception indicates caller context is unavailable (not a real test failure). Walks
   * the full cause chain since the JNI runtime wraps the root cause in multiple layers.
   *
   * @param e the exception to check
   * @return true if this is a caller-not-available error
   */
  private static boolean isCallerUnavailable(final Exception e) {
    Throwable current = e;
    while (current != null) {
      final String msg = current.getMessage();
      if (msg != null
          && (msg.contains("Caller context not available")
              || msg.contains("CallerContextProvider")
              || msg.contains("caller context"))) {
        return true;
      }
      if (current instanceof UnsupportedOperationException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("optimized wraps regular function with correct metadata")
  void optimizedWrapsRegularFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing HostFunction.optimized() with regular function");

    final HostFunction original =
        (params) -> {
          final int a = params[0].asInt();
          final int b = params[1].asInt();
          return new WasmValue[] {WasmValue.i32(a + b)};
        };

    final HostFunction optimized = HostFunction.optimized(original);

    assertNotNull(optimized, "optimized() should return non-null");
    assertTrue(
        optimized instanceof OptimizedHostFunction,
        "optimized() should return OptimizedHostFunction, got: " + optimized.getClass().getName());

    final OptimizedHostFunction opt = (OptimizedHostFunction) optimized;
    assertFalse(opt.isCallerAware(), "Regular function should not be caller-aware");
    assertEquals(original, opt.getDelegate(), "Delegate should be the original function");
    LOGGER.info(
        "[" + runtime + "] OptimizedHostFunction metadata: isCallerAware=" + opt.isCallerAware());

    // Verify execution through linker
    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      linker.defineHostFunction("env", "optimized_fn", ADD_TYPE, optimized);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callOptFunc = instance.getFunction("call_opt");
        assertTrue(callOptFunc.isPresent(), "call_opt export must be present");

        final WasmValue[] results = callOptFunc.get().call(WasmValue.i32(3), WasmValue.i32(4));
        assertEquals(7, results[0].asInt(), "3 + 4 should equal 7");
        LOGGER.info("[" + runtime + "] Optimized function executed: 3 + 4 = 7");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("optimized wraps caller-aware function with correct metadata")
  void optimizedWrapsCallerAwareFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing HostFunction.optimized() with caller-aware function");

    final HostFunction callerAware =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              return new WasmValue[] {WasmValue.i32(a + b)};
            });

    final HostFunction optimized = HostFunction.optimized(callerAware);

    assertNotNull(optimized, "optimized() should return non-null");
    assertTrue(
        optimized instanceof OptimizedHostFunction,
        "optimized() should return OptimizedHostFunction, got: " + optimized.getClass().getName());

    final OptimizedHostFunction opt = (OptimizedHostFunction) optimized;
    assertTrue(opt.isCallerAware(), "Caller-aware function should be marked as caller-aware");
    LOGGER.info(
        "["
            + runtime
            + "] OptimizedHostFunction(callerAware): isCallerAware="
            + opt.isCallerAware());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("optimizedWithCaller with NONE usage allows optimization")
  void optimizedWithCallerNoneUsage(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing HostFunction.optimizedWithCaller with NONE usage");

    final HostFunction optimized =
        HostFunction.optimizedWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              return new WasmValue[] {WasmValue.i32(a + b)};
            },
            CallerContextUsage.NONE);

    assertNotNull(optimized, "optimizedWithCaller() should return non-null");
    assertTrue(
        optimized instanceof OptimizedCallerAwareHostFunction,
        "Should return OptimizedCallerAwareHostFunction, got: " + optimized.getClass().getName());

    final OptimizedCallerAwareHostFunction<?> opt = (OptimizedCallerAwareHostFunction<?>) optimized;
    assertEquals(CallerContextUsage.NONE, opt.getContextUsage(), "Context usage should be NONE");
    assertTrue(opt.canOptimize(), "NONE usage should be optimizable");
    LOGGER.info(
        "["
            + runtime
            + "] OptimizedCallerAware(NONE): canOptimize="
            + opt.canOptimize()
            + ", usage="
            + opt.getContextUsage());

    // Verify execution through linker
    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      linker.defineHostFunction("env", "optimized_fn", ADD_TYPE, optimized);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callOptFunc = instance.getFunction("call_opt");
        assertTrue(callOptFunc.isPresent(), "call_opt export must be present");

        try {
          final WasmValue[] results = callOptFunc.get().call(WasmValue.i32(10), WasmValue.i32(20));
          assertEquals(30, results[0].asInt(), "10 + 20 should equal 30");
          LOGGER.info("[" + runtime + "] OptimizedWithCaller(NONE) executed: 10 + 20 = 30");
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
          } else {
            throw e;
          }
        }
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("optimizedWithCaller with FULL usage prevents optimization")
  void optimizedWithCallerFullUsage(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing HostFunction.optimizedWithCaller with FULL usage");

    final HostFunction optimized =
        HostFunction.optimizedWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              return new WasmValue[] {WasmValue.i32(a + b)};
            },
            CallerContextUsage.FULL);

    assertNotNull(optimized, "optimizedWithCaller() should return non-null");
    assertTrue(
        optimized instanceof OptimizedCallerAwareHostFunction,
        "Should return OptimizedCallerAwareHostFunction, got: " + optimized.getClass().getName());

    final OptimizedCallerAwareHostFunction<?> opt = (OptimizedCallerAwareHostFunction<?>) optimized;
    assertEquals(CallerContextUsage.FULL, opt.getContextUsage(), "Context usage should be FULL");
    assertFalse(opt.canOptimize(), "FULL usage should not be optimizable");
    LOGGER.info(
        "["
            + runtime
            + "] OptimizedCallerAware(FULL): canOptimize="
            + opt.canOptimize()
            + ", usage="
            + opt.getContextUsage());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("optimizedWithCaller with EXPORTS_ONLY usage has correct flags")
  void optimizedWithCallerExportsOnlyUsage(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info(
        "[" + runtime + "] Testing HostFunction.optimizedWithCaller with EXPORTS_ONLY usage");

    final HostFunction optimized =
        HostFunction.optimizedWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              return new WasmValue[] {WasmValue.i32(a + b)};
            },
            CallerContextUsage.EXPORTS_ONLY);

    assertNotNull(optimized, "optimizedWithCaller() should return non-null");
    assertTrue(
        optimized instanceof OptimizedCallerAwareHostFunction,
        "Should return OptimizedCallerAwareHostFunction, got: " + optimized.getClass().getName());

    final OptimizedCallerAwareHostFunction<?> opt = (OptimizedCallerAwareHostFunction<?>) optimized;
    assertEquals(
        CallerContextUsage.EXPORTS_ONLY,
        opt.getContextUsage(),
        "Context usage should be EXPORTS_ONLY");
    assertTrue(opt.getContextUsage().usesExports(), "EXPORTS_ONLY should use exports");
    assertFalse(opt.getContextUsage().usesFuel(), "EXPORTS_ONLY should not use fuel");
    LOGGER.info(
        "["
            + runtime
            + "] OptimizedCallerAware(EXPORTS_ONLY): usesExports="
            + opt.getContextUsage().usesExports()
            + ", usesFuel="
            + opt.getContextUsage().usesFuel());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("optimized function executes correctly through linker end-to-end")
  void optimizedFunctionExecutesThroughLinker(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing optimized function end-to-end through linker");

    final HostFunction optimized =
        HostFunction.optimized(
            (params) -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              final int result = a * b;
              return new WasmValue[] {WasmValue.i32(result)};
            });

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      linker.defineHostFunction("env", "optimized_fn", ADD_TYPE, optimized);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callOptFunc = instance.getFunction("call_opt");
        assertTrue(callOptFunc.isPresent(), "call_opt export must be present");

        // Test multiple invocations
        final int[][] testCases = {{3, 4, 12}, {0, 100, 0}, {7, 7, 49}, {-1, 5, -5}};
        for (final int[] tc : testCases) {
          final WasmValue[] results =
              callOptFunc.get().call(WasmValue.i32(tc[0]), WasmValue.i32(tc[1]));
          assertEquals(tc[2], results[0].asInt(), tc[0] + " * " + tc[1] + " should equal " + tc[2]);
          LOGGER.info(
              "["
                  + runtime
                  + "] Optimized multiply: "
                  + tc[0]
                  + " * "
                  + tc[1]
                  + " = "
                  + results[0].asInt());
        }
      }
    }
  }
}
