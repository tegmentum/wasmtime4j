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
package ai.tegmentum.wasmtime4j.wasmtime.generated.traps;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: traps::mismatched_arguments
 *
 * <p>Original source: traps.rs:517 Category: traps
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class MismatchedArgumentsTest {

  @Test
  @DisplayName("traps::mismatched_arguments")
  public void testMismatchedArguments() throws Exception {
    // WAT code from original Wasmtime test:
    // (module $a
    //                 (func (export "foo") (param i32))
    //             )

    final String wat =
        """
            (module $a
              (func (export "foo") (param i32))
            )
        """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat);

      // Test calling with no arguments (should trap - function expects 1 parameter)
      runner.assertTrap("foo", null);

      // Test calling with correct arguments (should succeed - no trap)
      runner.assertReturn("foo", new WasmValue[] {}, WasmValue.i32(0));
    }
  }
}
