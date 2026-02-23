package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
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
   * Gets the runtime that created this engine.
   *
   * <p>This method returns the WasmRuntime instance that was used to create this engine. This is
   * useful for operations that need to use the same runtime context, such as creating a Linker for
   * this engine.
   *
   * @return the WasmRuntime that created this engine
   */
  WasmRuntime getRuntime();

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
   * Precompiles a WebAssembly component into a serialized form for ahead-of-time (AOT) usage.
   *
   * <p>This method compiles a WebAssembly component binary into a serialized form that can be later
   * loaded without needing to recompile. This is useful for caching compiled components to disk or
   * distributing pre-compiled components.
   *
   * <p>The input must be a valid WebAssembly component (not a core module). For precompiling core
   * modules, use {@link #precompileModule(byte[])}.
   *
   * @param wasmBytes the WebAssembly component bytecode to precompile
   * @return the precompiled serialized component bytes
   * @throws WasmException if precompilation fails due to invalid bytecode or engine issues
   * @throws IllegalArgumentException if wasmBytes is null or empty
   * @since 1.1.0
   */
  byte[] precompileComponent(final byte[] wasmBytes) throws WasmException;

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
   * Compiles a WebAssembly module from a file path.
   *
   * <p>The file can contain either binary WebAssembly (.wasm) or WebAssembly Text (.wat) format. The
   * format is auto-detected based on file contents.
   *
   * @param path the path to the WebAssembly file
   * @return a compiled Module
   * @throws WasmException if compilation fails due to invalid bytecode or engine issues
   * @throws IllegalArgumentException if path is null
   * @since 1.1.0
   */
  Module compileFromFile(final java.nio.file.Path path) throws WasmException;

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
   * Detects whether a specific CPU feature is available on the current host.
   *
   * <p>This can be used to query for hardware capabilities like SSE4.2, AVX, AVX2, BMI1, BMI2,
   * LZCNT, POPCNT, etc. Feature names correspond to Cranelift ISA feature names.
   *
   * @param feature the CPU feature name to check (e.g., "sse4.2", "avx2")
   * @return an Optional containing true if the feature is detected, false if not detected, or
   *     empty if the feature name is not recognized
   * @throws IllegalArgumentException if feature is null
   * @since 1.1.0
   */
  java.util.Optional<Boolean> detectHostFeature(final String feature);

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
   * Checks if this engine shares the same configuration and compilation settings as another engine.
   *
   * <p>Two engines are considered the same if they were created with identical configurations and
   * will produce compatible compiled modules. This is useful for verifying that precompiled modules
   * from one engine can be used with another.
   *
   * @param other the other engine to compare with
   * @return true if both engines share the same configuration
   * @throws IllegalArgumentException if other is null
   * @since 1.1.0
   */
  boolean same(Engine other);

  /**
   * Checks if this engine was created with async support enabled.
   *
   * <p>When async support is enabled, the engine can create async stores and execute WebAssembly
   * code asynchronously. This is required for async WASI and non-blocking I/O operations.
   *
   * @return true if async support is enabled
   * @since 1.1.0
   */
  boolean isAsync();

  /**
   * Gets the pooling allocator metrics for this engine.
   *
   * <p>If the engine was configured to use a pooling allocator, this method returns metrics about
   * pool usage, allocations, and performance. If pooling allocation is not enabled, returns null.
   *
   * @return pooling allocator metrics, or null if pooling is not enabled
   * @since 1.1.0
   */
  default ai.tegmentum.wasmtime4j.pool.PoolStatistics getPoolingAllocatorMetrics() {
    return null;
  }

  /**
   * Detects whether the given bytes are a precompiled artifact produced by Wasmtime.
   *
   * <p>This method inspects the header of the bytes to determine if they look like a precompiled
   * WebAssembly module or component. This is useful for determining how to handle incoming bytes -
   * whether to compile them as source or deserialize them as precompiled.
   *
   * <p>Note that this method only performs a quick header check and does not validate the full
   * structure. A positive result does not guarantee that deserialization will succeed.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * byte[] bytes = Files.readAllBytes(path);
   * Precompiled type = engine.detectPrecompiled(bytes);
   * if (type == Precompiled.MODULE) {
   *     module = Module.deserialize(engine, bytes);
   * } else {
   *     module = engine.compileModule(bytes);
   * }
   * }</pre>
   *
   * @param bytes the bytes to inspect
   * @return the type of precompiled artifact, or null if not a precompiled artifact
   * @throws IllegalArgumentException if bytes is null
   * @since 1.1.0
   */
  Precompiled detectPrecompiled(byte[] bytes);

  /**
   * Detects whether the file at the given path contains a precompiled artifact.
   *
   * <p>This is a convenience method that reads the header of the file to determine if it contains a
   * precompiled WebAssembly module or component.
   *
   * @param path the path to the file to inspect
   * @return the type of precompiled artifact, or null if not a precompiled artifact
   * @throws IllegalArgumentException if path is null
   * @throws java.io.IOException if the file cannot be read
   * @since 1.1.0
   */
  default Precompiled detectPrecompiledFile(java.nio.file.Path path) throws java.io.IOException {
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }
    // Read first 16 bytes which should be enough for header detection
    byte[] header = new byte[16];
    try (java.io.InputStream is = java.nio.file.Files.newInputStream(path)) {
      int read = is.read(header);
      if (read < header.length) {
        header = java.util.Arrays.copyOf(header, read);
      }
    }
    return detectPrecompiled(header);
  }

  /**
   * Creates a new standalone shared WebAssembly linear memory.
   *
   * <p>Shared memory can be accessed by multiple threads simultaneously and supports atomic
   * operations. Unlike regular memory, shared memory does not require a Store context for creation
   * - only an Engine is needed.
   *
   * <p>The engine must be configured with threads support enabled ({@code wasmThreads(true)} in
   * {@link EngineConfig}).
   *
   * @param initialPages the initial number of 64KB pages
   * @param maxPages the maximum number of 64KB pages (required for shared memory)
   * @return a new shared WasmMemory instance
   * @throws WasmException if shared memory creation fails
   * @throws IllegalArgumentException if initialPages or maxPages is invalid
   * @since 1.1.0
   */
  WasmMemory createSharedMemory(int initialPages, int maxPages) throws WasmException;

  /**
   * Creates a weak reference to this engine.
   *
   * <p>The returned {@link WeakEngine} does not prevent this engine from being garbage collected or
   * closed. Use {@link WeakEngine#upgrade()} to attempt to obtain a strong reference.
   *
   * <p>This is useful for caches and other data structures that want to reference an engine without
   * preventing it from being cleaned up.
   *
   * @return a weak reference to this engine
   * @since 1.1.0
   */
  WeakEngine weak();

  /**
   * Creates a guest profiler for sampling-based WebAssembly performance profiling.
   *
   * <p>The profiler collects stack samples and produces profiles in the Firefox Processed Profile
   * Format (JSON), viewable at <a href="https://profiler.firefox.com/">profiler.firefox.com</a>.
   *
   * @param moduleName a label for the profile
   * @param interval the expected sampling interval (used as a hint by the profile format)
   * @param modules a map of module names to modules to include in the profile
   * @return a new GuestProfiler instance
   * @throws WasmException if profiler creation fails (e.g., guest debugging is enabled)
   * @since 1.1.0
   */
  ai.tegmentum.wasmtime4j.debug.GuestProfiler createGuestProfiler(
      String moduleName,
      java.time.Duration interval,
      java.util.Map<String, Module> modules)
      throws WasmException;

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
   *         .epochInterruption(true)
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
