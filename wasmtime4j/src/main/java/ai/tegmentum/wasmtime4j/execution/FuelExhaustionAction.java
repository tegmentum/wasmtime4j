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
 * Actions that can be taken when WebAssembly execution runs out of fuel.
 *
 * <p>When a fuel exhaustion callback is invoked, it must return one of these actions to indicate
 * how execution should proceed.
 *
 * @since 1.0.0
 */
public enum FuelExhaustionAction {
  /**
   * Continue execution by adding more fuel.
   *
   * <p>The callback must also provide the amount of fuel to add via {@link
   * FuelExhaustionResult#getAdditionalFuel()}.
   */
  CONTINUE(0),

  /**
   * Halt execution with a trap.
   *
   * <p>This will cause the WebAssembly execution to terminate with a trap exception.
   */
  TRAP(1),

  /**
   * Pause execution for async scenarios.
   *
   * <p>This action is useful for cooperative scheduling where execution can be resumed later.
   */
  PAUSE(2);

  private final int code;

  FuelExhaustionAction(final int code) {
    this.code = code;
  }

  /**
   * Gets the native code for this action.
   *
   * @return the native code
   */
  public int getCode() {
    return code;
  }

  /**
   * Gets the action corresponding to a native code.
   *
   * @param code the native code
   * @return the corresponding action
   * @throws IllegalArgumentException if the code is not valid
   */
  public static FuelExhaustionAction fromCode(final int code) {
    for (final FuelExhaustionAction action : values()) {
      if (action.code == code) {
        return action;
      }
    }
    throw new IllegalArgumentException("Unknown fuel exhaustion action code: " + code);
  }
}
