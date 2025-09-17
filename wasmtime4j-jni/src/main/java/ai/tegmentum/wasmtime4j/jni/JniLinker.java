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
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Linker interface.
 *
 * <p>This class provides a WebAssembly linker implementation using JNI calls to the native
 * Wasmtime library. The linker enables defining host functions and resolving imports before
 * instantiating WebAssembly modules.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 */
public final class JniLinker extends JniResource implements Linker {

  private static final Logger LOGGER = Logger.getLogger(JniLinker.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniLinker: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Flag to track if this linker has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Reference to the engine this linker was created for. */
  private final Engine engine;

  /**
   * Creates a new JNI linker with the given native handle and engine.
   *
   * @param nativeHandle the native linker handle
   * @param engine the engine this linker was created for
   * @throws IllegalArgumentException if nativeHandle is 0
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if engine is null
   */
  JniLinker(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    JniValidation.requireNonNull(engine, "engine");
    this.engine = engine;
    LOGGER.fine("Created JNI linker with handle: " + nativeHandle);
  }

  /**
   * Creates a new linker for the given engine.
   *
   * @param engine the engine to create the linker for
   * @return a new JniLinker instance
   * @throws WasmException if linker creation fails
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if engine is null
   */
  public static JniLinker create(final Engine engine) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");

    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JNI engine instance");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    final long nativeHandle = nativeCreate(jniEngine.getNativeHandle());
    if (nativeHandle == 0) {
      throw new WasmException("Failed to create native linker");
    }

    return new JniLinker(nativeHandle, engine);
  }

  @Override
  public void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(functionType, "functionType");
    JniValidation.requireNonNull(implementation, "implementation");
    ensureNotClosed();

