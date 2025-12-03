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
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
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
 * Panama FFI implementation of the WasiInputStream interface.
 *
 * <p>This class provides access to WASI Preview 2 input stream operations through Panama Foreign
 * Function API calls to the native Wasmtime library. Input streams support non-blocking and
 * blocking read operations.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class PanamaWasiInputStream extends PanamaResource implements WasiInputStream {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiInputStream.class.getName());

  // Panama FFI function handles
  private static final MethodHandle READ_HANDLE;
  private static final MethodHandle BLOCKING_READ_HANDLE;
  private static final MethodHandle SKIP_HANDLE;
  private static final MethodHandle SUBSCRIBE_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = PanamaResource.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_input_stream_read(
      //     context_handle, stream_handle, length, out_buffer, out_length)
      READ_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_input_stream_read").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, // return type
                  ValueLayout.ADDRESS, // context_handle
                  ValueLayout.ADDRESS, // stream_handle
                  ValueLayout.JAVA_LONG, // length
                  ValueLayout.ADDRESS, // out_buffer
                  ValueLayout.ADDRESS // out_length
                  ));

      // int wasmtime4j_panama_wasi_input_stream_blocking_read(
      //     context_handle, stream_handle, length, out_buffer, out_length)
      BLOCKING_READ_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_input_stream_blocking_read").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_input_stream_skip(
      //     context_handle, stream_handle, length, out_skipped)
      SKIP_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_input_stream_skip").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // void* wasmtime4j_panama_wasi_input_stream_subscribe(context_handle, stream_handle)
      SUBSCRIBE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_input_stream_subscribe").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_input_stream_close(context_handle, stream_handle)
      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_input_stream_close").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe(
          "Failed to initialize Panama FFI handles for WasiInputStream: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI input stream with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws IllegalArgumentException if either handle is null
   */
  public PanamaWasiInputStream(
      final MemorySegment contextHandle, final MemorySegment streamHandle) {
    super(streamHandle);
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI input stream with handle: " + streamHandle);
  }

  @Override
  public byte[] read(final long length) throws WasmException {
    PanamaValidation.requirePositive(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment buffer = arena.allocate(length);
      final MemorySegment outLength = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int) READ_HANDLE.invoke(contextHandle, nativeHandle, length, buffer, outLength);

      if (result != 0) {
        throw new WasmException("Failed to read from WASI input stream");
      }

      final long bytesRead = outLength.get(ValueLayout.JAVA_LONG, 0);
      if (bytesRead < 0) {
        throw new WasmException("Invalid bytes read count: " + bytesRead);
      }

      final byte[] data = new byte[(int) bytesRead];
      MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, 0, data, 0, (int) bytesRead);
      return data;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error reading from WASI input stream: " + e.getMessage(), e);
    }
  }

  @Override
  public byte[] blockingRead(final long length) throws WasmException {
    PanamaValidation.requirePositive(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment buffer = arena.allocate(length);
      final MemorySegment outLength = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int) BLOCKING_READ_HANDLE.invoke(contextHandle, nativeHandle, length, buffer, outLength);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking read from WASI input stream");
      }

      final long bytesRead = outLength.get(ValueLayout.JAVA_LONG, 0);
      if (bytesRead < 0) {
        throw new WasmException("Invalid bytes read count: " + bytesRead);
      }

      final byte[] data = new byte[(int) bytesRead];
      MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, 0, data, 0, (int) bytesRead);
      return data;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error performing blocking read: " + e.getMessage(), e);
    }
  }

  @Override
  public long skip(final long length) throws WasmException {
    PanamaValidation.requirePositive(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSkipped = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) SKIP_HANDLE.invoke(contextHandle, nativeHandle, length, outSkipped);

      if (result != 0) {
        throw new WasmException("Failed to skip bytes in WASI input stream");
      }

      final long skipped = outSkipped.get(ValueLayout.JAVA_LONG, 0);
      if (skipped < 0) {
        throw new WasmException("Invalid skipped count: " + skipped);
      }

      return skipped;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error skipping bytes in input stream: " + e.getMessage(), e);
    }
  }

  @Override
  public long blockingSkip(final long length) throws WasmException {
    PanamaValidation.requirePositive(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    // Note: Current native implementation doesn't have separate blocking skip
    // so we use the same as skip for now
    return skip(length);
  }

  @Override
  public WasiPollable subscribe() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try {
      final MemorySegment pollableHandle =
          (MemorySegment) SUBSCRIBE_HANDLE.invoke(contextHandle, nativeHandle);

      if (pollableHandle == null || pollableHandle.address() == 0) {
        throw new WasmException("Failed to create pollable for input stream");
      }

      return new PanamaWasiPollable(contextHandle, pollableHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating pollable: " + e.getMessage(), e);
    }
  }

  @Override
  public long getId() {
    return nativeHandle.address();
  }

  @Override
  public String getType() {
    return "wasi:io/input-stream";
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
    return null; // Instance ownership tracking not yet implemented for WASI I/O streams
  }

  @Override
  public boolean isOwned() {
    return true; // WASI streams are owned by default
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  public java.util.List<String> getAvailableOperations() {
    return java.util.Arrays.asList("read", "blocking-read", "skip", "blocking-skip", "subscribe");
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null || operation.isEmpty()) {
      throw new IllegalArgumentException("Operation cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    switch (operation) {
      case "read":
        if (parameters.length < 1 || !(parameters[0] instanceof Number)) {
          throw new IllegalArgumentException("read requires a length parameter");
        }
        return read(((Number) parameters[0]).longValue());

      case "blocking-read":
        if (parameters.length < 1 || !(parameters[0] instanceof Number)) {
          throw new IllegalArgumentException("blocking-read requires a length parameter");
        }
        return blockingRead(((Number) parameters[0]).longValue());

      case "skip":
        if (parameters.length < 1 || !(parameters[0] instanceof Number)) {
          throw new IllegalArgumentException("skip requires a length parameter");
        }
        return skip(((Number) parameters[0]).longValue());

      case "blocking-skip":
        if (parameters.length < 1 || !(parameters[0] instanceof Number)) {
          throw new IllegalArgumentException("blocking-skip requires a length parameter");
        }
        return blockingSkip(((Number) parameters[0]).longValue());

      case "subscribe":
        return subscribe();

      default:
        throw new WasmException("Unknown operation: " + operation);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceStats getStats() {
    return null; // Stats not yet implemented for WASI I/O streams
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceState getState() {
    return isClosed()
        ? ai.tegmentum.wasmtime4j.wasi.WasiResourceState.CLOSED
        : ai.tegmentum.wasmtime4j.wasi.WasiResourceState.ACTIVE;
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata getMetadata() throws WasmException {
    return null; // Metadata not yet implemented for WASI I/O streams
  }

  @Override
  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.empty(); // Access tracking not yet implemented for WASI I/O streams
  }

  @Override
  public java.time.Instant getCreatedAt() {
    return java.time.Instant
        .now(); // Creation time tracking not yet implemented for WASI I/O streams
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle createHandle() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
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
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    if (!isOwned()) {
      throw new IllegalStateException("Cannot transfer ownership of a borrowed resource");
    }
    // In WASI Preview 2, ownership transfer is handled at the component model level
    // For Panama streams, we log the transfer but don't change the underlying native resource
    // The native resource will be managed by the target instance after transfer
    LOGGER.fine(
        "Transferring ownership of input stream "
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
        LOGGER.warning("Failed to close WASI input stream (error code: " + result + ")");
      }
    } catch (final Throwable e) {
      throw new Exception("Error closing WASI input stream", e);
    }
  }

  @Override
  protected String getResourceType() {
    return "WasiInputStream";
  }
}
