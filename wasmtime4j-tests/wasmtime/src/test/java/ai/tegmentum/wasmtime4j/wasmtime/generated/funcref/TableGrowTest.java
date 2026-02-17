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

package ai.tegmentum.wasmtime4j.wasmtime.generated.funcref;

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
 * Generated test from WAST file: function-references/table_grow.wast
 *
 * <p>This test validates table.grow instruction with externref tables. Tables can be grown
 * dynamically and the previous size is returned.
 *
 * <p>Requires: gc = true (for externref tables)
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/function-references/table_grow.wast
 */
public final class TableGrowTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = TableGrowTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.grow from empty")
  public void testTableGrowFromEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/TableGrowTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Initial size is 0
      runner.assertReturn("size", new WasmValue[] {WasmValue.i32(0)});

      // Grow by 1, returns old size (0)
      runner.assertReturn(
          "grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1), WasmValue.nullExternref());

      // New size is 1
      runner.assertReturn("size", new WasmValue[] {WasmValue.i32(1)});

      // Get element at index 0 (should be null)
      runner.assertReturn("get", new WasmValue[] {WasmValue.nullExternref()}, WasmValue.i32(0));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.grow multiple times")
  public void testTableGrowMultiple(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/TableGrowTest_module2.wat");
      runner.compileAndInstantiate(moduleWat);

      // Grow by 0, returns current size (0)
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));

      // Grow by 1, returns old size (0)
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));

      // Grow by 0, returns current size (1)
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // Grow by 2, returns old size (1)
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(2));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.grow with max limit")
  public void testTableGrowWithMax(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/TableGrowTest_module3.wat");
      runner.compileAndInstantiate(moduleWat);

      // Grow within limits
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // Continue growing
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(2)}, WasmValue.i32(2));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(4)}, WasmValue.i32(6));

      // At max (10), grow by 0 should succeed
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(10)}, WasmValue.i32(0));

      // At max (10), grow by 1 should fail (-1)
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(1));
    }
  }
}
