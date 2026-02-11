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

package ai.tegmentum.wasmtime4j.wasmtime.generated.threads;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastThreadTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Message Passing (MP) memory model test with atomic operations.
 *
 * <p>Tests the Message Passing litmus test using atomic stores and loads. With atomic operations,
 * the outcome (L_0=1 && L_1=0) is forbidden due to sequential consistency.
 *
 * <p>Allowed: (L_0=1 && L_1=42) || (L_0=0 && L_1=0) || (L_0=0 && L_1=42)
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/threads/MP_atomic.wast
 */
public final class MessagePassingAtomicTest extends DualRuntimeTest {

  private static final String SHARED_MEMORY_MODULE =
      "(module (memory (export \"shared\") 1 1 shared))";

  /** Thread 1: atomic stores data (42) at offset 0, then flag (1) at offset 4. */
  private static final String THREAD1_MODULE =
      "(module\n"
          + "  (memory (import \"mem\" \"shared\") 1 10 shared)\n"
          + "  (func (export \"run\")\n"
          + "    (i32.atomic.store (i32.const 0) (i32.const 42))\n"
          + "    (i32.atomic.store (i32.const 4) (i32.const 1))\n"
          + "  )\n"
          + ")";

  /** Thread 2: atomic loads flag from offset 4, then data from offset 0. */
  private static final String THREAD2_MODULE =
      "(module\n"
          + "  (memory (import \"mem\" \"shared\") 1 1 shared)\n"
          + "  (func (export \"run\")\n"
          + "    (local i32 i32)\n"
          + "    (i32.atomic.load (i32.const 4))\n"
          + "    (local.set 0)\n"
          + "    (i32.atomic.load (i32.const 0))\n"
          + "    (local.set 1)\n"
          + "    (i32.store (i32.const 24) (local.get 0))\n"
          + "    (i32.store (i32.const 32) (local.get 1))\n"
          + "  )\n"
          + ")";

  /**
   * Check module: verifies allowed results. With atomics, (L_0=1 && L_1=0) is forbidden.
   *
   * <p>Allowed: (L_0=1 && L_1=42) || (L_0=0 && L_1=0) || (L_0=0 && L_1=42)
   */
  private static final String CHECK_MODULE =
      "(module\n"
          + "  (memory (import \"Mem\" \"shared\") 1 1 shared)\n"
          + "  (func (export \"check\") (result i32)\n"
          + "    (local i32 i32)\n"
          + "    (i32.load (i32.const 24))\n"
          + "    (local.set 0)\n"
          + "    (i32.load (i32.const 32))\n"
          + "    (local.set 1)\n"
          + "    (i32.and (i32.eq (local.get 0) (i32.const 1)) (i32.eq (local.get 1) (i32.const"
          + " 42)))\n"
          + "    (i32.and (i32.eq (local.get 0) (i32.const 0)) (i32.eq (local.get 1) (i32.const"
          + " 0)))\n"
          + "    (i32.and (i32.eq (local.get 0) (i32.const 0)) (i32.eq (local.get 1) (i32.const"
          + " 42)))\n"
          + "    (i32.or)\n"
          + "    (i32.or)\n"
          + "    (return)\n"
          + "  )\n"
          + ")";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Message Passing - atomic")
  public void testMessagePassingAtomic(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastThreadTestRunner runner = new WastThreadTestRunner(runtime)) {
      runner.defineSharedModule("Mem", SHARED_MEMORY_MODULE);

      runner.defineThread(
          "T1",
          thread -> {
            try {
              thread.importMemory("mem", "shared", "Mem", "shared");
              thread.compileAndInstantiate(THREAD1_MODULE);
              thread.assertReturnVoid("run");
            } catch (final Exception e) {
              throw new RuntimeException("Thread T1 failed", e);
            }
          });

      runner.defineThread(
          "T2",
          thread -> {
            try {
              thread.importMemory("mem", "shared", "Mem", "shared");
              thread.compileAndInstantiate(THREAD2_MODULE);
              thread.assertReturnVoid("run");
            } catch (final Exception e) {
              throw new RuntimeException("Thread T2 failed", e);
            }
          });

      runner.withTimeout(30).runThreads();

      // Verify with check module
      final ai.tegmentum.wasmtime4j.Instance checkInstance =
          runner.compileAndInstantiate("Check", CHECK_MODULE);
      runner.assertReturn(checkInstance, "check", new WasmValue[] {WasmValue.i32(1)});
    }
  }
}
