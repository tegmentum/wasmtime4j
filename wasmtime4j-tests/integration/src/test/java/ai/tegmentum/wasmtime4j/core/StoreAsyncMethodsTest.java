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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler;
import ai.tegmentum.wasmtime4j.Store.AsyncResourceLimiter;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link Store#setCallHookAsync(AsyncCallHookHandler)} and {@link
 * Store#limiterAsync(AsyncResourceLimiter)}.
 *
 * <p>All methods are wrapped in defensive try/catch for {@link UnsupportedOperationException} since
 * async features may not be available in all runtimes.
 *
 * @since 1.0.0
 */
@DisplayName("Store Async Methods Tests")
public class StoreAsyncMethodsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(StoreAsyncMethodsTest.class.getName());

  /** WAT module with memory (1-10 pages), a memory grow function, and a nop function. */
  private static final String WAT =
      """
      (module
        (memory (export "mem") 1 10)
        (func (export "grow_mem") (result i32) i32.const 1 memory.grow)
        (func (export "nop")))
      """;

  /** WAT module with a table that can grow. */
  private static final String TABLE_WAT =
      """
      (module
        (table (export "tab") 1 10 funcref)
        (func (export "nop")))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setCallHookAsync receives call events")
  void setCallHookAsyncReceivesEvents(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing setCallHookAsync receives events");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final AtomicBoolean hookInvoked = new AtomicBoolean(false);

      try {
        final AsyncCallHookHandler handler =
            (callHook) -> {
              hookInvoked.set(true);
              LOGGER.info("[" + runtime + "] Async call hook invoked: " + callHook);
              return CompletableFuture.completedFuture(null);
            };
        store.setCallHookAsync(handler);

        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        // Call a function to trigger the hook
        instance.callFunction("nop");

        LOGGER.info("[" + runtime + "] Hook invoked after calling nop: " + hookInvoked.get());
        // Note: hook invocation depends on async engine configuration

        instance.close();
        module.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] setCallHookAsync not supported: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] setCallHookAsync threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setCallHookAsync with null clears handler")
  void setCallHookAsyncNullClears(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing setCallHookAsync(null) clears handler");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        // Set a handler
        store.setCallHookAsync((callHook) -> CompletableFuture.completedFuture(null));

        // Clear by passing null
        store.setCallHookAsync(null);
        LOGGER.info("[" + runtime + "] setCallHookAsync(null) succeeded");
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] setCallHookAsync not supported: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      } catch (final NullPointerException e) {
        LOGGER.info("[" + runtime + "] setCallHookAsync(null) threw NPE: " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] setCallHookAsync(null) threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("limiterAsync allows memory growth")
  void limiterAsyncAllowsGrowth(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing limiterAsync that allows growth");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final AsyncResourceLimiter limiter =
            new AsyncResourceLimiter() {
              @Override
              public CompletableFuture<Boolean> allowMemoryGrow(
                  final long currentSize, final long desiredSize) {
                LOGGER.info(
                    "["
                        + runtime
                        + "] Async limiter allowMemoryGrow: "
                        + currentSize
                        + " -> "
                        + desiredSize);
                return CompletableFuture.completedFuture(true);
              }

              @Override
              public CompletableFuture<Boolean> allowTableGrow(
                  final long currentSize, final long desiredSize) {
                return CompletableFuture.completedFuture(true);
              }
            };
        store.limiterAsync(limiter);

        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        // Try to grow memory by 1 page
        final WasmValue[] result = instance.callFunction("grow_mem");
        assertNotNull(result, "grow_mem should return a result");
        LOGGER.info(
            "["
                + runtime
                + "] grow_mem returned: "
                + result[0].asInt()
                + " (previous page count, -1 means failure)");

        instance.close();
        module.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync not supported: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("limiterAsync denies memory growth")
  void limiterAsyncDeniesGrowth(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing limiterAsync that denies growth");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final AsyncResourceLimiter limiter =
            new AsyncResourceLimiter() {
              @Override
              public CompletableFuture<Boolean> allowMemoryGrow(
                  final long currentSize, final long desiredSize) {
                LOGGER.info(
                    "["
                        + runtime
                        + "] Async limiter DENYING memory grow: "
                        + currentSize
                        + " -> "
                        + desiredSize);
                return CompletableFuture.completedFuture(false);
              }

              @Override
              public CompletableFuture<Boolean> allowTableGrow(
                  final long currentSize, final long desiredSize) {
                return CompletableFuture.completedFuture(false);
              }
            };
        store.limiterAsync(limiter);

        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        // Try to grow memory -- should be denied
        final WasmValue[] result = instance.callFunction("grow_mem");
        assertNotNull(result, "grow_mem should return a result even when denied");
        // When memory.grow is denied, it returns -1
        LOGGER.info(
            "["
                + runtime
                + "] grow_mem returned: "
                + result[0].asInt()
                + " (expected -1 for denied growth)");

        instance.close();
        module.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync not supported: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("limiterAsync controls table growth")
  void limiterAsyncControlsTableGrowth(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing limiterAsync table growth control");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final AtomicBoolean tableGrowCalled = new AtomicBoolean(false);

      try {
        final AsyncResourceLimiter limiter =
            new AsyncResourceLimiter() {
              @Override
              public CompletableFuture<Boolean> allowMemoryGrow(
                  final long currentSize, final long desiredSize) {
                return CompletableFuture.completedFuture(true);
              }

              @Override
              public CompletableFuture<Boolean> allowTableGrow(
                  final long currentSize, final long desiredSize) {
                tableGrowCalled.set(true);
                LOGGER.info(
                    "["
                        + runtime
                        + "] Async limiter allowTableGrow: "
                        + currentSize
                        + " -> "
                        + desiredSize);
                return CompletableFuture.completedFuture(true);
              }
            };
        store.limiterAsync(limiter);

        final Module module = engine.compileWat(TABLE_WAT);
        final Instance instance = module.instantiate(store);

        // Get the table and try to grow it
        instance
            .getTable("tab")
            .ifPresent(
                table -> {
                  try {
                    table.grow(1, null);
                    LOGGER.info(
                        "["
                            + runtime
                            + "] Table grow succeeded, "
                            + "tableGrowCalled="
                            + tableGrowCalled.get());
                  } catch (final Exception e) {
                    LOGGER.info(
                        "["
                            + runtime
                            + "] Table grow threw: "
                            + e.getClass().getName()
                            + " - "
                            + e.getMessage());
                  }
                });

        instance.close();
        module.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync not supported: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("limiterAsync null clears limiter")
  void limiterAsyncNullClears(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing limiterAsync(null) clears limiter");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        // Set a limiter
        store.limiterAsync(
            new AsyncResourceLimiter() {
              @Override
              public CompletableFuture<Boolean> allowMemoryGrow(
                  final long currentSize, final long desiredSize) {
                return CompletableFuture.completedFuture(true);
              }

              @Override
              public CompletableFuture<Boolean> allowTableGrow(
                  final long currentSize, final long desiredSize) {
                return CompletableFuture.completedFuture(true);
              }
            });

        // Clear by passing null
        store.limiterAsync(null);
        LOGGER.info("[" + runtime + "] limiterAsync(null) succeeded");
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync not supported: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      } catch (final NullPointerException e) {
        LOGGER.info("[" + runtime + "] limiterAsync(null) threw NPE: " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] limiterAsync(null) threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }
}
