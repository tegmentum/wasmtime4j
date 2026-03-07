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
package ai.tegmentum.wasmtime4j.panama.wasi.nn;

import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Panama implementation of the NnContext interface for WASI-NN operations.
 *
 * <p>This class provides WASI-NN machine learning functionality using Panama FFI bindings to the
 * native Wasmtime WASI-NN implementation.
 *
 * @since 1.0.0
 */
public final class PanamaNnContext implements NnContext {

  private static final Logger LOGGER = Logger.getLogger(PanamaNnContext.class.getName());

  private final MemorySegment nativePtr;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama WASI-NN context with the specified native pointer.
   *
   * @param nativePtr the native context pointer
   */
  PanamaNnContext(final MemorySegment nativePtr) {
    if (nativePtr == null || nativePtr.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native pointer cannot be null");
    }
    this.nativePtr = nativePtr;

    // Capture nativePtr for cleanup (don't capture 'this')
    final MemorySegment ptrForCleanup = nativePtr;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaNnContext",
            () -> NativeWasiNnBindings.getInstance().nnContextClose(ptrForCleanup));
  }

  @Override
  public NnGraph loadGraph(
      final byte[] modelData, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(modelData, "modelData cannot be null");
    Objects.requireNonNull(encoding, "encoding cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    resourceHandle.beginOperation();
    try {

      final byte[][] parts = new byte[][] {modelData};
      return loadGraphInternal(parts, encoding, target);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public NnGraph loadGraph(
      final List<byte[]> modelParts, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(modelParts, "modelParts cannot be null");
    Objects.requireNonNull(encoding, "encoding cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    if (modelParts.isEmpty()) {
      throw new IllegalArgumentException("modelParts cannot be empty");
    }
    resourceHandle.beginOperation();
    try {

      final byte[][] partsArray = modelParts.toArray(new byte[0][]);
      return loadGraphInternal(partsArray, encoding, target);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public NnGraph loadGraphFromFile(
      final Path modelPath, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(modelPath, "modelPath cannot be null");
    try {
      final byte[] data = Files.readAllBytes(modelPath);
      return loadGraph(data, encoding, target);
    } catch (IOException e) {
      throw new NnException("Failed to read model file: " + modelPath, e);
    }
  }

  @Override
  public NnGraph loadGraphByName(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    throw new NnException(
        "Loading graphs by name is not supported in the host-side WASI-NN API. "
            + "Use loadGraph() with model bytes instead.");
  }

  @Override
  public Set<NnGraphEncoding> getSupportedEncodings() {
    resourceHandle.beginOperation();
    try {

      final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
      try (Arena arena = Arena.ofConfined()) {
        final int[] codes = bindings.nnContextSupportedEncodings(arena, nativePtr);
        final Set<NnGraphEncoding> encodings = EnumSet.noneOf(NnGraphEncoding.class);
        for (final int code : codes) {
          try {
            encodings.add(NnGraphEncoding.fromNativeCode(code));
          } catch (IllegalArgumentException e) {
            LOGGER.fine("Unknown encoding code from native: " + code);
          }
        }
        return encodings;
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Set<NnExecutionTarget> getSupportedTargets() {
    resourceHandle.beginOperation();
    try {
      return EnumSet.allOf(NnExecutionTarget.class);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isEncodingSupported(final NnGraphEncoding encoding) {
    Objects.requireNonNull(encoding, "encoding cannot be null");
    return getSupportedEncodings().contains(encoding);
  }

  @Override
  public boolean isTargetSupported(final NnExecutionTarget target) {
    Objects.requireNonNull(target, "target cannot be null");
    return true;
  }

  @Override
  public boolean isAvailable() {
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      return NativeWasiNnBindings.getInstance().nnContextIsAvailable(nativePtr) != 0;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public NnImplementationInfo getImplementationInfo() {
    resourceHandle.beginOperation();
    try {

      final String json = NativeWasiNnBindings.getInstance().nnContextGetBackendInfo(nativePtr);
      return NnImplementationInfo.parseFromJson(json);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  private NnGraph loadGraphInternal(
      final byte[][] parts, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment graphPtr =
          bindings.nnContextLoadGraph(
              arena, nativePtr, parts, encoding.getNativeCode(), target.getNativeCode());
      if (graphPtr.equals(MemorySegment.NULL)) {
        throw new NnException("Failed to load graph");
      }
      return new PanamaNnGraph(graphPtr, encoding, target);
    }
  }
}
