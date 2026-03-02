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
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Path;
import java.util.Map;

/**
 * Panama FFI implementation of WasiContext.
 *
 * <p>Provides WASI context functionality using Panama Foreign Function API bindings to the native
 * Wasmtime WASI implementation.
 *
 * @since 1.0.0
 */
public final class PanamaWasiContext implements WasiContext {

  private static final NativeWasiBindings NATIVE_BINDINGS = NativeWasiBindings.getInstance();

  private final MemorySegment contextHandle;
  private final NativeResourceHandle resourceHandle;

  /** WASI Preview 2 config: network access enabled. */
  private boolean networkEnabled;

  /** WASI Preview 2 config: async I/O enabled. */
  private boolean asyncIoEnabled;

  /** WASI Preview 2 config: process spawning enabled. */
  private boolean processEnabled;

  /** WASI Preview 2 config: component model enabled. */
  private boolean componentModelEnabled;

  /** WASI Preview 2 config: max async operations (-1 = unlimited). */
  private int maxAsyncOperations = -1;

  /** WASI Preview 2 config: async timeout in milliseconds (-1 = no timeout). */
  private long asyncTimeoutMs = -1;

  /** Creates a new Panama WASI context. */
  public PanamaWasiContext() {
    this.contextHandle = NATIVE_BINDINGS.wasiContextCreate();
    if (contextHandle == null || contextHandle.equals(MemorySegment.NULL)) {
      throw new RuntimeException("Failed to create WASI context");
    }

    // Capture handle locally for safety net (must NOT capture 'this')
    final MemorySegment handle = this.contextHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiContext",
            () -> NATIVE_BINDINGS.wasiContextDestroy(handle),
            this,
            () -> NATIVE_BINDINGS.wasiContextDestroy(handle));
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
  public WasiContext inheritArgs() {
    ensureNotClosed();

    final int result = NATIVE_BINDINGS.wasiContextInheritArgs(contextHandle);
    if (result != 0) {
      throw new RuntimeException("Failed to inherit command-line arguments");
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
  public WasiContext setStdoutAppend(final Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stdout append path cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment pathStr = arena.allocateFrom(path.toString());

      final int result = NATIVE_BINDINGS.wasiContextSetStdoutAppend(contextHandle, pathStr);
      if (result != 0) {
        throw new RuntimeException("Failed to set stdout append path");
      }
    }
    return this;
  }

  @Override
  public WasiContext setStderrAppend(final Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stderr append path cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment pathStr = arena.allocateFrom(path.toString());

      final int result = NATIVE_BINDINGS.wasiContextSetStderrAppend(contextHandle, pathStr);
      if (result != 0) {
        throw new RuntimeException("Failed to set stderr append path");
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
    ensureNotClosed();
    this.networkEnabled = enabled;
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
    ensureNotClosed();
    this.asyncIoEnabled = enabled;
    return this;
  }

  @Override
  public WasiContext setMaxAsyncOperations(final int maxOps) {
    if (maxOps < -1) {
      throw new IllegalArgumentException("maxOps must be >= -1");
    }
    ensureNotClosed();
    this.maxAsyncOperations = maxOps;
    return this;
  }

  @Override
  public WasiContext setAsyncTimeout(final long timeoutMs) {
    if (timeoutMs < -1) {
      throw new IllegalArgumentException("timeoutMs must be >= -1");
    }
    ensureNotClosed();
    this.asyncTimeoutMs = timeoutMs;
    return this;
  }

  @Override
  public WasiContext setComponentModelEnabled(final boolean enabled) {
    ensureNotClosed();
    this.componentModelEnabled = enabled;
    return this;
  }

  @Override
  public WasiContext setProcessEnabled(final boolean enabled) {
    ensureNotClosed();
    this.processEnabled = enabled;
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

  // ===== WASI Preview 2 Config Getters =====

  /**
   * Returns whether network access is enabled.
   *
   * @return true if network access is enabled
   */
  public boolean isNetworkEnabled() {
    return networkEnabled;
  }

  /**
   * Returns whether async I/O is enabled.
   *
   * @return true if async I/O is enabled
   */
  public boolean isAsyncIoEnabled() {
    return asyncIoEnabled;
  }

  /**
   * Returns whether process spawning is enabled.
   *
   * @return true if process spawning is enabled
   */
  public boolean isProcessEnabled() {
    return processEnabled;
  }

  /**
   * Returns whether component model is enabled.
   *
   * @return true if component model is enabled
   */
  public boolean isComponentModelEnabled() {
    return componentModelEnabled;
  }

  /**
   * Returns the max async operations limit.
   *
   * @return the max async operations (-1 = unlimited)
   */
  public int getMaxAsyncOperations() {
    return maxAsyncOperations;
  }

  /**
   * Returns the async timeout in milliseconds.
   *
   * @return the async timeout (-1 = no timeout)
   */
  public long getAsyncTimeoutMs() {
    return asyncTimeoutMs;
  }

  // ===== Environment and Argument Getters =====

  @Override
  public java.util.Map<String, String> getEnvironment() {
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outPtr = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLen = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.wasiContextGetEnvironment(contextHandle, outPtr, outLen);
      if (result != 0) {
        return java.util.Collections.emptyMap();
      }

      final MemorySegment dataPtr = outPtr.get(ValueLayout.ADDRESS, 0);
      final long length = outLen.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || length == 0) {
        if (!dataPtr.equals(MemorySegment.NULL)) {
          NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);
        }
        return java.util.Collections.emptyMap();
      }

      final MemorySegment sizedPtr = dataPtr.reinterpret(length);
      final byte[] bytes = sizedPtr.toArray(ValueLayout.JAVA_BYTE);
      NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);

      final String data = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
      final java.util.Map<String, String> env = new java.util.LinkedHashMap<>();
      for (String line : data.split("\n")) {
        if (!line.isEmpty()) {
          final int eqIdx = line.indexOf('=');
          if (eqIdx > 0) {
            env.put(line.substring(0, eqIdx), line.substring(eqIdx + 1));
          }
        }
      }
      return java.util.Collections.unmodifiableMap(env);
    }
  }

  @Override
  public java.util.List<String> getArguments() {
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outPtr = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLen = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.wasiContextGetArguments(contextHandle, outPtr, outLen);
      if (result != 0) {
        return java.util.Collections.emptyList();
      }

      final MemorySegment dataPtr = outPtr.get(ValueLayout.ADDRESS, 0);
      final long length = outLen.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || length == 0) {
        if (!dataPtr.equals(MemorySegment.NULL)) {
          NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);
        }
        return java.util.Collections.emptyList();
      }

      final MemorySegment sizedPtr = dataPtr.reinterpret(length);
      final byte[] bytes = sizedPtr.toArray(ValueLayout.JAVA_BYTE);
      NATIVE_BINDINGS.wasiFreeCaptureBuffer(dataPtr);

      final String data = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
      final java.util.List<String> args = new java.util.ArrayList<>();
      for (String line : data.split("\n")) {
        if (!line.isEmpty()) {
          args.add(line);
        }
      }
      return java.util.Collections.unmodifiableList(args);
    }
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
    resourceHandle.close();
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
