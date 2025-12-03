/*
 * Copyright 2024 Tegmentum AI
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
import ai.tegmentum.wasmtime4j.panama.util.PanamaResource;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
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
public final class PanamaWasiPollable extends PanamaResource implements WasiPollable {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiPollable.class.getName());

  // Panama FFI function handles
  private static final MethodHandle BLOCK_HANDLE;
  private static final MethodHandle READY_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = PanamaResource.getNativeLibrary();
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

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI pollable with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws IllegalArgumentException if either handle is null
   */
  public PanamaWasiPollable(final MemorySegment contextHandle, final MemorySegment pollableHandle) {
    super(pollableHandle);
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI pollable with handle: " + pollableHandle);
  }

  @Override
  public void block() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Pollable is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) BLOCK_HANDLE.invoke(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to block on WASI pollable");
      }

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
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Pollable is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outReady = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) READY_HANDLE.invoke(contextHandle, nativeHandle, outReady);

      if (result != 0) {
        throw new WasmException("Failed to check readiness of WASI pollable");
      }

      final int ready = outReady.get(ValueLayout.JAVA_INT, 0);
      return ready != 0;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error checking pollable readiness: " + e.getMessage(), e);
    }
  }

  @Override
  public long getId() {
    return nativeHandle.address();
  }

  @Override
  public String getType() {
    return "wasi:io/pollable";
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
    return null; // Instance ownership tracking not yet implemented for WASI I/O pollables
  }

  @Override
  public boolean isOwned() {
    return true; // WASI pollables are owned by default
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  public java.util.List<String> getAvailableOperations() {
    return java.util.Arrays.asList("block", "ready");
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null || operation.isEmpty()) {
      throw new IllegalArgumentException("Operation cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
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

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceStats getStats() {
    return null; // Stats not yet implemented for WASI I/O pollables
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceState getState() {
    return isClosed()
        ? ai.tegmentum.wasmtime4j.wasi.WasiResourceState.CLOSED
        : ai.tegmentum.wasmtime4j.wasi.WasiResourceState.ACTIVE;
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata getMetadata() throws WasmException {
    return null; // Metadata not yet implemented for WASI I/O pollables
  }

  @Override
  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.empty(); // Access tracking not yet implemented for WASI I/O pollables
  }

  @Override
  public java.time.Instant getCreatedAt() {
    return java.time.Instant
        .now(); // Creation time tracking not yet implemented for WASI I/O pollables
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle createHandle() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Pollable is closed: " + e.getMessage(), e);
    }
    return new PanamaWasiResourceHandle(nativeHandle.address(), getType(), getOwner());
  }

  @Override
  public void transferOwnership(final ai.tegmentum.wasmtime4j.wasi.WasiInstance targetInstance)
      throws WasmException {
    if (targetInstance == null) {
      throw new IllegalArgumentException("Target instance cannot be null");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Pollable is closed: " + e.getMessage(), e);
    }
    if (!isOwned()) {
      throw new IllegalStateException("Cannot transfer ownership of a borrowed resource");
    }
    // In WASI Preview 2, ownership transfer is handled at the component model level
    // For Panama pollables, we log the transfer but don't change the underlying native resource
    // The native resource will be managed by the target instance after transfer
    LOGGER.fine(
        "Transferring ownership of pollable "
            + nativeHandle.address()
            + " to instance "
            + targetInstance.getId());
  }

  /** Internal implementation of WasiResourceHandle for Panama WASI resources. */
  private static final class PanamaWasiResourceHandle
      implements ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle {
    private final long resourceId;
    private final String resourceType;
    private final ai.tegmentum.wasmtime4j.wasi.WasiInstance owner;

    PanamaWasiResourceHandle(
        final long resourceId,
        final String resourceType,
        final ai.tegmentum.wasmtime4j.wasi.WasiInstance owner) {
      this.resourceId = resourceId;
      this.resourceType = resourceType;
      this.owner = owner;
    }

    @Override
    public long getResourceId() {
      return resourceId;
    }

    @Override
    public String getResourceType() {
      return resourceType;
    }

    @Override
    public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
      return owner;
    }

    @Override
    public boolean isValid() {
      return resourceId != 0;
    }
  }

  @Override
  protected void doClose() throws Exception {
    try {
      final int result = (int) CLOSE_HANDLE.invoke(contextHandle, nativeHandle);
      if (result != 0) {
        LOGGER.warning("Failed to close WASI pollable (error code: " + result + ")");
      }
    } catch (final Throwable e) {
      throw new Exception("Error closing WASI pollable", e);
    }
  }

  @Override
  protected String getResourceType() {
    return "WasiPollable";
  }
}
