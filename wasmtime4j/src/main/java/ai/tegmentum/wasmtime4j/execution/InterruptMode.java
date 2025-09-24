package ai.tegmentum.wasmtime4j.execution;

import java.time.Duration;

/**
 * Epoch interruption modes for WebAssembly execution control.
 *
 * <p>Defines the behavior of epoch-based interruptions, determining
 * when and how WebAssembly execution can be interrupted based on
 * epoch deadlines and system requirements.
 *
 * @since 1.0.0
 */
public enum InterruptMode {

  /**
   * Cooperative interruption mode.
   *
   * <p>In cooperative mode:
   * <ul>
   *   <li>WebAssembly code yields control voluntarily at safe points</li>
   *   <li>Interruption occurs only at function boundaries and loop backedges</li>
   *   <li>Provides predictable and safe interruption behavior</li>
   *   <li>May have longer interruption latency for tight loops</li>
   *   <li>Maintains execution state consistency</li>
   * </ul>
   *
   * <p>Best for applications requiring deterministic interruption points
   * and consistent execution state.
   */
  COOPERATIVE(false, true, Duration.ofMillis(10), "Safe interruption at yield points"),

  /**
   * Preemptive interruption mode.
   *
   * <p>In preemptive mode:
   * <ul>
   *   <li>WebAssembly execution can be interrupted at any instruction</li>
   *   <li>Provides immediate responsiveness to interrupt signals</li>
   *   <li>May interrupt execution in the middle of operations</li>
   *   <li>Requires careful state preservation and recovery</li>
   *   <li>Lower interruption latency but potentially less predictable</li>
   * </ul>
   *
   * <p>Best for real-time applications requiring immediate response
   * to interruption signals.
   */
  PREEMPTIVE(true, false, Duration.ofNanos(100), "Immediate interruption at any point"),

  /**
   * Hybrid interruption mode.
   *
   * <p>In hybrid mode:
   * <ul>
   *   <li>Combines cooperative and preemptive approaches</li>
   *   <li>Attempts cooperative interruption first</li>
   *   <li>Falls back to preemptive interruption if cooperative fails</li>
   *   <li>Balances safety with responsiveness</li>
   *   <li>Uses timeout to switch from cooperative to preemptive</li>
   * </ul>
   *
   * <p>Best for general-purpose applications needing both safety
   * and responsiveness guarantees.
   */
  HYBRID(true, true, Duration.ofMillis(5), "Cooperative first, preemptive fallback"),

  /**
   * Emergency interruption mode.
   *
   * <p>In emergency mode:
   * <ul>
   *   <li>Forces immediate interruption regardless of execution state</li>
   *   <li>Used for critical system situations and resource exhaustion</li>
   *   <li>May leave execution in inconsistent state</li>
   *   <li>Requires comprehensive recovery mechanisms</li>
   *   <li>Bypasses normal interrupt protection</li>
   * </ul>
   *
   * <p>Best for emergency shutdowns and critical resource protection.
   */
  EMERGENCY(true, false, Duration.ofNanos(1), "Immediate forced interruption"),

  /**
   * Graceful interruption mode.
   *
   * <p>In graceful mode:
   * <ul>
   *   <li>Allows WebAssembly code to complete current operations</li>
   *   <li>Waits for natural stopping points in execution</li>
   *   <li>Provides maximum execution state consistency</li>
   *   <li>May have significant interruption delay</li>
   *   <li>Suitable for batch processing and non-time-critical operations</li>
   * </ul>
   *
   * <p>Best for batch operations and scenarios where execution
   * consistency is more important than interruption speed.
   */
  GRACEFUL(false, true, Duration.ofSeconds(1), "Wait for natural completion points");

  private final boolean canPreempt;
  private final boolean respectsSafePoints;
  private final Duration typicalLatency;
  private final String description;

  /**
   * Constructs an interrupt mode with specified characteristics.
   *
   * @param canPreempt whether this mode can interrupt at any point
   * @param respectsSafePoints whether this mode waits for safe interrupt points
   * @param typicalLatency typical time between interrupt signal and actual interruption
   * @param description human-readable description of the mode
   */
  InterruptMode(boolean canPreempt, boolean respectsSafePoints, Duration typicalLatency, String description) {
    this.canPreempt = canPreempt;
    this.respectsSafePoints = respectsSafePoints;
    this.typicalLatency = typicalLatency;
    this.description = description;
  }

  /**
   * Checks if this interrupt mode can preempt execution at any point.
   *
   * @return true if preemptive interruption is possible
   */
  public boolean canPreempt() {
    return canPreempt;
  }

  /**
   * Checks if this interrupt mode respects safe interruption points.
   *
   * @return true if mode waits for safe points, false if it interrupts immediately
   */
  public boolean respectsSafePoints() {
    return respectsSafePoints;
  }

  /**
   * Gets the typical latency between interrupt signal and actual interruption.
   *
   * @return expected interruption latency duration
   */
  public Duration getTypicalLatency() {
    return typicalLatency;
  }

  /**
   * Gets a human-readable description of this interrupt mode.
   *
   * @return description of interrupt mode behavior
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this interrupt mode provides deterministic interruption timing.
   *
   * @return true if interruption timing is deterministic
   */
  public boolean isDeterministic() {
    return respectsSafePoints && !canPreempt;
  }

  /**
   * Checks if this interrupt mode is suitable for real-time applications.
   *
   * @return true if suitable for real-time use
   */
  public boolean isRealTimeSuitable() {
    return canPreempt && typicalLatency.compareTo(Duration.ofMillis(1)) <= 0;
  }

  /**
   * Compares interrupt latency with another mode.
   *
   * @param other interrupt mode to compare with
   * @return negative if this mode is faster, positive if slower, zero if equal
   * @throws IllegalArgumentException if other is null
   */
  public int compareLatency(final InterruptMode other) {
    if (other == null) {
      throw new IllegalArgumentException("Interrupt mode cannot be null");
    }
    return this.typicalLatency.compareTo(other.typicalLatency);
  }

  /**
   * Gets the interrupt mode most suitable for the given requirements.
   *
   * @param requiresRealTime true if real-time response is required
   * @param requiresDeterminism true if deterministic behavior is required
   * @param requiresSafety true if execution safety is critical
   * @return most suitable interrupt mode
   */
  public static InterruptMode selectOptimal(boolean requiresRealTime, boolean requiresDeterminism, boolean requiresSafety) {
    if (requiresRealTime && requiresDeterminism) {
      // Conflicting requirements - choose hybrid as compromise
      return HYBRID;
    } else if (requiresRealTime) {
      return PREEMPTIVE;
    } else if (requiresDeterminism || requiresSafety) {
      return COOPERATIVE;
    } else {
      return GRACEFUL;
    }
  }

  /**
   * Gets all interrupt modes ordered by latency (fastest to slowest).
   *
   * @return array of interrupt modes in latency order
   */
  public static InterruptMode[] getByLatencyOrder() {
    return new InterruptMode[]{EMERGENCY, PREEMPTIVE, HYBRID, COOPERATIVE, GRACEFUL};
  }

  /**
   * Gets the default interrupt mode for general use.
   *
   * @return default interrupt mode (COOPERATIVE)
   */
  public static InterruptMode getDefault() {
    return COOPERATIVE;
  }
}