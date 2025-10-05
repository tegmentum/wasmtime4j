package ai.tegmentum.wasmtime4j.execution;

/** Represents a fuel consumption adjustment with an amount and reason. */
public final class FuelAdjustment {
  private final long amount;
  private final String reason;

  /**
   * Creates a new fuel adjustment.
   *
   * @param amount the amount of fuel to adjust
   * @param reason the reason for the adjustment
   */
  public FuelAdjustment(final long amount, final String reason) {
    this.amount = amount;
    this.reason = reason;
  }

  /**
   * Gets the adjustment amount.
   *
   * @return the amount of fuel to adjust
   */
  public long getAmount() {
    return amount;
  }

  /**
   * Gets the reason for the adjustment.
   *
   * @return the reason for the adjustment
   */
  public String getReason() {
    return reason;
  }
}
