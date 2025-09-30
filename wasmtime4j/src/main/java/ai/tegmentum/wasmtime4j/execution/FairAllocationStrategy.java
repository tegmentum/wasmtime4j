package ai.tegmentum.wasmtime4j.execution;

/**
 * Fair allocation strategy interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface FairAllocationStrategy {

  /**
   * Gets the strategy name.
   *
   * @return strategy name
   */
  String getName();

  /**
   * Gets the allocation algorithm.
   *
   * @return allocation algorithm
   */
  AllocationAlgorithm getAlgorithm();

  /**
   * Sets the allocation algorithm.
   *
   * @param algorithm allocation algorithm
   */
  void setAlgorithm(AllocationAlgorithm algorithm);

  /**
   * Gets the fairness metric.
   *
   * @return fairness metric
   */
  FairnessMetric getFairnessMetric();

  /**
   * Sets the fairness metric.
   *
   * @param metric fairness metric
   */
  void setFairnessMetric(FairnessMetric metric);

  /**
   * Allocates resources fairly among executions.
   *
   * @param requests list of allocation requests
   * @return allocation results
   */
  java.util.List<AllocationResult> allocate(java.util.List<AllocationRequest> requests);

  /**
   * Gets allocation constraints.
   *
   * @return allocation constraints
   */
  AllocationConstraints getConstraints();

  /**
   * Sets allocation constraints.
   *
   * @param constraints allocation constraints
   */
  void setConstraints(AllocationConstraints constraints);

  /**
   * Gets the allocation history.
   *
   * @return allocation history
   */
  AllocationHistory getHistory();

  /**
   * Evaluates allocation fairness.
   *
   * @return fairness evaluation
   */
  FairnessEvaluation evaluateFairness();

  /** Allocation algorithm enumeration. */
  enum AllocationAlgorithm {
    /** Round robin allocation. */
    ROUND_ROBIN,
    /** Weighted fair queuing. */
    WEIGHTED_FAIR_QUEUING,
    /** Proportional share. */
    PROPORTIONAL_SHARE,
    /** Lottery scheduling. */
    LOTTERY_SCHEDULING,
    /** Deficit round robin. */
    DEFICIT_ROUND_ROBIN
  }

  /** Fairness metric enumeration. */
  enum FairnessMetric {
    /** Jain's fairness index. */
    JAINS_INDEX,
    /** Coefficient of variation. */
    COEFFICIENT_OF_VARIATION,
    /** Max-min fairness. */
    MAX_MIN_FAIRNESS,
    /** Proportional fairness. */
    PROPORTIONAL_FAIRNESS
  }

  /** Allocation request interface. */
  interface AllocationRequest {
    /**
     * Gets the request ID.
     *
     * @return request ID
     */
    String getId();

    /**
     * Gets the requested resources.
     *
     * @return resource requirements
     */
    ResourceRequirements getRequiredResources();

    /**
     * Gets the request priority.
     *
     * @return priority
     */
    int getPriority();

    /**
     * Gets the request weight.
     *
     * @return weight
     */
    double getWeight();

    /**
     * Gets the deadline.
     *
     * @return deadline timestamp
     */
    long getDeadline();
  }

  /** Allocation result interface. */
  interface AllocationResult {
    /**
     * Gets the request ID.
     *
     * @return request ID
     */
    String getRequestId();

    /**
     * Gets the allocated resources.
     *
     * @return allocated resources
     */
    AllocatedResources getAllocatedResources();

    /**
     * Gets the allocation timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets the allocation score.
     *
     * @return allocation score
     */
    double getScore();

    /**
     * Checks if allocation was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();
  }

  /** Resource requirements interface. */
  interface ResourceRequirements {
    /**
     * Gets CPU requirement.
     *
     * @return CPU cores
     */
    double getCpuCores();

    /**
     * Gets memory requirement.
     *
     * @return memory in bytes
     */
    long getMemoryBytes();

    /**
     * Gets fuel requirement.
     *
     * @return fuel amount
     */
    long getFuel();

    /**
     * Gets time requirement.
     *
     * @return time in milliseconds
     */
    long getTimeMs();
  }

  /** Allocated resources interface. */
  interface AllocatedResources {
    /**
     * Gets allocated CPU cores.
     *
     * @return CPU cores
     */
    double getCpuCores();

    /**
     * Gets allocated memory.
     *
     * @return memory in bytes
     */
    long getMemoryBytes();

    /**
     * Gets allocated fuel.
     *
     * @return fuel amount
     */
    long getFuel();

    /**
     * Gets allocated time.
     *
     * @return time in milliseconds
     */
    long getTimeMs();
  }

  /** Allocation constraints interface. */
  interface AllocationConstraints {
    /**
     * Gets maximum CPU allocation per request.
     *
     * @return max CPU cores
     */
    double getMaxCpuPerRequest();

    /**
     * Gets maximum memory allocation per request.
     *
     * @return max memory in bytes
     */
    long getMaxMemoryPerRequest();

    /**
     * Gets minimum resource guarantee.
     *
     * @return min resource guarantee
     */
    ResourceRequirements getMinGuarantee();

    /**
     * Gets total available resources.
     *
     * @return available resources
     */
    AllocatedResources getTotalResources();
  }

  /** Allocation history interface. */
  interface AllocationHistory {
    /**
     * Gets historical allocations.
     *
     * @return list of historical allocations
     */
    java.util.List<AllocationResult> getHistoricalAllocations();

    /**
     * Gets allocation statistics.
     *
     * @return allocation statistics
     */
    AllocationStatistics getStatistics();

    /**
     * Gets allocation trends.
     *
     * @return allocation trends
     */
    AllocationTrends getTrends();
  }

  /** Fairness evaluation interface. */
  interface FairnessEvaluation {
    /**
     * Gets the fairness score.
     *
     * @return fairness score (0.0-1.0)
     */
    double getScore();

    /**
     * Gets fairness violations.
     *
     * @return list of violations
     */
    java.util.List<FairnessViolation> getViolations();

    /**
     * Gets improvement recommendations.
     *
     * @return list of recommendations
     */
    java.util.List<String> getRecommendations();
  }

  /** Allocation statistics interface. */
  interface AllocationStatistics {
    /**
     * Gets average allocation time.
     *
     * @return average time in milliseconds
     */
    double getAverageAllocationTime();

    /**
     * Gets successful allocation rate.
     *
     * @return success rate (0.0-1.0)
     */
    double getSuccessRate();

    /**
     * Gets resource utilization.
     *
     * @return utilization statistics
     */
    ResourceUtilization getResourceUtilization();
  }

  /** Allocation trends interface. */
  interface AllocationTrends {
    /**
     * Gets demand trends.
     *
     * @return demand trend data
     */
    TrendData getDemandTrends();

    /**
     * Gets fairness trends.
     *
     * @return fairness trend data
     */
    TrendData getFairnessTrends();

    /**
     * Gets efficiency trends.
     *
     * @return efficiency trend data
     */
    TrendData getEfficiencyTrends();
  }

  /** Fairness violation interface. */
  interface FairnessViolation {
    /**
     * Gets the violation type.
     *
     * @return violation type
     */
    ViolationType getType();

    /**
     * Gets the affected request ID.
     *
     * @return request ID
     */
    String getRequestId();

    /**
     * Gets the violation severity.
     *
     * @return severity
     */
    ViolationSeverity getSeverity();

    /**
     * Gets the violation description.
     *
     * @return description
     */
    String getDescription();
  }

  /** Resource utilization interface. */
  interface ResourceUtilization {
    /**
     * Gets CPU utilization.
     *
     * @return CPU utilization (0.0-1.0)
     */
    double getCpuUtilization();

    /**
     * Gets memory utilization.
     *
     * @return memory utilization (0.0-1.0)
     */
    double getMemoryUtilization();

    /**
     * Gets overall utilization.
     *
     * @return overall utilization (0.0-1.0)
     */
    double getOverallUtilization();
  }

  /** Trend data interface. */
  interface TrendData {
    /**
     * Gets data points.
     *
     * @return list of data points
     */
    java.util.List<DataPoint> getDataPoints();

    /**
     * Gets trend slope.
     *
     * @return trend slope
     */
    double getSlope();

    /**
     * Gets correlation coefficient.
     *
     * @return correlation coefficient
     */
    double getCorrelationCoefficient();
  }

  /** Data point interface. */
  interface DataPoint {
    /**
     * Gets timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets value.
     *
     * @return value
     */
    double getValue();
  }

  /** Violation type enumeration. */
  enum ViolationType {
    /** Unfair resource distribution. */
    UNFAIR_DISTRIBUTION,
    /** Starvation detected. */
    STARVATION,
    /** Priority inversion. */
    PRIORITY_INVERSION,
    /** Resource hogging. */
    RESOURCE_HOGGING
  }

  /** Violation severity enumeration. */
  enum ViolationSeverity {
    /** Low severity. */
    LOW,
    /** Medium severity. */
    MEDIUM,
    /** High severity. */
    HIGH,
    /** Critical severity. */
    CRITICAL
  }
}
