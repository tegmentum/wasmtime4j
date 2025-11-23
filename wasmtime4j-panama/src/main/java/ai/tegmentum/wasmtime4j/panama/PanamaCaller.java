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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the Caller interface for accessing WebAssembly instance context.
 *
 * <p>This class provides access to the calling WebAssembly instance's exports, memory, globals, and
 * execution state through Panama Foreign Function API bindings to the native Wasmtime caller
 * context.
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
final class PanamaCaller<T> implements Caller<T> {
  private static final Logger LOGGER = Logger.getLogger(PanamaCaller.class.getName());

  private final long callerHandle;
  private final PanamaStore store;

  /**
   * Creates a Panama caller context wrapper.
   *
   * @param callerHandle the native caller handle from Wasmtime
   * @param store the store this caller is associated with
   */
  PanamaCaller(final long callerHandle, final PanamaStore store) {
    if (callerHandle == 0) {
      throw new IllegalArgumentException("Caller handle cannot be 0");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.callerHandle = callerHandle;
    this.store = store;

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created PanamaCaller with handle: 0x" + Long.toHexString(callerHandle));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public T data() {
    // Get user data from the store
    final Object storeData = store.getData();
    return storeData != null ? (T) storeData : null;
  }

  @Override
  public Optional<Export> getExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    // TODO: Implement export retrieval
    return Optional.empty();
  }

  @Override
  public Optional<Function<T>> getFunction(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }

    // TODO: Implement function export retrieval
    return Optional.empty();
  }

  @Override
  public Optional<Memory> getMemory(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Memory name cannot be null");
    }

    // TODO: Implement caller memory access
    // Native bindings currently return 0 (not yet implemented)
    return Optional.empty();
  }

  @Override
  public Optional<Table> getTable(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Table name cannot be null");
    }

    // TODO: Implement table export retrieval
    return Optional.empty();
  }

  @Override
  public Optional<Global> getGlobal(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Global name cannot be null");
    }

    // TODO: Implement caller global access
    // Native bindings currently return 0 (not yet implemented)
    return Optional.empty();
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    // TODO: Implement export checking
    return false;
  }

  @Override
  public Optional<Long> fuelConsumed() {
    // TODO: Implement fuel consumption tracking
    return Optional.empty();
  }

  @Override
  public Optional<Long> fuelRemaining() {
    // TODO: Implement fuel remaining tracking
    return Optional.empty();
  }

  @Override
  public void addFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }

    // TODO: Implement fuel addition
    throw new WasmException("Fuel operations not yet implemented for Panama");
  }

  @Override
  public boolean hasEpochDeadline() {
    // TODO: Implement epoch deadline checking
    return false;
  }

  @Override
  public Optional<Long> epochDeadline() {
    // TODO: Implement epoch deadline retrieval
    return Optional.empty();
  }

  @Override
  public void setEpochDeadline(final long deadline) throws WasmException {
    // TODO: Implement epoch deadline setting
    throw new WasmException("Epoch operations not yet implemented for Panama");
  }

  /**
   * Gets the native caller handle.
   *
   * @return the native handle
   */
  long getCallerHandle() {
    return callerHandle;
  }

  /**
   * Gets the associated store.
   *
   * @return the store
   */
  PanamaStore getStore() {
    return store;
  }

  @Override
  public String toString() {
    return String.format("PanamaCaller{handle=0x%x}", callerHandle);
  }
}
