package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;

/**
 * Represents a tier transition decision with reasoning and timing.
 *
 * <p>This class encapsulates the decision logic for whether a WebAssembly function
 * should transition between compilation tiers, including the target tier, priority,
 * expected benefits, and timing constraints.
 *
 * @since 1.0.0
 */
public final class TierTransitionDecision {

  private final TransitionType transitionType;
  private final JitCompilationStrategy targetStrategy;
  private final String reason;
  private final double estimatedBenefit;
  private final CompilationPriority priority;
  private final Duration delay;

  /**
   * Tier transition types.
   */
  public enum TransitionType {
    /** No transition should occur. */
    NO_TRANSITION,
    /** Transition should occur immediately. */
    TRANSITION,
    /** Transition should be delayed. */
    DELAY
  }

  /**
   * Compilation priority levels for scheduling.
   */
  public enum CompilationPriority {
    LOW(1),
    MEDIUM(5),
    HIGH(10),
    CRITICAL(20);

    private final int weight;

    CompilationPriority(final int weight) {
      this.weight = weight;
    }

    public int getWeight() {
      return weight;
    }
  }

  private TierTransitionDecision(final TransitionType transitionType,
                                 final JitCompilationStrategy targetStrategy,
                                 final String reason,
                                 final double estimatedBenefit,
                                 final CompilationPriority priority,
                                 final Duration delay) {
    this.transitionType = transitionType;
    this.targetStrategy = targetStrategy;
    this.reason = reason;
    this.estimatedBenefit = estimatedBenefit;
    this.priority = priority;
    this.delay = delay;
  }

  /**
   * Creates a decision for no transition.
   *
   * @param reason reason for not transitioning
   * @return no transition decision
   */
  public static TierTransitionDecision noTransition(final String reason) {
    return new TierTransitionDecision(
        TransitionType.NO_TRANSITION,
        null,
        reason,
        0.0,
        CompilationPriority.LOW,
        Duration.ZERO
    );
  }

  /**
   * Creates a decision to transition to a target strategy.
   *
   * @param targetStrategy target compilation strategy
   * @param estimatedBenefit estimated performance benefit (0.0 to 1.0)
   * @return transition decision
   */
  public static TierTransitionDecision transition(final JitCompilationStrategy targetStrategy,
                                                  final double estimatedBenefit) {
    final CompilationPriority priority = determinePriority(targetStrategy, estimatedBenefit);
    return new TierTransitionDecision(
        TransitionType.TRANSITION,
        targetStrategy,
        "Beneficial tier transition",
        estimatedBenefit,
        priority,
        Duration.ZERO
    );
  }

  /**
   * Creates a decision to transition with specific priority.
   *
   * @param targetStrategy target compilation strategy
   * @param estimatedBenefit estimated performance benefit (0.0 to 1.0)
   * @param priority compilation priority
   * @return transition decision
   */
  public static TierTransitionDecision transition(final JitCompilationStrategy targetStrategy,
                                                  final double estimatedBenefit,
                                                  final CompilationPriority priority) {
    return new TierTransitionDecision(
        TransitionType.TRANSITION,
        targetStrategy,
        "Beneficial tier transition",
        estimatedBenefit,
        priority,
        Duration.ZERO
    );
  }

  /**
   * Creates a decision to delay transition.
   *
   * @param reason reason for delaying
   * @param delay delay duration
   * @return delay decision
   */
  public static TierTransitionDecision delay(final String reason, final Duration delay) {
    return new TierTransitionDecision(
        TransitionType.DELAY,
        null,
        reason,
        0.0,
        CompilationPriority.LOW,
        delay
    );
  }

  /**
   * Determines compilation priority based on target strategy and estimated benefit.
   */
  private static CompilationPriority determinePriority(final JitCompilationStrategy targetStrategy,
                                                       final double estimatedBenefit) {
    // High benefit transitions get high priority
    if (estimatedBenefit > 0.5) {
      return CompilationPriority.CRITICAL;
    }

    // Transitions to highly optimized tier get high priority
    if (targetStrategy == JitCompilationStrategy.HIGHLY_OPTIMIZED) {
      return CompilationPriority.HIGH;
    }

    // Significant benefit gets medium priority
    if (estimatedBenefit > 0.2) {
      return CompilationPriority.MEDIUM;
    }

    // Default to low priority
    return CompilationPriority.LOW;
  }

