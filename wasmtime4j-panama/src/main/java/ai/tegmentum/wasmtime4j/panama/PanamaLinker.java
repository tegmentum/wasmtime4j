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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
    // Initialize method handles using the NativeFunctionBindings system
    try {
      final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();

      CREATE_LINKER = bindings.getFunction("wasmtime4j_linker_create",
          FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_FUNCTION = bindings.getFunction("wasmtime4j_linker_define_function",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_HOST_FUNCTION = bindings.getFunction("wasmtime4j_linker_define_host_function",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, // linker
              ValueLayout.ADDRESS, // module_name
              ValueLayout.ADDRESS, // function_name
              ValueLayout.ADDRESS, // param_types
              ValueLayout.JAVA_INT, // param_count
              ValueLayout.ADDRESS, // return_types
              ValueLayout.JAVA_INT, // return_count
              ValueLayout.ADDRESS)); // host_function
      DEFINE_HOST_FUNCTION_SIMPLE = bindings.getFunction("wasmtime4j_linker_define_host_function_simple",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_MEMORY = bindings.getFunction("wasmtime4j_linker_define_memory",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_TABLE = bindings.getFunction("wasmtime4j_linker_define_table",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_GLOBAL = bindings.getFunction("wasmtime4j_linker_define_global",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DEFINE_INSTANCE = bindings.getFunction("wasmtime4j_linker_define_instance",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      CREATE_ALIAS = bindings.getFunction("wasmtime4j_linker_alias",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      ALIAS_MODULE = bindings.getFunction("wasmtime4j_linker_alias_module",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      INSTANTIATE = bindings.getFunction("wasmtime4j_linker_instantiate",
          FunctionDescriptor.of(ValueLayout.ADDRESS,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      ENABLE_WASI = bindings.getFunction("wasmtime4j_linker_enable_wasi",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS));
      DEFINE_WASI = bindings.getFunction("wasmtime4j_linker_define_wasi",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
      DESTROY_LINKER = bindings.getFunction("wasmtime4j_linker_destroy",
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
    // Use shared arena for linker lifetime - this lives as long as the linker instance
    final Arena arena = Arena.ofShared();

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

    // Use confined arena for temporary allocations during this method call
    try (Arena callArena = Arena.ofConfined()) {
      // Convert strings to native memory using confined arena
      final MemorySegment moduleNameSegment = callArena.allocateUtf8String(moduleName);
      final MemorySegment functionNameSegment = callArena.allocateUtf8String(name);

      // Convert function type to native representation
      final int[] paramTypes = convertToNativeTypes(functionType.getParamTypes());
      final int[] returnTypes = convertToNativeTypes(functionType.getReturnTypes());

      // Allocate type arrays in confined arena
      final MemorySegment paramTypesSegment = allocateIntArray(callArena, paramTypes);
      final MemorySegment returnTypesSegment = allocateIntArray(callArena, returnTypes);

      // Create host function wrapper (this uses the linker's shared arena)
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
  public void defineHostFunction(final String module, final String name, final HostFunction function)
      throws WasmException {
    PanamaValidation.requireNonBlank(module, "module");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(function, "function");
    ensureNotClosed();

    // Use confined arena for temporary string allocations
    try (Arena callArena = Arena.ofConfined()) {
      final MemorySegment moduleSegment = callArena.allocateUtf8String(module);
      final MemorySegment nameSegment = callArena.allocateUtf8String(name);
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
  public void defineMemory(final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    PanamaValidation.requireNonBlank(moduleName, "moduleName");
    PanamaValidation.requireNonBlank(name, "name");
    PanamaValidation.requireNonNull(memory, "memory");
    ensureNotClosed();

    // Use confined arena for temporary string allocations
    try (Arena callArena = Arena.ofConfined()) {
      if (!(memory instanceof PanamaMemory)) {
        throw new IllegalArgumentException("Memory must be a Panama memory instance");
      }

      final PanamaMemory panamaMemory = (PanamaMemory) memory;
      final MemorySegment moduleNameSegment = callArena.allocateUtf8String(moduleName);
      final MemorySegment memoryNameSegment = callArena.allocateUtf8String(name);
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
        throw new WasmException("Failed to create alias: " + fromModule + "::" + fromName
            + " -> " + toModule + "::" + toName);
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
   * Allocates an int array in the specified arena.
   *
   * @param arena the arena to allocate in
   * @param values the int values to store
   * @return memory segment containing the int array
   */
  private static MemorySegment allocateIntArray(final Arena arena, final int[] values) {
    if (values.length == 0) {
      return MemorySegment.NULL;
    }

    final MemorySegment segment = arena.allocateArray(ValueLayout.JAVA_INT, values.length);
    for (int i = 0; i < values.length; i++) {
      segment.setAtIndex(ValueLayout.JAVA_INT, i, values[i]);
    }
    return segment;
  }

  /** Native linker for creating upcall stubs. */
  private static final Linker NATIVE_LINKER = Linker.nativeLinker();

  /** Function descriptor for host function callbacks. */
  private static final FunctionDescriptor HOST_FUNCTION_DESCRIPTOR = FunctionDescriptor.of(
      ValueLayout.JAVA_INT, // return code (0 = success, -1 = error)
      ValueLayout.ADDRESS, // caller context
      ValueLayout.ADDRESS, // parameter values array
      ValueLayout.JAVA_INT, // parameter count
      ValueLayout.ADDRESS, // result values array
      ValueLayout.JAVA_INT, // result count
      ValueLayout.ADDRESS  // user data (host function reference)
  );

  /**
   * Creates a native host function wrapper using Panama upcall stubs.
   *
   * @param implementation the host function implementation
   * @param functionType the function type (can be null for simple host functions)
   * @return memory segment representing the native host function upcall stub
   */
  private MemorySegment createHostFunction(final HostFunction implementation, final FunctionType functionType) {
    try {
      // Create a method handle for our callback
      final MethodHandle callbackHandle = MethodHandles.lookup()
          .findStatic(PanamaLinker.class, "hostFunctionCallback",
              MethodType.methodType(int.class,
                  MemorySegment.class, // caller
                  MemorySegment.class, // params
                  int.class,           // param_count
                  MemorySegment.class, // results
                  int.class,           // result_count
                  MemorySegment.class  // user_data
              ));

      // Store the host function implementation in a way we can retrieve it
      final long hostFunctionId = PanamaHostFunctionRegistry.register(implementation, functionType);
      final MemorySegment userDataSegment = arena.allocate(ValueLayout.JAVA_LONG);
      userDataSegment.set(ValueLayout.JAVA_LONG, 0, hostFunctionId);

      // Bind the user data to the callback
      final MethodHandle boundCallback = callbackHandle.bindTo(userDataSegment);

      // Create the upcall stub
      final MemorySegment upcallStub = NATIVE_LINKER.upcallStub(
          boundCallback,
          HOST_FUNCTION_DESCRIPTOR,
          arena
      );

      LOGGER.fine("Created host function upcall stub for implementation: " + implementation.getClass().getSimpleName());
      return upcallStub;
    } catch (final Exception e) {
      LOGGER.severe("Failed to create host function upcall stub: " + e.getMessage());
      throw new RuntimeException("Failed to create host function wrapper", e);
    }
  }

  /**
   * Static callback method invoked from native code when a host function is called.
   * This method handles the conversion between native parameters and Java objects.
   *
   * @param caller the caller context (currently unused)
   * @param params pointer to array of parameter values
   * @param paramCount number of parameters
   * @param results pointer to array for result values
   * @param resultCount number of expected results
   * @param userData pointer to user data containing the host function ID
   * @return 0 on success, -1 on error
   */
  @SuppressWarnings("unused") // Called from native code via upcall
  public static int hostFunctionCallback(
      final MemorySegment caller,
      final MemorySegment params,
      final int paramCount,
      final MemorySegment results,
      final int resultCount,
      final MemorySegment userData) {
    try {
      // Defensive validation of input parameters
      if (userData == null || userData.equals(MemorySegment.NULL)) {
        LOGGER.severe("Host function callback invoked with null user data");
        return -1;
      }

      if (paramCount < 0 || resultCount < 0) {
        LOGGER.severe("Host function callback invoked with negative count parameters: "
            + "paramCount=" + paramCount + ", resultCount=" + resultCount);
        return -1;
      }

      if (paramCount > 0 && (params == null || params.equals(MemorySegment.NULL))) {
        LOGGER.severe("Host function callback invoked with null params but paramCount=" + paramCount);
        return -1;
      }

      if (resultCount > 0 && (results == null || results.equals(MemorySegment.NULL))) {
        LOGGER.severe("Host function callback invoked with null results but resultCount=" + resultCount);
        return -1;
      }

      // Retrieve the host function ID from user data
      final long hostFunctionId = userData.get(ValueLayout.JAVA_LONG, 0);

      // Get the registered host function and its type
      final PanamaHostFunctionRegistry.HostFunctionEntry entry =
          PanamaHostFunctionRegistry.get(hostFunctionId);

      if (entry == null) {
        LOGGER.severe("Host function callback invoked with invalid ID: " + hostFunctionId);
        return -1; // Error
      }

      final HostFunction hostFunction = entry.getImplementation();
      final FunctionType functionType = entry.getFunctionType();

      if (hostFunction == null) {
        LOGGER.severe("Host function implementation is null for ID: " + hostFunctionId);
        return -1;
      }

      // Convert native parameters to Java objects
      final Object[] javaParams = convertNativeParamsToJava(params, paramCount, functionType);

      // Invoke the host function with defensive error handling
      final Object[] javaResults;
      try {
        javaResults = hostFunction.call(javaParams);
      } catch (final Exception e) {
        LOGGER.severe("Host function implementation threw exception: " + e.getMessage());
        // Don't propagate the exception to native code, return error code instead
        return -1;
      }

      // Convert Java results back to native format
      convertJavaResultsToNative(javaResults, results, resultCount, functionType);

      return 0; // Success
    } catch (final Throwable e) {
      // Catch all throwables to prevent any exceptions from propagating to native code
      LOGGER.severe("Critical error in host function callback: " + e.getClass().getSimpleName()
          + ": " + e.getMessage());
      return -1; // Error
    }
  }

  /**
   * Converts native parameter values to Java objects.
   *
   * @param params pointer to native parameter array
   * @param paramCount number of parameters
   * @param functionType the function type for parameter conversion
   * @return array of Java objects representing the parameters
   * @throws IllegalArgumentException if conversion fails
   * @throws IndexOutOfBoundsException if memory access is invalid
   */
  private static Object[] convertNativeParamsToJava(
      final MemorySegment params,
      final int paramCount,
      final FunctionType functionType) {

    if (paramCount == 0) {
      return new Object[0];
    }

    if (paramCount < 0) {
      throw new IllegalArgumentException("Parameter count cannot be negative: " + paramCount);
    }

    final Object[] javaParams = new Object[paramCount];

    try {
      // If we have function type information, use it for precise conversion
      if (functionType != null && functionType.getParamTypes().length == paramCount) {
        final WasmValueType[] paramTypes = functionType.getParamTypes();

        for (int i = 0; i < paramCount; i++) {
          final long offset = i * 8L; // Assuming 8-byte values
          try {
            javaParams[i] = convertNativeValueToJava(params, offset, paramTypes[i]);
          } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to convert parameter " + i
                + " of type " + paramTypes[i] + ": " + e.getMessage(), e);
          }
        }
      } else {
        // Fallback to generic conversion (assume i32 for simplicity)
        for (int i = 0; i < paramCount; i++) {
          final long offset = i * 8L;
          try {
            javaParams[i] = params.get(ValueLayout.JAVA_INT, offset);
          } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to convert parameter " + i
                + " as I32: " + e.getMessage(), e);
          }
        }
      }
    } catch (final IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException("Memory access out of bounds during parameter conversion: "
          + e.getMessage());
    }

    return javaParams;
  }

  /**
   * Converts a single native value to a Java object based on the WebAssembly type.
   *
   * @param segment the memory segment containing the value
   * @param offset the offset within the segment
   * @param valueType the WebAssembly value type
   * @return the Java object representation
   */
  private static Object convertNativeValueToJava(
      final MemorySegment segment,
      final long offset,
      final WasmValueType valueType) {

    switch (valueType) {
      case I32:
        return segment.get(ValueLayout.JAVA_INT, offset);
      case I64:
        return segment.get(ValueLayout.JAVA_LONG, offset);
      case F32:
        return segment.get(ValueLayout.JAVA_FLOAT, offset);
      case F64:
        return segment.get(ValueLayout.JAVA_DOUBLE, offset);
      case V128:
        // For V128, we'll return a byte array representing the 128-bit value
        final byte[] v128Value = new byte[16];
        for (int i = 0; i < 16; i++) {
          v128Value[i] = segment.get(ValueLayout.JAVA_BYTE, offset + i);
        }
        return v128Value;
      case FUNCREF:
      case EXTERNREF:
        // For references, return the pointer value as a long
        return segment.get(ValueLayout.JAVA_LONG, offset);
      default:
        throw new IllegalArgumentException("Unsupported WebAssembly value type: " + valueType);
    }
  }

  /**
   * Converts Java result objects back to native format.
   *
   * @param javaResults array of Java result objects
   * @param results pointer to native results array
   * @param resultCount expected number of results
   * @param functionType the function type for result conversion
   * @throws IllegalArgumentException if conversion fails
   * @throws IndexOutOfBoundsException if memory access is invalid
   */
  private static void convertJavaResultsToNative(
      final Object[] javaResults,
      final MemorySegment results,
      final int resultCount,
      final FunctionType functionType) {

    if (resultCount == 0 || javaResults == null) {
      return;
    }

    if (resultCount < 0) {
      throw new IllegalArgumentException("Result count cannot be negative: " + resultCount);
    }

    final int actualResults = Math.min(javaResults.length, resultCount);

    if (javaResults.length < resultCount) {
      LOGGER.warning("Host function returned " + javaResults.length
          + " results but " + resultCount + " were expected");
    }

    try {
      // If we have function type information, use it for precise conversion
      if (functionType != null && functionType.getReturnTypes().length >= actualResults) {
        final WasmValueType[] returnTypes = functionType.getReturnTypes();

        for (int i = 0; i < actualResults; i++) {
          final long offset = i * 8L; // Assuming 8-byte values
          try {
            convertJavaValueToNative(javaResults[i], results, offset, returnTypes[i]);
          } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to convert result " + i
                + " of type " + returnTypes[i] + ": " + e.getMessage(), e);
          }
        }
      } else {
        // Fallback to generic conversion
        for (int i = 0; i < actualResults; i++) {
          final long offset = i * 8L;
          try {
            convertJavaValueToNativeGeneric(javaResults[i], results, offset);
          } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to convert result " + i
                + " generically: " + e.getMessage(), e);
          }
        }
      }
    } catch (final IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException("Memory access out of bounds during result conversion: "
          + e.getMessage());
    }
  }

  /**
   * Converts a single Java value to native format based on the WebAssembly type.
   *
   * @param javaValue the Java value to convert
   * @param segment the memory segment to write to
   * @param offset the offset within the segment
   * @param valueType the WebAssembly value type
   */
  private static void convertJavaValueToNative(
      final Object javaValue,
      final MemorySegment segment,
      final long offset,
      final WasmValueType valueType) {

    if (javaValue == null) {
      // For null values, write zero
      segment.set(ValueLayout.JAVA_LONG, offset, 0L);
      return;
    }

    switch (valueType) {
      case I32:
        if (javaValue instanceof Number) {
          segment.set(ValueLayout.JAVA_INT, offset, ((Number) javaValue).intValue());
        } else {
          throw new IllegalArgumentException("Expected Number for I32, got: " + javaValue.getClass());
        }
        break;
      case I64:
        if (javaValue instanceof Number) {
          segment.set(ValueLayout.JAVA_LONG, offset, ((Number) javaValue).longValue());
        } else {
          throw new IllegalArgumentException("Expected Number for I64, got: " + javaValue.getClass());
        }
        break;
      case F32:
        if (javaValue instanceof Number) {
          segment.set(ValueLayout.JAVA_FLOAT, offset, ((Number) javaValue).floatValue());
        } else {
          throw new IllegalArgumentException("Expected Number for F32, got: " + javaValue.getClass());
        }
        break;
      case F64:
        if (javaValue instanceof Number) {
          segment.set(ValueLayout.JAVA_DOUBLE, offset, ((Number) javaValue).doubleValue());
        } else {
          throw new IllegalArgumentException("Expected Number for F64, got: " + javaValue.getClass());
        }
        break;
      case V128:
        if (javaValue instanceof byte[]) {
          final byte[] bytes = (byte[]) javaValue;
          for (int i = 0; i < Math.min(16, bytes.length); i++) {
            segment.set(ValueLayout.JAVA_BYTE, offset + i, bytes[i]);
          }
        } else {
          throw new IllegalArgumentException("Expected byte[] for V128, got: " + javaValue.getClass());
        }
        break;
      case FUNCREF:
      case EXTERNREF:
        if (javaValue instanceof Number) {
          segment.set(ValueLayout.JAVA_LONG, offset, ((Number) javaValue).longValue());
        } else {
          throw new IllegalArgumentException("Expected Number for reference type, got: " + javaValue.getClass());
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported WebAssembly value type: " + valueType);
    }
  }

  /**
   * Generic conversion from Java value to native format when type information is not available.
   *
   * @param javaValue the Java value to convert
   * @param segment the memory segment to write to
   * @param offset the offset within the segment
   */
  private static void convertJavaValueToNativeGeneric(
      final Object javaValue,
      final MemorySegment segment,
      final long offset) {

    if (javaValue == null) {
      segment.set(ValueLayout.JAVA_LONG, offset, 0L);
    } else if (javaValue instanceof Integer) {
      segment.set(ValueLayout.JAVA_INT, offset, (Integer) javaValue);
    } else if (javaValue instanceof Long) {
      segment.set(ValueLayout.JAVA_LONG, offset, (Long) javaValue);
    } else if (javaValue instanceof Float) {
      segment.set(ValueLayout.JAVA_FLOAT, offset, (Float) javaValue);
    } else if (javaValue instanceof Double) {
      segment.set(ValueLayout.JAVA_DOUBLE, offset, (Double) javaValue);
    } else if (javaValue instanceof Number) {
      // Fallback for other number types - convert to long
      segment.set(ValueLayout.JAVA_LONG, offset, ((Number) javaValue).longValue());
    } else {
      throw new IllegalArgumentException("Cannot convert Java value to native: " + javaValue.getClass());
    }
  }
}