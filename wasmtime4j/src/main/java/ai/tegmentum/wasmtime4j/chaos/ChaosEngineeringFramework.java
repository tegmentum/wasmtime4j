/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.chaos;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Comprehensive chaos engineering framework for wasmtime4j providing systematic fault injection,
 * resilience testing, and failure scenario simulation.
 *
 * <p>This framework implements Netflix's Chaos Monkey principles with WebAssembly-specific fault
 * scenarios including:
 *
 * <ul>
 *   <li>Memory exhaustion and allocation failures
 *   <li>Execution timeout and infinite loop simulation
 *   <li>Native library loading failures
 *   <li>Resource constraint simulation (CPU, memory, disk)
 *   <li>Network partition and latency injection
 *   <li>Gradual degradation scenarios
 *   <li>Cascade failure simulation
 *   <li>Recovery time validation
 * </ul>
 *
 * @since 1.0.0
 */
public final class ChaosEngineeringFramework {

  private static final Logger LOGGER = Logger.getLogger(ChaosEngineeringFramework.class.getName());

  /** Types of chaos experiments that can be executed. */
  public enum ChaosExperimentType {
    MEMORY_EXHAUSTION("Simulate memory allocation failures"),
    CPU_SATURATION("Simulate CPU resource exhaustion"),
    TIMEOUT_INJECTION("Inject execution timeouts"),
    NATIVE_FAILURE("Simulate native library failures"),
    NETWORK_PARTITION("Simulate network connectivity issues"),
    DISK_FULL("Simulate disk space exhaustion"),
    GRADUAL_DEGRADATION("Simulate gradual performance degradation"),
    CASCADE_FAILURE("Simulate cascade failure scenarios"),
    RANDOM_ERRORS("Inject random runtime errors"),
    RESOURCE_STARVATION("Simulate resource starvation conditions");

    private final String description;

