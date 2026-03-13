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
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.util.TypeConversionUtilities;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JNI implementation of the Linker interface.
 *
 * <p>Extends {@link JniResource} for thread-safe lifecycle management and automatic cleanup via
 * phantom references.
 *
 * @param <T> the type of user data associated with stores
 * @since 1.0.0
 */
public class JniLinker<T> extends JniResource implements Linker<T> {
  private static final Logger LOGGER = Logger.getLogger(JniLinker.class.getName());
  // Array-based callback registry indexed by sequential callback ID for O(1) lookup
  // without Long autoboxing overhead on every callback dispatch from native code.
  private static final java.util.concurrent.atomic.AtomicReference<
          java.util.concurrent.atomic.AtomicReferenceArray<HostFunctionWrapper>>
      HOST_FUNCTION_CALLBACKS =
          new java.util.concurrent.atomic.AtomicReference<>(
              new java.util.concurrent.atomic.AtomicReferenceArray<>(256));
  private static final Object CALLBACKS_GROW_LOCK = new Object();

  private final Engine engine;
  private final Set<String> imports = new HashSet<>();
  private final java.util.Map<String, ai.tegmentum.wasmtime4j.validation.ImportInfo>
      importRegistry = new java.util.concurrent.ConcurrentHashMap<>();
  private final Set<Long> registeredCallbackIds =
      java.util.concurrent.ConcurrentHashMap.newKeySet();
  private boolean wasiEnabled = false;
  private volatile JniWasiContextImpl wasiContext = null;

  /**
   * Creates a new JNI linker with the given native handle.
   *
   * @param nativeHandle the native handle (must be non-zero)
   * @param engine the engine
   */
  public JniLinker(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    this.engine = engine;
  }

  /**
   * Gets the engine.
   *
   * @return the engine
   */
  public Engine getEngine() {
    return engine;
  }

  /**
   * Sets the WASI context for this linker.
   *
   * <p>The WASI context will be tracked on the store during instantiation.
   *
   * @param wasiCtx the WASI context to track
   */
  void setWasiContext(final JniWasiContextImpl wasiCtx) {
    this.wasiContext = wasiCtx;
  }

  @Override
  public void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    beginOperation();
    try {
      // Convert FunctionType to native representation
      final int[] paramTypes = TypeConversionUtilities.toNativeTypes(functionType.getParamTypes());
      final int[] returnTypes =
          TypeConversionUtilities.toNativeTypes(functionType.getReturnTypes());

      // Create a callback wrapper that will be invoked from native code
      final long callbackId = registerHostFunctionCallback(moduleName, name, implementation);

      try {
        final boolean success =
            nativeDefineHostFunction(
                nativeHandle, moduleName, name, paramTypes, returnTypes, callbackId);

        if (!success) {
          throw new WasmException("Failed to define host function: " + moduleName + "::" + name);
        }

        addImportWithMetadata(
            moduleName,
            name,
            ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION,
            functionType.toString());
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining host function: " + moduleName + "::" + name, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void defineMemory(
      final Store store, final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Memory name cannot be null");
    }
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    beginOperation();
    try {
      if (!(memory instanceof JniMemory)) {
        throw new IllegalArgumentException("Memory must be a JniMemory instance for JNI linker");
      }
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance for JNI linker");
      }

      final JniMemory jniMemory = (JniMemory) memory;
      final JniStore jniStore = (JniStore) store;
      final long memoryHandle = jniMemory.getNativeHandle();
      final long storeHandle = jniStore.getNativeHandle();

      try {
        final boolean success =
            nativeDefineMemory(nativeHandle, storeHandle, moduleName, name, memoryHandle);

        if (!success) {
          throw new WasmException("Failed to define memory: " + moduleName + "::" + name);
        }

        addImport(moduleName, name);
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining memory: " + moduleName + "::" + name, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void defineTable(
      final Store store, final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Table name cannot be null");
    }
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null");
    }
    beginOperation();
    try {
      if (!(table instanceof JniTable)) {
        throw new IllegalArgumentException("Table must be a JniTable instance for JNI linker");
      }
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance for JNI linker");
      }

