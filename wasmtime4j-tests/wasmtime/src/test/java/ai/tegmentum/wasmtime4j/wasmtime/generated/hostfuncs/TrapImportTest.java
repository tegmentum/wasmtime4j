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
package ai.tegmentum.wasmtime4j.wasmtime.generated.hostfuncs;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: host_funcs::trap_import
 *
 * <p>Original source: host_funcs.rs:476 Category: host_funcs
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class TrapImportTest {

  @Test
  @DisplayName("host_funcs::trap_import")
  public void testTrapImport() throws Exception {
    // This test verifies that a trap in an imported host function during start
    // is properly propagated

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define a host function that throws an exception (traps)
      runner.defineHostFunction(
          "",
          "",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
          (args) -> {
            throw new RuntimeException("trap from host function");
          });

      // The module calls the trapping host function in its start function
      final String wat = "(module (import \"\" \"\" (func)) (start 0))";

      // Instantiating should trap because the start function calls the trapping import
      final Exception exception =
          assertThrows(
              Exception.class,
              () -> runner.compileAndInstantiate(wat),
              "Module instantiation should trap due to start function calling trapping import");

      // Verify the exception occurred
      assertTrue(
          exception != null, "Exception should have been thrown from trapping host function");
    }
  }
}
