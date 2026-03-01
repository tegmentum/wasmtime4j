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
 * Generated test from WAST file: simple_ref_is_null.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class SimpleRefIsNullTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = SimpleRefIsNullTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("simple ref is null")
  public void testSimpleRefIsNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/SimpleRefIsNullTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/SimpleRefIsNullTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "func_is_null" ( ref.null func)) ( i32.const 1))
      runner.assertReturn(
          "func_is_null", new WasmValue[] {WasmValue.i32(1)}, WasmValue.funcref(null));

      // ( assert_return ( invoke "func_is_null_with_non_null_funcref") ( i32.const 0))
      runner.assertReturn("func_is_null_with_non_null_funcref", new WasmValue[] {WasmValue.i32(0)});

      // ( assert_return ( invoke "extern_is_null" ( ref.null extern)) ( i32.const 1))
      runner.assertReturn(
          "extern_is_null", new WasmValue[] {WasmValue.i32(1)}, WasmValue.externref(null));

      // ( assert_return ( invoke "extern_is_null" ( ref.extern 1)) ( i32.const 0))
      runner.assertReturn(
          "extern_is_null", new WasmValue[] {WasmValue.i32(0)}, WasmValue.externref(1L));
    }
  }
}
