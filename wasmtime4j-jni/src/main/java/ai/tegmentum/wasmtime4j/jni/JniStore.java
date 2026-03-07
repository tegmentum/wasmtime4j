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
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.config.ResourceLimiter;
import ai.tegmentum.wasmtime4j.config.ResourceLimiterAsync;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.util.Validation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * JNI implementation of the WebAssembly Store.
 *
 * <p>This class represents a WebAssembly store, which serves as an execution context for
 * WebAssembly instances. A store manages the runtime state of WebAssembly instances including
 * memory, globals, tables, and functions. All WebAssembly instances must be created within a store
 * context, and instances from different stores cannot interact directly.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource management with {@link AutoCloseable}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Thread-safe operations
 *   <li>Execution context management for WebAssembly instances
 *   <li>Resource isolation between different stores
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (JniEngine engine = JniEngine.create();
 *      JniStore store = engine.createStore()) {
 *
 *   // Compile module
 *   JniModule module = engine.compileModule(wasmBytes);
 *
 *   // Create instance within this store
 *   try (JniInstance instance = module.instantiate(store)) {
 *     // All operations on the instance are within this store context
 *     JniFunction exportedFunction = instance.getFunction("my_function");
 *     Object[] results = exportedFunction.call(args);
 *   }
 * }
 * }</pre>
 *
 * <p>Store Lifecycle:
 *
 * <ul>
 *   <li>Stores are created by engines using {@link JniEngine#createStore()}
 *   <li>Instances created within a store are tied to that store's lifetime
 *   <li>Closing a store invalidates all instances created within it
 *   <li>Stores cannot be shared between threads safely
 * </ul>
 *
 * <p>This implementation extends {@link JniResource} to provide automatic native resource
 * management and follows defensive programming practices to prevent native crashes.
 *
 * @since 1.0.0
 */
public final class JniStore extends JniResource implements Store {

  private static final Logger LOGGER = Logger.getLogger(JniStore.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniStore: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Custom data associated with this store. */
  private volatile Object customData;

  /** The engine that created this store. */
  private final Engine engine;

  /** Callback registry for managing callbacks and asynchronous operations. Lazily initialized. */
  private volatile CallbackRegistry callbackRegistry;

  /** Tracked WASI context reference for getWasiContext() and reapplyWasiContext(). */
  private volatile JniWasiContextImpl trackedWasiContext;

  /**
   * Creates a new JNI store with the given native handle.
   *
   * <p>This constructor is package-private and should only be used by the JniEngine or other JNI
   * classes. External code should create stores through {@link JniEngine#createStore()}.
   *
   * @param nativeHandle the native store handle from Wasmtime
   * @param engine the engine that created this store
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniStore(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    this.engine = engine;
    LOGGER.fine("Created JNI store with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Creates a new store that is compatible with the given module.
   *
   * <p><b>CRITICAL:</b> This method creates a Store that shares the exact same Engine Arc as the
   * module. This is required because Wasmtime's Instance::new() uses Arc::ptr_eq() to verify that
   * the Store and Module were created from the same Engine. Using {@link JniEngine#createStore()}
   * will create a Store with a different Arc clone, causing "cross-Engine instantiation is not
   * currently supported" errors.
   *
   * <p>Usage Example:
   *
   * <pre>{@code
   * try (JniEngine engine = JniEngine.create()) {
   *   JniModule module = (JniModule) engine.compileModule(wasmBytes);
   *
   *   // CORRECT: Use forModule to create a compatible store
   *   try (JniStore store = JniStore.forModule(module)) {
   *     JniInstance instance = (JniInstance) module.instantiate(store);
   *     // Instance created successfully
   *   }
   * }
   * }</pre>
   *
   * @param module the module to create a compatible store for
   * @return a new store that shares the same Engine Arc as the module
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if module is null or not a JniModule
   */
  public static JniStore forModule(final Module module) throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("module cannot be null");
    }
    if (!(module instanceof JniModule)) {
      throw new IllegalArgumentException("module must be a JniModule instance");
    }

    final JniModule jniModule = (JniModule) module;
    if (!jniModule.isValid()) {
      throw new IllegalStateException("Module is not valid");
    }

    final long storeHandle = nativeCreateStoreForModule(jniModule.getNativeHandle());
    if (storeHandle == 0) {
      throw new WasmException("Failed to create store for module");
    }

    return new JniStore(storeHandle, jniModule.getEngine());
  }

  /**
   * Adds fuel to this store.
   *
   * <p>This method adds additional fuel to the store's fuel limit. This can be used to extend
   * execution time for long-running WebAssembly computations.
   *
   * @param additionalFuel the amount of fuel to add (must be positive)
   * @throws WasmException if fuel cannot be added
   * @throws JniResourceException if this store has been closed
   */
  @Override
  public void addFuel(final long additionalFuel) throws WasmException {
    Validation.requireNonNegative(additionalFuel, "additionalFuel");
    beginOperation();
    try {
      final boolean success = nativeAddFuel(getNativeHandle(), additionalFuel);
      if (!success) {
        throw new WasmException("Failed to add fuel: " + additionalFuel);
      }
      LOGGER.fine(
          "Added " + additionalFuel + " fuel to store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error adding fuel", e);
    } finally {
      endOperation();
    }
  }

  // Interface implementation methods

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public Object getData() {
    return customData;
  }

  @Override
  public void setData(final Object data) {
    this.customData = data;
  }

  @Override
  public void setResourceLimiter(final ResourceLimiter limiter) throws WasmException {
    if (limiter == null) {
      throw new IllegalArgumentException("ResourceLimiter cannot be null");
    }
    beginOperation();
    try {
      this.resourceLimiter = limiter;
      nativeSetResourceLimiter(getNativeHandle());
      LOGGER.fine("Set resource limiter on store 0x" + Long.toHexString(getNativeHandle()));
    } finally {
      endOperation();
    }
  }

  @Override
  public void setResourceLimiterAsync(final ResourceLimiterAsync limiter) throws WasmException {
    if (limiter == null) {
      throw new IllegalArgumentException("ResourceLimiterAsync cannot be null");
    }
    beginOperation();
    try {
      // Clear any sync limiter since only one can be active
      this.resourceLimiter = null;
      this.resourceLimiterAsync = limiter;
      nativeSetResourceLimiterAsync(getNativeHandle());
      LOGGER.fine("Set async resource limiter on store 0x" + Long.toHexString(getNativeHandle()));
    } finally {
      endOperation();
    }
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    Validation.requireNonNegative(fuel, "fuel");
    beginOperation();
    try {
      final boolean success = nativeSetFuel(getNativeHandle(), fuel);
      if (!success) {
        throw new WasmException("Failed to set fuel to " + fuel);
      }
      LOGGER.fine("Set fuel to " + fuel + " for store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting fuel", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long getFuel() throws WasmException {
    beginOperation();
    try {
      return nativeGetFuelRemaining(getNativeHandle());
    } catch (final Exception e) {
      throw new WasmException("Failed to get remaining fuel", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void setEpochDeadline(final long ticks) throws WasmException {
    beginOperation();
    try {
      final boolean success = nativeSetEpochDeadline(getNativeHandle(), ticks);
      if (!success) {
        throw new WasmException("Failed to set epoch deadline to " + ticks);
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting epoch deadline", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long consumeFuel(final long fuel) throws WasmException {
    Validation.requireNonNegative(fuel, "fuel");
    beginOperation();
    try {
      final long consumed = nativeConsumeFuel(getNativeHandle(), fuel);
      if (consumed < 0) {
        throw new WasmException("Failed to consume fuel: " + fuel);
      }
      return consumed;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error consuming fuel", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long hostcallFuel() throws WasmException {
    beginOperation();
    try {
      final long fuel = nativeGetHostcallFuel(getNativeHandle());
      if (fuel < 0) {
        throw new WasmException("Failed to get hostcall fuel");
      }
      return fuel;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error getting hostcall fuel", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void setHostcallFuel(final long fuel) throws WasmException {
    Validation.requireNonNegative(fuel, "fuel");
    beginOperation();
    try {
      final boolean success = nativeSetHostcallFuel(getNativeHandle(), fuel);
      if (!success) {
        throw new WasmException("Failed to set hostcall fuel to " + fuel);
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting hostcall fuel", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public WasmFunction createHostFunction(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(functionType, "functionType cannot be null");
    Objects.requireNonNull(implementation, "implementation cannot be null");
    beginOperation();
    try {
      // Create the JNI host function wrapper
      final JniHostFunction hostFunction =
          new JniHostFunction(name, functionType, implementation, this);

      LOGGER.fine(
          "Created host function '" + name + "' in store 0x" + Long.toHexString(getNativeHandle()));
      return hostFunction;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create host function: " + name, e);
    } finally {
      endOperation();
    }
  }

  @Override
  public WasmFunction createHostFunctionUnchecked(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(functionType, "functionType cannot be null");
    Objects.requireNonNull(implementation, "implementation cannot be null");
    beginOperation();
    try {
      final JniHostFunction hostFunction =
          JniHostFunction.createUnchecked(name, functionType, implementation, this);

      LOGGER.fine(
          "Created unchecked host function '"
              + name
              + "' in store 0x"
              + Long.toHexString(getNativeHandle()));
      return hostFunction;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create unchecked host function: " + name, e);
    } finally {
      endOperation();
    }
  }

  /**
   * Sets the tracked WASI context for this store.
   *
   * <p>Called internally when a WASI context is applied to this store during instantiation.
   *
   * @param wasiCtx the WASI context to track
   */
  void setTrackedWasiContext(final JniWasiContextImpl wasiCtx) {
    this.trackedWasiContext = wasiCtx;
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.wasi.WasiContext> getWasiContext() {
    return java.util.Optional.ofNullable(trackedWasiContext);
  }

  @Override
  public void reapplyWasiContext() throws WasmException {
    final JniWasiContextImpl wasiCtx = trackedWasiContext;
    if (wasiCtx == null) {
      throw new WasmException("No WASI context is configured for this store");
    }
    beginOperation();
    try {
      final int result = nativeReapplyWasiContext(getNativeHandle(), wasiCtx.getNativeHandle());
      if (result != 0) {
        throw new WasmException("Failed to re-apply WASI context to store");
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public WasmGlobal createGlobal(
      final WasmValueType valueType, final boolean isMutable, final WasmValue initialValue)
      throws WasmException {
    Validation.requireNonNull(valueType, "valueType");
    Validation.requireNonNull(initialValue, "initialValue");
    beginOperation();
    try {
      // Validate that the initial value matches the specified type
      if (initialValue.getType() != valueType) {
        throw new IllegalArgumentException(
            "Initial value type "
                + initialValue.getType()
                + " does not match global type "
                + valueType);
      }

      try {
        // Call native method to create global
        final long globalHandle =
            nativeCreateGlobal(
                getNativeHandle(),
                valueType.toNativeTypeCode(),
                isMutable ? 1 : 0,
                extractValueComponents(initialValue));

        if (globalHandle == 0) {
          throw new JniException("Native global creation returned null handle");
        }

        // Create JniGlobal wrapper
        final JniGlobal global = new JniGlobal(globalHandle, this);
        LOGGER.fine(
            "Created global with type "
                + valueType
                + ", mutable="
                + isMutable
                + ", handle=0x"
                + Long.toHexString(globalHandle));
        return global;

      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to create global variable", e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final WasmValueType elementType, final int initialSize, final int maxSize)
      throws WasmException {
    Validation.requireNonNull(elementType, "elementType");
    if (initialSize < 0) {
      throw new IllegalArgumentException("Initial size cannot be negative: " + initialSize);
    }
    if (maxSize < -1) {
      throw new IllegalArgumentException("Max size must be -1 (unlimited) or >= 0: " + maxSize);
    }
    if (maxSize != -1 && maxSize < initialSize) {
      throw new IllegalArgumentException(
          "Max size (" + maxSize + ") cannot be less than initial size (" + initialSize + ")");
    }
    if (elementType != WasmValueType.FUNCREF && elementType != WasmValueType.EXTERNREF) {
      throw new IllegalArgumentException(
          "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
    }
    beginOperation();
    try {
      final long tableHandle =
          nativeCreateTable(
              getNativeHandle(), elementType.toNativeTypeCode(), initialSize, maxSize);

      if (tableHandle == 0) {
        throw new JniException("Native table creation returned null handle");
      }

      final JniTable table = new JniTable(tableHandle, this);
      LOGGER.fine(
          "Created table with element type "
              + elementType
              + ", size="
              + initialSize
              + ", max="
              + maxSize
              + ", handle=0x"
              + Long.toHexString(tableHandle));
      return table;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create table", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final WasmValueType elementType,
      final int initialSize,
      final int maxSize,
      final ai.tegmentum.wasmtime4j.WasmValue initValue)
      throws WasmException {
    Validation.requireNonNull(elementType, "elementType");
    Validation.requireNonNull(initValue, "initValue");
    if (initialSize < 0) {
      throw new IllegalArgumentException("Initial size cannot be negative: " + initialSize);
    }
    if (maxSize < -1) {
      throw new IllegalArgumentException("Max size must be -1 (unlimited) or >= 0: " + maxSize);
    }
    if (maxSize != -1 && maxSize < initialSize) {
      throw new IllegalArgumentException(
          "Max size (" + maxSize + ") cannot be less than initial size (" + initialSize + ")");
    }
    if (elementType != WasmValueType.FUNCREF && elementType != WasmValueType.EXTERNREF) {
      throw new IllegalArgumentException(
          "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
    }
    beginOperation();
    try {
      final long initRefId = wasmValueToRefHandle(initValue);
      final long tableHandle =
          nativeCreateTableWithInit(
              getNativeHandle(), elementType.toNativeTypeCode(), initialSize, maxSize, initRefId);

      if (tableHandle == 0) {
        throw new JniException("Native table creation with init value returned null handle");
      }

      final JniTable table = new JniTable(tableHandle, this);
      LOGGER.fine(
          "Created table with init value: elementType="
              + elementType
              + ", size="
              + initialSize
              + ", max="
              + maxSize
              + ", handle=0x"
              + Long.toHexString(tableHandle));
      return table;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create table with init value", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmMemory createMemory(final int initialPages, final int maxPages)
      throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative: " + initialPages);
    }
    if (maxPages < -1) {
      throw new IllegalArgumentException("Max pages must be -1 (unlimited) or >= 0: " + maxPages);
    }
    if (maxPages != -1 && maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
    }
    beginOperation();
    try {
      final long memoryHandle = nativeCreateMemory(getNativeHandle(), initialPages, maxPages);

      if (memoryHandle == 0) {
        throw new JniException("Native memory creation returned null handle");
      }

      final JniMemory memory = new JniMemory(memoryHandle, this);
      LOGGER.fine(
          "Created memory with initial="
              + initialPages
              + " pages, max="
              + maxPages
              + " pages, handle=0x"
              + Long.toHexString(memoryHandle));
      return memory;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create memory", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmMemory createSharedMemory(
      final int initialPages, final int maxPages) throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative: " + initialPages);
    }
    if (maxPages < 1) {
      throw new IllegalArgumentException("Shared memory requires a positive maximum page count");
    }
    if (maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
    }
    beginOperation();
    try {
      final long memoryHandle = nativeCreateSharedMemory(getNativeHandle(), initialPages, maxPages);

      if (memoryHandle == 0) {
        throw new JniException("Native shared memory creation returned null handle");
      }

      final JniMemory memory = new JniMemory(memoryHandle, this);
      LOGGER.fine(
          "Created shared memory with initial="
              + initialPages
              + " pages, max="
              + maxPages
              + " pages, handle=0x"
              + Long.toHexString(memoryHandle));
      return memory;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create shared memory", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmMemory createMemory(
      final ai.tegmentum.wasmtime4j.type.MemoryType memoryType) throws WasmException {
    Validation.requireNonNull(memoryType, "memoryType");
    beginOperation();
    try {
      final long minPages = memoryType.getMinimum();
      final long maxPages = memoryType.getMaximum().orElse(-1L);
      final int isShared = memoryType.isShared() ? 1 : 0;
      final int is64 = memoryType.is64Bit() ? 1 : 0;

      if (minPages < 0) {
        throw new IllegalArgumentException("Minimum pages cannot be negative: " + minPages);
      }
      if (maxPages != -1 && maxPages < minPages) {
        throw new IllegalArgumentException(
            "Maximum pages ("
                + maxPages
                + ") cannot be less than minimum pages ("
                + minPages
                + ")");
      }
      if (memoryType.isShared() && maxPages < 1) {
        throw new IllegalArgumentException("Shared memory requires a positive maximum page count");
      }

      try {
        final long memoryHandle =
            nativeCreateMemoryWithType(getNativeHandle(), minPages, maxPages, isShared, is64);

        if (memoryHandle == 0) {
          throw new JniException("Native memory creation with type returned null handle");
        }

        final JniMemory memory = new JniMemory(memoryHandle, this);
        LOGGER.fine(
            "Created memory from type: min="
                + minPages
                + ", max="
                + maxPages
                + ", shared="
                + memoryType.isShared()
                + ", 64bit="
                + memoryType.is64Bit()
                + ", handle=0x"
                + Long.toHexString(memoryHandle));
        return memory;

      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to create memory from type", e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final ai.tegmentum.wasmtime4j.type.TableType tableType) throws WasmException {
    Validation.requireNonNull(tableType, "tableType");
    beginOperation();
    try {
      final WasmValueType elementType = tableType.getElementType();
      final long minSize = tableType.getMinimum();
      final long maxSize = tableType.getMaximum().orElse(-1L);
      final boolean is64 = tableType.is64Bit();

      if (elementType != WasmValueType.FUNCREF && elementType != WasmValueType.EXTERNREF) {
        throw new IllegalArgumentException(
            "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
      }
      if (minSize < 0) {
        throw new IllegalArgumentException("Minimum size cannot be negative: " + minSize);
      }
      if (maxSize != -1 && maxSize < minSize) {
        throw new IllegalArgumentException(
            "Maximum size (" + maxSize + ") cannot be less than minimum size (" + minSize + ")");
      }

      try {
        final long tableHandle;
        if (is64) {
          final int hasMaximum = (maxSize == -1) ? 0 : 1;
          final long maximumSize = (maxSize == -1) ? 0 : maxSize;
          tableHandle =
              nativeCreateTable64(
                  getNativeHandle(),
                  elementType.toNativeTypeCode(),
                  minSize,
                  hasMaximum,
                  maximumSize);
        } else {
          tableHandle =
              nativeCreateTable(
                  getNativeHandle(),
                  elementType.toNativeTypeCode(),
                  (int) minSize,
                  maxSize == -1 ? -1 : (int) maxSize);
        }

        if (tableHandle == 0) {
          throw new JniException("Native table creation with type returned null handle");
        }

        final JniTable table = new JniTable(tableHandle, this);
        LOGGER.fine(
            "Created table from type: elementType="
                + elementType
                + ", min="
                + minSize
                + ", max="
                + maxSize
                + ", 64bit="
                + is64
                + ", handle=0x"
                + Long.toHexString(tableHandle));
        return table;

      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to create table from type", e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final ai.tegmentum.wasmtime4j.type.TableType tableType,
      final ai.tegmentum.wasmtime4j.WasmValue initValue)
      throws WasmException {
    Validation.requireNonNull(tableType, "tableType");
    Validation.requireNonNull(initValue, "initValue");
    beginOperation();
    try {
      final WasmValueType elementType = tableType.getElementType();
      final long minSize = tableType.getMinimum();
      final long maxSize = tableType.getMaximum().orElse(-1L);

      if (elementType != WasmValueType.FUNCREF && elementType != WasmValueType.EXTERNREF) {
        throw new IllegalArgumentException(
            "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
      }
      if (minSize < 0) {
        throw new IllegalArgumentException("Minimum size cannot be negative: " + minSize);
      }
      if (maxSize != -1 && maxSize < minSize) {
        throw new IllegalArgumentException(
            "Maximum size (" + maxSize + ") cannot be less than minimum size (" + minSize + ")");
      }

      try {
        final long initRefId = wasmValueToRefHandle(initValue);
        final long tableHandle =
            nativeCreateTableWithInit(
                getNativeHandle(),
                elementType.toNativeTypeCode(),
                (int) minSize,
                maxSize == -1 ? -1 : (int) maxSize,
                initRefId);

        if (tableHandle == 0) {
          throw new JniException(
              "Native table creation with type and init value returned null handle");
        }

        final JniTable table = new JniTable(tableHandle, this);
        LOGGER.fine(
            "Created table from type with init value: elementType="
                + elementType
                + ", min="
                + minSize
                + ", max="
                + maxSize
                + ", handle=0x"
                + Long.toHexString(tableHandle));
        return table;

      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to create table from type with init value", e);
      }
    } finally {
      endOperation();
    }
  }

  private long wasmValueToRefHandle(final ai.tegmentum.wasmtime4j.WasmValue initValue) {
    if (initValue.getValue() == null) {
      return 0L;
    }
    final Object val = initValue.getValue();
    if (val instanceof Long) {
      return (Long) val;
    }
    if (val instanceof JniFunctionReference) {
      return ((JniFunctionReference) val).getNativeHandle();
    }
    // Null ref for unsupported value types
    return 0L;
  }

  @Override
  public FunctionReference createFunctionReference(
      final HostFunction implementation, final FunctionType functionType) throws WasmException {
    Objects.requireNonNull(implementation, "Host function implementation cannot be null");
    Objects.requireNonNull(functionType, "Function type cannot be null");
    beginOperation();
    try {
      return new JniFunctionReference(implementation, functionType, this);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create function reference from host function", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public FunctionReference createFunctionReference(final WasmFunction function)
      throws WasmException {
    Objects.requireNonNull(function, "WebAssembly function cannot be null");
    beginOperation();
    try {
      return new JniFunctionReference(function, this);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create function reference from WebAssembly function", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    if (callbackRegistry == null) {
      synchronized (this) {
        if (callbackRegistry == null) {
          callbackRegistry = new JniCallbackRegistry(this);
        }
      }
    }
    return callbackRegistry;
  }

  @Override
  public Instance createInstance(final Module module) throws WasmException {
    Objects.requireNonNull(module, "Module cannot be null");
    beginOperation();
    try {
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule instance for JNI store");
      }

      JniModule jniModule = (JniModule) module;
      return jniModule.instantiate(this);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create instance from module", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public Instance createInstance(
      final Module module, final ai.tegmentum.wasmtime4j.Extern[] imports) throws WasmException {
    Objects.requireNonNull(module, "Module cannot be null");
    Objects.requireNonNull(imports, "Imports cannot be null");
    beginOperation();
    try {
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule instance for JNI store");
      }

      final JniModule jniModule = (JniModule) module;

      if (imports.length == 0) {
        return jniModule.instantiate(this);
      }

      try {
        final long[] externHandles = new long[imports.length];
        final int[] externTypes = new int[imports.length];

        for (int i = 0; i < imports.length; i++) {
          final ai.tegmentum.wasmtime4j.Extern ext = imports[i];
          if (ext == null) {
            throw new IllegalArgumentException("Import at index " + i + " is null");
          }
          externHandles[i] = extractExternHandle(ext);
          externTypes[i] = externTypeToNativeCode(ext.getType());
        }

        final long instanceHandle =
            nativeCreateInstanceWithImports(
                getNativeHandle(), jniModule.getNativeHandle(), externHandles, externTypes);

        if (instanceHandle == 0) {
          throw new WasmException("Failed to create instance with imports");
        }

        return new JniInstance(instanceHandle, jniModule, this);
      } catch (final WasmException e) {
        throw e;
      } catch (final Exception e) {
        throw new WasmException("Failed to create instance with imports", e);
      }
    } finally {
      endOperation();
    }
  }

  /**
   * Extracts the native handle from a JNI Extern wrapper.
   *
   * @param ext the extern to extract the handle from
   * @return the native handle
   */
  private static long extractExternHandle(final ai.tegmentum.wasmtime4j.Extern ext) {
    if (ext instanceof JniExternFunc) {
      return ((JniExternFunc) ext).getNativeHandle();
    } else if (ext instanceof JniExternGlobal) {
      return ((JniExternGlobal) ext).getNativeHandle();
    } else if (ext instanceof JniExternTable) {
      return ((JniExternTable) ext).getNativeHandle();
    } else if (ext instanceof JniExternMemory) {
      return ((JniExternMemory) ext).getNativeHandle();
    }
    throw new IllegalArgumentException("Unsupported extern type: " + ext.getClass().getName());
  }

  /**
   * Converts a Java ExternType to the native type code used by the Rust FFI layer.
   *
   * <p>Native codes: 0=Func, 1=Global, 2=Table, 3=Memory, 4=SharedMemory, 5=Tag
   *
   * @param type the Java ExternType
   * @return the native type code
   */
  private static int externTypeToNativeCode(final ai.tegmentum.wasmtime4j.type.ExternType type) {
    switch (type) {
      case FUNC:
        return 0;
      case GLOBAL:
        return 1;
      case TABLE:
        return 2;
      case MEMORY:
        return 3;
      case SHARED_MEMORY:
        return 4;
      case TAG:
        return 5;
      default:
        throw new IllegalArgumentException("Unknown extern type: " + type);
    }
  }

  @Override
  public boolean isValid() {
    if (isClosed() || getNativeHandle() == 0) {
      return false;
    }

    try {
      return nativeValidate(getNativeHandle());
    } catch (final Exception e) {
      LOGGER.warning("Error validating store: " + e.getMessage());
      return false;
    }
  }

  @Override
  protected void doClose() throws Exception {
    if (callbackRegistry != null) {
      try {
        callbackRegistry.close();
      } catch (Exception e) {
        LOGGER.warning("Error closing callback registry: " + e.getMessage());
      }
    }

    // Clean up resource limiter callback
    if ((resourceLimiter != null || resourceLimiterAsync != null) && nativeHandle != 0) {
      try {
        nativeClearResourceLimiter(nativeHandle);
      } catch (Exception e) {
        LOGGER.warning("Error clearing resource limiter: " + e.getMessage());
      }
      resourceLimiter = null;
      resourceLimiterAsync = null;
    }

    if (nativeHandle != 0) {
      nativeDestroyStore(nativeHandle);
      LOGGER.fine("Destroyed JNI store with handle: 0x" + Long.toHexString(nativeHandle));
    }
  }

  @Override
  protected String getResourceType() {
    return "Store";
  }

  // Native method declarations

  /**
   * Creates a new store that is compatible with a specific module.
   *
   * <p>CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Engine Arc as the
   * Module's internal wasmtime::Module. This is required because wasmtime's Instance::new() uses
   * Arc::ptr_eq() to verify engine compatibility.
   *
   * @param moduleHandle the native module handle
   * @return the native store handle, or 0 on failure
   */
  private static native long nativeCreateStoreForModule(long moduleHandle);

  /**
   * Adds fuel to a store.
   *
   * @param storeHandle the native store handle
   * @param additionalFuel the amount of fuel to add
   * @return true on success, false on failure
   */
  private static native boolean nativeAddFuel(long storeHandle, long additionalFuel);

  /**
   * Sets fuel to a specific amount (replaces current fuel).
   *
   * @param storeHandle the native store handle
   * @param fuel the fuel amount to set
   * @return true on success, false on failure
   */
  private static native boolean nativeSetFuel(long storeHandle, long fuel);

  /**
   * Gets the remaining fuel for a store.
   *
   * @param storeHandle the native store handle
   * @return remaining fuel or -1 if fuel is not enabled/available
   */
  private static native long nativeGetFuelRemaining(long storeHandle);

  /**
   * Tries to create a store, returning 0 on allocation failure.
   *
   * @param engineHandle the native engine handle
   * @return the native store handle, or 0 on allocation failure
   */
  /**
   * Package-private static helper for tryCreate.
   *
   * @param engineHandle the native engine handle
   * @return the native store handle, or 0 on allocation failure
   */
  static native long nativeTryCreateStore(long engineHandle);

  /**
   * Gets the hostcall fuel limit.
   *
   * @param storeHandle the native store handle
   * @return the hostcall fuel limit, or -1 on failure
   */
  private static native long nativeGetHostcallFuel(long storeHandle);

  /**
   * Sets the hostcall fuel limit.
   *
   * @param storeHandle the native store handle
   * @param fuel the hostcall fuel limit to set
   * @return true on success, false on failure
   */
  private static native boolean nativeSetHostcallFuel(long storeHandle, long fuel);

  /**
   * Sets the epoch deadline for a store.
   *
   * @param storeHandle the native store handle
   * @param ticks the number of epoch ticks before interruption
   * @return true on success, false on failure
   */
  private static native boolean nativeSetEpochDeadline(long storeHandle, long ticks);

  /**
   * Validates the store and checks its current state.
   *
   * @param storeHandle the native store handle
   * @return true if the store is valid, false otherwise
   */
  private static native boolean nativeValidate(long storeHandle);

  /**
   * Consumes a specific amount of fuel from the store.
   *
   * @param storeHandle the native store handle
   * @param fuel the amount of fuel to consume
   * @return the actual amount of fuel consumed
   */
  private static native long nativeConsumeFuel(long storeHandle, long fuel);

  /**
   * Creates a new global variable.
   *
   * @param storeHandle the native store handle
   * @param valueType the WebAssembly value type code
   * @param isMutable 1 if mutable, 0 if immutable
   * @param valueComponents array containing value components [i32, i64, f32, f64, refId]
   * @return the native global handle, or 0 on failure
   */
  private static native long nativeCreateGlobal(
      long storeHandle, int valueType, int isMutable, Object[] valueComponents);

  /**
   * Creates a new WebAssembly table.
   *
   * @param storeHandle the native store handle
   * @param elementType the element type code (FUNCREF or EXTERNREF)
   * @param initialSize the initial number of elements
   * @param maxSize the maximum number of elements (-1 for unlimited)
   * @return the native table handle, or 0 on failure
   */
  private static native long nativeCreateTable(
      long storeHandle, int elementType, int initialSize, int maxSize);

  private static native long nativeCreateTableWithInit(
      long storeHandle, int elementType, int initialSize, int maxSize, long initRefId);

  /**
   * Creates a new WebAssembly linear memory.
   *
   * @param storeHandle the native store handle
   * @param initialPages the initial number of 64KB pages
   * @param maxPages the maximum number of pages (-1 for unlimited)
   * @return the native memory handle, or 0 on failure
   */
  private static native long nativeCreateMemory(long storeHandle, int initialPages, int maxPages);

  private static native long nativeCreateSharedMemory(
      long storeHandle, int initialPages, int maxPages);

  /**
   * Creates a native memory with full type parameters (shared, 64-bit).
   *
   * @param storeHandle the native store handle
   * @param initialPages the initial number of pages
   * @param maxPages the maximum number of pages (-1 for unlimited)
   * @param isShared 1 if shared, 0 if not
   * @param is64 1 if 64-bit addressing, 0 if 32-bit
   * @return the native memory handle, or 0 on failure
   */
  private static native long nativeCreateMemoryWithType(
      long storeHandle, long initialPages, long maxPages, int isShared, int is64);

  /**
   * Creates a native memory asynchronously (async resource limiter path).
   *
   * @param storeHandle the native store handle
   * @param initialPages the initial number of pages
   * @param maxPages the maximum number of pages (-1 for unlimited)
   * @param isShared 1 if shared, 0 if not
   * @param is64 1 if 64-bit addressing, 0 if 32-bit
   * @return the native memory handle, or 0 on failure
   */
  private static native long nativeCreateMemoryAsync(
      long storeHandle, long initialPages, long maxPages, int isShared, int is64);

  /**
   * Creates a native table asynchronously (async resource limiter path).
   *
   * @param storeHandle the native store handle
   * @param elementType the element type code
   * @param initialSize the initial number of elements
   * @param maxSize the maximum number of elements (-1 for unlimited)
   * @return the native table handle, or 0 on failure
   */
  private static native long nativeCreateTableAsync(
      long storeHandle, int elementType, int initialSize, int maxSize);

  /**
   * Creates a native 64-bit table.
   *
   * @param storeHandle the native store handle
   * @param elementType the element type code
   * @param initialSize the initial number of elements
   * @param hasMaximum 1 if maximum is specified, 0 if unlimited
   * @param maximumSize the maximum number of elements
   * @return the native table handle, or 0 on failure
   */
  private static native long nativeCreateTable64(
      long storeHandle, int elementType, long initialSize, int hasMaximum, long maximumSize);

  /**
   * Destroys a native store and releases all associated resources.
   *
   * @param storeHandle the native store handle
   */
  /**
   * Creates a new instance with explicit imports.
   *
   * @param storeHandle the native store handle
   * @param moduleHandle the native module handle
   * @param externHandles array of native extern handles
   * @param externTypes array of native type codes (0=Func, 1=Global, 2=Table, 3=Memory,
   *     4=SharedMemory, 5=Tag)
   * @return the native instance handle, or 0 on failure
   */
  private static native long nativeCreateInstanceWithImports(
      long storeHandle, long moduleHandle, long[] externHandles, int[] externTypes);

  private static native void nativeDestroyStore(long storeHandle);

  private static native int nativeReapplyWasiContext(long storeHandle, long wasiContextHandle);

  /**
   * Extracts value components from WasmValue for passing to native code.
   *
   * @param value the WasmValue to extract components from
   * @return array containing [i32Value, i64Value, f32Value, f64Value, refValue]
   */
  private Object[] extractValueComponents(final WasmValue value) {
    final Object[] components = new Object[5];

    switch (value.getType()) {
      case I32:
        components[0] = value.asInt();
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = null;
        break;
      case I64:
        components[0] = 0;
        components[1] = value.asLong();
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = null;
        break;
      case F32:
        components[0] = 0;
        components[1] = 0L;
        components[2] = value.asFloat();
        components[3] = 0.0;
        components[4] = null;
        break;
      case F64:
        components[0] = 0;
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = value.asDouble();
        components[4] = null;
        break;
      case V128:
        // For V128, we'll store as byte array in the reference slot
        components[0] = 0;
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = value.asV128();
        break;
      case FUNCREF:
      case EXTERNREF:
        components[0] = 0;
        components[1] = 0L;
        components[2] = 0.0f;
        components[3] = 0.0;
        components[4] = value.getValue();
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }

    return components;
  }

  @Override
  public void gc() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      nativeGc(nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Deprecated
  @Override
  public <R> R throwException(final ai.tegmentum.wasmtime4j.ExnRef exceptionRef)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (exceptionRef == null) {
      throw new IllegalArgumentException("exceptionRef cannot be null");
    }
    throw new UnsupportedOperationException(
        "Wasmtime does not support host-initiated exception throwing. "
            + "Exceptions propagate only from WASM throw/throw_ref instructions. "
            + "Use takePendingException() instead.");
  }

  @Override
  public ai.tegmentum.wasmtime4j.ExnRef takePendingException()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      final long exnRefHandle = nativeTakePendingException(nativeHandle);
      if (exnRefHandle == 0) {
        return null;
      }
      return new JniExnRef(exnRefHandle, nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean hasPendingException() {
    if (!tryBeginOperation()) {
      return false;
    }
    try {
      return nativeHasPendingException(nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public void epochDeadlineAsyncYieldAndUpdate(final long deltaTicks)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      if (deltaTicks < 0) {
        throw new IllegalArgumentException("deltaTicks cannot be negative");
      }
      nativeEpochDeadlineAsyncYieldAndUpdate(nativeHandle, deltaTicks);
    } finally {
      endOperation();
    }
  }

  @Override
  public void epochDeadlineTrap() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      nativeEpochDeadlineTrap(nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public void epochDeadlineCallback(
      final ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback callback)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      // Store callback reference to prevent GC
      this.epochDeadlineCallback = callback;
      if (callback == null) {
        nativeClearEpochDeadlineCallback(nativeHandle);
      } else {
        nativeSetEpochDeadlineCallback(nativeHandle);
      }
    } finally {
      endOperation();
    }
  }

  // Callback holder to prevent garbage collection
  private ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback epochDeadlineCallback;

  /** Resource limiter callback holder to prevent garbage collection. */
  private ResourceLimiter resourceLimiter;

  /** Async resource limiter callback holder to prevent garbage collection. */
  private ResourceLimiterAsync resourceLimiterAsync;

  // Called from native code when epoch deadline is reached
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private long onEpochDeadlineReached(final long currentEpoch) {
    if (epochDeadlineCallback == null) {
      return 0; // Signal trap
    }
    final ai.tegmentum.wasmtime4j.Store.EpochDeadlineAction action =
        epochDeadlineCallback.onEpochDeadline(currentEpoch);
    if (action.shouldContinue()) {
      return action.getDeltaTicks(); // Positive = continue
    }
    if (action.shouldYield()) {
      return -action.getDeltaTicks(); // Negative = yield
    }
    return 0; // Signal trap
  }

  // Called from native code when memory growth is requested
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private boolean onMemoryGrowing(
      final long currentBytes, final long desiredBytes, final long maximumBytes) {
    if (resourceLimiter != null) {
      return resourceLimiter.memoryGrowing(currentBytes, desiredBytes, maximumBytes);
    }
    if (resourceLimiterAsync != null) {
      try {
        return resourceLimiterAsync.memoryGrowing(currentBytes, desiredBytes, maximumBytes).join();
      } catch (final Exception e) {
        LOGGER.warning("Async memoryGrowing callback failed: " + e.getMessage());
        return false;
      }
    }
    return true; // Allow by default
  }

  // Called from native code when table growth is requested
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private boolean onTableGrowing(
      final int currentElements, final int desiredElements, final int maximumElements) {
    if (resourceLimiter != null) {
      return resourceLimiter.tableGrowing(currentElements, desiredElements, maximumElements);
    }
    if (resourceLimiterAsync != null) {
      try {
        return resourceLimiterAsync
            .tableGrowing(currentElements, desiredElements, maximumElements)
            .join();
      } catch (final Exception e) {
        LOGGER.warning("Async tableGrowing callback failed: " + e.getMessage());
        return false;
      }
    }
    return true; // Allow by default
  }

  // Called from native code when memory growth fails
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private void onMemoryGrowFailed(final String error) {
    if (resourceLimiter != null) {
      resourceLimiter.memoryGrowFailed(error);
    } else if (resourceLimiterAsync != null) {
      resourceLimiterAsync.memoryGrowFailed(error);
    }
  }

  // Called from native code when table growth fails
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private void onTableGrowFailed(final String error) {
    if (resourceLimiter != null) {
      resourceLimiter.tableGrowFailed(error);
    } else if (resourceLimiterAsync != null) {
      resourceLimiterAsync.tableGrowFailed(error);
    }
  }

  private native void nativeGc(long storeHandle);

  private native long nativeTakePendingException(long storeHandle);

  private native boolean nativeHasPendingException(long storeHandle);

  private static native void nativeEpochDeadlineAsyncYieldAndUpdate(
      long storeHandle, long deltaTicks);

  private static native void nativeEpochDeadlineTrap(long storeHandle);

  private native void nativeSetEpochDeadlineCallback(long storeHandle);

  private native void nativeClearEpochDeadlineCallback(long storeHandle);

  // Debug handler support
  private ai.tegmentum.wasmtime4j.debug.DebugHandler debugHandler;

  @Override
  public void setDebugHandler(final ai.tegmentum.wasmtime4j.debug.DebugHandler handler) {
    if (handler == null) {
      throw new NullPointerException("handler cannot be null");
    }
    beginOperation();
    try {
      this.debugHandler = handler;
      nativeSetDebugHandler(getNativeHandle());
    } finally {
      endOperation();
    }
  }

  @Override
  public void clearDebugHandler() {
    beginOperation();
    try {
      this.debugHandler = null;
      nativeClearDebugHandler(getNativeHandle());
    } finally {
      endOperation();
    }
  }

  private native void nativeSetDebugHandler(long storeHandle);

  private native void nativeClearDebugHandler(long storeHandle);

  // Called from native code when a debug event occurs
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private void onDebugEvent(final int eventCode) {
    if (debugHandler == null) {
      return;
    }
    final ai.tegmentum.wasmtime4j.debug.DebugEvent event =
        ai.tegmentum.wasmtime4j.debug.DebugEvent.fromCode(eventCode);
    if (event != null) {
      debugHandler.handle(event, java.util.Collections.emptyList());
    }
  }

  // Call hook support
  private ai.tegmentum.wasmtime4j.func.CallHookHandler callHookHandler;
  private ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler asyncCallHookHandler;

  @Override
  public void setCallHook(final ai.tegmentum.wasmtime4j.func.CallHookHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      this.callHookHandler = handler;
      if (handler == null) {
        nativeClearCallHook(nativeHandle);
      } else {
        nativeSetCallHook(nativeHandle);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void setCallHookAsync(final ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      this.asyncCallHookHandler = handler;
      if (handler == null) {
        nativeClearCallHookAsync(nativeHandle);
      } else {
        nativeSetCallHookAsync(nativeHandle);
      }
    } finally {
      endOperation();
    }
  }

  // Called from native code when a call hook event occurs
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private void onCallHook(final int hookType)
      throws ai.tegmentum.wasmtime4j.exception.TrapException {
    if (callHookHandler != null) {
      callHookHandler.onCallHook(ai.tegmentum.wasmtime4j.func.CallHook.fromValue(hookType));
    }
  }

  // Called from native code for async call hook
  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called from native JNI code")
  private void onCallHookAsync(final int hookType) {
    if (asyncCallHookHandler != null) {
      asyncCallHookHandler.onCallHook(ai.tegmentum.wasmtime4j.func.CallHook.fromValue(hookType));
    }
  }

  private native void nativeSetCallHook(long storeHandle);

  private native void nativeClearCallHook(long storeHandle);

  private native void nativeSetCallHookAsync(long storeHandle);

  private native void nativeClearCallHookAsync(long storeHandle);

  /**
   * Sets the resource limiter on the native store with callback support.
   *
   * <p>This registers callbacks that will invoke {@link #onMemoryGrowing}, {@link #onTableGrowing},
   * {@link #onMemoryGrowFailed}, and {@link #onTableGrowFailed} on this JniStore instance.
   *
   * @param storeHandle the native store handle
   */
  private native void nativeSetResourceLimiter(long storeHandle);

  /**
   * Clears the resource limiter callbacks from the native store.
   *
   * @param storeHandle the native store handle
   */
  private native void nativeClearResourceLimiter(long storeHandle);

  /**
   * Sets the async resource limiter on the native store with callback support.
   *
   * <p>This uses the same callback methods as the sync limiter but registers via the async limiter
   * path in Wasmtime. Requires the engine to be configured with async support.
   *
   * @param storeHandle the native store handle
   */
  private native void nativeSetResourceLimiterAsync(long storeHandle);

  // ===== Fuel Async Methods =====

  private long fuelAsyncYieldInterval = 0;

  @Override
  public void setFuelAsyncYieldInterval(final long interval) throws WasmException {
    beginOperation();
    try {
      if (interval < 0) {
        throw new IllegalArgumentException("Interval cannot be negative");
      }
      this.fuelAsyncYieldInterval = interval;
      nativeSetFuelAsyncYieldInterval(nativeHandle, interval);
    } finally {
      endOperation();
    }
  }

  @Override
  public long getFuelAsyncYieldInterval() {
    return fuelAsyncYieldInterval;
  }

  // Native methods for new functionality
  private native void nativeSetFuelAsyncYieldInterval(long storeHandle, long interval);

  // ===== Backtrace API =====

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace captureBacktrace()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      final ai.tegmentum.wasmtime4j.debug.WasmBacktrace result =
          nativeCaptureBacktrace(nativeHandle);
      if (result == null) {
        return new ai.tegmentum.wasmtime4j.debug.WasmBacktrace(
            java.util.Collections.emptyList(), false);
      }
      return result;
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace forceCaptureBacktrace()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      final ai.tegmentum.wasmtime4j.debug.WasmBacktrace result =
          nativeForceCaptureBacktrace(nativeHandle);
      if (result == null) {
        return new ai.tegmentum.wasmtime4j.debug.WasmBacktrace(
            java.util.Collections.emptyList(), true);
      }
      return result;
    } finally {
      endOperation();
    }
  }

  private static native ai.tegmentum.wasmtime4j.debug.WasmBacktrace nativeCaptureBacktrace(
      long storeHandle);

  private static native ai.tegmentum.wasmtime4j.debug.WasmBacktrace nativeForceCaptureBacktrace(
      long storeHandle);

  // ===== Debugging API =====

  @Override
  public boolean isSingleStep() {
    if (!tryBeginOperation()) {
      return false;
    }
    try {
      return nativeIsSingleStep(getNativeHandle());
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean isAsync() {
    if (!tryBeginOperation()) {
      return false;
    }
    try {
      return nativeIsAsync(getNativeHandle());
    } finally {
      endOperation();
    }
  }

  @Override
  public java.util.Optional<java.util.List<ai.tegmentum.wasmtime4j.debug.Breakpoint>>
      breakpoints() {
    if (!tryBeginOperation()) {
      return java.util.Optional.empty();
    }
    try {
      final int count = nativeBreakpointCount(getNativeHandle());
      if (count < 0) {
        return java.util.Optional.empty(); // debugging not enabled or error
      }
      // We can report count but not individual breakpoints without iteration support
      return java.util.Optional.of(java.util.Collections.emptyList());
    } finally {
      endOperation();
    }
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.debug.BreakpointEditor> editBreakpoints() {
    if (!tryBeginOperation()) {
      return java.util.Optional.empty();
    }
    try {
      // Check if debugging is available by trying breakpoint count
      final int count = nativeBreakpointCount(getNativeHandle());
      if (count < -1) {
        return java.util.Optional.empty(); // error
      }
      final long storeHandle = getNativeHandle();
      return java.util.Optional.of(
          new ai.tegmentum.wasmtime4j.debug.BreakpointEditor() {
            @Override
            public ai.tegmentum.wasmtime4j.debug.BreakpointEditor addBreakpoint(
                final ai.tegmentum.wasmtime4j.Module module, final int pc) {
              java.util.Objects.requireNonNull(module, "module cannot be null");
              if (pc < 0) {
                throw new IllegalArgumentException("pc cannot be negative: " + pc);
              }
              final long moduleHandle = ((JniModule) module).getNativeHandle();
              nativeAddBreakpoint(storeHandle, moduleHandle, pc);
              return this;
            }

            @Override
            public ai.tegmentum.wasmtime4j.debug.BreakpointEditor removeBreakpoint(
                final ai.tegmentum.wasmtime4j.Module module, final int pc) {
              java.util.Objects.requireNonNull(module, "module cannot be null");
              if (pc < 0) {
                throw new IllegalArgumentException("pc cannot be negative: " + pc);
              }
              final long moduleHandle = ((JniModule) module).getNativeHandle();
              nativeRemoveBreakpoint(storeHandle, moduleHandle, pc);
              return this;
            }

            @Override
            public ai.tegmentum.wasmtime4j.debug.BreakpointEditor singleStep(
                final boolean enabled) {
              nativeSetSingleStep(storeHandle, enabled);
              return this;
            }

            @Override
            public void apply() {
              // Breakpoint edits are applied immediately via native calls
            }
          });
    } finally {
      endOperation();
    }
  }

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> debugExitFrames()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      final int[] frameData = nativeDebugExitFrames(getNativeHandle());
      if (frameData == null) {
        return java.util.Collections.emptyList();
      }
      final int frameCount = frameData.length / 4;
      final java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> frames =
          new java.util.ArrayList<>(frameCount);
      for (int i = 0; i < frameCount; i++) {
        final int base = i * 4;
        frames.add(
            new ai.tegmentum.wasmtime4j.debug.FrameHandle(
                0L, // no native ptr for snapshot approach
                frameData[base], // functionIndex
                frameData[base + 1], // pc
                frameData[base + 2], // numLocals
                frameData[base + 3], // numStack
                null, // instance (not available in snapshot)
                null)); // module (not available in snapshot)
      }
      return frames;
    } finally {
      endOperation();
    }
  }

  // Debugging native methods
  private static native boolean nativeIsSingleStep(long storeHandle);

  private static native boolean nativeIsAsync(long storeHandle);

  private static native int nativeBreakpointCount(long storeHandle);

  private static native int nativeAddBreakpoint(long storeHandle, long moduleHandle, int pc);

  private static native int nativeRemoveBreakpoint(long storeHandle, long moduleHandle, int pc);

  private static native int nativeSetSingleStep(long storeHandle, boolean enabled);

  private static native int[] nativeDebugExitFrames(long storeHandle);

  // ===== Native Async Bridge Methods =====
  //
  // These override the default CompletableFuture.supplyAsync() wrappers in Store with
  // real Tokio-based async execution. The native method spawns the operation on the Tokio
  // runtime and completes the CompletableFuture from a Tokio worker thread, freeing the
  // calling Java thread immediately.

  @Override
  public java.util.concurrent.CompletableFuture<Void> gcAsync() {
    beginOperation();
    try {
      final java.util.concurrent.CompletableFuture<Long> rawFuture =
          new java.util.concurrent.CompletableFuture<>();
      nativeGcAsyncBridge(getNativeHandle(), rawFuture);
      return rawFuture.thenApply(v -> null);
    } finally {
      endOperation();
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<Instance> createInstanceAsync(final Module module) {
    Objects.requireNonNull(module, "Module cannot be null");
    beginOperation();
    try {
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule instance for JNI store");
      }
      final JniModule jniModule = (JniModule) module;
      final java.util.concurrent.CompletableFuture<Long> rawFuture =
          new java.util.concurrent.CompletableFuture<>();
      nativeCreateInstanceAsyncBridge(getNativeHandle(), jniModule.getNativeHandle(), rawFuture);
      return rawFuture.thenApply(ptr -> new JniInstance(ptr, jniModule, this));
    } finally {
      endOperation();
    }
  }

  @Override
  @SuppressFBWarnings(
      value = "NP_NONNULL_PARAM_VIOLATION",
      justification = "CompletableFuture passed to native for async completion")
  public java.util.concurrent.CompletableFuture<ai.tegmentum.wasmtime4j.WasmMemory>
      createMemoryAsync(final ai.tegmentum.wasmtime4j.type.MemoryType memoryType) {
    Validation.requireNonNull(memoryType, "memoryType");
    beginOperation();
    try {
      final long minPages = memoryType.getMinimum();
      final long maxPages = memoryType.getMaximum().orElse(-1L);
      final int isShared = memoryType.isShared() ? 1 : 0;
      final int is64 = memoryType.is64Bit() ? 1 : 0;

      if (minPages < 0) {
        throw new IllegalArgumentException("Minimum pages cannot be negative: " + minPages);
      }
      if (maxPages != -1 && maxPages < minPages) {
        throw new IllegalArgumentException(
            "Maximum pages ("
                + maxPages
                + ") cannot be less than minimum pages ("
                + minPages
                + ")");
      }

      final java.util.concurrent.CompletableFuture<Long> rawFuture =
          new java.util.concurrent.CompletableFuture<>();
      nativeCreateMemoryAsyncBridge(
          getNativeHandle(), minPages, maxPages, isShared, is64, rawFuture);
      return rawFuture.thenApply(ptr -> new JniMemory(ptr, this));
    } finally {
      endOperation();
    }
  }

  @Override
  @SuppressFBWarnings(
      value = "NP_NONNULL_PARAM_VIOLATION",
      justification = "CompletableFuture passed to native for async completion")
  public java.util.concurrent.CompletableFuture<ai.tegmentum.wasmtime4j.WasmTable> createTableAsync(
      final ai.tegmentum.wasmtime4j.type.TableType tableType) {
    Validation.requireNonNull(tableType, "tableType");
    beginOperation();
    try {
      final WasmValueType elementType = tableType.getElementType();
      final long minSize = tableType.getMinimum();
      final long maxSize = tableType.getMaximum().orElse(-1L);

      if (elementType != WasmValueType.FUNCREF && elementType != WasmValueType.EXTERNREF) {
        throw new IllegalArgumentException(
            "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
      }

      final int elemTypeCode = (elementType == WasmValueType.FUNCREF) ? 0x70 : 0x6F;

      final java.util.concurrent.CompletableFuture<Long> rawFuture =
          new java.util.concurrent.CompletableFuture<>();
      nativeCreateTableAsyncBridge(
          getNativeHandle(), elemTypeCode, (int) minSize, (int) maxSize, rawFuture);
      return rawFuture.thenApply(ptr -> new JniTable(ptr, this));
    } finally {
      endOperation();
    }
  }

  // Native async bridge method declarations
  private static native void nativeGcAsyncBridge(
      long storeHandle, java.util.concurrent.CompletableFuture<Long> future);

  private static native void nativeCreateInstanceAsyncBridge(
      long storeHandle, long moduleHandle, java.util.concurrent.CompletableFuture<Long> future);

  private static native void nativeCreateMemoryAsyncBridge(
      long storeHandle,
      long initialPages,
      long maxPages,
      int isShared,
      int is64,
      java.util.concurrent.CompletableFuture<Long> future);

  private static native void nativeCreateTableAsyncBridge(
      long storeHandle,
      int elementType,
      int initialSize,
      int maxSize,
      java.util.concurrent.CompletableFuture<Long> future);
}
