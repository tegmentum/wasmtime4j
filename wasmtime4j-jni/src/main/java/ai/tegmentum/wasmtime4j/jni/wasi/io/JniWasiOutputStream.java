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
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiOutputStream interface.
 *
 * <p>This class provides access to WASI Preview 2 output stream operations through JNI calls to the
 * native Wasmtime library. Output streams support non-blocking and blocking write operations, as
 * well as stream splicing.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class JniWasiOutputStream extends JniResource implements WasiOutputStream {

  private static final Logger LOGGER = Logger.getLogger(JniWasiOutputStream.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiOutputStream: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI output stream with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws IllegalArgumentException if either handle is 0
   */
  public JniWasiOutputStream(final long contextHandle, final long streamHandle) {
    super(streamHandle);
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI output stream with handle: " + streamHandle);
  }

  @Override
  public long checkWrite() throws WasmException {
    ensureNotClosed();
    return nativeCheckWrite(contextHandle, nativeHandle);
  }

  @Override
  public void write(final byte[] contents) throws WasmException {
    if (contents == null) {
      throw new IllegalArgumentException("Contents cannot be null");
    }
    ensureNotClosed();
    nativeWrite(contextHandle, nativeHandle, contents);
  }

  @Override
  public void blockingWriteAndFlush(final byte[] contents) throws WasmException {
    if (contents == null) {
      throw new IllegalArgumentException("Contents cannot be null");
    }
    ensureNotClosed();
    nativeBlockingWriteAndFlush(contextHandle, nativeHandle, contents);
  }

  @Override
  public void flush() throws WasmException {
    ensureNotClosed();
    nativeFlush(contextHandle, nativeHandle);
  }

  @Override
  public void blockingFlush() throws WasmException {
    ensureNotClosed();
    nativeBlockingFlush(contextHandle, nativeHandle);
  }

  @Override
  public void writeZeroes(final long length) throws WasmException {
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();
    nativeWriteZeroes(contextHandle, nativeHandle, length);
  }

  @Override
  public void blockingWriteZeroesAndFlush(final long length) throws WasmException {
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();
    nativeBlockingWriteZeroesAndFlush(contextHandle, nativeHandle, length);
  }

  @Override
  public long splice(final WasiInputStream source, final long length) throws WasmException {
    if (source == null) {
      throw new IllegalArgumentException("Source stream cannot be null");
    }
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    // Get the native handle from the source stream
    if (!(source instanceof JniWasiInputStream)) {
      throw new IllegalArgumentException(
          "Source stream must be a JniWasiInputStream, got: " + source.getClass().getName());
    }
    final long sourceHandle = ((JniWasiInputStream) source).getNativeHandle();
    return nativeSplice(contextHandle, nativeHandle, sourceHandle, length);
  }

  @Override
  public long blockingSplice(final WasiInputStream source, final long length) throws WasmException {
    if (source == null) {
      throw new IllegalArgumentException("Source stream cannot be null");
    }
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    // Get the native handle from the source stream
    if (!(source instanceof JniWasiInputStream)) {
      throw new IllegalArgumentException(
          "Source stream must be a JniWasiInputStream, got: " + source.getClass().getName());
    }
    final long sourceHandle = ((JniWasiInputStream) source).getNativeHandle();
    return nativeBlockingSplice(contextHandle, nativeHandle, sourceHandle, length);
  }

  @Override
  public WasiPollable subscribe() throws WasmException {
    ensureNotClosed();

    final long pollableHandle = nativeSubscribe(contextHandle, nativeHandle);
    if (pollableHandle == 0) {
      throw new WasmException("Failed to create pollable for output stream");
    }
    return new JniWasiPollable(contextHandle, pollableHandle);
  }

  public long getId() {
    return nativeHandle;
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

  public java.time.Instant getCreatedAt() {
    return java.time.Instant.now();
  }

  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.empty();
  }

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

  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null || operation.isEmpty()) {
      throw new IllegalArgumentException("Operation cannot be null or empty");
    }
    ensureNotClosed();

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
  }

  @Override
  protected void doClose() throws Exception {
    nativeClose(contextHandle, nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "WasiOutputStream";
  }

  // Native method declarations

  /**
   * Checks the number of bytes that can be written without blocking.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @return number of bytes that can be written
   * @throws WasmException if the check fails
   */
  private static native long nativeCheckWrite(long contextHandle, long streamHandle)
      throws WasmException;

  /**
   * Writes bytes to the stream without blocking.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param contents bytes to write
   * @throws WasmException if the write operation fails
   */
  private static native void nativeWrite(long contextHandle, long streamHandle, byte[] contents)
      throws WasmException;

  /**
   * Writes bytes and flushes, blocking until complete.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param contents bytes to write
   * @throws WasmException if the write or flush operation fails
   */
  private static native void nativeBlockingWriteAndFlush(
      long contextHandle, long streamHandle, byte[] contents) throws WasmException;

  /**
   * Flushes buffered data to the underlying destination.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws WasmException if the flush operation fails
   */
  private static native void nativeFlush(long contextHandle, long streamHandle)
      throws WasmException;

  /**
   * Flushes buffered data, blocking until complete.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws WasmException if the flush operation fails
   */
  private static native void nativeBlockingFlush(long contextHandle, long streamHandle)
      throws WasmException;

  /**
   * Writes zero bytes to the stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param length number of zero bytes to write
   * @throws WasmException if the write operation fails
   */
  private static native void nativeWriteZeroes(long contextHandle, long streamHandle, long length)
      throws WasmException;

  /**
   * Writes zero bytes and flushes, blocking until complete.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param length number of zero bytes to write
   * @throws WasmException if the write or flush operation fails
   */
  private static native void nativeBlockingWriteZeroesAndFlush(
      long contextHandle, long streamHandle, long length) throws WasmException;

  /**
   * Transfers bytes from an input stream to this output stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param sourceHandle the native source stream handle
   * @param length maximum number of bytes to transfer
   * @return actual number of bytes transferred
   * @throws WasmException if the splice operation fails
   */
  private static native long nativeSplice(
      long contextHandle, long streamHandle, long sourceHandle, long length) throws WasmException;

  /**
   * Transfers bytes from an input stream, blocking until complete.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param sourceHandle the native source stream handle
   * @param length maximum number of bytes to transfer
   * @return actual number of bytes transferred
   * @throws WasmException if the splice operation fails
   */
  private static native long nativeBlockingSplice(
      long contextHandle, long streamHandle, long sourceHandle, long length) throws WasmException;

  /**
   * Creates a pollable for this output stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @return the native pollable handle
   * @throws WasmException if the pollable creation fails
   */
  private static native long nativeSubscribe(long contextHandle, long streamHandle)
      throws WasmException;

  /**
   * Closes the output stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws WasmException if the close operation fails
   */
  private static native void nativeClose(long contextHandle, long streamHandle)
      throws WasmException;
}
