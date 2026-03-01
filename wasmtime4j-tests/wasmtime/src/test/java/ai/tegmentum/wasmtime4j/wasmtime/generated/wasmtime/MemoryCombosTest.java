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
 * Generated test from WAST file: memory-combos.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class MemoryCombosTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = MemoryCombosTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory combos")
  public void testMemoryCombos(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/MemoryCombosTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/MemoryCombosTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "load_m1" ( i32.const 0)) ( i32.const 0))
      runner.assertReturn("load_m1", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));

      // ( assert_return ( invoke "store_m1" ( i32.const 0) ( i32.const 1)))
      runner.invoke("store_m1", WasmValue.i32(0), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m1" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("load_m1", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "size_m1") ( i32.const 1))
      runner.assertReturn("size_m1", new WasmValue[] {WasmValue.i32(1)});

      // ( assert_trap ( invoke "load_m1" ( i32.const 65536)) "out of bounds memory access")
      runner.assertTrap("load_m1", "out of bounds memory access", WasmValue.i32(65536));

      // ( assert_trap ( invoke "store_m1" ( i32.const 65536) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m1", "out of bounds memory access", WasmValue.i32(65536), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m1" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m1", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m1" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m1", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m1" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("grow_m1", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "size_m1") ( i32.const 2))
      runner.assertReturn("size_m1", new WasmValue[] {WasmValue.i32(2)});

      // ( assert_return ( invoke "grow_m1" ( i32.const -1)) ( i32.const -1))
      runner.assertReturn("grow_m1", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(-1));

      // ( assert_return ( invoke "load_m1" ( i32.const 65536)) ( i32.const 0))
      runner.assertReturn("load_m1", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65536));

      // ( assert_return ( invoke "store_m1" ( i32.const 65536) ( i32.const 1)))
      runner.invoke("store_m1", WasmValue.i32(65536), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m1" ( i32.const 65536)) ( i32.const 1))
      runner.assertReturn("load_m1", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(65536));

      // ( assert_trap ( invoke "load_m1" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m1", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m1" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m1", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m2" ( i32.const 0)) ( i32.const 0))
      runner.assertReturn("load_m2", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));

      // ( assert_return ( invoke "store_m2" ( i32.const 0) ( i32.const 1)))
      runner.invoke("store_m2", WasmValue.i32(0), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m2" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("load_m2", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "size_m2") ( i32.const 1))
      runner.assertReturn("size_m2", new WasmValue[] {WasmValue.i32(1)});

      // ( assert_trap ( invoke "load_m2" ( i32.const 65536)) "out of bounds memory access")
      runner.assertTrap("load_m2", "out of bounds memory access", WasmValue.i32(65536));

      // ( assert_trap ( invoke "store_m2" ( i32.const 65536) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m2", "out of bounds memory access", WasmValue.i32(65536), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m2" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m2", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m2" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m2", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m2" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("grow_m2", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "size_m2") ( i32.const 2))
      runner.assertReturn("size_m2", new WasmValue[] {WasmValue.i32(2)});

      // ( assert_return ( invoke "grow_m2" ( i32.const -1)) ( i32.const -1))
      runner.assertReturn("grow_m2", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(-1));

      // ( assert_return ( invoke "load_m2" ( i32.const 65536)) ( i32.const 0))
      runner.assertReturn("load_m2", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65536));

      // ( assert_return ( invoke "store_m2" ( i32.const 65536) ( i32.const 1)))
      runner.invoke("store_m2", WasmValue.i32(65536), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m2" ( i32.const 65536)) ( i32.const 1))
      runner.assertReturn("load_m2", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(65536));

      // ( assert_trap ( invoke "load_m2" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m2", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m2" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m2", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m3" ( i32.const 0)) ( i32.const 0))
      runner.assertReturn("load_m3", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));

      // ( assert_return ( invoke "store_m3" ( i32.const 0) ( i32.const 1)))
      runner.invoke("store_m3", WasmValue.i32(0), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m3" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("load_m3", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "size_m3") ( i32.const 1))
      runner.assertReturn("size_m3", new WasmValue[] {WasmValue.i32(1)});

      // ( assert_trap ( invoke "load_m3" ( i32.const 1)) "out of bounds memory access")
      runner.assertTrap("load_m3", "out of bounds memory access", WasmValue.i32(1));

      // ( assert_trap ( invoke "store_m3" ( i32.const 1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m3", "out of bounds memory access", WasmValue.i32(1), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m3" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m3", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m3" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m3", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m3" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("grow_m3", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "size_m3") ( i32.const 2))
      runner.assertReturn("size_m3", new WasmValue[] {WasmValue.i32(2)});

      // ( assert_return ( invoke "grow_m3" ( i32.const -1)) ( i32.const -1))
      runner.assertReturn("grow_m3", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(-1));

      // ( assert_return ( invoke "load_m3" ( i32.const 1)) ( i32.const 0))
      runner.assertReturn("load_m3", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));

      // ( assert_return ( invoke "store_m3" ( i32.const 1) ( i32.const 1)))
      runner.invoke("store_m3", WasmValue.i32(1), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m3" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("load_m3", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_trap ( invoke "load_m3" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m3", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m3" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m3", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m4" ( i32.const 0)) ( i32.const 0))
      runner.assertReturn("load_m4", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));

      // ( assert_return ( invoke "store_m4" ( i32.const 0) ( i32.const 1)))
      runner.invoke("store_m4", WasmValue.i32(0), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m4" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("load_m4", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "size_m4") ( i32.const 1))
      runner.assertReturn("size_m4", new WasmValue[] {WasmValue.i32(1)});

      // ( assert_trap ( invoke "load_m4" ( i32.const 1)) "out of bounds memory access")
      runner.assertTrap("load_m4", "out of bounds memory access", WasmValue.i32(1));

      // ( assert_trap ( invoke "store_m4" ( i32.const 1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m4", "out of bounds memory access", WasmValue.i32(1), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m4" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m4", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m4" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m4", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m4" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("grow_m4", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "size_m4") ( i32.const 2))
      runner.assertReturn("size_m4", new WasmValue[] {WasmValue.i32(2)});

      // ( assert_return ( invoke "grow_m4" ( i32.const -1)) ( i32.const -1))
      runner.assertReturn("grow_m4", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(-1));

      // ( assert_return ( invoke "load_m4" ( i32.const 1)) ( i32.const 0))
      runner.assertReturn("load_m4", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));

      // ( assert_return ( invoke "store_m4" ( i32.const 1) ( i32.const 1)))
      runner.invoke("store_m4", WasmValue.i32(1), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m4" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("load_m4", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_trap ( invoke "load_m4" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m4", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m4" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m4", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m6" ( i32.const 0)) ( i32.const 0))
      runner.assertReturn("load_m6", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));

      // ( assert_return ( invoke "store_m6" ( i32.const 0) ( i32.const 1)))
      runner.invoke("store_m6", WasmValue.i32(0), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m6" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("load_m6", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "size_m6") ( i32.const 1))
      runner.assertReturn("size_m6", new WasmValue[] {WasmValue.i32(1)});

      // ( assert_trap ( invoke "load_m6" ( i32.const 65536)) "out of bounds memory access")
      runner.assertTrap("load_m6", "out of bounds memory access", WasmValue.i32(65536));

      // ( assert_trap ( invoke "store_m6" ( i32.const 65536) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m6", "out of bounds memory access", WasmValue.i32(65536), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m6" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m6", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m6" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m6", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m6" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("grow_m6", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "size_m6") ( i32.const 2))
      runner.assertReturn("size_m6", new WasmValue[] {WasmValue.i32(2)});

      // ( assert_return ( invoke "grow_m6" ( i32.const -1)) ( i32.const -1))
      runner.assertReturn("grow_m6", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(-1));

      // ( assert_return ( invoke "load_m6" ( i32.const 65536)) ( i32.const 0))
      runner.assertReturn("load_m6", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65536));

      // ( assert_return ( invoke "store_m6" ( i32.const 65536) ( i32.const 1)))
      runner.invoke("store_m6", WasmValue.i32(65536), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m6" ( i32.const 65536)) ( i32.const 1))
      runner.assertReturn("load_m6", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(65536));

      // ( assert_trap ( invoke "load_m6" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m6", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m6" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m6", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m8" ( i32.const 0)) ( i32.const 0))
      runner.assertReturn("load_m8", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));

      // ( assert_return ( invoke "store_m8" ( i32.const 0) ( i32.const 1)))
      runner.invoke("store_m8", WasmValue.i32(0), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m8" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("load_m8", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "size_m8") ( i32.const 1))
      runner.assertReturn("size_m8", new WasmValue[] {WasmValue.i32(1)});

      // ( assert_trap ( invoke "load_m8" ( i32.const 1)) "out of bounds memory access")
      runner.assertTrap("load_m8", "out of bounds memory access", WasmValue.i32(1));

      // ( assert_trap ( invoke "store_m8" ( i32.const 1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m8", "out of bounds memory access", WasmValue.i32(1), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m8" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m8", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m8" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m8", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m8" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("grow_m8", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "size_m8") ( i32.const 2))
      runner.assertReturn("size_m8", new WasmValue[] {WasmValue.i32(2)});

      // ( assert_return ( invoke "grow_m8" ( i32.const -1)) ( i32.const -1))
      runner.assertReturn("grow_m8", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(-1));

      // ( assert_return ( invoke "load_m8" ( i32.const 1)) ( i32.const 0))
      runner.assertReturn("load_m8", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));

      // ( assert_return ( invoke "store_m8" ( i32.const 1) ( i32.const 1)))
      runner.invoke("store_m8", WasmValue.i32(1), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m8" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("load_m8", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_trap ( invoke "load_m8" ( i32.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m8", "out of bounds memory access", WasmValue.i32(-1));

      // ( assert_trap ( invoke "store_m8" ( i32.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m8", "out of bounds memory access", WasmValue.i32(-1), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m9" ( i64.const 0)) ( i32.const 0))
      runner.assertReturn("load_m9", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "store_m9" ( i64.const 0) ( i32.const 1)))
      runner.invoke("store_m9", WasmValue.i64(0L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m9" ( i64.const 0)) ( i32.const 1))
      runner.assertReturn("load_m9", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "size_m9") ( i64.const 1))
      runner.assertReturn("size_m9", new WasmValue[] {WasmValue.i64(1L)});

      // ( assert_trap ( invoke "load_m9" ( i64.const 65536)) "out of bounds memory access")
      runner.assertTrap("load_m9", "out of bounds memory access", WasmValue.i64(65536L));

      // ( assert_trap ( invoke "store_m9" ( i64.const 65536) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m9", "out of bounds memory access", WasmValue.i64(65536L), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m9" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m9", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m9" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m9", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m9" ( i64.const 1)) ( i64.const 1))
      runner.assertReturn("grow_m9", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "size_m9") ( i64.const 2))
      runner.assertReturn("size_m9", new WasmValue[] {WasmValue.i64(2L)});

      // ( assert_return ( invoke "grow_m9" ( i64.const -1)) ( i64.const -1))
      runner.assertReturn("grow_m9", new WasmValue[] {WasmValue.i64(-1L)}, WasmValue.i64(-1L));

      // ( assert_return ( invoke "load_m9" ( i64.const 65536)) ( i32.const 0))
      runner.assertReturn("load_m9", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(65536L));

      // ( assert_return ( invoke "store_m9" ( i64.const 65536) ( i32.const 1)))
      runner.invoke("store_m9", WasmValue.i64(65536L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m9" ( i64.const 65536)) ( i32.const 1))
      runner.assertReturn("load_m9", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(65536L));

      // ( assert_trap ( invoke "load_m9" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m9", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m9" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m9", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m10" ( i64.const 0)) ( i32.const 0))
      runner.assertReturn("load_m10", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "store_m10" ( i64.const 0) ( i32.const 1)))
      runner.invoke("store_m10", WasmValue.i64(0L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m10" ( i64.const 0)) ( i32.const 1))
      runner.assertReturn("load_m10", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "size_m10") ( i64.const 1))
      runner.assertReturn("size_m10", new WasmValue[] {WasmValue.i64(1L)});

      // ( assert_trap ( invoke "load_m10" ( i64.const 65536)) "out of bounds memory access")
      runner.assertTrap("load_m10", "out of bounds memory access", WasmValue.i64(65536L));

      // ( assert_trap ( invoke "store_m10" ( i64.const 65536) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m10", "out of bounds memory access", WasmValue.i64(65536L), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m10" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m10", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m10" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m10", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m10" ( i64.const 1)) ( i64.const 1))
      runner.assertReturn("grow_m10", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "size_m10") ( i64.const 2))
      runner.assertReturn("size_m10", new WasmValue[] {WasmValue.i64(2L)});

      // ( assert_return ( invoke "grow_m10" ( i64.const -1)) ( i64.const -1))
      runner.assertReturn("grow_m10", new WasmValue[] {WasmValue.i64(-1L)}, WasmValue.i64(-1L));

      // ( assert_return ( invoke "load_m10" ( i64.const 65536)) ( i32.const 0))
      runner.assertReturn("load_m10", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(65536L));

      // ( assert_return ( invoke "store_m10" ( i64.const 65536) ( i32.const 1)))
      runner.invoke("store_m10", WasmValue.i64(65536L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m10" ( i64.const 65536)) ( i32.const 1))
      runner.assertReturn("load_m10", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(65536L));

      // ( assert_trap ( invoke "load_m10" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m10", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m10" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m10", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m11" ( i64.const 0)) ( i32.const 0))
      runner.assertReturn("load_m11", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "store_m11" ( i64.const 0) ( i32.const 1)))
      runner.invoke("store_m11", WasmValue.i64(0L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m11" ( i64.const 0)) ( i32.const 1))
      runner.assertReturn("load_m11", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "size_m11") ( i64.const 1))
      runner.assertReturn("size_m11", new WasmValue[] {WasmValue.i64(1L)});

      // ( assert_trap ( invoke "load_m11" ( i64.const 1)) "out of bounds memory access")
      runner.assertTrap("load_m11", "out of bounds memory access", WasmValue.i64(1L));

      // ( assert_trap ( invoke "store_m11" ( i64.const 1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m11", "out of bounds memory access", WasmValue.i64(1L), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m11" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m11", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m11" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m11", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m11" ( i64.const 1)) ( i64.const 1))
      runner.assertReturn("grow_m11", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "size_m11") ( i64.const 2))
      runner.assertReturn("size_m11", new WasmValue[] {WasmValue.i64(2L)});

      // ( assert_return ( invoke "grow_m11" ( i64.const -1)) ( i64.const -1))
      runner.assertReturn("grow_m11", new WasmValue[] {WasmValue.i64(-1L)}, WasmValue.i64(-1L));

      // ( assert_return ( invoke "load_m11" ( i64.const 1)) ( i32.const 0))
      runner.assertReturn("load_m11", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "store_m11" ( i64.const 1) ( i32.const 1)))
      runner.invoke("store_m11", WasmValue.i64(1L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m11" ( i64.const 1)) ( i32.const 1))
      runner.assertReturn("load_m11", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(1L));

      // ( assert_trap ( invoke "load_m11" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m11", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m11" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m11", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m12" ( i64.const 0)) ( i32.const 0))
      runner.assertReturn("load_m12", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "store_m12" ( i64.const 0) ( i32.const 1)))
      runner.invoke("store_m12", WasmValue.i64(0L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m12" ( i64.const 0)) ( i32.const 1))
      runner.assertReturn("load_m12", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "size_m12") ( i64.const 1))
      runner.assertReturn("size_m12", new WasmValue[] {WasmValue.i64(1L)});

      // ( assert_trap ( invoke "load_m12" ( i64.const 1)) "out of bounds memory access")
      runner.assertTrap("load_m12", "out of bounds memory access", WasmValue.i64(1L));

      // ( assert_trap ( invoke "store_m12" ( i64.const 1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m12", "out of bounds memory access", WasmValue.i64(1L), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m12" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m12", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m12" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m12", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m12" ( i64.const 1)) ( i64.const 1))
      runner.assertReturn("grow_m12", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "size_m12") ( i64.const 2))
      runner.assertReturn("size_m12", new WasmValue[] {WasmValue.i64(2L)});

      // ( assert_return ( invoke "grow_m12" ( i64.const -1)) ( i64.const -1))
      runner.assertReturn("grow_m12", new WasmValue[] {WasmValue.i64(-1L)}, WasmValue.i64(-1L));

      // ( assert_return ( invoke "load_m12" ( i64.const 1)) ( i32.const 0))
      runner.assertReturn("load_m12", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "store_m12" ( i64.const 1) ( i32.const 1)))
      runner.invoke("store_m12", WasmValue.i64(1L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m12" ( i64.const 1)) ( i32.const 1))
      runner.assertReturn("load_m12", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(1L));

      // ( assert_trap ( invoke "load_m12" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m12", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m12" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m12", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m14" ( i64.const 0)) ( i32.const 0))
      runner.assertReturn("load_m14", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "store_m14" ( i64.const 0) ( i32.const 1)))
      runner.invoke("store_m14", WasmValue.i64(0L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m14" ( i64.const 0)) ( i32.const 1))
      runner.assertReturn("load_m14", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "size_m14") ( i64.const 1))
      runner.assertReturn("size_m14", new WasmValue[] {WasmValue.i64(1L)});

      // ( assert_trap ( invoke "load_m14" ( i64.const 65536)) "out of bounds memory access")
      runner.assertTrap("load_m14", "out of bounds memory access", WasmValue.i64(65536L));

      // ( assert_trap ( invoke "store_m14" ( i64.const 65536) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m14", "out of bounds memory access", WasmValue.i64(65536L), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m14" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m14", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m14" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m14", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m14" ( i64.const 1)) ( i64.const 1))
      runner.assertReturn("grow_m14", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "size_m14") ( i64.const 2))
      runner.assertReturn("size_m14", new WasmValue[] {WasmValue.i64(2L)});

      // ( assert_return ( invoke "grow_m14" ( i64.const -1)) ( i64.const -1))
      runner.assertReturn("grow_m14", new WasmValue[] {WasmValue.i64(-1L)}, WasmValue.i64(-1L));

      // ( assert_return ( invoke "load_m14" ( i64.const 65536)) ( i32.const 0))
      runner.assertReturn("load_m14", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(65536L));

      // ( assert_return ( invoke "store_m14" ( i64.const 65536) ( i32.const 1)))
      runner.invoke("store_m14", WasmValue.i64(65536L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m14" ( i64.const 65536)) ( i32.const 1))
      runner.assertReturn("load_m14", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(65536L));

      // ( assert_trap ( invoke "load_m14" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m14", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m14" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m14", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "load_m16" ( i64.const 0)) ( i32.const 0))
      runner.assertReturn("load_m16", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "store_m16" ( i64.const 0) ( i32.const 1)))
      runner.invoke("store_m16", WasmValue.i64(0L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m16" ( i64.const 0)) ( i32.const 1))
      runner.assertReturn("load_m16", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(0L));

      // ( assert_return ( invoke "size_m16") ( i64.const 1))
      runner.assertReturn("size_m16", new WasmValue[] {WasmValue.i64(1L)});

      // ( assert_trap ( invoke "load_m16" ( i64.const 1)) "out of bounds memory access")
      runner.assertTrap("load_m16", "out of bounds memory access", WasmValue.i64(1L));

      // ( assert_trap ( invoke "store_m16" ( i64.const 1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m16", "out of bounds memory access", WasmValue.i64(1L), WasmValue.i32(0));

      // ( assert_trap ( invoke "load_m16" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m16", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m16" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m16", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));

      // ( assert_return ( invoke "grow_m16" ( i64.const 1)) ( i64.const 1))
      runner.assertReturn("grow_m16", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "size_m16") ( i64.const 2))
      runner.assertReturn("size_m16", new WasmValue[] {WasmValue.i64(2L)});

      // ( assert_return ( invoke "grow_m16" ( i64.const -1)) ( i64.const -1))
      runner.assertReturn("grow_m16", new WasmValue[] {WasmValue.i64(-1L)}, WasmValue.i64(-1L));

      // ( assert_return ( invoke "load_m16" ( i64.const 1)) ( i32.const 0))
      runner.assertReturn("load_m16", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i64(1L));

      // ( assert_return ( invoke "store_m16" ( i64.const 1) ( i32.const 1)))
      runner.invoke("store_m16", WasmValue.i64(1L), WasmValue.i32(1));

      // ( assert_return ( invoke "load_m16" ( i64.const 1)) ( i32.const 1))
      runner.assertReturn("load_m16", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i64(1L));

      // ( assert_trap ( invoke "load_m16" ( i64.const -1)) "out of bounds memory access")
      runner.assertTrap("load_m16", "out of bounds memory access", WasmValue.i64(-1L));

      // ( assert_trap ( invoke "store_m16" ( i64.const -1) ( i32.const 0)) "out of bounds memory
      // access")
      runner.assertTrap(
          "store_m16", "out of bounds memory access", WasmValue.i64(-1L), WasmValue.i32(0));
    }
  }
}
