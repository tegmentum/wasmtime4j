package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * Main interface for WebAssembly runtime operations.
 *
 * <p>This is the primary entry point for interacting with WebAssembly modules. A WasmRuntime
 * provides methods to create engines, compile modules, and instantiate WebAssembly code.
 *
 * <p>The runtime abstracts away the underlying implementation details (JNI vs Panama FFI) and
 * provides a consistent API across different Java versions and native bindings.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
 *     Engine engine = runtime.createEngine();
 *     Module module = runtime.compileModule(engine, wasmBytes);
 *     Instance instance = runtime.instantiate(module);
 *     // Use the instance...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasmRuntime extends Closeable {

  /**
   * Creates a new WebAssembly engine with default configuration.
   *
   * <p>The engine is responsible for compilation and execution of WebAssembly modules. Multiple
   * engines can be created from a single runtime, allowing for isolated execution contexts.
   *
   * @return a new Engine instance
   * @throws WasmException if the engine cannot be created
   */
  Engine createEngine() throws WasmException;

  /**
   * Creates a new WebAssembly engine with custom configuration.
   *
   * @param config the engine configuration
   * @return a new Engine instance with the specified configuration
   * @throws WasmException if the engine cannot be created
   * @throws IllegalArgumentException if config is null
   */
  Engine createEngine(final EngineConfig config) throws WasmException;

  /**
   * Compiles WebAssembly bytecode into a Module.
   *
   * <p>This method performs validation and compilation of the WebAssembly bytecode. The resulting
   * module can be instantiated multiple times across different engines.
   *
   * @param engine the engine to use for compilation
   * @param wasmBytes the WebAssembly bytecode
   * @return a compiled Module
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if engine or wasmBytes is null
   */
  Module compileModule(final Engine engine, final byte[] wasmBytes) throws WasmException;

  /**
   * Creates a new store for the given engine.
   *
   * <p>A store represents an execution context that holds the runtime state for WebAssembly
   * instances. Each store maintains isolated linear memory, globals, and execution state.
   *
   * @param engine the engine to create the store for
   * @return a new Store instance
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if engine is null
   */
  Store createStore(final Engine engine) throws WasmException;

  /**
   * Creates a new linker for the given engine.
   *
   * <p>A linker provides the mechanism to define host functions and bind imports before
   * instantiating WebAssembly modules. It serves as a pre-instantiation environment where you can
   * register functions, memories, tables, and globals that modules can import.
   *
   * @param engine the engine to create the linker for
   * @return a new Linker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   */
  Linker createLinker(final Engine engine) throws WasmException;

  /**
   * Creates an instance of a WebAssembly module.
   *
   * <p>This method instantiates a compiled module, making its exported functions and memory
   * available for use. Each instance represents a separate execution context with its own memory
   * and global state.
   *
   * @param module the compiled module to instantiate
   * @return a new Instance of the module
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if module is null
   */
  Instance instantiate(final Module module) throws WasmException;

  /**
   * Creates an instance of a WebAssembly module with custom imports.
   *
   * @param module the compiled module to instantiate
   * @param imports the import definitions for the module
   * @return a new Instance of the module with the specified imports
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if module or imports is null
   */
  Instance instantiate(final Module module, final ImportMap imports) throws WasmException;

  /**
   * Gets information about the runtime implementation.
   *
   * @return runtime information including version and implementation type
   */
  RuntimeInfo getRuntimeInfo();

  /**
   * Checks if the runtime is still valid and usable.
   *
   * @return true if the runtime is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the runtime and releases associated resources.
   *
   * <p>After calling this method, the runtime becomes invalid and should not be used. Any attempt
   * to use the runtime after closing may result in exceptions.
   */
  @Override
  void close();
}
