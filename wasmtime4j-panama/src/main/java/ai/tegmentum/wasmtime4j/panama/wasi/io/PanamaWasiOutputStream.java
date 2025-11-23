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
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
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
 * Panama FFI implementation of the WasiOutputStream interface.
 *
 * <p>This class provides access to WASI Preview 2 output stream operations through Panama Foreign
 * Function API calls to the native Wasmtime library. Output streams support non-blocking and
 * blocking write operations, as well as stream splicing.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class PanamaWasiOutputStream extends PanamaResource implements WasiOutputStream {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiOutputStream.class.getName());

  // Panama FFI function handles
  private static final MethodHandle CHECK_WRITE_HANDLE;
  private static final MethodHandle WRITE_HANDLE;
  private static final MethodHandle BLOCKING_WRITE_AND_FLUSH_HANDLE;
  private static final MethodHandle FLUSH_HANDLE;
  private static final MethodHandle BLOCKING_FLUSH_HANDLE;
  private static final MethodHandle WRITE_ZEROES_HANDLE;
  private static final MethodHandle BLOCKING_WRITE_ZEROES_AND_FLUSH_HANDLE;
  private static final MethodHandle SPLICE_HANDLE;
  private static final MethodHandle BLOCKING_SPLICE_HANDLE;
  private static final MethodHandle SUBSCRIBE_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = PanamaResource.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_output_stream_check_write(context_handle, stream_handle,
      // out_capacity)
      CHECK_WRITE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_check_write").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_output_stream_write(context_handle, stream_handle, buffer,
      // length)
      WRITE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_write").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_output_stream_blocking_write_and_flush(...)
      BLOCKING_WRITE_AND_FLUSH_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_output_stream_blocking_write_and_flush")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_output_stream_flush(context_handle, stream_handle)
      FLUSH_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_flush").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_output_stream_blocking_flush(context_handle, stream_handle)
      BLOCKING_FLUSH_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_blocking_flush").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_output_stream_write_zeroes(context_handle, stream_handle,
      // length)
      WRITE_ZEROES_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_write_zeroes").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_output_stream_blocking_write_zeroes_and_flush(...)
      BLOCKING_WRITE_ZEROES_AND_FLUSH_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_output_stream_blocking_write_zeroes_and_flush")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_output_stream_splice(context, output, input, length,
      // out_spliced)
      SPLICE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_splice").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_output_stream_blocking_splice(...)
      BLOCKING_SPLICE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_blocking_splice").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // void* wasmtime4j_panama_wasi_output_stream_subscribe(context_handle, stream_handle)
      SUBSCRIBE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_subscribe").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_output_stream_close(context_handle, stream_handle)
      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_output_stream_close").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe(
          "Failed to initialize Panama FFI handles for WasiOutputStream: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI output stream with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws IllegalArgumentException if either handle is null
   */
  public PanamaWasiOutputStream(
      final MemorySegment contextHandle, final MemorySegment streamHandle) {
    super(streamHandle);
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI output stream with handle: " + streamHandle);
  }

  @Override
  public long checkWrite() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outCapacity = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) CHECK_WRITE_HANDLE.invoke(contextHandle, nativeHandle, outCapacity);

      if (result != 0) {
        throw new WasmException("Failed to check write capacity on WASI output stream");
      }

      final long capacity = outCapacity.get(ValueLayout.JAVA_LONG, 0);
      if (capacity < 0) {
        throw new WasmException("Invalid write capacity: " + capacity);
      }

      return capacity;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error checking write capacity: " + e.getMessage(), e);
    }
  }

  @Override
  public void write(final byte[] contents) throws WasmException {
    if (contents == null) {
      throw new IllegalArgumentException("Contents cannot be null");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment buffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, contents);

      final int result =
          (int) WRITE_HANDLE.invoke(contextHandle, nativeHandle, buffer, (long) contents.length);

      if (result != 0) {
        throw new WasmException("Failed to write to WASI output stream");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error writing to output stream: " + e.getMessage(), e);
    }
  }

  @Override
  public void blockingWriteAndFlush(final byte[] contents) throws WasmException {
    if (contents == null) {
      throw new IllegalArgumentException("Contents cannot be null");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment buffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, contents);

      final int result =
          (int)
              BLOCKING_WRITE_AND_FLUSH_HANDLE.invoke(
                  contextHandle, nativeHandle, buffer, (long) contents.length);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking write and flush");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error performing blocking write and flush: " + e.getMessage(), e);
    }
  }

  @Override
  public void flush() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) FLUSH_HANDLE.invoke(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to flush WASI output stream");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error flushing output stream: " + e.getMessage(), e);
    }
  }

  @Override
  public void blockingFlush() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) BLOCKING_FLUSH_HANDLE.invoke(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking flush");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error performing blocking flush: " + e.getMessage(), e);
    }
  }

  @Override
  public void writeZeroes(final long length) throws WasmException {
    PanamaValidation.requireNonNegative(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) WRITE_ZEROES_HANDLE.invoke(contextHandle, nativeHandle, length);

      if (result != 0) {
        throw new WasmException("Failed to write zeroes to output stream");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error writing zeroes: " + e.getMessage(), e);
    }
  }

  @Override
  public void blockingWriteZeroesAndFlush(final long length) throws WasmException {
    PanamaValidation.requireNonNegative(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    try {
      final int result =
          (int) BLOCKING_WRITE_ZEROES_AND_FLUSH_HANDLE.invoke(contextHandle, nativeHandle, length);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking write zeroes and flush");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException(
          "Error performing blocking write zeroes and flush: " + e.getMessage(), e);
    }
  }

  @Override
  public long splice(final WasiInputStream source, final long length) throws WasmException {
    if (source == null) {
      throw new IllegalArgumentException("Source stream cannot be null");
    }
    PanamaValidation.requireNonNegative(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    // Get the native handle from the source stream
    if (!(source instanceof PanamaWasiInputStream)) {
      throw new IllegalArgumentException(
          "Source stream must be a PanamaWasiInputStream, got: " + source.getClass().getName());
    }
    final MemorySegment sourceHandle;
    try {
      sourceHandle = ((PanamaWasiInputStream) source).getNativeHandle();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Source stream is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSpliced = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int) SPLICE_HANDLE.invoke(contextHandle, nativeHandle, sourceHandle, length, outSpliced);

      if (result != 0) {
        throw new WasmException("Failed to splice streams");
      }

      final long spliced = outSpliced.get(ValueLayout.JAVA_LONG, 0);
      if (spliced < 0) {
        throw new WasmException("Invalid spliced count: " + spliced);
      }

      return spliced;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error splicing streams: " + e.getMessage(), e);
    }
  }

  @Override
  public long blockingSplice(final WasiInputStream source, final long length) throws WasmException {
    if (source == null) {
      throw new IllegalArgumentException("Source stream cannot be null");
    }
    PanamaValidation.requireNonNegative(length, "length");
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }

    // Get the native handle from the source stream
    if (!(source instanceof PanamaWasiInputStream)) {
      throw new IllegalArgumentException(
          "Source stream must be a PanamaWasiInputStream, got: " + source.getClass().getName());
    }
    final MemorySegment sourceHandle;
    try {
      sourceHandle = ((PanamaWasiInputStream) source).getNativeHandle();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Source stream is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSpliced = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int)
              BLOCKING_SPLICE_HANDLE.invoke(
                  contextHandle, nativeHandle, sourceHandle, length, outSpliced);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking splice");
      }

      final long spliced = outSpliced.get(ValueLayout.JAVA_LONG, 0);
      if (spliced < 0) {
        throw new WasmException("Invalid spliced count: " + spliced);
      }

      return spliced;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error performing blocking splice: " + e.getMessage(), e);
    }
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
        throw new WasmException("Failed to create pollable for output stream");
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
    return "wasi:io/output-stream";
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
    return java.util.Arrays.asList(
        "check-write",
        "write",
        "blocking-write-and-flush",
        "flush",
        "blocking-flush",
        "write-zeroes",
        "blocking-write-zeroes-and-flush",
        "splice",
        "blocking-splice",
        "subscribe");
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    throw new UnsupportedOperationException(
        "Generic invoke not supported for WASI streams - use dedicated methods");
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
    throw new UnsupportedOperationException(
        "Resource handle creation not yet implemented for WASI streams");
  }

  @Override
  public void transferOwnership(final ai.tegmentum.wasmtime4j.wasi.WasiInstance targetInstance)
      throws WasmException {
    throw new UnsupportedOperationException(
        "Ownership transfer not yet implemented for WASI streams");
  }

  @Override
  protected void doClose() throws Exception {
    try {
      final int result = (int) CLOSE_HANDLE.invoke(contextHandle, nativeHandle);
      if (result != 0) {
        LOGGER.warning("Failed to close WASI output stream (error code: " + result + ")");
      }
    } catch (final Throwable e) {
      throw new Exception("Error closing WASI output stream", e);
    }
  }

  @Override
  protected String getResourceType() {
    return "WasiOutputStream";
  }
}
