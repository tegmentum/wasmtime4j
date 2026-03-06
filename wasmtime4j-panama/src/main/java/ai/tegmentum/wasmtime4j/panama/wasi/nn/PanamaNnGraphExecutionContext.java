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
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensor;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama implementation of the NnGraphExecutionContext interface for WASI-NN inference.
 *
 * <p>Output tensors are deserialized from the same format as JNI: {@code
 * [num_dims:i32LE][dim0:i32LE]...[dimN:i32LE][tensor_type:i32LE][data bytes]}.
 *
 * @since 1.0.0
 */
public final class PanamaNnGraphExecutionContext implements NnGraphExecutionContext {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaNnGraphExecutionContext.class.getName());

  private final MemorySegment nativePtr;
  private final NativeResourceHandle resourceHandle;
  private final NnGraph graph;

  /**
   * Creates a new Panama WASI-NN execution context.
   *
   * @param nativePtr the native execution context pointer
   * @param graph the parent graph
   */
  PanamaNnGraphExecutionContext(final MemorySegment nativePtr, final NnGraph graph) {
    if (nativePtr == null || nativePtr.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native pointer cannot be null");
    }
    this.nativePtr = nativePtr;
    this.graph = Objects.requireNonNull(graph, "graph cannot be null");

    final MemorySegment ptrForCleanup = nativePtr;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaNnGraphExecutionContext",
            () -> NativeWasiNnBindings.getInstance().nnExecClose(ptrForCleanup));
  }

  @Override
  public long getNativeHandle() {
    resourceHandle.ensureNotClosed();
    return nativePtr.address();
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
    resourceHandle.ensureNotClosed();

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
    resourceHandle.ensureNotClosed();

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
    resourceHandle.ensureNotClosed();

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    try (Arena arena = Arena.ofConfined()) {
      bindings.nnExecSetInputByIndex(
          arena,
          nativePtr,
          index,
          tensor.getDimensions(),
          tensor.getType().getNativeCode(),
          tensor.getData());
    }
  }

  @Override
  public void setInput(final String name, final NnTensor tensor) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(tensor, "tensor cannot be null");
    resourceHandle.ensureNotClosed();

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    try (Arena arena = Arena.ofConfined()) {
      bindings.nnExecSetInputByName(
          arena,
          nativePtr,
          name,
          tensor.getDimensions(),
          tensor.getType().getNativeCode(),
          tensor.getData());
    }
  }

  @Override
  public List<NnTensor> computeNoInputs() throws NnException {
    resourceHandle.ensureNotClosed();

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    try (Arena arena = Arena.ofConfined()) {
      bindings.nnExecCompute(arena, nativePtr);
    }

    // Probe outputs starting from index 0
    final List<NnTensor> outputs = new ArrayList<>();
    for (int i = 0; i < 1024; i++) {
      try (Arena arena = Arena.ofConfined()) {
        final byte[] serialized = bindings.nnExecGetOutputByIndex(arena, nativePtr, i);
        if (serialized == null) {
          break;
        }
        outputs.add(NnTensor.deserializeFromNative(null, serialized));
      } catch (NnException e) {
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
    resourceHandle.ensureNotClosed();

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    try (Arena arena = Arena.ofConfined()) {
      final byte[] serialized = bindings.nnExecGetOutputByIndex(arena, nativePtr, index);
      if (serialized == null) {
        throw new NnException("Failed to get output tensor at index " + index);
      }
      return NnTensor.deserializeFromNative(null, serialized);
    }
  }

  @Override
  public NnTensor getOutput(final String name) throws NnException {
    Objects.requireNonNull(name, "name cannot be null");
    resourceHandle.ensureNotClosed();

    final NativeWasiNnBindings bindings = NativeWasiNnBindings.getInstance();
    try (Arena arena = Arena.ofConfined()) {
      final byte[] serialized = bindings.nnExecGetOutputByName(arena, nativePtr, name);
      if (serialized == null) {
        throw new NnException("Failed to get output tensor with name: " + name);
      }
      return NnTensor.deserializeFromNative(name, serialized);
    }
  }

  @Override
  public int getInputCount() {
    return -1;
  }

  @Override
  public int getOutputCount() {
    return -1;
  }

  @Override
  public Map<String, NnTensorMetadata> getInputMetadata() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, NnTensorMetadata> getOutputMetadata() {
    return Collections.emptyMap();
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }
}
