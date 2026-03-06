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

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI bindings for WASI-NN native functions.
 *
 * <p>This class provides method handles for calling the C-exported WASI-NN functions from the
 * native library. It uses the singleton pattern for efficient handle caching.
 *
 * @since 1.0.0
 */
public final class NativeWasiNnBindings {

  private static final Logger LOGGER = Logger.getLogger(NativeWasiNnBindings.class.getName());
  private static final int ERROR_BUF_SIZE = 1024;

  private static volatile NativeWasiNnBindings instance;

  // Method handles for all WASI-NN native functions
  private final MethodHandle contextCreate;
  private final MethodHandle contextIsAvailable;
  private final MethodHandle contextSupportedEncodings;
  private final MethodHandle contextLoadGraph;
  private final MethodHandle contextGetBackendInfo;
  private final MethodHandle contextClose;
  private final MethodHandle graphGetEncoding;
  private final MethodHandle graphGetTarget;
  private final MethodHandle graphCreateExecCtx;
  private final MethodHandle graphClose;
  private final MethodHandle execSetInputByIndex;
  private final MethodHandle execSetInputByName;
  private final MethodHandle execCompute;
  private final MethodHandle execGetOutputByIndex;
  private final MethodHandle execGetOutputByName;
  private final MethodHandle execClose;
  private final MethodHandle freeBuffer;
  private final MethodHandle freeString;

