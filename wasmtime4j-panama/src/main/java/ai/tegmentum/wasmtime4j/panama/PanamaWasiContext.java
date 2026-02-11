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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryPermissions;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panama FFI implementation of WasiContext.
 *
 * <p>Provides WASI context functionality using Panama Foreign Function API bindings to the native
 * Wasmtime WASI implementation.
 *
 * @since 1.0.0
 */
public final class PanamaWasiContext implements WasiContext {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final MemorySegment contextHandle;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Creates a new Panama WASI context. */
  public PanamaWasiContext() {
    this.contextHandle = NATIVE_BINDINGS.wasiContextCreate();
    if (contextHandle == null || contextHandle.equals(MemorySegment.NULL)) {
      throw new RuntimeException("Failed to create WASI context");
    }
  }

  /**
   * Gets the native context handle.
   *
   * @return the native context memory segment
   */
  public MemorySegment getNativeContext() {
    ensureNotClosed();
    return this.contextHandle;
  }

  @Override
  public WasiContext setArgv(final String[] argv) {
    if (argv == null) {
      throw new IllegalArgumentException("Command line arguments cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      // Allocate array of string pointers
      final MemorySegment argsArray = arena.allocate(ValueLayout.ADDRESS, argv.length);

      // Convert each string to C string and store pointer
      for (int i = 0; i < argv.length; i++) {
        final MemorySegment argStr = arena.allocateFrom(argv[i]);
        argsArray.setAtIndex(ValueLayout.ADDRESS, i, argStr);
      }

      final int result = NATIVE_BINDINGS.wasiContextSetArgv(contextHandle, argsArray, argv.length);
      if (result != 0) {
        throw new RuntimeException("Failed to set command line arguments");
      }
    }
    return this;
  }

  @Override
  public WasiContext setEnv(final String key, final String value) {
    if (key == null) {
      throw new IllegalArgumentException("Environment variable key cannot be null");
    }
    if (value == null) {
      throw new IllegalArgumentException("Environment variable value cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment keyStr = arena.allocateFrom(key);
      final MemorySegment valueStr = arena.allocateFrom(value);

      final int result = NATIVE_BINDINGS.wasiContextSetEnv(contextHandle, keyStr, valueStr);
      if (result != 0) {
        throw new RuntimeException("Failed to set environment variable");
      }
    }
    return this;
  }

  @Override
  public WasiContext setEnv(final Map<String, String> env) {
    if (env == null) {
      throw new IllegalArgumentException("Environment variables map cannot be null");
    }
    ensureNotClosed();

    for (Map.Entry<String, String> entry : env.entrySet()) {
      setEnv(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public WasiContext inheritEnv() {
    ensureNotClosed();

    final int result = NATIVE_BINDINGS.wasiContextInheritEnv(contextHandle);
    if (result != 0) {
      throw new RuntimeException("Failed to inherit environment variables");
    }
    return this;
  }

  @Override
  public WasiContext inheritStdio() {
    ensureNotClosed();

    final int result = NATIVE_BINDINGS.wasiContextInheritStdio(contextHandle);
    if (result != 0) {
      throw new RuntimeException("Failed to inherit stdio");
    }
    return this;
  }

  @Override
  public WasiContext setStdin(final Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stdin path cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment pathStr = arena.allocateFrom(path.toString());

      final int result = NATIVE_BINDINGS.wasiContextSetStdin(contextHandle, pathStr);
      if (result != 0) {
        throw new RuntimeException("Failed to set stdin path");
      }
    }
    return this;
  }

  @Override
  public WasiContext setStdinBytes(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Stdin data cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment dataSegment;
      final long dataLen;

      if (data.length == 0) {
        dataSegment = MemorySegment.NULL;
        dataLen = 0;
      } else {
        dataSegment = arena.allocate(data.length);
        dataSegment.copyFrom(MemorySegment.ofArray(data));
        dataLen = data.length;
      }

      final int result =
          NATIVE_BINDINGS.wasiContextSetStdinBytes(contextHandle, dataSegment, dataLen);
      if (result != 0) {
        throw new RuntimeException("Failed to set stdin bytes");
      }
    }
    return this;
  }

  @Override
  public WasiContext setStdout(final Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stdout path cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment pathStr = arena.allocateFrom(path.toString());

      final int result = NATIVE_BINDINGS.wasiContextSetStdout(contextHandle, pathStr);
      if (result != 0) {
        throw new RuntimeException("Failed to set stdout path");
      }
    }
    return this;
  }

  @Override
  public WasiContext setStderr(final Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stderr path cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment pathStr = arena.allocateFrom(path.toString());

      final int result = NATIVE_BINDINGS.wasiContextSetStderr(contextHandle, pathStr);
      if (result != 0) {
        throw new RuntimeException("Failed to set stderr path");
      }
    }
    return this;
  }

  @Override
  public WasiContext preopenedDir(final Path hostPath, final String guestPath)
      throws WasmException {
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment hostPathStr = arena.allocateFrom(hostPath.toString());
      final MemorySegment guestPathStr = arena.allocateFrom(guestPath);

      final int result =
          NATIVE_BINDINGS.wasiContextPreopenDir(contextHandle, hostPathStr, guestPathStr);
      if (result != 0) {
        throw new WasmException("Failed to add pre-opened directory");
      }
    }
    return this;
  }

