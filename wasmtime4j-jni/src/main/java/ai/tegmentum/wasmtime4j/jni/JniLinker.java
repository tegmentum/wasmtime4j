package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.HashSet;
import java.util.Set;

/**
 * JNI implementation of the Linker interface.
 *
 * @param <T> the type of user data associated with stores
 * @since 1.0.0
 */
public class JniLinker<T> implements Linker<T> {
  private final long nativeHandle;
  private final Engine engine;
  private volatile boolean closed = false;
  private final Set<String> imports = new HashSet<>();

  /**
   * Creates a new JNI linker with the given native handle.
   *
   * @param nativeHandle the native handle
   * @param engine the engine
   */
  public JniLinker(final long nativeHandle, final Engine engine) {
    this.nativeHandle = nativeHandle;
    this.engine = engine;
  }

  /**
   * Gets the native handle.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Gets the engine.
   *
   * @return the engine
   */
  public Engine getEngine() {
    return engine;
  }

  @Override
  public void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Function name cannot be null or empty");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    // Convert FunctionType to native representation
    final int[] paramTypes = toNativeTypes(functionType.getParamTypes());
    final int[] returnTypes = toNativeTypes(functionType.getReturnTypes());

    // Create a callback wrapper that will be invoked from native code
    final long callbackId =
        registerHostFunctionCallback(moduleName, name, implementation, functionType);

