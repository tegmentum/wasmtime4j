package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Progress information for asynchronous WebAssembly instance instantiation.
 *
 * <p>This class provides real-time updates about the instantiation process,
 * including current phase, completion percentage, and resource allocation details.
 *
 * @since 1.0.0
 */
public final class AsyncInstantiationProgress {

  /** Standard instantiation phase names. */
  public static final String PHASE_INITIALIZATION = "initialization";
  public static final String PHASE_MEMORY_ALLOCATION = "memory_allocation";
  public static final String PHASE_TABLE_SETUP = "table_setup";
  public static final String PHASE_GLOBAL_INITIALIZATION = "global_initialization";
  public static final String PHASE_FUNCTION_LINKING = "function_linking";
  public static final String PHASE_HOST_BINDING = "host_binding";
  public static final String PHASE_STARTUP_EXECUTION = "startup_execution";
  public static final String PHASE_FINALIZATION = "finalization";

  private final String instanceId;
  private final String currentPhase;
  private final double completionPercentage;
  private final Duration elapsedTime;
  private final Optional<Duration> estimatedTimeRemaining;
  private final List<InstantiationPhase> completedPhases;
  private final long memoryAllocated;
  private final long totalMemoryRequired;
  private final int functionsLinked;
  private final int totalFunctions;
  private final String statusMessage;
  private final boolean cancellable;
  private final InstantiationResourceUsage resourceUsage;

  /**
   * Creates new instantiation progress information.
   *
   * @param instanceId unique identifier for the instance being created
   * @param currentPhase name of the current instantiation phase
   * @param completionPercentage completion percentage (0.0 - 1.0)
   * @param elapsedTime time elapsed since instantiation started
   * @param estimatedTimeRemaining optional estimated time remaining
   * @param completedPhases list of completed instantiation phases
   * @param memoryAllocated amount of memory allocated so far
   * @param totalMemoryRequired total memory required
   * @param functionsLinked number of functions linked so far
   * @param totalFunctions total number of functions to link
   * @param statusMessage current status message
   * @param cancellable whether instantiation can be cancelled
   * @param resourceUsage current resource usage information
   */
  public AsyncInstantiationProgress(
      final String instanceId,
      final String currentPhase,
      final double completionPercentage,
      final Duration elapsedTime,
      final Optional<Duration> estimatedTimeRemaining,
      final List<InstantiationPhase> completedPhases,
      final long memoryAllocated,
      final long totalMemoryRequired,
      final int functionsLinked,
      final int totalFunctions,
      final String statusMessage,
      final boolean cancellable,
      final InstantiationResourceUsage resourceUsage) {
    this.instanceId = instanceId;
    this.currentPhase = currentPhase;
    this.completionPercentage = Math.max(0.0, Math.min(1.0, completionPercentage));
    this.elapsedTime = elapsedTime;
    this.estimatedTimeRemaining = estimatedTimeRemaining;
    this.completedPhases = List.copyOf(completedPhases);
    this.memoryAllocated = memoryAllocated;
    this.totalMemoryRequired = totalMemoryRequired;
    this.functionsLinked = functionsLinked;
    this.totalFunctions = totalFunctions;
    this.statusMessage = statusMessage;
    this.cancellable = cancellable;
    this.resourceUsage = resourceUsage;
  }

  /**
   * Creates initial instantiation progress state.
   *
   * @param instanceId unique instance identifier
   * @param totalMemoryRequired total memory requirement
   * @param totalFunctions total number of functions
   * @return initial progress
   */
  public static AsyncInstantiationProgress initial(
      final String instanceId,
      final long totalMemoryRequired,
      final int totalFunctions) {
    return new AsyncInstantiationProgress(
        instanceId,
        PHASE_INITIALIZATION,
        0.0,
        Duration.ZERO,
        Optional.empty(),
        List.of(),
        0L,
        totalMemoryRequired,
        0,
        totalFunctions,
        "Starting instantiation",
        true,
        InstantiationResourceUsage.empty());
  }

  /**
   * Creates completed instantiation progress state.
   *
   * @param instanceId unique instance identifier
   * @param phases all completed phases
   * @param elapsedTime total time taken
   * @param finalResourceUsage final resource usage
   * @return completed progress
   */
  public static AsyncInstantiationProgress completed(
      final String instanceId,
      final List<InstantiationPhase> phases,
      final Duration elapsedTime,
      final InstantiationResourceUsage finalResourceUsage) {
    return new AsyncInstantiationProgress(
        instanceId,
        "completed",
        1.0,
        elapsedTime,
        Optional.of(Duration.ZERO),
        phases,
        finalResourceUsage.getMemoryUsed(),
        finalResourceUsage.getMemoryUsed(),
        finalResourceUsage.getFunctionsLinked(),
        finalResourceUsage.getFunctionsLinked(),
        "Instantiation completed successfully",
        false,
        finalResourceUsage);
  }

  /**
   * Gets the unique instance identifier.
   *
   * @return instance ID
   */
  public String getInstanceId() {
    return instanceId;
  }

