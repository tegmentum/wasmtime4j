package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.CodeBuilder;
import ai.tegmentum.wasmtime4j.CodeHint;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import java.lang.foreign.MemorySegment;

/**
 * Panama FFI implementation of the {@link CodeBuilder} interface.
 *
 * @since 1.1.0
 */
final class PanamaCodeBuilder implements CodeBuilder {

  private static final NativeEngineBindings NATIVE_BINDINGS = NativeEngineBindings.getInstance();

  private final MemorySegment engineHandle;
  private MemorySegment nativeHandle;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama code builder for the given engine.
   *
   * @param engineHandle the native engine handle
   * @throws WasmException if the native code builder cannot be created
   */
  PanamaCodeBuilder(final MemorySegment engineHandle) throws WasmException {
    this.engineHandle = engineHandle;
    this.nativeHandle = NATIVE_BINDINGS.codeBuilderCreate(engineHandle);
    if (this.nativeHandle == null || this.nativeHandle.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create native CodeBuilder");
    }
    final MemorySegment handleRef = this.nativeHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaCodeBuilder",
            () -> {
              NATIVE_BINDINGS.codeBuilderDestroy(handleRef);
            });
  }

  @Override
  public CodeBuilder wasmBinary(final byte[] bytes) throws WasmException {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be empty");
    }
    resourceHandle.ensureNotClosed();
    NATIVE_BINDINGS.codeBuilderWasmBinary(nativeHandle, bytes);
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
    resourceHandle.ensureNotClosed();
    NATIVE_BINDINGS.codeBuilderWasmBinaryOrText(nativeHandle, bytes);
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
    resourceHandle.ensureNotClosed();
    NATIVE_BINDINGS.codeBuilderDwarfPackage(nativeHandle, bytes);
    return this;
  }

  @Override
  public CodeBuilder hint(final CodeHint hint) {
    if (hint == null) {
      throw new IllegalArgumentException("hint cannot be null");
    }
    resourceHandle.ensureNotClosed();
    NATIVE_BINDINGS.codeBuilderHint(nativeHandle, hint.ordinal());
    return this;
  }

  @Override
  public CodeBuilder compileTimeBuiltinsBinary(final String name, final byte[] bytes) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be null or empty");
    }
    resourceHandle.ensureNotClosed();
    NATIVE_BINDINGS.codeBuilderCompileTimeBuiltinsBinary(nativeHandle, name, bytes);
    return this;
  }

  @Override
  public CodeBuilder compileTimeBuiltinsBinaryOrText(final String name, final byte[] bytes) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be null or empty");
    }
    resourceHandle.ensureNotClosed();
    NATIVE_BINDINGS.codeBuilderCompileTimeBuiltinsBinaryOrText(nativeHandle, name, bytes);
    return this;
  }

  @Override
  public CodeBuilder exposeUnsafeIntrinsics(final String importName) {
    if (importName == null || importName.isEmpty()) {
      throw new IllegalArgumentException("importName cannot be null or empty");
    }
    resourceHandle.ensureNotClosed();
    NATIVE_BINDINGS.codeBuilderExposeUnsafeIntrinsics(nativeHandle, importName);
    return this;
  }

  @Override
  public Module compileModule() throws WasmException {
    resourceHandle.ensureNotClosed();
    final MemorySegment moduleHandle = NATIVE_BINDINGS.codeBuilderCompileModule(nativeHandle);
    if (moduleHandle == null || moduleHandle.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to compile module via CodeBuilder");
    }
    return new PanamaModule(moduleHandle);
  }

  @Override
  public byte[] compileModuleSerialized() throws WasmException {
    resourceHandle.ensureNotClosed();
    return NATIVE_BINDINGS.codeBuilderCompileModuleSerialized(nativeHandle);
  }

  @Override
  public long compileComponent() throws WasmException {
    resourceHandle.ensureNotClosed();
    final MemorySegment componentHandle = NATIVE_BINDINGS.codeBuilderCompileComponent(nativeHandle);
    if (componentHandle == null || componentHandle.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to compile component via CodeBuilder");
    }
    return componentHandle.address();
  }

  @Override
  public byte[] compileComponentSerialized() throws WasmException {
    resourceHandle.ensureNotClosed();
    return NATIVE_BINDINGS.codeBuilderCompileComponentSerialized(nativeHandle);
  }

  @Override
  public void close() {
    resourceHandle.close();
  }
}
