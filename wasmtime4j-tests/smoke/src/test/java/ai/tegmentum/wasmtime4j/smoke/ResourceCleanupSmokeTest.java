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
package ai.tegmentum.wasmtime4j.smoke;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
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
 * Smoke test for resource cleanup and lifecycle management.
 *
 * <p>Validates that all major resource types (Engine, Store, Module, Instance) can be created and
 * closed in reverse order without exceptions.
 */
@DisplayName("Resource Cleanup Smoke Test")
public final class ResourceCleanupSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ResourceCleanupSmokeTest.class.getName());

  private static final String SIMPLE_WAT =
      """
      (module
        (func (export "noop"))
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "resource cleanup for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("all resources should close cleanly in reverse order")
  void allResourcesShouldCloseCleanlyInReverseOrder(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(SIMPLE_WAT);
    final Instance instance = module.instantiate(store);

    assertDoesNotThrow(instance::close, "Instance close should not throw");
    assertDoesNotThrow(module::close, "Module close should not throw");
    assertDoesNotThrow(store::close, "Store close should not throw");
    assertDoesNotThrow(engine::close, "Engine close should not throw");
    LOGGER.info("All resources closed cleanly for runtime: " + runtime);
  }
}
