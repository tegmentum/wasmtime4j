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
package ai.tegmentum.wasmtime4j.jni.wasi.io;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiInputStream interface.
 *
 * <p>This class provides access to WASI Preview 2 input stream operations through JNI calls to the
 * native Wasmtime library. Input streams support non-blocking and blocking read operations.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class JniWasiInputStream extends JniResource implements WasiInputStream {

  private static final Logger LOGGER = Logger.getLogger(JniWasiInputStream.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiInputStream: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  private final java.time.Instant createdAt = java.time.Instant.now();
  private volatile java.time.Instant lastAccessedAt;

  /**
   * Creates a new JNI WASI input stream with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws IllegalArgumentException if either handle is 0
   */
  public JniWasiInputStream(final long contextHandle, final long streamHandle) {
    super(streamHandle);
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI input stream with handle: " + streamHandle);
  }

  @Override
  public byte[] read(final long length) throws WasmException {
    Validation.requirePositive(length, "length");
    beginOperation();
    try {
      final byte[] result = nativeRead(contextHandle, nativeHandle, length);
      if (result == null) {
        throw new WasmException("Native read returned null");
      }
      lastAccessedAt = java.time.Instant.now();
      return result;
    } finally {
      endOperation();
    }
  }

  @Override
  public byte[] blockingRead(final long length) throws WasmException {
    Validation.requirePositive(length, "length");
    beginOperation();
    try {
      final byte[] result = nativeBlockingRead(contextHandle, nativeHandle, length);
      if (result == null) {
        throw new WasmException("Native blocking read returned null");
      }
      lastAccessedAt = java.time.Instant.now();
      return result;
    } finally {
      endOperation();
    }
  }

  @Override
  public long skip(final long length) throws WasmException {
    Validation.requirePositive(length, "length");
    beginOperation();
    try {
      final long skipped = nativeSkip(contextHandle, nativeHandle, length);
      lastAccessedAt = java.time.Instant.now();
      return skipped;
    } finally {
      endOperation();
    }
  }

  @Override
  public long blockingSkip(final long length) throws WasmException {
    Validation.requirePositive(length, "length");
    beginOperation();
    try {
      // Note: Current native implementation doesn't have separate blocking skip
      // so we use the same as skip for now
      return nativeSkip(contextHandle, nativeHandle, length);
    } finally {
      endOperation();
    }
  }

  @Override
  public WasiPollable subscribe() throws WasmException {
    beginOperation();
    try {
      final long pollableHandle = nativeSubscribe(contextHandle, nativeHandle);
      if (pollableHandle == 0) {
        throw new WasmException("Failed to create pollable for input stream");
      }
      return new JniWasiPollable(contextHandle, pollableHandle);
    } finally {
      endOperation();
    }
  }

  public long getId() {
    return nativeHandle;
  }

  public String getType() {
    return "wasi:io/input-stream";
  }

  public boolean isOwned() {
    return true; // WASI streams are owned by default
  }

  public boolean isValid() {
    return !isClosed();
  }

  public java.time.Instant getCreatedAt() {
    return createdAt;
  }

  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.ofNullable(lastAccessedAt);
  }

  public java.util.List<String> getAvailableOperations() {
    return java.util.Arrays.asList("read", "blocking-read", "skip", "blocking-skip", "subscribe");
  }

  /**
   * Invokes a named WASI input-stream operation with the given parameters.
   *
   * @param operation the operation name to invoke
   * @param parameters the operation-specific parameters
   * @return the result of the operation, or {@code null} for void operations
   * @throws WasmException if the operation fails
   * @throws IllegalArgumentException if the operation is null, empty, or unknown
   */
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null || operation.isEmpty()) {
      throw new IllegalArgumentException("Operation cannot be null or empty");
    }
    beginOperation();
    try {
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
    } finally {
      endOperation();
    }
  }

  @Override
  protected void doClose() throws Exception {
    nativeClose(contextHandle, nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "WasiInputStream";
  }

  // Native method declarations

  /**
   * Reads data from the input stream (non-blocking).
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param length the maximum number of bytes to read
   * @return the bytes read
   * @throws WasmException if the read operation fails
   */
  private static native byte[] nativeRead(long contextHandle, long streamHandle, long length)
      throws WasmException;

  /**
   * Reads data from the input stream (blocking).
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param length the maximum number of bytes to read
   * @return the bytes read
   * @throws WasmException if the read operation fails
   */
  private static native byte[] nativeBlockingRead(
      long contextHandle, long streamHandle, long length) throws WasmException;

  /**
   * Skips bytes in the input stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @param length the number of bytes to skip
   * @return the actual number of bytes skipped
   * @throws WasmException if the skip operation fails
   */
  private static native long nativeSkip(long contextHandle, long streamHandle, long length)
      throws WasmException;

  /**
   * Creates a pollable for this input stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @return the native pollable handle
   * @throws WasmException if the pollable creation fails
   */
  private static native long nativeSubscribe(long contextHandle, long streamHandle)
      throws WasmException;

  /**
   * Closes the input stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws WasmException if the close operation fails
   */
  private static native void nativeClose(long contextHandle, long streamHandle)
      throws WasmException;
}
