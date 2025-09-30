package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution priority levels for fuel allocation and consumption.
 *
 * <p>Priority levels determine fuel allocation order, consumption rates,
 * and interruption behavior in resource-constrained environments.
 *
 * @since 1.0.0
 */
public enum FuelPriority {

  /**
   * Critical priority for system-essential operations.
   *
   * <p>Critical priority contexts:
   * <ul>
   *   <li>Receive fuel allocation before all other priorities</li>
   *   <li>Are resistant to fuel throttling</li>
   *   <li>Can preempt lower priority executions</li>
   *   <li>Have the lowest fuel consumption overhead</li>
   * </ul>
   */
  CRITICAL(4, 0.5, true, 1.0),

  /**
   * High priority for important user-facing operations.
   *
   * <p>High priority contexts:
   * <ul>
   *   <li>Receive fuel allocation after critical but before normal</li>
   *   <li>Have reduced fuel consumption overhead</li>
   *   <li>Can interrupt normal and low priority executions</li>
   *   <li>Get priority in fuel budget allocations</li>
   * </ul>
   */
  HIGH(3, 0.7, true, 0.9),

  /**
   * Normal priority for standard operations.
   *
   * <p>Normal priority contexts:
   * <ul>
   *   <li>Receive standard fuel allocation</li>
   *   <li>Have standard fuel consumption rates</li>
   *   <li>Can be preempted by higher priority contexts</li>
   *   <li>Use default fuel management policies</li>
   * </ul>
   */
  NORMAL(2, 1.0, false, 0.8),

  /**
   * Low priority for background and batch operations.
   *
   * <p>Low priority contexts:
   * <ul>
   *   <li>Receive fuel allocation after all higher priorities</li>
   *   <li>Have higher fuel consumption overhead</li>
   *   <li>Can be throttled or suspended when fuel is scarce</li>
   *   <li>Use opportunistic execution scheduling</li>
   * </ul>
   */
  LOW(1, 1.5, false, 0.6),

  /**
   * Background priority for non-urgent operations.
   *
   * <p>Background priority contexts:
   * <ul>
   *   <li>Receive fuel only when no higher priority contexts need it</li>
   *   <li>Have the highest fuel consumption overhead</li>
   *   <li>Can be paused indefinitely when resources are needed</li>
   *   <li>Use best-effort execution with minimal guarantees</li>
   * </ul>
   */
  BACKGROUND(0, 2.0, false, 0.4);

  private final int level;
  private final double consumptionMultiplier;
  private final boolean canPreempt;
  private final double allocationShare;

  /**
   * Constructs a fuel priority level.
   *
   * @param level numeric priority level (higher is more important)
   * @param consumptionMultiplier fuel consumption rate multiplier
   * @param canPreempt whether this priority can preempt lower priorities
   * @param allocationShare relative share of fuel budget allocations (0.0-1.0)
   */
  FuelPriority(int level, double consumptionMultiplier, boolean canPreempt, double allocationShare) {
    this.level = level;
    this.consumptionMultiplier = consumptionMultiplier;
    this.canPreempt = canPreempt;
    this.allocationShare = allocationShare;
  }

  /**
   * Gets the numeric priority level.
   *
   * @return priority level (higher values indicate higher priority)
   */
  public int getLevel() {
    return level;
  }

  /**
   * Gets the fuel consumption rate multiplier for this priority.
   *
   * @return consumption multiplier (1.0 is standard rate, <1.0 is reduced, >1.0 is increased)
   */
  public double getConsumptionMultiplier() {
    return consumptionMultiplier;
  }

  /**
   * Checks if this priority level can preempt lower priority executions.
   *
   * @return true if can preempt, false otherwise
   */
  public boolean canPreempt() {
    return canPreempt;
  }

  /**
   * Gets the relative allocation share for fuel budget distributions.
   *
   * @return allocation share as a ratio (0.0-1.0)
   */
  public double getAllocationShare() {
    return allocationShare;
  }

  /**
   * Compares this priority with another for ordering.
   *
   * @param other priority to compare with
   * @return negative if this priority is lower, positive if higher, zero if equal
   */
  public int compareTo(FuelPriority other) {
    return Integer.compare(this.level, other.level);
  }

  /**
   * Checks if this priority is higher than another priority.
   *
   * @param other priority to compare with
   * @return true if this priority is higher
   * @throws IllegalArgumentException if other is null
   */
  public boolean isHigherThan(final FuelPriority other) {
    if (other == null) {
      throw new IllegalArgumentException("Priority cannot be null");
    }
    return this.level > other.level;
  }

  /**
   * Checks if this priority is lower than another priority.
   *
   * @param other priority to compare with
   * @return true if this priority is lower
   * @throws IllegalArgumentException if other is null
   */
  public boolean isLowerThan(final FuelPriority other) {
    if (other == null) {
      throw new IllegalArgumentException("Priority cannot be null");
    }
    return this.level < other.level;
  }

  /**
   * Gets the effective fuel amount after applying priority-based multipliers.
   *
   * @param baseFuelAmount base fuel amount before priority adjustments
   * @return effective fuel amount after priority-based scaling
   * @throws IllegalArgumentException if baseFuelAmount is negative
   */
  public long getEffectiveFuelAmount(final long baseFuelAmount) {
    if (baseFuelAmount < 0) {
      throw new IllegalArgumentException("Base fuel amount cannot be negative");
    }
    return Math.round(baseFuelAmount * allocationShare);
  }

  /**
   * Calculates fuel consumption cost with priority-based overhead.
   *
   * @param baseCost base fuel consumption cost
   * @return actual fuel cost after applying priority multipliers
   * @throws IllegalArgumentException if baseCost is negative
   */
  public long calculateConsumptionCost(final long baseCost) {
    if (baseCost < 0) {
      throw new IllegalArgumentException("Base cost cannot be negative");
    }
    return Math.round(baseCost * consumptionMultiplier);
  }

  /**
   * Creates a priority from a numeric level.
   *
   * @param level numeric priority level
   * @return corresponding FuelPriority enum value
   * @throws IllegalArgumentException if level is not valid
   */
  public static FuelPriority fromLevel(final int level) {
    for (FuelPriority priority : values()) {
      if (priority.level == level) {
        return priority;
      }
    }
    throw new IllegalArgumentException("Invalid priority level: " + level);
  }

  /**
   * Gets the default priority for general use.
   *
   * @return default fuel priority (NORMAL)
   */
  public static FuelPriority getDefault() {
    return NORMAL;
  }

  /**
   * Gets all priority levels ordered from highest to lowest.
   *
   * @return array of priorities in descending priority order
   */
  public static FuelPriority[] getOrderedPriorities() {
    return new FuelPriority[]{CRITICAL, HIGH, NORMAL, LOW, BACKGROUND};
  }
}