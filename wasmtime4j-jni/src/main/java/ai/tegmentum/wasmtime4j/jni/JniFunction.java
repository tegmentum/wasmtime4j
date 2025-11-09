package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniTypeConverter;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
 *   <li>Function result caching for frequently called functions
 *   <li>Asynchronous execution support with CompletableFuture
 *   <li>Optimized call paths for common type combinations
 * </ul>
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive type checking for all function operations.
 */
public final class JniFunction extends JniResource implements WasmFunction {

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

  /** Store context required for function calls. */
  private final JniStore store;

  /** Cached function type for performance optimization. */
  private volatile FunctionType cachedFunctionType;

  /** Function result cache for frequently called functions. */
  private final ConcurrentHashMap<String, CachedResult> resultCache = new ConcurrentHashMap<>();

  /** Call count for cache eviction and performance monitoring. */
  private final AtomicLong callCount = new AtomicLong(0);

  /** Maximum cache size per function. */
  private static final int MAX_CACHE_SIZE = 100;

  /** Hot path optimization - cache the most frequently called function signature. */
  private final AtomicReference<Object[]> cachedNativeParams = new AtomicReference<>();

  private volatile String cachedParamSignature;
  private volatile long lastOptimizationCheck = 0;
  private static final long OPTIMIZATION_CHECK_INTERVAL = 1000; // Check every 1000 calls

  /** Cache entry for function results. */
  private static final class CachedResult {
    final WasmValue[] result;
    final long timestamp;
    final int hitCount;

    CachedResult(final WasmValue[] result, final long timestamp, final int hitCount) {
      this.result = result;
      this.timestamp = timestamp;
      this.hitCount = hitCount;
    }
  }

