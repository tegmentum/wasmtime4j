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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for runtime auto-selection and manual override.
 *
 * <p>Validates that the runtime factory can auto-detect an available runtime and that manual
 * selection via system properties works correctly.
 */
@DisplayName("Runtime Selection Smoke Test")
public final class RuntimeSelectionSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(RuntimeSelectionSmokeTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Test
  @DisplayName("auto-selected runtime type should be non-null")
  void autoSelectedRuntimeTypeShouldBeNonNull() {
    clearRuntimeSelection();

    LOGGER.info("Testing auto-selection of runtime type");
    final RuntimeType selectedType = WasmRuntimeFactory.getSelectedRuntimeType();
    assertNotNull(selectedType, "Auto-selected runtime type should not be null");
    LOGGER.info("Auto-selected runtime type: " + selectedType);
  }

  @Test
  @DisplayName("auto-selected runtime should create engine")
  void autoSelectedRuntimeShouldCreateEngine() throws Exception {
    clearRuntimeSelection();

    LOGGER.info("Testing engine creation with auto-selected runtime");
    try (final Engine engine = Engine.create()) {
      assertNotNull(engine, "Engine should not be null with auto-selected runtime");
      assertTrue(engine.isValid(), "Engine should be valid with auto-selected runtime");
      LOGGER.info("Engine created with auto-selected runtime: " + engine.getClass().getName());
    }
  }

  @ParameterizedTest(name = "manual runtime selection for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("manually selected runtime should create engine")
  void manuallySelectedRuntimeShouldCreateEngine(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Testing manual runtime selection for: " + runtime);
    try (final Engine engine = Engine.create()) {
      assertNotNull(engine, "Engine should not be null for runtime " + runtime);
      assertTrue(engine.isValid(), "Engine should be valid for runtime " + runtime);
      LOGGER.info(
          "Engine created with manually selected runtime "
              + runtime
              + ": "
              + engine.getClass().getName());
    }
  }
}
