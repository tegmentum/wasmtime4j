package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * Interface for WASI component operations.
 *
 * <p>A WasiComponent represents a WebAssembly component that has been loaded and is ready
 * for instantiation and execution with WASI capabilities.
 *
 * @since 1.0.0
 */
public interface WasiComponent extends Closeable {

  /**
   * Gets the name of the component if available.
   *
   * @return the component name, or null if not specified
   */
  String getName();

  /**
   * Checks if the component is still valid and usable.
   *
   * @return true if the component is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the component and releases associated resources.
   *
   * <p>After calling this method, the component becomes invalid and should not be used.
   */
  @Override
  void close();
}