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
package ai.tegmentum.wasmtime4j.wasmtime.generated.multimemory;

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
 * Generated test from WAST file: multi-memory/simple.wast
 *
 * <p>Tests basic multi-memory functionality including store/load operations across different
 * memories, memory growth, memory.init, and memory.fill operations.
 *
 * <p>Requires: multi_memory = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/multi-memory/simple.wast
 */
public final class MultiMemorySimpleTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = MultiMemorySimpleTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("multi-memory store and load")
  public void testMultiMemoryStoreLoad(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/multimemory/MultiMemorySimpleTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Store different values in each memory
      runner.invoke("store1", WasmValue.i32(0), WasmValue.i64(1));
      runner.invoke("store2", WasmValue.i32(0), WasmValue.i64(2));

      // Verify values are stored in separate memories
      runner.assertReturn("load1", new WasmValue[] {WasmValue.i64(1)}, WasmValue.i32(0));
      runner.assertReturn("load2", new WasmValue[] {WasmValue.i64(2)}, WasmValue.i32(0));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("multi-memory grow and size")
  public void testMultiMemoryGrowSize(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/multimemory/MultiMemorySimpleTest_module2.wat");
      runner.compileAndInstantiate(moduleWat);

      // Check initial sizes
      runner.assertReturn("size1", new WasmValue[] {WasmValue.i32(1)});
      runner.assertReturn("size2", new WasmValue[] {WasmValue.i32(2)});

      // Grow memory 1
      runner.assertReturn("grow1", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(3));
      runner.assertReturn("grow1", new WasmValue[] {WasmValue.i32(4)}, WasmValue.i32(4));
      runner.assertReturn("grow1", new WasmValue[] {WasmValue.i32(8)}, WasmValue.i32(1));

      // Grow memory 2
      runner.assertReturn("grow2", new WasmValue[] {WasmValue.i32(2)}, WasmValue.i32(1));
      runner.assertReturn("grow2", new WasmValue[] {WasmValue.i32(3)}, WasmValue.i32(1));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("multi-memory init")
  public void testMultiMemoryInit(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/multimemory/MultiMemorySimpleTest_module3.wat");
      runner.compileAndInstantiate(moduleWat);

      // memory.init initializes from data segment into each memory
      runner.assertReturn("init1", new WasmValue[] {WasmValue.i32(1)});
      runner.assertReturn("init2", new WasmValue[] {WasmValue.i32(2)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("multi-memory fill")
  public void testMultiMemoryFill(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/multimemory/MultiMemorySimpleTest_module4.wat");
      runner.compileAndInstantiate(moduleWat);

      // memory.fill writes byte values to each memory
      runner.assertReturn("fill1", new WasmValue[] {WasmValue.i32(0x01010101)});
      runner.assertReturn("fill2", new WasmValue[] {WasmValue.i32(0x0202)});
    }
  }
}
