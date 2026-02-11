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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * End-to-end tests for WASI fd_write and fd_read operations. Verifies that WASM modules can call
 * fd_write/fd_read WASI syscalls and that the WasiContext I/O APIs (enableOutputCapture,
 * getStdoutCapture, getStderrCapture, setStdinBytes) are functional.
 *
 * @since 1.0.0
 */
@DisplayName("WASI I/O End-to-End Tests")
public class WasiIoEndToEndTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiIoEndToEndTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fd_write to stdout returns WASI_ESUCCESS")
  void fdWriteToStdoutSucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing fd_write to stdout");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; iov structure at offset 0: ptr=100, len=12
          (data (i32.const 0) "\\64\\00\\00\\00\\0c\\00\\00\\00")
          ;; Message at offset 100
          (data (i32.const 100) "Hello, WASI!")

          (func (export "write_hello") (result i32)
            i32.const 1      ;; fd=stdout
            i32.const 0      ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 200    ;; nwritten ptr
            call $fd_write
          )

          (func (export "get_nwritten") (result i32)
            i32.const 200
            i32.load
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.enableOutputCapture();

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);
      final WasmValue[] results = instance.callFunction("write_hello");
      assertEquals(0, results[0].asInt(), "fd_write should return WASI_ESUCCESS (0)");

      final int nwritten = instance.callFunction("get_nwritten")[0].asInt();
      LOGGER.info("[" + runtime + "] fd_write nwritten: " + nwritten);
      assertEquals(12, nwritten, "Should have written 12 bytes");

      // Verify output capture contains the written data
      final byte[] captured = wasiCtx.getStdoutCapture();
      assertNotNull(captured, "stdout capture should not be null when output capture is enabled");
      final String output = new String(captured, StandardCharsets.UTF_8);
      LOGGER.info("[" + runtime + "] Captured stdout: '" + output + "'");
      assertEquals("Hello, WASI!", output, "Captured output should match written data");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fd_write to stderr returns WASI_ESUCCESS")
  void fdWriteToStderrSucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing fd_write to stderr");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; iov structure at offset 0: ptr=100, len=11
          (data (i32.const 0) "\\64\\00\\00\\00\\0b\\00\\00\\00")
          ;; Error message at offset 100
          (data (i32.const 100) "Error: test")

          (func (export "write_error") (result i32)
            i32.const 2      ;; fd=stderr
            i32.const 0      ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 200    ;; nwritten ptr
            call $fd_write
          )

          (func (export "get_nwritten") (result i32)
            i32.const 200
            i32.load
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.enableOutputCapture();

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);
      final WasmValue[] results = instance.callFunction("write_error");
      assertEquals(0, results[0].asInt(), "fd_write to stderr should return WASI_ESUCCESS (0)");

      final int nwritten = instance.callFunction("get_nwritten")[0].asInt();
      LOGGER.info("[" + runtime + "] fd_write stderr nwritten: " + nwritten);
      assertEquals(11, nwritten, "Should have written 11 bytes");

      // Verify stderr capture contains the written data
      final byte[] captured = wasiCtx.getStderrCapture();
      assertNotNull(captured, "stderr capture should not be null when output capture is enabled");
      final String stderrOutput = new String(captured, StandardCharsets.UTF_8);
      LOGGER.info("[" + runtime + "] Captured stderr: '" + stderrOutput + "'");
      assertEquals("Error: test", stderrOutput, "Captured stderr should match written data");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fd_read from stdin with provided bytes")
  void fdReadFromStdinWithProvidedBytes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing fd_read from stdin");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_read"
            (func $fd_read (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; iov structure at offset 0: ptr=100, len=64
          (data (i32.const 0) "\\64\\00\\00\\00\\40\\00\\00\\00")

          (func (export "read_stdin") (result i32)
            i32.const 0      ;; fd=stdin
            i32.const 0      ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 200    ;; nread ptr
            call $fd_read
          )

          (func (export "get_nread") (result i32)
            i32.const 200
            i32.load
          )
        )
        """;

    final String inputData = "Hello from Java!";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.setStdinBytes(inputData.getBytes(StandardCharsets.UTF_8));

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);
      final WasmValue[] results = instance.callFunction("read_stdin");
      final int errno = results[0].asInt();
      LOGGER.info("[" + runtime + "] fd_read errno: " + errno);
      assertEquals(0, errno, "fd_read should return WASI_ESUCCESS (0)");

      final int nread = instance.callFunction("get_nread")[0].asInt();
      LOGGER.info("[" + runtime + "] fd_read nread: " + nread);

      // Verify stdin bytes were read correctly
      assertEquals(
          inputData.length(), nread, "Should have read all " + inputData.length() + " input bytes");

      final var memory = instance.getMemory("memory");
      assertNotNull(memory.orElse(null), "Memory export should be present");
      final byte[] readData = new byte[nread];
      memory.get().readBytes(100, readData, 0, nread);
      final String readString = new String(readData, StandardCharsets.UTF_8);
      LOGGER.info("[" + runtime + "] Read from stdin: '" + readString + "'");
      assertEquals(inputData, readString, "Read data should match provided stdin bytes");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fd_write to stdout and stderr in same module")
  void fdWriteToBothStreams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing fd_write to both stdout and stderr");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; stdout iov at offset 0: ptr=200, len=6
          (data (i32.const 0) "\\c8\\00\\00\\00\\06\\00\\00\\00")
          ;; stdout message at offset 200
          (data (i32.const 200) "stdout")

          ;; stderr iov at offset 16: ptr=300, len=6
          (data (i32.const 16) "\\2c\\01\\00\\00\\06\\00\\00\\00")
          ;; stderr message at offset 300
          (data (i32.const 300) "stderr")

          (func (export "write_both") (result i32)
            ;; Write to stdout
            i32.const 1      ;; fd=stdout
            i32.const 0      ;; iovs ptr (stdout iov)
            i32.const 1      ;; iovs_len
            i32.const 400    ;; nwritten ptr
            call $fd_write
            drop

            ;; Write to stderr
            i32.const 2      ;; fd=stderr
            i32.const 16     ;; iovs ptr (stderr iov)
            i32.const 1      ;; iovs_len
            i32.const 404    ;; nwritten ptr
            call $fd_write
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.enableOutputCapture();

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);
      final WasmValue[] results = instance.callFunction("write_both");
      assertEquals(0, results[0].asInt(), "fd_write to stderr should return WASI_ESUCCESS (0)");

      // Verify both stdout and stderr captures contain the written data
      final byte[] stdoutCapture = wasiCtx.getStdoutCapture();
      final byte[] stderrCapture = wasiCtx.getStderrCapture();

      assertNotNull(stdoutCapture, "stdout capture should not be null when output capture is enabled");
      assertNotNull(stderrCapture, "stderr capture should not be null when output capture is enabled");

      final String stdoutContent = new String(stdoutCapture, StandardCharsets.UTF_8);
      final String stderrContent = new String(stderrCapture, StandardCharsets.UTF_8);
      LOGGER.info(
          "[" + runtime + "] stdout='" + stdoutContent + "', stderr='" + stderrContent + "'");
      assertEquals("stdout", stdoutContent, "Captured stdout should match written data");
      assertEquals("stderr", stderrContent, "Captured stderr should match written data");

      instance.close();
      linker.close();
      module.close();
    }
  }
}
