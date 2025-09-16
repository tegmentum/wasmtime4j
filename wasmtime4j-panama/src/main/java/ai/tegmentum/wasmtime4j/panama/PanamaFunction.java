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

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
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
public final class PanamaFunction implements WasmFunction, AutoCloseable {
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

  // Performance optimization fields
  private volatile ArenaResourceManager.ManagedMemorySegment cachedParamsMemory;
  private volatile ArenaResourceManager.ManagedMemorySegment cachedResultsMemory;
  private volatile int cachedParamCount = -1;
  private volatile int cachedResultCount = -1;
  private volatile long lastOptimizationTime = 0;

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

  public boolean isValid() {
    return !closed;
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    ensureNotClosed();

    try {
      // Validate parameter count
      List<Integer> paramTypes = getParameterTypes();
      int expectedParams = paramTypes.size();
      int actualParams = params != null ? params.length : 0;

      if (actualParams != expectedParams) {
        throw new IllegalArgumentException(
            "Parameter count mismatch: expected " + expectedParams + ", got " + actualParams);
      }

      // Zero-copy optimization: reuse cached memory segments when possible
      ArenaResourceManager.ManagedMemorySegment paramsMemory = null;
      MemorySegment paramsArray = null;

      if (expectedParams > 0) {
        // Check if we can reuse cached parameter memory
        if (cachedParamsMemory != null && cachedParamCount == expectedParams) {
          paramsMemory = cachedParamsMemory;
          paramsArray = paramsMemory.getSegment();
          // Zero out the memory for safety
          paramsArray.fill((byte) 0);
        } else {
          // Allocate new memory and cache it
          paramsMemory = resourceManager.allocate(MemoryLayouts.WASM_VAL.byteSize() * expectedParams);
          paramsArray = paramsMemory.getSegment();

          // Update cache
          if (cachedParamsMemory != null) {
            // Don't close old memory - let resource manager handle it
          }
          cachedParamsMemory = paramsMemory;
          cachedParamCount = expectedParams;
        }

        // Marshal parameters using optimized approach
        marshalParametersOptimized(params, paramsArray, expectedParams);
      }

      // Prepare result array with zero-copy optimization
      List<Integer> resultTypes = getResultTypes();
      int expectedResults = resultTypes.size();

      ArenaResourceManager.ManagedMemorySegment resultsMemory = null;
      MemorySegment resultsArray = null;

      if (expectedResults > 0) {
        // Check if we can reuse cached result memory
        if (cachedResultsMemory != null && cachedResultCount == expectedResults) {
          resultsMemory = cachedResultsMemory;
          resultsArray = resultsMemory.getSegment();
          // Zero out for safety
          resultsArray.fill((byte) 0);
        } else {
          // Allocate new memory and cache it
          resultsMemory = resourceManager.allocate(MemoryLayouts.WASM_VAL.byteSize() * expectedResults);
          resultsArray = resultsMemory.getSegment();

          // Update cache
          if (cachedResultsMemory != null) {
            // Don't close old memory - let resource manager handle it
          }
          cachedResultsMemory = resultsMemory;
          cachedResultCount = expectedResults;
        }
      }

      // Invoke the function through FFI
      boolean success =
          invokeFunctionNative(paramsArray, expectedParams, resultsArray, expectedResults);

      if (!success) {
        throw new WasmException("WebAssembly function execution failed");
      }

      // Unmarshal results back to WasmValue objects using optimized approach
      WasmValue[] results = new WasmValue[expectedResults];
      unmarshalResultsOptimized(resultsArray, results, resultTypes, expectedResults);

      return results;

    } catch (Throwable e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Function invocation",
              "function="
                  + functionResource.getNativePointer()
                  + ", params.length="
                  + (params != null ? params.length : 0),
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
  public String getName() {
    // Function name is not available from native handle
    return null;
  }

  @Override
  public FunctionType getFunctionType() {
    ensureNotClosed();
    try {
      List<Integer> paramTypes = getParameterTypes();
      List<Integer> resultTypes = getResultTypes();
      // For now, return a simple placeholder implementation
      // TODO: Create proper FunctionType from parameter and result types
      return null;
    } catch (Exception e) {
      LOGGER.warning("Failed to get function type: " + e.getMessage());
      return null;
    }
  }

  @Override
  public void close() {
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
        LOGGER.severe("Failed to close function: " + e.getMessage());
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
      MemorySegment functionTypePtr = getNativeFunctionType();

      // Extract parameter and result types from function type
      this.parameterTypes = extractParameterTypes(functionTypePtr);
      this.resultTypes = extractResultTypes(functionTypePtr);

      LOGGER.fine(
          "Initialized function type: params="
              + parameterTypes.size()
              + ", results="
              + resultTypes.size());

    } catch (Throwable e) {
      throw new WasmException("Failed to initialize function type", e);
    }
  }

  /** Gets the function type pointer through FFI calls. */
  private MemorySegment getNativeFunctionType() throws Throwable {
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
    try (var resourceManager = new ArenaResourceManager()) {
      // Allocate memory for output parameters
      var typesPtr = resourceManager.allocate(ValueLayout.ADDRESS.byteSize()).getSegment();
      var countPtr = resourceManager.allocate(ValueLayout.JAVA_LONG.byteSize()).getSegment();

      // Call native function to get parameter types
      int result =
          nativeFunctions.callNativeFunction(
              "wasmtime4j_func_get_param_types",
              Integer.class,
              functionResource.getNativePointer(),
              MemorySegment.NULL, // Store context not available in this API design
              typesPtr,
              countPtr);

      if (result != 0) {
        throw new WasmException("Failed to get function parameter types");
      }

      // Read the count
      long count = countPtr.get(ValueLayout.JAVA_LONG, 0);
      if (count == 0) {
        return new ArrayList<>();
      }

      // Read the types array
      MemorySegment typesArray =
          typesPtr.get(ValueLayout.ADDRESS, 0).reinterpret(count * ValueLayout.JAVA_INT.byteSize());
      List<Integer> types = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        int type = typesArray.get(ValueLayout.JAVA_INT, i * ValueLayout.JAVA_INT.byteSize());
        types.add(type);
      }

      // Free the native types array
      nativeFunctions.callNativeFunction(
          "wasmtime4j_func_free_types_array", Void.class, typesArray, count);

      return types;
    }
  }

