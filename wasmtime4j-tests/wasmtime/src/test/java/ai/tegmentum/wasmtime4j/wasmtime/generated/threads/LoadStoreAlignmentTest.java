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
package ai.tegmentum.wasmtime4j.wasmtime.generated.threads;

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
 * Generated test from WAST file: threads/load-store-alignment.wast
 *
 * <p>This test validates atomic load/store operations with various alignments. Aligned atomic
 * operations should succeed, while misaligned operations should trap with "unaligned atomic".
 *
 * <p>Requires: threads = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/threads/load-store-alignment.wast
 */
public final class LoadStoreAlignmentTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = LoadStoreAlignmentTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("aligned atomic loads succeed")
  public void testAlignedAtomicLoads(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/"
                  + "LoadStoreAlignmentTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Aligned loads at address 0 should succeed
      runner.assertReturn("32.load8u", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));
      runner.assertReturn("32.load16u", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));
      runner.assertReturn("32.load32u", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));
      runner.assertReturn("64.load8u", new WasmValue[] {WasmValue.i64(0L)}, WasmValue.i32(0));
      runner.assertReturn("64.load16u", new WasmValue[] {WasmValue.i64(0L)}, WasmValue.i32(0));
      runner.assertReturn("64.load64u", new WasmValue[] {WasmValue.i64(0L)}, WasmValue.i32(0));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("misaligned atomic loads trap")
  public void testMisalignedAtomicLoads(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/"
                  + "LoadStoreAlignmentTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // i32.atomic.load8_u is always aligned (1 byte alignment)
      runner.assertReturn("32.load8u", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));

      // Misaligned 16-bit and 32-bit loads should trap
      runner.assertTrap("32.load16u", "unaligned atomic", WasmValue.i32(1));
      runner.assertTrap("32.load32u", "unaligned atomic", WasmValue.i32(1));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("aligned atomic stores succeed")
  public void testAlignedAtomicStores(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/"
                  + "LoadStoreAlignmentTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Aligned stores at address 0 should succeed (no return value)
      runner.invoke("32.store8", WasmValue.i32(0));
      runner.invoke("32.store16", WasmValue.i32(0));
      runner.invoke("32.store32", WasmValue.i32(0));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("misaligned atomic stores trap")
  public void testMisalignedAtomicStores(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/"
                  + "LoadStoreAlignmentTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Misaligned 16-bit and 32-bit stores should trap
      runner.assertTrap("32.store16", "unaligned atomic", WasmValue.i32(1));
      runner.assertTrap("32.store32", "unaligned atomic", WasmValue.i32(1));
    }
  }
}
