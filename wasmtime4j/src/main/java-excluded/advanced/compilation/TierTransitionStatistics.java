package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for compilation tier transitions.
 *
 * <p>Tracks metrics about when and how functions transition between
 * different compilation tiers, success rates, and performance impacts.
 *
 * @since 1.0.0
 */
public final class TierTransitionStatistics {

  private final Map<CompilationTier, AtomicLong> tierCounts;
  private final Map<TierTransition, AtomicLong> transitionCounts;
  private final AtomicLong totalTransitions;
  private final AtomicLong successfulTransitions;
  private final AtomicLong failedTransitions;
  private final List<TransitionEvent> recentTransitions;
  private final AtomicLong totalCompilationTimeMs;

  public TierTransitionStatistics() {
    this.tierCounts = new EnumMap<>(CompilationTier.class);
    this.transitionCounts = new EnumMap<>(TierTransition.class);
    this.totalTransitions = new AtomicLong(0);
    this.successfulTransitions = new AtomicLong(0);
    this.failedTransitions = new AtomicLong(0);
    this.recentTransitions = Collections.synchronizedList(new ArrayList<>());
    this.totalCompilationTimeMs = new AtomicLong(0);

    // Initialize counters
    for (final CompilationTier tier : CompilationTier.values()) {
      tierCounts.put(tier, new AtomicLong(0));
    }
    for (final TierTransition transition : TierTransition.values()) {
      transitionCounts.put(transition, new AtomicLong(0));
    }
  }

  /**
   * Records a successful tier transition.
   *
   * @param functionName the function that transitioned
   * @param fromTier the source tier
   * @param toTier the target tier
   * @param compilationTimeMs the time taken for compilation
   */
  public void recordSuccessfulTransition(final String functionName,
                                         final CompilationTier fromTier,
                                         final CompilationTier toTier,
                                         final long compilationTimeMs) {
    totalTransitions.incrementAndGet();
    successfulTransitions.incrementAndGet();
    totalCompilationTimeMs.addAndGet(compilationTimeMs);

    tierCounts.get(toTier).incrementAndGet();
    final TierTransition transition = TierTransition.of(fromTier, toTier);
    if (transition != null) {
      transitionCounts.get(transition).incrementAndGet();
    }

    recordTransitionEvent(functionName, fromTier, toTier, true, compilationTimeMs, null);
  }

  /**
   * Records a failed tier transition.
   *
   * @param functionName the function that failed to transition
   * @param fromTier the source tier
   * @param toTier the target tier
   * @param compilationTimeMs the time spent before failure
   * @param errorMessage the error that occurred
   */
  public void recordFailedTransition(final String functionName,
                                     final CompilationTier fromTier,
                                     final CompilationTier toTier,
                                     final long compilationTimeMs,
                                     final String errorMessage) {
    totalTransitions.incrementAndGet();
    failedTransitions.incrementAndGet();
    totalCompilationTimeMs.addAndGet(compilationTimeMs);

    recordTransitionEvent(functionName, fromTier, toTier, false, compilationTimeMs, errorMessage);
  }

  /**
   * Gets the total number of transitions attempted.
   *
   * @return total transition count
   */
  public long getTotalTransitions() {
    return totalTransitions.get();
  }

  /**
   * Gets the number of successful transitions.
   *
   * @return successful transition count
   */
  public long getSuccessfulTransitions() {
    return successfulTransitions.get();
  }

  /**
   * Gets the number of failed transitions.
   *
   * @return failed transition count
   */
  public long getFailedTransitions() {
    return failedTransitions.get();
  }

  /**
   * Gets the success rate for transitions.
   *
   * @return success rate (0.0 to 1.0)
   */
  public double getSuccessRate() {
    final long total = totalTransitions.get();
    return total == 0 ? 0.0 : (double) successfulTransitions.get() / total;
  }

  /**
   * Gets the count of functions currently in each tier.
   *
   * @param tier the compilation tier
   * @return count of functions in the tier
   */
  public long getTierCount(final CompilationTier tier) {
    return tierCounts.get(tier).get();
  }

