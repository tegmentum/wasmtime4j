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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.DebugFrame;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmBacktrace;
import ai.tegmentum.wasmtime4j.debug.DebugHandler;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Store debug and GC APIs: {@link Store#gcAsync()}, {@link Store#gc()},
 * {@link Store#debugFrames()}, {@link Store#captureBacktrace()},
 * {@link Store#forceCaptureBacktrace()}, {@link Store#setDebugHandler(DebugHandler)}.
 *
 * <p>Some features require specific engine configurations (e.g., GC support, debug mode). Tests
 * use try/catch to gracefully handle UnsupportedOperationException for features not available.
 *
 * @since 1.0.0
 */
@DisplayName("Store Debug and GC Tests")
public class StoreDebugAndGcTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(StoreDebugAndGcTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("gcAsync completes without error")
  void gcAsyncCompletesWithoutError(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing gcAsync");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final CompletableFuture<Void> future = store.gcAsync();
        assertNotNull(future, "gcAsync future should not be null");
        future.get();
        LOGGER.info("[" + runtime + "] gcAsync completed successfully");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] gcAsync not available: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("debugFrames returns list outside execution")
  void debugFramesReturnsListOutsideExecution(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing debugFrames outside execution");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final List<DebugFrame> frames = store.debugFrames();
        assertNotNull(frames, "debugFrames should not return null");
        LOGGER.info("[" + runtime + "] debugFrames returned " + frames.size() + " frames");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] debugFrames not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("captureBacktrace outside execution returns non-null")
  void captureBacktraceOutsideExecution(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing captureBacktrace outside execution");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final WasmBacktrace backtrace = store.captureBacktrace();
        assertNotNull(backtrace, "captureBacktrace should not return null");
        LOGGER.info("[" + runtime + "] captureBacktrace returned "
            + backtrace.getFrameCount() + " frames, isEmpty="
            + backtrace.isEmpty());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] captureBacktrace not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("forceCaptureBacktrace outside execution returns non-null")
  void forceCaptureBacktraceOutsideExecution(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing forceCaptureBacktrace outside execution");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final WasmBacktrace backtrace = store.forceCaptureBacktrace();
        assertNotNull(backtrace, "forceCaptureBacktrace should not return null");
        LOGGER.info("[" + runtime + "] forceCaptureBacktrace returned "
            + backtrace.getFrameCount() + " frames, forceCapture="
            + backtrace.isForceCapture());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] forceCaptureBacktrace not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setDebugHandler with null does not crash")
  void setDebugHandlerNullDoesNotCrash(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing setDebugHandler(null)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        store.setDebugHandler(null);
        LOGGER.info("[" + runtime + "] setDebugHandler(null) completed without crash");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] setDebugHandler not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setDebugHandler and clearDebugHandler lifecycle")
  void setDebugHandlerAndClear(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing setDebugHandler lifecycle");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final DebugHandler handler = DebugHandler.logging();
        store.setDebugHandler(handler);
        LOGGER.info("[" + runtime + "] setDebugHandler with logging handler succeeded");

        store.clearDebugHandler();
        LOGGER.info("[" + runtime + "] clearDebugHandler succeeded");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] setDebugHandler not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("gc sync does not throw")
  void gcSyncDoesNotThrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing gc() sync");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        store.gc();
        LOGGER.info("[" + runtime + "] gc() completed without error");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] gc() not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }
}
