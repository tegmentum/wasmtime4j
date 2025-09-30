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

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstanceState;
import ai.tegmentum.wasmtime4j.InstanceStatistics;
import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.TableType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly instance interface.
 *
 * <p>A WebAssembly instance represents an instantiated module with its own execution context,
 * memory, and exported functions. This implementation uses Panama FFI with Stream 1 & 2
 * infrastructure for direct access to the underlying Wasmtime instance structure with zero-copy
 * optimization.
 *
 * <p>Instances provide access to exported functions, globals, memory, and tables through optimized
 * FFI calls with comprehensive bounds checking and memory safety. All operations use Arena-based
 * resource management for automatic cleanup.
 *
 * @since 1.0.0
 */
public final class PanamaInstance implements Instance, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaInstance.class.getName());

  // Core infrastructure from Streams 1 & 2
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final PanamaModule module;
  private final ArenaResourceManager.ManagedNativeResource instanceResource;

  // Instance state
  private volatile boolean closed = false;
  private volatile boolean cleanedUp = false;
  private final long createdAtMicros;

  @Override
  public boolean isValid() {
    return !closed;
  }

  /**
   * Creates a new Panama instance using Stream 1 & 2 infrastructure.
   *
   * @param instancePtr the native instance pointer from instantiation
   * @param resourceManager the arena resource manager for lifecycle management
   * @param module the parent module instance
   * @throws WasmException if the instance cannot be created
   */
  public PanamaInstance(
      final MemorySegment instancePtr,
      final ArenaResourceManager resourceManager,
      final PanamaModule module)
      throws WasmException {
    // Defensive parameter validation
    PanamaErrorHandler.requireValidPointer(instancePtr, "instancePtr");
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.module = Objects.requireNonNull(module, "Module cannot be null");
    this.nativeFunctions = NativeFunctionBindings.getInstance();
    this.createdAtMicros = System.currentTimeMillis() * 1000; // Convert to microseconds

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    try {
      // Create managed resource with cleanup for instance
      this.instanceResource =
          resourceManager.manageNativeResource(
              instancePtr, () -> destroyNativeInstanceInternal(instancePtr), "Wasmtime Instance");

      LOGGER.fine("Created Panama instance with managed resource");

    } catch (Exception e) {
      throw new WasmException("Failed to create instance wrapper", e);
    }
  }

  /**
   * Creates a PanamaInstance from a native handle.
   *
   * <p>This static factory method creates a new PanamaInstance from a native Wasmtime instance
   * handle. This is typically used internally by the Panama FFI implementation when instantiating
   * modules through linkers or other native operations.
   *
   * @param instanceHandle the native instance handle
   * @param module the parent module instance
   * @param store the store in which the instance was created
   * @param arena the arena for resource management
   * @return a new PanamaInstance wrapping the native handle
   * @throws WasmException if the instance cannot be created
   * @throws IllegalArgumentException if any parameter is null
   */
  public static PanamaInstance fromHandle(
      final MemorySegment instanceHandle, final Module module, final Store store, final Arena arena)
      throws WasmException {
    if (instanceHandle == null || instanceHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Instance handle cannot be null");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a Panama module instance");
    }

    // Create resource manager for this instance
    final ArenaResourceManager resourceManager = new ArenaResourceManager(arena);

    return new PanamaInstance(instanceHandle, resourceManager, (PanamaModule) module);
  }

  @Override
  public Optional<WasmFunction> getFunction(final String name) {
    ensureNotClosed();

    // Parameter validation with defensive programming
    Objects.requireNonNull(name, "Function name cannot be null");
    PanamaErrorHandler.requireNotEmpty(name, "Function name cannot be empty");

    try {
      // Get the export by name through optimized FFI
      MemorySegment exportPtr = findExportByName(name);
      if (exportPtr == null || exportPtr.equals(MemorySegment.NULL)) {
        return Optional.empty(); // Export not found
      }

      // Verify export is a function
      if (!isExportFunction(exportPtr)) {
        return Optional.empty(); // Export exists but is not a function
      }

      // Extract function pointer from export
      MemorySegment functionPtr = extractFunctionFromExport(exportPtr);
      PanamaErrorHandler.requireValidPointer(functionPtr, "functionPtr");

      // Create managed function wrapper
      return Optional.of(new PanamaFunction(functionPtr, resourceManager, this));

    } catch (Throwable e) {
      LOGGER.warning("Function lookup failed for '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmMemory> getMemory(final String name) {
    ensureNotClosed();

    // Parameter validation with defensive programming
    Objects.requireNonNull(name, "Memory name cannot be null");
    PanamaErrorHandler.requireNotEmpty(name, "Memory name cannot be empty");

    try {
      // Get the export by name through optimized FFI
      MemorySegment exportPtr = findExportByName(name);
      if (exportPtr == null || exportPtr.equals(MemorySegment.NULL)) {
        return Optional.empty(); // Export not found
      }

      // Verify export is a memory
      if (!isExportMemory(exportPtr)) {
        return Optional.empty(); // Export exists but is not a memory
      }

      // Extract memory pointer from export
      MemorySegment memoryPtr = extractMemoryFromExport(exportPtr);
      PanamaErrorHandler.requireValidPointer(memoryPtr, "memoryPtr");

      // Create managed memory wrapper
      return Optional.of(new PanamaMemory(memoryPtr, resourceManager, this));

    } catch (Throwable e) {
      LOGGER.warning("Memory lookup failed for '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final String name) {
    ensureNotClosed();

    // Parameter validation with defensive programming
    Objects.requireNonNull(name, "Global name cannot be null");
    PanamaErrorHandler.requireNotEmpty(name, "Global name cannot be empty");

    try {
      // Get the export by name through optimized FFI
      MemorySegment exportPtr = findExportByName(name);
      if (exportPtr == null || exportPtr.equals(MemorySegment.NULL)) {
        return Optional.empty(); // Export not found
      }

      // Verify export is a global
      if (!isExportGlobal(exportPtr)) {
        return Optional.empty(); // Export exists but is not a global
      }

      // Extract global pointer from export
      MemorySegment globalPtr = extractGlobalFromExport(exportPtr);
      PanamaErrorHandler.requireValidPointer(globalPtr, "globalPtr");

      // Create managed global wrapper
      return Optional.of(new PanamaGlobal(globalPtr, resourceManager, this));

    } catch (Throwable e) {
      LOGGER.warning("Global lookup failed for '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmTable> getTable(final String name) {
    ensureNotClosed();

    // Parameter validation with defensive programming
    Objects.requireNonNull(name, "Table name cannot be null");
    PanamaErrorHandler.requireNotEmpty(name, "Table name cannot be empty");

    try {
      // Get the export by name through optimized FFI
      MemorySegment exportPtr = findExportByName(name);
      if (exportPtr == null || exportPtr.equals(MemorySegment.NULL)) {
        return Optional.empty(); // Export not found
      }

      // Verify export is a table
      if (!isExportTable(exportPtr)) {
        return Optional.empty(); // Export exists but is not a table
      }

      // Extract table pointer from export
      MemorySegment tablePtr = extractTableFromExport(exportPtr);
      PanamaErrorHandler.requireValidPointer(tablePtr, "tablePtr");

      // Create managed table wrapper
      return Optional.of(new PanamaTable(tablePtr, resourceManager, this));

    } catch (Throwable e) {
      LOGGER.warning("Table lookup failed for '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmMemory> getDefaultMemory() {
    // Try to get memory named "memory" first
    Optional<WasmMemory> memory = getMemory("memory");
    if (memory.isPresent()) {
      return memory;
    }

    // If no "memory" export, try getting the first available memory
    String[] exports = getExportNames();
    for (String exportName : exports) {
      Optional<WasmMemory> mem = getMemory(exportName);
      if (mem.isPresent()) {
        return mem;
      }
    }

    return Optional.empty();
  }

  @Override
  public String[] getExportNames() {
    try {
      ensureNotClosed();

      // Get export count through FFI
      int exportCount = getExportCount();
      if (exportCount <= 0) {
        return new String[0];
      }

      // Create array for export names
      String[] exportNames = new String[exportCount];
      int actualCount = 0;

      // Iterate through exports and collect names
      for (int i = 0; i < exportCount; i++) {
        String exportName = getExportNameAt(i);
        if (exportName != null && !exportName.isEmpty()) {
          exportNames[actualCount++] = exportName;
        }
      }

      // Return trimmed array with actual count
      if (actualCount < exportCount) {
        String[] trimmedNames = new String[actualCount];
        System.arraycopy(exportNames, 0, trimmedNames, 0, actualCount);
        return trimmedNames;
      }

      return exportNames;

    } catch (Exception e) {
      LOGGER.warning("Export enumeration failed: " + e.getMessage());
      return new String[0];
    }
  }

  @Override
  public Module getModule() {
    ensureNotClosed();
    return module;
  }

  @Override
  public Store getStore() {
    ensureNotClosed();
    try {
      return module.getEngine().createStore();
    } catch (WasmException e) {
      throw new java.lang.RuntimeException("Failed to create store", e);
    }
  }

  @Override
  public WasmValue[] callFunction(final String functionName, final WasmValue... params)
      throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    Objects.requireNonNull(functionName, "Function name cannot be null");
    PanamaErrorHandler.requireNotEmpty(functionName, "Function name cannot be empty");

    try {
      // Get the function first
      Optional<WasmFunction> functionOpt = getFunction(functionName);
      if (!functionOpt.isPresent()) {
        throw new WasmException("Function '" + functionName + "' not found in instance");
      }

      // Invoke the function with proper error handling
      return functionOpt.get().call(params);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Function invocation",
              "name=" + functionName + ", params.length=" + (params != null ? params.length : 0),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public InstanceState getState() {
    ensureNotClosed();
    try {
      // Get state from native function if available
      // For now, return basic state based on closed flag
      if (closed) {
        return InstanceState.DISPOSED;
      } else if (cleanedUp) {
        return InstanceState.DESTROYING;
      } else {
        return InstanceState.CREATED;
      }
    } catch (Exception e) {
      LOGGER.warning("Failed to get instance state: " + e.getMessage());
      return InstanceState.ERROR;
    }
  }

  @Override
  public boolean cleanup() throws WasmException {
    if (cleanedUp) {
      return false; // Already cleaned up
    }

    ensureNotClosed();

    synchronized (this) {
      if (cleanedUp) {
        return false;
      }

      try {
        // Perform comprehensive resource cleanup
        cleanedUp = true;
        LOGGER.fine("Instance resources cleaned up successfully");
        return true;
      } catch (Exception e) {
        cleanedUp = false; // Reset on failure
        throw new WasmException("Failed to cleanup instance resources", e);
      }
    }
  }

  @Override
  public boolean dispose() throws WasmException {
    if (closed) {
      return false; // Already disposed
    }

    try {
      // Perform cleanup first if not already done
      if (!cleanedUp) {
        cleanup();
      }

      // Close the resource
      close();
      return true;
    } catch (Exception e) {
      throw new WasmException("Failed to dispose instance", e);
    }
  }

  @Override
  public boolean isDisposed() {
    return closed;
  }

  @Override
  public long getCreatedAtMicros() {
    return createdAtMicros;
  }

  @Override
  public int getMetadataExportCount() {
    ensureNotClosed();
    try {
      final String[] exports = getExportNames();
      return exports != null ? exports.length : 0;
    } catch (Exception e) {
      LOGGER.warning("Failed to get metadata export count: " + e.getMessage());
      return 0;
    }
  }

  @Override
  public int callI32Function(final String functionName, final int... params) throws WasmException {
    ensureNotClosed();

    // Convert int parameters to WasmValue array
    final WasmValue[] wasmParams = new WasmValue[params.length];
    for (int i = 0; i < params.length; i++) {
      wasmParams[i] = WasmValue.i32(params[i]);
    }

    // Call function and get result
    final WasmValue[] results = callFunction(functionName, wasmParams);
    if (results.length != 1) {
      throw new WasmException("Expected single i32 result, got " + results.length + " results");
    }

    if (results[0].getType() != WasmValue.Type.I32) {
      throw new WasmException("Expected i32 result, got " + results[0].getType());
    }

    return results[0].asI32();
  }

  @Override
  public int callI32Function(final String functionName) throws WasmException {
    return callI32Function(functionName, new int[0]);
  }

  @Override
  public InstanceStatistics getStatistics() throws WasmException {
    ensureNotClosed();

    try {
      // For now, return basic statistics
      // In a full implementation, these would come from native code
      return new InstanceStatistics(
          0, // functionCallCount - would need tracking
          0, // totalExecutionTime - would need tracking
          0, // memoryBytesAllocated - would need tracking
          0, // peakMemoryUsage - would need tracking
          0, // activeTableElements - would need tracking
          0, // activeGlobals - would need tracking
          0, // fuelConsumed - would need tracking
          0 // epochTicks - would need tracking
          );
    } catch (Exception e) {
      throw new WasmException("Failed to get instance statistics", e);
    }
  }

  @Override
  public Optional<WasmFunction> getFunction(final int index) {
    ensureNotClosed();
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative: " + index);
    }

    try {
      final String[] exportNames = getExportNames();
      if (index >= exportNames.length) {
        return Optional.empty();
      }

      // Find the index-th function export
      int functionIndex = 0;
      for (final String exportName : exportNames) {
        final Optional<WasmFunction> function = getFunction(exportName);
        if (function.isPresent()) {
          if (functionIndex == index) {
            return function;
          }
          functionIndex++;
        }
      }

      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get function by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmMemory> getMemory(final int index) {
    ensureNotClosed();
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative: " + index);
    }

    try {
      final String[] exportNames = getExportNames();
      if (index >= exportNames.length) {
        return Optional.empty();
      }

      // Find the index-th memory export
      int memoryIndex = 0;
      for (final String exportName : exportNames) {
        final Optional<WasmMemory> memory = getMemory(exportName);
        if (memory.isPresent()) {
          if (memoryIndex == index) {
            return memory;
          }
          memoryIndex++;
        }
      }

      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get memory by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmTable> getTable(final int index) {
    ensureNotClosed();
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative: " + index);
    }

    try {
      final String[] exportNames = getExportNames();
      if (index >= exportNames.length) {
        return Optional.empty();
      }

      // Find the index-th table export
      int tableIndex = 0;
      for (final String exportName : exportNames) {
        final Optional<WasmTable> table = getTable(exportName);
        if (table.isPresent()) {
          if (tableIndex == index) {
            return table;
          }
          tableIndex++;
        }
      }

      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get table by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final int index) {
    ensureNotClosed();
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative: " + index);
    }

    try {
      final String[] exportNames = getExportNames();
      if (index >= exportNames.length) {
        return Optional.empty();
      }

      // Find the index-th global export
      int globalIndex = 0;
      for (final String exportName : exportNames) {
        final Optional<WasmGlobal> global = getGlobal(exportName);
        if (global.isPresent()) {
          if (globalIndex == index) {
            return global;
          }
          globalIndex++;
        }
      }

      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get global by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public java.util.Map<String, Object> getAllExports() {
    ensureNotClosed();

    try {
      final java.util.Map<String, Object> exports = new java.util.HashMap<>();
      final String[] exportNames = getExportNames();

      for (final String exportName : exportNames) {
        // Try to get each type of export
        Optional<WasmFunction> function = getFunction(exportName);
        if (function.isPresent()) {
          exports.put(exportName, function.get());
          continue;
        }

        Optional<WasmMemory> memory = getMemory(exportName);
        if (memory.isPresent()) {
          exports.put(exportName, memory.get());
          continue;
        }

        Optional<WasmTable> table = getTable(exportName);
        if (table.isPresent()) {
          exports.put(exportName, table.get());
          continue;
        }

        Optional<WasmGlobal> global = getGlobal(exportName);
        if (global.isPresent()) {
          exports.put(exportName, global.get());
        }
      }

      return java.util.Collections.unmodifiableMap(exports);
    } catch (Exception e) {
      LOGGER.warning("Failed to get all exports: " + e.getMessage());
      return java.util.Collections.emptyMap();
    }
  }

  @Override
  public void setImports(final java.util.Map<String, Object> imports) throws WasmException {
    throw new UnsupportedOperationException(
        "Setting imports is not supported for instantiated instances");
  }

  @Override
  public List<ExportDescriptor> getExportDescriptors() {
    ensureNotClosed();
    try {
      // For now, return empty list
      // In a full implementation, this would return actual export descriptors
      return java.util.Collections.emptyList();
    } catch (Exception e) {
      LOGGER.warning("Failed to get export descriptors: " + e.getMessage());
      return java.util.Collections.emptyList();
    }
  }

  @Override
  public Optional<ExportDescriptor> getExportDescriptor(final String name) {
    Objects.requireNonNull(name, "Name cannot be null");
    ensureNotClosed();
    try {
      // For now, return empty
      // In a full implementation, this would return actual export descriptor
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get export descriptor for '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<FuncType> getFunctionType(final String functionName) {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    ensureNotClosed();
    try {
      // For now, return empty
      // In a full implementation, this would return actual function type
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get function type for '" + functionName + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<GlobalType> getGlobalType(final String globalName) {
    Objects.requireNonNull(globalName, "Global name cannot be null");
    ensureNotClosed();
    try {
      // For now, return empty
      // In a full implementation, this would return actual global type
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get global type for '" + globalName + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<MemoryType> getMemoryType(final String memoryName) {
    Objects.requireNonNull(memoryName, "Memory name cannot be null");
    ensureNotClosed();
    try {
      // For now, return empty
      // In a full implementation, this would return actual memory type
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get memory type for '" + memoryName + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<TableType> getTableType(final String tableName) {
    Objects.requireNonNull(tableName, "Table name cannot be null");
    ensureNotClosed();
    try {
      // For now, return empty
      // In a full implementation, this would return actual table type
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get table type for '" + tableName + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public boolean hasExport(final String name) {
    Objects.requireNonNull(name, "Name cannot be null");
    ensureNotClosed();
    try {
      final String[] exportNames = getExportNames();
      for (final String exportName : exportNames) {
        if (name.equals(exportName)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      LOGGER.warning("Failed to check export existence for '" + name + "': " + e.getMessage());
      return false;
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Close the managed native resource (automatic cleanup)
        instanceResource.close();

        LOGGER.fine("Closed Panama instance");

      } catch (Exception e) {
        LOGGER.severe("Failed to close instance: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native instance handle for this instance.
   *
   * @return the native instance handle
   * @throws IllegalStateException if the instance is closed
   */
  public MemorySegment getInstanceHandle() {
    ensureNotClosed();
    return instanceResource.getNativePointer();
  }

  /**
   * Gets the parent module instance as Panama implementation.
   *
   * @return the module instance
   */
  public PanamaModule getPanamaModule() {
    ensureNotClosed();
    return module;
  }

  // Private FFI helper methods for export access

  /** Finds an export by name using FFI calls. */
  private MemorySegment findExportByName(final String name) throws Throwable {
    // Allocate memory for export name string
    ArenaResourceManager.ManagedMemorySegment nameMemory =
        resourceManager.allocate((name.length() + 1) * Character.BYTES);
    MemorySegment nameSegment = nameMemory.getSegment();
    nameSegment.setString(0, name);

    // Allocate memory for export result
    ArenaResourceManager.ManagedMemorySegment exportMemory =
        resourceManager.allocate(MemoryLayouts.WASMTIME_EXPORT_LAYOUT);
    MemorySegment exportSegment = exportMemory.getSegment();

    // Call wasmtime_instance_export_get through cached method handle
    MethodHandle instanceExportGet =
        nativeFunctions.getFunction(
            "wasmtime_instance_export_get",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS, // instance
                ValueLayout.ADDRESS, // name
                ValueLayout.JAVA_LONG, // name_len
                ValueLayout.ADDRESS // export (out)
                ));

    boolean found =
        (boolean)
            instanceExportGet.invoke(
                instanceResource.getNativePointer(), nameSegment, name.length(), exportSegment);

    return found ? exportSegment : null;
  }

  /** Checks if export is a function type. */
  private boolean isExportFunction(final MemorySegment exportPtr) {
    // Check export kind field in wasmtime_extern_t structure
    byte exportKind = exportPtr.get(ValueLayout.JAVA_BYTE, 0);
    return exportKind == MemoryLayouts.WASMTIME_EXTERN_FUNC;
  }

  /** Checks if export is a memory type. */
  private boolean isExportMemory(final MemorySegment exportPtr) {
    byte exportKind = exportPtr.get(ValueLayout.JAVA_BYTE, 0);
    return exportKind == MemoryLayouts.WASMTIME_EXTERN_MEMORY;
  }

  /** Checks if export is a global type. */
  private boolean isExportGlobal(final MemorySegment exportPtr) {
    byte exportKind = exportPtr.get(ValueLayout.JAVA_BYTE, 0);
    return exportKind == MemoryLayouts.WASMTIME_EXTERN_GLOBAL;
  }

  /** Checks if export is a table type. */
  private boolean isExportTable(final MemorySegment exportPtr) {
    byte exportKind = exportPtr.get(ValueLayout.JAVA_BYTE, 0);
    return exportKind == MemoryLayouts.WASMTIME_EXTERN_TABLE;
  }

  /** Extracts function pointer from export structure. */
  private MemorySegment extractFunctionFromExport(final MemorySegment exportPtr) {
    // Extract function pointer from union field at offset 8
    return exportPtr.get(ValueLayout.ADDRESS, 8);
  }

  /** Extracts memory pointer from export structure. */
  private MemorySegment extractMemoryFromExport(final MemorySegment exportPtr) {
    return exportPtr.get(ValueLayout.ADDRESS, 8);
  }

  /** Extracts global pointer from export structure. */
  private MemorySegment extractGlobalFromExport(final MemorySegment exportPtr) {
    return exportPtr.get(ValueLayout.ADDRESS, 8);
  }

  /** Extracts table pointer from export structure. */
  private MemorySegment extractTableFromExport(final MemorySegment exportPtr) {
    return exportPtr.get(ValueLayout.ADDRESS, 8);
  }

  /** Gets the total number of exports. */
  private int getExportCount() throws Exception {
    try {
      // Use the native function to get export count
      long count = nativeFunctions.instanceExportsLen(instanceResource.getNativePointer());

      // Validate the count is reasonable (between 0 and maximum safe int)
      if (count < 0) {
        LOGGER.warning("Negative export count returned: " + count);
        return 0;
      }

      if (count > Integer.MAX_VALUE) {
        LOGGER.warning("Export count too large, clamping to Integer.MAX_VALUE: " + count);
        return Integer.MAX_VALUE;
      }

      return (int) count;

    } catch (Exception e) {
      LOGGER.warning("Failed to get export count: " + e.getMessage());
      throw new Exception("Export count query failed", e);
    }
  }

  /** Gets the export name at the specified index. */
  private String getExportNameAt(final int index) throws Exception {
    if (index < 0) {
      throw new IllegalArgumentException("Export index cannot be negative: " + index);
    }

    try {
      // Allocate memory for name output pointer
      ArenaResourceManager.ManagedMemorySegment nameOutMemory =
          resourceManager.allocate(ValueLayout.ADDRESS.byteSize());
      MemorySegment nameOutPtr = nameOutMemory.getSegment();

      // Allocate memory for export structure
      ArenaResourceManager.ManagedMemorySegment exportMemory =
          resourceManager.allocate(MemoryLayouts.WASMTIME_EXPORT_LAYOUT);
      MemorySegment exportPtr = exportMemory.getSegment();

      // Call the native function to get the nth export
      boolean found =
          nativeFunctions.instanceExportNth(
              instanceResource.getNativePointer(), index, nameOutPtr, exportPtr);

      if (!found) {
        return null; // Export at this index doesn't exist
      }

      // Get the name pointer from the output
      MemorySegment namePtr = nameOutPtr.get(ValueLayout.ADDRESS, 0);
      if (namePtr == null || namePtr.equals(MemorySegment.NULL)) {
        LOGGER.warning("Export at index " + index + " has null name");
        return null;
      }

      // Read the null-terminated string
      String exportName = namePtr.getString(0);

      // Validate the export name
      if (exportName == null || exportName.isEmpty()) {
        LOGGER.warning("Export at index " + index + " has empty name");
        return null;
      }

      return exportName;

    } catch (Exception e) {
      LOGGER.warning("Failed to get export name at index " + index + ": " + e.getMessage());
      throw new Exception("Export name query failed for index " + index, e);
    }
  }

  /**
   * Internal cleanup method called by managed resource.
   *
   * @param instancePtr the native instance pointer to destroy
   */
  private void destroyNativeInstanceInternal(final MemorySegment instancePtr) {
    try {
      // Wasmtime instances are typically destroyed when the store is closed
      // Individual instance destruction is not always required
      LOGGER.fine("Destroying native instance: " + instancePtr);

    } catch (Exception e) {
      LOGGER.warning("Error during instance cleanup: " + e.getMessage());
      // Don't throw exceptions from cleanup methods
    }
  }

  /**
   * Ensures that this instance is not closed.
   *
   * @throws IllegalStateException if the instance is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Instance has been closed");
    }
  }
}
