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
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * JNI implementation of the NnGraphExecutionContext interface for WASI-NN inference.
 *
 * <p>This class provides execution context functionality for running ML inference operations using
 * JNI bindings to the native Wasmtime WASI-NN implementation. Output tensors are received as
 * serialized bytes from native code in the format: {@code [num_dims:i32][dim0:i32]...[dimN:i32]
 * [tensor_type:i32][data bytes]}.
 *
 * @since 1.0.0
 */
public final class JniNnGraphExecutionContext extends JniResource
    implements NnGraphExecutionContext {

  private static final Logger LOGGER = Logger.getLogger(JniNnGraphExecutionContext.class.getName());

  private final NnGraph graph;

  /**
   * Creates a new JNI WASI-NN execution context.
   *
   * @param nativeHandle the native execution context handle
   * @param graph the parent graph
   */
  JniNnGraphExecutionContext(final long nativeHandle, final NnGraph graph) {
    super(nativeHandle);
    this.graph = Objects.requireNonNull(graph, "graph cannot be null");
  }

  @Override
  protected String getResourceType() {
    return "NnGraphExecutionContext";
  }

  @Override
  public NnGraph getGraph() {
    return graph;
  }

  @Override
  public List<NnTensor> compute(final List<NnTensor> inputs) throws NnException {
    Objects.requireNonNull(inputs, "inputs cannot be null");
    if (inputs.isEmpty()) {
      throw new IllegalArgumentException("inputs cannot be empty");
    }
    for (int i = 0; i < inputs.size(); i++) {
      if (inputs.get(i) == null) {
        throw new IllegalArgumentException("Input tensor at index " + i + " cannot be null");
      }
    }
    ensureNotClosed();

    // Set each input tensor
    for (int i = 0; i < inputs.size(); i++) {
      final NnTensor tensor = inputs.get(i);
      if (tensor.isNamed()) {
        setInput(tensor.getName(), tensor);
      } else {
        setInput(i, tensor);
      }
    }

    return computeNoInputs();
  }

  @Override
  public List<NnTensor> computeByIndex(final NnTensor... inputs) throws NnException {
    Objects.requireNonNull(inputs, "inputs cannot be null");
    if (inputs.length == 0) {
      throw new IllegalArgumentException("inputs cannot be empty");
    }
    for (int i = 0; i < inputs.length; i++) {
      if (inputs[i] == null) {
        throw new IllegalArgumentException("Input tensor at index " + i + " cannot be null");
      }
    }
    ensureNotClosed();

    for (int i = 0; i < inputs.length; i++) {
      setInput(i, inputs[i]);
    }

    return computeNoInputs();
  }

  @Override
  public void setInput(final int index, final NnTensor tensor) throws NnException {
    Objects.requireNonNull(tensor, "tensor cannot be null");
    if (index < 0) {
      throw new IndexOutOfBoundsException("Input index cannot be negative: " + index);
    }
    ensureNotClosed();

    nativeSetInputByIndex(
        nativeHandle,
        index,
        tensor.getDimensions(),
        tensor.getType().getNativeCode(),
        tensor.getData());
  }

  @Override
  public void setInput(final String name, final NnTensor tensor) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(tensor, "tensor cannot be null");
    ensureNotClosed();

    nativeSetInputByName(
        nativeHandle,
        name,
        tensor.getDimensions(),
        tensor.getType().getNativeCode(),
        tensor.getData());
  }

  @Override
  public List<NnTensor> computeNoInputs() throws NnException {
    ensureNotClosed();

    // Run inference
    nativeCompute(nativeHandle);

    // Probe outputs starting from index 0 until we get a null/error
    final List<NnTensor> outputs = new ArrayList<>();
    for (int i = 0; i < 1024; i++) {
      try {
        final byte[] serialized = nativeGetOutputByIndex(nativeHandle, i);
        if (serialized == null) {
          break;
        }
        outputs.add(NnTensor.deserializeFromNative(null, serialized));
      } catch (NnException e) {
        // No more outputs available
        break;
      }
    }
    return outputs;
  }

  @Override
  public NnTensor getOutput(final int index) throws NnException {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Output index cannot be negative: " + index);
    }
    ensureNotClosed();

    final byte[] serialized = nativeGetOutputByIndex(nativeHandle, index);
    if (serialized == null) {
      throw new NnException("Failed to get output tensor at index " + index);
    }
    return NnTensor.deserializeFromNative(null, serialized);
  }

  @Override
  public NnTensor getOutput(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureNotClosed();

    final byte[] serialized = nativeGetOutputByName(nativeHandle, name);
    if (serialized == null) {
      throw new NnException("Failed to get output tensor with name: " + name);
    }
    return NnTensor.deserializeFromNative(name, serialized);
  }

  @Override
  public int getInputCount() {
    // WASI-NN API does not expose input/output counts
    return -1;
  }

  @Override
  public int getOutputCount() {
    // WASI-NN API does not expose input/output counts
    return -1;
  }

  @Override
  public Map<String, NnTensorMetadata> getInputMetadata() {
    // WASI-NN API does not expose tensor metadata
    return Collections.emptyMap();
  }

  @Override
  public Map<String, NnTensorMetadata> getOutputMetadata() {
    // WASI-NN API does not expose tensor metadata
    return Collections.emptyMap();
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
  // Note: setInput/compute methods throw NnException from the native side

  private static native void nativeSetInputByIndex(
      long handle, int index, int[] dims, int tensorType, byte[] data) throws NnException;

  private static native void nativeSetInputByName(
      long handle, String name, int[] dims, int tensorType, byte[] data) throws NnException;

  private static native void nativeCompute(long handle) throws NnException;

  private static native byte[] nativeGetOutputByIndex(long handle, int index) throws NnException;

  private static native byte[] nativeGetOutputByName(long handle, String name) throws NnException;

  private static native void nativeClose(long handle);
}
