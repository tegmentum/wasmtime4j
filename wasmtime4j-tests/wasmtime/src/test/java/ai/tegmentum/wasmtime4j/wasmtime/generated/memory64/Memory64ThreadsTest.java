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
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: memory64/threads.wast
 *
 * <p>This test validates atomic operations with 64-bit memory addressing. Tests all atomic
 * load/store operations, atomic read-modify-write operations (add, sub, and, or, xor, xchg,
 * cmpxchg), and ensures they correctly use i64 addresses when targeting memory64 memories.
 *
 * <p>Requires: memory64 = true, threads = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/memory64/threads.wast
 */
public final class Memory64ThreadsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = Memory64ThreadsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Atomic operations with memory64 addressing")
  public void testAtomicsWithMemory64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/memory64/"
                  + "Memory64ThreadsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Execute the run function which exercises all atomic operations
      // with 64-bit memory addresses. No return value expected.
      runner.invoke("run");
    }
  }
}
