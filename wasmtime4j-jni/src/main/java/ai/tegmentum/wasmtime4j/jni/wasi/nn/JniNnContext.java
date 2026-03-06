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
import ai.tegmentum.wasmtime4j.wasi.nn.NnContext.NnImplementationInfo;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  JniNnContext(final long nativeHandle) {
    super(nativeHandle);
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

    // Wrap single byte array as a 1-element array of arrays
    final byte[][] parts = new byte[][] {modelData};
    final long graphHandle =
        nativeLoadGraph(nativeHandle, parts, encoding.getNativeCode(), target.getNativeCode());
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
        nativeLoadGraph(nativeHandle, partsArray, encoding.getNativeCode(), target.getNativeCode());
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

  @Override
  public NnGraph loadGraphByName(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureNotClosed();

    // WASI-NN doesn't have a native "load by name" API in the host-side backend.
    // This would require a model registry, which is not part of wasmtime-wasi-nn.
    throw new NnException(
        "Loading graphs by name is not supported in the host-side WASI-NN API. "
            + "Use loadGraph() with model bytes instead.");
  }

  @Override
  public Set<NnGraphEncoding> getSupportedEncodings() {
    ensureNotClosed();

    final int[] codes = nativeSupportedEncodings(nativeHandle);
    final Set<NnGraphEncoding> encodings = EnumSet.noneOf(NnGraphEncoding.class);
    if (codes != null) {
      for (final int code : codes) {
        try {
          encodings.add(NnGraphEncoding.fromNativeCode(code));
        } catch (IllegalArgumentException e) {
          LOGGER.fine("Unknown encoding code from native: " + code);
        }
      }
    }
    return encodings;
  }

  @Override
  public Set<NnExecutionTarget> getSupportedTargets() {
    ensureNotClosed();

    // WASI-NN API doesn't expose supported targets per-backend.
    // Return all targets as potentially available; actual availability depends on hardware.
    return EnumSet.allOf(NnExecutionTarget.class);
  }

  @Override
  public boolean isEncodingSupported(final NnGraphEncoding encoding) {
    Objects.requireNonNull(encoding, "encoding cannot be null");
    return getSupportedEncodings().contains(encoding);
  }

  @Override
  public boolean isTargetSupported(final NnExecutionTarget target) {
    Objects.requireNonNull(target, "target cannot be null");
    // All targets are potentially valid; hardware support is checked at load time
    return true;
  }

  @Override
  public boolean isAvailable() {
    if (isClosed()) {
      return false;
    }
    return nativeIsAvailable(nativeHandle);
  }

  @Override
  public NnImplementationInfo getImplementationInfo() {
    ensureNotClosed();

    final String infoJson = nativeGetBackendInfo(nativeHandle);
    return NnImplementationInfo.parseFromJson(infoJson);
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  protected void doClose() throws Exception {
    nativeClose(nativeHandle);
  }

  // ===== Native method declarations =====
  // These match the JNI function names in wasmtime4j-native/src/jni/wasi_nn.rs

  static native long nativeCreate();

  private static native boolean nativeIsAvailable(long handle);

  private static native int[] nativeSupportedEncodings(long handle);

  private static native long nativeLoadGraph(
      long handle, byte[][] parts, int encodingCode, int targetCode);

  private static native String nativeGetBackendInfo(long handle);

  private static native void nativeClose(long handle);
}
