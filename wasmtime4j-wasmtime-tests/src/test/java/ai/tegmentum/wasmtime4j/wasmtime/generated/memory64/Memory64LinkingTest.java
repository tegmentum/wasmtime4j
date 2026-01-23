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

package ai.tegmentum.wasmtime4j.wasmtime.generated.memory64;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: memory64/linking.wast
 *
 * <p>Tests module linking with 32-bit and 64-bit memories, verifying that memory type compatibility
 * is correctly enforced during linking.
 *
 * <p>Requires: memory64 = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/memory64/linking.wast
 */
public final class Memory64LinkingTest extends DualRuntimeTest {

  private static final String MODULE_EXPORT32 = "(module (memory (export \"m\") 1))";

  private static final String MODULE_EXPORT64 = "(module (memory (export \"m\") i64 1))";

  private static final String MODULE_IMPORT64_FROM_64 =
      "(module (import \"export64\" \"m\" (memory i64 1)))";

  private static final String MODULE_IMPORT32_FROM_32 =
      "(module (import \"export32\" \"m\" (memory i32 1)))";

  private static final String MODULE_IMPORT64_FROM_32 =
      "(module (import \"export32\" \"m\" (memory i64 1)))";

  private static final String MODULE_IMPORT32_FROM_64 =
      "(module (import \"export64\" \"m\" (memory 1)))";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import 64-bit memory from 64-bit export succeeds")
  public void testImport64From64Succeeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the 64-bit memory exporting module
      runner.compileAndInstantiate("export64", MODULE_EXPORT64);
      runner.registerModule("export64");

      // This should succeed - importing 64-bit memory from 64-bit export
      runner.compileAndInstantiate(MODULE_IMPORT64_FROM_64);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import 32-bit memory from 32-bit export succeeds")
  public void testImport32From32Succeeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the 32-bit memory exporting module
      runner.compileAndInstantiate("export32", MODULE_EXPORT32);
      runner.registerModule("export32");

      // This should succeed - importing 32-bit memory from 32-bit export
      runner.compileAndInstantiate(MODULE_IMPORT32_FROM_32);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import 64-bit memory from 32-bit export fails - memory types incompatible")
  public void testImport64From32Fails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the 32-bit memory exporting module
      runner.compileAndInstantiate("export32", MODULE_EXPORT32);
      runner.registerModule("export32");

      // This should fail - cannot import 32-bit memory as 64-bit
      runner.assertUnlinkable(MODULE_IMPORT64_FROM_32, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import 32-bit memory from 64-bit export fails - memory types incompatible")
  public void testImport32From64Fails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the 64-bit memory exporting module
      runner.compileAndInstantiate("export64", MODULE_EXPORT64);
      runner.registerModule("export64");

      // This should fail - cannot import 64-bit memory as 32-bit
      runner.assertUnlinkable(MODULE_IMPORT32_FROM_64, "incompatible");
    }
  }
}
