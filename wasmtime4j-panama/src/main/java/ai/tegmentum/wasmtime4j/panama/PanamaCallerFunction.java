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

import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.TypedFunc;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaTypeConverter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Function for Caller context.
 *
 * <p>This class wraps a function handle obtained from a Caller context and provides function
 * invocation through direct native calls rather than instance delegation.
 *
 * @since 1.0.0
 */
final class PanamaCallerFunction implements WasmFunction, TypedFunc.TypedFunctionSupport {
  private static final Logger LOGGER = Logger.getLogger(PanamaCallerFunction.class.getName());

  private final MemorySegment funcHandle;
  private final PanamaStore store;
  private final String name;
  private final NativeFunctionBindings bindings;
  private volatile FunctionType cachedFunctionType;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama caller function with the given handle.
   *
   * @param funcHandle the native function handle from caller context
   * @param store the store this function is associated with
   * @param name the function name
   */
  PanamaCallerFunction(final MemorySegment funcHandle, final PanamaStore store, final String name) {
    if (funcHandle == null || funcHandle.equals(MemorySegment.NULL) || funcHandle.address() == 0) {
      throw new IllegalArgumentException("Function handle cannot be null or zero");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    this.funcHandle = funcHandle;
    this.store = store;
    this.name = name;
    this.bindings = NativeFunctionBindings.getInstance();

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "Created PanamaCallerFunction '"
              + name
              + "' with handle: 0x"
              + Long.toHexString(funcHandle.address()));
    }
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    if (params == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }
    ensureNotClosed();

    try (final Arena arena = Arena.ofConfined()) {
      final FunctionType funcType = getFunctionType();
      final int paramCount = params.length;
      final int resultCount = funcType.getReturnCount();

      // Allocate memory for parameters and results using WASM_VAL layout
      final long wasmValSize = MemoryLayouts.WASM_VAL.byteSize();
      final MemorySegment paramsSegment =
          paramCount > 0
              ? arena.allocate(wasmValSize * paramCount, MemoryLayouts.WASM_VAL.byteAlignment())
              : MemorySegment.NULL;
      final MemorySegment resultsSegment =
          resultCount > 0
              ? arena.allocate(wasmValSize * resultCount, MemoryLayouts.WASM_VAL.byteAlignment())
              : MemorySegment.NULL;

      // Marshal parameters
      if (paramCount > 0) {
        PanamaTypeConverter.marshalParameters(params, paramsSegment);
      }

      // Call native function
      final MemorySegment storePtr = store.getNativeStore();
      final int result =
          bindings.funcCall(
              funcHandle, storePtr, paramsSegment, paramCount, resultsSegment, resultCount);

      if (result != 0) {
        throw new WasmException(
            "Function call failed for '" + name + "' (error code: " + result + ")");
      }

      // Unmarshal results
      if (resultCount > 0) {
        return PanamaTypeConverter.unmarshalResults(resultsSegment, funcType.getReturnTypes());
      }
      return new WasmValue[0];

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Failed to call function '" + name + "': " + e.getMessage(), e);
    }
  }

  @Override
  public FunctionType getFunctionType() {
    if (cachedFunctionType != null) {
      return cachedFunctionType;
    }

    ensureNotClosed();
    try {
      final MemorySegment storePtr = store.getNativeStore();
      final MemorySegment funcTypePtr = bindings.funcGetType(funcHandle, storePtr);

      if (funcTypePtr == null || funcTypePtr.equals(MemorySegment.NULL)) {
        // Return empty function type if native call fails
        cachedFunctionType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
        return cachedFunctionType;
      }

      // Parse function type from native pointer
      // The native layer returns a struct with param count, return count, and type arrays
      final int paramCount = funcTypePtr.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);
      final int returnCount = funcTypePtr.get(java.lang.foreign.ValueLayout.JAVA_INT, 4);

      final WasmValueType[] paramTypes = new WasmValueType[paramCount];
      final WasmValueType[] returnTypes = new WasmValueType[returnCount];

      // Types start at offset 8 (after two ints)
      for (int i = 0; i < paramCount; i++) {
        final int typeVal = funcTypePtr.get(java.lang.foreign.ValueLayout.JAVA_INT, 8 + i * 4L);
        paramTypes[i] = PanamaTypeConverter.nativeToWasmType(typeVal);
      }
      for (int i = 0; i < returnCount; i++) {
        final int typeVal =
            funcTypePtr.get(java.lang.foreign.ValueLayout.JAVA_INT, 8 + (paramCount + i) * 4L);
        returnTypes[i] = PanamaTypeConverter.nativeToWasmType(typeVal);
      }

      // Clean up the native function type
      bindings.funcTypeDestroy(funcTypePtr);

      cachedFunctionType = new FunctionType(paramTypes, returnTypes);
      return cachedFunctionType;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get function type for '" + name + "'", e);
      // Return empty type on error
      cachedFunctionType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      return cachedFunctionType;
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public TypedFunc asTyped(final String signature) {
    return new PanamaTypedFunc(this, signature);
  }

  @Override
  public java.util.concurrent.CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return call(params);
          } catch (final WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /** Closes the function and releases resources. */
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    try {
      bindings.funcDestroy(funcHandle);
      LOGGER.fine("Closed PanamaCallerFunction: " + name);
    } catch (final Exception e) {
      LOGGER.warning("Error closing function: " + e.getMessage());
    }
  }

  /**
   * Ensures the function is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Function has been closed");
    }
  }

  @Override
  public String toString() {
    return String.format(
        "PanamaCallerFunction{name='%s', handle=0x%x}", name, funcHandle.address());
  }
}
