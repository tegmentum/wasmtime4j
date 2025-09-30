package ai.tegmentum.wasmtime4j.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Comprehensive fuel consumption statistics for execution analysis.
 *
 * <p>Provides detailed metrics on fuel usage patterns, efficiency,
 * performance characteristics, and resource utilization for a specific
 * execution context.
 *
 * @since 1.0.0
 */
public final class FuelStatistics {

  private final String contextId;
  private final Instant statisticsTimestamp;
  private final Duration trackingPeriod;

  // Fuel allocation and consumption metrics
  private final long totalAllocated;
  private final long totalConsumed;
  private final long currentRemaining;
  private final long peakConsumption;
  private final double averageConsumptionRate;
  private final double consumptionEfficiency;

  // Function-level fuel tracking
  private final Map<String, Long> fuelByFunction;
  private final Map<String, Double> efficiencyByFunction;
  private final String mostExpensiveFunction;
  private final String mostEfficientFunction;

  // Instruction-level fuel tracking
  private final long totalInstructions;
  private final double averageFuelPerInstruction;
  private final long peakInstructionBurst;
  private final double instructionEfficiency;

  // Time-based consumption patterns
  private final Instant firstConsumption;
  private final Instant lastConsumption;
  private final Duration totalExecutionTime;
  private final double fuelPerSecond;
  private final int consumptionEvents;

  // Resource utilization metrics
  private final double allocationUtilization;
  private final double wasteRatio;
  private final long unusedFuel;
  private final int fuelDeficits;

  // Performance characteristics
  private final Duration averageConsumptionLatency;
  private final Duration peakConsumptionLatency;
  private final int throttlingEvents;
  private final Duration totalThrottleTime;

  // Predictive metrics
  private final long projectedConsumption;
  private final Duration estimatedDepletion;
  private final double consumptionTrend;
  private final FuelPriority effectivePriority;

  /**
   * Constructs fuel statistics with comprehensive metrics.
   *
   * @param builder statistics builder with all required metrics
   */
  private FuelStatistics(final Builder builder) {
    this.contextId = builder.contextId;
    this.statisticsTimestamp = builder.statisticsTimestamp;
    this.trackingPeriod = builder.trackingPeriod;
    this.totalAllocated = builder.totalAllocated;
    this.totalConsumed = builder.totalConsumed;
    this.currentRemaining = builder.currentRemaining;
    this.peakConsumption = builder.peakConsumption;
    this.averageConsumptionRate = builder.averageConsumptionRate;
    this.consumptionEfficiency = builder.consumptionEfficiency;
    this.fuelByFunction = Map.copyOf(builder.fuelByFunction);
    this.efficiencyByFunction = Map.copyOf(builder.efficiencyByFunction);
    this.mostExpensiveFunction = builder.mostExpensiveFunction;
    this.mostEfficientFunction = builder.mostEfficientFunction;
    this.totalInstructions = builder.totalInstructions;
    this.averageFuelPerInstruction = builder.averageFuelPerInstruction;
    this.peakInstructionBurst = builder.peakInstructionBurst;
    this.instructionEfficiency = builder.instructionEfficiency;
    this.firstConsumption = builder.firstConsumption;
    this.lastConsumption = builder.lastConsumption;
    this.totalExecutionTime = builder.totalExecutionTime;
    this.fuelPerSecond = builder.fuelPerSecond;
    this.consumptionEvents = builder.consumptionEvents;
    this.allocationUtilization = builder.allocationUtilization;
    this.wasteRatio = builder.wasteRatio;
    this.unusedFuel = builder.unusedFuel;
    this.fuelDeficits = builder.fuelDeficits;
    this.averageConsumptionLatency = builder.averageConsumptionLatency;
    this.peakConsumptionLatency = builder.peakConsumptionLatency;
    this.throttlingEvents = builder.throttlingEvents;
    this.totalThrottleTime = builder.totalThrottleTime;
    this.projectedConsumption = builder.projectedConsumption;
    this.estimatedDepletion = builder.estimatedDepletion;
    this.consumptionTrend = builder.consumptionTrend;
    this.effectivePriority = builder.effectivePriority;
  }

  // Getters for all metrics

  public String getContextId() {
    return contextId;
  }

  public Instant getStatisticsTimestamp() {
    return statisticsTimestamp;
  }

  public Duration getTrackingPeriod() {
    return trackingPeriod;
  }

  public long getTotalAllocated() {
    return totalAllocated;
  }

  public long getTotalConsumed() {
    return totalConsumed;
  }

  public long getCurrentRemaining() {
    return currentRemaining;
  }

  public long getPeakConsumption() {
    return peakConsumption;
  }

  public double getAverageConsumptionRate() {
    return averageConsumptionRate;
  }

