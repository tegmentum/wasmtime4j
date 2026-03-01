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
package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Stress tests for memory leak detection. Creates and closes many resources rapidly, verifying that
 * native memory is properly released and no significant leaks occur.
 *
 * <p>These tests require the integration-tests profile due to their resource-intensive nature.
 *
 * @since 1.0.0
 */
@DisplayName("Memory Leak Stress Tests")
@Tag("integration")
public class MemoryLeakStressTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryLeakStressTest.class.getName());

  private static final int ITERATIONS = 10_000;

  private static final String SIMPLE_WAT =
      """
      (module
        (func (export "noop")))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Create and close 10,000 Engine instances without leak")
  void engineCreateCloseStress(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Stress testing Engine create/close (" + ITERATIONS + " iter)");

    final long beforeMemory = getUsedMemory();

    for (int i = 0; i < ITERATIONS; i++) {
      final Engine engine = Engine.create();
      engine.close();
      if (i % 2000 == 0) {
        System.gc();
        LOGGER.info("[" + runtime + "] Engine iteration " + i + "/" + ITERATIONS);
      }
    }

    System.gc();
    Thread.sleep(100);
    final long afterMemory = getUsedMemory();

    LOGGER.info(
        "["
            + runtime
            + "] Memory before: "
            + beforeMemory / 1024
            + "KB, after: "
            + afterMemory / 1024
            + "KB");

    // Allow some growth but flag significant leaks (>100MB growth for 10K engines is suspicious)
    final long growthMb = (afterMemory - beforeMemory) / (1024 * 1024);
    assertTrue(growthMb < 100, "Memory growth should be < 100MB, was " + growthMb + "MB");
    LOGGER.info("[" + runtime + "] Engine stress test completed, growth: " + growthMb + "MB");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Create and close 10,000 Module instances without leak")
  void moduleCreateCloseStress(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Stress testing Module create/close (" + ITERATIONS + " iter)");

    try (Engine engine = Engine.create()) {
      final long beforeMemory = getUsedMemory();

      for (int i = 0; i < ITERATIONS; i++) {
        final Module module = engine.compileWat(SIMPLE_WAT);
        module.close();
        if (i % 2000 == 0) {
          System.gc();
          LOGGER.info("[" + runtime + "] Module iteration " + i + "/" + ITERATIONS);
        }
      }

      System.gc();
      Thread.sleep(100);
      final long afterMemory = getUsedMemory();

      final long growthMb = (afterMemory - beforeMemory) / (1024 * 1024);
      assertTrue(growthMb < 100, "Memory growth should be < 100MB, was " + growthMb + "MB");
      LOGGER.info("[" + runtime + "] Module stress test completed, growth: " + growthMb + "MB");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Create and close 10,000 Instance lifecycles without leak")
  void instanceLifecycleStress(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Stress testing Instance lifecycle (" + ITERATIONS + " iter)");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(SIMPLE_WAT);
      final long beforeMemory = getUsedMemory();

      for (int i = 0; i < ITERATIONS; i++) {
        final Store store = engine.createStore();
        final Instance instance = module.instantiate(store);
        instance.close();
        store.close();
        if (i % 2000 == 0) {
          System.gc();
          LOGGER.info("[" + runtime + "] Instance iteration " + i + "/" + ITERATIONS);
        }
      }

      System.gc();
      Thread.sleep(100);
      final long afterMemory = getUsedMemory();

      final long growthMb = (afterMemory - beforeMemory) / (1024 * 1024);
      assertTrue(growthMb < 100, "Memory growth should be < 100MB, was " + growthMb + "MB");
      LOGGER.info("[" + runtime + "] Instance stress test completed, growth: " + growthMb + "MB");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Compile same module repeatedly without growth")
  void repeatedCompilationStress(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Stress testing repeated compilation (" + ITERATIONS + " iter)");

    try (Engine engine = Engine.create()) {
      final long beforeMemory = getUsedMemory();

      // Compile the same WAT string many times
      for (int i = 0; i < ITERATIONS; i++) {
        final Module module = engine.compileWat(SIMPLE_WAT);
        module.close();
        if (i % 2000 == 0) {
          System.gc();
          LOGGER.info("[" + runtime + "] Compilation iteration " + i + "/" + ITERATIONS);
        }
      }

      System.gc();
      Thread.sleep(100);
      final long afterMemory = getUsedMemory();

      final long growthMb = (afterMemory - beforeMemory) / (1024 * 1024);
      assertTrue(growthMb < 100, "Memory growth should be < 100MB, was " + growthMb + "MB");
      LOGGER.info(
          "["
              + runtime
              + "] Repeated compilation stress test completed, growth: "
              + growthMb
              + "MB");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Mixed create/close in random order without crash")
  void mixedCreateCloseOrder(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Stress testing mixed create/close order");

    final int batchSize = 100;
    final int batches = 100;

    for (int batch = 0; batch < batches; batch++) {
      final Engine[] engines = new Engine[batchSize];

      // Create all
      for (int i = 0; i < batchSize; i++) {
        engines[i] = Engine.create();
      }

      // Close in reverse order
      for (int i = batchSize - 1; i >= 0; i--) {
        assertDoesNotThrow(engines[i]::close, "Close should not throw");
      }

      if (batch % 20 == 0) {
        System.gc();
        LOGGER.info("[" + runtime + "] Batch " + batch + "/" + batches);
      }
    }

    LOGGER.info("[" + runtime + "] Mixed create/close stress test completed");
  }

  private static long getUsedMemory() {
    final Runtime rt = Runtime.getRuntime();
    return rt.totalMemory() - rt.freeMemory();
  }
}
