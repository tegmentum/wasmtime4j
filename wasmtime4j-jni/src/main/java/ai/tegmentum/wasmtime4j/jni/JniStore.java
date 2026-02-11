package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * JNI implementation of the WebAssembly Store.
 *
 * <p>This class represents a WebAssembly store, which serves as an execution context for
 * WebAssembly instances. A store manages the runtime state of WebAssembly instances including
 * memory, globals, tables, and functions. All WebAssembly instances must be created within a store
 * context, and instances from different stores cannot interact directly.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource management with {@link AutoCloseable}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Thread-safe operations
 *   <li>Execution context management for WebAssembly instances
 *   <li>Resource isolation between different stores
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (JniEngine engine = JniEngine.create();
 *      JniStore store = engine.createStore()) {
 *
 *   // Compile module
 *   JniModule module = engine.compileModule(wasmBytes);
 *
 *   // Create instance within this store
 *   try (JniInstance instance = module.instantiate(store)) {
 *     // All operations on the instance are within this store context
 *     JniFunction exportedFunction = instance.getFunction("my_function");
 *     Object[] results = exportedFunction.call(args);
 *   }
 * }
 * }</pre>
 *
 * <p>Store Lifecycle:
 *
 * <ul>
 *   <li>Stores are created by engines using {@link JniEngine#createStore()}
 *   <li>Instances created within a store are tied to that store's lifetime
 *   <li>Closing a store invalidates all instances created within it
 *   <li>Stores cannot be shared between threads safely
 * </ul>
 *
 * <p>This implementation extends {@link JniResource} to provide automatic native resource
 * management and follows defensive programming practices to prevent native crashes.
 *
 * @since 1.0.0
 */
public final class JniStore extends JniResource implements Store {

  private static final Logger LOGGER = Logger.getLogger(JniStore.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniStore: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Custom data associated with this store. */
  private volatile Object customData;

  /** The engine that created this store. */
  private final Engine engine;

  /** Callback registry for managing callbacks and asynchronous operations. */
  private final CallbackRegistry callbackRegistry;

  /**
   * Creates a new JNI store with the given native handle.
   *
   * <p>This constructor is package-private and should only be used by the JniEngine or other JNI
   * classes. External code should create stores through {@link JniEngine#createStore()}.
   *
   * @param nativeHandle the native store handle from Wasmtime
   * @param engine the engine that created this store
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniStore(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    this.engine = engine;
    this.callbackRegistry = new JniCallbackRegistry(this);
    LOGGER.fine("Created JNI store with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Creates a new store that is compatible with the given module.
   *
   * <p><b>CRITICAL:</b> This method creates a Store that shares the exact same Engine Arc as the
   * module. This is required because Wasmtime's Instance::new() uses Arc::ptr_eq() to verify that
   * the Store and Module were created from the same Engine. Using {@link JniEngine#createStore()}
   * will create a Store with a different Arc clone, causing "cross-Engine instantiation is not
   * currently supported" errors.
   *
   * <p>Usage Example:
   *
   * <pre>{@code
   * try (JniEngine engine = JniEngine.create()) {
   *   JniModule module = (JniModule) engine.compileModule(wasmBytes);
   *
   *   // CORRECT: Use forModule to create a compatible store
   *   try (JniStore store = JniStore.forModule(module)) {
   *     JniInstance instance = (JniInstance) module.instantiate(store);
   *     // Instance created successfully
   *   }
   * }
   * }</pre>
   *
   * @param module the module to create a compatible store for
   * @return a new store that shares the same Engine Arc as the module
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if module is null or not a JniModule
   */
  public static JniStore forModule(final Module module) throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("module cannot be null");
    }
    if (!(module instanceof JniModule)) {
      throw new IllegalArgumentException("module must be a JniModule instance");
    }

    final JniModule jniModule = (JniModule) module;
    if (!jniModule.isValid()) {
      throw new IllegalStateException("Module is not valid");
    }

    final long storeHandle = nativeCreateStoreForModule(jniModule.getNativeHandle());
    if (storeHandle == 0) {
      throw new WasmException("Failed to create store for module");
    }

    return new JniStore(storeHandle, jniModule.getEngine());
  }

  /**
   * Gets runtime information about this store.
   *
   * <p>This method provides diagnostic information about the store's current state, including
   * memory usage, number of active instances, and other runtime metrics that can be useful for
   * debugging and monitoring.
   *
   * @return a string containing store runtime information
   * @throws JniException if information cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public String getRuntimeInfo() {
    ensureNotClosed();

    try {
      final StringBuilder info = new StringBuilder();
      info.append("Store Runtime Information:\n");
      info.append("  Handle: 0x").append(Long.toHexString(getNativeHandle())).append("\n");
      info.append("  Valid: ").append(nativeValidate(getNativeHandle())).append("\n");
      info.append("  Execution Count: ")
          .append(nativeGetExecutionCount(getNativeHandle()))
          .append("\n");
      info.append("  Execution Time (μs): ")
          .append(nativeGetExecutionTime(getNativeHandle()))
          .append("\n");
      info.append("  Fuel Remaining: ")
          .append(nativeGetFuelRemaining(getNativeHandle()))
          .append("\n");
      info.append("  Fuel Consumed: ")
          .append(nativeGetTotalFuelConsumed(getNativeHandle()))
          .append("\n");
      info.append("  Total Memory (bytes): ")
          .append(nativeGetTotalMemoryBytes(getNativeHandle()))
          .append("\n");
      info.append("  Used Memory (bytes): ")
          .append(nativeGetUsedMemoryBytes(getNativeHandle()))
          .append("\n");
      info.append("  Active Instances: ")
          .append(nativeGetInstanceCount(getNativeHandle()))
          .append("\n");
      info.append("  Fuel Limit: ").append(nativeGetFuelLimit(getNativeHandle())).append("\n");
      info.append("  Memory Limit (bytes): ")
          .append(nativeGetMemoryLimit(getNativeHandle()))
          .append("\n");
      info.append("  Execution Timeout (secs): ")
          .append(nativeGetExecutionTimeout(getNativeHandle()));

      return info.toString();
    } catch (final Exception e) {
      throw new JniException("Failed to get store runtime information", e);
    }
  }

  /**
   * Sets the fuel limit for this store.
   *
   * <p>Fuel is a mechanism for limiting WebAssembly execution time. When fuel is enabled,
   * WebAssembly execution will be interrupted when the fuel limit is reached. This can be used to
   * prevent runaway computations.
   *
   * @param fuel the fuel limit (must be positive)
   * @throws JniException if the fuel limit cannot be set
   * @throws JniResourceException if this store has been closed
   */
  public void setFuelLimit(final long fuel) {
    JniValidation.requirePositive(fuel, "fuel");
    ensureNotClosed();

    try {
      // Use addFuel to set the fuel amount - this replaces the current fuel
      final boolean success = nativeAddFuel(getNativeHandle(), fuel);
      if (!success) {
        throw new JniException("Failed to set fuel to " + fuel);
      }
      LOGGER.fine("Set fuel to " + fuel + " for store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Unexpected error setting fuel", e);
    }
  }

  /**
   * Gets the remaining fuel for this store.
   *
   * <p>Returns the amount of fuel remaining for WebAssembly execution in this store. If fuel is not
   * enabled, this method returns -1.
   *
   * @return the remaining fuel, or -1 if fuel is not enabled
   * @throws JniException if the fuel amount cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getFuelRemaining() {
    ensureNotClosed();

    try {
      return nativeGetFuelRemaining(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get remaining fuel", e);
    }
  }

  /**
   * Adds fuel to this store.
   *
   * <p>This method adds additional fuel to the store's fuel limit. This can be used to extend
   * execution time for long-running WebAssembly computations.
   *
   * @param additionalFuel the amount of fuel to add (must be positive)
   * @throws WasmException if fuel cannot be added
   * @throws JniResourceException if this store has been closed
   */
  @Override
  public void addFuel(final long additionalFuel) throws WasmException {
    JniValidation.requirePositive(additionalFuel, "additionalFuel");
    ensureNotClosed();

    try {
      final boolean success = nativeAddFuel(getNativeHandle(), additionalFuel);
      if (!success) {
        throw new WasmException("Failed to add fuel: " + additionalFuel);
      }
      LOGGER.fine(
          "Added " + additionalFuel + " fuel to store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error adding fuel", e);
    }
  }

  // Interface implementation methods

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public Object getData() {
    return customData;
  }

  @Override
  public void setData(final Object data) {
    this.customData = data;
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    JniValidation.requirePositive(fuel, "fuel");
    ensureNotClosed();

    try {
      final boolean success = nativeSetFuel(getNativeHandle(), fuel);
      if (!success) {
        throw new WasmException("Failed to set fuel to " + fuel);
      }
      LOGGER.fine("Set fuel to " + fuel + " for store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting fuel", e);
    }
  }

  @Override
  public long getFuel() throws WasmException {
    ensureNotClosed();

    try {
      return nativeGetFuelRemaining(getNativeHandle());
    } catch (final Exception e) {
      throw new WasmException("Failed to get remaining fuel", e);
    }
  }

  @Override
  public void setEpochDeadline(final long ticks) throws WasmException {
    ensureNotClosed();
    try {
      final boolean success = nativeSetEpochDeadline(getNativeHandle(), ticks);
      if (!success) {
        throw new WasmException("Failed to set epoch deadline to " + ticks);
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting epoch deadline", e);
    }
  }

  @Override
  public long consumeFuel(final long fuel) throws WasmException {
    JniValidation.requirePositive(fuel, "fuel");
    ensureNotClosed();

    try {
      final long consumed = nativeConsumeFuel(getNativeHandle(), fuel);
      if (consumed < 0) {
        throw new WasmException("Failed to consume fuel: " + fuel);
      }
      return consumed;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error consuming fuel", e);
    }
  }

  @Override
  public long getRemainingFuel() throws WasmException {
    ensureNotClosed();

    try {
      return nativeGetFuelRemaining(getNativeHandle());
    } catch (final Exception e) {
      throw new WasmException("Failed to get remaining fuel", e);
    }
  }

  /**
   * Increments the epoch counter for this store.
   *
   * @throws WasmException if the epoch increment fails
   */
  public void incrementEpoch() throws WasmException {
    ensureNotClosed();

    try {
      nativeIncrementEpoch(getNativeHandle());
    } catch (final Exception e) {
      throw new WasmException("Failed to increment epoch", e);
    }
  }

  /**
   * Sets the memory limit for this store.
   *
   * @param bytes the memory limit in bytes
   * @throws WasmException if setting the memory limit fails
   */
  public void setMemoryLimit(final long bytes) throws WasmException {
    if (bytes < 0) {
      throw new IllegalArgumentException("Memory limit cannot be negative");
    }
    ensureNotClosed();

    try {
      final boolean success = nativeSetMemoryLimit(getNativeHandle(), bytes);
      if (!success) {
        throw new WasmException("Failed to set memory limit to " + bytes);
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting memory limit", e);
    }
  }

  /**
   * Sets the table element limit for this store.
   *
   * @param elements the maximum number of table elements
   * @throws WasmException if setting the table element limit fails
   */
  public void setTableElementLimit(final long elements) throws WasmException {
    if (elements < 0) {
      throw new IllegalArgumentException("Table element limit cannot be negative");
    }
    ensureNotClosed();

    try {
      final boolean success = nativeSetTableElementLimit(getNativeHandle(), elements);
      if (!success) {
        throw new WasmException("Failed to set table element limit to " + elements);
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting table element limit", e);
    }
  }

  /**
   * Sets the instance limit for this store.
   *
   * @param count the maximum number of instances
   * @throws WasmException if setting the instance limit fails
   */
  public void setInstanceLimit(final int count) throws WasmException {
    if (count < 0) {
      throw new IllegalArgumentException("Instance limit cannot be negative");
    }
    ensureNotClosed();

    try {
      final boolean success = nativeSetInstanceLimit(getNativeHandle(), count);
      if (!success) {
        throw new WasmException("Failed to set instance limit to " + count);
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting instance limit", e);
    }
  }

  @Override
  public WasmFunction createHostFunction(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(functionType, "functionType cannot be null");
    Objects.requireNonNull(implementation, "implementation cannot be null");
    ensureNotClosed();

    try {
      // Create the JNI host function wrapper
      final JniHostFunction hostFunction =
          new JniHostFunction(name, functionType, implementation, this);

      LOGGER.fine(
          "Created host function '" + name + "' in store 0x" + Long.toHexString(getNativeHandle()));
      return hostFunction;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create host function: " + name, e);
    }
  }

  @Override
  public WasmGlobal createGlobal(
      final WasmValueType valueType, final boolean isMutable, final WasmValue initialValue)
      throws WasmException {
    JniValidation.requireNonNull(valueType, "valueType");
    JniValidation.requireNonNull(initialValue, "initialValue");
    ensureNotClosed();

    // Validate that the initial value matches the specified type
    if (initialValue.getType() != valueType) {
      throw new IllegalArgumentException(
          "Initial value type "
              + initialValue.getType()
              + " does not match global type "
              + valueType);
    }

    try {
      // Call native method to create global
      final long globalHandle =
          nativeCreateGlobal(
              getNativeHandle(),
              valueType.toNativeTypeCode(),
              isMutable ? 1 : 0,
              extractValueComponents(initialValue));

      if (globalHandle == 0) {
        throw new JniException("Native global creation returned null handle");
      }

      // Create JniGlobal wrapper
      final JniGlobal global = new JniGlobal(globalHandle, this);
      LOGGER.fine(
          "Created global with type "
              + valueType
              + ", mutable="
              + isMutable
              + ", handle=0x"
              + Long.toHexString(globalHandle));
      return global;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create global variable", e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final WasmValueType elementType, final int initialSize, final int maxSize)
      throws WasmException {
    JniValidation.requireNonNull(elementType, "elementType");
    if (initialSize < 0) {
      throw new IllegalArgumentException("Initial size cannot be negative: " + initialSize);
    }
    if (maxSize < -1) {
      throw new IllegalArgumentException("Max size must be -1 (unlimited) or >= 0: " + maxSize);
    }
    if (maxSize != -1 && maxSize < initialSize) {
      throw new IllegalArgumentException(
          "Max size (" + maxSize + ") cannot be less than initial size (" + initialSize + ")");
    }
    if (elementType != WasmValueType.FUNCREF && elementType != WasmValueType.EXTERNREF) {
      throw new IllegalArgumentException(
          "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
    }
    ensureNotClosed();

    try {
      final long tableHandle =
          nativeCreateTable(
              getNativeHandle(), elementType.toNativeTypeCode(), initialSize, maxSize);

      if (tableHandle == 0) {
        throw new JniException("Native table creation returned null handle");
      }

      final JniTable table = new JniTable(tableHandle, this);
      LOGGER.fine(
          "Created table with element type "
              + elementType
              + ", size="
              + initialSize
              + ", max="
              + maxSize
              + ", handle=0x"
              + Long.toHexString(tableHandle));
      return table;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create table", e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmMemory createMemory(final int initialPages, final int maxPages)
      throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative: " + initialPages);
    }
    if (maxPages < -1) {
      throw new IllegalArgumentException("Max pages must be -1 (unlimited) or >= 0: " + maxPages);
    }
    if (maxPages != -1 && maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
    }
    ensureNotClosed();

    try {
      final long memoryHandle = nativeCreateMemory(getNativeHandle(), initialPages, maxPages);

      if (memoryHandle == 0) {
        throw new JniException("Native memory creation returned null handle");
      }

      final JniMemory memory = new JniMemory(memoryHandle, this);
      LOGGER.fine(
          "Created memory with initial="
              + initialPages
              + " pages, max="
              + maxPages
              + " pages, handle=0x"
              + Long.toHexString(memoryHandle));
      return memory;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create memory", e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmMemory createSharedMemory(
      final int initialPages, final int maxPages) throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative: " + initialPages);
    }
    if (maxPages < 1) {
      throw new IllegalArgumentException("Shared memory requires a positive maximum page count");
    }
    if (maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
    }
    ensureNotClosed();

    try {
      final long memoryHandle = nativeCreateSharedMemory(getNativeHandle(), initialPages, maxPages);

      if (memoryHandle == 0) {
        throw new JniException("Native shared memory creation returned null handle");
      }

      final JniMemory memory = new JniMemory(memoryHandle, this);
      LOGGER.fine(
          "Created shared memory with initial="
              + initialPages
              + " pages, max="
              + maxPages
              + " pages, handle=0x"
              + Long.toHexString(memoryHandle));
      return memory;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create shared memory", e);
    }
  }

  @Override
  public FunctionReference createFunctionReference(
      final HostFunction implementation, final FunctionType functionType) throws WasmException {
    Objects.requireNonNull(implementation, "Host function implementation cannot be null");
    Objects.requireNonNull(functionType, "Function type cannot be null");
    ensureNotClosed();

    try {
      return new JniFunctionReference(implementation, functionType, this);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create function reference from host function", e);
    }
  }

  @Override
  public FunctionReference createFunctionReference(final WasmFunction function)
      throws WasmException {
    Objects.requireNonNull(function, "WebAssembly function cannot be null");
    ensureNotClosed();

    try {
      return new JniFunctionReference(function, this);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create function reference from WebAssembly function", e);
    }
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return callbackRegistry;
  }

  @Override
  public Instance createInstance(final Module module) throws WasmException {
    Objects.requireNonNull(module, "Module cannot be null");
    ensureNotClosed();

    try {
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule instance for JNI store");
      }

      JniModule jniModule = (JniModule) module;
      return jniModule.instantiate(this);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create instance from module", e);
    }
  }

  @Override
  public boolean isValid() {
    if (isClosed() || getNativeHandle() == 0) {
      return false;
    }

    try {
      return nativeValidate(getNativeHandle());
    } catch (final Exception e) {
      LOGGER.warning("Error validating store: " + e.getMessage());
      return false;
    }
  }

  @Override
  protected void doClose() throws Exception {
    try {
      // Close the callback registry first
      callbackRegistry.close();
    } catch (Exception e) {
      LOGGER.warning("Error closing callback registry: " + e.getMessage());
    }

    if (nativeHandle != 0) {
      nativeDestroyStore(nativeHandle);
      LOGGER.fine("Destroyed JNI store with handle: 0x" + Long.toHexString(nativeHandle));
    }
  }

  @Override
  protected String getResourceType() {
    return "Store";
  }

  // Statistics and Metrics Methods

  /**
   * Gets the execution count for this store.
   *
   * <p>This method returns the total number of WebAssembly function executions that have occurred
   * within this store context. This includes both exported function calls and internal function
   * calls within WebAssembly modules.
   *
   * @return the number of executions performed in this store
   * @throws JniException if the execution count cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  @Override
  public long getExecutionCount() {
    ensureNotClosed();

    try {
      return nativeGetExecutionCount(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get execution count", e);
    }
  }

  /**
   * Gets the total execution time for this store.
   *
   * <p>This method returns the cumulative time spent executing WebAssembly code within this store
   * context, measured in microseconds for high precision timing.
   *
   * @return the total execution time in microseconds
   * @throws JniException if the execution time cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getExecutionTime() {
    ensureNotClosed();

    try {
      return nativeGetExecutionTime(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get execution time", e);
    }
  }

  /**
   * Gets the total execution time in microseconds for this store.
   *
   * <p>This includes time spent in both WebAssembly execution and host function calls made through
   * this store.
   *
   * @return the total execution time in microseconds
   * @since 1.0.0
   */
  @Override
  public long getTotalExecutionTimeMicros() {
    ensureNotClosed();

    try {
      return nativeGetExecutionTime(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get total execution time", e);
    }
  }

  /**
   * Gets the total fuel consumed by this store.
   *
   * <p>This method returns the cumulative amount of fuel consumed by all WebAssembly executions
   * within this store. Fuel consumption tracking must be enabled in the engine configuration for
   * this metric to be meaningful.
   *
   * @return the total fuel consumed
   * @throws JniException if the fuel consumption cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  @Override
  public long getTotalFuelConsumed() {
    ensureNotClosed();

    try {
      return nativeGetTotalFuelConsumed(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get total fuel consumed", e);
    }
  }

  /**
   * Gets the total memory allocated by this store.
   *
   * <p>This method returns the total amount of memory (in bytes) that has been allocated for
   * WebAssembly linear memories, tables, and other runtime structures within this store context.
   *
   * @return the total memory in bytes
   * @throws JniException if the memory information cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getTotalMemoryBytes() {
    ensureNotClosed();

    try {
      return nativeGetTotalMemoryBytes(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get total memory bytes", e);
    }
  }

  /**
   * Gets the currently used memory by this store.
   *
   * <p>This method returns the amount of memory (in bytes) currently in use by active WebAssembly
   * instances, linear memories, and other runtime structures within this store. This represents the
   * subset of total allocated memory that is actively being used.
   *
   * @return the used memory in bytes
   * @throws JniException if the memory information cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getUsedMemoryBytes() {
    ensureNotClosed();

    try {
      return nativeGetUsedMemoryBytes(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get used memory bytes", e);
    }
  }

  /**
   * Gets the number of active instances in this store.
   *
   * <p>This method returns the current number of active WebAssembly module instances that are
   * associated with this store. Each instance represents a separate instantiation of a compiled
   * module with its own state and memory.
   *
   * @return the number of active instances
   * @throws JniException if the instance count cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getInstanceCount() {
    ensureNotClosed();

    try {
      return nativeGetInstanceCount(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get instance count", e);
    }
  }

  // Configuration Getters

  /**
   * Gets the fuel limit for this store.
   *
   * <p>This method returns the maximum amount of fuel that can be consumed by WebAssembly execution
   * within this store. If fuel tracking is disabled or no limit is set, this method returns -1.
   *
   * @return the fuel limit, or -1 if no limit is set or fuel tracking is disabled
   * @throws JniException if the fuel limit cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getFuelLimit() {
    ensureNotClosed();

    try {
      return nativeGetFuelLimit(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get fuel limit", e);
    }
  }

  /**
   * Gets the memory limit for this store.
   *
   * <p>This method returns the maximum amount of memory (in bytes) that can be allocated for
   * WebAssembly linear memories and runtime structures within this store. If no memory limit is
   * set, this method returns -1.
   *
   * @return the memory limit in bytes, or -1 if no limit is set
   * @throws JniException if the memory limit cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getMemoryLimit() {
    ensureNotClosed();

    try {
      return nativeGetMemoryLimit(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get memory limit", e);
    }
  }

  /**
   * Gets the execution timeout for this store.
   *
   * <p>This method returns the maximum execution time (in seconds) allowed for WebAssembly
   * operations within this store. If no timeout is set, this method returns -1.
   *
   * @return the execution timeout in seconds, or -1 if no timeout is set
   * @throws JniException if the execution timeout cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getExecutionTimeout() {
    ensureNotClosed();

    try {
      return nativeGetExecutionTimeout(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get execution timeout", e);
    }
  }

  // Validation and Diagnostics

  /**
   * Validates the store and checks its current state.
   *
   * <p>This method performs comprehensive validation of the store's internal state and verifies
   * that all associated resources are in a consistent, valid state. This is useful for debugging
   * and ensuring store integrity.
   *
   * @return true if the store is valid and all checks pass, false otherwise
   * @throws JniResourceException if this store has been closed
   */
  public boolean validate() {
    if (isClosed() || getNativeHandle() == 0) {
      return false;
    }

    try {
      return nativeValidate(getNativeHandle());
    } catch (final Exception e) {
      LOGGER.warning("Error validating store: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets comprehensive diagnostic information about this store.
   *
   * <p>This method returns a formatted string containing detailed diagnostic information about the
   * store's current state, configuration, and runtime metrics. This information is useful for
   * debugging, monitoring, and performance analysis.
   *
   * @return formatted diagnostic information
   * @throws JniException if diagnostic information cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public String getDiagnosticInfo() {
    return getRuntimeInfo();
  }

  /**
   * Checks if the store is in a healthy state.
   *
   * <p>This method performs a health check that combines validation with runtime state analysis. A
   * store is considered healthy if it passes validation and all key metrics are within expected
   * ranges.
   *
   * @return true if the store is healthy, false otherwise
   */
  public boolean isHealthy() {
    if (!isValid()) {
      return false;
    }

    try {
      // Check if any critical resources are exhausted
      final long usedMemory = getUsedMemoryBytes();
      final long totalMemory = getTotalMemoryBytes();

      // Memory usage should not exceed total memory (this would indicate corruption)
      if (usedMemory > totalMemory && totalMemory > 0) {
        LOGGER.warning("Store health check failed: used memory exceeds total memory");
        return false;
      }

      // Check if fuel system is consistent
      final long fuelLimit = getFuelLimit();
      final long fuelRemaining = getFuel();

      // If fuel is enabled, remaining fuel should not exceed limit
      if (fuelLimit >= 0 && fuelRemaining > fuelLimit) {
        LOGGER.warning("Store health check failed: remaining fuel exceeds limit");
        return false;
      }

      return true;
    } catch (final Exception e) {
      LOGGER.warning("Store health check failed due to exception: " + e.getMessage());
      return false;
    }
  }

  // Native method declarations

  /**
   * Creates a new store with default configuration.
   *
   * @param engineHandle the native engine handle
   * @return the native store handle, or 0 on failure
   */
  private static native long nativeCreateStore(long engineHandle);

  /**
   * Creates a new store with custom configuration.
   *
   * @param engineHandle the native engine handle
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @return the native store handle, or 0 on failure
   */
  private static native long nativeCreateStoreWithConfig(
      long engineHandle,
      long fuelLimit,
      long memoryLimitBytes,
      long executionTimeoutSecs,
      int maxInstances,
      int maxTableElements,
      int maxFunctions);

  /**
   * Creates a new store that is compatible with a specific module.
   *
   * <p>CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Engine Arc as the
   * Module's internal wasmtime::Module. This is required because wasmtime's Instance::new() uses
   * Arc::ptr_eq() to verify engine compatibility.
   *
   * @param moduleHandle the native module handle
   * @return the native store handle, or 0 on failure
   */
  private static native long nativeCreateStoreForModule(long moduleHandle);

  /**
   * Adds fuel to a store.
   *
   * @param storeHandle the native store handle
   * @param additionalFuel the amount of fuel to add
   * @return true on success, false on failure
   */
  private static native boolean nativeAddFuel(long storeHandle, long additionalFuel);

  /**
   * Sets fuel to a specific amount (replaces current fuel).
   *
   * @param storeHandle the native store handle
   * @param fuel the fuel amount to set
   * @return true on success, false on failure
   */
  private static native boolean nativeSetFuel(long storeHandle, long fuel);

  /**
   * Gets the remaining fuel for a store.
   *
   * @param storeHandle the native store handle
   * @return remaining fuel or -1 if fuel is not enabled/available
   */
  private static native long nativeGetFuelRemaining(long storeHandle);

  /**
   * Sets the epoch deadline for a store.
   *
   * @param storeHandle the native store handle
   * @param ticks the number of epoch ticks before interruption
   * @return true on success, false on failure
   */
  private static native boolean nativeSetEpochDeadline(long storeHandle, long ticks);

  /**
   * Triggers garbage collection within a store.
   *
   * @param storeHandle the native store handle
   * @return true on success, false on failure
   */
  private static native boolean nativeGarbageCollect(long storeHandle);

  /**
   * Validates the store and checks its current state.
   *
   * @param storeHandle the native store handle
   * @return true if the store is valid, false otherwise
   */
  private static native boolean nativeValidate(long storeHandle);

  /**
   * Gets the execution count for this store.
   *
   * @param storeHandle the native store handle
   * @return the number of executions performed in this store
   */
  private static native long nativeGetExecutionCount(long storeHandle);

  /**
   * Gets the total execution time for this store.
   *
   * @param storeHandle the native store handle
   * @return the total execution time in microseconds
   */
  private static native long nativeGetExecutionTime(long storeHandle);

  /**
   * Gets the total fuel consumed by this store.
   *
   * @param storeHandle the native store handle
   * @return the total fuel consumed
   */
  private static native long nativeGetTotalFuelConsumed(long storeHandle);

  /**
   * Gets the total memory allocated by this store.
   *
   * @param storeHandle the native store handle
   * @return the total memory in bytes
   */
  private static native long nativeGetTotalMemoryBytes(long storeHandle);

  /**
   * Gets the currently used memory by this store.
   *
   * @param storeHandle the native store handle
   * @return the used memory in bytes
   */
  private static native long nativeGetUsedMemoryBytes(long storeHandle);

  /**
   * Gets the number of active instances in this store.
   *
   * @param storeHandle the native store handle
   * @return the number of active instances
   */
  private static native long nativeGetInstanceCount(long storeHandle);

  /**
   * Gets the fuel limit for this store.
   *
   * @param storeHandle the native store handle
   * @return the fuel limit, or -1 if no limit is set
   */
  private static native long nativeGetFuelLimit(long storeHandle);

  /**
   * Gets the memory limit for this store.
   *
   * @param storeHandle the native store handle
   * @return the memory limit in bytes, or -1 if no limit is set
   */
  private static native long nativeGetMemoryLimit(long storeHandle);

  /**
   * Gets the execution timeout for this store.
   *
   * @param storeHandle the native store handle
   * @return the execution timeout in seconds, or -1 if no timeout is set
   */
  private static native long nativeGetExecutionTimeout(long storeHandle);

  /**
   * Consumes a specific amount of fuel from the store.
   *
   * @param storeHandle the native store handle
   * @param fuel the amount of fuel to consume
   * @return the actual amount of fuel consumed
   */
  private static native long nativeConsumeFuel(long storeHandle, long fuel);

  /**
   * Increments the epoch counter for this store.
   *
   * @param storeHandle the native store handle
   */
  private static native void nativeIncrementEpoch(long storeHandle);

  /**
   * Sets the memory limit for this store.
   *
   * @param storeHandle the native store handle
   * @param bytes the memory limit in bytes (0 for unlimited)
   * @return true if successful, false otherwise
   */
  private static native boolean nativeSetMemoryLimit(long storeHandle, long bytes);

  /**
   * Sets the table element limit for this store.
   *
   * @param storeHandle the native store handle
   * @param elements the maximum number of table elements (0 for unlimited)
   * @return true if successful, false otherwise
   */
  private static native boolean nativeSetTableElementLimit(long storeHandle, long elements);

  /**
   * Sets the instance limit for this store.
   *
   * @param storeHandle the native store handle
   * @param count the maximum number of instances (0 for unlimited)
   * @return true if successful, false otherwise
   */
  private static native boolean nativeSetInstanceLimit(long storeHandle, int count);

  /**
   * Creates a new global variable.
   *
   * @param storeHandle the native store handle
   * @param valueType the WebAssembly value type code
   * @param isMutable 1 if mutable, 0 if immutable
   * @param valueComponents array containing value components [i32, i64, f32, f64, refId]
   * @return the native global handle, or 0 on failure
   */
  private static native long nativeCreateGlobal(
      long storeHandle, int valueType, int isMutable, Object[] valueComponents);

  /**
   * Creates a new WebAssembly table.
   *
   * @param storeHandle the native store handle
   * @param elementType the element type code (FUNCREF or EXTERNREF)
   * @param initialSize the initial number of elements
   * @param maxSize the maximum number of elements (-1 for unlimited)
   * @return the native table handle, or 0 on failure
   */
  private static native long nativeCreateTable(
      long storeHandle, int elementType, int initialSize, int maxSize);

  /**
   * Creates a new WebAssembly linear memory.
   *
   * @param storeHandle the native store handle
   * @param initialPages the initial number of 64KB pages
   * @param maxPages the maximum number of pages (-1 for unlimited)
   * @return the native memory handle, or 0 on failure
   */
  private static native long nativeCreateMemory(long storeHandle, int initialPages, int maxPages);

  private static native long nativeCreateSharedMemory(
      long storeHandle, int initialPages, int maxPages);

  /**
   * Destroys a native store and releases all associated resources.
   *
   * @param storeHandle the native store handle
   */
  private static native void nativeDestroyStore(long storeHandle);

  /**
   * Extracts value components from WasmValue for passing to native code.
   *
   * @param value the WasmValue to extract components from
   * @return array containing [i32Value, i64Value, f32Value, f64Value, refValue]
   */
  private Object[] extractValueComponents(final WasmValue value) {
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

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace captureBacktrace() {
    ensureNotClosed();
    return nativeCaptureBacktrace(nativeHandle);
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace forceCaptureBacktrace() {
    ensureNotClosed();
    return nativeForceCaptureBacktrace(nativeHandle);
  }

  @Override
  public void gc() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    nativeGc(nativeHandle);
  }

  @Override
  public <R> R throwException(final ai.tegmentum.wasmtime4j.ExnRef exceptionRef)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    if (exceptionRef == null) {
      throw new IllegalArgumentException("exceptionRef cannot be null");
    }
    if (!(exceptionRef instanceof JniExnRef)) {
      throw new IllegalArgumentException("ExnRef must be a JniExnRef instance");
    }

    final JniExnRef jniExnRef = (JniExnRef) exceptionRef;
    nativeThrowException(nativeHandle, jniExnRef.getNativeHandle());
    // This should never be reached - the native call always throws
    throw new ai.tegmentum.wasmtime4j.exception.WasmException("Exception was thrown");
  }

  @Override
  public ai.tegmentum.wasmtime4j.ExnRef takePendingException()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    final long exnRefHandle = nativeTakePendingException(nativeHandle);
    if (exnRefHandle == 0) {
      return null;
    }
    return new JniExnRef(exnRefHandle, nativeHandle);
  }

  @Override
  public boolean hasPendingException() {
    if (isClosed()) {
      return false;
    }
    return nativeHasPendingException(nativeHandle);
  }

  @Override
  public java.util.concurrent.CompletableFuture<Void> gcAsync()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          nativeGcAsync(nativeHandle);
          return null;
        });
  }

  @Override
  public void epochDeadlineAsyncYieldAndUpdate(final long deltaTicks)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    if (deltaTicks < 0) {
      throw new IllegalArgumentException("deltaTicks cannot be negative");
    }
    nativeEpochDeadlineAsyncYieldAndUpdate(nativeHandle, deltaTicks);
  }

  @Override
  public void epochDeadlineTrap() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    nativeEpochDeadlineTrap(nativeHandle);
  }

  @Override
  public void epochDeadlineCallback(
      final ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback callback)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    // Store callback reference to prevent GC
    this.epochDeadlineCallback = callback;
    if (callback == null) {
      nativeClearEpochDeadlineCallback(nativeHandle);
    } else {
      nativeSetEpochDeadlineCallback(nativeHandle);
    }
  }

  // Callback holder to prevent garbage collection
  private ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback epochDeadlineCallback;

  // Called from native code when epoch deadline is reached
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private long onEpochDeadlineReached(final long currentEpoch) {
    if (epochDeadlineCallback == null) {
      return -1; // Signal trap
    }
    final ai.tegmentum.wasmtime4j.Store.EpochDeadlineAction action =
        epochDeadlineCallback.onEpochDeadline(currentEpoch);
    if (action.shouldContinue()) {
      return action.getDeltaTicks();
    }
    return -1; // Signal trap
  }

  private native ai.tegmentum.wasmtime4j.debug.WasmBacktrace nativeCaptureBacktrace(long storeHandle);

  private native ai.tegmentum.wasmtime4j.debug.WasmBacktrace nativeForceCaptureBacktrace(
      long storeHandle);

  private native void nativeGc(long storeHandle);

  private native void nativeThrowException(long storeHandle, long exnRefHandle);

  private native long nativeTakePendingException(long storeHandle);

  private native boolean nativeHasPendingException(long storeHandle);

  private native void nativeGcAsync(long storeHandle);

  private static native void nativeEpochDeadlineAsyncYieldAndUpdate(
      long storeHandle, long deltaTicks);

  private static native void nativeEpochDeadlineTrap(long storeHandle);

  private native void nativeSetEpochDeadlineCallback(long storeHandle);

  private native void nativeClearEpochDeadlineCallback(long storeHandle);

  // Call hook support
  private ai.tegmentum.wasmtime4j.func.CallHookHandler callHookHandler;
  private ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler asyncCallHookHandler;

  @Override
  public void setCallHook(final ai.tegmentum.wasmtime4j.func.CallHookHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    this.callHookHandler = handler;
    if (handler == null) {
      nativeClearCallHook(nativeHandle);
    } else {
      nativeSetCallHook(nativeHandle);
    }
  }

  @Override
  public void setCallHookAsync(final ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    this.asyncCallHookHandler = handler;
    if (handler == null) {
      nativeClearCallHookAsync(nativeHandle);
    } else {
      nativeSetCallHookAsync(nativeHandle);
    }
  }

  // Called from native code when a call hook event occurs
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private void onCallHook(final int hookType)
      throws ai.tegmentum.wasmtime4j.exception.TrapException {
    if (callHookHandler != null) {
      callHookHandler.onCallHook(ai.tegmentum.wasmtime4j.func.CallHook.fromValue(hookType));
    }
  }

  // Called from native code for async call hook
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private void onCallHookAsync(final int hookType) {
    if (asyncCallHookHandler != null) {
      asyncCallHookHandler.onCallHook(ai.tegmentum.wasmtime4j.func.CallHook.fromValue(hookType));
    }
  }

  private native void nativeSetCallHook(long storeHandle);

  private native void nativeClearCallHook(long storeHandle);

  private native void nativeSetCallHookAsync(long storeHandle);

  private native void nativeClearCallHookAsync(long storeHandle);

  // ===== Concurrency Methods =====

  @Override
  public <T, R> R runConcurrent(final ai.tegmentum.wasmtime4j.concurrent.ConcurrentTask<T, R> task)
      throws WasmException {
    ensureNotClosed();
    JniValidation.requireNonNull(task, "task");
    // Create accessor wrapper
    @SuppressWarnings("unchecked")
    final JniAccessor<T> accessor = new JniAccessor<>((T) customData, nativeHandle);
    try {
      return task.execute(accessor);
    } finally {
      accessor.invalidate();
    }
  }

  @Override
  public <R> ai.tegmentum.wasmtime4j.concurrent.JoinHandle<R> spawn(
      final ai.tegmentum.wasmtime4j.concurrent.SpawnableTask<R> task) throws WasmException {
    ensureNotClosed();
    JniValidation.requireNonNull(task, "task");
    return new JniJoinHandle<>(task);
  }

  // ===== Debug Methods =====

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.debug.DebugFrame> debugFrames() throws WasmException {
    ensureNotClosed();
    // Return debug frames from native (empty if debugging not enabled)
    final String framesJson = nativeGetDebugFrames(nativeHandle);
    if (framesJson == null || framesJson.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    return parseDebugFrames(framesJson);
  }

  private java.util.List<ai.tegmentum.wasmtime4j.debug.DebugFrame> parseDebugFrames(final String json) {
    // Simple JSON parsing for debug frames
    java.util.List<ai.tegmentum.wasmtime4j.debug.DebugFrame> frames = new java.util.ArrayList<>();
    // For now return empty list - native implementation will provide real data
    return frames;
  }

  private ai.tegmentum.wasmtime4j.debug.DebugHandler debugHandler;

  @Override
  public void setDebugHandler(final ai.tegmentum.wasmtime4j.debug.DebugHandler handler)
      throws WasmException {
    ensureNotClosed();
    this.debugHandler = handler;
    if (handler == null) {
      nativeClearDebugHandler(nativeHandle);
    } else {
      nativeSetDebugHandler(nativeHandle);
    }
  }

  // ===== Fuel Async Methods =====

  private long fuelAsyncYieldInterval = 0;

  @Override
  public void setFuelAsyncYieldInterval(final long interval) throws WasmException {
    ensureNotClosed();
    if (interval < 0) {
      throw new IllegalArgumentException("Interval cannot be negative");
    }
    this.fuelAsyncYieldInterval = interval;
    nativeSetFuelAsyncYieldInterval(nativeHandle, interval);
  }

  @Override
  public long getFuelAsyncYieldInterval() {
    return fuelAsyncYieldInterval;
  }

  // Native methods for new functionality
  private native String nativeGetDebugFrames(long storeHandle);

  private native void nativeSetDebugHandler(long storeHandle);

  private native void nativeClearDebugHandler(long storeHandle);

  private native void nativeSetFuelAsyncYieldInterval(long storeHandle, long interval);

  // ===== Resource Limiter Methods =====

  private ResourceLimiter resourceLimiter;
  private AsyncResourceLimiter asyncResourceLimiter;

  @Override
  public void limiter(final ResourceLimiter limiter) throws WasmException {
    ensureNotClosed();
    this.resourceLimiter = limiter;
    if (limiter == null) {
      nativeClearLimiter(nativeHandle);
    } else {
      nativeSetLimiter(nativeHandle, limiter.getId());
    }
  }

  @Override
  public void limiterAsync(final AsyncResourceLimiter limiter) throws WasmException {
    ensureNotClosed();
    this.asyncResourceLimiter = limiter;
    if (limiter == null) {
      nativeClearAsyncLimiter(nativeHandle);
    } else {
      nativeSetAsyncLimiter(nativeHandle);
    }
  }

  @Override
  public ResourceLimiter getLimiter() {
    return resourceLimiter;
  }

  // Called from native code when async memory growth is requested
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private boolean onAsyncMemoryGrowRequest(final long currentPages, final long requestedPages) {
    if (asyncResourceLimiter == null) {
      return true;
    }
    try {
      return asyncResourceLimiter.allowMemoryGrow(currentPages, requestedPages).get();
    } catch (Exception e) {
      LOGGER.warning("Async memory grow callback failed: " + e.getMessage());
      return false;
    }
  }

  // Called from native code when async table growth is requested
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private boolean onAsyncTableGrowRequest(
      final long currentElements, final long requestedElements) {
    if (asyncResourceLimiter == null) {
      return true;
    }
    try {
      return asyncResourceLimiter.allowTableGrow(currentElements, requestedElements).get();
    } catch (Exception e) {
      LOGGER.warning("Async table grow callback failed: " + e.getMessage());
      return false;
    }
  }

  private native void nativeSetLimiter(long storeHandle, long limiterId);

  private native void nativeClearLimiter(long storeHandle);

  private native void nativeSetAsyncLimiter(long storeHandle);

  private native void nativeClearAsyncLimiter(long storeHandle);

  // ===== Async Creation Methods =====

  @Override
  public CompletableFuture<WasmTable> createTableAsync(
      final WasmValueType elementType, final int initialSize, final int maxSize) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return createTable(elementType, initialSize, maxSize);
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public CompletableFuture<WasmMemory> createMemoryAsync(
      final int initialPages, final int maxPages) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return createMemory(initialPages, maxPages);
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  // ===== Inner Classes =====

  /** JNI implementation of Accessor for concurrent store access. */
  private static final class JniAccessor<T>
      implements ai.tegmentum.wasmtime4j.concurrent.Accessor<T> {
    private final T data;
    private final long storeId;
    private volatile boolean valid = true;

    JniAccessor(final T data, final long storeId) {
      this.data = data;
      this.storeId = storeId;
    }

    @Override
    public T getData() {
      return data;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public long getStoreId() {
      return storeId;
    }

    @Override
    public void complete() throws WasmException {
      valid = false;
    }

    @Override
    public void fail(final Throwable error) throws WasmException {
      valid = false;
    }

    void invalidate() {
      valid = false;
    }
  }

  /** JNI implementation of JoinHandle for spawned tasks. */
  private static final class JniJoinHandle<T>
      implements ai.tegmentum.wasmtime4j.concurrent.JoinHandle<T> {
    private static final java.util.concurrent.atomic.AtomicLong ID_COUNTER =
        new java.util.concurrent.atomic.AtomicLong(0);

    private final long id;
    private final java.util.concurrent.CompletableFuture<T> future;

    JniJoinHandle(final ai.tegmentum.wasmtime4j.concurrent.SpawnableTask<T> task) {
      this.id = ID_COUNTER.incrementAndGet();
      this.future =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> {
                try {
                  return task.run();
                } catch (ai.tegmentum.wasmtime4j.exception.WasmException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    @Override
    public T join() throws WasmException, InterruptedException {
      try {
        return future.get();
      } catch (java.util.concurrent.ExecutionException e) {
        throw new WasmException("Task failed: " + e.getCause().getMessage(), e.getCause());
      }
    }

    @Override
    public T join(final long timeout, final java.util.concurrent.TimeUnit unit)
        throws WasmException, InterruptedException {
      try {
        return future.get(timeout, unit);
      } catch (java.util.concurrent.ExecutionException e) {
        throw new WasmException("Task failed: " + e.getCause().getMessage(), e.getCause());
      } catch (java.util.concurrent.TimeoutException e) {
        throw new WasmException("Task timed out", e);
      }
    }

    @Override
    public java.util.concurrent.CompletableFuture<T> toFuture() {
      return future;
    }

    @Override
    public boolean isDone() {
      return future.isDone();
    }

    @Override
    public boolean isCancelled() {
      return future.isCancelled();
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public long getId() {
      return id;
    }

    @Override
    public ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus getStatus() {
      if (future.isCancelled()) {
        return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.CANCELLED;
      }
      if (future.isDone()) {
        if (future.isCompletedExceptionally()) {
          return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.FAILED;
        }
        return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.COMPLETED;
      }
      return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.RUNNING;
    }
  }
}
