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
package ai.tegmentum.wasmtime4j.wasmtime.generated.funcref;

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
 * Generated test from WAST file: function-references/call_indirect.wast
 *
 * <p>Tests call_indirect with typed function references. Tests different table types: funcref, ref
 * null $type, and ref $type with various success and trap conditions.
 *
 * <p>Requires: gc = true, function_references = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/function-references/call_indirect.wast
 */
public final class FuncRefCallIndirectTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = FuncRefCallIndirectTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call_indirect t1 - funcref table")
  public void testCallIndirectT1(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/FuncRefCallIndirectTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // t1 with index 0 - should succeed (nop function)
      runner.assertReturn("t1", new WasmValue[] {}, WasmValue.i32(0));

      // t1 with index 1 - uninitialized element trap
      runner.assertTrap("t1", "uninitialized element", WasmValue.i32(1));

      // t1 with index 2 - out of bounds trap
      runner.assertTrap("t1", "out of bounds", WasmValue.i32(2));

      // t1-wrong-type with index 0 - call type mismatch
      runner.assertTrap("t1-wrong-type", "type mismatch", WasmValue.i32(0));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call_indirect t2 - ref null type table")
  public void testCallIndirectT2(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/FuncRefCallIndirectTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // t2 with index 0 - should succeed
      runner.assertReturn("t2", new WasmValue[] {}, WasmValue.i32(0));

      // t2 with index 1 - uninitialized element trap
      runner.assertTrap("t2", "uninitialized element", WasmValue.i32(1));

      // t2 with index 2 - out of bounds trap
      runner.assertTrap("t2", "out of bounds", WasmValue.i32(2));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call_indirect t3 - ref type table")
  public void testCallIndirectT3(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/FuncRefCallIndirectTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // t3 with index 0 - should succeed
      runner.assertReturn("t3", new WasmValue[] {}, WasmValue.i32(0));

      // t3 with index 1 - should succeed (initialized with ref.func $nop)
      runner.assertReturn("t3", new WasmValue[] {}, WasmValue.i32(1));

      // t3 with index 2 - out of bounds trap
      runner.assertTrap("t3", "out of bounds", WasmValue.i32(2));

      // t3-wrong-type with index 0 - call type mismatch
      runner.assertTrap("t3-wrong-type", "type mismatch", WasmValue.i32(0));
    }
  }
}
