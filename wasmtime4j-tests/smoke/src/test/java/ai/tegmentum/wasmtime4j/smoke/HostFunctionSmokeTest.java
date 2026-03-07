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
package ai.tegmentum.wasmtime4j.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for host function definition and invocation.
 *
 * <p>Validates that a host function (Java callback) can be defined, linked into a WebAssembly
 * module, and called through an exported wrapper function.
 */
@DisplayName("Host Function Smoke Test")
public final class HostFunctionSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(HostFunctionSmokeTest.class.getName());

  private static final String HOST_CALL_WAT =
      """
      (module
        (import "env" "multiply" (func $multiply (param i32 i32) (result i32)))
        (func (export "call_multiply") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          call $multiply
        )
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "host function invocation for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("host function should be callable from WASM")
  void hostFunctionShouldBeCallableFromWasm(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Testing host function with runtime: " + runtime);

    final HostFunction multiplyFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * params[1].asInt()));

    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Module module = engine.compileWat(HOST_CALL_WAT);
        final Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "multiply", funcType, multiplyFunc);
      LOGGER.info("Host function 'multiply' defined in linker");

      try (final Instance instance = linker.instantiate(store, module)) {
        final WasmValue[] results =
            instance.callFunction("call_multiply", WasmValue.i32(6), WasmValue.i32(7));

        assertNotNull(results, "Function results should not be null");
        assertEquals(1, results.length, "Should return exactly one result");
        assertEquals(42, results[0].asInt(), "6 * 7 should equal 42");
        LOGGER.info("Host function invocation succeeded: multiply(6, 7) = " + results[0].asInt());
      }
    }
  }
}
