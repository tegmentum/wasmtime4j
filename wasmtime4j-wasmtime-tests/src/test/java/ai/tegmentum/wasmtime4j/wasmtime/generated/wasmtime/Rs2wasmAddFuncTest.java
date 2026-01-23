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

package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

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
 * Generated test from WAST file: rs2wasm-add-func.wast
 *
 * <p>Simple add function test from a Rust-compiled WebAssembly module. Tests basic function export
 * with memory and table exports.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/rs2wasm-add-func.wast
 */
public final class Rs2wasmAddFuncTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = Rs2wasmAddFuncTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("add function from Rust module")
  public void testAddFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/Rs2wasmAddFuncTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Test the add function
      runner.assertReturn(
          "add", new WasmValue[] {WasmValue.i32(30)}, WasmValue.i32(10), WasmValue.i32(20));

      runner.assertReturn(
          "add", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0), WasmValue.i32(0));

      runner.assertReturn(
          "add", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(0), WasmValue.i32(-1));
    }
  }
}