      final JniTable jniTable = (JniTable) table;
      final JniStore jniStore = (JniStore) store;
      final long tableHandle = jniTable.getNativeHandle();
      final long storeHandle = jniStore.getNativeHandle();

      try {
        final boolean success =
            nativeDefineTable(nativeHandle, storeHandle, moduleName, name, tableHandle);

        if (!success) {
          throw new WasmException("Failed to define table: " + moduleName + "::" + name);
        }

        addImport(moduleName, name);
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining table: " + moduleName + "::" + name, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void defineGlobal(
      final Store store, final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Global name cannot be null");
    }
    if (global == null) {
      throw new IllegalArgumentException("Global cannot be null");
    }
    beginOperation();
    try {
      if (!(global instanceof JniGlobal)) {
        throw new IllegalArgumentException("Global must be a JniGlobal instance for JNI linker");
      }
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance for JNI linker");
      }

      final JniGlobal jniGlobal = (JniGlobal) global;
      final JniStore jniStore = (JniStore) store;
      final long globalHandle = jniGlobal.getNativeHandle();
      final long storeHandle = jniStore.getNativeHandle();

      try {
        final boolean success =
            nativeDefineGlobal(nativeHandle, storeHandle, moduleName, name, globalHandle);

        if (!success) {
          throw new WasmException("Failed to define global: " + moduleName + "::" + name);
        }

        addImport(moduleName, name);
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining global: " + moduleName + "::" + name, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void defineInstance(final Store store, final String moduleName, final Instance instance)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    beginOperation();
    try {
      if (!(instance instanceof JniInstance)) {
        throw new IllegalArgumentException("Instance must be a JniInstance for JNI linker");
      }
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
      }

      final JniInstance jniInstance = (JniInstance) instance;
      final long instanceHandle = jniInstance.getNativeHandle();

      final JniStore jniStore = (JniStore) store;
      final long storeHandle = jniStore.getNativeHandle();

      try {
        final boolean success =
            nativeDefineInstance(nativeHandle, storeHandle, moduleName, instanceHandle);

        if (!success) {
          throw new WasmException("Failed to define instance: " + moduleName);
        }

        addImport(moduleName, "*");
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining instance: " + moduleName, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void enableWasi() throws WasmException {
    beginOperation();
    try {
      // Check if WASI is already enabled - skip if so to avoid duplicate definition errors
      if (wasiEnabled) {
        LOGGER.fine("WASI already enabled for linker, skipping");
        return;
      }

      try {
        nativeEnableWasi(nativeHandle);
        wasiEnabled = true;
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to enable WASI", e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void alias(
      final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException {
    beginOperation();
    try {
      if (fromModule == null || fromModule.isEmpty()) {
        throw new IllegalArgumentException("fromModule cannot be null or empty");
      }
      if (fromName == null || fromName.isEmpty()) {
        throw new IllegalArgumentException("fromName cannot be null or empty");
      }
      if (toModule == null || toModule.isEmpty()) {
        throw new IllegalArgumentException("toModule cannot be null or empty");
      }
      if (toName == null || toName.isEmpty()) {
        throw new IllegalArgumentException("toName cannot be null or empty");
      }

      try {
        nativeAlias(nativeHandle, fromModule, fromName, toModule, toName);
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to create alias", e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void aliasModule(final String module, final String asModule) throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (asModule == null) {
      throw new IllegalArgumentException("Alias module name cannot be null");
    }
    beginOperation();
    try {
      final boolean success = nativeAliasModule(nativeHandle, module, asModule);
      if (!success) {
        throw new WasmException("Failed to alias module '" + module + "' as '" + asModule + "'");
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Error aliasing module: " + module, e);
    } finally {
      endOperation();
    }
  }

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.validation.ImportInfo> getImportRegistry() {
    beginOperation();
    try {
      return new java.util.ArrayList<>(importRegistry.values());
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.validation.ImportValidation validateImports(
      final Module... modules) {
    beginOperation();
    try {
      return ai.tegmentum.wasmtime4j.util.LinkerSupport.validateImports(imports, modules);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean hasImport(final String moduleName, final String name) {
    beginOperation();
    try {
      return ai.tegmentum.wasmtime4j.util.LinkerSupport.hasImport(imports, moduleName, name);
    } finally {
      endOperation();
    }
  }

  /**
   * Adds an import to the tracking set.
   *
   * @param moduleName the module name
   * @param name the import name
   */
  void addImport(final String moduleName, final String name) {
    ai.tegmentum.wasmtime4j.util.LinkerSupport.addImport(imports, moduleName, name);
  }

  /**
   * Adds an import with full metadata to the tracking registry.
   *
   * @param moduleName the module name
   * @param name the import name
   * @param importKind the import kind
   * @param typeSignature the type signature
   */
  void addImportWithMetadata(
      final String moduleName,
      final String name,
      final ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind importKind,
      final String typeSignature) {
    ai.tegmentum.wasmtime4j.util.LinkerSupport.addImportWithMetadata(
        imports, importRegistry, moduleName, name, importKind, typeSignature);
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
      }
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule for JNI linker");
      }

      final JniStore jniStore = (JniStore) store;
      final JniModule jniModule = (JniModule) module;

      // Track WASI context on the store if set
      if (wasiContext != null) {
        jniStore.setTrackedWasiContext(wasiContext);
      }

      try {
        final long instanceHandle =
            nativeInstantiate(
                nativeHandle, jniStore.getNativeHandle(), jniModule.getNativeHandle());

        if (instanceHandle == 0) {
          throw new WasmException("Failed to instantiate module");
        }

        return new JniInstance(instanceHandle, jniModule, jniStore);
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error instantiating module", e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public Instance instantiate(final Store store, final String moduleName, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
      }
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule for JNI linker");
      }

      final JniStore jniStore = (JniStore) store;
      final JniModule jniModule = (JniModule) module;

      // Track WASI context on the store if set
      if (wasiContext != null) {
        jniStore.setTrackedWasiContext(wasiContext);
      }

      try {
        final long instanceHandle =
            nativeInstantiateNamed(
                nativeHandle, jniStore.getNativeHandle(), moduleName, jniModule.getNativeHandle());

        if (instanceHandle == 0) {
          throw new WasmException("Failed to instantiate named module: " + moduleName);
        }

        final JniInstance instance = new JniInstance(instanceHandle, jniModule, jniStore);

        // Note: The instance is already defined in the linker by nativeInstantiateNamed,
        // so we don't need to call defineInstance here.

        return instance;
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error instantiating named module: " + moduleName, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.InstancePre instantiatePre(final Module module)
      throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    beginOperation();
    try {
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule for JNI linker");
      }

      final JniModule jniModule = (JniModule) module;

      try {
        final long instancePreHandle =
            nativeInstantiatePre(nativeHandle, jniModule.getNativeHandle());

        if (instancePreHandle == 0) {
          throw new WasmException("Failed to create InstancePre for module");
        }

        return new JniInstancePre(instancePreHandle, module, engine);
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error creating InstancePre", e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  protected void doClose() throws Exception {
    // DEFENSIVE: Only destroy native handle if it's valid
    if (nativeHandle == 0) {
      cleanupHostFunctionCallbacks();
      return;
    }

    // Destroy native linker first, then clean up Java callbacks.
    // Native code may invoke callbacks during destruction, so callbacks must remain
    // registered until nativeDestroyLinker() completes.
    nativeDestroyLinker(nativeHandle);
    cleanupHostFunctionCallbacks();
  }

  @Override
  protected String getResourceType() {
    return "JniLinker";
  }

  /** Cleans up host function callbacks registered by this linker instance. */
  private void cleanupHostFunctionCallbacks() {
    for (final Long callbackId : registeredCallbackIds) {
      callbacksRemove((int) callbackId.longValue());
    }
    registeredCallbackIds.clear();
  }

  /**
   * Registers a host function callback.
   *
   * @param moduleName the module name
   * @param name the function name
   * @param implementation the implementation
   * @param functionType the function type
   * @return callback ID for native code to invoke
   */
  private long registerHostFunctionCallback(
      final String moduleName, final String name, final HostFunction implementation) {
    final HostFunctionWrapper wrapper = new HostFunctionWrapper(moduleName, name, implementation);
    final long id = wrapper.getId();
    callbacksPut((int) id, wrapper);
    registeredCallbackIds.add(id);
    return id;
  }

  /**
   * Register a host function callback with a specific callback ID. This is used by JniHostFunction
   * to register callbacks created via store.createHostFunction().
   *
   * @param callbackId the callback ID to use
   * @param moduleName the module name (for debugging)
   * @param name the function name
   * @param implementation the implementation
   * @param functionType the function type
   */
  static void registerHostFunctionCallbackWithId(
      final long callbackId,
      final String moduleName,
      final String name,
      final HostFunction implementation) {
    final HostFunctionWrapper wrapper =
        new HostFunctionWrapper(callbackId, moduleName, name, implementation);
    callbacksPut((int) callbackId, wrapper);
  }

  /**
   * Unregister a host function callback. This is used by JniHostFunction when it is closed.
   *
   * @param callbackId the callback ID to unregister
   */
  static void unregisterHostFunctionCallback(final long callbackId) {
    callbacksRemove((int) callbackId);
  }

  /**
   * Invokes a host function callback from native code. This method is called by the native layer
   * when a WASM module calls a host function.
   *
   * @param callbackId the callback ID registered earlier
   * @param params array of parameter values (i32/i64/f32/f64 as long/double)
   * @return array of return values
   * @throws WasmException if the callback fails
   */
  @SuppressWarnings("unused") // Called from native code
  @SuppressFBWarnings(
      value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Called by native code through JNI")
  private static WasmValue[] invokeHostFunctionCallback(
      final long callbackId, final WasmValue[] params) throws WasmException {
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine(
          "invokeHostFunctionCallback - Called with callbackId="
              + callbackId
              + ", params.length="
              + params.length);
    }

    final HostFunctionWrapper wrapper = callbacksGet(callbackId);
    if (wrapper == null) {
      LOGGER.severe("Host function callback not found for callbackId=" + callbackId);
      throw new WasmException("Host function callback not found: " + callbackId);
    }

    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("Executing host function: " + wrapper.moduleName + "::" + wrapper.name);
    }
    try {
      final WasmValue[] results = wrapper.getImplementation().execute(params);
      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine(
            "invokeHostFunctionCallback - Completed successfully with "
                + results.length
                + " results");
      }
      return results;
    } catch (final Exception e) {
      LOGGER.severe("Host function execution failed: " + e.getMessage());
      throw new WasmException(
          "Host function '"
              + wrapper.moduleName
              + "::"
              + wrapper.name
              + "' threw exception: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public Linker<T> allowShadowing(final boolean allow) {
    beginOperation();
    try {
      nativeAllowShadowing(nativeHandle, allow);
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public Linker<T> allowUnknownExports(final boolean allow) {
    beginOperation();
    try {
      nativeAllowUnknownExports(nativeHandle, allow);
      return this;
    } finally {
      endOperation();
    }
  }

  @Override
  public void defineUnknownImportsAsTraps(final Store store, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore");
      }
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule");
      }

      final JniStore jniStore = (JniStore) store;
      final JniModule jniModule = (JniModule) module;

      final boolean success =
          nativeDefineUnknownImportsAsTraps(
              nativeHandle, jniStore.getNativeHandle(), jniModule.getNativeHandle());
      if (!success) {
        throw new WasmException("Failed to define unknown imports as traps");
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void defineUnknownImportsAsDefaultValues(final Store store, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore");
      }
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule");
      }

      final JniStore jniStore = (JniStore) store;
      final JniModule jniModule = (JniModule) module;

      final boolean success =
          nativeDefineUnknownImportsAsDefaultValues(
              nativeHandle, jniStore.getNativeHandle(), jniModule.getNativeHandle());
      if (!success) {
        throw new WasmException("Failed to define unknown imports as default values");
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void funcNewUnchecked(
      final Store store,
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    beginOperation();
    try {
      // Convert FunctionType to native representation
      final int[] paramTypes = TypeConversionUtilities.toNativeTypes(functionType.getParamTypes());
      final int[] returnTypes =
          TypeConversionUtilities.toNativeTypes(functionType.getReturnTypes());

      // Register callback and get ID
      final long callbackId = registerHostFunctionCallback(moduleName, name, implementation);

      try {
        final boolean success =
            nativeDefineHostFunctionUnchecked(
                nativeHandle, moduleName, name, paramTypes, returnTypes, callbackId);

        if (!success) {
          throw new WasmException(
              "Failed to define unchecked host function: " + moduleName + "::" + name);
        }

        addImportWithMetadata(
            moduleName,
            name,
            ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION,
            functionType.toString());
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw (WasmException) e;
        }
        throw new WasmException(
            "Failed to define unchecked host function: " + moduleName + "::" + name, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public Iterable<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> iter() {
    beginOperation();
    try {
      return ai.tegmentum.wasmtime4j.util.LinkerSupport.iterDefinitions(importRegistry);
    } finally {
      endOperation();
    }
  }

  @Override
  public Iterable<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> iter(
      final ai.tegmentum.wasmtime4j.Store store)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore");
      }

      final JniStore jniStore = (JniStore) store;
      final String[] result = nativeLinkerIter(nativeHandle, jniStore.getNativeHandle());

      if (result == null) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException("Failed to iterate linker");
      }

      final java.util.List<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> definitions =
          new java.util.ArrayList<>(result.length / 3);
      for (int i = 0; i < result.length; i += 3) {
        final String moduleName = result[i];
        final String itemName = result[i + 1];
        final int typeCode = Integer.parseInt(result[i + 2]);
        definitions.add(
            new ai.tegmentum.wasmtime4j.Linker.LinkerDefinition(
                moduleName, itemName, ai.tegmentum.wasmtime4j.type.ExternType.fromCode(typeCode)));
      }
      return java.util.Collections.unmodifiableList(definitions);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Extern getByImport(
      final Store store, final String moduleName, final String name) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore");
      }

      final JniStore jniStore = (JniStore) store;

      final long externHandle =
          nativeGetByImport(nativeHandle, jniStore.getNativeHandle(), moduleName, name);

      if (externHandle == 0) {
        return null;
      }

      final int externTypeCode = nativeGetExternType(externHandle);
      switch (externTypeCode) {
        case 0: // FUNC
          return new JniExternFunc(externHandle, jniStore);
        case 1: // TABLE
          return new JniExternTable(externHandle, jniStore);
        case 2: // MEMORY
          return new JniExternMemory(externHandle, jniStore);
        case 3: // GLOBAL
          return new JniExternGlobal(externHandle, jniStore);
        default:
          LOGGER.warning("Unknown extern type code: " + externTypeCode);
          return null;
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void defineName(final Store store, final String name, final Extern extern)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (extern == null) {
      throw new IllegalArgumentException("Extern cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
      }

      final JniStore jniStore = (JniStore) store;

      try {
        // Get the native handle from the extern
        final long externHandle = getExternNativeHandle(extern);
        final int externTypeCode = getExternTypeCode(extern);

        final boolean success =
            nativeDefineName(
                nativeHandle, jniStore.getNativeHandle(), name, externHandle, externTypeCode);

        if (!success) {
          throw new WasmException("Failed to define name: " + name);
        }

        addImportWithMetadata("", name, getExternImportKind(extern), extern.toString());
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining name: " + name, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void define(
      final Store store, final String moduleName, final String name, final Extern extern)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (extern == null) {
      throw new IllegalArgumentException("Extern cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
      }

      final JniStore jniStore = (JniStore) store;

      try {
        final long externHandle = getExternNativeHandle(extern);
        final int externTypeCode = getExternTypeCode(extern);

        final boolean success =
            nativeDefine(
                nativeHandle,
                jniStore.getNativeHandle(),
                moduleName,
                name,
                externHandle,
                externTypeCode);

        if (!success) {
          throw new WasmException("Failed to define: " + moduleName + "::" + name);
        }

        addImportWithMetadata(moduleName, name, getExternImportKind(extern), extern.toString());
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining: " + moduleName + "::" + name, e);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void module(final Store store, final String moduleName, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
      }
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JniModule for JNI linker");
      }

      final JniStore jniStore = (JniStore) store;
      final JniModule jniModule = (JniModule) module;

      try {
        final boolean success =
            nativeModule(
                nativeHandle, jniStore.getNativeHandle(), moduleName, jniModule.getNativeHandle());

        if (!success) {
          throw new WasmException("Failed to define module: " + moduleName);
        }
      } catch (final Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Error defining module: " + moduleName, e);
      }
    } finally {
      endOperation();
    }
  }

  /**
   * Gets the native handle from an Extern.
   *
   * @param extern the extern to get the handle from
   * @return the native handle
   */
  private long getExternNativeHandle(final Extern extern) {
    if (extern instanceof JniExternFunc) {
      return ((JniExternFunc) extern).getNativeHandle();
    } else if (extern instanceof JniExternMemory) {
      return ((JniExternMemory) extern).getNativeHandle();
    } else if (extern instanceof JniExternTable) {
      return ((JniExternTable) extern).getNativeHandle();
    } else if (extern instanceof JniExternGlobal) {
      return ((JniExternGlobal) extern).getNativeHandle();
    }
    throw new IllegalArgumentException("Unknown extern type: " + extern.getClass().getName());
  }

  /**
   * Gets the type code for an Extern.
   *
   * @param extern the extern to get the type code from
   * @return the type code (0=FUNC, 1=TABLE, 2=MEMORY, 3=GLOBAL)
   */
  private int getExternTypeCode(final Extern extern) {
    if (extern instanceof JniExternFunc) {
      return 0;
    } else if (extern instanceof JniExternTable) {
      return 1;
    } else if (extern instanceof JniExternMemory) {
      return 2;
    } else if (extern instanceof JniExternGlobal) {
      return 3;
    }
    throw new IllegalArgumentException("Unknown extern type: " + extern.getClass().getName());
  }

  /**
   * Gets the ImportInfo.ImportKind for an Extern.
   *
   * @param extern the extern to get the type from
   * @return the import kind
   */
  private ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind getExternImportKind(
      final Extern extern) {
    if (extern instanceof JniExternFunc) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION;
    } else if (extern instanceof JniExternTable) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.TABLE;
    } else if (extern instanceof JniExternMemory) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.MEMORY;
    } else if (extern instanceof JniExternGlobal) {
      return ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.GLOBAL;
    }
    throw new IllegalArgumentException("Unknown extern type: " + extern.getClass().getName());
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmFunction getDefault(
      final Store store, final String moduleName) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore");
      }

      final JniStore jniStore = (JniStore) store;

      final long funcHandle =
          nativeGetDefault(nativeHandle, jniStore.getNativeHandle(), moduleName);

      if (funcHandle == 0) {
        return null;
      }

      return new JniFunction(funcHandle, moduleName, 0, jniStore);
    } finally {
      endOperation();
    }
  }

  /** Wrapper for host function callbacks. */
  // =============================================================================
  // Array-based callback registry operations (eliminates Long autoboxing on lookup)
  // =============================================================================

  private static void callbacksPut(final int idx, final HostFunctionWrapper wrapper) {
    java.util.concurrent.atomic.AtomicReferenceArray<HostFunctionWrapper> arr =
        HOST_FUNCTION_CALLBACKS.get();
    if (idx >= arr.length()) {
      synchronized (CALLBACKS_GROW_LOCK) {
        arr = HOST_FUNCTION_CALLBACKS.get();
        if (idx >= arr.length()) {
          final int newLen = Math.max(idx + 1, arr.length() * 2);
          final java.util.concurrent.atomic.AtomicReferenceArray<HostFunctionWrapper> newArr =
              new java.util.concurrent.atomic.AtomicReferenceArray<>(newLen);
          for (int i = 0; i < arr.length(); i++) {
            newArr.set(i, arr.get(i));
          }
          HOST_FUNCTION_CALLBACKS.set(newArr);
          arr = newArr;
        }
      }
    }
    arr.set(idx, wrapper);
  }

  private static HostFunctionWrapper callbacksGet(final long id) {
    final int idx = (int) id;
    final java.util.concurrent.atomic.AtomicReferenceArray<HostFunctionWrapper> arr =
        HOST_FUNCTION_CALLBACKS.get();
    if (idx >= 0 && idx < arr.length()) {
      return arr.get(idx);
    }
    return null;
  }

  private static void callbacksRemove(final int idx) {
    final java.util.concurrent.atomic.AtomicReferenceArray<HostFunctionWrapper> arr =
        HOST_FUNCTION_CALLBACKS.get();
    if (idx >= 0 && idx < arr.length()) {
      arr.set(idx, null);
    }
  }

  private static class HostFunctionWrapper {
    private static final java.util.concurrent.atomic.AtomicLong nextId =
        new java.util.concurrent.atomic.AtomicLong(1);

    private final long id;
    private final String moduleName;
    private final String name;
    private final HostFunction implementation;

    HostFunctionWrapper(
        final String moduleName, final String name, final HostFunction implementation) {
      this.id = nextId.getAndIncrement();
      this.moduleName = moduleName;
      this.name = name;
      this.implementation = implementation;
    }

    /**
     * Creates a wrapper with a specific callback ID. Used by JniHostFunction to register callbacks
     * with a pre-assigned ID.
     */
    HostFunctionWrapper(
        final long callbackId,
        final String moduleName,
        final String name,
        final HostFunction implementation) {
      this.id = callbackId;
      this.moduleName = moduleName;
      this.name = name;
      this.implementation = implementation;
    }

    long getId() {
      return id;
    }

    HostFunction getImplementation() {
      return implementation;
    }
  }

  // Native method declarations

  /**
   * Defines a host function in the linker.
   *
   * @param linkerHandle the linker handle
   * @param moduleName the module name
   * @param name the function name
   * @param paramTypes array of parameter type codes
   * @param returnTypes array of return type codes
   * @param callbackId callback ID for invoking the Java implementation
   * @return true on success
   */
  private native boolean nativeDefineHostFunction(
      long linkerHandle,
      String moduleName,
      String name,
      int[] paramTypes,
      int[] returnTypes,
      long callbackId);

  /**
   * Defines an unchecked host function in the linker.
   *
   * <p>This uses {@code Func::new_unchecked} internally, which skips type-checking at call time for
   * better performance.
   *
   * @param linkerHandle the linker handle
   * @param moduleName the module name
   * @param name the function name
   * @param paramTypes array of parameter type codes
   * @param returnTypes array of return type codes
   * @param callbackId callback ID for invoking the Java implementation
   * @return true on success
   */
  private native boolean nativeDefineHostFunctionUnchecked(
      long linkerHandle,
      String moduleName,
      String name,
      int[] paramTypes,
      int[] returnTypes,
      long callbackId);

  /**
   * Defines a memory in the linker.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @param name the memory name
   * @param memoryHandle the memory handle
   * @return true on success
   */
  private native boolean nativeDefineMemory(
      long linkerHandle, long storeHandle, String moduleName, String name, long memoryHandle);

  /**
   * Defines a table in the linker.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @param name the table name
   * @param tableHandle the table handle
   * @return true on success
   */
  private native boolean nativeDefineTable(
      long linkerHandle, long storeHandle, String moduleName, String name, long tableHandle);

  /**
   * Defines a global in the linker.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @param name the global name
   * @param globalHandle the global handle
   * @return true on success
   */
  private native boolean nativeDefineGlobal(
      long linkerHandle, long storeHandle, String moduleName, String name, long globalHandle);

  /**
   * Defines an instance in the linker.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @param instanceHandle the instance handle
   * @return true on success
   */
  private native boolean nativeDefineInstance(
      long linkerHandle, long storeHandle, String moduleName, long instanceHandle);

  /**
   * Instantiates a module using the linker.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleHandle the module handle
   * @return instance handle or 0 on failure
   */
  private native long nativeInstantiate(long linkerHandle, long storeHandle, long moduleHandle);

  /**
   * Instantiates a named module using the linker.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @param moduleHandle the module handle
   * @return instance handle or 0 on failure
   */
  private native long nativeInstantiateNamed(
      long linkerHandle, long storeHandle, String moduleName, long moduleHandle);

  /**
   * Creates an InstancePre for fast repeated instantiation.
   *
   * @param linkerHandle the linker handle
   * @param moduleHandle the module handle
   * @return instancePre handle or 0 on failure
   */
  private native long nativeInstantiatePre(long linkerHandle, long moduleHandle);

  /**
   * Creates an alias for an export.
   *
   * @param linkerHandle the linker handle
   * @param fromModule the source module name
   * @param fromName the source export name
   * @param toModule the destination module name
   * @param toName the destination export name
   */
  private native void nativeAlias(
      long linkerHandle, String fromModule, String fromName, String toModule, String toName);

  /**
   * Aliases all definitions from one module name to another.
   *
   * @param linkerHandle the linker handle
   * @param moduleName the source module name
   * @param asModuleName the destination module name
   * @return true on success
   */
  private native boolean nativeAliasModule(
      long linkerHandle, String moduleName, String asModuleName);

  /**
   * Enables WASI for the linker.
   *
   * @param linkerHandle the linker handle
   */
  private native void nativeEnableWasi(long linkerHandle);

  /**
   * Destroys the linker.
   *
   * @param handle the linker handle
   */
  private native void nativeDestroyLinker(long handle);

  /**
   * Allows shadowing of prior definitions.
   *
   * @param linkerHandle the linker handle
   * @param allow true to allow shadowing
   */
  private native void nativeAllowShadowing(long linkerHandle, boolean allow);

  /**
   * Allows unknown exports from modules.
   *
   * @param linkerHandle the linker handle
   * @param allow true to allow unknown exports
   */
  private native void nativeAllowUnknownExports(long linkerHandle, boolean allow);

  /**
   * Defines all undefined imports as trapping functions.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleHandle the module handle
   * @return true on success
   */
  private native boolean nativeDefineUnknownImportsAsTraps(
      long linkerHandle, long storeHandle, long moduleHandle);

  /**
   * Defines all undefined imports with default values.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleHandle the module handle
   * @return true on success
   */
  private native boolean nativeDefineUnknownImportsAsDefaultValues(
      long linkerHandle, long storeHandle, long moduleHandle);

  /**
   * Gets an extern by import specifier.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @param name the import name
   * @return extern handle or 0 if not found
   */
  private native long nativeGetByImport(
      long linkerHandle, long storeHandle, String moduleName, String name);

  /**
   * Gets the default function for a module.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @return function handle or 0 if not found
   */
  private native long nativeGetDefault(long linkerHandle, long storeHandle, String moduleName);

  private native String[] nativeLinkerIter(long linkerHandle, long storeHandle);

  /**
   * Gets the type of an extern value.
   *
   * @param externHandle the extern handle
   * @return type code (0=FUNC, 1=TABLE, 2=MEMORY, 3=GLOBAL), or -1 on error
   */
  private native int nativeGetExternType(long externHandle);

  /**
   * Defines an item at the top-level namespace without a module prefix.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param name the name for the definition
   * @param externHandle the extern handle
   * @param externTypeCode the extern type code (0=FUNC, 1=TABLE, 2=MEMORY, 3=GLOBAL)
   * @return true on success
   */
  private native boolean nativeDefineName(
      long linkerHandle, long storeHandle, String name, long externHandle, int externTypeCode);

  /**
   * Native method to define an extern with module name and item name.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the module name
   * @param name the item name
   * @param externHandle the extern handle
   * @param externTypeCode the extern type code
   * @return true on success
   */
  private native boolean nativeDefine(
      long linkerHandle,
      long storeHandle,
      String moduleName,
      String name,
      long externHandle,
      int externTypeCode);

  /**
   * Native method to define a module in the linker.
   *
   * @param linkerHandle the linker handle
   * @param storeHandle the store handle
   * @param moduleName the name to define the module under
   * @param moduleHandle the module handle
   * @return true on success
   */
  private native boolean nativeModule(
      long linkerHandle, long storeHandle, String moduleName, long moduleHandle);
}
