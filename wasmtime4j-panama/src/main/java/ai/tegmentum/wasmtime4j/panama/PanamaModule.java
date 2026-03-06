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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Module.
 *
 * @since 1.0.0
 */
public final class PanamaModule implements Module {
  private static final Logger LOGGER = Logger.getLogger(PanamaModule.class.getName());
  private static final NativeEngineBindings NATIVE_BINDINGS = NativeEngineBindings.getInstance();

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeModule;
  private final byte[] wasmBytes;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama module from compiled bytecode.
   *
   * @param engine the engine used for compilation
   * @param wasmBytes the WebAssembly bytecode
   * @throws WasmException if module creation fails
   */
  public PanamaModule(final PanamaEngine engine, final byte[] wasmBytes) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("WASM bytes cannot be null or empty");
    }

    this.engine = engine;
    this.wasmBytes = wasmBytes.clone();
    this.arena = Arena.ofShared();

    // Allocate native memory for WASM bytes
    final MemorySegment bytesSegment = arena.allocate(wasmBytes.length);
    bytesSegment.copyFrom(MemorySegment.ofArray(wasmBytes));

    // Create native module via Panama FFI
    this.nativeModule =
        NATIVE_BINDINGS.moduleCreate(engine.getNativeEngine(), bytesSegment, wasmBytes.length);

    if (this.nativeModule == null || this.nativeModule.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to compile WASM module");
    }

    final MemorySegment moduleHandle = this.nativeModule;
    final Arena moduleArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaModule",
            () -> {
              if (nativeModule != null && !nativeModule.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(nativeModule);
              }
              arena.close();
            },
            this,
            () -> {
              if (moduleHandle != null && !moduleHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(moduleHandle);
              }
              moduleArena.close();
            });

    LOGGER.fine("Created Panama module");
  }

  /**
   * Creates a new PanamaModule from an existing native module pointer. Package-private constructor
   * for use by PanamaEngine.compileWat().
   *
   * @param engine the engine to use
   * @param nativeModulePtr the native module pointer
   * @throws WasmException if module is invalid
   */
  PanamaModule(final PanamaEngine engine, final MemorySegment nativeModulePtr)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }
    if (nativeModulePtr == null || nativeModulePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Native module pointer is null");
    }

    this.engine = engine;
    this.wasmBytes = null; // WAT modules don't have original bytes
    this.arena = Arena.ofShared();
    this.nativeModule = nativeModulePtr;

    final MemorySegment moduleHandle = this.nativeModule;
    final Arena moduleArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaModule",
            () -> {
              if (nativeModule != null && !nativeModule.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(nativeModule);
              }
              arena.close();
            },
            this,
            () -> {
              if (moduleHandle != null && !moduleHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(moduleHandle);
              }
              moduleArena.close();
            });

    LOGGER.fine("Created Panama module from native pointer");
  }

  /**
   * Creates a PanamaModule from a native module pointer without an associated PanamaEngine.
   *
   * <p>This constructor is used for modules extracted from component instances via {@link
   * ai.tegmentum.wasmtime4j.component.ComponentInstance#getModule(String)}. The extracted module is
   * owned by the native component engine and will be cleaned up when the module is closed.
   *
   * <p>Note: {@link #getEngine()} will return {@code null} for modules created with this
   * constructor.
   *
   * @param nativeModulePtr the native module pointer
   * @throws WasmException if the module pointer is null
   */
  PanamaModule(final MemorySegment nativeModulePtr) throws WasmException {
    if (nativeModulePtr == null || nativeModulePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Native module pointer is null");
    }

    this.engine = null;
    this.wasmBytes = null;
    this.arena = Arena.ofShared();
    this.nativeModule = nativeModulePtr;

    final MemorySegment moduleHandle = this.nativeModule;
    final Arena moduleArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaModule",
            () -> {
              if (nativeModule != null && !nativeModule.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(nativeModule);
              }
              arena.close();
            },
            this,
            () -> {
              if (moduleHandle != null && !moduleHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(moduleHandle);
              }
              moduleArena.close();
            });

    LOGGER.fine("Created Panama module from component instance export");
  }

  @Override
  public Instance instantiate(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    ensureNotClosed();
    return new PanamaInstance(this, (PanamaStore) store);
  }

  @Override
  public Instance instantiate(final Store store, final ImportMap imports) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (imports == null) {
      throw new IllegalArgumentException("Imports cannot be null");
    }
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance");
    }
    ensureNotClosed();

    final PanamaStore panamaStore = (PanamaStore) store;
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
        throw new WasmException("Missing import: " + modName + "::" + fieldName);
      }

      final Object value = moduleMap.get(fieldName);
      externs[i] = wrapAsPanamaExtern(value, panamaStore, imp);
    }

    return panamaStore.createInstance(this, externs);
  }

  /**
   * Wraps a WasmFunction/WasmGlobal/WasmMemory/WasmTable as the corresponding Panama Extern type.
   *
   * @param value the import value from the ImportMap
   * @param store the Panama store
   * @param imp the import type descriptor
   * @return the Extern wrapper
   * @throws WasmException if the value cannot be converted
   */
  private static ai.tegmentum.wasmtime4j.Extern wrapAsPanamaExtern(
      final Object value, final PanamaStore store, final ImportType imp) throws WasmException {
    if (value == null) {
      throw new WasmException(
          "Import value is null for " + imp.getModuleName() + "::" + imp.getName());
    }

    final ai.tegmentum.wasmtime4j.type.WasmTypeKind kind = imp.getType().getKind();

    switch (kind) {
      case FUNCTION:
        return wrapFunctionAsPanamaExtern(value, store, imp);
      case GLOBAL:
        if (value instanceof PanamaGlobal) {
          return new PanamaExternGlobal(((PanamaGlobal) value).getNativeGlobal(), store);
        }
        throw new WasmException(
            "Global import for "
                + imp.getModuleName()
                + "::"
                + imp.getName()
                + " must be a Panama runtime global (got "
                + value.getClass().getName()
                + ")");
      case MEMORY:
        if (value instanceof PanamaMemory) {
          return new PanamaExternMemory(((PanamaMemory) value).getNativeMemory(), store);
        }
        throw new WasmException(
            "Memory import for "
                + imp.getModuleName()
                + "::"
                + imp.getName()
                + " must be a Panama runtime memory (got "
                + value.getClass().getName()
                + ")");
      case TABLE:
        if (value instanceof PanamaTable) {
          return new PanamaExternTable(((PanamaTable) value).getNativeTable(), store);
        }
        throw new WasmException(
            "Table import for "
                + imp.getModuleName()
                + "::"
                + imp.getName()
                + " must be a Panama runtime table (got "
                + value.getClass().getName()
                + ")");
      default:
        throw new WasmException(
            "Unsupported import type kind: "
                + kind
                + " for "
                + imp.getModuleName()
                + "::"
                + imp.getName());
    }
  }

  /**
   * Wraps a WasmFunction as a PanamaExternFunc by extracting the native function handle.
   *
   * @param value the function value (must be a WasmFunction)
   * @param store the Panama store
   * @param imp the import type descriptor
   * @return the PanamaExternFunc wrapper
   * @throws WasmException if the function type is not supported
   */
  private static ai.tegmentum.wasmtime4j.Extern wrapFunctionAsPanamaExtern(
      final Object value, final PanamaStore store, final ImportType imp) throws WasmException {
    if (value instanceof PanamaHostFunction) {
      return new PanamaExternFunc(((PanamaHostFunction) value).getFunctionHandle(), store);
    }
    if (value instanceof PanamaCallerFunction) {
      return new PanamaExternFunc(((PanamaCallerFunction) value).getFuncHandle(), store);
    }
    throw new WasmException(
        "Function import for "
            + imp.getModuleName()
            + "::"
            + imp.getName()
            + " must be a Panama runtime function (PanamaHostFunction or PanamaCallerFunction)."
            + " Got "
            + value.getClass().getName());
  }

  @Override
  public List<ExportType> getExports() {
    ensureNotClosed();

    final long exportCount = NATIVE_BINDINGS.moduleExportsLen(nativeModule);
    if (exportCount == 0) {
      return java.util.Collections.emptyList();
    }

    final java.lang.foreign.MemorySegment jsonPtr =
        NATIVE_BINDINGS.moduleGetExportsJson(nativeModule);
    if (jsonPtr == null || jsonPtr.equals(java.lang.foreign.MemorySegment.NULL)) {
      LOGGER.warning("Failed to retrieve module exports");
      return java.util.Collections.emptyList();
    }

    try {
      final String jsonString = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);
      return java.util.Collections.unmodifiableList(parseExportsJson(jsonString));
    } finally {
      NATIVE_BINDINGS.moduleFreeString(jsonPtr);
    }
  }

  @Override
  public List<ImportType> getImports() {
    ensureNotClosed();

    final long importCount = NATIVE_BINDINGS.moduleImportsLen(nativeModule);
    if (importCount == 0) {
      return java.util.Collections.emptyList();
    }

    final java.lang.foreign.MemorySegment jsonPtr =
        NATIVE_BINDINGS.moduleGetImportsJson(nativeModule);
    if (jsonPtr == null || jsonPtr.equals(java.lang.foreign.MemorySegment.NULL)) {
      LOGGER.warning("Failed to retrieve module imports");
      return java.util.Collections.emptyList();
    }

    try {
      final String jsonString = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);
      return java.util.Collections.unmodifiableList(parseImportsJson(jsonString));
    } finally {
      NATIVE_BINDINGS.moduleFreeString(jsonPtr);
    }
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(name)) {
        return true;
      }
    }
    return false;
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
    final List<ImportType> imports = getImports();
    for (final ImportType importType : imports) {
      if (importType.getModuleName().equals(moduleName) && importType.getName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Module other) {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    if (resourceHandle.isClosed()) {
      return false;
    }
    if (!(other instanceof PanamaModule)) {
      return false;
    }
    final PanamaModule otherModule = (PanamaModule) other;
    if (otherModule.resourceHandle.isClosed()) {
      return false;
    }
    return NATIVE_BINDINGS.moduleSame(this.nativeModule, otherModule.nativeModule);
  }

  @Override
  public int getExportIndex(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    ensureNotClosed();
    try (final java.lang.foreign.Arena localArena = java.lang.foreign.Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment nameSegment = localArena.allocateFrom(name);
      return NATIVE_BINDINGS.moduleGetExportIndex(nativeModule, nameSegment);
    }
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.ModuleExport> getModuleExport(
      final String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    ensureNotClosed();

    try (final java.lang.foreign.Arena localArena = java.lang.foreign.Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment nameSegment = localArena.allocateFrom(name);
      final java.lang.foreign.MemorySegment outPtr =
          localArena.allocate(java.lang.foreign.ValueLayout.ADDRESS);

      final NativeInstanceBindings instanceBindings = NativeInstanceBindings.getInstance();
      final int result =
          instanceBindings.panamaModuleGetModuleExport(nativeModule, nameSegment, outPtr);

      if (result != 0) {
        return java.util.Optional.empty();
      }

      final java.lang.foreign.MemorySegment moduleExportPtr =
          outPtr.get(java.lang.foreign.ValueLayout.ADDRESS, 0);
      if (moduleExportPtr.equals(java.lang.foreign.MemorySegment.NULL)
          || moduleExportPtr.address() == 0) {
        return java.util.Optional.empty();
      }

      return java.util.Optional.of(new PanamaModuleExport(name, moduleExportPtr));
    } catch (final Exception e) {
      LOGGER.warning("Error getting module export: " + name + " - " + e.getMessage());
      return java.util.Optional.empty();
    }
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean validateImports(final ImportMap imports) {
    if (imports == null) {
      throw new IllegalArgumentException("Imports cannot be null");
    }
    ensureNotClosed();
    final List<ImportType> requiredImports = getImports();
    for (final ImportType requiredImport : requiredImports) {
      if (!imports.contains(requiredImport.getModuleName(), requiredImport.getName())) {
        LOGGER.fine(
            String.format(
                "Missing required import: %s.%s",
                requiredImport.getModuleName(), requiredImport.getName()));
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
  public String getName() {
    ensureNotClosed();
    try {
      final MemorySegment namePtr = NATIVE_BINDINGS.moduleGetName(nativeModule);
      if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
        final String name = namePtr.reinterpret(Long.MAX_VALUE).getString(0);
        NATIVE_BINDINGS.moduleFreeString(namePtr);
        return name;
      }
    } catch (final Exception e) {
      LOGGER.fine("Failed to get module name from native: " + e.getMessage());
    }
    return null;
  }

  @Override
  public void initializeCopyOnWriteImage() throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.moduleInitializeCowImage(nativeModule);
    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to initialize copy-on-write image");
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.ImageRange imageRange() throws WasmException {
    ensureNotClosed();
    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment startPtr = localArena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment endPtr = localArena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.moduleImageRange(nativeModule, startPtr, endPtr);
      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to get module image range");
      }
      return new ai.tegmentum.wasmtime4j.ImageRange(
          startPtr.get(ValueLayout.JAVA_LONG, 0), endPtr.get(ValueLayout.JAVA_LONG, 0));
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.ResourcesRequired resourcesRequired() {
    ensureNotClosed();
    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment minMemOut = localArena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment maxMemOut = localArena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment minTabOut = localArena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment maxTabOut = localArena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment numMemOut = localArena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment numTabOut = localArena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment numGlobOut = localArena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment numFuncOut = localArena.allocate(ValueLayout.JAVA_INT);

      final int result =
          NATIVE_BINDINGS.moduleResourcesRequired(
              nativeModule,
              minMemOut,
              maxMemOut,
              minTabOut,
              maxTabOut,
              numMemOut,
              numTabOut,
              numGlobOut,
              numFuncOut);

      if (result != 0) {
        // Fall back to default implementation on error
        return Module.super.resourcesRequired();
      }

      final long maxTab = maxTabOut.get(ValueLayout.JAVA_LONG, 0);
      return new ai.tegmentum.wasmtime4j.ResourcesRequired(
          minMemOut.get(ValueLayout.JAVA_LONG, 0),
          maxMemOut.get(ValueLayout.JAVA_LONG, 0),
          (int) minTabOut.get(ValueLayout.JAVA_LONG, 0),
          maxTab > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxTab,
          numMemOut.get(ValueLayout.JAVA_INT, 0),
          numTabOut.get(ValueLayout.JAVA_INT, 0),
          numGlobOut.get(ValueLayout.JAVA_INT, 0),
          numFuncOut.get(ValueLayout.JAVA_INT, 0));
    }
  }

  @Override
  public byte[] text() throws WasmException {
    ensureNotClosed();

    try (final Arena localArena = Arena.ofConfined()) {
      final MemorySegment dataPtrPtr = localArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment lenPtr = localArena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.moduleText(nativeModule, dataPtrPtr, lenPtr);
      if (result != 0) {
        throw new WasmException("Failed to get module text");
      }

      final long length = lenPtr.get(ValueLayout.JAVA_LONG, 0);
      final MemorySegment rawDataPtr = dataPtrPtr.get(ValueLayout.ADDRESS, 0);

      if (rawDataPtr == null || rawDataPtr.equals(MemorySegment.NULL) || length == 0) {
        return new byte[0];
      }

      try {
        final MemorySegment dataPtr = rawDataPtr.reinterpret(length);
        final byte[] textBytes = new byte[(int) length];
        MemorySegment.copy(dataPtr, ValueLayout.JAVA_BYTE, 0, textBytes, 0, (int) length);
        return textBytes;
      } finally {
        NATIVE_BINDINGS.freeByteArray(rawDataPtr, length);
      }
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable t) {
      throw new WasmException("Failed to get module text: " + t.getMessage());
    }
  }

  @Override
  public java.util.List<AddressMapping> addressMap() throws WasmException {
    ensureNotClosed();

    try (final Arena localArena = Arena.ofConfined()) {
      final MemorySegment codeOffsetsOut = localArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment wasmOffsetsOut = localArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment countOut = localArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          NATIVE_BINDINGS.moduleAddressMap(nativeModule, codeOffsetsOut, wasmOffsetsOut, countOut);

      if (result == 1) {
        // Address map not available
        return Collections.emptyList();
      }
      if (result != 0) {
        throw new WasmException("Failed to get module address map");
      }

      final long count = countOut.get(ValueLayout.JAVA_LONG, 0);
      if (count == 0) {
        return Collections.emptyList();
      }

      final MemorySegment codeOffsetsPtr = codeOffsetsOut.get(ValueLayout.ADDRESS, 0);
      final MemorySegment wasmOffsetsPtr = wasmOffsetsOut.get(ValueLayout.ADDRESS, 0);

      try {
        final MemorySegment codeOffsets = codeOffsetsPtr.reinterpret(count * Long.BYTES);
        final MemorySegment wasmOffsets = wasmOffsetsPtr.reinterpret(count * Long.BYTES);

        final java.util.List<AddressMapping> mappings = new java.util.ArrayList<>((int) count);
        for (int i = 0; i < count; i++) {
          final long codeOffset = codeOffsets.getAtIndex(ValueLayout.JAVA_LONG, i);
          final long wasmOffsetRaw = wasmOffsets.getAtIndex(ValueLayout.JAVA_LONG, i);
          final java.util.OptionalInt wasmOffset =
              wasmOffsetRaw < 0
                  ? java.util.OptionalInt.empty()
                  : java.util.OptionalInt.of((int) wasmOffsetRaw);
          mappings.add(new AddressMapping(codeOffset, wasmOffset));
        }
        return Collections.unmodifiableList(mappings);
      } finally {
        NATIVE_BINDINGS.freeAddressMap(codeOffsetsPtr, wasmOffsetsPtr, count);
      }
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable t) {
      throw new WasmException("Failed to get module address map: " + t.getMessage());
    }
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public byte[] serialize() throws WasmException {
    ensureNotClosed();

    try (final Arena localArena = Arena.ofConfined()) {
      final MemorySegment dataPtrPtr = localArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment lenPtr = localArena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.moduleSerialize(nativeModule, dataPtrPtr, lenPtr);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to serialize module");
      }

      final long length = lenPtr.get(ValueLayout.JAVA_LONG, 0);
      final MemorySegment rawDataPtr = dataPtrPtr.get(ValueLayout.ADDRESS, 0);

      if (rawDataPtr == null || rawDataPtr.equals(MemorySegment.NULL) || length == 0) {
        return new byte[0];
      }

      try {
        final MemorySegment dataPtr = rawDataPtr.reinterpret(length);
        final byte[] serialized = new byte[(int) length];
        MemorySegment.copy(dataPtr, ValueLayout.JAVA_BYTE, 0, serialized, 0, (int) length);
        LOGGER.fine("Serialized module to " + length + " bytes");
        return serialized;
      } finally {
        NATIVE_BINDINGS.freeByteArray(rawDataPtr, length);
      }
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable t) {
      throw new WasmException("Failed to serialize module: " + t.getMessage());
    }
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native module pointer.
   *
   * @return native module memory segment
   */
  public MemorySegment getNativeModule() {
    return nativeModule;
  }

  /**
   * Gets the original WebAssembly bytecode.
   *
   * @return a copy of the WebAssembly bytecode
   */
  public byte[] getWasmBytes() {
    if (wasmBytes == null) {
      return null;
    }
    return wasmBytes.clone();
  }

  /**
   * Parses JSON string containing module imports.
   *
   * @param jsonString JSON string from native code
   * @return list of ImportType objects
   */
  private List<ImportType> parseImportsJson(final String jsonString) {
    final com.google.gson.Gson gson = new com.google.gson.Gson();
    final com.google.gson.JsonArray jsonArray =
        gson.fromJson(jsonString, com.google.gson.JsonArray.class);

    final List<ImportType> imports = new java.util.ArrayList<>();

    for (final com.google.gson.JsonElement element : jsonArray) {
      final com.google.gson.JsonObject importObj = element.getAsJsonObject();
      final String moduleName = importObj.get("module").getAsString();
      final String fieldName = importObj.get("name").getAsString();
      final com.google.gson.JsonObject importTypeObj = importObj.getAsJsonObject("import_type");

      final ai.tegmentum.wasmtime4j.type.WasmType wasmType = parseTypeJson(importTypeObj);
      imports.add(new ImportType(moduleName, fieldName, wasmType));
    }

    return imports;
  }

  /**
   * Parses JSON string containing module exports.
   *
   * @param jsonString JSON string from native code
   * @return list of ExportType objects
   */
  private List<ExportType> parseExportsJson(final String jsonString) {
    final com.google.gson.Gson gson = new com.google.gson.Gson();
    final com.google.gson.JsonArray jsonArray =
        gson.fromJson(jsonString, com.google.gson.JsonArray.class);

    final List<ExportType> exports = new java.util.ArrayList<>();

    for (final com.google.gson.JsonElement element : jsonArray) {
      final com.google.gson.JsonObject exportObj = element.getAsJsonObject();
      final String name = exportObj.get("name").getAsString();
      final com.google.gson.JsonObject exportTypeObj = exportObj.getAsJsonObject("export_type");

      final ai.tegmentum.wasmtime4j.type.WasmType wasmType = parseTypeJson(exportTypeObj);
      exports.add(new ExportType(name, wasmType));
    }

    return exports;
  }

  /**
   * Parses a type JSON object (import or export) to WasmType.
   *
   * @param typeObj JSON object containing type information
   * @return WasmType instance
   */
  private ai.tegmentum.wasmtime4j.type.WasmType parseTypeJson(
      final com.google.gson.JsonObject typeObj) {
    final String typeKind = typeObj.keySet().iterator().next();

    return switch (typeKind) {
      case "Function" -> parseFunctionType(typeObj.getAsJsonObject("Function"));
      case "Global" -> parseGlobalType(typeObj.getAsJsonArray("Global"));
      case "Memory" -> parseMemoryType(typeObj.getAsJsonArray("Memory"));
      case "Table" -> parseTableType(typeObj.getAsJsonArray("Table"));
      default -> throw new IllegalArgumentException("Unknown type kind: " + typeKind);
    };
  }

  /**
   * Parses function type JSON.
   *
   * @param funcObj JSON object with params and returns arrays
   * @return FuncType instance
   */
  private ai.tegmentum.wasmtime4j.type.FuncType parseFunctionType(
      final com.google.gson.JsonObject funcObj) {
    final com.google.gson.JsonArray paramsArray = funcObj.getAsJsonArray("params");
    final com.google.gson.JsonArray returnsArray = funcObj.getAsJsonArray("returns");

    final List<ai.tegmentum.wasmtime4j.WasmValueType> params = parseValueTypeArray(paramsArray);
    final List<ai.tegmentum.wasmtime4j.WasmValueType> results = parseValueTypeArray(returnsArray);

    return ai.tegmentum.wasmtime4j.panama.type.PanamaFuncType.of(params, results);
  }

  /**
   * Parses global type JSON.
   *
   * @param globalArray JSON array [valueType, isMutable]
   * @return GlobalType instance
   */
  private ai.tegmentum.wasmtime4j.type.GlobalType parseGlobalType(
      final com.google.gson.JsonArray globalArray) {
    final String valueTypeStr = globalArray.get(0).getAsString();
    final boolean isMutable = globalArray.get(1).getAsBoolean();

    final ai.tegmentum.wasmtime4j.WasmValueType valueType = parseValueType(valueTypeStr);

    return ai.tegmentum.wasmtime4j.panama.type.PanamaGlobalType.of(valueType, isMutable);
  }

  /**
   * Parses memory type JSON.
   *
   * @param memoryArray JSON array [min, max(optional), is64, isShared, pageSizeLog2]
   * @return MemoryType instance
   */
  private ai.tegmentum.wasmtime4j.type.MemoryType parseMemoryType(
      final com.google.gson.JsonArray memoryArray) {
    final long minimum = memoryArray.get(0).getAsLong();
    final com.google.gson.JsonElement maxElement = memoryArray.get(1);
    final Long maximum = maxElement.isJsonNull() ? null : maxElement.getAsLong();
    final boolean is64Bit = memoryArray.get(2).getAsBoolean();
    final boolean isShared = memoryArray.get(3).getAsBoolean();
    final int pageSizeLog2 = memoryArray.size() > 4 ? memoryArray.get(4).getAsInt() : 16;

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaMemoryType(
        minimum, maximum, is64Bit, isShared, pageSizeLog2);
  }

  /**
   * Parses table type JSON.
   *
   * @param tableArray JSON array [elementType, min, max(optional)]
   * @return TableType instance
   */
  private ai.tegmentum.wasmtime4j.type.TableType parseTableType(
      final com.google.gson.JsonArray tableArray) {
    final String elementTypeStr = tableArray.get(0).getAsString();
    final long minimum = tableArray.get(1).getAsLong();
    final com.google.gson.JsonElement maxElement = tableArray.get(2);
    final Long maximum = maxElement.isJsonNull() ? null : maxElement.getAsLong();

    final ai.tegmentum.wasmtime4j.WasmValueType elementType = parseValueType(elementTypeStr);

    return ai.tegmentum.wasmtime4j.panama.type.PanamaTableType.of(elementType, minimum, maximum);
  }

  /**
   * Parses array of value types.
   *
   * @param array JSON array of value type strings
   * @return list of WasmValueType
   */
  private List<ai.tegmentum.wasmtime4j.WasmValueType> parseValueTypeArray(
      final com.google.gson.JsonArray array) {
    final List<ai.tegmentum.wasmtime4j.WasmValueType> types = new java.util.ArrayList<>();
    for (final com.google.gson.JsonElement element : array) {
      types.add(parseValueType(element.getAsString()));
    }
    return types;
  }

  /**
   * Parses value type string to enum.
   *
   * @param typeStr value type string from Rust (e.g., "I32", "FuncRef")
   * @return WasmValueType enum value
   */
  private ai.tegmentum.wasmtime4j.WasmValueType parseValueType(final String typeStr) {
    return switch (typeStr) {
      case "I32" -> ai.tegmentum.wasmtime4j.WasmValueType.I32;
      case "I64" -> ai.tegmentum.wasmtime4j.WasmValueType.I64;
      case "F32" -> ai.tegmentum.wasmtime4j.WasmValueType.F32;
      case "F64" -> ai.tegmentum.wasmtime4j.WasmValueType.F64;
      case "V128" -> ai.tegmentum.wasmtime4j.WasmValueType.V128;
      case "FuncRef" -> ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF;
      case "ExternRef" -> ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF;
      default -> throw new IllegalArgumentException("Unknown value type: " + typeStr);
    };
  }

  @Override
  public Iterable<ai.tegmentum.wasmtime4j.func.FunctionInfo> functions() {
    ensureNotClosed();

    final java.lang.foreign.MemorySegment jsonPtr =
        NATIVE_BINDINGS.moduleGetAllFunctions(nativeModule);
    if (jsonPtr == null || jsonPtr.equals(java.lang.foreign.MemorySegment.NULL)) {
      return java.util.Collections.emptyList();
    }

    try {
      final String jsonString = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);
      if (jsonString == null || jsonString.isEmpty() || jsonString.equals("[]")) {
        return java.util.Collections.emptyList();
      }
      return parseFunctionsJson(jsonString);
    } finally {
      NATIVE_BINDINGS.moduleFreeString(jsonPtr);
    }
  }

  @SuppressFBWarnings(value = "IMPROPER_UNICODE", justification = "JSON keys are ASCII-only")
  private static java.util.List<ai.tegmentum.wasmtime4j.func.FunctionInfo> parseFunctionsJson(
      final String json) {
    final java.util.List<ai.tegmentum.wasmtime4j.func.FunctionInfo> result =
        new java.util.ArrayList<>();

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

    final String[] pairs = obj.split(",(?=\\s*\")");
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
            name = value.substring(1, value.length() - 1);
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

  private static void parseValTypes(
      final String arrayStr, final java.util.List<ai.tegmentum.wasmtime4j.type.ValType> types) {
    final String trimmed = arrayStr.trim();
    if (trimmed.equals("[]")) {
      return;
    }
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
        return ai.tegmentum.wasmtime4j.type.ValType.i32();
    }
  }

  /**
   * Ensures the module is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