  /**
   * Gets the count of specific tier transitions.
   *
   * @param transition the tier transition type
   * @return count of this type of transition
   */
  public long getTransitionCount(final TierTransition transition) {
    return transitionCounts.get(transition).get();
  }

  /**
   * Gets the average compilation time.
   *
   * @return average compilation time
   */
  public Duration getAverageCompilationTime() {
    final long total = totalTransitions.get();
    if (total == 0) {
      return Duration.ZERO;
    }
    return Duration.ofMillis(totalCompilationTimeMs.get() / total);
  }

  /**
   * Gets recent transition events.
   *
   * @param limit maximum number of events to return
   * @return list of recent transition events
   */
  public List<TransitionEvent> getRecentTransitions(final int limit) {
    synchronized (recentTransitions) {
      final int size = recentTransitions.size();
      final int fromIndex = Math.max(0, size - limit);
      return new ArrayList<>(recentTransitions.subList(fromIndex, size));
    }
  }

  /**
   * Resets all statistics.
   */
  public void reset() {
    totalTransitions.set(0);
    successfulTransitions.set(0);
    failedTransitions.set(0);
    totalCompilationTimeMs.set(0);

    for (final AtomicLong count : tierCounts.values()) {
      count.set(0);
    }
    for (final AtomicLong count : transitionCounts.values()) {
      count.set(0);
    }

    synchronized (recentTransitions) {
      recentTransitions.clear();
    }
  }

  private void recordTransitionEvent(final String functionName,
                                     final CompilationTier fromTier,
                                     final CompilationTier toTier,
                                     final boolean successful,
                                     final long compilationTimeMs,
                                     final String errorMessage) {
    final TransitionEvent event = new TransitionEvent(
        functionName, fromTier, toTier, successful, compilationTimeMs, errorMessage, Instant.now());

    synchronized (recentTransitions) {
      recentTransitions.add(event);
      // Keep only recent events (last 1000)
      if (recentTransitions.size() > 1000) {
        recentTransitions.remove(0);
      }
    }
  }

  /**
   * Represents a tier transition event.
   */
  public static final class TransitionEvent {
    private final String functionName;
    private final CompilationTier fromTier;
    private final CompilationTier toTier;
    private final boolean successful;
    private final long compilationTimeMs;
    private final String errorMessage;
    private final Instant timestamp;

    public TransitionEvent(final String functionName,
                           final CompilationTier fromTier,
                           final CompilationTier toTier,
                           final boolean successful,
                           final long compilationTimeMs,
                           final String errorMessage,
                           final Instant timestamp) {
      this.functionName = functionName;
      this.fromTier = fromTier;
      this.toTier = toTier;
      this.successful = successful;
      this.compilationTimeMs = compilationTimeMs;
      this.errorMessage = errorMessage;
      this.timestamp = timestamp;
    }

    public String getFunctionName() { return functionName; }
    public CompilationTier getFromTier() { return fromTier; }
    public CompilationTier getToTier() { return toTier; }
    public boolean isSuccessful() { return successful; }
    public long getCompilationTimeMs() { return compilationTimeMs; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getTimestamp() { return timestamp; }
  }

  /**
   * Enumeration of tier transition types.
   */
  public enum TierTransition {
    BASELINE_TO_OPTIMIZED,
    BASELINE_TO_HIGHLY_OPTIMIZED,
    OPTIMIZED_TO_HIGHLY_OPTIMIZED;

    public static TierTransition of(final CompilationTier from, final CompilationTier to) {
      if (from == CompilationTier.BASELINE && to == CompilationTier.OPTIMIZED) {
        return BASELINE_TO_OPTIMIZED;
      } else if (from == CompilationTier.BASELINE && to == CompilationTier.HIGHLY_OPTIMIZED) {
        return BASELINE_TO_HIGHLY_OPTIMIZED;
      } else if (from == CompilationTier.OPTIMIZED && to == CompilationTier.HIGHLY_OPTIMIZED) {
        return OPTIMIZED_TO_HIGHLY_OPTIMIZED;
      }
      return null; // No valid transition
    }
  }
}