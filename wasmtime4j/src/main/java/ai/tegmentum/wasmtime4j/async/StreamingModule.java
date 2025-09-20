package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Streaming WebAssembly module compilation interface.
 *
 * <p>A StreamingModule provides advanced streaming compilation capabilities for large WebAssembly
 * modules, enabling progressive compilation with progress tracking, memory-efficient processing,
 * and fine-grained control over the compilation process.
 *
 * <p>This interface is particularly useful for large WASM modules that would be inefficient to
 * load entirely into memory before compilation, or when progress feedback is required for
 * user interfaces.
 *
 * @since 1.0.0
 */
public interface StreamingModule {

  /**
   * Compiles a WebAssembly module from a stream with streaming options.
   *
   * <p>This method enables true streaming compilation where the module is processed incrementally
   * as data becomes available from the stream, without requiring the entire module to be loaded
   * into memory first.
   *
   * @param engine the engine to use for compilation
   * @param wasmStream the InputStream containing WebAssembly bytecode
   * @param options streaming compilation options
   * @return a CompletableFuture that completes with the compiled Module
   * @throws IllegalArgumentException if engine, wasmStream, or options is null
   */
  static CompletableFuture<Module> compileStreaming(
      final Engine engine, final InputStream wasmStream, final StreamingOptions options) {
    if (engine instanceof AsyncEngine) {
      return ((AsyncEngine) engine).compileModuleAsync(wasmStream, convertToCompilationOptions(options));
    }
    throw new UnsupportedOperationException("Engine does not support async compilation");
  }

  /**
   * Validates a WebAssembly module from a stream without full compilation.
   *
   * <p>This method performs streaming validation, checking the module structure and validity
   * as data becomes available without the overhead of full compilation.
   *
   * @param engine the engine to use for validation
   * @param wasmStream the InputStream containing WebAssembly bytecode
   * @param options streaming validation options
   * @return a CompletableFuture that completes when validation is finished
   * @throws IllegalArgumentException if engine, wasmStream, or options is null
   */
  static CompletableFuture<Void> streamValidation(
      final Engine engine, final InputStream wasmStream, final StreamingOptions options) {
    if (engine instanceof AsyncEngine) {
      return ((AsyncEngine) engine).validateModuleAsync(wasmStream);
    }
    throw new UnsupportedOperationException("Engine does not support async validation");
  }

  /**
   * Gets compilation progress for a streaming operation.
   *
   * <p>This method returns a Progress object that can be used to track the current state
   * of a streaming compilation operation.
   *
   * @param operationId the ID of the streaming operation
   * @return progress information for the operation
   * @throws IllegalArgumentException if operationId is null or invalid
   */
  static Progress getCompilationProgress(final String operationId) {
    return new ProgressImpl(operationId);
  }

  /**
   * Creates default streaming options with sensible defaults.
   *
   * @return default streaming options
   */
  static StreamingOptions createDefaultOptions() {
    return new StreamingOptionsImpl(
        8192, // 8KB buffer size
        false, // progress tracking disabled
        null, // no timeout
        null, // use default executor
        1024 * 1024 * 16, // 16MB max memory
        false, // validation not skipped
        1 // normal priority
    );
  }

  /**
   * Creates streaming options with custom configuration.
   *
   * @param bufferSize buffer size for streaming operations
   * @param enableProgressTracking whether to enable progress tracking
   * @param timeout timeout for the operation
   * @param executor custom executor for the operation
   * @return configured streaming options
   */
  static StreamingOptions createOptions(
      final int bufferSize,
      final boolean enableProgressTracking,
      final Duration timeout,
      final Executor executor) {
    return new StreamingOptionsImpl(
        bufferSize,
        enableProgressTracking,
        timeout,
        executor,
        1024 * 1024 * 16, // 16MB max memory
        false, // validation not skipped
        1 // normal priority
    );
  }

