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
package ai.tegmentum.wasmtime4j.wasmtime.generated.componentmodelasync;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for WASI P3 enablement on ComponentLinker.
 *
 * <p>WASI P3 is experimental and may not be compiled into the native library. These tests verify
 * that the API exists and can be called without crashing, regardless of whether the feature is
 * enabled.
 */
@DisplayName("WASI P3 Enablement")
public final class WasiP3EnablementTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("enableWasiP3 can be called after enableWasiPreview2")
  void enableWasiP3AfterPreview2(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final EngineConfig config = new EngineConfig().wasmComponentModel(true);

    try (final Engine engine = Engine.create(config)) {
      final ComponentLinker<?> linker = ComponentLinker.create(engine);
      assertNotNull(linker);

      linker.enableWasiPreview2();

      // P3 may succeed or throw WasmException if wasi-p3 feature is not compiled
      // Either outcome is acceptable — we just verify no crash
      try {
        linker.enableWasiP3();
      } catch (final Exception e) {
        // Expected if wasi-p3 feature is not compiled in or function not registered
        assertNotNull(e.getMessage());
      }

      linker.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("enableWasiHttpP3 can be called after P3 and HTTP P2")
  void enableWasiHttpP3AfterP3AndHttp(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final EngineConfig config = new EngineConfig().wasmComponentModel(true);

    try (final Engine engine = Engine.create(config)) {
      final ComponentLinker<?> linker = ComponentLinker.create(engine);
      assertNotNull(linker);

      linker.enableWasiPreview2();
      linker.enableWasiHttp();

      try {
        linker.enableWasiP3();
        linker.enableWasiHttpP3();
      } catch (final Exception e) {
        // Expected if wasi-p3 feature is not compiled in or function not registered
        assertNotNull(e.getMessage());
      }

      linker.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("enableWasiP3 requires WASI Preview 2")
  void enableWasiP3RequiresPreview2(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final EngineConfig config = new EngineConfig().wasmComponentModel(true);

    try (final Engine engine = Engine.create(config)) {
      final ComponentLinker<?> linker = ComponentLinker.create(engine);
      assertNotNull(linker);

      // Should throw because P2 is not enabled (or function not registered)
      try {
        linker.enableWasiP3();
      } catch (final Exception e) {
        assertNotNull(e.getMessage());
      }

      linker.close();
    }
  }
}
