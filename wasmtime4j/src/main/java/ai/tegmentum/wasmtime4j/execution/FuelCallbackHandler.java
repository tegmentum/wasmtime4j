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

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Handler for fuel exhaustion events during WebAssembly execution.
 *
 * <p>This interface manages fuel exhaustion callbacks, allowing applications to add more fuel when
 * WebAssembly execution runs out of fuel, or to trap/pause execution.
 *
 * <p>Implementations can be created with automatic refill behavior or with custom callback logic.
 *
 * @since 1.0.0
 */
public interface FuelCallbackHandler extends AutoCloseable {

  /**
   * Gets the unique handler ID.
   *
   * @return the handler ID
   */
  long getId();

  /**
   * Gets the store ID this handler is associated with.
   *
   * @return the store ID
   */
  long getStoreId();

  /**
   * Handles a fuel exhaustion event.
   *
   * @param context the context information about the fuel exhaustion
   * @return the result indicating how execution should proceed
   * @throws WasmException if handling fails
   */
  FuelExhaustionResult handleExhaustion(FuelExhaustionContext context) throws WasmException;

  /**
   * Gets statistics about fuel exhaustion events.
   *
   * @return the current statistics
   * @throws WasmException if retrieving statistics fails
   */
  FuelCallbackStats getStats() throws WasmException;

  /**
   * Resets the statistics counters.
   *
   * @throws WasmException if resetting fails
   */
  void resetStats() throws WasmException;

  /**
   * Closes this handler and releases associated resources.
   *
   * @throws WasmException if closing fails
   */
  @Override
  void close() throws WasmException;
}
