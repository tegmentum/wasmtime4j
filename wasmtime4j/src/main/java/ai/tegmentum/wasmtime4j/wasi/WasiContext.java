package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * Main interface for WASI (WebAssembly System Interface) context operations.
 *
 * <p>This is the primary entry point for interacting with WASI functionality. A WasiContext
 * provides methods to create WASI-enabled WebAssembly components and manage their lifecycle.
 *
 * <p>The context abstracts away the underlying implementation details (JNI vs Panama FFI) and
 * provides a consistent API across different Java versions and native bindings.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiContext context = WasiFactory.createContext()) {
 *     WasiComponent component = context.createComponent(wasmBytes);
 *     // Use the component...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiContext extends Closeable {

  /**
   * Creates a new WASI component from WebAssembly bytecode.
   *
   * <p>This method loads and validates WebAssembly bytecode as a WASI component, making it
   * available for instantiation and execution with WASI capabilities.
   *
   * @param wasmBytes the WebAssembly bytecode for the component
   * @return a new WasiComponent instance
   * @throws WasmException if component creation fails
   * @throws IllegalArgumentException if wasmBytes is null
   */
  WasiComponent createComponent(final byte[] wasmBytes) throws WasmException;

  /**
   * Gets information about the WASI runtime implementation.
   *
   * @return runtime information including version and implementation type
   */
  WasiRuntimeInfo getRuntimeInfo();

  /**
   * Checks if the WASI context is still valid and usable.
   *
   * @return true if the context is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the WASI context and releases associated resources.
   *
   * <p>After calling this method, the context becomes invalid and should not be used. Any attempt
   * to use the context after closing may result in exceptions.
   */
  @Override
  void close();
}