  public double getConsumptionEfficiency() {
    return consumptionEfficiency;
  }

  public Map<String, Long> getFuelByFunction() {
    return fuelByFunction;
  }

  public Map<String, Double> getEfficiencyByFunction() {
    return efficiencyByFunction;
  }

  public String getMostExpensiveFunction() {
    return mostExpensiveFunction;
  }

  public String getMostEfficientFunction() {
    return mostEfficientFunction;
  }

  public long getTotalInstructions() {
    return totalInstructions;
  }

  public double getAverageFuelPerInstruction() {
    return averageFuelPerInstruction;
  }

  public long getPeakInstructionBurst() {
    return peakInstructionBurst;
  }

  public double getInstructionEfficiency() {
    return instructionEfficiency;
  }

  public Instant getFirstConsumption() {
    return firstConsumption;
  }

  public Instant getLastConsumption() {
    return lastConsumption;
  }

  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  public double getFuelPerSecond() {
    return fuelPerSecond;
  }

  public int getConsumptionEvents() {
    return consumptionEvents;
  }

  public double getAllocationUtilization() {
    return allocationUtilization;
  }

  public double getWasteRatio() {
    return wasteRatio;
  }

  public long getUnusedFuel() {
    return unusedFuel;
  }

  public int getFuelDeficits() {
    return fuelDeficits;
  }

  public Duration getAverageConsumptionLatency() {
    return averageConsumptionLatency;
  }

  public Duration getPeakConsumptionLatency() {
    return peakConsumptionLatency;
  }

  public int getThrottlingEvents() {
    return throttlingEvents;
  }

  public Duration getTotalThrottleTime() {
    return totalThrottleTime;
  }

  public long getProjectedConsumption() {
    return projectedConsumption;
  }

  public Duration getEstimatedDepletion() {
    return estimatedDepletion;
  }

  public double getConsumptionTrend() {
    return consumptionTrend;
  }

  public FuelPriority getEffectivePriority() {
    return effectivePriority;
  }

  /**
   * Calculates the fuel utilization rate as a percentage.
   *
   * @return fuel utilization percentage (0.0-100.0)
   */
  public double getUtilizationPercentage() {
    if (totalAllocated == 0) {
      return 0.0;
    }
    return (double) totalConsumed / totalAllocated * 100.0;
  }

  /**
   * Checks if fuel consumption is trending upward.
   *
   * @return true if consumption is increasing over time
   */
  public boolean isConsumptionIncreasing() {
    return consumptionTrend > 0.1; // 10% threshold for trend detection
  }

  /**
   * Checks if fuel depletion is imminent based on current consumption patterns.
   *
   * @param threshold time threshold for "imminent" depletion
   * @return true if fuel will be depleted within the threshold
   */
  public boolean isDepletionImminent(final Duration threshold) {
    return estimatedDepletion != null && estimatedDepletion.compareTo(threshold) <= 0;
  }

  /**
   * Gets the efficiency score as a normalized value (0.0-1.0).
   *
   * @return efficiency score where 1.0 is perfectly efficient
   */
  public double getEfficiencyScore() {
    return Math.max(0.0, Math.min(1.0, consumptionEfficiency));
  }

