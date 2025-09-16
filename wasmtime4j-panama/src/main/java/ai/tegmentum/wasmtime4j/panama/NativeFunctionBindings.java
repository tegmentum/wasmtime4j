/*
 * Copyright 2024 Tegmentum AI
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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Type-safe wrappers for native function signatures.
 *
 * <p>This class provides type-safe bindings for all Wasmtime native functions, ensuring
 * compile-time validation of function signatures and providing optimized access through cached
 * method handles.
 *
 * <p>All function bindings are lazily initialized and cached for optimal performance on repeated
 * calls.
 */
public final class NativeFunctionBindings {

  private static final Logger LOGGER = Logger.getLogger(NativeFunctionBindings.class.getName());

  // Singleton instance
  private static volatile NativeFunctionBindings instance;
  private static final Object INSTANCE_LOCK = new Object();

  // Core components
  private final NativeLibraryLoader libraryLoader;
  private final MethodHandleCache methodHandleCache;
  private final ConcurrentHashMap<String, FunctionBinding> functionBindings;

  // Status
  private volatile boolean initialized = false;

  /** Private constructor for singleton pattern. */
  private NativeFunctionBindings() {
    try {
      this.libraryLoader = NativeLibraryLoader.getInstance();
      this.methodHandleCache = new MethodHandleCache();
      this.functionBindings = new ConcurrentHashMap<>();

      // Verify library loader is ready before proceeding
      if (!this.libraryLoader.isLoaded()) {
        throw new IllegalStateException("Native library loader is not properly initialized");
      }

      initializeFunctionBindings();
      this.initialized = true;

      LOGGER.fine("Initialized NativeFunctionBindings successfully");
    } catch (Exception e) {
      LOGGER.severe("Failed to initialize NativeFunctionBindings: " + e.getMessage());
      this.initialized = false;
      throw new RuntimeException("Failed to initialize native function bindings", e);
    }
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeFunctionBindings getInstance() {
    NativeFunctionBindings result = instance;
    if (result == null) {
      synchronized (INSTANCE_LOCK) {
        result = instance;
        if (result == null) {
          try {
            instance = result = new NativeFunctionBindings();
          } catch (RuntimeException e) {
            LOGGER.severe("Failed to create NativeFunctionBindings singleton: " + e.getMessage());
            // Don't store failed instance, allow retry
            throw e;
          }
        }
      }
    }
    return result;
  }

  /**
   * Checks if the bindings are initialized and available.
   *
   * @return true if initialized, false otherwise
   */
  public boolean isInitialized() {
    return initialized && libraryLoader.isLoaded();
  }

  // Engine Functions

  /**
   * Creates a new Wasmtime engine.
   *
   * @return memory segment pointer to the engine, or null on failure
   */
  public MemorySegment engineCreate() {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeFunctionBindings not initialized, cannot create engine");
        return null;
      }

      MemorySegment result = callNativeFunction("wasmtime4j_engine_create", MemorySegment.class);
      if (result == null || result.equals(MemorySegment.NULL)) {
        LOGGER.warning("Engine creation returned null - this may indicate symbol lookup failure");
      } else {
        LOGGER.fine("Engine created successfully: " + result);
      }
      return result;
    } catch (Exception e) {
      LOGGER.severe("Exception during engine creation: " + e.getMessage());
      return null;
    }
  }

  /**
   * Destroys a Wasmtime engine.
   *
   * @param enginePtr pointer to the engine to destroy
   */
  public void engineDestroy(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_engine_destroy", Void.class, enginePtr);
  }

  /**
   * Configures an engine with options.
   *
   * @param enginePtr pointer to the engine
   * @param optionName name of the configuration option
   * @param optionValue value of the configuration option
   * @return 0 on success, negative error code on failure
   */
  public int engineConfigure(
      final MemorySegment enginePtr,
      final MemorySegment optionName,
      final MemorySegment optionValue) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(optionName, "optionName");

    return callNativeFunction(
        "wasmtime4j_engine_configure", Integer.class, enginePtr, optionName, optionValue);
  }

  /**
   * Sets the optimization level for an engine.
   *
   * @param enginePtr pointer to the engine
   * @param level the optimization level (0-2)
   * @return 0 on success, negative error code on failure
   */
  public int engineSetOptimizationLevel(final MemorySegment enginePtr, final int level) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_engine_set_optimization_level", Integer.class, enginePtr, level);
  }

  /**
   * Gets the optimization level for an engine.
   *
   * @param enginePtr pointer to the engine
   * @return the optimization level (0-2)
   */
  public int engineGetOptimizationLevel(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_engine_get_optimization_level", Integer.class, enginePtr);
  }

  /**
   * Sets debug information generation for an engine.
   *
   * @param enginePtr pointer to the engine
   * @param enabled true to enable debug information
   * @return 0 on success, negative error code on failure
   */
  public int engineSetDebugInfo(final MemorySegment enginePtr, final boolean enabled) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_engine_set_debug_info", Integer.class, enginePtr, enabled);
  }

  /**
   * Checks if debug information generation is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if debug information is enabled
   */
  public boolean engineIsDebugInfo(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_engine_is_debug_info", Boolean.class, enginePtr);
  }

  // Module Functions

  /**
   * Compiles a WebAssembly module.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to the WASM bytecode
   * @param wasmSize size of the WASM bytecode
   * @param modulePtr pointer to store the compiled module
   * @return 0 on success, negative error code on failure
   */
  public int moduleCompile(
      final MemorySegment enginePtr,
      final MemorySegment wasmBytes,
      final long wasmSize,
      final MemorySegment modulePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    validatePointer(modulePtr, "modulePtr");
    validateSize(wasmSize, "wasmSize");

    return callNativeFunction(
        "wasmtime4j_module_compile", Integer.class, enginePtr, wasmBytes, wasmSize, modulePtr);
  }

  /**
   * Destroys a WebAssembly module.
   *
   * @param modulePtr pointer to the module to destroy
   */
  public void moduleDestroy(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    callNativeFunction("wasmtime4j_module_destroy", Void.class, modulePtr);
  }

  // Store Functions

  /**
   * Creates a WebAssembly store.
   *
   * @param enginePtr pointer to the engine
   * @param storePtr pointer to store the created store
   * @return 0 on success, negative error code on failure
   */
  public int storeCreate(final MemorySegment enginePtr, final MemorySegment storePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(storePtr, "storePtr");

    return callNativeFunction("wasmtime4j_store_create", Integer.class, enginePtr, storePtr);
  }

  /**
   * Destroys a WebAssembly store.
   *
   * @param storePtr pointer to the store to destroy
   */
  public void storeDestroy(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    callNativeFunction("wasmtime4j_store_destroy", Void.class, storePtr);
  }

  /**
   * Creates a WebAssembly store with custom configuration.
   *
   * @param enginePtr pointer to the engine
   * @param storePtr pointer to store the created store
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @return 0 on success, negative error code on failure
   */
  public int storeCreateWithConfig(
      final MemorySegment enginePtr,
      final MemorySegment storePtr,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxFunctions) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(storePtr, "storePtr");

    return callNativeFunction(
        "wasmtime4j_store_create_with_config",
        Integer.class,
        enginePtr,
        storePtr,
        fuelLimit,
        memoryLimitBytes,
        executionTimeoutSecs,
        maxInstances,
        maxTableElements,
        maxFunctions);
  }

  /**
   * Sets fuel for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuel the fuel amount to set
   * @return 0 on success, negative error code on failure
   */
  public int storeSetFuel(final MemorySegment storePtr, final long fuel) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_set_fuel", Integer.class, storePtr, fuel);
  }

  /**
   * Gets remaining fuel from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuelOutPtr pointer to store the remaining fuel
   * @return 0 on success, negative error code on failure
   */
  public int storeGetFuel(final MemorySegment storePtr, final MemorySegment fuelOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(fuelOutPtr, "fuelOutPtr");
    return callNativeFunction("wasmtime4j_store_get_fuel", Integer.class, storePtr, fuelOutPtr);
  }

  /**
   * Adds fuel to a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuel the fuel amount to add
   * @return 0 on success, negative error code on failure
   */
  public int storeAddFuel(final MemorySegment storePtr, final long fuel) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_add_fuel", Integer.class, storePtr, fuel);
  }

  /**
   * Sets epoch deadline for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param ticks the epoch deadline in ticks
   * @return 0 on success, negative error code on failure
   */
  public int storeSetEpochDeadline(final MemorySegment storePtr, final long ticks) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_store_set_epoch_deadline", Integer.class, storePtr, ticks);
  }

  /**
   * Triggers garbage collection for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGc(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_gc", Integer.class, storePtr);
  }

  /**
   * Gets execution statistics for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param executionCountPtr pointer to store execution count
   * @param fuelConsumedPtr pointer to store fuel consumed
   * @param totalExecutionTimeNsPtr pointer to store total execution time in nanoseconds
   * @return 0 on success, negative error code on failure
   */
  public int storeGetExecutionStats(
      final MemorySegment storePtr,
      final MemorySegment executionCountPtr,
      final MemorySegment fuelConsumedPtr,
      final MemorySegment totalExecutionTimeNsPtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_store_get_execution_stats",
        Integer.class,
        storePtr,
        executionCountPtr,
        fuelConsumedPtr,
        totalExecutionTimeNsPtr);
  }

  /**
   * Gets memory usage statistics for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param totalBytesPtr pointer to store total bytes
   * @param usedBytesPtr pointer to store used bytes
   * @param instanceCountPtr pointer to store instance count
   * @return 0 on success, negative error code on failure
   */
  public int storeGetMemoryUsage(
      final MemorySegment storePtr,
      final MemorySegment totalBytesPtr,
      final MemorySegment usedBytesPtr,
      final MemorySegment instanceCountPtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_store_get_memory_usage",
        Integer.class,
        storePtr,
        totalBytesPtr,
        usedBytesPtr,
        instanceCountPtr);
  }

  /**
   * Validates store functionality.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeValidate(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_validate", Integer.class, storePtr);
  }

  /**
   * Triggers garbage collection in a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGarbageCollect(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_garbage_collect", Integer.class, storePtr);
  }

  /**
   * Gets metadata information from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param metadataPtr pointer to store metadata
   * @param keyPtr pointer to metadata key
   * @param valuePtr pointer to store metadata value
   * @param sizePtr pointer to store size
   * @return 0 on success, negative error code on failure
   */
  public int storeGetMetadata(
      final MemorySegment storePtr,
      final MemorySegment metadataPtr,
      final MemorySegment keyPtr,
      final MemorySegment valuePtr,
      final MemorySegment sizePtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(metadataPtr, "metadataPtr");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(valuePtr, "valuePtr");
    validatePointer(sizePtr, "sizePtr");
    return callNativeFunction(
        "wasmtime4j_store_get_metadata",
        Integer.class,
        storePtr,
        metadataPtr,
        keyPtr,
        valuePtr,
        sizePtr);
  }

  /**
   * Consumes fuel from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuel amount of fuel to consume
   * @param remainingPtr pointer to store remaining fuel
   * @return 0 on success, negative error code on failure
   */
  public int storeConsumeFuel(
      final MemorySegment storePtr, final long fuel, final MemorySegment remainingPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(remainingPtr, "remainingPtr");
    return callNativeFunction(
        "wasmtime4j_store_consume_fuel", Integer.class, storePtr, fuel, remainingPtr);
  }

  // WASI Functions

  /**
   * Creates a new WASI context.
   *
   * @return pointer to the created WASI context, or null pointer on failure
   */
  public MemorySegment wasiContextNew() {
    return callNativeFunction("wasmtime4j_wasi_context_new", MemorySegment.class);
  }

  /**
   * Sets an environment variable in a WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @param keyPtr pointer to the environment variable key string
   * @param valuePtr pointer to the environment variable value string
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextSetEnvironmentVariable(
      final MemorySegment contextPtr, final MemorySegment keyPtr, final MemorySegment valuePtr) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(valuePtr, "valuePtr");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_env", Integer.class, contextPtr, keyPtr, valuePtr);
  }

  /**
   * Sets command line arguments in a WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @param argsPtr pointer to the arguments array
   * @param argsCount number of arguments
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextSetArguments(
      final MemorySegment contextPtr, final MemorySegment argsPtr, final int argsCount) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(argsPtr, "argsPtr");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_args", Integer.class, contextPtr, argsPtr, argsCount);
  }

  /**
   * Destroys a WASI context.
   *
   * @param wasiContextPtr pointer to the WASI context to destroy
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextDestroy(final MemorySegment wasiContextPtr) {
    validatePointer(wasiContextPtr, "wasiContextPtr");
    return callNativeFunction("wasmtime4j_wasi_context_destroy", Integer.class, wasiContextPtr);
  }

  /**
   * Adds a directory mapping to a WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @param hostPtr pointer to the host path string
   * @param guestPtr pointer to the guest path string
   * @param dirCreate whether directory creation is allowed
   * @param dirRead whether directory reading is allowed
   * @param dirRemove whether directory removal is allowed
   * @param fileCreate whether file creation is allowed
   * @param fileRead whether file reading is allowed
   * @param fileWrite whether file writing is allowed
   * @param fileTruncate whether file truncation is allowed
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextAddDirectory(
      final MemorySegment contextPtr,
      final MemorySegment hostPtr,
      final MemorySegment guestPtr,
      final int dirCreate,
      final int dirRead,
      final int dirRemove,
      final int fileCreate,
      final int fileRead,
      final int fileWrite,
      final int fileTruncate) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(hostPtr, "hostPtr");
    validatePointer(guestPtr, "guestPtr");
    return callNativeFunction(
        "wasmtime4j_wasi_context_add_directory",
        Integer.class,
        contextPtr,
        hostPtr,
        guestPtr,
        dirCreate,
        dirRead,
        dirRemove,
        fileCreate,
        fileRead,
        fileWrite,
        fileTruncate);
  }

  // Instance Functions

  /**
   * Creates a WebAssembly instance.
   *
   * @param storePtr pointer to the store
   * @param modulePtr pointer to the module
   * @param instancePtr pointer to store the created instance
   * @return 0 on success, negative error code on failure
   */
  public int instanceCreate(
      final MemorySegment storePtr,
      final MemorySegment modulePtr,
      final MemorySegment instancePtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_instance_create", Integer.class, storePtr, modulePtr, instancePtr);
  }

  /**
   * Destroys a WebAssembly instance.
   *
   * @param instancePtr pointer to the instance to destroy
   */
  public void instanceDestroy(final MemorySegment instancePtr) {
    validatePointer(instancePtr, "instancePtr");
    callNativeFunction("wasmtime4j_instance_destroy", Void.class, instancePtr);
  }

  /**
   * Gets the number of exports in an instance.
   *
   * @param instancePtr pointer to the instance
   * @return the number of exports
   */
  public long instanceExportsLen(final MemorySegment instancePtr) {
    validatePointer(instancePtr, "instancePtr");
    return callNativeFunction("wasmtime4j_instance_exports_len", Long.class, instancePtr);
  }

  /**
   * Gets the nth export from an instance.
   *
   * @param instancePtr pointer to the instance
   * @param index the index of the export to retrieve
   * @param nameOutPtr pointer to receive the export name
   * @param exportOutPtr pointer to receive the export data
   * @return true if the export exists, false otherwise
   */
  public boolean instanceExportNth(
      final MemorySegment instancePtr,
      final long index,
      final MemorySegment nameOutPtr,
      final MemorySegment exportOutPtr) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(nameOutPtr, "nameOutPtr");
    validatePointer(exportOutPtr, "exportOutPtr");
    return callNativeFunction(
        "wasmtime4j_instance_export_nth",
        Boolean.class,
        instancePtr,
        index,
        nameOutPtr,
        exportOutPtr);
  }

  /**
   * Gets a cached method handle for a native function.
   *
   * @param functionName the name of the function
   * @return optional containing the method handle, or empty if not found
   */
  public Optional<MethodHandle> getMethodHandle(final String functionName) {
    FunctionBinding binding = functionBindings.get(functionName);
    if (binding == null) {
      LOGGER.warning("Unknown function binding: " + functionName);
      return Optional.empty();
    }

    return binding.getMethodHandle();
  }

  /**
   * Gets the function descriptor for a native function.
   *
   * @param functionName the name of the function
   * @return optional containing the function descriptor, or empty if not found
   */
  public Optional<FunctionDescriptor> getFunctionDescriptor(final String functionName) {
    FunctionBinding binding = functionBindings.get(functionName);
    if (binding == null) {
      return Optional.empty();
    }

    return Optional.of(binding.getDescriptor());
  }

  /**
   * Gets the method handle for a specific function with descriptor verification.
   *
   * @param functionName the name of the function
   * @param descriptor the expected function descriptor
   * @return the method handle, or null if not available or descriptor mismatch
   */
  public MethodHandle getFunction(final String functionName, final FunctionDescriptor descriptor) {
    FunctionBinding binding = functionBindings.get(functionName);
    if (binding == null) {
      LOGGER.warning("Function binding not found: " + functionName);
      return null;
    }

    // Verify descriptor matches (optional but recommended for safety)
    FunctionDescriptor bindingDescriptor = binding.getDescriptor();
    if (bindingDescriptor != null && !bindingDescriptor.equals(descriptor)) {
      LOGGER.warning(
          "Function descriptor mismatch for "
              + functionName
              + ": expected "
              + descriptor
              + ", got "
              + bindingDescriptor);
    }

    return binding.getMethodHandle().orElse(null);
  }

  /**
   * Gets the method handle for table size query.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableSize() {
    return getMethodHandle("wasmtime4j_table_size").orElse(null);
  }

  /**
   * Gets the method handle for table element get.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableGet() {
    return getMethodHandle("wasmtime4j_table_get").orElse(null);
  }

  /**
   * Gets the method handle for table element set.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableSet() {
    return getMethodHandle("wasmtime4j_table_set").orElse(null);
  }

  /**
   * Gets the method handle for table grow.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableGrow() {
    return getMethodHandle("wasmtime4j_table_grow").orElse(null);
  }

  /**
   * Gets the method handle for table deletion.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableDelete() {
    return getMethodHandle("wasmtime4j_table_destroy").orElse(null);
  }

  /**
   * Gets the method handle for table metadata retrieval.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableMetadata() {
    return getMethodHandle("wasmtime4j_table_metadata").orElse(null);
  }

  /**
   * Gets the method handle for creating host functions.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getFuncNewHost() {
    return getMethodHandle("wasmtime4j_func_new_host").orElse(null);
  }

  /**
   * Gets the method handle for creating function types.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getFuncTypeNew() {
    return getMethodHandle("wasmtime4j_functype_new").orElse(null);
  }

  /**
   * Gets the method handle for destroying function types.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getFuncTypeDelete() {
    return getMethodHandle("wasmtime4j_functype_destroy").orElse(null);
  }

  /**
   * Creates a component engine.
   *
   * @return memory segment pointer to the component engine, or null on failure
   */
  public MemorySegment createComponentEngine() {
    return callNativeFunction("wasmtime4j_component_engine_create", MemorySegment.class);
  }

  /**
   * Loads a component from bytes.
   *
   * @param enginePtr pointer to the component engine
   * @param wasmBytes pointer to the WASM component bytes
   * @return memory segment pointer to the component, or null on failure
   */
  public MemorySegment loadComponentFromBytes(
      final MemorySegment enginePtr, final MemorySegment wasmBytes) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    return callNativeFunction(
        "wasmtime4j_component_load_from_bytes", MemorySegment.class, enginePtr, wasmBytes);
  }

  /**
   * Instantiates a component.
   *
   * @param enginePtr pointer to the component engine
   * @param componentPtr pointer to the component
   * @return memory segment pointer to the component instance, or null on failure
   */
  public MemorySegment instantiateComponent(
      final MemorySegment enginePtr, final MemorySegment componentPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_instantiate", MemorySegment.class, enginePtr, componentPtr);
  }

  /**
   * Gets the active instances count for a component engine.
   *
   * @param enginePtr pointer to the component engine
   * @return number of active instances
   */
  public int getActiveInstancesCount(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_component_engine_active_instances", Integer.class, enginePtr);
  }

  /**
   * Cleans up inactive instances.
   *
   * @param enginePtr pointer to the component engine
   * @return number of instances cleaned up
   */
  public int cleanupInstances(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_component_engine_cleanup", Integer.class, enginePtr);
  }

  /**
   * Destroys a component engine.
   *
   * @param enginePtr pointer to the component engine to destroy
   */
  public void destroyComponentEngine(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_component_engine_destroy", Void.class, enginePtr);
  }

  /**
   * Gets the size of a component.
   *
   * @param componentPtr pointer to the component
   * @return component size in bytes
   */
  public long getComponentSize(final MemorySegment componentPtr) {
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction("wasmtime4j_component_size", Long.class, componentPtr);
  }

  /**
   * Checks if a component exports a specific interface.
   *
   * @param componentPtr pointer to the component
   * @param interfaceName name of the interface to check
   * @return true if the interface is exported
   */
  public boolean exportsInterface(final MemorySegment componentPtr, final String interfaceName) {
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_exports_interface", Boolean.class, componentPtr, interfaceName);
  }

  /**
   * Checks if a component imports a specific interface.
   *
   * @param componentPtr pointer to the component
   * @param interfaceName name of the interface to check
   * @return true if the interface is imported
   */
  public boolean importsInterface(final MemorySegment componentPtr, final String interfaceName) {
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_imports_interface", Boolean.class, componentPtr, interfaceName);
  }

  /**
   * Destroys a component.
   *
   * @param componentPtr pointer to the component to destroy
   */
  public void destroyComponent(final MemorySegment componentPtr) {
    validatePointer(componentPtr, "componentPtr");
    callNativeFunction("wasmtime4j_component_destroy", Void.class, componentPtr);
  }

  /**
   * Destroys a component instance.
   *
   * @param instancePtr pointer to the component instance to destroy
   */
  public void destroyComponentInstance(final MemorySegment instancePtr) {
    validatePointer(instancePtr, "instancePtr");
    callNativeFunction("wasmtime4j_component_instance_destroy", Void.class, instancePtr);
  }

  // Global Functions

  /**
   * Gets the value of a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @param valueOutPtr pointer to store the global value
   */
  public void globalGet(final MemorySegment globalPtr, final MemorySegment valueOutPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(valueOutPtr, "valueOutPtr");
    callNativeFunction("wasmtime_global_get", Void.class, globalPtr, valueOutPtr);
  }

  /**
   * Sets the value of a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @param valuePtr pointer to the new value
   */
  public void globalSet(final MemorySegment globalPtr, final MemorySegment valuePtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(valuePtr, "valuePtr");
    callNativeFunction("wasmtime_global_set", Void.class, globalPtr, valuePtr);
  }

  /**
   * Gets the type of a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @return pointer to the global type
   */
  public MemorySegment globalType(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    return callNativeFunction("wasmtime_global_type", MemorySegment.class, globalPtr);
  }

  /**
   * Creates a mutable WebAssembly global.
   *
   * @param storePtr pointer to the store
   * @param valueType the WebAssembly value type
   * @param initialValuePtr pointer to the initial value
   * @param globalOutPtr pointer to store the created global
   * @return 0 on success, negative error code on failure
   */
  public int globalCreateMutable(
      final MemorySegment storePtr,
      final int valueType,
      final MemorySegment initialValuePtr,
      final MemorySegment globalOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(initialValuePtr, "initialValuePtr");
    validatePointer(globalOutPtr, "globalOutPtr");

    return callNativeFunction(
        "wasmtime4j_global_create_mutable",
        Integer.class,
        storePtr,
        valueType,
        initialValuePtr,
        globalOutPtr);
  }

  /**
   * Creates an immutable WebAssembly global.
   *
   * @param storePtr pointer to the store
   * @param valueType the WebAssembly value type
   * @param initialValuePtr pointer to the initial value
   * @param globalOutPtr pointer to store the created global
   * @return 0 on success, negative error code on failure
   */
  public int globalCreateImmutable(
      final MemorySegment storePtr,
      final int valueType,
      final MemorySegment initialValuePtr,
      final MemorySegment globalOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(initialValuePtr, "initialValuePtr");
    validatePointer(globalOutPtr, "globalOutPtr");

    return callNativeFunction(
        "wasmtime4j_global_create_immutable",
        Integer.class,
        storePtr,
        valueType,
        initialValuePtr,
        globalOutPtr);
  }

  /**
   * Destroys a WebAssembly global.
   *
   * @param globalPtr pointer to the global to destroy
   */
  public void globalDestroy(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    callNativeFunction("wasmtime4j_global_destroy", Void.class, globalPtr);
  }

  /**
   * Gets type information for a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @param typeInfoOutPtr pointer to store the type information
   * @param mutabilityOutPtr pointer to store the mutability flag
   * @return 0 on success, negative error code on failure
   */
  public int globalGetTypeInfo(
      final MemorySegment globalPtr,
      final MemorySegment typeInfoOutPtr,
      final MemorySegment mutabilityOutPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(typeInfoOutPtr, "typeInfoOutPtr");
    validatePointer(mutabilityOutPtr, "mutabilityOutPtr");

    return callNativeFunction(
        "wasmtime4j_global_get_type_info",
        Integer.class,
        globalPtr,
        typeInfoOutPtr,
        mutabilityOutPtr);
  }

  /**
   * Registers a global for cross-module sharing.
   *
   * @param globalPtr pointer to the global
   * @param namePtr pointer to the global name string
   * @param registryPtr pointer to the global registry
   * @return 0 on success, negative error code on failure
   */
  public int globalRegisterShared(
      final MemorySegment globalPtr, final MemorySegment namePtr, final MemorySegment registryPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(namePtr, "namePtr");
    validatePointer(registryPtr, "registryPtr");

    return callNativeFunction(
        "wasmtime4j_global_register_shared", Integer.class, globalPtr, namePtr, registryPtr);
  }

  /**
   * Looks up a shared global by name.
   *
   * @param namePtr pointer to the global name string
   * @param registryPtr pointer to the global registry
   * @return pointer to the global, or null if not found
   */
  public MemorySegment globalLookupShared(
      final MemorySegment namePtr, final MemorySegment registryPtr) {
    validatePointer(namePtr, "namePtr");
    validatePointer(registryPtr, "registryPtr");

    return callNativeFunction(
        "wasmtime4j_global_lookup_shared", MemorySegment.class, namePtr, registryPtr);
  }

  /**
   * Gets direct access to a global's value for zero-copy operations.
   *
   * @param globalPtr pointer to the global
   * @return pointer to direct value access, or null if not supported
   */
  public MemorySegment globalGetDirectAccess(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    return callNativeFunction(
        "wasmtime4j_global_get_direct_access", MemorySegment.class, globalPtr);
  }

  /**
   * Releases direct access to a global's value.
   *
   * @param globalPtr pointer to the global
   * @param directPtr pointer to the direct access handle
   */
  public void globalReleaseDirectAccess(
      final MemorySegment globalPtr, final MemorySegment directPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(directPtr, "directPtr");
    callNativeFunction("wasmtime4j_global_release_direct_access", Void.class, globalPtr, directPtr);
  }

  // Error Handling Functions

  /**
   * Gets the last error message from the native library.
   *
   * @return pointer to error message string, or null if no error
   */
  public MemorySegment getLastErrorMessage() {
    return callNativeFunction("wasmtime4j_get_last_error_message", MemorySegment.class);
  }

  /**
   * Frees an error message returned by getLastErrorMessage.
   *
   * @param messagePtr pointer to the error message to free
   */
  public void freeErrorMessage(final MemorySegment messagePtr) {
    if (messagePtr != null && !messagePtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_free_error_message", Void.class, messagePtr);
    }
  }

  /** Clears any stored error state in the native library. */
  public void clearErrorState() {
    callNativeFunction("wasmtime4j_clear_error_state", Void.class);
  }

  /** Initializes all function bindings. */
  private void initializeFunctionBindings() {
    // Engine functions
    addFunctionBinding("wasmtime4j_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding("wasmtime4j_engine_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_engine_configure",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // option_name
            ValueLayout.ADDRESS)); // option_value

    // Engine configuration functions
    addFunctionBinding(
        "wasmtime4j_engine_set_optimization_level",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_INT)); // level

    addFunctionBinding(
        "wasmtime4j_engine_get_optimization_level",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_set_debug_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_BOOLEAN)); // enabled

    addFunctionBinding(
        "wasmtime4j_engine_is_debug_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.ADDRESS)); // engine_ptr

    // Module functions
    addFunctionBinding(
        "wasmtime4j_module_compile",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG, // wasm_size
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding("wasmtime4j_module_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Store functions
    addFunctionBinding(
        "wasmtime4j_store_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding("wasmtime4j_store_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_store_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // fuel_limit
            ValueLayout.JAVA_LONG, // memory_limit_bytes
            ValueLayout.JAVA_LONG, // execution_timeout_secs
            ValueLayout.JAVA_INT, // max_instances
            ValueLayout.JAVA_INT, // max_table_elements
            ValueLayout.JAVA_INT)); // max_functions

    // Additional Store functions
    addFunctionBinding(
        "wasmtime4j_store_set_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_store_get_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // fuel_out_ptr

    addFunctionBinding(
        "wasmtime4j_store_add_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_store_set_epoch_deadline",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // ticks

    addFunctionBinding(
        "wasmtime4j_store_gc",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_get_execution_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // execution_count_ptr
            ValueLayout.ADDRESS, // fuel_consumed_ptr
            ValueLayout.ADDRESS)); // total_execution_time_ns_ptr

    addFunctionBinding(
        "wasmtime4j_store_get_memory_usage",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // total_bytes_ptr
            ValueLayout.ADDRESS, // used_bytes_ptr
            ValueLayout.ADDRESS)); // instance_count_ptr

    addFunctionBinding(
        "wasmtime4j_store_validate",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    // Instance functions
    addFunctionBinding(
        "wasmtime4j_instance_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS)); // instance_ptr

    addFunctionBinding(
        "wasmtime4j_instance_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_exports_len",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return export count
            ValueLayout.ADDRESS)); // instance_ptr

    addFunctionBinding(
        "wasmtime4j_instance_export_nth",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return found
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS, // name_out_ptr
            ValueLayout.ADDRESS)); // export_out_ptr

    // Table functions
    addFunctionBinding(
        "wasmtime4j_table_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_table_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.ADDRESS, // ref_id_present out param
            ValueLayout.ADDRESS)); // ref_id out param

    addFunctionBinding(
        "wasmtime4j_table_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // elements
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_table_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_table_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // element_type out param
            ValueLayout.ADDRESS, // initial_size out param
            ValueLayout.ADDRESS, // has_maximum out param
            ValueLayout.ADDRESS, // maximum_size out param
            ValueLayout.ADDRESS)); // name_ptr out param

    // Host function bindings
    addFunctionBinding(
        "wasmtime4j_func_new_host",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return func_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // functype_ptr
            ValueLayout.ADDRESS)); // callback_ptr

    addFunctionBinding(
        "wasmtime4j_functype_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return functype_ptr
            ValueLayout.ADDRESS, // param_types array
            ValueLayout.JAVA_LONG, // param_count
            ValueLayout.ADDRESS, // return_types array
            ValueLayout.JAVA_LONG)); // return_count

    addFunctionBinding(
        "wasmtime4j_functype_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // functype_ptr

    // Component engine functions
    addFunctionBinding(
        "wasmtime4j_component_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return component_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // wasm_bytes

    addFunctionBinding(
        "wasmtime4j_component_instantiate",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // component_ptr

    addFunctionBinding(
        "wasmtime4j_component_engine_active_instances",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return count
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_component_engine_cleanup",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return count
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_component_engine_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_component_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // component_ptr

    addFunctionBinding(
        "wasmtime4j_component_exports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS)); // interface_name

    addFunctionBinding(
        "wasmtime4j_component_imports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS)); // interface_name

    addFunctionBinding(
        "wasmtime4j_component_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // component_ptr

    addFunctionBinding(
        "wasmtime4j_component_instance_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // instance_ptr

    // Global functions
    addFunctionBinding(
        "wasmtime_global_get",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS)); // value (out)

    addFunctionBinding(
        "wasmtime_global_set",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS)); // value

    addFunctionBinding(
        "wasmtime_global_type",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return global type
            ValueLayout.ADDRESS)); // global

    addFunctionBinding(
        "wasmtime4j_global_create_mutable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.ADDRESS, // initial_value
            ValueLayout.ADDRESS)); // global_ptr (out)

    addFunctionBinding(
        "wasmtime4j_global_create_immutable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.ADDRESS, // initial_value
            ValueLayout.ADDRESS)); // global_ptr (out)

    addFunctionBinding(
        "wasmtime4j_global_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // global_ptr

    addFunctionBinding(
        "wasmtime4j_global_get_type_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS, // type_info (out)
            ValueLayout.ADDRESS)); // mutability (out)

    // Cross-module global sharing functions
    addFunctionBinding(
        "wasmtime4j_global_register_shared",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS, // name
            ValueLayout.ADDRESS)); // registry

    addFunctionBinding(
        "wasmtime4j_global_lookup_shared",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return global or null
            ValueLayout.ADDRESS, // name
            ValueLayout.ADDRESS)); // registry

    // Zero-copy global access functions
    addFunctionBinding(
        "wasmtime4j_global_get_direct_access",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return direct pointer or null
            ValueLayout.ADDRESS)); // global

    addFunctionBinding(
        "wasmtime4j_global_release_direct_access",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS)); // direct_ptr

    // Error handling functions
    addFunctionBinding(
        "wasmtime4j_get_last_error_message",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns char*

    addFunctionBinding(
        "wasmtime4j_free_error_message",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // message_ptr

    addFunctionBinding(
        "wasmtime4j_clear_error_state", FunctionDescriptor.ofVoid()); // no parameters
  }

  /**
   * Adds a function binding with lazy initialization.
   *
   * @param functionName the name of the function
   * @param descriptor the function descriptor
   */
  private void addFunctionBinding(final String functionName, final FunctionDescriptor descriptor) {
    FunctionBinding binding = new FunctionBinding(functionName, descriptor);
    functionBindings.put(functionName, binding);
    LOGGER.finest("Added function binding: " + functionName);
  }

  /**
   * Calls a native function with type safety.
   *
   * @param functionName the name of the function
   * @param returnType the expected return type
   * @param args the function arguments
   * @return the function result
   * @throws IllegalStateException if the function call fails
   */
  @SuppressWarnings("unchecked")
  public <T> T callNativeFunction(
      final String functionName, final Class<T> returnType, final Object... args) {
    if (!isInitialized()) {
      throw new IllegalStateException("Native function bindings not initialized");
    }

    FunctionBinding binding = functionBindings.get(functionName);
    if (binding == null) {
      throw new IllegalArgumentException("Unknown function: " + functionName);
    }

    Optional<MethodHandle> methodHandle = binding.getMethodHandle();
    if (methodHandle.isEmpty()) {
      throw new IllegalStateException("Failed to get method handle for function: " + functionName);
    }

    try {
      Object result = methodHandle.get().invokeWithArguments(args);

      if (returnType == Void.class) {
        return null;
      } else {
        return returnType.cast(result);
      }
    } catch (Throwable e) {
      LOGGER.warning("Native function call failed: " + functionName + " - " + e.getMessage());
      throw new IllegalStateException("Native function call failed: " + functionName, e);
    }
  }

  /**
   * Validates that a pointer is not null.
   *
   * @param pointer the pointer to validate
   * @param name the name of the parameter for error messages
   * @throws IllegalArgumentException if the pointer is null
   */
  private void validatePointer(final MemorySegment pointer, final String name) {
    if (pointer == null || pointer.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }

  /**
   * Validates that a size is positive.
   *
   * @param size the size to validate
   * @param name the name of the parameter for error messages
   * @throws IllegalArgumentException if the size is not positive
   */
  private void validateSize(final long size, final String name) {
    if (size <= 0) {
      throw new IllegalArgumentException(name + " must be positive: " + size);
    }
  }

  /** Function binding with lazy initialization. */
  private final class FunctionBinding {
    private final String functionName;
    private final FunctionDescriptor descriptor;
    private volatile MethodHandle methodHandle;
    private volatile boolean initialized = false;

    FunctionBinding(final String functionName, final FunctionDescriptor descriptor) {
      this.functionName = functionName;
      this.descriptor = descriptor;
    }

    String getFunctionName() {
      return functionName;
    }

    FunctionDescriptor getDescriptor() {
      return descriptor;
    }

    Optional<MethodHandle> getMethodHandle() {
      if (!initialized) {
        synchronized (this) {
          if (!initialized) {
            initializeMethodHandle();
            initialized = true;
          }
        }
      }

      return Optional.ofNullable(methodHandle);
    }

    private void initializeMethodHandle() {
      Optional<MethodHandle> handle =
          methodHandleCache.getOrCreate(functionName, MemorySegment.NULL, descriptor);

      if (handle.isEmpty()) {
        // Try direct lookup from library loader
        handle = libraryLoader.lookupFunction(functionName, descriptor);
      }

      if (handle.isPresent()) {
        this.methodHandle = handle.get();
        LOGGER.finest("Initialized method handle for function: " + functionName);
      } else {
        LOGGER.warning("Failed to initialize method handle for function: " + functionName);
      }
    }
  }
}
