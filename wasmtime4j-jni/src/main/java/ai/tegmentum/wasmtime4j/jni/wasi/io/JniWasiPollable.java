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

package ai.tegmentum.wasmtime4j.jni.wasi.io;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiPollable interface.
 *
 * <p>This class provides access to WASI Preview 2 pollable operations through JNI calls to the
 * native Wasmtime library. Pollables represent events that can be waited on using poll operations.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class JniWasiPollable extends JniResource implements WasiPollable {

  private static final Logger LOGGER = Logger.getLogger(JniWasiPollable.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiPollable: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI pollable with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws IllegalArgumentException if either handle is 0
   */
  public JniWasiPollable(final long contextHandle, final long pollableHandle) {
    super(pollableHandle);
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI pollable with handle: " + pollableHandle);
  }

  @Override
  public void block() throws WasmException {
    ensureNotClosed();
    nativeBlock(contextHandle, nativeHandle);
  }

  @Override
  public boolean ready() throws WasmException {
    ensureNotClosed();
    return nativeReady(contextHandle, nativeHandle);
  }

  @Override
  public long getId() {
    return nativeHandle;
  }

  @Override
  public String getType() {
    return "wasi:io/pollable";
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
    throw new UnsupportedOperationException("not yet implemented: WASI pollable owner tracking");
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
    ensureNotClosed();

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
    throw new UnsupportedOperationException("not yet implemented: WASI pollable statistics");
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceState getState() {
    return isClosed()
        ? ai.tegmentum.wasmtime4j.wasi.WasiResourceState.CLOSED
        : ai.tegmentum.wasmtime4j.wasi.WasiResourceState.ACTIVE;
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata getMetadata() throws WasmException {
    throw new UnsupportedOperationException("not yet implemented: WASI pollable metadata");
  }

  @Override
  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.empty(); // Access tracking not yet implemented for WASI pollables
  }

  @Override
  public java.time.Instant getCreatedAt() {
    return java.time.Instant.now(); // Creation time tracking not yet implemented for WASI pollables
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle createHandle() throws WasmException {
    ensureNotClosed();
    return new JniWasiResourceHandle(nativeHandle, getType(), getOwner());
  }

  @Override
  public void transferOwnership(final ai.tegmentum.wasmtime4j.wasi.WasiInstance targetInstance)
      throws WasmException {
    if (targetInstance == null) {
      throw new IllegalArgumentException("Target instance cannot be null");
    }
    ensureNotClosed();
    if (!isOwned()) {
      throw new IllegalStateException("Cannot transfer ownership of a borrowed resource");
    }
    // In WASI Preview 2, ownership transfer is handled at the component model level
    // For JNI pollables, we log the transfer but don't change the underlying native resource
    // The native resource will be managed by the target instance after transfer
    LOGGER.fine(
        "Transferring ownership of pollable "
            + nativeHandle
            + " to instance "
            + targetInstance.getId());
  }

  /** Internal implementation of WasiResourceHandle for JNI WASI resources. */
  private static final class JniWasiResourceHandle
      implements ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle {
    private final long resourceId;
    private final String resourceType;
    private final ai.tegmentum.wasmtime4j.wasi.WasiInstance owner;

    JniWasiResourceHandle(
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
    nativeClose(contextHandle, nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "WasiPollable";
  }

  // Native method declarations

  /**
   * Blocks until this pollable is ready.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws WasmException if the wait operation fails
   */
  private static native void nativeBlock(long contextHandle, long pollableHandle)
      throws WasmException;

  /**
   * Checks if this pollable is currently ready without blocking.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @return true if the pollable is ready, false otherwise
   * @throws WasmException if the ready check fails
   */
  private static native boolean nativeReady(long contextHandle, long pollableHandle)
      throws WasmException;

  /**
   * Closes the pollable.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws WasmException if the close operation fails
   */
  private static native void nativeClose(long contextHandle, long pollableHandle)
      throws WasmException;
}
