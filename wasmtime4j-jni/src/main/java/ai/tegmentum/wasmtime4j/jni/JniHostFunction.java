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
import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniTypeConverter;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation for host functions that can be called from WebAssembly.
 *
 * <p>This class provides the JNI bridge for host functions, allowing Java code to expose functions
 * that can be imported and called by WebAssembly modules. It handles parameter marshalling, type
 * validation, and callback management with comprehensive defensive programming practices.
 *
 * <p>The implementation uses a registry pattern to track active host functions and prevent
 * premature garbage collection, ensuring memory safety across the JNI boundary.
 *
 * <p>Key Features:
 *
 * <ul>
 *   <li>Automatic parameter and return value marshalling
 *   <li>Type safety validation for WebAssembly function signatures
 *   <li>Exception propagation from Java to WebAssembly traps
 *   <li>Resource lifecycle management with automatic cleanup
 *   <li>Thread-safe callback registry to prevent GC issues
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * // Create a simple host function
 * HostFunction addFunction = (params) -> {
 *     int a = params[0].asInt();
 *     int b = params[1].asInt();
 *     return new WasmValue[] { WasmValue.i32(a + b) };
 * };
 *
 * // Register with store
 * WasmFunction func = store.createHostFunction("add", functionType, addFunction);
 *
 * // Add to import map
 * ImportMap imports = ImportMap.empty();
 * imports.addFunction("env", "add", func);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class JniHostFunction extends JniResource implements WasmFunction {
  private static final Logger LOGGER = Logger.getLogger(JniHostFunction.class.getName());

  // Array-based registry for host function callbacks to prevent GC.
  // Uses AtomicReferenceArray indexed by sequential host function ID for O(1) lookup
  // without Long autoboxing overhead on every callback dispatch.
  private static final java.util.concurrent.atomic.AtomicReference<
          java.util.concurrent.atomic.AtomicReferenceArray<JniHostFunction>>
      HOST_FUNCTION_REGISTRY =
          new java.util.concurrent.atomic.AtomicReference<>(
              new java.util.concurrent.atomic.AtomicReferenceArray<>(256));
  private static final Object REGISTRY_GROW_LOCK = new Object();
  private static final AtomicLong NEXT_HOST_FUNCTION_ID = new AtomicLong(1L);

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniHostFunction: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long hostFunctionId;
  private final String functionName;
  private final FunctionType functionType;
  private final HostFunction implementation;
  private final WeakReference<JniStore> storeRef;

  /** Cached param types to avoid cloning on every callback invocation. */
  private final ai.tegmentum.wasmtime4j.WasmValueType[] cachedParamTypes;

  /** Helper class to hold both the native handle and host function ID. */
  private static class HostFunctionHandle {
    final long nativeHandle;
    final long hostFunctionId;

    HostFunctionHandle(final long nativeHandle, final long hostFunctionId) {
      this.nativeHandle = nativeHandle;
      this.hostFunctionId = hostFunctionId;
    }
  }

  /**
   * Creates the native handle for a host function.
   *
   * @param functionName the name of the function
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @param store the store this host function belongs to
   * @return the host function handle containing native handle and ID
   * @throws WasmException if creation fails or any parameter is null
   */
  private static HostFunctionHandle createNativeHandle(
      final String functionName,
      final FunctionType functionType,
      final HostFunction implementation,
      final JniStore store)
      throws WasmException {
    // Validate all parameters first
    if (functionName == null) {
      throw new WasmException(
          "Failed to create native host function: Function name cannot be null");
    }
    if (functionType == null) {
      throw new WasmException(
          "Failed to create native host function: Function type cannot be null");
    }
    if (implementation == null) {
      throw new WasmException(
          "Failed to create native host function: Implementation cannot be null");
    }
    if (store == null) {
      throw new WasmException("Failed to create native host function: Store cannot be null");
    }

    final long hostFunctionId = NEXT_HOST_FUNCTION_ID.getAndIncrement();
    final long nativeHandle =
        nativeCreateHostFunction(
            store.getNativeHandle(),
            functionName,
            JniTypeConverter.marshalFunctionType(functionType),
            hostFunctionId);

    if (nativeHandle == 0) {
      throw new WasmException("Failed to create native host function: " + functionName);
    }

    return new HostFunctionHandle(nativeHandle, hostFunctionId);
  }

  /**
   * Creates the native handle for an unchecked host function.
   *
   * @param functionName the name of the function
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @param store the store this host function belongs to
   * @return the host function handle containing native handle and ID
   * @throws WasmException if creation fails or any parameter is null
   */
  private static HostFunctionHandle createNativeHandleUnchecked(
      final String functionName,
      final FunctionType functionType,
      final HostFunction implementation,
      final JniStore store)
      throws WasmException {
    if (functionName == null) {
      throw new WasmException(
          "Failed to create native host function: Function name cannot be null");
    }
    if (functionType == null) {
      throw new WasmException(
          "Failed to create native host function: Function type cannot be null");
    }
    if (implementation == null) {
      throw new WasmException(
          "Failed to create native host function: Implementation cannot be null");
    }
    if (store == null) {
      throw new WasmException("Failed to create native host function: Store cannot be null");
    }

    final long hostFunctionId = NEXT_HOST_FUNCTION_ID.getAndIncrement();
    final long nativeHandle =
        nativeCreateHostFunctionUnchecked(
            store.getNativeHandle(),
            functionName,
            JniTypeConverter.marshalFunctionType(functionType),
            hostFunctionId);

    if (nativeHandle == 0) {
      throw new WasmException("Failed to create native unchecked host function: " + functionName);
    }

    return new HostFunctionHandle(nativeHandle, hostFunctionId);
  }

  /**
   * Creates a new host function with the specified signature and implementation.
   *
   * @param functionName the name of the function (for debugging/logging)
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @param store the store this host function belongs to
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  JniHostFunction(
      final String functionName,
      final FunctionType functionType,
      final HostFunction implementation,
      final JniStore store)
      throws WasmException {
    this(
        createNativeHandle(functionName, functionType, implementation, store),
        functionName,
        functionType,
        implementation,
        store);
  }

  /**
   * Creates a new unchecked host function that skips per-call type validation.
   *
   * @param functionName the name of the function (for debugging/logging)
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @param store the store this host function belongs to
   * @return the created unchecked host function
   * @throws WasmException if host function creation fails
   */
  static JniHostFunction createUnchecked(
      final String functionName,
      final FunctionType functionType,
      final HostFunction implementation,
      final JniStore store)
      throws WasmException {
    return new JniHostFunction(
        createNativeHandleUnchecked(functionName, functionType, implementation, store),
        functionName,
        functionType,
        implementation,
        store);
  }

  private JniHostFunction(
      final HostFunctionHandle handle,
      final String functionName,
      final FunctionType functionType,
      final HostFunction implementation,
      final JniStore store)
      throws WasmException {
    super(handle.nativeHandle);

    this.hostFunctionId = handle.hostFunctionId;
    this.functionName = Objects.requireNonNull(functionName, "Function name cannot be null");
    this.functionType = Objects.requireNonNull(functionType, "Function type cannot be null");
    this.implementation = Objects.requireNonNull(implementation, "Implementation cannot be null");
    this.storeRef = new WeakReference<>(Objects.requireNonNull(store, "Store cannot be null"));
    this.cachedParamTypes = functionType.getParamTypes();

    try {
      // Register this host function to prevent GC
      registryPut((int) hostFunctionId, this);

      // Register with JniLinker's callback map so Rust can invoke this function
      // via JniLinker.invokeHostFunctionCallback when called from WASM
      JniLinker.registerHostFunctionCallbackWithId(
          hostFunctionId, null, functionName, implementation);

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            "Created host function '"
                + functionName
                + "' with ID: "
                + hostFunctionId
                + ", handle: 0x"
                + Long.toHexString(nativeHandle));
      }
    } catch (Exception e) {
      // Clean up registry on failure
      registryRemove((int) hostFunctionId);
      JniLinker.unregisterHostFunctionCallback(hostFunctionId);
      throw new WasmException("Failed to create host function: " + functionName, e);
    }
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    // Host functions are called FROM WebAssembly, not TO WebAssembly
    // This method shouldn't be used directly for host functions
    throw new ai.tegmentum.wasmtime4j.exception.ValidationException(
        "Host functions are called from WebAssembly, not directly from Java. "
            + "Use the callback mechanism instead.");
  }

  @Override
  public FunctionType getFunctionType() {
    return functionType;
  }

  @Override
  public String getName() {
    return functionName;
  }

  /**
   * Gets the unique ID for this host function.
   *
   * @return the host function ID
   */
  public long getHostFunctionId() {
    return hostFunctionId;
  }

  /**
   * Checks if this host function is still valid for use.
   *
   * @return true if the host function is valid
   */
  public boolean isValid() {
    return !isClosed() && getNativeHandle() != 0;
  }

  @Override
  public long toRawFuncRef() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (isClosed() || getNativeHandle() == 0) {
      throw new IllegalStateException("Host function is not valid");
    }
    final java.lang.ref.WeakReference<JniStore> storeRef = this.storeRef;
    if (storeRef == null || storeRef.get() == null) {
      throw new IllegalStateException("Store reference is null or garbage collected");
    }
    try {
      return JniFunction.nativeFuncToRaw(getNativeHandle(), storeRef.get().getNativeHandle());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to convert host function to raw funcref", e);
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
    // Host functions are called FROM WebAssembly, not TO WebAssembly
    // This method shouldn't be used directly for host functions
    final java.util.concurrent.CompletableFuture<WasmValue[]> result =
        new java.util.concurrent.CompletableFuture<>();
    result.completeExceptionally(
        new ai.tegmentum.wasmtime4j.exception.ValidationException(
            "Host functions are called from WebAssembly, not directly from Java. "
                + "Use the callback mechanism instead."));
    return result;
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, HostFunctions are owned by the Store. Destroying a HostFunction while the
   * Store still exists can corrupt the Store's internal slab state. We mark the HostFunction as
   * closed and remove from registry but don't destroy native resources - the Store handles that.
   */
  @Override
  protected void doClose() throws Exception {
    // Remove from registry to prevent further callbacks
    registryRemove((int) hostFunctionId);

    // Remove from JniLinker's callback map
    JniLinker.unregisterHostFunctionCallback(hostFunctionId);

    // Note: Do NOT call nativeDestroyHostFunction here. HostFunctions are Store-owned resources.
    // The Store will clean up all its HostFunctions when it is destroyed.
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "Host function '"
              + functionName
              + "' marked as closed (ID: "
              + hostFunctionId
              + "). Native resources freed with Store.");
    }
  }

  @Override
  protected String getResourceType() {
    return "HostFunction";
  }

  /**
   * Static callback method invoked by native code.
   *
   * <p>This method serves as the bridge between native WebAssembly execution and Java host function
   * implementation. It marshals parameters, invokes the callback, and marshals results back to
   * native code.
   *
   * @param hostFunctionId the ID of the host function to invoke
   * @param callerHandle the native caller context handle (0 if not available)
   * @param paramsData the serialized parameter data from WebAssembly
   * @param resultsBuffer buffer to write results to
   * @return 0 on success, error code on failure
   */
  @SuppressWarnings("unused") // Called by native code
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called by native code through JNI")
  private static int hostFunctionCallback(
      final long hostFunctionId,
      final long callerHandle,
      final byte[] paramsData,
      final byte[] resultsBuffer) {
    final JniHostFunction hostFunction = registryGet(hostFunctionId);
    if (hostFunction == null) {
      LOGGER.severe("Host function not found in registry: " + hostFunctionId);
      return -1;
    }

    try {
      if (hostFunction.isClosed()) {
        LOGGER.warning("Attempted to call closed host function: " + hostFunction.functionName);
        return -2;
      }

      // Unmarshal parameters from native format using cached param types
      final WasmValue[] wasmParams =
          JniTypeConverter.unmarshalParameters(paramsData, hostFunction.cachedParamTypes);

      // Invoke the Java callback
      final WasmValue[] wasmResults;

      // Check if this is a caller-aware host function
      if (hostFunction.implementation instanceof HostFunction.CallerAwareHostFunction) {
        // Create caller context if available
        if (callerHandle != 0 && hostFunction.storeRef.get() != null) {
          final JniCaller<?> caller = new JniCaller<>(callerHandle, hostFunction.storeRef.get());

          // Set the caller in a thread-local for CallerAwareHostFunction to access
          CALLER_CONTEXT.set(caller);
          try {
            wasmResults = hostFunction.implementation.execute(wasmParams);
          } finally {
            CALLER_CONTEXT.remove();
          }
        } else {
          // Caller context not available, execute without it
          LOGGER.warning(
              "Caller context requested but not available for function: "
                  + hostFunction.functionName);
          wasmResults = hostFunction.implementation.execute(wasmParams);
        }
      } else {
        // Regular host function without caller context
        wasmResults = hostFunction.implementation.execute(wasmParams);
      }

      // Validate result types match function signature
      final int expectedReturnCount = hostFunction.functionType.getReturnCount();
      if (wasmResults.length != expectedReturnCount) {
        LOGGER.severe(
            "Host function '"
                + hostFunction.functionName
                + "' returned "
                + wasmResults.length
                + " values, expected "
                + expectedReturnCount);
        return -3;
      }

      // Marshal results back to native format
      JniTypeConverter.marshalResults(wasmResults, resultsBuffer);
      return 0;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error in host function callback: " + hostFunction.functionName, e);
      return -4;
    }
  }

  // Thread-local storage for caller context
  private static final ThreadLocal<JniCaller<?>> CALLER_CONTEXT = new ThreadLocal<>();

  /**
   * Gets the current caller context for the executing host function.
   *
   * <p>This method is called by JniCallerContextProvider (via CallerAwareHostFunction) to access
   * the caller during execution.
   *
   * @param <T> the type of user data
   * @return the current caller context
   * @throws UnsupportedOperationException if no caller context is available
   */
  @SuppressWarnings("unchecked")
  static <T> Caller<T> getCurrentCaller() {
    final JniCaller<?> caller = CALLER_CONTEXT.get();
    if (caller == null) {
      throw new UnsupportedOperationException(
          "Caller context not available - this should be provided by the runtime");
    }
    return (JniCaller<T>) caller;
  }

  /**
   * Gets the current registry statistics for debugging.
   *
   * @return array containing [count, nextId]
   */
  static long[] getRegistryStats() {
    return new long[] {registrySize(), NEXT_HOST_FUNCTION_ID.get()};
  }

  /**
   * Retrieves a host function from the registry by ID.
   *
   * @param hostFunctionId the host function ID
   * @return the host function, or null if not found
   */
  static JniHostFunction getFromRegistry(final long hostFunctionId) {
    return registryGet(hostFunctionId);
  }

  // =============================================================================
  // Array-based registry operations (eliminates Long autoboxing on lookup)
  // =============================================================================

  private static void registryPut(final int idx, final JniHostFunction func) {
    java.util.concurrent.atomic.AtomicReferenceArray<JniHostFunction> arr =
        HOST_FUNCTION_REGISTRY.get();
    if (idx >= arr.length()) {
      synchronized (REGISTRY_GROW_LOCK) {
        arr = HOST_FUNCTION_REGISTRY.get();
        if (idx >= arr.length()) {
          final int newLen = Math.max(idx + 1, arr.length() * 2);
          final java.util.concurrent.atomic.AtomicReferenceArray<JniHostFunction> newArr =
              new java.util.concurrent.atomic.AtomicReferenceArray<>(newLen);
          for (int i = 0; i < arr.length(); i++) {
            newArr.set(i, arr.get(i));
          }
          HOST_FUNCTION_REGISTRY.set(newArr);
          arr = newArr;
        }
      }
    }
    arr.set(idx, func);
  }

  private static JniHostFunction registryGet(final long id) {
    final int idx = (int) id;
    final java.util.concurrent.atomic.AtomicReferenceArray<JniHostFunction> arr =
        HOST_FUNCTION_REGISTRY.get();
    if (idx >= 0 && idx < arr.length()) {
      return arr.get(idx);
    }
    return null;
  }

  private static void registryRemove(final int idx) {
    final java.util.concurrent.atomic.AtomicReferenceArray<JniHostFunction> arr =
        HOST_FUNCTION_REGISTRY.get();
    if (idx >= 0 && idx < arr.length()) {
      arr.set(idx, null);
    }
  }

  private static int registrySize() {
    final java.util.concurrent.atomic.AtomicReferenceArray<JniHostFunction> arr =
        HOST_FUNCTION_REGISTRY.get();
    int count = 0;
    for (int i = 0; i < arr.length(); i++) {
      if (arr.get(i) != null) {
        count++;
      }
    }
    return count;
  }

  // Native method declarations

  /**
   * Creates a new native host function.
   *
   * @param storeHandle the native store handle
   * @param functionName the function name
   * @param functionTypeData marshalled function type data
   * @param hostFunctionId the host function callback ID
   * @return the native host function handle, or 0 on failure
   */
  private static native long nativeCreateHostFunction(
      long storeHandle, String functionName, byte[] functionTypeData, long hostFunctionId);

  /**
   * Creates a native unchecked host function handle that skips per-call type validation.
   *
   * @param storeHandle the native store handle
   * @param functionName the function name
   * @param functionTypeData marshalled function type data
   * @param hostFunctionId the host function callback ID
   * @return the native host function handle, or 0 on failure
   */
  private static native long nativeCreateHostFunctionUnchecked(
      long storeHandle, String functionName, byte[] functionTypeData, long hostFunctionId);

  /**
   * Destroys a native host function and releases all associated resources.
   *
   * @param hostFunctionHandle the native host function handle
   */
  private static native void nativeDestroyHostFunction(long hostFunctionHandle);

  @Override
  public String toString() {
    if (!tryBeginOperation()) {
      return "JniHostFunction{name='" + functionName + "', closed=true}";
    }

    try {
      return String.format(
          "JniHostFunction{name='%s', type=%s, id=%d, handle=0x%x}",
          functionName, functionType, hostFunctionId, getNativeHandle());
    } finally {
      endOperation();
    }
  }
}
