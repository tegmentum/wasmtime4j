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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for WASI (WebAssembly System Interface) support.
 *
 * <p>Validates that a WASI context can be created, linked, and used to instantiate a module with
 * WASI imports.
 */
@DisplayName("WASI Smoke Test")
public final class WasiSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiSmokeTest.class.getName());

  private static final String WASI_MODULE_WAT =
      """
      (module
        (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
        (memory (export "memory") 1)
        (func (export "_start")
          i32.const 0
          call $proc_exit
        )
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "WASI instantiation succeeds for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI module should instantiate successfully")
  void wasiModuleShouldInstantiateSuccessfully(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Module module = engine.compileWat(WASI_MODULE_WAT)) {

      final WasiContext wasiCtx = WasiContext.create();

      try (final Linker<WasiContext> linker = Linker.create(engine)) {
        WasiLinkerUtils.addToLinker(linker, wasiCtx);

        try (final Instance instance = linker.instantiate(store, module)) {
          LOGGER.info(
              "WASI module instantiated for " + runtime + ": " + instance.getClass().getName());
        }
      }
    }
  }
}
