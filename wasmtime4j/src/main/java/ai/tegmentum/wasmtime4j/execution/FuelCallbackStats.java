/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.execution;

/**
 * Statistics for fuel callback handler events.
 *
 * <p>This class contains metrics about how many times fuel was exhausted, how much fuel was added,
 * and the outcomes of fuel exhaustion events (continued, trapped, or paused).
 *
 * @since 1.0.0
 */
public final class FuelCallbackStats {

  private final long exhaustionEvents;
  private final long totalFuelAdded;
  private final long continuedCount;
  private final long trappedCount;
  private final long pausedCount;

  /**
   * Creates a new fuel callback statistics instance.
   *
   * @param exhaustionEvents the total number of fuel exhaustion events
   * @param totalFuelAdded the total amount of fuel added via callbacks
   * @param continuedCount the number of times execution continued after exhaustion
   * @param trappedCount the number of times execution was trapped
   * @param pausedCount the number of times execution was paused
   */
  public FuelCallbackStats(
      final long exhaustionEvents,
      final long totalFuelAdded,
      final long continuedCount,
      final long trappedCount,
      final long pausedCount) {
    this.exhaustionEvents = exhaustionEvents;
    this.totalFuelAdded = totalFuelAdded;
    this.continuedCount = continuedCount;
    this.trappedCount = trappedCount;
    this.pausedCount = pausedCount;
  }

  /**
   * Gets the total number of fuel exhaustion events.
   *
   * @return the exhaustion event count
   */
  public long getExhaustionEvents() {
    return exhaustionEvents;
  }

  /**
   * Gets the total amount of fuel added via callbacks.
   *
   * @return the total fuel added
   */
  public long getTotalFuelAdded() {
    return totalFuelAdded;
  }

  /**
   * Gets the number of times execution continued after exhaustion.
   *
   * @return the continued count
   */
  public long getContinuedCount() {
    return continuedCount;
  }

  /**
   * Gets the number of times execution was trapped.
   *
   * @return the trapped count
   */
  public long getTrappedCount() {
    return trappedCount;
  }

  /**
   * Gets the number of times execution was paused.
   *
   * @return the paused count
   */
  public long getPausedCount() {
    return pausedCount;
  }

  @Override
  public String toString() {
    return "FuelCallbackStats{"
        + "exhaustionEvents="
        + exhaustionEvents
        + ", totalFuelAdded="
        + totalFuelAdded
        + ", continuedCount="
        + continuedCount
        + ", trappedCount="
        + trappedCount
        + ", pausedCount="
        + pausedCount
        + '}';
  }
}
