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
package ai.tegmentum.wasmtime4j.wasmtime.generated.threads;

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
 * Generated test from WAST file: threads/atomics_wait_address.wast
 *
 * <p>Tests atomic wait and notify operations with various edge cases including unaligned access,
 * out of bounds access, non-shared memory, and timeouts.
 *
 * <p>From https://bugzilla.mozilla.org/show_bug.cgi?id=1684861
 *
 * <p>Requires: threads = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/threads/atomics_wait_address.wast
 */
public final class AtomicsWaitAddressTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = AtomicsWaitAddressTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("unaligned atomic notify traps")
  public void testUnalignedAtomicNotifyTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/AtomicsWaitAddressTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      runner.assertTrap("main", "unaligned atomic");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("out of bounds atomic notify traps")
  public void testOutOfBoundsAtomicNotifyTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/AtomicsWaitAddressTest_module2.wat");
      runner.compileAndInstantiate(moduleWat);

      runner.assertTrap("main", "out of bounds memory access");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("unaligned atomic wait traps")
  public void testUnalignedAtomicWaitTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/AtomicsWaitAddressTest_module3.wat");
      runner.compileAndInstantiate(moduleWat);

      runner.assertTrap("wait32", "unaligned atomic");
      runner.assertTrap("wait64", "unaligned atomic");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("atomic wait on non-shared memory traps")
  public void testAtomicWaitOnNonSharedMemoryTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/AtomicsWaitAddressTest_module4.wat");
      runner.compileAndInstantiate(moduleWat);

      runner.assertTrap("wait32", "atomic wait on non-shared memory");
      runner.assertTrap("wait64", "atomic wait on non-shared memory");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("atomic wait with invalid value returns not-equal")
  public void testAtomicWaitInvalidValueReturnsNotEqual(final RuntimeType runtime)
      throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/AtomicsWaitAddressTest_module5.wat");
      runner.compileAndInstantiate(moduleWat);

      // Return value 1 means "not equal"
      runner.assertReturn("wait32", new WasmValue[] {WasmValue.i32(1)});
      runner.assertReturn("wait64", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("atomic wait with timeout returns timed-out")
  public void testAtomicWaitTimeoutReturnsTimedOut(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/AtomicsWaitAddressTest_module6.wat");
      runner.compileAndInstantiate(moduleWat);

      // Return value 2 means "timed out"
      runner.assertReturn("wait32", new WasmValue[] {WasmValue.i32(2)});
      runner.assertReturn("wait64", new WasmValue[] {WasmValue.i32(2)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("atomic wait with zero timeout returns timed-out")
  public void testAtomicWaitZeroTimeoutReturnsTimedOut(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/threads/AtomicsWaitAddressTest_module7.wat");
      runner.compileAndInstantiate(moduleWat);

      // Return value 2 means "timed out"
      runner.assertReturn("wait32", new WasmValue[] {WasmValue.i32(2)});
      runner.assertReturn("wait64", new WasmValue[] {WasmValue.i32(2)});
    }
  }
}
