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
 * Generated test from WAST file: memory64/linking-errors.wast
 *
 * <p>Tests that importing a 32-bit memory as 64-bit fails with proper error message.
 *
 * <p>Requires: memory64 = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/memory64/linking-errors.wast
 */
public final class Memory64LinkingErrorsTest extends DualRuntimeTest {

  private static final String MODULE_EXPORT_32BIT = "(module (memory (export \"mem\") 0))";

  private static final String MODULE_IMPORT_AS_64BIT =
      "(module (import \"m\" \"mem\" (memory i64 0)))";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import 32-bit memory as 64-bit fails with proper error")
  public void testImport32BitAs64BitFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the 32-bit memory exporting module
      runner.compileAndInstantiate("m", MODULE_EXPORT_32BIT);
      runner.registerModule("m");

      // Try to import as 64-bit - should fail
      // The WAST expects "expected 64-bit memory, found 32-bit memory"
      // Wasmtime returns "incompatible import type for `m::mem`"
      runner.assertUnlinkable(MODULE_IMPORT_AS_64BIT, "incompatible");
    }
  }
}
