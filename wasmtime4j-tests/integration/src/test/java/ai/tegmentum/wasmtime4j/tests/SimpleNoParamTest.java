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
package ai.tegmentum.wasmtime4j.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for calling WebAssembly functions with no parameters.
 *
 * <p>Validates that functions without parameters can be called correctly and return expected
 * values.
 */
public class SimpleNoParamTest extends DualRuntimeTest {

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Call function with no parameters")
  public void testNoParamFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (func (export "get42") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();

    try {
      final ai.tegmentum.wasmtime4j.Module module = engine.compileWat(wat);
      assertNotNull(module, "Module should not be null");

      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance should not be null");

      final WasmValue[] results = instance.callFunction("get42");
      assertNotNull(results, "Function results should not be null");
      assertEquals(1, results.length, "Function should return 1 value");
      assertEquals(42, results[0].asInt(), "Should return 42");

      System.out.println("✅ No-param function works!");

      instance.close();
      store.close();
    } finally {
      engine.close();
    }
  }
}
