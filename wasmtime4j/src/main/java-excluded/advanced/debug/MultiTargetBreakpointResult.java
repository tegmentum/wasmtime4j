package ai.tegmentum.wasmtime4j.debug;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of setting breakpoints across multiple debug targets.
 *
 * <p>Contains information about which targets successfully set breakpoints
 * and any errors encountered during the process.
 *
 * @since 1.0.0
 */
public final class MultiTargetBreakpointResult {

  private final boolean overallSuccess;
  private final int totalTargets;
  private final int successfulTargets;
  private final int failedTargets;
  private final Map<String, BreakpointResult> targetResults;
  private final List<String> globalErrors;
  private final Instant operationTimestamp;

  private MultiTargetBreakpointResult(final boolean overallSuccess,
                                      final int totalTargets,
                                      final int successfulTargets,
                                      final int failedTargets,
                                      final Map<String, BreakpointResult> targetResults,
                                      final List<String> globalErrors,
                                      final Instant operationTimestamp) {
    this.overallSuccess = overallSuccess;
    this.totalTargets = totalTargets;
    this.successfulTargets = successfulTargets;
    this.failedTargets = failedTargets;
    this.targetResults = Map.copyOf(targetResults);
    this.globalErrors = List.copyOf(globalErrors);
    this.operationTimestamp = Objects.requireNonNull(operationTimestamp);
  }

  /**
   * Creates a new builder for multi-target breakpoint results.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a successful result for all targets.
   *
   * @param targetResults map of target ID to breakpoint result
   * @return successful result
   */
  public static MultiTargetBreakpointResult allSuccessful(final Map<String, BreakpointResult> targetResults) {
    return new MultiTargetBreakpointResult(true, targetResults.size(), targetResults.size(), 0,
                                           targetResults, List.of(), Instant.now());
  }

  /**
   * Creates a partial success result.
   *
   * @param targetResults map of target ID to breakpoint result
   * @param globalErrors any global errors
   * @return partial success result
   */
  public static MultiTargetBreakpointResult partialSuccess(final Map<String, BreakpointResult> targetResults,
                                                           final List<String> globalErrors) {
    final long successful = targetResults.values().stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum();
    final int successfulCount = (int) successful;
    final int failedCount = targetResults.size() - successfulCount;

    return new MultiTargetBreakpointResult(successfulCount > 0, targetResults.size(),
                                           successfulCount, failedCount, targetResults,
                                           globalErrors, Instant.now());
  }

  /**
   * Checks if the overall operation was successful.
   *
   * @return true if at least one target succeeded
   */
  public boolean isOverallSuccess() {
    return overallSuccess;
  }

  /**
   * Gets the total number of targets.
   *
   * @return total targets
   */
  public int getTotalTargets() {
    return totalTargets;
  }

  /**
   * Gets the number of successful targets.
   *
   * @return successful targets count
   */
  public int getSuccessfulTargets() {
    return successfulTargets;
  }

  /**
   * Gets the number of failed targets.
   *
   * @return failed targets count
   */
  public int getFailedTargets() {
    return failedTargets;
  }

  /**
   * Gets the results for each target.
   *
   * @return map of target ID to breakpoint result
   */
  public Map<String, BreakpointResult> getTargetResults() {
    return targetResults;
  }

  /**
   * Gets any global errors that affected all targets.
   *
   * @return list of global errors
   */
  public List<String> getGlobalErrors() {
    return globalErrors;
  }

  /**
   * Gets when the operation was performed.
   *
   * @return operation timestamp
   */
  public Instant getOperationTimestamp() {
    return operationTimestamp;
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return success rate (0-100)
   */
  public double getSuccessRate() {
    if (totalTargets == 0) return 0.0;
    return (double) successfulTargets / totalTargets * 100.0;
  }

  /**
   * Checks if there were any global errors.
   *
   * @return true if global errors exist
   */
  public boolean hasGlobalErrors() {
    return !globalErrors.isEmpty();
  }

  /**
   * Builder for multi-target breakpoint results.
   */
  public static final class Builder {
    private boolean overallSuccess = false;
    private int totalTargets = 0;
    private int successfulTargets = 0;
    private int failedTargets = 0;
    private Map<String, BreakpointResult> targetResults = Map.of();
    private List<String> globalErrors = List.of();
    private Instant operationTimestamp = Instant.now();

    public Builder overallSuccess(final boolean overallSuccess) {
      this.overallSuccess = overallSuccess;
      return this;
    }

    public Builder totalTargets(final int totalTargets) {
      this.totalTargets = totalTargets;
      return this;
    }

    public Builder successfulTargets(final int successfulTargets) {
      this.successfulTargets = successfulTargets;
      return this;
    }

    public Builder failedTargets(final int failedTargets) {
      this.failedTargets = failedTargets;
      return this;
    }

    public Builder targetResults(final Map<String, BreakpointResult> targetResults) {
      this.targetResults = Map.copyOf(Objects.requireNonNull(targetResults));
      return this;
    }

    public Builder globalErrors(final List<String> globalErrors) {
      this.globalErrors = List.copyOf(Objects.requireNonNull(globalErrors));
      return this;
    }

    public Builder operationTimestamp(final Instant operationTimestamp) {
      this.operationTimestamp = Objects.requireNonNull(operationTimestamp);
      return this;
    }

    public MultiTargetBreakpointResult build() {
      return new MultiTargetBreakpointResult(overallSuccess, totalTargets, successfulTargets,
                                             failedTargets, targetResults, globalErrors, operationTimestamp);
    }
  }

  /**
   * Result of setting a breakpoint on a single target.
   */
  public static final class BreakpointResult {
    private final boolean successful;
    private final String targetId;
    private final String breakpointId;
    private final List<String> errors;
    private final List<String> warnings;

    public BreakpointResult(final boolean successful,
                            final String targetId,
                            final String breakpointId,
                            final List<String> errors,
                            final List<String> warnings) {
      this.successful = successful;
      this.targetId = Objects.requireNonNull(targetId);
      this.breakpointId = breakpointId;
      this.errors = List.copyOf(errors);
      this.warnings = List.copyOf(warnings);
    }

    public static BreakpointResult success(final String targetId, final String breakpointId) {
      return new BreakpointResult(true, targetId, breakpointId, List.of(), List.of());
    }

    public static BreakpointResult failure(final String targetId, final List<String> errors) {
      return new BreakpointResult(false, targetId, null, errors, List.of());
    }

    public boolean isSuccessful() { return successful; }
    public String getTargetId() { return targetId; }
    public String getBreakpointId() { return breakpointId; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }

    @Override
    public String toString() {
      return String.format("BreakpointResult{target='%s', successful=%s, errors=%d}",
          targetId, successful, errors.size());
    }
  }

  @Override
  public String toString() {
    return String.format("MultiTargetBreakpointResult{success=%s, targets=%d/%d, rate=%.1f%%}",
        overallSuccess, successfulTargets, totalTargets, getSuccessRate());
  }
}