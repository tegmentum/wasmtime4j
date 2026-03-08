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
  private final PanamaEngine engine;
  private MemorySegment nativeHandle;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama code builder for the given engine.
   *
   * @param engineHandle the native engine handle
   * @param engine the engine reference for modules created by this builder
   * @throws WasmException if the native code builder cannot be created
   */
  PanamaCodeBuilder(final MemorySegment engineHandle, final PanamaEngine engine)
      throws WasmException {
    this.engineHandle = engineHandle;
    this.engine = engine;
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
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.codeBuilderWasmBinary(nativeHandle, bytes);
      return this;
    } finally {
      resourceHandle.endOperation();
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
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.codeBuilderWasmBinaryOrText(nativeHandle, bytes);
      return this;
    } finally {
      resourceHandle.endOperation();
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
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.codeBuilderDwarfPackage(nativeHandle, bytes);
      return this;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public CodeBuilder hint(final CodeHint hint) {
    if (hint == null) {
      throw new IllegalArgumentException("hint cannot be null");
    }
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.codeBuilderHint(nativeHandle, hint.ordinal());
      return this;
    } finally {
      resourceHandle.endOperation();
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
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.codeBuilderCompileTimeBuiltinsBinary(nativeHandle, name, bytes);
      return this;
    } finally {
      resourceHandle.endOperation();
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
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.codeBuilderCompileTimeBuiltinsBinaryOrText(nativeHandle, name, bytes);
      return this;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public CodeBuilder exposeUnsafeIntrinsics(final String importName) {
    if (importName == null || importName.isEmpty()) {
      throw new IllegalArgumentException("importName cannot be null or empty");
    }
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.codeBuilderExposeUnsafeIntrinsics(nativeHandle, importName);
      return this;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Module compileModule() throws WasmException {
    resourceHandle.beginOperation();
    try {
      final MemorySegment moduleHandle = NATIVE_BINDINGS.codeBuilderCompileModule(nativeHandle);
      if (moduleHandle == null || moduleHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to compile module via CodeBuilder");
      }
      return new PanamaModule(engine, moduleHandle);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public byte[] compileModuleSerialized() throws WasmException {
    resourceHandle.beginOperation();
    try {
      return NATIVE_BINDINGS.codeBuilderCompileModuleSerialized(nativeHandle);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long compileComponent() throws WasmException {
    resourceHandle.beginOperation();
    try {
      final MemorySegment componentHandle =
          NATIVE_BINDINGS.codeBuilderCompileComponent(nativeHandle);
      if (componentHandle == null || componentHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to compile component via CodeBuilder");
      }
      return componentHandle.address();
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public byte[] compileComponentSerialized() throws WasmException {
    resourceHandle.beginOperation();
    try {
      return NATIVE_BINDINGS.codeBuilderCompileComponentSerialized(nativeHandle);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void close() {
    resourceHandle.close();
  }
}
