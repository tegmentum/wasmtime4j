package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.aot.AotCompiler;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.serialization.ModuleSerializer;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
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
   * Deserializes a module from serialized data using this engine.
   *
   * <p>This method reconstructs a module from previously serialized data. The deserialization
   * process validates compatibility and creates a module ready for instantiation.
   *
   * @param serializedData the serialized module data
   * @return a Module deserialized from the data
   * @throws WasmException if deserialization fails or data is incompatible
   * @throws IllegalArgumentException if serializedData is null
   * @since 1.0.0
   */
  Module deserializeModule(final byte[] serializedData) throws WasmException;

  /**
   * Deserializes a module from a SerializedModule using this engine.
   *
   * <p>This method is a convenience overload that extracts the data from a SerializedModule and
   * deserializes it.
   *
   * @param serializedModule the serialized module
   * @return a Module deserialized from the SerializedModule
   * @throws WasmException if deserialization fails or module is incompatible
   * @throws IllegalArgumentException if serializedModule is null
   * @since 1.0.0
   */
  Module deserializeModule(final SerializedModule serializedModule) throws WasmException;

  /**
   * Gets the module serializer associated with this engine.
   *
   * <p>The serializer can be used to serialize and deserialize modules compiled by this engine.
   * Different engines may have different serializers with varying capabilities.
   *
   * @return the module serializer for this engine
   * @since 1.0.0
   */
  ModuleSerializer getModuleSerializer();

  /**
   * Gets the AOT compiler associated with this engine.
   *
   * <p>The AOT compiler can be used to perform ahead-of-time compilation for optimized module
   * deployment. Not all engines may support AOT compilation.
   *
   * @return the AOT compiler for this engine
   * @throws UnsupportedOperationException if AOT compilation is not supported
   * @since 1.0.0
   */
  AotCompiler getAotCompiler();

  /**
   * Checks if this engine supports module serialization.
   *
   * @return true if serialization is supported, false otherwise
   * @since 1.0.0
   */
  boolean supportsModuleSerialization();

  /**
   * Checks if this engine supports AOT compilation.
   *
   * @return true if AOT compilation is supported, false otherwise
   * @since 1.0.0
   */
  boolean supportsAotCompilation();

  /**
   * Gets the version of the underlying WebAssembly runtime.
   *
   * @return the runtime version string
   * @since 1.0.0
   */
  String getRuntimeVersion();

  /**
   * Creates a new Engine with default configuration.
   *
   * @return a new Engine instance
   * @throws WasmException if the engine cannot be created
   */
  static Engine create() throws WasmException {
    return WasmRuntimeFactory.create().createEngine();
  }

  /**
   * Creates a new Engine with the specified configuration.
   *
   * @param config the engine configuration to use
   * @return a new Engine instance configured with the specified settings
   * @throws WasmException if the engine cannot be created
   * @throws IllegalArgumentException if config is null
   */
  static Engine create(final EngineConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Engine configuration cannot be null");
    }
    config.validate(); // Validate configuration before creating engine
    return WasmRuntimeFactory.create().createEngine(config);
  }
}
