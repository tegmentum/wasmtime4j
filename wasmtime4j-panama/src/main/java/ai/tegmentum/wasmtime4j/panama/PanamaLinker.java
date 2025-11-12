package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.DependencyResolution;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.ImportInfo;
import ai.tegmentum.wasmtime4j.ImportValidation;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstantiationPlan;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Linker.
 *
 * @param <T> the type of user data associated with stores
 * @since 1.0.0
 */
public final class PanamaLinker<T> implements ai.tegmentum.wasmtime4j.Linker<T> {
  private static final Logger LOGGER = Logger.getLogger(PanamaLinker.class.getName());
  private static final ConcurrentHashMap<Long, HostFunctionWrapper> HOST_FUNCTION_CALLBACKS =
      new ConcurrentHashMap<>();
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeLinker;
  private volatile boolean closed = false;
  private final Set<Long> registeredCallbackIds = new HashSet<>();

  /**
   * Creates a new Panama linker.
   *
   * @param engine the engine to create the linker for
   * @throws WasmException if linker creation fails
   */
  public PanamaLinker(final PanamaEngine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    this.engine = engine;
    this.arena = Arena.ofShared();

    // Create native linker via Panama FFI
    final MemorySegment enginePtr = engine.getNativeEngine();
    if (enginePtr == null || enginePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Engine has invalid native handle");
    }

    this.nativeLinker = NATIVE_BINDINGS.panamaLinkerCreate(enginePtr);
    if (this.nativeLinker == null || this.nativeLinker.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create native linker");
    }

    LOGGER.fine("Created Panama linker");
  }

  @Override
  public void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    // Convert FunctionType to native representation
    final int[] paramTypes = toNativeTypes(functionType.getParamTypes());
    final int[] returnTypes = toNativeTypes(functionType.getReturnTypes());

    // Register callback and get ID
    final long callbackId =
        registerHostFunctionCallback(moduleName, name, implementation, functionType);

