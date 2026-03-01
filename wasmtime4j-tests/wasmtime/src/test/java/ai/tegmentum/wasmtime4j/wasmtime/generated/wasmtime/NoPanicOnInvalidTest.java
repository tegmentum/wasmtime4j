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
package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: no-panic-on-invalid.wast
 *
 * <p>Tests that invalid binary modules are properly rejected without panicking. The test uses a
 * malformed binary module that has extra instructions after the function body ends.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/no-panic-on-invalid.wast
 */
public final class NoPanicOnInvalidTest extends DualRuntimeTest {

  /**
   * Binary module with invalid structure - has extra instructions after function body ends.
   *
   * <p>The binary encodes: - Type section with one function type (i32) -> i32 - Function section
   * referencing type 0 - Code section with a function that ends early, then has extra unreachable
   * code
   */
  private static final byte[] MALFORMED_MODULE = {
    // Magic number and version
    0x00,
    0x61,
    0x73,
    0x6d, // \0asm
    0x01,
    0x00,
    0x00,
    0x00, // version 1

    // Type section (id=1), 6 bytes
    0x01,
    0x06,
    0x01, // 1 type
    0x60,
    0x01,
    0x7f,
    0x01,
    0x7f, // func type: (i32) -> i32

    // Function section (id=3), 2 bytes
    0x03,
    0x02,
    0x01,
    0x00, // 1 function, type index 0

    // Code section (id=10), 20 bytes
    0x0a,
    0x14,
    0x01, // 1 function body
    0x12, // 18-byte function
    0x00, // no locals
    0x41,
    0x00, // i32.const 0
    0x41,
    0x00, // i32.const 0
    0x0d,
    0x00, // br_if 0
    0x41,
    0x00, // i32.const 0
    0x0f, // return
    0x0b, // end

    // Invalid: extra instructions after function body ends
    0x02,
    0x40, // block
    0x41,
    0x00, // i32.const 0
    0x0f, // return
    0x0b, // end

    // Extra end opcode
    0x0b // end
  };

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Malformed binary module is rejected without panic")
  public void testMalformedModuleRejected(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // The malformed module should be rejected during compilation
      // The error message varies by runtime, so we just verify it fails
      runner.assertMalformedBinary(MALFORMED_MODULE, null);
    }
  }
}
