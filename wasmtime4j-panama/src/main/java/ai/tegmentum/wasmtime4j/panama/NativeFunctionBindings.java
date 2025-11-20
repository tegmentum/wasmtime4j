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

import ai.tegmentum.wasmtime4j.WasmValue;
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
      callNativeFunction(
          "wasmtime4j_panama_module_free_serialized_data", Void.class, dataPtr, len);
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
    return callNativeFunction(
        "wasmtime4j_module_create", MemorySegment.class, enginePtr, wasmBytes, wasmSize);
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
    return callNativeFunction("wasmtime4j_module_imports_len", Long.class, modulePtr);
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
    return callNativeFunction("wasmtime4j_module_exports_len", Long.class, modulePtr);
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
    return callNativeFunction("wasmtime4j_store_create", MemorySegment.class, enginePtr);
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
   * Creates a WebAssembly instance (Panama FFI).
   *
   * @param storePtr pointer to the store
   * @param modulePtr pointer to the module
   * @return memory segment pointer to the instance, or null on failure
   */
  public MemorySegment instanceCreate(final MemorySegment storePtr, final MemorySegment modulePtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");
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
      final MemorySegment refIdOutPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(i32ValueOutPtr, "i32ValueOutPtr");
    validatePointer(i64ValueOutPtr, "i64ValueOutPtr");
    validatePointer(f32ValueOutPtr, "f32ValueOutPtr");
    validatePointer(f64ValueOutPtr, "f64ValueOutPtr");
    validatePointer(refIdPresentOutPtr, "refIdPresentOutPtr");
    validatePointer(refIdOutPtr, "refIdOutPtr");

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
        refIdOutPtr);
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
      final long refId) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");

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
        refId);
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
      case FUNCREF:
        if (value.asFuncref() != null) {
          refIdPresent = 1;
          refId = ((Long) value.asFuncref()).longValue();
        }
        break;
      case EXTERNREF:
        if (value.asExternref() != null) {
          refIdPresent = 1;
          refId = ((Long) value.asExternref()).longValue();
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
        refId);
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
      case FUNCREF:
        if (value.asFuncref() != null) {
          refIdPresent = 1;
          refId = ((Long) value.asFuncref()).longValue();
        }
        break;
      case EXTERNREF:
        if (value.asExternref() != null) {
          refIdPresent = 1;
          refId = ((Long) value.asExternref()).longValue();
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

    // Module introspection functions
    addFunctionBinding(
        "wasmtime4j_module_imports_len",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return import count
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_module_import_nth",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return found
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS, // name_out_ptr
            ValueLayout.ADDRESS)); // type_out_ptr

    addFunctionBinding(
        "wasmtime4j_module_exports_len",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return export count
            ValueLayout.ADDRESS)); // module_ptr

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

    // Store functions
    // Panama FFI binding: returns store pointer directly
    addFunctionBinding(
        "wasmtime4j_store_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return store*
            ValueLayout.ADDRESS)); // engine_ptr

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

    // Instance functions - Panama FFI: return instance pointer directly
    addFunctionBinding(
        "wasmtime4j_instance_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance*
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // module_ptr

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
            ValueLayout.ADDRESS)); // ref_id out

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
            ValueLayout.JAVA_LONG)); // ref_id

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
        "wasmtime4j_panama_linker_instantiate",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance* or null
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_linker_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // linker_ptr

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

    // WASI Linker Integration
    addFunctionBinding(
        "wasmtime4j_linker_add_wasi",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

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
        "wasmtime4j_panama_linker_instantiate",
        MemorySegment.class,
        linkerPtr,
        storePtr,
        modulePtr);
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
    try (final Arena arena = Arena.ofAuto()) {
      final MemorySegment featureNameSegment = arena.allocateFrom(featureName);
      return callNativeFunction(
          "wasmtime4j_panama_engine_supports_feature",
          Boolean.class,
          enginePtr,
          featureNameSegment);
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
}
