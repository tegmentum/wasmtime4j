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
package ai.tegmentum.wasmtime4j.wasmtime.generated.memory64;

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
 * Generated test from WAST file: memory64/bounds.wast
 *
 * <p>This test validates memory64 bounds checking for copy, fill, and init operations. 64-bit
 * addresses that exceed memory bounds should trap.
 *
 * <p>Requires: memory64 = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/memory64/bounds.wast
 */
public final class Memory64BoundsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = Memory64BoundsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory64 copy within bounds")
  public void testMemory64CopyWithinBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/memory64/"
                  + "Memory64BoundsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // copy within bounds should succeed
      runner.invoke("copy", WasmValue.i64(0L), WasmValue.i64(0L), WasmValue.i64(100L));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory64 copy out of bounds traps")
  public void testMemory64CopyOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/memory64/"
                  + "Memory64BoundsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // copy with destination out of bounds should trap
      runner.assertTrap(
          "copy",
          "out of bounds",
          WasmValue.i64(0x1_0000_0000L),
          WasmValue.i64(0L),
          WasmValue.i64(0L));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory64 fill within bounds")
  public void testMemory64FillWithinBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/memory64/"
                  + "Memory64BoundsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // fill within bounds should succeed
      runner.invoke("fill", WasmValue.i64(0L), WasmValue.i32(0), WasmValue.i64(100L));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory64 fill out of bounds traps")
  public void testMemory64FillOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/memory64/"
                  + "Memory64BoundsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // fill with address out of bounds should trap
      runner.assertTrap(
          "fill",
          "out of bounds",
          WasmValue.i64(0x1_0000_0000L),
          WasmValue.i32(0),
          WasmValue.i64(0L));
    }
  }
}
