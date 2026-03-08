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
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

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
    beginOperation();
    try {
      return nativeGetModuleName(nativeHandle);
    } catch (final Exception e) {
      java.util.logging.Logger.getLogger(JniModule.class.getName())
          .fine("Failed to get module name from native: " + e.getMessage());
      return null;
    } finally {
      endOperation();
    }
  }

  @Override
  @SuppressFBWarnings(
      value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
      justification =
          "Error details are logged internally only; exception thrown to caller has sanitized"
              + " message")
  public List<ImportType> getImports() {
    beginOperation();
    try {
      final List<ImportType> result = nativeGetModuleImports(nativeHandle);
      if (result == null) {
        java.util.logging.Logger.getLogger(JniModule.class.getName())
            .warning("nativeGetModuleImports returned null for handle: " + nativeHandle);
        return java.util.Collections.emptyList();
      }
      return java.util.Collections.unmodifiableList(result);
    } catch (final Throwable t) {
      java.util.logging.Logger.getLogger(JniModule.class.getName())
          .log(java.util.logging.Level.SEVERE, "nativeGetModuleImports failed", t);
      throw new RuntimeException("Failed to get module imports");
    } finally {
      endOperation();
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
    beginOperation();
    try {
      final JniStore jniStore = (JniStore) store;
      final long instanceHandle = nativeInstantiateModule(nativeHandle, jniStore.getNativeHandle());

      if (instanceHandle == 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to instantiate module - native instantiation returned null");
      }

      return new JniInstance(instanceHandle, this, store);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Instance instantiate(
      final ai.tegmentum.wasmtime4j.Store store,
      final ai.tegmentum.wasmtime4j.validation.ImportMap imports)
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
    beginOperation();
    try {
      final JniStore jniStore = (JniStore) store;
      final java.util.List<ImportType> moduleImports = getImports();

      if (moduleImports.isEmpty()) {
        return instantiate(store);
      }

      final java.util.Map<String, java.util.Map<String, Object>> importData = imports.getImports();
      final ai.tegmentum.wasmtime4j.Extern[] externs =
          new ai.tegmentum.wasmtime4j.Extern[moduleImports.size()];

      for (int i = 0; i < moduleImports.size(); i++) {
        final ImportType imp = moduleImports.get(i);
        final String modName = imp.getModuleName();
        final String fieldName = imp.getName();

        final java.util.Map<String, Object> moduleMap = importData.get(modName);
        if (moduleMap == null || !moduleMap.containsKey(fieldName)) {
          throw new ai.tegmentum.wasmtime4j.exception.WasmException(
              "Missing import: " + modName + "::" + fieldName);
        }

        final Object value = moduleMap.get(fieldName);
        externs[i] = wrapAsJniExtern(value, jniStore, imp);
      }

      return jniStore.createInstance(this, externs);
    } finally {
      endOperation();
    }
  }

  /**
   * Wraps a WasmFunction/WasmGlobal/WasmMemory/WasmTable as the corresponding JNI Extern type.
   *
   * @param value the import value from the ImportMap
   * @param store the JNI store
   * @param imp the import type descriptor
   * @return the Extern wrapper
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if the value cannot be converted
   */
  private static ai.tegmentum.wasmtime4j.Extern wrapAsJniExtern(
      final Object value, final JniStore store, final ImportType imp)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (value == null) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Import value is null for " + imp.getModuleName() + "::" + imp.getName());
    }

    final ai.tegmentum.wasmtime4j.type.WasmTypeKind kind = imp.getType().getKind();

    if (!(value instanceof JniResource)) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Import value for "
              + imp.getModuleName()
              + "::"
              + imp.getName()
              + " must be a JNI runtime object (got "
              + value.getClass().getName()
              + ")");
    }

    final long nativeHandle = ((JniResource) value).getNativeHandle();

    switch (kind) {
      case FUNCTION:
        return new JniExternFunc(nativeHandle, store);
      case GLOBAL:
        return new JniExternGlobal(nativeHandle, store);
      case MEMORY:
        return new JniExternMemory(nativeHandle, store);
      case TABLE:
        return new JniExternTable(nativeHandle, store);
      default:
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Unsupported import type kind: "
                + kind
                + " for "
                + imp.getModuleName()
                + "::"
                + imp.getName());
    }
  }

  @Override
  @SuppressFBWarnings(
      value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
      justification =
          "Error details are logged internally only; exception thrown to caller has sanitized"
              + " message")
  public List<ExportType> getExports() {
    beginOperation();
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
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }
    beginOperation();
    try {
      return nativeHasExport(nativeHandle, name);
    } catch (final Throwable t) {
      // Defensive: Return false on native error instead of crashing JVM
      return false;
    } finally {
      endOperation();
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
    beginOperation();
    try {
      return nativeHasImport(nativeHandle, moduleName, fieldName);
    } catch (final Throwable t) {
      // Defensive: Return false on native error instead of crashing JVM
      return false;
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Module other) {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    if (!tryBeginOperation()) {
      return false;
    }
    try {
      if (!(other instanceof JniModule)) {
        return false;
      }
      final JniModule otherModule = (JniModule) other;
      if (otherModule.isClosed()) {
        return false;
      }
      return nativeModuleSame(nativeHandle, otherModule.nativeHandle);
    } catch (final Throwable t) {
      return false;
    } finally {
      endOperation();
    }
  }

  @Override
  public int getExportIndex(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    beginOperation();
    try {
      return (int) nativeGetExportIndex(nativeHandle, name);
    } catch (final Throwable t) {
      return -1;
    } finally {
      endOperation();
    }
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.ModuleExport> getModuleExport(
      final String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    beginOperation();
    try {
      final long handle = nativeGetModuleExport(nativeHandle, name);
      if (handle == 0) {
        return java.util.Optional.empty();
      }
      return java.util.Optional.of(new JniModuleExport(name, handle));
    } catch (final Throwable t) {
      return java.util.Optional.empty();
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean validateImports(final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
    if (imports == null) {
      throw new IllegalArgumentException("imports cannot be null");
    }
    beginOperation();
    try {
      final List<ImportType> importTypes = getImports();

      for (final ImportType importType : importTypes) {
        final String moduleName = importType.getModuleName();
        final String fieldName = importType.getName();

        if (!imports.contains(moduleName, fieldName)) {
          return false;
        }
      }

      return true;
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.validation.ImportValidation validateImportsDetailed(
      final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
    if (imports == null) {
      throw new IllegalArgumentException("imports cannot be null");
    }
    beginOperation();
    try {
      return ai.tegmentum.wasmtime4j.util.ModuleValidationSupport.validateImportsDetailed(
          getImports(), imports);
    } finally {
      endOperation();
    }
  }

  @Override
  public byte[] serialize() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      return nativeSerializeModule(nativeHandle);
    } catch (final Throwable t) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to serialize module: " + t.getMessage());
    } finally {
      endOperation();
    }
  }

  @Override
  public void initializeCopyOnWriteImage() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      nativeInitializeCopyOnWriteImage(nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.ImageRange imageRange()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      final long[] range = nativeGetModuleImageRange(nativeHandle);
      if (range == null || range.length < 2) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to get module image range");
      }
      return new ai.tegmentum.wasmtime4j.ImageRange(range[0], range[1]);
    } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
      throw e;
    } catch (final Throwable t) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to get module image range: " + t.getMessage());
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.ResourcesRequired resourcesRequired() {
    beginOperation();
    try {
      final long[] data = nativeGetModuleResourcesRequired(nativeHandle);
      if (data == null || data.length < 8) {
        // Fall back to default implementation if native call fails
        return Module.super.resourcesRequired();
      }
      return new ai.tegmentum.wasmtime4j.ResourcesRequired(
          data[0], // minimumMemoryBytes
          data[1], // maximumMemoryBytes (-1 if unbounded)
          (int) data[2], // minimumTableElements
          data[3] > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) data[3], // maximumTableElements
          (int) data[4], // numMemories
          (int) data[5], // numTables
          (int) data[6], // numGlobals
          (int) data[7]); // numFunctions
    } finally {
      endOperation();
    }
  }

  @Override
  public byte[] text() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      final byte[] result = nativeGetModuleText(nativeHandle);
      if (result == null) {
        return new byte[0];
      }
      return result;
    } catch (final Throwable t) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to get module text: " + t.getMessage());
    } finally {
      endOperation();
    }
  }

  @Override
  public java.util.List<AddressMapping> addressMap()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    beginOperation();
    try {
      final long[] raw = nativeGetModuleAddressMap(nativeHandle);
      if (raw == null) {
        return java.util.Collections.emptyList();
      }
      final java.util.List<AddressMapping> result = new java.util.ArrayList<>(raw.length / 2);
      for (int i = 0; i < raw.length; i += 2) {
        final long codeOffset = raw[i];
        final long wasmOffsetRaw = raw[i + 1];
        final java.util.OptionalInt wasmOffset =
            wasmOffsetRaw < 0
                ? java.util.OptionalInt.empty()
                : java.util.OptionalInt.of((int) wasmOffsetRaw);
        result.add(new AddressMapping(codeOffset, wasmOffset));
      }
      return java.util.Collections.unmodifiableList(result);
    } catch (final Throwable t) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to get module address map: " + t.getMessage());
    } finally {
      endOperation();
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
   * Native method to compile a module from a file path.
   *
   * @param engineHandle the native engine handle
   * @param path the file path to compile from
   * @return the native module handle, or 0 on failure
   */
  static native long nativeCompileFromFile(long engineHandle, String path);

  /**
   * Native method to load a module from a trusted file, skipping validation.
   *
   * @param engineHandle the native engine handle
   * @param path the file path
   * @return the native module handle, or 0 on failure
   */
  static native long nativeFromTrustedFile(long engineHandle, String path);

  /**
   * Native method to deserialize a module from raw bytes (no file format wrapper).
   *
   * @param engineHandle the native engine handle
   * @param bytes the raw bytes
   * @return the native module handle, or 0 on failure
   */
  static native long nativeDeserializeRaw(long engineHandle, byte[] bytes);

  /**
   * Native method to deserialize a module from an open file descriptor (Unix only).
   *
   * @param engineHandle the native engine handle
   * @param fd the file descriptor
   * @return the native module handle, or 0 on failure
   */
  static native long nativeDeserializeOpenFile(long engineHandle, int fd);

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

  private native long nativeGetModuleExport(long moduleHandle, String exportName);

  private static native boolean nativeInitializeCopyOnWriteImage(long moduleHandle);

  private static native long[] nativeGetModuleResourcesRequired(long moduleHandle);

  private static native long[] nativeGetModuleImageRange(long moduleHandle);

  /**
   * Native method to get compiled machine code text from module.
   *
   * @param moduleHandle the native module handle
   * @return byte array of compiled machine code, or null on error
   */
  private native byte[] nativeGetModuleText(long moduleHandle);

  /**
   * Native method to get address map from module.
   *
   * <p>Returns interleaved long array [codeOffset0, wasmOffset0, codeOffset1, wasmOffset1, ...].
   * wasmOffset is -1 if no corresponding wasm offset exists.
   *
   * @param moduleHandle the native module handle
   * @return interleaved long array of address mappings, or null if not available
   */
  private native long[] nativeGetModuleAddressMap(long moduleHandle);

  /**
   * Native method to get the WASM module name from the custom name section.
   *
   * @param moduleHandle the native module handle
   * @return the module name, or null if the module has no name section
   */
  private native String nativeGetModuleName(long moduleHandle);

  /**
   * Native method to validate WebAssembly bytecode using full Wasmtime validation.
   *
   * @param bytecode the WebAssembly bytecode to validate
   * @return true if valid, false if invalid
   */
  private static native boolean nativeValidateModule(byte[] bytecode);

  /**
   * Native method to get all functions (imports, exports, and internal) as JSON.
   *
   * @param moduleHandle the native module handle
   * @return JSON string containing function info array, or null on error
   */
  private static native String nativeGetAllFunctions(long moduleHandle);

  /**
   * Validates WebAssembly bytecode using full Wasmtime validation.
   *
   * @param bytecode the WebAssembly bytecode to validate
   * @return true if valid, false otherwise
   */
  static boolean validateModuleBytes(final byte[] bytecode) {
    return nativeValidateModule(bytecode);
  }

  @Override
  public Iterable<ai.tegmentum.wasmtime4j.func.FunctionInfo> functions() {
    beginOperation();
    try {
      final String json = nativeGetAllFunctions(nativeHandle);
      if (json == null || json.isEmpty() || json.equals("[]")) {
        return java.util.Collections.emptyList();
      }
      return parseFunctionsJson(json);
    } finally {
      endOperation();
    }
  }

  private static java.util.List<ai.tegmentum.wasmtime4j.func.FunctionInfo> parseFunctionsJson(
      final String json) {
    final java.util.List<ai.tegmentum.wasmtime4j.func.FunctionInfo> result =
        new java.util.ArrayList<>();

    // Minimal JSON parsing for [{...},{...},...] array of function objects
    int pos = 1; // skip opening [
    while (pos < json.length()) {
      final int objStart = json.indexOf('{', pos);
      if (objStart < 0) {
        break;
      }
      final int objEnd = json.indexOf('}', objStart);
      if (objEnd < 0) {
        break;
      }
      final String obj = json.substring(objStart + 1, objEnd);
      result.add(parseSingleFunction(obj));
      pos = objEnd + 1;
    }
    return result;
  }

  @SuppressFBWarnings(value = "IMPROPER_UNICODE", justification = "JSON keys are ASCII-only")
  private static ai.tegmentum.wasmtime4j.func.FunctionInfo parseSingleFunction(final String obj) {
    int index = 0;
    String name = null;
    boolean isImport = false;
    boolean isExported = false;
    final java.util.List<ai.tegmentum.wasmtime4j.type.ValType> params = new java.util.ArrayList<>();
    final java.util.List<ai.tegmentum.wasmtime4j.type.ValType> returns =
        new java.util.ArrayList<>();

    // Parse key-value pairs (bracket-aware split to avoid splitting inside arrays)
    final java.util.List<String> pairs = splitTopLevelPairs(obj);
    for (final String pair : pairs) {
      final int colonPos = pair.indexOf(':');
      if (colonPos < 0) {
        continue;
      }
      final String key = pair.substring(0, colonPos).trim().replace("\"", "");
      final String value = pair.substring(colonPos + 1).trim();

      switch (key) {
        case "index":
          index = Integer.parseInt(value);
          break;
        case "name":
          if (!"null".equals(value)) {
            name = value.substring(1, value.length() - 1); // strip quotes
          }
          break;
        case "isImport":
          isImport = Boolean.parseBoolean(value);
          break;
        case "isExported":
          isExported = Boolean.parseBoolean(value);
          break;
        case "params":
          parseValTypes(value, params);
          break;
        case "returns":
          parseValTypes(value, returns);
          break;
        default:
          break;
      }
    }

    final ai.tegmentum.wasmtime4j.type.FuncType funcType =
        ai.tegmentum.wasmtime4j.type.FuncType.of(
            params.toArray(new ai.tegmentum.wasmtime4j.type.ValType[0]),
            returns.toArray(new ai.tegmentum.wasmtime4j.type.ValType[0]));
    return new ai.tegmentum.wasmtime4j.func.FunctionInfo(
        index, name, funcType, isImport, isExported);
  }

  private static java.util.List<String> splitTopLevelPairs(final String obj) {
    final java.util.List<String> pairs = new java.util.ArrayList<>();
    int depth = 0;
    int start = 0;
    for (int i = 0; i < obj.length(); i++) {
      final char ch = obj.charAt(i);
      if (ch == '[') {
        depth++;
      } else if (ch == ']') {
        depth--;
      } else if (ch == ',' && depth == 0) {
        pairs.add(obj.substring(start, i));
        start = i + 1;
      }
    }
    if (start < obj.length()) {
      pairs.add(obj.substring(start));
    }
    return pairs;
  }

  private static void parseValTypes(
      final String arrayStr, final java.util.List<ai.tegmentum.wasmtime4j.type.ValType> types) {
    // Parse ["i32","i64",...] or []
    final String trimmed = arrayStr.trim();
    if (trimmed.equals("[]")) {
      return;
    }
    // Strip brackets
    final String inner = trimmed.substring(1, trimmed.length() - 1);
    if (inner.isEmpty()) {
      return;
    }
    final String[] typeStrs = inner.split(",");
    for (final String typeStr : typeStrs) {
      final String cleaned = typeStr.trim().replace("\"", "");
      types.add(stringToValType(cleaned));
    }
  }

  private static ai.tegmentum.wasmtime4j.type.ValType stringToValType(final String typeStr) {
    switch (typeStr) {
      case "i32":
        return ai.tegmentum.wasmtime4j.type.ValType.i32();
      case "i64":
        return ai.tegmentum.wasmtime4j.type.ValType.i64();
      case "f32":
        return ai.tegmentum.wasmtime4j.type.ValType.f32();
      case "f64":
        return ai.tegmentum.wasmtime4j.type.ValType.f64();
      case "v128":
        return ai.tegmentum.wasmtime4j.type.ValType.v128();
      case "externref":
        return ai.tegmentum.wasmtime4j.type.ValType.externref();
      case "funcref":
        return ai.tegmentum.wasmtime4j.type.ValType.funcref();
      case "anyref":
        return ai.tegmentum.wasmtime4j.type.ValType.anyref();
      default:
        return ai.tegmentum.wasmtime4j.type.ValType.i32(); // fallback
    }
  }
}