    try {
      // Create host function wrapper
      final JniHostFunction jniHostFunction = new JniHostFunction(implementation);

      // Convert FunctionType parameters and returns to native representation
      final int[] paramTypesArray = convertToNativeTypes(functionType.getParamTypes());
      final int[] returnTypesArray = convertToNativeTypes(functionType.getReturnTypes());

      final boolean success = nativeDefineHostFunction(
          getNativeHandle(),
          moduleName,
          name,
          paramTypesArray,
          returnTypesArray,
          jniHostFunction.getNativeHandle()
      );

      if (!success) {
        throw new WasmException("Failed to define host function: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined host function " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining host function: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineMemory(final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(memory, "memory");
    ensureNotClosed();

    try {
      if (!(memory instanceof JniMemory)) {
        throw new IllegalArgumentException("Memory must be a JNI memory instance");
      }

      final JniMemory jniMemory = (JniMemory) memory;
      final boolean success = nativeDefineMemory(
          getNativeHandle(),
          moduleName,
          name,
          jniMemory.getNativeHandle()
      );

      if (!success) {
        throw new WasmException("Failed to define memory: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined memory " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining memory: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineTable(final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(table, "table");
    ensureNotClosed();

    try {
      if (!(table instanceof JniTable)) {
        throw new IllegalArgumentException("Table must be a JNI table instance");
      }

      final JniTable jniTable = (JniTable) table;
      final boolean success = nativeDefineTable(
          getNativeHandle(),
          moduleName,
          name,
          jniTable.getNativeHandle()
      );

      if (!success) {
        throw new WasmException("Failed to define table: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined table " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining table: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineGlobal(final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(global, "global");
    ensureNotClosed();

    try {
      if (!(global instanceof JniGlobal)) {
        throw new IllegalArgumentException("Global must be a JNI global instance");
      }

      final JniGlobal jniGlobal = (JniGlobal) global;
      final boolean success = nativeDefineGlobal(
          getNativeHandle(),
          moduleName,
          name,
          jniGlobal.getNativeHandle()
      );

      if (!success) {
        throw new WasmException("Failed to define global: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined global " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining global: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineInstance(final String moduleName, final Instance instance)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonNull(instance, "instance");
    ensureNotClosed();

    try {
      if (!(instance instanceof JniInstance)) {
        throw new IllegalArgumentException("Instance must be a JNI instance");
      }

      final JniInstance jniInstance = (JniInstance) instance;
      final boolean success = nativeDefineInstance(
          getNativeHandle(),
          moduleName,
          jniInstance.getNativeHandle()
      );

      if (!success) {
        throw new WasmException("Failed to define instance: " + moduleName);
      }

      LOGGER.fine("Defined instance for module " + moduleName);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining instance: " + e.getMessage(), e);
    }
  }

  @Override
  public void alias(final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException {
    JniValidation.requireNonBlank(fromModule, "fromModule");
    JniValidation.requireNonBlank(fromName, "fromName");
    JniValidation.requireNonBlank(toModule, "toModule");
    JniValidation.requireNonBlank(toName, "toName");
    ensureNotClosed();

    try {
      final boolean success = nativeAlias(
          getNativeHandle(),
          fromModule,
          fromName,
          toModule,
          toName
      );

      if (!success) {
        throw new WasmException("Failed to create alias: " + fromModule + "::" + fromName + " -> " + toModule + "::" + toName);
      }

      LOGGER.fine("Created alias " + fromModule + "::" + fromName + " -> " + toModule + "::" + toName);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating alias: " + e.getMessage(), e);
    }
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(module, "module");
    ensureNotClosed();

    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JNI store instance");
      }
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JNI module instance");
      }

      final JniStore jniStore = (JniStore) store;
      final JniModule jniModule = (JniModule) module;

      final long instanceHandle = nativeInstantiate(
          getNativeHandle(),
          jniStore.getNativeHandle(),
          jniModule.getNativeHandle()
      );

      if (instanceHandle == 0) {
        throw new WasmException("Failed to instantiate module");
      }

      final JniInstance instance = new JniInstance(instanceHandle, module, store);
      LOGGER.fine("Successfully instantiated module");
      return instance;
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error instantiating module: " + e.getMessage(), e);
    }
  }

  @Override
  public Instance instantiate(final Store store, final String moduleName, final Module module)
      throws WasmException {
    final Instance instance = instantiate(store, module);

    try {
      defineInstance(moduleName, instance);
      LOGGER.fine("Instantiated and registered module as '" + moduleName + "'");
      return instance;
    } catch (final WasmException e) {
      // If we can't register the instance, still return it but close it
      instance.close();
      throw e;
    }
  }

  @Override
  public void enableWasi() throws WasmException {
    ensureNotClosed();

    try {
      final boolean success = nativeEnableWasi(getNativeHandle());
      if (!success) {
        throw new WasmException("Failed to enable WASI");
      }

      LOGGER.fine("WASI support enabled");
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error enabling WASI: " + e.getMessage(), e);
    }
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !closed.get() && isNativeResourceValid();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        nativeDestroy(getNativeHandle());
        LOGGER.fine("Closed JNI linker");
      } catch (final Exception e) {
        LOGGER.warning("Error during linker cleanup: " + e.getMessage());
      }
    }
  }

  /**
   * Ensures this linker is not closed.
   *
   * @throws IllegalStateException if this linker is closed
   */
  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Linker has been closed");
    }
  }

  /**
   * Converts WasmValueType array to native type representation.
   *
   * @param types the WasmValueType array to convert
   * @return array of native type constants
   */
  private int[] convertToNativeTypes(final WasmValueType[] types) {
    final int[] nativeTypes = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      switch (types[i]) {
        case I32:
          nativeTypes[i] = 0;
          break;
        case I64:
          nativeTypes[i] = 1;
          break;
        case F32:
          nativeTypes[i] = 2;
          break;
        case F64:
          nativeTypes[i] = 3;
          break;
        case V128:
          nativeTypes[i] = 4;
          break;
        case FUNCREF:
          nativeTypes[i] = 5;
          break;
        case EXTERNREF:
          nativeTypes[i] = 6;
          break;
        default:
          throw new IllegalArgumentException("Unknown WebAssembly value type: " + types[i]);
      }
    }
    return nativeTypes;
  }

  // Native method declarations

  /**
   * Creates a new native linker.
   *
   * @param engineHandle the native engine handle
   * @return the native linker handle, or 0 on failure
   */
  private static native long nativeCreate(final long engineHandle);

  /**
   * Defines a host function in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param functionName the function name for the import
   * @param paramTypes array of parameter type constants
   * @param returnTypes array of return type constants
   * @param hostFunctionHandle the native host function handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineHostFunction(
      final long linkerHandle,
      final String moduleName,
      final String functionName,
      final int[] paramTypes,
      final int[] returnTypes,
      final long hostFunctionHandle);

  /**
   * Defines a memory in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param memoryName the memory name for the import
   * @param memoryHandle the native memory handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineMemory(
      final long linkerHandle,
      final String moduleName,
      final String memoryName,
      final long memoryHandle);

  /**
   * Defines a table in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param tableName the table name for the import
   * @param tableHandle the native table handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineTable(
      final long linkerHandle,
      final String moduleName,
      final String tableName,
      final long tableHandle);

  /**
   * Defines a global in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param globalName the global name for the import
   * @param globalHandle the native global handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineGlobal(
      final long linkerHandle,
      final String moduleName,
      final String globalName,
      final long globalHandle);

  /**
   * Defines an instance in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param instanceHandle the native instance handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineInstance(
      final long linkerHandle,
      final String moduleName,
      final long instanceHandle);

  /**
   * Creates an alias in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param fromModule the source module name
   * @param fromName the source export name
   * @param toModule the destination module name
   * @param toName the destination export name
   * @return true if successful, false otherwise
   */
  private static native boolean nativeAlias(
      final long linkerHandle,
      final String fromModule,
      final String fromName,
      final String toModule,
      final String toName);

  /**
   * Instantiates a module using the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param storeHandle the native store handle
   * @param moduleHandle the native module handle
   * @return the native instance handle, or 0 on failure
   */
  private static native long nativeInstantiate(
      final long linkerHandle,
      final long storeHandle,
      final long moduleHandle);

  /**
   * Enables WASI support in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeEnableWasi(final long linkerHandle);

  /**
   * Destroys the native linker.
   *
   * @param linkerHandle the native linker handle
   */
  private static native void nativeDestroy(final long linkerHandle);
}