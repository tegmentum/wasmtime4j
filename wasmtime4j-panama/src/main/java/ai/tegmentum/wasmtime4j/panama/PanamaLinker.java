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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.util.TypeConversionUtilities;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
  private static final NativeInstanceBindings NATIVE_INSTANCE_BINDINGS =
      NativeInstanceBindings.getInstance();
  private static final NativeStoreBindings NATIVE_STORE_BINDINGS =
      NativeStoreBindings.getInstance();
  private static final NativeMemoryBindings NATIVE_MEMORY_BINDINGS =
      NativeMemoryBindings.getInstance();

  /** Counter tracking the number of in-flight host function callbacks. */
  private static final AtomicInteger IN_FLIGHT_CALLBACKS = new AtomicInteger(0);

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeLinker;
  private final NativeResourceHandle resourceHandle;
  private final Set<String> imports = new HashSet<>();
  private final java.util.Map<String, ai.tegmentum.wasmtime4j.validation.ImportInfo>
      importRegistry = new java.util.concurrent.ConcurrentHashMap<>();
  private final Set<Long> registeredCallbackIds = new HashSet<>();
  private volatile PanamaWasiContext wasiContext = null;
  private volatile boolean wasiEnabled = false;

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

    this.nativeLinker = NATIVE_INSTANCE_BINDINGS.panamaLinkerCreate(enginePtr);
    if (this.nativeLinker == null || this.nativeLinker.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create native linker");
    }

    final MemorySegment linkerHandle = this.nativeLinker;
    final Arena linkerArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaLinker",
            () -> {
              // Wait for in-flight callbacks before destroying native resources
              waitForInFlightCallbacks();

              // Destroy native linker FIRST, before cleaning up callback map
              if (nativeLinker != null && !nativeLinker.equals(MemorySegment.NULL)) {
                NATIVE_INSTANCE_BINDINGS.panamaLinkerDestroy(nativeLinker);
              }

              // Now safe to clean up callbacks
              cleanupHostFunctionCallbacks();

              arena.close();
            },
            this,
            () -> {
              if (linkerHandle != null && !linkerHandle.equals(MemorySegment.NULL)) {
                NATIVE_INSTANCE_BINDINGS.panamaLinkerDestroy(linkerHandle);
              }
              linkerArena.close();
            });

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
    final int[] paramTypes = TypeConversionUtilities.toNativeTypes(functionType.getParamTypes());
    final int[] returnTypes = TypeConversionUtilities.toNativeTypes(functionType.getReturnTypes());

    // Register callback and get ID
    final long callbackId = registerHostFunctionCallback(moduleName, name, implementation);

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
          NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineHostFunction(
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

      addImportWithMetadata(
          moduleName,
          name,
          ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION,
          functionType.toString());

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

    final int result;

    // For instance-exported memories, use panamaLinkerDefineMemoryFromInstance to avoid
    // store mismatch issues. This keeps memory extraction and definition in the same
    // native call, ensuring consistent store context.
    if (panamaMemory.isInstanceExported()) {
      final PanamaInstance owningInstance = panamaMemory.getOwningInstance();
      final String exportName = panamaMemory.getExportName();
      // Get the instance's store - this is the store that created the instance and is needed
      // to extract shared memory from it. The linker's store (panamaStore) may be different
      // (e.g., a thread's store when defining shared memory across threads).
      final PanamaStore instanceStore = (PanamaStore) owningInstance.getStore();
      final MemorySegment exportNamePtr = arena.allocateFrom(exportName);

      // Use the instance's store to extract the memory, not the linker's store
      result =
          NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineMemoryFromInstance(
              nativeLinker,
              instanceStore.getNativeStore(),
              moduleNamePtr,
              namePtr,
              owningInstance.getNativeInstance(),
              exportNamePtr);
    } else {
      // For store-created memories, use the standard path with the memory pointer
      final MemorySegment memoryPtr = panamaMemory.getNativeMemory();
      result =
          NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineMemory(
              nativeLinker, panamaStore.getNativeStore(), moduleNamePtr, namePtr, memoryPtr);
    }

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to define memory: " + moduleName + "::" + name);
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
        NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineTable(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            namePtr,
            panamaTable.getNativeTable());

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to define table: " + moduleName + "::" + name);
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

    final PanamaStore panamaStore = (PanamaStore) store;

    // Get the native global pointer based on the global type
    final MemorySegment globalPtr;
    if (global instanceof PanamaGlobal) {
      globalPtr = ((PanamaGlobal) global).getNativeGlobal();
    } else if (global instanceof PanamaInstanceGlobal) {
      globalPtr = ((PanamaInstanceGlobal) global).getGlobalPointer();
    } else {
      throw new IllegalArgumentException(
          "Global must be a PanamaGlobal or PanamaInstanceGlobal, got: "
              + global.getClass().getName());
    }

    if (globalPtr == null || globalPtr.equals(MemorySegment.NULL)) {
      throw new WasmException(
          "Failed to get native global pointer for: " + moduleName + "::" + name);
    }

    // Allocate C strings for module name and global name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to define global
    final int result =
        NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineGlobal(
            nativeLinker, panamaStore.getNativeStore(), moduleNamePtr, namePtr, globalPtr);

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to define global: " + moduleName + "::" + name);
    }

    LOGGER.fine("Defined global: " + moduleName + "::" + name);
  }

  @Override
  public void defineInstance(final Store store, final String moduleName, final Instance instance)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
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
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }

    final PanamaInstance panamaInstance = (PanamaInstance) instance;
    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate C string for module name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);

    // Call native function to define instance
    final int result =
        NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineInstance(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            panamaInstance.getNativeInstance());

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to define instance: " + moduleName);
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

    // Allocate C strings
    final MemorySegment fromModulePtr = arena.allocateFrom(fromModule);
    final MemorySegment fromNamePtr = arena.allocateFrom(fromName);
    final MemorySegment toModulePtr = arena.allocateFrom(toModule);
    final MemorySegment toNamePtr = arena.allocateFrom(toName);

    // Call native function to create alias
    final int result =
        NATIVE_INSTANCE_BINDINGS.panamaLinkerAlias(
            nativeLinker, fromModulePtr, fromNamePtr, toModulePtr, toNamePtr);

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result,
          "Failed to create alias from "
              + fromModule
              + "::"
              + fromName
              + " to "
              + toModule
              + "::"
              + toName);
    }

    LOGGER.fine(
        "Created alias from " + fromModule + "::" + fromName + " to " + toModule + "::" + toName);
  }

  @Override
  public void aliasModule(final String module, final String asModule) throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (asModule == null) {
      throw new IllegalArgumentException("Alias module name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment modulePtr = arena.allocateFrom(module);
    final MemorySegment asModulePtr = arena.allocateFrom(asModule);

    final int result =
        NATIVE_INSTANCE_BINDINGS.panamaLinkerAliasModule(nativeLinker, modulePtr, asModulePtr);

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to alias module '" + module + "' as '" + asModule + "'");
    }

    LOGGER.fine("Aliased module '" + module + "' as '" + asModule + "'");
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

    // If we have a WASI context, attach it to the store before instantiation
    if (wasiContext != null) {
      final int hasWasi = NATIVE_STORE_BINDINGS.storeHasWasiContext(panamaStore.getNativeStore());
      if (hasWasi == 0) {
        // Store doesn't have WASI context yet, attach it
        final int result =
            NATIVE_STORE_BINDINGS.storeSetWasiContext(
                panamaStore.getNativeStore(), wasiContext.getNativeContext());
        if (result != 0) {
          throw PanamaErrorMapper.mapNativeError(result, "Failed to attach WASI context to store");
        }
        panamaStore.setTrackedWasiContext(wasiContext);
        LOGGER.fine("Attached WASI context to store before instantiation");
      }
    }

    // Call native function to instantiate module using linker
    final MemorySegment instancePtr =
        NATIVE_INSTANCE_BINDINGS.panamaLinkerInstantiate(
            nativeLinker, panamaStore.getNativeStore(), panamaModule.getNativeModule());

    if (instancePtr == null || instancePtr.address() == 0) {
      // Retrieve detailed error message from native side
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to instantiate module via linker");
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
    defineInstance(store, moduleName, instance);

    LOGGER.fine("Instantiated and registered module: " + moduleName);

    return instance;
  }

  @Override
  public ai.tegmentum.wasmtime4j.InstancePre instantiatePre(final Module module)
      throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule for Panama linker");
    }

    final PanamaModule panamaModule = (PanamaModule) module;

    // Call native function to create InstancePre
    final MemorySegment instancePrePtr =
        NATIVE_INSTANCE_BINDINGS.linkerInstantiatePre(nativeLinker, panamaModule.getNativeModule());

    if (instancePrePtr == null || instancePrePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create InstancePre for module");
    }

    LOGGER.fine("Created InstancePre for module");

    return new PanamaInstancePre(instancePrePtr, module, engine);
  }

  @Override
  public void enableWasi() throws WasmException {
    ensureNotClosed();

    // Check if WASI is already enabled - skip if so to avoid duplicate definition errors
    if (wasiEnabled) {
      LOGGER.fine("WASI already enabled for linker, skipping");
      return;
    }

    // Add WASI Preview 1 imports to the linker
    final int result = NATIVE_INSTANCE_BINDINGS.linkerAddWasi(nativeLinker);

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to enable WASI");
    }

    wasiEnabled = true;
    LOGGER.fine("Enabled WASI for linker");
  }

  /**
   * Checks if WASI has been enabled for this linker.
   *
   * @return true if WASI is enabled, false otherwise
   */
  public boolean isWasiEnabled() {
    return wasiEnabled;
  }

  /**
   * Sets the WASI context for this linker.
   *
   * <p>The WASI context will be automatically attached to the store during instantiation.
   *
   * @param wasiCtx the WASI context to use
   */
  public void setWasiContext(final PanamaWasiContext wasiCtx) {
    this.wasiContext = wasiCtx;
  }

  /**
   * Gets the WASI context set on this linker.
   *
   * @return the WASI context, or null if not set
   */
  public PanamaWasiContext getWasiContext() {
    return this.wasiContext;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public boolean hasImport(final String moduleName, final String name) {
    ensureNotClosed();
    return ai.tegmentum.wasmtime4j.util.LinkerSupport.hasImport(imports, moduleName, name);
  }

  @Override
  public ImportValidation validateImports(final Module... modules) {
    ensureNotClosed();
    return ai.tegmentum.wasmtime4j.util.LinkerSupport.validateImports(imports, modules);
  }

  @Override
  public List<ImportInfo> getImportRegistry() {
    ensureNotClosed();
    return new ArrayList<>(importRegistry.values());
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Waits for in-flight host function callbacks to complete.
   *
   * <p>This prevents a race condition where callbacks lookup wrappers that have been removed during
   * cleanup.
   */
  private void waitForInFlightCallbacks() {
    final int maxWaitMs = 5000;
    final int pollIntervalMs = 10;
    int waited = 0;
    while (IN_FLIGHT_CALLBACKS.get() > 0 && waited < maxWaitMs) {
      try {
        Thread.sleep(pollIntervalMs);
        waited += pollIntervalMs;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.warning("Interrupted while waiting for in-flight callbacks");
        break;
      }
    }
    if (waited >= maxWaitMs) {
      LOGGER.warning("Timed out waiting for in-flight callbacks, proceeding with cleanup");
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
                  int.class,
                  MemorySegment.class,
                  int.class));

      // Create the upcall stub
      final java.lang.foreign.Linker nativeLinker = java.lang.foreign.Linker.nativeLinker();
      final MemorySegment stub = nativeLinker.upcallStub(callbackHandle, callbackDescriptor, arena);
      LOGGER.fine("Created upcall stub at address: 0x" + Long.toHexString(stub.address()));

      return stub;

    } catch (final Exception e) {
      throw new IllegalStateException("Failed to create callback upcall stub", e);
    }
  }

  /**
   * Adds an import to the registry with full metadata.
   *
   * @param moduleName the module name
   * @param name the import name
   * @param importKind the import kind
   * @param typeSignature the type signature (optional)
   */
  void addImportWithMetadata(
      final String moduleName,
      final String name,
      final ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind importKind,
      final String typeSignature) {
    ai.tegmentum.wasmtime4j.util.LinkerSupport.addImportWithMetadata(
        imports, importRegistry, moduleName, name, importKind, typeSignature);
  }

  /**
   * Adds an import to the registry for tracking purposes.
   *
   * @param moduleName the module name
   * @param name the import name
   */
  void addImport(final String moduleName, final String name) {
    ai.tegmentum.wasmtime4j.util.LinkerSupport.addImport(imports, moduleName, name);
  }

  /**
   * Ensures the linker is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Registers a host function callback.
   *
   * @param moduleName the module name
   * @param name the function name
   * @param implementation the implementation
   * @return callback ID for native code to invoke
   */
  private long registerHostFunctionCallback(
      final String moduleName, final String name, final HostFunction implementation) {
    final HostFunctionWrapper wrapper = new HostFunctionWrapper(moduleName, name, implementation);
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
   * @param errorMsgPtr pointer to error message buffer (for writing on failure)
   * @param errorMsgLen size of error message buffer
   * @return 0 on success, non-zero on error
   */
  @SuppressWarnings("unused") // Called from native code via function pointer
  public static int invokeHostFunctionCallback(
      final long callbackId,
      final MemorySegment paramsPtr,
      final int paramsLen,
      final MemorySegment resultsPtr,
      final int resultsLen,
      final MemorySegment errorMsgPtr,
      final int errorMsgLen) {
    // Track in-flight callbacks to prevent race conditions during close()
    IN_FLIGHT_CALLBACKS.incrementAndGet();
    try {
      LOGGER.fine(
          "invokeHostFunctionCallback - Called with callbackId="
              + callbackId
              + ", paramsLen="
              + paramsLen);

      final HostFunctionWrapper wrapper = HOST_FUNCTION_CALLBACKS.get(callbackId);
      if (wrapper == null) {
        LOGGER.severe("Host function callback not found for callbackId=" + callbackId);
        PanamaErrorMapper.writeErrorMessage(
            errorMsgPtr, errorMsgLen, "Callback not found: " + callbackId);
        return -1; // Error: callback not found
      }

      // Reinterpret memory segments with correct sizes
      // Each WasmValue in native memory is 20 bytes (4-byte tag + 16-byte value)
      final long paramsBytes = paramsLen * 20L;
      final MemorySegment paramsSegment =
          paramsLen > 0 ? paramsPtr.reinterpret(paramsBytes) : paramsPtr;

      // Unmarshal parameters from native memory
      final WasmValue[] params = new WasmValue[paramsLen];
      for (int i = 0; i < paramsLen; i++) {
        params[i] = WasmValueMarshaller.unmarshalWasmValue(paramsSegment, i, null);
      }

      // Call the host function
      LOGGER.fine("Executing host function: " + wrapper.moduleName + "::" + wrapper.name);
      final WasmValue[] results = wrapper.getImplementation().execute(params);

      // Validate result count
      if (results.length != resultsLen) {
        LOGGER.severe(
            "Host function returned " + results.length + " values but expected " + resultsLen);
        PanamaErrorMapper.writeErrorMessage(
            errorMsgPtr,
            errorMsgLen,
            "Wrong result count: expected " + resultsLen + ", got " + results.length);
        return -3; // Error: wrong number of results
      }

      // Reinterpret results segment with correct size
      final long resultsBytes = resultsLen * 20L;
      final MemorySegment resultsSegment =
          resultsLen > 0 ? resultsPtr.reinterpret(resultsBytes) : resultsPtr;

      // Marshal results to native memory
      for (int i = 0; i < results.length; i++) {
        WasmValueMarshaller.marshalWasmValue(results[i], resultsSegment, i, null);
      }

      LOGGER.fine(
          "invokeHostFunctionCallback - Completed successfully with "
              + results.length
              + " results");
      return 0; // Success
    } catch (final Exception e) {
      LOGGER.log(java.util.logging.Level.SEVERE, "Host function execution failed", e);
      // Write the exception message to the error buffer for propagation back to Rust/Wasmtime
      PanamaErrorMapper.writeErrorMessage(errorMsgPtr, errorMsgLen, e.getMessage());
      return -2; // Error: exception during execution
    } finally {
      IN_FLIGHT_CALLBACKS.decrementAndGet();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Linker<T> allowShadowing(final boolean allow) {
    ensureNotClosed();
    final int result = NATIVE_INSTANCE_BINDINGS.linkerAllowShadowing(nativeLinker, allow ? 1 : 0);
    if (result != 0) {
      LOGGER.warning(
          "Failed to set allow shadowing: " + PanamaErrorMapper.getErrorDescription(result));
    }
    return this;
  }

  @Override
  public ai.tegmentum.wasmtime4j.Linker<T> allowUnknownExports(final boolean allow) {
    ensureNotClosed();
    final int result =
        NATIVE_INSTANCE_BINDINGS.linkerAllowUnknownExports(nativeLinker, allow ? 1 : 0);
    if (result != 0) {
      LOGGER.warning(
          "Failed to set allow unknown exports: " + PanamaErrorMapper.getErrorDescription(result));
    }
    return this;
  }

  @Override
  public void defineUnknownImportsAsTraps(final Store store, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    final int result =
        NATIVE_INSTANCE_BINDINGS.linkerDefineUnknownImportsAsTraps(
            nativeLinker, panamaStore.getNativeStore(), panamaModule.getNativeModule());

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to define unknown imports as traps");
    }

    LOGGER.fine("Defined unknown imports as traps");
  }

  @Override
  public void defineUnknownImportsAsDefaultValues(final Store store, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    final int result =
        NATIVE_INSTANCE_BINDINGS.linkerDefineUnknownImportsAsDefaultValues(
            nativeLinker, panamaStore.getNativeStore(), panamaModule.getNativeModule());

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to define unknown imports as default values");
    }

    LOGGER.fine("Defined unknown imports as default values");
  }

  @Override
  public void funcNewUnchecked(
      final Store store,
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
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
    final int[] paramTypes = TypeConversionUtilities.toNativeTypes(functionType.getParamTypes());
    final int[] returnTypes = TypeConversionUtilities.toNativeTypes(functionType.getReturnTypes());

    // Register callback and get ID
    final long callbackId = registerHostFunctionCallback(moduleName, name, implementation);

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

      // Call native function to define unchecked host function
      final int result =
          NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineHostFunctionUnchecked(
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
        throw new WasmException(
            "Failed to define unchecked host function: " + moduleName + "::" + name);
      }

      addImportWithMetadata(
          moduleName,
          name,
          ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION,
          functionType.toString());

      LOGGER.fine("Defined unchecked function: " + moduleName + "::" + name);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException(
          "Failed to define unchecked host function: " + moduleName + "::" + name, e);
    }
  }

  @Override
  public Iterable<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> iter() {
    ensureNotClosed();
    return ai.tegmentum.wasmtime4j.util.LinkerSupport.iterDefinitions(importRegistry);
  }

  @Override
  public Iterable<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> iter(
      final ai.tegmentum.wasmtime4j.Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final int nameBufferSize = 1024;

    try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment outCount =
          arena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);

      final int iterResult =
          NATIVE_INSTANCE_BINDINGS.linkerIter(nativeLinker, panamaStore.getNativeStore(), outCount);

      if (iterResult != 0) {
        throw new WasmException("Failed to iterate linker definitions");
      }

      final int count = outCount.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);
      final java.util.List<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> definitions =
          new java.util.ArrayList<>(count);

      for (int i = 0; i < count; i++) {
        final java.lang.foreign.MemorySegment moduleNameBuf = arena.allocate(nameBufferSize);
        final java.lang.foreign.MemorySegment itemNameBuf = arena.allocate(nameBufferSize);
        final java.lang.foreign.MemorySegment outTypeCode =
            arena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);

        final int getResult =
            NATIVE_INSTANCE_BINDINGS.linkerIterGet(
                i, moduleNameBuf, nameBufferSize, itemNameBuf, nameBufferSize, outTypeCode);

        if (getResult != 0) {
          throw new WasmException("Failed to get linker definition at index " + i);
        }

        final String moduleName = moduleNameBuf.getString(0);
        final String itemName = itemNameBuf.getString(0);
        final int typeCode = outTypeCode.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);

        definitions.add(
            new ai.tegmentum.wasmtime4j.Linker.LinkerDefinition(
                moduleName, itemName, ai.tegmentum.wasmtime4j.type.ExternType.fromCode(typeCode)));
      }

      return java.util.Collections.unmodifiableList(definitions);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Extern getByImport(
      final Store store, final String moduleName, final String name) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }

    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate C strings for module name and item name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to get the extern
    final MemorySegment externPtr =
        NATIVE_INSTANCE_BINDINGS.linkerGetByImport(
            nativeLinker, panamaStore.getNativeStore(), moduleNamePtr, namePtr);

    if (externPtr == null || externPtr.equals(MemorySegment.NULL)) {
      return null;
    }

    // Determine extern type and wrap appropriately
    final int externTypeCode = NATIVE_INSTANCE_BINDINGS.externGetType(externPtr);
    switch (externTypeCode) {
      case 0: // FUNC
        return new PanamaExternFunc(externPtr, panamaStore);
      case 1: // TABLE
        return new PanamaExternTable(externPtr, panamaStore);
      case 2: // MEMORY
        return new PanamaExternMemory(externPtr, panamaStore);
      case 3: // GLOBAL
        return new PanamaExternGlobal(externPtr, panamaStore);
      default:
        LOGGER.warning("Unknown extern type code: " + externTypeCode);
        return null;
    }
  }

  @Override
  public void defineName(final Store store, final String name, final Extern extern)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (extern == null) {
      throw new IllegalArgumentException("Extern cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore for Panama linker");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final MemorySegment storePtr = panamaStore.getNativeStore();

    // Use empty string for module name since this is a top-level name definition
    final MemorySegment moduleNamePtr = arena.allocateFrom("");
    final MemorySegment namePtr = arena.allocateFrom(name);

    try {
      final int result = defineExternByType(extern, storePtr, moduleNamePtr, namePtr);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to define name: " + name);
      }

      addImportWithMetadata("", name, getExternImportKind(extern), extern.toString());
      LOGGER.fine("Defined name: " + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Error defining name: " + name, e);
    }
  }

  @Override
  public void define(
      final Store store, final String moduleName, final String name, final Extern extern)
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
    if (extern == null) {
      throw new IllegalArgumentException("Extern cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore for Panama linker");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final MemorySegment storePtr = panamaStore.getNativeStore();
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    try {
      final int result = defineExternByType(extern, storePtr, moduleNamePtr, namePtr);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(
            result, "Failed to define: " + moduleName + "::" + name);
      }

      addImportWithMetadata(moduleName, name, getExternImportKind(extern), extern.toString());
      LOGGER.fine("Defined: " + moduleName + "::" + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Error defining: " + moduleName + "::" + name, e);
    }
  }

  @Override
  public void module(final Store store, final String moduleName, final Module module)
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

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore for Panama linker");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule for Panama linker");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;
    final MemorySegment storePtr = panamaStore.getNativeStore();
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment modulePtr = panamaModule.getNativeModule();

    try {
      final int result =
          NATIVE_INSTANCE_BINDINGS.panamaLinkerModule(
              nativeLinker, storePtr, moduleNamePtr, modulePtr);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to define module: " + moduleName);
      }

      LOGGER.fine("Defined module: " + moduleName);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Error defining module: " + moduleName, e);
    }
  }

  private int defineExternByType(
      final Extern extern,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment namePtr)
      throws WasmException {
    if (extern instanceof PanamaExternFunc) {
      throw new WasmException(
          "Defining a function extern by name is not supported. "
              + "Use defineHostFunction() to register host functions.");
    } else if (extern instanceof PanamaExternMemory) {
      final MemorySegment memPtr = ((PanamaExternMemory) extern).getNativeHandle();
      return NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineMemory(
          nativeLinker, storePtr, moduleNamePtr, namePtr, memPtr);
    } else if (extern instanceof PanamaExternTable) {
      final MemorySegment tablePtr = ((PanamaExternTable) extern).getNativeHandle();
      return NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineTable(
          nativeLinker, storePtr, moduleNamePtr, namePtr, tablePtr);
    } else if (extern instanceof PanamaExternGlobal) {
      final MemorySegment globalPtr = ((PanamaExternGlobal) extern).getNativeHandle();
      return NATIVE_INSTANCE_BINDINGS.panamaLinkerDefineGlobal(
          nativeLinker, storePtr, moduleNamePtr, namePtr, globalPtr);
    }
    throw new IllegalArgumentException("Unknown extern type: " + extern.getClass().getName());
  }

  private ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind getExternImportKind(
      final Extern extern) {
    if (extern instanceof PanamaExternFunc) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION;
    } else if (extern instanceof PanamaExternTable) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.TABLE;
    } else if (extern instanceof PanamaExternMemory) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.MEMORY;
    } else if (extern instanceof PanamaExternGlobal) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.GLOBAL;
    }
    return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION;
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmFunction getDefault(
      final Store store, final String moduleName) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }

    final PanamaStore panamaStore = (PanamaStore) store;

    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment = tempArena.allocateFrom(moduleName);
      final MemorySegment funcHandle =
          NATIVE_INSTANCE_BINDINGS.linkerGetDefault(
              nativeLinker, panamaStore.getNativeStore(), nameSegment);

      if (funcHandle == null
          || funcHandle.equals(MemorySegment.NULL)
          || funcHandle.address() == 0) {
        return null;
      }

      return new PanamaCallerFunction(funcHandle, panamaStore, moduleName);
    }
  }

  /** Wrapper for host function callbacks. */
  private static class HostFunctionWrapper {
    private static final AtomicLong nextId = new AtomicLong(1);

    private final long id;
    private final String moduleName;
    private final String name;
    private final HostFunction implementation;

    HostFunctionWrapper(
        final String moduleName, final String name, final HostFunction implementation) {
      this.id = nextId.getAndIncrement();
      this.moduleName = moduleName;
      this.name = name;
      this.implementation = implementation;
    }

    long getId() {
      return id;
    }

    HostFunction getImplementation() {
      return implementation;
    }
  }
}
