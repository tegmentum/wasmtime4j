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
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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

  private final long hostFunctionId;
  private final String functionName;
  private final FunctionType functionType;
  private final HostFunctionCallback callback;
  private final ArenaResourceManager arenaManager;
  private final PanamaErrorHandler errorHandler;

  private MemorySegment functionHandle;
  private MemorySegment upcallStub;
  private volatile boolean closed = false;

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
   * @param arenaManager the arena resource manager for memory lifecycle
   * @param errorHandler the error handler for exception mapping
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  public PanamaHostFunction(
      final String functionName,
      final FunctionType functionType,
      final HostFunctionCallback callback,
      final ArenaResourceManager arenaManager,
      final PanamaErrorHandler errorHandler)
      throws WasmException {
    this.hostFunctionId = nextHostFunctionId.getAndIncrement();
    this.functionName = Objects.requireNonNull(functionName, "Function name cannot be null");
    this.functionType = Objects.requireNonNull(functionType, "Function type cannot be null");
    this.callback = Objects.requireNonNull(callback, "Callback cannot be null");
    this.arenaManager = Objects.requireNonNull(arenaManager, "Arena manager cannot be null");
    this.errorHandler = Objects.requireNonNull(errorHandler, "Error handler cannot be null");

    try {
      // Register this host function to prevent GC
      hostFunctionRegistry.put(hostFunctionId, this);

      // Create the upcall stub for native-to-Java callback
      createUpcallStub();

      // Register for automatic resource cleanup
      arenaManager.registerManagedNativeResource(this, upcallStub, this::closeNative);

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Created host function '" + functionName + "' with ID: " + hostFunctionId);
      }
    } catch (Exception e) {
      hostFunctionRegistry.remove(hostFunctionId);
      throw PanamaErrorHandler.mapToWasmException(e, "Failed to create host function: " + functionName);
    }
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    ensureNotClosed();

    // Host functions are called FROM WebAssembly, not TO WebAssembly
    // This method shouldn't be used directly for host functions
    throw new UnsupportedOperationException(
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
    return closed;
  }

  /**
   * Closes this host function and releases its resources.
   */
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Remove from global registry
        hostFunctionRegistry.remove(hostFunctionId);

        // Unregister from arena manager - this will call closeNative()
        arenaManager.unregisterManagedResource(this);

        if (logger.isLoggable(Level.FINE)) {
          logger.fine("Closed host function '" + functionName + "' with ID: " + hostFunctionId);
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to close host function: " + functionName, e);
      } finally {
        closed = true;
      }
    }
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

    // Build method type for native callback (long parameters + return)
    final Class<?>[] nativeParams = new Class<?>[paramTypes.length + 1]; // +1 for host function ID
    nativeParams[0] = long.class; // host function ID
    for (int i = 0; i < paramTypes.length; i++) {
      nativeParams[i + 1] = getNativeType(paramTypes[i]);
    }

    final Class<?> nativeReturn =
        returnTypes.length == 0
            ? void.class
            : (returnTypes.length == 1 ? getNativeType(returnTypes[0]) : MemorySegment.class);

    final MethodType methodType = MethodType.methodType(nativeReturn, nativeParams);

    // Find the static callback method
    final MethodHandle baseHandle =
        lookup.findStatic(PanamaHostFunction.class, "nativeCallback", methodType);

    // Bind the host function ID to the method handle
    return MethodHandles.insertArguments(baseHandle, 0, hostFunctionId);
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
      case V128 -> byte[].class;
      case FUNCREF, EXTERNREF -> long.class; // Use handle/pointer as long
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
      if (hostFunction.closed) {
        logger.warning("Attempted to call closed host function: " + hostFunction.functionName);
        return null;
      }

      // Convert native parameters to WasmValue array
      final WasmValue[] wasmParams = hostFunction.marshalParameters(nativeParams);

      // Invoke the Java callback
      final WasmValue[] wasmResults = hostFunction.callback.execute(wasmParams);

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
      // Multiple return values - pack into memory segment
      // This would require more complex marshaling
      throw new UnsupportedOperationException("Multiple return values not yet implemented");
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
      case I32 -> wasmValue.asI32();
      case I64 -> wasmValue.asI64();
      case F32 -> wasmValue.asF32();
      case F64 -> wasmValue.asF64();
      case V128 -> wasmValue.asV128();
      case FUNCREF, EXTERNREF -> wasmValue.asExternref();
      default -> throw new IllegalArgumentException("Unsupported value type: " + wasmValue.getType());
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
    if (closed) {
      throw new IllegalStateException("Host function '" + functionName + "' has been closed");
    }
  }

  @Override
  public String toString() {
    if (closed) {
      return "PanamaHostFunction{name='" + functionName + "', closed=true}";
    }

    return String.format(
        "PanamaHostFunction{name='%s', type=%s, id=%d}",
        functionName, functionType, hostFunctionId);
  }
}