    try {
      // Create upcall stub for the callback function
      final MemorySegment callbackStub = createCallbackStub();

      // Allocate native memory for strings and arrays
      final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
      final MemorySegment namePtr = arena.allocateFrom(name);

      // Allocate and copy parameter types
      final MemorySegment paramTypesPtr = arena.allocate(ValueLayout.JAVA_INT, paramTypes.length);
      for (int i = 0; i < paramTypes.length; i++) {
        paramTypesPtr.setAtIndex(ValueLayout.JAVA_INT, i, paramTypes[i]);
      }

      // Allocate and copy return types
      final MemorySegment returnTypesPtr = arena.allocate(ValueLayout.JAVA_INT, returnTypes.length);
      for (int i = 0; i < returnTypes.length; i++) {
        returnTypesPtr.setAtIndex(ValueLayout.JAVA_INT, i, returnTypes[i]);
      }

      // Call native function to define host function
      final int result =
          NATIVE_BINDINGS.panamaLinkerDefineHostFunction(
              nativeLinker,
              moduleNamePtr,
              namePtr,
              paramTypesPtr,
              paramTypes.length,
              returnTypesPtr,
              returnTypes.length,
              callbackStub,
              callbackId);

      if (result != 0) {
        throw new WasmException("Failed to define host function: " + moduleName + "::" + name);
      }

      LOGGER.fine(
          "Defined host function: "
              + moduleName
              + "::"
              + name
              + " (callback ID: "
              + callbackId
              + ")");
    } catch (final Exception e) {
      // Unregister callback on failure
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Error defining host function: " + moduleName + "::" + name, e);
    }
  }

  @Override
  public void defineMemory(
      final Store store, final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("Memory must be a PanamaMemory");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaMemory panamaMemory = (PanamaMemory) memory;

    // Allocate C strings for module name and memory name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to define memory
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineMemory(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            namePtr,
            panamaMemory.getNativeMemory());

    if (result != 0) {
      throw new WasmException(
          "Failed to define memory: " + moduleName + "::" + name + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined memory: " + moduleName + "::" + name);
  }

  @Override
  public void defineTable(
      final Store store, final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(table instanceof PanamaTable)) {
      throw new IllegalArgumentException("Table must be a PanamaTable");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaTable panamaTable = (PanamaTable) table;

    // Allocate C strings for module name and table name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to define table
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineTable(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            namePtr,
            panamaTable.getNativeTable());

    if (result != 0) {
      throw new WasmException(
          "Failed to define table: " + moduleName + "::" + name + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined table: " + moduleName + "::" + name);
  }

  @Override
  public void defineGlobal(
      final Store store, final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (global == null) {
      throw new IllegalArgumentException("Global cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(global instanceof PanamaGlobal)) {
      throw new IllegalArgumentException("Global must be a PanamaGlobal");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaGlobal panamaGlobal = (PanamaGlobal) global;

    // Allocate C strings for module name and global name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to define global
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineGlobal(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            namePtr,
            panamaGlobal.getNativeGlobal());

    if (result != 0) {
      throw new WasmException(
          "Failed to define global: " + moduleName + "::" + name + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined global: " + moduleName + "::" + name);
  }

  @Override
  public void defineInstance(final String moduleName, final Instance instance)
      throws WasmException {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementation
    if (!(instance instanceof PanamaInstance)) {
      throw new IllegalArgumentException("Instance must be a PanamaInstance");
    }

    final PanamaInstance panamaInstance = (PanamaInstance) instance;

    // Get the store from the instance
    final Store store = panamaInstance.getStore();
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate C string for module name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);

    // Call native function to define instance
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineInstance(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            panamaInstance.getNativeInstance());

    if (result != 0) {
      throw new WasmException(
          "Failed to define instance: " + moduleName + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined instance: " + moduleName);
  }

  @Override
  public void alias(
      final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException {
    if (fromModule == null) {
      throw new IllegalArgumentException("From module cannot be null");
    }
    if (fromName == null) {
      throw new IllegalArgumentException("From name cannot be null");
    }
    if (toModule == null) {
      throw new IllegalArgumentException("To module cannot be null");
    }
    if (toName == null) {
      throw new IllegalArgumentException("To name cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement aliasing
    throw new UnsupportedOperationException("Aliasing not yet implemented");
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    // Call native function to instantiate module using linker
    final MemorySegment instancePtr =
        NATIVE_BINDINGS.panamaLinkerInstantiate(
            nativeLinker, panamaStore.getNativeStore(), panamaModule.getNativeModule());

    if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to instantiate module via linker");
    }

    // Wrap the native instance pointer
    return new PanamaInstance(instancePtr, panamaModule, panamaStore);
  }

  @Override
  public Instance instantiate(final Store store, final String moduleName, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    // Instantiate the module using the linker
    final Instance instance = instantiate(store, module);

    // Define the instance in the linker under the specified module name
    // This allows other modules to import from this instance
    defineInstance(moduleName, instance);

    LOGGER.fine("Instantiated and registered module: " + moduleName);

    return instance;
  }

  @Override
  public void enableWasi() throws WasmException {
    ensureNotClosed();

    // Call native function to enable WASI
    final int result = NATIVE_BINDINGS.panamaLinkerEnableWasi(nativeLinker);

    if (result != 0) {
      throw new WasmException("Failed to enable WASI (error code: " + result + ")");
    }

    LOGGER.fine("Enabled WASI for linker");
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !closed;
  }

  @Override
  public boolean hasImport(final String moduleName, final String name) {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement import check
    return false;
  }

  @Override
  public DependencyResolution resolveDependencies(final Module... modules) throws WasmException {
    if (modules == null || modules.length == 0) {
      throw new IllegalArgumentException("Modules cannot be null or empty");
    }
    ensureNotClosed();
    // TODO: Implement dependency resolution
    throw new UnsupportedOperationException("Dependency resolution not yet implemented");
  }

  @Override
  public ImportValidation validateImports(final Module... modules) {
    if (modules == null || modules.length == 0) {
      throw new IllegalArgumentException("Modules cannot be null or empty");
    }
    ensureNotClosed();
    // TODO: Implement import validation
    throw new UnsupportedOperationException("Import validation not yet implemented");
  }

  @Override
  public List<ImportInfo> getImportRegistry() {
    ensureNotClosed();
    // TODO: Implement import registry
    return Collections.emptyList();
  }

  @Override
  public InstantiationPlan createInstantiationPlan(final Module... modules) throws WasmException {
    if (modules == null || modules.length == 0) {
      throw new IllegalArgumentException("Modules cannot be null or empty");
    }
    ensureNotClosed();
    // TODO: Implement instantiation plan creation
    throw new UnsupportedOperationException("Instantiation plan not yet implemented");
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      cleanupHostFunctionCallbacks();
      // Destroy native linker
      if (nativeLinker != null && !nativeLinker.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.panamaLinkerDestroy(nativeLinker);
      }
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama linker");
    } catch (final Exception e) {
      LOGGER.warning("Error closing linker: " + e.getMessage());
    }
  }

  /** Cleans up host function callbacks registered by this linker instance. */
  private void cleanupHostFunctionCallbacks() {
    for (final Long callbackId : registeredCallbackIds) {
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
    }
    registeredCallbackIds.clear();
  }

  /**
   * Gets the native linker pointer.
   *
   * @return native linker memory segment
   */
  public MemorySegment getNativeLinker() {
    return nativeLinker;
  }

  /**
   * Creates an upcall stub for the host function callback.
   *
   * @return memory segment pointing to the callback function
   */
  private MemorySegment createCallbackStub() {
    try {
      // Define the function descriptor for the callback
      // int callback(long callbackId, void* paramsPtr, int paramsLen, void* resultsPtr, int
      // resultsLen)
      final FunctionDescriptor callbackDescriptor =
          FunctionDescriptor.of(
              ValueLayout.JAVA_INT, // return int
              ValueLayout.JAVA_LONG, // callbackId
              ValueLayout.ADDRESS, // paramsPtr
              ValueLayout.JAVA_INT, // paramsLen
              ValueLayout.ADDRESS, // resultsPtr
              ValueLayout.JAVA_INT); // resultsLen

      // Get the method handle for invokeHostFunctionCallback
      final java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
      final java.lang.invoke.MethodHandle callbackHandle =
          lookup.findStatic(
              PanamaLinker.class,
              "invokeHostFunctionCallback",
              java.lang.invoke.MethodType.methodType(
                  int.class,
                  long.class,
                  MemorySegment.class,
                  int.class,
                  MemorySegment.class,
                  int.class));

      // Create the upcall stub
      final java.lang.foreign.Linker nativeLinker = java.lang.foreign.Linker.nativeLinker();
      return nativeLinker.upcallStub(callbackHandle, callbackDescriptor, arena);

    } catch (final Exception e) {
      throw new IllegalStateException("Failed to create callback upcall stub", e);
    }
  }

  /**
   * Ensures the linker is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Linker has been closed");
    }
  }

  /**
   * Converts WasmValueTypes to native type codes.
   *
   * @param types the value types
   * @return array of native type codes
   */
  private int[] toNativeTypes(final WasmValueType[] types) {
    if (types == null || types.length == 0) {
      return new int[0];
    }

    final int[] nativeTypes = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      nativeTypes[i] = types[i].toNativeTypeCode();
    }
    return nativeTypes;
  }

  /**
   * Registers a host function callback.
   *
   * @param moduleName the module name
   * @param name the function name
   * @param implementation the implementation
   * @param functionType the function type
   * @return callback ID for native code to invoke
   */
  private long registerHostFunctionCallback(
      final String moduleName,
      final String name,
      final HostFunction implementation,
      final FunctionType functionType) {
    final HostFunctionWrapper wrapper =
        new HostFunctionWrapper(moduleName, name, implementation, functionType);
    final long id = wrapper.getId();
    HOST_FUNCTION_CALLBACKS.put(id, wrapper);
    registeredCallbackIds.add(id);
    return id;
  }

  /**
   * Callback function invoked from native code when a host function is called.
   *
   * <p>This method is called via Panama FFI function pointer.
   *
   * @param callbackId the callback ID
   * @param paramsPtr pointer to parameter array
   * @param paramsLen number of parameters
   * @param resultsPtr pointer to results buffer
   * @param resultsLen expected number of results
   * @return 0 on success, non-zero on error
   */
  @SuppressWarnings("unused") // Called from native code via function pointer
  private static int invokeHostFunctionCallback(
      final long callbackId,
      final MemorySegment paramsPtr,
      final int paramsLen,
      final MemorySegment resultsPtr,
      final int resultsLen) {
    try {
      LOGGER.info(
          "invokeHostFunctionCallback - Called with callbackId="
              + callbackId
              + ", paramsLen="
              + paramsLen);

      final HostFunctionWrapper wrapper = HOST_FUNCTION_CALLBACKS.get(callbackId);
      if (wrapper == null) {
        LOGGER.severe("Host function callback not found for callbackId=" + callbackId);
        return -1; // Error
      }

      // Unmarshal parameters from native memory
      final WasmValue[] params = new WasmValue[paramsLen];
      for (int i = 0; i < paramsLen; i++) {
        // Each WasmValue in native memory is represented as a tagged union
        // For now, we'll need to read the structure from native memory
        // TODO: This requires understanding the WasmValue native layout
        params[i] = unmarshalWasmValue(paramsPtr, i);
      }

      // Call the host function
      LOGGER.fine("Executing host function: " + wrapper.moduleName + "::" + wrapper.name);
      final WasmValue[] results = wrapper.getImplementation().execute(params);

      // Validate result count
      if (results.length != resultsLen) {
        LOGGER.severe(
            "Host function returned " + results.length + " values but expected " + resultsLen);
        return -3; // Error: wrong number of results
      }

      // Marshal results to native memory
      for (int i = 0; i < results.length; i++) {
        marshalWasmValue(results[i], resultsPtr, i);
      }

      LOGGER.info(
          "invokeHostFunctionCallback - Completed successfully with "
              + results.length
              + " results");
      return 0; // Success
    } catch (final Exception e) {
      LOGGER.log(java.util.logging.Level.SEVERE, "Host function execution failed", e);
      return -2; // Error
    }
  }

  /**
   * Unmarshals a WasmValue from native memory.
   *
   * @param ptr pointer to the WasmValue array
   * @param index index in the array
   * @return the unmarshaled WasmValue
   */
  private static WasmValue unmarshalWasmValue(final MemorySegment ptr, final int index) {
    // WasmValue native layout (from Rust):
    // - tag (int): 0=I32, 1=I64, 2=F32, 3=F64, 4=V128
    // - value (union of i32, i64, f32, f64, or 16 bytes for v128)
    // Total size: 4 (tag) + 16 (largest value) = 20 bytes per WasmValue

    final long offset = index * 20L;
    final int tag = ptr.get(ValueLayout.JAVA_INT, offset);

    switch (tag) {
      case 0: // I32
        final int i32Val = ptr.get(ValueLayout.JAVA_INT, offset + 4);
        return WasmValue.i32(i32Val);

      case 1: // I64
        final long i64Val = ptr.get(ValueLayout.JAVA_LONG, offset + 4);
        return WasmValue.i64(i64Val);

      case 2: // F32
        final float f32Val = ptr.get(ValueLayout.JAVA_FLOAT, offset + 4);
        return WasmValue.f32(f32Val);

      case 3: // F64
        final double f64Val = ptr.get(ValueLayout.JAVA_DOUBLE, offset + 4);
        return WasmValue.f64(f64Val);

      case 4: // V128
        final byte[] v128Bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
          v128Bytes[i] = ptr.get(ValueLayout.JAVA_BYTE, offset + 4 + i);
        }
        return WasmValue.v128(v128Bytes);

      default:
        throw new IllegalArgumentException("Unknown WasmValue tag: " + tag);
    }
  }

  /**
   * Marshals a WasmValue to native memory.
   *
   * @param value the WasmValue to marshal
   * @param ptr pointer to the results array
   * @param index index in the array
   */
  private static void marshalWasmValue(
      final WasmValue value, final MemorySegment ptr, final int index) {
    final long offset = index * 20L;

    switch (value.getType()) {
      case I32:
        ptr.set(ValueLayout.JAVA_INT, offset, 0); // tag
        ptr.set(ValueLayout.JAVA_INT, offset + 4, value.asI32());
        break;

      case I64:
        ptr.set(ValueLayout.JAVA_INT, offset, 1); // tag
        ptr.set(ValueLayout.JAVA_LONG, offset + 4, value.asI64());
        break;

      case F32:
        ptr.set(ValueLayout.JAVA_INT, offset, 2); // tag
        ptr.set(ValueLayout.JAVA_FLOAT, offset + 4, value.asF32());
        break;

      case F64:
        ptr.set(ValueLayout.JAVA_INT, offset, 3); // tag
        ptr.set(ValueLayout.JAVA_DOUBLE, offset + 4, value.asF64());
        break;

      case V128:
        ptr.set(ValueLayout.JAVA_INT, offset, 4); // tag
        final byte[] v128Bytes = value.asV128();
        for (int i = 0; i < 16; i++) {
          ptr.set(ValueLayout.JAVA_BYTE, offset + 4 + i, v128Bytes[i]);
        }
        break;

      default:
        throw new IllegalArgumentException("Unsupported WasmValue type: " + value.getType());
    }
  }

  /** Wrapper for host function callbacks. */
  private static class HostFunctionWrapper {
    private static final AtomicLong nextId = new AtomicLong(1);

    private final long id;
    private final String moduleName;
    private final String name;
    private final HostFunction implementation;
    private final FunctionType functionType;

    HostFunctionWrapper(
        final String moduleName,
        final String name,
        final HostFunction implementation,
        final FunctionType functionType) {
      this.id = nextId.getAndIncrement();
      this.moduleName = moduleName;
      this.name = name;
      this.implementation = implementation;
      this.functionType = functionType;
    }

    long getId() {
      return id;
    }

    HostFunction getImplementation() {
      return implementation;
    }

    FunctionType getFunctionType() {
      return functionType;
    }
  }
}
