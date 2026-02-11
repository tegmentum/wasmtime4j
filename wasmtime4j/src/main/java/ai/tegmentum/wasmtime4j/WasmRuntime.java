package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.TagType;

import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.config.Serializer;
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
   * Creates a new WasmRuntime builder for advanced configuration.
   *
   * @return a new WasmRuntimeBuilder
   */
  static WasmRuntimeBuilder builder() {
    return new WasmRuntimeBuilder();
  }

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
   * Compiles WebAssembly Text (WAT) format into a Module.
   *
   * <p>This method accepts WebAssembly modules in text format and compiles them to executable
   * bytecode. WAT compilation includes parsing, validation, and compilation to the same optimized
   * form as binary WebAssembly.
   *
   * @param engine the engine to use for compilation
   * @param watText the WebAssembly text format source
   * @return a compiled Module
   * @throws WasmException if compilation fails due to syntax errors or invalid WAT
   * @throws IllegalArgumentException if engine or watText is null
   * @since 1.0.0
   */
  Module compileModuleWat(final Engine engine, final String watText) throws WasmException;

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
   * Creates a new store with custom configuration.
   *
   * <p>This method allows creating a store with specific fuel limits, memory limits, and execution
   * timeouts configured at creation time.
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
  Store createStore(
      final Engine engine,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSeconds)
      throws WasmException;

  /**
   * Creates a new store with resource limits.
   *
   * <p>Resource limits are enforced during WebAssembly execution and control memory size, table
   * elements, and instance counts. Limits are applied per-resource (each memory/table can grow to
   * the limit independently).
   *
   * @param engine the engine to create the store for
   * @param limits the resource limits to apply
   * @return a new Store instance with the specified limits
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if engine or limits is null
   * @since 1.0.0
   */
  Store createStore(final Engine engine, final StoreLimits limits) throws WasmException;

  /**
   * Creates a new exception tag with the specified type in the given store.
   *
   * <p>Tags are used in the WebAssembly exception handling proposal to define types of exceptions
   * that can be thrown and caught. The tag type's function parameters define the payload types for
   * exceptions of this tag.
   *
   * @param store the store to create the tag in
   * @param tagType the type descriptor for this tag
   * @return a new Tag instance
   * @throws WasmException if tag creation fails
   * @throws IllegalArgumentException if store or tagType is null
   * @since 1.0.0
   */
  Tag createTag(Store store, TagType tagType) throws WasmException;

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
  <T> Linker<T> createLinker(final Engine engine) throws WasmException;

  /**
   * Creates a new linker with custom configuration.
   *
   * <p>This method allows creating a linker with specific settings such as allowing unknown exports
   * and enabling import shadowing.
   *
   * @param engine the engine to create the linker for
   * @param allowUnknownExports whether to allow modules with unknown exports
   * @param allowShadowing whether to allow import shadowing
   * @return a new Linker instance with the specified configuration
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  <T> Linker<T> createLinker(
      final Engine engine, final boolean allowUnknownExports, final boolean allowShadowing)
      throws WasmException;

  /**
   * Creates a new component linker for the given engine.
   *
   * <p>A component linker provides the mechanism to define host functions and bind imports before
   * instantiating WebAssembly components. Unlike the core module {@link Linker}, ComponentLinker
   * works with WIT (WebAssembly Interface Types) interface definitions and supports the full
   * Component Model type system.
   *
   * @param <T> the type of user data associated with stores used with this linker
   * @param engine the engine to create the component linker for
   * @return a new ComponentLinker instance
   * @throws WasmException if component linker creation fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  <T> ComponentLinker<T> createComponentLinker(final Engine engine) throws WasmException;

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
   * Creates a new WebAssembly component engine with default configuration.
   *
   * <p>The component engine provides support for the WebAssembly Component Model, enabling
   * composition, linking, and advanced orchestration of WebAssembly components.
   *
   * @return a new ComponentEngine instance
   * @throws WasmException if the component engine cannot be created
   */
  ComponentEngine createComponentEngine() throws WasmException;

  /**
   * Creates a new WebAssembly component engine with the specified configuration.
   *
   * @param config the component engine configuration
   * @return a new ComponentEngine instance
   * @throws WasmException if the component engine cannot be created
   * @throws IllegalArgumentException if config is null
   */
  ComponentEngine createComponentEngine(final ComponentEngineConfig config) throws WasmException;

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
   * Deserializes a previously serialized Module using the provided engine.
   *
   * <p>The serialized data must have been created by a module compiled with an engine that has the
   * same configuration as the provided engine. Deserialization is typically much faster than
   * compilation from WebAssembly bytecode.
   *
   * @param engine the engine to use for deserialization (must match original engine config)
   * @param serializedBytes the serialized module data
   * @return a deserialized Module ready for instantiation
   * @throws WasmException if deserialization fails
   * @throws IllegalArgumentException if engine or serializedBytes is null
   * @since 1.0.0
   */
  Module deserializeModule(final Engine engine, final byte[] serializedBytes) throws WasmException;

  /**
   * Creates a new Serializer with default configuration.
   *
   * <p>The default configuration provides reasonable caching and compression settings for most use
   * cases.
   *
   * @return a new Serializer instance
   * @throws WasmException if the serializer cannot be created
   * @since 1.0.0
   */
  Serializer createSerializer() throws WasmException;

  /**
   * Creates a new Serializer with custom configuration.
   *
   * <p>This method allows customizing cache size, compression settings, and other serialization
   * parameters for optimal performance.
   *
   * @param maxCacheSize the maximum cache size in bytes (0 for unlimited)
   * @param enableCompression whether to enable compression of serialized data
   * @param compressionLevel the compression level (0-9, higher is better compression)
   * @return a new Serializer instance with the specified configuration
   * @throws WasmException if the serializer cannot be created
   * @throws IllegalArgumentException if parameters are invalid
   * @since 1.0.0
   */
  Serializer createSerializer(
      final long maxCacheSize, final boolean enableCompression, final int compressionLevel)
      throws WasmException;

  // ===== DEBUGGING OPERATIONS =====

  /**
   * Gets debugging capabilities of this runtime.
   *
   * @return debugging capabilities information
   */
  String getDebuggingCapabilities();

  /**
   * Creates a new WASI context with default settings.
   *
   * <p>The WASI context provides configuration for WebAssembly System Interface functionality,
   * including file system access, environment variables, and command-line arguments.
   *
   * @return a new WasiContext instance
   * @throws WasmException if the context cannot be created
   * @since 1.0.0
   */
  WasiContext createWasiContext() throws WasmException;

  /**
   * Adds WASI imports to the specified linker.
   *
   * <p>This method configures the linker with all necessary WASI functions using the provided WASI
   * context for configuration.
   *
   * @param linker the linker to add WASI imports to
   * @param context the WASI context containing configuration
   * @throws WasmException if adding WASI imports fails
   * @throws IllegalArgumentException if linker or context is null
   * @since 1.0.0
   */
  void addWasiToLinker(Linker<WasiContext> linker, WasiContext context) throws WasmException;

  /**
   * Adds WASI Preview 2 functions to the given linker.
   *
   * <p>This method configures the linker with WASI Preview 2 function imports, enabling WebAssembly
   * components to access enhanced system functionality including:
   *
   * <ul>
   *   <li>Component-based filesystem operations with async I/O
   *   <li>Stream-based networking with HTTP/TCP/UDP support
   *   <li>Enhanced process and environment management
   *   <li>Component model resource management
   *   <li>WIT interface definitions and type validation
   * </ul>
   *
   * @param linker the linker to configure with WASI Preview 2 functions
   * @param context the WASI context containing configuration and state
   * @throws WasmException if adding WASI Preview 2 functions fails
   * @throws IllegalArgumentException if linker or context is null
   * @since 1.0.0
   */
  void addWasiPreview2ToLinker(Linker<WasiContext> linker, WasiContext context)
      throws WasmException;

  /**
   * Adds Component Model functions to the given linker.
   *
   * <p>This method configures the linker with WebAssembly Component Model functions, enabling:
   *
   * <ul>
   *   <li>Component compilation and instantiation
   *   <li>WIT interface parsing and validation
   *   <li>Component linking and composition
   *   <li>Resource management and lifecycle
   * </ul>
   *
   * @param linker the linker to configure with Component Model functions
   * @throws WasmException if adding Component Model functions fails
   * @throws IllegalArgumentException if linker is null
   * @since 1.0.0
   */
  void addComponentModelToLinker(Linker<WasiContext> linker) throws WasmException;

  /**
   * Creates a new WASI linker for the specified engine.
   *
   * <p>This method creates a linker pre-configured with WASI functions, simplifying the process of
   * linking WASI modules.
   *
   * @param engine the engine to create the linker for
   * @return a new WasiLinker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(Engine engine) throws WasmException;

  /**
   * Creates a new WASI linker with the specified configuration.
   *
   * <p>This method creates a linker pre-configured with WASI functions and the provided WASI
   * configuration.
   *
   * @param engine the engine to create the linker for
   * @param config the WASI configuration to use
   * @return a new WasiLinker instance with the specified configuration
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine or config is null
   * @since 1.0.0
   */
  ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(
      Engine engine, ai.tegmentum.wasmtime4j.wasi.WasiConfig config) throws WasmException;

  /**
   * Checks if this runtime supports the WebAssembly Component Model.
   *
   * <p>Component Model support enables advanced WebAssembly features including:
   *
   * <ul>
   *   <li>WIT (WebAssembly Interface Types) interfaces
   *   <li>Component composition and linking
   *   <li>Resource management and lifecycle
   *   <li>Interface validation and type checking
   * </ul>
   *
   * @return true if Component Model is supported, false otherwise
   * @since 1.0.0
   */
  boolean supportsComponentModel();

  /**
   * Gets the GC (Garbage Collection) runtime for this runtime instance.
   *
   * <p>The GC runtime provides access to WebAssembly GC proposal features including:
   *
   * <ul>
   *   <li>Struct and array type operations
   *   <li>Reference types (funcref, externref)
   *   <li>i31 reference support
   *   <li>GC heap management and statistics
   * </ul>
   *
   * <p>This method uses lazy initialization - the GC runtime is created with a default engine on
   * first access and cached for subsequent calls.
   *
   * @return the GC runtime instance
   * @throws WasmException if the GC runtime cannot be created
   * @throws UnsupportedOperationException if GC features are not supported by this runtime
   * @since 1.0.0
   */
  ai.tegmentum.wasmtime4j.gc.GcRuntime getGcRuntime() throws WasmException;

  /**
   * Gets the SIMD operations for this WebAssembly runtime.
   *
   * <p>The SIMD operations provide access to WebAssembly's SIMD features including 128-bit vector
   * operations, lane manipulation, and memory load/store operations.
   *
   * <p>This method uses lazy initialization - the SIMD operations are created on first access and
   * cached for subsequent calls.
   *
   * @return the SIMD operations instance
   * @throws WasmException if the SIMD operations cannot be created
   * @throws UnsupportedOperationException if SIMD features are not supported by this runtime
   * @since 1.0.0
   */
  ai.tegmentum.wasmtime4j.simd.SimdOperations getSimdOperations() throws WasmException;

  /**
   * Creates a new WASI-NN context for neural network inference operations.
   *
   * <p>The WASI-NN context provides access to machine learning inference capabilities, allowing
   * WebAssembly modules to load ML models and run inference. This is an experimental feature (Tier
   * 3 in Wasmtime) and may not be available in all builds.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * try (NnContext nn = runtime.createNnContext()) {
   *     if (nn.isEncodingSupported(NnGraphEncoding.ONNX)) {
   *         try (NnGraph graph = nn.loadGraph(modelBytes, NnGraphEncoding.ONNX, NnExecutionTarget.CPU)) {
   *             try (NnGraphExecutionContext exec = graph.createExecutionContext()) {
   *                 List<NnTensor> outputs = exec.computeByIndex(inputTensor);
   *             }
   *         }
   *     }
   * }
   * }</pre>
   *
   * @return a new NnContext instance
   * @throws ai.tegmentum.wasmtime4j.wasi.nn.NnException if WASI-NN is not available or context
   *     creation fails
   * @since 1.0.0
   */
  ai.tegmentum.wasmtime4j.wasi.nn.NnContext createNnContext()
      throws ai.tegmentum.wasmtime4j.wasi.nn.NnException;

  /**
   * Checks if WASI-NN (neural network inference) is available in this runtime.
   *
   * <p>WASI-NN is an experimental feature that requires specific Wasmtime build flags and backend
   * support (OpenVINO, ONNX Runtime, etc.). Use this method to check availability before attempting
   * to create an NnContext.
   *
   * @return true if WASI-NN is supported and available
   * @since 1.0.0
   */
  boolean isNnAvailable();

  /**
   * Deserializes a module from a file containing serialized module data.
   *
   * @param engine the engine to use for deserialization
   * @param path the path to the serialized module file
   * @return the deserialized Module
   * @throws WasmException if deserialization fails or file cannot be read
   * @throws IllegalArgumentException if engine or path is null
   * @since 1.0.0
   */
  Module deserializeModuleFile(Engine engine, java.nio.file.Path path) throws WasmException;

  /**
   * Closes the runtime and releases associated resources.
   *
   * <p>After calling this method, the runtime becomes invalid and should not be used. Any attempt
   * to use the runtime after closing may result in exceptions.
   */
  @Override
  void close();
}
