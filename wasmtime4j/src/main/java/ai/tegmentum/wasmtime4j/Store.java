package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;

/**
 * Represents a WebAssembly store.
 *
 * <p>A Store is an execution context that holds the runtime state for WebAssembly instances. Each
 * store maintains isolated linear memory, globals, and execution state. Objects like instances,
 * functions, and memories are tied to a specific store.
 *
 * <p>Stores are not thread-safe and should not be shared between threads without external
 * synchronization.
 *
 * @since 1.0.0
 */
public interface Store extends Closeable {

  /**
   * Gets the engine associated with this store.
   *
   * @return the Engine that created this store
   */
  Engine getEngine();

  /**
   * Gets custom data associated with this store, if any.
   *
   * @return the custom data, or null if none was set
   */
  Object getData();

  /**
   * Sets custom data to be associated with this store.
   *
   * @param data the custom data to associate
   */
  void setData(final Object data);

  /**
   * Sets the fuel amount available for WebAssembly execution.
   *
   * <p>Fuel is consumed during execution and can be used to limit the amount of computation
   * performed by WebAssembly code.
   *
   * @param fuel the amount of fuel to set
   * @throws IllegalArgumentException if fuel is negative
   */
  void setFuel(final long fuel) throws WasmException;

  /**
   * Gets the remaining fuel amount.
   *
   * @return the remaining fuel, or -1 if fuel consumption is disabled
   */
  long getFuel() throws WasmException;

  /**
   * Adds fuel to the store.
   *
   * @param fuel the amount of fuel to add
   * @throws IllegalArgumentException if fuel is negative
   */
  void addFuel(final long fuel) throws WasmException;

  /**
   * Sets the epoch deadline for WebAssembly execution.
   *
   * <p>Epochs provide a way to interrupt long-running WebAssembly code at regular intervals.
   *
   * @param ticks the number of epoch ticks before interruption
   */
  void setEpochDeadline(final long ticks) throws WasmException;

  /**
   * Consumes a specific amount of fuel from the store.
   *
   * <p>This method subtracts the specified amount of fuel from the store's fuel counter. If the
   * store doesn't have enough fuel, a WasmException is thrown.
   *
   * @param fuel the amount of fuel to consume
   * @return the remaining fuel after consumption
   * @throws WasmException if fuel consumption fails or insufficient fuel available
   * @throws IllegalArgumentException if fuel is negative
   */
  long consumeFuel(final long fuel) throws WasmException;

  /**
   * Gets the remaining fuel amount.
   *
   * @return the remaining fuel, or -1 if fuel consumption is disabled
   */
  long getRemainingFuel() throws WasmException;

  /**
   * Increments the epoch counter for this store.
   *
   * <p>This method manually advances the epoch counter, which can trigger epoch-based interruptions
   * if they are enabled.
   */
  void incrementEpoch() throws WasmException;

  /**
   * Sets the memory limit for this store.
   *
   * @param bytes maximum memory in bytes (0 for unlimited)
   * @throws IllegalArgumentException if bytes is negative
   */
  void setMemoryLimit(final long bytes) throws WasmException;

  /**
   * Sets the table element limit for this store.
   *
   * @param elements maximum number of table elements (0 for unlimited)
   * @throws IllegalArgumentException if elements is negative
   */
  void setTableElementLimit(final long elements) throws WasmException;

  /**
   * Sets the instance limit for this store.
   *
   * @param count maximum number of instances (0 for unlimited)
   * @throws IllegalArgumentException if count is negative
   */
  void setInstanceLimit(final int count) throws WasmException;

