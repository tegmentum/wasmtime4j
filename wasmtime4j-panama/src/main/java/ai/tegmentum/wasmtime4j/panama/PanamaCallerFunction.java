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

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.TypedFunc;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
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
  private final NativeInstanceBindings bindings;
  private volatile FunctionType cachedFunctionType;
  private final NativeResourceHandle resourceHandle;

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
    this.bindings = NativeInstanceBindings.getInstance();

    // Capture handle for safety net (must not capture 'this')
    final MemorySegment capturedHandle = funcHandle;
    final NativeInstanceBindings capturedBindings = this.bindings;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaCallerFunction",
            () -> {
              try {
                capturedBindings.funcDestroy(capturedHandle);
                LOGGER.fine("Closed PanamaCallerFunction: " + name);
              } catch (final Throwable t) {
                throw new Exception("Error closing function: " + t.getMessage(), t);
              }
            },
            this,
            () -> {
              try {
                capturedBindings.funcDestroy(capturedHandle);
              } catch (final Exception e) {
                LOGGER.warning("Safety net cleanup failed for PanamaCallerFunction: " + name);
              }
            });

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

      // Allocate memory using 20-byte FfiWasmValue layout (matching native Rust struct)
      final MemorySegment paramsSegment =
          paramCount > 0
              ? arena.allocate(
                  (long) WasmValueMarshaller.WASM_VALUE_SIZE * paramCount,
                  ValueLayout.JAVA_INT.byteAlignment())
              : MemorySegment.NULL;
      final MemorySegment resultsSegment =
          resultCount > 0
              ? arena.allocate(
                  (long) WasmValueMarshaller.WASM_VALUE_SIZE * resultCount,
                  ValueLayout.JAVA_INT.byteAlignment())
              : MemorySegment.NULL;

      // Marshal parameters using WasmValueMarshaller (correct 20-byte layout)
      if (paramCount > 0) {
        for (int i = 0; i < paramCount; i++) {
          WasmValueMarshaller.marshalWasmValue(params[i], paramsSegment, i, null);
        }
      }

      // Call native function
      final MemorySegment storePtr = store.getNativeStore();
      final int result =
          bindings.funcCall(
              funcHandle, storePtr, paramsSegment, paramCount, resultsSegment, resultCount);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Function call failed for '" + name + "'");
      }

      // Unmarshal results using WasmValueMarshaller (correct 20-byte layout)
      if (resultCount > 0) {
        final WasmValue[] results = new WasmValue[resultCount];
        for (int i = 0; i < resultCount; i++) {
          results[i] = WasmValueMarshaller.unmarshalWasmValue(resultsSegment, i, null);
        }
        return results;
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
        paramTypes[i] = WasmValueType.fromNativeTypeCode(typeVal);
      }
      for (int i = 0; i < returnCount; i++) {
        final int typeVal =
            funcTypePtr.get(java.lang.foreign.ValueLayout.JAVA_INT, 8 + (paramCount + i) * 4L);
        returnTypes[i] = WasmValueType.fromNativeTypeCode(typeVal);
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
  public long toRawFuncRef() throws WasmException {
    ensureNotClosed();
    return bindings.funcToRaw(funcHandle, store.getNativeStore());
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
    final boolean useNativeAsync = store != null && store.isAsync();
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            if (useNativeAsync) {
              return callNativeAsync(params);
            }
            return call(params);
          } catch (final WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * Calls this function using Wasmtime's native async call path.
   *
   * @param params the parameters to pass
   * @return the results
   * @throws WasmException if the call fails
   */
  private WasmValue[] callNativeAsync(final WasmValue... params) throws WasmException {
    if (params == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }
    ensureNotClosed();

    try (final Arena arena = Arena.ofConfined()) {
      final FunctionType funcType = getFunctionType();
      final int paramCount = params.length;
      final int resultCount = funcType.getReturnCount();

      final MemorySegment paramsSegment =
          paramCount > 0
              ? arena.allocate(
                  (long) WasmValueMarshaller.WASM_VALUE_SIZE * paramCount,
                  ValueLayout.JAVA_INT.byteAlignment())
              : MemorySegment.NULL;
      final MemorySegment resultsSegment =
          resultCount > 0
              ? arena.allocate(
                  (long) WasmValueMarshaller.WASM_VALUE_SIZE * resultCount,
                  ValueLayout.JAVA_INT.byteAlignment())
              : MemorySegment.NULL;

      if (paramCount > 0) {
        for (int i = 0; i < paramCount; i++) {
          WasmValueMarshaller.marshalWasmValue(params[i], paramsSegment, i, null);
        }
      }

      final MemorySegment storePtr = store.getNativeStore();
      final long nativeResultCount =
          bindings.funcCallNativeAsync(
              funcHandle, storePtr, paramsSegment, paramCount, resultsSegment, resultCount);

      if (nativeResultCount < 0) {
        throw PanamaErrorMapper.mapNativeError(
            (int) nativeResultCount, "Async function call failed for '" + name + "'");
      }

      final int actualCount = (int) nativeResultCount;
      if (actualCount > 0) {
        final WasmValue[] results = new WasmValue[actualCount];
        for (int i = 0; i < actualCount; i++) {
          results[i] = WasmValueMarshaller.unmarshalWasmValue(resultsSegment, i, null);
        }
        return results;
      }
      return new WasmValue[0];

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Failed async call to function '" + name + "': " + e.getMessage(), e);
    }
  }

  @Override
  public boolean matchesFuncType(
      final ai.tegmentum.wasmtime4j.Store storeCtx, final FunctionType funcType)
      throws WasmException {
    if (storeCtx == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (funcType == null) {
      return false;
    }
    ensureNotClosed();

    final WasmValueType[] paramTypes = funcType.getParamTypes();
    final WasmValueType[] resultTypes = funcType.getReturnTypes();

    try (final Arena matchArena = Arena.ofConfined()) {
      final MemorySegment paramCodes =
          paramTypes.length > 0
              ? matchArena.allocate(ValueLayout.JAVA_INT, paramTypes.length)
              : MemorySegment.NULL;
      for (int i = 0; i < paramTypes.length; i++) {
        paramCodes.setAtIndex(ValueLayout.JAVA_INT, i, paramTypes[i].ordinal());
      }

      final MemorySegment resultCodes =
          resultTypes.length > 0
              ? matchArena.allocate(ValueLayout.JAVA_INT, resultTypes.length)
              : MemorySegment.NULL;
      for (int i = 0; i < resultTypes.length; i++) {
        resultCodes.setAtIndex(ValueLayout.JAVA_INT, i, resultTypes[i].ordinal());
      }

      final MemorySegment storePtr = store.getNativeStore();
      final int result =
          bindings.funcMatchesTy(
              funcHandle, storePtr, paramCodes, paramTypes.length, resultCodes, resultTypes.length);
      if (result < 0) {
        throw new WasmException("Native func_matches_ty check failed");
      }
      return result == 1;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("func_matches_ty failed: " + e.getMessage(), e);
    }
  }

  /** Closes the function and releases resources. */
  public void close() {
    resourceHandle.close();
  }

  /**
   * Ensures the function is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Gets the native function handle.
   *
   * @return the native function memory segment
   */
  MemorySegment getFuncHandle() {
    return funcHandle;
  }

  @Override
  public String toString() {
    return String.format(
        "PanamaCallerFunction{name='%s', handle=0x%x}", name, funcHandle.address());
  }
}
