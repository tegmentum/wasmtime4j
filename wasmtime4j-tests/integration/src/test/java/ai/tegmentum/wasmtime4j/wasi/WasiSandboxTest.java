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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests that WASI sandbox cannot be escaped. Verifies that path traversal, symlink escape, access
 * to non-preopened directories, and write to read-only directories are blocked.
 *
 * @since 1.0.0
 */
@DisplayName("WASI Sandbox Escape Prevention Tests")
public class WasiSandboxTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiSandboxTest.class.getName());

  @TempDir Path tempDir;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  /**
   * WAT module that attempts path_open on a given guest path. Returns the fd_prestat_get result
   * code (0 = success, nonzero = error). The module attempts to open a path using fd_prestat_get to
   * check if preopen is available, then uses path_open to try accessing files.
   */
  private static final String PATH_OPEN_WAT =
      """
      (module
        (import "wasi_snapshot_preview1" "fd_prestat_get"
          (func $fd_prestat_get (param i32 i32) (result i32)))
        (import "wasi_snapshot_preview1" "path_open"
          (func $path_open (param i32 i32 i32 i32 i32 i64 i64 i32 i32) (result i32)))
        (memory (export "memory") 1)

        ;; Attempt to open a path relative to fd=3 (first preopened dir)
        ;; Path stored at memory offset 100 with length in param
        (func (export "try_open") (param $path_len i32) (result i32)
          i32.const 3        ;; dirfd (first preopened)
          i32.const 0        ;; dirflags
          i32.const 100      ;; path offset in memory
          local.get $path_len ;; path length
          i32.const 0        ;; oflags
          i64.const 0        ;; fs_rights_base
          i64.const 0        ;; fs_rights_inheriting
          i32.const 0        ;; fdflags
          i32.const 200      ;; fd output ptr
          call $path_open
        )

        ;; Check if fd=3 is a valid preopened directory
        (func (export "check_preopen") (result i32)
          i32.const 3        ;; fd
          i32.const 0        ;; buf
          call $fd_prestat_get
        )
      )
      """;

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Path traversal with ../ should be blocked")
  void pathTraversalShouldBeBlocked(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing path traversal prevention");

    final Path sandboxDir = tempDir.resolve("sandbox");
    Files.createDirectories(sandboxDir);
    Files.writeString(sandboxDir.resolve("allowed.txt"), "safe content");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(PATH_OPEN_WAT);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.preopenedDir(sandboxDir, "/sandbox");

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);

      // Write path traversal string to memory offset 100: "../../../etc/passwd"
      final byte[] traversalPath = "../../../etc/passwd".getBytes();
      final var memory = instance.getMemory("memory");
      assertTrue(memory.isPresent(), "Memory export should be present");
      for (int i = 0; i < traversalPath.length; i++) {
        memory.get().writeByte(100 + i, traversalPath[i]);
      }

      // Try to open the traversal path
      final WasmValue[] result =
          instance.callFunction("try_open", WasmValue.i32(traversalPath.length));
      final int errno = result[0].asInt();
      LOGGER.info("[" + runtime + "] Path traversal result (errno): " + errno);

      // Non-zero errno means access was denied (WASI_EACCES=2, WASI_ENOTCAPABLE=76, etc.)
      assertNotEquals(0, errno, "Path traversal should be blocked (non-zero errno expected)");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Access to non-preopened directory should be blocked")
  void accessToNonPreopenedDirectoryShouldBeBlocked(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing non-preopened directory access prevention");

    final Path sandboxDir = tempDir.resolve("sandbox");
    Files.createDirectories(sandboxDir);

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(PATH_OPEN_WAT);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.preopenedDir(sandboxDir, "/sandbox");

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);

      // Write path to memory: try to access /tmp (not preopened)
      final byte[] nonPreopenedPath = "/tmp/secret".getBytes();
      final var memory = instance.getMemory("memory");
      assertTrue(memory.isPresent(), "Memory export should be present");
      for (int i = 0; i < nonPreopenedPath.length; i++) {
        memory.get().writeByte(100 + i, nonPreopenedPath[i]);
      }

      // Try to open the non-preopened path
      final WasmValue[] result =
          instance.callFunction("try_open", WasmValue.i32(nonPreopenedPath.length));
      final int errno = result[0].asInt();
      LOGGER.info("[" + runtime + "] Non-preopened access result (errno): " + errno);

      assertNotEquals(0, errno, "Non-preopened directory access should be blocked");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Write to read-only preopened directory should be blocked")
  void writeToReadOnlyDirectoryShouldBeBlocked(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing read-only directory write prevention");

    final Path readOnlyDir = tempDir.resolve("readonly");
    Files.createDirectories(readOnlyDir);
    Files.writeString(readOnlyDir.resolve("existing.txt"), "read only content");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "path_open"
            (func $path_open (param i32 i32 i32 i32 i32 i64 i64 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; filename at offset 100
          (data (i32.const 100) "newfile.txt")

          (func (export "try_create") (result i32)
            i32.const 3        ;; dirfd (first preopened)
            i32.const 0        ;; dirflags
            i32.const 100      ;; path offset
            i32.const 11       ;; path length "newfile.txt"
            i32.const 1        ;; oflags: CREAT
            i64.const 64       ;; fs_rights_base: FD_WRITE
            i64.const 0        ;; fs_rights_inheriting
            i32.const 0        ;; fdflags
            i32.const 200      ;; fd output ptr
            call $path_open
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.preopenedDirReadOnly(readOnlyDir, "/readonly");

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);
      final WasmValue[] result = instance.callFunction("try_create");
      final int errno = result[0].asInt();
      LOGGER.info("[" + runtime + "] Write to read-only result (errno): " + errno);

      assertNotEquals(0, errno, "Write to read-only directory should be blocked");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Symlink escape from preopened directory should be blocked")
  void symlinkEscapeShouldBeBlocked(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing symlink escape prevention");

    final Path sandboxDir = tempDir.resolve("sandbox");
    Files.createDirectories(sandboxDir);

    // Create a symlink inside sandbox pointing outside
    final Path symlinkTarget = tempDir.resolve("outside");
    Files.createDirectories(symlinkTarget);
    Files.writeString(symlinkTarget.resolve("secret.txt"), "should not be readable");
    final Path symlink = sandboxDir.resolve("escape_link");
    try {
      Files.createSymbolicLink(symlink, symlinkTarget);
    } catch (final Exception e) {
      LOGGER.info("[" + runtime + "] Cannot create symlinks on this platform, skipping");
      return;
    }

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(PATH_OPEN_WAT);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.preopenedDir(sandboxDir, "/sandbox");

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);

      // Write symlink path to memory: "escape_link/secret.txt"
      final byte[] symlinkPath = "escape_link/secret.txt".getBytes();
      final var memory = instance.getMemory("memory");
      assertTrue(memory.isPresent(), "Memory export should be present");
      for (int i = 0; i < symlinkPath.length; i++) {
        memory.get().writeByte(100 + i, symlinkPath[i]);
      }

      // Try to open through the symlink
      final WasmValue[] result =
          instance.callFunction("try_open", WasmValue.i32(symlinkPath.length));
      final int errno = result[0].asInt();
      LOGGER.info("[" + runtime + "] Symlink escape result (errno): " + errno);

      // Wasmtime blocks symlink escapes with WASI_ENOTCAPABLE or WASI_EACCES
      assertNotEquals(0, errno, "Symlink escape should be blocked");

      instance.close();
      linker.close();
      module.close();
    }
  }
}
