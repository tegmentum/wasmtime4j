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

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JNI implementation of the Instance interface.
 *
 * <p>This class represents an instantiated WebAssembly module and provides access to its functions,
 * memories, tables, and globals through JNI calls to the native Wasmtime library. An instance is
 * the runtime representation of a compiled module.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 */
public final class JniInstance extends JniResource implements Instance {

  private static final Logger LOGGER = Logger.getLogger(JniInstance.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniInstance: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Reference to the module used to create this instance. */
  private final Module module;

  /** Reference to the store this instance belongs to. */
  private final Store store;

  /**
   * Creates a new JNI instance with the given native handle, module, and store.
   *
   * @param nativeHandle the native instance handle
   * @param module the module used to create this instance
   * @param store the store this instance belongs to
   * @throws IllegalArgumentException if nativeHandle is 0
   * @throws IllegalArgumentException if module or store is null
   */
  JniInstance(final long nativeHandle, final Module module, final Store store) {
    super(nativeHandle);
    Validation.requireNonNull(module, "module");
    Validation.requireNonNull(store, "store");
    this.module = module;
    this.store = store;
    LOGGER.fine("Created JNI instance with handle: " + nativeHandle);
  }

  /**
   * Gets a function export by name.
   *
   * @param name the name of the exported function
   * @return the function wrapper, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmFunction> getFunction(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    try {
      final JniStore jniStore = (JniStore) store;
      final long functionHandle =
          nativeGetFunction(getNativeHandle(), jniStore.getNativeHandle(), name);
      if (functionHandle == 0) {
        return Optional.empty();
      }
      // Get module handle for thread-local execution
      final long moduleHandle =
          (module instanceof JniModule) ? ((JniModule) module).getNativeHandle() : 0;
      return Optional.of(new JniFunction(functionHandle, name, moduleHandle, jniStore));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting function: " + name, e);
    }
  }

  /**
   * Gets a memory export by name.
   *
   * @param name the name of the exported memory
   * @return the memory wrapper, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmMemory> getMemory(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long memoryHandle =
          nativeGetMemory(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (memoryHandle == 0) {
        return Optional.empty();
      }
      final JniMemory memory = new JniMemory(memoryHandle, (JniStore) store);
      memory.setInstanceHandle(getNativeHandle());
      return Optional.of(memory);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory: " + name, e);
    }
  }

  /**
   * Gets a table export by name.
   *
   * @param name the name of the exported table
   * @return the table wrapper, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmTable> getTable(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long tableHandle =
          nativeGetTable(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (tableHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTable(tableHandle, (JniStore) store, getNativeHandle()));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table: " + name, e);
    }
  }

  /**
   * Gets a global export by name.
   *
   * @param name the name of the exported global
   * @return the global wrapper, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  @Override
  public Optional<WasmGlobal> getGlobal(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long globalHandle =
          nativeGetGlobal(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (globalHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniGlobal(globalHandle, (JniStore) store));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global: " + name, e);
    }
  }

  @Override
  public Optional<Tag> getTag(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long tagHandle =
          nativeGetTag(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (tagHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTag(tagHandle, ((JniStore) store).getNativeHandle()));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting tag: " + name, e);
    }
  }

  @Override
  public Optional<WasmMemory> getSharedMemory(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final long memoryHandle =
          nativeGetSharedMemory(getNativeHandle(), ((JniStore) store).getNativeHandle(), name);
      if (memoryHandle == 0) {
        return Optional.empty();
      }
      final JniMemory memory = new JniMemory(memoryHandle, (JniStore) store);
      memory.setInstanceHandle(getNativeHandle());
      return Optional.of(memory);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting shared memory: " + name, e);
    }
  }

  @Override
  public Optional<WasmFunction> debugFunction(final int functionIndex) {
    ensureNotClosed();
    try {
      final JniStore jniStore = (JniStore) store;
      final long functionHandle =
          nativeDebugFunction(getNativeHandle(), jniStore.getNativeHandle(), functionIndex);
      if (functionHandle == 0) {
        return Optional.empty();
      }
      final long moduleHandle =
          (module instanceof JniModule) ? ((JniModule) module).getNativeHandle() : 0;
      return Optional.of(new JniFunction(functionHandle, "", moduleHandle, jniStore));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in debugFunction: " + functionIndex, e);
    }
  }

  @Override
  public Optional<WasmGlobal> debugGlobal(final int globalIndex) {
    ensureNotClosed();
    try {
      final long globalHandle =
          nativeDebugGlobal(getNativeHandle(), ((JniStore) store).getNativeHandle(), globalIndex);
      if (globalHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniGlobal(globalHandle, (JniStore) store));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in debugGlobal: " + globalIndex, e);
    }
  }

  @Override
  public Optional<WasmMemory> debugMemory(final int memoryIndex) {
    ensureNotClosed();
    try {
      final long memoryHandle =
          nativeDebugMemory(getNativeHandle(), ((JniStore) store).getNativeHandle(), memoryIndex);
      if (memoryHandle == 0) {
        return Optional.empty();
      }
      final JniMemory memory = new JniMemory(memoryHandle, (JniStore) store);
      memory.setInstanceHandle(getNativeHandle());
      return Optional.of(memory);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in debugMemory: " + memoryIndex, e);
    }
  }

  @Override
  public Optional<WasmMemory> debugSharedMemory(final int memoryIndex) {
    ensureNotClosed();
    try {
      final long memoryHandle =
          nativeDebugSharedMemory(
              getNativeHandle(), ((JniStore) store).getNativeHandle(), memoryIndex);
      if (memoryHandle == 0) {
        return Optional.empty();
      }
      final JniMemory memory = new JniMemory(memoryHandle, (JniStore) store);
      memory.setInstanceHandle(getNativeHandle());
      return Optional.of(memory);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in debugSharedMemory: " + memoryIndex, e);
    }
  }

  @Override
  public Optional<WasmTable> debugTable(final int tableIndex) {
    ensureNotClosed();
    try {
      final long tableHandle =
          nativeDebugTable(getNativeHandle(), ((JniStore) store).getNativeHandle(), tableIndex);
      if (tableHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTable(tableHandle, (JniStore) store));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in debugTable: " + tableIndex, e);
    }
  }

  @Override
  public Optional<Tag> debugTag(final int tagIndex) {
    ensureNotClosed();
    try {
      final long tagHandle =
          nativeDebugTag(getNativeHandle(), ((JniStore) store).getNativeHandle(), tagIndex);
      if (tagHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTag(tagHandle, ((JniStore) store).getNativeHandle()));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in debugTag: " + tagIndex, e);
    }
  }

  /**
   * Checks if this instance has an export with the given name.
   *
   * @param name the export name to check
   * @return true if the export exists
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  public boolean hasExport(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      return nativeHasExport(getNativeHandle(), name);
    } catch (final Exception e) {
      LOGGER.warning("Error checking export existence: " + e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<Extern> getExport(final String name) {
    Validation.requireNonBlank(name, "name");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    try {
      final JniStore jniStore = (JniStore) store;
      final long storeHandle = jniStore.getNativeHandle();

      // Try function
      final long funcHandle = nativeGetFunction(getNativeHandle(), storeHandle, name);
      if (funcHandle != 0) {
        return Optional.of(new JniExternFunc(funcHandle, jniStore));
      }

      // Try memory
      final long memHandle = nativeGetMemory(getNativeHandle(), storeHandle, name);
      if (memHandle != 0) {
        return Optional.of(new JniExternMemory(memHandle, jniStore));
      }

      // Try table
      final long tableHandle = nativeGetTable(getNativeHandle(), storeHandle, name);
      if (tableHandle != 0) {
        return Optional.of(new JniExternTable(tableHandle, jniStore));
      }

      // Try global
      final long globalHandle = nativeGetGlobal(getNativeHandle(), storeHandle, name);
      if (globalHandle != 0) {
        return Optional.of(new JniExternGlobal(globalHandle, jniStore));
      }

      return Optional.empty();
    } catch (final Exception e) {
      LOGGER.warning("Error getting export: " + name + " - " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<ai.tegmentum.wasmtime4j.Extern> getExport(
      final ai.tegmentum.wasmtime4j.Store store,
      final ai.tegmentum.wasmtime4j.ModuleExport moduleExport)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleExport == null) {
      throw new IllegalArgumentException("ModuleExport cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    final long storeHandle = jniStore.getNativeHandle();
    final int[] outType = new int[1];

    try {
      final long externHandle =
          nativeGetModuleExport(
              getNativeHandle(), storeHandle, moduleExport.nativeHandle(), outType);

      if (externHandle == 0) {
        return Optional.empty();
      }

      return Optional.of(createExternFromNative(externHandle, outType[0], jniStore));
    } catch (final Exception e) {
      LOGGER.warning("Error getting module export: " + e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Creates the appropriate Extern wrapper from a native handle and type code.
   *
   * @param handle the native extern handle
   * @param nativeType the native type code (0=Func, 1=Global, 2=Table, 3=Memory, 4=SharedMem,
   *     5=Tag)
   * @param jniStore the JNI store
   * @return the Extern wrapper
   */
  private static ai.tegmentum.wasmtime4j.Extern createExternFromNative(
      final long handle, final int nativeType, final JniStore jniStore) {
    switch (nativeType) {
      case 0:
        return new JniExternFunc(handle, jniStore);
      case 1:
        return new JniExternGlobal(handle, jniStore);
      case 2:
        return new JniExternTable(handle, jniStore);
      case 3:
        return new JniExternMemory(handle, jniStore);
      default:
        LOGGER.warning("Unknown native extern type: " + nativeType);
        return new JniExternFunc(handle, jniStore);
    }
  }

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name
   */
  @Override
  protected String getResourceType() {
    return "Instance";
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, Instances are owned by the Store that created them. The native Instance
   * resources will be automatically freed when the Store is destroyed. We don't call
   * nativeDestroyInstance here to avoid corrupting the Store's internal slab state.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    // Instance resources are owned by Store and will be freed when Store is destroyed.
    // Explicitly destroying Instance before Store can corrupt wasmtime's internal slab state.
    LOGGER.fine(
        "Instance marked as closed (handle: 0x"
            + Long.toHexString(nativeHandle)
            + "). Native resources will be freed when Store is destroyed.");
  }

  @Override
  public String[] getExportNames() {
    ensureNotClosed();
    return nativeGetExportNames(getNativeHandle());
  }

  @Override
  public Module getModule() {
    ensureNotClosed();
    return module;
  }

  @Override
  public Store getStore() {
    ensureNotClosed();
    return store;
  }

  @Override
  public boolean isValid() {
    return !isClosed() && getNativeHandle() != 0;
  }

  @Override
  public WasmValue[] callFunction(final String functionName, final WasmValue... params)
      throws WasmException {
    Validation.requireNonBlank(functionName, "functionName");
    Validation.requireNonNull(params, "params");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalStateException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;

    // Pass WasmValue array directly to preserve type information
    // Native code now returns WasmValue[] directly, preserving type information
    final Object[] nativeResults =
        nativeCallFunction(getNativeHandle(), jniStore.getNativeHandle(), functionName, params);

    if (nativeResults == null) {
      return new WasmValue[0];
    }

    // Cast to WasmValue[] - native code returns WasmValue objects directly
    final WasmValue[] results = new WasmValue[nativeResults.length];
    for (int i = 0; i < nativeResults.length; i++) {
      if (nativeResults[i] instanceof WasmValue) {
        results[i] = (WasmValue) nativeResults[i];
      } else if (nativeResults[i] == null) {
        // Null represents null externref or funcref - shouldn't happen with new approach
        results[i] = WasmValue.externref(null);
      } else {
        throw new WasmException(
            "Unexpected return type: " + nativeResults[i].getClass() + " at index " + i);
      }
    }

    return results;
  }

  // Native method declarations

  /**
   * Gets a function export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param storeHandle the native store handle
   * @param name the function name
   * @return native function handle or 0 if not found
   */
  private static native long nativeGetFunction(long instanceHandle, long storeHandle, String name);

  /**
   * Gets a memory export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the memory name
   * @return native memory handle or 0 if not found
   */
  private static native long nativeGetMemory(long instanceHandle, long storeHandle, String name);

  /**
   * Gets a shared memory export from an instance (only shared memories).
   *
   * @param instanceHandle the native instance handle
   * @param storeHandle the native store handle
   * @param name the shared memory name
   * @return native memory handle or 0 if not found
   */
  private static native long nativeGetSharedMemory(
      long instanceHandle, long storeHandle, String name);

  /**
   * Gets a table export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the table name
   * @return native table handle or 0 if not found
   */
  private static native long nativeGetTable(long instanceHandle, long storeHandle, String name);

  /**
   * Gets a global export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the global name
   * @return native global handle or 0 if not found
   */
  private static native long nativeGetGlobal(long instanceHandle, long storeHandle, String name);

  private static native long nativeGetTag(long instanceHandle, long storeHandle, String name);

  private static native long nativeDebugFunction(
      long instanceHandle, long storeHandle, int functionIndex);

  private static native long nativeDebugGlobal(
      long instanceHandle, long storeHandle, int globalIndex);

  private static native long nativeDebugMemory(
      long instanceHandle, long storeHandle, int memoryIndex);

  private static native long nativeDebugSharedMemory(
      long instanceHandle, long storeHandle, int memoryIndex);

  private static native long nativeDebugTable(
      long instanceHandle, long storeHandle, int tableIndex);

  private static native long nativeDebugTag(long instanceHandle, long storeHandle, int tagIndex);

  /**
   * Checks if an instance has an export with the given name.
   *
   * @param instanceHandle the native instance handle
   * @param name the export name
   * @return true if the export exists
   */
  private static native boolean nativeHasExport(long instanceHandle, String name);

  /**
   * Gets all export names from an instance.
   *
   * @param instanceHandle the native instance handle
   * @return array of export names
   */
  private static native String[] nativeGetExportNames(long instanceHandle);

  private static native long nativeGetModuleExport(
      long instanceHandle, long storeHandle, long moduleExportHandle, int[] outType);

  /**
   * Calls a WebAssembly function directly without extracting a Function object.
   *
   * @param instanceHandle the native instance handle
   * @param storeHandle the native store handle
   * @param functionName the name of the function to call
   * @param params array of parameters (Integer, Long, Float, Double)
   * @return array of results (Integer, Long, Float, Double)
   */
  private static native Object[] nativeCallFunction(
      long instanceHandle, long storeHandle, String functionName, Object[] params);
}
