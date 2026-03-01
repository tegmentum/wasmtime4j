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
package ai.tegmentum.wasmtime4j.wasmtime.generated.gc;

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
 * Generated test from WAST file: gc/array-init-data.wast
 *
 * <p>Tests the array.init_data instruction which initializes an existing array with data from a
 * data segment. The module defines a mutable i8 array type and a data segment containing "abcd",
 * then exports a function that creates a default-initialized array and initializes it using
 * array.init_data with configurable destination offset, source offset, and length parameters.
 *
 * <p>The full WAST file includes assertions for:
 *
 * <ul>
 *   <li>In-bounds initializations (various offsets and lengths)
 *   <li>Out-of-bounds data segment accesses (traps)
 *   <li>Out-of-bounds array accesses (traps)
 *   <li>Correct content initialization
 *   <li>Little-endian data interpretation
 *   <li>Unaligned data access within segments
 * </ul>
 *
 * <p>Requires: gc = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/gc/array-init-data.wast
 */
public final class ArrayInitDataTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = ArrayInitDataTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("array.init_data instruction compiles correctly")
  public void testArrayInitDataCompile(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/ArrayInitDataTest_module1.wat");
      // Verifies that the array.init_data instruction compiles and the module instantiates
      runner.compileAndInstantiate(moduleWat);
    }
  }
}
