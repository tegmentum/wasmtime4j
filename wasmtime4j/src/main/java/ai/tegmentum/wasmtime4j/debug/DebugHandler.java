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

package ai.tegmentum.wasmtime4j.debug;

/**
 * Handler interface for receiving debug events during WebAssembly execution.
 *
 * <p>Implement this interface to receive callbacks when debug events occur, such as breakpoints
 * being hit, function calls, or traps.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * DebugHandler handler = new DebugHandler() {
 *     public DebugAction onEvent(DebugEvent event) {
 *         System.out.println("Event: " + event);
 *         if (event.getType() == DebugEvent.EventType.BREAKPOINT) {
 *             // Inspect state, modify variables, etc.
 *             return DebugAction.STEP;
 *         }
 *         return DebugAction.CONTINUE;
 *     }
 * };
 *
 * store.setDebugHandler(handler);
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface DebugHandler {

  /**
   * Called when a debug event occurs.
   *
   * <p>The handler can inspect the event and return an action indicating how execution should
   * proceed.
   *
   * @param event the debug event
   * @return the action to take
   */
  DebugAction onEvent(DebugEvent event);

  /**
   * Called before the handler is unregistered.
   *
   * <p>Override this method to perform cleanup when the handler is removed.
   */
  default void onUnregister() {
    // Default implementation does nothing
  }

  /**
   * Creates a handler that logs all events and continues execution.
   *
   * @return a logging debug handler
   */
  static DebugHandler logging() {
    return event -> {
      System.out.println("[DEBUG] " + event);
      return DebugAction.CONTINUE;
    };
  }

  /**
   * Creates a handler that breaks on all events.
   *
   * @return a break-all debug handler
   */
  static DebugHandler breakAll() {
    return event -> DebugAction.PAUSE;
  }

  /**
   * Creates a handler that only breaks on traps.
   *
   * @return a trap-only debug handler
   */
  static DebugHandler breakOnTrap() {
    return event -> event.getType() == DebugEvent.DebugEventType.EXCEPTION
        ? DebugAction.PAUSE
        : DebugAction.CONTINUE;
  }

  /**
   * Actions that can be returned by a debug handler.
   */
  enum DebugAction {
    /**
     * Continue execution normally.
     */
    CONTINUE,

    /**
     * Execute a single instruction and break again.
     */
    STEP,

    /**
     * Step into function calls.
     */
    STEP_INTO,

    /**
     * Step over function calls (complete the call and break after).
     */
    STEP_OVER,

    /**
     * Step out of the current function.
     */
    STEP_OUT,

    /**
     * Pause execution at this point.
     */
    PAUSE,

    /**
     * Abort execution with an error.
     */
    ABORT
  }
}
