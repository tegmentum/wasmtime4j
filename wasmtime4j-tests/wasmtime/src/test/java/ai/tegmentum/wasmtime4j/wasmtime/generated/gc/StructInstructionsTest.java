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
 * Generated test from WAST file: struct-instructions.wast
 *
 * <p>Tests comprehensive struct operations including struct.new, struct.new_default, struct.get,
 * struct.get_s, struct.get_u, and struct.set instructions with various field types.
 *
 * <p>Requires: gc = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/gc/struct-instructions.wast
 */
public final class StructInstructionsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = StructInstructionsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("struct.new_default and basic getters")
  public void testStructNewDefault(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/StructInstructionsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Create default struct
      runner.invoke("new-default");

      // Verify default values
      runner.assertReturn("get-f32", new WasmValue[] {WasmValue.f32(0.0f)});
      runner.assertReturn("get-s-i8", new WasmValue[] {WasmValue.i32(0)});
      runner.assertReturn("get-u-i8", new WasmValue[] {WasmValue.i32(0)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("struct.set and getter verification")
  public void testStructSetGet(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/StructInstructionsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Create default struct
      runner.invoke("new-default");

      // Set and verify f32
      runner.invoke("set-f32", WasmValue.f32(2.0f));
      runner.assertReturn("get-f32", new WasmValue[] {WasmValue.f32(2.0f)});

      // Set and verify i8 (test signed/unsigned)
      runner.invoke("set-i8", WasmValue.i32(-1));
      runner.assertReturn("get-s-i8", new WasmValue[] {WasmValue.i32(-1)});
      runner.assertReturn("get-u-i8", new WasmValue[] {WasmValue.i32(255)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("null struct dereference traps")
  public void testNullStructTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/StructInstructionsTest_module2.wat");
      runner.compileAndInstantiate(moduleWat);

      // All null dereference operations should trap
      runner.assertTrap("struct.get-null", "null reference");
      runner.assertTrap("struct.get_s-null", "null reference");
      runner.assertTrap("struct.get_u-null", "null reference");
      runner.assertTrap("struct.set-null", "null reference");
    }
  }
}
