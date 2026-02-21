package ai.tegmentum.wasmtime4j.jni;

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
    ensureNotClosed();
    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
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
    ensureUsable();

    try {
      final FunctionType functionType = getFunctionType();

      // Validate parameter types
      JniTypeConverter.validateParameterTypes(params, functionType.getParamTypes());

      // Marshal parameters
      final Object[] nativeParams = JniTypeConverter.wasmValuesToNativeParams(params);

      // Call native function with store context
      final Object[] nativeResults =
          nativeCallMultiValue(getNativeHandle(), store.getNativeHandle(), nativeParams);
      if (nativeResults == null) {
        throw new WasmException("Native function call returned null for '" + name + "'");
      }

      // Convert native results back to WasmValue array
      return JniTypeConverter.nativeResultsToWasmValues(
          nativeResults, functionType.getReturnTypes());
    } catch (final IllegalArgumentException e) {
      throw new WasmException("Parameter validation failed for function '" + name + "'", e);
    } catch (final RuntimeException e) {
      throw new WasmException("Native function call failed for '" + name + "'", e);
    } catch (final Exception e) {
      throw new WasmException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with no parameters.
   *
   * @return the return values
   * @throws WasmException if function execution fails
   */
  public WasmValue[] call() throws WasmException {
    return call(new WasmValue[0]);
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
   * @param params the parameters to pass to the function
   * @return a CompletableFuture containing the results
   */
  public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return call(params);
          } catch (final WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * Calls this function with integer parameters (optimized path).
   *
   * @param parameters the integer parameters
   * @return the return value as an integer (0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public int callInt(final int... parameters) {
    Validation.requireNonNull(parameters, "parameters");
    ensureUsable();

    try {
      return nativeCallInt(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with long parameters (optimized path).
   *
   * @param parameters the long parameters
   * @return the return value as a long (0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public long callLong(final long... parameters) {
    Validation.requireNonNull(parameters, "parameters");
    ensureUsable();

    try {
      return nativeCallLong(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with float parameters (optimized path).
   *
   * @param parameters the float parameters
   * @return the return value as a float (0.0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public float callFloat(final float... parameters) {
    Validation.requireNonNull(parameters, "parameters");
    ensureUsable();

    try {
      return nativeCallFloat(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with double parameters (optimized path).
   *
   * @param parameters the double parameters
   * @return the return value as a double (0.0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public double callDouble(final double... parameters) {
    Validation.requireNonNull(parameters, "parameters");
    ensureUsable();

    try {
      return nativeCallDouble(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
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
   * Calls a function with integer parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the integer parameters
   * @return the return value as an integer
   */
  private static native int nativeCallInt(long functionHandle, int[] parameters);

  /**
   * Calls a function with long parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the long parameters
   * @return the return value as a long
   */
  private static native long nativeCallLong(long functionHandle, long[] parameters);

  /**
   * Calls a function with float parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the float parameters
   * @return the return value as a float
   */
  private static native float nativeCallFloat(long functionHandle, float[] parameters);

  /**
   * Calls a function with double parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the double parameters
   * @return the return value as a double
   */
  private static native double nativeCallDouble(long functionHandle, double[] parameters);

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
  private static native long nativeFuncFromRaw(long storeHandle, long raw);
}
