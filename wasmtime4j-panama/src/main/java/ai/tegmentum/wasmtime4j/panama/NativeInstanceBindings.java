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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Native function bindings for Instance, Linker, InstancePre, Caller, Function, Function Reference,
 * extern type, and call hook operations.
 *
 * <p>Provides type-safe wrappers for all Wasmtime instance lifecycle, linker configuration and
 * instantiation, caller context access, function invocation, function reference management, and
 * call hook native functions. Hot-path methods use eagerly initialized volatile {@link
 * MethodHandle} fields for {@code invokeExact} optimization.
 *
 * <p>This class follows the singleton pattern with double-checked locking.
 */
public final class NativeInstanceBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeInstanceBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeInstanceBindings INSTANCE = new NativeInstanceBindings();
  }

  // Hot-path volatile MethodHandle fields for invokeExact optimization
  // Signature: (ADDRESS, ADDRESS) -> ADDRESS
  private volatile MethodHandle mhInstanceCreate;

  // Signature: (ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, JAVA_LONG) -> JAVA_LONG
  private volatile MethodHandle mhInstanceCallFunction;

  private NativeInstanceBindings() {
    super();
    initializeBindings();
    initializeHotPathHandles();
    markInitialized();
    LOGGER.fine("Initialized NativeInstanceBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeInstanceBindings getInstance() {
    return Holder.INSTANCE;
  }

  /** Eagerly initializes hot-path MethodHandles after all bindings are registered. */
  private void initializeHotPathHandles() {
    FunctionBinding instanceCreateBinding = getFunctionBinding("wasmtime4j_instance_create");
    if (instanceCreateBinding != null) {
      this.mhInstanceCreate = instanceCreateBinding.getMethodHandle().orElse(null);
    }

    FunctionBinding callBinding = getFunctionBinding("wasmtime4j_instance_call_function");
    if (callBinding != null) {
      this.mhInstanceCallFunction = callBinding.getMethodHandle().orElse(null);
    }
  }

  /**
   * Gets the method handle for Panama FFI instance creation.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaInstanceCreate() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_instance_create");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  // =============================================================================
  // Instance Operations
  // =============================================================================

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
   * Creates a WebAssembly instance with explicit imports (Panama FFI).
   *
   * @param storePtr pointer to the store
   * @param modulePtr pointer to the module
   * @param externPtrs pointer to array of extern handle pointers
   * @param externTypes pointer to array of extern type codes
   * @param count number of imports
   * @param instanceOut output pointer for the created instance
   * @return 0 on success, negative error code on failure
   */
  public int panamaInstanceCreateWithImports(
      final MemorySegment storePtr,
      final MemorySegment modulePtr,
      final MemorySegment externPtrs,
      final MemorySegment externTypes,
      final int count,
      final MemorySegment instanceOut) {
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");
    validatePointer(instanceOut, "instanceOut");

    return callNativeFunction(
        "wasmtime4j_panama_instance_create_with_imports",
        Integer.class,
        storePtr,
        modulePtr,
        externPtrs,
        externTypes,
        count,
        instanceOut);
  }

  /**
   * Gets an export from an instance using a pre-resolved ModuleExport handle (Panama FFI).
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param moduleExportPtr pointer to the ModuleExport handle
   * @param outHandle output pointer for the extern handle
   * @param outType output pointer for the extern type code
   * @return 0 on success, negative error code on failure
   */
  public int panamaInstanceGetModuleExport(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment moduleExportPtr,
      final MemorySegment outHandle,
      final MemorySegment outType) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(moduleExportPtr, "moduleExportPtr");
    validatePointer(outHandle, "outHandle");
    validatePointer(outType, "outType");

    return callNativeFunction(
        "wasmtime4j_panama_instance_get_module_export",
        Integer.class,
        instancePtr,
        storePtr,
        moduleExportPtr,
        outHandle,
        outType);
  }

  /**
   * Gets a ModuleExport handle for O(1) export lookups (Panama FFI).
   *
   * @param modulePtr pointer to the module
   * @param namePtr pointer to the export name string
   * @param outPtr output pointer for the ModuleExport handle
   * @return 0 on success, negative error code on failure
   */
  public int panamaModuleGetModuleExport(
      final MemorySegment modulePtr, final MemorySegment namePtr, final MemorySegment outPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(namePtr, "namePtr");
    validatePointer(outPtr, "outPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_get_module_export", Integer.class, modulePtr, namePtr, outPtr);
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
      // Fall back to generic callNativeFunction if handle not available
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
    try {
      return (long)
          mh.invokeExact(
              instancePtr, storePtr, functionName, paramsPtr, paramCount, resultsPtr, maxResults);
    } catch (Throwable t) {
      throw new RuntimeException("Native instanceCallFunction failed", t);
    }
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
   * Gets a shared memory export by name from an instance (only shared memories).
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the shared memory export
   * @return memory segment pointer or null if not found
   */
  public MemorySegment instanceGetSharedMemoryByName(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_panama_instance_get_shared_memory_by_name",
        MemorySegment.class,
        instancePtr,
        storePtr,
        name);
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
      final long pages,
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
   * Gets a tag export by name from an instance.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name name of the tag export
   * @return tag segment pointer or null if not found
   */
  public MemorySegment instanceGetTagByName(
      final MemorySegment instancePtr, final MemorySegment storePtr, final MemorySegment name) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    return callNativeFunction(
        "wasmtime4j_instance_get_tag_by_name", MemorySegment.class, instancePtr, storePtr, name);
  }

  /**
   * Gets a global export by name from an instance, wrapped for linker use.
   *
   * <p>Returns a properly wrapped Global struct that can be used with {@link
   * #panamaLinkerDefineGlobal}.
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

  // =============================================================================
  // Debug Operations
  // =============================================================================

  /**
   * Debug: gets a function by its internal module index.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param functionIndex the internal function index
   * @return function handle or null if not found
   */
  public MemorySegment instanceDebugFunction(
      final MemorySegment instancePtr, final MemorySegment storePtr, final int functionIndex) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_instance_debug_function",
        MemorySegment.class,
        instancePtr,
        storePtr,
        functionIndex);
  }

  /**
   * Debug: gets a global by its internal module index.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param globalIndex the internal global index
   * @return global handle or null if not found
   */
  public MemorySegment instanceDebugGlobal(
      final MemorySegment instancePtr, final MemorySegment storePtr, final int globalIndex) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_instance_debug_global",
        MemorySegment.class,
        instancePtr,
        storePtr,
        globalIndex);
  }

  /**
   * Debug: gets a memory by its internal module index.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param memoryIndex the internal memory index
   * @return memory handle or null if not found
   */
  public MemorySegment instanceDebugMemory(
      final MemorySegment instancePtr, final MemorySegment storePtr, final int memoryIndex) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_instance_debug_memory",
        MemorySegment.class,
        instancePtr,
        storePtr,
        memoryIndex);
  }

  /**
   * Debug: gets a shared memory by its internal module index.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param memoryIndex the internal memory index
   * @return shared memory handle or null if not found
   */
  public MemorySegment instanceDebugSharedMemory(
      final MemorySegment instancePtr, final MemorySegment storePtr, final int memoryIndex) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_instance_debug_shared_memory",
        MemorySegment.class,
        instancePtr,
        storePtr,
        memoryIndex);
  }

  /**
   * Debug: gets a table by its internal module index.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param tableIndex the internal table index
   * @return table handle or null if not found
   */
  public MemorySegment instanceDebugTable(
      final MemorySegment instancePtr, final MemorySegment storePtr, final int tableIndex) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_instance_debug_table",
        MemorySegment.class,
        instancePtr,
        storePtr,
        tableIndex);
  }

  /**
   * Debug: gets a tag by its internal module index.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param tagIndex the internal tag index
   * @return tag handle or null if not found
   */
  public MemorySegment instanceDebugTag(
      final MemorySegment instancePtr, final MemorySegment storePtr, final int tagIndex) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_instance_debug_tag",
        MemorySegment.class,
        instancePtr,
        storePtr,
        tagIndex);
  }

  // =============================================================================
  // Caller Operations
  // =============================================================================

  /**
   * Gets the fuel remaining in the caller if fuel metering is enabled.
   *
   * @param callerPtr pointer to the caller context
   * @param fuelOut pointer to store the fuel value
   * @return 0 on success, negative error code on failure
   */
  public int callerGetFuelRemaining(final MemorySegment callerPtr, final MemorySegment fuelOut) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(fuelOut, "fuelOut");
    return callNativeFunction(
        "wasmtime4j_panama_caller_get_fuel_remaining", Integer.class, callerPtr, fuelOut);
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
    return callNativeFunction("wasmtime4j_panama_caller_add_fuel", Integer.class, callerPtr, fuel);
  }

  /**
   * Sets the fuel level to a specific value for the caller.
   *
   * @param callerPtr pointer to the caller context
   * @param fuel the fuel level to set
   * @return 0 on success, negative error code on failure
   */
  public int callerSetFuel(final MemorySegment callerPtr, final long fuel) {
    validatePointer(callerPtr, "callerPtr");
    return callNativeFunction("wasmtime4j_panama_caller_set_fuel", Integer.class, callerPtr, fuel);
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
    return callNativeFunction(
        "wasmtime4j_panama_caller_has_export", Integer.class, callerPtr, name);
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
        "wasmtime4j_panama_caller_get_memory", Integer.class, callerPtr, name, memoryOut);
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
        "wasmtime4j_panama_caller_get_function", Integer.class, callerPtr, name, functionOut);
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
        "wasmtime4j_panama_caller_get_global", Integer.class, callerPtr, name, globalOut);
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
        "wasmtime4j_panama_caller_get_table", Integer.class, callerPtr, name, tableOut);
  }

  /**
   * Sets the fuel async yield interval for the caller's store.
   *
   * @param callerPtr pointer to the caller
   * @param interval the yield interval, or 0 to disable
   * @return 0 on success, non-zero on error
   */
  public int callerSetFuelAsyncYieldInterval(final MemorySegment callerPtr, final long interval) {
    validatePointer(callerPtr, "callerPtr");
    return callNativeFunction(
        "wasmtime4j_panama_caller_set_fuel_async_yield_interval",
        Integer.class,
        callerPtr,
        interval);
  }

  /**
   * Gets debug exit frames from the caller.
   *
   * <p>Two-phase protocol: first call with outData=NULL to get count, second call with allocated
   * buffer to get data.
   *
   * @param callerPtr pointer to the caller context
   * @param countOut pointer to store the frame count
   * @param outData pointer to frame data buffer (NULL for phase 1)
   * @return 0 on success, -1 if debugging not enabled, -2 on error
   * @since 1.1.0
   */
  public int callerDebugExitFrames(
      final MemorySegment callerPtr, final MemorySegment countOut, final MemorySegment outData) {
    validatePointer(callerPtr, "callerPtr");
    validatePointer(countOut, "countOut");
    return callNativeFunction(
        "wasmtime4j_panama_caller_debug_exit_frames", Integer.class, callerPtr, countOut, outData);
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
  /**
   * Checks if a function matches a function type using subtype-aware checking.
   *
   * @param funcPtr the function pointer
   * @param storePtr the store pointer
   * @param paramTypesPtr pointer to array of param type codes
   * @param paramCount number of param types
   * @param resultTypesPtr pointer to array of result type codes
   * @param resultCount number of result types
   * @return 1 if matches, 0 if not, negative on error
   */
  public int funcMatchesTy(
      final MemorySegment funcPtr,
      final MemorySegment storePtr,
      final MemorySegment paramTypesPtr,
      final int paramCount,
      final MemorySegment resultTypesPtr,
      final int resultCount) {
    validatePointer(funcPtr, "funcPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_func_matches_ty",
        Integer.class,
        funcPtr,
        storePtr,
        paramTypesPtr,
        paramCount,
        resultTypesPtr,
        resultCount);
  }

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

  /**
   * Gets a function export from an instance by name.
   *
   * @param instancePtr pointer to the instance
   * @param storePtr pointer to the store
   * @param name function name (native string)
   * @param funcPtrOut output pointer for the function handle
   * @return 0 on success, non-zero on failure
   */
  public int funcGet(
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final MemorySegment name,
      final MemorySegment funcPtrOut) {
    validatePointer(instancePtr, "instancePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(name, "name");
    validatePointer(funcPtrOut, "funcPtrOut");
    return callNativeFunction(
        "wasmtime4j_panama_func_get", Integer.class, instancePtr, storePtr, name, funcPtrOut);
  }

  /**
   * Converts a function to its raw funcref pointer value.
   *
   * @param funcPtr pointer to the function
   * @param storePtr pointer to the store
   * @return the raw funcref value (i64)
   */
  public long funcToRaw(final MemorySegment funcPtr, final MemorySegment storePtr) {
    validatePointer(funcPtr, "funcPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_func_to_raw", Long.class, funcPtr, storePtr);
  }

  /**
   * Reconstructs a function from a raw funcref pointer.
   *
   * @param storePtr pointer to the store
   * @param raw the raw funcref value
   * @return pointer to the reconstructed function, or NULL if invalid
   */
  public MemorySegment funcFromRaw(final MemorySegment storePtr, final long raw) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_func_from_raw", MemorySegment.class, storePtr, raw);
  }

  /**
   * Calls a WebAssembly function using Wasmtime's native async call path.
   *
   * <p>Uses {@code Func::call_async} via the async runtime, blocking the current thread but
   * enabling proper async host function interleaving.
   *
   * @param funcPtr pointer to the function
   * @param storePtr pointer to the store
   * @param paramsPtr pointer to parameters array (WasmValue format)
   * @param paramCount number of parameters
   * @param resultsPtr pointer to results buffer (WasmValue format)
   * @param maxResults maximum number of results
   * @return actual result count on success, negative value on failure
   */
  public long funcCallNativeAsync(
      final MemorySegment funcPtr,
      final MemorySegment storePtr,
      final MemorySegment paramsPtr,
      final long paramCount,
      final MemorySegment resultsPtr,
      final long maxResults) {
    validatePointer(funcPtr, "funcPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_func_call_async",
        Long.class,
        funcPtr,
        storePtr,
        paramsPtr,
        paramCount,
        resultsPtr,
        maxResults);
  }

  // =============================================================================
  // Panama Linker Operations
  // =============================================================================

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
   * Defines an unchecked host function in the linker (Panama FFI version).
   *
   * <p>This uses {@code Func::new_unchecked} internally, which skips type-checking at call time for
   * better performance. The caller is responsible for ensuring correct types.
   *
   * @param linkerPtr pointer to the linker
   * @param moduleName pointer to the module name string
   * @param name pointer to the function name string
   * @param paramTypes pointer to array of parameter type codes
   * @param paramCount number of parameters
   * @param returnTypes pointer to array of return type codes
   * @param returnCount number of returns
   * @param callbackFn callback function pointer
   * @param callbackId callback ID
   * @return 0 on success, non-zero on error
   */
  public int panamaLinkerDefineHostFunctionUnchecked(
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
        "wasmtime4j_panama_linker_define_host_function_unchecked",
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
   * Defines a module in the linker, instantiating it and registering all exports (Panama FFI
   * version). This corresponds to Wasmtime's {@code Linker::module()} which handles WASI commands
   * specially.
   *
   * @param linkerPtr pointer to the linker
   * @param storePtr pointer to the store
   * @param moduleNamePtr pointer to the module name string
   * @param modulePtr pointer to the module
   * @return 0 on success, negative error code on failure
   */
  public int panamaLinkerModule(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment moduleNamePtr,
      final MemorySegment modulePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(moduleNamePtr, "moduleNamePtr");
    validatePointer(modulePtr, "modulePtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_module",
        Integer.class,
        linkerPtr,
        storePtr,
        moduleNamePtr,
        modulePtr);
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
   * Aliases all definitions from one module name to another.
   *
   * @param linkerPtr pointer to the linker
   * @param modulePtr source module name (C string)
   * @param asModulePtr destination module name (C string)
   * @return 0 on success, non-zero error code on failure
   */
  public int panamaLinkerAliasModule(
      final MemorySegment linkerPtr,
      final MemorySegment modulePtr,
      final MemorySegment asModulePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(modulePtr, "modulePtr");
    validatePointer(asModulePtr, "asModulePtr");

    return callNativeFunction(
        "wasmtime4j_panama_linker_alias_module", Integer.class, linkerPtr, modulePtr, asModulePtr);
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
        "wasmtime4j_panama_linker_get_by_import",
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
   * Iterates over all definitions in the linker.
   *
   * <p>Stores the results in thread-local storage for retrieval with {@link #linkerIterGet}.
   *
   * @param linkerPtr the linker pointer
   * @param storePtr the store pointer
   * @param outCountPtr pointer to write the definition count
   * @return 0 on success, -1 on error
   */
  public int linkerIter(
      final MemorySegment linkerPtr,
      final MemorySegment storePtr,
      final MemorySegment outCountPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_linker_iter", Integer.class, linkerPtr, storePtr, outCountPtr);
  }

  /**
   * Gets a specific definition from the last iter() call.
   *
   * @param index the definition index
   * @param outModuleNamePtr buffer for the module name
   * @param moduleNameCapacity capacity of the module name buffer
   * @param outItemNamePtr buffer for the item name
   * @param itemNameCapacity capacity of the item name buffer
   * @param outTypeCodePtr pointer for the type code
   * @return 0 on success, -1 on error
   */
  public int linkerIterGet(
      final int index,
      final MemorySegment outModuleNamePtr,
      final int moduleNameCapacity,
      final MemorySegment outItemNamePtr,
      final int itemNameCapacity,
      final MemorySegment outTypeCodePtr) {
    return callNativeFunction(
        "wasmtime4j_panama_linker_iter_get",
        Integer.class,
        index,
        outModuleNamePtr,
        moduleNameCapacity,
        outItemNamePtr,
        itemNameCapacity,
        outTypeCodePtr);
  }

  // =============================================================================
  // InstancePre Operations
  // =============================================================================

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
  /**
   * Asynchronously instantiates from an InstancePre.
   *
   * <p>Requires async support enabled in the engine.
   *
   * @param instancePrePtr pointer to the InstancePre
   * @param storePtr pointer to the store
   * @return pointer to the created instance, or NULL on failure
   */
  public MemorySegment instancePreInstantiateAsync(
      final MemorySegment instancePrePtr, final MemorySegment storePtr) {
    validatePointer(instancePrePtr, "instancePrePtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_instance_pre_instantiate_async",
        MemorySegment.class,
        instancePrePtr,
        storePtr);
  }

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

  // =============================================================================
  // Function Reference Operations
  // =============================================================================

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

  // =============================================================================
  // Extern Type Operations
  // =============================================================================

  /**
   * Gets the type of an extern value.
   *
   * @param externPtr the extern pointer
   * @return the extern type code (0=FUNC, 1=TABLE, 2=MEMORY, 3=GLOBAL), or -1 on error
   */
  public int externGetType(final MemorySegment externPtr) {
    validatePointer(externPtr, "externPtr");
    return callNativeFunction("wasmtime4j_panama_linker_get_extern_type", Integer.class, externPtr);
  }

  // =============================================================================
  // Call Hook Operations
  // =============================================================================

  /**
   * Sets a call hook with a function pointer callback on the store.
   *
   * <p>The callback is invoked on every transition between host and WebAssembly code. The callback
   * receives the callback ID and hook type (0=CallingWasm, 1=ReturningFromWasm, 2=CallingHost,
   * 3=ReturningFromHost) and returns 0 to continue or non-zero to trap.
   *
   * @param storePtr the store pointer
   * @param callbackFn the upcall stub function pointer
   * @param callbackId the callback identifier for dispatch
   * @return 0 on success, non-zero on error
   */
  public int storeSetCallHookFn(
      final MemorySegment storePtr, final MemorySegment callbackFn, final long callbackId) {
    validatePointer(storePtr, "storePtr");
    validatePointer(callbackFn, "callbackFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_call_hook_fn",
        Integer.class,
        storePtr,
        callbackFn,
        callbackId);
  }

  // =============================================================================
  // Binding Registrations
  // =============================================================================

  private void initializeBindings() {
    initializeInstanceBindings();
    initializeCallerBindings();
    initializeFunctionBindings();
    initializeLinkerBindings();
    initializeInstancePreBindings();
    initializeFunctionReferenceBindings();
    initializeExternAndCallHookBindings();
    initializeTagExnRefAndHostFunctionBindings();
  }

  private void initializeInstanceBindings() {
    addFunctionBinding(
        "wasmtime4j_instance_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_create_with_imports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_get_module_export",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_module_get_module_export",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_call_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_instance_get_memory_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_get_shared_memory_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_get_memory_size_pages",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_grow_memory",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_get_global_type",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_has_global_export",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_get_global_value",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_set_global_value",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_DOUBLE,
            ValueLayout.JAVA_DOUBLE,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_instance_get_table_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_get_tag_by_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_get_global_wrapped",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
  }

  private void initializeCallerBindings() {
    addFunctionBinding(
        "wasmtime4j_panama_caller_get_fuel_remaining",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_caller_add_fuel",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_caller_set_fuel",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

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

    addFunctionBinding(
        "wasmtime4j_panama_caller_set_fuel_async_yield_interval",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_caller_debug_exit_frames",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
  }

  private void initializeFunctionBindings() {
    addFunctionBinding(
        "wasmtime4j_panama_func_call",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_func_type",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_func_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_func_type_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_func_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // name (c_char*)
            ValueLayout.ADDRESS)); // func_ptr out

    addFunctionBinding(
        "wasmtime4j_panama_func_to_raw",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // raw funcref value (i64)
            ValueLayout.ADDRESS, // func_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_func_from_raw",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // func_ptr out
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // raw funcref value (i64)
  }

  private void initializeLinkerBindings() {
    addFunctionBinding(
        "wasmtime4j_panama_linker_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_host_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_host_function_unchecked",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_host_function_async",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_global",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_memory",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_memory_from_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_table",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_module",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_alias",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_alias_module",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_linker_instantiate",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_linker_add_wasi",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_allow_shadowing",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_linker_allow_unknown_exports",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_unknown_imports_as_traps",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_unknown_imports_as_default_values",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_get_by_import",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_get_default",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_get_extern_type",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_extern_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_define_name",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_iter",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_linker_iter_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));
  }

  private void initializeInstancePreBindings() {
    addFunctionBinding(
        "wasmtime4j_linker_instantiate_pre",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_pre_instantiate",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_pre_is_valid",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_pre_instance_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_pre_preparation_time_ns",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_pre_avg_instantiation_time_ns",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_pre_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_instance_pre_instantiate_async",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // Panama-prefixed variants
    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_instantiate",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_instance_pre_instantiate_async",
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
  }

  private void initializeFunctionReferenceBindings() {
    addFunctionBinding(
        "wasmtime4j_panama_function_reference_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS));
  }

  private void initializeExternAndCallHookBindings() {
    addFunctionBinding(
        "wasmtime4j_panama_linker_get_extern_type",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_store_set_call_hook_fn",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: error code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG)); // callback_id
  }

  // ===== Tag Methods =====

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

  /**
   * Gets a single field value from an exception reference by index.
   *
   * @param exnRefPtr the exception reference pointer
   * @param storePtr the store pointer
   * @param index the zero-based field index
   * @param outType output pointer for the type code
   * @param outValueI64 output pointer for integer/long values
   * @param outValueF64 output pointer for float/double values
   * @return 0 on success, non-zero on failure
   */
  public int exnRefGetField(
      final MemorySegment exnRefPtr,
      final MemorySegment storePtr,
      final int index,
      final MemorySegment outType,
      final MemorySegment outValueI64,
      final MemorySegment outValueF64) {
    return callNativeFunction(
        "wasmtime4j_panama_exnref_get_field",
        Integer.class,
        exnRefPtr,
        storePtr,
        index,
        outType,
        outValueI64,
        outValueF64);
  }

  /**
   * Gets the number of fields in an exception reference.
   *
   * @param exnRefPtr the exception reference pointer
   * @param storePtr the store pointer
   * @return the field count, or -1 on error
   */
  public int exnRefFieldCount(final MemorySegment exnRefPtr, final MemorySegment storePtr) {
    return callNativeFunction(
        "wasmtime4j_panama_exnref_field_count", Integer.class, exnRefPtr, storePtr);
  }

  /**
   * Creates a new ExnRef from a tag and field values.
   *
   * @param storePtr the store pointer
   * @param tagPtr the tag pointer
   * @param fieldCount the number of fields
   * @param fieldTypesPtr pointer to int array of field type codes
   * @param fieldI64ValuesPtr pointer to long array of integer field values
   * @param fieldF64ValuesPtr pointer to double array of float field values
   * @return the new ExnRef pointer, or NULL on failure
   */
  public MemorySegment exnRefCreate(
      final MemorySegment storePtr,
      final MemorySegment tagPtr,
      final int fieldCount,
      final MemorySegment fieldTypesPtr,
      final MemorySegment fieldI64ValuesPtr,
      final MemorySegment fieldF64ValuesPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(tagPtr, "tagPtr");
    return callNativeFunction(
        "wasmtime4j_panama_exnref_create",
        MemorySegment.class,
        storePtr,
        tagPtr,
        fieldCount,
        fieldTypesPtr,
        fieldI64ValuesPtr,
        fieldF64ValuesPtr);
  }

  /**
   * Converts an ExnRef to its raw u32 representation.
   *
   * @param exnRefPtr the exception reference pointer
   * @param storePtr the store pointer
   * @return the raw value as a long, or -1 on failure
   */
  public long exnRefToRaw(final MemorySegment exnRefPtr, final MemorySegment storePtr) {
    validatePointer(exnRefPtr, "exnRefPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_exnref_to_raw", Long.class, exnRefPtr, storePtr);
  }

  /**
   * Creates an ExnRef from a raw u32 representation.
   *
   * @param storePtr the store pointer
   * @param raw the raw u32 value
   * @return the ExnRef pointer, or NULL if raw is 0 or invalid
   */
  public MemorySegment exnRefFromRaw(final MemorySegment storePtr, final int raw) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_exnref_from_raw", MemorySegment.class, storePtr, raw);
  }

  /**
   * Checks if an ExnRef matches a given heap type.
   *
   * @param exnRefPtr the exception reference pointer
   * @param storePtr the store pointer
   * @param heapTypeCode the heap type code (matching Java HeapType enum ordinal)
   * @return 1 if matches, 0 if not, -1 on error
   */
  public int exnRefMatchesTy(
      final MemorySegment exnRefPtr, final MemorySegment storePtr, final int heapTypeCode) {
    validatePointer(exnRefPtr, "exnRefPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_exnref_matches_ty", Integer.class, exnRefPtr, storePtr, heapTypeCode);
  }

  /**
   * Converts an ExternRef (identified by its i64 data) to a raw u32 GC heap index.
   *
   * @param storePtr the store pointer
   * @param externRefData the i64 data stored in the ExternRef
   * @return the raw value as a long, or -1 on failure
   */
  public long externRefToRaw(final MemorySegment storePtr, final long externRefData) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_externref_to_raw", Long.class, storePtr, externRefData);
  }

  /**
   * Creates an ExternRef from a raw u32 GC heap index.
   *
   * @param storePtr the store pointer
   * @param raw the raw u32 value
   * @return the i64 data from the ExternRef, or -1 on failure
   */
  public long externRefFromRaw(final MemorySegment storePtr, final int raw) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_externref_from_raw", Long.class, storePtr, raw);
  }

  // ===== Host Function MethodHandle Getters =====

  /**
   * Gets the method handle for creating a host function in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreCreateHostFunction() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_store_create_host_function");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  /**
   * Gets the method handle for creating an unchecked host function in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreCreateHostFunctionUnchecked() {
    FunctionBinding binding =
        getFunctionBinding("wasmtime4j_panama_store_create_host_function_unchecked");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  /**
   * Gets the method handle for destroying a host function.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaDestroyHostFunction() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_destroy_host_function");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  private void initializeTagExnRefAndHostFunctionBindings() {
    // Tag bindings
    addFunctionBinding(
        "wasmtime4j_panama_tag_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_tag_get_param_types",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_tag_get_return_types",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_tag_types_free",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_tag_equals",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_tag_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // ExnRef bindings
    addFunctionBinding(
        "wasmtime4j_panama_exnref_get_tag",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_is_valid",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_get_field",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_field_count",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_to_raw",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_from_raw",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_exnref_matches_ty",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_externref_to_raw",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_externref_from_raw",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    // Host function bindings
    addFunctionBinding(
        "wasmtime4j_panama_store_create_host_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_store_create_host_function_unchecked",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_destroy_host_function",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
  }
}