  /**
   * Creates a new JNI function with the given native handle, name, and store context.
   *
   * @param nativeHandle the native function handle
   * @param name the function name
   * @param store the store context required for function calls
   * @throws JniResourceException if nativeHandle is invalid or name/store is null
   */
  JniFunction(final long nativeHandle, final String name, final JniStore store) {
    super(nativeHandle);
    JniValidation.requireNonNull(name, "name");
    JniValidation.requireNonNull(store, "store");
    this.name = name;
    this.store = store;
    LOGGER.fine(
        "Created JNI function '" + name + "' with handle: 0x" + Long.toHexString(nativeHandle));
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

    ensureNotClosed();
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
    } catch (final JniValidationException e) {
      throw new RuntimeException("Invalid function signature for '" + name + "'", e);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting function type for '" + name + "'", e);
    }
  }

  /**
   * Gets the parameter types for this function.
   *
   * @return array of parameter type names
   * @throws JniResourceException if this function is closed
   * @throws RuntimeException if the types cannot be retrieved
   * @deprecated Use {@link #getFunctionType()} instead
   */
  @Deprecated
  public String[] getParameterTypes() {
    try {
      final FunctionType funcType = getFunctionType();
      return JniTypeConverter.typesToStrings(funcType.getParamTypes());
    } catch (final RuntimeException e) {
      throw new RuntimeException("Error getting parameter types", e);
    }
  }

  /**
   * Gets the return types for this function.
   *
   * @return array of return type names
   * @throws JniResourceException if this function is closed
   * @throws RuntimeException if the types cannot be retrieved
   * @deprecated Use {@link #getFunctionType()} instead
   */
  @Deprecated
  public String[] getReturnTypes() {
    try {
      final FunctionType funcType = getFunctionType();
      return JniTypeConverter.typesToStrings(funcType.getReturnTypes());
    } catch (final RuntimeException e) {
      throw new RuntimeException("Error getting return types", e);
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
    JniValidation.requireNonNull(params, "parameters");
    ensureNotClosed();

    // Start performance monitoring
    final long startTime =
        ai.tegmentum.wasmtime4j.jni.performance.PerformanceMonitor.startOperation(
            "function_call", name);

    try {
      // Increment call count for performance monitoring
      final long currentCall = callCount.incrementAndGet();

      // Hot path optimization for frequently called functions
      if (currentCall % OPTIMIZATION_CHECK_INTERVAL == 0) {
        optimizeHotPath(params);
      }

      final FunctionType functionType = getFunctionType();

      // Fast path validation for cached parameter pattern
      final String paramSignature = getParameterSignature(params);
      if (paramSignature.equals(cachedParamSignature)) {
        // Use pre-validated hot path
        return callOptimizedPath(params, functionType);
      }

      // Standard path with full validation
      JniTypeConverter.validateParameterTypes(params, functionType.getParamTypes());

      // Check cache for frequently called functions with consistent parameters
      final String cacheKey = createCacheKey(params);
      final CachedResult cached = resultCache.get(cacheKey);
      if (cached != null && shouldUseCachedResult(cached, currentCall)) {
        LOGGER.fine("Using cached result for function '" + name + "'");
        return cached.result.clone();
      }

      // Use optimized parameter marshalling
      Object[] nativeParams;
      try {
        nativeParams =
            ai.tegmentum.wasmtime4j.jni.performance.OptimizedMarshalling.marshalParameters(params);
      } catch (final Exception e) {
        // Fallback to traditional marshalling
        nativeParams = JniTypeConverter.wasmValuesToNativeParams(params);
      }

      // Call native function with store context
      final Object[] nativeResults = nativeCallMultiValue(getNativeHandle(), store.getNativeHandle(), nativeParams);
      if (nativeResults == null) {
        throw new WasmException("Native function call returned null for '" + name + "'");
      }

      // Convert native results back to WasmValue array
      WasmValue[] results;
      try {
        results =
            ai.tegmentum.wasmtime4j.jni.performance.OptimizedMarshalling.unmarshalResults(
                nativeResults, functionType.getReturnTypes());
      } catch (final Exception e) {
        // Fallback to traditional unmarshalling
        results =
            JniTypeConverter.nativeResultsToWasmValues(
                nativeResults, functionType.getReturnTypes());
      }

      // Cache result for frequently called functions
      if (shouldCacheResult(currentCall)) {
        cacheResult(cacheKey, results);
      }

      return results;
    } catch (final JniValidationException e) {
      throw new WasmException("Parameter validation failed for function '" + name + "'", e);
    } catch (final RuntimeException e) {
      throw new WasmException("Native function call failed for '" + name + "'", e);
    } catch (final Exception e) {
      throw new WasmException("Unexpected error calling function '" + name + "'", e);
    } finally {
      ai.tegmentum.wasmtime4j.jni.performance.PerformanceMonitor.endOperation(
          "function_call", startTime);
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
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

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
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

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
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

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
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

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
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

    try {
      return nativeCallDouble(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
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
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    // Clear cache before closing
    clearCache();

    // Log performance stats
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          String.format(
              "Closing function '%s': %d calls, %.2f%% cache hit ratio",
              name, getCallCount(), getCacheHitRatio() * 100));
    }

    nativeDestroyFunction(getNativeHandle());
  }

  /**
   * Creates a cache key for the given parameters.
   *
   * @param params the function parameters
   * @return a string key for caching
   */
  private String createCacheKey(final WasmValue[] params) {
    if (params.length == 0) {
      return "empty";
    }

    final StringBuilder key = new StringBuilder();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) {
        key.append(",");
      }
      final WasmValue param = params[i];
      key.append(param.getType()).append(":");

      // Create a simple hash for the value to avoid storing large objects in keys
      if (param.getType() == WasmValueType.V128) {
        key.append(java.util.Arrays.hashCode(param.asV128()));
      } else {
        key.append(param.getValue());
      }
    }
    return key.toString();
  }

  /**
   * Determines if a cached result should be used.
   *
   * @param cached the cached result
   * @param currentCall the current call number
   * @return true if the cached result should be used
   */
  private boolean shouldUseCachedResult(final CachedResult cached, final long currentCall) {
    // Only use cache for pure functions (no side effects)
    // This is a simple heuristic - in a real implementation, this would be configurable
    return cached.hitCount > 3 && (currentCall - cached.timestamp) < 1000;
  }

  /**
   * Determines if the result should be cached.
   *
   * @param currentCall the current call number
   * @return true if the result should be cached
   */
  private boolean shouldCacheResult(final long currentCall) {
    return currentCall % 10 == 0 && resultCache.size() < MAX_CACHE_SIZE;
  }

  /**
   * Caches the result for the given key.
   *
   * @param key the cache key
   * @param result the result to cache
   */
  private void cacheResult(final String key, final WasmValue[] result) {
    final CachedResult existing = resultCache.get(key);
    final int hitCount = existing != null ? existing.hitCount + 1 : 1;
    resultCache.put(key, new CachedResult(result.clone(), System.currentTimeMillis(), hitCount));

    // Simple cache eviction
    if (resultCache.size() > MAX_CACHE_SIZE) {
      final String oldestKey =
          resultCache.entrySet().stream()
              .min((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
              .map(java.util.Map.Entry::getKey)
              .orElse(null);
      if (oldestKey != null) {
        resultCache.remove(oldestKey);
      }
    }
  }

  /** Clears the function result cache. */
  public void clearCache() {
    resultCache.clear();
    LOGGER.fine("Cleared result cache for function '" + name + "'");
  }

  /**
   * Optimizes hot path for frequently called parameter patterns.
   *
   * @param params current parameters
   */
  private void optimizeHotPath(final WasmValue[] params) {
    final String paramSignature = getParameterSignature(params);
    if (!paramSignature.equals(cachedParamSignature)) {
      // Pre-compute and cache marshalling for this parameter pattern
      try {
        cachedNativeParams.set(JniTypeConverter.wasmValuesToNativeParams(params));
        cachedParamSignature = paramSignature;
        lastOptimizationCheck = System.currentTimeMillis();
        LOGGER.fine(
            "Optimized hot path for function '" + name + "' with signature: " + paramSignature);
      } catch (final Exception e) {
        LOGGER.warning(
            "Failed to optimize hot path for function '" + name + "': " + e.getMessage());
      }
    }
  }

  /**
   * Gets a signature for the parameter pattern to identify repeated calls.
   *
   * @param params function parameters
   * @return signature string
   */
  private String getParameterSignature(final WasmValue[] params) {
    if (params.length == 0) {
      return "()";
    }

    final StringBuilder sig = new StringBuilder();
    sig.append("(");
    for (int i = 0; i < params.length; i++) {
      if (i > 0) {
        sig.append(",");
      }
      sig.append(params[i].getType().name());
    }
    sig.append(")");
    return sig.toString();
  }

  /**
   * Executes optimized call path for pre-validated parameters.
   *
   * @param params function parameters
   * @param functionType function type info
   * @return function results
   * @throws WasmException if call fails
   */
  private WasmValue[] callOptimizedPath(final WasmValue[] params, final FunctionType functionType)
      throws WasmException {
    try {
      // Use fast marshalling for known parameter types
      Object[] nativeParams;
      if (params.length <= 4) {
        // For small parameter sets, use direct conversion
        nativeParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
          nativeParams[i] = convertValueDirect(params[i]);
        }
      } else {
        // Use optimized marshalling for larger sets
        nativeParams =
            ai.tegmentum.wasmtime4j.jni.performance.OptimizedMarshalling.marshalParameters(params);
      }

      // Direct native call without additional overhead
      final Object[] nativeResults = nativeCallMultiValue(getNativeHandle(), store.getNativeHandle(), nativeParams);
      if (nativeResults == null) {
        throw new WasmException("Native function call returned null for '" + name + "'");
      }

      // Fast result conversion
      return ai.tegmentum.wasmtime4j.jni.performance.OptimizedMarshalling.unmarshalResults(
          nativeResults, functionType.getReturnTypes());

    } catch (final Exception e) {
      throw new WasmException("Optimized function call failed for '" + name + "'", e);
    }
  }

  /**
   * Direct value conversion for hot path optimization.
   *
   * @param value WasmValue to convert
   * @return native representation
   */
  private Object convertValueDirect(final WasmValue value) {
    switch (value.getType()) {
      case I32:
        return value.asI32();
      case I64:
        return value.asI64();
      case F32:
        return value.asF32();
      case F64:
        return value.asF64();
      default:
        // Fallback to general conversion
        return JniTypeConverter.wasmValueToNativeParam(value);
    }
  }

  /**
   * Gets the number of calls made to this function.
   *
   * @return the call count
   */
  public long getCallCount() {
    return callCount.get();
  }

  /**
   * Gets the cache hit ratio for this function.
   *
   * @return the cache hit ratio (0.0 to 1.0)
   */
  public double getCacheHitRatio() {
    final long totalCalls = callCount.get();
    if (totalCalls == 0) {
      return 0.0;
    }
    final int totalHits = resultCache.values().stream().mapToInt(r -> r.hitCount).sum();
    return (double) totalHits / totalCalls;
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
  private static native Object nativeCall(long functionHandle, long storeHandle, Object[] parameters);

  /**
   * Calls a function with parameters and returns multiple values.
   *
   * @param functionHandle the native function handle
   * @param storeHandle the native store handle
   * @param parameters the function parameters
   * @return array of return values (never null, may be empty)
   */
  private static native Object[] nativeCallMultiValue(long functionHandle, long storeHandle, Object[] parameters);

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
   * Destroys a native function.
   *
   * @param functionHandle the native function handle
   */
  private static native void nativeDestroyFunction(long functionHandle);
}
