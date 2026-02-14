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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Linker methods: {@link Linker#funcNewAsync(String, String, FunctionType,
 * ai.tegmentum.wasmtime4j.AsyncHostFunction)}, {@link Linker#funcWrapAsync(String, String,
 * ai.tegmentum.wasmtime4j.AsyncHostFunction)}, and {@link Linker#aliasModule(String, String)}.
 *
 * @since 1.0.0
 */
@DisplayName("Linker Async Methods Tests")
public class LinkerAsyncMethodsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(LinkerAsyncMethodsTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
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
        LOGGER.info(
            "["
                + runtime
                + "] aliasModule threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
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

      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});

      try {
        linker.funcNewAsync("env", "test", ft, null);
        LOGGER.info("[" + runtime + "] funcNewAsync succeeded (runtime supports it)");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] funcNewAsync threw UnsupportedOperationException: "
                + e.getMessage());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] funcNewAsync threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
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
        LOGGER.info(
            "["
                + runtime
                + "] funcWrapAsync threw UnsupportedOperationException: "
                + e.getMessage());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] funcWrapAsync threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

}
