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
public final class PanamaWasiOutputStream implements WasiOutputStream, AutoCloseable {

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
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
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

  private final MemorySegment nativeHandle;
  private final MemorySegment contextHandle;
  private final NativeResourceHandle resourceHandle;
  private final java.time.Instant createdAt = java.time.Instant.now();
  private volatile java.time.Instant lastAccessedAt;

  // Pre-allocated output buffers for fixed-size outputs
  private final Arena bufferArena;
  private final MemorySegment bufOutCapacity;
  private final MemorySegment bufOutSpliced;

  /**
   * Creates a new Panama WASI output stream with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws IllegalArgumentException if either handle is null
   */
  public PanamaWasiOutputStream(
      final MemorySegment contextHandle, final MemorySegment streamHandle) {
    Validation.requireNonNull(streamHandle, "streamHandle");
    Validation.requireNonNull(contextHandle, "contextHandle");
    this.nativeHandle = streamHandle;
    this.contextHandle = contextHandle;

    // Pre-allocate output buffers for fixed-size results
    this.bufferArena = Arena.ofShared();
    this.bufOutCapacity = bufferArena.allocate(ValueLayout.JAVA_LONG);
    this.bufOutSpliced = bufferArena.allocate(ValueLayout.JAVA_LONG);

    final MemorySegment ctx = this.contextHandle;
    final MemorySegment handle = this.nativeHandle;
    final Arena capturedArena = this.bufferArena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiOutputStream",
            () -> {
              try {
                final int result = (int) CLOSE_HANDLE.invokeExact(contextHandle, nativeHandle);
                if (result != 0) {
                  LOGGER.warning(
                      "Failed to close WASI output stream: "
                          + PanamaErrorMapper.getErrorDescription(result));
                }
              } catch (final Throwable e) {
                throw new Exception("Error closing WASI output stream", e);
              }
              if (bufferArena.scope().isAlive()) {
                bufferArena.close();
              }
            },
            this,
            () -> {
              try {
                CLOSE_HANDLE.invokeExact(ctx, handle);
              } catch (final Throwable e) {
                LOGGER.warning("Safety net: error closing WASI output stream: " + e.getMessage());
              }
              if (capturedArena.scope().isAlive()) {
                capturedArena.close();
              }
            });

