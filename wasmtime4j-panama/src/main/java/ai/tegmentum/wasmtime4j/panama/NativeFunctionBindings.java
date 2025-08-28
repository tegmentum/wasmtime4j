/*
 * Copyright 2024 Tegmentum AI
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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Type-safe wrappers for native function signatures.
 *
 * <p>This class provides type-safe bindings for all Wasmtime native functions, ensuring
 * compile-time validation of function signatures and providing optimized access through cached
 * method handles.
 *
 * <p>All function bindings are lazily initialized and cached for optimal performance on repeated
 * calls.
 */
public final class NativeFunctionBindings {

  private static final Logger LOGGER = Logger.getLogger(NativeFunctionBindings.class.getName());

  // Singleton instance
  private static volatile NativeFunctionBindings instance;
  private static final Object INSTANCE_LOCK = new Object();

  // Core components
  private final NativeLibraryLoader libraryLoader;
  private final MethodHandleCache methodHandleCache;
  private final ConcurrentHashMap<String, FunctionBinding> functionBindings;

  // Status
  private volatile boolean initialized = false;

  /** Private constructor for singleton pattern. */
  private NativeFunctionBindings() {
    this.libraryLoader = NativeLibraryLoader.getInstance();
    this.methodHandleCache = new MethodHandleCache();
    this.functionBindings = new ConcurrentHashMap<>();

    initializeFunctionBindings();
    this.initialized = true;

    LOGGER.fine("Initialized NativeFunctionBindings");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   */
  public static NativeFunctionBindings getInstance() {
    NativeFunctionBindings result = instance;
    if (result == null) {
      synchronized (INSTANCE_LOCK) {
        result = instance;
        if (result == null) {
          instance = result = new NativeFunctionBindings();
        }
      }
    }
    return result;
  }

  /**
   * Checks if the bindings are initialized and available.
   *
   * @return true if initialized, false otherwise
   */
  public boolean isInitialized() {
    return initialized && libraryLoader.isLoaded();
  }

  // Engine Functions

  /**
   * Creates a new Wasmtime engine.
   *
   * @return memory segment pointer to the engine, or null on failure
   */
  public MemorySegment engineCreate() {
    return callNativeFunction("wasmtime4j_engine_create", MemorySegment.class);
  }

  /**
   * Destroys a Wasmtime engine.
   *
   * @param enginePtr pointer to the engine to destroy
   */
  public void engineDestroy(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_engine_destroy", Void.class, enginePtr);
  }

  /**
   * Configures an engine with options.
   *
   * @param enginePtr pointer to the engine
   * @param optionName name of the configuration option
   * @param optionValue value of the configuration option
   * @return 0 on success, negative error code on failure
   */
  public int engineConfigure(
      final MemorySegment enginePtr,
      final MemorySegment optionName,
      final MemorySegment optionValue) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(optionName, "optionName");

    return callNativeFunction(
        "wasmtime4j_engine_configure", Integer.class, enginePtr, optionName, optionValue);
  }

  // Module Functions

  /**
   * Compiles a WebAssembly module.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to the WASM bytecode
   * @param wasmSize size of the WASM bytecode
   * @param modulePtr pointer to store the compiled module
   * @return 0 on success, negative error code on failure
   */
  public int moduleCompile(
      final MemorySegment enginePtr,
      final MemorySegment wasmBytes,
      final long wasmSize,
      final MemorySegment modulePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    validatePointer(modulePtr, "modulePtr");
    validateSize(wasmSize, "wasmSize");

    return callNativeFunction(
        "wasmtime4j_module_compile", Integer.class, enginePtr, wasmBytes, wasmSize, modulePtr);
  }

  /**
   * Destroys a WebAssembly module.
   *
   * @param modulePtr pointer to the module to destroy
   */
  public void moduleDestroy(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    callNativeFunction("wasmtime4j_module_destroy", Void.class, modulePtr);
  }

  // Instance Functions

