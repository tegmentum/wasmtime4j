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

/**
 * Indicates state transitions in the WebAssembly VM.
 *
 * <p>This enum is passed to call hook functions to indicate what state transition the VM is making.
 * Call hooks are configured via {@link Store#setCallHook(CallHookHandler)} and are called on every
 * transition between WebAssembly and host code.
 *
 * <p>The four variants represent the four possible transitions:
 *
 * <ul>
 *   <li>{@link #CALLING_WASM} - Host is calling into WebAssembly
 *   <li>{@link #RETURNING_FROM_WASM} - Returning from WebAssembly back to host
 *   <li>{@link #CALLING_HOST} - WebAssembly is calling a host function
 *   <li>{@link #RETURNING_FROM_HOST} - Returning from host function back to WebAssembly
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * store.setCallHook((hook) -> {
 *     switch (hook) {
 *         case CALLING_WASM:
 *             System.out.println("Entering WebAssembly");
 *             break;
 *         case RETURNING_FROM_WASM:
 *             System.out.println("Leaving WebAssembly");
 *             break;
 *         case CALLING_HOST:
 *             System.out.println("Calling host function");
 *             break;
 *         case RETURNING_FROM_HOST:
 *             System.out.println("Returning from host function");
 *             break;
 *     }
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
public enum CallHook {

  /**
   * Indicates the VM is calling a WebAssembly function from the host.
   *
   * <p>This occurs when Java code calls a WebAssembly exported function.
   */
  CALLING_WASM(0),

  /**
   * Indicates the VM is returning from a WebAssembly function to the host.
   *
   * <p>This occurs when a WebAssembly function returns to the Java caller.
   */
  RETURNING_FROM_WASM(1),

  /**
   * Indicates the VM is calling a host function from WebAssembly.
   *
   * <p>This occurs when WebAssembly code calls an imported host function.
   */
  CALLING_HOST(2),

  /**
   * Indicates the VM is returning from a host function to WebAssembly.
   *
   * <p>This occurs when a host function returns to WebAssembly code.
   */
  RETURNING_FROM_HOST(3);

  private final int value;

  CallHook(final int value) {
    this.value = value;
  }

  /**
   * Gets the native integer value for this call hook type.
   *
   * @return the native value
   */
  public int getValue() {
    return value;
  }

  /**
   * Creates a CallHook from its native integer value.
   *
   * @param value the native value
   * @return the corresponding CallHook
   * @throws IllegalArgumentException if the value is unknown
   */
  public static CallHook fromValue(final int value) {
    switch (value) {
      case 0:
        return CALLING_WASM;
      case 1:
        return RETURNING_FROM_WASM;
      case 2:
        return CALLING_HOST;
      case 3:
        return RETURNING_FROM_HOST;
      default:
        throw new IllegalArgumentException("Unknown CallHook value: " + value);
    }
  }
}
