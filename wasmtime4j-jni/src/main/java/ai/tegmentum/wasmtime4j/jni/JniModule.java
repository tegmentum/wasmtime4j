package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * JNI implementation of the Module interface.
 *
 * <p>Extends {@link JniResource} for thread-safe lifecycle management and automatic cleanup via
 * phantom references.
 *
 * @since 1.0.0
 */
public class JniModule extends JniResource implements Module {
  private final Engine engine;

  /**
   * Creates a new JNI module with the given native handle.
   *
   * @param nativeHandle the native handle (must be non-zero)
   * @param engine the engine
   */
  public JniModule(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    this.engine = engine;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public String getName() {
    return "jni-module-" + nativeHandle;
  }

  @Override
  public List<ImportType> getImports() {
    ensureNotClosed();

    try {
      return nativeGetModuleImports(nativeHandle);
    } catch (final Throwable t) {
      return java.util.Collections.emptyList();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Instance instantiate(final ai.tegmentum.wasmtime4j.Store store)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("store must be a JniStore instance");
    }
    ensureNotClosed();

    final JniStore jniStore = (JniStore) store;
    final long instanceHandle = nativeInstantiateModule(nativeHandle, jniStore.getNativeHandle());

    if (instanceHandle == 0) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to instantiate module - native instantiation returned null");
    }

    return new JniInstance(instanceHandle, this, store);
  }

