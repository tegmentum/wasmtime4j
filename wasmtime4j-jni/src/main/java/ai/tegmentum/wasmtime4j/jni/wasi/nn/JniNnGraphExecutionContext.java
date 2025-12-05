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
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensorType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * JNI implementation of the NnGraphExecutionContext interface for WASI-NN inference.
 *
 * <p>This class provides execution context functionality for running ML inference operations using
 * JNI bindings to the native Wasmtime WASI-NN implementation.
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
   * @throws IllegalArgumentException if nativeHandle is 0
   */
  JniNnGraphExecutionContext(final long nativeHandle, final NnGraph graph) {
    super(nativeHandle);
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be 0");
    }
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

    // Run inference and get outputs
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

    // Set each input tensor by index
    for (int i = 0; i < inputs.length; i++) {
      setInput(i, inputs[i]);
    }

    // Run inference and get outputs
    return computeNoInputs();
  }

  @Override
  public void setInput(final int index, final NnTensor tensor) throws NnException {
    Objects.requireNonNull(tensor, "tensor cannot be null");
    if (index < 0) {
      throw new IndexOutOfBoundsException("Input index cannot be negative: " + index);
    }
    ensureNotClosed();

    final int result =
        nativeSetInputByIndex(
            nativeHandle,
            index,
            tensor.getDimensions(),
            tensor.getType().ordinal(),
            tensor.getData());
    if (result != 0) {
      throw new NnException("Failed to set input tensor at index " + index);
    }
  }

  @Override
  public void setInput(final String name, final NnTensor tensor) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(tensor, "tensor cannot be null");
    ensureNotClosed();

    final int result =
        nativeSetInputByName(
            nativeHandle,
            name,
            tensor.getDimensions(),
            tensor.getType().ordinal(),
            tensor.getData());
    if (result != 0) {
      throw new NnException("Failed to set input tensor with name: " + name);
    }
  }

  @Override
  public List<NnTensor> computeNoInputs() throws NnException {
    ensureNotClosed();

    final int result = nativeCompute(nativeHandle);
    if (result != 0) {
      throw new NnException("Failed to run inference");
    }

    // Get all outputs
    final int outputCount = getOutputCount();
    final List<NnTensor> outputs = new ArrayList<>(outputCount);
    for (int i = 0; i < outputCount; i++) {
      outputs.add(getOutput(i));
    }
    return outputs;
  }

  @Override
  public NnTensor getOutput(final int index) throws NnException {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Output index cannot be negative: " + index);
    }
    ensureNotClosed();

    // Get output data
    final byte[] data = nativeGetOutputByIndex(nativeHandle, index);
    if (data == null) {
      throw new NnException("Failed to get output tensor at index " + index);
    }

    // Get output metadata
    final int[] dimensions = nativeGetOutputDimensions(nativeHandle, index);
    final int typeOrdinal = nativeGetOutputType(nativeHandle, index);
    final NnTensorType type =
        typeOrdinal >= 0 && typeOrdinal < NnTensorType.values().length
            ? NnTensorType.values()[typeOrdinal]
            : NnTensorType.FP32;

    return NnTensor.fromBytes(dimensions, type, data);
  }

  @Override
  public NnTensor getOutput(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureNotClosed();

    // Get output data by name
    final byte[] data = nativeGetOutputByName(nativeHandle, name);
    if (data == null) {
      throw new NnException("Failed to get output tensor with name: " + name);
    }

    // Get output metadata by name
    final int[] dimensions = nativeGetOutputDimensionsByName(nativeHandle, name);
    final int typeOrdinal = nativeGetOutputTypeByName(nativeHandle, name);
    final NnTensorType type =
        typeOrdinal >= 0 && typeOrdinal < NnTensorType.values().length
            ? NnTensorType.values()[typeOrdinal]
            : NnTensorType.FP32;

    return NnTensor.fromBytes(name, dimensions, type, data);
  }

  @Override
  public int getInputCount() {
    ensureNotClosed();
    return nativeGetInputCount(nativeHandle);
  }

  @Override
  public int getOutputCount() {
    ensureNotClosed();
    return nativeGetOutputCount(nativeHandle);
  }

  @Override
  public Map<String, NnTensorMetadata> getInputMetadata() {
    ensureNotClosed();

    final String[] names = nativeGetInputNames(nativeHandle);
    final Map<String, NnTensorMetadata> metadata = new HashMap<>();

    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        final String name = names[i];
        final int[] dimensions = nativeGetInputDimensionsByIndex(nativeHandle, i);
        final int typeOrdinal = nativeGetInputTypeByIndex(nativeHandle, i);
        final NnTensorType type =
            typeOrdinal >= 0 && typeOrdinal < NnTensorType.values().length
                ? NnTensorType.values()[typeOrdinal]
                : null;
        metadata.put(name, new NnTensorMetadata(name, dimensions, type));
      }
    }

    return metadata;
  }

  @Override
  public Map<String, NnTensorMetadata> getOutputMetadata() {
    ensureNotClosed();

    final String[] names = nativeGetOutputNames(nativeHandle);
    final Map<String, NnTensorMetadata> metadata = new HashMap<>();

    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        final String name = names[i];
        final int[] dimensions = nativeGetOutputDimensions(nativeHandle, i);
        final int typeOrdinal = nativeGetOutputType(nativeHandle, i);
        final NnTensorType type =
            typeOrdinal >= 0 && typeOrdinal < NnTensorType.values().length
                ? NnTensorType.values()[typeOrdinal]
                : null;
        metadata.put(name, new NnTensorMetadata(name, dimensions, type));
      }
    }

    return metadata;
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

  private static native int nativeSetInputByIndex(
      long contextHandle, int index, int[] dimensions, int typeOrdinal, byte[] data);

  private static native int nativeSetInputByName(
      long contextHandle, String name, int[] dimensions, int typeOrdinal, byte[] data);

  private static native int nativeCompute(long contextHandle);

  private static native byte[] nativeGetOutputByIndex(long contextHandle, int index);

  private static native byte[] nativeGetOutputByName(long contextHandle, String name);

  private static native int[] nativeGetOutputDimensions(long contextHandle, int index);

  private static native int[] nativeGetOutputDimensionsByName(long contextHandle, String name);

  private static native int nativeGetOutputType(long contextHandle, int index);

  private static native int nativeGetOutputTypeByName(long contextHandle, String name);

  private static native int nativeGetInputCount(long contextHandle);

  private static native int nativeGetOutputCount(long contextHandle);

  private static native String[] nativeGetInputNames(long contextHandle);

  private static native String[] nativeGetOutputNames(long contextHandle);

  private static native int[] nativeGetInputDimensionsByIndex(long contextHandle, int index);

  private static native int nativeGetInputTypeByIndex(long contextHandle, int index);

  private static native void nativeClose(long contextHandle);
}