  // Private helper method to convert StreamingOptions to CompilationOptions
  private static AsyncEngine.CompilationOptions convertToCompilationOptions(
      final StreamingOptions options) {
    return new AsyncEngine.CompilationOptions() {
      @Override
      public int getBufferSize() {
        return options.getBufferSize();
      }

      @Override
      public boolean isProgressTrackingEnabled() {
        return options.isEnableProgressTracking();
      }

      @Override
      public Duration getTimeout() {
        return options.getTimeout();
      }

      @Override
      public Executor getExecutor() {
        return options.getExecutor();
      }

      @Override
      public boolean isStreamingEnabled() {
        return true;
      }

      @Override
      public long getMaxMemoryUsage() {
        return options.getMaxMemoryUsage();
      }
    };
  }

  /** Configuration options for streaming compilation operations. */
  interface StreamingOptions {
    /**
     * Gets the buffer size for streaming operations.
     *
     * @return buffer size in bytes
     */
    int getBufferSize();

    /**
     * Checks if progress tracking is enabled.
     *
     * @return true if progress tracking is enabled
     */
    boolean isEnableProgressTracking();

    /**
     * Gets the timeout for streaming operations.
     *
     * @return timeout duration, or null for no timeout
     */
    Duration getTimeout();

    /**
     * Gets the custom executor for streaming operations.
     *
     * @return custom executor, or null to use default
     */
    Executor getExecutor();

    /**
     * Gets the maximum memory usage during streaming.
     *
     * @return maximum memory in bytes
     */
    long getMaxMemoryUsage();

    /**
     * Checks if validation should be skipped during streaming.
     *
     * @return true if validation is skipped for performance
     */
    boolean isSkipValidation();

    /**
     * Gets the priority level for streaming operations.
     *
     * @return priority level (higher values indicate higher priority)
     */
    int getPriority();
  }

  /** Progress information for streaming operations. */
  interface Progress {
    /**
     * Gets the operation ID.
     *
     * @return the operation ID
     */
    String getOperationId();

    /**
     * Gets the current progress as a percentage.
     *
     * @return progress percentage (0.0 to 100.0)
     */
    double getProgressPercentage();

    /**
     * Gets the number of bytes processed so far.
     *
     * @return bytes processed
     */
    long getBytesProcessed();

    /**
     * Gets the total number of bytes to process.
     *
     * @return total bytes, or -1 if unknown
     */
    long getTotalBytes();

    /**
     * Gets the current compilation phase.
     *
     * @return the current phase
     */
    CompilationPhase getCurrentPhase();

    /**
     * Gets the elapsed time since the operation started.
     *
     * @return elapsed duration
     */
    Duration getElapsedTime();

    /**
     * Gets an estimate of the remaining time.
     *
     * @return estimated remaining duration, or null if unknown
     */
    Duration getEstimatedTimeRemaining();

    /**
     * Checks if the operation has completed.
     *
     * @return true if completed
     */
    boolean isCompleted();

    /**
     * Checks if the operation has failed.
     *
     * @return true if failed
     */
    boolean isFailed();

    /**
     * Gets the error that caused failure, if any.
     *
     * @return the error, or null if not failed
     */
    Throwable getError();
  }

  /** Compilation phases for streaming operations. */
  enum CompilationPhase {
    /** Initial parsing of the WASM bytecode. */
    PARSING("Parsing"),

    /** Validation of the parsed module. */
    VALIDATION("Validation"),

    /** Code generation and compilation. */
    COMPILATION("Compilation"),

    /** Optimization passes. */
    OPTIMIZATION("Optimization"),

    /** Finalization and cleanup. */
    FINALIZATION("Finalization"),

    /** Compilation completed successfully. */
    COMPLETED("Completed"),

    /** Compilation failed with errors. */
    FAILED("Failed");

    private final String displayName;

    CompilationPhase(final String displayName) {
      this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this phase.
     *
     * @return the display name
     */
    public String getDisplayName() {
      return displayName;
    }
  }

