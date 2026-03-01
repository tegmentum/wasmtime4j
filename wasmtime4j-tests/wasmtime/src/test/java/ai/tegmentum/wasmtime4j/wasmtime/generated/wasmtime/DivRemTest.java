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
 * Generated test from WAST file: div-rem.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class DivRemTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = DivRemTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("div rem")
  public void testDivRem(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/DivRemTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/DivRemTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "i32.div_s" ( i32.const -1) ( i32.const -1)) ( i32.const 1))
      runner.assertReturn(
          "i32.div_s", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(-1), WasmValue.i32(-1));

      // Compile and instantiate module 2
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/DivRemTest_module2.wat
      final String moduleWat2 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/DivRemTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_return ( invoke "i32.rem_s" ( i32.const 123121) ( i32.const -1)) ( i32.const 0))
      runner.assertReturn(
          "i32.rem_s",
          new WasmValue[] {WasmValue.i32(0)},
          WasmValue.i32(123121),
          WasmValue.i32(-1));

      // Compile and instantiate module 3
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/DivRemTest_module3.wat
      final String moduleWat3 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/DivRemTest_module3.wat");
      runner.compileAndInstantiate(moduleWat3);

      // ( assert_return ( invoke "i64.div_s" ( i64.const -1) ( i64.const -1)) ( i64.const 1))
      runner.assertReturn(
          "i64.div_s", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(-1L), WasmValue.i64(-1L));

      // Compile and instantiate module 4
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/DivRemTest_module4.wat
      final String moduleWat4 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/DivRemTest_module4.wat");
      runner.compileAndInstantiate(moduleWat4);

      // ( assert_return ( invoke "i64.rem_s" ( i64.const 123121) ( i64.const -1)) ( i64.const 0))
      runner.assertReturn(
          "i64.rem_s",
          new WasmValue[] {WasmValue.i64(0L)},
          WasmValue.i64(123121L),
          WasmValue.i64(-1L));
    }
  }
}
