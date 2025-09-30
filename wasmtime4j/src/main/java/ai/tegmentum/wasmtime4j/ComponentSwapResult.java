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

package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result of a component hot-swap operation.
 *
 * <p>This class provides detailed information about the outcome of a hot-swap operation, including
 * timing information, any issues encountered, and state preservation details.
 *
 * @since 1.0.0
 */
public final class ComponentSwapResult {

  private final SwapStatus status;
  private final ComponentSimple oldComponent;
  private final ComponentSimple newComponent;
  private final Instant startTime;
  private final Instant endTime;
  private final Duration totalTime;
  private final List<String> warnings;
  private final Optional<Exception> error;
  private final Map<String, Object> metrics;
  private final StatePreservationResult statePreservation;
  private final boolean rollbackPerformed;
  private final Optional<ComponentSimple> rollbackComponent;

  private ComponentSwapResult(Builder builder) {
    this.status = builder.status;
    this.oldComponent = builder.oldComponent;
    this.newComponent = builder.newComponent;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.totalTime = Duration.between(startTime, endTime);
    this.warnings = List.copyOf(builder.warnings);
    this.error = builder.error;
    this.metrics = Map.copyOf(builder.metrics);
    this.statePreservation = builder.statePreservation;
    this.rollbackPerformed = builder.rollbackPerformed;
    this.rollbackComponent = builder.rollbackComponent;
  }

  /**
   * Gets the swap status.
   *
   * @return the swap status
   */
  public SwapStatus getStatus() {
    return status;
  }

  /**
   * Gets the old component that was replaced.
   *
   * @return the old component
   */
  public ComponentSimple getOldComponent() {
    return oldComponent;
  }

  /**
   * Gets the new component that was swapped in.
   *
   * @return the new component
   */
  public ComponentSimple getNewComponent() {
    return newComponent;
  }

  /**
   * Gets the swap start time.
   *
   * @return the start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the swap end time.
   *
   * @return the end time
   */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Gets the total swap time.
   *
   * @return the total swap time
   */
  public Duration getTotalTime() {
    return totalTime;
  }

  /**
   * Gets any warnings generated during the swap.
   *
   * @return list of warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the error that caused swap failure, if any.
   *
   * @return the error, if swap failed
   */
  public Optional<Exception> getError() {
    return error;
  }

  /**
   * Gets the swap metrics.
   *
   * @return map of swap metrics
   */
  public Map<String, Object> getMetrics() {
    return metrics;
  }

  /**
   * Gets the state preservation result.
   *
   * @return the state preservation result
   */
  public StatePreservationResult getStatePreservation() {
    return statePreservation;
  }

  /**
   * Checks if a rollback was performed.
   *
   * @return true if rollback was performed
   */
  public boolean isRollbackPerformed() {
    return rollbackPerformed;
  }

  /**
   * Gets the component that was rolled back to, if any.
   *
   * @return the rollback component, if rollback occurred
   */
  public Optional<ComponentSimple> getRollbackComponent() {
    return rollbackComponent;
  }

  /**
   * Checks if the swap was successful.
   *
   * @return true if swap was successful
   */
  public boolean isSuccessful() {
    return status == SwapStatus.SUCCESS || status == SwapStatus.SUCCESS_WITH_WARNINGS;
  }

  /**
   * Checks if the swap failed.
   *
   * @return true if swap failed
   */
  public boolean isFailed() {
    return status == SwapStatus.FAILED || status == SwapStatus.ROLLED_BACK;
  }

  /**
   * Gets a human-readable summary of the swap result.
   *
   * @return swap result summary
   */
  public String getSummary() {
    StringBuilder summary = new StringBuilder();
    summary
        .append("Component swap ")
        .append(status.toString().toLowerCase())
        .append(" in ")
        .append(totalTime.toMillis())
        .append("ms");

    if (!warnings.isEmpty()) {
      summary.append(" with ").append(warnings.size()).append(" warning(s)");
    }

    if (rollbackPerformed) {
      summary.append(" (rollback performed)");
    }

    return summary.toString();
  }

