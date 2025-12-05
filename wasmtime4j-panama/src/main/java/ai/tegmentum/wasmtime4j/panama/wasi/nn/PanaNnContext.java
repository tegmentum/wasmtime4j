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

import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of the NnContext interface for WASI-NN operations.
 *
 * <p>This class provides WASI-NN machine learning functionality using Panama FFI bindings to the
 * native Wasmtime WASI-NN implementation.
 *
 * @since 1.0.0
 */
public final class PanaNnContext implements NnContext {

  private static final Logger LOGGER = Logger.getLogger(PanaNnContext.class.getName());
  private static final int MAX_ENCODINGS = 16;
  private static final int MAX_TARGETS = 16;

  private final MemorySegment nativeHandle;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new Panama WASI-NN context with the specified native handle.
   *
   * @param nativeHandle the native WASI-NN context handle
   * @throws IllegalArgumentException if nativeHandle is null or NULL
   */
  PanaNnContext(final MemorySegment nativeHandle) {
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native handle cannot be null");
    }
    this.nativeHandle = nativeHandle;
    LOGGER.log(Level.FINE, "Created PanaNnContext with handle: {0}", nativeHandle);
  }

  @Override
  public NnGraph loadGraph(
      final byte[] modelData, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(modelData, "modelData cannot be null");
    Objects.requireNonNull(encoding, "encoding cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment dataSegment = arena.allocate(modelData.length);
      dataSegment.copyFrom(MemorySegment.ofArray(modelData));

      final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
      final MemorySegment graphHandle =
          bindings.wasiNnLoadGraph(
              nativeHandle, dataSegment, modelData.length, encoding.ordinal(), target.ordinal());

      if (graphHandle == null || graphHandle.equals(MemorySegment.NULL)) {
        throw new NnException("Failed to load graph from model data");
      }

      return new PanaNnGraph(graphHandle, encoding, target);
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
    ensureNotClosed();

    // For multi-part models, concatenate the parts and load
    int totalLength = 0;
    for (final byte[] part : modelParts) {
      totalLength += part.length;
    }

    final byte[] combinedData = new byte[totalLength];
    int offset = 0;
    for (final byte[] part : modelParts) {
      System.arraycopy(part, 0, combinedData, offset, part.length);
      offset += part.length;
    }

    return loadGraph(combinedData, encoding, target);
  }

  @Override
  public NnGraph loadGraphFromFile(
      final Path modelPath, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(modelPath, "modelPath cannot be null");
    Objects.requireNonNull(encoding, "encoding cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    ensureNotClosed();

    try {
      final byte[] modelData = Files.readAllBytes(modelPath);
      return loadGraph(modelData, encoding, target);
    } catch (IOException e) {
      throw new NnException("Failed to read model file: " + modelPath, e);
    }
  }

  @Override
  public NnGraph loadGraphByName(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(name);

      final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
      final MemorySegment graphHandle =
          bindings.wasiNnLoadGraphByName(
              nativeHandle, nameSegment, NnExecutionTarget.CPU.ordinal());

      if (graphHandle == null || graphHandle.equals(MemorySegment.NULL)) {
        throw new NnException("Failed to load graph by name: " + name);
      }

      // When loading by name, we don't know the encoding/target - use AUTO/CPU as defaults
      return new PanaNnGraph(graphHandle, NnGraphEncoding.AUTODETECT, NnExecutionTarget.CPU);
    }
  }

  @Override
  public Set<NnGraphEncoding> getSupportedEncodings() {
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outEncodings = arena.allocate(ValueLayout.JAVA_INT, MAX_ENCODINGS);

      final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
      final int count =
          bindings.wasiNnGetSupportedEncodings(nativeHandle, outEncodings, MAX_ENCODINGS);

      final Set<NnGraphEncoding> encodings = EnumSet.noneOf(NnGraphEncoding.class);
      if (count > 0) {
        for (int i = 0; i < count; i++) {
          final int ordinal = outEncodings.getAtIndex(ValueLayout.JAVA_INT, i);
          if (ordinal >= 0 && ordinal < NnGraphEncoding.values().length) {
            encodings.add(NnGraphEncoding.values()[ordinal]);
          }
        }
      }
      return encodings;
    }
  }

  @Override
  public Set<NnExecutionTarget> getSupportedTargets() {
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outTargets = arena.allocate(ValueLayout.JAVA_INT, MAX_TARGETS);

      final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
      final int count = bindings.wasiNnGetSupportedTargets(nativeHandle, outTargets, MAX_TARGETS);

      final Set<NnExecutionTarget> targets = EnumSet.noneOf(NnExecutionTarget.class);
      if (count > 0) {
        for (int i = 0; i < count; i++) {
          final int ordinal = outTargets.getAtIndex(ValueLayout.JAVA_INT, i);
          if (ordinal >= 0 && ordinal < NnExecutionTarget.values().length) {
            targets.add(NnExecutionTarget.values()[ordinal]);
          }
        }
      }
      return targets;
    }
  }

  @Override
  public boolean isEncodingSupported(final NnGraphEncoding encoding) {
    Objects.requireNonNull(encoding, "encoding cannot be null");
    ensureNotClosed();

    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    return bindings.wasiNnIsEncodingSupported(nativeHandle, encoding.ordinal()) != 0;
  }

  @Override
  public boolean isTargetSupported(final NnExecutionTarget target) {
    Objects.requireNonNull(target, "target cannot be null");
    ensureNotClosed();

    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    return bindings.wasiNnIsTargetSupported(nativeHandle, target.ordinal()) != 0;
  }

  @Override
  public boolean isAvailable() {
    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    return bindings.wasiNnIsAvailable() != 0;
  }

  @Override
  public NnImplementationInfo getImplementationInfo() {
    ensureNotClosed();

    // Panama implementation returns basic info since the native API doesn't expose details
    final List<String> backends = new ArrayList<>();
    backends.add("wasmtime-wasi-nn");

    return new NnImplementationInfo("1.0.0", backends, "wasmtime-wasi-nn");
  }

  @Override
  public boolean isValid() {
    return !closed.get();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      LOGGER.log(Level.FINE, "Closing PanaNnContext with handle: {0}", nativeHandle);
      final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
      bindings.wasiNnContextClose(nativeHandle);
    }
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("NnContext has been closed");
    }
  }
}
