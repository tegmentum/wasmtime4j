package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.InstantiationConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.StreamingCompiler;
import ai.tegmentum.wasmtime4j.StreamingConfig;
import ai.tegmentum.wasmtime4j.StreamingFeedHandle;
import ai.tegmentum.wasmtime4j.StreamingInstantiator;
import ai.tegmentum.wasmtime4j.StreamingProgressListener;
import ai.tegmentum.wasmtime4j.StreamingStatistics;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.logging.Logger;

/**
 * JNI implementation of the StreamingCompiler interface.
 *
 * <p>This class provides streaming compilation of WebAssembly modules through JNI calls to the
 * native Wasmtime library. It supports progressive compilation as data arrives, enabling efficient
 * processing of large modules and network-delivered content.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Progressive compilation with configurable buffering
 *   <li>Automatic resource management with {@link AutoCloseable}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Thread-safe operations
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniStreamingCompiler extends JniResource implements StreamingCompiler {

  private static final Logger LOGGER = Logger.getLogger(JniStreamingCompiler.class.getName());

  private final JniEngine engine;
  private final List<StreamingProgressListener> progressListeners;
  private final ExecutorService executorService;

  /**
   * Creates a new JNI streaming compiler with the given engine.
   *
   * @param engine the engine to use for compilation
   * @throws JniException if the streaming compiler cannot be created
   */
  JniStreamingCompiler(final JniEngine engine) {
    super(createNativeStreamingCompiler(engine.getNativeHandle()));
    this.engine = JniValidation.requireNonNull(engine, "engine");
    this.progressListeners = new CopyOnWriteArrayList<>();
    this.executorService =
        Executors.newCachedThreadPool(
            r -> {
              final Thread thread =
                  new Thread(r, "StreamingCompiler-" + System.identityHashCode(this));
              thread.setDaemon(true);
              return thread;
            });
    LOGGER.fine(
        "Created JNI streaming compiler with handle: 0x" + Long.toHexString(getNativeHandle()));
  }

  /**
   * Begins streaming compilation of WebAssembly bytecode.
   *
   * <p>This method initiates progressive compilation as data becomes available. The compilation
   * process can begin before all data is received, enabling faster time-to-ready for large modules.
   *
   * @param input the input stream containing WebAssembly bytecode
   * @param config streaming compilation configuration
   * @return a CompletableFuture that completes when compilation is finished
   * @throws IllegalArgumentException if input or config is null
   */
  @Override
  public CompletableFuture<Module> compileStreaming(
      final InputStream input, final StreamingConfig config) {
    JniValidation.requireNonNull(input, "input");
    JniValidation.requireNonNull(config, "config");
    ensureNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Feed data in chunks
            final byte[] buffer = new byte[config.getBufferSize()];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
              if (bytesRead > 0) {
                nativeFeedChunk(getNativeHandle(), buffer, bytesRead);
              }
            }

            // Complete compilation
            final long moduleHandle = nativeComplete(getNativeHandle());
            return new JniModule(moduleHandle);
          } catch (final IOException e) {
            throw new RuntimeException("Failed to read streaming input", e);
          } catch (final Exception e) {
            throw new RuntimeException("Failed to compile streaming module", e);
          }
        },
        executorService);
  }

  /**
   * Begins streaming compilation from a reactive stream publisher.
   *
   * <p>This method supports reactive streams pattern with backpressure control, allowing the caller
   * to control the flow of data to the compiler.
   *
   * @param publisher the publisher of WebAssembly bytecode chunks
   * @param config streaming compilation configuration
   * @return a CompletableFuture that completes when compilation is finished
   * @throws IllegalArgumentException if publisher or config is null
   */
  @Override
  public CompletableFuture<Module> compileStreaming(
      final Flow.Publisher<ByteBuffer> publisher, final StreamingConfig config) {
    JniValidation.requireNonNull(publisher, "publisher");
    JniValidation.requireNonNull(config, "config");
    ensureNotClosed();

    final CompletableFuture<Module> future = new CompletableFuture<>();

    publisher.subscribe(
        new Flow.Subscriber<ByteBuffer>() {
          private Flow.Subscription subscription;

          @Override
          public void onSubscribe(final Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
          }

          @Override
          public void onNext(final ByteBuffer buffer) {
            try {
              final byte[] data = new byte[buffer.remaining()];
              buffer.get(data);
              nativeFeedChunk(getNativeHandle(), data, data.length);
              subscription.request(1);
            } catch (final Exception e) {
              future.completeExceptionally(e);
            }
          }

          @Override
          public void onError(final Throwable throwable) {
            future.completeExceptionally(throwable);
          }

          @Override
          public void onComplete() {
            try {
              final long moduleHandle = nativeComplete(getNativeHandle());
              future.complete(new JniModule(moduleHandle));
            } catch (final Exception e) {
              future.completeExceptionally(e);
            }
          }
        });

    return future;
  }

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
  @Override
  public StreamingFeedHandle startStreamingCompilation(final StreamingConfig config) {
    JniValidation.requireNonNull(config, "config");
    ensureNotClosed();

    return new JniStreamingFeedHandle(this);
  }

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
  @Override
  public CompletableFuture<StreamingInstantiator> compileStreamingWithInstantiation(
      final InputStream input,
      final StreamingConfig config,
      final InstantiationConfig instantiationConfig) {
    JniValidation.requireNonNull(input, "input");
    JniValidation.requireNonNull(config, "config");
    JniValidation.requireNonNull(instantiationConfig, "instantiationConfig");
    ensureNotClosed();

    return compileStreaming(input, config)
        .thenApply(module -> new JniStreamingInstantiator(module, instantiationConfig));
  }

  /**
   * Gets the engine associated with this streaming compiler.
   *
   * @return the Engine used for compilation
   */
  @Override
  public Engine getEngine() {
    return engine;
  }

  /**
   * Gets the current compilation statistics.
   *
   * <p>This method provides real-time information about the compilation progress, including bytes
   * processed, compilation phases completed, and performance metrics.
   *
   * @return current streaming compilation statistics
   */
  @Override
  public StreamingStatistics getStatistics() {
    ensureNotClosed();

    try {
      final long[] stats = nativeGetStatistics(getNativeHandle());
      return JniStreamingStatistics.create(
          stats[0], // bytesProcessed
          stats[1], // bytesReceived
          Double.longBitsToDouble(stats[2]), // progress
          stats[3], // memoryUsage
          stats[4], // peakMemoryUsage
          stats[5], // functionsCompiled
          stats[6] // totalFunctions
          );
    } catch (final Exception e) {
      LOGGER.warning("Failed to get compilation statistics: " + e.getMessage());
      return JniStreamingStatistics.create(0, 0, 0.0, 0, 0, 0, 0);
    }
  }

  /**
   * Cancels any ongoing streaming compilation operations.
   *
   * <p>This method attempts to gracefully cancel ongoing compilations. Already completed
   * compilations are not affected.
   *
   * @param mayInterruptIfRunning whether to interrupt threads currently executing compilation
   * @return true if cancellation was successful
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (isClosed()) {
      return false;
    }

    try {
      return nativeCancel(getNativeHandle(), mayInterruptIfRunning ? 1 : 0);
    } catch (final Exception e) {
      LOGGER.warning("Failed to cancel streaming compilation: " + e.getMessage());
      return false;
    }
  }

  /**
   * Registers a progress listener for streaming compilation events.
   *
   * <p>The listener will receive notifications about compilation progress, phase transitions, and
   * performance milestones.
   *
   * @param listener the progress event listener
   * @throws IllegalArgumentException if listener is null
   */
  @Override
  public void addProgressListener(final StreamingProgressListener listener) {
    JniValidation.requireNonNull(listener, "listener");
    progressListeners.add(listener);
  }

  /**
   * Removes a previously registered progress listener.
   *
   * @param listener the progress event listener to remove
   * @throws IllegalArgumentException if listener is null
   */
  @Override
  public void removeProgressListener(final StreamingProgressListener listener) {
    JniValidation.requireNonNull(listener, "listener");
    progressListeners.remove(listener);
  }

  @Override
  protected void doClose() throws Exception {
    // Shutdown executor service
    executorService.shutdown();

    if (getNativeHandle() != 0) {
      nativeDestroyStreamingCompiler(getNativeHandle());
      LOGGER.fine(
          "Destroyed JNI streaming compiler with handle: 0x" + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "StreamingCompiler";
  }

  // Native method declarations

  /** Feed data chunk to native streaming compiler. */
  void feedChunk(final byte[] data) {
    ensureNotClosed();
    nativeFeedChunk(getNativeHandle(), data, data.length);
  }

  /** Complete streaming compilation and get module. */
  Module complete() {
    ensureNotClosed();
    final long moduleHandle = nativeComplete(getNativeHandle());
    return new JniModule(moduleHandle);
  }

  /** Get current progress. */
  double getProgress() {
    ensureNotClosed();
    return nativeGetProgress(getNativeHandle());
  }

  /** Check if compilation is done. */
  boolean isDone() {
    if (isClosed()) {
      return true;
    }
    return nativeIsDone(getNativeHandle());
  }

  // Native method declarations

  /**
   * Creates a new native streaming compiler.
   *
   * @param engineHandle the native engine handle
   * @return native streaming compiler handle or 0 on failure
   */
  private static native long createNativeStreamingCompiler(long engineHandle);

  /**
   * Feeds a chunk of data to the streaming compiler.
   *
   * @param compilerHandle the native streaming compiler handle
   * @param data the data chunk
   * @param dataLen length of the data
   */
  private static native void nativeFeedChunk(long compilerHandle, byte[] data, int dataLen);

  /**
   * Completes streaming compilation and returns the module handle.
   *
   * @param compilerHandle the native streaming compiler handle
   * @return the compiled module handle
   */
  private static native long nativeComplete(long compilerHandle);

  /**
   * Gets the current compilation progress.
   *
   * @param compilerHandle the native streaming compiler handle
   * @return the compilation progress (0.0 to 1.0)
   */
  private static native double nativeGetProgress(long compilerHandle);

  /**
   * Gets compilation statistics.
   *
   * @param compilerHandle the native streaming compiler handle
   * @return array of statistics [bytesProcessed, bytesReceived, progress, memoryUsage,
   *     peakMemoryUsage, functionsCompiled, totalFunctions]
   */
  private static native long[] nativeGetStatistics(long compilerHandle);

  /**
   * Cancels the streaming compilation.
   *
   * @param compilerHandle the native streaming compiler handle
   * @param mayInterrupt whether to interrupt if running
   * @return true if cancelled, false otherwise
   */
  private static native boolean nativeCancel(long compilerHandle, int mayInterrupt);

  /**
   * Checks if compilation is done.
   *
   * @param compilerHandle the native streaming compiler handle
   * @return true if done, false otherwise
   */
  private static native boolean nativeIsDone(long compilerHandle);

  /**
   * Destroys a native streaming compiler and releases all associated resources.
   *
   * @param compilerHandle the native streaming compiler handle
   */
  private static native void nativeDestroyStreamingCompiler(long compilerHandle);
}
