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
import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation for host functions that can be called from WebAssembly.
 *
 * <p>This class provides the foundation for callback upcall handles, allowing Java code to expose
 * functions that can be imported and called by WebAssembly modules. It uses Panama's upcall stub
 * mechanism for optimal performance while maintaining type safety and error handling.
 *
 * <p>The implementation uses a registry pattern to track active host functions and prevent
 * premature garbage collection of callback stubs, ensuring memory safety across the FFI boundary.
 *
 * @since 1.0.0
 */
public final class PanamaHostFunction implements WasmFunction {
  private static final Logger logger = Logger.getLogger(PanamaHostFunction.class.getName());

  // Global registry for host function callbacks to prevent GC
  private static final ConcurrentHashMap<Long, PanamaHostFunction> hostFunctionRegistry =
      new ConcurrentHashMap<>();
  private static final AtomicLong nextHostFunctionId = new AtomicLong(1L);

  // Thread-local storage for caller context
  private static final ThreadLocal<PanamaCaller<?>> CALLER_CONTEXT = new ThreadLocal<>();

  private final long hostFunctionId;
  private final String functionName;
  private final FunctionType functionType;
  private final HostFunctionCallback callback;
  private final ai.tegmentum.wasmtime4j.func.HostFunction implementation;
  private final WeakReference<PanamaStore> storeRef;
  private final ArenaResourceManager arenaManager;
  private MemorySegment functionHandle;
  private MemorySegment upcallStub;
  private MemorySegment ffiUpcallStub; // FFI-compatible upcall stub for native registry
  private long funcRefId = 0L; // Native reference registry ID for table operations
  private final boolean unchecked; // Whether to use Func::new_unchecked for better performance
  private final NativeResourceHandle resourceHandle;

  /**
   * Functional interface for host function implementations.
   *
   * <p>Host functions implement this interface to provide custom logic that can be called from
   * WebAssembly modules. The interface uses WasmValue arrays for type-safe parameter passing.
   */
  @FunctionalInterface
  public interface HostFunctionCallback {
    /**
     * Executes the host function with the given parameters.
     *
     * @param params the parameters passed from WebAssembly
     * @return the results to return to WebAssembly
     * @throws WasmException if the function execution fails
     */
    WasmValue[] execute(WasmValue[] params) throws WasmException;
  }

  /**
   * Creates a new host function with the specified signature and implementation.
   *
   * @param functionName the name of the function (for debugging/logging)
   * @param functionType the WebAssembly function type signature
   * @param callback the Java implementation of the function
   * @param implementation the original HostFunction implementation (may be null for direct
   *     callbacks)
   * @param store the store this host function belongs to (may be null for direct callbacks)
   * @param arenaManager the arena resource manager for memory lifecycle
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  public PanamaHostFunction(
      final String functionName,
      final FunctionType functionType,
      final HostFunctionCallback callback,
      final ai.tegmentum.wasmtime4j.func.HostFunction implementation,
      final PanamaStore store,
      final ArenaResourceManager arenaManager)
      throws WasmException {
    this(functionName, functionType, callback, implementation, store, arenaManager, false);
  }

  /**
   * Creates a new host function with the specified signature and implementation.
   *
   * @param functionName the name of the function (for debugging/logging)
   * @param functionType the WebAssembly function type signature
   * @param callback the Java implementation of the function
   * @param implementation the original HostFunction implementation (may be null for direct
   *     callbacks)
   * @param store the store this host function belongs to (may be null for direct callbacks)
   * @param arenaManager the arena resource manager for memory lifecycle
   * @param unchecked if true, uses Func::new_unchecked for better performance
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  public PanamaHostFunction(
      final String functionName,
      final FunctionType functionType,
      final HostFunctionCallback callback,
      final ai.tegmentum.wasmtime4j.func.HostFunction implementation,
      final PanamaStore store,
      final ArenaResourceManager arenaManager,
      final boolean unchecked)
      throws WasmException {
    this.hostFunctionId = nextHostFunctionId.getAndIncrement();
    this.functionName = Objects.requireNonNull(functionName, "Function name cannot be null");
    this.functionType = Objects.requireNonNull(functionType, "Function type cannot be null");
    this.callback = Objects.requireNonNull(callback, "Callback cannot be null");
    this.implementation = implementation; // May be null
    this.storeRef = store != null ? new WeakReference<>(store) : null;
    this.arenaManager = Objects.requireNonNull(arenaManager, "Arena manager cannot be null");
    this.unchecked = unchecked;

    try {
      // Register this host function to prevent GC
      hostFunctionRegistry.put(hostFunctionId, this);

      // Create the upcall stub for native-to-Java callback
      createUpcallStub();

      // Register the function in the native registry for table operations
      if (store != null) {
        registerInNativeRegistry(store);
      }

      // Register for automatic resource cleanup
      arenaManager.registerManagedNativeResource(this, upcallStub, this::closeNative);

      // Initialize resource handle with cleanup logic
      final long capturedHostFunctionId = this.hostFunctionId;
      final long capturedFuncRefId = this.funcRefId;
      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaHostFunction",
              () -> {
                // Remove from global registry
                hostFunctionRegistry.remove(hostFunctionId);

                // Unregister from native reference registry
                if (funcRefId != 0) {
                  try {
                    final MethodHandle destroyHandle =
                        NativeInstanceBindings.getInstance().getPanamaDestroyHostFunction();
                    if (destroyHandle != null) {
                      destroyHandle.invoke(funcRefId);
                    }
                  } catch (final Throwable t) {
                    throw new Exception(
                        "Failed to unregister host function from native registry", t);
                  }
                }

                // Unregister from arena manager - this will call closeNative()
                arenaManager.unregisterManagedResource(this);

                if (logger.isLoggable(Level.FINE)) {
                  logger.fine(
                      "Closed host function '" + functionName + "' with ID: " + hostFunctionId);
                }
              },
              this,
              () -> {
                hostFunctionRegistry.remove(capturedHostFunctionId);
                if (capturedFuncRefId != 0) {
                  try {
                    final MethodHandle destroyHandle =
                        NativeInstanceBindings.getInstance().getPanamaDestroyHostFunction();
                    if (destroyHandle != null) {
                      destroyHandle.invoke(capturedFuncRefId);
                    }
                  } catch (final Throwable ignored) {
                    // Safety net — best effort
                  }
                }
              });

      if (logger.isLoggable(Level.FINE)) {
        logger.fine(
            "Created host function '"
                + functionName
                + "' with ID: "
                + hostFunctionId
                + ", funcRefId: "
                + funcRefId);
      }
    } catch (Exception e) {
      hostFunctionRegistry.remove(hostFunctionId);
      throw new WasmException("Failed to create host function: " + functionName, e);
    }
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    ensureNotClosed();

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

  @Override
  public long toRawFuncRef() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (functionHandle == null || functionHandle.equals(java.lang.foreign.MemorySegment.NULL)) {
      throw new IllegalStateException("Host function handle is not valid");
    }
    final WeakReference<PanamaStore> ref = this.storeRef;
    if (ref == null) {
      throw new IllegalStateException("Store reference is null");
    }
    final PanamaStore store = ref.get();
    if (store == null) {
      throw new IllegalStateException("Store has been garbage collected");
    }
    try {
      return NativeInstanceBindings.getInstance().funcToRaw(functionHandle, store.getNativeStore());
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
    return java.util.concurrent.CompletableFuture.failedFuture(
        new ai.tegmentum.wasmtime4j.exception.ValidationException(
            "Host functions are called from WebAssembly, not directly from Java. "
                + "Use the callback mechanism instead."));
  }

  /**
   * Gets the native function handle for use in WebAssembly import operations.
   *
   * <p>This handle can be passed to WebAssembly runtime functions to create function imports that
   * reference this host function.
   *
   * @return the native function handle
   * @throws IllegalStateException if the host function has been closed
   */
  public MemorySegment getFunctionHandle() {
    ensureNotClosed();
    return functionHandle;
  }

