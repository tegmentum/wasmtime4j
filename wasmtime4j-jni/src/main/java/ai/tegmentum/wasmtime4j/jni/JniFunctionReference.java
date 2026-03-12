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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniTypeConverter;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation for WebAssembly function references.
 *
 * <p>Function references enable dynamic function dispatch and callback mechanisms in WebAssembly
 * programs. They can be passed as parameters, stored in tables, and called indirectly through
 * call_indirect instructions.
 *
 * <p>This implementation provides thread-safe management of function references with comprehensive
 * defensive programming practices to prevent JVM crashes and resource leaks.
 *
 * <p>Key Features:
 *
 * <ul>
 *   <li>Thread-safe function reference creation and management
 *   <li>Support for both host functions and WebAssembly functions
 *   <li>Automatic resource cleanup and lifecycle management
 *   <li>Parameter and return value marshalling
 *   <li>Error handling and exception propagation
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniFunctionReference extends JniResource implements FunctionReference {
  private static final Logger LOGGER = Logger.getLogger(JniFunctionReference.class.getName());

  // Global registry for function references to prevent GC
  private static final ConcurrentHashMap<Long, JniFunctionReference> FUNCTION_REFERENCE_REGISTRY =
      new ConcurrentHashMap<>();
  private static final AtomicLong NEXT_FUNCTION_REFERENCE_ID = new AtomicLong(1L);

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniFunctionReference: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long functionReferenceId;
  private final String functionName;
  private final FunctionType functionType;
  private final HostFunction hostFunction; // Null for WebAssembly functions
  private final WasmFunction wasmFunction; // Null for host functions
  private final WeakReference<JniStore> storeRef;

  /** Helper class to hold both the native handle and function reference ID. */
  private static class FunctionReferenceHandle {
    final long nativeHandle;
    final long functionReferenceId;

    FunctionReferenceHandle(final long nativeHandle, final long functionReferenceId) {
      this.nativeHandle = nativeHandle;
      this.functionReferenceId = functionReferenceId;
    }
  }

  /**
   * Creates the native handle for a function reference from a host function.
   *
   * @param hostFunction the host function implementation
   * @param functionType the WebAssembly function type signature
   * @param store the store this function reference belongs to
   * @return the function reference handle containing native handle and ID
   * @throws WasmException if creation fails
   */
  private static FunctionReferenceHandle createNativeHandleFromHost(
      final HostFunction hostFunction, final FunctionType functionType, final JniStore store)
      throws WasmException {
    if (hostFunction == null) {
      throw new WasmException(
          "Failed to create native function reference: Host function cannot be null");
    }
    if (functionType == null) {
      throw new WasmException(
          "Failed to create native function reference: Function type cannot be null");
    }
    if (store == null) {
      throw new WasmException("Failed to create native function reference: Store cannot be null");
    }

    final long functionReferenceId = NEXT_FUNCTION_REFERENCE_ID.getAndIncrement();
    final long nativeHandle =
        nativeCreateFunctionReferenceFromHost(
            store.getNativeHandle(),
            JniTypeConverter.marshalFunctionType(functionType),
            functionReferenceId);

    if (nativeHandle == 0) {
      throw new WasmException("Failed to create native function reference from host function");
    }

    return new FunctionReferenceHandle(nativeHandle, functionReferenceId);
  }

  /**
   * Creates the native handle for a function reference from a WebAssembly function.
   *
   * @param wasmFunction the WebAssembly function
   * @param store the store this function reference belongs to
   * @return the function reference handle containing native handle and ID
   * @throws WasmException if creation fails
   */
  private static FunctionReferenceHandle createNativeHandleFromWasm(
      final WasmFunction wasmFunction, final JniStore store) throws WasmException {
    if (wasmFunction == null) {
      throw new WasmException(
          "Failed to create native function reference: WebAssembly function cannot be null");
    }
    if (store == null) {
      throw new WasmException("Failed to create native function reference: Store cannot be null");
    }

    if (!(wasmFunction instanceof JniFunction)) {
      throw new WasmException(
          "Failed to create native function reference: WebAssembly function must be a JNI"
              + " function");
    }
    final JniFunction jniFunction = (JniFunction) wasmFunction;

    final long functionReferenceId = NEXT_FUNCTION_REFERENCE_ID.getAndIncrement();
    final long nativeHandle =
        nativeCreateFunctionReferenceFromWasm(
            store.getNativeHandle(), jniFunction.getNativeHandle(), functionReferenceId);

    if (nativeHandle == 0) {
      throw new WasmException(
          "Failed to create native function reference from WebAssembly function");
    }

    return new FunctionReferenceHandle(nativeHandle, functionReferenceId);
  }

  /**
   * Creates a new function reference from a host function.
   *
   * @param hostFunction the host function implementation
   * @param functionType the WebAssembly function type signature
   * @param store the store this function reference belongs to
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  JniFunctionReference(
      final HostFunction hostFunction, final FunctionType functionType, final JniStore store)
      throws WasmException {
    this(
        createNativeHandleFromHost(hostFunction, functionType, store),
        "host_function_" + NEXT_FUNCTION_REFERENCE_ID.get(),
        functionType,
        hostFunction,
        null,
        store);
  }

  /**
   * Creates a new function reference from a WebAssembly function.
   *
   * @param wasmFunction the WebAssembly function
   * @param store the store this function reference belongs to
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  JniFunctionReference(final WasmFunction wasmFunction, final JniStore store) throws WasmException {
    this(
        createNativeHandleFromWasm(wasmFunction, store),
        wasmFunction.getName() != null
            ? wasmFunction.getName()
            : "wasm_function_" + NEXT_FUNCTION_REFERENCE_ID.get(),
        wasmFunction.getFunctionType(),
        null,
        wasmFunction,
        store);
  }

  private JniFunctionReference(
      final FunctionReferenceHandle handle,
      final String functionName,
      final FunctionType functionType,
      final HostFunction hostFunction,
      final WasmFunction wasmFunction,
      final JniStore store)
      throws WasmException {
    super(handle.nativeHandle);

    this.functionReferenceId = handle.functionReferenceId;
    this.functionName = Objects.requireNonNull(functionName, "Function name cannot be null");
    this.functionType = Objects.requireNonNull(functionType, "Function type cannot be null");
    this.hostFunction = hostFunction;
    this.wasmFunction = wasmFunction;
    this.storeRef = new WeakReference<>(Objects.requireNonNull(store, "Store cannot be null"));

    try {
      // Register this function reference to prevent GC
      FUNCTION_REFERENCE_REGISTRY.put(functionReferenceId, this);

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            "Created function reference '"
                + functionName
                + "' with ID: "
                + functionReferenceId
                + ", handle: 0x"
                + Long.toHexString(nativeHandle));
      }
    } catch (Exception e) {
      // Clean up registry on failure
      FUNCTION_REFERENCE_REGISTRY.remove(functionReferenceId);
      throw new WasmException("Failed to create function reference: " + functionName, e);
    }
  }

  @Override
  public FunctionType getFunctionType() {
    return functionType;
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    beginOperation();
    try {
      if (hostFunction != null) {
        // Direct call to host function
        return hostFunction.execute(params);
      } else if (wasmFunction != null) {
        // Delegate to WebAssembly function
        return wasmFunction.call(params);
      } else {
        // Call through native function reference
        return callNative(params);
      }
    } finally {
      endOperation();
    }
  }

  /**
   * Calls the function reference through native code.
   *
   * @param params the parameters to pass to the function
   * @return the results from the function
   * @throws WasmException if the call fails
   */
  private WasmValue[] callNative(final WasmValue[] params) throws WasmException {
    final JniStore store = storeRef.get();
    if (store == null) {
      throw new WasmException(
          "Store has been garbage collected for function reference: " + functionName);
    }
    if (store.isClosed()) {
      throw new WasmException("Store is closed for function reference: " + functionName);
    }

    try {
      // Marshal parameters to native format
      final byte[] paramData = JniTypeConverter.marshalParameters(params);

      // Allocate result buffer
      final int resultCount = functionType.getReturnCount();
      final byte[] resultBuffer = new byte[resultCount * 16]; // 16 bytes per value (worst case)

      // Call native function with store handle for access to the store context
      final int result =
          nativeCallFunctionReference(
              getNativeHandle(), store.getNativeHandle(), paramData, resultBuffer);
      if (result != 0) {
        throw new WasmException("Native function reference call failed with code: " + result);
      }

      // Unmarshal results from native format
      return JniTypeConverter.unmarshalResults(resultBuffer, functionType.getReturnTypes());

    } catch (WasmException e) {
      throw e;
    } catch (Exception e) {
      throw new WasmException("Failed to call function reference: " + functionName, e);
    }
  }

  @Override
  public String getName() {
    return functionName;
  }

  @Override
  public boolean isValid() {
    return !isClosed() && getNativeHandle() != 0;
  }

  @Override
  public long getId() {
    return functionReferenceId;
  }

  /**
   * Checks if this function reference is a host function.
   *
   * @return true if this is a host function reference
   */
  public boolean isHostFunction() {
    return hostFunction != null;
  }

  /**
   * Checks if this function reference is a WebAssembly function.
   *
   * @return true if this is a WebAssembly function reference
   */
  public boolean isWasmFunction() {
    return wasmFunction != null;
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, FunctionReferences are owned by the Store. Destroying a FunctionReference
   * while the Store still exists can corrupt the Store's internal slab state. We mark the
   * FunctionReference as closed and remove from registry but don't destroy native resources.
   */
  @Override
  protected void doClose() throws Exception {
    // Remove from registry to prevent further use
    FUNCTION_REFERENCE_REGISTRY.remove(functionReferenceId);

    // Note: Do NOT call nativeDestroyFunctionReference here. FunctionReferences are Store-owned.
    // The Store will clean up all its FunctionReferences when it is destroyed.
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "Function reference '"
              + functionName
              + "' marked as closed (ID: "
              + functionReferenceId
              + "). Native resources freed with Store.");
    }
  }

  @Override
  protected String getResourceType() {
    return "FunctionReference";
  }

  /**
   * Static callback method invoked by native code for host function references.
   *
   * @param functionReferenceId the ID of the function reference to invoke
   * @param paramsData the serialized parameter data from WebAssembly
   * @param resultsBuffer buffer to write results to
   * @return 0 on success, error code on failure
   */
  @SuppressWarnings("unused") // Called by native code
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called by native code through JNI")
  private static int functionReferenceCallback(
      final long functionReferenceId, final byte[] paramsData, final byte[] resultsBuffer) {
    final JniFunctionReference functionReference =
        FUNCTION_REFERENCE_REGISTRY.get(functionReferenceId);
    if (functionReference == null) {
      LOGGER.severe("Function reference not found in registry: " + functionReferenceId);
      return -1;
    }

    try {
      if (functionReference.isClosed()) {
        LOGGER.warning(
            "Attempted to call closed function reference: " + functionReference.functionName);
        return -2;
      }

      if (functionReference.hostFunction == null) {
        LOGGER.severe(
            "Function reference callback called on non-host function: "
                + functionReference.functionName);
        return -3;
      }

      // Unmarshal parameters from native format
      final WasmValue[] wasmParams =
          JniTypeConverter.unmarshalParameters(
              paramsData, functionReference.functionType.getParamTypes());

      // Invoke the host function
      final WasmValue[] wasmResults = functionReference.hostFunction.execute(wasmParams);

      // Validate result types match function signature
      final int expectedReturnCount = functionReference.functionType.getReturnCount();
      if (wasmResults.length != expectedReturnCount) {
        LOGGER.severe(
            "Function reference '"
                + functionReference.functionName
                + "' returned "
                + wasmResults.length
                + " values, expected "
                + expectedReturnCount);
        return -4;
      }

      // Marshal results back to native format
      JniTypeConverter.marshalResults(wasmResults, resultsBuffer);
      return 0;

    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE,
          "Error in function reference callback: " + functionReference.functionName,
          e);
      return -5;
    }
  }

  /**
   * Returns the Rust reference registry ID for this function reference.
   *
   * <p>This method is used when storing function references in globals or tables. It dereferences
   * the native handle (which is a pointer to a boxed u64) to return the actual Rust registry ID
   * used to look up the function in the Rust reference registry.
   *
   * @return the Rust registry ID as a long value
   */
  public long longValue() {
    return nativeGetRegistryId(getNativeHandle());
  }

  /**
   * Gets the current registry statistics for debugging.
   *
   * @return array containing [count, nextId]
   */
  static long[] getRegistryStats() {
    return new long[] {FUNCTION_REFERENCE_REGISTRY.size(), NEXT_FUNCTION_REFERENCE_ID.get()};
  }

  /**
   * Retrieves a function reference from the registry by ID.
   *
   * @param functionReferenceId the function reference ID
   * @return the function reference, or null if not found
   */
  static JniFunctionReference getFromRegistry(final long functionReferenceId) {
    return FUNCTION_REFERENCE_REGISTRY.get(functionReferenceId);
  }

  /**
   * Invokes a function reference callback from native code (called via JNI).
   *
   * <p>This method is called by the Rust JNI layer when a FunctionReference created from a host
   * function is invoked. It looks up the function reference in the registry and executes the
   * associated host function.
   *
   * @param functionReferenceId the function reference ID
   * @param params array of parameter values
   * @return array of return values
   * @throws WasmException if the callback fails
   */
  @SuppressWarnings("unused") // Called by native code
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called by native code through JNI")
  private static WasmValue[] invokeFunctionReferenceCallback(
      final long functionReferenceId, final WasmValue[] params) throws WasmException {
    final JniFunctionReference functionReference =
        FUNCTION_REFERENCE_REGISTRY.get(functionReferenceId);
    if (functionReference == null) {
      LOGGER.severe("Function reference not found in registry: " + functionReferenceId);
      throw new WasmException("Function reference not found: " + functionReferenceId);
    }

    if (functionReference.isClosed()) {
      LOGGER.warning(
          "Attempted to call closed function reference: " + functionReference.functionName);
      throw new WasmException(
          "Function reference has been closed: " + functionReference.functionName);
    }

    if (functionReference.hostFunction == null) {
      LOGGER.severe(
          "Function reference callback called on non-host function: "
              + functionReference.functionName);
      throw new WasmException(
          "Function reference is not a host function: " + functionReference.functionName);
    }

    try {
      LOGGER.fine(
          "Executing function reference callback: "
              + functionReference.functionName
              + " (ID: "
              + functionReferenceId
              + ")");
      return functionReference.hostFunction.execute(params);
    } catch (final Exception e) {
      LOGGER.severe(
          "Function reference execution failed: "
              + functionReference.functionName
              + " - "
              + e.getMessage());
      throw new WasmException(
          "Function reference execution failed: " + functionReference.functionName, e);
    }
  }

  // Native method declarations

  /**
   * Creates a new native function reference from a host function.
   *
   * @param storeHandle the native store handle
   * @param functionTypeData marshalled function type data
   * @param functionReferenceId the function reference callback ID
   * @return the native function reference handle, or 0 on failure
   */
  private static native long nativeCreateFunctionReferenceFromHost(
      long storeHandle, byte[] functionTypeData, long functionReferenceId);

  /**
   * Creates a new native function reference from a WebAssembly function.
   *
   * @param storeHandle the native store handle
   * @param wasmFunctionHandle the native WebAssembly function handle
   * @param functionReferenceId the function reference ID
   * @return the native function reference handle, or 0 on failure
   */
  private static native long nativeCreateFunctionReferenceFromWasm(
      long storeHandle, long wasmFunctionHandle, long functionReferenceId);

  /**
   * Dereferences the boxed native handle to return the Rust reference registry ID.
   *
   * <p>The native handle is a pointer to a heap-allocated Box&lt;u64&gt; containing the registry
   * ID. This method reads the u64 value from the box.
   *
   * @param nativeHandle the native function reference handle (pointer to boxed registry ID)
   * @return the Rust reference registry ID
   */
  private static native long nativeGetRegistryId(long nativeHandle);

  /**
   * Calls a function reference through native code.
   *
   * @param functionReferenceHandle the native function reference handle (registry ID)
   * @param storeHandle the native store handle for function call context
   * @param paramsData marshalled parameter data
   * @param resultsBuffer buffer to write results to
   * @return 0 on success, error code on failure
   */
  private static native int nativeCallFunctionReference(
      long functionReferenceHandle, long storeHandle, byte[] paramsData, byte[] resultsBuffer);

  @Override
  public String toString() {
    if (!tryBeginOperation()) {
      return "JniFunctionReference{name='" + functionName + "', closed=true}";
    }

    try {
      final String type = isHostFunction() ? "host" : (isWasmFunction() ? "wasm" : "native");
      return String.format(
          "JniFunctionReference{name='%s', type=%s, functionType=%s, id=%d, handle=0x%x}",
          functionName, type, functionType, functionReferenceId, getNativeHandle());
    } finally {
      endOperation();
    }
  }
}
