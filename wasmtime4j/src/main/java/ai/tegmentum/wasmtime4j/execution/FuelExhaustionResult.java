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
 * Result returned from a fuel exhaustion callback.
 *
 * <p>This class specifies the action to take when fuel is exhausted and optionally the amount of
 * additional fuel to add if execution should continue.
 *
 * @since 1.0.0
 */
public final class FuelExhaustionResult {

  private final FuelExhaustionAction action;
  private final long additionalFuel;

  private FuelExhaustionResult(final FuelExhaustionAction action, final long additionalFuel) {
    this.action = action;
    this.additionalFuel = additionalFuel;
  }

  /**
   * Creates a result that continues execution with the specified amount of additional fuel.
   *
   * @param additionalFuel the amount of fuel to add
   * @return a result that continues execution
   * @throws IllegalArgumentException if additionalFuel is negative
   */
  public static FuelExhaustionResult continueWith(final long additionalFuel) {
    if (additionalFuel < 0) {
      throw new IllegalArgumentException("Additional fuel cannot be negative: " + additionalFuel);
    }
    return new FuelExhaustionResult(FuelExhaustionAction.CONTINUE, additionalFuel);
  }

  /**
   * Creates a result that traps execution.
   *
   * @return a result that traps execution
   */
  public static FuelExhaustionResult trap() {
    return new FuelExhaustionResult(FuelExhaustionAction.TRAP, 0);
  }

  /**
   * Creates a result that pauses execution.
   *
   * @return a result that pauses execution
   */
  public static FuelExhaustionResult pause() {
    return new FuelExhaustionResult(FuelExhaustionAction.PAUSE, 0);
  }

  /**
   * Gets the action to take.
   *
   * @return the fuel exhaustion action
   */
  public FuelExhaustionAction getAction() {
    return action;
  }

  /**
   * Gets the amount of additional fuel to add.
   *
   * <p>This value is only meaningful when the action is {@link FuelExhaustionAction#CONTINUE}.
   *
   * @return the additional fuel amount
   */
  public long getAdditionalFuel() {
    return additionalFuel;
  }

  @Override
  public String toString() {
    return "FuelExhaustionResult{"
        + "action="
        + action
        + ", additionalFuel="
        + additionalFuel
        + '}';
  }
}