  /**
   * Gets the native function reference registry ID for table operations.
   *
   * <p>This ID is used to look up the function in the native reference registry when setting table
   * elements.
   *
   * @return the function reference ID, or 0 if not registered
   */
  public long getFuncRefId() {
    return funcRefId;
  }

  /**
   * Gets the upcall stub for direct FFI integration.
   *
   * <p>This method is intended for internal use by Panama FFI components and should not be used by
   * application code.
   *
   * @return the upcall stub memory segment
   * @throws IllegalStateException if the host function has been closed
   */
  public MemorySegment getUpcallStub() {
    ensureNotClosed();
    return upcallStub;
  }

  /**
   * Checks if this host function has been closed.
   *
   * @return true if the host function has been closed
   */
  public boolean isClosed() {
    return resourceHandle.isClosed();
  }

  /** Closes this host function and releases its resources. */
  public void close() throws WasmException {
    resourceHandle.close();
  }

  /**
   * Creates the Panama upcall stub for this host function.
   *
   * <p>The upcall stub provides a native function pointer that can be called from WebAssembly code,
   * which will then invoke the Java callback implementation.
   *
   * @throws WasmException if upcall stub creation fails
   */
  private void createUpcallStub() throws WasmException {
    try {
      // Create method handle for the callback wrapper
      final MethodHandle callbackHandle = createCallbackMethodHandle();

      // Create function descriptor based on WebAssembly function type
      final FunctionDescriptor descriptor = createFunctionDescriptor();

      // Create the upcall stub using Panama's Linker
      final Linker linker = Linker.nativeLinker();
      this.upcallStub = linker.upcallStub(callbackHandle, descriptor, arenaManager.getArena());

      // TODO: Create native function handle using native bindings
      // For now, we'll use the upcall stub directly
      this.functionHandle = upcallStub;

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Created upcall stub for host function: " + functionName);
      }
    } catch (Exception e) {
      throw new WasmException("Failed to create upcall stub for host function: " + functionName, e);
    }
  }

  /**
   * Converts a WasmValueType to its native type code for FFI.
   *
   * @param type the WasmValueType to convert
   * @return the native type code (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FUNCREF, 6=EXTERNREF)
   */
  private static int toNativeTypeCode(final WasmValueType type) {
    return switch (type) {
      case I32 -> 0;
      case I64 -> 1;
      case F32 -> 2;
      case F64 -> 3;
      case V128 -> 4;
      case FUNCREF -> 5;
      case EXTERNREF -> 6;
      case ANYREF -> 7;
      default -> throw new IllegalArgumentException("Unsupported type for native FFI: " + type);
    };
  }

  /**
   * Registers this host function in the native reference registry for table operations.
   *
   * <p>This creates a Wasmtime Func in the native side and registers it so it can be looked up when
   * setting table elements.
   *
   * @param store the store to register the function in
   * @throws WasmException if registration fails
   */
  private void registerInNativeRegistry(final PanamaStore store) throws WasmException {
    final NativeInstanceBindings bindings = NativeInstanceBindings.getInstance();
    final MethodHandle createHandle =
        unchecked
            ? bindings.getPanamaStoreCreateHostFunctionUnchecked()
            : bindings.getPanamaStoreCreateHostFunction();

    if (createHandle == null) {
      logger.warning("Native host function creation not available - table.set() may not work");
      return;
    }

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment storePtr = store.getNativeStore();
      if (storePtr == null || storePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Store has no native handle");
      }

      // Create FFI-compatible upcall stub that matches StoreHostFunctionCallback signature
      createFfiUpcallStub();

      // Build parameter types array
      final WasmValueType[] paramTypes = functionType.getParamTypes();
      final MemorySegment paramTypesSegment =
          tempArena.allocate(ValueLayout.JAVA_INT, paramTypes.length);
      for (int i = 0; i < paramTypes.length; i++) {
        paramTypesSegment.setAtIndex(ValueLayout.JAVA_INT, i, toNativeTypeCode(paramTypes[i]));
      }

      // Build return types array
      final WasmValueType[] returnTypes = functionType.getReturnTypes();
      final MemorySegment returnTypesSegment =
          tempArena.allocate(ValueLayout.JAVA_INT, returnTypes.length);
      for (int i = 0; i < returnTypes.length; i++) {
        returnTypesSegment.setAtIndex(ValueLayout.JAVA_INT, i, toNativeTypeCode(returnTypes[i]));
      }

      // Allocate output pointer for func_ref_id
      final MemorySegment funcRefIdOut = tempArena.allocate(ValueLayout.JAVA_LONG);

      // Call the native function with FFI-compatible upcall stub
      final int result =
          (int)
              createHandle.invoke(
                  storePtr,
                  ffiUpcallStub, // Use FFI-compatible callback, not upcallStub
                  hostFunctionId, // callback_id
                  paramTypesSegment,
                  paramTypes.length,
                  returnTypesSegment,
                  returnTypes.length,
                  funcRefIdOut);

      if (result != 0) {
        logger.warning(
            "Failed to register host function in native registry: "
                + PanamaErrorMapper.getErrorDescription(result));
        return;
      }

      this.funcRefId = funcRefIdOut.get(ValueLayout.JAVA_LONG, 0);
      logger.fine(
          "Registered host function '"
              + functionName
              + "' in native registry with funcRefId: "
              + funcRefId);
    } catch (WasmException e) {
      throw e;
    } catch (Throwable e) {
      throw new WasmException(
          "Failed to register host function in native registry: " + e.getMessage(), e);
    }
  }

  /**
   * Creates an FFI-compatible upcall stub that matches the StoreHostFunctionCallback signature.
   *
   * <p>The native code expects this signature: extern "C" fn( callback_id: i64, params_ptr: *const
   * c_void, params_len: c_uint, results_ptr: *mut c_void, results_len: c_uint, error_message_ptr:
   * *mut c_char, error_message_len: c_uint, ) -> c_int
   *
   * @throws WasmException if upcall stub creation fails
   */
  private void createFfiUpcallStub() throws WasmException {
    try {
      final MethodHandles.Lookup lookup = MethodHandles.lookup();

      // Create method handle for the FFI callback
      final MethodHandle ffiCallbackHandle =
          lookup.findStatic(
              PanamaHostFunction.class,
              "ffiCallback",
              MethodType.methodType(
                  int.class, // return type: c_int
                  long.class, // callback_id: i64
                  MemorySegment.class, // params_ptr: *const c_void
                  int.class, // params_len: c_uint
                  MemorySegment.class, // results_ptr: *mut c_void
                  int.class, // results_len: c_uint
                  MemorySegment.class, // error_message_ptr: *mut c_char
                  int.class // error_message_len: c_uint
                  ));

      // Create function descriptor matching StoreHostFunctionCallback
      final FunctionDescriptor ffiDescriptor =
          FunctionDescriptor.of(
              ValueLayout.JAVA_INT, // return type: c_int
              ValueLayout.JAVA_LONG, // callback_id: i64
              ValueLayout.ADDRESS, // params_ptr: *const c_void
              ValueLayout.JAVA_INT, // params_len: c_uint
              ValueLayout.ADDRESS, // results_ptr: *mut c_void
              ValueLayout.JAVA_INT, // results_len: c_uint
              ValueLayout.ADDRESS, // error_message_ptr: *mut c_char
              ValueLayout.JAVA_INT // error_message_len: c_uint
              );

      // Create the FFI-compatible upcall stub
      final Linker linker = Linker.nativeLinker();
      this.ffiUpcallStub =
          linker.upcallStub(ffiCallbackHandle, ffiDescriptor, arenaManager.getArena());

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Created FFI upcall stub for host function: " + functionName);
      }
    } catch (Exception e) {
      throw new WasmException(
          "Failed to create FFI upcall stub for host function: " + functionName, e);
    }
  }

  /**
   * Static FFI callback method invoked by native code through the FFI upcall stub.
   *
   * <p>This method matches the StoreHostFunctionCallback signature expected by the native code. It
   * receives parameters in FFI format, invokes the Java callback, and marshals results back.
   *
   * @param callbackId the ID of the host function to invoke
   * @param paramsPtr pointer to FFI parameter array
   * @param paramsLen number of parameters
   * @param resultsPtr pointer to FFI results array (output)
   * @param resultsLen expected number of results
   * @param errorMessagePtr pointer to error message buffer (output)
   * @param errorMessageLen size of error message buffer
   * @return 0 on success, non-zero on error
   */
  @SuppressWarnings("unused") // Called via upcall stub
  private static int ffiCallback(
      final long callbackId,
      final MemorySegment paramsPtr,
      final int paramsLen,
      final MemorySegment resultsPtr,
      final int resultsLen,
      final MemorySegment errorMessagePtr,
      final int errorMessageLen) {

    if (logger.isLoggable(Level.FINE)) {
      logger.fine(
          "FFI callback entered: callbackId="
              + callbackId
              + ", paramsLen="
              + paramsLen
              + ", resultsLen="
              + resultsLen);
    }

    final PanamaHostFunction hostFunction = hostFunctionRegistry.get(callbackId);
    if (hostFunction == null) {
      PanamaErrorMapper.writeErrorMessage(
          errorMessagePtr, errorMessageLen, "Host function not found: " + callbackId);
      return 1;
    }

    try {
      if (hostFunction.resourceHandle.isClosed()) {
        PanamaErrorMapper.writeErrorMessage(
            errorMessagePtr, errorMessageLen, "Host function closed: " + hostFunction.functionName);
        return 2;
      }

      // Unmarshal FFI parameters to WasmValue array
      final WasmValue[] wasmParams = unmarshalFfiParams(paramsPtr, paramsLen, hostFunction);

      // Invoke the Java callback
      final WasmValue[] wasmResults = hostFunction.callback.execute(wasmParams);

      // Marshal results back to FFI format
      marshalFfiResults(resultsPtr, resultsLen, wasmResults, hostFunction);

      if (logger.isLoggable(Level.FINE)) {
        logger.fine(
            "FFI callback completed successfully for: "
                + hostFunction.functionName
                + ", returned "
                + wasmResults.length
                + " results");
      }

      return 0; // Success
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error in FFI callback for: " + hostFunction.functionName, e);
      PanamaErrorMapper.writeErrorMessage(errorMessagePtr, errorMessageLen, e.getMessage());
      return 3;
    }
  }

  /**
   * Unmarshals FFI parameters to WasmValue array.
   *
   * <p>The FFI format uses FfiWasmValue structs: struct FfiWasmValue { tag: u8, padding: [u8; 7],
   * value: [u8; 16] }
   */
  private static WasmValue[] unmarshalFfiParams(
      final MemorySegment paramsPtr, final int paramsLen, final PanamaHostFunction hostFunction) {

    if (paramsLen == 0) {
      return new WasmValue[0];
    }

    // Rust FfiWasmValue layout:
    // - tag: i32 (4 bytes)
    // - value: [u8; 16] (16 bytes)
    // Total: 20 bytes
    final int ffiValueSize = 20;
    final WasmValueType[] paramTypes = hostFunction.functionType.getParamTypes();
    final WasmValue[] result = new WasmValue[paramsLen];

    // Reinterpret the zero-length segment with proper bounds
    // Panama upcalls receive pointers as zero-length segments that must be reinterpreted
    final long requiredSize = (long) paramsLen * ffiValueSize;
    final MemorySegment params = paramsPtr.reinterpret(requiredSize);

    for (int i = 0; i < paramsLen; i++) {
      final long offset = (long) i * ffiValueSize;
      final int tag = params.get(ValueLayout.JAVA_INT, offset); // Tag is i32, not u8
      final WasmValueType expectedType = i < paramTypes.length ? paramTypes[i] : WasmValueType.I32;

      result[i] = unmarshalFfiValue(params, offset, (byte) tag, expectedType);
    }

    return result;
  }

  /**
   * Unmarshals a single FFI value to WasmValue.
   *
   * @param ptr pointer to FfiWasmValue struct
   * @param offset offset within the array
   * @param tag the type tag
   * @param expectedType the expected WasmValueType
   * @return the unmarshalled WasmValue
   */
  private static WasmValue unmarshalFfiValue(
      final MemorySegment ptr,
      final long offset,
      final byte tag,
      final WasmValueType expectedType) {

    // Value starts at offset + 4 (after the 4-byte i32 tag)
    final long valueOffset = offset + 4;

    return switch (tag) {
      case 0 -> WasmValue.i32(ptr.get(ValueLayout.JAVA_INT, valueOffset));
      case 1 -> WasmValue.i64(ptr.get(ValueLayout.JAVA_LONG, valueOffset));
      case 2 -> WasmValue.f32(ptr.get(ValueLayout.JAVA_FLOAT, valueOffset));
      case 3 -> WasmValue.f64(ptr.get(ValueLayout.JAVA_DOUBLE, valueOffset));
      case 4 -> {
        // V128 - read 16 bytes
        final byte[] v128 = new byte[16];
        for (int j = 0; j < 16; j++) {
          v128[j] = ptr.get(ValueLayout.JAVA_BYTE, valueOffset + j);
        }
        yield WasmValue.v128(v128);
      }
      case 5 -> WasmValue.funcref(null); // FuncRef
      case 6 -> WasmValue.externref(null); // ExternRef
      default -> throw new IllegalArgumentException("Unknown FFI value tag: " + tag);
    };
  }

  /**
   * Marshals WasmValue results to FFI format.
   *
   * @param resultsPtr pointer to FFI results array (output)
   * @param resultsLen expected number of results
   * @param wasmResults the WasmValue results from callback
   * @param hostFunction the host function (for type info)
   */
  private static void marshalFfiResults(
      final MemorySegment resultsPtr,
      final int resultsLen,
      final WasmValue[] wasmResults,
      final PanamaHostFunction hostFunction) {

    if (resultsLen == 0) {
      return;
    }

    // FfiWasmValue layout in Rust:
    // - tag: i32 (4 bytes)
    // - value: [u8; 16] (16 bytes)
    // Total: 20 bytes (no padding needed, value is aligned at offset 4)
    final int ffiValueSize = 20;

    // Reinterpret the zero-length segment with proper bounds
    // Panama upcalls receive pointers as zero-length segments that must be reinterpreted
    final long requiredSize = (long) resultsLen * ffiValueSize;
    final MemorySegment results = resultsPtr.reinterpret(requiredSize);

    for (int i = 0; i < Math.min(resultsLen, wasmResults.length); i++) {
      final long offset = (long) i * ffiValueSize;
      marshalFfiValue(results, offset, wasmResults[i]);
    }
  }

  /**
   * Marshals a single WasmValue to FFI format.
   *
   * @param ptr pointer to FfiWasmValue struct
   * @param offset offset within the array
   * @param value the WasmValue to marshal
   */
  private static void marshalFfiValue(
      final MemorySegment ptr, final long offset, final WasmValue value) {

    // Rust FfiWasmValue layout:
    // - tag: i32 at offset 0 (4 bytes)
    // - value: [u8; 16] at offset 4 (16 bytes)

    // Write tag as i32 at offset
    final int tag =
        switch (value.getType()) {
          case I32 -> 0;
          case I64 -> 1;
          case F32 -> 2;
          case F64 -> 3;
          case V128 -> 4;
          case FUNCREF -> 5;
          case EXTERNREF -> 6;
          default -> throw new IllegalArgumentException("Unsupported type: " + value.getType());
        };
    ptr.set(ValueLayout.JAVA_INT, offset, tag);

    // Write value at offset + 4 (immediately after the 4-byte tag)
    final long valueOffset = offset + 4;

    switch (value.getType()) {
      case I32 -> ptr.set(ValueLayout.JAVA_INT, valueOffset, value.asInt());
      case I64 -> ptr.set(ValueLayout.JAVA_LONG, valueOffset, value.asLong());
      case F32 -> ptr.set(ValueLayout.JAVA_FLOAT, valueOffset, value.asFloat());
      case F64 -> ptr.set(ValueLayout.JAVA_DOUBLE, valueOffset, value.asDouble());
      case V128 -> {
        final byte[] v128 = value.asV128();
        for (int j = 0; j < Math.min(16, v128.length); j++) {
          ptr.set(ValueLayout.JAVA_BYTE, valueOffset + j, v128[j]);
        }
      }
      case FUNCREF -> {
        final Object ref = value.asFuncref();
        final long id = ref instanceof FunctionReference ? ((FunctionReference) ref).getId() : 0L;
        ptr.set(ValueLayout.JAVA_LONG, valueOffset, id);
      }
      case EXTERNREF -> {
        // ExternRef handling - externref IDs not yet fully supported
        ptr.set(ValueLayout.JAVA_LONG, valueOffset, 0L);
      }
      default -> throw new IllegalArgumentException("Unsupported type: " + value.getType());
    }
  }

  /**
   * Creates a method handle for the callback wrapper method.
   *
   * @return the method handle for invoking the host function
   * @throws NoSuchMethodException if the callback method cannot be found
   * @throws IllegalAccessException if the callback method cannot be accessed
   */
  private MethodHandle createCallbackMethodHandle()
      throws NoSuchMethodException, IllegalAccessException {
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    // Get the parameter types for the native callback
    final WasmValueType[] paramTypes = functionType.getParamTypes();
    final WasmValueType[] returnTypes = functionType.getReturnTypes();

    // Build method type for native callback (WITHOUT host function ID - that gets bound internally)
    final Class<?>[] nativeParams = new Class<?>[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      nativeParams[i] = getNativeType(paramTypes[i]);
    }

    final Class<?> nativeReturn =
        returnTypes.length == 0
            ? void.class
            : (returnTypes.length == 1 ? getNativeType(returnTypes[0]) : MemorySegment.class);

    final MethodType methodType = MethodType.methodType(nativeReturn, nativeParams);

    // Create adapter that matches the function descriptor exactly (no hostFunctionId parameter)
    return createGenericAdapter(methodType, hostFunctionId);
  }

  /**
   * Creates a function descriptor for the native callback signature.
   *
   * @return the function descriptor for Panama FFI
   */
  private FunctionDescriptor createFunctionDescriptor() {
    final WasmValueType[] paramTypes = functionType.getParamTypes();
    final WasmValueType[] returnTypes = functionType.getReturnTypes();

    // Build parameter layouts
    final ValueLayout[] paramLayouts = new ValueLayout[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      paramLayouts[i] = getNativeLayout(paramTypes[i]);
    }

    // Build return layout
    if (returnTypes.length == 0) {
      return FunctionDescriptor.ofVoid(paramLayouts);
    } else if (returnTypes.length == 1) {
      final ValueLayout returnLayout = getNativeLayout(returnTypes[0]);
      return FunctionDescriptor.of(returnLayout, paramLayouts);
    } else {
      // Multiple return values - use memory segment for struct return
      return FunctionDescriptor.of(ValueLayout.ADDRESS, paramLayouts);
    }
  }

  /**
   * Gets the native Java type for a WebAssembly value type.
   *
   * @param valueType the WebAssembly value type
   * @return the corresponding native Java type
   */
  private Class<?> getNativeType(final WasmValueType valueType) {
    return switch (valueType) {
      case I32 -> int.class;
      case I64 -> long.class;
      case F32 -> float.class;
      case F64 -> double.class;
      case V128 -> MemorySegment.class;
      case FUNCREF, EXTERNREF ->
          MemorySegment.class; // Use MemorySegment to match VALUE_LAYOUT.ADDRESS
      default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
    };
  }

  /**
   * Gets the native memory layout for a WebAssembly value type.
   *
   * @param valueType the WebAssembly value type
   * @return the corresponding Panama value layout
   */
  private ValueLayout getNativeLayout(final WasmValueType valueType) {
    return switch (valueType) {
      case I32 -> ValueLayout.JAVA_INT;
      case I64 -> ValueLayout.JAVA_LONG;
      case F32 -> ValueLayout.JAVA_FLOAT;
      case F64 -> ValueLayout.JAVA_DOUBLE;
      case V128 -> ValueLayout.ADDRESS;
      case FUNCREF, EXTERNREF -> ValueLayout.ADDRESS;
      default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
    };
  }

  /**
   * Static callback method invoked by native code through upcall stub.
   *
   * <p>This method serves as the bridge between native WebAssembly execution and Java host function
   * implementation. It marshals parameters, invokes the callback, and marshals results back to
   * native code.
   *
   * @param hostFunctionId the ID of the host function to invoke
   * @param nativeParams variable arguments representing the native parameters
   * @return the native return value
   */
  @SuppressWarnings("unused") // Called via method handle
  private static Object nativeCallback(final long hostFunctionId, final Object... nativeParams) {
    final PanamaHostFunction hostFunction = hostFunctionRegistry.get(hostFunctionId);
    if (hostFunction == null) {
      logger.severe("Host function not found in registry: " + hostFunctionId);
      return null; // or appropriate error value
    }

    try {
      if (hostFunction.resourceHandle.isClosed()) {
        logger.warning("Attempted to call closed host function: " + hostFunction.functionName);
        return null;
      }

      // Convert native parameters to WasmValue array
      final WasmValue[] wasmParams = hostFunction.marshalParameters(nativeParams);

      // Invoke the Java callback
      final WasmValue[] wasmResults;

      // Check if this is a caller-aware host function
      if (hostFunction.implementation instanceof HostFunction.CallerAwareHostFunction) {
        final PanamaStore store =
            hostFunction.storeRef != null ? hostFunction.storeRef.get() : null;
        if (store != null) {
          final long storeAddress = store.getNativeStore().address();
          if (storeAddress != 0) {
            final PanamaCaller<?> caller = new PanamaCaller<>(storeAddress, store);
            CALLER_CONTEXT.set(caller);
            try {
              wasmResults = hostFunction.implementation.execute(wasmParams);
            } finally {
              CALLER_CONTEXT.remove();
            }
          } else {
            logger.warning(
                "Store native handle is null for caller-aware function: "
                    + hostFunction.functionName);
            wasmResults = hostFunction.callback.execute(wasmParams);
          }
        } else {
          logger.warning(
              "Store reference not available for caller-aware function: "
                  + hostFunction.functionName);
          wasmResults = hostFunction.callback.execute(wasmParams);
        }
      } else {
        // Regular host function without caller context
        wasmResults = hostFunction.callback.execute(wasmParams);
      }

      // Convert WasmValue results back to native format
      return hostFunction.marshalResults(wasmResults);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error in host function callback: " + hostFunction.functionName, e);
      // Return appropriate error value based on return type
      final WasmValueType[] returnTypes = hostFunction.functionType.getReturnTypes();
      if (returnTypes.length == 0) {
        return null;
      } else if (returnTypes.length == 1) {
        return getDefaultValue(returnTypes[0]);
      } else {
        return MemorySegment.NULL;
      }
    }
  }

  /**
   * Generic adapter method that can handle any function signature by using method handle
   * transformation. This method serves as a bridge between the specific native signature and the
   * generic callback.
   */
  private static MethodHandle createGenericAdapter(
      final MethodType targetType, final long hostFunctionId) {
    try {
      // Create wrapper method that bridges between specific native signature and generic callback
      // The targetType now matches the function descriptor exactly (no hostFunctionId parameter)
      return createTypedWrapper(targetType, hostFunctionId);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to create generic adapter for type: " + targetType, e);
      throw new RuntimeException("Failed to create method handle adapter", e);
    }
  }

  /** Creates a typed wrapper method that converts between native signature and generic callback. */
  private static MethodHandle createTypedWrapper(
      final MethodType targetType, final long hostFunctionId)
      throws NoSuchMethodException, IllegalAccessException {

    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    final int paramCount = targetType.parameterCount(); // Now this is the exact parameter count

    // Create a wrapper method that matches exactly the target signature pattern
    final String wrapperMethodName = getWrapperMethodName(targetType);

    try {
      // Try to find a specific wrapper method for this signature
      // But we need to add the hostFunctionId parameter to the wrapper method signature
      final Class<?>[] wrapperParams = new Class<?>[paramCount + 1];
      wrapperParams[0] = long.class; // hostFunctionId
      for (int i = 0; i < paramCount; i++) {
        wrapperParams[i + 1] = targetType.parameterType(i);
      }
      final MethodType wrapperType = MethodType.methodType(targetType.returnType(), wrapperParams);

      final MethodHandle specificWrapper =
          lookup.findStatic(PanamaHostFunction.class, wrapperMethodName, wrapperType);
      return MethodHandles.insertArguments(specificWrapper, 0, hostFunctionId);
    } catch (NoSuchMethodException e) {
      // If no specific wrapper exists, create a generic one using method handle combinators
      return createDynamicWrapper(targetType, hostFunctionId);
    }
  }

  /** Creates a dynamic wrapper using MethodHandle combinators. */
  private static MethodHandle createDynamicWrapper(
      final MethodType targetType, final long hostFunctionId)
      throws NoSuchMethodException, IllegalAccessException {

    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    // Get the base generic callback
    final MethodHandle genericCallback =
        lookup.findStatic(
            PanamaHostFunction.class,
            "nativeCallback",
            MethodType.methodType(Object.class, long.class, Object[].class));

    // Create parameter gathering wrapper that matches target signature exactly
    final int paramCount = targetType.parameterCount(); // Now this is the exact parameter count

    // Create a MethodHandle that gathers parameters into Object array, maintaining exact signature
    MethodHandle wrapper;

    if (paramCount == 0) {
      // () -> ReturnType - no parameters
      wrapper = MethodHandles.insertArguments(genericCallback, 0, hostFunctionId, new Object[0]);
    } else {
      // Multi-parameter case: create parameter-gathering logic
      wrapper = createParameterGatheringWrapper(targetType, genericCallback, hostFunctionId);
    }

    // Handle return type conversion - ensure we convert to exact target return type
    final Class<?> returnType = targetType.returnType();
    if (returnType.isPrimitive() && returnType != void.class) {
      final MethodHandle converter = createReturnTypeConverter(returnType);
      wrapper = MethodHandles.filterReturnValue(wrapper, converter);
    } else if (returnType == void.class) {
      // For void return, we need to drop the Object return value
      wrapper =
          MethodHandles.filterReturnValue(
              wrapper, MethodHandles.empty(MethodType.methodType(void.class)));
    } else {
      // For non-primitive return types, ensure proper type conversion
      wrapper = wrapper.asType(targetType);
    }

    return wrapper;
  }

  /** Creates a parameter-gathering wrapper for multi-parameter functions. */
  private static MethodHandle createParameterGatheringWrapper(
      final MethodType targetType, final MethodHandle genericCallback, final long hostFunctionId)
      throws NoSuchMethodException, IllegalAccessException {

    final int paramCount = targetType.parameterCount(); // Now this is the exact parameter count

    // Create a wrapper that has exact target signature but converts parameters to Object array
    // internally
    final MethodHandle boundCallback =
        MethodHandles.insertArguments(genericCallback, 0, hostFunctionId);

    // Create a collector that gathers all parameters into Object array
    MethodHandle collector = boundCallback.asCollector(Object[].class, paramCount);

    // Convert the collector to match the exact target parameter types
    collector = collector.asType(targetType.changeReturnType(Object.class));

    return collector;
  }

  /** Gets wrapper method name for a specific signature pattern. */
  private static String getWrapperMethodName(final MethodType targetType) {
    final StringBuilder sb = new StringBuilder("wrapperFor");

    // Add parameter types (all parameters, since hostFunctionId is not in targetType anymore)
    for (int i = 0; i < targetType.parameterCount(); i++) {
      final Class<?> paramType = targetType.parameterType(i);
      sb.append(getTypeCode(paramType));
    }

    // Add return type
    sb.append("To");
    sb.append(getTypeCode(targetType.returnType()));

    return sb.toString();
  }

  /** Gets type code for wrapper method naming. */
  private static String getTypeCode(final Class<?> type) {
    if (type == int.class) {
      return "I";
    }
    if (type == long.class) {
      return "L";
    }
    if (type == float.class) {
      return "F";
    }
    if (type == double.class) {
      return "D";
    }
    if (type == void.class) {
      return "V";
    }
    return "O"; // Object
  }

  /** Creates a method handle converter for primitive return types. */
  private static MethodHandle createReturnTypeConverter(final Class<?> returnType)
      throws NoSuchMethodException, IllegalAccessException {
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    return switch (returnType.getName()) {
      case "float" ->
          lookup.findStatic(
              PanamaHostFunction.class,
              "convertToFloat",
              MethodType.methodType(float.class, Object.class));
      case "int" ->
          lookup.findStatic(
              PanamaHostFunction.class,
              "convertToInt",
              MethodType.methodType(int.class, Object.class));
      case "double" ->
          lookup.findStatic(
              PanamaHostFunction.class,
              "convertToDouble",
              MethodType.methodType(double.class, Object.class));
      case "long" ->
          lookup.findStatic(
              PanamaHostFunction.class,
              "convertToLong",
              MethodType.methodType(long.class, Object.class));
      default -> throw new IllegalArgumentException("Unsupported return type: " + returnType);
    };
  }

  // Helper methods for return type conversion
  private static float convertToFloat(final Object value) {
    if (value instanceof Number number) {
      return number.floatValue();
    }
    throw new ClassCastException("Cannot convert " + value + " to float");
  }

  private static int convertToInt(final Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    throw new ClassCastException("Cannot convert " + value + " to int");
  }

  private static double convertToDouble(final Object value) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    throw new ClassCastException("Cannot convert " + value + " to double");
  }

  private static long convertToLong(final Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    throw new ClassCastException("Cannot convert " + value + " to long");
  }

  // Specific wrapper methods for common function signatures

  // (long) -> void
  private static void wrapperForToV(final long hostFunctionId) {
    final Object result = nativeCallback(hostFunctionId, new Object[0]);
    // Void return, ignore result
  }

  // (long, int) -> void
  private static void wrapperForIToV(final long hostFunctionId, final int param1) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1});
    // Void return, ignore result
  }

  // (long, int, long) -> float
  private static float wrapperForILToF(
      final long hostFunctionId, final int param1, final long param2) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1, param2});
    return convertToFloat(result);
  }

  // (long, int) -> int
  private static int wrapperForIToI(final long hostFunctionId, final int param1) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1});
    return convertToInt(result);
  }

  // (long, long) -> long
  private static long wrapperForLToL(final long hostFunctionId, final long param1) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1});
    return convertToLong(result);
  }

  // (long, float) -> float
  private static float wrapperForFToF(final long hostFunctionId, final float param1) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1});
    return convertToFloat(result);
  }

  // (long, double) -> double
  private static double wrapperForDToD(final long hostFunctionId, final double param1) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1});
    return convertToDouble(result);
  }

  // (long) -> int (no params, returns int)
  private static int wrapperForToI(final long hostFunctionId) {
    final Object result = nativeCallback(hostFunctionId, new Object[0]);
    return convertToInt(result);
  }

  // (long) -> long (no params, returns long)
  private static long wrapperForToL(final long hostFunctionId) {
    final Object result = nativeCallback(hostFunctionId, new Object[0]);
    return convertToLong(result);
  }

  // (long) -> float (no params, returns float)
  private static float wrapperForToF(final long hostFunctionId) {
    final Object result = nativeCallback(hostFunctionId, new Object[0]);
    return convertToFloat(result);
  }

  // (long) -> double (no params, returns double)
  private static double wrapperForToD(final long hostFunctionId) {
    final Object result = nativeCallback(hostFunctionId, new Object[0]);
    return convertToDouble(result);
  }

  // (long, int, int) -> int (two int params, returns int)
  private static int wrapperForIIToI(
      final long hostFunctionId, final int param1, final int param2) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1, param2});
    return convertToInt(result);
  }

  // (long, long, long) -> long (two long params, returns long)
  private static long wrapperForLLToL(
      final long hostFunctionId, final long param1, final long param2) {
    final Object result = nativeCallback(hostFunctionId, new Object[] {param1, param2});
    return convertToLong(result);
  }

  /**
   * Marshals native parameters to WasmValue array.
   *
   * @param nativeParams the native parameter values
   * @return the corresponding WasmValue array
   */
  private WasmValue[] marshalParameters(final Object[] nativeParams) {
    final WasmValueType[] paramTypes = functionType.getParamTypes();
    final WasmValue[] wasmParams = new WasmValue[paramTypes.length];

    for (int i = 0; i < paramTypes.length; i++) {
      final Object nativeParam = nativeParams[i];
      wasmParams[i] = createWasmValue(paramTypes[i], nativeParam);
    }

    return wasmParams;
  }

  /**
   * Marshals WasmValue results back to native format.
   *
   * @param wasmResults the WasmValue results from the callback
   * @return the native return value
   */
  private Object marshalResults(final WasmValue[] wasmResults) {
    final WasmValueType[] returnTypes = functionType.getReturnTypes();

    if (returnTypes.length == 0) {
      return null;
    } else if (returnTypes.length == 1) {
      return extractNativeValue(wasmResults[0]);
    } else {
      // Multiple return values - return as an array
      final Object[] results = new Object[wasmResults.length];
      for (int i = 0; i < wasmResults.length; i++) {
        results[i] = extractNativeValue(wasmResults[i]);
      }
      return results;
    }
  }

  /**
   * Creates a WasmValue from a native parameter value.
   *
   * @param valueType the expected WebAssembly value type
   * @param nativeValue the native parameter value
   * @return the corresponding WasmValue
   */
  private WasmValue createWasmValue(final WasmValueType valueType, final Object nativeValue) {
    return switch (valueType) {
      case I32 -> WasmValue.i32((Integer) nativeValue);
      case I64 -> WasmValue.i64((Long) nativeValue);
      case F32 -> WasmValue.f32((Float) nativeValue);
      case F64 -> WasmValue.f64((Double) nativeValue);
      case V128 -> WasmValue.v128((byte[]) nativeValue);
      case FUNCREF, EXTERNREF -> WasmValue.externref(nativeValue); // Handle as reference
      default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
    };
  }

  /**
   * Extracts the native value from a WasmValue.
   *
   * @param wasmValue the WasmValue to extract from
   * @return the native value
   */
  private Object extractNativeValue(final WasmValue wasmValue) {
    return switch (wasmValue.getType()) {
      case I32 -> wasmValue.asInt();
      case I64 -> wasmValue.asLong();
      case F32 -> wasmValue.asFloat();
      case F64 -> wasmValue.asDouble();
      case V128 -> wasmValue.asV128();
      case FUNCREF, EXTERNREF -> wasmValue.asExternref();
      default ->
          throw new IllegalArgumentException("Unsupported value type: " + wasmValue.getType());
    };
  }

  /**
   * Gets a default value for a WebAssembly value type (used for error cases).
   *
   * @param valueType the WebAssembly value type
   * @return the default value
   */
  private static Object getDefaultValue(final WasmValueType valueType) {
    return switch (valueType) {
      case I32 -> 0;
      case I64 -> 0L;
      case F32 -> 0.0f;
      case F64 -> 0.0;
      case V128 -> new byte[16]; // Default V128 value (16 zeros)
      case FUNCREF, EXTERNREF -> 0L; // Null pointer/handle
      default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
    };
  }

  /** Native cleanup method called by the arena manager. */
  private void closeNative() {
    try {
      if (upcallStub != null && !upcallStub.equals(MemorySegment.NULL)) {
        // The upcall stub will be automatically cleaned up when the arena is closed
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("Released host function upcall stub: " + functionName);
        }
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to release host function resources: " + e.getMessage(), e);
    }
  }

  /**
   * Ensures the host function is not closed, throwing an exception if it is.
   *
   * @throws IllegalStateException if the host function has been closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Gets the current caller context for the executing host function.
   *
   * <p>This method is called by PanamaCallerContextProvider (via CallerAwareHostFunction) to access
   * the caller during execution.
   *
   * @param <T> the type of user data
   * @return the current caller context
   * @throws UnsupportedOperationException if no caller context is available
   */
  @SuppressWarnings("unchecked")
  static <T> Caller<T> getCurrentCaller() {
    final PanamaCaller<?> caller = CALLER_CONTEXT.get();
    if (caller == null) {
      throw new UnsupportedOperationException(
          "Caller context not available - this should be provided by the runtime");
    }
    return (PanamaCaller<T>) caller;
  }

  @Override
  public String toString() {
    if (resourceHandle.isClosed()) {
      return "PanamaHostFunction{name='" + functionName + "', closed=true}";
    }

    return String.format(
        "PanamaHostFunction{name='%s', type=%s, id=%d}",
        functionName, functionType, hostFunctionId);
  }
}
