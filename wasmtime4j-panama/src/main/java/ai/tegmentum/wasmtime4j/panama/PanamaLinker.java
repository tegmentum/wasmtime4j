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
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaMemoryManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly linker interface.
 *
 * <p>A WebAssembly linker provides the mechanism to define host functions and bind imports before
 * instantiating WebAssembly modules. This implementation uses Panama FFI for direct access to the
 * underlying Wasmtime linker structure with zero-copy optimization.
 *
 * <p>Linkers enable advanced WebAssembly integration patterns including host function binding,
 * module linking, and WASI integration. All operations use Arena-based resource management for
 * automatic cleanup.
 *
 * @since 1.0.0
 */
public final class PanamaLinker implements Linker, AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PanamaLinker.class.getName());

  /** Memory segment representing the native linker handle. */
  private final MemorySegment linkerHandle;

  /** Arena for managing memory lifecycle. */
  private final Arena arena;

  /** Reference to the engine this linker was created for. */
  private final Engine engine;

  /** Flag to track if this linker has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Memory manager for optimized FFI operations. */
  private final PanamaMemoryManager memoryManager;

  // Method handles for native functions (will be initialized from native library)
  private static final MethodHandle CREATE_LINKER;
  private static final MethodHandle DEFINE_FUNCTION;
  private static final MethodHandle DEFINE_HOST_FUNCTION;
  private static final MethodHandle DEFINE_HOST_FUNCTION_SIMPLE;
  private static final MethodHandle DEFINE_MEMORY;
  private static final MethodHandle DEFINE_TABLE;
  private static final MethodHandle DEFINE_GLOBAL;
  private static final MethodHandle DEFINE_INSTANCE;
  private static final MethodHandle CREATE_ALIAS;
  private static final MethodHandle ALIAS_MODULE;
  private static final MethodHandle INSTANTIATE;
  private static final MethodHandle ENABLE_WASI;
  private static final MethodHandle DEFINE_WASI;
  private static final MethodHandle DESTROY_LINKER;

  static {
    // Initialize method handles - these would be loaded from the native library
    // For now, we'll use placeholder implementations
    try {
      CREATE_LINKER = PanamaNativeLibrary.findFunction("wasmtime4j_linker_create",
          FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_FUNCTION = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_function",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_HOST_FUNCTION = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_host_function",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, // linker
              ValueLayout.ADDRESS, // module_name
              ValueLayout.ADDRESS, // function_name
              ValueLayout.ADDRESS, // param_types
              ValueLayout.JAVA_INT, // param_count
              ValueLayout.ADDRESS, // return_types
              ValueLayout.JAVA_INT, // return_count
              ValueLayout.ADDRESS)); // host_function
      DEFINE_HOST_FUNCTION_SIMPLE = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_host_function_simple",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_MEMORY = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_memory",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_TABLE = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_table",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_GLOBAL = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_global",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_INSTANCE = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_instance",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      CREATE_ALIAS = PanamaNativeLibrary.findFunction("wasmtime4j_linker_alias",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      ALIAS_MODULE = PanamaNativeLibrary.findFunction("wasmtime4j_linker_alias_module",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      INSTANTIATE = PanamaNativeLibrary.findFunction("wasmtime4j_linker_instantiate",
          FunctionDescriptor.of(ValueLayout.ADDRESS,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      ENABLE_WASI = PanamaNativeLibrary.findFunction("wasmtime4j_linker_enable_wasi",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS));
      DEFINE_WASI = PanamaNativeLibrary.findFunction("wasmtime4j_linker_define_wasi",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DESTROY_LINKER = PanamaNativeLibrary.findFunction("wasmtime4j_linker_destroy",
          FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    } catch (final Exception e) {
      throw new ExceptionInInitializerError("Failed to initialize Panama linker: " + e.getMessage());
    }
  }

  /**
   * Creates a new Panama linker with the given handle, arena, and engine.
   *
   * @param linkerHandle the native linker handle
   * @param arena the arena for memory management
   * @param engine the engine this linker was created for
   * @throws IllegalArgumentException if any parameter is null or handle is NULL
   */
  private PanamaLinker(final MemorySegment linkerHandle, final Arena arena, final Engine engine) {
    PanamaValidation.requireNonNull(linkerHandle, "linkerHandle");
    PanamaValidation.requireNonNull(arena, "arena");
    PanamaValidation.requireNonNull(engine, "engine");

    if (linkerHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Linker handle cannot be NULL");
    }

    this.linkerHandle = linkerHandle;
    this.arena = arena;
    this.engine = engine;
    this.memoryManager = new PanamaMemoryManager(arena);

    LOGGER.fine("Created Panama linker with handle: " + linkerHandle);
  }

  /**
   * Creates a new linker for the given engine.
   *
   * @param engine the engine to create the linker for
   * @return a new PanamaLinker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null or not a Panama engine
   */
  public static PanamaLinker create(final Engine engine) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException("Engine must be a Panama engine instance");
    }

    final PanamaEngine panamaEngine = (PanamaEngine) engine;
    final Arena arena = Arena.ofConfined();

    try {
      final MemorySegment engineHandle = panamaEngine.getHandle();
      final MemorySegment linkerHandle = (MemorySegment) CREATE_LINKER.invokeExact(engineHandle);

      if (linkerHandle.equals(MemorySegment.NULL)) {
        arena.close();
        throw new WasmException("Failed to create native linker");
      }

      return new PanamaLinker(linkerHandle, arena, engine);
    } catch (final WasmException e) {
      arena.close();
      throw e;
    } catch (final Throwable e) {
      arena.close();
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    PanamaValidation.requireNonBlank(moduleName, "moduleName");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(functionType, "functionType");
    PanamaValidation.requireNonNull(implementation, "implementation");
    ensureNotClosed();

    try {
      // Convert strings to native memory
      final MemorySegment moduleNameSegment = memoryManager.allocateString(moduleName);
      final MemorySegment functionNameSegment = memoryManager.allocateString(name);

      // Convert function type to native representation
      final int[] paramTypes = convertToNativeTypes(functionType.getParamTypes());
      final int[] returnTypes = convertToNativeTypes(functionType.getReturnTypes());

      final MemorySegment paramTypesSegment = memoryManager.allocateIntArray(paramTypes);
      final MemorySegment returnTypesSegment = memoryManager.allocateIntArray(returnTypes);

      // Create host function wrapper
      final MemorySegment hostFunctionHandle = createHostFunction(implementation, functionType);

      final boolean success = (boolean) DEFINE_HOST_FUNCTION.invokeExact(
          linkerHandle,
          moduleNameSegment,
          functionNameSegment,
          paramTypesSegment,
          paramTypes.length,
          returnTypesSegment,
          returnTypes.length,
          hostFunctionHandle
      );

      if (!success) {
        throw new WasmException("Failed to define host function: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined host function " + moduleName + "::" + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void defineMemory(final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    PanamaValidation.requireNonBlank(moduleName, "moduleName");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(memory, "memory");
    ensureNotClosed();

    try {
      if (!(memory instanceof PanamaMemory)) {
        throw new IllegalArgumentException("Memory must be a Panama memory instance");
      }

      final PanamaMemory panamaMemory = (PanamaMemory) memory;
      final MemorySegment moduleNameSegment = memoryManager.allocateString(moduleName);
      final MemorySegment memoryNameSegment = memoryManager.allocateString(name);
      final MemorySegment memoryHandle = panamaMemory.getHandle();

      final boolean success = (boolean) DEFINE_MEMORY.invokeExact(
          linkerHandle,
          moduleNameSegment,
          memoryNameSegment,
          memoryHandle
      );

      if (!success) {
        throw new WasmException("Failed to define memory: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined memory " + moduleName + "::" + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void defineTable(final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    PanamaValidation.requireNonBlank(moduleName, "moduleName");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(table, "table");
    ensureNotClosed();

    try {
      if (!(table instanceof PanamaTable)) {
        throw new IllegalArgumentException("Table must be a Panama table instance");
      }

      final PanamaTable panamaTable = (PanamaTable) table;
      final MemorySegment moduleNameSegment = memoryManager.allocateString(moduleName);
      final MemorySegment tableNameSegment = memoryManager.allocateString(name);
      final MemorySegment tableHandle = panamaTable.getHandle();

      final boolean success = (boolean) DEFINE_TABLE.invokeExact(
          linkerHandle,
          moduleNameSegment,
          tableNameSegment,
          tableHandle
      );

      if (!success) {
        throw new WasmException("Failed to define table: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined table " + moduleName + "::" + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void defineGlobal(final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    PanamaValidation.requireNonBlank(moduleName, "moduleName");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(global, "global");
    ensureNotClosed();

    try {
      if (!(global instanceof PanamaGlobal)) {
        throw new IllegalArgumentException("Global must be a Panama global instance");
      }

      final PanamaGlobal panamaGlobal = (PanamaGlobal) global;
      final MemorySegment moduleNameSegment = memoryManager.allocateString(moduleName);
      final MemorySegment globalNameSegment = memoryManager.allocateString(name);
      final MemorySegment globalHandle = panamaGlobal.getHandle();

      final boolean success = (boolean) DEFINE_GLOBAL.invokeExact(
          linkerHandle,
          moduleNameSegment,
          globalNameSegment,
          globalHandle
      );

      if (!success) {
        throw new WasmException("Failed to define global: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined global " + moduleName + "::" + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void defineInstance(final String moduleName, final Instance instance)
      throws WasmException {
    PanamaValidation.requireNonBlank(moduleName, "moduleName");
    PanamaValidation.requireNonNull(instance, "instance");
    ensureNotClosed();

    try {
      if (!(instance instanceof PanamaInstance)) {
        throw new IllegalArgumentException("Instance must be a Panama instance");
      }

      final PanamaInstance panamaInstance = (PanamaInstance) instance;
      final MemorySegment moduleNameSegment = memoryManager.allocateString(moduleName);
      final MemorySegment instanceHandle = panamaInstance.getHandle();

      final boolean success = (boolean) DEFINE_INSTANCE.invokeExact(
          linkerHandle,
          moduleNameSegment,
          instanceHandle
      );

      if (!success) {
        throw new WasmException("Failed to define instance: " + moduleName);
      }

      LOGGER.fine("Defined instance for module " + moduleName);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void alias(final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException {
    PanamaValidation.requireNonBlank(fromModule, "fromModule");
    PanamaValidation.requireNonBlank(fromName, "fromName");
    PanamaValidation.requireNonBlank(toModule, "toModule");
    PanamaValidation.requireNonBlank(toName, "toName");
    ensureNotClosed();

    try {
      final MemorySegment fromModuleSegment = memoryManager.allocateString(fromModule);
      final MemorySegment fromNameSegment = memoryManager.allocateString(fromName);
      final MemorySegment toModuleSegment = memoryManager.allocateString(toModule);
      final MemorySegment toNameSegment = memoryManager.allocateString(toName);

      final boolean success = (boolean) CREATE_ALIAS.invokeExact(
          linkerHandle,
          fromModuleSegment,
          fromNameSegment,
          toModuleSegment,
          toNameSegment
      );

      if (!success) {
        throw new WasmException("Failed to create alias: " + fromModule + "::" + fromName + " -> " + toModule + "::" + toName);
      }

      LOGGER.fine("Created alias " + fromModule + "::" + fromName + " -> " + toModule + "::" + toName);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    PanamaValidation.requireNonNull(store, "store");
    PanamaValidation.requireNonNull(module, "module");
    ensureNotClosed();

    try {
      if (!(store instanceof PanamaStore)) {
        throw new IllegalArgumentException("Store must be a Panama store instance");
      }
      if (!(module instanceof PanamaModule)) {
        throw new IllegalArgumentException("Module must be a Panama module instance");
      }

      final PanamaStore panamaStore = (PanamaStore) store;
      final PanamaModule panamaModule = (PanamaModule) module;

      final MemorySegment instanceHandle = (MemorySegment) INSTANTIATE.invokeExact(
          linkerHandle,
          panamaStore.getHandle(),
          panamaModule.getHandle()
      );

      if (instanceHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to instantiate module");
      }

      final PanamaInstance instance = PanamaInstance.fromHandle(instanceHandle, module, store, arena);
      LOGGER.fine("Successfully instantiated module");
      return instance;
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
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
  public void define(final String module, final String name, final WasmFunction function)
      throws WasmException {
    PanamaValidation.requireNonBlank(module, "module");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(function, "function");
    ensureNotClosed();

    try {
      if (!(function instanceof PanamaFunction)) {
        throw new IllegalArgumentException("Function must be a Panama function instance");
      }

      final PanamaFunction panamaFunction = (PanamaFunction) function;
      final MemorySegment moduleSegment = memoryManager.allocateString(module);
      final MemorySegment nameSegment = memoryManager.allocateString(name);
      final MemorySegment functionHandle = panamaFunction.getHandle();

      final boolean success = (boolean) DEFINE_FUNCTION.invokeExact(
          linkerHandle,
          moduleSegment,
          nameSegment,
          functionHandle
      );

      if (!success) {
        throw new WasmException("Failed to define function: " + module + "::" + name);
      }

      LOGGER.fine("Defined function " + module + "::" + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void defineHostFunction(final String module, final String name, final HostFunction function)
      throws WasmException {
    PanamaValidation.requireNonBlank(module, "module");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(function, "function");
    ensureNotClosed();

    try {
      final MemorySegment moduleSegment = memoryManager.allocateString(module);
      final MemorySegment nameSegment = memoryManager.allocateString(name);
      final MemorySegment hostFunctionHandle = createHostFunction(function, null);

      final boolean success = (boolean) DEFINE_HOST_FUNCTION_SIMPLE.invokeExact(
          linkerHandle,
          moduleSegment,
          nameSegment,
          hostFunctionHandle
      );

      if (!success) {
        throw new WasmException("Failed to define host function: " + module + "::" + name);
      }

      LOGGER.fine("Defined host function " + module + "::" + name);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void defineWasi(final WasiConfig config) throws WasmException {
    PanamaValidation.requireNonNull(config, "config");
    ensureNotClosed();

    try {
      // For now, pass NULL as config handle - full WASI config support would be added later
      final boolean success = (boolean) DEFINE_WASI.invokeExact(linkerHandle, MemorySegment.NULL);
      if (!success) {
        throw new WasmException("Failed to define WASI with configuration");
      }

      LOGGER.fine("WASI support defined with configuration");
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<Instance> instantiateAsync(final Store store, final Module module) {
    PanamaValidation.requireNonNull(store, "store");
    PanamaValidation.requireNonNull(module, "module");

    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
      try {
        return instantiate(store, module);
      } catch (final WasmException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void aliasModule(final String name, final Instance instance) throws WasmException {
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(instance, "instance");
    ensureNotClosed();

    try {
      if (!(instance instanceof PanamaInstance)) {
        throw new IllegalArgumentException("Instance must be a Panama instance");
      }

      final PanamaInstance panamaInstance = (PanamaInstance) instance;
      final MemorySegment nameSegment = memoryManager.allocateString(name);
      final MemorySegment instanceHandle = panamaInstance.getHandle();

      final boolean success = (boolean) ALIAS_MODULE.invokeExact(
          linkerHandle,
          nameSegment,
          instanceHandle
      );

      if (!success) {
        throw new WasmException("Failed to alias module: " + name);
      }

      LOGGER.fine("Aliased module as '" + name + "'");
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public void enableWasi() throws WasmException {
    ensureNotClosed();

    try {
      final boolean success = (boolean) ENABLE_WASI.invokeExact(linkerHandle);
      if (!success) {
        throw new WasmException("Failed to enable WASI");
      }

      LOGGER.fine("WASI support enabled");
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw PanamaExceptionMapper.mapException(e);
    }
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !closed.get() && !linkerHandle.equals(MemorySegment.NULL);
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        DESTROY_LINKER.invokeExact(linkerHandle);
        arena.close();
        LOGGER.fine("Closed Panama linker");
      } catch (final Throwable e) {
        LOGGER.warning("Error during linker cleanup: " + e.getMessage());
      }
    }
  }

  /**
   * Gets the native linker handle.
   *
   * @return the memory segment representing the native handle
   * @throws IllegalStateException if the linker is closed
   */
  public MemorySegment getHandle() {
    ensureNotClosed();
    return linkerHandle;
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

  /**
   * Creates a native host function wrapper.
   *
   * @param implementation the host function implementation
   * @param functionType the function type
   * @return memory segment representing the native host function
   */
  private MemorySegment createHostFunction(final HostFunction implementation, final FunctionType functionType) {
    // This would create a callback that can be called from native code
    // For now, return a placeholder
    return MemorySegment.NULL;
  }
}