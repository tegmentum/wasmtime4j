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
 * Events that can occur during guest debugging.
 *
 * <p>When a {@link DebugHandler} is set on a store, it will be invoked with these events at various
 * points during WebAssembly execution.
 *
 * @since 1.1.0
 */
public enum DebugEvent {

  /**
   * A host call returned an error.
   *
   * <p>This event fires when a host function call results in an error that would normally trap the
   * WebAssembly instance.
   */
  HOSTCALL_ERROR(0),

  /**
   * A caught exception was thrown.
   *
   * <p>This event fires when a WebAssembly exception is thrown and there is a matching catch
   * handler in the call stack.
   */
  CAUGHT_EXCEPTION_THROWN(1),

  /**
   * An uncaught exception was thrown.
   *
   * <p>This event fires when a WebAssembly exception is thrown and there is no matching catch
   * handler, which will cause a trap.
   */
  UNCAUGHT_EXCEPTION_THROWN(2),

  /**
   * A trap occurred.
   *
   * <p>This event fires when WebAssembly execution traps (e.g., out-of-bounds memory access,
   * division by zero, unreachable instruction).
   */
  TRAP(3),

  /**
   * A breakpoint was hit.
   *
   * <p>This event fires when execution reaches an instruction that has a breakpoint set.
   */
  BREAKPOINT(4),

  /**
   * An epoch yield occurred.
   *
   * <p>This event fires when the epoch deadline is reached and the store is configured to yield on
   * epoch changes.
   */
  EPOCH_YIELD(5);

  private final int code;

  DebugEvent(final int code) {
    this.code = code;
  }

  /**
   * Gets the native code for this event.
   *
   * @return the numeric code
   */
  public int getCode() {
    return code;
  }

  /**
   * Converts a native code to a DebugEvent.
   *
   * @param code the native code
   * @return the corresponding DebugEvent
   * @throws IllegalArgumentException if the code is not recognized
   */
  public static DebugEvent fromCode(final int code) {
    for (final DebugEvent event : values()) {
      if (event.code == code) {
        return event;
      }
    }
    throw new IllegalArgumentException("Unknown debug event code: " + code);
  }
}
