package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.ImportType;
import ai.tegmentum.wasmtime4j.Module;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JNI implementation of the Module interface.
 *
 * @since 1.0.0
 */
public class JniModule implements Module {
  private final long nativeHandle;
  private final Engine engine;
  private volatile boolean closed = false;

  /**
   * Creates a new JNI module with the given native handle.
   *
   * @param nativeHandle the native handle
   * @param engine the engine
   */
  public JniModule(final long nativeHandle, final Engine engine) {
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
    final List<ai.tegmentum.wasmtime4j.ModuleImport> moduleImports = getModuleImports();
    final List<ImportType> imports = new java.util.ArrayList<>(moduleImports.size());
    for (final ai.tegmentum.wasmtime4j.ModuleImport moduleImport : moduleImports) {
      imports.add(moduleImport.getImportType());
    }
    return java.util.Collections.unmodifiableList(imports);
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
      final ai.tegmentum.wasmtime4j.Store store, final ai.tegmentum.wasmtime4j.ImportMap imports)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (imports == null) {
      throw new IllegalArgumentException("imports cannot be null");
    }
    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("store must be a JniStore instance");
    }
    ensureNotClosed();

    final JniStore jniStore = (JniStore) store;
    // For now, use simple instantiation - proper ImportMap support can be added later
    final long instanceHandle =
        nativeInstantiateModuleWithImports(nativeHandle, jniStore.getNativeHandle(), 0);

    if (instanceHandle == 0) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to instantiate module with imports - native instantiation returned null");
    }

    return new JniInstance(instanceHandle, this, store);
  }

  @Override
  public List<ExportType> getExports() {
    final List<ai.tegmentum.wasmtime4j.ModuleExport> moduleExports = getModuleExports();
    final List<ExportType> exports = new java.util.ArrayList<>(moduleExports.size());
    for (final ai.tegmentum.wasmtime4j.ModuleExport moduleExport : moduleExports) {
      exports.add(moduleExport.getExportType());
    }
    return java.util.Collections.unmodifiableList(exports);
  }

  @Override
  public Map<String, String> getCustomSections() {
    // TODO: Implement custom sections retrieval
    return Collections.emptyMap();
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.GlobalType> getGlobalTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.GlobalType> globalTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.GLOBAL) {
        globalTypes.add((ai.tegmentum.wasmtime4j.GlobalType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(globalTypes);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.TableType> getTableTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.TableType> tableTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.TABLE) {
        tableTypes.add((ai.tegmentum.wasmtime4j.TableType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(tableTypes);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.MemoryType> getMemoryTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.MemoryType> memoryTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.MEMORY) {
        memoryTypes.add((ai.tegmentum.wasmtime4j.MemoryType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(memoryTypes);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.FuncType> getFunctionTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.FuncType> functionTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.FUNCTION) {
        functionTypes.add((ai.tegmentum.wasmtime4j.FuncType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(functionTypes);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.ExportDescriptor> getExportDescriptors() {
    final List<ai.tegmentum.wasmtime4j.ModuleExport> moduleExports = getModuleExports();
    final List<ai.tegmentum.wasmtime4j.ExportDescriptor> descriptors =
        new java.util.ArrayList<>(moduleExports.size());

    for (final ai.tegmentum.wasmtime4j.ModuleExport moduleExport : moduleExports) {
      final ai.tegmentum.wasmtime4j.ExportType exportType = moduleExport.getExportType();
      descriptors.add(
          new ai.tegmentum.wasmtime4j.jni.type.JniExportDescriptor(
              exportType.getName(), exportType.getType()));
    }

    return java.util.Collections.unmodifiableList(descriptors);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.ImportDescriptor> getImportDescriptors() {
    final List<ai.tegmentum.wasmtime4j.ModuleImport> moduleImports = getModuleImports();
    final List<ai.tegmentum.wasmtime4j.ImportDescriptor> descriptors =
        new java.util.ArrayList<>(moduleImports.size());

    for (final ai.tegmentum.wasmtime4j.ModuleImport moduleImport : moduleImports) {
      final ai.tegmentum.wasmtime4j.ImportType importType = moduleImport.getImportType();
      descriptors.add(
          new ai.tegmentum.wasmtime4j.jni.type.JniImportDescriptor(
              importType.getModuleName(), importType.getName(), importType.getType()));
    }

    return java.util.Collections.unmodifiableList(descriptors);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.ModuleImport> getModuleImports() {
    ensureNotClosed();
    if (nativeHandle == 0) {
      throw new IllegalStateException("Module has invalid native handle");
    }

    return nativeGetModuleImports(nativeHandle);
  }

  @Override
  public List<ai.tegmentum.wasmtime4j.ModuleExport> getModuleExports() {
    ensureNotClosed();
    if (nativeHandle == 0) {
      throw new IllegalStateException("Module has invalid native handle");
    }

    return nativeGetModuleExports(nativeHandle);
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.FuncType> getFunctionType(
      final String functionName) {
    if (functionName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(functionName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.FUNCTION) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.FuncType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.GlobalType> getGlobalType(
      final String globalName) {
    if (globalName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(globalName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.GLOBAL) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.GlobalType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.MemoryType> getMemoryType(
      final String memoryName) {
    if (memoryName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(memoryName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.MEMORY) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.MemoryType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.TableType> getTableType(
      final String tableName) {
    if (tableName == null) {
      return java.util.Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(tableName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.TABLE) {
        return java.util.Optional.of((ai.tegmentum.wasmtime4j.TableType) export.getType());
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
    if (nativeHandle == 0) {
      throw new IllegalStateException("Module has invalid native handle");
    }

    return nativeHasExport(nativeHandle, name);
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
    if (nativeHandle == 0) {
      throw new IllegalStateException("Module has invalid native handle");
    }

    return nativeHasImport(nativeHandle, moduleName, fieldName);
  }

  @Override
  public boolean validateImports(final ai.tegmentum.wasmtime4j.ImportMap imports) {
    // TODO: Implement import validation
    return true;
  }

  @Override
  public byte[] serialize() {
    if (closed) {
      throw new IllegalStateException("Module has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Module has invalid native handle");
    }

    return nativeSerializeModule(nativeHandle);
  }

  @Override
  public boolean isValid() {
    return !closed && nativeHandle != 0;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      nativeDestroyModule(nativeHandle);
    }
  }

  /**
   * Ensures this module has not been closed.
   *
   * @throws IllegalStateException if the module is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Module has been closed");
    }
  }

  /**
   * Native method to instantiate a module without imports.
   *
   * @param moduleHandle the native module handle
   * @param storeHandle the native store handle
   * @return the native instance handle, or 0 on failure
   */
  private static native long nativeInstantiateModule(long moduleHandle, long storeHandle);

  /**
   * Native method to instantiate a module with imports.
   *
   * @param moduleHandle the native module handle
   * @param storeHandle the native store handle
   * @param importMapHandle the native import map handle (0 if no imports)
   * @return the native instance handle, or 0 on failure
   */
  private static native long nativeInstantiateModuleWithImports(
      long moduleHandle, long storeHandle, long importMapHandle);

  private native byte[] nativeSerializeModule(long handle);

  private native void nativeDestroyModule(long handle);

  /**
   * Native method to get module exports.
   *
   * @param moduleHandle the native module handle
   * @return list of module exports
   */
  private native List<ai.tegmentum.wasmtime4j.ModuleExport> nativeGetModuleExports(
      long moduleHandle);

  /**
   * Native method to get module imports.
   *
   * @param moduleHandle the native module handle
   * @return list of module imports
   */
  private native List<ai.tegmentum.wasmtime4j.ModuleImport> nativeGetModuleImports(
      long moduleHandle);

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
}
