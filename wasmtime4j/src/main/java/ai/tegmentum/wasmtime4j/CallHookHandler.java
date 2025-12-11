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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.TrapException;

/**
 * Handler for call hook events during WebAssembly execution.
 *
 * <p>This functional interface is used with {@link Store#setCallHook(CallHookHandler)} to receive
 * notifications on every transition between WebAssembly and host code. The handler is invoked with
 * a {@link CallHook} indicating the type of transition.
 *
 * <p>Call hooks can be used for:
 *
 * <ul>
 *   <li>Debugging and profiling WebAssembly execution
 *   <li>Implementing execution timeouts
 *   <li>Resource tracking and metering
 *   <li>Security auditing of host function calls
 * </ul>
 *
 * <p><b>Performance note:</b> Call hooks are invoked on every entry/exit from WebAssembly code,
 * which can have a measurable performance impact. Use sparingly in performance-critical
 * applications.
 *
 * <p>Example:
 *
 * <pre>{@code
 * store.setCallHook((hook) -> {
 *     switch (hook) {
 *         case CALLING_WASM:
 *             long start = System.nanoTime();
 *             // ... track entry time
 *             break;
 *         case RETURNING_FROM_WASM:
 *             // ... calculate duration
 *             break;
 *     }
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface CallHookHandler {

  /**
   * Handles a call hook event.
   *
   * <p>This method is called on every transition between WebAssembly and host code. The handler
   * should be as fast as possible to minimize performance impact.
   *
   * <p>Throwing a {@link TrapException} from this handler will cause the WebAssembly execution to
   * trap with the specified trap reason.
   *
   * @param hook the type of call transition occurring
   * @throws TrapException if execution should be trapped
   */
  void onCallHook(CallHook hook) throws TrapException;
}
