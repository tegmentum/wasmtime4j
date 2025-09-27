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
  Linker createLinker(
      final Engine engine, final boolean allowUnknownExports, final boolean allowShadowing)
      throws WasmException;

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
   * Creates a new WebAssembly GC runtime for managing garbage-collected objects.
   *
   * <p>The GC runtime provides support for the WebAssembly Garbage Collection proposal, enabling
   * the creation and manipulation of GC-managed structs, arrays, and reference types.
   *
   * @param engine the engine to use for GC operations
   * @return a new GcRuntime instance
   * @throws WasmException if the GC runtime cannot be created
   * @throws IllegalArgumentException if engine is null
   */
  ai.tegmentum.wasmtime4j.gc.GcRuntime createGcRuntime(final Engine engine) throws WasmException;

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

  // ===== SIMD OPERATIONS =====

  /**
   * Checks if SIMD operations are supported by this runtime.
   *
   * @return true if SIMD is supported
   */
  boolean isSimdSupported();

  /**
   * Gets information about SIMD capabilities.
   *
   * @return string describing SIMD capabilities
   */
  String getSimdCapabilities();

  /**
   * Performs SIMD vector addition.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdAdd(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector subtraction.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdSubtract(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector multiplication.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdMultiply(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector division.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdDivide(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs saturated SIMD vector addition.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdAddSaturated(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector bitwise AND.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdAnd(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector bitwise OR.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdOr(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector bitwise XOR.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdXor(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector bitwise NOT.
   *
   * @param a vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdNot(final SimdOperations.V128 a) throws WasmException;

  /**
   * Performs SIMD vector equality comparison.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdEquals(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector less-than comparison.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdLessThan(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs SIMD vector greater-than comparison.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdGreaterThan(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Loads a SIMD vector from memory.
   *
   * @param memory WebAssembly memory
   * @param offset memory offset
   * @return loaded vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdLoad(final WasmMemory memory, final int offset) throws WasmException;

  /**
   * Loads a SIMD vector from memory with alignment.
   *
   * @param memory WebAssembly memory
   * @param offset memory offset
   * @param alignment required alignment
   * @return loaded vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdLoadAligned(
      final WasmMemory memory, final int offset, final int alignment) throws WasmException;

  /**
   * Stores a SIMD vector to memory.
   *
   * @param memory WebAssembly memory
   * @param offset memory offset
   * @param vector vector to store
   * @throws WasmException if operation fails
   */
  void simdStore(final WasmMemory memory, final int offset, final SimdOperations.V128 vector)
      throws WasmException;

  /**
   * Stores a SIMD vector to memory with alignment.
   *
   * @param memory WebAssembly memory
   * @param offset memory offset
   * @param vector vector to store
   * @param alignment required alignment
   * @throws WasmException if operation fails
   */
  void simdStoreAligned(
      final WasmMemory memory,
      final int offset,
      final SimdOperations.V128 vector,
      final int alignment)
      throws WasmException;

  /**
   * Converts integer vector to float vector.
   *
   * @param vector integer vector
   * @return float vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdConvertI32ToF32(final SimdOperations.V128 vector) throws WasmException;

  /**
   * Converts float vector to integer vector.
   *
   * @param vector float vector
   * @return integer vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdConvertF32ToI32(final SimdOperations.V128 vector) throws WasmException;

  /**
   * Extracts 32-bit integer lane from vector.
   *
   * @param vector source vector
   * @param lane lane index (0-3)
   * @return extracted value
   * @throws WasmException if operation fails
   */
  int simdExtractLaneI32(final SimdOperations.V128 vector, final int lane) throws WasmException;

  /**
   * Replaces 32-bit integer lane in vector.
   *
   * @param vector source vector
   * @param lane lane index (0-3)
   * @param value new value
   * @return modified vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdReplaceLaneI32(
      final SimdOperations.V128 vector, final int lane, final int value) throws WasmException;

  /**
   * Splats 32-bit integer to all lanes.
   *
   * @param value value to splat
   * @return vector with all lanes set to value
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdSplatI32(final int value) throws WasmException;

  /**
   * Splats 32-bit float to all lanes.
   *
   * @param value value to splat
   * @return vector with all lanes set to value
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdSplatF32(final float value) throws WasmException;

  /**
   * Performs vector shuffle operation.
   *
   * @param a first vector
   * @param b second vector
   * @param indices shuffle indices
   * @return shuffled vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdShuffle(
      final SimdOperations.V128 a, final SimdOperations.V128 b, final byte[] indices)
      throws WasmException;

  // ===== ADVANCED ARITHMETIC OPERATIONS =====

  /**
   * Performs fused multiply-add operation (a * b + c).
   *
   * @param a the first vector
   * @param b the second vector
   * @param c the third vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdFma(
      final SimdOperations.V128 a, final SimdOperations.V128 b, final SimdOperations.V128 c)
      throws WasmException;

  /**
   * Performs fused multiply-subtract operation (a * b - c).
   *
   * @param a the first vector
   * @param b the second vector
   * @param c the third vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdFms(
      final SimdOperations.V128 a, final SimdOperations.V128 b, final SimdOperations.V128 c)
      throws WasmException;

  /**
   * Performs vector reciprocal approximation.
   *
   * @param a the vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdReciprocal(final SimdOperations.V128 a) throws WasmException;

  /**
   * Performs vector square root.
   *
   * @param a the vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdSqrt(final SimdOperations.V128 a) throws WasmException;

  /**
   * Performs reciprocal square root approximation.
   *
   * @param a the vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdRsqrt(final SimdOperations.V128 a) throws WasmException;

  // ===== ADVANCED LOGICAL OPERATIONS =====

  /**
   * Performs bit population count (popcount) on vector.
   *
   * @param a the vector
   * @return the result vector with popcount for each lane
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdPopcount(final SimdOperations.V128 a) throws WasmException;

  /**
   * Performs variable bit shift left on vector lanes.
   *
   * @param a the vector to shift
   * @param b the shift amounts vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdShlVariable(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs variable bit shift right on vector lanes.
   *
   * @param a the vector to shift
   * @param b the shift amounts vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdShrVariable(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  // ===== VECTOR REDUCTION OPERATIONS =====

  /**
   * Performs horizontal sum reduction on vector.
   *
   * @param a the vector
   * @return the sum of all lanes as a scalar value
   * @throws WasmException if the operation fails
   */
  float simdHorizontalSum(final SimdOperations.V128 a) throws WasmException;

  /**
   * Performs horizontal minimum reduction on vector.
   *
   * @param a the vector
   * @return the minimum of all lanes as a scalar value
   * @throws WasmException if the operation fails
   */
  float simdHorizontalMin(final SimdOperations.V128 a) throws WasmException;

  /**
   * Performs horizontal maximum reduction on vector.
   *
   * @param a the vector
   * @return the maximum of all lanes as a scalar value
   * @throws WasmException if the operation fails
   */
  float simdHorizontalMax(final SimdOperations.V128 a) throws WasmException;

  // ===== ADVANCED COMPARISON AND SELECTION =====

  /**
   * Performs vector conditional selection based on mask.
   *
   * @param mask the mask vector (0 = select from b, non-zero = select from a)
   * @param a the first source vector
   * @param b the second source vector
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdSelect(
      final SimdOperations.V128 mask, final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

  /**
   * Performs vector blending with immediate mask.
   *
   * @param a the first vector
   * @param b the second vector
   * @param mask the immediate mask (8 bits for 4 lanes, 2 bits per lane)
   * @return the result vector
   * @throws WasmException if the operation fails
   */
  SimdOperations.V128 simdBlend(final SimdOperations.V128 a, final SimdOperations.V128 b, final int mask)
      throws WasmException;

  // ===== RELAXED OPERATIONS =====

  /**
   * Performs relaxed SIMD addition.
   *
   * @param a first vector
   * @param b second vector
   * @return result vector
   * @throws WasmException if operation fails
   */
  SimdOperations.V128 simdRelaxedAdd(final SimdOperations.V128 a, final SimdOperations.V128 b)
      throws WasmException;

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
   * Creates a new WASI linker for the given engine.
   *
   * <p>A WASI linker extends the standard linker functionality with WASI-specific capabilities,
   * providing fine-grained control over system interface access, filesystem permissions,
   * environment variables, and network access.
   *
   * @param engine the engine to create the WASI linker for
   * @return a new WasiLinker instance
   * @throws WasmException if WASI linker creation fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(final Engine engine)
      throws WasmException;

  /**
   * Creates a new WASI linker with the specified configuration.
   *
   * @param engine the engine to create the WASI linker for
   * @param config the WASI configuration to use
   * @return a new WasiLinker instance with the specified configuration
   * @throws WasmException if WASI linker creation fails
   * @throws IllegalArgumentException if engine or config is null
   * @since 1.0.0
   */
  ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(
      final Engine engine, final ai.tegmentum.wasmtime4j.wasi.WasiConfig config)
      throws WasmException;

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
   * Creates a debugger for the specified engine.
   *
   * @param engine the engine to create debugger for
   * @return a new Debugger instance
   * @throws WasmException if debugger cannot be created
   * @throws IllegalArgumentException if engine is null
   */
  ai.tegmentum.wasmtime4j.debug.Debugger createDebugger(final Engine engine) throws WasmException;

  /**
   * Checks if debugging is supported by this runtime.
   *
   * @return true if debugging is supported
   */
  boolean isDebuggingSupported();

  /**
   * Gets debugging capabilities of this runtime.
   *
   * @return debugging capabilities information
   */
  String getDebuggingCapabilities();

  /**
   * Creates a new WASI context with default settings.
   *
   * <p>The WASI context provides configuration for WebAssembly System Interface
   * functionality, including file system access, environment variables, and
   * command-line arguments.
   *
   * @return a new WasiContext instance
   * @throws WasmException if the context cannot be created
   * @since 1.0.0
   */
  WasiContext createWasiContext() throws WasmException;

  /**
   * Creates a new linker for the specified engine.
   *
   * <p>A linker manages imports and exports for WebAssembly modules, allowing
   * host functions and other modules to be linked together.
   *
   * @param <T> the type of user data associated with the store
   * @param engine the engine to create the linker for
   * @return a new Linker instance
   * @throws WasmException if the linker cannot be created
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  <T> Linker<T> createLinker(Engine engine) throws WasmException;

  /**
   * Adds WASI imports to the specified linker.
   *
   * <p>This method configures the linker with all necessary WASI functions
   * using the provided WASI context for configuration.
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
   * <p>This method configures the linker with WASI Preview 2 function imports, enabling
   * WebAssembly components to access enhanced system functionality including:
   * <ul>
   *   <li>Component-based filesystem operations with async I/O</li>
   *   <li>Stream-based networking with HTTP/TCP/UDP support</li>
   *   <li>Enhanced process and environment management</li>
   *   <li>Component model resource management</li>
   *   <li>WIT interface definitions and type validation</li>
   * </ul>
   *
   * @param linker the linker to configure with WASI Preview 2 functions
   * @param context the WASI context containing configuration and state
   * @throws WasmException if adding WASI Preview 2 functions fails
   * @throws IllegalArgumentException if linker or context is null
   * @since 1.0.0
   */
  void addWasiPreview2ToLinker(Linker<WasiContext> linker, WasiContext context) throws WasmException;

  /**
   * Adds Component Model functions to the given linker.
   *
   * <p>This method configures the linker with WebAssembly Component Model functions, enabling:
   * <ul>
   *   <li>Component compilation and instantiation</li>
   *   <li>WIT interface parsing and validation</li>
   *   <li>Component linking and composition</li>
   *   <li>Resource management and lifecycle</li>
   * </ul>
   *
   * @param linker the linker to configure with Component Model functions
   * @throws WasmException if adding Component Model functions fails
   * @throws IllegalArgumentException if linker is null
   * @since 1.0.0
   */
  void addComponentModelToLinker(Linker<WasiContext> linker) throws WasmException;

  /**
   * Checks if this runtime supports the WebAssembly Component Model.
   *
   * <p>Component Model support enables advanced WebAssembly features including:
   * <ul>
   *   <li>WIT (WebAssembly Interface Types) interfaces</li>
   *   <li>Component composition and linking</li>
   *   <li>Resource management and lifecycle</li>
   *   <li>Interface validation and type checking</li>
   * </ul>
   *
   * @return true if Component Model is supported, false otherwise
   * @since 1.0.0
   */
  boolean supportsComponentModel();

  /**
   * Deserializes a module from previously serialized bytes.
   *
   * <p>This method provides fast module loading by deserializing compiled
   * module data instead of recompiling from WebAssembly bytecode.
   *
   * @param engine the engine to use for deserialization
   * @param bytes the serialized module data
   * @return the deserialized Module
   * @throws WasmException if deserialization fails or data is invalid
   * @throws IllegalArgumentException if engine or bytes is null
   * @since 1.0.0
   */
  Module deserializeModule(Engine engine, byte[] bytes) throws WasmException;

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