  /**
   * Gets the current instantiation phase name.
   *
   * @return current phase name
   */
  public String getCurrentPhase() {
    return currentPhase;
  }

  /**
   * Gets the completion percentage.
   *
   * @return completion percentage (0.0 - 1.0)
   */
  public double getCompletionPercentage() {
    return completionPercentage;
  }

  /**
   * Gets the time elapsed since instantiation started.
   *
   * @return elapsed time
   */
  public Duration getElapsedTime() {
    return elapsedTime;
  }

  /**
   * Gets the estimated time remaining.
   *
   * @return optional estimated time remaining
   */
  public Optional<Duration> getEstimatedTimeRemaining() {
    return estimatedTimeRemaining;
  }

  /**
   * Gets the list of completed instantiation phases.
   *
   * @return immutable list of completed phases
   */
  public List<InstantiationPhase> getCompletedPhases() {
    return completedPhases;
  }

  /**
   * Gets the amount of memory allocated so far.
   *
   * @return memory allocated in bytes
   */
  public long getMemoryAllocated() {
    return memoryAllocated;
  }

  /**
   * Gets the total memory required for instantiation.
   *
   * @return total memory required in bytes
   */
  public long getTotalMemoryRequired() {
    return totalMemoryRequired;
  }

  /**
   * Gets the number of functions linked so far.
   *
   * @return functions linked count
   */
  public int getFunctionsLinked() {
    return functionsLinked;
  }

  /**
   * Gets the total number of functions to link.
   *
   * @return total functions count
   */
  public int getTotalFunctions() {
    return totalFunctions;
  }

  /**
   * Gets the current status message.
   *
   * @return status message
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Checks if instantiation can be cancelled.
   *
   * @return true if cancellation is supported
   */
  public boolean isCancellable() {
    return cancellable;
  }

  /**
   * Gets the current resource usage information.
   *
   * @return resource usage details
   */
  public InstantiationResourceUsage getResourceUsage() {
    return resourceUsage;
  }

  /**
   * Checks if instantiation is completed.
   *
   * @return true if instantiation is finished
   */
  public boolean isCompleted() {
    return completionPercentage >= 1.0;
  }

  /**
   * Calculates the memory allocation progress percentage.
   *
   * @return memory allocation progress (0.0 - 1.0)
   */
  public double getMemoryProgress() {
    return totalMemoryRequired > 0 ? (double) memoryAllocated / totalMemoryRequired : 0.0;
  }

  /**
   * Calculates the function linking progress percentage.
   *
   * @return function linking progress (0.0 - 1.0)
   */
  public double getFunctionProgress() {
    return totalFunctions > 0 ? (double) functionsLinked / totalFunctions : 0.0;
  }

  /**
   * Calculates the current instantiation throughput in operations per second.
   *
   * @return current throughput
   */
  public double getCurrentThroughput() {
    final double seconds = elapsedTime.toNanos() / 1_000_000_000.0;
    final int completedOperations = completedPhases.size();
    return seconds > 0 ? completedOperations / seconds : 0.0;
  }

  /**
   * Creates a copy with updated progress information.
   *
   * @param currentPhase new current phase
   * @param completionPercentage new completion percentage
   * @param memoryAllocated new memory allocated amount
   * @param functionsLinked new functions linked count
   * @param statusMessage new status message
   * @param resourceUsage updated resource usage
   * @return updated progress
   */
  public AsyncInstantiationProgress withUpdate(
      final String currentPhase,
      final double completionPercentage,
      final long memoryAllocated,
      final int functionsLinked,
      final String statusMessage,
      final InstantiationResourceUsage resourceUsage) {
    final Duration newElapsed = elapsedTime.plus(Duration.ofMillis(100)); // Simulate progression
    final Optional<Duration> eta = estimateTimeRemaining(completionPercentage, newElapsed);

    return new AsyncInstantiationProgress(
        instanceId,
        currentPhase,
        completionPercentage,
        newElapsed,
        eta,
        completedPhases,
        memoryAllocated,
        totalMemoryRequired,
        functionsLinked,
        totalFunctions,
        statusMessage,
        cancellable,
        resourceUsage);
  }

  private Optional<Duration> estimateTimeRemaining(
      final double completion, final Duration elapsed) {
    if (completion <= 0.0) {
      return Optional.empty();
    }

    final double totalEstimate = elapsed.toNanos() / completion;
    final long remainingNanos = (long) (totalEstimate - elapsed.toNanos());

    return remainingNanos > 0
        ? Optional.of(Duration.ofNanos(remainingNanos))
        : Optional.of(Duration.ZERO);
  }

  @Override
  public String toString() {
    return String.format(
        "AsyncInstantiationProgress{instance='%s', phase='%s', completion=%.1f%%, " +
        "memory=%s/%s, functions=%d/%d}",
        instanceId, currentPhase, completionPercentage * 100,
        formatBytes(memoryAllocated), formatBytes(totalMemoryRequired),
        functionsLinked, totalFunctions);
  }

  private String formatBytes(final long bytes) {
    if (bytes < 1024) return bytes + "B";
    if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
    return (bytes / (1024 * 1024)) + "MB";
  }
}