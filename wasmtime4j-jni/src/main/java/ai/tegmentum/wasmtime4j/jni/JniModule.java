package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeMethodBindings;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the WebAssembly Module.
 *
 * <p>This class represents a compiled WebAssembly module and provides access to its metadata,
 * imports, and exports through JNI calls to the native Wasmtime library. A module contains
 * the compiled WebAssembly bytecode and can be instantiated multiple times to create
 * independent execution contexts.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatic resource management with {@link AutoCloseable}</li>
 *   <li>Defensive programming to prevent JVM crashes</li>
 *   <li>Comprehensive parameter validation</li>
 *   <li>Thread-safe operations</li>
 *   <li>Module metadata inspection (exports, imports, size)</li>
 *   <li>Static bytecode validation without compilation</li>
 * </ul>
 *
 * <p>Usage Example:
 * <pre>{@code
 * try (JniEngine engine = JniEngine.create()) {
 *   // Validate bytecode first (optional but recommended)
 *   if (!JniModule.validate(wasmBytes)) {
 *     throw new IllegalArgumentException("Invalid WebAssembly bytecode");
 *   }
 *   
 *   // Compile module
 *   try (JniModule module = engine.compileModule(wasmBytes)) {
 *     // Inspect module metadata
 *     String[] functions = module.getExportedFunctions();
 *     String[] imports = module.getImportedFunctions();
 *     long moduleSize = module.getSize();
 *     
 *     // Create instances from this module
 *     try (JniStore store = engine.createStore()) {
 *       JniInstance instance = module.instantiate(store);
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>This implementation extends {@link JniResource} to provide automatic native resource
 * management and follows defensive programming practices to prevent native crashes.
 *
 * @since 1.0.0
 */
public final class JniModule extends JniResource {

  private static final Logger LOGGER = Logger.getLogger(JniModule.class.getName());

  /**
   * Creates a new JNI module with the given native handle.
   *
   * <p>This constructor is package-private and should only be used by the JniEngine
   * or other JNI classes. External code should create modules through
   * {@link JniEngine#compileModule(byte[])}.
   *
   * @param nativeHandle the native module handle from Wasmtime
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniModule(final long nativeHandle) {
    super(nativeHandle);
    LOGGER.fine("Created JNI module with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Creates an instance of this module within the specified store.
   *
   * <p>This method instantiates the compiled WebAssembly module, creating a new execution
   * context. The instance will have access to all exports defined by the module and
   * must satisfy all import requirements.
   *
   * @param store the store context for the new instance
   * @return a new module instance
   * @throws JniException if instantiation fails
   * @throws JniResourceException if this module or store has been closed
   */
  public JniInstance instantiate(final JniStore store) {
    JniValidation.requireNonNull(store, "store");
    ensureNotClosed();

    try {
      final long instanceHandle = nativeInstantiateModule(getNativeHandle(), store.getNativeHandle());
      JniValidation.requireValidHandle(instanceHandle, "instanceHandle");
      return new JniInstance(instanceHandle);
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Failed to instantiate module", e);
    }
  }

  /**
   * Gets the names of all functions exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent
   * external modification of the internal data structures.
   *
   * @return array of exported function names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedFunctions() {
    ensureNotClosed();
    
    try {
      final String[] functions = nativeGetExportedFunctions(getNativeHandle());
      return functions != null ? functions.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported functions", e);
    }
  }

  /**
   * Gets the names of all memories exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent
   * external modification of the internal data structures.
   *
   * @return array of exported memory names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedMemories() {
    ensureNotClosed();
    
    try {
      final String[] memories = nativeGetExportedMemories(getNativeHandle());
      return memories != null ? memories.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported memories", e);
    }
  }

  /**
   * Gets the names of all tables exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent
   * external modification of the internal data structures.
   *
   * @return array of exported table names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedTables() {
    ensureNotClosed();
    
    try {
      final String[] tables = nativeGetExportedTables(getNativeHandle());
      return tables != null ? tables.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported tables", e);
    }
  }

  /**
   * Gets the names of all globals exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent
   * external modification of the internal data structures.
   *
   * @return array of exported global names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedGlobals() {
    ensureNotClosed();
    
    try {
      final String[] globals = nativeGetExportedGlobals(getNativeHandle());
      return globals != null ? globals.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported globals", e);
    }
  }

  /**
   * Gets the names of all functions imported by this module.
   *
   * <p>Import names are returned in "module::name" format, where "module" is the
   * import module name and "name" is the imported function name.
   *
   * <p>This method returns a defensive copy of the import names to prevent
   * external modification of the internal data structures.
   *
   * @return array of imported function names in "module::name" format (never null, may be empty)
   * @throws JniException if the imports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getImportedFunctions() {
    ensureNotClosed();
    
    try {
      final String[] functions = nativeGetImportedFunctions(getNativeHandle());
      return functions != null ? functions.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get imported functions", e);
    }
  }

  /**
   * Validates WebAssembly bytecode without compiling it.
   *
   * <p>This static method performs structural validation of WebAssembly bytecode to check
   * if it conforms to the WebAssembly specification. This is useful for quick validation
   * before attempting compilation, which can be expensive.
   *
   * <p>Note: This method only validates the structure and format of the bytecode.
   * It does not check import/export compatibility or other runtime concerns.
   *
   * @param bytecode the WebAssembly bytecode to validate
   * @return true if the bytecode is structurally valid, false otherwise
   * @throws JniException if validation fails due to internal errors
   */
  public static boolean validate(final byte[] bytecode) {
    JniValidation.requireNonEmpty(bytecode, "bytecode");
    NativeMethodBindings.ensureInitialized();

    final byte[] bytecodeCopy = JniValidation.defensiveCopy(bytecode);
    
    try {
      return nativeValidateModule(bytecodeCopy);
    } catch (final Exception e) {
      LOGGER.warning("Error validating module bytecode: " + e.getMessage());
      throw new JniException("Module validation failed", e);
    }
  }

  /**
   * Gets the size of the compiled module in bytes.
   *
   * <p>This returns the size of the internal representation of the compiled module,
   * which may differ from the original WebAssembly bytecode size due to compilation
   * optimizations and internal data structures.
   *
   * @return the module size in bytes (always >= 0)
   * @throws JniException if the size cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public long getSize() {
    ensureNotClosed();
    
    try {
      final long size = nativeGetModuleSize(getNativeHandle());
      JniValidation.requireNonNegative(size, "moduleSize");
      return size;
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Failed to get module size", e);
    }
  }

  @Override
  protected void doClose() throws Exception {
    if (getNativeHandle() != 0) {
      nativeDestroyModule(getNativeHandle());
      LOGGER.fine("Destroyed JNI module with handle: 0x" + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "Module";
  }

  // Native method declarations

  /**
   * Instantiates a module within a store context.
   *
   * @param moduleHandle the native module handle
   * @param storeHandle the native store handle
   * @return native instance handle or 0 on failure
   */
  private static native long nativeInstantiateModule(long moduleHandle, long storeHandle);

  /**
   * Gets the names of functions exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of function names or null on error
   */
  private static native String[] nativeGetExportedFunctions(long moduleHandle);

  /**
   * Gets the names of memories exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of memory names or null on error
   */
  private static native String[] nativeGetExportedMemories(long moduleHandle);

  /**
   * Gets the names of tables exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of table names or null on error
   */
  private static native String[] nativeGetExportedTables(long moduleHandle);

  /**
   * Gets the names of globals exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of global names or null on error
   */
  private static native String[] nativeGetExportedGlobals(long moduleHandle);

  /**
   * Gets the names of functions imported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of function names in "module::name" format or null on error
   */
  private static native String[] nativeGetImportedFunctions(long moduleHandle);

  /**
   * Validates WebAssembly bytecode without compiling.
   *
   * @param bytecode the WebAssembly bytecode to validate
   * @return true if structurally valid, false otherwise
   */
  private static native boolean nativeValidateModule(byte[] bytecode);

  /**
   * Gets the size of a compiled module in bytes.
   *
   * @param moduleHandle the native module handle
   * @return the module size in bytes or -1 on error
   */
  private static native long nativeGetModuleSize(long moduleHandle);

  /**
   * Destroys a native module and releases all associated resources.
   *
   * @param moduleHandle the native module handle
   */
  private static native void nativeDestroyModule(long moduleHandle);
}
