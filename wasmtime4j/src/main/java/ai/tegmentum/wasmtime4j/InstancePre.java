package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * A pre-instantiated WebAssembly module optimized for fast instantiation.
 *
 * <p>InstancePre represents a WebAssembly module that has been prepared for instantiation with most
 * of the expensive setup work already completed. This allows for very fast creation of instances
 * when needed.
 *
 * <p>InstancePre is particularly useful in scenarios where the same module needs to be instantiated
 * multiple times, such as in serverless functions or request handling.
 *
 * @since 1.0.0
 */
public interface InstancePre extends Closeable {

  /**
   * Creates a new instance from this pre-instantiated module.
   *
   * <p>This method is optimized for speed and should be significantly faster than normal
   * instantiation since most preparation work has already been completed.
   *
   * @param store the store to create the instance in
   * @return a new Instance
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store is null
   */
  Instance instantiate(Store store) throws WasmException;

  /**
   * Creates a new instance with imports from this pre-instantiated module.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @return a new Instance with the specified imports
   * @throws WasmException if instantiation fails or imports don't match requirements
   * @throws IllegalArgumentException if store or imports is null
   */
  Instance instantiate(Store store, ImportMap imports) throws WasmException;

  /**
   * Asynchronously creates a new instance from this pre-instantiated module.
   *
   * <p>Requires the engine to be configured with {@code asyncSupport(true)}. This method uses the
   * native async instantiation API which allows for cooperative yielding during instantiation.
   *
   * <p>The default implementation wraps the synchronous {@link #instantiate(Store)} method in a
   * {@link CompletableFuture}. Implementations should override this to use native async
   * instantiation when available.
   *
   * @param store the store to create the instance in
   * @return a CompletableFuture that completes with the new Instance
   * @throws IllegalArgumentException if store is null
   * @since 1.1.0
   */
  default CompletableFuture<Instance> instantiateAsync(final Store store) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return instantiate(store);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Gets the module associated with this pre-instantiated module.
   *
   * @return the Module that was pre-instantiated
   */
  Module getModule();

  /**
   * Gets the engine that was used to create this pre-instantiated module.
   *
   * @return the Engine used for pre-instantiation
   */
  Engine getEngine();

  /**
   * Checks if this pre-instantiated module is still valid and usable.
   *
   * @return true if the module is valid, false otherwise
   */
  boolean isValid();

  /**
   * Gets the number of instances that have been created from this pre-instantiated module.
   *
   * @return instance count
   */
  long getInstanceCount();

  /**
   * Gets statistics about the pre-instantiation process.
   *
   * @return pre-instantiation statistics
   */
  PreInstantiationStatistics getStatistics();

  /**
   * Closes the pre-instantiated module and releases associated resources.
   *
   * <p>After closing, the module becomes invalid and should not be used. Any instances created from
   * this InstancePre remain valid.
   */
  @Override
  void close();
}
