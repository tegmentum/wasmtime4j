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

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for WebAssembly module compilation.
 *
 * <p>Validates that a WAT text module can be compiled into a valid Module object.
 */
@DisplayName("Module Compilation Smoke Test")
public final class ModuleCompilationSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleCompilationSmokeTest.class.getName());

  private static final String SIMPLE_MODULE_WAT = "(module (func (export \"f\")))";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "WAT compilation succeeds for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WAT module should compile successfully")
  void watModuleShouldCompileSuccessfully(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final Engine engine = Engine.create();
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT)) {
      assertTrue(module.isValid(), "Compiled module should be valid");
      assertTrue(module.hasExport("f"), "Module should export function 'f'");
      LOGGER.info("Module compiled, exports=" + module.getExports().size());
    }
  }
}
