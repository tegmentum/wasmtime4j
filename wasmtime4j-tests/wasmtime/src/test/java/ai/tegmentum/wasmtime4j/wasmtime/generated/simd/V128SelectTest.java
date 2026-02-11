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

package ai.tegmentum.wasmtime4j.wasmtime.generated.simd;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Generated test from WAST file: simd/v128-select.wast
 *
 * <p>This test validates SIMD v128 select instruction which conditionally selects between two
 * vectors based on an i32 condition.
 *
 * <p>Note: This test is Panama-only because V128 parameter passing is not supported in JNI runtime.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/simd/v128-select.wast
 */
public final class V128SelectTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = V128SelectTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  @DisplayName("v128 select - condition false (Panama only)")
  public void testV128SelectConditionFalse() throws Exception {
    // Use Panama runtime explicitly since JNI doesn't support V128 parameters
    try (final WastTestRunner runner = new WastTestRunner(RuntimeType.PANAMA)) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/simd/V128SelectTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // When condition is 0 (false), returns second operand (v128.const i64x2 2 2)
      // v128(high, low) - high is lane[1], low is lane[0]
      runner.assertReturn(
          "select",
          new WasmValue[] {WasmValue.v128(2L, 2L)},
          WasmValue.v128(1L, 1L),
          WasmValue.v128(2L, 2L),
          WasmValue.i32(0));
    }
  }

  @Test
  @DisplayName("v128 select - condition true (Panama only)")
  public void testV128SelectConditionTrue() throws Exception {
    // Use Panama runtime explicitly since JNI doesn't support V128 parameters
    try (final WastTestRunner runner = new WastTestRunner(RuntimeType.PANAMA)) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/simd/V128SelectTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // When condition is 1 (true), returns first operand (v128.const i64x2 1 1)
      // v128(high, low) - high is lane[1], low is lane[0]
      runner.assertReturn(
          "select",
          new WasmValue[] {WasmValue.v128(1L, 1L)},
          WasmValue.v128(1L, 1L),
          WasmValue.v128(2L, 2L),
          WasmValue.i32(1));
    }
  }
}
