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
 * Generated test from WAST file: export-large-signature.wast
 *
 * <p>This test validates that functions with very large signatures (many parameters) can be
 * exported and called correctly.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/export-large-signature.wast
 */
public final class ExportLargeSignatureTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = ExportLargeSignatureTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("many params - 17 f32 parameters")
  public void testManyParams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "ExportLargeSignatureTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Call many_params with 17 f32 arguments
      // The function has no return value, so we just verify it doesn't trap
      runner.assertReturn(
          "many_params",
          new WasmValue[] {},
          WasmValue.f32(1.0f),
          WasmValue.f32(2.0f),
          WasmValue.f32(3.0f),
          WasmValue.f32(4.0f),
          WasmValue.f32(5.0f),
          WasmValue.f32(6.0f),
          WasmValue.f32(7.0f),
          WasmValue.f32(8.0f),
          WasmValue.f32(9.0f),
          WasmValue.f32(10.0f),
          WasmValue.f32(11.0f),
          WasmValue.f32(12.0f),
          WasmValue.f32(13.0f),
          WasmValue.f32(14.0f),
          WasmValue.f32(15.0f),
          WasmValue.f32(16.0f),
          WasmValue.f32(17.0f));
    }
  }
}