  @Override
  public ai.tegmentum.wasmtime4j.Instance instantiate(
      final ai.tegmentum.wasmtime4j.Store store,
      final ai.tegmentum.wasmtime4j.validation.ImportMap imports)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    throw new UnsupportedOperationException(
        "ImportMap-based instantiation is not yet implemented."
            + " Use Linker-based instantiation instead.");
  }

  @Override
  @SuppressFBWarnings(
      value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
      justification =
          "Error details are logged internally only; exception thrown to caller has sanitized"
              + " message")
  public List<ExportType> getExports() {
    ensureNotClosed();

    try {
      final List<ExportType> result = nativeGetModuleExports(nativeHandle);
      if (result == null) {
        java.util.logging.Logger.getLogger(JniModule.class.getName())
            .warning("nativeGetModuleExports returned null for handle: " + nativeHandle);
        return java.util.Collections.emptyList();
      }
      return java.util.Collections.unmodifiableList(result);
    } catch (final Throwable t) {
      java.util.logging.Logger.getLogger(JniModule.class.getName())
          .log(java.util.logging.Level.SEVERE, "nativeGetModuleExports failed", t);
      throw new RuntimeException("Failed to get module exports");
    }
  }

  @Override
  public Map<String, byte[]> getCustomSections() {
    ensureNotClosed();

    try {
      return nativeGetCustomSections(nativeHandle);
    } catch (final Throwable t) {
      // Defensive: Return empty map on native error instead of crashing JVM
      return java.util.Collections.emptyMap();
    }
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.type.GlobalType> getGlobalTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.type.GlobalType> globalTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.GLOBAL) {
        globalTypes.add((ai.tegmentum.wasmtime4j.type.GlobalType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(globalTypes);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.type.TableType> getTableTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.type.TableType> tableTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.TABLE) {
        tableTypes.add((ai.tegmentum.wasmtime4j.type.TableType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(tableTypes);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.type.MemoryType> getMemoryTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.type.MemoryType> memoryTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.MEMORY) {
        memoryTypes.add((ai.tegmentum.wasmtime4j.type.MemoryType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(memoryTypes);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.type.FuncType> getFunctionTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.type.FuncType> functionTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.FUNCTION) {
        functionTypes.add((ai.tegmentum.wasmtime4j.type.FuncType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(functionTypes);
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.type.FuncType> getFunctionType(
      final String functionName) {
    if (functionName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(functionName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.FUNCTION) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.type.FuncType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.type.GlobalType> getGlobalType(
      final String globalName) {
    if (globalName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(globalName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.GLOBAL) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.type.GlobalType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.type.MemoryType> getMemoryType(
      final String memoryName) {
    if (memoryName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(memoryName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.MEMORY) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.type.MemoryType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.type.TableType> getTableType(
      final String tableName) {
    if (tableName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(tableName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.TABLE) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.type.TableType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }
    ensureNotClosed();

    try {
      return nativeHasExport(nativeHandle, name);
    } catch (final Throwable t) {
      // Defensive: Return false on native error instead of crashing JVM
      return false;
    }
  }

  @Override
  public boolean hasImport(final String moduleName, final String fieldName) {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (fieldName == null) {
      throw new IllegalArgumentException("Field name cannot be null");
    }
    ensureNotClosed();

    try {
      return nativeHasImport(nativeHandle, moduleName, fieldName);
    } catch (final Throwable t) {
      // Defensive: Return false on native error instead of crashing JVM
      return false;
    }
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Module other) {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    if (isClosed()) {
      return false;
    }
    if (!(other instanceof JniModule)) {
      return false;
    }
    final JniModule otherModule = (JniModule) other;
    if (otherModule.isClosed()) {
      return false;
    }
    try {
      return nativeModuleSame(nativeHandle, otherModule.nativeHandle);
    } catch (final Throwable t) {
      return false;
    }
  }

  @Override
  public int getExportIndex(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    ensureNotClosed();
    try {
      return (int) nativeGetExportIndex(nativeHandle, name);
    } catch (final Throwable t) {
      return -1;
    }
  }

  @Override
  public boolean validateImports(final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
    if (imports == null) {
      throw new IllegalArgumentException("imports cannot be null");
    }
    ensureNotClosed();

    final List<ImportType> importTypes = getImports();

    for (final ImportType importType : importTypes) {
      final String moduleName = importType.getModuleName();
      final String fieldName = importType.getName();

      if (!imports.contains(moduleName, fieldName)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public ai.tegmentum.wasmtime4j.validation.ImportValidation validateImportsDetailed(
      final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
    if (imports == null) {
      throw new IllegalArgumentException("imports cannot be null");
    }
    ensureNotClosed();
    return ai.tegmentum.wasmtime4j.util.ModuleValidationSupport.validateImportsDetailed(
        getImports(), imports);
  }

  @Override
  public byte[] serialize() {
    if (isClosed()) {
      return new byte[0];
    }

    try {
      return nativeSerializeModule(nativeHandle);
    } catch (final Throwable t) {
      // Defensive: Return empty array on native error instead of crashing JVM
      return new byte[0];
    }
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  protected void doClose() throws Exception {
    if (nativeHandle != 0) {
      // Native cleanup is now safe with the idempotent GLOBAL_CODE registry fix.
      // The wasmtime fork at tegmentum/wasmtime (fix/global-code-registry-idempotent-v41)
      // prevents SIGABRT when virtual addresses are reused before Arc is fully released.
      nativeDestroyModule(nativeHandle);
    }
  }

  @Override
  protected String getResourceType() {
    return "JniModule";
  }

  /**
   * Native method to instantiate a module without imports.
   *
   * @param moduleHandle the native module handle
   * @param storeHandle the native store handle
   * @return the native instance handle, or 0 on failure
   */
  private static native long nativeInstantiateModule(long moduleHandle, long storeHandle);


  private native byte[] nativeSerializeModule(long handle);

  private native void nativeDestroyModule(long handle);

  /**
   * Native method to get module exports.
   *
   * @param moduleHandle the native module handle
   * @return list of module exports
   */
  private native List<ExportType> nativeGetModuleExports(long moduleHandle);

  /**
   * Native method to get module imports.
   *
   * @param moduleHandle the native module handle
   * @return list of module imports
   */
  private native List<ImportType> nativeGetModuleImports(long moduleHandle);

  /**
   * Native method to check if module has an export.
   *
   * @param moduleHandle the native module handle
   * @param exportName the name of the export to check
   * @return true if export exists, false otherwise
   */
  private native boolean nativeHasExport(long moduleHandle, String exportName);

  /**
   * Native method to check if module has an import.
   *
   * @param moduleHandle the native module handle
   * @param moduleName the module name of the import
   * @param fieldName the field name of the import
   * @return true if import exists, false otherwise
   */
  private native boolean nativeHasImport(long moduleHandle, String moduleName, String fieldName);

  /**
   * Native method to get custom sections from the module.
   *
   * <p>Custom sections are binary data embedded in WebAssembly modules that can contain metadata,
   * debug information, or other arbitrary data. The data is Base64-encoded for safe transmission
   * through JNI.
   *
   * @param moduleHandle the native module handle
   * @return map of custom section names to their binary data
   */
  private native Map<String, byte[]> nativeGetCustomSections(long moduleHandle);

  /**
   * Native method to compile a module from a file path.
   *
   * @param engineHandle the native engine handle
   * @param path the file path to compile from
   * @return the native module handle, or 0 on failure
   */
  static native long nativeCompileFromFile(long engineHandle, String path);

  /**
   * Native method to check if two modules share the same underlying compiled code.
   *
   * @param moduleHandle1 the first native module handle
   * @param moduleHandle2 the second native module handle
   * @return true if the modules are the same
   */
  private static native boolean nativeModuleSame(long moduleHandle1, long moduleHandle2);

  /**
   * Native method to get the index of an export by name.
   *
   * @param moduleHandle the native module handle
   * @param exportName the name of the export
   * @return the zero-based index, or -1 if not found
   */
  private native long nativeGetExportIndex(long moduleHandle, String exportName);
}
