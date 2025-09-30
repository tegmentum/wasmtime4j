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

import ai.tegmentum.wasmtime4j.Caller;
import ai.tegmentum.wasmtime4j.Export;
import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.Global;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Table;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JNI implementation of the WebAssembly caller interface.
 *
 * <p>This implementation provides access to the calling WebAssembly instance context within host
 * functions. It uses JNI for direct native access to exports, fuel management, and execution state.
 *
 * <p>The caller interface enables host functions to:
 *
 * <ul>
 *   <li>Access exported functions, memory, tables, and globals from the calling instance
 *   <li>Monitor fuel consumption if fuel metering is enabled
 *   <li>Check epoch deadlines for execution time management
 *   <li>Interact safely with the WebAssembly execution environment
 * </ul>
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
public final class JniCaller<T> implements Caller<T>, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(JniCaller.class.getName());

  // Core infrastructure
  private final long callerHandle;
  private final T userData;
  private final JniInstance callingInstance;

  // Status
  private volatile boolean closed = false;

  /**
   * Creates a new JNI caller implementation.
   *
   * @param callerHandle native handle to the caller context
   * @param userData the store's user data
   * @param callingInstance the calling instance wrapper
   */
  public JniCaller(final long callerHandle, final T userData, final JniInstance callingInstance) {
    this.callerHandle = callerHandle;
    this.userData = userData;
    this.callingInstance = callingInstance;

    LOGGER.fine("Created JniCaller with handle=" + callerHandle);
  }

  @Override
  public T data() {
    ensureNotClosed();
    return userData;
  }

  @Override
  public Optional<Export> getExport(final String name) {
    ensureNotClosed();

    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    try {
      // Use the calling instance to get the export
      return callingInstance.getExport(name);
    } catch (Exception e) {
      LOGGER.warning("Failed to get export '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<Function> getFunction(final String name) {
    ensureNotClosed();

    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }

    try {
      long functionHandle = nativeGetFunction(callerHandle, name);
      if (functionHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniFunction(functionHandle));
    } catch (Exception e) {
      LOGGER.warning("Failed to get function '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<Memory> getMemory(final String name) {
    ensureNotClosed();

    if (name == null) {
      throw new IllegalArgumentException("Memory name cannot be null");
    }

    try {
      long memoryHandle = nativeGetMemory(callerHandle, name);
      if (memoryHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniMemory(memoryHandle));
    } catch (Exception e) {
      LOGGER.warning("Failed to get memory '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<Table> getTable(final String name) {
    ensureNotClosed();

    if (name == null) {
      throw new IllegalArgumentException("Table name cannot be null");
    }

    try {
      long tableHandle = nativeGetTable(callerHandle, name);
      if (tableHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTable(tableHandle));
    } catch (Exception e) {
      LOGGER.warning("Failed to get table '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<Global> getGlobal(final String name) {
    ensureNotClosed();

    if (name == null) {
      throw new IllegalArgumentException("Global name cannot be null");
    }

    try {
      long globalHandle = nativeGetGlobal(callerHandle, name);
      if (globalHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniGlobal(globalHandle));
    } catch (Exception e) {
      LOGGER.warning("Failed to get global '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public boolean hasExport(final String name) {
    ensureNotClosed();

    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    try {
      return nativeHasExport(callerHandle, name);
    } catch (Exception e) {
      LOGGER.warning("Failed to check export '" + name + "': " + e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<Long> fuelConsumed() {
    ensureNotClosed();

    try {
      long[] fuelArray = new long[1];
      boolean success = nativeGetFuel(callerHandle, fuelArray);
      if (success) {
        return Optional.of(fuelArray[0]);
      }
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get fuel consumption: " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<Long> fuelRemaining() {
    ensureNotClosed();

    try {
      long[] fuelArray = new long[1];
      boolean success = nativeGetFuelRemaining(callerHandle, fuelArray);
      if (success) {
        return Optional.of(fuelArray[0]);
      }
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get fuel remaining: " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void addFuel(final long fuel) throws WasmException {
    ensureNotClosed();

    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }

    try {
      boolean success = nativeAddFuel(callerHandle, fuel);
      if (!success) {
        throw new WasmException("Failed to add fuel to caller");
      }
    } catch (Exception e) {
      throw new WasmException("Failed to add fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean hasEpochDeadline() {
    ensureNotClosed();

    try {
      return nativeHasEpochDeadline(callerHandle);
    } catch (Exception e) {
      LOGGER.warning("Failed to check epoch deadline: " + e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<Long> epochDeadline() {
    ensureNotClosed();

    try {
      // For now, we use hasEpochDeadline as a proxy since the actual deadline value
      // is not directly accessible through the current Wasmtime API
      if (hasEpochDeadline()) {
        // We could implement a getter for the actual deadline value if needed
        // For now, return a placeholder value indicating deadline is set
        return Optional.of(0L);
      }
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get epoch deadline: " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void setEpochDeadline(final long deadline) throws WasmException {
    ensureNotClosed();

    try {
      boolean success = nativeSetEpochDeadline(callerHandle, deadline);
      if (!success) {
        throw new WasmException("Failed to set epoch deadline");
      }
    } catch (Exception e) {
      throw new WasmException("Failed to set epoch deadline: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the native caller handle.
   *
   * @return the native caller handle
   */
  public long getCallerHandle() {
    ensureNotClosed();
    return callerHandle;
  }

  /**
   * Gets the calling instance.
   *
   * @return the calling instance
   */
  public JniInstance getCallingInstance() {
    ensureNotClosed();
    return callingInstance;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      // Note: We don't actually destroy the caller handle here because
      // it's managed by the native runtime and will be cleaned up automatically
      // when the host function call completes.

      closed = true;
      LOGGER.fine("Closed JniCaller");
    }
  }

  /**
   * Ensures this caller is not closed.
   *
   * @throws IllegalStateException if the caller is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Caller has been closed");
    }
  }

  @Override
  public String toString() {
    return "JniCaller{" + "callerHandle=" + callerHandle + ", closed=" + closed + '}';
  }

  // Native method declarations
  private static native boolean nativeGetFuel(long callerHandle, long[] fuelOut);

  private static native boolean nativeGetFuelRemaining(long callerHandle, long[] fuelOut);

  private static native boolean nativeAddFuel(long callerHandle, long fuel);

  private static native boolean nativeSetEpochDeadline(long callerHandle, long deadline);

  private static native boolean nativeHasEpochDeadline(long callerHandle);

  private static native boolean nativeHasExport(long callerHandle, String name);

  private static native long nativeGetMemory(long callerHandle, String name);

  private static native long nativeGetFunction(long callerHandle, String name);

  private static native long nativeGetGlobal(long callerHandle, String name);

  private static native long nativeGetTable(long callerHandle, String name);
}
