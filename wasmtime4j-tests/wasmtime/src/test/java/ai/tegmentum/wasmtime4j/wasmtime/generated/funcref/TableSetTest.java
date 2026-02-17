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
 * Generated test from WAST file: function-references/table_set.wast
 *
 * <p>This test validates table.set instruction with externref and funcref tables. Tables can be
 * modified using table.set and values retrieved using table.get.
 *
 * <p>Requires: gc = true (for externref and typed function references)
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/function-references/table_set.wast
 */
public final class TableSetTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = TableSetTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.set externref operations")
  public void testTableSetExternref(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/TableSetTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Initial value is null
      runner.assertReturn(
          "get-externref", new WasmValue[] {WasmValue.nullExternref()}, WasmValue.i32(0));

      // Set externref to null explicitly
      runner.invoke("set-externref", WasmValue.i32(0), WasmValue.nullExternref());

      // Value should still be null
      runner.assertReturn(
          "is-null-externref", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.set out of bounds trap")
  public void testTableSetOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/TableSetTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Out of bounds at index 2 (table size is 1)
      runner.assertTrap("set-externref-trap", "out of bounds table access", WasmValue.i32(2));
    }
  }
}