    LOGGER.fine("Created Panama WASI output stream with handle: " + streamHandle);
  }

  @Override
  public long checkWrite() throws WasmException {
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      final int result =
          (int) CHECK_WRITE_HANDLE.invokeExact(contextHandle, nativeHandle, bufOutCapacity);

      if (result != 0) {
        throw new WasmException("Failed to check write capacity on WASI output stream");
      }

      final long capacity = bufOutCapacity.get(ValueLayout.JAVA_LONG, 0);
      if (capacity < 0) {
        throw new WasmException("Invalid write capacity: " + capacity);
      }

      return capacity;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error checking write capacity: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void write(final byte[] contents) throws WasmException {
    if (contents == null) {
      throw new IllegalArgumentException("Contents cannot be null");
    }
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment buffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, contents);

      final int result =
          (int)
              WRITE_HANDLE.invokeExact(contextHandle, nativeHandle, buffer, (long) contents.length);

      if (result != 0) {
        throw new WasmException("Failed to write to WASI output stream");
      }

      lastAccessedAt = java.time.Instant.now();

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error writing to output stream: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void blockingWriteAndFlush(final byte[] contents) throws WasmException {
    if (contents == null) {
      throw new IllegalArgumentException("Contents cannot be null");
    }
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment buffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, contents);

      final int result =
          (int)
              BLOCKING_WRITE_AND_FLUSH_HANDLE.invokeExact(
                  contextHandle, nativeHandle, buffer, (long) contents.length);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking write and flush");
      }

      lastAccessedAt = java.time.Instant.now();

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error performing blocking write and flush: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void flush() throws WasmException {
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      final int result = (int) FLUSH_HANDLE.invokeExact(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to flush WASI output stream");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error flushing output stream: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void blockingFlush() throws WasmException {
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      final int result = (int) BLOCKING_FLUSH_HANDLE.invokeExact(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking flush");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error performing blocking flush: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void writeZeroes(final long length) throws WasmException {
    Validation.requireNonNegative(length, "length");
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      final int result = (int) WRITE_ZEROES_HANDLE.invokeExact(contextHandle, nativeHandle, length);

      if (result != 0) {
        throw new WasmException("Failed to write zeroes to output stream");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error writing zeroes: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void blockingWriteZeroesAndFlush(final long length) throws WasmException {
    Validation.requireNonNegative(length, "length");
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      final int result =
          (int)
              BLOCKING_WRITE_ZEROES_AND_FLUSH_HANDLE.invokeExact(
                  contextHandle, nativeHandle, length);

      if (result != 0) {
        throw new WasmException("Failed to perform blocking write zeroes and flush");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException(
          "Error performing blocking write zeroes and flush: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long splice(final WasiInputStream source, final long length) throws WasmException {
    if (source == null) {
      throw new IllegalArgumentException("Source stream cannot be null");
    }
    Validation.requireNonNegative(length, "length");
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      // Get the native handle from the source stream
      if (!(source instanceof PanamaWasiInputStream)) {
        throw new IllegalArgumentException(
            "Source stream must be a PanamaWasiInputStream, got: " + source.getClass().getName());
      }
      final MemorySegment sourceHandle;
      try {
        sourceHandle = ((PanamaWasiInputStream) source).getNativeHandle();
      } catch (final IllegalStateException e) {
        throw new WasmException("Source stream is closed: " + e.getMessage(), e);
      }

      try {
        final int result =
            (int)
                SPLICE_HANDLE.invokeExact(
                    contextHandle, nativeHandle, sourceHandle, length, bufOutSpliced);

        if (result != 0) {
          throw new WasmException("Failed to splice streams");
        }

        final long spliced = bufOutSpliced.get(ValueLayout.JAVA_LONG, 0);
        if (spliced < 0) {
          throw new WasmException("Invalid spliced count: " + spliced);
        }

        return spliced;

      } catch (final WasmException e) {
        throw e;
      } catch (final Throwable e) {
        throw new WasmException("Error splicing streams: " + e.getMessage(), e);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long blockingSplice(final WasiInputStream source, final long length) throws WasmException {
    if (source == null) {
      throw new IllegalArgumentException("Source stream cannot be null");
    }
    Validation.requireNonNegative(length, "length");
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      // Get the native handle from the source stream
      if (!(source instanceof PanamaWasiInputStream)) {
        throw new IllegalArgumentException(
            "Source stream must be a PanamaWasiInputStream, got: " + source.getClass().getName());
      }
      final MemorySegment sourceHandle;
      try {
        sourceHandle = ((PanamaWasiInputStream) source).getNativeHandle();
      } catch (final IllegalStateException e) {
        throw new WasmException("Source stream is closed: " + e.getMessage(), e);
      }

      try {
        final int result =
            (int)
                BLOCKING_SPLICE_HANDLE.invokeExact(
                    contextHandle, nativeHandle, sourceHandle, length, bufOutSpliced);

        if (result != 0) {
          throw new WasmException("Failed to perform blocking splice");
        }

        final long spliced = bufOutSpliced.get(ValueLayout.JAVA_LONG, 0);
        if (spliced < 0) {
          throw new WasmException("Invalid spliced count: " + spliced);
        }

        return spliced;

      } catch (final WasmException e) {
        throw e;
      } catch (final Throwable e) {
        throw new WasmException("Error performing blocking splice: " + e.getMessage(), e);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public WasiPollable subscribe() throws WasmException {
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      final MemorySegment pollableHandle =
          (MemorySegment) SUBSCRIBE_HANDLE.invokeExact(contextHandle, nativeHandle);

      if (pollableHandle == null || pollableHandle.address() == 0) {
        throw new WasmException("Failed to create pollable for output stream");
      }

      return new PanamaWasiPollable(contextHandle, pollableHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating pollable: " + e.getMessage(), e);
    } finally {
      resourceHandle.endOperation();
    }
  }

  public long getId() {
    return nativeHandle.address();
  }

  public String getType() {
    return "wasi:io/output-stream";
  }

  public boolean isOwned() {
    return true; // WASI streams are owned by default
  }

  public boolean isValid() {
    return !isClosed();
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null || operation.isEmpty()) {
      throw new IllegalArgumentException("Operation cannot be null or empty");
    }
    try {
      resourceHandle.beginOperation();
    } catch (final IllegalStateException e) {
      throw new WasmException("Resource is closed: " + e.getMessage(), e);
    }
    try {
      switch (operation) {
        case "check-write":
          return checkWrite();

        case "write":
          if (parameters.length < 1 || !(parameters[0] instanceof byte[])) {
            throw new IllegalArgumentException("write requires a byte[] parameter");
          }
          write((byte[]) parameters[0]);
          return null;

        case "blocking-write-and-flush":
          if (parameters.length < 1 || !(parameters[0] instanceof byte[])) {
            throw new IllegalArgumentException(
                "blocking-write-and-flush requires a byte[] parameter");
          }
          blockingWriteAndFlush((byte[]) parameters[0]);
          return null;

        case "flush":
          flush();
          return null;

        case "blocking-flush":
          blockingFlush();
          return null;

        case "write-zeroes":
          if (parameters.length < 1 || !(parameters[0] instanceof Number)) {
            throw new IllegalArgumentException("write-zeroes requires a length parameter");
          }
          writeZeroes(((Number) parameters[0]).longValue());
          return null;

        case "blocking-write-zeroes-and-flush":
          if (parameters.length < 1 || !(parameters[0] instanceof Number)) {
            throw new IllegalArgumentException(
                "blocking-write-zeroes-and-flush requires a length parameter");
          }
          blockingWriteZeroesAndFlush(((Number) parameters[0]).longValue());
          return null;

        case "splice":
          if (parameters.length < 2
              || !(parameters[0] instanceof WasiInputStream)
              || !(parameters[1] instanceof Number)) {
            throw new IllegalArgumentException(
                "splice requires a WasiInputStream and length parameter");
          }
          return splice((WasiInputStream) parameters[0], ((Number) parameters[1]).longValue());

        case "blocking-splice":
          if (parameters.length < 2
              || !(parameters[0] instanceof WasiInputStream)
              || !(parameters[1] instanceof Number)) {
            throw new IllegalArgumentException(
                "blocking-splice requires a WasiInputStream and length parameter");
          }
          return blockingSplice(
              (WasiInputStream) parameters[0], ((Number) parameters[1]).longValue());

        case "subscribe":
          return subscribe();

        default:
          throw new WasmException("Unknown operation: " + operation);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.ofNullable(lastAccessedAt);
  }

  public java.time.Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the native stream handle.
   *
   * @return the native memory segment
   */
  public MemorySegment getNativeHandle() {
    resourceHandle.beginOperation();
    try {
      return nativeHandle;
    } finally {
      resourceHandle.endOperation();
    }
  }

  /**
   * Checks if this output stream has been closed.
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
}
