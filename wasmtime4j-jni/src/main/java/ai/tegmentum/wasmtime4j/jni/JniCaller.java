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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Caller;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the Caller interface for accessing WebAssembly instance context.
 *
 * <p>This class provides access to the calling WebAssembly instance's exports, memory, globals, and
 * execution state through JNI bindings to the native Wasmtime caller context.
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
final class JniCaller<T> implements Caller<T> {
  private static final Logger LOGGER = Logger.getLogger(JniCaller.class.getName());

  // Load native library
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniCaller: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long callerHandle;
  private final JniStore store;

  /**
   * Creates a JNI caller context wrapper.
   *
   * @param callerHandle the native caller handle from Wasmtime
   * @param store the store this caller is associated with
   */
  JniCaller(final long callerHandle, final JniStore store) {
    if (callerHandle == 0) {
      throw new IllegalArgumentException("Caller handle cannot be 0");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.callerHandle = callerHandle;
    this.store = store;

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created JniCaller with handle: 0x" + Long.toHexString(callerHandle));
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
  public Optional<Extern> getExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    try {
      // Try function
      final long funcHandle = nativeGetFunction(callerHandle, name);
      if (funcHandle != 0) {
        return Optional.of(new JniExternFunc(funcHandle, store));
      }

      // Try memory
      final long memHandle = nativeGetMemory(callerHandle, name);
      if (memHandle != 0) {
        return Optional.of(new JniExternMemory(memHandle, store));
      }

      // Try table
      final long tableHandle = nativeGetTable(callerHandle, name);
      if (tableHandle != 0) {
        return Optional.of(new JniExternTable(tableHandle, store));
      }

      // Try global
      final long globalHandle = nativeGetGlobal(callerHandle, name);
      if (globalHandle != 0) {
        return Optional.of(new JniExternGlobal(globalHandle, store));
      }

      return Optional.empty();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get export: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Extern> getExport(final ModuleExport moduleExport) {
    if (moduleExport == null) {
      throw new IllegalArgumentException("ModuleExport cannot be null");
    }
    // Delegate to name-based lookup using the ModuleExport's name
    return getExport(moduleExport.name());
  }

  @Override
  public Optional<WasmFunction> getFunction(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }

    try {
      final long funcHandle = nativeGetFunction(callerHandle, name);
      if (funcHandle == 0) {
        return Optional.empty();
      }
      // Module handle is 0 since we're getting the function from a caller context
      return Optional.of(new JniFunction(funcHandle, name, 0L, store));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get function: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmMemory> getMemory(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Memory name cannot be null");
    }

    try {
      final long memHandle = nativeGetMemory(callerHandle, name);
      if (memHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniMemory(memHandle, store));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get memory: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmTable> getTable(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Table name cannot be null");
    }

    try {
      final long tableHandle = nativeGetTable(callerHandle, name);
      if (tableHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTable(tableHandle, store));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get table: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Global name cannot be null");
    }

    try {
      final long globalHandle = nativeGetGlobal(callerHandle, name);
      if (globalHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniGlobal(globalHandle, store));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get global: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    try {
      return nativeHasExport(callerHandle, name);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to check export: " + name, e);
      return false;
    }
  }

  @Override
  public Optional<Long> fuelRemaining() {
    try {
      final long fuel = nativeGetFuelRemaining(callerHandle);
      return fuel >= 0 ? Optional.of(fuel) : Optional.empty();
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "Fuel remaining not available", e);
      return Optional.empty();
    }
  }

  @Override
  public void addFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }

    try {
      nativeAddFuel(callerHandle, fuel);
    } catch (Exception e) {
      throw new WasmException("Failed to add fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }

    try {
      nativeSetFuel(callerHandle, fuel);
    } catch (Exception e) {
      throw new WasmException("Failed to set fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public Engine engine() {
    // Get the engine from the store
    return store.getEngine();
  }

  @Override
  public void gc() throws WasmException {
    try {
      store.gc();
    } catch (Exception e) {
      throw new WasmException("Failed to perform GC: " + e.getMessage(), e);
    }
  }

  @Override
  public void setFuelAsyncYieldInterval(final long interval) throws WasmException {
    if (interval < 0) {
      throw new IllegalArgumentException("interval cannot be negative");
    }
    nativeSetFuelAsyncYieldInterval(callerHandle, interval);
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
  JniStore getStore() {
    return store;
  }

  @Override
  public String toString() {
    return String.format("JniCaller{handle=0x%x}", callerHandle);
  }

  // Native method declarations

  /**
   * Gets a memory export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the memory name
   * @return native memory handle, or 0 if not found
   */
  private static native long nativeGetMemory(long callerHandle, String name);

  /**
   * Gets a global export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the global name
   * @return native global handle, or 0 if not found
   */
  private static native long nativeGetGlobal(long callerHandle, String name);

  /**
   * Gets a function export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the function name
   * @return native function handle, or 0 if not found
   */
  private static native long nativeGetFunction(long callerHandle, String name);

  /**
   * Gets a table export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the table name
   * @return native table handle, or 0 if not found
   */
  private static native long nativeGetTable(long callerHandle, String name);

  /**
   * Checks if an export exists.
   *
   * @param callerHandle the native caller handle
   * @param name the export name
   * @return true if the export exists
   */
  private static native boolean nativeHasExport(long callerHandle, String name);

  /**
   * Gets the fuel remaining in the caller.
   *
   * @param callerHandle the native caller handle
   * @return fuel remaining, or -1 if not available
   */
  private static native long nativeGetFuelRemaining(long callerHandle);

  /**
   * Adds fuel to the caller.
   *
   * @param callerHandle the native caller handle
   * @param fuel the amount of fuel to add
   */
  private static native void nativeAddFuel(long callerHandle, long fuel);

  private static native void nativeSetFuel(long callerHandle, long fuel);

  /**
   * Sets the fuel async yield interval for the caller's store.
   *
   * @param callerHandle the native caller handle
   * @param interval the yield interval, or 0 to disable
   */
  private static native void nativeSetFuelAsyncYieldInterval(long callerHandle, long interval);
}
