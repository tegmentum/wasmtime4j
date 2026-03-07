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
 * Smoke test for the Linker subsystem.
 *
 * <p>Validates that functions can be defined in a Linker, used to instantiate a module with
 * imports, and called through the module's exported function.
 */
@DisplayName("Linker Smoke Test")
public final class LinkerSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(LinkerSmokeTest.class.getName());

  private static final String IMPORT_MODULE_WAT =
      """
      (module
        (import "math" "square" (func $square (param i32) (result i32)))
        (func (export "compute_square") (param i32) (result i32)
          local.get 0
          call $square
        )
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "linker instantiation and call for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("linked function should be callable through exported wrapper")
  void linkedFunctionShouldBeCallableThroughExportedWrapper(final RuntimeType runtime)
      throws Exception {
    setRuntime(runtime);

    LOGGER.info("Testing linker with runtime: " + runtime);

    final HostFunction squareFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * params[0].asInt()));

    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Module module = engine.compileWat(IMPORT_MODULE_WAT);
        final Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("math", "square", funcType, squareFunc);
      LOGGER.info("Host function 'square' defined in linker under module 'math'");

      try (final Instance instance = linker.instantiate(store, module)) {
        final WasmValue[] results = instance.callFunction("compute_square", WasmValue.i32(9));

        assertNotNull(results, "Function results should not be null");
        assertEquals(1, results.length, "Should return exactly one result");
        assertEquals(81, results[0].asInt(), "9 squared should equal 81");
        LOGGER.info("Linker call succeeded: compute_square(9) = " + results[0].asInt());
      }
    }
  }
}
