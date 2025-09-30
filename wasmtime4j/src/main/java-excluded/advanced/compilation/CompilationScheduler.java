package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduler for WebAssembly compilation tasks.
 *
 * <p>Manages prioritized compilation work, load balancing, and resource
 * allocation for JIT compilation tasks across multiple tiers.
 *
 * @since 1.0.0
 */
public final class CompilationScheduler {

  private final CompilationSchedulerConfig config;
  private final PriorityBlockingQueue<CompilationTask> taskQueue;
  private final AtomicLong taskIdGenerator;
  private final Executor compilationExecutor;

  /**
   * Creates a new compilation scheduler.
   *
   * @param config the scheduler configuration
   * @param compilationExecutor the executor for compilation tasks
   */
  public CompilationScheduler(final CompilationSchedulerConfig config,
                              final Executor compilationExecutor) {
    this.config = Objects.requireNonNull(config);
    this.compilationExecutor = Objects.requireNonNull(compilationExecutor);
    this.taskQueue = new PriorityBlockingQueue<>();
    this.taskIdGenerator = new AtomicLong(0);
  }

  /**
   * Schedules a compilation task.
   *
   * @param functionName the function to compile
   * @param tier the compilation tier
   * @param priority the task priority
   * @return future representing the compilation result
   */
  public CompletableFuture<CompilationResult> scheduleCompilation(final String functionName,
                                                                  final CompilationTier tier,
                                                                  final CompilationPriority priority) {
    final long taskId = taskIdGenerator.incrementAndGet();
    final CompilationTask task = new CompilationTask(taskId, functionName, tier, priority);

    taskQueue.offer(task);

    return CompletableFuture.supplyAsync(() -> executeTask(task), compilationExecutor);
  }

  /**
   * Schedules a compilation task with deadline.
   *
   * @param functionName the function to compile
   * @param tier the compilation tier
   * @param priority the task priority
   * @param deadline the compilation deadline
   * @return future representing the compilation result
   */
  public CompletableFuture<CompilationResult> scheduleCompilation(final String functionName,
                                                                  final CompilationTier tier,
                                                                  final CompilationPriority priority,
                                                                  final Instant deadline) {
    final long taskId = taskIdGenerator.incrementAndGet();
    final CompilationTask task = new CompilationTask(taskId, functionName, tier, priority, deadline);

    taskQueue.offer(task);

    return CompletableFuture.supplyAsync(() -> executeTask(task), compilationExecutor);
  }

  /**
   * Gets the number of pending compilation tasks.
   *
   * @return pending task count
   */
  public int getPendingTaskCount() {
    return taskQueue.size();
  }

  /**
   * Clears all pending compilation tasks.
   */
  public void clearPendingTasks() {
    taskQueue.clear();
  }

  /**
   * Checks if the scheduler is busy.
   *
   * @return true if busy, false otherwise
   */
  public boolean isBusy() {
    return !taskQueue.isEmpty();
  }

  private CompilationResult executeTask(final CompilationTask task) {
    try {
      // Simulate compilation work - in real implementation this would
      // interface with the actual compilation backend
      final long startTime = System.currentTimeMillis();

      // Check if task has expired
      if (task.getDeadline().isPresent() && Instant.now().isAfter(task.getDeadline().get())) {
        return CompilationResult.failure("Task expired before execution",
            System.currentTimeMillis() - startTime);
      }

      // Simulate compilation time based on tier
      final long compilationTime = calculateCompilationTime(task.getTier());
      Thread.sleep(compilationTime);

      final long totalTime = System.currentTimeMillis() - startTime;
      return CompilationResult.success(task.getFunctionName(), task.getTier(), totalTime);

    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return CompilationResult.failure("Compilation interrupted", 0);
    } catch (final Exception e) {
      return CompilationResult.failure("Compilation failed: " + e.getMessage(), 0);
    }
  }

  private long calculateCompilationTime(final CompilationTier tier) {
    return switch (tier) {
      case BASELINE -> 10;
      case OPTIMIZED -> 100;
      case HIGHLY_OPTIMIZED -> 500;
    };
  }

  /**
   * Compilation task representation.
   */
  private static final class CompilationTask implements Comparable<CompilationTask> {
    private final long taskId;
    private final String functionName;
    private final CompilationTier tier;
    private final CompilationPriority priority;
    private final Optional<Instant> deadline;
    private final Instant createdAt;

    public CompilationTask(final long taskId,
                           final String functionName,
                           final CompilationTier tier,
                           final CompilationPriority priority) {
      this(taskId, functionName, tier, priority, null);
    }

    public CompilationTask(final long taskId,
                           final String functionName,
                           final CompilationTier tier,
                           final CompilationPriority priority,
                           final Instant deadline) {
      this.taskId = taskId;
      this.functionName = Objects.requireNonNull(functionName);
      this.tier = Objects.requireNonNull(tier);
      this.priority = Objects.requireNonNull(priority);
      this.deadline = Optional.ofNullable(deadline);
      this.createdAt = Instant.now();
    }

    public long getTaskId() { return taskId; }
    public String getFunctionName() { return functionName; }
    public CompilationTier getTier() { return tier; }
    public CompilationPriority getPriority() { return priority; }
    public Optional<Instant> getDeadline() { return deadline; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public int compareTo(final CompilationTask other) {
      // Higher priority first, then earlier deadline, then creation order
      int result = Integer.compare(other.priority.getValue(), this.priority.getValue());
      if (result == 0 && deadline.isPresent() && other.deadline.isPresent()) {
        result = deadline.get().compareTo(other.deadline.get());
      }
      if (result == 0) {
        result = Long.compare(this.taskId, other.taskId);
      }
      return result;
    }
  }

  /**
   * Configuration for the compilation scheduler.
   */
  public static final class CompilationSchedulerConfig {
    private final int maxConcurrentTasks;
    private final Duration defaultTimeout;

    public CompilationSchedulerConfig(final int maxConcurrentTasks,
                                      final Duration defaultTimeout) {
      this.maxConcurrentTasks = maxConcurrentTasks;
      this.defaultTimeout = Objects.requireNonNull(defaultTimeout);
    }

    public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public Duration getDefaultTimeout() { return defaultTimeout; }
  }

  /**
   * Result of a compilation task.
   */
  public static final class CompilationResult {
    private final boolean successful;
    private final String functionName;
    private final CompilationTier tier;
    private final String errorMessage;
    private final long compilationTimeMs;

    private CompilationResult(final boolean successful,
                              final String functionName,
                              final CompilationTier tier,
                              final String errorMessage,
                              final long compilationTimeMs) {
      this.successful = successful;
      this.functionName = functionName;
      this.tier = tier;
      this.errorMessage = errorMessage;
      this.compilationTimeMs = compilationTimeMs;
    }

    public static CompilationResult success(final String functionName,
                                            final CompilationTier tier,
                                            final long compilationTimeMs) {
      return new CompilationResult(true, functionName, tier, null, compilationTimeMs);
    }

    public static CompilationResult failure(final String errorMessage,
                                            final long compilationTimeMs) {
      return new CompilationResult(false, null, null, errorMessage, compilationTimeMs);
    }

    public boolean isSuccessful() { return successful; }
    public String getFunctionName() { return functionName; }
    public CompilationTier getTier() { return tier; }
    public String getErrorMessage() { return errorMessage; }
    public long getCompilationTimeMs() { return compilationTimeMs; }
  }
}