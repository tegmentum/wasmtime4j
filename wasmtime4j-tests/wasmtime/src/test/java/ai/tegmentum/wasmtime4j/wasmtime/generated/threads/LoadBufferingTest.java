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
 * Load Buffering (LB) memory model test.
 *
 * <p>Tests the Load Buffering litmus test which checks memory ordering behavior when two threads
 * each load from a location and then store to a different location. With non-atomic operations, any
 * outcome is allowed.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/threads/LB.wast
 */
public final class LoadBufferingTest extends DualRuntimeTest {

  private static final String SHARED_MEMORY_MODULE =
      "(module (memory (export \"shared\") 1 1 shared))";

  /** Thread 1: loads from offset 4, stores 1 to offset 0, saves result at offset 24. */
  private static final String THREAD1_MODULE =
      "(module\n"
          + "  (memory (import \"mem\" \"shared\") 1 10 shared)\n"
          + "  (func (export \"run\")\n"
          + "    (local i32)\n"
          + "    (i32.load (i32.const 4))\n"
          + "    (local.set 0)\n"
          + "    (i32.store (i32.const 0) (i32.const 1))\n"
          + "    (i32.store (i32.const 24) (local.get 0))\n"
          + "  )\n"
          + ")";

  /** Thread 2: loads from offset 0, stores 1 to offset 4, saves result at offset 32. */
  private static final String THREAD2_MODULE =
      "(module\n"
          + "  (memory (import \"mem\" \"shared\") 1 1 shared)\n"
          + "  (func (export \"run\")\n"
          + "    (local i32)\n"
          + "    (i32.load (i32.const 0))\n"
          + "    (local.set 0)\n"
          + "    (i32.store (i32.const 4) (i32.const 1))\n"
          + "    (i32.store (i32.const 32) (local.get 0))\n"
          + "  )\n"
          + ")";

  /** Check module: verifies allowed results (L_0 = 0 or 1) and (L_1 = 0 or 1). */
  private static final String CHECK_MODULE =
      "(module\n"
          + "  (memory (import \"Mem\" \"shared\") 1 1 shared)\n"
          + "  (func (export \"check\") (result i32)\n"
          + "    (local i32 i32)\n"
          + "    (i32.load (i32.const 24))\n"
          + "    (local.set 0)\n"
          + "    (i32.load (i32.const 32))\n"
          + "    (local.set 1)\n"
          + "    (i32.or (i32.eq (local.get 0) (i32.const 1)) (i32.eq (local.get 0) (i32.const"
          + " 0)))\n"
          + "    (i32.or (i32.eq (local.get 1) (i32.const 1)) (i32.eq (local.get 1) (i32.const"
          + " 0)))\n"
          + "    (i32.and)\n"
          + "    (return)\n"
          + "  )\n"
          + ")";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Load Buffering - non-atomic")
  public void testLoadBuffering(final RuntimeType runtime) throws Exception {
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
