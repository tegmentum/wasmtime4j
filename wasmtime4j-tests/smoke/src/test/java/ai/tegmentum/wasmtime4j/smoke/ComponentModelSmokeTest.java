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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wit.WitS32;
import java.io.InputStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for the Component Model subsystem.
 *
 * <p>Validates that a WebAssembly component can be compiled, instantiated, and invoked through the
 * unified public API on both JNI and Panama runtimes. Uses the direct {@link
 * Component#instantiate()} path which uses the same engine for compilation and instantiation.
 */
@DisplayName("Component Model Smoke Test")
public final class ComponentModelSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentModelSmokeTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "component compile and invoke for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should compile component, instantiate, and invoke add function")
  void shouldCompileAndInvokeComponentFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Testing component model with runtime: " + runtime);

    try (final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime)) {
      assertTrue(
          wasmRuntime.supportsComponentModel(),
          "Runtime " + runtime + " should support component model");

      final byte[] componentBytes;
      try (InputStream is = getClass().getResourceAsStream("/components/add.wasm")) {
        assertNotNull(is, "add.wasm test component should be on classpath");
        componentBytes = is.readAllBytes();
      }
      LOGGER.info("Loaded add.wasm: " + componentBytes.length + " bytes");

      try (final ComponentEngine componentEngine = wasmRuntime.createComponentEngine()) {
        assertNotNull(componentEngine, "ComponentEngine should not be null");
        assertTrue(componentEngine.isValid(), "ComponentEngine should be valid");
        LOGGER.info("ComponentEngine created successfully");

        try (final Component component = componentEngine.compileComponent(componentBytes)) {
          assertNotNull(component, "Compiled component should not be null");
          assertTrue(component.isValid(), "Component should be valid");
          LOGGER.info("Component compiled, id=" + component.getId());

          try (final ComponentInstance instance = component.instantiate()) {
            assertNotNull(instance, "ComponentInstance should not be null");
            assertTrue(instance.isValid(), "ComponentInstance should be valid");
            LOGGER.info(
                "Component instantiated, exported functions: " + instance.getExportedFunctions());

            final Object result = instance.invoke("add", WitS32.of(3), WitS32.of(4));
            assertNotNull(result, "Result should not be null");
            LOGGER.info("add(3, 4) = " + result + " (type: " + result.getClass().getName() + ")");

            assertEquals(7, result, "add(3, 4) should equal 7");
            LOGGER.info("Component model invoke succeeded: add(3, 4) = " + result);
          }
        }
      }
    }
    LOGGER.info("Component model smoke test passed for runtime: " + runtime);
  }
}
