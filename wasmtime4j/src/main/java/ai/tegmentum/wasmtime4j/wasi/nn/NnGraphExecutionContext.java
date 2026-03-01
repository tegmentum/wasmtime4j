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
package ai.tegmentum.wasmtime4j.wasi.nn;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * Execution context for running inference on a loaded WASI-NN graph.
 *
 * <p>An execution context maintains the state needed to run inference operations on a loaded model.
 * It holds input and output tensor buffers and manages the inference lifecycle.
 *
 * <p>Execution contexts are created from {@link NnGraph} instances and must be closed when no
 * longer needed.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (NnGraphExecutionContext exec = graph.createExecutionContext()) {
 *     // Prepare input tensor
 *     NnTensor input = NnTensor.fromFloatArray("input", new int[]{1, 224, 224, 3}, imageData);
 *
 *     // Run inference with named tensors
 *     List<NnTensor> outputs = exec.compute(List.of(input));
 *
 *     // Process outputs
 *     float[] predictions = outputs.get(0).toFloatArray();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface NnGraphExecutionContext extends AutoCloseable {

  /**
   * Gets the native handle for this execution context.
   *
   * <p>This is an implementation detail used for FFI calls.
   *
   * @return the native handle
   */
  long getNativeHandle();

  /**
   * Gets the graph this execution context was created from.
   *
   * @return the parent graph
   */
  NnGraph getGraph();

  /**
   * Runs inference with the given named input tensors.
   *
   * <p>This method executes the neural network with the provided inputs and returns the output
   * tensors. Each input tensor should have a name that matches an input in the model.
   *
   * @param inputs the named input tensors
   * @return the output tensors
   * @throws NnException if inference fails
   * @throws NullPointerException if inputs is null
   * @throws IllegalArgumentException if inputs is empty or contains null tensors
   */
  List<NnTensor> compute(List<NnTensor> inputs) throws NnException;

  /**
   * Runs inference with input tensors specified by index.
   *
   * <p>This method executes the neural network using positional input tensors. The first tensor
   * goes to input index 0, the second to index 1, and so on.
   *
   * @param inputs the input tensors in order
   * @return the output tensors
   * @throws NnException if inference fails
   * @throws NullPointerException if inputs is null
   * @throws IllegalArgumentException if inputs is empty or contains null tensors
   */
  List<NnTensor> computeByIndex(NnTensor... inputs) throws NnException;

  /**
   * Sets an input tensor by index.
   *
   * <p>This method allows setting individual inputs before calling {@link #computeNoInputs()}.
   *
   * @param index the input index
   * @param tensor the input tensor
   * @throws NnException if setting the input fails
   * @throws NullPointerException if tensor is null
   * @throws IndexOutOfBoundsException if index is out of range
   */
  void setInput(int index, NnTensor tensor) throws NnException;

  /**
   * Sets an input tensor by name.
   *
   * <p>This method allows setting individual inputs before calling {@link #computeNoInputs()}.
   *
   * @param name the input name
   * @param tensor the input tensor
   * @throws NnException if setting the input fails
   * @throws NullPointerException if name or tensor is null
   * @throws IllegalArgumentException if the name is not a valid input
   */
  void setInput(String name, NnTensor tensor) throws NnException;

  /**
   * Runs inference using inputs previously set with {@link #setInput}.
   *
   * @return the output tensors
   * @throws NnException if inference fails
   * @throws IllegalStateException if not all inputs have been set
   */
  List<NnTensor> computeNoInputs() throws NnException;

  /**
   * Gets an output tensor by index after inference.
   *
   * @param index the output index
   * @return the output tensor
   * @throws NnException if getting the output fails
   * @throws IndexOutOfBoundsException if index is out of range
   * @throws IllegalStateException if inference has not been run
   */
  NnTensor getOutput(int index) throws NnException;

  /**
   * Gets an output tensor by name after inference.
   *
   * @param name the output name
   * @return the output tensor
   * @throws NnException if getting the output fails
   * @throws NullPointerException if name is null
   * @throws IllegalArgumentException if the name is not a valid output
   * @throws IllegalStateException if inference has not been run
   */
  NnTensor getOutput(String name) throws NnException;

  /**
   * Gets the number of inputs expected by the model.
   *
   * @return the input count
   */
  int getInputCount();

  /**
   * Gets the number of outputs produced by the model.
   *
   * @return the output count
   */
  int getOutputCount();

  /**
   * Gets metadata about the model inputs.
   *
   * @return a map of input names to their expected dimensions and types
   */
  Map<String, NnTensorMetadata> getInputMetadata();

  /**
   * Gets metadata about the model outputs.
   *
   * @return a map of output names to their dimensions and types
   */
  Map<String, NnTensorMetadata> getOutputMetadata();

  /**
   * Checks if this execution context is still valid (not closed).
   *
   * @return true if the context is valid
   */
  boolean isValid();

  /**
   * Closes this execution context and releases native resources.
   *
   * <p>After closing, the context cannot be used for inference operations.
   */
  @Override
  void close();

  /** Metadata about a tensor (input or output). */
  final class NnTensorMetadata {

    private final String name;
    private final int[] dimensions;
    private final NnTensorType type;

    /**
     * Creates a new NnTensorMetadata.
     *
     * @param name the tensor name
     * @param dimensions the expected dimensions (null means dynamic)
     * @param type the expected data type (null means any)
     */
    public NnTensorMetadata(final String name, final int[] dimensions, final NnTensorType type) {
      this.name = name;
      this.dimensions = dimensions != null ? dimensions.clone() : null;
      this.type = type;
    }

    /**
     * Gets the tensor name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the expected dimensions.
     *
     * @return the dimensions (null means dynamic)
     */
    @SuppressFBWarnings(
        value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS",
        justification = "Null indicates dynamic dimensions vs empty array for scalar tensor")
    public int[] getDimensions() {
      return dimensions != null ? dimensions.clone() : null;
    }

    /**
     * Gets the expected data type.
     *
     * @return the type (null means any)
     */
    public NnTensorType getType() {
      return type;
    }

    /**
     * Checks if the dimensions are fixed.
     *
     * @return true if dimensions are specified
     */
    public boolean hasFixedDimensions() {
      return dimensions != null;
    }

    /**
     * Checks if the type is specified.
     *
     * @return true if type is specified
     */
    public boolean hasFixedType() {
      return type != null;
    }
  }
}
