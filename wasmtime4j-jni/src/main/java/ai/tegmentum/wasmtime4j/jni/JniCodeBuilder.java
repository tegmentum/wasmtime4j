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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.CodeBuilder;
import ai.tegmentum.wasmtime4j.CodeHint;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.ErrorMapper;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * JNI implementation of the {@link CodeBuilder} interface.
 *
 * <p>Wraps a native {@code CodeBuilderState} handle and delegates compilation to the native layer.
 *
 * @since 1.1.0
 */
final class JniCodeBuilder implements CodeBuilder {

  private long nativeHandle;
  private final long engineHandle;
  private volatile boolean closed;
  private final ReentrantReadWriteLock closeLock = new ReentrantReadWriteLock();

  // Load native library
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Creates a new JNI code builder for the given engine.
   *
   * @param engineHandle the native engine handle
   * @throws WasmException if the native code builder cannot be created
   */
  JniCodeBuilder(final long engineHandle) throws WasmException {
    this.engineHandle = engineHandle;
    this.nativeHandle = nativeCreate(engineHandle);
    if (this.nativeHandle == 0) {
      throw new WasmException("Failed to create native CodeBuilder");
    }
  }

  @Override
  public CodeBuilder wasmBinary(final byte[] bytes) throws WasmException {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be empty");
    }
    beginOperation();
    try {
      final int result = nativeWasmBinary(nativeHandle, bytes);
      if (result != 0) {
        throw ErrorMapper.mapErrorCode(result, "Failed to set wasm binary");
      }
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public CodeBuilder wasmBinaryOrText(final byte[] bytes) throws WasmException {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be empty");
    }
    beginOperation();
    try {
      final int result = nativeWasmBinaryOrText(nativeHandle, bytes);
      if (result != 0) {
        throw ErrorMapper.mapErrorCode(result, "Failed to set wasm binary or text");
      }
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public CodeBuilder dwarfPackage(final byte[] bytes) throws WasmException {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be empty");
    }
    beginOperation();
    try {
      final int result = nativeDwarfPackage(nativeHandle, bytes);
      if (result != 0) {
        throw ErrorMapper.mapErrorCode(result, "Failed to set DWARF package");
      }
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public CodeBuilder hint(final CodeHint hint) {
    if (hint == null) {
      throw new IllegalArgumentException("hint cannot be null");
    }
    beginOperation();
    try {
      nativeHint(nativeHandle, hint.ordinal());
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public CodeBuilder compileTimeBuiltinsBinary(final String name, final byte[] bytes) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be null or empty");
    }
    beginOperation();
    try {
      nativeCompileTimeBuiltinsBinary(nativeHandle, name, bytes);
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public CodeBuilder compileTimeBuiltinsBinaryOrText(final String name, final byte[] bytes) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be null or empty");
    }
    beginOperation();
    try {
      nativeCompileTimeBuiltinsBinaryOrText(nativeHandle, name, bytes);
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public CodeBuilder exposeUnsafeIntrinsics(final String importName) {
    if (importName == null || importName.isEmpty()) {
      throw new IllegalArgumentException("importName cannot be null or empty");
    }
    beginOperation();
    try {
      nativeExposeUnsafeIntrinsics(nativeHandle, importName);
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public Module compileModule() throws WasmException {
    beginOperation();
    try {
      final long moduleHandle = nativeCompileModule(nativeHandle);
      if (moduleHandle == 0) {
        throw new WasmException("Failed to compile module via CodeBuilder");
      }
      return new JniModule(moduleHandle, null);
    } finally {
      endOperation();
    }
  }

  @Override
  public byte[] compileModuleSerialized() throws WasmException {
    beginOperation();
    try {
      final byte[] result = nativeCompileModuleSerialized(nativeHandle);
      if (result == null || result.length == 0) {
        throw new WasmException("Failed to compile module serialized via CodeBuilder");
      }
      return result;
    } finally {
      endOperation();
    }
  }

  @Override
  public long compileComponent() throws WasmException {
    beginOperation();
    try {
      final long componentHandle = nativeCompileComponent(nativeHandle);
      if (componentHandle == 0) {
        throw new WasmException("Failed to compile component via CodeBuilder");
      }
      return componentHandle;
    } finally {
      endOperation();
    }
  }

  @Override
  public byte[] compileComponentSerialized() throws WasmException {
    beginOperation();
    try {
      final byte[] result = nativeCompileComponentSerialized(nativeHandle);
      if (result == null || result.length == 0) {
        throw new WasmException("Failed to compile component serialized via CodeBuilder");
      }
      return result;
    } finally {
      endOperation();
    }
  }

  @Override
  public void close() {
    closeLock.writeLock().lock();
    try {
      if (!closed) {
        closed = true;
        if (nativeHandle != 0) {
          nativeDestroy(nativeHandle);
          nativeHandle = 0;
        }
      }
    } finally {
      closeLock.writeLock().unlock();
    }
  }

  private void beginOperation() {
    closeLock.readLock().lock();
    if (closed) {
      closeLock.readLock().unlock();
      throw new IllegalStateException("CodeBuilder has been closed");
    }
  }

  private void endOperation() {
    closeLock.readLock().unlock();
  }

  // Native methods
  private static native long nativeCreate(long engineHandle);

  private static native int nativeWasmBinary(long builderHandle, byte[] bytes);

  private static native int nativeWasmBinaryOrText(long builderHandle, byte[] bytes);

  private static native int nativeDwarfPackage(long builderHandle, byte[] bytes);

  private static native void nativeHint(long builderHandle, int hintOrdinal);

  private static native void nativeCompileTimeBuiltinsBinary(
      long builderHandle, String name, byte[] bytes);

  private static native void nativeCompileTimeBuiltinsBinaryOrText(
      long builderHandle, String name, byte[] bytes);

  private static native void nativeExposeUnsafeIntrinsics(long builderHandle, String importName);

  private static native long nativeCompileModule(long builderHandle);

  private static native byte[] nativeCompileModuleSerialized(long builderHandle);

  private static native long nativeCompileComponent(long builderHandle);

  private static native byte[] nativeCompileComponentSerialized(long builderHandle);

  private static native void nativeDestroy(long builderHandle);
}
