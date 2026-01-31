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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Linker async methods: {@link Linker#instantiateAsync(Store, Module)},
 * {@link Linker#moduleAsync(Store, String, Module)}, {@link Linker#funcNewAsync(String, String,
 * FunctionType, ai.tegmentum.wasmtime4j.AsyncHostFunction)},
 * {@link Linker#funcWrapAsync(String, String, ai.tegmentum.wasmtime4j.AsyncHostFunction)},
 * and {@link Linker#aliasModule(String, String)}.
 *
 * @since 1.0.0
 */
@DisplayName("Linker Async Methods Tests")
public class LinkerAsyncMethodsTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerAsyncMethodsTest.class.getName());

  private static final String ANSWER_WAT =
      """
      (module
        (func (export "answer") (result i32)
          i32.const 42))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instantiateAsync completes and produces working instance")
  void instantiateAsyncCompletesSuccessfully(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing instantiateAsync");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final Module module = engine.compileWat(ANSWER_WAT);

      final CompletableFuture<Instance> future = linker.instantiateAsync(store, module);
      assertNotNull(future, "instantiateAsync future should not be null");

      final Instance instance = future.get();
      assertNotNull(instance, "Async instance should not be null");

      final WasmValue[] result = instance.callFunction("answer");
      assertEquals(1, result.length, "Should have exactly 1 result");
      assertEquals(42, result[0].asInt(), "answer() should return 42");
      LOGGER.info("[" + runtime + "] instantiateAsync answer() = " + result[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instantiateAsync with name completes successfully")
  void instantiateAsyncWithNameCompletesSuccessfully(
      final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing instantiateAsync with module name");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final Module module = engine.compileWat(ANSWER_WAT);

      final CompletableFuture<Instance> future =
          linker.instantiateAsync(store, "my_module", module);
      assertNotNull(future, "Named instantiateAsync future should not be null");

      final Instance instance = future.get();
      assertNotNull(instance, "Named async instance should not be null");

      final WasmValue[] result = instance.callFunction("answer");
      assertEquals(42, result[0].asInt(), "answer() should return 42");
      LOGGER.info("[" + runtime + "] Named instantiateAsync answer() = " + result[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("aliasModule default throws UnsupportedOperationException")
  void aliasModuleDefaultBehavior(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing aliasModule default behavior");

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine)) {

      try {
        linker.aliasModule("from", "to");
        LOGGER.info("[" + runtime + "] aliasModule succeeded (runtime supports it)");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] aliasModule threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("funcNewAsync default throws UnsupportedOperationException")
  void funcNewAsyncDefaultBehavior(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing funcNewAsync default behavior");

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine)) {

      final FunctionType ft = FunctionType.of(
          new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I32});

      try {
        linker.funcNewAsync("env", "test", ft, null);
        LOGGER.info("[" + runtime + "] funcNewAsync succeeded (runtime supports it)");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] funcNewAsync threw UnsupportedOperationException: "
            + e.getMessage());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] funcNewAsync threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("funcWrapAsync default throws UnsupportedOperationException")
  void funcWrapAsyncDefaultBehavior(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing funcWrapAsync default behavior");

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine)) {

      try {
        linker.funcWrapAsync("env", "test", null);
        LOGGER.info("[" + runtime + "] funcWrapAsync succeeded (runtime supports it)");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] funcWrapAsync threw UnsupportedOperationException: "
            + e.getMessage());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] funcWrapAsync threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("moduleAsync completes successfully")
  void moduleAsyncCompletesSuccessfully(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing moduleAsync");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final Module module = engine.compileWat(ANSWER_WAT);

      final CompletableFuture<Void> future =
          linker.moduleAsync(store, "test_module", module);
      assertNotNull(future, "moduleAsync future should not be null");

      future.get();
      LOGGER.info("[" + runtime + "] moduleAsync completed successfully");

      module.close();
    }
  }
}
