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

import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import java.lang.foreign.Arena;
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
  private final ConcurrentHashMap<String, FunctionBinding> functionBindings;

  // Status
  private volatile boolean initialized = false;

  // Hot-path typed MethodHandles for invokeExact optimization
  // Signature: (ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, JAVA_LONG) -> JAVA_LONG
  private volatile MethodHandle mhInstanceCallFunction;

  // Signature: () -> ADDRESS
  private volatile MethodHandle mhEngineCreate;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG) -> ADDRESS
  private volatile MethodHandle mhModuleCreate;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG) -> JAVA_INT
  private volatile MethodHandle mhModuleValidate;

  // Signature: (ADDRESS) -> ADDRESS
  private volatile MethodHandle mhStoreCreate;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG, ADDRESS) -> JAVA_INT
  private volatile MethodHandle mhPanamaMemoryReadBytes;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG, ADDRESS) -> JAVA_INT
  private volatile MethodHandle mhPanamaMemoryWriteBytes;

  // Signature: (ADDRESS, ADDRESS) -> ADDRESS
  private volatile MethodHandle mhInstanceCreate;

  /** Private constructor for singleton pattern. */
  private NativeFunctionBindings() {
    try {
      this.libraryLoader = NativeLibraryLoader.getInstance();
      this.functionBindings = new ConcurrentHashMap<>();

      // Verify library loader is ready before proceeding
      if (!this.libraryLoader.isLoaded()) {
        throw new IllegalStateException("Native library loader is not properly initialized");
      }

      initializeFunctionBindings();

      // Eagerly initialize hot-path MethodHandles for invokeExact optimization
      FunctionBinding callBinding = functionBindings.get("wasmtime4j_instance_call_function");
      if (callBinding != null) {
        this.mhInstanceCallFunction = callBinding.getMethodHandle().orElse(null);
      }

      FunctionBinding engineCreateBinding = functionBindings.get("wasmtime4j_engine_create");
      if (engineCreateBinding != null) {
        this.mhEngineCreate = engineCreateBinding.getMethodHandle().orElse(null);
      }

      FunctionBinding moduleCreateBinding = functionBindings.get("wasmtime4j_module_create");
      if (moduleCreateBinding != null) {
        this.mhModuleCreate = moduleCreateBinding.getMethodHandle().orElse(null);
      }

      FunctionBinding moduleValidateBinding = functionBindings.get("wasmtime4j_module_validate");
      if (moduleValidateBinding != null) {
        this.mhModuleValidate = moduleValidateBinding.getMethodHandle().orElse(null);
      }

      FunctionBinding storeCreateBinding = functionBindings.get("wasmtime4j_store_create");
      if (storeCreateBinding != null) {
        this.mhStoreCreate = storeCreateBinding.getMethodHandle().orElse(null);
      }

      FunctionBinding memReadBinding = functionBindings.get("wasmtime4j_panama_memory_read_bytes");
      if (memReadBinding != null) {
        this.mhPanamaMemoryReadBytes = memReadBinding.getMethodHandle().orElse(null);
      }

      FunctionBinding memWriteBinding =
          functionBindings.get("wasmtime4j_panama_memory_write_bytes");
      if (memWriteBinding != null) {
        this.mhPanamaMemoryWriteBytes = memWriteBinding.getMethodHandle().orElse(null);
      }

      FunctionBinding instanceCreateBinding = functionBindings.get("wasmtime4j_instance_create");
      if (instanceCreateBinding != null) {
        this.mhInstanceCreate = instanceCreateBinding.getMethodHandle().orElse(null);
      }

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

      // Use fast path with invokeExact if available
      final MethodHandle mh = mhEngineCreate;
      MemorySegment result;
      if (mh != null) {
        result = (MemorySegment) mh.invokeExact();
      } else {
        result = callNativeFunction("wasmtime4j_engine_create", MemorySegment.class);
      }
      if (result == null || result.equals(MemorySegment.NULL)) {
        LOGGER.warning("Engine creation returned null - this may indicate symbol lookup failure");
      } else {
        LOGGER.fine("Engine created successfully: " + result);
      }
      return result;
    } catch (Throwable e) {
      LOGGER.severe("Exception during engine creation: " + e.getMessage());
      return null;
    }
  }

  /**
   * Creates a new Wasmtime engine with extended configuration.
   *
   * @param config the engine configuration
   * @return memory segment pointer to the engine, or null on failure
   */
  public MemorySegment engineCreateWithConfig(
      final ai.tegmentum.wasmtime4j.config.EngineConfig config) {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeFunctionBindings not initialized, cannot create engine");
        return null;
      }

      // Extract configuration values using EngineConfig getter methods
      int strategy = 0; // default Cranelift
      int optLevel = 2; // default Speed
      int debugInfo = config.isDebugInfo() ? 1 : 0;
      int wasmThreads = config.isWasmThreads() ? 1 : 0;
      int wasmSimd = config.isWasmSimd() ? 1 : 0;
      int wasmReferenceTypes = config.isWasmReferenceTypes() ? 1 : 0;
      int wasmBulkMemory = config.isWasmBulkMemory() ? 1 : 0;
      int wasmMultiValue = config.isWasmMultiValue() ? 1 : 0;
      int fuelEnabled = config.isConsumeFuel() ? 1 : 0;
      int maxMemoryPages = 0; // use default
      int maxStackSize = 0; // use default
      int epochInterruption = config.isEpochInterruption() ? 1 : 0;
      int maxInstances = 0; // use default
      int asyncSupport = config.isAsyncSupport() ? 1 : 0;
      int wasmGc = config.isWasmGc() ? 1 : 0;
      int wasmFunctionReferences = config.isWasmFunctionReferences() ? 1 : 0;
      int wasmExceptions = config.isWasmExceptions() ? 1 : 0;
      long memoryReservation = config.getMemoryReservation();
      long memoryGuardSize = config.getMemoryGuardSize();
      long memoryReservationForGrowth = config.getMemoryReservationForGrowth();
      int wasmTailCall = config.isWasmTailCall() ? 1 : 0;
      int wasmRelaxedSimd = config.isWasmRelaxedSimd() ? 1 : 0;
      int wasmMultiMemory = config.isWasmMultiMemory() ? 1 : 0;
      int wasmMemory64 = config.isWasmMemory64() ? 1 : 0;
      int wasmExtendedConst = config.isWasmExtendedConstExpressions() ? 1 : 0;
      int wasmComponentModel = 0; // Component model handled separately
      int coredumpOnTrap = config.isCoredumpOnTrap() ? 1 : 0;
      int craneliftNanCanonicalization = config.isCraneliftNanCanonicalization() ? 1 : 0;
      // Experimental features
      int wasmCustomPageSizes = config.isWasmCustomPageSizes() ? 1 : 0;
      int wasmWideArithmetic = config.isWasmWideArithmetic() ? 1 : 0;

      MemorySegment result =
          callNativeFunction(
              "wasmtime4j_panama_engine_create_with_extended_config",
              MemorySegment.class,
              strategy,
              optLevel,
              debugInfo,
              wasmThreads,
              wasmSimd,
              wasmReferenceTypes,
              wasmBulkMemory,
              wasmMultiValue,
              fuelEnabled,
              maxMemoryPages,
              maxStackSize,
              epochInterruption,
              maxInstances,
              asyncSupport,
              wasmGc,
              wasmFunctionReferences,
              wasmExceptions,
              memoryReservation,
              memoryGuardSize,
              memoryReservationForGrowth,
              wasmTailCall,
              wasmRelaxedSimd,
              wasmMultiMemory,
              wasmMemory64,
              wasmExtendedConst,
              wasmComponentModel,
              coredumpOnTrap,
              craneliftNanCanonicalization,
              wasmCustomPageSizes,
              wasmWideArithmetic);

      if (result == null || result.equals(MemorySegment.NULL)) {
        LOGGER.warning(
            "Engine creation with config returned null - this may indicate symbol lookup failure");
      } else {
        LOGGER.fine("Engine created with config successfully: " + result);
      }
      return result;
    } catch (Exception e) {
      LOGGER.severe("Exception during engine creation with config: " + e.getMessage());
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
   * Compiles a WebAssembly module from WAT (WebAssembly Text format).
   *
   * @param enginePtr pointer to the engine
   * @param watText pointer to the WAT text string
   * @param modulePtr pointer to store the compiled module
   * @return 0 on success, negative error code on failure
   */
  public int moduleCompileWat(
      final MemorySegment enginePtr, final MemorySegment watText, final MemorySegment modulePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(watText, "watText");
    validatePointer(modulePtr, "modulePtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_compile_wat", Integer.class, enginePtr, watText, modulePtr);
  }

  /**
   * Serializes a WebAssembly module.
   *
   * @param modulePtr pointer to the module to serialize
   * @param dataPtrPtr pointer to receive the serialized data pointer
   * @param lenPtr pointer to receive the data length
   * @return 0 on success, non-zero on error
   */
  public int moduleSerialize(
      final MemorySegment modulePtr, final MemorySegment dataPtrPtr, final MemorySegment lenPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(dataPtrPtr, "dataPtrPtr");
    validatePointer(lenPtr, "lenPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_serialize", Integer.class, modulePtr, dataPtrPtr, lenPtr);
  }

  /**
   * Deserializes a WebAssembly module from serialized bytes.
   *
   * @param enginePtr pointer to the engine
   * @param dataPtr pointer to the serialized module data
   * @param len length of the serialized data
   * @param modulePtrPtr pointer to store the deserialized module pointer
   * @return 0 on success, negative error code on failure
   */
  public int moduleDeserialize(
      final MemorySegment enginePtr,
      final MemorySegment dataPtr,
      final long len,
      final MemorySegment modulePtrPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(dataPtr, "dataPtr");
    validatePointer(modulePtrPtr, "modulePtrPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_deserialize",
        Integer.class,
        enginePtr,
        dataPtr,
        len,
        modulePtrPtr);
  }

  /**
   * Frees serialized module data.
   *
   * @param dataPtr pointer to the serialized data
   * @param len length of the data
   */
  public void moduleFreeSerializedData(final MemorySegment dataPtr, final long len) {
    if (dataPtr != null && !dataPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_module_free_serialized_data", Void.class, dataPtr, len);
    }
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

  /**
   * Creates a WebAssembly module from bytecode (Panama FFI).
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to the WASM bytecode
   * @param wasmSize size of the WASM bytecode
   * @return memory segment pointer to the module, or null on failure
   */
  public MemorySegment moduleCreate(
      final MemorySegment enginePtr, final MemorySegment wasmBytes, final long wasmSize) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    validateSize(wasmSize, "wasmSize");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhModuleCreate;
    if (mh != null) {
      try {
        return (MemorySegment) mh.invokeExact(enginePtr, wasmBytes, wasmSize);
      } catch (Throwable t) {
        throw new RuntimeException("Native moduleCreate failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_module_create", MemorySegment.class, enginePtr, wasmBytes, wasmSize);
  }

  /**
   * Initializes memory from a data segment.
   *
   * <p>This is equivalent to the WebAssembly memory.init instruction.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param instancePtr pointer to the instance
   * @param destOffset destination offset in memory
   * @param dataSegmentIndex data segment index
   * @param srcOffset source offset within the data segment
   * @param length number of bytes to copy
   * @return 0 on success, non-zero on error
   */
  public int panamaMemoryInit(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment instancePtr,
      final int destOffset,
      final int dataSegmentIndex,
      final int srcOffset,
      final int length) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_init",
        Integer.class,
        memoryPtr,
        storePtr,
        instancePtr,
        destOffset,
        dataSegmentIndex,
        srcOffset,
        length);
  }

  /**
   * Drops a data segment from memory.
   *
   * @param instancePtr pointer to the instance
   * @param dataSegmentIndex data segment index to drop
   * @return 0 on success, non-zero on error
   */
  public int dataSegmentDrop(final MemorySegment instancePtr, final int dataSegmentIndex) {
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_data_drop", Integer.class, instancePtr, dataSegmentIndex);
  }

  // ====================================================================================
  // Atomic Memory Operations
  // ====================================================================================

  /**
   * Atomic compare-and-swap on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param newValue new value to swap in
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicCompareAndSwapI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int expected,
      final int newValue,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        newValue,
        resultOut);
  }

  /**
   * Atomic compare-and-swap on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param newValue new value to swap in
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicCompareAndSwapI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long expected,
      final long newValue,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        newValue,
        resultOut);
  }

  /**
   * Atomic load of 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param resultOut pointer to store the value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicLoadI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_load_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        resultOut);
  }

  /**
   * Atomic load of 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param resultOut pointer to store the value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicLoadI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_load_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        resultOut);
  }

  /**
   * Atomic store of 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to store
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicStoreI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_store_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value);
  }

  /**
   * Atomic store of 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to store
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicStoreI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long value) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_store_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value);
  }

  /**
   * Atomic add on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to add
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicAddI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_add_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic add on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to add
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicAddI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_add_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic AND on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to AND
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicAndI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_and_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic OR on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to OR
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicOrI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_or_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic XOR on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to XOR
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicXorI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_xor_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic memory fence.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicFence(final MemorySegment memoryPtr, final MemorySegment storePtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_fence", Integer.class, memoryPtr, storePtr);
  }

  /**
   * Atomic notify/wake.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param count number of waiters to wake
   * @param resultOut pointer to store number of waiters woken
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicNotify(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int count,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_notify",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        count,
        resultOut);
  }

  /**
   * Atomic wait on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param timeoutNanos timeout in nanoseconds (-1 for infinite)
   * @param resultOut pointer to store the wait result (0=ok, 1=not-equal, 2=timed-out)
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicWait32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int expected,
      final long timeoutNanos,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_wait32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        timeoutNanos,
        resultOut);
  }

  /**
   * Atomic wait on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param timeoutNanos timeout in nanoseconds (-1 for infinite)
   * @param resultOut pointer to store the wait result (0=ok, 1=not-equal, 2=timed-out)
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicWait64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long expected,
      final long timeoutNanos,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_wait64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        timeoutNanos,
        resultOut);
  }

  /**
   * Creates a WebAssembly module from WAT text (Panama FFI).
   *
   * @param enginePtr pointer to the engine
   * @param watText pointer to null-terminated WAT text
   * @return memory segment pointer to the module, or null on failure
   */
  public MemorySegment moduleCreateWat(final MemorySegment enginePtr, final MemorySegment watText) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(watText, "watText");
    return callNativeFunction(
        "wasmtime4j_module_create_wat", MemorySegment.class, enginePtr, watText);
  }

  /**
   * Gets the number of imports in a module.
   *
   * @param modulePtr pointer to the module
   * @return the number of imports
   */
  public long moduleImportsLen(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    // Use the existing Panama FFI function
    final int count =
        callNativeFunction("wasmtime4j_panama_module_get_import_count", Integer.class, modulePtr);
    return count;
  }

  /**
   * Gets the nth import from a module.
   *
   * @param modulePtr pointer to the module
   * @param index the index of the import to retrieve
   * @param nameOutPtr pointer to receive the import name
   * @param typeOutPtr pointer to receive the import type
   * @return true if the import exists, false otherwise
   */
  public boolean moduleImportNth(
      final MemorySegment modulePtr,
      final long index,
      final MemorySegment nameOutPtr,
      final MemorySegment typeOutPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(nameOutPtr, "nameOutPtr");
    validatePointer(typeOutPtr, "typeOutPtr");
    return callNativeFunction(
        "wasmtime4j_module_import_nth", Boolean.class, modulePtr, index, nameOutPtr, typeOutPtr);
  }

  /**
   * Gets the number of exports in a module.
   *
   * @param modulePtr pointer to the module
   * @return the number of exports
   */
  public long moduleExportsLen(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    // Use the existing Panama FFI function
    final int count =
        callNativeFunction("wasmtime4j_panama_module_get_export_count", Integer.class, modulePtr);
    return count;
  }

  /**
   * Gets the nth export from a module.
   *
   * @param modulePtr pointer to the module
   * @param index the index of the export to retrieve
   * @param nameOutPtr pointer to receive the export name
   * @param typeOutPtr pointer to receive the export type
   * @return true if the export exists, false otherwise
   */
  public boolean moduleExportNth(
      final MemorySegment modulePtr,
      final long index,
      final MemorySegment nameOutPtr,
      final MemorySegment typeOutPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(nameOutPtr, "nameOutPtr");
    validatePointer(typeOutPtr, "typeOutPtr");
    return callNativeFunction(
        "wasmtime4j_module_export_nth", Boolean.class, modulePtr, index, nameOutPtr, typeOutPtr);
  }

  /**
   * Gets module exports as JSON string.
   *
   * @param modulePtr pointer to the module
   * @return MemorySegment pointing to JSON string, or NULL on error
   */
  public MemorySegment moduleGetExportsJson(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_module_get_exports_json", MemorySegment.class, modulePtr);
  }

  /**
   * Gets module imports as JSON string.
   *
   * @param modulePtr pointer to the module
   * @return MemorySegment pointing to JSON string, or NULL on error
   */
  public MemorySegment moduleGetImportsJson(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_module_get_imports_json", MemorySegment.class, modulePtr);
  }

  /**
   * Gets custom sections from a module as JSON.
   *
   * <p>Returns a JSON string mapping section names to Base64-encoded data.
   *
   * @param modulePtr pointer to the module
   * @return MemorySegment pointing to JSON string, or NULL on error
   */
  public MemorySegment moduleGetCustomSections(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_module_get_custom_sections", MemorySegment.class, modulePtr);
  }

  /**
   * Frees a C string returned by module functions.
   *
   * @param strPtr pointer to the string to free
   */
  public void moduleFreeString(final MemorySegment strPtr) {
    if (strPtr != null && !strPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_module_free_string", Void.class, strPtr);
    }
  }

  /**
   * Gets the name of a module.
   *
   * @param modulePtr pointer to the module
   * @return pointer to the module name string, or null if unnamed
   */
  public MemorySegment moduleGetName(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction("wasmtime4j_module_get_name", MemorySegment.class, modulePtr);
  }

  /**
   * Validates imports against a module.
   *
   * @param modulePtr pointer to the module
   * @param importsPtr pointer to imports array
   * @param importsCount number of imports
   * @return 0 on success, negative error code on failure
   */
  public int moduleValidateImports(
      final MemorySegment modulePtr, final MemorySegment importsPtr, final long importsCount) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(importsPtr, "importsPtr");
    return callNativeFunction(
        "wasmtime4j_module_validate_imports", Integer.class, modulePtr, importsPtr, importsCount);
  }

  /**
   * Gets the number of exports in a module.
   *
   * @param modulePtr pointer to the module
   * @return the number of exports
   */
  public long moduleExportCount(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction("wasmtime4j_module_export_count", Long.class, modulePtr);
  }

  /**
   * Gets export names from a module.
   *
   * @param modulePtr pointer to the module
   * @param namesOut pointer to array that will hold char* pointers
   * @param maxCount maximum number of names to retrieve
   * @return actual number of exports retrieved
   */
  public long moduleGetExportNames(
      final MemorySegment modulePtr, final MemorySegment namesOut, final long maxCount) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(namesOut, "namesOut");
    return callNativeFunction(
        "wasmtime4j_module_get_export_names", Long.class, modulePtr, namesOut, maxCount);
  }

  /**
   * Gets the kind of a specific export by name.
   *
   * @param modulePtr pointer to the module
   * @param name pointer to null-terminated C string name
   * @return 0=not found, 1=function, 2=global, 3=memory, 4=table
   */
  public int moduleGetExportKind(final MemorySegment modulePtr, final MemorySegment name) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(name, "name");
    return callNativeFunction("wasmtime4j_module_get_export_kind", Integer.class, modulePtr, name);
  }

  // Store Functions

  /**
   * Creates a new Wasmtime store.
   *
   * @param enginePtr pointer to the engine
   * @return memory segment pointer to the store, or null on failure
   */
  public MemorySegment storeCreate(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhStoreCreate;
    if (mh != null) {
      try {
        return (MemorySegment) mh.invokeExact(enginePtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native storeCreate failed", t);
      }
    }
    return callNativeFunction("wasmtime4j_store_create", MemorySegment.class, enginePtr);
  }

  /**
   * Creates a WebAssembly store that is compatible with a specific module.
   *
   * <p>CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Engine Arc as the
   * Module's internal wasmtime::Module. This is required because wasmtime's Instance::new() uses
   * Arc::ptr_eq() to verify engine compatibility.
   *
   * @param modulePtr pointer to the module
   * @return memory segment pointer to the store, or null on failure
   */
  public MemorySegment storeCreateForModule(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction("wasmtime4j_store_new_for_module", MemorySegment.class, modulePtr);
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
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @param storePtr pointer to store the created store (output parameter)
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
        "wasmtime4j_panama_store_create_with_config",
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
   * Sets WASI context on a WebAssembly store.
   *
   * <p>This function attaches a WASI context to a store, which is required before instantiating
   * modules that import WASI functions.
   *
   * @param storePtr pointer to the store
   * @param wasiCtxPtr pointer to the WASI context
   * @return 0 on success, negative error code on failure
   */
  public int storeSetWasiContext(final MemorySegment storePtr, final MemorySegment wasiCtxPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(wasiCtxPtr, "wasiCtxPtr");
    return callNativeFunction(
        "wasmtime4j_store_set_wasi_context", Integer.class, storePtr, wasiCtxPtr);
  }

  /**
   * Checks if a store has WASI context attached.
   *
   * @param storePtr pointer to the store
   * @return 1 if store has WASI context, 0 if not, negative on error
   */
  public int storeHasWasiContext(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_has_wasi_context", Integer.class, storePtr);
  }

  /**
   * Creates a new WebAssembly global variable.
   *
   * @param storePtr pointer to the store
   * @param valueType the WebAssembly value type code
   * @param isMutable 1 if mutable, 0 if immutable
   * @param initialValue the initial value for the global
   * @return pointer to the created global, or null on failure
   */
  public MemorySegment globalCreate(
      final MemorySegment storePtr,
      final int valueType,
      final int isMutable,
      final WasmValue initialValue) {
    validatePointer(storePtr, "storePtr");

    // Extract value components for native call
    Object[] valueComponents = extractValueForNative(initialValue);

    return callNativeFunction(
        "wasmtime4j_global_create",
        MemorySegment.class,
        storePtr,
        valueType,
        isMutable,
        valueComponents[0], // i32
        valueComponents[1], // i64
        valueComponents[2], // f32
        valueComponents[3], // f64
        valueComponents[4] // ref
        );
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
   * Captures backtrace from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param bufferOutPtr pointer to store the backtrace buffer pointer
   * @param bufferLenOutPtr pointer to store the buffer length
   * @return 0 on success, negative error code on failure
   */
  public int storeCaptureBacktrace(
      final MemorySegment storePtr,
      final MemorySegment bufferOutPtr,
      final MemorySegment bufferLenOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferOutPtr, "bufferOutPtr");
    validatePointer(bufferLenOutPtr, "bufferLenOutPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_capture_backtrace",
        Integer.class,
        storePtr,
        bufferOutPtr,
        bufferLenOutPtr);
  }

  /**
   * Force captures backtrace from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param bufferOutPtr pointer to store the backtrace buffer pointer
   * @param bufferLenOutPtr pointer to store the buffer length
   * @return 0 on success, negative error code on failure
   */
  public int storeForceCaptureBacktrace(
      final MemorySegment storePtr,
      final MemorySegment bufferOutPtr,
      final MemorySegment bufferLenOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferOutPtr, "bufferOutPtr");
    validatePointer(bufferLenOutPtr, "bufferLenOutPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_force_capture_backtrace",
        Integer.class,
        storePtr,
        bufferOutPtr,
        bufferLenOutPtr);
  }

  /**
   * Performs garbage collection on a WebAssembly store.
   *
   * <p>This triggers garbage collection to reclaim unreachable GC objects. If GC support is not
   * enabled in the engine configuration, this is a no-op.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGc(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_gc", Integer.class, storePtr);
  }

  /**
   * Performs asynchronous garbage collection on a WebAssembly store.
   *
   * <p>This is the async version of {@link #storeGc(MemorySegment)} that cooperatively yields
   * during GC if the store is configured with epoch-based interruption.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGcAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_gc_async", Integer.class, storePtr);
  }

  /**
   * Configures the epoch deadline to async yield and update.
   *
   * <p>When the epoch deadline is reached, execution will yield back to the async executor and then
   * continue with a new deadline of current epoch + deltaTicks.
   *
   * @param storePtr pointer to the store
   * @param deltaTicks number of ticks to add for the new deadline
   * @return 0 on success, negative error code on failure
   */
  public int storeEpochDeadlineAsyncYieldAndUpdate(
      final MemorySegment storePtr, final long deltaTicks) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_epoch_deadline_async_yield_and_update",
        Integer.class,
        storePtr,
        deltaTicks);
  }

  /**
   * Configures the epoch deadline to trap when reached.
   *
   * <p>When the epoch deadline is reached, execution will trap immediately.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeEpochDeadlineTrap(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_epoch_deadline_trap", Integer.class, storePtr);
  }

  /**
   * Sets an epoch deadline callback on the store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeSetEpochDeadlineCallback(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_epoch_deadline_callback", Integer.class, storePtr);
  }

  /**
   * Clears the epoch deadline callback from the store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeClearEpochDeadlineCallback(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_clear_epoch_deadline_callback", Integer.class, storePtr);
  }

  /**
   * Sets an epoch deadline callback with a function pointer on the store.
   *
   * <p>This allows the Java code to receive callbacks when the epoch deadline is reached and decide
   * whether to continue execution (by returning a positive delta) or trap (by returning a negative
   * value).
   *
   * @param storePtr pointer to the store
   * @param callbackFn function pointer for the epoch callback (takes callback_id, epoch; returns
   *     delta or negative to trap)
   * @param callbackId identifier passed to the callback to identify the Java callback
   * @return 0 on success, negative error code on failure
   */
  public int storeSetEpochDeadlineCallbackFn(
      final MemorySegment storePtr, final MemorySegment callbackFn, final long callbackId) {
    validatePointer(storePtr, "storePtr");
    validatePointer(callbackFn, "callbackFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_epoch_deadline_callback_fn",
        Integer.class,
        storePtr,
        callbackFn,
        callbackId);
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

  // Instance Functions

  /**
   * Creates a WebAssembly instance (Panama FFI).
   *
   * @param storePtr pointer to the store
   * @param modulePtr pointer to the module
   * @return memory segment pointer to the instance, or null on failure
   */
  public MemorySegment instanceCreate(final MemorySegment storePtr, final MemorySegment modulePtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhInstanceCreate;
    if (mh != null) {
      try {
        return (MemorySegment) mh.invokeExact(storePtr, modulePtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native instanceCreate failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_instance_create", MemorySegment.class, storePtr, modulePtr);
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
   * Calls a WebAssembly function in an instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param functionName name of the function to call
   * @param paramsPtr pointer to array of WasmValue parameters
   * @param paramCount number of parameters
   * @param resultsPtr pointer to buffer for WasmValue results
   * @param maxResults maximum number of results to return
   * @return number of actual results (0 on error)
   */
  public long instanceCallFunction(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment functionName,
      final MemorySegment paramsPtr,
      final long paramCount,
      final MemorySegment resultsPtr,
      final long maxResults) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(functionName, "functionName");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhInstanceCallFunction;
    if (mh != null) {
      try {
        return (long)
            mh.invokeExact(
                instancePtr, storePtr, functionName, paramsPtr, paramCount, resultsPtr, maxResults);
      } catch (Throwable t) {
        throw new RuntimeException("Native instanceCallFunction failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_instance_call_function",
        Long.class,
        instancePtr,
        storePtr,
        functionName,
        paramsPtr,
        paramCount,
        resultsPtr,
        maxResults);
  }

  /**
   * Fast path for instance function calls using invokeExact.
   *
   * <p>This method is optimized for performance by using invokeExact instead of
   * invokeWithArguments, which avoids varargs array creation, primitive boxing, and runtime type
   * checking.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param functionName name of the function to call
   * @param paramsPtr pointer to array of WasmValue parameters
   * @param paramCount number of parameters
   * @param resultsPtr pointer to buffer for WasmValue results
   * @param maxResults maximum number of results to return
   * @return number of actual results (0 on error)
   */
  public long instanceCallFunctionFast(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment functionName,
      final MemorySegment paramsPtr,
      final long paramCount,
      final MemorySegment resultsPtr,
      final long maxResults) {
    final MethodHandle mh = mhInstanceCallFunction;
    if (mh == null) {
      // Fall back to slow path if handle not available
      return instanceCallFunction(
          instancePtr, storePtr, functionName, paramsPtr, paramCount, resultsPtr, maxResults);
    }
    try {
      return (long)
          mh.invokeExact(
              instancePtr, storePtr, functionName, paramsPtr, paramCount, resultsPtr, maxResults);
    } catch (Throwable t) {
      throw new RuntimeException("Native instanceCallFunction failed", t);
    }
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
   * Gets a memory export by name from an instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the memory export
   * @return memory segment pointer or null if not found
   */
  public MemorySegment instanceGetMemoryByName(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_get_memory_by_name", MemorySegment.class, instancePtr, storePtr, name);
  }

  /**
   * Checks if an instance has a memory export with the given name.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the memory export
   * @return 1 if exists, 0 if not found
   */
  public int instanceHasMemoryExport(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_has_memory_export", Integer.class, instancePtr, storePtr, name);
  }

  /**
   * Gets memory size in pages by looking up the memory fresh.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the memory export
   * @param sizeOut pointer to store the size
   * @return 0 on success, negative error code on failure
   */
  public int instanceGetMemorySizePages(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final MemorySegment sizeOut) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    validatePointer(sizeOut, "sizeOut");
    return callNativeFunction(
        "wasmtime4j_instance_get_memory_size_pages",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        sizeOut);
  }

  /**
   * Gets memory size in bytes by looking up the memory fresh.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the memory export
   * @param sizeOut pointer to store the size
   * @return 0 on success, negative error code on failure
   */
  public int instanceGetMemorySizeBytes(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final MemorySegment sizeOut) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    validatePointer(sizeOut, "sizeOut");
    return callNativeFunction(
        "wasmtime4j_instance_get_memory_size_bytes",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        sizeOut);
  }

  /**
   * Grows memory by looking up the memory fresh.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the memory export
   * @param pages number of pages to grow
   * @param previousPagesOut pointer to store previous size
   * @return 0 on success, negative error code on failure
   */
  public int instanceGrowMemory(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final int pages,
      final MemorySegment previousPagesOut) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    validatePointer(previousPagesOut, "previousPagesOut");
    return callNativeFunction(
        "wasmtime4j_instance_grow_memory",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        pages,
        previousPagesOut);
  }

  /**
   * Reads bytes from memory by looking up the memory fresh.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the memory export
   * @param offset offset in memory
   * @param length number of bytes to read
   * @param buffer pointer to buffer to read into
   * @return 0 on success, negative error code on failure
   */
  public int instanceReadMemoryBytes(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final long offset,
      final long length,
      final MemorySegment buffer) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    validatePointer(buffer, "buffer");
    return callNativeFunction(
        "wasmtime4j_instance_read_memory_bytes",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        offset,
        length,
        buffer);
  }

  /**
   * Writes bytes to memory by looking up the memory fresh.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the memory export
   * @param offset offset in memory
   * @param length number of bytes to write
   * @param buffer pointer to buffer to write from
   * @return 0 on success, negative error code on failure
   */
  public int instanceWriteMemoryBytes(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final long offset,
      final long length,
      final MemorySegment buffer) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    validatePointer(buffer, "buffer");
    return callNativeFunction(
        "wasmtime4j_instance_write_memory_bytes",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        offset,
        length,
        buffer);
  }

  /**
   * Gets the type and mutability of a global export.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the global export
   * @param valueTypeOut output for value type code
   * @param isMutableOut output for mutability flag
   * @return 0 on success, negative error code on failure
   */
  public int instanceGetGlobalType(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final MemorySegment valueTypeOut,
      final MemorySegment isMutableOut) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_get_global_type",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        valueTypeOut,
        isMutableOut);
  }

  /**
   * Checks if a global export exists in an instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the global export
   * @return 0 if global exists, non-zero if not found or error
   */
  public int instanceHasGlobalExport(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_has_global_export", Integer.class, instancePtr, storePtr, name);
  }

  /**
   * Gets the value of a global by looking it up fresh from the instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the global export
   * @param i32Out output for i32 value
   * @param i64Out output for i64 value
   * @param f32Out output for f32 value (as double)
   * @param f64Out output for f64 value
   * @param refIdPresentOut output for reference presence flag
   * @param refIdOut output for reference ID
   * @return 0 on success, negative error code on failure
   */
  public int instanceGetGlobalValue(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final MemorySegment i32Out,
      final MemorySegment i64Out,
      final MemorySegment f32Out,
      final MemorySegment f64Out,
      final MemorySegment refIdPresentOut,
      final MemorySegment refIdOut) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_get_global_value",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        i32Out,
        i64Out,
        f32Out,
        f64Out,
        refIdPresentOut,
        refIdOut);
  }

  /**
   * Sets the value of a global by looking it up fresh from the instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the global export
   * @param valueTypeCode type code (0=I32, 1=I64, 2=F32, 3=F64, 5=FuncRef, 6=ExternRef)
   * @param i32Value i32 value
   * @param i64Value i64 value
   * @param f32Value f32 value (as double)
   * @param f64Value f64 value
   * @param refIdPresent reference presence flag
   * @param refId reference ID
   * @return 0 on success, negative error code on failure
   */
  public int instanceSetGlobalValue(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final int valueTypeCode,
      final int i32Value,
      final long i64Value,
      final double f32Value,
      final double f64Value,
      final int refIdPresent,
      final long refId) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_set_global_value",
        Integer.class,
        instancePtr,
        storePtr,
        name,
        valueTypeCode,
        i32Value,
        i64Value,
        f32Value,
        f64Value,
        refIdPresent,
        refId);
  }

  /**
   * Gets a table export by name from an instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the table export
   * @return table segment pointer or null if not found
   */
  public MemorySegment instanceGetTableByName(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_get_table_by_name", MemorySegment.class, instancePtr, storePtr, name);
  }

  /**
   * Gets a global export by name from an instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the global export
   * @return global segment pointer or null if not found
   */
  public MemorySegment instanceGetGlobalByName(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_get_global_by_name", MemorySegment.class, instancePtr, storePtr, name);
  }

  /**
   * Gets a global export by name from an instance, wrapped for linker use.
   *
   * <p>Unlike {@link #instanceGetGlobalByName}, this returns a properly wrapped Global struct that
   * can be used with {@link #panamaLinkerDefineGlobal}.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the global export
   * @return wrapped global segment pointer or null if not found
   */
  public MemorySegment instanceGetGlobalWrapped(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_panama_instance_get_global_wrapped",
        MemorySegment.class,
        instancePtr,
        storePtr,
        name);
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
   * Gets the method handle for Panama FFI table size retrieval.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableSize() {
    return getMethodHandle("wasmtime4j_panama_table_size").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table element get.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableGet() {
    return getMethodHandle("wasmtime4j_panama_table_get").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table element set.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableSet() {
    return getMethodHandle("wasmtime4j_panama_table_set").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table grow.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableGrow() {
    return getMethodHandle("wasmtime4j_panama_table_grow").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table metadata retrieval.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableMetadata() {
    return getMethodHandle("wasmtime4j_panama_table_metadata").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table fill.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableFill() {
    return getMethodHandle("wasmtime4j_panama_table_fill").orElse(null);
  }

  /**
   * Copies elements within a table (same table).
   *
   * <p>This is equivalent to the WebAssembly table.copy instruction when copying within the same
   * table.
   *
   * @param tablePtr pointer to the table
   * @param storePtr pointer to the store
   * @param dst destination index in the table
   * @param src source index in the table
   * @param count number of elements to copy
   * @return 0 on success, non-zero on error
   */
  public int panamaTableCopy(
      final MemorySegment tablePtr,
      final MemorySegment storePtr,
      final int dst,
      final int src,
      final int count) {
    validatePointer(tablePtr, "tablePtr");
    validatePointer(storePtr, "storePtr");

    return callNativeFunction(
        "wasmtime4j_panama_table_copy", Integer.class, tablePtr, storePtr, dst, src, count);
  }

  /**
   * Copies elements from one table to another.
   *
   * <p>This is equivalent to the WebAssembly table.copy instruction when copying between different
   * tables.
   *
   * @param dstTablePtr pointer to the destination table
   * @param storePtr pointer to the store
   * @param dst destination index in the destination table
   * @param srcTablePtr pointer to the source table
   * @param src source index in the source table
   * @param count number of elements to copy
   * @return 0 on success, non-zero on error
   */
  public int panamaTableCopyFrom(
      final MemorySegment dstTablePtr,
      final MemorySegment storePtr,
      final int dst,
      final MemorySegment srcTablePtr,
      final int src,
      final int count) {
    validatePointer(dstTablePtr, "dstTablePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(srcTablePtr, "srcTablePtr");

    return callNativeFunction(
        "wasmtime4j_panama_table_copy_from",
        Integer.class,
        dstTablePtr,
        storePtr,
        dst,
        srcTablePtr,
        src,
        count);
  }

  /**
   * Gets the method handle for Panama FFI table deletion.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableDelete() {
    return getMethodHandle("wasmtime4j_panama_table_destroy").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI memory creation (simple version).
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaMemoryCreate() {
    return getMethodHandle("wasmtime4j_panama_memory_create").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI memory creation (with configuration).
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaMemoryCreateWithConfig() {
    return getMethodHandle("wasmtime4j_panama_memory_create_with_config").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table creation.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableCreate() {
    return getMethodHandle("wasmtime4j_panama_table_create").orElse(null);
  }

  /**
   * Initializes a table from an element segment.
   *
   * <p>This is equivalent to the WebAssembly table.init instruction.
   *
   * @param tablePtr pointer to the table
   * @param storePtr pointer to the store
   * @param instancePtr pointer to the instance
   * @param destIndex destination index in the table
   * @param srcIndex source index in the element segment
   * @param count number of elements to copy
   * @param segmentIndex element segment index
   * @return 0 on success, non-zero on error
   */
  public int panamaTableInit(
      final MemorySegment tablePtr,
      final MemorySegment storePtr,
      final MemorySegment instancePtr,
      final int destIndex,
      final int srcIndex,
      final int count,
      final int segmentIndex) {
    validatePointer(tablePtr, "tablePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_table_init",
        Integer.class,
        tablePtr,
        storePtr,
        instancePtr,
        destIndex,
        srcIndex,
        count,
        segmentIndex);
  }

  /**
   * Drops an element segment.
   *
   * @param instancePtr pointer to the instance
   * @param segmentIndex element segment index to drop
   * @return 0 on success, non-zero on error
   */
  public int elemDrop(final MemorySegment instancePtr, final int segmentIndex) {
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_elem_drop", Integer.class, instancePtr, segmentIndex);
  }

  /**
   * Gets the method handle for Panama FFI global creation.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaGlobalCreate() {
    return getMethodHandle("wasmtime4j_panama_global_create").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI instance creation.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaInstanceCreate() {
    return getMethodHandle("wasmtime4j_panama_instance_create").orElse(null);
  }

  /**
   * Gets the method handle for setting fuel level in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetFuel() {
    return getMethodHandle("wasmtime4j_panama_store_set_fuel").orElse(null);
  }

  /**
   * Gets the method handle for getting remaining fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetFuel() {
    return getMethodHandle("wasmtime4j_panama_store_get_fuel").orElse(null);
  }

  /**
   * Gets the method handle for adding fuel to a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreAddFuel() {
    return getMethodHandle("wasmtime4j_panama_store_add_fuel").orElse(null);
  }

  /**
   * Gets the method handle for consuming fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreConsumeFuel() {
    return getMethodHandle("wasmtime4j_panama_store_consume_fuel").orElse(null);
  }

  /**
   * Gets the method handle for getting remaining fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetFuelRemaining() {
    return getMethodHandle("wasmtime4j_panama_store_get_fuel_remaining").orElse(null);
  }

  /**
   * Gets the method handle for setting epoch deadline in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetEpochDeadline() {
    return getMethodHandle("wasmtime4j_panama_store_set_epoch_deadline").orElse(null);
  }

  /**
   * Gets the method handle for getting execution statistics from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetExecutionStats() {
    return getMethodHandle("wasmtime4j_panama_store_get_execution_stats").orElse(null);
  }

  /**
   * Gets the method handle for creating a host function in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreCreateHostFunction() {
    return getMethodHandle("wasmtime4j_panama_store_create_host_function").orElse(null);
  }

  /**
   * Gets the method handle for destroying a host function.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaDestroyHostFunction() {
    return getMethodHandle("wasmtime4j_panama_destroy_host_function").orElse(null);
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

  // Component Linker Functions

  /**
   * Creates a new component linker from a component engine.
   *
   * @param enginePtr pointer to the component engine
   * @return pointer to the new component linker, or null on failure
   */
  public MemorySegment componentLinkerCreate(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_component_linker_new", MemorySegment.class, enginePtr);
  }

  /**
   * Creates a new component linker from a Wasmtime engine.
   *
   * @param enginePtr pointer to the Wasmtime engine
   * @return pointer to the new component linker, or null on failure
   */
  public MemorySegment componentLinkerCreateWithEngine(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_new_with_engine", MemorySegment.class, enginePtr);
  }

  /**
   * Destroys a component linker.
   *
   * @param linkerPtr pointer to the component linker to destroy
   */
  public void componentLinkerDestroy(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    callNativeFunction("wasmtime4j_component_linker_destroy", Void.class, linkerPtr);
  }

  /**
   * Checks if a component linker is valid.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if valid, 0 if invalid
   */
  public int componentLinkerIsValid(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction("wasmtime4j_component_linker_is_valid", Integer.class, linkerPtr);
  }

  /**
   * Disposes a component linker's resources.
   *
   * @param linkerPtr pointer to the component linker
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerDispose(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction("wasmtime4j_component_linker_dispose", Integer.class, linkerPtr);
  }

  /**
   * Checks if an interface is defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param namespacePtr pointer to namespace string
   * @param interfaceNamePtr pointer to interface name string
   * @return 1 if present, 0 if not, -1 on error
   */
  public int componentLinkerHasInterface(
      final MemorySegment linkerPtr,
      final MemorySegment namespacePtr,
      final MemorySegment interfaceNamePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(namespacePtr, "namespacePtr");
    validatePointer(interfaceNamePtr, "interfaceNamePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_has_interface",
        Integer.class,
        linkerPtr,
        namespacePtr,
        interfaceNamePtr);
  }

  /**
   * Checks if a function is defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param namespacePtr pointer to namespace string
   * @param interfaceNamePtr pointer to interface name string
   * @param functionNamePtr pointer to function name string
   * @return 1 if present, 0 if not, -1 on error
   */
  public int componentLinkerHasFunction(
      final MemorySegment linkerPtr,
      final MemorySegment namespacePtr,
      final MemorySegment interfaceNamePtr,
      final MemorySegment functionNamePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(namespacePtr, "namespacePtr");
    validatePointer(interfaceNamePtr, "interfaceNamePtr");
    validatePointer(functionNamePtr, "functionNamePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_has_function",
        Integer.class,
        linkerPtr,
        namespacePtr,
        interfaceNamePtr,
        functionNamePtr);
  }

  /**
   * Gets the number of host functions defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return the number of host functions
   */
  public long componentLinkerHostFunctionCount(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_host_function_count", Long.class, linkerPtr);
  }

  /**
   * Gets the number of interfaces defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return the number of interfaces
   */
  public long componentLinkerInterfaceCount(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction("wasmtime4j_component_linker_interface_count", Long.class, linkerPtr);
  }

  /**
   * Checks if WASI Preview 2 is enabled in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if enabled, 0 if not
   */
  public int componentLinkerWasiP2Enabled(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_wasi_p2_enabled", Integer.class, linkerPtr);
  }

  /**
   * Enables WASI Preview 2 in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerEnableWasiP2(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_enable_wasi_p2", Integer.class, linkerPtr);
  }

  /**
   * Sets WASI Preview 2 arguments for the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param argsJsonPtr pointer to JSON array of argument strings
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiArgs(
      final MemorySegment linkerPtr, final MemorySegment argsJsonPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(argsJsonPtr, "argsJsonPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_args", Integer.class, linkerPtr, argsJsonPtr);
  }

  /**
   * Adds a WASI Preview 2 environment variable to the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param keyPtr pointer to environment variable key string
   * @param valuePtr pointer to environment variable value string
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerAddWasiEnv(
      final MemorySegment linkerPtr, final MemorySegment keyPtr, final MemorySegment valuePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(valuePtr, "valuePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_add_wasi_env", Integer.class, linkerPtr, keyPtr, valuePtr);
  }

  /**
   * Sets whether to inherit environment from host in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritEnv(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_env", Integer.class, linkerPtr, inherit);
  }

  /**
   * Sets whether to inherit stdio from host in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritStdio(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_stdio", Integer.class, linkerPtr, inherit);
  }

  /**
   * Adds a preopened directory for WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param hostPathPtr pointer to host path string
   * @param guestPathPtr pointer to guest path string
   * @param readOnly 1 for read-only, 0 for read-write
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerAddWasiPreopenDir(
      final MemorySegment linkerPtr,
      final MemorySegment hostPathPtr,
      final MemorySegment guestPathPtr,
      final int readOnly) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(hostPathPtr, "hostPathPtr");
    validatePointer(guestPathPtr, "guestPathPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_add_wasi_preopen_dir",
        Integer.class,
        linkerPtr,
        hostPathPtr,
        guestPathPtr,
        readOnly);
  }

  /**
   * Sets whether network access is allowed in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowNetwork(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_network", Integer.class, linkerPtr, allow);
  }

  /**
   * Sets whether clock access is allowed in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowClock(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_clock", Integer.class, linkerPtr, allow);
  }

  /**
   * Sets whether random number generation is allowed in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowRandom(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_random", Integer.class, linkerPtr, allow);
  }

  /**
   * Enables WASI HTTP support in the component linker.
   *
   * <p>This enables HTTP request/response functionality in WebAssembly components. WASI Preview 2
   * must be enabled first for this to work.
   *
   * @param linkerPtr pointer to the component linker
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerEnableWasiHttp(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_enable_wasi_http", Integer.class, linkerPtr);
  }

  /**
   * Checks if WASI HTTP is enabled in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if WASI HTTP is enabled, 0 if not, negative on error
   */
  public int componentLinkerWasiHttpEnabled(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_wasi_http_enabled", Integer.class, linkerPtr);
  }

  /**
   * Instantiates a component using the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param componentPtr pointer to the component
   * @param instanceOutPtr pointer to store the instance pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerInstantiate(
      final MemorySegment linkerPtr,
      final MemorySegment componentPtr,
      final MemorySegment instanceOutPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(componentPtr, "componentPtr");
    validatePointer(instanceOutPtr, "instanceOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_instantiate",
        Integer.class,
        linkerPtr,
        componentPtr,
        instanceOutPtr);
  }

  /**
   * Gets all defined interface names from the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param jsonOutPtr pointer to store the JSON string pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerGetInterfaces(
      final MemorySegment linkerPtr, final MemorySegment jsonOutPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(jsonOutPtr, "jsonOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_get_interfaces", Integer.class, linkerPtr, jsonOutPtr);
  }

  /**
   * Gets all functions defined for a specific interface.
   *
   * @param linkerPtr pointer to the component linker
   * @param namespacePtr pointer to namespace string
   * @param interfaceNamePtr pointer to interface name string
   * @param jsonOutPtr pointer to store the JSON string pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerGetFunctions(
      final MemorySegment linkerPtr,
      final MemorySegment namespacePtr,
      final MemorySegment interfaceNamePtr,
      final MemorySegment jsonOutPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(namespacePtr, "namespacePtr");
    validatePointer(interfaceNamePtr, "interfaceNamePtr");
    validatePointer(jsonOutPtr, "jsonOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_get_functions",
        Integer.class,
        linkerPtr,
        namespacePtr,
        interfaceNamePtr,
        jsonOutPtr);
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
   * Gets the value of a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param storePtr pointer to the store
   * @param i32ValueOutPtr pointer to store i32 value
   * @param i64ValueOutPtr pointer to store i64 value
   * @param f32ValueOutPtr pointer to store f32 value
   * @param f64ValueOutPtr pointer to store f64 value
   * @param refIdPresentOutPtr pointer to store reference presence flag
   * @param refIdOutPtr pointer to store reference ID
   * @param v128BytesOutPtr pointer to store V128 bytes (16 bytes, can be null)
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalGet(
      final MemorySegment globalPtr,
      final MemorySegment storePtr,
      final MemorySegment i32ValueOutPtr,
      final MemorySegment i64ValueOutPtr,
      final MemorySegment f32ValueOutPtr,
      final MemorySegment f64ValueOutPtr,
      final MemorySegment refIdPresentOutPtr,
      final MemorySegment refIdOutPtr,
      final MemorySegment v128BytesOutPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(i32ValueOutPtr, "i32ValueOutPtr");
    validatePointer(i64ValueOutPtr, "i64ValueOutPtr");
    validatePointer(f32ValueOutPtr, "f32ValueOutPtr");
    validatePointer(f64ValueOutPtr, "f64ValueOutPtr");
    validatePointer(refIdPresentOutPtr, "refIdPresentOutPtr");
    validatePointer(refIdOutPtr, "refIdOutPtr");
    // v128BytesOutPtr can be null

    return callNativeFunction(
        "wasmtime4j_panama_global_get",
        Integer.class,
        globalPtr,
        storePtr,
        i32ValueOutPtr,
        i64ValueOutPtr,
        f32ValueOutPtr,
        f64ValueOutPtr,
        refIdPresentOutPtr,
        refIdOutPtr,
        v128BytesOutPtr == null ? MemorySegment.NULL : v128BytesOutPtr);
  }

  /**
   * Sets the value of a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param storePtr pointer to the store
   * @param valueType the value type (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FuncRef, 6=ExternRef)
   * @param i32Value i32 value
   * @param i64Value i64 value
   * @param f32Value f32 value
   * @param f64Value f64 value
   * @param refIdPresent reference presence flag
   * @param refId reference ID
   * @param v128BytesPtr pointer to V128 bytes (16 bytes, can be null)
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalSet(
      final MemorySegment globalPtr,
      final MemorySegment storePtr,
      final int valueType,
      final int i32Value,
      final long i64Value,
      final double f32Value,
      final double f64Value,
      final int refIdPresent,
      final long refId,
      final MemorySegment v128BytesPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");
    // v128BytesPtr can be null

    return callNativeFunction(
        "wasmtime4j_panama_global_set",
        Integer.class,
        globalPtr,
        storePtr,
        valueType,
        i32Value,
        i64Value,
        f32Value,
        f64Value,
        refIdPresent,
        refId,
        v128BytesPtr == null ? MemorySegment.NULL : v128BytesPtr);
  }

  /**
   * Sets a WebAssembly global value using WasmValue (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param storePtr pointer to the store
   * @param value the value to set
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalSet(
      final MemorySegment globalPtr, final MemorySegment storePtr, final WasmValue value) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");

    // Extract value components
    int i32Value = 0;
    long i64Value = 0;
    double f32Value = 0.0;
    double f64Value = 0.0;
    int refIdPresent = 0;
    long refId = 0;
    MemorySegment v128BytesPtr = null;

    switch (value.getType()) {
      case I32:
        i32Value = value.asI32();
        break;
      case I64:
        i64Value = value.asI64();
        break;
      case F32:
        f32Value = value.asF32();
        break;
      case F64:
        f64Value = value.asF64();
        break;
      case V128:
        final byte[] v128Bytes = value.asV128();
        if (v128Bytes != null && v128Bytes.length == 16) {
          v128BytesPtr = Arena.global().allocate(16);
          MemorySegment.copy(v128Bytes, 0, v128BytesPtr, ValueLayout.JAVA_BYTE, 0, 16);
        }
        break;
      case FUNCREF:
        final Object funcRef = value.asFuncref();
        if (funcRef != null) {
          refIdPresent = 1;
          if (funcRef instanceof FunctionReference) {
            refId = ((FunctionReference) funcRef).getId();
          } else if (funcRef instanceof Long) {
            refId = (Long) funcRef;
          } else {
            throw new IllegalArgumentException(
                "FUNCREF value must be FunctionReference or Long, got: " + funcRef.getClass());
          }
        }
        break;
      case EXTERNREF:
        final Object externRef = value.asExternref();
        if (externRef != null) {
          refIdPresent = 1;
          if (externRef instanceof ExternRef) {
            refId = ((ExternRef<?>) externRef).getId();
          } else if (externRef instanceof Long) {
            refId = (Long) externRef;
          } else {
            throw new IllegalArgumentException(
                "EXTERNREF value must be ExternRef or Long, got: " + externRef.getClass());
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }

    return panamaGlobalSet(
        globalPtr,
        storePtr,
        value.getType().toNativeTypeCode(),
        i32Value,
        i64Value,
        f32Value,
        f64Value,
        refIdPresent,
        refId,
        v128BytesPtr);
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
   * Creates a new WebAssembly global with a WasmValue (Panama FFI version).
   *
   * @param storePtr pointer to the store
   * @param valueType the value type (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FuncRef, 6=ExternRef)
   * @param mutability mutability flag (0=const, 1=var)
   * @param value the initial value
   * @param namePtr optional name pointer (can be null)
   * @param globalPtrPtr pointer to store the created global pointer
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalCreate(
      final MemorySegment storePtr,
      final int valueType,
      final int mutability,
      final WasmValue value,
      final MemorySegment namePtr,
      final MemorySegment globalPtrPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(globalPtrPtr, "globalPtrPtr");

    // Extract value components
    int i32Value = 0;
    long i64Value = 0;
    double f32Value = 0.0;
    double f64Value = 0.0;
    int refIdPresent = 0;
    long refId = 0;
    MemorySegment v128BytesPtr = MemorySegment.NULL;

    switch (value.getType()) {
      case I32:
        i32Value = value.asI32();
        break;
      case I64:
        i64Value = value.asI64();
        break;
      case F32:
        f32Value = value.asF32();
        break;
      case F64:
        f64Value = value.asF64();
        break;
      case V128:
        final byte[] v128Bytes = value.asV128();
        if (v128Bytes != null && v128Bytes.length == 16) {
          v128BytesPtr = Arena.global().allocate(16);
          MemorySegment.copy(v128Bytes, 0, v128BytesPtr, ValueLayout.JAVA_BYTE, 0, 16);
        }
        break;
      case FUNCREF:
        final Object funcRef2 = value.asFuncref();
        if (funcRef2 != null) {
          refIdPresent = 1;
          if (funcRef2 instanceof FunctionReference) {
            refId = ((FunctionReference) funcRef2).getId();
          } else if (funcRef2 instanceof Long) {
            refId = (Long) funcRef2;
          } else {
            throw new IllegalArgumentException(
                "FUNCREF value must be FunctionReference or Long, got: " + funcRef2.getClass());
          }
        }
        break;
      case EXTERNREF:
        final Object externRef2 = value.asExternref();
        if (externRef2 != null) {
          refIdPresent = 1;
          if (externRef2 instanceof ExternRef) {
            refId = ((ExternRef<?>) externRef2).getId();
          } else if (externRef2 instanceof Long) {
            refId = (Long) externRef2;
          } else {
            throw new IllegalArgumentException(
                "EXTERNREF value must be ExternRef or Long, got: " + externRef2.getClass());
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }

    return callNativeFunction(
        "wasmtime4j_panama_global_create",
        Integer.class,
        storePtr,
        valueType,
        mutability,
        i32Value,
        i64Value,
        f32Value,
        f64Value,
        refIdPresent,
        refId,
        v128BytesPtr,
        namePtr == null ? MemorySegment.NULL : namePtr,
        globalPtrPtr);
  }

  /**
   * Gets metadata for a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param valueTypePtr pointer to store the value type code
   * @param mutabilityPtr pointer to store the mutability flag
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalMetadata(
      final MemorySegment globalPtr,
      final MemorySegment valueTypePtr,
      final MemorySegment mutabilityPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(valueTypePtr, "valueTypePtr");
    validatePointer(mutabilityPtr, "mutabilityPtr");

    return callNativeFunction(
        "wasmtime4j_panama_global_metadata",
        Integer.class,
        globalPtr,
        valueTypePtr,
        mutabilityPtr,
        MemorySegment.NULL); // name_ptr - not needed for PanamaGlobal
  }

  /**
   * Destroys a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   */
  public void panamaGlobalDestroy(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    callNativeFunction("wasmtime4j_panama_global_destroy", Void.class, globalPtr);
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

  // Memory Functions

  /**
   * Gets memory size in pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param sizeOutPtr pointer to store the size in pages
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemorySizePages(
      final MemorySegment memoryPtr, final MemorySegment storePtr, final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_size_pages", Integer.class, memoryPtr, storePtr, sizeOutPtr);
  }

  /**
   * Grows memory by additional pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param additionalPages number of pages to grow
   * @param previousPagesOutPtr pointer to store the previous size in pages
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGrow(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final int additionalPages,
      final MemorySegment previousPagesOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(previousPagesOutPtr, "previousPagesOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_grow",
        Integer.class,
        memoryPtr,
        storePtr,
        additionalPages,
        previousPagesOutPtr);
  }

  /**
   * Grows memory by additional pages using 64-bit addressing (Panama FFI version).
   *
   * <p>This supports Memory64 proposal for memories larger than 4GB.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param additionalPages number of pages to grow (64-bit)
   * @param previousPagesOutPtr pointer to store the previous size in pages (64-bit)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGrow64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long additionalPages,
      final MemorySegment previousPagesOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(previousPagesOutPtr, "previousPagesOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_grow64",
        Integer.class,
        memoryPtr,
        storePtr,
        additionalPages,
        previousPagesOutPtr);
  }

  /**
   * Checks if memory uses 64-bit addressing (Memory64 proposal).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param is64BitOutPtr pointer to store the result (1 if 64-bit, 0 if 32-bit)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryIs64Bit(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment is64BitOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(is64BitOutPtr, "is64BitOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_is_64bit", Integer.class, memoryPtr, storePtr, is64BitOutPtr);
  }

  /**
   * Checks if memory is shared between threads (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param isSharedOutPtr pointer to store the result (1 if shared, 0 if not shared)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryIsShared(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment isSharedOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(isSharedOutPtr, "isSharedOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_is_shared", Integer.class, memoryPtr, storePtr, isSharedOutPtr);
  }

  /**
   * Gets memory type minimum pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param minimumOutPtr pointer to store the minimum pages (64-bit unsigned)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGetMinimum(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment minimumOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(minimumOutPtr, "minimumOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_get_minimum", Integer.class, memoryPtr, storePtr, minimumOutPtr);
  }

  /**
   * Gets memory type maximum pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param maximumOutPtr pointer to store the maximum pages (64-bit signed, -1 if unlimited)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGetMaximum(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment maximumOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(maximumOutPtr, "maximumOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_get_maximum", Integer.class, memoryPtr, storePtr, maximumOutPtr);
  }

  /**
   * Gets memory size in pages using 64-bit return value (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param sizeOutPtr pointer to store the size in pages (64-bit)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemorySizePages64(
      final MemorySegment memoryPtr, final MemorySegment storePtr, final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_size_pages64", Integer.class, memoryPtr, storePtr, sizeOutPtr);
  }

  /**
   * Reads bytes from memory (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset offset in memory to read from
   * @param length number of bytes to read
   * @param bufferPtr pointer to buffer to read into
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryReadBytes(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long length,
      final MemorySegment bufferPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferPtr, "bufferPtr");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhPanamaMemoryReadBytes;
    if (mh != null) {
      try {
        return (int) mh.invokeExact(memoryPtr, storePtr, offset, length, bufferPtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native panamaMemoryReadBytes failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_panama_memory_read_bytes",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        length,
        bufferPtr);
  }

  /**
   * Writes bytes to memory (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset offset in memory to write to
   * @param length number of bytes to write
   * @param bufferPtr pointer to buffer to write from
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryWriteBytes(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long length,
      final MemorySegment bufferPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferPtr, "bufferPtr");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhPanamaMemoryWriteBytes;
    if (mh != null) {
      try {
        return (int) mh.invokeExact(memoryPtr, storePtr, offset, length, bufferPtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native panamaMemoryWriteBytes failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_panama_memory_write_bytes",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        length,
        bufferPtr);
  }

  /**
   * Gets memory size in bytes (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param sizeOutPtr pointer to store the size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemorySizeBytes(
      final MemorySegment memoryPtr, final MemorySegment storePtr, final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_size_bytes", Integer.class, memoryPtr, storePtr, sizeOutPtr);
  }

  /**
   * Gets raw pointer to WASM linear memory for zero-copy access.
   *
   * <p>This provides direct access to the WebAssembly linear memory without copying, enabling
   * high-performance memory operations.
   *
   * <p><strong>WARNING:</strong> The returned pointer is only valid while:
   *
   * <ul>
   *   <li>The memory instance is alive
   *   <li>The store is alive
   *   <li>No memory.grow() operations are performed
   * </ul>
   *
   * <p>After memory.grow(), the pointer may be invalidated and must be re-obtained.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param dataPtrOutPtr pointer to store the raw memory data pointer
   * @param sizeOutPtr pointer to store the memory size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGetData(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment dataPtrOutPtr,
      final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(dataPtrOutPtr, "dataPtrOutPtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_get_data",
        Integer.class,
        memoryPtr,
        storePtr,
        dataPtrOutPtr,
        sizeOutPtr);
  }

  /**
   * Clears all memory and store handle registries (for testing purposes).
   *
   * <p>This function clears both memory and store handle registries to prevent stale handles from
   * interfering with subsequent tests. Should only be called in test teardown after all handles
   * have been properly destroyed.
   *
   * @return 0 on success, negative error code on failure
   */
  public int memoryClearHandleRegistries() {
    return callNativeFunction("wasmtime4j_panama_memory_clear_handle_registries", Integer.class);
  }

  // Error Handling Functions

  /**
   * Gets the last error message from the native library.
   *
   * @return pointer to error message string, or null if no error
   */
  public MemorySegment getLastErrorMessage() {
    return callNativeFunction("wasmtime4j_panama_get_last_error_message", MemorySegment.class);
  }

  /**
   * Frees an error message returned by getLastErrorMessage.
   *
   * @param messagePtr pointer to the error message to free
   */
  public void freeErrorMessage(final MemorySegment messagePtr) {
    if (messagePtr != null && !messagePtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_free_error_message", Void.class, messagePtr);
    }
  }

  /**
   * Frees a C string allocated by Rust.
   *
   * @param stringPtr pointer to the C string to free
   */
  public void freeString(final MemorySegment stringPtr) {
    if (stringPtr != null && !stringPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_free_string", Void.class, stringPtr);
    }
  }

  /** Clears any stored error state in the native library. */
  public void clearErrorState() {
    callNativeFunction("wasmtime4j_clear_error_state", Void.class);
  }

  // Serialization Functions

  /**
   * Creates a new module serializer.
   *
   * @return pointer to the created serializer, or null on failure
   */
  public MemorySegment serializerNew() {
    return callNativeFunction("wasmtime4j_serializer_new", MemorySegment.class);
  }

  /**
   * Creates a new module serializer with configuration.
   *
   * @param maxCacheSize maximum cache size in bytes
   * @param enableCompression whether to enable compression
   * @param compressionLevel compression level (0-9)
   * @return pointer to the created serializer, or null on failure
   */
  public MemorySegment serializerNewWithConfig(
      final long maxCacheSize, final boolean enableCompression, final int compressionLevel) {
    return callNativeFunction(
        "wasmtime4j_serializer_new_with_config",
        MemorySegment.class,
        maxCacheSize,
        enableCompression ? 1 : 0,
        compressionLevel);
  }

  /**
   * Destroys a module serializer.
   *
   * @param serializerPtr pointer to the serializer to destroy
   */
  public void serializerDestroy(final MemorySegment serializerPtr) {
    validatePointer(serializerPtr, "serializerPtr");
    callNativeFunction("wasmtime4j_serializer_destroy", Void.class, serializerPtr);
  }

  /**
   * Serializes module bytes.
   *
   * @param serializerPtr pointer to the serializer
   * @param enginePtr pointer to the engine
   * @param moduleBytes pointer to the module bytecode
   * @param moduleSize size of the module bytecode
   * @param resultBufferPtr pointer to store the result buffer
   * @param resultSizePtr pointer to store the result size
   * @return 0 on success, negative error code on failure
   */
  public int serializerSerialize(
      final MemorySegment serializerPtr,
      final MemorySegment enginePtr,
      final MemorySegment moduleBytes,
      final long moduleSize,
      final MemorySegment resultBufferPtr,
      final MemorySegment resultSizePtr) {
    validatePointer(serializerPtr, "serializerPtr");
    validatePointer(enginePtr, "enginePtr");
    validatePointer(moduleBytes, "moduleBytes");
    validatePointer(resultBufferPtr, "resultBufferPtr");
    validatePointer(resultSizePtr, "resultSizePtr");
    validateSize(moduleSize, "moduleSize");

    return callNativeFunction(
        "wasmtime4j_serializer_serialize",
        Integer.class,
        serializerPtr,
        enginePtr,
        moduleBytes,
        moduleSize,
        resultBufferPtr,
        resultSizePtr);
  }

  /**
   * Deserializes module bytes.
   *
   * @param serializerPtr pointer to the serializer
   * @param enginePtr pointer to the engine
   * @param serializedBytes pointer to the serialized bytecode
   * @param serializedSize size of the serialized bytecode
   * @param resultBufferPtr pointer to store the result buffer
   * @param resultSizePtr pointer to store the result size
   * @return 0 on success, negative error code on failure
   */
  public int serializerDeserialize(
      final MemorySegment serializerPtr,
      final MemorySegment enginePtr,
      final MemorySegment serializedBytes,
      final long serializedSize,
      final MemorySegment resultBufferPtr,
      final MemorySegment resultSizePtr) {
    validatePointer(serializerPtr, "serializerPtr");
    validatePointer(enginePtr, "enginePtr");
    validatePointer(serializedBytes, "serializedBytes");
    validatePointer(resultBufferPtr, "resultBufferPtr");
    validatePointer(resultSizePtr, "resultSizePtr");
    validateSize(serializedSize, "serializedSize");

    return callNativeFunction(
        "wasmtime4j_serializer_deserialize",
        Integer.class,
        serializerPtr,
        enginePtr,
        serializedBytes,
        serializedSize,
        resultBufferPtr,
        resultSizePtr);
  }

  /**
   * Clears the serializer cache.
   *
   * @param serializerPtr pointer to the serializer
   * @return 0 on success, negative error code on failure
   */
  public int serializerClearCache(final MemorySegment serializerPtr) {
    validatePointer(serializerPtr, "serializerPtr");
    return callNativeFunction("wasmtime4j_serializer_clear_cache", Integer.class, serializerPtr);
  }

  /**
   * Gets the cache entry count.
   *
   * @param serializerPtr pointer to the serializer
   * @return the number of entries in the cache
   */
  public long serializerCacheEntryCount(final MemorySegment serializerPtr) {
    validatePointer(serializerPtr, "serializerPtr");
    return callNativeFunction("wasmtime4j_serializer_cache_entry_count", Long.class, serializerPtr);
  }

  /**
   * Gets the cache total size in bytes.
   *
   * @param serializerPtr pointer to the serializer
   * @return the total cache size in bytes
   */
  public long serializerCacheTotalSize(final MemorySegment serializerPtr) {
    validatePointer(serializerPtr, "serializerPtr");
    return callNativeFunction("wasmtime4j_serializer_cache_total_size", Long.class, serializerPtr);
  }

  /**
   * Gets the cache hit rate.
   *
   * @param serializerPtr pointer to the serializer
   * @return the cache hit rate as a value between 0.0 and 1.0
   */
  public double serializerCacheHitRate(final MemorySegment serializerPtr) {
    validatePointer(serializerPtr, "serializerPtr");
    return callNativeFunction("wasmtime4j_serializer_cache_hit_rate", Double.class, serializerPtr);
  }

  /**
   * Frees a buffer allocated by serialization functions.
   *
   * @param buffer pointer to the buffer to free
   * @param size size of the buffer
   */
  public void serializerFreeBuffer(final MemorySegment buffer, final long size) {
    if (buffer != null && !buffer.equals(MemorySegment.NULL) && size > 0) {
      callNativeFunction("wasmtime4j_serializer_free_buffer", Void.class, buffer, size);
    }
  }

  // === Caller Context Functions ===

  /**
   * Gets the fuel consumed by the caller if fuel metering is enabled.
   *
   * @param callerPtr pointer to the caller context
   * @param fuelOut pointer to store the fuel value
   * @return 1 if fuel is available, 0 if fuel metering not enabled, negative error code on failure
   */
  public int callerGetFuel(final MemorySegment callerPtr, final MemorySegment fuelOut) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(fuelOut, "fuelOut");
    return callNativeFunction("wasmtime4j_caller_get_fuel", Integer.class, callerPtr, fuelOut);
  }

  /**
   * Gets the fuel remaining in the caller if fuel metering is enabled.
   *
   * @param callerPtr pointer to the caller context
   * @param fuelOut pointer to store the fuel value
   * @return 1 if fuel is available, 0 if fuel metering not enabled, negative error code on failure
   */
  public int callerGetFuelRemaining(final MemorySegment callerPtr, final MemorySegment fuelOut) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(fuelOut, "fuelOut");
    return callNativeFunction(
        "wasmtime4j_caller_get_fuel_remaining", Integer.class, callerPtr, fuelOut);
  }

  /**
   * Adds fuel to the caller.
   *
   * @param callerPtr pointer to the caller context
   * @param fuel amount of fuel to add
   * @return 0 on success, negative error code on failure
   */
  public int callerAddFuel(final MemorySegment callerPtr, final long fuel) {
    validatePointer(callerPtr, "callerPtr");
    return callNativeFunction("wasmtime4j_caller_add_fuel", Integer.class, callerPtr, fuel);
  }

  /**
   * Sets an epoch deadline for the caller.
   *
   * @param callerPtr pointer to the caller context
   * @param deadline the epoch deadline to set
   * @return 0 on success, negative error code on failure
   */
  public int callerSetEpochDeadline(final MemorySegment callerPtr, final long deadline) {
    validatePointer(callerPtr, "callerPtr");
    return callNativeFunction(
        "wasmtime4j_caller_set_epoch_deadline", Integer.class, callerPtr, deadline);
  }

  /**
   * Checks if the caller has an active epoch deadline.
   *
   * @param callerPtr pointer to the caller context
   * @return 1 if deadline is active, 0 if no deadline, negative error code on failure
   */
  public int callerHasEpochDeadline(final MemorySegment callerPtr) {
    validatePointer(callerPtr, "callerPtr");
    return callNativeFunction("wasmtime4j_caller_has_epoch_deadline", Integer.class, callerPtr);
  }

  /**
   * Checks if caller has an export with the given name.
   *
   * @param callerPtr pointer to the caller context
   * @param name name of the export to check
   * @return 1 if export exists, 0 if not found, negative error code on failure
   */
  public int callerHasExport(final MemorySegment callerPtr, final MemorySegment name) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(name, "name");
    return callNativeFunction("wasmtime4j_caller_has_export", Integer.class, callerPtr, name);
  }

  /**
   * Gets memory export from caller by name.
   *
   * @param callerPtr pointer to the caller context
   * @param name name of the memory export
   * @param memoryOut pointer to store the memory pointer
   * @return 1 if memory found, 0 if not found, negative error code on failure
   */
  public int callerGetMemory(
      final MemorySegment callerPtr, final MemorySegment name, final MemorySegment memoryOut) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(name, "name");
    validatePointer(memoryOut, "memoryOut");
    return callNativeFunction(
        "wasmtime4j_caller_get_memory", Integer.class, callerPtr, name, memoryOut);
  }

  /**
   * Gets function export from caller by name.
   *
   * @param callerPtr pointer to the caller context
   * @param name name of the function export
   * @param functionOut pointer to store the function pointer
   * @return 1 if function found, 0 if not found, negative error code on failure
   */
  public int callerGetFunction(
      final MemorySegment callerPtr, final MemorySegment name, final MemorySegment functionOut) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(name, "name");
    validatePointer(functionOut, "functionOut");
    return callNativeFunction(
        "wasmtime4j_caller_get_function", Integer.class, callerPtr, name, functionOut);
  }

  /**
   * Gets global export from caller by name.
   *
   * @param callerPtr pointer to the caller context
   * @param name name of the global export
   * @param globalOut pointer to store the global pointer
   * @return 1 if global found, 0 if not found, negative error code on failure
   */
  public int callerGetGlobal(
      final MemorySegment callerPtr, final MemorySegment name, final MemorySegment globalOut) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(name, "name");
    validatePointer(globalOut, "globalOut");
    return callNativeFunction(
        "wasmtime4j_caller_get_global", Integer.class, callerPtr, name, globalOut);
  }

  /**
   * Gets table export from caller by name.
   *
   * @param callerPtr pointer to the caller context
   * @param name name of the table export
   * @param tableOut pointer to store the table pointer
   * @return 1 if table found, 0 if not found, negative error code on failure
   */
  public int callerGetTable(
      final MemorySegment callerPtr, final MemorySegment name, final MemorySegment tableOut) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(name, "name");
    validatePointer(tableOut, "tableOut");
    return callNativeFunction(
        "wasmtime4j_caller_get_table", Integer.class, callerPtr, name, tableOut);
  }

  // =============================================================================
  // Panama Function FFI Operations
  // =============================================================================

  /**
   * Calls a WebAssembly function directly using a function handle.
   *
   * @param funcPtr pointer to the function
   * @param storePtr pointer to the store
   * @param paramsPtr pointer to parameters array (WasmValue format)
   * @param paramCount number of parameters
   * @param resultsPtr pointer to results buffer (WasmValue format)
   * @param resultCount maximum number of results
   * @return 0 on success, negative error code on failure
   */
  public int funcCall(
      final MemorySegment funcPtr,
      final MemorySegment storePtr,
      final MemorySegment paramsPtr,
      final long paramCount,
      final MemorySegment resultsPtr,
      final long resultCount) {
    validatePointer(funcPtr, "funcPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_func_call",
        Integer.class,
        funcPtr,
        storePtr,
        paramsPtr,
        paramCount,
        resultsPtr,
        resultCount);
  }

  /**
   * Gets the function type for a function handle.
   *
   * @param funcPtr pointer to the function
   * @param storePtr pointer to the store
   * @return pointer to the function type, or null on failure
   */
  public MemorySegment funcGetType(final MemorySegment funcPtr, final MemorySegment storePtr) {
    validatePointer(funcPtr, "funcPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_func_type", MemorySegment.class, funcPtr, storePtr);
  }

  /**
   * Destroys a function handle.
   *
   * @param funcPtr pointer to the function to destroy
   */
  public void funcDestroy(final MemorySegment funcPtr) {
    if (funcPtr != null && !funcPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_func_destroy", Void.class, funcPtr);
    }
  }

  /**
   * Destroys a function type handle.
   *
   * @param funcTypePtr pointer to the function type to destroy
   */
  public void funcTypeDestroy(final MemorySegment funcTypePtr) {
    if (funcTypePtr != null && !funcTypePtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_func_type_destroy", Void.class, funcTypePtr);
    }
  }

  /** Initializes all function bindings. */
  private void initializeFunctionBindings() {
    // Error handling functions
    addFunctionBinding(
        "wasmtime4j_panama_get_last_error_message", FunctionDescriptor.of(ValueLayout.ADDRESS));
    addFunctionBinding(
        "wasmtime4j_panama_free_error_message",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // message_ptr

    // Engine functions
    addFunctionBinding("wasmtime4j_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    // Engine creation with extended configuration
    addFunctionBinding(
        "wasmtime4j_panama_engine_create_with_extended_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return engine_ptr
            ValueLayout.JAVA_INT, // strategy
            ValueLayout.JAVA_INT, // opt_level
            ValueLayout.JAVA_INT, // debug_info
            ValueLayout.JAVA_INT, // wasm_threads
            ValueLayout.JAVA_INT, // wasm_simd
            ValueLayout.JAVA_INT, // wasm_reference_types
            ValueLayout.JAVA_INT, // wasm_bulk_memory
            ValueLayout.JAVA_INT, // wasm_multi_value
            ValueLayout.JAVA_INT, // fuel_enabled
            ValueLayout.JAVA_INT, // max_memory_pages
            ValueLayout.JAVA_INT, // max_stack_size
            ValueLayout.JAVA_INT, // epoch_interruption
            ValueLayout.JAVA_INT, // max_instances
            ValueLayout.JAVA_INT, // async_support
            ValueLayout.JAVA_INT, // wasm_gc
            ValueLayout.JAVA_INT, // wasm_function_references
            ValueLayout.JAVA_INT, // wasm_exceptions
            ValueLayout.JAVA_LONG, // memory_reservation
            ValueLayout.JAVA_LONG, // memory_guard_size
            ValueLayout.JAVA_LONG, // memory_reservation_for_growth
            ValueLayout.JAVA_INT, // wasm_tail_call
            ValueLayout.JAVA_INT, // wasm_relaxed_simd
            ValueLayout.JAVA_INT, // wasm_multi_memory
            ValueLayout.JAVA_INT, // wasm_memory64
            ValueLayout.JAVA_INT, // wasm_extended_const
            ValueLayout.JAVA_INT, // wasm_component_model
            ValueLayout.JAVA_INT, // coredump_on_trap
            ValueLayout.JAVA_INT, // cranelift_nan_canonicalization
            ValueLayout.JAVA_INT, // wasm_custom_page_sizes
            ValueLayout.JAVA_INT)); // wasm_wide_arithmetic

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
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_set_debug_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_BOOLEAN)); // enabled

    addFunctionBinding(
        "wasmtime4j_engine_is_debug_info",
        FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_validate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG)); // wasm_size

    addFunctionBinding(
        "wasmtime4j_engine_supports_feature",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_INT)); // feature_id

    addFunctionBinding(
        "wasmtime4j_engine_memory_limit_pages",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_stack_size_limit",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_fuel_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_epoch_interruption_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_fuel_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_epoch_interruption_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_coredump_on_trap_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_get_memory_limit",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_get_stack_limit",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_pulley",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_supports_feature",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=supported, 0=not supported, -1=error
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // feature_name (C string)

    addFunctionBinding(
        "wasmtime4j_engine_max_instances",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_reference_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_increment_epoch",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // engine_ptr

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

    addFunctionBinding(
        "wasmtime4j_module_export_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return export count
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_module_get_export_names",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return actual count
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // names_out (array of char*)
            ValueLayout.JAVA_LONG)); // max_count

    addFunctionBinding(
        "wasmtime4j_module_get_export_kind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return kind (0=not found, 1=function, etc)
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS)); // name (C string)

    // Panama FFI bindings: return module pointer directly
    addFunctionBinding(
        "wasmtime4j_module_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return module*
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG)); // wasm_size

    addFunctionBinding(
        "wasmtime4j_module_create_wat",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return module*
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // wat_text (null-terminated string)

    // Panama FFI module compile from WAT with output parameter
    addFunctionBinding(
        "wasmtime4j_panama_module_compile_wat",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wat_text (null-terminated string)
            ValueLayout.ADDRESS)); // module_ptr (output)

    // Module introspection functions - using existing Panama FFI functions
    addFunctionBinding(
        "wasmtime4j_panama_module_get_import_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return import count
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_get_export_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return export count
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_get_imports_json",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return JSON string pointer
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_get_exports_json",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return JSON string pointer
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_free_string",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // string_ptr

    addFunctionBinding(
        "wasmtime4j_module_export_nth",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return found
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS, // name_out_ptr
            ValueLayout.ADDRESS)); // type_out_ptr

    addFunctionBinding(
        "wasmtime4j_module_get_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return name string pointer
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_module_validate_imports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // imports_ptr
            ValueLayout.JAVA_LONG)); // imports_count

    // Module serialization functions
    addFunctionBinding(
        "wasmtime4j_panama_module_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // data_ptr_ptr (output)
            ValueLayout.ADDRESS)); // len_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_deserialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG, // len
            ValueLayout.ADDRESS)); // module_ptr_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_free_serialized_data",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG)); // len

    // Store functions
    // Panama FFI binding: returns store pointer directly
    addFunctionBinding(
        "wasmtime4j_store_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return store*
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding("wasmtime4j_store_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_store_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_LONG, // fuel_limit
            ValueLayout.JAVA_LONG, // memory_limit_bytes
            ValueLayout.JAVA_LONG, // execution_timeout_secs
            ValueLayout.JAVA_INT, // max_instances
            ValueLayout.JAVA_INT, // max_table_elements
            ValueLayout.JAVA_INT, // max_functions
            ValueLayout.ADDRESS)); // store_ptr (output)

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

    // Panama-prefixed store fuel functions
    addFunctionBinding(
        "wasmtime4j_panama_store_set_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_get_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // fuel_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_add_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_consume_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // fuel
            ValueLayout.ADDRESS)); // remaining_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_get_fuel_remaining",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // remaining_out_ptr

    // Panama-prefixed store epoch functions
    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // ticks

    addFunctionBinding(
        "wasmtime4j_panama_store_epoch_deadline_trap",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline_callback",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_epoch_deadline_callback",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    // Epoch deadline callback with function pointer (Panama FFI)
    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline_callback_fn",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG)); // callback_id

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

    addFunctionBinding(
        "wasmtime4j_store_set_wasi_context",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // wasi_ctx_ptr

    addFunctionBinding(
        "wasmtime4j_store_has_wasi_context",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    // Panama-prefixed epoch deadline functions
    addFunctionBinding(
        "wasmtime4j_panama_store_epoch_deadline_async_yield_and_update",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // delta ticks

    addFunctionBinding(
        "wasmtime4j_panama_store_epoch_deadline_trap",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline_callback",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_epoch_deadline_callback",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    // Instance functions - Panama FFI: return instance pointer directly
    addFunctionBinding(
        "wasmtime4j_instance_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance*
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // module_ptr

    // Panama FFI instance creation with output parameter
    addFunctionBinding(
        "wasmtime4j_panama_instance_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS)); // instance_ptr (output)

    addFunctionBinding(
        "wasmtime4j_instance_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_call_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return result count
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // function_name (C string)
            ValueLayout.ADDRESS, // params_ptr (WasmValue array)
            ValueLayout.JAVA_LONG, // param_count
            ValueLayout.ADDRESS, // results_ptr (WasmValue array)
            ValueLayout.JAVA_LONG)); // max_results

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

    // Instance export retrieval functions
    addFunctionBinding(
        "wasmtime4j_instance_get_memory_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return memory* or null
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // name (C string)

    // Instance memory operations with fresh lookup
    addFunctionBinding(
        "wasmtime4j_instance_has_memory_export",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // name (C string)

    addFunctionBinding(
        "wasmtime4j_instance_get_memory_size_pages",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_instance_get_memory_size_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_instance_grow_memory",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.JAVA_INT, // pages
            ValueLayout.ADDRESS)); // previous_pages_out

    addFunctionBinding(
        "wasmtime4j_instance_read_memory_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // buffer

    addFunctionBinding(
        "wasmtime4j_instance_write_memory_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // buffer

    addFunctionBinding(
        "wasmtime4j_instance_get_global_type",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS, // value_type_out
            ValueLayout.ADDRESS)); // is_mutable_out

    addFunctionBinding(
        "wasmtime4j_instance_has_global_export",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // name (C string)

    addFunctionBinding(
        "wasmtime4j_instance_get_global_value",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS, // i32_out
            ValueLayout.ADDRESS, // i64_out
            ValueLayout.ADDRESS, // f32_out
            ValueLayout.ADDRESS, // f64_out
            ValueLayout.ADDRESS, // ref_id_present_out
            ValueLayout.ADDRESS)); // ref_id_out

    addFunctionBinding(
        "wasmtime4j_instance_set_global_value",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, error code on failure
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.JAVA_INT, // value_type_code
            ValueLayout.JAVA_INT, // i32_value
            ValueLayout.JAVA_LONG, // i64_value
            ValueLayout.JAVA_DOUBLE, // f32_value
            ValueLayout.JAVA_DOUBLE, // f64_value
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_instance_get_table_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return table* or null
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // name (C string)

    addFunctionBinding(
        "wasmtime4j_instance_get_global_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return global* or null
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // name (C string)

    // Wrapped global for linker use
    addFunctionBinding(
        "wasmtime4j_panama_instance_get_global_wrapped",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return wrapped Global* or null
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // name (C string)

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

    // Panama FFI table functions
    addFunctionBinding(
        "wasmtime4j_panama_table_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // initial_size
            ValueLayout.JAVA_INT, // has_maximum
            ValueLayout.JAVA_INT, // maximum_size
            ValueLayout.ADDRESS, // name_ptr
            ValueLayout.ADDRESS)); // table_ptr out param

    addFunctionBinding(
        "wasmtime4j_panama_table_create64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_LONG, // initial_size (64-bit)
            ValueLayout.JAVA_INT, // has_maximum
            ValueLayout.JAVA_LONG, // maximum_size (64-bit)
            ValueLayout.ADDRESS, // name_ptr
            ValueLayout.ADDRESS)); // table_ptr out param

    addFunctionBinding(
        "wasmtime4j_panama_table_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size out param

    addFunctionBinding(
        "wasmtime4j_panama_table_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.ADDRESS, // ref_id_present out param
            ValueLayout.ADDRESS)); // ref_id out param

    addFunctionBinding(
        "wasmtime4j_panama_table_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_panama_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // delta
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG, // ref_id
            ValueLayout.ADDRESS)); // old_size out param

    addFunctionBinding(
        "wasmtime4j_panama_table_fill",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.JAVA_INT, // len
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_panama_table_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // element_type out param
            ValueLayout.ADDRESS, // initial_size out param (64-bit)
            ValueLayout.ADDRESS, // has_maximum out param
            ValueLayout.ADDRESS, // maximum_size out param (64-bit)
            ValueLayout.ADDRESS, // is_64 out param
            ValueLayout.ADDRESS)); // name_ptr out param

    addFunctionBinding(
        "wasmtime4j_panama_table_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_panama_table_is_64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return value (1=64-bit, 0=32-bit, -1=error)
            ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_panama_table_init",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.JAVA_INT, // src
            ValueLayout.JAVA_INT, // len
            ValueLayout.JAVA_INT)); // segment_index

    addFunctionBinding(
        "wasmtime4j_panama_table_copy",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.JAVA_INT, // src
            ValueLayout.JAVA_INT)); // len

    addFunctionBinding(
        "wasmtime4j_panama_table_copy_from",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // dst_table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.ADDRESS, // src_table_ptr
            ValueLayout.JAVA_INT, // src
            ValueLayout.JAVA_INT)); // len

    addFunctionBinding(
        "wasmtime4j_panama_elem_drop",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.JAVA_INT)); // segment_index

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

    // Panama FFI global get/set functions
    addFunctionBinding(
        "wasmtime4j_panama_global_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // global_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // i32_value out
            ValueLayout.ADDRESS, // i64_value out
            ValueLayout.ADDRESS, // f32_value out
            ValueLayout.ADDRESS, // f64_value out
            ValueLayout.ADDRESS, // ref_id_present out
            ValueLayout.ADDRESS, // ref_id out
            ValueLayout.ADDRESS)); // v128_bytes out (16 bytes)

    addFunctionBinding(
        "wasmtime4j_panama_global_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // global_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.JAVA_INT, // i32_value
            ValueLayout.JAVA_LONG, // i64_value
            ValueLayout.JAVA_DOUBLE, // f32_value
            ValueLayout.JAVA_DOUBLE, // f64_value
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG, // ref_id
            ValueLayout.ADDRESS)); // v128_bytes ptr (16 bytes, can be null)

    addFunctionBinding(
        "wasmtime4j_global_get_type_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS, // type_info (out)
            ValueLayout.ADDRESS)); // mutability (out)

    // Panama FFI global create/destroy/metadata functions
    addFunctionBinding(
        "wasmtime4j_panama_global_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.JAVA_INT, // mutability
            ValueLayout.JAVA_INT, // i32_value
            ValueLayout.JAVA_LONG, // i64_value
            ValueLayout.JAVA_DOUBLE, // f32_value
            ValueLayout.JAVA_DOUBLE, // f64_value
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG, // ref_id
            ValueLayout.ADDRESS, // v128_bytes ptr (16 bytes, can be null)
            ValueLayout.ADDRESS, // name_ptr (optional)
            ValueLayout.ADDRESS)); // global_ptr (out)

    addFunctionBinding(
        "wasmtime4j_panama_global_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // global_ptr
            ValueLayout.ADDRESS, // value_type (out)
            ValueLayout.ADDRESS, // mutability (out)
            ValueLayout.ADDRESS)); // name_ptr (out)

    addFunctionBinding(
        "wasmtime4j_panama_global_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // global_ptr

    // Panama FFI memory functions
    addFunctionBinding(
        "wasmtime4j_panama_memory_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // initial_pages
            ValueLayout.JAVA_INT, // maximum_pages
            ValueLayout.JAVA_INT, // is_shared
            ValueLayout.JAVA_INT, // memory_index
            ValueLayout.ADDRESS, // name
            ValueLayout.ADDRESS)); // memory_ptr_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_size_pages",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // additional_pages
            ValueLayout.ADDRESS)); // previous_pages_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_read_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // buffer

    addFunctionBinding(
        "wasmtime4j_panama_memory_write_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // buffer

    addFunctionBinding(
        "wasmtime4j_panama_memory_size_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_get_data",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // data_ptr_out
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_is_64bit",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // is_64bit_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_is_shared",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // is_shared_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_size_pages64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size_out (u64)

    addFunctionBinding(
        "wasmtime4j_panama_memory_grow64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // additional_pages
            ValueLayout.ADDRESS)); // previous_pages_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_get_minimum",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // minimum_out (u64)

    addFunctionBinding(
        "wasmtime4j_panama_memory_get_maximum",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // maximum_out (i64, -1 if unlimited)

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
        "wasmtime4j_free_string", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // string_ptr

    addFunctionBinding(
        "wasmtime4j_clear_error_state", FunctionDescriptor.ofVoid()); // no parameters

    // Serialization functions - from Task #288
    addFunctionBinding(
        "wasmtime4j_serializer_new",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns serializer*

    addFunctionBinding(
        "wasmtime4j_serializer_new_with_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns serializer*
            ValueLayout.JAVA_LONG, // max_cache_size
            ValueLayout.JAVA_INT, // enable_compression
            ValueLayout.JAVA_INT)); // compression_level

    addFunctionBinding(
        "wasmtime4j_serializer_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // serializer_ptr

    addFunctionBinding(
        "wasmtime4j_serializer_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // serializer_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // module_bytes
            ValueLayout.JAVA_LONG, // module_size
            ValueLayout.ADDRESS, // result_buffer (out)
            ValueLayout.ADDRESS)); // result_size (out)

    addFunctionBinding(
        "wasmtime4j_serializer_deserialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // serializer_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // serialized_bytes
            ValueLayout.JAVA_LONG, // serialized_size
            ValueLayout.ADDRESS, // result_buffer (out)
            ValueLayout.ADDRESS)); // result_size (out)

    addFunctionBinding(
        "wasmtime4j_serializer_clear_cache",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS)); // serializer_ptr

    addFunctionBinding(
        "wasmtime4j_serializer_cache_entry_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return count
            ValueLayout.ADDRESS)); // serializer_ptr

    addFunctionBinding(
        "wasmtime4j_serializer_cache_total_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size in bytes
            ValueLayout.ADDRESS)); // serializer_ptr

    addFunctionBinding(
        "wasmtime4j_serializer_cache_hit_rate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_DOUBLE, // return hit rate 0.0-1.0
            ValueLayout.ADDRESS)); // serializer_ptr

    addFunctionBinding(
        "wasmtime4j_serializer_free_buffer",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // buffer
            ValueLayout.JAVA_LONG)); // size

    // Additional Engine functions from Task #288
    addFunctionBinding(
        "wasmtime4j_engine_new", FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns engine*

    addFunctionBinding(
        "wasmtime4j_engine_new_with_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns engine*
            ValueLayout.JAVA_INT, // debug_info
            ValueLayout.JAVA_INT, // wasm_threads
            ValueLayout.JAVA_INT, // wasm_simd
            ValueLayout.JAVA_INT, // wasm_reference_types
            ValueLayout.JAVA_INT, // wasm_bulk_memory
            ValueLayout.JAVA_INT, // wasm_multi_value
            ValueLayout.JAVA_INT, // fuel_enabled
            ValueLayout.JAVA_INT, // max_memory_pages
            ValueLayout.JAVA_LONG, // max_stack_size
            ValueLayout.JAVA_INT, // epoch_interruption
            ValueLayout.JAVA_INT)); // max_instances

    addFunctionBinding(
        "wasmtime4j_engine_validate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_supports_feature",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return boolean as int
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_INT)); // feature

    addFunctionBinding(
        "wasmtime4j_engine_memory_limit_pages",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return pages
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_stack_size_limit",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return stack size
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_fuel_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return boolean as int
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_epoch_interruption_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return boolean as int
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_max_instances",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return max instances
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_reference_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return reference count
            ValueLayout.ADDRESS)); // engine_ptr

    // Panama Linker functions
    addFunctionBinding(
        "wasmtime4j_panama_linker_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return linker*
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_host_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // module_name (C string)
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS, // param_types (int array)
            ValueLayout.JAVA_INT, // param_count
            ValueLayout.ADDRESS, // return_types (int array)
            ValueLayout.JAVA_INT, // return_count
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_global",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_name (C string)
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS)); // global_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_memory",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_name (C string)
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS)); // memory_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_memory_from_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_name (C string)
            ValueLayout.ADDRESS, // memory_name (C string)
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS)); // export_name (C string)

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_table",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_name (C string)
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_name (C string)
            ValueLayout.ADDRESS)); // instance_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_alias",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // from_module_name (C string)
            ValueLayout.ADDRESS, // from_name (C string)
            ValueLayout.ADDRESS, // to_module_name (C string)
            ValueLayout.ADDRESS)); // to_name (C string)

    addFunctionBinding(
        "wasmtime4j_linker_instantiate",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance* or null
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // linker_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_allow_shadowing",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.JAVA_INT)); // allow (1=yes, 0=no)

    addFunctionBinding(
        "wasmtime4j_panama_linker_allow_unknown_exports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.JAVA_INT)); // allow (1=yes, 0=no)

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_unknown_imports_as_traps",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_unknown_imports_as_default_values",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // module_ptr

    // InstancePre operations
    addFunctionBinding(
        "wasmtime4j_linker_instantiate_pre",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance_pre* or null
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_instance_pre_instantiate",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance* or null
            ValueLayout.ADDRESS, // instance_pre_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_instance_pre_is_valid",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 1 if valid, 0 otherwise
            ValueLayout.ADDRESS)); // instance_pre_ptr

    addFunctionBinding(
        "wasmtime4j_instance_pre_instance_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return instance count
            ValueLayout.ADDRESS)); // instance_pre_ptr

    addFunctionBinding(
        "wasmtime4j_instance_pre_preparation_time_ns",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return preparation time in ns
            ValueLayout.ADDRESS)); // instance_pre_ptr

    addFunctionBinding(
        "wasmtime4j_instance_pre_avg_instantiation_time_ns",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return avg instantiation time in ns
            ValueLayout.ADDRESS)); // instance_pre_ptr

    addFunctionBinding(
        "wasmtime4j_instance_pre_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // instance_pre_ptr

    // SIMD operations
    addFunctionBinding(
        "wasmtime4j_panama_simd_add",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return i32 status code
            ValueLayout.JAVA_LONG, // runtimeHandle
            ValueLayout.ADDRESS, // vectorA (input)
            ValueLayout.ADDRESS, // vectorB (input)
            ValueLayout.ADDRESS)); // result (output)

    addFunctionBinding(
        "wasmtime4j_panama_simd_subtract",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_multiply",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_divide",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_add_saturated",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_and",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_or",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_xor",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_not",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_sqrt",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_reciprocal",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_rsqrt",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_fma",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_fms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_extract_lane_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return int
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT)); // lane index

    addFunctionBinding(
        "wasmtime4j_panama_simd_replace_lane_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, // lane index
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result

    addFunctionBinding(
        "wasmtime4j_panama_simd_convert_i32_to_f32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_convert_f32_to_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_horizontal_sum_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return int
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_horizontal_min_i32",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_horizontal_max_i32",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_relaxed_add",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_shuffle",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS, // shuffle mask
            ValueLayout.ADDRESS)); // result

    addFunctionBinding(
        "wasmtime4j_panama_simd_equals",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_less_than",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_greater_than",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_load",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_load_aligned",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_store",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_store_aligned",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_popcount",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_shl_variable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_shr_variable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_select",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_blend",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    // Component Model functions
    addFunctionBinding(
        "wasmtime4j_component_engine_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)); // config

    addFunctionBinding(
        "wasmtime4j_component_engine_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return component handle
            ValueLayout.ADDRESS, // engineHandle
            ValueLayout.ADDRESS, // bytes
            ValueLayout.JAVA_LONG)); // length

    addFunctionBinding(
        "wasmtime4j_component_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code (0 = success)
            ValueLayout.ADDRESS, // engineHandle
            ValueLayout.ADDRESS, // componentHandle
            ValueLayout.ADDRESS)); // instanceOut (pointer to pointer)

    addFunctionBinding(
        "wasmtime4j_component_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_instance_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_get_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_exports_interface",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_imports_interface",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_export_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_import_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_has_export",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_has_import",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_validate",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_get_export_name",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_get_import_name",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_free_string", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_get_exported_functions",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // instance handle
            ValueLayout.ADDRESS, // functions out (pointer to pointer array)
            ValueLayout.ADDRESS)); // count out

    addFunctionBinding(
        "wasmtime4j_panama_component_invoke",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // instance handle
            ValueLayout.ADDRESS, // function name
            ValueLayout.ADDRESS, // params pointer
            ValueLayout.JAVA_INT, // params count
            ValueLayout.ADDRESS, // results out
            ValueLayout.ADDRESS)); // results count out

    addFunctionBinding(
        "wasmtime4j_panama_component_free_string_array",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // strings pointer
            ValueLayout.JAVA_INT)); // count

    // Enhanced component engine functions (using instance IDs)
    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_engine_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns engine pointer

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // component handle
            ValueLayout.ADDRESS)); // instance ID out (pointer to u64)

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_invoke",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance ID
            ValueLayout.ADDRESS, // function name
            ValueLayout.ADDRESS, // params pointer
            ValueLayout.JAVA_INT, // params count
            ValueLayout.ADDRESS, // results out
            ValueLayout.ADDRESS)); // results count out

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_get_exports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance ID
            ValueLayout.ADDRESS, // functions out (pointer to pointer array)
            ValueLayout.ADDRESS)); // count out

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_engine_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // engine handle

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // wasm bytes
            ValueLayout.JAVA_LONG, // wasm size
            ValueLayout.ADDRESS)); // component out (pointer to pointer)

    // WIT value marshalling functions
    addFunctionBinding(
        "wasmtime4j_wit_value_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.JAVA_INT, // type_discriminator
            ValueLayout.ADDRESS, // value_ptr
            ValueLayout.JAVA_LONG, // value_len
            ValueLayout.ADDRESS, // out_data
            ValueLayout.ADDRESS)); // out_len

    addFunctionBinding(
        "wasmtime4j_wit_value_free_buffer",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // buffer_ptr
            ValueLayout.JAVA_LONG)); // buffer_len

    addFunctionBinding(
        "wasmtime4j_wit_value_validate_discriminator",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 1 if valid, 0 if invalid
            ValueLayout.JAVA_INT)); // type_discriminator

    addFunctionBinding(
        "wasmtime4j_wit_value_deserialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.JAVA_INT, // type_discriminator
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG, // data_len
            ValueLayout.ADDRESS, // out_value
            ValueLayout.ADDRESS)); // out_len

    // Component Linker functions
    addFunctionBinding(
        "wasmtime4j_component_linker_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns linker pointer
            ValueLayout.ADDRESS)); // engine pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_new_with_engine",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns linker pointer
            ValueLayout.ADDRESS)); // wasmtime engine pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_is_valid",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if valid, 0 if invalid
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_dispose",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_has_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if present, 0 if not, -1 on error
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // namespace string
            ValueLayout.ADDRESS)); // interface name string

    addFunctionBinding(
        "wasmtime4j_component_linker_has_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if present, 0 if not, -1 on error
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // namespace string
            ValueLayout.ADDRESS, // interface name string
            ValueLayout.ADDRESS)); // function name string

    addFunctionBinding(
        "wasmtime4j_component_linker_host_function_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns count
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_interface_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns count
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_wasi_p2_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if enabled, 0 if not
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_enable_wasi_p2",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_args",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS)); // args JSON string

    addFunctionBinding(
        "wasmtime4j_component_linker_add_wasi_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // key string
            ValueLayout.ADDRESS)); // value string

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_stdio",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_add_wasi_preopen_dir",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // host path string
            ValueLayout.ADDRESS, // guest path string
            ValueLayout.JAVA_INT)); // read only flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_network",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_clock",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_random",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    // WASI HTTP functions
    addFunctionBinding(
        "wasmtime4j_component_linker_enable_wasi_http",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_wasi_http_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if enabled, 0 if not, negative on error
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // component pointer
            ValueLayout.ADDRESS)); // instance out pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_get_interfaces",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS)); // json out pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_get_functions",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // namespace string
            ValueLayout.ADDRESS, // interface name string
            ValueLayout.ADDRESS)); // json out pointer

    // WASI context functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_argv",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_inherit_env",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_inherit_stdio",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stdin",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stdin_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stdout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stderr",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_preopen_dir",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_preopen_dir_readonly",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_preopen_dir_with_perms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    // WASI Output Capture Functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_enable_output_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_get_stdout_capture",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return buffer pointer
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS)); // data_len_out

    addFunctionBinding(
        "wasmtime4j_wasi_context_get_stderr_capture",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return buffer pointer
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS)); // data_len_out

    addFunctionBinding(
        "wasmtime4j_wasi_free_capture_buffer", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_has_stdout_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_has_stderr_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // WASI Linker Integration
    addFunctionBinding(
        "wasmtime4j_linker_add_wasi",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // WASI Preview 2 Clock Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_now",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // out_time pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_resolution",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // out_resolution pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_instant",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // when (nanoseconds)
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_duration",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // duration (nanoseconds)
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_wall_clock_now",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // out_seconds pointer (u64)
            ValueLayout.ADDRESS)); // out_nanoseconds pointer (u32)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_wall_clock_resolution",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // out_seconds pointer (u64)
            ValueLayout.ADDRESS)); // out_nanoseconds pointer (u32)

    // WASI Preview 2 Random Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_random_get_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // buffer pointer
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // out_actual_length pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_random_get_u64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // out_value pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_random_free_buffer",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // buffer

    // WASI Preview 2 TCP Socket Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_INT, // is_ipv6 (boolean as int)
            ValueLayout.ADDRESS)); // out_handle pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_start_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // address (C string)
            ValueLayout.JAVA_INT)); // port

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_finish_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_start_connect",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // address (C string)
            ValueLayout.JAVA_INT)); // port

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_finish_connect",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_input_stream pointer (u64)
            ValueLayout.ADDRESS)); // out_output_stream pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_start_listen",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_finish_listen",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_accept",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_client_socket pointer (u64)
            ValueLayout.ADDRESS, // out_input_stream pointer (u64)
            ValueLayout.ADDRESS)); // out_output_stream pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_local_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_remote_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_address_family",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_family pointer (u8: 0=IPv4, 1=IPv6)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_listen_backlog_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // backlog_size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // enabled (boolean as int)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_idle_time",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // idle_time (nanoseconds)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_interval",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // interval (nanoseconds)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // count

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_hop_limit",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // hop_limit

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_subscribe",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_shutdown",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // shutdown_type (0=receive, 1=send, 2=both)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_close",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    // WASI Preview 2 UDP Socket Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_INT, // is_ipv6 (boolean as int)
            ValueLayout.ADDRESS)); // out_handle pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_start_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // address (C string)
            ValueLayout.JAVA_INT)); // port

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_finish_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_stream",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // remote_address (C string, nullable)
            ValueLayout.JAVA_INT)); // remote_port (-1 for no remote)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_local_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_remote_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_address_family",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_family pointer (u8: 0=IPv4, 1=IPv6)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_set_unicast_hop_limit",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // hop_limit

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_set_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_set_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_subscribe",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_receive",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG, // max_datagrams
            ValueLayout.ADDRESS, // out_datagrams buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_count pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_send",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // datagrams buffer
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // out_sent_count pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_close",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    // Thread-Local Storage (TLS) Functions
    addFunctionBinding(
        "wasmtime4j_thread_put_int",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.JAVA_INT)); // value

    addFunctionBinding(
        "wasmtime4j_thread_get_int",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.ADDRESS)); // out_value pointer

    addFunctionBinding(
        "wasmtime4j_thread_contains_key",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.ADDRESS)); // out_exists pointer

    addFunctionBinding(
        "wasmtime4j_thread_remove_key",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS)); // key_ptr (C string)

    addFunctionBinding(
        "wasmtime4j_thread_clear_storage",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // thread_ptr

    addFunctionBinding(
        "wasmtime4j_thread_storage_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS)); // out_size pointer

    addFunctionBinding(
        "wasmtime4j_thread_storage_memory_usage",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS)); // out_memory_usage pointer (i64)

    // Additional TLS data types
    addFunctionBinding(
        "wasmtime4j_thread_put_long",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.JAVA_LONG)); // value

    addFunctionBinding(
        "wasmtime4j_thread_get_long",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.ADDRESS)); // out_value pointer

    addFunctionBinding(
        "wasmtime4j_thread_put_float",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.JAVA_FLOAT)); // value

    addFunctionBinding(
        "wasmtime4j_thread_get_float",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.ADDRESS)); // out_value pointer

    addFunctionBinding(
        "wasmtime4j_thread_put_double",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.JAVA_DOUBLE)); // value

    addFunctionBinding(
        "wasmtime4j_thread_get_double",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.ADDRESS)); // out_value pointer

    addFunctionBinding(
        "wasmtime4j_thread_put_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.ADDRESS, // bytes_ptr
            ValueLayout.JAVA_LONG)); // bytes_len

    addFunctionBinding(
        "wasmtime4j_thread_get_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_ptr
            ValueLayout.ADDRESS, // key_ptr (C string)
            ValueLayout.ADDRESS, // out_bytes_ptr
            ValueLayout.ADDRESS)); // out_bytes_len

    // Thread Lifecycle Functions
    addFunctionBinding(
        "wasmtime4j_thread_join",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // thread_handle

    addFunctionBinding(
        "wasmtime4j_thread_join_timeout",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_handle
            ValueLayout.JAVA_LONG, // timeout_ms
            ValueLayout.ADDRESS)); // out_joined

    addFunctionBinding(
        "wasmtime4j_thread_request_termination",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // thread_handle

    addFunctionBinding(
        "wasmtime4j_thread_force_terminate",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // thread_handle

    addFunctionBinding(
        "wasmtime4j_thread_is_termination_requested",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // thread_handle
            ValueLayout.ADDRESS)); // out_requested

    // Memory Management Functions
    addFunctionBinding("wasmtime4j_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // ptr

    // Performance Profiler Functions
    addFunctionBinding(
        "wasmtime4j_profiler_create", FunctionDescriptor.of(ValueLayout.ADDRESS)); // -> profiler*
    addFunctionBinding(
        "wasmtime4j_profiler_start",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_stop",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_modules_compiled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_total_compilation_time_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_average_compilation_time_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_bytes_compiled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_cache_hits",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // hits
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_cache_misses",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // misses
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_optimized_modules",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_current_memory_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_peak_memory_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_uptime_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_function_calls_per_second",
        FunctionDescriptor.of(
            ValueLayout.JAVA_DOUBLE, // calls_per_sec
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_total_function_calls",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_total_execution_time_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_record_compilation",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // profiler
            ValueLayout.JAVA_LONG, // compilation_time_nanos
            ValueLayout.JAVA_LONG, // bytecode_size
            ValueLayout.JAVA_BOOLEAN, // cached
            ValueLayout.JAVA_BOOLEAN)); // optimized
    addFunctionBinding(
        "wasmtime4j_profiler_record_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // profiler
            ValueLayout.ADDRESS, // function_name (C string)
            ValueLayout.JAVA_LONG)); // execution_time_nanos
    addFunctionBinding(
        "wasmtime4j_profiler_reset",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_is_profiling",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // is_profiling
            ValueLayout.ADDRESS)); // profiler

    // Flame Graph Collector Functions
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // FlameGraphCollector pointer
            ValueLayout.JAVA_LONG, // max_samples (usize)
            ValueLayout.JAVA_LONG)); // sampling_interval_ms
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_start",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // collector
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_record_sample",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // collector
            ValueLayout.ADDRESS, // function_name (C string)
            ValueLayout.ADDRESS, // file_name (C string)
            ValueLayout.JAVA_INT, // line_number (u32)
            ValueLayout.JAVA_LONG)); // duration_nanos
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_export_svg",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // collector
            ValueLayout.JAVA_INT, // width (u32)
            ValueLayout.JAVA_INT, // height (u32)
            ValueLayout.ADDRESS, // output_buffer
            ValueLayout.JAVA_LONG)); // buffer_size
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // collector

    // WASI HTTP Functions
    addFunctionBinding("wasi_http_config_builder_new", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasi_http_config_builder_allow_host",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.ADDRESS)); // host (C string)

    addFunctionBinding(
        "wasi_http_config_builder_block_host",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.ADDRESS)); // host (C string)

    addFunctionBinding(
        "wasi_http_config_builder_allow_all_hosts",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_BOOLEAN)); // allow

    addFunctionBinding(
        "wasi_http_config_builder_set_connect_timeout",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_LONG)); // timeout_ms

    addFunctionBinding(
        "wasi_http_config_builder_set_read_timeout",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_LONG)); // timeout_ms

    addFunctionBinding(
        "wasi_http_config_builder_set_write_timeout",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_LONG)); // timeout_ms

    addFunctionBinding(
        "wasi_http_config_builder_set_max_connections",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_INT)); // max_connections

    addFunctionBinding(
        "wasi_http_config_builder_set_max_connections_per_host",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_INT)); // max_connections_per_host

    addFunctionBinding(
        "wasi_http_config_builder_set_max_request_body_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_LONG)); // max_size

    addFunctionBinding(
        "wasi_http_config_builder_set_max_response_body_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_LONG)); // max_size

    addFunctionBinding(
        "wasi_http_config_builder_set_https_required",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_BOOLEAN)); // required

    addFunctionBinding(
        "wasi_http_config_builder_set_certificate_validation",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_BOOLEAN)); // enabled

    addFunctionBinding(
        "wasi_http_config_builder_set_http2_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_BOOLEAN)); // enabled

    addFunctionBinding(
        "wasi_http_config_builder_set_connection_pooling",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_BOOLEAN)); // enabled

    addFunctionBinding(
        "wasi_http_config_builder_set_follow_redirects",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_BOOLEAN)); // follow

    addFunctionBinding(
        "wasi_http_config_builder_set_max_redirects",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.JAVA_INT)); // max_redirects

    addFunctionBinding(
        "wasi_http_config_builder_set_user_agent",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // builder_ptr
            ValueLayout.ADDRESS)); // user_agent (C string)

    addFunctionBinding(
        "wasi_http_config_builder_build",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // config_ptr
            ValueLayout.ADDRESS)); // builder_ptr

    addFunctionBinding(
        "wasi_http_config_builder_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding("wasi_http_config_default", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding("wasi_http_config_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasi_http_ctx_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS)); // config_ptr

    addFunctionBinding("wasi_http_ctx_new_default", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasi_http_ctx_get_id",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // id
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_is_valid",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // is_valid
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_is_host_allowed",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // is_allowed
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS)); // host (C string)

    addFunctionBinding(
        "wasi_http_ctx_reset_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_total_requests",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_successful_requests",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_failed_requests",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_active_requests",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_bytes_sent",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_bytes_received",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_connection_timeouts",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_read_timeouts",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_blocked_requests",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_body_size_violations",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_active_connections",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_idle_connections",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // count
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_avg_duration_ms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // duration_ms
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_min_duration_ms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // duration_ms
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding(
        "wasi_http_ctx_stats_max_duration_ms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // duration_ms
            ValueLayout.ADDRESS)); // ctx_ptr

    addFunctionBinding("wasi_http_ctx_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasi_http_add_to_linker",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // http_ctx_ptr

    addFunctionBinding(
        "wasi_http_is_available",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns 1 if available

    // WASI-Threads Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_is_supported",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns 1 if supported

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_context_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return context*
            ValueLayout.ADDRESS, // module_handle
            ValueLayout.ADDRESS, // linker_handle
            ValueLayout.ADDRESS)); // store_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_context_close",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // context_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_spawn",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // thread_id
            ValueLayout.ADDRESS, // context_handle
            ValueLayout.JAVA_INT)); // thread_start_arg

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_add_to_linker",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // linker_handle
            ValueLayout.ADDRESS, // store_handle
            ValueLayout.ADDRESS)); // module_handle

    // Pooling Allocator Functions
    addFunctionBinding(
        "wasmtime4j_pooling_allocator_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return allocator*
            ValueLayout.JAVA_LONG, // instance_pool_size (usize)
            ValueLayout.JAVA_LONG, // max_memory_per_instance
            ValueLayout.JAVA_INT, // stack_size
            ValueLayout.JAVA_LONG, // max_stacks (usize)
            ValueLayout.JAVA_INT, // max_tables_per_instance
            ValueLayout.JAVA_LONG, // max_tables (usize)
            ValueLayout.JAVA_BOOLEAN, // memory_decommit_enabled
            ValueLayout.JAVA_BOOLEAN, // pool_warming_enabled
            ValueLayout.JAVA_FLOAT)); // pool_warming_percentage

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_allocate_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.ADDRESS)); // instance_id_out

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_reuse_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.JAVA_LONG)); // instance_id

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_release_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.JAVA_LONG)); // instance_id

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_get_statistics",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.ADDRESS)); // stats_out

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_reset_statistics",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_warm_pools",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_perform_maintenance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // allocator*

    // Function Reference functions (Panama FFI)
    addFunctionBinding(
        "wasmtime4j_panama_function_reference_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // param_types (int array)
            ValueLayout.JAVA_INT, // param_count
            ValueLayout.ADDRESS, // return_types (int array)
            ValueLayout.JAVA_INT, // return_count
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG, // callback_id
            ValueLayout.ADDRESS)); // result_out (u64 pointer)

    addFunctionBinding(
        "wasmtime4j_panama_function_reference_destroy",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // registry_id

    addFunctionBinding(
        "wasmtime4j_panama_function_reference_is_valid",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 1 if valid, 0 otherwise
            ValueLayout.JAVA_LONG)); // registry_id

    // Trap Introspection Functions
    addFunctionBinding(
        "wasmtime4j_panama_trap_parse_code",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // trap code
            ValueLayout.ADDRESS)); // error_message (C string)

    addFunctionBinding(
        "wasmtime4j_panama_trap_code_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // trap name (C string)
            ValueLayout.JAVA_INT)); // trap_code

    addFunctionBinding(
        "wasmtime4j_panama_trap_is_trap",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // 1 if trap, 0 otherwise
            ValueLayout.ADDRESS)); // error_message (C string)

    addFunctionBinding(
        "wasmtime4j_panama_trap_extract_function_name",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // bytes written, 0 on error
            ValueLayout.ADDRESS, // backtrace_line (C string)
            ValueLayout.ADDRESS, // out_buffer
            ValueLayout.JAVA_LONG)); // buffer_size

    addFunctionBinding(
        "wasmtime4j_panama_trap_extract_offset",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // instruction offset, -1 on error
            ValueLayout.ADDRESS)); // error_message (C string)

    addFunctionBinding(
        "wasmtime4j_panama_trap_extract_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // 0 on success, non-zero on error
            ValueLayout.ADDRESS, // error_message (C string)
            ValueLayout.ADDRESS)); // out_info (TrapInfo struct)

    // Debug Server Functions
    addFunctionBinding(
        "wasmtime4j_debug_create_server",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns server_ptr
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_debug_destroy_server",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // server_ptr

    addFunctionBinding(
        "wasmtime4j_debug_create_session_ffi",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns session_id (u64)
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_debug_set_breakpoint",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns breakpoint_id (u32)
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG, // session_id (u64)
            ValueLayout.JAVA_INT, // function_index (u32)
            ValueLayout.JAVA_INT)); // instruction_offset (u32)

    addFunctionBinding(
        "wasmtime4j_debug_remove_breakpoint",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG, // session_id (u64)
            ValueLayout.JAVA_INT)); // breakpoint_id (u32)

    addFunctionBinding(
        "wasmtime4j_debug_step_into",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG, // session_id (u64)
            ValueLayout.ADDRESS, // function_index_out (*mut u32)
            ValueLayout.ADDRESS)); // instruction_offset_out (*mut u32)

    addFunctionBinding(
        "wasmtime4j_debug_step_over",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG, // session_id (u64)
            ValueLayout.ADDRESS, // function_index_out (*mut u32)
            ValueLayout.ADDRESS)); // instruction_offset_out (*mut u32)

    addFunctionBinding(
        "wasmtime4j_debug_step_out",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG, // session_id (u64)
            ValueLayout.ADDRESS, // function_index_out (*mut u32)
            ValueLayout.ADDRESS)); // instruction_offset_out (*mut u32)

    addFunctionBinding(
        "wasmtime4j_debug_continue",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG)); // session_id (u64)

    addFunctionBinding(
        "wasmtime4j_debug_pause",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG)); // session_id (u64)

    addFunctionBinding(
        "wasmtime4j_debug_evaluate_expression",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG, // session_id (u64)
            ValueLayout.ADDRESS, // expression (C string)
            ValueLayout.ADDRESS)); // result_out (*mut EvaluationResultFFI)

    addFunctionBinding(
        "wasmtime4j_debug_free_evaluation_result",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // result (*mut EvaluationResultFFI)

    addFunctionBinding(
        "wasmtime4j_debug_get_local_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns count (i32), -1 on error
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG)); // session_id (u64)

    addFunctionBinding(
        "wasmtime4j_debug_get_stack_depth",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns depth (i32), -1 on error
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG)); // session_id (u64)

    addFunctionBinding(
        "wasmtime4j_debug_close_session_ffi",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // returns success
            ValueLayout.ADDRESS, // server_ptr
            ValueLayout.JAVA_LONG)); // session_id (u64)

    // Thread Affinity Functions (NUMA and CPU topology)
    addFunctionBinding(
        "thread_affinity_manager_new",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // out_ptr for manager*

    addFunctionBinding(
        "thread_affinity_manager_free",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // manager_ptr

    addFunctionBinding(
        "thread_affinity_get_cpu_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // manager_ptr
            ValueLayout.ADDRESS)); // out_ptr for count

    addFunctionBinding(
        "thread_affinity_get_total_assignments",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // manager_ptr
            ValueLayout.ADDRESS)); // out_ptr for count

    addFunctionBinding(
        "thread_affinity_get_total_migrations",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // manager_ptr
            ValueLayout.ADDRESS)); // out_ptr for count

    addFunctionBinding(
        "thread_affinity_get_balance_score",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // manager_ptr
            ValueLayout.ADDRESS)); // out_ptr for f64 score

    addFunctionBinding(
        "thread_affinity_get_cache_score",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // manager_ptr
            ValueLayout.ADDRESS)); // out_ptr for f64 score

    addFunctionBinding(
        "thread_affinity_assign_current_thread",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // manager_ptr
            ValueLayout.JAVA_INT, // priority (0=Background, 1=Normal, 2=High, 3=RealTime)
            ValueLayout.JAVA_INT, // cache_sensitivity (0=Low, 1=Medium, 2=High, 3=Critical)
            ValueLayout.JAVA_INT)); // preferred_core (-1 for none)

    addFunctionBinding(
        "thread_affinity_remove_current_thread",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // manager_ptr

    addFunctionBinding(
        "thread_affinity_get_current_core",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return core id or -1 if not assigned
            ValueLayout.ADDRESS)); // manager_ptr

    addFunctionBinding(
        "thread_affinity_enable_dynamic_adjustment",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // manager_ptr

    addFunctionBinding(
        "thread_affinity_disable_dynamic_adjustment",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // manager_ptr

    addFunctionBinding(
        "thread_affinity_is_dynamic_adjustment_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 1 if enabled, 0 if disabled
            ValueLayout.ADDRESS)); // manager_ptr

    addFunctionBinding(
        "thread_affinity_migrate_to_core",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // manager_ptr
            ValueLayout.JAVA_INT)); // core_id

    addFunctionBinding(
        "thread_affinity_get_counters_json",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (JSON string)
            ValueLayout.ADDRESS)); // manager_ptr

    addFunctionBinding(
        "thread_affinity_string_free",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // string ptr

    // CPU Topology Detection Functions
    addFunctionBinding(
        "thread_affinity_get_logical_core_count",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns core count

    addFunctionBinding(
        "thread_affinity_get_physical_core_count",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns core count

    addFunctionBinding(
        "thread_affinity_is_hyperthreading_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns 1 if enabled, 0 otherwise

    addFunctionBinding(
        "thread_affinity_get_l1_cache_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG)); // returns size in bytes

    addFunctionBinding(
        "thread_affinity_get_l2_cache_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG)); // returns size in bytes

    addFunctionBinding(
        "thread_affinity_get_l3_cache_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG)); // returns size in bytes

    addFunctionBinding(
        "thread_affinity_bind_to_core",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_INT)); // core_id

    addFunctionBinding(
        "thread_affinity_get_current_cpu",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns current CPU id or -1 on error

    // Coredump Functions
    addFunctionBinding(
        "wasmtime4j_coredump_free",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns c_int (0 on success, -1 on error)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_frame_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns frame count or -1 on error
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_trap_message",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (must be freed)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (must be freed)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_string_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // char*

    addFunctionBinding(
        "wasmtime4j_coredump_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 0 on success, negative on error
            ValueLayout.JAVA_LONG, // coredump_id (u64)
            ValueLayout.ADDRESS, // store_ptr (*mut Store)
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS, // out_ptr (*mut *mut u8)
            ValueLayout.ADDRESS)); // out_len (*mut c_long)

    addFunctionBinding(
        "wasmtime4j_coredump_bytes_free",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // ptr (*mut u8)
            ValueLayout.JAVA_LONG)); // len (c_long)

    addFunctionBinding(
        "wasmtime4j_coredump_get_frame_info",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (JSON string, must be freed)
            ValueLayout.JAVA_LONG, // coredump_id (u64)
            ValueLayout.JAVA_INT)); // frame_index (c_int)

    addFunctionBinding(
        "wasmtime4j_coredump_get_all_frames",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (JSON array string, must be freed)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_count",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns count or -1 on error

    addFunctionBinding(
        "wasmtime4j_coredump_get_all_ids",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns char* (JSON array of u64 IDs)

    addFunctionBinding(
        "wasmtime4j_coredump_clear_all",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns 0 on success, -1 on error

    // Panama WASI HTTP Config Builder Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_new",
        FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_allow_host",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_block_host",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_allow_all_hosts",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_connect_timeout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_read_timeout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_write_timeout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_connections",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_connections_per_host",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_request_body_size",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_response_body_size",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_https_required",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_certificate_validation",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_http2_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_connection_pooling",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_follow_redirects",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_redirects",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_user_agent",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_build",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_free",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Panama WASI HTTP Config Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_default", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Panama WASI HTTP Context Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_new",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_new_default", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_get_id",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_is_valid",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_is_host_allowed",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_reset_stats",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // Panama WASI HTTP Statistics Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_total_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_successful_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_failed_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_active_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_bytes_sent",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_bytes_received",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_connection_timeouts",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_read_timeouts",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_blocked_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_body_size_violations",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_active_connections",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_idle_connections",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_avg_duration_ms",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_min_duration_ms",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_max_duration_ms",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Panama WASI HTTP Linker Integration Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_add_to_linker",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_is_available", FunctionDescriptor.of(ValueLayout.JAVA_INT));

    // Panama WASI Context Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_set_argv",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_set_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_inherit_env",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_inherit_stdio",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_set_stdin",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_set_stdin_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_set_stdout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_set_stderr",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_enable_output_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_get_stdout_capture",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_get_stderr_capture",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_free_capture_buffer",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_has_stdout_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_has_stderr_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_preopen_dir",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_preopen_dir_readonly",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_context_preopen_dir_with_perms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    // Panama Caller Functions
    addFunctionBinding(
        "wasmtime4j_panama_caller_get_fuel",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_get_fuel_remaining",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_add_fuel",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_caller_set_epoch_deadline",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_caller_has_epoch_deadline",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_has_export",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_get_memory",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_get_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_get_global",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_get_table",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // Panama Instance Pre Functions
    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_instantiate",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_is_valid",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_instance_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_preparation_time_ns",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_avg_instantiation_time_ns",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_get_module",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Panama Experimental Features Functions
    addFunctionBinding(
        "wasmtime4j_panama_experimental_create_config", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_create_all_config",
        FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_stack_switching",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_call_cc",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_extended_const_expressions",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_apply_features",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_flexible_vectors",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_string_imports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_resource_types",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_type_imports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_shared_everything_threads",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_enable_custom_page_sizes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_get_feature_support",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_experimental_destroy_config",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Panama Memory Registry Functions
    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_create",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_register",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_clear_handle_registries",
        FunctionDescriptor.of(ValueLayout.JAVA_INT));

    // Panama Component Functions
    addFunctionBinding(
        "wasmtime4j_panama_component_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_component_instantiate",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_get_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_exports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_imports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_get_active_instances_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_cleanup_instances",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_free_wit_values",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_component_engine_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_instance_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Panama Component Orchestrator and Resource Manager
    addFunctionBinding(
        "wasmtime4j_panama_component_orchestrator_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_resource_manager_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_resource_manager_create_resource",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_distributed_component_manager_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // Panama Component Metrics Functions
    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_get_metrics",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_components_loaded",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_instances_created",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_instances_destroyed",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_avg_instantiation_time_nanos",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_peak_memory_usage",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_function_calls",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_error_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Panama WIT Interface Manager Functions
    addFunctionBinding(
        "wasmtime4j_panama_wit_interface_manager_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wit_interface_manager_register",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // Fuel Callback Functions
    addFunctionBinding(
        "wasmtime4j_fuel_callback_create_auto_refill",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return handler_id (u64)
            ValueLayout.JAVA_LONG, // store_id (u64)
            ValueLayout.JAVA_LONG, // refill_amount (u64)
            ValueLayout.JAVA_INT)); // max_refills (i32, -1 for unlimited)

    addFunctionBinding(
        "wasmtime4j_fuel_callback_create_custom",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return handler_id (u64)
            ValueLayout.JAVA_LONG, // store_id (u64)
            ValueLayout.ADDRESS, // callback_ptr (*const c_void)
            ValueLayout.JAVA_INT, // max_refill_count (i32, -1 for unlimited)
            ValueLayout.JAVA_LONG)); // max_total_fuel (i64, -1 for unlimited)

    addFunctionBinding(
        "wasmtime4j_fuel_callback_handle_exhaustion",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (0 success, non-zero error)
            ValueLayout.JAVA_LONG, // store_id (u64)
            ValueLayout.JAVA_LONG, // fuel_consumed (u64)
            ValueLayout.JAVA_LONG, // initial_fuel (u64)
            ValueLayout.JAVA_INT, // exhaustion_count (u32)
            ValueLayout.ADDRESS, // action_out (*mut i32)
            ValueLayout.ADDRESS)); // additional_fuel_out (*mut u64)

    addFunctionBinding(
        "wasmtime4j_fuel_callback_destroy",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (0 success, non-zero error)
            ValueLayout.JAVA_LONG)); // handler_id (u64)

    addFunctionBinding(
        "wasmtime4j_fuel_callback_get_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (0 success, non-zero error)
            ValueLayout.JAVA_LONG, // handler_id (u64)
            ValueLayout.ADDRESS, // exhaustion_events_out (*mut u64)
            ValueLayout.ADDRESS, // total_fuel_added_out (*mut u64)
            ValueLayout.ADDRESS, // continued_count_out (*mut u64)
            ValueLayout.ADDRESS, // trapped_count_out (*mut u64)
            ValueLayout.ADDRESS)); // paused_count_out (*mut u64)

    addFunctionBinding(
        "wasmtime4j_fuel_callback_reset_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (0 success, non-zero error)
            ValueLayout.JAVA_LONG)); // handler_id (u64)

    // Store Limiter Functions
    addFunctionBinding(
        "wasmtime4j_limiter_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return limiter_id (c_longlong)
            ValueLayout.JAVA_LONG, // max_memory_bytes (c_longlong, -1 for unlimited)
            ValueLayout.JAVA_LONG, // max_memory_pages (c_longlong, -1 for unlimited)
            ValueLayout.JAVA_LONG, // max_table_elements (c_longlong, -1 for unlimited)
            ValueLayout.JAVA_INT, // max_instances (c_int, -1 for unlimited)
            ValueLayout.JAVA_INT, // max_tables (c_int, -1 for unlimited)
            ValueLayout.JAVA_INT)); // max_memories (c_int, -1 for unlimited)

    addFunctionBinding(
        "wasmtime4j_limiter_create_default",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG)); // return limiter_id (c_longlong)

    addFunctionBinding(
        "wasmtime4j_limiter_free",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (0 success, non-zero error)
            ValueLayout.JAVA_LONG)); // limiter_id (c_longlong)

    addFunctionBinding(
        "wasmtime4j_limiter_allow_memory_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (1 allowed, 0 denied)
            ValueLayout.JAVA_LONG, // limiter_id (c_longlong)
            ValueLayout.JAVA_LONG, // current_pages (c_longlong)
            ValueLayout.JAVA_LONG)); // requested_pages (c_longlong)

    addFunctionBinding(
        "wasmtime4j_limiter_allow_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (1 allowed, 0 denied)
            ValueLayout.JAVA_LONG, // limiter_id (c_longlong)
            ValueLayout.JAVA_LONG, // current_elements (c_longlong)
            ValueLayout.JAVA_LONG)); // requested_elements (c_longlong)

    addFunctionBinding(
        "wasmtime4j_limiter_get_stats_json",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return *mut c_char (JSON string)
            ValueLayout.JAVA_LONG)); // limiter_id (c_longlong)

    addFunctionBinding(
        "wasmtime4j_limiter_string_free",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // s (*mut c_char)

    addFunctionBinding(
        "wasmtime4j_limiter_reset_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (0 success, non-zero error)
            ValueLayout.JAVA_LONG)); // limiter_id (c_longlong)

    addFunctionBinding(
        "wasmtime4j_limiter_get_count",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // return c_int (count of active limiters)

    addFunctionBinding(
        "wasmtime4j_limiter_get_config_json",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return *mut c_char (JSON string)
            ValueLayout.JAVA_LONG)); // limiter_id (c_longlong)

    // Exception Handling Bindings
    addFunctionBinding(
        "wasmtime4j_panama_tag_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return *mut c_void (tag pointer)
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // param_types
            ValueLayout.JAVA_INT, // param_count
            ValueLayout.ADDRESS, // return_types
            ValueLayout.JAVA_INT)); // return_count

    addFunctionBinding(
        "wasmtime4j_panama_tag_get_param_types",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return *mut c_int (types array)
            ValueLayout.ADDRESS, // tag_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // out_count

    addFunctionBinding(
        "wasmtime4j_panama_tag_get_return_types",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return *mut c_int (types array)
            ValueLayout.ADDRESS, // tag_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // out_count

    addFunctionBinding(
        "wasmtime4j_panama_tag_types_free",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // types_ptr
            ValueLayout.JAVA_INT)); // count

    addFunctionBinding(
        "wasmtime4j_panama_tag_equals",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (1 if equal, 0 if not)
            ValueLayout.ADDRESS, // tag1_ptr
            ValueLayout.ADDRESS, // tag2_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_tag_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // tag_ptr

    addFunctionBinding(
        "wasmtime4j_panama_exnref_get_tag",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return *mut c_void (tag pointer)
            ValueLayout.ADDRESS, // exnref_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_exnref_is_valid",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (1 if valid)
            ValueLayout.ADDRESS, // exnref_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_exnref_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // exnref_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_throw_exception",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // exnref_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_take_pending_exception",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return *mut c_void (exnref pointer)
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_has_pending_exception",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return c_int (1 if has pending)
            ValueLayout.ADDRESS)); // store_ptr

    // Panama host function for table operations
    addFunctionBinding(
        "wasmtime4j_panama_store_create_host_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // callback_fn
            ValueLayout.JAVA_LONG, // callback_id
            ValueLayout.ADDRESS, // param_types
            ValueLayout.JAVA_INT, // param_count
            ValueLayout.ADDRESS, // return_types
            ValueLayout.JAVA_INT, // return_count
            ValueLayout.ADDRESS)); // func_ref_id_out

    addFunctionBinding(
        "wasmtime4j_panama_destroy_host_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // func_ref_id

    // Atomic memory operations
    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // expected
            ValueLayout.JAVA_INT, // new_value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // expected
            ValueLayout.JAVA_LONG, // new_value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_load_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_load_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_store_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT)); // value

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_store_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG)); // value

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_add_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_add_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_and_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_or_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_xor_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_fence",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_notify",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // count
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_wait32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // expected
            ValueLayout.JAVA_LONG, // timeout_ns
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_wait64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // expected
            ValueLayout.JAVA_LONG, // timeout_ns
            ValueLayout.ADDRESS)); // result_out
  }

  // Panama Linker Functions

  /**
   * Creates a new Panama linker.
   *
   * @param enginePtr pointer to the engine
   * @return pointer to the linker, or null on failure
   */
  public MemorySegment panamaLinkerCreate(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_panama_linker_create", MemorySegment.class, enginePtr);
  }

  /**
   * Defines a host function in the Panama linker.
   *
   * @param linkerPtr pointer to the linker
   * @param moduleName module name (C string)
   * @param name function name (C string)
   * @param paramTypes parameter types array
   * @param paramCount number of parameters
   * @param returnTypes return types array
   * @param returnCount number of returns
   * @param callbackFn callback function pointer
   * @param callbackId callback ID
   * @return 0 on success, non-zero on error
   */
  public int panamaLinkerDefineHostFunction(
      final MemorySegment linkerPtr,
      final MemorySegment moduleName,
      final MemorySegment name,
      final MemorySegment paramTypes,
      final int paramCount,
      final MemorySegment returnTypes,
      final int returnCount,
      final MemorySegment callbackFn,
      final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(moduleName, "moduleName");
    validatePointer(name, "name");
    validatePointer(paramTypes, "paramTypes");
    validatePointer(returnTypes, "returnTypes");
    validatePointer(callbackFn, "callbackFn");

    return callNativeFunction(
        "wasmtime4j_panama_linker_define_host_function",
        Integer.class,
        linkerPtr,
        moduleName,
        name,
        paramTypes,
        paramCount,
        returnTypes,
        returnCount,
        callbackFn,
        callbackId);
  }

  /**
   * Defines a global in the linker (Panama FFI version).
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param moduleNamePtr pointer to the module name string
   * @param namePtr pointer to the global name string
   * @param globalPtr pointer to the global
   * @return 0 on success, negative error code on failure
   */
  public int panamaLinkerDefineGlobal(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment namePtr,
      final MemorySegment globalPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(moduleNamePtr, "moduleNamePtr");
    validatePointer(namePtr, "namePtr");
    validatePointer(globalPtr, "globalPtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_define_global",
        Integer.class,
        linkerPtr,
        storePtr,
        moduleNamePtr,
        namePtr,
        globalPtr);
  }

  /**
   * Defines a memory in the linker (Panama FFI version).
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param moduleNamePtr pointer to the module name string
   * @param namePtr pointer to the memory name string
   * @param memoryPtr pointer to the memory
   * @return 0 on success, negative error code on failure
   */
  public int panamaLinkerDefineMemory(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment namePtr,
      final MemorySegment memoryPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(moduleNamePtr, "moduleNamePtr");
    validatePointer(namePtr, "namePtr");
    validatePointer(memoryPtr, "memoryPtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_define_memory",
        Integer.class,
        linkerPtr,
        storePtr,
        moduleNamePtr,
        namePtr,
        memoryPtr);
  }

  /**
   * Defines a memory from an instance in the linker (Panama FFI version).
   *
   * <p>This variant extracts the memory from the instance and defines it in the linker all within
   * the same store context to avoid store mismatch issues.
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param moduleNamePtr pointer to the module name string
   * @param memoryNamePtr pointer to the memory name string
   * @param instancePtr pointer to the instance containing the memory
   * @param exportNamePtr pointer to the export name of the memory in the instance
   * @return 0 on success, negative error code on failure
   */
  public int panamaLinkerDefineMemoryFromInstance(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment memoryNamePtr,
      final MemorySegment instancePtr,
      final MemorySegment exportNamePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(moduleNamePtr, "moduleNamePtr");
    validatePointer(memoryNamePtr, "memoryNamePtr");
    validatePointer(instancePtr, "instancePtr");
    validatePointer(exportNamePtr, "exportNamePtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_define_memory_from_instance",
        Integer.class,
        linkerPtr,
        storePtr,
        moduleNamePtr,
        memoryNamePtr,
        instancePtr,
        exportNamePtr);
  }

  /**
   * Defines a table in the linker (Panama FFI version).
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param moduleNamePtr pointer to the module name string
   * @param namePtr pointer to the table name string
   * @param tablePtr pointer to the table
   * @return 0 on success, negative error code on failure
   */
  public int panamaLinkerDefineTable(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment namePtr,
      final MemorySegment tablePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(moduleNamePtr, "moduleNamePtr");
    validatePointer(namePtr, "namePtr");
    validatePointer(tablePtr, "tablePtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_define_table",
        Integer.class,
        linkerPtr,
        storePtr,
        moduleNamePtr,
        namePtr,
        tablePtr);
  }

  /**
   * Defines an instance in the linker (Panama FFI version).
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param moduleNamePtr pointer to the module name string
   * @param instancePtr pointer to the instance
   * @return 0 on success, negative error code on failure
   */
  public int panamaLinkerDefineInstance(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment instancePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(moduleNamePtr, "moduleNamePtr");
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_define_instance",
        Integer.class,
        linkerPtr,
        storePtr,
        moduleNamePtr,
        instancePtr);
  }

  /**
   * Instantiates a module using the linker (Panama FFI version).
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param modulePtr pointer to the module
   * @return pointer to the instance, or null on failure
   */
  public MemorySegment panamaLinkerInstantiate(
      final MemorySegment linkerPtr, final MemorySegment storePtr, final MemorySegment modulePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");

    return callNativeFunction(
        "wasmtime4j_linker_instantiate", MemorySegment.class, linkerPtr, storePtr, modulePtr);
  }

  /**
   * Creates an alias for an export in the linker.
   *
   * @param linkerPtr pointer to the linker
   * @param fromModulePtr pointer to the source module name
   * @param fromNamePtr pointer to the source export name
   * @param toModulePtr pointer to the destination module name
   * @param toNamePtr pointer to the destination export name
   * @return 0 on success, negative error code on failure
   */
  public int panamaLinkerAlias(
      final MemorySegment linkerPtr,
      final MemorySegment fromModulePtr,
      final MemorySegment fromNamePtr,
      final MemorySegment toModulePtr,
      final MemorySegment toNamePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(fromModulePtr, "fromModulePtr");
    validatePointer(fromNamePtr, "fromNamePtr");
    validatePointer(toModulePtr, "toModulePtr");
    validatePointer(toNamePtr, "toNamePtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_alias",
        Integer.class,
        linkerPtr,
        fromModulePtr,
        fromNamePtr,
        toModulePtr,
        toNamePtr);
  }

  /**
   * Destroys a Panama linker.
   *
   * @param linkerPtr pointer to the linker to destroy
   */
  public void panamaLinkerDestroy(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    callNativeFunction("wasmtime4j_panama_linker_destroy", Void.class, linkerPtr);
  }

  // =====================================================
  // InstancePre Operations
  // =====================================================

  /**
   * Creates an InstancePre from a linker and module for fast repeated instantiation.
   *
   * @param linkerPtr pointer to the linker
   * @param modulePtr pointer to the module
   * @return pointer to the InstancePre, or null on failure
   */
  public MemorySegment linkerInstantiatePre(
      final MemorySegment linkerPtr, final MemorySegment modulePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_linker_instantiate_pre", MemorySegment.class, linkerPtr, modulePtr);
  }

  /**
   * Instantiates from an InstancePre with a store.
   *
   * @param instancePrePtr pointer to the InstancePre
   * @param storePtr pointer to the store
   * @return pointer to the new instance, or null on failure
   */
  public MemorySegment instancePreInstantiate(
      final MemorySegment instancePrePtr, final MemorySegment storePtr) {
    validatePointer(instancePrePtr, "instancePrePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_instance_pre_instantiate", MemorySegment.class, instancePrePtr, storePtr);
  }

  /**
   * Checks if an InstancePre is valid.
   *
   * @param instancePrePtr pointer to the InstancePre
   * @return 1 if valid, 0 otherwise
   */
  public int instancePreIsValid(final MemorySegment instancePrePtr) {
    validatePointer(instancePrePtr, "instancePrePtr");
    return callNativeFunction("wasmtime4j_instance_pre_is_valid", Integer.class, instancePrePtr);
  }

  /**
   * Gets the instance count for an InstancePre.
   *
   * @param instancePrePtr pointer to the InstancePre
   * @return the number of instances created from this InstancePre
   */
  public long instancePreGetInstanceCount(final MemorySegment instancePrePtr) {
    validatePointer(instancePrePtr, "instancePrePtr");
    return callNativeFunction("wasmtime4j_instance_pre_instance_count", Long.class, instancePrePtr);
  }

  /**
   * Gets the preparation time in nanoseconds for an InstancePre.
   *
   * @param instancePrePtr pointer to the InstancePre
   * @return the preparation time in nanoseconds
   */
  public long instancePreGetPreparationTimeNs(final MemorySegment instancePrePtr) {
    validatePointer(instancePrePtr, "instancePrePtr");
    return callNativeFunction(
        "wasmtime4j_instance_pre_preparation_time_ns", Long.class, instancePrePtr);
  }

  /**
   * Gets the average instantiation time in nanoseconds for an InstancePre.
   *
   * @param instancePrePtr pointer to the InstancePre
   * @return the average instantiation time in nanoseconds
   */
  public long instancePreGetAvgInstantiationTimeNs(final MemorySegment instancePrePtr) {
    validatePointer(instancePrePtr, "instancePrePtr");
    return callNativeFunction(
        "wasmtime4j_instance_pre_avg_instantiation_time_ns", Long.class, instancePrePtr);
  }

  /**
   * Destroys an InstancePre.
   *
   * @param instancePrePtr pointer to the InstancePre to destroy
   */
  public void instancePreDestroy(final MemorySegment instancePrePtr) {
    validatePointer(instancePrePtr, "instancePrePtr");
    callNativeFunction("wasmtime4j_instance_pre_destroy", Void.class, instancePrePtr);
  }

  // Function Reference Functions (Panama FFI)

  /**
   * Creates a new function reference.
   *
   * @param storePtr pointer to the store
   * @param paramTypes parameter types array
   * @param paramCount number of parameters
   * @param returnTypes return types array
   * @param returnCount number of return values
   * @param callbackFn callback function pointer
   * @param callbackId callback ID for identifying the Java callback
   * @param resultOut pointer to store the registry ID
   * @return 0 on success, non-zero on error
   */
  public int functionReferenceCreate(
      final MemorySegment storePtr,
      final MemorySegment paramTypes,
      final int paramCount,
      final MemorySegment returnTypes,
      final int returnCount,
      final MemorySegment callbackFn,
      final long callbackId,
      final MemorySegment resultOut) {
    validatePointer(storePtr, "storePtr");
    validatePointer(paramTypes, "paramTypes");
    validatePointer(returnTypes, "returnTypes");
    validatePointer(callbackFn, "callbackFn");
    validatePointer(resultOut, "resultOut");

    return callNativeFunction(
        "wasmtime4j_panama_function_reference_create",
        Integer.class,
        storePtr,
        paramTypes,
        paramCount,
        returnTypes,
        returnCount,
        callbackFn,
        callbackId,
        resultOut);
  }

  /**
   * Destroys a function reference by its registry ID.
   *
   * @param registryId the registry ID of the function reference
   * @return 0 on success, non-zero on error
   */
  public int functionReferenceDestroy(final long registryId) {
    return callNativeFunction(
        "wasmtime4j_panama_function_reference_destroy", Integer.class, registryId);
  }

  /**
   * Checks if a function reference is valid.
   *
   * @param registryId the registry ID of the function reference
   * @return 1 if valid, 0 otherwise
   */
  public int functionReferenceIsValid(final long registryId) {
    return callNativeFunction(
        "wasmtime4j_panama_function_reference_is_valid", Integer.class, registryId);
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
      // Use library loader to lookup function (which handles symbol lookup and caching)
      Optional<MethodHandle> handle = libraryLoader.lookupFunction(functionName, descriptor);

      if (handle.isPresent()) {
        this.methodHandle = handle.get();
        LOGGER.finest("Initialized method handle for function: " + functionName);
      } else {
        LOGGER.warning("Failed to initialize method handle for function: " + functionName);
      }
    }
  }

  /**
   * Extracts value components from WasmValue for passing to native code.
   *
   * @param value the WasmValue to extract components from
   * @return array containing [i32Value, i64Value, f32Value, f64Value, refValue]
   */
  private Object[] extractValueForNative(final WasmValue value) {
    final Object[] components = new Object[5];

    switch (value.getType()) {
      case I32:
        components[0] = value.asI32();
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = null;
        break;
      case I64:
        components[0] = 0;
        components[1] = value.asI64();
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = null;
        break;
      case F32:
        components[0] = 0;
        components[1] = 0L;
        components[2] = value.asF32();
        components[3] = 0.0;
        components[4] = null;
        break;
      case F64:
        components[0] = 0;
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = value.asF64();
        components[4] = null;
        break;
      case V128:
        // For V128, we'll store as byte array in the reference slot
        components[0] = 0;
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = value.asV128();
        break;
      case FUNCREF:
      case EXTERNREF:
        components[0] = 0;
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = value.getValue();
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }

    return components;
  }

  /**
   * Validates WebAssembly bytes.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to WASM bytes
   * @param wasmSize size of WASM bytes
   * @return true if valid, false otherwise
   */
  public boolean engineValidate(
      final MemorySegment enginePtr, final MemorySegment wasmBytes, final long wasmSize) {
    return callNativeFunction(
        "wasmtime4j_engine_validate", Boolean.class, enginePtr, wasmBytes, wasmSize);
  }

  /**
   * Checks if engine supports a feature.
   *
   * @param enginePtr pointer to the engine
   * @param featureName feature name string
   * @return true if supported, false otherwise
   */
  public boolean engineSupportsFeature(final MemorySegment enginePtr, final String featureName) {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment featureNameSegment = arena.allocateFrom(featureName);
      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_supports_feature",
              Integer.class,
              enginePtr,
              featureNameSegment);
      return result == 1;
    }
  }

  /**
   * Gets the memory limit in pages.
   *
   * @param enginePtr pointer to the engine
   * @return memory limit in pages, or -1 if not set
   */
  public int engineMemoryLimitPages(final MemorySegment enginePtr) {
    return callNativeFunction(
        "wasmtime4j_panama_engine_get_memory_limit", Integer.class, enginePtr);
  }

  /**
   * Gets the stack size limit.
   *
   * @param enginePtr pointer to the engine
   * @return stack size limit in bytes, or -1 if not set
   */
  public long engineStackSizeLimit(final MemorySegment enginePtr) {
    return callNativeFunction("wasmtime4j_panama_engine_get_stack_limit", Long.class, enginePtr);
  }

  /**
   * Checks if fuel is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if fuel is enabled, false otherwise
   */
  public boolean engineFuelEnabled(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_is_fuel_enabled", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if epoch interruption is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if epoch interruption is enabled, false otherwise
   */
  public boolean engineEpochInterruptionEnabled(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_engine_is_epoch_interruption_enabled", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if coredump generation on trap is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if coredump generation is enabled, false otherwise
   */
  public boolean engineCoredumpOnTrapEnabled(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_engine_is_coredump_on_trap_enabled", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Gets the maximum number of instances.
   *
   * @param enginePtr pointer to the engine
   * @return maximum number of instances
   */
  public long engineMaxInstances(final MemorySegment enginePtr) {
    return callNativeFunction("wasmtime4j_engine_max_instances", Long.class, enginePtr);
  }

  /**
   * Gets the reference count.
   *
   * @param enginePtr pointer to the engine
   * @return reference count
   */
  public long engineReferenceCount(final MemorySegment enginePtr) {
    return callNativeFunction("wasmtime4j_engine_reference_count", Long.class, enginePtr);
  }

  /**
   * Increments the epoch counter.
   *
   * <p>This method is signal-safe and performs only an atomic increment. The epoch counter is used
   * for epoch-based interruption of WebAssembly execution.
   *
   * @param enginePtr pointer to the engine
   */
  public void engineIncrementEpoch(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_panama_engine_increment_epoch", Void.class, enginePtr);
  }

  /**
   * Checks if the Pulley interpreter is being used.
   *
   * @param enginePtr pointer to the engine
   * @return true if Pulley interpreter is being used, false if using Cranelift JIT
   */
  public boolean engineIsPulley(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_is_pulley", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Gets the precompilation compatibility hash for this engine.
   *
   * @param enginePtr pointer to the engine
   * @return the compatibility hash as a byte array, or null if not available
   */
  public byte[] enginePrecompileCompatibilityHash(final MemorySegment enginePtr) {
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment outDataPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLenPtr = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_precompile_compatibility_hash",
              Integer.class,
              enginePtr,
              outDataPtr,
              outLenPtr);

      if (result != 0) {
        return null;
      }

      final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
      final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        return null;
      }

      final MemorySegment hashData = dataPtr.reinterpret(dataLen);
      return hashData.toArray(ValueLayout.JAVA_BYTE);
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * Checks if a table supports 64-bit addressing (Table64).
   *
   * @param tablePtr pointer to the table
   * @param storePtr pointer to the store
   * @return true if the table supports 64-bit addressing
   */
  public boolean tableSupports64BitAddressing(
      final MemorySegment tablePtr, final MemorySegment storePtr) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_table_supports_64bit_addressing", Integer.class, tablePtr, storePtr);
    return result == 1;
  }

  /**
   * Precompiles WebAssembly bytecode into a serialized form for ahead-of-time (AOT) usage.
   *
   * <p>This method compiles the WebAssembly binary into a serialized form that can be later loaded
   * via Module.deserialize() without needing to recompile.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes the WebAssembly bytecode to precompile
   * @return the precompiled serialized module bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if precompilation fails
   */
  public byte[] enginePrecompileModule(final MemorySegment enginePtr, final byte[] wasmBytes)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(enginePtr, "enginePtr");
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }

    try (Arena tempArena = Arena.ofConfined()) {
      // Allocate and copy wasm bytes
      final MemorySegment wasmBytesSegment =
          tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);

      // Allocate output pointers
      final MemorySegment outDataPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLenPtr = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_precompile_module",
              Integer.class,
              enginePtr,
              wasmBytesSegment,
              (long) wasmBytes.length,
              outDataPtr,
              outLenPtr);

      if (result != 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to precompile module (error code: " + result + ")");
      }

      // Read output values
      final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
      final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Precompilation returned invalid data");
      }

      // Copy data to byte array
      final MemorySegment dataSegment = dataPtr.reinterpret(dataLen);
      final byte[] precompiledBytes = dataSegment.toArray(ValueLayout.JAVA_BYTE);

      // Free the native memory
      serializerFreeBuffer(dataPtr, dataLen);

      return precompiledBytes;
    }
  }

  // ===== SIMD Operations =====

  /**
   * SIMD vector addition.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdAdd(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_add", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD vector subtraction.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdSubtract(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_subtract",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD vector multiplication.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdMultiply(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_multiply",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD vector division.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdDivide(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_divide",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD bitwise AND.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdAnd(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_and", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD bitwise OR.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdOr(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_or", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD bitwise XOR.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdXor(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_xor", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD bitwise NOT.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return pointer to result vector bytes
   */
  public int simdNot(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_not", Integer.class, runtimeHandle, vector, resultData);
  }

  /**
   * SIMD equality comparison.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdEquals(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_equals",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD less than comparison.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdLessThan(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_less_than",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD greater than comparison.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdGreaterThan(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_greater_than",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD saturated addition.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdAddSaturated(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_add_saturated",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD square root.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return pointer to result vector bytes
   */
  public int simdSqrt(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_sqrt", Integer.class, runtimeHandle, vector, resultData);
  }

  /**
   * SIMD reciprocal.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return pointer to result vector bytes
   */
  public int simdReciprocal(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_reciprocal", Integer.class, runtimeHandle, vector, resultData);
  }

  /**
   * SIMD reciprocal square root.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return pointer to result vector bytes
   */
  public int simdRsqrt(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_rsqrt", Integer.class, runtimeHandle, vector, resultData);
  }

  /**
   * SIMD fused multiply-add.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param vectorC pointer to third vector bytes
   * @return pointer to result vector bytes
   */
  public int simdFma(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment vectorC,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_fma",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        vectorC,
        resultData);
  }

  /**
   * SIMD fused multiply-subtract.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param vectorC pointer to third vector bytes
   * @return pointer to result vector bytes
   */
  public int simdFms(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment vectorC,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_fms",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        vectorC,
        resultData);
  }

  /**
   * SIMD shuffle.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param indices pointer to indices bytes
   * @return pointer to result vector bytes
   */
  public int simdShuffle(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment indices,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_shuffle",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        indices,
        resultData);
  }

  /**
   * SIMD relaxed addition.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @return pointer to result vector bytes
   */
  public int simdRelaxedAdd(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_relaxed_add",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD extract lane i32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param laneIndex lane index (0-3)
   * @return extracted lane value
   */
  public int simdExtractLaneI32(
      final long runtimeHandle, final MemorySegment vector, final int laneIndex) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_extract_lane_i32", Integer.class, runtimeHandle, vector, laneIndex);
  }

  /**
   * SIMD replace lane i32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param laneIndex lane index (0-3)
   * @param value value to insert
   * @return pointer to result vector bytes
   */
  public int simdReplaceLaneI32(
      final long runtimeHandle,
      final MemorySegment vector,
      final int laneIndex,
      final int value,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_replace_lane_i32",
        Integer.class,
        runtimeHandle,
        vector,
        laneIndex,
        value,
        resultData);
  }

  /**
   * SIMD convert i32 to f32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return pointer to result vector bytes
   */
  public int simdConvertI32ToF32(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_convert_i32_to_f32",
        Integer.class,
        runtimeHandle,
        vector,
        resultData);
  }

  /**
   * SIMD convert f32 to i32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return pointer to result vector bytes
   */
  public int simdConvertF32ToI32(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_convert_f32_to_i32",
        Integer.class,
        runtimeHandle,
        vector,
        resultData);
  }

  /**
   * SIMD horizontal sum reduction.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return sum of all i32 lanes
   */
  public int simdHorizontalSumI32(final long runtimeHandle, final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_horizontal_sum_i32", Integer.class, runtimeHandle, vector);
  }

  /**
   * SIMD horizontal min reduction.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return minimum of all i32 lanes
   */
  public int simdHorizontalMinI32(final long runtimeHandle, final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_horizontal_min_i32", Integer.class, runtimeHandle, vector);
  }

  /**
   * SIMD horizontal max reduction.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return maximum of all i32 lanes
   */
  public int simdHorizontalMaxI32(final long runtimeHandle, final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_horizontal_max_i32", Integer.class, runtimeHandle, vector);
  }

  /**
   * SIMD load from memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdLoad(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_load", Integer.class, runtimeHandle, memoryHandle, offset, result);
  }

  /**
   * SIMD aligned load from memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdLoadAligned(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_load_aligned",
        Integer.class,
        runtimeHandle,
        memoryHandle,
        offset,
        result);
  }

  /**
   * SIMD store to memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param vector the vector data
   * @return status code (0 for success)
   */
  public int simdStore(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_store", Integer.class, runtimeHandle, memoryHandle, offset, vector);
  }

  /**
   * SIMD aligned store to memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param vector the vector data
   * @return status code (0 for success)
   */
  public int simdStoreAligned(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_store_aligned",
        Integer.class,
        runtimeHandle,
        memoryHandle,
        offset,
        vector);
  }

  /**
   * SIMD popcount operation.
   *
   * @param runtimeHandle the runtime handle
   * @param vector the vector data
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdPopcount(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_popcount", Integer.class, runtimeHandle, vector, result);
  }

  /**
   * SIMD variable shift left.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA the first vector
   * @param vectorB the shift amounts vector
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdShlVariable(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_shl_variable",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        result);
  }

  /**
   * SIMD variable shift right.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA the first vector
   * @param vectorB the shift amounts vector
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdShrVariable(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_shr_variable",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        result);
  }

  /**
   * SIMD select operation.
   *
   * @param runtimeHandle the runtime handle
   * @param mask the mask vector
   * @param vectorA the first vector
   * @param vectorB the second vector
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdSelect(
      final long runtimeHandle,
      final MemorySegment mask,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_select",
        Integer.class,
        runtimeHandle,
        mask,
        vectorA,
        vectorB,
        result);
  }

  /**
   * SIMD blend operation.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA the first vector
   * @param vectorB the second vector
   * @param mask the blend mask
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdBlend(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final int mask,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_blend",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        mask,
        result);
  }

  // Component Model Functions

  /**
   * Creates a new component engine.
   *
   * @param config the engine configuration
   * @return memory segment pointer to the component engine, or null on failure
   */
  public MemorySegment componentEngineCreate(final MemorySegment config) {
    return callNativeFunction("wasmtime4j_component_engine_create", MemorySegment.class, config);
  }

  /**
   * Destroys a component engine.
   *
   * @param engineHandle pointer to the component engine to destroy
   */
  public void componentEngineDestroy(final MemorySegment engineHandle) {
    validatePointer(engineHandle, "engineHandle");
    callNativeFunction("wasmtime4j_component_engine_destroy", Void.class, engineHandle);
  }

  /**
   * Loads a component from bytecode.
   *
   * @param engineHandle the component engine handle
   * @param bytes the component bytecode
   * @param length the bytecode length
   * @param componentOut pointer to receive the component handle
   * @return 0 on success, non-zero on error
   */
  public int componentLoadFromBytes(
      final MemorySegment engineHandle,
      final MemorySegment bytes,
      final long length,
      final MemorySegment componentOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(bytes, "bytes");
    validatePointer(componentOut, "componentOut");
    return callNativeFunction(
        "wasmtime4j_component_compile", Integer.class, engineHandle, bytes, length, componentOut);
  }

  /**
   * Instantiates a component.
   *
   * @param engineHandle the engine handle
   * @param componentHandle the component handle
   * @param instanceOut pointer to where the instance handle will be written
   * @return error code (0 = success, non-zero = error)
   */
  public int componentInstantiate(
      final MemorySegment engineHandle,
      final MemorySegment componentHandle,
      final MemorySegment instanceOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(componentHandle, "componentHandle");
    validatePointer(instanceOut, "instanceOut");
    return callNativeFunction(
        "wasmtime4j_component_instantiate",
        Integer.class,
        engineHandle,
        componentHandle,
        instanceOut);
  }

  /**
   * Destroys a component.
   *
   * @param componentHandle pointer to the component to destroy
   */
  public void componentDestroy(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    callNativeFunction("wasmtime4j_component_destroy", Void.class, componentHandle);
  }

  /**
   * Destroys a component instance.
   *
   * @param instanceHandle pointer to the component instance to destroy
   */
  public void componentInstanceDestroy(final MemorySegment instanceHandle) {
    validatePointer(instanceHandle, "instanceHandle");
    callNativeFunction("wasmtime4j_component_instance_destroy", Void.class, instanceHandle);
  }

  /**
   * Gets the size of a component.
   *
   * @param componentHandle the component handle
   * @return the component size in bytes
   */
  public long componentGetSize(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_size_bytes", Long.class, componentHandle);
  }

  /**
   * Checks if a component exports an interface.
   *
   * @param componentHandle the component handle
   * @param interfaceName the interface name (C string)
   * @return 1 if exports the interface, 0 otherwise
   */
  public int componentExportsInterface(
      final MemorySegment componentHandle, final MemorySegment interfaceName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(interfaceName, "interfaceName");
    return callNativeFunction(
        "wasmtime4j_component_exports_interface", Integer.class, componentHandle, interfaceName);
  }

  /**
   * Checks if a component imports an interface.
   *
   * @param componentHandle the component handle
   * @param interfaceName the interface name (C string)
   * @return 1 if imports the interface, 0 otherwise
   */
  public int componentImportsInterface(
      final MemorySegment componentHandle, final MemorySegment interfaceName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(interfaceName, "interfaceName");
    return callNativeFunction(
        "wasmtime4j_component_imports_interface", Integer.class, componentHandle, interfaceName);
  }

  /**
   * Gets the count of exports in a component.
   *
   * @param componentHandle the component handle
   * @return number of exports
   */
  public long componentExportCount(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_export_count", Long.class, componentHandle);
  }

  /**
   * Gets the count of imports in a component.
   *
   * @param componentHandle the component handle
   * @return number of imports
   */
  public long componentImportCount(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_import_count", Long.class, componentHandle);
  }

  /**
   * Checks if a component has a specific export.
   *
   * @param componentHandle the component handle
   * @param exportName the export name (C string)
   * @return 1 if has the export, 0 otherwise
   */
  public int componentHasExport(
      final MemorySegment componentHandle, final MemorySegment exportName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(exportName, "exportName");
    return callNativeFunction(
        "wasmtime4j_component_has_export", Integer.class, componentHandle, exportName);
  }

  /**
   * Checks if a component has a specific import.
   *
   * @param componentHandle the component handle
   * @param importName the import name (C string)
   * @return 1 if has the import, 0 otherwise
   */
  public int componentHasImport(
      final MemorySegment componentHandle, final MemorySegment importName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(importName, "importName");
    return callNativeFunction(
        "wasmtime4j_component_has_import", Integer.class, componentHandle, importName);
  }

  /**
   * Validates a component.
   *
   * @param componentHandle the component handle
   * @return 1 if valid, 0 otherwise
   */
  public int componentValidate(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_validate", Integer.class, componentHandle);
  }

  /**
   * Gets an export interface name by index.
   *
   * @param componentHandle the component handle
   * @param index the export index
   * @param nameOut pointer to receive the name string
   * @return 0 on success, non-zero on error
   */
  public int componentGetExportName(
      final MemorySegment componentHandle, final long index, final MemorySegment nameOut) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(nameOut, "nameOut");
    return callNativeFunction(
        "wasmtime4j_component_get_export_name", Integer.class, componentHandle, index, nameOut);
  }

  /**
   * Gets an import interface name by index.
   *
   * @param componentHandle the component handle
   * @param index the import index
   * @param nameOut pointer to receive the name string
   * @return 0 on success, non-zero on error
   */
  public int componentGetImportName(
      final MemorySegment componentHandle, final long index, final MemorySegment nameOut) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(nameOut, "nameOut");
    return callNativeFunction(
        "wasmtime4j_component_get_import_name", Integer.class, componentHandle, index, nameOut);
  }

  /**
   * Frees a string returned by component functions.
   *
   * @param stringPtr the string pointer to free
   */
  public void componentFreeString(final MemorySegment stringPtr) {
    if (stringPtr != null && !stringPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_component_free_string", Void.class, stringPtr);
    }
  }

  /**
   * Gets the list of exported function names from a component instance.
   *
   * @param instanceHandle the component instance handle
   * @param functionsOut output parameter for array of function name pointers
   * @param countOut output parameter for count of functions
   * @return 0 on success, non-zero error code on failure
   */
  public int componentGetExportedFunctions(
      final MemorySegment instanceHandle,
      final MemorySegment functionsOut,
      final MemorySegment countOut) {
    validatePointer(instanceHandle, "instanceHandle");
    validatePointer(functionsOut, "functionsOut");
    validatePointer(countOut, "countOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_get_exported_functions",
        Integer.class,
        instanceHandle,
        functionsOut,
        countOut);
  }

  /**
   * Invokes a component function with parameters.
   *
   * @param instanceHandle the component instance handle
   * @param functionName the function name to invoke
   * @param paramsPtr pointer to the parameters array
   * @param paramsCount number of parameters
   * @param resultsOut output parameter for results array
   * @param resultsCountOut output parameter for results count
   * @return 0 on success, non-zero error code on failure
   */
  public int componentInvoke(
      final MemorySegment instanceHandle,
      final MemorySegment functionName,
      final MemorySegment paramsPtr,
      final int paramsCount,
      final MemorySegment resultsOut,
      final MemorySegment resultsCountOut) {
    validatePointer(instanceHandle, "instanceHandle");
    validatePointer(functionName, "functionName");
    validatePointer(resultsOut, "resultsOut");
    validatePointer(resultsCountOut, "resultsCountOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_invoke",
        Integer.class,
        instanceHandle,
        functionName,
        paramsPtr,
        paramsCount,
        resultsOut,
        resultsCountOut);
  }

  /**
   * Frees an array of C strings returned by component functions.
   *
   * @param stringsPtr pointer to array of string pointers
   * @param count number of strings in the array
   */
  public void componentFreeStringArray(final MemorySegment stringsPtr, final int count) {
    if (stringsPtr != null && !stringsPtr.equals(MemorySegment.NULL)) {
      callNativeFunction(
          "wasmtime4j_panama_component_free_string_array", Void.class, stringsPtr, count);
    }
  }

  // Enhanced Component Engine Functions (using instance IDs)

  /**
   * Creates an enhanced component engine for managing component instances.
   *
   * @return pointer to the enhanced component engine, or NULL on failure
   */
  public MemorySegment enhancedComponentEngineCreate() {
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_engine_create", MemorySegment.class);
  }

  /**
   * Loads (compiles) a component from WebAssembly bytes.
   *
   * @param engineHandle the enhanced component engine handle
   * @param wasmBytes the WebAssembly component bytes
   * @param wasmSize the size of the WebAssembly bytes
   * @param componentOut output parameter for the component handle
   * @return error code (0 for success)
   */
  public int enhancedComponentLoadFromBytes(
      final MemorySegment engineHandle,
      final MemorySegment wasmBytes,
      final long wasmSize,
      final MemorySegment componentOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(wasmBytes, "wasmBytes");
    validatePointer(componentOut, "componentOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_load_from_bytes",
        Integer.class,
        engineHandle,
        wasmBytes,
        wasmSize,
        componentOut);
  }

  /**
   * Instantiates a component and returns an instance ID.
   *
   * @param engineHandle the enhanced component engine handle
   * @param componentHandle the component handle
   * @param instanceIdOut output parameter for the instance ID (u64)
   * @return 0 on success, non-zero error code on failure
   */
  public int enhancedComponentInstantiate(
      final MemorySegment engineHandle,
      final MemorySegment componentHandle,
      final MemorySegment instanceIdOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(componentHandle, "componentHandle");
    validatePointer(instanceIdOut, "instanceIdOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_instantiate",
        Integer.class,
        engineHandle,
        componentHandle,
        instanceIdOut);
  }

  /**
   * Invokes a component function using instance ID.
   *
   * @param engineHandle the enhanced component engine handle
   * @param instanceId the instance ID returned from instantiation
   * @param functionName the function name to invoke
   * @param paramsPtr pointer to the parameters array
   * @param paramsCount number of parameters
   * @param resultsOut output parameter for results array
   * @param resultsCountOut output parameter for results count
   * @return 0 on success, non-zero error code on failure
   */
  public int enhancedComponentInvoke(
      final MemorySegment engineHandle,
      final long instanceId,
      final MemorySegment functionName,
      final MemorySegment paramsPtr,
      final int paramsCount,
      final MemorySegment resultsOut,
      final MemorySegment resultsCountOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(functionName, "functionName");
    validatePointer(resultsOut, "resultsOut");
    validatePointer(resultsCountOut, "resultsCountOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_invoke",
        Integer.class,
        engineHandle,
        instanceId,
        functionName,
        paramsPtr,
        paramsCount,
        resultsOut,
        resultsCountOut);
  }

  /**
   * Gets the list of exported function names using instance ID.
   *
   * @param engineHandle the enhanced component engine handle
   * @param instanceId the instance ID
   * @param functionsOut output parameter for array of function name pointers
   * @param countOut output parameter for count of functions
   * @return 0 on success, non-zero error code on failure
   */
  public int enhancedComponentGetExports(
      final MemorySegment engineHandle,
      final long instanceId,
      final MemorySegment functionsOut,
      final MemorySegment countOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(functionsOut, "functionsOut");
    validatePointer(countOut, "countOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_get_exports",
        Integer.class,
        engineHandle,
        instanceId,
        functionsOut,
        countOut);
  }

  /**
   * Destroys an enhanced component engine and all its instances.
   *
   * @param engineHandle the enhanced component engine handle
   */
  public void enhancedComponentEngineDestroy(final MemorySegment engineHandle) {
    if (engineHandle != null && !engineHandle.equals(MemorySegment.NULL)) {
      callNativeFunction(
          "wasmtime4j_panama_enhanced_component_engine_destroy", Void.class, engineHandle);
    }
  }

  // Component Metrics Functions

  /**
   * Gets component metrics from an enhanced component engine.
   *
   * @param engineHandle the enhanced component engine handle
   * @param metricsOut output pointer for the metrics handle
   * @return 0 on success, non-zero on error
   */
  public int enhancedComponentGetMetrics(
      final MemorySegment engineHandle, final MemorySegment metricsOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(metricsOut, "metricsOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_get_metrics",
        Integer.class,
        engineHandle,
        metricsOut);
  }

  /**
   * Gets the components loaded count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of components loaded
   */
  public long componentMetricsGetComponentsLoaded(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_components_loaded", Long.class, metricsHandle);
  }

  /**
   * Gets the instances created count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of instances created
   */
  public long componentMetricsGetInstancesCreated(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_instances_created", Long.class, metricsHandle);
  }

  /**
   * Gets the instances destroyed count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of instances destroyed
   */
  public long componentMetricsGetInstancesDestroyed(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_instances_destroyed", Long.class, metricsHandle);
  }

  /**
   * Gets the average instantiation time in nanoseconds from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the average instantiation time in nanoseconds
   */
  public long componentMetricsGetAvgInstantiationTimeNanos(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_avg_instantiation_time_nanos",
        Long.class,
        metricsHandle);
  }

  /**
   * Gets the peak memory usage from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the peak memory usage in bytes
   */
  public long componentMetricsGetPeakMemoryUsage(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_peak_memory_usage", Long.class, metricsHandle);
  }

  /**
   * Gets the function calls count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of function calls
   */
  public long componentMetricsGetFunctionCalls(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_function_calls", Long.class, metricsHandle);
  }

  /**
   * Gets the error count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the error count
   */
  public long componentMetricsGetErrorCount(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_error_count", Long.class, metricsHandle);
  }

  /**
   * Destroys component metrics and frees associated resources.
   *
   * @param metricsHandle the component metrics handle to destroy
   */
  public void componentMetricsDestroy(final MemorySegment metricsHandle) {
    if (metricsHandle != null && !metricsHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_component_metrics_destroy", Void.class, metricsHandle);
    }
  }

  // WIT Value Marshalling Functions

  /**
   * Serializes a WIT value to binary format.
   *
   * @param typeDiscriminator the type discriminator (1-6)
   * @param valuePtr pointer to the value data
   * @param valueLen length of the value data
   * @param outData output buffer for serialized data
   * @param outLen output parameter for data length
   * @return 0 on success, non-zero on error
   */
  public int witValueSerialize(
      final int typeDiscriminator,
      final MemorySegment valuePtr,
      final long valueLen,
      final MemorySegment outData,
      final MemorySegment outLen) {
    validatePointer(valuePtr, "valuePtr");
    validatePointer(outData, "outData");
    validatePointer(outLen, "outLen");
    return callNativeFunction(
        "wasmtime4j_wit_value_serialize",
        Integer.class,
        typeDiscriminator,
        valuePtr,
        valueLen,
        outData,
        outLen);
  }

  /**
   * Deserializes a WIT value from binary format.
   *
   * @param typeDiscriminator the type discriminator (1-6)
   * @param dataPtr pointer to the serialized data
   * @param dataLen length of the data
   * @param outValue output buffer for deserialized value
   * @param outLen output parameter for value length
   * @return 0 on success, non-zero on error
   */
  public int witValueDeserialize(
      final int typeDiscriminator,
      final MemorySegment dataPtr,
      final long dataLen,
      final MemorySegment outValue,
      final MemorySegment outLen) {
    validatePointer(dataPtr, "dataPtr");
    validatePointer(outValue, "outValue");
    validatePointer(outLen, "outLen");
    return callNativeFunction(
        "wasmtime4j_wit_value_deserialize",
        Integer.class,
        typeDiscriminator,
        dataPtr,
        dataLen,
        outValue,
        outLen);
  }

  /**
   * Validates a type discriminator.
   *
   * @param typeDiscriminator the type discriminator to validate
   * @return true if valid (1), false otherwise (0)
   */
  public boolean witValueValidateDiscriminator(final int typeDiscriminator) {
    final int result =
        callNativeFunction(
            "wasmtime4j_wit_value_validate_discriminator", Integer.class, typeDiscriminator);
    return result == 1;
  }

  /**
   * Frees a buffer allocated by WIT value marshalling functions.
   *
   * @param ptr pointer to the buffer to free
   * @param len length of the buffer
   */
  public void witValueFreeBuffer(final MemorySegment ptr, final long len) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL) && len > 0) {
      callNativeFunction("wasmtime4j_wit_value_free_buffer", Void.class, ptr, len);
    }
  }

  // WASI Context Functions

  /**
   * Creates a new WASI context.
   *
   * @return pointer to the WASI context, or null on failure
   */
  public MemorySegment wasiContextCreate() {
    return callNativeFunction("wasmtime4j_wasi_context_create", MemorySegment.class);
  }

  /**
   * Destroys a WASI context.
   *
   * @param contextHandle the WASI context handle
   */
  public void wasiContextDestroy(final MemorySegment contextHandle) {
    if (contextHandle != null && !contextHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_wasi_context_destroy", Void.class, contextHandle);
    }
  }

  /**
   * Sets command line arguments for the WASI context.
   *
   * @param contextHandle the WASI context handle
   * @param args pointer to array of C strings
   * @param argCount number of arguments
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetArgv(
      final MemorySegment contextHandle, final MemorySegment args, final long argCount) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_argv", Integer.class, contextHandle, args, argCount);
  }

  /**
   * Sets an environment variable in the WASI context.
   *
   * @param contextHandle the WASI context handle
   * @param key the environment variable name (C string)
   * @param value the environment variable value (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetEnv(
      final MemorySegment contextHandle, final MemorySegment key, final MemorySegment value) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(key, "key");
    validatePointer(value, "value");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_env", Integer.class, contextHandle, key, value);
  }

  /**
   * Inherits all host environment variables.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on error
   */
  public int wasiContextInheritEnv(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction("wasmtime4j_wasi_context_inherit_env", Integer.class, contextHandle);
  }

  /**
   * Inherits host stdio streams.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on error
   */
  public int wasiContextInheritStdio(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_inherit_stdio", Integer.class, contextHandle);
  }

  /**
   * Sets stdin to read from a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStdin(final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stdin", Integer.class, contextHandle, path);
  }

  /**
   * Sets stdin from binary data buffer (supports binary data with null bytes).
   *
   * @param contextHandle the WASI context handle
   * @param dataPtr pointer to the binary data
   * @param dataLen length of the data in bytes
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStdinBytes(
      final MemorySegment contextHandle, final MemorySegment dataPtr, final long dataLen) {
    validatePointer(contextHandle, "contextHandle");
    // dataPtr can be NULL for empty input
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stdin_bytes",
        Integer.class,
        contextHandle,
        dataPtr == null ? MemorySegment.NULL : dataPtr,
        dataLen);
  }

  /**
   * Sets stdout to write to a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStdout(final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stdout", Integer.class, contextHandle, path);
  }

  /**
   * Sets stderr to write to a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStderr(final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stderr", Integer.class, contextHandle, path);
  }

  /**
   * Adds a pre-opened directory with full permissions.
   *
   * @param contextHandle the WASI context handle
   * @param hostPath the host filesystem path (C string)
   * @param guestPath the guest path visible to WASI module (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextPreopenDir(
      final MemorySegment contextHandle,
      final MemorySegment hostPath,
      final MemorySegment guestPath) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(hostPath, "hostPath");
    validatePointer(guestPath, "guestPath");
    return callNativeFunction(
        "wasmtime4j_wasi_context_preopen_dir", Integer.class, contextHandle, hostPath, guestPath);
  }

  /**
   * Adds a pre-opened directory with read-only permissions.
   *
   * @param contextHandle the WASI context handle
   * @param hostPath the host filesystem path (C string)
   * @param guestPath the guest path visible to WASI module (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextPreopenDirReadonly(
      final MemorySegment contextHandle,
      final MemorySegment hostPath,
      final MemorySegment guestPath) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(hostPath, "hostPath");
    validatePointer(guestPath, "guestPath");
    return callNativeFunction(
        "wasmtime4j_wasi_context_preopen_dir_readonly",
        Integer.class,
        contextHandle,
        hostPath,
        guestPath);
  }

  /**
   * Adds a pre-opened directory with custom permissions.
   *
   * @param contextHandle the WASI context handle
   * @param hostPath the host filesystem path (C string)
   * @param guestPath the guest path visible to WASI module (C string)
   * @param canRead whether reading is allowed
   * @param canWrite whether writing is allowed
   * @param canCreate whether file creation is allowed
   * @return 0 on success, non-zero on error
   */
  public int wasiContextPreopenDirWithPerms(
      final MemorySegment contextHandle,
      final MemorySegment hostPath,
      final MemorySegment guestPath,
      final int canRead,
      final int canWrite,
      final int canCreate) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(hostPath, "hostPath");
    validatePointer(guestPath, "guestPath");
    return callNativeFunction(
        "wasmtime4j_wasi_context_preopen_dir_with_perms",
        Integer.class,
        contextHandle,
        hostPath,
        guestPath,
        canRead,
        canWrite,
        canCreate);
  }

  // ===== WASI Output Capture Methods =====

  /**
   * Enables output capture for stdout and stderr.
   *
   * <p>This configures the WASI context to capture stdout and stderr to internal buffers instead of
   * inheriting from the host process.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on error
   */
  public int wasiContextEnableOutputCapture(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_enable_output_capture", Integer.class, contextHandle);
  }

  /**
   * Gets captured stdout data.
   *
   * <p>Returns a pointer to the captured stdout data and sets the length in the output parameter.
   * The caller must free the returned buffer using wasiFreeCaptureBuffer.
   *
   * @param contextHandle the WASI context handle
   * @param lengthOut output parameter for the data length
   * @return pointer to captured data, or NULL if capture is not enabled or empty
   */
  public MemorySegment wasiContextGetStdoutCapture(
      final MemorySegment contextHandle, final MemorySegment lengthOut) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(lengthOut, "lengthOut");
    return callNativeFunction(
        "wasmtime4j_wasi_context_get_stdout_capture",
        MemorySegment.class,
        contextHandle,
        lengthOut);
  }

  /**
   * Gets captured stderr data.
   *
   * <p>Returns a pointer to the captured stderr data and sets the length in the output parameter.
   * The caller must free the returned buffer using wasiFreeCaptureBuffer.
   *
   * @param contextHandle the WASI context handle
   * @param lengthOut output parameter for the data length
   * @return pointer to captured data, or NULL if capture is not enabled or empty
   */
  public MemorySegment wasiContextGetStderrCapture(
      final MemorySegment contextHandle, final MemorySegment lengthOut) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(lengthOut, "lengthOut");
    return callNativeFunction(
        "wasmtime4j_wasi_context_get_stderr_capture",
        MemorySegment.class,
        contextHandle,
        lengthOut);
  }

  /**
   * Frees a capture buffer allocated by getStdoutCapture or getStderrCapture.
   *
   * @param buffer pointer to the buffer to free (can be NULL)
   */
  public void wasiFreeCaptureBuffer(final MemorySegment buffer) {
    // buffer can be NULL
    callNativeFunction("wasmtime4j_wasi_free_capture_buffer", Void.class, buffer);
  }

  /**
   * Checks if stdout capture is enabled.
   *
   * @param contextHandle the WASI context handle
   * @return 1 if capture is enabled, 0 if not, -1 on error
   */
  public int wasiContextHasStdoutCapture(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_has_stdout_capture", Integer.class, contextHandle);
  }

  /**
   * Checks if stderr capture is enabled.
   *
   * @param contextHandle the WASI context handle
   * @return 1 if capture is enabled, 0 if not, -1 on error
   */
  public int wasiContextHasStderrCapture(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_has_stderr_capture", Integer.class, contextHandle);
  }

  /**
   * Adds a WASI context to a Store.
   *
   * <p>This must be called before instantiating WASI-enabled modules. The context will be used by
   * WASI imports when they are called.
   *
   * @param contextHandle the WASI context handle
   * @param storeHandle the Store handle
   * @return 0 on success, non-zero on error
   */
  public int wasiCtxAddToStore(final MemorySegment contextHandle, final MemorySegment storeHandle) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(storeHandle, "storeHandle");
    return callNativeFunction("wasi_ctx_add_to_store", Integer.class, contextHandle, storeHandle);
  }

  /**
   * Adds WASI Preview 1 imports to a linker.
   *
   * <p>Configures the linker to extract WASI context from store data when instantiating modules.
   * The store must have a WASI context attached before instantiating WASI-enabled modules.
   *
   * @param linkerHandle pointer to the linker
   * @return 0 on success, non-zero on failure
   */
  public int linkerAddWasi(final MemorySegment linkerHandle) {
    validatePointer(linkerHandle, "linkerHandle");
    return callNativeFunction("wasmtime4j_linker_add_wasi", Integer.class, linkerHandle);
  }

  // ===== WASI Preview 2 Clock Functions =====

  /**
   * Gets the current monotonic clock time in nanoseconds.
   *
   * @param outTime pointer to store the time value (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockNow(final MemorySegment outTime) {
    validatePointer(outTime, "outTime");
    return callNativeFunction("wasmtime4j_panama_wasi_monotonic_clock_now", Integer.class, outTime);
  }

  /**
   * Gets the monotonic clock resolution in nanoseconds.
   *
   * @param outResolution pointer to store the resolution value (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockResolution(final MemorySegment outResolution) {
    validatePointer(outResolution, "outResolution");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_monotonic_clock_resolution", Integer.class, outResolution);
  }

  /**
   * Subscribes to the monotonic clock for a specific instant.
   *
   * @param when the instant to subscribe to (nanoseconds)
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockSubscribeInstant(final long when, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_instant",
        Integer.class,
        when,
        outPollable);
  }

  /**
   * Subscribes to the monotonic clock for a duration.
   *
   * @param duration the duration to wait (nanoseconds)
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockSubscribeDuration(
      final long duration, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_duration",
        Integer.class,
        duration,
        outPollable);
  }

  /**
   * Gets the current wall clock time.
   *
   * @param outSeconds pointer to store the seconds since Unix epoch (u64)
   * @param outNanoseconds pointer to store the nanoseconds within the second (u32)
   * @return 0 on success, non-zero on failure
   */
  public int wasiWallClockNow(final MemorySegment outSeconds, final MemorySegment outNanoseconds) {
    validatePointer(outSeconds, "outSeconds");
    validatePointer(outNanoseconds, "outNanoseconds");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_wall_clock_now", Integer.class, outSeconds, outNanoseconds);
  }

  /**
   * Gets the wall clock resolution.
   *
   * @param outSeconds pointer to store the seconds component (u64)
   * @param outNanoseconds pointer to store the nanoseconds component (u32)
   * @return 0 on success, non-zero on failure
   */
  public int wasiWallClockResolution(
      final MemorySegment outSeconds, final MemorySegment outNanoseconds) {
    validatePointer(outSeconds, "outSeconds");
    validatePointer(outNanoseconds, "outNanoseconds");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_wall_clock_resolution", Integer.class, outSeconds, outNanoseconds);
  }

  // ===== WASI Preview 2 Random Functions =====

  /**
   * Generates cryptographically secure random bytes.
   *
   * @param buffer pointer to the buffer to fill with random bytes
   * @param length number of random bytes to generate
   * @param outActualLength pointer to store the actual number of bytes generated (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiRandomGetBytes(
      final MemorySegment buffer, final long length, final MemorySegment outActualLength) {
    validatePointer(buffer, "buffer");
    validatePointer(outActualLength, "outActualLength");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_random_get_bytes", Integer.class, buffer, length, outActualLength);
  }

  /**
   * Generates a cryptographically secure random 64-bit value.
   *
   * @param outValue pointer to store the random value (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiRandomGetU64(final MemorySegment outValue) {
    validatePointer(outValue, "outValue");
    return callNativeFunction("wasmtime4j_panama_wasi_random_get_u64", Integer.class, outValue);
  }

  /**
   * Frees a buffer allocated by wasiRandomGetBytes.
   *
   * @param buffer pointer to the buffer to free
   */
  public void wasiRandomFreeBuffer(final MemorySegment buffer) {
    callNativeFunction("wasmtime4j_panama_wasi_random_free_buffer", Void.class, buffer);
  }

  // ===== WASI Preview 2 TCP Socket Functions =====

  /**
   * Creates a new TCP socket.
   *
   * @param isIpv6 true for IPv6, false for IPv4
   * @param outHandle pointer to store the socket handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketCreate(final boolean isIpv6, final MemorySegment outHandle) {
    validatePointer(outHandle, "outHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_create", Integer.class, isIpv6 ? 1 : 0, outHandle);
  }

  /**
   * Starts binding a TCP socket to an address.
   *
   * @param socketHandle the socket handle
   * @param address the address to bind to (C string)
   * @param port the port to bind to
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketStartBind(
      final long socketHandle, final MemorySegment address, final int port) {
    validatePointer(address, "address");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_start_bind", Integer.class, socketHandle, address, port);
  }

  /**
   * Finishes binding a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketFinishBind(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_finish_bind", Integer.class, socketHandle);
  }

  /**
   * Starts connecting a TCP socket to an address.
   *
   * @param socketHandle the socket handle
   * @param address the address to connect to (C string)
   * @param port the port to connect to
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketStartConnect(
      final long socketHandle, final MemorySegment address, final int port) {
    validatePointer(address, "address");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_start_connect",
        Integer.class,
        socketHandle,
        address,
        port);
  }

  /**
   * Finishes connecting a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outInputStream pointer to store the input stream handle (u64)
   * @param outOutputStream pointer to store the output stream handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketFinishConnect(
      final long socketHandle,
      final MemorySegment outInputStream,
      final MemorySegment outOutputStream) {
    validatePointer(outInputStream, "outInputStream");
    validatePointer(outOutputStream, "outOutputStream");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_finish_connect",
        Integer.class,
        socketHandle,
        outInputStream,
        outOutputStream);
  }

  /**
   * Starts listening on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketStartListen(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_start_listen", Integer.class, socketHandle);
  }

  /**
   * Finishes listening on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketFinishListen(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_finish_listen", Integer.class, socketHandle);
  }

  /**
   * Accepts an incoming connection on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outClientSocket pointer to store the client socket handle (u64)
   * @param outInputStream pointer to store the input stream handle (u64)
   * @param outOutputStream pointer to store the output stream handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketAccept(
      final long socketHandle,
      final MemorySegment outClientSocket,
      final MemorySegment outInputStream,
      final MemorySegment outOutputStream) {
    validatePointer(outClientSocket, "outClientSocket");
    validatePointer(outInputStream, "outInputStream");
    validatePointer(outOutputStream, "outOutputStream");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_accept",
        Integer.class,
        socketHandle,
        outClientSocket,
        outInputStream,
        outOutputStream);
  }

  /**
   * Gets the local address of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketLocalAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_local_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the remote address of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketRemoteAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_remote_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the address family of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outFamily pointer to store the family (0=IPv4, 1=IPv6)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketAddressFamily(final long socketHandle, final MemorySegment outFamily) {
    validatePointer(outFamily, "outFamily");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_address_family", Integer.class, socketHandle, outFamily);
  }

  /**
   * Sets the listen backlog size for a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param backlogSize the backlog size
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetListenBacklogSize(final long socketHandle, final long backlogSize) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_listen_backlog_size",
        Integer.class,
        socketHandle,
        backlogSize);
  }

  /**
   * Enables or disables keep-alive on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param enabled true to enable, false to disable
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveEnabled(final long socketHandle, final boolean enabled) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_enabled",
        Integer.class,
        socketHandle,
        enabled ? 1 : 0);
  }

  /**
   * Sets the keep-alive idle time on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param idleTime the idle time in nanoseconds
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveIdleTime(final long socketHandle, final long idleTime) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_idle_time",
        Integer.class,
        socketHandle,
        idleTime);
  }

  /**
   * Sets the keep-alive interval on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param interval the interval in nanoseconds
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveInterval(final long socketHandle, final long interval) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_interval",
        Integer.class,
        socketHandle,
        interval);
  }

  /**
   * Sets the keep-alive probe count on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param count the probe count
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveCount(final long socketHandle, final int count) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_count",
        Integer.class,
        socketHandle,
        count);
  }

  /**
   * Sets the hop limit (TTL) on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param hopLimit the hop limit
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetHopLimit(final long socketHandle, final int hopLimit) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_hop_limit", Integer.class, socketHandle, hopLimit);
  }

  /**
   * Gets the receive buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketReceiveBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_receive_buffer_size",
        Integer.class,
        socketHandle,
        outSize);
  }

  /**
   * Sets the receive buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetReceiveBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_receive_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Gets the send buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSendBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_send_buffer_size", Integer.class, socketHandle, outSize);
  }

  /**
   * Sets the send buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetSendBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_send_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Creates a pollable for a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSubscribe(final long socketHandle, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_subscribe", Integer.class, socketHandle, outPollable);
  }

  /**
   * Shuts down a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param shutdownType 0=receive, 1=send, 2=both
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketShutdown(final long socketHandle, final int shutdownType) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_shutdown", Integer.class, socketHandle, shutdownType);
  }

  /**
   * Closes a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketClose(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_close", Integer.class, socketHandle);
  }

  // ===== WASI Preview 2 UDP Socket Functions =====

  /**
   * Creates a new UDP socket.
   *
   * @param isIpv6 true for IPv6, false for IPv4
   * @param outHandle pointer to store the socket handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketCreate(final boolean isIpv6, final MemorySegment outHandle) {
    validatePointer(outHandle, "outHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_create", Integer.class, isIpv6 ? 1 : 0, outHandle);
  }

  /**
   * Starts binding a UDP socket to an address.
   *
   * @param socketHandle the socket handle
   * @param address the address to bind to (C string)
   * @param port the port to bind to
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketStartBind(
      final long socketHandle, final MemorySegment address, final int port) {
    validatePointer(address, "address");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_start_bind", Integer.class, socketHandle, address, port);
  }

  /**
   * Finishes binding a UDP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketFinishBind(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_finish_bind", Integer.class, socketHandle);
  }

  /**
   * Sets the remote address for a connected UDP socket.
   *
   * @param socketHandle the socket handle
   * @param remoteAddress the remote address (C string, or NULL)
   * @param remotePort the remote port (-1 to clear)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketStream(
      final long socketHandle, final MemorySegment remoteAddress, final int remotePort) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_stream",
        Integer.class,
        socketHandle,
        remoteAddress,
        remotePort);
  }

  /**
   * Gets the local address of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketLocalAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_local_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the remote address of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketRemoteAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_remote_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the address family of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outFamily pointer to store the family (0=IPv4, 1=IPv6)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketAddressFamily(final long socketHandle, final MemorySegment outFamily) {
    validatePointer(outFamily, "outFamily");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_address_family", Integer.class, socketHandle, outFamily);
  }

  /**
   * Sets the unicast hop limit (TTL) on a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param hopLimit the hop limit
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSetUnicastHopLimit(final long socketHandle, final int hopLimit) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_set_unicast_hop_limit",
        Integer.class,
        socketHandle,
        hopLimit);
  }

  /**
   * Gets the receive buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketReceiveBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_receive_buffer_size",
        Integer.class,
        socketHandle,
        outSize);
  }

  /**
   * Sets the receive buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSetReceiveBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_set_receive_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Gets the send buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSendBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_send_buffer_size", Integer.class, socketHandle, outSize);
  }

  /**
   * Sets the send buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSetSendBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_set_send_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Creates a pollable for a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSubscribe(final long socketHandle, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_subscribe", Integer.class, socketHandle, outPollable);
  }

  /**
   * Receives datagrams from a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param maxDatagrams maximum number of datagrams to receive
   * @param outDatagrams buffer to store the datagrams
   * @param bufferSize size of the buffer
   * @param outCount pointer to store the number of datagrams received (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketReceive(
      final long socketHandle,
      final long maxDatagrams,
      final MemorySegment outDatagrams,
      final long bufferSize,
      final MemorySegment outCount) {
    validatePointer(outDatagrams, "outDatagrams");
    validatePointer(outCount, "outCount");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_receive",
        Integer.class,
        socketHandle,
        maxDatagrams,
        outDatagrams,
        bufferSize,
        outCount);
  }

  /**
   * Sends datagrams on a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param datagrams buffer containing the datagrams to send
   * @param count number of datagrams to send
   * @param outSentCount pointer to store the number of datagrams sent (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSend(
      final long socketHandle,
      final MemorySegment datagrams,
      final long count,
      final MemorySegment outSentCount) {
    validatePointer(datagrams, "datagrams");
    validatePointer(outSentCount, "outSentCount");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_send",
        Integer.class,
        socketHandle,
        datagrams,
        count,
        outSentCount);
  }

  /**
   * Closes a UDP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketClose(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_close", Integer.class, socketHandle);
  }

  // Thread-Local Storage (TLS) Functions

  /**
   * Puts an integer value into thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param value the integer value to store
   * @return 0 on success, non-zero on failure
   */
  public int threadPutInt(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final int value) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    return callNativeFunction(
        "wasmtime4j_thread_put_int", Integer.class, threadHandle, keyPtr, value);
  }

  /**
   * Gets an integer value from thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param outValue pointer to store the retrieved value
   * @return 0 on success, non-zero on failure
   */
  public int threadGetInt(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final MemorySegment outValue) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(outValue, "outValue");
    return callNativeFunction(
        "wasmtime4j_thread_get_int", Integer.class, threadHandle, keyPtr, outValue);
  }

  /**
   * Checks if a thread-local key exists.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param outExists pointer to store the existence flag (1 = exists, 0 = not exists)
   * @return 0 on success, non-zero on failure
   */
  public int threadContainsKey(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final MemorySegment outExists) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(outExists, "outExists");
    return callNativeFunction(
        "wasmtime4j_thread_contains_key", Integer.class, threadHandle, keyPtr, outExists);
  }

  /**
   * Removes a thread-local key.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @return 0 on success, non-zero on failure
   */
  public int threadRemoveKey(final MemorySegment threadHandle, final MemorySegment keyPtr) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    return callNativeFunction("wasmtime4j_thread_remove_key", Integer.class, threadHandle, keyPtr);
  }

  /**
   * Clears all thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @return 0 on success, non-zero on failure
   */
  public int threadClearStorage(final MemorySegment threadHandle) {
    validatePointer(threadHandle, "threadHandle");
    return callNativeFunction("wasmtime4j_thread_clear_storage", Integer.class, threadHandle);
  }

  /**
   * Gets the size of thread-local storage (number of keys).
   *
   * @param threadHandle pointer to the thread
   * @param outSize pointer to store the size
   * @return 0 on success, non-zero on failure
   */
  public int threadStorageSize(final MemorySegment threadHandle, final MemorySegment outSize) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_thread_storage_size", Integer.class, threadHandle, outSize);
  }

  /**
   * Gets the memory usage of thread-local storage in bytes.
   *
   * @param threadHandle pointer to the thread
   * @param outMemoryUsage pointer to store the memory usage (i64)
   * @return 0 on success, non-zero on failure
   */
  public int threadStorageMemoryUsage(
      final MemorySegment threadHandle, final MemorySegment outMemoryUsage) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(outMemoryUsage, "outMemoryUsage");
    return callNativeFunction(
        "wasmtime4j_thread_storage_memory_usage", Integer.class, threadHandle, outMemoryUsage);
  }

  // Additional TLS data type wrapper methods

  /**
   * Puts a long value into thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param value the long value to store
   * @return 0 on success, non-zero on failure
   */
  public int threadPutLong(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final long value) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    return callNativeFunction(
        "wasmtime4j_thread_put_long", Integer.class, threadHandle, keyPtr, value);
  }

  /**
   * Gets a long value from thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param outValue pointer to store the retrieved value
   * @return 0 on success, non-zero on failure
   */
  public int threadGetLong(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final MemorySegment outValue) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(outValue, "outValue");
    return callNativeFunction(
        "wasmtime4j_thread_get_long", Integer.class, threadHandle, keyPtr, outValue);
  }

  /**
   * Puts a float value into thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param value the float value to store
   * @return 0 on success, non-zero on failure
   */
  public int threadPutFloat(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final float value) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    return callNativeFunction(
        "wasmtime4j_thread_put_float", Integer.class, threadHandle, keyPtr, value);
  }

  /**
   * Gets a float value from thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param outValue pointer to store the retrieved value
   * @return 0 on success, non-zero on failure
   */
  public int threadGetFloat(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final MemorySegment outValue) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(outValue, "outValue");
    return callNativeFunction(
        "wasmtime4j_thread_get_float", Integer.class, threadHandle, keyPtr, outValue);
  }

  /**
   * Puts a double value into thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param value the double value to store
   * @return 0 on success, non-zero on failure
   */
  public int threadPutDouble(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final double value) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    return callNativeFunction(
        "wasmtime4j_thread_put_double", Integer.class, threadHandle, keyPtr, value);
  }

  /**
   * Gets a double value from thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param outValue pointer to store the retrieved value
   * @return 0 on success, non-zero on failure
   */
  public int threadGetDouble(
      final MemorySegment threadHandle, final MemorySegment keyPtr, final MemorySegment outValue) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(outValue, "outValue");
    return callNativeFunction(
        "wasmtime4j_thread_get_double", Integer.class, threadHandle, keyPtr, outValue);
  }

  /**
   * Puts a byte array into thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param bytesPtr pointer to the byte array
   * @param bytesLen length of the byte array
   * @return 0 on success, non-zero on failure
   */
  public int threadPutBytes(
      final MemorySegment threadHandle,
      final MemorySegment keyPtr,
      final MemorySegment bytesPtr,
      final long bytesLen) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(bytesPtr, "bytesPtr");
    return callNativeFunction(
        "wasmtime4j_thread_put_bytes", Integer.class, threadHandle, keyPtr, bytesPtr, bytesLen);
  }

  /**
   * Gets a byte array from thread-local storage.
   *
   * @param threadHandle pointer to the thread
   * @param keyPtr pointer to the key C string
   * @param outBytesPtr pointer to store the byte array pointer
   * @param outBytesLen pointer to store the byte array length
   * @return 0 on success, non-zero on failure
   */
  public int threadGetBytes(
      final MemorySegment threadHandle,
      final MemorySegment keyPtr,
      final MemorySegment outBytesPtr,
      final MemorySegment outBytesLen) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(outBytesPtr, "outBytesPtr");
    validatePointer(outBytesLen, "outBytesLen");
    return callNativeFunction(
        "wasmtime4j_thread_get_bytes",
        Integer.class,
        threadHandle,
        keyPtr,
        outBytesPtr,
        outBytesLen);
  }

  // Thread Lifecycle Functions

  /**
   * Joins a thread and waits for it to complete.
   *
   * @param threadHandle pointer to the thread
   * @return 0 on success, non-zero on failure
   */
  public int threadJoin(final MemorySegment threadHandle) {
    validatePointer(threadHandle, "threadHandle");
    return callNativeFunction("wasmtime4j_thread_join", Integer.class, threadHandle);
  }

  /**
   * Joins a thread with a timeout.
   *
   * @param threadHandle pointer to the thread
   * @param timeoutMs timeout in milliseconds
   * @param outJoined pointer to store whether thread joined (1) or timed out (0)
   * @return 0 on success, non-zero on failure
   */
  public int threadJoinTimeout(
      final MemorySegment threadHandle, final long timeoutMs, final MemorySegment outJoined) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(outJoined, "outJoined");
    return callNativeFunction(
        "wasmtime4j_thread_join_timeout", Integer.class, threadHandle, timeoutMs, outJoined);
  }

  /**
   * Requests graceful thread termination.
   *
   * @param threadHandle pointer to the thread
   * @return 0 on success, non-zero on failure
   */
  public int threadRequestTermination(final MemorySegment threadHandle) {
    validatePointer(threadHandle, "threadHandle");
    return callNativeFunction("wasmtime4j_thread_request_termination", Integer.class, threadHandle);
  }

  /**
   * Forces thread termination.
   *
   * @param threadHandle pointer to the thread
   * @return 0 on success, non-zero on failure
   */
  public int threadForceTerminate(final MemorySegment threadHandle) {
    validatePointer(threadHandle, "threadHandle");
    return callNativeFunction("wasmtime4j_thread_force_terminate", Integer.class, threadHandle);
  }

  /**
   * Checks if thread termination has been requested.
   *
   * @param threadHandle pointer to the thread
   * @param outRequested pointer to store termination request flag (1 = requested, 0 = not
   *     requested)
   * @return 0 on success, non-zero on failure
   */
  public int threadIsTerminationRequested(
      final MemorySegment threadHandle, final MemorySegment outRequested) {
    validatePointer(threadHandle, "threadHandle");
    validatePointer(outRequested, "outRequested");
    return callNativeFunction(
        "wasmtime4j_thread_is_termination_requested", Integer.class, threadHandle, outRequested);
  }

  // Memory Management Functions

  /**
   * Frees memory allocated by native functions.
   *
   * @param ptr the memory pointer to free
   */
  public void free(final MemorySegment ptr) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_free", Void.class, ptr);
    }
  }

  // WIT Parser Functions

  /**
   * Creates a new WIT parser.
   *
   * @return pointer to the WIT parser, or NULL on failure
   */
  public MemorySegment witParserNew() {
    return callNativeFunction("wasmtime4j_wit_parser_new", MemorySegment.class);
  }

  /**
   * Destroys a WIT parser.
   *
   * @param parserPtr pointer to the parser
   */
  public void witParserDestroy(final MemorySegment parserPtr) {
    if (parserPtr != null && !parserPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_wit_parser_destroy", Void.class, parserPtr);
    }
  }

  /**
   * Parses a WIT interface definition.
   *
   * @param parserPtr pointer to the parser
   * @param witText the WIT text to parse
   * @param resultPtr output pointer for parsed interface JSON
   * @return 0 on success, non-zero on failure
   */
  public int witParserParseInterface(
      final MemorySegment parserPtr, final MemorySegment witText, final MemorySegment resultPtr) {
    validatePointer(parserPtr, "parserPtr");
    validatePointer(witText, "witText");
    validatePointer(resultPtr, "resultPtr");
    return callNativeFunction(
        "wasmtime4j_wit_parser_parse_interface", Integer.class, parserPtr, witText, resultPtr);
  }

  /**
   * Validates WIT syntax.
   *
   * @param parserPtr pointer to the parser
   * @param witText the WIT text to validate
   * @param validPtr output pointer for validation result (1 = valid, 0 = invalid)
   * @return 0 on success, non-zero on failure
   */
  public int witParserValidateSyntax(
      final MemorySegment parserPtr, final MemorySegment witText, final MemorySegment validPtr) {
    validatePointer(parserPtr, "parserPtr");
    validatePointer(witText, "witText");
    validatePointer(validPtr, "validPtr");
    return callNativeFunction(
        "wasmtime4j_wit_parser_validate_syntax", Integer.class, parserPtr, witText, validPtr);
  }

  // ===== Performance Profiler Functions =====

  /**
   * Creates a new performance profiler with default configuration.
   *
   * @return pointer to the profiler, or NULL on failure
   */
  public MemorySegment profilerCreate() {
    return callNativeFunction("wasmtime4j_profiler_create", MemorySegment.class);
  }

  /**
   * Starts profiling.
   *
   * @param profilerHandle pointer to the profiler
   * @return true on success, false on failure
   */
  public boolean profilerStart(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_start", Boolean.class, profilerHandle);
  }

  /**
   * Stops profiling.
   *
   * @param profilerHandle pointer to the profiler
   * @return true on success, false on failure
   */
  public boolean profilerStop(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_stop", Boolean.class, profilerHandle);
  }

  /**
   * Destroys a performance profiler.
   *
   * @param profilerHandle pointer to the profiler
   */
  public void profilerDestroy(final MemorySegment profilerHandle) {
    if (profilerHandle != null && !profilerHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_profiler_destroy", Void.class, profilerHandle);
    }
  }

  /**
   * Gets the number of modules compiled.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of modules compiled
   */
  public long profilerGetModulesCompiled(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_modules_compiled", Long.class, profilerHandle);
  }

  /**
   * Gets total compilation time in nanoseconds.
   *
   * @param profilerHandle pointer to the profiler
   * @return total compilation time in nanoseconds
   */
  public long profilerGetTotalCompilationTimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_total_compilation_time_nanos", Long.class, profilerHandle);
  }

  /**
   * Gets average compilation time in nanoseconds.
   *
   * @param profilerHandle pointer to the profiler
   * @return average compilation time in nanoseconds
   */
  public long profilerGetAverageCompilationTimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_average_compilation_time_nanos", Long.class, profilerHandle);
  }

  /**
   * Gets total bytes of WASM bytecode compiled.
   *
   * @param profilerHandle pointer to the profiler
   * @return total bytes compiled
   */
  public long profilerGetBytesCompiled(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_bytes_compiled", Long.class, profilerHandle);
  }

  /**
   * Gets compilation cache hits.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of cache hits
   */
  public long profilerGetCacheHits(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_cache_hits", Long.class, profilerHandle);
  }

  /**
   * Gets compilation cache misses.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of cache misses
   */
  public long profilerGetCacheMisses(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_cache_misses", Long.class, profilerHandle);
  }

  /**
   * Gets number of optimized modules.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of optimized modules
   */
  public long profilerGetOptimizedModules(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_optimized_modules", Long.class, profilerHandle);
  }

  /**
   * Gets current memory usage in bytes.
   *
   * @param profilerHandle pointer to the profiler
   * @return current memory usage in bytes
   */
  public long profilerGetCurrentMemoryBytes(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_current_memory_bytes", Long.class, profilerHandle);
  }

  /**
   * Gets peak memory usage in bytes.
   *
   * @param profilerHandle pointer to the profiler
   * @return peak memory usage in bytes
   */
  public long profilerGetPeakMemoryBytes(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_peak_memory_bytes", Long.class, profilerHandle);
  }

  /**
   * Gets profiler uptime in nanoseconds.
   *
   * @param profilerHandle pointer to the profiler
   * @return uptime in nanoseconds
   */
  public long profilerGetUptimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_uptime_nanos", Long.class, profilerHandle);
  }

  /**
   * Gets function calls per second.
   *
   * @param profilerHandle pointer to the profiler
   * @return function calls per second
   */
  public double profilerGetFunctionCallsPerSecond(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_function_calls_per_second", Double.class, profilerHandle);
  }

  /**
   * Gets total function call count across all profiled functions.
   *
   * @param profilerHandle pointer to the profiler
   * @return total function calls
   */
  public long profilerGetTotalFunctionCalls(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_total_function_calls", Long.class, profilerHandle);
  }

  /**
   * Gets total execution time in nanoseconds across all profiled functions.
   *
   * @param profilerHandle pointer to the profiler
   * @return total execution time in nanoseconds
   */
  public long profilerGetTotalExecutionTimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_total_execution_time_nanos", Long.class, profilerHandle);
  }

  /**
   * Records a module compilation.
   *
   * @param profilerHandle pointer to the profiler
   * @param compilationTimeNanos compilation time in nanoseconds
   * @param bytecodeSize bytecode size in bytes
   * @param cached whether the compilation was from cache
   * @param optimized whether the module was optimized
   * @return true on success, false on failure
   */
  public boolean profilerRecordCompilation(
      final MemorySegment profilerHandle,
      final long compilationTimeNanos,
      final long bytecodeSize,
      final boolean cached,
      final boolean optimized) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_record_compilation",
        Boolean.class,
        profilerHandle,
        compilationTimeNanos,
        bytecodeSize,
        cached,
        optimized);
  }

  /**
   * Records a function execution.
   *
   * @param profilerHandle pointer to the profiler
   * @param functionName the function name
   * @param executionTimeNanos execution time in nanoseconds
   * @param memoryDelta memory delta in bytes (positive for allocation, negative for deallocation)
   * @return true on success, false on failure
   */
  public boolean profilerRecordFunction(
      final MemorySegment profilerHandle,
      final MemorySegment functionName,
      final long executionTimeNanos,
      final long memoryDelta) {
    validatePointer(profilerHandle, "profilerHandle");
    validatePointer(functionName, "functionName");
    return callNativeFunction(
        "wasmtime4j_profiler_record_function",
        Boolean.class,
        profilerHandle,
        functionName,
        executionTimeNanos,
        memoryDelta);
  }

  /**
   * Resets all profiler statistics.
   *
   * @param profilerHandle pointer to the profiler
   * @return true on success, false on failure
   */
  public boolean profilerReset(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_reset", Boolean.class, profilerHandle);
  }

  /**
   * Checks if profiling is currently active.
   *
   * @param profilerHandle pointer to the profiler
   * @return true if profiling is active
   */
  public boolean profilerIsProfiling(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_is_profiling", Boolean.class, profilerHandle);
  }

  // ====================================================================================
  // Flame Graph Collector Functions
  // ====================================================================================

  /**
   * Creates a new flame graph collector for performance profiling visualization.
   *
   * @param maxSamples maximum number of samples to collect
   * @param samplingIntervalMs sampling interval in milliseconds
   * @return pointer to the flame graph collector, or null on failure
   */
  public MemorySegment flameGraphCollectorCreate(
      final long maxSamples, final long samplingIntervalMs) {
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_create",
        MemorySegment.class,
        maxSamples,
        samplingIntervalMs);
  }

  /**
   * Starts flame graph collection.
   *
   * @param collectorHandle pointer to the flame graph collector
   * @return true on success, false on failure
   */
  public boolean flameGraphCollectorStart(final MemorySegment collectorHandle) {
    validatePointer(collectorHandle, "collectorHandle");
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_start", Boolean.class, collectorHandle);
  }

  /**
   * Records a stack trace sample in the flame graph collector.
   *
   * @param collectorHandle pointer to the flame graph collector
   * @param functionName pointer to the function name C string
   * @param fileName pointer to the file name C string
   * @param lineNumber line number in the source file
   * @param durationNanos execution duration in nanoseconds
   * @return true on success, false on failure
   */
  public boolean flameGraphCollectorRecordSample(
      final MemorySegment collectorHandle,
      final MemorySegment functionName,
      final MemorySegment fileName,
      final int lineNumber,
      final long durationNanos) {
    validatePointer(collectorHandle, "collectorHandle");
    validatePointer(functionName, "functionName");
    validatePointer(fileName, "fileName");
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_record_sample",
        Boolean.class,
        collectorHandle,
        functionName,
        fileName,
        lineNumber,
        durationNanos);
  }

  /**
   * Exports the flame graph as an SVG image.
   *
   * @param collectorHandle pointer to the flame graph collector
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @param outputBuffer pointer to the output buffer for the SVG data
   * @param bufferSize size of the output buffer in bytes
   * @return true on success, false on failure (e.g., buffer too small)
   */
  public boolean flameGraphCollectorExportSvg(
      final MemorySegment collectorHandle,
      final int width,
      final int height,
      final MemorySegment outputBuffer,
      final long bufferSize) {
    validatePointer(collectorHandle, "collectorHandle");
    validatePointer(outputBuffer, "outputBuffer");
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_export_svg",
        Boolean.class,
        collectorHandle,
        width,
        height,
        outputBuffer,
        bufferSize);
  }

  /**
   * Destroys a flame graph collector and frees associated resources.
   *
   * @param collectorHandle pointer to the flame graph collector to destroy
   */
  public void flameGraphCollectorDestroy(final MemorySegment collectorHandle) {
    if (collectorHandle != null && !collectorHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_flame_graph_collector_destroy", Void.class, collectorHandle);
    }
  }

  // ====================================================================================
  // WASI HTTP Functions
  // ====================================================================================

  /**
   * Creates a new WASI HTTP config builder.
   *
   * @return pointer to the config builder, or null on failure
   */
  public MemorySegment wasiHttpConfigBuilderNew() {
    return callNativeFunction("wasi_http_config_builder_new", MemorySegment.class);
  }

  /**
   * Adds an allowed host to the config builder.
   *
   * @param builderPtr pointer to the config builder
   * @param hostPtr pointer to the host string
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderAllowHost(
      final MemorySegment builderPtr, final MemorySegment hostPtr) {
    validatePointer(builderPtr, "builderPtr");
    validatePointer(hostPtr, "hostPtr");
    return callNativeFunction(
        "wasi_http_config_builder_allow_host", Integer.class, builderPtr, hostPtr);
  }

  /**
   * Adds a blocked host to the config builder.
   *
   * @param builderPtr pointer to the config builder
   * @param hostPtr pointer to the host string
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderBlockHost(
      final MemorySegment builderPtr, final MemorySegment hostPtr) {
    validatePointer(builderPtr, "builderPtr");
    validatePointer(hostPtr, "hostPtr");
    return callNativeFunction(
        "wasi_http_config_builder_block_host", Integer.class, builderPtr, hostPtr);
  }

  /**
   * Sets whether all hosts are allowed.
   *
   * @param builderPtr pointer to the config builder
   * @param allow true to allow all hosts
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderAllowAllHosts(
      final MemorySegment builderPtr, final boolean allow) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_allow_all_hosts", Integer.class, builderPtr, allow);
  }

  /**
   * Sets the connect timeout in milliseconds.
   *
   * @param builderPtr pointer to the config builder
   * @param timeoutMs timeout in milliseconds
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetConnectTimeout(
      final MemorySegment builderPtr, final long timeoutMs) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_connect_timeout", Integer.class, builderPtr, timeoutMs);
  }

  /**
   * Sets the read timeout in milliseconds.
   *
   * @param builderPtr pointer to the config builder
   * @param timeoutMs timeout in milliseconds
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetReadTimeout(
      final MemorySegment builderPtr, final long timeoutMs) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_read_timeout", Integer.class, builderPtr, timeoutMs);
  }

  /**
   * Sets the write timeout in milliseconds.
   *
   * @param builderPtr pointer to the config builder
   * @param timeoutMs timeout in milliseconds
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetWriteTimeout(
      final MemorySegment builderPtr, final long timeoutMs) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_write_timeout", Integer.class, builderPtr, timeoutMs);
  }

  /**
   * Sets the maximum number of connections.
   *
   * @param builderPtr pointer to the config builder
   * @param maxConnections maximum connections
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxConnections(
      final MemorySegment builderPtr, final int maxConnections) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_max_connections", Integer.class, builderPtr, maxConnections);
  }

  /**
   * Sets the maximum connections per host.
   *
   * @param builderPtr pointer to the config builder
   * @param maxConnectionsPerHost maximum connections per host
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxConnectionsPerHost(
      final MemorySegment builderPtr, final int maxConnectionsPerHost) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_max_connections_per_host",
        Integer.class,
        builderPtr,
        maxConnectionsPerHost);
  }

  /**
   * Sets the maximum request body size.
   *
   * @param builderPtr pointer to the config builder
   * @param maxSize maximum size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxRequestBodySize(
      final MemorySegment builderPtr, final long maxSize) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_max_request_body_size", Integer.class, builderPtr, maxSize);
  }

  /**
   * Sets the maximum response body size.
   *
   * @param builderPtr pointer to the config builder
   * @param maxSize maximum size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxResponseBodySize(
      final MemorySegment builderPtr, final long maxSize) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_max_response_body_size", Integer.class, builderPtr, maxSize);
  }

  /**
   * Sets whether HTTPS is required.
   *
   * @param builderPtr pointer to the config builder
   * @param required true to require HTTPS
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetHttpsRequired(
      final MemorySegment builderPtr, final boolean required) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_https_required", Integer.class, builderPtr, required);
  }

  /**
   * Sets whether certificate validation is enabled.
   *
   * @param builderPtr pointer to the config builder
   * @param enabled true to enable certificate validation
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetCertificateValidation(
      final MemorySegment builderPtr, final boolean enabled) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_certificate_validation", Integer.class, builderPtr, enabled);
  }

  /**
   * Sets whether HTTP/2 is enabled.
   *
   * @param builderPtr pointer to the config builder
   * @param enabled true to enable HTTP/2
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetHttp2Enabled(
      final MemorySegment builderPtr, final boolean enabled) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_http2_enabled", Integer.class, builderPtr, enabled);
  }

  /**
   * Sets whether connection pooling is enabled.
   *
   * @param builderPtr pointer to the config builder
   * @param enabled true to enable connection pooling
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetConnectionPooling(
      final MemorySegment builderPtr, final boolean enabled) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_connection_pooling", Integer.class, builderPtr, enabled);
  }

  /**
   * Sets whether to follow redirects.
   *
   * @param builderPtr pointer to the config builder
   * @param follow true to follow redirects
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetFollowRedirects(
      final MemorySegment builderPtr, final boolean follow) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_follow_redirects", Integer.class, builderPtr, follow);
  }

  /**
   * Sets the maximum number of redirects to follow.
   *
   * @param builderPtr pointer to the config builder
   * @param maxRedirects maximum redirects
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxRedirects(
      final MemorySegment builderPtr, final int maxRedirects) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_max_redirects", Integer.class, builderPtr, maxRedirects);
  }

  /**
   * Sets the user agent string.
   *
   * @param builderPtr pointer to the config builder
   * @param userAgentPtr pointer to the user agent string
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetUserAgent(
      final MemorySegment builderPtr, final MemorySegment userAgentPtr) {
    validatePointer(builderPtr, "builderPtr");
    validatePointer(userAgentPtr, "userAgentPtr");
    return callNativeFunction(
        "wasi_http_config_builder_set_user_agent", Integer.class, builderPtr, userAgentPtr);
  }

  /**
   * Builds a WASI HTTP config from the builder.
   *
   * @param builderPtr pointer to the config builder
   * @return pointer to the config, or null on failure
   */
  public MemorySegment wasiHttpConfigBuilderBuild(final MemorySegment builderPtr) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction("wasi_http_config_builder_build", MemorySegment.class, builderPtr);
  }

  /**
   * Frees a WASI HTTP config builder.
   *
   * @param builderPtr pointer to the config builder
   */
  public void wasiHttpConfigBuilderFree(final MemorySegment builderPtr) {
    if (builderPtr != null && !builderPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasi_http_config_builder_free", Void.class, builderPtr);
    }
  }

  /**
   * Creates a default WASI HTTP config.
   *
   * @return pointer to the config, or null on failure
   */
  public MemorySegment wasiHttpConfigDefault() {
    return callNativeFunction("wasi_http_config_default", MemorySegment.class);
  }

  /**
   * Frees a WASI HTTP config.
   *
   * @param configPtr pointer to the config
   */
  public void wasiHttpConfigFree(final MemorySegment configPtr) {
    if (configPtr != null && !configPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasi_http_config_free", Void.class, configPtr);
    }
  }

  /**
   * Creates a new WASI HTTP context with the specified config.
   *
   * @param configPtr pointer to the config
   * @return pointer to the context, or null on failure
   */
  public MemorySegment wasiHttpContextNew(final MemorySegment configPtr) {
    validatePointer(configPtr, "configPtr");
    return callNativeFunction("wasi_http_ctx_new", MemorySegment.class, configPtr);
  }

  /**
   * Creates a new WASI HTTP context with default config.
   *
   * @return pointer to the context, or null on failure
   */
  public MemorySegment wasiHttpContextNewDefault() {
    return callNativeFunction("wasi_http_ctx_new_default", MemorySegment.class);
  }

  /**
   * Gets the ID of a WASI HTTP context.
   *
   * @param contextPtr pointer to the context
   * @return the context ID
   */
  public long wasiHttpContextGetId(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_get_id", Long.class, contextPtr);
  }

  /**
   * Checks if a WASI HTTP context is valid.
   *
   * @param contextPtr pointer to the context
   * @return 1 if valid, 0 otherwise
   */
  public int wasiHttpContextIsValid(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_is_valid", Integer.class, contextPtr);
  }

  /**
   * Checks if a host is allowed by the context.
   *
   * @param contextPtr pointer to the context
   * @param hostPtr pointer to the host string
   * @return 1 if allowed, 0 otherwise
   */
  public int wasiHttpContextIsHostAllowed(
      final MemorySegment contextPtr, final MemorySegment hostPtr) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(hostPtr, "hostPtr");
    return callNativeFunction("wasi_http_ctx_is_host_allowed", Integer.class, contextPtr, hostPtr);
  }

  /**
   * Resets the statistics for a WASI HTTP context.
   *
   * @param contextPtr pointer to the context
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpContextResetStats(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_reset_stats", Integer.class, contextPtr);
  }

  /**
   * Gets the total number of requests.
   *
   * @param contextPtr pointer to the context
   * @return total request count
   */
  public long wasiHttpContextStatsTotalRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_total_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of successful requests.
   *
   * @param contextPtr pointer to the context
   * @return successful request count
   */
  public long wasiHttpContextStatsSuccessfulRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_successful_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of failed requests.
   *
   * @param contextPtr pointer to the context
   * @return failed request count
   */
  public long wasiHttpContextStatsFailedRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_failed_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of active requests.
   *
   * @param contextPtr pointer to the context
   * @return active request count
   */
  public int wasiHttpContextStatsActiveRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_active_requests", Integer.class, contextPtr);
  }

  /**
   * Gets the total bytes sent.
   *
   * @param contextPtr pointer to the context
   * @return total bytes sent
   */
  public long wasiHttpContextStatsBytesSent(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_bytes_sent", Long.class, contextPtr);
  }

  /**
   * Gets the total bytes received.
   *
   * @param contextPtr pointer to the context
   * @return total bytes received
   */
  public long wasiHttpContextStatsBytesReceived(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_bytes_received", Long.class, contextPtr);
  }

  /**
   * Gets the number of connection timeouts.
   *
   * @param contextPtr pointer to the context
   * @return connection timeout count
   */
  public long wasiHttpContextStatsConnectionTimeouts(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_connection_timeouts", Long.class, contextPtr);
  }

  /**
   * Gets the number of read timeouts.
   *
   * @param contextPtr pointer to the context
   * @return read timeout count
   */
  public long wasiHttpContextStatsReadTimeouts(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_read_timeouts", Long.class, contextPtr);
  }

  /**
   * Gets the number of blocked requests.
   *
   * @param contextPtr pointer to the context
   * @return blocked request count
   */
  public long wasiHttpContextStatsBlockedRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_blocked_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of body size violations.
   *
   * @param contextPtr pointer to the context
   * @return body size violation count
   */
  public long wasiHttpContextStatsBodySizeViolations(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_body_size_violations", Long.class, contextPtr);
  }

  /**
   * Gets the number of active connections.
   *
   * @param contextPtr pointer to the context
   * @return active connection count
   */
  public int wasiHttpContextStatsActiveConnections(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_active_connections", Integer.class, contextPtr);
  }

  /**
   * Gets the number of idle connections.
   *
   * @param contextPtr pointer to the context
   * @return idle connection count
   */
  public int wasiHttpContextStatsIdleConnections(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_idle_connections", Integer.class, contextPtr);
  }

  /**
   * Gets the average request duration in milliseconds.
   *
   * @param contextPtr pointer to the context
   * @return average duration in milliseconds
   */
  public long wasiHttpContextStatsAvgDurationMs(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_avg_duration_ms", Long.class, contextPtr);
  }

  /**
   * Gets the minimum request duration in milliseconds.
   *
   * @param contextPtr pointer to the context
   * @return minimum duration in milliseconds
   */
  public long wasiHttpContextStatsMinDurationMs(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_min_duration_ms", Long.class, contextPtr);
  }

  /**
   * Gets the maximum request duration in milliseconds.
   *
   * @param contextPtr pointer to the context
   * @return maximum duration in milliseconds
   */
  public long wasiHttpContextStatsMaxDurationMs(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasi_http_ctx_stats_max_duration_ms", Long.class, contextPtr);
  }

  /**
   * Frees a WASI HTTP context.
   *
   * @param contextPtr pointer to the context
   */
  public void wasiHttpContextFree(final MemorySegment contextPtr) {
    if (contextPtr != null && !contextPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasi_http_ctx_free", Void.class, contextPtr);
    }
  }

  /**
   * Adds WASI HTTP to a linker.
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param httpCtxPtr pointer to the WASI HTTP context
   * @return 0 on success, non-zero error code on failure
   */
  public int wasiHttpAddToLinker(
      final MemorySegment linkerPtr, final MemorySegment storePtr, final MemorySegment httpCtxPtr) {
    if (linkerPtr == null || linkerPtr.equals(MemorySegment.NULL)) {
      return -1;
    }
    if (storePtr == null || storePtr.equals(MemorySegment.NULL)) {
      return -1;
    }
    if (httpCtxPtr == null || httpCtxPtr.equals(MemorySegment.NULL)) {
      return -1;
    }
    return callNativeFunction(
        "wasi_http_add_to_linker", Integer.class, linkerPtr, storePtr, httpCtxPtr);
  }

  /**
   * Checks if WASI HTTP support is available.
   *
   * @return 1 if WASI HTTP is available, 0 otherwise
   */
  public int wasiHttpIsAvailable() {
    return callNativeFunction("wasi_http_is_available", Integer.class);
  }

  // Pooling Allocator Functions

  /**
   * Creates a new pooling allocator with default configuration.
   *
   * @return pointer to the allocator, or null on failure
   */
  public MemorySegment poolingAllocatorCreate() {
    return callNativeFunction("wasmtime4j_pooling_allocator_create", MemorySegment.class);
  }

  /**
   * Creates a new pooling allocator with custom configuration.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @param stackSize stack size for WebAssembly execution
   * @param maxStacks maximum number of stacks
   * @param maxTablesPerInstance maximum tables per instance
   * @param maxTables maximum total tables
   * @param memoryDecommitEnabled whether memory decommit is enabled
   * @param poolWarmingEnabled whether pool warming is enabled
   * @param poolWarmingPercentage pool warming percentage (0.0 to 1.0)
   * @return pointer to the allocator, or null on failure
   */
  public MemorySegment poolingAllocatorCreateWithConfig(
      final int instancePoolSize,
      final long maxMemoryPerInstance,
      final int stackSize,
      final int maxStacks,
      final int maxTablesPerInstance,
      final int maxTables,
      final boolean memoryDecommitEnabled,
      final boolean poolWarmingEnabled,
      final float poolWarmingPercentage) {
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_create_with_config",
        MemorySegment.class,
        (long) instancePoolSize,
        maxMemoryPerInstance,
        stackSize,
        (long) maxStacks,
        maxTablesPerInstance,
        (long) maxTables,
        memoryDecommitEnabled,
        poolWarmingEnabled,
        poolWarmingPercentage);
  }

  /**
   * Allocates an instance from the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceIdOut pointer to store the instance ID
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorAllocateInstance(
      final MemorySegment allocatorPtr, final MemorySegment instanceIdOut) {
    validatePointer(allocatorPtr, "allocatorPtr");
    validatePointer(instanceIdOut, "instanceIdOut");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_allocate_instance",
        Boolean.class,
        allocatorPtr,
        instanceIdOut);
  }

  /**
   * Reuses an existing instance from the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceId the instance ID to reuse
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorReuseInstance(
      final MemorySegment allocatorPtr, final long instanceId) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_reuse_instance", Boolean.class, allocatorPtr, instanceId);
  }

  /**
   * Releases an instance back to the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceId the instance ID to release
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorReleaseInstance(
      final MemorySegment allocatorPtr, final long instanceId) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_release_instance", Boolean.class, allocatorPtr, instanceId);
  }

  /**
   * Gets pool statistics.
   *
   * @param allocatorPtr pointer to the allocator
   * @param statsOut pointer to store the statistics
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorGetStatistics(
      final MemorySegment allocatorPtr, final MemorySegment statsOut) {
    validatePointer(allocatorPtr, "allocatorPtr");
    validatePointer(statsOut, "statsOut");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_get_statistics", Boolean.class, allocatorPtr, statsOut);
  }

  /**
   * Resets pool statistics.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorResetStatistics(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_reset_statistics", Boolean.class, allocatorPtr);
  }

  /**
   * Warms up the pools by pre-allocating resources.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorWarmPools(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_warm_pools", Boolean.class, allocatorPtr);
  }

  /**
   * Performs maintenance operations on the pools.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorPerformMaintenance(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_perform_maintenance", Boolean.class, allocatorPtr);
  }

  /**
   * Destroys a pooling allocator.
   *
   * @param allocatorPtr pointer to the allocator
   */
  public void poolingAllocatorDestroy(final MemorySegment allocatorPtr) {
    if (allocatorPtr != null && !allocatorPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_pooling_allocator_destroy", Void.class, allocatorPtr);
    }
  }

  // ===== Trap Introspection Functions =====

  /** Trap code constants matching Java TrapException.TrapType enum ordinals. */
  public static final class TrapCodes {
    /** Stack overflow trap. */
    public static final int STACK_OVERFLOW = 0;

    /** Memory out of bounds trap. */
    public static final int MEMORY_OUT_OF_BOUNDS = 1;

    /** Heap misaligned trap. */
    public static final int HEAP_MISALIGNED = 2;

    /** Table out of bounds trap. */
    public static final int TABLE_OUT_OF_BOUNDS = 3;

    /** Indirect call to null trap. */
    public static final int INDIRECT_CALL_TO_NULL = 4;

    /** Bad signature trap. */
    public static final int BAD_SIGNATURE = 5;

    /** Integer overflow trap. */
    public static final int INTEGER_OVERFLOW = 6;

    /** Integer division by zero trap. */
    public static final int INTEGER_DIVISION_BY_ZERO = 7;

    /** Bad conversion to integer trap. */
    public static final int BAD_CONVERSION_TO_INTEGER = 8;

    /** Unreachable code reached trap. */
    public static final int UNREACHABLE_CODE_REACHED = 9;

    /** Interrupt trap. */
    public static final int INTERRUPT = 10;

    /** Out of fuel trap. */
    public static final int OUT_OF_FUEL = 11;

    /** Null reference trap. */
    public static final int NULL_REFERENCE = 12;

    /** Array out of bounds trap. */
    public static final int ARRAY_OUT_OF_BOUNDS = 13;

    /** Unknown trap type. */
    public static final int UNKNOWN = 14;

    private TrapCodes() {}
  }

  /**
   * Parses trap code from an error message.
   *
   * @param errorMessage the error message to parse
   * @return trap code matching TrapCodes constants
   */
  public int trapParseCode(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return TrapCodes.UNKNOWN;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      return callNativeFunction("wasmtime4j_panama_trap_parse_code", Integer.class, messageSegment);
    }
  }

  /**
   * Gets the name of a trap code.
   *
   * @param trapCode the trap code
   * @return the trap code name
   */
  public String trapCodeName(final int trapCode) {
    final MemorySegment namePtr =
        callNativeFunction("wasmtime4j_panama_trap_code_name", MemorySegment.class, trapCode);
    if (namePtr == null || namePtr.equals(MemorySegment.NULL)) {
      return "unknown";
    }
    return namePtr.reinterpret(256).getString(0);
  }

  /**
   * Checks if an error message indicates a trap.
   *
   * @param errorMessage the error message to check
   * @return true if the message indicates a trap
   */
  public boolean trapIsTrap(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return false;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      final int result =
          callNativeFunction("wasmtime4j_panama_trap_is_trap", Integer.class, messageSegment);
      return result == 1;
    }
  }

  /**
   * Extracts function name from a backtrace line.
   *
   * @param backtraceLine the backtrace line to parse
   * @return the function name, or null if not found
   */
  public String trapExtractFunctionName(final String backtraceLine) {
    if (backtraceLine == null || backtraceLine.isEmpty()) {
      return null;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment lineSegment = arena.allocateFrom(backtraceLine);
      final int bufferSize = 256;
      final MemorySegment outBuffer = arena.allocate(bufferSize);
      final int bytesWritten =
          callNativeFunction(
              "wasmtime4j_panama_trap_extract_function_name",
              Integer.class,
              lineSegment,
              outBuffer,
              (long) bufferSize);
      if (bytesWritten <= 0) {
        return null;
      }
      return outBuffer.getString(0);
    }
  }

  /**
   * Extracts instruction offset from an error message.
   *
   * @param errorMessage the error message to parse
   * @return the instruction offset, or -1 if not found
   */
  public long trapExtractOffset(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return -1;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      return callNativeFunction(
          "wasmtime4j_panama_trap_extract_offset", Long.class, messageSegment);
    }
  }

  /** Comprehensive trap info structure returned by trapExtractInfo. */
  public static final class TrapInfo {
    private final int trapCode;
    private final long instructionOffset;
    private final boolean isTrap;

    /**
     * Creates a new TrapInfo.
     *
     * @param trapCode the trap code
     * @param instructionOffset the instruction offset
     * @param isTrap whether this is a trap
     */
    public TrapInfo(final int trapCode, final long instructionOffset, final boolean isTrap) {
      this.trapCode = trapCode;
      this.instructionOffset = instructionOffset;
      this.isTrap = isTrap;
    }

    /**
     * Gets the trap code.
     *
     * @return the trap code
     */
    public int getTrapCode() {
      return trapCode;
    }

    /**
     * Gets the instruction offset.
     *
     * @return the instruction offset, or -1 if not available
     */
    public long getInstructionOffset() {
      return instructionOffset;
    }

    /**
     * Checks if this is a trap.
     *
     * @return true if this is a trap
     */
    public boolean isTrap() {
      return isTrap;
    }

    @Override
    public String toString() {
      return "TrapInfo{trapCode="
          + trapCode
          + ", instructionOffset="
          + instructionOffset
          + ", isTrap="
          + isTrap
          + '}';
    }
  }

  /**
   * Extracts comprehensive trap information from an error message.
   *
   * @param errorMessage the error message to parse
   * @return TrapInfo containing trap code, offset, and whether it's a trap
   */
  public TrapInfo trapExtractInfo(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return new TrapInfo(TrapCodes.UNKNOWN, -1, false);
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      // TrapInfo struct layout: int trap_code, long instruction_offset, int is_trap
      // Padded: 4 bytes + 4 padding + 8 bytes + 4 bytes = 20 bytes, aligned to 8 = 24 bytes
      final MemorySegment outInfo = arena.allocate(24, 8);
      final int result =
          callNativeFunction(
              "wasmtime4j_panama_trap_extract_info", Integer.class, messageSegment, outInfo);
      if (result != 0) {
        return new TrapInfo(TrapCodes.UNKNOWN, -1, false);
      }
      final int trapCode = outInfo.get(ValueLayout.JAVA_INT, 0);
      final long instructionOffset = outInfo.get(ValueLayout.JAVA_LONG, 8);
      final int isTrap = outInfo.get(ValueLayout.JAVA_INT, 16);
      return new TrapInfo(trapCode, instructionOffset, isTrap == 1);
    }
  }

  // ==================== Module Cache Methods ====================

  /**
   * Creates a module cache with custom configuration.
   *
   * @param enginePtr pointer to the engine
   * @param cacheDirPtr pointer to cache directory string
   * @param maxCacheSize maximum cache size in bytes
   * @param maxEntries maximum number of entries
   * @param compressionEnabled whether compression is enabled
   * @param compressionLevel compression level (1-22)
   * @return pointer to the module cache, or null on failure
   */
  public MemorySegment moduleCacheCreateWithConfig(
      final MemorySegment enginePtr,
      final MemorySegment cacheDirPtr,
      final long maxCacheSize,
      final int maxEntries,
      final boolean compressionEnabled,
      final int compressionLevel) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_module_cache_create_with_config",
        MemorySegment.class,
        enginePtr,
        cacheDirPtr,
        maxCacheSize,
        (long) maxEntries,
        compressionEnabled ? 1 : 0,
        compressionLevel);
  }

  /**
   * Gets or compiles a module from the cache.
   *
   * @param cachePtr pointer to the module cache
   * @param bytecodePtr pointer to the bytecode
   * @param bytecodeLen length of the bytecode
   * @param moduleOutPtr pointer to store the module pointer
   * @return true on success, false on failure
   */
  public boolean moduleCacheGetOrCompile(
      final MemorySegment cachePtr,
      final MemorySegment bytecodePtr,
      final long bytecodeLen,
      final MemorySegment moduleOutPtr) {
    validatePointer(cachePtr, "cachePtr");
    validatePointer(bytecodePtr, "bytecodePtr");
    validatePointer(moduleOutPtr, "moduleOutPtr");
    final int result =
        callNativeFunction(
            "wasmtime4j_module_cache_get_or_compile",
            Integer.class,
            cachePtr,
            bytecodePtr,
            bytecodeLen,
            moduleOutPtr);
    return result != 0;
  }

  /**
   * Pre-compiles and caches a module.
   *
   * @param cachePtr pointer to the module cache
   * @param bytecodePtr pointer to the bytecode
   * @param bytecodeLen length of the bytecode
   * @param hashOutPtr pointer to store the hash output
   * @param hashOutLen length of the hash output buffer
   * @return length of hash written, or negative on error
   */
  public int moduleCachePrecompile(
      final MemorySegment cachePtr,
      final MemorySegment bytecodePtr,
      final long bytecodeLen,
      final MemorySegment hashOutPtr,
      final int hashOutLen) {
    validatePointer(cachePtr, "cachePtr");
    validatePointer(bytecodePtr, "bytecodePtr");
    validatePointer(hashOutPtr, "hashOutPtr");
    return callNativeFunction(
        "wasmtime4j_module_cache_precompile",
        Integer.class,
        cachePtr,
        bytecodePtr,
        bytecodeLen,
        hashOutPtr,
        (long) hashOutLen);
  }

  /**
   * Clears all entries from the module cache.
   *
   * @param cachePtr pointer to the module cache
   * @return true on success, false on failure
   */
  public boolean moduleCacheClear(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    final int result = callNativeFunction("wasmtime4j_module_cache_clear", Integer.class, cachePtr);
    return result != 0;
  }

  /**
   * Performs cache maintenance.
   *
   * @param cachePtr pointer to the module cache
   * @return true on success, false on failure
   */
  public boolean moduleCachePerformMaintenance(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    final int result =
        callNativeFunction("wasmtime4j_module_cache_perform_maintenance", Integer.class, cachePtr);
    return result != 0;
  }

  /**
   * Gets the number of entries in the cache.
   *
   * @param cachePtr pointer to the module cache
   * @return entry count, or negative on error
   */
  public long moduleCacheEntryCount(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_entry_count", Long.class, cachePtr);
  }

  /**
   * Gets the cache hit count.
   *
   * @param cachePtr pointer to the module cache
   * @return hit count, or negative on error
   */
  public long moduleCacheHitCount(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_hit_count", Long.class, cachePtr);
  }

  /**
   * Gets the cache miss count.
   *
   * @param cachePtr pointer to the module cache
   * @return miss count, or negative on error
   */
  public long moduleCacheMissCount(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_miss_count", Long.class, cachePtr);
  }

  /**
   * Gets the storage bytes used by the cache.
   *
   * @param cachePtr pointer to the module cache
   * @return storage bytes used, or negative on error
   */
  public long moduleCacheStorageBytes(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_storage_bytes", Long.class, cachePtr);
  }

  /**
   * Destroys a module cache.
   *
   * @param cachePtr pointer to the module cache
   */
  public void moduleCacheDestroy(final MemorySegment cachePtr) {
    if (cachePtr != null && !cachePtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_module_cache_destroy", Void.class, cachePtr);
    }
  }

  // ==================== Async Runtime Methods ====================

  /**
   * Initializes the async runtime.
   *
   * <p>This function initializes the global Tokio async runtime. It is safe to call multiple times.
   *
   * @return 0 on success, non-zero on error
   */
  public int asyncRuntimeInit() {
    return callNativeFunction("wasmtime4j_async_runtime_init", Integer.class);
  }

  /**
   * Gets async runtime information.
   *
   * <p>Returns information about the current async runtime state.
   *
   * @return pointer to runtime info string, or null on error
   */
  public MemorySegment asyncRuntimeInfo() {
    return callNativeFunction("wasmtime4j_async_runtime_info", MemorySegment.class);
  }

  /**
   * Shuts down the async runtime.
   *
   * <p>This function performs graceful shutdown of async operations.
   *
   * @return 0 on success, non-zero on error
   */
  public int asyncRuntimeShutdown() {
    return callNativeFunction("wasmtime4j_async_runtime_shutdown", Integer.class);
  }

  /**
   * Executes a WebAssembly function asynchronously.
   *
   * @param instancePtr pointer to the instance
   * @param functionName pointer to function name string
   * @param argsPtr pointer to arguments array
   * @param argsLen number of arguments
   * @param timeoutMs timeout in milliseconds
   * @param callback completion callback function pointer
   * @param userData user data for callback
   * @return operation ID on success, negative value on error
   */
  public int funcCallAsync(
      final MemorySegment instancePtr,
      final MemorySegment functionName,
      final MemorySegment argsPtr,
      final int argsLen,
      final long timeoutMs,
      final MemorySegment callback,
      final MemorySegment userData) {
    return callNativeFunction(
        "wasmtime4j_func_call_async",
        Integer.class,
        instancePtr,
        functionName,
        argsPtr,
        argsLen,
        timeoutMs,
        callback,
        userData);
  }

  /**
   * Compiles a WebAssembly module asynchronously.
   *
   * @param moduleBytes pointer to module bytecode
   * @param moduleLen length of module bytecode
   * @param timeoutMs timeout in milliseconds
   * @param callback completion callback function pointer
   * @param progressCallback progress callback function pointer
   * @param userData user data for callbacks
   * @return operation ID on success, negative value on error
   */
  public int moduleCompileAsync(
      final MemorySegment moduleBytes,
      final int moduleLen,
      final long timeoutMs,
      final MemorySegment callback,
      final MemorySegment progressCallback,
      final MemorySegment userData) {
    return callNativeFunction(
        "wasmtime4j_module_compile_async",
        Integer.class,
        moduleBytes,
        moduleLen,
        timeoutMs,
        callback,
        progressCallback,
        userData);
  }

  // =============================================================================
  // WASI-NN Functions
  // =============================================================================

  /**
   * Creates a new WASI-NN context.
   *
   * @return pointer to the WASI-NN context, or null on failure
   */
  public MemorySegment wasiNnContextCreate() {
    return callNativeFunction("wasmtime4j_panama_wasi_nn_context_create", MemorySegment.class);
  }

  /**
   * Checks if WASI-NN is available in this build.
   *
   * @return 1 if available, 0 if not
   */
  public int wasiNnIsAvailable() {
    return callNativeFunction("wasmtime4j_panama_wasi_nn_is_available", Integer.class);
  }

  /**
   * Gets the default execution target.
   *
   * @return the default target ordinal (0 = CPU)
   */
  public int wasiNnGetDefaultTarget() {
    return callNativeFunction("wasmtime4j_panama_wasi_nn_get_default_target", Integer.class);
  }

  /**
   * Closes a WASI-NN context.
   *
   * @param contextHandle the WASI-NN context handle
   */
  public void wasiNnContextClose(final MemorySegment contextHandle) {
    if (contextHandle != null && !contextHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_nn_context_close", Void.class, contextHandle);
    }
  }

  /**
   * Loads a graph from model data.
   *
   * @param contextHandle the WASI-NN context handle
   * @param dataPtr pointer to the model data
   * @param dataLen length of the model data
   * @param encodingOrdinal the graph encoding format ordinal
   * @param targetOrdinal the execution target ordinal
   * @return pointer to the graph, or null on error
   */
  public MemorySegment wasiNnLoadGraph(
      final MemorySegment contextHandle,
      final MemorySegment dataPtr,
      final long dataLen,
      final int encodingOrdinal,
      final int targetOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_load_graph",
        MemorySegment.class,
        contextHandle,
        dataPtr,
        dataLen,
        encodingOrdinal,
        targetOrdinal);
  }

  /**
   * Loads a graph by name.
   *
   * @param contextHandle the WASI-NN context handle
   * @param namePtr pointer to the null-terminated model name string
   * @param targetOrdinal the execution target ordinal
   * @return pointer to the graph, or null on error
   */
  public MemorySegment wasiNnLoadGraphByName(
      final MemorySegment contextHandle, final MemorySegment namePtr, final int targetOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(namePtr, "namePtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_load_graph_by_name",
        MemorySegment.class,
        contextHandle,
        namePtr,
        targetOrdinal);
  }

  /**
   * Gets supported encodings.
   *
   * @param contextHandle the WASI-NN context handle
   * @param outEncodings pointer to array to receive encoding ordinals
   * @param maxCount maximum number of encodings to return
   * @return number of encodings written, or -1 on error
   */
  public int wasiNnGetSupportedEncodings(
      final MemorySegment contextHandle, final MemorySegment outEncodings, final int maxCount) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_get_supported_encodings",
        Integer.class,
        contextHandle,
        outEncodings,
        maxCount);
  }

  /**
   * Gets supported targets.
   *
   * @param contextHandle the WASI-NN context handle
   * @param outTargets pointer to array to receive target ordinals
   * @param maxCount maximum number of targets to return
   * @return number of targets written, or -1 on error
   */
  public int wasiNnGetSupportedTargets(
      final MemorySegment contextHandle, final MemorySegment outTargets, final int maxCount) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_get_supported_targets",
        Integer.class,
        contextHandle,
        outTargets,
        maxCount);
  }

  /**
   * Checks if an encoding is supported.
   *
   * @param contextHandle the WASI-NN context handle
   * @param encodingOrdinal the encoding ordinal to check
   * @return 1 if supported, 0 if not
   */
  public int wasiNnIsEncodingSupported(
      final MemorySegment contextHandle, final int encodingOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_is_encoding_supported",
        Integer.class,
        contextHandle,
        encodingOrdinal);
  }

  /**
   * Checks if a target is supported.
   *
   * @param contextHandle the WASI-NN context handle
   * @param targetOrdinal the target ordinal to check
   * @return 1 if supported, 0 if not
   */
  public int wasiNnIsTargetSupported(final MemorySegment contextHandle, final int targetOrdinal) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_is_target_supported",
        Integer.class,
        contextHandle,
        targetOrdinal);
  }

  /**
   * Creates an execution context from a graph.
   *
   * @param graphHandle the graph handle
   * @return pointer to the execution context, or null on error
   */
  public MemorySegment wasiNnGraphCreateExecContext(final MemorySegment graphHandle) {
    validatePointer(graphHandle, "graphHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_graph_create_exec_context", MemorySegment.class, graphHandle);
  }

  /**
   * Closes a graph.
   *
   * @param graphHandle the graph handle
   */
  public void wasiNnGraphClose(final MemorySegment graphHandle) {
    if (graphHandle != null && !graphHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_nn_graph_close", Void.class, graphHandle);
    }
  }

  /**
   * Sets an input tensor by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the input index
   * @param dimsPtr pointer to dimensions array
   * @param dimsLen number of dimensions
   * @param typeOrdinal the tensor type ordinal
   * @param dataPtr pointer to tensor data
   * @param dataLen length of tensor data
   * @return 0 on success, -1 on error
   */
  public int wasiNnExecSetInput(
      final MemorySegment ctxHandle,
      final int index,
      final MemorySegment dimsPtr,
      final int dimsLen,
      final int typeOrdinal,
      final MemorySegment dataPtr,
      final long dataLen) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_set_input",
        Integer.class,
        ctxHandle,
        index,
        dimsPtr,
        dimsLen,
        typeOrdinal,
        dataPtr,
        dataLen);
  }

  /**
   * Runs inference.
   *
   * @param ctxHandle the execution context handle
   * @return 0 on success, -1 on error
   */
  public int wasiNnExecCompute(final MemorySegment ctxHandle) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction("wasmtime4j_panama_wasi_nn_exec_compute", Integer.class, ctxHandle);
  }

  /**
   * Gets output tensor data by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @param outData pointer to buffer to receive data
   * @param maxLen maximum buffer length
   * @return actual data length, or -1 on error
   */
  public long wasiNnExecGetOutput(
      final MemorySegment ctxHandle,
      final int index,
      final MemorySegment outData,
      final long maxLen) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output", Long.class, ctxHandle, index, outData, maxLen);
  }

  /**
   * Gets output tensor size by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @return the output size, or -1 on error
   */
  public long wasiNnExecGetOutputSize(final MemorySegment ctxHandle, final int index) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output_size", Long.class, ctxHandle, index);
  }

  /**
   * Gets output tensor dimensions by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @param outDims pointer to buffer to receive dimensions
   * @param maxDims maximum number of dimensions
   * @return number of dimensions, or -1 on error
   */
  public int wasiNnExecGetOutputDims(
      final MemorySegment ctxHandle,
      final int index,
      final MemorySegment outDims,
      final int maxDims) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output_dims",
        Integer.class,
        ctxHandle,
        index,
        outDims,
        maxDims);
  }

  /**
   * Gets output tensor type by index.
   *
   * @param ctxHandle the execution context handle
   * @param index the output index
   * @return the tensor type ordinal, or -1 on error
   */
  public int wasiNnExecGetOutputType(final MemorySegment ctxHandle, final int index) {
    validatePointer(ctxHandle, "ctxHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_nn_exec_get_output_type", Integer.class, ctxHandle, index);
  }

  /**
   * Closes an execution context.
   *
   * @param ctxHandle the execution context handle
   */
  public void wasiNnExecClose(final MemorySegment ctxHandle) {
    if (ctxHandle != null && !ctxHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_nn_exec_close", Void.class, ctxHandle);
    }
  }

  // ============================================================================
  // WASI-Threads Functions
  // ============================================================================

  /**
   * Checks if WASI-Threads is supported in this build.
   *
   * @return true if WASI-Threads is supported, false otherwise
   */
  public boolean wasiThreadsIsSupported() {
    try {
      final int result =
          callNativeFunction("wasmtime4j_panama_wasi_threads_is_supported", Integer.class);
      return result != 0;
    } catch (final Exception e) {
      LOGGER.fine("WASI-Threads support check failed: " + e.getMessage());
      return false;
    }
  }

  /**
   * Creates a new WASI-Threads context.
   *
   * @param moduleHandle the module memory segment
   * @param linkerHandle the linker memory segment
   * @param storeHandle the store memory segment
   * @param arena the arena for memory management
   * @return the created context memory segment, or NULL on error
   */
  public MemorySegment wasiThreadsContextCreate(
      final MemorySegment moduleHandle,
      final MemorySegment linkerHandle,
      final MemorySegment storeHandle,
      final java.lang.foreign.Arena arena) {
    validatePointer(moduleHandle, "moduleHandle");
    validatePointer(linkerHandle, "linkerHandle");
    validatePointer(storeHandle, "storeHandle");
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }

    return callNativeFunction(
        "wasmtime4j_panama_wasi_threads_context_create",
        MemorySegment.class,
        moduleHandle,
        linkerHandle,
        storeHandle);
  }

  /**
   * Closes and frees a WASI-Threads context.
   *
   * @param contextHandle the context memory segment
   */
  public void wasiThreadsContextClose(final MemorySegment contextHandle) {
    if (contextHandle != null && !contextHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_threads_context_close", Void.class, contextHandle);
    }
  }

  /**
   * Spawns a new thread using WASI-Threads.
   *
   * @param contextHandle the WASI-Threads context
   * @param threadStartArg the argument to pass to the thread start function
   * @return the thread ID (positive) on success, negative on error
   */
  public int wasiThreadsSpawn(final MemorySegment contextHandle, final int threadStartArg) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_threads_spawn", Integer.class, contextHandle, threadStartArg);
  }

  /**
   * Adds the WASI-Threads thread-spawn function to a linker.
   *
   * @param linkerHandle the linker memory segment
   * @param storeHandle the store memory segment
   * @param moduleHandle the module memory segment
   */
  public void wasiThreadsAddToLinker(
      final MemorySegment linkerHandle,
      final MemorySegment storeHandle,
      final MemorySegment moduleHandle) {
    validatePointer(linkerHandle, "linkerHandle");
    validatePointer(storeHandle, "storeHandle");
    validatePointer(moduleHandle, "moduleHandle");

    final int result =
        callNativeFunction(
            "wasmtime4j_panama_wasi_threads_add_to_linker",
            Integer.class,
            linkerHandle,
            storeHandle,
            moduleHandle);

    if (result != 0) {
      throw new RuntimeException("Failed to add WASI-Threads to linker, error code: " + result);
    }
  }

  // ============================================================================
  // Coredump Functions
  // ============================================================================

  /**
   * Frees a coredump from the registry.
   *
   * @param coredumpId the coredump ID
   * @return 0 on success, -1 on error
   */
  public int coredumpFree(final long coredumpId) {
    return callNativeFunction("wasmtime4j_coredump_free", Integer.class, coredumpId);
  }

  /**
   * Gets the number of frames in a coredump.
   *
   * @param coredumpId the coredump ID
   * @return frame count, or -1 on error
   */
  public int coredumpGetFrameCount(final long coredumpId) {
    return callNativeFunction("wasmtime4j_coredump_get_frame_count", Integer.class, coredumpId);
  }

  /**
   * Gets the trap message from a coredump.
   *
   * @param coredumpId the coredump ID
   * @return the trap message, or null if not available
   */
  public String coredumpGetTrapMessage(final long coredumpId) {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_trap_message", MemorySegment.class, coredumpId);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Gets the name of a coredump.
   *
   * @param coredumpId the coredump ID
   * @return the coredump name, or null if not set
   */
  public String coredumpGetName(final long coredumpId) {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_name", MemorySegment.class, coredumpId);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Frees a C string allocated by coredump functions.
   *
   * @param ptr the string pointer to free
   */
  public void coredumpStringFree(final MemorySegment ptr) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_coredump_string_free", Void.class, ptr);
    }
  }

  /**
   * Serializes a coredump to bytes.
   *
   * @param coredumpId the coredump ID
   * @param storePtr pointer to the store
   * @param name the coredump name for serialization
   * @return the serialized bytes, or null on error
   */
  public byte[] coredumpSerialize(
      final long coredumpId, final MemorySegment storePtr, final String name) {
    validatePointer(storePtr, "storePtr");
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment namePtr = arena.allocateFrom(name);
      final MemorySegment outPtr = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLen = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_coredump_serialize",
              Integer.class,
              coredumpId,
              storePtr,
              namePtr,
              outPtr,
              outLen);

      if (result != 0) {
        return null;
      }

      final MemorySegment bytesPtr = outPtr.get(ValueLayout.ADDRESS, 0);
      final long length = outLen.get(ValueLayout.JAVA_LONG, 0);

      if (bytesPtr.equals(MemorySegment.NULL) || length <= 0) {
        return null;
      }

      try {
        final byte[] bytes = new byte[(int) length];
        bytesPtr.reinterpret(length).asByteBuffer().get(bytes);
        return bytes;
      } finally {
        coredumpBytesFree(bytesPtr, length);
      }
    }
  }

  /**
   * Frees bytes allocated by coredump serialization.
   *
   * @param ptr the bytes pointer to free
   * @param len the length of the bytes
   */
  public void coredumpBytesFree(final MemorySegment ptr, final long len) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL) && len > 0) {
      callNativeFunction("wasmtime4j_coredump_bytes_free", Void.class, ptr, len);
    }
  }

  /**
   * Gets information about a specific frame in the coredump as JSON.
   *
   * @param coredumpId the coredump ID
   * @param frameIndex the frame index
   * @return JSON string with frame details, or null on error
   */
  public String coredumpGetFrameInfo(final long coredumpId, final int frameIndex) {
    final MemorySegment ptr =
        callNativeFunction(
            "wasmtime4j_coredump_get_frame_info", MemorySegment.class, coredumpId, frameIndex);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Gets all frame information as a JSON array.
   *
   * @param coredumpId the coredump ID
   * @return JSON array string with all frames, or null on error
   */
  public String coredumpGetAllFrames(final long coredumpId) {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_all_frames", MemorySegment.class, coredumpId);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Gets the number of registered coredumps.
   *
   * @return the count, or -1 on error
   */
  public int coredumpGetCount() {
    return callNativeFunction("wasmtime4j_coredump_get_count", Integer.class);
  }

  /**
   * Gets all registered coredump IDs as a JSON array.
   *
   * @return JSON array of coredump IDs, or null on error
   */
  public String coredumpGetAllIds() {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_all_ids", MemorySegment.class);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Clears all registered coredumps.
   *
   * @return 0 on success, -1 on error
   */
  public int coredumpClearAll() {
    return callNativeFunction("wasmtime4j_coredump_clear_all", Integer.class);
  }

  // ===== Exception Handling Methods =====

  /**
   * Creates a new WebAssembly tag for exception handling.
   *
   * @param storePtr the store pointer
   * @param paramTypes the parameter type codes
   * @param returnTypes the return type codes
   * @return the tag pointer, or NULL on error
   */
  public MemorySegment tagCreate(
      final MemorySegment storePtr, final int[] paramTypes, final int[] returnTypes) {
    validatePointer(storePtr, "storePtr");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment paramsSegment = arena.allocate(ValueLayout.JAVA_INT, paramTypes.length);
      for (int i = 0; i < paramTypes.length; i++) {
        paramsSegment.setAtIndex(ValueLayout.JAVA_INT, i, paramTypes[i]);
      }

      final MemorySegment returnsSegment = arena.allocate(ValueLayout.JAVA_INT, returnTypes.length);
      for (int i = 0; i < returnTypes.length; i++) {
        returnsSegment.setAtIndex(ValueLayout.JAVA_INT, i, returnTypes[i]);
      }

      return callNativeFunction(
          "wasmtime4j_panama_tag_create",
          MemorySegment.class,
          storePtr,
          paramsSegment,
          paramTypes.length,
          returnsSegment,
          returnTypes.length);
    }
  }

  /**
   * Gets the parameter types of a tag.
   *
   * @param tagPtr the tag pointer
   * @param storePtr the store pointer
   * @return the parameter type codes
   */
  public int[] tagGetParamTypes(final MemorySegment tagPtr, final MemorySegment storePtr) {
    validatePointer(tagPtr, "tagPtr");
    validatePointer(storePtr, "storePtr");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment countPtr = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment typesPtr =
          callNativeFunction(
              "wasmtime4j_panama_tag_get_param_types",
              MemorySegment.class,
              tagPtr,
              storePtr,
              countPtr);
      if (typesPtr == null || typesPtr.equals(MemorySegment.NULL)) {
        return new int[0];
      }
      final int count = countPtr.get(ValueLayout.JAVA_INT, 0);
      final int[] types = new int[count];
      for (int i = 0; i < count; i++) {
        types[i] =
            typesPtr
                .reinterpret(count * ValueLayout.JAVA_INT.byteSize())
                .getAtIndex(ValueLayout.JAVA_INT, i);
      }
      tagTypesArrayFree(typesPtr, count);
      return types;
    }
  }

  /**
   * Gets the return types of a tag.
   *
   * @param tagPtr the tag pointer
   * @param storePtr the store pointer
   * @return the return type codes
   */
  public int[] tagGetReturnTypes(final MemorySegment tagPtr, final MemorySegment storePtr) {
    validatePointer(tagPtr, "tagPtr");
    validatePointer(storePtr, "storePtr");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment countPtr = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment typesPtr =
          callNativeFunction(
              "wasmtime4j_panama_tag_get_return_types",
              MemorySegment.class,
              tagPtr,
              storePtr,
              countPtr);
      if (typesPtr == null || typesPtr.equals(MemorySegment.NULL)) {
        return new int[0];
      }
      final int count = countPtr.get(ValueLayout.JAVA_INT, 0);
      final int[] types = new int[count];
      for (int i = 0; i < count; i++) {
        types[i] =
            typesPtr
                .reinterpret(count * ValueLayout.JAVA_INT.byteSize())
                .getAtIndex(ValueLayout.JAVA_INT, i);
      }
      tagTypesArrayFree(typesPtr, count);
      return types;
    }
  }

  /**
   * Frees a tag types array.
   *
   * @param ptr the types array pointer
   * @param count the number of elements
   */
  private void tagTypesArrayFree(final MemorySegment ptr, final int count) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_tag_types_free", Void.class, ptr, count);
    }
  }

  /**
   * Checks if two tags are equal.
   *
   * @param tag1Ptr the first tag pointer
   * @param tag2Ptr the second tag pointer
   * @param storePtr the store pointer
   * @return 1 if equal, 0 if not equal, -1 on error
   */
  public int tagEquals(
      final MemorySegment tag1Ptr, final MemorySegment tag2Ptr, final MemorySegment storePtr) {
    validatePointer(tag1Ptr, "tag1Ptr");
    validatePointer(tag2Ptr, "tag2Ptr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_tag_equals", Integer.class, tag1Ptr, tag2Ptr, storePtr);
  }

  /**
   * Destroys a tag and frees its native resources.
   *
   * @param tagPtr the tag pointer
   */
  public void tagDestroy(final MemorySegment tagPtr) {
    if (tagPtr != null && !tagPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_tag_destroy", Void.class, tagPtr);
    }
  }

  // ===== Exception Reference Methods =====

  /**
   * Gets the tag from an exception reference.
   *
   * @param exnRefPtr the exception reference pointer
   * @param storePtr the store pointer
   * @return the tag pointer, or NULL on error
   */
  public MemorySegment exnRefGetTag(final MemorySegment exnRefPtr, final MemorySegment storePtr) {
    validatePointer(exnRefPtr, "exnRefPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_exnref_get_tag", MemorySegment.class, exnRefPtr, storePtr);
  }

  /**
   * Checks if an exception reference is valid.
   *
   * @param exnRefPtr the exception reference pointer
   * @param storePtr the store pointer
   * @return 1 if valid, 0 if not valid, -1 on error
   */
  public int exnRefIsValid(final MemorySegment exnRefPtr, final MemorySegment storePtr) {
    validatePointer(exnRefPtr, "exnRefPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_exnref_is_valid", Integer.class, exnRefPtr, storePtr);
  }

  /**
   * Destroys an exception reference and frees its native resources.
   *
   * @param exnRefPtr the exception reference pointer
   */
  public void exnRefDestroy(final MemorySegment exnRefPtr) {
    if (exnRefPtr != null && !exnRefPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_exnref_destroy", Void.class, exnRefPtr);
    }
  }

  // ===== Store Exception Methods =====

  /**
   * Throws an exception in the store.
   *
   * @param storePtr the store pointer
   * @param exnRefPtr the exception reference pointer
   * @return 0 on success, -1 on error
   */
  public int storeThrowException(final MemorySegment storePtr, final MemorySegment exnRefPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(exnRefPtr, "exnRefPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_throw_exception", Integer.class, storePtr, exnRefPtr);
  }

  /**
   * Takes and removes the pending exception from the store.
   *
   * @param storePtr the store pointer
   * @return the exception reference pointer, or NULL if no pending exception
   */
  public MemorySegment storeTakePendingException(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_take_pending_exception", MemorySegment.class, storePtr);
  }

  /**
   * Checks if the store has a pending exception.
   *
   * @param storePtr the store pointer
   * @return 1 if pending exception exists, 0 if not, -1 on error
   */
  public int storeHasPendingException(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_has_pending_exception", Integer.class, storePtr);
  }

  // ===== Linker Configuration Methods =====

  /**
   * Allows subsequent definitions to shadow prior definitions.
   *
   * @param linkerPtr the linker pointer
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, -1 on error
   */
  public int linkerAllowShadowing(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_panama_linker_allow_shadowing", Integer.class, linkerPtr, allow);
  }

  /**
   * Allows unknown exports from modules.
   *
   * @param linkerPtr the linker pointer
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, -1 on error
   */
  public int linkerAllowUnknownExports(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_panama_linker_allow_unknown_exports", Integer.class, linkerPtr, allow);
  }

  /**
   * Defines all undefined imports as trapping functions.
   *
   * @param linkerPtr the linker pointer
   * @param storePtr the store pointer
   * @param modulePtr the module pointer
   * @return 0 on success, -1 on error
   */
  public int linkerDefineUnknownImportsAsTraps(
      final MemorySegment linkerPtr, final MemorySegment storePtr, final MemorySegment modulePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_linker_define_unknown_imports_as_traps",
        Integer.class,
        linkerPtr,
        storePtr,
        modulePtr);
  }

  /**
   * Defines all undefined imports with default values.
   *
   * @param linkerPtr the linker pointer
   * @param storePtr the store pointer
   * @param modulePtr the module pointer
   * @return 0 on success, -1 on error
   */
  public int linkerDefineUnknownImportsAsDefaultValues(
      final MemorySegment linkerPtr, final MemorySegment storePtr, final MemorySegment modulePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_linker_define_unknown_imports_as_default_values",
        Integer.class,
        linkerPtr,
        storePtr,
        modulePtr);
  }

  /**
   * Gets a definition by its import specifier.
   *
   * @param linkerPtr the linker pointer
   * @param storePtr the store pointer
   * @param moduleNamePtr the module name string
   * @param namePtr the item name string
   * @return the extern pointer, or NULL if not found
   */
  public MemorySegment linkerGetByImport(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment namePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_linker_get",
        MemorySegment.class,
        linkerPtr,
        storePtr,
        moduleNamePtr,
        namePtr);
  }

  /**
   * Gets the default function for a module.
   *
   * @param linkerPtr the linker pointer
   * @param storePtr the store pointer
   * @param moduleNamePtr the module name string
   * @return the function pointer, or NULL if not found
   */
  public MemorySegment linkerGetDefault(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_linker_get_default",
        MemorySegment.class,
        linkerPtr,
        storePtr,
        moduleNamePtr);
  }

  /**
   * Gets the type of an extern value.
   *
   * @param externPtr the extern pointer
   * @return the extern type code (0=FUNC, 1=TABLE, 2=MEMORY, 3=GLOBAL), or -1 on error
   */
  public int externGetType(final MemorySegment externPtr) {
    validatePointer(externPtr, "externPtr");
    return callNativeFunction("wasmtime4j_panama_extern_type", Integer.class, externPtr);
  }

  // ===== Call Hook Methods =====

  /**
   * Sets a call hook on the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeSetCallHook(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_set_call_hook", Integer.class, storePtr);
  }

  /**
   * Clears the call hook from the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeClearCallHook(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_clear_call_hook", Integer.class, storePtr);
  }

  /**
   * Sets an async call hook on the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeSetCallHookAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_call_hook_async", Integer.class, storePtr);
  }

  /**
   * Clears the async call hook from the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeClearCallHookAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_clear_call_hook_async", Integer.class, storePtr);
  }

  // ===== Detect Precompiled Method =====

  /**
   * Detects whether bytes are a precompiled artifact.
   *
   * @param enginePtr the engine pointer
   * @param bytesPtr pointer to the bytes to inspect
   * @param bytesLen length of the bytes
   * @return -1 if not precompiled, 0 for MODULE, 1 for COMPONENT
   */
  public int engineDetectPrecompiled(
      final MemorySegment enginePtr, final MemorySegment bytesPtr, final long bytesLen) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(bytesPtr, "bytesPtr");
    return callNativeFunction(
        "wasmtime4j_panama_engine_detect_precompiled",
        Integer.class,
        enginePtr,
        bytesPtr,
        bytesLen);
  }
}
