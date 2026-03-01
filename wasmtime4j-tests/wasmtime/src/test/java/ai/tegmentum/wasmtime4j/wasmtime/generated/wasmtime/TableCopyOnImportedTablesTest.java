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
 * Generated test from WAST file: table_copy_on_imported_tables.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class TableCopyOnImportedTablesTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = TableCopyOnImportedTablesTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table copy on imported tables")
  public void testTableCopyOnImportedTables(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/TableCopyOnImportedTablesTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/TableCopyOnImportedTablesTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( register "m" $m)
      runner.registerModule("m");

      // Compile and instantiate module 2
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/TableCopyOnImportedTablesTest_module2.wat
      final String moduleWat2 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/TableCopyOnImportedTablesTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_return ( invoke "call_t" ( i32.const 1) ( i32.const 0) ( i32.const 0) ( i32.const
      // 0) ( i32.const 0) ( i32.const 0) ( i32.const 0)) ( i32.const 1))
      runner.assertReturn(
          "call_t",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // ( assert_return ( invoke "call_t" ( i32.const 0) ( i32.const 1) ( i32.const 0) ( i32.const
      // 0) ( i32.const 0) ( i32.const 0) ( i32.const 1)) ( i32.const 1))
      runner.assertReturn(
          "call_t",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1));

      // ( assert_return ( invoke "call_t" ( i32.const 0) ( i32.const 0) ( i32.const 1) ( i32.const
      // 0) ( i32.const 0) ( i32.const 0) ( i32.const 2)) ( i32.const 1))
      runner.assertReturn(
          "call_t",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(2));

      // ( assert_return ( invoke "call_u" ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const
      // 1) ( i32.const 0) ( i32.const 0) ( i32.const 0)) ( i32.const 1))
      runner.assertReturn(
          "call_u",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // ( assert_return ( invoke "call_u" ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const
      // 0) ( i32.const 1) ( i32.const 0) ( i32.const 1)) ( i32.const 1))
      runner.assertReturn(
          "call_u",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(1));

      // ( assert_return ( invoke "call_u" ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const
      // 0) ( i32.const 0) ( i32.const 1) ( i32.const 2)) ( i32.const 1))
      runner.assertReturn(
          "call_u",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(2));

      // ( invoke "copy_into_t_from_u" ( i32.const 3) ( i32.const 0) ( i32.const 3))
      runner.invoke("copy_into_t_from_u", WasmValue.i32(3), WasmValue.i32(0), WasmValue.i32(3));

      // ( assert_return ( invoke "call_t" ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const
      // 1) ( i32.const 0) ( i32.const 0) ( i32.const 3)) ( i32.const 1))
      runner.assertReturn(
          "call_t",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(3));

      // ( assert_return ( invoke "call_t" ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const
      // 0) ( i32.const 1) ( i32.const 0) ( i32.const 4)) ( i32.const 1))
      runner.assertReturn(
          "call_t",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(4));

      // ( assert_return ( invoke "call_t" ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const
      // 0) ( i32.const 0) ( i32.const 1) ( i32.const 5)) ( i32.const 1))
      runner.assertReturn(
          "call_t",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(5));

      // ( invoke "copy_into_u_from_t" ( i32.const 0) ( i32.const 0) ( i32.const 3))
      runner.invoke("copy_into_u_from_t", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(3));

      // ( assert_return ( invoke "call_u" ( i32.const 1) ( i32.const 0) ( i32.const 0) ( i32.const
      // 0) ( i32.const 0) ( i32.const 0) ( i32.const 0)) ( i32.const 1))
      runner.assertReturn(
          "call_u",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // ( assert_return ( invoke "call_u" ( i32.const 0) ( i32.const 1) ( i32.const 0) ( i32.const
      // 0) ( i32.const 0) ( i32.const 0) ( i32.const 1)) ( i32.const 1))
      runner.assertReturn(
          "call_u",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1));

      // ( assert_return ( invoke "call_u" ( i32.const 0) ( i32.const 0) ( i32.const 1) ( i32.const
      // 0) ( i32.const 0) ( i32.const 0) ( i32.const 2)) ( i32.const 1))
      runner.assertReturn(
          "call_u",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(2));

      // ( register "n" $n)
      runner.registerModule("n");

      // Compile and instantiate module 3
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/TableCopyOnImportedTablesTest_module3.wat
      final String moduleWat3 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/TableCopyOnImportedTablesTest_module3.wat");
      runner.compileAndInstantiate(moduleWat3);

      // ( invoke "copy_into_t_from_u_2" ( i32.const 0) ( i32.const 3) ( i32.const 1))
      runner.invoke("copy_into_t_from_u_2", WasmValue.i32(0), WasmValue.i32(3), WasmValue.i32(1));

      // ( assert_return ( invoke "call_t_2" ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 1) ( i32.const 0) ( i32.const 0) ( i32.const 0)) ( i32.const 1))
      runner.assertReturn(
          "call_t_2",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // ( invoke "copy_into_u_from_t_2" ( i32.const 4) ( i32.const 1) ( i32.const 1))
      runner.invoke("copy_into_u_from_t_2", WasmValue.i32(4), WasmValue.i32(1), WasmValue.i32(1));

      // ( assert_return ( invoke "call_u_2" ( i32.const 0) ( i32.const 1) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 4)) ( i32.const 1))
      runner.assertReturn(
          "call_u_2",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(4));
    }
  }
}
