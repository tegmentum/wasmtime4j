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
package ai.tegmentum.wasmtime4j.wasmtime.generated.simd;

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
 * Generated test from WAST file: simd/edge-of-memory.wast
 *
 * <p>This test validates SIMD lane operations near the edge of memory (at various offsets from the
 * end of allocated memory).
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/simd/edge-of-memory.wast
 */
public final class EdgeOfMemoryTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = EdgeOfMemoryTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("1-byte-from-end - i8x16 lanes")
  public void test1ByteFromEnd(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/simd/EdgeOfMemoryTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Test at memory offset 65535 (1 byte from end of 64KB memory)
      runner.assertReturn("1-byte-from-end", new WasmValue[] {}, WasmValue.i32(65535));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("2-byte-from-end - i16x8 lanes")
  public void test2ByteFromEnd(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/simd/EdgeOfMemoryTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Test at memory offset 65534 (2 bytes from end)
      runner.assertReturn("2-byte-from-end", new WasmValue[] {}, WasmValue.i32(65534));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("4-byte-from-end - i32x4/f32x4 lanes")
  public void test4ByteFromEnd(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/simd/EdgeOfMemoryTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Test at memory offset 65532 (4 bytes from end)
      runner.assertReturn("4-byte-from-end", new WasmValue[] {}, WasmValue.i32(65532));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("8-byte-from-end - i64x2/f64x2 lanes")
  public void test8ByteFromEnd(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/simd/EdgeOfMemoryTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Test at memory offset 65528 (8 bytes from end)
      runner.assertReturn("8-byte-from-end", new WasmValue[] {}, WasmValue.i32(65528));
    }
  }
}
