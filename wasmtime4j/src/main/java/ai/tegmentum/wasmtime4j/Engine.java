package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;

/**
 * WebAssembly compilation engine interface.
 *
 * <p>An Engine represents a WebAssembly compilation context that can be used to compile modules and
 * create stores. Engines are configured with compilation settings and can be reused across multiple
 * modules for efficiency.
 *
 * <p>Engines are thread-safe and can be shared across multiple threads for concurrent module
 * compilation and instantiation.
 *
 * @since 1.0.0
 */
public interface Engine extends Closeable {

  /**
   * Creates a new store associated with this engine.
   *
   * <p>A store represents an isolated execution context for WebAssembly instances. Each store
   * maintains its own linear memory, globals, and function state.
   *
   * @return a new Store instance
   * @throws WasmException if the store cannot be created
   */
  Store createStore() throws WasmException;

  /**
   * Creates a new store with custom data associated with this engine.
   *
   * @param data custom data to associate with the store
   * @return a new Store instance with the associated data
   * @throws WasmException if the store cannot be created
   */
  Store createStore(final Object data) throws WasmException;

  /**
   * Compiles WebAssembly bytecode into a module using this engine.
   *
   * <p>This method validates and compiles the provided WebAssembly bytecode. The compilation
   * process includes parsing, validation, and optimization.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @return a compiled Module
   * @throws WasmException if compilation fails due to invalid bytecode or engine issues
   * @throws IllegalArgumentException if wasmBytes is null
   */
  Module compileModule(final byte[] wasmBytes) throws WasmException;

  /**
   * Gets the configuration used by this engine.
   *
   * @return the engine configuration
   */
  EngineConfig getConfig();

  /**
   * Gets runtime statistics for this engine.
   *
   * @return the engine statistics
   */
  EngineStatistics getStatistics();

  /**
   * Checks if the engine is still valid and usable.
   *
   * @return true if the engine is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the engine and releases associated resources.
   *
   * <p>After closing, the engine becomes invalid and should not be used. Any stores or modules
   * created by this engine may also become invalid.
   */
  @Override
  void close();

  /**
   * Creates a new Engine with default configuration.
   *
   * @return a new Engine instance
   * @throws WasmException if the engine cannot be created
   */
  static Engine create() throws WasmException {
    return WasmRuntimeFactory.create().createEngine();
  }
}
