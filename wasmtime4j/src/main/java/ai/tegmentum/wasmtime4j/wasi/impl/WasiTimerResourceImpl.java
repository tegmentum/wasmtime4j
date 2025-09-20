package ai.tegmentum.wasmtime4j.wasi.impl;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasiResourceException;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiResourcePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Runnable;
import java.util.logging.Logger;

/**
 * Implementation of WasiResource for timer and time-based resources.
 *
 * <p>This implementation provides WASI Preview 2 time resource management including timer
 * operations, scheduling, and time-based events with proper permission enforcement and
 * resource limits.
 *
 * @since 1.0.0
 */
public final class WasiTimerResourceImpl extends WasiGenericResourceImpl {

  private static final Logger LOGGER = Logger.getLogger(WasiTimerResourceImpl.class.getName());

  private final TimerType timerType;
  private final Set<WasiResourcePermissions> permissions;
  private final Duration resolution;
  private final boolean allowScheduling;

  private final ScheduledExecutorService executor;
  private volatile ScheduledFuture<?> scheduledTask;
  private final AtomicBoolean timerActive = new AtomicBoolean(false);
  private final AtomicLong timerExecutions = new AtomicLong(0);
  private final AtomicLong timeRequests = new AtomicLong(0);
  private volatile Instant lastExecutionTime;
  private volatile Duration lastScheduledDuration;

