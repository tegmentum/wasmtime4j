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

import ai.tegmentum.wasmtime4j.panama.NativeWasiNnBindings;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of the NnGraph interface for WASI-NN loaded models.
 *
 * <p>This class represents a loaded ML model graph and provides methods for creating execution
 * contexts for inference.
 *
 * @since 1.0.0
 */
public final class PanaNnGraph implements NnGraph {

  private static final Logger LOGGER = Logger.getLogger(PanaNnGraph.class.getName());

  private final MemorySegment nativeHandle;
  private final NnGraphEncoding encoding;
  private final NnExecutionTarget executionTarget;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama WASI-NN graph with the specified parameters.
   *
   * @param nativeHandle the native graph handle
   * @param encoding the model encoding format
   * @param executionTarget the execution target
   * @throws IllegalArgumentException if nativeHandle is null or NULL
   */
  PanaNnGraph(
      final MemorySegment nativeHandle,
      final NnGraphEncoding encoding,
      final NnExecutionTarget executionTarget) {
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native handle cannot be null");
    }
    this.nativeHandle = nativeHandle;
    this.encoding = Objects.requireNonNull(encoding, "encoding cannot be null");
    this.executionTarget =
        Objects.requireNonNull(executionTarget, "executionTarget cannot be null");
    this.resourceHandle =
        new NativeResourceHandle(
            "PanaNnGraph",
            () -> {
              LOGGER.log(Level.FINE, "Closing PanaNnGraph with handle: {0}", nativeHandle);
              final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
              bindings.wasiNnGraphClose(nativeHandle);
            });
    LOGGER.log(Level.FINE, "Created PanaNnGraph with handle: {0}", nativeHandle);
  }

  @Override
  public long getNativeHandle() {
    return nativeHandle.address();
  }

  @Override
  public NnGraphEncoding getEncoding() {
    return encoding;
  }

  @Override
  public NnExecutionTarget getExecutionTarget() {
    return executionTarget;
  }

  @Override
  public NnGraphExecutionContext createExecutionContext() throws NnException {
    ensureNotClosed();

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    final MemorySegment execContextHandle = bindings.wasiNnGraphCreateExecContext(nativeHandle);

    if (execContextHandle == null || execContextHandle.equals(MemorySegment.NULL)) {
      throw new NnException("Failed to create execution context");
    }

    return new PanaNnGraphExecutionContext(execContextHandle, this);
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public String getModelName() {
    if (resourceHandle.isClosed()) {
      return null;
    }
    // The Panama FFI doesn't expose a model name getter, return null
    return null;
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native memory segment handle.
   *
   * @return the native handle as a MemorySegment
   */
  MemorySegment getNativeSegment() {
    return nativeHandle;
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
