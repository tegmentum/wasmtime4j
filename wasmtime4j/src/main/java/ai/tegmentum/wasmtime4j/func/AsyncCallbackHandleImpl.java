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

package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.type.FunctionType;

/**
 * Implementation of {@link CallbackRegistry.AsyncCallbackHandle}.
 *
 * @since 1.0.0
 */
public class AsyncCallbackHandleImpl extends CallbackHandleImpl
    implements CallbackRegistry.AsyncCallbackHandle {

  private volatile long timeoutMillis;

  /**
   * Creates a new async callback handle.
   *
   * @param id the unique callback identifier
   * @param name the callback name
   * @param functionType the function type signature
   * @param timeoutMillis the initial timeout in milliseconds
   */
  public AsyncCallbackHandleImpl(
      final long id,
      final String name,
      final FunctionType functionType,
      final long timeoutMillis) {
    super(id, name, functionType);
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public long getTimeoutMillis() {
    return timeoutMillis;
  }

  @Override
  public void setTimeoutMillis(final long timeoutMillis) {
    if (timeoutMillis <= 0) {
      throw new IllegalArgumentException("Timeout must be positive");
    }
    this.timeoutMillis = timeoutMillis;
  }
}
