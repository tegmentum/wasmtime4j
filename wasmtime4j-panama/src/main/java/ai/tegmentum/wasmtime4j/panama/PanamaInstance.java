package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.FunctionType;
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
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Instance.
 *
 * @since 1.0.0
 */
public final class PanamaInstance implements Instance {
  private static final Logger LOGGER = Logger.getLogger(PanamaInstance.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final PanamaModule module;
  private final PanamaStore store;
  private final Arena arena;
  private final MemorySegment nativeInstance;
  private final long createdAtMicros;
  private final AtomicBoolean disposed = new AtomicBoolean(false);
  private volatile boolean closed = false;

  /**
   * Creates a new Panama instance.
   *
   * @param module the module to instantiate
   * @param store the store to create the instance in
   * @throws WasmException if instance creation fails
   */
  public PanamaInstance(final PanamaModule module, final PanamaStore store) throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    if (!module.isValid()) {
      throw new IllegalStateException("Module is not valid");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (!store.isValid()) {
      throw new IllegalStateException("Store is not valid");
    }

    this.module = module;
    this.store = store;
    this.arena = Arena.ofConfined();
    this.createdAtMicros = System.currentTimeMillis() * 1000L;

    // Create native instance via Panama FFI
    this.nativeInstance =
        NATIVE_BINDINGS.instanceCreate(store.getNativeStore(), module.getNativeModule());

    if (this.nativeInstance == null || this.nativeInstance.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native instance");
    }

    LOGGER.fine("Created Panama instance");
  }

  @Override
  public Optional<WasmFunction> getFunction(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    // Check if the export exists and is a function
    final int exportKind =
        NATIVE_BINDINGS.moduleGetExportKind(
            module.getNativeModule(),
            arena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
    if (exportKind != 1) {
      return Optional.empty();
    }

    // TODO: Get the actual function type from the module
    // For now, use a placeholder type - the actual signature will be validated at call time
    final FunctionType functionType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
    return Optional.of(new PanamaFunction(this, name, functionType));
  }

  @Override
  public Optional<WasmFunction> getFunction(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Get all export names and find the index-th function export
    final String[] exportNames = getExportNames();
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
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment nameSegment =
        arena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
    final MemorySegment globalPtr =
        NATIVE_BINDINGS.instanceGetGlobalByName(
            nativeInstance, store.getNativeStore(), nameSegment);

    if (globalPtr == null || globalPtr.equals(MemorySegment.NULL)) {
      return Optional.empty();
    }

    return Optional.of(new PanamaGlobal(globalPtr));
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Iterate through exports and find the nth global
    final String[] exportNames = getExportNames();
    int globalCount = 0;
    for (final String name : exportNames) {
      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        if (globalCount == index) {
          return global;
        }
        globalCount++;
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<WasmMemory> getMemory(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment nameSegment =
        arena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
    final MemorySegment memoryPtr =
        NATIVE_BINDINGS.instanceGetMemoryByName(
            nativeInstance, store.getNativeStore(), nameSegment);

    if (memoryPtr == null || memoryPtr.equals(MemorySegment.NULL)) {
      return Optional.empty();
    }

    return Optional.of(new PanamaMemory(memoryPtr));
  }

  @Override
  public Optional<WasmMemory> getMemory(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Iterate through exports and find the nth memory
    final String[] exportNames = getExportNames();
    int memoryCount = 0;
    for (final String name : exportNames) {
      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        if (memoryCount == index) {
          return memory;
        }
        memoryCount++;
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<WasmTable> getTable(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment nameSegment =
        arena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
    final MemorySegment tablePtr =
        NATIVE_BINDINGS.instanceGetTableByName(nativeInstance, store.getNativeStore(), nameSegment);

    if (tablePtr == null || tablePtr.equals(MemorySegment.NULL)) {
      return Optional.empty();
    }

    return Optional.of(new PanamaTable(tablePtr));
  }

  @Override
  public Optional<WasmTable> getTable(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Iterate through exports and find the nth table
    final String[] exportNames = getExportNames();
    int tableCount = 0;
    for (final String name : exportNames) {
      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        if (tableCount == index) {
          return table;
        }
        tableCount++;
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<WasmMemory> getDefaultMemory() {
    ensureNotClosed();

    // First, try to get memory named "memory" (most common convention)
    final Optional<WasmMemory> namedMemory = getMemory("memory");
    if (namedMemory.isPresent()) {
      return namedMemory;
    }

    // If not found, try to get the first memory export
    final String[] exportNames = getExportNames();
    for (final String exportName : exportNames) {
      final Optional<WasmMemory> memory = getMemory(exportName);
      if (memory.isPresent()) {
        return memory;
      }
    }

    // No memory exports found
    return Optional.empty();
  }

  @Override
  public String[] getExportNames() {
    ensureNotClosed();

    // Get the export count
    final long exportCount = NATIVE_BINDINGS.moduleExportCount(module.getNativeModule());
    if (exportCount <= 0) {
      return new String[0];
    }

    try (final Arena exportArena = Arena.ofConfined()) {
      // Allocate array of pointers to hold C string pointers
      final MemorySegment namesArray = exportArena.allocate(ValueLayout.ADDRESS, (int) exportCount);

      // Call native function to populate the array
      final long actualCount =
          NATIVE_BINDINGS.moduleGetExportNames(module.getNativeModule(), namesArray, exportCount);

      if (actualCount <= 0) {
        return new String[0];
      }

      // Convert C strings to Java strings
      final String[] exportNames = new String[(int) actualCount];
      for (int i = 0; i < actualCount; i++) {
        final MemorySegment cStringPtr = namesArray.getAtIndex(ValueLayout.ADDRESS, i);
        if (cStringPtr != null && !cStringPtr.equals(MemorySegment.NULL)) {
          // Reinterpret as unbounded segment to read null-terminated string
          final MemorySegment unboundedPtr = cStringPtr.reinterpret(Long.MAX_VALUE);
          exportNames[i] = unboundedPtr.getString(0);
          // Free the C string allocated by Rust
          NATIVE_BINDINGS.freeString(cStringPtr);
        }
      }

      return exportNames;
    }
  }

  @Override
  public List<ExportDescriptor> getExportDescriptors() {
    ensureNotClosed();
    // TODO: Implement full export descriptors with proper type information
    // This requires additional native functions to get type details
    return Collections.emptyList();
  }

  @Override
  public Optional<ExportDescriptor> getExportDescriptor(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement export descriptor lookup with proper type information
    return Optional.empty();
  }

  @Override
  public Optional<FuncType> getFunctionType(final String functionName) {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement function type lookup
    return Optional.empty();
  }

  @Override
  public Optional<GlobalType> getGlobalType(final String globalName) {
    if (globalName == null) {
      throw new IllegalArgumentException("Global name cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement global type lookup
    return Optional.empty();
  }

  @Override
  public Optional<MemoryType> getMemoryType(final String memoryName) {
    if (memoryName == null) {
      throw new IllegalArgumentException("Memory name cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement memory type lookup
    return Optional.empty();
  }

  @Override
  public Optional<TableType> getTableType(final String tableName) {
    if (tableName == null) {
      throw new IllegalArgumentException("Table name cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement table type lookup
    return Optional.empty();
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena checkArena = Arena.ofConfined()) {
      final int exportKind =
          NATIVE_BINDINGS.moduleGetExportKind(
              module.getNativeModule(),
              checkArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));
      // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
      return exportKind > 0;
    }
  }

  @Override
  public Module getModule() {
    return module;
  }

  @Override
  public Store getStore() {
    return store;
  }

  @Override
  public WasmValue[] callFunction(final String functionName, final WasmValue... params)
      throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Maximum possible results (conservative estimate)
    final int maxResults = 16;

    try (final Arena callArena = Arena.ofConfined()) {
      // Allocate native memory for function name (C string)
      final MemorySegment functionNameSegment =
          callArena.allocateFrom(functionName, java.nio.charset.StandardCharsets.UTF_8);

      // Allocate and marshal parameters
      final MemorySegment paramsSegment;
      if (params != null && params.length > 0) {
        paramsSegment = callArena.allocate(params.length * 20L); // 20 bytes per WasmValue
        for (int i = 0; i < params.length; i++) {
          marshalWasmValue(params[i], paramsSegment, i);
        }
      } else {
        paramsSegment = MemorySegment.NULL;
      }

      // Allocate results buffer (zeroed)
      final MemorySegment resultsSegment = callArena.allocate(maxResults * 20L);
      // Zero out the results buffer
      for (long i = 0; i < maxResults * 20L; i++) {
        resultsSegment.set(ValueLayout.JAVA_BYTE, i, (byte) 0);
      }

      // Call native function
      final long resultCount =
          NATIVE_BINDINGS.instanceCallFunction(
              nativeInstance,
              store.getNativeStore(),
              functionNameSegment,
              paramsSegment,
              params != null ? params.length : 0,
              resultsSegment,
              maxResults);

      if (resultCount < 0) {
        throw new WasmException("Failed to call function: " + functionName);
      }

      // Unmarshal results
      final WasmValue[] results = new WasmValue[(int) resultCount];
      for (int i = 0; i < resultCount; i++) {
        results[i] = unmarshalWasmValue(resultsSegment, i);
      }

      return results;
    }
  }

  @Override
  public Map<String, Object> getAllExports() {
    ensureNotClosed();

    final Map<String, Object> exports = new java.util.HashMap<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      // Try each export type
      final Optional<WasmFunction> function = getFunction(name);
      if (function.isPresent()) {
        exports.put(name, function.get());
        continue;
      }

      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        exports.put(name, memory.get());
        continue;
      }

      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        exports.put(name, table.get());
        continue;
      }

      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        exports.put(name, global.get());
      }
    }

    return Collections.unmodifiableMap(exports);
  }

  /**
   * Gets all exported function names.
   *
   * @return list of exported function names
   */
  public List<String> getFunctionNames() {
    ensureNotClosed();
    final List<String> functionNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmFunction> function = getFunction(name);
      if (function.isPresent()) {
        functionNames.add(name);
      }
    }

    return Collections.unmodifiableList(functionNames);
  }

  /**
   * Gets all exported memory names.
   *
   * @return list of exported memory names
   */
  public List<String> getMemoryNames() {
    ensureNotClosed();
    final List<String> memoryNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        memoryNames.add(name);
      }
    }

    return Collections.unmodifiableList(memoryNames);
  }

  /**
   * Gets all exported table names.
   *
   * @return list of exported table names
   */
  public List<String> getTableNames() {
    ensureNotClosed();
    final List<String> tableNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        tableNames.add(name);
      }
    }

    return Collections.unmodifiableList(tableNames);
  }

  /**
   * Gets all exported global names.
   *
   * @return list of exported global names
   */
  public List<String> getGlobalNames() {
    ensureNotClosed();
    final List<String> globalNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        globalNames.add(name);
      }
    }

    return Collections.unmodifiableList(globalNames);
  }

