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

import java.util.Optional;

/**
 * Context information provided to fuel exhaustion callbacks.
 *
 * <p>This class contains details about the fuel exhaustion event, including how much fuel was
 * consumed, the initial fuel amount, and how many times fuel has been exhausted during the current
 * execution.
 *
 * @since 1.0.0
 */
public final class FuelExhaustionContext {

  private final long storeId;
  private final long fuelConsumed;
  private final long initialFuel;
  private final int exhaustionCount;
  private final String functionName;

  /**
   * Creates a new fuel exhaustion context.
   *
   * @param storeId the store ID where fuel was exhausted
   * @param fuelConsumed the amount of fuel consumed before exhaustion
   * @param initialFuel the initial fuel amount when execution started
   * @param exhaustionCount the number of times fuel has been exhausted in this execution
   * @param functionName the optional function name that was executing (may be null)
   */
  public FuelExhaustionContext(
      final long storeId,
      final long fuelConsumed,
      final long initialFuel,
      final int exhaustionCount,
      final String functionName) {
    this.storeId = storeId;
    this.fuelConsumed = fuelConsumed;
    this.initialFuel = initialFuel;
    this.exhaustionCount = exhaustionCount;
    this.functionName = functionName;
  }

  /**
   * Gets the store ID where fuel was exhausted.
   *
   * @return the store ID
   */
  public long getStoreId() {
    return storeId;
  }

  /**
   * Gets the amount of fuel consumed before exhaustion.
   *
   * @return the fuel consumed
   */
  public long getFuelConsumed() {
    return fuelConsumed;
  }

  /**
   * Gets the initial fuel amount when execution started.
   *
   * @return the initial fuel
   */
  public long getInitialFuel() {
    return initialFuel;
  }

  /**
   * Gets the number of times fuel has been exhausted in this execution.
   *
   * @return the exhaustion count
   */
  public int getExhaustionCount() {
    return exhaustionCount;
  }

  /**
   * Gets the function name that was executing when fuel ran out.
   *
   * @return an Optional containing the function name, or empty if not available
   */
  public Optional<String> getFunctionName() {
    return Optional.ofNullable(functionName);
  }

  @Override
  public String toString() {
    return "FuelExhaustionContext{"
        + "storeId="
        + storeId
        + ", fuelConsumed="
        + fuelConsumed
        + ", initialFuel="
        + initialFuel
        + ", exhaustionCount="
        + exhaustionCount
        + ", functionName='"
        + functionName
        + '\''
        + '}';
  }
}
