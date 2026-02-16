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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for WASI-NN neural network inference operations.
 *
 * <p>Provides type-safe wrappers for all WASI-NN native functions including context lifecycle,
 * graph loading, execution context management, tensor input/output, and inference computation.
 *
 * <p>This class follows the singleton pattern with initialization-on-demand holder.
 */
public final class NativeWasiNnBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeWasiNnBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeWasiNnBindings INSTANCE = new NativeWasiNnBindings();
  }

  private NativeWasiNnBindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeWasiNnBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeWasiNnBindings getInstance() {
    return Holder.INSTANCE;
  }

  private void initializeBindings() {
    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_context_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_is_available", FunctionDescriptor.of(ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_get_default_target",
        FunctionDescriptor.of(ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_context_close", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_load_graph",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_load_graph_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_get_supported_encodings",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_get_supported_targets",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_is_encoding_supported",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_is_target_supported",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_graph_create_exec_context",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_graph_close", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_exec_set_input",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_exec_compute",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_exec_get_output",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_exec_get_output_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_exec_get_output_dims",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_exec_get_output_type",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_nn_exec_close", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  // =============================================================================
  // WASI-NN Context Operations
  // =============================================================================

  /**
   * Creates a new WASI-NN context.
   *
   * @return pointer to the WASI-NN context, or null on failure
   */
  public MemorySegment wasiNnContextCreate() {
    return callNativeFunction("wasmtime4j_panama_wasi_nn_context_create", MemorySegment.class);
  }

  /**
   * Checks if WASI-NN is available in this build.
   *
   * @return 1 if available, 0 if not
   */
  public int wasiNnIsAvailable() {
    return callNativeFunction("wasmtime4j_panama_wasi_nn_is_available", Integer.class);
  }

  /**
   * Gets the default execution target.
   *
   * @return the default target ordinal (0 = CPU)
   */
  public int wasiNnGetDefaultTarget() {
    return callNativeFunction("wasmtime4j_panama_wasi_nn_get_default_target", Integer.class);
  }

  /**
   * Closes a WASI-NN context.
   *
   * @param contextHandle the WASI-NN context handle
   */
  public void wasiNnContextClose(final MemorySegment contextHandle) {
    if (contextHandle != null && !contextHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_nn_context_close", Void.class, contextHandle);
    }
  }

  // =============================================================================
  // WASI-NN Graph Operations
  // =============================================================================

  /**
   * Loads a graph from model data.
   *
   * @param contextHandle the WASI-NN context handle
   * @param dataPtr pointer to the model data
   * @param dataLen length of the model data
   * @param encodingOrdinal the graph encoding format ordinal
   * @param targetOrdinal the execution target ordinal
   * @return pointer to the graph, or null on error
   */
  public MemorySegment wasiNnLoadGraph(
      final MemorySegment contextHandle,
      final MemorySegment dataPtr,
      final long dataLen,
      final int encodingOrdinal,
      final int targetOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_load_graph",
        MemorySegment.class,
        contextHandle,
        dataPtr,
        dataLen,
        encodingOrdinal,
        targetOrdinal);
  }

  /**
   * Loads a graph by name.
   *
   * @param contextHandle the WASI-NN context handle
   * @param namePtr pointer to the null-terminated model name string
   * @param targetOrdinal the execution target ordinal
   * @return pointer to the graph, or null on error
   */
  public MemorySegment wasiNnLoadGraphByName(
      final MemorySegment contextHandle, final MemorySegment namePtr, final int targetOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(namePtr, "namePtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_load_graph_by_name",
        MemorySegment.class,
        contextHandle,
        namePtr,
        targetOrdinal);
  }

  /**
   * Gets supported encodings.
   *
   * @param contextHandle the WASI-NN context handle
   * @param outEncodings pointer to array to receive encoding ordinals
   * @param maxCount maximum number of encodings to return
   * @return number of encodings written, or -1 on error
   */
  public int wasiNnGetSupportedEncodings(
      final MemorySegment contextHandle, final MemorySegment outEncodings, final int maxCount) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_get_supported_encodings",
        Integer.class,
        contextHandle,
        outEncodings,
        maxCount);
  }

  /**
   * Gets supported targets.
   *
   * @param contextHandle the WASI-NN context handle
   * @param outTargets pointer to array to receive target ordinals
   * @param maxCount maximum number of targets to return
   * @return number of targets written, or -1 on error
   */
  public int wasiNnGetSupportedTargets(
      final MemorySegment contextHandle, final MemorySegment outTargets, final int maxCount) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_get_supported_targets",
        Integer.class,
        contextHandle,
        outTargets,
        maxCount);
  }

  /**
   * Checks if an encoding is supported.
   *
   * @param contextHandle the WASI-NN context handle
   * @param encodingOrdinal the encoding ordinal to check
   * @return 1 if supported, 0 if not
   */
  public int wasiNnIsEncodingSupported(
      final MemorySegment contextHandle, final int encodingOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_is_encoding_supported",
        Integer.class,
        contextHandle,
        encodingOrdinal);
  }

  /**
   * Checks if a target is supported.
   *
   * @param contextHandle the WASI-NN context handle
   * @param targetOrdinal the target ordinal to check
   * @return 1 if supported, 0 if not
   */
  public int wasiNnIsTargetSupported(final MemorySegment contextHandle, final int targetOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_is_target_supported",
        Integer.class,
        contextHandle,
        targetOrdinal);
  }

  /**
   * Closes a graph.
   *
   * @param graphHandle the graph handle
   */
  public void wasiNnGraphClose(final MemorySegment graphHandle) {
    if (graphHandle != null && !graphHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_nn_graph_close", Void.class, graphHandle);
    }
  }

  // =============================================================================
  // WASI-NN Execution Context Operations
  // =============================================================================

  /**
   * Creates an execution context from a graph.
   *
   * @param graphHandle the graph handle
   * @return pointer to the execution context, or null on error
   */
  public MemorySegment wasiNnGraphCreateExecContext(final MemorySegment graphHandle) {
    validatePointer(graphHandle, "graphHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_graph_create_exec_context", MemorySegment.class, graphHandle);
  }

  /**
   * Sets an input tensor by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the input index
   * @param dimsPtr pointer to dimensions array
   * @param dimsLen number of dimensions
   * @param typeOrdinal the tensor type ordinal
   * @param dataPtr pointer to tensor data
   * @param dataLen length of tensor data
   * @return 0 on success, -1 on error
   */
  public int wasiNnExecSetInput(
      final MemorySegment ctxHandle,
      final int index,
      final MemorySegment dimsPtr,
      final int dimsLen,
      final int typeOrdinal,
      final MemorySegment dataPtr,
      final long dataLen) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_set_input",
        Integer.class,
        ctxHandle,
        index,
        dimsPtr,
        dimsLen,
        typeOrdinal,
        dataPtr,
        dataLen);
  }

  /**
   * Runs inference.
   *
   * @param ctxHandle the execution context handle
   * @return 0 on success, -1 on error
   */
  public int wasiNnExecCompute(final MemorySegment ctxHandle) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction("wasmtime4j_panama_wasi_nn_exec_compute", Integer.class, ctxHandle);
  }

  /**
   * Gets output tensor data by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @param outData pointer to buffer to receive data
   * @param maxLen maximum buffer length
   * @return actual data length, or -1 on error
   */
  public long wasiNnExecGetOutput(
      final MemorySegment ctxHandle,
      final int index,
      final MemorySegment outData,
      final long maxLen) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output", Long.class, ctxHandle, index, outData, maxLen);
  }

  /**
   * Gets output tensor size by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @return the output size, or -1 on error
   */
  public long wasiNnExecGetOutputSize(final MemorySegment ctxHandle, final int index) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output_size", Long.class, ctxHandle, index);
  }

  /**
   * Gets output tensor dimensions by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @param outDims pointer to buffer to receive dimensions
   * @param maxDims maximum number of dimensions
   * @return number of dimensions, or -1 on error
   */
  public int wasiNnExecGetOutputDims(
      final MemorySegment ctxHandle,
      final int index,
      final MemorySegment outDims,
      final int maxDims) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output_dims",
        Integer.class,
        ctxHandle,
        index,
        outDims,
        maxDims);
  }

  /**
   * Gets output tensor type by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @return the tensor type ordinal, or -1 on error
   */
  public int wasiNnExecGetOutputType(final MemorySegment ctxHandle, final int index) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output_type", Integer.class, ctxHandle, index);
  }

  /**
   * Closes an execution context.
   *
   * @param ctxHandle the execution context handle
   */
  public void wasiNnExecClose(final MemorySegment ctxHandle) {
    if (ctxHandle != null && !ctxHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_nn_exec_close", Void.class, ctxHandle);
    }
  }
}
