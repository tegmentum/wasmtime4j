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
 * Generated test from WAST file: threads/atomics-end-of-memory.wast
 *
 * <p>This test validates atomic operations at the end of memory boundaries. Tests
 * memory.atomic.notify, memory.atomic.wait32, and memory.atomic.wait64 at the last valid offsets.
 *
 * <p>Requires: threads = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/threads/atomics-end-of-memory.wast
 */
public final class AtomicsEndOfMemoryTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = AtomicsEndOfMemoryTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("atomic.notify at end of memory")
  public void testAtomicNotifyEndOfMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/"
                  + "AtomicsEndOfMemoryTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // notify at offset 65532 (last 4-byte aligned position in 1-page memory)
      runner.assertReturn("notify_last", new WasmValue[] {WasmValue.i32(0)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("atomic.wait32 at end of memory")
  public void testAtomicWait32EndOfMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/"
                  + "AtomicsEndOfMemoryTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // wait32 at offset 65532 with timeout 0 returns 2 (timed out)
      runner.assertReturn("wait_last32", new WasmValue[] {WasmValue.i32(2)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("atomic.wait64 at end of memory")
  public void testAtomicWait64EndOfMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/"
                  + "AtomicsEndOfMemoryTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // wait64 at offset 65528 with timeout 0 returns 2 (timed out)
      runner.assertReturn("wait_last64", new WasmValue[] {WasmValue.i32(2)});
    }
  }
}
