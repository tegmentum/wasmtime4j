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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
 * unified public API on both JNI and Panama runtimes.
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

    try (final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime)) {
      assumeTrue(
          wasmRuntime.supportsComponentModel(),
          "Component model not supported for runtime: " + runtime);

      final byte[] componentBytes;
      try (InputStream is = getClass().getResourceAsStream("/components/add.wasm")) {
        assertNotNull(is, "add.wasm test component should be on classpath");
        componentBytes = is.readAllBytes();
      }

      try (final ComponentEngine componentEngine = wasmRuntime.createComponentEngine();
          final Component component = componentEngine.compileComponent(componentBytes);
          final ComponentInstance instance = component.instantiate()) {

        final Object result = instance.invoke("add", WitS32.of(3), WitS32.of(4));
        assertEquals(7, result, "add(3, 4) should equal 7");
        LOGGER.info("Component add(3, 4) = " + result);
      }
    }
  }
}
