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
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * JNI implementation of the NnGraph interface for WASI-NN loaded models.
 *
 * <p>This class represents a loaded ML model graph and provides methods for creating execution
 * contexts for inference.
 *
 * @since 1.0.0
 */
public final class JniNnGraph extends JniResource implements NnGraph {

  private static final Logger LOGGER = Logger.getLogger(JniNnGraph.class.getName());

  private final NnGraphEncoding encoding;
  private final NnExecutionTarget executionTarget;

  /**
   * Creates a new JNI WASI-NN graph with the specified parameters.
   *
   * @param nativeHandle the native graph handle
   * @param encoding the model encoding format
   * @param executionTarget the execution target
   */
  JniNnGraph(
      final long nativeHandle,
      final NnGraphEncoding encoding,
      final NnExecutionTarget executionTarget) {
    super(nativeHandle);
    this.encoding = Objects.requireNonNull(encoding, "encoding cannot be null");
    this.executionTarget =
        Objects.requireNonNull(executionTarget, "executionTarget cannot be null");
  }

  @Override
  protected String getResourceType() {
    return "NnGraph";
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
    beginOperation();
    try {
      final long execHandle = nativeCreateExecCtx(nativeHandle);
      if (execHandle == 0) {
        throw new NnException("Failed to create execution context");
      }
      return new JniNnGraphExecutionContext(execHandle, this);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  public String getModelName() {
    // WASI-NN API does not expose model names through the host-side backend
    return null;
  }

  @Override
  protected void doClose() throws Exception {
    nativeClose(nativeHandle);
  }

  // ===== Native method declarations =====
  // These match the JNI function names in wasmtime4j-native/src/jni/wasi_nn.rs

  private static native int nativeGetEncoding(long handle);

  private static native int nativeGetTarget(long handle);

  private static native long nativeCreateExecCtx(long handle);

  private static native void nativeClose(long handle);
}
