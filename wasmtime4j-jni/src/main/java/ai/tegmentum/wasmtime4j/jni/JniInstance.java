package ai.tegmentum.wasmtime4j.jni;

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
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Instance interface.
 *
 * <p>This class represents an instantiated WebAssembly module and provides access to its functions,
 * memories, tables, and globals through JNI calls to the native Wasmtime library. An instance is
 * the runtime representation of a compiled module.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 */
public final class JniInstance extends JniResource implements Instance {

  private static final Logger LOGGER = Logger.getLogger(JniInstance.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniInstance: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Flag to track if this instance has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Flag to track if resources have been cleaned up. */
  private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

  /** Creation timestamp in microseconds. */
  private final long createdAtMicros = System.currentTimeMillis() * 1000;

  /** Reference to the module used to create this instance. */
  private final Module module;

  /** Reference to the store this instance belongs to. */
  private final Store store;

  /**
   * Creates a new JNI instance with the given native handle, module, and store.
   *
   * @param nativeHandle the native instance handle
   * @param module the module used to create this instance
   * @param store the store this instance belongs to
   * @throws IllegalArgumentException if nativeHandle is 0
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if module or store is null
   */
  JniInstance(final long nativeHandle, final Module module, final Store store) {
    super(nativeHandle);
    JniValidation.requireNonNull(module, "module");
    JniValidation.requireNonNull(store, "store");
    this.module = module;
    this.store = store;
    LOGGER.fine("Created JNI instance with handle: " + nativeHandle);
  }

  /**
   * Gets a function export by name.
   *
   * @param name the name of the exported function
   * @return the function wrapper, or empty if not found
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmFunction> getFunction(final String name) {
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    try {
      final JniStore jniStore = (JniStore) store;
      final long functionHandle =
          nativeGetFunction(getNativeHandle(), jniStore.getNativeHandle(), name);
      if (functionHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniFunction(functionHandle, name));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting function: " + name, e);
    }
  }

  @Override
  public Optional<WasmFunction> getFunction(final int index) {
    ensureNotClosed();

    if (index < 0) {
      return Optional.empty();
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
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to get function by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Gets a memory export by name.
   *
   * @param name the name of the exported memory
   * @return the memory wrapper, or empty if not found
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmMemory> getMemory(final String name) {
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long memoryHandle =
          nativeGetMemory(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (memoryHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniMemory(memoryHandle));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory: " + name, e);
    }
  }

  @Override
  public Optional<WasmMemory> getMemory(final int index) {
    ensureNotClosed();

    if (index < 0) {
      return Optional.empty();
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
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to get memory by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Gets a table export by name.
   *
   * @param name the name of the exported table
   * @return the table wrapper, or empty if not found
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmTable> getTable(final String name) {
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long tableHandle =
          nativeGetTable(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (tableHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTable(tableHandle, (JniStore) store));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table: " + name, e);
    }
  }

  @Override
  public Optional<WasmTable> getTable(final int index) {
    ensureNotClosed();

    if (index < 0) {
      return Optional.empty();
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
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to get table by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<TableType> getTableType(final String tableName) {
    JniValidation.requireNonBlank(tableName, "tableName");
    ensureNotClosed();

    try {
      final Optional<WasmTable> table = getTable(tableName);
      if (!table.isPresent()) {
        return Optional.empty();
      }
      // Get table type information from the native table
      // For now, return a basic table type - this would need native implementation
      return Optional.empty(); // TODO: Implement native getTableType
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table type: " + tableName, e);
    }
  }

  @Override
  public Optional<MemoryType> getMemoryType(final String memoryName) {
    JniValidation.requireNonBlank(memoryName, "memoryName");
    ensureNotClosed();

    try {
      final Optional<WasmMemory> memory = getMemory(memoryName);
      if (!memory.isPresent()) {
        return Optional.empty();
      }
      // Get memory type information from the native memory
      // For now, return empty - this would need native implementation
      return Optional.empty(); // TODO: Implement native getMemoryType
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory type: " + memoryName, e);
    }
  }

  @Override
  public Optional<GlobalType> getGlobalType(final String globalName) {
    JniValidation.requireNonBlank(globalName, "globalName");
    ensureNotClosed();

    try {
      final Optional<WasmGlobal> global = getGlobal(globalName);
      if (!global.isPresent()) {
        return Optional.empty();
      }
      // Get global type information from the native global
      // For now, return empty - this would need native implementation
      return Optional.empty(); // TODO: Implement native getGlobalType
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global type: " + globalName, e);
    }
  }

  @Override
  public Optional<FuncType> getFunctionType(final String functionName) {
    JniValidation.requireNonBlank(functionName, "functionName");
    ensureNotClosed();

    try {
      final Optional<WasmFunction> function = getFunction(functionName);
      if (!function.isPresent()) {
        return Optional.empty();
      }
      // Get function type information from the native function
      // For now, return empty - this would need native implementation
      return Optional.empty(); // TODO: Implement native getFunctionType
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting function type: " + functionName, e);
    }
  }

  @Override
  public Optional<ExportDescriptor> getExportDescriptor(final String name) {
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      // TODO: Implement native getExportDescriptor to get actual export descriptor information
      // For now, return empty as this would need native implementation
      return Optional.empty();
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting export descriptor: " + name, e);
    }
  }

  @Override
  public List<ExportDescriptor> getExportDescriptors() {
    ensureNotClosed();

    try {
      // TODO: Implement native getExportDescriptors to get actual export descriptor information
      // For now, return empty list as this would need native implementation
      return Collections.emptyList();
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting export descriptors", e);
    }
  }

  /**
   * Gets a global export by name.
   *
   * @param name the name of the exported global
   * @return the global wrapper, or empty if not found
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmGlobal> getGlobal(final String name) {
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long globalHandle =
          nativeGetGlobal(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (globalHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniGlobal(globalHandle, (JniStore) store));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global: " + name, e);
    }
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final int index) {
    ensureNotClosed();

    if (index < 0) {
      return Optional.empty();
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
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to get global by index " + index + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Gets the default memory export (named "memory").
   *
   * @return the default memory wrapper, or empty if not found
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmMemory> getDefaultMemory() {
    return getMemory("memory");
  }

  /**
   * Checks if this instance has an export with the given name.
   *
   * @param name the export name to check
   * @return true if the export exists
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  public boolean hasExport(final String name) {
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      return nativeHasExport(getNativeHandle(), name);
    } catch (final Exception e) {
      LOGGER.warning("Error checking export existence: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name
   */
  @Override
  protected String getResourceType() {
    return "Instance";
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    nativeDestroyInstance(getNativeHandle());
  }

  @Override
  public String[] getExportNames() {
    ensureNotClosed();
    return nativeGetExportNames(getNativeHandle());
  }

  @Override
  public Module getModule() {
    ensureNotClosed();
    return module;
  }

  @Override
  public Store getStore() {
    ensureNotClosed();
    return store;
  }

  @Override
  public boolean isValid() {
    return !isClosed() && getNativeHandle() != 0;
  }

  @Override
  public WasmValue[] callFunction(final String functionName, final WasmValue... params)
      throws WasmException {
    JniValidation.requireNonBlank(functionName, "functionName");
    JniValidation.requireNonNull(params, "params");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;

    // Pass WasmValue array directly to preserve type information
    // Native code now returns WasmValue[] directly, preserving type information
    final Object[] nativeResults =
        nativeCallFunction(getNativeHandle(), jniStore.getNativeHandle(), functionName, params);

    if (nativeResults == null) {
      return new WasmValue[0];
    }

    // Cast to WasmValue[] - native code returns WasmValue objects directly
    final WasmValue[] results = new WasmValue[nativeResults.length];
    for (int i = 0; i < nativeResults.length; i++) {
      if (nativeResults[i] instanceof WasmValue) {
        results[i] = (WasmValue) nativeResults[i];
      } else if (nativeResults[i] == null) {
        // Null represents null externref or funcref - shouldn't happen with new approach
        results[i] = WasmValue.externref(null);
      } else {
        throw new WasmException(
            "Unexpected return type: " + nativeResults[i].getClass() + " at index " + i);
      }
    }

    return results;
  }

  /**
   * Disposes of this instance, releasing resources immediately.
   *
   * <p>This method provides explicit resource cleanup, allowing instances to be disposed of before
   * the store is closed. Once disposed, the instance becomes invalid and should not be used.
   *
   * @return true if disposal was successful, false if already disposed
   * @throws WasmException if disposal fails
   * @since 1.0.0
   */
  @Override
  public boolean dispose() throws WasmException {
    ensureNotClosed();

    try {
      final boolean result = nativeDispose(getNativeHandle());
      if (result) {
        // Mark as closed to prevent further use
        try {
          close();
        } catch (final Exception e) {
          LOGGER.warning("Failed to close instance after disposal: " + e.getMessage());
        }
      }
      return result;
    } catch (final Exception e) {
      throw new WasmException("Failed to dispose instance", e);
    }
  }

  /**
   * Checks if this instance has been disposed.
   *
   * <p>Disposed instances are no longer usable and will throw exceptions if operations are
   * attempted on them.
   *
   * @return true if the instance has been disposed, false otherwise
   * @since 1.0.0
   */
  @Override
  public boolean isDisposed() {
    try {
      return nativeIsDisposed(getNativeHandle());
    } catch (final Exception e) {
      LOGGER.warning("Failed to check dispose status: " + e.getMessage());
      return true; // Assume disposed if we can't check
    }
  }

  /**
   * Gets the creation timestamp of this instance in microseconds.
   *
   * <p>This timestamp represents when the instance was created, measured from the Unix epoch in
   * microseconds.
   *
   * @return the creation timestamp in microseconds since Unix epoch
   * @since 1.0.0
   */
  @Override
  public long getCreatedAtMicros() {
    ensureNotClosed();

    try {
      return nativeGetCreatedAtMicros(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get creation timestamp", e);
    }
  }

  /**
   * Gets the count of metadata exports in this instance.
   *
   * <p>Metadata exports include debugging information, custom sections, and other non-executable
   * exports that provide information about the module structure.
   *
   * @return the number of metadata exports
   * @since 1.0.0
   */
  @Override
  public int getMetadataExportCount() {
    ensureNotClosed();

    try {
      return (int) nativeGetMetadataExportCount(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get metadata export count", e);
    }
  }

  /**
   * Calls a 32-bit integer function with parameters.
   *
   * <p>This is an optimized calling convention for functions that take 32-bit integer parameters
   * and return a 32-bit integer result.
   *
   * @param functionName the name of the function to call
   * @param params array of 32-bit integer parameters
   * @return the 32-bit integer result
   * @throws WasmException if the function call fails or function doesn't exist
   * @throws IllegalArgumentException if functionName is null
   * @since 1.0.0
   */
  @Override
  public int callI32Function(final String functionName, final int... params) throws WasmException {
    JniValidation.requireNonEmpty(functionName, "functionName");
    JniValidation.requireNonNull(params, "params");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    try {
      final JniStore jniStore = (JniStore) store;
      return nativeCallI32Function(
          getNativeHandle(), jniStore.getNativeHandle(), functionName, params);
    } catch (final Exception e) {
      throw new WasmException("Failed to call I32 function " + functionName, e);
    }
  }

  /**
   * Calls a 32-bit integer function with no parameters.
   *
   * <p>This is an optimized calling convention for functions that take no parameters and return a
   * 32-bit integer result.
   *
   * @param functionName the name of the function to call
   * @return the 32-bit integer result
   * @throws WasmException if the function call fails or function doesn't exist
   * @throws IllegalArgumentException if functionName is null
   * @since 1.0.0
   */
  @Override
  public int callI32Function(final String functionName) throws WasmException {
    JniValidation.requireNonEmpty(functionName, "functionName");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    try {
      final JniStore jniStore = (JniStore) store;
      return nativeCallI32FunctionNoParams(
          getNativeHandle(), jniStore.getNativeHandle(), functionName);
    } catch (final Exception e) {
      throw new WasmException("Failed to call I32 function " + functionName, e);
    }
  }

  @Override
  public InstanceState getState() {
    ensureNotClosed();
    try {
      final int stateValue = nativeGetState(getNativeHandle());
      return convertIntToInstanceState(stateValue);
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to get instance state: " + e.getMessage());
      return InstanceState.ERROR;
    }
  }

  @Override
  public boolean cleanup() throws WasmException {
    if (cleanedUp.get()) {
      return false; // Already cleaned up
    }

    ensureNotClosed();

    try {
      final boolean result = nativeCleanupResources(getNativeHandle());
      if (result) {
        cleanedUp.set(true);
        LOGGER.fine("Instance resources cleaned up successfully");
      }
      return result;
    } catch (final RuntimeException e) {
      throw new WasmException("Failed to cleanup instance resources", e);
    }
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
    } catch (final RuntimeException e) {
      throw new WasmException("Failed to get instance statistics", e);
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
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to get all exports: " + e.getMessage());
      return java.util.Collections.emptyMap();
    }
  }

  @Override
  public void setImports(final java.util.Map<String, Object> imports) throws WasmException {
    throw new UnsupportedOperationException(
        "Setting imports is not supported for instantiated instances");
  }

  /**
   * Validates thread access for this instance.
   *
   * @return true if access is valid, false otherwise
   */
  public boolean validateThreadAccess() {
    try {
      return nativeValidateThreadAccess(getNativeHandle());
    } catch (final RuntimeException e) {
      LOGGER.warning("Thread access validation failed: " + e.getMessage());
      return false;
    }
  }

  /**
   * Converts integer state value to InstanceState enum.
   *
   * @param stateValue the integer state value from native code
   * @return the corresponding InstanceState
   */
  private static InstanceState convertIntToInstanceState(final int stateValue) {
    switch (stateValue) {
      case 0:
        return InstanceState.CREATING;
      case 1:
        return InstanceState.CREATED;
      case 2:
        return InstanceState.RUNNING;
      case 3:
        return InstanceState.SUSPENDED;
      case 4:
        return InstanceState.ERROR;
      case 5:
        return InstanceState.DISPOSED;
      case 6:
        return InstanceState.DESTROYING;
      default:
        LOGGER.warning("Unknown instance state value: " + stateValue);
        return InstanceState.ERROR;
    }
  }

  // Native method declarations

  /**
   * Gets a function export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param storeHandle the native store handle
   * @param name the function name
   * @return native function handle or 0 if not found
   */
  private static native long nativeGetFunction(long instanceHandle, long storeHandle, String name);

  /**
   * Gets a memory export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the memory name
   * @return native memory handle or 0 if not found
   */
  private static native long nativeGetMemory(long instanceHandle, long storeHandle, String name);

  /**
   * Gets a table export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the table name
   * @return native table handle or 0 if not found
   */
  private static native long nativeGetTable(long instanceHandle, long storeHandle, String name);

  /**
   * Gets a global export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the global name
   * @return native global handle or 0 if not found
   */
  private static native long nativeGetGlobal(long instanceHandle, long storeHandle, String name);

  /**
   * Checks if an instance has an export with the given name.
   *
   * @param instanceHandle the native instance handle
   * @param name the export name
   * @return true if the export exists
   */
  private static native boolean nativeHasExport(long instanceHandle, String name);

  /**
   * Gets all export names from an instance.
   *
   * @param instanceHandle the native instance handle
   * @return array of export names
   */
  private static native String[] nativeGetExportNames(long instanceHandle);

  /**
   * Destroys a native instance.
   *
   * @param instanceHandle the native instance handle
   */
  private static native void nativeDestroyInstance(long instanceHandle);

  /**
   * Disposes of a native instance.
   *
   * @param instanceHandle the native instance handle
   * @return true if disposal was successful, false otherwise
   */
  private static native boolean nativeDispose(long instanceHandle);

  /**
   * Checks if a native instance has been disposed.
   *
   * @param instanceHandle the native instance handle
   * @return true if the instance has been disposed, false otherwise
   */
  private static native boolean nativeIsDisposed(long instanceHandle);

  /**
   * Gets the creation timestamp of a native instance.
   *
   * @param instanceHandle the native instance handle
   * @return the creation timestamp in microseconds since Unix epoch
   */
  private static native long nativeGetCreatedAtMicros(long instanceHandle);

  /**
   * Gets the metadata export count for a native instance.
   *
   * @param instanceHandle the native instance handle
   * @return the number of metadata exports
   */
  private static native long nativeGetMetadataExportCount(long instanceHandle);

  /**
   * Calls a WebAssembly function directly without extracting a Function object.
   *
   * @param instanceHandle the native instance handle
   * @param storeHandle the native store handle
   * @param functionName the name of the function to call
   * @param params array of parameters (Integer, Long, Float, Double)
   * @return array of results (Integer, Long, Float, Double)
   */
  private static native Object[] nativeCallFunction(
      long instanceHandle, long storeHandle, String functionName, Object[] params);

  /**
   * Calls a 32-bit integer function with parameters.
   *
   * @param instanceHandle the native instance handle
   * @param storeHandle the native store handle
   * @param functionName the name of the function to call
   * @param params array of 32-bit integer parameters
   * @return the 32-bit integer result
   */
  private static native int nativeCallI32Function(
      long instanceHandle, long storeHandle, String functionName, int[] params);

  /**
   * Calls a 32-bit integer function with no parameters.
   *
   * @param instanceHandle the native instance handle
   * @param storeHandle the native store handle
   * @param functionName the name of the function to call
   * @return the 32-bit integer result
   */
  private static native int nativeCallI32FunctionNoParams(
      long instanceHandle, long storeHandle, String functionName);

  /**
   * Gets the state of a native instance.
   *
   * @param instanceHandle the native instance handle
   * @return the instance state as an integer
   */
  private static native int nativeGetState(long instanceHandle);

  /**
   * Performs cleanup of native instance resources.
   *
   * @param instanceHandle the native instance handle
   * @return true if cleanup was successful, false otherwise
   */
  private static native boolean nativeCleanupResources(long instanceHandle);

  /**
   * Validates thread access for a native instance.
   *
   * @param instanceHandle the native instance handle
   * @return true if access is valid, false otherwise
   */
  private static native boolean nativeValidateThreadAccess(long instanceHandle);
}
