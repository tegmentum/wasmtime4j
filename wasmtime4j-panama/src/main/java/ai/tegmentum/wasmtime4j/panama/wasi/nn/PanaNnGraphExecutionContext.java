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
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensor;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensorType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of the NnGraphExecutionContext interface for WASI-NN inference.
 *
 * <p>This class provides execution context functionality for running ML inference operations using
 * Panama FFI bindings to the native Wasmtime WASI-NN implementation.
 *
 * @since 1.0.0
 */
public final class PanaNnGraphExecutionContext implements NnGraphExecutionContext {

  private static final Logger LOGGER =
      Logger.getLogger(PanaNnGraphExecutionContext.class.getName());
  private static final int MAX_DIMENSIONS = 16;
  private static final int MAX_OUTPUT_SIZE = 64 * 1024 * 1024; // 64MB max output

  private final MemorySegment nativeHandle;
  private final NnGraph graph;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama WASI-NN execution context.
   *
   * @param nativeHandle the native execution context handle
   * @param graph the parent graph
   * @throws IllegalArgumentException if nativeHandle is null or NULL
   */
  PanaNnGraphExecutionContext(final MemorySegment nativeHandle, final NnGraph graph) {
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native handle cannot be null");
    }
    this.nativeHandle = nativeHandle;
    this.graph = Objects.requireNonNull(graph, "graph cannot be null");
    this.resourceHandle =
        new NativeResourceHandle(
            "PanaNnGraphExecutionContext",
            () -> {
              LOGGER.log(
                  Level.FINE, "Closing PanaNnGraphExecutionContext with handle: {0}", nativeHandle);
              final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
              bindings.wasiNnExecClose(nativeHandle);
            });
    LOGGER.log(Level.FINE, "Created PanaNnGraphExecutionContext with handle: {0}", nativeHandle);
  }

  @Override
  public long getNativeHandle() {
    return nativeHandle.address();
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

    try (Arena arena = Arena.ofConfined()) {
      final int[] dimensions = tensor.getDimensions();
      final byte[] data = tensor.getData();

      final MemorySegment dimsSegment = arena.allocate(ValueLayout.JAVA_INT, dimensions.length);
      for (int i = 0; i < dimensions.length; i++) {
        dimsSegment.setAtIndex(ValueLayout.JAVA_INT, i, dimensions[i]);
      }

      final MemorySegment dataSegment = arena.allocate(data.length);
      dataSegment.copyFrom(MemorySegment.ofArray(data));

      final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
      final int result =
          bindings.wasiNnExecSetInput(
              nativeHandle,
              index,
              dimsSegment,
              dimensions.length,
              tensor.getType().ordinal(),
              dataSegment,
              data.length);

      if (result != 0) {
        throw new NnException("Failed to set input tensor at index " + index);
      }
    }
  }

  @Override
  public void setInput(final String name, final NnTensor tensor) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(tensor, "tensor cannot be null");
    ensureNotClosed();

    // The Panama FFI doesn't support named inputs directly, use index 0 as fallback
    // This is a limitation of the current implementation
    LOGGER.log(
        Level.FINE, "Setting input by name ''{0}'' - falling back to index-based input", name);
    setInput(0, tensor);
  }

  @Override
  public List<NnTensor> computeNoInputs() throws NnException {
    ensureNotClosed();

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    final int result = bindings.wasiNnExecCompute(nativeHandle);

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

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();

    // Get output size first
    final long outputSize = bindings.wasiNnExecGetOutputSize(nativeHandle, index);
    if (outputSize < 0) {
      throw new NnException("Failed to get output tensor size at index " + index);
    }
    if (outputSize > MAX_OUTPUT_SIZE) {
      throw new NnException(
          "Output tensor size exceeds maximum: " + outputSize + " > " + MAX_OUTPUT_SIZE);
    }

    try (Arena arena = Arena.ofConfined()) {
      // Get output data
      final MemorySegment dataSegment = arena.allocate((int) outputSize);
      final long actualLen =
          bindings.wasiNnExecGetOutput(nativeHandle, index, dataSegment, outputSize);

      if (actualLen < 0) {
        throw new NnException("Failed to get output tensor at index " + index);
      }

      final byte[] data = new byte[(int) actualLen];
      MemorySegment.copy(dataSegment, ValueLayout.JAVA_BYTE, 0, data, 0, (int) actualLen);

      // Get output dimensions
      final MemorySegment dimsSegment = arena.allocate(ValueLayout.JAVA_INT, MAX_DIMENSIONS);
      final int dimCount =
          bindings.wasiNnExecGetOutputDims(nativeHandle, index, dimsSegment, MAX_DIMENSIONS);

      int[] dimensions;
      if (dimCount > 0) {
        dimensions = new int[dimCount];
        for (int i = 0; i < dimCount; i++) {
          dimensions[i] = dimsSegment.getAtIndex(ValueLayout.JAVA_INT, i);
        }
      } else {
        // Default to 1D tensor if dimensions not available
        dimensions = new int[] {(int) actualLen};
      }

      // Get output type
      final int typeOrdinal = bindings.wasiNnExecGetOutputType(nativeHandle, index);
      final NnTensorType type =
          typeOrdinal >= 0 && typeOrdinal < NnTensorType.values().length
              ? NnTensorType.values()[typeOrdinal]
              : NnTensorType.FP32;

      return NnTensor.fromBytes(dimensions, type, data);
    }
  }

  @Override
  public NnTensor getOutput(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureNotClosed();

    // The Panama FFI doesn't support named outputs directly, use index 0 as fallback
    LOGGER.log(
        Level.FINE, "Getting output by name ''{0}'' - falling back to index-based output", name);
    return getOutput(0);
  }

  @Override
  public int getInputCount() {
    ensureNotClosed();
    // The Panama FFI doesn't expose input count, return 1 as default
    return 1;
  }

  @Override
  public int getOutputCount() {
    ensureNotClosed();
    // The Panama FFI doesn't expose output count, return 1 as default
    return 1;
  }

  @Override
  public Map<String, NnTensorMetadata> getInputMetadata() {
    ensureNotClosed();
    // The Panama FFI doesn't expose input metadata
    return new HashMap<>();
  }

  @Override
  public Map<String, NnTensorMetadata> getOutputMetadata() {
    ensureNotClosed();
    // The Panama FFI doesn't expose output metadata
    return new HashMap<>();
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
