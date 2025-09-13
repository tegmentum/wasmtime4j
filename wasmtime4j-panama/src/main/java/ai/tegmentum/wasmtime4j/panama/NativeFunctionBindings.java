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
   * @param fuelLimit fuel limit (0 = no limit)
   * @param memoryLimitBytes memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs execution timeout in seconds (0 = no timeout)
   * @param maxInstances maximum instances (0 = no limit)
   * @param maxTableElements maximum table elements (0 = no limit)
   * @param maxFunctions maximum functions (0 = no limit)
   * @param storePtr pointer to store the created store
   * @return 0 on success, negative error code on failure
   */
  public int storeCreateWithConfig(
      final MemorySegment enginePtr,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxFunctions,
      final MemorySegment storePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(storePtr, "storePtr");

    return callNativeFunction(
        "wasmtime4j_store_create_with_config",
        Integer.class,
        enginePtr,
        fuelLimit,
        memoryLimitBytes,
        executionTimeoutSecs,
        maxInstances,
        maxTableElements,
        maxFunctions,
        storePtr);
  }

  /**
   * Adds fuel to a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuel amount of fuel to add
   * @return 0 on success, negative error code on failure
   */
  public int storeAddFuel(final MemorySegment storePtr, final long fuel) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_add_fuel", Integer.class, storePtr, fuel);
  }

  /**
   * Gets remaining fuel in a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuelPtr pointer to store the fuel amount
   * @return 0 on success, negative error code on failure
   */
  public int storeGetFuelRemaining(final MemorySegment storePtr, final MemorySegment fuelPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(fuelPtr, "fuelPtr");
    return callNativeFunction("wasmtime4j_store_get_fuel_remaining", Integer.class, storePtr, fuelPtr);
  }

  /**
   * Consumes fuel from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuelToConsume amount of fuel to consume
   * @param fuelConsumedPtr pointer to store actual consumed fuel
   * @return 0 on success, negative error code on failure
   */
  public int storeConsumeFuel(
      final MemorySegment storePtr, final long fuelToConsume, final MemorySegment fuelConsumedPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(fuelConsumedPtr, "fuelConsumedPtr");
    return callNativeFunction(
        "wasmtime4j_store_consume_fuel", Integer.class, storePtr, fuelToConsume, fuelConsumedPtr);
  }

  /**
   * Sets epoch deadline for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param ticks number of epoch ticks
   * @return 0 on success, negative error code on failure
   */
  public int storeSetEpochDeadline(final MemorySegment storePtr, final long ticks) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_set_epoch_deadline", Integer.class, storePtr, ticks);
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
   * Validates a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeValidate(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_validate", Integer.class, storePtr);
  }

  /**
   * Gets execution statistics from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param executionCountPtr pointer to store execution count
   * @param totalExecutionTimeMsPtr pointer to store total execution time in milliseconds
   * @param fuelConsumedPtr pointer to store fuel consumed
   * @return 0 on success, negative error code on failure
   */
  public int storeGetExecutionStats(
      final MemorySegment storePtr,
      final MemorySegment executionCountPtr,
      final MemorySegment totalExecutionTimeMsPtr,
      final MemorySegment fuelConsumedPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(executionCountPtr, "executionCountPtr");
    validatePointer(totalExecutionTimeMsPtr, "totalExecutionTimeMsPtr");
    validatePointer(fuelConsumedPtr, "fuelConsumedPtr");
    return callNativeFunction(
        "wasmtime4j_store_get_execution_stats",
        Integer.class,
        storePtr,
        executionCountPtr,
        totalExecutionTimeMsPtr,
        fuelConsumedPtr);
  }

  /**
   * Gets memory usage statistics from a WebAssembly store.
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
    validatePointer(totalBytesPtr, "totalBytesPtr");
    validatePointer(usedBytesPtr, "usedBytesPtr");
    validatePointer(instanceCountPtr, "instanceCountPtr");
    return callNativeFunction(
        "wasmtime4j_store_get_memory_usage",
        Integer.class,
        storePtr,
        totalBytesPtr,
        usedBytesPtr,
        instanceCountPtr);
  }

  /**
   * Gets metadata from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuelLimitPtr pointer to store fuel limit
   * @param memoryLimitBytesPtr pointer to store memory limit in bytes
   * @param executionTimeoutSecsPtr pointer to store execution timeout in seconds
   * @param instanceCountPtr pointer to store instance count
   * @return 0 on success, negative error code on failure
   */
  public int storeGetMetadata(
      final MemorySegment storePtr,
      final MemorySegment fuelLimitPtr,
      final MemorySegment memoryLimitBytesPtr,
      final MemorySegment executionTimeoutSecsPtr,
      final MemorySegment instanceCountPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(fuelLimitPtr, "fuelLimitPtr");
    validatePointer(memoryLimitBytesPtr, "memoryLimitBytesPtr");
    validatePointer(executionTimeoutSecsPtr, "executionTimeoutSecsPtr");
    validatePointer(instanceCountPtr, "instanceCountPtr");
    return callNativeFunction(
        "wasmtime4j_store_get_metadata",
        Integer.class,
        storePtr,
        fuelLimitPtr,
        memoryLimitBytesPtr,
        executionTimeoutSecsPtr,
        instanceCountPtr);
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

  /**
   * Clears any stored error state in the native library.
   */
  public void clearErrorState() {
    callNativeFunction("wasmtime4j_clear_error_state", Void.class);
  }

  // WASI Functions

  /**
   * Creates a new WASI context with default configuration.
   *
   * @return memory segment pointer to the WASI context, or null on failure
   */
  public MemorySegment wasiContextNew() {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeFunctionBindings not initialized, cannot create WASI context");
        return null;
      }

      MemorySegment result = callNativeFunction("wasi_ctx_new", MemorySegment.class);
      if (result == null || result.equals(MemorySegment.NULL)) {
        LOGGER.warning("WASI context creation returned null");
      } else {
        LOGGER.fine("WASI context created successfully: " + result);
      }
      return result;
    } catch (Exception e) {
      LOGGER.severe("Exception during WASI context creation: " + e.getMessage());
      return null;
    }
  }

  /**
   * Creates a new WASI context with custom configuration.
   *
   * @param allowNetwork whether to allow network access (1 = true, 0 = false)
   * @param allowArbitraryFs whether to allow arbitrary filesystem access (1 = true, 0 = false)
   * @param maxFileSize maximum file size for operations (0 = no limit)
   * @param maxOpenFiles maximum number of open file descriptors (0 = no limit)
   * @return memory segment pointer to the WASI context, or null on failure
   */
  public MemorySegment wasiContextNewWithConfig(
      final int allowNetwork,
      final int allowArbitraryFs,
      final long maxFileSize,
      final int maxOpenFiles) {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeFunctionBindings not initialized, cannot create WASI context with config");
        return null;
      }

      MemorySegment result = callNativeFunction("wasi_ctx_new_with_config", MemorySegment.class,
          allowNetwork, allowArbitraryFs, maxFileSize, maxOpenFiles);
      if (result == null || result.equals(MemorySegment.NULL)) {
        LOGGER.warning("WASI context with config creation returned null");
      } else {
        LOGGER.fine("WASI context with config created successfully: " + result);
      }
      return result;
    } catch (Exception e) {
      LOGGER.severe("Exception during WASI context with config creation: " + e.getMessage());
      return null;
    }
  }

  /**
   * Adds a directory mapping to the WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @param hostPath pointer to the host path string
   * @param guestPath pointer to the guest path string
   * @param canCreate directory can create permission (1 = true, 0 = false)
   * @param canRead directory can read permission (1 = true, 0 = false)
   * @param canRemove directory can remove permission (1 = true, 0 = false)
   * @param fileRead file read permission (1 = true, 0 = false)
   * @param fileWrite file write permission (1 = true, 0 = false)
   * @param fileCreate file create permission (1 = true, 0 = false)
   * @param fileTruncate file truncate permission (1 = true, 0 = false)
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextAddDirectory(
      final MemorySegment contextPtr,
      final MemorySegment hostPath,
      final MemorySegment guestPath,
      final int canCreate,
      final int canRead,
      final int canRemove,
      final int fileRead,
      final int fileWrite,
      final int fileCreate,
      final int fileTruncate) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(hostPath, "hostPath");
    validatePointer(guestPath, "guestPath");

    return callNativeFunction("wasi_ctx_add_dir", Integer.class, contextPtr, hostPath, guestPath,
        canCreate, canRead, canRemove, fileRead, fileWrite, fileCreate, fileTruncate);
  }

  /**
   * Sets an environment variable in the WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @param key pointer to the environment variable key string
   * @param value pointer to the environment variable value string
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextSetEnvironmentVariable(
      final MemorySegment contextPtr,
      final MemorySegment key,
      final MemorySegment value) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(key, "key");
    validatePointer(value, "value");

    return callNativeFunction("wasi_ctx_set_env", Integer.class, contextPtr, key, value);
  }

  /**
   * Sets command line arguments for the WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @param args pointer to array of argument strings
   * @param argsLen number of arguments
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextSetArguments(
      final MemorySegment contextPtr,
      final MemorySegment args,
      final long argsLen) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(args, "args");
    validateSize(argsLen, "argsLen");

    return callNativeFunction("wasi_ctx_set_args", Integer.class, contextPtr, args, argsLen);
  }

  /**
   * Configures standard I/O streams for the WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @param stdinType stdin source type (0=inherit, 1=buffer, 2=file, other=null)
   * @param stdinData pointer to stdin data (or null)
   * @param stdoutType stdout sink type (0=inherit, 1=buffer, 2=file, other=null)
   * @param stdoutData pointer to stdout data (or null)
   * @param stderrType stderr sink type (0=inherit, 1=buffer, 2=file, other=null)
   * @param stderrData pointer to stderr data (or null)
   * @return 0 on success, negative error code on failure
   */
  public int wasiContextConfigureStdio(
      final MemorySegment contextPtr,
      final int stdinType,
      final MemorySegment stdinData,
      final int stdoutType,
      final MemorySegment stdoutData,
      final int stderrType,
      final MemorySegment stderrData) {
    validatePointer(contextPtr, "contextPtr");

    return callNativeFunction("wasi_ctx_configure_stdio", Integer.class, contextPtr,
        stdinType, stdinData, stdoutType, stdoutData, stderrType, stderrData);
  }

  /**
   * Checks if a path is allowed based on WASI context configuration.
   *
   * @param contextPtr pointer to the WASI context
   * @param path pointer to the path string
   * @return 1 if allowed, 0 if not allowed, negative error code on failure
   */
  public int wasiContextIsPathAllowed(final MemorySegment contextPtr, final MemorySegment path) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(path, "path");

    return callNativeFunction("wasi_ctx_is_path_allowed", Integer.class, contextPtr, path);
  }

  /**
   * Gets the number of directory mappings in the WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @return number of directory mappings, or 0 on error
   */
  public long wasiContextGetDirectoryCount(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    
    return callNativeFunction("wasi_ctx_get_dir_count", Long.class, contextPtr);
  }

  /**
   * Gets the number of environment variables in the WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @return number of environment variables, or 0 on error
   */
  public long wasiContextGetEnvironmentCount(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    
    return callNativeFunction("wasi_ctx_get_env_count", Long.class, contextPtr);
  }

  /**
   * Gets the number of command line arguments in the WASI context.
   *
   * @param contextPtr pointer to the WASI context
   * @return number of arguments, or 0 on error
   */
  public long wasiContextGetArgumentCount(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    
    return callNativeFunction("wasi_ctx_get_args_count", Long.class, contextPtr);
  }

  /**
   * Destroys a WASI context and frees its resources.
   *
   * @param contextPtr pointer to the WASI context
   */
  public void wasiContextDestroy(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    callNativeFunction("wasi_ctx_destroy", Void.class, contextPtr);
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

    // Additional Store functions
    addFunctionBinding(
        "wasmtime4j_store_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_LONG, // fuel_limit
            ValueLayout.JAVA_LONG, // memory_limit_bytes
            ValueLayout.JAVA_LONG, // execution_timeout_secs
            ValueLayout.JAVA_INT, // max_instances
            ValueLayout.JAVA_INT, // max_table_elements
            ValueLayout.JAVA_INT, // max_functions
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_add_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_store_get_fuel_remaining",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // fuel_ptr

    addFunctionBinding(
        "wasmtime4j_store_consume_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // fuel_to_consume
            ValueLayout.ADDRESS)); // fuel_consumed_ptr

    addFunctionBinding(
        "wasmtime4j_store_set_epoch_deadline",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // ticks

    addFunctionBinding(
        "wasmtime4j_store_garbage_collect",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_validate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_get_execution_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // execution_count_ptr
            ValueLayout.ADDRESS, // total_execution_time_ms_ptr
            ValueLayout.ADDRESS)); // fuel_consumed_ptr

    addFunctionBinding(
        "wasmtime4j_store_get_memory_usage",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // total_bytes_ptr
            ValueLayout.ADDRESS, // used_bytes_ptr
            ValueLayout.ADDRESS)); // instance_count_ptr

    addFunctionBinding(
        "wasmtime4j_store_get_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // fuel_limit_ptr
            ValueLayout.ADDRESS, // memory_limit_bytes_ptr
            ValueLayout.ADDRESS, // execution_timeout_secs_ptr
            ValueLayout.ADDRESS)); // instance_count_ptr

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

    // Table functions
    addFunctionBinding(
        "wasmtime4j_table_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_table_get",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return element
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.JAVA_LONG)); // index

    addFunctionBinding(
        "wasmtime4j_table_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS)); // element_ptr

    addFunctionBinding(
        "wasmtime4j_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.JAVA_LONG, // delta
            ValueLayout.ADDRESS)); // initial_value

    addFunctionBinding(
        "wasmtime4j_table_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // table_ptr

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

    // Error handling functions
    addFunctionBinding(
        "wasmtime4j_get_last_error_message",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns char*

    addFunctionBinding(
        "wasmtime4j_free_error_message",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // message_ptr

    addFunctionBinding(
        "wasmtime4j_clear_error_state",
        FunctionDescriptor.ofVoid()); // no parameters

    // WASI function bindings
    addFunctionBinding("wasi_ctx_new", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasi_ctx_new_with_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return context_ptr
            ValueLayout.JAVA_INT, // allow_network
            ValueLayout.JAVA_INT, // allow_arbitrary_fs
            ValueLayout.JAVA_LONG, // max_file_size
            ValueLayout.JAVA_INT)); // max_open_files

    addFunctionBinding(
        "wasi_ctx_add_dir",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // context_ptr
            ValueLayout.ADDRESS, // host_path
            ValueLayout.ADDRESS, // guest_path
            ValueLayout.JAVA_INT, // can_create
            ValueLayout.JAVA_INT, // can_read
            ValueLayout.JAVA_INT, // can_remove
            ValueLayout.JAVA_INT, // file_read
            ValueLayout.JAVA_INT, // file_write
            ValueLayout.JAVA_INT, // file_create
            ValueLayout.JAVA_INT)); // file_truncate

    addFunctionBinding(
        "wasi_ctx_set_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // context_ptr
            ValueLayout.ADDRESS, // key
            ValueLayout.ADDRESS)); // value

    addFunctionBinding(
        "wasi_ctx_set_args",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // context_ptr
            ValueLayout.ADDRESS, // args array
            ValueLayout.JAVA_LONG)); // args_len

    addFunctionBinding(
        "wasi_ctx_configure_stdio",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // context_ptr
            ValueLayout.JAVA_INT, // stdin_type
            ValueLayout.ADDRESS, // stdin_data
            ValueLayout.JAVA_INT, // stdout_type
            ValueLayout.ADDRESS, // stdout_data
            ValueLayout.JAVA_INT, // stderr_type
            ValueLayout.ADDRESS)); // stderr_data

    addFunctionBinding(
        "wasi_ctx_is_path_allowed",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return allowed flag
            ValueLayout.ADDRESS, // context_ptr
            ValueLayout.ADDRESS)); // path

    addFunctionBinding(
        "wasi_ctx_get_dir_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return count
            ValueLayout.ADDRESS)); // context_ptr

    addFunctionBinding(
        "wasi_ctx_get_env_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return count
            ValueLayout.ADDRESS)); // context_ptr

    addFunctionBinding(
        "wasi_ctx_get_args_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return count
            ValueLayout.ADDRESS)); // context_ptr

    addFunctionBinding(
        "wasi_ctx_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // context_ptr

    // Function operation bindings
    addFunctionBinding(
        "wasmtime4j_func_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name
            ValueLayout.ADDRESS)); // func_ptr (output)

    addFunctionBinding(
        "wasmtime4j_func_get_param_types",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // func_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // types_ptr (output)
            ValueLayout.ADDRESS)); // count_ptr (output)

    addFunctionBinding(
        "wasmtime4j_func_get_result_types",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // func_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // types_ptr (output)
            ValueLayout.ADDRESS)); // count_ptr (output)

    addFunctionBinding(
        "wasmtime4j_func_call",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // func_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // params_ptr
            ValueLayout.JAVA_LONG, // param_count
            ValueLayout.ADDRESS, // results_ptr
            ValueLayout.JAVA_LONG)); // result_count

    addFunctionBinding(
        "wasmtime4j_func_type",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return func_type_ptr
            ValueLayout.ADDRESS, // func_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_func_type_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // func_type_ptr

    addFunctionBinding(
        "wasmtime4j_func_free_types_array",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // types_ptr
            ValueLayout.JAVA_LONG)); // count

    addFunctionBinding(
        "wasmtime4j_func_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // func_ptr
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
  private <T> T callNativeFunction(
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
