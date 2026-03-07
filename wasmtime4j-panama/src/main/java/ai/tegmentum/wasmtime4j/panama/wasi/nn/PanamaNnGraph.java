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
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama implementation of the NnGraph interface for WASI-NN loaded models.
 *
 * @since 1.0.0
 */
public final class PanamaNnGraph implements NnGraph {

  private static final Logger LOGGER = Logger.getLogger(PanamaNnGraph.class.getName());

  private final MemorySegment nativePtr;
  private final NativeResourceHandle resourceHandle;
  private final NnGraphEncoding encoding;
  private final NnExecutionTarget executionTarget;

  /**
   * Creates a new Panama WASI-NN graph.
   *
   * @param nativePtr the native graph pointer
   * @param encoding the model encoding format
   * @param executionTarget the execution target
   */
  PanamaNnGraph(
      final MemorySegment nativePtr,
      final NnGraphEncoding encoding,
      final NnExecutionTarget executionTarget) {
    if (nativePtr == null || nativePtr.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native pointer cannot be null");
    }
    this.nativePtr = nativePtr;
    this.encoding = Objects.requireNonNull(encoding, "encoding cannot be null");
    this.executionTarget =
        Objects.requireNonNull(executionTarget, "executionTarget cannot be null");

    final MemorySegment ptrForCleanup = nativePtr;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaNnGraph", () -> NativeWasiNnBindings.getInstance().nnGraphClose(ptrForCleanup));
  }

  @Override
  public long getNativeHandle() {
    resourceHandle.beginOperation();
    try {
      return nativePtr.address();
    } finally {
      resourceHandle.endOperation();
    }
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
    resourceHandle.beginOperation();
    try {

      final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment execPtr = bindings.nnGraphCreateExecCtx(arena, nativePtr);
        return new PanamaNnGraphExecutionContext(execPtr, this);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public String getModelName() {
    return null;
  }

  @Override
  public void close() {
    resourceHandle.close();
  }
}