  /**
   * Creates a WebAssembly instance.
   *
   * @param storePtr pointer to the store
   * @param modulePtr pointer to the module
   * @param instancePtr pointer to store the created instance
   * @return 0 on success, negative error code on failure
   */
  public int instanceCreate(
      final MemorySegment storePtr,
      final MemorySegment modulePtr,
      final MemorySegment instancePtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(modulePtr, "modulePtr");
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_instance_create", Integer.class, storePtr, modulePtr, instancePtr);
  }

  /**
   * Destroys a WebAssembly instance.
   *
   * @param instancePtr pointer to the instance to destroy
   */
  public void instanceDestroy(final MemorySegment instancePtr) {
    validatePointer(instancePtr, "instancePtr");
    callNativeFunction("wasmtime4j_instance_destroy", Void.class, instancePtr);
  }

  /**
   * Gets a cached method handle for a native function.
   *
   * @param functionName the name of the function
   * @return optional containing the method handle, or empty if not found
   */
  public Optional<MethodHandle> getMethodHandle(final String functionName) {
    FunctionBinding binding = functionBindings.get(functionName);
    if (binding == null) {
      LOGGER.warning("Unknown function binding: " + functionName);
      return Optional.empty();
    }

    return binding.getMethodHandle();
  }

  /**
   * Gets the function descriptor for a native function.
   *
   * @param functionName the name of the function
   * @return optional containing the function descriptor, or empty if not found
   */
  public Optional<FunctionDescriptor> getFunctionDescriptor(final String functionName) {
    FunctionBinding binding = functionBindings.get(functionName);
    if (binding == null) {
      return Optional.empty();
    }

    return Optional.of(binding.getDescriptor());
  }

  /**
   * Gets the method handle for table size query.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableSize() {
    return getMethodHandle("wasmtime4j_table_size").orElse(null);
  }

  /**
   * Gets the method handle for table element get.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableGet() {
    return getMethodHandle("wasmtime4j_table_get").orElse(null);
  }

  /**
   * Gets the method handle for table element set.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableSet() {
    return getMethodHandle("wasmtime4j_table_set").orElse(null);
  }

  /**
   * Gets the method handle for table grow.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableGrow() {
    return getMethodHandle("wasmtime4j_table_grow").orElse(null);
  }

  /**
   * Gets the method handle for table deletion.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableDelete() {
    return getMethodHandle("wasmtime4j_table_destroy").orElse(null);
  }

  /**
   * Gets the method handle for creating host functions.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getFuncNewHost() {
    return getMethodHandle("wasmtime4j_func_new_host").orElse(null);
  }

  /**
   * Gets the method handle for creating function types.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getFuncTypeNew() {
    return getMethodHandle("wasmtime4j_functype_new").orElse(null);
  }

  /**
   * Gets the method handle for destroying function types.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getFuncTypeDelete() {
    return getMethodHandle("wasmtime4j_functype_destroy").orElse(null);
  }

  /** Initializes all function bindings. */
  private void initializeFunctionBindings() {
    // Engine functions
    addFunctionBinding("wasmtime4j_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding("wasmtime4j_engine_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_engine_configure",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // option_name
            ValueLayout.ADDRESS)); // option_value

