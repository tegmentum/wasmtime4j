package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

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
   * Compiles WebAssembly Text format (WAT) into a module using this engine.
   *
   * <p>This method parses the WAT text format and compiles it into a WebAssembly module. The
   * compilation process includes WAT parsing, validation, and optimization.
   *
   * @param wat the WebAssembly text format (WAT) source code
   * @return a compiled Module
   * @throws WasmException if compilation fails due to invalid WAT syntax, validation errors, or
   *     engine issues
   * @throws IllegalArgumentException if wat is null or empty
   * @since 1.0.0
   */
  Module compileWat(final String wat) throws WasmException;

  /**
   * Precompiles WebAssembly bytecode into a serialized form for ahead-of-time (AOT) usage.
   *
   * <p>This method compiles the WebAssembly binary into a serialized form that can be later loaded
   * via {@code Module.deserialize()} without needing to recompile. This is useful for caching
   * compiled modules to disk or distributing pre-compiled modules.
   *
   * <p>The returned byte array can be stored and later loaded using the engine's deserialization
   * methods, avoiding the compilation overhead during runtime.
   *
   * @param wasmBytes the WebAssembly bytecode to precompile
   * @return the precompiled serialized module bytes
   * @throws WasmException if precompilation fails due to invalid bytecode or engine issues
   * @throws IllegalArgumentException if wasmBytes is null or empty
   * @since 1.0.0
   */
  byte[] precompileModule(final byte[] wasmBytes) throws WasmException;

  /**
   * Compiles WebAssembly bytecode from an input stream into a module using this engine.
   *
   * <p>This method reads the entire stream contents and then compiles the WebAssembly bytecode. The
   * compilation process includes parsing, validation, and optimization.
   *
   * <p>Note: Wasmtime requires the complete WebAssembly bytecode before compilation can begin, so
   * the stream is fully read into memory before compilation starts. For very large modules,
   * consider using memory-mapped files or chunked reading strategies.
   *
   * @param stream the input stream containing WebAssembly bytecode
   * @return a compiled Module
   * @throws WasmException if compilation fails due to invalid bytecode or engine issues
   * @throws IOException if reading from the stream fails
   * @throws IllegalArgumentException if stream is null
   * @since 1.0.0
   */
  Module compileFromStream(final InputStream stream) throws WasmException, IOException;

  /**
   * Increments the epoch counter.
   *
   * <p>This method is signal-safe and performs only an atomic increment. The epoch counter is used
   * for epoch-based interruption of WebAssembly execution.
   *
   * <p>This should typically be called from a separate thread or signal handler to periodically
   * increment the epoch, allowing for cooperative timeslicing of long-running WebAssembly code.
   *
   * <p>Stores created from this engine with an epoch deadline will be interrupted when the epoch
   * counter exceeds their deadline.
   *
   * @since 1.0.0
   */
  void incrementEpoch();

  /**
   * Gets the configuration used by this engine.
   *
   * @return the engine configuration
   */
  EngineConfig getConfig();

  /**
   * Checks if the engine is still valid and usable.
   *
   * @return true if the engine is valid, false otherwise
   */
  boolean isValid();

  /**
   * Checks if the engine supports a specific WebAssembly feature.
   *
   * <p>This method allows checking for feature support such as threads, SIMD, reference types, and
   * other WebAssembly proposals.
   *
   * @param feature the WebAssembly feature to check
   * @return true if the feature is supported, false otherwise
   * @throws IllegalArgumentException if feature is null
   * @since 1.0.0
   */
  boolean supportsFeature(final WasmFeature feature);

  /**
   * Gets the memory limit in pages for this engine.
   *
   * <p>Returns the maximum number of WebAssembly pages (64KB each) that can be allocated for linear
   * memory in this engine. A value of 0 indicates no limit.
   *
   * @return the memory limit in pages, or 0 for unlimited
   * @since 1.0.0
   */
  int getMemoryLimitPages();

  /**
   * Gets the stack size limit for this engine.
   *
   * <p>Returns the maximum stack size in bytes for WebAssembly execution. A value of 0 indicates
   * the default stack size is used.
   *
   * @return the stack size limit in bytes, or 0 for default
   * @since 1.0.0
   */
  long getStackSizeLimit();

  /**
   * Checks if fuel consumption is enabled for this engine.
   *
   * <p>Fuel consumption allows limiting the amount of computation that can be performed by
   * WebAssembly code, providing protection against infinite loops and other resource exhaustion
   * attacks.
   *
   * @return true if fuel consumption is enabled, false otherwise
   * @since 1.0.0
   */
  boolean isFuelEnabled();

  /**
   * Checks if epoch-based interruption is enabled for this engine.
   *
   * <p>Epoch interruption provides a way to interrupt long-running WebAssembly code at regular
   * intervals, enabling cooperative multitasking and timeout handling.
   *
   * @return true if epoch interruption is enabled, false otherwise
   * @since 1.0.0
   */
  boolean isEpochInterruptionEnabled();

  /**
   * Gets the maximum number of instances that can be created with this engine.
   *
   * <p>Returns the maximum number of WebAssembly instances that can be active simultaneously. A
   * value of 0 indicates no limit.
   *
   * @return the maximum number of instances, or 0 for unlimited
   * @since 1.0.0
   */
  int getMaxInstances();

  /**
   * Gets the reference count for this engine.
   *
   * <p>Returns the number of stores and other objects that are currently holding references to this
   * engine. This is useful for debugging and resource management.
   *
   * @return the current reference count
   * @since 1.0.0
   */
  long getReferenceCount();

  /**
   * Checks if coredump generation on trap is enabled for this engine.
   *
   * <p>When enabled, traps will generate a coredump that can be used for post-mortem debugging of
   * WebAssembly execution failures.
   *
   * @return true if coredump generation is enabled, false otherwise
   * @since 1.0.0
   */
  boolean isCoredumpOnTrapEnabled();

  /**
   * Captures current engine statistics.
   *
   * <p>Returns a snapshot of engine statistics including compilation metrics, memory usage, and
   * cache performance. This method is implemented by the underlying runtime (JNI or Panama).
   *
   * @return engine statistics snapshot
   * @since 1.0.0
   */
  default ai.tegmentum.wasmtime4j.performance.EngineStatistics captureStatistics() {
    return ai.tegmentum.wasmtime4j.performance.EngineStatistics.capture(this);
  }

  /**
   * Checks if the Pulley interpreter is being used.
   *
   * <p>Pulley is Wasmtime's portable, optimizing interpreter that can be used instead of or
   * alongside the Cranelift JIT compiler. This method returns true if the engine is configured to
   * use Pulley for WebAssembly execution.
   *
   * @return true if Pulley interpreter is being used, false if using Cranelift JIT
   * @since 1.1.0
   */
  default boolean isPulley() {
    return false;
  }

  /**
   * Gets the precompilation compatibility hash for this engine.
   *
   * <p>This hash uniquely identifies the compilation settings and target configuration of this
   * engine. Two engines with the same compatibility hash can share precompiled modules. This is
   * useful for caching and distributing precompiled WebAssembly modules.
   *
   * <p>The hash includes factors such as:
   *
   * <ul>
   *   <li>Target architecture (x86_64, aarch64, etc.)
   *   <li>Compiler settings and optimizations
   *   <li>Enabled WebAssembly features
   *   <li>Wasmtime version
   * </ul>
   *
   * @return the compatibility hash as a byte array, or empty array if not available
   * @since 1.1.0
   */
  default byte[] precompileCompatibilityHash() {
    return new byte[0];
  }

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

  /**
   * Creates a new Engine with the specified configuration.
   *
   * <p>The provided configuration allows customizing engine behavior including compilation
   * settings, resource limits, and feature enablement.
   *
   * @param config the engine configuration to use
   * @return a new Engine instance with the specified configuration
   * @throws WasmException if the engine cannot be created
   * @throws IllegalArgumentException if config is null
   * @since 1.0.0
   */
  static Engine create(final EngineConfig config) throws WasmException {
    return WasmRuntimeFactory.create().createEngine(config);
  }

  /**
   * Creates a new builder for configuring an Engine.
   *
   * <p>The builder provides a fluent API for configuring engine options. Call {@link
   * EngineConfig#consumeFuel(boolean)}, {@link EngineConfig#setEpochInterruption(boolean)}, and
   * other configuration methods, then create the engine with {@link #create(EngineConfig)}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Engine engine = Engine.create(
   *     Engine.builder()
   *         .consumeFuel(true)
   *         .setEpochInterruption(true)
   * );
   * }</pre>
   *
   * @return a new EngineConfig for configuring the engine
   * @since 1.0.0
   */
  static EngineConfig builder() {
    return new EngineConfig();
  }
}
