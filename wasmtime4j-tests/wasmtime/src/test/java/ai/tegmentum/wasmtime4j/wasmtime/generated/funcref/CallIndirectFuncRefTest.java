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
 * <p>This test validates call_indirect instruction with typed function references. Tests various
 * table types (funcref, ref null $type, ref $type) and trap conditions.
 *
 * <p>Requires: gc = true, function_references = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/function-references/call_indirect.wast
 */
public final class CallIndirectFuncRefTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = CallIndirectFuncRefTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call_indirect funcref table - valid call")
  public void testCallIndirectFuncrefValid(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/CallIndirectFuncRefTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // t1(0) calls nop function at index 0 - should succeed
      runner.invoke("t1", WasmValue.i32(0));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call_indirect funcref table - uninitialized element")
  public void testCallIndirectFuncrefUninitialized(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/CallIndirectFuncRefTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // t1(1) calls uninitialized element - should trap
      runner.assertTrap("t1", "uninitialized element", WasmValue.i32(1));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call_indirect funcref table - out of bounds")
  public void testCallIndirectFuncrefOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/CallIndirectFuncRefTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // t1(2) is out of bounds - should trap
      runner.assertTrap("t1", "out of bounds", WasmValue.i32(2));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call_indirect funcref table - call type mismatch")
  public void testCallIndirectFuncrefTypeMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/CallIndirectFuncRefTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // t1-wrong-type(0) calls nop but expects (param i32) - should trap
      runner.assertTrap("t1-wrong-type", "call type mismatch", WasmValue.i32(0));
    }
  }
}