  /**
   * Creates a new timer resource.
   *
   * @param id the unique resource identifier
   * @param name the resource name
   * @param config the resource configuration
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws WasiResourceException if timer setup fails
   */
  public WasiTimerResourceImpl(final long id, final String name, final WasiResourceConfig config) {
    super(id, name, "time", config);

    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    // Extract timer-specific configuration
    final String typeStr = (String) config.getProperty("timer_type").orElse("MONOTONIC");
    this.timerType = TimerType.fromName(typeStr);

    this.permissions = config.getPermissions().getClass().isAssignableFrom(Set.class)
        ? (Set<WasiResourcePermissions>) config.getPermissions()
        : WasiResourcePermissions.READ_ONLY;

    final Object resolutionObj = config.getProperty("resolution_ms").orElse(1);
    final long resolutionMs = resolutionObj instanceof Integer
        ? (Integer) resolutionObj : Long.parseLong(resolutionObj.toString());
    this.resolution = Duration.ofMillis(resolutionMs);

    this.allowScheduling = (Boolean) config.getProperty("allow_scheduling").orElse(false);

    // Create executor only if scheduling is allowed
    if (allowScheduling) {
      this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
        final Thread t = new Thread(r, "WasiTimer-" + name);
        t.setDaemon(true); // Don't prevent JVM shutdown
        return t;
      });
    } else {
      this.executor = null;
    }

    LOGGER.fine("Created timer resource '" + name + "' with type: " + timerType +
        ", resolution: " + resolution.toMillis() + "ms, scheduling: " + allowScheduling);
  }

  /**
   * Gets the current time.
   *
   * @return the current time instant
   * @throws WasmException if time access fails
   */
  public Instant getCurrentTime() throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.READ);
    recordAccess();

    timeRequests.incrementAndGet();

    switch (timerType) {
      case REALTIME:
        return Instant.now();
      case MONOTONIC:
        // For monotonic time, we use System.nanoTime() but convert to Instant
        // In a real implementation, this would be more sophisticated
        final long nanos = System.nanoTime();
        return Instant.ofEpochSecond(0, nanos);
      default:
        throw new WasiResourceException("Unsupported timer type: " + timerType);
    }
  }

  /**
   * Schedules a task to run after the specified delay.
   *
   * @param delay the delay before execution
   * @param task the task to execute
   * @throws WasmException if scheduling fails
   */
  public void scheduleOnce(final Duration delay, final Runnable task) throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.EXECUTE);
    recordAccess();

    if (!allowScheduling) {
      throw new WasiResourceException("Scheduling is not allowed for this timer resource");
    }

    if (executor == null) {
      throw new WasiResourceException("Executor is not available for scheduling");
    }

    if (delay == null) {
      throw new IllegalArgumentException("Delay cannot be null");
    }

    if (task == null) {
      throw new IllegalArgumentException("Task cannot be null");
    }

    // Cancel any existing scheduled task
    cancelScheduledTask();

    // Wrap the task to track execution
    final Runnable wrappedTask = () -> {
      try {
        timerExecutions.incrementAndGet();
        lastExecutionTime = Instant.now();
        task.run();
        LOGGER.fine("Executed scheduled task for timer " + getName());
      } catch (final Exception e) {
        LOGGER.warning("Error executing scheduled task: " + e.getMessage());
      } finally {
        timerActive.set(false);
      }
    };

    scheduledTask = executor.schedule(wrappedTask, delay.toMillis(), TimeUnit.MILLISECONDS);
    timerActive.set(true);
    lastScheduledDuration = delay;

    LOGGER.fine("Scheduled task to run in " + delay.toMillis() + "ms");
  }

  /**
   * Schedules a task to run repeatedly with the specified interval.
   *
   * @param initialDelay the initial delay before first execution
   * @param period the period between executions
   * @param task the task to execute
   * @throws WasmException if scheduling fails
   */
  public void scheduleRepeating(final Duration initialDelay, final Duration period, final Runnable task)
      throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.EXECUTE);
    recordAccess();

    if (!allowScheduling) {
      throw new WasiResourceException("Scheduling is not allowed for this timer resource");
    }

    if (executor == null) {
      throw new WasiResourceException("Executor is not available for scheduling");
    }

    if (initialDelay == null) {
      throw new IllegalArgumentException("Initial delay cannot be null");
    }

    if (period == null) {
      throw new IllegalArgumentException("Period cannot be null");
    }

    if (task == null) {
      throw new IllegalArgumentException("Task cannot be null");
    }

    // Cancel any existing scheduled task
    cancelScheduledTask();

    // Wrap the task to track execution
    final Runnable wrappedTask = () -> {
      try {
        timerExecutions.incrementAndGet();
        lastExecutionTime = Instant.now();
        task.run();
        LOGGER.fine("Executed repeating task for timer " + getName());
      } catch (final Exception e) {
        LOGGER.warning("Error executing repeating task: " + e.getMessage());
      }
    };

    scheduledTask = executor.scheduleAtFixedRate(wrappedTask,
        initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    timerActive.set(true);
    lastScheduledDuration = period;

    LOGGER.fine("Scheduled repeating task with initial delay " + initialDelay.toMillis() +
        "ms and period " + period.toMillis() + "ms");
  }

  /**
   * Cancels any currently scheduled task.
   *
   * @throws WasmException if cancellation fails
   */
  public void cancelScheduledTask() throws WasmException {
    ensureValid();
    recordAccess();

    if (scheduledTask != null && !scheduledTask.isDone()) {
      scheduledTask.cancel(false); // Don't interrupt if already running
      LOGGER.fine("Cancelled scheduled task for timer " + getName());
    }

    timerActive.set(false);
    scheduledTask = null;
  }

  /**
   * Sleeps for the specified duration.
   *
   * @param duration the duration to sleep
   * @throws WasmException if sleep fails
   */
  public void sleep(final Duration duration) throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.READ);
    recordAccess();

    if (duration == null) {
      throw new IllegalArgumentException("Duration cannot be null");
    }

    if (duration.isNegative() || duration.isZero()) {
      throw new IllegalArgumentException("Duration must be positive");
    }

    try {
      Thread.sleep(duration.toMillis());
      LOGGER.fine("Slept for " + duration.toMillis() + "ms");
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WasiResourceException("Sleep was interrupted", e);
    }
  }

  /**
   * Gets timer-specific statistics.
   *
   * @return timer usage statistics
   */
  public TimerStats getTimerStats() {
    return new TimerStats() {
      @Override
      public long getTimerExecutions() {
        return timerExecutions.get();
      }

      @Override
      public long getTimeRequests() {
        return timeRequests.get();
      }

      @Override
      public boolean isTimerActive() {
        return timerActive.get();
      }

      @Override
      public TimerType getTimerType() {
        return timerType;
      }

      @Override
      public Duration getResolution() {
        return resolution;
      }

      @Override
      public Instant getLastExecutionTime() {
        return lastExecutionTime;
      }

      @Override
      public Duration getLastScheduledDuration() {
        return lastScheduledDuration;
      }

      @Override
      public boolean isSchedulingAllowed() {
        return allowScheduling;
      }
    };
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }

    // Handle timer-specific operations first
    switch (operation.toLowerCase()) {
      case "get_current_time":
        return getCurrentTime();

      case "schedule_once":
        if (parameters.length < 2) {
          throw new IllegalArgumentException("schedule_once requires delay and task parameters");
        }
        scheduleOnce((Duration) parameters[0], (Runnable) parameters[1]);
        return null;

      case "schedule_repeating":
        if (parameters.length < 3) {
          throw new IllegalArgumentException("schedule_repeating requires initialDelay, period, and task parameters");
        }
        scheduleRepeating((Duration) parameters[0], (Duration) parameters[1], (Runnable) parameters[2]);
        return null;

      case "cancel_task":
        cancelScheduledTask();
        return null;

      case "sleep":
        if (parameters.length < 1) {
          throw new IllegalArgumentException("sleep requires duration parameter");
        }
        sleep((Duration) parameters[0]);
        return null;

      case "is_timer_active":
        return timerActive.get();

      case "get_stats":
        return getTimerStats();

      case "get_timer_type":
        return timerType;

      default:
        // Delegate to parent for common operations
        return super.invoke(operation, parameters);
    }
  }

  @Override
  protected void performCleanup() {
    // Cancel any scheduled tasks and shutdown executor
    if (scheduledTask != null && !scheduledTask.isDone()) {
      scheduledTask.cancel(true); // Interrupt if running
    }

    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (final InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
      LOGGER.fine("Shutdown timer executor for " + getName());
    }

    timerActive.set(false);
    super.performCleanup();
    LOGGER.fine("Timer resource cleanup completed for " + getName());
  }

  /**
   * Ensures the resource has the specified permission.
   *
   * @param required the required permission
   * @throws WasiResourceException if permission is not granted
   */
  private void ensurePermission(final WasiResourcePermissions required) throws WasiResourceException {
    if (!permissions.contains(required)) {
      throw new WasiResourceException("Permission denied: " + required.getName());
    }
  }

  /**
   * Enumeration of timer types.
   */
  public enum TimerType {
    REALTIME("realtime"),
    MONOTONIC("monotonic");

    private final String name;

    TimerType(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static TimerType fromName(final String name) {
      if (name == null) {
        return MONOTONIC; // Default
      }

      for (final TimerType type : values()) {
        if (type.name.equalsIgnoreCase(name.trim())) {
          return type;
        }
      }

      return MONOTONIC; // Default fallback
    }
  }

  /**
   * Interface for timer-specific statistics.
   */
  public interface TimerStats {
    long getTimerExecutions();
    long getTimeRequests();
    boolean isTimerActive();
    TimerType getTimerType();
    Duration getResolution();
    Instant getLastExecutionTime();
    Duration getLastScheduledDuration();
    boolean isSchedulingAllowed();
  }
}