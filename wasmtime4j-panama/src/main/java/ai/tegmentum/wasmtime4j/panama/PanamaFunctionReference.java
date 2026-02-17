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
 * Panama FFI implementation for WebAssembly function references.
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
 *   <li>Parameter and return value marshalling via Panama FFI
 *   <li>Error handling and exception propagation
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaFunctionReference implements FunctionReference {
  private static final Logger LOGGER = Logger.getLogger(PanamaFunctionReference.class.getName());

  // Global registry for function references to prevent GC
  private static final ConcurrentHashMap<Long, PanamaFunctionReference>
      FUNCTION_REFERENCE_REGISTRY = new ConcurrentHashMap<>();
  private static final AtomicLong NEXT_FUNCTION_REFERENCE_ID = new AtomicLong(1L);

  private static final NativeInstanceBindings NATIVE_BINDINGS =
      NativeInstanceBindings.getInstance();

  /** Global arena for the callback stub - lives for the entire JVM lifetime. */
  private static final Arena GLOBAL_CALLBACK_ARENA = Arena.ofAuto();

  /** Global callback stub address, lazily initialized. */
  private static volatile MemorySegment GLOBAL_CALLBACK_STUB = null;

  /** Lock for initializing the global callback stub. */
  private static final Object CALLBACK_STUB_LOCK = new Object();

  private final long functionReferenceId;
  private final String functionName;
  private final FunctionType functionType;
  private final HostFunction hostFunction; // Null for WebAssembly functions
  private final WasmFunction wasmFunction; // Null for host functions
  private final WeakReference<PanamaStore> storeRef;
  private final ArenaResourceManager arenaManager;

  /** The native registry ID for this function reference, used when setting funcref globals. */
  private long nativeRegistryId = -1;

  private MemorySegment upcallStub;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new function reference from a host function.
   *
   * @param hostFunction the host function implementation
   * @param functionType the WebAssembly function type signature
   * @param store the store this function reference belongs to
   * @param arenaManager the arena resource manager for memory lifecycle
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if any required parameter is null
   */
  public PanamaFunctionReference(
      final HostFunction hostFunction,
      final FunctionType functionType,
      final PanamaStore store,
      final ArenaResourceManager arenaManager)
      throws WasmException {
    if (hostFunction == null) {
      throw new WasmException("Failed to create function reference: Host function cannot be null");
    }
    if (functionType == null) {
      throw new WasmException("Failed to create function reference: Function type cannot be null");
    }
    if (store == null) {
      throw new WasmException("Failed to create function reference: Store cannot be null");
    }
    if (arenaManager == null) {
      throw new WasmException("Failed to create function reference: Arena manager cannot be null");
    }

    this.functionReferenceId = NEXT_FUNCTION_REFERENCE_ID.getAndIncrement();
    this.functionName = "host_function_" + functionReferenceId;
    this.functionType = functionType;
    this.hostFunction = hostFunction;
    this.wasmFunction = null;
    this.storeRef = new WeakReference<>(store);
    this.arenaManager = arenaManager;

    try {
      // Register this function reference to prevent GC
      FUNCTION_REFERENCE_REGISTRY.put(functionReferenceId, this);

      // Create the upcall stub for native-to-Java callback
      createUpcallStub();

      // Create native function reference so it can be used with funcref globals
      createNativeFunctionReference(store);

      // Register for automatic resource cleanup
      arenaManager.registerManagedNativeResource(this, upcallStub, this::closeNative);

      // Initialize resource handle with cleanup logic
      final long capturedFuncRefId = this.functionReferenceId;
      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaFunctionReference",
              () -> {
                // Remove from global registry
                FUNCTION_REFERENCE_REGISTRY.remove(functionReferenceId);

                // Unregister from arena manager - this will call closeNative()
                if (arenaManager != null && upcallStub != null) {
                  arenaManager.unregisterManagedResource(this);
                }

                if (LOGGER.isLoggable(Level.FINE)) {
                  LOGGER.fine(
                      "Closed function reference '"
                          + functionName
                          + "' with ID: "
                          + functionReferenceId);
                }
              },
              this,
              () -> {
                FUNCTION_REFERENCE_REGISTRY.remove(capturedFuncRefId);
              });

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            "Created host function reference '"
                + functionName
                + "' with ID: "
                + functionReferenceId
                + ", native registry ID: "
                + nativeRegistryId);
      }
    } catch (Exception e) {
      FUNCTION_REFERENCE_REGISTRY.remove(functionReferenceId);
      throw new WasmException("Failed to create function reference: " + functionName, e);
    }
  }

  /**
   * Creates a new function reference from a WebAssembly function.
   *
   * @param wasmFunction the WebAssembly function
   * @param store the store this function reference belongs to
   * @param arenaManager the arena resource manager for memory lifecycle
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if any required parameter is null
   */
  public PanamaFunctionReference(
      final WasmFunction wasmFunction,
      final PanamaStore store,
      final ArenaResourceManager arenaManager)
      throws WasmException {
    if (wasmFunction == null) {
      throw new WasmException(
          "Failed to create function reference: WebAssembly function cannot be null");
    }
    if (store == null) {
      throw new WasmException("Failed to create function reference: Store cannot be null");
    }
    if (arenaManager == null) {
      throw new WasmException("Failed to create function reference: Arena manager cannot be null");
    }

    this.functionReferenceId = NEXT_FUNCTION_REFERENCE_ID.getAndIncrement();
    this.functionName =
        wasmFunction.getName() != null
            ? wasmFunction.getName()
            : "wasm_function_" + functionReferenceId;
    this.functionType = wasmFunction.getFunctionType();
    this.hostFunction = null;
    this.wasmFunction = wasmFunction;
    this.storeRef = new WeakReference<>(store);
    this.arenaManager = arenaManager;

    try {
      // Register this function reference to prevent GC
      FUNCTION_REFERENCE_REGISTRY.put(functionReferenceId, this);

      // WebAssembly functions don't need upcall stubs
      this.upcallStub = null;

      // Initialize resource handle with cleanup logic
      final long capturedFuncRefId2 = this.functionReferenceId;
      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaFunctionReference",
              () -> {
                // Remove from global registry
                FUNCTION_REFERENCE_REGISTRY.remove(functionReferenceId);

                if (LOGGER.isLoggable(Level.FINE)) {
                  LOGGER.fine(
                      "Closed function reference '"
                          + functionName
                          + "' with ID: "
                          + functionReferenceId);
                }
              },
              this,
              () -> {
                FUNCTION_REFERENCE_REGISTRY.remove(capturedFuncRefId2);
              });

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            "Created WebAssembly function reference '"
                + functionName
                + "' with ID: "
                + functionReferenceId);
      }
    } catch (Exception e) {
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
    ensureNotClosed();
    Objects.requireNonNull(params, "Parameters cannot be null");

    if (hostFunction != null) {
      // Direct call to host function
      return hostFunction.execute(params);
    } else if (wasmFunction != null) {
      // Delegate to WebAssembly function
      return wasmFunction.call(params);
    } else {
      throw new WasmException("Function reference has no associated function");
    }
  }

  @Override
  public String getName() {
    return functionName;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed() && (hostFunction != null || wasmFunction != null);
  }

  @Override
  public long getId() {
    // Return native registry ID if available (for use with funcref globals/tables)
    // Otherwise return the Java-side ID
    return nativeRegistryId >= 0 ? nativeRegistryId : functionReferenceId;
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
   * Gets the upcall stub for this function reference.
   *
   * <p>This is used when passing the function reference to native code.
   *
   * @return the upcall stub memory segment, or null for WebAssembly functions
   */
  public MemorySegment getUpcallStub() {
    return upcallStub;
  }

  /**
   * Returns the registry ID as a long value for storage in globals or tables.
   *
   * @return the registry ID
   */
  public long longValue() {
    return functionReferenceId;
  }

  /**
   * Closes this function reference and releases its resources.
   *
   * @throws WasmException if closing fails
   */
  public void close() throws WasmException {
    resourceHandle.close();
  }

  /**
   * Creates the Panama upcall stub for this function reference.
   *
   * <p>The upcall stub provides a native function pointer that can be called from WebAssembly code,
   * which will then invoke the Java callback implementation.
   *
   * @throws WasmException if upcall stub creation fails
   */
  private void createUpcallStub() throws WasmException {
    if (hostFunction == null) {
      // WebAssembly functions don't need upcall stubs
      return;
    }

    try {
      // Create method handle for the callback wrapper
      final MethodHandle callbackHandle = createCallbackMethodHandle();

      // Create function descriptor based on WebAssembly function type
      final FunctionDescriptor descriptor = createFunctionDescriptor();

      // Create the upcall stub using Panama's Linker
      final Linker linker = Linker.nativeLinker();
      this.upcallStub = linker.upcallStub(callbackHandle, descriptor, arenaManager.getArena());

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("Created upcall stub for function reference: " + functionName);
      }
    } catch (Exception e) {
      throw new WasmException(
          "Failed to create upcall stub for function reference: " + functionName, e);
    }
  }

  /**
   * Creates the native function reference so it can be used with funcref globals.
   *
   * <p>This registers the function in the native registry and stores the registry ID, which is
   * required when setting funcref globals.
   *
   * @param store the store to create the function reference in
   * @throws WasmException if creation fails
   */
  private void createNativeFunctionReference(final PanamaStore store) throws WasmException {
    if (hostFunction == null) {
      // WebAssembly functions don't need native function references created here
      // They already have a native Func
      return;
    }

    try (final Arena tempArena = Arena.ofConfined()) {
      // Convert function type to native arrays
      final WasmValueType[] paramTypes = functionType.getParamTypes();
      final WasmValueType[] returnTypes = functionType.getReturnTypes();

      // Allocate param types array
      final MemorySegment paramTypesSegment =
          tempArena.allocate(ValueLayout.JAVA_INT, paramTypes.length);
      for (int i = 0; i < paramTypes.length; i++) {
        paramTypesSegment.setAtIndex(ValueLayout.JAVA_INT, i, paramTypes[i].toNativeTypeCode());
      }

      // Allocate return types array
      final MemorySegment returnTypesSegment =
          tempArena.allocate(ValueLayout.JAVA_INT, returnTypes.length);
      for (int i = 0; i < returnTypes.length; i++) {
        returnTypesSegment.setAtIndex(ValueLayout.JAVA_INT, i, returnTypes[i].toNativeTypeCode());
      }

      // Allocate result output
      final MemorySegment resultOut = tempArena.allocate(ValueLayout.JAVA_LONG);

      // Get the global callback function pointer
      final MemorySegment callbackFn = getGlobalCallbackStub();

      // Call native function to create the function reference
      final int result =
          NATIVE_BINDINGS.functionReferenceCreate(
              store.getNativeStore(),
              paramTypesSegment,
              paramTypes.length,
              returnTypesSegment,
              returnTypes.length,
              callbackFn,
              functionReferenceId,
              resultOut);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to create native function reference");
      }

      // Store the native registry ID
      this.nativeRegistryId = resultOut.get(ValueLayout.JAVA_LONG, 0);

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            "Created native function reference with registry ID: "
                + nativeRegistryId
                + " for "
                + functionName);
      }
    }
  }

  /**
   * Gets the native registry ID for this function reference.
   *
   * <p>This ID is used when setting funcref globals or storing in tables.
   *
   * @return the native registry ID, or -1 if not registered
   */
  public long getNativeRegistryId() {
    return nativeRegistryId;
  }

  /**
   * Creates a method handle for the callback wrapper method.
   *
   * @return the method handle for invoking the function reference
   * @throws NoSuchMethodException if the callback method cannot be found
   * @throws IllegalAccessException if the callback method cannot be accessed
   */
  private MethodHandle createCallbackMethodHandle()
      throws NoSuchMethodException, IllegalAccessException {
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    // Get the parameter types for the native callback
    final WasmValueType[] paramTypes = functionType.getParamTypes();
    final WasmValueType[] returnTypes = functionType.getReturnTypes();

    // Build method type for native callback
    final Class<?>[] nativeParams = new Class<?>[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      nativeParams[i] = getNativeType(paramTypes[i]);
    }

    final Class<?> nativeReturn =
        returnTypes.length == 0
            ? void.class
            : (returnTypes.length == 1 ? getNativeType(returnTypes[0]) : MemorySegment.class);

    final MethodType methodType = MethodType.methodType(nativeReturn, nativeParams);

    // Create adapter that matches the function descriptor exactly
    return createGenericAdapter(methodType, functionReferenceId);
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
      case FUNCREF, EXTERNREF -> MemorySegment.class;
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
   * @param functionReferenceId the ID of the function reference to invoke
   * @param nativeParams variable arguments representing the native parameters
   * @return the native return value
   */
  @SuppressWarnings("unused") // Called via method handle
  private static Object nativeCallback(
      final long functionReferenceId, final Object... nativeParams) {
    final PanamaFunctionReference funcRef = FUNCTION_REFERENCE_REGISTRY.get(functionReferenceId);
    if (funcRef == null) {
      LOGGER.severe("Function reference not found in registry: " + functionReferenceId);
      return null;
    }

    try {
      if (funcRef.resourceHandle.isClosed()) {
        LOGGER.warning("Attempted to call closed function reference: " + funcRef.functionName);
        return null;
      }

      if (funcRef.hostFunction == null) {
        LOGGER.severe(
            "Function reference callback called on non-host function: " + funcRef.functionName);
        return null;
      }

      // Convert native parameters to WasmValue array
      final WasmValue[] wasmParams = funcRef.marshalParameters(nativeParams);

      // Invoke the host function
      final WasmValue[] wasmResults = funcRef.hostFunction.execute(wasmParams);

      // Convert WasmValue results back to native format
      return funcRef.marshalResults(wasmResults);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error in function reference callback: " + funcRef.functionName, e);
      // Return appropriate error value based on return type
      final WasmValueType[] returnTypes = funcRef.functionType.getReturnTypes();
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
   * transformation.
   */
  private static MethodHandle createGenericAdapter(
      final MethodType targetType, final long functionReferenceId) {
    try {
      return createDynamicWrapper(targetType, functionReferenceId);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create generic adapter for type: " + targetType, e);
      throw new RuntimeException("Failed to create method handle adapter", e);
    }
  }

  /** Creates a dynamic wrapper using MethodHandle combinators. */
  private static MethodHandle createDynamicWrapper(
      final MethodType targetType, final long functionReferenceId)
      throws NoSuchMethodException, IllegalAccessException {

    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    // Get the base generic callback
    final MethodHandle genericCallback =
        lookup.findStatic(
            PanamaFunctionReference.class,
            "nativeCallback",
            MethodType.methodType(Object.class, long.class, Object[].class));

    // Create parameter gathering wrapper that matches target signature exactly
    final int paramCount = targetType.parameterCount();

    // Create a MethodHandle that gathers parameters into Object array
    MethodHandle wrapper;

    if (paramCount == 0) {
      // () -> ReturnType - no parameters
      wrapper =
          MethodHandles.insertArguments(genericCallback, 0, functionReferenceId, new Object[0]);
    } else {
      // Multi-parameter case: create parameter-gathering logic
      wrapper = createParameterGatheringWrapper(targetType, genericCallback, functionReferenceId);
    }

    // Handle return type conversion
    final Class<?> returnType = targetType.returnType();
    if (returnType.isPrimitive() && returnType != void.class) {
      final MethodHandle converter = createReturnTypeConverter(returnType);
      wrapper = MethodHandles.filterReturnValue(wrapper, converter);
    } else if (returnType == void.class) {
      // For void return, we need to drop the Object return value
      // Create a filter that takes Object and returns void (discards the value)
      final MethodHandle voidDropper =
          MethodHandles.dropArguments(
              MethodHandles.empty(MethodType.methodType(void.class)), 0, Object.class);
      wrapper = MethodHandles.filterReturnValue(wrapper, voidDropper);
    } else {
      // For non-primitive return types, ensure proper type conversion
      wrapper = wrapper.asType(targetType);
    }

    return wrapper;
  }

  /** Creates a parameter-gathering wrapper for multi-parameter functions. */
  private static MethodHandle createParameterGatheringWrapper(
      final MethodType targetType,
      final MethodHandle genericCallback,
      final long functionReferenceId) {

    final int paramCount = targetType.parameterCount();

    // Create a wrapper that has exact target signature but converts parameters to Object array
    final MethodHandle boundCallback =
        MethodHandles.insertArguments(genericCallback, 0, functionReferenceId);

    // Create a collector that gathers all parameters into Object array
    MethodHandle collector = boundCallback.asCollector(Object[].class, paramCount);

    // Convert the collector to match the exact target parameter types
    collector = collector.asType(targetType.changeReturnType(Object.class));

    return collector;
  }

  /** Creates a method handle converter for primitive return types. */
  private static MethodHandle createReturnTypeConverter(final Class<?> returnType)
      throws NoSuchMethodException, IllegalAccessException {
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    return switch (returnType.getName()) {
      case "float" ->
          lookup.findStatic(
              PanamaFunctionReference.class,
              "convertToFloat",
              MethodType.methodType(float.class, Object.class));
      case "int" ->
          lookup.findStatic(
              PanamaFunctionReference.class,
              "convertToInt",
              MethodType.methodType(int.class, Object.class));
      case "double" ->
          lookup.findStatic(
              PanamaFunctionReference.class,
              "convertToDouble",
              MethodType.methodType(double.class, Object.class));
      case "long" ->
          lookup.findStatic(
              PanamaFunctionReference.class,
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
      case FUNCREF, EXTERNREF -> WasmValue.externref(nativeValue);
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
      case V128 -> new byte[16];
      case FUNCREF, EXTERNREF -> 0L;
      default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
    };
  }

  /** Native cleanup method called by the arena manager. */
  private void closeNative() {
    try {
      if (upcallStub != null && !upcallStub.equals(MemorySegment.NULL)) {
        // The upcall stub will be automatically cleaned up when the arena is closed
        if (LOGGER.isLoggable(Level.FINE)) {
          LOGGER.fine("Released function reference upcall stub: " + functionName);
        }
      }
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING, "Failed to release function reference resources: " + e.getMessage(), e);
    }
  }

  /**
   * Ensures the function reference is not closed.
   *
   * @throws WasmException if the function reference has been closed
   */
  private void ensureNotClosed() throws WasmException {
    resourceHandle.ensureNotClosed();
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
   * Retrieves a function reference from the registry by ID (package-private).
   *
   * @param functionReferenceId the function reference ID
   * @return the function reference, or null if not found
   */
  static PanamaFunctionReference getFromRegistry(final long functionReferenceId) {
    return FUNCTION_REFERENCE_REGISTRY.get(functionReferenceId);
  }

  /**
   * Retrieves a function reference from the registry by ID.
   *
   * <p>This is the public accessor for looking up function references during value unmarshalling.
   *
   * @param functionReferenceId the function reference ID
   * @return the function reference, or null if not found
   */
  public static FunctionReference getFunctionReferenceById(final long functionReferenceId) {
    return FUNCTION_REFERENCE_REGISTRY.get(functionReferenceId);
  }

  /**
   * Invokes a function reference callback from native code.
   *
   * <p>This method is called by the Rust Panama FFI layer when a FunctionReference created from a
   * host function is invoked.
   *
   * @param functionReferenceId the function reference ID
   * @param params array of parameter values
   * @return array of return values
   * @throws WasmException if the callback fails
   */
  @SuppressWarnings("unused") // Called by native code
  static WasmValue[] invokeFunctionReferenceCallback(
      final long functionReferenceId, final WasmValue[] params) throws WasmException {
    final PanamaFunctionReference funcRef = FUNCTION_REFERENCE_REGISTRY.get(functionReferenceId);
    if (funcRef == null) {
      LOGGER.severe("Function reference not found in registry: " + functionReferenceId);
      throw new WasmException("Function reference not found: " + functionReferenceId);
    }

    if (funcRef.resourceHandle.isClosed()) {
      LOGGER.warning("Attempted to call closed function reference: " + funcRef.functionName);
      throw new WasmException("Function reference has been closed: " + funcRef.functionName);
    }

    if (funcRef.hostFunction == null) {
      LOGGER.severe(
          "Function reference callback called on non-host function: " + funcRef.functionName);
      throw new WasmException("Function reference is not a host function: " + funcRef.functionName);
    }

    try {
      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            "Executing function reference callback: "
                + funcRef.functionName
                + " (ID: "
                + functionReferenceId
                + ")");
      }
      return funcRef.hostFunction.execute(params);
    } catch (final Exception e) {
      LOGGER.severe(
          "Function reference execution failed: " + funcRef.functionName + " - " + e.getMessage());
      throw new WasmException("Function reference execution failed: " + funcRef.functionName, e);
    }
  }

  /**
   * Gets the global callback stub address for use with native function reference creation.
   *
   * <p>This method returns a memory segment pointing to a static callback function that can be
   * passed to native code. When the native code calls this callback, it dispatches to the
   * appropriate Java function reference based on the callback ID.
   *
   * @return the global callback stub memory segment
   */
  public static MemorySegment getGlobalCallbackStub() {
    if (GLOBAL_CALLBACK_STUB == null) {
      synchronized (CALLBACK_STUB_LOCK) {
        if (GLOBAL_CALLBACK_STUB == null) {
          GLOBAL_CALLBACK_STUB = createGlobalCallbackStub();
        }
      }
    }
    return GLOBAL_CALLBACK_STUB;
  }

  /**
   * Creates the global callback stub for native-to-Java function reference callbacks.
   *
   * @return the callback stub memory segment
   */
  private static MemorySegment createGlobalCallbackStub() {
    try {
      // Define the function descriptor for the callback
      // int callback(long callbackId, void* paramsPtr, int paramsLen, void* resultsPtr,
      //              int resultsLen, char* errorMsgPtr, int errorMsgLen)
      final FunctionDescriptor callbackDescriptor =
          FunctionDescriptor.of(
              ValueLayout.JAVA_INT, // return int
              ValueLayout.JAVA_LONG, // callbackId
              ValueLayout.ADDRESS, // paramsPtr
              ValueLayout.JAVA_INT, // paramsLen
              ValueLayout.ADDRESS, // resultsPtr
              ValueLayout.JAVA_INT, // resultsLen
              ValueLayout.ADDRESS, // errorMsgPtr
              ValueLayout.JAVA_INT); // errorMsgLen

      // Get the method handle for invokeNativeCallback
      final java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
      final java.lang.invoke.MethodHandle callbackHandle =
          lookup.findStatic(
              PanamaFunctionReference.class,
              "invokeNativeCallback",
              java.lang.invoke.MethodType.methodType(
                  int.class,
                  long.class,
                  MemorySegment.class,
                  int.class,
                  MemorySegment.class,
                  int.class,
                  MemorySegment.class,
                  int.class));

      // Create the upcall stub using the global arena
      final java.lang.foreign.Linker nativeLinker = java.lang.foreign.Linker.nativeLinker();
      final MemorySegment stub =
          nativeLinker.upcallStub(callbackHandle, callbackDescriptor, GLOBAL_CALLBACK_ARENA);

      LOGGER.fine(
          "Created global function reference callback stub at address: 0x"
              + Long.toHexString(stub.address()));

      return stub;

    } catch (final Exception e) {
      throw new IllegalStateException("Failed to create global callback stub", e);
    }
  }

  /**
   * Native callback entry point for function reference invocation.
   *
   * <p>This static method is called by native code through the global callback stub. It unmarshals
   * parameters, invokes the function reference, and marshals results back.
   *
   * @param callbackId the function reference ID
   * @param paramsPtr pointer to the parameter array
   * @param paramsLen number of parameters
   * @param resultsPtr pointer to the result buffer
   * @param resultsLen expected number of results
   * @param errorMsgPtr pointer to error message buffer (for writing on failure)
   * @param errorMsgLen size of error message buffer
   * @return 0 on success, non-zero on error
   */
  @SuppressWarnings("unused") // Called by native code through upcall stub
  public static int invokeNativeCallback(
      final long callbackId,
      final MemorySegment paramsPtr,
      final int paramsLen,
      final MemorySegment resultsPtr,
      final int resultsLen,
      final MemorySegment errorMsgPtr,
      final int errorMsgLen) {
    try {
      // Look up the function reference
      final PanamaFunctionReference funcRef = FUNCTION_REFERENCE_REGISTRY.get(callbackId);
      if (funcRef == null) {
        LOGGER.severe("Function reference not found in registry: " + callbackId);
        PanamaErrorMapper.writeErrorMessage(errorMsgPtr, errorMsgLen, "Function reference not found: " + callbackId);
        return -1;
      }

      if (funcRef.resourceHandle.isClosed()) {
        LOGGER.warning("Attempted to call closed function reference: " + funcRef.functionName);
        PanamaErrorMapper.writeErrorMessage(errorMsgPtr, errorMsgLen, "Closed function: " + funcRef.functionName);
        return -2;
      }

      if (funcRef.hostFunction == null) {
        LOGGER.severe(
            "Function reference callback called on non-host function: " + funcRef.functionName);
        PanamaErrorMapper.writeErrorMessage(errorMsgPtr, errorMsgLen, "Not a host function: " + funcRef.functionName);
        return -3;
      }

      // Reinterpret paramsPtr with proper size (may be zero-length from upcall)
      final int valueSize = 20; // FFI format: 4 byte tag + 16 byte value
      final long paramsBufferSize = (long) paramsLen * valueSize;
      final MemorySegment reinterpretedParams =
          paramsLen > 0 ? paramsPtr.reinterpret(paramsBufferSize) : paramsPtr;

      // Unmarshal parameters
      final WasmValue[] params =
          unmarshalParameters(reinterpretedParams, paramsLen, funcRef.functionType);

      // Invoke the host function
      final WasmValue[] results = funcRef.hostFunction.execute(params);

      // Marshal results back to native format
      try {
        // CRITICAL: The resultsPtr from native code is a zero-length segment by default.
        // We must reinterpret it with the proper size before writing.
        final long bufferSize = (long) resultsLen * valueSize;
        final MemorySegment reinterpretedResults = resultsPtr.reinterpret(bufferSize);
        marshalResultsToFfi(results, reinterpretedResults, resultsLen);
      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Error marshaling results in callback: " + callbackId, e);
        PanamaErrorMapper.writeErrorMessage(errorMsgPtr, errorMsgLen, "Marshal error: " + e.getMessage());
        return -5;
      }

      return 0;
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Error in function reference callback: " + callbackId, e);
      // Write the exception message to the error buffer for propagation back to Rust/Wasmtime
      PanamaErrorMapper.writeErrorMessage(errorMsgPtr, errorMsgLen, e.getMessage());
      return -4;
    }
  }


  /**
   * Unmarshals parameters from native FFI format to WasmValue array.
   *
   * @param paramsPtr pointer to the parameter array in FFI format
   * @param paramsLen number of parameters
   * @param functionType the function type for type information
   * @return array of WasmValue parameters
   */
  private static WasmValue[] unmarshalParameters(
      final MemorySegment paramsPtr, final int paramsLen, final FunctionType functionType) {
    if (paramsLen == 0) {
      return new WasmValue[0];
    }

    final WasmValue[] params = new WasmValue[paramsLen];
    final WasmValueType[] paramTypes = functionType.getParamTypes();

    // FFI parameter format: 20 bytes per value (4 byte tag + 16 byte value)
    final int valueSize = 20;

    for (int i = 0; i < paramsLen; i++) {
      final long offset = (long) i * valueSize;
      final int tag = paramsPtr.get(ValueLayout.JAVA_INT, offset);
      final WasmValueType type = i < paramTypes.length ? paramTypes[i] : WasmValueType.I32;

      switch (type) {
        case I32:
          params[i] = WasmValue.i32(paramsPtr.get(ValueLayout.JAVA_INT, offset + 4));
          break;
        case I64:
          params[i] = WasmValue.i64(paramsPtr.get(ValueLayout.JAVA_LONG, offset + 4));
          break;
        case F32:
          params[i] = WasmValue.f32(paramsPtr.get(ValueLayout.JAVA_FLOAT, offset + 4));
          break;
        case F64:
          params[i] = WasmValue.f64(paramsPtr.get(ValueLayout.JAVA_DOUBLE, offset + 4));
          break;
        default:
          // For other types, default to i32
          params[i] = WasmValue.i32(paramsPtr.get(ValueLayout.JAVA_INT, offset + 4));
          break;
      }
    }

    return params;
  }

  /**
   * Marshals WasmValue results back to native FFI format.
   *
   * @param results the result values
   * @param resultsPtr pointer to the result buffer
   * @param resultsLen expected number of results
   */
  private static void marshalResultsToFfi(
      final WasmValue[] results, final MemorySegment resultsPtr, final int resultsLen) {
    if (resultsLen == 0 || results == null || results.length == 0) {
      return;
    }

    // FFI result format: 20 bytes per value (4 byte tag + 16 byte value)
    final int valueSize = 20;

    for (int i = 0; i < Math.min(results.length, resultsLen); i++) {
      final long offset = (long) i * valueSize;
      final WasmValue result = results[i];

      // Set type tag
      resultsPtr.set(ValueLayout.JAVA_INT, offset, result.getType().toNativeTypeCode());

      // Set value
      switch (result.getType()) {
        case I32:
          resultsPtr.set(ValueLayout.JAVA_INT, offset + 4, result.asI32());
          break;
        case I64:
          resultsPtr.set(ValueLayout.JAVA_LONG, offset + 4, result.asI64());
          break;
        case F32:
          resultsPtr.set(ValueLayout.JAVA_FLOAT, offset + 4, result.asF32());
          break;
        case F64:
          resultsPtr.set(ValueLayout.JAVA_DOUBLE, offset + 4, result.asF64());
          break;
        default:
          // For other types, store as i32
          resultsPtr.set(ValueLayout.JAVA_INT, offset + 4, 0);
          break;
      }
    }
  }

  @Override
  public String toString() {
    if (resourceHandle.isClosed()) {
      return "PanamaFunctionReference{name='" + functionName + "', closed=true}";
    }

    final String type = isHostFunction() ? "host" : (isWasmFunction() ? "wasm" : "unknown");
    return String.format(
        "PanamaFunctionReference{name='%s', type=%s, functionType=%s, id=%d, nativeId=%d}",
        functionName, type, functionType, functionReferenceId, nativeRegistryId);
  }
}
