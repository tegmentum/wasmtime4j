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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Caller;
import ai.tegmentum.wasmtime4j.Export;
import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.Global;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Table;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly caller interface.
 *
 * <p>This implementation provides access to the calling WebAssembly instance context
 * within host functions. It uses Panama FFI for direct native access to exports,
 * fuel management, and execution state.
 *
 * <p>The caller interface enables host functions to:
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
public final class PanamaCaller<T> implements Caller<T>, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaCaller.class.getName());

  // Core infrastructure
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final MemorySegment callerPtr;
  private final MemorySegment instancePtr;
  private final MemorySegment storePtr;
  private final T userData;

  // Instance state
  private final PanamaInstance callingInstance;

  // Status
  private volatile boolean closed = false;

  /**
   * Creates a new Panama caller implementation.
   *
   * @param resourceManager the arena resource manager for memory management
   * @param nativeFunctions the native function bindings
   * @param callerPtr pointer to the native caller context
   * @param instancePtr pointer to the calling instance
   * @param storePtr pointer to the store
   * @param userData the store's user data
   * @param callingInstance the calling instance wrapper
   */
  public PanamaCaller(
      final ArenaResourceManager resourceManager,
      final NativeFunctionBindings nativeFunctions,
      final MemorySegment callerPtr,
      final MemorySegment instancePtr,
      final MemorySegment storePtr,
      final T userData,
      final PanamaInstance callingInstance) {
    this.resourceManager = resourceManager;
    this.nativeFunctions = nativeFunctions;
    this.callerPtr = callerPtr;
    this.instancePtr = instancePtr;
    this.storePtr = storePtr;
    this.userData = userData;
    this.callingInstance = callingInstance;

    LOGGER.fine("Created PanamaCaller with caller=" + callerPtr);
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
      Optional<Export> export = getExport(name);
      if (export.isPresent() && export.get().getType() == Export.Type.FUNCTION) {
        return Optional.of((Function) export.get().getValue());
      }
      return Optional.empty();
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
      Optional<Export> export = getExport(name);
      if (export.isPresent() && export.get().getType() == Export.Type.MEMORY) {
        return Optional.of((Memory) export.get().getValue());
      }
      return Optional.empty();
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
      Optional<Export> export = getExport(name);
      if (export.isPresent() && export.get().getType() == Export.Type.TABLE) {
        return Optional.of((Table) export.get().getValue());
      }
      return Optional.empty();
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
      Optional<Export> export = getExport(name);
      if (export.isPresent() && export.get().getType() == Export.Type.GLOBAL) {
        return Optional.of((Global) export.get().getValue());
      }
      return Optional.empty();
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
      return getExport(name).isPresent();
    } catch (Exception e) {
      LOGGER.warning("Failed to check export '" + name + "': " + e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<Long> fuelConsumed() {
    ensureNotClosed();

    try {
      // Allocate memory for fuel output
      ArenaResourceManager.ManagedMemorySegment fuelPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      // Get remaining fuel from the store
      int result = nativeFunctions.storeGetFuel(storePtr, fuelPtr.getSegment());
      if (result != 0) {
        LOGGER.warning("Failed to get fuel consumption, error code: " + result);
        return Optional.empty();
      }

      // Extract fuel value
      long remainingFuel = (Long) MemoryLayouts.C_SIZE_T.varHandle().get(fuelPtr.getSegment(), 0);

      // Note: This implementation returns remaining fuel, not consumed fuel
      // In a complete implementation, we would need to track initial fuel to calculate consumed
      return Optional.of(remainingFuel);

    } catch (Exception e) {
      LOGGER.warning("Failed to get fuel consumption: " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public boolean hasEpochDeadline() {
    ensureNotClosed();

    try {
      // Check if epoch interruption is enabled on the engine
      // This is a simplified check - in a complete implementation we would
      // check for active epoch deadlines on the store
      return epochDeadline().isPresent();
    } catch (Exception e) {
      LOGGER.warning("Failed to check epoch deadline: " + e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<Long> epochDeadline() {
    ensureNotClosed();

    try {
      // In the current implementation, we don't have direct access to epoch deadlines
      // This would require additional native functions to be implemented
      // For now, return empty to indicate no deadline is available
      LOGGER.fine("Epoch deadline access not fully implemented in current native bindings");
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.warning("Failed to get epoch deadline: " + e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Gets the native caller pointer.
   *
   * @return the native caller pointer
   */
  public MemorySegment getCallerPointer() {
    ensureNotClosed();
    return callerPtr;
  }

  /**
   * Gets the calling instance.
   *
   * @return the calling instance
   */
  public PanamaInstance getCallingInstance() {
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

      // Note: We don't actually destroy the caller pointer here because
      // it's managed by the native runtime and will be cleaned up automatically
      // when the host function call completes.

      closed = true;
      LOGGER.fine("Closed PanamaCaller");
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
    return "PanamaCaller{" +
           "callerPtr=" + callerPtr +
           ", instancePtr=" + instancePtr +
           ", storePtr=" + storePtr +
           ", closed=" + closed +
           '}';
  }
}