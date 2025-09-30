package ai.tegmentum.wasmtime4j.parallel;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Statistics for an instance pool.
 *
 * <p>Provides metrics and usage information about a WebAssembly instance pool,
 * including size, utilization, and performance metrics.
 *
 * @since 1.0.0
 */
public final class InstancePoolStatistics {

  private final int totalInstances;
  private final int availableInstances;
  private final int inUseInstances;
  private final int unhealthyInstances;
  private final double utilizationPercentage;
  private final long totalRequests;
  private final long fulfilledRequests;
  private final long rejectedRequests;
  private final Duration averageWaitTime;
  private final Duration averageExecutionTime;
  private final Instant statisticsTimestamp;
  private final Duration uptime;

  private InstancePoolStatistics(final int totalInstances,
                                 final int availableInstances,
                                 final int inUseInstances,
                                 final int unhealthyInstances,
                                 final double utilizationPercentage,
                                 final long totalRequests,
                                 final long fulfilledRequests,
                                 final long rejectedRequests,
                                 final Duration averageWaitTime,
                                 final Duration averageExecutionTime,
                                 final Instant statisticsTimestamp,
                                 final Duration uptime) {
    this.totalInstances = totalInstances;
    this.availableInstances = availableInstances;
    this.inUseInstances = inUseInstances;
    this.unhealthyInstances = unhealthyInstances;
    this.utilizationPercentage = utilizationPercentage;
    this.totalRequests = totalRequests;
    this.fulfilledRequests = fulfilledRequests;
    this.rejectedRequests = rejectedRequests;
    this.averageWaitTime = Objects.requireNonNull(averageWaitTime);
    this.averageExecutionTime = Objects.requireNonNull(averageExecutionTime);
    this.statisticsTimestamp = Objects.requireNonNull(statisticsTimestamp);
    this.uptime = Objects.requireNonNull(uptime);
  }

  /**
   * Creates a new builder for instance pool statistics.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the total number of instances in the pool.
   *
   * @return total instances
   */
  public int getTotalInstances() {
    return totalInstances;
  }

  /**
   * Gets the number of available instances.
   *
   * @return available instances
   */
  public int getAvailableInstances() {
    return availableInstances;
  }

  /**
   * Gets the number of instances currently in use.
   *
   * @return in-use instances
   */
  public int getInUseInstances() {
    return inUseInstances;
  }

  /**
   * Gets the number of unhealthy instances.
   *
   * @return unhealthy instances
   */
  public int getUnhealthyInstances() {
    return unhealthyInstances;
  }

  /**
   * Gets the pool utilization percentage.
   *
   * @return utilization percentage (0-100)
   */
  public double getUtilizationPercentage() {
    return utilizationPercentage;
  }

  /**
   * Gets the total number of requests processed.
   *
   * @return total requests
   */
  public long getTotalRequests() {
    return totalRequests;
  }

  /**
   * Gets the number of fulfilled requests.
   *
   * @return fulfilled requests
   */
  public long getFulfilledRequests() {
    return fulfilledRequests;
  }

  /**
   * Gets the number of rejected requests.
   *
   * @return rejected requests
   */
  public long getRejectedRequests() {
    return rejectedRequests;
  }

  /**
   * Gets the average wait time for requests.
   *
   * @return average wait time
   */
  public Duration getAverageWaitTime() {
    return averageWaitTime;
  }

  /**
   * Gets the average execution time.
   *
   * @return average execution time
   */
  public Duration getAverageExecutionTime() {
    return averageExecutionTime;
  }

  /**
   * Gets when these statistics were collected.
   *
   * @return statistics timestamp
   */
  public Instant getStatisticsTimestamp() {
    return statisticsTimestamp;
  }

  /**
   * Gets the pool uptime.
   *
   * @return uptime duration
   */
  public Duration getUptime() {
    return uptime;
  }

  /**
   * Gets the request fulfillment rate.
   *
   * @return fulfillment rate (0-1)
   */
  public double getFulfillmentRate() {
    if (totalRequests == 0) return 1.0;
    return (double) fulfilledRequests / totalRequests;
  }

  /**
   * Builder for instance pool statistics.
   */
  public static final class Builder {
    private int totalInstances;
    private int availableInstances;
    private int inUseInstances;
    private int unhealthyInstances;
    private double utilizationPercentage;
    private long totalRequests;
    private long fulfilledRequests;
    private long rejectedRequests;
    private Duration averageWaitTime = Duration.ZERO;
    private Duration averageExecutionTime = Duration.ZERO;
    private Instant statisticsTimestamp = Instant.now();
    private Duration uptime = Duration.ZERO;

    public Builder totalInstances(final int totalInstances) {
      this.totalInstances = totalInstances;
      return this;
    }

    public Builder availableInstances(final int availableInstances) {
      this.availableInstances = availableInstances;
      return this;
    }

    public Builder inUseInstances(final int inUseInstances) {
      this.inUseInstances = inUseInstances;
      return this;
    }

    public Builder unhealthyInstances(final int unhealthyInstances) {
      this.unhealthyInstances = unhealthyInstances;
      return this;
    }

    public Builder utilizationPercentage(final double utilizationPercentage) {
      this.utilizationPercentage = utilizationPercentage;
      return this;
    }

    public Builder totalRequests(final long totalRequests) {
      this.totalRequests = totalRequests;
      return this;
    }

    public Builder fulfilledRequests(final long fulfilledRequests) {
      this.fulfilledRequests = fulfilledRequests;
      return this;
    }

    public Builder rejectedRequests(final long rejectedRequests) {
      this.rejectedRequests = rejectedRequests;
      return this;
    }

    public Builder averageWaitTime(final Duration averageWaitTime) {
      this.averageWaitTime = Objects.requireNonNull(averageWaitTime);
      return this;
    }

    public Builder averageExecutionTime(final Duration averageExecutionTime) {
      this.averageExecutionTime = Objects.requireNonNull(averageExecutionTime);
      return this;
    }

    public Builder statisticsTimestamp(final Instant statisticsTimestamp) {
      this.statisticsTimestamp = Objects.requireNonNull(statisticsTimestamp);
      return this;
    }

    public Builder uptime(final Duration uptime) {
      this.uptime = Objects.requireNonNull(uptime);
      return this;
    }

    public InstancePoolStatistics build() {
      return new InstancePoolStatistics(
          totalInstances, availableInstances, inUseInstances, unhealthyInstances,
          utilizationPercentage, totalRequests, fulfilledRequests, rejectedRequests,
          averageWaitTime, averageExecutionTime, statisticsTimestamp, uptime);
    }
  }

  @Override
  public String toString() {
    return String.format("InstancePoolStatistics{total=%d, available=%d, inUse=%d, unhealthy=%d, utilization=%.1f%%}",
        totalInstances, availableInstances, inUseInstances, unhealthyInstances, utilizationPercentage);
  }
}