  private NativeWasiNnBindings() {
    final SymbolLookup lookup = NativeLibraryLoader.getInstance().getSymbolLookup();
    final Linker linker = Linker.nativeLinker();

    // Context functions
    contextCreate =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_context_create").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS));

    contextIsAvailable =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_context_is_available").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    contextSupportedEncodings =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_context_supported_encodings").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS));

    contextLoadGraph =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_context_load_graph").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

    contextGetBackendInfo =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_context_get_backend_info").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    contextClose =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_context_close").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Graph functions
    graphGetEncoding =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_graph_get_encoding").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    graphGetTarget =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_graph_get_target").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    graphCreateExecCtx =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_graph_create_exec_ctx").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

    graphClose =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_graph_close").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Exec context functions
    execSetInputByIndex =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_exec_set_input_by_index").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

    execSetInputByName =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_exec_set_input_by_name").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

    execCompute =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_exec_compute").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

    execGetOutputByIndex =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_exec_get_output_by_index").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

    execGetOutputByName =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_exec_get_output_by_name").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

    execClose =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_exec_close").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Memory management
    freeBuffer =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_free_buffer").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    freeString =
        linker.downcallHandle(
            lookup.find("wasmtime4j_nn_free_string").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    LOGGER.log(Level.FINE, "NativeWasiNnBindings initialized");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the bindings instance
   */
  public static NativeWasiNnBindings getInstance() {
    if (instance == null) {
      synchronized (NativeWasiNnBindings.class) {
        if (instance == null) {
          instance = new NativeWasiNnBindings();
        }
      }
    }
    return instance;
  }

  // ===== Context operations =====

  /**
   * Creates a new NnContext.
   *
   * @return the context pointer
   * @throws NnException if creation fails
   */
  public MemorySegment nnContextCreate() throws NnException {
    try {
      return (MemorySegment) contextCreate.invokeExact();
    } catch (Throwable t) {
      throw new NnException("Failed to create NN context: " + t.getMessage(), t);
    }
  }

  /**
   * Checks if any backends are available.
   *
   * @param ctxPtr the context pointer
   * @return 1 if available, 0 otherwise
   */
  public int nnContextIsAvailable(final MemorySegment ctxPtr) {
    try {
      return (int) contextIsAvailable.invokeExact(ctxPtr);
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Failed to check NN availability", t);
      return 0;
    }
  }

  /**
   * Gets supported encoding codes.
   *
   * @param arena the arena for allocations
   * @param ctxPtr the context pointer
   * @return array of encoding codes
   */
  public int[] nnContextSupportedEncodings(final Arena arena, final MemorySegment ctxPtr) {
    try {
      final MemorySegment outBuf = arena.allocate(ValueLayout.JAVA_INT, 16);
      final MemorySegment outCount = arena.allocate(ValueLayout.JAVA_INT);
      final int result = (int) contextSupportedEncodings.invokeExact(ctxPtr, outBuf, 16, outCount);
      if (result != 0) {
        return new int[0];
      }
      final int count = outCount.get(ValueLayout.JAVA_INT, 0);
      final int[] codes = new int[count];
      for (int i = 0; i < count; i++) {
        codes[i] = outBuf.getAtIndex(ValueLayout.JAVA_INT, i);
      }
      return codes;
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Failed to get supported encodings", t);
      return new int[0];
    }
  }

  /**
   * Loads a graph.
   *
   * @param arena the arena for allocations
   * @param ctxPtr the context pointer
   * @param parts the model data parts
   * @param encodingCode the encoding code
   * @param targetCode the target code
   * @return the graph pointer
   * @throws NnException if loading fails
   */
  public MemorySegment nnContextLoadGraph(
      final Arena arena,
      final MemorySegment ctxPtr,
      final byte[][] parts,
      final int encodingCode,
      final int targetCode)
      throws NnException {
    try {
      // Allocate arrays of pointers and lengths
      final MemorySegment ptrsArray = arena.allocate(ValueLayout.ADDRESS, parts.length);
      final MemorySegment lensArray = arena.allocate(ValueLayout.JAVA_INT, parts.length);

      for (int i = 0; i < parts.length; i++) {
        final MemorySegment partData = arena.allocate(parts[i].length);
        partData.copyFrom(MemorySegment.ofArray(parts[i]));
        ptrsArray.setAtIndex(ValueLayout.ADDRESS, i, partData);
        lensArray.setAtIndex(ValueLayout.JAVA_INT, i, parts[i].length);
      }

      final MemorySegment errorBuf = arena.allocate(ERROR_BUF_SIZE);

      final MemorySegment graphPtr =
          (MemorySegment)
              contextLoadGraph.invokeExact(
                  ctxPtr,
                  ptrsArray,
                  lensArray,
                  parts.length,
                  encodingCode,
                  targetCode,
                  errorBuf,
                  ERROR_BUF_SIZE);

      if (graphPtr.equals(MemorySegment.NULL)) {
        final String error = errorBuf.getString(0, StandardCharsets.UTF_8);
        throw new NnException("Failed to load graph: " + error);
      }
      return graphPtr;
    } catch (NnException e) {
      throw e;
    } catch (Throwable t) {
      throw new NnException("Failed to load graph: " + t.getMessage(), t);
    }
  }

  /**
   * Gets backend info as JSON string.
   *
   * @param ctxPtr the context pointer
   * @return the JSON info string, or null
   */
  public String nnContextGetBackendInfo(final MemorySegment ctxPtr) {
    try {
      final MemorySegment strPtr = (MemorySegment) contextGetBackendInfo.invokeExact(ctxPtr);
      if (strPtr.equals(MemorySegment.NULL)) {
        return null;
      }
      // Read the C string
      final MemorySegment reinterpreted = strPtr.reinterpret(ERROR_BUF_SIZE);
      final String result = reinterpreted.getString(0, StandardCharsets.UTF_8);
      // Free the native string
      freeString.invokeExact(strPtr);
      return result;
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Failed to get backend info", t);
      return null;
    }
  }

  /**
   * Closes an NnContext.
   *
   * @param ctxPtr the context pointer
   */
  public void nnContextClose(final MemorySegment ctxPtr) {
    try {
      contextClose.invokeExact(ctxPtr);
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Failed to close NN context", t);
    }
  }

  // ===== Graph operations =====

  /**
   * Creates an execution context from a graph.
   *
   * @param arena the arena for allocations
   * @param graphPtr the graph pointer
   * @return the execution context pointer
   * @throws NnException if creation fails
   */
  public MemorySegment nnGraphCreateExecCtx(final Arena arena, final MemorySegment graphPtr)
      throws NnException {
    try {
      final MemorySegment errorBuf = arena.allocate(ERROR_BUF_SIZE);
      final MemorySegment execPtr =
          (MemorySegment) graphCreateExecCtx.invokeExact(graphPtr, errorBuf, ERROR_BUF_SIZE);
      if (execPtr.equals(MemorySegment.NULL)) {
        final String error = errorBuf.getString(0, StandardCharsets.UTF_8);
        throw new NnException("Failed to create execution context: " + error);
      }
      return execPtr;
    } catch (NnException e) {
      throw e;
    } catch (Throwable t) {
      throw new NnException("Failed to create execution context: " + t.getMessage(), t);
    }
  }

  /**
   * Closes an NnGraph.
   *
   * @param graphPtr the graph pointer
   */
  public void nnGraphClose(final MemorySegment graphPtr) {
    try {
      graphClose.invokeExact(graphPtr);
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Failed to close NN graph", t);
    }
  }

  // ===== Execution context operations =====

  /**
   * Sets an input tensor by index.
   *
   * @param arena the arena for allocations
   * @param execPtr the execution context pointer
   * @param index the input index
   * @param dims the tensor dimensions
   * @param tensorType the tensor type code
   * @param data the tensor data
   * @throws NnException if setting fails
   */
  public void nnExecSetInputByIndex(
      final Arena arena,
      final MemorySegment execPtr,
      final int index,
      final int[] dims,
      final int tensorType,
      final byte[] data)
      throws NnException {
    try {
      final MemorySegment dimsSegment = arena.allocate(ValueLayout.JAVA_INT, dims.length);
      for (int i = 0; i < dims.length; i++) {
        dimsSegment.setAtIndex(ValueLayout.JAVA_INT, i, dims[i]);
      }

      final MemorySegment dataSegment = arena.allocate(data.length);
      dataSegment.copyFrom(MemorySegment.ofArray(data));

      final MemorySegment errorBuf = arena.allocate(ERROR_BUF_SIZE);

      final int result =
          (int)
              execSetInputByIndex.invokeExact(
                  execPtr,
                  index,
                  dimsSegment,
                  dims.length,
                  tensorType,
                  dataSegment,
                  data.length,
                  errorBuf,
                  ERROR_BUF_SIZE);

      if (result != 0) {
        final String error = errorBuf.getString(0, StandardCharsets.UTF_8);
        throw new NnException("Failed to set input at index " + index + ": " + error);
      }
    } catch (NnException e) {
      throw e;
    } catch (Throwable t) {
      throw new NnException("Failed to set input at index " + index + ": " + t.getMessage(), t);
    }
  }

  /**
   * Sets an input tensor by name.
   *
   * @param arena the arena for allocations
   * @param execPtr the execution context pointer
   * @param name the input name
   * @param dims the tensor dimensions
   * @param tensorType the tensor type code
   * @param data the tensor data
   * @throws NnException if setting fails
   */
  public void nnExecSetInputByName(
      final Arena arena,
      final MemorySegment execPtr,
      final String name,
      final int[] dims,
      final int tensorType,
      final byte[] data)
      throws NnException {
    try {
      final MemorySegment nameSegment = arena.allocateFrom(name, StandardCharsets.UTF_8);

      final MemorySegment dimsSegment = arena.allocate(ValueLayout.JAVA_INT, dims.length);
      for (int i = 0; i < dims.length; i++) {
        dimsSegment.setAtIndex(ValueLayout.JAVA_INT, i, dims[i]);
      }

      final MemorySegment dataSegment = arena.allocate(data.length);
      dataSegment.copyFrom(MemorySegment.ofArray(data));

      final MemorySegment errorBuf = arena.allocate(ERROR_BUF_SIZE);

      final int result =
          (int)
              execSetInputByName.invokeExact(
                  execPtr,
                  nameSegment,
                  dimsSegment,
                  dims.length,
                  tensorType,
                  dataSegment,
                  data.length,
                  errorBuf,
                  ERROR_BUF_SIZE);

      if (result != 0) {
        final String error = errorBuf.getString(0, StandardCharsets.UTF_8);
        throw new NnException("Failed to set input '" + name + "': " + error);
      }
    } catch (NnException e) {
      throw e;
    } catch (Throwable t) {
      throw new NnException("Failed to set input '" + name + "': " + t.getMessage(), t);
    }
  }

  /**
   * Runs inference.
   *
   * @param arena the arena for allocations
   * @param execPtr the execution context pointer
   * @throws NnException if inference fails
   */
  public void nnExecCompute(final Arena arena, final MemorySegment execPtr) throws NnException {
    try {
      final MemorySegment errorBuf = arena.allocate(ERROR_BUF_SIZE);
      final int result = (int) execCompute.invokeExact(execPtr, errorBuf, ERROR_BUF_SIZE);
      if (result != 0) {
        final String error = errorBuf.getString(0, StandardCharsets.UTF_8);
        throw new NnException("Inference failed: " + error);
      }
    } catch (NnException e) {
      throw e;
    } catch (Throwable t) {
      throw new NnException("Inference failed: " + t.getMessage(), t);
    }
  }

  /**
   * Gets an output tensor by index as serialized bytes.
   *
   * @param arena the arena for allocations
   * @param execPtr the execution context pointer
   * @param index the output index
   * @return the serialized tensor bytes, or null if not available
   * @throws NnException if getting fails
   */
  public byte[] nnExecGetOutputByIndex(
      final Arena arena, final MemorySegment execPtr, final int index) throws NnException {
    try {
      final MemorySegment outData = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outDataLen = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment errorBuf = arena.allocate(ERROR_BUF_SIZE);

      final int result =
          (int)
              execGetOutputByIndex.invokeExact(
                  execPtr, index, outData, outDataLen, errorBuf, ERROR_BUF_SIZE);

      if (result != 0) {
        final String error = errorBuf.getString(0, StandardCharsets.UTF_8);
        throw new NnException("Failed to get output at index " + index + ": " + error);
      }

      final MemorySegment dataPtr = outData.get(ValueLayout.ADDRESS, 0);
      final int dataLen = outDataLen.get(ValueLayout.JAVA_INT, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        return null;
      }

      // Copy data out before freeing native buffer
      final byte[] bytes = dataPtr.reinterpret(dataLen).toArray(ValueLayout.JAVA_BYTE);

      // Free the native buffer
      freeBuffer.invokeExact(dataPtr, dataLen);

      return bytes;
    } catch (NnException e) {
      throw e;
    } catch (Throwable t) {
      throw new NnException("Failed to get output at index " + index + ": " + t.getMessage(), t);
    }
  }

  /**
   * Gets an output tensor by name as serialized bytes.
   *
   * @param arena the arena for allocations
   * @param execPtr the execution context pointer
   * @param name the output name
   * @return the serialized tensor bytes, or null if not available
   * @throws NnException if getting fails
   */
  public byte[] nnExecGetOutputByName(
      final Arena arena, final MemorySegment execPtr, final String name) throws NnException {
    try {
      final MemorySegment nameSegment = arena.allocateFrom(name, StandardCharsets.UTF_8);
      final MemorySegment outData = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outDataLen = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment errorBuf = arena.allocate(ERROR_BUF_SIZE);

      final int result =
          (int)
              execGetOutputByName.invokeExact(
                  execPtr, nameSegment, outData, outDataLen, errorBuf, ERROR_BUF_SIZE);

      if (result != 0) {
        final String error = errorBuf.getString(0, StandardCharsets.UTF_8);
        throw new NnException("Failed to get output '" + name + "': " + error);
      }

      final MemorySegment dataPtr = outData.get(ValueLayout.ADDRESS, 0);
      final int dataLen = outDataLen.get(ValueLayout.JAVA_INT, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        return null;
      }

      final byte[] bytes = dataPtr.reinterpret(dataLen).toArray(ValueLayout.JAVA_BYTE);
      freeBuffer.invokeExact(dataPtr, dataLen);

      return bytes;
    } catch (NnException e) {
      throw e;
    } catch (Throwable t) {
      throw new NnException("Failed to get output '" + name + "': " + t.getMessage(), t);
    }
  }

  /**
   * Closes an execution context.
   *
   * @param execPtr the execution context pointer
   */
  public void nnExecClose(final MemorySegment execPtr) {
    try {
      execClose.invokeExact(execPtr);
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Failed to close NN execution context", t);
    }
  }
}
