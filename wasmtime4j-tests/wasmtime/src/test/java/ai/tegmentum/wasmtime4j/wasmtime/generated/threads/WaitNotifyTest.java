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
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastThreadTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: threads/wait_notify.wast
 *
 * <p>Tests that looping notify eventually unblocks a parallel waiting thread. This test validates
 * the WebAssembly threads proposal wait/notify synchronization mechanism.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/threads/wait_notify.wast
 */
public final class WaitNotifyTest extends DualRuntimeTest {

  /** WAT module that exports shared memory. */
  private static final String SHARED_MEMORY_MODULE =
      "(module (memory (export \"shared\") 1 1 shared))";

  /** WAT module for thread 1 that waits on the shared memory. */
  private static final String WAIT_MODULE =
      "(module\n"
          + "  (memory (import \"mem\" \"shared\") 1 10 shared)\n"
          + "  (func (export \"run\") (result i32)\n"
          + "    (memory.atomic.wait32 (i32.const 0) (i32.const 0) (i64.const -1))\n"
          + "  )\n"
          + ")";

  /** WAT module for thread 2 that notifies waiters. */
  private static final String NOTIFY_MODULE =
      "(module\n"
          + "  (memory (import \"mem\" \"shared\") 1 1 shared)\n"
          + "  (func (export \"notify-0\") (result i32)\n"
          + "    (memory.atomic.notify (i32.const 0) (i32.const 0))\n"
          + "  )\n"
          + "  (func (export \"notify-1-while\")\n"
          + "    (loop\n"
          + "      (i32.const 1)\n"
          + "      (memory.atomic.notify (i32.const 0) (i32.const 1))\n"
          + "      (i32.ne)\n"
          + "      (br_if 0)\n"
          + "    )\n"
          + "  )\n"
          + ")";

  /**
   * Tests that notify eventually unblocks a parallel waiting thread.
   *
   * <p>Thread 1 waits on the shared memory at offset 0 with infinite timeout. Thread 2 loops
   * calling notify until it successfully wakes a waiter. When Thread 2's notify returns 1 (meaning
   * it woke 1 thread), Thread 1's wait should return 0 (meaning it was woken by notify).
   *
   * @param runtime the runtime type to test
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("notify unblocks waiting thread")
  public void testNotifyUnblocksWaitingThread(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastThreadTestRunner runner = new WastThreadTestRunner(runtime)) {
      // Define shared module with shared memory
      runner.defineSharedModule("Mem", SHARED_MEMORY_MODULE);

      // Thread 1: Waits on the shared memory
      runner.defineThread(
          "T1",
          thread -> {
            try {
              thread.importMemory("mem", "shared", "Mem", "shared");
              thread.compileAndInstantiate(WAIT_MODULE);
              // This wait should eventually return 0 when notified
              thread.assertReturn("run", new WasmValue[] {WasmValue.i32(0)});
            } catch (final Exception e) {
              throw new RuntimeException("Thread T1 failed", e);
            }
          });

      // Thread 2: Notifies waiters
      runner.defineThread(
          "T2",
          thread -> {
            try {
              thread.importMemory("mem", "shared", "Mem", "shared");
              thread.compileAndInstantiate(NOTIFY_MODULE);
              // Notifying with count=0 should return 0 (no waiters woken)
              thread.assertReturn("notify-0", new WasmValue[] {WasmValue.i32(0)});
              // Loop until we wake someone
              thread.assertReturnVoid("notify-1-while");
            } catch (final Exception e) {
              throw new RuntimeException("Thread T2 failed", e);
            }
          });

      // Run threads with timeout
      runner.withTimeout(30).runThreads();
    }
  }
}
