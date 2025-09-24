package ai.tegmentum.wasmtime4j;

/**
 * Listener for streaming compilation progress events.
 *
 * <p>StreamingProgressListener provides callbacks for various events during streaming WebAssembly
 * compilation, allowing applications to monitor progress, handle errors, and respond to state
 * changes.
 *
 * @since 1.0.0
 */
public interface StreamingProgressListener {

  /**
   * Called when streaming compilation starts.
   *
   * @param config the streaming configuration being used
   */
  default void onCompilationStarted(StreamingConfig config) {
    // Default implementation does nothing
  }

  /**
   * Called when the compilation phase changes.
   *
   * @param previousPhase the previous compilation phase (can be null for initial phase)
   * @param currentPhase the current compilation phase
   * @param statistics current compilation statistics
   */
  default void onPhaseChanged(
      CompilationPhase previousPhase, CompilationPhase currentPhase, StreamingStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called periodically during compilation to report progress.
   *
   * @param statistics current compilation statistics
   */
  default void onProgressUpdate(StreamingStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when data is received from the input stream.
   *
   * @param bytesReceived number of new bytes received
   * @param totalBytesReceived total bytes received so far
   */
  default void onDataReceived(long bytesReceived, long totalBytesReceived) {
    // Default implementation does nothing
  }

  /**
   * Called when a compilation phase completes successfully.
   *
   * @param phase the completed phase
   * @param phaseStats statistics for the completed phase
   */
  default void onPhaseCompleted(CompilationPhase phase, CompilationPhaseStats phaseStats) {
    // Default implementation does nothing
  }

  /**
   * Called when a compilation phase fails.
   *
   * @param phase the failed phase
   * @param error the error that caused the failure
   * @param phaseStats statistics for the failed phase
   */
  default void onPhaseError(CompilationPhase phase, Throwable error, CompilationPhaseStats phaseStats) {
    // Default implementation does nothing
  }

  /**
   * Called when function compilation begins.
   *
   * @param functionIndex the index of the function being compiled
   * @param functionName the name of the function (if available)
   * @param totalFunctions total number of functions in the module
   */
  default void onFunctionCompilationStarted(int functionIndex, String functionName, int totalFunctions) {
    // Default implementation does nothing
  }

  /**
   * Called when function compilation completes.
   *
   * @param functionIndex the index of the compiled function
   * @param functionName the name of the function (if available)
   * @param compiledFunctions total number of functions compiled so far
   * @param totalFunctions total number of functions in the module
   */
  default void onFunctionCompilationCompleted(
      int functionIndex, String functionName, int compiledFunctions, int totalFunctions) {
    // Default implementation does nothing
  }

  /**
   * Called when caching events occur (if caching is enabled).
   *
   * @param event the type of cache event
   * @param key the cache key involved
   * @param statistics current compilation statistics including cache metrics
   */
  default void onCacheEvent(CacheEvent event, String key, StreamingStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when memory usage crosses significant thresholds.
   *
   * @param currentUsage current memory usage in bytes
   * @param maxUsage maximum allowed memory usage in bytes
   * @param threshold the threshold that was crossed (0.0 to 1.0)
   */
  default void onMemoryThreshold(long currentUsage, long maxUsage, double threshold) {
    // Default implementation does nothing
  }

  /**
   * Called when compilation completes successfully.
   *
   * @param module the compiled module
   * @param statistics final compilation statistics
   */
  default void onCompilationCompleted(Module module, StreamingStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when compilation fails.
   *
   * @param error the error that caused compilation to fail
   * @param statistics compilation statistics at the time of failure
   */
  default void onCompilationFailed(Throwable error, StreamingStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when compilation is cancelled.
   *
   * @param statistics compilation statistics at the time of cancellation
   */
  default void onCompilationCancelled(StreamingStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Cache event types for streaming compilation.
   */
  enum CacheEvent {
    /** Cache hit - requested data was found in cache. */
    HIT,
    /** Cache miss - requested data was not found in cache. */
    MISS,
    /** Cache store - data was stored in cache. */
    STORE,
    /** Cache eviction - data was removed from cache. */
    EVICTION,
    /** Cache error - cache operation failed. */
    ERROR
  }
}