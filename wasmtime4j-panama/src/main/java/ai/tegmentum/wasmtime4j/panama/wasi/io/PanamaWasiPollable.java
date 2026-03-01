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
package ai.tegmentum.wasmtime4j.panama.wasi.io;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiPollable interface.
 *
 * <p>This class provides access to WASI Preview 2 pollable operations through Panama Foreign
 * Function API calls to the native Wasmtime library. Pollables are used for event notification and
 * blocking operations on streams.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class PanamaWasiPollable implements WasiPollable, AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiPollable.class.getName());

  // Panama FFI function handles
  private static final MethodHandle BLOCK_HANDLE;
  private static final MethodHandle READY_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_pollable_block(context_handle, pollable_handle)
      BLOCK_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_pollable_block").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_pollable_ready(context_handle, pollable_handle, out_ready)
      READY_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_pollable_ready").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_pollable_close(context_handle, pollable_handle)
      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_pollable_close").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiPollable: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  private final MemorySegment nativeHandle;
  private final MemorySegment contextHandle;
  private final NativeResourceHandle resourceHandle;
  private final java.time.Instant createdAt = java.time.Instant.now();
  private volatile java.time.Instant lastAccessedAt;

  /**
   * Creates a new Panama WASI pollable with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws IllegalArgumentException if either handle is null
   */
  public PanamaWasiPollable(final MemorySegment contextHandle, final MemorySegment pollableHandle) {
    Validation.requireNonNull(pollableHandle, "pollableHandle");
    Validation.requireNonNull(contextHandle, "contextHandle");
    this.nativeHandle = pollableHandle;
    this.contextHandle = contextHandle;

    final MemorySegment ctx = this.contextHandle;
    final MemorySegment handle = this.nativeHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiPollable",
            () -> {
              try {
                final int result = (int) CLOSE_HANDLE.invoke(contextHandle, nativeHandle);
                if (result != 0) {
                  LOGGER.warning(
                      "Failed to close WASI pollable: "
                          + PanamaErrorMapper.getErrorDescription(result));
                }
              } catch (final Throwable e) {
                throw new Exception("Error closing WASI pollable", e);
              }
            },
            this,
            () -> {
              try {
                CLOSE_HANDLE.invoke(ctx, handle);
              } catch (final Throwable e) {
                LOGGER.warning("Safety net: error closing WASI pollable: " + e.getMessage());
              }
            });

    LOGGER.fine("Created Panama WASI pollable with handle: " + pollableHandle);
  }

  @Override
  public void block() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final IllegalStateException e) {
      throw new WasmException("Pollable is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) BLOCK_HANDLE.invoke(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to block on WASI pollable");
      }

      lastAccessedAt = java.time.Instant.now();

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error blocking on pollable: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean ready() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final IllegalStateException e) {
      throw new WasmException("Pollable is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outReady = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) READY_HANDLE.invoke(contextHandle, nativeHandle, outReady);

      if (result != 0) {
        throw new WasmException("Failed to check readiness of WASI pollable");
      }

      final int ready = outReady.get(ValueLayout.JAVA_INT, 0);
      lastAccessedAt = java.time.Instant.now();
      return ready != 0;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error checking pollable readiness: " + e.getMessage(), e);
    }
  }

  public long getId() {
    return nativeHandle.address();
  }

  public String getType() {
    return "wasi:io/pollable";
  }

  public boolean isOwned() {
    return true; // WASI pollables are owned by default
  }

  public boolean isValid() {
    return !isClosed();
  }

  public java.util.List<String> getAvailableOperations() {
    return java.util.Arrays.asList("block", "ready");
  }

  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null || operation.isEmpty()) {
      throw new IllegalArgumentException("Operation cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final IllegalStateException e) {
      throw new WasmException("Pollable is closed: " + e.getMessage(), e);
    }

    switch (operation) {
      case "block":
        block();
        return null;

      case "ready":
        return ready();

      default:
        throw new WasmException("Unknown operation: " + operation);
    }
  }

  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.ofNullable(lastAccessedAt);
  }

  public java.time.Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the native pollable handle.
   *
   * @return the native memory segment
   */
  public MemorySegment getNativeHandle() {
    ensureNotClosed();
    return nativeHandle;
  }

  /**
   * Checks if this pollable has been closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return resourceHandle.isClosed();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
