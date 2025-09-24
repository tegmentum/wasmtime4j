package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * Interface for streaming WebAssembly module compilation.
 *
 * <p>StreamingCompiler enables progressive compilation of WebAssembly modules as data arrives,
 * allowing for efficient processing of large modules and network-delivered content. It supports
 * both push-based and pull-based streaming patterns with configurable buffering and backpressure
 * handling.
 *
 * @since 1.0.0
 */
public interface StreamingCompiler extends AutoCloseable {

  /**
   * Begins streaming compilation of WebAssembly bytecode.
   *
   * <p>This method initiates progressive compilation as data becomes available. The compilation
   * process can begin before all data is received, enabling faster time-to-ready for large
   * modules.
   *
   * @param input the input stream containing WebAssembly bytecode
   * @param config streaming compilation configuration
   * @return a CompletableFuture that completes when compilation is finished
   * @throws IllegalArgumentException if input or config is null
   */
  CompletableFuture<Module> compileStreaming(InputStream input, StreamingConfig config);

  /**
   * Begins streaming compilation from a reactive stream publisher.
   *
   * <p>This method supports reactive streams pattern with backpressure control, allowing the
   * caller to control the flow of data to the compiler.
   *
   * @param publisher the publisher of WebAssembly bytecode chunks
   * @param config streaming compilation configuration
   * @return a CompletableFuture that completes when compilation is finished
   * @throws IllegalArgumentException if publisher or config is null
   */
  CompletableFuture<Module> compileStreaming(
      Flow.Publisher<ByteBuffer> publisher, StreamingConfig config);

  /**
   * Begins streaming compilation with manual data feeding.
   *
   * <p>This method returns a feed handle that allows the caller to manually provide WebAssembly
   * bytecode chunks. This is useful for custom streaming scenarios or when integrating with
   * non-standard data sources.
   *
   * @param config streaming compilation configuration
   * @return a StreamingFeedHandle for manually feeding data
   * @throws IllegalArgumentException if config is null
   */
  StreamingFeedHandle startStreamingCompilation(StreamingConfig config);

  /**
   * Compiles WebAssembly module with progressive instantiation preparation.
   *
   * <p>This method combines streaming compilation with preparation for fast instantiation,
   * optimizing the overall module loading pipeline.
   *
   * @param input the input stream containing WebAssembly bytecode
   * @param config streaming compilation configuration
   * @param instantiationConfig configuration for instantiation preparation
   * @return a CompletableFuture that completes with a StreamingInstantiator
   * @throws IllegalArgumentException if any parameter is null
   */
  CompletableFuture<StreamingInstantiator> compileStreamingWithInstantiation(
      InputStream input, StreamingConfig config, InstantiationConfig instantiationConfig);

  /**
   * Gets the engine associated with this streaming compiler.
   *
   * @return the Engine used for compilation
   */
  Engine getEngine();

  /**
   * Gets the current compilation statistics.
   *
   * <p>This method provides real-time information about the compilation progress, including bytes
   * processed, compilation phases completed, and performance metrics.
   *
   * @return current streaming compilation statistics
   */
  StreamingStatistics getStatistics();

  /**
   * Cancels any ongoing streaming compilation operations.
   *
   * <p>This method attempts to gracefully cancel ongoing compilations. Already completed
   * compilations are not affected.
   *
   * @param mayInterruptIfRunning whether to interrupt threads currently executing compilation
   * @return true if cancellation was successful
   */
  boolean cancel(boolean mayInterruptIfRunning);

  /**
   * Registers a progress listener for streaming compilation events.
   *
   * <p>The listener will receive notifications about compilation progress, phase transitions, and
   * performance milestones.
   *
   * @param listener the progress event listener
   * @throws IllegalArgumentException if listener is null
   */
  void addProgressListener(StreamingProgressListener listener);

  /**
   * Removes a previously registered progress listener.
   *
   * @param listener the progress event listener to remove
   * @throws IllegalArgumentException if listener is null
   */
  void removeProgressListener(StreamingProgressListener listener);

  /**
   * Closes the streaming compiler and releases associated resources.
   *
   * <p>This method cancels any ongoing streaming operations and cleans up internal resources.
   * After closing, the compiler should not be used.
   */
  @Override
  void close();
}