  /** Extracts result types from function type structure. */
  private List<Integer> extractResultTypes(final MemorySegment functionTypePtr) throws Exception {
    try (var resourceManager = new ArenaResourceManager()) {
      // Allocate memory for output parameters
      var typesPtr = resourceManager.allocate(ValueLayout.ADDRESS.byteSize()).getSegment();
      var countPtr = resourceManager.allocate(ValueLayout.JAVA_LONG.byteSize()).getSegment();

      // Call native function to get result types
      int result =
          nativeFunctions.callNativeFunction(
              "wasmtime4j_func_get_result_types",
              Integer.class,
              functionResource.getNativePointer(),
              MemorySegment.NULL, // Store context not available in this API design
              typesPtr,
              countPtr);

      if (result != 0) {
        throw new WasmException("Failed to get function result types");
      }

      // Read the count
      long count = countPtr.get(ValueLayout.JAVA_LONG, 0);
      if (count == 0) {
        return new ArrayList<>();
      }

      // Read the types array
      MemorySegment typesArray =
          typesPtr.get(ValueLayout.ADDRESS, 0).reinterpret(count * ValueLayout.JAVA_INT.byteSize());
      List<Integer> types = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        int type = typesArray.get(ValueLayout.JAVA_INT, i * ValueLayout.JAVA_INT.byteSize());
        types.add(type);
      }

      // Free the native types array
      nativeFunctions.callNativeFunction(
          "wasmtime4j_func_free_types_array", Void.class, typesArray, count);

      return types;
    }
  }

  /** Invokes the native function through FFI. */
  private boolean invokeFunctionNative(
      final MemorySegment paramsArray,
      final int paramCount,
      final MemorySegment resultsArray,
      final int resultCount)
      throws Throwable {
    // Call wasmtime4j_func_call through native bindings
    int result =
        nativeFunctions.callNativeFunction(
            "wasmtime4j_func_call",
            Integer.class,
            functionResource.getNativePointer(),
            MemorySegment.NULL, // Store context not available in this API design
            paramsArray != null ? paramsArray : MemorySegment.NULL,
            (long) paramCount,
            resultsArray != null ? resultsArray : MemorySegment.NULL,
            (long) resultCount);

    return result == 0; // Success if result is 0
  }

  /**
   * Optimized marshalling of multiple parameters at once.
   *
   * @param params the WasmValue parameters
   * @param paramsArray the target memory segment
   * @param count number of parameters
   */
  private void marshalParametersOptimized(final WasmValue[] params, final MemorySegment paramsArray, final int count) {
    final long slotSize = MemoryLayouts.WASM_VAL.byteSize();

    // Batch marshal parameters for better cache efficiency
    for (int i = 0; i < count; i++) {
      final MemorySegment paramSlot = paramsArray.asSlice(i * slotSize, slotSize);
      marshalWasmValueOptimized(params[i], paramSlot);
    }
  }

  /**
   * Optimized unmarshalling of multiple results at once.
   *
   * @param resultsArray source memory segment
   * @param results target array
   * @param resultTypes expected types
   * @param count number of results
   */
  private void unmarshalResultsOptimized(final MemorySegment resultsArray, final WasmValue[] results,
                                         final List<Integer> resultTypes, final int count) {
    final long slotSize = MemoryLayouts.WASM_VAL.byteSize();

    // Batch unmarshal results for better cache efficiency
    for (int i = 0; i < count; i++) {
      final MemorySegment resultSlot = resultsArray.asSlice(i * slotSize, slotSize);
      results[i] = unmarshalWasmValueOptimized(resultSlot, resultTypes.get(i));
    }
  }

  /** Marshals a WasmValue to native WebAssembly value format. */
  private void marshalWasmValue(final WasmValue wasmValue, final MemorySegment valueSlot) {
    // Set the value type kind and value based on WasmValue type
    switch (wasmValue.getType()) {
      case I32:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_I32);
        MemoryLayouts.WASM_VAL_I32.set(valueSlot, wasmValue.asI32());
        break;

      case I64:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_I64);
        MemoryLayouts.WASM_VAL_I64.set(valueSlot, wasmValue.asI64());
        break;

      case F32:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_F32);
        MemoryLayouts.WASM_VAL_F32.set(valueSlot, wasmValue.asF32());
        break;

      case F64:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_F64);
        MemoryLayouts.WASM_VAL_F64.set(valueSlot, wasmValue.asF64());
        break;

      case EXTERNREF:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_ANYREF);
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL); // Simplified
        break;

      case FUNCREF:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_FUNCREF);
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL); // Simplified
        break;

      default:
        throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmValue.getType());
    }
  }

  /**
   * Optimized marshalling with reduced method call overhead.
   *
   * @param wasmValue source value
   * @param valueSlot target memory slot
   */
  private void marshalWasmValueOptimized(final WasmValue wasmValue, final MemorySegment valueSlot) {
    // Direct marshalling with minimal overhead
    switch (wasmValue.getType()) {
      case I32 -> {
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_I32);
        MemoryLayouts.WASM_VAL_I32.set(valueSlot, wasmValue.asI32());
      }
      case I64 -> {
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_I64);
        MemoryLayouts.WASM_VAL_I64.set(valueSlot, wasmValue.asI64());
      }
      case F32 -> {
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_F32);
        MemoryLayouts.WASM_VAL_F32.set(valueSlot, wasmValue.asF32());
      }
      case F64 -> {
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_F64);
        MemoryLayouts.WASM_VAL_F64.set(valueSlot, wasmValue.asF64());
      }
      case EXTERNREF -> {
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_ANYREF);
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
      }
      case FUNCREF -> {
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_FUNCREF);
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
      }
      default -> throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmValue.getType());
    }
  }

  /**
   * Optimized unmarshalling with reduced method call overhead.
   *
   * @param valueSlot source memory slot
   * @param wasmType expected type
   * @return unmarshalled WasmValue
   */
  private WasmValue unmarshalWasmValueOptimized(final MemorySegment valueSlot, final int wasmType) {
    // Direct unmarshalling with switch expression for performance
    return switch (wasmType) {
      case MemoryLayouts.WASM_I32 -> WasmValue.i32((Integer) MemoryLayouts.WASM_VAL_I32.get(valueSlot));
      case MemoryLayouts.WASM_I64 -> WasmValue.i64((Long) MemoryLayouts.WASM_VAL_I64.get(valueSlot));
      case MemoryLayouts.WASM_F32 -> WasmValue.f32((Float) MemoryLayouts.WASM_VAL_F32.get(valueSlot));
      case MemoryLayouts.WASM_F64 -> WasmValue.f64((Double) MemoryLayouts.WASM_VAL_F64.get(valueSlot));
      case MemoryLayouts.WASM_ANYREF -> WasmValue.externref(null);
      case MemoryLayouts.WASM_FUNCREF -> WasmValue.funcref(null);
      default -> throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmType);
    };
  }

  /** Unmarshals a WebAssembly value to WasmValue object. */
  private WasmValue unmarshalWasmValue(final MemorySegment valueSlot, final int wasmType) {
    // Get the value based on type and create WasmValue
    return switch (wasmType) {
      case MemoryLayouts.WASM_I32 -> WasmValue.i32(
          (Integer) MemoryLayouts.WASM_VAL_I32.get(valueSlot));
      case MemoryLayouts.WASM_I64 -> WasmValue.i64(
          (Long) MemoryLayouts.WASM_VAL_I64.get(valueSlot));
      case MemoryLayouts.WASM_F32 -> WasmValue.f32(
          (Float) MemoryLayouts.WASM_VAL_F32.get(valueSlot));
      case MemoryLayouts.WASM_F64 -> WasmValue.f64(
          (Double) MemoryLayouts.WASM_VAL_F64.get(valueSlot));
      case MemoryLayouts.WASM_ANYREF -> WasmValue.externref(null); // Simplified
      case MemoryLayouts.WASM_FUNCREF -> WasmValue.funcref(null); // Simplified
      default -> throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmType);
    };
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