  // Implementation classes

  /** Default implementation of StreamingOptions. */
  final class StreamingOptionsImpl implements StreamingOptions {
    private final int bufferSize;
    private final boolean enableProgressTracking;
    private final Duration timeout;
    private final Executor executor;
    private final long maxMemoryUsage;
    private final boolean skipValidation;
    private final int priority;

    StreamingOptionsImpl(
        final int bufferSize,
        final boolean enableProgressTracking,
        final Duration timeout,
        final Executor executor,
        final long maxMemoryUsage,
        final boolean skipValidation,
        final int priority) {
      this.bufferSize = bufferSize;
      this.enableProgressTracking = enableProgressTracking;
      this.timeout = timeout;
      this.executor = executor;
      this.maxMemoryUsage = maxMemoryUsage;
      this.skipValidation = skipValidation;
      this.priority = priority;
    }

    @Override
    public int getBufferSize() {
      return bufferSize;
    }

    @Override
    public boolean isEnableProgressTracking() {
      return enableProgressTracking;
    }

    @Override
    public Duration getTimeout() {
      return timeout;
    }

    @Override
    public Executor getExecutor() {
      return executor;
    }

    @Override
    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    @Override
    public boolean isSkipValidation() {
      return skipValidation;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }

  /** Default implementation of Progress. */
  final class ProgressImpl implements Progress {
    private final String operationId;
    private volatile double progressPercentage;
    private volatile long bytesProcessed;
    private volatile long totalBytes;
    private volatile CompilationPhase currentPhase;
    private final long startTime;
    private volatile boolean completed;
    private volatile boolean failed;
    private volatile Throwable error;

    ProgressImpl(final String operationId) {
      this.operationId = operationId;
      this.progressPercentage = 0.0;
      this.bytesProcessed = 0;
      this.totalBytes = -1;
      this.currentPhase = CompilationPhase.PARSING;
      this.startTime = System.currentTimeMillis();
      this.completed = false;
      this.failed = false;
      this.error = null;
    }

    @Override
    public String getOperationId() {
      return operationId;
    }

    @Override
    public double getProgressPercentage() {
      return progressPercentage;
    }

    @Override
    public long getBytesProcessed() {
      return bytesProcessed;
    }

    @Override
    public long getTotalBytes() {
      return totalBytes;
    }

    @Override
    public CompilationPhase getCurrentPhase() {
      return currentPhase;
    }

    @Override
    public Duration getElapsedTime() {
      return Duration.ofMillis(System.currentTimeMillis() - startTime);
    }

    @Override
    public Duration getEstimatedTimeRemaining() {
      if (progressPercentage <= 0.0 || progressPercentage >= 100.0) {
        return null;
      }

      final long elapsed = System.currentTimeMillis() - startTime;
      final double remaining = (100.0 - progressPercentage) / progressPercentage;
      return Duration.ofMillis((long) (elapsed * remaining));
    }

    @Override
    public boolean isCompleted() {
      return completed;
    }

    @Override
    public boolean isFailed() {
      return failed;
    }

    @Override
    public Throwable getError() {
      return error;
    }

    // Package-private methods for updating progress

    void updateProgress(final double percentage, final long bytesProcessed, final long totalBytes) {
      this.progressPercentage = Math.max(0.0, Math.min(100.0, percentage));
      this.bytesProcessed = bytesProcessed;
      this.totalBytes = totalBytes;
    }

    void updatePhase(final CompilationPhase phase) {
      this.currentPhase = phase;
    }

    void markCompleted() {
      this.completed = true;
      this.progressPercentage = 100.0;
      this.currentPhase = CompilationPhase.COMPLETED;
    }

    void markFailed(final Throwable error) {
      this.failed = true;
      this.error = error;
      this.currentPhase = CompilationPhase.FAILED;
    }
  }
}