  /**
   * Creates a new builder for ComponentSwapResult.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a successful swap result.
   *
   * @param oldComponent the old component
   * @param newComponent the new component
   * @param startTime the start time
   * @param endTime the end time
   * @return successful swap result
   */
  public static ComponentSwapResult success(
      ComponentSimple oldComponent,
      ComponentSimple newComponent,
      Instant startTime,
      Instant endTime) {
    return builder()
        .status(SwapStatus.SUCCESS)
        .oldComponent(oldComponent)
        .newComponent(newComponent)
        .startTime(startTime)
        .endTime(endTime)
        .statePreservation(StatePreservationResult.success())
        .build();
  }

  /**
   * Creates a failed swap result.
   *
   * @param oldComponent the old component
   * @param newComponent the new component
   * @param startTime the start time
   * @param endTime the end time
   * @param error the error that caused failure
   * @return failed swap result
   */
  public static ComponentSwapResult failure(
      ComponentSimple oldComponent,
      ComponentSimple newComponent,
      Instant startTime,
      Instant endTime,
      Exception error) {
    return builder()
        .status(SwapStatus.FAILED)
        .oldComponent(oldComponent)
        .newComponent(newComponent)
        .startTime(startTime)
        .endTime(endTime)
        .error(error)
        .statePreservation(StatePreservationResult.failed("Swap failed"))
        .build();
  }

  /** Builder for ComponentSwapResult. */
  public static final class Builder {
    private SwapStatus status;
    private ComponentSimple oldComponent;
    private ComponentSimple newComponent;
    private Instant startTime;
    private Instant endTime;
    private List<String> warnings = List.of();
    private Optional<Exception> error = Optional.empty();
    private Map<String, Object> metrics = Map.of();
    private StatePreservationResult statePreservation = StatePreservationResult.success();
    private boolean rollbackPerformed = false;
    private Optional<ComponentSimple> rollbackComponent = Optional.empty();

    private Builder() {}

    public Builder status(SwapStatus status) {
      this.status = status;
      return this;
    }

    public Builder oldComponent(ComponentSimple oldComponent) {
      this.oldComponent = oldComponent;
      return this;
    }

    public Builder newComponent(ComponentSimple newComponent) {
      this.newComponent = newComponent;
      return this;
    }

    public Builder startTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder endTime(Instant endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder warnings(List<String> warnings) {
      this.warnings = warnings;
      return this;
    }

    public Builder error(Exception error) {
      this.error = Optional.of(error);
      return this;
    }

    public Builder metrics(Map<String, Object> metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder statePreservation(StatePreservationResult statePreservation) {
      this.statePreservation = statePreservation;
      return this;
    }

    public Builder rollbackPerformed(boolean rollbackPerformed) {
      this.rollbackPerformed = rollbackPerformed;
      return this;
    }

    public Builder rollbackComponent(ComponentSimple rollbackComponent) {
      this.rollbackComponent = Optional.of(rollbackComponent);
      return this;
    }

    public ComponentSwapResult build() {
      return new ComponentSwapResult(this);
    }
  }

  /** Hot-swap status values. */
  public enum SwapStatus {
    /** Swap completed successfully. */
    SUCCESS,
    /** Swap completed with warnings. */
    SUCCESS_WITH_WARNINGS,
    /** Swap failed. */
    FAILED,
    /** Swap was cancelled. */
    CANCELLED,
    /** Swap was rolled back due to issues. */
    ROLLED_BACK,
    /** Swap is in progress. */
    IN_PROGRESS
  }

  /** Result of state preservation during swap. */
  public static final class StatePreservationResult {
    private final boolean successful;
    private final String message;
    private final Map<String, Object> preservedState;

    private StatePreservationResult(
        boolean successful, String message, Map<String, Object> preservedState) {
      this.successful = successful;
      this.message = message;
      this.preservedState = Map.copyOf(preservedState);
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getMessage() {
      return message;
    }

    public Map<String, Object> getPreservedState() {
      return preservedState;
    }

    public static StatePreservationResult success() {
      return new StatePreservationResult(true, "State preserved successfully", Map.of());
    }

    public static StatePreservationResult success(Map<String, Object> preservedState) {
      return new StatePreservationResult(true, "State preserved successfully", preservedState);
    }

    public static StatePreservationResult failed(String message) {
      return new StatePreservationResult(false, message, Map.of());
    }
  }
}
