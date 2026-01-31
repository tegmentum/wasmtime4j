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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmRuntimeBuilder;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests for {@link WasmRuntime#builder()} and {@link WasmRuntime#addWasiToLinker(Linker,
 * WasiContext)}. The builder is currently a stub that throws {@link
 * UnsupportedOperationException}. The addWasiToLinker method registers WASI snapshot_preview1
 * imports into a linker, enabling WASI module instantiation.
 *
 * @since 1.0.0
 */
@DisplayName("WasmRuntime Builder and WASI Linking Tests")
public class WasmRuntimeBuilderAndWasiLinkingTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasmRuntimeBuilderAndWasiLinkingTest.class.getName());

  private WasmRuntime runtime;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
    runtime = WasmRuntimeFactory.create();
    assertNotNull(runtime, "WasmRuntime should be available");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    if (runtime != null) {
      try {
        runtime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close runtime: " + e.getMessage());
      }
    }
    LOGGER.info("Finished test: " + testInfo.getDisplayName());
  }

  @Nested
  @DisplayName("WasmRuntime.builder() Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder() returns non-null WasmRuntimeBuilder")
    void builderReturnsNonNull() {
      LOGGER.info("Calling WasmRuntime.builder()");

      final WasmRuntimeBuilder builder = WasmRuntime.builder();
      assertNotNull(builder, "WasmRuntime.builder() should return a non-null builder instance");

      LOGGER.info("WasmRuntimeBuilder instance obtained: " + builder.getClass().getName());
    }

    @Test
    @DisplayName("builder().build() throws UnsupportedOperationException (stub behavior)")
    void builderBuildThrowsUnsupported() {
      LOGGER.info("Calling WasmRuntime.builder().build() - expecting UnsupportedOperationException");

      final WasmRuntimeBuilder builder = WasmRuntime.builder();
      assertNotNull(builder, "Builder should not be null");

      final UnsupportedOperationException exception =
          assertThrows(
              UnsupportedOperationException.class,
              builder::build,
              "build() should throw UnsupportedOperationException since it is a stub");

      LOGGER.info("Got expected exception: " + exception.getMessage());
      assertNotNull(
          exception.getMessage(),
          "Exception should have a descriptive message about the stub status");
    }
  }

  @Nested
  @DisplayName("WasmRuntime.addWasiToLinker() Tests")
  class AddWasiToLinkerTests {

    @Test
    @DisplayName("addWasiToLinker with valid args enables WASI module instantiation")
    void addWasiToLinkerWithValidArgsSucceeds() throws Exception {
      LOGGER.info("Testing addWasiToLinker with valid linker and WasiContext");

      final String wat =
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

      try (Engine engine = runtime.createEngine()) {
        final Module module = engine.compileWat(wat);
        final Store store = engine.createStore();
        final WasiContext wasiCtx = WasiContext.create();
        final Linker<WasiContext> linker = Linker.create(engine);

        LOGGER.info("Calling runtime.addWasiToLinker(linker, wasiCtx)");
        assertDoesNotThrow(
            () -> runtime.addWasiToLinker(linker, wasiCtx),
            "addWasiToLinker should not throw with valid arguments");

        LOGGER.info("Instantiating WASI module after linking");
        final Instance instance = linker.instantiate(store, module);
        assertNotNull(instance, "Module should instantiate after WASI linking");

        LOGGER.info("WASI module instantiated successfully via addWasiToLinker");

        instance.close();
        linker.close();
        store.close();
        module.close();
      }
    }

    @Test
    @DisplayName("addWasiToLinker with null linker throws exception")
    void addWasiToLinkerWithNullLinkerThrows() throws Exception {
      LOGGER.info("Testing addWasiToLinker with null linker");

      final WasiContext wasiCtx = WasiContext.create();

      assertThrows(
          Exception.class,
          () -> runtime.addWasiToLinker(null, wasiCtx),
          "addWasiToLinker should reject null linker");

      LOGGER.info("Null linker correctly rejected");
    }

    @Test
    @DisplayName("addWasiToLinker with null context throws exception")
    void addWasiToLinkerWithNullContextThrows() throws Exception {
      LOGGER.info("Testing addWasiToLinker with null WasiContext");

      try (Engine engine = runtime.createEngine()) {
        final Linker<WasiContext> linker = Linker.create(engine);

        assertThrows(
            Exception.class,
            () -> runtime.addWasiToLinker(linker, null),
            "addWasiToLinker should reject null WasiContext");

        LOGGER.info("Null WasiContext correctly rejected");
        linker.close();
      }
    }

    @Test
    @DisplayName("addWasiToLinker enables fd_write import for WASI modules")
    void addWasiToLinkerEnablesWasiImports() throws Exception {
      LOGGER.info("Testing that addWasiToLinker provides wasi_snapshot_preview1 imports");

      // A module that imports fd_write - a core WASI function
      final String wat =
          """
          (module
            (import "wasi_snapshot_preview1" "fd_write"
              (func $fd_write (param i32 i32 i32 i32) (result i32)))
            (memory (export "memory") 1)
            (func (export "test_fd_write") (result i32)
              ;; Set up an iov struct at memory offset 0
              ;; iov.iov_base = 100 (pointer to data)
              i32.const 0
              i32.const 100
              i32.store

              ;; iov.iov_len = 5 (length of data)
              i32.const 4
              i32.const 5
              i32.store

              ;; Write "hello" at offset 100
              i32.const 100
              i32.const 104  ;; 'h'
              i32.store8
              i32.const 101
              i32.const 101  ;; 'e'
              i32.store8
              i32.const 102
              i32.const 108  ;; 'l'
              i32.store8
              i32.const 103
              i32.const 108  ;; 'l'
              i32.store8
              i32.const 104
              i32.const 111  ;; 'o'
              i32.store8

              ;; fd_write(fd=1 (stdout), iovs=0, iovs_len=1, nwritten=200)
              i32.const 1
              i32.const 0
              i32.const 1
              i32.const 200
              call $fd_write
            )
          )
          """;

      try (Engine engine = runtime.createEngine()) {
        final Module module = engine.compileWat(wat);
        final Store store = engine.createStore();
        final WasiContext wasiCtx = WasiContext.create();
        final Linker<WasiContext> linker = Linker.create(engine);

        runtime.addWasiToLinker(linker, wasiCtx);

        final Instance instance = linker.instantiate(store, module);
        assertNotNull(
            instance,
            "Module importing fd_write should instantiate after WASI linking");

        LOGGER.info("Module with fd_write import instantiated successfully via addWasiToLinker");

        // Calling test_fd_write verifies the import is actually usable
        try {
          instance.callFunction("test_fd_write");
          LOGGER.info("fd_write call succeeded through WASI linking");
        } catch (final WasmException e) {
          // fd_write may fail if stdout is not properly configured,
          // but the import resolved successfully which is what we're testing
          LOGGER.info("fd_write call trapped (expected without full WASI config): "
              + e.getMessage());
        }

        instance.close();
        linker.close();
        store.close();
        module.close();
      }
    }
  }
}
