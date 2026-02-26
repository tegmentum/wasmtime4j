package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.CodeBuilder;
import ai.tegmentum.wasmtime4j.CodeHint;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.ErrorMapper;
import ai.tegmentum.wasmtime4j.exception.WasmException;

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
    ensureNotClosed();
    final int result = nativeWasmBinary(nativeHandle, bytes);
    if (result != 0) {
      throw ErrorMapper.mapErrorCode(result, "Failed to set wasm binary");
    }
    return this;
  }

  @Override
  public CodeBuilder wasmBinaryOrText(final byte[] bytes) throws WasmException {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be empty");
    }
    ensureNotClosed();
    final int result = nativeWasmBinaryOrText(nativeHandle, bytes);
    if (result != 0) {
      throw ErrorMapper.mapErrorCode(result, "Failed to set wasm binary or text");
    }
    return this;
  }

  @Override
  public CodeBuilder dwarfPackage(final byte[] bytes) throws WasmException {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be empty");
    }
    ensureNotClosed();
    final int result = nativeDwarfPackage(nativeHandle, bytes);
    if (result != 0) {
      throw ErrorMapper.mapErrorCode(result, "Failed to set DWARF package");
    }
    return this;
  }

  @Override
  public CodeBuilder hint(final CodeHint hint) {
    if (hint == null) {
      throw new IllegalArgumentException("hint cannot be null");
    }
    ensureNotClosed();
    nativeHint(nativeHandle, hint.ordinal());
    return this;
  }

  @Override
  public Module compileModule() throws WasmException {
    ensureNotClosed();
    final long moduleHandle = nativeCompileModule(nativeHandle);
    if (moduleHandle == 0) {
      throw new WasmException("Failed to compile module via CodeBuilder");
    }
    return new JniModule(moduleHandle, null);
  }

  @Override
  public byte[] compileModuleSerialized() throws WasmException {
    ensureNotClosed();
    final byte[] result = nativeCompileModuleSerialized(nativeHandle);
    if (result == null || result.length == 0) {
      throw new WasmException("Failed to compile module serialized via CodeBuilder");
    }
    return result;
  }

  @Override
  public long compileComponent() throws WasmException {
    ensureNotClosed();
    final long componentHandle = nativeCompileComponent(nativeHandle);
    if (componentHandle == 0) {
      throw new WasmException("Failed to compile component via CodeBuilder");
    }
    return componentHandle;
  }

  @Override
  public byte[] compileComponentSerialized() throws WasmException {
    ensureNotClosed();
    final byte[] result = nativeCompileComponentSerialized(nativeHandle);
    if (result == null || result.length == 0) {
      throw new WasmException("Failed to compile component serialized via CodeBuilder");
    }
    return result;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      if (nativeHandle != 0) {
        nativeDestroy(nativeHandle);
        nativeHandle = 0;
      }
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("CodeBuilder has been closed");
    }
  }

  // Native methods
  private static native long nativeCreate(long engineHandle);

  private static native int nativeWasmBinary(long builderHandle, byte[] bytes);

  private static native int nativeWasmBinaryOrText(long builderHandle, byte[] bytes);

  private static native int nativeDwarfPackage(long builderHandle, byte[] bytes);

  private static native void nativeHint(long builderHandle, int hintOrdinal);

  private static native long nativeCompileModule(long builderHandle);

  private static native byte[] nativeCompileModuleSerialized(long builderHandle);

  private static native long nativeCompileComponent(long builderHandle);

  private static native byte[] nativeCompileComponentSerialized(long builderHandle);

  private static native void nativeDestroy(long builderHandle);
}
