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

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test that validates native library loading for each runtime.
 *
 * <p>Verifies that the native Wasmtime library can be loaded and a runtime instance created
 * successfully. This is the most fundamental smoke test — if this fails, nothing else will work.
 */
@DisplayName("Native Library Loading Smoke Test")
public final class NativeLibraryLoadingSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(NativeLibraryLoadingSmokeTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "native library available for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("native library should be available")
  void nativeLibraryShouldBeAvailable(final RuntimeType runtime) {
    setRuntime(runtime);

    LOGGER.info("Checking native library availability for runtime: " + runtime);
    final boolean available = WasmRuntimeFactory.isRuntimeAvailable(runtime);
    LOGGER.info("Runtime " + runtime + " available: " + available);

    assertTrue(available, "Runtime " + runtime + " should be available");
  }

  @ParameterizedTest(name = "runtime creation succeeds for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("runtime creation should succeed")
  void runtimeCreationShouldSucceed(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Creating runtime for: " + runtime);
    try (final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime)) {
      assertNotNull(wasmRuntime, "Created runtime should not be null for " + runtime);
      LOGGER.info("Successfully created runtime: " + wasmRuntime.getClass().getName());
    }
  }
}