  @Override
  public WasiContext preopenedDirReadOnly(final Path hostPath, final String guestPath)
      throws WasmException {
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment hostPathStr = arena.allocateFrom(hostPath.toString());
      final MemorySegment guestPathStr = arena.allocateFrom(guestPath);

      final int result =
          NATIVE_BINDINGS.wasiContextPreopenDirReadonly(contextHandle, hostPathStr, guestPathStr);
      if (result != 0) {
        throw new WasmException("Failed to add read-only pre-opened directory");
      }
    }
    return this;
  }

  @Override
  public WasiContext setWorkingDirectory(final String workingDir) {
    // Working directory support requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setNetworkEnabled(final boolean enabled) {
    // Network enabling requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setMaxOpenFiles(final int maxFds) {
    if (maxFds < -1) {
      throw new IllegalArgumentException("maxFds must be >= -1");
    }
    // Resource limiting requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setAsyncIoEnabled(final boolean enabled) {
    // Async I/O requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setMaxAsyncOperations(final int maxOps) {
    if (maxOps < -1) {
      throw new IllegalArgumentException("maxOps must be >= -1");
    }
    // Async operations limiting requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setAsyncTimeout(final long timeoutMs) {
    if (timeoutMs < -1) {
      throw new IllegalArgumentException("timeoutMs must be >= -1");
    }
    // Async timeout requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setComponentModelEnabled(final boolean enabled) {
    // Component Model support requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setProcessEnabled(final boolean enabled) {
    // Process operations require additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext setFilesystemWorkingDir(final Path workingDir) {
    if (workingDir == null) {
      throw new IllegalArgumentException("Working directory cannot be null");
    }
    // Filesystem working directory requires additional native binding
    // For now, this is a no-op that doesn't fail
    ensureNotClosed();
    return this;
  }

  @Override
  public WasiContext preopenedDirWithPermissions(
      final Path hostPath, final String guestPath, final WasiDirectoryPermissions permissions)
      throws WasmException {
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    if (permissions == null) {
      throw new IllegalArgumentException("Permissions cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment hostPathStr = arena.allocateFrom(hostPath.toString());
      final MemorySegment guestPathStr = arena.allocateFrom(guestPath);

      final int canRead = permissions.canRead() ? 1 : 0;
      final int canWrite = permissions.canWrite() ? 1 : 0;
      final int canCreate = permissions.canCreate() ? 1 : 0;

      final int result =
          NATIVE_BINDINGS.wasiContextPreopenDirWithPerms(
              contextHandle, hostPathStr, guestPathStr, canRead, canWrite, canCreate);
      if (result != 0) {
        throw new WasmException("Failed to add pre-opened directory with custom permissions");
      }
    }
    return this;
  }

  // ===== Output Capture Methods =====

  @Override
  public WasiContext enableOutputCapture() throws WasmException {
    ensureNotClosed();

    final int result = NATIVE_BINDINGS.wasiContextEnableOutputCapture(contextHandle);
    if (result != 0) {
      throw new WasmException("Failed to enable output capture");
    }
    return this;
  }

  @Override
  public byte[] getStdoutCapture() {
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      // Allocate space for the length output parameter (size_t = long on 64-bit)
      final MemorySegment lengthOut = arena.allocate(ValueLayout.JAVA_LONG);

      final MemorySegment dataPtr =
          NATIVE_BINDINGS.wasiContextGetStdoutCapture(contextHandle, lengthOut);

      if (dataPtr == null || dataPtr.equals(MemorySegment.NULL)) {
        return null;
      }

      final long length = lengthOut.get(ValueLayout.JAVA_LONG, 0);
      if (length == 0) {
        NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);
        return new byte[0];
      }

      // Reinterpret the segment to have the correct size
      final MemorySegment sizedPtr = dataPtr.reinterpret(length);
      final byte[] data = sizedPtr.toArray(ValueLayout.JAVA_BYTE);

      // Free the native buffer
      NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);

      return data;
    }
  }

  @Override
  public byte[] getStderrCapture() {
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      // Allocate space for the length output parameter (size_t = long on 64-bit)
      final MemorySegment lengthOut = arena.allocate(ValueLayout.JAVA_LONG);

      final MemorySegment dataPtr =
          NATIVE_BINDINGS.wasiContextGetStderrCapture(contextHandle, lengthOut);

      if (dataPtr == null || dataPtr.equals(MemorySegment.NULL)) {
        return null;
      }

      final long length = lengthOut.get(ValueLayout.JAVA_LONG, 0);
      if (length == 0) {
        NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);
        return new byte[0];
      }

      // Reinterpret the segment to have the correct size
      final MemorySegment sizedPtr = dataPtr.reinterpret(length);
      final byte[] data = sizedPtr.toArray(ValueLayout.JAVA_BYTE);

      // Free the native buffer
      NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);

      return data;
    }
  }

  @Override
  public boolean hasStdoutCapture() {
    ensureNotClosed();
    return NATIVE_BINDINGS.wasiContextHasStdoutCapture(contextHandle) == 1;
  }

  @Override
  public boolean hasStderrCapture() {
    ensureNotClosed();
    return NATIVE_BINDINGS.wasiContextHasStderrCapture(contextHandle) == 1;
  }

  /**
   * Gets the native context handle.
   *
   * @return the native context handle
   */
  public MemorySegment getNativeHandle() {
    return contextHandle;
  }

  /** Closes this WASI context and releases native resources. */
  public void close() {
    if (closed.compareAndSet(false, true)) {
      NATIVE_BINDINGS.wasiContextDestroy(contextHandle);
    }
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("WASI context is closed");
    }
  }
}
