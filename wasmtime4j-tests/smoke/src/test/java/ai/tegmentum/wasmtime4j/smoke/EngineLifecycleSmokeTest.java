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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for Engine creation and lifecycle management.
 *
 * <p>Validates that an Engine can be created with default configuration and closed without errors.
 */
@DisplayName("Engine Lifecycle Smoke Test")
public final class EngineLifecycleSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(EngineLifecycleSmokeTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "engine create and close for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine should create and close without error")
  void engineShouldCreateAndCloseWithoutError(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Creating engine with runtime: " + runtime);
    try (final Engine engine = Engine.create()) {
      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine.isValid(), "Engine should be valid after creation");
      LOGGER.info("Engine created successfully, valid=" + engine.isValid());
    }
    LOGGER.info("Engine closed successfully for runtime: " + runtime);
  }

  @ParameterizedTest(name = "engine double close is safe for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine double close should not throw")
  void engineDoubleCloseShouldNotThrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Testing double close safety for runtime: " + runtime);
    final Engine engine = Engine.create();
    engine.close();
    assertDoesNotThrow(engine::close, "Double close should not throw");
    LOGGER.info("Double close completed safely for runtime: " + runtime);
  }
}