  /**
   * Creates a host function that can be imported by WebAssembly modules.
   *
   * <p>The created function will be bound to this store and can be added to import maps for module
   * instantiation. The function type must accurately describe the parameter and return types.
   *
   * @param name the name of the function (for debugging/logging purposes)
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @return a WasmFunction that can be used in import maps
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  WasmFunction createHostFunction(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException;

  /**
   * Creates a new WebAssembly global variable with the specified type and initial value.
   *
   * <p>Global variables can store values that persist across function calls and can be either
   * mutable or immutable. The created global will be bound to this store.
   *
   * @param valueType the type of values this global will store
   * @param isMutable true if the global should be mutable, false for immutable
   * @param initialValue the initial value to store in the global
   * @return a new WasmGlobal that can be used in import maps or accessed directly
   * @throws WasmException if global creation fails
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  WasmGlobal createGlobal(
      final WasmValueType valueType, final boolean isMutable, final WasmValue initialValue)
      throws WasmException;

  /**
   * Creates a new immutable WebAssembly global variable with the specified type and initial value.
   *
   * <p>This is a convenience method equivalent to calling {@link #createGlobal(WasmValueType,
   * boolean, WasmValue)} with {@code isMutable} set to {@code false}.
   *
   * @param valueType the type of values this global will store
   * @param initialValue the initial value to store in the global
   * @return a new immutable WasmGlobal
   * @throws WasmException if global creation fails
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  default WasmGlobal createImmutableGlobal(
      final WasmValueType valueType, final WasmValue initialValue) throws WasmException {
    return createGlobal(valueType, false, initialValue);
  }

  /**
   * Creates a new mutable WebAssembly global variable with the specified type and initial value.
   *
   * <p>This is a convenience method equivalent to calling {@link #createGlobal(WasmValueType,
   * boolean, WasmValue)} with {@code isMutable} set to {@code true}.
   *
   * @param valueType the type of values this global will store
   * @param initialValue the initial value to store in the global
   * @return a new mutable WasmGlobal
   * @throws WasmException if global creation fails
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  default WasmGlobal createMutableGlobal(
      final WasmValueType valueType, final WasmValue initialValue) throws WasmException {
    return createGlobal(valueType, true, initialValue);
  }

  /**
   * Creates a function reference from a host function.
   *
   * <p>Function references enable dynamic function dispatch and callbacks in WebAssembly programs.
   * They can be passed as parameters, stored in tables, and called indirectly.
   *
   * @param implementation the Java implementation of the function
   * @param functionType the WebAssembly function type signature
   * @return a FunctionReference that can be used as a WebAssembly value
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  FunctionReference createFunctionReference(
      final HostFunction implementation, final FunctionType functionType) throws WasmException;

  /**
   * Creates a function reference from an existing WebAssembly function.
   *
   * @param function the WebAssembly function to create a reference for
   * @return a FunctionReference for the given function
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if function is null
   */
  FunctionReference createFunctionReference(final WasmFunction function) throws WasmException;

  /**
   * Gets the callback registry for this store.
   *
   * <p>The callback registry provides centralized management of callbacks and asynchronous
   * operations between Java and WebAssembly.
   *
   * @return the callback registry
   */
  CallbackRegistry getCallbackRegistry();

  /**
   * Creates an instance of a WebAssembly module in this store.
   *
   * <p>This method instantiates a compiled module within this store's execution context, making its
   * exported functions, memory, and globals available for use.
   *
   * @param module the compiled module to instantiate
   * @return a new Instance of the module
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if module is null
   */
  Instance createInstance(final Module module) throws WasmException;

  /**
   * Checks if the store is still valid and usable.
   *
   * @return true if the store is valid, false otherwise
   */
  boolean isValid();

  /**
   * Gets the total amount of fuel consumed by this store since creation.
   *
   * <p>This method provides historical fuel consumption data for monitoring and profiling
   * WebAssembly execution patterns.
   *
   * @return the total fuel consumed since store creation
   * @throws WasmException if fuel tracking is not enabled
   * @since 1.0.0
   */
  long getTotalFuelConsumed() throws WasmException;

  /**
   * Gets the number of function executions performed in this store.
   *
   * <p>This counter includes all function calls made through this store, both host functions and
   * WebAssembly functions.
   *
   * @return the total number of function executions
   * @since 1.0.0
   */
  long getExecutionCount();

  /**
   * Gets the total execution time in microseconds for this store.
   *
   * <p>This includes time spent in both WebAssembly execution and host function calls made through
   * this store.
   *
   * @return the total execution time in microseconds
   * @since 1.0.0
   */
  long getTotalExecutionTimeMicros();

  /**
   * Closes the store and releases associated resources.
   *
   * <p>After closing, the store becomes invalid and should not be used. All instances and other
   * objects associated with this store may also become invalid.
   */
  @Override
  void close();

  /**
   * Creates a new Store for the given engine.
   *
   * @param engine the engine to create the store for
   * @return a new Store instance
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if engine is null
   */
  static Store create(final Engine engine) throws WasmException {
    return WasmRuntimeFactory.create().createStore(engine);
  }

  /**
   * Creates a new Store with custom configuration.
   *
   * <p>This factory method allows creating a store with specific fuel limits, memory limits, and
   * execution timeouts configured at creation time.
   *
   * @param engine the engine to create the store for
   * @param fuelLimit the initial fuel limit (0 for unlimited)
   * @param memoryLimitBytes the memory limit in bytes (0 for unlimited)
   * @param executionTimeoutSeconds the execution timeout in seconds (0 for unlimited)
   * @return a new Store instance with the specified configuration
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if engine is null or limits are negative
   * @since 1.0.0
   */
  static Store create(
      final Engine engine,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSeconds)
      throws WasmException {
    return WasmRuntimeFactory.create()
        .createStore(engine, fuelLimit, memoryLimitBytes, executionTimeoutSeconds);
  }
}
