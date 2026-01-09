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

package ai.tegmentum.wasmtime4j.jni.wasi.nn;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JNI implementation of the NnContext interface for WASI-NN operations.
 *
 * <p>This class provides WASI-NN machine learning functionality using JNI bindings to the native
 * Wasmtime WASI-NN implementation.
 *
 * @since 1.0.0
 */
public final class JniNnContext extends JniResource implements NnContext {

  private static final Logger LOGGER = Logger.getLogger(JniNnContext.class.getName());

  /**
   * Creates a new JNI WASI-NN context with the specified native handle.
   *
   * @param nativeHandle the native WASI-NN context handle
   * @throws IllegalArgumentException if nativeHandle is 0
   */
  public JniNnContext(final long nativeHandle) {
    super(nativeHandle);
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be 0");
    }
  }

  @Override
  protected String getResourceType() {
    return "NnContext";
  }

  @Override
  public NnGraph loadGraph(
      final byte[] modelData, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(modelData, "modelData cannot be null");
    Objects.requireNonNull(encoding, "encoding cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    ensureNotClosed();

    final long graphHandle =
        nativeLoadGraph(nativeHandle, modelData, encoding.ordinal(), target.ordinal());
    if (graphHandle == 0) {
      throw new NnException("Failed to load graph from model data");
    }
    return new JniNnGraph(graphHandle, encoding, target);
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

    final byte[][] partsArray = modelParts.toArray(new byte[0][]);
    final long graphHandle =
        nativeLoadGraphMultiPart(nativeHandle, partsArray, encoding.ordinal(), target.ordinal());
    if (graphHandle == 0) {
      throw new NnException("Failed to load graph from model parts");
    }
    return new JniNnGraph(graphHandle, encoding, target);
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

  /**
   * Loads a neural network graph from a file path specified as a string.
   *
   * @param modelPath the file path to the model
   * @param encoding the model encoding format
   * @param target the execution target
   * @return the loaded graph
   * @throws NnException if loading fails
   */
  public NnGraph loadGraphFromFile(
      final String modelPath, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(modelPath, "modelPath cannot be null");
    return loadGraphFromFile(Paths.get(modelPath), encoding, target);
  }

  @Override
  public NnGraph loadGraphByName(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureNotClosed();

    final long graphHandle = nativeLoadGraphByName(nativeHandle, name);
    if (graphHandle == 0) {
      throw new NnException("Failed to load graph by name: " + name);
    }
    // When loading by name, we don't know the encoding/target - use AUTO/CPU as defaults
    return new JniNnGraph(graphHandle, NnGraphEncoding.AUTODETECT, NnExecutionTarget.CPU);
  }

  /**
   * Loads a neural network graph by its registered name with explicit encoding and target.
   *
   * @param name the registered graph name
   * @param encoding the expected model encoding format
   * @param target the execution target
   * @return the loaded graph
   * @throws NnException if loading fails
   */
  public NnGraph loadGraphByName(
      final String name, final NnGraphEncoding encoding, final NnExecutionTarget target)
      throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(encoding, "encoding cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    ensureNotClosed();

    final long graphHandle = nativeLoadGraphByName(nativeHandle, name);
    if (graphHandle == 0) {
      throw new NnException("Failed to load graph by name: " + name);
    }
    return new JniNnGraph(graphHandle, encoding, target);
  }

  @Override
  public Set<NnGraphEncoding> getSupportedEncodings() {
    ensureNotClosed();

    final int[] encodingOrdinals = nativeGetSupportedEncodings(nativeHandle);
    final Set<NnGraphEncoding> encodings = EnumSet.noneOf(NnGraphEncoding.class);
    for (final int ordinal : encodingOrdinals) {
      if (ordinal >= 0 && ordinal < NnGraphEncoding.values().length) {
        encodings.add(NnGraphEncoding.values()[ordinal]);
      }
    }
    return encodings;
  }

  @Override
  public Set<NnExecutionTarget> getSupportedTargets() {
    ensureNotClosed();

    final int[] targetOrdinals = nativeGetSupportedTargets(nativeHandle);
    final Set<NnExecutionTarget> targets = EnumSet.noneOf(NnExecutionTarget.class);
    for (final int ordinal : targetOrdinals) {
      if (ordinal >= 0 && ordinal < NnExecutionTarget.values().length) {
        targets.add(NnExecutionTarget.values()[ordinal]);
      }
    }
    return targets;
  }

  @Override
  public boolean isEncodingSupported(final NnGraphEncoding encoding) {
    Objects.requireNonNull(encoding, "encoding cannot be null");
    ensureNotClosed();
    return nativeIsEncodingSupported(nativeHandle, encoding.ordinal()) != 0;
  }

  @Override
  public boolean isTargetSupported(final NnExecutionTarget target) {
    Objects.requireNonNull(target, "target cannot be null");
    ensureNotClosed();
    return nativeIsTargetSupported(nativeHandle, target.ordinal()) != 0;
  }

  @Override
  public boolean isAvailable() {
    return nativeIsAvailable(nativeHandle) != 0;
  }

  @Override
  public NnImplementationInfo getImplementationInfo() {
    ensureNotClosed();

    final String version = nativeGetVersion(nativeHandle);
    final String[] backendsArray = nativeGetBackends(nativeHandle);
    final List<String> backends =
        backendsArray != null ? Arrays.asList(backendsArray) : new ArrayList<>();
    final String defaultBackend = nativeGetDefaultBackend(nativeHandle);

    return new NnImplementationInfo(version, backends, defaultBackend);
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  protected void doClose() throws Exception {
    if (nativeHandle != 0) {
      nativeClose(nativeHandle);
    }
  }

  // ===== Native method declarations =====

  private static native long nativeLoadGraph(
      long contextHandle, byte[] modelData, int encodingOrdinal, int targetOrdinal);

  private static native long nativeLoadGraphMultiPart(
      long contextHandle, byte[][] modelParts, int encodingOrdinal, int targetOrdinal);

  private static native long nativeLoadGraphByName(long contextHandle, String name);

  private static native int[] nativeGetSupportedEncodings(long contextHandle);

  private static native int[] nativeGetSupportedTargets(long contextHandle);

  private static native int nativeIsEncodingSupported(long contextHandle, int encodingOrdinal);

  private static native int nativeIsTargetSupported(long contextHandle, int targetOrdinal);

  private static native int nativeIsAvailable(long contextHandle);

  private static native String nativeGetVersion(long contextHandle);

  private static native String[] nativeGetBackends(long contextHandle);

  private static native String nativeGetDefaultBackend(long contextHandle);

  private static native void nativeClose(long contextHandle);
}
