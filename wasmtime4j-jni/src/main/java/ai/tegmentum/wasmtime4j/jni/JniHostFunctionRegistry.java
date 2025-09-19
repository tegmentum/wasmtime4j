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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.jni.util.JniTypeConverter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for host function callbacks to prevent garbage collection and enable native callbacks.
 *
 * <p>This class manages a global registry of host functions that are defined in linkers. The
 * registry ensures that host function implementations are not garbage collected while they are
 * registered with native linkers, and provides the callback mechanism for native code to invoke
 * Java implementations.
 *
 * <p>Thread Safety: This class is fully thread-safe and designed for concurrent access from
 * multiple threads and native code.
 *
 * @since 1.0.0
 */
final class JniHostFunctionRegistry {
  private static final Logger LOGGER = Logger.getLogger(JniHostFunctionRegistry.class.getName());

  // Global registry to prevent GC and enable callbacks
  private static final ConcurrentHashMap<Long, HostFunction> HOST_FUNCTIONS =
      new ConcurrentHashMap<>();
  private static final AtomicLong NEXT_ID = new AtomicLong(1L);

  /** Private constructor - this is a utility class. */
  private JniHostFunctionRegistry() {}

  /**
   * Registers a host function implementation and returns a unique callback ID.
   *
   * @param hostFunction the host function implementation to register
   * @return the unique callback ID for this function
   * @throws IllegalArgumentException if hostFunction is null
   */
  static long register(final HostFunction hostFunction) {
    if (hostFunction == null) {
      throw new IllegalArgumentException("Host function cannot be null");
    }

    final long callbackId = NEXT_ID.getAndIncrement();
    HOST_FUNCTIONS.put(callbackId, hostFunction);

    LOGGER.fine("Registered host function with callback ID: " + callbackId);
    return callbackId;
  }

  /**
   * Unregisters a host function by its callback ID.
   *
   * @param callbackId the callback ID to unregister
   * @return true if a function was removed, false if ID was not found
   */
  static boolean unregister(final long callbackId) {
    final HostFunction removed = HOST_FUNCTIONS.remove(callbackId);
    if (removed != null) {
      LOGGER.fine("Unregistered host function with callback ID: " + callbackId);
      return true;
    }
    return false;
  }

  /**
   * Gets the current number of registered host functions.
   *
   * @return the number of registered host functions
   */
  static int getRegistrySize() {
    return HOST_FUNCTIONS.size();
  }

  /**
   * Clears all registered host functions.
   *
   * <p>This method is primarily for testing and cleanup scenarios.
   */
  static void clear() {
    final int size = HOST_FUNCTIONS.size();
    HOST_FUNCTIONS.clear();
    if (size > 0) {
      LOGGER.info("Cleared " + size + " host functions from registry");
    }
  }

  /**
   * Static callback method invoked by native code when a host function is called.
   *
   * <p>This method serves as the bridge between native WebAssembly execution and Java host function
   * implementations. It marshals parameters, invokes the callback, and marshals results back to
   * native code.
   *
   * @param callbackId the ID of the host function to invoke
   * @param paramsData the serialized parameter data from WebAssembly
   * @param resultsBuffer buffer to write results to (may be null for functions with no returns)
   * @param resultsBufferSize the size of the results buffer
   * @return 0 on success, negative error code on failure
   */
  @SuppressWarnings("unused") // Called by native code
  private static int hostFunctionCallback(
      final long callbackId,
      final byte[] paramsData,
      final byte[] resultsBuffer,
      final int resultsBufferSize) {

    final HostFunction hostFunction = HOST_FUNCTIONS.get(callbackId);
    if (hostFunction == null) {
      LOGGER.severe("Host function not found in registry: " + callbackId);
      return -1;
    }

    try {
      // Unmarshal parameters from native format
      final WasmValue[] wasmParams = JniTypeConverter.unmarshalParameters(paramsData);

      // Invoke the Java callback
      final WasmValue[] wasmResults = hostFunction.execute(wasmParams);

      // Marshal results back to native format if there are any
      if (wasmResults != null && wasmResults.length > 0) {
        if (resultsBuffer == null) {
          LOGGER.severe(
              "Host function returned "
                  + wasmResults.length
                  + " values but no results buffer provided");
          return -2;
        }

        final int bytesWritten = JniTypeConverter.marshalResults(wasmResults, resultsBuffer);
        if (bytesWritten > resultsBufferSize) {
          LOGGER.severe(
              "Results buffer overflow: needed "
                  + bytesWritten
                  + " bytes but buffer is "
                  + resultsBufferSize
                  + " bytes");
          return -3;
        }
      }

      return 0;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error in host function callback: " + callbackId, e);
      return -4;
    }
  }

  /**
   * Gets registry statistics for debugging and monitoring.
   *
   * @return array containing [registrySize, nextId]
   */
  static long[] getStats() {
    return new long[] {HOST_FUNCTIONS.size(), NEXT_ID.get()};
  }
}
