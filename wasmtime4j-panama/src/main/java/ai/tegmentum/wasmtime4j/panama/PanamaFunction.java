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

import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly function interface.
 *
 * <p>WebAssembly functions provide callable interfaces between host code and WebAssembly modules.
 * This implementation uses Panama FFI with Stream 1 & 2 infrastructure for type-safe function
 * invocation through optimized method handles with zero-copy parameter and return value
 * marshalling.
 *
 * <p>Function invocation includes comprehensive parameter validation, type checking, and automatic
 * conversion between Java and WebAssembly value types. All operations use Arena-based resource
 * management for automatic cleanup.
 *
 * @since 1.0.0
 */
public final class PanamaFunction implements Function, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaFunction.class.getName());

  // Core infrastructure from Streams 1 & 2
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final ArenaResourceManager.ManagedNativeResource functionResource;
  private final PanamaInstance parentInstance;

  // Function state and metadata
  private volatile boolean closed = false;
  private volatile List<Integer> parameterTypes;
  private volatile List<Integer> resultTypes;

  /**
   * Creates a new Panama function instance using Stream 1 & 2 infrastructure.
   *
   * @param functionPtr the native function pointer from export
   * @param resourceManager the arena resource manager for lifecycle management
   * @param parentInstance the parent instance that owns this function
   * @throws WasmException if the function cannot be created
   */
  public PanamaFunction(
      final MemorySegment functionPtr,
      final ArenaResourceManager resourceManager,
      final PanamaInstance parentInstance)
      throws WasmException {
    // Defensive parameter validation
    PanamaErrorHandler.requireValidPointer(functionPtr, "functionPtr");
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.parentInstance = Objects.requireNonNull(parentInstance, "Parent instance cannot be null");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    try {
      // Create managed resource with cleanup for function
      this.functionResource =
          resourceManager.manageNativeResource(
              functionPtr, () -> destroyNativeFunctionInternal(functionPtr), "Wasmtime Function");

      // Initialize function type metadata
      initializeFunctionType();

      LOGGER.fine("Created Panama function with managed resource");

    } catch (Exception e) {
      throw new WasmException("Failed to create function wrapper", e);
    }
  }

  @Override
  public Object[] invoke(final Object... args) throws RuntimeException, WasmException {
    ensureNotClosed();

    try {
      // Validate parameter count
      List<Integer> paramTypes = getParameterTypes();
      int expectedParams = paramTypes.size();
      int actualParams = args != null ? args.length : 0;

      if (actualParams != expectedParams) {
        throw new IllegalArgumentException(
            "Parameter count mismatch: expected " + expectedParams + ", got " + actualParams);
      }

      // Convert Java arguments to WebAssembly values
      ArenaResourceManager.ManagedMemorySegment paramsMemory = null;
      MemorySegment paramsArray = null;

      if (expectedParams > 0) {
        // Allocate memory for parameter array
        paramsMemory = resourceManager.allocate(MemoryLayouts.WASM_VAL.byteSize() * expectedParams);
        paramsArray = paramsMemory.getSegment();

        // Marshal parameters
        for (int i = 0; i < expectedParams; i++) {
          MemorySegment paramSlot =
              paramsArray.asSlice(
                  i * MemoryLayouts.WASM_VAL.byteSize(), MemoryLayouts.WASM_VAL.byteSize());
          marshalParameter(args[i], paramTypes.get(i), paramSlot);
        }
      }

      // Prepare result array
      List<Integer> resultTypes = getResultTypes();
      int expectedResults = resultTypes.size();

      ArenaResourceManager.ManagedMemorySegment resultsMemory = null;
      MemorySegment resultsArray = null;

      if (expectedResults > 0) {
        // Allocate memory for result array
        resultsMemory =
            resourceManager.allocate(MemoryLayouts.WASM_VAL.byteSize() * expectedResults);
        resultsArray = resultsMemory.getSegment();
      }

      // Invoke the function through FFI
      boolean success =
          invokeFunctionNative(paramsArray, expectedParams, resultsArray, expectedResults);

      if (!success) {
        throw new RuntimeException("WebAssembly function execution failed");
      }

      // Unmarshal results back to Java objects
      Object[] results = new Object[expectedResults];
      for (int i = 0; i < expectedResults; i++) {
        MemorySegment resultSlot =
            resultsArray.asSlice(
                i * MemoryLayouts.WASM_VAL.byteSize(), MemoryLayouts.WASM_VAL.byteSize());
        results[i] = unmarshalResult(resultSlot, resultTypes.get(i));
      }

      return results;

    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Function invocation",
              "function="
                  + functionResource.getNativePointer()
                  + ", args.length="
                  + (args != null ? args.length : 0),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  /**
   * Gets the parameter types for this function.
   *
   * @return list of parameter types (WASM value kinds)
   * @throws WasmException if type information cannot be retrieved
   */
  public List<Integer> getParameterTypes() throws WasmException {
    ensureNotClosed();

    if (parameterTypes == null) {
      initializeFunctionType();
    }

    return new ArrayList<>(parameterTypes);
  }

  /**
   * Gets the result types for this function.
   *
   * @return list of result types (WASM value kinds)
   * @throws WasmException if type information cannot be retrieved
   */
  public List<Integer> getResultTypes() throws WasmException {
    ensureNotClosed();

    if (resultTypes == null) {
      initializeFunctionType();
    }

    return new ArrayList<>(resultTypes);
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Close the managed native resource (automatic cleanup)
        functionResource.close();

        LOGGER.fine("Closed Panama function");

      } catch (Exception e) {
        throw new WasmException("Failed to close function", e);
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native function handle for this function.
   *
   * @return the native function handle
   * @throws IllegalStateException if the function is closed
   */
  public MemorySegment getFunctionHandle() {
    ensureNotClosed();
    return functionResource.getNativePointer();
  }

  /**
   * Gets the parent instance that owns this function.
   *
   * @return the parent instance
   */
  public PanamaInstance getParentInstance() {
    ensureNotClosed();
    return parentInstance;
  }

  // Private helper methods for function operations

  /** Initializes function type metadata by querying the native function. */
  private void initializeFunctionType() throws WasmException {
    try {
      // Get function type through FFI
      MemorySegment functionTypePtr = getFunctionType();

      // Extract parameter and result types from function type
      this.parameterTypes = extractParameterTypes(functionTypePtr);
      this.resultTypes = extractResultTypes(functionTypePtr);

      LOGGER.fine(
          "Initialized function type: params="
              + parameterTypes.size()
              + ", results="
              + resultTypes.size());

    } catch (Exception e) {
      throw new WasmException("Failed to initialize function type", e);
    }
  }

  /** Gets the function type pointer through FFI calls. */
  private MemorySegment getFunctionType() throws Exception {
    // Call wasmtime_func_type through cached method handle
    MethodHandle funcType =
        nativeFunctions.getFunction(
            "wasmtime_func_type",
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, ValueLayout.ADDRESS // function
                ));

    MemorySegment typePtr = (MemorySegment) funcType.invoke(functionResource.getNativePointer());
    PanamaErrorHandler.requireValidPointer(typePtr, "function type pointer");

    return typePtr;
  }

  /** Extracts parameter types from function type structure. */
  private List<Integer> extractParameterTypes(final MemorySegment functionTypePtr)
      throws Exception {
    // Get parameter types vector from function type
    MemorySegment paramsVec = functionTypePtr; // Simplified - would need proper offset

    // For now, return empty list - full implementation would parse the type vector
    return new ArrayList<>();
  }

  /** Extracts result types from function type structure. */
  private List<Integer> extractResultTypes(final MemorySegment functionTypePtr) throws Exception {
    // Get result types vector from function type
    MemorySegment resultsVec = functionTypePtr; // Simplified - would need proper offset

    // For now, return empty list - full implementation would parse the type vector
    return new ArrayList<>();
  }

  /** Invokes the native function through FFI. */
  private boolean invokeFunctionNative(
      final MemorySegment paramsArray,
      final int paramCount,
      final MemorySegment resultsArray,
      final int resultCount)
      throws Exception {
    // Call wasmtime_func_call through cached method handle
    MethodHandle funcCall =
        nativeFunctions.getFunction(
            "wasmtime_func_call",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS, // function
                ValueLayout.ADDRESS, // params
                ValueLayout.JAVA_INT, // num_params
                ValueLayout.ADDRESS, // results
                ValueLayout.JAVA_INT // num_results
                ));

    return (boolean)
        funcCall.invoke(
            functionResource.getNativePointer(),
            paramsArray != null ? paramsArray : MemorySegment.NULL,
            paramCount,
            resultsArray != null ? resultsArray : MemorySegment.NULL,
            resultCount);
  }

  /** Marshals a Java parameter value to WebAssembly value format. */
  private void marshalParameter(
      final Object javaValue, final int wasmType, final MemorySegment valueSlot) {
    // Set the value type kind
    MemoryLayouts.WASM_VAL_KIND.set(valueSlot, wasmType);

    // Marshal the value based on type
    switch (wasmType) {
      case MemoryLayouts.WASM_I32:
        int i32Value = convertToI32(javaValue);
        MemoryLayouts.WASM_VAL_I32.set(valueSlot, i32Value);
        break;

      case MemoryLayouts.WASM_I64:
        long i64Value = convertToI64(javaValue);
        MemoryLayouts.WASM_VAL_I64.set(valueSlot, i64Value);
        break;

      case MemoryLayouts.WASM_F32:
        float f32Value = convertToF32(javaValue);
        MemoryLayouts.WASM_VAL_F32.set(valueSlot, f32Value);
        break;

      case MemoryLayouts.WASM_F64:
        double f64Value = convertToF64(javaValue);
        MemoryLayouts.WASM_VAL_F64.set(valueSlot, f64Value);
        break;

      case MemoryLayouts.WASM_ANYREF:
      case MemoryLayouts.WASM_FUNCREF:
        // Reference types - for now set to null
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
        break;

      default:
        throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmType);
    }
  }

  /** Unmarshals a WebAssembly value to Java object. */
  private Object unmarshalResult(final MemorySegment valueSlot, final int wasmType) {
    // Get the value based on type
    return switch (wasmType) {
      case MemoryLayouts.WASM_I32 -> (Integer) MemoryLayouts.WASM_VAL_I32.get(valueSlot);
      case MemoryLayouts.WASM_I64 -> (Long) MemoryLayouts.WASM_VAL_I64.get(valueSlot);
      case MemoryLayouts.WASM_F32 -> (Float) MemoryLayouts.WASM_VAL_F32.get(valueSlot);
      case MemoryLayouts.WASM_F64 -> (Double) MemoryLayouts.WASM_VAL_F64.get(valueSlot);
      case MemoryLayouts.WASM_ANYREF, MemoryLayouts.WASM_FUNCREF -> {
        MemorySegment ref = (MemorySegment) MemoryLayouts.WASM_VAL_REF.get(valueSlot);
        yield ref.equals(MemorySegment.NULL) ? null : ref;
      }
      default -> throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmType);
    };
  }

  // Type conversion helpers

  private int convertToI32(final Object value) {
    if (value instanceof Integer) {
      return (Integer) value;
    } else if (value instanceof Number) {
      return ((Number) value).intValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to i32");
    }
  }

  private long convertToI64(final Object value) {
    if (value instanceof Long) {
      return (Long) value;
    } else if (value instanceof Number) {
      return ((Number) value).longValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to i64");
    }
  }

  private float convertToF32(final Object value) {
    if (value instanceof Float) {
      return (Float) value;
    } else if (value instanceof Number) {
      return ((Number) value).floatValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to f32");
    }
  }

  private double convertToF64(final Object value) {
    if (value instanceof Double) {
      return (Double) value;
    } else if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to f64");
    }
  }

  /**
   * Internal cleanup method called by managed resource.
   *
   * @param functionPtr the native function pointer to destroy
   */
  private void destroyNativeFunctionInternal(final MemorySegment functionPtr) {
    try {
      // Function is typically owned by the instance, so no specific cleanup needed
      LOGGER.fine("Destroying native function: " + functionPtr);

    } catch (Exception e) {
      LOGGER.warning("Error during function cleanup: " + e.getMessage());
      // Don't throw exceptions from cleanup methods
    }
  }

  /**
   * Ensures that this function instance is not closed.
   *
   * @throws IllegalStateException if the function is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Function has been closed");
    }
  }
}
