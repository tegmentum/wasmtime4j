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
   * Closes the runtime and releases associated resources.
   *
   * <p>After calling this method, the runtime becomes invalid and should not be used. Any attempt
   * to use the runtime after closing may result in exceptions.
   */
  @Override
  void close();
}
