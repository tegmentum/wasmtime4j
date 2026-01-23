/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wasmtime.generated.tailcall;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: tail-call/loop-across-modules.wast
 *
 * <p>Tests tail call optimization across module boundaries. Module A exports function f that does
 * return_call_indirect through a table. Module B imports the table and function f, exports function
 * g that does return_call to f, and populates the table with g in its start function. This creates
 * a tail-call loop: A.f -> indirect -> B.g -> A.f
 *
 * <p>This test verifies that tail call chains across Wasm modules have O(1) stack usage.
 *
 * <p>Requires: tail_call = true, reference_types = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/tail-call/loop-across-modules.wast
 */
public final class LoopAcrossModulesTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = LoopAcrossModulesTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("tail call loop across modules - O(1) stack usage")
  public void testTailCallLoopAcrossModules(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Load and instantiate module A first
      final String moduleAWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/tailcall/LoopAcrossModulesTest_module1.wat");
      runner.compileAndInstantiate(moduleAWat);

      // Register module A's exports under name "A"
      runner.registerModule("A");

      // Load and instantiate module B which imports from A
      final String moduleBWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/tailcall/LoopAcrossModulesTest_module2.wat");
      runner.compileAndInstantiate(moduleBWat);

      // Call g with a large count - if tail calls work properly, this won't overflow the stack
      // The loop: g(100000) -> f(100000) -> indirect(g, 99999) -> g(99999) -> f(99999) -> ...
      // Eventually f(0) returns 42
      runner.assertReturn("g", new WasmValue[] {WasmValue.i32(42)}, WasmValue.i32(100000));
    }
  }
}