    ChaosExperimentType(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Severity levels for chaos experiments. */
  public enum ChaosSeverity {
    LOW(0.1), // 10% failure rate
    MEDIUM(0.25), // 25% failure rate
    HIGH(0.5), // 50% failure rate
    EXTREME(0.8); // 80% failure rate

    private final double failureRate;

    ChaosSeverity(final double failureRate) {
      this.failureRate = failureRate;
    }

    public double getFailureRate() {
      return failureRate;
    }
  }

  /** Configuration for a chaos experiment. */
  public static final class ChaosExperiment {
    private final String experimentId;
    private final ChaosExperimentType type;
    private final ChaosSeverity severity;
    private final Duration duration;
    private final String targetComponent;
    private final Map<String, Object> parameters;
    private final boolean enabled;
    private volatile Instant startTime;
    private volatile Instant endTime;
    private final AtomicLong executionCount;
    private final AtomicLong failureCount;

    /**
     * Creates a new chaos experiment.
     *
     * @param experimentId unique identifier for the experiment
     * @param type type of chaos experiment
     * @param severity severity level of the experiment
     * @param duration duration the experiment should run
     * @param targetComponent target component for the experiment
     * @param parameters experiment-specific parameters
     */
    public ChaosExperiment(
        final String experimentId,
        final ChaosExperimentType type,
        final ChaosSeverity severity,
        final Duration duration,
        final String targetComponent,
        final Map<String, Object> parameters) {
      this.experimentId = experimentId;
      this.type = type;
      this.severity = severity;
      this.duration = duration;
      this.targetComponent = targetComponent;
      this.parameters = Map.copyOf(parameters != null ? parameters : Map.of());
      this.enabled = true;
      this.executionCount = new AtomicLong(0);
      this.failureCount = new AtomicLong(0);
    }

    public String getExperimentId() {
      return experimentId;
    }

    public ChaosExperimentType getType() {
      return type;
    }

    public ChaosSeverity getSeverity() {
      return severity;
    }

    public Duration getDuration() {
      return duration;
    }

    public String getTargetComponent() {
      return targetComponent;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public Instant getStartTime() {
      return startTime;
    }

    public Instant getEndTime() {
      return endTime;
    }

    public long getExecutionCount() {
      return executionCount.get();
    }

    public long getFailureCount() {
      return failureCount.get();
    }

    /**
     * Checks if the chaos experiment is currently active.
     *
     * @return true if the experiment is active, false otherwise
     */
    public boolean isActive() {
      return startTime != null
          && (endTime == null || Instant.now().isBefore(endTime))
          && Instant.now().isBefore(startTime.plus(duration));
    }

    public void start() {
      this.startTime = Instant.now();
      this.endTime = startTime.plus(duration);
    }

    public void recordExecution() {
      this.executionCount.incrementAndGet();
    }

    public void recordFailure() {
      this.failureCount.incrementAndGet();
    }

    public void stop() {
      this.endTime = Instant.now();
    }
  }

  /** Result of a chaos experiment execution. */
  public static final class ChaosExperimentResult {
    private final String experimentId;
    private final boolean successful;
    private final Duration executionTime;
    private final String resultMessage;
    private final Map<String, Object> metrics;
    private final Throwable exception;
    private final Instant timestamp;

    /**
     * Creates a new chaos experiment result.
     *
     * @param experimentId identifier of the experiment
     * @param successful whether the experiment was successful
     * @param executionTime time taken to execute the experiment
     * @param resultMessage result message from the experiment
     * @param metrics experiment metrics
     * @param exception exception thrown during experiment (if any)
     */
    public ChaosExperimentResult(
        final String experimentId,
        final boolean successful,
        final Duration executionTime,
        final String resultMessage,
        final Map<String, Object> metrics,
        final Throwable exception) {
      this.experimentId = experimentId;
      this.successful = successful;
      this.executionTime = executionTime;
      this.resultMessage = resultMessage;
      this.metrics = Map.copyOf(metrics != null ? metrics : Map.of());
      this.exception = exception;
      this.timestamp = Instant.now();
    }

    public String getExperimentId() {
      return experimentId;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public Duration getExecutionTime() {
      return executionTime;
    }

    public String getResultMessage() {
      return resultMessage;
    }

    public Map<String, Object> getMetrics() {
      return metrics;
    }

    public Throwable getException() {
      return exception;
    }

    public Instant getTimestamp() {
      return timestamp;
    }
  }

  /** Fault injection interface for different failure types. */
  public interface FaultInjector {
    /**
     * Injects a fault based on the experiment configuration.
     *
     * @param experiment the chaos experiment
     * @return true if fault was injected, false otherwise
     */
    boolean injectFault(ChaosExperiment experiment);

    /**
     * Removes the injected fault.
     *
     * @param experiment the chaos experiment
     */
    void removeFault(ChaosExperiment experiment);

    /**
     * Gets the fault injector name.
     *
     * @return injector name
     */
    String getName();
  }

  /** Memory exhaustion fault injector. */
  @SuppressFBWarnings(
      value = "DM_GC",
      justification = "System.gc() is intentional for chaos engineering memory pressure simulation")
  private static final class MemoryExhaustionInjector implements FaultInjector {
    private volatile List<byte[]> memoryHog;

    @Override
    public boolean injectFault(final ChaosExperiment experiment) {
      try {
        final int allocationSize =
            (Integer) experiment.getParameters().getOrDefault("allocationSize", 10_000_000);
        final int allocationCount =
            (Integer) experiment.getParameters().getOrDefault("allocationCount", 100);

        memoryHog = new java.util.ArrayList<>();
        for (int i = 0; i < allocationCount; i++) {
          memoryHog.add(new byte[allocationSize]);
          if (i % 10 == 0) {
            System.gc(); // Force garbage collection to increase pressure
            Thread.sleep(10);
          }
        }
        return true;
      } catch (final Exception e) {
        LOGGER.warning("Memory exhaustion injection failed: " + e.getMessage());
        return false;
      }
    }

    @Override
    public void removeFault(final ChaosExperiment experiment) {
      if (memoryHog != null) {
        memoryHog.clear();
        memoryHog = null;
        System.gc();
      }
    }

    @Override
    public String getName() {
      return "MemoryExhaustionInjector";
    }
  }

  /** CPU saturation fault injector. */
  private static final class CpuSaturationInjector implements FaultInjector {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private volatile boolean active = false;
    private volatile Thread[] cpuThreads;

    @Override
    public boolean injectFault(final ChaosExperiment experiment) {
      try {
        final int threadCount =
            (Integer)
                experiment
                    .getParameters()
                    .getOrDefault("threadCount", Runtime.getRuntime().availableProcessors());
        final int intensityPercent =
            (Integer) experiment.getParameters().getOrDefault("intensityPercent", 80);

        active = true;
        cpuThreads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
          cpuThreads[i] =
              new Thread(
                  () -> {
                    while (active) {
                      // CPU-intensive work
                      for (int j = 0; j < intensityPercent * 1000; j++) {
                        Math.sin(SECURE_RANDOM.nextDouble());
                      }
                      try {
                        Thread.sleep(100 - intensityPercent); // Brief pause based on intensity
                      } catch (final InterruptedException e) {
                        break;
                      }
                    }
                  },
                  "ChaosEngineeringCpuSaturation-" + i);
          cpuThreads[i].setDaemon(true);
          cpuThreads[i].start();
        }
        return true;
      } catch (final Exception e) {
        LOGGER.warning("CPU saturation injection failed: " + e.getMessage());
        return false;
      }
    }

    @Override
    public void removeFault(final ChaosExperiment experiment) {
      active = false;
      if (cpuThreads != null) {
        for (final Thread thread : cpuThreads) {
          if (thread != null) {
            thread.interrupt();
          }
        }
        cpuThreads = null;
      }
    }

    @Override
    public String getName() {
      return "CpuSaturationInjector";
    }
  }

  /** Random error fault injector. */
  private static final class RandomErrorInjector implements FaultInjector {
    private final SecureRandom random = new SecureRandom();
    private volatile boolean active = false;

    @Override
    public boolean injectFault(final ChaosExperiment experiment) {
      active = true;
      return true;
    }

    @Override
    public void removeFault(final ChaosExperiment experiment) {
      active = false;
    }

    @Override
    public String getName() {
      return "RandomErrorInjector";
    }

    public boolean shouldInjectError(final double failureRate) {
      return active && random.nextDouble() < failureRate;
    }
  }

  /** Network latency fault injector. */
  private static final class NetworkLatencyInjector implements FaultInjector {
    private volatile boolean active = false;
    private volatile long latencyMs = 0;

    @Override
    public boolean injectFault(final ChaosExperiment experiment) {
      this.latencyMs = (Long) experiment.getParameters().getOrDefault("latencyMs", 1000L);
      this.active = true;
      return true;
    }

    @Override
    public void removeFault(final ChaosExperiment experiment) {
      this.active = false;
      this.latencyMs = 0;
    }

    @Override
    public String getName() {
      return "NetworkLatencyInjector";
    }

    public void injectLatency() {
      if (active && latencyMs > 0) {
        try {
          Thread.sleep(latencyMs);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /** Active chaos experiments. */
  private final ConcurrentHashMap<String, ChaosExperiment> activeExperiments =
      new ConcurrentHashMap<>();

  /** Available fault injectors. */
  private final ConcurrentHashMap<ChaosExperimentType, FaultInjector> faultInjectors =
      new ConcurrentHashMap<>();

  /** Experiment execution history. */
  private final ConcurrentHashMap<String, ChaosExperimentResult> experimentResults =
      new ConcurrentHashMap<>();

  /** Framework state. */
  private final AtomicBoolean enabled = new AtomicBoolean(false);

  private final AtomicBoolean safetyMode = new AtomicBoolean(true);
  private final AtomicLong totalExperiments = new AtomicLong(0);
  private final AtomicLong successfulExperiments = new AtomicLong(0);
  private final AtomicLong failedExperiments = new AtomicLong(0);

  /** Background processing. */
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

  private final SecureRandom random = new SecureRandom();

  /** Configuration. */
  private volatile Duration maxExperimentDuration = Duration.ofMinutes(30);

  private volatile int maxConcurrentExperiments = 3;
  private volatile double globalFailureRate = 0.1;

  /** Specific injectors. */
  private final RandomErrorInjector randomErrorInjector = new RandomErrorInjector();

  private final NetworkLatencyInjector networkLatencyInjector = new NetworkLatencyInjector();

  /** Creates a new chaos engineering framework. */
  public ChaosEngineeringFramework() {
    initializeFaultInjectors();
    startBackgroundProcessing();
    LOGGER.info("Chaos Engineering Framework initialized");
  }

  /** Initializes fault injectors. */
  private void initializeFaultInjectors() {
    faultInjectors.put(ChaosExperimentType.MEMORY_EXHAUSTION, new MemoryExhaustionInjector());
    faultInjectors.put(ChaosExperimentType.CPU_SATURATION, new CpuSaturationInjector());
    faultInjectors.put(ChaosExperimentType.RANDOM_ERRORS, randomErrorInjector);
    faultInjectors.put(ChaosExperimentType.NETWORK_PARTITION, networkLatencyInjector);
  }

  /**
   * Enables chaos engineering with safety checks.
   *
   * @param safetyEnabled true to enable safety mode (limited impact)
   */
  public void enable(final boolean safetyEnabled) {
    this.safetyMode.set(safetyEnabled);
    this.enabled.set(true);
    LOGGER.warning(
        "Chaos Engineering ENABLED"
            + (safetyEnabled ? " (Safety Mode)" : " (FULL MODE - USE WITH CAUTION)"));
  }

  /** Disables chaos engineering and stops all active experiments. */
  public void disable() {
    this.enabled.set(false);
    stopAllExperiments();
    LOGGER.info("Chaos Engineering DISABLED");
  }

  /**
   * Starts a chaos experiment.
   *
   * @param experiment the experiment to start
   * @return true if experiment was started successfully
   */
  public boolean startExperiment(final ChaosExperiment experiment) {
    if (!enabled.get()) {
      LOGGER.warning("Cannot start experiment: Chaos Engineering is disabled");
      return false;
    }

    if (activeExperiments.size() >= maxConcurrentExperiments) {
      LOGGER.warning("Cannot start experiment: Maximum concurrent experiments reached");
      return false;
    }

    if (safetyMode.get() && experiment.getSeverity() == ChaosSeverity.EXTREME) {
      LOGGER.warning("Cannot start EXTREME experiment: Safety mode is enabled");
      return false;
    }

    try {
      final FaultInjector injector = faultInjectors.get(experiment.getType());
      if (injector == null) {
        LOGGER.warning("No fault injector available for type: " + experiment.getType());
        return false;
      }

      experiment.start();
      if (injector.injectFault(experiment)) {
        activeExperiments.put(experiment.getExperimentId(), experiment);
        totalExperiments.incrementAndGet();

        LOGGER.warning(
            String.format(
                "Started chaos experiment: %s [%s] for %s on %s",
                experiment.getExperimentId(),
                experiment.getType(),
                experiment.getDuration(),
                experiment.getTargetComponent()));

        // Schedule automatic stop
        executorService.schedule(
            () -> stopExperiment(experiment.getExperimentId()),
            experiment.getDuration().toMillis(),
            TimeUnit.MILLISECONDS);

        return true;
      } else {
        experiment.stop();
        failedExperiments.incrementAndGet();
        return false;
      }

    } catch (final Exception e) {
      LOGGER.severe("Failed to start chaos experiment: " + e.getMessage());
      failedExperiments.incrementAndGet();
      return false;
    }
  }

  /**
   * Stops a chaos experiment.
   *
   * @param experimentId the experiment ID to stop
   * @return true if experiment was stopped successfully
   */
  public boolean stopExperiment(final String experimentId) {
    final ChaosExperiment experiment = activeExperiments.remove(experimentId);
    if (experiment == null) {
      return false;
    }

    try {
      final FaultInjector injector = faultInjectors.get(experiment.getType());
      if (injector != null) {
        injector.removeFault(experiment);
      }

      experiment.stop();
      successfulExperiments.incrementAndGet();

      final ChaosExperimentResult result =
          new ChaosExperimentResult(
              experimentId,
              true,
              Duration.between(experiment.getStartTime(), experiment.getEndTime()),
              "Experiment completed successfully",
              Map.of(
                  "executionCount",
                  experiment.getExecutionCount(),
                  "failureCount",
                  experiment.getFailureCount()),
              null);

      experimentResults.put(experimentId, result);

      LOGGER.info(
          String.format(
              "Stopped chaos experiment: %s [executions=%d, failures=%d]",
              experimentId, experiment.getExecutionCount(), experiment.getFailureCount()));

      return true;

    } catch (final Exception e) {
      LOGGER.severe("Failed to stop chaos experiment " + experimentId + ": " + e.getMessage());
      return false;
    }
  }

  /** Stops all active experiments immediately. */
  public void stopAllExperiments() {
    final List<String> experimentIds = List.copyOf(activeExperiments.keySet());
    for (final String experimentId : experimentIds) {
      stopExperiment(experimentId);
    }
    LOGGER.info("Stopped all chaos experiments");
  }

  /**
   * Determines if a fault should be injected based on current experiments.
   *
   * @param component the target component
   * @param operation the operation being performed
   * @return true if fault should be injected
   */
  public boolean shouldInjectFault(final String component, final String operation) {
    if (!enabled.get()) {
      return false;
    }

    // Check active experiments
    for (final ChaosExperiment experiment : activeExperiments.values()) {
      if (experiment.isActive()
          && experiment.getTargetComponent().equals(component)
          && random.nextDouble() < experiment.getSeverity().getFailureRate()) {
        experiment.recordExecution();
        experiment.recordFailure();
        return true;
      }
    }

    // Global random failure injection
    if (random.nextDouble() < globalFailureRate) {
      return randomErrorInjector.shouldInjectError(globalFailureRate);
    }

    return false;
  }

  /**
   * Injects network latency if network experiments are active.
   *
   * @param operation the network operation
   */
  public void maybeInjectNetworkLatency(final String operation) {
    if (!enabled.get()) {
      return;
    }

    for (final ChaosExperiment experiment : activeExperiments.values()) {
      if (experiment.isActive() && experiment.getType() == ChaosExperimentType.NETWORK_PARTITION) {
        networkLatencyInjector.injectLatency();
        experiment.recordExecution();
        break;
      }
    }
  }

  /**
   * Creates a pre-configured memory exhaustion experiment.
   *
   * @param duration experiment duration
   * @param severity chaos severity
   * @return configured experiment
   */
  public ChaosExperiment createMemoryExhaustionExperiment(
      final Duration duration, final ChaosSeverity severity) {
    return new ChaosExperiment(
        "memory_exhaustion_" + System.currentTimeMillis(),
        ChaosExperimentType.MEMORY_EXHAUSTION,
        severity,
        duration,
        "wasmtime4j",
        Map.of("allocationSize", 5_000_000, "allocationCount", 50));
  }

  /**
   * Creates a pre-configured CPU saturation experiment.
   *
   * @param duration experiment duration
   * @param severity chaos severity
   * @return configured experiment
   */
  public ChaosExperiment createCpuSaturationExperiment(
      final Duration duration, final ChaosSeverity severity) {
    return new ChaosExperiment(
        "cpu_saturation_" + System.currentTimeMillis(),
        ChaosExperimentType.CPU_SATURATION,
        severity,
        duration,
        "wasmtime4j",
        Map.of("threadCount", Runtime.getRuntime().availableProcessors(), "intensityPercent", 70));
  }

  /**
   * Creates a pre-configured random error experiment.
   *
   * @param duration experiment duration
   * @param severity chaos severity
   * @return configured experiment
   */
  public ChaosExperiment createRandomErrorExperiment(
      final Duration duration, final ChaosSeverity severity) {
    return new ChaosExperiment(
        "random_errors_" + System.currentTimeMillis(),
        ChaosExperimentType.RANDOM_ERRORS,
        severity,
        duration,
        "wasmtime4j",
        Map.of("errorTypes", List.of("RuntimeException", "OutOfMemoryError", "TimeoutException")));
  }

  /** Starts background processing for experiment management. */
  private void startBackgroundProcessing() {
    // Experiment monitoring task
    executorService.scheduleAtFixedRate(this::monitorExperiments, 30, 30, TimeUnit.SECONDS);

    // Safety checks task
    executorService.scheduleAtFixedRate(this::performSafetyChecks, 60, 60, TimeUnit.SECONDS);
  }

  /** Monitors active experiments and handles timeouts. */
  private void monitorExperiments() {
    try {
      final List<String> expiredExperiments = new java.util.ArrayList<>();

      for (final Map.Entry<String, ChaosExperiment> entry : activeExperiments.entrySet()) {
        final ChaosExperiment experiment = entry.getValue();
        if (!experiment.isActive()) {
          expiredExperiments.add(entry.getKey());
        }
      }

      // Clean up expired experiments
      for (final String experimentId : expiredExperiments) {
        stopExperiment(experimentId);
      }

      if (!expiredExperiments.isEmpty()) {
        LOGGER.fine("Cleaned up " + expiredExperiments.size() + " expired experiments");
      }

    } catch (final Exception e) {
      LOGGER.warning("Experiment monitoring failed: " + e.getMessage());
    }
  }

  /** Performs safety checks and emergency shutdowns if needed. */
  private void performSafetyChecks() {
    try {
      // Check system resource usage
      final Runtime runtime = Runtime.getRuntime();
      final long usedMemory = runtime.totalMemory() - runtime.freeMemory();
      final long maxMemory = runtime.maxMemory();
      final double memoryUsage = (double) usedMemory / maxMemory;

      // Emergency shutdown if memory usage is critical
      if (memoryUsage > 0.95) {
        LOGGER.severe(
            String.format(
                "EMERGENCY: Critical memory usage (%.1f%%) - stopping all chaos experiments",
                memoryUsage * 100));
        stopAllExperiments();
        return;
      }

      // Warning if memory usage is high
      if (memoryUsage > 0.85) {
        LOGGER.warning(
            String.format(
                "High memory usage (%.1f%%) during chaos experiments", memoryUsage * 100));
      }

      // Check for runaway experiments
      final long currentTime = System.currentTimeMillis();
      for (final ChaosExperiment experiment : activeExperiments.values()) {
        final long runningTime = currentTime - experiment.getStartTime().toEpochMilli();
        if (runningTime > maxExperimentDuration.toMillis()) {
          LOGGER.warning("Stopping runaway experiment: " + experiment.getExperimentId());
          stopExperiment(experiment.getExperimentId());
        }
      }

    } catch (final Exception e) {
      LOGGER.warning("Safety check failed: " + e.getMessage());
    }
  }

  /**
   * Gets statistics about chaos engineering execution.
   *
   * @return formatted statistics
   */
  public String getStatistics() {
    return String.format(
        "Chaos Engineering Statistics: enabled=%s, safety_mode=%s, "
            + "total_experiments=%d, successful=%d, failed=%d, active=%d",
        enabled.get(),
        safetyMode.get(),
        totalExperiments.get(),
        successfulExperiments.get(),
        failedExperiments.get(),
        activeExperiments.size());
  }

  /**
   * Gets detailed status of all active experiments.
   *
   * @return formatted status report
   */
  @SuppressFBWarnings(
      value = "VA_FORMAT_STRING_USES_NEWLINE",
      justification = "Using \\n for consistent output in status report display")
  public String getActiveExperimentsStatus() {
    if (activeExperiments.isEmpty()) {
      return "No active chaos experiments";
    }

    final StringBuilder sb = new StringBuilder("Active Chaos Experiments:\n");
    for (final ChaosExperiment experiment : activeExperiments.values()) {
      final Duration remaining = Duration.between(Instant.now(), experiment.getEndTime());
      sb.append(
          String.format(
              "  %s [%s]: %s on %s (remaining: %s, executions: %d, failures: %d)\n",
              experiment.getExperimentId(),
              experiment.getType(),
              experiment.getSeverity(),
              experiment.getTargetComponent(),
              remaining.toSeconds() > 0 ? remaining.toSeconds() + "s" : "expired",
              experiment.getExecutionCount(),
              experiment.getFailureCount()));
    }
    return sb.toString();
  }

  /** Sets the global failure rate for random errors. */
  public void setGlobalFailureRate(final double failureRate) {
    this.globalFailureRate = Math.max(0.0, Math.min(1.0, failureRate));
    LOGGER.info("Global failure rate set to: " + (this.globalFailureRate * 100) + "%");
  }

  /** Gets the current global failure rate. */
  public double getGlobalFailureRate() {
    return globalFailureRate;
  }

  /** Checks if chaos engineering is enabled. */
  public boolean isEnabled() {
    return enabled.get();
  }

  /** Checks if safety mode is enabled. */
  public boolean isSafetyMode() {
    return safetyMode.get();
  }

  /** Gets the number of active experiments. */
  public int getActiveExperimentCount() {
    return activeExperiments.size();
  }

  /** Shuts down the chaos engineering framework. */
  public void shutdown() {
    disable();
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (final InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Chaos Engineering Framework shutdown");
  }
}
