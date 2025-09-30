package ai.tegmentum.wasmtime4j;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for streaming WebAssembly module instantiation.
 *
 * <p>StreamingInstantiator enables progressive instantiation of WebAssembly modules during or
 * immediately after compilation, allowing for faster time-to-ready and reduced latency for large
 * modules.
 *
 * @since 1.0.0
 */
public interface StreamingInstantiator extends AutoCloseable {

  /**
   * Begins streaming instantiation of the compiled module.
   *
   * <p>This method creates a WebAssembly instance progressively, allowing execution to begin as
   * soon as critical functions are available.
   *
   * @param store the store to create the instance in
   * @param config streaming instantiation configuration
   * @return a CompletableFuture that completes when instantiation is ready for use
   * @throws IllegalArgumentException if store or config is null
   */
  CompletableFuture<Instance> instantiateStreaming(Store store, InstantiationConfig config);

  /**
   * Begins streaming instantiation with imports.
   *
   * <p>This method creates a WebAssembly instance with the specified imports, performing
   * progressive import resolution and linking.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @param config streaming instantiation configuration
   * @return a CompletableFuture that completes when instantiation is ready for use
   * @throws IllegalArgumentException if any parameter is null
   */
  CompletableFuture<Instance> instantiateStreaming(
      Store store, ImportMap imports, InstantiationConfig config);

  /**
   * Creates a streaming instance handle for manual control over instantiation.
   *
   * <p>This method returns a handle that allows fine-grained control over the instantiation
   * process, including priority-based function compilation and lazy loading.
   *
   * @param store the store to create the instance in
   * @param config streaming instantiation configuration
   * @return a StreamingInstanceHandle for manual instantiation control
   * @throws IllegalArgumentException if store or config is null
   */
  StreamingInstanceHandle createStreamingInstance(Store store, InstantiationConfig config);

  /**
   * Creates a streaming instance handle with imports.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @param config streaming instantiation configuration
   * @return a StreamingInstanceHandle for manual instantiation control
   * @throws IllegalArgumentException if any parameter is null
   */
  StreamingInstanceHandle createStreamingInstance(
      Store store, ImportMap imports, InstantiationConfig config);

  /**
   * Pre-instantiates the module for fast instantiation.
   *
   * <p>This method performs as much instantiation work as possible ahead of time, creating an
   * InstancePre that can be instantiated quickly when needed.
   *
   * @param config configuration for pre-instantiation
   * @return a CompletableFuture that completes with an InstancePre
   * @throws IllegalArgumentException if config is null
   */
  CompletableFuture<InstancePre> preInstantiate(InstantiationConfig config);

  /**
   * Pre-instantiates the module with imports for fast instantiation.
   *
   * @param imports the import definitions for the module
   * @param config configuration for pre-instantiation
   * @return a CompletableFuture that completes with an InstancePre
   * @throws IllegalArgumentException if imports or config is null
   */
  CompletableFuture<InstancePre> preInstantiate(ImportMap imports, InstantiationConfig config);

  /**
   * Gets the module associated with this streaming instantiator.
   *
   * @return the Module being instantiated
   */
  Module getModule();

  /**
   * Gets the current instantiation statistics.
   *
   * <p>This method provides real-time information about the instantiation progress, including
   * functions prepared, memory allocated, and performance metrics.
   *
   * @return current streaming instantiation statistics
   */
  InstantiationStatistics getStatistics();

  /**
   * Cancels any ongoing streaming instantiation operations.
   *
   * @param mayInterruptIfRunning whether to interrupt threads currently executing instantiation
   * @return true if cancellation was successful
   */
  boolean cancel(boolean mayInterruptIfRunning);

  /**
   * Registers a progress listener for streaming instantiation events.
   *
   * @param listener the progress event listener
   * @throws IllegalArgumentException if listener is null
   */
  void addProgressListener(InstantiationProgressListener listener);

  /**
   * Removes a previously registered progress listener.
   *
   * @param listener the progress event listener to remove
   * @throws IllegalArgumentException if listener is null
   */
  void removeProgressListener(InstantiationProgressListener listener);

  /** Closes the streaming instantiator and releases associated resources. */
  @Override
  void close();
}
