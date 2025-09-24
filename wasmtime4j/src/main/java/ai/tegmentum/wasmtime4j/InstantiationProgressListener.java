package ai.tegmentum.wasmtime4j;

/**
 * Listener for streaming instantiation progress events.
 *
 * <p>InstantiationProgressListener provides callbacks for various events during streaming
 * WebAssembly instantiation, allowing applications to monitor progress, handle errors, and respond
 * to state changes.
 *
 * @since 1.0.0
 */
public interface InstantiationProgressListener {

  /**
   * Called when streaming instantiation starts.
   *
   * @param config the instantiation configuration being used
   */
  default void onInstantiationStarted(InstantiationConfig config) {
    // Default implementation does nothing
  }

  /**
   * Called when the instantiation phase changes.
   *
   * @param previousPhase the previous instantiation phase (can be null for initial phase)
   * @param currentPhase the current instantiation phase
   * @param statistics current instantiation statistics
   */
  default void onPhaseChanged(
      InstantiationPhase previousPhase,
      InstantiationPhase currentPhase,
      InstantiationStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called periodically during instantiation to report progress.
   *
   * @param statistics current instantiation statistics
   */
  default void onProgressUpdate(InstantiationStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when an instantiation phase completes successfully.
   *
   * @param phase the completed phase
   * @param statistics current instantiation statistics
   */
  default void onPhaseCompleted(InstantiationPhase phase, InstantiationStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when an instantiation phase fails.
   *
   * @param phase the failed phase
   * @param error the error that caused the failure
   * @param statistics instantiation statistics at the time of failure
   */
  default void onPhaseError(
      InstantiationPhase phase, Throwable error, InstantiationStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when function compilation begins.
   *
   * @param functionIndex the index of the function being compiled
   * @param functionName the name of the function (if available)
   * @param priority the compilation priority for this function
   * @param totalFunctions total number of functions in the module
   */
  default void onFunctionCompilationStarted(
      int functionIndex, String functionName, CompilationPriority priority, int totalFunctions) {
    // Default implementation does nothing
  }

  /**
   * Called when function compilation completes.
   *
   * @param functionIndex the index of the compiled function
   * @param functionName the name of the function (if available)
   * @param compilationStats compilation statistics for this function
   * @param compiledFunctions total number of functions compiled so far
   * @param totalFunctions total number of functions in the module
   */
  default void onFunctionCompilationCompleted(
      int functionIndex,
      String functionName,
      FunctionCompilationStats compilationStats,
      int compiledFunctions,
      int totalFunctions) {
    // Default implementation does nothing
  }

  /**
   * Called when memory allocation begins.
   *
   * @param initialPages initial number of memory pages to allocate
   * @param maximumPages maximum number of memory pages (0 for unlimited)
   */
  default void onMemoryAllocationStarted(long initialPages, long maximumPages) {
    // Default implementation does nothing
  }

  /**
   * Called when memory allocation completes.
   *
   * @param memory the allocated memory instance
   * @param pagesAllocated actual number of pages allocated
   */
  default void onMemoryAllocationCompleted(Memory memory, long pagesAllocated) {
    // Default implementation does nothing
  }

  /**
   * Called when table allocation begins.
   *
   * @param tableIndex the index of the table being allocated
   * @param tableType the type of the table
   */
  default void onTableAllocationStarted(int tableIndex, TableType tableType) {
    // Default implementation does nothing
  }

  /**
   * Called when table allocation completes.
   *
   * @param tableIndex the index of the allocated table
   * @param table the allocated table instance
   */
  default void onTableAllocationCompleted(int tableIndex, Table table) {
    // Default implementation does nothing
  }

  /**
   * Called when global allocation begins.
   *
   * @param globalIndex the index of the global being allocated
   * @param globalType the type of the global
   */
  default void onGlobalAllocationStarted(int globalIndex, GlobalType globalType) {
    // Default implementation does nothing
  }

  /**
   * Called when global allocation completes.
   *
   * @param globalIndex the index of the allocated global
   * @param global the allocated global instance
   */
  default void onGlobalAllocationCompleted(int globalIndex, Global global) {
    // Default implementation does nothing
  }

  /**
   * Called when import resolution begins.
   *
   * @param moduleName the module name of the import
   * @param fieldName the field name of the import
   * @param importType the type of the import
   */
  default void onImportResolutionStarted(String moduleName, String fieldName, ImportType importType) {
    // Default implementation does nothing
  }

  /**
   * Called when import resolution completes.
   *
   * @param moduleName the module name of the import
   * @param fieldName the field name of the import
   * @param importType the type of the import
   * @param resolved whether the import was successfully resolved
   */
  default void onImportResolutionCompleted(
      String moduleName, String fieldName, ImportType importType, boolean resolved) {
    // Default implementation does nothing
  }

  /**
   * Called when the start function begins execution.
   */
  default void onStartFunctionExecutionStarted() {
    // Default implementation does nothing
  }

  /**
   * Called when the start function completes execution.
   *
   * @param executionTime time taken to execute the start function
   */
  default void onStartFunctionExecutionCompleted(java.time.Duration executionTime) {
    // Default implementation does nothing
  }

  /**
   * Called when instance pooling events occur (if pooling is enabled).
   *
   * @param event the type of pool event
   * @param statistics current instantiation statistics including pool metrics
   */
  default void onPoolEvent(PoolEvent event, InstantiationStatistics statistics) {
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
   * Called when instantiation completes successfully.
   *
   * @param instance the instantiated instance
   * @param statistics final instantiation statistics
   */
  default void onInstantiationCompleted(Instance instance, InstantiationStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when instantiation fails.
   *
   * @param error the error that caused instantiation to fail
   * @param statistics instantiation statistics at the time of failure
   */
  default void onInstantiationFailed(Throwable error, InstantiationStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Called when instantiation is cancelled.
   *
   * @param statistics instantiation statistics at the time of cancellation
   */
  default void onInstantiationCancelled(InstantiationStatistics statistics) {
    // Default implementation does nothing
  }

  /**
   * Pool event types for streaming instantiation.
   */
  enum PoolEvent {
    /** Pool hit - requested resource was found in pool. */
    HIT,
    /** Pool miss - requested resource was not found in pool. */
    MISS,
    /** Pool allocation - new resource was allocated for pool. */
    ALLOCATION,
    /** Pool eviction - resource was removed from pool. */
    EVICTION,
    /** Pool error - pool operation failed. */
    ERROR
  }
}