  /**
   * Creates a builder for constructing fuel statistics.
   *
   * @param contextId execution context identifier
   * @return new statistics builder
   * @throws IllegalArgumentException if contextId is null or empty
   */
  public static Builder builder(final String contextId) {
    if (contextId == null || contextId.trim().isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    return new Builder(contextId);
  }

  @Override
  public String toString() {
    return String.format(
        "FuelStatistics{contextId='%s', allocated=%d, consumed=%d, remaining=%d, efficiency=%.2f%%, utilization=%.1f%%}",
        contextId, totalAllocated, totalConsumed, currentRemaining,
        consumptionEfficiency * 100, getUtilizationPercentage());
  }

  /**
   * Builder for creating FuelStatistics instances.
   */
  public static final class Builder {
    private final String contextId;
    private Instant statisticsTimestamp = Instant.now();
    private Duration trackingPeriod = Duration.ZERO;
    private long totalAllocated = 0;
    private long totalConsumed = 0;
    private long currentRemaining = 0;
    private long peakConsumption = 0;
    private double averageConsumptionRate = 0.0;
    private double consumptionEfficiency = 0.0;
    private Map<String, Long> fuelByFunction = Map.of();
    private Map<String, Double> efficiencyByFunction = Map.of();
    private String mostExpensiveFunction = "";
    private String mostEfficientFunction = "";
    private long totalInstructions = 0;
    private double averageFuelPerInstruction = 0.0;
    private long peakInstructionBurst = 0;
    private double instructionEfficiency = 0.0;
    private Instant firstConsumption = Instant.now();
    private Instant lastConsumption = Instant.now();
    private Duration totalExecutionTime = Duration.ZERO;
    private double fuelPerSecond = 0.0;
    private int consumptionEvents = 0;
    private double allocationUtilization = 0.0;
    private double wasteRatio = 0.0;
    private long unusedFuel = 0;
    private int fuelDeficits = 0;
    private Duration averageConsumptionLatency = Duration.ZERO;
    private Duration peakConsumptionLatency = Duration.ZERO;
    private int throttlingEvents = 0;
    private Duration totalThrottleTime = Duration.ZERO;
    private long projectedConsumption = 0;
    private Duration estimatedDepletion = Duration.ZERO;
    private double consumptionTrend = 0.0;
    private FuelPriority effectivePriority = FuelPriority.NORMAL;

    private Builder(final String contextId) {
      this.contextId = contextId;
    }

    public Builder statisticsTimestamp(final Instant timestamp) {
      this.statisticsTimestamp = timestamp;
      return this;
    }

    public Builder trackingPeriod(final Duration period) {
      this.trackingPeriod = period;
      return this;
    }

    public Builder totalAllocated(final long allocated) {
      this.totalAllocated = allocated;
      return this;
    }

    public Builder totalConsumed(final long consumed) {
      this.totalConsumed = consumed;
      return this;
    }

    public Builder currentRemaining(final long remaining) {
      this.currentRemaining = remaining;
      return this;
    }

    public Builder peakConsumption(final long peak) {
      this.peakConsumption = peak;
      return this;
    }

    public Builder averageConsumptionRate(final double rate) {
      this.averageConsumptionRate = rate;
      return this;
    }

    public Builder consumptionEfficiency(final double efficiency) {
      this.consumptionEfficiency = efficiency;
      return this;
    }

    public Builder fuelByFunction(final Map<String, Long> fuel) {
      this.fuelByFunction = fuel;
      return this;
    }

    public Builder efficiencyByFunction(final Map<String, Double> efficiency) {
      this.efficiencyByFunction = efficiency;
      return this;
    }

    public Builder mostExpensiveFunction(final String function) {
      this.mostExpensiveFunction = function;
      return this;
    }

    public Builder mostEfficientFunction(final String function) {
      this.mostEfficientFunction = function;
      return this;
    }

    public Builder totalInstructions(final long instructions) {
      this.totalInstructions = instructions;
      return this;
    }

    public Builder averageFuelPerInstruction(final double fuelPerInstruction) {
      this.averageFuelPerInstruction = fuelPerInstruction;
      return this;
    }

    public Builder peakInstructionBurst(final long burst) {
      this.peakInstructionBurst = burst;
      return this;
    }

    public Builder instructionEfficiency(final double efficiency) {
      this.instructionEfficiency = efficiency;
      return this;
    }

    public Builder firstConsumption(final Instant first) {
      this.firstConsumption = first;
      return this;
    }

    public Builder lastConsumption(final Instant last) {
      this.lastConsumption = last;
      return this;
    }

    public Builder totalExecutionTime(final Duration time) {
      this.totalExecutionTime = time;
      return this;
    }

    public Builder fuelPerSecond(final double rate) {
      this.fuelPerSecond = rate;
      return this;
    }

    public Builder consumptionEvents(final int events) {
      this.consumptionEvents = events;
      return this;
    }

    public Builder allocationUtilization(final double utilization) {
      this.allocationUtilization = utilization;
      return this;
    }

    public Builder wasteRatio(final double ratio) {
      this.wasteRatio = ratio;
      return this;
    }

    public Builder unusedFuel(final long unused) {
      this.unusedFuel = unused;
      return this;
    }

    public Builder fuelDeficits(final int deficits) {
      this.fuelDeficits = deficits;
      return this;
    }

    public Builder averageConsumptionLatency(final Duration latency) {
      this.averageConsumptionLatency = latency;
      return this;
    }

    public Builder peakConsumptionLatency(final Duration latency) {
      this.peakConsumptionLatency = latency;
      return this;
    }

    public Builder throttlingEvents(final int events) {
      this.throttlingEvents = events;
      return this;
    }

    public Builder totalThrottleTime(final Duration time) {
      this.totalThrottleTime = time;
      return this;
    }

    public Builder projectedConsumption(final long projection) {
      this.projectedConsumption = projection;
      return this;
    }

    public Builder estimatedDepletion(final Duration depletion) {
      this.estimatedDepletion = depletion;
      return this;
    }

    public Builder consumptionTrend(final double trend) {
      this.consumptionTrend = trend;
      return this;
    }

    public Builder effectivePriority(final FuelPriority priority) {
      this.effectivePriority = priority;
      return this;
    }

    public FuelStatistics build() {
      return new FuelStatistics(this);
    }
  }
}