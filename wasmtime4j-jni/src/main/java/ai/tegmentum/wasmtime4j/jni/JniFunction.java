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

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.TypedFunc;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniTypeConverter;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasmFunction interface.
 *
 * <p>This class provides comprehensive access to WebAssembly functions through JNI calls to the
 * native Wasmtime library. It supports the complete WebAssembly type system including basic types
 * (i32, i64, f32, f64), SIMD types (v128), and reference types (funcref, externref).
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Complete WebAssembly type system support
 *   <li>Multi-value parameter and return handling
 *   <li>Type validation and conversion between Java and WebAssembly
 *   <li>Asynchronous execution support with CompletableFuture
 * </ul>
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive type checking for all function operations.
 */
public final class JniFunction extends JniResource
    implements WasmFunction, TypedFunc.TypedFunctionSupport {

  private static final Logger LOGGER = Logger.getLogger(JniFunction.class.getName());

  /** Cached empty params array to avoid allocation for no-arg calls. */
  private static final WasmValue[] EMPTY_PARAMS = new WasmValue[0];

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniFunction: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Function name for debugging and identification. */
  private final String name;

  /** Module handle for thread-local execution. */
  private final long moduleHandle;

  /** Store context required for function calls. */
  private final JniStore store;

  /** Cached function type for performance optimization. */
  private volatile FunctionType cachedFunctionType;

  /** Whether parameter types have been validated at least once for the cached signature. */
  private volatile boolean typeValidated;

  /** Cached return types to avoid cloning on every call. */
  private volatile WasmValueType[] cachedReturnTypes;

  /** Cached fast-path signature for typed native calls. */
  private volatile FastPath fastPath;

  /** Enum of supported typed fast-path signatures. */
  private enum FastPath {
    VOID,
    TO_I32,
    I_I,
    II_I,
    I_V,
    II_V,
    J_J,
    D_D,
    III_I,
    GENERIC
  }

  /**
   * Creates a new JNI function with the given native handle, name, module handle, and store
   * context.
   *
   * @param nativeHandle the native function handle
   * @param name the function name
   * @param moduleHandle the module handle for thread-local execution
   * @param store the store context required for function calls
   * @throws JniResourceException if nativeHandle is invalid or name/store is null
   */
  JniFunction(
      final long nativeHandle, final String name, final long moduleHandle, final JniStore store) {
    super(nativeHandle);
    Validation.requireNonNull(name, "name");
    Validation.requireNonNull(store, "store");
    this.name = name;
    this.moduleHandle = moduleHandle;
    this.store = store;
    LOGGER.fine(
        "Created JNI function '"
            + name
            + "' with handle: 0x"
            + Long.toHexString(nativeHandle)
            + ", module: 0x"
            + Long.toHexString(moduleHandle));
  }

  /**
   * Ensures this function and its owning store are still usable.
   *
   * @throws JniResourceException if this function or its store has been closed
   */
  private void ensureUsable() {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
    } finally {
      endOperation();
    }
  }

  /**
   * Gets the name of this function.
   *
   * @return the function name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the module handle for thread-local execution.
   *
   * <p>This handle is used when executing functions on WebAssembly threads, where each thread
   * creates its own Store and Instance from the Module.
   *
   * @return the native module handle
   */
  public long getModuleHandle() {
    return moduleHandle;
  }

  /**
   * Gets the function type signature.
   *
   * @return the function type
   * @throws JniResourceException if this function is closed
   * @throws WasmException if the function type cannot be retrieved
   */
  @Override
  public FunctionType getFunctionType() {
    // Use cached type if available
    if (cachedFunctionType != null) {
      return cachedFunctionType;
    }

    ensureUsable();
    try {
      final String[] paramTypeStrings = nativeGetParameterTypes(getNativeHandle());
      final String[] returnTypeStrings = nativeGetReturnTypes(getNativeHandle());

      if (paramTypeStrings == null || returnTypeStrings == null) {
        throw new RuntimeException("Failed to retrieve function signature for '" + name + "'");
      }

      final WasmValueType[] paramTypes = JniTypeConverter.stringsToTypes(paramTypeStrings);
      final WasmValueType[] returnTypes = JniTypeConverter.stringsToTypes(returnTypeStrings);

      cachedFunctionType = new FunctionType(paramTypes, returnTypes);
      return cachedFunctionType;
    } catch (final IllegalArgumentException e) {
      throw new RuntimeException("Invalid function signature for '" + name + "'", e);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting function type for '" + name + "'", e);
    }
  }

  /**
   * Calls this function with the given WebAssembly parameters.
   *
   * @param params the parameters to pass to the function
   * @return the results returned by the function
   * @throws WasmException if function execution fails
   */
  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    Validation.requireNonNull(params, "parameters");

    // Hold the read lock across the entire call to prevent use-after-free
    // from concurrent close() between the check and the native call.
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      final FunctionType functionType = getFunctionType();

      // Skip per-call type validation after first successful call with matching param count.
      // The function signature doesn't change, so re-validation is redundant.
      if (!typeValidated || params.length != functionType.getParamCount()) {
        JniTypeConverter.validateParameterTypes(params, functionType.getParamTypes());
        typeValidated = true;
      }

      // Marshal parameters
      final Object[] nativeParams = JniTypeConverter.wasmValuesToNativeParams(params);

      // Call native function with store context
      final Object[] nativeResults =
          nativeCallMultiValue(getNativeHandle(), store.getNativeHandle(), nativeParams);
      if (nativeResults == null) {
        throw new WasmException("Native function call returned null for '" + name + "'");
      }

      // Convert native results back to WasmValue array using cached return types
      return JniTypeConverter.nativeResultsToWasmValues(
          nativeResults, getReturnTypesInternal());
    } catch (final IllegalArgumentException e) {
      throw new WasmException("Parameter validation failed for function '" + name + "'", e);
    } catch (final RuntimeException e) {
      throw new WasmException("Native function call failed for '" + name + "'", e);
    } catch (final Exception e) {
      throw new WasmException("Unexpected error calling function '" + name + "'", e);
    } finally {
      endOperation();
    }
  }

  /**
   * Calls this function with no parameters.
   *
   * @return the return values
   * @throws WasmException if function execution fails
   */
  public WasmValue[] call() throws WasmException {
    return call(EMPTY_PARAMS);
  }

  /**
   * Legacy call method for backward compatibility.
   *
   * @param parameters the function parameters
   * @return the return value (null if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   * @deprecated Use {@link #call(WasmValue...)} instead
   */
  @Deprecated
  public Object call(final Object... parameters) {
    Validation.requireNonNull(parameters, "parameters");
    ensureUsable();

    try {
      return nativeCall(getNativeHandle(), store.getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function asynchronously with the given parameters.
   *
   * <p>When the store has async support enabled, this uses Wasmtime's native {@code
   * Func::call_async} which enables proper async host function interleaving. For non-async stores,
   * the synchronous {@link #call(WasmValue...)} is run on the ForkJoinPool.
   *
   * @param params the parameters to pass to the function
   * @return a CompletableFuture containing the results
   */
  public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
    final boolean useNativeAsync = store != null && store.isAsync();
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            if (useNativeAsync) {
              return callNativeAsync(params);
            }
            return call(params);
          } catch (final WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * Internal method that calls the native async function binding.
   *
   * @param params the parameters to pass to the function
   * @return the results returned by the function
   * @throws WasmException if function execution fails
   */
  private WasmValue[] callNativeAsync(final WasmValue... params) throws WasmException {
    Validation.requireNonNull(params, "parameters");
    ensureUsable();

    try {
      final FunctionType functionType = getFunctionType();
      JniTypeConverter.validateParameterTypes(params, functionType.getParamTypes());
      final Object[] nativeParams = JniTypeConverter.wasmValuesToNativeParams(params);

      final Object[] nativeResults =
          nativeCallAsync(getNativeHandle(), store.getNativeHandle(), nativeParams);
      if (nativeResults == null) {
        throw new WasmException("Native async function call returned null for '" + name + "'");
      }

      return JniTypeConverter.nativeResultsToWasmValues(
          nativeResults, getReturnTypesInternal());
    } catch (final IllegalArgumentException e) {
      throw new WasmException("Parameter validation failed for async function '" + name + "'", e);
    } catch (final RuntimeException e) {
      throw new WasmException("Native async function call failed for '" + name + "'", e);
    } catch (final Exception e) {
      throw new WasmException("Unexpected error in async function call '" + name + "'", e);
    }
  }

  /**
   * Creates a typed function wrapper with the specified signature.
   *
   * <p>This method provides zero-cost typed function calls by eliminating runtime type checking
   * overhead. The signature string encodes parameter and return types in a compact format.
   *
   * @param signature the signature string (e.g., "ii->i" for (i32, i32) -> i32)
   * @return a new TypedFunc instance wrapping this function
   * @throws IllegalArgumentException if signature is invalid
   */
  @Override
  public TypedFunc asTyped(final String signature) {
    return new JniTypedFunc(this.store, this, signature);
  }

  @Override
  public long toRawFuncRef() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureUsable();
    if (store == null) {
      throw new IllegalStateException("Function store reference is null");
    }
    try {
      return nativeFuncToRaw(getNativeHandle(), store.getNativeHandle());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to convert function to raw funcref", e);
    }
  }

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name
   */
  @Override
  public boolean matchesFuncType(final Store store, final FunctionType funcType)
      throws WasmException {
    beginOperation();
    try {
      if (store == null) {
        throw new IllegalArgumentException("store cannot be null");
      }
      if (funcType == null) {
        throw new IllegalArgumentException("funcType cannot be null");
      }
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("store must be a JniStore");
      }
      final JniStore jniStore = (JniStore) store;

      final WasmValueType[] paramTypes = funcType.getParamTypes();
      final WasmValueType[] resultTypes = funcType.getReturnTypes();

      final int[] paramCodes = new int[paramTypes.length];
      for (int i = 0; i < paramTypes.length; i++) {
        paramCodes[i] = paramTypes[i].ordinal();
      }

      final int[] resultCodes = new int[resultTypes.length];
      for (int i = 0; i < resultTypes.length; i++) {
        resultCodes[i] = resultTypes[i].ordinal();
      }

      final int result =
          nativeFuncMatchesTy(
              getNativeHandle(), jniStore.getNativeHandle(), paramCodes, resultCodes);
      if (result < 0) {
        throw new WasmException("Native func_matches_ty check failed");
      }
      return result == 1;
    } finally {
      endOperation();
    }
  }

  @Override
  protected String getResourceType() {
    return "Function[" + name + "]";
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, Functions are owned by the Store. Destroying a Function while the Store
   * still exists can corrupt the Store's internal slab state ("object used with wrong store"
   * panic). We mark the Function as closed but don't destroy it - the Store will handle cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          String.format(
              "Function '%s' marked as closed. "
                  + "Native resources will be freed when Store is destroyed.",
              name));
    }
    // Note: Do NOT call nativeDestroyFunction here. Functions are Store-owned resources.
    // Destroying them while the Store exists causes "object used with wrong store" panics.
    // The Store will clean up all its Functions when it is destroyed.
  }

  /**
   * Returns cached return types array, populating the cache on first call.
   * Avoids cloning the array from FunctionType on every function call.
   */
  private WasmValueType[] getReturnTypesInternal() {
    WasmValueType[] types = cachedReturnTypes;
    if (types == null) {
      types = getFunctionType().getReturnTypes();
      cachedReturnTypes = types;
    }
    return types;
  }

  // =============================================================================
  // Typed Fast-Path Overrides
  // =============================================================================

  /**
   * Resolves the fast-path enum for the current function type. Called lazily on first fast-path
   * invocation.
   */
  private FastPath resolveFastPath() {
    FastPath fp = this.fastPath;
    if (fp != null) {
      return fp;
    }
    final FunctionType type = getFunctionType();
    final WasmValueType[] params = type.getParamTypes();
    final WasmValueType[] results = type.getReturnTypes();
    fp = classifySignature(params, results);
    this.fastPath = fp;
    return fp;
  }

  @SuppressWarnings("checkstyle:CyclomaticComplexity")
  private static FastPath classifySignature(
      final WasmValueType[] params, final WasmValueType[] results) {
    final int pc = params.length;
    final int rc = results.length;

    if (pc == 0 && rc == 0) {
      return FastPath.VOID;
    }
    if (pc == 0 && rc == 1 && results[0] == WasmValueType.I32) {
      return FastPath.TO_I32;
    }
    if (pc == 1 && params[0] == WasmValueType.I32) {
      if (rc == 1 && results[0] == WasmValueType.I32) {
        return FastPath.I_I;
      }
      if (rc == 0) {
        return FastPath.I_V;
      }
    }
    if (pc == 2 && params[0] == WasmValueType.I32 && params[1] == WasmValueType.I32) {
      if (rc == 1 && results[0] == WasmValueType.I32) {
        return FastPath.II_I;
      }
      if (rc == 0) {
        return FastPath.II_V;
      }
    }
    if (pc == 3
        && params[0] == WasmValueType.I32
        && params[1] == WasmValueType.I32
        && params[2] == WasmValueType.I32
        && rc == 1
        && results[0] == WasmValueType.I32) {
      return FastPath.III_I;
    }
    if (pc == 1 && params[0] == WasmValueType.I64 && rc == 1 && results[0] == WasmValueType.I64) {
      return FastPath.J_J;
    }
    if (pc == 1 && params[0] == WasmValueType.F64 && rc == 1 && results[0] == WasmValueType.F64) {
      return FastPath.D_D;
    }
    return FastPath.GENERIC;
  }

  /** {@inheritDoc} */
  @Override
  public void callVoid() throws WasmException {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      if (resolveFastPath() == FastPath.VOID) {
        nativeCall_V(getNativeHandle(), store.getNativeHandle());
        return;
      }
      // Fall back to default implementation
      WasmFunction.super.callVoid();
    } finally {
      endOperation();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int callToI32() throws WasmException {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      if (resolveFastPath() == FastPath.TO_I32) {
        return nativeCall_I(getNativeHandle(), store.getNativeHandle());
      }
      return WasmFunction.super.callToI32();
    } finally {
      endOperation();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int callI32ToI32(final int arg) throws WasmException {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      if (resolveFastPath() == FastPath.I_I) {
        return nativeCallI_I(getNativeHandle(), store.getNativeHandle(), arg);
      }
      return WasmFunction.super.callI32ToI32(arg);
    } finally {
      endOperation();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int callI32I32ToI32(final int arg1, final int arg2) throws WasmException {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      if (resolveFastPath() == FastPath.II_I) {
        return nativeCallII_I(getNativeHandle(), store.getNativeHandle(), arg1, arg2);
      }
      return WasmFunction.super.callI32I32ToI32(arg1, arg2);
    } finally {
      endOperation();
    }
  }

  /** {@inheritDoc} */
  @Override
  public long callI64ToI64(final long arg) throws WasmException {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      if (resolveFastPath() == FastPath.J_J) {
        return nativeCallJ_J(getNativeHandle(), store.getNativeHandle(), arg);
      }
      return WasmFunction.super.callI64ToI64(arg);
    } finally {
      endOperation();
    }
  }

  /** {@inheritDoc} */
  @Override
  public double callF64ToF64(final double arg) throws WasmException {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      if (resolveFastPath() == FastPath.D_D) {
        return nativeCallD_D(getNativeHandle(), store.getNativeHandle(), arg);
      }
      return WasmFunction.super.callF64ToF64(arg);
    } finally {
      endOperation();
    }
  }

  // Native method declarations

  /**
   * Gets the parameter types for a function.
   *
   * @param functionHandle the native function handle
   * @return array of parameter type names or null on error
   */
  private static native String[] nativeGetParameterTypes(long functionHandle);

  /**
   * Gets the return types for a function.
   *
   * @param functionHandle the native function handle
   * @return array of return type names or null on error
   */
  private static native String[] nativeGetReturnTypes(long functionHandle);

  /**
   * Calls a function with generic parameters.
   *
   * @param functionHandle the native function handle
   * @param storeHandle the native store handle
   * @param parameters the function parameters
   * @return the return value or null
   */
  private static native Object nativeCall(
      long functionHandle, long storeHandle, Object[] parameters);

  /**
   * Calls a function with parameters and returns multiple values.
   *
   * @param functionHandle the native function handle
   * @param storeHandle the native store handle
   * @param parameters the function parameters
   * @return array of return values (never null, may be empty)
   */
  private static native Object[] nativeCallMultiValue(
      long functionHandle, long storeHandle, Object[] parameters);

  /**
   * Converts this function to its raw funcref pointer value.
   *
   * @param functionHandle the native function handle
   * @param storeHandle the native store handle
   * @return the raw funcref value
   */
  static native long nativeFuncToRaw(long functionHandle, long storeHandle);

  /**
   * Reconstructs a function from a raw funcref pointer.
   *
   * @param storeHandle the native store handle
   * @param raw the raw funcref value
   * @return the native function handle, or 0 if invalid
   */
  static native long nativeFuncFromRaw(long storeHandle, long raw);

  /**
   * Calls a function asynchronously using Wasmtime's native async runtime.
   *
   * <p>Uses {@code Func::call_async} via the Tokio runtime, enabling proper async host function
   * interleaving. Requires the store to have been created with async support enabled.
   *
   * @param functionHandle the native function handle
   * @param storeHandle the native store handle
   * @param parameters the function parameters as WasmValue objects
   * @return array of return values (never null, may be empty)
   */
  private static native Object[] nativeCallAsync(
      long functionHandle, long storeHandle, Object[] parameters);

  /**
   * Checks if a function matches a function type using subtype-aware checking.
   *
   * @param functionHandle the native function handle
   * @param storeHandle the native store handle
   * @param paramTypeCodes param type ordinals (WasmValueType.ordinal())
   * @param resultTypeCodes result type ordinals (WasmValueType.ordinal())
   * @return 1 if matches, 0 if not, -1 on error
   */
  private static native int nativeFuncMatchesTy(
      long functionHandle, long storeHandle, int[] paramTypeCodes, int[] resultTypeCodes);

  // Typed fast-path native methods — 1 JNI crossing, 0 heap allocations

  /** () -> void. */
  private static native void nativeCall_V(long functionHandle, long storeHandle);

  /** () -> i32. */
  private static native int nativeCall_I(long functionHandle, long storeHandle);

  /** (i32) -> i32. */
  private static native int nativeCallI_I(long functionHandle, long storeHandle, int arg0);

  /** (i32, i32) -> i32. */
  private static native int nativeCallII_I(
      long functionHandle, long storeHandle, int arg0, int arg1);

  /** (i32) -> void. */
  private static native void nativeCallI_V(long functionHandle, long storeHandle, int arg0);

  /** (i32, i32) -> void. */
  private static native void nativeCallII_V(
      long functionHandle, long storeHandle, int arg0, int arg1);

  /** (i64) -> i64. */
  private static native long nativeCallJ_J(long functionHandle, long storeHandle, long arg0);

  /** (f64) -> f64. */
  private static native double nativeCallD_D(long functionHandle, long storeHandle, double arg0);

  /** (i32, i32, i32) -> i32. */
  private static native int nativeCallIII_I(
      long functionHandle, long storeHandle, int arg0, int arg1, int arg2);
}