  /**
   * Determines if a transition should occur.
   *
   * @return true if transition should happen
   */
  public boolean shouldTransition() {
    return transitionType == TransitionType.TRANSITION;
  }

  /**
   * Determines if the transition should be delayed.
   *
   * @return true if transition should be delayed
   */
  public boolean shouldDelay() {
    return transitionType == TransitionType.DELAY;
  }

  /**
   * Gets the transition type.
   *
   * @return transition type
   */
  public TransitionType getTransitionType() {
    return transitionType;
  }

  /**
   * Gets the target compilation strategy.
   *
   * @return target strategy or null if no transition
   */
  public JitCompilationStrategy getTargetStrategy() {
    return targetStrategy;
  }

  /**
   * Gets the reason for the decision.
   *
   * @return decision reason
   */
  public String getReason() {
    return reason;
  }

  /**
   * Gets the estimated performance benefit.
   *
   * @return estimated benefit (0.0 to 1.0)
   */
  public double getEstimatedBenefit() {
    return estimatedBenefit;
  }

  /**
   * Gets the compilation priority.
   *
   * @return compilation priority
   */
  public CompilationPriority getPriority() {
    return priority;
  }

  /**
   * Gets the delay duration for delayed transitions.
   *
   * @return delay duration
   */
  public Duration getDelay() {
    return delay;
  }

  /**
   * Creates a modified decision with different priority.
   *
   * @param newPriority new compilation priority
   * @return decision with updated priority
   */
  public TierTransitionDecision withPriority(final CompilationPriority newPriority) {
    return new TierTransitionDecision(
        transitionType,
        targetStrategy,
        reason,
        estimatedBenefit,
        newPriority,
        delay
    );
  }

  /**
   * Creates a modified decision with additional delay.
   *
   * @param additionalDelay additional delay to add
   * @return decision with updated delay
   */
  public TierTransitionDecision withAdditionalDelay(final Duration additionalDelay) {
    final Duration newDelay = delay.plus(additionalDelay);
    return new TierTransitionDecision(
        TransitionType.DELAY,
        targetStrategy,
        reason,
        estimatedBenefit,
        priority,
        newDelay
    );
  }

  /**
   * Determines if this decision represents a beneficial transition.
   *
   * @return true if transition is beneficial
   */
  public boolean isBeneficial() {
    return shouldTransition() && estimatedBenefit > 0.0;
  }

  /**
   * Determines if this decision represents a high-priority transition.
   *
   * @return true if high priority
   */
  public boolean isHighPriority() {
    return priority.getWeight() >= CompilationPriority.HIGH.getWeight();
  }

  /**
   * Compares transition decisions by priority and benefit.
   *
   * @param other other decision
   * @return comparison result
   */
  public int comparePriority(final TierTransitionDecision other) {
    // Compare by priority weight first
    final int priorityComparison = Integer.compare(
        other.priority.getWeight(), this.priority.getWeight());

    if (priorityComparison != 0) {
      return priorityComparison;
    }

    // Then by estimated benefit
    return Double.compare(other.estimatedBenefit, this.estimatedBenefit);
  }

  @Override
  public String toString() {
    switch (transitionType) {
      case NO_TRANSITION:
        return String.format("NoTransition{reason='%s'}", reason);

      case TRANSITION:
        return String.format("Transition{target=%s, benefit=%.2f%%, priority=%s}",
                           targetStrategy.getName(),
                           estimatedBenefit * 100,
                           priority);

      case DELAY:
        return String.format("Delay{reason='%s', delay=%s}",
                           reason, delay);

      default:
        return "UnknownTransition";
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    final TierTransitionDecision that = (TierTransitionDecision) obj;
    return Double.compare(that.estimatedBenefit, estimatedBenefit) == 0 &&
           transitionType == that.transitionType &&
           targetStrategy == that.targetStrategy &&
           priority == that.priority &&
           reason.equals(that.reason) &&
           delay.equals(that.delay);
  }

  @Override
  public int hashCode() {
    int result = transitionType.hashCode();
    result = 31 * result + (targetStrategy != null ? targetStrategy.hashCode() : 0);
    result = 31 * result + reason.hashCode();
    result = 31 * result + Double.hashCode(estimatedBenefit);
    result = 31 * result + priority.hashCode();
    result = 31 * result + delay.hashCode();
    return result;
  }
}