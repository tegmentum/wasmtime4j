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

import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: traps::call_signature_mismatch
 *
 * <p>Original source: traps.rs:548 Category: traps
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallSignatureMismatchTest {

  @Test
  @DisplayName("traps::call_signature_mismatch")
  public void testCallSignatureMismatch() throws Exception {
    // WAT code from original Wasmtime test:
    // (module $a
    //                 (func $foo
    //                     i32.const 0
    //                     call_indirect)
    //                 (func $bar (param i32))
    //                 (start $foo)
    //
    //                 (table 1 funcref)
    //                 (elem (i32.const 0) 1)
    //             )

    final String wat =
        """
            (module $a
              (func $foo
                i32.const 0
                call_indirect)
              (func $bar (param i32))
              (start $foo)

              (table 1 funcref)
              (elem (i32.const 0) 1)
            )
        """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      // The start function calls an indirect function with signature mismatch
      // (no params vs expecting i32 param), which should trap during instantiation
      runner.assertUnlinkable(wat, null);
    }
  }
}
