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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests that closing resources in incorrect dependency order does not crash the JVM. Resources have
 * implicit dependencies: Instance depends on Store and Module, Store depends on Engine, etc.
 * Closing in the wrong order should be handled gracefully.
 *
 * @since 1.0.0
 */
@DisplayName("Out-of-Order Close Tests")
public class OutOfOrderCloseTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(OutOfOrderCloseTest.class.getName());

  private static final String ADD_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Close Engine before Store should not crash")
  void closeEngineBeforeStore(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing close Engine before Store");

    final Engine engine = Engine.create();
    final Store store = engine.createStore();

    LOGGER.info("[" + runtime + "] Closing Engine first (Store still alive)");
    assertDoesNotThrow(engine::close, "Closing Engine before Store should not crash");

    LOGGER.info("[" + runtime + "] Now closing Store");
    assertDoesNotThrow(store::close, "Closing Store after Engine should not crash");

    LOGGER.info("[" + runtime + "] Both closed without crash");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Close Store before Instance should not crash")
  void closeStoreBeforeInstance(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing close Store before Instance");

    final Engine engine = Engine.create();
    final Module module = engine.compileWat(ADD_WAT);
    final Store store = engine.createStore();
    final Instance instance = module.instantiate(store);

    LOGGER.info("[" + runtime + "] Closing Store first (Instance still alive)");
    assertDoesNotThrow(store::close, "Closing Store before Instance should not crash");

    LOGGER.info("[" + runtime + "] Now closing Instance");
    assertDoesNotThrow(instance::close, "Closing Instance after Store should not crash");

    module.close();
    engine.close();
    LOGGER.info("[" + runtime + "] All closed without crash");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Close Module before Instance that uses it should not crash")
  void closeModuleBeforeInstance(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing close Module before Instance");

    final Engine engine = Engine.create();
    final Module module = engine.compileWat(ADD_WAT);
    final Store store = engine.createStore();
    final Instance instance = module.instantiate(store);

    LOGGER.info("[" + runtime + "] Closing Module first (Instance still alive)");
    assertDoesNotThrow(module::close, "Closing Module before Instance should not crash");

    LOGGER.info("[" + runtime + "] Now closing Instance");
    assertDoesNotThrow(instance::close, "Closing Instance after Module should not crash");

    store.close();
    engine.close();
    LOGGER.info("[" + runtime + "] All closed without crash");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Close Linker before Instance it created should not crash")
  void closeLinkerBeforeInstance(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing close Linker before Instance");

    final Engine engine = Engine.create();
    final Module module =
        engine.compileWat(
            """
            (module
              (func (export "noop")))
            """);
    final Store store = engine.createStore();
    final Linker<Void> linker = Linker.create(engine);
    final Instance instance = linker.instantiate(store, module);

    LOGGER.info("[" + runtime + "] Closing Linker first (Instance still alive)");
    assertDoesNotThrow(linker::close, "Closing Linker before Instance should not crash");

    LOGGER.info("[" + runtime + "] Now closing Instance");
    assertDoesNotThrow(instance::close, "Closing Instance after Linker should not crash");

    module.close();
    store.close();
    engine.close();
    LOGGER.info("[" + runtime + "] All closed without crash");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Close Engine while Module still alive should not crash")
  void closeEngineWhileModuleAlive(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing close Engine while Module alive");

    final Engine engine = Engine.create();
    final Module module = engine.compileWat(ADD_WAT);

    LOGGER.info("[" + runtime + "] Closing Engine first (Module still alive)");
    assertDoesNotThrow(engine::close, "Closing Engine while Module alive should not crash");

    LOGGER.info("[" + runtime + "] Now closing Module");
    assertDoesNotThrow(module::close, "Closing Module after Engine should not crash");

    LOGGER.info("[" + runtime + "] Both closed without crash");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Close all in reverse-creation order should work")
  void closeAllInReverseCreationOrder(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing close in reverse-creation order (correct order)");

    final Engine engine = Engine.create();
    final Module module = engine.compileWat(ADD_WAT);
    final Store store = engine.createStore();
    final Instance instance = module.instantiate(store);

    LOGGER.info("[" + runtime + "] Closing Instance -> Store -> Module -> Engine");
    assertDoesNotThrow(instance::close, "Instance close should not crash");
    assertDoesNotThrow(store::close, "Store close should not crash");
    assertDoesNotThrow(module::close, "Module close should not crash");
    assertDoesNotThrow(engine::close, "Engine close should not crash");

    LOGGER.info("[" + runtime + "] All closed in reverse order without crash");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Close all in creation order should not crash")
  void closeAllInCreationOrder(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing close in creation order (wrong order)");

    final Engine engine = Engine.create();
    final Module module = engine.compileWat(ADD_WAT);
    final Store store = engine.createStore();
    final Instance instance = module.instantiate(store);

    LOGGER.info("[" + runtime + "] Closing Engine -> Module -> Store -> Instance");
    assertDoesNotThrow(engine::close, "Engine close should not crash");
    assertDoesNotThrow(module::close, "Module close should not crash");
    assertDoesNotThrow(store::close, "Store close should not crash");
    assertDoesNotThrow(instance::close, "Instance close should not crash");

    LOGGER.info("[" + runtime + "] All closed in creation order without crash");
  }
}
