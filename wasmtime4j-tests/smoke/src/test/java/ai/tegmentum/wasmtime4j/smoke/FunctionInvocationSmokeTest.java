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
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for WebAssembly function invocation.
 *
 * <p>Validates the full compile-instantiate-invoke path with a simple add function.
 */
@DisplayName("Function Invocation Smoke Test")
public final class FunctionInvocationSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(FunctionInvocationSmokeTest.class.getName());

  private static final String ADD_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add
        )
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "function invocation returns correct result for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("add function should return correct result")
  void addFunctionShouldReturnCorrectResult(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Testing function invocation with runtime: " + runtime);
    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Module module = engine.compileWat(ADD_WAT);
        final Instance instance = module.instantiate(store)) {

      final WasmValue[] results = instance.callFunction("add", WasmValue.i32(3), WasmValue.i32(4));

      assertNotNull(results, "Function results should not be null");
      assertEquals(1, results.length, "Should return exactly one result");
      assertEquals(7, results[0].asInt(), "3 + 4 should equal 7");
      LOGGER.info("Function invocation succeeded: add(3, 4) = " + results[0].asInt());
    }
  }
}
