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
}
