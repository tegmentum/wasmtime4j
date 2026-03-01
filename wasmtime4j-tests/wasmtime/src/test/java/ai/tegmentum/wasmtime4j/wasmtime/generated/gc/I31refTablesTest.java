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
package ai.tegmentum.wasmtime4j.wasmtime.generated.gc;

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
 * Generated test from WAST file: gc/i31ref-tables.wast
 *
 * <p>Tests i31ref values in tables with operations like table.get, table.grow, table.fill,
 * table.copy, and table.init.
 *
 * <p>Requires: gc = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/gc/i31ref-tables.wast
 */
public final class I31refTablesTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = I31refTablesTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("i31ref table initial state")
  public void testI31refTableInitialState(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/I31refTablesTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Initial size is 3
      runner.assertReturn("size", new WasmValue[] {WasmValue.i32(3)});

      // Check initial values
      runner.assertReturn("get", new WasmValue[] {WasmValue.i32(999)}, WasmValue.i32(0));
      runner.assertReturn("get", new WasmValue[] {WasmValue.i32(888)}, WasmValue.i32(1));
      runner.assertReturn("get", new WasmValue[] {WasmValue.i32(777)}, WasmValue.i32(2));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("i31ref table grow")
  public void testI31refTableGrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/I31refTablesTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Grow from size 3 to size 5, returns old size (3)
      runner.assertReturn(
          "grow", new WasmValue[] {WasmValue.i32(3)}, WasmValue.i32(2), WasmValue.i32(333));

      // New size is 5
      runner.assertReturn("size", new WasmValue[] {WasmValue.i32(5)});

      // New elements have value 333
      runner.assertReturn("get", new WasmValue[] {WasmValue.i32(333)}, WasmValue.i32(3));
      runner.assertReturn("get", new WasmValue[] {WasmValue.i32(333)}, WasmValue.i32(4));
    }
  }
}