    // Module functions
    addFunctionBinding(
        "wasmtime4j_module_compile",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG, // wasm_size
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding("wasmtime4j_module_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Instance functions
    addFunctionBinding(
        "wasmtime4j_instance_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS)); // instance_ptr

    addFunctionBinding(
        "wasmtime4j_instance_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // Table functions
    addFunctionBinding(
        "wasmtime4j_table_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_table_get",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return element
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.JAVA_LONG)); // index

    addFunctionBinding(
        "wasmtime4j_table_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS)); // element_ptr

    addFunctionBinding(
        "wasmtime4j_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.JAVA_LONG, // delta
            ValueLayout.ADDRESS)); // initial_value

    addFunctionBinding(
        "wasmtime4j_table_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // table_ptr

    // Host function bindings
    addFunctionBinding(
        "wasmtime4j_func_new_host",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return func_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // functype_ptr
            ValueLayout.ADDRESS)); // callback_ptr

    addFunctionBinding(
        "wasmtime4j_functype_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return functype_ptr
            ValueLayout.ADDRESS, // param_types array
            ValueLayout.JAVA_LONG, // param_count
            ValueLayout.ADDRESS, // return_types array
            ValueLayout.JAVA_LONG)); // return_count

    addFunctionBinding(
        "wasmtime4j_functype_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // functype_ptr
  }

  /**
   * Adds a function binding with lazy initialization.
   *
   * @param functionName the name of the function
   * @param descriptor the function descriptor
   */
  private void addFunctionBinding(final String functionName, final FunctionDescriptor descriptor) {
    FunctionBinding binding = new FunctionBinding(functionName, descriptor);
    functionBindings.put(functionName, binding);
    LOGGER.finest("Added function binding: " + functionName);
  }

  /**
   * Calls a native function with type safety.
   *
   * @param functionName the name of the function
   * @param returnType the expected return type
   * @param args the function arguments
   * @return the function result
   * @throws IllegalStateException if the function call fails
   */
  @SuppressWarnings("unchecked")
  private <T> T callNativeFunction(
      final String functionName, final Class<T> returnType, final Object... args) {
    if (!isInitialized()) {
      throw new IllegalStateException("Native function bindings not initialized");
    }

    FunctionBinding binding = functionBindings.get(functionName);
    if (binding == null) {
      throw new IllegalArgumentException("Unknown function: " + functionName);
    }

    Optional<MethodHandle> methodHandle = binding.getMethodHandle();
    if (methodHandle.isEmpty()) {
      throw new IllegalStateException("Failed to get method handle for function: " + functionName);
    }

    try {
      Object result = methodHandle.get().invokeWithArguments(args);

      if (returnType == Void.class) {
        return null;
      } else {
        return returnType.cast(result);
      }
    } catch (Throwable e) {
      LOGGER.warning("Native function call failed: " + functionName + " - " + e.getMessage());
      throw new IllegalStateException("Native function call failed: " + functionName, e);
    }
  }

  /**
   * Validates that a pointer is not null.
   *
   * @param pointer the pointer to validate
   * @param name the name of the parameter for error messages
   * @throws IllegalArgumentException if the pointer is null
   */
  private void validatePointer(final MemorySegment pointer, final String name) {
    if (pointer == null || pointer.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }

  /**
   * Validates that a size is positive.
   *
   * @param size the size to validate
   * @param name the name of the parameter for error messages
   * @throws IllegalArgumentException if the size is not positive
   */
  private void validateSize(final long size, final String name) {
    if (size <= 0) {
      throw new IllegalArgumentException(name + " must be positive: " + size);
    }
  }

  /** Function binding with lazy initialization. */
  private final class FunctionBinding {
    private final String functionName;
    private final FunctionDescriptor descriptor;
    private volatile MethodHandle methodHandle;
    private volatile boolean initialized = false;

    FunctionBinding(final String functionName, final FunctionDescriptor descriptor) {
      this.functionName = functionName;
      this.descriptor = descriptor;
    }

    String getFunctionName() {
      return functionName;
    }

    FunctionDescriptor getDescriptor() {
      return descriptor;
    }

    Optional<MethodHandle> getMethodHandle() {
      if (!initialized) {
        synchronized (this) {
          if (!initialized) {
            initializeMethodHandle();
            initialized = true;
          }
        }
      }

      return Optional.ofNullable(methodHandle);
    }

    private void initializeMethodHandle() {
      Optional<MethodHandle> handle =
          methodHandleCache.getOrCreate(functionName, MemorySegment.NULL, descriptor);

      if (handle.isEmpty()) {
        // Try direct lookup from library loader
        handle = libraryLoader.lookupFunction(functionName, descriptor);
      }

      if (handle.isPresent()) {
        this.methodHandle = handle.get();
        LOGGER.finest("Initialized method handle for function: " + functionName);
      } else {
        LOGGER.warning("Failed to initialize method handle for function: " + functionName);
      }
    }
  }
}