  /**
   * Gets the count of exported functions.
   *
   * @return number of function exports
   */
  public int getFunctionCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmFunction> function = getFunction(name);
      if (function.isPresent()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Gets the count of exported memories.
   *
   * @return number of memory exports
   */
  public int getMemoryCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Gets the count of exported tables.
   *
   * @return number of table exports
   */
  public int getTableCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Gets the count of exported globals.
   *
   * @return number of global exports
   */
  public int getGlobalCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        count++;
      }
    }

    return count;
  }

  @Override
  public void setImports(final Map<String, Object> imports) throws WasmException {
    if (imports == null) {
      throw new IllegalArgumentException("Imports cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement import setting
    throw new UnsupportedOperationException("Import setting not yet implemented");
  }

  @Override
  public InstanceStatistics getStatistics() throws WasmException {
    ensureNotClosed();
    // TODO: Implement statistics collection
    throw new UnsupportedOperationException("Statistics not yet implemented");
  }

  @Override
  public InstanceState getState() {
    if (closed || disposed.get()) {
      return InstanceState.DISPOSED;
    }
    return InstanceState.CREATED;
  }

  @Override
  public boolean cleanup() throws WasmException {
    if (disposed.get()) {
      return false;
    }
    // TODO: Implement resource cleanup
    disposed.set(true);
    return true;
  }

  @Override
  public boolean isValid() {
    return !closed && !disposed.get();
  }

  @Override
  public boolean dispose() throws WasmException {
    if (disposed.getAndSet(true)) {
      return false;
    }
    // TODO: Implement disposal
    return true;
  }

  @Override
  public boolean isDisposed() {
    return disposed.get();
  }

  @Override
  public long getCreatedAtMicros() {
    return createdAtMicros;
  }

  @Override
  public int getMetadataExportCount() {
    ensureNotClosed();
    final long count = NATIVE_BINDINGS.moduleExportCount(module.getNativeModule());
    return (int) count;
  }

  @Override
  public int callI32Function(final String functionName, final int... params) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Convert int parameters to WasmValue array
    final WasmValue[] wasmParams;
    if (params != null && params.length > 0) {
      wasmParams = new WasmValue[params.length];
      for (int i = 0; i < params.length; i++) {
        wasmParams[i] = WasmValue.i32(params[i]);
      }
    } else {
      wasmParams = new WasmValue[0];
    }

    // Call the function
    final WasmValue[] results = callFunction(functionName, wasmParams);

    // Validate and extract result
    if (results == null || results.length == 0) {
      throw new WasmException("Function " + functionName + " did not return a value");
    }
    if (results.length > 1) {
      throw new WasmException(
          "Function " + functionName + " returned multiple values, expected single i32");
    }
    if (results[0].getType() != WasmValueType.I32) {
      throw new WasmException(
          "Function " + functionName + " returned " + results[0].getType() + ", expected I32");
    }

    return results[0].asI32();
  }

  @Override
  public int callI32Function(final String functionName) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Call the function with no parameters
    final WasmValue[] results = callFunction(functionName);

    // Validate and extract result
    if (results == null || results.length == 0) {
      throw new WasmException("Function " + functionName + " did not return a value");
    }
    if (results.length > 1) {
      throw new WasmException(
          "Function " + functionName + " returned multiple values, expected single i32");
    }
    if (results[0].getType() != WasmValueType.I32) {
      throw new WasmException(
          "Function " + functionName + " returned " + results[0].getType() + ", expected I32");
    }

    return results[0].asI32();
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      disposed.set(true);
      // Destroy native instance
      if (nativeInstance != null && !nativeInstance.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.instanceDestroy(nativeInstance);
      }
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama instance");
    } catch (final Exception e) {
      LOGGER.warning("Error closing instance: " + e.getMessage());
    }
  }

  /**
   * Gets the native instance pointer.
   *
   * @return native instance memory segment
   */
  public MemorySegment getNativeInstance() {
    return nativeInstance;
  }

  /**
   * Marshals a WasmValue to native memory.
   *
   * @param value the WasmValue to marshal
   * @param ptr pointer to the array
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

  /**
   * Unmarshals a WasmValue from native memory.
   *
   * @param ptr pointer to the array
   * @param index index in the array
   * @return the unmarshaled WasmValue
   */
  private static WasmValue unmarshalWasmValue(final MemorySegment ptr, final int index) {
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
   * Ensures the instance is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Instance has been closed");
    }
    if (disposed.get()) {
      throw new IllegalStateException("Instance has been disposed");
    }
  }
}