    try {
      final boolean success =
          nativeDefineHostFunction(
              nativeHandle, moduleName, name, paramTypes, returnTypes, callbackId);

      if (!success) {
        throw new WasmException("Failed to define host function: " + moduleName + "::" + name);
      }

      addImport(moduleName, name);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Error defining host function: " + moduleName + "::" + name, e);
    }
  }

  @Override
  public void defineMemory(final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Memory name cannot be null or empty");
    }
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    ensureNotClosed();

    if (!(memory instanceof JniMemory)) {
      throw new IllegalArgumentException("Memory must be a JniMemory instance for JNI linker");
    }

    final JniMemory jniMemory = (JniMemory) memory;
    final long memoryHandle = jniMemory.getNativeHandle();

    try {
      final boolean success = nativeDefineMemory(nativeHandle, moduleName, name, memoryHandle);

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
  }

  @Override
  public void defineTable(final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Table name cannot be null or empty");
    }
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null");
    }
    ensureNotClosed();

    if (!(table instanceof JniTable)) {
      throw new IllegalArgumentException("Table must be a JniTable instance for JNI linker");
    }

    final JniTable jniTable = (JniTable) table;
    final long tableHandle = jniTable.getNativeHandle();

    try {
      final boolean success = nativeDefineTable(nativeHandle, moduleName, name, tableHandle);

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
  }

  @Override
  public void defineGlobal(final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Global name cannot be null or empty");
    }
    if (global == null) {
      throw new IllegalArgumentException("Global cannot be null");
    }
    ensureNotClosed();

    if (!(global instanceof JniGlobal)) {
      throw new IllegalArgumentException("Global must be a JniGlobal instance for JNI linker");
    }

    final JniGlobal jniGlobal = (JniGlobal) global;
    final long globalHandle = jniGlobal.getNativeHandle();

    try {
      final boolean success = nativeDefineGlobal(nativeHandle, moduleName, name, globalHandle);

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
  }

  @Override
  public void defineInstance(final String moduleName, final Instance instance)
      throws WasmException {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    ensureNotClosed();

    if (!(instance instanceof JniInstance)) {
      throw new IllegalArgumentException("Instance must be a JniInstance for JNI linker");
    }

    final JniInstance jniInstance = (JniInstance) instance;
    final long instanceHandle = jniInstance.getNativeHandle();

    try {
      final boolean success = nativeDefineInstance(nativeHandle, moduleName, instanceHandle);

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
  }

  @Override
  public void enableWasi() throws WasmException {
    // TODO: Implement WASI enabling
    throw new UnsupportedOperationException("WASI enabling not yet implemented");
  }

  @Override
  public void alias(
      final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException {
    // TODO: Implement import aliasing
    throw new UnsupportedOperationException("Import aliasing not yet implemented");
  }

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.ImportInfo> getImportRegistry() {
    // TODO: Implement import registry retrieval
    return java.util.Collections.emptyList();
  }

  @Override
  public ai.tegmentum.wasmtime4j.ImportValidation validateImports(final Module... modules) {
    // TODO: Implement import validation
    throw new UnsupportedOperationException("Import validation not yet implemented");
  }

  @Override
  public ai.tegmentum.wasmtime4j.DependencyResolution resolveDependencies(final Module... modules)
      throws WasmException {
    // TODO: Implement dependency resolution
    throw new UnsupportedOperationException("Dependency resolution not yet implemented");
  }

  @Override
  public boolean hasImport(final String moduleName, final String name) {
    return imports.contains(moduleName + "::" + name);
  }

  /**
   * Adds an import to the tracking set.
   *
   * @param moduleName the module name
   * @param name the import name
   */
  void addImport(final String moduleName, final String name) {
    imports.add(moduleName + "::" + name);
  }

  @Override
  public boolean isValid() {
    return !closed && nativeHandle != 0;
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
    }
    if (!(module instanceof JniModule)) {
      throw new IllegalArgumentException("Module must be a JniModule for JNI linker");
    }

    final JniStore jniStore = (JniStore) store;
    final JniModule jniModule = (JniModule) module;

    try {
      final long instanceHandle =
          nativeInstantiate(nativeHandle, jniStore.getNativeHandle(), jniModule.getNativeHandle());

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
  }

  @Override
  public Instance instantiate(final Store store, final String moduleName, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore for JNI linker");
    }
    if (!(module instanceof JniModule)) {
      throw new IllegalArgumentException("Module must be a JniModule for JNI linker");
    }

    final JniStore jniStore = (JniStore) store;
    final JniModule jniModule = (JniModule) module;

    try {
      final long instanceHandle =
          nativeInstantiateNamed(
              nativeHandle, jniStore.getNativeHandle(), moduleName, jniModule.getNativeHandle());

      if (instanceHandle == 0) {
        throw new WasmException("Failed to instantiate named module: " + moduleName);
      }

      final JniInstance instance = new JniInstance(instanceHandle, jniModule, jniStore);

      // Define the instance in the linker so it can be used by other modules
      defineInstance(moduleName, instance);

      return instance;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Error instantiating named module: " + moduleName, e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.InstantiationPlan createInstantiationPlan(final Module... modules)
      throws WasmException {
    // TODO: Implement instantiation plan creation
    throw new UnsupportedOperationException("Instantiation plan creation not yet implemented");
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      nativeDestroyLinker(nativeHandle);
    }
  }

  /**
   * Ensures the linker is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Linker has been closed");
    }
  }

  /**
   * Converts WasmValueType array to native type codes.
   *
   * @param types the types to convert
   * @return array of native type codes
   */
  private int[] toNativeTypes(final ai.tegmentum.wasmtime4j.WasmValueType[] types) {
    if (types == null || types.length == 0) {
      return new int[0];
    }

    final int[] nativeTypes = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      nativeTypes[i] = types[i].toNativeTypeCode();
    }
    return nativeTypes;
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
      final String moduleName,
      final String name,
      final HostFunction implementation,
      final FunctionType functionType) {
    // For now, create a simple wrapper and store it
    // In a full implementation, this would register with a callback manager
    // that can be invoked from native code
    final HostFunctionWrapper wrapper =
        new HostFunctionWrapper(moduleName, name, implementation, functionType);
    return wrapper.getId();
  }

  /** Wrapper for host function callbacks. */
  private static class HostFunctionWrapper {
    private static final java.util.concurrent.atomic.AtomicLong nextId =
        new java.util.concurrent.atomic.AtomicLong(1);

    private final long id;
    private final String moduleName;
    private final String name;
    private final HostFunction implementation;
    private final FunctionType functionType;

    HostFunctionWrapper(
        final String moduleName,
        final String name,
        final HostFunction implementation,
        final FunctionType functionType) {
      this.id = nextId.getAndIncrement();
      this.moduleName = moduleName;
      this.name = name;
      this.implementation = implementation;
      this.functionType = functionType;
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
   * Defines a memory in the linker.
   *
   * @param linkerHandle the linker handle
   * @param moduleName the module name
   * @param name the memory name
   * @param memoryHandle the memory handle
   * @return true on success
   */
  private native boolean nativeDefineMemory(
      long linkerHandle, String moduleName, String name, long memoryHandle);

  /**
   * Defines a table in the linker.
   *
   * @param linkerHandle the linker handle
   * @param moduleName the module name
   * @param name the table name
   * @param tableHandle the table handle
   * @return true on success
   */
  private native boolean nativeDefineTable(
      long linkerHandle, String moduleName, String name, long tableHandle);

  /**
   * Defines a global in the linker.
   *
   * @param linkerHandle the linker handle
   * @param moduleName the module name
   * @param name the global name
   * @param globalHandle the global handle
   * @return true on success
   */
  private native boolean nativeDefineGlobal(
      long linkerHandle, String moduleName, String name, long globalHandle);

  /**
   * Defines an instance in the linker.
   *
   * @param linkerHandle the linker handle
   * @param moduleName the module name
   * @param instanceHandle the instance handle
   * @return true on success
   */
  private native boolean nativeDefineInstance(
      long linkerHandle, String moduleName, long instanceHandle);

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
   * Destroys the linker.
   *
   * @param handle the linker handle
   */
  private native void nativeDestroyLinker(long handle);
}
