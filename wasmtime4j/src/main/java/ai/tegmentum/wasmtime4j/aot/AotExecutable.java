package ai.tegmentum.wasmtime4j.aot;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.io.Closeable;

/**
 * Represents an ahead-of-time compiled WebAssembly executable.
 *
 * <p>AotExecutable is the result of AOT compilation and represents a fully compiled and optimized
 * module that can be executed with minimal overhead. It provides the fastest possible startup time
 * by eliminating the need for runtime compilation.
 *
 * <p>AOT executables are platform-specific and can only be executed on compatible engines and
 * platforms.
 *
 * @since 1.0.0
 */
public interface AotExecutable extends Closeable {

  /**
   * Creates an instance of this AOT executable in the given store.
   *
   * <p>This method provides the fastest possible instantiation by using pre-compiled code without
   * any compilation overhead.
   *
   * @param store the store to create the instance in
   * @return a new Instance of this executable
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store is null
   */
  Instance instantiate(final Store store) throws WasmException;

  /**
   * Gets the target platform this executable was compiled for.
   *
   * @return the target platform
   */
  TargetPlatform getTargetPlatform();

  /**
   * Checks if this executable is compatible with the given engine.
   *
   * <p>Compatibility is determined by comparing the engine configuration with the compilation
   * settings used to create this executable.
   *
   * @param engine the engine to check compatibility with
   * @return true if the executable is compatible with the engine, false otherwise
   * @throws IllegalArgumentException if engine is null
   */
  boolean isCompatibleWith(final Engine engine);

  /**
   * Gets the AOT options that were used to compile this executable.
   *
   * @return the AOT compilation options
   */
  AotOptions getCompilationOptions();

  /**
   * Gets metadata about this AOT executable.
   *
   * @return executable metadata
   */
  AotExecutableMetadata getMetadata();

  /**
   * Checks if the executable is still valid and usable.
   *
   * @return true if the executable is valid, false otherwise
   */
  boolean isValid();

  /**
   * Gets the size of the executable code in bytes.
   *
   * @return the executable size
   */
  long getSize();

  /**
   * Closes the executable and releases associated resources.
   *
   * <p>After closing, the executable becomes invalid and should not be used.
   */
  @Override
  void close();
}